import java.lang.*;
import java.util.*;

class ExpectedCounts{
  List<CorrectCounts> lCorrectCounts = new ArrayList<CorrectCounts>();
  FeatureSet featureset;
  double fTotalScore = Misc.zeroMaybeLog();
  Equ equBest = null;

  ExpectedCounts(Collection<Equ> cNBest, List<ConcreteEqu> lCorrect){
    //initialize the logsumprobabilities (which are just sums for now
    if(lCorrect != null){
      lCorrectCounts = new ArrayList<CorrectCounts>();
      for(ConcreteEqu concrete : lCorrect){
        lCorrectCounts.add(new CorrectCounts(concrete));
      }
    }
    //now go though all the Equ
    FeatureListSet fls = new FeatureListSet();
    for(Equ equ : cNBest){
      if((equBest == null) || (equ.fll.fCrossProd > equBest.fll.fCrossProd)){
        equBest = equ;
      }
      Misc.Assert(equ.fll != null);
      double fScore = equ.fll.fCrossProd;
      if(!Config.config.bLogScores){
        fScore = Math.exp(equ.fll.fCrossProd);
      }
      fTotalScore = Misc.addMaybeLog(fTotalScore, fScore);
      fls.add(equ.fll);
      if(lCorrect != null){
        for(CorrectCounts cc : lCorrectCounts){
          cc.addSample(equ, fScore);
        }
      }
    }
    this.featureset = new FeatureSet(Config.config.bLogScores);
    featureset.addFeatureListSet(fls);
    //now finish it by dividing by the total score and log/exp as appropriate
    featureset.divideByMaybeLog(fTotalScore, Config.config.bLogScores);
    featureset.expMaybeLog();
    if(lCorrect != null){
      for(CorrectCounts cc : lCorrectCounts){
        cc.finish(fTotalScore);
      }
    }
  }
  
  class CorrectCounts{
    double fSumCorrectScores = Misc.zeroMaybeLog();
    double fLogProbCorrect = Double.NaN; // this get initialized later
    double fMaxCorrectScore = Double.NEGATIVE_INFINITY;
    double fMaxIncorrectScore = Double.NEGATIVE_INFINITY;
    ConcreteEqu concrete = null;
    boolean bFinished = false;
    int iNum = 0;
    int iNumCorrect = 0;
    Equ equBest = null;
    CorrectCounts(ConcreteEqu concrete){
      this.concrete = concrete;
    }
    
    boolean isCorrect(){
      return fMaxCorrectScore > fMaxIncorrectScore;
    }
    

    void addSample(Equ equ, double fScore){
      iNum++;
      if(equ.equalsConcrete(concrete)){
        fSumCorrectScores = Misc.addMaybeLog(fSumCorrectScores, fScore);
        fMaxCorrectScore = Math.max(fMaxCorrectScore, fScore);
        iNumCorrect++;
        if((equBest == null) || (equ.fll.fCrossProd > equBest.fll.fCrossProd)){
          equBest = equ;
        }
      } else {
        fMaxIncorrectScore = Math.max(fMaxIncorrectScore, fScore);
      }
    }
    
    void finish(double fTotalScore){
      Misc.Assert(!Misc.isBadDouble(this.fSumCorrectScores, 
                                    Config.config.bLogScores));
      double fProbCorrect = Misc.divMaybeLog(fSumCorrectScores, fTotalScore);
      Misc.Assert(!Misc.isBadDouble(fProbCorrect, Config.config.bLogScores));
      if(Config.config.bLogScores){
        this.fLogProbCorrect = fProbCorrect; // it's already log
      } else {
        this.fLogProbCorrect = Math.log(fProbCorrect); // it's already log
      }
      Misc.Assert(!Misc.isBadDouble(fProbCorrect, true));
      this.bFinished = true;
    }
    
    void print(String sHeader){
      System.out.println(sHeader + " FracCorr: " 
                         + Misc.fracStr(iNumCorrect, iNum)
                         + " NumCorr: " + iNumCorrect + " Total: " + iNum);
    }

  }
  
}