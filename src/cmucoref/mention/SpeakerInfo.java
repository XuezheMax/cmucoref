package cmucoref.mention;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class SpeakerInfo {
	private String speakerId;
	private String speakerName;
	private String speakerDesc;
	private String[] speakerNameStrings; // tokenized speaker name
	private Mention speaker = null;
	private Set<Mention> mentions = new LinkedHashSet<Mention>();  // Mentions that corresponds to the speaker...

	protected static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+|_+");
	
	private boolean isQuotationSpeaker = false;
	
	public SpeakerInfo(int speakerId, String speakerName, boolean isQuotationSpeaker) {
		this.speakerId = "PER" + speakerId;
		this.isQuotationSpeaker = isQuotationSpeaker;
		int commaPos = speakerName.indexOf(',');
		if(commaPos > 0) {
			// drop everything after the ,
			this.speakerName = speakerName.substring(0, commaPos);
			
			if(commaPos < speakerName.length()) {
				speakerDesc = speakerName.substring(commaPos + 1).trim();
				if(speakerDesc.isEmpty()) {
					speakerDesc = null;
				}
			}
		}
		else {
			this.speakerName = speakerName;
		}
		
		this.speakerNameStrings = WHITESPACE_PATTERN.split(this.speakerName.toLowerCase());
	}
	
	public void setSpeaker(Mention mention) {
		this.speaker = mention;
	}
	
	public Mention getSpeaker() {
		return speaker;
	}
	
	public int getSpeakerMentionId() {
		return speaker == null ? -1 : speaker.mentionID;
	}
	
	public String getSpeakerName() {
		return speakerName;
	}

	public String getSpeakerDesc() {
		return speakerDesc;
	}

	public String[] getSpeakerNameStrings() {
		return speakerNameStrings;
	}
	
	public String getSpkeakerNameAsOneString() {
		String str = "[";
		for(String name : speakerNameStrings) {
			str += name + " ";
		}
		str = str.trim() + "]";
		return str;
	}

	public Set<Mention> getMentions() {
		return mentions;
	}
	
	public String toString() {
		return speakerId;
	}

	public boolean containsMention(Mention m) {
		return mentions.contains(m);
	}
	
	public void addMention(Mention mention) {
		if(mentions.isEmpty() && mention.isProper()) {
			// check if mention name is probably better indicator of the speaker
			// TODO
		}
		mentions.add(mention);
	}
	
	public boolean equals(SpeakerInfo speakerInfo) {
		return this.speakerId.equals(speakerInfo.toString());
	}
	
	public boolean isQuotationSpeaker() {
		return isQuotationSpeaker;
	}
	
	public boolean isDefaultSpeaker() {
		return this.speakerId.equals("PER0");
	}
}
