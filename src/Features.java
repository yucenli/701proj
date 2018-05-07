import java.lang.*;
import java.util.*;
import java.util.concurrent.*;

import com.google.common.collect.*;
import org.apache.commons.lang3.tuple.*;
import gnu.trove.list.*;
import gnu.trove.list.array.*;

class Features{
  private Map<Object, Integer> mFeatureToIndex = 
    new ConcurrentHashMap<Object,Integer>();
  private Map<Integer, Object> mIndexToFeature = 
    new ConcurrentHashMap<Integer,Object>();
  DerivationFeatures derivationfeatures = new DerivationFeatures(this);
  SystemFeatures systemfeatures = new SystemFeatures(this);

  Map<Object,Double> mFudgeFactors;

  LogLinear loglinear;
  Integer iCurFeature = 0;
  int iMaxNumFeatures = Config.config.iMaxNumFeatures;

  public synchronized Integer addFeature(Object oFeature, double fInitWeight,
                                         double fFudgeFactor){
    //check one more time if the feature exists
    Integer iFeature = mFeatureToIndex.get(oFeature);
    if(iFeature != null){
      return iFeature;
    }
    //ok it's definitely not there
    iFeature = iCurFeature;
    loglinear.addFeature(iFeature, fInitWeight, fFudgeFactor);
    mFeatureToIndex.put(oFeature, iFeature);
    mIndexToFeature.put(iFeature, oFeature);
    iCurFeature++;
    Misc.Assert(iCurFeature < iMaxNumFeatures);
    return iFeature;
  }

  public Object getFeature(int iFeature){
    Misc.Assert(iFeature < iCurFeature);
    return mIndexToFeature.get(iFeature);
  }

  public Integer getFeatureIndex(Object oFeature){
    return getFeatureIndex(oFeature, Config.config.fDefaultFeatureWeight,
                           null);
  }

  public Integer getFeatureIndexFudgeFactor(Object oFeature, 
                                            Object oFudgeFactor){
    return getFeatureIndex(oFeature, Config.config.fDefaultFeatureWeight,
                           oFudgeFactor);
  }

  public Integer getFeatureIndex(Object oFeature, double fInitScore, 
                                 Object oFudgeFactor){
    Integer iFeature = mFeatureToIndex.get(oFeature);
    if(iFeature == null){
      Double fFudgeFactor = 1.0;
      if(Config.config.bUseFudgeFactors && (oFudgeFactor != null)){
        fFudgeFactor = mFudgeFactors.get(oFudgeFactor);
      }
      if(fFudgeFactor == null){
        fFudgeFactor = 1.0;
      }
      iFeature = addFeature(oFeature, fInitScore, fFudgeFactor);
    }
    return iFeature;
  }

  void addFeature(TIntList lFeatures, Object... aFeature){
    ImmutableList<Object> oFeature=ImmutableList.builder().add(aFeature).build();
    int iFeature = getFeatureIndex(oFeature);
    lFeatures.add(iFeature);
  }

  
  void addFeature(FeatureSet featureset, Object... aFeature){
    addFeature(1.0, featureset, aFeature);
  }
  void addFeature(double fScore, FeatureSet featureset, Object... aFeature){
    ImmutableList<Object> oFeature=ImmutableList.builder().add(aFeature).build();
    int iFeature = getFeatureIndex(oFeature);
    featureset.updateFeature(iFeature, fScore);
  }

  void addDoubleCrossFeatures(FeatureSet featureset, List<Object> lFeaturesA,
                              List<Object> lFeaturesB){
    for(Object oFeatureA : lFeaturesA){
      for(Object oFeatureB : lFeaturesB){
        addFeature(featureset, oFeatureA, oFeatureB);
      }
    }
  }

  void addCrossFeatures(FeatureSet featureset, Object oFeatureA, 
                        List<Object> lFeaturesB){
    for(Object oFeatureB : lFeaturesB){
      addFeature(featureset, oFeatureA, oFeatureB);
    }
  }

  void addCrossFeatures(TIntList lFeatures, Object oFeatureA, 
                        List<Object> lFeaturesB){
    for(Object oFeatureB : lFeaturesB){
      addFeature(lFeatures, oFeatureA, oFeatureB);
    }
  }

  int[] crossFeaturesAsArray(Object oFeatureA, List<Object> lFeaturesB){
    int[] aFeatures = new int[lFeaturesB.size()];
    for(int iFeature = 0; iFeature < lFeaturesB.size(); iFeature++){
      Object oFeatureB = lFeaturesB.get(iFeature);
      ImmutableList<Object> oFeature = 
        ImmutableList.builder().add(oFeatureA,oFeatureB).build();
      aFeatures[iFeature] = getFeatureIndexFudgeFactor(oFeature, oFeatureB);
    }
    return aFeatures;
  }

  int[] doubleCrossFeaturesAsArray(List<Object> lFeatures1, 
                                   List<Object> lFeatures2){
    int iSize1 = lFeatures1.size();
    int iSize2 = lFeatures2.size();
    int[] aFeatures = new int[iSize1*iSize2];
    for(int iFeature1 = 0; iFeature1 < iSize1; iFeature1++){
      Object oFeature1 = lFeatures1.get(iFeature1);
      for(int iFeature2 = 0; iFeature2 < iSize2; iFeature2++){
        Object oFeature2 = lFeatures2.get(iFeature2);
        ImmutableList<Object> oFeature = 
          ImmutableList.builder().add(oFeature1,oFeature2).build();
        int iFeatureIndex = getFeatureIndexFudgeFactor(oFeature, oFeature1);
        aFeatures[iSize2*iFeature1 + iFeature2] = iFeatureIndex;
      }
    }
    return aFeatures;
  }

  void addCrossFeatures(double fScore, FeatureSet featureset, Object oFeatureA, 
                        List<Object> lFeaturesB){
    for(Object oFeatureB : lFeaturesB){
      addFeature(fScore, featureset, oFeatureA, oFeatureB);
    }
  }

  void addFeatures(FeatureSet featureset, List<Object> lFeatures){
    for(Object oFeature : lFeatures){
      addFeature(featureset, oFeature);
    }
  }

}
