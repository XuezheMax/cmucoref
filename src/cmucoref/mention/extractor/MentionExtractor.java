package cmucoref.mention.extractor;

import cmucoref.document.Document;
import cmucoref.mention.Mention;

import java.util.List;
import edu.stanford.nlp.dcoref.Dictionaries;

public abstract class MentionExtractor {
	public abstract List<List<Mention>> extractPredictedMentions(Document doc, Dictionaries dict);
}
