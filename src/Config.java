import java.util.*;
import java.io.*;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

class Config{
  static Config config;

  public Map<String,Double> mNumbers = new HashMap<String,Double>();

  //actual config variables
  int iMaxNumFeatures = 50000000;
  double fDefaultFeatureWeight = 0.0;
  double fAlpha = 1.0;
  double fLambda= 0.1;
  boolean bLogScores = true;
  String sFullQuestionFile;
  boolean bTestOneQuestion = false;
  int iTestQuestionNumber = 0;
  String sStanfordParseDirectory;
  int iNumIters = 5000;
  String sQuestionCvsFile = null;
  String sTrainQuestionNumberFile = null;
  String sTestQuestionNumberFile = null;
  String sHandAnnotationFile = null;
  int iPercentageEquationAnnotations;
  String sFoldFilePrefix;
  int iNumFolds;
  boolean bUseFoldData = true;
  boolean bParallelize = false;
  int iNumThreads;
  boolean bUseCheatFeature = false;
  boolean bLoadNGrams = true;
  int iMinEquationCount = 0;
  boolean bPrintBarsAroundConstants = false;
  boolean bPruneToTenQuestionsForTesting = true;
  boolean bPrintErrorAnalysis = true;
  boolean bPrintCorrectErrorAnalysis = true;
  boolean bUseOnlyDebugFeatures = false;
  int iTestPeriod=1;
  boolean bUseExactWordFeatures = false;
  int iMaxEquFactors = 0;
  int iMaxEquTerms = 0;
  int iMaxBeamSize;
  boolean bPruneOutMultiQuestions = false;
  boolean bThirdOrderFeatures = false;
  boolean bPrintAllTotalNumbers = false;
  boolean bFeaturesPeerPath = false;
  boolean bFeaturesStructuralNulls = false;
  boolean bPrintPerQuestionStats = true;
  boolean bPruneOnFirstIter = false;
  boolean bPrintDuringQuestionLoad = false;
  int iMinSystemCount = 0;
  boolean bUseFudgeFactors = true;
  boolean bUseOffsetFeature = false;
  boolean bBrokenUnknownWordPerInstance = false;
  boolean bUnknownWordPerInstance = true;
  boolean bUseNonCanonicalSlotSigs = true;
  boolean bUseCanonicalSlotSigs = false;
  boolean bUseSubTermSigs = true;
  boolean bRequireUnknownStringOverlap = false;
  boolean bRequireUnknownStringOverlapForOnlyGold = false;
  boolean bUseOnlySubmissionA = false;
  boolean bUseOnlyStandard = false;
  boolean bUseOnlyGoldUnknowns = false;
  boolean bUseOnlyInterestStandard = false;
  boolean bUseOnlySolutionStandard = false;
  boolean bUseNewFeatures = true;
  boolean bUseOldNewFeatures = false;
  int iMaxNearbyDist = 5;
  boolean bUseOnlyGoldUnknownSystems = false;
  boolean bUseOnlyGoldUnknownsForTrain = false;
  boolean bPredictOnlyTemplates = false;
  boolean bPipeline = false;
  boolean bPrintCorrectSystems = true;
  boolean bLoadCorrectSystems = false;
  boolean bAssumeCorrectSystem = false;
  boolean bPrintFullStats = true;
  //old features
  boolean bDepthPathHasConjNeg = false;
  boolean bUseDistBins = false;
  boolean bUsePeerOverlapFeatures = false;
  boolean bUseNounPhraseOverlapFeatures = false;
  boolean bUseOnlyCommonFeatures = false;
  int iMinCommonFeatureCount = 0;
  boolean bUseNumberFeatures = false;
  int iMinNGramCount = 0;
  boolean bBinaryNGrams = true;
  List<String> lStopWords = Arrays.asList("the", "a");
  Set<String> setStopWords;
  boolean bFillSlotsInOrder = true;
  boolean bSemiSupervised = true;
  boolean bUseOnlyHandAnnotationQuestionsForTrain = false;
  boolean bUseOnlyHandAnnotationQuestionsAnnotatedForTrain = true;
  boolean bCalcCorrectUsingBeam = true;
  int iMaxNumPerSystemInBeam;
  boolean bLoadHandSignatures = false;
  boolean bUseFullHandAnnotations = true;
  boolean bNormalizeEquations = true;
  //new features
  boolean bUseRelationFeature = true;
  boolean bLexicalizeNouns = true;
  boolean bNGramFeatures = true;
  int iMaxNGram = 2;
  boolean bNGramOrig = false;
  boolean bNGramStems = true;
  boolean bNGramPos = false;
  boolean bPosSolutionsFeature = true;
  boolean bRoundSolutionsFeature = true;
  boolean bRemoveNumbersFromNGrams = true;
  boolean bUseDocumentFeatures = true;
  boolean bUseSingleFeatures = true;
  boolean bUsePairFeatures = true;
  boolean bRelationshipFeature = true;
  boolean bUseDepPathStems = false;
  boolean bUseAllDepPathFeatures = true;
  boolean bUseUnknownFeatures = true;
  //end config variables

  static {
    //config = load("config/config.json");
  }

  void loadNumbers(){
    mNumbers.put("hundred", 100.);
    mNumbers.put("thousand", 1000.);
    mNumbers.put("million", 1000000.);
    mNumbers.put("billion", 1000000000.);
    mNumbers.put("trillion", 1000000000000.);
    mNumbers.put("1\\/3", (1.0/3.0));
    mNumbers.put("1\\/2", 0.5);
    mNumbers.put("one", 1.);
    mNumbers.put("once", 1.);
    mNumbers.put("two", 2.);
    mNumbers.put("three", 3.);
    mNumbers.put("four", 4.);
    mNumbers.put("five", 5.);
    mNumbers.put("six", 6.);
    mNumbers.put("seven", 7.);
    mNumbers.put("eight", 8.);
    mNumbers.put("nine", 9.);
    mNumbers.put("ten", 10.);
    mNumbers.put("eleven", 10.);
    mNumbers.put("twelve", 10.);
    mNumbers.put("twenty", 20.);
    mNumbers.put("1", 1.);
    mNumbers.put("2", 2.);
    mNumbers.put("3", 3.);
    mNumbers.put("4", 4.);
    mNumbers.put("5", 5.);
    mNumbers.put("6", 6.);
    mNumbers.put("7", 7.);
    mNumbers.put("8", 8.);
    mNumbers.put("9", 9.);
    mNumbers.put("10", 10.);
    mNumbers.put("both", 2.);
  }

  static void load(){
    load(null, false);
  }

  static void load(boolean bSuppressPrint){
    load(null, bSuppressPrint);
  }

  static void load(String sFileName, boolean bSuppressPrint){
    if(sFileName == null){
      sFileName = "config/config.json";
    }
    try{
      File file = new File(sFileName);
      FileReader reader = new FileReader(file);
      config = new Gson().fromJson(reader, Config.class);
      config.loadNumbers();
      List<String> lConfig = FileUtils.readLines(file);
      if(!bSuppressPrint){
        System.out.println("************CONFIG***************");
        System.out.println("CONFIG: " +  sFileName);
        for(String sLine : lConfig){
          System.out.println(sLine);
        }
      }
      config.setStopWords = new HashSet<String>(config.lStopWords);
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }
  }
}
