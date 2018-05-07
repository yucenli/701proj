import java.lang.*;
import java.util.*;
import java.io.*;

import org.apache.commons.lang3.tuple.*;
import org.apache.commons.io.FileUtils;

class CleanQuestions{
  static List<String> lAllowed = 
    Arrays.asList("1st","2nd","3rd","2-digit","mp3","par-3","par-4","par-5",
                  "H1","3-digit","4th","5th","6th","h1","13th", "21st","10th",
                  "16th","30th","b12");

  static void checkNoSpaceBeforeUnits(int iIndex, String sWord){
    if(!lAllowed.contains(sWord) && 
       sWord.matches(".*\\d.*") && 
       sWord.matches(".*[A-Za-z].*")){
      System.out.println("BadWord: " + iIndex + " " + sWord);
    }
  }

  static void checkNoSlash(int iIndex, String sWord){
    if(sWord.contains("/")){
      System.out.println("BadWord: " + iIndex + " " + sWord);
    }
  }

  public static void checkForBadWords(List<Question> lQuestions){
    for(Question question : lQuestions){
      if(question.sQuestion.contains("(") || question.sQuestion.contains(")")){
        System.out.println("Bad Question: " + question.iIndex);
      }
      for(StanfordSentence sentence : question.doc.lSentences){
        for(StanfordWord word : sentence.lWords){
          if(!word.isRootParent()){
            checkNoSpaceBeforeUnits(question.iIndex, word.sWordOrig);
            checkNoSlash(question.iIndex, word.sWordOrig);
          }
        }
      }
    }
  }
  
  static List<String> getWordListAsStrings(Question question){
    List<String> lWordStrings = new ArrayList<String>();
    for(StanfordSentence sentence : question.doc.lSentences){
      for(StanfordWord word : sentence.lWords){
        if(!word.isRootParent()){
          lWordStrings.add(word.sWordOrig);
        }
      }
    }
    return lWordStrings;
  }

  public static void checkForDuplicates(List<Question> lQuestions){
    //first get string lists for each
    List<List<String>> llQuestionWordStrings = new ArrayList<List<String>>();
    for(Question question : lQuestions){
      llQuestionWordStrings.add(getWordListAsStrings(question));
    }

    for(int iQuestion1 = 0; iQuestion1 < lQuestions.size(); iQuestion1++){
      List<String> lQuestion1 = llQuestionWordStrings.get(iQuestion1);
      int iIndex1 = lQuestions.get(iQuestion1).iIndex;
      double fClosest = 0.0;
      for(int iQuestion2 = iQuestion1+1; iQuestion2 < lQuestions.size();
          iQuestion2++){
        List<String> lQuestion2 = llQuestionWordStrings.get(iQuestion2);
        int iIndex2 = lQuestions.get(iQuestion2).iIndex;
        double fSim = Misc.similarity(lQuestion1, lQuestion2);
        //System.out.println(iIndex1 + " " + iIndex2 + " " + fSim);
        fClosest = Math.max(fClosest, fSim);
      }
      System.out.println("CLOSEST: " + iIndex1 + " " + fClosest);
    }
  }

  public static void printClosest(List<List<String>> llWordStrings){
    for(int iQuestion1 = 0; iQuestion1 < llWordStrings.size(); iQuestion1++){
      List<String> lQuestion1 = llWordStrings.get(iQuestion1);
      double fClosest = 0.0;
      for(int iQuestion2 = iQuestion1+1; iQuestion2 < llWordStrings.size();
          iQuestion2++){
        List<String> lQuestion2 = llWordStrings.get(iQuestion2);
        double fSim = Misc.similarity(lQuestion1, lQuestion2);
        //System.out.println(iIndex1 + " " + iIndex2 + " " + fSim);
        fClosest = Math.max(fClosest, fSim);
      }
      System.out.println("CLOSEST: " + fClosest);
    }
  }

  public static void processGeoQuery() throws IOException {
    List<List<String>> llWordStrings = new ArrayList<List<String>>();
    List<String> lLines =
      FileUtils.readLines(new File("data/geoqueries880-questions.txt"));
    for(String sLine : lLines){
      llWordStrings.add(Arrays.asList(sLine.split(",")));
    }
    printClosest(llWordStrings);
  }

  public static void processFreebase() throws IOException {
    List<List<String>> llWordStrings = new ArrayList<List<String>>();
    List<String> lTrainLines =
      FileUtils.readLines(new File("data/free917-train.txt"));
    List<String> lTestLines =
      FileUtils.readLines(new File("data/free917-test.txt"));
    List<String> lAllLines = new ArrayList<String>(lTrainLines);
    lAllLines.addAll(lTestLines);
    for(String sLine : lAllLines){
      llWordStrings.add(Arrays.asList(sLine.split(" ")));
    }
    printClosest(llWordStrings);
  }  


  static void findConstants(List<Question> lQuestions){
    StringBuilder sb = new StringBuilder();
    Map<String,Double> mAllowed = new HashMap<String,Double>();
    List<Pair<String,Double>> llAllowed = new ArrayList<Pair<String,Double>>();
    llAllowed.add(ImmutablePair.of("cent",0.01));
    llAllowed.add(ImmutablePair.of("cents",0.01));
    llAllowed.add(ImmutablePair.of("penny",0.01));
    llAllowed.add(ImmutablePair.of("pennies",0.01));
    llAllowed.add(ImmutablePair.of("nickel",0.05));
    llAllowed.add(ImmutablePair.of("nickel",5.0));
    llAllowed.add(ImmutablePair.of("dime",0.1));
    llAllowed.add(ImmutablePair.of("dime",10.0));
    llAllowed.add(ImmutablePair.of("quarter",0.25));
    llAllowed.add(ImmutablePair.of("quarter",25.0));
    llAllowed.add(ImmutablePair.of("consecutive even",2.0));
    llAllowed.add(ImmutablePair.of("even consecutive",2.0));
    llAllowed.add(ImmutablePair.of("consecutive odd",2.0));
    llAllowed.add(ImmutablePair.of("odd consecutive",2.0));
    llAllowed.add(ImmutablePair.of("consecutive",1.0));
    llAllowed.add(ImmutablePair.of("digit",10.0));
    llAllowed.add(ImmutablePair.of("digit",100.0));
    llAllowed.add(ImmutablePair.of("DIGIT",10.0));
    llAllowed.add(ImmutablePair.of("DIGIT",100.0));
    llAllowed.add(ImmutablePair.of("%",0.01));
    llAllowed.add(ImmutablePair.of("percentage",100.0));
    llAllowed.add(ImmutablePair.of("percent",100.0));
    llAllowed.add(ImmutablePair.of("%",100.0));

    for(Question question : lQuestions){
      if(question.system == null){
        continue;
      }
      //if(question.iIndex != 6114){
      //  continue;
      //}
      List<Double> lAllowedConstants = new ArrayList<Double>();
      lAllowedConstants.add(1.0);
      lAllowedConstants.add(0.0);
      if(question.config.lAllowedConstants != null){
        lAllowedConstants.addAll(question.config.lAllowedConstants);
      }
      for(Pair<String,Double> pair : llAllowed){
        if(question.sQuestion.contains(pair.getLeft())){
          lAllowedConstants.add(pair.getRight());
        }
      }
      if(question.system.system.hasDisallowedConstants(lAllowedConstants)){
        //System.out.println("Has Disallowed: allowed: " + lAllowedConstants);
        //System.out.println(question.toFullString());
        sb.append("**************************");
        question.toFullStringBuilder(sb, false);
      } else {
        //System.out.println("All Allowed");
      }
    }
    try {
      FileUtils.writeStringToFile(new File("constants.txt"), sb.toString());
    }catch(IOException ex){
      throw new RuntimeException(ex);
    }
  }

  public static void computeEquationStats(){
    Model.load();
    //extract the equations from the training set
    CountMap<Equation> mEquationCounts = new CountMap<Equation>();
    for(Question question : Model.lTrainQuestions){
      for(Equation equation : question.system.lSingleEquations){
        mEquationCounts.increment(equation);
      }
    }
    for(Map.Entry<Equation,Integer> entry : mEquationCounts.entrySet()){
      System.out.println("Entry: " + entry.getValue() + " " 
                         + entry.getKey().toExactString());
    }
  }

  public static void computeEquationSystemStats(){
    Model.load();
    //extract the equations from the training set
    CountMap<String> mEquationCounts = new CountMap<String>();
    for(Question question : Model.lTrainQuestions){
      mEquationCounts.increment(question.system.system.toFeatureString());
    }
    for(Map.Entry<String,Integer> entry : mEquationCounts.entrySet()){
      System.out.println("Entry: " + entry.getValue() + " " 
                         + entry.getKey());
    }
  }

  static List<String> getWordList(StanfordDocument doc){
    List<String> lWordStrings = new ArrayList<String>();
    for(StanfordSentence sentence : doc.lSentences){
      for(StanfordWord word : sentence.lWords){
        if(!word.isRootParent()){
          lWordStrings.add(word.sWordOrig);
        }
      }
    }
    return lWordStrings;
  }

  static void removeDuplicates(){
    List<QuestionText> lQuestions = 
      QuestionText.readListFromJson("data/questions.json");
    String sDirectory = "../stanford-parser/parses-manual/";
    List<List<String>> lWordLists = new ArrayList<List<String>>();
    for(QuestionText question : lQuestions){
      if(question.isValid()){
        String sFilename = sDirectory + "question-" + question.iIndex + ".xml";
        StanfordDocument doc = StanfordParser.parseFile(sFilename);
        lWordLists.add(getWordList(doc));
      } else {
        lWordLists.add(null);
      }
    }

    for(int iQuestion1 = 0; iQuestion1 < lQuestions.size(); iQuestion1++){
      QuestionText question = lQuestions.get(iQuestion1);
      if(!question.isValid()){
        continue;
      }
      List<String> lQuestion1 = lWordLists.get(iQuestion1);
      int iIndex1 = lQuestions.get(iQuestion1).iIndex;
      double fClosest = -1.0;
      int iClosest = -1;
      for(int iQuestion2 = iQuestion1-1; iQuestion2 >= 0; iQuestion2--){
        QuestionText question2 = lQuestions.get(iQuestion2);
        if(!question2.isValid()){
          continue;
        }
        List<String> lQuestion2 = lWordLists.get(iQuestion2);
        Misc.Assert(lQuestion1 != null);
        Misc.Assert(lQuestion2 != null);
        int iIndex2 = lQuestions.get(iQuestion2).iIndex;
        double fSim = Misc.similarity(lQuestion1, lQuestion2);
        if(fSim > fClosest){
          fClosest = fSim;
          iClosest = iQuestion2;
        }
      }
      if(fClosest > 0.85){
        QuestionText questionClose = lQuestions.get(iClosest);
        QuestionText questionToKeep = questionClose;
        QuestionText questionDup = question;
        if((questionDup.lEquations != null) && 
           (questionToKeep.lEquations == null)){
          questionToKeep = questionDup;
          questionDup = questionClose;
        }
        System.out.println("Marking Duplicate: " + questionToKeep.iIndex + " " 
                           + questionDup.iIndex);
        questionDup.iDuplicate = questionToKeep.iIndex;
      }
    }
    QuestionText.writeListToJson(lQuestions, "question.json");
    Misc.Assert(lWordLists.size() == lQuestions.size());
  }

  static void testNewQuestions(){
    List<Question> lQuestions = 
      Question.loadFromFileNew(Config.config.sFullQuestionFile, true);
    int iNumDups = 0;
    for(Question question : lQuestions){
      if(question.config.iDuplicate != null){
        iNumDups++;
      }
    }
    System.out.println("NumDups: " + iNumDups);
  }

  static void testQuestions(){
    List<Question> lQuestions = 
      Question.loadFromFileNew(Config.config.sFullQuestionFile, false);
    Maxima maxima = new Maxima();
    for(Question question : lQuestions){
      if(question.system != null){
        System.out.println("Testing: " + question.iIndex);
        maxima.testQuestion(question);
      }
    }
  }

  static void testQuestionsMatrix(){
    List<Question> lQuestions = 
      Question.loadFromFileNew(Config.config.sFullQuestionFile, false);
    List<SimpleSolver> lSolvers = Question.buildSolvers();
    for(Question question : lQuestions){
      if(question.system != null){
        String sTestResult = question.test(lSolvers);
        if(sTestResult != null){
          System.out.println("Failed on Question-" + question.iIndex + ": " + 
                             sTestResult);
          Misc.Assert(sTestResult == null);
        }
      }
    }
  }

  public static void main(String[] args) throws Exception{
    Model.load();
    //Config.load();
    
    //List<Question> lQuestions = 
    //  Question.loadWithParses("data/questions-all-equations.txt",false);
    //check for tokens that start with a number but contain letters
    //checkForBadWords(lQuestions);
    //checkForDuplicates(lQuestions);
    //test(lQuestions);
    //processGeoQuery();
    //processFreebase();
    findConstants(Model.lAllQuestions);
    //computeEquationStats();
    //computeEquationSystemStats();
    //removeDuplicates();
    //testNewQuestions();
    //testQuestionsMatrix();
  }
}