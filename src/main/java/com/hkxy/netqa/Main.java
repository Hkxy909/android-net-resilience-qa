package com.hkxy.netqa;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Main {

	private static int flakyCounter = 0;

	public static void main(String[] args) throws IOException {
		// Spin up a local test server on a random free port so the QA suite runs offline without external APIs
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		int port = server.getAddress().getPort();

		server.createContext("/checkin/ok", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] body = "{\"status\":\"SYNCED\"}".getBytes();
				exchange.sendResponseHeaders(200, body.length);
				OutputStream os = exchange.getResponseBody();
				os.write(body);
				os.close();
			}
		});

		// Simulates a dropped/overloaded server on the first 2 attempts, then recovers with HTTP 200 on attempt 3
		server.createContext("/checkin/flaky", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				flakyCounter++;
				if (flakyCounter < 3) {
					byte[] body = "Service Unavailable".getBytes();
					exchange.sendResponseHeaders(503, body.length);
					OutputStream os = exchange.getResponseBody();
					os.write(body);
					os.close();
				} else {
					byte[] body = "{\"status\":\"RECOVERED\"}".getBytes();
					exchange.sendResponseHeaders(200, body.length);
					OutputStream os = exchange.getResponseBody();
					os.write(body);
					os.close();
				}
			}
		});

		server.createContext("/checkin/unauthorized", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] body = "Invalid device token".getBytes();
				exchange.sendResponseHeaders(401, body.length);
				OutputStream os = exchange.getResponseBody();
				os.write(body);
				os.close();
			}
		});

		server.start();

		System.out.println("--- Android Network Resilience & REST QA Runner ---");
		AdbNetworkController adbNet = new AdbNetworkController();
		if (adbNet.isDeviceConnected()) {
			System.out.println("ADB Device detected: Running live Wi-Fi state check...");
			adbNet.setWifiState(true);
		} else {
			System.out.println("No ADB device connected. Running local mock MDM sync server on port " + port);
		}

		HttpChaosTester tester = new HttpChaosTester(1500);
		String baseUrl = "http://127.0.0.1:" + port;

		ArrayList<PingResult> results = new ArrayList<>();
		results.add(tester.checkEndpointWithRetry(baseUrl + "/checkin/ok", 3));
		results.add(tester.checkEndpointWithRetry(baseUrl + "/checkin/flaky", 3));
		results.add(tester.checkEndpointWithRetry(baseUrl + "/checkin/unauthorized", 3));

		for (int i = 0; i < results.size(); i++) {
			System.out.println("Test " + (i + 1) + ": " + results.get(i).toString());
		}

		double dropRate = tester.calculateDropRatePercent(results);
		System.out.println("Failed / Rejected Rate: " + String.format("%.1f", dropRate) + "%");

		server.stop(0);
	}
}
