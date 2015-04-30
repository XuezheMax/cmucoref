package cmucoref.mention;

import java.util.HashSet;
import java.util.Set;

public class MentionCluster {
	public Mention representative = null;
	public int clusterID = -1;
	
	public Set<Mention> mentionSet = null;
	
	public MentionCluster(){
		mentionSet = new HashSet<Mention>();
	}
	
	public MentionCluster(Mention repres) {
		this();
		this.representative = repres;
		clusterID = repres.mentionID;
		mentionSet.add(repres);
	}
	
	public int size(){
		return mentionSet.size();
	}
	
	public boolean isSingleton(){
		return mentionSet.size() == 1;
	}
	
	public boolean add(Mention mention){
		return mentionSet.add(mention);
	}
}
