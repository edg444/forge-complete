package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

/**
 * Pointing at where a hidden Entirely Normal Armchair is. Its opponents can return it only "if they see this
 * artifact", which the rulings and FAQ take as being able to show where it is - so a right guess means they've found
 * it: it's no longer hidden and they may return it right away. A wrong guess shows they didn't know. Once each turn
 * per opponent, so pointing at every spot in turn isn't a way to find it (user, 2026-10-09).
 */
public class SeekHiddenEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " looks for something hidden.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player seeker = sa.getActivatingPlayer();
        final Game game = seeker.getGame();
        for (final Card chair : getDefinedCardsOrTargeted(sa)) {
            if (!chair.isHiddenFrom(seeker)) {
                continue;
            }
            final Player hider = chair.getController();
            final FCollection<GameEntity> spots = new FCollection<>();
            for (final Card c : hider.getCardsIn(ZoneType.Battlefield)) {
                if (!c.isHiddenFrom(seeker)) {
                    spots.add(c);
                }
            }
            spots.add(hider);
            final GameEntity guess = seeker.getController().chooseSingleEntityForEffect(spots, sa,
                    "Point at the permanent it's hidden behind, or at " + hider + " if it's in plain sight", null);
            if (guess == null) {
                continue;
            }
            final GameEntity actual = chair.getHiddenSpot();
            final String where = guess == hider ? "in plain sight" : "behind " + guess;
            if (!guess.equals(actual)) {
                game.getGameLog().add(GameLogEntryType.INFORMATION, seeker + " looked " + where + " and found nothing.");
                continue;
            }

            chair.setHiddenSpot(null);
            exileSeekEffects(game, chair);
            game.getGameLog().add(GameLogEntryType.INFORMATION, seeker + " found " + chair + " " + where + "!");

            // now that they see it, they may return it - its own {0} ability, on the stack as usual
            for (final SpellAbility ab : chair.getSpellAbilities()) {
                if (ab.getApi() != ApiType.ChangeZone || !ab.isActivatedAbility()) {
                    continue;
                }
                final SpellAbility bounce = ab;
                bounce.setActivatingPlayer(seeker);
                final Map<String, Object> params = Maps.newHashMap();
                if (bounce.canPlay() && seeker.getController().confirmAction(sa, PlayerActionConfirmMode.Random,
                        "Return " + chair + " to its owner's hand now?", null, chair, params)) {
                    seeker.getController().playChosenSpellAbility(bounce);
                }
                break;
            }
        }
    }

    /** The hiding is over, so the "Something Hidden" effects pointing at it go. */
    static void exileSeekEffects(final Game game, final Card chair) {
        for (final Player p : new PlayerCollection(game.getPlayers())) {
            for (final Card eff : new CardCollection(p.getCardsIn(ZoneType.Command))) {
                if (HideEffect.SEEK_EFFECT_NAME.equals(eff.getName()) && eff.isRemembered(chair)) {
                    game.getAction().exileEffect(eff);
                }
            }
        }
    }
}
