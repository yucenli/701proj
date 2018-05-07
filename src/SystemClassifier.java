import java.lang.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.commons.lang3.tuple.*;

class SystemClassifier extends BruteForceClassifier{
  List<Question> lTrainQuestions;
  List<Question> lTestQuestions;
  List<Question> lTrainPruned;
  List<Question> lTestPruned;
  List<EquationSystem> lSystems;
  Map<Pair<Question,EquationSystem>,FeatureSet> mFeatureSets =
    new HashMap<Pair<Question,EquationSystem>, FeatureSet>();
  Map<Pair<Question,EquationSystem>,FeatureSet> mLogFeatureSets =
    new HashMap<Pair<Question,EquationSystem>, FeatureSet>();


  SystemClassifier(List<Question> lTrainQuestions, 
                   List<Question> lTestQuestions,
                   List<EquationSystem> lSystems){
    this.lTrainQuestions = lTrainQuestions;
    this.lTestQuestions = lTestQuestions;
    this.lSystems = lSystems;
    this.lTrainPruned = prune(lTrainQuestions, lSystems);
    this.lTestPruned = prune(lTestQuestions, lSystems);
    System.out.println("TrainPruned: " + lTrainPruned.size());
    System.out.println("TestPruned: " + lTestPruned.size());
    indexSystems(lSystems);
    Model.features.systemfeatures.calcTrainFeatures(lTrainQuestions);
    calcFeatureSets();
  }

  void indexSystems(List<EquationSystem> lSystems){
    for(int iSystem = 0; iSystem < lSystems.size(); iSystem++){
      lSystems.get(iSystem).iIndex = iSystem;
    }
  }

  void calcFeatureSets(){
    System.out.println("Calcing featuresets....");
    List<Callable<FeatureSetCallable>> lCallables=
      new ArrayList<Callable<FeatureSetCallable>>();
    List<Question> lAllQuestions = new ArrayList<Question>(lTrainQuestions);
    lAllQuestions.addAll(lTestQuestions);
    for(Question question : lAllQuestions){
      for(EquationSystem system : lSystems){
        lCallables.add(new FeatureSetCallable(question, system));
      }
    }
    try{
      ExecutorService executor = 
        Executors.newFixedThreadPool(Config.config.iNumThreads);
      List<Future<FeatureSetCallable>> lFutures =executor.invokeAll(lCallables);
      System.out.println("Done with parallel");
      executor.shutdownNow();
      for(Future<FeatureSetCallable> future : lFutures){
        Misc.Assert(future.isDone());
        FeatureSetCallable callable = future.get();
        Misc.Assert(!callable.featureset.bLog);
        callable.featureset.lock();
        mFeatureSets.put(ImmutablePair.of(callable.question, callable.system),
                         callable.featureset);
        FeatureSet fsLog = new FeatureSet(callable.featureset);
        fsLog.maybeLog();
        fsLog.lock();
        mLogFeatureSets.put(ImmutablePair.of(callable.question,callable.system),
                            fsLog);
        
      }
      System.out.println("Terminating...");
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
      Misc.Assert(executor.isTerminated());
      System.out.println("Terminated.");
    } catch(ExecutionException|InterruptedException ex){
      throw new RuntimeException(ex);
    }
  }


  class FeatureSetCallable implements Callable<FeatureSetCallable>{
    Question question;
    EquationSystem system;
    FeatureSet featureset;
    FeatureSetCallable(Question question, EquationSystem system){
      this.question = question;
      this.system = system;
    }

    public FeatureSetCallable call(){
      this.featureset = 
        Model.features.systemfeatures.getFeatureSet(question,system);
      Misc.Assert(!this.featureset.bLog);
      return this;
    }
  }


  Update calcUpdate(boolean bTest){
    List<Question> lQuestions = bTest ? lTestPruned : lTrainPruned;
    int iOrigSize = bTest ? lTestQuestions.size() : lTrainQuestions.size();
    FeatureCountAggregate fca = new FeatureCountAggregate(iOrigSize, 
                                                          iMaxNumFeatures,
                                                          true);
    for(Question question : lQuestions){
      FeatureCounts featurecounts = new FeatureCounts(question);
      for(EquationSystem system : lSystems){
        boolean bCorrect = question.system.system.equals(system);
        if(bCorrect){
          String sSystemFeature = system.toFeatureString();
          String sQuestionFeature = question.system.system.toFeatureString();
          Misc.Assert(sSystemFeature.equals(sQuestionFeature));
        }
        FeatureSet featureset = 
          mFeatureSets.get(ImmutablePair.of(question,system));
        Misc.Assert(!featureset.bLog);
        double fScore = Model.loglinear.scoreMaybeLog(featureset);
        FeatureSet featuresetLog = 
          mLogFeatureSets.get(ImmutablePair.of(question,system));
        Misc.Assert(featuresetLog.bLog);
        if(Config.config.bLogScores){
          featurecounts.addSample(featuresetLog, fScore, bCorrect, system);
        } else {
          featurecounts.addSample(featureset, fScore, bCorrect, system);
        }
      }
      featurecounts.finish();
      fca.addSample(featurecounts);
    }
    return fca;
  }
}
