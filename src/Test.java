import java.lang.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;

import com.google.common.collect.*;
import org.apache.commons.io.FileUtils;

import com.google.gson.reflect.*;
import com.google.gson.*;

import org.apache.commons.lang3.tuple.*;

class Test{

  static void testReader(){
    Model.load();
  }

  static void testLogAdd(){
    System.out.println("Result: " + Misc.logAdd(0.0, Double.NEGATIVE_INFINITY));
  }

  static void testNumbers(){
    double fNum =Misc.parseDouble("16,000");
    System.out.println("BadNum: " + fNum);
  }

  static void testParser(){
    String sResult = "a=4";
    List<Double> lResults = MaximaParser.parseDoubleResults(sResult);
    System.out.println("Results: " + lResults);
  }

  static void testNumber(){
    Config.load();
    String sTest = "";
    System.out.println("Fixed: " + Misc.getNumber(sTest));
  }


  static void diffTester(){
    List<Integer> list1 = Arrays.asList(1,2,3);
    List<Integer> list2 = Arrays.asList(2,3,4);
    System.out.println("Sim: " + Misc.similarity(list1, list2));
  }

  static List<Object> toList(Object... aObjects){
    return Arrays.asList(aObjects);
  }

  static ImmutableList<Object> toImmutList(Object... aObjects){
    return ImmutableList.builder().add(aObjects).build();
  }

  static void testLists(){
    Object oStrs = new String[]{"nate","is","cool"};
    List<Object> lObjs = toList(oStrs);
    System.out.println("Size: " + lObjs.size());
    ImmutableList lImmutObjs = ImmutableList.of("nate","is","cool");
    ImmutableList<Object> lImmutObjs2 = ImmutableList.of((Object)"nate",(Object)"is",(Object)"cool");
    ImmutableList<Object> lImmutObjs3 = toImmutList("nate","is","cool");
    System.out.println("Size2: " + lImmutObjs.size());
    System.out.println("Equals: " + lImmutObjs.equals(lImmutObjs2));
    System.out.println("Size3: " + lImmutObjs.size());
    System.out.println("Equals: " + lImmutObjs.equals(lImmutObjs3));
    
 }

  static void testProbs(){
    int iNumCorrect = 780;
    int iNumTotal = 139620;
    double fWeightCorrect = 4.5122600975603095;
    double fWeightIncorrect = -0.025349753173043412;
    double fLogCorrect = Math.log(iNumCorrect)+fWeightCorrect;
    double fLogIncorrect = Math.log(iNumTotal-iNumCorrect)+fWeightIncorrect;
    double fLogTotal = Misc.logAdd(fLogCorrect, fLogIncorrect);
    double fLogProb = fLogCorrect-fLogTotal;
    double fProb = Math.exp(fLogProb);
    System.out.println("Prob: " + fProb);
    //update is expected count of correct features = prob of each correct
    // - expected count total = prob of each total
    double fProbOneCorrect = 1.0/((double)iNumCorrect);

    double fLogScoreOneCorrect = fWeightCorrect;
    double fLogScoreOneIncorrect = fWeightIncorrect;

    double fLogProbOneCorrect = 
      fLogScoreOneCorrect-(Math.log(iNumCorrect)+fLogScoreOneCorrect);
    double fProbOneCorrectOther = Math.exp(fLogProbOneCorrect);

    double fLogProbOneCorrectTotal = fLogScoreOneCorrect-fLogTotal;
    double fProbOneCorrectTotal = Math.exp(fLogProbOneCorrectTotal);

    double fLogProbOneIncorrectTotal = fLogScoreOneIncorrect-fLogTotal;
    double fProbOneIncorrectTotal = Math.exp(fLogProbOneIncorrectTotal);


    double fUpdate = fProbOneCorrectOther-fProbOneCorrectTotal;
    System.out.println("ProbOneCorr: " + fProbOneCorrect 
                       + "\n ProbOneCorrOther: " + fProbOneCorrectOther
                       + "\n ProbOneCorrTotal: " + fProbOneCorrectTotal
                       + "\n ProbOneIncorrTotal: " + fProbOneIncorrectTotal
                       + "\n Update: " + fUpdate);

  }

  static void testPaths(){
    Config.load();
    int iIndex = 72;
    System.out.println("Loading Stanford Parse for: " + iIndex);
    String sDirectory = Config.config.sStanfordParseDirectory;
    String sFilename = sDirectory + "question-" + iIndex + ".xml";
    StanfordDocument doc = StanfordParser.parseFile(sFilename);
    StanfordSentence sentence = doc.lSentences.get(1);
    for(StanfordWord word : sentence.lWords){
      String sParent = "ROOT";
      if(!word.isRootParent() && !word.isRoot()){
        sParent = word.getParent().toFullString();
      }
      System.out.println("PARENT: " + word.toFullString() + " " + sParent);
    }

    for(int iWordFrom = 0; iWordFrom < sentence.lWords.size(); iWordFrom++){
      StanfordWord wordFrom = sentence.lWords.get(iWordFrom);
      if(wordFrom.isRootParent()){
        continue;
      }
      String sWordFrom = wordFrom.toFullString();
      for(int iWordTo = 0; iWordTo < sentence.lWords.size();iWordTo++){
        StanfordWord wordTo = sentence.lWords.get(iWordTo);
        if(wordTo.isRootParent()){
          continue;
        }
        String sWordTo = wordTo.toFullString();
        StanfordSentence.Path path = sentence.aPaths[iWordFrom][iWordTo];
        if(path == null){
          System.out.println("Path From: " + sWordFrom + " to " + sWordTo 
                             + " Dist: " + sentence.aDists[iWordFrom][iWordTo]
                             + " PDist: null");
        } else {
          System.out.println("Path From: " + sWordFrom + " to " + sWordTo 
                             + " Dist: " + sentence.aDists[iWordFrom][iWordTo]
                             + " PDist: "+ path.iDist);
        }
        // System.out.println("Dist: " + sentence.aDists[iWordFrom][iWordTo]);
        // if(path == null){
        //   System.out.println("  is null....");
        // } else {
        //   System.out.println(path);
        // }
      }
    }
  }

  static void testPaths2(){
    Model.load();
    for(Question question : Model.lAllQuestions){
      for(StanfordSentence sentence : question.doc.lSentences){
        for(int iWordFrom = 0; iWordFrom < sentence.lWords.size(); iWordFrom++){
          if(sentence.lWords.get(iWordFrom).isRootParent()){
            continue;
          }
          for(int iWordTo = 0; iWordTo < sentence.lWords.size(); iWordTo++){
            if(sentence.lWords.get(iWordTo).isRootParent()){
              continue;
            }
            int iDist = sentence.aDists[iWordFrom][iWordTo];
            StanfordSentence.Path path = sentence.aPaths[iWordFrom][iWordTo];
            int iPathDist = path.iDist;
            if(iDist != iPathDist){
              System.out.println("BadDists: " + iDist + " " + iPathDist);
              System.out.println("Loc: " + question.iIndex + " " + 
                                 sentence.lWords.get(iWordFrom).toFullString()+
                                 " " +
                                 sentence.lWords.get(iWordTo).toFullString() +
                                 "\n" + path.toString());
            }
            Misc.Assert(iDist == iPathDist);
          }
        }
      }
    }
  }

  static void testImmutableList(){
    ImmutableList lInts = ImmutableList.of(1,2);
    ImmutableList lInts2 = ImmutableList.of(1,2);
    System.out.println("Equals: " + lInts.equals(lInts2));
  }

  static void doubleCheck(){
    Double fFirst = 0.1;
    Double fSecond = 0.1;
    System.out.println("Equal: " + (fFirst == fSecond));
  }

  static void testSingleEqu(){
    Model.load();
    Misc.Assert(Model.lTrainQuestions.size() == 1);
    Question question = Model.lTrainQuestions.get(0);
    ConcreteEqu concreteCorr = question.lCorrectEqus.get(1);
    System.out.println("Concrete: " + concreteCorr.toString());
    StanfordWord word466 = question.doc.lSentences.get(0).lWords.get(9);
    StanfordWord wordBoys = question.doc.lSentences.get(1).lWords.get(7);
    StanfordWord wordGirls = question.doc.lSentences.get(1).lWords.get(5);
    Equ equ = new Equ();
    EquTerm term466 = new EquTerm(true);
    EquTerm termBoys = new EquTerm(false);
    EquTerm termGirls = new EquTerm(false);
    equ.lTerms.add(term466);
    equ.lTerms.add(termBoys);
    equ.lTerms.add(termGirls);
    equ.lEqualitySets.add(Arrays.asList(termBoys));
    equ.lEqualitySets.add(Arrays.asList(termGirls));
    term466.addFactor(word466);
    termBoys.addUnknown(wordBoys);
    termGirls.addUnknown(wordGirls);
    System.out.println("Equ: " + equ.toString() + " Concrete: " +
                       equ.toConcreteString());
    //System.out.println("Equal: " + equ.equalsConcrete(concreteCorr) + 
    //                   " Subset: " + equ.concreteSubsetOf(concreteCorr));
    System.out.println("Equal: " + equ.equalsConcrete(concreteCorr, false));
    //compare term by term
    for(EquTerm term : equ.lTerms){
      for(OldConcreteTerm termConcrete : concreteCorr.lTerms){
        System.out.println("Compare: " + term + " to: " + termConcrete + " --> "
                           + term.concrete.equals(termConcrete, false));
      }
    }
    Relation<EquTerm,OldConcreteTerm> relation = Relation.EquConcreteNotOpposite;
    boolean bEqual = Misc.equalSets(equ.lTerms, concreteCorr.lTerms, relation);
    System.out.println("Equal Exact: " + bEqual);
  }

  static void testEquBeam(){
    Model.load();
    Misc.Assert(Model.lTrainQuestions.size() == 1);
    Question question = Model.lTrainQuestions.get(0);
    ConcreteEqu concreteCorr = question.lCorrectEqus.get(0);
    System.out.println("Concrete: " + concreteCorr.toString());
    List<Equ> lNBestAll = EquClassifier.computeNBestEqu(question,concreteCorr);
    System.out.println("Num NBest: " + lNBestAll.size());
    System.out.println("Beam:");
    for(Equ equ : lNBestAll){
      System.out.println(equ + " Finished: " + equ.bFinished + " Equal: " +
                         equ.equalsConcrete(concreteCorr));
    }
  }
  
  static void testJson(){
    String sFileContents = null;
    try{
      sFileContents = 
        FileUtils.readFileToString(new File("questions.json"));
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }
    
    List<QuestionText> lQuestions = 
      new Gson().fromJson(sFileContents,
                          new TypeToken<List<QuestionText>>(){}.getType());
    for(QuestionText question : lQuestions){
      System.out.println("Question: " + question.sQuestion);
    }
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    System.out.println(gson.toJson(lQuestions));
  }

  static void testTerms() throws ParseException,TokenMgrError{
    // Equation equation1 = 
    //   Parser.parseEquation("a+b=2",new ArrayList<String>(), 
    //                        new ArrayList<Double>());
    // System.out.println("Term: " + equation1.termLeft);
    // MappedTerm mt = new MappedTerm(equation1.termLeft, false);
    // System.out.println("MT: " + mt);
    // System.out.println("Equation: " + equation1);
    // MappedTerm mtEq = new MappedTerm(equation1);
    // System.out.println("MT: " + mtEq);
    // System.out.println("term: " + mtEq.term);
    // //ConcreteSystem system =Parser.parseSystem(Arrays.asList("a+b=3","a-b=2"));
    ConcreteSystem system =Parser.parseSystem(Arrays.asList("a=c-b","a=b"));
    // MappedTerm mtSystem = new MappedTerm(system.system);
    // System.out.println("System: " + system.system);
    // System.out.println("MTSystem: " + mtSystem);
    ConcreteTerm ctSystem = new ConcreteTerm(system);
    // System.out.println("FTSystem: " + ctSystem);
    // System.out.println("TermofMTSystem: " + mtSystem.term);
    System.out.println("TermofFTSystem: " + ctSystem.mt);
    // System.out.println("TermSig: " + mt.getSignature());
    // System.out.println("MTSig: " + mtSystem.getSignature());
    System.out.println("FTSig: " + ctSystem.mt.recalcSignature());
  }

  // static void testTerms2() throws ParseException,TokenMgrError{
  //   Equation equation1 = 
  //     Parser.parseEquation("a*b+b*c+d*e=f",
  //                          Pair.of((List<String>)new ArrayList<String>(), 0), 
  //                          new ArrayList<Double>());
  //   System.out.println("Equation: " + equation1);
  //   MappedTerm mtEq = new MappedTerm(equation1, false);
  //   //mtEq.resort();
  //   System.out.println("MT: " + mtEq);
  //   System.out.println("term: " + mtEq.term);
  //   System.out.println("Sig: " + mtEq.term.recalcSignature());
  //   System.out.println("Sigm: " + mtEq.aUniqueSlotSignatures[0]);
  //   System.out.println("Sign: " + mtEq.aUniqueSlotSignatures[1]);
  //   System.out.println("Sigo: " + mtEq.aUniqueSlotSignatures[2]);
  //   System.out.println("Sigp: " + mtEq.aUniqueSlotSignatures[3]);
  //   System.out.println("Sigq: " + mtEq.aUniqueSlotSignatures[4]);
  //   System.out.println("Sig01: " + mtEq.aUniqueSlotPairSignatures[0][1]);
  //   System.out.println("Sig02: " + mtEq.aUniqueSlotPairSignatures[0][2]);
  //   System.out.println("Sig03: " + mtEq.aUniqueSlotPairSignatures[0][3]);
  //   System.out.println("Sig12: " + mtEq.aUniqueSlotPairSignatures[1][2]);
  //   System.out.println("Sig13: " + mtEq.aUniqueSlotPairSignatures[1][3]);
  //   System.out.println("Sig23: " + mtEq.aUniqueSlotPairSignatures[2][3]);
  // }

  static void testTerms3() throws ParseException,TokenMgrError{
    Config.load();
    ConcreteSystem system1 =
      Parser.parseSystem(Arrays.asList("a*b+b*d+e*f=g","h=a"));
    ConcreteSystem system2 =
      Parser.parseSystem(Arrays.asList("a*b+b*d+e*f=g","h=e"));
    ConcreteTerm ctSystem1 = new ConcreteTerm(system1);
    ConcreteTerm ctSystem2 = new ConcreteTerm(system2);
    String sSig1 = ctSystem1.mt.recalcSignature();
    String sSig2 = ctSystem2.mt.recalcSignature();
    String sConcreteSig1 = ctSystem1.getSignature();
    String sConcreteSig2 = ctSystem2.getSignature();
    System.out.println("Sys1: " + system1);
    System.out.println("Sys2: " + system2);
    System.out.println("FT1: " + ctSystem1.mt);
    System.out.println("FT2: " + ctSystem2.mt);
    System.out.println("Sig1: " + sSig1);
    System.out.println("Sig2: " + sSig2);
    System.out.println("EqualSig: " + sSig1.equals(sSig2));
    System.out.println("ConcreteSig1: " + sConcreteSig1);
    System.out.println("ConcreteSig2: " + sConcreteSig2);
    System.out.println("EqualConcrete: " + sConcreteSig1.equals(sConcreteSig2));
    System.out.println("T1: " + 
                       Arrays.toString(ctSystem1.mt.aUniqueSubTermSignatures[0]));
    System.out.println("T2: " + 
                       Arrays.toString(ctSystem1.mt.aUniqueSubTermSignatures[1]));
  }
  
  static void testTermsFromFile() throws ParseException,TokenMgrError{
    String sFilename = "data/test-equivalent-equations.json";
    List<List<List<String>>> lllTests = Misc.readListListListStrings(sFilename);
    for(List<List<String>> llTest : lllTests){
      List<String> lSigs = new ArrayList<String>();
      for(List<String> lSystem : llTest){
        ConcreteSystem system = Parser.parseSystem(lSystem);
        ConcreteTerm ctSystem = new ConcreteTerm(system);
        String sSig = ctSystem.mt.recalcSignature();
        lSigs.add(sSig);
      }
      //now make sure they're all equal
      if(new HashSet<String>(lSigs).size() != 1){
        System.out.println("Bad Sigs: " + lSigs);
        Misc.Assert(false);
      }
      System.out.println("ALL EQUAL");
    }
  }

  static void testTerms4() throws ParseException,TokenMgrError{
    Config.load();
    ConcreteSystem system =
      //Parser.parseSystem(Arrays.asList("a=2+c/6+4","a+f=7-3"));
      Parser.parseSystem(Arrays.asList("a+b=3","a+b=4"));
    ConcreteTerm ct = new ConcreteTerm(system);
    System.out.println("CS:\n" + system);
    System.out.println("FT:\n" + ct);
    System.out.println("CONCRETE: " + ct.getSignature());
    System.out.println("MT:\n" + ct.mt);
    String[] aSigs01 = ct.mt.aUniqueSubTermPairSignatures[0][1];
    String[] aSigs02 = ct.mt.aUniqueSubTermPairSignatures[0][2];
    String[] aSigs03 = ct.mt.aUniqueSubTermPairSignatures[0][3];
    String[] aSigs04 = ct.mt.aUniqueSubTermPairSignatures[0][4];
    String[] aSigs05 = ct.mt.aUniqueSubTermPairSignatures[0][5];

    String sSigs01 = ct.mt.aUniqueSlotPairSignatures[0][1];
    String sSigs02 = ct.mt.aUniqueSlotPairSignatures[0][2];
    String sSigs03 = ct.mt.aUniqueSlotPairSignatures[0][3];
    String sSigs04 = ct.mt.aUniqueSlotPairSignatures[0][4];
    String sSigs05 = ct.mt.aUniqueSlotPairSignatures[0][5];
    System.out.println("NumUnique: " + ct.mt.numUniqueSlots() + " " +
                       " Unknown: " + ct.mt.mapUnknowns.iNumUnique + " " +
                       " Nums: " + ct.mt.mapNumbers.iNumUnique);
    System.out.println("MT-PAIR01: " + Arrays.toString(aSigs01));
    System.out.println("MT-PAIR02: " + Arrays.toString(aSigs02));
    System.out.println("MT-PAIR03: " + Arrays.toString(aSigs03));
    System.out.println("MT-PAIR04: " + Arrays.toString(aSigs03));
    System.out.println("MT-PAIR05: " + Arrays.toString(aSigs03));

    System.out.println("MT-SPAIR01: " + sSigs01);
    System.out.println("MT-SPAIR02: " + sSigs02);
    System.out.println("MT-SPAIR03: " + sSigs03);
    System.out.println("MT-SPAIR04: " + sSigs03);
    System.out.println("MT-SPAIR05: " + sSigs03);

    // System.out.println("MT-0: " + ct.mt.aUniqueSlotSignatures[0]);
    // System.out.println("MT-1: " + ct.mt.aUniqueSlotSignatures[1]);
    // System.out.println("MT-2: " + ct.mt.aUniqueSlotSignatures[2]);
    // System.out.println("MT-3: " + ct.mt.aUniqueSlotSignatures[3]);
    // System.out.println("MT-4: " + ct.mt.aUniqueSlotSignatures[4]);
    // System.out.println("MT-5: " + ct.mt.aUniqueSlotSignatures[5]);
  }

  static void testTerms5() throws ParseException,TokenMgrError{
    Config.load(true);
    ConcreteSystem system =
      Parser.parseSystem(Arrays.asList("m+n+o=5","n-m=6"));
    system.system.setNumUses();
    ConcreteTerm ct = new ConcreteTerm(system);
    System.out.println("MT: " + ct.mt.toString());
    int iNumSlots = 5;
    for(int iSlot = 0; iSlot < iNumSlots; iSlot++){
      System.out.println("MT" + iSlot + ": " 
                         + ct.mt.aSimpleSlotSignatures[iSlot]);
    }
    for(int iSlot1 = 0; iSlot1 < iNumSlots; iSlot1++){
      for(int iSlot2 = 0; iSlot2 < iNumSlots; iSlot2++){
        System.out.println("MT" + iSlot1 + iSlot2 + ": " 
                           + ct.mt.aSimpleSlotPairSignatures[iSlot1][iSlot2]);
      }
    }
    for(int iSlot = 0; iSlot < iNumSlots; iSlot++){
      System.out.println("MTSUB" + iSlot + ": " + 
                         Arrays.toString(ct.mt.aSimpleSubTermSignatures[iSlot]));
    }
    for(int iSlot1 = 0; iSlot1 < iNumSlots; iSlot1++){
      for(int iSlot2 = 0; iSlot2 < iNumSlots; iSlot2++){
        String sSig = 
          Arrays.toString(ct.mt.aSimpleSubTermPairSignatures[iSlot1][iSlot2]);
        System.out.println("MTSUB" + iSlot1 + iSlot2 + ": " + sSig);
      }
    }

    System.out.println("SIGS");
    for(int iSlot1 = 0; iSlot1 < iNumSlots; iSlot1++){
      System.out.println("SINGLESIGS-" + iSlot1 + ": " 
                         + ct.mt.getSlotSignatures(iSlot1));
      for(int iSlot2 = 0; iSlot2 < iNumSlots; iSlot2++){
        System.out.println("PairSIGS-" + iSlot1 + iSlot2 + ": " 
                           + ct.mt.getSlotSignatures(iSlot1,iSlot2));
      }
    }
  }

  static void testTerms6() throws ParseException,TokenMgrError{
    Config.load(true);
    ConcreteSystem system =
      Parser.parseSystem(Arrays.asList("nate+bob=5","nate-bob=6"));
    system.system.setNumUses();
    ConcreteTerm ct = new ConcreteTerm(system);
    System.out.println("CT: " + ct.toDebugString());
  }

  static void testUnknownMatch(){
    Model.load(null, 0, true);
    Question question = Question.getQuestion(15, Model.lAllQuestions);
    // System.out.println("Sys: " + question.system);
    // System.out.println("Sys: " + question.system.system);
    // System.out.println("CT: " + question.ct);
    // System.out.println("MT: " + question.ct.mt);
    // System.out.println("Indexes: " + 
    //                    question.ct.mt.mapUnknowns.lOriginalIndexes);
    List<FilledTerm> lFilledTerm = 
      TermClassifier.computeAllCorrect(question, false, new FeatureCache(),
                                       Question.buildSolvers());
    for(FilledTerm ft : lFilledTerm){
      System.out.println("FT: " + ft.toString());
    }
    System.out.println("Num: " + lFilledTerm.size());
  }
 
  static void printDepPath(StanfordWord word1, StanfordWord word2){
    if(word1 == word2){
      System.out.println("DepPaths: " + word1 + " " + word2 
                         + "--> SAME WORD");
    } else if(word1.sentence.iIndex == word2.sentence.iIndex){
      System.out.println("DepPaths: " + word1 + " " + word2);
      System.out.println(word1.sentence
                         .aPaths[word1.iIndex][word2.iIndex]);
    } else {
      System.out.println("DepPaths: " + word1 + " " + word2 
                         + "--> DIFFERENT SENTENCES");
    }
  }

  static void printDepPaths(FilledTerm ft){
    System.out.println("FT: " + ft);
    System.out.println("MT: " + ft.mt);
    printDepPath(ft.lUnknownWords.get(0),
                 ft.lNumberWords.get(0).number.wordNoun);
    printDepPath(ft.lUnknownWords.get(1), 
                 ft.lNumberWords.get(1).number.wordNoun);
  }
  

  static void printFeatures(){
    Model.load(null,0,true);
    for(Question question : Model.lAllQuestions){
      if(question.bGoldUnknowns){
        System.out.println("Question: " + question.toFullString());
        List<FilledTerm> lFilledTerm = 
          TermClassifier.computeAllCorrect(question, false, new FeatureCache(),
                                           Question.buildSolvers());
        if(lFilledTerm.size() >=5){
          System.out.println("Bad Size: " + lFilledTerm.size());
          System.out.println("List: " + lFilledTerm);
        }
        Misc.Assert(lFilledTerm.size() < 5);
        for(FilledTerm ft : lFilledTerm){
          printDepPaths(ft);
        }
      }
    }
  }

  static void printGoldUnknownIndexes(){
    Model.load(null,0,true);
    for(Question question : Model.lAllQuestions){
      if(question.bGoldUnknowns){
        System.out.println(question.iIndex);
      }
    }
  }

  static void printHowManyObjs(){
    Model.load(null,0,true);
    for(Question question : Model.lAllQuestions){
      List<StanfordWord> lHowManyObjs = 
        Model.features.derivationfeatures.getHowManyObjs(question.doc);
      if(lHowManyObjs.size() != 0){
        System.out.println("Question: " + question.sQuestion);
        System.out.println(lHowManyObjs);
      }
    }
    System.out.println("NumQuestions: " + Model.lAllQuestions.size());
  }

  static void printFeatures2(){
    Model.load(null,0,true);
    for(Question question : Model.lAllQuestions){
      if(true){
      //if(question.iIndex == 44){
      //if(question.bGoldUnknowns && (question.iIndex == 17)){
        System.out.println("Question: " + question.toFullString());
        List<FilledTerm> lFilledTerm = 
          TermClassifier.computeAllCorrect(question, false, new FeatureCache(),
                                           Question.buildSolvers());
        if(lFilledTerm.size() >= 10){
          System.out.println("Bad Size: " + lFilledTerm.size());
          System.out.println("List: " + lFilledTerm);
          Misc.Assert(lFilledTerm.size() < 10);
        }
        FeatureCache cache = new FeatureCache();
        for(FilledTerm ft : lFilledTerm){
          System.out.println("FT: " + ft.toString());
          FeatureListList fll = 
            Model.features.derivationfeatures.getSimpleFeatures(ft,question,
                                                                cache,
                                                                Question.buildSolvers());
          System.out.println(fll.toString());
        }
      }
    }
  }
  
  static void printNPs(){
    Model.load(null, 0, true);
    Question question = Question.getQuestion(1948, Model.lAllQuestions);
    System.out.println(question.sQuestion);
    for(StanfordWord wordNoun : question.doc.lNouns){
      System.out.println(wordNoun.sWordOrig + " : " + wordNoun.getSubTree());
    }
  }

  static void printTerms(){
    Model.load(null, 0, true);
    Question question = Question.getQuestion(5163, Model.lAllQuestions);
    List<FilledTerm> lFilledTerm = 
      TermClassifier.computeAllCorrect(question, false, new FeatureCache(),
                                       Question.buildSolvers());
    FilledTerm ft = lFilledTerm.get(0);
    ft.buildSlots();
    System.out.println("FT: " + ft.toString());
    for(Slot slot : ft.lSlots){
      System.out.println(slot);
    }
  }

  static void loadQuestions(){
    Model.load(null, 0, true);
  }

  static void checkWords(){
    Model.load(null, 0, true);
    for(Question question : Model.lAllQuestions){
      for(StanfordSentence sentence : question.doc.lSentences){
        for(StanfordWord word : sentence.lWords){
          if(word.isRootParent()){
            continue;
          }
          if(word.isRoot()){
            continue;
          }
          if((word.number != null) && (word.number.fNumber != 1.0)){
            continue;
          }
          if(word.ptParent == null){
            continue;
          }
          if(word.ptParent.ptParent == null){
            continue;
          }
          if(word.sPos.equals("JJ")){
            StanfordParseTree ptNonTerm = word.ptParent.ptParent;
            if(ptNonTerm.sNonTerminal.equals("NP")){
              if((ptNonTerm.lChildren.size() == 2)){
                //check that the first child is a dt
                StanfordParseTree ptOther = ptNonTerm.lChildren.get(0);
                if(ptOther.word != null){
                  if(ptOther.word.sPos.equals("DT")){
                    System.out.println("Sentence: " + sentence.sSentenceOrig);
                    System.out.println("Word: " + word);
                  }
                }
              }
            }
          }
          // if((word.sPos.equals("CD") || word.sPos.equals("DT"))){
          //   if(word.ptParent == null){
          //     continue;
          //   }
          //   if(word.ptParent.ptParent == null){
          //     continue;
          //   }
          //   if(word.ptParent.ptParent.sNonTerminal.equals("NN") ||
          //      word.ptParent.ptParent.sNonTerminal.equals("NP")){
          //     if(word.ptParent.ptParent.lChildren.size() == 1){

          //       System.out.println("Sentence: " + sentence.sSentenceOrig);
          //       System.out.println("Word: " + word);
          //       System.out.println("Parent: " + 
          //                          word.ptParent.ptParent.sNonTerminal);
          //     }
          //   }
          // }
          // if((word.sPos.equals("CD") || word.sPos.equals("DT")) &&
          //    (word.lDeps.size() != 0)){
          //   if((word.number != null) && (word.number.fNumber != 1.0)){
          //     continue;
          //   }
          //   System.out.println("Sentence: " + sentence.sSentenceOrig);
          //   System.out.println("Word: " + word);
          //   if(word.ptParent == null){
          //     System.out.println("NoParent");
          //   } else {
          //     if(word.ptParent.ptParent == null){
          //       System.out.println("NoParentParent");
          //     } else {
          //       System.out.println("HasParent: " + 
          //                          word.ptParent.ptParent.sNonTerminal);
          //     }
          //   }
          // }
        }
      }
    }
  }

  static void printSlots(){
    Model.load(null, 0, true);
    Question question = Question.getQuestion(6102, Model.lAllQuestions);
    List<FilledTerm> lFilledTerm = 
      TermClassifier.computeAllCorrect(question, false, new FeatureCache(),
                                       Question.buildSolvers());
    for(FilledTerm ft : lFilledTerm){
      System.out.println("FilledTerm: " + ft);
      for(StanfordWord word : ft.lNumberWords){
        System.out.println("Word: " + word + " Noun: " + word.number.wordNoun);
      }
    }
  }

  static void checkAllQuestions(){
    Model.load(null, 0, false);
    List<SimpleSolver> lSolvers = Question.buildSolvers();
    for(Question question : Model.lAllQuestions){
      List<Double> lCalced = question.ct.solve(lSolvers);
      if(lCalced == null){
        System.out.println("Question-" + question.iIndex + ": INVALID"); 
      } else {
        boolean bFoundError = false;
        for(Double fSolution : question.lSolutions){
          boolean bFoundCorr = false;
          for(Double fCalced : lCalced){
            if(Question.solutionClose(fSolution, fCalced)){
              bFoundCorr = true;
              break;
            }
          }
          if(!bFoundCorr){
            bFoundError = true;
            break;
          }
        }
        if(bFoundError){
          System.out.println("Question-" + question.iIndex + ": WRONG" + 
            " Sol: " + question.lSolutions + " Calced: " + lCalced); 
          System.out.println("Concrete: " + question.system);
          System.out.println("CT: " + question.ct);
          System.out.println("Solutions: " + question.lSolutions);
          System.out.println("Calced: " + lCalced);
          System.out.println("TestResult: " + question.test(lSolvers));
          //now print out the unique ids
          List<MappedTerm> lMappedTerms = question.ct.mt.getTerminals();
          for(MappedTerm mt : lMappedTerms){
            System.out.println("Term: " + mt.term.iUniqueId + 
                               " Type: " + mt.term.type);
          }
          //Misc.Assert(false);
        } else {

          System.out.println("Question-" + question.iIndex + ": WRONG" +
                  " Sol: " + question.lSolutions + " Calced: " + lCalced);
          System.out.println("Concrete: " + question.system);
          System.out.println("CT: " + question.ct);
          System.out.println("Solutions: " + question.lSolutions);
          System.out.println("Calced: " + lCalced);
          System.out.println("TestResult: " + question.test(lSolvers));
          //now print out the unique ids
          List<MappedTerm> lMappedTerms = question.ct.mt.getTerminals();
          for(MappedTerm mt : lMappedTerms){
            System.out.println("Term: " + mt.term.iUniqueId +
                    " Type: " + mt.term.type);
          }
        }
      }
    }
  }

  static void computeDataStats(){
    Model.load(null, 0, true);
    int iProblems = Model.lAllQuestions.size();
    int iSentences = 0;
    int iWords = 0;
    int iNumEquations = 0;
    int[] aNumEquations = new int[10];
    int iNumSlots = 0;
    int iNumNouns = 0;
    Set<String> setWordTypes = new HashSet<String>();
    Set<String> setSystems = new HashSet<String>();
    for(Question question : Model.lAllQuestions){
      iNumNouns += question.doc.lNouns.size();
      setSystems.add(question.ct.mt.toString(false,true));
      int iNumEquationsCur = question.ct.mt.term.lSubTerms.size();
      aNumEquations[iNumEquationsCur]++;
      iNumEquations += iNumEquationsCur;
      iNumSlots += question.ct.mt.mapUnknowns.numInstances();
      iNumSlots += question.ct.mt.mapNumbers.numInstances();
      for(StanfordSentence sentence : question.doc.lSentences){
        iSentences++;
        for(StanfordWord word : sentence.lWords){
          if(!word.isRootParent()){
            iWords++;
            setWordTypes.add(word.sWordOrig);
          }
        }
      }
    }    
    System.out.println("Problems: " + iProblems);
    System.out.println("Sentences: " + iSentences);
    System.out.println("Words: " + iWords);
    System.out.println("Word Types: " + setWordTypes.size());
    System.out.println("NumEquations: " + iNumEquations);
    System.out.println("NumEquations: " + Arrays.toString(aNumEquations));
    System.out.println("SystemTypes: " + setSystems.size());
    System.out.println("NumSlots: " + iNumSlots);
    System.out.println("NumNouns: " + iNumNouns);
  }

  static void computeSystemStats(){
    Model.load(null, 0, true);
    CountMap<String> mCounts = new CountMap<String>();
    Set<String> setSigs = new HashSet<String>();
    for(Question question : Model.lAllQuestions){
      mCounts.increment(question.ct.mt.toString(false,true));
      setSigs.add(question.ct.mt.getSignature());
    }
    CountMap<Integer> mCountCounts = new CountMap<Integer>();
    for(Integer iCount : mCounts.values()){
      mCountCounts.increment(iCount);
    }
    for(Map.Entry<Integer,Integer> entry : mCountCounts.entrySet()){
      System.out.println("Count: " + entry.getKey() + " " + entry.getValue());
    }
    System.out.println("NumCounts: " + mCounts.size());
    System.out.println("NumSigs: " + setSigs.size());
  }
      
  static void printStopWords(){
    Config.load(true);
    System.out.println("Stop: " + Config.config.lStopWords);
  }

  static void printCommonWords(){
    Model.load(null, 0, true);
    CountMap<Object> mCounts = new CountMap<Object>();
    for(Question question : Model.lAllQuestions){
      mCounts.incrementAll(question.lNGrams);
    }
    //now sort
    List<Map.Entry<Object,Integer>> lEntries = 
      new ArrayList<Map.Entry<Object,Integer>>(mCounts.entrySet());
    Comparator<Map.Entry<Object,Integer>> comparator =
      new Misc.MapEntryValueComparator<Object,Integer>();
    Collections.sort(lEntries, Collections.reverseOrder(comparator));
    //now print the top 20
    for(Map.Entry<Object,Integer> entry: lEntries){
      System.out.println("Entry: " + entry);
    }
  }

  static void printAllTemplates(){
    Model.load(null, 0, true);
    Set<String> setSystems = new HashSet<String>();
    for(Question question : Model.lAllQuestions){
      String sSig = question.ct.mt.toString(false,true);
      boolean bNew = setSystems.add(sSig);
      if(bNew){
        System.out.println(question.system);
      }
    }    
  }

  static void findTopExamples() throws IOException{
    Model.load(null, 0, true);
    CountMap<String> mCounts = new CountMap<String>();
    Map<Integer,Question> mQuestions = new HashMap<Integer,Question>();
    for(Question question : Model.lAllQuestions){
      mCounts.increment(question.ct.mt.toString(false,true));
      mQuestions.put(question.iIndex, question);
    }
    List<Map.Entry<String,Integer>> lEntries = 
      new ArrayList<Map.Entry<String,Integer>>(mCounts.entrySet());
    Comparator<Map.Entry<String,Integer>> comparator =
      new Misc.MapEntryValueComparator<String,Integer>();
    Collections.sort(lEntries,Collections.reverseOrder(comparator));
    //take the top 5
    Set<String> setSystems = new HashSet<String>();
    for(Map.Entry<String,Integer> entry : lEntries.subList(0,5)){
      setSystems.add(entry.getKey());
    }
    
    for(int iFold = 0; iFold < 5; iFold++){
      Set<String> setFoundSystems = new HashSet<String>();
      //load the training numbers
      List<Integer> lTrain = new ArrayList<Integer>();
      for(int iTrainFold = 0; iTrainFold < 5; iTrainFold++){
        if(iTrainFold == iFold){
          continue;
        }
        lTrain.addAll(Misc.loadIntList("data/indexes-1-fold-" + iTrainFold +
                                       ".txt"));
      }
      //now find the first one of each type
      List<Integer> lHand = new ArrayList<Integer>();
      for(Integer iQuestion : lTrain){
        Question question = mQuestions.get(iQuestion);
        if(question.bGoldUnknownSystem && !question.bGoldUnknowns){
          continue;
        }
        String sSig = question.ct.mt.toString(false,true);
        if(setSystems.contains(sSig) && !setFoundSystems.contains(sSig)){
          lHand.add(question.iIndex);
          setFoundSystems.add(sSig);
        }
      }
      //now write out the hand
      FileUtils.writeLines(new File("data/hand-5-fold-" + iFold + ".txt"),
                           lHand);
    }
  }

  static void printQuestionsFromTemplates(){
    Model.load(null, 0, true);
    for(Question question : Model.lAllQuestions){
      String sSig = question.ct.mt.toString(false,true);
      // if(sSig.equals("-a+-m+n::-b+m+n::")){
      if(sSig.equals("(a*m)+(b*n)+-c::-d+m+n::")){
        System.out.println(question.toFullString());
      }
    }
  }

  static void printDataset(){
    Model.load(null, 0, true);
    for(Question question : Model.lAllQuestions){
      System.out.println(question.toFullString());
    }    
  }

  static void printUncommon(){
    CountMap<String> mCounts = new CountMap<String>();
    Model.load(null, 0, true);
    Map<String,List<Question>> mQuestions = new HashMap<String,List<Question>>();
    for(Question question : Model.lAllQuestions){
      String sSig = question.ct.mt.toString(false,true);
      mCounts.increment(sSig);
      List<Question> lQuestions = mQuestions.get(sSig);
      if(lQuestions == null){
        lQuestions = new ArrayList<Question>();
        mQuestions.put(sSig,lQuestions);
      }
      lQuestions.add(question);
    }  

    for(Map.Entry<String,Integer> entry : mCounts.entrySet()){
      if(entry.getValue() < 10){
        System.out.println("********************************");
        System.out.println("Template:" + entry.getKey() + " Count: " 
                           + entry.getValue());
        for(Question question : mQuestions.get(entry.getKey())){
          System.out.println(question.toFullString());
        }
      }
    }
  }

  static void printErrorQuestions(){
    Model.load(null, 0, true);
    List<Integer> lNums = Misc.loadIntList("errorquestions.txt");
    for(Integer iQuestion : lNums){
      Question question = Question.getQuestion(iQuestion, Model.lAllQuestions);
      System.out.println(question.toFullString());
    }
  }

  static void printQuestionType(String sQuestion){
    Model.load(null, 0, true);
    Question questionToFind = Question.getQuestion(Integer.parseInt(sQuestion), 
                                                   Model.lAllQuestions);
    String sSig = questionToFind.ct.mt.toString(false,true);
    for(Question question : Model.lAllQuestions){
      if(question.ct.mt.toString(false,true).equals(sSig)){
        System.out.println(question.toFullString());
      }
    }
  }

  static int permutations(int iNumTotal, int iNumChoose){
    int iPerms = 1;
    for(int iCur = iNumTotal; iCur > iNumTotal-iNumChoose; iCur--){
      iPerms *= iCur;
    }
    return iPerms;
  }

  static void printDerivationStats(){
    Model.load(null, 0, true);
    Set<MappedTerm> setTemplates = new HashSet<MappedTerm>();
    for(Question question : Model.lAllQuestions){
      setTemplates.add(question.ct.mt);
    }
    int iNumTotal =0;
    List<Integer> lNums = new ArrayList<Integer>();
    int iMax = 0;
    int iMin = Integer.MAX_VALUE;
    for(Question question : Model.lAllQuestions){
      int iNumCur = 0;
      for(MappedTerm mt : setTemplates){
        int iNumbers = permutations(question.doc.lNumbers.size(), 
                                    mt.mapNumbers.iNumUnique);
        int iNouns = (int) Math.pow(question.doc.lNumbers.size(), 
                              mt.mapUnknowns.numInstances());
        iNumCur += iNumbers*iNouns;
      }
      Misc.Assert(iNumCur != 0);
      iNumTotal += iNumCur;
      iMin = Math.min(iMin, iNumCur);
      iMax = Math.max(iMax, iNumCur);
      lNums.add(iNumCur);
    }
    Collections.sort(lNums);
    int iHalf = (int)(lNums.size()/2);
    System.out.println("Max: " +iMax);
    System.out.println("Min: " +iMin);
    System.out.println("Avg: " + iNumTotal/Model.lAllQuestions.size());
    System.out.println("Median: " + lNums.get(iHalf));
  }


  public static void computeUniqueSystems(){
    Model.load(null, 0, true);
    Set<MappedTerm> setTemplates = new HashSet<MappedTerm>();
    for(Question question : Model.lAllQuestions){
      setTemplates.add(question.ct.mt);
    }
    System.out.println("NumTemplates: " + setTemplates.size() + 
                       " question: " + Model.lAllQuestions.size());
  }

  public static void writeEquivalenceSets() throws IOException{
    Model.load(null, 0, true);
    Map<String, List<Integer>> mSets = new HashMap<String,List<Integer>>();
    for(Question question : Model.lAllQuestions){
      String sSig = question.ct.mt.recalcSignature();
      List<Integer> lEquivalent = mSets.get(sSig);
      if(lEquivalent == null){
        lEquivalent = new ArrayList<Integer>();
        mSets.put(sSig, lEquivalent);
      }
      lEquivalent.add(question.iIndex);
    }
    List<List<Integer>> llEquivalent = 
      new ArrayList<List<Integer>>(mSets.values());
    Type type = new TypeToken<List<List<Integer>>>(){}.getType();
    String sOut = new Gson().toJson(llEquivalent, type);
    FileUtils.writeStringToFile(new File("test.json"), sOut);
  }
  
  static void calcNumberReusedNumbers(){
    Model.load(null, 0, true);
    Set<MappedTerm> setTemplates = new HashSet<MappedTerm>();
    for(Question question : Model.lAllQuestions){
      setTemplates.add(question.ct.mt);
    }
    int iNumReused = 0;
    for(MappedTerm mt : setTemplates){
      if(mt.mapNumbers.iNumUnique != mt.mapNumbers.numInstances()){
        System.out.println("Reuse: " + mt.toString(false,true));
        iNumReused++;
      }
    }
    System.out.println("Num Reused: " + iNumReused);
  }

  static void genUniqueTemplateFile(){
    Model.load(null, 0, true);
    CountMap<MappedTerm> mCounts = new CountMap<MappedTerm>();
    //MappedTerm mt5281 = null;
    for(Question question : Model.lAllQuestions){
      mCounts.increment(question.ct.mt);
      // if(question.iIndex == 5281){
      //   mt5281 = question.ct.mt;
      // }
    }

    Set<MappedTerm> setTemplates = new HashSet<MappedTerm>();
    List<QuestionText> lUnique = new ArrayList<QuestionText>();
    for(Question question : Model.lAllQuestions){
      if(!setTemplates.contains(question.ct.mt)){
      //if(question.ct.mt.equals(mt5281)){
        lUnique.add(question.qt);
        setTemplates.add(question.ct.mt);
        System.out.println("Index: " + question.iIndex + " Count: " +
                           mCounts.get(question.ct.mt));
      }
      System.out.println("Num: " + question.iIndex);
    }
    QuestionText.writeListToJson(lUnique, "unique.json");
  }

  static void genNewUniqueTemplateFile(){
    Model.load(null, 0, true);
    Map<MappedTerm,List<QuestionText>> mMap = 
      new DefaultMap<MappedTerm,List<QuestionText>>(ArrayList.class);
    List<Integer> lCorrect = Misc.loadIntList("data/final-results-correct.txt");
    for(Question question : Model.lAllQuestions){
      question.qt.bCorrect = lCorrect.contains(question.iIndex);
      mMap.get(question.ct.mt).add(question.qt);
    }
    int iIndex =  0;
    for(List<QuestionText> lQuestionText : mMap.values()){
      QuestionText.writeListToJson(lQuestionText, "unique-" + iIndex + ".json");
      iIndex++;
    }
  }

  static void printSingleQueryVarProblems(){
    Model.load(null, 0, true);
    List<QuestionText> lQt = new ArrayList<QuestionText>();
    for(Question question : Model.lAllQuestions){
      if((question.qt.lEquations.size() == 2) && 
         (question.qt.lQueryVars.size() == 1)){
        lQt.add(question.qt);
      }
    }
    QuestionText.writeListToJson(lQt, "test.json");
  }

  static boolean sameOrder(List<Double> lCorrect, List<Double> lQuestion, 
                    List<Integer> lMostCommon){
    for(int iCur = 0; iCur < lMostCommon.size(); iCur++){
      Double fCorrect = lCorrect.get(iCur);
      int iIndex = lMostCommon.get(iCur);
      if(iIndex >= lQuestion.size()){
        return false;
      }
      Double fGuess = lQuestion.get(iIndex);
      if(!fGuess.equals(fCorrect)){
        return false;
      }
    }
    return true;
  }


  static void majorityBaseline(String[] args){
    int iFold = Integer.parseInt(args[0]);
    Model.load(null, iFold, true);
    CountMap<MappedTerm> mCounts = new CountMap<MappedTerm>();
    for(Question question : Model.lTrainQuestions){
      //find the common system
      mCounts.increment(question.ct.mt);
    }    
    //find the most common one
    MappedTerm mtCommon = null;
    int iMostCommon = -1;
    for(Map.Entry<MappedTerm,Integer> entry : mCounts.entrySet()){
      if(entry.getValue() > iMostCommon){
        iMostCommon = entry.getValue();
        mtCommon = entry.getKey();
      }
    }
    //now compute the most common ordering
    Map<MappedTerm, CountMap<List<Integer>>> mOrderings = 
      new HashMap<MappedTerm, CountMap<List<Integer>>>();
    for(Question question : Model.lTrainQuestions){
      CountMap<List<Integer>> mCurCounts = mOrderings.get(question.ct.mt);
      if(mCurCounts == null){
        mCurCounts = new CountMap<List<Integer>>();
        mOrderings.put(question.ct.mt, mCurCounts);
      }
      //compute the ordering
      List<Double> lCorrect = question.ct.lNumberDoubles;
      List<Double> lProblem = question.doc.lDoubles;
      List<Integer> lOrdering = new ArrayList<Integer>();
      for(Double fCur : lCorrect){
        int iIndex = lProblem.indexOf(fCur);
        Misc.Assert(iIndex != -1);
        lOrdering.add(iIndex);
      }
      mCurCounts.increment(lOrdering);
    }
    // now find the most common ordering for each type
    Map<MappedTerm, List<Integer>> mMostCommon = 
      new HashMap<MappedTerm, List<Integer>>();
    for(Map.Entry<MappedTerm, CountMap<List<Integer>>> entryCur : 
          mOrderings.entrySet()){
      List<Integer> lMostCommon = null;
      iMostCommon = -1;
      for(Map.Entry<List<Integer>,Integer> entry : 
            entryCur.getValue().entrySet()){
        if(entry.getValue() > iMostCommon){
          iMostCommon = entry.getValue();
          lMostCommon = entry.getKey();
        }
      }
      mMostCommon.put(entryCur.getKey(), lMostCommon);
    }
    // now test how many we get correct
    int iNumCorrectMajority = 0;
    int iNumCorrectOracle = 0;
    for(Question question : Model.lTestQuestions){
      boolean bCorrect = sameOrder(question.ct.lNumberDoubles, 
                                   question.doc.lDoubles,
                                   mMostCommon.get(question.ct.mt));
      if(bCorrect){
        iNumCorrectOracle++;
        if(question.ct.mt.equals(mtCommon)){
          iNumCorrectMajority++;
        }
      }
    }
    System.out.println("Correct: " + Misc.div(iNumCorrectMajority, 
                                              Model.lTestQuestions.size())
                       + Misc.div(iNumCorrectOracle, 
                                  Model.lTestQuestions.size())
                       + " " + iNumCorrectMajority + " " + iNumCorrectOracle
                       + " " + Model.lTestQuestions.size());
  }


  static void numThings(){
    //num things
    Model.load(null, 0, true);
    //num unique systems
    Set<String> setSystems = new HashSet<String>();
    Set<String> setOrig = new HashSet<String>();
    for(Question question : Model.lAllQuestions){
      setSystems.add(question.ct.mt.toString(false,true));
      setOrig.add(question.system.system.toString());
    }
    System.out.println("Num: " + setSystems.size());
    System.out.println("Orig: " + setOrig.size());
  }

  static void printDifferences(){
    //num things
    Model.load(null, 0, true);
    //num unique systems
    for(Question question1 : Model.lAllQuestions){
      for(Question question2 : Model.lAllQuestions){
        if(question1.ct.mt.toString(false,true).equals(question2.ct.mt.toString(false,true)) && 
           !question1.system.system.toString().equals(question2.system.system.toString())){
          System.out.println("Diff: " + question1.iIndex + " " + 
                             question2.iIndex);
          System.out.println("  " + question1.ct.mt.toString(false,true));
          System.out.println("  " + question1.system.system.toString());
          System.out.println("  " + question2.system.system.toString());
        }
      }
    }
  }


  static void calcRepeats(){
    Config.load(true);
    List<Question> lQuestions = Question.loadAllValidWithEquations();
    System.out.println("Num Questions: " + lQuestions.size());
    CountMap<String> mSystems = new CountMap<String>();
    CountMap<String> mOrig = new CountMap<String>();
    
    for(Question question : lQuestions){
      mSystems.increment(question.ct.mt.toString(false,true));
      mOrig.increment(question.system.system.toString());
    }
    System.out.println("Num: " + mSystems.size());
    System.out.println("Orig: " + mOrig.size());
    int iRepeatSystem = 0;
    for(Integer iCount : mSystems.values()){
      if(iCount > 1){
        iRepeatSystem++;
      }
    }
    int iRepeatOrig = 0;
    for(Integer iCount : mOrig.values()){
      if(iCount > 1){
        iRepeatOrig++;
      }
    }
    System.out.println("RepeatNum: " + iRepeatSystem);
    System.out.println("RepeatOrig: " + iRepeatOrig);
  }

  public static void main(String[] args) throws Exception{
    
    //testReader();
    //testLogAdd();
    //testNumbers();
    //testParser();
    //testNumber();
    //diffTester();
    //testLists();
    //testProbs();
    //testPaths();
    //testPaths2();
    //testImmutableList();
    //testEquBeam();
    //testSingleEqu();
    //testJson2();
    //testTerms6();
    //testTermsFromFile();
    ///testUnknownMatch();
    //printFeatures2();
    //printGoldUnknownIndexes();
    //printHowManyObjs();
    //loadQuestions();
    //printNPs();
    //printWords();
    //printTerms();
    //checkWords();
    //printSlots();
    //checkAllQuestions();
    //computeDataStats();
    //printStopWords();
    //printCommonWords();
    //printAllTemplates();
    //findTopExamples();
    //computeSystemStats();
    //printQuestionsFromTemplates();
    //printDataset();
    //printUncommon();
    //printErrorQuestions();
    //printQuestionType(args[0]);
    //printQuestionTypes();
    printDerivationStats();
    //printStopWords();
    //computeUniqueSystems();
    //writeEquivalenceSets();
    //calcNumberReusedNumbers();
    //genNewUniqueTemplateFile();
    //printSingleQueryVarProblems();
    //majorityBaseline(args);
    //numThings();
    //calcRepeats();
    //printDifferences();
  }
}
