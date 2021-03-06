package io.github.haykam821.minefield.game.phase;

import io.github.haykam821.minefield.game.MinefieldConfig;
import io.github.haykam821.minefield.game.map.MinefieldMap;
import io.github.haykam821.minefield.game.map.MinefieldMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.entity.FloatingText;
import xyz.nucleoid.plasmid.entity.FloatingText.VerticalAlign;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDamageListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;

public class MinefieldWaitingPhase {
	private static final Formatting GUIDE_FORMATTING = Formatting.GOLD;
	private static final Text[] GUIDE_LINES = {
		new TranslatableText("gameType.minefield.minefield").formatted(GUIDE_FORMATTING).formatted(Formatting.BOLD),
		new TranslatableText("text.minefield.guide.reach_the_other_side").formatted(GUIDE_FORMATTING),
		new TranslatableText("text.minefield.guide.avoid_mines").formatted(GUIDE_FORMATTING),
		new TranslatableText("text.minefield.guide.mines_teleport_players").formatted(GUIDE_FORMATTING)
	};

	private final GameSpace gameSpace;
	private final MinefieldMap map;
	private final MinefieldConfig config;
	private FloatingText guideText;

	public MinefieldWaitingPhase(GameSpace gameSpace, MinefieldMap map, MinefieldConfig config) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<MinefieldConfig> context) {
		MinefieldMapBuilder mapBuilder = new MinefieldMapBuilder(context.getConfig());
		MinefieldMap map = mapBuilder.create();

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(map.createGenerator(context.getServer()))
			.setDefaultGameMode(GameMode.ADVENTURE);

		return context.createOpenProcedure(worldConfig, game -> {
			MinefieldWaitingPhase phase = new MinefieldWaitingPhase(game.getSpace(), map, context.getConfig());
			GameWaitingLobby.applyTo(game, context.getConfig().getPlayerConfig());

			MinefieldActivePhase.setRules(game);

			// Listeners
			game.on(GameOpenListener.EVENT, phase::open);
			game.on(GameTickListener.EVENT, phase::tick);
			game.on(OfferPlayerListener.EVENT, phase::offerPlayer);
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDamageListener.EVENT, phase::onPlayerDamage);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
			game.on(RequestStartListener.EVENT, phase::requestStart);
		});
	}

	private boolean isFull() {
		return this.gameSpace.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	private void open() {
		// Spawn guide text
		this.gameSpace.getWorld().getChunk(new BlockPos(this.map.getGuideTextPos()));
		this.guideText = FloatingText.spawn(this.gameSpace.getWorld(), this.map.getGuideTextPos(), GUIDE_LINES, VerticalAlign.CENTER);
	}

	private void tick() {
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			if (this.map.isBelowPlatform(player)) {
				this.map.spawn(player, this.gameSpace.getWorld());
			}
		}
	}

	private JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	private StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameSpace.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.NOT_ENOUGH_PLAYERS;
		}

		MinefieldActivePhase.open(this.gameSpace, this.map, this.config, this.guideText);
		return StartResult.OK;
	}

	private void addPlayer(ServerPlayerEntity player) {
		this.map.spawn(player, this.gameSpace.getWorld());
	}

	private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return ActionResult.FAIL;
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.map.spawn(player, this.gameSpace.getWorld());
		return ActionResult.FAIL;
	}
}