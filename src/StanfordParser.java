import java.lang.*;
import java.util.*;
import java.io.*;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;
//import javax.xml.parsers.Docume;

class StanfordParser{

  static StanfordDocument parseFile(String sFilename){
    try{
      DocumentBuilder builder = 
        DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Node nodeRoot = builder.parse(new File(sFilename)).getDocumentElement();
      return parse(nodeRoot);
    } catch(SAXException|ParserConfigurationException|IOException ex){
      throw new RuntimeException(ex);
    }

  }

  static StanfordDocument parseString(String str) throws IOException{
    try{
      DocumentBuilder builder = 
        DocumentBuilderFactory.newInstance().newDocumentBuilder();
      InputStream is = Misc.stringToInputStream(str);
      Node nodeRoot = builder.parse(is).getDocumentElement();
      return parse(nodeRoot);
    } catch(SAXException|ParserConfigurationException ex){
      throw new RuntimeException(ex);
    }
  }
  
  static StanfordDocument parse(Node nodeRoot)throws IOException{
    Node nodeDoc = 
      Misc.getFirstChildByName(nodeRoot, "document");
    Misc.Assert(nodeDoc.getNodeName().equals("document"));
    StanfordDocument doc = new StanfordDocument(nodeDoc);
    //doc.print();
    return doc;
  }

  public static void main(String[] args) throws IOException{
    Misc.Assert(args.length == 1);
    String sFileName = args[0];
    StanfordDocument doc = parseFile(sFileName);
    for(StanfordSentence sentence : doc.lSentences){
      System.out.print(sentence.sSentence + " ");
    }
    System.out.println();
    for(StanfordNumber number : doc.lNumbers){
      System.out.println("Number: " + number.toString() + " NP: " 
                         + number.ptNP.sPhrase);
    }
  }  

}