
public class Document {

	String words;
	String name;
	double[] vector;
	
	
	public Document(String words, String name) {
		this.words = words;
		this.name = name;
	}
	
	public void setWords(String words) {
		this.words = words;
	}
	
	public String getWords() {
		return words;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setVector(double[] vector) {
		this.vector = vector;
	}
	
	public double[] getVector() {
		return vector;
	}
}
