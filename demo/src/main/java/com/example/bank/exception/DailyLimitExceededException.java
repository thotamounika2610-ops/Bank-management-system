package com.example.bank.exception;

public class DailyLimitExceededException extends RuntimeException {

	public DailyLimitExceededException(String message) {
		super(message);
	}
}
