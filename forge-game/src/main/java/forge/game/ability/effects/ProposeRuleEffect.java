package forge.game.ability.effects;

import java.util.Arrays;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.MyRandom;

/**
 * The Policy Maker: "a player outside the game" proposes one of the Rules$ (SVars of the host, each an effect whose
 * SpellDescription$ is the rule, user ruling 2026-10-22: a random rule from real cards that affect every player). Each
 * player in the game agrees or not; if they all do, the rule applies. The host's SVar PolicyRejected is 1 if an
 * opponent didn't agree. The AI agrees to its own rules, and to an opponent's half the time.
 */
public class ProposeRuleEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Ask a player outside the game to propose a new rule.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        host.setSVar("PolicyRejected", "Number$0");
        final String pick = Aggregates.random(Arrays.asList(sa.getParam("Rules").split(",")));
        final SpellAbility rule = AbilityFactory.getAbility(host.getSVar(pick), host);
        rule.setActivatingPlayer(p);
        final String text = rule.getParamOrDefault("SpellDescription", pick);
        game.getAction().notifyOfValue(sa, p, "Proposed rule: " + text, null);

        boolean all = true;
        boolean opponentSaidNo = false;
        for (final Player pl : game.getPlayers()) {
            final boolean yes = pl.getController().isAI() ? pl.equals(p) || MyRandom.getRandom().nextBoolean()
                    : pl.getController().confirmAction(sa, null, "Agree to this rule for the rest of the game?\n" + text, null);
            game.getGameLog().add(GameLogEntryType.INFORMATION, pl + (yes ? " agrees to " : " doesn't agree to ") + "the rule: " + text);
            if (!yes) {
                all = false;
                opponentSaidNo |= pl.isOpponentOf(p);
            }
        }
        if (all) {
            AbilityUtils.resolve(rule);
        }
        if (opponentSaidNo) {
            host.setSVar("PolicyRejected", "Number$1");
        }
    }
}
