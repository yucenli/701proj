import java.lang.*;
import java.util.*;

class Slot{
  StanfordWord wordNoun;
  StanfordWord wordNumber;
  List<List<Integer>> llTermLists;
  Double fNearbyConstant;
  Double fUnknownSolution = null;

  Slot(StanfordWord word, boolean bNumber, List<List<Integer>> llTermLists,
       Double fNearbyConstant){
    if(bNumber){
      this.wordNoun = word.number.wordNoun;
      this.wordNumber = word;
    } else {
      this.wordNoun = word;
    }
    this.llTermLists = llTermLists;
    this.fNearbyConstant = fNearbyConstant;
  }

  boolean isNumber(){
    return (this.wordNumber != null);
  }

  Double getNumberValue(){
    if(this.isNumber()){
      return this.wordNumber.number.fNumber;
    } else {
      //might be null
      return this.fUnknownSolution;
    }
  }

  public boolean equals(Object obj){
    if(!(obj instanceof Slot)){
      return false;
    }
    Slot slotOther = (Slot) obj;
    return (this.wordNoun == slotOther.wordNoun) && 
      (this.wordNumber == slotOther.wordNumber) &&
      (this.llTermLists == slotOther.llTermLists) &&
      (Objects.equals(this.fNearbyConstant, slotOther.fNearbyConstant));
  }

  public int hashCode(){
    if(wordNumber == null){
      return wordNoun.hashCode();
    } else {
      int iHash =Misc.hashCombine(wordNoun.hashCode(),wordNumber.hashCode());
      iHash = Misc.hashCombine(iHash, llTermLists.hashCode());
      if(fNearbyConstant != null){
        iHash = Misc.hashCombine(iHash, fNearbyConstant.hashCode());
      }
      return iHash;
    }
  }

  StringBuilder toStringBuilder(StringBuilder sb){
    sb.append("Slot[Noun: ").append(wordNoun);
    if(wordNumber != null){
      sb.append(" Num: ").append(wordNumber);
    }
    sb.append(" Terms: ").append(llTermLists).append("]");
    return sb;
  }
  public String toString(){
    return toStringBuilder(new StringBuilder()).toString();
  }

}