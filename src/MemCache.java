import java.lang.*;
import java.util.*;
import java.util.concurrent.*;

import com.google.common.collect.*;

class MemCache{
  private Map<Object,Object> mCache =new ConcurrentHashMap<Object,Object>();
  
  synchronized Object addObject(Object obj){
    //double check
    Object oFromCache = mCache.get(obj);
    if(oFromCache == null){
      oFromCache = obj;
      mCache.put(obj,obj);
    }
    return oFromCache;
  }

  Object get(Object obj){
    Object oFromCache = mCache.get(obj);
    if(oFromCache == null){
      return addObject(obj);
    } else {
      return oFromCache;
    }
  }
  
  // Object get(Object... aObjs){
  //   ImmutableList<Object> lObjs = ImmutableList.builder().add(aObjs).build();
  //   return get(lObjs);
  // }

  Object get(Object... aObjs){
    List<Object> lObjs = Arrays.asList(aObjs);
    return get(lObjs);
  }



}