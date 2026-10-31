package forge.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.HashMultiset;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameEntity;
import forge.game.GameLogEntry;
import forge.game.GameObject;
import forge.game.GameOutcome;
import forge.game.GameStage;
import forge.game.Match;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.judgetower.Judge;
import forge.game.judgetower.JudgeInfraction;
import forge.game.judgetower.JudgeTowerOptions;
import forge.game.judgetower.table.Observed;
import forge.game.judgetower.table.Observed.CardObs;
import forge.game.judgetower.table.Observed.PlayerObs;
import forge.game.judgetower.table.TableActions;
import forge.game.judgetower.table.TableDiff;
import forge.game.judgetower.table.TableLink;
import forge.game.judgetower.table.TableSeat;
import forge.game.judgetower.table.TableStackItem;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.PlaySpellAbility;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.Trigger;
import forge.game.undo.GameMemento;
import forge.game.undo.UndoManager;
import forge.game.zone.ZoneType;
import forge.LobbyPlayer;

/**
 * Judge Tower played by hand. The hosted game is the physical table, with no rules running and only the primitives of
 * TableActions changing it, and this runs a second, private game, the judge's, where the rules run. Human seats there are
 * answered from the table (PlayerControllerJudge); AI seats are ordinary, and their work is copied over.
 *
 * The engine does a step's turn-based actions first and then lets the humans do them on the table, and compares once
 * state-based actions and triggers are done, before anyone gets priority. A resolution or a cast goes the other way: the
 * human does it on the table, and then the engine does it again and again under a snapshot, going through the decisions
 * it needs, until what it comes to is what's on the table. If there's no way to get there, that's an infraction.
 *
 * Everything runs on the game thread, the table's prompts blocking it while a human works.
 */
public final class TableJudge implements TableLink, Match.GameRunner {
    /** How many ways of carrying something out to try. Past this it's the player's word: nothing proves them wrong. */
    private static final int MAX_ATTEMPTS = 128;
    /** Effects can pick at random in ways that don't go through a player, so a failed search gets a few more goes. */
    private static final int RANDOM_RETRIES = 6;

    private final Function<Player, TableSeat> seatFactory;
    private final Map<Integer, TableSeat> seats = new LinkedHashMap<>();
    private final Map<Integer, Integer> engineToTable = new HashMap<>();
    private final Map<Integer, Card> tableCards = new HashMap<>();
    private final GameMemento.Recorder tableRecorder = new GameMemento.Recorder();

    private Game table;
    private Game engine;
    private JudgeTowerOptions options;
    private UndoManager.Mark checkpointMark;
    private UndoManager.Mark acceptedMark;
    /** The table as it last agreed with the judge: what's different now is what the player has done. */
    private Observed accepted;
    private Observed tableNow;
    private Observed afterEffect;
    private boolean started;
    private boolean dirty;
    private boolean searching;
    private boolean asked;
    private Runnable resolving;
    private int logCopied;
    private Decisions decisions = new Decisions();

    private final Set<TableSeat> windowed = new HashSet<>();
    private final Map<SpellAbility, TableStackItem> declared = new HashMap<>();
    private final Map<TableStackItem, Integer> targetsTaken = new HashMap<>();
    private final List<Boolean> coins = new ArrayList<>();
    private final Map<Integer, List<Integer>> dice = new HashMap<>();
    private final List<String> announced = new ArrayList<>();
    private int coinsUsed;
    private final Map<Integer, Integer> diceUsed = new HashMap<>();

    public TableJudge(final Function<Player, TableSeat> seatFactory0) {
        seatFactory = seatFactory0;
    }

    /** The decisions a re-execution comes to, walked through in order: the first of each, then the next way, and so on. */
    static final class Decisions {
        private final List<Integer> path = new ArrayList<>();
        private final List<Integer> sizes = new ArrayList<>();
        private int position;

        int pick(final int options) {
            if (options <= 1) {
                return 0;
            }
            final int i = position++;
            if (i == path.size()) {
                path.add(0);
                sizes.add(options);
            } else {
                sizes.set(i, options);
            }
            return Math.min(path.get(i), options - 1);
        }

        void restart() {
            position = 0;
        }

        boolean next() {
            while (position < path.size()) {
                path.remove(path.size() - 1);
                sizes.remove(sizes.size() - 1);
            }
            for (int i = path.size() - 1; i >= 0; i--) {
                if (path.get(i) + 1 < sizes.get(i)) {
                    path.set(i, path.get(i) + 1);
                    return true;
                }
                path.remove(i);
                sizes.remove(i);
            }
            return false;
        }

        List<Integer> snapshot() {
            return new ArrayList<>(path);
        }

        void replay(final List<Integer> ways) {
            path.clear();
            sizes.clear();
            path.addAll(ways);
            for (int i = 0; i < ways.size(); i++) {
                sizes.add(ways.get(i) + 1);
            }
            position = 0;
        }
    }

    // ---------------------------------------------------------------- running the game

    @Override
    public void run(final Game hosted, final GameOutcome lastOutcome, final Runnable startGameHook) {
        table = hosted;
        options = table.getRules().getJudgeTowerOptions();
        table.getJudge().setTableLink(this);
        table.setAge(GameStage.Play);
        int seat = 0;
        for (final Player p : table.getRegisteredPlayers()) {
            if (!p.getController().isAI()) {
                seats.put(seat, seatFactory.apply(p));
            }
            seat++;
        }
        final List<RegisteredPlayer> judged = new ArrayList<>();
        seat = 0;
        for (final RegisteredPlayer rp : hosted.getMatch().getPlayers()) {
            final LobbyPlayer lobby = seats.containsKey(seat) ? new LobbyPlayerJudge(rp.getPlayer().getName(), this) : rp.getPlayer();
            final RegisteredPlayer judgedSeat = new RegisteredPlayer(rp.getDeck()).setPlayer(lobby);
            judgedSeat.setTeamNumber(rp.getTeamNumber());
            judged.add(judgedSeat);
            seat++;
        }
        final Match match = new Match(table.getRules(), judged, "Judge");
        match.setZonesPreparedHook(this::onZonesPrepared);
        final Game judgeGame = match.createGame();
        judgeGame.AI_TIMEOUT = table.AI_TIMEOUT;
        match.startGame(judgeGame, startGameHook);
        finish();
    }

    /** The judge's game has been dealt its Tower: it takes the table's order, and each card learns its other self. */
    private void onZonesPrepared(final Game game) {
        engine = game;
        engine.getJudge().setTableLink(this);
        for (final Card c : table.getCardsInGame()) {
            tableCards.put(c.getId(), c);
        }
        final Map<String, List<Card>> byName = new HashMap<>();
        for (final Card c : engine.getRegisteredPlayers().get(0).getCardsIn(ZoneType.Library)) {
            byName.computeIfAbsent(c.getName(), k -> new ArrayList<>()).add(c);
        }
        final CardCollection order = new CardCollection();
        for (final Card t : table.getRegisteredPlayers().get(0).getCardsIn(ZoneType.Library)) {
            final Card e = byName.get(t.getName()).remove(0);
            engineToTable.put(e.getId(), t.getId());
            order.add(e);
        }
        engine.getRegisteredPlayers().get(0).getZone(ZoneType.Library).setCards(order);
        for (int i = 0; i < engine.getRegisteredPlayers().size(); i++) {
            pairByName(engine.getRegisteredPlayers().get(i).getCardsIn(ZoneType.Battlefield), table.getRegisteredPlayers().get(i).getCardsIn(ZoneType.Battlefield));
        }
    }

    private void pairByName(final Iterable<Card> engineCards, final Iterable<Card> tableList) {
        final List<Card> left = new ArrayList<>();
        tableList.forEach(left::add);
        for (final Card e : engineCards) {
            for (final Card t : left) {
                if (t.getName().equals(e.getName())) {
                    engineToTable.put(e.getId(), t.getId());
                    left.remove(t);
                    break;
                }
            }
        }
    }

    private void finish() {
        if (table.isGameOver()) {
            return;
        }
        copyLosses();
        mirrorAll();
        final GameOutcome outcome = engine.getOutcome();
        table.setGameOver(outcome != null ? outcome.getWinCondition() : GameEndReason.Draw);
    }

    private void copyLosses() {
        for (int i = 0; i < engine.getRegisteredPlayers().size(); i++) {
            final Player e = engine.getRegisteredPlayers().get(i);
            final Player t = table.getRegisteredPlayers().get(i);
            if (e.hasLost() && !t.hasLost()) {
                t.loseConditionMet(e.getOutcome().lossState, e.getOutcome().loseConditionSpell);
                table.onPlayerLost(t);
                seats.remove(i);
            }
        }
    }

    /** The app's way out: a concession, or the match being closed. */
    private void checkTableStillPlaying() {
        if (table.isGameOver() && !engine.isGameOver()) {
            engine.setGameOver(GameEndReason.Draw);
            return;
        }
        for (int i = 0; i < table.getRegisteredPlayers().size(); i++) {
            final Player t = table.getRegisteredPlayers().get(i);
            final Player e = engine.getRegisteredPlayers().get(i);
            if (t.hasLost() && !e.hasLost()) {
                e.concede();
                engine.getAction().checkGameOverCondition();
                seats.remove(i);
            }
        }
    }

    // ---------------------------------------------------------------- seats and ids

    private int seatOf(final Player enginePlayer) {
        return engine.getRegisteredPlayers().indexOf(enginePlayer);
    }

    TableSeat seat(final Player enginePlayer) {
        return seats.get(seatOf(enginePlayer));
    }

    private boolean isHuman(final int seat) {
        return seats.containsKey(seat);
    }

    private Player tablePlayer(final Player enginePlayer) {
        return table.getRegisteredPlayers().get(seatOf(enginePlayer));
    }

    private Player tablePlayer(final int seat) {
        return table.getRegisteredPlayers().get(seat);
    }

    private Player enginePlayer(final Player tablePlayer) {
        return engine.getRegisteredPlayers().get(table.getRegisteredPlayers().indexOf(tablePlayer));
    }

    private int tableId(final Card engineCard) {
        final Integer id = engineToTable.get(engineCard.getId());
        return id != null ? id : -engineCard.getId() - 1;
    }

    Card tableCard(final Card engineCard) {
        return tableCards.get(tableId(engineCard));
    }

    private Card engineCard(final int tableId) {
        for (final Map.Entry<Integer, Integer> e : engineToTable.entrySet()) {
            if (e.getValue() == tableId) {
                final Card found = engine.findById(e.getKey());
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Observed readTable() {
        return Observed.read(table, Card::getId, options.life != JudgeTowerOptions.LifeRule.INFINITE);
    }

    private Observed readEngine() {
        return Observed.read(engine, this::tableId, options.life != JudgeTowerOptions.LifeRule.INFINITE);
    }

    /** New on both sides since they last agreed: a token the player made and the one the engine made. */
    private void pairNewCards(final Observed tableSide) {
        final Set<Integer> known = new HashSet<>(engineToTable.values());
        final Map<String, List<Integer>> free = new HashMap<>();
        for (final CardObs t : tableSide.cards.values()) {
            if (!known.contains(t.id())) {
                free.computeIfAbsent(t.name() + "/" + t.zone(), k -> new ArrayList<>()).add(t.id());
            }
        }
        for (final CardObs j : readEngine().cards.values()) {
            if (j.id() < 0) {
                final List<Integer> ids = free.get(j.name() + "/" + j.zone());
                if (ids != null && !ids.isEmpty()) {
                    engineToTable.put(-j.id() - 1, ids.remove(0));
                }
            }
        }
    }

    // ---------------------------------------------------------------- the engine's moments

    @Override
    public void afterStepBegan() {
        if (searching) {
            return;
        }
        final PhaseHandler ph = engine.getPhaseHandler();
        if (!started) {
            // the game's opening is the engine's, and nothing could be done about it yet
            started = true;
            mirrorAll();
            return;
        }
        final PhaseType phase = ph.getPhase();
        final int activeSeat = seatOf(ph.getPlayerTurn());
        final TableSeat active = seats.get(activeSeat);
        if (active != null && (phase == PhaseType.UNTAP || phase == PhaseType.UPKEEP)) {
            active.stopAutoOk();
        }
        if (active == null) {
            mirrorAll();
        } else {
            mirrorAiSeats();
        }
        syncMarker();

        final boolean hasPriority = phase != PhaseType.UNTAP && phase != PhaseType.CLEANUP;
        final String prompt = phase.nameForUi + ": carry out the step, then OK.";
        if (hasPriority) {
            for (final Player p : engine.getPlayersInTurnOrder(ph.getPlayerTurn())) {
                final TableSeat s = seats.get(seatOf(p));
                if (s != null && !windowed.contains(s)) {
                    s.window(TableSeat.Window.STEP, prompt);
                    checkTableStillPlaying();
                }
            }
            dirty = true;
        } else if (active != null) {
            if (!windowed.contains(active)) {
                active.window(TableSeat.Window.STEP, prompt);
                checkTableStillPlaying();
            }
            windowed.clear();
            if (!engine.isGameOver()) {
                check(null, false);
            }
        }
        windowed.clear();
    }

    @Override
    public void beforePriority(final Player player) {
        if (searching) {
            return;
        }
        if (dirty && !engine.isGameOver()) {
            dirty = false;
            check(null, false);
        }
    }

    @Override
    public void afterAiAction() {
        if (searching) {
            return;
        }
        for (final TableSeat s : seats.values()) {
            s.stopAutoOk();
        }
        mirrorAll();
        settleWindows();
        dirty = true;
    }

    private void settleWindows() {
        for (final Player p : engine.getPlayersInTurnOrder(engine.getPhaseHandler().getPlayerTurn())) {
            final TableSeat s = seats.get(seatOf(p));
            if (s != null) {
                s.window(TableSeat.Window.SETTLE, "Anything of yours to do for what just happened? Then OK.");
                checkTableStillPlaying();
            }
        }
    }

    @Override
    public void resolveTop() {
        final SpellAbilityStackInstance top = engine.getStack().peek();
        final Player controller = top.getSpellAbility().getActivatingPlayer();
        final TableSeat seat = seat(controller);
        if (seat == null) {
            // the AI's, carried out by the engine; a human's decisions in it are asked for
            engine.getStack().resolveStack();
            mirrorAll();
            settleWindows();
            dirty = true;
            return;
        }
        final Runnable action = () -> {
            afterEffect = null;
            engine.getStack().resolveStack();
            afterEffect = readEngine();
            engine.getPhaseHandler().settleForTable();
        };
        resolving = action;
        asked = false;
        syncMarker();
        final List<SpellAbilityStackInstance> onTableBefore = table.getStack().asList();
        final SpellAbilityStackInstance topOnTable = onTableBefore.isEmpty() ? null : onTableBefore.get(0);
        seat.window(TableSeat.Window.RESOLVE, "Resolve " + top.getSourceCard().getName() + " by hand, then OK: " + top.getStackDescription());
        checkTableStillPlaying();
        if (engine.isGameOver()) {
            return;
        }
        // an ability just goes; a spell goes when its card is put where it belongs
        if (topOnTable != null && !topOnTable.getSpellAbility().isSpell() && table.getStack().asList().contains(topOnTable)) {
            TableActions.removeFromStack(table, topOnTable);
        }
        for (final Player p : engine.getPlayersInTurnOrder(engine.getPhaseHandler().getPlayerTurn())) {
            final TableSeat s = seats.get(seatOf(p));
            if (s != null && s != seat) {
                s.window(TableSeat.Window.SETTLE, "Anything of yours to do for what's resolving? Then OK.");
                checkTableStillPlaying();
            }
        }
        if (!engine.isGameOver()) {
            search(controller, action, false);
        }
        resolving = null;
    }

    /** A human has played something, its declarations and costs on the table. */
    boolean play(final PlayerControllerJudge controller, final Player player, final SpellAbility sa) {
        final boolean[] result = new boolean[1];
        search(player, () -> {
            afterEffect = null;
            result[0] = PlaySpellAbility.playSpellAbility(controller, player, sa);
            if (!result[0]) {
                throw engine.getJudge().cannotBeDone(player, sa);
            }
        }, true);
        // what comes of it (a trigger, a zone change's trigger) is for the check before the next priority
        dirty = true;
        return result[0];
    }

    /** They have priority. */
    List<SpellAbility> priority(final Player player) {
        final TableSeat seat = seat(player);
        if (seat == null) {
            return null;
        }
        syncMarker();
        table.getPhaseHandler().setPriority(tablePlayer(player));
        final SpellAbility sa = seat.priority(engine.getPhaseHandler().getPhase().nameForUi + ": you have priority.");
        checkTableStillPlaying();
        if (engine.isGameOver() || player.hasLost()) {
            return null;
        }
        if (sa == null) {
            // a pass is a commitment to what's on the table
            check(player, false);
            return null;
        }
        return new ArrayList<>(List.of(sa));
    }

    void openWindow(final Player player, final TableSeat.Window kind, final String prompt) {
        final TableSeat seat = seat(player);
        if (seat == null) {
            return;
        }
        if (engine.getPhaseHandler().getPlayerTurn() != player && seats.get(seatOf(engine.getPhaseHandler().getPlayerTurn())) == null) {
            mirrorAll();
        }
        syncMarker();
        seat.window(kind, prompt);
        windowed.add(seat);
        checkTableStillPlaying();
    }

    // ---------------------------------------------------------------- comparing

    private record Verdict(List<TableDiff.Item> charges, List<Integer> chargedTo, List<TableDiff.Item> mirrored) { }

    private void check(final Player resolver, final boolean ignoreStack) {
        final Observed t = readTable();
        pairNewCards(t);
        final Verdict v = judge(t, readEngine(), resolver, ignoreStack);
        if (v.charges().isEmpty()) {
            accept(v, ignoreStack);
        } else {
            rule(v, resolver);
        }
    }

    private Verdict judge(final Observed t, final Observed e, final Player resolver, final boolean ignoreStack) {
        final Set<String> differedAfterEffect = new HashSet<>();
        if (afterEffect != null) {
            for (final TableDiff.Item item : TableDiff.compare(t, afterEffect)) {
                differedAfterEffect.add(item.key());
            }
        }
        final List<TableDiff.Item> charges = new ArrayList<>();
        final List<Integer> chargedTo = new ArrayList<>();
        final List<TableDiff.Item> mirrored = new ArrayList<>();
        final int resolverSeat = resolver == null ? -1 : seatOf(resolver);
        final int activeSeat = seatOf(engine.getPhaseHandler().getPlayerTurn());
        for (final TableDiff.Item item : TableDiff.compare(t, e)) {
            // what happens to what's left of someone out of the game is nobody's to answer for
            if (item.seat() >= 0 && engine.getRegisteredPlayers().get(item.seat()).hasLost()) {
                mirrored.add(item);
                continue;
            }
            if (item.kind() == TableDiff.Kind.STACK) {
                if (ignoreStack) {
                    continue;
                }
                if (isHuman(item.seat())) {
                    charges.add(item);
                    chargedTo.add(item.seat());
                } else {
                    mirrored.add(item);
                }
                continue;
            }
            if (item.kind() == TableDiff.Kind.TOWER) {
                final int seat = resolverSeat >= 0 ? resolverSeat : activeSeat;
                if (isHuman(seat)) {
                    charges.add(item);
                    chargedTo.add(seat);
                } else {
                    mirrored.add(item);
                }
                continue;
            }
            final boolean ai = !isHuman(item.seat());
            // the effect's part was right, so what's left is state-based actions and triggers: its owner's to do
            final boolean effectWasRight = afterEffect != null && !differedAfterEffect.contains(item.key());
            if (resolverSeat < 0) {
                if (ai) {
                    mirrored.add(item);
                } else {
                    charges.add(item);
                    chargedTo.add(item.seat());
                }
            } else if (ai) {
                if (asked || effectWasRight) {
                    mirrored.add(item);
                } else {
                    charges.add(item);
                    chargedTo.add(resolverSeat);
                }
            } else {
                charges.add(item);
                chargedTo.add(effectWasRight ? item.seat() : resolverSeat);
            }
        }
        return new Verdict(charges, chargedTo, mirrored);
    }

    private void accept(final Verdict v, final boolean keepStack) {
        if (!v.mirrored().isEmpty()) {
            final Observed e = readEngine();
            ensureCounterparts();
            for (final TableDiff.Item item : v.mirrored()) {
                mirrorItem(item, e);
            }
        }
        if (!keepStack) {
            rebuildStack();
        }
        rebuildCombat();
        markAccepted();
    }

    private void markAccepted() {
        accepted = readTable();
        acceptedMark = new UndoManager.Mark(tableRecorder.capture(table), table.getGameLog().size());
        announced.clear();
        coins.clear();
        dice.clear();
        coinsUsed = 0;
        diceUsed.clear();
        declared.clear();
        targetsTaken.clear();
        asked = false;
        afterEffect = null;
        tableNow = null;
    }

    private void rule(final Verdict v, final Player resolver) {
        final StringBuilder reason = new StringBuilder();
        final int seat = v.chargedTo().get(0);
        final Player offender = engine.getRegisteredPlayers().get(seat);
        reason.append(offender.getName()).append(": ").append(v.charges().get(0).text());
        if (v.charges().size() > 1) {
            reason.append(" (and ").append(v.charges().size() - 1).append(" more)");
        }
        final TableSeat s = seats.get(seat);
        if (s != null && s.appeal(reason.toString())) {
            forceEngineToTable(v);
            return;
        }
        throw new JudgeInfraction(offender, "table:" + engine.getPhaseHandler().getTurn(), reason.toString(), true);
    }

    /** Overruled: the table stands, the engine having got it wrong (a script's bug, say). What's on the stack stays the engine's. */
    private void forceEngineToTable(final Verdict v) {
        final Observed t = readTable();
        for (final TableDiff.Item item : v.charges()) {
            switch (item.kind()) {
                case ZONE -> {
                    final Card e = engineCard(item.id());
                    final CardObs o = t.cards.get(item.id());
                    if (e != null && o != null) {
                        final Player to = o.zoneSeat() >= 0 ? engine.getRegisteredPlayers().get(o.zoneSeat()) : e.getOwner();
                        engine.getAction().moveTo(to.getZone(o.zone()), e, null, AbilityKey.newMap());
                    }
                }
                case TAPPED -> {
                    final Card e = engineCard(item.id());
                    if (e != null) {
                        e.setTapped(t.cards.get(item.id()).tapped());
                    }
                }
                case COUNTERS -> {
                    final Card e = engineCard(item.id());
                    final Card c = tableCards.get(item.id());
                    if (e != null && c != null) {
                        e.setCounters(HashMultiset.create(c.getCounters()));
                    }
                }
                case PLAYER -> {
                    final int playerSeat = item.id() / 10;
                    final PlayerObs o = t.players.get(playerSeat);
                    final Player p = engine.getRegisteredPlayers().get(playerSeat);
                    p.setLife(o.life(), null);
                    p.setCounters(HashMultiset.create(tablePlayer(playerSeat).getCounters()));
                }
                case TOWER -> {
                    final CardCollection order = new CardCollection();
                    for (final int id : t.tower) {
                        final Card e = engineCard(id);
                        if (e != null) {
                            order.add(e);
                        }
                    }
                    engine.getRegisteredPlayers().get(0).getZone(ZoneType.Library).setCards(order);
                }
                default -> { }
            }
        }
        // anything that can't be forced, the table takes the engine's word for
        mirrorAll();
    }

    /** Going through the ways of doing it, until one of them comes to what's on the table. */
    private void search(final Player resolver, final Runnable action, final boolean ignoreStack) {
        final UndoManager.Mark base = new UndoManager.Mark(new GameMemento.Recorder().capture(engine), engine.getGameLog().size());
        decisions = new Decisions();
        tableNow = readTable();
        final Map<Integer, Integer> pairsBefore = new HashMap<>(engineToTable);
        JudgeInfraction thrown = null;
        Verdict best = null;
        List<Integer> bestWay = null;
        boolean outOfBudget = false;
        int retries = 0;
        searching = true;
        try {
            for (int attempt = 0;; attempt++) {
                startAttempt();
                boolean threw = false;
                try {
                    action.run();
                } catch (final JudgeInfraction inf) {
                    threw = true;
                    if (thrown == null) {
                        thrown = inf;
                    }
                }
                if (!threw) {
                    pairNewCards(tableNow);
                    final Verdict v = judge(tableNow, readEngine(), resolver, ignoreStack);
                    if (v.charges().isEmpty()) {
                        searching = false;
                        accept(v, ignoreStack);
                        return;
                    }
                    if (best == null || v.charges().size() < best.charges().size()) {
                        best = v;
                        bestWay = decisions.snapshot();
                    }
                }
                engine.getUndoManager().rollbackTo(engine, base);
                engineToTable.clear();
                engineToTable.putAll(pairsBefore);
                if (attempt + 1 >= MAX_ATTEMPTS) {
                    outOfBudget = true;
                    break;
                }
                if (!decisions.next()) {
                    if (thrown != null || retries++ >= RANDOM_RETRIES) {
                        break;
                    }
                }
            }
        } finally {
            searching = false;
        }
        if (thrown != null) {
            throw thrown;
        }
        // the closest it came, so the ruling is about that, and an overruling starts from there
        searching = true;
        decisions.replay(bestWay);
        startAttempt();
        try {
            action.run();
        } finally {
            searching = false;
        }
        pairNewCards(tableNow);
        final Verdict v = judge(tableNow, readEngine(), resolver, ignoreStack);
        if (outOfBudget) {
            engine.getGameLog().add(forge.game.GameLogEntryType.INFORMATION, "The judge couldn't check this all the way: the table stands.");
            forceEngineToTable(v);
            return;
        }
        rule(v, resolver);
    }

    private void startAttempt() {
        decisions.restart();
        coinsUsed = 0;
        diceUsed.clear();
        declared.clear();
        targetsTaken.clear();
        afterEffect = null;
    }

    boolean isSearching() {
        return searching;
    }

    Decisions decisions() {
        return decisions;
    }

    // ---------------------------------------------------------------- what the table shows, for the decisions

    private Observed tableView() {
        return tableNow != null ? tableNow : readTable();
    }

    /** Of these cards in the judge's game, the ones the player has done something with since the table last agreed. */
    CardCollection touched(final Iterable<Card> options0) {
        final CardCollection result = new CardCollection();
        final Observed now = tableView();
        for (final Card c : options0) {
            final int id = tableId(c);
            final CardObs was = accepted == null ? null : accepted.cards.get(id);
            final CardObs is = now.cards.get(id);
            if (is != null && !is.equals(was)) {
                result.add(c);
            }
        }
        return result;
    }

    ZoneType zoneOnTable(final Card engineCardNow) {
        final CardObs is = tableView().cards.get(tableId(engineCardNow));
        return is == null ? null : is.zone();
    }

    /** Where a card is in the Tower on the table, top being 0; -1 if it isn't there. */
    int towerPosition(final Card engineCardNow) {
        return tableView().tower.indexOf(tableId(engineCardNow));
    }

    /** Where a card's item is on the table's stack, top being 0; a large number if it has none. */
    int stackPosition(final SpellAbility sa) {
        final List<SpellAbilityStackInstance> items = table.getStack().asList();
        final int id = tableId(sa.getHostCard());
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getSourceCard().getId() == id) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    List<String> announcedStrings() {
        return announced;
    }

    boolean wasAnnounced(final String text) {
        for (final String a : announced) {
            if (a.equalsIgnoreCase(text)) {
                return true;
            }
        }
        return false;
    }

    /** The table's stack item this spell or ability is, if there's one the player put there. */
    TableStackItem declaredFor(final SpellAbility sa) {
        final SpellAbility root = sa.getRootAbility();
        final TableStackItem known = declared.get(root);
        if (known != null) {
            return known;
        }
        final int id = tableId(root.getHostCard());
        for (final SpellAbilityStackInstance si : table.getStack().asList()) {
            if (si.getSpellAbility() instanceof TableStackItem item && si.getSourceCard().getId() == id
                    && item.isSpell() == root.isSpell() && !declared.containsValue(item)) {
                declared.put(root, item);
                return item;
            }
        }
        return null;
    }

    /** The next of what's declared for this item, in the judge's game. */
    List<GameObject> takeTargets(final TableStackItem item, final int max) {
        final List<GameObject> result = new ArrayList<>();
        final List<GameObject> all = new ArrayList<>();
        item.getTargets().forEach(all::add);
        int taken = targetsTaken.getOrDefault(item, 0);
        while (taken < all.size() && result.size() < max) {
            final GameObject mapped = toEngine(all.get(taken));
            if (mapped != null) {
                result.add(mapped);
            }
            taken++;
        }
        targetsTaken.put(item, taken);
        return result;
    }

    private GameObject toEngine(final GameObject tableObject) {
        if (tableObject instanceof Card c) {
            return engineCard(c.getId());
        }
        if (tableObject instanceof Player p) {
            return enginePlayer(p);
        }
        if (tableObject instanceof SpellAbility sa) {
            final Card host = engineCard(sa.getHostCard().getId());
            for (final SpellAbilityStackInstance si : engine.getStack()) {
                if (host != null && si.getSourceCard().equals(host)) {
                    return si.getSpellAbility();
                }
            }
        }
        return null;
    }

    /** What's marked on the table's combat, in the judge's game. */
    Map<Card, GameEntity> attacksOnTable() {
        final Map<Card, GameEntity> result = new LinkedHashMap<>();
        final Combat combat = table.getPhaseHandler().getCombat();
        if (combat == null) {
            return result;
        }
        for (final Card a : combat.getAttackers()) {
            final Card e = engineCard(a.getId());
            final GameEntity d = combat.getDefenderByAttacker(a);
            final GameObject def = toEngine(d);
            if (e != null && def instanceof GameEntity entity) {
                result.put(e, entity);
            }
        }
        return result;
    }

    Map<Card, List<Card>> blocksOnTable(final Player blocker) {
        final Map<Card, List<Card>> result = new LinkedHashMap<>();
        final Combat combat = table.getPhaseHandler().getCombat();
        if (combat == null) {
            return result;
        }
        for (final Card b : combat.getAllBlockers()) {
            final Card e = engineCard(b.getId());
            if (e == null || !tablePlayer(blocker).equals(b.getController())) {
                continue;
            }
            for (final Card a : combat.getAttackersBlockedBy(b)) {
                final Card ea = engineCard(a.getId());
                if (ea != null) {
                    result.computeIfAbsent(e, k -> new ArrayList<>()).add(ea);
                }
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- the table takes the engine's word

    private void ensureCounterparts() {
        for (final CardObs j : readEngine().cards.values()) {
            if (j.id() < 0) {
                final Card e = engine.findById(-j.id() - 1);
                if (e == null) {
                    continue;
                }
                final Card t = TableActions.newToken(table, tablePlayer(e.getController()), e.getName(), e.getImageKey(),
                        e.getType().toString(), e.getNetPower(), e.getNetToughness());
                engineToTable.put(e.getId(), t.getId());
                tableCards.put(t.getId(), t);
            }
        }
    }

    void mirrorAll() {
        ensureCounterparts();
        final Observed e = readEngine();
        final Observed t = readTable();
        for (final CardObs j : e.cards.values()) {
            mirrorCard(j);
        }
        for (final CardObs c : t.cards.values()) {
            if (!e.cards.containsKey(c.id()) && c.zone() != ZoneType.Stack) {
                final Card gone = tableCards.get(c.id());
                if (gone != null && gone.getZone() != null) {
                    gone.getZone().remove(gone);
                }
            }
        }
        mirrorTower(e);
        for (final PlayerObs o : e.players) {
            mirrorPlayer(o);
        }
        rebuildStack();
        rebuildCombat();
        copyLog();
        syncMarker();
        revealPublicCards();
        markAccepted();
    }

    private void mirrorAiSeats() {
        final Observed t = readTable();
        pairNewCards(t);
        final Observed e = readEngine();
        ensureCounterparts();
        boolean any = false;
        for (final TableDiff.Item item : TableDiff.compare(readTable(), e)) {
            if (item.kind() != TableDiff.Kind.STACK && item.kind() != TableDiff.Kind.TOWER && !isHuman(item.seat())) {
                mirrorItem(item, e);
                any = true;
            }
        }
        if (any) {
            rebuildCombat();
            copyLog();
            markAccepted();
        }
        revealPublicCards();
    }

    private void mirrorItem(final TableDiff.Item item, final Observed e) {
        switch (item.kind()) {
            case TOWER -> mirrorTower(e);
            case PLAYER -> mirrorPlayer(e.players.get(item.id() / 10));
            case STACK -> { }
            case EXTRA -> {
                final Card gone = tableCards.get(item.id());
                if (gone != null && gone.getZone() != null) {
                    gone.getZone().remove(gone);
                }
            }
            default -> {
                final CardObs j = e.cards.get(item.id());
                if (j != null) {
                    mirrorCard(j);
                }
            }
        }
    }

    private void mirrorCard(final CardObs j) {
        final Card c = tableCards.get(j.id());
        final Card e = engineCard(j.id());
        if (c == null || e == null) {
            return;
        }
        final Player zoneOwner = j.zoneSeat() >= 0 ? tablePlayer(j.zoneSeat()) : j.owner() >= 0 ? tablePlayer(j.owner()) : c.getOwner();
        if (j.owner() >= 0 && !c.getOwner().equals(tablePlayer(j.owner()))) {
            c.setOwner(tablePlayer(j.owner()));
        }
        final boolean movedZone = c.getZone() == null || c.getZone().getZoneType() != j.zone()
                || (j.zoneSeat() >= 0 && !c.getZone().getPlayer().equals(zoneOwner));
        if (movedZone) {
            TableActions.move(table, c, j.zone(), zoneOwner, null);
        }
        if (j.zone() != ZoneType.Battlefield) {
            return;
        }
        if (c.isTapped() != j.tapped()) {
            TableActions.setTapped(table, c, j.tapped());
        }
        if (c.isPhasedOut() != j.phasedOut()) {
            TableActions.setPhasedOut(table, c, j.phasedOut());
        }
        if (c.isFaceDown() != j.faceDown()) {
            TableActions.setFaceDown(table, c, j.faceDown());
        }
        if (!j.faceDown() && !c.getCurrentStateName().name().equals(j.state()) && c.hasState(CardStateName.smartValueOf(j.state()))) {
            TableActions.showState(table, c, CardStateName.smartValueOf(j.state()));
        }
        if (!HashMultiset.create(c.getCounters()).equals(HashMultiset.create(e.getCounters()))) {
            c.setCounters(HashMultiset.create(e.getCounters()));
        }
        // nobody's held to it, but it's as well to see what the AI's creatures have been hit with
        TableActions.setDamage(table, c, e.getDamage());
        final GameEntity on = e.getEntityAttachedTo();
        final GameEntity tableOn = on instanceof Card a ? tableCard(a) : on instanceof Player p ? tablePlayer(p) : null;
        if (tableOn == null) {
            TableActions.detach(table, c);
        } else if (!tableOn.equals(c.getEntityAttachedTo())) {
            TableActions.attach(table, c, tableOn);
        }
    }

    private void mirrorTower(final Observed e) {
        final CardCollection order = new CardCollection();
        for (final int id : e.tower) {
            final Card c = tableCards.get(id);
            if (c != null) {
                order.add(c);
            }
        }
        if (!order.isEmpty() || !e.tower.isEmpty()) {
            TableActions.setTowerOrder(table, table.getRegisteredPlayers().get(0), order);
        }
    }

    private void mirrorPlayer(final PlayerObs o) {
        final Player t = tablePlayer(o.seat());
        final Player e = engine.getRegisteredPlayers().get(o.seat());
        if (options.life != JudgeTowerOptions.LifeRule.INFINITE && t.getLife() != e.getLife()) {
            t.setLife(e.getLife(), null);
        }
        if (!HashMultiset.create(t.getCounters()).equals(HashMultiset.create(e.getCounters()))) {
            t.setCounters(HashMultiset.create(e.getCounters()));
        }
    }

    /** What's on the stack, as the engine has it: its wording, and everything on it matched up. */
    private void rebuildStack() {
        table.getStack().clear();
        final List<SpellAbilityStackInstance> items = new ArrayList<>();
        engine.getStack().forEach(items::add);
        Collections.reverse(items);
        for (final SpellAbilityStackInstance si : items) {
            final Card source = tableCard(si.getSourceCard());
            if (source == null) {
                continue;
            }
            final List<Object> targets = new ArrayList<>();
            for (SpellAbility sa = si.getSpellAbility(); sa != null; sa = sa.getSubAbility()) {
                for (final GameObject target : sa.getTargets()) {
                    if (target instanceof Card c && tableCard(c) != null) {
                        targets.add(tableCard(c));
                    } else if (target instanceof Player p) {
                        targets.add(tablePlayer(p));
                    } else if (target instanceof SpellAbility on) {
                        final Card host = tableCard(on.getHostCard());
                        for (final SpellAbilityStackInstance t : table.getStack().asList()) {
                            if (t.getSourceCard().equals(host)) {
                                targets.add(t.getSpellAbility());
                                break;
                            }
                        }
                    }
                }
            }
            TableActions.pushStack(table, source, tablePlayer(si.getSpellAbility().getActivatingPlayer()), si.isSpell(), si.getStackDescription(), targets);
        }
    }

    private void rebuildCombat() {
        final Combat ec = engine.getPhaseHandler().getCombat();
        if (ec == null) {
            if (table.getPhaseHandler().getCombat() != null) {
                table.getPhaseHandler().setCombat(null);
                table.updateCombatForView();
                table.fireEvent(new forge.game.event.GameEventCombatChanged());
            }
            return;
        }
        final Combat tc = new Combat(tablePlayer(ec.getAttackingPlayer()));
        for (final Card a : ec.getAttackers()) {
            final GameEntity d = ec.getDefenderByAttacker(a);
            final GameEntity td = d instanceof Card c ? tableCard(c) : d instanceof Player p ? tablePlayer(p) : null;
            if (tableCard(a) != null && td != null) {
                tc.addAttacker(tableCard(a), td);
            }
        }
        for (final Card b : ec.getAllBlockers()) {
            for (final Card a : ec.getAttackersBlockedBy(b)) {
                if (tableCard(a) != null && tableCard(b) != null) {
                    tc.addBlocker(tableCard(a), tableCard(b));
                }
            }
        }
        table.getPhaseHandler().setCombat(tc);
        table.updateCombatForView();
        table.fireEvent(new forge.game.event.GameEventCombatChanged());
    }

    private void copyLog() {
        final List<GameLogEntry> entries = engine.getGameLog().getLogEntries(null);
        // newest first
        if (logCopied > entries.size()) {
            logCopied = entries.size();
        }
        for (int i = entries.size() - logCopied - 1; i >= 0; i--) {
            table.getGameLog().add(entries.get(i).type(), entries.get(i).message());
        }
        logCopied = entries.size();
    }

    /** Where the turn is: the marker, which isn't anything a player can get wrong. */
    private void syncMarker() {
        final PhaseHandler ph = engine.getPhaseHandler();
        if (ph.getPhase() == null || ph.getPlayerTurn() == null) {
            return;
        }
        final PhaseHandler tph = table.getPhaseHandler();
        if (tph.getPhase() != ph.getPhase() || tph.getTurn() != ph.getTurn() || !tablePlayer(ph.getPlayerTurn()).equals(tph.getPlayerTurn())) {
            tph.devModeSet(ph.getPhase(), tablePlayer(ph.getPlayerTurn()), false, ph.getTurn());
        }
        revealPublicCards();
    }

    /** Nothing on the table runs statics, so what the format makes public has to be shown by hand. */
    private void revealPublicCards() {
        if (options.hidden == JudgeTowerOptions.HiddenInfo.NORMAL) {
            return;
        }
        for (final Player p : table.getRegisteredPlayers()) {
            for (final Card c : p.getCardsIn(ZoneType.Hand)) {
                TableActions.reveal(table, c);
            }
            if (options.hidden == JudgeTowerOptions.HiddenInfo.ALL_PUBLIC) {
                for (final Card c : p.getCardsIn(ZoneType.Exile)) {
                    TableActions.reveal(table, c);
                }
                if (!p.getCardsIn(ZoneType.Library).isEmpty()) {
                    TableActions.reveal(table, p.getCardsIn(ZoneType.Library).get(0));
                }
            }
        }
    }

    // ---------------------------------------------------------------- the engine's checkpoints

    @Override
    public void onCheckpoint() {
        if (searching) {
            return;
        }
        checkpointMark = new UndoManager.Mark(tableRecorder.capture(table), table.getGameLog().size());
    }

    @Override
    public void onRolledBack() {
        searching = false;
        resolving = null;
        dirty = false;
        windowed.clear();
        if (checkpointMark != null) {
            table.getUndoManager().rollbackTo(table, checkpointMark);
        }
        markAccepted();
    }

    @Override
    public void onRuled() {
        copyLosses();
        mirrorAll();
    }

    @Override
    public List<Card> adoptShuffle(final List<Card> cards) {
        if (!searching) {
            return null;
        }
        final List<Card> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> {
            final int pa = towerPosition(a);
            final int pb = towerPosition(b);
            return Integer.compare(pa < 0 ? Integer.MAX_VALUE : pa, pb < 0 ? Integer.MAX_VALUE : pb);
        });
        return sorted;
    }

    @Override
    public Boolean nextCoin() {
        if (!searching || coinsUsed >= coins.size()) {
            return null;
        }
        return coins.get(coinsUsed++);
    }

    @Override
    public Integer nextDie(final int sides) {
        final List<Integer> rolled = dice.get(sides);
        final int used = diceUsed.getOrDefault(sides, 0);
        if (!searching || rolled == null || used >= rolled.size()) {
            return null;
        }
        diceUsed.put(sides, used + 1);
        return rolled.get(used);
    }

    // ---------------------------------------------------------------- for the table's prompts

    @Override
    public List<SpellAbility> offeredAbilities(final Card tableCardNow, final Player tablePlayerNow) {
        final Card e = engineCard(tableCardNow.getId());
        if (e == null) {
            return List.of();
        }
        final List<SpellAbility> abilities = e.getAllPossibleAbilities(enginePlayer(tablePlayerNow), false);
        // what a card can do once it's cast is what's on offer for it there, not casting it over again
        if (e.isInPlay() || e.isInZone(ZoneType.Stack)) {
            abilities.removeIf(sa -> (sa.isSpell() || sa.isLandAbility()) && !sa.canPlay(true));
        }
        return abilities;
    }

    @Override
    public List<String> triggerTexts(final Card tableCardNow) {
        final List<String> texts = new ArrayList<>();
        for (final Trigger t : tableCardNow.getTriggers()) {
            if (!t.isIntrinsic() || t.getParam("Secondary") != null) {
                continue;
            }
            final String text = t.toString();
            if (text != null && !text.isEmpty()) {
                texts.add(text);
            }
        }
        return texts;
    }

    @Override
    public void noteCoin(final boolean won) {
        coins.add(won);
    }

    @Override
    public void noteDie(final int sides, final int result) {
        dice.computeIfAbsent(sides, k -> new ArrayList<>()).add(result);
    }

    @Override
    public void announce(final String what) {
        announced.add(what);
    }

    @Override
    public void resetTable() {
        if (acceptedMark != null) {
            table.getUndoManager().rollbackTo(table, acceptedMark);
        }
    }

    @Override
    public boolean askAi() {
        if (resolving == null) {
            return false;
        }
        final Observed t = readTable();
        final UndoManager.Mark base = new UndoManager.Mark(new GameMemento.Recorder().capture(engine), engine.getGameLog().size());
        final Map<Integer, Integer> pairsBefore = new HashMap<>(engineToTable);
        decisions = new Decisions();
        tableNow = t;
        searching = true;
        boolean any = false;
        try {
            startAttempt();
            try {
                resolving.run();
            } catch (final JudgeInfraction ignored) {
                // what it's asked for is the AI's part: the human's own hasn't been done yet, so this says nothing
            }
            pairNewCards(t);
            final Observed e = readEngine();
            ensureCounterparts();
            for (final TableDiff.Item item : TableDiff.compare(readTable(), e)) {
                if (item.kind() != TableDiff.Kind.STACK && item.kind() != TableDiff.Kind.TOWER && !isHuman(item.seat())) {
                    mirrorItem(item, e);
                    any = true;
                }
            }
        } finally {
            searching = false;
            engine.getUndoManager().rollbackTo(engine, base);
            engineToTable.clear();
            engineToTable.putAll(pairsBefore);
            tableNow = null;
        }
        asked = true;
        return any;
    }

    Judge judge() {
        return engine.getJudge();
    }

    /** The same, into a hand, for tests outside this package. */
    public Card addForTest(final String name, final Player tablePlayer) {
        return addToBoth(name, tablePlayer, ZoneType.Hand);
    }

    void tapOnEngine(final Card tableCardNow) {
        engineCard(tableCardNow.getId()).setTapped(true);
        markAccepted();
    }

    /** Tests set a table up in both games at once. The card on the table is returned. */
    Card addToBoth(final String name, final Player tablePlayer, final ZoneType zone) {
        final Player e = enginePlayer(tablePlayer);
        final forge.item.PaperCard paper = forge.StaticData.instance().getCommonCards().getCard(name);
        final Card onEngine = Card.fromPaperCard(paper, e);
        final Card onTable = Card.fromPaperCard(paper, tablePlayer);
        engine.getAction().moveTo(e.getZone(zone), onEngine, null, AbilityKey.newMap());
        onEngine.setSickness(false);
        TableActions.move(table, onTable, zone, tablePlayer, null);
        engineToTable.put(onEngine.getId(), onTable.getId());
        tableCards.put(onTable.getId(), onTable);
        engine.getAction().checkStaticAbilities();
        markAccepted();
        return onTable;
    }
}
