import java.lang.*;
import java.util.*;

interface EquationNodeVisitor{
  public void visit(Equation equation, List<String> lOps);
  public void visit(EquationTerm.Complex equation, List<String> lOps);
  public void visit(EquationTerm.Number number, List<String> lOps);
  public void visit(EquationTerm.Unknown unknown, List<String> lOps);
  public void visit(EquationTerm.Constant constant, List<String> lOps);
}