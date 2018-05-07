import java.lang.*;
import java.util.*;

class ConcreteEquation{
  Equation equation;
  List<Double> lNumbers;
  String sConcrete;

  ConcreteEquation(ConcreteSystem system, int iEquation){
    this.equation = new Equation(system.system.lEquations.get(iEquation));
    List<String> lUnknowns = new ArrayList<String>();
    this.lNumbers = new ArrayList<Double>();
    this.equation.renumber(system.lUnknowns, lUnknowns, system.lNumbers,
                           lNumbers);
    equation.iNumUnknowns = lUnknowns.size();
    equation.iNumNumbers = lNumbers.size();
  }
  public String toString(){
    if(sConcrete == null){
      List<String> lUnknowns = Misc.listOfLetters('m', equation.iNumUnknowns);
      sConcrete = equation.toString(lUnknowns, lNumbers);
    }
    return sConcrete;
  }
}