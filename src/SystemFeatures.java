import java.lang.*;
import java.util.*;

class SystemFeatures{
  Features features;
  Set<Object> setTrainFeatures;


  SystemFeatures(Features features){
    this.features = features;
  }

  void calcTrainFeatures(List<Question> lTrain){
    Misc.Assert(false);
    // setTrainFeatures = new HashSet<Object>();
    // Set<EquationSystem> setSystems = new HashSet<EquationSystem>();
    // for(Question question : Model.lTrainQuestions){
    //   setSystems.add(question.system.system);
    // }
    // int iQuestion = 0;
    // for(Question question : Model.lTrainQuestions){
    //   System.out.println("Building Question: " + iQuestion + " out of: " +
    //                      Model.lTrainQuestions.size());
    //   iQuestion++;
    //   for(EquationSystem system : setSystems){
    //     for(String sEquationFeature : system.getEquationletFeatures()){
    //       for(Object oNGram : question.lFeatures){
    //         setTrainFeatures.add(Arrays.asList(oNGram,sEquationFeature));
    //       }
    //     }
    //   }
    // }
    // for(Question question : Model.lTrainQuestions){
    //   List<String> lFeatures = question.system.system.getEquationletFeatures();
    //   //now build all cross features
    //   for(String sEquationFeature : lFeatures){
    //     for(Object oNGram : question.lFeatures){
    //       setTrainFeatures.add(Arrays.asList(oNGram,sEquationFeature));
    //     }
    //   }
    // }
  }


  List<Object> getNumberFeatures(Question question, EquationSystem system){
    List<Object> lFeatures = new ArrayList<Object>();
    int iDiffNumNumbers = question.doc.lNumbers.size()-system.iNumNumbers;
    if(iDiffNumNumbers > 0){
      lFeatures.add(Arrays.asList("POS-DIFF-NUM-NUMBERS", iDiffNumNumbers));
    } else if(iDiffNumNumbers < 0){
      lFeatures.add(Arrays.asList("NEG-DIFF-NUM-NUMBERS", -iDiffNumNumbers));
    } else {
      lFeatures.add("NO-DIFF-NUM-NUMBERS");
    }
    int iAbsDiff = Math.abs(iDiffNumNumbers);
    lFeatures.add(Arrays.asList("ABS-DIFF-NUM-NUMBERS", iAbsDiff));
    return lFeatures;
  }

  FeatureSet getFeatureSet(Question question, EquationSystem system){
    FeatureSet fs = new FeatureSet(false);
    //String sSystem = system.toFeatureString();
    // add the question level features
    //double fNGramValue = 2.0/((double)question.lFeatures.size());
    //double fNGramValue = 1.0;

    Misc.Assert(false);
    // for(String sEquationlet : system.getEquationletFeatures()){
    //   for(Object oNGram : question.lFeatures){
    //     Object oFeature = Arrays.asList(oNGram, sEquationlet);
    //     if(setTrainFeatures.contains(oFeature)){
    //       features.addFeature(fs, oFeature);
    //     }
    //   }
    // }
    
    //List<Object> lNumberFeatures = getNumberFeatures(question, system);
    //features.addFeatures(fs, lNumberFeatures);
    //features.addFeature(fs, sSystem);
    // let's start with just the correct feature
    //if(question.system.system.equals(system)){
    //  features.addFeature(fs, "CORRECT");
    //}
    //features.addFeature(fs, sSystem, question.iIndex);
    return fs;
  }
}