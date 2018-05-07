import java.lang.*;
import java.util.*;

class EquationLoc{
  // the location of interest is the last node in the list
  List<EquationNode> lAncestors;
  
  EquationLoc(List<EquationNode> lAncestors){
    this.lAncestors = lAncestors;
  }

  EquationNode getNode(){
    return lAncestors.get(lAncestors.size()-1);
  }

  static boolean areAncestors(EquationLoc loc1, EquationLoc loc2){
    int iMinLen = Math.min(loc1.lAncestors.size(), loc2.lAncestors.size());
    for(int iNode = 0; iNode < iMinLen; iNode++){
      if(loc1.lAncestors.get(iNode) != loc2.lAncestors.get(iNode)){
        return false;
      }
    }
    // one is a sublist of the other
    return true;
  }

}