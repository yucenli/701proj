import java.lang.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;

class Equ implements Comparable<Equ>{
  List<EquTerm> lTerms = new ArrayList<EquTerm>();
  List<List<EquTerm>> lEqualitySets = new ArrayList<List<EquTerm>>();
  boolean bFinished = false;
  FeatureListList fll = null;
  
  List<String> lAllTerms = new ArrayList<String>();
  List<String> lPosTerms = new ArrayList<String>();
  List<String> lNegTerms = new ArrayList<String>();

  Equ(){
  }

  Equ(Equ equToCopy, Slot slotAugment, StanfordWord wordAugment){
    this.copyFrom(equToCopy);
    //now augment
    this.augment(slotAugment, wordAugment);
    this.fixTerms();
  }
  
  Equ(Equ equToCopy, boolean bFinished){
    this.copyFrom(equToCopy);
    this.bFinished = bFinished;
    this.fixTerms();
  }

  int numUnknownSlots(){
    int iNumUnknownSlots = 0;
    for(EquTerm term : lTerms){
      if(term.wordUnknown != null){
        iNumUnknownSlots++;
      }
    }
    return iNumUnknownSlots;
  }

  int numNumberSlots(){
    int iNumNumberSlots = 0;
    for(EquTerm term : lTerms){
      iNumNumberSlots += term.lFactors.size();
    }
    return iNumNumberSlots;
  }


  // void computeStructuralLists(){
  //   for(EquTerm term : equ.lTerms){
  //     String sTerm = (String) memcache.get(term.toStructuralString());
  //     lAllTerms.add(sTerm);
  //     if(term.bMinus){
  //       lNegTerms.add(sTerm);
  //     } else {
  //       lPosTerms.add(sTerm);
  //     }
  //   }
  //   Collections.sort(lAllTerms);
  //   Collections.sort(lPosTerms);
  //   Collections.sort(lNegTerms);
  // }

  void fixTerms(){
    for(EquTerm term : lTerms){
      term.fix();
    }
  }


  void copyFrom(Equ equToCopy){
    Map<EquTerm,EquTerm> mTerms = new HashMap<EquTerm,EquTerm>();
    for(EquTerm termToCopy : equToCopy.lTerms){
      EquTerm termNew = new EquTerm(termToCopy);
      lTerms.add(termNew);
      mTerms.put(termToCopy, termNew);
    }
    for(List<EquTerm> lEqualitySetToCopy : equToCopy.lEqualitySets){
      List<EquTerm> lNewEqualitySet = new ArrayList<EquTerm>();
      this.lEqualitySets.add(lNewEqualitySet);
      for(EquTerm termFrom : lEqualitySetToCopy){
        lNewEqualitySet.add(mTerms.get(termFrom));
      }
    }
  }


  boolean containsWord(StanfordWord word){
    Misc.Assert(word != null);
    for(EquTerm term : lTerms){
      if(term.containsWord(word)){
        return true;
      }
    }
    return false;
  }

  public int compareTo(Equ equOther){
    //score them if necessary
    this.score();
    equOther.score();
    return Misc.doubleToComparableInt(this.fll.fCrossProd - 
                                      equOther.fll.fCrossProd);
  }


  void score(){
    if(this.fll == null){
      this.fll = Model.features.derivationfeatures.getFeatures(this);
    }
  }

  boolean isEmpty(){
    return (lTerms.size() == 0);
  }

  Equ genFinishedCopy(){
    Misc.Assert(!this.isEmpty());
    return new Equ(this, true);
  }

  void augment(Slot slot, StanfordWord word){
    Misc.Assert(this.fll == null);
    EquTerm termToAugment = null;
    if(slot.iTerm == null){
      termToAugment = new EquTerm(slot.bMinus);
      lTerms.add(termToAugment);
    } else {
      termToAugment = lTerms.get(slot.iTerm);
    }
    if(slot.iEqualitySet != null){
      Misc.Assert(termToAugment.wordUnknown == null);
      //get the word from the existing entry in the equality set
      List<EquTerm> lEqualitySet = this.lEqualitySets.get(slot.iEqualitySet);
      StanfordWord wordUnknown = lEqualitySet.get(0).wordUnknown;
      Misc.Assert(wordUnknown != null);
      //add the word to the term
      termToAugment.addUnknown(wordUnknown);
      //add the term to the equality set
      lEqualitySet.add(termToAugment);
    } else if(slot.bUnknown){
      termToAugment.addUnknown(word);
      if(slot.iEqualitySet == null){
        //add a new equality set with a single term in it
        List<EquTerm> lEqualitySet = new ArrayList<EquTerm>();
        lEqualitySet.add(termToAugment);
        lEqualitySets.add(lEqualitySet);
      } else {
        lEqualitySets.get(slot.iEqualitySet).add(termToAugment);
      }
    } else if(slot.bDenominator){
      termToAugment.addDenominator(word);
    } else {
      termToAugment.addFactor(word);
    }
  }

  class FilledSlot{
    StanfordWord word;
    StanfordWord wordOrig;
    Slot slot;
    FilledSlot(StanfordWord word, Slot slot){
      this.slot = slot;
      this.wordOrig = word;
      if(!slot.bUnknown){
        //get the number noun
        this.word = word.number.wordNoun;
      } else {
        this.word = word;
      }
    }
  }
  
  class Slot{
    boolean bUnknown = false;
    Integer iTerm = null;
    boolean bDenominator = false;
    boolean bMinus = false;
    Integer iEqualitySet = null;

    Equ getEqu(){
      return Equ.this;
    }
    Equ genAugmentedCopy(StanfordWord word){
      //first copy the enclosing class
      return new Equ(Equ.this, this, word);
    }
    String getType(){
      Misc.Assert(iTerm != null);
      EquTerm term = lTerms.get(iTerm);
      String sMinus = term.bMinus ? "MINUS-" : "";
      if(bUnknown){
        return (sMinus + "UNKNOWN");
      } else if(bDenominator){
        return (sMinus + "DENOMINATOR");
      } else {
        return (sMinus + "FACTOR");
      }
    }
  }
  
  Slot genUnknownSlot(int iTerm){
    Slot slot = new Slot();
    slot.iTerm = iTerm;
    slot.bUnknown = true;
    return slot;
  }

  Slot genExistingUnknownSlot(int iTerm, int iEqualitySet){
    Slot slot = new Slot();
    slot.bUnknown = true;
    slot.iTerm = iTerm;
    slot.iEqualitySet = iEqualitySet;
    return slot;
  }


  Slot genNewUnknownSlot(boolean bMinus){
    Slot slot = new Slot();
    slot.bUnknown = true;
    slot.bMinus = bMinus;
    return slot;
  }

  Slot genNewExistingUnknownSlot(boolean bMinus, int iEqualitySet){
    Slot slot = new Slot();
    slot.bUnknown = true;
    slot.bMinus = bMinus;
    slot.iEqualitySet = iEqualitySet;
    return slot;
  }

  Slot genFactorSlot(int iTerm){
    Slot slot = new Slot();
    slot.iTerm = iTerm;
    return slot;
  }

  Slot genDenominatorSlot(int iTerm){
    Slot slot = new Slot();
    slot.iTerm = iTerm;
    slot.bDenominator = true;
    return slot;
  }

  Slot genNewFactorSlot(boolean bMinus){
    Slot slot = new Slot();
    slot.bMinus = bMinus;
    return slot;
  }

  Slot genNewDenominatorSlot(boolean bMinus){
    Slot slot = new Slot();
    slot.bMinus = bMinus;
    slot.bDenominator = true;
    return slot;
  }

  List<Slot> getAvailableUnknownSlots(){
    Misc.Assert(!bFinished);
    List<Slot> lSlots = new ArrayList<Slot>();
    for(int iTerm = 0; iTerm < lTerms.size(); iTerm++){
      EquTerm term = lTerms.get(iTerm);
      if(term.wordUnknown == null){
        lSlots.add(genUnknownSlot(iTerm));
      }
    }
    if(lTerms.size() < Config.config.iMaxEquTerms){
      lSlots.add(genNewUnknownSlot(false));
      lSlots.add(genNewUnknownSlot(true));
    }
    return lSlots;
  }

  List<Slot> getAvailableNumberSlots(){
    Misc.Assert(!bFinished);
    List<Slot> lSlots = new ArrayList<Slot>();
    for(int iTerm = 0; iTerm < lTerms.size(); iTerm++){
      EquTerm term = lTerms.get(iTerm);
      if(term.lFactors.size() < Config.config.iMaxEquFactors){
        lSlots.add(genFactorSlot(iTerm));
      }
      if(term.wordDenominator == null){
        lSlots.add(genDenominatorSlot(iTerm));
      }
    }
    if(lTerms.size() < Config.config.iMaxEquTerms){
      lSlots.add(genNewFactorSlot(false));
      lSlots.add(genNewDenominatorSlot(false));
      lSlots.add(genNewFactorSlot(true));
      lSlots.add(genNewDenominatorSlot(true));
    }
    return lSlots;
  }

  List<Slot> getAvailableEqualUnknownSlots(){
    List<Slot> lSlots = new ArrayList<Slot>();
    for(int iTerm = 0; iTerm < lTerms.size(); iTerm++){
      EquTerm term = lTerms.get(iTerm);
      if(term.wordUnknown == null){
        for(int iEqualitySet = 0; iEqualitySet < lEqualitySets.size(); 
            iEqualitySet++){
          lSlots.add(genExistingUnknownSlot(iTerm, iEqualitySet));
        }
      }
    }
    if(lTerms.size() < Config.config.iMaxEquTerms){
      for(int iEqualitySet = 0; iEqualitySet < lEqualitySets.size(); 
          iEqualitySet++){
        lSlots.add(genNewExistingUnknownSlot(false, iEqualitySet));
        lSlots.add(genNewExistingUnknownSlot(true, iEqualitySet));
      }
    }    
    return lSlots;
  }

  List<FilledSlot> getFilledSlots(){
    //this is not currently coded to handle multislots
    Misc.Assert(Config.config.bPruneOutMultiQuestions);
    List<FilledSlot> lFilledSlots = new ArrayList<FilledSlot>();
    for(int iTerm = 0; iTerm < lTerms.size(); iTerm++){
      EquTerm term = lTerms.get(iTerm);
      if(term.wordDenominator != null){
        lFilledSlots.add(new FilledSlot(term.wordDenominator, 
                                  genDenominatorSlot(iTerm)));
      }
      if(term.wordUnknown != null){
        lFilledSlots.add(new FilledSlot(term.wordUnknown, 
                                  genUnknownSlot(iTerm)));
      }
      for(StanfordWord word : term.lFactors){
        lFilledSlots.add(new FilledSlot(word, genFactorSlot(iTerm)));
      }
    }
    return lFilledSlots;
  }

  public int hashCode(){
    int iHashCode = -1;
    for(EquTerm term : lTerms){
      iHashCode ^= term.hashCode();
    }
    return iHashCode;
  }

  public boolean equals(Object obj){
    if(!(obj instanceof Equ)){
      return false;
    }
    return equalsExact((Equ) obj);
  }

  //*********compare to other Equ
  boolean equalsExact(Equ equOther){
    // check if it's directly equal or if it's equal * -1
    return this.equalsExact(equOther, false) || this.equalsExact(equOther,true);
  }

  boolean equalsExact(Equ equOther, boolean bOpposite){
    // if the sizes of things aren't equal they can't possibly be equal
    if((this.lTerms.size() != equOther.lTerms.size()) || 
       (this.lEqualitySets.size() != equOther.lEqualitySets.size())){
      return false;
    }
    //check that the terms themselves can be made equal
    Relation<EquTerm,EquTerm> relation = Relation.EquEquExact;
    if(!Misc.equalSets(this.lTerms, equOther.lTerms, relation)){
      return false;
    }
    //check that the equalitysets can also be made equal
    return Misc.equalSets(this.lEqualitySets, equOther.lEqualitySets, 
                          Relation.EquEquExactList);
  }

  // //*********compare to other Equ
  // boolean equalsConcrete(Equ equOther){
  //   // check if it's directly equal or if it's equal * -1
  //   return this.equalsConcrete(equOther, false) || 
  //     this.equalsConcrete(equOther,true);
  // }

  // boolean equalsConcrete(Equ equOther, boolean bOpposite){
  //   // if the sizes of things aren't equal they can't possibly be equal
  //   if((this.lTerms.size() != equOther.lTerms.size()) || 
  //      (this.lEqualitySets.size() != equOther.lEqualitySets.size())){
  //     return false;
  //   }
  //   //check that the terms themselves can be made equal
  //   Relation<EquTerm,EquTerm> relation = Relation.getEquEqu(bOpposite);
  //   if(!Misc.equalSets(this.lTerms, equOther.lTerms, relation)){
  //     return false;
  //   }
  //   //check that the equalitysets can also be made equal
  //   return Misc.equalSets(this.lEqualitySets, equOther.lEqualitySets, 
  //                         Relation.getEquEquList(bOpposite));
  // }

  // boolean concreteSubsetOf(Equ equOther){
  //   return this.concreteSubsetOf(equOther, false) || 
  //     this.concreteSubsetOf(equOther, true);
  // }

  // boolean concreteSubsetOf(Equ equOther, boolean bOpposite){
  //   //first check sizes
  //   if((this.lTerms.size() > equOther.lTerms.size()) ||
  //      (this.lEqualitySets.size() > equOther.lEqualitySets.size())){
  //     return false;
  //   }
  //   // check that the terms are a subset
  //   Relation<EquTerm,EquTerm> relation = Relation.getEquEqu(bOpposite);
  //   if(!Misc.isASubsetOfB(this.lTerms, equOther.lTerms, relation)){
  //     return false;
  //   }
  //   //check that the equalitysets can also be make a subset
  //   return Misc.isASubsetOfB(this.lEqualitySets, equOther.lEqualitySets, 
  //                            Relation.getEquEquList(bOpposite));
  // }

  //*********compare to concrete*******************8

  boolean equalsConcrete(ConcreteEqu equOther){
    // check if it's directly equal or if it's equal * -1
    return this.equalsConcrete(equOther, false) || 
      this.equalsConcrete(equOther,true);
  }

  boolean equalsConcrete(ConcreteEqu equOther, boolean bOpposite){
    // if the sizes of things aren't equal they can't possibly be equal
    Misc.Assert(this != null);
    Misc.Assert(equOther != null);
    Misc.Assert(this.lTerms != null);
    Misc.Assert(equOther.lTerms != null);
    if((this.lTerms.size() != equOther.lTerms.size()) || 
       (this.lEqualitySets.size() != equOther.lEqualitySets.size())){
      //System.out.println("Bad sizes");
      return false;
    }
    //check that the terms themselves can be made equal
    Relation<EquTerm,OldConcreteTerm> relation =Relation.getEquConcrete(bOpposite);
    if(!Misc.equalSets(this.lTerms, equOther.lTerms, relation)){
      //System.out.println("Bad Terms");
      return false;
    }
    //check that the equalitysets can also be made equal
    boolean bResult = Misc.equalSets(this.lEqualitySets, equOther.lEqualitySets,
                                     Relation.getEquConcreteList(bOpposite));
    // if(!bResult){
    //   System.out.println("BadEqualitySets");
    // }
    return bResult;
  }

  boolean concreteSubsetOf(ConcreteEqu equOther){
    return this.concreteSubsetOf(equOther, false) || 
      this.concreteSubsetOf(equOther, true);
  }

  boolean concreteSubsetOf(ConcreteEqu equOther, boolean bOpposite){
    //first check sizes
    if((this.lTerms.size() > equOther.lTerms.size()) ||
       (this.lEqualitySets.size() > equOther.lEqualitySets.size())){
      return false;
    }
    // check that the terms are a subset
    Relation<EquTerm,OldConcreteTerm> relation =Relation.getEquConcrete(bOpposite);
    if(!Misc.isASubsetOfB(this.lTerms, equOther.lTerms, relation)){
      return false;
    }
    //check that the equalitysets can also be make a subset
    return Misc.isASubsetOfB(this.lEqualitySets, equOther.lEqualitySets, 
                             Relation.getEquConcreteList(bOpposite));
  }


  public String toString(){
    return toStringBuilder(new StringBuilder()).toString();
  }

  public StringBuilder toStringBuilder(StringBuilder sb){
    for(EquTerm term : lTerms){
      term.toStringBuilder(sb);
    }
    sb.append(" = 0");
    return sb;
  }

  public String toConcreteString(){
    return toConcreteStringBuilder(new StringBuilder()).toString();
  }

  public StringBuilder toConcreteStringBuilder(StringBuilder sb){
    for(EquTerm term : lTerms){
      term.concrete.toStringBuilder(sb);
    }
    sb.append(" = 0");
    return sb;
  }

}