package com.epam.deltix.util.security;

import java.security.*;

public class ExitPreventingException extends SecurityException {
	private int		mStatus;
		
	public int		getStatus () {
		return (mStatus);
	}
		
	public ExitPreventingException (int status) {
		mStatus = status;
	}
}

