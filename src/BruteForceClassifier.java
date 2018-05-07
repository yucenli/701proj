import java.lang.*;
import java.util.*;

import edu.stanford.nlp.optimization.*;

abstract class BruteForceClassifier implements DiffFunction{
  Update update;
  double[] aWeightsPrev;
  int iIter = 0;
  int iMaxNumFeatures = Config.config.iMaxNumFeatures;

  List<Question> prune(List<Question> lQuestions,List<EquationSystem> lSystems){
    Set<EquationSystem> setSystems=new HashSet<EquationSystem>(lSystems);
    List<Question> lPruned = new ArrayList<Question>();
    for(Question question : lQuestions){
      if(setSystems.contains(question.system.system)){
        lPruned.add(question);
      }
    }
    return lPruned;
  }

  void train(){
    Minimizer<DiffFunction> lbfgs = new QNMinimizer();
    double[] aInitWeights = new double[Config.config.iMaxNumFeatures];
    double[] aBestWeights = lbfgs.minimize(this, 0.001, aInitWeights, 
                                           Config.config.iNumIters);
    System.out.println("********CONVERGED************");
    Model.loglinear.setWeights(aBestWeights);
    Update updateFinalTrain = calcUpdate(false);
    updateFinalTrain.print("FINAL TOTAL TRAIN STATS: ");
    Update updateFinalTest = calcUpdate(true);
    updateFinalTest.print("FINAL TOTAL TEST STATS: ");
  }
  
  public int domainDimension(){
    System.out.println("ASK: domain dimension");
    return Config.config.iMaxNumFeatures;
  }

  double calcSumOfWeightsSquared(){
    double fSum = 0.0;
    for(int iFeature=0; iFeature < Model.loglinear.iCurNumFeatures; iFeature++){
      double fWeight = Model.loglinear.aWeights[iFeature];
      fSum += fWeight*fWeight;
    }
    return fSum;
  }

  public double valueAt(double[] aWeights){
    System.out.println("ASK: domain value");
    Misc.Assert(aWeights != aWeightsPrev);
    if(Arrays.equals(aWeights, aWeightsPrev)){
      System.out.println("****EQUAL WEIGHTS-PREV: Value At*****");
    } else {
      updateWeights(aWeights);
    }
    //calc sum of weights squared
    double fSumOfWeightsSquared = calcSumOfWeightsSquared();
    double fReg = ((Config.config.fLambda*fSumOfWeightsSquared)/2.0);
    double fObjective = update.getSumLogProbCorr()-fReg;
    //return negative since we're minimizing
    return 0.0-fObjective;
  }

  public double[] derivativeAt(double[] aWeights){
    System.out.println("ASK: domain derivative");
    Misc.Assert(aWeights != aWeightsPrev);
    if(Arrays.equals(aWeights, aWeightsPrev)){
      System.out.println("****EQUAL WEIGHTS-PREV: derivativeAt*****");
    } else {
      updateWeights(aWeights);
    }
    // add in the regulariztion (- lambda * theta)
    double[] aDerivative = update.getDerivative();
    
    // note that it's ok to do this twice since the Misc methods are const
    // add in the regulariztion (- lambda * theta)
    aDerivative = Misc.add(aDerivative, 
                           Misc.mult(aWeights,-Config.config.fLambda));

    // return negative derivative because it's a minimizer
    aDerivative = Misc.mult(aDerivative, -1.0);

    return aDerivative;
  }    

  void updateWeights(double[] aWeights){
    aWeightsPrev = Arrays.copyOf(aWeights, aWeights.length);
    Model.loglinear.setWeights(aWeights);

    //first run test
    if((iIter != 0) && (iIter % Config.config.iTestPeriod == 0)){
      Update updateTest = calcUpdate(true);
      updateTest.print("TOTAL TEST STATS-" + iIter + ": ");
    }
    //then update the training update
    this.update = calcUpdate(false);
    this.update.print("TOTAL TRAIN STATS-" + iIter + ": ");                    
    iIter++;
    if(iIter >= Config.config.iNumIters){
      System.out.println("Hit max number iters, exiting....");
      System.exit(-1);
    }
  }


  abstract Update calcUpdate(boolean bTest);


}