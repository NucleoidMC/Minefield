package io.github.haykam821.minefield.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.haykam821.minefield.game.event.PressPressurePlateEvent;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(AbstractPressurePlateBlock.class)
public class AbstractPressurePlateBlockMixin {
	@Inject(method = "updatePlateState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;emitGameEvent(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/event/GameEvent;Lnet/minecraft/util/math/BlockPos;)V", ordinal = 1))
	private void invokeAfterBlockPlaceListeners(Entity entity, World world, BlockPos pos, BlockState state, int power, CallbackInfo ci) {
		if (world.isClient()) return;

		try (EventInvokers invokers = Stimuli.select().forEntityAt(entity, pos)) {
			invokers.get(PressPressurePlateEvent.EVENT).pressPressurePlate(pos);
		}
	}
}
