package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.HiddenPrintedToken;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

/**
 * Surprise Party: hiding a printed token (TokenScript$) on the battlefield - behind one of your permanents or in plain
 * sight, one per spot since the tokens can't touch (rulings). Each opponent gets a command-zone "Hidden Tokens" effect
 * for pointing at where one is, once each turn, as with Entirely Normal Armchair (SeekHiddenEffect).
 */
public class HidePrintedTokenEffect extends SpellAbilityEffect {

    public static final String SEEK_TOKENS_NAME = "Hidden Tokens";

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " hides something.";
    }

    /** Your permanents and you (plain sight), less the spots already holding one of your hidden tokens. */
    public static FCollection<GameEntity> freeSpots(final Player you) {
        final FCollection<GameEntity> spots = new FCollection<>();
        spots.addAll(you.getCardsIn(ZoneType.Battlefield));
        spots.add(you);
        for (final HiddenPrintedToken t : you.getHiddenPrintedTokens()) {
            spots.remove(t.getSpot());
        }
        return spots;
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final FCollection<GameEntity> spots = freeSpots(you);
        if (spots.isEmpty()) {
            return;
        }
        final GameEntity spot = you.getController().chooseSingleEntityForEffect(spots, sa,
                "Choose a permanent to hide the token behind, or yourself to hide it in plain sight", null);
        if (spot == null) {
            return;
        }
        you.getHiddenPrintedTokens().add(new HiddenPrintedToken(you, spot, sa.getParam("TokenScript")));
        game.getGameLog().add(GameLogEntryType.INFORMATION, you + " hid something on their side of the battlefield.");

        for (final Player opp : you.getOpponents()) {
            boolean seeking = false;
            for (final Card eff : opp.getCardsIn(ZoneType.Command)) {
                seeking |= SEEK_TOKENS_NAME.equals(eff.getName()) && eff.isRemembered(you);
            }
            if (seeking) {
                continue;
            }
            final Card eff = createEffect(sa, sa.getHostCard(), opp, SEEK_TOKENS_NAME, null, game.getNextTimestamp());
            eff.addRemembered(you);
            eff.addSpellAbility(AbilityFactory.getAbility("ST$ SeekHidden | Cost$ 0 | ActivationZone$ Command"
                    + " | ActivationLimit$ 1 | PrintedTokensOf$ Remembered | SpellDescription$ " + you
                    + " has hidden printed tokens on their side of the battlefield. Once each turn, you may point at where"
                    + " you think one is: behind one of their permanents, or at them for in plain sight.", eff));
            eff.updateStateForView();
            game.getAction().moveToCommand(eff, sa);
        }
    }
}
