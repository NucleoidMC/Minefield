package io.github.haykam821.minefield.game.phase;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import io.github.haykam821.minefield.game.MinefieldConfig;
import io.github.haykam821.minefield.game.map.MinefieldMap;
import io.github.haykam821.minefield.game.map.MinefieldMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.DisplayEntity.BillboardMode;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class MinefieldWaitingPhase {
	private static final Formatting GUIDE_FORMATTING = Formatting.GOLD;
	private static final Text GUIDE_TEXT = Text.empty()
		.append(Text.translatable("gameType.minefield.minefield").formatted(Formatting.BOLD))
		.append(ScreenTexts.LINE_BREAK)
		.append(Text.translatable("text.minefield.guide.reach_the_other_side"))
		.append(ScreenTexts.LINE_BREAK)
		.append(Text.translatable("text.minefield.guide.avoid_mines"))
		.append(ScreenTexts.LINE_BREAK)
		.append(Text.translatable("text.minefield.guide.mines_teleport_players"))
		.formatted(GUIDE_FORMATTING);

	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final MinefieldMap map;
	private final MinefieldConfig config;
	private HolderAttachment guideText;

	public MinefieldWaitingPhase(GameSpace gameSpace, ServerWorld world, MinefieldMap map, MinefieldConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<MinefieldConfig> context) {
		MinefieldMapBuilder mapBuilder = new MinefieldMapBuilder(context.config());
		MinefieldMap map = mapBuilder.create();

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithWorld(worldConfig, (activity, world) -> {
			MinefieldWaitingPhase phase = new MinefieldWaitingPhase(activity.getGameSpace(), world, map, context.config());
			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());

			MinefieldActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(PlayerDamageEvent.EVENT, phase::onPlayerDamage);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
		});
	}

	private void enable() {
		TextDisplayElement element = new TextDisplayElement(GUIDE_TEXT);

		element.setBillboardMode(BillboardMode.CENTER);
		element.setLineWidth(350);

		ElementHolder holder = new ElementHolder();
		holder.addElement(element);

		// Spawn guide text
		Vec3d guideTextPos = this.map.getGuideTextPos();
		this.guideText = ChunkAttachment.of(holder, world, guideTextPos);
	}

	private void tick() {
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			if (!this.map.isInSpawn(player)) {
				this.map.spawn(player, this.world);
			}
		}
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.map.getSpawnPos()).and(() -> {
			offer.player().changeGameMode(GameMode.ADVENTURE);
		});
	}

	private GameResult requestStart() {
		MinefieldActivePhase.open(this.gameSpace, this.world, this.map, this.config, this.guideText);
		return GameResult.ok();
	}

	private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return ActionResult.FAIL;
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.map.spawn(player, this.world);
		return ActionResult.FAIL;
	}
}