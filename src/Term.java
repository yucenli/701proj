import java.lang.*;
import java.util.*;
import com.google.common.base.Joiner;

import com.google.common.primitives.*;

class Term{
  enum Type{Complex,Unknown,Number,Constant};
  enum Op{PLUS,TIMES,DIVIDEBY,SYSTEM;
      public String toString(){
      if(this == TIMES){
        return "*";
      } else if(this == PLUS){
        return "+";
      } else if(this == DIVIDEBY){
        return "/";
      } else {
        // should never try to print a "system" op
        Misc.Assert(false);
        return null;
      }
    }
  };
  //this is only for anything, but represents minus in the equation, not in
  // the number pulled from the text.  For constants it's fine either way
  Type type;
  boolean bMinus;
  //for constant
  double fConstant;
  //for complex
  List<MappedTerm> lSubTerms;
  Op op;
  int iNumUnknowns = -1;
  int iNumNumbers = -1;

  String sSignature;

  int iNumUses = -1;
  int iOriginalIndex = -1;
  int iOriginalOriginalIndex = -1;
  String sPathSig;
  int iUniqueId = -1;
  
  Term(){
  }


  String recalcSignature(){
    StringBuilder sb = new StringBuilder();
    if(bMinus){
      sb.append("-");
    }
    if(type == Type.Complex){
      sb.append("(");
      boolean bFirst = true;
      for(MappedTerm mt : lSubTerms){
        if(!bFirst){
          if(this.op == Op.SYSTEM){
            sb.append(":");
          } else {
            sb.append(this.op);
          }
        }
        bFirst = false;
        sb.append(mt.term.recalcSignature());
      }
      sb.append(")");
    } else if(type == Type.Unknown){
      sb.append("?").append(":").append(iNumUses);
      if(iNumUses > 1){
        sb.append(sPathSig);
        //if(iUniqueId != -1){
          sb.append(":").append(iUniqueId);
          //}
      }
    } else if(type == Type.Number){
      sb.append("#").append(":").append(iNumUses);
      if(iNumUses > 1){
        sb.append(sPathSig);
        //if(iUniqueId != -1){
          sb.append(":").append(iUniqueId);
          //}
      }
    } else if(type == Type.Constant){
      sb.append(Double.toString(fConstant));
    } else {
      Misc.Assert(false);
    }
    return sb.toString();
  }

  String calcConcreteSignature(List<Double> lNumberDoubles, int iStart){
    StringBuilder sb = new StringBuilder();
    if(bMinus){
      sb.append("-");
    }
    if(type == Type.Complex){
      sb.append("(");
      List<String> lSubSigs = new ArrayList<String>();
      for(MappedTerm mt : lSubTerms){
        lSubSigs.add(mt.term.calcConcreteSignature(lNumberDoubles, iStart));
        iStart+= mt.mapNumbers.numInstances();
      }
      //now sort the subsigs
      Collections.sort(lSubSigs);
      if(this.op == Op.SYSTEM){
        Joiner.on(":").appendTo(sb, lSubSigs);
      } else {
        Joiner.on(this.op.toString()).appendTo(sb, lSubSigs);
      }
      sb.append(")");
    } else if(type == Type.Unknown){
      sb.append("?");
    } else if(type == Type.Number){
      sb.append(lNumberDoubles.get(iStart));
    } else if(type == Type.Constant){
      sb.append(Double.toString(fConstant));
    } else {
      Misc.Assert(false);
    }
    return sb.toString();
  }

  String getSignature(){
    if(sSignature == null){
      sSignature = recalcSignature();
    }
    return sSignature;
  }

  StringBuilder toStringBuilder(StringBuilder sb, List<String> lUnknowns, 
                                List<String> lNumbers, boolean bIncludeExtras,
                                boolean bFeatureString){
    if(bMinus){ 
      sb.append("-");
    }
    if(type == Type.Complex){
      int iCurUnknown = 0;
      int iCurNumber = 0;
      if(this.op == Op.SYSTEM){
        for(MappedTerm mt : lSubTerms){
          List<String> lCurUnknowns = 
            lUnknowns.subList(iCurUnknown, iCurUnknown + mt.term.iNumUnknowns);
          List<String> lCurNumbers = 
            lNumbers.subList(iCurNumber, iCurNumber + mt.term.iNumNumbers);
          mt.term.toStringBuilder(sb, lCurUnknowns, lCurNumbers,
                                  bIncludeExtras, bFeatureString);

          iCurUnknown += mt.term.iNumUnknowns;
          iCurNumber += mt.term.iNumNumbers;

          if(bFeatureString){
            sb.append("::");
          } else {
            sb.append(" = 0\n");
          }
        }
      } else {
        boolean bFirst = true;
        for(MappedTerm mt : lSubTerms){
          if(!bFirst){
            sb.append(op);
          }
          bFirst = false;
          if(mt.term.type == Type.Complex){
            sb.append("(");
          }
          List<String> lCurUnknowns = 
            lUnknowns.subList(iCurUnknown, iCurUnknown + mt.term.iNumUnknowns);
          List<String> lCurNumbers = 
            lNumbers.subList(iCurNumber, iCurNumber + mt.term.iNumNumbers);
          mt.term.toStringBuilder(sb, lCurUnknowns, lCurNumbers, bIncludeExtras,
                                  bFeatureString);

          iCurUnknown += mt.term.iNumUnknowns;
          iCurNumber += mt.term.iNumNumbers;
          
          if(mt.term.type == Type.Complex){
            sb.append(")");
          }
        }
      }
    } else if(type == Type.Unknown){
      Misc.Assert(lUnknowns.size() == 1);
      Misc.Assert(lNumbers.size() == 0);
      sb.append(lUnknowns.get(0));
      if(bIncludeExtras){
        sb.append(":").append(iNumUses);
        if(iNumUses > 1){
          sb.append(sPathSig);
          if(iUniqueId != -1){
            sb.append(":").append(iUniqueId);
          }
        }
      }
    } else if(type == Type.Number){
      Misc.Assert(iNumUnknowns == 0);
      Misc.Assert(lUnknowns.size() == 0);
      Misc.Assert(lNumbers.size() == 1);
      sb.append(lNumbers.get(0));
      if(bIncludeExtras){
        sb.append(":").append(iNumUses);
        if(iNumUses > 1){
          sb.append(sPathSig);
          if(iUniqueId != -1){
            sb.append(":").append(iUniqueId);
          }
        }
      }
    } else if(type == Type.Constant){
      Misc.Assert(lUnknowns.size() == 0);
      Misc.Assert(lNumbers.size() == 0);
      sb.append(Double.toString(fConstant));
    } else {
      Misc.Assert(false);
    }
    return sb;
  }

  List<Double> solve(SimpleSolver solver, List<Double> lNumbers,
                     int iNumUnknowns){
    Misc.Assert(this.op == Op.SYSTEM);
    for(int iEquation = 0; iEquation < this.lSubTerms.size(); iEquation++){
      Term termEquation = this.lSubTerms.get(iEquation).term;
      double[] aConstraint = new double[iNumUnknowns+1];
      termEquation.toConstraint(aConstraint, lNumbers, 1.0);
      //flip the sign of the constant term because of the way we're calcing now
      aConstraint[aConstraint.length-1] = -aConstraint[aConstraint.length-1];
      solver.addConstraint(aConstraint, iEquation);
    }
    boolean bValid = solver.solve();
    if(!bValid){
      return null;
    }
    return new ArrayList<Double>(Doubles.asList(solver.x));
  }


  public Double calcValue(List<Double> lNumbers){
    if(this.type == Type.Unknown){
      return null;
    } else if(this.type == Type.Number){
      Misc.Assert(iUniqueId < lNumbers.size());
      if(this.bMinus){
        return -lNumbers.get(iUniqueId);
      } else {
        return lNumbers.get(iUniqueId);
      }
    } else if(this.type == Type.Constant){
      Misc.Assert(!this.bMinus);
      return fConstant;
    }
    Misc.Assert(this.type == Type.Complex);
    Misc.Assert(this.lSubTerms.size() != 0);
    if(this.op == op.PLUS){
      double fResult = 0.0;
      for(MappedTerm mt : this.lSubTerms){
        Double fVal = mt.term.calcValue(lNumbers);
        if(fVal == null){
          return null;
        } else {
          fResult += fVal;
        }
      }
      if(bMinus){
        return -fResult;
      } else {
        return fResult;
      }
    } else if(this.op == op.TIMES){
      double fResult = 1.0;
      for(MappedTerm mt : this.lSubTerms){
        Double fVal = mt.term.calcValue(lNumbers);
        if(fVal == null){
          return null;
        } else {
          fResult *= fVal;
        }
      }
      if(bMinus){
        return -fResult;
      } else {
        return fResult;
      }
    } else if(this.op == op.DIVIDEBY){
      Misc.Assert(this.lSubTerms.size() == 2);
      Double fNumerator = this.lSubTerms.get(0).term.calcValue(lNumbers);
      Double fDenominator = this.lSubTerms.get(1).term.calcValue(lNumbers);
      Misc.Assert(fDenominator != null);
      if(fNumerator == null){
        return null;
      }
      if(bMinus){
        return -fNumerator/fDenominator;
      } else {
        return fNumerator/fDenominator;
      }
    } else {
      Misc.Assert(false);
      return null;
    }
  }  

  public void toConstraint(double[] aConstraint, List<Double> lNumbers,
                              double fMultiplier){
    Misc.Assert(this.op != Op.SYSTEM);
    if(this.bMinus){
      fMultiplier = -fMultiplier;
    }
    if(this.type == Type.Unknown){
      Misc.Assert(iUniqueId < aConstraint.length-1);
      aConstraint[iUniqueId] += fMultiplier;
      return;
    } else if(this.type == Type.Number){
      Misc.Assert(iUniqueId < lNumbers.size());
      aConstraint[aConstraint.length-1] +=fMultiplier*lNumbers.get(iUniqueId);
      return;
    } else if(this.type == Type.Constant){
      aConstraint[aConstraint.length-1] += fMultiplier*fConstant;
      return;
    }
    Misc.Assert(this.type == Type.Complex);
    if(this.op == Op.PLUS){
      for(MappedTerm mt : this.lSubTerms){
        mt.term.toConstraint(aConstraint, lNumbers, fMultiplier);
      }
    } else if(this.op == Op.TIMES){ 
      Term termUnknown = null;
      for(MappedTerm mt : this.lSubTerms){
        Double fVal = mt.term.calcValue(lNumbers);
        if(fVal == null){
          Misc.Assert(termUnknown == null);
          termUnknown = mt.term;
        } else {
          fMultiplier *= fVal;
        }
      }
      if(termUnknown == null){
        aConstraint[aConstraint.length-1] += fMultiplier;
      } else {
        termUnknown.toConstraint(aConstraint, lNumbers, fMultiplier);
      }
    } else if(this.op == Op.DIVIDEBY){
      Misc.Assert(this.lSubTerms.size() == 2);
      Double fDenominator = 
        this.lSubTerms.get(1).term.calcValue(lNumbers);
      Misc.Assert(fDenominator != null);
      fMultiplier = fMultiplier/fDenominator;
      this.lSubTerms.get(0).term.toConstraint(aConstraint,lNumbers,fMultiplier);
    } else {
      Misc.Assert(false);
    }
  }



  public String toString(){
    List<String> lUnknowns = Misc.listOfLetters('m', iNumUnknowns);
    List<String> lNumbers = Misc.listOfLetters('a', iNumNumbers);
    return toStringBuilder(new StringBuilder(), lUnknowns, lNumbers,
                           false, false).toString();
  }

}