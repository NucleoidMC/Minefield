package io.github.haykam821.minefield.game.event;

import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.event.EventType;

public interface PressPressurePlateListener {
	public EventType<PressPressurePlateListener> EVENT = EventType.create(PressPressurePlateListener.class, listeners -> {
		return pos -> {
			for (PressPressurePlateListener listener : listeners) {
				listener.pressPressurePlate(pos);
			}
		};
	});

	public void pressPressurePlate(BlockPos pos);
}
