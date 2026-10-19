package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Advice;
import com.codex.pestcontrol.core.Battle;
import com.codex.pestcontrol.core.Experience;
import com.codex.pestcontrol.core.GameRecord;
import com.codex.pestcontrol.core.Goal;
import com.codex.pestcontrol.core.Landers;
import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Portal;
import com.codex.pestcontrol.core.Reward;
import com.codex.pestcontrol.core.SharedBank;
import com.codex.pestcontrol.core.Sitting;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The Pilot's side panel: the crest with your points toward the goal, then three pages.
 * Battle is the game in progress with the island charted, Goal is the void pieces you are
 * working toward and how many games that takes, and Record is how your games have gone.
 */
public class PilotPanel extends PluginPanel
{
	static final int WIDTH = 200;
	private static final String[] TABS = {"battle", "goal", "record"};
	private static final int RECENT = 10;

	private final ItemIcons icons;
	private final Actions actions;
	private final Crest crest = new Crest();
	private final Segmented tabs = new Segmented(true, "Battle", "Goal", "Record");
	private final Segmented presets;
	private final OutpostMap map = new OutpostMap();
	private final CardLayout cards = new CardLayout();
	private final JPanel content = new JPanel(cards);
	private final JScrollPane battle = scroller();
	private final JScrollPane goal = scroller();
	private final JScrollPane record = scroller();

	private View view = View.loggedOut();
	private String goalShown;
	private String recordShown;

	public PilotPanel(ItemIcons icons, Actions actions)
	{
		super(false);
		this.icons = icons;
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(Theme.INK);

		String[] labels = new String[Goal.values().length];
		for (Goal g : Goal.values())
		{
			labels[g.ordinal()] = g.getShortTitle();
		}
		presets = new Segmented(false, labels);
		presets.onChange(i -> actions.preset(Goal.values()[i]));

		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(crest, BorderLayout.NORTH);
		JPanel tabHolder = new JPanel(new BorderLayout());
		tabHolder.setOpaque(false);
		tabHolder.setBorder(new EmptyBorder(8, 8, 2, 8));
		tabHolder.add(tabs, BorderLayout.CENTER);
		north.add(tabHolder, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		content.setOpaque(false);
		content.add(battle, TABS[0]);
		content.add(goal, TABS[1]);
		content.add(record, TABS[2]);
		add(content, BorderLayout.CENTER);
		tabs.onChange(this::showTab);
		rebuild();
	}

	private static JScrollPane scroller()
	{
		JScrollPane scroll = new JScrollPane();
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new java.awt.Dimension(8, 0));
		return scroll;
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void update(View view)
	{
		boolean started = view.state == View.State.BATTLE && this.view.state != View.State.BATTLE;
		this.view = view;
		presets.setSelected(view.preset == null ? -1 : view.preset.ordinal(), false);
		updateCrest();
		rebuild();
		if (started)
		{
			showTab(0);
		}
	}

	/** Opens one tab, for previews and when a game starts. */
	public void showTab(int index)
	{
		tabs.setSelected(index, false);
		cards.show(content, TABS[index]);
	}

	private void updateCrest()
	{
		Outlook o = view.outlook;
		if (o == null)
		{
			crest.update("PEST CONTROL", null, null, "Log in to plan your points", null, Theme.TEXT_MUTED, Theme.GOLD);
			return;
		}
		String heading = goalTitle().toUpperCase(Locale.ENGLISH);
		Color calm = Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.35);
		if (!o.isPointsKnown())
		{
			crest.update(heading, null, null, "Board a lander to read your points", "Board a lander", calm, Theme.GOLD);
			return;
		}
		if (o.isComplete())
		{
			crest.update(heading, o.getPoints(), "points", "Goal complete: every piece is yours", "Goal complete", Theme.GREEN, Theme.GREEN);
			return;
		}
		if (o.isAffordable())
		{
			crest.update(heading, o.getPoints(), "points", "Enough for all of it: go and buy", "Enough for all of it", Theme.TEAL, Theme.TEAL);
			return;
		}
		String status = Format.number(o.getRemaining()) + " to go, about " + Format.plural(o.getGames(), "game", "games");
		String shortStatus = Format.number(o.getRemaining()) + " to go, ~" + Format.number(o.getGames()) + " games";
		crest.update(heading, o.getPoints(), "of " + Format.number(o.getCost()), status, shortStatus, calm, Theme.GOLD);
	}

	private String goalTitle()
	{
		if (view.preset != null)
		{
			return view.preset.getTitle();
		}
		return view.goal.isEmpty() ? "No goal" : "Your own goal";
	}

	private void rebuild()
	{
		rebuildBattle();
		String g = goalSignature();
		if (!g.equals(goalShown))
		{
			goalShown = g;
			rebuildGoal();
		}
		String r = view.state + "|" + view.played + "|" + view.games.size() + "|" + view.now / 60_000L;
		if (!r.equals(recordShown))
		{
			recordShown = r;
			rebuildRecord();
		}
	}

	private String goalSignature()
	{
		Outlook o = view.outlook;
		return view.state.name().equals("LOGGED_OUT") + "|" + view.goal + "|" + view.owned + "|" + view.ownedKnown + "|" + view.missing + "|" + view.elite
			+ "|" + (o == null ? "-" : o.getPoints() + "/" + o.getPerGame() + "/" + Math.round(o.getMinutes())) + "|" + view.boat
			+ "|" + view.games.size() + "|" + view.ownedFrom + "|" + view.combat + "|" + view.now / 60_000L;
	}

	private boolean loggedOut(StackPanel stack)
	{
		if (view.outlook == null)
		{
			stack.add(note("The outpost is quiet", "Log in and Codex Pest Control Pilot tells you which portal to work next in every game, and counts your points toward the void pieces you want."));
			return true;
		}
		return false;
	}

	// The Battle tab.

	private void rebuildBattle()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (loggedOut(stack))
		{
			replace(battle, stack);
			return;
		}
		View.Fight f = view.fight;
		if (f == null)
		{
			map.setFight(null);
			if (view.state == View.State.LANDER)
			{
				stack.add(note("Waiting in the lander", "When the game starts the Pilot charts the island and tells you which portal to work, marking it in the world and on the minimap."));
			}
			else
			{
				stack.add(note("Not in a game", "Take the boat from Port Sarim to the Void Knights' Outpost and board a lander. The Pilot takes over once the game starts."));
			}
			stack.add(map);
			if (view.last != null)
			{
				stack.add(new SectionHeader("Last game", Format.ago(view.last.getEnded(), view.now)));
				stack.add(new GameRow(view.last, view.now));
			}
			stack.add(new SectionHeader("How it reads a game"));
			stack.add(Ui.wrapped("Shields drop in one of six orders. After the first one or two drops the Pilot knows the rest, numbers the portals on the chart and sends you ahead of the crowd. Open portals come first, the weakest unless another is much nearer, and spinners healing one are called out.",
				Theme.small(), Theme.TEXT_MUTED, WIDTH - 16));
			replace(battle, stack);
			return;
		}

		stack.add(adviceCard(f.advice));
		map.setFight(f);
		stack.add(map);

		JPanel tiles = new JPanel(new GridLayout(1, 3, 6, 0));
		tiles.setOpaque(false);
		Color knight = f.knightHealth < 0 ? Theme.TEXT_MUTED : f.knightHealth <= 60 ? Theme.BLOOD : f.knightHealth <= 120 ? Theme.EMBER : Theme.GREEN;
		tiles.add(new StatTile(f.knightHealth < 0 ? "-" : String.valueOf(f.knightHealth), "Knight", "health", knight));
		boolean points = f.damage >= Battle.DAMAGE_FOR_POINTS;
		tiles.add(new StatTile(String.valueOf(f.damage), "Damage", points ? "earned" : "of " + Battle.DAMAGE_FOR_POINTS, points ? Theme.GREEN : Theme.EMBER));
		Color active = f.activity < 0 ? Theme.TEXT_MUTED : f.activity < 30 ? Theme.EMBER : f.activity < 60 ? Theme.GOLD : Theme.GREEN;
		tiles.add(new StatTile(f.activity < 0 ? "-" : f.activity + "%", "Activity", "zeal bar", active));
		stack.add(tiles);
		if (!points)
		{
			stack.add(Ui.wrapped("Deal " + (Battle.DAMAGE_FOR_POINTS - f.damage) + " more damage to be given points for this game.", Theme.small(), Theme.EMBER, WIDTH - 16));
		}

		stack.add(new SectionHeader("Shield order", f.rotations == 1 ? "known" : f.rotations + " possible"));
		stack.add(orderCard(f));
		replace(battle, stack);
	}

	private Card adviceCard(Advice a)
	{
		Color colour = a.getPortal() != null ? Theme.portal(a.getPortal())
			: a.getKind() == Advice.Kind.GUARD ? Theme.BLOOD : a.getKind() == Advice.Kind.DONE ? Theme.GREEN : Theme.GOLD;
		Card card = new Card(new BorderLayout(0, 3));
		card.accent(colour).highlighted(true);
		String kicker;
		switch (a.getKind())
		{
			case ATTACK:
				kicker = "WORK NEXT";
				break;
			case NEXT:
				kicker = "GET AHEAD";
				break;
			case GUARD:
				kicker = "KNIGHT IN DANGER";
				break;
			case DONE:
				kicker = "VICTORY";
				break;
			default:
				kicker = "WAIT";
		}
		card.add(Ui.label(kicker, Theme.small(), Theme.TEXT_MUTED), BorderLayout.NORTH);
		card.add(Ui.label(a.getHeadline(), Theme.title(), colour), BorderLayout.CENTER);
		card.add(Ui.wrapped(a.getDetail(), Theme.small(), Theme.TEXT, WIDTH - 30), BorderLayout.SOUTH);
		return card;
	}

	private Card orderCard(View.Fight f)
	{
		Card card = new Card(new BorderLayout(0, 4));
		StackPanel rows = new StackPanel(3, 0);
		List<Portal> rest = new ArrayList<>();
		for (Portal p : Portal.values())
		{
			if (!f.order.contains(p))
			{
				rest.add(p);
			}
		}
		for (int i = 0; i < f.order.size(); i++)
		{
			Portal p = f.order.get(i);
			View.PortalState s = f.portal(p);
			String state = s.fallen ? "fallen" : s.shielded ? "shielded" : "open";
			rows.add(Ui.label("<html>" + (i + 1) + ".&nbsp; <font color='" + hex(Theme.portal(p)) + "'>" + p.getTitle() + "</font>"
				+ " <font color='" + hex(Theme.TEXT_FAINT) + "'>" + p.getSide() + ", " + state + "</font></html>", Theme.small(), Theme.TEXT));
		}
		if (!rest.isEmpty())
		{
			StringBuilder names = new StringBuilder();
			for (Portal p : rest)
			{
				if (names.length() > 0)
				{
					names.append(", ");
				}
				names.append("<font color='").append(hex(Theme.portal(p))).append("'>").append(p.getTitle()).append("</font>");
			}
			String lead = f.order.isEmpty() ? "No shield down yet: " : "Still to come: ";
			rows.add(Ui.label("<html><div style='width:" + Math.round((WIDTH - 36) * 0.75f) + "px'>" + lead + names + "</div></html>", Theme.small(), Theme.TEXT_MUTED));
		}
		card.add(rows, BorderLayout.CENTER);
		return card;
	}

	private static String hex(Color c)
	{
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	// The Goal tab.

	private void rebuildGoal()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (loggedOut(stack))
		{
			replace(goal, stack);
			return;
		}
		Outlook o = view.outlook;
		stack.add(presets);

		Card progress = new Card(new BorderLayout(0, 8));
		progress.accent(o.isComplete() ? Theme.GREEN : o.isAffordable() ? Theme.TEAL : Theme.GOLD);
		Meter meter = new Meter();
		String held = o.isPointsKnown() ? Format.number(o.getPoints()) : "?";
		meter.set(o.progress(), held + " / " + Format.number(o.getCost()) + " points",
			o.getCost() == 0 ? "" : Math.round(o.progress() * 100) + "%", Theme.progressColor(o.progress()));
		progress.add(meter, BorderLayout.NORTH);
		JPanel tiles = new JPanel(new GridLayout(1, 3, 6, 0));
		tiles.setOpaque(false);
		tiles.add(new StatTile(Format.number(o.getGames()), "Games", "left", o.getGames() == 0 ? Theme.GREEN : Theme.GOLD));
		tiles.add(new StatTile(o.getGames() == 0 ? "-" : Format.span(o.getMinutes()), "Time", "about", Theme.GOLD));
		tiles.add(new StatTile(String.format(Locale.ENGLISH, o.getPerGame() % 1 == 0 ? "%.0f" : "%.1f", o.getPerGame()), "Per game",
			o.isMeasured() ? "yours" : "assumed", o.isMeasured() ? Theme.TEAL : Theme.TEXT_MUTED));
		progress.add(tiles, BorderLayout.CENTER);
		if (o.getNextBuy() != null && !o.isAffordable())
		{
			progress.add(Ui.wrapped("You can already buy the " + o.getNextBuy().getTitle().toLowerCase(Locale.ENGLISH) + ".", Theme.small(), Theme.TEAL, WIDTH - 30), BorderLayout.SOUTH);
		}
		else if (o.isOverCap())
		{
			progress.add(Ui.wrapped("The knights hold at most " + Format.number(com.codex.pestcontrol.core.Logbook.POINTS_CAP) + " points, so buy as you go.", Theme.small(), Theme.EMBER, WIDTH - 30), BorderLayout.SOUTH);
		}
		stack.add(progress);
		String faster = fasterLander();
		if (faster != null)
		{
			stack.add(Ui.wrapped(faster, Theme.small(), Theme.TEAL, WIDTH - 16));
		}

		boolean levels = !view.missing.isEmpty() && hasVoidGear();
		boolean diary = !view.elite && wantsElite();
		if (levels || diary)
		{
			Card need = new Card(new BorderLayout(0, 3));
			need.accent(Theme.BLOCKED);
			need.add(Ui.label(levels ? "Levels to wear void" : "Elite void is locked", Theme.body(), Theme.BLOCKED), BorderLayout.NORTH);
			StringBuilder text = new StringBuilder();
			if (levels)
			{
				text.append("Void gear needs ").append(String.join(", ", view.missing)).append(". ");
			}
			if (diary)
			{
				text.append("Elite pieces need the hard Western Provinces diary. ");
			}
			text.append("Points still count meanwhile.");
			need.add(Ui.wrapped(text.toString(), Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
			stack.add(need);
		}

		addMilestones(stack, o);
		addSpare(stack, o);

		stack.add(new SectionHeader("Rewards", "click to choose"));
		int spend = Math.max(0, o.getPoints());
		for (Reward r : Reward.values())
		{
			RewardRow.Standing standing;
			String rowNote = null;
			boolean chosen = view.goal.contains(r);
			if (chosen && view.owned.contains(r))
			{
				standing = RewardRow.Standing.OWNED;
			}
			else if (chosen && r.isElite() && !view.elite)
			{
				standing = RewardRow.Standing.LOCKED;
				rowNote = "diary";
			}
			else if (chosen && o.isPointsKnown() && spend >= r.getCost())
			{
				standing = RewardRow.Standing.CAN_BUY;
			}
			else if (chosen)
			{
				standing = RewardRow.Standing.IN_GOAL;
			}
			else
			{
				standing = RewardRow.Standing.NONE;
				if (view.owned.contains(r))
				{
					rowNote = "owned";
				}
			}
			stack.add(new RewardRow(r, standing, rowNote, icons, () -> actions.toggle(r)));
		}

		StringBuilder foot = new StringBuilder();
		if (o.isMeasured())
		{
			foot.append("Games left counts the points your recent games really gave, losses included");
		}
		else
		{
			foot.append("Assumes won ").append(view.boat == null ? "" : view.boat.getTitle().toLowerCase(Locale.ENGLISH) + " ")
				.append("games until three of yours are measured");
		}
		foot.append(view.ownedFrom == SharedBank.Source.BANK_STEWARD ? ". Pieces in your bank, as Codex Bank Steward last saw it, count as owned."
			: view.ownedFrom == SharedBank.Source.GEAR_ROADMAP ? ". Pieces in your bank, as Codex Gear Roadmap last saw it, count as owned."
			: view.ownedKnown ? ". Pieces in your bank count as owned." : ". Open your bank so pieces you own are not counted.");
		if (view.ownedFrom == SharedBank.Source.GEAR_ROADMAP && view.goal.contains(Reward.SEAL) && !view.owned.contains(Reward.SEAL))
		{
			// Codex Gear Roadmap saves only the gear it ranks, and a void seal is not gear to it.
			foot.append(" It does not keep void seals, so open your bank once to count yours.");
		}
		stack.add(Ui.wrapped(foot.toString(), Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
		replace(goal, stack);
	}

	/**
	 * Points the goal leaves spare, turned into the combat experience they buy at the reward shop,
	 * the most first, in bundles of 100 for the bonus.
	 */
	private void addSpare(StackPanel stack, Outlook o)
	{
		int spare = Experience.spare(o);
		if (spare < 10 || view.levels == null)
		{
			return;
		}
		stack.add(new SectionHeader("Spare points", Format.number(spare) + " into xp"));
		Card card = new Card(new BorderLayout(0, 6));
		card.accent(Theme.TEAL);
		StackPanel rows = new StackPanel(3, 0);
		for (Experience.Line line : Experience.of(view.levels, spare))
		{
			String left = line.getSkill().getTitle() + "  " + line.getLevel();
			rows.add(line.isLocked() ? line(left, "needs " + Experience.MIN_LEVEL, Theme.TEXT_FAINT)
				: line(left, "+" + Format.number(line.getXp()) + " xp", Theme.TEAL));
		}
		card.add(rows, BorderLayout.CENTER);
		stack.add(card);
		String text = o.isComplete() || o.getCost() == 0
			? "Your goal is done, so every point is spare."
			: "Points beyond what your goal still costs.";
		stack.add(Ui.wrapped(text + " Spend them 100 at a time for 10% more experience.", Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
	}

	/** The pieces still needed in the order to buy them, each with the games until it can be. */
	private void addMilestones(StackPanel stack, Outlook o)
	{
		if (o.getNeeded().size() < 2)
		{
			return;
		}
		java.util.Set<Reward> locked = java.util.EnumSet.noneOf(Reward.class);
		if (!view.elite)
		{
			for (Reward r : o.getNeeded())
			{
				if (r.isElite())
				{
					locked.add(r);
				}
			}
		}
		List<Outlook.Milestone> steps = o.milestones(locked);
		stack.add(new SectionHeader("Buy order", "as points come"));
		Card card = new Card(new BorderLayout());
		StackPanel rows = new StackPanel(0, 0);
		for (int i = 0; i < steps.size(); i++)
		{
			Outlook.Milestone m = steps.get(i);
			rows.add(new MilestoneRow(m, i == 0, i == steps.size() - 1, locked.contains(m.getReward()), icons));
		}
		card.add(rows, BorderLayout.CENTER);
		stack.add(card);
	}

	/**
	 * A line when another lander you can board has earned you clearly more points an hour than the
	 * one the estimate assumes, or null.
	 */
	private String fasterLander()
	{
		Landers.Line best = Landers.best(view.landers, view.combat);
		Landers.Line now = view.boat == null ? null : Landers.of(view.landers, view.boat);
		if (best == null || now == null || !now.isTrusted() || best.getBoat() == now.getBoat()
			|| best.pointsPerHour() < now.pointsPerHour() * 1.1)
		{
			return null;
		}
		return "Your " + best.getBoat().getTitle().toLowerCase(Locale.ENGLISH) + " games earn " + Math.round(best.pointsPerHour())
			+ " points an hour, against " + Math.round(now.pointsPerHour()) + " on " + now.getBoat().getTitle().toLowerCase(Locale.ENGLISH) + ".";
	}

	private boolean wantsElite()
	{
		for (Reward r : view.goal)
		{
			if (r.isElite() && !view.owned.contains(r))
			{
				return true;
			}
		}
		return false;
	}

	private boolean hasVoidGear()
	{
		for (Reward r : view.goal)
		{
			if (r.isVoidGear() && !view.owned.contains(r))
			{
				return true;
			}
		}
		return false;
	}

	// The Record tab.

	private void rebuildRecord()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (loggedOut(stack))
		{
			replace(record, stack);
			return;
		}
		JPanel tiles = new JPanel(new GridLayout(2, 2, 6, 6));
		tiles.setOpaque(false);
		tiles.add(new StatTile(Format.number(view.played), "Games", "played", Theme.GOLD));
		String rate = view.played == 0 ? "-" : Math.round(100.0 * view.won / view.played) + "%";
		tiles.add(new StatTile(rate, "Won", Format.number(view.won) + " games", Theme.GREEN));
		tiles.add(new StatTile(Format.number(view.earned), "Points", "earned", Theme.GOLD_BRIGHT));
		double hours = view.seconds / 3600.0;
		String perHour = hours < 0.05 ? "-" : String.valueOf(Math.round(view.earned / hours));
		tiles.add(new StatTile(perHour, "Per hour", "in games", Theme.TEAL));
		stack.add(tiles);

		List<GameRecord> games = view.games;
		if (games.isEmpty())
		{
			stack.add(note("No games yet", "Every game you play is logged here: how it ended, the points it gave, your damage and how long it ran."));
			replace(record, stack);
			return;
		}
		addSitting(stack, view.sitting);
		addDamage(stack, games);
		addLanders(stack);

		stack.add(new SectionHeader("Recent games", Format.plural(Math.min(RECENT, games.size()), "game", "games")));
		for (int i = games.size() - 1; i >= 0 && i >= games.size() - RECENT; i--)
		{
			stack.add(new GameRow(games.get(i), view.now));
		}
		replace(record, stack);
	}

	/** A small label on the left and a coloured figure on the right. */
	private static JComponent line(String left, String right, Color color)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(Ui.label(left, Theme.small(), Theme.TEXT_MUTED), BorderLayout.CENTER);
		row.add(Ui.label(right, Theme.small(), color), BorderLayout.EAST);
		return row;
	}

	/** The sitting in hand or the last one, with its points an hour counting the lander time. */
	private void addSitting(StackPanel stack, Sitting s)
	{
		if (s == null || s.isEmpty())
		{
			return;
		}
		stack.add(new SectionHeader(s.isCurrent() ? "This sitting" : "Last sitting",
			s.isCurrent() ? Format.span(s.spanMs() / 60_000.0) : Format.ago(s.getGames().get(s.getGames().size() - 1).getEnded(), view.now)));
		JPanel tiles = new JPanel(new GridLayout(1, 3, 6, 0));
		tiles.setOpaque(false);
		int count = s.getGames().size();
		tiles.add(new StatTile(Format.number(count), count == 1 ? "Game" : "Games", s.getWon() + " won", Theme.GOLD));
		tiles.add(new StatTile(Format.number(s.getPoints()), "Points", s.getUnpaid() > 0 ? s.getUnpaid() + " unpaid" : "earned", Theme.GOLD_BRIGHT));
		tiles.add(new StatTile(s.pointsPerHour() > 0 ? String.valueOf(s.pointsPerHour()) : "-", "Per hour", "waits in", Theme.TEAL));
		stack.add(tiles);
	}

	/** Games charted for damage. */
	private static final int CHARTED = 20;

	/** Your damage game by game against the 50 a game needs for points. */
	private void addDamage(StackPanel stack, List<GameRecord> games)
	{
		List<GameRecord> finished = new ArrayList<>();
		for (int i = games.size() - 1; i >= 0 && finished.size() < CHARTED; i--)
		{
			if (games.get(i).getResult() != GameRecord.Result.LEFT)
			{
				finished.add(0, games.get(i));
			}
		}
		if (finished.size() < 2)
		{
			return;
		}
		int sum = 0;
		int shortOf = 0;
		for (GameRecord g : finished)
		{
			sum += g.getDamage();
			shortOf += g.getDamage() < Battle.DAMAGE_FOR_POINTS ? 1 : 0;
		}
		stack.add(new SectionHeader("Your damage", "last " + finished.size()));
		Card card = new Card(new BorderLayout(0, 6));
		card.add(new DamageChart(finished), BorderLayout.CENTER);
		String line = "Average " + Math.round(sum / (double) finished.size()) + " a game. "
			+ (shortOf == 0 ? "Every one of these earned its points." : shortOf + " of " + finished.size() + " fell short of the 50 for points.");
		card.add(Ui.wrapped(line, Theme.small(), shortOf == 0 ? Theme.TEXT_MUTED : Theme.EMBER, WIDTH - 30), BorderLayout.SOUTH);
		stack.add(card);
	}

	/** Each lander you have played, side by side, with the one that pays best an hour lit. */
	private void addLanders(StackPanel stack)
	{
		if (view.landers.isEmpty())
		{
			return;
		}
		Landers.Line best = Landers.best(view.landers, view.combat);
		stack.add(new SectionHeader("Landers", "points an hour"));
		Card card = new Card(new BorderLayout(0, 4));
		StackPanel rows = new StackPanel(5, 0);
		for (Landers.Line l : view.landers)
		{
			boolean top = l == best && view.landers.size() > 1;
			JPanel row = new JPanel(new BorderLayout(6, 0));
			row.setOpaque(false);
			JPanel words = new JPanel(new BorderLayout(0, 1));
			words.setOpaque(false);
			words.add(Ui.label(l.getBoat().getTitle() + (top ? "  -  best" : ""), Theme.body(), top ? Theme.GOLD_BRIGHT : Theme.TEXT), BorderLayout.NORTH);
			words.add(Ui.label(Format.plural(l.getGames(), "game", "games") + ", " + Math.round(l.winRate() * 100) + "% won, "
				+ String.format(Locale.ENGLISH, "%.1f", l.getPointsPerGame()) + " a game", Theme.small(), Theme.TEXT_FAINT), BorderLayout.SOUTH);
			row.add(words, BorderLayout.CENTER);
			String rate = l.isTrusted() ? String.valueOf(Math.round(l.pointsPerHour())) : "-";
			row.add(Ui.label(rate, Theme.title(), top ? Theme.GOLD_BRIGHT : l.isTrusted() ? Theme.GOLD : Theme.TEXT_FAINT), BorderLayout.EAST);
			rows.add(row);
		}
		card.add(rows, BorderLayout.CENTER);
		card.add(Ui.wrapped("Points a game count losses; the hour counts the wait in the lander. A lander needs " + Landers.TRUSTED
			+ " games before it is rated.", Theme.small(), Theme.TEXT_FAINT, WIDTH - 30), BorderLayout.SOUTH);
		stack.add(card);
	}

	static Card note(String title, String text)
	{
		Card card = new Card(new BorderLayout(0, 4));
		card.accent(Theme.GOLD_DIM);
		card.add(Ui.label(title, Theme.body(), Theme.GOLD), BorderLayout.NORTH);
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 30), BorderLayout.CENTER);
		return card;
	}

	private static void replace(JScrollPane scroll, JComponent view)
	{
		int position = scroll.getVerticalScrollBar().getValue();
		Objects.requireNonNull(view);
		scroll.setViewportView(view);
		scroll.revalidate();
		SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(position));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.setColor(Theme.INK);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
