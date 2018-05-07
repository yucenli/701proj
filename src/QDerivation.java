import java.lang.*;
import java.util.*;
import java.util.concurrent.*;

class QDerivation{
  Question question;
  List<QEDerivation> lQEDerivations = new ArrayList<QEDerivation>();
  QDerivation(Question question, Set<Equation> setEquations){
    this.question = question;
    for(Equation equation : setEquations){
      lQEDerivations.add(new QEDerivation(question, equation));
    }
  }

  public void addCallables(List<Callable<Object>> lCallables){
    for(QEDerivation qed : lQEDerivations){
      qed.addCallables(lCallables);
    }
  }


  void reset(){
    for(QEDerivation qed : lQEDerivations){
      qed.reset();
    }
  }

  FeatureCounts calcFeatureCounts(){
    FeatureCounts featurecounts = 
      FeatureCounts.makeEquationDerivationFeatureCounts(question);
    for(QEDerivation qed : lQEDerivations){
      featurecounts.integrate(qed.featurecounts);
      //so we fail fast if we don't do a reset
      qed.featurecounts = null;
    }
    featurecounts.finish();
    return featurecounts;
  }

}