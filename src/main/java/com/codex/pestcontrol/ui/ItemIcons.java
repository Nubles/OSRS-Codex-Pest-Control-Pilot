package com.codex.pestcontrol.ui;

import java.awt.image.BufferedImage;
import java.util.Collection;

/** Item pictures, which may arrive later; the callback runs once one has loaded. */
public interface ItemIcons
{
	BufferedImage get(int itemId, Runnable onLoaded);

	/**
	 * Runs {@code then} once every one of these pictures has arrived: at once when they all have,
	 * otherwise when the last comes in. A card drawn to be shared waits on this, so it never goes
	 * out with an empty slot where a picture was still on its way.
	 */
	default void whenLoaded(Collection<Integer> itemIds, Runnable then)
	{
		then.run();
	}
}
