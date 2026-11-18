package com.example.bank.exception;

public class ResourceAccessDeniedException extends RuntimeException {

	public ResourceAccessDeniedException(String message) {
		super(message);
	}
}
