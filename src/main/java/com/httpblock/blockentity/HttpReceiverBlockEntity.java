package com.httpblock.blockentity;

import com.httpblock.HTTPBlock;
import com.httpblock.block.HttpReceiverBlock;
import com.httpblock.config.HttpBlockConfig;
import com.httpblock.http.HttpClientService;
import com.httpblock.http.HttpPollManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.concurrent.atomic.AtomicBoolean;

public class HttpReceiverBlockEntity extends BlockEntity {
	private String url = "";
	private String lastMessage = "";
	private String lastHash = "";
	private int pulseTicks = 0;
	private final AtomicBoolean requestInFlight = new AtomicBoolean(false);

	public HttpReceiverBlockEntity(BlockPos pos, BlockState state) {
		super(HTTPBlock.HTTP_RECEIVER_ENTITY, pos, state);
	}

	public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, HttpReceiverBlockEntity receiver) {
		if (level instanceof ServerLevel) {
			HttpPollManager.registerReceiver(receiver);
		}
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url == null ? "" : url.trim();
		setChanged();
	}

	public String getLastMessage() {
		return lastMessage;
	}

	public void poll(ServerLevel serverLevel) {
		if (url.isBlank() || !requestInFlight.compareAndSet(false, true)) {
			return;
		}

		HttpClientService.getEvent(url).whenComplete((event, error) -> serverLevel.getServer().execute(() -> {
			requestInFlight.set(false);
			if (isRemoved() || level != serverLevel || event == null || event.hash().isBlank()) {
				return;
			}

			if (!event.hash().equals(lastHash)) {
				lastHash = event.hash();
				lastMessage = event.message();
				emitPulse();
				setChanged();
			}
		}));
	}

	public void serverTick(ServerLevel serverLevel) {
		if (pulseTicks > 0) {
			pulseTicks--;
			if (pulseTicks == 0) {
				setPowered(serverLevel, false);
			}
		}
	}

	private void emitPulse() {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		pulseTicks = Math.max(1, HttpBlockConfig.getPulseLength());
		setPowered(serverLevel, true);
		HttpReceiverBlock.playPulseSound(serverLevel, worldPosition);
	}

	private void setPowered(ServerLevel serverLevel, boolean powered) {
		BlockState state = getBlockState();
		if (state.getBlock() instanceof HttpReceiverBlock && state.getValue(HttpReceiverBlock.POWERED) != powered) {
			serverLevel.setBlock(worldPosition, state.setValue(HttpReceiverBlock.POWERED, powered), Block.UPDATE_ALL);
			serverLevel.updateNeighborsAt(worldPosition, state.getBlock());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		url = input.getStringOr("url", "");
		lastMessage = input.getStringOr("lastMessage", "");
		lastHash = input.getStringOr("lastHash", "");
		pulseTicks = input.getIntOr("pulseTicks", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		output.putString("url", url);
		output.putString("lastMessage", lastMessage);
		output.putString("lastHash", lastHash);
		output.putInt("pulseTicks", pulseTicks);
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
}
