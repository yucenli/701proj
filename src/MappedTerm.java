import java.lang.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;
import com.google.common.base.Joiner;

class MappedTerm{
  Comparator<MappedTerm> comparatorTermSignatures = 
    new ComparatorTermSignatures();
  //Comparator<MappedTerm> comparatorFullSignatures = 
  //   new ComparatorFullSignatures();

  Term term;
  //these map slots to unique slots
  TermMap mapUnknowns = new TermMap(this, true);
  TermMap mapNumbers = new TermMap(this, false);

  String[] aUniqueSlotSignatures;
  String[][] aUniqueSlotPairSignatures;

  String[][] aUniqueSubTermSignatures;
  String[][][] aUniqueSubTermPairSignatures;

  String[] aSimpleSlotSignatures;
  String[][] aSimpleSlotPairSignatures;
  String[][] aSimpleSubTermSignatures;
  String[][][] aSimpleSubTermPairSignatures;

  String sSignature;

  List<List<List<Integer>>> lllTermLists;
  List<Double> lNearbyConstants;

  TermMap getMap(boolean bUnknown){
    return bUnknown ? mapUnknowns : mapNumbers;
  }

  MappedTerm(EquationSystem system, List<Boolean> lMinus){
    Misc.Assert(lMinus.size() == system.lEquations.size());
    this.term = new Term();
    this.term.type = Term.Type.Complex;
    this.term.op = Term.Op.SYSTEM;
    this.term.lSubTerms = new ArrayList<MappedTerm>();
    for(int iEquation = 0; iEquation < system.lEquations.size(); iEquation++){
      Boolean bMinus = lMinus.get(iEquation);
      Equation equation = system.lEquations.get(iEquation);
      this.term.lSubTerms.add(new MappedTerm(equation, bMinus));
    }
    initComplexMaps();
    this.getSignature();
    if(Config.config.bNormalizeEquations){
      resort();
    } else {
      //and now that we're done rebuild all the maps all the way down
      this.accept(new MapRebuilder(), new ArrayList<String>());
    }
    buildSimpleSignatures();
    Misc.Assert(this.mapUnknowns.iNumUnique == system.iNumUnknowns);
    Misc.Assert(this.mapNumbers.iNumUnique == system.iNumNumbers);
  }


  MappedTerm(Equation equation, boolean bMinus){
    this.term = new Term();
    this.term.type = Term.Type.Complex;
    this.term.lSubTerms = new ArrayList<MappedTerm>();
    //force it to be a plus at this level
    this.term.op = Term.Op.PLUS;
    if(bMinus){
      addSubTerms(equation.termLeft, true);
      addSubTerms(equation.termRight, false);
    } else {
      addSubTerms(equation.termLeft, false);
      addSubTerms(equation.termRight, true);
    }
    initComplexMaps();
  }

  
  MappedTerm(EquationTerm equationterm, boolean bMinus){
    this.term = new Term();
    if(equationterm instanceof EquationTerm.Complex){
      this.term.type = Term.Type.Complex;
      this.term.lSubTerms = new ArrayList<MappedTerm>();
      addSubTerms(equationterm, bMinus);
      //sort the list and compute the maps
      initComplexMaps();
    } else if(equationterm instanceof EquationTerm.Unknown){
      EquationTerm.Unknown unknown = (EquationTerm.Unknown)equationterm; 
      this.term.iNumUses = unknown.iNumUses;
      this.term.iOriginalIndex = unknown.iIndex;
      this.term.iOriginalOriginalIndex = unknown.iOrigIndex;
      this.term.bMinus = bMinus;
      this.term.type = Term.Type.Unknown;
      this.mapUnknowns.initSingle(unknown.iIndex);
      this.mapNumbers.initEmpty();
    } else if(equationterm instanceof EquationTerm.Number){
      EquationTerm.Number number = (EquationTerm.Number)equationterm; 
      this.term.iNumUses = number.iNumUses;
      this.term.iOriginalIndex = number.iIndex;
      this.term.bMinus = bMinus;
      this.term.type = Term.Type.Number;
      this.mapUnknowns.initEmpty();
      this.mapNumbers.initSingle(number.iIndex);
    } else if(equationterm instanceof EquationTerm.Constant){
      this.term.type = Term.Type.Constant;
      this.term.fConstant = ((EquationTerm.Constant)equationterm).fConstant;
      //for constants, we'll just bake the minus in
      if(bMinus){
        this.term.fConstant = -this.term.fConstant;
      }
      this.mapUnknowns.initEmpty();
      this.mapNumbers.initEmpty();
    } else {
      // no other types
      Misc.Assert(false);
    }
  }

  void buildInstanceTermLists(TermMap tm, int iStart,
                              List<List<Integer>> llTermLists){
    TermMap tmCur = tm;
    for(int iSubTerm = 0; iSubTerm < tmCur.mt.term.lSubTerms.size();iSubTerm++){
      MappedTerm mtSub = tmCur.mt.term.lSubTerms.get(iSubTerm);
      TermMap tmSub = mtSub.getMap(tm.bUnknown);
      for(int iInstance = 0; iInstance < tmSub.numInstances(); iInstance++){
        llTermLists.get(iStart+iInstance).add(iSubTerm);
      }
      //go in one deeper
      if(tmSub.mt.term.type == Term.Type.Complex){
        buildInstanceTermLists(tmSub, iStart,llTermLists);
      }
      iStart += tmSub.numInstances();
    }
  }

  List<List<Integer>> buildInstanceTermLists(boolean bUnknown){
    TermMap tm = this.getMap(bUnknown);
    List<List<Integer>> llTermLists = Misc.genListOfListOf(tm.numInstances());
    buildInstanceTermLists(tm, 0, llTermLists);
    return llTermLists;
  }

  List<List<Integer>> buildSlotToInstanceMap(boolean bUnknown){
    TermMap tm = this.getMap(bUnknown);
    List<List<Integer>> llSlotToInstanceMap;
    if(bUnknown && Config.config.bUnknownWordPerInstance){
       llSlotToInstanceMap = Misc.genListOfListOf(tm.numInstances());
      //build one to one mapping
      for(int iSlot = 0; iSlot < tm.numInstances(); iSlot++){
        llSlotToInstanceMap.get(iSlot).add(iSlot);
      }
    } else {
      llSlotToInstanceMap = Misc.genListOfListOf(tm.iNumUnique);
      //build it from the aMap
      for(int iInstance = 0; iInstance < tm.aMap.length; iInstance++){
        llSlotToInstanceMap.get(tm.aMap[iInstance]).add(iInstance);
      }
    }
    return llSlotToInstanceMap;
  }

  void buildTermLists(){
    List<List<Integer>> llUnknownTermLists = buildInstanceTermLists(true);
    List<List<Integer>> llNumberTermLists = buildInstanceTermLists(false);
    //first build the slot to instance maps
    List<List<Integer>> llUnknownSlotToInstances = buildSlotToInstanceMap(true);
    List<List<Integer>> llNumberSlotToInstances = buildSlotToInstanceMap(false);
    //initialize the lllTermLists
    this.lllTermLists=new ArrayList<List<List<Integer>>>();
    for(int iSlot = 0; iSlot < this.numFilledTermSlots(); iSlot++){
      List<List<Integer>> llTermLists =  new ArrayList<List<Integer>>();
      lllTermLists.add(llTermLists);
      if(iSlot < this.numUnknownSlots()){
        for(Integer iInstance : llUnknownSlotToInstances.get(iSlot)){
          llTermLists.add(llUnknownTermLists.get(iInstance));
        }
      } else {
        int iNumberSlot = iSlot-this.numUnknownSlots();
        List<Integer> lInstances = llNumberSlotToInstances.get(iNumberSlot);
        for(Integer iInstance : lInstances){
          llTermLists.add(llNumberTermLists.get(iInstance));
        }
      }
    }
    List<Double> lUnknownNearbyConstants = buildInstanceNearbyConstants(true);
    List<Double> lNumberNearbyConstants = buildInstanceNearbyConstants(false);
    //now build the nearbyconstant list
    this.lNearbyConstants = Misc.genEmptyList(this.numFilledTermSlots());
    for(int iSlot = 0; iSlot < this.numFilledTermSlots(); iSlot++){
      if(iSlot < this.numUnknownSlots()){
        for(Integer iInstance : llUnknownSlotToInstances.get(iSlot)){
          Double fConstant = lUnknownNearbyConstants.get(iInstance);
          if(fConstant != null){
            this.lNearbyConstants.set(iSlot, fConstant);
          }
        }
      } else {
        int iNumberSlot = iSlot-this.numUnknownSlots();
        List<Integer> lInstances = llNumberSlotToInstances.get(iNumberSlot);
        for(Integer iInstance : lInstances){
          Double fConstant = lNumberNearbyConstants.get(iInstance);
          if(fConstant != null){
            this.lNearbyConstants.set(iSlot, fConstant);
          }
        }
      }
    }
  }      
  
  List<Double> buildInstanceNearbyConstants(boolean bUnknown){
    TermMap tm = this.getMap(bUnknown);
    List<Double> lNearbyConstants = Misc.genEmptyList(tm.numInstances());
    int iInstance = 0;
    for(MappedTerm mtEquation : this.term.lSubTerms){
      //these are the equations
      for(MappedTerm mtTerm : mtEquation.term.lSubTerms){
        //these are the terms
        if(mtTerm.term.type == Term.Type.Complex){
          Double fConstant = null;
          for(MappedTerm mtSub : mtTerm.term.lSubTerms){
            //these are the members of the terms
            if(mtSub.term.type == Term.Type.Constant){
              fConstant = mtSub.term.fConstant;
            }
          }
          int iNumInst = mtTerm.getMap(bUnknown).numInstances();
          for(int iCurInst = 0; iCurInst < iNumInst; iCurInst++){
            lNearbyConstants.set(iInstance, fConstant);
            iInstance++;
          }
        } else {
          int iNumInst = mtTerm.getMap(bUnknown).numInstances();
          iInstance += iNumInst;
        }
      }
    }
    return lNearbyConstants;
  }


  List<List<List<Integer>>> getTermLists(){
    Misc.Assert(lllTermLists != null);
    return lllTermLists;
  }

  List<Set<Pair<Integer,Integer>>> buildPairSets(){
    int iNumSlots = numUnknownSlots() + mapNumbers.iNumUnique;
    List<Set<Pair<Integer,Integer>>> lPairSets = 
      new ArrayList<Set<Pair<Integer,Integer>>>();
    for(int iSlot = 0; iSlot < iNumSlots; iSlot++){
      lPairSets.add(new HashSet<Pair<Integer,Integer>>());
    }
    int iNumberInstance = 0;
    int iUnknownInstance = 0;
    for(int iSub = 0; iSub < this.term.lSubTerms.size(); iSub++){
      MappedTerm mtSub = this.term.lSubTerms.get(iSub);
      //do unknowns
      for(int iCurInstance = 0; iCurInstance < mtSub.mapUnknowns.numInstances();
          iCurInstance++){
        if(Config.config.bUnknownWordPerInstance){
          Set<Pair<Integer,Integer>> ps = lPairSets.get(iUnknownInstance);
          ps.add(ImmutablePair.of(iSub,iCurInstance));
        } else {
          int iThisUnique = this.mapUnknowns.aMap[iUnknownInstance];
          int iSubUnique = mtSub.mapUnknowns.aMap[iCurInstance];
          Set<Pair<Integer,Integer>> ps = lPairSets.get(iThisUnique);
          ps.add(ImmutablePair.of(iSub,iSubUnique));
        }
        iUnknownInstance++;
      }
      //do the numbers
      int iNumSubUnknownSlots = mtSub.numUnknownSlots();
      for(int iCurInstance = 0; iCurInstance < mtSub.mapNumbers.numInstances();
          iCurInstance++){
        int iThisUnique = this.mapNumbers.aMap[iNumberInstance];
        int iSubUnique = mtSub.mapNumbers.aMap[iCurInstance];
        Set<Pair<Integer,Integer>> ps = 
          lPairSets.get(iThisUnique+numUnknownSlots());
        ps.add(ImmutablePair.of(iSub,iSubUnique+iNumSubUnknownSlots));
        iNumberInstance++;
      }
    }
    return lPairSets;
  }

  char slotLetter(int iSlot, int iNumUnknowns){
    if(iSlot < iNumUnknowns){
      return (char) ('m' + iSlot); 
    } else {
      return (char) ('a' + (iSlot-iNumUnknowns));
    }
  }

  int numUnknownSlots(){
    return Config.config.bUnknownWordPerInstance ?
      mapUnknowns.numInstances() : mapUnknowns.iNumUnique;
  }
    
  List<Double> solve(List<SimpleSolver> lSolvers, 
                     List<Double> lUniqueNumbers){
    Misc.Assert(lUniqueNumbers.size() == mapNumbers.iNumUnique);
    //remap the numbers to instance slots
    //List<Double> lInstanceNumbers = mapNumbers.remap(lUniqueNumbers);
    SimpleSolver solver = lSolvers.get(mapUnknowns.iNumUnique-1);
    return this.term.solve(solver, lUniqueNumbers, mapUnknowns.iNumUnique);
  }



  public void buildSimpleSignatures(){
    //the simple signatures are really simple
    int iNumSlots = numUnknownSlots() + mapNumbers.iNumUnique;

    //so the basic slots are simple
    aSimpleSlotSignatures = new String[iNumSlots];
    aSimpleSlotPairSignatures = new String[iNumSlots][iNumSlots];
    String sSignature = this.toString(false, true);
    for(int iSlot1 = 0; iSlot1 < iNumSlots; iSlot1++){
      aSimpleSlotSignatures[iSlot1] = sSignature + "->" 
        + slotLetter(iSlot1, numUnknownSlots());
      for(int iSlot2 = 0; iSlot2 < iNumSlots; iSlot2++){
        aSimpleSlotPairSignatures[iSlot1][iSlot2] = 
          sSignature + "->" + slotLetter(iSlot1, numUnknownSlots()) 
          + ":" + slotLetter(iSlot2, numUnknownSlots());
      }
    }
    aSimpleSubTermSignatures = new String[iNumSlots][0];
    aSimpleSubTermPairSignatures = new String[iNumSlots][iNumSlots][0];
    //now build the pair sets
    List<Set<Pair<Integer,Integer>>> lPairSets = buildPairSets();
    //and use the pair sets to fill in the signatures
    for(int iSlot1 = 0; iSlot1 < iNumSlots; iSlot1++){
      Set<Pair<Integer,Integer>> setPairs1 = lPairSets.get(iSlot1);
      Set<String> setSingleSigs = new HashSet<String>();
      for(Pair<Integer,Integer> pair : setPairs1){
        MappedTerm mtSub = this.term.lSubTerms.get(pair.getLeft());
        int iNumUnknownSlotsSub = Config.config.bUnknownWordPerInstance ?
          mtSub.mapUnknowns.numInstances() : mtSub.mapUnknowns.iNumUnique;
        setSingleSigs.add(mtSub.toString(false,true) + "->" + 
                          slotLetter(pair.getRight(), iNumUnknownSlotsSub));
      }
      aSimpleSubTermSignatures[iSlot1] = setSingleSigs.toArray(new String[0]);
      for(int iSlot2 = 0; iSlot2 < iNumSlots; iSlot2++){
        Set<Pair<Integer,Integer>> setPairs2 = lPairSets.get(iSlot2);
        Set<String> setPairSigs = new HashSet<String>();
        for(Pair<Integer,Integer> pair1 : setPairs1){
          for(Pair<Integer,Integer> pair2 : setPairs2){
            if(pair1.getLeft() == pair2.getLeft()){
              MappedTerm mtSub = this.term.lSubTerms.get(pair1.getLeft());
              int iNumUnknownSlotsSub = Config.config.bUnknownWordPerInstance ?
                mtSub.mapUnknowns.numInstances() : mtSub.mapUnknowns.iNumUnique;
              String sSig = mtSub.toString(false,true) 
                + "->" + slotLetter(pair1.getRight(), iNumUnknownSlotsSub) 
                + ":" + slotLetter(pair2.getRight(), iNumUnknownSlotsSub);
              setPairSigs.add(sSig);
            }
          }
        }
        String[] aPairSigs = setPairSigs.toArray(new String[0]);
        aSimpleSubTermPairSignatures[iSlot1][iSlot2] = aPairSigs;
      }
    }
  }


  public void accept(MappedTermVisitor mtv, List<String> lOps){
    //we'll do post order, start with the subterms
    if(this.term.type == Term.Type.Complex){
      String sOp;
      if(this.term.op == Term.Op.SYSTEM){
        sOp = "=";
      } else {
        sOp = this.term.op.toString();
      }
      if(this.term.bMinus){
        sOp = "-" + sOp;
      }
      lOps.add(sOp);
      for(MappedTerm mtSub : term.lSubTerms){
        mtSub.accept(mtv, lOps);
      }
      lOps.remove(lOps.size()-1);
    }
    //now go to this term
    if(this.term.bMinus){
      lOps.add("-");
      mtv.visit(this, lOps);
      lOps.remove(lOps.size()-1);
    } else {
      mtv.visit(this, lOps);
    }
  }

  // void setFirstUses(){
  //   //first calcFirstUsesForEachInstance
  //   int[] aUniqueFirstUse = new int[iNumUnique];
  //   Arrays.fill(aUniqueFirstUse, Integer.MAX_VALUE);
  //   Arrays.fill(aInstanceFirstUse, Integer.MAX_VALUE);
  //   for(int iInstance = 0; iInstance < numInstances(); iInstance++){
  //     iUnique = aMap[iInstance];
  //     iUniqueFirstUse[iUnique] = Math.min(iUniqueFirstUse[iUnique], iInstance);
  //   }
  //   List<Term> lTerminals = getTerminals();
  //   for(int iInstance = 0; iInstance < numInstances(); iInstance++){
  //     lTerminals.get(iInstance).iFirstUse = aUniqueFirstUse[aMap[iInstance]];
  //   }
  // }

  void addSubTerms(EquationTerm equationterm, boolean bMinus){
    boolean bCompatibleComplexTerm = false;
    EquationTerm.Complex complex = null;
    if(equationterm instanceof EquationTerm.Complex){
      complex = (EquationTerm.Complex) equationterm;
      if(this.term.op == null){
        bCompatibleComplexTerm = true;
        if(complex.op == Operator.PLUS){
          this.term.op = Term.Op.PLUS;
        } else if(complex.op == Operator.TIMES){
          this.term.op = Term.Op.TIMES;
        } else if(complex.op == Operator.DIVIDEBY){
          this.term.op = Term.Op.DIVIDEBY;
        } else if(complex.op == Operator.MINUS){
          this.term.op = Term.Op.PLUS;
        } else {
          Misc.Assert(false);
        }
        //pass bMinus down for sum terms, otherwise stick it into this term
        if(this.term.op != Term.Op.PLUS){
          this.term.bMinus = bMinus;
          bMinus = false;
        }
      } else if(this.term.op == Term.Op.PLUS){
        if((complex.op == Operator.PLUS) || (complex.op == Operator.MINUS)){
          bCompatibleComplexTerm = true;
        }
      } else if(this.term.op == Term.Op.TIMES){
        if(complex.op == Operator.TIMES){
          bCompatibleComplexTerm = true;
        }
      } else {
        //minus is not allowed
        Misc.Assert(this.term.op == Term.Op.DIVIDEBY);
      }
    }
    if(bCompatibleComplexTerm){
      addSubTerms(complex.termLeft, bMinus);
      boolean bRightMinus = complex.op == Operator.MINUS ? !bMinus : bMinus;
      addSubTerms(complex.termRight, bRightMinus);
    } else {
      this.term.lSubTerms.add(new MappedTerm(equationterm, bMinus));
    }
  }

  int numFilledTermSlots(){
    return this.numUnknownSlots()+mapNumbers.iNumUnique;
  }

  int numUniqueSlots(){
    return mapUnknowns.numUnknownsFilledTerm() + mapNumbers.iNumUnique;
  }

  int numInstanceSlots(){
    return mapUnknowns.numInstances()+mapNumbers.numInstances();
  }

  List<Object> getSlotSignatures(int iSlot){
    Misc.Assert(Config.config.bUseNonCanonicalSlotSigs ||
                Config.config.bUseCanonicalSlotSigs);
    List<Object> lSignatures = new ArrayList<Object>();
    if(Config.config.bUseNonCanonicalSlotSigs){
      lSignatures.add(aSimpleSlotSignatures[iSlot]);
      if(Config.config.bUseSubTermSigs && (this.term.lSubTerms.size() != 1)){
        String[] aSubTermSignatures = aSimpleSubTermSignatures[iSlot];
        for(String sSubTermSignature : aSubTermSignatures){
          lSignatures.add(sSubTermSignature);
        }
      }
    }
    if(Config.config.bUseCanonicalSlotSigs){
      //system level signature
      lSignatures.add((Object)aUniqueSlotSignatures[iSlot]);
      //all the equation level signatures
      if(Config.config.bUseSubTermSigs  && (this.term.lSubTerms.size() != 1)){
        String[] aSubTermSignatures = aUniqueSubTermSignatures[iSlot];
        for(String sSubTermSignature : aSubTermSignatures){
          lSignatures.add(sSubTermSignature);
        }
      }
    }
    return lSignatures;
  }


  List<Object> getSlotSignatures(int iSlot1, int iSlot2){
    List<Object> lSignatures = new ArrayList<Object>();
    if(Config.config.bUseNonCanonicalSlotSigs){
      lSignatures.add(aSimpleSlotPairSignatures[iSlot1][iSlot2]);
      if(Config.config.bUseSubTermSigs && (this.term.lSubTerms.size() != 1)){
        String[] aSubTermSignatures = 
          aSimpleSubTermPairSignatures[iSlot1][iSlot2];
        for(String sSubTermSignature : aSubTermSignatures){
          lSignatures.add(sSubTermSignature);
        }
      }
    }
    if(Config.config.bUseCanonicalSlotSigs){
      lSignatures.add((Object)aUniqueSlotPairSignatures[iSlot1][iSlot2]);
      //all the equation level signatures
      if(Config.config.bUseSubTermSigs && (this.term.lSubTerms.size() != 1)){
        String[] aSubTermSignatures =
          aUniqueSubTermPairSignatures[iSlot1][iSlot2];
        for(String sSubTermSignature : aSubTermSignatures){
          Misc.Assert(sSubTermSignature != null);
          lSignatures.add(sSubTermSignature);
        }
      }
    }
    return lSignatures;
  }


  String getSlotInstanceSignature(int iSlot){
    if(iSlot < mapUnknowns.numInstances()){
      return mapUnknowns.lSlotInstanceSignatures.get(iSlot);
    } else {
      return mapNumbers.lSlotInstanceSignatures.get(iSlot-
                                                    mapUnknowns.numInstances());
    }
  }
  
  int getAgreementDepth(List<Integer> list1, List<Integer> list2){
    int iSize = Math.min(list1.size(), list2.size());
    for(int iIndex = 0; iIndex < iSize; iIndex++){
      if(list1.get(iIndex) != list2.get(iIndex)){
        return iIndex;
      }
    }
    //unless these are the same exact term, these should disagree before this
    System.out.println("Bad Lists: " + list1 + list2);
    Misc.Assert(false);
    return -1;
  }

  void buildSlotSignatures(){
    //first calc the term indexlists
    List<List<Integer>> llTermIndexes = mapUnknowns.calcTermIndexLists();
    llTermIndexes.addAll(mapNumbers.calcTermIndexLists());
    //now build the boolean lists for each pair
    int iNumSlots = this.numInstanceSlots();

    //1. we'll build the slotinstance pair signatures but not keep them around
    String[][] aInstancePairSignatures = 
      new String[iNumSlots][iNumSlots];
    for(int iSlot1 = 0; iSlot1 < iNumSlots; iSlot1++){
      for(int iSlot2 = 0; iSlot2 < iNumSlots; iSlot2++){
        if(iSlot1 == iSlot2){
          continue;
        }
        //build the boolean agreement list
        int iAgreementDepth = getAgreementDepth(llTermIndexes.get(iSlot1),
                                                llTermIndexes.get(iSlot2));
        aInstancePairSignatures[iSlot1][iSlot2] = new StringBuilder()
          .append("[")
          .append(getSlotInstanceSignature(iSlot1)).append(",")
          .append(getSlotInstanceSignature(iSlot2)).append(",")
          .append(iAgreementDepth)
          .append("]")
          .toString();
      }
    }
    //2. build the unique slot to slot instance mapping
    List<List<Integer>> llSlotInstances = 
      mapUnknowns.calcSlotInstanceLists(0, false);
    int iUnknownInstances = mapUnknowns.numInstances();
    llSlotInstances.addAll(mapNumbers.calcSlotInstanceLists(iUnknownInstances, 
                                                            false));

    //now build the unique pair signatures
    int iNumUnique = mapUnknowns.numUnknownsFilledTerm()+mapNumbers.iNumUnique;
    aUniqueSlotPairSignatures = new String[iNumUnique][iNumUnique];
    for(int iSlot1 = 0; iSlot1 < iNumUnique; iSlot1++){
      for(int iSlot2 = 0; iSlot2 < iNumUnique; iSlot2++){
        if(iSlot1 == iSlot2){
          continue;
        }
        List<String> lInstancePairs = new ArrayList<String>();
        for(Integer iInstanceSlot1 : llSlotInstances.get(iSlot1)){
          List<String> lInstancePairsCurSlot = new ArrayList<String>();
          for(Integer iInstanceSlot2 : llSlotInstances.get(iSlot2)){
            Misc.Assert(iInstanceSlot1 != iInstanceSlot2);
            String sInstancePairSignature = 
              aInstancePairSignatures[iInstanceSlot1][iInstanceSlot2];
            Misc.Assert(sInstancePairSignature != null);
            lInstancePairsCurSlot.add(sInstancePairSignature);
          }
          Collections.sort(lInstancePairsCurSlot);
          lInstancePairs.add(lInstancePairsCurSlot.toString());
        }
        Collections.sort(lInstancePairs);
        String sCurSig = Joiner.on(",").join(lInstancePairs);
        aUniqueSlotPairSignatures[iSlot1][iSlot2] = new StringBuilder()
          .append("PAIR-SIG")
          .append(this.getSignature()).append("-->")
          .append("[").append(sCurSig).append("]")
          .toString();
      }
    }

    // now build the individual unique slot signatures
    aUniqueSlotSignatures = new String[iNumUnique];
    for(int iUniqueSlot = 0; iUniqueSlot < iNumUnique; iUniqueSlot++){
      List<String> lInstancesCurSlot = new ArrayList<String>();
      for(Integer iInstanceSlot : llSlotInstances.get(iUniqueSlot)){
        lInstancesCurSlot.add(getSlotInstanceSignature(iInstanceSlot));
      }
      Collections.sort(lInstancesCurSlot);
      String sCurSig = Joiner.on(",").join(lInstancesCurSlot);
      aUniqueSlotSignatures[iUniqueSlot] = new StringBuilder()
        .append("SINGLE-SIG")
        .append(this.getSignature()).append("-->")
        .append("[").append(sCurSig).append("]")
        .toString();
    }      
  }

  Set<Pair<Integer,Integer>> getSubTermMap(int iUniqueSlot){
    if(iUniqueSlot < mapUnknowns.numUnknownsFilledTerm()){
      return mapUnknowns.lSubTermMaps.get(iUniqueSlot);
    } else {
      return  mapNumbers.lSubTermMaps.get(iUniqueSlot-
                                          mapUnknowns.numUnknownsFilledTerm());
    }
  }

  int remapSlot(MappedTerm mt, int iUnique, int iSlot){
    if(iUnique < mapUnknowns.numUnknownsFilledTerm()){
      return iSlot;
    } else {
      return iSlot + mt.mapUnknowns.iNumUnique;
    }
  }

  void buildSubTermSignatures(){
    // build the single signatures
    this.aUniqueSubTermSignatures = new String[this.numUniqueSlots()][0];
    for(int iUnique = 0; iUnique < this.numUniqueSlots(); iUnique++){
      Set<Pair<Integer,Integer>> setSubTermMap = getSubTermMap(iUnique);
      String[] aCurSigs = new String[setSubTermMap.size()];
      this.aUniqueSubTermSignatures[iUnique] = aCurSigs;
      int iSig = 0;
      for(Pair<Integer,Integer> pairCur : setSubTermMap){
        int iSubTerm = pairCur.getLeft();
        int iUniqueSlot = pairCur.getRight();
        MappedTerm mtSub = this.term.lSubTerms.get(iSubTerm);
        if(mtSub.term.type == Term.Type.Complex){
          aCurSigs[iSig] = 
            "SUBTERM-SIG[" + mtSub.aUniqueSlotSignatures[iUniqueSlot]+"]";
        } else {
          aCurSigs[iSig] = "STUB";
        }
        iSig++;
      }
    }
    //build the pair signatures
    this.aUniqueSubTermPairSignatures = 
      new String[this.numUniqueSlots()][this.numUniqueSlots()][0];
    for(int iUnique1 = 0; iUnique1 < this.numUniqueSlots(); iUnique1++){
      Set<Pair<Integer,Integer>> setSubTermMaps1 = getSubTermMap(iUnique1);
      for(int iUnique2 = 0; iUnique2 < this.numUniqueSlots(); iUnique2++){
        if(iUnique1 == iUnique2){
          continue;
        }
        List<String> lSigs = new ArrayList<String>();
        Set<Pair<Integer,Integer>> setSubTermMaps2 = getSubTermMap(iUnique2);
        for(Pair<Integer,Integer> pair1 : setSubTermMaps1){
          for(Pair<Integer,Integer> pair2 : setSubTermMaps2){
            if(pair1.getLeft() == pair2.getLeft()){
              MappedTerm mtSub = this.term.lSubTerms.get(pair1.getLeft());
              int iSlot1 = remapSlot(mtSub, iUnique1, pair1.getRight());
              int iSlot2 = remapSlot(mtSub, iUnique2, pair2.getRight());
              //they're in the same subterm so add the appropriate sig
              if(mtSub.term.type == Term.Type.Complex){
                String sNewSig = "SUBTERM-PAIR-SIG[" + 
                  mtSub.aUniqueSlotPairSignatures[iSlot1][iSlot2] + "]";
                lSigs.add(sNewSig);
              } else {
                lSigs.add("STUB");
              }
            }
          }
        }
        this.aUniqueSubTermPairSignatures[iUnique1][iUnique2] = 
          lSigs.toArray(new String[lSigs.size()]);
      }
    }
  }


  static class SlotSetter implements MappedTermVisitor{
    public void visit(MappedTerm mt, List<String> lOps){
      mt.term.sPathSig = lOps.toString();
    }
  }

  static class Resorter implements MappedTermVisitor{
    public void visit(MappedTerm mt, List<String> lOps){
      if(mt.term.type == Term.Type.Complex){
        mt.sortSubTerms();
      }
    }
  }
  
  static class TerminalGetter implements MappedTermVisitor{
    List<MappedTerm> lUnknowns = new ArrayList<MappedTerm>();
    List<MappedTerm> lNumbers = new ArrayList<MappedTerm>();
    public void visit(MappedTerm mt, List<String> lOps){
      if(mt.term.type == Term.Type.Unknown){
        lUnknowns.add(mt);
      } else if(mt.term.type == Term.Type.Number){
        lNumbers.add(mt);
      }
    }
  }

  List<MappedTerm> getTerminals(){
    TerminalGetter tg = new TerminalGetter();
    this.accept(tg, new ArrayList<String>());
    Misc.Assert(tg.lUnknowns.size() == mapUnknowns.numInstances());
    Misc.Assert(tg.lNumbers.size() == mapNumbers.numInstances());
    List<MappedTerm> lTerminals = new ArrayList<MappedTerm>(tg.lUnknowns);
    lTerminals.addAll(tg.lNumbers);
    return lTerminals;
  }

  void rebuildMaps(){
    //1. get the terminals in the new order
    TerminalGetter tg = new TerminalGetter();
    this.accept(tg, new ArrayList<String>());
    mapUnknowns.rebuildMaps(tg.lUnknowns);
    mapNumbers.rebuildMaps(tg.lNumbers);
  }

  static class MapRebuilder implements MappedTermVisitor{
    public void visit(MappedTerm mt, List<String> lOps){
      if(mt.term.type == Term.Type.Complex){
        mt.rebuildMaps();
      }
    }
  }

  void resort(){
    //set everything
    SlotSetter setter = new SlotSetter();
    this.accept(setter, new ArrayList<String>());
    List<MappedTerm> lTerminals = getTerminals();
    List<List<String>> llSigs = new ArrayList<List<String>>();
    for(int iUnique = 0; iUnique < numUniqueSlots(); iUnique++){
      llSigs.add(new ArrayList<String>());
    }
    //build the full map
    List<Integer> lFullMap = new ArrayList<Integer>();
    for(int iInstance = 0; iInstance < numInstanceSlots(); iInstance++){
      int iUnique;
      if(iInstance < mapUnknowns.numInstances()){
        lFullMap.add(mapUnknowns.aMap[iInstance]);
      } else {
        lFullMap.add(mapNumbers.aMap[iInstance-mapUnknowns.numInstances()]+
                     mapUnknowns.iNumUnique);
      }
    }
    for(int iInstance = 0; iInstance < numInstanceSlots(); iInstance++){
      int iUnique = lFullMap.get(iInstance);
      llSigs.get(iUnique).add(lTerminals.get(iInstance).term.sPathSig);
    }
    //resort all the lists
    for(int iUnique = 0; iUnique < numUniqueSlots(); iUnique++){
      Collections.sort(llSigs.get(iUnique));
    }
    //now stick them back into the sigs
    for(int iInstance = 0; iInstance < numInstanceSlots(); iInstance++){
      int iUnique = lFullMap.get(iInstance);
      lTerminals.get(iInstance).term.sPathSig = llSigs.get(iUnique).toString();
    }
    //and now resort based on these
    //now go into a resort loop for a minute:
    String sPrev = null;
    int iIter = 0;
    Resorter resorter = new Resorter();
    while(!this.toString(true,false).equals(sPrev)){
      Misc.Assert(iIter < 10);
      iIter++;
      sPrev = this.toString(true,false);
      //resort
      this.accept(resorter, new ArrayList<String>());
      //and rebuild the maps
      rebuildMaps();
    }
    //and now that we're done rebuild all the maps all the way down
    this.accept(new MapRebuilder(), new ArrayList<String>());
  }


  void sortSubTerms(){
    if(this.term.op != Term.Op.DIVIDEBY){
      Collections.sort(this.term.lSubTerms,comparatorTermSignatures);
    }
  }


  void initComplexMaps(){
    //first sort the sublist by the term signatures
    if(Config.config.bNormalizeEquations){
      sortSubTerms();
    }
    mapUnknowns.initComplex();
    mapNumbers.initComplex();
    buildSlotSignatures();
    buildSubTermSignatures();
  }

  static class ComparatorTermSignatures implements Comparator<MappedTerm>{
    public int compare(MappedTerm mt1, MappedTerm mt2){
      return mt1.term.recalcSignature().compareTo(mt2.term.recalcSignature());
    }
  }


  String recalcSignature(){
    StringBuilder sb = new StringBuilder();
    //first add the term signature
    sb.append(this.term.recalcSignature());
    //now add signature sets for all the equivalence sets
    //1. calc the equivalence sets themselves
    sb.append("--->");
    this.mapUnknowns.appendSignatureToStringBuilder(sb);
    this.mapNumbers.appendSignatureToStringBuilder(sb);
    return sb.toString();
  }

  String getSignature(){
    if(sSignature == null){
      this.sSignature = recalcSignature();
    }
    return sSignature;
  }
 
  String calcConcreteSignature(List<Double> lUniqueNumbers){
    StringBuilder sb = new StringBuilder();
    List<Double> lInstanceDoubles = mapNumbers.remap(lUniqueNumbers);
    //first add the term signature
    sb.append(this.term.calcConcreteSignature(lInstanceDoubles, 0));
    //add equivalence set signatures only for unknowns
    sb.append("--->");
    this.mapUnknowns.appendSignatureToStringBuilder(sb, lInstanceDoubles);
    return sb.toString();
  }
  
  public int hashCode(){
    return this.getSignature().hashCode();
  }


  public boolean equals(Object obj){
    if(!(obj instanceof MappedTerm)){
      return false;
    }
    MappedTerm mtOther = (MappedTerm) obj;
    return this.getSignature().equals(mtOther.getSignature());
  }


  StringBuilder toStringBuilder(StringBuilder sb, 
                                List<String> lUnknowns, 
                                List<String> lNumbers){
    return toStringBuilder(sb, lUnknowns, lNumbers, false, false);
  }
  StringBuilder toStringBuilder(StringBuilder sb, 
                                List<String> lUnknowns, 
                                List<String> lNumbers,
                                boolean bIncludeExtras,
                                boolean bFeatureString){
    //only remap once at the top level, recursive calls do not call this
    // recursive calls call directly through to term.toStringBuilder
    List<String> lFullUnknowns;
    if(Config.config.bUnknownWordPerInstance && 
       (lUnknowns.size() == mapUnknowns.numInstances())){
      lFullUnknowns = lUnknowns;
    } else {
      lFullUnknowns = mapUnknowns.remap(lUnknowns);
    }
    List<String> lFullNumbers = mapNumbers.remap(lNumbers);
    return term.toStringBuilder(sb, lFullUnknowns, lFullNumbers,bIncludeExtras,
                                bFeatureString);
  }

  StringBuilder toStringBuilder(StringBuilder sb){
    return toStringBuilder(sb, false, false);
  }
  StringBuilder toStringBuilder(StringBuilder sb, boolean bIncludeExtras,
                                boolean bFeatureString){
    List<String> lUnknowns = 
      Misc.listOfLetters('m', mapUnknowns.numUnknownsFilledTerm());
    List<String> lNumbers = Misc.listOfLetters('a', mapNumbers.iNumUnique);
    return toStringBuilder(sb, lUnknowns, lNumbers, bIncludeExtras,
                           bFeatureString);
  }

  public String toString(){
    return toString(false, false);
  }
  public String toString(boolean bIncludeExtras, boolean bFeatureString){
    return toStringBuilder(new StringBuilder(),bIncludeExtras,
                           bFeatureString).toString();
  }

}