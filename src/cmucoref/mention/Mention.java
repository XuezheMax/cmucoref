package cmucoref.mention;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.exception.MentionException;

import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Dictionaries.Animacy;
import edu.stanford.nlp.dcoref.Dictionaries.Gender;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;
import edu.stanford.nlp.dcoref.Dictionaries.Number;
import edu.stanford.nlp.dcoref.Dictionaries.Person;

public class Mention implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final Comparator<Mention> headIndexOrderComparator = new MentionComparatorHeadIndexOrder();
	public static final Comparator<Mention> postTreeOrderComparator = new MentionComparatorPostTreeOrder();
	
	public int startIndex;
	public int endIndex;
	public int headIndex;
	public int mentionID = -1;
	
	public Lexicon headword = null;
	public String headString = null;
	public MentionType mentionType;
	public Number number;
	public Gender gender;
	public Animacy animacy;
	public Person person;
	
	public List<Mention> listMember = null;
	public Mention belongTo = null;
	public List<Mention> appositions = null;
	public Mention apposTo = null;
	public List<Mention> predicateNominatives = null;
	public Mention predNomiTo = null;
	public List<Mention> relativePronouns = null;
	public Mention relPronTo = null;
	public List<Mention> spanMatchs = null;
	public int closestSpanMatchPos = -1;
	public MentionType closestSpanMatchType = null;
	public boolean spanMatch = false;
	public int localAttrMatchOfSent = -1;
	public int localAttrMatchOfMention = -1;
	public MentionType localAttrMatchType = null;
	
	public int sentID = -1;
	
	public Mention antecedent = null;
	public MentionCluster corefCluster = null;
	public boolean isRepresentative = false;
	
	public Mention(){
		
	}
	
	public Mention(int mentionID, int startIndex, int endIndex){
		this.mentionID = mentionID;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		
		closestSpanMatchPos = -1;
		closestSpanMatchType = null;
		spanMatch = false;
		
		localAttrMatchOfSent = -1;
		localAttrMatchOfMention = -1;
		localAttrMatchType = null;
		
		antecedent = null;
		this.corefCluster = null;
		this.isRepresentative = false;
	}
	
	public Mention(int mentionID, int startIndex, int endIndex, int headIndex, int sentID){
		this(mentionID, startIndex, endIndex);
		this.headIndex = headIndex;
		this.sentID = sentID;
	}
	
	public Mention(edu.stanford.nlp.dcoref.Mention mention, int sentID){
		this(mention.mentionID, mention.startIndex + 1, mention.endIndex + 1, mention.headIndex + 1, sentID);
	}
	
	public boolean cover(Mention mention){
		return ((this.sentID == mention.sentID) 
				&& (this.startIndex <= mention.startIndex && this.endIndex >= mention.endIndex) 
				&& (this.endIndex - this.startIndex > mention.endIndex - mention.startIndex));
	}
	
	public boolean overlap(Mention mention){
		if(this.startIndex < mention.startIndex){
			return this.endIndex > mention.startIndex && this.endIndex < mention.endIndex;
		}
		else if(this.startIndex > mention.startIndex){
			return mention.endIndex > this.startIndex && mention.endIndex < this.endIndex;
		}
		else{
			return false;
		}
	}
	
	public void setRepres(){
		this.isRepresentative = true;
		this.corefCluster = new MentionCluster(this);
	}
	
	public void setAntec(Mention antec){
		this.antecedent = antec;
		this.corefCluster = antec.corefCluster;
		this.corefCluster.add(this);
	}
	
	public boolean isSingleton(){
		return this.corefCluster == null || this.corefCluster.isSingleton();
	}
	
	public boolean corefTo(Mention mention){
		return this.corefCluster != null 
				&& mention.corefCluster != null 
				&& this.corefCluster.clusterID == mention.corefCluster.clusterID;
	}
	
	public boolean isNominative(){
		return this.mentionType == MentionType.NOMINAL;
	}
	
	public boolean isProper(){
		return this.mentionType == MentionType.PROPER;
	}
	
	public boolean isPronominal(){
		return this.mentionType == MentionType.PRONOMINAL;
	}
	
	public boolean isList(){
		return this.mentionType == MentionType.LIST;
	}
	
	public int getDistOfMention(Mention mention){
//		int distOfSent = this.getDistOfSent(mention);
//		int lowerbound = distOfSent * 10;
//		int upperbound = (distOfSent + 1) * 10 - 1;
//		
//		int distOfMention = (this.mentionID - mention.mentionID) / 5;
//		if(distOfMention < lowerbound){
//			distOfMention = lowerbound;
//		}
//		if(distOfMention > upperbound){
//			distOfMention = upperbound;
//		}
//		return distOfMention;
		return this.mentionID - mention.mentionID;
	}
	
	public int getDistOfSent(Mention mention){
		int distOfSent = (this.sentID - mention.sentID) / 4;
		
		if(distOfSent > 9){
			distOfSent = 9;
		}
		return distOfSent;
	}
	
	public boolean numberAgree(Mention mention){
//		if(this.mentionType == MentionType.LIST && mention.mentionType == MentionType.LIST){
//			return true;
//		}
		
		if(this.number != Number.UNKNOWN && mention.number != Number.UNKNOWN){
			return this.number == mention.number ? true : false;
		}
		else{
			return true;
		}
	}
	
	public boolean genderAgree(Mention mention){
		if(this.gender != Gender.UNKNOWN && mention.gender != Gender.UNKNOWN){
			return this.gender == mention.gender ? true : false;
		}
		else{
			return true;
		}
	}
	
	public boolean animateAgree(Mention mention){
		if(this.animacy == Animacy.UNKNOWN || mention.animacy == Animacy.UNKNOWN){
			return true;
		}
		else{
			return this.animacy == mention.animacy ? true : false;
		}
	}
	
	public boolean personAgree(Mention mention){
		if(this.person == Person.UNKNOWN || mention.person == Person.UNKNOWN){
			return true;
		}
		else{
			return this.person == mention.person ? true : false;
		}
	}
	
	public boolean NERAgree(Mention mention){
		if(this.headword.ner.equals("O") || mention.headword.ner.equals("O")){			
			return true;
		}
		else{
			return this.headword.ner.equals(mention.headword.ner) ? true : false;
		}
	}
	
	public boolean attrAgree(Mention mention){
		if(!(this.numberAgree(mention))){
			return false;
		}
		else if(!(this.genderAgree(mention))){
			return false;
		}
		else if(!this.isPronominal() && !mention.isPronominal() && !(this.animateAgree(mention))){
			return false;
		}
		else if(this.isPronominal() && mention.isPronominal() && !(this.personAgree(mention))){
			return false;
		}
		else if(this.isProper() && mention.isProper() && !(this.NERAgree(mention))){
			return false;
		}
		else{
			return true;
		}
	}
	
	public boolean isPureNerMention(Sentence sent){
		if(headword.ner.equals("O")){
			return false;
		}
		
		for(int i = this.startIndex; i < this.endIndex; ++i){
			if(!(sent.getLexicon(i).ner.equals(headword.ner))){
				return false;
			}
		}
		return true;
	}
	
	public void addListMember(Mention member) {
		// Don't handle nested lists
		if(member.listMember != null && member.listMember.size() > 0){
			return;
		}
		
		if(listMember == null){
			listMember = new ArrayList<Mention>();
		}
		listMember.add(member);
		member.belongTo = this;
		//this.mentionType = MentionType.LIST;
		this.number = Number.PLURAL;
	}
	
	public boolean isListMemberOf(Mention m){
		return belongTo == null ? false : this.belongTo.equals(m);
	}
	
	public void addSpanMatch(Mention spanMatch, Document doc){
		if(this.spanMatchs == null){
			this.spanMatchs = new ArrayList<Mention>();
		}
		if(spanMatch.spanMatchs != null){
			this.spanMatchs.addAll(spanMatch.spanMatchs);
		}
		this.spanMatchs.add(spanMatch);
		this.spanMatch = true;
		this.closestSpanMatchPos = this.getDistOfSent(spanMatch);
		this.closestSpanMatchType = spanMatch.mentionType;
	}
	
	public void addApposition(Mention appo) throws MentionException{
		if(appo.apposTo != null){
			if(this.equals(appo.apposTo) || appo.apposTo.isListMemberOf(this)) {
				return;
			}
			else{
				throw new MentionException(appo.headString + " is apposition to another mention " + appo.apposTo.mentionID);
			}
		}
		
		if(appo.headword.basic_head != this.headIndex || !appo.headword.basic_deprel.equals("appos")){
			return;
		}
		
		if(appositions == null){
			appositions = new ArrayList<Mention>();
		}
		
		appositions.add(appo);
		appo.apposTo = this;
	}
	
	public void addPredicativeNominative(Mention predMomi) throws MentionException{
		if(predMomi.predNomiTo != null){
			return;
		}
		
		if(predicateNominatives == null){
			predicateNominatives = new ArrayList<Mention>();
		}
		
		predicateNominatives.add(predMomi);
		predMomi.predNomiTo = this;
	}
	
	public void addRelativePronoun(Mention relPron) throws MentionException{
		throw new MentionException(relPron.headString + " is relative pronoun to mention " + this.headString);
	}
	
	private static final List<String> entityWordsToExclude =
			Arrays.asList(new String[]{ "the","this", "mr.", "miss", "mrs.", "dr.", "ms.", "inc.", "ltd.", "corp.", "'s"});
	
	public boolean spanMatch(Sentence sent, Mention mention, Sentence sentOfM){
		if(this.isPronominal() || mention.isPronominal()){
			System.err.println("error: head matching does not apply for pronominals");
			System.exit(0);
		}
		return this.headMatch(sent, mention, sentOfM)
				&& (this.wordsInclude(sent, mention, sentOfM) || this.relaxedSpanMatch(sent, mention, sentOfM));
	}
	
	public boolean headMatch(Sentence sent, Mention mention, Sentence sentOfM){
		if(this.isPronominal() || mention.isPronominal()){
			System.err.println("error: head matching does not apply for pronominals");
			System.exit(0);
		}
		if(this.headString.equals(mention.headString)){
			return true;
		}
		else if(this.exactSpanMatch(sent, mention, sentOfM)){
			return true;
		}
		else if(this.relaxedSpanMatch(sent, mention, sentOfM)){
			return true;
		}
		else if(this.isProper() && mention.isProper()){
			return this.isAcronymTo(mention, sentOfM) || mention.isAcronymTo(this, sent);
		}
		else{
			return false;
		}
	}
	public boolean wordsInclude(Sentence sent, Mention mention, Sentence sentOfM){
		if(this.isPronominal() || mention.isPronominal()){
			System.err.println("error: word including does not apply for pronominals");
			System.exit(0);
		}
		
		Set<String> wordsOfThis = new HashSet<String>();
		for(int i = this.startIndex; i < this.endIndex; ++i){
			wordsOfThis.add(sent.getLexicon(i).form.toLowerCase());
		}
		
		Set<String> wordsExceptStopWords = new HashSet<String>();
		for(int i = mention.startIndex; i < mention.endIndex; ++i){
			wordsExceptStopWords.add(sentOfM.getLexicon(i).form.toLowerCase());
		}
		wordsExceptStopWords.removeAll(entityWordsToExclude);
		//wordsExceptStopWords.remove(mention.headString);
		
		if(wordsOfThis.containsAll(wordsExceptStopWords)){
			return true;
		}
		else if(this.isProper() && mention.isProper()){
			return this.isAcronymTo(mention, sentOfM) || mention.isAcronymTo(this, sent);
		}
		else{
			return false;
		}
	}
	
	public boolean isAcronymTo(Mention mention, Sentence sent){
		if(!this.isProper() || !mention.isProper()){
			System.err.println("error: acronym applies for propers");
			System.exit(0);
		}
		
		if(this.endIndex - this.startIndex != 1){
			return false;
		}
		
		if(mention.endIndex - mention.startIndex == 1 && this.headString.equals(mention.headString)){
			return false;
		}
		
		String acronym = this.headword.form;
		for(int i = 0; i < acronym.length(); ++i){
			if(acronym.charAt(i) < 'A' || acronym.charAt(i) > 'Z'){
				return false;
			}
		}
		
		int acronymPos = 0;
		for(int i = mention.startIndex; i < mention.endIndex; ++i){
			String word = sent.getLexicon(i).form;
			if(word.equals(acronym) && (mention.endIndex - mention.startIndex) > 1) {
				return false;
			}
			
			for(int ch = 0; ch < word.length(); ++ch){
				if (word.charAt(ch) >= 'A' && word.charAt(ch) <= 'Z') {
					if(acronymPos >= acronym.length()){
						return false;
					}
					if(acronym.charAt(acronymPos) != word.charAt(ch)){
						return false;
					}
					acronymPos++;
				}
			}
		}
		
		if(acronymPos != acronym.length()){
			return false;
		}
		return true;
	}
	
	public boolean relaxedSpanMatch(Sentence sent, Mention mention, Sentence sentOfM){
		if(this.isPronominal() || mention.isPronominal()){
			System.err.println("error: relaxed matching does not apply for pronominals");
			System.exit(0);
		}
		
		String anaphSpanBeforeHead = this.getRelaxedSpan(sent).toLowerCase();
		String antecSpanBeforeHead = mention.getRelaxedSpan(sentOfM).toLowerCase();
		boolean relaxedSpanMatch = anaphSpanBeforeHead.equals(antecSpanBeforeHead) 
				|| anaphSpanBeforeHead.equals(antecSpanBeforeHead + " 's")
				|| antecSpanBeforeHead.equals(anaphSpanBeforeHead + " 's");
		return relaxedSpanMatch;
	}
	
	public boolean exactSpanMatch(Sentence sent, Mention mention, Sentence sentOfM){
		if(this.isPronominal() || mention.isPronominal()){
			System.err.println("error: exact matching does not apply for pronominals");
			System.exit(0);
		}
		
		String anaphSpan = this.getSpan(sent).toLowerCase();
		String antecSpan = mention.getSpan(sentOfM).toLowerCase();
		boolean exactSpanMatch = anaphSpan.equals(antecSpan) 
				|| anaphSpan.equals(antecSpan + " 's") 
				|| antecSpan.equals(anaphSpan + " 's");
		return exactSpanMatch;
	}
	
	public String getSpan(Sentence sent){
		StringBuilder span = new StringBuilder();
		for(int i = startIndex; i < endIndex; ++i){
			if(i > startIndex){
				span.append(" ");
			}
			span.append(sent.getLexicon(i).form);
		}
		return span.toString();
	}
	
	public String getRelaxedSpan(Sentence sent){
		StringBuilder span = new StringBuilder();
		int posComma = -1;
		int posWH = -1;
		for(int i = startIndex; i < endIndex; ++i){
			String postag = sent.getLexicon(i).postag;
			if(posComma == -1 && postag.equals(",")){
				posComma = i;
			}
			if(posWH == -1 && postag.startsWith("W")){
				posWH = i;
			}
		}
		
		int posEnd = this.endIndex;
		if(posComma != -1 && this.headIndex < posComma){
			posEnd = posComma;
		}
		if(posComma==-1 && posWH != -1 && this.headIndex < posWH){
			posEnd = posWH;
		}
		
		for(int i = startIndex; i < posEnd; ++i){
			if(i > startIndex){
				span.append(" ");
			}
			span.append(sent.getLexicon(i).form);
		}
		return span.toString();
	}
	
	private void getHeadword(Sentence sent){
		if(headword == null){
			Lexicon lex = sent.getLexicon(headIndex);
			headword= new Lexicon(lex);
			headString = headword.form.toLowerCase();
		}
		if(!headword.ner.equals("O")){
			// make sure that the head of a NE is not a known suffix, e.g., Corp.
			int start = headIndex;
			while(start >= 0){
				String head = sent.getLexicon(start).form.toLowerCase();
				if (knownSuffix(head)) {
					start --;
				}
				else {
					this.headString = head;
					this.headIndex = start;
					this.headword = new Lexicon(sent.getLexicon(headIndex));
					break;
				}
			}
		}
	}
	
	public void process(Sentence sent, Dictionaries dict){
		getHeadword(sent);
		setType(sent, dict);
		setNumber(sent, dict);
		setAnimacy(sent, dict);
		setGender(sent, dict, getGender(sent, dict));
		setPerson(sent, dict);
	}
	
	private static final String [] commonNESuffixes = {
		"Corp", "Co", "Inc", "Ltd"
	};

	private static boolean knownSuffix(String s) {
		if(s.endsWith(".")) {
			s = s.substring(0, s.length() - 1);
		}
		for(String suff: commonNESuffixes){
			if(suff.equalsIgnoreCase(s)){
				return true;
			}
		}
		return false;
	}
	
	private void setType(Sentence sent, Dictionaries dict){
		if(headword.postag.startsWith("PRP") || 
				( (endIndex - startIndex) == 1 && headword.ner.equals("O") && 
				( dict.allPronouns.contains(headString) || dict.relativePronouns.contains(headString) ))){
			mentionType = MentionType.PRONOMINAL;
		}
		else if(!headword.ner.equals("O") || headword.postag.startsWith("NNP")){
			mentionType = MentionType.PROPER;
		}
		else{
			mentionType = MentionType.NOMINAL;
		}
	}
	
	private void setNumber(Sentence sent, Dictionaries dict){
		if(this.isPronominal()){
			if (dict.pluralPronouns.contains(headString)) {
				number = Number.PLURAL;
			}
			else if(dict.singularPronouns.contains(headString)){
				number = Number.SINGULAR;
			}
			else{
				number = Number.UNKNOWN;
			}
		}
		else if(this.isList()){
			number = Number.PLURAL;
		}
		else if(!headword.ner.equals("O") && !this.isNominative()){
			if(!(headword.ner.equals("ORGANIZATION") || headword.ner.startsWith("ORG"))){
				number = Number.SINGULAR;
			}
			else{
				// ORGs can be both plural and singular
				number = Number.UNKNOWN;
			}
		}
		else{
			if(headword.postag.startsWith("N") && headword.postag.endsWith("S")){
				number = Number.PLURAL;
			}
			else if(headword.postag.startsWith("N")){
				number = Number.SINGULAR;
			}
			else{
				number = Number.UNKNOWN;
			}
		}
		
		if(!this.isPronominal()) {
			if(number == Number.UNKNOWN){
				if(dict.singularWords.contains(headString)){
					number = Number.SINGULAR;
				}
			}
			else if(dict.pluralWords.contains(headString)){
				number = Number.PLURAL;
			}
		}
	}
	
	private Gender getGender(Sentence sent, Dictionaries dict){
		LinkedList<String> mStr = new LinkedList<String>();
		for(int i = startIndex; i <= headIndex; ++i){
			mStr.add(sent.getLexicon(i).form.toLowerCase());
		}
		
		int len = mStr.size();
		
		char firstLetter = headword.form.charAt(0);
		if(len > 1 && Character.isUpperCase(firstLetter) && headword.ner.equals("PERSON")){
			int firstNameIdx = len - 2;
			String secondToLast = mStr.get(firstNameIdx);
			if(firstNameIdx > 1 && (secondToLast.length() == 1 || (secondToLast.length() == 2 && secondToLast.endsWith(".")))) {
				firstNameIdx--;
			}
			secondToLast = mStr.get(firstNameIdx);
			
			for(int i = 0; i <= firstNameIdx; ++i){
				if(dict.genderNumber.containsKey(mStr)){
					return dict.genderNumber.get(mStr);
				}
				mStr.poll();
			}
			
			List<String> convertedStr = new ArrayList<String>(2);
			convertedStr.add(secondToLast);
			convertedStr.add("!");
			if(dict.genderNumber.containsKey(convertedStr)){ 
				return dict.genderNumber.get(convertedStr);
			}

			if(dict.genderNumber.containsKey(convertedStr.subList(0, 1))){
				return dict.genderNumber.get(convertedStr.subList(0, 1));
			}
		}
		if(len > 0){
			List<String> convertedStr = new ArrayList<String>(1);
			convertedStr.add(headString);
			if(dict.genderNumber.containsKey(convertedStr)){
				return dict.genderNumber.get(convertedStr);
			}
		}
		return null;
	}
	
	private void setGender(Sentence sent, Dictionaries dict, Gender genderNumberResult){
		gender = Gender.UNKNOWN;
		if(genderNumberResult != null && this.number != Number.PLURAL){
			gender = genderNumberResult;
		}
		
		if (this.isPronominal()) {
			if (dict.malePronouns.contains(headString)) {
				gender = Gender.MALE;
			} 
			else if (dict.femalePronouns.contains(headString)) {
				gender = Gender.FEMALE;
			}
		}
		else{
			if(gender == Gender.UNKNOWN) {
				if(headword.ner.equals("PERSON") || headword.ner.equals("PER")){
					int[] ids = getNerSpan(sent);
					for(int j = ids[0]; j < ids[1]; ++j){
						if(dict.maleWords.contains(sent.getLexicon(j).form.toLowerCase())) {
							gender = Gender.MALE;
							break;
						}
						if(dict.femaleWords.contains(sent.getLexicon(j).form.toLowerCase())) {
							gender = Gender.FEMALE;
							break;
						}
					}
				}
				else{
					if(dict.maleWords.contains(headString)) {
						gender = Gender.MALE;
					}
					else if(dict.femaleWords.contains(headString)) {
						gender = Gender.FEMALE;
					}
					else if(dict.neutralWords.contains(headString)) {
						gender = Gender.NEUTRAL;
					}
					else if(this.isProper() || this.isNominative()){
						if(animacy == Animacy.INANIMATE){
							gender = Gender.NEUTRAL;
						}
					}
				}
			}
		}
	}
	
	private void setAnimacy(Sentence sent, Dictionaries dict){
		if(this.isPronominal()) {
			if(dict.animatePronouns.contains(headString)) {
				animacy = Animacy.ANIMATE;
			}
			else if(dict.inanimatePronouns.contains(headString)) {
				animacy = Animacy.INANIMATE;
			}
			else{
				animacy = Animacy.UNKNOWN;
			}
		}
		else if(headword.ner.equals("PERSON") || headword.ner.equals("PER")){
			animacy = Animacy.ANIMATE;
		}
		else if(headword.ner.equals("LOCATION") || headword.ner.startsWith("LOC")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("MONEY")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("NUMBER")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("PERCENT")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("DATE")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("TIME")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("MISC")){
			animacy = Animacy.UNKNOWN;
		}
		else if(headword.ner.equals("VEH")){
			animacy = Animacy.UNKNOWN;
		}
		else if(headword.ner.equals("FAC")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("GPE")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("WEA")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("ORG")){
			animacy = Animacy.INANIMATE;
		}
		else{
			animacy = Animacy.UNKNOWN;
		}
		
		if(!this.isPronominal()) {
			// Better heuristics using DekangLin:
			if(animacy == Animacy.UNKNOWN) {
				if(dict.animateWords.contains(headString) || dict.animateWords.contains(headword.form)) {
					animacy = Animacy.ANIMATE;
				}
				else if(dict.inanimateWords.contains(headString) || dict.inanimateWords.contains(headword.form)) {
					animacy = Animacy.INANIMATE;
				}
			}
		}
	}
	
	private void setPerson(Sentence sent, Dictionaries dict){
		// only do for pronoun
		if(!this.isPronominal()){
			person = Person.UNKNOWN;
			return;
		}
		
		String spanToString = getSpan(sent).toLowerCase();
		if(dict.firstPersonPronouns.contains(spanToString)) {
			if (number == Number.SINGULAR) {
				person = Person.I;
			}
			else if (number == Number.PLURAL) {
				person = Person.WE;
			}
			else {
				person = Person.UNKNOWN;
			}
			}
		else if(dict.secondPersonPronouns.contains(spanToString)) {
			person = Person.YOU;
		}
		else if(dict.thirdPersonPronouns.contains(spanToString)) {
			if (gender == Gender.MALE && number == Number.SINGULAR) {
				person = Person.HE;
			}
			else if (gender == Gender.FEMALE && number == Number.SINGULAR) {
				person = Person.SHE;
			}
			else if ((gender == Gender.NEUTRAL || animacy == Animacy.INANIMATE) && number == Number.SINGULAR) {
				person = Person.IT;
			}
			else if (number == Number.PLURAL) {
				person = Person.THEY;
			}
			else {
				person = Person.UNKNOWN;
			}
		}
		else {
			person = Person.UNKNOWN;
		}
	}
	
	private int[] getNerSpan(Sentence sent){
		if(headword.ner.equals("O")){
			return null;
		}
		
		int start = headIndex;
		while(start > 0){
			String preNer = sent.getLexicon(start - 1).ner;
			if(headword.ner.equals(preNer)){
				start--;
			}
			else{
				break;
			}
		}
		
		int end = headIndex + 1;
		while(end < sent.length()){
			String nextNer = sent.getLexicon(end).ner;
			if(headword.ner.equals(nextNer)){
				end++;
			}
			else{
				break;
			}
		}
		
		int[] res = new int[2];
		res[0] = start;
		res[1] = end;
		return res;
	}
	
	public boolean equals(Mention m){
		return this.sentID == m.sentID && this.mentionID == m.mentionID
				&& this.startIndex == m.startIndex && this.endIndex == m.endIndex ;
	}
	
	public void display(Sentence sent, PrintStream printer){
		printer.println("#Begin Mention " + this.mentionID);
		printer.println("sent ID: " + this.sentID);
		printer.println("mention ID: " + this.mentionID);
		printer.println("cluster ID: " + (this.corefCluster == null ? -1 : this.corefCluster.clusterID));
		printer.println(this.startIndex + " " + this.endIndex + " " + this.getSpan(sent));
		printer.println("headIndex: " + this.headIndex);
		printer.println("headString: " + this.headString);
		printer.println("mention type: " + this.mentionType);
		printer.println("mention gender: " + this.gender);
		printer.println("mention number: " + this.number);
		printer.println("mention animacy: " + this.animacy);
		printer.println("mention person: " + this.person);
		printer.println("mention ner: " + this.headword.ner);
		printer.println("#end Mention " + this.mentionID);
		printer.println("===================================");
		printer.flush();
	}
}
