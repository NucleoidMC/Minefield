package io.github.haykam821.minefield.game.map;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

public class MinefieldMap {
	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	private final MapTemplate template;
	private final BlockBounds start;
	private final Box spawnBox;
	private final Vec3d spawnPos;
	private final BlockBounds end;
	private final Vec3d guideTextPos;

	public MinefieldMap(MapTemplate template, BlockBounds start, BlockBounds end, Vec3d guideTextPos) {
		this.template = template;
		this.start = start;
		this.spawnBox = Box.enclosing(start.min().add(0, 1, 0), start.max().add(1, 3, 1));
		this.spawnPos = start.centerBottom().add(0, 1, 0);
		this.end = end;
		this.guideTextPos = guideTextPos;
	}

	public void removeBarrierPerimeter(ServerWorld world) {
		BlockPos.Mutable pos = new BlockPos.Mutable();

		pos.setY(this.start.min().getY() + 2);

		int barrierMinX = this.start.min().getX() - 1;
		int barrierMaxX = this.start.max().getX() + 1;

		int barrierMinZ = this.start.min().getZ() - 1;
		int barrierMaxZ = this.start.max().getZ() + 1;

		for (int x = barrierMinX; x <= barrierMaxX; x += 1) {
			pos.setX(x);

			pos.setZ(barrierMinZ);
			world.setBlockState(pos, AIR);

			pos.setZ(barrierMaxZ);
			world.setBlockState(pos, AIR);
		}

		for (int z = barrierMinZ + 1; z <= barrierMaxZ - 1; z += 1) {
			pos.setZ(z);

			pos.setX(barrierMinX);
			world.setBlockState(pos, AIR);

			pos.setX(barrierMaxX);
			world.setBlockState(pos, AIR);
		}
	}

	public boolean isInSpawn(ServerPlayerEntity player) {
		return this.spawnBox.contains(player.getPos());
	}

	public Vec3d getSpawnPos() {
		return this.spawnPos;
	}

	public void spawn(ServerPlayerEntity player, ServerWorld world) {
		player.teleport(world, this.spawnPos.getX(), this.spawnPos.getY(), this.spawnPos.getZ(), -90, 0);
	}

	public boolean isAtEnd(ServerPlayerEntity player) {
		return this.end.contains(player.getBlockPos());
	}

	public boolean isBelowPlatform(ServerPlayerEntity player) {
		return player.getY() < this.spawnPos.getY();
	}

	public Vec3d getGuideTextPos() {
		return this.guideTextPos;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}