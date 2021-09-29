package io.github.haykam821.minefield.game.map;

import java.util.Random;

import io.github.haykam821.minefield.game.MinefieldConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;

public class MinefieldMapBuilder {
	private static final int ORIGIN_Y = 64;
	private static final BlockState START_FLOOR = Blocks.SMOOTH_STONE.getDefaultState();
	private static final BlockState MINES_FLOOR = Blocks.SANDSTONE.getDefaultState();
	private static final BlockState MINE = Blocks.STONE_PRESSURE_PLATE.getDefaultState();
	private static final BlockState END_FLOOR = Blocks.EMERALD_BLOCK.getDefaultState();

	private final MinefieldConfig config;

	public MinefieldMapBuilder(MinefieldConfig config) {
		this.config = config;
	}

	public MinefieldMap create() {
		MapTemplate template = MapTemplate.createEmpty();
		MinefieldMapConfig mapConfig = this.config.getMapConfig();

		int maxZ = mapConfig.getZ() - 1;
		BlockBounds start = BlockBounds.of(new BlockPos(0, ORIGIN_Y, 0), new BlockPos(mapConfig.getPadding() - 1, ORIGIN_Y, maxZ));
		BlockBounds mines = BlockBounds.of(new BlockPos(mapConfig.getPadding(), ORIGIN_Y, 0), new BlockPos(mapConfig.getPadding() + mapConfig.getX() - 1, ORIGIN_Y, maxZ));
		BlockBounds end = BlockBounds.of(new BlockPos(mapConfig.getPadding() + mapConfig.getX(), ORIGIN_Y, 0), new BlockPos(mapConfig.getPadding() * 2 + mapConfig.getX() - 1, ORIGIN_Y + 2, maxZ));

		Vec3d guideTextPos = new Vec3d(mapConfig.getPadding() + 1, mines.min().getY() + 2.8, mines.center().getZ());

		// Place blocks in template
		for (BlockPos pos : start) {
			template.setBlockState(pos, START_FLOOR);
		}
		for (BlockPos pos : end) {
			if (pos.getY() == ORIGIN_Y) {
				template.setBlockState(pos, END_FLOOR);
			}
		}

		Random random = new Random();
		for (BlockPos pos : mines) {
			template.setBlockState(pos, MINES_FLOOR);
			if (random.nextDouble() < mapConfig.getMineChance()) {
				template.setBlockState(pos.up(), MINE);
			}
		}

		return new MinefieldMap(template, start, end, guideTextPos);
	}
}