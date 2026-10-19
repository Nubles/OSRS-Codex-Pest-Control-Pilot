package com.codex.pestcontrol;

import com.codex.pestcontrol.core.Advice;
import com.codex.pestcontrol.core.Battle;
import com.codex.pestcontrol.core.Boat;
import com.codex.pestcontrol.core.Field;
import com.codex.pestcontrol.core.GameRecord;
import com.codex.pestcontrol.core.Goal;
import com.codex.pestcontrol.core.Landers;
import com.codex.pestcontrol.core.Logbook;
import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Portal;
import com.codex.pestcontrol.core.Reward;
import com.codex.pestcontrol.core.Style;
import com.codex.pestcontrol.overlay.MinimapMarker;
import com.codex.pestcontrol.overlay.PilotPlaque;
import com.codex.pestcontrol.overlay.PortalMarker;
import com.codex.pestcontrol.ui.Actions;
import com.codex.pestcontrol.ui.Format;
import com.codex.pestcontrol.ui.ItemIcons;
import com.codex.pestcontrol.ui.PilotPanel;
import com.codex.pestcontrol.ui.Sigil;
import com.codex.pestcontrol.ui.Theme;
import com.codex.pestcontrol.ui.View;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Codex Pest Control Pilot",
	description = "Tells you which Pest Control portal to work next, marks it in the world and counts your points toward the void piece you choose",
	tags = {"pest control", "void", "portal", "minigame", "points", "commendation", "codex"}
)
public class CodexPestControlPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CodexPestControlPlugin.class);
	private static final String LOGBOOK_KEY = "logbook";
	/** Codex Gear Roadmap keeps the bank it last saw here (item ids); read only, and only when that plugin has saved one. */
	private static final String GEAR_ROADMAP_GROUP = "codexgearroadmap";
	private static final String GEAR_ROADMAP_BANK = "bank";
	/** The Void Knights' Outpost island. */
	private static final int OUTPOST_REGION = 10537;
	/** Ticks after a game's status bar goes for its closing messages to arrive. */
	private static final int SETTLE_TICKS = 4;
	/** A spinner this close to a portal is healing it. */
	private static final int SPINNER_RANGE = 8;
	/** A brawler this close to a portal is in the way. */
	private static final int BRAWLER_RANGE = 6;
	/** A player this close to a portal is working it. */
	private static final int CROWD_RANGE = 10;

	private static final Pattern SHIELD = Pattern.compile("The ([a-z]+), [^ ]+ portal shield has dropped!", Pattern.CASE_INSENSITIVE);
	private static final Pattern COMMENDATION = Pattern.compile("([0-9,]+) (?:Void Knight )?Commendation points?", Pattern.CASE_INSENSITIVE);
	private static final Pattern NOW_HAVE = Pattern.compile("now have ([0-9,]+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern NUMBER = Pattern.compile("([0-9,]+)");

	private static final int[] PORTAL_TEXT = {
		InterfaceID.PestStatusOverlay.PEST_STATUS_PORTTXT1, InterfaceID.PestStatusOverlay.PEST_STATUS_PORTTXT2,
		InterfaceID.PestStatusOverlay.PEST_STATUS_PORTTXT3, InterfaceID.PestStatusOverlay.PEST_STATUS_PORTTXT4
	};
	private static final Map<Integer, Integer> OPEN = new HashMap<>();
	private static final Map<Integer, Integer> SHIELDED = new HashMap<>();
	private static final Set<Integer> SPINNERS = new java.util.HashSet<>();
	private static final Set<Integer> BRAWLERS = new java.util.HashSet<>();
	private static final Skill[] VOID_SKILLS = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC};

	static
	{
		int[] open = {NpcID.PEST_PORTAL_1_ACTIVE, NpcID.PEST_PORTAL_2_ACTIVE, NpcID.PEST_PORTAL_3_ACTIVE, NpcID.PEST_PORTAL_4_ACTIVE};
		int[] openBt = {NpcID.PEST_PORTAL_1_ACTIVE_BT1, NpcID.PEST_PORTAL_2_ACTIVE_BT1, NpcID.PEST_PORTAL_3_ACTIVE_BT1, NpcID.PEST_PORTAL_4_ACTIVE_BT1};
		int[] shut = {NpcID.PEST_PORTAL_SHIELD_1_ACTIVE, NpcID.PEST_PORTAL_SHIELD_2_ACTIVE, NpcID.PEST_PORTAL_SHIELD_3_ACTIVE, NpcID.PEST_PORTAL_SHIELD_4_ACTIVE};
		int[] shutBt = {NpcID.PEST_PORTAL_SHIELD_1_ACTIVE_BT1, NpcID.PEST_PORTAL_SHIELD_2_ACTIVE_BT1, NpcID.PEST_PORTAL_SHIELD_3_ACTIVE_BT1, NpcID.PEST_PORTAL_SHIELD_4_ACTIVE_BT1};
		for (int i = 0; i < 4; i++)
		{
			OPEN.put(open[i], i);
			OPEN.put(openBt[i], i);
			SHIELDED.put(shut[i], i);
			SHIELDED.put(shutBt[i], i);
		}
		SPINNERS.add(NpcID.PEST_SPINNER_1);
		SPINNERS.add(NpcID.PEST_SPINNER_2);
		SPINNERS.add(NpcID.PEST_SPINNER_3);
		SPINNERS.add(NpcID.PEST_SPINNER_4);
		SPINNERS.add(NpcID.PEST_SPINNER_5);
		BRAWLERS.add(NpcID.PEST_BRAWLER_1);
		BRAWLERS.add(NpcID.PEST_BRAWLER_2);
		BRAWLERS.add(NpcID.PEST_BRAWLER_3);
		BRAWLERS.add(NpcID.PEST_BRAWLER_4);
		BRAWLERS.add(NpcID.PEST_BRAWLER_5);
	}

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Gson gson;

	@Inject
	private CodexPestControlConfig config;

	@Inject
	private PortalMarker marker;

	@Inject
	private MinimapMarker minimap;

	private final PilotPlaque plaque = new PilotPlaque();
	private final Map<Portal, NPC> portals = new EnumMap<>(Portal.class);
	private final List<NPC> spinners = new ArrayList<>();
	private final List<NPC> brawlers = new ArrayList<>();

	private PilotPanel panel;
	private NavigationButton navButton;

	private String profile;
	private Logbook logbook;
	private Set<Reward> carriedOwned = EnumSet.noneOf(Reward.class);
	/** Rewards in the bank as Codex Gear Roadmap last saved it, or null when it has saved none. Used until this plugin sees the bank. */
	private Set<Reward> roadmapOwned;
	private Battle battle;
	/** A game whose status bar has gone, waiting a few ticks for its closing messages. */
	private Battle ending;
	private int endingTicks;
	private Result result = new Result();
	private Boat landerBoat;
	private boolean inLander;
	private boolean dirty;
	private boolean unsaved;

	/** What a game's closing messages said. */
	private static final class Result
	{
		boolean lost;
		boolean noZeal;
		int earned = -1;
		int total = -1;
	}

	@Provides
	CodexPestControlConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexPestControlConfig.class);
	}

	@Override
	protected void startUp()
	{
		ItemIcons icons = (itemId, onLoaded) ->
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			image.onLoaded(onLoaded);
			return image;
		};
		panel = new PilotPanel(icons, new PanelActions());
		navButton = NavigationButton.builder()
			.tooltip("Codex Pest Control Pilot")
			.icon(Sigil.icon(16))
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(plaque);
		overlayManager.add(marker);
		overlayManager.add(minimap);
		clientThread.invokeLater(() ->
		{
			loadProfile();
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				carriedOwned = carried();
				for (NPC npc : client.getTopLevelWorldView().npcs())
				{
					track(npc);
				}
			}
			publish();
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(plaque);
		overlayManager.remove(marker);
		overlayManager.remove(minimap);
		clientToolbar.removeNavigation(navButton);
		clientThread.invokeLater(() ->
		{
			save();
			logbook = null;
			profile = null;
			battle = null;
			ending = null;
			portals.clear();
			spinners.clear();
			brawlers.clear();
		});
		panel = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
		{
			if (battle != null)
			{
				finish(battle, true);
				battle = null;
			}
			portals.clear();
			spinners.clear();
			brawlers.clear();
			inLander = false;
			save();
			publish();
		}
		else if (state == GameState.LOADING)
		{
			// A new area: every NPC the client knew is gone and the new ones spawn afresh.
			portals.clear();
			spinners.clear();
			brawlers.clear();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		save();
		loadProfile();
		carriedOwned = client.getGameState() == GameState.LOGGED_IN ? carried() : EnumSet.noneOf(Reward.class);
		publish();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CodexPestControlConfig.GROUP.equals(event.getGroup()))
		{
			dirty = true;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		track(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		track(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		spinners.remove(npc);
		brawlers.remove(npc);
		portals.values().removeIf(p -> p == npc);
	}

	private void track(NPC npc)
	{
		int id = npc.getId();
		if (SPINNERS.contains(id))
		{
			if (!spinners.contains(npc))
			{
				spinners.add(npc);
			}
			return;
		}
		if (BRAWLERS.contains(id))
		{
			if (!brawlers.contains(npc))
			{
				brawlers.add(npc);
			}
			return;
		}
		Integer open = OPEN.get(id);
		Integer shut = SHIELDED.get(id);
		if (open != null)
		{
			Portal p = Portal.values()[open];
			portals.put(p, npc);
			if (battle != null)
			{
				battle.shieldDropped(p);
			}
		}
		else if (shut != null)
		{
			portals.put(Portal.values()[shut], npc);
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (battle != null && event.getActor() instanceof NPC && event.getHitsplat().isMine())
		{
			Portal on = null;
			for (Map.Entry<Portal, NPC> e : portals.entrySet())
			{
				if (e.getValue() == event.getActor())
				{
					on = e.getKey();
				}
			}
			battle.addDamage(event.getHitsplat().getAmount(), on);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		String text = event.getMessage().replaceAll("<[^>]*>", "");
		Matcher shield = SHIELD.matcher(text);
		if (shield.lookingAt())
		{
			if (battle != null)
			{
				battle.shieldDropped(Portal.named(shield.group(1)));
				dirty = true;
			}
			return;
		}
		if (battle == null && ending == null)
		{
			return;
		}
		String lower = text.toLowerCase(Locale.ENGLISH);
		if (lower.contains("void knight was killed") || lower.contains("island is lost"))
		{
			result.lost = true;
		}
		if (lower.contains("lack of zeal"))
		{
			result.noZeal = true;
		}
		Matcher total = NOW_HAVE.matcher(text);
		if (total.find())
		{
			result.total = parse(total.group(1));
		}
		Matcher points = COMMENDATION.matcher(text);
		if (points.find() && !text.substring(0, points.start()).toLowerCase(Locale.ENGLISH).endsWith("now have "))
		{
			result.earned = parse(points.group(1));
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (logbook == null)
		{
			return;
		}
		int id = event.getContainerId();
		if (id == InventoryID.BANK)
		{
			if (logbook.seeOwned(owned(event.getItemContainer()), System.currentTimeMillis()))
			{
				unsaved = true;
			}
			dirty = true;
		}
		else if (id == InventoryID.INV || id == InventoryID.WORN)
		{
			carriedOwned = carried();
			dirty = true;
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (logbook == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		boolean inGame = visible(InterfaceID.PestStatusOverlay.PEST_STATUS_PORT1);
		if (inGame && battle == null)
		{
			result = new Result();
			ending = null;
			for (NPC npc : client.getTopLevelWorldView().npcs())
			{
				track(npc);
			}
			battle = new Battle();
			// Portals that spawned before the game was seen may already be open; when is not known.
			for (Map.Entry<Portal, NPC> e : portals.entrySet())
			{
				if (OPEN.containsKey(e.getValue().getId()))
				{
					battle.foundOpen(e.getKey());
				}
			}
		}
		else if (!inGame && battle != null)
		{
			ending = battle;
			endingTicks = 0;
			battle = null;
		}

		if (battle != null)
		{
			battle.tick();
			for (Portal p : Portal.values())
			{
				int hp = number(PORTAL_TEXT[p.ordinal()]);
				if (hp >= 0)
				{
					battle.setHealth(p, hp);
				}
			}
			int knight = number(InterfaceID.PestStatusOverlay.PEST_STATUS_OVER_HEALTH);
			if (knight >= 0)
			{
				battle.setKnightHealth(knight);
			}
			dirty = true;
		}

		if (ending != null && ++endingTicks >= SETTLE_TICKS)
		{
			finish(ending, false);
			ending = null;
		}

		boolean lander = visible(InterfaceID.PestLanderOverlay.PEST_LANDER_OVER_POINTS);
		if (lander)
		{
			int points = number(InterfaceID.PestLanderOverlay.PEST_LANDER_OVER_POINTS);
			if (logbook.seePoints(points, System.currentTimeMillis()))
			{
				unsaved = true;
			}
			Boat boat = landerBoat();
			if (boat != null)
			{
				landerBoat = boat;
			}
		}
		if (lander != inLander)
		{
			inLander = lander;
			dirty = true;
		}
		if (unsaved && battle == null)
		{
			save();
		}
		if (dirty)
		{
			publish();
		}
	}

	/** Logs a finished game, updates the points and says when a piece of the goal is now affordable. */
	private void finish(Battle b, boolean left)
	{
		Result r = result;
		result = new Result();
		boolean allFallen = true;
		for (Portal p : Portal.values())
		{
			allFallen &= b.isFallen(p);
		}
		GameRecord.Result outcome;
		if (left && r.earned < 0)
		{
			outcome = GameRecord.Result.LEFT;
		}
		else if (r.lost)
		{
			outcome = GameRecord.Result.LOST;
		}
		else if (r.noZeal)
		{
			outcome = GameRecord.Result.NO_POINTS;
		}
		else if (r.earned > 0 || allFallen)
		{
			outcome = GameRecord.Result.WON;
		}
		else
		{
			outcome = GameRecord.Result.LEFT;
		}
		int earned = Math.max(0, r.earned);
		Boat boat = Boat.forPoints(earned);
		if (boat == null)
		{
			boat = landerBoat;
		}
		if (outcome == GameRecord.Result.WON && earned == 0 && boat != null)
		{
			// The points message was missed; a won game gives the lander's points.
			earned = boat.getPoints();
		}

		if (outcome != GameRecord.Result.LEFT)
		{
			logbook.getClock().learn(b.getDropTicks());
		}
		Outlook before = outlook();
		long now = System.currentTimeMillis();
		logbook.record(new GameRecord(now, boat, outcome, earned, b.getDamage(), (int) Math.round(b.getTicks() * 0.6)));
		if (r.total >= 0)
		{
			logbook.seePoints(r.total, now);
		}
		else if (earned > 0 && logbook.getPoints() >= 0)
		{
			logbook.seePoints(logbook.getPoints() + earned, now);
		}
		unsaved = true;
		dirty = true;
		announce(before, outlook());
	}

	private void announce(Outlook before, Outlook after)
	{
		if (!config.announce() || before == null || after == null || !after.isPointsKnown())
		{
			return;
		}
		Reward now = after.getNextBuy();
		Reward was = before.getNextBuy();
		if (now == null || was == now && before.isAffordable() == after.isAffordable())
		{
			return;
		}
		String text = after.isAffordable()
			? "your " + Format.number(after.getPoints()) + " points now cover everything left in your goal. Visit the Void Knights' reward shop."
			: "your " + Format.number(after.getPoints()) + " points now cover the " + now.getTitle().toLowerCase(Locale.ENGLISH) + ".";
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT).append("Codex Pest Control Pilot: ")
				.append(ChatColorType.NORMAL).append(text)
				.build())
			.build());
	}

	private Boat landerBoat()
	{
		Widget w = client.getWidget(InterfaceID.PestLanderOverlay.PEST_LANDER_OVER_LEVEL);
		if (w == null || w.getText() == null)
		{
			return null;
		}
		String t = w.getText().toLowerCase(Locale.ENGLISH);
		if (t.contains("novice"))
		{
			return Boat.NOVICE;
		}
		if (t.contains("intermediate"))
		{
			return Boat.INTERMEDIATE;
		}
		if (t.contains("veteran"))
		{
			return Boat.VETERAN;
		}
		return null;
	}

	private boolean visible(int component)
	{
		Widget w = client.getWidget(component);
		return w != null && !w.isHidden();
	}

	/** The first number in a widget's text, or -1. */
	private int number(int component)
	{
		Widget w = client.getWidget(component);
		if (w == null || w.getText() == null)
		{
			return -1;
		}
		Matcher m = NUMBER.matcher(w.getText());
		return m.find() ? parse(m.group(1)) : -1;
	}

	private static int parse(String digits)
	{
		try
		{
			return Integer.parseInt(digits.replace(",", ""));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	private Set<Reward> owned(ItemContainer container)
	{
		Set<Reward> out = EnumSet.noneOf(Reward.class);
		if (container == null)
		{
			return out;
		}
		for (Item item : container.getItems())
		{
			for (Reward r : Reward.values())
			{
				if (r.isOwnedBy(item.getId()))
				{
					out.add(r);
				}
			}
		}
		return out;
	}

	private Set<Reward> carried()
	{
		Set<Reward> out = owned(client.getItemContainer(InventoryID.INV));
		out.addAll(owned(client.getItemContainer(InventoryID.WORN)));
		return out;
	}

	/** The shape Codex Gear Roadmap saves its bank in; only the item ids are read. */
	private static final class SavedBank
	{
		List<Integer> items;
	}

	/** The void pieces in Codex Gear Roadmap's saved bank, or null when that plugin is not installed or has not seen the bank. */
	private Set<Reward> readRoadmapBank()
	{
		try
		{
			String json = configManager.getRSProfileConfiguration(GEAR_ROADMAP_GROUP, GEAR_ROADMAP_BANK);
			if (json == null || json.isEmpty())
			{
				return null;
			}
			SavedBank bank = gson.fromJson(json, SavedBank.class);
			if (bank == null || bank.items == null)
			{
				return null;
			}
			Set<Reward> out = EnumSet.noneOf(Reward.class);
			for (Integer id : bank.items)
			{
				for (Reward r : Reward.values())
				{
					if (id != null && r.isOwnedBy(id))
					{
						out.add(r);
					}
				}
			}
			return out;
		}
		catch (JsonParseException e)
		{
			return null;
		}
	}

	private void loadProfile()
	{
		profile = configManager.getRSProfileKey();
		logbook = null;
		roadmapOwned = null;
		if (profile == null)
		{
			return;
		}
		try
		{
			String saved = configManager.getRSProfileConfiguration(CodexPestControlConfig.GROUP, LOGBOOK_KEY);
			if (saved != null && !saved.isEmpty())
			{
				Logbook read = gson.fromJson(saved, Logbook.class);
				logbook = read == null ? null : read.repair();
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read the saved logbook, starting afresh", e);
		}
		if (logbook == null)
		{
			logbook = new Logbook();
		}
		roadmapOwned = readRoadmapBank();
		unsaved = false;
		dirty = true;
	}

	private void save()
	{
		if (profile == null || logbook == null || !unsaved)
		{
			return;
		}
		unsaved = false;
		configManager.setConfiguration(CodexPestControlConfig.GROUP, profile, LOGBOOK_KEY, gson.toJson(logbook));
	}

	private Set<Reward> owned()
	{
		Set<Reward> owned = logbook.getOwned();
		if (fromRoadmap())
		{
			owned.addAll(roadmapOwned);
		}
		owned.addAll(carriedOwned);
		return owned;
	}

	/** Whether the owned pieces come from Codex Gear Roadmap's bank, this plugin not having seen the bank yet. */
	private boolean fromRoadmap()
	{
		return logbook.getOwnedSeen() == 0 && roadmapOwned != null;
	}

	private Boat estimateBoat()
	{
		switch (config.lander())
		{
			case NOVICE:
				return Boat.NOVICE;
			case INTERMEDIATE:
				return Boat.INTERMEDIATE;
			case VETERAN:
				return Boat.VETERAN;
			default:
				List<GameRecord> games = logbook.getGames();
				for (int i = games.size() - 1; i >= 0; i--)
				{
					if (games.get(i).getBoat() != null)
					{
						return games.get(i).getBoat();
					}
				}
				Player me = client.getLocalPlayer();
				return me == null ? Boat.NOVICE : Boat.forCombat(me.getCombatLevel());
		}
	}

	private Outlook outlook()
	{
		if (logbook == null)
		{
			return null;
		}
		return Outlook.of(logbook.getGoal(), owned(), logbook.getPoints(), logbook.pointsPerGame(estimateBoat()),
			logbook.minutesPerGame(), logbook.isMeasured());
	}

	/** Works out the advice and hands the panel, the plaque and the markers what they show. */
	private void publish()
	{
		dirty = false;
		PilotPanel target = panel;
		long now = System.currentTimeMillis();
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		if (logbook == null || !loggedIn)
		{
			plaque.show(null);
			marker.show(null);
			if (target != null)
			{
				View view = logbook == null ? View.loggedOut() : view(View.State.LOGGED_OUT, null, now);
				SwingUtilities.invokeLater(() -> target.update(view));
			}
			return;
		}

		Player me = client.getLocalPlayer();
		View.Fight fight = null;
		Advice advice = null;
		if (battle != null)
		{
			Field field = field(me);
			advice = battle.advise(field);
			List<View.PortalState> states = new ArrayList<>();
			for (Portal p : Portal.values())
			{
				Integer crowd = field.crowdAt(p);
				states.add(new View.PortalState(p, battle.isShielded(p), battle.isFallen(p), battle.getHealth(p), crowd == null ? -1 : crowd,
					battle.secondsToFall(p), battle.isYoursAlone(p)));
			}
			fight = new View.Fight(states, advice, battle.getKnightHealth(), battle.getDamage(), battle.expectedOrder(),
				battle.getPossible().size(), (int) Math.round(battle.getTicks() * 0.6), activity(), battle.secondsToNextDrop(logbook.getClock()));
		}

		boolean outpost = me != null && me.getWorldLocation().getRegionID() == OUTPOST_REGION;
		View.State state = battle != null ? View.State.BATTLE : inLander ? View.State.LANDER : outpost ? View.State.OUTPOST : View.State.AWAY;
		View view = view(state, fight, now);
		showPlaque(state, advice, battle, outlook(), Goal.matching(logbook.getGoal()));
		showMarker(advice);
		if (target != null)
		{
			SwingUtilities.invokeLater(() -> target.update(view));
		}
	}

	/** What can be seen around the island now: distances, pests and players at each portal, and your style. */
	private Field field(Player me)
	{
		Field field = new Field()
			.knightWarn(config.knightWarn())
			.activity(activity())
			.nextDrop(battle.secondsToNextDrop(logbook.getClock()))
			.style(style());
		WorldPoint here = me == null ? null : me.getWorldLocation();
		for (Map.Entry<Portal, NPC> e : portals.entrySet())
		{
			Portal p = e.getKey();
			WorldPoint at = e.getValue().getWorldLocation();
			if (here != null)
			{
				field.distance(p, here.distanceTo(at));
			}
			field.spinners(p, near(spinners, at, SPINNER_RANGE));
			field.brawlers(p, near(brawlers, at, BRAWLER_RANGE));
			int crowd = 0;
			for (Player other : client.getTopLevelWorldView().players())
			{
				if (other != me && other.getWorldLocation().distanceTo(at) <= CROWD_RANGE)
				{
					crowd++;
				}
			}
			field.crowd(p, crowd);
		}
		return field;
	}

	private static int near(List<NPC> npcs, WorldPoint at, int range)
	{
		int n = 0;
		for (NPC npc : npcs)
		{
			if (npc.getWorldLocation().distanceTo(at) <= range)
			{
				n++;
			}
		}
		return n;
	}

	/** Your style from the weapon you hold. */
	private Style style()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Item weapon = worn == null ? null : worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (weapon == null || weapon.getId() <= 0)
		{
			return Style.MELEE;
		}
		return Style.ofWeapon(itemManager.getItemComposition(weapon.getId()).getName());
	}

	/** The game's activity bar as a percentage, or -1 when it cannot be read. */
	private int activity()
	{
		Widget container = client.getWidget(InterfaceID.PestStatusOverlay.ACTIVITY_CONTAINER);
		Widget bar = client.getWidget(InterfaceID.PestStatusOverlay.ACTIVITY_BAR);
		Widget track = container == null ? null : container.getChild(0);
		Widget fill = bar == null ? null : bar.getChild(0);
		if (track == null || fill == null || track.getWidth() <= 0)
		{
			return -1;
		}
		return (int) Math.max(0, Math.min(100, Math.round(100.0 * fill.getWidth() / track.getWidth())));
	}

	private View view(View.State state, View.Fight fight, long now)
	{
		Outlook outlook = outlook();
		Set<Reward> goal = logbook.getGoal();
		List<String> missing = new ArrayList<>();
		boolean elite = false;
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			for (Skill s : VOID_SKILLS)
			{
				if (client.getRealSkillLevel(s) < 42)
				{
					missing.add("42 " + s.getName());
				}
			}
			if (client.getRealSkillLevel(Skill.PRAYER) < 22)
			{
				missing.add("22 Prayer");
			}
			elite = client.getVarbitValue(VarbitID.WESTERN_DIARY_HARD_COMPLETE) == 1;
		}
		List<GameRecord> games = new ArrayList<>(logbook.getGames());
		GameRecord last = games.isEmpty() ? null : games.get(games.size() - 1);
		Player me = client.getLocalPlayer();
		View view = new View(state, me == null ? null : me.getName(), fight, last, outlook, goal, owned(), Goal.matching(goal),
			estimateBoat(), logbook.getOwnedSeen() > 0 || fromRoadmap(), missing, elite, logbook.getPlayed(), logbook.getWon(), logbook.getEarned(),
			logbook.getSeconds(), games, now);
		view.ownedFromRoadmap = fromRoadmap();
		view.landers = Landers.of(games);
		view.combat = me == null ? 0 : me.getCombatLevel();
		return view;
	}

	private void showPlaque(View.State state, Advice advice, Battle b, Outlook o, Goal preset)
	{
		CodexPestControlConfig.PlaqueMode mode = config.plaque();
		if (mode == CodexPestControlConfig.PlaqueMode.NEVER || state == View.State.AWAY
			|| state != View.State.BATTLE && mode != CodexPestControlConfig.PlaqueMode.AT_THE_OUTPOST)
		{
			plaque.show(null);
			return;
		}
		if (advice != null)
		{
			Color colour = advice.getPortal() != null ? Theme.portal(advice.getPortal())
				: advice.getKind() == Advice.Kind.GUARD ? Theme.BLOOD : Theme.GOLD;
			String kicker = advice.getKind() == Advice.Kind.ATTACK ? "WORK NEXT" : advice.getKind() == Advice.Kind.NEXT ? "GET AHEAD"
				: advice.getKind() == Advice.Kind.GUARD ? "KNIGHT IN DANGER" : advice.getKind() == Advice.Kind.DONE ? "VICTORY" : "WAIT";
			String foot;
			Color footColor;
			if (b.hasPoints())
			{
				foot = b.getDamage() + " damage: points earned";
				footColor = Theme.GREEN;
			}
			else
			{
				foot = b.getDamage() + " of " + Battle.DAMAGE_FOR_POINTS + " damage for points";
				footColor = Theme.EMBER;
			}
			plaque.show(new PilotPlaque.Shown(kicker, advice.getHeadline(), colour, advice.getDetail(), foot, footColor));
			return;
		}
		String goal = preset != null ? preset.getTitle() : "Your goal";
		if (o == null || !o.isPointsKnown())
		{
			plaque.show(new PilotPlaque.Shown("PEST CONTROL", goal, Theme.GOLD, "Board a lander to read your points", null, null));
			return;
		}
		String detail = o.isComplete() ? "Every piece is yours"
			: o.isAffordable() ? "Enough for all of it: visit the reward shop"
			: Format.number(o.getRemaining()) + " to go, about " + Format.plural(o.getGames(), "game", "games");
		plaque.show(new PilotPlaque.Shown("PEST CONTROL", Format.number(o.getPoints()) + " points", Theme.GOLD_BRIGHT,
			goal + ": " + detail, null, null));
	}

	private void showMarker(Advice advice)
	{
		if (advice == null || !config.markPortal())
		{
			marker.show(null);
			return;
		}
		NPC target = advice.getPortal() == null ? null : portals.get(advice.getPortal());
		List<NPC> maybe = new ArrayList<>();
		List<Color> colours = new ArrayList<>();
		if (advice.getKind() == Advice.Kind.WAIT && config.markCandidates())
		{
			for (Portal p : advice.getCandidates())
			{
				NPC npc = portals.get(p);
				if (npc != null)
				{
					maybe.add(npc);
					colours.add(Theme.portal(p));
				}
			}
		}
		if (target == null && maybe.isEmpty())
		{
			marker.show(null);
			return;
		}
		String label = advice.getKind() == Advice.Kind.ATTACK ? "Attack" : "Next";
		marker.show(new PortalMarker.Mark(target, target == null ? Theme.GOLD : Theme.portal(advice.getPortal()), label, maybe, colours));
	}

	/** The panel's buttons; each change is made on the client thread, where the logbook lives. */
	private final class PanelActions implements Actions
	{
		@Override
		public void preset(Goal goal)
		{
			clientThread.invokeLater(() ->
			{
				if (logbook != null)
				{
					logbook.setGoal(EnumSet.copyOf(goal.getRewards()));
					unsaved = true;
					save();
					publish();
				}
			});
		}

		@Override
		public void toggle(Reward reward)
		{
			clientThread.invokeLater(() ->
			{
				if (logbook != null)
				{
					logbook.toggle(reward);
					unsaved = true;
					save();
					publish();
				}
			});
		}
	}
}
