package io.github.haykam821.minefield.game.phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.haykam821.minefield.game.MinefieldConfig;
import io.github.haykam821.minefield.game.event.PressPressurePlateListener;
import io.github.haykam821.minefield.game.map.MinefieldMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.PlayerRemoveListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

public class MinefieldActivePhase {
	private static final BlockState AIR = Blocks.AIR.getDefaultState();

	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final MinefieldMap map;
	private final MinefieldConfig config;
	private final Set<ServerPlayerEntity> players;
	private final Object2IntOpenHashMap<ServerPlayerEntity> explosions = new Object2IntOpenHashMap<>();
	private final List<ServerPlayerEntity> resetPlayers = new ArrayList<>();
	private int endTicks = -1;

	public MinefieldActivePhase(GameSpace gameSpace, MinefieldMap map, MinefieldConfig config, Set<ServerPlayerEntity> players) {
		this.world = gameSpace.getWorld();
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;

		this.players = players;
		this.explosions.defaultReturnValue(0);
	}

	public static void setRules(GameLogic game) {
		game.setRule(GameRule.BREAK_BLOCKS, RuleResult.DENY);
		game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
		game.setRule(GameRule.CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.HUNGER, RuleResult.DENY);
		game.setRule(GameRule.INTERACTION, RuleResult.DENY);
		game.setRule(GameRule.PLACE_BLOCKS, RuleResult.DENY);
		game.setRule(GameRule.PORTALS, RuleResult.DENY);
		game.setRule(GameRule.PVP, RuleResult.DENY);
		game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
	}

	public static void open(GameSpace gameSpace, MinefieldMap map, MinefieldConfig config) {
		Set<ServerPlayerEntity> players = gameSpace.getPlayers().stream().collect(Collectors.toSet());
		MinefieldActivePhase phase = new MinefieldActivePhase(gameSpace, map, config, players);

		gameSpace.openGame(game -> {
			MinefieldActivePhase.setRules(game);

			// Listeners
			game.on(GameOpenListener.EVENT, phase::open);
			game.on(GameTickListener.EVENT, phase::tick);
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
			game.on(PlayerRemoveListener.EVENT, phase::removePlayer);
			game.on(PressPressurePlateListener.EVENT, phase::onPressPressurePlate);
		});
	}

	private void open() {
 		for (ServerPlayerEntity player : this.players) {
			player.setGameMode(GameMode.ADVENTURE);
			this.map.spawn(player, this.world);
		}
	}

	private void tick() {
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
				this.gameSpace.getPlayers().sendMessage(new TranslatableText("text.minefield.win", player.getDisplayName()).formatted(Formatting.GOLD));
				this.endTicks = this.config.getEndTicks();
			}
		}

		// Reset players that stepped on a mine between now and the last tick
		for (ServerPlayerEntity player : this.resetPlayers) {
			this.map.spawn(player, this.world);
		}
		this.resetPlayers.clear();
	}

	private void setSpectator(PlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}

	private void addPlayer(ServerPlayerEntity player) {
		if (!this.players.contains(player)) {
			this.setSpectator(player);
		}
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.map.spawn(player, this.world);
		return ActionResult.FAIL;
	}

	private void removePlayer(ServerPlayerEntity player) {
		this.players.remove(player);
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