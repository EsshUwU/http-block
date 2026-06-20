package com.httpblock.config;

import com.httpblock.HTTPBlock;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HttpBlockConfig {
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("http-block.properties");
	private static int pollRate = 20;
	private static int pulseLength = 20;

	private HttpBlockConfig() {
	}

	public static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			save();
			return;
		}

		try {
			for (String line : Files.readAllLines(CONFIG_PATH)) {
				String[] parts = line.split("=", 2);
				if (parts.length != 2) {
					continue;
				}
				if (parts[0].trim().equals("pollRate")) {
					pollRate = clamp(parse(parts[1], pollRate));
				} else if (parts[0].trim().equals("pulseLength")) {
					pulseLength = clamp(parse(parts[1], pulseLength));
				}
			}
		} catch (IOException e) {
			HTTPBlock.LOGGER.warn("Failed to load HTTP Block config", e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, "pollRate=" + pollRate + System.lineSeparator() + "pulseLength=" + pulseLength + System.lineSeparator());
		} catch (IOException e) {
			HTTPBlock.LOGGER.warn("Failed to save HTTP Block config", e);
		}
	}

	public static int getPollRate() {
		return pollRate;
	}

	public static void setPollRate(int ticks) {
		pollRate = clamp(ticks);
		save();
	}

	public static int getPulseLength() {
		return pulseLength;
	}

	public static void setPulseLength(int ticks) {
		pulseLength = clamp(ticks);
		save();
	}

	private static int parse(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ignored) {
			return fallback;
		}
	}

	private static int clamp(int value) {
		return Math.max(1, Math.min(1200, value));
	}
}
