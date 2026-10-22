package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;

/**
 * Adventurer Beguiler: the targeted card is now on an adventure for you, so no one else may cast it from exile any
 * more. This takes it out of every other adventure effect that lets someone cast it; the script's sub-ability gives
 * the activator their own.
 */
public class TakeAdventureEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return getTargetCards(sa) + " is now on an adventure for " + sa.getActivatingPlayer() + ".";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        for (final Card c : getTargetCards(sa)) {
            for (final Player p : game.getPlayers()) {
                for (final Card eff : new CardCollection(p.getCardsIn(ZoneType.Command))) {
                    if (!eff.isRemembered(c) || !lets(eff)) {
                        continue;
                    }
                    eff.removeRemembered(c);
                    if (eff.getRememberedCount() == 0) {
                        game.getAction().exileEffect(eff);
                    }
                }
            }
        }
    }

    private static boolean lets(final Card eff) {
        for (final StaticAbility st : eff.getStaticAbilities()) {
            if (st.hasParam("MayPlay")) {
                return true;
            }
        }
        return false;
    }
}
