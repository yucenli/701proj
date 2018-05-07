import java.lang.*;
import java.util.*;

class IntArrayList{
  List<int[]> lIntArrays = new ArrayList<int[]>();
  void add(int[] array){
    if(array.length != 0){
      lIntArrays.add(array);
    }
  }
  
  class Iterator implements IntIterator{
    int iArray = 0;
    int iIndex = 0;
    //we'll cache the list lookup
    int[] aCur = null;
    Iterator(){
      if(lIntArrays.size() != 0){
        this.aCur = lIntArrays.get(0);
      }
    }


    public boolean hasNext(){
      return aCur != null;
    }

    public int next(){
      int iRetVal = aCur[iIndex];
      iIndex++;
      if(iIndex == aCur.length){
        iArray++;
        if(iArray == lIntArrays.size()){
          aCur = null;
        } else {
          aCur = lIntArrays.get(iArray);
          iIndex = 0;
        }
      }
      return iRetVal;
    }
  }

  IntIterator iterator(){
    return new Iterator();
  }

}