// Builds forge-gui/res/lists/PrintingTraits.txt from Scryfall bulk data, for the facts Forge doesn't
// carry about a physical printing: watermark, the true border (borderless, gold, yellow...),
// whether the artwork shows an open mouth (Knight of the Kitchen Sink), which way its creature
// faces (Sly Spy), how many figures it shows (art menace), and how many lines of text are printed
// on it (wordy, Lexivore: rulelines leaves out lines that are only reminder text, textlines counts every line),
// whether its art shows an artifact creature (Assembled Ensemble), whether it is a Booster Fun printing (Collectigull),
// which Universe a Universes Beyond printing is from (Byode), and which named characters it names or shows (Vorthos,
// see characters.js).
//
// Inputs (download fresh from https://api.scryfall.com/bulk-data):
//   default-cards.jsonl.gz  - every printing
//   art-tags.jsonl.gz       - Tagger illustration tags
//   ../rules-lines/results.jsonl - lines counted on each card image (see ../rules-lines/count.py)
//
// After a new set: download both bulk files, then in ../rules-lines run `node jobs.js` and
// `.venv/Scripts/python count.py` (it only reads printings it hasn't seen), then run this.
//
// Only printings that exist in forge-gui/res/editions are written, keyed by Forge edition code and
// collector number (what PaperCard carries). Border is written only where Scryfall disagrees with
// what Forge's edition files already say, so the file patches the edition data rather than
// duplicating it. Like FlavorText.txt, this is a fork-local file rather than an edit to the
// upstream-tracked edition files, which would conflict on every merge.
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const readline = require('readline');
const { buildCharacters, charactersOf, characterRows } = require('./characters');
const SET_PLANES = JSON.parse(fs.readFileSync(path.join(__dirname, 'set-planes.json'), 'utf8')).planes;

const ROOT = path.join(__dirname, '..', '..');
const EDITIONS = path.join(ROOT, 'forge-gui', 'res', 'editions');
const OUT = path.join(ROOT, 'forge-gui', 'res', 'lists', 'PrintingTraits.txt');
const CARDS = path.join(__dirname, 'default-cards.jsonl.gz');
const TAGS = path.join(__dirname, 'art-tags.jsonl.gz');
// Byode's Universes: built by ../universes/resolve.py from fresher data than CARDS, so printings newer than
// CARDS still get theirs
const UNIVERSES = path.join(__dirname, '..', 'universes', 'universes.tsv');

function readUniverses() {
    const map = new Map();
    for (const line of fs.readFileSync(UNIVERSES, 'utf8').split('\n')) {
        const [set, cn, universe] = line.split('|');
        if (universe) map.set(`${set}|${cn.toLowerCase()}`, universe.trim());
    }
    return map;
}

// Scryfall's tag for "open mouth" is named after this very card; its children (roar, yelling,
// screaming...) are open mouths too, which is how Scryfall's own art:open-mouth search counts them.
const OPEN_MOUTH_TAG = 'loose-lips';
// Tagger's facing tags (left-facing names Sly Spy itself; right is "from our perspective", as the
// Unstable FAQ rules). Only about one creature artwork in six is tagged either way, so an untagged
// one is unknown, not facing nowhere.
const FACING_TAGS = { facingleft: 'left-facing', facingright: 'right-facing' };
// Tagger's figure counts (Garbage Elemental's art menace). "solo" is one figure; these and their
// children are two or more. Art tagged both ways, or neither, is unknown and left to the players.
const SOLO_TAG = 'solo';
const MULTI_FIGURE_TAGS = ['two-figures', 'three-figures', 'four-figures', 'five-figures', 'six-figures',
    'seven-figures', 'eight-figures', 'duo', 'trio', 'group', 'crowd', 'army', 'horde', 'swarm',
    'multiple-characters', 'circle-of-people'];
// Tagger's "solo" means one main subject, but the Unstable FAQ counts any living or undead being able to move -
// Paradise Druid (WOC 129) is tagged solo and has butterflies. So a solo artwork whose tags show an animal
// (bug, bird...) beside another kind of being is left unknown. These are Tagger's roots for kinds of beings;
// an animal counts by its own kind (bug, bird...), so a bird snapping up a bug is two.
const BEING_ROOTS = ['humanoid', 'undead', 'dragon', 'spirit', 'elemental', 'demon', 'vampire', 'horror', 'faerie',
    'construct', 'golem', 'robot', 'eldrazi', 'sliver', 'ooze', 'wyvern', 'drake', 'kaiju', 'alien', 'devil',
    'werewolf', 'phyrexian', 'mutant', 'aquatic-creature', 'mystical-creature', 'shapeshifter', 'hybrid', 'avatar'];
const LINES = path.join(__dirname, '..', 'rules-lines', 'results.jsonl');
// Goblin Haberdasher's hats: worn headwear in Tagger's hat tree, helmets and crowns included (user, 2026-10-02;
// the FAQ: "a garment worn on the head that's not part of another garment"), but not a helmet being held, a sticker
// sheet, or "not-a-hat" (a thing used as a hat - Haberdasher's own beehive). Only on single-figure art, since a
// background character's hat doesn't count (rulings) and Tagger doesn't say who wears it. The plain "helmet" tag
// alone doesn't confirm one either: helmets are carried as often as worn (8ED Seasoned Marshal holds hers and is
// tagged "helmet", not "helmet-holding"), so that art is left to the honor-system question.
const HAT_TAG = 'hat';
const NOT_WORN_HAT_TAGS = ['not-a-hat', 'helmet-holding', 'hat-sticker', 'the-horned-halo'];

const RESERVED = new Set(['metadata', 'other']);
const CARD_RE = /^(.?[0-9A-Z-]+\S*[A-Z]*)\s(?:[SCURML]\s)?([^@$]+?)(?: @([^$]*))?(?: \$\{(.+)\})?$/;
const TOKEN_RE = /^(.?[0-9A-Z-]+\S?[A-Z☇]*)\s([^@]+?)(?: @(.*))?$/;

function readEditions() {
    // scryfall "set|cn" -> [{ code, cn, border }] - a list, since a few Forge editions share one
    // Scryfall set (6ED and its Chinese alternate-art edition, MB1 and PLST, ...)
    const printings = new Map();
    for (const file of fs.readdirSync(EDITIONS)) {
        if (!file.endsWith('.txt')) continue;
        const lines = fs.readFileSync(path.join(EDITIONS, file), 'utf8').split(/\r?\n/);
        const meta = {};
        let section = null;
        const entries = [];
        for (const raw of lines) {
            const line = raw.trim();
            if (!line || line.startsWith('#')) continue;
            const sec = line.match(/^\[(.+)\]$/);
            if (sec) { section = sec[1].toLowerCase(); continue; }
            if (section === 'metadata') {
                const eq = line.indexOf('=');
                if (eq > 0) meta[line.slice(0, eq).trim()] = line.slice(eq + 1).trim();
                continue;
            }
            if (!section || RESERVED.has(section)) continue;
            if (section === 'tokens') {
                const m = line.match(TOKEN_RE);
                if (m) entries.push({ token: true, cn: m[1] });
            } else {
                const m = line.match(CARD_RE);
                if (!m) continue;
                const params = {};
                if (m[4]) for (const p of m[4].matchAll(/"([^"]+)"\s*:\s*"([^"]+)"/g)) params[p[1]] = p[2];
                entries.push({ token: false, cn: m[1], name: m[2].trim(), border: params.border });
            }
        }
        if (!meta.Code) continue;
        const scry = (meta.ScryfallCode || meta.Code).toLowerCase();
        const tokens = (meta.TokensCode || ('T' + (meta.ScryfallCode || meta.Code))).toLowerCase();
        const edBorder = (meta.Border || 'Black').toLowerCase();
        for (const e of entries) {
            const key = (e.token ? tokens : scry) + '|' + e.cn.toLowerCase();
            if (!printings.has(key)) printings.set(key, []);
            printings.get(key).push({
                // a token shares its edition code and numbering with the set's cards (UST token 12 vs
                // Knight of the Kitchen Sink 12a-f), so token rows get their own "@" prefix
                code: (e.token ? '@' : '') + meta.Code,
                cn: e.cn,
                // per-line border overrides are looked up by collector number among the edition's
                // cards, so a token only ever gets the edition default
                border: e.token ? edBorder : (e.border || edBorder).toLowerCase(),
            });
            // Forge numbers some printings with an F Scryfall doesn't have (Unfinity's acorn cards are F3 where
            // Scryfall says 3); match those by number too, but only to the same card, since an F can mean
            // something else elsewhere
            const f = !e.token && e.cn.match(/^F(\d.*)$/);
            if (f) {
                const alias = scry + '|' + f[1].toLowerCase();
                if (!printings.has(alias)) printings.set(alias, []);
                printings.get(alias).push({ code: meta.Code, cn: e.cn, border: (e.border || edBorder).toLowerCase(), onlyName: e.name });
            }
        }
    }
    return printings;
}

async function readTags() {
    const byId = new Map();
    const rl = readline.createInterface({ input: fs.createReadStream(TAGS).pipe(zlib.createGunzip()), crlfDelay: Infinity });
    for await (const line of rl) {
        if (!line.trim()) continue;
        const t = JSON.parse(line);
        byId.set(t.id, t);
    }
    return byId;
}

function readFacingIllustrations(byId) {
    const result = {};
    for (const [trait, slug] of Object.entries(FACING_TAGS)) {
        const tag = [...byId.values()].find(t => t.slug === slug);
        if (!tag) throw new Error(`tag ${slug} not found`);
        result[trait] = new Set(tag.taggings.map(g => g.illustration_id));
        console.log(`${slug.padEnd(18)}: ${result[trait].size} arts`);
    }
    return result;
}

function descendants(byId, slug) {
    const root = [...byId.values()].find(t => t.slug === slug);
    if (!root) throw new Error(`tag ${slug} not found`);
    const seen = new Set();
    const stack = [root.id];
    while (stack.length) {
        const id = stack.pop();
        if (seen.has(id) || !byId.has(id)) continue;
        seen.add(id);
        stack.push(...byId.get(id).child_ids);
    }
    return seen;
}

// illustration id -> 'solo' | 'multi'
function readFigureIllustrations(byId) {
    const kind = new Map();
    const mark = (ids, k) => {
        for (const id of ids) {
            for (const g of byId.get(id).taggings) {
                const prev = kind.get(g.illustration_id);
                kind.set(g.illustration_id, prev && prev !== k ? 'both' : k);
            }
        }
    };
    mark(descendants(byId, SOLO_TAG), 'solo');
    const multi = new Set();
    for (const slug of MULTI_FIGURE_TAGS) for (const id of descendants(byId, slug)) multi.add(id);
    mark(multi, 'multi');
    for (const [ill, k] of kind) if (k === 'both') kind.delete(ill);

    // tag id -> the kind of being it names, if any
    const beingKind = new Map();
    for (const root of BEING_ROOTS) for (const id of descendants(byId, root)) beingKind.set(id, root);
    const animal = [...byId.values()].find(t => t.slug === 'animal');
    for (const child of animal.child_ids) {
        if (!byId.has(child)) continue;
        for (const id of descendants(byId, byId.get(child).slug)) beingKind.set(id, 'animal:' + byId.get(child).slug);
    }
    for (const id of descendants(byId, 'animal')) if (!beingKind.has(id)) beingKind.set(id, 'animal');
    const kindsOf = new Map();
    for (const t of byId.values()) {
        const k = beingKind.get(t.id);
        if (!k) continue;
        for (const g of t.taggings) {
            if (kind.get(g.illustration_id) !== 'solo') continue;
            if (!kindsOf.has(g.illustration_id)) kindsOf.set(g.illustration_id, new Set());
            kindsOf.get(g.illustration_id).add(k);
        }
    }
    let demoted = 0;
    for (const [ill, kinds] of kindsOf) {
        if (kinds.size >= 2 && [...kinds].some(k => k.startsWith('animal'))) {
            kind.delete(ill);
            demoted++;
        }
    }
    console.log(`solo with beings : ${demoted} left unknown (an animal beside another kind of being)`);
    console.log(`figure arts       : ${[...kind.values()].filter(k => k === 'solo').length} solo, ${[...kind.values()].filter(k => k === 'multi').length} multi`);
    return kind;
}

// Lines with at least one word outside parentheses (Alexander Clamilton's rulings, CR 207.2): a line that is only
// reminder text isn't a line of rules text. OCR sometimes misses a parenthesis; an opening one that never closes is
// ignored so it can't swallow the rules lines after it, and a stray closing one leaves the lines before it counted.
function rulesLinesWithoutReminder(rows) {
    const isOpen = ch => ch === '(' || ch === '（';
    const isClose = ch => ch === ')' || ch === '）';
    const stack = [];
    rows.forEach((row, i) => [...row].forEach((ch, j) => {
        if (isOpen(ch)) stack.push(`${i}:${j}`);
        else if (isClose(ch)) stack.pop();
    }));
    const unmatched = new Set(stack);
    let depth = 0;
    let n = 0;
    rows.forEach((row, i) => {
        let outside = false;
        [...row].forEach((ch, j) => {
            if (isOpen(ch)) {
                if (!unmatched.has(`${i}:${j}`)) depth++;
            } else if (isClose(ch)) {
                depth = Math.max(0, depth - 1);
            } else if (depth === 0 && /[A-Za-z0-9]/.test(ch)) {
                outside = true;
            }
        });
        if (outside) n++;
    });
    return n;
}

// "set|cn|face" -> { rules, text }, the latest readable count for each face
async function readLines() {
    const lines = new Map();
    if (!fs.existsSync(LINES)) {
        console.log('lines             : no ../rules-lines/results.jsonl, skipped');
        return lines;
    }
    const rl = readline.createInterface({ input: fs.createReadStream(LINES), crlfDelay: Infinity });
    for await (const line of rl) {
        if (!line.trim()) continue;
        const r = JSON.parse(line);
        const key = `${r.key}|${r.face}`;
        if (r.status === 'ok') lines.set(key, { rules: rulesLinesWithoutReminder(r.rows.slice(0, r.lines)), text: r.lines + r.flavor_rows, flavor: r.flavor_rows });
        else lines.delete(key);
    }
    console.log(`lines             : ${lines.size} faces read`);
    return lines;
}

// illustration ids showing a worn hat
function readHatIllustrations(byId) {
    const notWorn = new Set(NOT_WORN_HAT_TAGS.flatMap(slug => [...descendants(byId, slug)]));
    const worn = [...descendants(byId, HAT_TAG)].filter(id => !notWorn.has(id));
    const bySlug = slug => [...byId.values()].find(t => t.slug === slug);
    const generic = { hat: bySlug(HAT_TAG), helmet: bySlug('helmet') };
    const tagsOf = new Map();
    for (const id of [...worn, ...notWorn]) {
        for (const g of byId.get(id).taggings) {
            if (!tagsOf.has(g.illustration_id)) tagsOf.set(g.illustration_id, new Set());
            tagsOf.get(g.illustration_id).add(id);
        }
    }
    const hats = new Set();
    for (const [ill, ids] of tagsOf) {
        const has = slug => [...ids].some(id => byId.get(id).slug === slug);
        let wornIds = [...ids].filter(id => !notWorn.has(id));
        // the plain "hat" tag may be naming the thing that isn't worn
        if (has('not-a-hat')) wornIds = wornIds.filter(id => id !== generic.hat.id);
        wornIds = wornIds.filter(id => id !== generic.helmet.id);
        if (wornIds.length) hats.add(ill);
    }
    console.log(`hat arts          : ${hats.size}`);
    return hats;
}

// Selfie Preservation's "a tree in its art": Tagger's tree tag and everything under it - dead and fallen trees
// count (FAQ: still tree-shaped), and so do Treefolk (FAQ: "Absolutely") - plus bonsai, a living tree tagged
// outside the tree tree. Stumps aren't (ruling), and tree-stump isn't under it. Untagged art stays a question.
const TREE_TAG = 'tree';
const EXTRA_TREE_TAGS = ['bonsai-tree'];

function readTreeIllustrations(byId) {
    const ids = [...descendants(byId, TREE_TAG), ...EXTRA_TREE_TAGS.flatMap(slug => [...descendants(byId, slug)])];
    const ills = new Set();
    for (const id of ids) for (const g of byId.get(id).taggings) ills.add(g.illustration_id);
    console.log(`tree arts         : ${ills.size}`);
    return ills;
}

// Princess Luna's "a moon in their art": Tagger's moon tag and everything under it (full, crescent, blood, eclipse,
// Mirrodin's suns - "technically also moons", says Tagger, after the Moons of Mirrodin novel), plus the lunar
// eclipse, a moon in shadow tagged outside the moon tree. Untagged art doesn't count (user, 2026-10-10).
const MOON_TAG = 'moon';
const EXTRA_MOON_TAGS = ['lunar-eclipse'];

function readMoonIllustrations(byId) {
    const ids = [...descendants(byId, MOON_TAG), ...EXTRA_MOON_TAGS.flatMap(slug => [...descendants(byId, slug)])];
    const ills = new Set();
    for (const id of ids) for (const g of byId.get(id).taggings) ills.add(g.illustration_id);
    console.log(`moon arts         : ${ills.size}`);
    return ills;
}

// Don't Try This at Home's "hot": Tagger's own hot tag ("Things that are hot, like fire or explosions") and everything
// under it, plus what the rulings name or plainly burns but Tagger files elsewhere - the sun, torches, candles,
// volcanoes. Art Tagger hasn't tagged at all stays a question (user, 2026-10-21).
const HOT_TAG = 'hot';
const EXTRA_HOT_TAGS = ['sun', 'torch', 'candle', 'volcano', 'flamethrower', 'blowtorch'];

function readHotIllustrations(byId) {
    const ids = [...descendants(byId, HOT_TAG), ...EXTRA_HOT_TAGS.filter(slug => [...byId.values()].some(t => t.slug === slug))
        .flatMap(slug => [...descendants(byId, slug)])];
    const ills = new Set();
    for (const id of ids) for (const g of byId.get(id).taggings) ills.add(g.illustration_id);
    console.log(`hot arts          : ${ills.size}`);
    return ills;
}

// Ignacio of Myra's Marvels' objects: Tagger's tags of the same names and everything under each. Fire is Tagger's fire
// tree only, so lava and magma aren't fire (rulings). Written as artobjects=armor,fire... per face.
// Scavenger Hunt's items too: a humanoid animal or alien (animal-person, alien), food, and horns and tusks (horn, tusk).
const ART_OBJECTS = ['armor', 'blood', 'book', 'bottle', 'fang', 'fire', 'scroll', 'spear', 'sword', 'water',
    'animal-person', 'alien', 'food', 'horn', 'tusk'];

function readArtObjectIllustrations(byId) {
    const byIll = new Map();
    for (const object of ART_OBJECTS) {
        for (const id of descendants(byId, object)) {
            for (const g of byId.get(id).taggings) {
                if (!byIll.has(g.illustration_id)) byIll.set(g.illustration_id, new Set());
                byIll.get(g.illustration_id).add(object);
            }
        }
    }
    console.log('art object arts   : ' + byIll.size);
    return byIll;
}

// Acornelia's "a spell with a squirrel in its art": Tagger's squirrel tag and everything under it (named squirrels,
// squirrel people and hordes), plus secret-squirrel, a squirrel hidden in the picture. Art Tagger hasn't tagged at all
// is written as untaggedart, so a script can ask about just that art and trust the tags for the rest.
const SQUIRREL_TAG = 'squirrel';
const EXTRA_SQUIRREL_TAGS = ['secret-squirrel'];

function readSquirrelIllustrations(byId) {
    const ids = [...descendants(byId, SQUIRREL_TAG), ...EXTRA_SQUIRREL_TAGS.flatMap(slug => [...descendants(byId, slug)])];
    const ills = new Set();
    for (const id of ids) for (const g of byId.get(id).taggings) ills.add(g.illustration_id);
    console.log(`squirrel arts     : ${ills.size}`);
    return ills;
}

// Jermane, Pride of the Circus's "creature with four or more legs": Tagger's four-legs and many-legs (5+) tags and
// everything under them - not lots-of-legs itself, which takes in three-legs. Tagger tags the whole picture, and the
// legs only count on the figure that is the creature (rulings), so art with a rider is left out: those legs are
// usually the mount's (Black Knight). Untagged and left-out art stays a question.
const LEG_TAGS = ['four-legs', 'many-legs'];
const RIDER_TAGS = ['mount-rider', 'cavalry'];

function readFourLegsIllustrations(byId) {
    const riders = new Set();
    for (const id of RIDER_TAGS.flatMap(slug => [...descendants(byId, slug)])) {
        for (const g of byId.get(id).taggings) riders.add(g.illustration_id);
    }
    const ills = new Set();
    for (const id of LEG_TAGS.flatMap(slug => [...descendants(byId, slug)])) {
        for (const g of byId.get(id).taggings) if (!riders.has(g.illustration_id)) ills.add(g.illustration_id);
    }
    console.log(`four+ legs arts   : ${ills.size}`);
    return ills;
}

// Assembled Ensemble's "a spell with an artifact creature in its art". Tagger has no such tag, so: these always show
// one (the rulings' Dancing Scimitar is an animated object)...
const ARTIFACT_CREATURE_TAGS = ['robot', 'construct', 'golem', 'thopter', 'servo', 'automaton', 'juggernaut', 'animated-object'];
// ...and these only when the art is an artifact creature's own (user ruling: Darksteel Gargoyle's art counts, Abbey
// Gargoyles' doesn't, a scarecrow in a field doesn't, a Vehicle never does). An artifact creature's own art always
// shows it, tagged or not (Dancing Scimitar, which the rulings name).
const ARTIFACT_CREATURE_IF_ITS_OWN_TAGS = ['gargoyle', 'scarecrow', 'cyborg', 'clockwork', 'vehicle', 'homunculus'];

async function readArtifactCreatureIllustrations(byId) {
    const ills = new Set();
    for (const slug of ARTIFACT_CREATURE_TAGS) {
        for (const id of descendants(byId, slug)) for (const g of byId.get(id).taggings) ills.add(g.illustration_id);
    }
    const ownArt = new Set();
    const rl = readline.createInterface({ input: fs.createReadStream(CARDS).pipe(zlib.createGunzip()), crlfDelay: Infinity });
    for await (const line of rl) {
        if (!line.trim()) continue;
        const c = JSON.parse(line);
        for (const f of Array.isArray(c.card_faces) ? c.card_faces : [c]) {
            const type = f.type_line ?? c.type_line ?? '';
            const ill = f.illustration_id ?? c.illustration_id;
            if (ill && /\bArtifact\b/.test(type) && /\bCreature\b/.test(type)) ownArt.add(ill);
        }
    }
    // every artifact creature's own art counts, which takes in the IF_ITS_OWN tags' qualifying arts too; they're
    // listed so the ruling stays written down next to the tags it's about
    for (const ill of ownArt) ills.add(ill);
    console.log(`artifact creature arts: ${ills.size}`);
    return ills;
}

function readTaggedIllustrations(byId) {
    const ills = new Set();
    for (const t of byId.values()) for (const g of t.taggings) ills.add(g.illustration_id);
    console.log(`tagged arts       : ${ills.size}`);
    return ills;
}

function readOpenMouthIllustrations(byId) {
    const seen = descendants(byId, OPEN_MOUTH_TAG);
    const ills = new Set();
    for (const id of seen) for (const g of byId.get(id).taggings) ills.add(g.illustration_id);
    console.log(`open-mouth tags   : ${[...seen].map(i => byId.get(i).slug).join(', ')}`);
    console.log(`open-mouth arts   : ${ills.size}`);
    return ills;
}

// The face a player sees: the front, or the back once transformed. Split/adventure/flip cards have
// one illustration for the whole card, so they only ever have a front.
function faceFlags(c, pick) {
    const faces = Array.isArray(c.card_faces) ? c.card_faces : [];
    const front = pick(faces[0]) ?? pick(c);
    const back = faces.length > 1 ? pick(faces[1]) : undefined;
    return { front, back };
}

async function main() {
    const printings = readEditions();
    const forgeTotal = [...printings.values()].reduce((n, l) => n + l.length, 0);
    console.log(`forge printings   : ${forgeTotal}`);
    const tags = await readTags();
    const openMouth = readOpenMouthIllustrations(tags);
    const facing = readFacingIllustrations(tags);
    const figures = readFigureIllustrations(tags);
    const hats = readHatIllustrations(tags);
    const trees = readTreeIllustrations(tags);
    const moons = readMoonIllustrations(tags);
    const squirrels = readSquirrelIllustrations(tags);
    const hots = readHotIllustrations(tags);
    const fourLegs = readFourLegsIllustrations(tags);
    const artObjects = readArtObjectIllustrations(tags);
    const artifactCreatures = await readArtifactCreatureIllustrations(tags);
    const tagged = readTaggedIllustrations(tags);
    const lineCounts = await readLines();
    const universes = readUniverses();
    const characters = buildCharacters(tags);

    const rows = [];
    const stats = { scanned: 0, matched: 0, watermark: 0, backWatermark: 0, border: 0, openMouth: 0, backOpenMouth: 0, facing: 0, figures: 0, lines: 0, hats: 0, trees: 0, moons: 0, squirrels: 0, hots: 0, artifactCreatures: 0, untagged: 0, boosterFun: 0, universe: 0, characters: 0 };
    const borderFixes = {};
    const seen = new Set();
    const rl = readline.createInterface({ input: fs.createReadStream(CARDS).pipe(zlib.createGunzip()), crlfDelay: Infinity });
    for await (const line of rl) {
        if (!line.trim()) continue;
        const c = JSON.parse(line);
        stats.scanned++;
        const key = c.set + '|' + c.collector_number.toLowerCase();
        const list = printings.get(key);
        if (!list || seen.has(key)) continue;
        seen.add(key);
        const wm = faceFlags(c, o => o && o.watermark);
        const om = faceFlags(c, o => o && o.illustration_id ? openMouth.has(o.illustration_id) : undefined);
        const named = charactersOf(characters, c);
        const faces = {};
        for (const [trait, ills] of Object.entries(facing)) {
            faces[trait] = faceFlags(c, o => o && o.illustration_id ? ills.has(o.illustration_id) : undefined);
        }
        for (const p of list) {
            if (p.onlyName && p.onlyName !== c.name && p.onlyName !== (c.card_faces || [{}])[0].name) continue;
            stats.matched++;
            const traits = [];
            if (c.border_color && c.border_color !== p.border) {
                traits.push('border=' + c.border_color);
                stats.border++;
                const k = `${p.border}->${c.border_color}`;
                borderFixes[k] = (borderFixes[k] || 0) + 1;
            }
            if (wm.front) { traits.push('watermark=' + wm.front); stats.watermark++; }
            if (wm.back) { traits.push('backwatermark=' + wm.back); stats.backWatermark++; }
            if (om.front) { traits.push('openmouth'); stats.openMouth++; }
            if (om.back) { traits.push('backopenmouth'); stats.backOpenMouth++; }
            const fig = faceFlags(c, o => o && o.illustration_id ? figures.get(o.illustration_id) : undefined);
            if (fig.front) { traits.push('figures=' + fig.front); stats.figures++; }
            if (fig.back) { traits.push('backfigures=' + fig.back); stats.figures++; }
            const hat = faceFlags(c, o => o && o.illustration_id && figures.get(o.illustration_id) === 'solo'
                && hats.has(o.illustration_id) ? true : undefined);
            if (hat.front) { traits.push('hat'); stats.hats++; }
            if (hat.back) { traits.push('backhat'); stats.hats++; }
            const tree = faceFlags(c, o => o && o.illustration_id && trees.has(o.illustration_id) ? true : undefined);
            if (tree.front) { traits.push('tree'); stats.trees++; }
            if (tree.back) { traits.push('backtree'); stats.trees++; }
            const moon = faceFlags(c, o => o && o.illustration_id && moons.has(o.illustration_id) ? true : undefined);
            if (moon.front) { traits.push('moon'); stats.moons++; }
            if (moon.back) { traits.push('backmoon'); stats.moons++; }
            const squirrel = faceFlags(c, o => o && o.illustration_id && squirrels.has(o.illustration_id) ? true : undefined);
            if (squirrel.front) { traits.push('squirrel'); stats.squirrels++; }
            if (squirrel.back) { traits.push('backsquirrel'); stats.squirrels++; }
            const hot = faceFlags(c, o => o && o.illustration_id && hots.has(o.illustration_id) ? true : undefined);
            if (hot.front) { traits.push('hot'); stats.hots++; }
            if (hot.back) { traits.push('backhot'); stats.hots++; }
            const legs = faceFlags(c, o => o && o.illustration_id && fourLegs.has(o.illustration_id) ? true : undefined);
            if (legs.front) traits.push('fourlegs');
            if (legs.back) traits.push('backfourlegs');
            const objects = faceFlags(c, o => o && o.illustration_id && artObjects.has(o.illustration_id)
                ? [...artObjects.get(o.illustration_id)].sort().join(',') : undefined);
            if (objects.front) traits.push('artobjects=' + objects.front);
            if (objects.back) traits.push('backartobjects=' + objects.back);
            const ac = faceFlags(c, o => o && o.illustration_id && artifactCreatures.has(o.illustration_id) ? true : undefined);
            if (ac.front) { traits.push('artifactcreature'); stats.artifactCreatures++; }
            if (ac.back) { traits.push('backartifactcreature'); stats.artifactCreatures++; }
            const untagged = faceFlags(c, o => o && o.illustration_id ? !tagged.has(o.illustration_id) : undefined);
            if (untagged.front) { traits.push('untaggedart'); stats.untagged++; }
            if (untagged.back) { traits.push('backuntaggedart'); stats.untagged++; }
            // Collectigull's "Booster Fun card": Scryfall's boosterfun promo type (Throne of Eldraine on)
            if ((c.promo_types || []).includes('boosterfun')) { traits.push('boosterfun'); stats.boosterFun++; }
            // The Unknown Wizard, You, Magic Playtester: a playtest card is a printing with Scryfall's playtest promo type
            // Minnea, Planar Tourist: the plane of the printing's set, when the wiki sets it on a single plane
            const plane = SET_PLANES[(c.set || '').toUpperCase()];
            if (plane) { traits.push('plane=' + plane); stats.planes = (stats.planes || 0) + 1; }
            if ((c.promo_types || []).includes('playtest')) { traits.push('playtest'); stats.playtest = (stats.playtest || 0) + 1; }
            if (universes.has(key)) { traits.push('universe=' + universes.get(key)); stats.universe++; }
            if (named.length) { traits.push('characters=' + named.join(',')); stats.characters++; }
            // Park Map's "the same art": the illustration (Scryfall's id, cut short), on land printings only - only a land
            // card is searched for, and the permanents it's compared with share art with it only if they're lands too
            if (/\bLand\b/.test(c.type_line || '')) {
                const art = faceFlags(c, o => o && o.illustration_id ? o.illustration_id.replace(/-/g, '').slice(0, 10) : undefined);
                if (art.front) traits.push('art=' + art.front);
                if (art.back) traits.push('backart=' + art.back);
            }
            for (const [face, prefix] of [[0, ''], [1, 'back']]) {
                const n = lineCounts.get(`${key}|${face}`);
                if (n) {
                    traits.push(`${prefix}rulelines=${n.rules}`, `${prefix}textlines=${n.text}`);
                    // Hardy of Myra's Marvels; none written means none
                    if (n.flavor) traits.push(`${prefix}flavorlines=${n.flavor}`);
                    stats.lines++;
                }
            }
            for (const [trait, f] of Object.entries(faces)) {
                if (f.front) { traits.push(trait); stats.facing++; }
                if (f.back) { traits.push('back' + trait); stats.facing++; }
            }
            if (traits.length) rows.push(`${p.code}|${p.cn}|${traits.join('|')}`);
        }
    }
    for (const [key, universe] of universes) {
        if (seen.has(key)) continue;
        for (const p of printings.get(key) || []) {
            rows.push(`${p.code}|${p.cn}|universe=${universe}`);
            stats.universe++;
        }
    }

    rows.push(...characterRows(characters));
    rows.sort();
    fs.writeFileSync(OUT, rows.join('\n') + '\n', 'utf8');
    console.log(`scryfall scanned  : ${stats.scanned}`);
    console.log(`forge matched     : ${stats.matched} of ${forgeTotal} (${(stats.matched / forgeTotal * 100).toFixed(1)}%)`);
    console.log(`watermark         : ${stats.watermark} front, ${stats.backWatermark} back`);
    console.log(`open mouth        : ${stats.openMouth} front, ${stats.backOpenMouth} back`);
    console.log(`facing            : ${stats.facing}`);
    console.log(`figures           : ${stats.figures}`);
    console.log(`hats (one figure) : ${stats.hats}`);
    console.log(`trees             : ${stats.trees}`);
    console.log(`moons             : ${stats.moons}`);
    console.log(`squirrels         : ${stats.squirrels}`);
    console.log(`hot              : ${stats.hots}`);
    console.log(`artifact creatures: ${stats.artifactCreatures}`);
    console.log(`untagged arts     : ${stats.untagged}`);
    console.log(`booster fun       : ${stats.boosterFun}`);
    console.log(`playtest          : ${stats.playtest || 0}`);
    console.log(`planes            : ${stats.planes || 0}`);
    console.log(`universes beyond  : ${stats.universe}`);
    console.log(`named characters  : ${stats.characters}`);
    console.log(`line counts       : ${stats.lines}`);
    console.log(`border corrected  : ${stats.border} ${JSON.stringify(borderFixes)}`);
    console.log(`rows              : ${rows.length} -> ${OUT} (${(fs.statSync(OUT).size / 1048576).toFixed(2)} MB)`);
}

if (require.main === module) main();
module.exports = { readEditions };
