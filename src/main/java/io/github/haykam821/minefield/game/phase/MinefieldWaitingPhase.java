package io.github.haykam821.minefield.game.phase;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import io.github.haykam821.minefield.game.MinefieldConfig;
import io.github.haykam821.minefield.game.map.MinefieldMap;
import io.github.haykam821.minefield.game.map.MinefieldMapBuilder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class MinefieldWaitingPhase {
	private static final ChatFormatting GUIDE_FORMATTING = ChatFormatting.GOLD;
	private static final Component GUIDE_TEXT = Component.empty()
		.append(Component.translatable("gameType.minefield.minefield").withStyle(ChatFormatting.BOLD))
		.append(CommonComponents.NEW_LINE)
		.append(Component.translatable("text.minefield.guide.reach_the_other_side"))
		.append(CommonComponents.NEW_LINE)
		.append(Component.translatable("text.minefield.guide.avoid_mines"))
		.append(CommonComponents.NEW_LINE)
		.append(Component.translatable("text.minefield.guide.mines_teleport_players"))
		.withStyle(GUIDE_FORMATTING);

	private final GameSpace gameSpace;
	private final ServerLevel world;
	private final MinefieldMap map;
	private final MinefieldConfig config;
	private HolderAttachment guideText;

	public MinefieldWaitingPhase(GameSpace gameSpace, ServerLevel level, MinefieldMap map, MinefieldConfig config) {
		this.gameSpace = gameSpace;
		this.world = level;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<MinefieldConfig> context) {
		MinefieldMapBuilder mapBuilder = new MinefieldMapBuilder(context.config());
		MinefieldMap map = mapBuilder.create();

		RuntimeLevelConfig levelConfig = new RuntimeLevelConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithLevel(levelConfig, (activity, level) -> {
			MinefieldWaitingPhase phase = new MinefieldWaitingPhase(activity.getGameSpace(), level, map, context.config());
			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());

			MinefieldActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(PlayerDamageEvent.EVENT, phase::onPlayerDamage);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
		});
	}

	private void enable() {
		TextDisplayElement element = new TextDisplayElement(GUIDE_TEXT);

		element.setBillboardMode(BillboardConstraints.CENTER);
		element.setLineWidth(350);

		ElementHolder holder = new ElementHolder();
		holder.addElement(element);

		// Spawn guide text
		Vec3 guideTextPos = this.map.getGuideTextPos();
		this.guideText = ChunkAttachment.of(holder, world, guideTextPos);
	}

	private void tick() {
		for (ServerPlayer player : this.gameSpace.getPlayers()) {
			if (!this.map.isInSpawn(player)) {
				this.map.spawn(player, this.world);
			}
		}
	}

	private JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.world, this.map.getSpawnPos()).thenRunForEach(player -> {
			player.setGameMode(GameType.ADVENTURE);
		});
	}

	private GameResult requestStart() {
		MinefieldActivePhase.open(this.gameSpace, this.world, this.map, this.config, this.guideText);
		return GameResult.ok();
	}

	private EventResult onPlayerDamage(ServerPlayer player, DamageSource source, float amount) {
		return EventResult.DENY;
	}

	private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		this.map.spawn(player, this.world);
		return EventResult.DENY;
	}
}