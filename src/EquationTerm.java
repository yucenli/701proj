import java.lang.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;

abstract class EquationTerm extends EquationNode{
  
  abstract public boolean hasDisallowedConstants(List<Double> lAllowedConstants);

  abstract public EquationTerm copy();

  abstract public void toFeatureStringBuilder(StringBuilder sb, 
                                              Set<EquationNode> setArgs,
                                              List<Integer> lUnknowns,
                                              List<Integer> lNumbers);
  
  abstract public void getAllLocs(List<EquationLoc> lLocs, 
                                  List<EquationNode> lAncestors);

  abstract public void normalize();
  abstract public StringBuilder toStringBuilder(StringBuilder sb, 
                                                List<String> lUnknowns,
                                                List<Double> lNumbers,
                                                Boolean bExact);
  abstract public StringBuilder toStringBuilder(StringBuilder sb, 
                                                List<String> lUnknowns,
                                                List<String> lNumbers);
  public String toString(){
    return toStringBuilder(new StringBuilder(), null, null, true).toString();
  }

  abstract public void renumber(List<String> lOldUnknowns, 
                                List<String> lNewUnknowns,
                                List<Double> lOldNumbers, 
                                List<Double> lNewNumbers);

  abstract public Double calcValue(List<Double> lNumbers);
  abstract public boolean hasDivide();

  abstract public void addUsedNumbers(Set<Integer> setUsed);
  
  abstract public boolean toConstraint(double[] aConstraint, 
                                       List<Double> lNumbers,
                                       double fMultiplier, boolean bLeft);

  abstract public void accept(EquationNodeVisitor visitor, List<String> lOps);
  
  static public EquationTerm engrainConstants(EquationTerm termCur,
                                              List<Double> lNumbers,
                                              List<Integer> lPointers){
    if(termCur instanceof Complex){
      Complex complex = (Complex) termCur;
      complex.termLeft = engrainConstants(complex.termLeft, lNumbers,lPointers);
      complex.termRight =engrainConstants(complex.termRight,lNumbers,lPointers);
      return complex;
    } else if(termCur instanceof Number){
      Number number = (Number) termCur;
      Integer iNewIndex = lPointers.get(number.iIndex);
      if(iNewIndex == null){
        //new to change it to a constant
        return new Constant(lNumbers.get(number.iIndex));
      } else {
        //leave it intack, but update it's index
        number.iIndex = iNewIndex;
        return number;
      }
    } else {
      Misc.Assert((termCur instanceof Constant) ||(termCur instanceof Unknown));
      return termCur;
    }
  }


  static class Complex extends EquationTerm{
    Operator op;
    EquationTerm termLeft;
    EquationTerm termRight;
    Complex(Operator op, EquationTerm termLeft, EquationTerm termRight){
      this.op = op;
      this.termLeft = termLeft;
      this.termRight = termRight;
    }

    public void accept(EquationNodeVisitor visitor, List<String> lOps){
      visitor.visit(this, lOps);
      lOps.add(this.op.toString());
      this.termLeft.accept(visitor, lOps);
      this.termRight.accept(visitor, lOps);
      lOps.remove(lOps.size()-1);
    }

    public boolean hasDisallowedConstants(List<Double> lAllowedConstants){
      return termLeft.hasDisallowedConstants(lAllowedConstants) || 
        termRight.hasDisallowedConstants(lAllowedConstants);
    }
    
    public EquationTerm copy(){
      return new Complex(this.op, termLeft.copy(), termRight.copy());
    }

    public void toFeatureStringBuilder(StringBuilder sb, 
                                       Set<EquationNode> setArgs,
                                       List<Integer> lUnknowns,
                                       List<Integer> lNumbers){
      sb.append("(");
      termLeft.toFeatureStringBuilder(sb,setArgs,lUnknowns,lNumbers);
      sb.append(")");
      sb.append(this.op.toString());
      sb.append("(");
      termRight.toFeatureStringBuilder(sb, setArgs,lUnknowns,lNumbers);
      sb.append(")");
    }

    public void getAllLocs(List<EquationLoc> lLocs, 
                           List<EquationNode> lAncestors){
      lAncestors.add(this);
      lLocs.add(new EquationLoc(new ArrayList<EquationNode>(lAncestors)));
      termLeft.getAllLocs(lLocs, lAncestors);
      termRight.getAllLocs(lLocs, lAncestors);
      lAncestors.remove(lAncestors.size()-1);
    }


    public void addUsedNumbers(Set<Integer> setUsed){
      termLeft.addUsedNumbers(setUsed);
      termRight.addUsedNumbers(setUsed);
    }


    public boolean hasDivide(){
      if(op.equals(Operator.DIVIDEBY)){
        return true;
      } else {
        return termLeft.hasDivide() || termRight.hasDivide();
      }
    }

    public boolean toConstraint(double[] aConstraint, List<Double> lNumbers,
                                double fMultiplier, boolean bLeft){

      boolean bSuccess = true;
      if((this.op == Operator.PLUS) || (this.op == Operator.MINUS)){
        bSuccess &= 
          termLeft.toConstraint(aConstraint, lNumbers, fMultiplier, bLeft);
        if(this.op == Operator.MINUS){
          bSuccess &= 
            termRight.toConstraint(aConstraint, lNumbers, -fMultiplier, bLeft);
        } else {
          bSuccess &=
            termRight.toConstraint(aConstraint, lNumbers, fMultiplier, bLeft);
        }
      } else {
        Double fLeft = termLeft.calcValue(lNumbers);
        Double fRight = termRight.calcValue(lNumbers);
        if((fLeft != null) && (fRight != null)){
          double fVal = fMultiplier*op.perform(fLeft, fRight);
          if(bLeft){
            fVal = -fVal;
          }
          aConstraint[aConstraint.length-1] += fVal;
        } else if(op == Operator.TIMES){
          if(fLeft == null){
            if(fRight == null){
              return false;
            }
            bSuccess &= 
              termLeft.toConstraint(aConstraint, lNumbers, fMultiplier*fRight,
                                    bLeft);
          } else {
            Misc.Assert(fRight == null);
            bSuccess &=
              termRight.toConstraint(aConstraint, lNumbers, fMultiplier*fLeft,
                                     bLeft);
          }
        } else {
          Misc.Assert(op == Operator.DIVIDEBY);
          if(fRight == null){
            return false;
          }
          bSuccess &= termLeft.toConstraint(aConstraint, lNumbers, 
                                            fMultiplier/fRight, bLeft);
        }
      }
      return bSuccess;
    }

    public Double calcValue(List<Double> lNumbers){
      Double fLeft = termLeft.calcValue(lNumbers);
      Double fRight = termRight.calcValue(lNumbers);
      if((fLeft == null) || (fRight == null)){
        return null;
      } else {
        return op.perform(fLeft, fRight);
      }
    }    


    public void renumber(List<String> lOldUnknowns, List<String> lNewUnknowns,
                         List<Double> lOldNumbers, List<Double> lNewNumbers){
      termLeft.renumber(lOldUnknowns, lNewUnknowns, lOldNumbers,lNewNumbers);
      termRight.renumber(lOldUnknowns,lNewUnknowns, lOldNumbers,lNewNumbers);
    }

    public void normalize(){
      //only reorder if the operator is order agnostic
      if((this.op == Operator.TIMES) || (this.op == Operator.PLUS)){
        if((termRight instanceof Number) && (termLeft instanceof Unknown)){
          EquationTerm termTmp = termLeft;
          this.termLeft = termRight;
          this.termRight = termTmp;
        }
      }
      this.termLeft.normalize();
      this.termRight.normalize();
    }

    public StringBuilder toStringBuilder(StringBuilder sb, 
                                         List<String> lUnknowns,
                                         List<Double> lNumbers,
                                         Boolean bExact){
      if(termLeft instanceof Complex){
        sb.append("(");
        termLeft.toStringBuilder(sb, lUnknowns, lNumbers, bExact);
        sb.append(")");
      } else {
        termLeft.toStringBuilder(sb, lUnknowns, lNumbers, bExact);
      }
      sb.append(op.toString());
      if(termRight instanceof Complex){
        sb.append("(");
        termRight.toStringBuilder(sb, lUnknowns, lNumbers, bExact);
        sb.append(")");
      } else {
        termRight.toStringBuilder(sb, lUnknowns, lNumbers, bExact);
      }
      return sb;
    }

    public StringBuilder toStringBuilder(StringBuilder sb, 
                                         List<String> lUnknowns,
                                         List<String> lNumbers){
      if(termLeft instanceof Complex){
        sb.append("(");
        termLeft.toStringBuilder(sb, lUnknowns, lNumbers);
        sb.append(")");
      } else {
        termLeft.toStringBuilder(sb, lUnknowns, lNumbers);
      }
      sb.append(op.toString());
      if(termRight instanceof Complex){
        sb.append("(");
        termRight.toStringBuilder(sb, lUnknowns, lNumbers);
        sb.append(")");
      } else {
        termRight.toStringBuilder(sb, lUnknowns, lNumbers);
      }
      return sb;
    }
  }
  static class Unknown extends EquationTerm{
    int iIndex;
    int iOrigIndex;
    int iNumUses = -1;
    int iFirstUse = -1;
    Unknown(int iIndex){
      this.iIndex = iIndex;
    }

    Unknown(String sUnknown, Pair<List<String>,Integer> pairUnknowns){
      this.iIndex = getIndex(sUnknown, pairUnknowns.getLeft());
      this.iOrigIndex = pairUnknowns.getRight();
      pairUnknowns.setValue(pairUnknowns.getRight()+1);
    }

    public void accept(EquationNodeVisitor visitor, List<String> lOps){
      visitor.visit(this, lOps);
    }
    public boolean hasDisallowedConstants(List<Double> lAllowedConstants){
      return false;
    }

    public EquationTerm copy(){
      return new Unknown(this.iIndex);
    }

    public void toFeatureStringBuilder(StringBuilder sb, 
                                       Set<EquationNode> setArgs,
                                       List<Integer> lUnknowns,
                                       List<Integer> lNumbers){
      //check if our index is already contain in the list of unkowns
      for(int iUnknown = 0; iUnknown < lUnknowns.size(); iUnknown++){
        if(iIndex == lUnknowns.get(iUnknown)){
          sb.append((char)('m' + iUnknown));
          return;
        }
      }
      // not in the current list so add it
      sb.append((char)('m' + lUnknowns.size()));
      lUnknowns.add(iIndex);
    }

    public void getAllLocs(List<EquationLoc> lLocs, 
                           List<EquationNode> lAncestors){
      lAncestors.add(this);
      lLocs.add(new EquationLoc(new ArrayList<EquationNode>(lAncestors)));
      lAncestors.remove(lAncestors.size()-1);
    }

    static int getIndex(String sUnknown, List<String> lUnknowns){
      for(int iIndex = 0; iIndex < lUnknowns.size(); iIndex++){
        if(lUnknowns.get(iIndex).equals(sUnknown)){
          return iIndex;
        }
      }
      // doesn't exist so add it
      lUnknowns.add(sUnknown);
      return lUnknowns.size()-1;
    }

    public void addUsedNumbers(Set<Integer> setUsed){
      //nothing to do
    }

    public boolean hasDivide(){
      return false;
    }

    public boolean toConstraint(double[] aConstraint, List<Double> lNumbers,
                             double fMultiplier, boolean bLeft){
      if(iIndex >= aConstraint.length-1){
        System.out.println("Bad Index: " + iIndex + " " + aConstraint.length);
      }
      Misc.Assert(iIndex < aConstraint.length-1);
      if(bLeft){
        aConstraint[iIndex] += fMultiplier;
      } else {
        aConstraint[iIndex] -= fMultiplier;
      }
      return true;
    }

    public Double calcValue(List<Double> lNumbers){
      return null;
    }    

    public void renumber(List<String> lOldUnknowns, List<String> lNewUnknowns,
                         List<Double> lOldNumbers, List<Double> lNewNumbers){
      String sUnknown = lOldUnknowns.get(iIndex);
      this.iIndex = getIndex(sUnknown, lNewUnknowns);
    }

    public void normalize(){}

    public StringBuilder toStringBuilder(StringBuilder sb, 
                                         List<String> lUnknowns, 
                                         List<Double> lNumbers,
                                         Boolean bExact){
      if(lUnknowns == null){
        Misc.Assert(bExact != null);
        if(bExact){
          return sb.append((char)('m'+iIndex)).append(":").append(iOrigIndex);
        } else {
          return sb.append("#");
        }
      } else {
        Misc.Assert(bExact == null);
        return sb.append(lUnknowns.get(iIndex));
      }
    }

    public StringBuilder toStringBuilder(StringBuilder sb, 
                                         List<String> lUnknowns, 
                                         List<String> lNumbers){
      return sb.append(lUnknowns.get(iIndex));
    }
  }
  static class Number extends EquationTerm{
    int iIndex;
    int iNumUses = -1;
    int iFirstUse = -1;
    Number(int iIndex){
      this.iIndex = iIndex;
    }
    Number(String sNumber, List<Double> lNumbers, boolean bNegative){
      //first turn the number into a double
      Double fNumber = Misc.parseDouble(sNumber);
      if(bNegative){
        fNumber = -fNumber;
      }
      this.iIndex = getIndex(fNumber, lNumbers);
    }
    
    public void accept(EquationNodeVisitor visitor, List<String> lOps){
      visitor.visit(this, lOps);
    }

    public boolean hasDisallowedConstants(List<Double> lAllowedConstants){
      return false;
    }

    public EquationTerm copy(){
      return new Number(this.iIndex);
    }

    public void toFeatureStringBuilder(StringBuilder sb, 
                                       Set<EquationNode> setArgs,
                                       List<Integer> lUnknowns,
                                       List<Integer> lNumbers){
      //check if our index is already contain in the list of unkowns
      for(int iNumber = 0; iNumber < lNumbers.size(); iNumber++){
        if(iIndex == lNumbers.get(iNumber)){
          sb.append((char)('a' + iNumber));
          return;
        }
      }
      // not in the current list so add it
      sb.append((char)('a' + lNumbers.size()));
      lNumbers.add(iIndex);
    }

    public void getAllLocs(List<EquationLoc> lLocs, 
                           List<EquationNode> lAncestors){
      lAncestors.add(this);
      lLocs.add(new EquationLoc(new ArrayList<EquationNode>(lAncestors)));
      lAncestors.remove(lAncestors.size()-1);
    }

    public void addUsedNumbers(Set<Integer> setUsed){
      setUsed.add(iIndex);
    }

    static int getIndex(Double fNumber, List<Double> lNumbers){
      //check it exists and reuse the existing number if it already exists
      for(int iNumber = 0; iNumber < lNumbers.size(); iNumber++){
        if(Misc.isCloseTo(fNumber, lNumbers.get(iNumber))){
          return iNumber;
        }
      }
      //it doesn't exist yet, so add it
      lNumbers.add(fNumber);
      return lNumbers.size()-1;
    }

    public boolean hasDivide(){
      return false;
    }

    public boolean toConstraint(double[] aConstraint, List<Double> lNumbers,
                             double fMultiplier, boolean bLeft){
      Misc.Assert(iIndex < lNumbers.size());
      if(bLeft){
        aConstraint[aConstraint.length-1] -= fMultiplier*lNumbers.get(iIndex);
      } else {
        aConstraint[aConstraint.length-1] += fMultiplier*lNumbers.get(iIndex);
      }
      return true;
    }

    public Double calcValue(List<Double> lNumbers){
      return lNumbers.get(iIndex);
    }    

    public void renumber(List<String> lOldUnknowns, List<String> lNewUnknowns,
                         List<Double> lOldNumbers, List<Double> lNewNumbers){
      double fNumber = lOldNumbers.get(iIndex);
      this.iIndex = getIndex(fNumber, lNewNumbers);
    }

    public void normalize(){}

    public StringBuilder toStringBuilder(StringBuilder sb, 
                                         List<String> lUnknowns, 
                                         List<Double> lNumbers, 
                                         Boolean bExact){
      if(lNumbers == null){
        Misc.Assert(bExact != null);
        if(bExact){
          return sb.append((char)('a'+iIndex));
        } else {
          return sb.append("#");
        }
      } else {
        Double fVal = lNumbers.get(iIndex);
        if(fVal < 0.0){
          return sb.append("(").append(fVal).append(")");
        } else {
          return sb.append(fVal);
        }
      }
    }

    public StringBuilder toStringBuilder(StringBuilder sb, 
                                         List<String> lUnknowns, 
                                         List<String> lNumbers){
      return sb.append(lNumbers.get(iIndex));
    }
  }

  static class Constant extends EquationTerm{
    double fConstant;
    Constant(double fConstant){
      this.fConstant = fConstant;
    }

    public void accept(EquationNodeVisitor visitor, List<String> lOps){
      visitor.visit(this, lOps);
    }

    public boolean hasDisallowedConstants(List<Double> lAllowedConstants){
      return !lAllowedConstants.contains(fConstant);
    }

    public EquationTerm copy(){
      return new Constant(this.fConstant);
    }

    public void toFeatureStringBuilder(StringBuilder sb, 
                                       Set<EquationNode> setArgs,
                                       List<Integer> lUnknowns,
                                       List<Integer> lNumbers){
      //round the constant to two digits
      sb.append(Misc.roundToTwoDecimalPlaces(fConstant));
    }

    public void getAllLocs(List<EquationLoc> lLocs, 
                           List<EquationNode> lAncestors){
      lAncestors.add(this);
      lLocs.add(new EquationLoc(new ArrayList<EquationNode>(lAncestors)));
      lAncestors.remove(lAncestors.size()-1);
    }

    public void addUsedNumbers(Set<Integer> setUsed){
      //nothing to do
    }
    public boolean hasDivide(){
      return false;
    }

    public boolean toConstraint(double[] aConstraint, List<Double> lNumbers,
                             double fMultiplier, boolean bLeft){
      if(bLeft){
        aConstraint[aConstraint.length-1] -= fMultiplier*fConstant;
      } else {
        aConstraint[aConstraint.length-1] += fMultiplier*fConstant;
      }
      return true;
    }

    public void renumber(List<String> lOldUnknowns, List<String> lNewUnknowns,
                         List<Double> lOldNumbers, List<Double> lNewNumbers){
      //constants don't get renumbered
    }

    public Double calcValue(List<Double> lNumbers){
      return fConstant;
    }    

    public void normalize(){}

    public StringBuilder toStringBuilder(StringBuilder sb,
                                         List<String> lUnknowns,
                                         List<Double> lNumbers,
                                         Boolean bExact){
      if(Config.config.bPrintBarsAroundConstants){
        return sb.append("|").append(fConstant).append("|");
      } else {
        return sb.append(fConstant);
      }
    }
    public StringBuilder toStringBuilder(StringBuilder sb,
                                         List<String> lUnknowns,
                                         List<String> lNumbers){
      if(Config.config.bPrintBarsAroundConstants){
        return sb.append("|").append(fConstant).append("|");
      } else {
        return sb.append(fConstant);
      }
    }
  }
}

