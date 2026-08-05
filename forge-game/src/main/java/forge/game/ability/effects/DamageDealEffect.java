package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardDamageTable;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.*;
import forge.util.collect.FCollection;

public class DamageDealEffect extends DamageBaseEffect {

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility spellAbility) {
        // when damageStackDescription is called, just build exactly what is happening
        final StringBuilder stringBuilder = new StringBuilder();
        final String damage = spellAbility.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(spellAbility.getHostCard(), damage, spellAbility);

        List<GameObject> targets = getTargets(spellAbility);
        final List<Card> definedSources = AbilityUtils.getDefinedCards(spellAbility.getHostCard(), spellAbility.getParam("DamageSource"), spellAbility);

        if (targets.isEmpty() || definedSources.isEmpty()) {
            return "";
        }

        stringBuilder.append(definedSources.get(0).toString()).append(" deals").append(" ").append(dmg).append(" damage ");

        // if use targeting we show all targets and corresponding damage
        if (spellAbility.usesTargeting()) {
            if (spellAbility.hasParam("DivideEvenly")) {
                stringBuilder.append("divided evenly (rounded down) to \n");
            } else if (spellAbility.isDividedAsYouChoose()) {
                stringBuilder.append("divided to \n");
            } else
                stringBuilder.append("to ");

            final List<Card> targetCards = getTargetCards(spellAbility);
            final List<Player> players = getTargetPlayers(spellAbility);

            int targetCount = targetCards.size() + players.size();

            // target cards
            for (int i = 0; i < targetCards.size(); i++) {
                Card targetCard = targetCards.get(i);
                stringBuilder.append(targetCard);
                Integer v = spellAbility.getDividedValue(targetCard);
                if (v != null) //fix null damage stack description
                    stringBuilder.append(" (").append(v).append(" damage)");

                if (i == targetCount - 2) {
                    stringBuilder.append(" and ");
                } else if (i + 1 < targetCount) {
                    stringBuilder.append(", ");
                }
            }

            // target players
            for (int i = 0; i < players.size(); i++) {
                Player targetPlayer = players.get(i);
                stringBuilder.append(targetPlayer);
                Integer v = spellAbility.getDividedValue(targetPlayer);
                if (v != null) //fix null damage stack description
                    stringBuilder.append(" (").append(v).append(" damage)");

                if (i == players.size() - 2) {
                    stringBuilder.append(" and ");
                } else if (i + 1 < players.size()) {
                    stringBuilder.append(", ");
                }
            }
        } else {
            if (spellAbility.hasParam("DivideEvenly")) {
                stringBuilder.append("divided evenly (rounded down) ");
            } else if (spellAbility.isDividedAsYouChoose()) {
                stringBuilder.append("divided as you choose ");
            }
            stringBuilder.append("to ").append(Lang.joinHomogenous(targets));
        }

        if (spellAbility.hasParam("Radiance")) {
            stringBuilder.append(" and each other ").append(spellAbility.getParam("ValidTgts"))
                    .append(" that shares a color with ");
            if (targets.size() > 1) {
                stringBuilder.append("them");
            } else {
                stringBuilder.append("it");
            }
        }

        stringBuilder.append(".");
        if (spellAbility.hasParam("ReplaceDyingDefined")) {
            String statement = "If that creature would die this turn, exile it instead.";
            String[] sentences = spellAbility.getParamOrDefault("SpellDescription", "").split("\\.");
            for (String s : sentences) {
                if (s.contains("would die")) {
                    statement = s;
                    break;
                }
            }
            stringBuilder.append(" ").append(statement);
        }
        return stringBuilder.toString();
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#resolve(forge.game.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game game = hostCard.getGame();

        // Double Deal-style delayed damage: the game this resolves in ends before the damage
        // applies, so it can't be a normal card trigger - queue it on the Match instead (see
        // DrawEffect/DiscardEffect and Match.java for the identical Double Dip mechanism).
        if (sa.hasParam("NextGameFirstUpkeep")) {
            final int queuedDmg = AbilityUtils.calculateAmount(hostCard, sa.getParam("NumDmg"), sa);
            game.getMatch().queueFirstUpkeepDamage(sa.getActivatingPlayer().getRegisteredPlayer(), queuedDmg);
            return;
        }

        final List<Card> definedSources = AbilityUtils.getDefinedCards(hostCard, sa.getParam("DamageSource"), sa);
        if (definedSources == null || definedSources.isEmpty()) {
            return;
        }

        for (Card source : definedSources) {
            // Run replacement effects
            game.getReplacementHandler().run(ReplacementType.AssignDealDamage, AbilityKey.mapFromAffected(source));
        }

        int dmg = AbilityUtils.calculateAmount(hostCard, sa.getParam("NumDmg"), sa);

        // "Damage equal to its power" has to keep a 1/2 creature's half instead of rounding it away.
        // Halves$ True says NumDmg already counts halves; otherwise a power-derived amount is
        // re-read in halves automatically, so the hundreds of cards that word it that way need no
        // script change. The whole part goes through the damage map as usual and the leftover half
        // is marked afterwards, the same way combat damage handles it.
        int dmgInHalves = sa.hasParam("Halves") ? dmg : powerDerivedHalves(hostCard, sa);
        final boolean halves = dmgInHalves >= 0 && !sa.isDividedAsYouChoose();
        final boolean oddHalf = halves && dmgInHalves % 2 != 0;
        if (halves) {
            dmg = dmgInHalves / 2;
        }
        final List<GameEntity> halfTargets = Lists.newArrayList();

        final boolean divideOnResolution = sa.hasParam("DividerOnResolution");

        List<GameEntity> tgts = Lists.newArrayList();
        if (sa.hasParam("CardChoices") || sa.hasParam("PlayerChoices")) { // choosing outside Defined/Targeted
            final Player activator = sa.getActivatingPlayer();
            FCollection<GameEntity> choices = new FCollection<>();
            if (sa.hasParam("CardChoices")) {
                choices.addAll(CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield),
                        sa.getParam("CardChoices"), activator, hostCard, sa));
            }
            if (sa.hasParam("PlayerChoices")) {
                choices.addAll(AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("PlayerChoices"), sa));
            }

            int n = sa.hasParam("ChoiceAmount") ?
                    AbilityUtils.calculateAmount(hostCard, sa.getParam("ChoiceAmount"), sa) : 1;
            if (sa.hasParam("Random")) { // only for Whimsy and Faerie Dragon
                for (int i = 0; i < n; i++) {
                    GameEntity random = Aggregates.random(choices);
                    tgts.add(random);
                    choices.remove(random);
                    hostCard.addRemembered(random); // remember random choices for log
                }
            } else { // only for Comet, Stellar Pup
                final String prompt = sa.hasParam("ChoicePrompt") ? sa.getParam("ChoicePrompt") :
                        Localizer.getInstance().getMessage("lblChooseEntityDmg");
                tgts.addAll(activator.getController().chooseEntitiesForEffect(choices, n, n, null, sa,
                        prompt, null, null));
            }
        } else {
            tgts = getTargetEntities(sa);
        }

        if (sa.hasParam("OptionalDecider")) {
            Player decider = Iterables.getFirst(AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("OptionalDecider"), sa), null);
            if (decider != null && !decider.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoyouWantDealTargetDamageToTarget", dmg, tgts), null)) {
                return;
            }
        }

        // Right now for Fireball, maybe later for other stuff
        if (sa.hasParam("DivideEvenly")) {
            String evenly = sa.getParam("DivideEvenly");
            if (evenly.equals("RoundedDown")) {
                dmg = tgts.isEmpty() ? 0 : dmg / tgts.size();
            }
        }

        final CardCollection untargetedCards = CardUtil.getRadiance(sa);

        //Remember params from this effect have been moved to dealDamage in GameAction
        boolean usedDamageMap = true;
        CardDamageTable damageMap = sa.getDamageMap();
        CardDamageTable preventMap = sa.getPreventMap();
        GameEntityCounterTable counterTable = sa.getCounterTable();

        if (damageMap == null) {
            // make a new damage map
            damageMap = new CardDamageTable();
            preventMap = new CardDamageTable();
            counterTable = new GameEntityCounterTable();
            usedDamageMap = false;
        }
        if (sa.hasParam("DamageMap")) {
            sa.setDamageMap(damageMap);
            sa.setPreventMap(preventMap);
            sa.setCounterTable(counterTable);
            usedDamageMap = true;
        }

        for (Card source : definedSources) {
            final Card sourceLKI = hostCard.getGame().getChangeZoneLKIInfo(source);

            if (divideOnResolution) {
                // Dividing Damage up to multiple targets using combat damage box
                // Currently only used for Master of the Wild Hunt
                List<Player> players = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("DividerOnResolution"), sa);
                if (players.isEmpty()) {
                    return;
                }

                CardCollection assigneeCards = new CardCollection(IterableUtil.filter(tgts, Card.class));

                Player assigningPlayer = players.get(0);
                Map<Card, Integer> map = assigningPlayer.getController().assignCombatDamage(sourceLKI, assigneeCards, null, dmg, null, true);
                for (Entry<Card, Integer> dt : map.entrySet()) {
                    damageMap.put(sourceLKI, dt.getKey(), dt.getValue());
                }

                if (!usedDamageMap) {
                    game.getAction().dealDamage(false, damageMap, preventMap, counterTable, sa);
                }
                replaceDying(sa);
                return;
            }

            if (sa.hasParam("RelativeTarget")) {
                tgts = AbilityUtils.getDefinedEntities(source, sa.getParam("Defined"), sa);
            }

            for (final GameEntity o : tgts) {
                dmg = (sa.usesTargeting() && sa.isDividedAsYouChoose()) ? sa.getDividedValue(o) : dmg;
                if (dmg <= 0 && !oddHalf) {
                    continue;
                }
                if (o instanceof Card c) {
                    final Card gc = game.getCardState(c, null);
                    if (gc == null || !c.equalsWithGameTimestamp(gc) || !gc.isInPlay() || gc.isPhasedOut()) {
                        // timestamp different or not in play
                        continue;
                    }
                    if (dmg > 0) {
                        internalDamageDeal(sa, sourceLKI, gc, dmg, damageMap);
                    }
                    if (oddHalf) {
                        halfTargets.add(gc);
                    }
                } else if (o instanceof Player p) {
                    if (dmg > 0) {
                        damageMap.put(sourceLKI, p, dmg);
                    }
                    if (oddHalf) {
                        halfTargets.add(p);
                    }
                }
            }
            for (final Card unTgtC : untargetedCards) {
                if (unTgtC.isInPlay()) {
                    internalDamageDeal(sa, sourceLKI, unTgtC, dmg, damageMap);
                }
            }
        }
        if (!usedDamageMap) {
            game.getAction().dealDamage(false, damageMap, preventMap, counterTable, sa);
        }
        // after the whole damage, so a fractional prevention shield gets first claim on the half
        for (final GameEntity halfTarget : halfTargets) {
            if (halfTarget.useHalfPreventShield()) {
                continue;
            }
            if (halfTarget instanceof Card halfCard) {
                halfCard.addHalfDamage();
            } else if (halfTarget instanceof Player halfPlayer) {
                // a player's half only queues up - dealDamage already flushed the queue on its way
                // out, so without this the half would wait for some later damage to cash it in
                halfPlayer.addHalfDamage();
                halfPlayer.processDamage();
            }
        }
        replaceDying(sa);
    }

    /**
     * Re-reads a damage amount that came from a creature's power or toughness in halves, so a 1/2
     * creature contributes 1 half rather than 0. Returns -1 when the amount isn't power-derived.
     */
    private static int powerDerivedHalves(Card hostCard, SpellAbility sa) {
        final String amount = sa.getParam("NumDmg");
        if (StringUtils.isBlank(amount)) {
            return -1;
        }
        String svarval = amount.indexOf('$') > 0 ? amount : sa.getSVar(amount);
        if (StringUtils.isBlank(svarval)) {
            svarval = hostCard.getSVar(amount);
        }
        if (StringUtils.isBlank(svarval) || svarval.contains("Halves")
                || (!svarval.contains("CardPower") && !svarval.contains("CardToughness"))) {
            return -1;
        }
        final String inHalves = svarval.replace("CardPower", "CardPowerHalves")
                .replace("CardToughness", "CardToughnessHalves");
        return AbilityUtils.calculateAmount(hostCard, inHalves, sa);
    }

    protected void internalDamageDeal(SpellAbility sa, Card sourceLKI, Card c, int dmg, CardDamageTable damageMap) {
        final Card hostCard = sa.getHostCard();
        final Player activationPlayer = sa.getActivatingPlayer();
        int excess = 0;
        int dmgToTarget = 0;
        if (sa.hasParam("ExcessDamage")) {
            int lethal = c.getExcessDamageValue(sourceLKI.hasKeyword(Keyword.DEATHTOUCH));
            dmgToTarget = Math.min(lethal, dmg);
            excess = dmg - dmgToTarget;
        }

        if (sa.hasParam("ExcessDamage") && (!sa.hasParam("ExcessDamageCondition") ||
                sourceLKI.isValid(sa.getParam("ExcessDamageCondition").split(","), activationPlayer, hostCard, sa))) {
            damageMap.put(sourceLKI, c, dmgToTarget);

            List<GameEntity> list = AbilityUtils.getDefinedEntities(hostCard, sa.getParam("ExcessDamage"), sa);

            if (!list.isEmpty()) {
                damageMap.put(sourceLKI, list.get(0), excess);
            }

            if (sa.hasParam("RememberRedirectedExcess")) {
                hostCard.addRemembered(excess);
            }
        } else {
            damageMap.put(sourceLKI, c, dmg);
        }
    }
}
