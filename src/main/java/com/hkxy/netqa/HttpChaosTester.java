package com.hkxy.netqa;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;

public class HttpChaosTester {

	private int timeoutMs;

	public HttpChaosTester(int timeoutMs) {
		if (timeoutMs <= 0) {
			throw new IllegalArgumentException("Timeout must be positive: " + timeoutMs);
		}
		this.timeoutMs = timeoutMs;
	}

	// HTTP 429 (Too Many Requests) and 5xx server errors should be retried during unstable connection tests
	public boolean isRetryableStatus(int statusCode) {
		return statusCode == 408 || statusCode == 429 || (statusCode >= 500 && statusCode <= 599);
	}

	// Validates if JSON check-in payload from an Android agent has required MDM fields
	public boolean isValidSyncPayload(String jsonPayload) {
		if (jsonPayload == null) {
			return false;
		}
		String trimmed = jsonPayload.trim();
		if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
			return false;
		}
		return trimmed.contains("\"deviceId\"") && trimmed.contains("\"batteryLevel\"")
				&& trimmed.contains("\"osVersion\"");
	}

	// Calculates packet loss percentage from a batch of test runs
	public double calculateDropRatePercent(ArrayList<PingResult> results) {
		if (results == null || results.isEmpty()) {
			return 0.0;
		}
		int failedCount = 0;
		for (int i = 0; i < results.size(); i++) {
			if (!results.get(i).isSuccess()) {
				failedCount++;
			}
		}
		return ((double) failedCount / results.size()) * 100.0;
	}

	public PingResult checkEndpointWithRetry(String url, int maxAttempts) {
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("maxAttempts must be at least 1");
		}

		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(timeoutMs))
				.build();

		int attempt = 0;
		String lastError = "";
		int lastStatus = -1;
		long elapsed = 0;

		while (attempt < maxAttempts) {
			attempt++;
			long start = System.currentTimeMillis();

			try {
				HttpRequest req = HttpRequest.newBuilder()
						.uri(URI.create(url))
						.timeout(Duration.ofMillis(timeoutMs))
						.GET()
						.build();

				HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
				elapsed = System.currentTimeMillis() - start;
				lastStatus = resp.statusCode();

				if (!isRetryableStatus(lastStatus)) {
					return new PingResult(url, lastStatus, elapsed, attempt, "");
				}
				lastError = "Retryable HTTP " + lastStatus;
			} catch (Exception e) {
				elapsed = System.currentTimeMillis() - start;
				lastStatus = -1;
				lastError = e.getClass().getSimpleName();
			}
		}

		return new PingResult(url, lastStatus, elapsed, attempt, lastError);
	}
}
