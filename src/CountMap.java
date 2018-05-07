import java.lang.*;
import java.util.*;

class CountMap<K> extends HashMap<K,Integer>{
  public void increment(K k){
    this.put(k, get(k)+1);
  }

  public void incrementAll(List<K> list){
    for(K k : list){
      increment(k);
    }
  }

  public Integer get(Object k){
    Integer iCur = (Integer) super.get(k);
    if(iCur == null){
      iCur = 0;
      this.put((K)k,iCur);
    }
    return iCur;
  }
}