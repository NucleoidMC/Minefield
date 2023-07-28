package io.github.haykam821.minefield.game.phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import eu.pb4.holograms.api.holograms.AbstractHologram;
import io.github.haykam821.minefield.Main;
import io.github.haykam821.minefield.game.MinefieldConfig;
import io.github.haykam821.minefield.game.event.PressPressurePlateEvent;
import io.github.haykam821.minefield.game.map.MinefieldMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.game.stats.StatisticKeys;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class MinefieldActivePhase {
	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final MinefieldMap map;
	private final MinefieldConfig config;
	private final AbstractHologram guideText;
	private final Set<ServerPlayerEntity> players;
	private final Object2IntOpenHashMap<ServerPlayerEntity> explosions = new Object2IntOpenHashMap<>();
	private final List<ServerPlayerEntity> resetPlayers = new ArrayList<>();
	private final GameStatisticBundle statistics;
	private boolean singleplayer;
	private int endTicks = -1;
	private int ticks = 0;

	public MinefieldActivePhase(GameSpace gameSpace, ServerWorld world, MinefieldMap map, MinefieldConfig config, AbstractHologram guideText, Set<ServerPlayerEntity> players) {
		this.world = world;
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

	public static void open(GameSpace gameSpace, ServerWorld world, MinefieldMap map, MinefieldConfig config, AbstractHologram guideText) {
		Set<ServerPlayerEntity> players = gameSpace.getPlayers().stream().collect(Collectors.toSet());
		MinefieldActivePhase phase = new MinefieldActivePhase(gameSpace, world, map, config, guideText, players);

		gameSpace.setActivity(activity -> {
			MinefieldActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GamePlayerEvents.REMOVE, phase::removePlayer);
			activity.listen(PressPressurePlateEvent.EVENT, phase::onPressPressurePlate);
		});
	}

	private void enable() {
		this.singleplayer = this.players.size() == 1;

 		for (ServerPlayerEntity player : this.players) {
			player.changeGameMode(GameMode.ADVENTURE);
			this.map.spawn(player, this.world);

			if (!this.singleplayer && this.statistics != null) {
				this.statistics.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1);
			}
		}
	}

	private void tick() {
		this.ticks += 1;
		if (this.guideText != null && ticks == this.config.getGuideTicks()) {
			this.guideText.hide();
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

		for (ServerPlayerEntity player : this.players) {
			if (this.map.isBelowPlatform(player)) {
				this.map.spawn(player, this.world);
			}

			if (this.map.isAtEnd(player) && this.endTicks == -1) {
				this.gameSpace.getPlayers().sendMessage(Text.translatable("text.minefield.win", player.getDisplayName()).formatted(Formatting.GOLD));
				this.endTicks = this.config.getEndTicks();

				if (!this.singleplayer && this.statistics != null) {
					this.statistics.forPlayer(player).increment(StatisticKeys.GAMES_WON, 1);
					this.statistics.forPlayer(player).set(StatisticKeys.QUICKEST_TIME, this.ticks);

					for (ServerPlayerEntity statisticPlayer : this.players) {
						if (player != statisticPlayer) {
							this.statistics.forPlayer(statisticPlayer).increment(StatisticKeys.GAMES_LOST, 1);
						}
					}
				}
			}
		}

		// Reset players that stepped on a mine between now and the last tick
		for (ServerPlayerEntity player : this.resetPlayers) {
			this.map.spawn(player, this.world);

			if (!this.singleplayer && this.statistics != null) {
				this.statistics.forPlayer(player).increment(Main.MINES_ACTIVATED, 1);
			}
		}
		this.resetPlayers.clear();
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.map.getSpawnPos()).and(() -> {
			this.setSpectator(offer.player());
		});
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.map.spawn(player, this.world);
		return ActionResult.FAIL;
	}

	private void removePlayer(ServerPlayerEntity player) {
		if (this.players.remove(player) && !this.singleplayer && this.statistics != null) {
			this.statistics.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1);
		}
	}

	private void onPressPressurePlate(BlockPos pos) {
		this.world.playSound(null, pos.up(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1, 1);
		this.world.spawnParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 1, 0, 0, 0, 1);

		Box box = new Box(pos);
		for (ServerPlayerEntity player : this.players) {
			if (box.intersects(player.getBoundingBox())) {
				this.resetPlayers.add(player);
			}
		}

		if (this.config.shouldRemoveExplodedPressurePlates()) {
			this.world.setBlockState(pos, AIR);
		}
	}
}