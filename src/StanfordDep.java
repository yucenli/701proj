import java.lang.*;
import java.util.*;

import org.w3c.dom.*;

class StanfordDep{
  Node nodeParent;
  Node nodeChild;
  String sType;
  int iParent;
  int iChild;
  StanfordWord wordParent;
  StanfordWord wordChild;
  static Set<String> setSubjLabels = 
    new HashSet<String>(Arrays.asList("nsubj","nsubjpass"));
  static Set<String> setDobjLabels = 
    new HashSet<String>(Arrays.asList("dobj"));


  StanfordDep(StanfordWord wordParent, StanfordWord wordChild, String sType){
    this.wordParent = wordParent;
    this.wordChild = wordChild;
    this.sType = sType;
    this.iParent = -1;
    this.iChild = -1;
    wordChild.depParent = this;
    wordParent.lDeps.add(this);
  }

  StanfordDep(Node node, List<StanfordWord> lWords){
    Misc.Assert(node.getNodeType() == Node.ELEMENT_NODE);
    Misc.Assert(node.getNodeName().equals("dep"));
    this.sType = Misc.getAttribute(node, "type");
    Node nodeGovernor = Misc.getFirstChildByName(node, "governor");
    Node nodeDependent = Misc.getFirstChildByName(node, "dependent");
    iParent = Integer.parseInt(Misc.getAttribute(nodeGovernor, "idx"));
    iChild = Integer.parseInt(Misc.getAttribute(nodeDependent, "idx"));
    wordParent = lWords.get(iParent);
    wordChild = lWords.get(iChild);
    wordParent.lDeps.add(this);
    wordChild.depParent = this;
  }
  
  boolean isSubj(){
    return setSubjLabels.contains(this.sType);
  }
}