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
package forge.deck.generation;

import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.*;
import forge.card.mana.ManaCost;
import forge.deck.CardPool;
import forge.deck.DeckFormat;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Generate2ColorDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id: Generate2ColorDeck.java 14959 2012-03-28 14:03:43Z Chris H. $
 */
public abstract class DeckGeneratorBase {
    protected final DebugTrace trace = new DebugTrace();
    protected final Map<String, Integer> cardCounts = new HashMap<>();
    protected int maxDuplicates = 4;
    protected boolean useArtifacts = true;
    protected String basicLandEdition = null;
    protected List<String> inverseDLands = new ArrayList<>();
    protected List<String> dLands = new ArrayList<>();

    protected ColorSet colors;
    protected final CardPool tDeck = new CardPool();
    /** Cards whose print was picked on purpose, for Deck.optimiseCardArt to leave alone. */
    protected final Set<String> pinnedPrintings = new HashSet<>();
    protected final IDeckGenPool pool;
    protected IDeckGenPool landPool;
    protected final DeckFormat format;
    protected final IDeckGenPool fullCardDB;

    protected abstract float getLandPercentage();
    protected abstract float getCreaturePercentage();
    protected abstract float getSpellPercentage();

    public DeckGeneratorBase(IDeckGenPool pool0, DeckFormat format0, Predicate<PaperCard> formatFilter0) {
        pool = new DeckGenPool(format0.getCardPool(pool0).getAllCards(formatFilter0));
        format = format0;
        fullCardDB = pool0;
    }

    public DeckGeneratorBase(IDeckGenPool pool0, DeckFormat format0) {
        pool = new DeckGenPool(format0.getCardPool(pool0).getAllCards());
        format = format0;
        fullCardDB = pool0;
    }

    public Set<String> getPinnedPrintings() {
        return pinnedPrintings;
    }

    public void setSingleton(boolean singleton) {
        maxDuplicates = singleton ? 1 : 4;
    }
    public void setUseArtifacts(boolean value) {
        useArtifacts = value;
    }

    protected void addCreaturesAndSpells(int size, List<ImmutablePair<FilterCMC, Integer>> cmcLevels, boolean forAi) {
        trace.append("Building deck of ").append(size).append("cards\n");

        final Iterable<PaperCard> cards = selectCardsOfMatchingColorForPlayer(forAi);
        // build subsets based on type

        final Iterable<PaperCard> creatures = IterableUtil.filter(cards, PaperCardPredicates.IS_CREATURE);
        final int creatCnt = (int) Math.ceil(getCreaturePercentage() * size);
        trace.append("Creatures to add:").append(creatCnt).append("\n");
        addCmcAdjusted(creatures, creatCnt, cmcLevels);

        Predicate<PaperCard> preSpells = PaperCardPredicates.fromRules(CardRulesPredicates.IS_NON_CREATURE_SPELL);
        final Iterable<PaperCard> spells = IterableUtil.filter(cards, preSpells);
        final int spellCnt = (int) Math.ceil(getSpellPercentage() * size);
        trace.append("Spells to add:").append(spellCnt).append("\n");
        addCmcAdjusted(spells, spellCnt, cmcLevels);

        trace.append(String.format("Current deck size: %d... should be %f%n", tDeck.countAll(), size * (getCreaturePercentage() + getSpellPercentage())));

        reinforceTypalPayoffs(size, forAi);
        reinforceBorderPayoffs(size, forAi);
    }

    /**
     * A random pile is fine, a random pile with a Zombie lord and no other Zombies is not. Find the
     * creature type this deck's card text actually cares about and, if the deck is short on bodies of
     * that type, trade some filler for them. Only the single strongest theme is reinforced, so a
     * multicolor pile stays a pile rather than turning into three half-tribes.
     */
    protected void reinforceTypalPayoffs(int size, boolean forAi) {
        final Map<String, Integer> payoffWeight = new HashMap<>();
        final Set<String> payoffNames = new HashSet<>();
        for (Entry<PaperCard, Integer> e : tDeck) {
            for (String type : referencedCreatureTypes(e.getKey())) {
                increment(payoffWeight, type, e.getValue());
                payoffNames.add(e.getKey().getName());
            }
        }
        if (payoffWeight.isEmpty()) {
            return;
        }

        String theme = null;
        int best = 0;
        for (Entry<String, Integer> e : payoffWeight.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                theme = e.getKey();
            }
        }

        int have = 0;
        for (Entry<PaperCard, Integer> e : tDeck) {
            if (e.getKey().getRules().getType().hasCreatureType(theme)) {
                have += e.getValue();
            }
        }

        // roughly a tenth of the deck, so 60-card decks aim for six bodies
        final int want = Math.max(4, size / 10);
        if (have >= want) {
            return;
        }

        final String wanted = theme;
        final List<PaperCard> candidates = Lists.newArrayList(IterableUtil.filter(
                selectCardsOfMatchingColorForPlayer(forAi),
                c -> c.getRules().getType().isCreature() && c.getRules().getType().hasCreatureType(wanted)
                        && !c.getRules().getType().hasAllCreatureTypes()));
        if (candidates.isEmpty()) {
            return;
        }

        trace.append("Reinforcing ").append(wanted).append(": have ").append(have)
                .append(", want ").append(want).append("\n");

        int added = 0;
        for (int attempt = 0; added < want - have && attempt < candidates.size() * 4; attempt++) {
            final PaperCard cp = candidates.get(MyRandom.getRandom().nextInt(candidates.size()));
            if (!addOneCopy(cp) || !removeOneFiller(c -> payoffNames.contains(c.getName())
                    || c.getRules().getType().hasCreatureType(wanted))) {
                continue;
            }
            added++;
        }
        trace.append("Reinforced with ").append(added).append(" ").append(wanted).append("(s)\n");
    }

    private static final Map<CardEdition.BorderColor, List<String>> BORDER_PAYOFF_PROPERTIES = Map.of(
            CardEdition.BorderColor.WHITE, List.of("WhiteBordered", "BorderColorWhite"),
            CardEdition.BorderColor.SILVER, List.of("BorderColorSilver"),
            CardEdition.BorderColor.BLACK, List.of("BorderColorBlack"));

    /**
     * The Whiteboarder pumps white-bordered creatures, Border Guardian rewards casting spells of a border. Neither
     * does anything in a pile of whatever printings came up, so when the deck has one, move its other cards to
     * printings with that border where they exist and, if it's still short, trade filler for cards that have one.
     */
    protected void reinforceBorderPayoffs(int size, boolean forAi) {
        final Map<CardEdition.BorderColor, Integer> payoffWeight = new EnumMap<>(CardEdition.BorderColor.class);
        final Set<String> payoffNames = new HashSet<>();
        boolean creaturesOnly = true;
        for (Entry<PaperCard, Integer> e : tDeck) {
            final String script = scriptText(e.getKey().getRules());
            for (Entry<CardEdition.BorderColor, List<String>> b : BORDER_PAYOFF_PROPERTIES.entrySet()) {
                for (String property : b.getValue()) {
                    if (script.contains(property)) {
                        increment(payoffWeight, b.getKey(), e.getValue());
                        payoffNames.add(e.getKey().getName());
                        creaturesOnly &= script.contains("Creature." + property);
                        break;
                    }
                }
            }
        }
        if (payoffWeight.isEmpty()) {
            return;
        }
        // a card that rewards every border (Border Guardian) leans white, the one most printings don't have
        CardEdition.BorderColor wanted = null;
        for (CardEdition.BorderColor b : List.of(CardEdition.BorderColor.WHITE, CardEdition.BorderColor.SILVER,
                CardEdition.BorderColor.BLACK)) {
            if (payoffWeight.containsKey(b) && (wanted == null || payoffWeight.get(b) > payoffWeight.get(wanted))) {
                wanted = b;
            }
        }
        final CardEdition.BorderColor border = wanted;
        final boolean onlyCreatures = creaturesOnly;
        final Predicate<PaperCard> counts = c -> !payoffNames.contains(c.getName())
                && (onlyCreatures ? c.getRules().getType().isCreature() : !c.getRules().getType().isLand());

        // the pool keeps one preferred print per name, so other borders are only in the full database
        final Map<String, List<PaperCard>> reprints = new HashMap<>();
        for (PaperCard pc : fullCardDB.getAllCards(pc -> printedBorder(pc) == border)) {
            reprints.computeIfAbsent(pc.getName(), k -> new ArrayList<>()).add(pc);
        }
        int have = 0;
        for (Entry<PaperCard, Integer> e : Lists.newArrayList(tDeck)) {
            final PaperCard pc = e.getKey();
            if (!counts.test(pc)) {
                continue;
            }
            final List<PaperCard> options = reprints.get(pc.getName());
            if (printedBorder(pc) != border && options != null) {
                tDeck.remove(pc, e.getValue());
                tDeck.add(options.get(MyRandom.getRandom().nextInt(options.size())), e.getValue());
                trace.append("Reprinted ").append(pc.getName()).append(" with a ").append(border).append(" border\n");
            }
            if (printedBorder(pc) == border || options != null) {
                have += e.getValue();
                pinnedPrintings.add(pc.getName());
            }
        }

        final int want = Math.max(4, size / 10);
        if (have >= want) {
            return;
        }
        trace.append("Reinforcing ").append(border).append(" borders: have ").append(have).append(", want ").append(want).append("\n");
        final List<PaperCard> candidates = Lists.newArrayList(IterableUtil.filter(
                selectCardsOfMatchingColorForPlayer(forAi), c -> counts.test(c) && reprints.containsKey(c.getName())));
        if (candidates.isEmpty()) {
            return;
        }
        int added = 0;
        for (int attempt = 0; added < want - have && attempt < candidates.size() * 4; attempt++) {
            final String name = candidates.get(MyRandom.getRandom().nextInt(candidates.size())).getName();
            final int newCount = cardCounts.getOrDefault(name, 0) + 1;
            if (newCount > maxDuplicates
                    || !removeOneFiller(c -> payoffNames.contains(c.getName()) || printedBorder(c) == border)) {
                continue;
            }
            final List<PaperCard> options = reprints.get(name);
            tDeck.add(options.get(MyRandom.getRandom().nextInt(options.size())));
            cardCounts.put(name, newCount);
            pinnedPrintings.add(name);
            added++;
        }
        trace.append("Reinforced with ").append(added).append(" ").append(border).append("-bordered card(s)\n");
    }

    private static String scriptText(CardRules rules) {
        final StringBuilder sb = new StringBuilder();
        for (ICardFace face : rules.getAllFaces()) {
            if (face == null) {
                continue;
            }
            for (Iterable<String> lines : List.of(face.getStaticAbilities(), face.getTriggers(), face.getAbilities(),
                    face.getReplacements())) {
                for (String line : lines) {
                    sb.append(line).append('\n');
                }
            }
            for (Entry<String, String> v : face.getVariables()) {
                sb.append(v.getValue()).append('\n');
            }
        }
        return sb.toString();
    }

    /** The border printed on this printing, as Card.printedBorderColor works it out in a game. */
    private static CardEdition.BorderColor printedBorder(PaperCard pc) {
        final PrintingTraits.Traits t = PrintingTraits.get(pc.getEdition(), pc.getCollectorNumber(), false);
        if (t != null && t.getBorder() != null) {
            return t.getBorder();
        }
        final CardEdition edition = StaticData.instance().getEditions().get(pc.getEdition());
        return edition == null ? CardEdition.BorderColor.BLACK : edition.getBorderColor(pc.getCollectorNumber());
    }

    /** Creature types this card's rules text singles out, ignoring its own type line and name. */
    private static Set<String> referencedCreatureTypes(PaperCard card) {
        final CardRules rules = card.getRules();
        String text = rules.getOracleText();
        if (StringUtils.isBlank(text)) {
            return Collections.emptySet();
        }
        // the card's own name would otherwise read as a reference ("Zombie Master" mentioning Zombie)
        text = text.replace(card.getName(), "");

        final Set<String> found = new HashSet<>();
        final Matcher m = TYPE_WORD.matcher(text);
        while (m.find()) {
            String word = m.group(1);
            if (!CardType.isACreatureType(word) && word.endsWith("s")) {
                word = word.substring(0, word.length() - 1);
            }
            if (CardType.isACreatureType(word) && !rules.getType().hasAllCreatureTypes()) {
                found.add(word);
            }
        }
        return found;
    }

    private static final Pattern TYPE_WORD = Pattern.compile("\\b([A-Z][a-z]+)\\b");

    private boolean addOneCopy(PaperCard cp) {
        final int newCount = cardCounts.getOrDefault(cp.getName(), 0) + 1;
        if (newCount > maxDuplicates) {
            return false;
        }
        tDeck.add(pool.getCard(cp.getName(), cp.getEdition()));
        cardCounts.put(cp.getName(), newCount);
        return true;
    }

    /** Drop one random card that is neither a land nor one to keep (a payoff, or part of the theme we're building). */
    private boolean removeOneFiller(Predicate<PaperCard> keep) {
        final List<PaperCard> candidates = Lists.newArrayList();
        for (PaperCard c : tDeck.toFlatList()) {
            if (c.getRules().getType().isLand() || keep.test(c)) {
                continue;
            }
            candidates.add(c);
        }
        if (candidates.isEmpty()) {
            return false;
        }
        final PaperCard victim = candidates.get(MyRandom.getRandom().nextInt(candidates.size()));
        tDeck.remove(victim);
        cardCounts.computeIfPresent(victim.getName(), (k, v) -> v - 1);
        return true;
    }

    public CardPool getDeck(final int size, final boolean forAi) {
        return null; // all but theme deck do override this method
    }

    protected boolean setBasicLandPool(String edition){
        Predicate<PaperCard> isSetBasicLand;
        if (edition != null){
            isSetBasicLand = PaperCardPredicates.printedInSet(edition)
                    .and(PaperCardPredicates.IS_BASIC_LAND);
        } else {
            isSetBasicLand = PaperCardPredicates.IS_BASIC_LAND;
        }

        landPool = new DeckGenPool(StaticData.instance().getCommonCards().getAllCards(isSetBasicLand));
        return landPool.contains("Plains");
    }

    protected int addSome(int cnt, List<PaperCard> source) {
        int srcLen = source.size();
        if (srcLen == 0) { return 0; }

        int res = 0;
        while (res < cnt) {
            PaperCard cp = source.get(MyRandom.getRandom().nextInt(srcLen));
            // TODO AltName conversion needed?
            int newCount = cardCounts.get(cp.getName()) + 1;

            //add card to deck if not already maxed out on card
            if (newCount <= maxDuplicates) {
                tDeck.add(pool.getCard(cp.getName(), cp.getEdition()));
                //see if current card is from an edition with basic lands to use for this deck
                if(basicLandEdition == null){
                    if(setBasicLandPool(cp.getEdition())){
                        basicLandEdition = cp.getEdition();
                    }
                }
                cardCounts.put(cp.getName(), newCount);
                trace.append(String.format("(%d) %s [%s]%n", cp.getRules().getManaCost().getCMC(), cp.getName(), cp.getRules().getManaCost()));
                res++;
            }

            //remove card from source if now maxed out on card
            if (newCount >= maxDuplicates) {
                source.remove(cp);
                srcLen--;
                if (srcLen == 0) { break; }
            }
        }
        return res;
    }

    protected int addSomeStr(int cnt, List<String> source) {
        int srcLen = source.size();
        if (srcLen == 0) { return 0; }

        int res = 0;
        while (res < cnt) {
            String s = source.get(MyRandom.getRandom().nextInt(srcLen));
            int newCount = cardCounts.get(s) + 1;

            //add card to deck if not already maxed out on card
            if (newCount <= maxDuplicates) {
                tDeck.add(pool.getCard(s));
                cardCounts.put(s, newCount);
                trace.append(s + "\n");
                res++;
            }

            //remove card from source if now maxed out on card
            if (newCount >= maxDuplicates) {
                source.remove(s);
                srcLen--;
                if (srcLen == 0) { break; }
            }
        }
        return res;
    }

    protected void addBasicLand(int cnt) {
    	addBasicLand(cnt, null);
    }
    protected void addBasicLand(int cnt, String edition) {
        trace.append(cnt).append(" basic lands remain").append("\n");

        // attempt to optimize basic land counts according to colors of picked cards
        final Map<String, Integer> clrCnts = countLands(tDeck);

        if (cnt > 0 && clrCnts.isEmpty()) {
            // The deck is completely colorless. Add some Plains then to fill the deck.
            clrCnts.put(MagicColor.Constant.BASIC_LANDS.get(0), cnt);
        }

        // total of all ClrCnts
        float totalColor = 0;
        for (Entry<String, Integer> c : clrCnts.entrySet()) {
            totalColor += c.getValue();
            trace.append(c.getKey()).append(":").append(c.getValue()).append("\n");
        }

        trace.append("totalColor:").append(totalColor).append("\n");

        int landsLeft = cnt;
        for (Entry<String, Integer> c : clrCnts.entrySet()) {
            String basicLandName = c.getKey();

            // calculate number of lands for each color
            final int nLand = Math.min(landsLeft, Math.round(cnt * c.getValue() / totalColor));
            trace.append("nLand-").append(basicLandName).append(":").append(nLand).append("\n");

            // just to prevent a null exception by the deck size fixing code
            cardCounts.put(basicLandName, nLand);

            if (!landPool.contains("Plains")) {//in case none of the cards came from a set with all basic lands
                setBasicLandPool("BFZ");
                basicLandEdition="BFZ";
            }

            for (int i = 0; i < nLand; i++) {
                tDeck.add(landPool.getCard(basicLandName, edition != null ? edition : basicLandEdition), 1);
            }

            landsLeft -= nLand;
        }
    }

    protected void adjustDeckSize(int targetSize) {
        // fix under-sized or over-sized decks, due to integer arithmetic
        int actualSize = tDeck.countAll();
        if (actualSize < targetSize) {
            addSome(targetSize - actualSize, tDeck.toFlatList());
        }
        else if (actualSize > targetSize) {
            for (int i = 0; i < 3 && actualSize > targetSize; i++) {
                List<PaperCard> toRemove = tDeck.toFlatList().stream()
                        .filter(PaperCardPredicates.NOT_BASIC_LAND)
                        .collect(StreamUtil.random(actualSize - targetSize));
                tDeck.removeAllFlat(toRemove);

                for (PaperCard c : toRemove) {
                    trace.append("Removed:").append(c.getName()).append("\n");
                }
                actualSize = tDeck.countAll();
            }
        }
    }

    protected void addCmcAdjusted(Iterable<PaperCard> source, int cnt, List<ImmutablePair<FilterCMC, Integer>> cmcLevels) {
        int totalWeight = 0;
        for (ImmutablePair<FilterCMC, Integer> pair : cmcLevels) {
            totalWeight += pair.getRight();
        }

        float variability = 0.6f; // if set to 1, you'll get minimum cards to choose from
        float desiredWeight = (float)cnt / ( maxDuplicates * variability ); 
        float desiredOverTotal = desiredWeight / totalWeight;
        float requestedOverTotal = (float)cnt / totalWeight;

        for (ImmutablePair<FilterCMC, Integer> pair : cmcLevels) {
            Iterable<PaperCard> matchingCards = IterableUtil.filter(source, PaperCardPredicates.fromRules(pair.getLeft()));
            int cmcCountForPool = (int) Math.ceil(pair.getRight() * desiredOverTotal);

            int addOfThisCmc = Math.round(pair.getRight() * requestedOverTotal);
            trace.append(String.format("Adding %d cards for cmc range from a pool with %d cards:%n", addOfThisCmc, cmcCountForPool));

            final List<PaperCard> curved = Aggregates.random(matchingCards, cmcCountForPool);
            final List<PaperCard> curvedRandomized = Lists.newArrayList();
            for (PaperCard c : curved) {
                cardCounts.put(c.getName(), 0);
                curvedRandomized.add(pool.getCard(c.getName()));
            }

            addSome(addOfThisCmc, curvedRandomized);
        }
    }

    protected Iterable<PaperCard> selectCardsOfMatchingColorForPlayer(boolean forAi) {
        // start with all cards
        // remove cards that generated decks don't like
        Predicate<CardRules> canPlay = forAi ? AI_CAN_PLAY : CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS;
        Predicate<CardRules> hasColor = new MatchColorIdentity(colors);
        Predicate<CardRules> canUseInFormat = c -> !c.getAiHints().getRemNonCommanderDecks() || format.hasCommander();

        if (useArtifacts) {
            hasColor = hasColor.or(COLORLESS_CARDS.and(c -> meetsColorNeeds(c, colors)));
        }
        return IterableUtil.filter(pool.getAllCards(), PaperCardPredicates.fromRules(canPlay.and(hasColor).and(canUseInFormat)));
    }

    // A colorless card that needs a color (Kefnet's Monument: blue creatures) is dead weight in a deck without it.
    public static boolean meetsColorNeeds(CardRules c, ColorSet colors) {
        final DeckHints needs = c.getAiHints().getDeckNeeds();
        if (needs == null || !needs.isValid()) {
            return true;
        }
        final ColorSet needed = needs.getColors();
        return needed.isColorless() || needed.sharesColorWith(colors);
    }

    protected static Map<String, Integer> countLands(ItemPool<PaperCard> outList) {
        // attempt to optimize basic land counts according
        // to color representation
        Map<String, Integer> res = new TreeMap<>();
        // count each card color using mana costs
        // TODO: count hybrid mana differently?
        for (Entry<PaperCard, Integer> cpe : outList) {
            int profile = cpe.getKey().getRules().getManaCost().getColorProfile();

            if ((profile & MagicColor.WHITE) != 0) {
                increment(res, MagicColor.Constant.BASIC_LANDS.get(0), cpe.getValue());
            }
            else if ((profile & MagicColor.BLUE) != 0) {
                increment(res, MagicColor.Constant.BASIC_LANDS.get(1), cpe.getValue());
            }
            else if ((profile & MagicColor.BLACK) != 0) {
                increment(res, MagicColor.Constant.BASIC_LANDS.get(2), cpe.getValue());
            }
            else if ((profile & MagicColor.RED) != 0) {
                increment(res, MagicColor.Constant.BASIC_LANDS.get(3), cpe.getValue());
            }
            else if ((profile & MagicColor.GREEN) != 0) {
                increment(res, MagicColor.Constant.BASIC_LANDS.get(4), cpe.getValue());
            }
        }
        return res;
    }

    protected static <K> void increment(Map<K, Integer> map, K key, int delta) {
        map.merge(key, delta, Integer::sum);
    }

    public static final Predicate<CardRules> AI_CAN_PLAY = CardRulesPredicates.IS_KEPT_IN_AI_DECKS.and(CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS);

    public static final Predicate<CardRules> COLORLESS_CARDS = c -> {
        ManaCost mc = c.getManaCost();
        return c.getColorIdentity().isColorless() && !mc.isNoCost();
    };

    public static class MatchColorIdentity implements Predicate<CardRules> {
        private final ColorSet allowedColor;

        public MatchColorIdentity(ColorSet color) {
            allowedColor = color;
        }

        @Override
        public boolean test(CardRules subject) {
            ManaCost mc = subject.getManaCost();
            return !mc.isPureGeneric() && allowedColor.containsAllColorsFrom(subject.getColorIdentity().getColor());
            //return  mc.canBePaidWithAvaliable(allowedColor);
            // return allowedColor.containsAllColorsFrom(mc.getColorProfile());
        }
    }

    public static class FilterCMC implements Predicate<CardRules> {
        private final int min;
        private final int max;

        public FilterCMC(int from, int to) {
            min = from;
            max = to;
        }

        @Override
        public boolean test(CardRules c) {
            ManaCost mc = c.getManaCost();
            int cmc = mc.getCMC();
            return cmc >= min && cmc <= max && !mc.isNoCost();
        }
    }

    protected List<String> getDualLandList(boolean forAi) {
        return getDualLandList(forAi ? AI_CAN_PLAY : CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS);
    }
    /**
     * Get list of dual lands for this color combo.
     *
     * @return dual land names
     */
    protected List<String> getDualLandList(Predicate<CardRules> canPlay) {
        if (colors.countColors() > 3) {
            addCardNameToList("Rupture Spire", dLands);
            addCardNameToList("Undiscovered Paradise", dLands);
        }

        if (colors.countColors() > 2) {
            addCardNameToList("Evolving Wilds", dLands);
            addCardNameToList("Terramorphic Expanse", dLands);
        }

        //filter to provide all dual lands from pool matching 2 or 3 colors from current deck
        Predicate<CardRules> dualLandFilter = CardRulesPredicates.coreType(CardType.CoreType.Land);
        Predicate<CardRules> exceptBasicLand = CardRulesPredicates.NOT_BASIC_LAND;

        Iterable<PaperCard> landCards = pool.getAllCards(PaperCardPredicates.fromRules(dualLandFilter.and(exceptBasicLand).and(canPlay)));
        Iterable<String> dualLandPatterns = Arrays.asList("Add \\{([WUBRG])\\} or \\{([WUBRG])\\}",
                "Add \\{([WUBRG])\\}, \\{([WUBRG])\\}, or \\{([WUBRG])\\}",
                "Add \\{([WUBRG])\\}\\{([WUBRG])\\}",
                "Add \\{[WUBRG]\\}\\{[WUBRG]\\}, \\{([WUBRG])\\}\\{([WUBRG])\\}, or \\{[WUBRG]\\}\\{[WUBRG]\\}");
        for (String pattern:dualLandPatterns) {
            regexLandSearch(pattern, landCards);
        }
        regexFetchLandSearch(landCards);

        return dLands;
    }

    public List<String> regexLandSearch(String pattern, Iterable<PaperCard> landCards) {
        Pattern p = Pattern.compile(pattern);
        for (PaperCard card:landCards) {
            Matcher matcher = p.matcher(card.getRules().getOracleText());
            while (matcher.find()) {
                List<String> manaColorNames = new ArrayList<>();
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    manaColorNames.add(matcher.group(i));
                }
                ColorSet manaColorSet = ColorSet.fromNames(manaColorNames);
                if (colors.hasAllColors(manaColorSet.getColor())){
                    addCardNameToList(card.getName(),dLands);
                }else{
                    addCardNameToList(card.getName(),inverseDLands);
                }
            }
        }
        return dLands;
    }

    public List<String> regexFetchLandSearch(Iterable<PaperCard> landCards) {
        final String fetchPattern="Search your library for an* ([^\\s]*) or ([^\\s]*) card";
        Map<String,String> colorLookup= new HashMap<>();
        colorLookup.put("Plains","W");
        colorLookup.put("Forest","G");
        colorLookup.put("Mountain","R");
        colorLookup.put("Island","U");
        colorLookup.put("Swamp","B");
        Pattern p = Pattern.compile(fetchPattern);
        for (PaperCard card:landCards) {
            Matcher matcher = p.matcher(card.getRules().getOracleText());
            while (matcher.find()) {
                List<String> manaColorNames = new ArrayList<>();
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    manaColorNames.add(colorLookup.get(matcher.group(i)));
                }
                ColorSet manaColorSet = ColorSet.fromNames(manaColorNames);
                if (colors.hasAllColors(manaColorSet.getColor())){
                    addCardNameToList(card.getName(),dLands);
                }else{
                    addCardNameToList(card.getName(),inverseDLands);
                }
            }
        }
        return dLands;
    }

    private void addCardNameToList(String cardName, List<String> cardNameList) {
        if (pool.contains(cardName)) { //avoid adding card if it's not in pool
            cardNameList.add(cardName);
        }
    }
}
