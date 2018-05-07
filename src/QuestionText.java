import java.lang.*;
import java.util.*;
import java.io.*;

import org.apache.commons.io.FileUtils;
import com.google.gson.reflect.*;
import com.google.gson.*;

class QuestionText{
  int iIndex;
  Integer iDuplicate;
  String sSkipReason;
  String sType;
  String sHardness;
  Boolean bNonlinear;
  Boolean bOrderUnclear;
  List<Double> lAllowedConstants;
  String sQuestion;
  List<String> lEquations;
  List<String> lUnknowns;
  List<String> lQueryVars;
  List<Double> lSolutions;
  Boolean bCorrect; // used for writing out json files which indicate whether
  // it's correct or not
  QuestionText(){};
  QuestionText(String sQuestion, List<Double> lSolutions, 
               List<String> lEquations, List<String> lQueryVars,
               QuestionConfig config, int iIndex){
    this.iIndex = iIndex;
    this.sQuestion = sQuestion;
    this.lSolutions = lSolutions;
    if(lEquations.size() != 0){
      this.lEquations = lEquations;
    }
    this.lQueryVars = lQueryVars;
    //load the config
    Misc.Assert(config.sReject == null);
    Misc.Assert(config.sFullReject == null);
    Misc.Assert(config.sBad == null);
    this.sSkipReason = config.sSkipReason;
    if(config.lAllowedConstants != null){
      this.lAllowedConstants = config.lAllowedConstants;
    }
    if(config.bNonlinear){
      this.bNonlinear = config.bNonlinear;
    }
    if(config.bOrderUnclear){
      this.bOrderUnclear = config.bOrderUnclear;
    }
  }

  boolean isValid(){
    return (sSkipReason == null) && ((bNonlinear == null) || !bNonlinear) &&
      (iDuplicate == null);
  }

  static List<QuestionText> readListFromJson(String sFilename){
    String sFileContents = null;
    try{
      sFileContents = 
        FileUtils.readFileToString(new File(sFilename));
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }
    List<QuestionText> lQuestions = 
      new Gson().fromJson(sFileContents,
                          new TypeToken<List<QuestionText>>(){}.getType());
    return lQuestions;
  }

  static void writeListToJson(List<QuestionText> lQuestions, String sFilename){
    Gson gson = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();
    String sJson = gson.toJson(lQuestions);
    try{
      FileUtils.writeStringToFile(new File(sFilename), sJson);
    }catch(IOException ex){
      throw new RuntimeException(ex);
    }
  }


}