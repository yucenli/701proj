import java.lang.*;
import java.util.*;

class QuestionConfig{
  String sSkipReason = null;
  String sReject = null;
  String sFullReject = null;
  String sBad = null;
  boolean bNonlinear = false;
  boolean bOrderUnclear = false;
  String sAllowedConstants = null;
  List<Double> lAllowedConstants = null;
  Integer iDuplicate = null;
  String sType = null;
  String sHardness = null;

  QuestionConfig(){}

  QuestionConfig(QuestionText question){
    this.sSkipReason = question.sSkipReason;
    this.sType = question.sType;
    this.sHardness = question.sHardness;
    if(question.bNonlinear != null){
      this.bNonlinear = question.bNonlinear;
    }
    if(question.bOrderUnclear != null){
      this.bOrderUnclear = question.bOrderUnclear;
    }
    if(question.lAllowedConstants != null){
      this.lAllowedConstants = question.lAllowedConstants;
    }
    this.iDuplicate = question.iDuplicate;
  }


  boolean isGood(){
    return (sSkipReason == null) && (sReject == null) && (sBad == null) &&
      (sFullReject == null);
  }

  boolean isValid(){
    return isGood() && !bNonlinear && (iDuplicate == null);
  }


  int read(List<String> lLines, int iCurLine){
    while(readOne(lLines.get(iCurLine))){
      iCurLine++;
    }
    return iCurLine;
  }

  boolean readOne(String sLine){
    if(sLine.startsWith("skip")){
      sSkipReason = sLine;
      return true;
    } else if(sLine.equals("orderunclear")){
      bOrderUnclear = true;
      return true;
    } else if(sLine.equals("nonlinear")){
      bNonlinear = true;
      return true;
    } else if(sLine.startsWith("reject")){
      sReject = sLine;
      return true;
    } else if(sLine.startsWith("fullreject")){
      sFullReject = sLine;
      return true;
    } else if(sLine.startsWith("constants")){
      sAllowedConstants = sLine;
      sLine = sLine.substring(10);
      this.lAllowedConstants = new ArrayList<Double>();
      for(String sConstant : Arrays.asList(sLine.split(" "))){
        Double fNum = Misc.getNumber(sConstant);
        if(fNum == null){
          System.out.println("BadNum:" + sConstant + ":");
          Misc.Assert(fNum != null);
        }
        lAllowedConstants.add(fNum);
      }
      return true;
    } 
    return false;
  }

  StringBuilder toStringBuilder(StringBuilder sb){
    if(sSkipReason != null){
      sb.append(sSkipReason).append("\n");
    }
    if(bOrderUnclear){
      sb.append("orderunclear").append("\n");
    } 
    if(bNonlinear){
      sb.append("nonlinear").append("\n");
    } 
    if(sAllowedConstants != null){
      sb.append(sAllowedConstants).append("\n");
    } 
    return sb;
  }

}