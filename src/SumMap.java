import java.lang.*;
import java.util.*;

class SumMap<K> extends HashMap<K,Double>{
  boolean bLog;
  SumMap(boolean bLog){
    this.bLog = bLog;
  }

  public void increment(K k, double fIncrement){
    this.put(k, Misc.addMaybeLog(get(k), fIncrement, this.bLog));
  }

  public Double get(Object k){
    Double fCur = (Double) super.get(k);
    if(fCur == null){
      fCur = 0.0;
      this.put((K)k,fCur);
    }
    return fCur;
  }

  List<K> getBest(){
    Misc.Assert(this.size() != 0);
    List<Map.Entry<K,Double>> lEntries = 
      new ArrayList<Map.Entry<K,Double>>(this.entrySet());
    Comparator<Map.Entry<K,Double>> comparator = 
      Collections.reverseOrder(new Misc.MapEntryValueComparator<K,Double>());
    //higest first (due to reverse order)
    Collections.sort(lEntries, comparator);
    Double fHighest = lEntries.get(0).getValue();
    List<K> lBest = new ArrayList<K>();
    for(Map.Entry<K,Double> entry : lEntries){
      if(entry.getValue().equals(fHighest)){
        lBest.add(entry.getKey());
      } else {
        break;
      }
    }
    return lBest;
  }
}