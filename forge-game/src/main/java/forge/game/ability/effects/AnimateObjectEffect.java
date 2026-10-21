package forge.game.ability.effects;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.GameLogEntryType;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.card.TokenCreateTable;
import forge.game.card.sticker.AppliedSticker;
import forge.game.card.sticker.Sticker;
import forge.game.card.sticker.StickerKind;
import forge.game.card.sticker.StickerSheet;
import forge.game.card.token.TokenInfo;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Animate Object: an inanimate object from outside the game, stickered and put onto the battlefield as a creature
 * token. The object itself means nothing to the game (rulings: its printed numbers, text and art are ignored), so it's
 * a blank TokenScript$ creature with no name. After Tickets$ more tickets, the stickers go on before it enters: a
 * power/toughness sticker, then any of a name, an art and an ability sticker, each paid for in tickets as usual.
 */
public class AnimateObjectEffect extends TokenEffectBase {

    private static final StickerKind[] OPTIONAL = {StickerKind.NAME, StickerKind.ART, StickerKind.ABILITY};

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getActivatingPlayer() + " animates an inanimate object.";
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Player you = sa.getActivatingPlayer();
        final Game game = you.getGame();
        if (sa.hasParam("Tickets")) {
            final GameEntityCounterTable table = new GameEntityCounterTable();
            you.addCounter(CounterEnumType.TICKET, Integer.parseInt(sa.getParam("Tickets")), you, table);
            table.replaceCounterEffect(game, sa);
        }
        final Card object = TokenInfo.getProtoType(sa.getParam("TokenScript"), sa, you, false);
        object.getCurrentState().setName("");
        game.getGameLog().add(GameLogEntryType.INFORMATION, you + " chooses an inanimate object from outside the game.");

        stick(sa, you, object, StickerKind.PT, false);
        for (final StickerKind kind : OPTIONAL) {
            stick(sa, you, object, kind, true);
        }

        final TokenCreateTable tokenTable = new TokenCreateTable();
        tokenTable.put(you, object, 1);
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

    private static void stick(final SpellAbility sa, final Player you, final Card object, final StickerKind kind,
            final boolean optional) {
        final List<Sticker> options = Lists.newArrayList();
        for (final Sticker s : StickerSheet.getAvailableStickers(you)) {
            if (s.getKind() == kind) {
                options.add(s);
            }
        }
        if (options.isEmpty()) {
            return;
        }
        final Sticker chosen = you.getController().chooseSticker(options, object, sa, optional);
        if (chosen == null) {
            return;
        }
        if (chosen.getTickets() > 0) {
            you.subtractCounter(CounterEnumType.TICKET, chosen.getTickets(), you);
        }
        final int position = kind == StickerKind.NAME ? you.getController().chooseStickerNamePosition(chosen, object) : 0;
        object.addSticker(new AppliedSticker(chosen, you.getGame().getNextTimestamp(), position));
    }
}
