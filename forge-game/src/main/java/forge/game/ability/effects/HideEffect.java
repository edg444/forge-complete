package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

/**
 * Entirely Normal Armchair: "During your turn, if this card is in your hand, you may hide it on the battlefield."
 * <p>
 * It really is on the battlefield; it's just hidden - behind one of your permanents, or in plain sight (rulings, FAQ).
 * Opponents don't see it (Card.isHiddenFrom): it isn't drawn for them, and they can't target it or use its abilities.
 * Each opponent gets a command-zone "Something Hidden" effect for pointing at where it is, once each turn (user,
 * 2026-10-09: a hidden spot to guess) - see SeekHiddenEffect. The effects go away when it leaves the battlefield.
 */
public class HideEffect extends SpellAbilityEffect {

    public static final String SEEK_EFFECT_NAME = "Something Hidden";

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " hides something.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final Card host = sa.getHostCard();
        if (!you.getZone(ZoneType.Hand).contains(host)) {
            return;
        }

        final FCollection<GameEntity> spots = new FCollection<>();
        spots.addAll(you.getCardsIn(ZoneType.Battlefield));
        spots.add(you);
        GameEntity spot = you.getController().chooseSingleEntityForEffect(spots, sa,
                "Choose a permanent to hide it behind, or yourself to hide it in plain sight", null);
        if (spot == null) {
            spot = you;
        }

        final Card hidden = game.getAction().moveToPlay(host, sa, null);
        if (hidden == null || !hidden.isInPlay()) {
            return;
        }
        hidden.setHiddenSpot(spot);
        game.getGameLog().add(GameLogEntryType.INFORMATION, you + " hid something on their side of the battlefield.");

        for (final Player opp : you.getOpponents()) {
            final Card eff = createEffect(sa, hidden, opp, SEEK_EFFECT_NAME, null, game.getNextTimestamp());
            eff.addRemembered(hidden);
            addExileOnMovedTrigger(eff, "Battlefield");
            eff.addSpellAbility(AbilityFactory.getAbility("ST$ SeekHidden | Cost$ 0 | ActivationZone$ Command"
                    + " | ActivationLimit$ 1 | Defined$ Remembered | SpellDescription$ " + you
                    + " has hidden something on their side of the battlefield. Once each turn, you may point at where"
                    + " you think it is: behind one of their permanents, or at them for in plain sight.", eff));
            eff.updateStateForView();
            game.getAction().moveToCommand(eff, sa);
        }
    }
}
