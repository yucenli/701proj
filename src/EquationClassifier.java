import java.lang.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.math3.util.CombinatoricsUtils;

class EquationClassifier extends BruteForceClassifier{
  List<Question> lTrainQuestions;
  List<Question> lTestQuestions;
  List<Question> lTrainPruned;
  List<Question> lTestPruned;
  List<QDerivation> lTrainDerivations;
  List<QDerivation> lTestDerivations;

  //Map<Pair<StanfordWord,StanfordWord>,List<Object>> mWordPairFeatures = 
  //  new HashMap<Pair<StanfordWord,StanfordWord>,List<Object>>();

  EquationClassifier(List<Question> lTrainQuestions, 
                     List<Question> lTestQuestions){
    this.lTrainQuestions = lTrainQuestions;
    this.lTestQuestions = lTestQuestions;
    buildQDerivations(lTrainQuestions, lTestQuestions);
  }

  List<Question> prune(List<Question> lAllQuestions,Set<Equation> setEquations){
    // prune to the set where all equations are in the 
    List<Question> lPrunedQuestions = new ArrayList<Question>();
    for(Question question : lAllQuestions){
      if(setEquations.containsAll(question.system.lSingleEquations)){
        lPrunedQuestions.add(question);
      }
    }
    return lPrunedQuestions;
  }

  List<QDerivation> buildQDerivations(List<Question> lQuestions,
                                      Set<Equation> setEquations){
    List<QDerivation> lQDerivations = new ArrayList<QDerivation>();
    for(Question question : lQuestions){
      lQDerivations.add(new QDerivation(question, setEquations));
    }
    return lQDerivations;
  }

  void buildQDerivations(List<Question> lTrainQuestions, 
                         List<Question> lTestQuestions){
    System.out.println("All Train Size: " + lTrainQuestions.size());
    System.out.println("All Test Size: " + lTestQuestions.size());
    if(Config.config.bPruneToTenQuestionsForTesting){
      lTrainQuestions=
        lTrainQuestions.subList(0, Math.min(10, lTrainQuestions.size()));
      System.out.println("******DEBUG PRUNING TRAIN SIZE TO 10******");
    }
    //extract the equations from the training set
    CountMap<Equation> mEquationCounts = new CountMap<Equation>();
    for(Question question : lTrainQuestions){
      for(Equation equation : question.system.lSingleEquations){
        mEquationCounts.increment(equation);
      }
    }
    Set<Equation> setEquations = new HashSet<Equation>();
    for(Map.Entry<Equation,Integer> entry : mEquationCounts.entrySet()){
      if((entry.getValue() >= Config.config.iMinEquationCount) ||
         Config.config.bTestOneQuestion || 
         Config.config.bPruneToTenQuestionsForTesting){
        setEquations.add(entry.getKey());
      }
    }
    System.out.println("NumEquations: " + setEquations.size());
    lTrainPruned = prune(lTrainQuestions, setEquations);
    lTestPruned = prune(lTestQuestions, setEquations);
    System.out.println("ValidQuestions: Train: " + lTrainPruned.size()
                       + " Test: " + lTestPruned.size());

    lTrainDerivations = buildQDerivations(lTrainPruned, setEquations);
    lTestDerivations = buildQDerivations(lTestPruned, setEquations);
  }

  void reset(List<QDerivation> lQDerivations){
    Model.features.derivationfeatures.resetCache();
    for(QDerivation qder : lQDerivations){
      qder.reset();
    }
  }

  void computeFeatureCounts(List<QDerivation> lQDerivations, 
                            FeatureCountAggregate fca){
    reset(lQDerivations);
    List<Callable<Object>> lCallables = new ArrayList<Callable<Object>>();
    for(QDerivation qder : lQDerivations){
      qder.addCallables(lCallables);
    }
    Collections.shuffle(lCallables);
    System.out.println("NumCallables: " + lCallables.size());
    System.out.println("Going Parallel....");
    Stopwatch stopwatch = new Stopwatch();
    List<Object> lFeatureCounts = Parallel.process(lCallables);
    System.out.println("Parallel Time: " + stopwatch.secs());
    //now gather up the featurecounts by question
    for(QDerivation qder : lQDerivations){
      fca.addSample(qder.calcFeatureCounts());
    }
  }

  Update calcUpdate(boolean bTest){
    List<QDerivation> lQDerivations =bTest ? lTestDerivations:lTrainDerivations;
    int iOrigSize = bTest ? lTestQuestions.size() : lTrainQuestions.size();
    FeatureCountAggregate fca = 
      new FeatureCountAggregate(iOrigSize, iMaxNumFeatures, true);
    computeFeatureCounts(lQDerivations, fca);
    return fca;
  }
}