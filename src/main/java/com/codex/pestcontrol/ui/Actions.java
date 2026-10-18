package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Goal;
import com.codex.pestcontrol.core.Reward;

/** What the panel asks the plugin to do. */
public interface Actions
{
	void preset(Goal goal);

	void toggle(Reward reward);
}
