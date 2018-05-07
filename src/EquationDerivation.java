import java.lang.*;
import java.util.*;

class EquationDerivation {
  Equation equation;
  List<StanfordWord> lNumbers;
  List<StanfordWord> lUnknowns;
  boolean bCorrect;
  String sConcrete;
  FeatureListList fll;

  EquationDerivation(Equation equation, List<StanfordWord> lUnknowns, 
                     List<StanfordWord> lNumbers,
                     Set<String> setCorrectConcrete){
    Misc.Assert(lUnknowns.size() == equation.iNumUnknowns);
    Misc.Assert(lNumbers.size() == equation.iNumNumbers);
    this.equation = equation;
    this.lNumbers = lNumbers;
    this.lUnknowns = lUnknowns;
    this.sConcrete = makeConcreteString();
    this.bCorrect = setCorrectConcrete.contains(sConcrete);
    fll = Model.features.derivationfeatures.getFeatures(this);
  }
    
  private String makeConcreteString(){
    List<String> lUnknowns = Misc.listOfLetters('m', equation.iNumUnknowns);
    List<Double> lDoubles = StanfordNumber.wordsToDoubles(lNumbers);
    return equation.toString(lUnknowns, lDoubles);
  }

  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append(sConcrete).append("\n    ");
    StanfordWord.toStringBuilderWithIndexes(sb, lUnknowns);
    sb.append("\n    ");
    StanfordWord.toStringBuilderWithIndexes(sb, lNumbers);
    return sb.toString();
  }
}
