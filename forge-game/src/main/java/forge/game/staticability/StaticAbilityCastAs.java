package forge.game.staticability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import forge.StaticData;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Form of the Mulldrifter's "You may cast creature cards from your hand as though they were the card Mulldrifter.":
 * {@code Mode$ CastAs | ValidCard$ <cards> | Activator$ <players> | AffectedZone$ <zone> | CastAs$ <card name>}. Each
 * matching card gets that card's spells (with its alternative costs, such as evoke), and a card cast with one takes on
 * that card's copiable values as it goes on the stack (see {@code Card.setSplitStateToPlayAbility}), keeping them
 * until it leaves the stack other than by resolving or leaves the battlefield - it's a new object either way.
 */
public class StaticAbilityCastAs {

    // the named card each game casts as, made once per game
    private static final Map<Game, Map<String, Card>> TEMPLATES = new WeakHashMap<>();

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
                for (final SpellAbility sa : template(game, name, activator).getSpellAbilities()) {
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
            }
        }
        return result;
    }
}
