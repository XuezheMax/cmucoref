package cmucoref.document;

public class Lexicon {
	public int id = 0;
	public String form = null;
	public String lemma = null;
	public String cpostag = null;
	public String postag = null;
	
	public int basic_head = 0;
	public String basic_deprel = null;
	
	public int collapsed_head = 0;
	public String collapsed_deprel = null;
	
	public String ner;
	
	public Lexicon(){
		id = 0;
	}
	
	public Lexicon(int id){
		this.id = id;
	}
	
	public Lexicon(int id, String form, String lemma, String postag){
		this(id);
		this.form = form;
		this.lemma = lemma;
		this.postag = postag;
	}
	
	public Lexicon(int id, String form, String lemma, String cpostag, String postag){
		this(id, form, lemma, postag);
		this.cpostag = cpostag;
	}
	
	public Lexicon(int id, String form, String lemma, String cpostag, String postag, String ner){
		this(id, form, lemma, cpostag, postag);
		this.ner = ner;
	}
	
	public Lexicon(int id, String form, String lemma, String cpostag, String postag, 
			String ner, int basic_head, String basic_deprel, int collapsed_head, String collapsed_deprel){
		this(id, form, lemma, cpostag, postag, ner);
		this.basic_head = basic_head;
		this.basic_deprel = basic_deprel;
		this.collapsed_head = collapsed_head;
		this.collapsed_deprel = collapsed_deprel;
	}
	
	public Lexicon(Lexicon lex){
		this(lex.id, lex.form, lex.lemma, lex.cpostag, lex.postag, 
				lex.ner, lex.basic_head, lex.basic_deprel,
				lex.collapsed_head, lex.collapsed_deprel);
	}
}
