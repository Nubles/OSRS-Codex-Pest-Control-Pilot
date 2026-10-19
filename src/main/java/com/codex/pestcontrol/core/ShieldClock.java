package com.codex.pestcontrol.core;

/**
 * How long shields take to drop, learned from your own games: the ticks from the start to the
 * first drop, and between each drop and the next. Saved inside the logbook.
 */
public class ShieldClock
{
	/** Gaps taken as one sample at least; others are missed messages or late starts. */
	static final int MIN_GAP = 5;
	static final int MAX_GAP = 600;
	/** Samples needed before a gap is trusted. */
	static final int TRUSTED = 2;

	private long[] sums = new long[4];
	private int[] counts = new int[4];

	/** Learns from one game's drop ticks, -1 for drops whose moment was not seen. */
	public void learn(java.util.List<Integer> drops)
	{
		repair();
		int previous = 0;
		for (int step = 0; step < drops.size() && step < 4; step++)
		{
			int at = drops.get(step);
			if (at < 0 || previous < 0)
			{
				previous = at;
				continue;
			}
			int gap = at - previous;
			if (gap >= MIN_GAP && gap <= MAX_GAP)
			{
				sums[step] += gap;
				counts[step]++;
			}
			previous = at;
		}
	}

	/** Ticks the gap before drop number step usually lasts, or -1 when not yet learned. */
	public int expected(int step)
	{
		repair();
		if (step < 0 || step >= 4 || counts[step] < TRUSTED)
		{
			return -1;
		}
		long gap = Math.round(sums[step] / (double) counts[step]);
		// Only a damaged save gives an average no single gap could.
		return gap < MIN_GAP || gap > MAX_GAP ? -1 : (int) gap;
	}

	public int samples(int step)
	{
		repair();
		return step < 0 || step >= 4 ? 0 : counts[step];
	}

	/** Fills in what a damaged save left out; a step with a count or sum below zero starts again. */
	void repair()
	{
		if (sums == null || sums.length != 4)
		{
			sums = new long[4];
		}
		if (counts == null || counts.length != 4)
		{
			counts = new int[4];
		}
		for (int i = 0; i < 4; i++)
		{
			if (sums[i] < 0 || counts[i] < 0)
			{
				sums[i] = 0;
				counts[i] = 0;
			}
		}
	}
}
