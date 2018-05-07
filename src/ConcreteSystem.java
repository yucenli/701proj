import java.lang.*;
import java.util.*;

class ConcreteSystem{
  EquationSystem system;
  List<String> lUnknowns;
  List<Double> lNumbers;
  List<Integer> lQuery; // indexes into lUnknowns
  List<Equation> lSingleEquations;
  List<ConcreteEquation> lConcreteEquations;
  Set<String> setConcreteStrings;
  Set<String> setEquationStrings;


  ConcreteSystem(List<Equation> lEquations, List<String> lUnknowns,
                 List<Double> lNumbers){
    this.system = new EquationSystem(lEquations, lUnknowns.size(), 
                                     lNumbers.size());
    this.lUnknowns = lUnknowns;
    this.lNumbers = lNumbers;
    this.lQuery = null;
    if(Config.config.bNormalizeEquations){
      normalize();
    }
  }

  void initializeExtras(){
    this.lSingleEquations = new ArrayList<Equation>();
    this.lConcreteEquations = new ArrayList<ConcreteEquation>();
    this.setConcreteStrings = new HashSet<String>();
    this.setEquationStrings = new HashSet<String>();
    for(int iEquation = 0; iEquation < system.lEquations.size(); iEquation++){
      ConcreteEquation concrete = new ConcreteEquation(this, iEquation);
      lConcreteEquations.add(concrete);
      setConcreteStrings.add(concrete.toString());
      lSingleEquations.add(concrete.equation);
      setEquationStrings.add(concrete.equation.toExactString());
    }
  }


  List<Double> getQuery(List<Double> lUnknowns){
    List<Double> lQuery = new ArrayList<Double>();
    for(Integer iQuery : this.lQuery){
      lQuery.add(lUnknowns.get(iQuery));
    }
    return lQuery;
  }

  void normalize(){
    //first try to normalize a bit by structurally sorting the equations
    List<String> lNewUnknowns = new ArrayList<String>(lUnknowns.size());
    List<Double> lNewNumbers = new ArrayList<Double>(lNumbers.size());
    system.normalize(lUnknowns, lNewUnknowns, lNumbers, lNewNumbers);
    renumberQuery(lUnknowns, lNewUnknowns);
    this.lUnknowns = lNewUnknowns;
    this.lNumbers = lNewNumbers;
  }

  void renumberQuery(List<String> lUnknowns, List<String> lNewUnknowns){
    if(lQuery == null){
      return;
    }
    List<Integer> lNewQuery = new ArrayList<Integer>();
    for(Integer iQuery : lQuery){
      String sUnknown = lUnknowns.get(iQuery);
      //find it in the new unknown list
      for(int iUnknown = 0; iUnknown < lNewUnknowns.size(); iUnknown++){
        if(sUnknown.equals(lNewUnknowns.get(iUnknown))){
          lNewQuery.add(iUnknown);
          break;
        }
      }
    }
    Misc.Assert(lNewQuery.size() == this.lQuery.size());
    this.lQuery = lNewQuery;
  }


  boolean isNonlinear(){
    return system.isNonlinear(lNumbers, lUnknowns.size());
  }

  void engrainConstants(List<Double> lTextNumbers){
    List<Double> lNewNumbers = new ArrayList<Double>();
    List<Integer> lPointers = new ArrayList<Integer>();
    for(Double fNumber : lNumbers){
      if(lTextNumbers.contains(fNumber)){
        lPointers.add(lNewNumbers.size());
        lNewNumbers.add(fNumber);
      } else {
        lPointers.add(null);
      }
    }
    system.engrainConstants(lNumbers, lPointers, lNewNumbers.size());
    this.lNumbers = lNewNumbers;
    if(Config.config.bNormalizeEquations){
      normalize();
    }
    initializeExtras();
  }


  void setQuery(List<String> lQueryStrings){
    Misc.Assert(lQuery == null);
    this.lQuery = new ArrayList<Integer>();
    for(String sQuery : lQueryStrings){
      //find it in the list
      boolean bFound = false;
      for(int iUnknown = 0; iUnknown < lUnknowns.size(); iUnknown++){
        if(sQuery.equals(lUnknowns.get(iUnknown))){
          this.lQuery.add(iUnknown);
          bFound = true;
          break;
        }
      }
      if(!bFound){
        System.out.println("Couldn't find:" + sQuery + ":");
        Misc.Assert(bFound);
      }
    }
  }

  public String toString(){
    return toStringBuilder(new StringBuilder()).toString();
  }
  
  StringBuilder toStringBuilder(StringBuilder sb){
    return system.toStringBuilder(sb, lUnknowns, lNumbers, null);
  }
  

}