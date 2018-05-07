class Triple<L,M,R>{
  L l;
  M m;
  R r;
  Triple(L l, M m, R r){
    this.l = l;
    this.m = m;
    this.r = r;
  }

  static <L,M,R> Triple of(L l, M m, R r){
    return new Triple(l,m,r);
  }

  L getLeft(){
    return l;
  }

  M getMiddle(){
    return m;
  }

  R getRight(){
    return r;
  }

}