package com.httpblock.command;

import com.httpblock.HTTPBlock;
import com.httpblock.config.HttpBlockConfig;
import com.httpblock.http.HttpPollManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class HttpBlockCommands {
	private HttpBlockCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(Commands.literal("http")
				.then(Commands.literal("pollrate")
						.then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
								.executes(context -> {
									int ticks = IntegerArgumentType.getInteger(context, "ticks");
									HttpBlockConfig.setPollRate(ticks);
									context.getSource().sendSuccess(() -> Component.literal("HTTP Block poll rate set to " + ticks + " ticks"), true);
									return 1;
								})))
				.then(Commands.literal("pulselength")
						.then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
								.executes(context -> {
									int ticks = IntegerArgumentType.getInteger(context, "ticks");
									HttpBlockConfig.setPulseLength(ticks);
									context.getSource().sendSuccess(() -> Component.literal("HTTP Block pulse length set to " + ticks + " ticks"), true);
									return 1;
								})))
				.then(Commands.literal("reload")
						.executes(context -> {
							HttpBlockConfig.load();
							context.getSource().sendSuccess(() -> Component.literal("HTTP Block config reloaded"), true);
							return 1;
						}))
				.then(Commands.literal("status")
						.executes(context -> {
							Counts counts = count(context.getSource().getServer());
							context.getSource().sendSuccess(() -> Component.literal("HTTP Blocks\n\nPoll Rate: " + HttpBlockConfig.getPollRate()
									+ "\nPulse Length: " + HttpBlockConfig.getPulseLength()
									+ "\n\nSenders: " + counts.senders()
									+ "\nReceivers: " + counts.receivers()), false);
							return 1;
						}))));
	}

	private static Counts count(net.minecraft.server.MinecraftServer server) {
		return new Counts(HttpPollManager.senderCount(), HttpPollManager.receiverCount());
	}

	private record Counts(int senders, int receivers) {
	}
}
