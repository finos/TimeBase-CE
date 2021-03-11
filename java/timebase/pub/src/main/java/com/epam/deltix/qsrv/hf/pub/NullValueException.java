package com.epam.deltix.qsrv.hf.pub;

/**
 *  Thrown by variable access methods when the value is null.
 */
public class NullValueException extends RuntimeException {	
	public static final NullValueException INSTANCE = 
        new NullValueException ("NULL");

	private NullValueException (String msg) {
		super(msg);
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return null;
	}		
}
