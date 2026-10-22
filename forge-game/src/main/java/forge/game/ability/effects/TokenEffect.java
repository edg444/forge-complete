/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;

import forge.util.Lang;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.event.GameEventCombatChanged;
import forge.game.event.GameEventTokenCreated;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class TokenEffect extends TokenEffectBase {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa.hasParam("SpellDescription")) {
            final Card host = sa.getHostCard();
            String desc = sa.getParam("SpellDescription");
            List<String> words = Arrays.asList(desc.split(" "));
            final List<Player> creators = getDefinedPlayersOrTargeted(sa, "TokenOwner");
            if (!words.get(0).equalsIgnoreCase("Create") && desc.contains(" create")) {
                String[] parts = desc.split(" create", 2);
                desc = parts[0] + " " + Lang.joinHomogenous(creators) + " create" + parts[1];
                if (creators.size() == 1) {
                    desc = desc.replaceAll(" create ", " creates ");
                }
                if (desc.contains("you")) {
                    desc = desc.replaceAll("you", sa.getActivatingPlayer().getName());
                }
            } else if (StringUtils.containsIgnoreCase(desc,"Create")) {
                String verb = creators.size() == 1 ? "creates" : "create";
                String start = Lang.joinHomogenous(creators) + " " + verb;
                String create = desc.contains("Create") ? "Create" : "create";
                desc = desc.replaceFirst(".*" + create, "");
                desc = start + desc;
                //try to put the right amount of tokens for X calculations and the like
                if (sa.hasParam("TokenAmount") && !StringUtils.isNumeric(sa.getParam("TokenAmount"))) {
                    final int numTokens = AbilityUtils.calculateAmount(host, sa.getParam("TokenAmount"), sa);
                    if (numTokens != 0) { //0 probably means calculation isn't ready in time for stack
                        if (numTokens != 1) { //if we are making more than one, substitute the numeral for a/an
                            String numeral = " " + Lang.getNumeral(numTokens) + " ";
                            List<String> words2 = Arrays.asList(desc.split(" "));
                            String target = " " + words2.get(words2.indexOf(verb) + 1) + " ";
                            desc = desc.replaceFirst(target, numeral);
                        }
                        //try to cut out unneeded description, which would now be confusing
                        String truncate = null;
                        if (desc.contains(", where")) {
                            truncate = ", where";
                        } else if (desc.contains(" for each")) {
                            truncate = " for each";
                        }
                        if (truncate != null) { //if we do truncate, make sure the string ends properly
                            desc = desc.split(truncate)[0];
                            if (desc.endsWith("token") && numTokens > 1) {
                                desc = desc + "s.";
                            } else {
                                desc = desc + ".";
                            }
                        }
                    }
                }
                //pronoun replacement for things that create an amount based on what you control
                desc = desc.replace("you control","they control");
            }
            return desc;
        }
        return sa.getDescription();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        // linked Abilities, if it needs chosen values, but nothing is chosen, no token can be created
        if (sa.hasParam("TokenTypes")) {
            if (sa.getParam("TokenTypes").contains("ChosenType") && !host.hasChosenType()) {
                return;
            }
        }
        if (sa.hasParam("TokenColors")) {
            if (sa.getParam("TokenColors").contains("ChosenColor") && !host.hasChosenColor()) {
                return;
            }
        }

        MutableBoolean combatChanged = new MutableBoolean(false);

        boolean useZoneTable = true;
        CardZoneTable triggerList = sa.getChangeZoneTable();
        if (triggerList == null) {
            triggerList = new CardZoneTable();
            useZoneTable = false;
        }
        if (sa.hasParam("ChangeZoneTable")) {
            sa.setChangeZoneTable(triggerList);
            useZoneTable = true;
        }

        int amount = finiteAmount(sa, AbilityUtils.calculateAmount(host, sa.getParamOrDefault("TokenAmount", "1"), sa), "tokens");
        if (amount < 1) {
            return;
        }

        String[] scripts = sa.getParam("TokenScript").split(",");
        if ("FromRandomCard".equals(sa.getParam("TokenScript"))) {
            final String script = randomCardToken(sa);
            if (script == null) {
                return;
            }
            scripts = new String[] {script};
        }
        makeTokenTable(getDefinedPlayersOrTargeted(sa, "TokenOwner"), scripts,
                amount, false, triggerList, combatChanged, sa);

        if (!useZoneTable) {
            triggerList.triggerChangesZoneAll(game, sa);
            triggerList.clear();
        }

        game.fireEvent(new GameEventTokenCreated());

        if (combatChanged.isTrue()) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
    }

    private static final java.util.regex.Pattern TOKEN_SCRIPT = java.util.regex.Pattern.compile("TokenScript\\$ ([^|]+)");

    /**
     * The Traveling Postman: cards at random until one creates a token other than a copy (a token script it names), then
     * the token of the activator's choice among the ones that card can create.
     */
    private static String randomCardToken(final SpellAbility sa) {
        final forge.token.TokenDb tokens = forge.StaticData.instance().getAllTokens();
        final List<forge.item.PaperCard> pool = new java.util.ArrayList<>(forge.StaticData.instance().getCommonCards().getUniqueCards());
        java.util.Collections.shuffle(pool, forge.util.MyRandom.getRandom());
        for (final forge.item.PaperCard pc : pool) {
            final java.util.Map<String, String> found = new java.util.LinkedHashMap<>();
            for (final forge.card.ICardFace face : pc.getRules().getAllFaces()) {
                if (face == null) {
                    continue;
                }
                final List<String> texts = new java.util.ArrayList<>();
                face.getAbilities().forEach(texts::add);
                face.getTriggers().forEach(texts::add);
                face.getStaticAbilities().forEach(texts::add);
                face.getReplacements().forEach(texts::add);
                face.getVariables().forEach(v -> texts.add(v.getValue()));
                for (final String text : texts) {
                    final java.util.regex.Matcher m = TOKEN_SCRIPT.matcher(text);
                    while (m.find()) {
                        for (final String script : m.group(1).trim().split(",")) {
                            if (tokens.containsRule(script.trim())) {
                                found.put(tokens.getRules().get(script.trim()).getName() + " (" + script.trim() + ")", script.trim());
                            }
                        }
                    }
                }
            }
            if (found.isEmpty()) {
                continue;
            }
            final Player p = sa.getActivatingPlayer();
            p.getGame().getAction().notifyOfValue(sa, p, "The random card is " + pc.getName() + ".", null);
            final List<String> labels = new java.util.ArrayList<>(found.keySet());
            final String label = labels.size() == 1 || p.getController().isAI() ? forge.util.Aggregates.random(labels)
                    : p.getController().chooseSomeType("token", sa, labels);
            return label == null ? null : found.get(label);
        }
        return null;
    }
}
