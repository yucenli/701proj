import java.lang.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;

class TermCountAggregate implements Update{
  Stopwatch stopwatch = new Stopwatch();
  double fTime;
  CountMap<String> mSystemCounts;
  int iNumAllTotal;
  
  double[] aDerivative = new double[Config.config.iMaxNumFeatures];
  double fSumLogProbCorr = 0.0;
  List<Stats> lStats;
  Stats statsAll = new Stats(" All ");
  Stats statsAllTotal = new Stats(" All Total ");
  Stats statsTop5 = new Stats(" Top5 ");
  Stats statsNotTop5 = new Stats(" NotTop5 ");
  Stats statsGoldSys = new Stats(" GoldSys ");
  Stats statsStandard = new Stats(" Standard ");
  Stats statsNonStandard = new Stats(" NonStandard ");
  Stats statsSubmissionA = new Stats(" SubmissionA ");
  Stats statsInterestStandard = new Stats(" InterestStandard ");
  Stats statsSolutionStandard = new Stats(" SolutionStandard ");
  List<Stats> lCountStats;
  Stats stats0to5 = new Stats(" System0to5 ");
  Stats stats6to10 = new Stats(" System6to10 ");
  Stats stats10to15 = new Stats(" System10to15 ");
  Stats stats15to20 = new Stats(" System15to20 ");
  Stats statsUnder20 = new Stats(" SystemUnder20 ");
  Stats statsOver20 = new Stats(" SystemOver20 ");

  TermCountAggregate(int iNumAllTotal, CountMap<String> mSystemCounts){
    this.iNumAllTotal = iNumAllTotal;
    this.mSystemCounts = mSystemCounts;
    this.lCountStats = new ArrayList<Stats>();
    for(int i = 0; i < 5; i++){
      lCountStats.add(new Stats(" System" + (i+1) + " "));
    }
    lStats = new ArrayList<Stats>();
    lStats.add(statsAllTotal);
    lStats.add(statsAll);
    lStats.add(statsTop5);
    lStats.add(statsNotTop5);
    lStats.add(statsGoldSys);
    lStats.add(statsStandard);
    lStats.add(statsNonStandard);
    lStats.add(statsSubmissionA);
    lStats.add(statsInterestStandard);
    lStats.add(statsSolutionStandard);
    lStats.addAll(lCountStats);
    lStats.add(stats0to5);
    lStats.add(stats6to10);
    lStats.add(stats10to15);
    lStats.add(stats15to20);
    lStats.add(statsUnder20);
    lStats.add(statsOver20);
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

  void printErrorAnalysis(String sHeader, FilledTerm ft, Question question,
                          int iSystemCount){
    boolean bCorrectStrict = ft.isCorrectStrict(question);
    boolean bCorrectLoose = ft.isCorrectLoose(question.ct);
    String sCorrectStrict = bCorrectStrict ? "CORRECT-STRICT" : "WRONG-STRICT";
    String sCorrectLoose = bCorrectLoose ? "CORRECT-LOOSE" : "WRONG-LOOSE";
    boolean bHasCorrectSystem = 
      ft.mt.toString(false,true).equals(question.ct.mt.toString(false,true));
    StringBuilder sb = new StringBuilder();
    sb.append("****").append(sHeader).append(": ")
      .append(" SysCount: ").append(iSystemCount)
      .append(": ").append(sCorrectLoose).append(" : ")
      .append(sCorrectStrict)
      .append(" Score: ")
      .append(ft.fll.fCrossProd).append(":\n")
      .append(" Question: ").append(question.iIndex)
      .append(" HasCorrectSystem: ").append(bHasCorrectSystem)
      .append("\n");
    if(!bHasCorrectSystem){
      sb.append("BADSYSTEM: ").append(ft.mt.toString(false,true))
        .append("CORRSYSTEM: ").append(question.ct.mt.toString(false,true))
        .append("\n");
    }

    if(question.config.sType != null){
      sb.append(" TYPE: ").append(question.config.sType).append("\n");
    }
    ft.mt.toStringBuilder(sb);
    ft.toStringBuilder(sb);
    ft.fll.toStringBuilder(sb);
    System.out.println(sb.toString());
  }

  synchronized void addSample(ExpectedTermCounts etc){
    if(!etc.bHasCorrect){
      //for now just return but we'll need to add to the other stats below
      lStats.get(0).iNumTotal++;
      return;
    }
    if(Config.config.bPrintPerQuestionStats){
      etc.print("QUESTIONSTATS-" + etc.question.iIndex);
    }
    //first calc the "update" info
    Misc.Assert(etc.fLogProbCorrectOverall != Double.NEGATIVE_INFINITY);
    fSumLogProbCorr += etc.fLogProbCorrectOverall;
    Misc.Assert(!Misc.isBadDouble(etc.fLogProbCorrectOverall, true));
    Misc.Assert(!etc.fsAll.bLog);
    Misc.Assert(!etc.fsCorr.bLog);
    // add the correct weights
    for(Map.Entry<Integer,Double> entry : etc.fsCorr.mFeatures.entrySet()){
      int iFeature = entry.getKey();
      double fNewVal = aDerivative[iFeature] + entry.getValue();
      Misc.Assert(!Misc.isBadDouble(fNewVal));
      aDerivative[iFeature] = fNewVal;
    }
    //add the all weights
    for(Map.Entry<Integer,Double> entry :etc.fsAll.mFeatures.entrySet()){
      int iFeature = entry.getKey();
      double fNewVal = aDerivative[iFeature] - entry.getValue();
      Misc.Assert(!Misc.isBadDouble(fNewVal));
      aDerivative[iFeature] = fNewVal;
    }
    //print the error analysis
    int iSystemCount = mSystemCounts.get(etc.question.ct.mt.getSignature());
    if(Config.config.bPrintErrorAnalysis && 
       (!etc.isCorrectInBeam() || Config.config.bPrintCorrectErrorAnalysis)){
      printErrorAnalysis("BestInBeam", etc.ftBestInBeam,etc.question,
                         iSystemCount);
      printErrorAnalysis("Best", etc.ftBest, etc.question, iSystemCount);
      if(etc.ftBestCorrectInBeam != null){
        printErrorAnalysis("BestCorrectInBeam", etc.ftBestCorrectInBeam,
                           etc.question, iSystemCount);
      }
      printErrorAnalysis("BestCorrect", etc.ftBestCorrect, etc.question,
                         iSystemCount);
      if(etc.ftBestIncorrect != null){
        printErrorAnalysis("BestIncorrect", etc.ftBestIncorrect,etc.question,
                           iSystemCount);
      }
    }
    //now update the stats as appropriate
    Misc.Assert(iSystemCount > 0);
    statsAll.addSample(etc);
    statsAllTotal.addSample(etc);
    if((etc.question.config.sType != null) && 
       (etc.question.config.sType.contains("standard") ||
        etc.question.config.sType.contains("submission"))){
      statsStandard.addSample(etc);
    } else {
      statsNonStandard.addSample(etc);
    }
    if(Model.setHandSignatures
       .contains(etc.question.ct.mt.toString(false,true))){
      statsTop5.addSample(etc);
    } else {
      statsNotTop5.addSample(etc);
    }

    if(etc.question.bGoldUnknownSystem){
      statsGoldSys.addSample(etc);
    }
    if((etc.question.config.sType != null) && 
       etc.question.config.sType.contains("submission-a")){
      statsSubmissionA.addSample(etc);
    }
    if((etc.question.config.sType != null) && 
       etc.question.config.sType.equals("interest-standard")){
      statsInterestStandard.addSample(etc);
    }
    if((etc.question.config.sType != null) && 
       etc.question.config.sType.equals("solution-standard")){
      statsSolutionStandard.addSample(etc);
    }
    if(iSystemCount <= 5){
      lCountStats.get(iSystemCount-1).addSample(etc);
      stats0to5.addSample(etc);
    } else if(iSystemCount <= 10){
      stats6to10.addSample(etc);
    } else if(iSystemCount <= 15){
      stats10to15.addSample(etc);
    } else if(iSystemCount <= 20){
      stats15to20.addSample(etc);
    } else {
      statsOver20.addSample(etc);
    }
    if(iSystemCount <= 20){
      statsUnder20.addSample(etc);
    }
  }

  public void print(String sHeader){
    if(Config.config.bPrintFullStats){
      for(Stats stats : lStats){
        System.out.println(stats.toString(sHeader));
      }
    } else {
      System.out.println(lStats.get(0).toString(sHeader));
    }
  }


  class Stats{
    Set<String> setCorrectSystems = new HashSet<String>();
    Set<String> setCorrectSystemsNumerical = new HashSet<String>();
    List<Integer> lCorrectSystems = new ArrayList<Integer>();
    String sName;
    int iNumTotal;
    
    
    int iNumCorrect = 0;
    int iNumSortaCorrect = 0;
    int iNumCorrectInBeam = 0;
    int iNumCorrectSystemInBeam = 0;
    int iNumSortaCorrectInBeam = 0;

    int iNumMarginalCorrect = 0;
    int iNumMarginalSortaCorrect = 0;
    int iNumMarginalCorrectInBeam = 0;
    int iNumMarginalCorrectInBeamNumerical = 0;
    int iNumMarginalCorrectInBeamNumericalPipeline = 0;
    int iNumMarginalSortaCorrectInBeam = 0;

    int iNumHasCorrectSystemInBeam = 0;
    int iNumHasCorrectSystem = 0;
    int iNumHasCorrectInBeam = 0;

    double fTotalProbCorr = 0.0;
    double fTotalProbCorrInBeam = 0.0;
    Stats(String sName){
      this.sName = sName;
    }
    int getTotal(){
      if(this.sName.equals(" All Total ")){
        return iNumAllTotal;
      } else {
        return iNumTotal;
      }
    }

    double getFrac(double iNumerator){
      return iNumerator/((double)getTotal());
    }

    void addSample(ExpectedTermCounts etc){
      iNumTotal++;
      if(etc.isCorrectOverall()){
        iNumCorrect++;
      }
      if(etc.isSortaCorrectOverall()){
        iNumSortaCorrect++;
      }
      if(etc.isCorrectInBeam()){
        iNumCorrectInBeam++;
      }
      if(etc.isCorrectSystemInBeam()){
        iNumCorrectSystemInBeam++;
      }
      if(etc.isSortaCorrectInBeam()){
        iNumSortaCorrectInBeam++;
      }
      if(etc.bMarginalCorrectOverall){
        iNumMarginalCorrect++;
      }
      if(etc.bMarginalSortaCorrectOverall){
        iNumMarginalSortaCorrect++;
      }
      if(etc.bMarginalCorrectInBeam){
        iNumMarginalCorrectInBeam++;
        setCorrectSystems.add(etc.question.ct.mt.toString(false,true));
      }
      if(etc.bMarginalCorrectInBeamNumerical){
        iNumMarginalCorrectInBeamNumerical++;
        setCorrectSystemsNumerical.add(etc.question.ct.mt.toString(false,true));
        if(Model.lCorrectSystems.contains(etc.question.iIndex)){
          iNumMarginalCorrectInBeamNumericalPipeline++;
        }
      }
      if(etc.bMarginalSortaCorrectInBeam){
        iNumMarginalSortaCorrectInBeam++;
      }

      if(etc.bBeamHasCorrectSystem){
        iNumHasCorrectSystemInBeam++;
      }
      if(etc.bHasCorrectSystem){
        iNumHasCorrectSystem++;
        lCorrectSystems.add(etc.question.iIndex);
      }
      if(etc.iNumCorrectInBeam > 0){
        iNumHasCorrectInBeam++;
      }

      double fProbCorr = Math.exp(etc.fLogProbCorrectOverall); 
      Misc.Assert(!Misc.isBadDouble(fProbCorr));
      fTotalProbCorr += fProbCorr;
      Misc.Assert(!Misc.isBadDouble(fTotalProbCorr));
      Misc.Assert(!Misc.isBadDouble(etc.fLogProbCorrectInBeam, true));
      double fProbCorrInBeam = Math.exp(etc.fLogProbCorrectInBeam); 
      Misc.Assert(!Misc.isBadDouble(fProbCorrInBeam));
      fTotalProbCorrInBeam += fProbCorrInBeam;
      Misc.Assert(!Misc.isBadDouble(fTotalProbCorrInBeam));
    }
    String toString(String sHeader){
      StringBuilder sb = new StringBuilder();
      sb.append(sHeader).append(" ").append(sName).append(" ");
      if(!Config.config.bPrintFullStats){
        sb.append(" MargCorrNumerical: ")
          .append(getFrac(iNumMarginalCorrectInBeamNumerical))
          .append(" MargCorrEquations: ")
          .append(getFrac(iNumMarginalCorrectInBeam));
      } else {
        sb.append(" MargCorrInBeam: ").append(getFrac(iNumMarginalCorrectInBeam))
          .append(" MargCorrInBeamNum: ")
          .append(getFrac(iNumMarginalCorrectInBeamNumerical))
          .append(" MargCorrInBeamNumPipe: ")
          .append(getFrac(iNumMarginalCorrectInBeamNumericalPipeline))
          .append(" FracCorrInBeam: ").append(getFrac(iNumCorrectInBeam))
          .append(" SumLogs: ").append(getSumLogProbCorr())
          .append(" FracCorrSystemInBeam: ")
          .append(getFrac(iNumCorrectSystemInBeam))
          .append(" FracHasCorrSystemInBeam: ")
          .append(getFrac(iNumHasCorrectSystemInBeam))
          .append(" FracHasCorrSystem: ")
          .append(getFrac(iNumHasCorrectSystem))
          .append(" FracHasCorrInBeam: ")
          .append(getFrac(iNumHasCorrectInBeam))
          .append(" NumCorrSystems: ")
          .append(setCorrectSystems.size())
          .append(" NumCorrSystemsNumerical: ")
          .append(setCorrectSystemsNumerical.size())
          .append(" FracSortaCorrInBeam: ")
          .append(getFrac(iNumSortaCorrectInBeam))
          .append(" FracCorr: ").append(getFrac(iNumCorrect))
          .append(" FracSortaCorr: ").append(getFrac(iNumSortaCorrect))
          .append(" MargSortaCorrInBeam: ")
          .append(getFrac(iNumMarginalSortaCorrectInBeam))
          .append(" MargCorr: ").append(getFrac(iNumMarginalCorrect))
          .append(" MargSortaCorr: ").append(getFrac(iNumMarginalSortaCorrect))
          .append(" AveProbCorr: ").append(getFrac(fTotalProbCorr))
          .append(" AveProbCorrInBeam: ")
          .append(getFrac(fTotalProbCorrInBeam))
          .append(" NumFeatures: ").append(Model.features.iCurFeature)
          .append(" DerivativeSum: ").append(getDerivativeSum())
          .append(" Num: ").append(iNumTotal)
          .append(" Max: ").append(Misc.div(this.iNumTotal, this.getTotal()))
          .append(" Time: ").append(stopwatch.secs());
      }
      return sb.toString();
    }
  }

}
