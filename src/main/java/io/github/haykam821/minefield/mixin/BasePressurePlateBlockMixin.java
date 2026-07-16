package io.github.haykam821.minefield.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.haykam821.minefield.game.event.PressPressurePlateEvent;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(BasePressurePlateBlock.class)
public class BasePressurePlateBlockMixin {
	@Inject(method = "checkPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/core/BlockPos;)V", ordinal = 1))
	private void invokePressPressurePlateListeners(Entity entity, Level world, BlockPos pos, BlockState state, int power, CallbackInfo ci) {
		if (world.isClientSide()) return;

		try (EventInvokers invokers = Stimuli.select().forEntityAt(entity, pos)) {
			invokers.get(PressPressurePlateEvent.EVENT).pressPressurePlate(pos);
		}
	}
}
