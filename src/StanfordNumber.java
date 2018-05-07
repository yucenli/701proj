import java.lang.*;
import java.util.*;

import java.text.*;

class StanfordNumber{
  static Map<String,Double> mNumbers;
  static StanfordNumber ZERO = new StanfordNumber(0.0);
  static StanfordNumber ONE = new StanfordNumber(1.0);

  static {
    mNumbers = new HashMap<String,Double>();
    mNumbers.put("double",2.0);
    mNumbers.put("triple",3.0);
    mNumbers.put("half",0.5);
    mNumbers.put("thousand",1000.0);
    mNumbers.put("one", 1.0);
    mNumbers.put("once", 1.0);
    mNumbers.put("two", 2.0);
    mNumbers.put("twice", 2.0);
    mNumbers.put("thrice", 3.0);
    mNumbers.put("three", 3.0);
    mNumbers.put("four", 4.0);
    mNumbers.put("five", 5.0);
    mNumbers.put("six", 6.0);
    mNumbers.put("seven", 7.0);
    mNumbers.put("eight", 8.0);
    mNumbers.put("nine", 9.0);
    mNumbers.put("ten", 10.0);
    mNumbers.put("eleven", 10.0);
    mNumbers.put("twelve", 10.0);
    mNumbers.put("One", 1.0);
    mNumbers.put("Once", 1.0);
    mNumbers.put("Two", 2.0);
    mNumbers.put("Twice", 2.0);
    mNumbers.put("Three", 3.0);
    mNumbers.put("Four", 4.0);
    mNumbers.put("Five", 5.0);
    mNumbers.put("Six", 6.0);
    mNumbers.put("Seven", 7.0);
    mNumbers.put("Eight", 8.0);
    mNumbers.put("Nine", 9.0);
    mNumbers.put("Ten", 10.0);
    mNumbers.put("1\\/2",0.5);
    mNumbers.put("2\\/3",0.666666666666);
    mNumbers.put("1\\/4",0.25);
    //mNumbers.put("penny",0.01);
    //mNumbers.put("nickel",0.05);
    //mNumbers.put("dime",0.1);
    //mNumbers.put("quarter",0.25);
    //mNumbers.put("pennies",0.01);
    //mNumbers.put("nickels",0.05);
    //mNumbers.put("dimes",0.1);
    //mNumbers.put("quarters",0.25);
    mNumbers.put("hundreds",100.0);
    mNumbers.put("tens",10.0);
    mNumbers.put("fifteen",15.0);
    mNumbers.put("forty-five",45.0);
    mNumbers.put("eighteen",18.0);
    // mNumbers.put("first",1.0);
    // mNumbers.put("second",2.0);
    // mNumbers.put("third",3.0);
    // mNumbers.put("fourth",4.0);
    // mNumbers.put("fifth",5.0);
    // mNumbers.put("sixth",6.0);
    // mNumbers.put("seventh",7.0);
    // mNumbers.put("eighth",8.0);
    // mNumbers.put("ninth",9.0);
    // mNumbers.put("tenth",10.0);
    // mNumbers.put("1st",1.0);
    // mNumbers.put("2nd",2.0);
    // mNumbers.put("3rd",3.0);
    // mNumbers.put("4th",4.0);
    // mNumbers.put("5th",5.0);
    // mNumbers.put("6th",6.0);
    // mNumbers.put("7th",7.0);
    // mNumbers.put("8th",8.0);
    // mNumbers.put("9th",9.0);
    // mNumbers.put("10th",10.0);
  }

  boolean bIsLargest;
  double fNumber;
  int iSentence;
  int iWord; // first word
  StanfordWord word; // it's parent is the noun it's modifying
  int iWordIndex; // which *number* word it is (i.e. it's the 2nd number in the sent)
  StanfordParseTree ptNP;
  StanfordWord wordNoun;

  StanfordNumber(double fNumber, int iSentence, int iWord, int iWordIndex, 
                 StanfordWord word){
    this.fNumber = fNumber;
    this.iSentence = iSentence;
    this.iWord = iWord;
    this.word = word;
    this.iWordIndex = iWordIndex;
    //this.ptNP = word.ptParent.findNP();
    this.wordNoun = findNoun();
    Misc.Assert(wordNoun != null);
  }

  StanfordWord findNoun(){
    if(word.isRoot()){
      StanfordWord wordSubj = word.getSubj(true);
      if(wordSubj == null){
        return this.word;
      } else {
        return wordSubj;
      }
    } else {
      return word.getParent();
    }
  }

  public boolean equals(Object obj){
    if(!(obj instanceof StanfordNumber)){
      return false;
    }
    StanfordNumber numberOther = (StanfordNumber) obj;
    return ((this.fNumber == numberOther.fNumber) &&
            (this.iSentence == numberOther.iSentence) &&
            (this.iWord == numberOther.iWord));
  }

  public int hashCode(){
    return (int) fNumber;
  }

  // nonref number
  StanfordNumber(double fNumber){
    this.fNumber = fNumber;
    this.iSentence = -1;
    this.iWord = -1;
    this.word = null;
  }

  boolean isNonRef(){
    return (iSentence == -1);
  }


  static List<StanfordNumber> findNumbers(StanfordDocument doc){
    List<StanfordNumber> lNumbers = new ArrayList<StanfordNumber>();
    for(StanfordSentence sentence : doc.lSentences){
      findNumbers(sentence, lNumbers);
    }
    if(lNumbers.size() > 0){
      StanfordNumber numberLargest = lNumbers.get(0);
      for(StanfordNumber numberCur : lNumbers){
        if(numberCur.fNumber > numberLargest.fNumber){
          numberLargest = numberCur;
        }
      }
      numberLargest.bIsLargest = true;
    }
    return lNumbers;
  }

  static Double parseDouble(String sDouble){
    try{
      NumberFormat nf = NumberFormat.getNumberInstance();
      Double doub = nf.parse(sDouble).doubleValue();
      return doub;
    } catch(java.text.ParseException ex){
      return null;
    }
  }

  static Double getNumber(String sWord){
    //check if it's a known word
    Double fWord = mNumbers.get(sWord);
    if(fWord != null){
      return fWord;
    }
    //don't allow 2nd 3rd, 4th, etc.
    if(sWord.endsWith("nd") || sWord.endsWith("rd") || sWord.endsWith("th")){
      return null;
    }

    return parseDouble(sWord);
  }

  static void findNumbers(StanfordSentence sentence, 
                          List<StanfordNumber> lNumbers){
    // tokenize
    int iNumWordsFound = 0;
    for(int iWord = 0; iWord < sentence.lWords.size(); iWord++){
      StanfordWord word = sentence.lWords.get(iWord);
      Double fWord = getNumber(word.sWord);
      if(fWord != null){
        StanfordNumber number = 
          new StanfordNumber(fWord,sentence.iIndex,iWord,iNumWordsFound,word);
        word.number = number;
        lNumbers.add(number);
        iNumWordsFound++;
      } 
    }
  }

  static List<Double> numbersToDoubles(List<StanfordNumber> lNumbers){
    List<Double> lDoubles = new ArrayList<Double>(lNumbers.size());
    for(StanfordNumber number : lNumbers){
      lDoubles.add(number.fNumber);
    }
    return lDoubles;
  }

  static List<Double> wordsToDoubles(List<StanfordWord> lNumberWords){
    List<Double> lDoubles = new ArrayList<Double>(lNumberWords.size());
    for(StanfordWord wordNumber : lNumberWords){
      Misc.Assert(wordNumber.number != null);
      lDoubles.add(wordNumber.number.fNumber);
    }
    return lDoubles;
  }

  public String toString(){
    // if(this.equals(ZERO)){
    //   return "NUMBER:ZERO";
    // } else if(this.equals(ONE)){
    //   return "NUMBER:ONE";
    // }
    // StringBuilder sb = new StringBuilder();
    // sb.append("<").append(fNumber).append(",").append(iSentence).append(",")
    //   .append(iWord).append(",").append(word).append(">");
    // return sb.toString();
    return Double.toString(fNumber);
  }

}