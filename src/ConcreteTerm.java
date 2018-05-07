import java.lang.*;
import java.util.*;

class ConcreteTerm{
  MappedTerm mt;
  List<String> lUnknownStrings;
  List<Double> lNumberDoubles;
  Integer iNumUnknowns;
  String sSignature = null;
  
  ConcreteTerm(ConcreteSystem system){
    if(Config.config.bNormalizeEquations){
      mt = findCanonicalMappedTerm(system.system);
    } else {
      mt = new MappedTerm(system.system, 
                          Misc.genFullList(system.system.lEquations.size(), 
                                           Boolean.FALSE));
    }
    mt.buildTermLists();
    iNumUnknowns = mt.mapUnknowns.iNumUnique;
    lUnknownStrings = remapInputs(system.lUnknowns, mt.mapUnknowns);
    lNumberDoubles = remapInputs(system.lNumbers, mt.mapNumbers);
  }

  // List<Double> remapNumbers(List<Double> lInput){
  //   Misc.Assert(lInput.size() == mt.mapNumbers.iNumUnique);
  //   List<Double> lOutput = Misc.genEmptyList(lInput.size());
  //   for(int iInstance = 0; iInstance < mt.mapNumbers.numInstances();iInstance++){
  //     int iIndex = mt.mapNumbers.lIndexes.get(iInstance);
  //     int iUnique = mt.mapNumbers.aMap[iInstance];
  //     double fNew = lInput.get(iIndex);
  //     Double fCur = lOutput.get(iUnique);
  //     if(fCur != null){
  //       Misc.Assert(fCur == fNew);
  //     } else {
  //       lOutput.set(iUnique, fNew);
  //     }
  //   }
  //   mt.mapNumbers.lIndexes = null;
  //   return lOutput;
  // }

  <T> List<T> remapInputs(List<T> lInput, TermMap tm){
    Misc.Assert(lInput.size() == tm.iNumUnique);
    List<T> lOutput = Misc.genEmptyList(lInput.size());
    for(int iInstance = 0; iInstance < tm.numInstances();iInstance++){
      int iIndex = tm.lIndexes.get(iInstance);
      int iUnique = tm.aMap[iInstance];
      T tNew = lInput.get(iIndex);
      T tCur = lOutput.get(iUnique);
      if(tCur != null){
        Misc.Assert(tCur.equals(tNew));
      } else {
        lOutput.set(iUnique, tNew);
      }
    }
    tm.lIndexes = null;
    return lOutput;
  }

  List<List<Boolean>> genAllBooleanLists(int iSize){
    List<List<Boolean>> llBools = new ArrayList<List<Boolean>>();
    if(iSize == 1){
      llBools.add(new ArrayList<Boolean>(Arrays.asList(false)));
      llBools.add(new ArrayList<Boolean>(Arrays.asList(true)));
      return llBools;
    }

    for(List<Boolean> lBools : genAllBooleanLists(iSize-1)){
      List<Boolean> lFalse = new ArrayList<Boolean>(lBools);
      List<Boolean> lTrue = lBools;
      lFalse.add(false);
      lTrue.add(true);
      llBools.add(lFalse);
      llBools.add(lTrue);
    }
    return llBools;
  }

  MappedTerm findCanonicalMappedTerm(EquationSystem system){
    List<List<Boolean>> llMinus = genAllBooleanLists(system.lEquations.size());
    MappedTerm mtBest = null;
    for(List<Boolean> lMinus : llMinus){
      MappedTerm mtNew = new MappedTerm(system, lMinus);
      if((mtBest == null) ||
         (mtNew.recalcSignature().compareTo(mtBest.recalcSignature()) > 0)){
        mtBest = mtNew;
      }
    }
    return mtBest;
  }

  List<Double> solve(List<SimpleSolver> lSolvers){
    return mt.solve(lSolvers, lNumberDoubles);
  }


  String getSignature(){
    if(sSignature == null){
      if(Config.config.bNormalizeEquations){
        this.sSignature = mt.calcConcreteSignature(lNumberDoubles);
      } else {
        this.sSignature = mt.toString(false,true)+lNumberDoubles;
      }
    }
    return this.sSignature;
  }

  StringBuilder toStringBuilder(StringBuilder sb){
    List<String> lUnknowns = Misc.listOfLetters('m', iNumUnknowns);
    List<String> lNumbers = Misc.toListOfStrings(lNumberDoubles);
    return mt.toStringBuilder(sb, lUnknowns, lNumbers);
  }

  public String toDebugString(){
    List<String> lNumbers = Misc.toListOfStrings(lNumberDoubles);
    return mt.toStringBuilder(new StringBuilder(), lUnknownStrings, lNumbers)
      .toString();
  }

  public String toString(){
    return toStringBuilder(new StringBuilder()).toString();
  }

}