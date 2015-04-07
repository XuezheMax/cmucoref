package cmucoref.exception;

public class CreatingInstanceException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public CreatingInstanceException(){}
	
	public CreatingInstanceException(String message){
		super(message);
	}
	
	public CreatingInstanceException(Throwable cause){
		super(cause);
	}
}
