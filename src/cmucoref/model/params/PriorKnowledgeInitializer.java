package cmucoref.model.params;

import cmucoref.model.Parameters;

public class PriorKnowledgeInitializer extends ParameterInitializer{

	@Override
	public void initializeMentionParams(Parameters parameters) {
//		Arrays.fill(parameters, 1);
//		
//		String[] mentionTypes = {"NOMINAL", "PRONOMINAL", "PROPER"};
//		String[] NounTypes = {"NOMINAL", "PROPER"};
//		Pair<Integer, Integer> index = null;
//		
//		//init distance features
//		for(int i = 0; i < 10; ++i){
//			for(String mentionType : mentionTypes){
//				index = model.getMentionFeatureIndex("DISTOFSENT=" + i, "ANAPHTYPE=" + mentionType + ", " + "ANTECTYPE=PRONOMINAL");
//				parameters[index.first] = 10 / (i + 1);
//				
//				index = model.getMentionFeatureIndex("DISTOFSENT=" + i, "ANAPHTYPE=PRONOMINAL" + ", " + "ANTECTYPE=" + mentionType);
//				parameters[index.first] = 10 / (i + 1);
//			}
//		}
//		
//		//init attribute match features
//		//nominal & proper
//		index = model.getMentionFeatureIndex("GENMAT=true, NUMMAT=true, ANIMAT=true", "ANAPHTYPE=NOMINAL, ANTECTYPE=NOMINAL");
//		parameters[index.first] = 63;
//		index = model.getMentionFeatureIndex("GENMAT=true, NUMMAT=true, ANIMAT=true", "ANAPHTYPE=PROPER, ANTECTYPE=NOMINAL");
//		parameters[index.first] = 63;
//		index = model.getMentionFeatureIndex("GENMAT=true, NUMMAT=true, ANIMAT=true", "ANAPHTYPE=NOMINAL, ANTECTYPE=PROPER");
//		parameters[index.first] = 63;
//		//proper & proper
//		index = model.getMentionFeatureIndex("GENMAT=true, NUMMAT=true, ANIMAT=true, NERMAT=true", "ANAPHTYPE=PROPER, ANTECTYPE=PROPER");
//		parameters[index.first] = 112;
//		index = model.getMentionFeatureIndex("GENMAT=true, NUMMAT=true, ANIMAT=true, NERMAT=false", "ANAPHTYPE=PROPER, ANTECTYPE=PROPER");
//		parameters[index.first] = 14;
//		//pronominal
//		for(int i = 0; i < 10; ++i){
//			for(String type : NounTypes){
//				index = model.getMentionFeatureIndex("GENMAT=true, NUMMAT=true", 
//						"DISTOFSENT=" + i + ", " +  "ANAPHTYPE=" + type + ", " + "ANTECTYPE=PRONOMINAL");
//				parameters[index.first] = 10 - i;
//				
//				index = model.getMentionFeatureIndex("GENMAT=true, NUMMAT=true", 
//						"DISTOFSENT=" + i + ", " +  "ANAPHTYPE=PRONOMINAL" + ", " + "ANTECTYPE=" + type);
//				parameters[index.first] = 10 - i;
//			}
//			
//			index = model.getMentionFeatureIndex("GENMAT=true, NUMMAT=true, PERMAT=true", 
//						"DISTOFSENT=" + i + ", " +  "ANAPHTYPE=PRONOMINAL, ANTECTYPE=PRONOMINAL");
//			parameters[index.first] = 20 - 2 * i;
//		}
//		
//		//init head match features
//		for(String anaphType : NounTypes){
//			for(String antecType : NounTypes){				
//				index = model.getMentionFeatureIndex("SPMAT=true, HDMAT=true", 
//						"ATTRMAT=true" + ", " + "ANAPHTYPE=" + anaphType + ", " + "ANTECTYPE=" + antecType);
//				parameters[index.first] = 30;
//				
//				index = model.getMentionFeatureIndex("SPMAT=true, HDMAT=true", 
//						"ATTRMAT=true" + ", " + "ANAPHTYPE=" + anaphType + ", " + "ANTECTYPE=" + antecType);
//				parameters[index.first] = 15;
//			}
//		}
//		
//		//init new cluster features
//		for(String type : NounTypes){
//			index = model.getMentionFeatureIndex("TYPE=" + type + ", " + "HDMATPOS=10, SPMAT=false", "NEWCLUSTER");
//			parameters[index.first] = 40;
//			index = model.getMentionFeatureIndex("TYPE=" + type + ", " + "HDMATPOS=0, SPMAT=true", "NEWCLUSTER");
//			parameters[index.first] = 0.01;
//		}
//		index = model.getMentionFeatureIndex("TYPE=PRONOMINAL, LOCATTRMAT=false", "NEWCLUSTER");
//		parameters[index.first] = 2;
//				
//		double[] c = new double[model.givenSizeofMention()];
//		for(int j = 0; j < parameters.length; ++j){
//			int gid = model.getMentionGidFromIndex(j);
//			c[gid] += parameters[j];
//		}
//		
//		for(int j = 0; j < parameters.length; ++j){
//			int gid = model.getMentionGidFromIndex(j);
//			parameters[j] = parameters[j] / c[gid];
//		}
	}

	@Override
	public void initializeEventParams(Parameters parameters) {
		// TODO Auto-generated method stub
		
	}

}
