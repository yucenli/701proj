import java.lang.*;
import java.util.*;

class ExpectedTermCounts{
  Question question;
  boolean bTest;
  //for the updates
  FeatureSet fsAll;
  FeatureSet fsCorr;
  double fLogProbCorrectOverall;

  //for stat reporting
  FilledTerm ftBest = null;
  FilledTerm ftBestCorrect = null;
  FilledTerm ftBestInBeam = null;
  FilledTerm ftBestCorrectInBeam = null;
  FilledTerm ftBestIncorrect = null;
  
  int iNumInBeam = -1;
  int iNumCorrectTotal = -1;
  int iNumCorrectInBeam = 0;


  double fMaxCorrectScoreOverall = Double.NEGATIVE_INFINITY;
  double fMaxCorrectScoreInBeam = Double.NEGATIVE_INFINITY;
  double fMaxIncorrectScore = Double.NEGATIVE_INFINITY;

  double fLogProbCorrectInBeam;
  SumMap<String> mMarginals = new SumMap<String>(Config.config.bLogScores);
  SumMap<List<Double>> mNumericalMarginals = 
    new SumMap<List<Double>>(Config.config.bLogScores);
  Map<String,String> mConcreteToSystem = new HashMap<String,String>();
  boolean bMarginalCorrectInBeam = false;
  boolean bMarginalCorrectOverall = false;
  boolean bMarginalSortaCorrectInBeam = false;
  boolean bMarginalSortaCorrectOverall = false;

  boolean bHasCorrect = false;

  boolean bMarginalCorrectInBeamNumerical = false;
  boolean bMarginalCorrectSystemInBeam = false;

  boolean bBeamHasCorrectSystem = false;
  Set<String> setSystemsInBeam = new HashSet<String>();

  boolean bHasCorrectSystem = false;


  ExpectedTermCounts(List<FilledTerm> lNBest, List<FilledTerm> lNBestCorr,
                     Question question, boolean bTest, 
                     List<SimpleSolver> lSolvers){
    if(lNBest == null){
      bHasCorrect = false;
      return;
    }
    this.iNumInBeam = lNBest.size();
    this.iNumCorrectTotal = 0;//lNBestCorr.size();
    this.question = question;
    this.bTest = bTest;
    Set<FilledTerm> setFtCorr = null;
    if(lNBestCorr != null){
      setFtCorr = new HashSet<FilledTerm>(lNBestCorr);
      //only include the correct ones not in the beam
      setFtCorr.removeAll(lNBest);
      
    }
    bHasCorrectSystem = lNBest.get(0).mt.equals(question.ct.mt);

    //now go though all the Equ
    FeatureListSet flsAll = new FeatureListSet();
    FeatureListSet flsCorr = new FeatureListSet();
    double fTotalScore = Misc.zeroMaybeLog();
    double fTotalCorrectScore = Misc.zeroMaybeLog();

    int iMaxIters = (setFtCorr == null) ? 1 : 2;
    for(int iIter = 0; iIter < iMaxIters; iIter++){
      boolean bInBeam = (iIter == 0);
      Collection<FilledTerm> cFtCur = bInBeam ? lNBest : setFtCorr;
      for(FilledTerm ft : cFtCur){
        if(Config.config.bPipeline && (iIter == 0)){
          if(!ft.mt.equals(lNBest.get(0).mt)){
            System.out.println("Top: " + lNBest.get(0).mt.toString(false,true));
            System.out.println("Cur: " + ft.mt.toString(false,true));
          }
          Misc.Assert(ft.mt.equals(lNBest.get(0).mt));
        }
        ftBest = getBest(ftBest, ft);
        if(bInBeam){
          ftBestInBeam = getBest(ftBestInBeam, ft);
          if(ft.mt.toString(true,false).equals(question.ct.mt.toString(true,
                                                                       false))){
            bBeamHasCorrectSystem = true;
          }
          setSystemsInBeam.add(ft.mt.toString(true,false));
        }
        double fScore = ft.fll.fCrossProd;
        if(!Config.config.bLogScores){
          fScore = Math.exp(fScore);
        }
        double fTotalBefore = fTotalScore;
        fTotalScore = Misc.addMaybeLog(fTotalScore, fScore);
        if(Misc.isBadDouble(fTotalScore)){
          System.out.println("Bad Total: " + fTotalScore + " " + fTotalBefore 
                             + " " + fScore);
          Misc.Assert(!Misc.isBadDouble(fTotalScore));
        }
        flsAll.add(ft.fll);
        if(!Config.config.bPredictOnlyTemplates){
          mMarginals.increment(ft.getConcreteSignature(), fScore);
          mConcreteToSystem.put(ft.getConcreteSignature(), 
                                ft.mt.toString(false,true));
        }
        if(bTest){
          //run the solver answer
          List<Double> lCalced = ft.getRoundedSortedNumericalSolution(lSolvers);
          if(lCalced != null){
            mNumericalMarginals.increment(lCalced, fScore);
          }
        }
        
        //if(ft.getConcreteSignature().equals(question.ct.getSignature())){
        if(ft.isCorrect(question, bTest, lSolvers)){
          iNumCorrectTotal++;
          bHasCorrect = true;
          flsCorr.add(ft.fll);
          //it's correct
          fTotalCorrectScore = Misc.addMaybeLog(fTotalCorrectScore, fScore);
          ftBestCorrect = getBest(ftBestCorrect, ft);
          fMaxCorrectScoreOverall = Math.max(fMaxCorrectScoreOverall, fScore);
          if(bInBeam){
            ftBestCorrectInBeam = getBest(ftBestCorrectInBeam, ft);
            iNumCorrectInBeam++;
            fMaxCorrectScoreInBeam = Math.max(fMaxCorrectScoreInBeam, fScore);
          }
        } else {
          Misc.Assert(bInBeam);
          fMaxIncorrectScore = Math.max(fMaxIncorrectScore, fScore);
          ftBestIncorrect = getBest(ftBestIncorrect, ft);
        }
      }
      if(bInBeam){
        //finish all the in beam stats
        double fProbCorrectInBeam = 
          Misc.divMaybeLog(fTotalCorrectScore, fTotalScore);
        Misc.Assert(!Misc.isBadDouble(fProbCorrectInBeam,
                                      Config.config.bLogScores));
        if(Config.config.bLogScores){
          this.fLogProbCorrectInBeam = fProbCorrectInBeam; // it's already log
        } else {
          this.fLogProbCorrectInBeam = Math.log(fProbCorrectInBeam); 
        }
        Misc.Assert(!Misc.isBadDouble(fProbCorrectInBeam, true));
        if(!Config.config.bPredictOnlyTemplates){
          List<String> lBest = mMarginals.getBest();
          if(lBest.contains(question.ct.getSignature())){
            if(lBest.size() == 1){
              bMarginalCorrectInBeam = true;
            }
            bMarginalSortaCorrectInBeam = true;
          }
          if(lBest.size() == 1){
            String sSystemSig = mConcreteToSystem.get(lBest.get(0));
            if(sSystemSig.equals(question.ct.mt.toString(false,true))){
              bMarginalCorrectSystemInBeam = true;
            }
          }
          if(mNumericalMarginals.size() != 0){
            List<List<Double>> llBestCalced = mNumericalMarginals.getBest();
            if(llBestCalced.size() == 1){
              List<Double> lBestCalced = llBestCalced.get(0);
              if(question.isCorrectLoose(lBestCalced)){
                bMarginalCorrectInBeamNumerical = true;
              }
            }
          }   
        }
      }
    }
    if(!bHasCorrect){
      //just bail out
      return;
    }

    //these are all for the updates
    this.fsAll = new FeatureSet(Config.config.bLogScores);
    this.fsCorr = new FeatureSet(Config.config.bLogScores);
    fsAll.addFeatureListSet(flsAll);
    fsCorr.addFeatureListSet(flsCorr);
    //now finish it by dividing by the total score and log/exp as appropriate
    fsAll.divideByMaybeLog(fTotalScore, Config.config.bLogScores);
    fsCorr.divideByMaybeLog(fTotalCorrectScore, Config.config.bLogScores);
    fsAll.expMaybeLog();
    fsCorr.expMaybeLog();
    Misc.Assert(!Misc.isBadDouble(fTotalCorrectScore, Config.config.bLogScores));
    double fProbCorrectOverall = 
      Misc.divMaybeLog(fTotalCorrectScore, fTotalScore);
    Misc.Assert(!Misc.isBadDouble(fProbCorrectOverall,Config.config.bLogScores));
    if(Config.config.bLogScores){
      this.fLogProbCorrectOverall = fProbCorrectOverall; // it's already log
    } else {
      this.fLogProbCorrectOverall = Math.log(fProbCorrectOverall); 
    }
    Misc.Assert(!Misc.isBadDouble(fProbCorrectOverall, true));
    if(!Config.config.bPredictOnlyTemplates){
      List<String> lBest = mMarginals.getBest();
      if(lBest.contains(question.ct.getSignature())){
        if(lBest.size() == 1){
          bMarginalCorrectOverall = true;
        }
        bMarginalSortaCorrectOverall = true;
      }
    }
  }
  
  FilledTerm getBest(FilledTerm ftCurBest, FilledTerm ftNew){
    if(ftCurBest == null) {
      return ftNew;
    }
    if (ftNew.fll != null && ftNew.fll.fCrossProd > ftCurBest.fll.fCrossProd){
      return ftNew;
    }

    return ftCurBest;
  }

  
  boolean isCorrectOverall(){
    boolean bCorrect = (fMaxCorrectScoreOverall > fMaxIncorrectScore);
    //if(bCorrect){
    //  Misc.Assert(ftBest.isCorrect(question, this.bTest));
    //}
    return bCorrect;
  }
  boolean isSortaCorrectOverall(){
    return fMaxCorrectScoreOverall >= fMaxIncorrectScore;
  }
  boolean isCorrectInBeam(){
    boolean bCorrect = (fMaxCorrectScoreInBeam > fMaxIncorrectScore);
    //if(bCorrect){
    //  Misc.Assert(ftBestInBeam.isCorrect(question, this.bTest));
    //}
    return bCorrect;
  }
  boolean isCorrectSystemInBeam(){
    //String sBestSystemSig = ftBestInBeam.mt.toString(false,true);
    //String sCorrSystemSig = question.ct.mt.toString(false,true);
    //return sBestSystemSig.equals(sCorrSystemSig);
    return bMarginalCorrectSystemInBeam;
  }

  boolean isSortaCorrectInBeam(){
    return fMaxCorrectScoreInBeam >= fMaxIncorrectScore;
  }


  void print(String sHeader){
    System.out.println(sHeader 
                       + " MarginalCorrInBeam: " + bMarginalCorrectInBeam
                       + " MarginalCorrInBeamNum: " 
                       + bMarginalCorrectInBeamNumerical 
                       + " BeamHasCorrectSystem: " + bBeamHasCorrectSystem
                       + " NumSystemsInBeam: " + setSystemsInBeam.size()
                       + " FracCorr: " 
                       + Misc.fracStr(iNumCorrectInBeam, iNumInBeam)
                       + " NumCorrInBeam: " + iNumCorrectInBeam 
                       + " TotalInBeam: " + iNumInBeam + " CorrTotal: "
                       + iNumCorrectTotal);
  }
}