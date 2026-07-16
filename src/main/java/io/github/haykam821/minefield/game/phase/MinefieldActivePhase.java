package io.github.haykam821.minefield.game.phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import io.github.haykam821.minefield.Main;
import io.github.haykam821.minefield.game.MinefieldConfig;
import io.github.haykam821.minefield.game.event.PressPressurePlateEvent;
import io.github.haykam821.minefield.game.map.MinefieldMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKeys;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class MinefieldActivePhase {
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	private final ServerLevel level;
	private final GameSpace gameSpace;
	private final MinefieldMap map;
	private final MinefieldConfig config;
	private final HolderAttachment guideText;
	private final Set<ServerPlayer> players;
	private final Object2IntOpenHashMap<ServerPlayer> explosions = new Object2IntOpenHashMap<>();
	private final List<ServerPlayer> resetPlayers = new ArrayList<>();
	private final GameStatisticBundle statistics;
	private boolean singleplayer;
	private int endTicks = -1;
	private int ticks = 0;

	public MinefieldActivePhase(GameSpace gameSpace, ServerLevel level, MinefieldMap map, MinefieldConfig config, HolderAttachment guideText, Set<ServerPlayer> players) {
		this.level = level;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.guideText = guideText;

		this.players = players;
		this.explosions.defaultReturnValue(0);

		this.statistics = config.getStatisticBundle(gameSpace);
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BREAK_BLOCKS);
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.INTERACTION);
		activity.deny(GameRuleType.PLACE_BLOCKS);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.THROW_ITEMS);
	}

	public static void open(GameSpace gameSpace, ServerLevel level, MinefieldMap map, MinefieldConfig config, HolderAttachment guideText) {
		Set<ServerPlayer> players = gameSpace.getPlayers().participants().stream().collect(Collectors.toSet());
		MinefieldActivePhase phase = new MinefieldActivePhase(gameSpace, level, map, config, guideText, players);

		gameSpace.setActivity(activity -> {
			MinefieldActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GamePlayerEvents.REMOVE, phase::removePlayer);
			activity.listen(PressPressurePlateEvent.EVENT, phase::onPressPressurePlate);
		});
	}

	private void enable() {
		this.singleplayer = this.players.size() == 1;

 		for (ServerPlayer player : this.players) {
			player.setGameMode(GameType.ADVENTURE);

			if (!this.singleplayer && this.statistics != null) {
				this.statistics.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1);
			}
		}

		for (ServerPlayer player : this.gameSpace.getPlayers().spectators()) {
			player.setGameMode(GameType.SPECTATOR);
			this.map.spawn(player, this.level);
		}

		this.map.removeBarrierPerimeter(this.level);
	}

	private void tick() {
		this.ticks += 1;
		if (this.guideText != null && ticks == this.config.getGuideTicks()) {
			this.guideText.destroy();
		}

		// Delay between game end and game close
		if (this.endTicks >= 0) {
			if (this.endTicks == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			} else {
				this.endTicks -= 1;
			}
			return;
		}

		for (ServerPlayer player : this.players) {
			if (this.map.isBelowPlatform(player)) {
				this.map.spawn(player, this.level);
			}

			if (this.map.isAtEnd(player) && this.endTicks == -1) {
				this.gameSpace.getPlayers().sendMessage(Component.translatable("text.minefield.win", player.getDisplayName()).withStyle(ChatFormatting.GOLD));
				this.endTicks = this.config.getEndTicks();
				this.gameSpace.getPlayers().playSound(SoundEvents.PLAYER_LEVELUP, SoundSource.UI, 1, 1);

				if (!this.singleplayer && this.statistics != null) {
					this.statistics.forPlayer(player).increment(StatisticKeys.GAMES_WON, 1);
					this.statistics.forPlayer(player).set(StatisticKeys.QUICKEST_TIME, this.ticks);

					for (ServerPlayer statisticPlayer : this.players) {
						if (player != statisticPlayer) {
							this.statistics.forPlayer(statisticPlayer).increment(StatisticKeys.GAMES_LOST, 1);
						}
					}
				}
			}
		}

		// Reset players that stepped on a mine between now and the last tick
		for (ServerPlayer player : this.resetPlayers) {
			this.map.spawn(player, this.level);

			if (!this.singleplayer && this.statistics != null) {
				this.statistics.forPlayer(player).increment(Main.MINES_ACTIVATED, 1);
			}
		}
		this.resetPlayers.clear();
	}

	private void setSpectator(ServerPlayer player) {
		player.setGameMode(GameType.SPECTATOR);
	}

	private JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.level, this.map.getSpawnPos()).thenRunForEach(player -> {
			this.setSpectator(player);
		});
	}

	private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		this.map.spawn(player, this.level);
		return EventResult.DENY;
	}

	private void removePlayer(ServerPlayer player) {
		if (this.players.remove(player) && !this.singleplayer && this.statistics != null) {
			this.statistics.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1);
		}
	}

	private void onPressPressurePlate(BlockPos pos) {
		this.level.playSound(null, pos.above(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 1);
		this.level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 1, 0, 0, 0, 1);

		AABB box = new AABB(pos);
		for (ServerPlayer player : this.players) {
			if (box.intersects(player.getBoundingBox())) {
				this.resetPlayers.add(player);
			}
		}

		if (this.config.shouldRemoveExplodedPressurePlates()) {
			this.level.setBlockAndUpdate(pos, AIR);
		}
	}
}