/* Generated By:JavaCC: Do not edit this line. Parser.java */
import java.lang.*;
import java.util.*;
import java.io.*;
import org.apache.commons.lang3.tuple.*;

class Parser implements ParserConstants {

  public static List<Double> parseMaximaDoubleResults(String sResults){
    try{
      InputStream is = new ByteArrayInputStream(sResults.getBytes());
      return new Parser(is).MaximaDoubleResults();
    } catch(ParseException ex){
      throw new RuntimeException(ex);
    } catch(TokenMgrError ex){
      throw new RuntimeException(ex);
    }
  }

  public static ConcreteSystem parseSystemNoException(List<String> lStrings){
    try{
      return parseSystem(lStrings);
    } catch(ParseException ex){
      throw new RuntimeException(ex);
    } catch(TokenMgrError ex){
      throw new RuntimeException(ex);
    }
  }

  public static ConcreteSystem parseSystem(List<String> lStrings)
    throws ParseException,TokenMgrError{
    if(Config.config.bPrintDuringQuestionLoad){
      System.out.println("Parsing system: " + lStrings);
    }
    Pair<List<String>,Integer> pairUnknowns =
      new MutablePair((List<String>)new ArrayList<String>(), 0);
    List<Double> lNumbers = new ArrayList<Double>();
    List<Equation> lEquations = new ArrayList<Equation>();
    for(String str : lStrings){
      lEquations.add(parseEquation(str, pairUnknowns, lNumbers));
    }
    return new ConcreteSystem(lEquations, pairUnknowns.getLeft(), lNumbers);
  }

  public static Equation parseEquation(String str,
                                       Pair<List<String>,Integer> pairUnknowns,
                                       List<Double> lNumbers)
    throws ParseException, TokenMgrError {
    if(Config.config.bPrintDuringQuestionLoad){
      System.out.println("Parsing Expression: " + str);
    }
    InputStream is = new ByteArrayInputStream(str.getBytes());
    Equation equation = new Parser(is).Parse(pairUnknowns, lNumbers);
    if(Config.config.bPrintDuringQuestionLoad){
      System.out.println("Done parsing...");
    }
    return equation;
  }

  final public Equation Parse(Pair<List<String>,Integer> pairUnknowns, List<Double> lNumbers) throws ParseException {
  EquationTerm termLeft;
  EquationTerm termRight;
    termLeft = Sum(pairUnknowns, lNumbers);
    jj_consume_token(EQUALS);
    termRight = Sum(pairUnknowns, lNumbers);
    {if (true) return new Equation(termLeft, termRight);}
    throw new Error("Missing return statement in function");
  }

  final public EquationTerm Sum(Pair<List<String>,Integer> pairUnknowns,
                 List<Double> lNumbers) throws ParseException {
 Token tUnknown = null;
 Token tNumber = null;
 Token tOperator;
 EquationTerm term;
 EquationTerm term2;
    term = Term(pairUnknowns, lNumbers);
    label_1:
    while (true) {
      if (jj_2_1(2)) {
        ;
      } else {
        break label_1;
      }
      if (jj_2_2(2)) {
        tOperator = jj_consume_token(PLUS);
      } else if (jj_2_3(2)) {
        tOperator = jj_consume_token(MINUS);
      } else {
        jj_consume_token(-1);
        throw new ParseException();
      }
      term2 = Term(pairUnknowns, lNumbers);
        Operator op = Operator.fromString(tOperator.image);
        term = new EquationTerm.Complex(op, term, term2);
    }
    {if (true) return term;}
    throw new Error("Missing return statement in function");
  }

  final public EquationTerm Term(Pair<List<String>,Integer> pairUnknowns,
                  List<Double> lNumbers) throws ParseException {
 Token tUnknown = null;
 Token tNumber = null;
 Token tOperator;
 EquationTerm term;
 EquationTerm term2;
    if (jj_2_4(2)) {
      term = SimpleTerm(pairUnknowns, lNumbers);
    } else if (jj_2_5(2)) {
      jj_consume_token(MINUS);
      tNumber = jj_consume_token(NUMBER);
      term = new EquationTerm.Number(tNumber.image, lNumbers, true);
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
    label_2:
    while (true) {
      if (jj_2_6(2)) {
        ;
      } else {
        break label_2;
      }
      if (jj_2_9(2)) {
        if (jj_2_7(2)) {
          tOperator = jj_consume_token(TIMES);
        } else if (jj_2_8(2)) {
          tOperator = jj_consume_token(DIVIDEBY);
        } else {
          jj_consume_token(-1);
          throw new ParseException();
        }
        term2 = SimpleTerm(pairUnknowns, lNumbers);
        Operator op = Operator.fromString(tOperator.image);
        term = new EquationTerm.Complex(op, term, term2);
      } else if (jj_2_10(2)) {
        term2 = ParenTerm(pairUnknowns, lNumbers);
          term = new EquationTerm.Complex(Operator.TIMES, term, term2);
      } else {
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
    {if (true) return term;}
    throw new Error("Missing return statement in function");
  }

  final public EquationTerm SimpleTerm(Pair<List<String>,Integer> pairUnknowns,
                        List<Double> lNumbers) throws ParseException {
  EquationTerm term;
  EquationTerm term2;
  Token tUnknown = null;
  Token tNumber = null;
  boolean bUnknown = false;
    if (jj_2_12(2)) {
      tUnknown = jj_consume_token(NAME);
       term = new EquationTerm.Unknown(tUnknown.image, pairUnknowns);
    } else if (jj_2_13(2)) {
      tNumber = jj_consume_token(NUMBER);
      label_3:
      while (true) {
        if (jj_2_11(2)) {
          ;
        } else {
          break label_3;
        }
        tUnknown = jj_consume_token(NAME);
          Misc.Assert(!bUnknown);
          bUnknown = true;
      }
      term = new EquationTerm.Number(tNumber.image, lNumbers, false);
      if(bUnknown){
        term2 = new EquationTerm.Unknown(tUnknown.image, pairUnknowns);
        term = new EquationTerm.Complex(Operator.TIMES, term, term2);
      }
    } else if (jj_2_14(2)) {
      term = ParenTerm(pairUnknowns, lNumbers);
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
    {if (true) return term;}
    throw new Error("Missing return statement in function");
  }

  final public EquationTerm ParenTerm(Pair<List<String>,Integer> pairUnknowns,
                       List<Double> lNumbers) throws ParseException {
  EquationTerm term;
    if (jj_2_15(2)) {
      jj_consume_token(OPEN_PAREN);
    } else if (jj_2_16(2)) {
      jj_consume_token(OPEN_BRACKET);
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
    term = Sum(pairUnknowns, lNumbers);
    if (jj_2_17(2)) {
      jj_consume_token(CLOSE_PAREN);
    } else if (jj_2_18(2)) {
      jj_consume_token(CLOSE_BRACKET);
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
    {if (true) return term;}
    throw new Error("Missing return statement in function");
  }

// parsing of maxima results
  final public List<Double> MaximaDoubleResults() throws ParseException {
  List<Double> lResults = new ArrayList<Double>();
  Double fCur = null;
    label_4:
    while (true) {
      if (jj_2_19(2)) {
        ;
      } else {
        break label_4;
      }
      jj_consume_token(OPEN_BRACKET);
    }
    label_5:
    while (true) {
      if (jj_2_20(2)) {
        ;
      } else {
        break label_5;
      }
      fCur = OneMaximaDoubleResult();
        lResults.add(fCur);
    }
    label_6:
    while (true) {
      if (jj_2_21(2)) {
        ;
      } else {
        break label_6;
      }
      jj_consume_token(CLOSE_BRACKET);
    }
    {if (true) return lResults;}
    throw new Error("Missing return statement in function");
  }

  final public Double OneMaximaDoubleResult() throws ParseException {
  Token tName;
  Token tResult;
    tName = jj_consume_token(NAME);
    jj_consume_token(EQUALS);
    if (jj_2_22(2)) {
      tResult = jj_consume_token(NUMBER);
                          {if (true) return Misc.parseDouble(tResult.image);}
    } else if (jj_2_23(2)) {
      jj_consume_token(MINUS);
      tResult = jj_consume_token(NUMBER);
                                  {if (true) return 0-Misc.parseDouble(tResult.image);}
    } else {
      jj_consume_token(-1);
      throw new ParseException();
    }
    throw new Error("Missing return statement in function");
  }

  private boolean jj_2_1(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_1(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  private boolean jj_2_2(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_2(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(1, xla); }
  }

  private boolean jj_2_3(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_3(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(2, xla); }
  }

  private boolean jj_2_4(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_4(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(3, xla); }
  }

  private boolean jj_2_5(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_5(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(4, xla); }
  }

  private boolean jj_2_6(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_6(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(5, xla); }
  }

  private boolean jj_2_7(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_7(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(6, xla); }
  }

  private boolean jj_2_8(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_8(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(7, xla); }
  }

  private boolean jj_2_9(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_9(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(8, xla); }
  }

  private boolean jj_2_10(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_10(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(9, xla); }
  }

  private boolean jj_2_11(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_11(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(10, xla); }
  }

  private boolean jj_2_12(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_12(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(11, xla); }
  }

  private boolean jj_2_13(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_13(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(12, xla); }
  }

  private boolean jj_2_14(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_14(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(13, xla); }
  }

  private boolean jj_2_15(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_15(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(14, xla); }
  }

  private boolean jj_2_16(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_16(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(15, xla); }
  }

  private boolean jj_2_17(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_17(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(16, xla); }
  }

  private boolean jj_2_18(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_18(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(17, xla); }
  }

  private boolean jj_2_19(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_19(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(18, xla); }
  }

  private boolean jj_2_20(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_20(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(19, xla); }
  }

  private boolean jj_2_21(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_21(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(20, xla); }
  }

  private boolean jj_2_22(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_22(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(21, xla); }
  }

  private boolean jj_2_23(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_23(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(22, xla); }
  }

  private boolean jj_3_11() {
    if (jj_scan_token(NAME)) return true;
    return false;
  }

  private boolean jj_3_20() {
    if (jj_3R_10()) return true;
    return false;
  }

  private boolean jj_3_18() {
    if (jj_scan_token(CLOSE_BRACKET)) return true;
    return false;
  }

  private boolean jj_3_13() {
    if (jj_scan_token(NUMBER)) return true;
    Token xsp;
    while (true) {
      xsp = jj_scanpos;
      if (jj_3_11()) { jj_scanpos = xsp; break; }
    }
    return false;
  }

  private boolean jj_3_19() {
    if (jj_scan_token(OPEN_BRACKET)) return true;
    return false;
  }

  private boolean jj_3_16() {
    if (jj_scan_token(OPEN_BRACKET)) return true;
    return false;
  }

  private boolean jj_3_12() {
    if (jj_scan_token(NAME)) return true;
    return false;
  }

  private boolean jj_3R_8() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_12()) {
    jj_scanpos = xsp;
    if (jj_3_13()) {
    jj_scanpos = xsp;
    if (jj_3_14()) return true;
    }
    }
    return false;
  }

  private boolean jj_3_8() {
    if (jj_scan_token(DIVIDEBY)) return true;
    return false;
  }

  private boolean jj_3_2() {
    if (jj_scan_token(PLUS)) return true;
    return false;
  }

  private boolean jj_3_1() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_2()) {
    jj_scanpos = xsp;
    if (jj_3_3()) return true;
    }
    if (jj_3R_7()) return true;
    return false;
  }

  private boolean jj_3_17() {
    if (jj_scan_token(CLOSE_PAREN)) return true;
    return false;
  }

  private boolean jj_3_15() {
    if (jj_scan_token(OPEN_PAREN)) return true;
    return false;
  }

  private boolean jj_3R_9() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_15()) {
    jj_scanpos = xsp;
    if (jj_3_16()) return true;
    }
    if (jj_3R_11()) return true;
    return false;
  }

  private boolean jj_3_10() {
    if (jj_3R_9()) return true;
    return false;
  }

  private boolean jj_3R_11() {
    if (jj_3R_7()) return true;
    return false;
  }

  private boolean jj_3_23() {
    if (jj_scan_token(MINUS)) return true;
    if (jj_scan_token(NUMBER)) return true;
    return false;
  }

  private boolean jj_3_22() {
    if (jj_scan_token(NUMBER)) return true;
    return false;
  }

  private boolean jj_3_7() {
    if (jj_scan_token(TIMES)) return true;
    return false;
  }

  private boolean jj_3_14() {
    if (jj_3R_9()) return true;
    return false;
  }

  private boolean jj_3R_10() {
    if (jj_scan_token(NAME)) return true;
    if (jj_scan_token(EQUALS)) return true;
    return false;
  }

  private boolean jj_3_6() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_9()) {
    jj_scanpos = xsp;
    if (jj_3_10()) return true;
    }
    return false;
  }

  private boolean jj_3_9() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_7()) {
    jj_scanpos = xsp;
    if (jj_3_8()) return true;
    }
    if (jj_3R_8()) return true;
    return false;
  }

  private boolean jj_3_5() {
    if (jj_scan_token(MINUS)) return true;
    if (jj_scan_token(NUMBER)) return true;
    return false;
  }

  private boolean jj_3_21() {
    if (jj_scan_token(CLOSE_BRACKET)) return true;
    return false;
  }

  private boolean jj_3_4() {
    if (jj_3R_8()) return true;
    return false;
  }

  private boolean jj_3_3() {
    if (jj_scan_token(MINUS)) return true;
    return false;
  }

  private boolean jj_3R_7() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3_4()) {
    jj_scanpos = xsp;
    if (jj_3_5()) return true;
    }
    return false;
  }

  /** Generated Token Manager. */
  public ParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  /** Current token. */
  public Token token;
  /** Next token. */
  public Token jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  private int jj_gen;
  final private int[] jj_la1 = new int[0];
  static private int[] jj_la1_0;
  static {
      jj_la1_init_0();
   }
   private static void jj_la1_init_0() {
      jj_la1_0 = new int[] {};
   }
  final private JJCalls[] jj_2_rtns = new JJCalls[23];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  /** Constructor with InputStream. */
  public Parser(java.io.InputStream stream) {
     this(stream, null);
  }
  /** Constructor with InputStream and supplied encoding */
  public Parser(java.io.InputStream stream, String encoding) {
    try { jj_input_stream = new SimpleCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source = new ParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream) {
     ReInit(stream, null);
  }
  /** Reinitialise. */
  public void ReInit(java.io.InputStream stream, String encoding) {
    try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor. */
  public Parser(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new ParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Constructor with generated Token Manager. */
  public Parser(ParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  /** Reinitialise. */
  public void ReInit(ParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 0; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  static private final class LookaheadSuccess extends java.lang.Error { }
  final private LookaheadSuccess jj_ls = new LookaheadSuccess();
  private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0; Token tok = token;
      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }


/** Get the next Token. */
  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

/** Get the specific Token. */
  final public Token getToken(int index) {
    Token t = token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;

  private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      jj_entries_loop: for (java.util.Iterator<?> it = jj_expentries.iterator(); it.hasNext();) {
        int[] oldentry = (int[])(it.next());
        if (oldentry.length == jj_expentry.length) {
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              continue jj_entries_loop;
            }
          }
          jj_expentries.add(jj_expentry);
          break jj_entries_loop;
        }
      }
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  /** Generate ParseException. */
  public ParseException generateParseException() {
    jj_expentries.clear();
    boolean[] la1tokens = new boolean[18];
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 0; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 18; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.add(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = jj_expentries.get(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  /** Enable tracing. */
  final public void enable_tracing() {
  }

  /** Disable tracing. */
  final public void disable_tracing() {
  }

  private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 23; i++) {
    try {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0: jj_3_1(); break;
            case 1: jj_3_2(); break;
            case 2: jj_3_3(); break;
            case 3: jj_3_4(); break;
            case 4: jj_3_5(); break;
            case 5: jj_3_6(); break;
            case 6: jj_3_7(); break;
            case 7: jj_3_8(); break;
            case 8: jj_3_9(); break;
            case 9: jj_3_10(); break;
            case 10: jj_3_11(); break;
            case 11: jj_3_12(); break;
            case 12: jj_3_13(); break;
            case 13: jj_3_14(); break;
            case 14: jj_3_15(); break;
            case 15: jj_3_16(); break;
            case 16: jj_3_17(); break;
            case 17: jj_3_18(); break;
            case 18: jj_3_19(); break;
            case 19: jj_3_20(); break;
            case 20: jj_3_21(); break;
            case 21: jj_3_22(); break;
            case 22: jj_3_23(); break;
          }
        }
        p = p.next;
      } while (p != null);
      } catch(LookaheadSuccess ls) { }
    }
    jj_rescan = false;
  }

  private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) { p = p.next = new JJCalls(); break; }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

}