package io.github.haykam821.minefield.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.minefield.game.map.MinefieldMapConfig;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;

public class MinefieldConfig {
	public static final Codec<MinefieldConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			MinefieldMapConfig.CODEC.fieldOf("map").forGetter(MinefieldConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(MinefieldConfig::getPlayerConfig),
			Codec.INT.optionalFieldOf("end_ticks", 20 * 5).forGetter(MinefieldConfig::getEndTicks),
			Codec.BOOL.optionalFieldOf("remove_exploded_pressure_plates", true).forGetter(MinefieldConfig::shouldRemoveExplodedPressurePlates)
		).apply(instance, MinefieldConfig::new);
	});

	private final MinefieldMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final int endTicks;
	private final boolean removeExplodedPressurePlates;

	public MinefieldConfig(MinefieldMapConfig mapConfig, PlayerConfig playerConfig, int endTicks, boolean removeExplodedPressurePlates) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.endTicks = endTicks;
		this.removeExplodedPressurePlates = removeExplodedPressurePlates;
	}

	public MinefieldMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public int getEndTicks() {
		return this.endTicks;
	}

	public boolean shouldRemoveExplodedPressurePlates() {
		return this.removeExplodedPressurePlates;
	}
}