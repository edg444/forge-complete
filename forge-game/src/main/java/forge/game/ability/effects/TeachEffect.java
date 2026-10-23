package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Teach (Pyromancy 101): as the spell resolves, after its other effects, it's exiled taught to a creature its
 * controller controls - chosen, not targeted, and not optional if there's one (rulings). With none, it's exiled taught
 * to nobody. The taught creature is remembered on the exiled card, which loses it when either one changes zones.
 */
public class TeachEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        for (final Card spell : AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParamOrDefault("Defined", "ReplacedCard"), sa)) {
            final Card exiled = game.getAction().exile(game.getCardState(spell), sa, null);
            if (exiled == null || !exiled.isInZone(ZoneType.Exile)) {
                continue;
            }
            final CardCollection creatures = p.getCreaturesInPlay();
            if (creatures.isEmpty()) {
                game.getGameLog().add(GameLogEntryType.INFORMATION, exiled + " is exiled, taught to nobody.");
                continue;
            }
            final Card student = p.getController().chooseSingleEntityForEffect(creatures, sa,
                    "Choose a creature you control to teach " + exiled.getTranslatedName() + " to", false, null);
            exiled.setTaughtTo(student);
            game.getGameLog().add(GameLogEntryType.INFORMATION, exiled + " is taught to " + student + ".");
        }
    }
}
