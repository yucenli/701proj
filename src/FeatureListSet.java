import java.lang.*;
import java.util.*;

class FeatureListSet{
  Map<FeatureList,Double> mFeatureLists = new HashMap<FeatureList,Double>();

  void add(FeatureListList featurelistlist){
    double fScore = featurelistlist.scoreMaybeLog();
    for(FeatureList featurelist : featurelistlist.lFeatureLists){
      Double fCur = mFeatureLists.get(featurelist);
      if(fCur == null){
        mFeatureLists.put(featurelist, fScore);
      } else {
        mFeatureLists.put(featurelist, Misc.addMaybeLog(fScore, fCur));
      }
    }
  }

  void addFeatureListSet(FeatureListSet flsOther){
    for(Map.Entry<FeatureList,Double> entry : flsOther.mFeatureLists.entrySet()){
      FeatureList featurelist = entry.getKey();
      Double fScore = entry.getValue();
      Double fCur = mFeatureLists.get(featurelist);
      if(fCur == null){
        mFeatureLists.put(featurelist, fScore);
      } else {
        mFeatureLists.put(featurelist, Misc.addMaybeLog(fScore, fCur));
      }
    }
  }
}