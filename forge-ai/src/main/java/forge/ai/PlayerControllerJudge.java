package forge.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.collect.ListMultimap;

import forge.LobbyPlayer;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostDecisionMakerBase;
import forge.game.keyword.KeywordInterface;
import forge.game.judgetower.table.TableSeat;
import forge.game.judgetower.table.TableStackItem;
import forge.game.player.DelayedReveal;
import forge.game.player.PlaySpellAbility;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

/**
 * A human's seat in the judge's game. It answers from the table: what's declared on the stack, then the cards the
 * player's done something with, and failing those it goes through the options for TableJudge's search. When the engine
 * isn't re-executing something the player has done by hand (the AI's spell is resolving), it asks them.
 *
 * It extends the AI's controller for the long tail of rare decisions, which get the AI's answer; if that's not what's on
 * the table, the search fails over to the ruling, and Overrule has the table stand.
 */
final class PlayerControllerJudge extends PlayerControllerAi {
    private final TableJudge link;

    PlayerControllerJudge(final Game game, final Player player, final LobbyPlayer lobby, final TableJudge link0) {
        super(game, player, lobby);
        link = link0;
    }

    @Override
    public boolean isAI() {
        return false;
    }

    private <T> T pick(final String prompt, final List<T> options, final Function<T, String> label) {
        if (options.isEmpty()) {
            return null;
        }
        if (link.isSearching()) {
            return options.get(link.decisions().pick(options.size()));
        }
        if (options.size() == 1 || link.seat(player) == null) {
            return options.get(0);
        }
        final List<String> labels = new ArrayList<>();
        options.forEach(o -> labels.add(label.apply(o)));
        return options.get(link.seat(player).choose(prompt, labels, 1, 1).get(0));
    }

    /** The cards among these that the player did something with come first, then the other ways of making up the number. */
    private List<Card> pickCards(final String prompt, final Iterable<Card> options, final int min0, final int max0) {
        final List<Card> all = new ArrayList<>();
        options.forEach(all::add);
        final int min = Math.min(min0, all.size());
        final int max = Math.min(max0, all.size());
        if (!link.isSearching() && link.seat(player) != null) {
            if (all.size() == min) {
                return all;
            }
            final List<String> labels = new ArrayList<>();
            all.forEach(c -> labels.add(c.getName() + " (" + c.getId() + ")"));
            final List<Card> chosen = new ArrayList<>();
            for (final int i : link.seat(player).choose(prompt, labels, min, max)) {
                chosen.add(all.get(i));
            }
            return chosen;
        }
        final List<Card> touched = new ArrayList<>(link.touched(all));
        final List<List<Card>> ways = new ArrayList<>();
        if (touched.size() >= min && touched.size() <= max) {
            ways.add(touched);
        } else if (touched.size() > max) {
            ways.add(new ArrayList<>(touched.subList(0, max)));
        } else {
            final List<Card> filled = new ArrayList<>(touched);
            for (final Card c : all) {
                if (filled.size() >= min) {
                    break;
                }
                if (!filled.contains(c)) {
                    filled.add(c);
                }
            }
            ways.add(filled);
        }
        addSubsets(ways, all, min, max);
        final List<Card> way = ways.get(link.decisions().pick(ways.size()));
        return new ArrayList<>(way);
    }

    private static void addSubsets(final List<List<Card>> ways, final List<Card> all, final int min, final int max) {
        final int limit = 12;
        if (min == 0 && ways.size() < limit && !ways.get(0).isEmpty()) {
            ways.add(new ArrayList<>());
        }
        if (min <= 1 && max >= 1) {
            for (final Card c : all) {
                if (ways.size() >= limit) {
                    return;
                }
                final List<Card> one = new ArrayList<>();
                one.add(c);
                if (!ways.contains(one)) {
                    ways.add(one);
                }
            }
        }
    }

    // ---------------------------------------------------------------- priority and playing

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        return link.priority(player);
    }

    @Override
    public boolean playChosenSpellAbility(final SpellAbility sa) {
        return link.play(this, player, sa);
    }

    @Override
    public boolean playSaFromPlayEffect(final SpellAbility tgtSA) {
        return PlaySpellAbility.playSpellAbility(this, player, tgtSA);
    }

    @Override
    public void playSpellAbilityNoStack(final SpellAbility effectSA, final boolean canSetupTargets) {
        PlaySpellAbility.playSpellAbilityNoStack(this, player, effectSA, !canSetupTargets);
    }

    @Override
    public boolean playTrigger(final Card host, final WrappedAbility wrapperAbility, final boolean isMandatory) {
        return PlaySpellAbility.playSpellAbilityNoStack(this, player, wrapperAbility, false);
    }

    @Override
    public List<SpellAbility> orderSimultaneousSa(final List<SpellAbility> activePlayerSAs) {
        final List<SpellAbility> ordered = new ArrayList<>(activePlayerSAs);
        ordered.sort(Comparator.comparingInt(link::stackPosition));
        return ordered;
    }

    @Override
    public void orderAndPlaySimultaneousSa(final List<SpellAbility> activePlayerSAs) {
        final List<SpellAbility> ordered = orderSimultaneousSa(activePlayerSAs);
        for (int i = ordered.size() - 1; i >= 0; i--) {
            final SpellAbility next = ordered.get(i);
            if (next.isTrigger() && !next.isCopied()) {
                PlaySpellAbility.playSpellAbility(this, player, next);
            } else {
                if (next.isCopied() && next.isSpell()) {
                    if (!next.getHostCard().isInZone(ZoneType.Stack)) {
                        next.setHostCard(player.getGame().getAction().moveToStack(next.getHostCard(), next));
                    } else {
                        player.getGame().getStackZone().add(next.getHostCard());
                    }
                }
                player.getGame().getStack().add(next);
            }
        }
    }

    @Override
    public boolean chooseTargetsFor(final SpellAbility sa) {
        final TableStackItem item = link.declaredFor(sa);
        if (item == null) {
            // nothing on the table for it: the engine has to have something, and the stack won't match
            return JudgeTowerAi.chooseTargets(sa, getAi(), player);
        }
        final int max = sa.getMaxTargets();
        final List<GameObject> targets = link.takeTargets(item, max);
        for (final GameObject target : targets) {
            final boolean legal = target instanceof SpellAbility on ? sa.canTargetSpellAbility(on) : sa.canTarget(target);
            if (!legal) {
                link.judge().illegalTarget(player, sa, target, sa.getHostCard() + " - Cannot target this.");
            }
            sa.getTargets().add(target);
        }
        if (sa.isDividedAsYouChoose() && !targets.isEmpty()) {
            final int total = sa.getStillToDivide();
            final int each = total / targets.size();
            int extra = total - each * targets.size();
            for (final GameObject target : targets) {
                sa.addDividedAllocation(target, each + (extra-- > 0 ? 1 : 0));
            }
        }
        if (targets.size() < sa.getMinTargets()) {
            return JudgeTowerAi.chooseTargets(sa, getAi(), player);
        }
        return true;
    }

    @Override
    public List<AbilitySub> chooseModeForAbility(final SpellAbility sa, final List<AbilitySub> possible, final int min, final int num, final boolean allowRepeat) {
        final TableStackItem item = link.declaredFor(sa);
        if (item != null && !item.modes.isEmpty()) {
            final List<AbilitySub> chosen = new ArrayList<>();
            for (final String mode : item.modes) {
                for (final AbilitySub sub : possible) {
                    if (mode.equals(sub.getDescription()) && (allowRepeat || !chosen.contains(sub))) {
                        chosen.add(sub);
                        break;
                    }
                }
            }
            return chosen;
        }
        return JudgeTowerAi.chooseModes(possible, num);
    }

    @Override
    public Integer announceRequirements(final SpellAbility ability, final int min, final int max, final String announce) {
        final TableStackItem item = link.declaredFor(ability);
        if ("X".equalsIgnoreCase(announce)) {
            return item != null && item.x != null ? item.x : link.judge().requiredX(ability, player, max);
        }
        return pick(announce, numbers(min, max), String::valueOf);
    }

    @Override
    public List<OptionalCostValue> chooseOptionalCosts(final SpellAbility chosen, final List<OptionalCostValue> optionalCostValues) {
        final TableStackItem item = link.declaredFor(chosen);
        if (item == null) {
            return link.judge().requiredOptionalCosts(chosen, optionalCostValues);
        }
        final List<OptionalCostValue> result = new ArrayList<>();
        for (final OptionalCostValue v : optionalCostValues) {
            if (item.optionalCosts.contains(v.toString())) {
                result.add(v);
            }
        }
        return result;
    }

    @Override
    public int chooseNumberForKeywordCost(final SpellAbility sa, final Cost cost, final KeywordInterface keyword, final String prompt, final int max) {
        final TableStackItem item = link.declaredFor(sa);
        if (item != null && item.x != null) {
            return Math.min(max, item.x);
        }
        return max == 1 || !cost.isOnlyManaCost() ? Math.min(max, 1) : Math.min(max, link.judge().getOptions().xValue);
    }

    @Override
    public CostDecisionMakerBase getCostDecisionMaker(final Player payer, final SpellAbility ability, final boolean effect, final String prompt) {
        return new TableCostDecision(payer, ability, effect, link);
    }

    @Override
    public boolean appealJudgeRuling(final String reason) {
        return link.seat(player) != null && link.seat(player).appeal(reason);
    }

    // ---------------------------------------------------------------- combat and the like

    @Override
    public void declareAttackers(final Player attacker, final Combat combat) {
        link.openWindow(player, TableSeat.Window.ATTACK, "Declare your attackers and tap them, then OK.");
        link.attacksOnTable().forEach((card, defender) -> {
            combat.removeFromCombat(card);
            combat.addAttacker(card, defender);
        });
    }

    @Override
    public void declareBlockers(final Player defender, final Combat combat) {
        link.openWindow(player, TableSeat.Window.BLOCK, "Declare your blockers, then OK.");
        link.blocksOnTable(player).forEach((blocker, attackers) -> {
            for (final Card attacker : attackers) {
                if (!CombatUtil.canBlock(attacker, blocker, combat)) {
                    link.judge().illegalBlock(player, blocker, attacker);
                }
                combat.addBlocker(attacker, blocker);
            }
        });
    }

    @Override
    public CardCollectionView chooseCardsToDiscardToMaximumHandSize(final int numDiscard) {
        link.openWindow(player, TableSeat.Window.DISCARD, "Discard down to your maximum hand size (" + numDiscard + " cards), then OK.");
        final CardCollection discarded = new CardCollection();
        final CardCollection others = new CardCollection();
        for (final Card c : player.getCardsIn(ZoneType.Hand)) {
            if (link.zoneOnTable(c) == ZoneType.Hand) {
                others.add(c);
            } else {
                discarded.add(c);
            }
        }
        while (discarded.size() < numDiscard && !others.isEmpty()) {
            discarded.add(others.remove(0));
        }
        return new CardCollection(discarded.subList(0, Math.min(numDiscard, discarded.size())));
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(final Card attacker, final CardCollectionView blockers, final CardCollectionView remaining, final int damageDealt,
            final GameEntity defender, final boolean overrideOrder) {
        final boolean toDefender = defender != null && attacker.hasKeyword(forge.game.keyword.Keyword.TRAMPLE);
        final Map<Card, Integer> map = new HashMap<>();
        if (blockers.isEmpty()) {
            map.put(null, damageDealt);
            return map;
        }
        if (blockers.size() == 1 && !toDefender) {
            map.put(blockers.getFirst(), damageDealt);
            return map;
        }
        final List<Card> onTable = new ArrayList<>();
        final Map<Card, Card> back = new HashMap<>();
        for (final Card b : blockers) {
            final Card t = link.tableCard(b);
            if (t != null) {
                onTable.add(t);
                back.put(t, b);
            }
        }
        final Map<Card, Integer> assigned = link.seat(player).assignDamage(link.tableCard(attacker), onTable, damageDealt, toDefender);
        assigned.forEach((t, amount) -> map.put(t == null ? null : back.get(t), amount));
        return map;
    }

    @Override
    public boolean mulliganKeepHand(final Player mulliganingPlayer, final int cardsToReturn) {
        return true;
    }

    // ---------------------------------------------------------------- effects' decisions

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(final FCollectionView<T> optionList, final DelayedReveal delayedReveal, final SpellAbility sa, final String title,
            final boolean isOptional, final Player relatedPlayer, final Map<String, Object> params) {
        final List<T> options = new ArrayList<>();
        optionList.forEach(options::add);
        final CardCollection touched = new CardCollection();
        for (final T o : options) {
            if (o instanceof Card c) {
                touched.add(c);
            }
        }
        final CardCollection evidence = link.isSearching() ? link.touched(touched) : new CardCollection();
        options.sort(Comparator.comparingInt(o -> o instanceof Card c && evidence.contains(c) ? 0 : 1));
        return pick(title, options, Object::toString);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(final FCollectionView<T> optionList, final int min, final int max, final DelayedReveal delayedReveal, final SpellAbility sa,
            final String title, final Player relatedPlayer, final Map<String, Object> params) {
        final List<T> options = new ArrayList<>();
        optionList.forEach(options::add);
        if (options.isEmpty()) {
            return options;
        }
        if (options.get(0) instanceof Card) {
            return (List<T>) pickCards(title, (List<Card>) options, min, max);
        }
        final T one = pick(title, options, Object::toString);
        return one == null ? new ArrayList<>() : new ArrayList<>(Collections.singletonList(one));
    }

    @Override
    public CardCollectionView chooseCardsForEffect(final CardCollectionView sourceList, final SpellAbility sa, final String title, final int min, final int max, final boolean isOptional,
            final Map<String, Object> params) {
        return new CardCollection(pickCards(title, sourceList, isOptional ? 0 : min, max));
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(final SpellAbility sa, final int min, final int max, final CardCollectionView validTargets, final String message) {
        return new CardCollection(pickCards(message, validTargets, min, max));
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(final SpellAbility sa, final int min, final int max, final CardCollectionView validTargets, final String message) {
        return new CardCollection(pickCards(message, validTargets, min, max));
    }

    @Override
    public CardCollection chooseCardsToDiscardFrom(final Player playerDiscard, final SpellAbility sa, final CardCollection validCards, final int min, final int max,
            final CardCollectionView visibleToChooser) {
        return new CardCollection(pickCards("Discard", validCards, min, max));
    }

    @Override
    public CardCollectionView chooseCardsToRevealFromHand(final int min, final int max, final CardCollectionView valid) {
        return new CardCollection(pickCards("Reveal", valid, min, max));
    }

    @Override
    public Card chooseSingleCardForZoneChange(final ZoneType destination, final List<ZoneType> origin, final SpellAbility sa, final CardCollection fetchList, final DelayedReveal delayedReveal,
            final String selectPrompt, final boolean isOptional, final Player decider) {
        final List<Card> chosen = pickCards(selectPrompt, fetchList, isOptional ? 0 : 1, 1);
        return chosen.isEmpty() ? null : chosen.get(0);
    }

    @Override
    public List<Card> chooseCardsForZoneChange(final ZoneType destination, final List<ZoneType> origin, final SpellAbility sa, final CardCollection fetchList, final int min, final int max,
            final DelayedReveal delayedReveal, final String selectPrompt, final Player decider) {
        return pickCards(selectPrompt, fetchList, min, max);
    }

    @Override
    public boolean confirmAction(final SpellAbility sa, final PlayerActionConfirmMode mode, final String message, final List<String> options, final Card cardToShow,
            final Map<String, Object> params) {
        // what a player may do, they must. A choice between named options is no such thing
        if (options == null || options.isEmpty()) {
            return true;
        }
        return pick(message, List.of(true, false), yes -> options.get(yes ? 0 : Math.min(1, options.size() - 1)));
    }

    @Override
    public boolean chooseBinary(final SpellAbility sa, final String question, final BinaryChoiceType kindOfChoice, final Boolean defaultChoice) {
        return pick(question, List.of(true, false), String::valueOf);
    }

    @Override
    public boolean chooseFlipResult(final SpellAbility sa, final Player flipper, final boolean call) {
        return pick("Flip result", List.of(true, false), String::valueOf);
    }

    private static List<Integer> numbers(final int min, final int max) {
        final List<Integer> values = new ArrayList<>();
        for (int i = min; i <= max && values.size() < 24; i++) {
            values.add(i);
        }
        return values;
    }

    @Override
    public int chooseNumber(final SpellAbility sa, final String title, final int min, final int max) {
        final List<Integer> values = numbers(min, max);
        values.sort(Comparator.comparingInt(v -> link.wasAnnounced(String.valueOf(v)) ? 0 : 1));
        final Integer value = pick(title, values, String::valueOf);
        return value == null ? min : value;
    }

    @Override
    public int chooseNumber(final SpellAbility sa, final String title, final List<Integer> values, final Player relatedPlayer) {
        final List<Integer> options = new ArrayList<>(values);
        options.sort(Comparator.comparingInt(v -> link.wasAnnounced(String.valueOf(v)) ? 0 : 1));
        final Integer value = pick(title, options, String::valueOf);
        return value == null ? 0 : value;
    }

    private String pickAnnounced(final String prompt, final Collection<String> options) {
        final List<String> list = new ArrayList<>(options);
        list.sort(Comparator.comparingInt(o -> link.wasAnnounced(o) ? 0 : 1));
        return pick(prompt, list, o -> o);
    }

    @Override
    public String chooseSomeType(final String kindOfType, final SpellAbility sa, final Collection<String> validTypes, final boolean isOptional) {
        return pickAnnounced("Choose a " + kindOfType + " type", validTypes);
    }

    @Override
    public String chooseProtectionType(final SpellAbility sa, final List<String> choices) {
        return pickAnnounced("Choose a protection", choices);
    }

    @Override
    public byte chooseColor(final String message, final SpellAbility sa, final ColorSet colors) {
        final List<Byte> options = new ArrayList<>();
        for (final byte b : ManaAtom.MANATYPES) {
            if (colors.hasAnyColor(b)) {
                options.add(b);
            }
        }
        options.sort(Comparator.comparingInt(b -> link.wasAnnounced(colorName(b)) ? 0 : 1));
        final Byte chosen = pick(message, options, PlayerControllerJudge::colorName);
        return chosen == null ? 0 : chosen;
    }

    private static String colorName(final byte b) {
        return forge.card.MagicColor.toLongString(b);
    }

    @Override
    public CounterType chooseCounterType(final List<CounterType> options, final SpellAbility sa, final String prompt, final Map<String, Object> params) {
        return pick(prompt, new ArrayList<>(options), Object::toString);
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(final List<SpellAbility> spells, final SpellAbility sa, final String title, final Map<String, Object> params) {
        return pick(title, new ArrayList<>(spells), Object::toString);
    }

    @Override
    public Object vote(final SpellAbility sa, final String prompt, final List<Object> options, final ListMultimap<Object, Player> votes, final Player forPlayer, final boolean optional) {
        return pick(prompt, new ArrayList<>(options), Object::toString);
    }

    @Override
    public boolean chooseCardsPile(final SpellAbility sa, final CardCollectionView pile1, final CardCollectionView pile2, final String faceUp) {
        return pick("Choose a pile", List.of(true, false), yes -> yes ? pile1.toString() : pile2.toString());
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(final CardCollection topN) {
        final CardCollection top = new CardCollection();
        final CardCollection bottom = new CardCollection();
        // the table's Tower says it: in order of where they are, those that went past everything else to the bottom
        final List<Card> byPosition = new ArrayList<>(topN);
        byPosition.sort(Comparator.comparingInt(link::towerPosition));
        final int towerSize = player.getCardsIn(ZoneType.Library).size();
        for (final Card c : byPosition) {
            if (link.towerPosition(c) >= towerSize - topN.size() && towerSize > topN.size()) {
                bottom.add(c);
            } else {
                top.add(c);
            }
        }
        return ImmutablePair.of(top, bottom);
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(final CardCollection topN) {
        final CardCollection top = new CardCollection();
        final CardCollection graveyard = new CardCollection();
        final List<Card> byPosition = new ArrayList<>(topN);
        byPosition.sort(Comparator.comparingInt(link::towerPosition));
        for (final Card c : byPosition) {
            if (link.zoneOnTable(c) == ZoneType.Graveyard) {
                graveyard.add(c);
            } else {
                top.add(c);
            }
        }
        return ImmutablePair.of(top, graveyard);
    }

    @Override
    public boolean willPutCardOnTop(final Card c) {
        return link.towerPosition(c) <= 0;
    }

    @Override
    public CardCollectionView orderMoveToZoneList(final CardCollectionView cards, final ZoneType destinationZone, final SpellAbility source) {
        if (destinationZone != ZoneType.Library) {
            return cards;
        }
        // the last one put on top is the first on the table
        final List<Card> ordered = new ArrayList<>();
        cards.forEach(ordered::add);
        ordered.sort(Comparator.comparingInt((Card c) -> link.towerPosition(c)).reversed());
        return new CardCollection(ordered);
    }

    @Override
    public boolean payCostToPreventEffect(final Cost cost, final SpellAbility sa, final boolean alreadyPaid, final FCollectionView<Player> allPayers) {
        // "unless" costs are always a choice: the table says which by what's been paid
        final boolean pay = pick("Pay " + cost.toSimpleString() + "?", List.of(true, false), String::valueOf);
        return pay && super.payCostToPreventEffect(cost, sa, alreadyPaid, allPayers);
    }

    @Override
    public String chooseCardName(final SpellAbility sa, final Predicate<ICardFace> cpp, final String valid, final String message) {
        for (final String a : announcedNames()) {
            return a;
        }
        return super.chooseCardName(sa, cpp, valid, message);
    }

    private List<String> announcedNames() {
        return link.announcedStrings();
    }
}
