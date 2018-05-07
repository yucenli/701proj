import java.lang.*;
import java.util.*;

class ConcreteEqu{
  List<OldConcreteTerm> lTerms = new ArrayList<OldConcreteTerm>();
  List<List<OldConcreteTerm>> lEqualitySets = new ArrayList<List<OldConcreteTerm>>();

  ConcreteEqu(ConcreteEquation equation) throws BadEquationException{
    //we'll make a list of concrete terms for each unknown
    for(int iUnknown = 0; iUnknown < equation.equation.iNumUnknowns;iUnknown++){
      lEqualitySets.add(new ArrayList<OldConcreteTerm>());
    }
    //then we'll add all the terms
    addTerms(equation.equation.termLeft, equation.lNumbers, false);
    addTerms(equation.equation.termRight, equation.lNumbers, true);
  }

  int numSlots(){
    int iNumSlots = 0;
    for(OldConcreteTerm term : lTerms){
      iNumSlots += term.numSlots();
    }
    return iNumSlots;
  }

  class BadEquationException extends Exception{
    BadEquationException(){
      super();
    }
    BadEquationException(String sMesg){
      super(sMesg);
    }
  }


  void addTerms(EquationTerm equationterm, List<Double> lNumbers,
                       boolean bRight) throws BadEquationException{
    if(equationterm instanceof EquationTerm.Complex){
      EquationTerm.Complex complex = (EquationTerm.Complex) equationterm;
      if(complex.op.equals(Operator.PLUS)){
        //addterms for each of them
        addTerms(complex.termLeft, lNumbers, bRight);
        addTerms(complex.termRight, lNumbers, bRight);
      } else if(complex.op.equals(Operator.MINUS)){
        addTerms(complex.termLeft, lNumbers, bRight);
        addNewTerm(!bRight, complex.termRight, lNumbers);
      } else {
        // if it's multiple or divide then we just have a single term
        addNewTerm(bRight, equationterm, lNumbers);
      }
    } else {
      //add a new term for this
      addNewTerm(bRight, equationterm, lNumbers);
    }
  }

  boolean isZero(EquationTerm equationterm, List<Double> lNumbers){
    if(equationterm instanceof EquationTerm.Number){
      Double fNumber = lNumbers.get(((EquationTerm.Number)equationterm).iIndex);
      return fNumber.equals(0.0);
    } else if(equationterm instanceof EquationTerm.Constant){
      return (((EquationTerm.Constant)equationterm).fConstant == 0.0);
    } else {
      return false;
    }
  }
  void addNewTerm(boolean bMinus, EquationTerm equationterm, 
                  List<Double> lNumbers) throws BadEquationException{
    //first check if the term is zero
    if(isZero(equationterm, lNumbers)){
      //don't add it if it's zero
      return;
    }
    OldConcreteTerm concreteterm = new OldConcreteTerm(bMinus);
    lTerms.add(concreteterm);
    addToTerm(equationterm, lNumbers, concreteterm);
  }

  void addToTerm(EquationTerm equationterm, List<Double> lNumbers, 
                        OldConcreteTerm concreteterm) throws BadEquationException{
    if(equationterm instanceof EquationTerm.Unknown){
      if(concreteterm.bUnknown){
        throw new BadEquationException("MultipleUnknowns");
      }
      int iUnknown = ((EquationTerm.Unknown) equationterm).iIndex;
      //add this concreteterm to the equalityset for this unknown
      lEqualitySets.get(iUnknown).add(concreteterm);
      concreteterm.bUnknown = true;
    } else if(equationterm instanceof EquationTerm.Number){
      Double fNumber =lNumbers.get(((EquationTerm.Number) equationterm).iIndex);
      concreteterm.addFactor(fNumber);
    } else if(equationterm instanceof EquationTerm.Constant){
      Double fNumber = ((EquationTerm.Constant) equationterm).fConstant;
      concreteterm.addFactor(fNumber);
    } else if(equationterm instanceof EquationTerm.Complex){
      EquationTerm.Complex complex = (EquationTerm.Complex) equationterm;
      if(complex.op.equals(Operator.TIMES)){
        addToTerm(complex.termLeft, lNumbers, concreteterm);
        addToTerm(complex.termRight, lNumbers, concreteterm);
      } else if(complex.op.equals(Operator.DIVIDEBY)){
        addToTerm(complex.termLeft, lNumbers, concreteterm);
        Double fNumber = null;
        if(complex.termRight instanceof EquationTerm.Number){
           fNumber = 
             lNumbers.get(((EquationTerm.Number) complex.termRight).iIndex);
        } else if(complex.termRight instanceof EquationTerm.Constant){
          fNumber = ((EquationTerm.Constant) complex.termRight).fConstant;
        } else {
          // can't handle other cases
          throw new BadEquationException("BadInnerTerm: DivideBy: " 
                                         + complex.termRight);
        }
        concreteterm.fDenominator = fNumber;
      } else {  
        //can't add or substract inside of a single term
        throw new BadEquationException("BadInnerTerm: MulitplyBy: "
                                       + equationterm);
      }
    } else {
      //there's no other EquationTerm types (at least right now)
      Misc.Assert(false);
    }
  }

  public String toString(){
    return toStringBuilder(new StringBuilder()).toString();
  }

  public StringBuilder toStringBuilder(StringBuilder sb){
    for(OldConcreteTerm term : lTerms){
      term.toStringBuilder(sb);
    }
    sb.append(" = 0");
    return sb;
  }

}
