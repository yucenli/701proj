import java.lang.*;
import java.util.*;
import java.io.*;
import java.text.*;
import java.lang.reflect.*;

import au.com.bytecode.opencsv.*;
import org.apache.commons.lang3.*;
import org.apache.commons.io.FileUtils;
import com.google.gson.reflect.*;
import com.google.gson.*;
import org.apache.commons.lang3.tuple.*;
import org.w3c.dom.*;
import edu.stanford.nlp.process.Morphology;

import difflib.*;

class Misc {

  static void Assert(boolean bMustBeTrue){
    if(!bMustBeTrue){
      throw new RuntimeException("Assertion Failed:");
    }
  }

  static void Assert(boolean bMustBeTrue, String sMesg){
    if(!bMustBeTrue){
      throw new RuntimeException("Assertion Failed:" + sMesg);
    }
  }

  static List<String[]> readCsvFile(String sFileName)
    throws java.io.IOException{
    List<String[]> llEntries = 
      (new CSVReader(new FileReader(sFileName),
                                    ',','\"', '\0')).readAll();
    return llEntries;
  }

  static List<String[]> readCsvFileNoException(String sFileName){
    try{
      return readCsvFile(sFileName);
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }
  }


  static void writeCsvFileNoException(String sFileName, 
                                      List<String[]> llEntries){
    try{
      writeCsvFile(sFileName, llEntries);
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }
  }

  static void writeCsvFile(String sFileName, List<String[]> llEntries)
    throws java.io.IOException{
    CSVWriter writer = new CSVWriter(new FileWriter(sFileName));
    writer.writeAll(llEntries);
    writer.close();
  }

  static public int doubleToComparableInt(double fVal){
    if(fVal < 0){
      return -1;
    } else if(fVal > 0){
      return 1;
    } else {
      if(fVal != 0){
        System.out.println("Bad Comparable Val: " + fVal);
      }
      Assert(fVal == 0);
      return 0;
    }
  }


  public static final long DoubleConsts_EXP_BIT_MASK = 0x7FF0000000000000L;
  public static final long DoubleConsts_SIGNIF_BIT_MASK = 0x000FFFFFFFFFFFFFL;

  static int doubleToHashCode(double value){
    // from Double.
    long bits = Double.doubleToRawLongBits(value);
    // Check for NaN based on values of bit fields, maximum
    // exponent and nonzero significand.
    if ( ((bits & DoubleConsts_EXP_BIT_MASK) ==
          DoubleConsts_EXP_BIT_MASK) &&
         (bits & DoubleConsts_SIGNIF_BIT_MASK) != 0L){
        bits = 0x7ff8000000000000L;
    }
    return (int)(bits ^ (bits >>> 32));
  }

  static int hashCombine(int iCurHash, Object obj){
    return hashCombine(iCurHash, obj.hashCode());
  }

  static int hashCombine(int iCurHash, int iNewHash){
    //copied from boost::hash_combine
    return (iCurHash ^ (iNewHash+0x9e3779b9 + (iCurHash << 6)+(iCurHash >> 2)));
  }

  static <T> T loadGsonWithBackslashEscaped(String sFileName, Class<T> clazz){
    try{
      String sFileContents = FileUtils.readFileToString(new File(sFileName));
      sFileContents = sFileContents.replace("\\", "\\\\");
      return new Gson().fromJson(new StringReader(sFileContents), clazz);
    }catch(IOException ex){
      throw new RuntimeException(ex);
    }
  } 

  static Double parseDouble(String sDouble){
    try{
      NumberFormat nf = NumberFormat.getNumberInstance();
      Double doub = nf.parse(sDouble).doubleValue();
      return doub;
    } catch(java.text.ParseException ex){
      return null;
    }
  }

  static Double getNumber(String sWord){
    //check if it's a known word
    Double fWord = Config.config.mNumbers.get(sWord);
    if(fWord != null){
      return fWord;
    }
    //don't allow 2nd 3rd, 4th, etc.
    if(sWord.endsWith("nd") || sWord.endsWith("rd") || sWord.endsWith("th")){
      return null;
    }

    return parseDouble(sWord);
  }

  static boolean nullSafeEquals(Object obj1, Object obj2){
    if(obj1 == null){
      return (obj2 == null);
    } else {
      return obj1.equals(obj2);
    }
  }

  public static boolean isCloseTo(double a, double b) {
    //round both to 2 digits and then compare
    return roundToTwoDecimalPlaces(a) == roundToTwoDecimalPlaces(b);
    // if (a>b) {
    //   return (a-b)<=1e-2;
    // } else {
    //   return (b-a)<=1e-2;
    // }
  }

  public static boolean isCloseTo(List<Double> list1, List<Double> list2) {
    if(list1.size() != list2.size()){
      return false;
    }
    for(int iIndex = 0; iIndex < list1.size(); iIndex++){
      if(!isCloseTo(list1.get(iIndex), list2.get(iIndex))){
        return false;
      }
    }
    //all items are close
    return true;
  }


  public static boolean isCloseTo(double a, double b, double fPrecision) {
    if (a>b) {
      return (a-b)<=fPrecision;
    } else {
      return (b-a)<=fPrecision;
    }
  }

  static String fracStr(long iNum, long iDenom){
    return Double.toString(((double)iNum)/((double)iDenom));
  }
  static String fracStr(int iNum, int iDenom){
    return Double.toString(((double)iNum)/((double)iDenom));
  }

  static String makeOneLine(List<String> lLines){
    StringBuilder sb = new StringBuilder();
    for(String sLine : lLines){
      sb.append(sLine);
    }
    return sb.toString();
  }

  static List<Integer> loadIntList(String sFile){
    List<String> lLines;
    try{
      lLines = FileUtils.readLines(new File(sFile));
    }catch(IOException ex){
      throw new RuntimeException(ex);
    }

    List<Integer> lInts = new ArrayList<Integer>(lLines.size());
    for(String sLine : lLines){
      lInts.add(Integer.parseInt(sLine));
    }
    return lInts;
  }

  static boolean arraycompare(double[] array1, int iPos1, double[] array2, 
                              int iPos2, int iLen){
    for(int iPos = 0; iPos < iLen; iPos++){
      if(array1[iPos1+iPos] != array2[iPos2+iPos]){
        return false;
      }
    }
    return true;
  }

  static boolean isBadDouble(double fVal){
    return Double.isInfinite(fVal) || Double.isNaN(fVal);
  }

  static boolean isBadDouble(double fVal, boolean bLog){
    if(bLog){
      //if it's log then we allow negative infinity, which is just zero
      return Double.isNaN(fVal) || (fVal == Double.POSITIVE_INFINITY);
    } else {
      //Misc.Assert(!bLog);
      return Double.isInfinite(fVal) || Double.isNaN(fVal);
    }
  }

  static class MapEntryValueComparator<T1, T extends Comparable>
    implements Comparator<Map.Entry<T1,T>>{
    public int compare(Map.Entry<T1,T> entry1, Map.Entry<T1,T> entry2){
      return entry1.getValue().compareTo(entry2.getValue());
    }
  }
  static class pairRightComparator<T1, T extends Comparable>
    implements Comparator<Pair<T1,T>>{
    public int compare(Pair<T1,T> pair1, Pair<T1,T> pair2){
      return pair1.getRight().compareTo(pair2.getRight());
    }
  }

  static class TripleRightComparator<T1, T2, T extends Comparable>
    implements Comparator<Triple<T1,T2,T>>{
    public int compare(Triple<T1,T2,T> triple1, Triple<T1,T2,T> triple2){
      return triple1.getRight().compareTo(triple2.getRight());
    }
  }

  static class pairLeftComparator<T extends Comparable, T2>
    implements Comparator<Pair<T,T2>>{
    public int compare(Pair<T,T2> pair1, Pair<T,T2> pair2){
      return pair1.getLeft().compareTo(pair2.getLeft());
    }
  }

  static class ListComparator<T extends Comparable>
    implements Comparator<List<T>>{
    public int compare(List<T> list1, List<T> list2){
      if(list1.size() != list2.size()){
        return list1.size()-list2.size();
      }
      for(int i = 0; i < list1.size(); i++){
        int iCompare = list1.get(i).compareTo(list2.get(i));
        if(iCompare != 0){
          return iCompare;
        }
      }
      // they are exactly equal from a comparable perspective
      if(!list1.equals(list2)){
        System.out.println("BadList: " + list1 + "-->" + list2);
        Misc.Assert(list1.equals(list2));
      }
      return 0;
    }
  }

  static <T> void addAllSubsets(List<T> list, List<T> lCur, int iIndex, 
                                List<List<T>> llists){
    //getsubsets with, and subsets without
    if(iIndex == list.size()){
      if(lCur.size() != 0){
        llists.add(new ArrayList<T>(lCur));
      }
      return;
    }
    //get all subsets without this item
    addAllSubsets(list, lCur, iIndex+1, llists);
    //get all subsets *with* this item
    lCur.add(list.get(iIndex));
    addAllSubsets(list, lCur, iIndex+1, llists);
    lCur.remove(lCur.size()-1);
  }

  static <T> List<List<T>> getAllSubsets(List<T> list){
    //we'll do this recursively
    List<List<T>> llists = new ArrayList<List<T>>();
    addAllSubsets(list, new ArrayList<T>(), 0, llists);
    return llists;
  }

  static InputStream stringToInputStream(String str){
    return new ByteArrayInputStream(str.getBytes());
  }

  public static String getAttribute(Node node, String sName){
    NamedNodeMap map = node.getAttributes();
    Node nodeName = map.getNamedItem(sName);
    Assert(nodeName != null);
    return nodeName.getNodeValue();
  }

  public static String getTextAttribute(Node node, String sName){
    Node nodeTextChild = getFirstChildByName(node, sName);
    NodeList nodelistChildren = nodeTextChild.getChildNodes();
    for(int iChild = 0; iChild < nodelistChildren.getLength(); iChild++){
      Node nodeChild = nodelistChildren.item(iChild);
      if(nodeChild.getNodeType() == Node.TEXT_NODE){
        return nodeChild.getNodeValue();
      }
    }
    Misc.Assert(false); // should always find the text
    return null;
  }

  public static int getTextAttributeAsInt(Node node, String sName){
    return Integer.parseInt(getTextAttribute(node, sName));
  }

  public static Node getFirstChildByName(Node node, String sName){
    NodeList nodelistChildren = node.getChildNodes();
    for(int iChild = 0; iChild < nodelistChildren.getLength(); iChild++){
      Node nodeChild = nodelistChildren.item(iChild);
      if(nodeChild.getNodeName().equals(sName)){
        return nodeChild;
      }
    }
    Misc.Assert(false); // should always find the text
    return null;
  }

  public static boolean hasChildByName(Node node, String sName){
    NodeList nodelistChildren = node.getChildNodes();
    for(int iChild = 0; iChild < nodelistChildren.getLength(); iChild++){
      Node nodeChild = nodelistChildren.item(iChild);
      if(nodeChild.getNodeName().equals(sName)){
        return true;
      }
    }
    return false;
  }

  public static List<Node> getAllChildrenByName(Node node, String sName){
    List<Node> lNodes = new ArrayList<Node>();
    NodeList nodelistChildren = node.getChildNodes();
    for(int iChild = 0; iChild < nodelistChildren.getLength(); iChild++){
      Node nodeChild = nodelistChildren.item(iChild);
      if(nodeChild.getNodeName().equals(sName)){
        lNodes.add(nodeChild);
      }
    }
    Misc.Assert(lNodes.size() != 0);
    return lNodes;
  }

  static double[] mult(double[] a, double fMultipler){
    double[] aNew = new double[a.length];
    for(int i = 0; i < a.length; i++){
      aNew[i] = a[i]*fMultipler;
    }
    return aNew;
  }

  static double[] add(double[] a, double[] b){
    Assert(a.length == b.length);
    double[] c = new double[a.length];
    for(int i = 0; i < a.length; i++){
      c[i] = a[i] + b[i];
    }
    return c;
  }


  public static double logAdd(double lx, double ly) {
    double max, negDiff;
    if (lx > ly) {
      max = lx;
      negDiff = ly - lx;   
    } else {   
      max = ly;   
      negDiff = lx - ly;   
    }   
    if(Double.isNaN(max)){
      System.out.println("Bad LogAdd: " + lx + " " + ly);
      Assert(!Double.isNaN(max));
    }
    if (max == Double.NEGATIVE_INFINITY) {   
      return max;   
      //} else if (negDiff < -LOGTOLERANCE) {   
      //return max;   
    } else if(negDiff == Double.NEGATIVE_INFINITY){
      return max;
    } else {   
      double fRetVal = max + Math.log1p(Math.exp(negDiff));   
      if(Double.isNaN(fRetVal)){
        System.out.println("Bad LogAdd: " + lx + " " + ly);
        Assert(!Double.isNaN(fRetVal));
      }
      return fRetVal;
    }   
  }


  public static double addMaybeLog(double a, double b){
    return addMaybeLog(a, b, Config.config.bLogScores);
  }

  public static double addMaybeLog(double a, double b, boolean bLog){
    if(bLog){
      return logAdd(a, b);
    } else {
      return a+b;
    }
  }

  public static double multMaybeLog(double a, double b){
    return multMaybeLog(a,b,Config.config.bLogScores);
  }
  public static double multMaybeLog(double a, double b, boolean bLog){
    if(bLog){
      return a+b;
    } else {
      return a*b;
    }
  }

  public static double divMaybeLog(double a, double b){
    return divMaybeLog(a,b,Config.config.bLogScores);
  }
  public static double divMaybeLog(double a, double b, boolean bLog){
    if(bLog){
      return a-b;
    } else {
      return a/b;
    }
  }

  public static double zeroMaybeLog(){
    return zeroMaybeLog(Config.config.bLogScores);
  }

  public static double zeroMaybeLog(boolean bLog){
    if(bLog){
      return Double.NEGATIVE_INFINITY;
    } else {
      return 0.0;
    }
  }

  public static double oneMaybeLog(){
    return oneMaybeLog(Config.config.bLogScores);
  }
  public static double oneMaybeLog(boolean bLog){
    if(bLog){
      return 0.0;
    } else {
      return 1.0;
    }
  }

  public static double maybeLog(double fVal){
    return maybeLog(fVal, Config.config.bLogScores);
  }

  public static double maybeLog(double fVal, boolean bLog){
    if(bLog){
      return Math.log(fVal);
    } else {
      return fVal;
    }
  }

  public static double maybeExp(double fVal){
    if(Config.config.bLogScores){
      return Math.exp(fVal);
    } else {
      return fVal;
    }
  }

  static double max(double... aVals){
    double fMax = aVals[0];
    for(int iVal = 1; iVal < aVals.length; iVal++){
      fMax = Math.max(fMax, aVals[iVal]);
    }
    return fMax;
  }

  static DecimalFormat dfTwo = new DecimalFormat("#.##");
  static double roundToTwoDecimalPlaces(double fVal){
    return Double.valueOf(dfTwo.format(fVal));
  }

  static <T> List<T> genEmptyList(int iSize){
    List<T> list = new ArrayList<T>();
    for(int i = 0; i < iSize; i++){
      list.add(null);
    }
    return list;
  }

  static <T> List<T> genFullList(int iSize, T tInit){
    List<T> list = new ArrayList<T>(iSize);
    for(int i = 0; i < iSize; i++){
      list.add(tInit);
    }
    return list;
  }

  static <T> List<List<T>> genListOfListOf(int iSize){
    List<List<T>> llist = new ArrayList<List<T>>(iSize);
    for(int i = 0; i < iSize; i++){
      llist.add(new ArrayList<T>());
    }
    return llist;
  }


  static <T> List<T> genListOfNew(int iSize, Class type){
    List<T> list = new ArrayList<T>(iSize);
    for(int i = 0; i < iSize; i++){
      try {
        list.add((T)type.newInstance());
      } catch(Exception ex){
        throw new RuntimeException(ex);
      }
    }
    return list;
  }

  static <T extends Comparable<T>>  int listCompare(List<T> list1, 
                                                    List<T> list2){
    if(list1.size() != list2.size()){
      return list1.size()-list2.size();
    }
    for(int iIndex = 0; iIndex < list1.size(); iIndex++){
      int iCur = list1.get(iIndex).compareTo(list2.get(iIndex));
      if(iCur != 0){
        return iCur;
      }
    }
    // the lists are identical from a comparable perspective
    return 0;
  }

  static <T> void addNTimes(List<T> list, T t, int iNumTimes){
    for(int i = 0; i < iNumTimes; i++){
      list.add(t);
    }
  }

  static int findFirstLetter(String str){
    for(int iChar = 0; iChar < str.length(); iChar++){
      if(Character.isLetter(str.charAt(iChar))){
        return iChar;
      }
    }
    //should never reach here
    Misc.Assert(false);
    return 0;
  }

  static List<String> listOfLetters(char cStart, int iSize){
    List<String> lLetters = new ArrayList<String>();
    for(int iLetter = 0; iLetter < iSize; iLetter++){
      lLetters.add(Character.toString((char)(cStart + iLetter)));
    }
    return lLetters;
  }

  static List<Integer> listOfIntegers(int iStart, int iSize){
    List<Integer> lInts = new ArrayList<Integer>();
    for(int i = 0; i < iSize; i++){
      lInts.add(i);
    }
    return lInts;
  }

  static <T> List<String> toStringList(List<T> list){
    List<String> lStrings = new ArrayList<String>();
    for(T t: list){
      lStrings.add(t.toString());
    } 
    return lStrings;
  }



  static <T> void genAllPermutations(List<T> lCur, 
                                     List<T> lAll, boolean[] aUsed, 
                                     List<List<T>> llObjs){
    if(lCur.size() == lAll.size()){
      llObjs.add(new ArrayList<T>(lCur));
      return;
    }
    for(int iIndex = 0; iIndex < lAll.size(); iIndex++){
      if(aUsed[iIndex]){
        continue;
      }
      T obj = lAll.get(iIndex);
      Misc.Assert(obj != null);
      lCur.add(obj);
      aUsed[iIndex] = true;
      genAllPermutations(lCur, lAll, aUsed, llObjs);
      aUsed[iIndex] = false;
      lCur.remove(lCur.size()-1);
    }
  }

  static <T> List<List<T>> genAllPermutations(List<T> lAll){ 
    List<List<T>> llObjs = new ArrayList<List<T>>();
    genAllPermutations(new ArrayList<T>(), lAll, new boolean[lAll.size()],
                       llObjs);
    return llObjs;
  }

	private static int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}

  static int levenshteinDistance(List<?> list1, List<?> list2){
		int[][] distance = new int[list1.size() + 1][list2.size() + 1];
 
		for (int i = 0; i <= list1.size(); i++){
			distance[i][0] = i;
    }

		for (int j = 1; j <= list2.size(); j++){
			distance[0][j] = j;
    }
 
		for (int i = 1; i <= list1.size(); i++){
			for (int j = 1; j <= list2.size(); j++){
        int iComb = distance[i - 1][j - 1] + 
          (list1.get(i-1).equals(list2.get(j-1)) ? 0 : 1);
				distance[i][j] = minimum(
          distance[i - 1][j] + 1,
          distance[i][j - 1] + 1,
          iComb
          );
      }
    }
 
		return distance[list1.size()][list2.size()];    
  }

  static double similarity(List<?> list1, List<?> list2){
    int iMaxLen = Math.max(list1.size(), list2.size());
    int iDist = levenshteinDistance(list1, list2);
    return 1.0-(((double)iDist)/((double) iMaxLen));
  }

  static boolean stringContainsAtLeastOneOf(String str, List<String> lSubStrs){
    for(String sSubStr : lSubStrs){
      if(str.contains(sSubStr)){
        return true;
      }
    }
    return false;
  }
  
 
// 	public static int computeLevenshteinDistance(String str1,String str2) {
// 		int[][] distance = new int[str1.length() + 1][str2.length() + 1];
 
// 		for (int i = 0; i <= str1.length(); i++)
// 			distance[i][0] = i;
// 		for (int j = 1; j <= str2.length(); j++)
// 			distance[0][j] = j;
 
// 		for (int i = 1; i <= str1.length(); i++)
// 			for (int j = 1; j <= str2.length(); j++)
// 				distance[i][j] = minimum(
// 						distance[i - 1][j] + 1,
// 						distance[i][j - 1] + 1,
// 						distance[i - 1][j - 1]+ ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));
 
// 		return distance[str1.length()][str2.length()];    
// 	}
// }


  static <T1,T2> boolean equalSets(List<T1> list1,List<T2> list2,
                                   Relation<T1,T2> relation){
    if(list1.size() != list2.size()){
      return false;
    }
    boolean[] aUsed = new boolean[list2.size()];
    for(T1 t1 : list1){
      boolean bFoundEqual = false;
      for(int i2 = 0; i2 < list2.size(); i2++){
        if(aUsed[i2]){
          continue;
        }
        T2 t2 = list2.get(i2);
        if(relation.equal(t1, t2)){
          aUsed[i2] = true;
          bFoundEqual = true;
          break;
        }
      }
      if(!bFoundEqual){
        return false;
      }
    }
    return true;
  }


  static <T1,T2> boolean isASubsetOfB(List<T1> listA, List<T2> listB,
                                      Relation<T1,T2> relation, 
                                      boolean[] aUsed,int iA){
    if(iA == listA.size()){
      //we found a path, woohoo!
      return true;
    }
    T1 tA = listA.get(iA);
    //find a subset for this
    for(int iB = 0; iB < listB.size(); iB++){
      if(aUsed[iB]){
        continue;
      }
      T2 tB = listB.get(iB);
      if(relation.isASubsetOfB(tA, tB)){
        aUsed[iB] = true;
        if(isASubsetOfB(listA, listB, relation, aUsed, iA+1)){
          return true;
        }
        //backtrack
        aUsed[iB] = false;
      }
    }
    //if no path returned true then no possible subset matching
    return false;
  }

  static <T1,T2> boolean isASubsetOfB(List<T1> listA, List<T2> listB, 
                                      Relation<T1,T2> relation){
    if(listA.size() > listB.size()){
      return false;
    }
    return isASubsetOfB(listA, listB, relation, new boolean[listB.size()], 0);
  }

  static double div(int iNumerator, int iDenominator){
    return (((double) iNumerator)/((double) iDenominator));
  }

  static List<String> toListOfStrings(List<?> lObjs){
    List<String> lStrings = new ArrayList<String>();
    for(Object obj : lObjs){
      lStrings.add(obj.toString());
    }
    return lStrings;
  }

  static <T> List<T> remap(List<Integer> lMap, List<T> lOrig){
    List<T> lNew = new ArrayList<T>(lMap.size());
    for(Integer iCur : lMap){
      lNew.add(lOrig.get(iCur));
    }
    return lNew;
  }

  static List<List<List<String>>> readListListListStrings(String sFilename){
    String sFileContents = null;
    try{
      sFileContents = 
        FileUtils.readFileToString(new File(sFilename));
    } catch(IOException ex){
      throw new RuntimeException(ex);
    }
    Type type = new TypeToken<List<List<List<String>>>>(){}.getType();
    List<List<List<String>>> list = new Gson().fromJson(sFileContents, type);
    return list;
  }


  static <T> List<List<T>> splitList(List<T> list, int iNumPieces){
    int iSize = list.size()/iNumPieces;
    List<List<T>> llPieces = new ArrayList<List<T>>();
    for(int iPiece = 0; iPiece < iNumPieces; iPiece++){
      int iStart = iPiece*iSize;
      int iEnd = (iPiece+1)*iSize;
      if(iPiece == (iNumPieces-1)){
        iEnd = list.size();
      }
      llPieces.add(new ArrayList<T>(list.subList(iStart, iEnd)));
    }
    return llPieces;
  }

  static int overlapType(String sUnknown, StanfordWord word,
                         Morphology morphology){
    //build the np set
    List<StanfordWord> lNP = word.getSubTree();
    Set<String> setNP = new HashSet<String>();
    for(StanfordWord wordCur : lNP){
      setNP.add(wordCur.sWord.toLowerCase());
      setNP.add(wordCur.sWordOrig.toLowerCase());
    }
    List<String> lWords = Arrays.asList(sUnknown.split("_"));
    int iNumOverlaps = 0;
    //now stem all the words
    for(String sWord : lWords){
      if(setNP.contains(sWord) || 
         setNP.contains(morphology.stem(sWord).toLowerCase())){
        iNumOverlaps++;
      }
    }
    if(iNumOverlaps == 0){
      return 0;
    }
    if(iNumOverlaps == lWords.size()){
      if(iNumOverlaps == lNP.size()){
        //exact match
        return 4;
      } else {
        //all words matched but not same as np
        return 3;
      }
    } else {
      //not all words matched
      return 2;
    }
  }


  static StanfordWord getWord(String sSig, StanfordDocument doc){
    String[] aSplit = sSig.split(":");
    if(aSplit.length != 3){
      System.out.println("Bad Split len: " + sSig);
    }
    Misc.Assert(aSplit.length == 3);
    String sWord = aSplit[0];
    int iSentence = Integer.parseInt(aSplit[1]);
    int iWord = Integer.parseInt(aSplit[2]);
    StanfordWord word = doc.lSentences.get(iSentence).lWords.get(iWord);
    if(!sWord.equals(word.sWordOrig)){
      System.out.println("String: " + sWord + " does not equal StanfordWord: " 
                         + word.sWordOrig + " Sig: " + sSig + " sent: " + 
                         iSentence + " word: " + iWord);
    }
    Misc.Assert(sWord.equals(word.sWordOrig));
    if(!word.isNoun()){
      System.out.println(sSig + " is not a noun!");
    }
    Misc.Assert(word.isNoun());
    return word;
  }

  static boolean isInteger(double fVal){
    return ((fVal == Math.floor(fVal)) && !Double.isInfinite(fVal));
  }

}