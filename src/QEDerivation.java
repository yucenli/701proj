import java.lang.*;
import java.util.*;
import java.util.concurrent.*;


class QEDerivation{
  //Question Equation Derivation
  static int iNumDone = 0;
  Question question;
  Equation equation;
  FeatureCounts featurecounts;

  QEDerivation(Question question, Equation equation){
    this.question = question;
    this.equation = equation;
    reset();
  }

  void reset(){
    this.featurecounts = 
      FeatureCounts.makeEquationDerivationFeatureCounts(question);
  }

  // synchronized void addSample(FeatureListList fll, boolean bCorrect){
  //     //featurecounts.addSample(fll, bCorrect);
  // }
  // public Object call(){
  //   try{
  //     UnknownChooser chooser = new UnknownChooser();
  //     chooser.chooseAll();
  //     incrementDone();
  //     if(iNumDone%100 == 0){
  //       System.out.println("Finished: " + iNumDone);
  //     }
  //     return null;
  //   }catch(Exception ex){
  //     System.out.println("Caught Exception in parallel execution");
  //     ex.printStackTrace();
  //     System.exit(-1);
  //     throw ex;
  //   }
  // }
  
  public void addCallables(List<Callable<Object>> lCallables){
    ParallelUnknownChooser chooser = new ParallelUnknownChooser(lCallables);
    chooser.chooseAll();
  }


  // static synchronized void incrementDone(){
  //   iNumDone++;
  // }

  synchronized void integrateFeatureCounts(FeatureCounts featurecounts){
    this.featurecounts.integrate(featurecounts);
  }

  // void add(EquationDerivation der){
  //   boolean bCorrect = 
  //     question.system.setConcreteStrings.contains(der.makeConcreteString());
  //   FeatureListList fll = Model.features.derivationfeatures.getFeatures(der);
  //   //this.addSample(fll, bCorrect);
  // }

  class NumberChooser extends Choosable<StanfordWord> 
    implements Callable<Object>{
    List<StanfordWord> lUnknowns;
    FeatureCounts featurecounts =
      FeatureCounts.makeEquationDerivationFeatureCounts(question);
    int iInitSize;
    int iInitNum;

    NumberChooser(List<StanfordWord> lUnknowns){
      super(question.doc.lNumberWords, equation.iNumNumbers);
      this.lUnknowns = lUnknowns;
      this.iInitSize = this.lUnknowns.size();
      this.iInitNum = equation.iNumUnknowns;
      Misc.Assert(this.lUnknowns.size() == equation.iNumUnknowns);
    }
    
    public Object call(){
      try{
        this.chooseAll();
        integrateFeatureCounts(this.featurecounts);
        this.featurecounts=null;
      }catch(Exception ex){
        System.out.println("Catch Exception in parallel execution");
        ex.printStackTrace();
        System.exit(-1);
        throw ex;
      }
      return null;
    }

    void choose(List<StanfordWord> lNumbers){
      if(this.lUnknowns.size() != equation.iNumUnknowns){
        System.out.println("InitNum: " + this.iInitNum + " InitSize: " + 
                           this.iInitSize + " CurNum: " + equation.iNumUnknowns
                           + " CurSize: " + this.lUnknowns.size());
        Misc.Assert(this.lUnknowns.size() == equation.iNumUnknowns);
      }
      EquationDerivation der = 
        new EquationDerivation(equation, this.lUnknowns, lNumbers,
                               question.system.setConcreteStrings);
      featurecounts.addSample(der.fll, der.bCorrect, der,
                              der.equation.toExactString());
    }
  }
  
  // class UnknownChooser extends Choosable<StanfordWord>{
  //   UnknownChooser(){
  //     super(question.doc.lNouns, equation.iNumUnknowns);
  //   }
  //   void choose(List<StanfordWord> lUnknowns){
  //     Misc.Assert(lUnknowns.size() == equation.iNumUnknowns);
  //     NumberChooser chooser = new NumberChooser(lUnknowns);
  //     chooser.chooseAll();
  //   }
  // }

  class ParallelUnknownChooser extends Choosable<StanfordWord>{
    List<Callable<Object>> lCallables;
    ParallelUnknownChooser(List<Callable<Object>> lCallables){
      super(question.doc.lNouns, equation.iNumUnknowns);
      this.lCallables = lCallables;
    }
    void choose(List<StanfordWord> lUnknowns){
      Misc.Assert(lUnknowns.size() == equation.iNumUnknowns);
      NumberChooser chooser = new NumberChooser(lUnknowns);
      lCallables.add(chooser);
    }
  }
}