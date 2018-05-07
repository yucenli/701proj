import java.lang.*;
import java.util.*;
import java.util.concurrent.*;

import com.google.common.collect.*;
import gnu.trove.list.*;
import gnu.trove.list.array.*;
import org.apache.commons.collections4.CollectionUtils;

class DerivationFeatures{
  Features features;
  Map<Object,FeatureList> mFeatureListCache;// = 
  //new ConcurrentHashMap<Object,FeatureList>();
  Map<Object,List<Object>> mFeatureCache;// = 
  //new ConcurrentHashMap<Object,List<Object>>();

  MemCache memcache = new MemCache();

  DerivationFeatures(Features features){
    this.features = features;
  }

  Map<Object,Object> mCommonFeatures;

  void computeCommonFeatures(List<Question> lQuestions){
    Map<Object,Integer> mMaxFeatureCounts = new HashMap<Object,Integer>();
    CountMap<Object> mFeatureCounts = new CountMap<Object>();
    for(Question question : lQuestions){
      CountMap<Object> mCurQuestionCounts = new CountMap<Object>();
      Set<Object> setCurQuestion = new HashSet<Object>();
      //get all the words
      List<StanfordWord> lSlotWords = new ArrayList<StanfordWord>();
      lSlotWords.addAll(question.doc.lNouns);
      int iNumNouns = question.doc.lNouns.size();
      for(StanfordWord wordNumber : question.doc.lNumberWords){
        lSlotWords.add(wordNumber);
      }
      for(int iWord1 = 0; iWord1 < lSlotWords.size(); iWord1++){
        StanfordWord word1 = lSlotWords.get(iWord1);
        boolean bNumber1 = (iWord1 >= iNumNouns);
        List<Object> lWordFeatures = new ArrayList<Object>();
        StanfordNumber number1 = null;
        if(bNumber1){
          number1 = word1.number;
          word1 = word1.number.wordNoun;
          lWordFeatures.addAll(getNumberFeatures(number1));
        }
        lWordFeatures.addAll(getGeneralWordFeatures(word1));
        mCurQuestionCounts.incrementAll(lWordFeatures);
        setCurQuestion.addAll(lWordFeatures);
        for(int iWord2 = 0; iWord2 < lSlotWords.size(); iWord2++){
          boolean bNumber2 = (iWord2 >= iNumNouns);
          StanfordWord word2 = lSlotWords.get(iWord2);
          List<Object> lWordPairFeatures = new ArrayList<Object>();
          StanfordNumber number2 = null;
          if(bNumber2){
            number2 = word2.number;
            word2 = word2.number.wordNoun;
            if(bNumber1){
              lWordPairFeatures.addAll(getNumberFeatures(number1, number2));
            }
          }
          lWordPairFeatures.addAll(getWordOverlapFeatures(word1, word2));
          mCurQuestionCounts.incrementAll(lWordPairFeatures);
          setCurQuestion.addAll(lWordPairFeatures);
        }
      }
      for(Object oFeature : setCurQuestion){
        mFeatureCounts.increment(oFeature);
      }
      for(Map.Entry<Object,Integer> entry : mCurQuestionCounts.entrySet()){
        Integer iCur = mMaxFeatureCounts.get(entry.getKey());
        if((iCur == null) || (entry.getValue() > iCur)){
          mMaxFeatureCounts.put(entry.getKey(), entry.getValue());
        }   
      }
    }
    mCommonFeatures = new HashMap<Object,Object>();
    for(Map.Entry<Object,Integer> entry : mFeatureCounts.entrySet()){
      if((entry.getValue() > Config.config.iMinCommonFeatureCount) ||
         Config.config.bTestOneQuestion){
        Object oFeature = entry.getKey();
        mCommonFeatures.put(oFeature,oFeature);
      }
    }
    //calc the fudge factors
    Map<Object,Double> mFudgeFactors = new ConcurrentHashMap<Object,Double>();
    for(Map.Entry<Object,Integer> entry : mMaxFeatureCounts.entrySet()){
      mFudgeFactors.put(entry.getKey(), Misc.div(1, entry.getValue()));
    }
    Model.features.mFudgeFactors = mFudgeFactors;
  }

  void resetCache(){
    //reset it instead of rescoring, 
    // because we may not need many of the cache entries
    mFeatureListCache = null;//new ConcurrentHashMap<Object,FeatureList>();
    //remove everything that hasn't been used since the last iteration
    // and reset the crossprod on the rest
    // Iterator<Map.Entry<Object, FeatureList>> iter = 
    //   mFeatureListCache.entrySet().iterator();
    // while(iter.hasNext()){
    //   Map.Entry<Object, FeatureList> entry = iter.next();
    //   FeatureList featurelist = entry.getValue();
    //   if(!featurelist.hasCrossProd()) {
    //     iter.remove();
    //   } else {
    //     //clear it's crossprod so it will get recomputed for this iter
    //     featurelist.reset();
    //   }
    // }
  }

  FeatureSet getFeatureSet(EquationDerivation der){
    // FeatureSet fs = new FeatureSet(false);
    // String sEquation = der.equation.toExactString();
    // List<StanfordWord> lNumberNouns = getNumberWordNouns(der.lNumbers);
    // addWordOverlapFeatures(fs, sEquation, der.lUnknowns, lNumberNouns);
    // addUnknownWordFeatures(fs, sEquation, der.lUnknowns);
    // addNumberWordFeatures(fs, sEquation, lNumberNouns);
    // return fs;
    //getFeatures(der);
    return null;
  }

  FeatureListList getDebugFeatures(FilledTerm ft){
    FeatureListList fll = new FeatureListList();
    //add features for individual words connected to a mappedterm
    for(int iSlot1 = 0; iSlot1 < ft.mt.numUniqueSlots(); iSlot1++){
      StanfordWord word1 = ft.getUniqueSlotNoun(iSlot1);
      //lets build a feature with the word, crossed with the slot signtures
      String sSlotSignature1;
      if(Config.config.bUseNonCanonicalSlotSigs){
        sSlotSignature1 = ft.mt.aSimpleSlotSignatures[iSlot1];
      } else {
        sSlotSignature1 = ft.mt.aUniqueSlotSignatures[iSlot1];
      }
      Object oKey1 = memcache.get(word1, sSlotSignature1);
      int iFeature1 = features.getFeatureIndex(oKey1);
      addSingleFeature(fll, iFeature1);
      for(int iSlot2 = 0; iSlot2 < ft.mt.numUniqueSlots(); iSlot2++){
        if(iSlot2 == iSlot1){
          continue;
        }
        StanfordWord word2 = ft.getUniqueSlotNoun(iSlot2);
        String sSlotSignature2;
        if(Config.config.bUseNonCanonicalSlotSigs){
          sSlotSignature2 =ft.mt.aSimpleSlotPairSignatures[iSlot1][iSlot2];
        } else {
          sSlotSignature2 =ft.mt.aUniqueSlotPairSignatures[iSlot1][iSlot2];
        }
        Object oKey2 = memcache.get(word1, word2, sSlotSignature2);
        int iFeature2 = features.getFeatureIndex(oKey2);
        addSingleFeature(fll, iFeature2);
      }
    }
    return fll;
  }

  void addEquationFeatures(FeatureListList fll, FilledTerm ft){
    //a. the full system
    if(Config.config.bUseNonCanonicalSlotSigs){
      addSingleFeature(fll, 
                       features.getFeatureIndex(ft.mt.toString(false,true)));
    }
    if(Config.config.bUseCanonicalSlotSigs){
      addSingleFeature(fll, features.getFeatureIndex(ft.mt.getSignature()));
    }
    //b. each equation in the system
    Misc.Assert(ft.mt.term.type == Term.Type.Complex);
    if(Config.config.bUseSubTermSigs && ft.mt.term.lSubTerms.size() > 1){
      for(MappedTerm mtSub : ft.mt.term.lSubTerms){
        if(Config.config.bUseNonCanonicalSlotSigs){
          int iFeature = 
            features.getFeatureIndexFudgeFactor(mtSub.toString(false,true),
                                                0.3333);
          addSingleFeature(fll, iFeature);
        }
        if(Config.config.bUseCanonicalSlotSigs){
          int iFeature = 
            features.getFeatureIndexFudgeFactor(mtSub.getSignature(), 0.3333);
          addSingleFeature(fll, iFeature);
        }
      }
    }
  }

  // boolean samePrep(List<StanfordWord> lWords1, List<StanfordWord> lWords2){
  //   if((lWords1.size() == 1) && (lWords2.size() ==1)){
  //     if(lWords1.get(0).sWord.equals(lWords2.get(0).sWord)){
  //       return true;
  //     }
  //   }
  //   System.out.println("Not same prep: " + lWords1 + " " + lWords2);
  //   return false;
  // }


  List<String> toStringsWithPos(List<StanfordWord> lWords){
    List<String> lStrings = new ArrayList<String>();
    for(StanfordWord word : lWords){
      lStrings.add(word + "/" + word.sPos);
    }
    return lStrings;
  }

  Set<String> findOneType(List<StanfordWord> lWords, boolean bVerb){
    Set<String> setWords = new HashSet<String>();
    for(StanfordWord wordCur : lWords){
      if(wordCur.sWord.equals(",") || wordCur.sWord.equals("if")){
        // for now comma and if break this dependency structure
        return setWords;
      }
      if((bVerb && wordCur.isVerb()) || (!bVerb && wordCur.isPrep())){
        setWords.add(wordCur.sWord);
      }
    }
    return setWords;
  }


  boolean sameType(List<StanfordWord> lWords1, List<StanfordWord> lWords2,
                   boolean bVerb){
    Set<String> setType1 = findOneType(lWords1, bVerb);
    Set<String> setType2 = findOneType(lWords2, bVerb);
    if(CollectionUtils.intersection(setType1, setType2).size() != 0){
      return true;
    }
    // String sType = bVerb ? "VERB" : "PREP";
    // System.out.println("Not same " +  sType + ": " + setType1 + " " 
    //                   + setType2 + " " + toStringsWithPos(lWords1) 
    //                   + " " + toStringsWithPos(lWords2));
    return false;
  }

  void addEqualStemFeature(FeatureListList fll, FilledTerm ft, int iWord1,
                           int iWord2){
    StanfordWord word0 = ft.getUniqueSlotNoun(iWord1);
    StanfordWord word1 = ft.getUniqueSlotNoun(iWord2);
    if((word0 == null) || (word1 == null)){
      return;
    }
    boolean bEqual = word0.sWord.equals(word1.sWord);
    Object oFeature = Arrays.asList("SAMESTEM" + iWord1 + iWord2, bEqual);
    addSingleFeature(fll, features.getFeatureIndex(oFeature));
  }

  List<StanfordWord> getWordPath(StanfordWord word1, StanfordWord word2){
    Misc.Assert(word1.sentence.iIndex == word2.sentence.iIndex);
    Misc.Assert(word1.iIndex != word2.iIndex);
    int iLowIndex = Math.min(word1.iIndex, word2.iIndex);
    int iHighIndex = Math.max(word1.iIndex, word2.iIndex);
    return word1.sentence.lWords.subList(iLowIndex+1, iHighIndex);
  }

  boolean wordPathContainsAnd(StanfordWord word1, StanfordWord word2){
    if((word1.sentence.iIndex != word2.sentence.iIndex) ||
       (word1.iIndex == word2.iIndex)){
      return false;
    }
    List<StanfordWord> lPath = getWordPath(word1, word2);
    for(StanfordWord word : lPath){
      if(word.sWordOrig.equals("and") || word.sWordOrig.equals("or")){
        return true;
      }
    }
    return false;
  }

  void addEqualPrepObjToObj(FeatureListList fll,
                            StanfordWord wordPrepObjParent,
                            StanfordWord wordObj, String sFeatureName){
    if((wordPrepObjParent == null) || (wordObj == null)){
      return;
    }

    for(StanfordWord wordPrepObj : wordPrepObjParent.getPrepObjs()){
      if(wordPrepObj.sWord.equals(wordObj.sWord)){
        addSingleFeature(fll, features.getFeatureIndex(sFeatureName));
        return;
      }
    }
  }

  void addQuestionSentenceFeatures(FeatureListList fll, FilledTerm ft){
    for(int iSlot = 0; iSlot < 8; iSlot++){
      StanfordWord wordCur = ft.getUniqueSlotNoun(iSlot);
      if(wordCur == null){
        continue;
      }
      if(wordCur.sentence.bIsQuestionSentence){
        addSingleFeature(fll, 
                         features.getFeatureIndex("QUESTIONSENTENCE"+iSlot));
      }
    }
  }

  static List<StanfordWord> getHowManyObjs(StanfordDocument doc){
    List<StanfordWord> lHowManyObjs = new ArrayList<StanfordWord>();
    for(StanfordSentence sentence : doc.lSentences){
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
    return lHowManyObjs;
  } 

  void addHowManyFeature(FeatureListList fll, StanfordWord word, 
                         List<StanfordWord> lHowManyObjs, boolean bExact,
                         String sFeatureName){
    if(word == null){
      return;
    }
    for(StanfordWord wordHowManyObj : lHowManyObjs){
      boolean bExactlyEqual = (wordHowManyObj == word);
      boolean bStringEqual = wordHowManyObj.sWord.equals(word.sWord);
      //explictly disallow exact equality when we're looking for string 
      // equality
      if((bExact && bExactlyEqual) ||
         (!bExact && bStringEqual && !bExactlyEqual)){
        addSingleFeature(fll, features.getFeatureIndex(sFeatureName));
        return;
      }
    }
  }

  void addHowManyFeatures(FeatureListList fll, FilledTerm ft,Question question){
    List<StanfordWord> lHowManyObjs = getHowManyObjs(question.doc);
    //check for overlap with the unknowns (0-3) as well as slot 7
    addHowManyFeature(fll, ft.getUniqueSlotNoun(0), lHowManyObjs, false,
                      "HowManyOverlap0");
    addHowManyFeature(fll, ft.getUniqueSlotNoun(1), lHowManyObjs, false,
                      "HowManyOverlap1");
    addHowManyFeature(fll, ft.getUniqueSlotNoun(2), lHowManyObjs, false,
                      "HowManyOverlap2");
    addHowManyFeature(fll, ft.getUniqueSlotNoun(3), lHowManyObjs, false,
                      "HowManyOverlap3");
    addHowManyFeature(fll, ft.getUniqueSlotNoun(7), lHowManyObjs, false,
                      "HowManyOverlap7");
    for(int iSlot = 0; iSlot < 7; iSlot++){
      addHowManyFeature(fll, ft.getUniqueSlotNoun(iSlot), lHowManyObjs, true,
                        "IsHowManyObj" + iSlot);
    }
  }


  void addIsPrepObjOfObj(FeatureListList fll, StanfordWord wordPrepObj, 
                         StanfordWord wordObj, String sFeatureName){
    if((wordPrepObj == null) || (wordObj == null)){
      return;
    }
    for(StanfordWord wordPrepObjCur : wordObj.getPrepObjs()){
      if(wordPrepObjCur == wordPrepObj){
        addSingleFeature(fll, features.getFeatureIndex(sFeatureName));
        return;
      }
    }
  }

  void addNearEachOtherFeatures(FeatureListList fll,
                                StanfordWord word1, StanfordWord word2,
                                String sFeatureName){
    if((word1 == null) || (word2 == null) || 
       (word1.sentence.iIndex != word2.sentence.iIndex)){
      return;
    }
    if(Math.abs(word1.iIndex-word2.iIndex) < 5){
      addSingleFeature(fll, features.getFeatureIndex(sFeatureName));
    }
  }

  FeatureListList getNewFeaturesSimple(FilledTerm ft, Question question){
    FeatureListList fll = new FeatureListList();
    addSingleFeature(fll, features.getFeatureIndex("OFFSET"));
    //we want to look at unknown 0, number 0 --> slot 0, slot4 & slot 2, slot5
    StanfordWord word0 = ft.getUniqueSlotNoun(0);
    StanfordWord word1 = ft.getUniqueSlotNoun(1);
    StanfordWord word2 = ft.getUniqueSlotNoun(2);
    StanfordWord word3 = ft.getUniqueSlotNoun(3);
    StanfordWord word4 = ft.getUniqueSlotNoun(4);
    StanfordWord word5 = ft.getUniqueSlotNoun(5);
    StanfordWord word6 = ft.getUniqueSlotNoun(6);
    StanfordWord word7 = ft.getUniqueSlotNoun(7);
    if((word0 != null) && (word1 != null) && (word4 != null) &&
       (word5 != null)){
      if((word0.sentence.iIndex == word4.sentence.iIndex) &&
         (word1.sentence.iIndex == word5.sentence.iIndex) && 
         (word0.iIndex != word4.iIndex) && 
         (word1.iIndex != word5.iIndex)){
        if(Config.config.bUseRelationFeature){
          List<StanfordWord> lWords1 = getWordPath(word0, word4);
          List<StanfordWord> lWords2 = getWordPath(word1, word5);
          boolean bSameVerb = sameType(lWords1, lWords2, true);
          boolean bSamePrep = sameType(lWords1, lWords2, false);
          if(bSameVerb || bSamePrep){
            addSingleFeature(fll,features.getFeatureIndex("SAME-VERB-OR-PREP"));
          }
        }
      }
      //check for and separating some things
      boolean bAnd04 = wordPathContainsAnd(word0, word4);
      boolean bAnd15 = wordPathContainsAnd(word1, word5);
      boolean bAnd01 = wordPathContainsAnd(word0, word1);
      boolean bAnd45 = wordPathContainsAnd(word4, word5);
      if(!bAnd04 && !bAnd15 && bAnd01 && bAnd45){
        addSingleFeature(fll, features.getFeatureIndex("CONJ-BETWEEN"));
      }
    }

    //now check for stem equality between all numbers in first equation
    addEqualStemFeature(fll, ft, 4,5);
    addEqualStemFeature(fll, ft, 4,6);
    addEqualStemFeature(fll, ft, 5,6);
    // as well as the unknowns and the number in the second equation
    addEqualStemFeature(fll, ft, 0,1);
    addEqualStemFeature(fll, ft, 2,3);
    addEqualStemFeature(fll, ft, 2,7);
    addEqualStemFeature(fll, ft, 3,7);
    //as well as the two sums like rainstorm question 474
    addEqualStemFeature(fll, ft, 6,7);
    //check for word equality/nonequality of the variables
    Object oFeature02 = Arrays.asList("SAMEWORD02", (word0 == word2));
    Object oFeature13 = Arrays.asList("SAMEWORD13", (word1 == word3));
    addSingleFeature(fll, features.getFeatureIndex(oFeature02));
    addSingleFeature(fll, features.getFeatureIndex(oFeature13));
    Object oFeature01 = Arrays.asList("DIFFWORD01", (word0 != word1));
    Object oFeature03 = Arrays.asList("DIFFWORD03", (word0 != word3));
    Object oFeature12 = Arrays.asList("DIFFWORD12", (word1 != word2));
    Object oFeature23 = Arrays.asList("DIFFWORD23", (word2 != word3));
    addSingleFeature(fll, features.getFeatureIndex(oFeature01));
    addSingleFeature(fll, features.getFeatureIndex(oFeature03));
    addSingleFeature(fll, features.getFeatureIndex(oFeature12));
    addSingleFeature(fll, features.getFeatureIndex(oFeature23));

    //check if the prepobjs match, either prepobj 7 with obj 2,3
    // or prepobj 0 with obj 4 or prepobj 1 with obj 5 or vice versa
    addEqualPrepObjToObj(fll, word7, word2, "EQPREPOBJ72");
    addEqualPrepObjToObj(fll, word7, word3, "EQPREPOBJ73");
    addEqualPrepObjToObj(fll, word0, word4, "EQPREPOBJ04");
    addEqualPrepObjToObj(fll, word1, word5, "EQPREPOBJ15");
    addEqualPrepObjToObj(fll, word4, word0, "EQPREPOBJ40");
    addEqualPrepObjToObj(fll, word5, word1, "EQPREPOBJ51");
    addEqualPrepObjToObj(fll, word7, word4, "EQPREPOBJ74");
    addEqualPrepObjToObj(fll, word7, word5, "EQPREPOBJ75");
    //addquestion sentence features
    addQuestionSentenceFeatures(fll, ft);
    addIsPrepObjOfObj(fll, word6, word7, "ISPREPOBJ67");
    addIsPrepObjOfObj(fll, word7, word6, "ISPREPOBJ76");
    //add the near each other features
    addNearEachOtherFeatures(fll, word6, word7, "ISNEAR67");
    // how many features
    addHowManyFeatures(fll, ft, question);
    return fll;
  }    

  FeatureListList getSimpleFeatures(FilledTerm ft, Question question, 
                                    FeatureCache cache, 
                                    List<SimpleSolver> lSolvers){
    FeatureListList fll = new FeatureListList();
    if(Config.config.bUseOffsetFeature){
      addSingleFeature(fll, features.getFeatureIndex("OFFSET"), cache);
    }
    
    if(Config.config.bUseDocumentFeatures){
      fll.add(getCachedDocFeatureList(ft, question, cache));
    }
    if(ft.isFilled()){
      addSolutionFeatures(fll, ft, lSolvers, cache);
    }
    for(int iSlot1 = 0; iSlot1 < ft.mt.numFilledTermSlots(); iSlot1++){
      Slot slot1 = ft.getSlot(iSlot1);
      if(ft.isSlotEmpty(iSlot1)){
        continue;
      }
      if(!slot1.isNumber() && !Config.config.bUseUnknownFeatures){
        continue;
      }
      if(Config.config.bUseSingleFeatures){
        if((slot1.fNearbyConstant != null) && slot1.isNumber()){
          //lexicalize this
          Object oFeature = Arrays.asList("NEARBY-CONSTANT-NUMBER", 
                                          slot1.wordNoun.sWord, 
                                          slot1.fNearbyConstant);
          addSingleFeature(fll, features.getFeatureIndex(oFeature),cache);
        }
        //single word features
        fll.add(getCachedSimpleWordFeatureList(ft, iSlot1, question, cache));
      }
      if(Config.config.bUsePairFeatures){
        for(int iSlot2 = iSlot1+1; iSlot2<ft.mt.numFilledTermSlots(); iSlot2++){
          Slot slot2 = ft.getSlot(iSlot2);
          if(ft.isSlotEmpty(iSlot2)){
            continue;
          }
          if(!slot2.isNumber() && !Config.config.bUseUnknownFeatures){
            continue;
          }
          //word pair features
          fll.add(getCachedSimpleWordPairFeatureList(ft, iSlot1, iSlot2, cache));
          //two members of a pair must be in the same sentence, but not same word
          if((slot1.wordNoun.sentence.iIndex != slot2.wordNoun.sentence.iIndex)||
             (slot1.wordNoun.iIndex == slot2.wordNoun.iIndex)){
            continue;
          }
          //now look for another distict word pair
          if(Config.config.bUseRelationFeature){
            for(int iSlot3=iSlot1+1;iSlot3<ft.mt.numFilledTermSlots();iSlot3++){
              Slot slot3 = ft.getSlot(iSlot3);
              if((iSlot3 == iSlot2) ||ft.isSlotEmpty(iSlot3)){
                continue;
              }
              if(!slot3.isNumber() && !Config.config.bUseUnknownFeatures){
                continue;
              }

              for(int iSlot4 = iSlot3+1 ; iSlot4<ft.mt.numFilledTermSlots();
                  iSlot4++){
                Slot slot4 = ft.getSlot(iSlot4);
                if((iSlot4 == iSlot2) || ft.isSlotEmpty(iSlot4)){
                  continue;
                }
                if(!slot4.isNumber() && !Config.config.bUseUnknownFeatures){
                  continue;
                }
                //two members of a pair must be in same sentence,
                //but not same word
                if((slot3.wordNoun.sentence.iIndex != 
                    slot4.wordNoun.sentence.iIndex)
                   || (slot3.wordNoun.iIndex == slot4.wordNoun.iIndex)){
                  continue;
                }
                //quad features
                if(Config.config.bRelationshipFeature){
                  if(equalRelations(slot1, slot2, slot3, slot4)){
                    Object oFeature = 
                      Arrays.asList("SAME-RELATIONSHIP",
                                    ft.mt.toString(false,true),
                                    iSlot1,iSlot2,
                                    iSlot3,iSlot4);
                    addSingleFeature(fll, features.getFeatureIndex(oFeature),
                                     cache);
                  }
                }
              }
            }
          }
        }
      }
    }
    return fll;
  }

  void addSolutionFeatures(FeatureListList fll, FilledTerm ft,
                           List<SimpleSolver> lSolvers, 
                           FeatureCache cache){
    List<Double> lCalced = ft.solve(lSolvers);
    if(lCalced == null){
      //addSingleFeature(fll, features.getFeatureIndex("UNSOLVABLE"),cache);
      return;
    }
    boolean bRound = true;
    boolean bPositive = true;
    boolean bRoundAndPositive = true;
    for(Double fCalced : lCalced){
      if(fCalced <= 0){
        bPositive = false;
        bRoundAndPositive = false;
      }
      if(!Misc.isInteger(fCalced)){
        bRound = false;
        bRoundAndPositive = false;
      }
    }
    Object oRound = Arrays.asList("ROUND", bRound);
    Object oPositive = Arrays.asList("POSITIVE", bPositive);
    Object oRoundAndPositive = Arrays.asList("ROUNDANDPOS", bRoundAndPositive);
    if(Config.config.bRoundSolutionsFeature){
      addSingleFeature(fll, features.getFeatureIndex(oRound),cache);
    }
    if(Config.config.bPosSolutionsFeature){
      addSingleFeature(fll, features.getFeatureIndex(oPositive),cache);
    }
    if(Config.config.bRoundSolutionsFeature && 
       Config.config.bPosSolutionsFeature){
      addSingleFeature(fll, features.getFeatureIndex(oRoundAndPositive),cache);
    }
  }

  FeatureList getCachedDocFeatureList(FilledTerm ft, Question question,
                                           FeatureCache cache){
    String sSystemSig = ft.mt.toString(false,true);
    Object oSlotKey = Arrays.asList(sSystemSig, question.iIndex);
    FeatureList featurelist = cache.mFeatureListCache.get(oSlotKey);
    if(featurelist == null){
      List<Object> lDocFeatures =getCachedSimpleDocFeatures(question,cache);
      //now cross every feature with the slot
      int[] aFeatures = 
        features.crossFeaturesAsArray(sSystemSig, lDocFeatures);
      featurelist = new FeatureList(aFeatures);
      cache.mFeatureListCache.put(oSlotKey, featurelist);
    }
    return featurelist;
  }

  FeatureList getCachedSimpleWordFeatureList(FilledTerm ft, int iSlot,
                                             Question question,
                                             FeatureCache cache){
    Slot slot = ft.getSlot(iSlot);
    String sSlotSignature = ft.mt.aSimpleSlotSignatures[iSlot];
    Object oSlotKey = Arrays.asList(sSlotSignature, slot);
    FeatureList featurelist = cache.mFeatureListCache.get(oSlotKey);
    if(featurelist == null){
      List<Object> lWordFeatures = 
        getCachedSimpleWordFeatures(slot, question, cache);
      List<Object> lSlotSignatures = ft.mt.getSlotSignatures(iSlot);
      //now cross every feature with the slot
      int[] aFeatures = 
        features.doubleCrossFeaturesAsArray(lWordFeatures, lSlotSignatures);
      featurelist = new FeatureList(aFeatures);
      cache.mFeatureListCache.put(oSlotKey, featurelist);
    }
    return featurelist;
  }

  List<Object> getCachedSimpleDocFeatures(Question question,FeatureCache cache){
    List<Object> lDocFeatures = cache.mFeatureCache.get(question.iIndex);
    if(lDocFeatures == null){
      lDocFeatures = getSimpleDocFeatures(question);
      cache.mFeatureCache.put(question.iIndex, lDocFeatures);
    }
    return lDocFeatures;
  }

  List<Object> getCachedSimpleWordFeatures(Slot slot, Question question,
                                           FeatureCache cache){
    List<Object> lWordFeatures = cache.mFeatureCache.get(slot);
    if(lWordFeatures == null){
      lWordFeatures = getSimpleSlotFeatures(slot, question);
      cache.mFeatureCache.put(slot, lWordFeatures);
    }
    return lWordFeatures;
  }

  FeatureList getCachedSimpleWordPairFeatureList(FilledTerm ft, int iSlot1,
                                                 int iSlot2, 
                                                 FeatureCache cache){
    Slot slot1 = ft.getSlot(iSlot1);
    Slot slot2 = ft.getSlot(iSlot2);
    String sSlotSignature = ft.mt.aSimpleSlotPairSignatures[iSlot1][iSlot2];
    Object oSlotKey = Arrays.asList(sSlotSignature, slot1, slot2);
    FeatureList featurelist = cache.mFeatureListCache.get(oSlotKey);
    if(featurelist == null){
      List<Object> lWordFeatures = getCachedWordFeatures(slot1, slot2, cache);
      List<Object> lSlotSignatures = ft.mt.getSlotSignatures(iSlot1, iSlot2);
      //now cross every feature with the slot
      int[] aFeatures = 
        features.doubleCrossFeaturesAsArray(lWordFeatures, lSlotSignatures);
      featurelist = new FeatureList(aFeatures);
      cache.mFeatureListCache.put(oSlotKey, featurelist);
    }
    return featurelist;
  }

  List<Object> getCachedWordFeatures(Slot slot1, Slot slot2,FeatureCache cache){
    Object oWordKey = Arrays.asList(slot1, slot2);
    List<Object> lWordFeatures = cache.mFeatureCache.get(oWordKey);
    if(lWordFeatures == null){
      lWordFeatures = getSimpleSlotPairFeatures(slot1, slot2);
      cache.mFeatureCache.put(oWordKey, lWordFeatures);
    }
    return lWordFeatures;
  }

  void addHowManyFeatures(Slot slot, Question question, List<Object> lFeatures){
    boolean bAddedExactlyEqual = false;
    boolean bAddedStringEqual = false;
    for(StanfordWord wordHowManyObj : question.doc.lHowManyObjs){
      boolean bExactlyEqual = (wordHowManyObj == slot.wordNoun);
      boolean bStringEqual = wordHowManyObj.sWord.equals(slot.wordNoun.sWord);
      // equality
      if(!bAddedExactlyEqual && bExactlyEqual){
        lFeatures.add("HowManyObj");
        bAddedExactlyEqual = true;
      } else if (!bAddedStringEqual && bStringEqual && !bExactlyEqual){
        //explictly disallow exact equality when we're looking for string 
        lFeatures.add("StemEqualToHowManyObj");
        bAddedStringEqual = true;
      }
    }
  }    

  List<Object> getSimpleDocFeatures(Question question){
    List<Object> lFeatures = new ArrayList<Object>();
    lFeatures.add("");//add a dummy feature to just learn the weight of the sig
    if(Config.config.bNGramFeatures){
      lFeatures.addAll(question.lNGrams);
    }
    return lFeatures;
  }


  List<Object> getSimpleSlotFeatures(Slot slot, Question question){
    List<Object> lFeatures = new ArrayList<Object>();
    addHowManyFeatures(slot, question, lFeatures);
    if(slot.wordNoun.sentence.bIsQuestionSentence){
      lFeatures.add("QUESTION-SENTENCE");
    }
    if(slot.isNumber()){
      if((slot.wordNumber.number.fNumber == 1.0) ||
         (slot.wordNumber.number.fNumber == 2.0)){
        lFeatures.add("IS-ONE-OR-TWO");
      }
    }
    if(Config.config.bLexicalizeNouns){
      lFeatures.add(slot.wordNoun.sWord);
    }
    return lFeatures;
  }

  void addFeatureIf(List<Object> lFeatures, boolean bTrigger, Object... aObjs){
    Object oFeature = Arrays.asList(aObjs);
    if(bTrigger){
      lFeatures.add(oFeature);
    }
  }
                    
  boolean areNearEachOther(StanfordWord word1, StanfordWord word2){
    if((word1 == null) || (word2 == null) || 
       (word1.sentence.iIndex != word2.sentence.iIndex)){
      return false;
    }
    return (Math.abs(word1.iIndex-word2.iIndex)<= Config.config.iMaxNearbyDist);
  }

  boolean isAPrepObjOfB(StanfordWord wordA, StanfordWord wordB, boolean bExact){
    for(StanfordWord wordPrepObjOfB : wordB.getPrepObjs()){
      if((bExact && (wordA == wordPrepObjOfB)) ||
         (!bExact && wordA.sWord.equals(wordPrepObjOfB.sWord))){
        return true;
      }
    }
    return false;
  }

  void addDepPathFeatures(List<Object> lFeatures, Slot slot1, Slot slot2){
    if(slot1.wordNoun.sentence.iIndex == slot2.wordNoun.sentence.iIndex){
      int iWord1 = slot1.wordNoun.iIndex;
      int iWord2 = slot2.wordNoun.iIndex;
      StanfordSentence.Path path=slot1.wordNoun.sentence.aPaths[iWord1][iWord2];
      for(int iWord = 0; iWord < path.lStemPath.size(); iWord++){
        String sWord = path.lWordPath.get(iWord);
        String sStem = path.lStemPath.get(iWord);
        Dir dir = path.lDirPath.get(iWord);
        String sDep = path.lDepPath.get(iWord);
        if(Config.config.bUseDepPathStems){
          lFeatures.add(Arrays.asList("DEP-PATH-STEM", sStem));
        }
        if(Config.config.bUseAllDepPathFeatures){
          lFeatures.add(Arrays.asList("DEP-PATH-WORD", sWord));
          lFeatures.add(Arrays.asList("DEP-PATH-DEP", sDep));
          lFeatures.add(Arrays.asList("DEP-PATH-WORD-DEP", sWord, sDep));
          lFeatures.add(Arrays.asList("DEP-PATH-DEP-DIR", sWord, dir));
          lFeatures.add(Arrays.asList("DEP-PATH-WORD-DEP-DIR", sWord,sDep,dir));
        }
      }
    }
  }

  List<Object> getSimpleSlotPairFeatures(Slot slot1, Slot slot2){
    List<Object> lFeatures = new ArrayList<Object>();
    //1. check if the wordpath between them contains and
    boolean bContainsAnd = wordPathContainsAnd(slot1.wordNoun,slot2.wordNoun);
    addFeatureIf(lFeatures, bContainsAnd, "PATH-CONTAINS-AND");
    //2. check if they're exactly the same word
    addFeatureIf(lFeatures, slot1.wordNoun == slot2.wordNoun, "SAME-WORD");
    addFeatureIf(lFeatures, slot1.wordNoun != slot2.wordNoun, "DIFF-WORD");
    //3. check if their word stems overlap
    boolean bEqualStems = slot1.wordNoun.sWord.equals(slot2.wordNoun.sWord);
    addFeatureIf(lFeatures, bEqualStems, "SAME-STEM");
    //4.  check if they're in the same sentence and near each other
    if(slot1.isNumber() && slot2.isNumber()){
      boolean bNearEachOther = areNearEachOther(slot1.wordNoun, slot2.wordNoun);
      addFeatureIf(lFeatures, bNearEachOther, "NEAR-EACH-OTHER");
    }
    //5.  add is prepobjof both exact and same stem
    //these features are directional, so we have to do both directions
    boolean b1EqualToPrepObjOf2 = isAPrepObjOfB(slot1.wordNoun, slot2.wordNoun,
                                                false);
    boolean b2EqualToPrepObjOf1 = isAPrepObjOfB(slot2.wordNoun,slot1.wordNoun,
                                                false);
    addFeatureIf(lFeatures, b1EqualToPrepObjOf2, "1EqualToPrepObjOf2");
    addFeatureIf(lFeatures, b2EqualToPrepObjOf1, "2EqualToPrepObjOf1");
    boolean b1PrepObjOf2 = isAPrepObjOfB(slot1.wordNoun, slot2.wordNoun, true);
    boolean b2PrepObjOf1 = isAPrepObjOfB(slot2.wordNoun, slot1.wordNoun, true);
    addFeatureIf(lFeatures, b1PrepObjOf2, "1PrepObjOf2");
    addFeatureIf(lFeatures, b2PrepObjOf1, "2PrepObjOf1");
    //6.  if both are numbers then add a feature indicating which is bigger
    Double fNumber1 = slot1.getNumberValue();
    Double fNumber2 = slot2.getNumberValue();
    if((fNumber1 != null) && (fNumber2 != null)){
      if(fNumber1 > fNumber2){
        lFeatures.add("1GreaterThan2");
      } else if(fNumber2 > fNumber1){
        lFeatures.add("1LessThan2");
      }
      if(fNumber1 == fNumber2){
        lFeatures.add("NumericallyEqual");
      } else {
        lFeatures.add("NotNumericallyEqual");
      }
    }
    //7. add bad of word features for the path in between
    addDepPathFeatures(lFeatures, slot1, slot2);
    return lFeatures;
  }
  
  <T> boolean hasEqualPrefix(int iLen, List<T> list1, List<T> list2){
    if((list1.size() < iLen) || (list2.size() < iLen)){
      return false;
    }
    for(int iIndex = 0; iIndex < iLen; iIndex++){
      if(!Objects.equals(list1.get(iIndex), list2.get(iIndex))){
        return false;
      }
    }
    return true;
  }

  boolean equalRelations(Slot slot1, Slot slot2, Slot slot3,Slot slot4){
    if((slot1.isNumber() == slot2.isNumber()) || 
       (slot3.isNumber() == slot4.isNumber())){
      return false;
    }
    // right now we're not going to handle non-unique variables
    for(Slot slot : Arrays.asList(slot1, slot2, slot3, slot4)){
      if(slot.llTermLists.size() != 1){
        return false;
      }
    }
    //now check if the term signatures is met
    if(!hasEqualPrefix(2, slot1.llTermLists.get(0), slot2.llTermLists.get(0))||
       !hasEqualPrefix(2, slot3.llTermLists.get(0), slot4.llTermLists.get(0))||
       !hasEqualPrefix(1, slot1.llTermLists.get(0), slot2.llTermLists.get(0))){
      return false;
    }

    List<StanfordWord> lWords1 = getWordPath(slot1.wordNoun, slot2.wordNoun);
    List<StanfordWord> lWords2 = getWordPath(slot3.wordNoun, slot4.wordNoun);
    boolean bSameVerb = sameType(lWords1, lWords2, true);
    boolean bSamePrep = sameType(lWords1, lWords2, false);
    return bSameVerb || bSamePrep;
  }

  FeatureListList getNewFeatures(FilledTerm ft){
    FeatureListList fll = new FeatureListList();
    addSingleFeature(fll, features.getFeatureIndex("OFFSET"));
    //for all pairs indicate if it's the same word or not
    for(int iSlot1 = 0; iSlot1 < ft.mt.numFilledTermSlots(); iSlot1++){
      StanfordWord word1 = ft.getUniqueSlotNoun(iSlot1);
      if(word1 == null){
        continue;
      }
      for(int iSlot2 = 0; iSlot2 < ft.mt.numFilledTermSlots(); iSlot2++){
        if(iSlot2 == iSlot1){
          continue;
        }
        StanfordWord word2 = ft.getUniqueSlotNoun(iSlot2);
        if(word2 == null){
          continue;
        }
        String sSlotSig = ft.mt.aSimpleSlotPairSignatures[iSlot1][iSlot2];
        if(word1.sWord.equals(word2.sWord)){
          Object oFeature = Arrays.asList(sSlotSig,"SAME-STEM");
          addSingleFeature(fll, features.getFeatureIndex(oFeature));
        }
        for(int iSlot3 = 0; iSlot3 < ft.mt.numFilledTermSlots(); iSlot3++){
          if((iSlot3 == iSlot2) || (iSlot3 == iSlot1)){
            continue;
          }
          StanfordWord word3 = ft.getUniqueSlotNoun(iSlot3);
          if(word3 == null){
            continue;
          }
          for(int iSlot4 = 0; iSlot4 < ft.mt.numFilledTermSlots(); iSlot4++){
            if((iSlot4 == iSlot3) || (iSlot4 == iSlot2) || (iSlot4 == iSlot1)){
              continue;
            }
            StanfordWord word4 = ft.getUniqueSlotNoun(iSlot4);
            if(word4 == null){
              continue;
            }
            if((word1.sentence.iIndex == word2.sentence.iIndex) && 
               (word3.sentence.iIndex == word4.sentence.iIndex)){
              StanfordSentence.Path path1 = 
                word1.sentence.aPaths[word1.iIndex][word2.iIndex];
              StanfordSentence.Path path2 = 
                word3.sentence.aPaths[word3.iIndex][word4.iIndex];
              if(path1.lDepPath.equals(path2.lDepPath)){
                String sSig = ft.mt.toString(false,true);
                String sQuadSig = sSig + ":" + iSlot1 + ":" + iSlot2 + ":" +
                  iSlot3 + ":" + iSlot4;
                Object oFeature2 = Arrays.asList(sQuadSig, "EQUAL-DEP-PATHS");
                addSingleFeature(fll, features.getFeatureIndex(oFeature2));
              }
            }
          }
        }
      }
    }
    return fll;
  }


  FeatureListList getFeatures(FilledTerm ft, Question question, 
                              FeatureCache cache, 
                              List<SimpleSolver> lSolvers){
    if(Config.config.bUseNewFeatures){
      if(Config.config.bUseOldNewFeatures){
        return getNewFeaturesSimple(ft, question);
      } else {
        return getSimpleFeatures(ft, question, cache, lSolvers);
      }
    }
    if(Config.config.bUseOnlyDebugFeatures){
      return getDebugFeatures(ft);
    }
    FeatureListList fll = new FeatureListList();
    //0. the offset feature
    if(Config.config.bUseOffsetFeature){
      addSingleFeature(fll, features.getFeatureIndex("OFFSET"));
    }
    //1. first get structural features
    addEquationFeatures(fll, ft);
    //2. now get features for the filled slots
    for(int iSlot1 = 0; iSlot1 < ft.mt.numFilledTermSlots(); iSlot1++){
      if(ft.getUniqueSlotNoun(iSlot1) == null){
        continue;
      }
      //individual slot features
      fll.add(getCachedWordFeatureList(ft, iSlot1));
      if(Config.config.bUseNumberFeatures){
        addNumberFeatures(fll, ft, iSlot1);
      }
      for(int iSlot2 = 0; iSlot2 < ft.mt.numFilledTermSlots(); iSlot2++){
        if(iSlot2 == iSlot1){
          continue;
        }
        if(ft.getUniqueSlotNoun(iSlot2) == null){
          continue;
        }
        if(Config.config.bUseNumberFeatures){
          addNumberFeatures(fll, ft, iSlot1, iSlot2);
        }
        fll.add(getCachedWordPairFeatureList(ft, iSlot1, iSlot2));
      }
    }
    return fll;
  }

  List<Object> getNumberFeatures(StanfordNumber number){
    List<Object> lFeatures = new ArrayList<Object>();
    if(number.bIsLargest){
      lFeatures.add("LARGEST-NUMBER");
    }
    if(number.wordNoun.isNoun()){
      lFeatures.add("NUMBER-MODIFIES-NOUN");
    } else {
      lFeatures.add("NUMBER-DOESNT-MODIFY-NOUN");
    }
    return lFeatures;
  }

  void addNumberFeatures(FeatureListList fll, FilledTerm ft, int iSlot){
    if(iSlot < ft.lUnknownWords.size()){
      //it's an uknown, so there's no number features
      return;
    }
    StanfordNumber number = 
      ft.lNumberWords.get(iSlot-ft.lUnknownWords.size()).number;
    List<Object> lFeatures = getNumberFeatures(number);
    if(lFeatures.size() != 0){
      List<Object> lSlotSignatures = ft.mt.getSlotSignatures(iSlot);
      addDoubleCrossFeaturesAsSingleFeatures(fll, lFeatures, lSlotSignatures);
    }
  }

  void addDoubleCrossFeaturesAsSingleFeatures(FeatureListList fll,
                                              List<Object> lFeaturesA,
                                              List<Object> lFeaturesB){
    for(Object oFeatureA : lFeaturesA){
      for(Object oFeatureB : lFeaturesB){
        Object oFeature = Arrays.asList(oFeatureA, oFeatureB);
        int iFeature = features.getFeatureIndexFudgeFactor(oFeature, oFeatureA);
        addSingleFeature(fll, iFeature);
      }
    }
  }

  List<Object> getNumberFeatures(StanfordNumber number1,StanfordNumber number2){
    List<Object> lFeatures = new ArrayList<Object>();
    if(number1.fNumber > number2.fNumber){
      lFeatures.add("FIRST-GREATER");
    } else {
      lFeatures.add("SECOND-GREATER");
    }
    if(number1.fNumber == number2.fNumber){
      lFeatures.add("NUMBERS-EQUAL");
    } else {
      lFeatures.add("NUMBERS-NOT-EQUAL");
    }
    return lFeatures;
  }


  void addNumberFeatures(FeatureListList fll, FilledTerm ft, int iSlot1, 
                         int iSlot2){
    boolean bNumber1 = (iSlot1 >= ft.lUnknownWords.size());
    boolean bNumber2 = (iSlot2 >= ft.lUnknownWords.size());
    if(bNumber1 && bNumber2){
      StanfordNumber number1 = 
        ft.lNumberWords.get(iSlot1-ft.lUnknownWords.size()).number;
      StanfordNumber number2 = 
        ft.lNumberWords.get(iSlot2-ft.lUnknownWords.size()).number;
      List<Object> lFeatures = getNumberFeatures(number1, number2);
      if(lFeatures.size() != 0){
        List<Object> lSlotSignatures = ft.mt.getSlotSignatures(iSlot1, iSlot2);
        addDoubleCrossFeaturesAsSingleFeatures(fll, lFeatures, lSlotSignatures);
      }
    }
  }


  FeatureList getCachedWordFeatureList(FilledTerm ft, int iSlot){
    StanfordWord word = ft.getUniqueSlotNoun(iSlot);
    String sSlotSignature;
    if(Config.config.bUseNonCanonicalSlotSigs){
      sSlotSignature = ft.mt.aSimpleSlotSignatures[iSlot];
    } else {
      sSlotSignature = ft.mt.aUniqueSlotSignatures[iSlot];
    }
    Object oSlotKey = Arrays.asList(sSlotSignature, word);
    FeatureList featurelist = mFeatureListCache.get(oSlotKey);
    if(featurelist == null){
      List<Object> lWordFeatures = getCachedWordFeatures(word);
      List<Object> lSlotSignatures = ft.mt.getSlotSignatures(iSlot);
      //now cross every feature with the slot
      int[] aFeatures = 
        features.doubleCrossFeaturesAsArray(lWordFeatures, lSlotSignatures);
      featurelist = new FeatureList(aFeatures);
      mFeatureListCache.put(oSlotKey, featurelist);
    }
    return featurelist;
  }

  FeatureList getCachedWordPairFeatureList(FilledTerm ft,int iSlot1,int iSlot2){
    StanfordWord word1 = ft.getUniqueSlotNoun(iSlot1);
    StanfordWord word2 = ft.getUniqueSlotNoun(iSlot2);
    String sSlotSignature;
    if(Config.config.bUseNonCanonicalSlotSigs){
      sSlotSignature = ft.mt.aSimpleSlotPairSignatures[iSlot1][iSlot2];
    } else {
      sSlotSignature = ft.mt.aUniqueSlotPairSignatures[iSlot1][iSlot2];
    }
    Object oSlotKey = Arrays.asList(sSlotSignature, word1, word2);
    FeatureList featurelist = mFeatureListCache.get(oSlotKey);
    if(featurelist == null){
      List<Object> lWordFeatures = getCachedWordFeatures(word1, word2);
      List<Object> lSlotSignatures = ft.mt.getSlotSignatures(iSlot1, iSlot2);
      //now cross every feature with the slot
      int[] aFeatures = 
        features.doubleCrossFeaturesAsArray(lWordFeatures, lSlotSignatures);
      featurelist = new FeatureList(aFeatures);
      mFeatureListCache.put(oSlotKey, featurelist);
    }
    return featurelist;
  }

  List<Object> getCachedWordFeatures(StanfordWord word){
    List<Object> lWordFeatures = mFeatureCache.get(word);
    if(lWordFeatures == null){
      if(Config.config.bUseUnknownFeatures){
        lWordFeatures = getUnknownWordFeatures(word);
      } else {
        lWordFeatures = getGeneralWordFeatures(word);
      }
      mFeatureCache.put(word, lWordFeatures);
    }
    return lWordFeatures;
  }

  List<Object> getCachedWordFeatures(StanfordWord word1, StanfordWord word2){
    Object oWordKey = memcache.get(word1, word2);
    List<Object> lWordFeatures = mFeatureCache.get(oWordKey);
    if(lWordFeatures == null){
      lWordFeatures = getWordOverlapFeatures(word1, word2);
      mFeatureCache.put(oWordKey, lWordFeatures);
    }
    return lWordFeatures;
  }

  FeatureListList getFeatures(Equ equ){
    return null;
  }

  FeatureListList getFeatures(EquationDerivation der){
    FeatureListList fll = new FeatureListList();
    // String sEquation = der.equation.toExactString();
    // if(Config.config.bUseOnlyDebugFeatures){
    //   Object oFeature = Arrays.asList(sEquation,der.lNumbers,der.lUnknowns);
    //   int iFeature = features.getFeatureIndex(oFeature);
    //   fll.add(new FeatureList(new int[]{iFeature}));
    //   return fll;
    // }
    // List<StanfordWord> lNumberNouns = getNumberWordNouns(der.lNumbers);
    // addWordOverlapFeatures(fll, sEquation, der.lUnknowns, lNumberNouns);
    // addUnknownWordFeatures(fll, sEquation, der.lUnknowns);
    // addNumberWordFeatures(fll, sEquation, lNumberNouns);
    // //add a single feature for the equation to measure it's popularity
    // addSingleFeature(fll, features.getFeatureIndex(sEquation));
    // addSingleFeature(fll, features.getFeatureIndex("OFFSET"));
    // Misc.Assert(fll.lFeatureLists.size() != 0); // must have 1 feature
    return fll;
  }

  void addSingleFeature(FeatureListList fll, int iFeature){
    //this should never get called without a cache
    Misc.Assert(false);
  }

  void addSingleFeature(FeatureListList fll, int iFeature, FeatureCache cache){
    FeatureList featurelist = cache.mFeatureListCache.get(iFeature);
    if(featurelist == null){
      featurelist = new FeatureList(new int[]{iFeature});
      cache.mFeatureListCache.put(iFeature, featurelist);
    }
    fll.add(featurelist);
  }



  FeatureSet getFeatures(Derivation der, boolean bCorrect){
    FeatureSet featureset = new FeatureSet(false);
    // List<StanfordWord> lNumberNouns = getNumberWordNouns(der.lNumbers);
    // List<Object> lFeatures = new ArrayList<Object>();
    // lFeatures.addAll(getWordOverlapFeatures(der.lUnknowns, lNumberNouns));
    // lFeatures.addAll(getUnknownWordFeatures(der.lUnknowns));
    // lFeatures.addAll(getNumberWordFeatures(lNumberNouns));
    // //add the features with and without crossing with the equation system
    // features.addFeatures(featureset, lFeatures);
    // features.addCrossFeatures(featureset, der.system.toFeatureString(),
    //                           lFeatures);
    // features.addFeature(featureset, der.system.toFeatureString());
    // if(Config.config.bUseCheatFeature){
    //   features.addFeature(featureset, Arrays.asList("CORRECT", bCorrect));
    // }
    return featureset;
  }

  List<StanfordWord> getNumberWordNouns(List<StanfordWord> lNumberWords){
    //this pulls all the nouns that the numbers hand off of
    //first build up the word and type lists
    List<StanfordWord> lWords = new ArrayList<StanfordWord>();
    //first add the numbers
    for(StanfordWord wordNumber : lNumberWords){
      lWords.add(wordNumber.number.wordNoun);
    }
    return lWords;
  }

  // void addWordOverlapFeatures(FeatureListList fll, String sEquation,
  //                             List<StanfordWord> lUnknownWords,
  //                             List<StanfordWord> lNumberNouns){
  //   List<StanfordWord> lWords = new ArrayList<StanfordWord>(lNumberNouns);
  //   lWords.addAll(lUnknownWords);
  //   for(int iWord1 = 0; iWord1 < lWords.size(); iWord1++){
  //     for(int iWord2 = iWord1+1; iWord2 < lWords.size(); iWord2++){
  //       //check in the cache
  //       StanfordWord word1 = lWords.get(iWord1);
  //       StanfordWord word2 = lWords.get(iWord2);
  //       Object oEquationLoc = memcache.get(sEquation,iWord1,iWord2);
  //       Object  oEquationLocWords = memcache.get(oEquationLoc, word1, word2);
  //       FeatureList featurelist = mFeatureListCache.get(oEquationLocWords);
  //       if(featurelist == null){
  //         Object oWordPair = memcache.get(word1, word2);
  //         List<Object> lWordOverlapFeatures = mFeatureCache.get(oWordPair);
  //         if(lWordOverlapFeatures == null){
  //           lWordOverlapFeatures = getWordOverlapFeatures(word1,word2);
  //           mFeatureCache.put(oWordPair,lWordOverlapFeatures);
  //         }
  //         int[] aFeatures = features.crossFeaturesAsArray(oEquationLoc, 
  //                                                         lWordOverlapFeatures);
  //         featurelist = new FeatureList(aFeatures);
  //         mFeatureListCache.put(oEquationLocWords, featurelist);
  //       }
  //       fll.add(featurelist);
  //     }
  //   }
  // }

  List<Object> getOverlapTypes(StanfordWord word1, StanfordWord word2){
    List<Object> lOverlapTypes = new ArrayList<Object>();
    if(word1.sWordOrig.equals(word2.sWordOrig)){
      if(Config.config.bUseExactWordFeatures){
        lOverlapTypes.add("EXACT-OVERLAP");
      }
    }
    if(word1.sWord.equals(word2.sWord)){
      lOverlapTypes.add("STEM-OVERLAP");
    }
    return lOverlapTypes;
  }

  void addUnknownWordFeatures(FeatureListList fll, String sEquation, 
                              List<StanfordWord> lUnknowns){
    for(int iUnknown =0 ; iUnknown < lUnknowns.size(); iUnknown++){
      StanfordWord word = lUnknowns.get(iUnknown);
      Object oEquationLoc = memcache.get("UNKNOWN",sEquation,iUnknown);
      Object oEquationLocCrossWord = memcache.get(oEquationLoc,word);
      FeatureList featurelist = mFeatureListCache.get(oEquationLocCrossWord);
      if(featurelist == null){
        Object oWordType = memcache.get("UNKNOWN",word);
        List<Object> lFeatureObjs = mFeatureCache.get(oWordType);
        if(lFeatureObjs == null){
          lFeatureObjs = getUnknownWordFeatures(word);
          mFeatureCache.put(oWordType,lFeatureObjs);
        }
        int[] aFeatures = features.crossFeaturesAsArray(oEquationLoc, 
                                                        lFeatureObjs);
        featurelist = new FeatureList(aFeatures);
        mFeatureListCache.put(oEquationLocCrossWord, featurelist);
      }
      fll.add(featurelist);
    }
  }

  void addNumberWordFeatures(FeatureListList fll, String sEquation,
                             List<StanfordWord> lNumberNouns){
    for(int iNumber =0 ; iNumber < lNumberNouns.size(); iNumber++){
      StanfordWord word = lNumberNouns.get(iNumber);
      Object oEquationLoc = memcache.get("NUMBER-NOUN",sEquation,iNumber);
      Object oEquationLocCrossWord = memcache.get(oEquationLoc,word);
      FeatureList featurelist = mFeatureListCache.get(oEquationLocCrossWord);
      if(featurelist == null){
        Object oWordType = memcache.get("NUMBER-NOUN", word);
        List<Object> lFeatureObjs = mFeatureCache.get(oWordType);
        if(lFeatureObjs == null){
          lFeatureObjs = getGeneralWordFeatures(word);
          mFeatureCache.put(oWordType,lFeatureObjs);
        }
        int[] aFeatures = features.crossFeaturesAsArray(oEquationLoc, 
                                                        lFeatureObjs);
        featurelist = new FeatureList(aFeatures);
        mFeatureListCache.put(oEquationLocCrossWord, featurelist);
      }
      fll.add(featurelist);
    }
  }

  List<Object> getUnknownWordFeatures(StanfordWord word){
    //check if the sentence is a question
    List<Object> lFeatures = new ArrayList<Object>();
    StanfordWord wordLast = 
      word.sentence.lWords.get(word.sentence.lWords.size()-1);
    boolean bIsQuestionSentence = wordLast.sWordOrig.equals("?");
    //does the sentence contain how many?
    List<String> lHowMany = Arrays.asList("How many", "How much", "how many",
                                          "how much", "How long", "how long",
                                          "How old", "how old");
    List<String> lCommand = Arrays.asList("Find","Calculate");
    boolean bContainsHowMany = 
      Misc.stringContainsAtLeastOneOf(word.sentence.sSentenceOrig, lHowMany);
    boolean bContainsCommand = 
      Misc.stringContainsAtLeastOneOf(word.sentence.sSentenceOrig, lCommand);
    boolean bContainsWP = false;
    for(StanfordWord wordCur : word.sentence.lWords){
      if(wordCur.isRootParent()){
        continue;
      }
      if(wordCur.sPos.equals("WP")){
        bContainsWP = true;
        break;
      }
    }
    //now add features for all the question word stuff
    lFeatures.add(memcache.get("CONTAINS-HOW-MANY",bContainsHowMany));
    lFeatures.add(memcache.get("CONTAINS-COMMAND",bContainsCommand));
    lFeatures.add(memcache.get("CONTAINS-WP",bContainsWP));
    lFeatures.add(memcache.get("CONTAINS-QUESTION-WORDS",
                               bContainsHowMany | bContainsCommand |
                               bContainsWP));

    lFeatures.addAll(getGeneralWordFeatures(word));
    return lFeatures;
  }

  List<Object> getGeneralWordFeatures(StanfordWord word){
    List<Object> lFeatures = new ArrayList<Object>();
    //just add the word itself
    if(Config.config.bUseExactWordFeatures){
      addIfCommon(lFeatures, "EXACTWORD", word.sWordOrig);
    }
    addIfCommon(lFeatures, "WORDSTEM", word.sWord);

    //get all children of the word
    for(StanfordDep depChild : word.lDeps){
      if(Config.config.bUseExactWordFeatures){
        addIfCommon(lFeatures, "CHILD-EXACTWORD", depChild.wordChild.sWordOrig);
      }
      addIfCommon(lFeatures, "CHILD-STEMWORD",depChild.wordChild.sWord);
      addIfCommon(lFeatures, "CHILD-STEMWORD-DEP",depChild.wordChild.sWord,
                  depChild.sType);
    }

    if(word.isRoot()){
      addIfCommon(lFeatures, "IS-ROOT");
      return lFeatures;
    }

    //find the root of the sentence
    String sWordRoot = word.sentence.wordRoot.sWordOrig;
    addIfCommon(lFeatures, "SENTENCE-ROOT", sWordRoot);

    //what's the immediate parent and type of the word
    String sExactWordParent = word.getParent().sWordOrig;
    String sStemWordParent = word.getParent().sWord;
    String sType = word.depParent.sType;
    if(Config.config.bUseExactWordFeatures){
      addIfCommon(lFeatures, "PARENT-EXACTWORD-DEP",sExactWordParent,sType);
    }
    addIfCommon(lFeatures, "PARENT-STEMWORD-DEP",sStemWordParent, sType);
    if(Config.config.bUseExactWordFeatures){    
      addIfCommon(lFeatures, "PARENT-EXACTWORD",sExactWordParent);
    }
    addIfCommon(lFeatures, "PARENT-STEMWORD",sStemWordParent);
    addIfCommon(lFeatures, "PARENT-DEP",sType);


    //first get the whole path in POS and in words and deps
    ImmutableList.Builder<String> lExactWordPath =ImmutableList.builder();
    lExactWordPath.add("EXACT-WORD-PATH-TO-ROOT");
    ImmutableList.Builder<String> lWordStemPath = ImmutableList.builder();
    lWordStemPath.add("WORD-STEM-PATH-TO-ROOT");
    ImmutableList.Builder<String> lDepPath = ImmutableList.builder();
    lDepPath.add("DEP-PATH-TO-ROOT");
    ImmutableList.Builder<String> lPosPath = ImmutableList.builder();
    lPosPath.add("POS-PATH-TO-ROOT");
    do{
      String sDepType = word.depParent.sType;
      word = word.getParent();
      lExactWordPath.add(word.sWordOrig);
      lWordStemPath.add(word.sWord);
      lDepPath.add(sDepType);
      lPosPath.add(word.sPos);
    } while(!word.isRoot());
    if(Config.config.bUseExactWordFeatures){
      addIfCommon(lFeatures, lExactWordPath.build());
    }
    addIfCommon(lFeatures, lWordStemPath.build());
    addIfCommon(lFeatures, lDepPath.build());
    addIfCommon(lFeatures, lPosPath.build());
    return lFeatures;
  }

  int distToBin(int iDist){
    if(!Config.config.bUseDistBins){
      return iDist;
    }
    //for now lets do 1,2,3,4-6,>6
    if(iDist < 3){
      return iDist;
    } else if(iDist < 7){
      return 3;
    } else {
      return 7;
    }
  }
  
  List<Object> getWordOverlapFeatures(StanfordWord word1, StanfordWord word2){
    List<Object> lFeatures = new ArrayList<Object>();
    if(word1 == word2){
      addIfCommon(lFeatures, "SAME-WORD");
      return lFeatures;
    } 
    addIfCommon(lFeatures, "DIFFERENT-WORD");

    //are they in the same sentence
    if(word1.sentence.iIndex == word2.sentence.iIndex){
      Misc.Assert(word1.iIndex != word2.iIndex);
      addIfCommon(lFeatures, "SAME-SENTENCE");
      //add dependency path features
      addDependencyPathFeatures(lFeatures, word1, word2);
      //now add the words/stems/pos in between
      addInBetweenWordFeatures(lFeatures, word1, word2);
    } else {
      addIfCommon(lFeatures, "DIFF-SENTENCE");
      addIfCommon(lFeatures, 
                  memcache.get("SENTENCE-DIST",
                               word1.sentence.iIndex-word2.sentence.iIndex));
    }

    //check for exact and stem overlap between the words themselves
    for(Object oOverlapType : getOverlapTypes(word1, word2)){
      addIfCommon(lFeatures, oOverlapType);
    }
    
    if(Config.config.bUsePeerOverlapFeatures){
      addPeerOverlapFeatures(lFeatures, word1, word2);
    }
    if(Config.config.bUseNounPhraseOverlapFeatures){
      addNounPhraseOverlapFeatures(lFeatures, word1, word2);;
    }

    return lFeatures;
  }

  void addNounPhraseOverlapFeatures(List<Object> lFeatures, StanfordWord word1,
                                    StanfordWord word2){
    List<StanfordWord> lNP1 = word1.getSubTree();
    List<StanfordWord> lNP2 = word2.getSubTree();
    List<String> lStems1 = StanfordWord.getStemList(lNP1, false);
    List<String> lStems2 = StanfordWord.getStemList(lNP2, false);
    if(lStems1.equals(lStems2)){
      lFeatures.add("NP-SAME-STEMS");
      if(lStems1.size() == 1){
        lFeatures.add("NP-SAME-STEM-SIZE-1");
      }
    }
    //now check if there's any overlap in the adjects and nouns
    Set<String> setNNADJStems1 = 
      new HashSet<String>(StanfordWord.getStemList(lNP1, true));
    Set<String> setNNADJStems2 = 
      new HashSet<String>(StanfordWord.getStemList(lNP2, true));
    if(setNNADJStems1.equals(setNNADJStems2)){
      lFeatures.add("NP-SAME-STEMS-NP-ADJ");
      if(setNNADJStems1.size() == 1){
        lFeatures.add("NP-SAME-STEMS-NP-ADJ-SIZE-1");
      }
    }
    int iOverlapSize = 
      CollectionUtils.intersection(setNNADJStems1,setNNADJStems2).size();
    int iMaxSize = Math.max(setNNADJStems1.size(), setNNADJStems2.size());
    if(iOverlapSize !=0){
      lFeatures.add("NP-HAS-OVERLAP-NP-ADJ");
      if((setNNADJStems1.size() == 1) || (setNNADJStems2.size() == 1)){
        lFeatures.add("NP-HAS-OVERLAP-NP-ADJ-SIZE-1");
      }
    }
    //let's bin the overlap
    double fOverlap = Misc.div(iOverlapSize, iMaxSize);
    if(fOverlap == 0.0){
    } else if(fOverlap <= 0.25){
      fOverlap = 0.25;
    } else if(fOverlap <= 0.5){
      fOverlap = 0.5;
    } else if(fOverlap <= 0.75){
      fOverlap = 0.75;
    } else {
      fOverlap = 1.0;
    }
    lFeatures.add(Arrays.asList("NP-OVERLAP-FRAC", fOverlap));
    //and lets call it day with that
  }

  void addIfCommon(List<Object> lFeatures, Object... aObjs){
    Object oFeature = Arrays.asList(aObjs);
    if(!Config.config.bUseOnlyCommonFeatures || (mCommonFeatures == null)){
      lFeatures.add(oFeature);
    } else {
      oFeature = mCommonFeatures.get(oFeature);
      if(oFeature != null){
        lFeatures.add(oFeature);
      }
    }
  }


  void addDependencyPathFeatures(List<Object> lFeatures, StanfordWord word1,
                                 StanfordWord word2){

      StanfordSentence.Path path = 
        word1.sentence.aPaths[word1.iIndex][word2.iIndex];
      if(path == null){
        System.out.println("Path from: " + word1.toFullString() + " to: " + 
                           word2.toFullString() + " is null");
        System.out.println("Sentence: " + word1.sentence.sSentenceOrig);
        Misc.Assert(path != null);
      }
      addIfCommon(lFeatures, "DEPPATHLEN",path.iDist);
      addIfCommon(lFeatures, "DEPPATHDIRS",path.lDirPath);
      addIfCommon(lFeatures, "DEPPATHDEPS",path.lDepPath);
      if(Config.config.bUseExactWordFeatures){
        addIfCommon(lFeatures, "DEPPATHWORDS",path.lWordPath);
      }
      addIfCommon(lFeatures, "DEPPATHSTEM",path.lStemPath);
      addIfCommon(lFeatures, "DEPPATHPOS",path.lPosPath);
      addIfCommon(lFeatures, "DEPPATHROOT",path.sWordRoot);
      addIfCommon(lFeatures, "DEPPATHPOSROOT",path.lPosPath,path.sWordRoot);
      if(path.bConj){
        addIfCommon(lFeatures, "DEPPATHHASCONJ");
      }
  }

  void addInBetweenWordFeatures(List<Object> lFeatures, StanfordWord word1,
                                StanfordWord word2){
    Misc.Assert(word1 != word2);
    int iFirst = word1.iIndex;
    int iSecond = word2.iIndex;
    String sDir = "FORWARD";
    if(iFirst > iSecond){
      sDir = "BACKWARD";
      iFirst = word2.iIndex;
      iSecond = word1.iIndex;
    }
    
    //if they're in the same sentence what's the sequence of words or pos tags
    // between them
    StanfordSentence sentence = word1.sentence;
    List<String> lWordStrings = 
      sentence.lWordStrings.subList(iFirst+1,iSecond);
    if(Config.config.bUseExactWordFeatures){
      addIfCommon(lFeatures, "WORD-SEQUENCE", lWordStrings);
    }
    List<String> lWordStemStrings = 
      sentence.lWordStemStrings.subList(iFirst+1,iSecond);
    addIfCommon(lFeatures, "WORD-STEM-SEQUENCE", lWordStemStrings);
    List<String> lPosStrings = 
      sentence.lPosStrings.subList(iFirst+1,iSecond);
    addIfCommon(lFeatures, "POS-SEQUENCE", lPosStrings);
  }


  void addPeerOverlapFeatures(List<Object> lFeatures, StanfordWord word1,
                              StanfordWord word2){
    //add features for overlap of nearby words
    boolean bSameSentence = (word1.sentence.iIndex == word2.sentence.iIndex);
    
    for(StanfordWord wordPeer1 : word1.sentence.lWords){
      if(wordPeer1.isRootParent() || (wordPeer1.iIndex == word1.iIndex)){
        continue;
      }
      int iDist1 = 
        distToBin(word1.sentence.aDists[word1.iIndex][wordPeer1.iIndex]);
      String sPos1 = wordPeer1.sPos;
      for(StanfordWord wordPeer2 : word2.sentence.lWords){
        if(wordPeer2.isRootParent() || (wordPeer2.iIndex == word2.iIndex) ||
           (wordPeer1 == wordPeer2)){
          continue;
        }
        int iDist2 = 
          distToBin(word2.sentence.aDists[word2.iIndex][wordPeer2.iIndex]);
        String sPos2 = wordPeer2.sPos;
        for(Object oOverlapType : getOverlapTypes(wordPeer1, wordPeer2)){
          Object oSubFeatures = memcache.get(oOverlapType,iDist1, iDist2,
                                             sPos1, sPos2, bSameSentence);
          lFeatures.add(oSubFeatures);
        }
      }
    }
    if(Config.config.bFeaturesPeerPath){
      addPeerPathFeatures(lFeatures, "PEERPATH1TO2", word1, word2);
      addPeerPathFeatures(lFeatures, "PEERPATH2TO1", word2, word1);
    }
  }


  void addPeerPathFeatures(List<Object> lFeatures, String sHeader, 
                           StanfordWord word1, StanfordWord word2){
    //check for feature overlap between a word and nearby words
    for(StanfordWord wordPeer1 : word1.sentence.lWords){
      if(wordPeer1.isRootParent() || (wordPeer1.iIndex == word1.iIndex)){
        continue;
      }
      if(wordPeer1.sWord.equals(word2.sWord)){
        //add the path between word1 and wordpeer1
        StanfordSentence.Path path = 
          word1.sentence.aPaths[wordPeer1.iIndex][word1.iIndex];
        lFeatures.add(memcache.get(sHeader, "DEPPATHLEN",path.iDist));
        lFeatures.add(memcache.get(sHeader, "DEPPATHDIRS",path.lDirPath));
        lFeatures.add(memcache.get(sHeader, "DEPPATHDEPS",path.lDepPath));
        if(Config.config.bUseExactWordFeatures){
          lFeatures.add(memcache.get(sHeader, "DEPPATHWORDS",path.lWordPath));
        }
        lFeatures.add(memcache.get(sHeader, "DEPPATHSTEM",path.lStemPath));
        lFeatures.add(memcache.get(sHeader, "DEPPATHPOS",path.lPosPath));
        lFeatures.add(memcache.get(sHeader, "DEPPATHROOT",path.sWordRoot));
        lFeatures.add(memcache.get(sHeader, "DEPPATHPOSROOT",path.lPosPath, 
                                   path.sWordRoot));
        lFeatures.add(memcache.get(sHeader, "DEPPATHHASCONJ",path.bConj));
      }
    }
  }

}