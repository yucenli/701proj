import java.lang.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.math3.util.CombinatoricsUtils;

class EquClassifier extends BruteForceClassifier{
  List<Question> lTrainQuestions;
  List<Question> lTestQuestions;
  List<Question> lTrainPruned;
  List<Question> lTestPruned;
  List<QDerivation> lTrainDerivations;
  List<QDerivation> lTestDerivations;
  int iOrigNumEquationsTrain;
  int iOrigNumEquationsTest;

  //Map<Pair<StanfordWord,StanfordWord>,List<Object>> mWordPairFeatures = 
  //  new HashMap<Pair<StanfordWord,StanfordWord>,List<Object>>();

  EquClassifier(List<Question> lTrainQuestions, 
                     List<Question> lTestQuestions){
    this.lTrainQuestions = lTrainQuestions;
    this.lTestQuestions = lTestQuestions;
    this.lTrainPruned = prune(lTrainQuestions);
    this.lTestPruned = prune(lTestQuestions);
    iOrigNumEquationsTrain = calcNumEquations(lTrainQuestions);
    iOrigNumEquationsTest = calcNumEquations(lTestQuestions);
    //Model.features.derivationfeatures.initializeStructures(lTrainPruned);
  }

  List<Question> prune(List<Question> lQuestions){
    List<Question> lQuestionsPruned = new ArrayList<Question>();
    for(Question question : lQuestions){
      if(question.lCorrectEqus != null){
        if(Config.config.bPruneOutMultiQuestions){
          //check if any equations are multi
          boolean bHasMulti = false;
          for(Equation equation : question.system.lSingleEquations){
            if(SlotCounter.hasMultiSlotsOrConstants(equation)){
              bHasMulti = true;
              break;
            }
          }
          if(!bHasMulti){
            lQuestionsPruned.add(question);
          }
        } else {
          lQuestionsPruned.add(question);
        }
      }
    }
    return lQuestionsPruned;
  }

  static int calcNumEquations(List<Question> lQuestions){
    int iNumEquations = 0;
    for(Question question : lQuestions){
      iNumEquations+= question.system.lSingleEquations.size();
    }
    return iNumEquations;
  }

  static boolean allFinished(List<Equ> lEqu){
    for(Equ equ : lEqu){
      if(!equ.bFinished){
        return false;
      }
    }
    return true;
  }

  static List<Equ> computeNBestEqu(Question question, 
                                   ConcreteEqu concreteCorrect){
    List<StanfordWord> lNouns = question.doc.lNouns;
    List<StanfordWord> lNumberWords = question.doc.lNumberWords;
    List<Equ> lBeam = new ArrayList<Equ>();
    //start with just and empty equ
    lBeam.add(new Equ());
    int iIter = 0;
    while(!allFinished(lBeam)){
      //we'll do this as a set to remove duplicates
      Set<Equ> setNewEqu = new HashSet<Equ>();
      for(Equ equ : lBeam){
        if(equ.bFinished){
          setNewEqu.add(equ);
          continue;
        }
        //augment new unknowns
        List<Equ.Slot> lUnknownSlots = equ.getAvailableUnknownSlots();
        for(Equ.Slot slotUnknown : lUnknownSlots){
          for(StanfordWord wordNoun : lNouns){
            if(!slotUnknown.getEqu().containsWord(wordNoun)){
              setNewEqu.add(slotUnknown.genAugmentedCopy(wordNoun));
            }
          }
        }
        //augment new numbers
        List<Equ.Slot> lNumberSlots = equ.getAvailableNumberSlots();
        for(Equ.Slot slotNumber : lNumberSlots){
          for(StanfordWord wordNumber : lNumberWords){
            if(!slotNumber.getEqu().containsWord(wordNumber)){
              setNewEqu.add(slotNumber.genAugmentedCopy(wordNumber));
            }
          }
        }
        //equal unknown slots
        for(Equ.Slot slot : equ.getAvailableEqualUnknownSlots()){
          setNewEqu.add(slot.genAugmentedCopy(null));
        }

        //also consider the possibility of just finishing the equ here
        if(!equ.isEmpty()){
          setNewEqu.add(equ.genFinishedCopy());
        }
      }
      //now turn the set back into a list and sort it
      if(concreteCorrect != null){
        // System.out.println("Beam Before Prune at Iter: " + iIter + " Size: " 
        //                    +setNewEqu.size());
        // for(Equ equ : setNewEqu){
        //   System.out.println("Equ: " + equ + " Finished: " + equ.bFinished + 
        //                      " Equal: " + equ.equalsConcrete(concreteCorrect) + 
        //                      " subset: " + 
        //                      equ.concreteSubsetOf(concreteCorrect) 
        //                      + " Concrete: " + equ.toConcreteString());
        // }
        //filter out everything which could not be grown into the correct answer
        lBeam = new ArrayList<Equ>();
        for(Equ equ : setNewEqu){
          if(equ.bFinished){
            //if it's finished then it must be equal
            if(equ.equalsConcrete(concreteCorrect)){
              lBeam.add(equ);
            }
          } else 
            if(equ.concreteSubsetOf(concreteCorrect)){
            lBeam.add(equ);
          }
        }
        // System.out.println("Beam After Prune at Iter: " + iIter + " Size: " 
        //                    +lBeam.size());
        // for(Equ equ : lBeam){
        //   System.out.println("Equ: " + equ + " Finished: " + equ.bFinished + 
        //                      " Equal: " + equ.equalsConcrete(concreteCorrect) + 
        //                      " subset: " + 
        //                    equ.concreteSubsetOf(concreteCorrect) 
        //                      + " Concrete: " + equ.toConcreteString());
        // }
      } else {
        //take everything for now
        lBeam = new ArrayList<Equ>(setNewEqu);
      }
      //first score everything
      for(Equ equ : lBeam){
        equ.score();
      }
      Collections.sort(lBeam, Collections.reverseOrder());
      //now prune the beam
      if(lBeam.size() > Config.config.iMaxBeamSize){
        //create a new arraylist so the make sure to garbage collect the rest
        lBeam =new ArrayList<Equ>(lBeam.subList(0, Config.config.iMaxBeamSize));
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


  static class ComputeExpectedAggregateCallable implements Callable<Object>{
    Question question;
    ExpectedAggregate ea;
    ComputeExpectedAggregateCallable(Question question, ExpectedAggregate ea){
      this.question = question;
      this.ea = ea;
    }
    public Object call(){
      try{
        computeExpectedAggregate(question, ea);
      }catch(Exception ex){
        System.out.println("Exception in parallel execution for question:" + 
                           question.iIndex);
        ex.printStackTrace();
        System.exit(-1);
        throw ex;
      }
      return null;
    }
  }

  static boolean hasAll(List<Equ> lEqus, List<ConcreteEqu> lToFind){
    for(ConcreteEqu concreteToFind : lToFind){
      boolean bFound = false;
      for(Equ equ : lEqus){
        if(equ.equalsConcrete(concreteToFind)){
          bFound = true;
          break;
        }
      }
      if(!bFound){
        return false;
      }
    }
    //found them all
    return true;
  }


  static void computeExpectedAggregate(Question question, ExpectedAggregate ea){
    //System.out.println("**********Computing Equ For: " + question.iIndex);
    //first compute the nbest overall
    List<Equ> lNBest = computeNBestEqu(question, null);
    boolean bHasAllCorrectInBeam = hasAll(lNBest, question.lCorrectEqus);
    ExpectedCounts ecAllInBeam =
      new ExpectedCounts(lNBest,question.lCorrectEqus);
    Set<Equ> setAllNBest = new HashSet<Equ>(lNBest);
    // and then do an update for each correct equation
    List<ExpectedCounts> lExpectedCountsCorr = new ArrayList<ExpectedCounts>();
    for(int iCorrect = 0; iCorrect < question.lCorrectEqus.size();iCorrect++){
      ConcreteEqu concreteCorr = question.lCorrectEqus.get(iCorrect);
      //System.out.println("*************Computing for Question: " 
      //                   + question.iIndex + " Equation: " 
      //                   + concreteCorr.toString());
      List<Equ> lEquCur = computeNBestEqu(question, concreteCorr);
      setAllNBest.addAll(lEquCur);
      ExpectedCounts ecCorr = new ExpectedCounts(lEquCur, null);
      lExpectedCountsCorr.add(ecCorr);
    }
    ExpectedCounts ecAll =new ExpectedCounts(setAllNBest,question.lCorrectEqus);
    for(int iCorrect = 0; iCorrect < question.lCorrectEqus.size();iCorrect++){
      ea.addSample(ecAll, lExpectedCountsCorr.get(iCorrect),
                   ecAll.lCorrectCounts.get(iCorrect), 
                   ecAllInBeam.lCorrectCounts.get(iCorrect), ecAllInBeam);
      //ecAllInBeam.lCorrectCounts.get(iCorrect).print("Question-" + 
      //                                               question.iIndex + ": ");
    }

    ea.addQuestionSample(bHasAllCorrectInBeam);
  }


  static void computeExpectedAggregate(List<Question> lQuestions,
                                       ExpectedAggregate ea){
    Model.features.derivationfeatures.resetCache();
    if(Config.config.bParallelize){
      List<Callable<Object>> lCallables = new ArrayList<Callable<Object>>();
      for(Question question : lQuestions){
        lCallables.add(new ComputeExpectedAggregateCallable(question, ea));
      }
      Parallel.process(lCallables);
    } else {
      for(Question question : lQuestions){
        computeExpectedAggregate(question, ea);
      }
    }
  }

  Update calcUpdate(boolean bTest){
    List<Question> lQuestions = bTest ? lTestPruned :lTrainPruned;
    int iOrigEquations = bTest ? iOrigNumEquationsTest : iOrigNumEquationsTrain;
    int iOrigQuestions = bTest ? lTestQuestions.size() : lTrainQuestions.size();
    ExpectedAggregate ea = new ExpectedAggregate(iOrigQuestions,iOrigEquations);
    computeExpectedAggregate(lQuestions, ea);
    return ea;
  }
}