package com.httpblock.blockentity;

import com.httpblock.HTTPBlock;
import com.httpblock.http.HttpClientService;
import com.httpblock.http.HttpPollManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.state.BlockState;

public class HttpSenderBlockEntity extends BlockEntity {
	private String url = "";
	private String payload = "{\"message\":\"hello\"}";

	public HttpSenderBlockEntity(BlockPos pos, BlockState state) {
		super(HTTPBlock.HTTP_SENDER_ENTITY, pos, state);
		HttpPollManager.registerSender(this);
	}

	public void send() {
		if (!url.isBlank()) {
			HttpClientService.post(url, payload);
		}
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = trim(url);
		setChanged();
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload == null ? "" : payload;
		setChanged();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		url = input.getStringOr("url", "");
		payload = input.getStringOr("payload", "{\"message\":\"hello\"}");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		output.putString("url", url);
		output.putString("payload", payload);
		super.saveAdditional(output);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void setChanged() {
		super.setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, 3);
		}
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}
}
