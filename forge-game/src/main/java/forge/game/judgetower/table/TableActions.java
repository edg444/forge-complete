package forge.game.judgetower.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import forge.card.CardStateName;
import forge.card.GamePieceType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.event.GameEventCardAttachment;
import forge.game.event.GameEventCardCounters;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventCardTapped;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventShuffle;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.player.Player;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * The primitives: all a human can do to the table, and all that ever changes it. They're physical, with no rules and no
 * triggers, so everything goes around GameAction; they tell the board views what changed themselves.
 */
public final class TableActions {
    private TableActions() { }

    private static void log(final Game game, final String message) {
        game.getGameLog().add(GameLogEntryType.INFORMATION, message);
    }

    /** @param zoneOwner whose hand or battlefield it is; for the zones that aren't anyone's own, it doesn't matter */
    public static void move(final Game game, final Card c, final ZoneType to, final Player zoneOwner, final Integer index) {
        final Zone from = c.getZone();
        if (from != null && from.getZoneType() == ZoneType.Stack && to != ZoneType.Stack) {
            removeSpellOf(game, c);
        }
        if (from != null) {
            from.remove(c);
        }
        final Combat combat = game.getPhaseHandler().getCombat();
        if (combat != null && to != ZoneType.Battlefield) {
            combat.removeFromCombat(c);
            game.updateCombatForView();
        }
        if (to != ZoneType.Battlefield) {
            detach(game, c);
            for (final Card attached : new CardCollection(c.getAttachedCards())) {
                detach(game, attached);
            }
            c.setTapped(false);
            c.setPhasedOut(null);
            c.setDamage(0);
            c.setCounters(com.google.common.collect.HashMultiset.create());
            if (c.isFaceDown()) {
                c.forceTurnFaceUp();
            }
            if (c.getCurrentStateName() != CardStateName.Original && c.hasState(CardStateName.Original)) {
                c.setState(CardStateName.Original, true);
            }
        }
        final Player owner = zoneOwner != null ? zoneOwner : c.getOwner();
        if (to == ZoneType.Stack) {
            game.getStackZone().add(c, index);
        } else {
            owner.getZone(to).add(c, index);
        }
        if (to == ZoneType.Battlefield) {
            setController(c, owner);
        }
        c.updateStateForView();
        if (to != ZoneType.Stack) {
            for (final Player p : game.getRegisteredPlayers()) {
                p.updateZoneForView(p.getZone(to));
            }
        }
        // the zone event has the views catch up with the card in its new place
        game.fireEvent(new GameEventCardStatsChanged(c));
    }

    /** Every player can see into every hand and so on, when the format says so: nothing on the table applies statics. */
    public static void reveal(final Game game, final Card c) {
        for (final Player p : game.getRegisteredPlayers()) {
            c.addMayLookTemp(p);
        }
        c.updateStateForView();
    }

    public static void setTapped(final Game game, final Card c, final boolean tapped) {
        c.setTapped(tapped);
        game.fireEvent(new GameEventCardTapped(c, tapped));
    }

    public static void setPhasedOut(final Game game, final Card c, final boolean out) {
        c.setPhasedOut(out ? c.getController() : null);
        c.updateStateForView();
        game.fireEvent(new GameEventCardStatsChanged(c));
    }

    /** Turns a card face down or up, or shows its other face, whichever the player said. */
    public static void setFaceDown(final Game game, final Card c, final boolean down) {
        if (down) {
            c.turnFaceDown(true);
        } else {
            c.forceTurnFaceUp();
        }
        c.updateStateForView();
        game.fireEvent(new GameEventCardStatsChanged(c));
    }

    public static void showState(final Game game, final Card c, final CardStateName state) {
        c.setState(state, true);
        c.updateStateForView();
        game.fireEvent(new GameEventCardStatsChanged(c));
    }

    public static void setCounters(final Game game, final Card c, final CounterType type, final int amount) {
        final int old = c.getCounters(type);
        c.setCounters(type, Math.max(0, amount));
        game.fireEvent(new GameEventCardCounters(c, type, old, Math.max(0, amount)));
        game.fireEvent(new GameEventCardStatsChanged(c));
    }

    public static void setDamage(final Game game, final Card c, final int damage) {
        c.setDamage(Math.max(0, damage));
        game.fireEvent(new GameEventCardStatsChanged(c));
    }

    public static void attach(final Game game, final Card c, final GameEntity to) {
        detach(game, c);
        c.setEntityAttachedTo(to);
        to.addAttachedCard(c);
        game.fireEvent(new GameEventCardAttachment(c, null, to));
    }

    public static void detach(final Game game, final Card c) {
        final GameEntity on = c.getEntityAttachedTo();
        if (on != null) {
            on.removeAttachedCard(c);
            c.setEntityAttachedTo(null);
            game.fireEvent(new GameEventCardAttachment(c, on, null));
        }
    }

    public static void setController(final Card c, final Player player) {
        c.clearTempControllers();
        if (!c.getOwner().equals(player)) {
            c.addTempController(player, c.getGame().getNextTimestamp());
        }
    }

    /** Hands a permanent over: it moves to the other player's side. */
    public static void giveControl(final Game game, final Card c, final Player player) {
        move(game, c, ZoneType.Battlefield, player, null);
    }

    public static void setLife(final Game game, final Player p, final int life) {
        p.setLife(life, null);
        log(game, p.getName() + "'s life is now " + life + ".");
    }

    public static void setCounters(final Game game, final Player p, final CounterType type, final int amount) {
        p.setCounters(type, Math.max(0, amount), null, true);
        log(game, p.getName() + " has " + Math.max(0, amount) + " " + type + " counters.");
    }

    public static void setPoison(final Game game, final Player p, final int amount) {
        setCounters(game, p, CounterEnumType.POISON, amount);
    }

    public static void shuffleTower(final Game game, final Player p) {
        final Zone tower = p.getZone(ZoneType.Library);
        final List<Card> cards = new ArrayList<>(tower.getCards());
        Collections.shuffle(cards, MyRandom.getRandom());
        tower.setCards(cards);
        game.fireEvent(new GameEventShuffle(p));
        log(game, p.getName() + " shuffled the Tower.");
    }

    public static void setTowerOrder(final Game game, final Player p, final List<Card> order) {
        p.getZone(ZoneType.Library).setCards(order);
    }

    /** A token only the table knows of until the judge has one to pair it with. */
    public static Card newToken(final Game game, final Player controller, final String name, final String imageKey,
            final String types, final int power, final int toughness) {
        final Card c = new Card(game.nextCardId(), game);
        c.setName(name);
        if (imageKey != null) {
            c.setImageKey(imageKey);
        }
        c.setOwner(controller);
        c.setGamePieceType(GamePieceType.TOKEN);
        c.getCurrentState().setType(forge.card.CardType.parse(types, true));
        c.setBasePower(power);
        c.setBaseToughness(toughness);
        c.setGameTimestamp(game.getNextTimestamp());
        c.updateStateForView();
        move(game, c, ZoneType.Battlefield, controller, null);
        return c;
    }

    public static TableStackItem pushStack(final Game game, final Card source, final Player controller, final boolean spell, final String text,
            final List<?> targets) {
        final TableStackItem item = new TableStackItem(source, controller, spell, text);
        for (final Object t : targets) {
            item.getTargets().add((forge.game.GameObject) t);
        }
        game.getStack().pushForTable(new SpellAbilityStackInstance(item));
        game.updateStackForView();
        // the views have no event of their own for this: this one has them look at the stack again
        game.fireEvent(new GameEventSpellRemovedFromStack(item.getView()));
        return item;
    }

    public static void removeFromStack(final Game game, final SpellAbilityStackInstance si) {
        game.getStack().remove(si);
        game.updateStackForView();
        game.fireEvent(new GameEventSpellRemovedFromStack(si.getSpellAbility().getView()));
    }

    private static void removeSpellOf(final Game game, final Card c) {
        for (final SpellAbilityStackInstance si : new ArrayList<>(game.getStack().asList())) {
            if (si.getSpellAbility().isSpell() && si.getSourceCard().getId() == c.getId()) {
                removeFromStack(game, si);
            }
        }
    }

    /** The table's combat marks. There's no combat to mark until someone starts one, as there's no rule to do it. */
    public static Combat combat(final Game game) {
        Combat combat = game.getPhaseHandler().getCombat();
        if (combat == null) {
            combat = new Combat(game.getPhaseHandler().getPlayerTurn());
            game.getPhaseHandler().setCombat(combat);
        }
        return combat;
    }

    public static void declareAttacker(final Game game, final Card c, final GameEntity defender) {
        final Combat combat = combat(game);
        combat.removeFromCombat(c);
        combat.addAttacker(c, defender);
        game.updateCombatForView();
        game.fireEvent(new GameEventCombatChanged());
    }

    public static void declareBlocker(final Game game, final Card blocker, final Card attacker) {
        final Combat combat = combat(game);
        combat.addBlocker(attacker, blocker);
        game.updateCombatForView();
        game.fireEvent(new GameEventCombatChanged());
    }

    public static void removeFromCombat(final Game game, final Card c) {
        final Combat combat = game.getPhaseHandler().getCombat();
        if (combat != null) {
            combat.undoBlockingAssignment(c);
            combat.removeFromCombat(c);
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    }

    public static void note(final Game game, final Player p, final String what) {
        log(game, p.getName() + ": " + what);
    }
}
