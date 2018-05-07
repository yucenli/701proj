import java.lang.*;
import java.util.*;

abstract class Choosable<T>{
  List<T> lAll;
  int iSize;
  boolean bAllowDuplicates;

  Choosable(List<T> lAll, int iSize){
    this(lAll, iSize, false);
  }

  Choosable(List<T> lAll, int iSize, boolean bAllowDuplicates){
    this.lAll = lAll;
    this.iSize = iSize;
    this.bAllowDuplicates = bAllowDuplicates;
  }

  abstract void choose(List<T> list);

  void chooseAll(){
    chooseAll(new ArrayList<T>(iSize));
  }

  void chooseAll(List<T> lCur){
    if(lCur.size() == iSize){
      this.choose(new ArrayList<T>(lCur));
      return;
    }
    for(T tCur : lAll){
      if(!bAllowDuplicates && lCur.contains(tCur)){
        continue;
      }
      lCur.add(tCur);
      chooseAll(lCur);
      lCur.remove(lCur.size()-1);
    }
  }
}