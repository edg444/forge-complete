package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import forge.util.TextUtil;

public class CountersMoveEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        // nothing is chosen until it resolves, so there's no source or destination to name yet
        if (sa.hasParam("ChooseOnResolution")) {
            return sa.getParamOrDefault("SpellDescription", "Move a counter from one permanent onto another.");
        }
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

        Card source = null;
        if (sa.usesTargeting() && sa.getMinTargets() == 2) {
            if (tgtCards.size() < 2) {
                return "";
            }
            source = tgtCards.remove(0);
        } else {
            List<Card> srcCards = getDefinedCardsOrTargeted(sa, "Source");

            if (srcCards.size() > 0) {
                source = srcCards.get(0);
            }
        }
        final String countername = sa.getParam("CounterType");
        final String counterAmount = sa.getParamOrDefault("CounterNum", "1");
        int amount = 0;
        if (!"Any".equals(counterAmount) && !"All".equals(counterAmount)) {
            amount = AbilityUtils.calculateAmount(sa.getHostCard(), counterAmount, sa);
        }

        sb.append("Move ");
        if ("Any".matches(countername)) {
            if (amount == 1) {
                sb.append("a counter");
            } else {
                sb.append(amount).append(" ").append(" counter");
            }
        } else if ("All".equals(countername)) {
            sb.append("all counter");
        } else {
            sb.append(amount).append(" ").append(countername).append(" counter");
        }
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" from ").append(source).append(" to ");
        try {
            sb.append(tgtCards.get(0));
        } catch (final IndexOutOfBoundsException exception) {
            System.out.println(TextUtil.concatWithSpace("Somehow this is missing targets?", source.toString()));
        }

        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final String counterName = sa.getParam("CounterType");
        final String counterNum = sa.getParamOrDefault("CounterNum", "1");
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();
        final Game game = host.getGame();

        CounterType cType = null;
        if (!counterName.matches("Any") && !counterName.matches("All")) {
            try {
                cType = CounterType.getType(counterName);
            } catch (Exception e) {
                System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                return;
            }
        }

        GameEntityCounterTable table = new GameEntityCounterTable();

        if (sa.hasParam("ChooseOnResolution")) {
            moveChosenCounter(sa, table);
        } else if (sa.hasParam("ValidSource")) {
            // uses for multi sources -> one defined/target
            // this needs given counter type
            CardCollectionView srcCards = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("ValidSource"), activator, host, sa);
            List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

            if (tgtCards.isEmpty()) {
                return;
            }
            Card dest = tgtCards.get(0);

            Card cur = game.getCardState(dest, null);
            if (cur == null || !cur.equalsWithGameTimestamp(dest)) {
                // Test to see if the card we're trying to add is in the expected state
                return;
            }
            dest = cur;

            Map<String, Object> params = Maps.newHashMap();
            params.put("Target", dest);

            if ("All".equals(counterName)) {
                // only select cards if the counterNum is any
                if (counterNum.equals("Any")) {
                    srcCards = CardLists.filter(srcCards, CardPredicates.hasCounters());
                    srcCards = activator.getController().chooseCardsForEffect(srcCards, sa,
                            Localizer.getInstance().getMessage("lblChooseTakeCountersCard", "any"), 0,
                            srcCards.size(), true, params);
                }
            } else {
                // target can't receive this counter type
                if (!dest.canReceiveCounters(cType)) {
                    return;
                }
                srcCards = CardLists.filter(srcCards, CardPredicates.hasCounter(cType));

                // only select cards if the counterNum is any
                if (counterNum.equals("Any")) {
                    params.put("CounterType", cType);
                    srcCards = activator.getController().chooseCardsForEffect(srcCards, sa,
                            Localizer.getInstance().getMessage("lblChooseTakeCountersCard", cType.getName()), 0,
                            srcCards.size(), true, params);
                }
            }

            Multiset<CounterType> countersToAdd = HashMultiset.create();

            for (Card src : srcCards) {
                if ("All".equals(counterName)) {
                    final Multiset<CounterType> tgtCounters = HashMultiset.create(src.getCounters());
                    for (Multiset.Entry<CounterType> e : tgtCounters.entrySet()) {
                        removeCounter(sa, src, dest, e.getElement(), counterNum, countersToAdd);
                    }
                } else {
                    removeCounter(sa, src, dest, cType, counterNum, countersToAdd);
                }
            }
            for (Multiset.Entry<CounterType> e : countersToAdd.entrySet()) {
                dest.addCounter(e.getElement(), e.getCount(), activator, table);
            }

            game.updateLastStateForCard(dest);
        } else if (sa.hasParam("ValidDefined")) {
            // one Source to many Targets
            // need given CounterType
            // currently used for Forgotten Ancient
            List<Card> srcCards = getDefinedCardsOrTargeted(sa, "Source");
            if (srcCards.isEmpty()) {
                return;
            }
            Card source = srcCards.get(0);

            if (source.getCounters(cType) <= 0) {
                return;
            }
            Map<String, Object> params = Maps.newHashMap();
            params.put("CounterType", cType);
            params.put("Source", source);

            CardCollectionView tgtCards = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("ValidDefined"), activator, host, sa);

            if (counterNum.equals("Any")) {
                tgtCards = activator.getController().chooseCardsForEffect(
                        tgtCards, sa, Localizer.getInstance().getMessage("lblChooseCardToGetCountersFrom",
                                cType.getName(), source.getTranslatedName()),
                        0, tgtCards.size(), true, params);
            }

            boolean updateSource = false;

            for (final Card dest : tgtCards) {
                // rule 121.5: If the first and second objects are the same object, nothing happens
                if (source.equals(dest)) {
                    continue;
                }
                if (!dest.canReceiveCounters(cType)) {
                    continue;
                }
                if (!source.canRemoveCounters(cType)) {
                    continue;
                }

                Card cur = game.getCardState(dest, null);
                if (cur == null || !cur.equalsWithGameTimestamp(dest)) {
                    // Test to see if the card we're trying to add is in the expected state
                    continue;
                }

                params = Maps.newHashMap();
                params.put("CounterType", cType);
                params.put("Source", source);
                params.put("Target", cur);
                int cnum = activator.getController().chooseNumber(sa,
                        Localizer.getInstance().getMessage("lblPutHowManyTargetCounterOnCard", cType.getName(),
                                cur.getTranslatedName()),
                        0, source.getCounters(cType), params);

                if (cnum > 0) {
                    source.subtractCounter(cType, cnum, activator);
                    cur.addCounter(cType, cnum, activator, table);
                    game.updateLastStateForCard(cur);
                    updateSource = true;
                }
            }
            if (updateSource) {
                // update source
                game.updateLastStateForCard(source);
            }
        } else {
            Card source = null;
            List<Card> tgtCards = getDefinedCardsOrTargeted(sa);
            // special logic for moving from Target to Target
            if (sa.usesTargeting() && sa.getMinTargets() == 2) {
                if (tgtCards.size() < 2) {
                    return;
                }
                source = tgtCards.remove(0);
            } else {
                List<Card> srcCards = getDefinedCardsOrTargeted(sa, "Source");
                if (srcCards.size() > 0) {
                    source = srcCards.get(0);
                }
            }
            if (source == null) {
                return;
            }

            // source doesn't has any counters to move
            if (!source.hasCounters()) {
                return;
            }

            for (final Card dest : tgtCards) {
                if (null != dest) {
                    // rule 121.5: If the first and second objects are the same object, nothing happens
                    if (source.equals(dest)) {
                        continue;
                    }
                    Card cur = game.getCardState(dest, null);
                    if (cur == null || !cur.equalsWithGameTimestamp(dest)) {
                        // Test to see if the card we're trying to add is in the expected state
                        continue;
                    }

                    final CounterType convertType = sa.hasParam("ConvertToDestinationType")
                            ? chooseConvertedCounterType(sa, cur) : null;

                    Multiset<CounterType> countersToAdd = HashMultiset.create();
                    if ("All".equals(counterName)) {
                        final Multiset<CounterType> tgtCounters = HashMultiset.create(source.getCounters());
                        for (CounterType e : tgtCounters.elementSet()) {
                            removeCounter(sa, source, cur, e, convertType != null ? convertType : e, counterNum, countersToAdd);
                        }
                    } else if ("EachNotOn".equals(counterName)) {
                        final Multiset<CounterType> tgtCounters = HashMultiset.create(source.getCounters());
                        for (CounterType e : tgtCounters.elementSet()) {
                            if (cur.getCounters(e) > 0) {
                                continue;
                            }
                            removeCounter(sa, source, cur, e, convertType != null ? convertType : e, counterNum, countersToAdd);
                        }
                    } else if ("Any".equals(counterName)) {
                        // any counterType currently only Leech Bonder
                        final Multiset<CounterType> tgtCounters = source.getCounters();

                        final List<CounterType> typeChoices = Lists.newArrayList();
                        // get types of counters
                        for (CounterType ct : tgtCounters.elementSet()) {
                            if (dest.canReceiveCounters(convertType != null ? convertType : ct) && source.canRemoveCounters(ct)) {
                                typeChoices.add(ct);
                            }
                        }
                        if (typeChoices.isEmpty()) {
                            return;
                        }

                        while (!typeChoices.isEmpty()) {
                            Map<String, Object> params = Maps.newHashMap();
                            params.put("Source", source);
                            params.put("Target", dest);
                            String title = Localizer.getInstance().getMessage("lblSelectRemoveCounterType");
                            CounterType chosenType = pc.chooseCounterType(typeChoices, sa, title, params);

                            removeCounter(sa, source, cur, chosenType, convertType != null ? convertType : chosenType, counterNum, countersToAdd);
                            if (!counterNum.equals("Any")) {
                                break;
                            }
                            typeChoices.remove(chosenType);
                        }
                    } else {
                        removeCounter(sa, source, cur, cType, convertType != null ? convertType : cType, counterNum, countersToAdd);
                    }

                    for (Multiset.Entry<CounterType> e : countersToAdd.entrySet()) {
                        cur.addCounter(convertType != null ? convertType : e.getElement(), e.getCount(), activator, table);
                    }
                }
            }
            // update source
            game.updateLastStateForCard(source);
        }
        table.replaceCounterEffect(game, sa);
    } // moveCounterResolve

    /**
     * Giant Fan and Everythingamajig: "Move a counter from one permanent onto another." No target
     * is named, so both permanents are chosen as it resolves - hexproof, shroud and protection
     * don't stop it. Rule 122.5 covers the rest: if the counter can't come off the first or go on
     * the second, nothing moves.
     */
    private void moveChosenCounter(final SpellAbility sa, final GameEntityCounterTable table) {
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();
        final Game game = activator.getGame();

        final CardCollectionView withCounters = CardLists.filter(game.getCardsIn(ZoneType.Battlefield),
                CardPredicates.hasCounters());
        if (withCounters.isEmpty()) {
            return;
        }
        Map<String, Object> params = Maps.newHashMap();
        params.put("MoveRole", "Source");
        final Card source = pc.chooseSingleEntityForEffect(withCounters, sa,
                "Choose the permanent to move a counter from", params);
        if (source == null) {
            return;
        }

        final CardCollectionView others = CardLists.filter(game.getCardsIn(ZoneType.Battlefield),
                c -> !c.equals(source));
        if (others.isEmpty()) {
            return;
        }
        params = Maps.newHashMap();
        params.put("MoveRole", "Destination");
        params.put("Source", source);
        final Card dest = pc.chooseSingleEntityForEffect(others, sa,
                "Choose the permanent to move the counter from " + source.getName() + " onto", params);
        if (dest == null) {
            return;
        }

        final List<CounterType> kinds = Lists.newArrayList(source.getCounters().elementSet());
        kinds.removeIf(ct -> !source.canRemoveCounters(ct));
        if (kinds.isEmpty()) {
            return;
        }
        params = Maps.newHashMap();
        params.put("MoveRole", "Remove");
        params.put("Source", source);
        params.put("Target", dest);
        final CounterType removed = pc.chooseCounterType(kinds, sa,
                Localizer.getInstance().getMessage("lblSelectRemoveCounterType"), params);

        final CounterType becomes = sa.hasParam("ConvertToDestinationType")
                ? chooseConvertedCounterType(sa, dest) : removed;

        final Multiset<CounterType> countersToAdd = HashMultiset.create();
        removeCounter(sa, source, dest, removed, becomes, "1", countersToAdd);
        if (!countersToAdd.isEmpty()) {
            dest.addCounter(becomes, countersToAdd.size(), activator, table);
            game.updateLastStateForCard(dest);
        }
        game.updateLastStateForCard(source);
    }

    // "If the second permanent refers to any kind of counter, the moved counter becomes one of those
    // counters. Otherwise, it becomes a +1/+1 counter." When it names several kinds, the player picks.
    private static CounterType chooseConvertedCounterType(final SpellAbility sa, final Card dest) {
        final List<CounterType> referred = referredCounterTypes(dest);
        if (referred.isEmpty()) {
            return CounterEnumType.P1P1;
        }
        final Map<String, Object> params = Maps.newHashMap();
        params.put("MoveRole", "Becomes");
        params.put("Target", dest);
        return sa.getActivatingPlayer().getController().chooseCounterType(referred, sa,
                "Choose the kind of counter it becomes on " + dest.getName(), params);
    }

    /**
     * The kinds of counter a permanent's text refers to: each kind named as "... counter(s)" in its
     * text box (stolen boxes count, see Card.getTextBoxOracle), plus loyalty for a planeswalker -
     * the Unstable FAQ says its loyalty costs are symbols that refer to loyalty counters - and
     * defense for a battle, whose defense is counted the same way.
     */
    public static List<CounterType> referredCounterTypes(final Card dest) {
        final java.util.Set<CounterType> found = new java.util.LinkedHashSet<>();
        if (dest.isPlaneswalker()) {
            found.add(CounterEnumType.LOYALTY);
        }
        if (dest.isBattle()) {
            found.add(CounterEnumType.DEFENSE);
        }
        final String text = dest.getTextBoxOracle().toLowerCase();
        for (final CounterType ct : CounterType.getValues()) {
            final String name = ct.getName().toLowerCase().replace('_', ' ');
            // whole word only, so "age counter" isn't found inside "page counter"
            if (java.util.regex.Pattern.compile("(?<![\\p{L}\\p{N}])" + java.util.regex.Pattern.quote(name) + " counter")
                    .matcher(text).find()) {
                found.add(ct);
            }
        }
        return Lists.newArrayList(found);
    }

    protected void removeCounter(SpellAbility sa, final Card src, final Card dest, CounterType cType, String counterNum, Multiset<CounterType> countersToAdd) {
        removeCounter(sa, src, dest, cType, cType, counterNum, countersToAdd);
    }

    protected void removeCounter(SpellAbility sa, final Card src, final Card dest, CounterType cType, CounterType destCheckType, String counterNum, Multiset<CounterType> countersToAdd) {
        final Card host = sa.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final PlayerController pc = activator.getController();
        final Game game = host.getGame();

        // rule 121.5: If the first and second objects are the same object, nothing happens
        if (src.equals(dest)) {
            return;
        }

        if (!dest.canReceiveCounters(destCheckType)) {
            return;
        }
        if (!src.canRemoveCounters(cType)) {
            return;
        }

        int cmax = src.getCounters(cType);
        if (cmax <= 0) {
            return;
        }

        int cnum = 0;
        if (counterNum.equals("All")) {
            cnum = cmax;
        } else if (counterNum.equals("Any")) {
            Map<String, Object> params = Maps.newHashMap();
            params.put("CounterType", cType);
            params.put("Source", src);
            params.put("Target", dest);
            int min = sa.hasParam("NonZero") && countersToAdd.isEmpty() ? 1 : 0;
            cnum = pc.chooseNumber(
                    sa, Localizer.getInstance().getMessage("lblTakeHowManyTargetCounterFromCard",
                            cType.getName(), src.getTranslatedName()),
                    min, cmax, params);
        } else {
            cnum = Math.min(cmax, AbilityUtils.calculateAmount(host, counterNum, sa));
        }
        if (cnum > 0) {
            src.subtractCounter(cType, cnum, activator);
            game.updateLastStateForCard(src);
            countersToAdd.add(cType, cnum);
        }
    }
}
