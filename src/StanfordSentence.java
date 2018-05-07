import java.lang.*;
import java.util.*;
import java.io.*;

import org.w3c.dom.*;
import org.apache.commons.io.FileUtils;

class StanfordSentence{
  StanfordDocument doc;
  String sSentence;
  String sSentenceOrig;
  int iIndex;
  List<StanfordWord> lWords;
  List<StanfordWord> lWordsOrig;
  List<StanfordDep> lDeps = new ArrayList<StanfordDep>();
  List<StanfordNumber> lNumbers = new ArrayList<StanfordNumber>();
  List<StanfordWord> lNumberWords = new ArrayList<StanfordWord>();
  List<StanfordWord> lNouns = new ArrayList<StanfordWord>();
  List<String> lWordStrings = new ArrayList<String>();
  List<String> lWordStemStrings = new ArrayList<String>();
  List<String> lPosStrings = new ArrayList<String>();
  List<Double> lDoubles = new ArrayList<Double>();
  Set<Double> setDoubles = new HashSet<Double>();
  boolean bIsQuestionSentence;

  StanfordParseTree parseTree;
  StanfordWord wordRoot;
  static Map<String,String> mWordMap;
  int[][] aDists;
  Path[][] aPaths;

  static {
    mWordMap = new HashMap<String,String>();
    mWordMap.put("-LRB-","(");
    mWordMap.put("-RRB-",")");
    mWordMap.put("-LSB-","[");
    mWordMap.put("-RSB-","]");
  }

  StanfordSentence(Node node, int iIndex, StanfordDocument doc){
    this.doc = doc;
    Misc.Assert(node.getNodeType() == Node.ELEMENT_NODE);
    Misc.Assert(node.getNodeName().equals("sentence"));
    Misc.Assert(Integer.parseInt(Misc.getAttribute(node, "id")) == iIndex+1);
    this.iIndex = iIndex;
    this.processWords(Misc.getFirstChildByName(node, "tokens"));
    this.processDeps(Misc.getFirstChildByName(node, "dependencies"));
    this.parseTree = 
      StanfordParseTree.build(Misc.getTextAttribute(node, "parse"), lWordsOrig);
    setRoot();
    buildDistMap();
    buildPaths();
    calcStringSequences();
    calcIsQuestionSentence();
  }

  void calcIsQuestionSentence(){
    StanfordWord wordLast = this.lWords.get(this.lWords.size()-1);
    boolean bIsQuestionSentence = wordLast.sWordOrig.equals("?");
    //does the sentence contain how many?
    List<String> lHowMany = Arrays.asList("How many", "How much", "how many",
                                          "how much", "How long", "how long",
                                          "How old", "how old");
    List<String> lCommand = Arrays.asList("Find","Calculate");
    boolean bContainsHowMany = 
      Misc.stringContainsAtLeastOneOf(this.sSentenceOrig, lHowMany);
    boolean bContainsCommand = 
      Misc.stringContainsAtLeastOneOf(this.sSentenceOrig, lCommand);
    boolean bContainsWP = false;
    for(StanfordWord wordCur : this.lWords){
      if(wordCur.isRootParent()){
        continue;
      }
      if(wordCur.sPos.equals("WP")){
        bContainsWP = true;
        break;
      }
    }
    this.bIsQuestionSentence =  (bIsQuestionSentence || bContainsHowMany 
                                 || bContainsCommand || bContainsWP);
  }


  void calcStringSequences(){    
    for(StanfordWord word : lWords){
      lWordStrings.add(word.sWordOrig);
      lWordStemStrings.add(word.sWord);
      lPosStrings.add(word.sPos);
    }
  }

  class Path{
    int iDist;
    List<Dir> lDirPath = new ArrayList<Dir>();
    List<String> lDepPath = new ArrayList<String>();
    List<String> lWordPath = new ArrayList<String>();
    List<String> lStemPath = new ArrayList<String>();
    List<String> lPosPath = new ArrayList<String>();
    String sWordRoot = "null";
    boolean bConj = false;
    Path(List<Dir> lDirs, List<StanfordDep> lDeps){
      Misc.Assert(lDeps.size() == lDirs.size());
      iDist = lDeps.size();
      //should be able to build everything I want from the dep path
      boolean bFoundDown = false;
      for(int iDep = 0; iDep < lDeps.size(); iDep++){
        Dir dir = lDirs.get(iDep);
        StanfordDep dep = lDeps.get(iDep);
        this.lDirPath.add(dir);
        this.lDepPath.add(dep.sType + ":" + dir);
        if(dep.sType.equals("conj")){
          bConj = true;
        }
        //if this is not the last dep in the path, take the far end of the dep
        if(iDep != lDeps.size()-1){
          StanfordWord word = (dir == Dir.UP) ? dep.wordParent : dep.wordChild;
          this.lWordPath.add(word.sWordOrig);
          this.lStemPath.add(word.sWord);
          this.lPosPath.add(word.sPos);
          if(word.isRoot()){
            this.sWordRoot = word.sWord;
          }
        }
      }
    }
    public String toString(){
      return toStringBuilder(new StringBuilder()).toString();
    }

    StringBuilder toStringBuilder(StringBuilder sb){
      sb.append("PATH:")
        .append("\n   length: ").append(this.iDist)
        .append("\n   root: ").append(this.sWordRoot)
        .append("\n   ").append(this.lDirPath)
        .append("\n   ").append(this.lDepPath)
        .append("\n   ").append(this.lWordPath)
        .append("\n   ").append(this.lStemPath)
        .append("\n   ").append(this.lPosPath).append("\n");
      return sb;
    }
  }

  void buildPaths(){
    //first initialize aPaths
    aPaths = new Path[lWords.size()][lWords.size()];

    //do a dfs to every other word
    for(StanfordWord wordCur : lWords){
      if(wordCur.isRootParent()){
        continue;
      }
      //first set the path to myself as a zero length path
      aPaths[wordCur.iIndex][wordCur.iIndex] = 
        new Path(new ArrayList<Dir>(), new ArrayList<StanfordDep>());
      //System.out.println("***Building Paths Down: " + wordCur.toFullString());
      buildPathsDown(wordCur.iIndex, wordCur, new ArrayList<Dir>(),
                     new ArrayList<StanfordDep>(), null);
      if(!wordCur.isRoot()){
        //System.out.println("***Building Paths Up: " + wordCur.toFullString());
        buildPathsUp(wordCur.iIndex, wordCur, new ArrayList<Dir>(),
                     new ArrayList<StanfordDep>());
      }
    }
  }

  void buildPathsUp(int iStartWord, StanfordWord wordCur, List<Dir> lDirs,
                    List<StanfordDep> lDeps){
    StanfordWord wordPrev = null;
    while(!wordCur.isRoot()){
      StanfordDep depParent = wordCur.depParent;
      StanfordWord wordParent = depParent.wordParent;
      lDeps.add(depParent);
      lDirs.add(Dir.UP);
      Misc.Assert(aPaths[iStartWord][wordParent.iIndex] == null);
      //System.out.println("Adding Path from: " + 
      //                   lWords.get(iStartWord).toFullString() + " to " 
      //                   + lWords.get(wordParent.iIndex).toFullString());
      aPaths[iStartWord][wordParent.iIndex] = new Path(lDirs, lDeps);
      wordPrev = wordCur;
      wordCur = wordParent;
      Misc.Assert(wordPrev != null);
      //and then build paths back down, but skip the previous word
      buildPathsDown(iStartWord, wordCur, lDirs, lDeps, wordPrev);
    }
  }

  void buildPathsDown(int iStartWord, StanfordWord wordCur, List<Dir> lDirs,
                      List<StanfordDep> lDeps, StanfordWord wordToSkip){
    for(StanfordDep depChild : wordCur.lDeps){
      StanfordWord wordChild = depChild.wordChild;
      if(wordChild == wordToSkip){
        continue;
      }
      lDeps.add(depChild);
      lDirs.add(Dir.DOWN);
      Misc.Assert(aPaths[iStartWord][wordChild.iIndex] == null);
      //System.out.println("Adding Path from: " + 
      //                   lWords.get(iStartWord).toFullString() + " to " 
      //                   + lWords.get(wordChild.iIndex).toFullString());
      aPaths[iStartWord][wordChild.iIndex] = new Path(lDirs, lDeps);
      buildPathsDown(iStartWord, wordChild, lDirs, lDeps, null);
      lDeps.remove(lDeps.size()-1);
      lDirs.remove(lDirs.size()-1);
    }
  }

  void buildDistMap(){
    aDists = new int[lWords.size()][lWords.size()];
    //start with everything set to max_value indicating no path
    for(StanfordWord word : lWords){
      Arrays.fill(aDists[word.iIndex], Integer.MAX_VALUE);
    }
    //now initialize with the one hop edges
    for(StanfordWord word : lWords){
      aDists[word.iIndex][word.iIndex] = 0;
      //if(word.isRootParent()){
      //  continue;
      //}
      //set dist of 1 to parent and all children
      if(word.depParent != null){
        Misc.Assert(word.depParent.wordParent.iIndex != word.iIndex);
        aDists[word.iIndex][word.depParent.wordParent.iIndex]=1;
        aDists[word.depParent.wordParent.iIndex][word.iIndex]=1;
      }
      for(StanfordDep depChild : word.lDeps){
        Misc.Assert(depChild.wordChild.iIndex != word.iIndex);
        aDists[word.iIndex][depChild.wordChild.iIndex] = 1;
        aDists[depChild.wordChild.iIndex][word.iIndex] = 1;
      }
    }
    //now run floyd-warshall
    for(int k = 0; k < lWords.size(); k++){
      for(int i = 0; i < lWords.size(); i++){
        for(int j = 0; j < lWords.size(); j++){
          int iDist_ik = aDists[i][k];
          int iDist_kj = aDists[k][j];
          int iDist_ij = aDists[i][j];
          int iDist_sum = iDist_ik + iDist_kj;
          if((iDist_ik != Integer.MAX_VALUE)&&(iDist_kj!=Integer.MAX_VALUE)){
            if(iDist_sum < iDist_ij){
              aDists[i][j] = iDist_sum;
            }
          }
        }
      }
    }
  }

  void setRoot(){
    for(StanfordWord word : lWords){
      if(word.sWord.equals("ROOT")){
        Misc.Assert(wordRoot == null);
        //add it's child
        Misc.Assert(word.lDeps.size() == 1);
        wordRoot = word.lDeps.get(0).wordChild;
      }
    }
    Misc.Assert(wordRoot != null);
  }


  void processWords(Node node){
    Misc.Assert(node.getNodeType() == Node.ELEMENT_NODE);
    Misc.Assert(node.getNodeName().equals("tokens"));
    lWords = new ArrayList<StanfordWord>();
    lWordsOrig = new ArrayList<StanfordWord>();
    // add the root word (index 0);
    StanfordWord wordRoot = new StanfordWord(this);
    lWords.add(wordRoot);
    lWordsOrig.add(wordRoot);
    List<Node> lTokenNodes = Misc.getAllChildrenByName(node, "token");
    StringBuilder sb = new StringBuilder();
    StringBuilder sbOrig = new StringBuilder();
    List<String> lBadRests = Arrays.asList("digit","1999","2001","2005","foot-long","23","43","time");
    List<String> lGoodRests = Arrays.asList("point","pound","year","inch",
                                            "hour","day","cent","foot","mile",
                                            "ounce","dollar","kilogram","month",
                                            "rupee","volume","liter","minute",
                                            "octane","mile-per-hour",
                                            "milliliter");
    //List<String> lBadRests = Arrays.asList("digit","1999","2001","2005","foot-long","23","43","half","time", "miles","kilograms","months","rupees","place","litre","dollars","min","place");
    for(int iWord = 0; iWord < lTokenNodes.size(); iWord++){
      StanfordWord word = new StanfordWord(lTokenNodes.get(iWord), iWord+1,
                                           this);
      lWords.add(word);
      // now let's decide if we want to split a dashed word
      if(word.sWordOrig.contains("-")){
        String sPreSplit = word.sWordOrig;
        int iDash = word.sWordOrig.indexOf('-');
        String sNumber = word.sWordOrig.substring(0, iDash);
        String sRest = word.sWordOrig.substring(iDash+1);
        Double fNumber = Misc.getNumber(sNumber);
        if((fNumber != null) && (!lBadRests.contains(sRest))){
          if(!lGoodRests.contains(sRest)){
            System.out.println("Bad Rest: " + sRest);
            Misc.Assert(lGoodRests.contains(sRest));
          }
          //need to change the current word to be rest and generate a new word
          StanfordWord wordNumber = new StanfordWord(word);
          Misc.Assert(word.sWord.equals(word.sWordOrig));
          word.sWord = sRest;
          word.sWordOrig = sRest;
          word.sWordOrigCase = sRest;
          word.sWordPreSplit = sPreSplit;
          word.sPos = "NN";
          wordNumber.sWord = sNumber;
          wordNumber.sWordOrig = sNumber;
          wordNumber.sWordOrigCase = sNumber;
          wordNumber.sPos = "CD";
          lWords.add(wordNumber);
          sb.append(" ").append(wordNumber.sWord);
          sbOrig.append(" ").append(wordNumber.sWordOrigCase);
          StanfordDep dep = new StanfordDep(word, wordNumber, "num");
        }
      }
      lWordsOrig.add(word);

      if(iWord != 0){
        sb.append(" ");
        sbOrig.append(" ");
      }
      sb.append(word.sWord);
      sbOrig.append(word.sWordOrigCase);
    }
    sSentence = sb.toString();
    sSentenceOrig = sbOrig.toString();
    //now reindex the words
    for(int iWord = 0; iWord < lWords.size(); iWord++){
      lWords.get(iWord).iIndex = iWord;
    }
  }

  void processDeps(Node node){
    Misc.Assert(node.getNodeType() == Node.ELEMENT_NODE);
    Misc.Assert(node.getNodeName().equals("dependencies"));
    Misc.Assert(Misc.getAttribute(node, "type").equals("basic-dependencies"));
    for(Node nodeDep : Misc.getAllChildrenByName(node, "dep")){
      StanfordDep dep = new StanfordDep(nodeDep, lWordsOrig);
      lDeps.add(dep);
    }
  }

  public String toString(){
    return toStringBuilder(new StringBuilder(), false).toString();
  }

  public StringBuilder toStringBuilder(StringBuilder sb, 
                                       boolean bIncludeIndexes){
    //just print the words as a string
    boolean bFirstWord = true;
    for(StanfordWord word : lWords){
      if(word.sWordOrigCase == null){
        continue;
      }
      if(!bFirstWord && !word.sPos.equals(".")){
        sb.append(" ");
      }
      bFirstWord = false;
      String sSpecialWord = mWordMap.get(word.sWordOrigCase);
      if(sSpecialWord != null){
        sb.append(sSpecialWord);
      } else {
        Misc.Assert(!word.sWordOrig.startsWith("-") || 
                    (word.sWordOrig.length() == 1) ||
                    word.sWordOrig.equals("--") || 
                    Character.isDigit(word.sWordOrig.charAt(1)),
                    "BadWord: "+word.sWordOrig + " Sent: " + sSentenceOrig);
        sb.append(word.sWordOrigCase);
        if(bIncludeIndexes){
          sb.append(":").append(word.iIndex);
        }
      }
    }
    return sb;
  }

  String getDeps(){
    StringBuilder sbWords = new StringBuilder();
    StringBuilder sbPos = new StringBuilder();
    StringBuilder sbLabels = new StringBuilder();
    StringBuilder sbDep = new StringBuilder();
    for(int iWord = 1; iWord < lWords.size(); iWord++){
      StanfordWord word = lWords.get(iWord);
      if(iWord != 1){
        sbWords.append("\t");
        sbPos.append("\t");
        sbLabels.append("\t");
        sbDep.append("\t");
      }
      sbWords.append(word.sWordOrig);
      sbPos.append(word.sPos);
      if((word.depParent != null) && (word.depParent.wordParent != null)){
        sbLabels.append(word.depParent.sType);
        sbDep.append(word.depParent.wordParent.iIndex);
      } else {
        sbLabels.append("ROOT");
        sbDep.append(0);
      }
    }
    return new StringBuilder().append(sbWords).append("\n").append(sbPos)
      .append("\n").append(sbLabels).append("\n").append(sbDep).append("\n")
      .toString();
  }

  void writeToLatexFile(){
    writeToLatexFile("draw_dep/test2.txt");
  }

  void writeToLatexFile(String sFilename){
    //
    try{
      FileUtils.writeStringToFile(new File(sFilename), this.getDeps());
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }
  }

}