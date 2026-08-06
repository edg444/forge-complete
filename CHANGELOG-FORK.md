# Fork changelog

Everything this fork adds on top of upstream [Card-Forge/forge](https://github.com/Card-Forge/forge).
Upstream's own daily changes are **not** listed here — only work done in this fork.

- `origin` → `https://github.com/edg444/forge-complete.git` (this fork)
- `upstream` → `https://github.com/Card-Forge/forge.git` (merged in regularly; see README of the workflow below)

Reconstructed 2026-08-06 from git history (`git log --no-merges HEAD --not upstream/master`),
which remains the authoritative record if this file and the commits ever disagree.

**Keeping this current:** add to *Unreleased* as work lands, and date a section when it's built and
deployed. The one-line rule: if it changed behaviour, it belongs here.

---

## Engine capabilities added by this fork

A reference index — the reusable machinery, as opposed to individual cards. Card scripts across the
fork depend on these, so removing one is never local.

### Half values (Unhinged)

A complete half-integer layer running parallel to the whole-number one.

- **Power/toughness** — `PT:2.5/1`, `basePowerHalves()`/`baseToughnessHalves()`, half-aware P/T
  boosts (`AddPowerHalves$`/`AddToughnessHalves$`, `addPTBoostHalves`). Net P/T is computed in halves
  and floor-divided, so `getNetToughness` must mirror `getNetPower` rather than use the whole-number
  breakdown.
- **Mana** — real half shards (`HW`, `HR`, …) in costs and in the pool, floating half mana as change,
  half mana symbols drawn in both UIs, wired into pool events and the mana-burn setting.
- **Life** — half life totals, half life payment (`PayLifeHalves<N>`), half life shown in the avatar
  and life panel.
- **Damage** — half damage marked on creatures, half-aware prevention shields.

### Honour-system mechanics

- `CostFlavorAction<description/ButtonLabel>` — a cost paid by asserting you did something physical.
- `AILogic$ Chance.N` — rolled once per turn (not at every priority), one attempt per turn, honoured
  by every API rather than only ones using the base `canPlay`.
- Persistent honour states modelled as custom counters toggled by two zero-cost abilities
  (Standing Army's `STANDING`, Fat Ass's `EATING`).

### Choices the rules can't derive

- `DB$ ChooseArtist` + `Card.ArtistIsChosen` + `sharesArtistWith <restriction>` + `DB$ SetArtist`.
- `ChooseType | Type$ word | FreeInput$ True` (free text) with `MinLetters$` validation;
  `Type$ letter` with an AI branch (`MostCommonInitial`) in `chooseSomeType`.
- `DB$ ChooseEyeColor`, `DB$ GuessArtist`.
- `Card.chosenTypeKind`, so the detail panel says "chosen word"/"chosen letter", not always
  "chosen type".

### Card-text and name mechanics

- `Count$ CardPunctuationMarks`, `CardTextBoxNumbers`, `WordsInName` (readable from a static),
  `ChosenLetterInName`, `CardBingoLines`, `CurrentHour`, `MergedCount`,
  `DifferentExpansionSymbols`.
- Properties: `textHasChosenWord`, `nameStartsWithChosenLetter`, `fewerLettersInNameThanSource`,
  `HasHalfSymbol`, `Rarity<name>`, `alphabeticallyFirstNonLand`, `SilverBordered`.
- Flavor text available in-game (`CardFlavorText`, `Card.getTextBoxContents`) — several Un-set cards
  read the whole text box, flavor included.
- Flavor names count in name-based mechanics.

### Five-face split cards

Only Who // What // When // Where // Why has ever needed this.

- `CardStateName.Split3/4/5` and a shared `SPLIT_STATES` list; every hardcoded LeftSplit+RightSplit
  pair iterates the states a card actually has, so **two-face splits take an identical path**.
- `ALTERNATE` advances to the next face instead of always landing on face 1.
- Combined name, mana cost, colour, colour identity, type and oracle text fold in every face.
- Fuse, Aftermath and Rooms are deliberately left two-face.
- Split-state views are populated in a **separate pass** from ability-text rendering, because
  rendering one face can read a sibling.

### Zones

- **Dual residency** (`Card.shadowZone`) — Yet Another Aether Vortex's top-of-library permanents are
  on the battlefield *and* in the library. Battlefield is the primary zone; the library keeps a
  shadow entry. Residency must end in `Zone.remove`, never `Zone.add`: leaving the battlefield hands
  a *copy* to the destination zone, so an add-side hook never sees the original.

### Other

- `DB$ RememberNumber` — writes a number into the remembered set (which is a `Set`, so repeats
  collapse).
- `NotifyMessage$` works on **any** ability, not only dice rolls.
- `UnlessPaidSubAbility$` — fires when an Unless cost *is* paid.
- `AddTrigger$`, `RandomSet`, `Staying Power`, additional activation zones, `Rotate180` display for
  tokens printed inverted.
- Dev mode can pick which printing to add.

---

## Log

### Unreleased

_(nothing pending)_

### 2026-08-06 — Unhinged green and multicolor; five-face splits

- **Cards:** B-I-N-G-O, Creature Guy, Elvish House Party, Fat Ass, Fraction Jackson, Gluetius
  Maximus, Graphic Violence, Keeper of the Sacred Word, Land Aid '04, Laughing Hyena, Monkey Monkey
  Monkey, Name Dropping, the 141-character Elemental, Remodel, Side to Side, S.N.O.T., Stone-Cold
  Basilisk, Supersize, Symbol Status, Meddling Kids, Rare-B-Gone,
  Who // What // When // Where // Why.
- Yet Another Aether Vortex's third clause (dual residency), including the battlefield→top-of-library
  round trip staying put with no triggers.
- Five-face split card support (see above).
- WOE Role tokens printed inverted (`role_sorcerer`, `role_young_hero`, `role_cursed`) render rotated,
  with a rotate control, on desktop and mobile.
- Existing Gotcha cards reworded so the text box carries the full Oracle condition.
- Expansion-Symbol registered as a creature type.
- **AI:** takes a lethal solo attack rather than benching a creature whose evasion needs it to attack
  alone; picks a letter for letter-choosing cards instead of choosing nothing.
- **Deck generation:** random decks reinforce orphaned typal payoffs with some enablers (a Zombie
  lord no longer arrives with no Zombies). Random-colour piles are preserved by design.

### 2026-08-05 — Unhinged red; half mana production

- **Cards:** Assquatch, Curse of the Fire Penguin, Deal Damage, Dumb Ass, Face to Face, Frazzled
  Editor, Goblin S.W.A.T. Team, Mana Flair, Mons's Goblin Waiters, Orcish Paratroopers, Punctuate,
  Pygmy Giant, Red-Hot Hottie, Rocket-Powered Turbo Slug, Sauté, Six-y Beast, Touch and Go, Yet
  Another Aether Vortex (first two clauses), Zzzyxas's Abyss, Zombie Fanboy, Working Stiff, When
  Fluffy Bunnies Attack, Wet Willie of the Damned, Vile Bile, Tainted Monkey, Stop That!, Phyrexian
  Librarian, Persecute Artist, Necro-Impotence.
- Half mana **production**; half life fixes (cost prompt, affordability, copy, set, repaint).
- Flavor text data available in-game.
- Oracle text sync rounds 4–5 (self-reference pronouns, retemplating).
- Chosen artist shown in the detail panel.

### 2026-08-02

- AI stops attacking into walls that just absorb it.

### 2026-08-01

- **Cards:** Mother of Goons, Kill! Destroy!, Infernal Spawn of Infernal Spawn of Evil, Farewell to
  Arms, Eye to Eye, Mouth to Mouth, Loose Lips.
- `Chance.N` reworked: rolled once per turn, one attempt per turn, honoured by every API.
- AI battle handling: don't let a battle swallow the whole attack step; send leftover attackers;
  only commit the whole attack to a battle it can finish.
- Four AI fixes: prowess timing, vehicle reanimation, named card legality, tap-ability hoarding.

### 2026-07-31

- Dev mode can pick which printing to add.
- Framed! chooses its mode at resolution.
- Flavor names count in name-based mechanics.

### 2026-07-28 to 07-30

- **Cards:** The Fallen Apart, Eye to Eye, Duh, Bloodletter of Nesting Vampires, Bad Ass, Aesthetic
  Consultation.
- Half damage shown on creatures; half kept when power becomes damage; `WordsInName` readable from a
  static ability.
- Head to Head prevention as a named effect; Man of Measure rules text; granted land types read from
  the card.

### 2026-07-26 to 07-27 — the half-value layer and Unhinged white/blue

- **Cards:** _____, Ambiguity, Artful Looter, Avatar of Me, Brushstroke Paintermage, Bursting
  Beebles, Carnivorous Death-Parrot, Cheatyface, Collector Protector, Double Header, Drawn Together,
  Emcee, Erase (Not the Urza's Legacy One), Fascist Art Director, First Come First Served, Flaccify,
  Framed!, Frankie Peanuts, Greater Morphling, Head to Head, Ladies' Knight, Little Girl, Look at Me
  I'm R&D, Loose Lips, Magical Hacker, Man of Measure, Moniker Mage, Mouth to Mouth, Now I Know My
  ABC's, Number Crunch, Question Elemental?, Richard Garfield Ph.D., Save Life, Smart Ass, Spell
  Counter, Standing Army, Staying Power, Wordmail.
- The half-value layer built out: half P/T on the card state, real half mana shards, half mana
  symbols in both UIs, half-aware prevention.
- Child registered as a creature type; half set-P/T params registered with the P/T layer.

### 2026-07-25

- Unhinged/Arena promo cards; half life support; half power and toughness.

### 2026-07-23

- **The full Unglued (UGL) silver-bordered set.**
- In-game ability text synced to current Scryfall Oracle wording.

### 2026-07-21 — first changes

- Runeblade Raiser (Alchemy: Tarkir), Aquatic Subtlety (Alchemy: Lorwyn Eclipsed).
- Several new/unimplemented cards plus supporting engine features.
- Android debug build made self-contained and update-safe — bundles this fork's own
  `assets.zip` so the app never silently replaces it with upstream's card database, and uses a
  timestamp-based versionCode so installs are genuine in-place updates.

---

## Build and deployment

Recorded here because the build has fork-specific patches that are easy to lose.

- **Desktop:** whole-reactor `mvn install`; launch with working directory `forge-gui/` (the jar
  doesn't bundle `res/`). Use the Desktop `.bat` launcher.
- **Android:** whole-reactor `-P android-debug` (never `-pl forge-gui-android` alone), `subst` drive
  aliases to dodge the 8191-char command line, JDK 17, sign manually with `apksigner` (the plugin's
  built-in debug signer produces a corrupt signature on JDK 9+).
- **Upstream merges:** commit local work first, `git fetch upstream`, `git merge upstream/master`.
  Conflicts, when they happen, are in the core engine files this fork patches — `Card.java`,
  `CardView.java`, `CardFactory.java`, `CardRules.java`, `CardState.java`, `CardProperty.java` — not
  in card scripts, which essentially never conflict.
