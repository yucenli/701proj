import java.lang.*;
import java.util.*;

class StanfordParseTree {
  static Set<String> setNPNonTerminals = 
    new HashSet<String>(Arrays.asList("NP","NX","QP"));
  String sNonTerminal = null;
  List<StanfordParseTree> lChildren = null;
  StanfordWord word = null;
  StanfordParseTree ptParent;
  String sPhrase;

  StanfordParseTree(StanfordWord word){
    this.word = word;
    word.ptParent = this;
    this.sPhrase = word.sWord;
  }

  StanfordParseTree(String sNonTerminal, List<StanfordParseTree> lChildren){
    this.sNonTerminal = sNonTerminal;
    this.lChildren = lChildren;
    // set the children's parent
    for(StanfordParseTree ptChild : lChildren){
      ptChild.ptParent = this;
    }
    // build the phrase
    StringBuilder sbPhrase = new StringBuilder();
    sbPhrase.append(lChildren.get(0).sPhrase);
    for(StanfordParseTree ptChild : lChildren.subList(1,lChildren.size())){
      sbPhrase.append(" ").append(ptChild.sPhrase);
    }
    sPhrase = sbPhrase.toString();
  }

  boolean isTerminal(){
    return (word != null);
  }

  static List<String> tokenize(String sParse){
    List<String> lTokens = new ArrayList<String>();
    int iCurStart = 0;
    for(int iCur = 0; iCur < sParse.length(); iCur++){
      char cCur = sParse.charAt(iCur);
      switch(cCur){
      case '(':
        Misc.Assert(iCurStart == iCur);
        lTokens.add("(");
        iCurStart = iCur+1;
        break;
      case ')':
        if(iCurStart < iCur){
          String sToken = sParse.substring(iCurStart, iCur);
          Misc.Assert(!sToken.equals(" "));
          lTokens.add(sToken);
        }
        lTokens.add(")");
        iCurStart = iCur+1;
        break;
      case ' ':
        if(iCurStart < iCur){
          String sToken = sParse.substring(iCurStart, iCur);
          Misc.Assert(!sToken.equals(" "));
          lTokens.add(sToken);
        }
        iCurStart = iCur+1;
        break;
        // otherwise just keep going
      }
    }
    return lTokens;
  }

  static StanfordParseTree build(String sParse, List<StanfordWord> lWords){
    Misc.Assert(lWords != null);
    List<String> lTokens = tokenize(sParse);
    Stack<Object> stack = new Stack<Object>();
    int iWord = 1;
    for(String sCur : lTokens){
      if(sCur.equals(")")){
        StanfordParseTree pt;
        Object objLast = stack.pop();
        if(objLast instanceof String){
          // it's a terminal
          String sWord = (String) objLast;
          String sPos = (String) stack.pop();
          StanfordWord wordCur = lWords.get(iWord);
          iWord++;
          Misc.Assert(wordCur != null);
          Misc.Assert(wordCur.sWordOrigCase != null);
          if(!wordCur.sWordPreSplit.equals(sWord)){
            System.out.println("Bad Word: " + sWord + " " + 
                               wordCur.sWordOrigCase);
            System.out.println("Parse: " + sParse);
            Misc.Assert(wordCur.sWordPreSplit.equals(sWord));
          }
          if(wordCur.sWordOrig.equals(wordCur.sWordPreSplit)){
            //if it's a split word then the pos will change
            Misc.Assert(wordCur.sPos.equals(sPos));
          }
          String sParen = (String) stack.pop();
          Misc.Assert(sParen.equals("("));
          pt = new StanfordParseTree(wordCur);
        } else {
          List<StanfordParseTree> lChildren = new ArrayList<StanfordParseTree>();
          Misc.Assert(objLast instanceof StanfordParseTree);
          while(objLast instanceof StanfordParseTree){
            lChildren.add((StanfordParseTree) objLast);
            objLast = stack.pop();
          }
          //lChildren is in reverse order right now
          Collections.reverse(lChildren);
          Misc.Assert(objLast instanceof String);
          String sLabel = (String) objLast;
          String sParen = (String) stack.pop();
          Misc.Assert(sParen.equals("("));
          pt = new StanfordParseTree(new String(sLabel), lChildren);
        }
        // push the new parsetree onto the stack
        stack.push(pt);
      } else {
        stack.push(sCur);
      }
    }
    //the stack should have exactly one parsetree on it
    Misc.Assert(stack.size() == 1);
    StanfordParseTree pt = (StanfordParseTree) stack.pop();
    return pt;
  }

  boolean isValidNP(){
    Misc.Assert(this.sNonTerminal != null);
    Misc.Assert(this.word == null);
    //Misc.Assert(!this.sNonTerminal.equals("QP"));
    if(!setNPNonTerminals.contains(this.sNonTerminal)){
      return false;
    }
    // check if it has a coordinating conjunction child -- don't grep NP across these
    for(StanfordParseTree ptChild : this.lChildren){
      if(ptChild.isTerminal() && ptChild.word.sPos.equals("CC")){
        return false;
      }
    }
    return true;
  }

  StanfordParseTree findNP(){
    //walk up the tree until we find a stopping point
    StanfordParseTree ptNP = this;
    Misc.Assert(ptNP.ptParent != null);
    while(ptNP.ptParent.isValidNP()){
      ptNP = ptNP.ptParent;
      Misc.Assert(ptNP.ptParent != null);
    }
    return ptNP;
  }

  public String toString(){
    return toString(false);
  }

  public String toString(boolean bFlat){
    if(bFlat){
      return this.toStringBuilderFlat(new StringBuilder()).toString();
    } else {
      return this.toStringBuilder(new StringBuilder(), "").toString();
    }
  }

  StringBuilder toStringBuilderFlat(StringBuilder sb){
    if(word != null){
      sb.append(" (").append(word.sPos).append(" ").append(word.sWord).append(")");
    } else {
      sb.append(" (").append(sNonTerminal);
      for(StanfordParseTree ptChild : lChildren){
        ptChild.toStringBuilderFlat(sb);
      }
      sb.append(")");
    }
    return sb;
  }

  StringBuilder toStringBuilder(StringBuilder sb, String sIndent){
    if(word != null){
      sb.append("\n").append(sIndent).append(word.sPos).append(" ")
        .append(word.sWord);
    } else {
      if(!sIndent.equals("")){
        sb.append("\n");
      }
      sb.append(sIndent).append(sNonTerminal);
      for(StanfordParseTree ptChild : lChildren){
        ptChild.toStringBuilder(sb, sIndent + "  ");
      }
    }
    return sb;
  }
}