package com.httpblock.client;

import com.httpblock.client.screen.HttpBlockConfigScreen;
import com.httpblock.net.HttpBlockPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class HTTPBlockClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(HttpBlockPayloads.OpenConfig.TYPE, (payload, context) ->
				Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new HttpBlockConfigScreen(payload))));
	}
}
