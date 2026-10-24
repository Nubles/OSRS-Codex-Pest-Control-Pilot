package com.codex.pestcontrol.core;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Your best games ever, kept apart from the recent games so they outlive them: the most damage
 * in a game, the fastest win and the longest run of games won.
 */
public final class Bests
{
	/** What a game can beat. */
	public enum Kind
	{
		DAMAGE,
		FASTEST,
		STREAK
	}

	/** A best is only news once there are this many games to have beaten. */
	static final int NEWS_AFTER = 5;
	/** A win shorter than this was not a whole game seen from the start. */
	static final int SHORTEST_WIN = 60;

	private int damage;
	private long damageAt;
	private int fastest;
	private long fastestAt;
	private int streak;
	private int bestStreak;
	private long bestStreakAt;
	private int counted;
	/** Whether the run in hand has already been called a new best. */
	private boolean runTold;

	/** Works the bests out from the games kept, for a save made before bests were kept. */
	static Bests of(List<GameRecord> games)
	{
		Bests b = new Bests();
		for (GameRecord g : games)
		{
			b.add(g);
		}
		return b;
	}

	/** @return what the game beat, empty early on when every game is a best */
	public Set<Kind> add(GameRecord g)
	{
		Set<Kind> beat = EnumSet.noneOf(Kind.class);
		if (g == null || g.getResult() == GameRecord.Result.LEFT)
		{
			return beat;
		}
		boolean news = counted >= NEWS_AFTER;
		counted++;
		if (g.getDamage() > damage)
		{
			if (news && damage > 0)
			{
				beat.add(Kind.DAMAGE);
			}
			damage = g.getDamage();
			damageAt = g.getEnded();
		}
		if (g.getResult() == GameRecord.Result.LOST)
		{
			streak = 0;
			runTold = false;
			return beat;
		}
		if (g.getResult() == GameRecord.Result.WON && g.getSeconds() >= SHORTEST_WIN && (fastest == 0 || g.getSeconds() < fastest))
		{
			if (news && fastest > 0)
			{
				beat.add(Kind.FASTEST);
			}
			fastest = g.getSeconds();
			fastestAt = g.getEnded();
		}
		streak++;
		if (streak > bestStreak)
		{
			// Said once, as the run first passes the old best.
			if (news && bestStreak > 0 && !runTold)
			{
				beat.add(Kind.STREAK);
				runTold = true;
			}
			bestStreak = streak;
			bestStreakAt = g.getEnded();
		}
		return beat;
	}

	/** A copy to hand the panel, which draws on another thread. */
	public Bests copy()
	{
		Bests c = new Bests();
		c.damage = damage;
		c.damageAt = damageAt;
		c.fastest = fastest;
		c.fastestAt = fastestAt;
		c.streak = streak;
		c.bestStreak = bestStreak;
		c.bestStreakAt = bestStreakAt;
		c.counted = counted;
		c.runTold = runTold;
		return c;
	}

	Bests repair()
	{
		damage = Math.max(0, damage);
		fastest = Math.max(0, fastest);
		streak = Math.max(0, streak);
		bestStreak = Math.max(streak, bestStreak);
		counted = Math.max(0, counted);
		return this;
	}

	public boolean isEmpty()
	{
		return counted == 0;
	}

	/** Most damage in one game, 0 before any. */
	public int getDamage()
	{
		return damage;
	}

	public long getDamageAt()
	{
		return damageAt;
	}

	/** Seconds of the fastest win, 0 before any. */
	public int getFastest()
	{
		return fastest;
	}

	public long getFastestAt()
	{
		return fastestAt;
	}

	/** Games won in a row up to now. */
	public int getStreak()
	{
		return streak;
	}

	/** The longest run of games won. */
	public int getBestStreak()
	{
		return bestStreak;
	}

	public long getBestStreakAt()
	{
		return bestStreakAt;
	}

	/** The chat line for what a game beat, or null. */
	public String news(Set<Kind> beat)
	{
		if (beat == null || beat.isEmpty())
		{
			return null;
		}
		StringBuilder s = new StringBuilder("A new best: ");
		String sep = "";
		if (beat.contains(Kind.DAMAGE))
		{
			s.append(sep).append(damage).append(" damage");
			sep = ", ";
		}
		if (beat.contains(Kind.FASTEST))
		{
			s.append(sep).append("a win in ").append(fastest / 60).append("m ").append(String.format(java.util.Locale.ENGLISH, "%02ds", fastest % 60));
			sep = ", ";
		}
		if (beat.contains(Kind.STREAK))
		{
			s.append(sep).append(bestStreak).append(" wins in a row");
		}
		return s.toString();
	}
}
