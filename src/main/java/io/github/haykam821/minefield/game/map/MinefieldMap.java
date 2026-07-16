package io.github.haykam821.minefield.game.map;

import java.util.Set;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

public class MinefieldMap {
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	private final MapTemplate template;
	private final BlockBounds start;
	private final AABB spawnBox;
	private final Vec3 spawnPos;
	private final BlockBounds end;
	private final Vec3 guideTextPos;

	public MinefieldMap(MapTemplate template, BlockBounds start, BlockBounds end, Vec3 guideTextPos) {
		this.template = template;
		this.start = start;
		this.spawnBox = AABB.encapsulatingFullBlocks(start.min().offset(0, 1, 0), start.max().offset(1, 3, 1));
		this.spawnPos = start.centerBottom().add(0, 1, 0);
		this.end = end;
		this.guideTextPos = guideTextPos;
	}

	public void removeBarrierPerimeter(ServerLevel world) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		pos.setY(this.start.min().getY() + 2);

		int barrierMinX = this.start.min().getX() - 1;
		int barrierMaxX = this.start.max().getX() + 1;

		int barrierMinZ = this.start.min().getZ() - 1;
		int barrierMaxZ = this.start.max().getZ() + 1;

		for (int x = barrierMinX; x <= barrierMaxX; x += 1) {
			pos.setX(x);

			pos.setZ(barrierMinZ);
			world.setBlockAndUpdate(pos, AIR);

			pos.setZ(barrierMaxZ);
			world.setBlockAndUpdate(pos, AIR);
		}

		for (int z = barrierMinZ + 1; z <= barrierMaxZ - 1; z += 1) {
			pos.setZ(z);

			pos.setX(barrierMinX);
			world.setBlockAndUpdate(pos, AIR);

			pos.setX(barrierMaxX);
			world.setBlockAndUpdate(pos, AIR);
		}
	}

	public boolean isInSpawn(ServerPlayer player) {
		return this.spawnBox.contains(player.position());
	}

	public Vec3 getSpawnPos() {
		return this.spawnPos;
	}

	public void spawn(ServerPlayer player, ServerLevel level) {
		player.teleportTo(level, this.spawnPos.x(), this.spawnPos.y(), this.spawnPos.z(), Set.of(), -90, 0, true);
	}

	public boolean isAtEnd(ServerPlayer player) {
		return this.end.contains(player.blockPosition());
	}

	public boolean isBelowPlatform(ServerPlayer player) {
		return player.getY() < this.spawnPos.y();
	}

	public Vec3 getGuideTextPos() {
		return this.guideTextPos;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}