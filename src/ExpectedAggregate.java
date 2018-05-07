import java.lang.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;

class ExpectedAggregate implements Update{
  int iNumAllQuestions;
  int iNumAllEquations;
  int iNumEquations = 0;

  int iNumQuestions = 0;
  int iNumQuestionsAllInBeam = 0;

  int iNumCorrect = 0;
  int iNumCorrectInBeam = 0;

  double fSumLogProbCorr = 0.0;

  double fTotalProbCorr = 0.0;
  double fTotalProbCorrInBeam = 0.0;

  double[] aDerivative = new double[Config.config.iMaxNumFeatures];

  ExpectedAggregate(int iNumAllQuestions, int iNumAllEquations){
    this.iNumAllQuestions = iNumAllQuestions;
    this.iNumAllEquations = iNumAllEquations;
  }

  public double getSumLogProbCorr(){
    return fSumLogProbCorr;
  }

  public double[] getDerivative(){
    return aDerivative;
  }

  double getDerivativeSum(){
    double fSum = 0.0;
    for(int iFeature = 0; iFeature < Model.features.iCurFeature; iFeature++){
      fSum += Math.abs(aDerivative[iFeature]);
    }
    return fSum;
  }

  int getTotalEquations(boolean bAllTotal){
    return bAllTotal ? iNumAllEquations : iNumEquations;
  }

  int getTotalQuestions(boolean bAllTotal){
    return bAllTotal ? iNumAllQuestions : iNumQuestions;
  }

  double getFracEquationsCorr(boolean bAllTotal){
    return Misc.div(iNumCorrect, getTotalEquations(bAllTotal));
  }

  double getFracEquationsCorrInBeam(boolean bAllTotal){
    return Misc.div(iNumCorrectInBeam, getTotalEquations(bAllTotal));
  }

  double getFracAllInBeam(boolean bAllTotal){
    return Misc.div(iNumQuestionsAllInBeam, getTotalQuestions(bAllTotal));
  }

  double getAveProbCorr(boolean bAllTotal){
    return fTotalProbCorr/((double) getTotalEquations(bAllTotal));
  }

  double getAveProbCorrInBeam(boolean bAllTotal){
    return fTotalProbCorrInBeam/((double) getTotalEquations(bAllTotal));
  }

  synchronized void addQuestionSample(boolean bAllEquationsInBeam){
    iNumQuestions++;
    if(bAllEquationsInBeam){
      iNumQuestionsAllInBeam++;
    }
  }

  void printFeatures(FeatureListList fll){
    Misc.Assert(false);//doesn't handle fudgefactors
    List<Pair<Integer,Double>> lFeatureWeights = 
      new ArrayList<Pair<Integer,Double>>();
    for(FeatureList featurelist : fll.lFeatureLists){
      for(int iIndex = 0; iIndex < featurelist.aFeatures.length; iIndex++){
        int iFeature = featurelist.aFeatures[iIndex];
        double fWeight = Model.loglinear.aWeights[iFeature];
        lFeatureWeights.add(ImmutablePair.of(iFeature, fWeight));
      }
    }
    Comparator<Pair<Integer,Double>> comparator =
      new Misc.pairRightComparator<Integer,Double>();
    Collections.sort(lFeatureWeights, Collections.reverseOrder(comparator));
    for(Pair<Integer,Double> pair : lFeatureWeights){
      Object oFeature = Model.features.getFeature(pair.getLeft());
      System.out.println(oFeature + " --> " + pair.getRight());
    }
  }


  synchronized void addSample(ExpectedCounts ecAll, ExpectedCounts ecCorr, 
                              ExpectedCounts.CorrectCounts corrcounts,
                              ExpectedCounts.CorrectCounts corrcountsInBeam,
                              ExpectedCounts ecAllInBeam){
    iNumEquations++;
    if(corrcounts.isCorrect()){
      iNumCorrect++;
    }
    if(Config.config.bPrintErrorAnalysis){
      Equ equBestInBeam = ecAllInBeam.equBest;
      boolean bCorrect = equBestInBeam.equalsConcrete(corrcounts.concrete);
      String sCorrect = bCorrect ? "CORRECT" : "WRONG";
      System.out.println("****BestInBeam: " + sCorrect + ":" + equBestInBeam 
                         + " : " + equBestInBeam.fll.fCrossProd);
      printFeatures(equBestInBeam.fll);
      Equ equCorrBest = ecCorr.equBest;
      System.out.println("****BestCorrect: " + equCorrBest + " : " + 
                         equCorrBest.fll.fCrossProd);
      printFeatures(equCorrBest.fll);
      Equ equCorrBestInBeam = corrcountsInBeam.equBest;
      if(equCorrBestInBeam != null){
        System.out.println("****BestCorrectInBeam: " + equCorrBestInBeam + " : "
                           + equCorrBestInBeam.fll.fCrossProd);
        printFeatures(equCorrBestInBeam.fll);
      } else {
        System.out.println("****BestCorrectInBeam: null");
      }
    }
    Misc.Assert(corrcounts.bFinished);
    Misc.Assert(corrcounts.fLogProbCorrect != Double.NEGATIVE_INFINITY);
    fSumLogProbCorr += corrcounts.fLogProbCorrect;
    Misc.Assert(!Misc.isBadDouble(corrcounts.fLogProbCorrect, true));
    double fProbCorr = Math.exp(corrcounts.fLogProbCorrect); 
    Misc.Assert(!Misc.isBadDouble(fProbCorr));
    fTotalProbCorr += fProbCorr;
    fTotalProbCorrInBeam += Math.exp(corrcountsInBeam.fLogProbCorrect);
    Misc.Assert(!Misc.isBadDouble(fTotalProbCorr));

    Misc.Assert(!ecAll.featureset.bLog);
    Misc.Assert(!ecCorr.featureset.bLog);
    // add the correct weights
    for(Map.Entry<Integer,Double> entry : 
          ecCorr.featureset.mFeatures.entrySet()){
      int iFeature = entry.getKey();
      double fNewVal = aDerivative[iFeature] + entry.getValue();
      Misc.Assert(!Misc.isBadDouble(fNewVal));
      aDerivative[iFeature] = fNewVal;
    }
    //add the all weights
    for(Map.Entry<Integer,Double> entry :ecAll.featureset.mFeatures.entrySet()){
      int iFeature = entry.getKey();
      double fNewVal = aDerivative[iFeature] - entry.getValue();
      Misc.Assert(!Misc.isBadDouble(fNewVal));
      aDerivative[iFeature] = fNewVal;
    }
    // System.out.println("AddSample -- fsAll: " +ecAll.featureset.mFeatures.size()
    //                    + " fsCorr: " 
    //                    + ecCorr.featureset.mFeatures.size());
    // Set<Integer> setFeatures = 
    //   new HashSet<Integer>(ecAll.featureset.mFeatures.keySet());
    // setFeatures.addAll(ecCorr.featureset.mFeatures.keySet());
    // List<Integer> lFeatures = new ArrayList<Integer>(setFeatures);
    // Collections.sort(lFeatures);
    // for(Integer iFeature : setFeatures){
    //   Double fAll = ecAll.featureset.mFeatures.get(iFeature);
    //   Double fCorr = ecCorr.featureset.mFeatures.get(iFeature);
    //   System.out.println(Model.features.getFeature(iFeature));
    //   System.out.println(iFeature + ": " + fCorr + " --> " +
    //                      fAll + " Der: " + aDerivative[iFeature]);
    // }
  }

  String toString(String sHeader, boolean bAllTotal){
    StringBuilder sb = new StringBuilder();
    sb.append(sHeader);
    if(bAllTotal){
      sb.append(" All: ");
    } else {
      sb.append(" Just: ");
    }
    sb.append(" FracEquationsCorr: ").append(getFracEquationsCorr(bAllTotal))
      .append(" FracEquationsCorrInBeam: ")
      .append(getFracEquationsCorrInBeam(bAllTotal))
      .append(" FracAllInBeam: ").append(getFracAllInBeam(bAllTotal))
      .append(" SumLogs: ").append(getSumLogProbCorr())
      .append(" AveProbCorr: ").append(getAveProbCorr(bAllTotal))
      .append(" AveProbCorrInBeam: ").append(getAveProbCorrInBeam(bAllTotal))
      .append(" NumFeatures: ").append(Model.features.iCurFeature)
      .append(" DerivativeSum: ").append(getDerivativeSum())
      .append(" NumEquations: ").append(getTotalEquations(bAllTotal))
      .append(" NumQuestions: ").append(getTotalQuestions(bAllTotal))
      .append(" MaxAvg: ").append(Misc.fracStr(getTotalQuestions(bAllTotal),
                                               getTotalEquations(bAllTotal)));
    return sb.toString();
  }

  public void print(String sHeader){
    Misc.Assert(iNumEquations > 0);
    Misc.Assert(iNumQuestions > 0);
    System.out.println(toString(sHeader, false));
    if(Config.config.bPrintAllTotalNumbers){
      System.out.println(toString(sHeader, true));
    }
  }
}