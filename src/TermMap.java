import java.lang.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;
import com.google.common.collect.Ordering;
import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;

class TermMap{
  MappedTerm mt;
  boolean bUnknown;
  //maps instance slots to unique slots
  int[] aMap;
  int iNumUnique;
  List<List<Integer>> lUniqueToInstanceMap;

  List<String> lSlotInstanceSignatures;
  List<List<Integer>> lEquivalenceSets;
  List<List<Integer>> lTermIndexLists;  
  List<Set<Pair<Integer,Integer>>> lSubTermMaps;
  //only used while building
  List<Integer> lIndexes;
  List<Integer> lOriginalIndexes;
  TermMap(MappedTerm mt, boolean bUnknown){
    this.mt = mt;
    this.bUnknown = bUnknown;
  }
  int numInstances(){
    return aMap.length;
  }

  int numUnknownsFilledTerm(){
    Misc.Assert(bUnknown);
    if(Config.config.bBrokenUnknownWordPerInstance){
      return this.numInstances();
    } else {
      return this.iNumUnique;
    }
  }


  void initEmpty(){
    this.aMap = new int[0];
    this.iNumUnique = 0;
    this.lIndexes = new ArrayList<Integer>();
    setTermNum();
  }
  void initSingle(int iIndex){
    this.aMap = new int[]{0};
    this.iNumUnique = 1;
    this.lIndexes = Arrays.asList(iIndex);
    setTermNum();
  }
  //actually build the map
  void initComplex(){
    // these map unique slots to indexes
    List<Integer> lUniqueToIndex = new ArrayList<Integer>();
    //these map instance slots to unique slots, will get turned to arrays
    List<Integer> lMap = new ArrayList<Integer>();
    // these map instance slots to the original indexes
    // these are only used during initialization
    this.lIndexes = new ArrayList<Integer>();
    for(MappedTerm mt : this.mt.term.lSubTerms){
      TermMap tmSub = mt.getMap(bUnknown);
      buildMap(tmSub.lIndexes, lUniqueToIndex, lMap);
      this.lIndexes.addAll(tmSub.lIndexes);
      tmSub.lIndexes = null;
    }
    //store what we need now
    this.iNumUnique = lUniqueToIndex.size();
    this.aMap = Ints.toArray(lMap);
    setTermNum();
    calcEquivalenceSets();
    buildSlotSignatures();
    calcSubTermMaps();
  }

  void rebuildMaps(List<MappedTerm> lTerminals){
    //lterminals contains all the terminals in the new order
    List<Integer> lIndexes = new ArrayList<Integer>();
    if(this.lIndexes != null){
      this.lOriginalIndexes = Misc.genEmptyList(this.numInstances());
    }
    for(int iInstance = 0;iInstance < this.numInstances();iInstance++){
      MappedTerm mtCur = lTerminals.get(iInstance);
      int iCur = lIndexes.indexOf(mtCur.term.iOriginalIndex);
      if(this.lIndexes != null){
        this.lIndexes.set(iInstance, mtCur.term.iOriginalIndex);
        this.lOriginalIndexes.set(iInstance, mtCur.term.iOriginalOriginalIndex);
      }
      if(iCur != -1){
        this.aMap[iInstance] = iCur;
        mtCur.term.iUniqueId = iCur;
      } else {
        this.aMap[iInstance] = lIndexes.size();
        mtCur.term.iUniqueId = lIndexes.size();
        lIndexes.add(mtCur.term.iOriginalIndex);
      }
    }
  }


  void calcTermIndexLists(MappedTerm mtCur, int iStartSlot, 
                          List<List<Integer>> llIndexes){
    if(mtCur.term.type != Term.Type.Complex){
      return;
    }
    for(int iSubTerm = 0; iSubTerm < mtCur.term.lSubTerms.size(); iSubTerm++){
      MappedTerm mtSub = mtCur.term.lSubTerms.get(iSubTerm);
      TermMap tmSub = mtSub.getMap(bUnknown);
      for(int iSlot = iStartSlot; iSlot < iStartSlot + tmSub.numInstances(); 
          iSlot++){
        llIndexes.get(iSlot).add(iSubTerm);
      }
      calcTermIndexLists(mtSub, iStartSlot, llIndexes);
      iStartSlot += tmSub.numInstances();
    }
  }


  List<List<Integer>> calcTermIndexLists(){
    List<List<Integer>> llIndexes = new ArrayList<List<Integer>>();
    for(int iSlot = 0; iSlot < this.numInstances(); iSlot++){
      llIndexes.add(new ArrayList<Integer>());
    }
    calcTermIndexLists(this.mt, 0, llIndexes);
    return llIndexes;
  }


  //these map unique slots at this level to a list of subterms and unique
  // slots at the next level
  // when Config.config.bUnknowWordPerInstance is true each slots maps
  // to a single slot on the term below
  void calcSubTermMaps(){
    this.lSubTermMaps = new ArrayList<Set<Pair<Integer,Integer>>>();
    int iCurNumUnique = this.iNumUnique;
    if(this.bUnknown){
      iCurNumUnique = numUnknownsFilledTerm();
    }
    for(int iUnique = 0; iUnique < iCurNumUnique; iUnique++){
      lSubTermMaps.add(new HashSet<Pair<Integer,Integer>>());
    }
    int iStartSlot = 0;
    int iSubTerm = 0;
    MappedTerm mtSub = this.mt.term.lSubTerms.get(0);
    TermMap tmSub = mtSub.getMap(bUnknown);
    for(int iInstanceSlot = 0; iInstanceSlot < aMap.length; iInstanceSlot++){
      while(iInstanceSlot-iStartSlot == tmSub.numInstances()){
        iSubTerm++;
        mtSub = this.mt.term.lSubTerms.get(iSubTerm);
        tmSub = mtSub.getMap(bUnknown);
        iStartSlot = iInstanceSlot;
      }
      if(Config.config.bBrokenUnknownWordPerInstance && bUnknown){
        int iSubSlot = iInstanceSlot-iStartSlot;
        lSubTermMaps.get(iInstanceSlot).add(ImmutablePair.of(iSubTerm, 
                                                             iSubSlot));
      } else {
        int iSubSlot = mtSub.getMap(bUnknown).aMap[iInstanceSlot-iStartSlot];
        lSubTermMaps.get(aMap[iInstanceSlot]).add(ImmutablePair.of(iSubTerm, 
                                                                   iSubSlot));
      }
    }
  }



  static void buildMap(List<Integer> lIndexes, List<Integer> lUniqueToIndex,
                       List<Integer> lUniqueMap){
    //input is lIndexes, output is lUniqueMap, and lUnqueToIndex gets updates
    // as necessary
    for(Integer iIndex : lIndexes){
      int iUnique = lUniqueToIndex.indexOf(iIndex);
      if(iUnique != -1){
        lUniqueMap.add(iUnique);
      } else {
        lUniqueMap.add(lUniqueToIndex.size());
        lUniqueToIndex.add(iIndex);
      }
    }
  }

  void setTermNum(){
    if(bUnknown){
      mt.term.iNumUnknowns = this.numInstances();
    } else {
      mt.term.iNumNumbers = this.numInstances();
    }
  }      

  void buildSlotSignatures(){
    Misc.Assert(mt.term.type == Term.Type.Complex);
    this.lSlotInstanceSignatures = new ArrayList<String>();
    for(int iSlot = 0; iSlot < this.numInstances(); iSlot++){
      int iIndex = iSlot;
      String sSignature = null;
      int iSub = 0;
      for(MappedTerm mtSub : mt.term.lSubTerms){
        TermMap tmSub = mtSub.getMap(bUnknown);
        if(iIndex < tmSub.numInstances()){
          sSignature = mtSub.term.recalcSignature();
          if((this.mt.term.op == Term.Op.DIVIDEBY) && (iSub==1)){
            sSignature = "/" + sSignature;
          }
          if(tmSub.numInstances() > 1){
            sSignature += ":" + tmSub.lSlotInstanceSignatures.get(iIndex);
          } else {
            Misc.Assert(tmSub.numInstances() == 1);
            if(bUnknown){
              sSignature += ":?";
            } else {
              sSignature += ":#";
            } 
          }
          break;
        } else {
          iIndex -= tmSub.numInstances();
        }
        iSub++;
      }
      Misc.Assert(sSignature != null);
      lSlotInstanceSignatures.add(sSignature);
    }
  }

  String getConcreteSlotSignature(int iSlot, List<Double> lNumberDoubles){
    MappedTerm mtCur = mt;
    List<String> lConcrete = new ArrayList<String>();
    int iStart = 0;
    while(mtCur.term.type == Term.Type.Complex){
      for(MappedTerm mtSub : mtCur.term.lSubTerms){
        TermMap tmSub = mtSub.getMap(bUnknown);
        if(iSlot < tmSub.numInstances()){
          lConcrete.add(mtSub.term.calcConcreteSignature(lNumberDoubles, 
                                                         iStart));
          mtCur = mtSub;
          break;
        } else {
          iSlot -= tmSub.numInstances();
          iStart += mtSub.term.iNumNumbers;
        }
      }
    }
    return Joiner.on(":").join(lConcrete);
  }

  List<List<Integer>> calcSlotInstanceLists(int iOffset, 
                                            boolean bAlwaysUnique){
    List<List<Integer>> llSlotInstanceLists = new ArrayList<List<Integer>>();
    if(!bAlwaysUnique && bUnknown && 
       Config.config.bBrokenUnknownWordPerInstance){
      Misc.Assert(iOffset == 0);
      //the mapping is just one to one in this case
      for(int iUnique = 0; iUnique < this.numInstances(); iUnique++){
        llSlotInstanceLists.add(Arrays.asList(iUnique));
      }
    } else {
      for(int iUnique = 0; iUnique < this.iNumUnique; iUnique++){
        llSlotInstanceLists.add(new ArrayList<Integer>());
      }
      for(int iSlot = 0; iSlot < aMap.length; iSlot++){
        llSlotInstanceLists.get(aMap[iSlot]).add(iSlot+iOffset);
      }
    }
    return llSlotInstanceLists;
  }

  void calcEquivalenceSets(){
    List<List<Integer>> lAllSets = calcSlotInstanceLists(0, true);
    //now prune to only include lists with more than one
    List<List<Integer>> lPrunedSets = new ArrayList<List<Integer>>();
    for(List<Integer> lCur : lAllSets){
      if(lCur.size() > 1){
        lPrunedSets.add(lCur);
      }
    }
    this.lEquivalenceSets = lPrunedSets;
  }

  StringBuilder appendSignatureToStringBuilder(StringBuilder sb){
    if(lEquivalenceSets.size() == 0){
      sb.append("[]");
      return sb;
    }
    List<String> lEquivalenceSignatures = new ArrayList<String>();
    for(List<Integer> lEquivalenceSet : lEquivalenceSets){
      List<String> lCurSigs = Misc.remap(lEquivalenceSet, 
                                         lSlotInstanceSignatures);
      //now sort them
      Collections.sort(lCurSigs, Collections.reverseOrder());
      lEquivalenceSignatures.add(Joiner.on(",").join(lCurSigs));
    }
    Collections.sort(lEquivalenceSignatures, Collections.reverseOrder());
    sb.append("[");
    Joiner.on("][").appendTo(sb, lEquivalenceSignatures);
    sb.append("]");
    return sb;
  }

  StringBuilder appendSignatureToStringBuilder(StringBuilder sb, 
                                               List<Double> lNumberDoubles){
    //first calc the equivalence sets
    if(lEquivalenceSets.size() == 0){
      sb.append("NOEQUIV");
      return sb;
    }
    List<String> lEquivalenceSignatures = new ArrayList<String>();
    for(List<Integer> lEquivalenceSet : lEquivalenceSets){
      List<String> lCurSigs = new ArrayList<String>();
      for(Integer iSlot : lEquivalenceSet){
        lCurSigs.add(getConcreteSlotSignature(iSlot, lNumberDoubles));
      }
      //now sort them
      Collections.sort(lCurSigs, Collections.reverseOrder());
      lEquivalenceSignatures.add(Joiner.on(",").join(lCurSigs));
    }
    Collections.sort(lEquivalenceSignatures, Collections.reverseOrder());
    sb.append("[");
    Joiner.on("][").appendTo(sb, lEquivalenceSignatures);
    sb.append("]");
    return sb;
  }

  <T> List<T> remap(List<T> lOrig){
    Misc.Assert(lOrig.size() == this.iNumUnique);
    List<T> lNew = new ArrayList<T>(aMap.length);
    for(int iCur : aMap){
      lNew.add(lOrig.get(iCur));
    }
    return lNew;
  }

}
