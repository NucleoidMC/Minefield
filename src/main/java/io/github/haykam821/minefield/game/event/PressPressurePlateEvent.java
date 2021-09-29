package io.github.haykam821.minefield.game.event;

import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface PressPressurePlateEvent {
	public StimulusEvent<PressPressurePlateEvent> EVENT = StimulusEvent.create(PressPressurePlateEvent.class, context -> {
		return pos -> {
			try {
				for (PressPressurePlateEvent listener : context.getListeners()) {
					listener.pressPressurePlate(pos);
				}
			} catch (Throwable throwable) {
				context.handleException(throwable);
			}
		};
	});

	public void pressPressurePlate(BlockPos pos);
}
