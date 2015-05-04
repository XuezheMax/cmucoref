package cmucoref.decoder;

import java.io.IOException;
import java.util.List;

import cmucoref.document.Document;
import cmucoref.manager.CorefManager;
import cmucoref.mention.Mention;
import cmucoref.model.CorefModel;
import cmucoref.model.FeatureVector;

public class Decoder {
	public Decoder(){}
	
	public List<List<Mention>> decode(Document doc, CorefManager manager, CorefModel model) throws IOException{
		List<List<Mention>> mentionList = manager.mentionExtractor.extractPredictedMentions(doc, model.options);
		List<Mention> allMentions = manager.mentionExtractor.getSingleMentionList(doc, mentionList, model.options);
		
		for(int i = 0; i < allMentions.size(); ++i){
			Mention antecedent = null;
			double score = Double.NEGATIVE_INFINITY;
			Mention anaph = allMentions.get(i);
			
			if(anaph.preciseMatchs != null){
				int length = anaph.preciseMatchs.size();
				for(int j = length - 1; j >= 0; --j){
					Mention antec = anaph.preciseMatchs.get(j);
					FeatureVector fv = new FeatureVector();
					manager.mentionFeatGen.genCoreferentFeatures(anaph, doc.getSentence(anaph.sentID), 
							antec, doc.getSentence(antec.sentID), manager.getDict(), model, fv);
					double prob = model.getScore(fv);
					if(prob > score){
						score = prob;
						antecedent = antec;
					}
				}
				FeatureVector fv = new FeatureVector();
				manager.mentionFeatGen.genNewClusterFeatures(anaph, doc.getSentence(anaph.sentID), model, fv);
				double prob = model.getScore(fv);
				if(prob > score){
					antecedent = null;
				}
			}
			else{
				for(int j = i - 1; j >= 0; --j){
					Mention antec = allMentions.get(j);
					if(anaph.ruleout(antec, manager.getDict())) {
						continue;
					}
					
					FeatureVector fv = new FeatureVector();
					manager.mentionFeatGen.genCoreferentFeatures(anaph, doc.getSentence(anaph.sentID), 
							antec, doc.getSentence(antec.sentID), manager.getDict(), model, fv);
					
					double prob = model.getScore(fv);
					if(prob > score){
						score = prob;
						antecedent = antec;
					}
				}
				FeatureVector fv = new FeatureVector();
				manager.mentionFeatGen.genNewClusterFeatures(anaph, doc.getSentence(anaph.sentID), model, fv);
				double prob = model.getScore(fv);
				if(prob > score){
					antecedent = null;
				}
			}
			
			if(antecedent == null){
				anaph.setRepres();
			}
			else{
				anaph.setAntec(antecedent);
			}
		}
		
		return mentionList;
	}
}
