import java.lang.*;
import java.util.*;

class DebugPrinter{

  static List<StanfordWord> convertToWords(List<String> lStrings,
                                           StanfordDocument doc){
    List<StanfordWord> lWords = new ArrayList<StanfordWord>();
    for(String sCur : lStrings){
      lWords.add(Misc.getWord(sCur, doc));
    }
    return lWords;
  }

  static FilledTerm buildFilledTerm(List<String> lUnknowns,
                                    List<String> lNumbers,
                                    Question question,
                                    String sSystem){
    MappedTerm mt = null;
    Map<String,MappedTerm> mSystems = new HashMap<String,MappedTerm>();
    for(Question questionCur : Model.lAllQuestions){
      if(questionCur.ct.mt.toString(false,true).equals(sSystem)){
        mt = questionCur.ct.mt;
      }
    }
    Misc.Assert(mt != null);
    List<StanfordWord> lUnknownWords = convertToWords(lUnknowns, question.doc);
    List<StanfordWord> lNumberWords = convertToWords(lNumbers, question.doc);
    FilledTerm ft = new FilledTerm(mt);
    ft.lUnknownWords = lUnknownWords;
    ft.lNumberWords = lNumberWords;
    return ft;
  }



  static void print(int iQuestion, List<String> lUnknowns, 
                    List<String> lNumbers, String sSystem){
    Question question = null;
    for(Question questionCur : Model.lAllQuestions){
      if(questionCur.iIndex == iQuestion){
        question = questionCur;
        break;
      }
    }
    Misc.Assert(question != null);
    FilledTerm ft = buildFilledTerm(lUnknowns, lNumbers, question, sSystem);
    FeatureListList fll = 
      Model.features.derivationfeatures.getFeatures(ft, question, 
                                                    new FeatureCache(),
                                                    Question.buildSolvers());
    System.out.println(fll.toString());
  }
  
  static void printTest1(){
    print(5761, Arrays.asList("cartons:1:22", "number:1:20"),
          Arrays.asList("3:0:10", "6:0:3", "100:1:10", "360:1:13"), 
          "(a*0.01*m)+(b*0.01*n)+(-c*d*0.01)::-d+m+n::");
  }


  public static void main(String[] args) throws Exception{
    Model.load();
    Model.features.derivationfeatures
      .computeCommonFeatures(Model.lTrainQuestions);
    printTest1();
  }  

}