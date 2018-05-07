import java.lang.*;
import java.util.*;
import java.io.*;
import org.apache.commons.lang3.tuple.*;

import org.apache.commons.io.FileUtils;

class EquationTester{
  static List<EquationSystem> getSystems(){
    System.out.println("NumQuestions: " + Model.lTrainQuestions.size());
    Set<EquationSystem> setSystems = new HashSet<EquationSystem>();
    for(Question question : Model.lTrainQuestions){
      setSystems.add(question.system.system);
    }
    System.out.println("NumSystems: " + setSystems.size());
    for(EquationSystem system : setSystems){
      System.out.println(system);
    }

    return new ArrayList<EquationSystem>(setSystems);
  }

  static void testNew(){
    Model.load();
    System.out.println("TrainSize: " + Model.lTrainQuestions.size());
    System.out.println("TestSize: " + Model.lTestQuestions.size());
    Map<EquationSystem, Integer> mCounts =new HashMap<EquationSystem,Integer>();
    for(Question question : Model.lTrainQuestions){
      Integer iCount = mCounts.get(question.system.system);
      if(iCount == null){
        mCounts.put(question.system.system, 1);
      } else {
        mCounts.put(question.system.system, iCount+1);
      }
    }
    List<Map.Entry<EquationSystem,Integer>> lEntries = 
      new ArrayList<Map.Entry<EquationSystem,Integer>>(mCounts.entrySet());
    Collections.sort(lEntries, 
                     Collections.reverseOrder(new Misc.MapEntryValueComparator<EquationSystem,Integer>()));
    List<EquationSystem> lSystems = new ArrayList<EquationSystem>();
    for(Map.Entry<EquationSystem,Integer> entry : lEntries){
      //should be the top 10
      Misc.Assert(entry.getValue() > 1);
      lSystems.add(entry.getKey());
      if(lSystems.size() >= 10){
        break;
      }
    }
    // //put off the two largest
    // EquationSystem systemLargest = lEntries.get(lEntries.size()-1).getKey();
    // System.out.println("Largest: "+lEntries.get(lEntries.size()-1).getValue());
    // System.out.println(systemLargest);
    // List<EquationSystem> lSystems;
    // if(!Config.config.bTestOneQuestion){
    //   EquationSystem systemSecond = lEntries.get(lEntries.size()-2).getKey();
    //   System.out.println("Second: "+lEntries.get(lEntries.size()-2).getValue());
    //   System.out.println(systemSecond);
    //   lSystems = Arrays.asList(systemLargest, systemSecond);
    // } else {
    //   lSystems = Arrays.asList(systemLargest);
    // }
    // List<EquationSystem> lSystems = new ArrayList<EquationSystem>();
    // for(Map.Entry<EquationSystem,Integer> entry : mCounts.entrySet()){
    //   if(entry.getValue() >= 2){
    //     lSystems.add(entry.getKey());
    //   }
    // }

    DerivationClassifier classifier = 
      new DerivationClassifier(Model.lTrainQuestions, Model.lTestQuestions,
                               lSystems);
    classifier.train();
  }


  static void calcSystemStats(){
    List<Question> lQuestions = 
      Question.loadFromFileNew("data/questions-all-equations.txt", false);
    List<Question> lValidQuestions = new ArrayList<Question>();
    for(Question question : lQuestions){
      if(question.system != null){
        lValidQuestions.add(question);
      }
    }
    Collections.shuffle(lValidQuestions);
    //let's randomly split it in half
    int iSplit = lValidQuestions.size()/2;
    List<Question> lTrain = lValidQuestions.subList(0, iSplit);
    List<Question> lTest = 
      lValidQuestions.subList(iSplit, lValidQuestions.size());
    //write out the numbers
    List<Integer> lTrainIndexes = new ArrayList<Integer>();
    List<Integer> lTestIndexes = new ArrayList<Integer>();
    for(Question question : lTrain){
      lTrainIndexes.add(question.iIndex);
    }
    for(Question question : lTest){
      lTestIndexes.add(question.iIndex);
    }
    try{
      FileUtils.writeLines(new File("train.txt"), lTrainIndexes);
      FileUtils.writeLines(new File("test.txt"), lTestIndexes);
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }

    Map<EquationSystem, Integer> mCounts =new HashMap<EquationSystem,Integer>();
    for(Question question : lTrain){
      Integer iCurCount = mCounts.get(question.system.system);
      if(iCurCount == null){
        mCounts.put(question.system.system,1);
      } else {
        mCounts.put(question.system.system,iCurCount+1);
      }
    }
    // how many systems have just 1 count
    int iNumOnes = 0;
    int iNumMoreOne = 0;
    int iMin = 3;
    for(Integer iCount : mCounts.values()){
      if(iCount == 1){
        iNumOnes++;
      } else if(iCount >= iMin){
        iNumMoreOne++;
      }
    }
    System.out.println("NumOnes: " + iNumOnes + " NumMoreOne: " + iNumMoreOne);
    Set<EquationSystem> setTrainSystems = new HashSet<EquationSystem>();
    for(Question question : lTrain){
      setTrainSystems.add(question.system.system);
    }
    int iContained = 0;
    int iContainedMoreOne = 0;
    for(Question question : lTest){
      if(setTrainSystems.contains(question.system.system)){
        iContained++;
        if(mCounts.get(question.system.system) >= iMin){
          iContainedMoreOne++;
        }
      }
    }
    System.out.println("NumTest: " + lTest.size() + " Contained: " + 
                       iContained + " ContainedMoreOne: " + iContainedMoreOne);
    System.out.println("NumTrainUnique: " + setTrainSystems.size());
  }

  
  static List<Question> pruneBadNumberQuestions(List<Question> lQuestions){
    List<Question> lPruned = new ArrayList<Question>();
    for(Question question : lQuestions){
      boolean bBad = false;
      for(StanfordWord wordNumber : question.doc.lNumberWords){
        if(wordNumber.isRoot() || 
           !wordNumber.depParent.wordParent.isNoun()){
          bBad = true;
          break;
        }
      }
      if(!bBad){
        lPruned.add(question);
      }
    }
    return lPruned;
  }

  static void test(){
    Model.lTrainQuestions = pruneBadNumberQuestions(Model.lTrainQuestions);
    Model.lTestQuestions = pruneBadNumberQuestions(Model.lTestQuestions);
    Model.lAllQuestions = pruneBadNumberQuestions(Model.lAllQuestions);
    checkStats();
    List<EquationSystem> lSystems = getSystems();
    DerivationClassifier classifier = 
      new DerivationClassifier(Model.lTrainQuestions, Model.lTestQuestions,
                               lSystems);
    classifier.train();
  }
 
  public static void checkStats(){
    int iNumBad = 0;
    for(Question question : Model.lAllQuestions){
      for(StanfordWord wordNumber : question.doc.lNumberWords){
        if(wordNumber.isRoot() || 
           !wordNumber.depParent.wordParent.isNoun()){
          iNumBad++;
          break;
        }
      }
    }
    System.out.println("NumBad: " + iNumBad + " out of: " 
                       + Model.lAllQuestions.size());
  }

  public static void testSystemClassifier(){
    Model.load();
    List<EquationSystem> lSystems = getSystems();
    SystemClassifier classifier = 
      new SystemClassifier(Model.lTrainQuestions, Model.lTestQuestions,
                           lSystems);
    classifier.train();
  }

  public static void testEquationClassifier(){
    Model.load();
    EquationClassifier classifier = 
      new EquationClassifier(Model.lTrainQuestions, Model.lTestQuestions);
    classifier.train();
  }

  public static void testEquClassifier(){
    Model.load();
    EquClassifier classifier = new EquClassifier(Model.lTrainQuestions, 
                                                 Model.lTestQuestions);
    classifier.train();
  }

  public static void testTermClassifier(String[] args){
    if((args.length < 1) || (args.length > 2)){
      System.out.println("You need to specify the # folds on the command-line");
    }
    int iTestFold = Integer.parseInt(args[0]);
    String sConfigFile = null;
    if(args.length == 2){
      sConfigFile = args[1];
    }
    Model.load(sConfigFile, iTestFold, false);
    TermClassifier classifier = new TermClassifier(Model.lTrainQuestions, 
                                                   Model.lTestQuestions);
    classifier.train();
  }

  // public static void buildCrossFeatures(){
  //   Model.load();
  //   Set<EquationSystem> setSystems = new HashSet<EquationSystem>();
  //   for(Question question : Model.lTrainQuestions){
  //     setSystems.add(question.system.system);
  //   }

  //   Set<Object> setAllFeatures = new HashSet<Object>();
  //   int iQuestion = 0;
  //   for(Question question : Model.lTrainQuestions){
  //     System.out.println("Building Question: " + iQuestion + " out of: " +
  //                        Model.lTrainQuestions.size());
  //     iQuestion++;
  //     for(EquationSystem system : setSystems){
  //       for(String sEquationFeature : system.getEquationletFeatures()){
  //         for(Object oNGram : question.lFeatures){
  //           setAllFeatures.add(Arrays.asList(oNGram,sEquationFeature));
  //         }
  //       }
  //     }
  //   }
  //   System.out.println("All Features: " + setAllFeatures.size());

  //   CountMap<Object> mCounts = new CountMap<Object>();
  //   for(Question question : Model.lTrainQuestions){
  //     List<String> lFeatures = question.system.system.getEquationletFeatures();
  //     //now build all cross features
  //     for(String sEquationFeature : lFeatures){
  //       for(Object oNGram : question.lFeatures){
  //         mCounts.increment(Arrays.asList(oNGram,sEquationFeature));
  //       }
  //     }
  //   }
  //   int iNumMore1 = 0;
  //   int iNumMore2 = 0;
  //   for(Integer iCount : mCounts.values()){
  //     if(iCount > 1){
  //       iNumMore1++;
  //     }
  //     if(iCount > 2){
  //       iNumMore2++;
  //     }
  //   }


  //   //now check the size
  //   System.out.println("Size: " + mCounts.size());
  //   System.out.println("NumMore1: " + iNumMore1);
  //   System.out.println("NumMore2: " + iNumMore2);
  // }
  
  static class TopNStats{
    int iN;
    Set<EquationSystem> setSystems = new HashSet<EquationSystem>();
    int iTrainCount = 0;
    int iTestCount = 0;
    TopNStats(int iN){
      this.iN = iN;
    }
  }

  public static void calcNaiveStats(){
    //sort just by count and see how many are
    Model.load();
    CountMap<EquationSystem> mCounts = new CountMap<EquationSystem>();
    for(Question question : Model.lTrainQuestions){
      mCounts.increment(question.system.system);
    }
    List<Map.Entry<EquationSystem,Integer>> lCounts = 
      new ArrayList<Map.Entry<EquationSystem,Integer>>(mCounts.entrySet());
    Comparator<Map.Entry<EquationSystem,Integer>> comparator = 
      new Misc.MapEntryValueComparator<EquationSystem,Integer>();
    //sort from largest to smallest
    Collections.sort(lCounts, Collections.reverseOrder(comparator));
    List<TopNStats> lTopNStats = new ArrayList<TopNStats>();
    for(Integer iTopN : Arrays.asList(1,2,3,4,5,10,20)){
      lTopNStats.add(new TopNStats(iTopN));
    }

    for(int iEntry = 0; iEntry < lCounts.size(); iEntry++){
      EquationSystem system = lCounts.get(iEntry).getKey();
      for(TopNStats stats : lTopNStats){
        if(iEntry < stats.iN){
          stats.setSystems.add(system);
        }
      }
    }

    for(Question question : Model.lTrainQuestions){
      for(TopNStats stats : lTopNStats){
        if(stats.setSystems.contains(question.system.system)){
          stats.iTrainCount++;
        }
      }
    }
    for(Question question : Model.lTestQuestions){
      for(TopNStats stats : lTopNStats){
        if(stats.setSystems.contains(question.system.system)){
          stats.iTestCount++;
        }
      }
    }
    for(TopNStats stats : lTopNStats){
      String sTrainFrac = Misc.fracStr(stats.iTrainCount, 
                                       Model.lTrainQuestions.size());
      String sTestFrac = Misc.fracStr(stats.iTestCount, 
                                      Model.lTestQuestions.size());
      System.out.println("Top-" + stats.iN + ": Train: " + stats.iTrainCount
                         + " " + sTrainFrac + " Test: " + stats.iTestCount
                         + " " + sTestFrac);
    }

  }

  public static void main(String[] args) throws Exception{
    //Model.load();
    //test();
    //writeQuestions();
    //checkStats();
    //calcSystemStats();
    //testNew();
    //testSystemClassifier(); 
   //timeTest();
    //buildCrossFeatures();
    //calcNaiveStats();
    //testEquationClassifier();
    //testEquClassifier();
    testTermClassifier(args);
  }
}