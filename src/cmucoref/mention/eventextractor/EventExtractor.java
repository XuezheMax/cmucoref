package cmucoref.mention.eventextractor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cmucoref.mention.*;
import cmucoref.model.Options;
import cmucoref.document.*;

public abstract class EventExtractor {
	
	protected static final Set<String> copulas = new HashSet<String>(Arrays.asList("appear", "be", "become", "seem", "remain"));
	private static final Set<String> roles = new HashSet<String>(Arrays.asList("nsubj", "nsubjpass", "iobj", "dobj", "agent", "xsubj", "xcomp"));
	
	public static EventExtractor createExtractor(String extractorClassName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (EventExtractor) Class.forName(extractorClassName).newInstance();
	}
	
	public EventExtractor() {}
	
	public abstract void extractEvents(Document doc, List<List<Mention>> mentionList, Options options);
	
	protected boolean indexOutsideMention(int index, Mention mention) {
		return index < mention.startIndex || mention.endIndex <= index;
	}
	
	public static boolean acceptableGrammRole(String grammRole, boolean includePrep) {
		return roles.contains(grammRole) || includePrep && grammRole.startsWith("prep_");
	}
}
