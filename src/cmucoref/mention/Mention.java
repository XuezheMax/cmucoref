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
	
	private static final String [] commonNESuffixes = {
			"Corp", "Co", "Inc", "Ltd", "Province", "State"
	};
	
	private static Set<String> locationModifiers = new HashSet<String>(Arrays.asList("east", "west", "north", "south",
			"eastern", "western", "northern", "southern", "northwestern", "southwestern", "northeastern",
			"southeastern", "upper", "lower"));
	
	private static final List<String> personalTitles = Arrays.asList("atty.", "attorney", "coach", "president",
			"doctor", "dir.", "director", "fr.", "father", "gov.", "governor", "prof.", "professor");
	
	private static Set<String> excludePossessivePronouns = new HashSet<String>(Arrays.asList("our", "my", "your"));
	private static final Set<String> quantDeterminers = new HashSet<String>(Arrays.asList("both"));
		
	private static final Set<String> pluralDeterminers = new HashSet<String>(Arrays.asList("these", "those"));
		
	private static final Set<String> singularDeterminers = new HashSet<String>(Arrays.asList("this", "that", "the"));
	
	private static final Set<String> locationPronouns = new HashSet<String>(Arrays.asList("it", "its", "itself"));
	private static final Set<String> norpPronouns = new HashSet<String>(Arrays.asList("it", "its", "itself", "they", "them", "their", "theirs", "themself", "themselves"));
		
	public static final Set<String> dates = new HashSet<String>(Arrays.asList(
			"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "yesterday", 
			"tomorrow", "today", "tonight", "january", "february", "march", "april", "may", "june", "july", 
			"august", "september", "october", "november", "december"));
	
	public static final Set<String> temporals = new HashSet<String>(Arrays.asList(
			"second", "seconds", "minute", "minutes", "hour", "hours", "day", "days", "week", "weeks", 
			"month", "months", "year", "years", "decade", "decades", "century", "centuries", "millennium", "millenniums",
			"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", 
			"now", "yesterday", "tomorrow", "today", "tonight",
			"age", "time", "era", "epoch", "morning", "evening", "night", "noon", "afternoon",
			"semester", "semesters", "trimester", "trimesters", "quarter", "quarters", "term", "terms", 
			"winter", "spring", "summer", "fall", "autumn", "season",
			"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"));
	
	private static final List<String> entityWordsToExclude = Arrays.asList("the", "this", "that", "those", 
			"these", "mr.", "miss", "mrs.", "dr.", "ms.", "inc.", "ltd.", "corp.", "'s");
	
	private static final List<String> entityPOSToExclude = 
			Arrays.asList(".", ",", "``", "''", ":", "-LRB-", "-RRB-", "UH");
	
	private static final Set<String> parts = new HashSet<String>(Arrays.asList("hundreds", "thousands", 
			"millions", "billions", "tens", "dozens", "group", "groups", "bunch", "a number", "numbers", 
			"a pinch", "a total", "all", "both"));
	
	public static final Set<String> rolesofNoun = new HashSet<String>(Arrays.asList("nsubj", "nsubjpass", "iobj", 
			"dobj", "agent", "xsubj", "pobj", "root", "conj", "appos", "xcomp", "poss"));
	
	public static enum Definiteness {DEFINITE, GENERIC};
	
	private static final long serialVersionUID = 1L;
	
	public static final Comparator<Mention> headIndexOrderComparator = new MentionComparatorHeadIndexOrder();
	public static final Comparator<Mention> postTreeOrderComparator = new MentionComparatorPostTreeOrder();
	public static final Comparator<Mention> headIndexWithSpeakerOrderComparator = new MentionComparatorHeadIndexWithSpeakerOrder();
	public static final Comparator<Mention> syntacticOrderComparator = new MentionComparatorSyntacticOrder();
	
	public static Options options = null;
	
	private final Document doc;
	public int startIndex;
	public int endIndex;
	public int headIndex;
	public final int originalHeadIndex;
	public int mentionID = -1;
	
	public Lexicon headword = null;
	public String headString = null;
	private MentionType mentionType;
	public Number number;
	public Gender gender;
	public Animacy animacy;
	public Person person;
	public Definiteness definite;
	
	public SpeakerInfo speakerInfo = null;
	public SpeakerInfo utteranceInfo = null; // the utterances with this mention as speaker;
	public SpeakerInfo preSpeakerInfo = null;
	
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
	
	private Set<Event> eventSet = null;
	private Event mainEvent = null;
	
	public Mention(int mentionID, int startIndex, int endIndex, int headIndex, int sentID, Document doc){
		this.mentionID = mentionID;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		
		preciseMatch = false;
		stringMatch = false;
		localAttrMatch = null;
		
		antecedent = null;
		this.corefCluster = null;
		this.isRepresentative = false;
		this.headIndex = headIndex;
		this.originalHeadIndex = this.headIndex;
		this.sentID = sentID;
		
		this.doc = doc;
		this.eventSet = new HashSet<Event>();
	}
	
	public Mention(edu.stanford.nlp.dcoref.Mention mention, int sentID, Document doc){
		this(mention.mentionID, mention.startIndex + 1, mention.endIndex + 1, mention.headIndex + 1, sentID, doc);
	}
	
	/**
	 * 
	 * @param mention
	 * @return
	 */
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
	
	public void addEvent(Event event) {
		eventSet.add(event);
	}
	
	public void setMainEvent(Event mainEvent, boolean strict) {
		if(this.mainEvent == null || strict) {
			this.mainEvent = mainEvent;
		}
	}
	
	public MentionType getMentionType() {
		return this.mentionType;
	}
	
	public Set<Event> getEventSet() {
		return this.eventSet;
	}
	
	public Event getMainEvent() {
		return mainEvent;
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
	
	public List<Mention> getListMembers() {
		return this.listMember;
	}
	
	public List<Mention> getPredicateNominatives() {
		return this.predicateNominatives;
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
	
	public boolean isNominative() {
		return !isList() && (this.mentionType == MentionType.NOMINAL);
	}
	
	public boolean isProper() {
		return !isList() && (this.mentionType == MentionType.PROPER);
	}
	
	public boolean isPronominal() {
		return !isList() && (this.mentionType == MentionType.PRONOMINAL);
	}
	
	public boolean isDetPronominal() {
		return isPronominal() && (singularDeterminers.contains(this.headString) || pluralDeterminers.contains(this.headString));
	}
	
	public boolean isPossessiveOrReflexivePronominal(Dictionaries dict) {
		return isPronominal() 
				&& (dict.possessivePronouns.contains(this.headString) 
						|| dict.reflexivePronouns.contains(this.headString));
	}
	
	public boolean isReflexivePronominal(Dictionaries dict) {
		return isPronominal() && dict.reflexivePronouns.contains(this.headString);
	}
	
	public boolean isList(){
		return this.listMember != null && this.listMember.size() > 0;
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
				|| (this.person == Person.WE && antec.person == Person.WE)
				|| (this.person == Person.YOU && antec.person == Person.YOU))
				&& !(this.speakerInfo.equals(antec.speakerInfo))) {
			return true;
		}
		
		//speakers and !<I> mentions in its utterance
		if(antec.speakerTo(this) && this.person != Person.I) {
			return true;
		}
		
		//determiner pronouns with distance larger than 1
		if(antec.isDetPronominal() && this.getDistOfSent(antec) > 1) {
			return true;
		}
		
		//reflexive pronouns with distance larger than 0
		if((this.isReflexivePronominal(dict) || antec.isReflexivePronominal(dict)) 
			&& this.getAbsoluteDistOfSent(antec) > 0) {
			return true;
		}
		
		// same predicate with subj and obj role
		if(this.samePredicateSubjandObj(antec, dict) == -1) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * ret: 0 netural, 1 coreferent, -1 rule out
	 * @param mention
	 * @return
	 */
	private int samePredicateSubjandObj(Mention mention, Dictionaries dict) {
		for(Event thisEvent : this.eventSet) {
			for(Event mEvent : mention.eventSet) {
				if(thisEvent.samePredicateWithSubjObj(mEvent)) {
					if(mention.isReflexivePronominal(dict)) {
						return 1;
					}
					else {
						return -1;
					}
				}
				else if(mEvent.samePredicateWithSubjObj(thisEvent)) {
					if(this.isReflexivePronominal(dict)) {
						return 1;
					}
					else {
						return -1;
					}
				}
			}
		}
		return 0;
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
	
	public int getAbsoluteDistOfSent(Mention mention) {
		int distOfSent = 0;
		int min = Math.min(this.sentID, mention.sentID);
		int max = Math.max(this.sentID, mention.sentID);
		for(int i = min; i <= max; ++i) {
			if(this.doc.getSentence(i).present()) {
				distOfSent++;
			}
		}
		return Math.max(0, distOfSent - 1);
	}
	
	public int getDistOfSent(Mention mention){
		int distOfSent = this.getAbsoluteDistOfSent(mention);
				
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
		if(this.isDetPronominal()) {
			if(mention.isPronominal()) {
				return this.headString.equals(mention.headString);
			}
			else {
				return false;
			}
		}
		else if(mention.isDetPronominal()) {
			if(this.isPronominal()) {
				return this.headString.equals(mention.headString);
			}
			else {
				return false;
			}
		}
		
		if(this.person == Person.UNKNOWN 
				&& mention.person != Person.I 
				&& mention.person != Person.WE 
				&& mention.person != Person.YOU) {
			return true;
		}
		else if(mention.person == Person.UNKNOWN
				&& this.person != Person.I
				&& this.person != Person.WE
				&& this.person != Person.YOU) {
			return true;
		}
		else {
			return this.person == mention.person;
		}
	}
	
	public boolean NERAgree(Mention mention, Dictionaries dict) {
		if(this.isPronominal()) {
			if(mention.headword.ner.equals("O")){
				return true;
			}
			else if(mention.isList()) {
				if(mention.headword.ner.equals("PERSON") || mention.headword.ner.equals("PER")) {
					return dict.pluralPronouns.contains(headString) || quantDeterminers.contains(headString);
				}
				else if(mention.headword.ner.equals("EVENT")
						|| mention.headword.ner.equals("TIME")
						|| mention.headword.ner.equals("DATE")) {
					return false;
				}
				else {
					return norpPronouns.contains(headString) || quantDeterminers.contains(headString);
				}
			}
			else if(mention.headword.ner.equals("MISC")
					|| mention.headword.ner.equals("LAW")) {
				return true;
			}
			else if(mention.headword.ner.equals("NORP")) {
				return norpPronouns.contains(headString) || quantDeterminers.contains(headString);
			}
			else if(mention.headword.ner.equals("PERSON") || mention.headword.ner.equals("PER")) {
				return dict.personPronouns.contains(headString) || quantDeterminers.contains(headString);
			}
			else if(mention.headword.ner.equals("LOCATION") 
					|| mention.headword.ner.equals("GPE")
					|| mention.headword.ner.equals("PRODUCT")
					|| mention.headword.ner.equals("LOC")
					|| mention.headword.ner.equals("FAC")
					|| mention.headword.ner.equals("WORK_OF_ART")
					|| mention.headword.ner.equals("MONEY") 
					|| mention.headword.ner.equals("PERCENT") 
					|| mention.headword.ner.equals("CARDINAL") 
					|| mention.headword.ner.equals("QUANTITY") 
					|| mention.headword.ner.equals("ORDINAL") 
					|| mention.headword.ner.equals("NUMBER")
					|| mention.headword.ner.equals("LANGUAGE")
					|| mention.headword.ner.equals("ORGANIZATION") || mention.headword.ner.equals("ORG")) {
				return locationPronouns.contains(headString);
			}
			else if(mention.headword.ner.equals("EVENT")
					|| mention.headword.ner.equals("TIME")
					|| mention.headword.ner.equals("DATE")) {
				return false;
			}
			else {
				return false;
			}
		}
		else if(mention.isPronominal()){
			return mention.NERAgree(this, dict);
		}
		
		return this.headword.ner.equals("O") || mention.headword.ner.equals("O")
				|| this.headword.ner.equals(mention.headword.ner);
	}
	
	public boolean attrAgree(Mention mention, Dictionaries dict) {
		if(!(this.numberAgree(mention))) {
			return false;
		}
		else if(!(this.genderAgree(mention))) {
			return false;
		}
		else if(!(this.animateAgree(mention))) {
			return false;
		}
		else if(!(this.NERAgree(mention, dict))) {
			return false;
		}
		else if(!(this.personAgree(mention))) {
			return false;
		}
		else {
			return true;
		}
	}
	
	public boolean isPureNerMention(Sentence sent) {
		String headNE = sent.getLexicon(headIndex).ner;
		if(headNE.equals("O")){
			return false;
		}
		
		for(int i = this.startIndex; i < this.endIndex; ++i){
			if(!(sent.getLexicon(i).ner.equals(headNE))){
				return false;
			}
		}
		return true;
	}
	
	public void addListMember(Mention member, Sentence sent) {
		// Don't handle nested lists
		if(member.listMember != null && member.listMember.size() > 0){
			return;
		}
		
		if(member.belongTo != null && !member.belongTo.equals(this)) {
			this.display(sent, System.err);
			member.belongTo.display(sent, System.err);
			member.display(sent, System.err);
			throw new RuntimeException(member.headString + " is list member of another mention.");
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
		
		// skip list member
		if(this.belongTo != null) {
			return;
		}
		
		if(appo.apposTo != null) {
			if(appo.apposTo(this)) {
				return;
			}
			else {
				throw new MentionException(appo.headString + " is apposition to another mention " + appo.apposTo.mentionID + "\n this mentionID: " + this.mentionID);
			}
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
		if(!predMomi.numberAgree(this)) {
			return;
		}
		
		if(!predMomi.animateAgree(this)) {
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
	
	private boolean mentionSpeakerMatch(Mention mention, Sentence sent, SpeakerInfo speakerInfo) {
		//non-quotation speaker only
		if(speakerInfo.isQuotationSpeaker()) {
			return false;
		}
		
		String[] speakerNameStrings = speakerInfo.getSpeakerNameStrings();
		Set<String> nameSet = new HashSet<String>(Arrays.asList(speakerNameStrings));
		int[] ids = mention.getNerSpan(sent);
		for(int i = ids[0]; i < ids[1]; ++i) {
			String form = sent.getLexicon(i).form.toLowerCase();
			if(form.equals("'s")) {
				continue;
			}
			
			if(!nameSet.contains(form)) {
				return false;
			}
		}
		return true;
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
		
		// I , we and you with same speaker
		if((this.person == Person.I && antec.person == Person.I) 
			&& (this.speakerInfo.equals(antec.speakerInfo))) {
			return true;
		}
		
		//we with same (non-default) speaker
		if(((this.person == Person.WE && antec.person == Person.WE) 
				|| (this.person == Person.YOU && antec.person == Person.YOU))
			&& this.speakerInfo.equals(antec.speakerInfo)
			&& !this.speakerInfo.isDefaultSpeaker()) {
			return true;
		}
		
		
		//antec is the speaker of anaph (quotation speaker only)
		if((this.person == Person.I || this.person == Person.WE) 
				&& antec.numberAgree(this) && antec.speakerTo(this)) {
			return true;
		}
		//speaker-mention name match (non-quotation speaker only)
		if(antec.person == Person.I && this.headword.ner.startsWith("PER") && mentionSpeakerMatch(this, sent, antec.speakerInfo) 
			|| this.person == Person.I && antec.headword.ner.startsWith("PER") && mentionSpeakerMatch(antec, antecSent, this.speakerInfo)) {
			return true;
		}
		// you and I with adjacent speakers
		if(this.preSpeakerInfo != null && this.preSpeakerInfo.equals(antec.speakerInfo)
				&& this.numberAgree(antec)
				&& ((this.person == Person.I && antec.person == Person.YOU) 
						|| (this.person == Person.YOU && antec.person == Person.I))) {
			return true;
		}
		// same predicate with subj and obj role
		if(this.samePredicateSubjandObj(antec, dict) == 1) {
			return true;
		}
		return false;
	}
	
	public void addPreciseMatch(Mention preciseMatch){
		if(this.preciseMatchs == null){
			this.preciseMatchs = new ArrayList<Mention>();
		}
		
		if(this.preciseMatchs.size() > 0) {
			if(preciseMatch.preciseMatchs == null) {
				preciseMatch.preciseMatchs = new ArrayList<Mention>();
			}
			preciseMatch.preciseMatchs.addAll(this.preciseMatchs);
			preciseMatch.preciseMatch = true;
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
	
	public void addStringMatch(Mention stringMatch) {
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
			if(antec.isList() && this.isList()) {
				return this.exactSpanMatch(sent, antec, antecSent) || this.listMemberMatch(sent, antec, antecSent, dict);
			}
			else {
				return this.exactSpanMatch(sent, antec, antecSent);
			}
		}
		
		return antec.headMatch(antecSent, this, sent, dict)
				&& (antec.compatibleModifier(antecSent, this, sent, dict) 
					|| this.relaxedSpanMatch(sent, antec, antecSent)
					|| this.exactSpanMatch(sent, antec, antecSent)
					|| this.acronymMatch(sent, antec, antecSent, dict)
					|| (options.useDemonym() && this.isDemonym(sent, antec, antecSent, dict)));
	}
	
	public boolean listMemberMatch(Sentence sent, Mention antec, Sentence antecSent, Dictionaries dict) {
		if(this.listMember.size() != antec.listMember.size()) {
			return false;
		}
		
		for(Mention anaphMember : this.listMember) {
			boolean memberMatch = false;
			for(Mention antecMember: antec.listMember) {
				if(anaphMember.isPronominal()) {
					if(antecMember.isPronominal()) {
						if(anaphMember.headString.equals(antecMember.headString)) {
							memberMatch = true;
							break;
						}
					}
				}
				else if(!antecMember.isPronominal()) {
					if(anaphMember.stringMatch(sent, antecMember, antecSent, dict)) {
						memberMatch = true;
						break;
					}
				}
			}
			if(!memberMatch) {
				return false;
			}
		}
		return true;
	}
	
	public boolean headMatch(Sentence sent, Mention anaph, Sentence anaphSent, Dictionaries dict){
		if(this.isPronominal() || anaph.isPronominal()){
			throw new RuntimeException("error: head matching does not apply for pronominals");
		}
		
		if(this.headString.equals(anaph.headString)){
			return true;
		}
		else if(this.headString.equalsIgnoreCase(anaphSent.getLexicon(anaph.originalHeadIndex).form)) {
			return true;
		}
		else if(sent.getLexicon(this.originalHeadIndex).form.equalsIgnoreCase(anaph.headString)) {
			return true;
		}
		else if(sent.getLexicon(this.originalHeadIndex).form.equalsIgnoreCase(anaphSent.getLexicon(anaph.originalHeadIndex).form)) {
			return true;
		}
		else if(this.exactSpanMatch(sent, anaph, anaphSent)){
			return true;
		}
		else if(this.relaxedSpanMatch(sent, anaph, anaphSent)){
			return true;
		}
		else if(this.acronymMatch(sent, anaph, anaphSent, dict)) {
			return true;
		}
		else if(options.useDemonym() && this.isDemonym(sent, anaph, anaphSent, dict)) {
			return true;
		}
		else if((this.headword.ner.startsWith("PER") && anaph.headword.ner.startsWith("PER") 
				|| this.headword.ner.startsWith("ORG") && anaph.headword.ner.startsWith("ORG") 
				|| this.headword.ner.startsWith("LOC") && anaph.headword.ner.startsWith("LOC")
				|| this.headword.ner.startsWith("GPE") && anaph.headword.ner.startsWith("GPE"))
				&& this.wordsInclude(sent, anaph, anaphSent, dict)) {
			return true;
		}
		else{
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	private boolean compatibleModifierforProper(Sentence sent, Mention anaph, Sentence anaphSent, Dictionaries dict) {
		Set<String> locModifiersOfAntec = new HashSet<String>();
		Set<String> numModifiersOfAntec = new HashSet<String>();
		for(int i = this.startIndex; i < this.endIndex; ++i) {
			Lexicon antecLex = sent.getLexicon(i);
			if(locationModifiers.contains(antecLex.form.toLowerCase()) && antecLex.basic_head == this.headIndex ) {
				locModifiersOfAntec.add(antecLex.form.toLowerCase());
			}
			if(antecLex.postag.equals("CD")) {
				numModifiersOfAntec.add(antecLex.form.toLowerCase());
			}
		}
		
		Set<String> locModifiersOfAnaph = new HashSet<String>();
		Set<String> numModifiersOfAnaph = new HashSet<String>();
		for(int i = anaph.startIndex; i < anaph.endIndex; ++i) {
			Lexicon anaphLex = anaphSent.getLexicon(i);
			if(locationModifiers.contains(anaphLex.form.toLowerCase())) {
				locModifiersOfAnaph.add(anaphLex.form.toLowerCase());
			}
			if(anaphLex.postag.equals("CD") && !entityWordsToExclude.contains(anaphLex.form.toLowerCase())) {
				numModifiersOfAnaph.add(anaphLex.form.toLowerCase());
			}
		}
		
		if(!numModifiersOfAnaph.containsAll(numModifiersOfAntec) || !numModifiersOfAntec.containsAll(numModifiersOfAnaph)) {
			return false;
		}
		
		if((anaph.headword.ner.equals("GPE") || anaph.headword.ner.startsWith("LOC")) && 
				(!locModifiersOfAnaph.containsAll(locModifiersOfAntec) || !locModifiersOfAntec.containsAll(locModifiersOfAnaph))) {
			return false;
		}
		
		return true;
	}
	
	public boolean compatibleModifier(Sentence sent, Mention anaph, Sentence anaphSent, Dictionaries dict) {
		if(this.isPronominal() || anaph.isPronominal()){
			throw new RuntimeException("error: compatible modifiers does not apply for pronominals");
		}
		
		Set<String> modifiersOfAntec = new HashSet<String>();
		Set<String> possessivePronsOfAntec = new HashSet<String>();
		Set<String> locModifiersOfAntec = new HashSet<String>();
		Set<String> numberModifiersofAntec = new HashSet<String>();
		
		for(int i = this.startIndex; i < this.endIndex; ++i) {
			Lexicon antecLex = sent.getLexicon(i);
			if(antecLex.postag.equals("PRP$") || dict.possessivePronouns.contains(antecLex.form.toLowerCase())) {
				possessivePronsOfAntec.add(antecLex.form.toLowerCase());
			}
			else if(locationModifiers.contains(antecLex.form.toLowerCase()) && antecLex.basic_head == this.headIndex) {
				locModifiersOfAntec.add(antecLex.form.toLowerCase());
			}
			else if(antecLex.postag.equals("CD")) {
				numberModifiersofAntec.add(antecLex.form.toLowerCase());
			}

			modifiersOfAntec.add(antecLex.form.toLowerCase());
		}
		possessivePronsOfAntec.removeAll(excludePossessivePronouns);
		
		Set<String> modifiersOfAnaph = new HashSet<String>();
		Set<String> possessivePronsOfAanph = new HashSet<String>();
		Set<String> locModifiersOfAnaph = new HashSet<String>();
		Set<String> numberModifierOfAnaph = new HashSet<String>();
		
		for(int i = anaph.startIndex; i < anaph.endIndex; ++i) {
			Lexicon anaphLex = anaphSent.getLexicon(i);
			if(anaphLex.postag.equals("PRP$") || dict.possessivePronouns.contains(anaphLex.form.toLowerCase())) {
				possessivePronsOfAanph.add(anaphLex.form.toLowerCase());
			}
			else if(anaphLex.postag.startsWith("JJ") || anaphLex.postag.startsWith("N") 
					||anaphLex.postag.equals("RB")
					|| anaphLex.postag.startsWith("V") || anaphLex.postag.equals("CD")) {
				
				modifiersOfAnaph.add(anaphLex.form.toLowerCase());
				
				if(locationModifiers.contains(anaphLex.form.toLowerCase())) {
					locModifiersOfAnaph.add(anaphLex.form.toLowerCase());
				}
				else if(anaphLex.postag.equals("CD")) {
					numberModifierOfAnaph.add(anaphLex.form.toLowerCase());
				}
			}
		}
		
		modifiersOfAnaph.removeAll(entityWordsToExclude);
		if(anaph.headword.ner.equals("PERSON") || anaph.headword.ner.equals("PER")) {
			modifiersOfAnaph.removeAll(personalTitles);
		}
		possessivePronsOfAanph.removeAll(excludePossessivePronouns);
		
		if(!modifiersOfAntec.containsAll(modifiersOfAnaph)) {
			return false;
		}
		
		if(anaph.isProper() && !numberModifierOfAnaph.containsAll(numberModifiersofAntec)) {
			return false;
		}
		
		if((anaph.headword.ner.equals("GPE") || anaph.headword.ner.startsWith("LOC") || anaph.headword.ner.equals("NORP")) 
				&& !locModifiersOfAnaph.containsAll(locModifiersOfAntec)) {
			return false;
		}
		
		if(!possessivePronsOfAntec.containsAll(possessivePronsOfAanph)) {
			return false;
		}
		
		return true;
	}
	
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
	
	public boolean acronymMatch(Sentence sent, Mention mention, Sentence sentOfM, Dictionaries dict) {
		return this.isProper() && mention.isProper() && 
				(this.isAcronymTo(mention, sentOfM) || mention.isAcronymTo(this, sent)
						|| this.isStateAbrrev(sent, mention, sentOfM, dict) 
						|| mention.isStateAbrrev(sentOfM, this, sent, dict));
	}
	
	public boolean isStateAbrrev(Sentence sent, Mention mention, Sentence sentOfM, Dictionaries dict) {
		String anaphSpan = this.getSpan(sent);
		String antecSpan = mention.getSpan(sentOfM);
		
		// The US state matching part (only) is done cased
		String thisNormed = dict.lookupCanonicalAmericanStateName(anaphSpan);
		String antNormed = dict.lookupCanonicalAmericanStateName(antecSpan);
		if (thisNormed != null && thisNormed.equals(antNormed)) {
			return true;
		}
		return false;
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
	
	private boolean partitiveRule(Sentence sent) {
		return ((headIndex + 1 < endIndex) &&
				parts.contains(headword.form.toLowerCase(Locale.ENGLISH)) &&
				sent.getLexicon(headIndex + 1).form.equalsIgnoreCase("of")) ||
				((headIndex + 1 < endIndex) && (headIndex - 1 >= startIndex) &&
				sent.getLexicon(headIndex + 1).form.equalsIgnoreCase("of") &&
				sent.getLexicon(headIndex - 1).form.equalsIgnoreCase("a") &&
				parts.contains("a " + headword.form.toLowerCase(Locale.ENGLISH)));
	}
	
	private boolean numberOfRule(Sentence sent) {
		return (headIndex + 1 < endIndex) && 
				headword.postag.equals("CD") 
				&& sent.getLexicon(headIndex + 1).form.equalsIgnoreCase("of");
	}
	
	public void correctHeadIndex(Sentence sent, Dictionaries dict) {
		// make sure that the head of a NE is not a known suffix, e.g., Corp.
		if(!headword.ner.equals("O")) {
			int start = headIndex;
			while(start >= 0){
				String head = sent.getLexicon(start).form.toLowerCase();
				if (knownSuffix(head)) {
					start--;
				}
				else {
					this.headString = head;
					this.headIndex = start;
					this.headword = new Lexicon(sent.getLexicon(headIndex));
					setType(sent, dict);
					setDefiniteness(sent, dict);
					setNumber(sent, dict);
					setAnimacy(sent, dict);
					setGender(sent, dict, getGender(sent, dict));
					setPerson(sent, dict);
					return;
				}
			}
		}
		
		// make sure that the head of a mention is not "the"
		if(headString.equals("the")) {
			int start = headIndex + 1;
			while(start < endIndex) {
				String pos = sent.getLexicon(start).postag;
				if(pos.startsWith("V") || pos.startsWith("JJ") || pos.startsWith("N")) {
					this.headString = sent.getLexicon(start).form.toLowerCase();
					this.headIndex = start;
					this.headword = new Lexicon(sent.getLexicon(headIndex));
					setType(sent, dict);
					setDefiniteness(sent, dict);
					setNumber(sent, dict);
					setAnimacy(sent, dict);
					setGender(sent, dict, getGender(sent, dict));
					setPerson(sent, dict);
					return;
				}
				else {
					start++;
				}
			}
		}
		
		// set definite for list member moentions
//		setDefiniteness(sent, dict);
	}
	
	private void getHeadword(Sentence sent, List<Mention> mentions, Dictionaries dict, Set<Mention> remove){
		if(headword == null){
			Lexicon lex = sent.getLexicon(headIndex);
			headword= new Lexicon(lex);
			headString = headword.form.toLowerCase();
		}
		
		// make sure that the NER of a number-of mention is O
		if(numberOfRule(sent)) {
			headword.ner = "O";
		}
		// make sure partitive mention's head is correct
		while(partitiveRule(sent)) {
			for(Mention mention : mentions) {
				if(this.headIndex == mention.headIndex) { 
					if(this.cover(mention)) {
						remove.add(mention);
					}
				}
			}
			//break if this is removed.
			if(remove.contains(this)) {
				break;
			}
			
			int ofPos = headIndex + 1;
			for(int i = ofPos + 1; i < endIndex; ++i) {
				if(sent.getLexicon(i).basic_head == ofPos) {
					headIndex = i;
					this.headword = new Lexicon(sent.getLexicon(headIndex));
					this.headString = headword.form.toLowerCase();
					break;
				}
			}
			//break if new headIndex does not exist
			if(headIndex + 1 == ofPos) {
				break;
			}
		}
	}
	
	public void process(Sentence sent, List<Mention> mentions, Dictionaries dict, Set<Mention> remove) {
		getHeadword(sent, mentions, dict, remove);
		setType(sent, dict);
		setDefiniteness(sent, dict);
		setNumber(sent, dict);
		setAnimacy(sent, dict);
		setGender(sent, dict, getGender(sent, dict));
		setPerson(sent, dict);
	}

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
	
	private void setType(Sentence sent, Dictionaries dict) {
		if(this.isList()) {
			if(this.mentionType == MentionType.PRONOMINAL) {
				this.mentionType = MentionType.NOMINAL;
			}
		}
		else if((endIndex - startIndex) == 1 && headword.ner.equals("O") 
				&& (dict.allPronouns.contains(headString) 
					|| dict.relativePronouns.contains(headString)
//					|| (headword.postag.startsWith("PRP") && headString.equals("'s"))
					|| dict.determiners.contains(headString)
					|| quantDeterminers.contains(headString) 
					|| singularDeterminers.contains(headString)
					|| pluralDeterminers.contains(headString))) {
			mentionType = MentionType.PRONOMINAL;
		}
		else if(!headword.ner.equals("O") || headword.postag.startsWith("NNP")) {
			// CARDINAL, QUANTITY are not regarded as Proper
			if(headword.ner.equals("CARDINAL") || headword.ner.equals("QUANTITY") 
				|| headword.ner.equals("MONEY") || headword.ner.equals("NUMBER")) {
				mentionType = MentionType.NOMINAL;
			}
			else {
				mentionType = MentionType.PROPER;
			}
		}
		else {
			mentionType = MentionType.NOMINAL;
		}
	}
	
	private void setDefiniteness(Sentence sent, Dictionaries dict) {
		if(this.isNominative()) {
			if(this.endIndex - this.startIndex == 1 && dates.contains(headString)) {
				this.definite = Definiteness.DEFINITE;
				return;
			}
			
			this.definite = Definiteness.GENERIC;
			for(int i = this.startIndex; i <= this.headIndex; ++i) {
				String word = sent.getLexicon(i).form.toLowerCase();
				String pos = sent.getLexicon(i).postag;
				if(word.equals("its") || pos.equals("POS") || dict.determiners.contains(word) || quantDeterminers.contains(word)) {
					this.definite = Definiteness.DEFINITE;
					return;
				}
				else if((word.equals("a") || word.equals("an")) && pos.equals("DT")) {
					this.definite = Definiteness.GENERIC;
					return;
				}
			}
			
			if(this.belongTo != null) {
				for(int i = this.belongTo.startIndex; i <= this.belongTo.headIndex; ++i) {
					String word = sent.getLexicon(i).form.toLowerCase();
					String pos = sent.getLexicon(i).postag;
					if(word.equals("its") || pos.equals("POS") || dict.determiners.contains(word) || quantDeterminers.contains(word)) {
						this.definite = Definiteness.DEFINITE;
						return;
					}
					else if((word.equals("a") || word.equals("an")) && pos.equals("DT")) {
						this.definite = Definiteness.GENERIC;
						return;
					}
				}
			}
		}
		else if(this.isList()) {
			// proper is always definite
			if(this.mentionType == MentionType.PROPER) {
				this.definite = Definiteness.DEFINITE;
				return;
			}
			
			this.definite = Definiteness.GENERIC;
			for(Mention member : this.listMember) {
				if(member.isDefinite()) {
					this.definite = Definiteness.DEFINITE;
					return;
				}
			}
		}
		else {
			this.definite = Definiteness.DEFINITE;
		}
	}
	
	private void setNumber(Sentence sent, Dictionaries dict) {
		if(this.isList()) {
			number = Number.PLURAL;
		}
		else if(this.isPronominal()) {
			if (dict.pluralPronouns.contains(headString) || pluralDeterminers.contains(headString)) {
				number = Number.PLURAL;
			}
			else if(dict.singularPronouns.contains(headString) || singularDeterminers.contains(headString)) {
				number = Number.SINGULAR;
			}
			else if(headString.equals("'s")) {
				number = Number.PLURAL;
			}
			else if(quantDeterminers.contains(headString)) {
				number = Number.PLURAL;
			}
			else {
				number = Number.UNKNOWN;
			}
		}
		else if(numberOfRule(sent)) {
			if(headword.form.equalsIgnoreCase("one")) {
				number = Number.SINGULAR;
			}
			else if(headword.form.equalsIgnoreCase("half")) {
				number = Number.UNKNOWN;
			}
			else {
				number = Number.PLURAL;
			}
		}
		else if(!headword.ner.equals("O") && !this.isNominative()) {
			if(headword.ner.equals("NORP")) {
				// NORP can be both plural and singular
				if((dict.isAdjectivalDemonym(headword.form) || headword.ner.equals("NORP")) 
						&& headString.endsWith("s")) {
					number = Number.PLURAL;
				}
				else if(headword.postag.startsWith("N") && headword.postag.endsWith("S")) {
					number = Number.PLURAL;
				}
				else if(headword.postag.startsWith("N")) {
					number = Number.SINGULAR;
				}
				else {
					number = Number.UNKNOWN;
				}
			}
			else {
				number = Number.SINGULAR;
			}
		}
		else {
			if(headword.postag.startsWith("N") && headword.postag.endsWith("S")) {
				number = Number.PLURAL;
			}
			else if(headword.postag.startsWith("N")) {
				number = Number.SINGULAR;
			}
			else {
				number = Number.UNKNOWN;
			}
		}
		
		if(!this.isPronominal()) {
			if(number == Number.UNKNOWN) {
				if (dict.pluralPronouns.contains(headString) || pluralDeterminers.contains(headString)) {
					number = Number.PLURAL;
				}
				else if(dict.singularPronouns.contains(headString) || singularDeterminers.contains(headString)) {
					number = Number.SINGULAR;
				}
				else if(headString.equals("you")) {
					if(headIndex - 2 >= startIndex && sent.getLexicon(headIndex - 1).form.equals("of")
						&& (sent.getLexicon(headIndex - 2).form.toLowerCase().equals("both") 
							|| sent.getLexicon(headIndex - 2).form.toLowerCase().equals("all"))) {
						number = Number.PLURAL;
					}
				}
				else if(dict.singularWords.contains(headString)){
					number = Number.SINGULAR;
				}
				else if(dict.pluralWords.contains(headString)){
					number = Number.PLURAL;
				}
			}
		}
	}
	
	private Gender getGender(Sentence sent, Dictionaries dict) {
		LinkedList<String> mStr = new LinkedList<String>();
		for(int i = startIndex; i <= headIndex; ++i) {
			mStr.add(sent.getLexicon(i).form.toLowerCase());
		}
		
		int len = mStr.size();
		
		char firstLetter = headword.form.charAt(0);
		if(len > 1 && Character.isUpperCase(firstLetter) && headword.ner.equals("PERSON")) {
			int firstNameIdx = len - 2;
			String secondToLast = mStr.get(firstNameIdx);
			if(firstNameIdx > 1 && (secondToLast.length() == 1 || (secondToLast.length() == 2 && secondToLast.endsWith(".")))) {
				firstNameIdx--;
			}
			secondToLast = mStr.get(firstNameIdx);
			
			for(int i = 0; i <= firstNameIdx; ++i) {
				if(dict.genderNumber.containsKey(mStr)) {
					return dict.genderNumber.get(mStr);
				}
				mStr.poll();
			}
			
			List<String> convertedStr = new ArrayList<String>(2);
			convertedStr.add(secondToLast);
			convertedStr.add("!");
			if(dict.genderNumber.containsKey(convertedStr)) { 
				return dict.genderNumber.get(convertedStr);
			}

			if(dict.genderNumber.containsKey(convertedStr.subList(0, 1))) {
				return dict.genderNumber.get(convertedStr.subList(0, 1));
			}
		}
		
//		if(len > 0) {
//			List<String> convertedStr = new ArrayList<String>(1);
//			convertedStr.add(headString);
//			if(dict.genderNumber.containsKey(convertedStr)) {
//				return dict.genderNumber.get(convertedStr);
//			}
//		}
		
		return null;
	}
	
	private void setGender(Sentence sent, Dictionaries dict, Gender genderNumberResult) {
		gender = Gender.UNKNOWN;
		if(genderNumberResult != null && this.number != Number.PLURAL) {
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
		else {
			if(gender == Gender.UNKNOWN) {
				if (dict.malePronouns.contains(headString)) {
					gender = Gender.MALE;
				} 
				else if (dict.femalePronouns.contains(headString)) {
					gender = Gender.FEMALE;
				}
				else if(headword.ner.equals("PERSON") || headword.ner.equals("PER")) {
					int[] ids = getNerSpan(sent);
					for(int j = ids[0]; j < ids[1]; ++j) {
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
					if(dict.maleWords.contains(headString) || dict.maleWords.contains(headword.lemma.toLowerCase())) {
						gender = Gender.MALE;
					}
					else if(dict.femaleWords.contains(headString) || dict.femaleWords.contains(headword.lemma.toLowerCase())) {
						gender = Gender.FEMALE;
					}
					else if(dict.neutralWords.contains(headString) || dict.neutralWords.contains(headword.lemma.toLowerCase())) {
						gender = Gender.NEUTRAL;
					}
					else if((this.isProper() || this.isNominative()) && animacy == Animacy.INANIMATE) {
						gender = Gender.NEUTRAL;
					}
				}
			}
		}
	}
	
	//re-define animate and inanimate pronouns
	private final Set<String> animatePronouns = Generics.newHashSet(Arrays.asList(new String[]{ "i", "me", "myself", "mine", "my", "we", "us", "ourself", "ourselves", "ours", "our", "you", "yourself", "yours", "your", "yourselves", "he", "him", "himself", "his", "she", "her", "herself", "hers", "her", "who", "whom"}));
	private final Set<String> inanimatePronouns = Generics.newHashSet(Arrays.asList(new String[]{"it", "itself", "its", "where", "when", "the", "that", "this", "those", "these"}));
	
	private void setAnimacy(Sentence sent, Dictionaries dict) {
		if(this.isPronominal()) {
			if(animatePronouns.contains(headString) || headString.equals("'s")) {
				animacy = Animacy.ANIMATE;
			}
			else if(inanimatePronouns.contains(headString)) {
				animacy = Animacy.INANIMATE;
			}
			else {
				animacy = Animacy.UNKNOWN;
			}
		}
		else if(headword.ner.equals("PERSON") || headword.ner.equals("PER")) {
			animacy = Animacy.ANIMATE;
		}
		else if(headword.ner.equals("NORP")) {
			animacy = Animacy.ANIMATE;
		}
		else if(headword.ner.equals("GPE")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("LOCATION") || headword.ner.startsWith("LOC")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("MONEY")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("CARDINAL")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("ORDINAL")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("QUANTITY")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("NUMBER")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("PERCENT")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("DATE")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("TIME")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("LANGUAGE")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("MISC")) {
			animacy = Animacy.UNKNOWN;
		}
		else if(headword.ner.equals("FAC")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("VEH")) {
			animacy = Animacy.UNKNOWN;
		}
		else if(headword.ner.equals("WEA")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("PRODUCT")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("ORG")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("LAW")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("EVENT")) {
			animacy = Animacy.INANIMATE;
		}
		else if(headword.ner.equals("WORK_OF_ART")) {
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
				else if(animatePronouns.contains(headString)) {
					animacy = Animacy.ANIMATE;
				}
				else if(inanimatePronouns.contains(headString)) {
					animacy = Animacy.INANIMATE;
				}
				else {
					animacy = Animacy.UNKNOWN;
				}
			}
		}
	}
	
	private void setPerson(Sentence sent, Dictionaries dict) {
		if(!this.isPronominal()) {
			if(this.isList()) {
				return;
			}
			else if(headword.ner.equals("O") 
					&& dict.firstPersonPronouns.contains(headString) && headword.postag.equals("PRP")) {
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
			else if(headword.ner.equals("O")
					&& dict.secondPersonPronouns.contains(headString) && headword.postag.equals("PRP")) {
				person = Person.YOU;
			}
			else if(this.number == Number.UNKNOWN) {
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
		if(dict.firstPersonPronouns.contains(spanToString) || spanToString.equals("'s")) {
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
		else if(singularDeterminers.contains(spanToString)) {
			person = Person.IT;
		}
		else if(pluralDeterminers.contains(spanToString)) {
			person = Person.THEY;
		}
		else if(quantDeterminers.contains(spanToString)) {
			person = Person.THEY;
		}
		else {
			person = Person.UNKNOWN;
		}
	}
	
	private int[] getNerSpan(Sentence sent) {
		if(headword.ner.equals("O")) {
			return null;
		}
		
		int start = headIndex;
		while(start > 0) {
			String preNer = sent.getLexicon(start - 1).ner;
			if(headword.ner.equals(preNer)) {
				start--;
			}
			else{
				break;
			}
		}
		
		int end = headIndex + 1;
		while(end < sent.length()) {
			String nextNer = sent.getLexicon(end).ner;
			if(headword.ner.equals(nextNer)) {
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
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Mention)) {
			return false;
		}
		Mention m = (Mention) obj;
		return this.sentID == m.sentID && this.startIndex == m.startIndex && this.endIndex == m.endIndex ;
	}
	
	@Override
	public int hashCode() {
		return this.sentID ^ this.startIndex ^ this.endIndex;
	}
	
	public void display(Sentence sent, PrintStream printer) {
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
		int[] nerSpan = this.getNerSpan(sent);
		if(nerSpan != null) {
			printer.println("mention ner span " + nerSpan[0] + " " + nerSpan[1]);
		}
		printer.println("mention speaker: " + (this.speakerInfo == null ? "null" : this.speakerInfo.toString() + " " + this.speakerInfo.getSpeakerMentionId() + " " + this.speakerInfo.getSpeakerName() + " " + this.speakerInfo.getSpkeakerNameAsOneString()));
		printer.println("previous speaker: " + (this.preSpeakerInfo == null ? "null" : this.preSpeakerInfo.toString() + " " + this.preSpeakerInfo.getSpeakerName()));
		printer.println("list member of: " + (this.belongTo == null ? " null" : this.belongTo.mentionID));
		printer.println("predicate nominative to: " + (this.predNomiTo == null ? "null" : this.predNomiTo.mentionID));
		printer.print("event list: ");
		for(Event event : eventSet) {
			printer.print(event.toString() + " ");
		}
		printer.println();
		printer.println("#end Mention " + this.mentionID);
		printer.println("===================================");
		printer.flush();
	}
	
	public void display(PrintStream printer) {
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
		printer.println("mention speaker: " + (this.speakerInfo == null ? "null" : this.speakerInfo.toString() + " " + this.speakerInfo.getSpeakerMentionId() + " " + this.speakerInfo.getSpeakerName() + " " + this.speakerInfo.getSpkeakerNameAsOneString()));
		printer.println("previous speaker: " + (this.preSpeakerInfo == null ? "null" : this.preSpeakerInfo.toString() + " " + this.preSpeakerInfo.getSpeakerName()));
		printer.println("list member of: " + (this.belongTo == null ? " null" : this.belongTo.mentionID));
		printer.println("predicate nominative to: " + (this.predNomiTo == null ? "null" : this.predNomiTo.mentionID));
		printer.print("event list: ");
		for(Event event : eventSet) {
			printer.print(event.toString() + " ");
		}
		printer.println();
		printer.println("#end Mention " + this.mentionID);
		printer.println("===================================");
		printer.flush();
	}
}
