package forge.gamemodes.match.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.judgetower.table.TableActions;
import forge.game.judgetower.table.TableLink;
import forge.game.judgetower.table.TableSeat;
import forge.game.judgetower.table.TableStackItem;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.Selection;
import forge.gui.interfaces.IGuiGame;
import forge.player.PlayerControllerHuman;
import forge.trackable.TrackableObject;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.util.ThreadUtil;

/**
 * Judge Tower's table: a human working on it by hand. Clicking a card, a player or a stack item brings up what can be
 * done to it; the Cancel button brings up what can be done to the table; OK says they're done. Nothing here knows any
 * rules, and nothing's refused. It goes through the lists every GUI has, so desktop, mobile and hotseat are all this.
 *
 * The lists can't be shown from the EDT on mobile, so they run on a pool thread, with the game thread waiting on this
 * input and nothing else touching the table meanwhile.
 */
public final class InputTable extends InputSyncronizedBase {
    private static final long serialVersionUID = 1L;

    /** A row of a menu: what it says, and what it does. */
    private record Row(String label, Runnable action) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final TableLink link;
    private final TableSeat.Window window;
    private final String prompt;
    private final Runnable onAutoOk;
    private volatile boolean busy;

    private SpellAbility play;
    // a spell or ability being declared: its targets are clicked on, and OK finishes it
    private SpellAbility declaring;
    private List<String> declaredModes = List.of();
    private List<String> declaredCosts = List.of();
    private Integer declaredX;
    private final List<GameObject> declaredTargets = new ArrayList<>();
    private String triggerText;
    private Card triggerSource;

    /** @param window0 null when the player has priority rather than something to get done */
    public InputTable(final PlayerControllerHuman controller, final TableLink link0, final TableSeat.Window window0, final String prompt0, final Runnable onAutoOk0) {
        super(controller);
        link = link0;
        window = window0;
        prompt = prompt0;
        onAutoOk = onAutoOk0;
    }

    public boolean isPriorityPrompt() {
        return window == null;
    }

    public TableSeat.Window getWindow() {
        return window;
    }

    /** What they've played by the time they said OK, with what's declared for it on the table. */
    public SpellAbility getPlay() {
        return play;
    }

    private static Localizer loc() {
        return Localizer.getInstance();
    }

    private Game table() {
        return getController().getGame();
    }

    private Player me() {
        return getController().getPlayer();
    }

    private IGuiGame gui() {
        return getController().getGui();
    }

    private boolean isDeclaring() {
        return declaring != null || triggerSource != null;
    }

    @Override
    protected boolean isBusy() {
        return busy;
    }

    @Override
    protected void showMessage() {
        final String message;
        if (isDeclaring()) {
            message = loc().getMessage("lblJudgeTableDeclareTargets", declaredTargets.size());
        } else if (play != null) {
            message = loc().getMessage("lblJudgeTablePayCosts", play.getHostCard().getName());
        } else {
            message = prompt;
        }
        showMessage(message + "\n" + loc().getMessage("lblJudgeTableHelp"));
        gui().updateButtons(getOwner(), loc().getMessage("lblOK"), loc().getMessage("lblJudgeTableMenu"), true, !isDeclaring(), true);
    }

    @Override
    protected Selection getSelection() {
        final List<TrackableObject> chosen = new ArrayList<>();
        for (final GameObject t : declaredTargets) {
            if (t instanceof Card c) {
                chosen.add(c.getView());
            } else if (t instanceof Player p) {
                chosen.add(p.getView());
            }
        }
        return Selection.ofChosen(chosen);
    }

    private void refresh() {
        showSelection();
        showMessage();
    }

    /** Runs a menu or a dialog off the EDT, as the app's dialogs need, with clicks on the table ignored until it's over. */
    private void work(final Runnable what) {
        if (busy || isFinished()) {
            return;
        }
        busy = true;
        ThreadUtil.invokeInGameThread(() -> {
            try {
                what.run();
            } finally {
                busy = false;
                refresh();
            }
        });
    }

    private void showMenu(final String title, final List<Row> rows) {
        if (rows.isEmpty()) {
            return;
        }
        final Row row = gui().oneOrNone(title, rows);
        if (row != null) {
            row.action().run();
        }
    }

    @Override
    protected void onOk() {
        if (busy) {
            return;
        }
        if (isDeclaring()) {
            finishDeclaring();
            refresh();
            return;
        }
        stop();
    }

    @Override
    protected void onCancel() {
        work(this::tableMenu);
    }

    @Override
    protected boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        if (isDeclaring()) {
            toggleTarget(card);
            return true;
        }
        work(() -> cardMenu(card));
        return true;
    }

    @Override
    protected void onPlayerSelected(final Player player, final ITriggerEvent triggerEvent) {
        if (isDeclaring()) {
            toggleTarget(player);
            return;
        }
        work(() -> playerMenu(player));
    }

    @Override
    public boolean selectAbility(final SpellAbility ab) {
        if (ab == null || isFinished()) {
            return false;
        }
        if (isDeclaring()) {
            toggleTarget(ab);
            return true;
        }
        for (final SpellAbilityStackInstance si : table().getStack().asList()) {
            if (si.getSpellAbility().equals(ab)) {
                work(() -> stackMenu(si));
                return true;
            }
        }
        return false;
    }

    @Override
    public String getActivateAction(final Card card) {
        return isDeclaring() ? "select card as target" : loc().getMessage("lblJudgeTableMenu");
    }

    private void toggleTarget(final GameObject target) {
        if (!declaredTargets.remove(target)) {
            declaredTargets.add(target);
        }
        refresh();
    }

    // ---------------------------------------------------------------- playing something

    private static boolean usesTargets(final SpellAbility sa) {
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.usesTargeting()) {
                return true;
            }
        }
        return false;
    }

    private void startPlay(final SpellAbility sa) {
        declaredModes = List.of();
        declaredCosts = List.of();
        declaredX = null;
        declaredTargets.clear();
        boolean targets = usesTargets(sa);
        if (sa.getApi() == ApiType.Charm) {
            final List<String> modes = new ArrayList<>();
            for (final AbilitySub mode : CharmEffect.makePossibleOptions(sa, false)) {
                modes.add(mode.getDescription());
                targets |= usesTargets(mode);
            }
            declaredModes = new ArrayList<>(gui().many(sa.getHostCard().getName(), loc().getMessage("lblJudgeTableModes"), 0, modes.size(), modes, null));
        }
        final List<OptionalCostValue> optional = GameActionUtil.getOptionalCostValues(sa);
        if (!optional.isEmpty()) {
            final List<String> costs = new ArrayList<>();
            optional.forEach(o -> costs.add(o.toString()));
            declaredCosts = new ArrayList<>(gui().many(sa.getHostCard().getName(), loc().getMessage("lblJudgeTableOptionalCosts"), 0, costs.size(), costs, null));
        }
        if (sa.costHasManaX() || "X".equalsIgnoreCase(sa.getParam("Announce"))) {
            declaredX = gui().getInteger(loc().getMessage("lblJudgeTableX"), 0, 99, false);
        }
        declaring = sa;
        if (!targets) {
            finishDeclaring();
        }
    }

    private void finishDeclaring() {
        if (triggerSource != null) {
            TableActions.pushStack(table(), triggerSource, me(), false, triggerText, declaredTargets);
            triggerSource = null;
            declaredTargets.clear();
            return;
        }
        final SpellAbility sa = declaring;
        declaring = null;
        final Card onTable = table().findById(tableIdOfHost(sa));
        if (sa.isLandAbility()) {
            if (onTable != null) {
                TableActions.move(table(), onTable, ZoneType.Battlefield, me(), null);
            }
        } else if (!sa.isManaAbility() && onTable != null) {
            if (sa.isSpell()) {
                TableActions.move(table(), onTable, ZoneType.Stack, me(), null);
            }
            final TableStackItem item = TableActions.pushStack(table(), onTable, me(), sa.isSpell(), sa.toString(), declaredTargets);
            item.modes.addAll(declaredModes);
            item.optionalCosts.addAll(declaredCosts);
            item.x = declaredX;
        }
        declaredTargets.clear();
        play = sa;
    }

    // the card this menu came from; the ability offered is the judge's game's own
    private Card playedFrom;

    private int tableIdOfHost(final SpellAbility sa) {
        return playedFrom != null ? playedFrom.getId() : sa.getHostCard().getId();
    }

    // ---------------------------------------------------------------- the menus

    private void cardMenu(final Card card) {
        final List<Row> rows = new ArrayList<>();
        if (window == null && play == null) {
            for (final SpellAbility sa : link.offeredAbilities(card, me())) {
                rows.add(new Row(loc().getMessage("lblJudgeTablePlay", sa.toString()), () -> {
                    playedFrom = card;
                    startPlay(sa);
                }));
            }
        }
        final List<String> triggers = link.triggerTexts(card);
        for (final String text : triggers) {
            rows.add(new Row(loc().getMessage("lblJudgeTableTrigger", text), () -> startTrigger(card, text)));
        }
        rows.add(new Row(loc().getMessage("lblJudgeTableOtherTrigger"), () -> {
            final String text = gui().showInputDialog(loc().getMessage("lblJudgeTableOtherTriggerPrompt"), card.getName(), null, "", null, false);
            if (text != null) {
                startTrigger(card, text);
            }
        }));
        if (card.isInZone(ZoneType.Battlefield)) {
            addBattlefieldRows(card, rows);
        }
        addMoveRows(card, rows);
        showMenu(card.getName(), rows);
    }

    private void startTrigger(final Card card, final String text) {
        triggerSource = card;
        triggerText = text;
        declaredTargets.clear();
        final List<String> ways = List.of(loc().getMessage("lblJudgeTableWithTargets"), loc().getMessage("lblJudgeTableNoTargets"));
        final String way = gui().oneOrNone(text, ways);
        if (way == null) {
            triggerSource = null;
        } else if (way.equals(ways.get(1))) {
            finishDeclaring();
        }
    }

    private void addMoveRows(final Card card, final List<Row> rows) {
        if (!card.isInZone(ZoneType.Battlefield)) {
            rows.add(new Row(loc().getMessage("lblJudgeTableToBattlefield"), () -> TableActions.move(table(), card, ZoneType.Battlefield, me(), null)));
        }
        for (final Player p : table().getPlayers()) {
            if (!(card.isInZone(ZoneType.Hand) && p.equals(card.getController()))) {
                rows.add(new Row(loc().getMessage("lblJudgeTableToHand", p.getName()), () -> TableActions.move(table(), card, ZoneType.Hand, p, null)));
            }
        }
        if (!card.isInZone(ZoneType.Graveyard)) {
            rows.add(new Row(loc().getMessage("lblJudgeTableToGraveyard"), () -> TableActions.move(table(), card, ZoneType.Graveyard, me(), null)));
        }
        if (!card.isInZone(ZoneType.Exile)) {
            rows.add(new Row(loc().getMessage("lblJudgeTableToExile"), () -> TableActions.move(table(), card, ZoneType.Exile, null, null)));
        }
        rows.add(new Row(loc().getMessage("lblJudgeTableToTowerTop"), () -> TableActions.move(table(), card, ZoneType.Library, me(), 0)));
        rows.add(new Row(loc().getMessage("lblJudgeTableToTowerBottom"), () -> TableActions.move(table(), card, ZoneType.Library, me(), null)));
    }

    private void addBattlefieldRows(final Card card, final List<Row> rows) {
        rows.add(new Row(loc().getMessage(card.isTapped() ? "lblJudgeTableUntap" : "lblJudgeTableTap"), () -> TableActions.setTapped(table(), card, !card.isTapped())));
        rows.add(new Row(loc().getMessage("lblJudgeTableCounters"), () -> chooseCounters(card)));
        rows.add(new Row(loc().getMessage("lblJudgeTableDamage"), () -> {
            final Integer damage = gui().getInteger(card.getName(), 0, 99, false);
            if (damage != null) {
                TableActions.setDamage(table(), card, damage);
            }
        }));
        rows.add(new Row(loc().getMessage("lblJudgeTableAttack"), () -> chooseDefender(card)));
        rows.add(new Row(loc().getMessage("lblJudgeTableBlock"), () -> chooseAttackerToBlock(card)));
        final Combat combat = table().getPhaseHandler().getCombat();
        if (combat != null && (combat.isAttacking(card) || combat.isBlocking(card))) {
            rows.add(new Row(loc().getMessage("lblRemoveFromCombat"), () -> TableActions.removeFromCombat(table(), card)));
        }
        rows.add(new Row(loc().getMessage(card.isFaceDown() ? "lblJudgeTableFaceUp" : "lblJudgeTableFaceDown"),
                () -> TableActions.setFaceDown(table(), card, !card.isFaceDown())));
        for (final CardStateName state : List.of(CardStateName.Original, CardStateName.Backside, CardStateName.Flipped)) {
            if (card.hasState(state) && card.getCurrentStateName() != state && (state != CardStateName.Original || card.hasState(CardStateName.Backside) || card.hasState(CardStateName.Flipped))) {
                rows.add(new Row(loc().getMessage("lblJudgeTableShowFace", card.getState(state).getName()), () -> TableActions.showState(table(), card, state)));
            }
        }
        rows.add(new Row(loc().getMessage(card.isPhasedOut() ? "lblJudgeTablePhaseIn" : "lblJudgeTablePhaseOut"),
                () -> TableActions.setPhasedOut(table(), card, !card.isPhasedOut())));
        rows.add(new Row(loc().getMessage("lblJudgeTableAttach"), () -> chooseAttachment(card)));
        if (card.getEntityAttachedTo() != null) {
            rows.add(new Row(loc().getMessage("lblJudgeTableDetach"), () -> TableActions.detach(table(), card)));
        }
        for (final Player p : table().getPlayers()) {
            if (!p.equals(card.getController())) {
                rows.add(new Row(loc().getMessage("lblJudgeTableGiveControl", p.getName()), () -> TableActions.giveControl(table(), card, p)));
            }
        }
    }

    private void chooseCounters(final Card card) {
        final List<CounterEnumType> types = new ArrayList<>(Arrays.asList(CounterEnumType.values()));
        // those that are there, then the ones most things use, then the rest as they come
        types.sort(Comparator.comparingInt(t -> card.getCounters(t) > 0 ? 0 : t == CounterEnumType.P1P1 || t == CounterEnumType.M1M1 || t == CounterEnumType.LOYALTY ? 1 : 2));
        final CounterEnumType type = gui().oneOrNone(card.getName(), types);
        if (type != null) {
            final Integer amount = gui().getInteger(type.toString(), 0, 99, false);
            if (amount != null) {
                TableActions.setCounters(table(), card, type, amount);
            }
        }
    }

    private void chooseDefender(final Card card) {
        final List<GameEntity> defenders = new ArrayList<>();
        final Combat combat = TableActions.combat(table());
        combat.getDefenders().forEach(defenders::add);
        final GameEntity defender = defenders.size() == 1 ? defenders.get(0) : gui().oneOrNone(card.getName(), defenders);
        if (defender != null) {
            TableActions.declareAttacker(table(), card, defender);
        }
    }

    private void chooseAttackerToBlock(final Card card) {
        final Combat combat = table().getPhaseHandler().getCombat();
        if (combat == null) {
            return;
        }
        final List<Card> attackers = new ArrayList<>();
        combat.getAttackers().forEach(attackers::add);
        final Card attacker = gui().oneOrNone(card.getName(), attackers);
        if (attacker != null) {
            TableActions.declareBlocker(table(), card, attacker);
        }
    }

    private void chooseAttachment(final Card card) {
        final List<GameEntity> options = new ArrayList<>();
        for (final Player p : table().getPlayers()) {
            options.add(p);
            p.getCardsIn(ZoneType.Battlefield).forEach(c -> {
                if (!c.equals(card)) {
                    options.add(c);
                }
            });
        }
        final GameEntity on = gui().oneOrNone(card.getName(), options);
        if (on != null) {
            TableActions.attach(table(), card, on);
        }
    }

    private void playerMenu(final Player player) {
        final List<Row> rows = new ArrayList<>();
        rows.add(new Row(loc().getMessage("lblJudgeTableLife", player.getLife()), () -> {
            final String life = gui().showInputDialog(player.getName(), loc().getMessage("lblJudgeTableLife", player.getLife()), null, String.valueOf(player.getLife()), null, false);
            if (life != null) {
                try {
                    TableActions.setLife(table(), player, Integer.parseInt(life.trim()));
                } catch (final NumberFormatException ignored) {
                    // nothing happens: it's a typo, not a move
                }
            }
        }));
        rows.add(new Row(loc().getMessage("lblJudgeTablePoison", player.getPoisonCounters()), () -> {
            final Integer poison = gui().getInteger(player.getName(), 0, 99, false);
            if (poison != null) {
                TableActions.setPoison(table(), player, poison);
            }
        }));
        rows.add(new Row(loc().getMessage("lblJudgeTablePlayerCounters"), () -> {
            final List<CounterEnumType> types = new ArrayList<>(Arrays.asList(CounterEnumType.values()));
            final CounterEnumType type = gui().oneOrNone(player.getName(), types);
            if (type != null) {
                final Integer amount = gui().getInteger(type.toString(), 0, 99, false);
                if (amount != null) {
                    TableActions.setCounters(table(), player, type, amount);
                }
            }
        }));
        showMenu(player.getName(), rows);
    }

    private void stackMenu(final SpellAbilityStackInstance si) {
        final List<Row> rows = new ArrayList<>();
        rows.add(new Row(loc().getMessage("lblJudgeTableOffStack"), () -> TableActions.removeFromStack(table(), si)));
        showMenu(si.getSourceCard().getName(), rows);
    }

    private void tableMenu() {
        final List<Row> rows = new ArrayList<>();
        final CardCollection tower = new CardCollection(me().getCardsIn(ZoneType.Library));
        if (!tower.isEmpty()) {
            rows.add(new Row(loc().getMessage("lblJudgeTableDraw"), () -> TableActions.move(table(), tower.get(0), ZoneType.Hand, me(), null)));
            rows.add(new Row(loc().getMessage("lblJudgeTableMill"), () -> TableActions.move(table(), tower.get(0), ZoneType.Graveyard, me(), null)));
            rows.add(new Row(loc().getMessage("lblJudgeTableExileTop"), () -> TableActions.move(table(), tower.get(0), ZoneType.Exile, null, null)));
            rows.add(new Row(loc().getMessage("lblJudgeTableLookAtTop"), () -> arrangeTop(tower)));
            rows.add(new Row(loc().getMessage("lblJudgeTableSearch"), () -> searchTower(tower)));
        }
        rows.add(new Row(loc().getMessage("lblJudgeTableShuffle"), () -> TableActions.shuffleTower(table(), me())));
        rows.add(new Row(loc().getMessage("lblJudgeTableToken"), this::createToken));
        rows.add(new Row(loc().getMessage("lblJudgeTableCoin"), () -> {
            final boolean won = MyRandom.getRandom().nextBoolean();
            link.noteCoin(won);
            TableActions.note(table(), me(), won ? loc().getMessage("lblJudgeTableCoinWon") : loc().getMessage("lblJudgeTableCoinLost"));
        }));
        rows.add(new Row(loc().getMessage("lblJudgeTableDie"), () -> {
            final Integer sides = gui().getInteger(loc().getMessage("lblJudgeTableDieSides"), 2, 100, false);
            if (sides != null) {
                final int result = MyRandom.getRandom().nextInt(sides) + 1;
                link.noteDie(sides, result);
                TableActions.note(table(), me(), "d" + sides + ": " + result);
            }
        }));
        rows.add(new Row(loc().getMessage("lblJudgeTableAnnounce"), () -> {
            final String what = gui().showInputDialog(loc().getMessage("lblJudgeTableAnnouncePrompt"), loc().getMessage("lblJudgeTableAnnounce"), null, "", null, false);
            if (what != null && !what.isBlank()) {
                link.announce(what.trim());
                TableActions.note(table(), me(), what.trim());
            }
        }));
        if (window == TableSeat.Window.RESOLVE) {
            rows.add(new Row(loc().getMessage("lblJudgeTableAskAi"), link::askAi));
        }
        rows.add(new Row(loc().getMessage("lblJudgeTableReset"), () -> {
            link.resetTable();
            play = null;
            declaring = null;
            triggerSource = null;
            declaredTargets.clear();
        }));
        if (window != TableSeat.Window.RESOLVE) {
            rows.add(new Row(loc().getMessage("lblJudgeTableAutoOk"), () -> {
                onAutoOk.run();
                stop();
            }));
        }
        showMenu(loc().getMessage("lblJudgeTower"), rows);
    }

    private void arrangeTop(final CardCollection tower) {
        final Integer n = gui().getInteger(loc().getMessage("lblJudgeTableHowMany"), 1, Math.min(tower.size(), 20), false);
        if (n == null) {
            return;
        }
        final List<CardView> top = new ArrayList<>();
        for (final Card c : tower.subList(0, n)) {
            top.add(c.getView());
        }
        final List<CardView> ordered = gui().order(loc().getMessage("lblJudgeTower"), loc().getMessage("lblJudgeTableTopOfTower"), top, null);
        if (ordered == null) {
            return;
        }
        final List<Card> newOrder = new ArrayList<>();
        for (final CardView v : ordered) {
            for (final Card c : tower) {
                if (c.getView().equals(v)) {
                    newOrder.add(c);
                    break;
                }
            }
        }
        newOrder.addAll(tower.subList(n, tower.size()));
        TableActions.setTowerOrder(table(), me(), newOrder);
    }

    private void searchTower(final CardCollection tower) {
        final List<CardView> views = new ArrayList<>();
        tower.forEach(c -> views.add(c.getView()));
        final CardView chosen = gui().oneOrNone(loc().getMessage("lblJudgeTower"), views);
        for (final Card c : tower) {
            if (c.getView().equals(chosen)) {
                // a card just taken out of the Tower gets the usual menu
                cardMenu(c);
                return;
            }
        }
    }

    private void createToken() {
        final String name = gui().showInputDialog(loc().getMessage("lblJudgeTableTokenName"), loc().getMessage("lblJudgeTableToken"), null, "", null, false);
        if (name == null || name.isBlank()) {
            return;
        }
        final String types = gui().showInputDialog(loc().getMessage("lblJudgeTableTokenTypes"), name, null, "Creature", null, false);
        final Integer power = gui().getInteger(loc().getMessage("lblJudgeTableTokenPower"), 0, 99, false);
        final Integer toughness = gui().getInteger(loc().getMessage("lblJudgeTableTokenToughness"), 0, 99, false);
        TableActions.newToken(table(), me(), name.trim(), null, types == null || types.isBlank() ? "Creature" : types.trim(),
                power == null ? 0 : power, toughness == null ? 0 : toughness);
    }
}
