package com.codex.pestcontrol;

import com.codex.pestcontrol.core.Advice;
import com.codex.pestcontrol.core.Alerts;
import com.codex.pestcontrol.core.Battle;
import com.codex.pestcontrol.core.Bests;
import com.codex.pestcontrol.core.Boarding;
import com.codex.pestcontrol.core.Boat;
import com.codex.pestcontrol.core.Experience;
import com.codex.pestcontrol.core.Field;
import com.codex.pestcontrol.core.GameRecord;
import com.codex.pestcontrol.core.GameWatch;
import com.codex.pestcontrol.core.GameSummary;
import com.codex.pestcontrol.core.Goal;
import com.codex.pestcontrol.core.Landers;
import com.codex.pestcontrol.core.Logbook;
import com.codex.pestcontrol.core.NavigatorGoals;
import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Portal;
import com.codex.pestcontrol.core.Reward;
import com.codex.pestcontrol.core.Salvage;
import com.codex.pestcontrol.core.SharedBank;
import com.codex.pestcontrol.core.Sitting;
import com.codex.pestcontrol.core.Style;
import com.codex.pestcontrol.core.VoidCheck;
import com.codex.pestcontrol.overlay.MinimapMarker;
import com.codex.pestcontrol.overlay.PilotPlaque;
import com.codex.pestcontrol.overlay.PortalMarker;
import com.codex.pestcontrol.overlay.RewardShopGuide;
import com.codex.pestcontrol.ui.Actions;
import com.codex.pestcontrol.ui.Format;
import com.codex.pestcontrol.ui.ItemIcons;
import com.codex.pestcontrol.ui.LoadingIcons;
import com.codex.pestcontrol.ui.PilotPanel;
import com.codex.pestcontrol.ui.Sigil;
import com.codex.pestcontrol.ui.Theme;
import com.codex.pestcontrol.ui.View;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
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
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
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
	/** Where a save this version could not keep whole is put aside, once, before it is written over. */
	private static final String BACKUP_KEY = "logbookBackup";
	/** Past this a save is far bigger than any the pilot writes, and is not copied aside. */
	private static final int BACKUP_MAX = 1_000_000;
	/** Codex Gear Roadmap keeps the bank it last saw here (item ids); read only, and only when that plugin has saved one. */
	/** The Void Knights' Outpost island. */
	private static final int OUTPOST_REGION = 10537;
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
	private ScheduledExecutorService executor;

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

	@Inject
	private RewardShopGuide shopGuide;

	@Inject
	private Notifier notifier;

	private final PilotPlaque plaque = new PilotPlaque();
	private final Map<Portal, NPC> portals = new EnumMap<>(Portal.class);
	private final List<NPC> spinners = new ArrayList<>();
	private final List<NPC> brawlers = new ArrayList<>();

	private PilotPanel panel;
	private NavigationButton navButton;

	private String profile;
	private Logbook logbook;
	private Set<Reward> carriedOwned = EnumSet.noneOf(Reward.class);
	/** The rewards worn now, for the void check. */
	private Set<Reward> worn = EnumSet.noneOf(Reward.class);
	/** Rewards in the bank as Codex Gear Roadmap last saved it, or null when it has saved none. Used until this plugin sees the bank. */
	private SharedBank shared;
	/** Codex Navigator's goal board as saved for this profile, or null; and what its goals need, read from it. */
	private String navBoard;
	private String navKey;
	private List<NavigatorGoals.Need> navNeeds = Collections.emptyList();
	/** The rewards in {@link #shared}, worked out once per read. */
	private Set<Reward> sharedOwned = EnumSet.noneOf(Reward.class);
	private Battle battle;
	/** A game whose status bar has gone, waiting a few ticks for its closing messages. */
	private Battle ending;
	private final GameWatch watch = new GameWatch();
	private final Alerts alerts = new Alerts();
	private final Boarding boarding = new Boarding();
	/**
	 * The connection dropped and the scene is being sent again: a portal that spawns open may have
	 * dropped its shield while the connection was gone, so when is not known. Until the first tick back.
	 */
	private boolean rejoining;
	private Result result = new Result();
	private Boat landerBoat;
	private boolean inLander;
	private boolean dirty;
	private boolean unsaved;
	/** Counts logbook loads, so a slow read that an account switch or shutdown overtook is dropped. */
	private int loadGeneration;

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
		ItemIcons icons = new LoadingIcons(itemManager::getImage);
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
		overlayManager.add(shopGuide);
		clientThread.invokeLater(() ->
		{
			loadProfile();
			watch.reset();
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				carriedOwned = carried();
				worn = owned(client.getItemContainer(InventoryID.WORN));
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
		overlayManager.remove(shopGuide);
		clientToolbar.removeNavigation(navButton);
		clientThread.invokeLater(() ->
		{
			save();
			// A logbook still being read off the client thread must not come back after this.
			loadGeneration++;
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
	public void onClientShutdown(ClientShutdown event)
	{
		// ConfigManager writes its changes to disk in this same event, before a save queued on the
		// client thread runs, so a save made here has to be sent on by hand or it is never written.
		CompletableFuture<Void> done = new CompletableFuture<>();
		clientThread.invokeLater(() ->
		{
			try
			{
				if (unsaved && profile != null && logbook != null)
				{
					save();
					configManager.sendConfig();
				}
			}
			finally
			{
				done.complete(null);
			}
		});
		event.waitFor(done);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
		{
			// A dropped connection the client gets back puts you in the same game: it is held, not
			// ended, and carries on with your damage. Logging out or hopping leaves it.
			rejoining = state == GameState.CONNECTION_LOST;
			if (state != GameState.CONNECTION_LOST)
			{
				if (battle != null && logbook != null)
				{
					finish(battle, true);
				}
				else if (ending != null && logbook != null)
				{
					finish(ending, false);
				}
				battle = null;
				ending = null;
				watch.reset();
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
		if (configManager.getRSProfileKey() == null)
		{
			// Logging out can leave no profile until the next login. The account's logbook stays in
			// hand, saved, so the panel still shows it and the game under way is still recorded.
			return;
		}
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
		else if (NavigatorGoals.GROUP.equals(event.getGroup()) && NavigatorGoals.KEY.equals(event.getKey()))
		{
			clientThread.invokeLater(() ->
			{
				if (profile != null && profile.equals(configManager.getRSProfileKey()))
				{
					readNavigator();
					dirty = true;
				}
			});
		}
		else if (SharedBank.STEWARD_GROUP.equals(event.getGroup()) && SharedBank.STEWARD_KEY.equals(event.getKey())
			|| SharedBank.ROADMAP_GROUP.equals(event.getGroup()) && SharedBank.ROADMAP_KEY.equals(event.getKey()))
		{
			// Another Codex plugin saved the bank; it only counts for the profile in hand.
			clientThread.invokeLater(() ->
			{
				if (profile != null && profile.equals(configManager.getRSProfileKey()) && logbook != null)
				{
					readShared();
					dirty = true;
				}
			});
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
			if (battle != null && rejoining)
			{
				battle.foundOpen(p);
			}
			else if (battle != null)
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
			worn = owned(client.getItemContainer(InventoryID.WORN));
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
		switch (watch.tick(visible(InterfaceID.PestStatusOverlay.PEST_STATUS_PORT1)))
		{
			case START:
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
				Alerts.Alert begun = alerts.start(battle);
				// Only a game boarded from the lander is news: not one already on when the plugin started.
				if (inLander)
				{
					notifier.notify(config.notifyStart(), begun.getMessage());
				}
				break;
			case RESUME:
				battle = ending != null ? ending : new Battle();
				ending = null;
				alerts.resume(battle);
				break;
			case SETTLE:
				ending = battle;
				battle = null;
				break;
			case FINISH:
				if (ending != null)
				{
					finish(ending, false);
				}
				ending = null;
				break;
			default:
				break;
		}
		if (battle != null && portals.isEmpty())
		{
			// Back from a dropped connection, the scene was loaded afresh: find the portals again.
			for (NPC npc : client.getTopLevelWorldView().npcs())
			{
				track(npc);
			}
		}
		// The first tick back in play: the scene has been sent again, so a portal that opens from
		// now on is seen opening.
		rejoining = false;

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
			for (Alerts.Alert a : alerts.check(battle, config.knightWarn()))
			{
				notifier.notify(a.getKind() == Alerts.Kind.SHIELD ? config.notifyShield() : config.notifyKnight(), a.getMessage());
			}
			dirty = true;
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
			boolean quiet = boarding.isQuiet(client.getWorld());
			int ready = boarding.getReady();
			Widget readyLine = client.getWidget(InterfaceID.PestLanderOverlay.PEST_LANDER_OVER_PL);
			boarding.tick(readyLine == null ? null : readyLine.getText());
			dirty |= quiet != boarding.isQuiet(client.getWorld()) || ready != boarding.getReady();
		}
		else
		{
			boarding.leave();
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
		GameRecord game = new GameRecord(now, boat, outcome, earned, b.getDamage(), (int) Math.round(b.getTicks() * 0.6));
		Set<Bests.Kind> beat = logbook.record(game);
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
		Outlook after = outlook();
		summarise(game, after, beat);
		announce(before, after);
	}

	/** Real levels in the order of the skills the reward shop sells experience in. */
	private int[] levels()
	{
		Experience.Skill[] skills = Experience.Skill.values();
		int[] out = new int[skills.length];
		for (int i = 0; i < skills.length; i++)
		{
			out[i] = client.getRealSkillLevel(Skill.valueOf(skills[i].name()));
		}
		return out;
	}

	/** A line in the chat box after a game; see {@link GameSummary}. */
	private void summarise(GameRecord game, Outlook after, Set<Bests.Kind> beat)
	{
		String text = config.gameSummary() ? GameSummary.of(game, after, Sitting.latest(logbook.getGames(), game.getEnded())) : null;
		String best = config.gameSummary() ? logbook.getBests().news(beat) : null;
		if (text == null && best == null)
		{
			return;
		}
		text = best == null ? text : text == null ? best + "." : text + " " + best + ".";
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT).append("Codex Pest Control Pilot: ")
				.append(ChatColorType.NORMAL).append(text)
				.build())
			.build());
	}

	/** The goal's elite pieces while the hard Western Provinces diary still locks them. */
	private Set<Reward> locked(Outlook o)
	{
		Set<Reward> locked = EnumSet.noneOf(Reward.class);
		if (client.getVarbitValue(VarbitID.WESTERN_DIARY_HARD_COMPLETE) != 1)
		{
			for (Reward r : o.getNeeded())
			{
				if (r.isElite())
				{
					locked.add(r);
				}
			}
		}
		return locked;
	}

	private void announce(Outlook before, Outlook after)
	{
		if (!config.announce() || after == null)
		{
			return;
		}
		String text = GameSummary.announcement(before, after, locked(after));
		if (text == null)
		{
			return;
		}
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

	/**
	 * The bank another Codex plugin last saved for this profile: Codex Bank Steward's full bank,
	 * else Codex Gear Roadmap's gear, else null.
	 */
	private SharedBank readSharedBank()
	{
		return SharedBank.read(gson,
			configManager.getRSProfileConfiguration(SharedBank.STEWARD_GROUP, SharedBank.STEWARD_KEY),
			configManager.getRSProfileConfiguration(SharedBank.ROADMAP_GROUP, SharedBank.ROADMAP_KEY));
	}

	/** Reads Codex Navigator's goal board for this profile; never writes it. */
	private void readNavigator()
	{
		navBoard = profile == null ? null : configManager.getRSProfileConfiguration(NavigatorGoals.GROUP, NavigatorGoals.KEY);
		navKey = null;
	}

	/** The levels Navigator's goals need in the shop's skills, read again only when the board or a level changes. */
	private List<NavigatorGoals.Need> navigatorNeeds(int[] levels)
	{
		if (levels == null)
		{
			return Collections.emptyList();
		}
		String key = Arrays.toString(levels);
		if (!key.equals(navKey))
		{
			navKey = key;
			Map<String, Integer> byName = new LinkedHashMap<>();
			for (Experience.Skill s : Experience.Skill.values())
			{
				byName.put(s.getTitle(), levels[s.ordinal()]);
			}
			navNeeds = NavigatorGoals.needs(gson, navBoard, byName);
		}
		return navNeeds;
	}

	/** Experience in the order of the skills the reward shop sells experience in. */
	private long[] experience()
	{
		Experience.Skill[] skills = Experience.Skill.values();
		long[] out = new long[skills.length];
		for (int i = 0; i < skills.length; i++)
		{
			out[i] = client.getSkillExperience(Skill.valueOf(skills[i].name()));
		}
		return out;
	}

	private void readShared()
	{
		shared = readSharedBank();
		sharedOwned = shared == null ? EnumSet.noneOf(Reward.class) : owned(shared);
	}

	/** The rewards in a shared bank. */
	private static Set<Reward> owned(SharedBank bank)
	{
		Set<Reward> out = EnumSet.noneOf(Reward.class);
		for (int id : bank.ids())
		{
			for (Reward r : Reward.values())
			{
				if (r.isOwnedBy(id))
				{
					out.add(r);
				}
			}
		}
		return out;
	}

	private void loadProfile()
	{
		profile = configManager.getRSProfileKey();
		logbook = null;
		shared = null;
		sharedOwned = EnumSet.noneOf(Reward.class);
		int generation = ++loadGeneration;
		if (profile == null)
		{
			return;
		}
		// A damaged save keeps all it can: a field or game that cannot be read is lost alone.
		String saved = configManager.getRSProfileConfiguration(CodexPestControlConfig.GROUP, LOGBOOK_KEY);
		if (saved == null || saved.length() < Logbook.LARGE)
		{
			Logbook read = Logbook.read(gson, saved);
			loaded(profile, saved, read, Salvage.wouldLose(gson, saved, read));
		}
		else
		{
			// Far bigger than any save the pilot writes (a hand-edited one): reading it takes tens of
			// milliseconds, so it is read off the client thread and taken up on it. Until then the
			// pilot shows and records nothing, as when logged out.
			String forProfile = profile;
			executor.execute(() ->
			{
				Logbook got;
				boolean loses;
				try
				{
					got = Logbook.read(gson, saved);
					loses = Salvage.wouldLose(gson, saved, got);
				}
				catch (RuntimeException e)
				{
					log.warn("Could not read the saved logbook", e);
					got = new Logbook();
					loses = true;
				}
				Logbook read = got;
				boolean lose = loses;
				clientThread.invokeLater(() ->
				{
					// Not if the account changed, or the plugin was turned off, while it was read.
					if (generation == loadGeneration && forProfile.equals(profile))
					{
						loaded(forProfile, saved, read, lose);
						dirty = true;
					}
				});
			});
		}
		readShared();
		readNavigator();
		unsaved = false;
		dirty = true;
	}

	/**
	 * Takes up the logbook read from {@code saved}, first putting {@code saved} aside when saving
	 * over it would lose something ({@code lose}): a newer version's fields, or damage mended.
	 */
	private void loaded(String forProfile, String saved, Logbook read, boolean lose)
	{
		if (lose && saved.length() <= BACKUP_MAX)
		{
			log.warn("The saved logbook holds what this version cannot keep; a copy is kept under {}", BACKUP_KEY);
			configManager.setConfiguration(CodexPestControlConfig.GROUP, forProfile, BACKUP_KEY, saved);
		}
		logbook = read;
		unsaved = false;
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
		if (fromSharedBank())
		{
			owned.addAll(sharedOwned);
		}
		owned.addAll(carriedOwned);
		return owned;
	}

	/** Whether the owned pieces come from another Codex plugin's saved bank, this plugin not having seen the bank yet. */
	private boolean fromSharedBank()
	{
		return logbook.getOwnedSeen() == 0 && shared != null;
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
			shopGuide.show(null);
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
		Outlook outlook = outlook();
		showPlaque(state, advice, battle, outlook, Goal.matching(logbook.getGoal()), view.voidCheck);
		showMarker(advice);
		showShop(outlook);
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
			estimateBoat(), logbook.getOwnedSeen() > 0 || fromSharedBank(), missing, elite, logbook.getPlayed(), logbook.getWon(), logbook.getEarned(),
			logbook.getSeconds(), games, now);
		view.ownedFrom = fromSharedBank() ? shared.getSource() : null;
		view.landers = Landers.of(games);
		view.combat = me == null ? 0 : me.getCombatLevel();
		view.sitting = Sitting.latest(games, now);
		view.levels = me == null ? null : levels();
		view.xp = me == null ? null : experience();
		view.goals = navigatorNeeds(view.levels);
		view.voidCheck = battle == null && me != null ? VoidCheck.of(worn, style(), owned()) : null;
		view.nameOnCard = config.nameOnCard();
		view.bests = logbook.getBests().copy();
		view.knightWarn = config.knightWarn();
		view.ready = state == View.State.LANDER ? boarding.getReady() : -1;
		view.landerTip = state == View.State.LANDER && boarding.isQuiet(client.getWorld()) ? boarding.tip() : null;
		return view;
	}

	/**
	 * The plaque's line on the goal once the points are read. Points that cover elite pieces the
	 * hard diary still locks do not send the player to the shop for all of it, as the panel's
	 * crest and the void card say too.
	 */
	static String goalDetail(Outlook o, boolean elite)
	{
		if (o.isComplete())
		{
			return "Every piece is yours";
		}
		if (o.isAffordable())
		{
			boolean locked = false;
			for (Reward r : o.getNeeded())
			{
				locked |= r.isElite() && !elite;
			}
			return locked ? "Points enough; the diary is due" : "Enough for all of it: visit the reward shop";
		}
		String left = Format.number(o.getRemaining()) + " to go, about " + Format.plural(o.getGames(), "game", "games");
		Set<Reward> locked = EnumSet.noneOf(Reward.class);
		for (Reward r : o.getNeeded())
		{
			if (r.isElite() && !elite)
			{
				locked.add(r);
			}
		}
		Set<Reward> buyable = o.buyableNow(locked);
		// Part of the goal is already yours to buy: worth a trip to the shop before the next game.
		return buyable.size() == 1 ? "buy the " + buyable.iterator().next().getShortTitle().toLowerCase(Locale.ENGLISH) + " now; " + left
			: buyable.size() > 1 ? "buy " + buyable.size() + " pieces now; " + left : left;
	}

	private void showPlaque(View.State state, Advice advice, Battle b, Outlook o, Goal preset, VoidCheck voidCheck)
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
		if (voidCheck != null && voidCheck.isWarning() && config.voidCheck())
		{
			// Still time to fix it before the game: that matters more than the points tally.
			plaque.show(new PilotPlaque.Shown("VOID CHECK", voidCheck.getHeadline(), Theme.EMBER, voidCheck.getDetail(), null, null));
			return;
		}
		if (state == View.State.LANDER && boarding.isQuiet(client.getWorld()))
		{
			plaque.show(new PilotPlaque.Shown("QUIET LANDER", "Try world " + Boarding.PEST_CONTROL_WORLD, Theme.GOLD_BRIGHT, boarding.tip(), null, null));
			return;
		}
		String goal = preset != null ? preset.getTitle() : "Your goal";
		if (o == null || !o.isPointsKnown())
		{
			plaque.show(new PilotPlaque.Shown("PEST CONTROL", goal, Theme.GOLD, "Board a lander to read your points", null, null));
			return;
		}
		String detail = goalDetail(o, client.getVarbitValue(VarbitID.WESTERN_DIARY_HARD_COMPLETE) == 1);
		plaque.show(new PilotPlaque.Shown("PEST CONTROL", Format.number(o.getPoints()) + " points", Theme.GOLD_BRIGHT,
			goal + ": " + detail, null, null));
	}

	private void showShop(Outlook o)
	{
		if (!config.markShop() || o == null || !o.isPointsKnown() || o.isComplete())
		{
			shopGuide.show(null);
			return;
		}
		Set<Reward> locked = locked(o);
		Set<Reward> goal = EnumSet.noneOf(Reward.class);
		goal.addAll(o.getNeeded());
		goal.removeAll(locked);
		shopGuide.show(new RewardShopGuide.Guide(o.buyableNow(locked), goal));
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
