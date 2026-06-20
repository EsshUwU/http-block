package com.httpblock.http;

import com.httpblock.HTTPBlock;
import com.httpblock.blockentity.HttpReceiverBlockEntity;
import com.httpblock.blockentity.HttpSenderBlockEntity;
import com.httpblock.config.HttpBlockConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class HttpPollManager {
	private static final Set<HttpReceiverBlockEntity> RECEIVERS = ConcurrentHashMap.newKeySet();
	private static final Set<HttpSenderBlockEntity> SENDERS = ConcurrentHashMap.newKeySet();
	private static int ticks;

	private HttpPollManager() {
	}

	public static void tick(MinecraftServer server) {
		ticks++;
		boolean poll = ticks >= HttpBlockConfig.getPollRate();
		if (poll) {
			ticks = 0;
		}

		RECEIVERS.removeIf(receiver -> receiver.isRemoved() || !(receiver.getLevel() instanceof ServerLevel));
		for (HttpReceiverBlockEntity receiver : RECEIVERS) {
			ServerLevel level = (ServerLevel) receiver.getLevel();
			receiver.serverTick(level);
			if (poll) {
				receiver.poll(level);
			}
		}
	}

	public static void registerReceiver(HttpReceiverBlockEntity receiver) {
		RECEIVERS.add(receiver);
	}

	public static void registerSender(HttpSenderBlockEntity sender) {
		SENDERS.add(sender);
	}

	public static int receiverCount() {
		RECEIVERS.removeIf(receiver -> receiver.isRemoved() || receiver.getLevel() == null);
		return RECEIVERS.size();
	}

	public static int senderCount() {
		SENDERS.removeIf(sender -> sender.isRemoved() || sender.getLevel() == null);
		return SENDERS.size();
	}
}
