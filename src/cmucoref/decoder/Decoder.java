package cmucoref.decoder;

import java.io.IOException;
import java.util.List;

import cmucoref.document.Document;
import cmucoref.document.Sentence;
import cmucoref.manager.CorefManager;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;

public class Decoder {
	public Decoder(){}
	
	public static Decoder createDecoder(String extractorClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (Decoder) Class.forName(extractorClassName).newInstance();
	}
	
	private void decodeMention(Document doc, Mention anaph, int anaphId, List<Mention> allMentions, CorefManager manager, CorefModel model) {
		Mention antecedent = null;
		double score = Double.NEGATIVE_INFINITY;
		Sentence anaphSent = doc.getSentence(anaph.sentID);
		
		boolean useEvent = false;
		
		if(anaph.preciseMatchs != null) {
			for(Mention antec : anaph.preciseMatchs) {
				Sentence antecSent = doc.getSentence(antec.sentID);
				FeatureVector mfv = new FeatureVector();
				manager.mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 2, 
						manager.getDict(), model, useEvent, mfv, null);
				double prob = model.getScore(mfv, null);
				if(prob > score) {
					score = prob;
					antecedent = antec;
				}
			}
		}
		else if(anaph.stringMatchs != null) {
			int length = anaph.stringMatchs.size();
			for(int j = length - 1; j >= 0; --j) {
//			for(int j = 0; j < length; ++j) {
				Mention antec = anaph.stringMatchs.get(j);
				Sentence antecSent = doc.getSentence(antec.sentID);
				FeatureVector mfv = new FeatureVector();
				manager.mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 1,
						manager.getDict(), model, useEvent, mfv, null);
				double prob = model.getScore(mfv, null);
				if(prob > score) {
					score = prob;
					antecedent = antec;
				}
			}
		}
		else {
			for(int j = anaphId - 1; j >= 0; --j) {
				Mention antec = allMentions.get(j);
				Sentence antecSent = doc.getSentence(antec.sentID);
				
				if(anaph.ruleout(anaphSent, antec, antecSent, manager.getDict())) {
					continue;
				}
				
				FeatureVector mfv = new FeatureVector();
				manager.mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 0, 
						manager.getDict(), model, useEvent, mfv, null);
				
				double prob = model.getScore(mfv, null);
				if(prob > score){
					score = prob;
					antecedent = antec;
				}
			}
			FeatureVector mfv = new FeatureVector();
			manager.mentionFeatGen.genNewClusterFeatures(anaph, anaphSent, model, useEvent, mfv, null);
			double prob = model.getScore(mfv, null);
			if(prob > score) {
				antecedent = null;
			}
		}
		
		if(antecedent == null) {
			anaph.setRepres();
		}
		else{
			anaph.setAntec(antecedent);
		}
	}
	
	private void decodeMentionWithEvent(Document doc, Mention anaph, int anaphId, List<Mention> allMentions, CorefManager manager, CorefModel model) {
		Mention antecedent = null;
		double score = Double.NEGATIVE_INFINITY;
		Sentence anaphSent = doc.getSentence(anaph.sentID);
		
		boolean useEvent = true;
		
		if(anaph.preciseMatchs != null) {
			for(Mention antec : anaph.preciseMatchs) {
				Sentence antecSent = doc.getSentence(antec.sentID);
				FeatureVector mfv = new FeatureVector();
				FeatureVector efv = new FeatureVector();
				manager.mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 2, 
						manager.getDict(), model, useEvent, mfv, efv);
				double prob = model.getScore(mfv, efv);
				if(prob > score) {
					score = prob;
					antecedent = antec;
				}
			}
		}
		else if(anaph.stringMatchs != null) {
			int length = anaph.stringMatchs.size();
			for(int j = length - 1; j >= 0; --j) {
//			for(int j = 0; j < length; ++j) {
				Mention antec = anaph.stringMatchs.get(j);
				Sentence antecSent = doc.getSentence(antec.sentID);
				FeatureVector mfv = new FeatureVector();
				FeatureVector efv = new FeatureVector();
				manager.mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 1,
						manager.getDict(), model, useEvent, mfv, efv);
				double prob = model.getScore(mfv, efv);
				if(prob > score) {
					score = prob;
					antecedent = antec;
				}
			}
		}
		else {
			for(int j = anaphId - 1; j >= 0; --j) {
				Mention antec = allMentions.get(j);
				Sentence antecSent = doc.getSentence(antec.sentID);
				
				if(anaph.ruleout(anaphSent, antec, antecSent, manager.getDict())) {
					continue;
				}
				
				FeatureVector mfv = new FeatureVector();
				FeatureVector efv = new FeatureVector();
				manager.mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 0, 
						manager.getDict(), model, useEvent, mfv, efv);
				
				double prob = model.getScore(mfv, efv);
				if(prob > score){
					score = prob;
					antecedent = antec;
				}
			}
			FeatureVector mfv = new FeatureVector();
			FeatureVector efv = new FeatureVector();
			manager.mentionFeatGen.genNewClusterFeatures(anaph, anaphSent, model, useEvent, mfv, efv);
			double prob = model.getScore(mfv, efv);
			if(prob > score) {
				antecedent = null;
			}
		}
		
		if(score == Double.NEGATIVE_INFINITY) {
			decodeMention(doc, anaph, anaphId, allMentions, manager, model);
		}
		else {
			if(antecedent == null) {
				anaph.setRepres();
			}
			else{
				anaph.setAntec(antecedent);
			}
		}
	}
	
	public List<List<Mention>> decode(Document doc, CorefManager manager, CorefModel model, boolean useEvent) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		List<List<Mention>> mentionList = manager.mentionExtractor.extractPredictedMentions(doc, model.options);
		List<Mention> allMentions = manager.mentionExtractor.getSingleMentionList(doc, mentionList, model.options);
		for(int i = 0; i < allMentions.size(); ++i) {
			Mention anaph = allMentions.get(i);
			if(useEvent) {
				decodeMentionWithEvent(doc, anaph, i, allMentions, manager, model);
			}
			else {
				decodeMention(doc, anaph, i, allMentions, manager, model);
			}
		}
		
		return mentionList;
	}
}
