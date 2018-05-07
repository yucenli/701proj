import java.lang.*;
import java.util.*;

class SlotCounter implements EquationNodeVisitor{
  int[] aUnknownCounts;
  int[] aNumberCounts;
  int iConstantCount = 0;
  SlotCounter(int iNumUnknowns, int iNumNumbers){
      this.aUnknownCounts = new int[iNumUnknowns];
      this.aNumberCounts = new int[iNumNumbers];
  }
  public void visit(Equation equation, List<String> lOps){};
  public void visit(EquationTerm.Complex equation, List<String> lOps){};
  public void visit(EquationTerm.Number number, List<String> lOps){
    this.aNumberCounts[number.iIndex]++;
  };
  public void visit(EquationTerm.Unknown unknown, List<String> lOps){
    this.aUnknownCounts[unknown.iIndex]++;
  };
  public void visit(EquationTerm.Constant constant, List<String> lOps){
    iConstantCount++;
  };

  static boolean hasGreaterThanOne(int[] array){
    for(int iIndex = 0; iIndex < array.length; iIndex++){
      int iCount = array[iIndex];
      Misc.Assert(iCount > 0);
      if(iCount > 1){
        return true;
      }
    }
    return false;
  }


  static boolean hasMultiUnknowns(Equation equation){
    SlotCounter counter = new SlotCounter(equation.iNumUnknowns, 
                                          equation.iNumNumbers);
    equation.accept(counter, new ArrayList<String>());
    return hasGreaterThanOne(counter.aUnknownCounts);
  }

  static boolean hasMultiSlots(Equation equation){
    SlotCounter counter = new SlotCounter(equation.iNumUnknowns, 
                                          equation.iNumNumbers);
    equation.accept(counter, new ArrayList<String>());
    return hasGreaterThanOne(counter.aUnknownCounts) ||
      hasGreaterThanOne(counter.aNumberCounts);
  }

  static boolean hasMultiSlotsOrConstants(Equation equation){
    SlotCounter counter = new SlotCounter(equation.iNumUnknowns, 
                                          equation.iNumNumbers);
    equation.accept(counter, new ArrayList<String>());
    return hasGreaterThanOne(counter.aUnknownCounts) ||
      hasGreaterThanOne(counter.aNumberCounts) || (counter.iConstantCount > 0);
  }

}
