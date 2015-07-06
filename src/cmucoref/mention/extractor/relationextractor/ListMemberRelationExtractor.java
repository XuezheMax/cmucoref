package cmucoref.mention.extractor.relationextractor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import cmucoref.document.Lexicon;
import cmucoref.document.Sentence;
import cmucoref.mention.Mention;
import cmucoref.util.Pair;

public class ListMemberRelationExtractor extends RelationExtractor {
	
	public ListMemberRelationExtractor(){}

	@Override
	public Set<Pair<Integer, Integer>> extractRelation(Sentence sent, List<Mention> mentions) {
		Set<Pair<Integer, Integer>> relationSet = new HashSet<Pair<Integer, Integer>>();
		HashSet<Integer> isList = new HashSet<Integer>();
		Map<Integer, List<Mention>> listToMembersMap = new HashMap<Integer, List<Mention>>();
		
		List<Mention> memberCands = getListMemberCandidates(sent, mentions);
		
		for(Mention cand : memberCands) {
//			int parIndex = cand.headword.basic_head;
			int parIndex = sent.getLexicon(cand.originalHeadIndex).basic_head;
			Mention list = null;
			for(Mention mention2 : mentions) {
				if(cand.equals(mention2)) {
					continue;
				}
				
//				if((mention2.headIndex == parIndex) && (mention2.cover(cand))) {
				if((mention2.originalHeadIndex == parIndex) && (mention2.cover(cand))) {
					if(list == null || mention2.cover(list)) {
						list = mention2;
					}
				}
			}
			if(list != null) {
				relationSet.add(new Pair<Integer, Integer>(cand.mentionID, list.mentionID));
				isList.add(list.mentionID);
				List<Mention> members = listToMembersMap.get(list.mentionID);
				if(members == null) {
					members = new ArrayList<Mention>();
					listToMembersMap.put(list.mentionID, members);
				}
				members.add(cand);
			}
		}
		
		for(Mention list : mentions) {
			if(isList.contains(list.mentionID)) {
				Mention firstMember = getFirstMember(list, listToMembersMap.get(list.mentionID), mentions);
				if(firstMember == null) {
//					list.display(sent, System.err);
					//throw new RuntimeException("cannot find first list member");
				}
				else {
					relationSet.add(new Pair<Integer, Integer>(firstMember.mentionID, list.mentionID));
				}
			}
		}
		return relationSet;
	}
	
	private Mention getFirstMember(Mention list, List<Mention> listMembers, List<Mention> mentions) {
		Mention firstMember = null;
		for(Mention member : mentions) {
			if(checkFirstMember(list, listMembers, member)) {
				if(firstMember == null || member.cover(firstMember)) {
					firstMember = member;
				}
			}
		}
		return firstMember;
	}
	
	private boolean checkFirstMember(Mention list, List<Mention> listMembers, Mention member) {
//		if(!(list.cover(member) && list.headIndex == member.headIndex)) {
		if(!(list.cover(member) && list.originalHeadIndex == member.originalHeadIndex)) {
			return false;
		}
		
		for(Mention listMember : listMembers) {
			if(member.cover(listMember)) {
				return false;
			}
		}
		return true;
	}
	
	private List<Mention> getListMemberCandidates(Sentence sent, List<Mention> mentions) {
		List<Mention> memberCands = new ArrayList<Mention>();
		for(Mention cand : mentions) {
//			Lexicon headword = cand.headword;
			Lexicon headword = sent.getLexicon(cand.originalHeadIndex);
			if(headword.basic_deprel.equals("conj")) {
				boolean isLargestSpan = true;
				for(Mention mention : mentions) {
//					if(mention.headIndex == cand.headIndex && mention.cover(cand)) {
					if(mention.originalHeadIndex == cand.originalHeadIndex && mention.cover(cand)) {
						isLargestSpan = false;
						break;
					}
				}
				if(isLargestSpan) {
					memberCands.add(cand);
				}
			}
		}
		return memberCands;
	}
}
