package forge.ai.ability;

import forge.ai.*;
import forge.StaticData;
import forge.card.CardStateName;
import forge.card.ICardFace;
import forge.card.CardTypeView;
import forge.game.Game;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
import forge.game.spellability.SpellPermanent;
import forge.game.staticability.StaticAbilityCastAs;
import forge.game.zone.ZoneType;
import forge.util.IterableUtil;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PlayAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final String logic = sa.getParamOrDefault("AILogic", "");

        final Game game = ai.getGame();
        final Card source = sa.getHostCard();
        // don't use this as a response (ReplaySpell logic is an exception, might be called from a subability
        // while the trigger is on stack)
        if (!game.getStack().isEmpty() && !"ReplaySpell".equals(logic)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (game.getRules().hasAppliedVariant(GameType.MoJhoSto) && source.getName().equals("Jhoira of the Ghitu Avatar")) {
            // Additional logic for MoJhoSto:
            // Do not activate Jhoira too early, usually there are few good targets
            AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
            int numLandsForJhoira = aic.getIntProperty(AiProps.MOJHOSTO_NUM_LANDS_TO_ACTIVATE_JHOIRA);
            int chanceToActivateInst = 100 - aic.getIntProperty(AiProps.MOJHOSTO_CHANCE_TO_USE_JHOIRA_COPY_INSTANT);
            if (ai.getLandsInPlay().size() < numLandsForJhoira) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            // Don't spam activate the Instant copying ability all the time to give the AI a chance to use other abilities
            // Can probably be improved, but as random as MoJhoSto already is, probably not a huge deal for now
            if ("Instant".equals(sa.getParam("AnySupportedCard")) && MyRandom.percentTrue(chanceToActivateInst)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        List<Card> cards = getPlayableCards(sa, ai);
        if (cards.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
        }
        // Isochron Scepter: a free copy is only worth making when the AI would cast it now
        if (sa.hasParam("CopyCard") && sa.hasParam("WithoutManaCost") && !sa.isTrigger()
                && cards.stream().noneMatch(c -> GraveCastAi.castValue(ai, c) > 0)) {
            return new AiAbilityDecision(0, AiPlayDecision.AnotherTime);
        }

        if ("ReplaySpell".equals(logic)) {
            if (ComputerUtil.targetPlayableSpellCard(ai, cards, sa, sa.hasParam("WithoutManaCost"), false)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        } else if (logic.startsWith("NeedsChosenCard")) {
            int minCMC = 0;
            if (sa.getPayCosts().getCostMana() != null) {
                minCMC = sa.getPayCosts().getTotalMana().getCMC();
            }
            cards = CardLists.filter(cards, CardPredicates.greaterCMC(minCMC));
            if (chooseSingleCard(ai, sa, cards, sa.hasParam("Optional"), null, null) != null) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
        } else if ("WithTotalCMC".equals(logic)) {
            // Try to play only when there are more than three playable cards.
            if (cards.size() < 3)
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            if (sa.costHasManaX()) {
                int amount = ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());
                if (amount < ComputerUtilCard.getBestAI(cards).getCMC())
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                int totalCMC = 0;
                for (Card c : cards) {
                    totalCMC += c.getCMC();
                }
                if (amount > totalCMC)
                    amount = totalCMC;
                sa.setXManaCostPaid(amount);
            }
        }

        if (source != null && source.hasKeyword(Keyword.HIDEAWAY) && source.hasExiledCard()) {
            // AI is not very good at playing non-permanent spells this way, at least yet
            // (might be possible to enable it for Sorceries in Main1/Main2 if target is available,
            // but definitely not for most Instants)
            Card rem = source.getExiledCards().getFirst();
            CardTypeView t = rem.getState(CardStateName.Original).getType();

            if (t.isPermanent() && !t.isLand()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /**
     * <p>
     * doTriggerAINoCost
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     *
     * @return a boolean.
     */
    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        if (sa.usesTargeting()) {
            if (!sa.hasParam("AILogic")) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            if ("ReplaySpell".equals(sa.getParam("AILogic"))) {
                boolean result = ComputerUtil.targetPlayableSpellCard(ai, getPlayableCards(sa, ai), sa, sa.hasParam("WithoutManaCost"), mandatory);
                return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            return checkApiLogic(ai, sa);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(Player ai, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(final Player ai, final SpellAbility sa, Iterable<Card> options,
            final boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        final CardStateName state;
        if (sa.hasParam("CastTransformed")) {
            state = CardStateName.Backside;
            options.forEach(c -> c.changeToState(CardStateName.Backside));
        } else {
            state = CardStateName.Original; 
        }

        Predicate<SpellAbility> validSA;
        if (sa.hasParam("ValidSA")) {
            validSA = SpellAbilityPredicates.isValid(sa.getParam("ValidSA").split(","), ai, sa.getHostCard(), sa);
        } else {
            validSA = null;
        }
        List<Card> tgtCards = CardLists.filter(options, c -> {
            // TODO needs to be aligned for MDFC along with getAbilityToPlay so the knowledge
            // of which spell was the reason for the choice can be used there
            for (SpellAbility s : AbilityUtils.getSpellsFromPlayEffect(c, ai, state, false, validSA, sa.hasParam("CastFromBattlefield"))) {
                if (s.isLandAbility()) {
                    // might want to run some checks here but it's rare anyway
                    return true;
                }
                Spell spell = (Spell) s;
                if (params != null && params.containsKey("CMCLimit")) {
                    Integer cmcLimit = (Integer) params.get("CMCLimit");
                    if (spell.getPayCosts().getTotalMana().getCMC() > cmcLimit)
                        continue;
                }
                if (sa.hasParam("WithoutManaCost")) {
                    // Try to avoid casting instants and sorceries with X in their cost, since X will be assumed to be 0.
                    if (!(spell instanceof SpellPermanent)) {
                        if (spell.costHasManaX()) {
                            continue;
                        }
                    }

                    spell = (Spell) spell.copyWithNoManaCost();
                } else if (sa.hasParam("PlayCost")) {
                    Cost abCost;
                    if ("ManaCost".equals(sa.getParam("PlayCost"))) {
                        abCost = new Cost(c.getManaCost(), false);
                    } else {
                        abCost = new Cost(sa.getParam("PlayCost"), false);
                    }

                    spell = (Spell) spell.copyWithManaCostReplaced(spell.getActivatingPlayer(), abCost);
                }
                if (sa.isKeyword(Keyword.MADNESS) && !spell.usesTargeting()) {
                    // The card is already in exile and goes to the graveyard if it is not cast: the usual "is this a
                    // good moment" heuristics have nothing left to decide, only whether the madness cost can be paid
                    return ComputerUtilCost.canPayCost(spell, ai, true);
                }
                if (AiPlayDecision.WillPlay == ((PlayerControllerAi)ai.getController()).getAi().canPlayFromEffectAI(spell, !(isOptional || sa.hasParam("Optional")), true)) {
                    // Before accepting, see if the spell has a valid number of targets (it should at this point).
                    // Proceeding past this point if the spell is not correctly targeted will result
                    // in "Failed to add to stack" error and the card disappearing from the game completely.
                    if (!spell.isTargetNumberValid() || !ComputerUtilCost.canPayCost(spell, ai, true)) {
                        // if we won't be able to pay the cost, don't choose the card
                        return false;
                    }
                    return true;
                }
            }
            return false;
        });

        if (sa.hasParam("CastTransformed")) {
            options.forEach(c -> c.changeToState(CardStateName.Original));
        }

        final Card best = ComputerUtilCard.getBestAI(tgtCards);
        if (sa.usesTargeting() && !sa.isTargetNumberValid()) {
            sa.getTargets().add(best);
        }
        return best;
    }

    private static List<Card> getPlayableCards(SpellAbility sa, Player ai) {
        List<Card> cards;
        final Card source = sa.getHostCard();

        // ValidZone comes first: it defines the pool, and every later filter has to narrow that
        // same pool. Applying them in the other order let the pool be rebuilt afterward, throwing
        // away the ValidSA filter entirely - the AI would cast "cast an instant or sorcery from
        // your graveyard" effects while owning no instants or sorceries at all.
        if (sa.hasParam("ValidZone")) {
            cards = new CardCollection(AbilityUtils.filterListByType(
                    ai.getGame().getCardsIn(ZoneType.listValueOf(sa.getParam("ValidZone"))),
                    sa.getParamOrDefault("Valid", "Card"), sa));
        } else if (sa.usesTargeting()) {
            cards = CardUtil.getValidCardsToTarget(sa);
        } else if (!sa.hasParam("Valid")) {
            cards = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
        } else {
            cards = new CardCollection();
        }

        if (sa.hasParam("ValidSA")) {
            final String valid[] = sa.getParam("ValidSA").split(",");
            final List<Card> invalid = cards.stream().filter(c -> !IterableUtil.any(AbilityUtils.getBasicSpellsFromPlayEffect(c, ai), SpellAbilityPredicates.isValid(valid, ai, source, sa))).collect(Collectors.toList());
            if (!invalid.isEmpty())
                cards.removeAll(invalid);
        }

        // A card costing more than the whole allowance can never be one of the spells cast, so it
        // must not be what makes the effect look worth casting.
        if (sa.hasParam("WithTotalCMC")) {
            final int budget = AbilityUtils.calculateAmount(source, sa.getParam("WithTotalCMC"), sa);
            if (budget >= 0) {
                cards = cards.stream().filter(c -> c.getCMC() <= budget).collect(Collectors.toList());
            }
        }

        // exclude own card
        cards.remove(source);
        return cards;
    }


    /**
     * Imaginary Friends (Plane)'s free cast "only as another card": no real strategy - try the choices in random order
     * and take the first whose spell the AI would cast (it has targets, isn't pointless right now).
     */
    @Override
    public String chooseCardName(final Player ai, final SpellAbility sa, final List<ICardFace> faces) {
        if (!sa.hasParam("CastAsAnother")) {
            return super.chooseCardName(ai, sa, faces);
        }
        final List<Card> defined = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
        final Card host = defined.isEmpty() ? null : defined.get(0);
        final List<ICardFace> order = new ArrayList<>(faces);
        Collections.shuffle(order, MyRandom.getRandom());
        String fallback = "";
        int tried = 0;
        for (final ICardFace face : order) {
            final String name = face.getName();
            final forge.item.PaperCard pc = StaticData.instance().getCommonCards().getUniqueByName(name);
            if (pc == null || pc.getRules().getAiHints().getRemAIDecks()) {
                continue;
            }
            if (fallback.isEmpty()) {
                fallback = name;
            }
            // building and judging a spell isn't free, and one good enough choice is all it needs
            if (host == null || ++tried > 30) {
                break;
            }
            for (final SpellAbility spell : StaticAbilityCastAs.template(ai.getGame(), name, ai).getSpellAbilities()) {
                if (!spell.isSpell()) {
                    continue;
                }
                final SpellAbility copy = spell.copy(host, ai, false);
                final boolean willing = SpellApiToAi.Converter.get(copy).canPlayWithSubs(ai, copy).willingToPlay();
                copy.resetTargets();
                if (willing) {
                    return name;
                }
            }
        }
        return fallback;
    }
}
