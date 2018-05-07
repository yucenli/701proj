import java.lang.*;
import java.util.*;

import org.w3c.dom.*;

class StanfordMention{
  int iSentence;
  int iStart;
  int iEnd;
  int iHead;
  StanfordSentence sentence;
  StanfordWord word;
  
  StanfordMention(Node node, List<StanfordSentence> lSentences){
    iSentence = Misc.getTextAttributeAsInt(node, "sentence")-1;
    iStart = Misc.getTextAttributeAsInt(node, "start");
    iEnd = Misc.getTextAttributeAsInt(node, "end");
    iHead = Misc.getTextAttributeAsInt(node, "head");
    sentence = lSentences.get(iSentence);
    word = sentence.lWordsOrig.get(iHead);
  }
}