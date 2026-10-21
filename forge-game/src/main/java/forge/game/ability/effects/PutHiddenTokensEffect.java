package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.card.TokenCreateTable;
import forge.game.card.token.TokenInfo;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.HiddenPrintedToken;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Surprise Party: "put onto the battlefield any number of printed ... tokens ... that you hid on the battlefield before
 * you cast this enchantment, and that weren't spotted by an opponent before you cast this enchantment."
 * AtCast$ True notes, as it's cast, which hidden tokens are unspotted; as it resolves later, those still hidden go onto
 * the battlefield - however many its controller chooses.
 */
public class PutHiddenTokensEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " yells \"Surprise!\"";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        final Card host = sa.getHostCard();
        if (sa.hasParam("AtCast")) {
            final List<HiddenPrintedToken> unspotted = Lists.newArrayList();
            for (final HiddenPrintedToken t : you.getHiddenPrintedTokens()) {
                if (!t.isSpotted()) {
                    unspotted.add(t);
                }
            }
            you.getHiddenTokensAtCast().put(host.getId(), unspotted);
            return;
        }

        game.getGameLog().add(GameLogEntryType.INFORMATION, you + " yells \"Surprise!\"");
        final List<HiddenPrintedToken> eligible = Lists.newArrayList();
        final List<HiddenPrintedToken> atCast = you.getHiddenTokensAtCast().remove(host.getId());
        if (atCast != null) {
            for (final HiddenPrintedToken t : atCast) {
                if (you.getHiddenPrintedTokens().contains(t)) {
                    eligible.add(t);
                }
            }
        }
        if (eligible.isEmpty()) {
            return;
        }
        final int n = you.getController().chooseNumber(sa, "How many of your hidden tokens do you put onto the battlefield?",
                0, eligible.size());
        if (n <= 0) {
            return;
        }

        final TokenCreateTable tokenTable = new TokenCreateTable();
        for (final HiddenPrintedToken t : eligible.subList(0, n)) {
            you.getHiddenPrintedTokens().remove(t);
            final Card token = TokenInfo.getProtoType(t.getTokenScript(), sa, you, false);
            token.setTokenSpawningAbility(sa);
            tokenTable.put(you, token, 1);
        }
        if (you.getHiddenPrintedTokens().isEmpty()) {
            SeekHiddenEffect.exileTokenSeekEffects(game, you);
        }
        final CardZoneTable triggerList = new CardZoneTable();
        final MutableBoolean combatChanged = new MutableBoolean(false);
        makeTokenTable(tokenTable, false, triggerList, combatChanged, sa);
        triggerList.triggerChangesZoneAll(game, sa);
        game.fireEvent(new GameEventTokenCreated());
        if (combatChanged.isTrue()) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    }
}
