package cmucoref.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Pair<T1, T2> implements Comparable<Pair<T1,T2>>, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public T1 first;
	public T2 second;
	
	public Pair(){
		super();
		first = null;
		second = null;
	}
	
	public Pair(T1 first, T2 second){
		super();
		this.first = first;
		this.second = second;
	}
	
	public Pair<T1, T2> clone(){
		return new Pair<T1, T2>(first, second);
	}
	
	@Override
	public int hashCode(){
		int hashFirst = first != null ? first.hashCode() : 0;
		int hashSecond = second != null ? second.hashCode() : 0;
		return (hashFirst + hashSecond) * hashSecond + hashFirst;
	}
	
	 @Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Pair<?, ?>)) {
			return false;
		}
		@SuppressWarnings("unchecked")
		Pair<T1, T2> otherPair = (Pair<T1, T2>) obj;
		
		return ((this.first == otherPair.first 
				|| (this.first != null && otherPair.first != null && this.first.equals(otherPair.first))) 
				&& 
				(this.second == otherPair.second 
				|| (this.second != null && otherPair.second != null && this.second.equals(otherPair.second))));
	}
	
	public String toString(){
		return "(" + first + ", " + second + ")";
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException{
		out.writeObject(first);
		out.writeObject(second);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
		first = (T1) in.readObject();
		second = (T2) in.readObject();
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(Pair<T1, T2> another) {
		if (first instanceof Comparable) {
			int comp = ((Comparable<T1>) first).compareTo(another.first);
			if (comp != 0) {
				return comp;
			}
		}

		if (second instanceof Comparable) {
			return ((Comparable<T2>) second).compareTo(another.second);
		}

		if ((!(first instanceof Comparable)) && (!(second instanceof Comparable))) {
			throw new AssertionError("Neither element of pair comparable");
		}

		return 0;
	}
}