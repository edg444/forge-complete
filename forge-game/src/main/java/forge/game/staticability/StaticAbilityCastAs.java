package forge.game.staticability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import forge.StaticData;
import forge.card.CardSplitType;
import forge.card.CardType;
import forge.card.ICardFace;
import forge.deck.DeckFormat;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.MyRandom;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Form of the Mulldrifter's "You may cast creature cards from your hand as though they were the card Mulldrifter.":
 * {@code Mode$ CastAs | ValidCard$ <cards> | Activator$ <players> | AffectedZone$ <zone> | CastAs$ <card name>}. Each
 * matching card gets that card's spells (with its alternative costs, such as evoke), and a card cast with one takes on
 * that card's copiable values as it goes on the stack (see {@code Card.setSplitStateToPlayAbility}), keeping them
 * until it leaves the stack other than by resolving or leaves the battlefield - it's a new object either way.
 * <p>
 * Imaginary Friends (Plane)'s "as though they were other cards legal in this game's format with the same type and mana
 * cost" is {@code CastAs$ AnotherCard}: the card gets one stand-in spell, and which card it's cast as is asked when
 * that spell is chosen ({@link #chooseAnotherCard}), since the chosen card's own spell is the one that gets cast.
 */
public class StaticAbilityCastAs {

    public static final String ANOTHER_CARD = "AnotherCard";

    // the named card each game casts as, made once per game
    private static final Map<Game, Map<String, Card>> TEMPLATES = new WeakHashMap<>();
    // the cards a spell with these types and mana cost may be cast as, worked out once per game
    private static final Map<Game, Map<String, List<ICardFace>>> ANOTHER_CARDS = new WeakHashMap<>();
    // how many cards the AI weighs casting one card as, and how far down its random order it looks for them
    private static final int AI_PICKS = 3;
    private static final int AI_LOOK = 15;
    // a random order kept for the turn, so what the AI is weighing doesn't change every time it looks at its hand
    private static final Map<Card, Pair<Integer, List<String>>> AI_ORDER_BY_CARD = new WeakHashMap<>();
    // Fourth Stage of Magic Design (OncePerTurn$): the turn each static was last cast with
    private static final Map<StaticAbility, Integer> USED_ON_TURN = new WeakHashMap<>();

    public static synchronized Card template(final Game game, final String name, final Player owner) {
        return TEMPLATES.computeIfAbsent(game, g -> new HashMap<>()).computeIfAbsent(name,
                n -> Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(n), owner));
    }

    /** The spells {@code card} may be cast with as though it were another card. */
    public static List<SpellAbility> getCastAsSpells(final Card card, final Player activator) {
        final List<SpellAbility> result = new ArrayList<>();
        final Game game = card.getGame();
        // Enolc, Perfect Clone: a card's own CastAs static, read from the zone it's cast from (EffectZone$ Hand)
        final List<Card> sources = new ArrayList<>(game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        if (!sources.contains(card)) {
            sources.add(card);
        }
        for (final Card ca : sources) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.CastAs)) {
                    continue;
                }
                final ZoneType from = ZoneType.smartValueOf(stAb.getParamOrDefault("AffectedZone", "Hand"));
                if (!card.isInZone(from) || !stAb.matchesValidParam("ValidCard", card)
                        || !stAb.matchesValidParam("Activator", activator)) {
                    continue;
                }
                final String name = stAb.getParam("CastAs");
                if (stAb.hasParam("OncePerTurn") && usedThisTurn(stAb)) {
                    continue;
                }
                if (ANOTHER_CARD.equals(name)) {
                    if (activator.getController().isAI()) {
                        // no strategy: the first few cards in a random order that can be cast right now (some have
                        // their own timing, "only during combat"), offered as ordinary spells for its usual logic
                        int found = 0;
                        int looked = 0;
                        for (final String pick : aiOrder(card, stAb)) {
                            if (found >= AI_PICKS || ++looked > AI_LOOK) {
                                break;
                            }
                            final List<SpellAbility> spells = castAsSpells(card, activator, pick);
                            spells.forEach(s -> s.setCastAsStatic(stAb));
                            spells.removeIf(s -> {
                                s.setActivatingPlayer(activator);
                                return !s.canPlay(true);
                            });
                            if (!spells.isEmpty()) {
                                result.addAll(spells);
                                found++;
                            }
                        }
                    } else if (!anotherCardChoices(card, stAb).isEmpty()) {
                        for (final SpellAbility sa : card.getSpellAbilities()) {
                            if (sa.isSpell()) {
                                final SpellAbility standIn = sa.copy(card, activator, false);
                                standIn.setCastAsAnother(true);
                                standIn.setCastAsStatic(stAb);
                                standIn.setDescription(stAb.hasParam("SameManaCost")
                                        ? "Cast as another card with the same mana cost (" + stAb.getHostCard() + ")"
                                        : "Cast as another card with the same type and mana cost");
                                result.add(standIn);
                                break;
                            }
                        }
                    }
                    continue;
                }
                // Enolc: CastAs$ YourCommander - any of the activator's commanders
                final List<String> names = "YourCommander".equals(name)
                        ? activator.getCommanders().stream().map(Card::getName).filter(n -> !n.equals(card.getName())).toList()
                        : List.of(name);
                for (final String each : names) {
                    List<SpellAbility> spells = castAsSpells(card, activator, each);
                    // Banned Eldraine Card: RaiseCost$ - the card's cost on top of the copied one's
                    if (stAb.hasParam("RaiseCost")) {
                        final List<SpellAbility> raised = new ArrayList<>();
                        for (final SpellAbility s : spells) {
                            final forge.game.cost.Cost cost = s.getPayCosts().copy();
                            cost.add(new forge.game.cost.Cost(stAb.getParam("RaiseCost"), false));
                            final SpellAbility r = s.copyWithDefinedCost(cost);
                            r.setDescription("Cast as " + each + " (" + cost.toSimpleString() + ")");
                            raised.add(r);
                        }
                        spells = raised;
                    }
                    spells.forEach(s -> s.setCastAsStatic(stAb));
                    result.addAll(spells);
                }
            }
        }
        return result;
    }

    private static List<SpellAbility> castAsSpells(final Card card, final Player activator, final String name) {
        final List<SpellAbility> result = new ArrayList<>();
        for (final SpellAbility sa : template(card.getGame(), name, activator).getSpellAbilities()) {
            if (!sa.isSpell()) {
                continue;
            }
            final SpellAbility copy = sa.copy(card, activator, false);
            copy.setCastAs(name);
            copy.setDescription(sa.getAlternativeCost() == null
                    ? "Cast as " + name + " (" + sa.getPayCosts().toSimpleString() + ")"
                    : "Cast as " + name + ": " + sa.getDescription());
            result.add(copy);
        }
        // a land has no spell: cast as one, it's a spell that puts itself onto the battlefield (Banned Eldraine Card's
        // Mystic Sanctuary)
        if (result.isEmpty() && template(card.getGame(), name, activator).isLand()) {
            final SpellAbility land = forge.game.ability.AbilityFactory.getAbility("SP$ PermanentNoncreature | Cost$ 0"
                    + " | SpellDescription$ Cast as " + name + ".", card);
            land.setActivatingPlayer(activator);
            land.setCastAs(name);
            land.setDescription("Cast as " + name + " (it enters the battlefield as it resolves)");
            result.add(land);
        }
        return result;
    }

    /** The choices for this card the AI knows how to play: none its deck builder is told to leave out. */
    public static List<String> aiPlayableChoices(final Card card) {
        return aiPlayableChoices(card, null);
    }
    private static List<String> aiPlayableChoices(final Card card, final StaticAbility stAb) {
        final List<String> result = new ArrayList<>();
        for (final ICardFace face : anotherCardChoices(card, stAb)) {
            final PaperCard pc = StaticData.instance().getCommonCards().getUniqueByName(face.getName());
            if (pc != null && !pc.getRules().getAiHints().getRemAIDecks()) {
                result.add(face.getName());
            }
        }
        return result;
    }

    private static synchronized List<String> aiOrder(final Card card, final StaticAbility stAb) {
        final int turn = card.getGame().getPhaseHandler().getTurn();
        // the kept order is Imaginary Friends' - another static's choices aren't the same cards
        final boolean keep = !stAb.hasParam("SameManaCost");
        final Pair<Integer, List<String>> kept = keep ? AI_ORDER_BY_CARD.get(card) : null;
        if (kept != null && kept.getLeft() == turn) {
            return kept.getRight();
        }
        final List<String> names = aiPlayableChoices(card, stAb);
        if (!keep) {
            Collections.shuffle(names, MyRandom.getRandom());
            return names;
        }
        Collections.shuffle(names, MyRandom.getRandom());
        AI_ORDER_BY_CARD.put(card, Pair.of(turn, names));
        return names;
    }

    /** The cards legal in this game's format, other than this one, with this card's card types and mana cost. */
    public static List<ICardFace> anotherCardChoices(final Card card) {
        return anotherCardChoices(card, null);
    }

    /**
     * With {@code SameManaCost$} (Fourth Stage of Magic Design) any nonland card with the same mana cost will do, and
     * with {@code NoRepeat$} the names its host was already cast as are left out.
     */
    public static List<ICardFace> anotherCardChoices(final Card card, final StaticAbility stAb) {
        final Game game = card.getGame();
        final DeckFormat format = gameFormat(game);
        final boolean anyType = stAb != null && stAb.hasParam("SameManaCost");
        final EnumSet<CardType.CoreType> types = coreTypes(card.getType().getCoreTypes());
        final String cost = card.getManaCost().getShortString();
        final String key = (anyType ? "nonland" : types) + "|" + cost + "|" + format;
        final List<ICardFace> all;
        synchronized (ANOTHER_CARDS) {
            all = ANOTHER_CARDS.computeIfAbsent(game, g -> new HashMap<>()).computeIfAbsent(key, k -> {
                final List<ICardFace> result = new ArrayList<>();
                for (final PaperCard pc : StaticData.instance().getCommonCards().getUniqueCards()) {
                    // a split card's halves are cast one at a time, so the whole card's cost isn't any one spell's
                    if (pc.getRules().getSplitType() == CardSplitType.Split) {
                        continue;
                    }
                    final ICardFace face = pc.getRules().getMainPart();
                    if (face.getManaCost().getShortString().equals(cost) && format.isLegalCard(pc) && (anyType
                            ? !face.getType().isLand() : coreTypes(face.getType().getCoreTypes()).equals(types))) {
                        result.add(face);
                    }
                }
                return result;
            });
        }
        final List<String> used = stAb != null && stAb.hasParam("NoRepeat") ? stAb.getHostCard().getNamedCards() : List.of();
        return all.stream().filter(f -> !f.getName().equals(card.getName()) && !used.contains(f.getName())).toList();
    }

    private static synchronized boolean usedThisTurn(final StaticAbility stAb) {
        final Integer turn = USED_ON_TURN.get(stAb);
        return turn != null && turn == stAb.getHostCard().getGame().getPhaseHandler().getTurn();
    }

    /** A spell cast with a CastAs static has been cast: what Fourth Stage of Magic Design keeps track of. */
    public static synchronized void recordCast(final SpellAbility sp) {
        final StaticAbility stAb = sp.getCastAsStatic();
        if (stAb.hasParam("OncePerTurn")) {
            USED_ON_TURN.put(stAb, stAb.getHostCard().getGame().getPhaseHandler().getTurn());
        }
        if (stAb.hasParam("NoRepeat") && sp.getCastAs() != null) {
            stAb.getHostCard().addNamedCard(sp.getCastAs());
        }
    }

    /**
     * Asks which card the stand-in spell is cast as, and returns that card's spell for this card to be cast with, or
     * null if the player backed out.
     */
    public static SpellAbility chooseAnotherCard(final Card card, final Player activator, final SpellAbility sa) {
        final List<ICardFace> choices = anotherCardChoices(card, sa.getCastAsStatic());
        if (choices.isEmpty()) {
            return null;
        }
        final String name = activator.getController().chooseCardName(sa, choices,
                "Choose a card to cast " + card.getName() + " as");
        if (name == null || name.isEmpty()) {
            return null;
        }
        final List<SpellAbility> spells = new ArrayList<>();
        for (final SpellAbility spell : template(card.getGame(), name, activator).getSpellAbilities()) {
            if (spell.isSpell()) {
                final SpellAbility copy = spell.copy(card, activator, false);
                copy.setCastAs(name);
                copy.setCastAsStatic(sa.getCastAsStatic());
                spells.add(copy);
            }
        }
        if (spells.isEmpty()) {
            return null;
        }
        return spells.size() == 1 ? spells.get(0) : activator.getController().getAbilityToPlay(card, spells);
    }

    private static EnumSet<CardType.CoreType> coreTypes(final Iterable<CardType.CoreType> types) {
        final EnumSet<CardType.CoreType> result = EnumSet.noneOf(CardType.CoreType.class);
        types.forEach(result::add);
        return result;
    }

    // a Commander variant decides what's legal; otherwise it's the game type's own format
    private static DeckFormat gameFormat(final Game game) {
        final GameRules rules = game.getRules();
        for (final GameType t : new GameType[] {GameType.Commander, GameType.Oathbreaker, GameType.TinyLeaders, GameType.Brawl}) {
            if (rules.hasAppliedVariant(t)) {
                return t.getDeckFormat();
            }
        }
        return rules.getGameType().getDeckFormat();
    }
}
