import java.lang.*;
import java.util.*;

class EquationSystem{
  List<Equation> lEquations;
  int iNumUnknowns;
  int iNumNumbers;
  boolean bHasEquivalentEquations;
  int iIndex;
  List<String> lEquationletFeatures;

  EquationSystem(List<Equation> lEquations, int iNumUnknowns, int iNumNumbers){
    this.lEquations = lEquations;
    this.iNumUnknowns = iNumUnknowns;
    this.iNumNumbers = iNumNumbers;
    this.bHasEquivalentEquations = this.hasEquivalentEquations();
  }


  class CalcNumUsesVisitor implements EquationNodeVisitor{
    int[] aUnknownNumUses;
    int[] aNumberNumUses;
    boolean bCalc = true;
    CalcNumUsesVisitor(int iNumUnknowns, int iNumNumbers){
      aUnknownNumUses = new int[iNumUnknowns];
      aNumberNumUses = new int[iNumNumbers];
    }
    public void visit(Equation equation, List<String> lOps){}
    public void visit(EquationTerm.Complex equation, List<String> lOps){}
    public void visit(EquationTerm.Number number, List<String> lOps){
      if(bCalc){
        aNumberNumUses[number.iIndex]++;
      } else {
        Misc.Assert(number.iNumUses == -1);
        number.iNumUses = aNumberNumUses[number.iIndex];
      }
    }
    public void visit(EquationTerm.Unknown unknown, List<String> lOps){
      if(bCalc){
        aUnknownNumUses[unknown.iIndex]++;
      } else {
        Misc.Assert(unknown.iNumUses == -1);
        unknown.iNumUses = aUnknownNumUses[unknown.iIndex];
      }
    }
    public void visit(EquationTerm.Constant constant, List<String> lOps){}
  }

  void setNumUses(){
    CalcNumUsesVisitor visitor=new CalcNumUsesVisitor(iNumUnknowns,iNumNumbers);
    for(Equation equation : lEquations){
      equation.accept(visitor, new ArrayList<String>());
    }
    visitor.bCalc = false;
    for(Equation equation : lEquations){
      equation.accept(visitor, new ArrayList<String>());
    }
  }

  boolean hasDisallowedConstants(List<Double> lAllowedConstants){
    for(Equation equation : this.lEquations){
      if(equation.hasDisallowedConstants(lAllowedConstants)){
        return true;
      }
    }
    return false;
  }

  
  void normalize(List<String> lUnknowns, List<String> lNewUnknowns,
                 List<Double> lNumbers, List<Double> lNewNumbers){
    Collections.sort(lEquations, Equation.comparatorStructural);
    for(Equation equation : lEquations){
      equation.renumber(lUnknowns, lNewUnknowns, lNumbers, lNewNumbers);
    }
    this.iNumUnknowns = lNewUnknowns.size();
    this.iNumNumbers = lNewNumbers.size();
    this.bHasEquivalentEquations = this.hasEquivalentEquations();
  }

  List<String> getEquationletFeatures(){
    if(lEquationletFeatures == null){
      lEquationletFeatures = new ArrayList<String>();
      for(Equation equation : this.lEquations){
        lEquationletFeatures.addAll(equation.getAllEquationletFeatures());
      }
    }
    return lEquationletFeatures;
  }


  boolean hasEquivalentEquations(){
    boolean bHasEquivalentEquations = false;
    // check if it has equivalent equations
    for(int iEquation = 0; iEquation < lEquations.size()-1; iEquation++){
      if(lEquations.get(iEquation).equals(lEquations.get(iEquation+1))){
        bHasEquivalentEquations = true;
        break;
      }
    }
    return bHasEquivalentEquations;
  }


  void engrainConstants(List<Double> lNumbers, List<Integer> lPointers,
                        int iNewNumNumbers){
    for(Equation equation : lEquations){
      equation.engrainConstants(lNumbers, lPointers);
    }
    this.iNumNumbers = iNewNumNumbers;
    setNumUses();
  }

  boolean isNonlinear(List<Double> lNumbers, int iNumUnknowns){
    for(Equation equation : lEquations){
      if(equation.toConstraint(lNumbers, iNumUnknowns) == null){
        return true;
      }
    }
    return false;
  }
  public boolean equals(Object obj){
    if(!(obj instanceof EquationSystem)){
      return false;
    }
    EquationSystem systemOther = (EquationSystem) obj;
    return this.lEquations.equals(systemOther.lEquations);
  }

  public int hashCode(){
    return lEquations.hashCode();
  }


  int numUnknowns(){
    return iNumUnknowns;
  }

  int numNumbers(){
    return iNumNumbers;
  }

  public String toString(){
    return toStringBuilder(new StringBuilder(), null, null, true).toString();
  }

  StringBuilder toStringBuilder(StringBuilder sb, List<String> lUnknowns,
                                List<Double> lNumbers, Boolean bExact){
    for(Equation equation : lEquations){
      equation.toStringBuilder(sb, lUnknowns, lNumbers, bExact).append("\n");
    }
    return sb;
  }

  public String toFeatureString(){
    StringBuilder sb = new StringBuilder();
    for(Equation equation : lEquations){
      equation.toStringBuilder(sb, null, null, true).append(":::");
    }
    return sb.toString();
  }
}