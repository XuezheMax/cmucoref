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
		
		if(x == Double.POSITIVE_INFINITY && y == Double.POSITIVE_INFINITY) {
			return Double.POSITIVE_INFINITY;
		}
		
		double vmax = Math.max(x, y);
		double vmin = Math.min(x, y);
		
		return (vmax > vmin + MINUS_LOG_EPSILON) ? vmax : vmax + Math.log(1.0 + Math.exp(vmin - vmax));
		
//		if(Double.isNaN(ret)) {
//			throw new RuntimeException("NaN: x=" + x + "y=" + y);
//		}
//		return ret;
	}
}
