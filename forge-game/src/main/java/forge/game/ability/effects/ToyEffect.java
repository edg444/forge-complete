package forge.game.ability.effects;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.card.token.TokenInfo;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * Applejack's Family Gathering: one of the activator's real toys goes onto the battlefield as a 2/2 creature token with
 * the toy's name, colors and creature types; flying if it has wings, scry 2 if it has a horn, a Food if neither. The
 * toys are listed once per match as the game begins (user ruling), and a toy whose token is still on the battlefield
 * can't be put there again. The AI owns a fixed random set of My Little Pony toys.
 */
public class ToyEffect extends TokenEffectBase {

    private static final String NOTE = "ToyBox";
    private static final String TOKEN_TOY = "ToyName";

    // name|colors|creature types|wings|horn
    private static final List<String> AI_TOYS = List.of(
            "Twilight Sparkle|U|Alicorn Pony|true|true",
            "Applejack|G|Pony|false|false",
            "Fluttershy|W|Pegasus Pony|true|false",
            "Pinkie Pie|R|Pony|false|false",
            "Rainbow Dash|U|Pegasus Pony|true|false",
            "Rarity|W|Unicorn Pony|false|true",
            "Princess Celestia|W|Alicorn Pony|true|true",
            "Spike|G|Dragon|false|false");

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return "Put a toy you own onto the battlefield as a 2/2 creature token with that toy's name, colors, and creature types.";
    }

    /** The toys this player listed as the game began, filling the box the first time it's needed. */
    @SuppressWarnings("unchecked")
    public static List<String> toyBox(final Player owner, final Card source) {
        if (owner.getMatchNote(NOTE) instanceof List<?> box) {
            return (List<String>) box;
        }
        final List<String> box = Lists.newArrayList();
        if (owner.getController().isAI()) {
            final List<String> stock = Lists.newArrayList(AI_TOYS);
            java.util.Collections.shuffle(stock, new Random(MyRandom.getRandom().nextLong()));
            box.addAll(stock.subList(0, 4));
        } else {
            final SpellAbility sa = new SpellAbility.EmptySa(source, owner);
            while (box.size() < 30 && owner.getController().confirmAction(sa, null, box.isEmpty()
                    ? source.getTranslatedName() + ": list the toys you own. Add a toy?"
                    : "Add another toy? (" + box.size() + " listed)", null)) {
                final String name = StringUtils.trimToEmpty(owner.getController().guessString(sa, "The toy's name:")).replace("|", "/");
                if (name.isEmpty()) {
                    continue;
                }
                final StringBuilder colors = new StringBuilder();
                for (final MagicColor.Color c : owner.getController().chooseColors("Choose each of " + name + "'s colors (none for colorless)",
                        sa, 0, 5, ColorSet.WUBRG)) {
                    colors.append(c.getShortName());
                }
                final List<String> types = Lists.newArrayList();
                for (final String t : StringUtils.trimToEmpty(owner.getController().guessString(sa,
                        name + "'s creature types, separated by spaces (blank for none):")).split("\\s+")) {
                    final String type = StringUtils.capitalize(t.toLowerCase());
                    if (CardType.isACreatureType(type) && !types.contains(type)) {
                        types.add(type);
                    }
                }
                final boolean wings = owner.getController().confirmAction(sa, null, "Does " + name + " have wings?", null);
                final boolean horn = owner.getController().confirmAction(sa, null, "Does " + name + " have a horn?", null);
                box.add(name + "|" + colors + "|" + String.join(" ", types) + "|" + wings + "|" + horn);
            }
        }
        owner.setMatchNote(NOTE, box);
        final List<String> names = Lists.newArrayList();
        for (final String toy : box) {
            names.add(toy.split("\\|", -1)[0]);
        }
        owner.getGame().getGameLog().add(GameLogEntryType.INFORMATION, owner + "'s toys: "
                + (names.isEmpty() ? "none" : String.join(", ", names)) + ".");
        return box;
    }

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = p.getGame();
        final List<String> free = Lists.newArrayList();
        for (final String toy : toyBox(p, host)) {
            final String name = toy.split("\\|", -1)[0];
            if (!p.getCardsIn(ZoneType.Battlefield).anyMatch(c -> name.equals(c.getSVar(TOKEN_TOY)))) {
                free.add(toy);
            }
        }
        if (free.isEmpty()) {
            game.getGameLog().add(GameLogEntryType.INFORMATION, p + " has no toy left to put onto the battlefield.");
            return;
        }
        String toy = free.get(0);
        if (!p.getController().isAI() && free.size() > 1) {
            final List<String> names = Lists.newArrayList();
            for (final String t : free) {
                names.add(t.split("\\|", -1)[0]);
            }
            toy = free.get(Math.max(0, names.indexOf(p.getController().chooseSomeType("toy", sa, names, false))));
        }
        final String[] f = toy.split("\\|", -1);
        final boolean wings = Boolean.parseBoolean(f[3]);
        final boolean horn = Boolean.parseBoolean(f[4]);

        final Card result = TokenInfo.getProtoType("c_2_2_nameless", sa, p, false);
        result.setName(f[0]);
        result.setColor(f[1].isEmpty() ? ColorSet.C : ColorSet.fromNames(f[1].toCharArray()));
        result.setCreatureTypes(f[2].isEmpty() ? Lists.newArrayList() : Lists.newArrayList(f[2].split(" ")));
        if (wings) {
            result.getCurrentState().addIntrinsicKeyword("Flying", true);
        }
        result.setSVar(TOKEN_TOY, f[0]);
        result.setTokenSpawningAbility(sa);
        final CardZoneTable triggerList = new CardZoneTable();
        final MutableBoolean combatChanged = new MutableBoolean(false);
        makeTokenTable(makeTokenTableInternal(p, result, 1), false, triggerList, combatChanged, sa);
        triggerList.triggerChangesZoneAll(game, sa);
        game.fireEvent(new GameEventTokenCreated());
        if (combatChanged.isTrue()) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }

        final String follow = horn ? "DB$ Scry | ScryNum$ 2" : wings ? null : "DB$ Token | TokenScript$ c_a_food_sac";
        if (follow != null) {
            final SpellAbility then = AbilityFactory.getAbility(follow, host);
            then.setActivatingPlayer(p);
            AbilityUtils.resolve(then);
        }
    }
}
