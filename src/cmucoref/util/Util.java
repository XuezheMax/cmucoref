package cmucoref.util;

public class Util {
	private static final int MINUS_LOG_EPSILON = 50;

	// log(exp(x) + exp(y));
	//  this can be used recursivly
	//e.g., log(exp(log(exp(x) + exp(y))) + exp(z)) =
	//log(exp (x) + exp(y) + exp(z))
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
	}
	
	// log(exp(x) - exp(y)), x >= y
	public static double logsubsexp(double x, double y) {
		if(Double.compare(x, y) <= 0) {
			return Double.NEGATIVE_INFINITY;
		}
		
		return (x > y + MINUS_LOG_EPSILON) ? x : x + Math.log(1.0 - Math.exp(y - x));
	}
}
