import java.lang.*;
import java.util.*;

class DefaultMap<K,V> extends HashMap<K,V>{
  private Class type;
  DefaultMap(Class type){
    this.type = type;
  }
  public V get(Object k){
    V vCur = (V) super.get(k);
    if(vCur == null){
      try {
        vCur = (V) type.newInstance();
      } catch(Exception ex){
        ex.printStackTrace();
        System.out.println("Instantiation Exception: "  + ex);
        System.exit(-1);
      }
      this.put((K) k, vCur);
    }
    return vCur;
  }
}