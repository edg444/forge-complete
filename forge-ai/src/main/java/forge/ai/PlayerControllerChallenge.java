package forge.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.combat.AttackRequirement;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.herospath.HerosPath;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

/**
 * A Hero's Path challenge deck (the Hydra, the Horde, Xenagos). It never chooses to do anything: it casts what its rules
 * reveal, attacks only with creatures that have to, and never blocks unless the heroes say so. "You make any choices for
 * the Hydra": the hero who makes its choices is asked (HerosPath.getChooser); with no human hero, the choices are made
 * in the heroes' favor.
 */
public class PlayerControllerChallenge extends PlayerControllerAi {

    public PlayerControllerChallenge(final Game game, final Player p, final LobbyPlayer lp) {
        super(game, p, lp);
    }

    /** The human hero's controller, who's asked to make this deck's choices; null when every hero is the AI. */
    private PlayerController human() {
        final Player chooser = HerosPath.getChooser(getGame());
        return chooser == null || chooser.getController().isAI() ? null : chooser.getController();
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        return null;
    }

    @Override
    public boolean mulliganKeepHand(final Player firstPlayer, final int cardsToReturn) {
        return true;
    }

    @Override
    public List<PaperCard> sideboard(final Deck deck, final GameType gameType, final String message) {
        return null;
    }

    @Override
    public Map<DeckSection, List<? extends PaperCard>> complainCardsCantPlayWell(final Deck myDeck) {
        return null;
    }

    @Override
    public Player chooseStartingPlayer(final boolean isFirstGame) {
        final PlayerController human = human();
        if (human != null) {
            return human.chooseStartingPlayer(isFirstGame);
        }
        final Player chooser = HerosPath.getChooser(getGame());
        return chooser == null ? player : chooser;
    }

    /** Only the creatures that have to attack do, each at whatever the heroes choose. */
    @Override
    public void declareAttackers(final Player attacker, final Combat combat) {
        final Map<Card, GameEntity> legal = combat.getAttackConstraints().getLegalAttackers().getLeft();
        final Map<Card, AttackRequirement> reqs = combat.getAttackConstraints().getRequirements();
        final PlayerController human = human();
        for (final Map.Entry<Card, GameEntity> e : legal.entrySet()) {
            final Card c = e.getKey();
            final AttackRequirement req = reqs.get(c);
            if (!c.getController().equals(player) || req == null || !req.hasRequirement()) {
                continue;
            }
            final FCollection<GameEntity> options = new FCollection<>();
            for (final GameEntity d : combat.getDefenders()) {
                if (!(d instanceof Card dc && dc.getController().equals(player)) && CombatUtil.canAttack(c, d)) {
                    options.add(d);
                }
            }
            GameEntity defender = e.getValue();
            if (options.size() > 1) {
                if (human != null) {
                    defender = human.chooseSingleEntityForEffect(options, null, new SpellAbility.EmptySa(c, player),
                            "Choose what " + c.getTranslatedName() + " attacks", false, null, null);
                } else {
                    defender = heroToAttack(options);
                }
            }
            if (defender != null && !combat.isAttacking(c)) {
                combat.addAttacker(c, defender);
            }
        }
        if (!CombatUtil.validateAttackers(combat)) {
            // fall back on the attack the rules require, as declared by the requirement solver
            for (final Card own : CardLists.filterControlledBy(combat.getAttackers(), player)) {
                combat.removeFromCombat(own);
            }
            for (final Map.Entry<Card, GameEntity> e : legal.entrySet()) {
                if (e.getKey().getController().equals(player)) {
                    combat.addAttacker(e.getKey(), e.getValue());
                }
            }
        }
    }

    /** In the heroes' favor: the hero best able to block (most untapped creatures), never a planeswalker. */
    private GameEntity heroToAttack(final FCollectionView<GameEntity> options) {
        GameEntity best = null;
        int bestBlockers = -1;
        for (final GameEntity d : options) {
            if (d instanceof Player p) {
                final int blockers = CardLists.filter(p.getCreaturesInPlay(), Card::isUntapped).size();
                if (blockers > bestBlockers) {
                    best = d;
                    bestBlockers = blockers;
                }
            }
        }
        return best != null ? best : options.get(0);
    }

    @Override
    public void declareBlockers(final Player defender, final Combat combat) {
        final PlayerController human = human();
        if (human != null) {
            human.declareBlockers(defender, combat);
        }
        // otherwise it doesn't block: blocking for it is the heroes' choice, and it never helps them
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(final Card attacker, final CardCollectionView blockers,
            final CardCollectionView remaining, final int damageDealt, final GameEntity defender, final boolean overrideOrder) {
        final PlayerController human = human();
        if (human != null) {
            return human.assignCombatDamage(attacker, blockers, remaining, damageDealt, defender, overrideOrder);
        }
        return super.assignCombatDamage(attacker, blockers, remaining, damageDealt, defender, overrideOrder);
    }

    /** "The Hydra casts that card": the heroes choose its targets; it pays nothing. */
    @Override
    public boolean playSaFromPlayEffect(final SpellAbility tgtSA) {
        if (!chooseTargetsFor(tgtSA)) {
            return false;
        }
        return ComputerUtil.playStack(tgtSA, player, getGame());
    }

    @Override
    public boolean chooseTargetsFor(final SpellAbility sa) {
        final PlayerController human = human();
        for (SpellAbility step = sa; step != null; step = step.getSubAbility()) {
            if (!step.usesTargeting()) {
                continue;
            }
            step.resetTargets();
            if (human != null) {
                if (!human.chooseTargetsFor(step)) {
                    return false;
                }
                continue;
            }
            final TargetRestrictions tr = step.getTargetRestrictions();
            final List<GameEntity> candidates = new ArrayList<>();
            for (final GameEntity o : tr.getAllCandidates(step)) {
                candidates.add(o);
            }
            final int max = Math.min(candidates.size(), tr.getMaxTargets(step.getHostCard(), step));
            sortForHeroes(candidates, step);
            for (int i = 0; i < max; i++) {
                step.getTargets().add(candidates.get(i));
            }
            if (step.getTargets().size() < tr.getMinTargets(step.getHostCard(), step)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(final FCollectionView<T> optionList,
            final DelayedReveal delayedReveal, final SpellAbility sa, final String title, final boolean isOptional,
            final Player relatedPlayer, final Map<String, Object> params) {
        final PlayerController human = human();
        if (human != null) {
            return human.chooseSingleEntityForEffect(optionList, delayedReveal, sa, title, isOptional, relatedPlayer, params);
        }
        if (optionList.isEmpty()) {
            return null;
        }
        final List<T> sorted = new ArrayList<>(optionList);
        sortForHeroes(sorted, sa);
        return sorted.get(0);
    }

    @Override
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(final FCollectionView<T> optionList, final int min,
            final int max, final DelayedReveal delayedReveal, final SpellAbility sa, final String title,
            final Player relatedPlayer, final Map<String, Object> params) {
        final PlayerController human = human();
        if (human != null) {
            return human.chooseEntitiesForEffect(optionList, min, max, delayedReveal, sa, title, relatedPlayer, params);
        }
        final List<T> sorted = new ArrayList<>(optionList);
        sortForHeroes(sorted, sa);
        return new ArrayList<>(sorted.subList(0, Math.min(max, sorted.size())));
    }

    @Override
    public CardCollectionView chooseCardsForEffect(final CardCollectionView sourceList, final SpellAbility sa,
            final String title, final int min, final int max, final boolean isOptional, final Map<String, Object> params) {
        final PlayerController human = human();
        if (human != null) {
            return human.chooseCardsForEffect(sourceList, sa, title, min, max, isOptional, params);
        }
        final List<Card> sorted = new ArrayList<>(new CardCollection(sourceList));
        sortForHeroes(sorted, sa);
        return new CardCollection(sorted.subList(0, Math.min(max, sorted.size())));
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(final SpellAbility sa, final int min, final int max,
            final CardCollectionView validTargets, final String message) {
        final PlayerController human = human();
        if (human != null) {
            return human.choosePermanentsToSacrifice(sa, min, max, validTargets, message);
        }
        final List<Card> sorted = new ArrayList<>(new CardCollection(validTargets));
        sortForHeroes(sorted, sa);
        return new CardCollection(sorted.subList(0, Math.min(Math.max(min, Math.min(max, sorted.size())), sorted.size())));
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(final SpellAbility sa, final int min, final int max,
            final CardCollectionView validTargets, final String message) {
        final PlayerController human = human();
        if (human != null) {
            return human.choosePermanentsToDestroy(sa, min, max, validTargets, message);
        }
        return choosePermanentsToSacrifice(sa, min, max, validTargets, message);
    }

    @Override
    public List<AbilitySub> chooseModeForAbility(final SpellAbility sa, final List<AbilitySub> possible, final int min,
            final int num, final boolean allowRepeat) {
        final PlayerController human = human();
        if (human != null) {
            return human.chooseModeForAbility(sa, possible, min, num, allowRepeat);
        }
        // in the heroes' favor: a mode that destroys none of their permanents, else damage while they can take it
        final List<AbilitySub> ordered = new ArrayList<>(possible);
        ordered.sort(Comparator.comparingInt(this::heroHarm));
        return new ArrayList<>(ordered.subList(0, Math.min(num, ordered.size())));
    }

    /** Rough cost to the heroes of a mode: each permanent of theirs it destroys counts 4, damage to each counts 1 a point. */
    private int heroHarm(final AbilitySub mode) {
        final Card host = mode.getHostCard();
        if (mode.getApi() == ApiType.DestroyAll && mode.hasParam("ValidCards")) {
            int count = 0;
            for (final Player hero : HerosPath.getHeroes(getGame())) {
                count += CardLists.getValidCards(hero.getCardsIn(ZoneType.Battlefield), mode.getParam("ValidCards"),
                        player, host, mode).size();
            }
            return 4 * count;
        }
        if ((mode.getApi() == ApiType.DamageAll || mode.getApi() == ApiType.DealDamage) && mode.hasParam("NumDmg")) {
            final int dmg = AbilityUtils.calculateAmount(host, mode.getParam("NumDmg"), mode);
            int harm = 0;
            for (final Player hero : HerosPath.getHeroes(getGame())) {
                // damage that would leave a hero at 4 life or less is as bad as it gets
                harm += hero.getLife() - dmg <= 4 ? 1000 : dmg;
            }
            return harm;
        }
        return 0;
    }

    @Override
    public int chooseNumber(final SpellAbility sa, final String title, final int min, final int max) {
        final PlayerController human = human();
        return human != null ? human.chooseNumber(sa, title, min, max) : min;
    }

    @Override
    public boolean confirmAction(final SpellAbility sa, final PlayerActionConfirmMode mode, final String message,
            final List<String> options, final Card cardToShow, final Map<String, Object> params) {
        final PlayerController human = human();
        if (human != null && sa != null && !sa.isTrigger()) {
            return human.confirmAction(sa, mode, message, options, cardToShow, params);
        }
        return super.confirmAction(sa, mode, message, options, cardToShow, params);
    }

    /**
     * In the heroes' favor, best first: of the deck's own cards, the most valuable (it loses them; with
     * AILogic$ ChallengeWeakest, the one closest to dying, since damage is coming to it), of anyone else's the least
     * valuable; of players, the one with the most life.
     */
    private <T> void sortForHeroes(final List<T> options, final SpellAbility sa) {
        final boolean weakest = sa != null && "ChallengeWeakest".equals(sa.getParam("AILogic"));
        options.sort((a, b) -> Integer.compare(heroScore(b, weakest), heroScore(a, weakest)));
    }

    private int heroScore(final Object o, final boolean weakest) {
        if (o instanceof Card c) {
            final int value = c.isCreature() ? ComputerUtilCard.evaluateCreature(c) : ComputerUtilCard.evaluatePermanentList(new CardCollection(c));
            if (c.getController().equals(player)) {
                return weakest && c.isCreature() ? 100000 - 1000 * c.getNetToughness() + 1000 * c.getDamage() + value / 100 : 50000 + value;
            }
            return -value;
        }
        if (o instanceof Player p) {
            return p.equals(player) ? -100000 : p.getLife();
        }
        return 0;
    }
}
