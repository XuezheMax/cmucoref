package cmucoref.util;

public class Util {

	// log(exp(x) + exp(y));
	//  this can be used recursivly
	//e.g., log(exp(log(exp(x) + exp(y))) + exp(z)) =
	//log(exp (x) + exp(y) + exp(z))
	
	private static final int MINUS_LOG_EPSILON = 50;

	public static double logsumexp(double x, double y){
		if(x == Double.NEGATIVE_INFINITY && y == Double.NEGATIVE_INFINITY) {
			return Double.NEGATIVE_INFINITY;
		}
		
		double vmax = x > y ? x : y;
		double vmin = x > y ? y : x;
		
		if (vmax > vmin + MINUS_LOG_EPSILON) {
			return vmax;
		}
		else {
			return vmax + Math.log(1.0 + Math.exp(vmin - vmax));
		}
	}
}
