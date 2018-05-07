import java.lang.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;

class FeatureCounts{
  //List<StanfordWord> lUnknowns;
  //EquationSystem system;
  Question question;
  FeatureSet fsCorrect;
  FeatureSet fsAll;
  FeatureListSet flsCorrect;
  FeatureListSet flsAll;
  double fTotalScore = Misc.zeroMaybeLog();
  boolean bFinished = false;
  double fTotalScoreCorr = Misc.zeroMaybeLog();
  boolean bFoundCorrect = false;
  boolean bUseFeatureLists = false;
  double fMaxScoreIncorrect = Double.NEGATIVE_INFINITY;
  double fMaxScoreCorrect = Double.NEGATIVE_INFINITY;
  EquationDerivation derMaxScoreCorrect = null;
  EquationDerivation derMaxScoreIncorrect = null;

  MarginalMap<String> mMarginals;

  List<Pair<Double,String>> lScores = new ArrayList<Pair<Double,String>>();
  int iCorrectRank = 0;
  int iCorrectMarginalRank = 0;
 
  Set<String> setCorrect;
  Set<String> setCorrectMarginals;

  static FeatureCounts makeEquationDerivationFeatureCounts(Question question){
    return new FeatureCounts(question, true, question.system.setConcreteStrings,
                             question.system.setEquationStrings);
  }


  FeatureCounts(Question question){
    this(question, false,new HashSet<String>(), new HashSet<String>());
  }

  FeatureCounts(Question question, boolean bUseFeatureLists,
                Set<String> setCorrect, 
                Set<String> setCorrectMarginals){
    this.question = question;
    this.bUseFeatureLists = bUseFeatureLists;
    this.setCorrect = setCorrect;
    this.setCorrectMarginals = setCorrectMarginals;
    mMarginals = new MarginalMap<String>(Config.config.bLogScores);
    if(bUseFeatureLists){
      flsCorrect = new FeatureListSet();
      flsAll = new FeatureListSet();
    } else {
      fsCorrect = new FeatureSet(Config.config.bLogScores);
      fsAll = new FeatureSet(Config.config.bLogScores);
    }
  }
  
  void integrate(FeatureCounts fcOther){
    Misc.Assert(!this.bFinished);
    Misc.Assert(!fcOther.bFinished);
    //integrate the scores and the marginals
    lScores.addAll(fcOther.lScores);
    for(Map.Entry<String,Double> entry : fcOther.mMarginals.entrySet()){
      mMarginals.add(entry.getKey(), entry.getValue());
    }
    if(fcOther.bUseFeatureLists){
      Misc.Assert(this.bUseFeatureLists);
      integrateFeatureLists(fcOther);
      return;
    }
    Misc.Assert(!this.bUseFeatureLists);

    Misc.Assert(this.question.iIndex == fcOther.question.iIndex);
    this.fTotalScore = Misc.addMaybeLog(fTotalScore, fcOther.fTotalScore);
    this.fsAll.addFeatureSet(fcOther.fsAll);
    this.bFoundCorrect |= fcOther.bFoundCorrect;
    this.fTotalScoreCorr = Misc.addMaybeLog(fTotalScoreCorr,
                                            fcOther.fTotalScoreCorr);
    this.fsCorrect.addFeatureSet(fcOther.fsCorrect);
    this.fMaxScoreCorrect = Math.max(fMaxScoreCorrect,fcOther.fMaxScoreCorrect);
    this.fMaxScoreIncorrect = Math.max(fMaxScoreIncorrect,
                                       fcOther.fMaxScoreIncorrect);
  }

  void integrateFeatureLists(FeatureCounts fcOther){
    Misc.Assert(this.question.iIndex == fcOther.question.iIndex);
    this.fTotalScore = Misc.addMaybeLog(fTotalScore, fcOther.fTotalScore);
    this.flsAll.addFeatureListSet(fcOther.flsAll);
    this.bFoundCorrect |= fcOther.bFoundCorrect;
    this.fTotalScoreCorr = Misc.addMaybeLog(fTotalScoreCorr,
                                            fcOther.fTotalScoreCorr);
    this.flsCorrect.addFeatureListSet(fcOther.flsCorrect);
    if(fcOther.fMaxScoreCorrect > this.fMaxScoreCorrect){
      this.fMaxScoreCorrect = fcOther.fMaxScoreCorrect;
      this.derMaxScoreCorrect = fcOther.derMaxScoreCorrect;
    }
    if(fcOther.fMaxScoreIncorrect > this.fMaxScoreIncorrect){
      this.fMaxScoreIncorrect = fcOther.fMaxScoreIncorrect;
      this.derMaxScoreIncorrect = fcOther.derMaxScoreIncorrect;
    }
  }


  boolean isCorrect(){
    return fMaxScoreCorrect > fMaxScoreIncorrect;
  }

  boolean isSortaCorrect(){
    return fMaxScoreCorrect >= fMaxScoreIncorrect;
  }

  double getProbCorr(){
    double fProbCorr = Misc.divMaybeLog(fTotalScoreCorr, fTotalScore);
    return Misc.maybeExp(fProbCorr);
  }

  void addSample(FeatureSet featureset, double fScore, boolean bCorrect, 
                 Object oMarginal){
    Misc.Assert(oMarginal == null); // integration of these is unimplemented
    Misc.Assert(fsAll != null);
    this.fTotalScore = Misc.addMaybeLog(fTotalScore, fScore);
    Misc.Assert(fScore != Misc.zeroMaybeLog());
    if(fTotalScore == Misc.zeroMaybeLog()){
      System.out.println("BadTotal: " + fTotalScore + " " + fScore);
      Misc.Assert(fTotalScore != Misc.zeroMaybeLog());
    }
    //take the log of the featureset if we need to an it's not already done
    if(!featureset.bLog){
      featureset.maybeLog();
    }
    //end debug
    fsAll.addFeatureSetWithMultiplierMaybeLog(featureset, fScore, 
                                              Config.config.bLogScores);
    if(bCorrect){
      bFoundCorrect = true;
      fTotalScoreCorr = Misc.addMaybeLog(fTotalScoreCorr, fScore);
      fsCorrect.addFeatureSetWithMultiplierMaybeLog(featureset, fScore,
                                                    Config.config.bLogScores);
      fMaxScoreCorrect = Math.max(fMaxScoreCorrect, fScore);
    } else {
      fMaxScoreIncorrect = Math.max(fMaxScoreIncorrect, fScore);
    }

    if(oMarginal != null){
      mMarginals.add(oMarginal.toString(), fScore);
    }
  }


  void addSample(FeatureListList featurelistlist, boolean bCorrect,
                 EquationDerivation der, Object oMarginal){
    Misc.Assert(bUseFeatureLists);
    double fScore = featurelistlist.scoreMaybeLog();
    Misc.Assert(fScore != Misc.zeroMaybeLog());
    this.fTotalScore = Misc.addMaybeLog(fTotalScore,fScore);
    if(fTotalScore == Misc.zeroMaybeLog()){
      System.out.println("BadTotal: " + fTotalScore + " " + fScore);
      Misc.Assert(fTotalScore != Misc.zeroMaybeLog());
    }

    flsAll.add(featurelistlist);
    if(bCorrect){
      bFoundCorrect = true;
      fTotalScoreCorr = Misc.addMaybeLog(fTotalScoreCorr, fScore);
      flsCorrect.add(featurelistlist);
      if(fScore > fMaxScoreCorrect){
        fMaxScoreCorrect = fScore;
        derMaxScoreCorrect = der;
      }
    } else {
      if(fScore > fMaxScoreIncorrect){
        fMaxScoreIncorrect = fScore;
        derMaxScoreIncorrect = der;
      }
    }
    
    mMarginals.add(oMarginal.toString(), fScore);
    if(setCorrect.contains(der.sConcrete)){
      lScores.add(ImmutablePair.of(fScore,der.sConcrete));
    } else {
      lScores.add(ImmutablePair.of(fScore,(String)null));
    }
  }

  void printErrorAnalysis(){
    if(isCorrect()){
      System.out.println("****Question-" + question.iIndex + ": CORRECT");
    } else {
      System.out.println("****Question-" + question.iIndex + ": WRONG");
      System.out.println("Best Correct: " + fMaxScoreCorrect);
      System.out.println(derMaxScoreCorrect.toString());
      System.out.println(derMaxScoreCorrect.fll.toString());
      System.out.println("Best Wrong: " + fMaxScoreIncorrect);
      System.out.println(derMaxScoreIncorrect.toString());
      System.out.println(derMaxScoreIncorrect.fll.toString());
    }
  }

  void finish(){
    if(!bFoundCorrect){
      System.out.println("Numbers: " + question.doc.lDoubles);

      System.out.println("Couldn't find a correct solution for question: " +
                         question.iIndex + " --> " + question.toString());
      System.out.println(question.toFullString());
      System.out.println("NumTotal: " + lScores.size());
      System.out.println("NumNouns: " + question.doc.lNouns.size());
      Misc.Assert(bFoundCorrect);
    }
    Misc.Assert(!bFinished);
    if(bUseFeatureLists){
      // we'll transform it to a non-featurelist version
      fsCorrect = new FeatureSet(Config.config.bLogScores);
      fsCorrect.addFeatureListSet(flsCorrect);
      fsAll = new FeatureSet(Config.config.bLogScores);
      fsAll.addFeatureListSet(flsAll);
    }

    fsCorrect.divideByMaybeLog(fTotalScoreCorr, Config.config.bLogScores);
    fsAll.divideByMaybeLog(fTotalScore, Config.config.bLogScores);
    fsCorrect.expMaybeLog();
    fsAll.expMaybeLog();
    //calc the ranking numbers
    Comparator<Pair<Double,String>> comparator = 
      new Misc.pairLeftComparator<Double,String>();
    Collections.sort(lScores, Collections.reverseOrder(comparator));
    List<Map.Entry<String,Double>> lMarginals = 
      new ArrayList<Map.Entry<String,Double>>(mMarginals.entrySet());
    Comparator<Map.Entry<String,Double>> comparatorMarginals = 
      new Misc.MapEntryValueComparator<String,Double>();
    Collections.sort(lMarginals, Collections.reverseOrder(comparatorMarginals));
    //now find the higest rank
    Set<String> setCorrectLeft = new HashSet<String>(setCorrect);
    for(int iScore = 0; iScore < lScores.size(); iScore++){
      Pair<Double,String> pair = lScores.get(iScore);
      setCorrectLeft.remove(pair.getRight());
      if(setCorrectLeft.size() == 0){
        iCorrectRank = iScore;
        break;
      }
    }
    if(setCorrectLeft.size() != 0){
      System.out.println("****ERROR: Couldn't find a concrete equation for question: " +
                         question.iIndex + " --> " + question.toString());
      System.out.println("AllCorrect: " + setCorrect);
      System.out.println("CorrectLeft: " + setCorrectLeft);
      //Misc.Assert(setCorrectLeft.size() == 0);
      //TODO -- FIX THIS PROBLEM
      iCorrectRank = Integer.MAX_VALUE;
    }

    Set<String> setCorrectMarginalsLeft =
      new HashSet<String>(setCorrectMarginals);
    for(int iMarginal = 0; iMarginal < lMarginals.size(); iMarginal++){
      Map.Entry<String,Double> entry = lMarginals.get(iMarginal);
      setCorrectMarginalsLeft.remove(entry.getKey());
      if(setCorrectMarginalsLeft.size() == 0){
        iCorrectMarginalRank = iMarginal;
        break;
      }
    }
    if(setCorrectMarginalsLeft.size() != 0){
      System.out.println("****Couldn't find a marginal equation for question: " +
                         question.iIndex + " --> " + question.toString());
      System.out.println("AllCorrect: " + setCorrectMarginals);
      System.out.println("CorrectLeft: " + setCorrectMarginalsLeft);
      //Misc.Assert(setCorrectMarginalsLeft.size() == 0);
      //TODO -- FIX THIS PROBLEM
      iCorrectMarginalRank = Integer.MAX_VALUE;
    }

    this.bFinished = true;
    if(Config.config.bPrintErrorAnalysis){
      printErrorAnalysis();
      System.out.println("CorrectRank: " + iCorrectRank + " out of: " +
                         lScores.size());
      System.out.println("CorrectMarginalRank: " + iCorrectMarginalRank 
                         + " out of: " + lMarginals.size());
    }
    Misc.Assert(lScores.size() != 0);
  }
}