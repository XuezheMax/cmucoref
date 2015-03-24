package cmucoref.exception;

public class OptionException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public OptionException(){}
	
	public OptionException(String message){
		super(message);
	}
	
	public OptionException(Throwable cause){
		super(cause);
	}

}
