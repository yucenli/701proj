import java.lang.*;
import java.util.*;
import java.io.*;
import java.text.*;

import org.apache.commons.io.FileUtils;
import edu.stanford.nlp.process.*;
import edu.stanford.nlp.ling.Word;

class FullQuestion implements Comparable<FullQuestion>{
  enum Type {AmazonResults1, AmazonResultsSolutions, SolutionDownload, SolutionRequest,Canonical}
  Type type;
  Integer iMyId;//0
  Integer iOrigId;//1
  String sQuestionOrig;//2
  boolean bBad;//3
  String sQuestionCleaned;//4
  String sSolution;//5
  List<Double> lSolutions;//6,7,8
  String sXml;//9
  int iNumInstances = 1;

  FullQuestion(String[] aQuestion) throws IOException{
    this.iMyId = new Integer(aQuestion[0]);
    this.iOrigId = new Integer(aQuestion[1]);
    this.sQuestionOrig = aQuestion[2];
    this.bBad = (aQuestion[3].equals("true"));
    this.sQuestionCleaned = aQuestion[4];
    this.sSolution = aQuestion[5];
    this.lSolutions = new ArrayList<Double>();
    // get the solutions
    try {
      NumberFormat nf = NumberFormat.getNumberInstance();
      if(!aQuestion[6].equals("")){
        this.lSolutions.add(nf.parse(aQuestion[6]).doubleValue());
        if(!aQuestion[7].equals("")){
          this.lSolutions.add(nf.parse(aQuestion[7]).doubleValue());
          if(!aQuestion[8].equals("")){
            this.lSolutions.add(nf.parse(aQuestion[8]).doubleValue());
          }
        } else {
          Misc.Assert(aQuestion[8].equals(""));
        }
        //}
      } else {
        Misc.Assert(this.bBad);
      }
    } catch(java.text.ParseException ex){
      throw new RuntimeException(ex);
    }
    if(aQuestion.length > 9 && (!aQuestion[9].equals(""))){
      System.out.println("BadQuestion: " + this.iMyId + " len: " 
                         + aQuestion.length);
    }
    Misc.Assert(aQuestion.length <= 9);
  } 

  public int compareTo(FullQuestion question){
    return this.iMyId - question.iMyId;
  }

  static Double parseDouble(String sDouble) throws java.text.ParseException{
    NumberFormat nf = NumberFormat.getNumberInstance();
    if(sDouble.startsWith("$")){
      sDouble = sDouble.substring(1,sDouble.length());
    }
    Double doub = nf.parse(sDouble).doubleValue();
    return doub;
  }

  static List<FullQuestion> readCsvFile(String sFileIn) 
    throws IOException{
    System.out.println("Reading Csv File");
    List<String[]> lQuestionsIn = Misc.readCsvFile(sFileIn);
    System.out.println("Done Reading Csv File");
    List<FullQuestion> lQuestionsOut = new ArrayList<FullQuestion>();
    //if it contains a header then skip it
    if(lQuestionsIn.get(0)[0].equals("HITId")){
      lQuestionsIn = lQuestionsIn.subList(1, lQuestionsIn.size());
    }
    for(String[] aQuestion : lQuestionsIn){
      lQuestionsOut.add(new FullQuestion(aQuestion));
    }
    return lQuestionsOut;
  }

  // Question toSimpleQuestion(){
  //   return new Question(this.sQuestionCleaned, this.lSolutions,null,
  //                       null, this.iMyId);
  // }

  // static List<Question> loadSimpleQuestions(){
  //   try{
  //     List<FullQuestion> lQuestions = 
  //       readCsvFile(Config.config.sQuestionCvsFile);
  //     List<Question> lSimpleQuestions = new ArrayList<Question>();
  //     for(FullQuestion question : lQuestions){
  //       lSimpleQuestions.add(question.toSimpleQuestion());
  //     }
  //     return lSimpleQuestions;
  //   } catch (IOException ex){
  //     throw new RuntimeException(ex);
  //   }
  // }
}
