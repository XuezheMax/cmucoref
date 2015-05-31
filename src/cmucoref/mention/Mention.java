package cmucoref.mention;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cmucoref.document.Document;
import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.exception.MentionException;
import cmucoref.model.Options;

import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Dictionaries.Animacy;
import edu.stanford.nlp.dcoref.Dictionaries.Gender;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;
import edu.stanford.nlp.dcoref.Dictionaries.Number;
import edu.stanford.nlp.dcoref.Dictionaries.Person;
import edu.stanford.nlp.util.Generics;

public class Mention implements Serializable{
	/**
	 * 
	 */
	
	public static enum Definiteness {DEFINITE, GENERIC};
	
	private static final long serialVersionUID = 1L;
	
	public static final Comparator<Mention> headIndexOrderComparator = new MentionComparatorHeadIndexOrder();
	public static final Comparator<Mention> postTreeOrderComparator = new MentionComparatorPostTreeOrder();
	public static final Comparator<Mention> headIndexWithSpeakerOrderComparator = new MentionComparatorHeadIndexWithSpeakerOrder();
	
	public static Options options = null;
	
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
	public Definiteness definite;
	
	public SpeakerInfo speakerInfo = null;
	public SpeakerInfo utteranceInfo = null; // the utterances with this mention as speaker;
	public SpeakerInfo nextSpeakerInfo = null; //TODO
	
	private List<Mention> listMember = null;
	private Mention belongTo = null;
	private List<Mention> appositions = null;
	private Mention apposTo = null;
	private List<Mention> predicateNominatives = null;
	private Mention predNomiTo = null;
	private List<Mention> relativePronouns = null;
	private Mention relPronTo = null;
	private List<Mention> roleAppositions = null;
	private Mention roleApposTo = null;
	
	public List<Mention> preciseMatchs = null;
	public boolean preciseMatch = false;
	public List<Mention> stringMatchs = null;
	public boolean stringMatch = false;
	
	public Mention localAttrMatch = null;
	
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
		
		preciseMatch = false;
		stringMatch = false;
		localAttrMatch = null;
		
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
	
	public void setRepres() {
		this.isRepresentative = true;
		this.corefCluster = new MentionCluster(this);
	}
	
	public void setAntec(Mention antec) {
		this.antecedent = antec;
		this.corefCluster = antec.corefCluster;
		this.corefCluster.add(this);
	}
	
	public void setSingleton() {
		this.isRepresentative = true;
		this.corefCluster = new MentionCluster(this);
		this.antecedent = null;
	}
	
	public boolean isSingleton(){
		return this.corefCluster == null || this.corefCluster.isSingleton();
	}
	
	public boolean corefTo(Mention mention){
		if(mention == null) {
			return false;
		}
		
		return this.corefCluster != null 
				&& mention.corefCluster != null 
				&& this.corefCluster.clusterID == mention.corefCluster.clusterID;
	}
	
	public Mention getApposTo() {
		return apposTo;
	}
	
	public Mention getBelognTo() {
		return belongTo;
	}
	
	public Mention getPredNomiTo() {
		return predNomiTo;
	}
	
	public Mention getRelPronTo() {
		return relPronTo;
	}
	
	public Mention getRoleApposTo() {
		return roleApposTo;
	}
	
	public boolean isListMemberOf(Mention m) {
		return belongTo == null ? false : this.belongTo.equals(m);
	}
	
	public boolean apposTo(Mention antec) {
		return this.apposTo == null ? false : this.apposTo.equals(antec);
	}
	
	public boolean predNomiTo(Mention antec) {
		return this.predNomiTo == null ? false : this.predNomiTo.equals(antec);
	}
	
	public boolean relPronTo(Mention antec) {
		return this.relPronTo == null ? false : this.relPronTo.equals(antec);
	}
	
	public boolean roleApposTo(Mention antec) {
		return this.roleApposTo == null ? false : this.roleApposTo.equals(antec);
	}
	
	public boolean speakerTo(Mention anaph) {
		Mention anaphSpeaker = anaph.speakerInfo.getSpeaker();
		return anaphSpeaker == null ? false : this.equals(anaphSpeaker);
	}
	
	public void setSpeakerInfo(SpeakerInfo speakerInfo) {
		this.speakerInfo = speakerInfo;
		speakerInfo.addMention(this);
	}
	
	public boolean isNominative(){
		return !isList() && (this.mentionType == MentionType.NOMINAL);
	}
	
	public boolean isProper(){
		return !isList() && (this.mentionType == MentionType.PROPER);
	}
	
	public boolean isPronominal(){
		return !isList() && (this.mentionType == MentionType.PRONOMINAL);
	}
	
	public boolean isPossessiveOrReflexivePronominal(Dictionaries dict) {
		return isPronominal() 
				&& (dict.possessivePronouns.contains(this.headString) 
						|| dict.reflexivePronouns.contains(this.headString));
	}
	
	public boolean isList(){
		return this.listMember != null;
	}
	
	public boolean isDefinite() {
		return this.definite == Definiteness.DEFINITE;
	}
	
	public boolean ruleout(Sentence sent, Mention antec, Sentence antecSent, Dictionaries dict) {
		//attribute does not match ---> rule out
		if(!antec.attrAgree(this, dict)) {
			return true;
		}
		
		//cover ---> rule out
		if(!antec.isPossessiveOrReflexivePronominal(dict) && !this.isPossessiveOrReflexivePronominal(dict) 
				&& (antec.cover(this) || this.cover(antec))) {
			return true;
		}
		
		//<I>s, <YOU>s or <WE>s with different speakers ---> rule out
		if(((this.person == Person.I && antec.person == Person.I) 
				|| (this.person == Person.YOU && antec.person == Person.YOU)
				|| (this.person == Person.WE && antec.person == Person.WE))
				&& !(this.speakerInfo.equals(antec.speakerInfo))) {
			return true;
		}
		
		//speakers and !<I> mentions in its utterance
		if(antec.speakerTo(this) && this.person != Person.I) {
			return true;
		}
		
		return false;
	}
	
//	public int getDistOfMention(Mention mention){
////		int distOfSent = this.getDistOfSent(mention);
////		int lowerbound = distOfSent * 10;
////		int upperbound = (distOfSent + 1) * 10 - 1;
////		
////		int distOfMention = (this.mentionID - mention.mentionID) / 5;
////		if(distOfMention < lowerbound){
////			distOfMention = lowerbound;
////		}
////		if(distOfMention > upperbound){
////			distOfMention = upperbound;
////		}
////		return distOfMention;
//		return this.mentionID - mention.mentionID;
//	}
	
	public int getDistOfSent(Mention mention){
		int distOfSent = this.sentID - mention.sentID;
		
		if(distOfSent == 0 && (this.isPronominal() || mention.isPronominal())) {
			distOfSent = 1;
		}
		
		if(distOfSent > 49) {
			distOfSent = 49;
		}
		
		return distOfSent;
	}
	
	public boolean numberAgree(Mention mention){
		if(this.number != Number.UNKNOWN && mention.number != Number.UNKNOWN){
			return this.number == mention.number;
		}
		else{
			return true;
		}
	}
	
	public boolean genderAgree(Mention mention){
		if(this.gender != Gender.UNKNOWN && mention.gender != Gender.UNKNOWN){
			return this.gender == mention.gender;
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
			return this.animacy == mention.animacy;
		}
	}
	
	public boolean personAgree(Mention mention) {
		if(this.person == Person.UNKNOWN || mention.person == Person.UNKNOWN) {
			return true;
		}
		else {
			return this.person == mention.person;
		}
	}
	
	public boolean NERAgree(Mention mention, Dictionaries dict) {
		if(this.isPronominal()){
			if(mention.headword.ner.equals("O")){
				return true;
			}
			else if(mention.headword.ner.equals("MISC")
					|| mention.headword.ner.equals("NORP")
					|| mention.headword.ner.equals("LAW")){
				return true;
			}
			else if(mention.headword.ner.equals("ORGANIZATION") || mention.headword.ner.equals("ORG")
					|| mention.headword.ner.equals("GPE")
					|| mention.headword.ner.equals("PRODUCT")){
				return dict.organizationPronouns.contains(headString);
			}
			else if(mention.headword.ner.equals("PERSON")){
				return dict.personPronouns.contains(headString);
			}
			else if(mention.headword.ner.equals("LOCATION") 
					|| mention.headword.ner.equals("LOC")
					|| mention.headword.ner.equals("FAC")
					|| mention.headword.ner.equals("WORK_OF_ART")){
				return dict.locationPronouns.contains(headString);
			}
			else if(mention.headword.ner.equals("DATE") || mention.headword.ner.equals("TIME")){
				return dict.dateTimePronouns.contains(headString);
			}
			else if(mention.headword.ner.equals("MONEY") 
					|| mention.headword.ner.equals("PERCENT") 
					|| mention.headword.ner.equals("CARDINAL") 
					|| mention.headword.ner.equals("QUANTITY") 
					|| mention.headword.ner.equals("ORDINAL") 
					|| mention.headword.ner.equals("NUMBER")
					|| mention.headword.ner.equals("LANGUAGE")){
				return dict.moneyPercentNumberPronouns.contains(headString);
			}
			else if(mention.headword.ner.equals("EVENT")) {
				return false;
			}
			else{
				return false;
			}
		}
		else if(mention.isPronominal()){
			return mention.NERAgree(this, dict);
		}
		
		return this.headword.ner.equals("O") || mention.headword.ner.equals("O")
				|| this.headword.ner.equals(mention.headword.ner);
	}
	
	public boolean attrAgree(Mention mention, Dictionaries dict){
		if(!(this.numberAgree(mention))){
			return false;
		}
		else if(!(this.genderAgree(mention))){
			return false;
		}
		else if(!(this.animateAgree(mention))){
			return false;
		}
		else if(!(this.NERAgree(mention, dict))){
			return false;
		}
		else if(!(this.personAgree(mention))){
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
		
		this.number = Number.PLURAL;
		if(this.mentionType == MentionType.PRONOMINAL) {
			this.mentionType = MentionType.NOMINAL;
		}
		
		if(this.person == Person.I) {
			this.person = Person.WE;
		}
		else if(this.person == Person.HE || this.person == Person.SHE || this.person == Person.IT) {
			this.person = Person.THEY;
		}
		else if(this.person == Person.UNKNOWN) {
			this.person = Person.THEY;
		}
		
		
		if(member.person == Person.I || member.person == Person.WE) {
			this.person = Person.WE;
		}
		else if(member.person == Person.YOU) {
			if(this.person == Person.THEY) {
				this.person = Person.YOU;
			}
		}
	}
	
	public int apposOrder(Mention mention) {
		if(this.isProper()) {
			if(mention.isProper()) {
				return this.headIndex < mention.headIndex ? -1 : (this.headIndex == mention.headIndex ? 0 : 1);
			}
			else {
				return -1;
			}
		}
		else if (this.isPronominal()) {
			if(mention.isProper()){
				return 1;
			}
			else if(mention.isPronominal()) {
				return this.headIndex < mention.headIndex ? -1 : (this.headIndex == mention.headIndex ? 0 : 1);
			}
			else if(mention.isNominative()) {
				return -1;
			}
			else {
				throw new RuntimeException("mention type error: " + mention.mentionType);
			}
		}
		else if(this.isNominative()){
			if(mention.isNominative()) {
				return this.headIndex < mention.headIndex ? -1 : (this.headIndex == mention.headIndex ? 0 : 1);
			}
			else {
				return 1;
			}
		}
		else {
			throw new RuntimeException("mention type error: " + this.mentionType);
		}
	}
	
	public void addApposition(Mention appo, Dictionaries dict) throws MentionException {
		if(!appo.attrAgree(this, dict)) {
			return;
		}
		
		if(appo.apposTo != null) {
			if(appo.apposTo(this) || appo.apposTo.isListMemberOf(this)) {
				return;
			}
			else {
				throw new MentionException(appo.headString + " is apposition to another mention " + appo.apposTo.mentionID);
			}
		}
		
		if(appo.headword.basic_head != this.headIndex || !appo.headword.basic_deprel.equals("appos")) {
			return;
		}
		
		if(appositions == null) {
			appositions = new ArrayList<Mention>();
		}
		
		appositions.add(appo);
		appo.apposTo = this;
	}
	
	public void addRoleApposition(Mention role, Sentence sent, Dictionaries dict) throws MentionException {
		if(role.isPronominal()) {
			return;
		}
		
		if(!this.headword.ner.startsWith("PER") && !this.headword.ner.equals("O")) {
			return;
		}
		
		if(!role.headword.ner.startsWith("PER") && !role.headword.ner.equals("O")) {
			return;
		}
		
		if(role.headword.ner.equals("O") && this.headword.ner.equals("O")) {
			return;
		}
		
		if(!role.attrAgree(this, dict)) {
			return;
		}
		
		String thisString = this.getSpan(sent);
		if(thisString.contains("'") || thisString.contains("and")) {
			return;
		}
		
		if(dict.demonymSet.contains(thisString.toLowerCase()) 
			|| dict.demonymSet.contains(role.getSpan(sent).toLowerCase())) {
			return;
		}
		
		if(role.roleApposTo != null) {
			if(role.roleApposTo(this) || role.roleApposTo.isListMemberOf(this)) {
				return;
			}
			else {
				throw new MentionException(role.headString + " is role apposition to another mention " + role.apposTo.mentionID);
			}
		}
		
		if(roleAppositions == null) {
			roleAppositions = new ArrayList<Mention>();
		}
		
		roleAppositions.add(role);
		role.roleApposTo = this;
	}
	
	public void addPredicativeNominative(Mention predMomi, Dictionaries dict) throws MentionException {
		if(!predMomi.attrAgree(this, dict)) {
			return;
		}
		
		if(predMomi.predNomiTo != null) {
			return;
		}
		
		if(predicateNominatives == null) {
			predicateNominatives = new ArrayList<Mention>();
		}
		
		predicateNominatives.add(predMomi);
		predMomi.predNomiTo = this;
	}
	
	public void addRelativePronoun(Mention relPron) throws MentionException {
		this.relativePronouns.add(relPron);
		throw new MentionException(relPron.headString + " is relative pronoun to mention " + this.headString);
	}
	
	public boolean preciseMatch(Sentence sent, Mention antec, Sentence antecSent, Dictionaries dict) {
		if(this.apposTo(antec)) {
			return true;
		}
		
		if(this.predNomiTo(antec)) {
			return true;
		}
		
		if(this.roleApposTo(antec)) {
			return true;
		}
		
		if(((this.person == Person.I && antec.person == Person.I) 
				|| (this.person == Person.YOU && antec.person == Person.YOU))
				&& (this.speakerInfo.equals(antec.speakerInfo))) {
			return true;
		}
		
		if((this.person == Person.I || this.person == Person.WE) 
				&& antec.numberAgree(this) && antec.speakerTo(this)) {
			return true;
		}
		
		return false;
	}
	
	public void addPreciseMatch(Mention preciseMatch, Document doc){
		if(this.preciseMatchs == null){
			this.preciseMatchs = new ArrayList<Mention>();
		}
		
		this.preciseMatchs.add(preciseMatch);
		this.preciseMatch = true;
	}
	
	public boolean stringMatch(Sentence sent, Mention antec, Sentence antecSent, Dictionaries dict) {
		if(!this.isPronominal() && !antec.isPronominal() 
				&& !this.cover(antec) && !antec.cover(this)
				&& this.isDefinite()) {
			return this.spanMatch(sent, antec, antecSent, dict);
		}
		
		return false;
	}
	
	public void addStringMatch(Mention stringMatch, Document doc) {
		if(this.stringMatchs == null) {
			this.stringMatchs = new ArrayList<Mention>();
		}
		
		this.stringMatchs.add(stringMatch);
		this.stringMatch = true;
	}
	
	private boolean spanMatch(Sentence sent, Mention antec, Sentence antecSent, Dictionaries dict){
		if(this.isPronominal() || antec.isPronominal()) {
			throw new RuntimeException("error: span matching does not apply for pronominals");
		}
		
		if(antec.isList() || this.isList()) {
			return this.exactSpanMatch(sent, antec, antecSent);
		}
		
		return this.headMatch(sent, antec, antecSent, dict)
				&& (antec.compatibleModifier(antecSent, this, sent, dict) 
					|| this.relaxedSpanMatch(sent, antec, antecSent)
					|| this.exactSpanMatch(sent, antec, antecSent)
					|| this.acronymMatch(sent, antec, antecSent)
					|| (options.useDemonym() && this.isDemonym(sent, antec, antecSent, dict)));
	}
	
	public boolean headMatch(Sentence sent, Mention mention, Sentence sentOfM, Dictionaries dict){
		if(this.isPronominal() || mention.isPronominal()){
			throw new RuntimeException("error: head matching does not apply for pronominals");
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
		else if(this.acronymMatch(sent, mention, sentOfM)) {
			return true;
		}
		else if(options.useDemonym() && this.isDemonym(sent, mention, sentOfM, dict)) {
			return true;
		}
		else{
			return false;
		}
	}
	
	private static Set<String> locationModifiers = new HashSet<String>(Arrays.asList("east", "west", "north", "south",
			"eastern", "western", "northern", "southern", "northwestern", "southwestern", "northeastern",
			"southeastern", "upper", "lower"));
	
	private static Set<String> excludePossessivePronouns = new HashSet<String>(Arrays.asList("our", "my", "your"));
	
	@SuppressWarnings("unused")
	private boolean compatibleModifierforProper(Sentence sent, Mention anaph, Sentence anaphSent, Dictionaries dict) {
		Set<String> locModifiersOfAntec = new HashSet<String>();
		for(int i = this.startIndex; i < this.endIndex; ++i) {
			Lexicon antecLex = sent.getLexicon(i);
			if(locationModifiers.contains(antecLex.form.toLowerCase())) {
				locModifiersOfAntec.add(antecLex.form.toLowerCase());
			}
		}
		
		Set<String> locModifiersOfAnaph = new HashSet<String>();
		for(int i = anaph.startIndex; i < anaph.endIndex; ++i) {
			Lexicon anaphLex = anaphSent.getLexicon(i);
			if(locationModifiers.contains(anaphLex.form.toLowerCase())) {
				locModifiersOfAnaph.add(anaphLex.form.toLowerCase());
			}
		}
		
		if(locModifiersOfAnaph.containsAll(locModifiersOfAntec) && locModifiersOfAntec.containsAll(locModifiersOfAnaph)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean compatibleModifier(Sentence sent, Mention anaph, Sentence anaphSent, Dictionaries dict) {
		if(this.isPronominal() || anaph.isPronominal()){
			throw new RuntimeException("error: compatible modifiers does not apply for pronominals");
		}
		
		Set<String> modifiersOfAntec = new HashSet<String>();
		Set<String> possessivePronsOfAntec = new HashSet<String>();
		Set<String> locModifiersOfAntec = new HashSet<String>();
		
		for(int i = this.startIndex; i < this.endIndex; ++i) {
			Lexicon antecLex = sent.getLexicon(i);
			if(antecLex.postag.equals("PRP$") || dict.possessivePronouns.contains(antecLex.form.toLowerCase())) {
				possessivePronsOfAntec.add(antecLex.form.toLowerCase());
			}
			else if(locationModifiers.contains(antecLex.form.toLowerCase()) && antecLex.basic_head == this.headIndex) {
				locModifiersOfAntec.add(antecLex.form.toLowerCase());
			}
			
			modifiersOfAntec.add(antecLex.form.toLowerCase());
		}
		possessivePronsOfAntec.removeAll(excludePossessivePronouns);
		
		Set<String> modifiersOfAnaph = new HashSet<String>();
		Set<String> possessivePronsOfAanph = new HashSet<String>();
		Set<String> locModifiersOfAnaph = new HashSet<String>();
		
		for(int i = anaph.startIndex; i < anaph.endIndex; ++i) {
			Lexicon anaphLex = anaphSent.getLexicon(i);
			if(anaphLex.postag.equals("PRP$") || dict.possessivePronouns.contains(anaphLex.form.toLowerCase())) {
				possessivePronsOfAanph.add(anaphLex.form.toLowerCase());
			}
			else if((anaphLex.postag.equals("JJ") || anaphLex.postag.startsWith("N")
					|| anaphLex.postag.startsWith("V") || anaphLex.postag.equals("CD"))
					&& (!entityWordsToExclude.contains(anaphLex.form.toLowerCase()))) {
				
				modifiersOfAnaph.add(anaphLex.form.toLowerCase());
				
				if(locationModifiers.contains(anaphLex.form.toLowerCase())) {
					locModifiersOfAnaph.add(anaphLex.form.toLowerCase());
				}
			}
		}
		possessivePronsOfAanph.removeAll(excludePossessivePronouns);
		
		if(!modifiersOfAntec.containsAll(modifiersOfAnaph)) {
			return false;
		}
		
		if((anaph.headword.ner.equals("GPE") || anaph.headword.ner.startsWith("LOC")) && !locModifiersOfAnaph.containsAll(locModifiersOfAntec)) {
			return false;
		}
		
		if(!possessivePronsOfAntec.isEmpty() && !possessivePronsOfAntec.containsAll(possessivePronsOfAanph)) {
			return false;
		}
		
		return true;
	}
	
	private static final List<String> entityWordsToExclude =
			Arrays.asList("the", "this", "that", "those", "these", "mr.", "miss", "mrs.", "dr.", "ms.", "inc.", "ltd.", "corp.", "'s");
	
	private static final List<String> entityPOSToExclude = 
			Arrays.asList(".", ",", "``", "''", ":");
	
	//this mention includes all words in anaph
	public boolean wordsInclude(Sentence sent, Mention anaph, Sentence anaphSent, Dictionaries dict){
		if(this.isPronominal() || anaph.isPronominal()){
			throw new RuntimeException("error: word including does not apply for pronominals");
		}
		
		Set<String> wordsOfThis = new HashSet<String>();
		
		for(int i = this.startIndex; i < this.endIndex; ++i) {
			Lexicon antecLex = sent.getLexicon(i);
			wordsOfThis.add(antecLex.form.toLowerCase());
		}
		
		Set<String> wordsExceptStopWords = new HashSet<String>();
		for(int i = anaph.startIndex; i < anaph.endIndex; ++i) {
			Lexicon anaphLex = anaphSent.getLexicon(i);
			
			if(entityPOSToExclude.contains(anaphLex.postag)) {
				continue;
			}
			wordsExceptStopWords.add(anaphLex.form.toLowerCase());
		}
		wordsExceptStopWords.removeAll(entityWordsToExclude);
		
		if(wordsOfThis.containsAll(wordsExceptStopWords)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean isDemonym(Sentence sent, Mention mention, Sentence sentOfM, Dictionaries dict) {
		if(this.isPronominal() || mention.isPronominal()) {
			throw new RuntimeException("error: exact matching does not apply for pronominals");
		}
		
		String anaphSpan = this.getSpan(sent);
		String antecSpan = mention.getSpan(sentOfM);
		
		// The US state matching part (only) is done cased
		String thisNormed = dict.lookupCanonicalAmericanStateName(anaphSpan);
		String antNormed = dict.lookupCanonicalAmericanStateName(antecSpan);
		if (thisNormed != null && thisNormed.equals(antNormed)) {
			return true;
		}
		
		// The rest is done uncased
		anaphSpan = anaphSpan.toLowerCase(Locale.ENGLISH);
		antecSpan = antecSpan.toLowerCase(Locale.ENGLISH);
		
		if (anaphSpan.startsWith("the ")) {
			anaphSpan = anaphSpan.substring(4);
		}
		if (antecSpan.startsWith("the ")) {
			antecSpan = antecSpan.substring(4);
		}

		Set<String> anaphDemonyms = dict.getDemonyms(anaphSpan);
		Set<String> antecDemonyms = dict.getDemonyms(antecSpan);
		if (anaphDemonyms.contains(antecSpan) || antecDemonyms.contains(anaphSpan)) {
			return true;
		}
		return false;
	}
	
	public boolean acronymMatch(Sentence sent, Mention mention, Sentence sentOfM) {
		return this.isProper() && mention.isProper() && (this.isAcronymTo(mention, sentOfM) || mention.isAcronymTo(this, sent));
	}
	
	public boolean isAcronymTo(Mention mention, Sentence sent){
		if(!this.isProper() || !mention.isProper()){
			throw new RuntimeException("error: acronym applies for propers");
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
			throw new RuntimeException("error: relaxed matching does not apply for pronominals");
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
			throw new RuntimeException("error: exact matching does not apply for pronominals");
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
		setDefiniteness(sent, dict);
		setNumber(sent, dict);
		setAnimacy(sent, dict);
		setGender(sent, dict, getGender(sent, dict));
		setPerson(sent, dict);
	}
	
	private static final String [] commonNESuffixes = {
		"Corp", "Co", "Inc", "Ltd"
	};
	
	private static final Set<String> pluralDeterminers = new HashSet<String>(Arrays.asList("these", "those"));
	
	private static final Set<String> singularDeterminers = new HashSet<String>(Arrays.asList("this"));

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
				((endIndex - startIndex) == 1 && headword.ner.equals("O") && 
				(dict.allPronouns.contains(headString) 
					|| dict.relativePronouns.contains(headString)
					|| dict.determiners.contains(headString)))){
			mentionType = MentionType.PRONOMINAL;
		}
		else if(!headword.ner.equals("O") || headword.postag.startsWith("NNP")){
			mentionType = MentionType.PROPER;
		}
		else{
			mentionType = MentionType.NOMINAL;
		}
	}
	
	private void setDefiniteness(Sentence sent, Dictionaries dict) {
		if(this.isNominative()) {
			this.definite = Definiteness.GENERIC;
			for(int i = this.startIndex; i <= this.headIndex; ++i) {
				String word = sent.getLexicon(i).form.toLowerCase();
				String pos = sent.getLexicon(i).postag;
				if(word.equals("its") || pos.equals("POS") || dict.determiners.contains(word)) {
					this.definite = Definiteness.DEFINITE;
					return;
				}
			}
		}
		else {
			this.definite = Definiteness.DEFINITE;
		}
	}
	
	private void setNumber(Sentence sent, Dictionaries dict){
		if(this.isPronominal()){
			if (dict.pluralPronouns.contains(headString) || pluralDeterminers.contains(headString)) {
				number = Number.PLURAL;
			}
			else if(dict.singularPronouns.contains(headString) || singularDeterminers.contains(headString)){
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
			if(headword.ner.equals("ORGANIZATION") || headword.ner.startsWith("ORG") 
					|| headword.ner.equals("PRODUCT")
					|| headword.ner.equals("NORP")) {
				// ORGs, PRODUCTs and NORP can be both plural and singular
				number = Number.UNKNOWN;
			}
			else{
				number = Number.SINGULAR;
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
	
	//re-define animate pronouns
	private final Set<String> animatePronouns = Generics.newHashSet(Arrays.asList(new String[]{ "i", "me", "myself", "mine", "my", "we", "us", "ourself", "ourselves", "ours", "our", "you", "yourself", "yours", "your", "yourselves", "he", "him", "himself", "his", "she", "her", "herself", "hers", "her", "who", "whom"}));
	
	private void setAnimacy(Sentence sent, Dictionaries dict){
		if(this.isPronominal()) {
			if(animatePronouns.contains(headString)) {
				animacy = Animacy.ANIMATE;
			}
			else if(dict.inanimatePronouns.contains(headString) || dict.determiners.contains(headString)) {
				animacy = Animacy.INANIMATE;
			}
			else{
				animacy = Animacy.UNKNOWN;
			}
		}
		else if(headword.ner.equals("PERSON") || headword.ner.equals("PER")){
			animacy = Animacy.ANIMATE;
		}
		else if(headword.ner.equals("GPE")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("LOCATION") || headword.ner.startsWith("LOC")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("MONEY")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("CARDINAL")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("ORDINAL")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("QUANTITY")){
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
		else if(headword.ner.equals("NORP")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("LANGUAGE")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("MISC")){
			animacy = Animacy.UNKNOWN;
		}
		else if(headword.ner.equals("FAC")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("VEH")){
			animacy = Animacy.UNKNOWN;
		}
		else if(headword.ner.equals("WEA")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("PRODUCT")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("ORG")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("LAW")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("EVENT")){
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("WORK_OF_ART")){
			animacy = Animacy.UNKNOWN;
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
		if(!this.isPronominal()){
			if(this.number == Number.UNKNOWN) {
				person = Person.UNKNOWN;
			}
			else if(this.number == Number.PLURAL) {
				person = Person.THEY;
			}
			else if(this.gender == Gender.MALE) {
				person = Person.HE;
			}
			else if(this.gender == Gender.FEMALE) {
				person = Person.SHE;
			}
			else if(this.gender == Gender.NEUTRAL) {
				person = Person.IT;
			}
			else {
				if(this.animacy == Animacy.INANIMATE) {
					person = Person.IT;
				}
				else {
					person = Person.UNKNOWN;
				}
			}
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
		return this.sentID == m.sentID && this.startIndex == m.startIndex && this.endIndex == m.endIndex ;
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
		printer.println("mention definiteness: " + this.definite);
		printer.println("mention gender: " + this.gender);
		printer.println("mention number: " + this.number);
		printer.println("mention animacy: " + this.animacy);
		printer.println("mention person: " + this.person);
		printer.println("mention ner: " + this.headword.ner);
		printer.println("mention speaker: " + (this.speakerInfo == null ? "null" : this.speakerInfo.toString() + " " + this.speakerInfo.getSpeakerName()));
		printer.println("#end Mention " + this.mentionID);
		printer.println("===================================");
		printer.flush();
	}
	
	public void display(PrintStream printer){
		printer.println("#Begin Mention " + this.mentionID);
		printer.println("sent ID: " + this.sentID);
		printer.println("mention ID: " + this.mentionID);
		printer.println("cluster ID: " + (this.corefCluster == null ? -1 : this.corefCluster.clusterID));
		printer.println(this.startIndex + " " + this.endIndex);
		printer.println("headIndex: " + this.headIndex);
		printer.println("headString: " + this.headString);
		printer.println("mention type: " + this.mentionType);
		printer.println("mention definiteness: " + this.definite);
		printer.println("mention gender: " + this.gender);
		printer.println("mention number: " + this.number);
		printer.println("mention animacy: " + this.animacy);
		printer.println("mention person: " + this.person);
		printer.println("mention ner: " + this.headword.ner);
		printer.println("mention speaker: " + (this.speakerInfo == null ? "null" : this.speakerInfo.toString() + " " + this.speakerInfo.getSpeakerName()));
		printer.println("#end Mention " + this.mentionID);
		printer.println("===================================");
		printer.flush();
	}
}
