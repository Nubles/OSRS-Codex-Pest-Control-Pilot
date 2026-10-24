package com.codex.pestcontrol.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.codex.pestcontrol.core.Reward;
import java.awt.Rectangle;
import java.lang.reflect.Proxy;
import java.util.Map;
import net.runelite.api.widgets.Widget;
import org.junit.Test;

public class RewardShopGuideTest
{
	/** A widget that answers only what the guide asks. */
	private static Widget widget(String text, int item, Rectangle bounds, boolean hidden, Widget... kids)
	{
		return (Widget) Proxy.newProxyInstance(Widget.class.getClassLoader(), new Class<?>[]{Widget.class}, (proxy, m, args) ->
		{
			switch (m.getName())
			{
				case "getText":
					return text;
				case "getItemId":
					return item;
				case "getBounds":
					return bounds;
				case "isHidden":
					return hidden;
				case "getDynamicChildren":
					return kids;
				case "getStaticChildren":
				case "getNestedChildren":
					return new Widget[0];
				default:
					return null;
			}
		});
	}

	@Test
	public void aLinesWordsAndPictureAreLitTogether()
	{
		Widget options = widget("", -1, new Rectangle(0, 0, 400, 300), false,
			widget(null, 8839, new Rectangle(10, 10, 36, 32), false),
			widget("Void knight top", -1, new Rectangle(50, 12, 100, 14), false),
			widget("(250 pts)", -1, new Rectangle(50, 28, 60, 14), false),
			widget("Elite void top", -1, new Rectangle(250, 12, 100, 14), false),
			widget("Attack", -1, new Rectangle(10, 200, 60, 14), false));
		Map<Reward, Rectangle> lines = RewardShopGuide.lines(options);
		assertEquals(2, lines.size());
		assertEquals(new Rectangle(10, 10, 140, 32), lines.get(Reward.TOP));
		assertEquals(new Rectangle(250, 12, 100, 14), lines.get(Reward.ELITE_TOP));
	}

	@Test
	public void hiddenLinesAndEmptyBoundsAreLeftAlone()
	{
		Widget options = widget("", -1, new Rectangle(0, 0, 400, 300), false,
			widget("Void knight robe", -1, new Rectangle(50, 12, 100, 14), true),
			widget("Void knight gloves", -1, new Rectangle(0, 0, 0, 0), false));
		assertTrue(RewardShopGuide.lines(options).isEmpty());
		assertFalse(RewardShopGuide.lines(widget("", -1, null, true)).containsKey(Reward.TOP));
	}
}
