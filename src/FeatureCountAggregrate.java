import java.lang.*;
import java.util.*;

class FeatureCountAggregate implements Update{
  int iNumTotal;
  int iNumAllTotal;

  int iNumCorr;
  int iNumSortaCorr;

  boolean bPrintTopN;
  List<Integer> lRankCounts;
  List<Integer> lRankCutoffs = Arrays.asList(10,100,1000,10000,100000,1000000);
  List<Integer> lMarginalRankCounts;
  List<Integer> lMarginalRankCutoffs = Arrays.asList(1,2,3,4,5,10,20);

  double fProbCorrTotal;

  double fSumLogProbCorr = 0.0;

  double[] aDerivative;

  FeatureCountAggregate(int iNumAllTotal, int iMaxNumFeatures, 
                        boolean bPrintTopN){
    this.iNumAllTotal = iNumAllTotal;
    this.aDerivative = new double[iMaxNumFeatures];
    this.bPrintTopN = bPrintTopN;
    this.lRankCounts = Misc.genFullList(lRankCutoffs.size(), 0);
    this.lMarginalRankCounts = Misc.genFullList(lMarginalRankCutoffs.size(), 0);
  }

  public double[] getDerivative(){
    return aDerivative;
  }
  
  public double getSumLogProbCorr(){
    return fSumLogProbCorr;
  }


  double getFracCorr(boolean bIncludeAll){
    if(bIncludeAll){
      return ((double)iNumCorr)/((double)iNumAllTotal);
    } else {
      return ((double)iNumCorr)/((double)iNumTotal);
    }
  }
  
  double getMarginalFracInTopN(int iN, boolean bIncludeAll){
    if(bIncludeAll){
      return ((double)lMarginalRankCounts.get(iN))/((double)iNumAllTotal);
    } else {
      return ((double)lMarginalRankCounts.get(iN))/((double)iNumTotal);
    }
  }

  double getFracInTopN(int iN, boolean bIncludeAll){
    if(bIncludeAll){
      return ((double)lRankCounts.get(iN))/((double)iNumAllTotal);
    } else {
      return ((double)lRankCounts.get(iN))/((double)iNumTotal);
    }
  }

  double getFracSortaCorr(boolean bIncludeAll){
    if(bIncludeAll){
      return ((double)iNumSortaCorr)/((double)iNumAllTotal);
    } else {
      return ((double)iNumSortaCorr)/((double)iNumTotal);
    }
  }

  double getAverageProbCorr(boolean bIncludeAll){
    if(bIncludeAll){
      return fProbCorrTotal/((double)iNumAllTotal);
    } else {
      return fProbCorrTotal/((double)iNumTotal);
    }
  }

  double[] derivative(){
    return aDerivative;
  }

  void addSample(FeatureCounts featurecounts){
    Misc.Assert(featurecounts.bFinished);
    iNumTotal++;
    if(featurecounts.isCorrect()){
      iNumCorr++;
    }
    if(bPrintTopN){
      for(int iIndex = 0; iIndex < lRankCutoffs.size(); iIndex++){
        if(featurecounts.iCorrectRank <= lRankCutoffs.get(iIndex)){
          lRankCounts.set(iIndex, lRankCounts.get(iIndex)+1);
        }
      }
      for(int iIndex = 0; iIndex < lMarginalRankCutoffs.size(); iIndex++){
        if(featurecounts.iCorrectMarginalRank<=lMarginalRankCutoffs.get(iIndex)){
          lMarginalRankCounts.set(iIndex, lMarginalRankCounts.get(iIndex)+1);
        }
      }
    }
      
    if(featurecounts.isSortaCorrect()){
      iNumSortaCorr++;
    }

    double fProbCorr = featurecounts.getProbCorr(); 
    fProbCorrTotal += fProbCorr;
    fSumLogProbCorr += Math.log(fProbCorr);

    Misc.Assert(!featurecounts.fsCorrect.bLog);
    Misc.Assert(!featurecounts.fsAll.bLog);
    // add the correct weights
    for(Map.Entry<Integer,Double> entry : 
          featurecounts.fsCorrect.mFeatures.entrySet()){
      if(aDerivative[entry.getKey()]+entry.getValue() == 
         Double.NEGATIVE_INFINITY){
        System.out.println("BadUpdate: " + aDerivative[entry.getKey()]
                           + " PLUS " + entry.getValue());
        Misc.Assert(false);
      }
      aDerivative[entry.getKey()]+= entry.getValue();
    }
    //subtract the overall weights
    for(Map.Entry<Integer,Double> entry : 
          featurecounts.fsAll.mFeatures.entrySet()){
      if(aDerivative[entry.getKey()]-entry.getValue() == 
         Double.NEGATIVE_INFINITY){
        System.out.println("BadUpdate: " + aDerivative[entry.getKey()] 
                           + " MINUS " + entry.getValue());
        Misc.Assert(false);
      }
      aDerivative[entry.getKey()] -= entry.getValue();
    }
  }

  String getResultString(String sHeader, boolean bAllTotal){
    StringBuilder sb = new StringBuilder();
    sb.append(sHeader);
    if(bAllTotal){
      sb.append(" AllTotal ");
    } else {
      sb.append(" JustTotal ");
    }
    sb.append("FracCorr: ")
      .append(getFracCorr(bAllTotal));
    if(bPrintTopN){
      for(int iIndex = 0; iIndex < lRankCutoffs.size(); iIndex++){
        sb.append(" FracTop").append(lRankCutoffs.get(iIndex))
          .append(": ").append(getFracInTopN(iIndex, bAllTotal));
      }
      for(int iIndex = 0; iIndex < lMarginalRankCutoffs.size(); iIndex++){
        sb.append(" FracMarginalTop").append(lMarginalRankCutoffs.get(iIndex))
          .append(": ").append(getMarginalFracInTopN(iIndex, bAllTotal));
      }
    }
    sb.append(" FracSortaCorr: ").append(getFracSortaCorr(bAllTotal))
      .append(" AvgProbCorr: ").append(getAverageProbCorr(bAllTotal));
    if(bAllTotal){
      sb.append(" NumTotal: ").append(iNumAllTotal);
    } else {
      sb.append(" NumTotal: ").append(iNumTotal);
    }
    if(!bAllTotal){
      sb.append(" Features: ").append(Model.features.iCurFeature);
    }
    return sb.toString();
  }


  public void print(String sHeader){
    System.out.println(getResultString(sHeader, false));
    if(iNumTotal != iNumAllTotal){
      System.out.println(getResultString(sHeader, true));
    }
  }

}