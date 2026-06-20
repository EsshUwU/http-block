package com.httpblock.net;

import com.httpblock.HTTPBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class HttpBlockPayloads {
	private static final StreamCodec<RegistryFriendlyByteBuf, String> STRING = ByteBufCodecs.stringUtf8(32767).cast();

	private HttpBlockPayloads() {
	}

	public record OpenConfig(BlockPos pos, boolean sender, String url, String payload, String lastMessage) implements CustomPacketPayload {
		public static final Identifier ID = HTTPBlock.id("open_config");
		public static final Type<OpenConfig> TYPE = new Type<>(ID);
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfig> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC.cast(), OpenConfig::pos,
				ByteBufCodecs.BOOL.cast(), OpenConfig::sender,
				STRING, OpenConfig::url,
				STRING, OpenConfig::payload,
				STRING, OpenConfig::lastMessage,
				OpenConfig::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record SaveConfig(BlockPos pos, boolean sender, String url, String payload) implements CustomPacketPayload {
		public static final Identifier ID = HTTPBlock.id("save_config");
		public static final Type<SaveConfig> TYPE = new Type<>(ID);
		public static final StreamCodec<RegistryFriendlyByteBuf, SaveConfig> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC.cast(), SaveConfig::pos,
				ByteBufCodecs.BOOL.cast(), SaveConfig::sender,
				STRING, SaveConfig::url,
				STRING, SaveConfig::payload,
				SaveConfig::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
