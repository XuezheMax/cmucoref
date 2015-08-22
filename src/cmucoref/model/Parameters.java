package cmucoref.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import cmucoref.model.params.ParameterInitializer;
import cmucoref.util.Util;

public class Parameters implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private double[] parameters = null;
	private double[] nils = null;
	private double[] uni_parameters = null;
	private double nil;
	private double uni_nil;
	
	public Parameters(){}
	
	public Parameters(int featSize, int givenSize, ParameterInitializer initializer, boolean isMentionParam) {
		parameters = new double[featSize];
		nil = Double.NEGATIVE_INFINITY;
		nils = new double[givenSize];
		uni_parameters = new double[givenSize];
		uni_nil = Double.NEGATIVE_INFINITY;
		this.initialize(initializer, isMentionParam);
	}
	
	private void initialize(ParameterInitializer initializer, boolean isMentionParam) {
		if(isMentionParam) {
			initializer.initializeMentionParams(this);
		}
		else {
			initializer.initializeEventParams(this);
		}
	}
	
	public double getScore(FeatureVector fv) {
		if(fv.size() == 0) {
			throw new RuntimeException("empty fv: " + fv.size());
		}
		
		double score = 0.0;
		for(Object b : fv) {
			Feature f = (Feature)(b);
			if(f.gid == -1) {
				throw new RuntimeException("gid cannot be -1");
			}
			
			if(f.index == -1) {
				score += nils[f.gid];
			}
			else {
				score += parameters[f.index];
			}
		}
		return score;
	}
	
	public double getScoreWithNil(FeatureVector fv) {
		if(fv.size() == 0) {
			throw new RuntimeException("empty fv: " + fv.size());
		}
		
		double score = 0.0;
		for(Object b : fv) {
			Feature f = (Feature)(b);
			if(f.gid == -1) {
				if(f.index == -1) {
					score += Util.logsumexp(nil, uni_nil);
				}
				else if(f.index < 0) {
					int eid = -f.index - 2;
					score += Util.logsumexp(nil, uni_parameters[eid]);
				}
				else {
					throw new RuntimeException("f.index should be negative: " + f.index);
				}
			}
			else if(f.index == -1) {
				score += Util.logsumexp(nils[f.gid], uni_nil);
			}
			else if(f.index < 0) {
				int eid = -f.index - 2;
				score += Util.logsumexp(nils[f.gid], uni_parameters[eid]);
			}
			else {
				score += parameters[f.index];
			}
		}
		return score;
	}
	
	public void updateParam(int index, double val) {
		parameters[index] = val;
	}
	
	public void updateNil(int gid, double val) {
		nils[gid] = val;
	}
	
	public void updateNil(double nil) {
		this.nil = nil;
	}
	
	public void updateUniParam(int index, double val) {
		uni_parameters[index] = val;
	}
	
	public void updateUni_Nil(double uni_nil) {
		this.uni_nil = uni_nil;
	}
	
	public double paramAt(int index) {
		return parameters[index];
	}
	
	public double nilAt(int gid) {
		return nils[gid];
	}
	
	public double uniParamAt(int eid) {
		return uni_parameters[eid];
	}
	
	public double nil() {
		return nil;
	}
	
	public double uni_nil() {
		return uni_nil;
	}
	
	public int sizeOfFeat() {
		return parameters.length;
	}
	
	public int sizeOfNil() {
		return nils.length;
	}
	
	public int sizeOfUni() {
		return uni_parameters.length;
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(this.parameters);
		out.writeObject(this.nils);
		out.writeObject(this.uni_parameters);
		out.writeDouble(uni_nil);
		out.writeDouble(nil);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		parameters = (double[]) in.readObject();
		nils = (double[]) in.readObject();
		uni_parameters = (double[]) in.readObject();
		uni_nil = in.readDouble();
		nil = in.readDouble();
		
	}
}
