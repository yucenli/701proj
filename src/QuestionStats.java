import java.lang.*;
import java.util.*;
import java.io.*;

import org.apache.commons.io.FileUtils;


class QuestionStats{
  static void addQuestion(StringBuilder sb, Question question, 
                   CountMap<Equation> mCounts){
    sb.append("Question-").append(question.iIndex).append(":\n")
      .append(question.sQuestion).append("\n");
    sb.append("------\n");
    for(ConcreteEquation concrete : question.system.lConcreteEquations){
      int iCount = mCounts.get(concrete.equation);
      sb.append(iCount).append(": ").append(concrete.sConcrete).append("\n");
    }
    sb.append("****************************************\n");
  }

  static void printNumEquations(){
    Model.load();
    CountMap<Equation> mEquationCounts = new CountMap<Equation>();
    for(Question question : Model.lTrainQuestions){
      for(Equation equation : question.system.lSingleEquations){
        mEquationCounts.increment(equation);
      }
    }
    Set<Equation> setEquations = new HashSet<Equation>();
    for(Map.Entry<Equation,Integer> entry : mEquationCounts.entrySet()){
      if((entry.getValue() >= Config.config.iMinEquationCount) ||
         Config.config.bTestOneQuestion || 
         Config.config.bPruneToTenQuestionsForTesting){
        setEquations.add(entry.getKey());
      }
    }
    int iNumEquationsIn = 0;
    int iNumEquationsTotal = 0;
    for(Question question : Model.lTrainQuestions){
      iNumEquationsTotal += question.system.lSingleEquations.size();
      for(Equation equation : question.system.lSingleEquations){
        if(setEquations.contains(equation)){
          iNumEquationsIn++;
        }
      }
    }
    System.out.println("NumEquationInstancesTotal: " + iNumEquationsTotal + 
                       " NumEquationsIn: " + iNumEquationsIn + 
                       " NumQuestions: " + Model.lTrainQuestions.size());
    
  }

  static void printEquationCounts(){
    Model.load();
    CountMap<Equation> mEquationCounts = new CountMap<Equation>();
    for(Question question : Model.lTrainQuestions){
      for(Equation equation : question.system.lSingleEquations){
        mEquationCounts.increment(equation);
      }
    }
    StringBuilder sb = new StringBuilder();
    for(Question question : Model.lTrainQuestions){
      addQuestion(sb, question, mEquationCounts);
    }
    try{
      FileUtils.writeStringToFile(new File("counts.out"), sb.toString());
    }catch(IOException ex){
      throw new RuntimeException(ex);
    }
  }



  static void printMultipleUnknowns(){
    Model.load();
    int iNumQuestions = 0;
    for(Question question : Model.lAllQuestions){
      List<Equation> lMulti = new ArrayList<Equation>();
      for(Equation equation : question.system.lSingleEquations){
        if(SlotCounter.hasMultiUnknowns(equation)){
          lMulti.add(equation);
        }
      }
      if(lMulti.size() != 0){
        System.out.println("MULTI: " + question.toFullString());
        iNumQuestions++;
      }
    }
    System.out.println("Questions: " + iNumQuestions + " total: " + 
                       Model.lAllQuestions.size());
  }

  static void printInfrequentEquations(){
    Model.load();
    CountMap<Equation> mEquationCounts = new CountMap<Equation>();
    for(Question question : Model.lTrainQuestions){
      for(Equation equation : question.system.lSingleEquations){
        mEquationCounts.increment(equation);
      }
    }
    Set<Equation> setEquations = new HashSet<Equation>();
    for(Map.Entry<Equation,Integer> entry : mEquationCounts.entrySet()){
      if((entry.getValue() >= Config.config.iMinEquationCount) ||
         Config.config.bTestOneQuestion || 
         Config.config.bPruneToTenQuestionsForTesting){
        setEquations.add(entry.getKey());
      }
    }
    StringBuilder sb = new StringBuilder();
    //now find all the equations which are not in this set
    for(Question question : Model.lTrainQuestions){
      for(ConcreteEquation concrete : question.system.lConcreteEquations){
        if(!setEquations.contains(concrete.equation)){
          if(concrete.lNumbers.size() == 0){
            sb.append("****************************************\n")
              .append("EQUATION WITH NO NUMBERS: ").append(concrete.equation)
              .append("\n");
          } else {
            //find the number of times this equation appears
            int iCount = mEquationCounts.get(concrete.equation);
            //find which sentence (if any) contains all the necessary numbers
            for(StanfordSentence sentence : question.doc.lSentences){
              if(sentence.lDoubles.containsAll(concrete.lNumbers)){
                //found a good sentence
                sb.append("**********************************-")
                  .append(question.iIndex).append("\n")
                  .append(sentence.sSentenceOrig).append("\n")
                  .append(iCount).append(": ")
                  .append(concrete.sConcrete).append("\n");
                break;
              }
            }
          }
        }
      }
    }
    try{
      FileUtils.writeStringToFile(new File("single.out"), sb.toString());
    }catch(IOException ex){
      throw new RuntimeException(ex);
    }
    System.out.println("NumEquations: " + setEquations.size());
  }


  static void calcUniqueStats(){
    Model.load();
    List<Question> lTrainQuestions = Model.lTrainQuestions;
    List<Question> lTestQuestions = Model.lTestQuestions;
    int iMinTrainNum = 1;
    if(false){
      lTrainQuestions = Model.lAllQuestions;
      lTestQuestions = Model.lAllQuestions;
      iMinTrainNum = 2;
    }

    int iNumQuestions = lTestQuestions.size();
    int iNumEquationInstances = 0;
    int iNumEquationsSeenOnlyOnce = 0;
    int iNumEquationsSeenOnlyOnceSingleSentence = 0;
    int iNumEquationsSeenOnlyOnceNoNumbers = 0;
    int iNumQuestionsWithEquationsSeenOnlyOnce = 0;
    int iNumSystemsSeenOnlyOnce = 0;
    int iNumStructuresSeenOnlyOnce = 0;
    int iNumQuestionsWithStructuresSeenOnlyOnce = 0;
    int iNumQuestionsBadEqus=0;
    int iNumQuestionsBadEquWithoutUnique = 0;
    CountMap<Equation> mEquationCounts = new CountMap<Equation>();
    CountMap<EquationSystem> mSystemCounts = new CountMap<EquationSystem>();
    CountMap<String> mStructuralCounts = new CountMap<String>();
    for(Question question : lTrainQuestions){
      mSystemCounts.increment(question.system.system);
      for(Equation equation : question.system.lSingleEquations){
        mEquationCounts.increment(equation);
        mStructuralCounts.increment(equation.toStructuralString());
      }
    }
    // int iNumEquUnique = 0;
    // int iNumEquUniqueSubset = 0;
    // for(Question questionTest : lTestQuestions){
    //   boolean bFoundUnique = false;
    //   boolean bFoundUniqueSubset = false;
    //   for(ConcreteEqu equTest : questionTest.lCorrectEqus){
    //     //see if this one if found anywhere in the train
    //     boolean bFoundEqual = false;
    //     boolean bFoundSuperset = false;
    //     for(Question questionTrain : lTrainQuestions){
    //       for(ConcreteEqu equTrain : questionTrain.lCorrectEqus){
    //         if(equTest.equals(equTrain)){
    //           bFoundEqual = true;
    //         } else if(equTest.subsetOf(equTrain)) {
    //           bFoundSuperset = true;
    //         }
    //       }
    //     }
    //     if(!bFoundEqual){
    //       bFoundUnique = true;
    //     }
    //     if(!bFoundSuperset){
    //       bFoundUniqueSubset = true;
    //     }
    //   }
    //   if(bFoundUnique){
    //     iNumEquUnique++;
    //   }
    //   if(bFoundUniqueSubset){
    //     iNumEquUniqueSubset++;
    //   }
    // }


    Set<Equation> setEquations = new HashSet<Equation>();
    Set<EquationSystem> setSystems = new HashSet<EquationSystem>();
    Set<String> setStructures = new HashSet<String>();
    for(Map.Entry<Equation,Integer> entry : mEquationCounts.entrySet()){
      if(entry.getValue() >= iMinTrainNum){
        setEquations.add(entry.getKey());
      }
    }
    for(Map.Entry<EquationSystem,Integer> entry : 
          mSystemCounts.entrySet()){
      if(entry.getValue() >= iMinTrainNum){
        setSystems.add(entry.getKey());
      }
    }
    for(Map.Entry<String,Integer> entry : mStructuralCounts.entrySet()){
      if(entry.getValue() >= iMinTrainNum){
        setStructures.add(entry.getKey());
      }
    }
    for(Question question : lTestQuestions){
      if(question.lCorrectEqus == null){
        iNumQuestionsBadEqus++;
      }
      boolean bContainsOnlyOnce = false;
      boolean bContainsOnlyOnceStructure = false;
      if(!setSystems.contains(question.system.system)){
        iNumSystemsSeenOnlyOnce++;
      }
      for(ConcreteEquation concrete : question.system.lConcreteEquations){
        iNumEquationInstances++;
        if(!setEquations.contains(concrete.equation)){
          bContainsOnlyOnce = true;
          iNumEquationsSeenOnlyOnce++;
          boolean bSingleSentence = false;
          if(concrete.lNumbers.size() == 0){
            iNumEquationsSeenOnlyOnceNoNumbers++;
          } else {
            for(StanfordSentence sentence : question.doc.lSentences){
              if(sentence.lDoubles.containsAll(concrete.lNumbers)){
                bSingleSentence = true;
                break;
              }
            }
            if(bSingleSentence){
              iNumEquationsSeenOnlyOnceSingleSentence++;
            }
          }
        }
        if(!setStructures.contains(concrete.equation.toStructuralString())){
          bContainsOnlyOnceStructure = true;
          iNumStructuresSeenOnlyOnce++;
        }
      }
      if(bContainsOnlyOnce){
        iNumQuestionsWithEquationsSeenOnlyOnce++;
      } else {
        if(question.lCorrectEqus == null){
          iNumQuestionsBadEquWithoutUnique++;
          System.out.println("BadQuestion: " + question.toFullString());
        }
      }
      if(bContainsOnlyOnceStructure){
        iNumQuestionsWithStructuresSeenOnlyOnce++;
      }
    }
    System.out.println("NumQuestions: " + iNumQuestions);
    System.out.println("NumQuestionsBadEqus: " + iNumQuestionsBadEqus);
    System.out.println("NumQuestionsBadEqusWithoutUnique: " + 
                       iNumQuestionsBadEquWithoutUnique);
    System.out.println("NumQuestionsWithUniqueEquations: " + 
                       iNumQuestionsWithEquationsSeenOnlyOnce);
    System.out.println("NumQuestionsWithUniqueStructures: " + 
                       iNumQuestionsWithStructuresSeenOnlyOnce);
    //System.out.println("NumQuestionsWithUniqueEqu:" + iNumEquUnique);
    //System.out.println("NumQuestionsWithUniqueEquSubset:" 
    //+iNumEquUniqueSubset);
    System.out.println("NumEquationInstances: " + iNumEquationInstances);
    System.out.println("NumUniqueEquationInstances: " + 
                       iNumEquationsSeenOnlyOnce);
    System.out.println("NumUniqueEquationInstancesNoNumbers: " + 
                       iNumEquationsSeenOnlyOnceNoNumbers);
    System.out.println("NumUniqueEquationInstancesSingleSentence: " + 
                       iNumEquationsSeenOnlyOnceSingleSentence);
    System.out.println("NumUniqueStructureInstances: " + 
                       iNumStructuresSeenOnlyOnce);
    System.out.println("NumUniqueSystems: " + 
                       iNumSystemsSeenOnlyOnce);
    
  }

  static void calcUniqueStatsTerm(){
    Model.load();
    List<Question> lTrainQuestions = Model.lTrainQuestions;
    List<Question> lTestQuestions = Model.lTestQuestions;
    int iMinTrainNum = 1;
    if(true){
      lTrainQuestions = Model.lAllQuestions;
      lTestQuestions = Model.lAllQuestions;
      iMinTrainNum = 2;
    }

    int iNumQuestions = lTestQuestions.size();
    int iNumEquationInstances = 0;
    int iNumEquationsSeenOnlyOnce = 0;
    int iNumEquationsSeenOnlyOnceSingleSentence = 0;
    int iNumEquationsSeenOnlyOnceNoNumbers = 0;
    int iNumQuestionsWithEquationsSeenOnlyOnce = 0;
    int iNumSystemsSeenOnlyOnce = 0;
    int iNumStructuresSeenOnlyOnce = 0;
    int iNumQuestionsWithStructuresSeenOnlyOnce = 0;
    int iNumQuestionsBadEqus=0;
    int iNumQuestionsBadEquWithoutUnique = 0;
    CountMap<String> mEquationCounts = new CountMap<String>();
    CountMap<String> mSystemCounts = new CountMap<String>();
    CountMap<String> mStructuralCounts = new CountMap<String>();
    for(Question question : lTrainQuestions){
      mSystemCounts.increment(question.ct.mt.getSignature());
      for(MappedTerm mtEqu : question.ct.mt.term.lSubTerms){
        mEquationCounts.increment(mtEqu.getSignature());
        mStructuralCounts.increment(mtEqu.term.getSignature());
      }
    }

    Set<String> setEquations = new HashSet<String>();
    Set<String> setSystems = new HashSet<String>();
    Set<String> setStructures = new HashSet<String>();
    for(Map.Entry<String,Integer> entry : mEquationCounts.entrySet()){
      if(entry.getValue() >= iMinTrainNum){
        setEquations.add(entry.getKey());
      }
    }
    for(Map.Entry<String,Integer> entry : 
          mSystemCounts.entrySet()){
      if(entry.getValue() >= iMinTrainNum){
        setSystems.add(entry.getKey());
      }
    }
    for(Map.Entry<String,Integer> entry : mStructuralCounts.entrySet()){
      if(entry.getValue() >= iMinTrainNum){
        setStructures.add(entry.getKey());
      }
    }
    for(Question question : lTestQuestions){
      if(question.lCorrectEqus == null){
        iNumQuestionsBadEqus++;
      }
      boolean bContainsOnlyOnce = false;
      boolean bContainsOnlyOnceStructure = false;
      if(!setSystems.contains(question.ct.mt.getSignature())){
        iNumSystemsSeenOnlyOnce++;
      }
      for(MappedTerm mtEqu : question.ct.mt.term.lSubTerms){
        iNumEquationInstances++;
        if(!setEquations.contains(mtEqu.getSignature())){
          bContainsOnlyOnce = true;
          iNumEquationsSeenOnlyOnce++;
          boolean bSingleSentence = false;
          if(mtEqu.mapNumbers.numInstances() == 0){
            iNumEquationsSeenOnlyOnceNoNumbers++;
          }
        }
        if(!setStructures.contains(mtEqu.term.getSignature())){
          bContainsOnlyOnceStructure = true;
          iNumStructuresSeenOnlyOnce++;
        }
      }
      if(bContainsOnlyOnce){
        iNumQuestionsWithEquationsSeenOnlyOnce++;
      } else {
        if(question.lCorrectEqus == null){
          iNumQuestionsBadEquWithoutUnique++;
          System.out.println("BadQuestion: " + question.toFullString());
        }
      }
      if(bContainsOnlyOnceStructure){
        iNumQuestionsWithStructuresSeenOnlyOnce++;
      }
    }
    System.out.println("NumQuestions: " + iNumQuestions);
    System.out.println("NumQuestionsBadEqus: " + iNumQuestionsBadEqus);
    System.out.println("NumQuestionsBadEqusWithoutUnique: " + 
                       iNumQuestionsBadEquWithoutUnique);
    System.out.println("NumQuestionsWithUniqueEquations: " + 
                       iNumQuestionsWithEquationsSeenOnlyOnce);
    System.out.println("NumQuestionsWithUniqueStructures: " + 
                       iNumQuestionsWithStructuresSeenOnlyOnce);
    //System.out.println("NumQuestionsWithUniqueEqu:" + iNumEquUnique);
    //System.out.println("NumQuestionsWithUniqueEquSubset:" 
    //+iNumEquUniqueSubset);
    System.out.println("NumEquationInstances: " + iNumEquationInstances);
    System.out.println("NumUniqueEquationInstances: " + 
                       iNumEquationsSeenOnlyOnce);
    System.out.println("NumUniqueEquationInstancesNoNumbers: " + 
                       iNumEquationsSeenOnlyOnceNoNumbers);
    System.out.println("NumUniqueEquationInstancesSingleSentence: " + 
                       iNumEquationsSeenOnlyOnceSingleSentence);
    System.out.println("NumUniqueStructureInstances: " + 
                       iNumStructuresSeenOnlyOnce);
    System.out.println("NumUniqueSystems: " + 
                       iNumSystemsSeenOnlyOnce);
    
  }



  static void printUniqueEquations(){
    Model.load();
    CountMap<Equation> mEquationCounts = new CountMap<Equation>();
    for(Question question : Model.lAllQuestions){
      for(Equation equation : question.system.lSingleEquations){
        mEquationCounts.increment(equation);
      }
    }
    Set<Equation> setEquations = new HashSet<Equation>();
    for(Map.Entry<Equation,Integer> entry : mEquationCounts.entrySet()){
      if(entry.getValue() > 1){
        setEquations.add(entry.getKey());
      }
    }
    StringBuilder sb = new StringBuilder();
    //now find all the equations which are not in this set
    for(Question question : Model.lAllQuestions){
      for(ConcreteEquation concrete : question.system.lConcreteEquations){
        if(!setEquations.contains(concrete.equation)){
          if(concrete.lNumbers.size() == 0){
            sb.append("0****************************************\n")
              .append("NO NUMBERS\n")
              .append(concrete.equation)
              .append("\n");
          } else {
            //find which sentence (if any) contains all the necessary numbers
            List<String> lSentences = new ArrayList<String>();
            for(StanfordSentence sentence : question.doc.lSentences){
              if(sentence.lDoubles.containsAll(concrete.lNumbers)){
                lSentences.add(sentence.sSentenceOrig);
              }
            }
            sb.append("**********************************-")
              .append(question.iIndex).append("\n");
            if(lSentences.size() != 0){
              sb.append("SENTENCES\n");
              //print out all the sentences
              for(String sSentence : lSentences){
                sb.append("**Sentence: ").append(sSentence).append("\n");
              }

            } else {
              sb.append("QUESTION\n");
              sb.append(question.sQuestion).append("\n");
            }
            sb.append(concrete.sConcrete).append("\n");
          }
        }
      }
    }
    try{
      FileUtils.writeStringToFile(new File("single.out"), sb.toString());
    }catch(IOException ex){
      throw new RuntimeException(ex);
    }
    System.out.println("NumEquations: " + setEquations.size());
  }

  static void loadToEqu(){
    Model.load();
    int iNumTotal = 0;
    int iNumBad = 0;
    int iNumBadQuestions = 0;
    StringBuilder sb = new StringBuilder();
    int iMaxSlots = 0;
    for(Question question : Model.lTestQuestions){
      boolean bBadQuestion = false;
      for(ConcreteEquation equation : question.system.lConcreteEquations){
        iNumTotal++;
        try{
          ConcreteEqu concrete = new ConcreteEqu(equation);
          iMaxSlots = Math.max(iMaxSlots, concrete.numSlots());
        }catch(ConcreteEqu.BadEquationException ex){
          System.out.println("BadQuestion: " + question.sQuestion);
          System.out.println(ex.getMessage() + " EQ: " + equation.sConcrete);
          iNumBad++;
          bBadQuestion= true;
        }
      }
      if(bBadQuestion){
        question.toFullStringBuilder(sb, false);
        iNumBadQuestions++;
      }
    }
    System.out.println("Equations Num Bad: " + iNumBad + " out of: " 
                       + iNumTotal);
    System.out.println("Question Num Bad: " + iNumBadQuestions + " out of: " 
                       + Model.lTestQuestions.size());
    try{
      FileUtils.writeStringToFile(new File("bad.out"), sb.toString());
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }
  }

  public static void numUniqueEquations(){
    Model.load();
    CountMap<Equation> mEquationCounts = new CountMap<Equation>();
    for(Question question : Model.lAllQuestions){
      for(Equation equation : question.system.lSingleEquations){
        mEquationCounts.increment(equation);
      }
    }
    int iNumWith1 = 0;

    for(Question question : Model.lAllQuestions){
      for(Equation equation : question.system.lSingleEquations){
        if(mEquationCounts.get(equation) == 1){
          iNumWith1++;
          break;
        }
      }
    }
    System.out.println("NumWith1: " + iNumWith1 + " out of: " 
                       + Model.lAllQuestions.size());
  }

  static void printCoinQuestions(){
    Config.load();
    //let's read in all the questions
    List<Question> lQuestions = Question.loadAllValid();
    List<String> lKeys = Arrays.asList("penn", "nickel", "dime","quarter",
                                       "coin");
    for(Question question : lQuestions){
      if(question.system == null){
        continue;
      }
      for(String sKey : lKeys){
        if(question.sQuestion.contains(sKey)){
          System.out.println(question.iIndex);
          break;
        }
      }
    }
  }

  static void calcTypeStats(){
    Config.load();
    //let's read in all the questions
    List<Question> lQuestions = Question.loadAllValid();
    Map<String,Map<EquationSystem,List<Integer>>> mSystems =
      new HashMap<String,Map<EquationSystem,List<Integer>>>();
    for(Question question : lQuestions){
      if((question.config.sType != null) && 
         question.config.sType.contains("coin-standard")){
        Map<EquationSystem,List<Integer>> mCur = 
          mSystems.get(question.config.sType);
        if(mCur == null){
          mCur = new HashMap<EquationSystem,List<Integer>>();
          mSystems.put(question.config.sType, mCur);
        }
        List<Integer> lCur = mCur.get(question.system.system);
        if(lCur == null){
          lCur = new ArrayList<Integer>();
          mCur.put(question.system.system, lCur);
        }
        lCur.add(question.iIndex);
      }
    }
    //now print them out
    for(Map.Entry<String,Map<EquationSystem,List<Integer>>> entry : 
          mSystems.entrySet()){
      System.out.println("************Type: " + entry.getKey());
      for(Map.Entry<EquationSystem,List<Integer>> entryInner : 
            entry.getValue().entrySet()){
        System.out.println("System-" + entryInner.getValue().size() + ": " +
                           entryInner.getKey().toString());
        System.out.println("   " + entryInner.getValue());
      }
    }
  }

  static void calcNumWithTypes(){
    Config.load();
    //let's read in all the questions
    List<Question> lQuestions = Question.loadAllValid();
    int iNumTypes = 0;
    int iNumQuestions = 0;
    for(Question question : lQuestions){
      if(question.system != null){
        iNumQuestions++;
        if(question.config.sType != null){
          iNumTypes++;
        }
      }
    }
    System.out.println("NumQuestions: " + iNumQuestions + " WithTypes: " +
                       iNumTypes);
  }

  static void calcSystemCounts(){
    Config.load();
    //let's read in all the questions
    List<Question> lQuestions = Question.loadAllValidWithEquations();
    CountMap<EquationSystem> mSystems = new CountMap<EquationSystem>();
    for(Question question : lQuestions){
      if(question.config.sType != null){
        continue;
      }
      mSystems.increment(question.system.system);
    }
    List<Map.Entry<EquationSystem,Integer>> lCounts = 
      new ArrayList<Map.Entry<EquationSystem,Integer>>(mSystems.entrySet());
    Comparator<Map.Entry<EquationSystem,Integer>> comparator = 
      new Misc.MapEntryValueComparator<EquationSystem,Integer>();
    //sort largest first
    Collections.sort(lCounts, Collections.reverseOrder(comparator));
    //now print out the top 10
    for(int iIndex = 0; iIndex < 50; iIndex++){
      Map.Entry<EquationSystem,Integer> entry = lCounts.get(iIndex);
      System.out.println("System-" + entry.getValue() + 
                         ": " + entry.getKey().toFeatureString());
    }
  }

  static void labelSubmissionTypes(){
    Config.load();
    //let's read in all the questions
    List<QuestionText> lQuestionTexts = 
      QuestionText.readListFromJson(Config.config.sFullQuestionFile);
    List<Question> lQuestions =
      Question.loadFromFileNew(Config.config.sFullQuestionFile, true);
    Misc.Assert(lQuestionTexts.size() == lQuestions.size());
    for(int iQuestion = 0; iQuestion < lQuestions.size(); iQuestion++){
      Question question = lQuestions.get(iQuestion);
      if((question.system == null) || !question.config.isValid()){
        continue;
      }
      question.loadStanfordParse();
      QuestionText questiontext = lQuestionTexts.get(iQuestion);
      String sSystem = question.system.system.toFeatureString();
      if(sSystem.equals("m+n=a:::(b*m)+(c*n)=d:::")){
        if(questiontext.sType != null){
          System.out.println("BadType: " + questiontext.sType + " Ind: " 
                             + question.iIndex);
          Misc.Assert(questiontext.sType == null);
        }
        questiontext.sType = "submission-a";
      } else if(sSystem.equals("(a*m)+(b*n)=c:::(d*m)+(e*n)=f:::")){
        Misc.Assert(questiontext.sType == null);
        questiontext.sType = "submission-b";
      }        
    }
    //now write the questions back out
    QuestionText.writeListToJson(lQuestionTexts, "questions.json");
  }

  static public void printSystemCounts(){
    Model.load();
    CountMap<String> mSystemCounts = new CountMap<String>();
    for(Question question : Model.lTrainQuestions){
      mSystemCounts.increment(question.ct.mt.getSignature());
    }
    CountMap<Integer> mHistogram = new CountMap<Integer>();
    for(Question question : Model.lTestQuestions){
      mHistogram.increment(mSystemCounts.get(question.ct.mt.getSignature()));
    }
    //now print the histogram as percentages
    List<Integer> lHistogram = new ArrayList<Integer>(mHistogram.keySet());
    Collections.sort(lHistogram, Collections.reverseOrder());
    for(Integer iCount : lHistogram){
      System.out.println("Hist: " + iCount + " " + mHistogram.get(iCount));
    }
  }

  static void testAllQuestions(){
    Model.load();
    List<SimpleSolver> lSolvers = Question.buildSolvers();

    int iNumFailures = 0;
    for(Question question : Model.lAllQuestions){
      String sResult = question.test(lSolvers);
      if(sResult != null){
        iNumFailures++;
      }
    }
    System.out.println("Num Failures: " + iNumFailures + " out of: " + 
                       Model.lAllQuestions.size());
  }

  static void allQuestionsStats(){
    Config.load();
    List<Question> lQuestions = Question.loadAllValidWithEquations();
    System.out.println("NumQuestions: " + lQuestions.size());
    CountMap<String> mSystemCounts = new CountMap<String>();
    for(Question question : lQuestions){
      mSystemCounts.increment(question.ct.mt.getSignature());
    }
    CountMap<Integer> mHistogram = new CountMap<Integer>();
    for(Question question : lQuestions){
      Integer iCount = mSystemCounts.get(question.ct.mt.getSignature());
      mHistogram.increment(iCount);
    }
    int[] aHistogram = new int[20];
    for(Integer iCount : mSystemCounts.values()){
      for(int iMinCount = 0; iMinCount < aHistogram.length; iMinCount++){
        if(iCount >= iMinCount){
          aHistogram[iMinCount] += iCount;
        }
      }
    }
    for(int iMinCount = 0; iMinCount < aHistogram.length; iMinCount++){
      System.out.println("Min: " + iMinCount + " Num: " +aHistogram[iMinCount]);
    }
  }

  static List<Question> getFrequentQuestions(List<Question> lQuestions, 
                                             int iMinCount){
    CountMap<String> mCounts = new CountMap<String>();
    for(Question question : lQuestions){
      mCounts.increment(question.ct.mt.getSignature());
    }
    List<Question> lPruned = new ArrayList<Question>();
    for(Question question : lQuestions){
      if(mCounts.get(question.ct.mt.getSignature()) >= iMinCount){
        lPruned.add(question);
      }
    }
    return lPruned;
  }

  static void updateHistogram(List<Question> lQuestions, int iNumFolds, 
                              int[] aHist){
    Collections.shuffle(lQuestions);
    //now split it
    List<List<Question>> llFolds = Misc.splitList(lQuestions, iNumFolds);
    for(int iTestFold = 0; iTestFold < iNumFolds; iTestFold++){
      CountMap<String> mCounts = new CountMap<String>();
      for(int iFold = 0; iFold < iNumFolds; iFold++){
        if(iFold == iTestFold){
          continue;
        }
        for(Question question : llFolds.get(iFold)){
          mCounts.increment(question.ct.mt.toString(false,true));
        }
      }
      //now look at the counts for the test fold
      for(Question question : llFolds.get(iTestFold)){
        int iCount = mCounts.get(question.ct.mt.toString(false,true));
        if(iCount >= 10){
          aHist[9]++;
        } else {
          aHist[iCount]++;
        }
      }
    }
  }

  static void histogramForMin(int iMin, int iNumFolds){
    Config.load(true);
    List<Question> lQuestions = Question.loadAllValidWithEquations();
    //first get all the data with that cut
    List<Question> lPrunedQuestions = getFrequentQuestions(lQuestions, iMin);
    int[] aHist = new int[10];
    for(int i=0; i < 30; i++){
      updateHistogram(lPrunedQuestions, iNumFolds, aHist);
    }
    System.out.println("NumQuestions: " + lPrunedQuestions.size());
    //first get the total num
    int iTotal = 0;
    for(int iCount : aHist){
      iTotal += iCount;
    }
    System.out.println("Total: " + iTotal);
    for(int iCount = 0; iCount < 10; iCount++){
      System.out.println("Count: " + iCount + " " + aHist[iCount] + " " +
                         Misc.div(aHist[iCount], iTotal));
    }
  }

  static void findSystem(){
    Config.load();
    List<Question> lQuestions = Question.loadAllValidWithEquations();
    for(Question question : lQuestions){
      //System.out.println(question.ct);
      String sSystem = "(-m*a)+(-n*b)+c = 0\n(-m*b)+(-n*a)+d = 0\n";
      if(question.ct.mt.toString().equals(sSystem)){
        System.out.println("Found it-" + question.iIndex + ":" 
                           + question.ct.mt);
      }
    }
  }
  
  static void compareStats(){
    Config.load();
    List<Question> lQuestions = Question.loadAllValidWithEquations();
    Set<String> setNonCanonical = new HashSet<String>();
    Set<String> setCanonical = new HashSet<String>();
    int iNumDiff = 0;
    int iNumOpoDiff = 0;
    int iNumEqualSig = 0;
    Set<Integer> setQuestions = new HashSet<Integer>();
    for(Question question1 : lQuestions){
      String sSig1 = question1.ct.mt.getSignature();
      String sEqu1 = question1.ct.mt.toString(true,false);
      for(Question question2 : lQuestions){
        String sSig2 = question2.ct.mt.getSignature();
        String sEqu2 = question2.ct.mt.toString(true,false);
        if(sSig1.equals(sSig2)){
          iNumEqualSig++;
          if(!sEqu1.equals(sEqu2)){
            setQuestions.add(question1.iIndex);
            setQuestions.add(question2.iIndex);
            iNumDiff++;
            System.out.println("***************************");
            System.out.println("Question1: " + question1.sQuestion);
            System.out.println("Old1:" + question1.system.system.toString());
            System.out.println("Equ1: " + sEqu1);
            System.out.println("Sig1: " + question1.ct.mt.recalcSignature());
            System.out.println("Question2: " + question2.sQuestion);
            System.out.println("Old2:" + question2.system.system.toString());
            System.out.println("Equ2: " + sEqu2);
            System.out.println("Sig2: " + question2.ct.mt.recalcSignature());
          }
        }
        if(sEqu1.equals(sEqu2)){
          if(!sSig1.equals(sSig2)){
            iNumOpoDiff++;
          }
        }
      }
    }
    System.out.println("NumOpoDiff: " + iNumOpoDiff);
    System.out.println("NumDiff: " + iNumDiff + " Out of: " + iNumEqualSig);
    System.out.println("NumQuestions: " + setQuestions.size() + " out of: " 
                       + lQuestions.size());
  }
  
  static void checkCorrect(){
    Model.load();
    Question question = Question.getQuestion(2570, Model.lAllQuestions);
    FilledTerm ft = new FilledTerm(question.ct.mt);
    ft.lUnknownWords = 
      DebugPrinter.convertToWords(Arrays.asList("test:0:6","word:2:2",
                                                "test:0:6","word:2:2"),
                                  question.doc);
    ft.lNumberWords = 
      DebugPrinter.convertToWords(Arrays.asList("3:1:6","5:2:6","110:3:11",
                                                  "30:0:8"),
                                    question.doc);
    System.out.println("Correct: " + ft.isCorrectStrict(question) + " " +
                       ft.isCorrectLoose(question.ct));
  }
  
  static void printDepPath(){
    Model.load();
    Question question = Question.getQuestion(271, Model.lAllQuestions);
    StanfordSentence.Path path = question.doc.lSentences.get(0).aPaths[2][5];
    System.out.println("Path: " + path.lStemPath);
    System.out.println("Path: " + path);
  }

  static void printAllowedUnknowns(){
    Model.load();
    Question question = Question.getQuestion(2570, Model.lAllQuestions);
    System.out.println(question.llAllowedUnknowns);
    System.out.println(question.ct);
    System.out.println(question.system);
  }

  static void generateFolds(int iMin, int iNumFolds){
    Config.load(true);
    List<Question> lQuestions = Question.loadAllValidWithEquations();
    //first get all the data with that cut
    //List<Question> lQuestions = getFrequentQuestions(lAllQuestions, iMin);
    Collections.shuffle(lQuestions);
    List<List<Question>> llFolds = Misc.splitList(lQuestions, iNumFolds);
    for(int iFold = 0; iFold < iNumFolds; iFold++){
      List<Question> lFold = llFolds.get(iFold);
      List<Integer> lIndexes = new ArrayList<Integer>();
      for(Question question : lFold){
        lIndexes.add(question.iIndex);
      }
      //now write this out to a file
      String sFileName = "data/indexes-all-fold-" + iFold + ".txt";
      try{
        FileUtils.writeLines(new File(sFileName), lIndexes);
      }catch(IOException ex){
        throw new RuntimeException(ex);
      }
    }
  }


  static void printEqCombineQuestions(){
    Model.load(null, 0, true);
    //find all questions without a system in train
    Set<String> setSystems = new HashSet<String>();
    Set<String> setEquations = new HashSet<String>();
    for(Question question : Model.lTrainQuestions){
      setSystems.add(question.ct.mt.toString(false,true));
      for(MappedTerm mt : question.ct.mt.term.lSubTerms){
        setEquations.add(mt.toString(false,true));
      }
    }
    int iNumSystem = 0;
    int iNumEq = 0;
    int iNumNeither = 0;
    int iNumTotal = Model.lTestQuestions.size();
    for(Question question : Model.lTestQuestions){
      if(!setSystems.contains(question.ct.mt.toString(false,true))){
        //check if all the equations are contains
        boolean bFoundAll = true;
        for(MappedTerm mt : question.ct.mt.term.lSubTerms){
          if(!setEquations.contains(mt.toString(false,true))){
            bFoundAll = false;
            break;
          }
        }
        if(bFoundAll){
          //printout the question
          System.out.println(question.toFullString());
          iNumEq++;
        } else {
          iNumNeither++;
        }
      } else {
        iNumSystem++;
      }
    }
    System.out.println("NumSystem: " + iNumSystem);
    System.out.println("NumEq: " + iNumEq);
    System.out.println("NumNeither: " + iNumNeither);
    System.out.println("NumTotal: " + iNumTotal);
  }


  public static void main(String[] args) throws Exception{
    //printInfrequentEquations();
    //printEquationCounts();
    //printNumEquations();
    //printMultipleUnknowns();
    //loadToEqu();
    //numUniqueEquations();
    //printUniqueEquations();
    //numUniqueEquations();
    //calcUniqueStats();
    //calcUniqueStatsTerm();
    //calcTypeStats();
    //printCoinQuestions();
    //calcNumWithTypes();
    //calcSystemCounts();
    //labelSubmissionTypes();
    //printSystemCounts();
    //testAllQuestions();
    //allQuestionsStats();
    //findSystem();
    //compareStats();
    //printOutNonOverlap();
    //checkCorrect();
    //printDepPath();
    //printAllowedUnknowns();
    //histogramForMin(7, 5);
    //generateFolds(7,10);
    printEqCombineQuestions();
  }
}