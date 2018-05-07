import java.lang.*; 
import java.util.*;
import java.io.*;

class CalcBest{

  static class Stats{
    String sType;
    double fMarginal;
    double fMarginalNum;
    double fScore;
    String sLine;
    Stats(String sLine){
      this.sLine = sLine;
      //String[] aSplit = sLine.split(" ");
      // fCorr = Double.parseDouble(aSplit[9]);
      // fMarginal = Double.parseDouble(aSplit[17]);
      int iStart = 0;
      if(sLine.contains("All Total")){
        iStart++;
      }
      String[] aSplit = sLine.split("\\s+");
      fScore = Double.parseDouble(aSplit[iStart+11]);
      sType = aSplit[iStart+3];
      fMarginal = Double.parseDouble(aSplit[iStart+5]);
      fMarginalNum = Double.parseDouble(aSplit[iStart+7]);
      //fScore = Double.parseDouble(aSplit[21]);
      //iNum = Integer.parseInt(aSplit[31]);
    }
    public String toString(){
      StringBuilder sb = new StringBuilder();
      //sb.append("Type: " + sType + " Corr: " + fCorr + " Marginal: " + fMarginal
      //          + " Score: " + fScore + " Num: " + iNum);
      sb.append("Type: " + sType + " Marginal: " + fMarginal + " MarginalNum: "
                + fMarginalNum );
      return sb.toString();
    }
  }



  static void calcBest(String sFilename)throws IOException{
    //read in line by line
    BufferedReader reader = new BufferedReader(new FileReader(sFilename));
    String line = null;
    Stats statsBestTrain = null;
    Map<String,Stats> mBestTest = null;
    Map<String,Stats> mPrevTest = new HashMap<String,Stats>();
    while ((line = reader.readLine()) != null) {
      if(line.contains("FINAL")){
        continue;
      }
      if(line.contains("TOTAL")){
        Stats statsCur = new Stats(line);
        if(line.contains("TRAIN") && line.contains("All Total")){
          if((statsBestTrain == null) || 
             (statsBestTrain.fScore <= statsCur.fScore)){
            statsBestTrain = statsCur;
            mBestTest = mPrevTest;
            mPrevTest = new HashMap<String,Stats>();
          }
        } else if(line.contains("TEST")){
          mPrevTest.put(statsCur.sType, statsCur);
        }
        
      }
    }
    System.out.println("Best Train: " + statsBestTrain);
    for(Stats statsTest : mBestTest.values()){
      System.out.println("Best Test: " + statsTest);
    }
  }

  public static void main(String[] args) throws Exception{
    Misc.Assert(args.length == 1);
    calcBest(args[0]);
  }

}