import java.lang.*;
import java.util.*;
import java.io.*;

import org.apache.commons.io.FileUtils;

import uk.ac.ed.ph.jacomax.*;
import uk.ac.ed.ph.jacomax.utilities.*;

class Maxima{
  MaximaInteractiveProcess process;


  Maxima(){
    MaximaConfiguration configuration = JacomaxSimpleConfigurator.configure();
    MaximaProcessLauncher launcher = new MaximaProcessLauncher(configuration);
    this.process = launcher.launchInteractiveProcess();
    try{
      String sResult = process.executeCall("display2d:false;", 10);
      String sParsed = 
        MaximaOutputUtilities.parseSingleLinearOutputResult(sResult);
      Misc.Assert(sParsed.equals("false"));
    } catch(MaximaTimeoutException ex){
      throw new RuntimeException(ex);
    }
  }


  void close(){
    if(process != null){
      process.terminate();
    }
    process = null;
  }
  
  public void finalize() {
    close();
  }
  
  String run(String sCommand){
    try{
      System.out.println("Running Command: " + sCommand);
      String result = process.executeCall(sCommand, 10);
      return MaximaOutputUtilities.parseSingleLinearOutputResult(result);
    } catch(MaximaTimeoutException ex){
      throw new RuntimeException(ex);
    }
  }

  String buildSolveCommand(List<String> lUnknowns, List<String> lNumbers,
                           List<String> lSolveUnknowns,
                           EquationSystem system){
    Misc.Assert(lUnknowns.size() == lSolveUnknowns.size());
    StringBuilder sb = new StringBuilder();
    sb.append("float(solve([");
    boolean bFirst = true;
    for(Equation equation : system.lEquations){
      if(!bFirst){
        sb.append(",");
      }
      bFirst = false;
      equation.toStringBuilder(sb, lUnknowns, lNumbers);
    }
    sb.append("],[");
    bFirst = true;
    for(String sUnknown : lSolveUnknowns){
      if(!bFirst){
        sb.append(",");
      }
      bFirst = false;
      sb.append(sUnknown);
    }
    sb.append("]));");
    return sb.toString();
  }


  List<Double> solve(ConcreteSystem system){
    List<String> lUnknowns = Misc.listOfLetters('a', system.lUnknowns.size());
    List<String> lNumbers = Misc.toStringList(system.lNumbers);
    String sCommand = buildSolveCommand(lUnknowns, lNumbers, lUnknowns,
                                        system.system);
    String sResult = run(sCommand);
    //parse the result
    return MaximaParser.parseDoubleResults(sResult);
  }


  String canonicalizeSolution(String sSolution, int iNumNumbers){
    //assumes numbers are in range a-l
    //assumes numbers are in range m-z
    StringBuilder sb = new StringBuilder();
    int iCur = 0;
    char cCurNum = 'a';
    char cCurUnknown = 'm';
    List<Character> lNewChars = Misc.<Character>genEmptyList(iNumNumbers);
    for(int iChar = 0; iChar < sSolution.length(); iChar++){
      char c = sSolution.charAt(iChar);
      if((c >= 'a') && (c < 'm')){
        // it's a number var
        int iIndex = c-'a';
        Character cNew = lNewChars.get(iIndex);
        if(cNew != null){
          sb.append(cNew);
        } else {
          lNewChars.set(iIndex, cCurNum);
          sb.append(cCurNum);
          cCurNum++;
        }
      } else if((c >= 'm') && (c < 'z')){
        sb.append(cCurUnknown);
        cCurUnknown++;
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }


  String canonicalize(EquationSystem system){
    Misc.Assert(system.iNumUnknowns < 13);
    Misc.Assert(system.iNumUnknowns < 13);
    List<String> lUnknowns = Misc.listOfLetters('m',system.iNumUnknowns);
    List<String> lNumbers = Misc.listOfLetters('a',system.iNumNumbers);
    //now generate all permutations of the unknowns
    List<String> lCanonicalized = new ArrayList<String>();
    for(List<String> lSolveUnknowns : Misc.genAllPermutations(lUnknowns)){
      String sCommand = buildSolveCommand(lUnknowns, lNumbers, lSolveUnknowns,
                                          system);
      String sResult = this.run(sCommand);
      String sCanonicalized = canonicalizeSolution(sResult, system.iNumNumbers);
      lCanonicalized.add(sCanonicalized);
    }
    //now sort the list
    Collections.sort(lCanonicalized);
    return lCanonicalized.get(0);
  }

  static void test(){
    Maxima maxima = new Maxima();
    String sResult = maxima.run("solve([x+y=d,a*x+b*y=c],[x,y]);");
    maxima.close();
    System.out.println("Result: " + sResult);
  }

  void testQuestion(Question question){
    List<Double> lResults = this.solve(question.system);
    if(question.config.bOrderUnclear){
      for(Double fSolution : question.lSolutions){
        //make sure we have it somewhere in our list of results
        boolean bFound = false;
        for(Double fResult : lResults){
          bFound |= Question.solutionClose(fSolution,fResult);
        }
        Misc.Assert(bFound);
      }
    } else {
      for(int iQuery = 0; iQuery < question.system.lQuery.size(); iQuery++){
        double fResult = lResults.get(question.system.lQuery.get(iQuery));
        double fSolution = question.lSolutions.get(iQuery);
        if(!Question.solutionClose(fSolution,fResult)){
          System.out.println("Results: " + lResults + " Correct: " + fSolution
                             + " Found: " + fResult);
          Misc.Assert(Question.solutionClose(fSolution,fResult));
        }
      }
    }
  }


  static void test2(){
    Model.load();
    Maxima maxima = new Maxima();
    List<SimpleSolver> lSolvers = Question.buildSolvers();
    try{
      for(Question question : Model.lAllQuestions){
        System.out.println("Solving question:\n" + question.toFullString());
        String sTestResult = question.test(lSolvers);
        Misc.Assert(sTestResult == null);
        maxima.testQuestion(question);
      }
    }finally{
      maxima.close();
    }
  }


  static void test3(){
    Model.load();
    Maxima maxima = new Maxima();
    try{
      Question question = Model.lAllQuestions.get(10);
      maxima.canonicalize(question.system.system);
    }finally{
      maxima.close();
    }
  }

  static void test4(){
    List<Question> lQuestions = Question.loadFromFileNew("test.txt", false);
    Question question1 = lQuestions.get(0);
    Question question2 = lQuestions.get(1);
    Maxima maxima = new Maxima();
    try{
      String sQuestion1 = maxima.canonicalize(question1.system.system);
      String sQuestion2 = maxima.canonicalize(question1.system.system);
      System.out.println("Question1: " + sQuestion1);    
      System.out.println("Question2: " + sQuestion2);    
      System.out.println("Equal: " + sQuestion1.equals(sQuestion2));
    } finally {
      maxima.close();
    }
  }

  static boolean AllNumbersInOneSentence(Question question, Equation equation){
    List<List<StanfordNumber>> llMaps = question.findAllNumberMappings();
    List<StanfordNumber> lCurEquation = new ArrayList<StanfordNumber>();
    Set<Integer> setUsed = equation.getUsedNumbers();
    String sEquation = equation.toString(question.system.lUnknowns, 
                                         question.system.lNumbers);
    if(setUsed.size() == 0){
      System.out.println("****ZERO USED:" + sEquation);
      return true;
    }
    for(List<StanfordNumber> lNumbers : llMaps){
      StanfordSentence sentence = null;
      boolean bSameSentence = true;
      for(Integer iNumber : setUsed){
        if(iNumber >= lNumbers.size()){
          System.out.println(question.toFullString());
          System.out.println("Index: " + iNumber + " Indices: " + setUsed
                             + " equation: " + sEquation);
          Misc.Assert(iNumber < lNumbers.size());
        }
        StanfordSentence sentenceCur = lNumbers.get(iNumber).word.sentence;
        if(sentence == null){
          sentence = sentenceCur;
        } else {
          if(sentenceCur != sentence){
            bSameSentence = false;
            break;
          }
        }
      }
      if(bSameSentence){
        System.out.println(sentence.sSentenceOrig);
        System.out.println(sEquation);
        //we found one mapping that matches
        return true;
      }
    }
    //no mapping matched
    return false;
  }

  static void test5(){
    Model.load();
    Maxima maxima = new Maxima();
    try{
      Set<String> setCanonical = new HashSet<String>();
      Set<EquationSystem> setSystems = new HashSet<EquationSystem>();
      Map<String,Integer> mCounts=new CountMap<String>();
      Set<String> setTrainEquations = new HashSet<String>();
      for(Question question : Model.lTrainQuestions){
        String sCanonical = maxima.canonicalize(question.system.system);
        setCanonical.add(sCanonical);
        setSystems.add(question.system.system);
        mCounts.put(sCanonical, mCounts.get(sCanonical)+1);
        for(Equation equation : question.system.system.lEquations){
          setTrainEquations.add(equation.toStructuralString());
        }
      }
      // now sort the counts
      List<Integer> lCounts = new ArrayList<Integer>(mCounts.values());
      Collections.sort(lCounts);

      int iFoundCanonical = 0;
      int iFound = 0;
      int iNumAllFound = 0;
      int iNumAllSameSentence = 0;
      Set<String> setNewCanonical = new HashSet<String>();
      List<Question> lNew = new ArrayList<Question>();
      Map<String,Integer> mCountsTestNoTrain=new CountMap<String>();
      for(Question question : Model.lTestQuestions){
        String sCanonical = maxima.canonicalize(question.system.system);
        if(setCanonical.contains(sCanonical)){
          iFoundCanonical++;
        } else {
          mCountsTestNoTrain.put(sCanonical, 
                                 mCountsTestNoTrain.get(sCanonical)+1);
          boolean bNew = setNewCanonical.add(sCanonical);
          if(bNew){
            lNew.add(question);
          }
          boolean bAllFound = true;
          boolean bAllSameSentence = true;
          for(Equation equation : question.system.system.lEquations){
            if(!setTrainEquations.contains(equation.toStructuralString())){
              bAllFound = false;
              if(!AllNumbersInOneSentence(question, equation)){
                bAllSameSentence = false;
              }
            }
          }
          if(bAllFound){
            iNumAllFound++;
          } else {
            iNumAllSameSentence++;
          }
        }
      }
      System.out.println("NumCanonical: " + iFoundCanonical + " New: " + 
                         setNewCanonical.size() + " NumAllFound: " + 
                         iNumAllFound + " NumSameSentence: " + 
                         iNumAllSameSentence + " out of: " + 
                         Model.lTestQuestions.size());
      try{
        FileUtils.writeLines(new File("counts.txt"), lCounts);
        FileUtils.writeLines(new File("counts-test-notrain.txt"), 
                             mCountsTestNoTrain.values());
      }catch(Exception ex){
        throw new RuntimeException(ex);
      }
    }finally{
      maxima.close();
    }
  }


  public static void main(final String[] args) throws Exception {
    test5();
  }
}