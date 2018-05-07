import java.lang.*;
import java.util.*;

abstract class EquationNode{
  EquationNode nodeParent = null;

  abstract void getAllLocs(List<EquationLoc> lLocs, 
                           List<EquationNode> lAncestors);
  abstract void toFeatureStringBuilder(StringBuilder sb, 
                                       Set<EquationNode> setArgs,
                                       List<Integer> lUnknowns, 
                                       List<Integer> lNumbers);
  abstract public void accept(EquationNodeVisitor visitor, List<String> lOps);

  String toFeatureString(Set<EquationNode> setArgs){
    StringBuilder sb = new StringBuilder();
    List<Integer> lUnknowns = new ArrayList<Integer>();
    List<Integer> lNumbers = new ArrayList<Integer>();
    this.toFeatureStringBuilder(sb, setArgs, lUnknowns, lNumbers);
    return sb.toString();
  }

  //abstract List<EquationNode> getChildren();

  void setParent(EquationNode node){
    this.nodeParent = node;
  }

  void setAllParents(EquationNode parent){
    //this.setParent(parent);
    //for(EquationNode nodeChild : getChildren()){
    //  nodeChild.setAllParents(this);
    //}
  }
  
}