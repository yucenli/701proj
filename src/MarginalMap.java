import java.lang.*;
import java.util.*;

class MarginalMap<K> extends HashMap<K,Double>{
  boolean bLog;

  MarginalMap(boolean bLog){
    this.bLog = bLog;
  }

  public void add(K k, Double fVal){
    this.put(k, Misc.addMaybeLog(get(k), fVal, this.bLog));
  }

  public Double get(Object k){
    Double fCur = (Double) super.get(k);
    if(fCur == null){
      fCur = Misc.zeroMaybeLog(this.bLog);
      this.put((K)k,fCur);
    }
    return fCur;
  }
}