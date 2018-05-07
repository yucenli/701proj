import java.lang.*;
import java.util.*;
import java.io.*;

import org.apache.commons.io.FileUtils;

class Amazon{
  static void read() throws IOException{
    List<Question> lQuestions =
      Question.loadFromFileNew("data/questions-all-equations.txt", false);
    Map<Integer,Question> mQuestions = new HashMap<Integer,Question>();
    for(Question question : lQuestions){
      mQuestions.put(question.iIndex, question);
    }
    Set<Integer> setBadSpelling = 
      new HashSet<Integer>(Arrays.asList(6489));
    String sBadSpelling = "You mistyped one of your unknowns";

    Set<Integer> setQueryNotEqualToUnknown =
      new HashSet<Integer>(Arrays.asList(5154,5835,6505,6675,6679,5406,6150,
                                         6755,6772,5094));
    String sQueryNotEqualToUnknown = "The unknowns you listed do not match to order or the set that the question asked for.";

    Set<Integer> setBadUnknowns = 
      new HashSet<Integer>(Arrays.asList(5095,5724,5164,5192,5929,5939,5947,
                                         5356,6722,6739,6149,6889,5628,6351));
    String sBadUnknowns = "Unknown names must contain only letters and \"_\".  They cannot contain numbers, such as \"number_1\", spaces such as \"number one\", and they cannot contains symbols such as \"one%\", or \"jane\'s\".  In the list of unknown names, the unknowns must be typed *exactly* as they are in the equations, including the same use of Uppercase and Lowercase.";
    Set<Integer> setBadEquations = 
      new HashSet<Integer>(Arrays.asList(6462,6481,6588,6015,6027,5589,
                                         5127,6423,6440,5745,5148,5044,
                                         5055,6428,5765,6497,6514,6522,5802,
                                         6495,5893,5282,6598,5299,6627,
                                         5314,5970,5978,6657,6031,6672,
                                         6047,6716,5454,6731,5478,5479,6176,
                                         5049,5054,5121));
    String sBadEquations = "These equations are either badly formatted, or they do not correctly solve the problem.  Note that equations can only contain numbers, unknowns, and the operators \"=\", \"+\",\"-\",\"*\", \"/\".  Equations can also contain \"(\" and \")\" for grouping.  All parenthesis must be matched, i.e. the number of \"(\" must match the number of \")\".  Any problems which require additional operators such as powers, or \"<\" should be marked as BAD with an appropriate comment.  All operations much be explicit.  So \"3x\" should be written as \"3*x\".  All equations must contain and \"=\", otherwise it is not an equation.  For single variable questions just write a single variable equation, such as \"x = 3*5\"";


    Set<Integer> setBadParenthesis = 
      new HashSet<Integer>(Arrays.asList(6797,6214,5584,6888));
    String sBadParenthesis = "The parenthesis don't match up.";

    Set<Integer> setSkipForNow = 
      new HashSet<Integer>(Arrays.asList(5015,5016));

    Set<Integer> setRoundingIssues =
      new HashSet<Integer>(Arrays.asList(6000));//accept these
      
    Set<Integer> setProblemsToSkip = 
      new HashSet<Integer>(Arrays.asList(
                                         5066,//requires rounding
                                         5079,//requires units conversion
                                         5141,//question doesn't make sense
                                         5151,//asks for a fraction as answer
                                         5778,//uclr if asking for 1 answer or2 
                                         5845,// there are two possible corr answers
                                         5888,//ambiguous, is 210 roundtrip or one way dist?
                                         5303,//which number?
                                         6576,//requires less than constraint
                                         6144,//requires rounding
                                         6184,//requires rounding
                                         5721//ambigous could be 5 or 29
                                         ));
    Map<Integer,String> mBad = new HashMap<Integer,String>();
    mBad.put(6435,"You equations should contain a unknown for the number");
    mBad.put(5734,"You equations should contain a unknown for each number since that's what being asked for");

    Set<Integer> setShouldBeOrderUnclear =
      new HashSet<Integer>(Arrays.asList(5106,6654,6008));
    String sShouldBeOrderUnclear = "This problem should have been marked as order unclear";

    List<String[]>lRows = 
      Misc.readCsvFile("amazon/Batch_1419266_batch_results_a.csv");
    int iQuestion = 0;
    List<SimpleSolver> lSolvers = Question.buildSolvers();
    List<String[]> lReject = new ArrayList<String[]>();
    int iNumReject = 0;
    for(String[] aRow : lRows){
      if(aRow[0].equals("HITId")){
        lReject.add(aRow);
        continue;
      }
      String[] aRejectRow = new String[36];
      System.arraycopy(aRow, 0, aRejectRow, 0, aRow.length);
      iQuestion++;
      int iIndex = Integer.parseInt(aRow[27]);
      if(setBadUnknowns.contains(iIndex)){
        aRejectRow[35] = sBadUnknowns;
      }
      if(setBadEquations.contains(iIndex)){
        aRejectRow[35] = sBadEquations;
      }
      if(setBadParenthesis.contains(iIndex)){
        aRejectRow[35] = sBadParenthesis;
      }
      if(setShouldBeOrderUnclear.contains(iIndex)){
        aRejectRow[35] = sShouldBeOrderUnclear;
      }
      if(mBad.containsKey(iIndex)){
        aRejectRow[35] = mBad.get(iIndex);
      }
      if(setBadSpelling.contains(iIndex)){
        aRejectRow[35] = sBadSpelling;
      }
      if(setQueryNotEqualToUnknown.contains(iIndex)){
        aRejectRow[35] = sQueryNotEqualToUnknown;
      }

      if(setSkipForNow.contains(iIndex) || setProblemsToSkip.contains(iIndex)||
         setRoundingIssues.contains(iIndex)){
        continue;
      }

      if(setBadUnknowns.contains(iIndex) || setBadEquations.contains(iIndex) ||
         setBadParenthesis.contains(iIndex) || 
         setShouldBeOrderUnclear.contains(iIndex) ||
         mBad.containsKey(iIndex) ||
         setBadSpelling.contains(iIndex) ||
         setQueryNotEqualToUnknown.contains(iIndex)){
        iNumReject++;
        lReject.add(aRejectRow);
        continue;
      }

      if(setSkipForNow.contains(iIndex) || setProblemsToSkip.contains(iIndex)||
         setRoundingIssues.contains(iIndex)){
        continue;
      }


      Question question = mQuestions.get(iIndex);
      Misc.Assert(question != null);
      String sQuestion = aRow[28];
      String sEquations = aRow[31];
      String[] aEquations = sEquations.split("\\r?\\n");
      List<String> lEquations = new ArrayList<String>();
      for(int iEquation = 0; iEquation < aEquations.length; iEquation++){
        String sEquation = aEquations[iEquation];
        if(sEquation.startsWith("1)") || sEquation.startsWith("2)") ||
           sEquation.startsWith("3) ")){
          sEquation = sEquation.substring(2);
        } else if(sEquation.equals("")){
          continue;
        }
        lEquations.add(sEquation);
      }
      if(lEquations.size() == 0){
        System.out.println("Bad Question");
        aRejectRow[35] = "No Equations were given";
        continue;
      }

      String sOrderUnclear = aRow[32];
      boolean bOrderUnclear = sOrderUnclear.equals("ORDERUNCLEAR");
      Misc.Assert(bOrderUnclear || sOrderUnclear.equals(""));
      String sUnknowns = aRow[33];
      String[] aUnknowns = sUnknowns.split("\\r?\\n");
      List<String> lUnknowns = new ArrayList<String>();
      for(int iUnknown = 0; iUnknown < aUnknowns.length; iUnknown++){
        String sUnknown = aUnknowns[iUnknown];
        if(sUnknown.startsWith("1)") || sUnknown.startsWith("2)") ||
           sUnknown.startsWith("3)")){
          sUnknown = sUnknown.substring(Misc.findFirstLetter(sUnknown));
        } else if(sUnknown.contains(",")){
          lUnknowns.addAll(Arrays.asList(sUnknown.split(", ")));
        } else if(sUnknown.equals("")){
          continue;
        } else {
          sUnknown = sUnknown.replaceAll("\\s+$", "");
          lUnknowns.add(sUnknown);
        }
      }
      if(lUnknowns.size() == 0){
        System.out.println("Bad Question");
        continue;
      }
      System.out.println("****************************");
      System.out.println("iQuestion: " + iQuestion + " out of: " +lRows.size());
      System.out.println("Question-"+ iIndex + ":");
      System.out.println(sQuestion);
      System.out.println(sEquations);
      System.out.println("ORDERUNCLEAR:" + sOrderUnclear);
      System.out.println(sUnknowns);
      System.out.println("Processing...");
      ConcreteSystem system = Parser.parseSystemNoException(lEquations);
      system.setQuery(lUnknowns);
      //Misc.Assert(question.system == null);
      //question.system = system;
      // if(!question.system.isNonlinear()){
      //   question.test(lSolvers, bOrderUnclear);
      // }
    }
    System.out.println("Num Rejected: " + iNumReject);
    Misc.writeCsvFileNoException("amazon/reject.csv", lReject);
  }

  static StringBuilder toStringBuilder(StringBuilder sb, int iIndex, 
                                       QuestionConfig config,
                                       String sQuestion,
                                       List<Double> lSolutions, 
                                       List<String> lEquations,
                                       List<String> lUnknowns){
    sb.append("Question-").append(iIndex).append(":\n");
    if(config.sReject != null){
      sb.append(config.sReject).append("\n");
    }
    if(config.sBad != null){
      sb.append(config.sBad).append("\n");
    }
    if(config.bOrderUnclear){
      sb.append("orderunclear\n");
    }
    sb.append(sQuestion)
      .append("\n");
    for(Double fSolution : lSolutions){
      sb.append(fSolution).append("\n");
    }
    sb.append("------\n");
    for(String sEquation : lEquations){
      sb.append(sEquation).append("\n");
    }
    sb.append("------\n");
    for(String sUnknown : lUnknowns){
      sb.append(sUnknown).append("\n");
    }
    sb.append("*******************\n");
    return sb;
  }


  static void read2(String sCsvFile) throws IOException{
    int iQuestion = 28;
    // int iBad = 33;
    // int iComment = 34;
    // int iEquations = 35;
    // int iOrderUnclear = 36;
    // int iUnknowns = 37;
    // int iApprove = 38;
    // int iReject = 39;

    int iBad = 29;
    int iComment = 30;
    int iEquations = 31;
    int iOrderUnclear = 32;
    int iUnknowns = 33;
    int iApprove = 34;
    int iReject = 35;
    List<Question> lOldQuestions =
      Question.loadFromFileNew("data/questions-all-equations.txt", true);
    Map<Integer,Question> mQuestions = new HashMap<Integer,Question>();
    for(Question question : lOldQuestions){
      mQuestions.put(question.iIndex, question);
    }
    List<String[]>lRows = Misc.readCsvFile(sCsvFile);
    List<Question> lQuestions = new ArrayList<Question>();
    StringBuilder sb = new StringBuilder();
    List<SimpleSolver> lSolvers = Question.buildSolvers();
    for(String[] aRow : lRows){
      if(aRow[0].equals("HITId")){
        continue;
      }
      int iIndex = Integer.parseInt(aRow[27]);
      String sQuestion = aRow[iQuestion];
      String sBad = aRow[iBad];
      boolean bBad = sBad.equals("BAD");
      if(!bBad && !sBad.equals("")){
        System.out.println("Bad bad: " + sBad);
      }
      Misc.Assert(bBad || sBad.equals(""));
      String sComment = aRow[iComment];
      String sEquations = aRow[iEquations];
      String[] aEquations = sEquations.split("\\r?\\n");
      List<String> lEquations = new ArrayList<String>();
      for(int iEquation = 0; iEquation < aEquations.length; iEquation++){
        String sEquation = aEquations[iEquation];
        if(sEquation.startsWith("1)") || sEquation.startsWith("2)") ||
           sEquation.startsWith("3) ")){
          sEquation = sEquation.substring(2);
        } else if(sEquation.equals("")){
          continue;
        }
        lEquations.add(sEquation);
      }
      QuestionConfig config = new QuestionConfig();
      if(bBad){
        config.sBad = "bad - " + sComment;
      } else if(lEquations.size() == 0){
        config.sReject = "reject - no equations provided";
      }
      String sOrderUnclear = aRow[iOrderUnclear];
      config.bOrderUnclear = sOrderUnclear.equals("ORDERUNCLEAR");
      Misc.Assert(config.bOrderUnclear || sOrderUnclear.equals(""));
      String sUnknowns = aRow[iUnknowns];
      String[] aUnknowns = sUnknowns.split("\\r?\\n");
      List<String> lUnknowns = new ArrayList<String>();
      for(int iUnknown = 0; iUnknown < aUnknowns.length; iUnknown++){
        String sUnknown = aUnknowns[iUnknown];
        if(sUnknown.startsWith("1)") || sUnknown.startsWith("2)") ||
           sUnknown.startsWith("3)")){
          sUnknown = sUnknown.substring(Misc.findFirstLetter(sUnknown));
        } else if(sUnknown.contains(",")){
          lUnknowns.addAll(Arrays.asList(sUnknown.split(", ")));
        } else if(sUnknown.equals("")){
          continue;
        } else {
          sUnknown = sUnknown.replaceAll("\\s+$", "");
          lUnknowns.add(sUnknown);
        }
      }
      if(lUnknowns.size() == 0){
        config.sReject = "reject - no unknowns listed";
      }
      ConcreteSystem system = null;
      try{
        system = Parser.parseSystemNoException(lEquations);
      } catch(RuntimeException ex){
        config.sReject = "reject - the equations you provided do not fit the specified format";
      }
      if(system != null){
        try{
          system.setQuery(lUnknowns);
        } catch(RuntimeException ex){
          config.sReject = "reject - the unknowns in your list could not be found in the equations your provided (maybe do to a typo?)";
        }
      }
      Question questionOld = mQuestions.get(iIndex);
      if(config.isGood()){
        Question question = new Question(sQuestion, questionOld.lSolutions, 
                                       system, config, iIndex);
        String sTest = question.test(lSolvers);
        if(sTest != null){
          config.sReject = "reject - TESTFAIL: " + sTest;
        }
      }
      
      toStringBuilder(sb, iIndex, config, sQuestion, questionOld.lSolutions, 
                      lEquations, lUnknowns);
    }
    FileUtils.writeStringToFile(new File("nate.out"), sb.toString());
  }

  static void test(String sFilename){
    List<Question> lQuestions =
      Question.loadFromFileNew(sFilename, true);
    List<SimpleSolver> lSolvers = Question.buildSolvers();
    int iNumNonlinear = 0;    
    int iNumNoEquations = 0;
    int iNumCorrect = 0;
    int iNumWrong = 0;
    int iNumSkipped = 0;
    System.out.println("failed tests********");
    for(Question question : lQuestions){
      if(question.config.sSkipReason != null){
        iNumSkipped++;
      } else if(question.config.bNonlinear){
        iNumNonlinear++;
      } else if(question.system == null){
        iNumNoEquations++;
      } else {
        String sTest = question.test(lSolvers);
        if(sTest == null){
          iNumCorrect++;
        } else {
          System.out.println("***********************");
          System.out.println("QUESTION-" + question.iIndex + ": " + sTest);
          iNumWrong++;
        }
      }
    }
    System.out.println("NonLinear: " + iNumNonlinear + " NoEquations: " 
                       + iNumNoEquations + " Correct: " + iNumCorrect 
                       + " Wrong: " + iNumWrong + " Skipped: " + 
                       iNumSkipped + " out of: " + 
                       lQuestions.size());
  }
  
  static void integrate(String sFileNew){
    List<Question> lQuestions =
      Question.loadFromFileNew("data/questions-all-equations.txt", true);
    List<Question> lNew =
      Question.loadFromFileNew(sFileNew, true);
    Map<Integer,Question> mNew = questionsToMap(lNew);
    List<Question> lFinal = new ArrayList<Question>(lQuestions.size());
    for(Question question : lQuestions){
      if(mNew.containsKey(question.iIndex)){
        Misc.Assert(question.system == null);
        lFinal.add(mNew.get(question.iIndex));
      } else {
        lFinal.add(question);
      }
    }
    Question.writeAllToFile(lFinal, "nate.out");
    //Question.writeAllToFile(lNew, "nate.out");
  }

  static Map<Integer,Question> questionsToMap(List<Question> lQuestions){
    Map<Integer,Question> mQuestions = new HashMap<Integer,Question>();
    for(Question question : lQuestions){
      mQuestions.put(question.iIndex, question);
    }
    return mQuestions;
  }


  static void buildResponse(String sQuestionFile, String sCsvFile){
    List<Question> lQuestions = Question.loadFromFileNew(sQuestionFile,
                                                         true);
    Map<Integer,Question> mQuestions = questionsToMap(lQuestions);
    List<String[]>lRows = Misc.readCsvFileNoException(sCsvFile);
    List<String[]> lOutput = new ArrayList<String[]>(lRows.size());
    for(String[] aRow : lRows){
      if(aRow[0].equals("HITId")){
        lOutput.add(aRow);
        continue;
      }
      System.out.println("LENGTH: " + aRow.length);
      int iFeedback=22;
      int iQuestion = 28;
      int iBad = 29;
      int iComment = iBad+1;
      int iEquations = iBad+2;
      int iOrderUnclear = iBad+3;
      int iUnknowns = iBad+4;
      int iApprove = iBad+5;
      int iReject = iBad+6;

      String[] aRowOut = new String[iReject+1];
      lOutput.add(aRowOut);
      System.arraycopy(aRow, 0, aRowOut, 0, aRow.length);
      System.out.println("Index: " + aRow[27]);
      int iIndex = Integer.parseInt(aRow[27]);
      Question question = mQuestions.get(iIndex);
      Misc.Assert(question != null);
      String sFeedback = null;
      if(question.config.sFullReject != null){
        sFeedback = question.config.sFullReject;
      } else if(question.config.sReject != null){
        sFeedback = question.config.sReject;
      }
      if(sFeedback != null){
        StringBuilder sb = new StringBuilder();
        if(question.config.sFullReject == null){
          sb.append("Your answer was not rejected, but it could have been rejected for the following reason:\n");
        }
        sb.append(sFeedback)
          .append("\n***Question: ").append(question.sQuestion)
          .append("\n***Your Equations: \n").append(aRow[iEquations])
          .append("\n***Your Unknowns: \n").append(aRow[iUnknowns]);
        if(question.system != null){
          sb.append("\n***Correct Equations: \n")
            .append(question.system.toString())
            .append("***Correct Unknowns: \n");
          if(question.system.lQuery != null){
            for(Integer iUnknown : question.system.lQuery){
              sb.append(question.system.lUnknowns.get(iUnknown)).append("\n");
            } 
          } else {
            for(String sUnknown : question.system.lUnknowns){
              sb.append(sUnknown).append("\n");
            }
          }
        }
        aRowOut[iFeedback] = sb.toString();
      }

      if(question.config.sFullReject != null){
        aRowOut[iReject] = question.config.sFullReject;
      } else {
        aRowOut[iApprove] = "X";
      }
    }
    Misc.writeCsvFileNoException("nate.csv", lOutput);
  }

  static void rewriteTest(String sFile){
    //rewrite to nate.out
    List<Question> lQuestions = Question.loadFromFileNew(sFile, true);
    Question.writeAllToFile(lQuestions, "nate.out");
    test("nate.out");
  }

  static void unreject(String sCsvFile){
    List<Question> lQuestions = 
      Question.loadFromFileNew("data/questions-all-equations.txt", true);
    Map<Integer,Question> mQuestions = questionsToMap(lQuestions);
    //we're going to update in place here
    List<String[]>lRows = Misc.readCsvFileNoException(sCsvFile);
    int iFeedback=22;
    int iBad = 33;
    if(lRows.get(0).length == 36){
      iBad = 29;
    } else {
      Misc.Assert(lRows.get(0).length == 40);
    }
    int iComment = iBad+1;
    int iEquations = iBad+2;
    int iOrderUnclear = iBad+3;
    int iUnknowns = iBad+4;
    int iApprove = iBad+5;
    int iReject = iBad+6;
    System.out.println("Going through the rows");
    int iNumReverted = 0;
    for(String[] aRow : lRows){
      Misc.Assert(aRow.length==iReject+1);
      if(aRow[0].equals("HITId")){
        if(aRow.length != iReject+1){
          System.out.println("Bad Length: " + aRow.length + " " + (iReject+1));
        }
        Misc.Assert(aRow[iReject].equals("Reject"));
        continue;
      }
      String sReject = aRow[iReject];
      //if it wasn't rejected before, then just leave it alone
      if(sReject.equals("")){
        System.out.println("Skipping because no reject");
        continue;
      }
      int iIndex = Integer.parseInt(aRow[27]);
      if(iIndex == 5044){
        aRow[iReject] = "";
        aRow[iApprove] = "X";
        aRow[iFeedback] = "You rejection is being reverted because it was rejected in error";
        continue;
      }

      Question question = mQuestions.get(iIndex);
      if(question == null){
        System.out.println("No Question: " + iIndex + " MapSize: " 
                           + mQuestions.size());
        Misc.Assert(question != null);
      }
      //if there's no equations, and it's not marked bad, then leave the
      // rejection
      String sBad = aRow[iBad];
      boolean bBad = sBad.equals("BAD");
      Misc.Assert(bBad || sBad.equals(""));
      if(!bBad && aRow[iEquations].equals("")){
        System.out.println("Skipping bad: " + bBad);
        continue;
      }
      //otherwise revert the rejection with feedback
      String sFeedback = "";
      StringBuilder sb = new StringBuilder();
      sb.append("Your reject is being reverted, because we've decided that we were being too harsh with the rejections.  But do not take this as a signal that your answer was correct.  You answer was still incorrect for the reason listed below. Even though your response was not rejected, please still e-mail us if you think your response was correct, or do not understand why we marked it incorrect.  We're hoping to use this feedback channel to correct mistakes instead of rejecting responses.\n***Reason:\n");
      sb.append(sReject);
      sb.append("\n***Question:\n").append(question.sQuestion);
      sb.append("\n***Your Equations:\n").append(aRow[iEquations]);
      sb.append("\n***Your Unknowns:\n").append(aRow[iUnknowns]);
      if(question.system != null){
        sb.append("\n***Correct Equations: \n")
          .append(question.system.toString())
          .append("***Correct Unknowns: \n");
        Misc.Assert(question.system.lQuery != null);
        for(Integer iUnknown : question.system.lQuery){
          sb.append(question.system.lUnknowns.get(iUnknown)).append("\n");
        } 
      }
      iNumReverted++;
      aRow[iFeedback] = sb.toString();
      aRow[iApprove] = "X";
      aRow[iReject] = "";
    }
    String sStart = "amazon/amazon-response";
    Misc.Assert(sCsvFile.startsWith(sStart));
    String sOutputFile = sStart + "-revert-rejection" 
      + sCsvFile.substring(sStart.length(), sCsvFile.length());
    Misc.writeCsvFileNoException(sOutputFile, lRows);
    System.out.println("Reverted: " + iNumReverted);
  }

  static void generateNewUploadFile(){
    List<Question> lQuestions = 
      Question.loadFromFileNew("data/questions-all-equations.txt", true);
    List<String[]> lOut = new ArrayList<String[]>();
    lOut.add(new String[]{"QuestionNumber","InputQuestion"});
    int iNumValid = 0;
    int iNumNoEquation = 0;
    for(Question question : lQuestions){
      if(question.config.isValid()){
        iNumValid++;
        if(question.system == null){
          iNumNoEquation++;
          lOut.add(new String[]{Integer.toString(question.iIndex),
                                question.sQuestion});
        }
      }
    }
    System.out.println("NumValid: " + iNumValid);
    System.out.println("NumNoEquation: " + iNumNoEquation);
    Misc.writeCsvFileNoException("nate.csv", lOut);
  }


  public static void main(String[] args) throws Exception{
    //read2("amazon/amazon-results-912-0-thru-300.csv");
    //test("data/questions-all-equations.txt");
    //test("nate.out");
    //rewriteTest("data/questions-all-equations.txt");
    //integrate("data/questions-amazon-912-0-300-fixed.txt");
    //test("data/questions-amazon-912-0-300-fixed.txt");
    buildResponse("data/questions-amazon-912-0-300-fixed.txt", 
                  "amazon/amazon-results-912-0-thru-300.csv");
    //test("data/questions-all-equations.txt");
    //unreject("amazon/amazon-response-0-thru-100.csv");
    //unreject("amazon/amazon-response-100-thru-200.csv");
    //unreject("amazon/amazon-response-200-thru-500.csv");
    //generateNewUploadFile();
  }
}