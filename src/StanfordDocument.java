import java.lang.*;
import java.util.*;

import org.w3c.dom.*;

class StanfordDocument{
  List<StanfordSentence> lSentences = new ArrayList<StanfordSentence>();
  List<StanfordCorefChain> lCorefChains = new ArrayList<StanfordCorefChain>();
  List<Double> lDoubles;
  List<StanfordNumber> lNumbers = new ArrayList<StanfordNumber>();
  List<StanfordWord> lNumberWords = new ArrayList<StanfordWord>();
  List<StanfordWord> lNouns = new ArrayList<StanfordWord>();
  double[] aAnswer;
  List<StanfordWord> lHowManyObjs;
 
  StanfordDocument(Node node){
    Misc.Assert(node.getNodeType() == Node.ELEMENT_NODE);
    Misc.Assert(node.getNodeName().equals("document"));
    processSentences(Misc.getFirstChildByName(node, "sentences"));
    if(Misc.hasChildByName(node, "coreference")){
      processCoref(Misc.getFirstChildByName(node, "coreference"));
    }
    lNumbers = StanfordNumber.findNumbers(this);
    findNounsAndNumberWords();
    Misc.Assert(lNumbers.size() == lNumberWords.size());
    lDoubles = StanfordNumber.numbersToDoubles(lNumbers);
    calcHowManyObjs();
  }
 
  void calcHowManyObjs(){
    this.lHowManyObjs = new ArrayList<StanfordWord>();
    for(StanfordSentence sentence : this.lSentences){
      if(sentence.sSentenceOrig.contains("How many") ||
         sentence.sSentenceOrig.contains("how many") ||
         sentence.sSentenceOrig.contains("How much") ||
         sentence.sSentenceOrig.contains("how much")){
        // now try to automatically find how many obj
        StanfordWord wordCurQuery = null;
        boolean bNounSearch = false;
        for(int iWord = 0; iWord < sentence.lWords.size(); iWord++){
          StanfordWord word = sentence.lWords.get(iWord);
          if(bNounSearch){
            if(word.isNoun()){
              wordCurQuery = word;
            } else if(!word.isPossessive() && (wordCurQuery != null)){
              //we've found the word
              lHowManyObjs.add(wordCurQuery);
              bNounSearch = false;
              wordCurQuery = null;
            }
          }
          if((word.sWordOrig != null) &&
             (word.sWordOrig.equals("How") || word.sWordOrig.equals("how"))){
            StanfordWord wordNext = sentence.lWords.get(iWord+1);
            if(wordNext.sWordOrig.equals("many") || 
               wordNext.sWordOrig.equals("much")){
              //skip the next word and turn on noun search
              iWord++;
              bNounSearch = true;
            }
          }
        }
      }
    }
  }


  void findNounsAndNumberWords(){
    for(StanfordSentence sentence : lSentences){
      for(StanfordWord word : sentence.lWords){
        if(word.isNoun()){
          sentence.lNouns.add(word);
          lNouns.add(word);
        }
        if(word.number != null){
          sentence.lNumberWords.add(word);
          sentence.lNumbers.add(word.number);
          sentence.lDoubles.add(word.number.fNumber);
          sentence.setDoubles.add(word.number.fNumber);
          lNumberWords.add(word);
        }
      }
    }
  }


  void processSentences(Node node){
    List<Node> lSentenceNodes = Misc.getAllChildrenByName(node, "sentence");
    for(int iSentence = 0; iSentence < lSentenceNodes.size(); iSentence++){
      lSentences.add(new StanfordSentence(lSentenceNodes.get(iSentence), 
                                          iSentence, this));
    }
  }

  void processCoref(Node node){
    List<Node> lCorefNodes = Misc.getAllChildrenByName(node, "coreference");
    for(Node nodeCoref : lCorefNodes){
      lCorefChains.add(new StanfordCorefChain(nodeCoref, lSentences));
    }
  }

  // void calcNumFeatures(StanfordNumber num, String sPos, FeatureSet featureset){
  //   // add a feature for the num itself
  //   if(num.isNonRef()){
  //     featureset.add(Arrays.asList("NUM",sPos,"NOREF:" + num.fNumber));
  //   } else {
  //     //add features combining sentence index and position
  //     featureset.add(Arrays.asList("SENTINDEX",sPos, num.iSentence));
  //     featureset.add(Arrays.asList("SENTINDEX",sPos, num.iSentence));
  //     featureset.add(Arrays.asList("SENTWORDINDEX",sPos, num.iSentence, 
  //                                  num.iWordIndex));
  //     StanfordWord wordCur = num.word;
  //     while(true){
  //     //add a feature for the word
  //       featureset.add(Arrays.asList("DEPPATHWORD",sPos,wordCur.sWord));
  //       featureset.add(Arrays.asList("DEPPATHPOS",sPos,wordCur.sPos));
  //       StanfordDep depParent = wordCur.depParent;
  //       Misc.Assert(depParent != null);
  //       if(!depParent.sType.equals("root")){
  //         featureset.add(Arrays.asList("DEPPATHDEP",sPos,depParent.sType));
  //         featureset.add(Arrays.asList("DEPPATHLINKWORD",sPos,wordCur.sWord,
  //                                      depParent.sType));
  //         featureset.add(Arrays.asList("DEPPATHWORDWORD",sPos,wordCur.sWord,
  //                                    depParent.wordParent.sWord));
  //         featureset.add(Arrays.asList("DEPPATHLINKPOS",sPos,wordCur.sPos,
  //                                      depParent.sType));
  //         featureset.add(Arrays.asList("DEPPATHPOSPOS",sPos,wordCur.sPos,
  //                                      depParent.wordParent.sPos));
  //         wordCur = depParent.wordParent;
  //       } else {
  //         // this should be the verb
  //         featureset.add(Arrays.asList("NUMVERB", sPos, wordCur.sWord));
  //         break;
  //       }
  //     }
  //   }
  // }

  StanfordWord findLowestCommonWord(StanfordWord word1, StanfordWord word2){
    // add all the word1 deps to the set
    Set<StanfordWord> setWords = new HashSet<StanfordWord>();
    setWords.add(word1);
    StanfordDep depCur = word1.depParent;
    while(depCur != null){
      setWords.add(depCur.wordParent);
      depCur = depCur.wordParent.depParent;
    }
    if(setWords.contains(word2)){
      return word2;
    } else {
      depCur = word2.depParent;
      while(!setWords.contains(depCur.wordParent)){
        depCur = depCur.wordParent.depParent;
        Misc.Assert(depCur != null);
      }      
      return depCur.wordParent;
    }
  }

  List<String> getDepPathToAncestor(StanfordWord word, StanfordWord wordAncestor){
    List<String> lPath = new ArrayList<String>();
    while(word != wordAncestor){
      lPath.add(word.depParent.sType);
      word = word.depParent.wordParent;
    }
    return lPath;
  }

  List<String> getDepPath(StanfordWord word1, StanfordWord word2, StanfordWord wordCommon){
    //first half is word1 up to common
    List<String> lPath = getDepPathToAncestor(word1, wordCommon);
    //second half is word2 down to common -- this will compute the reverse 
    List<String> lPathRight = getDepPathToAncestor(word2, wordCommon);
    Collections.reverse(lPathRight);
    lPath.addAll(lPathRight);
    return lPath;
  }

  // void calcNumPairFeatures(StanfordNumber num1, String sPos1, StanfordNumber num2,
  //                          String sPos2, FeatureSet featureset){
  //   // same sentence
  //   boolean bSameSent = (num1.iSentence == num2.iSentence) &&
  //     !num1.isNonRef() && !num2.isNonRef();

  //   featureset.add(Arrays.asList("SameSentence",sPos1,sPos2,bSameSent));
  //   //sentence index
  //   featureset.add(Arrays.asList("SentenceIndex",sPos1,num1.iSentence,
  //                               sPos2,num2.iSentence));
  //   featureset.add(Arrays.asList("SentenceIndexDiff",sPos1,sPos2, 
  //                                Math.abs(num2.iSentence-num1.iSentence)));
  //   if(bSameSent){
  //     featureset.add(Arrays.asList("IndexInSentence",sPos1, sPos2, num1.iWordIndex,
  //                                  num2.iWordIndex));
  //     // find the lowest common word in the dag
  //     StanfordWord wordCommon = findLowestCommonWord(num1.word, num2.word);
  //     List<String> lPath = getDepPath(num1.word, num2.word, wordCommon);
  //     featureset.add(Arrays.asList("DEPPATH", sPos1, sPos2, lPath));
  //     for(String sDep : lPath){
  //       featureset.add(Arrays.asList("DEPPATHCONTAINS", sPos1, sPos2, sDep));
  //     }
  //     featureset.add(Arrays.asList("COMMONWORD", sPos1, sPos2, 
  //                                  wordCommon.sWord));
  //     featureset.add(Arrays.asList("COMMONPOS", sPos1, sPos2, 
  //                                  wordCommon.sPos));
  //     featureset.add(Arrays.asList("SameSentenceOrder",sPos1,sPos2));
  //   }
  // }

  int getType(StanfordNumber num){
    if(num.isNonRef()){
      if(num.fNumber == 0.0){
        return 0;
      } else {
        Misc.Assert(num.fNumber == 1.0);
        return 1;
      }
    } else {
      return 2;
    }
  }

  // void calcFullEquationFeatures(List<StanfordNumber> lNum, FeatureSet featureset){
  //   StanfordNumber num0 = lNum.get(0);
  //   StanfordNumber num1 = lNum.get(1);
  //   StanfordNumber num2 = lNum.get(2);
  //   //calc triple features
  //   // -- for now just check if they're all in the same sentence
  //   boolean bSameSentence = (num0.iSentence == num1.iSentence) &&
  //     (num0.iSentence == num2.iSentence) &&
  //     !num0.isNonRef() && !num1.isNonRef() && !num2.isNonRef();
  //   featureset.add(Arrays.asList("TripleSameSentence",bSameSentence));
  //   if(bSameSentence){
  //     featureset.add(Arrays.asList("SameSentenceIndex",num0.iWordIndex,
  //                                  num1.iWordIndex, num2.iWordIndex));
  //   }

  //   // turn the numbers into categories
  //   int iTypeNum0 = getType(num0);
  //   int iTypeNum1 = getType(num1);
  //   int iTypeNum2 = getType(num2);
  //   featureset.add(Arrays.asList("TYPES",iTypeNum0,iTypeNum1,iTypeNum2));
  //   Integer iSentence = null;
  //   boolean bAllSame = true;
  //   boolean bAllRef = true;
  //   int iNum = 0;
  //   for(StanfordNumber num : lNum){
  //     if(!num.isNonRef()){
  //       if(iSentence == null){
  //         iSentence = num.iSentence;
  //       } else {
  //         if(iSentence != num.iSentence){
  //           bAllSame = false;
  //         }
  //       }
  //     } else {
  //       bAllRef = false;
  //     }
  //     iNum++;
  //   }

  //   if(bAllSame && (iSentence != null)){
  //     featureset.add(Arrays.asList("SAMESENT"));
  //     featureset.add(Arrays.asList("SAMESENT", iSentence));
  //     featureset.add(Arrays.asList("TYPES-SENTENCE",iTypeNum0,iTypeNum1,
  //                                  iTypeNum2, iSentence));
  //   }
  //   if(bAllRef){
  //     Misc.Assert(!num0.isNonRef());
  //     Misc.Assert(!num1.isNonRef());
  //     Misc.Assert(!num2.isNonRef());
  //     Misc.Assert(num0.word != null);
  //     Misc.Assert(num1.word != null);
  //     Misc.Assert(num2.word != null);
  //     if(num0.word.sWord.equals(num1.word.sWord) &&
  //        num0.word.sWord.equals(num2.word.sWord)){
  //       //all nonref and all words equal
  //       featureset.add(Arrays.asList("EQUAL-NOUNS"));
  //       featureset.add(Arrays.asList("EQUAL-NOUN-IS", num0.word.sWord));
  //     }
  //   }
  // }


  // void calcEquationFeatures(List<StanfordNumber> lNum, FeatureSet featureset){
  //   StanfordNumber num0 = lNum.get(0);
  //   StanfordNumber num1 = lNum.get(1);
  //   StanfordNumber num2 = lNum.get(2);

  //   calcNumFeatures(num0, "A", featureset);
  //   calcNumFeatures(num1, "A", featureset);
  //   calcNumFeatures(num2, "b", featureset);
  //   calcNumPairFeatures(num0, "A", num1, "A", featureset);
  //   calcNumPairFeatures(num0, "A", num2, "b", featureset);
  //   calcNumPairFeatures(num1, "A", num2, "b", featureset);
  //   calcFullEquationFeatures(lNum, featureset);
  // }

  // void calcVariableFeatures(StanfordNumber num1, StanfordNumber num2, String sType, 
  //                           FeatureSet featureset){
  //   //for now just calc if they use the same noun or not
  //   if(num1.isNonRef() || num2.isNonRef()){
  //     featureset.add(Arrays.asList("SAMENOUNNONREF", sType));
  //     if(num1.isNonRef() && num2.isNonRef()){
  //       featureset.add(Arrays.asList("BOTHNOUNNONREF", sType));
  //     }
  //   } else {
  //     String sNoun1 = num1.word.depParent.wordParent.sWord;
  //     String sNoun2 = num2.word.depParent.wordParent.sWord;
  //     boolean bCoref = sNoun1.equals(sNoun2);
  //     featureset.add(Arrays.asList("SAMENOUN:",sType, bCoref));
  //   }
  // }

  // void calcAllNumFeatures(List<StanfordNumber> lNum1, List<StanfordNumber> lNum2, 
  //                         FeatureSet featureset){
  //   if(lNumbers.size() <= 6){
  //     //do we use all the numbers if we can
  //     int iNumRef = 0;
  //     for(StanfordNumber num : lNum1){
  //       if(!num.isNonRef()){
  //         iNumRef++;
  //       }
  //     }
  //     for(StanfordNumber num : lNum2){
  //       if(!num.isNonRef()){
  //         iNumRef++;
  //       }
  //     }
  //     if(iNumRef == lNumbers.size()){
  //       featureset.add(Arrays.asList("USEDALLNUMBERS"));
  //     } else {
  //       featureset.add(Arrays.asList("DIDNTUSEALLNUMBERS"));
  //     }
  //   }
  // }

  // FeatureSet calcFeatures(List<StanfordNumber> lNum1, List<StanfordNumber> lNum2, 
  //                         Features features){
  //   // independant per number features
  //   FeatureSet featureset = new FeatureSet(features);
  //   calcEquationFeatures(lNum1, featureset);
  //   calcEquationFeatures(lNum2, featureset);
  //   calcVariableFeatures(lNum1.get(0), lNum2.get(0), "PosA", featureset);
  //   calcVariableFeatures(lNum1.get(1), lNum2.get(1), "PosA", featureset);
  //   calcVariableFeatures(lNum1.get(2), lNum2.get(2), "Posb", featureset);
  //   calcAllNumFeatures(lNum1, lNum2, featureset);
  //   //featureset.add(Arrays.asList("EXACT",new ArrayList(lNum1),
  //   //                             new ArrayList(lNum2)));
  //   // not calcing difference features -- I'll just enforce diff numbers used
  //   // need to think about what happens when the numbers are 1 or 0
  //   return featureset;
  // }

  void print(){
    for(StanfordSentence sentence : this.lSentences){
      System.out.println("Sentence: " + sentence.sSentence);
      System.out.println("SentenceOrig: " + sentence.sSentenceOrig);
    }
    for(StanfordCorefChain chain : this.lCorefChains){
      StringBuilder sb = new StringBuilder();
      sb.append("Coref:");
      for(StanfordMention mention : chain.lMentions){
        sb.append(" ").append(mention.iSentence).append("-")
          .append(mention.word.sWord).append("-").append(mention.iHead);
      }
      System.out.println(sb.toString());
    }
    System.out.println("Numbers: " + this.lNumbers);
  }


  // FeatureSet calcFeatures(List<Number> lNum1, List<Number> lNum2, 
  //                         Features features){
  //   //we'll just add a feature for the equation itself
  //   FeatureSet featureset = new FeatureSet(features);
  //   featureset.add(Arrays.asList("EXACT",new ArrayList(lNum1),
  //                                new ArrayList(lNum2)));
  //   return featureset;
  // }

  public String toString(){
    return toString(false);
  }

  public String toString(boolean bIncludeIndexes){
    // we'll just print this as a paragraph for now
    StringBuilder sb = new StringBuilder();
    for(int iSentence = 0; iSentence < lSentences.size(); iSentence++){
      if(iSentence != 0){
        sb.append("\n");
      }
      if(bIncludeIndexes){
        sb.append("Sent:").append(iSentence).append(" ");
      }
      lSentences.get(iSentence).toStringBuilder(sb, bIncludeIndexes);
    }
    return sb.toString();
  }

  public String toStringOneLine(){
    return toStringBuilderOneLine(new StringBuilder()).toString();
  }

  public StringBuilder toStringBuilderOneLine(StringBuilder sb){
    for(StanfordSentence sentence : this.lSentences){
      sentence.toStringBuilder(sb, false);
      sb.append(" ");
    }
    return sb;
  }


}