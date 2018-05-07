class Main{
  public static void main(String[] args) throws Exception{
    if((args.length < 1) || (args.length > 2)){
      System.out.println("You need to specify the # folds on the command-line");
    }
    int iTestFold = Integer.parseInt(args[0]);
    String sConfigFile = null;
    if(args.length == 2){
      sConfigFile = args[1];
    }


    Model.load(sConfigFile, iTestFold, false);
    TermClassifier classifier = new TermClassifier(Model.lTrainQuestions, 
                                                   Model.lTestQuestions);

    classifier.train();

    Test.checkAllQuestions();
  }
}
