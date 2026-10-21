package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Merfolk Surveyor's explore-clash: you and an opponent each reveal the top card of your library. Revealed lands go to
 * their owners' hands, and the host gets a +1/+1 counter for each nonland revealed. Each player whose card was a nonland
 * keeps it on top or puts it into their graveyard. You win if your card's mana value was higher (WinSubAbility$).
 */
public class ExploreClashEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getHostCard().getDisplayName() + " - Explore-clash with an opponent.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player you = sa.getActivatingPlayer();
        final Player opponent = you.getOpponents().isEmpty() ? null
                : you.getController().chooseSingleEntityForEffect(you.getOpponents(), sa, "Choose an opponent to explore-clash with", null);
        final List<Player> clashers = Lists.newArrayList(you);
        if (opponent != null) {
            clashers.add(opponent);
        }
        final Map<Player, Integer> manaValues = Maps.newHashMap();
        final CardCollection revealed = new CardCollection();
        int nonlands = 0;
        for (final Player p : clashers) {
            final CardCollection top = p.getTopXCardsFromLibrary(1);
            if (top.isEmpty()) {
                manaValues.put(p, -1);
                continue;
            }
            revealed.add(top.getFirst());
            manaValues.put(p, top.getFirst().getCMC());
        }
        game.getAction().revealTo(revealed, game.getPlayers(), "Explore-clash: ", false);
        for (final Card r : revealed) {
            final Player owner = r.getOwner();
            if (r.isLand()) {
                game.getAction().moveTo(ZoneType.Hand, r, sa, AbilityKey.newMap());
                continue;
            }
            nonlands++;
            final Map<String, Object> params = Maps.newHashMap();
            params.put("RevealedCard", r);
            if (owner.getController().confirmAction(sa, null, "Put " + r.getTranslatedName() + " into your graveyard?", r, params)) {
                game.getAction().moveTo(ZoneType.Graveyard, r, sa, AbilityKey.newMap());
            }
        }
        final Card gameHost = game.getCardState(host);
        if (nonlands > 0 && gameHost.isInPlay() && gameHost.equalsWithGameTimestamp(host)) {
            final GameEntityCounterTable table = new GameEntityCounterTable();
            gameHost.addCounter(CounterEnumType.P1P1, nonlands, you, table);
            table.replaceCounterEffect(game, sa);
        }
        final int mine = manaValues.getOrDefault(you, -1);
        final int theirs = opponent == null ? -1 : manaValues.getOrDefault(opponent, -1);
        if (mine > theirs) {
            game.getAction().notifyOfValue(sa, you, you + " wins the explore-clash.", null);
            final SpellAbility win = sa.getAdditionalAbility("WinSubAbility");
            if (win != null) {
                AbilityUtils.resolve(win);
            }
        }
    }
}
