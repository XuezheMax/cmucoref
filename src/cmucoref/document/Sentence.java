package cmucoref.document;

public class Sentence {
	private int id = 0;
	private String pennTree = null;
	private Lexicon[] lexicons = null;
	
	public Sentence(){}
	
	public Sentence(int length, int id){
		lexicons = new Lexicon[length];
		this.id = id;
	}
	
	public Sentence(Lexicon[] lexicons, String pennTree, int id){
		this.lexicons = lexicons;
		this.pennTree = pennTree;
		this.id = id;
	}
	
	public int length(){
		return lexicons.length;
	}
	
	public int getId(){
		return id;
	}
	
	public Lexicon[] getLexicons(){
		return lexicons;
	}
	
	public String getPennTree(){
		return pennTree;
	}
	
	public Lexicon getLexicon(int index){
		return lexicons[index];
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public void setLexicon(Lexicon lexicon, int index){
		lexicons[index] = lexicon;
	}
	
	public void setPennTree(String pennTree){
		this.pennTree = pennTree;
	}
	
	public void setLexicons(Lexicon[] lexicons){
		this.lexicons = lexicons;
	}
	
	public String getRawText(){
		StringBuilder text = new StringBuilder();
		for(int i = 1; i < lexicons.length; ++i){
			if(i > 1){
				text.append(" ");
			}
			text.append(lexicons[i].form);
		}
		return text.toString();
	}
}
