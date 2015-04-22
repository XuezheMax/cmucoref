package cmucoref.model.params;

import java.util.Arrays;

import cmucoref.util.Pair;

public class PriorKnowledgeInitializer extends ParameterInitializer{

	@Override
	public void initializeParams(double[] parameters) {
		Arrays.fill(parameters, 1);
		
		String[] mentionTypes = {"NOMINAL", "PRONOMINAL", "PROPER"};
		String[] NounTypes = {"NOMINAL", "PROPER"};
		Pair<Integer, Integer> index = null;
		
		//init new cluster features
		for(String type : NounTypes){
			index = model.getFeatureIndex("TYPE=" + type, "NEWCLUSTER");
			parameters[index.first] = 4;
		}
		
		//init distance features
		for(int i = 0; i < 10; ++i){
			for(String anaphType : mentionTypes){
				for(String antecType : mentionTypes){
					index = model.getFeatureIndex("DISTOFSENT=" + i, 
							"ANAPHTYPE=" + anaphType + ", " + "ANTECTYPE=" + antecType);
					parameters[index.first] = 10 / (i + 1);
				}
			}
		}
		
		//init gender match and number match features
		for(int i = 0; i < 10; ++i){
			for(String anaphType : mentionTypes){
				for(String antecType : mentionTypes){
					index = model.getFeatureIndex("GENMAT=true", 
							"GENMAT(" + "DISTOFSENT=" + i + ", "
							+ "ANAPHTYPE=" + anaphType + ", " + "ANTECTYPE=" + antecType + ")");
					parameters[index.first] = 9;
					
					index = model.getFeatureIndex("NUMMAT=true", 
							"NUMMAT(" + "DISTOFSENT=" + i + ", "
							+ "ANAPHTYPE=" + anaphType + ", " + "ANTECTYPE=" + antecType + ")");
					parameters[index.first] = 9;
				}
			}
		}
		
		//init animacy match features
		for(int i = 0; i < 10; ++i){
			for(String anaphType : NounTypes){
				for(String antecType : NounTypes){
					index = model.getFeatureIndex("ANIMAT=true", 
							"GENMAT1=true, " + "DISTOFSENT=" + i + ", "
							+ "ANAPHTYPE=" + anaphType + ", " + "ANTECTYPE=" + antecType);
					parameters[index.first] = 9;
				}
			}
		}
		
		//init NER match features
		for(int i = 0; i < 10; ++i){
			index = model.getFeatureIndex("NERMAT=true", 
					"GENMAT2=true, " + "DISTOFSENT=" + i + ", "
					+ "ANAPHTYPE=PROPER, ANTECTYPE=PROPER");
			parameters[index.first] = 9;
		}
		
		//init person match features
		for(int i = 0; i < 10; ++i){
			index = model.getFeatureIndex("PERMAT=true", 
					"GENMAT=true, NUMMAT=true, " + "DISTOFSENT=" + i + ", "
					+ "ANAPHTYPE=PRONOMINAL, ANTECTYPE=PRONOMINAL");
			parameters[index.first] += 9 - i;
		}
		
		double[] c = new double[model.givenSize()];
		for(int j = 0; j < parameters.length; ++j){
			int gid = model.getGidFromIndex(j);
			c[gid] += parameters[j];
		}
		
		for(int j = 0; j < parameters.length; ++j){
			int gid = model.getGidFromIndex(j);
			parameters[j] = parameters[j] / c[gid];
		}
	}

}
