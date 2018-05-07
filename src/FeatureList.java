class FeatureList{
  int[] aFeatures;
  private Double fCrossProd;
  FeatureList(int[] aFeatures){
    this.aFeatures = aFeatures;
    this.fCrossProd = Model.loglinear.crossprod(aFeatures);
  }
  
  void reset(){
    fCrossProd = null;
  }

  boolean hasCrossProd(){
    return (fCrossProd != null);
  }

  double getCrossProd(){
    if(fCrossProd == null){
      this.fCrossProd = Model.loglinear.crossprod(aFeatures);
    }
    return fCrossProd;
  }   
}