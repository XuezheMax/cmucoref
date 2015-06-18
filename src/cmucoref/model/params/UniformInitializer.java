package cmucoref.model.params;

public class UniformInitializer extends ParameterInitializer{

	public UniformInitializer(){}
	
	@Override
	public void initializeParams(double[] parameters) {
		double[] c = new double[model.givenSize()];
		for(int j = 0; j < parameters.length; ++j){
			int gid = model.getGidFromIndex(j);
			c[gid] += 1.0;
		}
		
		for(int j = 0; j < parameters.length; ++j){
			int gid = model.getGidFromIndex(j);
			parameters[j] = Math.log(1.0 / c[gid]);
		}
	}

}
