package com.httpblock;

import com.httpblock.block.HttpReceiverBlock;
import com.httpblock.block.HttpSenderBlock;
import com.httpblock.blockentity.HttpReceiverBlockEntity;
import com.httpblock.blockentity.HttpSenderBlockEntity;
import com.httpblock.command.HttpBlockCommands;
import com.httpblock.config.HttpBlockConfig;
import com.httpblock.http.HttpClientService;
import com.httpblock.http.HttpPollManager;
import com.httpblock.net.HttpBlockPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPBlock implements ModInitializer {
	public static final String MOD_ID = "http-block";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final HttpSenderBlock HTTP_SENDER = registerBlock("http_sender", key ->
			new HttpSenderBlock(BlockBehaviour.Properties.of()
					.setId(key)
					.mapColor(MapColor.COLOR_GRAY)
					.strength(2.0F, 6.0F)
					.sound(SoundType.METAL)));

	public static final HttpReceiverBlock HTTP_RECEIVER = registerBlock("http_receiver", key ->
			new HttpReceiverBlock(BlockBehaviour.Properties.of()
					.setId(key)
					.mapColor(MapColor.COLOR_LIGHT_BLUE)
					.strength(2.0F, 6.0F)
					.sound(SoundType.METAL)));

	public static final BlockEntityType<HttpSenderBlockEntity> HTTP_SENDER_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("http_sender"),
			FabricBlockEntityTypeBuilder.create(HttpSenderBlockEntity::new, HTTP_SENDER).build());

	public static final BlockEntityType<HttpReceiverBlockEntity> HTTP_RECEIVER_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("http_receiver"),
			FabricBlockEntityTypeBuilder.create(HttpReceiverBlockEntity::new, HTTP_RECEIVER).build());

	@Override
	public void onInitialize() {
		HttpBlockConfig.load();
		registerPayloads();
		registerNetworking();
		HttpBlockCommands.register();

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
			entries.accept(HTTP_SENDER);
			entries.accept(HTTP_RECEIVER);
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> HttpPollManager.tick(server));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> HttpClientService.shutdown());
		LOGGER.info("HTTP Block initialized");
	}

	private static void registerPayloads() {
		PayloadTypeRegistry.clientboundPlay().register(HttpBlockPayloads.OpenConfig.TYPE, HttpBlockPayloads.OpenConfig.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(HttpBlockPayloads.SaveConfig.TYPE, HttpBlockPayloads.SaveConfig.CODEC);
	}

	private static void registerNetworking() {
		ServerPlayNetworking.registerGlobalReceiver(HttpBlockPayloads.SaveConfig.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			player.level().getServer().executeIfPossible(() -> {
				if (!player.mayInteract(player.level(), payload.pos())) {
					return;
				}

				if (player.level().getBlockEntity(payload.pos()) instanceof HttpSenderBlockEntity sender && payload.sender()) {
					sender.setUrl(payload.url());
					sender.setPayload(payload.payload());
				} else if (player.level().getBlockEntity(payload.pos()) instanceof HttpReceiverBlockEntity receiver && !payload.sender()) {
					receiver.setUrl(payload.url());
				}
			});
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static <T extends Block> T registerBlock(String name, BlockFactory<T> factory) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id(name));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(name));
		T block = factory.create(blockKey);
		BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	@FunctionalInterface
	private interface BlockFactory<T extends Block> {
		T create(ResourceKey<Block> key);
	}
}
