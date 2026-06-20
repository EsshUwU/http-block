package com.httpblock.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.httpblock.HTTPBlock;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HttpClientService {
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, task -> {
		Thread thread = new Thread(task, "HTTP Block Worker");
		thread.setDaemon(true);
		return thread;
	});
	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.executor(EXECUTOR)
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	private HttpClientService() {
	}

	public static void post(String url, String payload) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofSeconds(10))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(payload == null ? "" : payload))
					.build();
			CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
					.exceptionally(error -> {
						HTTPBlock.LOGGER.warn("HTTP sender request failed: {}", error.getMessage());
						return null;
					});
		} catch (IllegalArgumentException e) {
			HTTPBlock.LOGGER.warn("Invalid HTTP sender URL '{}'", url);
		}
	}

	public static CompletableFuture<HttpEvent> getEvent(String url) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();

			return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
					.thenApply(response -> {
						if (response.statusCode() < 200 || response.statusCode() >= 300) {
							return null;
						}
						JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
						String hash = object.has("hash") && !object.get("hash").isJsonNull() ? object.get("hash").getAsString() : "";
						String message = object.has("message") && !object.get("message").isJsonNull() ? object.get("message").getAsString() : "";
						return new HttpEvent(hash, message);
					})
					.exceptionally(error -> {
						HTTPBlock.LOGGER.debug("HTTP receiver poll failed: {}", error.getMessage());
						return null;
					});
		} catch (IllegalArgumentException e) {
			HTTPBlock.LOGGER.warn("Invalid HTTP receiver URL '{}'", url);
			return CompletableFuture.completedFuture(null);
		}
	}

	public static void shutdown() {
		EXECUTOR.shutdownNow();
	}
}
