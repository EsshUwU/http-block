package com.httpblock.block;

import com.httpblock.HTTPBlock;
import com.httpblock.blockentity.HttpSenderBlockEntity;
import com.httpblock.net.HttpBlockPayloads;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class HttpSenderBlock extends BaseEntityBlock {
	public static final BooleanProperty POWERED = BooleanProperty.create("powered");
	public static final MapCodec<HttpSenderBlock> CODEC = simpleCodec(HttpSenderBlock::new);

	public HttpSenderBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HttpSenderBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock, Orientation orientation, boolean movedByPiston) {
		if (level.isClientSide()) {
			return;
		}

		boolean hasSignal = level.hasNeighborSignal(pos);
		boolean wasPowered = state.getValue(POWERED);
		if (hasSignal != wasPowered) {
			level.setBlock(pos, state.setValue(POWERED, hasSignal), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, hasSignal ? 1.0F : 0.75F);
			if (hasSignal && level.getBlockEntity(pos) instanceof HttpSenderBlockEntity sender) {
				sender.send();
			}
		}
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return false;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof HttpSenderBlockEntity sender) {
			ServerPlayNetworking.send(serverPlayer, new HttpBlockPayloads.OpenConfig(pos, true, sender.getUrl(), sender.getPayload(), ""));
		}
		return InteractionResult.SUCCESS;
	}
}
