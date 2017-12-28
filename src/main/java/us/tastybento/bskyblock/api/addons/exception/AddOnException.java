package us.tastybento.bskyblock.api.addons.exception;

public abstract class AddOnException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 4203162022348693854L;

	public AddOnException(String errorMessage){
		super("AddOnException : "+errorMessage);
	}
	
}
