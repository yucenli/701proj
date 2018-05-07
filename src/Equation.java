import java.lang.*;
import java.util.*;

class Equation extends EquationNode{
  EquationTerm termLeft;
  EquationTerm termRight;
  String sExactString;
  String sStructuralString;
  Integer iNumNumbers;
  Integer iNumUnknowns;
  static Comparator<Equation> comparatorStructural = new StructuralComparator();
  static Comparator<Equation> comparatorExact = new ExactComparator();

  Path[][] aPaths;

  Equation(Equation equationToCopy){
    this.termLeft = equationToCopy.termLeft.copy();
    this.termRight = equationToCopy.termRight.copy();
  }


  Equation(EquationTerm termLeft, EquationTerm termRight){
    this.termLeft = termLeft;
    this.termRight = termRight;
    if(Config.config.bNormalizeEquations){
      termLeft.normalize();
      termRight.normalize();
    }
  }

  boolean hasDisallowedConstants(List<Double> lAllowedConstants){
    return termLeft.hasDisallowedConstants(lAllowedConstants) || 
      termRight.hasDisallowedConstants(lAllowedConstants);
  }

  List<EquationLoc> getAllLocs(){
    List<EquationLoc> lLocs = new ArrayList<EquationLoc>();
    getAllLocs(lLocs, new ArrayList<EquationNode>());
    return lLocs;
  }

  public void getAllLocs(List<EquationLoc> lLocs,List<EquationNode> lAncestors){
    lAncestors.add(this);
    this.termLeft.getAllLocs(lLocs, lAncestors);
    this.termRight.getAllLocs(lLocs, lAncestors);
    lAncestors.remove(lAncestors.size()-1);
  }

  public void toFeatureStringBuilder(StringBuilder sb,Set<EquationNode> setArgs,
                                     List<Integer> lUnknowns,
                                     List<Integer> lNumbers){
    termLeft.toFeatureStringBuilder(sb,setArgs,lUnknowns, lNumbers);
    sb.append("=");
    termRight.toFeatureStringBuilder(sb, setArgs,lUnknowns, lNumbers);
  }
  
  List<String> getAllEquationletFeatures(){
    Set<String> setFeatures = new HashSet<String>();
    List<EquationLoc> lLocs = getAllLocs();
    Set<EquationNode> setArgs = new HashSet<EquationNode>();
    for(EquationLoc locHead : lLocs){
      EquationNode nodeHead = locHead.getNode();
      setFeatures.add(nodeHead.toFeatureString(setArgs));
      for(EquationLoc locArg1 : lLocs){
        setArgs.add(locArg1.getNode());
        setFeatures.add(nodeHead.toFeatureString(setArgs));
        for(EquationLoc locArg2 : lLocs){
          if(EquationLoc.areAncestors(locArg1, locArg2)){
            continue;
          }
          setArgs.add(locArg2.getNode());
          setFeatures.add(nodeHead.toFeatureString(setArgs));
          for(EquationLoc locArg3 : lLocs){
            if(EquationLoc.areAncestors(locArg3, locArg1) ||
               EquationLoc.areAncestors(locArg3, locArg2)){
              continue;
            }
            setArgs.add(locArg3.getNode());
            setFeatures.add(nodeHead.toFeatureString(setArgs));            
            setArgs.remove(locArg3);
          }
          setArgs.remove(locArg2);
        }
        setArgs.remove(locArg1);
      }
    }
    return new ArrayList<String>(setFeatures);
  }



  void engrainConstants(List<Double> lNumbers, List<Integer> lPointers){
    termLeft = EquationTerm.engrainConstants(termLeft, lNumbers, lPointers);
    termRight = EquationTerm.engrainConstants(termRight, lNumbers, lPointers);
    //reset the strings
    sExactString = null;
    sStructuralString = null;
    //and now we'll build the paths
    //buildPaths();
  }

  Set<Integer> getUsedNumbers(){
    Set<Integer> setUsed = new HashSet<Integer>();
    termLeft.addUsedNumbers(setUsed);
    termRight.addUsedNumbers(setUsed);
    return setUsed;
  }

  boolean hasDivide(){
    return termLeft.hasDivide() || termRight.hasDivide();
  }

  double[] toConstraint(List<Double> lNumbers, int iNumUnknowns){
    double[] aConstraint = new double[iNumUnknowns+1];
    boolean bSuccess = termLeft.toConstraint(aConstraint, lNumbers, 1.0, true);
    bSuccess &= termRight.toConstraint(aConstraint, lNumbers, 1.0, false);
    if(bSuccess){
      return aConstraint;
    } else {
      return null;
    }
  }

  void renumber(List<String> lOldUnknowns, List<String> lNewUnknowns,
                List<Double> lOldNumbers, List<Double> lNewNumbers){
    this.termLeft.renumber(lOldUnknowns, lNewUnknowns, lOldNumbers,lNewNumbers);
    this.termRight.renumber(lOldUnknowns,lNewUnknowns, lOldNumbers,lNewNumbers);
    this.sStructuralString = null;
    this.sExactString = null;
  }


  static class StructuralComparator implements Comparator<Equation>{
    public int compare(Equation equation1, Equation equation2){
      return equation1.toStructuralString().compareTo(
        equation2.toStructuralString());
    }
  }

  static class ExactComparator implements Comparator<Equation>{
    public int compare(Equation equation1, Equation equation2){
      return equation1.toExactString().compareTo(equation2.toExactString());
    }
  }

  public boolean equals(Object oOther){
    if(!(oOther instanceof Equation)){
      return false;
    }
    Equation equationOther = (Equation) oOther;
    boolean bEqual = this.toExactString().equals(equationOther.toExactString());
    if(bEqual){
      Misc.Assert(this.hashCode() == equationOther.hashCode());
    }
    return bEqual;
  }

  public int hashCode(){
    return this.toExactString().hashCode();
  }

  public String calcExactString(){
    return toStringBuilder(new StringBuilder(), null, null, true).toString();
  }


  public String toExactString(){
    if(this.sExactString == null){
      this.sExactString = calcExactString();        
    }
    return this.sExactString;
  }

  public String calcStructuralString(){
    return toStringBuilder(new StringBuilder(), null, null, false).toString();
  }

  public String toStructuralString(){
    if(this.sStructuralString == null){
      this.sStructuralString = calcStructuralString(); 
    }
    return this.sStructuralString;
  }

  public String toString(){
    return toExactString();
  }

  String toString(List<String> lUnknowns, List<Double> lNumbers){
    return toStringBuilder(new StringBuilder(), lUnknowns, 
                           lNumbers, null).toString();
  }

  StringBuilder toStringBuilder(StringBuilder sb, List<String> lUnknowns,
                                List<Double> lNumbers, Boolean bExact){
    Misc.Assert(((lUnknowns == null) && (lNumbers == null) && (bExact!=null))||
                ((lUnknowns != null) && (lNumbers != null) && (bExact ==null)));
                
    termLeft.toStringBuilder(sb, lUnknowns, lNumbers, bExact);
    sb.append("=");
    termRight.toStringBuilder(sb, lUnknowns, lNumbers, bExact);
    return sb;
  }

  StringBuilder toStringBuilder(StringBuilder sb, List<String> lUnknowns,
                                List<String> lNumbers){
    termLeft.toStringBuilder(sb, lUnknowns, lNumbers);
    sb.append("=");
    termRight.toStringBuilder(sb, lUnknowns, lNumbers);
    return sb;
  }


  class Path{
    int iDist;
    List<Dir> lDirPath = new ArrayList<Dir>();
    List<String> lOpPath = new ArrayList<String>();
    boolean bSameSideOfEquals = true;
    Path(List<Dir> lDirs, List<Operator> lOps){
      for(int iOp = 0; iOp < lOps.size(); iOp++){
        Dir dir = lDirs.get(iOp);
        Operator op = lOps.get(iOp);
        lDirPath.add(dir);
        lOpPath.add(op + ":" + dir);
        bSameSideOfEquals &= (op != null);
      }
    }
    public String toString(){
      return toStringBuilder(new StringBuilder()).toString();
    }

    StringBuilder toStringBuilder(StringBuilder sb){
      sb.append("PATH:")
        .append("\n   SameSideOfEquals: ").append(this.bSameSideOfEquals)
        .append("\n   ").append(this.lDirPath)
        .append("\n   ").append(this.lOpPath).append("\n");
      return sb;
    }
  }

  void buildPaths(){
    // int iNumTerms = this.iNumNumbers+this.iNumUnknowns;
    // aPaths = new Path[iNumTerms][iNumTerms];
    // //first set all the parent nodes
    // this.setAllParents(null);
    // Map<EquationNode,EquationNode> mParents = 
    //   new HashMap<EquationNode,EquationNode>();
    // buildParentMap(termLeft, null, mParents);
  }

  //void buildParentMap(EquationTerm term, EquationTerm termParent

  public void accept(EquationNodeVisitor visitor, List<String> lOps){
    visitor.visit(this, lOps);
    lOps.add("=");
    termLeft.accept(visitor, lOps);
    termRight.accept(visitor, lOps);
    lOps.remove(lOps.size()-1);
  }

}