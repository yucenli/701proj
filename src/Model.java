import java.lang.*;
import java.util.*;

class Model{
  static Config config;

  static LogLinear loglinear;
  static Features features;
  static List<Question> lTrainQuestions;
  static List<Question> lTestQuestions;
  static List<Question> lAllQuestions;
  static List<Integer> lHandAnnotations;
  static Set<String> setHandSignatures;
  static List<Integer> lCorrectSystems;
  static int iTestFold = -1;

  static void load(int iTestFold){
    load(null, iTestFold, false);
  }

  static void load(boolean bSuppressPrint){
    Misc.Assert(!Config.config.bUseFoldData);
    load(null, -1, bSuppressPrint);
  }

  static void load(){
    Misc.Assert(!Config.config.bUseFoldData);
    load(null, -1, false);
  }
  static void load(String sConfigFile){
    Misc.Assert(!Config.config.bUseFoldData);
    load(sConfigFile, -1, false);
  }

  static void loadSignatures(){
    setHandSignatures = new HashSet<String>();
    if(!Config.config.bLoadHandSignatures){
      return;
    }
    if(!Config.config.bTestOneQuestion){
      for(Integer iQuestion : lHandAnnotations){
        Question question = Question.getQuestion(iQuestion, lAllQuestions);
        Misc.Assert(question != null);
        setHandSignatures.add(question.ct.mt.toString(false,true));
      }
    }
  }

  static void load(String sConfigFile, int iTestFold, boolean bSuppressPrint){
    Config.load(sConfigFile, bSuppressPrint);
    Model.iTestFold = iTestFold;
    loglinear = new LogLinear();
    features = new Features();
    loglinear.features = features;
    features.loglinear = loglinear;

    lTrainQuestions = Question.load(true, iTestFold);
    lTestQuestions = Question.load(false, iTestFold);
    lAllQuestions = new ArrayList<Question>();
    lAllQuestions.addAll(lTrainQuestions);
    lAllQuestions.addAll(lTestQuestions);
    lHandAnnotations = new ArrayList<Integer>();
    if(Config.config.bUseFullHandAnnotations){
      int iNumHand = 
        (lTrainQuestions.size()*Config.config.iPercentageEquationAnnotations)/100;
      for(int i = 0; i < iNumHand; i++){
        lHandAnnotations.add(lTrainQuestions.get(i).iIndex);
      } 
    }

    if(Config.config.bLoadCorrectSystems){
      lCorrectSystems = Misc.loadIntList("correctsystems-" + iTestFold +".txt");
    } else {
      lCorrectSystems = new ArrayList<Integer>();
    }

    loadSignatures();
  }
}
