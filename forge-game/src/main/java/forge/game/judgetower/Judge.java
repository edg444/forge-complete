package forge.game.judgetower;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.Range;

import forge.ImageKeys;
import forge.card.GamePieceType;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.GameStage;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPayment;
import forge.game.event.GameEventAddLog;
import forge.game.judgetower.table.TableLink;
import forge.game.keyword.KeywordInterface;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityAlternativeCost;
import forge.game.undo.GameMemento;
import forge.game.undo.UndoManager;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.Infinity;
import forge.util.Localizer;

/**
 * Judge Tower: one Tower (library) and graveyard shared by all, unlimited mana, and everything must be done as soon as
 * it can be. A player caught breaking a rule of the format or the Comprehensive Rules loses; the game goes back to the
 * most recent legal state and carries on for the others (user rulings, 2026-10-30).
 *
 * This is the per-game judge. Human players can try anything their prompts have room for, and are ruled on when they
 * commit to it; AI seats play the forced actions from mandatoryActions, so they never commit an infraction. Its strikes
 * and overruled rulings are part of the game state, so they're written after a rollback, never before one.
 */
public final class Judge {
    private final Game game;
    private final JudgeTowerOptions options;
    // the undo package is outside the game's snapshots, and so is this reference, which must survive a restore
    private final GameMemento.Recorder recorder = new GameMemento.Recorder();
    private final AtomicReference<UndoManager.Mark> lastLegal = new AtomicReference<>();
    private final Map<Player, Integer> strikes = new HashMap<>();
    private final Set<String> overruled = new HashSet<>();
    private TableLink tableLink;

    public Judge(final Game game0, final JudgeTowerOptions options0) {
        game = game0;
        options = options0;
    }

    public JudgeTowerOptions getOptions() {
        return options;
    }

    /** The physical table this game is checking, when humans are playing it all by hand. */
    public TableLink getTableLink() {
        return tableLink;
    }

    public void setTableLink(final TableLink link) {
        tableLink = link;
    }

    /** What a player's zones, life and hand size are set to, once the players exist. */
    public void onGameCreated() {
        for (final Player p : game.getRegisteredPlayers()) {
            // the starting hand only: the maximum hand size stays what it is
            p.addStartingHandOnlyBonus(options.startingHand - p.getStartingHandSize());
            if (options.life == JudgeTowerOptions.LifeRule.INFINITE) {
                p.setStartingLife(Infinity.VALUE);
            }
        }
    }

    /** The format's rules, on a card in the player's command zone so they can be read during the game. */
    public void createRulesEffect(final Player player) {
        final Card eff = new Card(game.nextCardId(), game);
        eff.setGameTimestamp(game.getNextTimestamp());
        eff.setName(Localizer.getInstance().getMessage("lblJudgeTower"));
        eff.setOwner(player);
        eff.setGamePieceType(GamePieceType.EFFECT);
        eff.setImageKey(ImageKeys.getTokenKey("judge_tower"));

        eff.addStaticAbility("Mode$ Continuous | EffectZone$ Command | Affected$ You | Description$ You have an "
                + "unlimited amount of mana of each type. If a cost includes X, X is " + options.xValue + ".");
        eff.addStaticAbility("Mode$ MustAttack | EffectZone$ Command | ValidCreature$ Creature.YouCtrl | Description$ "
                + "Creatures you control attack each combat if able.");
        eff.addStaticAbility("Mode$ MustBlock | EffectZone$ Command | ValidCreature$ Creature.YouCtrl | Description$ "
                + "Creatures you control block each combat if able.");
        if (options.hidden != JudgeTowerOptions.HiddenInfo.NORMAL) {
            eff.addStaticAbility("Mode$ Continuous | EffectZone$ Command | Affected$ Card.YouOwn | AffectedZone$ Hand "
                    + "| MayLookAt$ Player | Description$ Play with your hand revealed.");
        }
        if (options.hidden == JudgeTowerOptions.HiddenInfo.ALL_PUBLIC) {
            eff.addStaticAbility("Mode$ Continuous | EffectZone$ Command | Affected$ Card.YouOwn | AffectedZone$ Exile "
                    + "| MayLookAt$ Player | Secondary$ True");
            eff.addStaticAbility("Mode$ Continuous | EffectZone$ Command | Affected$ Card.TopLibrary "
                    + "| AffectedZone$ Library | MayLookAt$ Player | Description$ The top card of the Tower is revealed.");
        }

        final PlayerZone com = player.getZone(ZoneType.Command);
        eff.updateStateForView();
        com.add(eff);
        player.updateZoneForView(com);
        player.getManaPool().makeUnlimited(eff);
    }

    public void onEndOfTurn() {
        if (options.life == JudgeTowerOptions.LifeRule.RESET) {
            for (final Player p : game.getPlayers()) {
                p.resetLife();
            }
        }
    }

    /** Marks the game as it is now as the state an infraction goes back to. Only humans can commit one. */
    public void checkpoint() {
        if (!hasHumanPlayer()) {
            return;
        }
        try {
            lastLegal.set(new UndoManager.Mark(recorder.capture(game), game.getGameLog().size()));
            if (tableLink != null) {
                tableLink.onCheckpoint();
            }
        } catch (final RuntimeException ex) {
            System.err.println("Judge Tower: could not take a snapshot: " + ex);
        }
    }

    /**
     * Whether what a human picks now is theirs to get wrong. An infraction needs a legal state to go back to, so before
     * there is one (the mulligans, say) prompts keep to what's legal.
     */
    public boolean isJudging() {
        return game.getAge() == GameStage.Play && lastLegal.get() != null;
    }

    private boolean hasHumanPlayer() {
        for (final Player p : game.getPlayers()) {
            if (!p.getController().isAI()) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- what must be done

    /**
     * Everything the player must do before they can pass priority: what they could cast, activate or otherwise do now,
     * by the format's rules. The player may take these in any order (user ruling, 2026-10-30).
     */
    public List<SpellAbility> mandatoryActions(final Player player) {
        final List<SpellAbility> actions = new ArrayList<>();
        for (final Card c : reachableCards(player)) {
            if (canBeUsedWhereItIs(c, player)) {
                actions.addAll(mandatoryActions(c, player));
            }
        }
        return actions;
    }

    private Set<Card> reachableCards(final Player player) {
        final Set<Card> cards = new LinkedHashSet<>(player.getCardsIn(ZoneType.Hand));
        cards.addAll(player.getCardsIn(ZoneType.Graveyard));
        for (final Player p : game.getPlayers()) {
            if (!p.getCardsIn(ZoneType.Library).isEmpty()) {
                cards.add(p.getCardsIn(ZoneType.Library).get(0));
            }
        }
        cards.addAll(player.getCardsIn(ZoneType.Command));
        cards.addAll(game.getCardsIn(ZoneType.Exile));
        cards.addAll(game.getCardsIn(ZoneType.Battlefield));
        return cards;
    }

    /**
     * Whether the card could have anything to do where it is, right now. Asking properly for every card at every
     * priority would be slow in the extreme: with some cards in play, working out how a spell could be cast re-applies
     * every static ability in the game, and the shared graveyard is most of the Tower late on. So: a spell in hand at
     * sorcery speed on someone else's turn is out, and so is what's in a graveyard, in exile or on top of the Tower
     * unless something could let it be used from there.
     */
    private static boolean canBeUsedWhereItIs(final Card c, final Player player) {
        final ZoneType zone = c.getZone().getZoneType();
        if (zone == ZoneType.Battlefield || zone == ZoneType.Command) {
            return true;
        }
        if (!c.mayPlay(player).isEmpty()) {
            return true;
        }
        if (zone == ZoneType.Hand) {
            if (player.canCastSorcery()) {
                return true;
            }
            for (final SpellAbility sa : c.getSpellAbilities()) {
                if (!sa.isSpell() || c.isInstant() || sa.withFlash(c, player)) {
                    return true;
                }
            }
            // the other faces of a card, which only the full check goes through
            return c.isSplitCard() || c.isModal() || c.hasState(forge.card.CardStateName.Secondary);
        }
        // flashback and its like are keywords, turned into a way to cast the card only when it's asked about
        if (c.isFaceDown() || c.isForetold() || c.isPlotted() || !c.getKeywords().isEmpty()) {
            return true;
        }
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getRestrictions().getZone() == zone) {
                return true;
            }
            if (sa.isBasicSpell() && !StaticAbilityAlternativeCost.alternativeCosts(sa, c, player).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private List<SpellAbility> mandatoryActions(final Card c, final Player player) {
        final List<SpellAbility> able = ableActions(c, player);
        final List<SpellAbility> actions = new ArrayList<>();
        final int bottomMost = bottomMostPosition(able);
        for (final SpellAbility sa : able) {
            if (isOverruled(actionKey(sa))) {
                continue;
            }
            // of what's printed on one card, the bottom-most goes first
            if (sa.isActivatedAbility() && printedPosition(sa) >= 0 && printedPosition(sa) < bottomMost) {
                continue;
            }
            if (sa.isSpell() && hasAlternativeToIt(sa, able)) {
                continue;
            }
            actions.add(sa);
        }
        return actions;
    }

    /** What the player could do with the card now, before the format's rules on the order of doing it. */
    private List<SpellAbility> ableActions(final Card c, final Player player) {
        final List<SpellAbility> able = new ArrayList<>();
        for (final SpellAbility sa : c.getAllPossibleAbilities(player, true)) {
            // the check for playability doesn't cover a target that isn't there
            if (isSpent(sa) || !canPay(sa, player) || !hasTargets(sa)) {
                continue;
            }
            // Casting a spell that came back to the zone it was cast from isn't required again this turn. Without this,
            // a spell with buyback (which must be paid) would have to be cast forever. A flashback isn't held back:
            // that's another zone, and it must be cast at once (user ruling, 2026-10-30).
            if (sa.isSpell() && (options.oneAndDone ? wasCastThisTurn(c, null) : wasCastThisTurn(c, c.getZone().getZoneType()))) {
                continue;
            }
            able.add(sa);
        }
        return able;
    }

    private static boolean hasTargets(final SpellAbility sa) {
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.usesTargeting() && a.getTargetRestrictions().getMinTargets(a.getHostCard(), a) > 0
                    && !a.getTargetRestrictions().hasCandidates(a)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSpent(final SpellAbility sa) {
        return sa.isActivatedAbility() && sa.getActivationsThisTurn() > 0;
    }

    private boolean wasCastThisTurn(final Card c, final ZoneType from) {
        for (final SpellAbility cast : game.getStack().getSpellsCastThisTurn()) {
            final Card host = cast.getHostCard();
            if (host.getId() == c.getId() && (from == null
                    || (host.getCastFrom() != null && host.getCastFrom().getZoneType() == from))) {
                return true;
            }
        }
        return false;
    }

    /** Mana is unlimited, except that snow mana has to come from snow permanents. Other costs are checked as usual. */
    private static boolean canPay(final SpellAbility sa, final Player player) {
        final Cost cost = sa.getPayCosts();
        if (cost == null) {
            return true;
        }
        if (cost.hasManaCost() && cost.getTotalMana().getShardCount(ManaCostShard.S) > CardLists.count(
                player.getCardsIn(ZoneType.Battlefield),
                c -> c.isSnow() && !c.isTapped() && !c.getManaAbilities().isEmpty())) {
            return false;
        }
        return CostPayment.canPayAdditionalCosts(cost, sa, false);
    }

    private static boolean isAlternative(final SpellAbility sa) {
        return sa.getAlternativeCost() != null || sa.isCastFaceDown();
    }

    /** Alternative costs must be used if able: a normal cast is out while the same face has one to pay. */
    private static boolean hasAlternativeToIt(final SpellAbility sa, final List<SpellAbility> able) {
        if (isAlternative(sa)) {
            return false;
        }
        for (final SpellAbility other : able) {
            if (other.isSpell() && isAlternative(other) && other.getCardStateName() == sa.getCardStateName()) {
                return true;
            }
        }
        return false;
    }

    /** The highest position of the activated abilities here, -1 if none of them is on the printed text. */
    private static int bottomMostPosition(final List<SpellAbility> able) {
        int bottomMost = -1;
        for (final SpellAbility sa : able) {
            if (sa.isActivatedAbility()) {
                bottomMost = Math.max(bottomMost, printedPosition(sa));
            }
        }
        return bottomMost;
    }

    /**
     * Where the ability is in the card's Oracle text, or -1 if it isn't there (one granted by another card has no
     * place in the order). Going by the list of abilities wouldn't do: scripts list keywords and mana abilities first.
     */
    static int printedPosition(final SpellAbility sa) {
        final String oracle = normalize(sa.getHostCard().getOracleText(), sa.getHostCard());
        if (oracle.isEmpty()) {
            return -1;
        }
        final String own = normalize(sa.getDescription(), sa.getHostCard());
        if (own.isEmpty()) {
            return -1;
        }
        final int found = oracle.indexOf(own);
        if (found >= 0) {
            return found;
        }
        // scripts often word the description differently past the cost, and a keyword's reminder text isn't printed
        final int head = oracle.indexOf(own.substring(0, Math.min(16, own.length())));
        if (head >= 0) {
            return head;
        }
        if (sa.getKeyword() != null) {
            return oracle.indexOf(sa.getKeyword().getKeyword().toString().toLowerCase(Locale.ROOT));
        }
        return -1;
    }

    private static String normalize(final String text, final Card host) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\([^)]*\\)", "").replace("CARDNAME", "~").replace("NICKNAME", "~")
                .replace(host.getName(), "~").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- rulings on what a player does

    private JudgeInfraction infraction(final Player offender, final String key, final String reason) {
        return new JudgeInfraction(offender, key, reason);
    }

    private boolean isOverruled(final String key) {
        return overruled.contains(key);
    }

    private static String actionKey(final SpellAbility sa) {
        return "act:" + sa.getHostCard().getId() + ":" + sa.getDescription();
    }

    private static Localizer loc() {
        return Localizer.getInstance();
    }

    /** A player has decided to pass priority. */
    public void checkPass(final Player player) {
        final List<SpellAbility> left = mandatoryActions(player);
        if (!left.isEmpty()) {
            final SpellAbility sa = left.get(0);
            throw infraction(player, actionKey(sa),
                    loc().getMessage("lblJudgeInfractionPassed", player.getName(), sa.getHostCard().getName(), sa.toString()));
        }
    }

    /** A player has chosen something to cast, activate or do. */
    public void checkPlay(final Player player, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        // an ability already used this turn gets the format's ruling below; overruling a mistaken play is for that turn
        if (!isSpent(sa) && (!sa.canPlay(true) || !canPay(sa, player) || !hasTargets(sa))) {
            illegal(player, "illegal:" + actionKey(sa) + thisTurn(),
                    loc().getMessage("lblJudgeInfractionIllegalPlay", player.getName(), host.getName(), sa.toString()));
        }
        final String key = "play:" + actionKey(sa);
        if (isOverruled(key)) {
            return;
        }
        if (isSpent(sa)) {
            throw infraction(player, key, loc().getMessage("lblJudgeInfractionTwice", player.getName(), host.getName()));
        }
        final List<SpellAbility> able = ableActions(host, player);
        if (sa.isActivatedAbility() && printedPosition(sa) >= 0 && printedPosition(sa) < bottomMostPosition(able)) {
            throw infraction(player, key, loc().getMessage("lblJudgeInfractionOrder", player.getName(), host.getName()));
        }
        if (sa.isSpell() && options.oneAndDone && wasCastThisTurn(host, null)) {
            throw infraction(player, key, loc().getMessage("lblJudgeInfractionOneAndDone", player.getName(), host.getName()));
        }
        if (sa.isSpell() && hasAlternativeToIt(sa, able)) {
            throw infraction(player, key, loc().getMessage("lblJudgeInfractionAlternative", player.getName(), host.getName()));
        }
    }

    private void illegal(final Player player, final String key, final String reason) {
        if (!isOverruled(key)) {
            throw infraction(player, key, reason);
        }
    }

    private String thisTurn() {
        return ":" + game.getPhaseHandler().getTurn();
    }

    /** Something a player has chosen as a target that can't be one. The reason comes from the prompt that took it. */
    public void illegalTarget(final Player player, final SpellAbility sa, final Object target, final String why) {
        illegal(player, "target:" + actionKey(sa) + ":" + target + thisTurn(),
                loc().getMessage("lblJudgeInfractionTarget", player.getName(), target, sa.getHostCard().getName(), why));
    }

    /** Something a player has chosen, for a cost or an effect, from outside what could be chosen. */
    public void illegalChoice(final Player player, final SpellAbility sa, final Object chosen) {
        illegal(player, "choice:" + actionKey(sa) + ":" + chosen + thisTurn(),
                loc().getMessage("lblJudgeInfractionChoice", player.getName(), chosen, sa.getHostCard().getName()));
    }

    /** Which modes were chosen, out of all of them, and which of them can legally be. */
    public void checkModes(final Player player, final SpellAbility sa, final List<AbilitySub> chosen,
            final List<AbilitySub> legal) {
        for (final AbilitySub mode : chosen) {
            if (!legal.contains(mode)) {
                illegal(player, "mode:" + actionKey(sa) + ":" + mode.getDescription() + thisTurn(),
                        loc().getMessage("lblJudgeInfractionMode", player.getName(), sa.getHostCard().getName(), mode.toString()));
            }
        }
    }

    /** A creature declared as an attacker (or anything else), that can't attack that defender, or isn't the attacker's to send. */
    public void illegalAttack(final Player declarer, final Card attacker, final GameEntity defender) {
        illegal(declarer, "attacker:" + attacker.getId() + thisTurn(),
                loc().getMessage("lblJudgeInfractionAttacker", declarer.getName(), attacker.getName(), defender));
    }

    /** The attack as declared, before anything takes an illegal attacker out of combat again. */
    public void checkAttackers(final Player declarer, final Player attacking, final Combat combat,
            final CardCollection attackingAlready) {
        for (final Card attacker : combat.getAttackers()) {
            if (attackingAlready.contains(attacker)) {
                continue;
            }
            final GameEntity defender = combat.getDefenderByAttacker(attacker);
            if (!attacker.getController().equals(attacking) || !CombatUtil.canAttack(attacker, defender)) {
                illegalAttack(declarer, attacker, defender);
            }
        }
    }

    public void illegalBlock(final Player player, final Card blocker, final Card attacker) {
        illegal(player, "blocker:" + blocker.getId() + ":" + attacker.getId() + thisTurn(),
                loc().getMessage("lblJudgeInfractionBlocker", player.getName(), blocker.getName(), attacker.getName()));
    }

    /** The engine couldn't go through with what a player has played on the table. */
    public JudgeInfraction cannotBeDone(final Player player, final SpellAbility sa) {
        return infraction(player, "undoable:" + actionKey(sa) + thisTurn(),
                loc().getMessage("lblJudgeInfractionIllegalPlay", player.getName(), sa.getHostCard().getName(), sa.toString()));
    }

    /** X as the player announced it, from the range the rules allow. */
    public void checkX(final Player player, final SpellAbility sa, final int value, final Range<Integer> range) {
        final int required = requiredX(sa, player, range.getMaximum());
        final String key = "x:" + actionKey(sa);
        if (value != required && !isOverruled(key)) {
            throw infraction(player, key,
                    loc().getMessage("lblJudgeInfractionX", player.getName(), sa.getHostCard().getName(), value, required));
        }
    }

    /** The value of X for this cost, short of the format's value only where the cost can't be paid with that many. */
    public int requiredX(final SpellAbility sa, final Player player, final int max) {
        int required = Math.min(options.xValue, max);
        final Cost cost = sa.getPayCosts();
        if (cost != null) {
            final Integer nonMana = cost.getMaxForNonManaX(sa, player, false);
            if (nonMana != null) {
                required = Math.min(required, nonMana);
            }
        }
        return Math.max(required, 0);
    }

    /** Which of the optional costs must be paid: all that can be. */
    public List<OptionalCostValue> requiredOptionalCosts(final SpellAbility sa, final List<OptionalCostValue> offered) {
        final List<OptionalCostValue> required = new ArrayList<>();
        for (final OptionalCostValue opt : offered) {
            if (CostPayment.canPayAdditionalCosts(opt.getCost(), sa, false)) {
                required.add(opt);
            }
        }
        return required;
    }

    public void checkOptionalCosts(final Player player, final SpellAbility sa, final List<OptionalCostValue> offered,
            final List<OptionalCostValue> chosen) {
        final String key = "optional:" + actionKey(sa);
        if (!isOverruled(key) && !chosen.containsAll(requiredOptionalCosts(sa, offered))) {
            throw infraction(player, key, loc().getMessage("lblJudgeInfractionOptionalCost", player.getName(), sa.getHostCard().getName()));
        }
    }

    /**
     * How many times to pay a keyword's additional cost (kicker, multikicker, replicate...): as many as it takes, where
     * "any number of times" has to be some number, and the format's X is the only one it has (my call, not a source's).
     */
    public int keywordCostAmount(final PlayerController pc, final SpellAbility sa, final Cost cost,
            final KeywordInterface keyword, final String prompt, final int max) {
        final Player player = pc.getPlayer();
        int required = 0;
        if (CostPayment.canPayAdditionalCosts(cost, sa, false)) {
            required = max == 1 || !cost.isOnlyManaCost() ? Math.min(max, 1) : Math.min(max, options.xValue);
        }
        if (pc.isAI()) {
            return required;
        }
        final int chosen = pc.chooseNumberForKeywordCost(sa, cost, keyword, prompt, max);
        final String key = "keyword:" + actionKey(sa);
        if (chosen != required && !isOverruled(key)) {
            throw infraction(player, key, loc().getMessage("lblJudgeInfractionOptionalCost", player.getName(), sa.getHostCard().getName()));
        }
        return chosen;
    }

    /** A player has turned down something they "may" do; in this format they must. */
    public void checkDeclined(final Player player, final SpellAbility sa, final String what) {
        final String key = "declined:" + (sa != null ? actionKey(sa) : what);
        if (!isOverruled(key)) {
            throw infraction(player, key, loc().getMessage("lblJudgeInfractionDeclined", player.getName(),
                    sa != null ? sa.getHostCard().getName() : what));
        }
    }

    /** True when an attack that's invalid under the "must attack" rules stands, the player having overruled that. */
    public boolean checkAttack(final Player player) {
        final String key = "attack:" + game.getPhaseHandler().getTurn();
        if (!isOverruled(key)) {
            throw infraction(player, key, loc().getMessage("lblJudgeInfractionAttack", player.getName()));
        }
        return true;
    }

    public void checkBlocks(final Player player, final String error) {
        final String key = "block:" + game.getPhaseHandler().getTurn();
        if (error != null && !isOverruled(key)) {
            throw infraction(player, key, loc().getMessage("lblJudgeInfractionBlock", player.getName(), error));
        }
    }

    // ---------------------------------------------------------------- the ruling

    /**
     * Back to the last legal state, then the ruling: the offender may appeal one that's wrong (a card script's bug must
     * not put someone out of the game), and otherwise takes a strike, the last of which loses them the game.
     */
    public void rule(final JudgeInfraction inf) {
        final Player offender = inf.getOffender();
        final String reason = inf.getMessage();
        game.getUndoManager().rollbackTo(game, lastLegal.get());
        if (tableLink != null) {
            tableLink.onRolledBack();
        }

        if (!inf.isAppealed() && offender.getController().appealJudgeRuling(reason)) {
            overruled.add(inf.getKey());
            log(loc().getMessage("lblJudgeOverruled", offender.getName(), reason));
            if (tableLink != null) {
                tableLink.onRuled();
            }
            return;
        }

        final int committed = strikes.merge(offender, 1, Integer::sum);
        if (committed < options.strikes) {
            log(loc().getMessage("lblJudgeStrike", reason, committed, options.strikes));
        } else {
            log(reason);
            offender.loseConditionMet(GameLossReason.JudgeInfraction, reason);
            game.getAction().checkGameOverCondition();
        }
        if (tableLink != null) {
            tableLink.onRuled();
        }
    }

    private void log(final String message) {
        game.fireEvent(new GameEventAddLog(GameLogEntryType.GAME_OUTCOME, message));
    }
}
