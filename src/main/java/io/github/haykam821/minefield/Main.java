package io.github.haykam821.minefield;

import io.github.haykam821.minefield.game.MinefieldConfig;
import io.github.haykam821.minefield.game.phase.MinefieldWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	private static final String MOD_ID = "minefield";

	private static final Identifier MINEFIELD_ID = new Identifier(MOD_ID, "minefield");
	public static final GameType<MinefieldConfig> MINEFIELD_TYPE = GameType.register(MINEFIELD_ID, MinefieldWaitingPhase::open, MinefieldConfig.CODEC);

	@Override
	public void onInitialize() {
		return;
	}
}