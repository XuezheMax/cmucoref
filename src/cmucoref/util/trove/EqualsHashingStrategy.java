package cmucoref.util.trove;

import gnu.trove.strategy.HashingStrategy;

public class EqualsHashingStrategy<T> implements HashingStrategy<T>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int computeHashCode(T t) {
		return t.hashCode();
	}

	@Override
	public boolean equals(T t1, T t2) {
		return t1.equals(t2);
	}
	
}
