import java.lang.*;
import java.util.*;

import org.w3c.dom.*;

class StanfordCorefChain{
  List<StanfordMention> lMentions = new ArrayList<StanfordMention>();
  StanfordCorefChain(Node node, List<StanfordSentence> lSentences){
    List<Node> lMentionNodes = Misc.getAllChildrenByName(node, "mention");
    for(Node nodeMention : lMentionNodes){
      lMentions.add(new StanfordMention(nodeMention, lSentences));
    }
  }

}