import java.lang.*;
import java.util.*;

class EquTerm{
  boolean bMinus;
  StanfordWord wordDenominator;
  List<StanfordWord> lFactors = new ArrayList<StanfordWord>();
  StanfordWord wordUnknown;
  OldConcreteTerm concrete;
  boolean bFixed = false;
  String sStructural = null;

  EquTerm(boolean bMinus){
    this.bMinus = bMinus;
    this.concrete = new OldConcreteTerm(bMinus);
  }

  EquTerm(EquTerm termToCopy){
    this.bMinus = termToCopy.bMinus;
    this.wordDenominator = termToCopy.wordDenominator;
    this.lFactors = new ArrayList<StanfordWord>(termToCopy.lFactors);
    this.wordUnknown = termToCopy.wordUnknown;
    this.concrete = new OldConcreteTerm(termToCopy.concrete);
  }
  
  void fix(){
    this.bFixed = true;
  }

  String toStructuralString(){
    Misc.Assert(bFixed);
    if(this.sStructural == null){
      StringBuilder sb = new StringBuilder();
      if(bMinus){
        sb.append("-");
      } else {
        sb.append("+");
      }
      for(StanfordWord word : lFactors){
        sb.append("a");
      }
      if(wordUnknown != null){
        sb.append("x");
      }
      if(wordDenominator != null){
        sb.append("/a");
      }
      sStructural = sb.toString();
    }
    return sStructural;
  }

  boolean containsWord(StanfordWord word){
    Misc.Assert(word != null);
    for(StanfordWord wordFactor : lFactors){
      if(word == wordFactor){
        return true;
      }
    }
    return (word == wordUnknown) ||
      (word == wordDenominator);
  }

  public boolean equals(Object obj){
    if(!(obj instanceof EquTerm)){
      return false;
    }
    EquTerm termOther = (EquTerm) obj;
    if((this.bMinus != termOther.bMinus) ||
       !Objects.equals(this.wordDenominator,termOther.wordDenominator) ||
       !Objects.equals(this.wordUnknown, termOther.wordUnknown)){
      return false;
    }
    //we have to compare the factor lists -- turn them to sets to do this
    Set<StanfordWord> setFactors = new HashSet<StanfordWord>(this.lFactors);
    Set<StanfordWord> setFactorsOther = 
      new HashSet<StanfordWord>(termOther.lFactors);
    return setFactors.equals(setFactorsOther);
  }

  public int hashCode(){
    int iHashCode = -1;
    for(StanfordWord word : lFactors){
      iHashCode ^= word.hashCode();
    }
    iHashCode = Misc.hashCombine(iHashCode, bMinus);
    if(wordUnknown != null){
      iHashCode = Misc.hashCombine(iHashCode, wordUnknown);
    }
    if(wordDenominator != null){
      iHashCode = Misc.hashCombine(iHashCode, wordDenominator);
    }
    return iHashCode;
  }


  void addFactor(StanfordWord wordFactor){
    Misc.Assert(!bFixed);
    Misc.Assert(lFactors.size() < Config.config.iMaxEquFactors);
    this.lFactors.add(wordFactor);
    this.concrete.addFactor(wordFactor.number.fNumber);
  }

  void addUnknown(StanfordWord wordUnknown){
    Misc.Assert(!bFixed);
    Misc.Assert(this.wordUnknown == null);
    this.wordUnknown = wordUnknown;
    this.concrete.addUnknown();
  }

  void addDenominator(StanfordWord wordDenominator){
    Misc.Assert(!bFixed);
    Misc.Assert(this.wordDenominator == null);
    this.wordDenominator = wordDenominator;
    this.concrete.addDenominator(wordDenominator.number.fNumber);
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
    int iNumFactors = lFactors.size();
    if(wordUnknown != null){
      iNumFactors++;
    }
    if(iNumFactors > 1){
      sb.append("(");
    }
    boolean bIsFirst = true;
    if(wordUnknown != null){
      sb.append(wordUnknown.toFullString());
      bIsFirst = false;
    }
    for(StanfordWord wordFactor : lFactors){
      if(!bIsFirst){
        sb.append("*");
      }
      bIsFirst = false;
      sb.append(wordFactor.toFullString());
    }
    if(iNumFactors > 1){
      sb.append(")");
    }
    if(wordDenominator != null){
      sb.append("/").append(wordDenominator.toFullString());
    }
    return sb;
  }

}