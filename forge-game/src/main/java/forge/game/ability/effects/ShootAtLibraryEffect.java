package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.MyRandom;

/**
 * Nerf War's volley: fire a toy blaster at a library until it's empty. Nobody can aim a real blaster
 * at a digital library, so unlike the honor-system costs this is simulated for every player, dart by
 * dart - each dart hits or misses, and a hit knocks a few cards off the top of the stack.
 * Knocked-off cards are remembered on the host for the rest of the script to move and count.
 */
public class ShootAtLibraryEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        final int shots = amount(sa, "Shots", 6);
        final int hitPercent = amount(sa, "HitPercent", 40);
        final int knockMin = amount(sa, "KnockMin", 1);
        final int knockMax = Math.max(knockMin, amount(sa, "KnockMax", 3));

        for (final Player p : getTargetPlayers(sa)) {
            if (!p.isInGame()) {
                continue;
            }
            final CardCollection library = new CardCollection(p.getCardsIn(ZoneType.Library));
            final CardCollection knocked = new CardCollection();
            final StringBuilder log = new StringBuilder();

            for (int dart = 1; dart <= shots; dart++) {
                log.append("Dart ").append(dart).append(": ");
                if (knocked.size() >= library.size()) {
                    log.append("nothing left to hit.\n");
                    continue;
                }
                if (!MyRandom.percentTrue(hitPercent)) {
                    log.append("missed.\n");
                    continue;
                }
                int count = knockMin + MyRandom.getRandom().nextInt(knockMax - knockMin + 1);
                count = Math.min(count, library.size() - knocked.size());
                // darts hit the stack and slide the top cards off, so it's always the current top
                for (int i = 0; i < count; i++) {
                    knocked.add(library.get(knocked.size()));
                }
                if (count == 0) {
                    log.append("hit, but nothing fell off.\n");
                } else {
                    log.append("hit! Knocked off ").append(count).append(count == 1 ? " card.\n" : " cards.\n");
                }
            }
            log.append("Total: ").append(knocked.size()).append(knocked.size() == 1 ? " card" : " cards")
                    .append(" knocked off ").append(p).append("'s library.");
            game.getAction().notifyOfValue(sa, p, log.toString(), null);

            host.addRemembered(knocked);
        }
    }

    private static int amount(final SpellAbility sa, final String param, final int dflt) {
        return sa.hasParam(param) ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam(param), sa) : dflt;
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Fire a Nerf® blaster until empty at " + Lang.joinHomogenous(getTargetPlayers(sa)) + "'s library.";
    }
}
