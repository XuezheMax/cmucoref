package cmucoref.mention.eventextractor;

import java.util.List;

import cmucoref.mention.*;
import cmucoref.model.Options;
import cmucoref.document.*;

public abstract class EventExtractor {
	
	public static EventExtractor createExtractor(String extractorClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (EventExtractor) Class.forName(extractorClassName).newInstance();
	}
	
	public EventExtractor() {}
	
	public abstract void extractEvents(Document doc, List<List<Mention>> mentionList, Options options);
}
