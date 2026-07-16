package io.github.haykam821.minefield;

import io.github.haykam821.minefield.game.MinefieldConfig;
import io.github.haykam821.minefield.game.phase.MinefieldWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKey;

public class Main implements ModInitializer {
	private static final String MOD_ID = "minefield";

	private static final Identifier MINEFIELD_ID = identifier("minefield");
	public static final GameType<MinefieldConfig> MINEFIELD_TYPE = GameTypes.register(MINEFIELD_ID, MinefieldConfig.CODEC, MinefieldWaitingPhase::open);

	private static final Identifier MINES_ACTIVATED_ID = identifier("mines_activated");
	public static final StatisticKey<Integer> MINES_ACTIVATED = StatisticKey.intKey(MINES_ACTIVATED_ID);

	@Override
	public void onInitialize() {
		return;
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}