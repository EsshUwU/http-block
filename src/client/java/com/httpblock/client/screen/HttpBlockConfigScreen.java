package com.httpblock.client.screen;

import com.httpblock.net.HttpBlockPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HttpBlockConfigScreen extends Screen {
	private final HttpBlockPayloads.OpenConfig config;
	private EditBox urlBox;
	private EditBox payloadBox;
	private boolean saved;

	public HttpBlockConfigScreen(HttpBlockPayloads.OpenConfig config) {
		super(Component.literal(config.sender() ? "HTTP Sender" : "HTTP Receiver"));
		this.config = config;
	}

	@Override
	protected void init() {
		int panelWidth = Math.min(360, width - 32);
		int left = (width - panelWidth) / 2;
		int top = Math.max(24, (height - 180) / 2);

		urlBox = new EditBox(font, left, top + 42, panelWidth, 20, Component.literal("URL"));
		urlBox.setMaxLength(4096);
		urlBox.setValue(config.url());
		addRenderableWidget(urlBox);

		if (config.sender()) {
			payloadBox = new EditBox(font, left, top + 92, panelWidth, 20, Component.literal("Payload"));
			payloadBox.setMaxLength(32767);
			payloadBox.setValue(config.payload());
			addRenderableWidget(payloadBox);
		}

		addRenderableWidget(Button.builder(Component.literal("Save"), button -> {
			save();
			onClose();
		}).bounds(left, top + 142, 80, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(left + 88, top + 142, 80, 20).build());
		setInitialFocus(urlBox);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractMenuBackground(graphics);
		int panelWidth = Math.min(360, width - 32);
		int left = (width - panelWidth) / 2;
		int top = Math.max(24, (height - 180) / 2);

		graphics.fill(left - 12, top - 12, left + panelWidth + 12, top + 176, 0xE0101010);
		graphics.outline(left - 12, top - 12, panelWidth + 24, 188, 0xFF555555);
		graphics.text(font, title, left, top, 0xFFFFFFFF);
		graphics.text(font, Component.literal("URL"), left, top + 30, 0xFFCCCCCC);

		if (config.sender()) {
			graphics.text(font, Component.literal("Payload"), left, top + 80, 0xFFCCCCCC);
		} else {
			graphics.text(font, Component.literal("Last Message"), left, top + 80, 0xFFCCCCCC);
			String message = config.lastMessage().isBlank() ? "(none)" : config.lastMessage();
			graphics.text(font, message, left, top + 96, 0xFFFFFFFF);
		}

		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void removed() {
		if (!saved && urlBox != null) {
			save();
		}
		super.removed();
	}

	private void save() {
		saved = true;
		String payload = payloadBox == null ? "" : payloadBox.getValue();
		ClientPlayNetworking.send(new HttpBlockPayloads.SaveConfig(config.pos(), config.sender(), urlBox.getValue(), payload));
	}
}
