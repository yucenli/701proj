import java.lang.*;
import java.util.*;
import java.util.concurrent.*;


class DerivationClassifier extends BruteForceClassifier{
  List<Question> lTrainQuestions;
  List<Question> lTestQuestions;
  List<Question> lTrainPruned;
  List<Question> lTestPruned;
  List<EquationSystem> lSystems;

  DerivationClassifier(List<Question> lTrainQuestions, 
                       List<Question> lTestQuestions,
                       List<EquationSystem> lSystems){
    this.lTrainQuestions = lTrainQuestions;
    this.lTestQuestions = lTestQuestions;
    this.lSystems = lSystems;
    this.lTrainPruned = prune(lTrainQuestions, lSystems);
    this.lTestPruned = prune(lTestQuestions, lSystems);
    System.out.println("TrainPruned: " + lTrainPruned.size());
    System.out.println("TestPruned: " + lTestPruned.size());
  }


  static class UnknownChooser extends Choosable<StanfordWord>{
    Question question;
    EquationSystem system;
    FeatureCounts featurecounts;
    UnknownChooser(Question question, EquationSystem system, 
                   FeatureCounts featurecounts){
      super(question.doc.lNouns, system.numUnknowns());
      this.question = question;
      this.system = system;
      this.featurecounts = featurecounts;
    }

    void choose(List<StanfordWord> lUnknowns){
      NumberChooser chooser = new NumberChooser(question, system,
                                                lUnknowns, featurecounts);
      chooser.chooseAll();
    }
  }

  static class UnknownChooserParallel extends Choosable<StanfordWord>{
    Question question;
    EquationSystem system;
    FeatureCounts featurecounts;
    List<Callable<FeatureCounts>> lCallables;
    UnknownChooserParallel(Question question, EquationSystem system,
                           List<Callable<FeatureCounts>> lCallables){
      super(question.doc.lNouns, system.numUnknowns());
      this.question = question;
      this.system = system;
      this.lCallables = lCallables;
    }

    void choose(List<StanfordWord> lUnknowns){
      NumberChooser chooser = new NumberChooser(question, system,
                                                lUnknowns, 
                                                new FeatureCounts(question));
      lCallables.add(new NumberChooserCallable(chooser));
    }
  }


  static class NumberChooserCallable implements Callable<FeatureCounts>{
    NumberChooser chooser;
    NumberChooserCallable(NumberChooser chooser){
      this.chooser = chooser;
    }
    public FeatureCounts call(){
      chooser.chooseAll();
      return chooser.featurecounts;
    }
  }



  static class NumberChooser extends Choosable<StanfordWord>{
    Question question;
    EquationSystem system;
    List<StanfordWord> lUnknowns;
    FeatureCounts featurecounts;
    NumberChooser(Question question, EquationSystem system, 
                  List<StanfordWord> lUnknowns, FeatureCounts featurecounts){
      super(question.doc.lNumberWords, system.numNumbers());
      this.question = question;
      this.system = system;
      this.lUnknowns = lUnknowns;
      this.featurecounts = featurecounts;
    }

    void choose(List<StanfordWord> lNumbers){
      Derivation der =new Derivation(system, this.lUnknowns, lNumbers);
      boolean bCorrect = der.equalsConcrete(question.system);
      FeatureSet featureset = 
        Model.features.derivationfeatures.getFeatures(der, bCorrect);
      double fScore = Model.loglinear.scoreMaybeLog(featureset);
      featurecounts.addSample(featureset, fScore, bCorrect, null);
    }
  }

  FeatureCounts calcFeatureCounts(Question question){
    FeatureCounts featurecounts = new FeatureCounts(question);
    for(EquationSystem system : lSystems){
      UnknownChooser chooser = 
        new UnknownChooser(question, system, featurecounts);
      chooser.chooseAll();
    }
    featurecounts.finish();
    return featurecounts;
  }

  void calcFeatureCountsParallel(Question question,
                                 List<Callable<FeatureCounts>> lCallables){
    for(EquationSystem system : lSystems){
      UnknownChooserParallel chooser = 
        new UnknownChooserParallel(question, system, lCallables);
      chooser.chooseAll();
    }
  }

  Update calcUpdate(boolean bTest){
    Misc.Assert(false); // need to rescore the featurelist cache
    List<Question> lQuestions = bTest ? lTestPruned : lTrainPruned;
    int iOrigSize = bTest ? lTestQuestions.size() : lTrainQuestions.size();
    FeatureCountAggregate fca = new FeatureCountAggregate(iOrigSize, 
                                                          iMaxNumFeatures,
                                                          false);
    if(Config.config.bParallelize){
      List<Callable<FeatureCounts>> lCallables = 
        new ArrayList<Callable<FeatureCounts>>();
      for(Question question : lQuestions){
        calcFeatureCountsParallel(question, lCallables);
      }
      try{
        ExecutorService executor = 
          Executors.newFixedThreadPool(Config.config.iNumThreads);
        List<Future<FeatureCounts>> lFutures = executor.invokeAll(lCallables);
        System.out.println("Done with parallel");
        executor.shutdownNow();
        Map<Integer,FeatureCounts> mFeatureCounts = 
          new HashMap<Integer,FeatureCounts>();
        for(Future<FeatureCounts> future : lFutures){
          Misc.Assert(future.isDone());
          FeatureCounts fcNew = future.get();
          FeatureCounts fcCur = mFeatureCounts.get(fcNew.question.iIndex);
          if(fcCur == null){
            mFeatureCounts.put(fcNew.question.iIndex, fcNew);
          } else {
            fcCur.integrate(fcNew);
          }
        }
        System.out.println("Terminating...");
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        Misc.Assert(executor.isTerminated());
        System.out.println("Terminated.");
        // now add all the featurecounts the fca
        for(FeatureCounts featurecounts : mFeatureCounts.values()){
          featurecounts.finish();
          fca.addSample(featurecounts);
        }
      } catch(ExecutionException|InterruptedException ex){
        throw new RuntimeException(ex);
      }
    } else {
      int iQuestion = 0;
      for(Question question : lQuestions){
        FeatureCounts featurecounts = calcFeatureCounts(question);
        fca.addSample(featurecounts);
        System.out.println("Finished Question: " + iQuestion + " out of: "
                           + lQuestions.size());
        iQuestion++;
      }
    }
    return fca;
  }
}