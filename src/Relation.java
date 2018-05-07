import java.lang.*;
import java.util.*;

abstract class Relation<T1, T2>{
  //exact
  static Relation<EquTerm,EquTerm> EquEquExact = new EquEquExact();
  static Relation<List<EquTerm>,List<EquTerm>> EquEquExactList = 
    new EquEquExactList();
  // //concrete
  // static Relation<EquTerm,EquTerm> EquEquNotOpposite = new EquEqu(false);
  // static Relation<EquTerm,EquTerm> EquEquOpposite = new EquEqu(true);
  // static Relation<List<EquTerm>,List<EquTerm>> EquEquListNotOpposite = 
  //   new EquEquList(false);
  // static Relation<List<EquTerm>,List<EquTerm>> EquEquListOpposite = 
  //   new EquEquList(true);

  //concreteequ
  static Relation<EquTerm,OldConcreteTerm> EquConcreteNotOpposite =
    new EquConcrete(false);
  static Relation<EquTerm,OldConcreteTerm> EquConcreteOpposite = 
    new EquConcrete(true);
  static Relation<List<EquTerm>,List<OldConcreteTerm>> EquConcreteListNotOpposite= 
    new EquConcreteList(false);
  static Relation<List<EquTerm>,List<OldConcreteTerm>> EquConcreteListOpposite = 
    new EquConcreteList(true);
  


  abstract boolean equal(T1 t1, T2 t2);
  abstract boolean isASubsetOfB(T1 t1, T2 t2);

  // static Relation<EquTerm,EquTerm> getEquEqu(boolean bOpposite){
  //   return bOpposite ? EquEquOpposite : EquEquNotOpposite;
  // }

  // static Relation<List<EquTerm>,List<EquTerm>> getEquEquList(boolean bOpposite){
  //   return bOpposite ? EquEquListOpposite : EquEquListNotOpposite;
  // }

  static Relation<EquTerm,OldConcreteTerm> getEquConcrete(boolean bOpposite){
    return bOpposite ? EquConcreteOpposite : EquConcreteNotOpposite;
  }

  static Relation<List<EquTerm>,List<OldConcreteTerm>> 
    getEquConcreteList(boolean bOpposite){
    return bOpposite ? EquConcreteListOpposite : EquConcreteListNotOpposite;
  }

  static class EquEquExact extends Relation<EquTerm,EquTerm>{
    public boolean equal(EquTerm term1, EquTerm term2){
      return term1.equals(term2);
    }
    public boolean isASubsetOfB(EquTerm term1, EquTerm term2){
      Misc.Assert(false);//not implemented
      return false;
    }
  }

  static class EquEquExactList extends Relation<List<EquTerm>,List<EquTerm>>{
    public boolean equal(List<EquTerm> term1, List<EquTerm> term2){
      return Misc.equalSets(term1, term2, EquEquExact);
    }
    public boolean isASubsetOfB(List<EquTerm> term1, List<EquTerm> term2){
      return Misc.isASubsetOfB(term1, term2, EquEquExact);
    }
  }

  // static class EquEqu extends Relation<EquTerm,EquTerm>{
  //   boolean bOpposite;
  //   EquEqu(boolean bOpposite){
  //     this.bOpposite = bOpposite;
  //   }
  //   public boolean equal(EquTerm term1, EquTerm term2){
  //     return term1.concrete.equals(term2.concrete);
  //   }
  //   public boolean isASubsetOfB(EquTerm term1, EquTerm term2){
  //     return term1.concrete.subsetOf(term2.concrete, bOpposite);
  //   }
  // }

  // static class EquEquList extends Relation<List<EquTerm>,List<EquTerm>>{
  //   Relation<EquTerm,EquTerm> relation;
  //   EquEquList(boolean bOpposite){
  //     relation = getEquEqu(bOpposite);
  //   }
  //   public boolean equal(List<EquTerm> term1, List<EquTerm> term2){
  //     return Misc.equalSets(term1, term2, this.relation);
  //   }
  //   public boolean isASubsetOfB(List<EquTerm> term1, List<EquTerm> term2){
  //     return Misc.isASubsetOfB(term1, term2, this.relation);
  //   }
  // }

  static class EquConcrete extends Relation<EquTerm,OldConcreteTerm>{
    boolean bOpposite;
    EquConcrete(boolean bOpposite){
      this.bOpposite = bOpposite;
    }
    public boolean equal(EquTerm term1, OldConcreteTerm term2){
      return term1.concrete.equals(term2, bOpposite);
    }
    public boolean isASubsetOfB(EquTerm term1, OldConcreteTerm term2){
      return term1.concrete.subsetOf(term2, bOpposite);
    }
  }

  static class EquConcreteList 
    extends Relation<List<EquTerm>,List<OldConcreteTerm>>{
    Relation<EquTerm,OldConcreteTerm> relation;
    EquConcreteList(boolean bOpposite){
      relation = getEquConcrete(bOpposite);
    }
    public boolean equal(List<EquTerm> term1, List<OldConcreteTerm> term2){
      return Misc.equalSets(term1, term2, this.relation);
    }
    public boolean isASubsetOfB(List<EquTerm> term1, List<OldConcreteTerm> term2){
      return Misc.isASubsetOfB(term1, term2, this.relation);
    }
  }
  


}