import java.lang.*;
import java.util.*;
import com.google.common.collect.*;

class OldConcreteTerm{
  boolean bMinus;
  Multiset<Double> setFactors = HashMultiset.create();
  Double fDenominator = null;
  boolean bUnknown = false;
  TermStructure structure;
  
  OldConcreteTerm(boolean bMinus){
    this.bMinus = bMinus;
    structure = new TermStructure(bMinus);
  }

  OldConcreteTerm(OldConcreteTerm termToCopy){
    this.bMinus = termToCopy.bMinus;
    this.setFactors = HashMultiset.create(termToCopy.setFactors);
    this.fDenominator = termToCopy.fDenominator;
    this.bUnknown = termToCopy.bUnknown;
    this.structure = new TermStructure(termToCopy.structure);
  }

  String toStructuralString(){
    StringBuilder sb = new StringBuilder();
    if(bMinus){
      sb.append("-");
    } else {
      sb.append("+");
    }
    for(Double fDouble : setFactors){
      sb.append("a");
    }
    if(bUnknown){
      sb.append("x");
    }
    if(fDenominator != null){
      sb.append("/a");
    }
    return sb.toString();
  }

  int numSlots(){
    int iNumSlots = setFactors.size();
    if(fDenominator != null){
      iNumSlots ++;
    }
    if(bUnknown){
      iNumSlots++;
    }
    return iNumSlots;
  }

  void addFactor(Double fFactor){
    Misc.Assert(!fFactor.equals(0.0));
    setFactors.add(fFactor);
    this.structure.addFactor();
  }

  void addUnknown(){
    Misc.Assert(!this.bUnknown);
    this.bUnknown = true;
    this.structure.addFactor();
  }
  
  void addDenominator(Double fDenominator){
    Misc.Assert(this.fDenominator == null);
    this.fDenominator = fDenominator;
    this.structure.addFactor();
  }


  public int hashCode(){
    Misc.Assert(false); // this should never get used
    return 0;
  }

  public boolean equals(Object obj){
    Misc.Assert(false); // this should never get used, right?
    if(!(obj instanceof OldConcreteTerm)){
      return false;
    }
    OldConcreteTerm termOther = (OldConcreteTerm) obj;
    return equals(termOther, false) || equals(termOther, true);
  }

  static boolean equalDouble(Double fA, Double fB){
    if(fA == null){
      return (fB == null);
    } else {
      return fA.equals(fB);
    }
  }

  public boolean equals(OldConcreteTerm termOther, boolean bOpposite){
    boolean bResult = (((this.bMinus == termOther.bMinus) != bOpposite) &&
                       this.setFactors.equals(termOther.setFactors) &&
                       equalDouble(this.fDenominator, termOther.fDenominator) &&
                       (this.bUnknown == termOther.bUnknown));
    //System.out.println("Comparing: " + this + " to: " + termOther + " --> " +
    //                   bResult);
    return  bResult;
  }

  public boolean subsetOf(OldConcreteTerm termSuperset, boolean bOpposite){
    //checks if this is a subset of termSuperset
    if((this.bMinus == termSuperset.bMinus) == bOpposite){
      return false;
    }
    //if this one has an unknown then the superset must as well
    if(this.bUnknown && !termSuperset.bUnknown){
      return false;
    }
    // all the factor counts in the superset must be greater than factor counts
    // in subset
    if(!Multisets.containsOccurrences(termSuperset.setFactors,this.setFactors)){
      return false;
    }
    if(this.fDenominator != null){
      return this.fDenominator.equals(termSuperset.fDenominator);
    } else {
      return true;
    }
  }
  
  public String toString(){
    return toStringBuilder(new StringBuilder()).toString();
  }

  StringBuilder toStringBuilder(StringBuilder sb){
    if(bMinus){
      sb.append(" - ");
    } else {
      sb.append(" + ");
    }
    int iNumFactors = setFactors.size();
    if(bUnknown){
      iNumFactors++;
    }
    if(iNumFactors > 1){
      sb.append("(");
    }
    boolean bIsFirst = true;
    if(bUnknown){
      sb.append("x");
      bIsFirst = false;
    }
    for(Double fFactor : setFactors){
      if(!bIsFirst){
        sb.append("*");
      }
      bIsFirst = false;
      sb.append(fFactor);
    }
    if(iNumFactors > 1){
      sb.append(")");
    }
    if(fDenominator != null){
      sb.append("/").append(fDenominator);
    }
    return sb;
  }


}