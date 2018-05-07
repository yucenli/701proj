import java.lang.*;
import java.util.*;

class Derivation{
  EquationSystem system;
  List<StanfordWord> lUnknowns;
  List<StanfordWord> lNumbers;

  Derivation(EquationSystem system, List<StanfordWord> lUnknowns,
             List<StanfordWord> lNumbers){
    // I need to possible resort the derivation if the system has
    // equivalent equations
    Misc.Assert(!system.bHasEquivalentEquations);
    this.system = system;
    this.lUnknowns = lUnknowns;
    this.lNumbers = lNumbers;
  }

  boolean equalsConcrete(ConcreteSystem system){
    //Misc.Assert(system.lQuery == null); // we need to add this to the derivation
    if(!this.system.equals(system.system)){
      return false;
    }
    //now go through the words and make sure the numbers are the same
    Misc.Assert(this.lNumbers.size() == system.lNumbers.size());
    for(int iNumber = 0; iNumber < lNumbers.size(); iNumber++){
      double fNumber = this.lNumbers.get(iNumber).number.fNumber;
      if(!Misc.isCloseTo(fNumber, system.lNumbers.get(iNumber))){
        return false;
      }
    }
    //eventually add something about query variables
    return true;
  }
}