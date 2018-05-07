import java.lang.*;
import java.util.*;

class FilledTerm implements Comparable<FilledTerm>{
  MappedTerm mt;
  List<StanfordWord> lUnknownWords;
  List<StanfordWord> lNumberWords;
  FeatureListList fll;
  String sConcreteSignature = null;
  List<Slot> lSlots;
  int iNextSlot = 0;

  FilledTerm(MappedTerm mt){
    this.mt = mt;
    this.lUnknownWords = Misc.genEmptyList(mt.numUnknownSlots());
    this.lNumberWords = Misc.genEmptyList(mt.mapNumbers.iNumUnique);
  }

  FilledTerm(FilledTerm ft, List<StanfordWord> lUnknownWords){
    //first make sure this filled term has all numbers and no unknowns filled in
    for(StanfordWord word : ft.lNumberWords){
      Misc.Assert(word != null);
    }
    for(StanfordWord word : ft.lUnknownWords){
      Misc.Assert(word == null);
    }
    this.mt = ft.mt;
    //don't need to copy lnumberwords because it's already full, and we
    // never modify a filledterm, only augument it
    this.lNumberWords = ft.lNumberWords;
    Misc.Assert(lUnknownWords.size() == mt.numUnknownSlots());
    this.lUnknownWords = lUnknownWords;
  }


  FilledTerm(FilledTerm ft, int iSlot, StanfordWord wordNew){
    if(Config.config.bFillSlotsInOrder){
      Misc.Assert(ft.iNextSlot == iSlot);
      this.iNextSlot = ft.iNextSlot+1;
    }
    this.mt = ft.mt;
    this.lUnknownWords = new ArrayList<StanfordWord>(ft.lUnknownWords);
    this.lNumberWords = new ArrayList<StanfordWord>(ft.lNumberWords);
    if(iSlot < mt.numUnknownSlots()){
      Misc.Assert(lUnknownWords.get(iSlot) == null);
      this.lUnknownWords.set(iSlot, wordNew);
    } else {
      int iNumberSlot = iSlot-mt.numUnknownSlots();
      Misc.Assert(lNumberWords.get(iNumberSlot) == null);
      lNumberWords.set(iNumberSlot, wordNew);
    }
  }


  List<Double> solve(List<SimpleSolver> lSolvers){
    Misc.Assert(this.isFilled());
    List<Double> lNumberDoubles = StanfordNumber.wordsToDoubles(lNumberWords);
    List<Double> lUniqueSolutions = mt.solve(lSolvers, lNumberDoubles);
    if(lUniqueSolutions == null){
      // we can't fill the slots in this case
      return null;
    }
    if(lUniqueSolutions.size() != mt.mapUnknowns.iNumUnique){
      System.out.println("NumSolutions: " + lUniqueSolutions.size() + 
                         " " + mt.mapUnknowns.iNumUnique);
      Misc.Assert(lUniqueSolutions.size() == mt.mapUnknowns.iNumUnique);
    }
    List<Double> lSolutions = lUniqueSolutions;
    if(lSolutions.size() != lUnknownWords.size()){
      //need to remap
      lSolutions = mt.mapUnknowns.remap(lSolutions);
    }
    Misc.Assert(lSolutions.size() == lUnknownWords.size());
    //now stick the solutions into the slots
    if(lSlots == null){
      buildSlots();
    }
    for(int iUnknown = 0; iUnknown < lUnknownWords.size(); iUnknown++){
      Slot slotUnknown = lSlots.get(iUnknown);
      Misc.Assert(!slotUnknown.isNumber());
      slotUnknown.fUnknownSolution = lSolutions.get(iUnknown);
    }
    return lUniqueSolutions;
  }

  public int compareTo(FilledTerm ftOther){
    Misc.Assert(this.fll != null);
    Misc.Assert(ftOther.fll != null);
    double fCompare = this.fll.fCrossProd - ftOther.fll.fCrossProd;
    return Misc.doubleToComparableInt(fCompare);
  }

  int numSlots(){
    return this.mt.numFilledTermSlots();
  }

  boolean isSlotEmpty(int iSlot){
    if(iSlot < mt.numUnknownSlots()){
      return (lUnknownWords.get(iSlot) == null);
    } else {
      return (lNumberWords.get(iSlot-mt.numUnknownSlots()) == null);
    }
  }
  

  void buildSlots(){
    this.lSlots = new ArrayList<Slot>(this.numSlots());
    for(int iSlot = 0; iSlot < this.numSlots(); iSlot++){
      if(this.isSlotEmpty(iSlot)){
        lSlots.add(null);
        continue;
      }
      if(iSlot < mt.numUnknownSlots()){
        lSlots.add(new Slot(lUnknownWords.get(iSlot), false, 
                            mt.getTermLists().get(iSlot), 
                            mt.lNearbyConstants.get(iSlot)));
      } else {
        int iNumberSlot = iSlot-this.mt.numUnknownSlots();
        lSlots.add(new Slot(lNumberWords.get(iNumberSlot), true,
                            mt.getTermLists().get(iSlot), 
                            mt.lNearbyConstants.get(iSlot)));
      }
    }
  }

  Slot getSlot(int iSlot){
    Misc.Assert(lUnknownWords.size() == mt.numUnknownSlots());
    Misc.Assert(lNumberWords.size() == mt.mapNumbers.iNumUnique);
    if(lSlots == null){
      buildSlots();
    }
    return lSlots.get(iSlot);
  }


  void score(Question question, FeatureCache cache, 
             List<SimpleSolver> lSolvers){
    if(this.fll == null){
      this.fll = 
        Model.features.derivationfeatures.getFeatures(this, question, cache,
                                                      lSolvers);
    }
  }

  public boolean equal(Object obj){
    if(!(obj instanceof FilledTerm)){
      return false;
    }
    FilledTerm ftOther = (FilledTerm) obj;
    return this.mt.getSignature().equals(ftOther.mt.getSignature()) &&
      this.lUnknownWords.equals(ftOther.lUnknownWords) &&
      this.lNumberWords.equals(ftOther.lNumberWords);
  }

  public int hashCode(){
    int iHashCode = this.mt.getSignature().hashCode();
    iHashCode = Misc.hashCombine(iHashCode, lUnknownWords);
    iHashCode = Misc.hashCombine(iHashCode, lNumberWords);
    return iHashCode;
  }

  boolean isFilled(){
    return !(lUnknownWords.contains(null) || lNumberWords.contains(null));
  }

  StanfordWord getUniqueSlotNoun(int iSlot){
    Misc.Assert(lUnknownWords.size() == mt.numUnknownSlots());
    Misc.Assert(lNumberWords.size() == mt.mapNumbers.iNumUnique);
    if(iSlot < mt.numUnknownSlots()){
      return lUnknownWords.get(iSlot);
    } else {
      StanfordWord wordNumber = lNumberWords.get(iSlot-mt.numUnknownSlots());
      if(wordNumber == null){
        return null;
      } else {
        Misc.Assert(wordNumber.number.wordNoun != null);
        return wordNumber.number.wordNoun;
      }
    }
  }
  boolean isCorrect(Question question, boolean bTest, 
                    List<SimpleSolver> lSolvers){
    if(Config.config.bPredictOnlyTemplates){
      return this.mt.equals(question.ct.mt);
    } else if(Config.config.bSemiSupervised){
      if(Config.config.bUseOnlyHandAnnotationQuestionsForTrain){
        if(bTest){
          return question.isCorrectNumerically(this, lSolvers);
        } else {
          if((Config.config.bRequireUnknownStringOverlap ||
              (Config.config.bRequireUnknownStringOverlapForOnlyGold &&
               question.bGoldUnknowns))){
            return isCorrectStrict(question);
          } else {
            return isCorrectLoose(question.ct);
          }
        }
      } else if(Config.config.bUseOnlyHandAnnotationQuestionsAnnotatedForTrain){
        if(bTest){
          return question.isCorrectNumerically(this, lSolvers);
        } else if(Model.lHandAnnotations.contains(question.iIndex)){
          if((Config.config.bRequireUnknownStringOverlap ||
              (Config.config.bRequireUnknownStringOverlapForOnlyGold &&
               question.bGoldUnknowns))){
            return isCorrectStrict(question);
          } else {
            return isCorrectLoose(question.ct);
          }
        } else {
          return question.isCorrectNumerically(this, lSolvers);
        }
      }
      Misc.Assert(false);
      return false;
    } else {
      if(!bTest && 
         (Config.config.bRequireUnknownStringOverlap ||
          (Config.config.bRequireUnknownStringOverlapForOnlyGold &&
           question.bGoldUnknowns))){
        return isCorrectStrict(question);
      } else {
        return isCorrectLoose(question.ct);
      }
    }
  }

  List<Double> getRoundedSortedNumericalSolution(List<SimpleSolver> lSolvers){
    List<Double> lCalced = this.solve(lSolvers);
    if(lCalced == null){
      return null;
    }
    List<Double> lRounded = new ArrayList<Double>();
    for(Double fCalced : lCalced){
      lRounded.add(Misc.roundToTwoDecimalPlaces(fCalced));
    }
    Collections.sort(lRounded);
    return lRounded;
  }


  boolean isCorrectStrict(Question question){
    //first check that it's the correct system
    if(!this.mt.toString(false,true)
       .equals(question.ct.mt.toString(false,true))){
      return false;
    }

    //first check the numbers
    Misc.Assert(this.lNumberWords.size() == question.ct.lNumberDoubles.size());
    //first check the numbers
    for(int iNumber = 0; iNumber < this.lNumberWords.size(); iNumber++){
      if(Double.compare(this.lNumberWords.get(iNumber).number.fNumber,
                         question.ct.lNumberDoubles.get(iNumber)) != 0){
        return false;
      }
    }
    //now check the unknowns
    for(int iUnknown = 0; iUnknown < this.lUnknownWords.size(); iUnknown++){
      int iUnique = iUnknown;
      if(Config.config.bUnknownWordPerInstance){
        iUnique = question.ct.mt.mapUnknowns.aMap[iUnknown];
      }
      List<StanfordWord> lAllowed = question.llAllowedUnknowns.get(iUnique);
      if(!lAllowed.contains(lUnknownWords.get(iUnknown))){
        return false;
      }
    }
    //it passed all the tests
    return true;
  }

  boolean isCorrectLoose(ConcreteTerm ctCorrect){
    return this.getConcreteSignature().equals(ctCorrect.getSignature());
  }


  String calcConcreteSignature(){
    List<Double> lNumberDoubles = new ArrayList<Double>();
    for(StanfordWord wordNumber : lNumberWords){
      lNumberDoubles.add(wordNumber.number.fNumber);
    }
    if(Config.config.bNormalizeEquations){
      return mt.calcConcreteSignature(lNumberDoubles);
    } else {
      return mt.toString(false,true)+lNumberDoubles;
    }
  }

  String getConcreteSignature(){
    if(sConcreteSignature == null){
      Misc.Assert(isFilled());
      sConcreteSignature = calcConcreteSignature();
    }
    return sConcreteSignature;
  }


  StringBuilder toStringBuilder(StringBuilder sb){
    Misc.Assert(this.isFilled());
    List<String> lUnknownStrings = new ArrayList<String>();
    for(StanfordWord word : lUnknownWords){
      lUnknownStrings.add(word.toFullString());
    }
    List<String> lNumberStrings = new ArrayList<String>();
    for(StanfordWord word : lNumberWords){
      lNumberStrings.add(word.toFullString());
    }
    mt.toStringBuilder(sb, lUnknownStrings, lNumberStrings);
    return sb;
  }

  public String toString(){
    return toStringBuilder(new StringBuilder()).toString();
  }

}