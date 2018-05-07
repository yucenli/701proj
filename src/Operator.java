enum Operator{
  TIMES,PLUS,MINUS,DIVIDEBY;
  static Operator fromString(String sOp){
    switch(sOp){
    case "*":
      return TIMES;
    case "+":
      return PLUS;
    case "-":
      return MINUS;
    case "/":
      return DIVIDEBY;
    default:
      Misc.Assert(false);
      return null;
    }
  }
  
  public double perform(double fLeft, double fRight){
    if(this.equals(TIMES)){
      return fLeft*fRight;
    } else if(this.equals(PLUS)){
      return fLeft+fRight;
    } else if(this.equals(MINUS)){
      return fLeft-fRight;
    } else if(this.equals(DIVIDEBY)){
      return fLeft/fRight;
    } else {
      Misc.Assert(false);
      return Double.NaN;
    }
  }

  public String toString(){
    if(this == TIMES){
      return "*";
    } else if(this == PLUS){
      return "+";
    } else if(this == MINUS){
      return "-";
    } else if(this == DIVIDEBY){
      return "/";
    } else {
      Misc.Assert(false);
      return null;
    }
  }
};