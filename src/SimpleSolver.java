import java.lang.*;
import java.util.*;

import org.ejml.data.*;
import org.ejml.ops.*;
import org.ejml.simple.*;
import com.google.common.collect.*; 

class SimpleSolver{
  int iSize;
  double[] A;
  double[] x;
  double[] b;
  DenseMatrix64F matrixA;
  DenseMatrix64F matrixx;
  DenseMatrix64F matrixb;

  SimpleSolver(int iSize){
    this.iSize = iSize;
    A = new double[iSize*iSize];
    x = new double[iSize];
    b = new double[iSize];
    matrixA = DenseMatrix64F.wrap(iSize,iSize,A);
    matrixx = DenseMatrix64F.wrap(iSize,1,x);
    matrixb = DenseMatrix64F.wrap(iSize,1,b);
  }

  void addConstraint(double[] aConstraint, int iPos){
    Misc.Assert(aConstraint.length == iSize+1);
    b[iPos] = aConstraint[aConstraint.length-1];
    //copy the data into A
    System.arraycopy(aConstraint, 0, A, iPos*iSize, (aConstraint.length-1));
  }

  boolean solve(){
    boolean bValid = CommonOps.solve(matrixA, matrixb, matrixx);
    for(int iVar = 0; iVar < x.length; iVar++){
      double fVar = x[iVar];
      bValid &= !Double.isNaN(fVar) && !Double.isInfinite(fVar);
    }
    return bValid;
  }


  boolean hasAllNonNegAnswer(){
    for(int iAnswer = 0; iAnswer < b.length; iAnswer++){
      if(b[iAnswer] < 0){
        return false;
      }
    }
    return true;
  }

}