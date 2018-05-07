import java.lang.*;
import java.util.*;

import gnu.trove.list.*;
import gnu.trove.list.array.*;

import org.apache.commons.lang3.tuple.*;

class LogLinear{
  double[] aWeights = new double[Config.config.iMaxNumFeatures];
  double[] aFudgeFactors;

  Features features;
  int iCurNumFeatures;

  LogLinear(){
    if(Config.config.bUseFudgeFactors){
      aFudgeFactors = new double[Config.config.iMaxNumFeatures];
      Arrays.fill(aFudgeFactors, 1.0);
    }
  }

  // double getWeight(int iFeature){
  //   if(Config.config.bUseFudgeFactors){
  //     return aWeights[iFeature]*aFudgeFactors[iFeature];
  //   } else {
  //     return aWeights[iFeature];
  //   }
  // }

  double getFudgeFactor(int iFeature){
    if(Config.config.bUseFudgeFactors){
      return aFudgeFactors[iFeature];
    } else {
      return 1.0;
    }
  }

  void setWeights(double[] aNewWeights){
    Misc.Assert(aNewWeights.length == aWeights.length);
    this.aWeights = aNewWeights;
    // double fMaxWeight = Double.NEGATIVE_INFINITY;
    // double fMinWeight = Double.POSITIVE_INFINITY;
    // for(int iFeature = 0; iFeature < aNewWeights.length; iFeature++){
    //   double fWeight = aNewWeights[iFeature];
    //   fMaxWeight = Math.max(fMaxWeight, fWeight);
    //   fMinWeight = Math.min(fMinWeight, fWeight);
    // }
  }
  
  synchronized void addFeature(int iFeature, double fInitWeight, 
                               double fFudgeFactor){
    Misc.Assert(iFeature == iCurNumFeatures);
    aWeights[iFeature] =  fInitWeight;
    if(Config.config.bUseFudgeFactors){
      aFudgeFactors[iFeature] = fFudgeFactor;
    }    
    iCurNumFeatures++;
  }

  double crossprod(FeatureSet featureset){
    Misc.Assert(featureset != null);
    Misc.Assert(!featureset.bLog);
    double fCrossProd = 0;
    for(Map.Entry<Integer,Double> entry : featureset.mFeatures.entrySet()){
      fCrossProd += aWeights[entry.getKey()]*entry.getValue();
    }
    if(Misc.isBadDouble(fCrossProd)){
      System.out.println("BadCrossProd: " + fCrossProd);
      System.out.println("Features:");
      for(Map.Entry<Integer,Double> entry : featureset.mFeatures.entrySet()){
        double fWeight = aWeights[entry.getKey()];
        double fVal = entry.getValue();
        System.out.println("  " + entry.getKey() + " --> " + fWeight*fVal + 
                           " = " + fWeight + " * " + fVal);
        System.out.println("   " + features.getFeature(entry.getKey()));
      }
      Misc.Assert(!Misc.isBadDouble(fCrossProd));
    }
    return fCrossProd;
  }
  
  double score(FeatureSet featureset){
    Misc.Assert(!featureset.bLog);
    double fCrossProd = crossprod(featureset);
    Misc.Assert(!Misc.isBadDouble(fCrossProd));
    double fScore = Math.exp(fCrossProd);
    if(Misc.isBadDouble(fScore)){
      System.out.println("Bad Score: " + fScore + " exp of: " + fCrossProd);
      System.out.println("Features:");
      for(Map.Entry<Integer,Double> entry : featureset.mFeatures.entrySet()){
        double fWeight = aWeights[entry.getKey()];
        double fVal = entry.getValue();
        System.out.println("  " + entry.getKey() + " --> " + fWeight*fVal + 
                           " = " + fWeight + " * " + fVal);
        System.out.println("   " + features.getFeature(entry.getKey()));
      }
    }
    Misc.Assert(!Misc.isBadDouble(fScore));
    return fScore;
  }

  double crossprod(int[] aFeatures){
    double fCrossProd = 0;
    for(int iIndex = 0; iIndex < aFeatures.length; iIndex++){
      double fPrev = fCrossProd;
      int iFeature = aFeatures[iIndex];
      fCrossProd += aWeights[iFeature]*getFudgeFactor(iFeature);
      if(Misc.isBadDouble(fCrossProd)){
        System.out.println("BadCrossProd: " + fCrossProd + " " + 
                           fPrev + " " + aWeights[iFeature] + 
                           " --> " + features.getFeature(aFeatures[iIndex]));
        Misc.Assert(!Misc.isBadDouble(fCrossProd));
      }
    }
    if(Misc.isBadDouble(fCrossProd)){
      System.out.println("BadCrossProd: " + fCrossProd);
      Misc.Assert(!Misc.isBadDouble(fCrossProd));
    }
    return fCrossProd;
  }


  double crossprod(IntIterator iter){
    Misc.Assert(false);// doesn't handle fudge factors
    double fCrossProd = 0;
    while(iter.hasNext()){
      fCrossProd += aWeights[iter.next()];
    }
    if(Misc.isBadDouble(fCrossProd)){
      System.out.println("BadCrossProd: " + fCrossProd);
      Misc.Assert(!Misc.isBadDouble(fCrossProd));
    }
    return fCrossProd;
  }

  double score(IntIterator iter){
    double fCrossProd = crossprod(iter);
    Misc.Assert(!Misc.isBadDouble(fCrossProd));
    double fScore = Math.exp(fCrossProd);
    if(Misc.isBadDouble(fScore)){
      System.out.println("Bad Score: " + fScore + " exp of: " + fCrossProd);
      Misc.Assert(!Misc.isBadDouble(fScore));
    }
    return fScore;
  }


  double scoreMaybeLog(IntIterator iter){
    if(Config.config.bLogScores){
      return crossprod(iter); 
    } else {
      return score(iter);
    }
  }

  double scoreMaybeLog(FeatureSet featureset){
    if(Config.config.bLogScores){
      return crossprod(featureset); 
    } else {
      return score(featureset);
    }
  }

  void print(){
    // this should never get done because we have too many features

    // System.out.println("FeatureWeights:  " + iCurNumFeatures);
    // List<Pair<Integer,Double>> lPairs = new ArrayList<Pair<Integer,Double>>();
    // for(int iFeature = 0; iFeature < iCurNumFeatures; iFeature++){
    //   lPairs.add(ImmutablePair.of(iFeature, getWeight(iFeature)));
    // }
    // Comparator<Pair<Integer,Double>> comparator = 
    // Collections.reverseOrder(new Misc.pairRightComparator<Integer,Double>());
    // Collections.sort(lPairs, comparator);
    // int iLex = 0;
    // for(Pair<Integer,Double> pair : lPairs){
    //   Object oFeature = features.getFeature(pair.getLeft());
    //   if(pair.getRight() != 0.0){
    //     System.out.println("    Feature: " + oFeature + " --> " + 
    //                        pair.getRight());
    //   }
    // }
    // System.out.println("FeatureWeights-Lex:  " + iLex);
  }


}