package io.github.haykam821.minefield.game.map;

import io.github.haykam821.minefield.game.MinefieldConfig;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;

public class MinefieldMapBuilder {
	private static final int ORIGIN_Y = 64;
	private static final BlockState START_FLOOR = Blocks.SMOOTH_STONE.defaultBlockState();
	private static final BlockState MINES_FLOOR = Blocks.SANDSTONE.defaultBlockState();
	private static final BlockState MINE = Blocks.STONE_PRESSURE_PLATE.defaultBlockState();
	private static final BlockState END_FLOOR = Blocks.EMERALD_BLOCK.defaultBlockState();
	private static final BlockState BARRIER = Blocks.BARRIER.defaultBlockState();

	private final MinefieldConfig config;

	public MinefieldMapBuilder(MinefieldConfig config) {
		this.config = config;
	}

	private void placeBarrierPerimeter(MapTemplate template, BlockBounds start) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		pos.setY(start.min().getY() + 2);

		int barrierMinX = start.min().getX() - 1;
		int barrierMaxX = start.max().getX() + 1;

		int barrierMinZ = start.min().getZ() - 1;
		int barrierMaxZ = start.max().getZ() + 1;

		for (int x = barrierMinX; x <= barrierMaxX; x += 1) {
			pos.setX(x);

			pos.setZ(barrierMinZ);
			template.setBlockState(pos, BARRIER);

			pos.setZ(barrierMaxZ);
			template.setBlockState(pos, BARRIER);
		}

		for (int z = barrierMinZ + 1; z <= barrierMaxZ - 1; z += 1) {
			pos.setZ(z);

			pos.setX(barrierMinX);
			template.setBlockState(pos, BARRIER);

			pos.setX(barrierMaxX);
			template.setBlockState(pos, BARRIER);
		}
	}

	public MinefieldMap create() {
		MapTemplate template = MapTemplate.createEmpty();
		MinefieldMapConfig mapConfig = this.config.getMapConfig();

		int maxZ = mapConfig.getZ() - 1;
		BlockBounds start = BlockBounds.of(new BlockPos(0, ORIGIN_Y, 0), new BlockPos(mapConfig.getPadding() - 1, ORIGIN_Y, maxZ));
		BlockBounds mines = BlockBounds.of(new BlockPos(mapConfig.getPadding(), ORIGIN_Y, 0), new BlockPos(mapConfig.getPadding() + mapConfig.getX() - 1, ORIGIN_Y, maxZ));
		BlockBounds end = BlockBounds.of(new BlockPos(mapConfig.getPadding() + mapConfig.getX(), ORIGIN_Y, 0), new BlockPos(mapConfig.getPadding() * 2 + mapConfig.getX() - 1, ORIGIN_Y + 2, maxZ));

		Vec3 guideTextPos = new Vec3(mapConfig.getPadding() + 1, mines.min().getY() + 2, mines.center().z());

		// Place blocks in template
		for (BlockPos pos : start) {
			template.setBlockState(pos, START_FLOOR);
		}
		for (BlockPos pos : end) {
			if (pos.getY() == ORIGIN_Y) {
				template.setBlockState(pos, END_FLOOR);
			}
		}

		RandomSource random = RandomSource.createThreadLocalInstance();
		for (BlockPos pos : mines) {
			template.setBlockState(pos, MINES_FLOOR);
			if (random.nextDouble() < mapConfig.getMineChance()) {
				template.setBlockState(pos.above(), MINE);
			}
		}

		this.placeBarrierPerimeter(template, start);

		return new MinefieldMap(template, start, end, guideTextPos);
	}
}