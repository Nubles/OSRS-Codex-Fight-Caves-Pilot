package com.codex.fightcaves;

import com.codex.fightcaves.core.Banked;
import com.codex.fightcaves.core.CaveMap;
import com.codex.fightcaves.core.Chase;
import com.codex.fightcaves.core.Compare;
import com.codex.fightcaves.core.Damage;
import com.codex.fightcaves.core.EarlyCall;
import com.codex.fightcaves.core.Fall;
import com.codex.fightcaves.core.Grid;
import com.codex.fightcaves.core.KillOrder;
import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.Monster;
import com.codex.fightcaves.core.Outlook;
import com.codex.fightcaves.core.Placement;
import com.codex.fightcaves.core.PrayerCall;
import com.codex.fightcaves.core.PrayerPacer;
import com.codex.fightcaves.core.Presence;
import com.codex.fightcaves.core.Rehearsal;
import com.codex.fightcaves.core.Protect;
import com.codex.fightcaves.core.Ready;
import com.codex.fightcaves.core.RotationTracker;
import com.codex.fightcaves.core.Spawn;
import com.codex.fightcaves.core.Splits;
import com.codex.fightcaves.core.StandPlanner;
import com.codex.fightcaves.core.Threat;
import com.codex.fightcaves.core.Waves;
import com.codex.fightcaves.core.WeaponRange;
import com.codex.fightcaves.overlay.CaveOverlay;
import com.codex.fightcaves.overlay.Marks;
import com.codex.fightcaves.overlay.PilotPlaque;
import com.codex.fightcaves.ui.Format;
import com.codex.fightcaves.ui.PilotPanel;
import com.codex.fightcaves.ui.Sigil;
import com.codex.fightcaves.ui.Theme;
import com.codex.fightcaves.ui.View;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
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
	name = "Codex Fight Caves Pilot",
	description = "Reads your Fight Cave rotation from the first spawns, marks where to stand for each wave and paces your prayer potions against the waves left",
	tags = {"fight caves", "fight cave", "tzhaar", "jad", "fire cape", "rotation", "waves", "safespot", "prayer", "codex"}
)
public class CodexFightCavesPilotPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CodexFightCavesPilotPlugin.class);
	static final int FIGHT_CAVE_REGION = 9551;
	private static final String LOGBOOK_KEY = "logbook";
	private static final Pattern WAVE = Pattern.compile("Wave: (\\d+)");
	private static final Pattern KILL_COUNT = Pattern.compile("Your TzTok-Jad kill count is");
	private static final Pattern TAGS = Pattern.compile("<[^>]*>");
	/** Ticks to wait after the wave message for all of the wave's spawns to arrive. */
	private static final int SPAWN_GRACE = 3;
	/** Ticks between saves while something has changed. */
	private static final int SAVE_TICKS = 100;
	/** Ticks between re-working where to stand while a wave is on. */
	private static final int PLAN_TICKS = 2;
	/** Ticks to wait, after leaving the cave on the Jad wave, for the kill count that says it was a cape. */
	private static final int CAPE_GRACE = 10;
	/** How much safer a new tile has to be before the gilt tile moves, so it does not flicker. */
	private static final double MOVE_MARGIN = 0.8;

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
	private CodexFightCavesPilotConfig config;

	@Inject
	private CaveOverlay caveOverlay;

	private final PilotPlaque plaque = new PilotPlaque();
	private final RotationTracker tracker = new RotationTracker();
	private final Presence presence = new Presence();
	/** A wave call heard on the tick you were moved in, before your tile said so. */
	private final EarlyCall earlyCall = new EarlyCall();
	/** The last ten seconds or so of the wave on now, for the review if you fall. */
	private final Fall fall = new Fall();
	/** Damage taken since the last tick was filed. */
	private int tickDamage;
	/** What could land on you at once past your prayer when it is enough to kill you, else 0. */
	private int danger;
	/** Creatures that appeared recently, waiting to be matched to a wave. */
	private final List<Sighting> sightings = new ArrayList<>();
	/** The creatures of the wave on now, for re-working where to stand. */
	private final List<NPC> waveNpcs = new ArrayList<>();
	/** Item id to {kind, doses}: kind 1 is a prayer potion, 2 a super restore or sanfew, 0 neither. */
	private final Map<Integer, int[]> doseCache = new HashMap<>();

	private PilotPanel panel;
	private NavigationButton navButton;

	private String profile;
	private Logbook book;
	private PrayerPacer pacer = new PrayerPacer();
	private boolean unsaved;

	private boolean inCave;
	private Grid grid;
	private CaveMap caveMap;
	private int tick;
	private int pendingWave;
	private int pendingTick;
	private boolean died;
	/** Hitpoints on the last tick in the cave, or -1 before the first. */
	private int lastHp = -1;
	/** The tick you left the cave on the Jad wave without falling, or -1: the cape message may follow. */
	private int jadExitTick = -1;
	private int lastPrayer = -1;
	private int waveSpent;
	private int prayerDoses;
	private int restoreDoses;
	private int lastDoses = -1;
	private boolean warnedShort;

	/** The last wave read, as it spawned, for the panel to show before the rotation is known. */
	private final List<Placement> observedNow = new ArrayList<>();
	private int observedWave;

	/** The last health seen on each creature's bar, kept once the bar fades. */
	private final Map<NPC, Double> health = new HashMap<>();
	/** Weapon item id to how far it attacks from. */
	private final Map<Integer, Integer> rangeCache = new HashMap<>();
	private int weaponRange = WeaponRange.DEFAULT;
	/** What you wield, by name, or null; and the ammunition in your quiver. */
	private String weaponName;
	private int ammo;
	/** Pieces of food carried, and item id to whether it is food. */
	private int foodCount;
	private final Map<Integer, Boolean> foodCache = new HashMap<>();
	/** Bank Steward's shared bank as last read, parsed again only when it changes. */
	private String stewardJson;
	private Banked stewardBank;
	private PrayerCall call;
	private List<KillOrder.Target> order = java.util.Collections.emptyList();

	private StandPlanner.Spot liveSpot;
	private StandPlanner.Spot nextSpot;
	private int nextSpotWave;
	private String lastViewKey;

	/** Walkable tiles of the cave's map square, [x][y], read once per copy of the cave. */
	private boolean[][] floor;
	/** The next wave played out in scene tiles, for the floor, and in the cave's own tiles, for the panel. */
	private Rehearsal rehearsal;
	private Rehearsal rehearsalCave;
	/** What the rehearsal was worked out for, so it is only redone when the wave or the tile changes. */
	private String rehearsalKey;
	/** Why the gilt tile beats where you stand, or null. */
	private Compare compare;
	/** The road to Jad being rated for this rotation, one wave at a time, or null. */
	private Outlook outlook;
	/** What the outlook is being rated for: "rotation:reach". */
	private String outlookKey;

	private static final class Sighting
	{
		final NPC npc;
		final Monster monster;
		final int tick;

		Sighting(NPC npc, Monster monster, int tick)
		{
			this.npc = npc;
			this.monster = monster;
			this.tick = tick;
		}
	}

	@Provides
	CodexFightCavesPilotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexFightCavesPilotConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new PilotPanel();
		// The panel is only rebuilt while it is open, so bring it up to date as it opens.
		panel.onOpen(() -> clientThread.invokeLater(() -> publish(true)));
		navButton = NavigationButton.builder()
			.tooltip("Codex Fight Caves Pilot")
			.icon(Sigil.icon(16))
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		overlayManager.add(caveOverlay);
		overlayManager.add(plaque);
		clientThread.invokeLater(() ->
		{
			loadProfile();
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				countDoses(client.getItemContainer(InventoryID.INV));
				readWeapon(client.getItemContainer(InventoryID.WORN));
			}
			publish(true);
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(caveOverlay);
		overlayManager.remove(plaque);
		clientToolbar.removeNavigation(navButton);
		clientThread.invokeLater(() ->
		{
			save();
			book = null;
			profile = null;
			resetCave();
			inCave = false;
			presence.reset();
		});
		panel = null;
	}

	// Events.

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOADING:
				// A fresh copy of the cave means fresh collision data, and fresh scene tiles for
				// the road to Jad to start from.
				grid = null;
				outlook = null;
				break;
			case LOGIN_SCREEN:
			case HOPPING:
				jadExitTick = -1;
				// The next account, or this one after playing elsewhere, may carry something else:
				// the first inventory seen after logging in is the new base, not doses drunk.
				lastDoses = -1;
				// Nor is what the last account carried and wielded: the game may send nothing for
				// an empty pack or empty hands, so the ready check would read the old ones.
				prayerDoses = 0;
				restoreDoses = 0;
				foodCount = 0;
				weaponName = null;
				weaponRange = 1;
				ammo = 0;
				earlyCall.clear();
				save();
				resetCave();
				inCave = false;
				presence.reset();
				publish(true);
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		// The logbook is otherwise saved once a minute; closing the client must not lose the
		// last of a run. Saved here, before RuneLite sends its config on.
		try
		{
			save();
		}
		catch (RuntimeException e)
		{
			log.warn("Could not save the logbook on shutdown", e);
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		save();
		loadProfile();
		publish(true);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// The logbook itself is saved under the same group; its own saves need no redraw.
		boolean ours = CodexFightCavesPilotConfig.GROUP.equals(event.getGroup()) && !LOGBOOK_KEY.equals(event.getKey());
		boolean steward = Banked.STEWARD_GROUP.equals(event.getGroup()) && Banked.STEWARD_KEY.equals(event.getKey());
		if (ours || steward)
		{
			clientThread.invokeLater(() -> publish(true));
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		// Kept even before you are seen inside: on the tick you are moved in, the first spawns
		// can come before your tile does. Only the cave's own creatures are kept, and only for
		// a few ticks unless a wave call claims them.
		NPC npc = event.getNpc();
		Monster m = Monster.of(npc.getName(), npc.getCombatLevel());
		if (m != null && book != null)
		{
			sightings.add(new Sighting(npc, m, tick));
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		waveNpcs.remove(event.getNpc());
		health.remove(event.getNpc());
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (inCave && event.getActor() == client.getLocalPlayer())
		{
			died = true;
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!inCave || book == null || book.active == null || event.getActor() != client.getLocalPlayer())
		{
			return;
		}
		if (Damage.counts(event.getHitsplat().getHitsplatType(), event.getHitsplat().getAmount()))
		{
			Logbook.hurt(book.active, event.getHitsplat().getAmount());
			tickDamage += event.getHitsplat().getAmount();
			unsaved = true;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			countDoses(event.getItemContainer());
		}
		else if (event.getContainerId() == InventoryID.BANK)
		{
			seeBank(event.getItemContainer());
		}
		else if (event.getContainerId() == InventoryID.WORN)
		{
			readWeapon(event.getItemContainer());
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		String text = TAGS.matcher(event.getMessage()).replaceAll("");
		Matcher wave = WAVE.matcher(text);
		if (wave.find() && book != null)
		{
			if (inCave || arriveNow())
			{
				onWave(Integer.parseInt(wave.group(1)));
			}
			else
			{
				// Possibly the cave's first call, sent before your new tile could be read.
				earlyCall.hear(Integer.parseInt(wave.group(1)), tick);
			}
			return;
		}
		if (KILL_COUNT.matcher(text).find() && book != null && book.active != null && (inCave || jadExitTick >= 0))
		{
			jadExitTick = -1;
			Logbook.Active a = book.active;
			// Jad's own wave is timed like the rest; a missed wave call leaves a gap, not a shift.
			Logbook.advance(a, Waves.LAST + 1);
			a.wave = Waves.LAST;
			Logbook.Run run = book.finish(Logbook.Result.CAPE);
			unsaved = true;
			save();
			if (config.chat() && run != null)
			{
				chat("fire cape in " + Format.ticks(run.ticks)
					+ (run.rotation > 0 ? " on rotation " + run.rotation : "")
					+ (run.dosesUsed > 0 ? ", " + Format.doses(run.dosesUsed) + " drunk." : "."));
			}
			resetCave();
			publish(true);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN || book == null)
		{
			return;
		}
		tick++;
		Player me = client.getLocalPlayer();
		boolean known = me != null && me.getLocalLocation() != null;
		// Physically in the cave this tick; a lone odd tick does not end the run (see Presence).
		boolean here = known && isInCave();
		Presence.Change change = presence.tick(known, here);
		if (change == Presence.Change.ENTERED)
		{
			enterCave();
			inCave = true;
			int early = earlyCall.take(tick);
			if (early > 0)
			{
				onWave(early);
			}
		}
		else if (change == Presence.Change.LEFT)
		{
			leaveCave();
		}
		else if (!presence.isInside() && book.active != null && presence.outsideFor() >= Presence.SETTLE_TICKS
			&& (jadExitTick < 0 || tick - jadExitTick > CAPE_GRACE))
		{
			// Logged in outside the cave with a run still open: it ended while we were away. Or
			// you walked out on Jad and no cape message came.
			book.finish(Logbook.Result.LEFT);
			jadExitTick = -1;
			unsaved = true;
		}
		inCave = presence.isInside();

		if (inCave && here)
		{
			if (grid == null)
			{
				buildGrid();
			}
			if (book.active != null)
			{
				book.active.ticks++;
			}
			trackPrayer();
			// A fall that the death event missed still shows as hitpoints dropping to nothing. Only
			// a drop counts: just after logging in the levels can read 0 before they arrive.
			int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
			if (book.active != null && lastHp > 0 && hp <= 0 && client.getRealSkillLevel(Skill.HITPOINTS) > 0)
			{
				died = true;
			}
			lastHp = hp;
			if (book.active != null && book.active.wave < Waves.JAD && jadOnFloor())
			{
				// Jad or a healer is out but the wave call was missed (a run picked up part way, or
				// one carried on elsewhere): it is the Jad wave, and nothing may be offered on it.
				Logbook.advance(book.active, Waves.JAD);
				waveSpent = 0;
				unsaved = true;
			}
			if (pendingWave > 0 && readyToRead())
			{
				readWave();
			}
			else if (pendingWave == 0)
			{
				pruneSightings();
			}
			plan();
			if (tick % PLAN_TICKS != 0)
			{
				// Off the planning ticks, rate one more wave of the road to Jad.
				stepOutlook();
			}
		}
		else if (!inCave)
		{
			pruneSightings();
		}
		if (tick % SAVE_TICKS == 0)
		{
			save();
		}
		publish(false);
	}

	// The cave.

	/**
	 * True when you stand in the cave. Your own tile is read back to the cave's template, not the
	 * loaded map squares: the cave's square sits just south of TzHaar City on the world map, so
	 * walking the city's south side loads it without you being anywhere near a wave.
	 */
	private boolean isInCave()
	{
		Player me = client.getLocalPlayer();
		LocalPoint lp = me == null ? null : me.getLocalLocation();
		if (lp == null)
		{
			return false;
		}
		WorldPoint p = WorldPoint.fromLocalInstance(client, lp);
		return p != null && p.getRegionID() == FIGHT_CAVE_REGION;
	}

	/**
	 * True when you are in the cave by your own tile, though the tick that says so has not come
	 * yet: entering there and then, so a wave call on that very tick is not lost.
	 */
	private boolean arriveNow()
	{
		if (book == null || client.getGameState() != GameState.LOGGED_IN || !isInCave() || !presence.enterNow())
		{
			return false;
		}
		enterCave();
		inCave = true;
		earlyCall.clear();
		return true;
	}

	private void enterCave()
	{
		// Creatures seen in the moment before you were placed belong to the wave about to be called.
		List<Sighting> early = new ArrayList<>(sightings);
		resetCave();
		sightings.addAll(early);
		tracker.reset();
		Logbook.Active a = book.active;
		if (a != null)
		{
			// Picking a run back up after logging out: replay what it had seen.
			for (Map.Entry<Integer, List<String>> e : a.seen.entrySet())
			{
				List<Spawn> spawns = new ArrayList<>();
				for (String code : e.getValue())
				{
					spawns.add(Spawn.ofCode(code));
				}
				tracker.observe(e.getKey(), spawns);
			}
			if (tracker.rotation() == 0 && a.rotation > 0)
			{
				tracker.assume(a.rotation);
			}
		}
	}

	private void leaveCave()
	{
		if (book.active != null && book.active.wave == Waves.JAD && !died)
		{
			// Killing Jad moves you out of the cave, and the kill count can arrive a tick later.
			jadExitTick = tick;
		}
		else if (book.active != null)
		{
			Logbook.Run run = book.finish(died ? Logbook.Result.DIED : Logbook.Result.LEFT);
			if (died && run != null && run.wave < Waves.JAD)
			{
				review(run);
			}
			unsaved = true;
			save();
		}
		resetCave();
	}

	/** Files why a run fell before Jad, from its last few seconds, and says so in the chat. */
	private void review(Logbook.Run run)
	{
		Fall.Review r = fall.review(prayerDoses + restoreDoses);
		if (r == null)
		{
			return;
		}
		run.fall = r.getText();
		run.fallHp = r.getHp();
		if (config.chat())
		{
			chat("fell on wave " + run.wave + ". " + r.getText());
		}
	}

	private void resetCave()
	{
		grid = null;
		caveMap = null;
		sightings.clear();
		waveNpcs.clear();
		pendingWave = 0;
		died = false;
		lastPrayer = -1;
		lastHp = -1;
		waveSpent = 0;
		warnedShort = false;
		liveSpot = null;
		nextSpot = null;
		nextSpotWave = 0;
		observedNow.clear();
		observedWave = 0;
		health.clear();
		call = null;
		order = java.util.Collections.emptyList();
		floor = null;
		outlook = null;
		clearRehearsal();
		compare = null;
		fall.clear();
		tickDamage = 0;
		danger = 0;
		caveOverlay.show(Marks.NONE);
	}

	private void clearRehearsal()
	{
		rehearsal = null;
		rehearsalCave = null;
		rehearsalKey = null;
	}

	private void buildGrid()
	{
		WorldView wv = client.getTopLevelWorldView();
		Player me = client.getLocalPlayer();
		CollisionData[] maps = wv == null ? null : wv.getCollisionMaps();
		if (maps == null || me == null || me.getLocalLocation() == null)
		{
			return;
		}
		grid = new Grid(maps[wv.getPlane()].getFlags());
		caveMap = new CaveMap(book.anchors);
		floor = readFloor();
	}

	/** Which tiles of the cave's map square are open floor, for the panel's map of the cave. */
	private boolean[][] readFloor()
	{
		boolean[][] f = new boolean[64][64];
		for (int x = 0; x < grid.getWidth(); x++)
		{
			for (int y = 0; y < grid.getHeight(); y++)
			{
				int[] c = caveLocal(new int[]{x, y});
				if (c != null && c[0] >= 0 && c[1] >= 0 && c[0] < 64 && c[1] < 64)
				{
					f[c[0]][c[1]] = grid.walkable(x, y);
				}
			}
		}
		return f;
	}

	/** A scene tile as a tile of the cave's map square, counted from its south-west corner, or null. */
	private int[] caveLocal(int[] scene)
	{
		WorldPoint p = cave(scene[0], scene[1]);
		return p == null ? null : new int[]{p.getX() - CaveMap.BASE_X, p.getY() - CaveMap.BASE_Y};
	}

	/** A scene tile in the cave's own coordinates, which stay put from one copy of the cave to the next. */
	private WorldPoint cave(int sceneX, int sceneY)
	{
		WorldView wv = client.getTopLevelWorldView();
		LocalPoint lp = wv == null ? null : LocalPoint.fromScene(sceneX, sceneY, wv);
		return lp == null ? null : WorldPoint.fromLocalInstance(client, lp);
	}

	/** A tile in the cave's own coordinates on this copy of the cave, as a scene tile, or null. */
	private int[] scene(int caveX, int caveY)
	{
		WorldView wv = client.getTopLevelWorldView();
		Collection<WorldPoint> points = WorldPoint.toLocalInstance(wv, new WorldPoint(caveX, caveY, wv.getPlane()));
		for (WorldPoint p : points)
		{
			LocalPoint lp = LocalPoint.fromWorld(wv, p);
			if (lp != null)
			{
				return new int[]{lp.getSceneX(), lp.getSceneY()};
			}
		}
		return null;
	}

	/** The south-west scene tile of a creature's body. */
	private static int[] southWest(NPC npc, Monster m)
	{
		LocalPoint lp = npc.getLocalLocation();
		if (lp == null)
		{
			return null;
		}
		int size = m == null ? 1 : m.getSize();
		return new int[]{(lp.getX() - size * 64) / 128, (lp.getY() - size * 64) / 128};
	}

	// Waves.

	private void onWave(int wave)
	{
		if (book == null)
		{
			return;
		}
		if (wave < 1 || wave > Waves.LAST)
		{
			return;
		}
		Logbook.Active a = book.active;
		if (a != null && (wave < a.wave || wave == 1 && a.wave > 1))
		{
			// A new run began without the old one closing.
			book.finish(Logbook.Result.LEFT);
			a = null;
		}
		if (a == null)
		{
			// Past wave 1 this is a run picked up part way: plugin turned on mid-run, or a run
			// from another computer. Its earlier waves stay untimed so the splits still line up.
			book.begin(wave, System.currentTimeMillis());
			tracker.reset();
			outlook = null;
			warnedShort = false;
		}
		else
		{
			int was = a.wave;
			if (Logbook.advance(a, wave))
			{
				pacer.learn(was, waveSpent);
				book.prayerSpent = pacer.getSpent();
				book.prayerHitpoints = pacer.getHitpoints();
			}
		}
		waveSpent = 0;
		pendingWave = wave;
		pendingTick = tick;
		liveSpot = null;
		waveNpcs.clear();
		unsaved = true;
	}

	/** True once every creature of the wave has appeared, or the grace period is over. */
	private boolean readyToRead()
	{
		if (tick >= pendingTick + SPAWN_GRACE)
		{
			return true;
		}
		return matched(pendingWave).size() >= Waves.of(pendingWave).size();
	}

	/** The recent sightings that belong to a wave, at most as many of each creature as it holds. */
	private List<Sighting> matched(int wave)
	{
		Map<Monster, Integer> left = new EnumMap<>(Monster.class);
		for (Monster m : Waves.of(wave))
		{
			left.merge(m, 1, Integer::sum);
		}
		List<Sighting> list = new ArrayList<>();
		for (Sighting s : sightings)
		{
			Integer n = left.get(s.monster);
			if (s.tick >= pendingTick - 2 && n != null && n > 0)
			{
				left.put(s.monster, n - 1);
				list.add(s);
			}
		}
		return list;
	}

	private void readWave()
	{
		int wave = pendingWave;
		pendingWave = 0;
		List<Sighting> found = matched(wave);
		sightings.clear();
		if (found.isEmpty() || caveMap == null)
		{
			return;
		}
		List<Spawn> spawns = new ArrayList<>();
		List<double[]> centres = new ArrayList<>();
		for (Sighting s : found)
		{
			waveNpcs.add(s.npc);
			int[] sw = southWest(s.npc, s.monster);
			WorldPoint c = sw == null ? null : cave(sw[0], sw[1]);
			if (c == null)
			{
				continue;
			}
			double[] corner = {c.getX(), c.getY()};
			centres.add(corner);
			spawns.add(caveMap.classify(corner[0], corner[1]));
		}
		if (spawns.size() != found.size())
		{
			return;
		}
		observedNow.clear();
		for (int i = 0; i < found.size(); i++)
		{
			observedNow.add(new Placement(found.get(i).monster, spawns.get(i)));
		}
		observedWave = wave;
		int before = tracker.rotation();
		tracker.observe(wave, spawns);
		Logbook.Active a = book.active;
		if (a != null)
		{
			List<String> codes = new ArrayList<>();
			for (Spawn s : spawns)
			{
				codes.add(s.getCode());
			}
			a.seen.put(wave, codes);
		}
		int rotation = tracker.rotation();
		if (a != null && rotation > 0)
		{
			a.rotation = rotation;
		}
		// Not on Jad's wave, where the pilot says nothing at all.
		if (rotation > 0 && rotation != before && config.chat() && wave < Waves.JAD)
		{
			chat("rotation " + rotation + ", read on wave " + wave + ".");
		}
		if (rotation > 0 && tracker.clean() && found.size() == Waves.of(wave).size())
		{
			learn(rotation, wave, found, spawns, centres);
		}
		unsaved = true;
	}

	/**
	 * Remembers what a cleanly read wave showed: where each spawn point really is, and which
	 * creature stood where, in case it differs from the table.
	 */
	private void learn(int rotation, int wave, List<Sighting> found, List<Spawn> spawns, List<double[]> centres)
	{
		for (int i = 0; i < found.size(); i++)
		{
			CaveMap.learn(book.anchors, spawns.get(i), found.get(i).monster.getSize(), centres.get(i)[0], centres.get(i)[1]);
		}
		List<Placement> table = book.placements(rotation, wave);
		List<String> seen = new ArrayList<>();
		boolean[] used = new boolean[found.size()];
		for (Placement p : table)
		{
			String code = null;
			// Prefer the sighting of this creature at the spawn the table expects.
			for (int pass = 0; pass < 2 && code == null; pass++)
			{
				for (int i = 0; i < found.size(); i++)
				{
					if (!used[i] && found.get(i).monster == p.getMonster() && (pass == 1 || spawns.get(i) == p.getSpawn()))
					{
						used[i] = true;
						code = spawns.get(i).getCode();
						break;
					}
				}
			}
			if (code == null)
			{
				return;
			}
			seen.add(code);
		}
		boolean differs = false;
		for (int i = 0; i < table.size(); i++)
		{
			differs |= !table.get(i).getSpawn().getCode().equals(seen.get(i));
		}
		if (differs)
		{
			book.corrections.put(rotation + ":" + wave, seen);
		}
		if (caveMap != null)
		{
			caveMap = new CaveMap(book.anchors);
		}
	}

	// The road to Jad.

	/**
	 * Rates one more wave of your rotation, from the wave after the one on now. The outlook is
	 * started over when the rotation or your weapon's reach changes, and saved once it reaches
	 * the last wave before Jad, so the Waves page can show it outside the cave.
	 */
	private void stepOutlook()
	{
		int rotation = tracker.rotation();
		Player me = client.getLocalPlayer();
		if (!config.outlook() || rotation == 0 || grid == null || caveMap == null || book.active == null
			|| me == null || me.getLocalLocation() == null)
		{
			return;
		}
		String key = rotation + ":" + weaponRange;
		if (outlook == null || !key.equals(outlookKey))
		{
			outlookKey = key;
			outlook = new Outlook(book.outlooks.get(key), book.active.wave + 1,
				me.getLocalLocation().getSceneX(), me.getLocalLocation().getSceneY());
		}
		if (outlook.done())
		{
			return;
		}
		outlook.step(grid, w -> spawnFoes(rotation, w), weaponRange);
		if (outlook.done())
		{
			book.outlooks.put(key, outlook.encode());
			unsaved = true;
		}
	}

	/** A wave's creatures on their spawn tiles, in this copy of the cave's scene tiles. */
	private List<StandPlanner.Foe> spawnFoes(int rotation, int wave)
	{
		List<StandPlanner.Foe> list = new ArrayList<>();
		for (Placement p : book.placements(rotation, wave))
		{
			int[] t = caveMap.tile(p.getSpawn(), p.getMonster().getSize());
			int[] s = scene(t[0], t[1]);
			if (s == null)
			{
				return java.util.Collections.emptyList();
			}
			list.add(new StandPlanner.Foe(p.getMonster(), s[0], s[1]));
		}
		return list;
	}

	/** The road to Jad for your rotation: this visit's when there is one, otherwise the saved one. */
	private String outlookCodes(int rotation)
	{
		if (rotation <= 0)
		{
			return null;
		}
		if (outlook != null && (rotation + ":" + weaponRange).equals(outlookKey))
		{
			return outlook.encode();
		}
		return Outlook.saved(book.outlooks, rotation, weaponRange);
	}

	/** Why the next split wave (or, with none left, the next pray wave) is hard, from this visit's plan. */
	private String outlookReason(int wave)
	{
		Outlook o = outlook;
		if (o == null)
		{
			return null;
		}
		String codes = o.encode();
		int next = Outlook.nextAtLeast(codes, wave, Outlook.Rating.SPLIT);
		if (next == 0)
		{
			next = Outlook.nextAtLeast(codes, wave, Outlook.Rating.PRAY);
		}
		return next == 0 ? null : o.reason(next);
	}

	// Prayer.

	private void trackPrayer()
	{
		int prayer = client.getBoostedSkillLevel(Skill.PRAYER);
		if (lastPrayer >= 0 && prayer < lastPrayer && book.active != null)
		{
			waveSpent += lastPrayer - prayer;
		}
		lastPrayer = prayer;
	}

	private void countDoses(ItemContainer inventory)
	{
		int prayerCount = 0;
		int restoreCount = 0;
		int food = 0;
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (item.getId() <= 0)
				{
					continue;
				}
				if (foodCache.computeIfAbsent(item.getId(), id -> !noted(id) && Ready.isFood(itemManager.getItemComposition(id).getName())))
				{
					food += item.getQuantity();
				}
				int[] kind = doseCache.computeIfAbsent(item.getId(), this::doseKind);
				if (kind[0] == 1)
				{
					prayerCount += kind[1] * item.getQuantity();
				}
				else if (kind[0] == 2)
				{
					restoreCount += kind[1] * item.getQuantity();
				}
			}
		}
		int total = prayerCount + restoreCount;
		if (inCave && lastDoses >= 0 && total < lastDoses && book != null && book.active != null)
		{
			book.active.dosesUsed += lastDoses - total;
			unsaved = true;
		}
		lastDoses = total;
		prayerDoses = prayerCount;
		restoreDoses = restoreCount;
		foodCount = food;
	}

	/** The bank changed while open: count its doses for how many runs it covers. */
	private void seeBank(ItemContainer bank)
	{
		if (bank == null || book == null)
		{
			return;
		}
		Map<Integer, Long> items = new HashMap<>();
		for (Item item : bank.getItems())
		{
			if (item.getId() > 0 && item.getQuantity() > 0)
			{
				items.merge(item.getId(), (long) item.getQuantity(), Long::sum);
			}
		}
		long doses = Banked.count(items);
		long now = System.currentTimeMillis();
		// Saved when the count moves, or now and then so "seen" stays honest across restarts.
		if (doses != book.bankDoses || now - book.bankSeenAt > 10 * 60_000L)
		{
			unsaved = true;
		}
		book.bankDoses = doses;
		book.bankSeenAt = now;
	}

	/** The doses in your bank: the pilot's own look or Bank Steward's, whichever is newer, or null. */
	private Banked banked()
	{
		String json = configManager.getRSProfileConfiguration(Banked.STEWARD_GROUP, Banked.STEWARD_KEY);
		if (json == null || json.isEmpty())
		{
			stewardJson = null;
			stewardBank = null;
		}
		else if (!json.equals(stewardJson))
		{
			stewardJson = json;
			stewardBank = Banked.steward(gson, json);
		}
		Banked own = book == null || book.bankDoses < 0 ? null : new Banked(book.bankDoses, book.bankSeenAt, Banked.Source.PILOT);
		return Banked.newest(own, stewardBank);
	}

	/** True for a banknote: noted sharks or potions carried are not food or doses to drink. */
	private boolean noted(int itemId)
	{
		return itemManager.getItemComposition(itemId).getNote() != -1;
	}

	private int[] doseKind(int itemId)
	{
		if (noted(itemId))
		{
			return new int[]{0, 0};
		}
		String name = itemManager.getItemComposition(itemId).getName();
		if (name == null)
		{
			return new int[]{0, 0};
		}
		int open = name.lastIndexOf('(');
		if (open < 0 || !name.endsWith(")"))
		{
			return new int[]{0, 0};
		}
		int doses;
		try
		{
			doses = Integer.parseInt(name.substring(open + 1, name.length() - 1));
		}
		catch (NumberFormatException e)
		{
			return new int[]{0, 0};
		}
		String base = name.substring(0, open).trim();
		if (base.equals("Prayer potion"))
		{
			return new int[]{1, doses};
		}
		if (base.equals("Super restore") || base.equals("Sanfew serum"))
		{
			return new int[]{2, doses};
		}
		return new int[]{0, 0};
	}

	// Where to stand.

	private void plan()
	{
		Player me = client.getLocalPlayer();
		if (grid == null || me == null || me.getLocalLocation() == null || book.active == null)
		{
			caveOverlay.show(Marks.NONE);
			clearRehearsal();
			compare = null;
			// Nothing worked out this tick: a warning or call from an earlier one would be stale.
			danger = 0;
			call = null;
			return;
		}
		int px = me.getLocalLocation().getSceneX();
		int py = me.getLocalLocation().getSceneY();
		int wave = book.active.wave;
		int rotation = tracker.rotation();

		List<NPC> alive = aliveFoes();
		List<StandPlanner.Foe> foes = new ArrayList<>();
		for (NPC npc : alive)
		{
			Monster m = monsterOf(npc);
			int[] sw = southWest(npc, m);
			if (m != null && sw != null)
			{
				foes.add(new StandPlanner.Foe(m, sw[0], sw[1], healthOf(npc), npc));
			}
		}

		boolean jad = wave >= Waves.JAD;
		if (!jad && config.standTile() && config.followFight() && !foes.isEmpty() && tick % PLAN_TICKS == 0)
		{
			StandPlanner.Spot best = StandPlanner.plan(grid, foes, px, py, weaponRange);
			if (best != null && liveSpot != null)
			{
				int steps = Math.max(Math.abs(liveSpot.getX() - px), Math.abs(liveSpot.getY() - py));
				StandPlanner.Spot kept = StandPlanner.score(grid, foes, liveSpot.getX(), liveSpot.getY(), steps, weaponRange);
				liveSpot = kept.getCost() <= best.getCost() + MOVE_MARGIN ? kept : best;
			}
			else
			{
				liveSpot = best;
			}
		}
		if (foes.isEmpty() || jad)
		{
			// On Jad's wave no tile is offered: one left over from the wave before would read as
			// where to stand against him.
			liveSpot = null;
		}
		// Jad is yours to read: no prayer call, no eat warning and no kill order on his wave.
		List<Threat> threats = jad ? java.util.Collections.emptyList() : Threat.of(grid, foes, px, py);
		Protect up = activeProtect();
		int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
		if (jad)
		{
			// The last of wave 62 says nothing about a fall to Jad, which is never reviewed.
			fall.clear();
		}
		else
		{
			fall.add(new Fall.Frame(hp, client.getBoostedSkillLevel(Skill.PRAYER), up, threats, tickDamage));
		}
		tickDamage = 0;
		danger = !jad && config.eatWarning() ? Threat.danger(threats, up, hp) : 0;
		call = !jad && config.prayerCall() ? PrayerCall.of(threats) : null;
		order = !jad && config.killOrder() && foes.size() > 1
			? KillOrder.rank(grid, foes, px, py, weaponRange, activeProtect()) : java.util.Collections.emptyList();

		// As the wave on now draws to a close, plan for the one about to spawn.
		boolean closing = rotation > 0 && wave + 1 < Waves.JAD && alive.size() <= 1 && pendingWave == 0 && caveMap != null;
		List<Marks.Ghost> ghosts = new ArrayList<>();
		if (closing)
		{
			List<StandPlanner.Foe> next = new ArrayList<>();
			for (Placement p : book.placements(rotation, wave + 1))
			{
				int[] t = caveMap.tile(p.getSpawn(), p.getMonster().getSize());
				int[] s = scene(t[0], t[1]);
				if (s != null)
				{
					next.add(new StandPlanner.Foe(p.getMonster(), s[0], s[1]));
					ghosts.add(new Marks.Ghost(p.getMonster(), s[0], s[1]));
				}
			}
			if (config.standTile() && !next.isEmpty() && (nextSpot == null || nextSpotWave != wave + 1 || tick % (PLAN_TICKS * 2) == 0))
			{
				nextSpot = StandPlanner.plan(grid, next, px, py, weaponRange);
				nextSpotWave = wave + 1;
			}
			rehearse(next, wave + 1, px, py);
		}
		else
		{
			nextSpot = null;
			nextSpotWave = 0;
			clearRehearsal();
		}

		List<NPC> stuck = new ArrayList<>();
		// Jad counts as a ranged creature, but saying he is stuck behind a pillar would be telling
		// you how to fight him, so his wave gets no tags.
		if (config.stuckTags() && !jad)
		{
			for (NPC npc : alive)
			{
				Monster m = monsterOf(npc);
				int[] sw = southWest(npc, m);
				if (m != null && m.isRanged() && sw != null && Chase.run(grid, m, sw[0], sw[1], px, py).getEnd() == Chase.End.STUCK)
				{
					stuck.add(npc);
				}
			}
		}

		Marks.Stand stand = null;
		StandPlanner.Spot spot = shownSpot();
		if (spot != null && config.standTile())
		{
			stand = new Marks.Stand(spot.getX(), spot.getY(), spot == nextSpot, spot.safeFromRange());
		}
		compare = spot != null && config.standTile() ? Compare.of(StandPlanner.score(grid, foesOf(spot), px, py, 0, weaponRange), spot) : null;
		Map<NPC, Integer> ranks = new HashMap<>();
		for (int i = 0; i < order.size(); i++)
		{
			KillOrder.Target t = order.get(i);
			if (t.isReachable() && t.getFoe().getTag() instanceof NPC)
			{
				ranks.put((NPC) t.getFoe().getTag(), i + 1);
			}
		}
		caveOverlay.show(new Marks(stand, config.nextSpawns() ? ghosts : java.util.Collections.emptyList(), stuck, ranks, rehearsal));
	}

	/**
	 * Plays the next wave out from its spawn points to the tile you will stand on: the teal
	 * tile when there is one, otherwise where you are. Only redone when the wave, the tile or
	 * your weapon changes, so the panel's replay is not restarted every few ticks.
	 */
	private void rehearse(List<StandPlanner.Foe> next, int wave, int px, int py)
	{
		if (!config.rehearse() || next.isEmpty())
		{
			clearRehearsal();
			return;
		}
		StandPlanner.Spot target = nextSpot != null && nextSpotWave == wave ? nextSpot : null;
		int sx = target != null ? target.getX() : px;
		int sy = target != null ? target.getY() : py;
		String key = wave + ":" + sx + "," + sy + ":" + weaponRange + ":" + next.size();
		if (key.equals(rehearsalKey))
		{
			return;
		}
		rehearsalKey = key;
		rehearsal = Rehearsal.of(grid, next, sx, sy, weaponRange);
		rehearsalCave = rehearsal.map(this::caveLocal);
	}

	/** The creatures a spot was worked out against. */
	private static List<StandPlanner.Foe> foesOf(StandPlanner.Spot spot)
	{
		List<StandPlanner.Foe> list = new ArrayList<>();
		for (StandPlanner.Outcome o : spot.getOutcomes())
		{
			list.add(o.getFoe());
		}
		return list;
	}

	/** The spot to show: the next wave's once the wave on now is all but done, otherwise the live one. */
	private StandPlanner.Spot shownSpot()
	{
		if (nextSpot != null && (liveSpot == null || aliveFoes().isEmpty()))
		{
			return nextSpot;
		}
		return liveSpot != null ? liveSpot : nextSpot;
	}

	/** The share of a creature's hitpoints left, from its health bar or the last one it showed. */
	private double healthOf(NPC npc)
	{
		int ratio = npc.getHealthRatio();
		int scale = npc.getHealthScale();
		if (ratio >= 0 && scale > 0)
		{
			double h = ratio / (double) scale;
			health.put(npc, h);
			return h;
		}
		return health.getOrDefault(npc, 1.0);
	}

	/** The protection prayer you have up, or null. */
	private Protect activeProtect()
	{
		if (client.getVarbitValue(VarbitID.PRAYER_PROTECTFROMMAGIC) == 1)
		{
			return Protect.MAGIC;
		}
		if (client.getVarbitValue(VarbitID.PRAYER_PROTECTFROMMISSILES) == 1)
		{
			return Protect.MISSILES;
		}
		if (client.getVarbitValue(VarbitID.PRAYER_PROTECTFROMMELEE) == 1)
		{
			return Protect.MELEE;
		}
		return null;
	}

	private void readWeapon(ItemContainer worn)
	{
		Item weapon = worn == null ? null : worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		Item quiver = worn == null ? null : worn.getItem(EquipmentInventorySlot.AMMO.getSlotIdx());
		ammo = quiver == null || quiver.getId() <= 0 ? 0 : quiver.getQuantity();
		if (weapon == null || weapon.getId() <= 0)
		{
			weaponRange = 1;
			weaponName = null;
			return;
		}
		weaponName = itemManager.getItemComposition(weapon.getId()).getName();
		weaponRange = rangeCache.computeIfAbsent(weapon.getId(), id -> WeaponRange.of(itemManager.getItemComposition(id).getName()));
	}

	/** True when TzTok-Jad or one of his healers is in the scene. */
	private boolean jadOnFloor()
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return false;
		}
		for (NPC npc : wv.npcs())
		{
			if (npc != null && Monster.isJadFight(npc.getName()))
			{
				return true;
			}
		}
		return false;
	}

	private List<NPC> aliveFoes()
	{
		List<NPC> list = new ArrayList<>();
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return list;
		}
		for (NPC npc : wv.npcs())
		{
			if (npc != null && !npc.isDead() && monsterOf(npc) != null)
			{
				list.add(npc);
			}
		}
		return list;
	}

	/** The creature an NPC is, counting a split Tz-Kek's halves as the bats they fight like. */
	private static Monster monsterOf(NPC npc)
	{
		Monster m = Monster.of(npc.getName(), npc.getCombatLevel());
		if (m == null && "Tz-Kek".equalsIgnoreCase(npc.getName()))
		{
			return Monster.TZ_KIH;
		}
		return m;
	}

	// Saving and showing.

	private void loadProfile()
	{
		profile = configManager.getRSProfileKey();
		book = null;
		if (profile == null)
		{
			return;
		}
		try
		{
			String saved = configManager.getRSProfileConfiguration(CodexFightCavesPilotConfig.GROUP, LOGBOOK_KEY);
			if (saved != null && !saved.isEmpty())
			{
				Logbook read = gson.fromJson(saved, Logbook.class);
				book = read == null ? null : read.repair();
			}
		}
		catch (JsonParseException e)
		{
			log.warn("Could not read the saved logbook, starting afresh", e);
		}
		if (book == null)
		{
			book = new Logbook();
		}
		pacer = new PrayerPacer(book.prayerSpent, book.prayerHitpoints);
		unsaved = false;
		lastViewKey = null;
	}

	private void save()
	{
		if (profile == null || book == null || !unsaved)
		{
			return;
		}
		// Written out first: on client close this runs off the client thread, and a logbook caught
		// changing mid-write must stay marked unsaved rather than be lost.
		String json = gson.toJson(book);
		unsaved = false;
		configManager.setConfiguration(CodexFightCavesPilotConfig.GROUP, profile, LOGBOOK_KEY, json);
	}

	private void chat(String text)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.runeLiteFormattedMessage(new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT).append("Codex Pilot: ")
				.append(ChatColorType.NORMAL).append(text)
				.build())
			.build());
	}

	/**
	 * Hands the panel and the plaque what they show; the panel only when something it shows
	 * changed. The logbook is copied for the panel only then, not every tick: it holds every run.
	 */
	private void publish(boolean force)
	{
		PilotPanel target = panel;
		if (target == null || !target.isOpen())
		{
			// Closed: no logbook copy and no rebuild, just the plaque, and outside the cave only to take
			// it down. Opening the panel forces a fresh view.
			lastViewKey = null;
			if (inCave || force || plaque.isShowing())
			{
				updatePlaque(buildView(false));
			}
			return;
		}
		String key = viewKey();
		boolean changed = force || !key.equals(lastViewKey);
		if (!changed)
		{
			if (inCave)
			{
				// The plaque follows prayer and doses every tick, and needs no logbook.
				updatePlaque(buildView(false));
			}
			return;
		}
		View view = buildView(true);
		updatePlaque(view);
		lastViewKey = key;
		if (target != null)
		{
			SwingUtilities.invokeLater(() -> target.update(view));
		}
	}

	/** @param withBook whether the panel will show it, so it needs its own copy of the logbook */
	private View buildView(boolean withBook)
	{
		if (book == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return View.loggedOut();
		}
		int prayer = client.getBoostedSkillLevel(Skill.PRAYER);
		int level = client.getRealSkillLevel(Skill.PRAYER);
		if (!inCave)
		{
			PrayerPacer.Pace full = pacer.pace(1, level, level, 0, 0, config.jadDoses());
			PrayerPacer.Pace carried = prayerDoses + restoreDoses > 0
				? pacer.pace(1, level, level, prayerDoses, restoreDoses, config.jadDoses()) : null;
			return new View(View.State.OUTSIDE, 0, 0, null, 0, false, 0, null, null, null, prayer, level,
				pacer.learned(), full.getDosesNeeded(), 0, null, withBook ? copy(book) : null).withCarried(carried)
				.withForecast(withBook ? Splits.forecast(book.runs, 1, 0, 0) : null)
				.withReady(Ready.check(level, weaponName, weaponRange, ammo, foodCount, prayerDoses + restoreDoses))
				.withBanked(withBook ? banked() : null);
		}
		Logbook.Active a = book.active;
		int wave = a == null ? 0 : a.wave;
		PrayerPacer.Pace pace = wave > 0 ? pacer.pace(wave, prayer, level, prayerDoses, restoreDoses, config.jadDoses()) : null;
		if (pace != null && config.chat())
		{
			if (pace.getState() == PrayerPacer.State.SHORT && !warnedShort && wave > 6 && wave < Waves.JAD)
			{
				warnedShort = true;
				chat("prayer is behind pace: about " + Math.max(1, (int) Math.ceil(-pace.getSpare())) + " doses short for the waves left at your rate.");
			}
			else if (pace.getState() == PrayerPacer.State.AMPLE || pace.getState() == PrayerPacer.State.ON_PACE)
			{
				warnedShort = false;
			}
		}

		View.Stand stand = null;
		StandPlanner.Spot spot = shownSpot();
		if (spot != null && caveMap != null && config.standTile())
		{
			double[] f = fraction(spot.getX(), spot.getY());
			List<View.Fate> fates = new ArrayList<>();
			for (StandPlanner.Outcome o : spot.getOutcomes())
			{
				fates.add(fate(o));
			}
			if (f != null)
			{
				stand = new View.Stand(f[0], f[1], spot == nextSpot, spot.safeFromRange(), spot.getSteps(), fates);
			}
		}
		double[] you = null;
		Player me = client.getLocalPlayer();
		if (me != null && me.getLocalLocation() != null && caveMap != null)
		{
			you = fraction(me.getLocalLocation().getSceneX(), me.getLocalLocation().getSceneY());
		}
		// Raced against your best cape at the end of the last wave cleared.
		Integer versus = a != null && wave > 1 ? Splits.versusPb(book.runs, wave - 1, a.waveStartTick, a.partial) : null;
		int[] youTile = me != null && me.getLocalLocation() != null && grid != null
			? caveLocal(new int[]{me.getLocalLocation().getSceneX(), me.getLocalLocation().getSceneY()}) : null;
		int rotation = tracker.rotation();
		List<Placement> observed = observedWave == wave ? observed() : java.util.Collections.emptyList();
		return new View(View.State.IN_CAVE, wave, rotation, tracker.candidates(), tracker.decidingWave(wave + 1), tracker.clean(),
			aliveFoes().size(), stand, you, pace, prayer, level, pacer.learned(), 0, a == null ? 0 : a.ticks, versus, withBook ? copy(book) : null)
			.withObserved(observed).withFight(callView(), killsView())
			.withRehearsal(config.rehearse() ? rehearsalCave : null, floor, youTile)
			.withCompare(compare)
			.withSplits(a == null ? null : Splits.recent(book.runs, a.splits, 3))
			.withPartial(a != null && a.partial)
			.withDanger(danger)
			.withForecast(withBook && a != null && !a.partial ? Splits.forecast(book.runs, wave, a.ticks, a.ticks - a.waveStartTick) : null)
			.withOutlook(config.outlook() ? outlookCodes(rotation) : null, config.outlook() ? outlookReason(wave) : null);
	}

	private View.Call callView()
	{
		PrayerCall c = call;
		return c == null ? null : new View.Call(c.getPrayer(), c.getAlso(), c.getPrayer() == activeProtect(), c.getAnswered());
	}

	private List<View.Kill> killsView()
	{
		List<View.Kill> list = new ArrayList<>();
		for (KillOrder.Target t : order)
		{
			list.add(new View.Kill(t.getFoe().getMonster(), t.getReason().getText(), t.isReachable()));
		}
		return list;
	}

	private List<Placement> observed()
	{
		return new ArrayList<>(observedNow);
	}

	private static View.Fate fate(StandPlanner.Outcome o)
	{
		Monster m = o.getFoe().getMonster();
		Chase c = o.getChase();
		switch (c.getEnd())
		{
			case STUCK:
				if (o.isInReach())
				{
					return new View.Fate(m, "stuck where you can hit it", true);
				}
				return new View.Fate(m, m.isRanged() ? "stuck, cannot see you" : "stuck out of reach", true);
			case AT_ONCE:
				return new View.Fate(m, o.getStyle() == Monster.Style.MELEE && m.isRanged() ? "beside you, hits with melee"
					: m.isRanged() ? "can hit you at once" : "already on you", false);
			default:
				if (o.getStyle() == Monster.Style.MELEE && m.isRanged())
				{
					return new View.Fate(m, "comes beside you, hits with melee", false);
				}
				return new View.Fate(m, (m.isRanged() ? "sees you in " : "reaches you in ") + c.getTicks() + (c.getTicks() == 1 ? " tick" : " ticks"), false);
		}
	}

	/** A scene tile as fractions of the cave's width and height, for the panel's chart. */
	private double[] fraction(int sceneX, int sceneY)
	{
		WorldPoint p = cave(sceneX, sceneY);
		if (p == null)
		{
			return null;
		}
		return new double[]{CaveMap.fx(p.getX() - CaveMap.BASE_X), CaveMap.fy(p.getY() - CaveMap.BASE_Y)};
	}

	/** A copy of the logbook for the Swing thread, so it never reads one being written. */
	private Logbook copy(Logbook b)
	{
		return gson.fromJson(gson.toJson(b), Logbook.class).repair();
	}

	/** What the panel shows, minus the clock, which only needs a fresh draw every few seconds. */
	private String viewKey()
	{
		StringBuilder k = new StringBuilder();
		k.append(client.getGameState()).append(inCave).append(tracker.rotation())
			.append(tracker.candidates()).append(book == null ? "" : book.attempts + "/" + book.capes)
			.append(book == null || book.active == null ? "" : book.active.wave + "/" + book.active.dosesUsed)
			.append(prayerDoses).append('/').append(restoreDoses).append('/').append(client.getBoostedSkillLevel(Skill.PRAYER));
		k.append('d').append(danger);
		if (!inCave)
		{
			// The cave mouth's ready check and bank count.
			Banked b = banked();
			k.append('f').append(foodCount).append('a').append(ammo).append('w').append(weaponRange).append(weaponName)
				.append('b').append(b == null ? "-" : b.getDoses() + "@" + b.getSavedAt() / 60_000L)
				.append('l').append(client.getRealSkillLevel(Skill.PRAYER));
		}
		PrayerCall c = call;
		if (c != null)
		{
			k.append('p').append(c.getPrayer()).append(c.getAlso()).append(c.getPrayer() == activeProtect());
		}
		for (KillOrder.Target t : order)
		{
			k.append('k').append(t.getFoe().getMonster().ordinal()).append(t.getReason().ordinal());
		}
		StandPlanner.Spot spot = shownSpot();
		if (spot != null)
		{
			k.append(spot.getX()).append(',').append(spot.getY()).append(spot == nextSpot);
		}
		k.append('r').append(rehearsalKey);
		Outlook o = outlook;
		if (o != null)
		{
			// While waves are still being rated, redraw every few of them rather than every one.
			k.append('o').append(o.done() ? o.encode() : String.valueOf(o.rated() / 6));
		}
		Compare cmp = compare;
		if (cmp != null)
		{
			k.append('c').append(cmp.getHeadline());
			for (Compare.Row r : cmp.getRows())
			{
				k.append(r.getHere()).append(r.getThere());
			}
		}
		if (book != null && book.active != null)
		{
			k.append('s').append(book.active.splits.size());
		}
		if (inCave)
		{
			// The run clock ticks on the panel every five seconds or so.
			k.append('t').append(tick / 8);
		}
		return k.toString();
	}

	private void updatePlaque(View v)
	{
		// Nothing on Jad's wave, not even the wave and your doses: the fight is yours to read.
		if (!config.plaque() || v.getState() != View.State.IN_CAVE || v.getWave() == 0 || v.quiet())
		{
			plaque.show(null, null, null, null, null, null, null);
			return;
		}
		String note = v.getRotation() > 0 ? "Rotation " + v.getRotation() : "Reading rotation";
		PrayerPacer.Pace pace = v.getPace();
		String prayer = pace == null ? null : Format.doses(pace.getDoses()) + " (" + Format.spare(pace.getSpare()) + ")";
		View.Call c = v.getCall();
		String call = c == null ? null : (c.isUp() ? "" : "Pray ") + c.getPrayer().getShortName() + (c.isUp() ? " up" : "");
		plaque.show("Wave " + v.getWave(), note, prayer, pace == null ? Theme.TEXT_MUTED : Theme.of(pace.getState()),
			call, c == null ? null : Theme.of(c.getPrayer()), v.getDanger() > 0 ? "Eat: " + v.getDanger() + " can land" : null);
	}

	/** Drops sightings too old to belong to any wave, so the list cannot grow. */
	private void pruneSightings()
	{
		Iterator<Sighting> it = sightings.iterator();
		while (it.hasNext())
		{
			if (it.next().tick < tick - 10)
			{
				it.remove();
			}
		}
	}
}
