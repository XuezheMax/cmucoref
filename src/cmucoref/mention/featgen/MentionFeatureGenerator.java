package cmucoref.mention.featgen;

import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;

public class MentionFeatureGenerator {
	public MentionFeatureGenerator(){}
	
	public void genMentionFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, CorefModel model, FeatureVector fv){
		// anaph starts a new cluster
		if(antec == null){
			genNewClusterFeatures(anaph, anaphSent, model, fv);
			return;
		}
		
		// anaph is coreferent with previous antec 
		if((anaph.mentionType == MentionType.NOMINAL || anaph.mentionType == MentionType.PROPER)
			&& (antec.mentionType == MentionType.NOMINAL || antec.mentionType == MentionType.PROPER)){
			//string match feature
			genStringMatchFeatures(anaph, anaphSent, antec, antecSent, model, fv);
			//head match feature
			genHeadFeatures(anaph, anaphSent, antec, antecSent, model, fv);
		}
	}
	
	protected void genStringMatchFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, CorefModel model, FeatureVector fv){
		//extract string match
		String anaphSpan = anaph.getSpan(anaphSent).toLowerCase();
		String antecSpan = antec.getSpan(antecSent).toLowerCase();
		boolean extractStrMatch = anaphSpan.equals(antecSpan) || anaphSpan.equals(antecSpan + " 's") || antecSpan.equals(anaphSpan + " 's");
		addFeature("EXMAT=" + Boolean.toString(extractStrMatch) + "&" + "ANAPHTYPE=" + anaph.mentionType, "EXMAT&" + "ANTECTYPE=" + antec.mentionType.toString(), model, fv);
		
		//relaxed string match
		String anaphSpanBeforeHead = anaph.getSpanBeforeHead(anaphSent).toLowerCase();
		String antecSpanBeforeHead = antec.getSpanBeforeHead(antecSent).toLowerCase();
		boolean relaxedStrMatch = anaphSpanBeforeHead.equals(antecSpanBeforeHead) 
								|| anaphSpanBeforeHead.equals(antecSpanBeforeHead + " 's")
								|| antecSpanBeforeHead.equals(anaphSpanBeforeHead + " 's");
		addFeature("RLMAT=" + Boolean.toString(relaxedStrMatch) + "&" + "ANAPHTYPE=" + anaph.mentionType, "RLMAT&" + "ANTECTYPE=" + antec.mentionType.toString(), model, fv);
		
		//Acronym
		boolean isAcronym = anaph.isAcronymTo(antec, antecSent) || antec.isAcronymTo(anaph, anaphSent);
		addFeature("ACRONYM=" + Boolean.toString(isAcronym), "ACRONYM", model, fv);
		
		//antec includes all non-stop words of anaph
		boolean wordIncluded = antec.wordsIncluded(antecSent, anaph, anaphSent);
		addFeature("WRDIN=" + Boolean.toString(wordIncluded)+ "&" + "ANAPHTYPE=" + anaph.mentionType, "WRDIN&" + "ANTECTYPE=" + antec.mentionType.toString(), model, fv);
		
		//head match
		boolean headMatch = anaph.headString.equals(antec.headString);
		addFeature("HDMAT=" + Boolean.toString(headMatch) + "&" + "ANAPHTYPE=" + anaph.mentionType, "HDMAT&" + "ANTECTYPE=" + antec.mentionType.toString(), model, fv);
	}
	
	protected void genHeadFeatures(Mention anaph, Sentence anaphSent, Mention antec, Sentence antecSent, CorefModel model, FeatureVector fv){
		
	}
	
	protected void genNewClusterFeatures(Mention anaph, Sentence anaphSent, CorefModel model, FeatureVector fv){
		
	}
	
	protected final void addFeature(String feat, String given, CorefModel model, FeatureVector fv){
		int num = model.getFeatureIndex(feat, given);
		if(fv != null && num >= 0){
			fv.add(num, 1.0);
		}
	}
}
