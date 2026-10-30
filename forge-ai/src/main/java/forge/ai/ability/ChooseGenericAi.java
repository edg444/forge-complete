package forge.ai.ability;

import com.google.common.collect.Lists;
import forge.ai.*;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;
import forge.util.collect.FCollection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChooseGenericAi extends SpellAbilityAi {

    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if ("Khans".equals(aiLogic) || "Dragons".equals(aiLogic)) {
            return true;
        } else if ("Pump".equals(aiLogic) || "BestOption".equals(aiLogic)) {
            for (AbilitySub sb : sa.getAdditionalAbilityList("Choices")) {
                if (SpellApiToAi.Converter.get(sb).canPlayWithSubs(ai, sb).willingToPlay()) {
                    return true;
                }
            }
        } else if ("Always".equals(aiLogic)) {
            return true;
        } else if ("FlavorJudge".equals(aiLogic)) {
            return chooseFlavorJudgeTarget(ai, sa);
        } else if ("BestPT".equals(aiLogic)) {
            return chooseBestPT(ai, sa.getAdditionalAbilityList("Choices")) != null;
        } else if ("ChangeTextKill".equals(aiLogic)) {
            // Mind Bend, Crystal Spray: the target is the parent's, the land type swap is the choice
            return ChangeTextAi.targetKill(ai, sa);
        }
        return false;
    }

    /**
     * Master of Winds: which "base power and toughness become X/Y until end of turn" choice to take, or null to
     * leave the creature as it is. Big power for an attack nothing can safely block, the form that wins a fight
     * already set up, else the toughest form. Plain arithmetic (first strike and deathtouch only), not a simulation.
     */
    private static SpellAbility chooseBestPT(final Player ai, final List<? extends SpellAbility> choices) {
        final List<SpellAbility> options = Lists.newArrayList();
        for (SpellAbility sp : choices) {
            if (sp.hasParam("Power") && sp.hasParam("Toughness")) {
                options.add(sp);
            }
        }
        if (options.isEmpty()) {
            return null;
        }
        final Card host = options.get(0).getHostCard();
        if (!host.isCreature() || !host.isInPlay() || !host.getController().equals(ai)) {
            return null;
        }
        SpellAbility mostPower = options.get(0);
        SpellAbility mostToughness = options.get(0);
        for (SpellAbility sp : options) {
            if (ptPower(host, sp) > ptPower(host, mostPower)) {
                mostPower = sp;
            }
            if (ptToughness(host, sp) > ptToughness(host, mostToughness)) {
                mostToughness = sp;
            }
        }
        final SpellAbility tougher = ptToughness(host, mostToughness) > host.getNetToughness() ? mostToughness : null;

        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final PhaseType phase = game.getPhaseHandler().getPhase();
        final boolean inCombat = combat != null && (combat.isAttacking(host) || combat.isBlocking(host));
        if (inCombat && phase.isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS) && phase.isBefore(PhaseType.COMBAT_END)) {
            final CardCollection foes = combat.isAttacking(host) ? combat.getBlockers(host) : combat.getAttackersBlockedBy(host);
            if (foes.isEmpty()) {
                return ptPower(host, mostPower) > host.getNetPower() ? mostPower : null;
            }
            SpellAbility best = null;
            int bestScore = fightScore(host, host.getNetPower(), host.getNetToughness(), foes);
            for (SpellAbility sp : options) {
                final int score = fightScore(host, ptPower(host, sp), ptToughness(host, sp), foes);
                if (score > bestScore) {
                    best = sp;
                    bestScore = score;
                }
            }
            return best;
        }

        final boolean mayAttack = inCombat && combat.isAttacking(host)
                || game.getPhaseHandler().isPlayerTurn(ai) && phase.isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && CombatUtil.canAttack(host);
        if (mayAttack && ptPower(host, mostPower) > host.getNetPower()) {
            final int toughness = ptToughness(host, mostPower) - host.getDamage();
            boolean safe = true;
            for (Card blocker : ai.getOpponents().getCreaturesInPlay()) {
                if (CombatUtil.canBlock(host, blocker) && host.canBeDestroyed() && (blocker.getNetPower() >= toughness
                        || blocker.getNetPower() > 0 && blocker.hasKeyword(Keyword.DEATHTOUCH))) {
                    safe = false;
                    break;
                }
            }
            if (safe) {
                return mostPower;
            }
        }
        return tougher;
    }

    private static int ptPower(final Card host, final SpellAbility sp) {
        return AbilityUtils.calculateAmount(host, sp.getParam("Power"), sp) + host.getNetPower() - host.getBasePower();
    }

    private static int ptToughness(final Card host, final SpellAbility sp) {
        return AbilityUtils.calculateAmount(host, sp.getParam("Toughness"), sp) + host.getNetToughness() - host.getBaseToughness();
    }

    // value of the foes it kills, plus its own value if it lives
    private static int fightScore(final Card host, final int power, final int toughness, final CardCollection foes) {
        final boolean hostFirst = host.hasFirstStrike() || host.hasDoubleStrike();
        final boolean hostDeathtouch = host.hasKeyword(Keyword.DEATHTOUCH);
        int incoming = 0;
        boolean deadlyFoe = false;
        boolean foeFirst = false;
        for (Card foe : foes) {
            incoming += Math.max(0, foe.getNetPower());
            deadlyFoe |= foe.getNetPower() > 0 && foe.hasKeyword(Keyword.DEATHTOUCH);
            foeFirst |= foe.hasFirstStrike() || foe.hasDoubleStrike();
        }
        final boolean survives = toughness > 0
                && (!host.canBeDestroyed() || incoming < toughness - host.getDamage() && !deadlyFoe);
        int score = survives ? ComputerUtilCard.evaluateCreature(host) : 0;
        if (!survives && foeFirst && !hostFirst) {
            return score;
        }
        final List<Card> byToughness = Lists.newArrayList(foes);
        byToughness.sort((a, b) -> Integer.compare(a.getNetToughness() - a.getDamage(), b.getNetToughness() - b.getDamage()));
        int remaining = power;
        for (Card foe : byToughness) {
            final int need = hostDeathtouch ? 1 : Math.max(1, foe.getNetToughness() - foe.getDamage());
            if (remaining < need) {
                break;
            }
            remaining -= need;
            if (foe.canBeDestroyed()) {
                score += ComputerUtilCard.evaluateCreature(foe);
            }
        }
        return score;
    }

    private static boolean chooseFlavorJudgeTarget(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        if (game.getStack().isEmpty()
                || forge.game.staticability.StaticAbilityStackFirstInFirstOut.applies(game)) {
            return false;
        }
        final SpellAbility topSA = ComputerUtilAbility.getTopSpellAbilityOnStack(game, sa);
        if (topSA == null || ai.getYourTeam().contains(topSA.getActivatingPlayer())
                || (topSA.isSpell() && !topSA.isCounterableBy(sa))) {
            return false;
        }
        sa.resetTargets();
        if (!sa.canTargetSpellAbility(topSA)) {
            return false;
        }
        sa.getTargets().add(topSA);
        return true;
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (sa.hasParam("AILogic")) {
            // This is equivalent to what was here before but feels bad
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        AiAbilityDecision decision;
        if (sa.isTrigger()) {
            decision = doTriggerNoCost(aiPlayer, sa, sa.isMandatory());
        } else {
            decision = checkApiLogic(aiPlayer, sa);
        }

        return decision;
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player aiPlayer, final SpellAbility sa, final boolean mandatory) {
        if ("CombustibleGearhulk".equals(sa.getParam("AILogic")) || "SoulEcho".equals(sa.getParam("AILogic"))) {
            for (final Player p : aiPlayer.getOpponents()) {
                if (p.canBeTargetedBy(sa)) {
                    sa.resetTargets();
                    sa.getTargets().add(p);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay); // perhaps the opponent(s) had Sigarda, Heron's Grace or another effect giving hexproof in play, still play the creature as 6/6
        }
        if ("StealPhase".equals(sa.getParam("AILogic")) && sa.usesTargeting()) {
            // Clocknapper: rob the weakest opponent, whose next turn is where the stolen phase happens
            sa.resetTargets();
            final List<Player> opps = Lists.newArrayList(aiPlayer.getOpponents());
            opps.sort((a, b) -> Integer.compare(a.getLife(), b.getLife()));
            for (final Player p : opps) {
                if (p.canBeTargetedBy(sa)) {
                    sa.getTargets().add(p);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            if (mandatory && aiPlayer.canBeTargetedBy(sa)) {
                sa.getTargets().add(aiPlayer); // stealing from yourself changes nothing
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if ("AskThenTapOrUntap".equals(sa.getParam("AILogic"))) {
            // Focused Funambulist: aim at the opposing creature most worth tapping, else one of its own to untap, and
            // let the simulated person answer (chooseSingleSpellAbility)
            sa.resetTargets();
            final List<Card> theirs = CardLists.filter(CardLists.getTargetableCards(
                    aiPlayer.getOpponents().getCreaturesInPlay(), sa), c -> c.isUntapped());
            final List<Card> mine = CardLists.filter(CardLists.getTargetableCards(aiPlayer.getCreaturesInPlay(), sa),
                    c -> c.isTapped());
            final Card target = !theirs.isEmpty() ? ComputerUtilCard.getBestCreatureAI(theirs)
                    : !mine.isEmpty() ? ComputerUtilCard.getBestCreatureAI(mine) : null;
            if (target != null) {
                sa.getTargets().add(target);
            }
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        if (ComputerUtilAbility.getAbilitySourceName(sa).equals("Deathmist Raptor")) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return super.doTriggerNoCost(aiPlayer, sa, mandatory);
    }

    @Override
    public SpellAbility chooseSingleSpellAbility(Player player, SpellAbility sa, List<SpellAbility> spells, Map<String, Object> params) {
        Card host = sa.getHostCard();
        final Game game = host.getGame();
        final String logic = sa.getParam("AILogic");
        if (logic == null) {
            return spells.get(0);
        } else if ("ChangeTextKill".equals(logic)) {
            for (final SpellAbility sp : spells) {
                if (sp.hasParam("ChangeTypeWord")) {
                    return sp;
                }
            }
            return spells.get(0);
        } else if ("DamnationChoice".equals(logic)) {
            return MiscCombatAi.damnationChoice(sa, spells);
        } else if (logic.startsWith("GameChance.")) {
            // Honor-system fact about the AI's own situation for the whole game (e.g. wearing
            // glasses): spells.get(0) is the "no" answer, spells.get(1) the "yes"
            if (spells.size() > 1 && rollGameChanceLogic(player, sa, logic)) {
                return spells.get(1);
            }
            return spells.get(0);
        } else if (logic.startsWith("Chance.")) {
            // Honor-system choices (see the Sex Appeal/Bureaucracy/Common Courtesy/Sorry family of
            // cards) where spells.get(0) is the safe/default answer - Chance.N is the percent chance
            // of picking something else instead, uniformly among the other listed choices, so the
            // AI isn't permanently locked into the same answer every time.
            if (spells.size() > 1 && MyRandom.percentTrue(Integer.parseInt(logic.substring("Chance.".length())))) {
                return Aggregates.random(spells.subList(1, spells.size()));
            }
            return spells.get(0);
        } else if ("AskThenTapOrUntap".equals(logic)) {
            // Focused Funambulist: a simulated person thinks it looks balanced half the time; spells.get(1) is "yes"
            return spells.size() > 1 && MyRandom.percentTrue(50) ? spells.get(1) : spells.get(0);
        } else if ("FlavorJudge".equals(logic)) {
            // the AI has no real person to ask, so a simulated one finds the story nonsense half the time;
            // spells.get(1) is the "no" answer
            return spells.size() > 1 && MyRandom.percentTrue(50) ? spells.get(1) : spells.get(0);
        } else if ("StealPhase".equals(logic)) {
            return chooseStolenPhase(player, sa, spells);
        } else if ("HinderAttacker".equals(logic)) {
            // Dumb Ass: the creature's own controller doesn't decide whether it attacks - an
            // opponent does, and wants whichever answer hurts that controller more. Forcing the
            // attack is only good when the AI can eat the creature for free; otherwise the attack
            // is simply denied. Choice order is fixed by the card: [0] attacks, [1] doesn't.
            if (spells.size() < 2) {
                return spells.get(0);
            }
            for (final Card blocker : player.getCreaturesInPlay()) {
                if (!CombatUtil.canBlock(host, blocker)) {
                    continue;
                }
                if (ComputerUtilCombat.canDestroyAttacker(player, host, blocker, null, false)
                        && !ComputerUtilCombat.canDestroyBlocker(player, blocker, host, null, false)) {
                    return spells.get(0); // walk it into a blocker that kills it for nothing
                }
            }
            return spells.get(1);
        } else if ("MostMatchingCreatures".equals(logic)) {
            // Ineffable Blessing: each choice names the creatures it rewards (AIMatch$), so pick the one most of
            // the creature cards still to come - library and hand - would satisfy
            final CardCollectionView toCome = CardLists.filter(player.getCardsIn(ZoneType.Library, ZoneType.Hand),
                    CardPredicates.CREATURES);
            SpellAbility best = spells.get(0);
            int bestCount = -1;
            for (final SpellAbility choice : spells) {
                final int count = choice.hasParam("AIMatch")
                        ? CardLists.getValidCardCount(toCome, choice.getParam("AIMatch"), player, host, sa) : 0;
                if (count > bestCount) {
                    best = choice;
                    bestCount = count;
                }
            }
            return best;
        } else if ("Random".equals(logic)) {
            return Aggregates.random(spells);
        } else if ("PleasantTaxer".equals(logic)) {
            // tax everyone's casting of whichever kind of spell the AI's own deck has fewer of
            final int creatures = CardLists.count(player.getAllCards(), c -> !c.isToken() && c.isCreature());
            final int others = CardLists.count(player.getAllCards(), c -> !c.isToken() && !c.isCreature() && !c.isLand());
            final String want = creatures > others ? "Casting noncreature spells" : "Casting creature spells";
            for (final SpellAbility each : spells) {
                if (want.equals(each.getDescription())) {
                    return each;
                }
            }
            return spells.get(0);
        } else if ("LandOrNonland".equals(logic)) {
            // Abundance, Abundant Harvest: a land while short of mana for the hand, otherwise a spell
            final int lands = CardLists.count(player.getCardsIn(ZoneType.Battlefield), Card::isLand);
            final CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
            final int landsInHand = CardLists.count(hand, Card::isLand);
            int topCmc = 0;
            for (final Card c : hand) {
                if (!c.isLand()) {
                    topCmc = Math.max(topCmc, c.getCMC());
                }
            }
            final CardCollectionView library = player.getCardsIn(ZoneType.Library);
            boolean wantLand = landsInHand == 0 && (lands < 4 || lands < topCmc);
            if (!library.anyMatch(Card::isLand)) {
                wantLand = false;
            } else if (!library.anyMatch(c -> !c.isLand())) {
                wantLand = true;
            }
            for (final SpellAbility each : spells) {
                if (wantLand != each.getParamOrDefault("Valid", "").contains("nonLand")) {
                    return each;
                }
            }
            return spells.get(0);
        } else if ("Philosopher".equals(logic)) {
            // The Salubrious Philosopher: lands until it has five, then removal while opponents have creatures
            final int lands = CardLists.count(player.getCardsIn(ZoneType.Battlefield), Card::isLand)
                    + CardLists.count(player.getCardsIn(ZoneType.Hand), Card::isLand);
            final boolean threats = player.getOpponents().getCreaturesInPlay().size() > 0;
            final String want = lands < 5 ? "Mana base" : threats ? "Removal" : "Win condition";
            for (final SpellAbility each : spells) {
                if (want.equals(each.getDescription())) {
                    return each;
                }
            }
            return spells.get(0);
        } else if ("Sketch".equals(logic)) {
            // D00-DL: the AI's fifteen-second drawing - each pick a feature more, until it stops
            return MyRandom.percentTrue(55) ? Aggregates.random(spells) : null;
        } else if ("Phasing".equals(logic)) { // Teferi's Realm : keep aggressive
            List<SpellAbility> filtered = spells.stream()
                    .filter(sp -> !sp.getDescription().contains("Creature") && !sp.getDescription().contains("Land"))
                    .collect(Collectors.toList());
            return Aggregates.random(filtered);
        } else if ("PayUnlessCost".equals(logic)) {
            for (final SpellAbility sp : spells) {
                String unlessCost = sp.getParam("UnlessCost");
                sp.setActivatingPlayer(sa.getActivatingPlayer());
                Cost unless = new Cost(unlessCost, false);
                if (SpellApiToAi.Converter.get(sp).willPayUnlessCost(player, sp, unless, false, new FCollection<>(player))
                        && ComputerUtilCost.canPayCost(unless, sp, player, true)) {
                    return sp;
                }
            }
            return spells.get(0);
        } else if ("Khans".equals(logic) || "Dragons".equals(logic)) { // Fate Reforged sieges
            for (final SpellAbility sp : spells) {
                if (sp.getDescription().equals(logic)) {
                    return sp;
                }
            }
        } else if ("SelfOthers".equals(logic)) {
            SpellAbility self = null, others = null;
            for (final SpellAbility sp : spells) {
                if (sp.getDescription().equals("Self")) {
                    self = sp;
                } else {
                    others = sp;
                }
            }
            String hostname = host.getName();
            if (hostname.equals("May Civilization Collapse")) {
                if (player.getLandsInPlay().isEmpty()) {
                    return self;
                }
            } else if (hostname.equals("Feed the Machine")) {
                if (player.getCreaturesInPlay().isEmpty()) {
                    return self;
                }
            } else if (hostname.equals("Surrender Your Thoughts")) {
                if (player.getCardsIn(ZoneType.Hand).isEmpty()) {
                    return self;
                }
            } else if (hostname.equals("The Fate of the Flammable")) {
                if (!player.canLoseLife()) {
                    return self;
                }
            }
            return others;
        } else if ("Counters".equals(logic)) {
            // TODO: this code will need generalization if this logic is used for cards other
            // than Elspeth Conquers Death with different choice parameters
            SpellAbility p1p1 = null, loyalty = null;
            for (final SpellAbility sp : spells) {
                if (("P1P1").equals(sp.getParam("CounterType"))) {
                    p1p1 = sp;
                } else {
                    loyalty = sp;
                }
            }
            if (sa.getParent().getTargetCard() != null && sa.getParent().getTargetCard().isPlaneswalker()) {
                return loyalty;
            } else {
                return p1p1;
            }
        } else if ("Fatespinner".equals(logic)) {
            SpellAbility skipDraw = null, /*skipMain = null,*/ skipCombat = null;
            for (final SpellAbility sp : spells) {
                if (sp.getDescription().equals("FatespinnerSkipDraw")) {
                    skipDraw = sp;
                } else if (sp.getDescription().equals("FatespinnerSkipMain")) {
                    //skipMain = sp;
                } else {
                    skipCombat = sp;
                }
            }
            // FatespinnerSkipDraw,FatespinnerSkipMain,FatespinnerSkipCombat
            if (game.getReplacementHandler().wouldPhaseBeSkipped(player, PhaseType.DRAW)) {
                return skipDraw;
            }
            if (game.getReplacementHandler().wouldPhaseBeSkipped(player, PhaseType.COMBAT_BEGIN)) {
                return skipCombat;
            }

            // TODO If combat is poor, Skip Combat
            // Todo if hand is empty or mostly empty, skip main phase
            // Todo if hand has gas, skip draw
            return Aggregates.random(spells);
        } else if ("SinProdder".equals(logic)) {
            SpellAbility allow = null, deny = null;
            for (final SpellAbility sp : spells) {
                if (sp.getDescription().equals("No")) {
                    allow = sp;
                } else {
                    deny = sp;
                }
            }

            Card imprinted = host.getImprintedCards().getFirst();
            int dmg = imprinted.getCMC();
            Player owner = imprinted.getOwner();

            //useless cards in hand
            if (imprinted.getName().equals("Bridge from Below") ||
                    imprinted.getName().equals("Haakon, Stromgald Scourge")) {
                return allow;
            }

            //bad cards when are thrown from the library to the graveyard, but Yixlid can prevent that
            if (!player.getGame().isCardInPlay("Yixlid Jailer") && (
                    imprinted.getName().equals("Gaea's Blessing") ||
                    imprinted.getName().equals("Narcomoeba"))) {
                return allow;
            }

            // milling against Tamiyo is pointless
            if (owner.isCardInCommand("Emblem — Tamiyo, the Moon Sage")) {
                return allow;
            }

            // milling a land against Gitrog result in card draw
            if (imprinted.isLand() && owner.isCardInPlay("The Gitrog Monster")) {
                // try to mill owner
                if (owner.getCardsIn(ZoneType.Library).size() < 5) {
                    return deny;
                }
                return allow;
            }

            // milling a creature against Sidisi result in more creatures
            if (imprinted.isCreature() && owner.isCardInPlay("Sidisi, Brood Tyrant")) {
                return allow;
            }

            //if Iona does prevent from casting, allow it to draw
            for (final Card io : player.getCardsIn(ZoneType.Battlefield, "Iona, Shield of Emeria")) {
                if (imprinted.getColor().hasAnyColor(MagicColor.fromName(io.getChosenColor()))) {
                    return allow;
                }
            }

            if (dmg == 0) {
                // If CMC = 0, mill it!
                return deny;
            } else if (dmg + 3 > player.getLife()) {
                // if low on life, do nothing.
                return allow;
            } else if (player.getLife() - dmg > 15) {
                // TODO Check "danger" level of card
                // If lots of life, and card might be dangerous? Mill it!
                return deny;
            }
            // if unsure, random?
            return Aggregates.random(spells);
        } else if ("CombustibleGearhulk".equals(logic)) {
            Player controller = sa.getActivatingPlayer();
            List<ZoneType> zones = ZoneType.listValueOf("Graveyard, Battlefield, Exile");
            int life = player.getLife();
            CardCollectionView revealedCards = controller.getCardsIn(zones);

            if (revealedCards.size() < 5) {
                // Not very many revealed cards, just guess based on lifetotal
                return life < 7 ? spells.get(0) : spells.get(1);
            }

            int totalCMC = 0;
            for (Card c : revealedCards) {
                totalCMC += c.getCMC();
            }

            int bestGuessDamage = totalCMC * 3 / revealedCards.size();
            return life <= bestGuessDamage ? spells.get(0) : spells.get(1);
        }  else if ("SoulEcho".equals(logic)) {
            return sa.getHostCard().getController().getLife() < 10 ? spells.get(0) : Aggregates.random(spells);
        } else if ("Pump".equals(logic) || "BestOption".equals(logic)) {
            List<SpellAbility> filtered = Lists.newArrayList();
            // filter first for the spells which can be done
            for (SpellAbility sp : spells) {
                if (SpellApiToAi.Converter.get(sp).canPlayWithSubs(player, sp).willingToPlay()) {
                    filtered.add(sp);
                }
            }

            // TODO find better way to check
            if (!filtered.isEmpty()) {
                return filtered.get(0);
            }
        } else if ("BestPT".equals(logic)) {
            final SpellAbility best = chooseBestPT(player, spells);
            if (best != null) {
                return best;
            }
            // made to pick (no "may"): the toughest form is the safe one
            SpellAbility toughest = spells.get(0);
            for (SpellAbility sp : spells) {
                if (sp.hasParam("Toughness") && toughest.hasParam("Toughness") && ptToughness(host, sp) > ptToughness(host, toughest)) {
                    toughest = sp;
                }
            }
            return toughest;
        } else if ("FoodOrTreasure".equals(logic)) {
            // Tireless Provisioner
            // TODO: implement a better way to check for possible benefits in each case. If made generic, replace
            // fixed spells.get(N) with a way to detect which SA creates which token
            return ComputerUtil.aiLifeInDanger(player, false, 0) ? spells.get(0) : spells.get(1);
        }
        return spells.get(0);   // return first choice if no logic found
    }
    /**
     * Clocknapper. The beginning phase is the default: the AI untaps and draws while the victim doesn't. Combat
     * wins only when what can attack in it looks lethal - nothing untaps before then, so that's creatures untapped
     * and unsick now that won't be tapped attacking this turn (the sickness flag only clears on the AI's own turn).
     */
    private static SpellAbility chooseStolenPhase(final Player ai, final SpellAbility sa, final List<SpellAbility> spells) {
        SpellAbility beginning = spells.get(0);
        SpellAbility combat = null;
        for (final SpellAbility sp : spells) {
            if ("Beginning".equals(sp.getParam("Phase"))) {
                beginning = sp;
            } else if ("Combat".equals(sp.getParam("Phase"))) {
                combat = sp;
            }
        }
        final Player victim = sa.getTargets().getFirstTargetedPlayer();
        if (combat == null || victim == null || !ai.isOpponentOf(victim)) {
            return beginning;
        }
        final Game game = ai.getGame();
        final boolean attacksStillToCome = game.getPhaseHandler().isPlayerTurn(ai)
                && game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS);
        int power = 0;
        for (final Card c : ai.getCreaturesInPlay()) {
            if (c.isTapped() || c.isSick() || (attacksStillToCome && !c.hasKeyword(Keyword.VIGILANCE))) {
                continue;
            }
            power += ComputerUtilCombat.damageIfUnblocked(c, victim, null, false);
        }
        return power >= victim.getLife() ? combat : beginning;
    }
}
