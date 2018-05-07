
import java.io.*;
import java.util.*;

/*import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.util.*;
import edu.stanford.nlp.pipeline.*;*/
import edu.stanford.nlp.simple.*;

public class NaiveBayes {
	static class Occurrence {
		int add;
		int mult;
		int pos;
		
		public Occurrence(int add, int mult, int pos) {
			this.add = add;
			this.mult = mult;
			this.pos = pos;
		}
		
		public void incAdd() {
			this.add++;
		}
		
		public void incMult() {
			this.mult++;
		}
		
		public double addProb() {
			return ((double) add) / ((double) (add + mult));
		}

		public double multProb() {
			return ((double) mult) / ((double) (add + mult));
		}
		
		public String toString() {
			return "add = " + add + " mult = " + mult
					+ " POS = " + ((pos == NOUN)? "noun" : "verb");
		}
	}
	
	private static final int FAIL = 1;
	private static final int ADD = 1;
	private static final int MULT = 2;
	private static final int NAH = 0;
	private static final int NOUN = 1;
	private static final int VERB = 2;
	private static HashMap<String, Occurrence> hm = new HashMap<String, Occurrence>();
	
	public static void main(String[] args) {
		//generateTest();
		try {
			train();
			classifyAll();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//String s = "Jason found 49 seashells and 48 starfish on the beach . He gave 13 of the seashells to Tim . "
				//+ "How many seashells does Jason now have ? ";
		//System.out.println(classify(s));
		
		//String[] a = {"I am good"};
		/*String s = "Jason found 49 seashells and 48 starfish on the beach . He gave 13 of the seashells to Tim . "
				+ "How many seashells does Jason now have ? ";
		AnnotationPipeline pipeline = new AnnotationPipeline();
		pipeline.addAnnotator(new TokenizerAnnotator(false, "en"));
		pipeline.addAnnotator(new WordsToSentencesAnnotator(false));
		pipeline.addAnnotator(new POSTaggerAnnotator(false));

		Annotation document = new Annotation(s);
		pipeline.annotate(document);
		for (CoreMap sentence: document.get(CoreAnnotations.SentencesAnnotation.class)) {
			for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
				String tokenText = token.get(TextAnnotation.class);
				String tokenPOS = token.get(PartOfSpeechAnnotation.class);
				System.out.printf("%s: %s \n", tokenText, tokenPOS);
			}
		} */
		
	}
	public static void generateTest() {
		String add1 = "E:\\project\\701project\\add.txt";
		String mult1 = "E:\\project\\701project\\mult1.txt";
		String mult2 = "E:\\project\\701project\\mult2.txt";
		String outAll = "E:\\project\\701project\\allQuestions.txt";
		String line;
		try {
			FileWriter fw = new FileWriter(outAll);
			BufferedWriter bw = new BufferedWriter(fw);
			
			FileReader fr1 = new FileReader(add1);
			BufferedReader br1 = new BufferedReader(fr1);
			FileReader fr2 = new FileReader(mult1);
			BufferedReader br2 = new BufferedReader(fr2);
			FileReader fr3 = new FileReader(mult2);
			BufferedReader br3 = new BufferedReader(fr3);

			while((line = br1.readLine()) != null) {
				bw.write(line);
				bw.newLine();
				bw.write(Integer.toString(ADD));
				bw.newLine();
			}
			while((line = br2.readLine()) != null) {
				bw.write(line);
				bw.newLine();
				bw.write(Integer.toString(MULT));
				bw.newLine();
			}
			while((line = br3.readLine()) != null) {
				bw.write(line);
				bw.newLine();
				bw.write(Integer.toString(MULT));
				bw.newLine();
			}
			
			br1.close();
			br2.close();
			br3.close();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static int checkPOS(String pos) {
		String[] verbs = {"VB", "VBD", "VBG", "VBN", "VBP", "VBZ"};
		String[] nouns = {"NN", "NNS"};
		for (int i = 0; i < verbs.length; i++) {
			if (pos.equals(verbs[i])) return VERB;
		}
		for (int i = 0; i < nouns.length; i++) {
			if (pos.equals(nouns[i])) return NOUN;
		}
		return NAH;
	}
	public static void train() throws Exception {
		String trainFile = "E:\\project\\701project\\allQuestions.txt";
		
		String line;
		FileReader fr = new FileReader(trainFile);
		BufferedReader br = new BufferedReader(fr);
		while((line = br.readLine()) != null) {
			String category = br.readLine();
			int probType;
			if (category.equals(Integer.toString(ADD))) probType = ADD;
			else probType = MULT;
			
			Document doc = new Document(line);
			HashSet<String> seen = new HashSet<String>();
			for (Sentence sent : doc.sentences()) {
				int len = sent.length();
				List<String> tags = sent.posTags();
				List<String> lemmas = sent.lemmas();
				//System.out.println(tags);
				//System.out.println(lemmas);
				for (int i = 0; i < len; i++) {
					if (checkPOS(tags.get(i)) == NAH) continue;
					int posCategory = checkPOS(tags.get(i));
					String word = lemmas.get(i);
					if (seen.contains(word)) continue;
					if (!hm.containsKey(word)) {
						Occurrence oc = new Occurrence(0, 0, posCategory);
						if (probType == ADD) oc.incAdd();
						else oc.incMult();
						hm.put(word, oc);
						//System.out.println(word);
					}
					else {
						Occurrence oc = hm.get(word);
						if (probType == ADD) oc.incAdd();
						else oc.incMult();
					}
					seen.add(word);
				}
			}
			
		}
		/* for (Map.Entry<String, Occurrence> entry : hm.entrySet()) {
			String word = entry.getKey();
			Occurrence oc = entry.getValue();
			System.out.printf("%s : %s \n", word, oc.toString());
		} */
		br.close();
	}
	
	public static int classify(String question, boolean debug) {
		if (debug) System.out.println(question);
		Document doc = new Document(question);
		HashSet<String> seen = new HashSet<String>();
		double verbProbAdd = 0, nounProbAdd = 0, verbProbMult = 0, nounProbMult = 0;
		int nounCount = 0, verbCount = 0;
		double probAdd = 0, probMult = 0;
		
		for (Sentence sent : doc.sentences()) {
			int len = sent.length();
			List<String> tags = sent.posTags();
			List<String> lemmas = sent.lemmas();
			if (debug) System.out.println(tags);
			if (debug) System.out.println(lemmas);
			for (int i = 0; i < len; i++) {
				if (checkPOS(tags.get(i)) == NAH) continue;
				int posCategory = checkPOS(tags.get(i));
				String word = lemmas.get(i);
				if (seen.contains(word)) continue;
				if (!hm.containsKey(word)) continue;
				
				Occurrence oc = hm.get(word);
				if (posCategory == NOUN) {
					nounProbAdd += oc.addProb();
					nounProbMult += oc.multProb();
					nounCount++;
				}
				if (posCategory == VERB) {
					verbProbAdd += oc.addProb();
					verbProbMult += oc.multProb();
					verbCount++;
				}
			}
		}
		if ((verbCount == 0) || (nounCount == 0)) {
			if (debug) System.out.println("FAILED : verb / noun count = 0");
			//System.out.println(question);
			return FAIL;
		}
		
		verbProbAdd = verbProbAdd / verbCount;
		verbProbMult = verbProbMult / verbCount;
		nounProbAdd = nounProbAdd / nounCount;
		nounProbMult = nounProbMult / nounCount;
		
		probAdd = verbProbAdd * nounProbAdd;
		probMult = verbProbMult * nounProbMult;
		if (debug) {
			System.out.printf("verbProbAdd = %.4f, nounProbAdd = %.4f, "
					+ "verbProbMult = %.4f, nounProbMult = %.4f \n", 
					verbProbAdd, nounProbAdd, verbProbMult, nounProbMult);
			System.out.printf("probAdd = %.4f, probMult = %.4f \n", 
					probAdd, probMult);
			System.out.println();
		}
		
		if (probAdd < probMult) return MULT;
		else return ADD;
	}
	
	public static void classifyAll() throws Exception {
		String testFile = "E:\\project\\701project\\someQuestions.txt";
		String outAdd = "E:\\project\\701project\\allOutputAdd.txt";
		String outMult = "E:\\project\\701project\\allOutputMult.txt";
		String line;
		int correct = 0;
		int wrong = 0;
		
		FileWriter fw1 = new FileWriter(outAdd);
		BufferedWriter bw1 = new BufferedWriter(fw1);
		FileWriter fw2 = new FileWriter(outMult);
		BufferedWriter bw2 = new BufferedWriter(fw2);
		
		FileReader fr = new FileReader(testFile);
		BufferedReader br = new BufferedReader(fr);
		while((line = br.readLine()) != null) {
			String category = br.readLine();
			int estimation = classify(line, true);
			
			if ((estimation == ADD) || (estimation == FAIL)) {
				bw1.write(line);
				bw1.newLine();
			}
			else if (estimation == MULT) {
				bw2.write(line);
				bw2.newLine();
			}
			
			if (Integer.toString(estimation).equals(category)) {
				correct++;
			}
			else {
				wrong++;
				classify(line, true);
			}
		}
		double accuracy = ((double) correct) / ((double) (correct + wrong));
		System.out.println(correct);
		System.out.println(correct + wrong);
		System.out.println(accuracy);
		br.close();
		bw1.close();
		bw2.close();
	}
}
