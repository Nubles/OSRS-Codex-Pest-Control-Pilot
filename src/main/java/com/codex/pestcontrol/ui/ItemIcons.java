package com.codex.pestcontrol.ui;

import java.awt.image.BufferedImage;

/** Item pictures, which may arrive later; the callback runs once one has loaded. */
public interface ItemIcons
{
	BufferedImage get(int itemId, Runnable onLoaded);
}
