package cmucoref.exception;

public class MentionException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MentionException(){}
	
	public MentionException(String message){
		super(message);
	}
	
	public MentionException(Throwable cause){
		super(cause);
	}

}
