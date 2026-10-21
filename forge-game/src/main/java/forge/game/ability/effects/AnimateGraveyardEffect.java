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
 * Animate Graveyard's Aura spell, the way Animate Library's works (AnimateLibraryEffect): a graveyard isn't an object on
 * the battlefield until the spell resolves, so the spell makes the caster's graveyard a permanent (a
 * {@link GamePieceType#GRAVEYARD} object) and enters attached to it. The graveyard is still a graveyard - cards still go
 * to it, and its cards aren't on the battlefield (rulings). It never enters or leaves: when the Aura goes, it's just a
 * graveyard again, and nothing dies.
 */
public class AnimateGraveyardEffect extends SpellAbilityEffect {

    @Override
    public void resolve(final SpellAbility sa) {
        final Card aura = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();

        final Card existing = findGraveyardPermanent(p);
        final Card graveyard = existing != null ? existing : createGraveyardPermanent(p);

        final CardZoneTable table = new CardZoneTable();
        aura.setController(p, 0);
        final ZoneType previousZone = aura.getZone().getZoneType();
        aura.attachToEntity(graveyard, sa);

        final Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
        moveParams.put(AbilityKey.LastStateBattlefield, game.copyLastStateBattlefield());
        moveParams.put(AbilityKey.LastStateGraveyard, game.copyLastStateGraveyard());
        final Card c = game.getAction().moveToPlay(aura, p, sa, moveParams);
        if (c.getZone().getZoneType() != previousZone) {
            table.put(previousZone, c.getZone().getZoneType(), c);
        }
        table.triggerChangesZoneAll(game, sa);

        if (!c.isInPlay() || !c.isAttachedToEntity(graveyard)) {
            removeGraveyardPermanent(graveyard);
            return;
        }
        c.addLeavesPlayCommand(() -> removeGraveyardPermanent(graveyard));
    }

    private static Card createGraveyardPermanent(final Player owner) {
        final Game game = owner.getGame();
        final Card yard = new Card(game.nextCardId(), game);
        yard.setGamePieceType(GamePieceType.GRAVEYARD);
        // a graveyard has no name, mana cost, color or creature types (rulings); Animate Graveyard gives it the rest
        yard.setName("");
        yard.setOwner(owner);
        yard.setType(CardType.parse("Creature", false));
        yard.setColor(ColorSet.C);
        yard.setBasePower(0);
        yard.setBaseToughness(0);
        yard.setImageKey(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD));
        yard.setGameTimestamp(game.getNextTimestamp());
        yard.setLayerTimestamp(game.getNextTimestamp());
        yard.setSickness(true);

        final Zone battlefield = owner.getZone(ZoneType.Battlefield);
        battlefield.add(yard);
        yard.setZone(battlefield);
        return yard;
    }

    public static Card findGraveyardPermanent(final Player owner) {
        for (final Card c : owner.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (c.getGamePieceType() == GamePieceType.GRAVEYARD && c.getOwner().equals(owner)) {
                return c;
            }
        }
        return null;
    }

    /** A graveyard stays a creature while any Animate Graveyard is still on it. */
    private static void removeGraveyardPermanent(final Card yard) {
        if (!yard.isInPlay()) {
            return;
        }
        for (final Card a : yard.getAttachedCards()) {
            if (a.isInPlay() && a.hasSVar("AuraSpell") && a.getSVar("AuraSpell").contains("AnimateGraveyard")) {
                return;
            }
        }
        yard.getGame().getAction().ceaseToExist(yard, true);
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Enchant " + sa.getActivatingPlayer() + "'s graveyard.";
    }
}
