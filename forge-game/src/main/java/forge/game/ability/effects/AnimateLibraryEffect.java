package forge.game.ability.effects;

import java.util.Map;

import forge.ImageKeys;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;

/**
 * Animate Library's Aura spell. There is nothing to target until it resolves - a library isn't an object
 * on the battlefield - so the spell makes the caster's library a permanent (a {@link GamePieceType#LIBRARY}
 * object), then enters attached to it the way an Aura spell does (AttachEffect).
 * <p>
 * Per the user's ruling the library neither enters nor leaves the battlefield: it just starts and stops
 * being a permanent, so it is placed and removed without zone-change triggers. The Aura's own statics give
 * it its power and toughness and its replacement keeps it from leaving; when the Aura goes, it's a library
 * again.
 */
public class AnimateLibraryEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card aura = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();

        // a second Animate Library enchants the library that's already a permanent
        final Card existing = findLibraryPermanent(p);
        final Card library = existing != null ? existing : createLibraryPermanent(p);

        final CardZoneTable table = new CardZoneTable();
        aura.setController(p, 0);
        final ZoneType previousZone = aura.getZone().getZoneType();
        aura.attachToEntity(library, sa);

        final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, game.copyLastStateGraveyard());
        final Card c = game.getAction().moveToPlay(aura, p, sa, moveParams);
        if (c.getZone().getZoneType() != previousZone) {
            table.put(previousZone, c.getZone().getZoneType(), c);
        }
        table.triggerChangesZoneAll(game, sa);

        if (!c.isInPlay() || !c.isAttachedToEntity(library)) {
            // the Aura didn't make it (countered on the way in, replaced elsewhere): nothing is animated
            removeLibraryPermanent(library);
            return;
        }
        c.addLeavesPlayCommand(() -> removeLibraryPermanent(library));
    }

    private static Card createLibraryPermanent(final Player owner) {
        final Game game = owner.getGame();
        final Card lib = new Card(game.nextCardId(), game);
        lib.setGamePieceType(GamePieceType.LIBRARY);
        // a library has no name, mana cost or color; what it is comes from Animate Library
        lib.setName("");
        lib.setOwner(owner);
        lib.setType(CardType.parse("Artifact Creature", false));
        lib.setColor(ColorSet.C);
        lib.setBasePower(0);
        lib.setBaseToughness(0);
        lib.setImageKey(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD));
        lib.setGameTimestamp(game.getNextTimestamp());
        lib.setLayerTimestamp(game.getNextTimestamp());
        lib.setSickness(true);

        final Zone battlefield = owner.getZone(ZoneType.Battlefield);
        battlefield.add(lib);
        lib.setZone(battlefield);
        return lib;
    }

    public static Card findLibraryPermanent(final Player owner) {
        for (final Card c : owner.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (c.getGamePieceType() == GamePieceType.LIBRARY && c.getOwner().equals(owner)) {
                return c;
            }
        }
        return null;
    }

    /** A library stays a permanent while any Animate Library is still on it. */
    private static void removeLibraryPermanent(final Card lib) {
        if (!lib.isInPlay()) {
            return;
        }
        for (final Card a : lib.getAttachedCards()) {
            if (a.isInPlay() && a.hasSVar("AuraSpell") && a.getSVar("AuraSpell").contains("AnimateLibrary")) {
                return;
            }
        }
        lib.getGame().getAction().ceaseToExist(lib, true);
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Enchant " + sa.getActivatingPlayer() + "'s library.";
    }
}
