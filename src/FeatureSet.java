import java.lang.*;
import java.util.*;

//import gnu.trove.map.*;
//import gnu.trove.map.hash.*;
import org.apache.commons.lang3.tuple.*;

class FeatureSet{
  boolean bLocked = false;
  boolean bLog;
  Map<Integer,Double> mFeatures = new HashMap<Integer,Double>();
  //TIntDoubleMap mFeatures = new TIntDoubleHashMap();

  FeatureSet(boolean bLog){
    this.bLog = bLog;
  }

  FeatureSet(FeatureSet featuresetToCopy){
    this.bLog = featuresetToCopy.bLog;
    this.mFeatures = new HashMap<Integer,Double>(featuresetToCopy.mFeatures);
  }

  void lock(){
    this.bLocked = true;
  }

  void addFeature(int iFeature){
    Misc.Assert(!bLocked);
    Misc.Assert(!bLog);
    Misc.Assert(mFeatures.get(iFeature) == null);
    mFeatures.put(iFeature, 1.0);
  }
  void addFeature(int iFeature, double fScore){
    Misc.Assert(!bLocked);
    Misc.Assert(!bLog);
    Misc.Assert(mFeatures.get(iFeature) == null);
    mFeatures.put(iFeature, fScore);
  }

  void updateFeature(int iFeature, double fScoreToAdd){
    Misc.Assert(!bLocked);
    if(this.bLog){
      fScoreToAdd = Math.log(fScoreToAdd);
    }
    Double fCurScore = mFeatures.get(iFeature);
    if(fCurScore == null){
      mFeatures.put(iFeature, fScoreToAdd);
    } else {
      double fNewScore = Misc.addMaybeLog(fCurScore, fScoreToAdd, this.bLog);
      mFeatures.put(iFeature, fNewScore);
    }
  }

  void setFeature(int iFeature, double fScore){
    Misc.Assert(!bLocked);
    Misc.Assert(!bLog);
    Misc.Assert(mFeatures.get(iFeature) != null);
    mFeatures.put(iFeature, fScore);
  }

  void addFeatureSet(FeatureSet fsNew){
    addFeatureSetWithMultiplierMaybeLog(fsNew, Misc.oneMaybeLog(fsNew.bLog),
                                        fsNew.bLog);
  }
  


  void addFeatureSetWithMultiplierMaybeLog(FeatureSet fsNew, double fMultiplier,
                                           boolean bLogMultiplier){
    addFeatureSetWithMultiplierMaybeLog(fsNew, fMultiplier,bLogMultiplier,true);
  }

  void addFeatureSetWithMultiplierMaybeLog(FeatureSet fsNew, double fMultiplier,
                                           boolean bLogMultiplier, 
                                           boolean bAllowUpdates){
    Misc.Assert(bLogMultiplier == fsNew.bLog);
    Misc.Assert(!bLocked);
    if(this.bLog){
      if(fsNew.bLog){
        //both are log so just logadd
        for(Map.Entry<Integer,Double> entry : fsNew.mFeatures.entrySet()){
          Double fCur = mFeatures.get(entry.getKey());
          Misc.Assert(!Misc.isBadDouble(entry.getValue(), true));
          //both log so add to multiply
          double fMult = entry.getValue()+fMultiplier;
          if(fCur == null){
            mFeatures.put(entry.getKey(), fMult);
          } else {
            Misc.Assert(bAllowUpdates);
            Misc.Assert(!Misc.isBadDouble(fCur, true));
            Double fNew = Misc.logAdd(fCur,fMult);
            Misc.Assert(!Misc.isBadDouble(fNew, true));
            mFeatures.put(entry.getKey(), fNew);
          }
        }
      } else {
        Misc.Assert(fMultiplier == Misc.oneMaybeLog(bLogMultiplier));
        //this is log other is not so must upgrade it to log
        for(Map.Entry<Integer,Double> entry : fsNew.mFeatures.entrySet()){
          Double fCur = mFeatures.get(entry.getKey());
          Double fNew = Math.log(entry.getValue());
          Misc.Assert(!Misc.isBadDouble(entry.getValue(), true));
          Misc.Assert(!Misc.isBadDouble(fNew, true));
          if(fCur == null){
            mFeatures.put(entry.getKey(), fNew);
          } else {
            Misc.Assert(bAllowUpdates);
            Misc.Assert(!Misc.isBadDouble(fCur, true));
            mFeatures.put(entry.getKey(), Misc.logAdd(fCur,fNew));
          }
        }
      }
    } else {
      Misc.Assert(fMultiplier == Misc.oneMaybeLog(bLogMultiplier));
      //other must not be log
      Misc.Assert(!fsNew.bLog);
      for(Map.Entry<Integer,Double> entry : fsNew.mFeatures.entrySet()){
        Double fCur = mFeatures.get(entry.getKey());
        Misc.Assert(!Misc.isBadDouble(entry.getValue(),false));
        if(fCur == null){
          mFeatures.put(entry.getKey(), entry.getValue());
        } else {
          Misc.Assert(bAllowUpdates);
          Misc.Assert(!Misc.isBadDouble(fCur, false));
          Double fNew = fCur + entry.getValue();
          Misc.Assert(!Misc.isBadDouble(fNew, false));
          mFeatures.put(entry.getKey(), fNew);
        }
      }
    }
  }

  void multiplyByMaybeLog(double fFactor, boolean bFactorIsLog){
    Misc.Assert(!bLocked);
    Misc.Assert(!Misc.isBadDouble(fFactor, bFactorIsLog));
    if(this.bLog && !bFactorIsLog){
      fFactor = Math.log(fFactor);
    } else if(!this.bLog && bFactorIsLog){
      fFactor = Math.exp(fFactor);
    }
    Misc.Assert(!Misc.isBadDouble(fFactor, this.bLog));
    for(Map.Entry<Integer,Double> entry : this.mFeatures.entrySet()){
      double fNewVal = Misc.multMaybeLog(entry.getValue(),fFactor, this.bLog);
      if(Misc.isBadDouble(fNewVal, this.bLog)){
        System.out.println("BadMultiply: " + entry.getValue() + " " + fFactor);
        Misc.Assert(!Misc.isBadDouble(fNewVal, this.bLog));
      }
      mFeatures.put(entry.getKey(),fNewVal);
    }
  }


  void divideByMaybeLog(double fDivisor, boolean bDivisorIsLog){
    Misc.Assert(!bLocked);
    Misc.Assert(this.bLog == bDivisorIsLog);
    for(Map.Entry<Integer,Double> entry : this.mFeatures.entrySet()){
      double fNewVal = Misc.divMaybeLog(entry.getValue(),fDivisor, this.bLog);
      if(Misc.isBadDouble(fNewVal, this.bLog)){
        System.out.println("Bad Divide: " + entry.getValue() + " " + fDivisor
                           + " " + fNewVal + " log: " + this.bLog);
        Misc.Assert(!Misc.isBadDouble(fNewVal, this.bLog));
      }
      mFeatures.put(entry.getKey(),fNewVal);
    }
  }

  void expMaybeLog(){
    Misc.Assert(!bLocked);
    Misc.Assert(this.bLog == Config.config.bLogScores);
    if(!this.bLog){
      return;
    }
    for(Map.Entry<Integer,Double> entry : this.mFeatures.entrySet()){
      double fNewVal = Math.exp(entry.getValue());
      if(Misc.isBadDouble(fNewVal, false)){
        System.out.println("Bad Exponentiate: " + entry.getValue() + " " +
                           fNewVal);
        Misc.Assert(!Misc.isBadDouble(fNewVal, false));
      }
      mFeatures.put(entry.getKey(),fNewVal);
    }
    this.bLog = false;
  }

  void maybeLog(){
    Misc.Assert(!bLocked);
    Misc.Assert(!this.bLog);
    if(!Config.config.bLogScores){
      return;
    }
    for(Map.Entry<Integer,Double> entry : this.mFeatures.entrySet()){
      double fNewVal = Math.log(entry.getValue());
      if(Misc.isBadDouble(fNewVal, false)){
        System.out.println("Bad Log: " + entry.getValue() + " " +
                           fNewVal);
        Misc.Assert(!Misc.isBadDouble(fNewVal, false));
      }
      mFeatures.put(entry.getKey(),fNewVal);
    }
    this.bLog = true;
  }

  public String toString(){
    List<Map.Entry<Integer,Double>> lEntries = 
      new ArrayList<Map.Entry<Integer,Double>>(this.mFeatures.entrySet());
    Comparator<Map.Entry<Integer,Double>> comparator = 
      new Misc.MapEntryValueComparator<Integer,Double>();
    Collections.sort(lEntries, Collections.reverseOrder(comparator));
    StringBuilder sb = new StringBuilder();
    for(Map.Entry<Integer,Double> entry : lEntries){
      Object oFeature = Model.features.getFeature(entry.getKey());
      sb.append("\nFeature: ").append(oFeature).append(" --> ")
        .append(entry.getValue());
    }
    return sb.toString();
  }

  public String toString(boolean bSortByWeight){
    if(!bSortByWeight){
      return toString();
    }
    List<Triple<Integer,Double,Double>> lTriples = 
      new ArrayList<Triple<Integer,Double,Double>>();
    
    for(Map.Entry<Integer,Double> entry : this.mFeatures.entrySet()){
      int iFeature = entry.getKey();
      double fWeight = entry.getValue();
      double fScore = Model.loglinear.aWeights[iFeature]*fWeight;
      lTriples.add(Triple.of(iFeature, fWeight, fScore));
    }
    Comparator<Triple<Integer,Double,Double>> comparator = 
      Collections.reverseOrder(new Misc.TripleRightComparator());
    Collections.sort(lTriples, comparator);
    StringBuilder sb = new StringBuilder();
    for(Triple<Integer,Double,Double> triple : lTriples){
      int iFeature = triple.getLeft();
      Object oFeature = Model.features.getFeature(iFeature);
      double fWeight = Model.loglinear.aWeights[iFeature];
      sb.append("\nFeature-").append(iFeature).append(": ").append(oFeature)
        .append(" --> ")
        .append(fWeight).append(" * ").append(triple.getMiddle()).append(" = ")
        .append(triple.getRight());
    }
    return sb.toString();
  }

  void addFeatureListSet(FeatureListSet fls){
    //all featurelistset scores are same logness as Config.config.bLogScores
    Misc.Assert(bLog == Config.config.bLogScores);
    for(Map.Entry<FeatureList,Double> entry : fls.mFeatureLists.entrySet()){
      FeatureList featurelist = entry.getKey();
      Double fScore = entry.getValue();
      for(int iIndex = 0; iIndex < featurelist.aFeatures.length; iIndex++){
        int iFeature = featurelist.aFeatures[iIndex];
        double fFudgeFactor = Model.loglinear.getFudgeFactor(iFeature);
        fScore = fScore*fFudgeFactor;
        Double fCur = this.mFeatures.get(iFeature);
        if(fCur == null){
          mFeatures.put(iFeature, fScore);
        } else {
          mFeatures.put(iFeature, Misc.addMaybeLog(fCur, fScore));
        }
      }
    }
  }


  double[] flatten(int iMaxNumFeatures){
    double[] aOut = new double[iMaxNumFeatures];
    for(Map.Entry<Integer,Double> entry : mFeatures.entrySet()){
      aOut[entry.getKey()] = entry.getValue();
    }
    return aOut;
  }

}  
