
/*
 * Jessye Lam
 * 500702091
 * CPS 842 - K-Means Document Clustering
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	public static HashMap<String, Document> documents = new HashMap<String, Document>();
	public static HashMap<String, Integer> dictionary = new HashMap<String, Integer>();
	public static HashMap<String, Double> idfMap = new HashMap<String, Double>();
	public static HashMap<String, Double> purity = new HashMap<String, Double>();
	public static HashMap<String, Double> ssd = new HashMap<String, Double>();
	public static List<Centroid> clusterResult;
	public static Set<String> set;
	public static int n;
	public static Set<String> setDictionary;

	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		// Reading the documents
		readDocs("athletics");
		readDocs("cricket");
		readDocs("football");
		readDocs("rugby");
		readDocs("tennis");
		set = documents.keySet();
		n = documents.size();
		/*
		 * Dictionary takes a few minutes to create so to save time I read it from a txt
		 * file createDictionary(); PrintWriter writer = new
		 * PrintWriter("dictionary.txt", "UTF-8"); write(writer);
		 */
		readDictionary();
		setDictionary = dictionary.keySet();
		// Calculates the document vectors for all documents
		calculateTFIDF();
		// K-Means clustering
		clusterResult = kMeansCluster(5);
		// Displaying Clusters
		for (Centroid c : clusterResult) {
			for (Document d : c.getClusteredDocs()) {
				System.out.print(d.getName() + ", ");
			}
			System.out.println();
		}
		calculateSSD(clusterResult);
		calculatePurity(clusterResult);
		printValues();
	}

	// Prints purity and ssd values to files
	public static void printValues() throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("ssd.txt", "UTF-8");
		PrintWriter pWriter = new PrintWriter("purity.txt", "UTF-8");
		Set<String> setPurity = purity.keySet();
		Set<String> setSSD = ssd.keySet();

		TreeMap<String, Double> tmP = new TreeMap<String, Double>(purity);
		Iterator<String> itP = tmP.keySet().iterator();

		while (itP.hasNext()) {
			String s = itP.next();
			pWriter.print(s + ": " + purity.get(s));
			pWriter.println();
		}

		TreeMap<String, Double> tmS = new TreeMap<String, Double>(ssd);
		Iterator<String> itS = tmS.keySet().iterator();

		while (itS.hasNext()) {
			String s = itS.next();
			writer.print(s + ": " + ssd.get(s));
			writer.println();
		}
		/*
		 * for (String key : setPurity) { pWriter.println(key + ": " + purity.get(key));
		 * } for(String key : setSSD) { writer.println(key + ": " + ssd.get(key)); }
		 */
		writer.close();
		pWriter.close();
	}

	// Calculate purity of each cluster
	public static void calculatePurity(List<Centroid> clusterResult) {
		int athletics;
		int football;
		int rugby;
		int tennis;
		int cricket;
		double p;
		String[] label = { "athletics", "football", "rugby", "tennis", "cricket" };

		for (Centroid c : clusterResult) {
			athletics = 0;
			football = 0;
			rugby = 0;
			tennis = 0;
			cricket = 0;
			for (Document d : c.getClusteredDocs()) {
				if (d.getName().contains("athletics"))
					athletics++;
				else if (d.getName().contains("football"))
					football++;
				else if (d.getName().contains("rugby"))
					rugby++;
				else if (d.getName().contains("tennis"))
					tennis++;
				else if (d.getName().contains("cricket"))
					cricket++;
			}
			int[] arr = { athletics, football, rugby, tennis, cricket };
			int largest = Integer.MIN_VALUE;
			int index = 0;
			for (int i = 0; i < arr.length; i++) {
				if (largest < arr[i]) {
					largest = arr[i];
					index = i;
				}
			}
			p = (double) largest / (double) c.getClusteredDocs().size();
			purity.put(c.getClusteredDocs().get(0).getName() + "(" + label[index] + ")", p);
		}

	}

	// Calculating tightness of cluster using sum of squared distances
	public static void calculateSSD(List<Centroid> clusterResult) {

		for (Centroid c : clusterResult) {
			Document mean = c.getClusteredDocs().get(0);
			double total = 0;
			for (Document d : c.getClusteredDocs()) {
				if (!mean.equals(d)) {
					for (int i = 0; i < d.getVector().length; i++) {
						total += Math.pow((d.getVector()[i] - mean.getVector()[i]), 2);
					}
					// documents.get(d.getName()).setSSD(total);
				}
			}
			ssd.put(c.getClusteredDocs().get(0).getName(), total);
		}

	}

	// K-Means clustering
	public static List<Centroid> kMeansCluster(int k) {
		List<Centroid> centroids = new ArrayList<Centroid>();
		List<Document> cluster, previous;
		Centroid c;
		int count = 0;
		List<String> rand = new ArrayList<String>();

		rand = randomDocs(k);
		// String[] s = new String[] { "athletics", "cricket", "football", "rugby",
		// "tennis" };

		// Initial centroids
		for (int i = 0; i < k; i++) {

			c = new Centroid();
			cluster = new ArrayList<Document>();
			cluster.add(documents.get(rand.get(i)));
			c.setClusteredDocs(cluster);
			centroids.add(c);

			/*
			 * c = new Centroid(); cluster = new ArrayList<Document>();
			 * cluster.add(documents.get(s[i] + "002")); c.setClusteredDocs(cluster);
			 * centroids.add(c);
			 */

		}

		System.out.println("First Iteration");
		List<Centroid> result = initCentroid(k);
		result = initCentroid(k);

		// Perform clustering until it doesn't change or 200 iterations
		while (true) {

			previous = getPreviousCenters(centroids);
			result = initClusters(rand, centroids);
			rand.clear();
			centroids = initCentroid(k);
			centroids = relocateCentroids(result);
			if (checkStop(previous, centroids) || count == 300) {
				System.out.println("Done");
				break;
			} else {
				System.out.println("Next Iteration");
				result = initCentroid(k);
			}
			count++;
		}
		return result;
	}

	// Returns a list of documents with the centroid documents
	// Used to see if the mean changes
	private static List<Document> getPreviousCenters(List<Centroid> centroids) {
		List<Document> temp = new ArrayList<Document>();
		for (Centroid c : centroids) {
			temp.add(c.getClusteredDocs().get(0));
		}
		return temp;
	}

	// Checks if the mean vector has changed after relocation
	private static boolean checkStop(List<Document> previous, List<Centroid> centroids) {

		for (int i = 0; i < centroids.size(); i++) {
			if (!centroids.get(i).getClusteredDocs().get(0).getVector().equals(previous.get(i).getVector()))
				return false;
		}
		return true;
	}

	// Calculates the mean vector and relocates the centroid
	// Returns list of clusters with new centroid
	public static List<Centroid> relocateCentroids(List<Centroid> centroids) {
		double[] tempVector = new double[idfMap.size()];
		Document mean;
		List<Centroid> result = centroids;

		for (int i = 0; i < centroids.size(); i++) {
			if (centroids.get(i).getClusteredDocs().size() > 0) {
				tempVector = new double[idfMap.size()];
				mean = new Document("", "Cluster" + i);
				// Calculate mean document vector
				for (int j = 0; j < idfMap.size(); j++) {
					double total = 0;
					for (Document doc : centroids.get(i).getClusteredDocs()) {
						total += doc.getVector()[j];
					}
					tempVector[j] = total / centroids.get(i).getClusteredDocs().size();
				}
				// Relocate centroid
				mean.setVector(tempVector);
				result.get(i).getClusteredDocs().set(0, mean);
			}
		}
		return result;
	}

	// Inserting documents into nearest centroid
	// Returns list of clusters with all documents inserted
	public static List<Centroid> initClusters(List<String> rand, List<Centroid> centroids) {
		List<Centroid> result = new ArrayList<Centroid>();
		List<Document> temp = new ArrayList<Document>();
		Centroid nearest, tempC;

		// Gets centroid
		for (Centroid c : centroids) {
			List<Document> list = new ArrayList<Document>();
			list.add(c.getClusteredDocs().get(0));
			tempC = new Centroid();
			tempC.setClusteredDocs(list);
			result.add(tempC);
		}

		// Finds nearest centroid for document and adds to cluster
		for (String key : set) {
			if (!rand.contains(key)) {
				/*
				 * if (count > 700) { Centroid c = empty(result); if (c != null) { temp =
				 * c.getClusteredDocs(); for (int i = 0; i < result.size(); i++) { if
				 * (centroids.get(i).equals(c)) { temp.add(documents.get(key));
				 * c.setClusteredDocs(temp); result.set(i, c); } } } } else {
				 */
				nearest = nearestCentroid(documents.get(key), result);
				temp = nearest.getClusteredDocs();
				for (int i = 0; i < centroids.size(); i++) {
					if (result.get(i).equals(nearest)) {
						temp.add(documents.get(key));
						nearest.setClusteredDocs(temp);
						result.set(i, nearest);
						// }
					}
				}
			}
		}
		return result;
	}

	// Checks if centroid contains any documents
	// Unused
	public static Centroid empty(List<Centroid> centroids) {
		for (Centroid c : centroids) {
			if (c.getClusteredDocs().size() < 2)
				return c;
		}
		return null;
	}

	// Returns nearest centroid
	public static Centroid nearestCentroid(Document doc, List<Centroid> centroids) {
		double minimum = Double.MIN_VALUE;
		Centroid nearest = null;

		// For every centroid, find the similarity
		for (Centroid centroid : centroids) {
			double similarity = findCosSim(doc.getVector(), centroid.getClusteredDocs().get(0).getVector());

			if (similarity > minimum) {
				minimum = similarity;
				nearest = centroid;
			}
		}
		return nearest;
	}

	// Used to reinitialize the centroids list
	public static List<Centroid> initCentroid(int k) {
		Centroid temp;
		List<Document> list;
		List<Centroid> tempC = new ArrayList<Centroid>();

		for (int i = 0; i < k; i++) {
			temp = new Centroid();
			list = new ArrayList<Document>();
			temp.setClusteredDocs(list);
			tempC.add(temp);
		}
		return tempC;
	}

	// Returns list of random documents
	private static List<String> randomDocs(int k) {
		List<String> rand = new ArrayList<String>();
		String doc;

		while (rand.size() < k) {
			doc = getRandomDoc();
			if (!rand.contains(doc))
				rand.add(doc);
		}
		return rand;
	}

	// Returns a random document
	public static String getRandomDoc() {
		String[] s = new String[] { "athletics", "cricket", "football", "rugby", "tennis" };
		int randomNum = ThreadLocalRandom.current().nextInt(0, 4 + 1);
		int randomNum2;
		DecimalFormat format = new DecimalFormat("000");

		switch (randomNum) {
		case 0:
			randomNum2 = ThreadLocalRandom.current().nextInt(1, 101 + 1);
			return s[randomNum] + format.format(randomNum2);
		case 1:
			randomNum2 = ThreadLocalRandom.current().nextInt(1, 124 + 1);
			return s[randomNum] + format.format(randomNum2);
		case 2:
			randomNum2 = ThreadLocalRandom.current().nextInt(1, 265 + 1);
			return s[randomNum] + format.format(randomNum2);
		case 3:
			randomNum2 = ThreadLocalRandom.current().nextInt(1, 147 + 1);
			return s[randomNum] + format.format(randomNum2);
		case 4:
			randomNum2 = ThreadLocalRandom.current().nextInt(1, 100 + 1);
			return s[randomNum] + format.format(randomNum2);
		default:
			return null;
		}

	}

	// Calculates the document vectors for every document
	private static void calculateTFIDF() {
		calculateIDF();
		int i;
		double[] vector;
		double tf, weight;

		for (String key : set) {
			i = 0;
			vector = new double[idfMap.size()];

			for (String word : setDictionary) {
				tf = 1 + Math.log10(termFreq(key, word));
				if (tf < 0)
					tf = 0;
				weight = tf * idfMap.get(word);
				vector[i] = weight;
				i++;
			}
			documents.get(key).setVector(vector);
		}
	}

	// Calculates the idf value for every term in the dictionary
	private static void calculateIDF() {

		for (String key : setDictionary) {
			double idf = Math.log10(n / dictionary.get(key));
			if (idf < 0)
				idf = 0;
			idfMap.put(key, idf);
		}

	}

	// Return similarity between two vectors
	public static double findCosSim(double[] vector1, double[] vector2) {
		double dotProduct = getDotProduct(vector1, vector2);
		double magV1 = getMagnitude(vector1);
		double magV2 = getMagnitude(vector2);
		double result = dotProduct / (magV1 * magV2);
		if (Double.isNaN(result))
			return 0;
		else
			return result;
	}

	// Return magnitude of a vector
	public static double getMagnitude(double[] vector) {
		double sum = 0;

		for (int i = 0; i < vector.length; i++) {
			sum += Math.pow(vector[i], 2);
		}
		return Math.sqrt(sum);
	}

	// Return dot product of two vectors
	public static double getDotProduct(double[] vector1, double[] vector2) {
		double sum = 0;

		for (int i = 0; i < vector1.length; i++) {
			sum += vector1[i] * vector2[i];
		}
		return sum;
	}

	// Reads the dictionary file and creates a dictionary
	public static void readDictionary() throws FileNotFoundException {
		File file = new File("dictionary.txt");
		Scanner sc = new Scanner(file);
		String line;

		while (sc.hasNextLine()) {
			line = sc.nextLine();
			String[] s = line.split(": ");
			dictionary.put(s[0], Integer.parseInt(s[1]));
		}
		sc.close();
	}

	// Prints out dictionary to dictionary.txt
	public static void write(PrintWriter fileDictionary) {
		TreeMap<String, Integer> tm = new TreeMap<String, Integer>(dictionary);
		Iterator<String> it = tm.keySet().iterator();

		while (it.hasNext()) {
			String s = it.next();
			fileDictionary.print(s + ": " + dictionary.get(s));
			fileDictionary.println();
		}
		System.out.println("Dictionary Complete");
	}

	// Reads documents and stores into hashmap and document object
	// Key is the document name and value is the document object
	public static void readDocs(String type) throws FileNotFoundException {
		File file = new File(type + "/001.txt");
		Scanner sc;
		int count = 1;
		DecimalFormat format = new DecimalFormat("000");
		String path, line, content;

		while (file.exists()) {
			sc = new Scanner(file);
			content = "";

			while (sc.hasNextLine()) {
				line = sc.nextLine();
				if (line.length() > 0)
					content = content.concat(line).concat(" ");
			}

			content = removeStopwordAndStem(content);
			Document doc = new Document(content, type + format.format(count));
			documents.put(type + format.format(count), doc);
			count++;
			path = type + "/" + format.format(count) + ".txt";
			file = new File(path);
		}
	}

	// Returns string with stopwords removed and stemming
	public static String removeStopwordAndStem(String s) {
		String s2 = s.toLowerCase();
		Stemmer stem = new Stemmer();
		String word = null;
		char[] wordArr = null;
		StringBuilder sb = new StringBuilder();
		CharSequence charAdd = " ";

		s2 = s2.replaceAll("[0123456789]", "");
		s2 = s2.replaceAll("[+.()<>=?;/{}\"+_`!@#$|%&*^:,]", "");
		s2 = s2.replaceAll("\\[[^\\[]*\\]", "");
		s2 = s2.replaceAll("--", " ");
		s2 = s2.replaceAll(" -", " ");
		s2 = s2.replaceAll("- ", " ");
		s2 = s2.replaceAll(" '", " ");
		s2 = s2.replaceAll("' ", " ");

		Pattern p = Pattern.compile(
				"\\b(i|a|about|an|and|are|as|at|be|by|for|from|how|in|is|it|of|on|or|that|the|this|to|was|what|when|where|who|will|with|the)\\b\\s?");
		Matcher m = p.matcher(s2);
		String s3 = m.replaceAll("");

		ArrayList<String> temp = new ArrayList<>(Arrays.asList(s3.split("\\s+")));

		for (int j = 0; j < temp.size(); j++) {
			word = temp.get(j);
			wordArr = word.toCharArray();
			for (int k = 0; k < wordArr.length; k++) {
				stem.add(wordArr[k]);
			}
			stem.stem();
			temp.set(j, stem.toString());
		}

		for (int k = 0; k < temp.size(); k++) {
			sb.append(temp.get(k));
			if (k < temp.size() - 1)
				sb.append(charAdd);
		}
		return sb.toString();
	}

	// Returns document frequency of a term
	public static int docFreq(String s) {
		int count = 0;

		for (String key : set) {
			if (exactMatch(documents.get(key).getWords(), s))
				count++;
		}
		return count;
	}

	// Returns true if term exists inside a document
	public static boolean exactMatch(String source, String term) {
		for (String word : source.split("\\s+")) {
			if (word.equals(term))
				return true;
		}
		return false;
	}

	// Returns term frequency of a term inside a document
	public static int termFreq(String key, String term) {
		int count = 0;
		if (exactMatch(documents.get(key).getWords(), term)) {
			String[] s = documents.get(key).getWords().split("\\s+");
			for (int i = 0; i < s.length; i++) {
				// for (String word : documents.get(key).getWords().split("\\s+")) {
				if (s[i].equals(term))
					count++;
			}
		}
		return count;
	}

	// Creates dictionary from all documents
	public static void createDictionary() {

		for (String key : set) {
			String[] s = documents.get(key).getWords().split("\\s+");
			for (int j = 0; j < s.length; j++) {
				if (!s[j].equals("") & !dictionary.containsKey(s[j])) {
					dictionary.put(s[j], docFreq(s[j]));
				}
			}
		}
	}

}
