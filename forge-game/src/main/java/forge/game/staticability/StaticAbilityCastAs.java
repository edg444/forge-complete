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

    public static synchronized Card template(final Game game, final String name, final Player owner) {
        return TEMPLATES.computeIfAbsent(game, g -> new HashMap<>()).computeIfAbsent(name,
                n -> Card.fromPaperCard(StaticData.instance().getCommonCards().getUniqueByName(n), owner));
    }

    /** The spells {@code card} may be cast with as though it were another card. */
    public static List<SpellAbility> getCastAsSpells(final Card card, final Player activator) {
        final List<SpellAbility> result = new ArrayList<>();
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
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
                if (ANOTHER_CARD.equals(name)) {
                    if (activator.getController().isAI()) {
                        // no strategy: the first few cards in a random order that can be cast right now (some have
                        // their own timing, "only during combat"), offered as ordinary spells for its usual logic
                        int found = 0;
                        int looked = 0;
                        for (final String pick : aiOrder(card)) {
                            if (found >= AI_PICKS || ++looked > AI_LOOK) {
                                break;
                            }
                            final List<SpellAbility> spells = castAsSpells(card, activator, pick);
                            spells.removeIf(s -> {
                                s.setActivatingPlayer(activator);
                                return !s.canPlay(true);
                            });
                            if (!spells.isEmpty()) {
                                result.addAll(spells);
                                found++;
                            }
                        }
                    } else if (!anotherCardChoices(card).isEmpty()) {
                        for (final SpellAbility sa : card.getSpellAbilities()) {
                            if (sa.isSpell()) {
                                final SpellAbility standIn = sa.copy(card, activator, false);
                                standIn.setCastAsAnother(true);
                                standIn.setDescription("Cast as another card with the same type and mana cost");
                                result.add(standIn);
                                break;
                            }
                        }
                    }
                    continue;
                }
                result.addAll(castAsSpells(card, activator, name));
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
        return result;
    }

    /** The choices for this card the AI knows how to play: none its deck builder is told to leave out. */
    public static List<String> aiPlayableChoices(final Card card) {
        final List<String> result = new ArrayList<>();
        for (final ICardFace face : anotherCardChoices(card)) {
            final PaperCard pc = StaticData.instance().getCommonCards().getUniqueByName(face.getName());
            if (pc != null && !pc.getRules().getAiHints().getRemAIDecks()) {
                result.add(face.getName());
            }
        }
        return result;
    }

    private static synchronized List<String> aiOrder(final Card card) {
        final int turn = card.getGame().getPhaseHandler().getTurn();
        final Pair<Integer, List<String>> kept = AI_ORDER_BY_CARD.get(card);
        if (kept != null && kept.getLeft() == turn) {
            return kept.getRight();
        }
        final List<String> names = aiPlayableChoices(card);
        Collections.shuffle(names, MyRandom.getRandom());
        AI_ORDER_BY_CARD.put(card, Pair.of(turn, names));
        return names;
    }

    /** The cards legal in this game's format, other than this one, with this card's card types and mana cost. */
    public static List<ICardFace> anotherCardChoices(final Card card) {
        final Game game = card.getGame();
        final DeckFormat format = gameFormat(game);
        final EnumSet<CardType.CoreType> types = coreTypes(card.getType().getCoreTypes());
        final String cost = card.getManaCost().getShortString();
        final String key = types + "|" + cost + "|" + format;
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
                    if (face.getManaCost().getShortString().equals(cost)
                            && coreTypes(face.getType().getCoreTypes()).equals(types) && format.isLegalCard(pc)) {
                        result.add(face);
                    }
                }
                return result;
            });
        }
        return all.stream().filter(f -> !f.getName().equals(card.getName())).toList();
    }

    /**
     * Asks which card the stand-in spell is cast as, and returns that card's spell for this card to be cast with, or
     * null if the player backed out.
     */
    public static SpellAbility chooseAnotherCard(final Card card, final Player activator, final SpellAbility sa) {
        final List<ICardFace> choices = anotherCardChoices(card);
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
