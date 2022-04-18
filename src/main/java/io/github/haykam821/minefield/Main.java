package io.github.haykam821.minefield;

import io.github.haykam821.minefield.game.MinefieldConfig;
import io.github.haykam821.minefield.game.phase.MinefieldWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;
import xyz.nucleoid.plasmid.game.stats.StatisticKey;

public class Main implements ModInitializer {
	public static final String MOD_ID = "minefield";

	private static final Identifier MINEFIELD_ID = new Identifier(MOD_ID, "minefield");
	public static final GameType<MinefieldConfig> MINEFIELD_TYPE = GameType.register(MINEFIELD_ID, MinefieldConfig.CODEC, MinefieldWaitingPhase::open);

	private static final Identifier MINES_ACTIVATED_ID = new Identifier(MOD_ID, "mines_activated");
	public static final StatisticKey<Integer> MINES_ACTIVATED = StatisticKey.intKey(MINES_ACTIVATED_ID);

	@Override
	public void onInitialize() {
		return;
	}
}