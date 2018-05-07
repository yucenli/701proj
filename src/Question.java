import java.lang.*;
import java.util.*;
import java.io.*;

import org.apache.commons.lang3.tuple.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import edu.stanford.nlp.process.Morphology;

class Question{
  int iIndex;
  String sQuestion;
  List<Double> lSolutions;
  ConcreteSystem system;
  StanfordDocument doc;
  QuestionConfig config;
  List<Object> lNGrams;
  List<ConcreteEqu> lCorrectEqus;
  ConcreteTerm ct;
  List<List<StanfordWord>> llAllowedUnknowns;
  boolean bGoldUnknowns;
  boolean bGoldUnknownSystem = false;
  QuestionText qt;
  

  Question(String sQuestion, List<Double> lSolutions, ConcreteSystem system,
           QuestionConfig config, int iIndex){
    this.sQuestion = sQuestion;
    this.lSolutions = lSolutions;
    this.config = config;
    this.iIndex = iIndex;
    this.system = system;
  }

  Question(QuestionText question){
    this.iIndex = question.iIndex;
    this.sQuestion = question.sQuestion;
    this.lSolutions = question.lSolutions;
    if(question.lEquations != null){
      this.system = Parser.parseSystemNoException(question.lEquations);
      //this.system.setQuery(question.lQueryVars);
    }
    this.config = new QuestionConfig(question);
    this.qt = question;
  }


  void loadStanfordParse(){
    loadStanfordParse(Config.config.sStanfordParseDirectory);
  }

  void loadStanfordParse(String sDirectory){
    if(Config.config.bPrintDuringQuestionLoad){
      System.out.println("Loading Stanford Parse for: " + this.iIndex);
    }
    String sFilename = sDirectory + "question-" + iIndex + ".xml";
    doc = StanfordParser.parseFile(sFilename);
    if(this.system != null){
      system.engrainConstants(doc.lDoubles);
      //buildEqus();
      this.ct = new ConcreteTerm(system);
      this.computeAllowedUnknowns();
    }
  }

  void computeAllowedUnknowns(){
    this.llAllowedUnknowns = new ArrayList<List<StanfordWord>>();
    if(this.qt.lUnknowns != null){
      for(String sUnknown : qt.lUnknowns){
        llAllowedUnknowns.add(Arrays.asList(Misc.getWord(sUnknown, this.doc)));
      }
      bGoldUnknowns = true;
      return;
    }

    Morphology morphology = new Morphology();
    this.llAllowedUnknowns = new ArrayList<List<StanfordWord>>();
    for(String sUnknown : this.ct.lUnknownStrings){
      if(sUnknown.contains(":")){
        //it's an exact match
        continue;
      }
      List<Pair<StanfordWord,Integer>> lOverlaps = 
        new ArrayList<Pair<StanfordWord,Integer>>();
      int iBestOverlapType = 0;
      for(StanfordWord word : this.doc.lNouns){
        int iOverlapType = Misc.overlapType(sUnknown,word,morphology);
        lOverlaps.add(ImmutablePair.of(word, iOverlapType));
        iBestOverlapType = Math.max(iBestOverlapType, iOverlapType);
      }
      //now add everything to the list with the best overlap type
      List<StanfordWord> lAllowed = new ArrayList<StanfordWord>();
      for(Pair<StanfordWord,Integer> pair : lOverlaps){
        if(pair.getRight() == iBestOverlapType){
          lAllowed.add(pair.getLeft());
        }
      }
      llAllowedUnknowns.add(lAllowed);
    }
  }


  void buildEqus(){
    lCorrectEqus = new ArrayList<ConcreteEqu>();
    try{
      for(ConcreteEquation equation : system.lConcreteEquations){
        lCorrectEqus.add(new ConcreteEqu(equation));
      }
    } catch(ConcreteEqu.BadEquationException ex){
      lCorrectEqus = null;
    }
  }

  static List<Question> loadFromJson(String sFilename, boolean bIncludeInvalid){
    List<QuestionText> lQuestionTexts = 
      QuestionText.readListFromJson(sFilename);
    List<Question> lQuestions = new ArrayList<Question>();
    for(QuestionText qt : lQuestionTexts){
      if(Config.config.bPrintDuringQuestionLoad){
        System.out.println("Generating Question: " + qt.iIndex);
      }
      Question question = new Question(qt);
      if(bIncludeInvalid || question.config.isValid()){
        lQuestions.add(question);
      }
    }
    return lQuestions;
  }

  static List<Question> loadWithParses(String sFilename, 
                                       boolean bIncludeInvalid){
    List<Question> lQuestions = loadFromFileNew(sFilename, bIncludeInvalid);
    for(Question question : lQuestions){
      question.loadStanfordParse();
    }
    return lQuestions;
  }

  static List<Question> loadAllValid(){
    return loadWithParses(Config.config.sFullQuestionFile, false);
  }

  static List<Question> loadAllValidWithEquations(){
    List<Question> lQuestions = 
      Question.loadFromFileNew(Config.config.sFullQuestionFile, false);
    List<Question> lQuestionsPruned = new ArrayList<Question>();
    for(Question question : lQuestions){
      if(question.system != null){
        question.loadStanfordParse();
        lQuestionsPruned.add(question);
      }
    }
    return lQuestionsPruned;
  }


  static List<Question> loadFromFileNew(String sFilename, 
                                        boolean bIncludeInvalid){
    if(true){
      return loadFromJson(sFilename, bIncludeInvalid);
    }
    List<String> lLines;
    try{
      lLines = FileUtils.readLines(new File(sFilename));
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }
    List<Question> lQuestions = new ArrayList<Question>();
    for(int iLine = 0; iLine < lLines.size();){
      if(!lLines.get(iLine).startsWith("Question")){
        System.out.println("Bad line-" + iLine + ": " + lLines.get(iLine));
        Misc.Assert(lLines.get(iLine).startsWith("Q"));
      }
      int iIndex = Integer.parseInt(lLines.get(iLine).split("-|:")[1]);
      iLine++;
      if(Config.config.bPrintDuringQuestionLoad){
        System.out.println("*****PARSING QUESTION: " + iIndex);
      }
      QuestionConfig config = new QuestionConfig();
      iLine = config.read(lLines, iLine);
      String sQuestion = lLines.get(iLine);
      iLine++;
      List<Double> lSolutions = new ArrayList<Double>();
      while((iLine <lLines.size()) && !lLines.get(iLine).contains("----")){
        lSolutions.add(Double.parseDouble(lLines.get(iLine)));
        iLine++;
      }
      iLine++;
      List<String> lEquations = new ArrayList<String>();
      while((iLine <lLines.size()) && 
            !lLines.get(iLine).startsWith("**************") &&
            !lLines.get(iLine).startsWith("----")){
        lEquations.add(lLines.get(iLine));
        iLine++;
      }
      ConcreteSystem system = null;
      if(lEquations.size() > 0){
        system = Parser.parseSystemNoException(lEquations);
      }
      List<String> lUnknowns = null;
      if(lLines.get(iLine).startsWith("----")){
        //there's an unknown list
        iLine++;
        lUnknowns = new ArrayList<String>();
        while(!lLines.get(iLine).startsWith("**************")){
          lUnknowns.add(lLines.get(iLine));
          iLine++;
        }
        system.setQuery(lUnknowns);
      }
      if(bIncludeInvalid || config.isValid()){
        lQuestions.add(new Question(sQuestion, lSolutions, system, 
                                    config, iIndex));
      }
      iLine++;
    }
    return lQuestions;
  }

  static List<QuestionText> loadToQuestionText(String sFilename){
    List<String> lLines;
    try{
      lLines = FileUtils.readLines(new File(sFilename));
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }
    List<QuestionText> lQuestions = new ArrayList<QuestionText>();
    for(int iLine = 0; iLine < lLines.size();){
      if(!lLines.get(iLine).startsWith("Question")){
        System.out.println("Bad line-" + iLine + ": " + lLines.get(iLine));
        Misc.Assert(lLines.get(iLine).startsWith("Q"));
      }
      int iIndex = Integer.parseInt(lLines.get(iLine).split("-|:")[1]);
      iLine++;
      if(Config.config.bPrintDuringQuestionLoad){
        System.out.println("*****PARSING QUESTION: " + iIndex);
      }
      QuestionConfig config = new QuestionConfig();
      iLine = config.read(lLines, iLine);
      String sQuestion = lLines.get(iLine);
      iLine++;
      List<Double> lSolutions = new ArrayList<Double>();
      while((iLine <lLines.size()) && !lLines.get(iLine).contains("----")){
        lSolutions.add(Double.parseDouble(lLines.get(iLine)));
        iLine++;
      }
      iLine++;
      List<String> lEquations = new ArrayList<String>();
      while((iLine <lLines.size()) && 
            !lLines.get(iLine).startsWith("**************") &&
            !lLines.get(iLine).startsWith("----")){
        lEquations.add(lLines.get(iLine));
        iLine++;
      }
      List<String> lUnknowns = null;
      if(lLines.get(iLine).startsWith("----")){
        //there's an unknown list
        iLine++;
        lUnknowns = new ArrayList<String>();
        while(!lLines.get(iLine).startsWith("**************")){
          lUnknowns.add(lLines.get(iLine));
          iLine++;
        }
      }
      lQuestions.add(new QuestionText(sQuestion, lSolutions, lEquations, 
                                      lUnknowns, config, iIndex));
      iLine++;
    }
    return lQuestions;
  }

  static void rewriteQuestionsAsJson(){
    Config.load();
    String sFileIn = "data/questions-all-equations.txt";
    String sFileOut = "questions.json";
    List<QuestionText> lQuestions = loadToQuestionText(sFileIn);
    QuestionText.writeListToJson(lQuestions, sFileOut);
  }
  

  static List<Question> load(boolean bTrain, int iTestFold){
//  static List<Question> load(boolean bTrain){
    List<Question> lQuestions =loadFromFileNew(Config.config.sFullQuestionFile,
                                               false);
    if(Config.config.bTestOneQuestion){
      List<Integer> lNums = Arrays.asList(Config.config.iTestQuestionNumber);
      lQuestions = pruneQuestionList(lQuestions,lNums);
    } else {
      List<Integer> lNums;
      if(Config.config.bUseFoldData){
        System.out.println("TEST FOLD: " + iTestFold);
        lNums = new ArrayList<Integer>();
        for(int iFold = 0; iFold < Config.config.iNumFolds; iFold++){
          if((bTrain && (iFold == iTestFold)) || 
             (!bTrain && (iFold != iTestFold))){
            continue;
          }
          String sFileName = Config.config.sFoldFilePrefix + iFold + ".txt";
          lNums.addAll(Misc.loadIntList(sFileName));
        }
        Misc.Assert(lNums.size() > 0);
      } else {
        if(bTrain){
          lNums = Misc.loadIntList(Config.config.sTrainQuestionNumberFile);
        } else {
          lNums = Misc.loadIntList(Config.config.sTestQuestionNumberFile);
        }
      }
      lQuestions = pruneQuestionList(lQuestions,lNums);
    }      
    if(Config.config.bPrintDuringQuestionLoad){
      System.out.println("Loading Stanford Parses:");
    }
    for(Question question : lQuestions){
      if(Config.config.bPrintDuringQuestionLoad){
        System.out.println("Loading Stanford Parses for Question-" + 
                           question.iIndex);
      }
      question.loadStanfordParse();
    }
    if(Config.config.bLoadNGrams){
      CountMap<Object> mCounts = new CountMap<Object>();
      for(Question question : lQuestions){
        question.lNGrams = getNGramFeatures(question);
        mCounts.incrementAll(question.lNGrams);
      }
      if(Config.config.bUseFudgeFactors){
        Model.features.mFudgeFactors = new HashMap<Object,Double>();
      }

      for(Question question : lQuestions){
        List<Object> lNewNGrams = new ArrayList<Object>();
        for(Object obj : question.lNGrams){
          if(!(mCounts.get(obj) < Config.config.iMinNGramCount)){
            lNewNGrams.add(obj);
            if(Config.config.bUseFudgeFactors){
              double idf=Math.log(Misc.div(lQuestions.size(),mCounts.get(obj)));
              Model.features.mFudgeFactors.put(obj, idf);
            }
          }
        }
        question.lNGrams = lNewNGrams;
      }
    }
    //compute the questions which are using systems which have at least
    // one gold unknown
    Set<String> setGoldUnknownSystems = new HashSet<String>();
    for(Question question : lQuestions){
      if(question.bGoldUnknowns){
        setGoldUnknownSystems.add(question.ct.mt.toString(false,true));
      }
    }
    int iNumGoldUnknownSystemQuestions = 0;
    for(Question question : lQuestions){
      if(setGoldUnknownSystems.contains(question.ct.mt.toString(false,true))){
        question.bGoldUnknownSystem = true;
        iNumGoldUnknownSystemQuestions++;
      }
    }
    System.out.println("NUM Gold Unknown Systems: " 
                       + setGoldUnknownSystems.size() + " Questions: " +
                       iNumGoldUnknownSystemQuestions);
    return lQuestions;
  }

  static List<LinkedList<String>> genEmptyNGramList(int iSize){
    List<LinkedList<String>> llNGrams =new ArrayList<LinkedList<String>>(iSize);
    for(int i = 0; i < iSize; i++){
      llNGrams.add(new LinkedList<String>());
    }
    return llNGrams;
  }

  static List<Object> getNGramFeatures(Question question){
    List<Object> lFeatures = new ArrayList<Object>();
    //let's just try n-gram features to being with
    //initial the ngram list with empty linked lists
    int iMaxNGram = Config.config.iMaxNGram;

    for(StanfordSentence sentence : question.doc.lSentences){
      
      List<LinkedList<String>> llNGramsOrig = genEmptyNGramList(iMaxNGram);
      List<LinkedList<String>> llNGramsStem = genEmptyNGramList(iMaxNGram);
      List<LinkedList<String>> llNGramsPos = genEmptyNGramList(iMaxNGram);
      int iWordsAdded = 0;
      for(int iWord = 0; iWord < sentence.lWords.size(); iWord++){
        StanfordWord wordCur = sentence.lWords.get(iWord);
        if(wordCur.isRootParent() || 
           Config.config.setStopWords.contains(wordCur.sWord)){
          continue;
        }
        if(Config.config.bRemoveNumbersFromNGrams &&
           (StanfordNumber.getNumber(wordCur.sWord) != null)){
          continue;
        }

        iWordsAdded++;
        //add the new words
        for(int iNGram = 0; iNGram < Config.config.iMaxNGram; iNGram++){
          llNGramsOrig.get(iNGram).addLast(wordCur.sWordOrig);
          llNGramsStem.get(iNGram).addLast(wordCur.sWord);
          llNGramsPos.get(iNGram).addLast(wordCur.sPos);
        }
        //subtract the old ones if necessary
        int iMaxRemove = Math.min(iWordsAdded-1, Config.config.iMaxNGram);
        for(int iNGram = 0; iNGram < iMaxRemove; iNGram++){
          llNGramsOrig.get(iNGram).removeFirst();
          llNGramsStem.get(iNGram).removeFirst();
          llNGramsPos.get(iNGram).removeFirst();
        }
        //now add the features
        int iMaxToAdd = Math.min(iWordsAdded, Config.config.iMaxNGram);
        for(int iNGram = 0; iNGram < iMaxToAdd; iNGram++){
          List<String> lOrig = new ArrayList<String>(llNGramsOrig.get(iNGram));
          List<String> lStem = new ArrayList<String>(llNGramsStem.get(iNGram));
          List<String> lPos = new ArrayList<String>(llNGramsPos.get(iNGram));
          Misc.Assert(lStem.size() <= Config.config.iMaxNGram);
          
          if(Config.config.bNGramOrig){
            lFeatures.add(Arrays.asList("NGRAM-ORIG:",iNGram, lOrig));
          }
          if(Config.config.bNGramStems){
            lFeatures.add(Arrays.asList("NGRAM-STEM:",iNGram, lStem));
          }
          if(Config.config.bNGramPos){
            lFeatures.add(Arrays.asList("NGRAM-POS:",iNGram, lPos));
          }
        }
      }
    }
    if(Config.config.bBinaryNGrams){
      //convert to set and back to make them binary
      Set<Object> setFeatures = new HashSet<Object>(lFeatures);
      lFeatures = new ArrayList<Object>(setFeatures);
    } 
    return lFeatures;
  }



  static List<Question> pruneQuestionList(List<Question> lQuestions,
                                          List<Integer> lNums){
    //put questions into a map
    Map<Integer, Question> mQuestions = new HashMap<Integer,Question>();
    for(Question question : lQuestions){
      mQuestions.put(question.iIndex, question);
    }
    // build a list from the nums
    List<Question> lPruned = new ArrayList<Question>(lNums.size());
    for(Integer iIndex : lNums){
      Question question = mQuestions.get(iIndex);
      //Misc.Assert(question != null, "Missing Question: " + iIndex);
      if(question != null){
        lPruned.add(question);
      }
    }
    return lPruned;
  }

  public String toString(){
    StringBuilder sb = new StringBuilder();
    for(StanfordSentence sentence : this.doc.lSentences){
      sb.append(" ").append(sentence.sSentenceOrig);
    }
    return sb.toString();
  }

  public String toFullString(){
    return toFullStringBuilder(new StringBuilder(), false).toString();
  }

  public StringBuilder toFullStringBuilder(StringBuilder sb, 
                                           boolean bUseParseText){
    sb.append("Question-").append(this.iIndex).append(":\n");
    config.toStringBuilder(sb);
    if(bUseParseText){
      this.doc.toStringBuilderOneLine(sb);
    } else {
      sb.append(sQuestion);
    }
    sb.append("\n");
    for(Double fSolution : lSolutions){
      sb.append(fSolution).append("\n");
    }
    sb.append("------\n");
    if(this.system != null){
      this.system.toStringBuilder(sb);
      List<String> lQuery;
      if(system.lQuery != null){
        lQuery = new ArrayList<String>();
        Misc.Assert(system.lQuery.size() != 0);
        for(Integer iQuery : system.lQuery){
          lQuery.add(system.lUnknowns.get(iQuery));
        }
      } else {
        lQuery = system.lUnknowns.subList(0, this.lSolutions.size());
      }
      sb.append("------\n");
      for(String sQuery : lQuery){
        sb.append(sQuery).append("\n");
      }
    }
    return sb;
  }

  static void writeStringsForParsing(String sQuestionFile, String sDir)
    throws IOException{
    Config.load();
    List<Question> lQuestions = loadFromFileNew(sQuestionFile, false);
    String sMainDir = "stanford-parser/" + sDir + "/";
    PrintWriter pw = new PrintWriter(new FileWriter(sMainDir + "files.txt"));
    for(Question question : lQuestions){
      String sFilename = "question-" + question.iIndex;
      File file = new File(sMainDir + sFilename);
      boolean bChanged = true;
      if(file.exists()){
        String sOld = FileUtils.readFileToString(file);
        bChanged = !sOld.equals(question.sQuestion);
      }
      if(bChanged){
        System.out.println("Found change in question: " + question.iIndex);
        FileUtils.writeStringToFile(file, question.sQuestion);
        pw.println(sDir + "/" + sFilename);
      }
    }
    pw.close();
  }


  static Map<Integer,Question> questionsToMap(List<Question> lQuestions){
    Map<Integer,Question> map = new HashMap<Integer,Question>();
    for(Question question : lQuestions){
      map.put(question.iIndex, question);
    }
    return map;
  }

  static double getNumber(EquationTerm term, List<Double> lNumbers){
    // assumes this is either a number of a number*unknown
    if(term instanceof EquationTerm.Unknown){
      return 1.0;
    } else {
      EquationTerm.Complex complex = (EquationTerm.Complex) term;
      if(complex.termLeft instanceof EquationTerm.Number){
        Misc.Assert(complex.termRight instanceof EquationTerm.Unknown);
        return lNumbers.get(((EquationTerm.Number) complex.termLeft).iIndex);
      } else {
        Misc.Assert(complex.termLeft instanceof EquationTerm.Unknown);
        Misc.Assert(complex.termRight instanceof EquationTerm.Number);
        return lNumbers.get(((EquationTerm.Number) complex.termRight).iIndex);
      }
    }        
  }


  static void writeAllToFile(List<Question> lQuestions, String sFilename){
    StringBuilder sb = new StringBuilder();
    for(Question question : lQuestions){
      System.out.println("Writing Question: " + question.iIndex);
      question.toFullStringBuilder(sb, false);
      sb.append("*******************\n");
    }
    try{
      FileUtils.writeStringToFile(new File(sFilename), sb.toString());
    }catch(IOException ex){
      throw new RuntimeException(ex);
    }
  } 


  static boolean solutionClose(double fCorrect, double fCalced){
    if(fCorrect == 0){
      return ((fCalced < 0.1) && (fCalced > -0.1));
    } else {
      return ((fCalced/fCorrect) < 1.1) && ((fCalced/fCorrect) > .99);
    }
  }

  String test(List<SimpleSolver> lSolvers){
    //System.out.println("Testing question: " + this.iIndex +" "+this.bNonlinear);
    if(this.config.bNonlinear){
      System.out.println("Skipping because nonlinear");
      return "NonLinear";
    }
    if(this.system == null){
      System.out.println("Skipping no equations");
      return "No Equations";
    }

    if(this.system.lUnknowns.size() != this.system.system.lEquations.size()){
      return "Num Equations: " + this.system.system.lEquations.size() + 
        " Num Unknowns: " + this.system.lUnknowns.size();
    }
    Misc.Assert(this.system.system.lEquations.size() <= 10);
    int iSize = this.system.system.lEquations.size();
    SimpleSolver solver = lSolvers.get(iSize-1);
    for(int iEquation = 0; iEquation < iSize; iEquation++){
      Equation equation = this.system.system.lEquations.get(iEquation);
      double[] aConstraint = equation.toConstraint(this.system.lNumbers,
                                                   iSize);
      if(aConstraint == null){
        return "Cannot make constraint for equation: " + equation.toString();
      }
      solver.addConstraint(aConstraint, iEquation);
    }
    boolean bValid = solver.solve();
    if(!bValid){
      return "Matrix is singular";
    }
    boolean bHasQuery = (this.system.lQuery != null);
    if(bHasQuery){
      if(this.lSolutions.size() !=this.system.lQuery.size()){
        return "BadQuery size: " + this.system.lQuery.size() + 
          " insteadof: " + this.lSolutions.size();
        //Misc.Assert(this.lSolutions.size() ==this.system.lQuery.size());
      }
    }
    if(config.bOrderUnclear){
      for(Double fSolution : this.lSolutions){
        boolean bFound = false;
        if(bHasQuery){
          for(Integer iIndex : this.system.lQuery){
            if(solutionClose(fSolution, solver.x[iIndex])){
              bFound = true;
              break;
            }
          }
        } else {
          for(int iIndex = 0; iIndex < solver.x.length; iIndex++){
            if(solutionClose(fSolution, solver.x[iIndex])){
              bFound = true;
              break;
            }
          }
        }
        if(!bFound){
          return "Bad OrderUnclear Answer: Correct: " + lSolutions
            + " Calced: " + Arrays.toString(solver.x) + " Couldn't find: " 
            + fSolution;
        }
      }
    } else {
      System.out.println("Correct: " + this.lSolutions + " Calced: " +
                         Arrays.toString(solver.x));
      for(int iSol = 0; iSol <this.lSolutions.size();iSol++){
        double fSolution = this.lSolutions.get(iSol);
        int iIndex = bHasQuery ? this.system.lQuery.get(iSol) : iSol;
        double fCalced = solver.x[iIndex];
        if(!solutionClose(fSolution, fCalced)){
          //make the solution set
          List<Double> lCalced;
          if(this.system.lQuery != null){
            lCalced = new ArrayList<Double>();
            for(Integer iQuery : this.system.lQuery){
              lCalced.add(solver.x[iQuery]);
            }
          } else {
            lCalced = Arrays.asList(ArrayUtils.toObject(solver.x));
          }
          return "Correct: " + lSolutions + " Calced: " + lCalced + " x: " 
            + Arrays.toString(solver.x);
        }
      }
    }
    return null;
  }

  void findAllNumberMappings(List<StanfordNumber> lCur,
                             List<List<StanfordNumber>> llMaps){
    if(lCur.size() == system.lNumbers.size()){
      llMaps.add(new ArrayList<StanfordNumber>(lCur));
      return;
    }
    double fNumber = system.lNumbers.get(lCur.size());
    boolean bFoundOne = false;
    for(StanfordNumber number : doc.lNumbers){
      if(Misc.isCloseTo(number.fNumber, fNumber)){
        bFoundOne = true;
        lCur.add(number);
        findAllNumberMappings(lCur, llMaps);
        lCur.remove(lCur.size()-1);
      }
    }
    if(!bFoundOne){
      System.out.println(this.toFullString());
      System.out.println("Couldn't find mapping for: " + fNumber);
      Misc.Assert(bFoundOne);
    }
  }


  List<List<StanfordNumber>> findAllNumberMappings(){
    List<List<StanfordNumber>> llMaps = new ArrayList<List<StanfordNumber>>();
    findAllNumberMappings(new ArrayList<StanfordNumber>(), llMaps);
    return llMaps;
  }


  static List<SimpleSolver> buildSolvers(){
    List<SimpleSolver> lSolvers = new ArrayList<SimpleSolver>();
    for(int iSolver = 1; iSolver <= 10; iSolver++){
      lSolvers.add(new SimpleSolver(iSolver));
    }
    return lSolvers;
  }

  static void testAll(String sFile){
    List<Question> lQuestions = loadFromFileNew(sFile, false);
    List<SimpleSolver> lSolvers = buildSolvers();
    //now make sure the answers are consistent
    for(Question question : lQuestions){
      question.test(lSolvers);
    }
    int iNumEquation=0;
    int iNumNoEquation=0;
    for(Question question : lQuestions){
      if(question.system != null){
        iNumEquation++;
      } else{
        iNumNoEquation++;
      }
    }
    System.out.println("Num with equations: " + iNumEquation + " numWithout: " 
                       + iNumNoEquation);
  }

  public static void writeHtml() throws IOException{
    Config.load();
    List<FullQuestion> lQuestions = 
      FullQuestion.readCsvFile(Config.config.sQuestionCvsFile);
    for(FullQuestion question : lQuestions){
      String sFileName = "html/"+question.iMyId + ".html";
      FileUtils.writeStringToFile(new File(sFileName), question.sSolution);
    }
  }

  static Question getQuestion(int iIndex, List<Question> lQuestions){
    for(Question question : lQuestions){
      if(question.iIndex == iIndex){
        return question;
      }
    }
    Misc.Assert(false);
    return null;
  }

  boolean isCorrectLoose(List<Double> lCalced){
    //now compare these to the correct answer
    boolean bFoundError = false;
    for(Double fSolution : this.lSolutions){
      boolean bFoundCorr = false;
      for(Double fCalced : lCalced){
        if(Question.solutionClose(fSolution, fCalced)){
          bFoundCorr = true;
          break;
        }
      }
      if(!bFoundCorr){
        bFoundError = true;
        break;
      }
    }
    return !bFoundError;
  }    
    


  boolean isCorrectNumerically(FilledTerm ft, List<SimpleSolver> lSolvers){
    List<Double> lCalced = ft.solve(lSolvers);
    if(lCalced == null){
      return false;
    }
    return isCorrectLoose(lCalced);
  }


  public static void main(String[] args) throws Exception{
    //test("data/twobytwo-equations-submission.txt");
    //rewriteTwoByTwo();
    //test2();
    //rewriteAll();
    //rewriteNewAll("data/questions-all-equations.txt", "nate.out");
    //rewriteNewAll("nate.out","nate2.out");
    //testAll("data/questions-all-equations.txt");
    //writeHtml();
    //writeAmazonCsv();
    writeStringsForParsing("data/questions.json", "questions");
    //rewriteQuestionsAsJson();
  }  
}
