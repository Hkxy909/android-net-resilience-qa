package com.hkxy.netqa;

public class PingResult {
	private String endpoint;
	private int statusCode;
	private long latencyMs;
	private int attemptsUsed;
	private String errorMessage;

	public PingResult(String endpoint, int statusCode, long latencyMs, int attemptsUsed, String errorMessage) {
		this.endpoint = endpoint;
		this.statusCode = statusCode;
		this.latencyMs = latencyMs;
		this.attemptsUsed = attemptsUsed;
		this.errorMessage = errorMessage;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public long getLatencyMs() {
		return latencyMs;
	}

	public int getAttemptsUsed() {
		return attemptsUsed;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public boolean isSuccess() {
		return statusCode >= 200 && statusCode < 300;
	}

	@Override
	public String toString() {
		if (errorMessage != null && !errorMessage.isEmpty()) {
			return endpoint + " -> FAILED (" + errorMessage + ") after " + attemptsUsed + " attempt(s)";
		}
		return endpoint + " -> HTTP " + statusCode + " (" + latencyMs + "ms, attempts: " + attemptsUsed + ")";
	}
}
