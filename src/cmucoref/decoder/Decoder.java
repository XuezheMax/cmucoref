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
	
	public List<List<Mention>> decode(Document doc, CorefManager manager, CorefModel model) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		List<List<Mention>> mentionList = manager.mentionExtractor.extractPredictedMentions(doc, model.options);
		List<Mention> allMentions = manager.mentionExtractor.getSingleMentionList(doc, mentionList, model.options);
		
		for(int i = 0; i < allMentions.size(); ++i) {
			Mention antecedent = null;
			double score = Double.NEGATIVE_INFINITY;
			Mention anaph = allMentions.get(i);
			Sentence anaphSent = doc.getSentence(anaph.sentID);
			
			if(anaph.preciseMatchs != null) {
				for(Mention antec : anaph.preciseMatchs) {
					Sentence antecSent = doc.getSentence(antec.sentID);
					FeatureVector fv = new FeatureVector();
					manager.mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 2, 
							manager.getDict(), model, fv);
					double prob = model.getScore(fv);
					if(prob > score) {
						score = prob;
						antecedent = antec;
					}
				}
			}
			else if(anaph.stringMatchs != null) {
				int length = anaph.stringMatchs.size();
				for(int j = length - 1; j >= 0; --j) {
//				for(int j = 0; j < length; ++j) {
					Mention antec = anaph.stringMatchs.get(j);
					Sentence antecSent = doc.getSentence(antec.sentID);
					FeatureVector fv = new FeatureVector();
					manager.mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 1,
							manager.getDict(), model, fv);
					double prob = model.getScore(fv);
					if(prob > score) {
						score = prob;
						antecedent = antec;
					}
				}
			}
			else {
				for(int j = i - 1; j >= 0; --j) {
					Mention antec = allMentions.get(j);
					Sentence antecSent = doc.getSentence(antec.sentID);
					
					if(anaph.ruleout(anaphSent, antec, antecSent, manager.getDict())) {
						continue;
					}
					
					FeatureVector fv = new FeatureVector();
					manager.mentionFeatGen.genCoreferentFeatures(anaph, anaphSent, antec, antecSent, 0, 
							manager.getDict(), model, fv);
					
					double prob = model.getScore(fv);
					if(prob > score){
						score = prob;
						antecedent = antec;
					}
				}
				FeatureVector fv = new FeatureVector();
				manager.mentionFeatGen.genNewClusterFeatures(anaph, anaphSent, model, fv);
				double prob = model.getScore(fv);
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
		
		return mentionList;
	}
}
