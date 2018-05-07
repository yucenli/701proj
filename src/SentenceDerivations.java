import java.lang.*;
import java.util.*;

class SentenceDerivations{
  StanfordSentence sentence;
  Set<String> setCorrectConcrete;
  List<EquationDerivation> lDers = new ArrayList<EquationDerivation>();
  boolean bFoundCorrect = false;

  SentenceDerivations(StanfordSentence sentence,Set<String> setCorrectConcrete){
    this.sentence = sentence;
    this.setCorrectConcrete = setCorrectConcrete;
  }

  void buildEquationDerivations(List<Equation> lEquations){
    for(Equation equation : lEquations){
      UnknownChooser chooser = new UnknownChooser(equation);
      chooser.chooseAll();
    }
    Misc.Assert(bFoundCorrect);
  }


  void add(EquationDerivation der){
    lDers.add(der);
    bFoundCorrect |= der.bCorrect;
  }

  class NumberChooser extends Choosable<StanfordWord>{
    List<StanfordWord> lUnknowns;
    Equation equation;
    NumberChooser(List<StanfordWord> lUnknowns, Equation equation){
      super(sentence.lNumberWords, equation.iNumNumbers);
      this.lUnknowns = lUnknowns;
      this.equation = equation;
    }

    void choose(List<StanfordWord> lNumbers){
      add(new EquationDerivation(this.equation, this.lUnknowns, lNumbers,
                                 setCorrectConcrete));
    }
  }

  List<StanfordWord> getNounList(int iNumUnknowns){
    if(sentence.lNouns.size() >= iNumUnknowns){
      return sentence.lNouns;
    } else {
      //make a new arraylist
      List<StanfordWord> lNouns = new ArrayList<StanfordWord>(sentence.lNouns);
      for(StanfordWord word : sentence.lWords){
        if(word.isRootParent() || lNouns.contains(word)){
          continue;
        }
        lNouns.add(word);
        if(lNouns.size() >= iNumUnknowns){
          break;
        }
      }
      return lNouns;
    }
  }

                                                    
  class UnknownChooser extends Choosable<StanfordWord>{
    Equation equation;
    UnknownChooser(Equation equation){
      super(getNounList(equation.iNumUnknowns), equation.iNumUnknowns);
      this.equation = equation;
    }
    void choose(List<StanfordWord> lUnknowns){
      NumberChooser chooser = new NumberChooser(lUnknowns, equation);
      chooser.chooseAll();
    }
  }




}
