import java.lang.*;
import java.util.*;
import java.io.*;

import java.util.concurrent.*;
import org.apache.commons.io.FileUtils;

import org.apache.commons.math3.util.CombinatoricsUtils;

class TermClassifier extends BruteForceClassifier{
  List<Question> lTrainQuestions;
  List<Question> lTestQuestions;
  List<Question> lTrainPruned;
  List<Question> lTestPruned;
  List<MappedTerm> lSystems;
  CountMap<String> mSystemCounts;
  //Map<Pair<StanfordWord,StanfordWord>,List<Object>> mWordPairFeatures = 
  //  new HashMap<Pair<StanfordWord,StanfordWord>,List<Object>>();
  TermClassifier(List<Question> lTrainQuestions, 
                 List<Question> lTestQuestions){
    this.lTrainQuestions = lTrainQuestions;
    this.lTestQuestions = lTestQuestions;

    Set<MappedTerm> setSystems = new HashSet<MappedTerm>();
    mSystemCounts = new CountMap<String>();
    //we'll do this over train and test now
    for(Question question : lTrainQuestions){
      mSystemCounts.increment(question.ct.mt.getSignature());
    }
    for(Question question : lTestQuestions){
      mSystemCounts.increment(question.ct.mt.getSignature());
    }
    this.lTrainPruned = new ArrayList<Question>();

    for(Question question : lTrainQuestions){
      if(!isGoodType(question, false)){
        continue;
      }
      if((mSystemCounts.get(question.ct.mt.getSignature()) >=
          Config.config.iMinSystemCount) || Config.config.bTestOneQuestion){
        if(!Config.config.bUseOnlyHandAnnotationQuestionsAnnotatedForTrain ||
           Model.lHandAnnotations.contains(question.iIndex)){
          setSystems.add(question.ct.mt);
        }
        lTrainPruned.add(question);
      }
    }
    //compute the termlists for all the systems
    this.lSystems = new ArrayList<MappedTerm>(setSystems);
    this.lTestPruned = new ArrayList<Question>();
    for(Question question : lTestQuestions){
      if(!isGoodType(question, true)){
        continue;
      }
      if(setSystems.contains(question.ct.mt)){
        lTestPruned.add(question);
      }
    }
    System.out.println("NumTrainFull: " + lTrainQuestions.size());
    System.out.println("NumTestFull: " + lTestQuestions.size());
    System.out.println("NumTrain: " + lTrainPruned.size());
    System.out.println("NumTest: " + lTestPruned.size());
    calcCommonFeatures();
  }

  boolean isGoodType(Question question, boolean bTest){
    if(Config.config.bUseOnlySubmissionA){
      Misc.Assert(!Config.config.bUseOnlyStandard);
      if((question.config.sType == null) ||
         !question.config.sType.equals("submission-a")){
        return false;
      } else {
        return true;
      }
    } else if(Config.config.bUseOnlyStandard){
      if(question.config.sType == null){
        return false;
      }
      return question.config.sType.contains("standard") ||
        question.config.sType.contains("submission");
    } else if(Config.config.bUseOnlyInterestStandard){
      if((question.config.sType == null) ||
         !question.config.sType.equals("interest-standard")){
        return false;
      }
      if(Config.config.bUseOnlyGoldUnknowns){
        return question.bGoldUnknowns;
      } else {
        return true;
      }
    } else if(Config.config.bUseOnlySolutionStandard){
      if((question.config.sType == null) ||
         !question.config.sType.equals("solution-standard")){
        return false;
      }
      if(Config.config.bUseOnlyGoldUnknowns){
        return question.bGoldUnknowns;
      } else {
        return true;
      }
    } else if(Config.config.bUseOnlyGoldUnknowns){
      return question.bGoldUnknowns;
    } else if(Config.config.bUseOnlyGoldUnknownSystems){
      if(!question.bGoldUnknownSystem){
        return false;
      }
      if(Config.config.bUseOnlyHandAnnotationQuestionsForTrain && !bTest){
        return Model.lHandAnnotations.contains(question.iIndex);
      } else if(Config.config.bUseOnlyGoldUnknownsForTrain){
        return (bTest || question.bGoldUnknowns);
      } else {
        return true;
      }
    } else if(Config.config.bUseOnlyHandAnnotationQuestionsForTrain && !bTest){
      boolean bContains = Model.lHandAnnotations.contains(question.iIndex);
      // if(bContains){
      //   System.out.println("Hand Annotations contains: " + question.iIndex);
      // } else {
      //   System.out.println("Hand Annotations nocontain: " + question.iIndex);
      // }
      return bContains;
    } else {
      //no limitations, so include everything
      return true;
    }
  }
  
  void calcCommonFeatures(){
    Model.features.derivationfeatures.computeCommonFeatures(this.lTrainPruned);
  }

  static boolean allFilled(List<FilledTerm> lFilledTerms){
    for(FilledTerm ft : lFilledTerms){
      if(!ft.isFilled()){
        return false;
      }
    }
    return true;
  }

  static class UnknownChoosable extends Choosable<StanfordWord>{
    List<FilledTerm> lFts;
    FilledTerm ftTemplate;
    Question question;
    UnknownChoosable(Question question, List<FilledTerm> lFts, 
                     FilledTerm ftTemplate){
      super(question.doc.lNouns, 
            question.ct.mt.numUnknownSlots(),
            Config.config.bUnknownWordPerInstance);
      this.question = question;
      this.lFts = lFts;
      this.ftTemplate = ftTemplate;
    }
    void choose(List<StanfordWord> lUnknowns){
      for(int iUnknown = 0; iUnknown < lUnknowns.size(); iUnknown++){
        StanfordWord word = lUnknowns.get(iUnknown);
        if(!isGoodUnknown(iUnknown, word, question)){
          return;
        }
      }  
      //   if(Config.config.bRequireUnknownStringOverlap  ||
      //      Config.config.bRequireUnknownStringOverlapForOnlyGold){
      //     for(int iUnknown = 0; iUnknown < lUnknowns.size(); iUnknown++){
      //       StanfordWord word = lUnknowns.get(iUnknown);
      //       if(question.bGoldUnknowns){
      //         //Misc.Assert(Config.config.bUnknownWordPerInstance);
      //         if(!question.llAllowedUnknowns.get(iUnknown).contains(word)){
      //           return;
      //         }
      //       } else if(Config.config.bRequireUnknownStringOverlap){
      //         //require it for everything, not only gold
      //         int iUnique = iUnknown;
      //         if(Config.config.bUnknownWordPerInstance){
      //           iUnique = question.ct.mt.mapUnknowns.aMap[iUnknown];
      //         }
      //         List<StanfordWord> lAllowed=question.llAllowedUnknowns.get(iUnique);
      //         if(!lAllowed.contains(lUnknowns.get(iUnknown))){
      //           //not a valid choice
      //           return;
      //         }
      //       }
      //     }
      //   }
      lFts.add(new FilledTerm(ftTemplate, lUnknowns));
    }
  }

  static boolean isGoodUnknown(int iUnknown, StanfordWord word, 
                               Question question){
    if(Config.config.bRequireUnknownStringOverlap  ||
       Config.config.bRequireUnknownStringOverlapForOnlyGold){
      if(question.bGoldUnknowns){
        //Misc.Assert(Config.config.bUnknownWordPerInstance);
        if(!question.llAllowedUnknowns.get(iUnknown).contains(word)){
          return false;
        }
      } else if(Config.config.bRequireUnknownStringOverlap){
        //require it for everything, not only gold
        int iUnique = iUnknown;
        if(Config.config.bUnknownWordPerInstance){
          iUnique = question.ct.mt.mapUnknowns.aMap[iUnknown];
        }
        List<StanfordWord> lAllowed=question.llAllowedUnknowns.get(iUnique);
        if(!lAllowed.contains(word)){
          //not a valid choice
          return false;
        }
      }
    }
    return true;
  }
  
  static class NumberChoosable extends Choosable<StanfordWord>{
    Question question;
    List<FilledTerm> lFts;
    NumberChoosable(Question question){
      super(question.doc.lNumberWords, question.ct.lNumberDoubles.size());
      this.question = question;
      this.lFts = new ArrayList<FilledTerm>();
    }
    void choose(List<StanfordWord> lNumberWords){
      for(int iNumber = 0; iNumber < lNumberWords.size(); iNumber++){
        if(lNumberWords.get(iNumber).number.fNumber != 
           question.ct.lNumberDoubles.get(iNumber)){
          //not a good choice
          return;
        }
      }
      //all the numbers match up
      FilledTerm ft = new FilledTerm(question.ct.mt);
      ft.lNumberWords = lNumberWords;
      UnknownChoosable choosable = 
        new UnknownChoosable(question, lFts, ft);
      choosable.chooseAll();
    }
  }

  static List<FilledTerm> computeAllCorrect(Question question, boolean bScore,
                                            FeatureCache cache,
                                            List<SimpleSolver> lSolvers){
    NumberChoosable choosable = new NumberChoosable(question);
    choosable.chooseAll();
    if(choosable.lFts.size() == 0){
      System.out.println("Bad Allowed Unknowns for Question- " + 
                         question.iIndex + ": " + question.llAllowedUnknowns);
    }
    Misc.Assert(choosable.lFts.size() > 0);
    if(bScore){
      for(FilledTerm ft : choosable.lFts){
        ft.score(question, cache, lSolvers);
      }
    }
    return choosable.lFts;
  }

  


  List<FilledTerm> computeNBestOverall(Question question, FeatureCache cache,
                                       List<SimpleSolver> lSolvers,
                                       boolean bOnlyCorrect){
    List<StanfordWord> lNouns = question.doc.lNouns;

    List<StanfordWord> lNumberWords = question.doc.lNumberWords;
    List<FilledTerm> lBeam = new ArrayList<FilledTerm>();
    if(bOnlyCorrect || Config.config.bAssumeCorrectSystem){
      lBeam.add(new FilledTerm(question.ct.mt));
    } else {
      //start with an empty filled term for every mapped term
      for(MappedTerm mt : this.lSystems){
        //make sure we have enough numbers to fill it
        if((lNumberWords.size() >= mt.mapNumbers.iNumUnique) &&
           (lNouns.size() >= mt.mapUnknowns.iNumUnique)){
          //if(lNumberWords.size() >= mt.mapNumbers.iNumUnique){
          lBeam.add(new FilledTerm(mt));
        }
      }
      //if we're doing pipeline then keep only the bestone
      if(Config.config.bPipeline){
        FilledTerm ftBest = lBeam.get(0);
        for(FilledTerm ftCur : lBeam){
          ftCur.score(question, cache, lSolvers);
          if(ftCur.fll.fCrossProd > ftBest.fll.fCrossProd){
            ftBest = ftCur;
          }
        }
        lBeam = Arrays.asList(ftBest);
      }

    }
    if(lBeam.size() == 0){
      return null;
    }
    if(Config.config.bPredictOnlyTemplates){
      //first score everything
      for(FilledTerm ft : lBeam){
        ft.score(question, cache, lSolvers);
      }
      Collections.sort(lBeam, Collections.reverseOrder());
      return lBeam;
    }
    int iIter = 0;
    int iPrevBeamSize = lBeam.size();
    while(!allFilled(lBeam)){
      // if(!((lBeam.size() == Config.config.iMaxBeamSize) || 
      //      (lBeam.size() >= iPrevBeamSize))){
      //   System.out.println("BADNESS: " + iIter + " " + lBeam.size() + " " +
      //                      iPrevBeamSize);
      //   //Misc.Assert((lBeam.size() == Config.config.iMaxBeamSize) || 
      //   //            (lBeam.size() >= iPrevBeamSize));
      // }
      iPrevBeamSize = lBeam.size();
      Set<FilledTerm> setNewFTs = new HashSet<FilledTerm>();
      for(FilledTerm ft : lBeam){
        if(ft.isFilled()){
          setNewFTs.add(ft);
          continue;
        }
        if(Config.config.bFillSlotsInOrder){
          int iSlot = ft.iNextSlot;
          boolean bUnknown = (iSlot < ft.lUnknownWords.size());
          List<StanfordWord> lWords = bUnknown ? lNouns : lNumberWords;
          List<StanfordWord> lFilledWords = 
            bUnknown ? ft.lUnknownWords : ft.lNumberWords;
          for(StanfordWord word : lWords){
            if((Config.config.bUnknownWordPerInstance && bUnknown) ||
               !lFilledWords.contains(word)){
              boolean bInclude = false;
              if(!bOnlyCorrect){
                bInclude = true;
              } else {
                if(bUnknown){
                  bInclude = isGoodUnknown(iSlot, word, question);
                } else {
                  //must be a number
                  int iNumber = iSlot - ft.lUnknownWords.size();
                  double fCorrect = question.ct.lNumberDoubles.get(iNumber);
                  if(Double.compare(word.number.fNumber,fCorrect) == 0){
                    bInclude = true;
                  }
                }
              }
              if(bInclude){
                FilledTerm ftNew = new FilledTerm(ft, iSlot, word);
                setNewFTs.add(ftNew);
                if(ftNew.isFilled() && bOnlyCorrect){
                  if(!ftNew.isCorrect(question, false, lSolvers)){
                    System.out.println("FT: " + ftNew);
                    System.out.println("Corr: " + question.ct);
                    Misc.Assert(ft.isCorrect(question, false, lSolvers));
                  }
                }
              }
            }
          }
        } else {
          boolean bAddedOne = false;
          for(int iSlot = 0; iSlot < ft.mt.numFilledTermSlots(); iSlot++){
            //check if it's already filled
            if(ft.getUniqueSlotNoun(iSlot) != null){
              continue;
            }
            boolean bUnknown = (iSlot < ft.lUnknownWords.size());
            List<StanfordWord> lWords = bUnknown ? lNouns : lNumberWords;
            List<StanfordWord> lFilledWords = 
              bUnknown ? ft.lUnknownWords : ft.lNumberWords;
            for(StanfordWord word : lWords){
              if((Config.config.bUnknownWordPerInstance && bUnknown) ||
                 !lFilledWords.contains(word)){
                setNewFTs.add(new FilledTerm(ft, iSlot, word));
                bAddedOne = true;
              }
            }
          }
          Misc.Assert(bAddedOne);
        }
      }
      lBeam = new ArrayList<FilledTerm>(setNewFTs);
      if((iIter > 0) || Config.config.bPruneOnFirstIter){
        //first score everything
        for(FilledTerm ft : lBeam){
          ft.score(question, cache, lSolvers);
        }
        Collections.sort(lBeam, Collections.reverseOrder());
        //now prune the beam
        if(lBeam.size() > Config.config.iMaxBeamSize){
          //create a new arraylist to make sure the rest get garbage collected
          if(bOnlyCorrect){
            int iMaxBeamSize = Config.config.iMaxBeamSize;
            lBeam = new ArrayList<FilledTerm>(lBeam.subList(0, iMaxBeamSize));
          } else {
            List<FilledTerm> lNewBeam = new ArrayList<FilledTerm>();
            CountMap<String> mSystemCounts = new CountMap<String>();
            for(FilledTerm ftCur : lBeam){
              String sSig = ftCur.mt.toString(false,true);
              if(mSystemCounts.get(sSig) <Config.config.iMaxNumPerSystemInBeam){
                mSystemCounts.increment(sSig);
                lNewBeam.add(ftCur);
                if(lNewBeam.size() == Config.config.iMaxBeamSize){
                  break;
                }
              }
            }
            lBeam = lNewBeam;
          }
        }
      }
      iIter++;
    }      
    Misc.Assert(lBeam.size() != 0);
    return lBeam;
  }

  double computeProb(double fScore, double fTotalScore){
    double fLogProb = Misc.divMaybeLog(fScore, fTotalScore);
    return Misc.maybeExp(fLogProb);
  }

  ExpectedTermCounts computeExpectedTermCounts(Question question,boolean bTest){
    //we'll reset the cache on a per question basis each iteration
    FeatureCache cache = new FeatureCache();
    //first compute the nbest overall
    List<SimpleSolver> lSolvers = Question.buildSolvers();
    List<FilledTerm> lNBest = computeNBestOverall(question, cache, lSolvers,
                                                  false);
    List<FilledTerm> lNBestCorr = null;
    boolean bCalcCorr = false;
    if(Config.config.bSemiSupervised){
      if(Config.config.bUseOnlyHandAnnotationQuestionsAnnotatedForTrain){
        bCalcCorr = Model.lHandAnnotations.contains(question.iIndex);
      } else {
        bCalcCorr = question.bGoldUnknowns;
      }
    } else {
      bCalcCorr = true;
    }
    if(bCalcCorr){
      if(Config.config.bCalcCorrectUsingBeam){
        lNBestCorr = computeNBestOverall(question, cache, lSolvers, true);
      } else {
        lNBestCorr = computeAllCorrect(question, true, cache,
                                       lSolvers);
      }
    } 
    ExpectedTermCounts etc = new ExpectedTermCounts(lNBest, lNBestCorr, 
                                                    question, bTest,
                                                    lSolvers);
    return etc;
  }


  class ComputeEtcCallable implements Callable<ExpectedTermCounts>{
    Question question;
    boolean bTest;
    ComputeEtcCallable(Question question, boolean bTest){
      this.question = question;
      this.bTest = bTest;
    }
    public ExpectedTermCounts call(){
      try{
        return computeExpectedTermCounts(question, bTest);
      }catch(Exception ex){
        System.out.println("Exception in parallel execution for question:" + 
                           question.iIndex);
        ex.printStackTrace();
        System.exit(-1);
        throw ex;
      }
    }
  }

  void computeTermCountAggregate(List<Question> lQuestions, boolean bTest,
                                 TermCountAggregate tca){
    Model.features.derivationfeatures.resetCache();
    List<ExpectedTermCounts> lEtcs;
    if(Config.config.bParallelize){
      List<Callable<ExpectedTermCounts>> lCallables = 
        new ArrayList<Callable<ExpectedTermCounts>>();
      for(Question question : lQuestions){
        lCallables.add(new ComputeEtcCallable(question, bTest));
      }
      lEtcs = Parallel.process(lCallables);
      
    } else {
      lEtcs = new ArrayList<ExpectedTermCounts>();
      for(Question question : lQuestions){
        lEtcs.add(computeExpectedTermCounts(question, bTest));
      }
    }
    for(ExpectedTermCounts etc : lEtcs){
      tca.addSample(etc);
    }
  }

  Update calcUpdate(boolean bTest){
    List<Question> lQuestions = bTest ? lTestPruned :lTrainPruned;
    int iOrigTotal = bTest ? lTestQuestions.size() : lTrainQuestions.size();
    TermCountAggregate tca = new TermCountAggregate(iOrigTotal, mSystemCounts);
    computeTermCountAggregate(lQuestions, bTest, tca);
    if(bTest && Config.config.bPrintCorrectSystems){
      try{
        FileUtils.writeLines(new File("correctsystems-" + Model.iTestFold 
                                      + ".txt"),
                             tca.lStats.get(0).lCorrectSystems);
      }catch(IOException ex){
        throw new RuntimeException(ex);
      }
    }

    return tca;
  }
}