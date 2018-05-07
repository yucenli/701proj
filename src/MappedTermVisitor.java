import java.lang.*;
import java.util.*;

interface MappedTermVisitor{
  public void visit(MappedTerm mt, List<String> lOps);
}