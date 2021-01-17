package io.github.haykam821.minefield.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.haykam821.minefield.game.event.PressPressurePlateListener;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.game.ManagedGameSpace;

@Mixin(AbstractPressurePlateBlock.class)
public class AbstractPressurePlateBlockMixin {
	@Inject(method = "updatePlateState", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/AbstractPressurePlateBlock;playPressSound(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;)V"))
	private void invokeAfterBlockPlaceListeners(World world, BlockPos pos, BlockState state, int power, CallbackInfo ci) {
		if (world.isClient()) return;

		ManagedGameSpace gameSpace = ManagedGameSpace.forWorld(world);
		if (gameSpace == null) return;
		
		gameSpace.invoker(PressPressurePlateListener.EVENT).pressPressurePlate(pos);
	}
}
