import java.util.*;
import java.lang.*;
import java.util.concurrent.*;

class FeatureCache{
  Map<Object,FeatureList> mFeatureListCache = 
    new ConcurrentHashMap<Object,FeatureList>();
  Map<Object,List<Object>> mFeatureCache = 
    new ConcurrentHashMap<Object,List<Object>>();
}