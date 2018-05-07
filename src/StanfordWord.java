import java.lang.*;
import java.util.*;

import org.w3c.dom.*;

class StanfordWord{
  String sWord;
  String sWordOrig;
  String sWordOrigCase;
  String sWordPreSplit;
  List<StanfordDep> lDeps = new ArrayList<StanfordDep>();
  String sPos;
  String sNer;
  int iIndex;
  StanfordSentence sentence;
  StanfordDep depParent = null;
  StanfordParseTree ptParent = null;
  StanfordNumber number = null; // if this word is a number word
  static Set<String> setAdjPosTags = 
    new HashSet<String>(Arrays.asList("JJ","JJR","JJS","PRP$"));
  static Set<String> setNounPosTags = 
    new HashSet<String>(Arrays.asList("NN","NNS","NNP","NNPS","PRP","WP"));
  static Set<String> setVerbPosTags = 
    new HashSet<String>(Arrays.asList("VB","VBD","VBG","VBN","VBP","VBZ"));
  List<StanfordDep> lSimpleDeps;

  StanfordWord(StanfordSentence sentence){
    // ROOT
    iIndex = 0;
    sWord = "ROOT";
    sPos = null;
    sNer = null;
    depParent = null;
    this.sentence = sentence;
  }

  StanfordWord(StanfordWord word){
    this.sWord = word.sWord;
    this.sWordOrigCase = word.sWordOrigCase;
    this.sWordPreSplit = word.sWordPreSplit;
    this.sWordOrig = word.sWordOrig;
    this.sPos = word.sPos;
    this.sNer = word.sNer;
    this.number = word.number;
    this.sentence = word.sentence;
  }

  StanfordWord(Node node, int iIndex, StanfordSentence sentence){
    this.sentence = sentence;
    Misc.Assert(node.getNodeType() == Node.ELEMENT_NODE);
    Misc.Assert(node.getNodeName().equals("token"));
    Misc.Assert(Integer.parseInt(Misc.getAttribute(node, "id")) == iIndex);
    this.iIndex = iIndex;
    sWordOrigCase = Misc.getTextAttribute(node, "word");
    sWordPreSplit = sWordOrigCase;
    Misc.Assert(sWordOrigCase != null);
    sWordOrig = sWordOrigCase.toLowerCase();
    sWord = Misc.getTextAttribute(node, "lemma");
    sPos = Misc.getTextAttribute(node, "POS");
    sNer = Misc.getTextAttribute(node, "NER");
  }
  
  void genSimpleDeps(){
    if(lSimpleDeps != null){
      return;
    }
    lSimpleDeps = new ArrayList<StanfordDep>();
    for(StanfordDep dep : lDeps){
      if(dep.sType.equals("amod") || dep.sType.equals("nn")){
        lSimpleDeps.add(dep);
      }
    }
  }

  boolean isRootParent(){
    return (depParent == null);
  }

  boolean isRoot(){
    if(depParent == null){
      System.out.println("Testing root: " + this.toFullString());
      for(StanfordDep dep : lDeps){
        System.out.println("  Child: " + dep.wordChild.toFullString());
      }
    }
    Misc.Assert(depParent != null);
    return depParent.sType.equals("root");
  }

  StanfordWord getParent(){
    Misc.Assert(!this.isRoot());
    return this.depParent.wordParent;
  }


  boolean isNoun(){
    if(setNounPosTags.contains(this.sPos)){
      return true;
    }
    if(this.isRootParent()){
      return false;
    }
    if(this.isRoot()){
      return false;
    }
    if(this.ptParent == null){
      return false;
    }
    if(this.ptParent.ptParent == null){
      return false;
    }
    if((this.number != null) && (this.number.fNumber != 1.0)){
      return false;
    }
    StanfordParseTree ptNonTerm = this.ptParent.ptParent;
    if((this.sPos.equals("CD") || this.sPos.equals("DT"))){
      if((ptNonTerm.sNonTerminal.equals("NN") ||
          ptNonTerm.sNonTerminal.equals("NP")) && 
         (this.ptParent.ptParent.lChildren.size() == 1)){
        return true;
      }
    }
    if(this.sPos.equals("JJ")){
      if(ptNonTerm.sNonTerminal.equals("NP")){
        if((ptNonTerm.lChildren.size() == 2)){
          //check that the first child is a dt
          StanfordParseTree ptOther = ptNonTerm.lChildren.get(0);
          if(ptOther.word != null){
            if(ptOther.word.sPos.equals("DT")){
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  boolean isAdj(){
    return setAdjPosTags.contains(this.sPos);
  }

  boolean isPrep(){
    return this.sPos.equals("IN");
  }

  boolean isPossessive(){
    return this.sPos.equals("POS");
  }

  boolean isVerb(){
    return setVerbPosTags.contains(this.sPos);
  }

  StanfordWord getSubj(boolean bAllowNull){
    StanfordWord wordSubj =findChildByDep(StanfordDep.setSubjLabels,bAllowNull);
    //Misc.Assert((wordSubj == null) || wordSubj.isNoun() || 
    //            wordSubj.sPos.equals("CD"));
    return wordSubj;
  }

  StanfordWord getDobj(boolean bAllowNull){
    Misc.Assert(this.isVerb());
    StanfordWord wordDobj =findChildByDep(StanfordDep.setDobjLabels,bAllowNull);
    Misc.Assert((wordDobj == null) || wordDobj.isNoun());
    return wordDobj;
  }

  List<StanfordWord> getPrepObjs(){
    List<StanfordWord> lPreps = findChildrenByDep("prep");
    List<StanfordWord> lPobjs = new ArrayList<StanfordWord>();
    //now find all the objs
    for(StanfordWord wordPrep : lPreps){
      StanfordWord wordPobj = wordPrep.findChildByDep("pobj", true);
      if(wordPobj != null){
        lPobjs.add(wordPobj);
      }
    }
    return lPobjs;
  }


  StanfordWord findChildByDep(String sDep, boolean bAllowNull){
    return findChildByDep(new HashSet<String>(Arrays.asList(sDep)), bAllowNull);
  }

  StanfordWord findChildByDep(Set<String> setTypes, boolean bAllowNull){
    StanfordWord wordToFind = null;
    for(StanfordDep dep : this.lDeps){
      if(setTypes.contains(dep.sType)){
        Misc.Assert(wordToFind == null);
        wordToFind = dep.wordChild;
      }
    }
    if(!bAllowNull && !(wordToFind != null)){
      this.sentence.writeToLatexFile();
      Misc.Assert(bAllowNull || (wordToFind != null));
    }
    return wordToFind;
  }

  List<StanfordWord> findChildrenByDep(String sType){
    List<StanfordWord> lFound = new ArrayList<StanfordWord>();
    for(StanfordDep dep : this.lDeps){
      if(sType.equals(dep.sType)){
        lFound.add(dep.wordChild);
      }
    }
    return lFound;
  }
  


  boolean hasDecendants(List<String> lWords){
    for(StanfordDep dep : lDeps){
      if(dep.wordChild.sWord.equals(lWords.get(0))){
        if((lWords.size() == 1) || 
           dep.wordChild.hasDecendants(lWords.subList(1, lWords.size()))){
          return true;
        }
      }
    }
    return false;
  }

  boolean hasAncestors(List<String> lWords){
    if(depParent.wordParent.sWord.equals(lWords.get(0))){
      if((lWords.size() == 1) || 
         depParent.wordParent.hasAncestors(lWords.subList(1, lWords.size()))){
        return true;
      }
    }
    return false;
  }

  static Comparator<StanfordWord> comparatorStanfordWordIndex =
    new StanfordWordIndexComparator();

  static class StanfordWordIndexComparator implements Comparator<StanfordWord>{
    public int compare(StanfordWord word1, StanfordWord word2){
      Misc.Assert(word1.iIndex != word2.iIndex);
      return word1.iIndex - word2.iIndex;
    }
  }

  List<StanfordWord> getSubTree(){
    List<StanfordWord> lSubTree = new ArrayList<StanfordWord>();
    //subtree includes myself
    lSubTree.add(this);
    //and the subtrees of all my children
    for(StanfordDep dep : lDeps){
      if(!Arrays.asList("conj","cc").contains(dep.sType)){
        lSubTree.addAll(dep.wordChild.getSubTree());
      }
    }
    //now sort them by index
    Collections.sort(lSubTree, comparatorStanfordWordIndex);
    //make sure they're contiguous -- they're not becauase of commas
    // int iPrev = lSubTree.get(0).iIndex-1;
    // for(StanfordWord word : lSubTree){
    //   if(word.iIndex != iPrev+1){
    //     System.out.println("Bad List: " + lSubTree);
    //     Misc.Assert(word.iIndex == iPrev+1);
    //   }
    //   iPrev = word.iIndex;
    // }
    return lSubTree;
  }

  boolean equalSimpleTree(StanfordWord wordOther, boolean bStem){
    if((bStem && !this.sWord.equals(wordOther.sWord)) ||
       (!bStem && !this.sWordOrig.equals(wordOther.sWordOrig))){
      return false;
    }
    this.genSimpleDeps();
    wordOther.genSimpleDeps();
    if(this.lSimpleDeps.size() != wordOther.lSimpleDeps.size()){
      return false;
    }
    for(int iDep = 0; iDep < this.lSimpleDeps.size(); iDep++){
      StanfordDep depThis = this.lSimpleDeps.get(iDep);
      StanfordDep depOther = wordOther.lSimpleDeps.get(iDep);
      //if the the subtrees (including the edge labels) are not equivalent
      // and in equivalent order then they are not equal (for now)
      if(!depThis.sType.equals(depOther.sType) || 
         !depThis.wordChild.equalSimpleTree(depOther.wordChild, bStem)){
        return false;
      }
    }
    //everything checks out, so return true
    return true;
  }

  // StringBuilder addConstituent(StringBuilder sb){
  //   boolean bAddedSelf = false;
  //   for(StanfordDep dep : lDeps){
  //     if(!bAddedSelf && (dep.wordChild.iIndex > this.iIndex)){
  //       sb.append(" ").append(this.sWordOrig);
  //     }
  //     dep.wordChild.addConstituent(sb);
  //   }
  //   if(!bAddedSelf){
  //     sb.append(" ").append(this.sWordOrig);
  //   }
  //   return sb;
  // }


  // String getConstituent(){
  //   return addConstituent(new StringBuilder()).toString();
  // }

  public String toString(){
    return toFullString();
  }

  public String toFullString(){
    return new StringBuilder().append(sWordOrig).append(":").
      append(sentence.iIndex).append(":").append(this.iIndex).toString();
  }


  static boolean equalStem(StanfordWord word1, StanfordWord word2){
    return (word1.sWord != null) && (word2.sWord != null) &&
      word1.sWord.equals(word2.sWord);
  }

  static boolean equalOrig(StanfordWord word1, StanfordWord word2){
    return (word1.sWordOrig != null) && (word2.sWordOrig != null) &&
      word1.sWordOrig.equals(word2.sWordOrig);
  }

  static String toStringWithIndexes(List<StanfordWord> lWords){
    return toStringBuilderWithIndexes(new StringBuilder(),lWords).toString();
  }


  static StringBuilder toStringBuilderWithIndexes(StringBuilder sb,
                                                  List<StanfordWord> lWords){
    sb.append("[");
    for(StanfordWord word : lWords){
      sb.append(word.toFullString()).append(", ");
    }
    sb.append("]");
    return sb;
  }


  static List<String> getStemList(List<StanfordWord> lWords){
    return getStemList(lWords, false);
  }
  static List<String> getStemList(List<StanfordWord> lWords, 
                                  boolean bOnlyAdjAndNoun){
    List<String> lStems = new ArrayList<String>();
    for(StanfordWord word : lWords){
      if(!bOnlyAdjAndNoun || word.isNoun() || word.isAdj()){
        lStems.add(word.sWord);
      }
    }
    return lStems;
  }


}