class TermStructure{
  boolean bMinus;
  int iNumFactors;
  boolean bDenominator;
  boolean bUnknown;
  
 TermStructure(boolean bMinus){
    this.bMinus = bMinus;
  }

  TermStructure(TermStructure tsToCopy){
    this.bMinus = tsToCopy.bMinus;
    this.iNumFactors = tsToCopy.iNumFactors;
    this.bDenominator = tsToCopy.bDenominator;
    this.bUnknown = tsToCopy.bUnknown;
  }

  void addFactor(){
    iNumFactors++;
  }

  void addUnknown(){
    Misc.Assert(!this.bUnknown);
    this.bUnknown = true;
  }
  
  void addDenominator(Double fDenominator){
    Misc.Assert(!this.bDenominator);
    this.bDenominator = true;
  }

  public int hashCode(){
    Misc.Assert(false); // this should never get used
    return 0;
  }

  public boolean equals(Object obj){
    Misc.Assert(false); // this should never get used, right?
    return false;
    // if(!(obj instanceof OldConcreteTerm)){
    //   return false;
    // }
    // OldConcreteTerm termOther = (OldConcreteTerm) obj;
    // return equals(termOther, false) || equals(termOther, true);
  }


  boolean subsetOf(TermStructure termOther, boolean bOpposite){
    if((this.bMinus == termOther.bMinus) == bOpposite){
      return false;
    }
    if(this.bDenominator && !termOther.bDenominator){
      return false;
    }
    if(this.bUnknown && !termOther.bUnknown){
      return false;
    }
    return (this.iNumFactors <= termOther.iNumFactors);
  }
}