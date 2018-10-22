package com.randomnoun.common.exception;

/** An exception which is thrown by DAOs when an objectId is supplied that doesn't
 * have a record in the database.
 * 
 * <p>Would typically be caused by a user deleting an object, then hitting the back button on their browser
 * to an object editor page showing that object, and then hitting reload.
 * 
 * <p>I used to use IllegalArgumentExceptions for this, but now these are being caught and acted upon
 * in the Action classes, I thought it better to use a new exception type.
 * 
 * @author knoxg
 */
public class ObjectNotFoundException extends RuntimeException {

	/** generated serialVersionUID */
	private static final long serialVersionUID = -2560326697104634124L;

	public ObjectNotFoundException(String message) {
		super(message);
	}

	public ObjectNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
