package cmucoref.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Parameters implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private double[] parameters = null;
	
	public Parameters(){}
	
	public Parameters(int size){
		parameters = new double[size];
	}
	
	public Parameters(double[] parameters){
		this.parameters = parameters;
	}
	
	public double getScore(FeatureVector fv){
		return fv.getScore(parameters);
	}
	
	public void update(FeatureVector fv, double val){
		fv.update(parameters, val);
	}
	
	public double paramAt(int index){
		return parameters[index];
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException{
		out.writeObject(this.parameters);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
		parameters = (double[]) in.readObject();
	}
}
