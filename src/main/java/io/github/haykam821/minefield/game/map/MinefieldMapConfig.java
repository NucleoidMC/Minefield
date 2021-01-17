package io.github.haykam821.minefield.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class MinefieldMapConfig {
	public static final Codec<MinefieldMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(MinefieldMapConfig::getX),
			Codec.INT.fieldOf("z").forGetter(MinefieldMapConfig::getZ),
			Codec.INT.fieldOf("padding").forGetter(MinefieldMapConfig::getPadding),
			Codec.DOUBLE.optionalFieldOf("mine_chance", 0.6).forGetter(MinefieldMapConfig::getMineChance)
		).apply(instance, MinefieldMapConfig::new);
	});

	private final int x;
	private final int z;
	private final int padding;
	private final double mineChance;

	public MinefieldMapConfig(int x, int z, int padding, double mineChance) {
		this.x = x;
		this.z = z;
		this.padding = padding;
		this.mineChance = mineChance;
	}

	public int getX() {
		return this.x;
	}

	public int getZ() {
		return this.z;
	}

	public int getPadding() {
		return this.padding;
	}

	public double getMineChance() {
		return this.mineChance;
	}
}