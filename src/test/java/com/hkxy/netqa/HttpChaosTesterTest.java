package com.hkxy.netqa;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HttpChaosTesterTest {

	private HttpServer mockServer;
	private int port;
	private int hitCount;
	private HttpChaosTester tester;

	@BeforeEach
	public void setUp() throws IOException {
		hitCount = 0;
		tester = new HttpChaosTester(500);
		mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		port = mockServer.getAddress().getPort();

		mockServer.createContext("/flaky", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				hitCount++;
				if (hitCount < 3) {
					exchange.sendResponseHeaders(503, -1);
				} else {
					byte[] ok = "OK".getBytes();
					exchange.sendResponseHeaders(200, ok.length);
					OutputStream os = exchange.getResponseBody();
					os.write(ok);
					os.close();
				}
			}
		});

		mockServer.createContext("/auth-fail", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				hitCount++;
				exchange.sendResponseHeaders(401, -1);
			}
		});

		mockServer.start();
	}

	@AfterEach
	public void tearDown() {
		if (mockServer != null) {
			mockServer.stop(0);
		}
	}

	@Test
	public void testRecoversFrom503OnThirdAttempt() {
		String url = "http://127.0.0.1:" + port + "/flaky";
		PingResult res = tester.checkEndpointWithRetry(url, 3);

		assertTrue(res.isSuccess());
		assertEquals(200, res.getStatusCode());
		assertEquals(3, res.getAttemptsUsed());
	}

	@Test
	public void testFailsIfMaxRetriesTooLowForFlakyEndpoint() {
		String url = "http://127.0.0.1:" + port + "/flaky";
		PingResult res = tester.checkEndpointWithRetry(url, 2);

		assertFalse(res.isSuccess());
		assertEquals(503, res.getStatusCode());
		assertEquals(2, res.getAttemptsUsed());
	}

	@Test
	public void testDoesNotRetryOn401Unauthorized() {
		String url = "http://127.0.0.1:" + port + "/auth-fail";
		PingResult res = tester.checkEndpointWithRetry(url, 4);

		assertFalse(res.isSuccess());
		assertEquals(401, res.getStatusCode());
		// Should stop immediately on 401 instead of wasting retries
		assertEquals(1, res.getAttemptsUsed());
	}

	@Test
	public void testValidAndMalformedSyncPayloads() {
		String validJson = "{\"deviceId\":\"A102\", \"batteryLevel\":84, \"osVersion\":\"14\"}";
		String missingField = "{\"deviceId\":\"A102\", \"batteryLevel\":84}";
		String brokenJson = "{\"deviceId\":\"A102\"";

		assertTrue(tester.isValidSyncPayload(validJson));
		assertFalse(tester.isValidSyncPayload(missingField));
		assertFalse(tester.isValidSyncPayload(brokenJson));
		assertFalse(tester.isValidSyncPayload(null));
	}

	@Test
	public void testCalculateDropRateBoundaryCases() {
		ArrayList<PingResult> list = new ArrayList<>();
		assertEquals(0.0, tester.calculateDropRatePercent(list));

		list.add(new PingResult("http://test/1", 200, 15, 1, ""));
		list.add(new PingResult("http://test/2", 503, 120, 3, "Retryable HTTP 503"));

		assertEquals(50.0, tester.calculateDropRatePercent(list), 0.01);
	}

	@Test
	public void testInvalidTimeoutThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> {
			new HttpChaosTester(0);
		});
	}
}
