import java.lang.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;

class FeatureListList{
  List<FeatureList> lFeatureLists = new ArrayList<FeatureList>();
  double fCrossProd = 0;

  void add(FeatureList featurelist){
    if(featurelist.aFeatures.length != 0){
      lFeatureLists.add(featurelist);
      fCrossProd += featurelist.getCrossProd();
    }
  }

  //void addSingleFeature(int iFeature){
  //  lFeatureLists.add(new FeatureList(new int[]{iFeature}));
  //}

  double scoreMaybeLog(){
    if(lFeatureLists.size() == 0){
      Misc.Assert(!Config.config.bUsePairFeatures && 
                  !Config.config.bUseDocumentFeatures);
      if(Config.config.bLogScores){
        return 0.0;
      } else {
        return 1.0;
      }
    }
    Misc.Assert(lFeatureLists.size() != 0);
    double fScore = Config.config.bLogScores ? fCrossProd :Math.exp(fCrossProd);
    if(fScore == Misc.zeroMaybeLog()){
      System.out.println("Bad CrossProd: " + this.fCrossProd + " Score: " 
                         + fScore);
      Misc.Assert(fScore != Misc.zeroMaybeLog());
    }
    if(Misc.isBadDouble(fScore)){
      System.out.println("BadCrossProd: " + fCrossProd + " " + fScore);
      Misc.Assert(!Misc.isBadDouble(fScore));
    }
    return fScore;
  }

  public String toString(){
    return toStringBuilder(new StringBuilder()).toString();
  }

  StringBuilder toStringBuilder(StringBuilder sb){
    //first get all the features as pairs
    List<Triple<Integer,Object,Double>> lFeatures = 
      new ArrayList<Triple<Integer,Object,Double>>();
    for(FeatureList featurelist : lFeatureLists){
      int[] aFeatures = featurelist.aFeatures;
      for(int iIndex = 0; iIndex < aFeatures.length; iIndex++){
        int iFeature = aFeatures[iIndex];
        double fWeight = Model.loglinear.aWeights[iFeature];
        double fFudgeFactor = Model.loglinear.getFudgeFactor(iFeature);
        Object oFeature = Model.features.getFeature(iFeature);
        lFeatures.add(Triple.of(iFeature, oFeature, fWeight*fFudgeFactor));
      }
    }
    // now sort by the weight - highest weight to lowest weight
    Comparator<Triple<Integer,Object, Double>> comparator = 
      new Misc.TripleRightComparator();
    Collections.sort(lFeatures, Collections.reverseOrder(comparator));
    //now print them out
    for(Triple<Integer,Object, Double> triple : lFeatures){
      int iFeature = triple.getLeft();
      double fWeight = Model.loglinear.aWeights[iFeature];
      double fFudgeFactor = Model.loglinear.getFudgeFactor(iFeature);
      sb.append(triple.getMiddle()).append(" --> ")
        .append(fWeight);
      if(Config.config.bUseFudgeFactors){
        sb.append(" * ").append(fFudgeFactor).append(" = ")
          .append(triple.getRight());
      }
      sb.append("\n");
    }
    return sb;
  }


}