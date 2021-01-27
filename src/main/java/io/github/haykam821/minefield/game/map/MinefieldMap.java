package io.github.haykam821.minefield.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class MinefieldMap {
	private final MapTemplate template;
	private final Vec3d spawnPos;
	private final BlockBounds end;
	private final Vec3d guideTextPos;

	public MinefieldMap(MapTemplate template, BlockBounds start, BlockBounds end, Vec3d guideTextPos) {
		this.template = template;
		this.spawnPos = start.getCenterBottom().add(0, 1, 0);
		this.end = end;
		this.guideTextPos = guideTextPos;
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