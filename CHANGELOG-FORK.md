# Fork changelog

Everything this fork adds on top of upstream [Card-Forge/forge](https://github.com/Card-Forge/forge).
Upstream's own daily changes are **not** listed here — only work done in this fork, plus a note per
upstream merge (commit count, conflict resolutions, a short line on what came in).

- `origin` → `https://github.com/edg444/forge-complete.git` (this fork)
- `upstream` → `https://github.com/Card-Forge/forge.git` (merged in regularly; see README of the workflow below)

First reconstructed 2026-08-06 from git history (`git log --no-merges HEAD --not upstream/master`).
**Completed 2026-10-23:** every fork commit up to `7b3a3caa50e` (574 of them) and every upstream merge
was checked against this log. Missing changes were added in place, and the thinly summarized early
history (2026-07-21 to 2026-09-25) was rebuilt from the commits, with big squashed commits listed card
by card. Upstream merge counts include upstream's own merge commits (`git rev-list --count M^1..M^2`).
Corrections made then: 2026-08-05 no longer lists Face to Face (upstream since 2021) and uses the
`Name:` spellings "Stop That" and "Bloodletter"; Maro's Gone Nuts moved 74 doubling scripts to
`/Doubled`, not 76. Git history remains the authoritative record if this file and the commits ever
disagree.

**Keeping this current:** add to *Unreleased* as work lands, and date a section when it's built and
deployed. The one-line rule: if it changed behavior, it belongs here.

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

### Honor-system mechanics

- `CostFlavorAction<description/ButtonLabel>` — a cost paid by asserting you did something physical.
- `AILogic$ Chance.N` — rolled once per turn (not at every priority), one attempt per turn, honored
  by every API rather than only ones using the base `canPlay`.
- Persistent honor states modeled as custom counters toggled by two zero-cost abilities
  (Standing Army's `STANDING`, Fat Ass's `EATING`, Hoisted Hireling's `HOISTED`).

### Choices the rules can't derive

- `DB$ ChooseArtist` + `Card.ArtistIsChosen` + `sharesArtistWith <restriction>` + `DB$ SetArtist`. The chosen
  artist is a person (`ArtistCredit`): "A & B" credits split, nicknames ignored, merged cards' artists included.
- GenericChoice `AILogic$ MostMatchingCreatures` with an `AIMatch$ <valid>` on each choice - the AI picks what
  its creature cards in library and hand match most (Ineffable Blessing).
- `ChooseType | Type$ word | FreeInput$ True` (free text) with `MinLetters$` validation;
  `Type$ letter` with an AI branch (`MostCommonInitial`) in `chooseSomeType`.
- `DB$ ChooseEyeColor`, `DB$ GuessArtist`.
- `Card.chosenTypeKind`, so the detail panel says "chosen word"/"chosen letter", not always
  "chosen type".

### Card-text and name mechanics

- `Count$ CardPunctuationMarks`, `CardCapitalLetters`, `CardTextBoxNumbers`, `WordsInName` (readable from a static),
  `ChosenLetterInName`, `CardBingoLines`, `CurrentHour`, `MergedCount`,
  `DifferentExpansionSymbols`.
- Properties: `textHasChosenWord`, `nameStartsWithChosenLetter`, `fewerLettersInNameThanSource`,
  `HasHalfSymbol`, `Rarity<name>` (a basic land is common), `alphabeticallyFirstNonLand`, `SilverBordered`.
- Die results: `ResultSubAbilities$` keys `N`, `N-M`, and open-ended `N+` ("on an N or higher").
- Flavor text available in-game (`CardFlavorText`, `Card.getTextBoxContents`) — several Un-set cards
  read the whole text box, flavor included.
- Flavor names count in name-based mechanics.
- **A secret word the players guess at** (Hangman): ChooseType `Guessable$ True` (with `MinLetters$`,
  `MaxLetters$`, `WordList$ True` for the AI's vocabulary from `res/lists/Words.txt`), `DB$ GuessWord` with
  `GuessWrong$`/`GuessCorrect$`, trigger `Mode$ WordGuessed`. The word never reaches the view - only the
  hangman sheet does.

### Printing traits

- `res/lists/PrintingTraits.txt` (`forge.card.PrintingTraits`, lazily loaded like `CardFlavorText`) —
  per printing: watermark (and back-face watermark), open-mouth artwork (Scryfall Tagger's
  `loose-lips` tag and its children), and the printed border wherever Scryfall disagrees with the
  edition files (6,724 printings, mostly borderless). Keyed `CODE|collector`; tokens `@CODE|collector`.
  Regenerate with `_tools/printing-traits/generate.js` from Scryfall's default-cards and art-tags bulk.
- `Card.printedBorderColor()` (knows `BORDERLESS`/`YELLOW`), `getWatermark()`, `hasOpenMouthArt()`,
  `getCollectorNumberValue()` (last digit run: 12a → 12). `borderColor()` and the silver-border rules
  are deliberately unchanged.
- Properties `BlackBordered`, `WhiteBordered`, `CollectorNumberEven`/`Odd`, `Watermarked`, `Watermark_<name>`,
  `OpenMouthArt`; `nameWords_Odd`/`_Even`, `nameWords_EQ<n or SVar>`.
- **Printed lines of text** (`rulelines=`/`textlines=`, front and back) — counted on every printing's card
  image by `_tools/rules-lines` (OCR), not estimated from Oracle text. `Card.isWordy()` (4+ lines of rules text,
  reminder text included) and `Card.getPrintedTextLines()` (whole text box, flavor included; -1 = unread).
  After a new set: `jobs.js`, `count.py` (reads only new printings), then `generate.js`.
- **Figures in the art** (`figures=solo|multi`, art menace) — Tagger's `solo` and two-or-more tags. A solo
  artwork that also shows an animal beside another kind of being is unknown (`Card.getArtFigures()` 0).
  Properties `ArtFiguresSolo`, `ArtFiguresMultiple`, `ArtFiguresUnknown`, and `ControlledByAI` for
  honor-system facts the AI can't check.
- **Hats in the art** (`hat`, Goblin Haberdasher) — confirmed hats only; `HatInArt` also counts the controller's
  answer to `GameAction.askHatInArt`, kept on the card (`Card.getHatInArtClaim()`).
- **Trees in the art** (`tree`, Selfie Preservation) — Tagger's `tree` tag and its whole subtree (dead, fallen,
  Treefolk...) plus `bonsai-tree`; `tree-stump` isn't in it. Property `KnownTreeInArt`; untagged art is asked
  about where it matters.
- **Booster Fun** (`boosterfun`, Collectigull) — Scryfall's promo type; property `BoosterFun`.

### Five-face split cards

Only Who // What // When // Where // Why has ever needed this.

- `CardStateName.Split3/4/5` and a shared `SPLIT_STATES` list; every hardcoded LeftSplit+RightSplit
  pair iterates the states a card actually has, so **two-face splits take an identical path**.
- `ALTERNATE` advances to the next face instead of always landing on face 1.
- Combined name, mana cost, color, color identity, type and oracle text fold in every face.
- Fuse, Aftermath and Rooms are deliberately left two-face.
- Split-state views are populated in a **separate pass** from ability-text rendering, because
  rendering one face can read a sibling.
- Network delta sync encodes a card-state key as `cardId * CSV_STATE_SLOTS + ordinal`; the fork
  widens `CSV_STATE_SLOTS` from upstream's 16 to 32 (`DeltaPacket`), since the three extra states
  push `CardStateName` to 18 values. Upstream's 16-slot guard otherwise fails `DeltaPacket`'s
  class init and breaks all network play. The client (`NetworkGuiGame`) decodes **every**
  `CardStateViewType` property as a slot reference, not just upstream's four named ones, so
  `Split3State`–`Split5State` arrive as real state views. Covered by `FiveFaceSplitNetworkTest`.

### Zones

- **Dual residency** (`Card.shadowZone`) — Yet Another Aether Vortex's top-of-library permanents are
  on the battlefield *and* in the library. Battlefield is the primary zone; the library keeps a
  shadow entry. Residency must end in `Zone.remove`, never `Zone.add`: leaving the battlefield hands
  a *copy* to the destination zone, so an add-side hook never sees the original.
  Masterful Ninja uses it with the hand as the second zone (`GameAction.putOntoBattlefieldAlsoInHand` /
  `endAlsoInHand` / `isAlsoInHand`, API `AlsoOnBattlefield`); `Card.isInZone(Hand)` sees that residency, and a
  control change keeps any second residency.
- **Unnoticed zone changes** — `AbilityKey.Unnoticed` in a move's params skips replacement effects, zone-change
  triggers and the "entered/left the battlefield this turn" records (Masterful Ninja).
- **Programs** (The Grand Calcutron) — a hand that is an ordered row of revealed cards: `Player.hasProgram`,
  `ReorderZone | Program$ True`, `OnlyFirstOfProgram$` on CantBeCast/CantPlayLand, static `PlaceInProgram`,
  `PlayerController.chooseProgramPosition`.
- **Booster pile** (Myntasha, Honored One) — `ZoneType.BoosterPile`, outside the game; filled by cost
  `OpenBooster<1>`, dug by keyword `Booster cascade`.

### Pink, a sixth color

`MagicColor.PINK` (`1 << 5`) is a real color bit, deliberately left out of `ALL_COLORS` so nothing
that iterates the five touches it — but a pink permanent is **not** colorless. `ColorSet` gained 32
constants at indices 32–63 to preserve its ordinal-equals-mask invariant. Only Water Gun Balloon
Game's Giant Teddy Bear is pink; "choose a color" prompts still offer exactly five.

### Printed text vs. errata (R&D's Secret Lair)

- `StaticAbilityMode.IgnoreErrata` + `StaticAbilityIgnoreErrata` — is the Lair out, and does *this
  printing* predate a given errata date.
- Properties `PrintedTextActive`, `PrintedBefore <yyyyMMdd>`, `SourcePrintedBefore <yyyyMMdd>`
  (the last asks about the card doing the checking, for an Aura's Enchant restriction).
- The one broad rule is templating: a card printed before Dominaria reads "target creature or
  player" and can't hit planeswalkers or battles. Everything else is per card, on the card.

### Infinity

- **Infinite mana** — `ManaPool.addInfinite(color, ...)` (Mox Lotus's `addInfiniteColorless()` is now the colorless
  case): a genuine per-color flag, not a large number. Spending an unbounded color refills it, and paying one point of
  any shard with it settles the whole shard (`{∞}`, `{R}×∞` in the payment prompt). It clears with the pool and
  displays as ∞. A produced amount that's infinite becomes `Infinity-<color>` tokens (`GameActionUtil.generatedMana`);
  infinite combo mana is unbounded mana of every offered color.
- **Infinite quantities** (`forge.util.Infinity`, Infinity Elemental) — the engine counts in ints, so ∞ is the reserved
  value `Infinity.VALUE` (2^24), and anything past `Infinity.THRESHOLD` (2^20) reads as infinite: small enough that
  sums and AI evaluations can't overflow, big enough that no finite game gets there. `Infinity.add/subtract`
  saturate, `Infinity.format` shows ∞ / -∞. Wired through power and toughness (`PT:∞/5`; boosts and counters don't
  change an infinite value, a set value replaces it), life (gains and losses don't change infinite life, any amount
  can be paid from it, setting it works), damage, counters (removing some of infinitely many leaves infinitely many
  but counts as removed; removing infinitely many empties them; per-counter triggers fire once for an infinite batch)
  and display (P/T, life, damage, counters, the damage assignment dialogs, desktop and mobile). Mobile fonts are baked
  from a fixed character list, so ∞ is added to it (`FSkinFont.getCharacterSet`, `Assets.COUNTER_FONT_CHARS`); the
  font-cache marker is `v2-inf`, so fonts cached without it regenerate once.
- **X = ∞** — offered when unbounded mana that can pay X is floating, or a source of it (Mox Lotus) can still be
  activated (`ManaPool.canPayInfiniteX`).
- **Infinitely many objects or repetitions** — tokens, copies, "do this X times", dice, coin flips, Clues, explores,
  proliferates and the like ask the controller for a finite number instead (`SpellAbilityEffect.finiteAmount`,
  `PlayerController.chooseFiniteForInfinite`; the AI picks `PlayerControllerAi.FINITE_FOR_INFINITE`, 30). Drawing
  infinitely many draws the library and then fails the draw that loses.

### Two-Headed Giant

- **Giant teams** (`Game.formGiantTeam`, `Game.isGiantTeam`, `Player.isOnGiantTeam`/`sharesTurnWith`/`getGiantTeam`)
  — CR 805 shared team turns and 810, for every team in a `GameType.TwoHeadedGiant` game or the team Better Than One
  forms mid-game. One turn: `PhaseHandler.isPlayerTurn` is true for every head, `getActivePlayers()` lists them
  (primary first), and each head untaps, draws, plays a land, discards to hand size, and gets its "that player"
  phase triggers (`TriggerPhase`/`TriggerTurnBegin` fire triggers naming no player once, on the primary's run).
  Turn hand-off skips teammates (`nextTurnAfter`); a skip or added step one head gets, the team gets (805.8).
- **Shared life** — teammates hold one `Player.LifeTotal` (`joinLifeTotal`, `getLifeSharers`), so gains, losses and
  payments still happen to each player (triggers, replacements, life lost this turn) while the total is the team's.
  Can't-gain/can't-lose-life (810.9g/h) and can't-lose/can't-win the game (810.8a) are team-wide.
- **Shared poison** — counters stay on each player; `getPoisonCounters()` is the team's sum, removals come off
  teammates when needed (810.10b), `getOpponentsTotalPoisonCounters` counts a team once, and a team of n loses at
  `getPoisonCountersToLose()` = 5n + 5.
- **Team combat** — each head declares into one combined attack (805.10b); a defender may block creatures attacking
  any player on their team (805.10d, `CombatUtil.canBeBlocked`). AI blocks and danger checks count attackers aimed
  at teammates (`ComputerUtilCombat.getAttackersOfTeam`).
- **Starting team** — whoever starts, their team's primary player takes the first turn (103.1a, 805.2); mulligans
  are declared by everyone, starting team first, then taken together (`MulliganService.runTeamMulligans`, 805.3a).
- **Proliferate** — one poison counter per team however many heads are chosen (701.34b); heads count their team's
  poison as theirs (810.10d).
- **Team wins and losses** — a head losing takes the team with them, a head winning doesn't make teammates lose
  (`GameAction.checkGameOverCondition`). Teammates see each other's hands (810.5, `PlayerView.isGiantTeammateOf`).
  Every player on the winning side scores the game (`GameOutcome.getWinningPlayers`; `Match` tallies use `isWinner`).
- **Table layout** — each team sits in one half of the screen: on mobile the local player's teammates are the panels
  just above them (`MatchController.seatGiantTeamsTogether`); on desktop the local team's fields go in the bottom cell
  and the others' in the top cell whatever the multiplayer-layout preference (`CMatchUI.sortPlayersForGiantTeams`,
  `VMatchUI.giantSide`), though fields can still be moved by hand.
- **Lobby variant** (desktop and mobile): equal lobby teams of 2+, or seats 1+2 vs 3+4 when every slot is on its
  own team; teammates are seated together; 30 life (+15 per head beyond two); the starting team skips its first draw.
- **Players joining mid-game** (`Game.addOutsidePlayer`, `OutsidePlayers` factory plugged in by FModel) — an AI player
  seated beside a team, appended to the registered players (which match registrations by position) with their own
  `RegisteredPlayer`; `GameEventPlayerAdded` gives them a field on desktop and a panel on mobile.

### Other

- `DB$ RememberNumber` — writes a number into the remembered set (which is a `Set`, so repeats
  collapse).
- `NotifyMessage$` works on **any** ability, not only dice rolls.
- `UnlessPaidSubAbility$` — fires when an Unless cost *is* paid.
- `AddTrigger$`, `RandomSet`, `Staying Power`, additional activation zones, `Rotate180` display for
  tokens printed inverted.
- Dev mode can pick which printing to add.
- **Several dice rolled for one** (`ReplaceEffect | VarName$ CombinedDice`, The Big Idea): a `RollDice`
  replacement that turns the first die into extra dice whose total is its result; roll triggers still see each
  physical die.
- **Last strike** (`K:Last Strike`, Unstable) — a further combat damage step after the regular one, run as a
  repeat of `COMBAT_DAMAGE` rather than a new `PhaseType`.
- **AI: no doubling up on pending twins** (`ComputerUtil.targetedByPendingTwins`) — simultaneous triggers are
  targeted one at a time with the earlier ones already on the stack, so effects that set rather than add (Animate,
  keyword-only Pump) skip cards an identical pending ability of the AI's already targets.
- **K: lines keep upstream's text.** The engine parses a K: line's head into a `Keyword` and looks sentence
  keywords up by their exact text, so Oracle wording for them is display-only, from
  `CardFactoryUtil.keywordAsPrinted`. `KeywordScriptTextTest` enforces it.
- **"Turn to its other face"** (`SetState | Mode$ OtherFace`, Optimus Prime, Inspiring Leader): a face-down
  permanent turns face up, a double-faced one (MDFCs included) converts under 701.28a, anything else is skipped.
- **Legendary partner** (`K:Legendary partner`, Sol, Advocate Eternal): the other commander only has to be a
  would-be commander (`CardRules.canBeCommanderIfLegendary`, `DeckFormat.isLegalCommanderWith`) and is made
  perpetually legendary at game start (`Player.promoteLegendaryPartners`). Card property `IsPartner` = the
  source's owner's other commander ("its partner").
- **Friends** (Nightmare Moon): `Player.addFriend` (mutual, all game, logged), player property `Friend`, copied by
  GameCopier. `BecomeFriend$ True` on an ability makes its activator and payment helpers the host controller's
  friends when it goes on the stack.
- **Anyone helps pay** (`AnyPlayerHelpsPay$ True`): Assist's payment (702.132a) offered to every other player in
  turn order, for activated abilities too; helpers are recorded on the ability (`SpellAbility.getPaymentHelpers`).
- **Play with permission** (`MayPlayWithPermission$ <players>` on a MayPlay static, Princess Luna):
  `MayPlayPermission.ask` asks them as a human or AI starts casting; a refusal holds for the turn. AI lends to
  non-opponents only.
- **Real-world nighttime**: `Count$RealWorldNighttime` (6 PM to 6 AM local); `AbilityUtils.realWorldTime` is the
  clock (tests replace it, `CurrentHour` reads it too).
- **Moon in the art**: printing trait `moon`/`backmoon` (Tagger's moon tree plus lunar eclipse), card property
  `KnownMoonInArt`.
- **Toy colors** (`Protection | Gains$ ToyColors`, Rarity): a human picks the colors at resolution (pink and gold in
  a silver-bordered game); `PlayerController.getKnownToyColors` gives the AI's toy, fixed per game
  (`SpellAbilityAi.gameSeed`).
- **Bank** (`K:Bank`, Memory Bank): resolving after its owner cast it from their hand (`ValidLKI$` on the move
  replacement, since the moving card is already the new object), it's exiled and marked banked (`Card.getBank`). When
  a game ends, `Match.recordBank` notes what bank still has exiled; the next game starts those cards in exile
  (`Match.startBanked`), castable from there by their owner, and once cast they're ordinary spells.
- **Ransom** (Squidnapper): GainControl `LoseControl$ Ransom` records whose creature it took
  (`Card.addRansom`); `ST$ Ransom | Activator$ Player.HeldForRansom` is the special action that player takes to pay
  and end those control effects (`Card.payRansom`). The AI pays when the creature is worth it and the life can be
  spared.
- **Four-faced** (`K:Four-faced`, Loopy Lobster): a permanent's stage (`Card.getStage`, 1 until it first moves; a status of
  the object, so a new object starts over and a copy keeps its own). Becoming tapped triggers `DB$ AdvanceStage`
  (1→2→3→4→1). Static abilities take `Stage$ N` for that stage only, and the stage shows on the card's marker line.
- **Maximum hand size as a condition** (Learned Learner): player property `maxHandSize <cmp><n>` (no maximum counts
  as larger than any number), worked out from the hand-size effects themselves
  (`StaticAbilityContinuous.projectedMaxHandSize`), since a condition checked in an earlier layer would otherwise see
  them half-applied. Static abilities now take `CheckDefinedPlayer$`, as triggers and abilities already did.
- **Cast as though it were another card** (`S:Mode$ CastAs | ValidCard$ … | Activator$ … | AffectedZone$ Hand |
  CastAs$ <name>`, Form of the Mulldrifter): each matching card gets the named card's spells, alternative costs
  included (`SpellAbility.getCastAs`). Cast that way, it takes on the named card's copiable values as it goes on the
  stack (a clone state, like Prototype), so they last until it leaves the stack other than by resolving or leaves the
  battlefield.
- **Restart the turn** (`SP$ RestartTurn`, Do-Over): `forge.game.TurnRestart` keeps the game as it stood between
  the last turn and this one (only in a game with a card that can restart a turn) and puts it back: the same card and
  player objects, field by field, so effects from before the turn that refer to them still work. A restart waits for
  the spell to finish resolving (`PhaseHandler.requestTurnRestart`, run at the end of `MagicStack.resolveStack`),
  exiles the card, then begins the turn again with its untap step. Nothing triggers while the turn is undone.
- **Scrycast** (`K:Scrycast:<cost>`, Biting Remark): in `GameAction.scry`, after the cards are seen and before they're
  arranged, each Scrycast card may be cast for its cost (a Play effect, so the AI decides like any free cast); the rest
  are scried as usual and no further card is looked at (ruling).
- **Auras on spells, casting from the battlefield** (Animate Spell): an Aura spell can target a spell on the stack
  (`SVar:AuraSpell` with `TgtZone$ Stack`; AttachEffect attaches to a targeted spell's card). ChangeZone
  `AllowNonPermanent$ True` puts an instant or sorcery onto the battlefield when a card says so (101.1 over 304.4).
  Play `CastFromBattlefield$ True` casts a card that's on the battlefield (`SpellAbility.setCastFromBattlefield`,
  honored by `Spell.canPlayFromHost` and the zone check); an animated instant isn't cast as a creature spell. Player
  definition `AttachedBy <cards>Owner/Controller`.
- **Combat damage on the stack** (`S:Mode$ CombatDamageUsesStack`, `forge.game.combat.CombatDamageOnStack`): while a
  permanent with it is on the battlefield, each damage step's assignment goes on the stack as one object controlled
  by no player (`MagicStack.addCombatDamage`: no cast triggers, not counted as activated) and is dealt when it
  resolves. A source that left deals its damage as it last existed; damage to a permanent that left or stopped being
  a creature/planeswalker/battle isn't dealt (rulings).
- **Latest Pro Tour winning deck** (`forge.card.WinningDeck`, card property `InLatestWinningDeck`): filled by
  `forge.model.MetagamerDeckLoader` at startup from the user-folder cache (`metagamer-winning-deck.txt`) or the
  shipped `res/lists/MetagamerWinningDeck.txt`, then refreshed at most daily in the background from magic.gg: the
  event archive's newest finished "Pro Tour" (players are listed in finishing order, so the first is the winner),
  that event's Top 8 deck page (the finals' format), and the winner's `<main-deck>`. Offline keeps the old deck.
- **Counts as N creatures** (`S:Mode$ CountsAsCreatures | Amount$ 5`, `CountsAsCreatures.count`): weighs the card
  in counts whose valid restriction is creatures restricted to a controller (`Count$Valid`, per-player `Valid`
  counts, `CreaturesInPlay`, and the IsPresent checks of triggers, statics, conditions and restrictions). Java-coded
  counts (`getCreaturesInPlay().size()` in individual effects or AI) still see one.
- **Players with a subtype** (`S:Mode$ PlayerType | Affected$ You | Type$ Flagbearer`,
  `StaticAbilityPlayerType.hasType`). **MustTarget `IncludePlayers$ True`**: such players meet the requirement too;
  requirements for the same type and zone merge. The human targeting input keeps allowed players clickable
  (`InputSelectTargets.setMustTargetPlayers`), and `StaticAbilityMustTarget.filterMustTargets` filters players too.
- **Vanguard requirements** (`VanguardRequirement:Instant,Sorcery,Land`, Ral's Vanguard): every starting-deck card
  (the main deck, not the sideboard) must have one of the types, or the vanguard is left out at game start with a
  log line (`RegisteredPlayer.getUnmetVanguardAvatars`).
- **Cause of a draw**: DrawCards replacements get the drawing ability as `Cause`, so `ValidCause$` works on them.
  Ability property `CastSpell` = a spell that was cast, not a copy.
- **Everypony wins**: `WinsGame | Defined$ Player` no longer turns the other winners into losers; GameWinAi skips a
  win whose condition doesn't hold.
- **Paired booster slots** (`Pair=<card>|<card>` lines in an edition's booster-slot section, Reality Fracture's
  echoed pairs): the slot deals one whole pair, the partner taken from the same sheet as the card that rolled
  (so borderless brings borderless), and fills its other cards from different pairs
  (`BoosterGenerator.generatePairedSlot`).
- **Partners open together** (edition metadata `BoosterPartnersTogether=True` and `UncommonPartnerPairChance=<%>`,
  Battlebond; `BoosterGenerator.keepPartnersTogether`): a "partner with" card in a pack brings its partner, with the
  same foil state and in collector number order. A rare or mythic pair takes an uncommon's place, a foil pair a
  common's, and packs without a rare pair get an uncommon pair at the given chance in place of two uncommons. Sheet
  operator `PartnerWith` matches cards with "partner with" (`Uncommon:!PartnerWith` keeps them out of a slot).

---

## Log

### Unreleased

- **Reality Fracture play boosters deal real echoed pairs** (`RealityFractureEchoedPairsTest`). Upstream's three
  EchoedPair cards were three independent picks (its edition file said "known to be incorrect, but currently unable to
  assign these cards as a pair"). Now each pack has one whole pair plus a card from a different pair, as Wizards'
  *Collecting Reality Fracture* describes; each card keeps the slot's published odds (uncommon 88.9%, rare 6.7%,
  mythic 1.4%, borderless). The 43 pairs come from mtg.wiki's echoed pairs table, checked against Scryfall: each pair
  shares its rarity and artist, and together they're exactly the 86 cards at FRA #195-280. Draft, sealed and the
  prerelease use it. Also fixed two misspelled names in the echoed pair sheet ("Lyla, Tolarian Archangel" and
  "Winter, Tormented Lover"): the sheet dropped them, so Lyra and Winter never came from the echoed pairs slot and
  were dealt by the ordinary rare and uncommon slots instead.
- **Battlebond packs keep partners together** (`BattlebondPartnersTest`). Partner pairs always open in the same pack
  (mtg.wiki; Gavin Verhey: a foil partner's partner is foil too), and upstream dealt each card on its own. Rates and
  layout follow the lethe.xyz collation project, the only pack-level study found (at least 9 boxes): a rare or mythic
  pair in 1 of 10 packs, with two uncommons instead of three; an uncommon pair, from its own run, in 29 of 139 packs,
  taking two uncommon slots; never both in one pack. The rare sheet now gives each pair one sheet position and rares
  twice a mythic's weight, which yields exactly Lethe's 1/10 and its assumed 7/55 mythic rate. Upstream weighted
  each rare 8 times a mythic, so a mythic turned up in about 1 pack in 29. Still not handled: a foil Will and Rowan
  pair uses the regular #1/#2 printings instead of the foil-only alternate art (#255/#256), and a foil uncommon partner
  pair can't open, since the uncommon sheet the foil is drawn from leaves partners out.


- **Set review: Limited Edition Alpha** (all 290 names, `LimitedEditionAlphaAuditTest`).
  - Rock Hydra's `{R}{R}{R}` and Cyclopean Tomb could be activated in an opponent's upkeep ("Activate only
    during your upkeep" had `ActivationPhases$ Upkeep` without `PlayerTurn$ True`).
  - Chaos Orb flipped (animation and destruction) even if it had left the battlefield before resolving; the flip
    now needs it still there, as the Oracle text says.
  - Meekstone's AI play check used `PowerGE3` (no such property), so the AI never cast it.
  - Mana Vault's draw-step trigger had no `TriggerZones$`; Ivory Cup's life-gain deck hint was filed as a
    keyword; in-game text: Chaoslace's reminder, Flight's stray reminder, and the trigger Farmstead grants (it
    read "Enchanted land has ..." on the land itself).
- **R&D's Secret Lair: Limited Edition Alpha printed text** (`SecretLairAlphaPrintedTextTest`). Every LEA card's
  printed text was compared with its Oracle text using MTGJSON `originalText` for every paper printing (Scryfall
  lists the printings); each change below is gated on `PrintedTextActive` plus `PrintedBefore` the first printing
  that carried the corrected text. 30th Anniversary Edition reprints use modern wording (mtg.wiki), so the cutoffs
  treat them as modern. Printed variants of statics use `ActiveDescription$`, so they show only while they apply.
  - *Castle* (before 6ED): attacking creatures lose the bonus.
  - *Consecrate Land*: its printed "All enchantments on target land are destroyed" was briefly added as an
    enters trigger and then removed again - the Oracle "can't be enchanted by other Auras" already sends them to
    the graveyard through the state-based check (`GameAction`'s attach legality pass).
  - *Animate Artifact* (before 3ED): also sets an artifact creature's P/T to its mana value.
  - *Clone* (before ONS): can't be cast while no creatures are on the battlefield.
  - *Volcanic Eruption* (before 3ED): deals X damage, however many Mountains actually died.
  - *Nether Shadow* (before 3ED): returns only if you pay its casting cost, {B}{B}.
  - *Lord of Atlantis* (before TSB) and *Zombie Master* (before 30A): "All Merfolk/Zombies in play", themselves
    included.
  - *Earthbind* (before 3ED): can target only a flying creature (a targeting restriction, not Enchant, or it would
    fall off once it removed the flying).
  - *Goblin Balloon Brigade* (before 3ED): `{R}` gives every Goblin flying.
  - *Manabarbs* (before 3ED): any land becoming tapped, for mana or not, damages that land's controller.
  - *Living Artifact* (before 4ED): counts life lost rather than damage, and the counters go on the artifact.
  - *Kormus Bell* (before 4ED): the Swamps are colorless.
  - *Mana Vault* (before 3ED): `{4}: Untap` works at any time instead of the upkeep trigger. Its printed damage
    ("if Mana Vault remains tapped during upkeep") is what the draw-step trigger already models.
  - *Island Sanctuary* (Alpha only; Beta changed it): creatures without flying or islandwalk can't damage you,
    rather than not attack you.
  - *White Ward* (before 4ED): nothing exempts White Ward from the protection, so it falls off.
  - *Gloom* (before 3ED): the extra {3} applies to Circles of Protection, whatever their color, not to white
    enchantments.
  - *Warp Artifact*, *Cursed Land*, *Feedback* (before 3ED): damage at every player's upkeep ("each turn" /
    "each upkeep").
  - *Demonic Attorney*: each opponent may concede first ("If opponent doesn't concede the game immediately");
    the AI never does.
  - *Lich*: each point of damage destroys (not sacrifices) one of your nontoken permanents, without regeneration,
    and you lose if a point goes unpaid; you lose when Lich is destroyed, not when it's sacrificed.
  - Left as Oracle on purpose: differences the old rules made moot (Mono Artifact abilities tapped implicitly,
    tapped Continuous Artifacts were off, walls couldn't attack, "interrupt" speed), "destroyed" vs "sacrificed"
    on upkeep costs, and texts whose literal reading is unclear: "during upkeep" without "your" (Rock Hydra,
    Cyclopean Tomb, Black Vise), Reverse Damage's retroactive gain, Sacrifice's "black creature", Personal
    Incarnation, Camouflage and False Orders' old mechanics, and Lich's missing "you don't lose at 0 life" (the
    card can't work without it).
- **Set review: Limited Edition Beta** (its two new cards, Circle of Protection: Black and Volcanic Island) - no
  changes needed.
- **Set review: Arabian Nights** (all 77 new names) - Giant Tortoise's in-game text now reads as its Oracle text
  ("This creature gets +0/+3 as long as it's untapped."). Checked and left as they are: Drop of Honey skipping
  indestructible creatures matches its 2021 ruling; Nafs Asp's effect removes itself when its draw-step trigger
  fires (`OneOff$`).
- **Set review: Antiquities** (all 85 new names) - scripts needed no changes. R&D's Secret Lair printed text
  (`SecretLairAntiquitiesPrintedTextTest`, same method as Alpha):
  - *Gate to Phyrexia*: no "only once each turn" (`ActivationLimit$` is now an SVar).
  - *Golgothian Sylex*: takes "all cards from the Antiquities expansion" - ATQ printings (`PrintingIs ATQ`), not
    every card with an Antiquities name.
  - *Onulet* (before 4ED) and *Su-Chi*: "If it goes to the graveyard" - from any zone, not just when it dies.
  - *Shapeshifter* (before 4ED): the number is 0 to 6.
  - *Tawnos's Wand* (before 4ED): artifact creatures can still block the creature.
  - Left as Oracle: Mono Artifact abilities (tap implied), Armageddon Clock's "end of your upkeep" (the draw-step
    trigger is that), Titania's Song lasting until the next untap step (only the cleanup step differs), Hurkyl's
    Recall's timing line, and the old mechanics of Tawnos's Coffin, Transmute Artifact and Goblin Artisans.
- **Set review: Celebration Cards (PCEL)** - the four scripted cards (1996 World Champion, Deb Thomas, Robot Chicken,
  Shichifukujin Dragon) needed no changes; Fraternal Exaltation, Phoenix Heart, Proposal and Splendid Genesis have
  no script yet.
- **Set review: Legends** (all 310 new names, `LegendsAuditTest`):
  - *Glyph of Destruction* prevented the combat damage the Wall dealt (`ValidSource$`) instead of all damage dealt
    to it, cancelling its own +10/+0, and could target an opponent's blocking Wall. Now `ValidTarget$`, any
    damage, and `+YouCtrl`.
  - *Axelrod Gunnarson*'s "creature it damaged dies" trigger had no `TriggerZones$`, so it also fired from the
    graveyard or hand.
  - R&D's Secret Lair printed text: *Blight* (before 6ED) destroys the tapped land at the beginning of the next end
    step; *Greed* (before 4ED) is `{B}`: draw a card and lose 2 life (no life payment, so it works at 1 life);
    *Kobold Drill Sergeant* and *Kobold Taskmaster* (before TSB) pump themselves too ("All your Kobolds");
    *Tempest Efreet* lets the opponent concede the game instead.
  - Left as Oracle: Anti-Magic Aura's printed "All enchantments on target creature are destroyed" (the
    state-based check already removes them), Wall of Tombstones (printed 0/1+*, same as Oracle), and the old
    mechanics of Imprison, Enchantment Alteration and Gaseous Form.
- **Set review: Dragon Con (PDRC)** - Nalathni Dragon needed no changes.
- **Set review: The Dark** (all 119 new names, `TheDarkAuditTest`):
  - *Rag Man* could target any player, itself included; Oracle says target opponent.
  - R&D's Secret Lair printed text: *Blood Moon* (The Dark only; Chronicles changed it) makes nonbasic lands
    *basic* Mountains; *Eater of the Dead* (before MB2) has no "if it's tapped", so it works untapped too;
    *Orc General* (The Dark's printing) can sacrifice itself and pumps all Orcs, itself included.
  - Left as Oracle: Ball Lightning's "buried at the end of the turn it's summoned" (the end-step sacrifice is
    the same in practice) and Mana Clash's "target player".
- **Set review: Fallen Empires and HarperPrism promos (PHPR)** (107 new names, `FallenEmpiresAuditTest`):
  - *Thelon's Curse*'s choice prompt asked for "tapped nongreen creatures"; it's tapped blue ones.
  - R&D's Secret Lair printed text: *Thelonite Monk* (Fallen Empires) turns the land into a *basic* Forest.
  - Left as Oracle: Night Soil's "two creatures in any graveyard" (reads either way; Commander 2013 settled it as
    a single graveyard), and "bury"/"destroy" upkeep wording (Deep Spawn, Seasinger).
- **Set review: Ice Age** (all 346 new names, `IceAgeAuditTest`):
  - *Word of Blasting*'s Wall could be regenerated; Oracle says it can't.
  - *Dwarven Armory* worked only in your own upkeep; it's "any upkeep step".
  - In-game text: *Orcish Farmer*'s ability had no description, *Mind Warp* now reads as its Oracle text, and
    *Reality Twist*'s Mountain line said the wrong color.
  - R&D's Secret Lair printed text: *Enduring Renewal* (before TSB) draws the creature card and then discards it;
    *Zur's Weirding* (before 6ED) lets the card be drawn, and another player's 2 life makes its owner discard it;
    *Hecatomb* (Ice Age only; 5ED added "or bury Hecatomb") just sacrifices four creatures and stays;
    *Pit Trap* (before USG) needs the creature to be attacking you.
  - Left as Oracle: the Walls printed without defender (Illusionary Wall, Thunder Wall, Wall of Lava, Drift of the
    Dead), Auras' old "choose target land" and "{0}: Tap land ... enchants" templating (Caribou Range, Mystic Might,
    Earthlore, Forbidden Lore, Hot Springs), "target opponent" on permanents, Necropotence's discard-phase return
    and "remove from the game" discards, the "when attack or defense is announced" timing lines, and the old
    mechanics of General Jarkeld, Melee, Dance of the Dead and Touch of Vitae.
- **Set review: Homelands** (all 115 new names, `HomelandsAuditTest`) - scripts needed no changes. R&D's Secret Lair
  printed text:
  - *Faerie Noble* (Homelands' printing): "All Faeries you control" - both bonuses include itself.
  - *Anaba Ancestor* and *Baron Sengir* (Homelands' printing): "target Minotaur" / "target Vampire", so they can
    target themselves.
  - *Sengir Autocrat* (before 6ED): the Serfs are buried (destroyed, so they die) rather than exiled.
  - *Apocalypse Chime* (Homelands' printing): buries "all cards from the Homelands expansion" - Homelands printings
    (`PrintingIs HML`), not every card with a Homelands name.
  - Left as Oracle: creature types renamed since (Soraya the Falconer's Falcons, Aysen Crusader's Heroes),
    "bury" for "sacrifice" (Retribution, Hungry Mist), Giant Oyster's upkeep counter (now the draw step), targeted
    old templating (Coral Reef, Drudge Spell), Daughter of Autumn's untargeted "a white creature", and the old
    mechanics of Giant Albatross, Chain Stasis and Timmerian Fiends.
- **Set review: Alliances** (all 144 new names, `AlliancesAuditTest`):
  - *Wandering Mage*'s {U} ability could target a noncreature Cleric or Wizard (a kindred card); it's "target
    Cleric or Wizard creature". *Tidal Control*'s target prompt said "red of green".
  - R&D's Secret Lair printed text:
    - *Heart of Yavimaya*, *Lake of the Dead*, *Kjeldoran Outpost*, *Balduvian Trading Post* and *Soldevi
      Excavations* (Alliances is their only paper printing): "When it comes into play, sacrifice a land or bury it"
      is a trigger, so the land is in play first and can be tapped for mana in response. *Sheltered Valley*
      likewise buries the other Valleys with a trigger. Same handling as Lotus Vale.
    - *Soldevi Sentry* (Alliances' printing): the opponent may draw whether or not it regenerates.
    - *Mystic Compass* (before 6ED): targets only a mana-producing land.
  - Left as Oracle: Martyrdom's old redirect mechanics, Burnout's "interrupt", Phyrexian Devourer's single
    "+X/+X counter", untargeted "any creature" prevention (Soldevi Heretic, Wandering Mage), targeted old costs
    (Balduvian Dead, Benthic Explorers), Lord of Tresserhorn's "pay 2 life", and Seasoned Tactician's "all damage
    ... from one source" (Pentagram of the Ages shows it meant the next damage).
- **Set review: Mirage** (all 313 new names, `MirageAuditTest`):
  - *Radiant Essence* only noticed an opponent's black *creature*; it's any black permanent.
  - *Kukemssa Pirates*' unblocked trigger forced you to take an artifact (and lose the combat damage); it's "you
    may". *Wall of Resistance*'s end-step trigger now only runs from the battlefield.
  - In-game text: *Cadaverous Bloom*'s {B}{B} ability read "Add {B}{B} or {G}{G}", and *Subterranean Spirit*'s
    stack text said "each creature and without flying".
  - R&D's Secret Lair printed text:
    - *Lion's Eye Diamond* (Mirage's printing): "Play this ability as a mana source" - a true mana ability, usable
      while paying for a spell, without "Activate only as an instant".
    - *Psychic Transfer* (before 6ED): also needs you to have at least 1 life.
    - *Forbidden Crypt* (before 6ED): exiling your cards is a trigger ("Whenever a card is put into your
      graveyard"), so they do reach the graveyard first - your creatures die.
  - Left as Oracle: the "target opponent" choices on permanents, upkeep/draw-step timing lines (Misers' and
    Paupers' Cage, Preferred Selection), "bury" for "sacrifice" (Basalt Golem, the flash Auras), Unyaro Griffin's
    old interrupt targeting, Infernal Contract's life "cost", Consuming Ferocity's end-of-upkeep check, and
    Shadowbane/Reflect Damage/Bone Mask's "all damage from one source".
- **Set review: Multiverse Gift Box and Visions** (167 new names, `VisionsAuditTest`):
  - *Mob Mentality* pumped by the number of non-Wall creatures you control; it's the number of attacking creatures.
    *Equipoise*'s artifact prompt read "Choose a artifact".
  - R&D's Secret Lair printed text: *Rowen* (before 6ED) reveals only the first card you draw during your draw
    phase, not the first card each turn.
  - Left as Oracle: Blanket of Night's "each mana-producing land", costs that were old templating (Chronatog,
    Vampiric Tutor), Relentless Assault's "additional attack during your main phase", Sands of Time's "beginning
    of each player's turn", Breathstealer's Crypt's draw trigger, and Necromancy's old mechanics.
- **Set review: MicroProse promo (Aswan Jaguar) and Portal** (159 new names, `PortalAuditTest`):
  - *Gift of Estates* added up all opponents' lands; it's "an opponent controls more lands than you", so it now
    compares against the opponent with the most (as Land Tax does).
  - R&D's Secret Lair printed text:
    - Portal's "When CARDNAME comes into play from your hand" - the trigger doesn't happen when the card enters
      from anywhere else: *Spiritual Guardian*, *Mercenary Knight*, *Fire Dragon*, *Fire Imp*, *Pillaging Horde*,
      *Plant Elemental*, *Thundering Wurm* (Portal only), *Venerable Monk* and *Serpent Warrior* (before STH),
      *Gravedigger* (TMP), *Thundermare* (WTH), *Wood Elves* (EXO), *Ingenious Thief*, *Owl Familiar* and
      *Dread Reaper* (S99), *Primeval Force* (8ED), *Ebon Dragon* (DRB) and *Serpent Assassin* (J25).
    - *Blessed Reversal* (before ULG): "Play only after you're attacked, before you declare interceptors."
  - Left as Oracle: Portal's two-player wording ("your opponent", "you" for target player - Mind Rot, Natural
    Spring, Lava Axe, Final Strike), "any one" meaning target, and "destroy" for sacrifice on the Forest/Island
    costs (Plant Elemental, Primeval Force, Thing from the Deep).
- **Set review: Weatherlight** (all 161 new names, `WeatherlightAuditTest`):
  - In-game text: *Betrothed of Fire*'s first ability read "Enchanted creature get  +2/+0".
  - R&D's Secret Lair printed text:
    - *Scorched Ruins* (Weatherlight is its only paper printing): "When it comes into play, sacrifice two untapped
      lands or bury it" is a trigger, as on Lotus Vale - it can be tapped for mana first.
    - *Fire Whip* (before TSB): "Tap enchanted creature:" is the Aura's own ability, so it works even while the
      creature has summoning sickness.
    - *Abeyance* (Weatherlight's printing): stops "abilities requiring an activation cost", mana abilities
      included.
  - Left as Oracle: the attacking-and-unblocked activated abilities that became triggers (Ophidian, Bone Dancer,
    Goblin Vandal, Goblin Grenadiers), "last paid cumulative upkeep" wording (Revered Unicorn, Mwonvuli Ooze, Wave
    of Terror, Heart of Bogardan - same numbers as the age counters), Doomsday's life "cost", "target opponent"
    on permanents (Aura of Silence, Mana Web), and the "all damage from one source" shields (Kithkin Armor,
    Desperate Gambit).
- **Set review: Tempest Promos (Dirtcowl Wurm) and Tempest** (299 new names, `TempestAuditTest`):
  - *Mindwhip Sliver*'s granted ability had no sorcery-speed restriction. *Thalakos Lowlands*' in-game text read
    "Add {W} or {U}. or {U}.".
  - R&D's Secret Lair printed text:
    - *Hand to Hand* (Tempest is its only paper printing): stops "abilities requiring an activation cost" during
      combat, mana abilities included (as on Abeyance).
    - *Flickering Ward* (before MB2): no "This effect doesn't remove Flickering Ward", so choosing white makes it
      fall off.
    - *Blood Frenzy* (Tempest only): no "Cast this spell only before the combat damage step".
    - *Escaped Shapeshifter* (Tempest only): no "not named Escaped Shapeshifter", so it copies abilities from
      another Escaped Shapeshifter too.
  - Left as Oracle: "target opponent" on permanents (Havoc, Warmth, Insight, Pallimud, Booby Trap, Maddening Imp),
    costs that were old templating (Meditate, Scorched Earth, Abandon Hope, Worthy Cause, Spontaneous Combustion)
    and "When you play" choices (Dracoplasm, Minion of the Wastes), creature types gained since (Eladamri, Selenia,
    Orim, Metallic Sliver, Mongrel Pack's Hounds), Static Orb's missing "as long as it's untapped" (tapped
    artifacts were off), Dauthi Mindripper and Soltari Guerrillas' abilities that became triggers, the "all damage
    from one source" shields (Circle of Protection: Shadow, Invulnerability), and Ertai's Meddling's old mechanics.
- **Set review: Judge Gift Cards 1998, Stronghold Promos (Revenant) and Stronghold** (139 new names,
  `StrongholdAuditTest`) - scripts needed no changes. R&D's Secret Lair printed text:
  - *Mox Diamond* (Stronghold's printing; From the Vault: Relics changed it): "When Mox Diamond comes into play,
    choose and discard a land card or sacrifice Mox Diamond" is a trigger, so it's in play first and can be tapped
    for mana in response.
  - *Skeleton Scavengers* (Stronghold only): puts the +1/+1 counter on as the ability resolves, whether or not it
    regenerates.
  - Left as Oracle: Pursuit of Knowledge's "Skip drawing a card:" (old templating for the replacement), Mogg
    Flunkies' "during a turn in which no other creature you control attacks or blocks" (differs only with extra
    combats), the "all damage from one source" redirects and shields (Shaman en-Kor, Samite Blessing, Hidden
    Retreat), Silver Wyvern's interrupt, Thalakos Deceiver's ability that became a trigger, Volrath's Laboratory's
    "When you play" choice, creature types gained since (Crovax, Sliver Queen, Shifting Wall, Hornet Cannon's Insect,
    Sword of the Chosen's "target legend"), and Volrath's Shapeshifter's old copy wording.
- **Set review: Exodus Promos (Monstrous Hound) and Exodus** (138 new names, `ExodusAuditTest`):
  - In-game text: *Survival of the Fittest* read "then shuffle. Then shuffle."; target prompts on *Oath of
    Scholars* ("more life than you" - it's cards in hand), *Oath of Lieges* and *Elven Palisade*.
  - R&D's Secret Lair printed text: *City of Traitors* (Exodus only) says "If you play a land" with no "another",
    so playing it triggers its own sacrifice.
  - Left as Oracle: the Keepers' and Oaths' "Play this ability only if" / "if ... target opponent" conditions (the
    same choice, made as a target), Scalding Salamander's ability that became a trigger, Thopter Squadron's and
    Mirri's creature types, discard-phase timing (Manabond, Necrologia, Spellbook), Cunning's "at end of turn",
    Scrivener's "instant or interrupt", interrupt speed (Ertai, Null Brooch), cost templating (Hatred, Aether Tide,
    Sonic Burst, Culling the Weak) and the "any one source" redirects and shields (Kor Chant, Penance).
- **Set review: Portal Second Age** (all 112 new names, `PortalSecondAgeAuditTest`) - scripts needed no changes.
  R&D's Secret Lair printed text:
  - "When CARDNAME comes into play from your hand", as on Portal - not from anywhere else: *Screeching Drake*,
    *Talas Explorer*, *Brutal Nightstalker*, *Foul Spirit*, *Predatory Nightstalker* and *Ogre Arsonist* (Portal
    Second Age only), *Goblin Matron* (before USG), *Ravenous Rats* (UDS), *Angel of Mercy* and *Denizen of the
    Deep* (S99), *Vampiric Spirit* (8ED), *Magma Giant* (5DN), *Temple Acolyte* (DDF) and *Sea Drake* (MH2).
  - *Return of the Nightstalkers*' printed reminder - "Treat these Nightstalkers as though they just came into play
    from your hand" - so Brutal and Predatory Nightstalker's triggers still happen when it returns them.
  - Left as Oracle: Portal's "destroy" for sacrifice (Cruel Edict, Predatory Nightstalker, Foul Spirit, Wildfire,
    Goblin Firestarter), two-player wording ("your opponent", Extinguish and False Summoning's "another player"),
    "any one" meaning target, and Goblin Matron's search without "may" (finding nothing was always allowed).
- **Set review: Unglued** (83 new names, `UngluedAuditTest`):
  - *Goblin Bowling Team* dealt no damage at all: a damage replacement only runs when its ability is a
    ReplaceEffect, and this one rolls the die first, so the event was buffered and erased. The replacement handler
    now runs any whose chain or choices hold a ReplaceEffect, and passes the replaced event to choices too. The
    same bug hit *Don't Try This at Home* (UNF) on a source whose art Tagger hasn't tagged - "It's hot" and
    "It isn't hot" both erased the damage; "It isn't hot" now leaves it as it is (not tested in game).
  - In-game text: *Free-for-All*'s trigger left out "face down".
  - R&D's Secret Lair printed text:
    - *Goblin Bowling Team* (Unglued only): a trigger - "Whenever Goblin Bowling Team damages any creature or
      player, roll a six-sided die. Goblin Bowling Team deals to that creature or player additional damage equal
      to the die roll." Read literally that extra damage triggers it again, an endless loop against anything that
      survives it, so the extra damage doesn't retrigger (a choice; no ruling covers it).
    - *Spatula of the Ages* (Unglued only): "any card from an Unglued supplement" - Unglued printings, not every
      silver-bordered or acorn card.
  - Left as Oracle: Urza's Contact Lenses' missing "as long as it's untapped" (tapped artifacts were off), the
    honor-system wording changes (Sex Appeal's "players in the room of the opposite sex", Miss Demeanor's "during
    each other player's turn"), Chicken creature types (Fowl Play, Chicken à la King, Chicken Egg), Denied!'s "only as
    any opponent casts", "When you play" templating (Jumbo Imp, Temp of the Damned, Elvish Impersonators) and the
    "any one source" shields (Knight of the Hokey Pokey, Clay Pigeon).
- **Set review: Urza's Saga Promos (Lightning Dragon) and Urza's Saga** (307 new names, `UrzasSagaAuditTest`):
  - *Soul Sculptor*'s effect ended only when you cast a creature spell; it's any player. *Goblin Cadets*'
    "becomes blocked" trigger fired once per blocker. In-game text: *Wall of Junk*'s "it's owner's hand".
  - R&D's Secret Lair printed text:
    - *Gilded Drake* (Urza's Saga only): "exchange control of Gilded Drake for target creature one of your opponents
      controls or sacrifice Gilded Drake" - no "still resolves if its target becomes illegal", so the trigger is
      countered and the Drake stays if the target is gone, and it never triggers with nothing to target.
    - *Lifeline* (Urza's Saga only): "return that creature from your graveyard" - only creatures you own.
    - *Planar Void* (Urza's Saga only): no "another", so it exiles itself too.
  - Left as Oracle: Mishra's Helix's untargeted "Tap X lands", Phyrexian Processor's "When it comes into play, pay
    any amount of life" (a trigger, but nothing can use the gap), Carpet of Flowers' "During your main phase"
    (one use, kept by the errata), discard-phase and "at end of turn" timing (Antagonism, Grafted Skullcap, Thran
    Quarry, Waylay), creature types gained since (the Opals and Hiddens, Mobile Fort, Barrin), echo's old
    reminder, and the "any one source" shields (the Runes of Protection, Sanctum Guardian, Redeem).
- **Set review: Urza's Legacy Promos (Beast of Burden) and Urza's Legacy** (140 new names) - no changes needed.
  Left as Oracle: Viashino Bey's printed "When Viashino Bey attacks, all creatures you control attack if able" (a
  trigger would come too late to make anything attack), Thran War Machine's "attacks each turn", creature types
  gained since (Opal Avenger's Guardian, Radiant), interrupts (Hidden Gibbons) and the "any one source" shields
  (Martyr's Cause).
- **Set review: Portal Three Kingdoms Promos (Lu Bu, Master-at-Arms) and Portal Three Kingdoms** (151 new names,
  `PortalThreeKingdomsAuditTest`) - scripts needed no changes. R&D's Secret Lair printed text: *Zodiac Dragon*
  (Portal Three Kingdoms only) says "If Zodiac Dragon is put into your graveyard" with no "from play", so it also
  comes back when discarded or milled. Left as Oracle: Portal's vocabulary ("any one" for target, "destroy" for
  sacrifice, "your opponent", Taoist Hermit and Zuo Ci's "can't choose" for hexproof, Preemptive Strike's "another
  player") and "at the end of that turn" timing (Warrior's Oath).
- **Set review: Urza's Destiny Promos (False Prophet) and Urza's Destiny** (142 new names, `UrzasDestinyAuditTest`):
  - *Mark of Fury* only returned at your own end step; it's "the end step", whoever's turn it is. In-game text:
    *Opposition* read "Tap target artifact, creature     or land."
  - Checked and left as they are (tested): Private Research still counts the Aura's counters after the creature
    dies (Incendiary reads them the same way), and Scour finds the exiled enchantment's controller.
  - No R&D's Secret Lair changes: the printed texts differ only in old templating ("global enchantment",
    "enchant creature card", "at end of turn") and creature types (Lurking Jackals' Hound).
- **Set review: Starter 1999** (26 new names, `Starter1999AuditTest`) - scripts needed no changes. R&D's Secret
  Lair printed text: *Grim Tutor* (before M21) says "Search your library for any card and put that card into your
  hand" - no shuffle.
- **Set review: Mercadian Masques Promos (Overtaker), Media and Collaboration Promos (PMEI) and Mercadian Masques**
  (317 new names, the ten PMEI ones being modern Universes Beyond promos) - no changes needed. Left as Oracle: the
  Ramosian and Cateran tutors' "Rebel card" / "Mercenary card" (no non-permanent ones exist), Strongarm Thug's
  untargeted "return a Mercenary card", Flaming Sword's misprinted "Target creature", creature types (Monkey Cage's
  Apes), "your opponent" (Thieves' Auction) and "at end of turn" timing.
- **Set review: Nemesis Promos (Rathi Assassin) and Nemesis** (143 new names) - no changes needed. Left as Oracle:
  the tutors' "Rebel card" / "Mercenary card" / "Goblin card" wording, Stronghold Gambit's singular "the creature
  card ... with the lowest converted mana cost" (ties read either way) and "at the end of each player's turn"
  timing (Mana Cache).
- **Set review: Prophecy Promos (Avatar of Hope) and Prophecy** (143 new names, `ProphecyAuditTest`):
  - In-game text: *Verdant Field*'s granted ability had no description; *Windscouter* ("it's owner's"), *Spitting
    Spider* ("each creature and with flying") and *Troubled Healer*'s target prompt.
  - R&D's Secret Lair printed text: *Rhystic Cave* (Prophecy only) is "{T}: Add one mana of any color to your mana
    pool unless any player pays {1}" - a mana ability, usable while paying for a spell, without "Activate only as
    an instant".
  - Left as Oracle: Hollow Warrior's "tap an untapped creature you control" (the printed wording would let a
    vigilant attacker, or itself, pay - too unclear to follow), creature types gained since (Chimeric Idol's
    Turtle), "at the end of your turn" timing (Citadel of Pain, Wells, Troublesome Spirit) and Informers'
    "black/white spells or abilities".

- **Set review: Invasion** (310 new names, `InvasionAuditTest`):
  - Fixed: *Saproling Infestation* only counted kicked spells you controlled; "Whenever a player kicks a spell" is
    anyone's.
  - **Engine:** delayed "at the beginning of the next end step" moves (`AtEOT$`) take `Graveyard` - put into its
    owner's graveyard, not sacrificed.
  - R&D's Secret Lair printed text: *Elvish Champion* ("All Elves", so it pumps itself, through Eighth Edition),
    *Kangee, Aerie Keeper* ("All Birds", Invasion only), *Opt* (looks at the top card and may bottom it, which isn't a
    scry, until Ixalan), *Cauldron Dance* (the creature from your hand isn't optional and is put into your
    graveyard at end of turn, not sacrificed, until Commander 2017) and *Psychic Battle* (Invasion only: without
    its last sentence, a target it changes triggers Psychic Battles again).
  - Left as Oracle: Dueling Grounds' "each turn" and Urborg Drake's "attacks each turn" (as with Thran War
    Machine), "creature or player" damage wording, Legend-type wording (Tsabo Tavoc, Empress Galina, Captain
    Sisay), Firebrand Ranger's non-optional land and "at end of turn" timing (Liberate, Skizzik, Spinal Embrace).

- **Set review: Planeshift** (142 new names, `PlaneshiftAuditTest`):
  - Fixed: *Questing Phelddagrif*'s Hippo went to every opponent instead of the targeted one.
  - In-game text: *Dominaria's Judgment* ("an Swamp", "an Mountain", "an Forest").
  - R&D's Secret Lair printed text: *Lord of the Undead* ("All Zombies", so it pumps itself, through Eighth
    Edition).
  - Left as Oracle: Surprise Deployment's non-optional creature (as with Firebrand Ranger), Sparkcaster's "target
    player", "at end of turn" timing (Keldon Twilight, Planeswalker's Mischief) and the Lairs' "is a Lair in
    addition to its land type" (the type line does that).

- **Set review: Apocalypse** (142 new names) - no changes needed. Left as Oracle: False Dawn's printed "Colored
  mana symbols on all permanents you control and on all cards you own that aren't in play become {W}" (a text change
  to mana symbols the engine has no way to apply), Dragon Arch's non-optional creature (as with Firebrand Ranger),
  Quicksilver Dagger's "target player" and the Flagbearers' "could target a Flagbearer" wording.

- **Set review: Odyssey** (321 new names, `OdysseyAuditTest`):
  - In-game text: *Childhood Horror*, *Dirty Wererat* and *Frightcrawler* ("Enchanted Threshold"), *Phantom Whelp*
    ("it's owner's").
  - R&D's Secret Lair printed text: *Think Tank* (Odyssey only) looks at the top card and may put it into your
    graveyard, which isn't a surveil.
  - Left as Oracle: Liquid Fire's "5 damage divided as you choose between target creature and that creature's
    controller" (the same choice, made as you cast it), Aboshan's "Cephalid", Testament of Faith's Wall wording,
    Otarian Juggernaut's "attacks each turn", Puppeteer's non-optional tap or untap and "at end of turn" timing
    (Bloodcurdler, Mad Dog, Shifty Doppelganger). Millikin and Charmed Pendant aren't mana abilities by today's rules
    (a cost that mills), which their printed text doesn't change.

- **Set review: Torment** (142 new names, `TormentAuditTest`):
  - In-game text: *Temporary Insanity*'s target prompt said "less than or equal to".
  - R&D's Secret Lair printed text: *Balthor the Stout* (Torment only) is "All Barbarians get +1/+1" and "{R}: Target
    Barbarian gets +1/+0", so it pumps itself and can target itself.
  - Left as Oracle: Carrion Rats' and Carrion Wurm's "deals no combat damage", Gurzigost's "deal its combat damage to
    defending player", Far Wanderings' "three" basic lands, Equal Treatment's "creature or player" and "at end of
    turn" timing (Shambling Swarm, False Memories, Ichorid, Dawn of the Dead).

- **Set review: Judgment** (141 new names):
  - Fixed: *Cephalid Constable* returned up to its power in permanents instead of up to the damage it dealt;
    *Spurnmage Advocate*, *Pulsemage Advocate* and *Nullmage Advocate* could take their cards from more than one
    opponent's graveyard.
  - In-game text: *Shaman's Trance* ("Other players cast spells from their graveyards").
  - Left as Oracle: Wormfang Crab's printing lets the opponent exile the Crab itself, which then returns and triggers
    again (the loop the "other than this creature" erratum exists to stop), Nomad Mythmaker's "enchant creature
    card", Krosan Wayfarer's non-optional land, the Wishes' "Choose ... reveal" wording, Nantuko Monastery's missing
    creature types and Firecat Blitz's "Cat" tokens.

- **Set review: Onslaught Promos (Silent Specter) and Onslaught** (320 new names, `OnslaughtAuditTest`):
  - Fixed: *Ixidor's Will* counted only your Wizards ("{2} for each Wizard on the battlefield"); *Misery Charm*
    could return a Cleric card from any graveyard; *Skirk Commando*'s and *Snapping Thragg*'s "you may have it deal"
    damage wasn't optional.
  - R&D's Secret Lair printed text: *Rummaging Wizard* (Onslaught only) looks at the top card and may put it into
    your graveyard, which isn't a surveil.
  - Left as Oracle: "other than Legend or Wall" (Artificial Evolution, Imagecrafter, Mistform Mutant, Standardize -
    Legend is no longer a creature type), Aurification's Wall wording, Grand Melee's "each turn", Piety Charm's
    "target enchant creature" and "target creature or player" / "target player" damage.

- **Set review: Friday Night Magic 2003 (Withered Wretch), Arena League 2003 (Elvish Aberration, Skirk Marauder,
  Bonesplitter), Legions Promos (Feral Throwback) and Legions** (146 new names) - no changes needed. Left as Oracle:
  Mistform Ultimus's "may attack as though it weren't a Wall", Skirk Drill Sergeant's "Goblin card" (a Goblin instant
  or sorcery can't be put onto the battlefield), Celestial Gatekeeper's "Bird and/or Cleric cards" and "target player"
  damage.

- **Set review: Scourge Promos (Soul Collector) and Scourge** (141 new names):
  - Fixed: *Decree of Savagery*'s cycling trigger ("you may put four +1/+1 counters") wasn't optional.
  - Left as Oracle: Upwelling's printed "Mana pools don't empty at the end of phases or turns" (written when mana only
    emptied between phases), "Whenever a creature is turned face up" (Aven Farseer, Aphetto Runecaster, Bonethorn
    Valesk), Ageless Sentinels' Wall wording, Dragonstorm's "Dragon card", Goblin Brigand's "attacks each turn" and
    "at end of turn" timing (Form of the Dragon, Lingering Death, Force Bubble, Call to the Grave).

- **Set review: Mirrodin Promos (Sword of Kaldra) and Mirrodin** (268 new names, `MirrodinAuditTest`):
  - Fixed: *Groffskithur* could return a card named Groffskithur from any graveyard, not just yours.
  - R&D's Secret Lair printed text: *Loxodon Warhammer* (Mirrodin and Ninth Edition) grants "Whenever this creature
    deals damage, you gain that much life" - a trigger, not lifelink.
  - Left as Oracle: Grim Reminder's "played a card" (the revealed card is nonland, so it's the same as casting),
    Duplicant's "a creature card is imprinted" (it only matters after a second imprint), Spellweaver Helix's "a card is
    played", Mindslaver's mana burn reminder and "at end of turn" timing (Jinxed Choker, Glimmervoid, Quicksilver
    Fountain, Dross Harvester).

- **Set review: Arena League 2004 (Mise, Glacial Ray, Booster Tutor, Goblin Mime, Serum Visions, Darksteel Ingot,
  Granny's Payback, Circle of Protection: Art), Darksteel Promos (Shield of Kaldra) and Darksteel** (169 new names,
  `DarksteelAuditTest`):
  - R&D's Secret Lair printed text: *Darksteel Pendant* (Darksteel only) looks at the top card and may bottom it,
    which isn't a scry. A sweep for the same erratum on older cards found *Soldevi Excavations*, missed in the
    Alliances review; it gets the same printed ability.
  - Left as Oracle: Chimeric Egg's and Gemini Engine's missing Construct type and "at end of turn" timing (Tears of
    Rage, Nim Abomination).

- **Set review: Fifth Dawn Promos (Helm of Kaldra) and Fifth Dawn** (161 new names) - no changes needed. Left as
  Oracle: Helm of Kaldra's "Avatar Legend" token, Chimeric Coils' and Ensouled Scimitar's missing creature types,
  Grafted Wargear's "unattached from a creature" and "at end of turn" timing (Possessed Portal, Endless Whispers,
  Chimeric Coils).

- **Set review: Champions of Kamigawa Promos (Ryusei, the Falling Star) and Champions of Kamigawa** (282 new
  names):
  - Fixed: *Hana Kami* could return an Arcane card from any graveyard, not just yours; *Teller of Tales*' "you may
    tap or untap target creature" wasn't optional.
  - Left as Oracle: Kiki-Jiki's printed "That creature token has haste" (a granted ability rather than part of the
    copy - it only matters if the token itself is copied), Reweave's "a card that shares a card type", Through the
    Breach's and Psychic Puppetry's non-optional wording, Gifts Ungiven's and Kodama's Reach's exact counts, Tide of
    War's "defending player sacrifices" and "at end of turn" timing (Otherworldly Journey, Hikari, Junkyo Bell).

- **Set review: Unhinged** - skipped here; its cards are being checked one by one in the Unhinged scripting pass.
- **Set review: Arena League 2005 (Skyknight Legionnaire, Genju of the Spires, Okina Nightwatch), Betrayers of
  Kamigawa Promos (Budoka Pupil, Ink-Eyes, Servant of Oni) and Betrayers of Kamigawa** (161 new names) - no changes
  needed. Left as Oracle: Toshiro Umezawa's "If that card would be put into a graveyard this turn", Toils of Night and
  Day's non-optional tap or untap, Slumbering Tora's missing Cat type, Overblaze's "creature or player", Fumiko's
  "attacks each turn" and "at end of turn" timing (Mark of the Oni, Shirei).
- **Set review: Saviors of Kamigawa Promos (Ghost-Lit Raider, Kiyomaro) and Saviors of Kamigawa** (162 new names):
  - Fixed: *Choice of Damnations*' life-loss choice read "That player lose that much life".
  - Left as Oracle: Erayo's Essence's printed "Counter the first spell played by each opponent each turn",
    Matsu-Tribe Birdstalker's "may block as though it had flying" (reach), Iname as One's "Spirit card", Shape
    Stealer's "change power and toughness", Pain's Reward's bidding wording, "of their choice" sacrifices (Thoughts
    of Ruin, Michiko Konda, Exile into Darkness) and "at end of turn" timing (Ideas Unbound, Glitterfang, Footsteps
    of the Goryo).
- **Set review: Ravnica Promos (Gleancrawler) and Ravnica: City of Guilds** (278 new names, `RavnicaAuditTest`):
  - Fixed: *Dimir Cutpurse* drew before the player discarded; the discard comes first.
  - R&D's Secret Lair printed text: *Remand* (through Duel Decks: Jace vs. Vraska) counters the spell into its owner's
    graveyard and then returns the card to their hand, so "put into a graveyard" triggers see it; *Incite Hysteria*
    (Ravnica only) affects creatures that share a color with the target, so a colorless target is untouched.
  - Left as Oracle: Ghosts of the Innocent's "creature or player", Dimir Doppelganger's "gains this ability" (rather
    than a copy exception), Circu's "target library", Grifter's Blade's "comes into play equipping", Sabertooth
    Alley Cat's "attacks each turn", "of their choice" sacrifices (Woebringer Demon, Savra, Twisted Justice,
    Stoneshaker Shaman) and "at end of turn" timing (Transluminant, Voyager Staff, Flickerform, Necroplasm).
- **Set review: Arena League 2006 (Coiling Oracle, Castigate, Wee Dragonauts, Surging Flame), Guildpact Promos (Djinn
  Illuminatus, Gruul Guildmage) and Guildpact** (159 new names):
  - Fixed: *Pillory of the Sleepless* showed "Enchanted creature can't attack or block." twice in its card text.
  - Left as Oracle: Electrolyze's "any number of target creatures and/or players" (2 damage still means one or two
    targets), Predatory Focus's "deal their combat damage to defending player", Sword of the Paruns' non-optional tap
    or untap, Mizzium Transreliquat's "gains this ability", Graven Dominator's "becomes 1/1", Leyline of the Void's
    missing "from anywhere" and "at end of turn" timing (Ghostway, Thunderheads, Killer Instinct, Ghost Council of
    Orzhova).
- **Set review: Dissension Promos (Avatar of Discord) and Dissension** (175 new names, `DissensionAuditTest`):
  - R&D's Secret Lair printed text: *Palliation Accord* (Dissension only) uses shield counters, which under today's
    rules also keep the enchantment from being damaged or destroyed.
  - Not done yet: Flash Foliage's printing has no "Cast this spell only during combat after blockers are declared",
    but a token put onto the battlefield blocking before blockers are declared would be cleared when the AI declares
    its blockers, so it keeps the Oracle restriction for now. Curtain of Light (Saviors) also lacks the line, but an
    attacking creature isn't "unblocked" until blockers are declared, so nothing changes there.
  - Left as Oracle: Anthem of Rakdos's "creature or player", Omnibian's "becomes a 3/3 Frog", Aethermage's Touch's
    "into play with" wording, "of their choice" sacrifices (Rakdos the Defiler, Rakdos Riteknife, Hit) and "at end
    of turn" timing (Ignorant Bliss, Rakdos Guildmage).
- **Set review: Coldsnap Promos (Allosaurus Rider) and Coldsnap** (147 new names) - no changes needed. Left as Oracle:
  the printed {Si} "one mana from a snow permanent" (today's "snow source"), Thrumming Stone's "Spells you control",
  Haakon's "Knight cards", Rimewind Taskmage's non-optional tap or untap, Lovisa Coldeyes' and Field Marshal's
  "Barbarians, Warriors, and Berserkers"/"Other Soldiers" and "at end of turn" timing (Lightning Serpent, Phyrexian
  Etchings, Magmatic Core, Brooding Saurian).
- **Set review: Time Spiral Promos (Lotus Bloom) and Time Spiral** (280 new names, `TimeSpiralAuditTest`):
  - Fixed: *Scion of the Ur-Dragon* could find any Dragon card, including a changeling kindred spell, rather than a
    Dragon permanent card; *Durkwood Tracker* only checked "if this creature is on the battlefield" when activated, so
    it still dealt damage after leaving; *Coral Trickster*'s "you may tap or untap target permanent" wasn't optional.
  - R&D's Secret Lair printed text: *Scion of the Ur-Dragon* (through Commander 2017) searches for a "Dragon card" -
    finding a changeling kindred spell makes it a copy of that spell, which stays on the battlefield (rule 110.4b).
  - Left as Oracle: the suspend reminder's "When you remove the last, play it" (keywords follow today's rules), Amrou
    Scout's "Rebel card" (a Rebel spell can't be put onto the battlefield anyway), Dralnu's flashback wording,
    Plated Pegasus's "creature or player", Sudden Spoiling's "become 0/2", "of their choice" sacrifices (Smallpox,
    Curse of the Cabal) and "at end of turn" timing (Magus of the Jar, Norin the Wary). MTGJSON's printed text for
    Venser's Sliver is another card's Spanish text, so it wasn't compared.
- **Set review: Happy Holidays** - no changes; these cards were scripted one by one in the fork, and MTGJSON has no
  printed text for them to compare.
- **Set review: Planar Chaos Promos (Hedge Troll, Oros) and Planar Chaos** (163 new names):
  - Fixed: *Firefright Mage* could only target a creature you control (it's any target creature); *Porphyry Nodes*
    skipped indestructible creatures when finding the least power, so with a tie you couldn't choose the indestructible
    one and destroy nothing.
  - Left as Oracle: the printed "All Slivers" / "this creature" Sliver wording (only creatures are Slivers today),
    Cautery Sliver's and Healing Leaves' "creature or player", Blightspeaker's "Rebel card", Voidstone Gargoyle's
    "named card can't be played", "of their choice" sacrifices (Shivan Wumpus, Brain Gorgers) and "at end of turn"
    timing (Treacherous Urge, Fatal Frenzy, Molten Firebird, Pyrohemia, Groundbreaker).
- **Set review: Future Sight Promos (Korlash) and Future Sight** (179 new names):
  - Fixed: *Scourge of Kher Ridges*' first ability read "each creature and without flying".
  - Left as Oracle: Glittering Wish's non-optional "Choose a multicolored card", Emberwilde Augur's "target player",
    Pyromancer's Swath's and Riddle of Lightning's "creature or player", Steamflogger Boss's "Rigger creatures", the
    suspend/madness reminder wording and "at end of turn" timing (Saltskitter, Nacatl War-Pride, Sparkspitter).
- **Set review: Lorwyn Promos (Shriekmaw, Wren's Run Packmaster) and Lorwyn** (278 new names):
  - Fixed: *Wanderer's Twig*'s text ended "then shuffle. Then shuffle."; *Caterwauling Boggart* listed its menace
    grant twice, once as "Elemental creatures".
  - Left as Oracle: Heat Shimmer's printed "It has haste and..." (granted rather than part of the copy, as with
    Kiki-Jiki), Purity's "If a spell or ability would deal damage to you" (noncombat damage), the printed hideaway
    and deathtouch reminders (keywords follow today's rules), Chandra Nalaar's, Brion Stoutarm's and Incendiary
    Command's "target player", Lignify's and Mirror Entity's "is/becomes X/X" and "at end of turn" timing (Galepowder
    Mage, Incandescent Soulstoke, Lowland Oaf, Colfenor's Urn, Merrow Commerce).
- **Set review: Morningtide Promos (Door of Destinies) and Morningtide** (147 new names):
  - Fixed: *Rage Forger*'s "you may have that creature deal 1 damage" wasn't optional.
  - Left as Oracle: Everbark Shaman's and Scapeshift's exact land counts, Shard Volley's "creature or player" and "at
    end of turn" timing (Stenchskipper).
- **Set review: Shadowmoor Promos (Demigod of Revenge) and Shadowmoor** (273 new names):
  - Fixed: *Medicine Runner*'s "you may remove a counter" wasn't optional; *Firespout*'s flying half said "if {R} was
    spent" in its text.
  - Left as Oracle: Cemetery Puca's "gains this ability" (as with Dimir Doppelganger), Godhead of Awe's and Inkfathom
    Witch's "become 1/1 / 4/1", Rite of Consumption's and Flame Javelin's "target player" / "creature or player",
    "of their choice" sacrifices (Din of the Fireherd) and "at end of turn" timing (Puppeteer Clique, Wound
    Reflection, Sygg, Turn to Mist, Mistmeadow Witch, Elemental Mastery, Impromptu Raid).
- **Set review: Eventide Promos (Overbeing of Myth) and Eventide** (178 new names):
  - Fixed: *Swirling Spriggan* could recolor any creature; it's "target creature you control".
  - Left as Oracle: Figure of Destiny's and the Mimics' "becomes N/N", Fire at Will's "any number of target" creatures
    (3 damage still means one to three), Pyrrhic Revival's "with a -1/-1 counter" (it adds to any others either way),
    Ward of Bones' and Moonhold's "can't play ... cards" and Crackleburr's "creature or player".
- **Set review: From the Vault: Dragons (Hellkite Overlord) and Shards of Alara** (224 new names,
  `ShardsOfAlaraAuditTest`):
  - Fixed: *Mighty Emergence*'s "you may put two +1/+1 counters" wasn't optional; Elspeth, Knight-Errant's emblem
    read "are indestructible" where the card says "have indestructible".
  - R&D's Secret Lair printed text: *Elspeth, Knight-Errant* (through Duel Decks: Elspeth vs. Tezzeret) has no emblem -
    her -8 makes the artifacts, creatures, enchantments, and lands you control as it resolves indestructible for the
    rest of the game, and permanents you get later aren't covered (rule 611.2c).
  - Left as Oracle: Minion Reflector's "That token has haste and..." (granted, as with Heat Shimmer), Brilliant
    Ultimatum's "play any number of cards", Death Baron's "Skeleton creatures", Blightning's, Ajani Vengeant's and
    Exuberant Firestoker's "target player" / "creature or player", "of their choice" sacrifices (Fleshbag Marauder,
    Cruel Ultimatum) and "at end of turn" timing (Hell's Thunder, unearth, Sphinx Sovereign, Drumhunter).
- **Set review: Conflux Promos (Malfegor) and Conflux** (140 new names):
  - Fixed: *Bloodhall Ooze*'s "you may put a +1/+1 counter" triggers weren't optional; *Exploding Borders* said
    "Then shuffle." twice.
  - Left as Oracle: Ignite Disorder's "any number of target" creatures (3 damage still means one to three),
    Thornling's "is indestructible this turn", Maelstrom Archangel's "play a nonland card", Banefire's, Goblin
    Razerunners', Quenchable Fire's, Exploding Borders' and Nicol Bolas's "target player" / "creature or player",
    and "at end of turn" timing (Wall of Reverence, Goblin Razerunners, Hellspark Elemental, unearth).
- **Set review: Alara Reborn Promos (Dragon Broodmother) and Alara Reborn** (141 new names, `AlaraRebornAuditTest`):
  - Fixed: *Blitz Hellion* only shuffled itself away at your own end step (it's every end step); *Godtracker of Jund*
    ignored itself entering with power 5 or greater.
  - R&D's Secret Lair printed text: *Etherwrought Page* (Alara Reborn only) looks at the top card and may put it into
    your graveyard, which isn't a surveil; *Lorescale Coatl* (before Core Set 2021) makes its counter optional.
  - Left as Oracle: Aven Mimeomancer's "is 3/1", Monstrous Carabid's "each turn", Sen Triplets' "can't play spells
    or activated abilities", Glassdust Hulk's "is unblockable", Dauntless Escort's "are indestructible", Mage
    Slayer's "defending player", Lich Lord of Unx's and Nemesis of Reason's top-cards-into-graveyard (rule 701.17a
    defines that as mill), "of their choice" sacrifices (Defiler of Souls, Thraximundar), "creature or player" and
    "at end of turn" timing (Lightning Reaver, Blitz Hellion, Predatory Advantage, Slave of Bolas).
- **Set review: 2009 promos (Spellbreaker Behemoth, Rampaging Baloths), Magic 2010 Promos (Vampire Nocturnus, Honor
  of the Pure, Ant Queen) and Magic 2010** (113 new names):
  - In-game text: *Undead Slayer* ("Skeleton, Vampire     or Zombie").
  - R&D's Secret Lair printed text: *Rampaging Baloths* (before Edge of Eternities Commander) makes its Beast
    optional.
  - Left as Oracle: Harm's Way's "you or a permanent you control" (the same shield), Protean Hydra's "the end
    step", Sphinx Ambassador's "names a card", Indestructibility's "is indestructible", Berserkers of Blood Ridge's
    "each turn", Tome Scour's top-cards-into-graveyard (rule 701.17a) and Yawning Fissure's "sacrifices a land".
- **Set review: Planechase (Kor Sanctifiers, Whiplash Trap, Hideous End, Beast Hunt previews) and Planechase Planes**
  (44 new names, `PlanechaseAuditTest`):
  - In-game text: *The Fourth Sphere*'s chaos ability ("put a 2/2 black zombie token onto the battlefield").
  - R&D's Secret Lair printed text: all forty Planechase planes (through Planechase Anthology, or before March of the
    Machine Commander for its ten reprints) say "Whenever you roll {CHAOS}", so chaos that ensues any other way -
    Missy, the Will of the Planeswalkers sorceries, Fixed Point in Time - doesn't trigger them. New chaos-trigger
    param `RolledOnly$ True` (the planar die now marks its chaos roll), and revealed planes' chaos abilities
    (Pools of Becoming) now respect their trigger conditions.
  - Left as Oracle: "and/or" vs "or" for the planeswalk-and-upkeep triggers (The Aether Flues, Cliffside Market, The
    Maelstrom), Naya's "additional lands", Stronghold Furnace's "creature or player", Bant's "is indestructible",
    Sokenzan's "After this main phase" and Lethe Lake's top-cards-into-graveyard.
- **Set review: 2009-2011 DCI promo planes and schemes** (11 new names):
  - In-game text: *Perhaps You've Met My Cohort* ("and put that card onto the battlefield").
  - R&D's Secret Lair printed text: *Celestine Reef*, *Horizon Boughs*, *Mirrored Depths*, *Tazeem* and *Tember City*
    (through Planechase Anthology) say "Whenever you roll {CHAOS}", as above. The five schemes' printed text matches
    Oracle (checked on the scans); Tormented Soul's "is unblockable" left as Oracle.
- **Set review: Zendikar** (219 new names):
  - In-game text: *Quest for the Holy Relic* said "then shuffle. Then shuffle your library."
  - R&D's Secret Lair printed text: *Lotus Cobra* (before Zendikar Rising) makes its mana optional.
  - Left as Oracle: Quest for Pure Flame's "creature or player" (as with Furnace of Rath), Oracle of Mul Daya's "play
    the top card of your library if it's a land card", Sorin Markov's "control target player's next turn",
    Gigantiform's "is 8/8", Eldrazi Monument's "are indestructible", Aether Figment's "is unblockable", "of their
    choice" sacrifices (World Queller, Gatekeeper of Malakir) and Archive Trap's and Hedron Crab's
    top-cards-into-graveyard.
- **Set review: Worldwake Promos (Celestial Colonnade, Comet Storm) and Worldwake** (142 new names):
  - Fixed: *Mordant Dragon* dealt damage equal to its power instead of the combat damage it dealt; *Dead Reckoning*
    wasn't optional, and declining it still dealt the damage.
  - Left as Oracle: the Zendikons' "is put into a graveyard" (it's on the battlefield, so it dies), Summit Apes'
    menace wording, Creeping Tar Pit's and Terra Eternal's "is unblockable" / "are indestructible", Omnath's "doesn't
    empty from your mana pool", Wrexial's "if that card would be put into a graveyard this turn" (it differs only if
    the card leaves the stack another way and reaches a graveyard later that turn), "of their choice" sacrifices
    (Anowon, Butcher of Malakir) and Searing Blaze's, Cosi's Ravager's and Razor Boomerang's "target player" /
    "creature or player".
- **Set review: 2010 promos (Emrakul, the Aeons Torn), Rise of the Eldrazi Promos (Lord of Shatterskull Pass) and Rise
  of the Eldrazi** (220 new names): no script changes.
  - Left as Oracle: totem armor (now umbra armor - keywords follow current rules), Splinter Twin's "That token has
    haste" (granted, as with Kiki-Jiki), Realms Uncharted's "four land cards" (an exact count in a hidden search),
    Training Grounds' "up to {2}", World at War's "first postcombat main phase", Gigantomancer's "becomes 7/7",
    Transcendent Master's and Deathless Angel's "is indestructible", Ulamog's Crusher's and Lust for War's "each
    turn", Raid Bombardment's "defending player" and "creature or player" (Forked Bolt, Explosive Revelation).
- **Set review: Archenemy (Reassembling Skeleton, Plummet, Sorcerer's Strongbox, Chandra's Outrage previews) and
  Archenemy Schemes** (49 new names):
  - In-game text: *The Iron Guardian Stirs* ("put a 4/6 colorless Golem creature token onto the battlefield"),
    *Introductions Are in Order*'s second mode ("or you may put").
  - Left as Oracle: "of their choice" sacrifices (May Civilization Collapse, Feed the Machine, Only Blood Ends Your
    Nightmares), Which of You Burns Brightest?'s "target opponent" and My Undead Horde Awakens' "is put into a
    graveyard".
- **Set review: Magic 2011 Promos (Sun Titan, Ancient Hellkite) and Magic 2011** (105 new names):
  - In-game text: *Fauna Shaman* said "then shuffle. Then shuffle."
  - R&D's Secret Lair printed text: *Ajani's Pridemate* (before War of the Spark) makes its counter optional.
  - Left as Oracle: Leyline of Sanctity's and Sacred Wolf's hexproof wording, Knight Exemplar's and Phylactery
    Lich's "are/is indestructible", Leyline of Anticipation's "nonland cards", Diminish's "becomes 1/1", Elixir of
    Immortality's "into your library" (a card only goes to its owner's library), "of their choice" sacrifices
    (Destructive Force) and "creature or player" (Inferno Titan, Ember Hauler).
- **Set review: From the Vault: Relics (Sword of Body and Mind), Duel Decks: Elspeth vs. Tezzeret (Contagion Clasp,
  Kemba's Skyguard), Scars of Mirrodin Promos (Wurmcoil Engine, Steel Hellkite, Memoricide), Scars of Mirrodin and
  its Poison Counter, and the 2011 FNM promo (Go for the Throat)** (219 new names, `ScarsOfMirrodinAuditTest`):
  - Fixed: *Argent Sphinx* came back under its owner's control; it returns under the control of whoever exiled it.
  - In-game text: *Vedalken Certarch* ("creature     or land").
  - Left as Oracle: Glint Hawk Idol's untyped "2/2 artifact creature" and the untyped Wurm/Insect tokens (Wurmcoil
    Engine, Carrion Call, Trigon of Infestation - creature-type updates), Echo Circlet's missing "each combat",
    Contagion Engine's "proliferate, then proliferate again", Asceticism's hexproof wording, "is indestructible" and
    "each turn" (Darksteel Juggernaut, Flameborn Hellion), "of their choice" sacrifices (Dispense Justice) and "target
    player" / "creature or player" (Vulshok Replica, Arc Trail, Galvanic Blast).
- **Set review: Mirrodin Besieged Promos (Glissa, the Traitor, Hero of Bladehold, Mirran Crusader, Thopter Assembly)
  and Mirrodin Besieged** (141 new names):
  - Fixed: *Rot Wolf*'s draw trigger had no trigger zone, so it also worked from your hand or graveyard.
  - Left as Oracle: Cryptoplasm's "gains this ability" (granted, as with Dimir Doppelganger), Tezzeret, Agent of
    Bolas's "becomes a 5/5", the untyped Horror/Zombie/Myr tokens and Inkmoth Nexus's Blinkmoth (creature-type
    updates), Kemba's Legion's missing "each combat", "is indestructible", "each turn" (Kuldotha Ringleader,
    Phyrexian Juggernaut) and "target player" / "creature or player" (Concussive Bolt, Red Sun's Zenith, Kuldotha
    Flamefiend).
- **Set review: New Phyrexia Promos (Surgical Extraction, Pristine Talisman, Phyrexian Metamorph, Sheoldred,
  Whispering One) and New Phyrexia** (162 new names): no script changes.
  - Left as Oracle: Impaler Shrike's "When" (a trigger word, same as "Whenever"), Praetor's Grasp's "look at and
    play", Chancellor of the Tangle's missing "of the game", the Shrines' "or", the untyped Golem/Myr/Insect/Germ
    tokens (creature-type updates), "of their choice" sacrifices (Geth's Verdict, Phyrexian Obliterator), Trespassing
    Souleater's "is unblockable" and "creature or player" (Gut Shot, Rage Extractor).
- **Set review: Commander 2011 and its promos** (51 new names): no script changes.
  - Left as Oracle: Karador's "cast one creature card", Zedruu's {R}{W}{U} cost order, the Vows' "or a
    planeswalker", Avatar of Slaughter's "each turn", "of their choice" sacrifices (Martyr's Bond, Syphon Flesh,
    Tribute to the Wild) and Acorn Catapult's "creature or player".
- **Set review: Magic 2012 Promos (Bloodlord of Vaasgoth, Chandra's Phoenix, Garruk's Horde) and Magic 2012** (97
  new names): no script changes.
  - Left as Oracle: Phantasmal Image's "gains" (as with Dimir Doppelganger), Turn to Frog's and Skinshifter's
    "becomes a 1/1 / 4/4", Aegis Angel's "is indestructible", Stormblood Berserker's menace wording, Scepter of
    Empires' "target player" and "creature or player" (Warstorm Surge, Chandra, the Firebrand).
- **Set review: From the Vault: Legends (Mikaeus, the Lunarch), Innistrad Promos, Innistrad and the 2011 Wizards Play
  Network promos** (240 new names, `InnistradAuditTest`):
  - Fixed: a stolen *Mirror-Mad Phantasm* revealed cards from its controller's library; it's its owner's library.
  - Left as Oracle: Nevermore's "The named card can't be cast", Curse of the Pierced Heart's player-only damage,
    Evil Twin's "gains" (as with Dimir Doppelganger), Angelic Overseer's, Creepy Doll's and Manor Gargoyle's "is
    indestructible", "each turn" (Curse of the Nightly Hunt, Furor of the Bitten, Galvanic Juggernaut), "of their
    choice" sacrifices (Tribute to Hunger) and "target player" / "creature or player" (Brimstone Volley, Heretic's
    Punishment, Stensia Bloodhall).
- **Set review: the 2012 FNM and Wizards Play Network promos, Dark Ascension Promos and Dark Ascension** (155 new
  names, `DarkAscensionAuditTest`):
  - Fixed: *Elbrus, the Binding Blade* transformed without unattaching first; *Withengar Unbound*'s lose-the-game
    trigger had no trigger zone.
  - In-game text: *Dawntreader Elk* and *Jar of Eyeballs* (stray spaces).
  - Left as Oracle: Counterlash's "nonland card in your hand", Grafdigger's Cage's "can't enter the battlefield from
    graveyards or libraries", Pyreheart Wolf's menace wording, Immerwolf's "Other Wolf and Werewolf creatures",
    "is/are indestructible" (Break of Day, Falkenrath Aristocrat, Predator Ooze) and Hellrider's "defending player".
- **Set review: Avacyn Restored Promos, the 2012 Helvault promos and Avacyn Restored** (216 new names,
  `AvacynRestoredAuditTest`):
  - Fixed: *Dread Slaver*'s return trigger had no trigger zone; *Ghostly Touch*'s "you may tap or untap" wasn't
    optional.
  - In-game text: *Herald of War* ("Angels and Humans you cast cost 1 less").
  - R&D's Secret Lair printed text: *Descendants' Path* (before the 2022 Secret Lair printing) says "Otherwise, put
    that card on the bottom", so a creature card you could cast but don't stays on top.
  - Secret Lair Drop printings can now carry their own release date (`printDate` in the edition file), since the
    edition's single 2019 date made every later Secret Lair reprint count as an old printing. Set for the Secret Lair
    printings of Descendants' Path, Grim Tutor, Lotus Cobra, Rampaging Baloths and Zombie Master, which were all
    printed with the corrected text.
  - Left as Oracle: Reforge the Soul's "and draws", Alchemist's Refuge's "nonland cards", Avacyn's "are
    indestructible", Ghostform's "are unblockable", "each turn" (Guise of Fire, Tormentor's Trident), "of their
    choice" sacrifices (Grave Exchange, Rite of Ruin) and "target player" / "creature or player" (Bonfire of the
    Damned, Vigilante Justice, Gang of Devils).
- **Set review: Planechase 2012 Planes, Planechase 2012 and its promo plane (Stairs to Infinity)** (62 new names,
  `Planechase2012AuditTest`):
  - R&D's Secret Lair printed text: the 32 Planechase 2012 planes (through Planechase Anthology, or before March of
    the Machine Commander for its ten reprints) say "Whenever you roll {CHAOS}", like the 2009 planes; *Mass Mutiny*
    (before Commander 2013) targets a creature for each opponent rather than up to one.
  - Left as Oracle: Kilnspire District's "precombat main phase", Windriddle Palaces' "play the top card", Akoum's
    "enchantment cards", Sakashima's Student's "still a Ninja", Silent-Blade Oni's "nonland card", totem armor (now
    umbra armor), Gavony's "are indestructible" and "target player" (Stensia, Kilnspire District).
- **Set review: Magic 2013 Promos (Mwonvuli Beast Tracker, Magmaquake, Cathedral of War, Staff of Nin, Xathrid
  Gorgon) and Magic 2013** (107 new names): no script changes.
  - Left as Oracle: Omniscience's and Yeva's "cards", Mindclaw Shaman's "instant or sorcery card from it",
    Reckless Brute's "each turn", "of their choice" sacrifices (Nefarox) and "target player" / "creature or player"
    (Chandra's Fury, Flames of the Firebrand).
- **Set review: Duel Decks: Izzet vs. Golgari** (Jarad, Golgari Lich Lord; Izzet Charm; Goblin Electromancer; Dreg
  Mangler; Korozda Guildmage; Street Spasm): no script changes.
  - Left as Oracle: Izzet Charm's modes and Korozda Guildmage's token wording (templating).
- **Set review: Return to Ravnica Promos (Archon of the Triumvirate, Supreme Verdict, Hypersonic Dragon)**: no
  script changes.
- **Set review: Return to Ravnica** (221 new names): in-game text only, no rules changes.
  - In-game text: *Teleportal*'s stack text said +2/+0 (it gives +1/+0); *Sphinx of the Chimes*' cost was missing
    its colon. A populate sub-ability no longer adds its own "Populate." line to the card text, which showed it twice
    on *Coursers' Accord*, *Druid's Deliverance*, *Eyes in the Skies*, *Horncaller's Chant*, *Sundering Growth*,
    *Trostani's Judgment* and other later cards (`CopyPermanentEffect`).
  - Left as Oracle: Epic Experiment's "instant and sorcery card with converted mana cost X or less" (CR 709.3a only
    evaluates the half being cast, so it's the same as "spells"), Rootborn Defenses' "are indestructible", Palisade Giant's and Sphere of Safety's "or" wording,
    Rest in Peace's "all cards from all graveyards", Volatile Rig's "each turn", Oak Street Innkeeper's "as long
    as it's not your turn", Slaughter Games' "name a nonland card", "can't be countered by spells or abilities"
    (Abrupt Decay, Counterflux), "target player" (Hellhole Flailer, Rakdos's Return), "of their choice"
    sacrifices (Desecration Demon, Slum Reaper) and mill wording.
- **Set review: Friday Night Magic 2013 (Experiment One, Sin Collector, Ghor-Clan Rampager, Dimir Charm), Duels of the
  Planeswalkers 2014 Promos (Ogre Battledriver, Bonescythe Sliver) and Gatecrash Promos** (13 new names): no script
  changes.
- **Set review: Gatecrash** (219 new names): in-game text only, no rules changes.
  - In-game text: *Domri Rade*'s emblem (stray spaces, missing comma); *Rust Scarab*'s and *Simic Manipulator*'s
    target prompts; *Simic Manipulator*'s cost showed "{0}" and "any number of" counters; *Voidwalk*'s "at the
    beginning of the next end step".
  - Left as Oracle: Thespian's Stage's and Lazav's "gains this ability" / "gains hexproof", Biomass Mutation's
    "become X/X", Sylvan Primordial's one search per destroyed permanent, Diluvian Primordial's "card cast this way
    ... this turn", Crypt Ghast's "add {B}", "are indestructible" (Frontline Medic, Boros Charm, Gideon,
    Champion of Justice), "is unblockable", "target player" / "creature or player" and "of their choice"
    sacrifices.
- **Set review: Dragon's Maze Promos (Trostani's Summoner, Melek, Izzet Paragon, Maze's End, Render Silent, Breaking
  // Entering) and Dragon's Maze** (136 new names): in-game text only, no rules changes.
  - In-game text: *Hired Torturer* (stray spaces), *Master of Cruelties* ("this combat"), *Korozda Gorgon*'s cost,
    *Legion's Initiative*'s delayed trigger, *Reap Intellect*, *Armed // Dangerous*, *Toil // Trouble*, "of their
    choice" (*Catch // Release*, *Far // Away*), *Rot Farm Skeleton*'s "Mill four cards" and *Blood Baron of
    Vizkopa*'s protection order.
  - Left as Oracle: Melek's "cast the top card of your library if it's an instant or sorcery card" (CR 715.3a and
    712.11c only evaluate the Adventure or face being cast, so it's the same as "spells"), Council of the Absolute's "name a card other than a creature or land card" and "cast cards",
    Plasm Capture's "precombat main phase", Vorel's "put another of those counters", Progenitor Mimic's "gains",
    Dragonshift's and Turn's "becomes a 4/4" / "0/1", Blood Scrivener's and Voice of Resurgence's wording.
- **Set review: Magic 2014 Promos (Hive Stirrings, Colossal Whale, Megantic Sliver, Goblin Diplomats), San Diego
  Comic-Con 2013 (Chandra, Pyromaster; Garruk, Caller of Beasts) and Magic 2014** (109 new names): in-game text
  only, no rules changes.
  - In-game text: *Colossal Whale*'s target prompt, *Savage Summoning*'s "additional +1/+1 counter", *Shadowborn
    Apostle*'s cost ("Shadowborn Apostles").
  - Left as Oracle: Jace's Mindseeker's "instant or sorcery card" (same as "spell" under CR 715.3a and 712.11c),
    Corpse Hauler's "another" (it's already sacrificed), Savage Summoning's "creature card",
    Liliana's Reaver's and Xathrid Necromancer's "token tapped", Dragon Egg's "It has", "of their choice"
    sacrifices and "target creature or player".
- **Set review: Duel Decks: Heroes vs. Monsters (Ordeal of Purphoros, Polukranos, Satyr Hedonist, Cavalry Pegasus,
  Anax and Cymede), Theros Promos and Theros** (214 new names; the Theros Hero's Path cards aren't game cards):
  - Fixed: *Fate Foretold* had two SVars named TrigDraw, so its enter trigger used the dies trigger's draw (same
    player, but by accident).
  - Left as Oracle: Prophet of Kruphix's "creature cards" (CR 601.3e), Artisan of Forms' "gain this ability",
    "each turn" (Deathbellow Raider), menace and "target creature or player" / "target player" wording, and "of
    their choice" sacrifices.
- **Set review: Face the Hydra**: its 10 Hydra deck cards aren't in Forge.
- **Set review: Commander 2013** (51 new names, `Commander2013AuditTest`):
  - R&D's Secret Lair printed text: *Marath, Will of the Wild*'s "X may be 0" printed text now stops at Commander
    Legends, which added "X can't be 0"; the Magic Online promo (December 2020) gets a `printDate` too.
  - Left as Oracle: Darksteel Mutation's "is a 0/1 Insect artifact creature" (it already replaces the other types),
    Tempt with Immortality's and Tempt with Vengeance's "for each player who does", Opal Palace's "a number of
    +1/+1 counters", Jeleva's "instant or sorcery card", Hooded Horror's "if" and Primal Vigor's token wording.
- **Set review: Friday Night Magic 2014, Born of the Gods Promos and Born of the Gods** (163 new names,
  `BornOfTheGodsAuditTest`; the Born of the Gods Hero's Path cards aren't game cards):
  - Fixed: *Kraken of the Straits* also stopped creatures whose power equaled the number of Islands from blocking.
  - In-game text: *Forgestoker Dragon* said its damage couldn't be regenerated; *Noble Quarry*'s two lure lines are
    one, as on the card.
  - Left as Oracle: Courser of Kruphix's "play the top card of your library if it's a land card" (only the face
    played counts, as with spells), Oracle of Bones' "instant or sorcery card", Gild's Gold token, Brimaz's
    "attacking" / "blocking" tokens, "each turn" and "target creature or player" wording.
- **Set review: Battle the Horde**: only Unquenchable Fury is in Forge (no changes); the other Horde deck cards aren't.
- **Set review: Journey into Nyx Promos and Journey into Nyx** (157 new names; the Hero's Path cards aren't game
  cards): in-game text only, no rules changes.
  - In-game text: *Font of Fertility* (stray spaces), and target prompts or sub-ability text on *Reviving Melody*,
    *Leonin Iconoclast* and *Quarry Colossus*.
  - Left as Oracle: Twinflame's "Those tokens have haste", Godsend's "cast cards", Wildfire Cerberus's "creature
    your opponents control", Market Festival's "adds two mana", Kruphix's mana-pool wording, menace and "each turn".
- **Set review: Defeat a God**: its 8 Xenagos deck cards aren't in Forge.
- **Set review: Conspiracy** (65 new names): in-game text only, no rules changes.
  - In-game text: the hidden agendas *Immediate Action*, *Secrets of Paradise* and *Brago's Favor* ("with the
    chosen name"), *Worldknit*'s condition and "any color", *Marchesa, the Black Rose*'s garbled return trigger,
    *Grenzo, Dungeon Warden*'s "Grenzo enters".
  - Left as Oracle: Brago's "When", Dack's Duplicate's "gains haste and dethrone", Muzzio's "reveal an artifact
    card" and "of their choice" sacrifices. The draft-matters text isn't shown in game (Forge doesn't run drafts
    with it).
- **Set review: San Diego Comic-Con 2014, Duels of the Planeswalkers 2015 Promos, the M15 Prerelease Challenge (Garruk
  the Slayer), Magic 2015 Promos and Magic 2015** (142 new names): in-game text only, no rules changes.
  - In-game text: *Yisan, the Wanderer Bard*'s cost ("Put a Verse counter on this creature").
  - Left as Oracle: Aetherspouts' "top or bottom", Generator Servant's "If that mana", Act on Impulse's "cards
    exiled this way", Spectra Ward's "all colors", Mercurial Pretender's "gains", Avarice Amulet's "When",
    menace, "each turn" and "target creature or player" wording.
- **Set review: Duel Decks: Speed vs. Cunning (Mardu Heart-Piercer, Mystic Monastery, Nomad Outpost, Zurgo
  Helmsmasher, Thousand Winds, Jeskai Elder), Khans of Tarkir and Khans of Tarkir Promos** (228 new names,
  `KhansOfTarkirAuditTest`):
  - R&D's Secret Lair printed text: *Taigam's Scheming* and *Sultai Ascendancy* (Khans of Tarkir, their only
    printing) look at the top cards and put any number into the graveyard, which isn't a surveil, so "whenever you
    surveil" doesn't trigger.
  - In-game text: *Howl of the Horde* ("You may choose new targets for the copy"), *Narset, Enlightened Master*,
    *Quiet Contemplation*, the costs of *Necropolis Fiend* ("Exile X cards") and *Retribution of the Ancients*
    ("counters"), *Sorin, Solemn Visitor*'s emblem ("of their choice"), stray spaces in *Embodiment of Spring* and
    *Abzan Banner*.
  - Left as Oracle: Raid's "attacked with a creature", Anafenza's graveyard replacement (its 2014 ruling already
    read it the Oracle way), Villainous Wealth's "nonland cards", Ashcloud Phoenix's "under your control", Lens of
    Clarity's "any time". The five Morph—Reveal cards still say "from your hand" (Oracle: "in your hand").
- **Set review: Commander 2014** (59 new names): in-game text only, no rules changes.
  - In-game text: an instant or sorcery's casting restriction (`Text:`) is its own paragraph instead of running into
    the spell's text (*Wake the Dead*, and older *Aleatory*, *Chaotic Strike*, *Choking Vines*, *Curtain of Light*,
    *Dazzling Beauty*, *Disharmony*, *False Orders*, *Festival*, *Fog Patch*, *Panic*, *Rapid Fire*, *Savage Beating*,
    *Suffocation*); *Domineering Will* no longer repeats "They block this turn if able."
  - Left as Oracle: Masterwork of Ingenuity's and Containment Priest's "enter the battlefield", Assault Suit's "a
    planeswalker you control", "of their choice" sacrifices and token templating.
- **Set review: Friday Night Magic 2015, Ugin's Fate and Fate Reforged** (145 new names):
  - Fixed: *Lightning Shrieker* shuffled itself away only at your own end step; it's every end step.
  - In-game text: a bolster or adapt sub-ability no longer adds its own "Bolster N." / "Adapt N." line (*Abzan
    Advantage*, *Honor's Reward*, *Map the Wastes*, and later *Pinion Feast* and *Jetfire, Ingenious Scientist*;
    *Enduring Victory*, which relied on it, now says it itself); *Daghatar the Adamant*'s "Daghatar enters".
  - Left as Oracle: Renowned Weaponsmith's "{2}", Ainok Guide's "on top of it", "with flying you control" and
    mill / token templating.
- **Set review: Fate Reforged Promos** (15 new names): in-game text only — *Flamerush Rider* said "Exile the token
  at end of combat" twice.
- **Set review: Dragons of Tarkir and Dragons of Tarkir Promos** (228 new names): in-game text only, no rules
  changes.
  - In-game text: *Hedonist's Trove*'s "You can't cast more than one spell this way each turn", *Learn from the
    Past*'s doubled "Draw a card.", *Corpseweft*'s cost ("Exile one or more creature cards"), a typo in *Revealing
    Wind*'s prompt; *Pinion Feast* is fixed by the bolster change above.
  - Left as Oracle: Hedonist's Trove's "land cards" / "nonland cards", Illusory Gains, Narset Transcendent's "cast
    your next", menace wording.
- **Set review: Magic Origins Clash Pack, Magic Origins and Magic Origins Promos** (178 new names): in-game text
  only, no rules changes.
  - In-game text: missing spaces in *Flameshadow Conjuring*, *Psychic Rebuttal* and *Send to Sleep*; *Despoiler of
    Souls*' cost ("Exile two other creature cards").
  - Left as Oracle: Sphinx's Tutelage's "If they're both", Mage-Ring Network's printed "Remove X", Talent
    of the Telepath's "card", Prickleboar, Hixus and token / mill templating.
- **Set review: Duel Decks: Zendikar vs. Eldrazi, Battle for Zendikar, Zendikar Expeditions and Battle for
  Zendikar Promos** (228 new names): in-game text only, no rules changes.
  - In-game text: *Titan's Presence*'s cost ("reveal a colorless creature card"), *Transgress the Mind* showed only
    "Target player reveals their hand."
  - Left as Oracle: the printed "{2}" mana of Kozilek's Channeler, Hedron Archive and others, Brutal Expulsion's
    "put into a graveyard", Conduit of Ruin's "on top of it".
- **Set review: Commander 2015** (56 new names):
  - Fixed: *Broodbirth Viper* drew only on combat damage to an opponent; it's any player (combat damage can be
    redirected to its controller).
  - In-game text: *Gigantoplasm*'s granted ability ("base power and toughness X/X"), *Centaur Vinecrasher*'s "a number
    of +1/+1 counters", *Karlov of the Ghost Council*'s cost ("from Karlov").
  - Left as Oracle: Ezuri's Predation's Phyrexian Beasts (a creature type gained since), Dream Pillager's "nonland
    cards", Mirror Match's and Gigantoplasm's templating, mill / token wording.
- **Set review: Commander 2015 Tokens** — not in Forge: its one new card, *Experience*, is a counter reminder card, not
  a game object.
- **Set review: Friday Night Magic 2016, Oath of the Gatewatch and Oath of the Gatewatch Promos** (176 new names):
  - In-game text: *Nissa's Judgment* ("Choose up to one target creature an opponent controls" ... "that creature"),
    *Dazzling Reflection*'s missing prevention sentence, *Kozilek, the Great Distortion*'s cost ("Discard a card with
    mana value X"), *Grip of the Roil* repeated "Draw a card." after its surge line.
  - Left as Oracle: Goblin Dark-Dwellers' printed "If that card would be put into your graveyard this turn" (the
    effect can follow the card onto the stack and no further, CR 400.7h, so it's the Oracle "that spell"), Iona's
    Blessing's "each combat", Eldrazi Mimic's "become equal to", Fall of the Titans' "targets", Loam Larva's "on top
    of it", Ruins of Oran-Rief's "entered this turn", token templating.
- **Set review: Duel Decks: Blessed vs. Cursed** (6 new names): no changes.
- **Set review: Shadows over Innistrad Promos and Shadows over Innistrad** (260 new names): in-game text only, no rules
  changes.
  - In-game text: *Avacyn's Judgment*'s madness damage is "divided as you choose among those permanents and/or
    players", *Essence Flux*'s missing "If it's a Spirit, put a +1/+1 counter on it.", *Sinister Concoction*'s cost
    order.
  - Left as Oracle: Not Forgotten's "your choice of the top or bottom", Tireless Tracker's Landfall ability word,
    Brain in a Jar's "card", Fevered Visions' "him or her", mill / token / "any target" templating.
- **Set review: Eldritch Moon and Eldritch Moon Promos** (199 new names, `EldritchMoonAuditTest`):
  - R&D's Secret Lair printed text: *Contingency Plan* (every printing) and *Grim Flayer* (before Duskmourn
    Commander; Double Masters 2022 still printed the old text) look at the top cards and put any number into the
    graveyard, which isn't a surveil, so "whenever you surveil" doesn't trigger.
  - In-game text: a cost with an amount like X now counts plurally — "discard X cards" (*Nahiri's Wrath*, *Firestorm*,
    *Conflagrate*, *Gix, Yawgmoth Praetor*, *Devastating Dreams*' "at random" and others), "return X Swamps you
    control to their owner's hand" (*Infernal Harvest*), "remove X loyalty counters from among" (*Light Up the
    Night*, *Hierophant Bio-Titan*; the stray double space before "among" is gone for every amount).
  - Left as Oracle: Gisa and Geralf's "Once during each of your turns" (the release notes already read the printed
    text that way), Coax from the Blind Eternities' "face-up" (a face-down exiled card has no characteristics, so it
    was never an Eldrazi card; CR 406.3a), Tamiyo's emblem "spells", mill / token templating.
- **Set review: Conspiracy: Take the Crown** (79 new names, `ConspiracyTakeTheCrownAuditTest`):
  - R&D's Secret Lair printed text: *Volatile Chimera* (its only printing) becomes a copy and then gains its ability,
    so the ability isn't part of the copy: something that copies the Chimera doesn't get it.
  - In-game text: *Garbage Fire* showed no text (its description was in the wrong field), *Caller of the
    Untamed*'s missing colon, *Assemble the Rank and Vile*'s "create a tapped 2/2 black Zombie creature token".
  - Left as Oracle: Sovereign's Realm's "Your deck", Hold the Perimeter's "This creature can't block", Custodi
    Soulcaller's "attacked with a creature", Regal Behemoth's "add one mana" (no "additional"). The draft-matters
    text (Animus of Predation, Leovold's Operative and the rest) isn't shown, as Forge doesn't run that part.
- **Set review: Kaladesh, Kaladesh Inventions and Kaladesh Promos** (248 new names):
  - In-game text: an energy payment of six or more reads "Pay eight {E}" instead of a row of symbols (*Aetherworks
    Marvel*, *Aethersquall Ancient*, *Aethertorch Renegade*, *Architect of the Untamed*, *Era of Innovation* and
    later cards); *Failed Inspection*'s missing "then discard a card", *Disappearing Act*'s "a permanent".
  - Left as Oracle: Torrential Gearhulk's printed "that card ... this turn" (CR 400.7h, as Goblin Dark-Dwellers),
    Aether Meltdown's "Enchanted permanent", Rashmi's "nonland card", Gonti's "look at it",
    Aetherworks Marvel's "card", Chandra, Pyrogenius's and Aethertorch Renegade's "target player", Authority of the
    Consuls' "enters under an opponent's control", mill / token / "any target" templating.
- **Set review: Commander 2016** (56 new names):
  - Fixed: *Vial Smasher the Fierce* could only damage the randomly chosen opponent; it may hit a planeswalker that
    player controls instead.
  - In-game text: *Reyhan, Last of the Abzan*'s "Reyhan enters".
  - Left as Oracle: Magus of the Will's "play cards", Tymna's "your postcombat main phase", Grip of Phyresis'
    Phyrexian Germ, Frenzied Fugue's "or", Faerie Artisans' "enters under an opponent's control".
- **Set review: Friday Night Magic 2017** (3 new names): no changes.
- **Set review: Judge Gift Cards 2017** (1 new name, *Rules Lawyer*): no changes.
- **Set review: Aether Revolt and Aether Revolt Promos** (182 new names): in-game text only, no rules changes.
  - In-game text: *Gonti's Aether Heart*'s "Pay eight {E}" (the energy change above); *Pacification Array*'s and *Heart
    of Kiran*'s prompts.
  - Left as Oracle: Revolt's "a permanent you controlled left the battlefield", the Expertises' "card", Kari Zev's
    Ragavan wording, Winding Constrictor's "placed", Pia's Revolution's "him or her".
- **Set review: Amonkhet, Amonkhet Invocations and Amonkhet Promos** (244 new names, `AmonkhetAuditTest`):
  - Fixed: *Watchers of the Dead* also exiled its controller's graveyard; only opponents' graveyards are affected.
    *Dispossess* could target yourself; it's "target opponent".
  - R&D's Secret Lair printed text: *Naga Oracle* (every printing) looks at the top three cards and puts any number
    into the graveyard, which isn't a surveil, so "whenever you surveil" doesn't trigger.
  - In-game text: *Commit // Memory*'s "hand and graveyard".
  - Left as Oracle: Bounty of the Luxa's "precombat main phase" (now "first main phase"), Vizier of the Menagerie's
    "cast the top card ... if it's a creature card", Injury's "target player", Cruel Reality's "of their choice",
    mill / token templating.
- **Set review: Archenemy: Nicol Bolas Schemes** (19 new names, `HourOfDevastationAuditTest`):
  - Fixed: *Pay Tribute to Me* made each opponent sacrifice a second creature whenever you controlled any land; it
    takes six or more. *Every Dream a Nightmare* never drew you any cards for discarded lands.
  - Left as Oracle: Know Evil's "You can't target any player more than once", Power Without Equal's and When Will
    You Learn?'s "nonland cards".
- **Set review: Hour of Devastation and Hour of Devastation Promos** (183 new names, `HourOfDevastationAuditTest`):
  - Fixed: *Hour of Glory* never exiled a God's same-named cards from its controller's hand.
  - In-game text: *Unesh, Criosphinx Sovereign*'s "top four cards"; *Dust*'s target count reads its -1/-1 counters
    properly (it already allowed every legal target).
  - Left as Oracle: Nicol Bolas, God-Pharaoh's "target opponent or creature an opponent controls", Majestic
    Myriarch's intervening "if" (it only moves when the condition is checked), Hazoret's Undying Fury's, Wildfire
    Eternal's and Abandoned Sarcophagus' "cards", Neheb's "your postcombat main phase", mill templating.
- **Set review: Commander 2017** (56 new names): no changes.
  - Left as Oracle: Kess, Dissident Mage's printed "During each of your turns ... If a card cast this way would be put
    into your graveyard this turn" (its rulings already allow one card a turn, and "that card" can't be followed past
    the stack, CR 400.7h), Portal Mage's "player or planeswalker", Teferi's Protection's "have protection", Kheru
    Mind-Eater's and Magus of the Mind's "play cards".
- **Set review: HasCon 2017** (3 new names): no changes.
- **Set review: 2016 Heroes of the Realm** (3 new names): no changes (Dungeon Master shows its "1d4" loyalty roll as
  a line of its own).
- **Set review: Ixalan and Ixalan Promos** (245 new names, `IxalanAuditTest`):
  - Fixed: *Raiders' Wake*'s raid trigger also fired at each opponent's end step.
  - R&D's Secret Lair: *Search for Azcanta* printed before Secret Lair's Heart of the Explorer (2023-11-20, now its own
    print date) looks at the top card and may put it into the graveyard, which isn't a surveil; *Hostage Taker*
    printed before Adventures in the Forgotten Realms Commander can exile itself.
  - In-game text: *Ashes of the Abhorrent*'s static reads as one sentence; *Dire Fleet Daredevil*'s replacement says
    "a graveyard".
  - Left as Oracle: raid's "attacked with a creature", Sunbird's Invocation's "card", Settle the Wreckage's "their
    library", Tocatli Honor Guard's "entering the battlefield", Hostage Taker's mana wording.
- **Set review: Unstable** (241 new names, `UnstableRivalsAuditTest`):
  - R&D's Secret Lair: *Ordinary Pony* printed before Unsanctioned can flicker the same creature with each Pony
    trigger.
  - In-game text: *Box of Free-Range Goblins*, *Steamfloggery*, *Time Out* and Unglued's *Growth Spurt* start with
    "Roll a six-sided die."; *Hammer Helper*'s "and roll a six-sided die"; *Time Out*'s "where X is the result";
    *Steamfloggery*'s assemble reminder text. Engine: an ability step with no text of its own (like a die roll
    mid-chain) no longer leaves a double space.
  - Left as Oracle: Clocknapper's, X's, Spike's, Gimme Five's and Side Quest's "silver-bordered", Boomflinger's
    "target player".
- **Set review: Rivals of Ixalan and Rivals of Ixalan Promos** (178 new names, `UnstableRivalsAuditTest`):
  - Fixed: *Tetzimoc, Primal Death*'s prey ability didn't actually reveal it.
  - In-game text: *Induced Amnesia*'s "then draws that many cards".
  - Left as Oracle: Angrath's "target opponent", "card"/"spell" wording.
- **Set review: Dominaria and Dominaria Promos** (231 new names, `DominariaAuditTest`):
  - Fixed: *The Flame of Keld*'s chapter III also added 2 to opponents' red sources' damage.
  - R&D's Secret Lair: *Teferi, Hero of Dominaria* printed before Secret Lair's (2021-02-22, now its own print date)
    untaps exactly two lands, so it must untap two tapped lands if there are any, an opponent's if need be
    (CR 701.26b).
  - In-game text: *Fall of the Thran*'s chapters II and III had no text; *Traxos, Scourge of Kroog*'s "enters tapped"
    showed twice; *Arbor Armament*'s "That creature gains reach until end of turn."; *Darigaaz Reincarnated*'s
    "this card"; Teferi's delayed trigger says "up to two lands".
  - Left as Oracle: Muldrotha's "play up to one permanent card of each permanent type", Rona's, Stolen Strategy's
    and Jaya Ballard's emblem's "cards", Precognition Field, Tatyova's added ability word, Helm of the Host's "if
    equipped creature is legendary", mill and planeswalker-redirect wording. Firesong and Sunspeaker's "target
    creature or player" is its Oracle text.
- **Set review: Battlebond and Battlebond Promos** (85 new names):
  - In-game text: *Decorated Champion*'s trigger was hidden.
  - Left as Oracle: Gorm the Great's two "must be blocked" lines (they're keyword lines), "under your team's
    control", "sacrifices a creature they control".
- **Set review: Global Series Jiang Yanggu & Mu Yanling** (30 new names):
  - In-game text: *Sacred White Deer*'s "Activate only if you control a Yanggu planeswalker."
- **Set review: Core Set 2019 and Core Set 2019 Promos** (181 new names, `CoreSet2019AuditTest`):
  - Fixed: *Ursine Champion* lost its Berserker type (it became a "Beserker"); *Sarkhan's Dragonfire* let you order
    the cards it puts on the bottom instead of putting them in a random order.
  - In-game text: *Bone Dragon*'s "Exile seven other cards", *Vivien Reid*'s "artifact, enchantment, or creature",
    *Apex of Power*'s "spells from among them", *Vivien's Invocation*'s "damage equal to".
  - Left as Oracle: Doublecast's "When you next cast", Mirror Image's "any creature", Palladia-Mors' "if",
    Liliana, Untouched by Death's "Zombie cards", mill wording.
- **Set review: 2017 Heroes of the Realm** (3 new names):
  - Fixed: *Inzerva, Master of Insights*'s -2 looked only at opponents' libraries, not each other player's; its "Scry
    2." wasn't shown.
- **Set review: Commander 2018** (59 new names, `CoreSet2019AuditTest`):
  - R&D's Secret Lair: *Yennett, Cryptic Sovereign* printed before Commander Masters draws only when the revealed card's
    mana value is even, so declining to cast an odd card draws nothing (its Secret Lair printings now carry their own
    print date).
  - In-game text: *Echo Storm*'s and *Fury Storm*'s "You may choose new targets for the copies."; *Varchild, Betrayer
    of Kjeldor*'s and *Xantcha, Sleeper Agent*'s split lines read as one; *Estrid's Invocation*'s "you may" and "its
    owner's".
  - Left as Oracle: Saheeli, the Gifted's printed "costs {1} less ... for each artifact" (the same as affinity),
    Ever-Watching Threshold's trigger condition, Xantcha's "As ... enters, an opponent ... gains control of it",
    Aminatou's Augury's "card", Boreas Charger's split sentences, "Totem armor".
- **Set review: Guilds of Ravnica and Guilds of Ravnica Promos** (234 new names, `GuildsOfRavnicaAuditTest`):
  - Fixed: *Torch Courier* could target itself (it gives another creature haste).
  - R&D's Secret Lair: *Invert* (its only printing has no "until end of turn") switches power and toughness
    indefinitely (CR 611.2a).
  - In-game text: *Intervention* said "Target player deals 3 damage"; *Mephitic Vapors* didn't show "Surveil 2.";
    *Cosmotronic Wave*, *Gruesome Menagerie*, *Mission Briefing*, *Vraska's Stoneglare* and *Experimental Frenzy*
    repeated part of their text; *Thief of Sanity*, *Mnemonic Betrayal*, *Dispersal* and *Statue* now match Oracle;
    *Moodmark Painter*'s "can't be blocked" reminder; *Mausoleum Secrets*' search prompt says "your graveyard".
  - Left as Oracle: Experimental Frenzy's "play the top card" / "can't play cards from your hand", Omnispell Adept's
    and Mnemonic Betrayal's "cards", Thief of Sanity's "you may look at it", Kraul Harpooner's word order, mill and
    "shuffle their library" wording. The Lockets' printed hybrid costs are a data artifact, not a difference.
- **Set review: M19 Gift Pack** (5 new names): no changes.
- **Set review: Game Night** (5 new names): no changes.
- **Set review: Ravnica Allegiance and Ravnica Allegiance Promos** (242 new names, `WarOfTheSparkAuditTest`):
  - Fixed: *Orzhov Locket*'s draw ability didn't tap it, so it could also be tapped for mana that turn.
  - In-game text: *Amplifire*'s "its base toughness becomes twice that card's toughness".
- **Set review: Mythic Edition** (2 new names): no changes.
- **Set review: War of the Spark and War of the Spark Promos** (245 new names, `WarOfTheSparkAuditTest`):
  - Fixed: *Tolsimir, Friend to Wolves* could only fight an opponent's creature, not any creature you don't control.
  - In-game text: *Finale of Revelation*'s "Exile Finale of Revelation.", *Time Wipe* repeated "Destroy all
    creatures.", *Soul Diviner*'s cost comma, *Courage in Crisis*'s proliferate reminder; *Prison Realm*'s target
    prompt mentions planeswalkers.
  - Left as Oracle: "amass N" (now "amass Zombies N"), Karn, the Great Creator's "in exile" (a face-down card has no
    characteristics, CR 406.3a), Vivien, Champion of the Wilds' "you may look at that card", Bolas's Citadel's "play
    the top card", Dreadhorde Arcanist's and Finale of Promise's "card ... this turn" (CR 400.7h), Spark Double's
    "if that permanent is legendary", ability words, mill wording.
- **Set review: Modern Horizons** (209 new names, `Commander2019AuditTest`):
  - In-game text: *Vengeful Devil*'s "Morbid —", *Kaya's Guile*'s "Exile all opponents' graveyards.", *Pyrophobia*
    repeated "Cowards can't block this turn." Engine: entwine's reminder says "Choose all" rather than "Choose both"
    on a charm with more than two modes (Kaya's Guile; Untap, Upkeep, Draw's Oracle says "all of them").
  - Left as Oracle: Watcher for Tomorrow's printed "Hideaway" (it already included entering tapped), Cabal
    Therapist's "precombat main phase", Collected Conjuring's "sorcery cards", Splicer's Skill's Golem type (the token
    gained Phyrexian later), "Totem armor", shuffle wording.
- **Set review: Core Set 2020 and Core Set 2020 Promos** (174 new names):
  - Fixed: *Cavalier of Thorns* didn't reveal the cards it looks at.
  - In-game text: *Goldmane Griffin* named "Ajani, Valiant" instead of Ajani, Inspiring Leader; *Noxious Grasp*
    repeated "You gain 1 life."
  - Left as Oracle: Mystic Forge's "cast the top card ... if it's an artifact card or a colorless nonland card",
    Aether Gust's "on the top or bottom", Vivien, Arkbow Ranger's "choose ... reveal it", Chandra, Acolyte of Flame's
    "that card ... this turn" (CR 400.7h), Omnath's ability word, mill wording.
- **Set review: 2018 Heroes of the Realm** (4 new names, `Commander2019AuditTest`):
  - Fixed: *The Legend of Arena*'s planeswalker got its extra loyalty after entering instead of entering with it;
    its chapters' second sentences weren't shown.
- **Set review: Commander 2019** (57 new names): no changes.
  - Left as Oracle: Elsha of the Infinite's "cast the top card ... if it's a noncreature, nonland card", Chainer's
    "creature card", Wall of Stolen Identity's "any creature".
- **Set review: Throne of Eldraine and Throne of Eldraine Promos** (269 new names):
  - In-game text: *Yorvo, Lord of Garenbrig*'s "Yorvo enters with four +1/+1 counters"; *Escape to the Wilds*' effect
    text was garbled; *Ogre Errant*'s target prompt says "Knight".
  - Left as Oracle: Bartered Cow's "dies or when you discard it", Hushbringer's and Mirrormade's "enters the
    battlefield", Archon of Absolution's "you or a planeswalker", affinity spelled out on The Circle of Loyalty and
    Emry, mill and shuffle wording.
- **Set review: Ponies: The Galloping** (3 new names): no changes.
- **Set review: Mystery Booster Playtest Cards 2019** (119 new names, `ThroneOfEldraineAuditTest`):
  - In-game text: *Time Sidewalk*'s opening-hand ability wasn't shown (engine: an instant or sorcery now shows its
    "If this card is in your opening hand" text); *Command the Chaff*'s "Exile Command the Chaff."; *Personal Decoy*'s
    "You can't be attacked."; *Underdark Beholder*'s Oracle wording.
- **Set review: Game Night 2019** (5 new names): no changes.
- **Set review: Theros Beyond Death and Theros Beyond Death Promos** (246 new names, `TherosBeyondDeathAuditTest`):
  - Fixed: *Rage-Scarred Berserker* and *Nylea's Huntmaster* could target any creature, not just one you control;
    *Kiora Bests the Sea God*'s chapter II kept only creatures tapped, not every nonland permanent; *Uro, Titan of
    Nature's Wrath*'s attack trigger was optional.
  - R&D's Secret Lair: *Eat to Extinction* (every printing says "Look at the top card of your library. You may put
    that card into your graveyard.", not "Surveil 1") doesn't surveil, so surveil triggers don't see it.
  - In-game text: *Erebos's Intervention*'s first mode dropped "You gain X life."; *Skophos Warleader* and
    *Slaughter-Priest of Mogis* say "another creature or an enchantment"; *Underworld Charger*'s escape cost says
    "three other cards".
  - Cleanup: *Tectonic Giant*'s attack trigger carried an unused source filter.
- **Set review: Theros Beyond Death Tokens** (1 new name): no changes. *Acorn Stash* is a play aid for acorn counters
  with no rules text, so it isn't in Forge.
- **Set review: Unsanctioned** (16 new names, `Commander2020AuditTest`):
  - In-game text: *B.O.B. (Bevy of Beebles)* showed its loyalty line twice.
  - Left as Oracle: *Underdome*'s printing says "silver-bordered costs"; Un-costs are the same thing, since acorn
    counts as silver-bordered.
- **Set review: Commander 2020** (71 new names, `Commander2020AuditTest`):
  - Fixed: *Jirina Kudro* made Human tokens instead of Human Soldiers; *Selective Adaptation* skipped double strike;
    *Spellpyre Phoenix*'s return wasn't optional.
  - In-game text: a cost that puts a counter names it in lowercase, as removing one already did (*Netherborn Altar*'s
    "Put a soul counter", *Mazemind Tome*, *Bloodletter Quill*, *Jessica Jones*, *Yisan*); *Souvenir Snatcher*'s
    target prompt says "noncreature artifact".
  - Left as Oracle: Haldan's "play noncreature cards ... if you exiled them", Tayam's mill wording.
- **Set review: Ikoria: Lair of Behemoths and Ikoria Promos** (228 new names):
  - Fixed: *Kaheera, the Orphanguard* would pump itself if it became an Elemental.
  - R&D's Secret Lair: *Titans' Nest* (its only printing looks at the top card and may put it into the graveyard)
    doesn't surveil, so surveil triggers don't see it (Think Tank precedent).
  - In-game text: a "reveal a creature card" cost no longer says "Creature card" (*Charge of the Forever-Beast*,
    *Disaster Radius*, *Induce Despair*, *Living Destiny*); *Indatha Crystal*'s mana text had stray spaces;
    *Wingspan Mentor*'s and *Duskfang Mentor*'s target prompts say "you control".
  - Left as Oracle: Lurrus's "During each of your turns, you may cast one permanent spell", Gyruda's and Skull
    Prophet's mill wording, Lavabrink Venturer's "chosen value".
- **Set review: Jumpstart Front Cards** (46 new names): no changes. The theme cards have no rules text and aren't in
  Forge.
- **Set review: Core Set 2021 and Core Set 2021 Promos** (183 new names, `ZendikarRisingAuditTest`):
  - Fixed: *Warden of the Woods*' draw wasn't optional; *Barrin, Tolarian Archmage* could return itself if it was
    also a planeswalker.
  - In-game text: *Rookie Mistake* and *Gnarled Sage* read in Oracle order; *Invigorating Surge*'s target prompt.
  - Left as Oracle: Chandra, Flame's Catalyst's "would be put into your graveyard this turn" (CR 400.7h), Sanctum of
    All's "an ability", the Sanctums' "precombat main phase".
- **Set review: Jumpstart** (37 new names, `ZendikarRisingAuditTest`):
  - Fixed: *Archaeomender* targeted "an artifact you control" in the graveyard; it's an artifact card you own.
  - In-game text: a "with different names" cost reads "cards" and "names" (*Ormos, Archive Keeper*'s "Discard three
    cards with different names", *Transmutation Font*'s "artifact tokens with different names" instead of
    "nameses").
  - Left as Oracle: Scholar of the Lost Trove's "this turn" (CR 400.7h), the Thriving lands' "enters the battlefield
    tapped".
- **Set review: 2019 Heroes of the Realm** (7 new names): no changes.
- **Set review: Zendikar Rising and Zendikar Rising Promos** (255 new names, `ZendikarRisingAuditTest`):
  - In-game text: kicker shows "X can't be 0." (*Thieving Skydiver*); *Tazri, Beacon of Unity* lists Wizard;
    *Pyroclastic Hellion* and *Sea Gate Stormcaller* show their full triggers; *Grakmaw*'s "Grakmaw enters with",
    *Mesa Lynx*'s and *Shatterskull Charger*'s Oracle wording; *Zareth San*'s cost said "you control" twice.
  - Left as Oracle: Drana, the Last Bloodchief's "a +1/+1 counter" (it adds to other enters-with counters either
    way), "enters the battlefield" wording.
- **Set review: Zendikar Rising Substitute Cards** (1 new name): no changes. The substitute card isn't a game object.
- **Set review: Zendikar Rising Commander** (6 new names): no changes.
- **Set review: Secret Lair Drop** (73 cards first printed there, `CommanderLegendsAuditTest`; its reversible reprints
  were already reviewed in their own sets):
  - Fixed: *The Celestial Toymaker* never put the chosen pile into your hand (it looked for the exiled cards in the
    library).
  - In-game text: partner variants read "Partner—Survivors" like Oracle (was "Partner — Survivors"), and the
    Unknown Event's *Ready to run* shows as itself with its own reminder text.
  - Left as Oracle: the Universes Within names' printed text (Greymond, Hansk, ...), which only differs in the name.
- **Set review: Commander Legends** (171 new names, `CommanderLegendsAuditTest`):
  - In-game text: *Alharu, Solemn Ritualist*'s token trigger wasn't shown; an X tap cost reads "Tap X untapped
    artifacts you control" (*Glacian*, *Merchant's Dockhand*, *Aryel*, *Hazel of the Rootbloom*, ...; *Glacian* said
    "artifacts you controls you control"); *Jeska's Will* and *Gnostro*'s choose-one text; *Slurrk*'s "Slurrk enters
    with"; *Keskit*'s "artifacts and/or creatures".
  - Left as Oracle: the printed "Golem" tokens (now Phyrexian Golem), Sphinx of the Second Sun's and Belbe's
    postcombat main phase wording, the Wills' "choose both" without "instead"; Nevinyrral still lists its three
    hexproof qualities on separate lines.
- **Set review: Kaldheim Commander** (16 new names, `KaldheimAuditTest`):
  - R&D's Secret Lair: *Hero of Bretagard* and *Ranar the Ever-Watchful* (their only printing says "Whenever you
    exile one or more cards from your hand and/or permanents from the battlefield") count cards leaving your hand only
    when a spell or ability you control exiles them (Oracle update, 2021-06-18: any exile from your hand counts).
- **Set review: Kaldheim and Kaldheim Promos** (287 new names, `KaldheimAuditTest`):
  - Fixed: *Spectral Steel* could never return an Aura (malformed target filter); *Cinderheart Giant* targeted its
    random creature, so hexproof creatures were never picked (Oracle doesn't target); *Rampage of the Valkyries*
    made only opponents sacrifice, not each other player; *Arni Brokenbrow*'s boast wasn't optional.
  - In-game text: *Blizzard Brawl* reads in Oracle order; *Great Hall of Starnheim*'s "Sacrifice this land and a
    creature you control"; *Icebreaker Kraken*'s "snow lands" (was "Land.Snows"); *Brinebarrow Intruder*'s target
    prompt.
  - Left as Oracle: the Sagas' "this Saga", "precombat" and "enters the battlefield" wording, Run Ashore's "their
    choice of", Bounty of Skemfar's "up to one".
- **Set review: Commander 2021** (81 new names, `StrixhavenAuditTest`):
  - Fixed: *Author of Shadows* exiled every graveyard you don't own (a Two-Headed Giant teammate's too), not just
    opponents'.
  - R&D's Secret Lair: *Laelia, the Blade Reforged* (Commander 2021's printing says "Whenever you exile one or more
    cards from your library and/or your graveyard") counts only exiles a spell or ability you control makes; Battle
    for Baldur's Gate's printing and Oracle count any.
  - In-game text: *Sly Instigator* said "can't be blocked this turn" (it lasts until your next turn).
- **Set review: Strixhaven: School of Mages and Strixhaven Promos** (274 new names, `StrixhavenAuditTest`):
  - Fixed: *Lukka, Wayward Bonder*'s -2 exiled the creature at the next end step, not your next upkeep;
    *Letter of Acceptance*'s draw ability didn't need {T}; *Dueling Coach* could only target creatures you control.
  - R&D's Secret Lair: *Curate* (Strixhaven's printing looks at the top two cards; The Brothers' War's is the first to
    say "Surveil 2") doesn't surveil; *Elemental Expressionist* (its printings say "When you exile this creature")
    makes its Elemental only when a spell or ability its controller controls exiles the creature (Oracle update,
    2021-04-16).
  - In-game text: *Infuse with Vitality*, *Show of Confidence*, *Snow Day*, *Mage Hunters' Onslaught* and *Draconic
    Intervention* showed only part of their text; *Draconic Intervention*'s "Instant;Sorcery card"; *Rootha*'s
    "spell you control"; *Academic Probation*'s and *Hofri Ghostforge*'s Oracle wording; prompts on *Tenured
    Inkcaster* and *Vanishing Verse*.
  - Left as Oracle: Uvilda's "hone counters" (now refine), Hofri's printed "your graveyard" (a card always goes to its
    owner's, CR 400.3), Team Pennant's "token creature" equip text.
- **Set review: Strixhaven Mystical Archive** (1 new name): no changes.
- **Set review: Modern Horizons 2 and Modern Horizons 2 Promos** (255 new names, `ModernHorizons2AuditTest`):
  - Fixed: *Magus of the Bridge* missed your nontoken creatures that died under an opponent's control (it checked
    controller, not "your graveyard").
  - In-game text: *Chatterfang* and *Lonis* read "You may sacrifice any number of" for "Sacrifice X"; *Scion of
    Draco*'s keyword grant is one Oracle line, not five; *Smell Fear* described its fight twice; *Svyelun*'s Oracle
    wording; *Disciple of the Sun*'s prompt.
  - Left as Oracle (no Secret Lair versions): printed "amass 2" (now Amass Zombies), Harmonic Prodigy's "an ability"
    (abilities that trigger are triggered abilities), Subtlety's "top or bottom" (still the owner's choice).
- **Set review: Forgotten Realms Commander** (60 new names, `ForgottenRealmsAuditTest`):
  - Fixed: *Immovable Rod* could target itself (it says "another target permanent").
  - In-game text: *Clay Golem*'s "Roll 1d8" (now "Roll a d8", in the dice-roll cost itself); *Grim Hireling*'s "You
    may sacrifice any number of treasures" for "Sacrifice X Treasures"; *Klauth's Will* ran its "choose both" line into
    "Choose one".
- **Set review: Adventures in the Forgotten Realms and its Promos** (257 new names, `ForgottenRealmsAuditTest`):
  - R&D's Secret Lair: *Delina, Wild Mage* (its only printing says "Roll again.") keeps rolling until it gets 1-14;
    Oracle's "You may roll again." stays optional.
  - In-game text: *Displacer Beast*'s and *Gelatinous Cube*'s ability words after the cost; *Power of Persuasion*'s
    10-19 line was missing; *Clever Conjurer*'s prompt said "another".
  - Left as Oracle: The Book of Vile Darkness's printed "It gains all triggered abilities" (differs only when Vecna is
    copied), Druid Class's missing Landfall label, Earth-Cult Elemental's and Acererak's "sacrifices a permanent"
    (always their choice).
- **Set review: Adventures in the Forgotten Realms Tokens** (1 new name): *Tomb of Annihilation*'s Oubliette and
  Sandfall Cell rooms now read as Oracle ("a creature, an artifact, and a land"; "of their choice").
- **Set review: 2020 Heroes of the Realm** (3 new names): no changes.
- **Set review: Midnight Hunt Commander** (38 new names, `MidnightHuntAuditTest`):
  - Fixed: *Curse of Clinging Webs* watched creatures the enchanted player owns, not controls (a misspelled
    "EnchantedPlayerCtr"); *Tomb Tyrant* could return a noncreature Zombie card.
  - In-game text: *Gorex, the Tombshell* lacked "you may" (a permanent spell's additional-cost line now honors its own
    cost description); *Cleaver Skaab*'s "Zombie".
- **Set review: Innistrad: Midnight Hunt and its Promos** (249 new names, `MidnightHuntAuditTest`):
  - Fixed: *Sunrise Cavalier* could put its counter on any creature (it says "target creature you control").
  - R&D's Secret Lair: *Consider* (before The Lord of the Rings Commander), *Otherworldly Gaze* (before Murders at
    Karlov Manor Commander) and *Larder Zombie* (all its printings) say "Look at the top card(s)" instead of surveil,
    so surveil triggers don't see them (Think Tank precedent).
  - In-game text: *Corpse Cobble*'s "you may sacrifice"; *Homestead Courage* was missing its vigilance; *Slogurk*'s
    "Remove three +1/+1 counters from Slogurk".
  - Left as Oracle: Siphon Insight's printed "look at and play" (you looked at the card before exiling it face down,
    so CR 406.3 already lets you keep looking),
    Florian's "your postcombat main phase" (every postcombat main phase is one).
- **Set review: Crimson Vow Commander** (38 new names): no changes.
- **Set review: Innistrad: Crimson Vow and its Promos** (253 new names, `CrimsonVowNeonDynastyAuditTest`):
  - Fixed: *Boarded Window* exiled itself after 4 life lost, not 4 damage dealt to you.
  - R&D's Secret Lair: *Cruel Witness* (before Foundations Jumpstart) says "look at the top card" instead of
    "surveil 1", so surveil triggers don't see it (Think Tank precedent); *Bloodvial Purveyor*'s printed version
    (no "until end of turn" on its printings, so the buff sticks) is now its own "(As printed)" trigger with a
    printing cutoff.
  - In-game text: *Alchemist's Gambit* was missing "During that turn, damage can't be prevented."; *Alchemist's
    Gambit*, *Lunar Rejection* and *Parasitic Grasp* repeated their last sentence on the cleave line; *Chill of the
    Grave* described its untap clause twice; *Henrika*'s spacing.
- **Set review: 2021 Heroes of the Realm** (4 new names): no changes.
- **Set review: Neon Dynasty Commander** (38 new names): *Kappa Cannoneer*'s Oracle wording ("this creature or another
  artifact").
- **Set review: Kamigawa: Neon Dynasty and its Promos** (267 new names, `CrimsonVowNeonDynastyAuditTest`):
  - Fixed: *Kaito Shizuki* phased out at every end step, not just yours; *Satsuki*'s second mode could return a Saga
    from any graveyard; *Storyweave*'s extra counters went on any player's enchantment creatures.
  - In-game text: *March of Reckless Joy* described its play permission twice; *Shigeki*'s "Return Shigeki";
    *Kappa Tech-Wrecker*'s prompt.
- **Set review: Game Day Promos 2022** (5 new names): no changes.
- **Set review: New Capenna Commander and its Promos** (93 new names): *Vivien's Stampede* didn't show its draw clause;
  *Gavel of the Righteous* read "remove a counter from a this equipment" (fixed in the remove-a-counter cost, which also
  fixes "a this creature" on 12 other cards); *Extravagant Replication*'s prompt; *False Floor* defined its tap
  replacement twice under one name.
- **Set review: Streets of New Capenna and its Promos** (251 new names, `StreetsOfNewCapennaAuditTest`):
  - R&D's Secret Lair: *Sanguine Spy* (its physical printings say "Look at the top card") doesn't surveil (Think Tank
    precedent).
  - In-game text: *Halo Fountain* read "Untap two tapped creaturess" and "Untap 15 tapped creatures" (counts eleven
    through twenty are now spelled out in costs, as Oracle does, and the untap cost lost a stray space); *Rooftop
    Nuisance* described its untap clause twice; *Falco Spara* and *Rigo* said "This creature enters with a shield
    counter" - every legendary card that relied on the default text (24, from *Dark Depths* to *Zack Fair*) now
    uses its Oracle wording and name.
- **Set review: Commander Legends: Battle for Baldur's Gate and its Promos** (344 new names,
  `BattleForBaldursGateAuditTest`):
  - Fixed: *Kenku Artificer* could only target noncreature artifacts you control (Oracle has no such limit).
  - R&D's Secret Lair: *Veteran Soldier*'s printings make its tokens attack "that player" (the one your commander
    attacked) instead of each opponent.
  - In-game text: *Overwhelming Encounter*'s 10-19 and 20 results described +2/+2 instead of their counters;
    *Cone of Cold*'s 20 result, *Jon Irenicus*'s goad clause, *Wyll's Reversal*'s copy and *Gale's Redirection*'s
    wording were incomplete; *Thunderwave*'s 10-19 row; Replicate with a non-mana cost now reads "Replicate—Tap an
    untapped Horror you control." (*Psionic Ritual*); roll tables with hyphens (*Contraband Livestock*, *Journey to the
    Lost City*, *Druid of the Emerald Grove*'s "9 or less" and "20+"); ability words on *Stirge* and *Myrkul's
    Invoker*; *Safana*'s word order; prompts on *Livaan* and *Psionic Ritual*; Veteran Soldier's Oracle "that
    opponent".
  - Left as Oracle: Windshaper Planetar's printed "player or planeswalker" (battle-era errata), Volo's Journal
    gaining the Book type, Black Market Connections' "precombat main phase".
- **Set review: Battle for Baldur's Gate Tokens** (1 new name): *Baldur's Gate Wilderness* (a 20-room paper dungeon)
  isn't in Forge.
- **Set review: Double Masters 2022** (1 new name): no changes.
- **Set review: 30th Anniversary Play Promos** (1 new name): no changes.
- **Set review: Dominaria United Commander** (48 new names): no script changes; *Greensleeves, Maro-Sorcerer*'s
  protections now read as one ability (see Dominaria United). Left as Oracle: *Tiller Engine*'s printed "enters tapped
  and under your control" (same ability).
- **Set review: Dominaria United and its Promos** (243 new names, `DominariaUnitedAuditTest`):
  - Fixed: *Aggressive Sabotage* could only target an opponent (Oracle says "target player").
  - R&D's Secret Lair: *Uurg, Spawn of Turg*'s printings before Edge of Eternities Commander's look at the top card
    instead of surveilling, so surveil triggers don't see them.
  - In-game text: a keyword after Flash and a long-form keyword no longer starts with a comma (*Cleaving Skyrider*'s
    ", flying"); consecutive protections join the way Oracle prints them ("Protection from planeswalkers and from
    Wizards", "from Vampires, from Werewolves, and from Zombies"; also *Baneslayer Angel*, *Kitsune Riftwalker*,
    *Emrakul, the World Anew*, *Balefang the Unslayable*); legends' self-sacrifice costs keep the name ("Sacrifice
    Vohar", *King Darien*, plus the *Nivea*, *Pizza Face*, *Professor Zei*, *Kirk* and *Lander Rizzi* descriptions that
    weren't showing); an empty cost description no longer leaves a double space (*Yotia Declares War*, *Founding of
    Omashu*, *Summon: G.F. Ifrit*); *Sheoldred's Restoration* had no text; *Colossal Growth*'s and *Rundvelt
    Hordemaster*'s second sentences; *Protect the Negotiators* repeated its counter clause; "another Cleric"
    (*Shadow-Rite Priest*); "{1}," (*The Ever-Changing 'Dane*); Domain on *Jodah's Codex* and *Radha's Firebrand*;
    *The Lady of Otaria*'s "You may put".
  - Left as Oracle: *Argivian Phalanx*'s spelled-out affinity, Sagas' printed self-references, *Yotia Declares War*'s
    "becomes a creature", *Najal*'s "when you cast your next".
- **Set review: Dominaria United Jumpstart Front Cards** (10 new names): theme cards, not game pieces; not in Forge.
- **Set review: Warhammer 40,000 Commander** (168 new names, `Warhammer40kUnfinityAuditTest`):
  - R&D's Secret Lair: *Triumph of Saint Katherine*'s printing says "dies", so it also shuffles itself back when it
    dies under another player's control; *Canoptek Wraith*'s printing searches for basics with the same name as any
    land you control (two different names allowed) instead of one chosen land.
  - In-game text: *Tyranid Invasion* had no text; *Sautekh Immortal*'s etbCounter had its description in the condition
    slot (now "Elite Troops — ..."; *Bringer of Green Zenith's Twilight* had a lowercase "no condition" there too);
    *Hierophant Bio-Titan*'s "Frenzied Metabolism — ... you may remove any number of ..." (permanent spells' extra costs
    now show their ability word) and keyword order; "another creature" (*Illuminor Szeras*).
  - Left as Oracle: *Reaver Titan*'s "Void Shields" ability word (a keyword can't carry one), *Bloodthirster*'s
    "After this phase", *Canoptek Wraith*'s and Vehicles' printed self-references.
- **Set review: Unfinity Sticker Sheets** (48 new names): no changes (every sticker checked against Scryfall).
- **Set review: Unfinity Tokens** (1 new name): *Ticket Bucket-Bot* holds ticket counters in paper; Forge keeps them on
  the player, so it isn't in Forge.
- **Set review: Unfinity** (249 new names, `Warhammer40kUnfinityAuditTest`):
  - R&D's Secret Lair: *Captain Rex Nebula*'s printings have Crash Land deal its damage before the Vehicle is sacrificed,
    so that damage can trigger Crash Land again.
  - In-game text: Attractions no longer show a raw "Prize:TrigPrize" line, and five no longer read "Visit — Visit —"
    (*Bumper Cars*, *Costume Shop*, *Ferris Wheel*, *Gallery of Legends*, *Swinging Ship*); *Gift Shop* repeated "that
    hasn't been chosen"; *Trash Bin* said "a creature card" (it already returned a random card); *Grand Marshal
    Macie*'s "If you do" sentence and "Macie" (was "Grand"); "Roxi", "Solaflora" and "Truss" (with "Remove X hack
    counters"); a tap cost's "you control" goes where the description puts it (*T.A.P.P.E.R.*); self-references in the
    current Oracle template ("this creature", "this card", "this Aura", "this enchantment", "this Equipment") on 21
    cards from *A Good Day to Pie* to *_____-o-saurus*; *Goblin Blastronauts*' "enters"; *Haunted House*'s "your next
    end step"; *Foam Weapons Kiosk*'s "That creature"; curly apostrophes on *Fluros* and *Gallery of Legends*.
  - Left as Oracle: *Ferris Wheel* (MTGJSON's "phased out this game" isn't what the card says), *The Superlatorium*,
    *Scavenger Hunt* and *Trivia Contest* naming themselves (most of their Oracle variants do), *Push Your Luck*'s and
    *Squirrel Stack*'s missing "!" (an Oracle typo), *Saw in Half*'s "base", *Storybook Ride*'s and *Myra the
    Magnificent*'s clarifications.

### 2026-10-23, sixteenth deploy (desktop, Android, GitHub) — Secret Lair ponies: Applejack, Fluttershy, Pinkie Pie

- **Secret Lair ponies: Applejack, Fluttershy, Pinkie Pie (SLD 1537-1539)** - the last cards from released sets
  missing besides Alchemy: Reality Fracture.
  - PrintingTraits art objects `smile` and `tail` (Scryfall Tagger trees) and property `KnownArtObject_<object>`;
    untagged art is asked about (ChooseCard `PeopleOnly$`: the AI picks nothing).
  - New effect `Toy`: toys listed once per match as the game begins (`SVar:ToyBox`), one at a time while its token
    stays; the AI owns four random My Little Pony toys.
  - `SVar:PartyIsEveryCreature`: your party is every creature you control and always full, even of one. Full-party
    checks now read `Count$PartyFull` (13 scripts), which equals `Count$Party` without it.
  - Which card uses what: Applejack the `Toy` effect; Fluttershy `KnownArtObject_tail`; Pinkie Pie
    `KnownArtObject_smile` and `SVar:PartyIsEveryCreature`.
  - The 12 existing scripts moved to `Count$PartyFull`: Archpriest of Iona, Coveted Prize, Linvala, Shield of Sea
    Gate, Multiclass Baldric, Nalia de'Arnise, Nimble Trapfinder, Squad Commander, The Destined Black Mage, The
    Destined Thief, The Destined Warrior, The Destined White Mage, Wizard from Beyond (Pinkie Pie is the 13th).
  - `PrintingTraits.txt` regenerated with the two new art objects: 14,604 printing lines gain `smile` and/or
    `tail`.
  - **Tests:** `SecretLairPoniesTest`.

### 2026-10-23, fifteenth deploy (desktop, Android, GitHub) — Mystery Booster 2 playtest complete (parts 6–12); AI and random-deck fixes; Exclude The List setting; 5-commit upstream merge

- **Upstream merge (5 commits).** No conflicts.
  - Upstream brought: a fix for `Highest`/`Lowest` with a double operator and Mod by zero in `AbilityUtils`
    (#12371); a "confine mouse in fullscreen" preference for the mobile UI on a computer (#12383; mobile Settings,
    Adventure settings, `forge-gui-mobile-dev` launcher); Un-card docs list updates, including which version of a
    plane is implemented.

- **AI: mass zone changes wait for Main 2 on the AI's own turn.** `ChangeZoneAllAi` held a battlefield sweep back
  only in Main 1, so an instant-speed one (Perilous Vault) fired in upkeep and exiled creatures that could have
  attacked first. Now everything before Main 2 on its own turn waits (`AILogic$ Main1` and cast-from-effect still go).
- **AI: Master of Winds (and A-Master of Winds) uses its P/T switch.** New GenericChoice `AILogic$ BestPT`: big
  power for an attack nothing can safely block or when unblocked, the form that wins a declared fight, else the
  toughest form or decline. Without a logic the AI declined the "may" every time.
- **Random decks: border payoffs get their borders.** `DeckGeneratorBase.reinforceBorderPayoffs` (after the typal
  pass): a deck with a card whose script asks for a border (`WhiteBordered`, `BorderColorWhite/Silver/Black` - The
  Whiteboarder, Border Guardian) moves its other cards to printings with that border from the full card database
  (the generator pool keeps one print per name), creatures only when the payoff only counts creatures, then trades
  filler for cards that have one until a tenth of the deck (min 4) does. Those names are pinned:
  `Deck.optimiseCardArt(allowedSets, keepPrinting)` leaves them alone, so the coherent-border art pass can't undo it.
- **Setting: Exclude The List** (`FPref.EXCLUDE_THE_LIST`, off by default; desktop Preferences and mobile Settings,
  under Random Deck Generation). Every card on The List (PLST) and The List (Unfinity Foil Edition) (ULST) is also
  printed elsewhere, so this drops printings, not cards: Full card pool draft and sealed packs come from every
  printing but those (`LimitedPoolType.fullCardpoolBooster()`, also used by Full metasets; the List printings used to
  make their cards more likely, too), and generated decks swap any List printing for another printing of the card
  (`DeckgenUtil.dropTheListPrintings`, after the card art pass) with the generator's own picks filtered
  (`DeckGeneratorBase.setPrintingFilter`, `PaperCardPredicates.NOT_ON_THE_LIST`).
  The desktop deck editor's generator (`CDeckgen`) and `DeckProxy` honor it too.
- **Tests:** `PerilousVaultTimingTest`, `MasterOfWindsAiTest`, `BorderPayoffDeckGenTest`, `ExcludeTheListTest`,
  `MysteryBooster2Playtest6Test` through `MysteryBooster2Playtest12Test`. `BorderPayoffDeckGenTest` was fixed in the
  Exclude The List commit: it read borders by edition only, but PrintingTraits knows List reprints of white-bordered
  cards.

- **Mystery Booster 2 playtest cards (MB2), part 12: Runed Terror - Mystery Booster 2 playtest cards complete.**
  - While a permanent with `SVar:SequentialPhases` is on the battlefield, players take their phases one after another:
    as each phase ends the next player in turn order becomes active and takes it, and once all have, the first takes
    the next phase. A turn begins when a player becomes active and ends with their cleanup step; "until end of turn"
    effects end with the turn of whoever was active when they began, and per-turn counts reset only for that player.
    When it leaves, the active player continues their turn as normal. Extra turns wait until it has left.

- **Mystery Booster 2 playtest cards (MB2), part 11: Panglacial Shinobi, There // They're // Their, Blurry Visionary,
  Starting Town NPC, Cleaver Blow.**
  - `LibraryNinjutsu$` (ninjutsu from the library while searching it, shuffling the attacker away) and cost position
    `Shuffle` for `PutCardToLib`. `ActivateWhileSearching$` abilities can now be activated only while their
    controller searches their own library, and the AI uses them.
  - New effect `Sleeve` (the top two cards become one modal double-faced card for the rest of the game, kept on each
    new object). `SVar:HandAdventure` gives creature cards in hand an Adventure, kept while it's cast and on that
    Adventure.
  - `SVar:Multicleave` (pay {N} per bracket set removed as it's cast, people only); `Cleave<i>_` properties and
    `Count$Cleaved` check what's left.

- **Mystery Booster 2 playtest cards (MB2), part 10: Omnipresent Impostor, A Girl and Her Dogs, Alberix, the Trade
  Planet, Don't Worry About It, Plant a Sapling // Fully-Grown Treefolk, Planeswalkerificate, Werewhat, Defender of the
  Queue, Built Bear.**
  - `SVar:HasAllCardNames` (shares every card name, faces included, not tokens') and `SVar:SearchStandIn` (can be
    found in place of any card searched for in its owner's library). The basic supertype on any card allows any number
    in a deck.
  - NameIt `AINames$` (the AI takes the first name it doesn't already control). An Aura can enchant a card in hand
    (`AuraSpell` with `TgtZone$ Hand`).
  - ChangeZone `StaysTransformed$`: a transforming card shuffled in transformed keeps its back face up from zone to zone
    and can be cast as it.
  - Costs `AddToughness<N>` / `SubToughness<N|X>` (loyalty costs on toughness) and static `DamageLowersToughness`.
  - New effect `BecomeBackFace` (a single-faced permanent gets an exiled card as its back face; they separate on
    leaving). Keyword `Positioning` (a left-to-right line kept with state-based actions; property `AdjacentToSource`).
  - `SVar:BuildOptions` / `BuildAIPicks`: options picked once per match as the game begins, printed on the card and
    re-applied after each zone change, raising the mana cost {1} per two points past the free one.
  - New token: legendary 1/1 white Dog (`w_1_1_dog_legendary`, A Girl and Her Dogs).

- **Mystery Booster 2 playtest cards (MB2), part 9: Kozilek, Compleated, Pokey, the Scallywagg, Chatzuk, Mighty
  Guitarist, Luxior, Ignited, Orb of Origin, Boulder Jockey, Keeper of the Crown, All-Star Kicker.**
  - New mana symbols: `{C/P}` (colorless Phyrexian; a gray icon made from the white Phyrexian one, in a spare spot of
    the default mana sprite sheet) and `{L}` (one mana from a legendary source; drawn placeholder icons on desktop
    and mobile, payment rules alongside `{Z}`).
  - Keyword `Annihinfect`. Supertypes Poly, Mono (activated abilities cost an additional {T}) and Continuous. Cost
    `LandDrop<N>` (give up a land drop; only on your turn). `Cost.add` now refreshes whether the cost has {T}.
  - Static modes `CoinFlipAsD20` and `D20AsCoinFlip` (people are asked; the swapped event isn't swapped back).
  - Card property `inBandGE<N>` and `Count$BandSize.<Defined>`. `SVar:AssistKicker:<n>`: another player may help pay
    the kicker part. New effect `PassTheBall` (a teammate gives a permanent or a card in hand to another).
  - New token: Boulder, a 3/3 colorless Construct artifact creature (`c_3_3_a_construct_boulder`, Boulder Jockey).
    MB2 edition adds the creature type Champion and the planeswalker type Luxior; Poly, Mono and Continuous get UI
    labels.

- **Mystery Booster 2 playtest cards (MB2), part 8: Flavor Disaster, Immersturm Battlefield, Vuzzle Spaceship, Fludge,
  Gunk Guardian (and the Gunk card it makes), Glade of the Pump Spells, Pyromancy 101, Zone of Flame, Meandered
  Towershell.**
  - Keywords `Negamorph` (megamorph with a -1/-1 counter) and `Teach` (a resolving spell is exiled taught to a creature
    you control; property `TaughtBySource` while both stay put).
  - `SVar:PayManaCostToPlay` on a land: its mana cost is paid to play it as a land drop (the AI only picks it when it
    can pay).
  - "Enchant zone": an Aura whose `AuraSpell` is the new `EnchantZone` effect picks a zone as it resolves and stays on the
    battlefield unattached; ChangesZoneAll `EnchantedZone$` triggers on cards entering or leaving that zone.
  - MB2 edition: Realm enchantment type, Gunk spell type, Gunk in `[conjured]`.

- **Mystery Booster 2 playtest cards (MB2), part 7: Lich's Duel Mastery, Lifening Elemental, The Many Deeds of
  Belzenlok, Toofer, Keeper of the Full Grip, Yawgmoth's Day Planner, You Compleat Me, Boltfire, Toddler's Rage.**
  - Splice from a permanent card adds its keywords to the spell while it's on the stack (Lifening Elemental's lifelink).
  - New effects `CopyChapter` (copy a Saga card's chapter N ability, wherever the card is) and `MaxLife` (a maximum life
    total for the rest of the game; gains stop there).
  - New trigger `CardAdvantage`: a resolved spell left its controller's net change in cards (hand plus nontoken
    permanents, from just before it was cast) ahead of at least one opponent's.
  - MayPlay `MayPlayLifeLossManaOnly$`: only mana from abilities that make you lose life (paid life, or damage or life
    loss to you in the effect) can pay for it.
  - Keyword `Flashforward` (cast from exile for its cost, then to the bottom of the library). Tantrum's combat rule: a
    blocker with tantrum assigns lethal to what it blocks and the rest to the attacking player (AI, desktop and mobile
    damage dialogs).

- **Mystery Booster 2 playtest cards (MB2), part 6: Catch of the Day, Duelists' Convocation International, Heart of a
  Duelist, Map to Lorthos's Temple, Night of the Flying Merfolk, No-Regrets Egret, Search Elemental, Creepy Crawler.**
  - Static mode `DrawFromAnywhere` (a person picks the position, blind; the AI draws from the top). Keyword
    `Bedtime story` (no lore counter as the Saga enters; one at the beginning of your end step instead of your main
    phase). `SVar:MulliganPeek:<n>` (reveal from hand before a mulligan decision to look at the top n; people only).
  - "Afraid of you" per turn (`Player.isAfraidOf`, property `IsAfraidOfYou`): a Horror, Nightmare, enchantment or
    face-down creature you control entered (every opponent) or attacked that player.
  - New effect `WrittenNumber` (`Mode$ Write`/`Cross`) and `Card.writtenDown`, shown in card detail as "Written down".
    The random 10-digit number never starts with 0.
  - A replacement that copies its card more than once (several `Clone`s as it enters) no longer re-applies to its own
    later copies. MB2 edition adds the Quest enchantment type.

### 2026-10-22, fourteenth deploy (desktop, Android, GitHub) — Unknown Event complete; promos (PH22, PH23, PF24, PUNK, PF25, PF26); Mystery Booster 2 playtest parts 1–5; 16-commit upstream merge

- **Upstream merge (16 commits).** Three conflicts: Defend the Celestus and Infernal Denizen keep upstream's fixes
  (targets you control; `GainControl` `Chooser$`) with the fork's wording, and the mobile MatchScreen keeps both the
  fork's Advanced Tactics grid tab and upstream's compact icon tabs.
  - Upstream brought: a unified phase indicator and compact menu-bar tabs for the mobile Compact layout
    (`VUnifiedPhase`, `FMenuBar`, `FMenuTab`; a fix for it clashing with the open stack, new stack icon); organizing
    Main Deck and Sideboard during desktop drafts (#12046); a refused netplay client no longer reconnects (#12361);
    rules fixes - `VotePlayer$ Other`, taking the initiative on a lost player, RestartGame beginning-of-combat
    commands (#12380), checking and redirecting a spell's targets (#12378), three SwitchBlockEffect defects
    (Sorrow's Path, General Jarkeld; #12375), The Wrath of Khan's delayed trigger (#12369), "Some fixes" (#12382:
    goad AI, counters, GainControl, Infernal Denizen, Lobotomy, Needle Drop, Stardew Valley, ...), creature-target
    fixes (Defend the Celestus, Rally the Monastery, Windborne Charge; #12360); Magik's exile now `ForgetOnMoved`;
    Adventure sideboss maps treated as dungeons (#12358); edition updates (PRM, PSPL, TRK); smaller skin PNGs.

- **Tests:** `UnknownEventBatch30Test` through `UnknownEventBatch58Test` and `UnknownEventScriptsLoadTest`;
  `HeroesOfTheRealm2022Test`; `BlackLotusUnknownPlanechaseTest`, `BlackLotusUnknownPlanechase2Test`,
  `BlackLotusUnknownPlanechase3Test`; `MagicFestPromosTest`; `MysteryBooster2PlaytestTest` and
  `MysteryBooster2Playtest2Test` through `MysteryBooster2Playtest5Test`. Test fixes: Heroes of Kamigawa's test no
  longer depends on what its +2 mills; Processing Plant's test no longer assumes which of the two exiled cards the AI
  returns; `UnknownEventBatch33Test`'s Unstickerify test tolerates a random noncreature beneath the sticker.

- **Mystery Booster 2 playtest cards (MB2), part 5: Friarball, Knight of Lost Causes, Champion of the Hareish,
  Marchesa's Surprise Party, Arcanum Things, Intangible Vibes, Tax Taker.**
  - Keywords `Coststorm` (copies per different mana value among your other spells and lands played this turn) and
    `Buddy list` (new effect `BuddyList`, `Count$OnBuddyList.<Defined>`; the list is per game). `Count$YouAreWayBehind`
    (checked with state-based actions, kept for the turn).
  - Conspiracy subtypes (`[ConspiracyTypes]`, Secret Mission). `SVar:SecretMissions` is chosen secretly as each game
    begins (property `SecretMission_<n>`; the AI picks at random).
  - Static mode `AreTokens`: matching cards are tokens on the battlefield and cease to exist once they leave.
  - New trigger `TaxPaid` (`ValidPayer$`, `ValidTaxer$`, `TriggerCount$Amount`): mana paid for an "unless" cost, for
    attack or block costs (once per declaration) or for a cost increase from another player's permanent or spell.
  - New token: 2/2 white Monk.

- **Mystery Booster 2 playtest cards (MB2), part 4: Chea, Friend to Maybe Too Many, Lutri, Pauper Otter, Avacyn's Collar,
  the Symbol of Her Church, Wormhole Warp, Your Wish Is My Command, Oddric, Lunar Marquis, Brigid, Who's Seen Some
  Stuff, Jeskai Baller, Glimpse, the Unthinkable.**
  - Keywords `Nimble` (can't be blocked by power 3 or greater), `Thoughtweft` (new Continuous `GainsPrintedKeywordsOf$`:
    the printed keywords of the other matching creatures) and `Tantrum` (a keyword for now, its combat rule comes with
    Toddler's Rage). Rebound on a permanent spell exiles it instead of entering.
  - Static mode `CantBeChosen` (`ValidCard$` filters ChooseCard, Clone and CopyPermanent choices; `ValidName$` filters
    card-name choices). Play `ExtraModes$` (one more mode for a modal spell it casts; the AI tops its picks up).
  - Properties `SilverGoldOrangeOrPurpleSymbol` (uncommon or better from Exodus on, by printing) and
    `HasSacSelfForColorless`. New token: 1/1 white Athlete.

- **Mystery Booster 2 playtest cards (MB2), part 3: Madlands, Maestros' Totally Safe Hideout, Snap Judgment, Terry Pin,
  Turboturtle, Essence of Ajani, Rule with an Even Hand, Phyrexian Seedling, Mothers Yamazaki.**
  - Madness on a land plays it (a land play). AttackRestrict `EvenAttackers$` (`GlobalAttackRestrictions.isEvenOnly`;
    the AI drops its weakest attacker from an odd attack). New effect `DecideMatch` (Game/GameOutcome flag the Match
    reads). SpellAbility property `SorcerySpeed`. Keywords `Proliferatelink` (proliferate per damage, with lifelink, before
    lethal damage is checked) and `Partner with itself` (two copies in a Commander deck, both commanders;
    `Deck.getCommanders` lists a commander once per copy).
  - An emblem card (`SVar:EmblemCard`) counts as an emblem in the command zone, where ChangeZone from the stack can now
    put a resolving spell.
  - Maestros' Totally Safe Hideout's land casualty is scripted as an enters replacement (the token copy has it too).
    `CardRules`: a card with `Partner with itself` partners with a copy of itself and is offered as a second
    commander.

- **Mystery Booster 2 playtest cards (MB2), part 2: Naturalize 2, Microscope, Lazotep Archway, Processing Plant,
  Penumbra Umbra, Narod, the Beige Flower, Hish of the Snake Cult, Wowzer, the Aspirational.**
  - Static mode `CombatDamageManaValue` (with Doran's too, the greater of the two); keyword `Daunt`; count
    `Count$YouHaveAll <things>` (energy, monarch, city's blessing, initiative, or a permanent subtype you control).
  - MB2 edition adds the land type Ulamog's.

- **Mystery Booster 2 playtest cards (MB2), part 1: Wrath of Leknif, Spuzzem Strategist, Abbot of the Sacred Meeple,
  Dairy Cow, Rin and Seri, Inseparabler, TL;DR, Subgoyf, Plain Walker, Can't Quite Recall, Muraganda Eldrazi, Sliver of
  Hope, Two by Four.**
  - Keywords `Deworded` (`Card.hasNoAbilities()` is true in every zone), `Forbidden` (a deck-conformance problem when it's
    in the main deck), `Hope` (all damage to the attacking creature is prevented).
  - Property `HasNonKeywordAbility`; count `$NonCreatureSubtypes`; Landwalk:Planeswalker is planeswalkerwalk.
  - Primeval and base power/toughness 4 counters work through lasting effects that follow the counter (`ForgetCounter$`).
  - Abbot of the Sacred Meeple's board-game text and Spuzzem Strategist's line are text only (nothing in a Magic game for
    them to do). MB2 edition adds the creature type Spuzzem.

- **MagicFest 2025 (PF25): Second City, All-You-Can-Eat Buffet, Spaghetti Junction. MagicFest 2026 (PF26): In Residence,
  Bicycle Elemental, Auspicious Aquarium.**
  - Property `SecondSpellThisTurn` (mana restriction), `HasChosenWord` (a whole word in the name, type line or rules
    text; `CardProperty.hasWord`), AI ChooseType logic `MostCommonWordInDeck`.
  - New effect `Introduce` (a typed introduction, refused if used this game; the AI has stock lines;
    `IntroducedSubAbility$`) and per-game player notes (`Player.getGameNote`/`setGameNote`).
  - Auspicious Aquarium's twelve different creatures are differently named (user ruling 2026-10-22); the PF26 edition
    adds the creature type Shrimp.
  - New tokens: Deep Dish Pizza, a Food with "{3}, {T}, Sacrifice: gain 3 life and draw a card" (Second City), and
    Atlantakul, a legendary 20/20 colorless Eldrazi with annihilator 2 (Spaghetti Junction).

- **Black Lotus Unknown Planechase (PUNK): Unknown Event, The Pro Tour, Royal Booster, Year-End Review, Artist Alley.
  Every PUNK card is now in.**
  - OpenBooster `ExileUpTo$` (each player keeps cards of their own pack exiled face down that only they may look at and
    play; `Exactly$`, `WithoutManaCost$`, `IgnoreManaType$`), `BoosterDeck$` (a shuffled pack kept on an effect that
    has the monarch draw from it at their end step, any type of mana spendable on those cards) and `DrawFromUnder$`;
    `Player.drawFromOutsideGame` counts and triggers like a draw. Property `StandardLegal`.
  - A plane may share its name with a nonplane card (Artist Alley, user ruling 2026-10-22): the loader keeps them apart,
    each edition's printing of a shared name goes to the kind most of that edition's cards are, and upstream's
    playtest-clash spelling "P-<name>" in an edition file resolves to the card of that name.
  - Year-End Review's "promoted to commander" is scripted with the existing AlterAttribute `Commander` (each player
    may then put the new commander into the command zone).

- **Black Lotus Unknown Planechase (PUNK): Pin Collector's Booth, Pin Trading, Shrinking Plane, Windmill Farm, New
  Player's Journey, Welcome to Valley, Sorin's Remastered Manor, Multiversal High Council, Stroopwafel Cafe, Mad Labs.**
  - New effect `MatchNumber` (a real-world number a person gives once per match, the AI's random within
    `AIMin$`/`AIMax$`; `Count$MatchNumber.<Key>`), `Count$CurrentYear`. User rulings 2026-10-22: worn pins (AI 0-5),
    years played (AI 1 to this year - 1993), seven folds, Windmill Farm's spin (random permanent, full turn 80%).
  - Printing trait `dnd` (the D&D Secret Lair drops, from MTGJSON's SLD subsets) and `PrintingTraits.councilUniverseOf`
    / `Card.getCouncilUniverse()`: D&D is its own Universe there (Forgotten Realms sets plus those Secret Lairs).
    Properties `UniversesBeyond`, `CorePrinting`, `CreatureTypeListedIn_<SVar>`; count `$CouncilUniverseCount`;
    target restriction `TargetsWithDifferentUniverses$`; Continuous `MayLookAt$ Owner`.
  - New effect `MadLabs` (the three blind choices, then a token copy for each player); `CounterKeywordType.keywordCounterNames()`.
  - Stroopwafel Cafe's top-of-library Food mills itself as its cost: a card in a library can't be tapped or sacrificed.
  - New tokens: Pin, an Aura with enchant creature and +1/+1 (Pin Collector's Booth), and Tulip Petal, an artifact
    with "{T}, Sacrifice: add one mana of any color" (Windmill Farm). The PUNK edition adds the enchantment type Pin;
    `PrintingTraits.txt` marks 77 Secret Lair printings `dnd`.

- **Black Lotus Unknown Planechase (PUNK): High and Dry Black Market, Team-Up!, Probability Flux, Clamilton Estate,
  Event Horizon, The Sphere, Game Knights Live, The Command Zone, High Noon At Thunder Junction.**
  - Charm `ChooseOnResolve$` (each player picks modes as a RepeatEach resolves; the chooser becomes the activator) and
    RepeatEach `StartingWithYou$`; the AI fills an "up to N" pawprint budget with repeated modes.
  - Player property `NeighborsEliminated` (both original-seating neighbors have lost); xMath `RoundUpExtra.N`; Amass
    `TokenScript$`; ResetCommanderTax `CastCount$` (the cast count itself becomes 0).
  - Clash `Clasher$` (a player other than the source's controller), `FirstToSay$` (the clasher's opponents in turn
    order may say it; the first one clashes) and `RememberWinner$`.
  - Phenomena whose text has no "planeswalk away" still planeswalk away after their trigger (CR 704.6f). Event Horizon's
    nonbasic lands become Wastes: named Wastes, basic, only "{T}: Add {C}" (user ruling 2026-10-22).
  - New tokens: 1/1 black Assassin (High Noon At Thunder Junction) and 0/0 red Fan Army (The Sphere). The PUNK edition
    adds the planar types Clamhattan, Foldaria, Foundations and Universia Beyondia and the creature type Fan.

- **2022 Heroes of the Realm (PH22): Treizeci, Sun of Serra; Elusen, the Giving; Heroes of Kamigawa; Wizard from
  Beyond. 2023 Heroes of the Realm (PH23): Ormacar, Relic Wraith. MagicFest 2024 (PF24): Convention Maro.**
  - Printing trait `retroframe` (Scryfall's 1993 and 1997 frames; `Card.hasRetroFrame()`) and property `Nostalgic`:
    legendary, artifact, or a retro-frame printing (user ruling 2026-10-22).
  - Donating: a control change made by the old controller's own spell or ability (GainControl, or the exchanger's side
    of an exchange) carries a donor (`AbilityKey.Donor`, `TriggeredDonor`); ChangesController `Donated$ True`.
  - OpenBooster `ToBoosterPile$` (`Sets$` limits the sets the player picks from, `UpToOne$`); property
    `NamePrintedIn <codes>` (the name is in any of those sets' card lists).
  - Keywords `Precious` (the other commander may be a legendary noncreature artifact) and `Create a Character` (any
    nonlegendary creature can choose the Background, and becomes legendary like a Legendary partner's other one).
  - "As you create your deck, choose an [A] and a [B]": SVars `DeckChoiceA`/`DeckChoiceB`, asked once per match as the
    game begins (user ruling 2026-10-22), property `DeckChoice_<letter><n>`; property `Landscape` (split cards and
    Rooms but not aftermath, battles) and count `$ArtistsAmong`.
  - `PrintingTraits.txt` regenerated with the new trait: `retroframe` is on 14,300 printing lines. New tokens: three
    green \*/\* Elementals named Maro for Convention Maro's deck choice (power and toughness equal to cards in your
    hand, card types in your graveyard, or artists among your nonland, nontoken permanents). The PH22 edition adds the
    planeswalker type Wanderer.

- **Mobile Settings: one "Alternate Player Zone Layout" again.** The fork's zone-tabs-on-the-left option
  (`UI_ZONE_TABS_LEFT`) is gone; upstream restored its own setting of that name (`UI_ALT_PLAYERZONETABS`), and the two
  showed with the same label. Hand, Library, Graveyard and Exile tabs are on the right edge, as upstream has them.

- **Unknown Event: The Flavormancer (UNK mythic). Every Unknown Event card is now in.**
  - As the game begins, with it as your commander, a person marks their own legendary creature and planeswalker cards
    as Heroes or Villains (honor system), and the AI goes by the Flavormancer's lists: 225 hero and 123 villain card
    names from the MTG wiki (and Wizards' Reality Fracture story), reviewed by the user. They perpetually gain the
    creature type; planeswalkers may now have the creature types Hero and Villain.

- **Unknown Event: Command From the Shadows, Advanced Tactics (UNK rares).**
  - Secret commanders (SVar `SecretCommanders` on a conspiracy): every commander starts the game hidden from everyone
    but its controller in the command zone, until it's cast the first time; it's cast normally.
  - Advanced Tactics' 5x5 grid (new effect `TacticsGrid`, Card/CardView `TacticsGrid`): filled at random as it enters,
    each entering creature placed by its controller, `{1}` moves; CantBlockBy `GridAdjacency$` keeps blocks to
    orthogonal neighbors. The grid has its own view: a "Grid" tab next to Stack, Combat and Log on desktop (doc
    `REPORT_GRID`, only there while a grid exists), a "Grid" tab in the menu bar on mobile (new game event
    `GameEventTacticsGrid`, `IGuiGame.updateTacticsGrid`).
  - The first version showed the desktop grid in a separate popup window (`TacticsGridWindow`); a follow-up commit
    made it the Grid tab (`CTacticsGrid`, `views/VTacticsGrid`, `EDocID.REPORT_GRID`).

- **Unknown Event: Occupation of Kulrath, Occupation of Llanowar, Sort of _____ and _____, The Whiteboarder, The
  Chaos Keeper, The Charitable Drafter, The Rhystic Storyteller, The Game-Changing Host, Clear, Fair Magic, The
  Commander Recommender (UNK rares).**
  - Battle type `Control Point` (its controller protects it). Damage to a battle removes its defense counters as the
    damage source's controller, and CounterRemovedOnce triggers take `ValidPlayer$` and give `TriggeredPlayer`.
  - New effect `SortOfSword`: a trigger made of the first half of one random sword and the second half of another.
  - New effect `Whiteboard` (`Mode$ Note` / `Create`): notes kept on the card, and a white-bordered token built from
    them (SVar `WhiteBorderedToken` makes a card white-bordered).
  - OpenBooster `PutUnder$` / `TakeFromUnder$` (a pack kept under a card) and `Draft$` (a pack drafted in turn order).
  - Card property `flavorWords<op><n>` (words of the printing's flavor text); SVar `CommanderUnsleeved`.
  - New effect `Retroactive`: this turn's resolved spells and abilities (`Game.getResolvedThisTurn`) apply to the host
    where they would have - "all" effects it fits and targeted ones it could have been a target of.
  - New effect `DestroyAnything`: destroys permanents and command-zone objects, or a designation picked from a list.
  - New trigger `GuessedWrong` and effect `GuessedWrong`: every guess effect reports a wrong guess, and Liar's Pendulum
    and Master of Predicaments now do too. New effect `RecommendDeck` (saved Commander decks or a random precon).
  - `FModel` gives `RecommendDeck` the saved Commander decks from every deck subfolder, or
    `DeckgenUtil.getRandomCommanderPreconDeck()`. The battle type Control Point is added to `TypeLists.txt`.

- **Unknown Event: Blue Screen of Death, The Snapstone Wielder, Welcome to Australia, The Sprinkler of Stardust, The
  Fact Checker, The Convention Enthusiast, The Policy Maker, The Beleaguered Boxer, The Multiplayer Nitpickers (UNK
  rares).**
  - `Count$ActivePrioritySeconds`: real seconds the active player has held priority this turn.
  - SVars `NoLandsCommanderEffect` / `NoLandsHandSize`: for a commander whose deck has no land cards, as the game
    begins. Play `AnySupportedCard$ Nonland` and `RandomTargets$` (targets picked at random, as High Troller's).
  - New effect `ReverseLibraries` (each library's order reversed).
  - New static `ChooseGuessOutcome`: guessing a card (name, artist, Guess Who, trivia, invaders, the Actualizer's
    lie) is just choosing how it comes out. New effect `FactCheck`.
  - New effect `ProposeRule`: a random rule from real cards that affect every player (the Policy Maker's 15, each with
    its source card), applied if every player agrees.
  - OpenBooster `Swap$ N` and `Match.swapInDeck`: cards leave the deck for the match and the saved deck file.
  - New static `PhantomPlayer`: player and opponent counts are one higher, and "for each player/opponent" repeats
    (RepeatEach, CopyPermanent `ForEach$`) run once more for a phantom who does nothing.

- **Unknown Event: Need for Speed (Not the Odyssey One), Blu, Mansion Prince, Slobad, Actually Just Fine, The Octopus
  Official, Syx, Robot Overlord, The Actualizer, The Good Time Sleuth, The Meld Hound, X, Mystery Racer (UNK
  rares).**
  - Need for Speed's super haste goes to creature cards in your hand, where it does something.
  - Rooms can have a sealed door that can never be unlocked (`UnlockDoor Mode$ OnlyOne`: unlock the chosen door,
    seal the other). CopyPermanent `ValidSupportedCopy$ Card.Room` picks among Rooms only.
  - New static `SacrificeInterchangeably`: a sacrifice cost or effect asking for a creature takes an artifact too,
    and the other way around.
  - New effect `Tournament` (`Mode$ Announce` / `Advance`): a player's top-8 bracket of exiled piles.
  - Syx: SVar `AsksInvaders` (names written down as the game begins, if it's your commander, and logged after the
    game), card property `Invader`, new effect `GuessInvader`.
  - New effect `Actualize`: read out a hand card with one lie (another real card's name, cost or type line, or a
    different number); opponents guess at once.
  - New effect `MeldIntoSpell`: the card melds with a creature spell (combined like Glurg) as a merged card, and a
    spell's merged cards now go with it when it leaves the stack. The creature type `Human?` is added.

- **Unknown Event: Chicago Loop, ______, The Playful Winners, Huge Truck, Split into Three Parts (UNK).**
  - Orange is a color (`MagicColor.ORANGE`, the color byte's top bit; `ColorSet` masks with `0xFF`), with the
    other Un-colors. New tokens: a 2/2 orange Bear with haste, a 3/1 green Dinosaur Skeleton with trample.
    Protection from orange works; orange shows the colorless mana icon and watermark; new UI label "Orange".
  - New cost `PaySpeed<X>`: the payer's speed goes down by X, to as low as 0.
  - Written on the card in the deck, like a Legacy word: `PaperCard` flags `name` (an all-underscore name becomes
    it; new effect `WriteName`, card property `writtenNameAllows`) and `tally` (`Count$TallyMarks`; the winner's
    cards with SVar `TallyOnWin`, and their commanders, get one when the match records a win).
  - Clone `CopyRandomPartner$`: a random Vintage-legal card with any partner variant.
  - New effect `ShareBackup`: the abilities the triggering backup ability would give, kept for good.
  - Token `TokenZone$ Exile`: made in exile, where it stays (no 704.5d), with its `WithCounters`.

- **Unknown Event: Noyd, Keeper of the Void, Sharp Eraser, Banned Eldraine Card, Rafi, Retro Racer, The Egotistical
  Velociraptor, The Ancient Dingus, Fifth Stage of Magic Design (UNK rares).**
  - Real-time timers: new effect `StartTimer` (`Minutes$`), checked when a player would receive priority; the host's
    `TimerExpires` SVar is what happens when it runs out. Players also keep how long they held priority this turn.
  - New effect `EraseAbilities`: whole keywords and abilities, gone for the rest of the game in every zone. The AI
    erases only what its creature evaluator counts against a creature (defender, can't block, upkeep costs, ...).
  - CastAs `RaiseCost$` (more on top of the copied card's cost), `Perpetual$` (that card for the rest of the game),
    and a land cast as a spell that puts itself onto the battlefield.
  - New effect `AiTakeover`: an AI plays for its player until the end of their next turn (the GUI supplies the AI).
    `FModel` creates it as an AI player named "Grumpy Co-player".
  - ReplaceToken `Type$ Instead` (none created; the count is the host's `ReplacedTokens`). RandomCards `Defined$`,
    and any Magic card without `LegalIn$`.
  - Rafi uses the existing `ManaBurn` and `CombatDamageUsesStack` statics.

- **Boosters: no crash when a foil slot has nothing to make a foil of.** `BoosterGenerator.generateFoilCard` took
  the first card of an empty sheet; that pack now just has no foil (seen in the Summon the Pack test's random packs).

- **Unknown Event: Shahrazad and Sindbad, The Trivia Mastermind, The Cosplayzer, Yeah Nah // Nah Yeah (UNK
  rares).**
  - The match counts its subgames (`Count$SubgamesThisMatch`).
  - New effect `TriviaGuess`: the controller names the card and picks power, toughness, or mana value (only mana
    value for a card without power and toughness); an opponent guesses it, and a wrong guess puts the card into its
    controller's hand. The AI guesses from the name alone, 0 to 6.
  - Copy `FirstLetter$ <letter>`. Trigger `SharedLimit$ <key>`: once each turn among a card's triggers with that key.
    Ability property `NotOnceCopier` (not an ability with `OnceCopier$`).
  - New effect `ResolveNow`: the target spell resolves as soon as this is done resolving, before anyone gets priority
    (`MagicStack.resolveRightAfter`) - not in the middle of it.

- **Unknown Event: Interrogation Robot, The Zonian Brawler, The Karst, Enchanted (UNK cards).**
  - Ability property `LastNamedSpell`: the half of a split card named last. The AI's card-name choice now picks one
    of the offered halves of a split card with more than two halves.
  - DeckRule Size `SetMin$ <n>`: the commander's deck's minimum size, the commander included. SVar
    `StartingLifeSet:<n>`: a commander that sets its player's starting life total.
  - DamageDone trigger `DamageAmount$ EQSourcePower` (fighting and biting) and `Lethal$ True` (the damage left the
    creature with lethal damage marked).
  - Library search `DifferentPT$` (no shared power or toughness; a card without them shares neither) and
    `DifferentCardTypes$`.

- **Unknown Event: Dockbreacher, New Magic Game Plus, Compleated Clone Shell, Enolc, Perfect Clone (UNK cards).**
  - ReplaceToken `Type$ FirstPerTurn`: only the first such token a player creates each turn; the replacement's
    controller draws a card for each other one.
  - RestartGame `KeepOnBattlefield$ <valid>`: those permanents stay on the battlefield through the restart.
  - Dig `AtRandom$ True`: the cards are taken at random (from an unordered zone such as the sideboard).
  - SVar `CopiesOtherCommander`: one of two commanders is a copy of the other in every zone, keeping its name.
    CastAs `CastAs$ YourCommander`, and a card's own CastAs static works from the zone it's cast from.

- **Unknown Event: Sue, Everlasting Dinosaur, Awoken Nephilim, Seventh Stage of Magic Design, Saga of Krark Losing
  His Thumb, Niv-Mizzet's Diary, Simic, Value Engine (UNK rares).**
  - New keyword Fossilize (`K:Fossilize:<cost>` or `ManaCostMinus<N>`): from the graveyard, any time, it returns with
    a finality counter as an artifact and not a creature.
  - SVar `BecomesCopyOf:<names>`: one is named as the game begins (kept for the match) and the card is a copy of it
    in every zone, also legendary. The AI takes one whose color identity holds its deck.
  - ChangeZone `Imprint$ Source`: from an effect, imprinted on the card that made it. Play `NoExtraTurns$ True`:
    the copy's extra-turn effects do nothing.
  - Players keep the lands that entered under their control and the cards they drew during their own last turn;
    `Count$FewestOppOwnLastTurn.Lands|Draws`.
  - Flip Ahead is a lore-counter replacement that, on a lost flip, puts no counter and triggers the current chapter
    again.

- **Unknown Event: Maeve, Wearer of Many Hats, Halana and Alena and Gisa and Geralf, The Vast Scrier, The
  Salubrious Philosopher, Falko, Showoff Pilot, The Traveling Postman (UNK rares).**
  - SVar `AnyNumberOfRoles`: the Role state-based action leaves that card's Roles alone. A Role created attached
    isn't "attached" (CR 701.3a, user ruling), so Maeve draws only when a Role is moved onto it.
  - ChangeZone/combat `OwnAttackTriggers$ True` (`TriggerHandler.runOwnTriggers`): a creature put onto the
    battlefield attacking has its own "whenever this creature attacks" triggers trigger, and no one else's.
  - Card properties `addsMana` and `removalSpell` (instants and sorceries that can damage, destroy, or exile a
    permanent); ChooseGeneric AI `Philosopher`.
  - `Spectacle:ManaCostMinus<N>`: the card's mana cost minus that much generic mana. Ability property `Spectacle`.
  - Token `TokenScript$ FromRandomCard`: random cards until one names a token script, then one of its tokens.

- **Unknown Event: Sixth Stage of Magic Design (UNK white rare).**
  - Card property `blackOrSilverSymbol` (`CardPrintings.hasBlackOrSilverSymbol`, user ruling): from Exodus on,
    commons and basic lands are black and uncommons silver, except Dominaria's and Coldsnap's white commons; every
    set before Exodus is black, except Alpha through Fifth Edition (and their variants), which have no symbol. Tokens
    count as black.

- **Unknown Event: The Keeper of Kaldra, The Good Gamers, Kallist Rhoka (UNK rares).**
  - MakeCard `Companion$ True` (each card made in the command zone is its owner's companion) and `OncePerGame$ True`
    (each player's first resolution this game).
  - SVar `DamageIsCommanderDamage`: its damage to an opponent counts as commander damage from its controller's
    (first) commander, combat or not.
  - Clone `Swap$ True`: the card copied also becomes a copy of the clone as it was before. A spell cloned on the
    stack stays the copy as it resolves.

- **Unknown Event: The Kami Knight, Bobby, Another Dwarf, Fioran Reformist, Azorius Traffic Enforcement, The
  Tokenator, Third Stage of Magic Design, Naming Screen, Hosting Season, The Miniaturizer (UNK white rares).**
  - New static `CantBeGoaded` (`ValidCard$`), read by `Card.addGoad`.
  - DisableTriggers `KeywordTrigger$ <keyword>`: only that keyword's triggers ("backup abilities").
  - BecomeMonarch `Abolish$ True`: no one is the monarch. Player property `withMostCreatures` (more than each
    other player). Democracy is its two parts on the card, so two Reformists draw twice (my call).
  - Card properties `rulesTextOrTypeHasChosenWord`, `doesNotShareNameWith OtherYourCreatures`. AI ChooseType logics
    `MostWordMatches` (the word on the most of its nonland permanents and graveyard cards) and `UniqueName`.
  - Animate `Name$ ChosenType`. TokenColors `ChosenColors` (every chosen color). AI ChooseNumber `AILogic$ Half`.
  - `Count$DateIs.<yyyy-mm-dd>,...` (the device's date).
  - Copy `RemoveCreatureType$ True` (keeps its other card types); `ThisManaCost` in an added keyword is the copy's
    mana cost. Token script `c_2_2_nameless` (its name is taken away as it's made).
  - Fix: Second Stage of Magic Design missed ability words after an Oracle text's first line (now
    `Card.hasKeywordOrAbilityWord`).

- **Unknown Event: Second Stage of Magic Design, Untap, Upkeep, Draw, The Countering Runner, The Fearsome Flock,
  Lorthos, Tentacled Terror, The Fish Brewer, Fourth Stage of Magic Design (UNK blue rares).**
  - Card properties `unsharedKeywordOrAbilityWord` (keyword abilities a card has plus ability words it's printed
    with; enchant left out, as for The Horizon Seeker) and `wouldCounterASpell`.
  - Psychic damage (`DealDamage | Psychic$ True`): no loss of life; a player dealt 20 of it loses the game.
    It's logged, and kept in game snapshots.
  - Fix: `TriggerHandler` runs waiting triggers from a copy of the list, since The Fish Brewer's taps, made while
    counting, can add waiting triggers of their own.
  - `K:Level up:<cost>:SplitSecond`: the level up ability can't be responded to.
  - New static `ActivateAsController` (`ValidCard$`, `Activator$`): activate abilities of others' permanents. Mana
    abilities of them aren't offered while paying costs.
  - Panharmonicon `TapForEach$`: tap any number of those as the trigger event happens, one more trigger each.
  - Mono Eminence starting hand size (SVar `StartingHandSizeIfMono:<color>:<size>`), also the maximum hand size.
  - CastAs `SameManaCost$` (any nonland card with that mana cost), `OncePerTurn$`, `NoRepeat$`. A person picks from
    the list of those cards, as for Imaginary Friends.
  - AddPhaseAi `AILogic$ ExtraStep` (extra draw step; extra untap step with two or more things tapped). Entwined, the
    extra steps come draw, upkeep, untap (CR 500.8). Token script `all_1_1_fish`.

- **Unknown Event: Memnarchitect, Triple Threat, The Knight of Land Drops, The Wheeling Runner, Blustering Barnyard,
  Mana Max, Afterburner (UNK rares).**
  - Card properties `notOriginallyArtifactCreature` and `StartingDeck`.
  - SVar `TripleCommanderDamage`: commander damage to an opponent of its controller counts triple, per copy.
  - `CardRules.partnersWithKnights`: "Partner with Knight" pairs with any legendary Knight.
  - New effect `RandomCards` (`Amount$`, `LegalIn$`): random cards legal in a format, put into your hand.
  - New trigger `ManaLost`: a player's mana pool empties with mana still in it.
  - Land type Barnyard. Blustering Barnyard's storm makes token copies of the land it is (my call). Token scripts
    `w_2_2_horse_with_horsemanship`, `g_3_3_ox_trample`.

- **Unknown Event: The Colossal Dreadmaw, Just a Li'l Guy, Oozeavite, Retto, Family Racer, Your Favorite Character,
  Playtest Wish, Fear of the Unknown, The Companion of the Wilds (UNK rares).**
  - New effect `LockPT` (`Card.lockPT`): the creature is that power and toughness until end of turn, whatever else
    applies.
  - SVar `AsksFamily`: five creature names written down as each game begins (a person picks from card-name search,
    the AI its five biggest creatures; my call), property `InFamily`.
  - New effect `FavoriteCharacter`: a Magic character's name, up to three colors, two creature types (replacing its
    own) and two keywords, written onto the card in every zone.
  - SVar `OldCompanion`: as your companion it's cast from the command zone instead of being put into your hand for
    {3}.
  - Oozeavite's counters that are also +1/+1 counters are kept as a keyword counter and a +1/+1 counter, moved
    together (my call). Playtest Wish takes the playtest card from outside the game as it resolves and lets you play
    it from exile this turn (my call). New token script `g_0_0_ooze`.

- **Unknown Event: The Multifaceted Phyrexian, Incubation Triformer, The Underworld Fantasy Queen, Bin Chicken, Dan,
  Shrewd Trader, Spice Rack, Mister Cheddar, Cheese Sliver, Surprise, Nerd!, The Gunky Runner (UNK rares).**
  - SVar `BargainAnyPermanent`: its controller's bargain costs can sacrifice any permanent.
  - SVar `MaxOneCommander` + a chosen player: a state-based action leaves that player one commander in their command
    zone, the rest go to their graveyard (and aren't offered straight back by 903.9a).
  - Mister Cheddar as your commander: Sliver cards in the deck may be in it any number of times (DeckFormat).
  - SVar `InvertTemporaryPumps`: a Pump or PumpAll's positive until-end-of-turn power or toughness becomes negative
    (`PumpEffect.surprise`).
  - Bin Chicken: Food tokens you control are also Junk and Junk tokens also Food (my call for "treat as though").

- **Unknown Event: Barce, Friend Finder, The Collector (UNK mythics).**
  - `CardRules.findsFriends`: Barce and another commander-eligible card of one color or less may be commanders
    together as though each had partner (user ruling: only cards that could already be a commander).
  - New effect `Signature`: who signed a physical card, asked of its owner the first time it matters and kept per copy
    for the match; the AI's cards are unsigned (user ruling). Property `notKnownUnsigned`. `Mode$ DestroyByName`
    destroys a creature whose name is within five letters' length of the discarded card's signer. A played signed
    land is copied as a token (my call).

- **Unknown Event: The Horizon Seeker, The Convincing General, The Faction Dragon, Polis the Planeshifter (UNK
  mythics); fix Red and Black Legac.**
  - `Count$DifferentNamedKeywords`: keyword abilities, ability words and keyword actions named in the rules text of
    permanents you control and instant and sorcery cards in your graveyard, or had by those permanents; reminder text
    doesn't count, nor do enchant and the everyday actions (user ruling). Keyword actions come from a new
    `[KeywordActions]` section in `res/lists/LegacyWords.txt` (CR 701, effective 2026-09-25).
  - SVar `AtGameStartIfCommander`: an ability run as the game begins if the card is its owner's commander.
  - SVar `AsksFaction`: as the game begins, a faction whose colors the deck fits; the card's mana cost becomes its
    symbols and its in-game color identity its colors. Factions are the watermarks with set colors, checked against
    Scryfall: the guilds, Khans clans, dragonlord clans, colleges and families (my call; `GameAction.FACTIONS`).
    Property `hasChosenWatermark`.
  - New effect `ChooseRandomPlane` and continuous static `GainsPlaneAbilities$`: the imprinted plane's abilities,
    working from the battlefield; its chaos abilities ignore the planar die (trigger param `OnlyWhenAffected`).
    ChaosEnsues `AnyGame$` works outside Planechase.
  - Red and Black Legac's coin flip named the wrong ApiType (`FlipACoin`), so the card couldn't be built. New
    `UnknownEventScriptsLoadTest` builds every Unknown Event card so a bad script can't hide.

- **Unknown Event: Minnea, Planar Tourist, The Generational Historian, The Strixhaven-Lorwyn Rover, The Waffle
  Restaurant (UNK mythics).**
  - ChooseType `Type$ Plane` (every plane a printing's set is on; the AI takes the plane most of its spells are from).
    Card properties `fromChosenPlane` and `planeUnclear`; ReduceCost `HonorAsk$ <question>` asks the caster as the
    cost is really paid (never the AI), for a printing whose set is on several planes or none (user ruling).
  - SVar `AsksReleaseYear`: as the game begins, if it's your commander, the year every printing in the deck fits, or
    any year if they don't share one (user ruling); the AI takes its most common year. Property `releasedInChosenYear`.
  - SVar `AsksCollege` and DeckRule `ColorIdentityOneOf$`: the deck is legal if one of the listed color identities
    covers it (user ruling); the college (one that fits) and a Lorwyn type are chosen as the game begins, and the
    commander's color identity in game is the college's. Property `allChosenColors`.
  - DeckRule restriction word `CreatesFood`: a card whose rules text creates a Food (my call, for The Waffle
    Restaurant's "cards ... that can create Food tokens").

- **Unknown Event: Taught by Narset, Serra, Vito, Surrak and Bruce Tarl (UNK commander enchantments).**
  - SVar `CommanderEnchantment`: the Aura stays attached to its commander as it moves between face-up zones
    (`GameAction.followCommander`, run after every zone change). It waits where it is while the commander is a spell,
    comes along to the battlefield, graveyard, exile, hand and command zone, and falls off if the commander goes to a
    library or face down. Cast on a commander in your command zone, it goes there with it (SVar `AuraSpell`).

- **Unknown Event: Unstickerify sees under stickers; Mysterious Hero, Ring Out, Adventurer Beguiler, Takesies //
  Backsies, You're Toast!, Trash Panda (UNK).**
  - Unstickerify: a stickered playtest card becomes a copy of the real card under its sticker while enchanted (Clone
    `CopyFromChosenName$`, `Duration$ UntilUnattached`). New effect `StickerBeneath`: a person names that card from
    card-name search, or says it's unclear (a Wastes); the AI's stickers each hide a random real card (user ruling).
    Kept per copy for the match.
  - New effect `SecretIdentity` + SVar `AsksSecretIdentity:<ability>`: before the game begins a sleeved deck may cover
    Mysterious Hero with another creature card from the deck, which leaves the deck; it's that creature in every zone
    (`PerpetualClone`) until the granted special action removes it (user ruling). AI covers with its highest mana
    value creature, and removes the cover when the hero is the better creature.
  - PlayEffect `ForcedTarget$`: the cast spell may target only that (`SpellAbility.setForcedTargets`).
  - New effect `TakeAdventure` + card property `OnAdventure`: no one else may cast the card from exile any more.
  - New effects `MoveCounterFromEach` (Takesies) and `TreatCountersAs` (Backsies: until end of turn every counter
    reads as one of that type, `Game.getCountersTreatedAs` in `GameEntity.getCounters`).
  - New effect `ManifestBread`: manifest dread, but the face-down card is a Food artifact that can't be turned face up.
  - New effect `PutInZoneOf` (into another player's zone; the card stays its owner's) and player property
    `OpponentDredge<N>` (Trash Panda's opponent dredge, decided by the card's owner).

- **Unknown Event: Your Favorite Missing Character (UNK).**
  - SVar `AsksMissingCharacter:<ability SVars>`: as the game begins (user ruling) its owner picks two colors, up to
    three creature types, a new name and one of the listed abilities, kept for the match and written onto the card
    in every zone (`Card.writeMissingCharacter`); a `MissingStatic*` SVar is a static ability, the others triggers.
    The AI picks its deck's two main colors and creature types, a made-up name, and the ability its deck uses most.

- **Unknown Event: The Pleasant Taxer (UNK).**
  - New static mode `ActionTax` (`Action$ Search|Draw`, `ValidPlayer$`, `Amount$`): a library search, or a draw beyond
    the player's first each turn, is paid for each time or it doesn't happen (user ruling). The AI pays when it can.
  - ChooseGenericAi `AILogic$ PleasantTaxer`: taxes casting whichever kind of spell its own deck has fewer of.

- **Unknown Event: Guess Who it Is!, Unstickerify, Higher Level Zone Monster (UNK uncommons).**
  - New effect `GuessWho`: the guesser asks five questions from a menu of the card's characteristics (colors, types,
    mana value, power, toughness, keywords), answered truthfully, then names a card from card-name search; the AI
    asks whichever question best splits the creatures in its library still possible and names the likeliest (user
    ruling).
  - Unstickerify: Forge can't see under a sticker, so a stickered (playtest) card is always the unclear case and
    becomes a Wastes.
  - Clone `AddColors$ ChosenColor`: the host's chosen colors.

- **Unknown Event: Windy City Aven, Windy City Elemental, Identify the Culprit, Hero's Uncle, Red and Black Legac,
  17-Year Cicadas, Fear of Forgetting Names (UNK uncommons).**
  - SVar `AsksMagicCon:<city>`: as the match begins a person says whether they attended (honor system); the AI
    attended at random, once per match (user ruling). `Count$AttendedMagicCon.<city>`. The Windy City's blessing is
    an effect named Windy City's Blessing, gained like Ascend's.
  - Identify the Culprit: the AI caster picks the creature it most wants exiled, an AI guesser guesses at random (user
    ruling).
  - Hero's Uncle's legendary team-up creatures are Scryfall Tagger's `legendary-team-up` oracle tag (user ruling), the
    26 tagged cards listed in its script.
  - `Count$CastManaSpentColor.<color>`; Animate `Name$ Nameless` (it loses its name).
  - New token script `w_1_1_insect_flying`.

- **Unknown Event: Tax Draw, Tax Sweeper, Tax Hounds, Clear, the Mind, Gather, the Townsfolk, Devoted Jeskai, Devoted
  Mardu, Tricky Mage, Team Mage, Dialogue Tree, Good Knight, Rosewater's Nemesis, Echoing Echo, Out of Town, Brave
  Falconhawk, Rideable Mantis, Xerex Squire, Farseeing Flockmate (UNK uncommons).**
  - `Count$HypotenuseOf <defined>` (rounded up) and `Count$SaddledByThisTurn <valid>` (the saddlers as they were).
  - Devoted Mardu's Mobilize is the keyword as the rules define it (whenever this creature attacks), not its
    reminder's "Whenever you attack".
  - Clear, the Mind and Gather, the Townsfolk are in `*_unknown_event.txt`.

- **Unknown Event: Team Acceleration, Tax Keeper, Devoted Sultai, Arcbound Mamba, You and Ooze Army?, Rampant, Growth,
  Artist Alley, Mana Conference, Urza's Hot Dog Stand, Reflective Gate, Must Be Knights, Unclaimed Battle Axe, Devoted
  Temur, CGI Fight Sequence, Nevermind, Tax Bolt, Furnace Oriflamme, Across the Multiverse, Lava, Axe (UNK
  uncommons).**
  - `Count$HighestCommanderTax`, `Count$CardMarkedColors`, `Count$CreatureTypesAmong <valid>` (changelings add
    nothing).
  - Card property `printingFrom_<SET>[_<SET>...]`: the card's own printing, unlike `set<SET>` (its original one).
  - Defined player `ParentTargetDefendingPlayer`: the player the parent's targeted attacker is attacking.
  - `ChooseType | Ballot$ True`: every chooser votes secretly, then the votes are revealed together; Animate
    `Types$ ChosenTypes` gains each type voted for.
  - SVar `AsksSignedColors`: as the match begins, a person says which colors are signed in each copy's art box (honor
    system, like Mark Ritual); the AI's copies are unsigned. Read back by `Produced$ Combo ColorID`.
  - CountersPutAi `AILogic$ PoisonModular` (Arcbound Mamba).
  - Rampant, Growth and Lava, Axe are in `*_unknown_event.txt`: their file names collide with Rampant Growth's and
    Lava Axe's.

### 2026-10-21, thirteenth deploy (desktop, Android, GitHub) — Unknown Event
  rares and uncommons, through Windmill Slam (178+ of 398 UNK names); no upstream changes to merge

- **Unknown Event: Devoted Abzan, Bad Knight, Team Perseverance, Impressive Rat, Vamping Vampire, Fear of Fear
  Itself, Soul Drainer, Would You Have Done the Same?, Sheoldred's Terror, Ransack, the Lab, Magic Designer, Windmill
  Slam (UNK uncommons).**
  - `Count$DevotionTriple.<c1>.<c2>.<c3>`.
  - A permanent with SVar `ScoresAtGameEnd:<team>` on the battlefield as the game ends scores that team a point on
    the match scoreboard.
  - New effect `ResetCommanderDamage` (`DamagedPlayer$`).
  - New creature type Windmill.
  - New token: Windmill Slam's 4/4 artifact creature Windmill with defender.
  - Ransack, the Lab is in `ransack_the_lab_unknown_event.txt`: its file name collides with Ransack the Lab's.
- **Unknown Event: Groaaaaag, Hungry Monster, Monet, Sensei of the Sewers, The Mystery Raceway, Crow Scarer (UNK).**
  - `K:Ninjutsu:<cost>:FixedCommander` and `K:Suspend:N:<cost>:Commander`: from the command zone they pay the
    commander tax and count toward it, like a cast (user ruling).
  - Abilities take `CommanderTaxApplies$`.
  - The Mystery Raceway's "Team X won the race" is your own race team (user ruling).
  - Crow Scarer's Cowards can't attack you or your planeswalkers and battles, and can't block your creatures (user
    ruling).
  - Commander Suspend gets its own label ("Commander Suspend N"), and is offered from the command zone as well as
    the hand.
  - New tokens: a 1/1 black flying Bird (Crow Scarer) and a 1/1 green Turtle with training (Monet).
- **Unknown Event: Bag of Stroopwafels, Famous Museum, Guilded Lotus, Kibo's Bananamobile, Sawtooth Avenger, Tinfoil
  Helm, Power Level Analyzer, Twenty Lessons, That Which Was Compleated (UNK uncommons).**
  - `Play AnySupportedCard$ Type:<type>` (every card with that type or subtype) and `XIs$` (the value of X when
    the played spell has X in its mana cost).
  - Player property `hasConspiracyInCommand`.
  - New Banana token for Kibo's Bananamobile: an artifact with "{T}, Sacrifice this token: Add {R} or {G}. You gain
    2 life."
- **Unknown Event: Ano'thr, Equipment Commander, Zimone's Homework, First Stage of Magic Design, The Dogronmaster
  (UNK rares).**
  - `Count$YourLifeIn.LazyCaterer|Fibonacci|Square`; card property `powerEqualsToughness`.
- **Unknown Event: Wheel of Not Ideal, The Cobra King, Autumn Willow and Baron Sengir, The Bird Champion, Emerin,
  Shivan Angel (UNK).**
  - New effect `PassLibraries`: each player's library, in order, goes to the next player in turn order; the cards
    keep their owners.
  - Card property `Damaged <defined>`: cards that dealt damage this turn to the defined card.
  - New tokens: a 1/1 green-blue flying Bird (The Bird Champion) and Cobra Coil, a 1/1 blue Serpent (The Cobra King).
- **Unknown Event: Genevieve, Conniving Dragon, Margle, Cousin of Yargle, The Knight of Commentary, The Judge of
  Height, Tarkiran Towelsbane, Drop Bear (UNK rares).**
  - Continuous statics take `CantGainAbilities$`: from that effect on, nothing in layer 6 gives the card abilities
    back, whatever its timestamp (Pump grants included).
  - `Count$ShorterCreatures`: creatures whose toughness plus mana value is less than the source's.
  - New creature types Artist, Athlete, Chef, Judge and Koala, printed on UNK cards; Forge dropped them before.
  - New 1/1 red Human Rebel token (Tarkiran Towelsbane).
- **Unknown Event: The Majestic Duo, The Zabulous Cosplayer, Shen, Wish Granter, The Goblin Mastermind, The Brewing
  Chef (UNK rares).**
  - Card property `CreatesToken`: an ability that makes a token (Token, CopyPermanent, Amass, Investigate,
    Incubate, keyword-made ones included).
  - Copy effects now add `AddTriggers$` after `RemoveAllAbilities$`, so a copy that "loses all other abilities"
    keeps the trigger it gains.
- **Unknown Event: Evy, Fang Keeper, The Ice Dancer, The Sad Creaturez, Tarelily, Lovestruck Archer, Princess
  Snowfall, The Joiner of Cats, The Bus Runner, The River Warlock (UNK rares).**
  - Evy's "draw on it" is an honor-system choice of what you drew; a creature that has shared its feelings
    (The Sad Creaturez) keeps a "Shares its feelings" marker while it stays on the battlefield.
  - New Desert Vehicle token for The Bus Runner: a 4/4 artifact land with crew 2 and {T}: Add {C}. Whenever it or a
    Gamer you control becomes tapped it loses an hour counter; with none left, each opponent loses 1 life, you gain
    1, and it gets eight hour counters again.
- **Unknown Event: The Fifth Alias, Gomif, Fast Racer, The Mox Painter, Efteekay, Flame of the Kav, Frie, Displaced
  in Time, Pirk, Heroic Captain, The Cookout Creator, The Wizardly Barge (UNK rares).**
  - A deck with SVar `AsksSecretList:N` secretly lists N nonland cards as the game begins; card property
    `OnYourSecretList`.
  - New static `SpeedUncapped`: speed can rise past 4, which still counts as max speed.
    - Past 4 the speed display shows the number instead of "Max speed", and the speed-increase trigger now checks
      whether speed can still rise (`Count$YourSpeedCanIncrease`) instead of "less than 4".
  - `NameCard ExcludeControlled$`; `LegalIn$` now takes any format by name (Vintage for "tournament-legal").
  - `Count$DifferentNames <valid>`.
  - Warp's end-step exile is marked `WarpExile` (spell property) for "exiled because it was warped".
  - `CounterAddedOnce FullyStations$`: the counters took a Spacecraft to its highest station number.
  - New effect `NameIt`: a player gives a permanent a name of their own.
    - **AI:** it picks one of six ready-made names (Starhopper, Lucky Comet, Void Runner, Silver Lantern, Wandering
      Star, The Long Shot).
  - New tokens: Pirk's legendary 2/2 Spacecraft (Station 6, a flying artifact creature at 6+) and The Cookout
    Creator's 1/1 colorless Human.
- **Unknown Event: The Asker of More Mana, The Keeper of Favorite Cards, The Water Maro, The Vegetable Car, Jiffy,
  Vehicle Repairer, The Bearded Teakeeper, Uh, Squen & Cragg Made a Card, The Herald of Numot, The Caffeinated
  Runner, The Weekly Princess, The Milling Runner (UNK rares).**
  - New static `ManaPaymentTax`: every mana payment costs more, effects and triggers included (a cost of no mana
    at all is left alone).
  - A per-game favorites list on each player: `NameCard AddToFavorites$`, card property `YourFavorite`.
  - Card property `nameHasWord_<word>`.
  - New keyword Onionfect: the damage is still dealt, but it becomes Food tokens for the source's controller.
    Deathtouch on the source still counts for a creature it damages.
  - New static `InvertPTCounters`, which turns +1/+1 and -1/-1 counters around.
  - New static `JumpStartNotExiled`; jump-start's exile now checks spell property `JumpstartExiled`.
  - `DayTime Value$ Neither` stops tracking day and night, and the match background goes back to normal.
    - `GameEventDayTimeChanged` now carries null for "neither"; no day or night sound plays then.
  - `DayTimeChanges` triggers take `To$ Day|Night`.
- **Unknown Event: The Sixth Seraph, The Keeper of Four Scythes, The Knight of Weeks, The Highland Runner, Crux of
  Mirrodin, The Curve Keeper, Theopholos, Order Acolyte, Agoraphobic Phyrexian (UNK white rares).**
  - `StopBeingCommander` takes `CommandersOf$` (every commander those players have) and `ShuffleIntoLibrary$`.
  - `Count$LongestManaValueRun <valid>`: the most consecutive mana values among those permanents.
  - A commander with SVar `StartingLifeBonus` (and optionally `StartingLifeIfMono`) raises its owner's starting life
    as the game begins.
  - New artifact type Scythe, for the Keeper's Scythe tokens.
  - "Originally printed in" no longer counts promo sets, whose catch-all ones (DCI Promos, Magic Online Promos)
    carry one early date for years of cards and made them look like every card's first printing.
- **Unknown Event: Htbr, Racetrack Referee, Stika, Playtestress, Gerrard and Hanna, Draw Team Lines, Ratatwotwo,
  Kylem All-Star, Save Point, The Inspector Inspector, Robby, Another Dwarf (UNK white rares).**
  - `CantBeCast` now takes `ValidSA$`; new spell property `AlternativeOrFree` (an alternative cost, or no mana cost).
  - `Squad:N:NonLegendary` makes squad tokens that aren't legendary.
  - New Chef Role token.
- **Unknown Event: Lord of Atlanta, The Karfell Rocker, Rinta, Cousin of Sitka, The Clever Magician, The Misty Stepper
  (UNK blue rares).**
  - The Clever Magician: Forge can't make a proposed rule true and the AI can't weigh one, so a proposal is always
    declined and it scries 2 (as its commander, after the opening hand is kept).
- **Unknown Event: The Bringer of the Red Beard, The Wiley Speedster, Mijo, the Bull, Mzed, Mercenary Leader, At Least
  It's a Dry Heat, Auntie Ant, Ant Auntie, The Powerful Dragon, Tragda, the Burner, Brutal Command, Math is for
  Blockers, Snapsail Rider (UNK red rares).**
  - New Red Beard and Mercenary (can't block) tokens; Mijo uses the existing Rock token.
  - Not modeled: At Least It's a Dry Heat's redirection and player-damage clauses, and Snapsail Rider's match points
    (Forge matches have no base points; it says so when it enters).
- **Unknown Event: Stormscale Wurm, Praetorhoof Behemoth, Lander Rizzi, Emrakul and Chatterfang, The Bear Force Pilot
  Runner, The Magical City, New, The Goblin Sparring Grounds, The Local Guides, A-Town, Lona, Tracker of the Known,
  Mr. Wiggles, Helpful Butterfly.**
  - **City (user ruling):** a player whose deck has The Local Guides or A-Town types their city as the match begins;
    the AI is where its human opponent is (`Count$CityStartsWith`, `Count$CityHasFirstLetterOf`).
  - **Releases (user ruling):** property `releasesGE<n>` counts the sets Forge has a card in, digital-only ones too, but
    not promos, custom sets or other-language sets. `ReduceCost` `Color$ AnyOneColored` takes off one colored symbol.
  - "can be your Commander" with a capital C now makes a card commander-eligible too.
  - New tokens: The Bear Force Pilot Runner's 0/0 flying Vehicle (crew 2, +1/+1 for each Bear you control) and
    Emrakul and Chatterfang's 1/1 green Eldrazi Squirrel Scion ("Sacrifice this creature: Add {G} or {B}.").
- **Unknown Event: Guild Pact, The Five Stages of Grief, Call Up Emrakul to Help, Everyone's an Impostor, Pulse of the
  Hunter Maze, The Paradise Bird, Phyrexian Cytoshaper, Maître Tree, Grek the Ogre, Land Station.**
  - Everyone's an Impostor humors its own reminder text: granted changeling gives every creature type.
  - New Donkey and Ogre Noble tokens.
  - **Fix:** Catch-Up Mechanic's Oracle text drops the space before its em dash ("choose any number instead—").
- **Unknown Event: Another Night in Vegas, Yet Another Night in Vegas, A Chaotic Night in Vegas, The Archenemy's
  Charm, Monster Mash-Up, The Zassacre Zirl.**
  - `Cleanup` `ResetChosenModes$` lets every mode be chosen again; `SetInMotion` `DefinedName$` sets a named scheme in
    motion from outside the game; new `StopBeingCommander` effect (`MinCommanders$`).
  - A Chaotic Night in Vegas picks among all thirteen modes of the three Nights (its text says twelve).
- **Unknown Event: Old Way Phyrexian, Hidetsugu's Poison Rite, Invasion Specialist, Quest Compleated Beast, Rankle,
  Master of Pranksters, The Swamp Kitten, Rat of God, Davvol's Birthdaymobile, Auntie Flint (UNK black rares).**
  - New static mode `CantRemoveCounter` (players), player property `poisonCounters<op><n>`, and
    `Count$DeckIdentityMono.<color>` for Mono Eminence (the deck is every nontoken card the player owns in the game).
- **Unknown Event: Colorless Ultimatum, Battle Myrsphere, Chancellor of the Mulligan, Wurmcoil Broodmother, Mindslaver
  Toolkit, Guildmark, Trampled Lotus, Seer of the Bright Side (UNK RA07-RB02).**
  - **Pledge:** new `Pledge` effect; a player joins one of the ten guilds for the rest of the game, and mana can be
    `Produced$ Combo GuildColors`. The AI joins the guild its deck's colors favor.
  - **Tapping opponents' lands:** new `TapForMana` effect; you get one mana of a color each land could make.
  - **Split second on abilities:** an ability with `SplitSecond$ True` ("splitter second") stops responses like a
    split second spell.
- **Unknown Event: Goblin Savant, Jitte and Divining Top, Bob the Claymore, Phyrexian Incubator, The Mysterious Sphere,
  Oilskelion, Winter's Demonmech, Chandra's Dragonmech, Platinum Persecutor (UNK RA02a-RA06a).**
  - New property `backFaceType<Type>` (a double-faced card whose back face has the type).
  - **Race teams:** a deck with Winter's Demonmech is on Team Speed Demons, and one with Chandra's Dragonmech on Team
    Cloudspire (`SVar:ForbiddenRaceTeam`); a deck with both chooses (honor system).
- **Unknown Event: Delphia, Undecided, Gen, Confider of Ages, You, Magic Playtester, You, Iterative Playtester, The
  Unknown Wizard (UNK mythics).**
  - **Playtest cards:** a new `playtest` printing trait, from Scryfall's playtest promo type (property `Playtest`).
  - **Planes:** a new `plane=` printing trait, from the MTG Wiki's set infoboxes (`_tools/printing-traits/set-planes.json`;
    sets on several planes are left out). It's ready for Minnea, Planar Tourist.
  - **Tooling:** `PrintingTraits.txt` was regenerated with both traits. The generator
    (`_tools/printing-traits/generate.js`) and `set-planes.json` were committed with this batch, then untracked again
    in the next commit, since `_tools` is git-ignored; they stay in the local checkout only.
  - **Delphia:** the Phyrexians won the February 18th, 2023 event (MTG Wiki), so its Phyrexian Victory sets apply.
    New property `originallyPrintedIn_` (the set a card's name was first printed in).
  - **Gen:** `Count$DifferentReleaseYears`, by each permanent's own printing.
  - **Playtesters:** `AdjustNumber` `WhileOnStackOrBattlefield$`; a permanent keeps the change until it leaves the
    battlefield.
- **Unknown Event: Point to the Scoreboard, Chic // Ago, One Does Not, Illuminating Detective, Unclaimed Cat, Catch-Up
  Mechanic, Drive to Work, I'm Going On An Adventure!, Spare Changeling, Blended Twistling (UNK CU11-CZ03).**
  - New card property `sophisticated` (fifteen or more letters in the name), spell property `AdditionalCostPaid`
    (kicker and other optional costs, or a part of a spell's cost that isn't mana), and counts `DevotionHybrid` and
    `OpponentsCommandersRevealed`.
  - Drive to Work's story is the honor system; the AI tells one 80% of the time.
- **Unknown Event: Azra Matchthrower, Establishing Shot, Now THIS Is Aether Racing, Burn the Phyresis, My Deck is
  About a Seven, Irrefutable Evidence, Drake with Set's Mechanic, Unclaimed Bird, Educated Detective, Shadowmoor Draw
  Spell, Wistful Puppeteer, Literal Delver of Secrets (UNK CR04a-CU06).**
  - **Battlebond:** `Bond` `Battlebond$ True` pairs a creature with a battle, and the pair holds while you control both.
  - **Race teams (user ruling):** each Aether Racing line is castable by its own team, or by a player on neither team
    yet, who then joins that team.
  - **Collect evidence:** the trigger now knows which cards were collected (`ValidCard$`).
  - **Counters:** `RemoveCounter` `EachKind$ True` removes one counter of each kind; the AI picks opponents' permanents,
    and its own only when all their counters are -1/-1.
  - **Untap Convoke:** new keyword. Tapped creatures are untapped to pay, as convoke taps untapped ones; it counts as
    convoke, and backing out of the spell taps them again. The AI uses every one that can pay.
  - New Animated Role token.
- **Unknown Event: Underdog Racer, Unclaimed Tanadon, Take the High Ground, Tarkir Omenpath, Unknown Event Shores,
  Command Mine, Command Power Plant, Fear of Going 0-2 Drop, Aggressive Detective, Join the Winning Team (UNK
  CG05a-CR03d).**
  - **Scoreboard (user ruling):** new `ScorePoint` effect. The match keeps the Mirran and Phyrexian teams' points;
    you're behind when your team has fewer (`Count$YourTeamBehind`).
  - **Teams:** a deck with a card that cares about a team (`SVar:AsksTeam`) joins one as the game begins, if it hasn't
    this match, so team-based costs and abilities work from the start. Race teams (Cloudspire, Speed Demons) work the
    same way (`Teams$ Race`, `Count$RaceTeam.Cloudspire`).
  - **Event record (user ruling):** `Count$EventLosses` counts the matches lost in a gauntlet or tournament (only draft
    and sealed gauntlets go on after a loss), and otherwise the games lost this match.
  - **Fix:** a static that sets a card's mana cost (`ManaCost$`, `Incorporate$`, `SetColoredSymbolsTo$`) now gives the
    cost back when it stops applying. Before, Celestial Dawn's symbols stayed white after it left.
- **Unknown Event: Joraga Peach, Naturalize the Phyresis, That's No Moonmist, Growing Detective, Investi-Gate, Merfolk
  Surveyor, Fear of Unsleeved Decks (UNK CG01-CG04).**
  - **Explore-clash:** new `ExploreClash` effect (`WinSubAbility$`); the AI decides about its own revealed card as it
    does when exploring. Each player whose revealed card was a nonland keeps it on top or bins it, as explore does.
  - **Sleeves (user ruling):** Forge's sleeve choice decides. Built-in sleeve 0 is the plain Magic card back, so it's
    unsleeved; any other built-in sleeve or a card-art sleeve counts as sleeved (`Player.isSleeved`,
    `Count$YouAreSleeved`).
- **Unknown Event: Unclaimed Blessing, Life at Stake, Mark Ritual, Long-Term Phyresis Study, Common Curve Filler,
  May the Mana Be with You (UNK CB11-CC03).**
  - **Teams (user ruling):** new `ChooseTeam` effect. A player joins the Mirran or Phyrexian team the first time a card
    asks, and stays on it for the whole match; every card reads the same team (`Count$UnknownTeam.Mirran`). The AI
    joins at random, then picks its target to suit the team.
  - **Match notes:** `Match` keeps per-player notes that last the whole match (`Player.getMatchNote`).
  - **Mark Ritual (user ruling):** as the match begins, you're asked whether each copy is signed by a Magic card
    designer. A signed copy is red, costs {1}{R}, and adds {R}{R}{R}. The AI's copies are unsigned.
  - **Common Curve Filler (user ruling):** the number is chosen as the game begins and written into its mana cost,
    power and toughness for the game. The AI fills the emptiest slot of its curve from two to five.
- **Unknown Event: Reverse Ninja, Demon Detective, Syr Konrad's Squire, Fear // Loathing, May of the Machine, Shedding
  Snake (UNK CB02-CB05).**
  - **Ustujnin:** `ChangeZone` `Ustujnin$ True` puts the card onto the battlefield blocking the creature the returned
    blocker was blocking (from its combat last-known information).
  - **Commander tax:** `Player.getCommanderTax` now counts only casts since the tax was last reset; the new
    `ResetCommanderTax` effect resets it without touching the cast count. The AI pays Shedding Snake's 2 life only when
    there's tax to take off (`UnlessAI$ CommanderTax`).
  - **Detectives:** `Count$AllCommandersRevealed` is true with no commanders, or none still face down.
  - **Dates:** `Count$RealWorldMonthDay` (May 6th is 506), from the real date where the player is.
- **Unknown Event: Your Mana Rock, Unicycling Automaton, Color Pie, Melira's Snacks (UNK CA02b-CA12).**
  - **Your Mana Rock:** its two colors are marked in the deck editor like Cryptic Spires', or chosen as the game
    begins (user ruling). Any card with `SETCOLORID` now gets this, so an unmarked Cryptic Spires asks too instead of
    making no mana. The AI's two colors are now its deck's two most prominent; before, it only ever marked one.
  - **Hand Backup:** the revealed card is marked until it's cast from your hand and enters with its counter, and it
    has basic landcycling in your hand until end of turn.
  - **Poison Tolerance:** new `PoisonTolerance` static mode; each instance raises the poison counters it takes for you
    to lose.
- **Unknown Event: A Container of Booster Packs (UNK CA02).** `OpenBooster` gains `Container$ N`.
  - You name the container's N different sets (honor system), and a real booster of one of them at random is opened.
  - You keep one card in hand; the rest of the pack stays out of the game.
  - **AI:** its container holds sets at random; it opens it in its second main phase and keeps the best card (new
    `OpenBoosterAi`; other pack-opening cards are still left alone).
- **Tests:** one new test class per batch, `UnknownEventBatch1Test` through `UnknownEventBatch29Test` (29 classes, in
  `forge-gui-desktop/src/test/java/forge/ai/`).

### 2026-10-21, twelfth deploy (desktop, Android, GitHub) — Unfinity complete; 24-commit upstream merge

- **Upstream merge (24 commits).** Five card-script conflicts, resolved by keeping upstream's fixes with the fork's
  Scryfall wording: Bell Borca, Spectral Sergeant (`CharacteristicDefining$`), Fear of Change (`Origin$ Battlefield`
  on its dies trigger), Overclocked Electromancer (`CombatDamage$ True`); Disciple of Perdition and Urza, Academy
  Headmaster kept the fork's text, upstream's change already being in it.
  - Upstream brought (per its commit subjects): Star Trek (TRK) cards and edition updates for SDS, TRC and TRK (Borg
    Cube among them); card fixes (Bell Borca, Sutured Ghoul and Unlicensed Hearse P/T statics marked
    characteristic-defining; Righteous Indignation; Experimental Frenzy and The Belligerent; Fear of Change, My
    Champion Stands Supreme, Death or Glory, Become Anonymous and Wake to Slaughter; Bleeding Effect, Mondo Gecko and
    Party Dude; Oglor and Quandrix Command; Mole Module, Overclocked Electromancer and Raphael; Your Mistake Is My
    Triumph, Disciple of Perdition and Last Rites; Magik, Illyana Rasputin); a TargetSelection/InputSelectTargets update
    fixing Chaoslace and similar cards; selectable players marked in entity-list prompts; a centralized User-Agent
    (with a UI identifier) for image and data downloads; Realm of Legends 1.081 Adventure-mode fixes; an LDA
    deck-generation data update for
    Reality Fracture (Standard/Modern); a new splash screen and a Start-button text fix.
  - Count: 22 non-merge upstream commits plus 2 upstream merge commits (24 in `M^1..M^2`).
- **Unfinity: Truss, Chief Engineer (UNF #181).** New `AdjustNumber` effect. It reaches power, toughness, loyalty, a
  spell's generic mana, and the literal numbers in the target's abilities (damage, cards, life, counters, tokens, dice
  and sides, pumps and the like - user ruling).
  - The change lasts until end of turn, never goes below 1, and can't touch an X (rulings).
  - Names and collector numbers stay as printed.
  - **AI:** before combat it adds its hack counters to its best attacker's power, or failing that takes them off an
    opponent's best creature.
- **Unfinity: It Came from Planet Glurg (UNF #169).** New `CloneCombined` effect: a copy of X creatures at once, merged
  in full (user ruling).
  - Total power and toughness, combined mana costs, and every type, color and ability.
  - Every name: `Card.getNames` and name sharing see each one, and so do the word-count and first-letter checks
    (rulings).
  - **AI:** it caps X at the creatures there are to copy (new `AIXMax` SVar for permanent spells).
    - It picks the best creatures to copy (`CloneCombinedAi`).
- **Unfinity: Log Flume (UNF #220).** New `JumpInLog` effect and `FlumeLog`.
  - **Blocking:** attacking logmates are blocked all together or not at all; one that isn't attacking doesn't count
    (CombatUtil.validateBlocks). The AI unblocks a log it has only partly blocked.
  - **Targeting:** a spell or ability the log's player controls that targets one logmate targets each of them, from
    before it's put on the stack (rulings).
- **Unfinity: Grand Marshal Macie (UNF #168).** "Until end of turn" effects are now `PausableUntil`s that a tapped
  Macie can hold (new `HoldUntil` effect).
  - A held effect that reaches its end waits, and ends as soon as she untaps or leaves; if she untaps first, it goes
    on to its usual end (rulings).
  - Leaving her tapped in the untap step runs her `NotUntapped` SVar there: a pause counter, then life loss.
  - **AI:** in its end step it holds an effect of its own, and stays tapped to keep holding while its life allows.
- **Unfinity: Urza's Fun House (UNF #199).** The Fun House button picks one of Urza, Academy Headmaster's three outcome
  lists at 75/15/10, the page's own weights, then an outcome evenly within it.
  - Planeswalker-only outcomes are reworded as Wizards' page has them.
  - An outcome with no legal targets is clicked again (rulings).
  - {∞} is colorless, once per object, with an Urza's Mine, Power-Plant and Tower (rulings). No engine change.
- **Unfinity Attractions: Memory Test, Scavenger Hunt, The Superlatorium, Trivia Contest (UNF #221, #226, #230,
  #233).**
  - **Memory Test:** new `MemoryTest` effect. Five nonland cards are exiled from the bottom and turned face down; the
    player names them from the card-name search, the same card twice if it's there twice (rulings).
    **AI:** it remembers each card 80% of the time (user ruling).
  - **Scavenger Hunt:** new `ScavengerHunt` effect. Art Tagger tags with the item counts (animal-person, alien,
    food, horn, tusk, now in the `artobjects` trait); for other art the player is asked.
    **AI:** it finds a tagged card in time 80% of the time.
  - **The Superlatorium:** new `SuperlativeChoice` effect. The person outside the game is simulated, judging by stats
    with some noise (user ruling).
  - **Trivia Contest:** new `TriviaContest` effect. The player names a card from that category's list against the
    clock (honor system). **AI:** it answers in time 75% of the time when there's an answer, and as the opponent
    picks planeswalker card.
- **Unfinity Attractions: Guess Your Fate, Push Your Luck, Squirrel Stack (UNF #215, #224, #228).**
  - **Guess Your Fate:** played for real with a person in the room; the player says whether the guess was in the
    name (user ruling). The AI's simulated guesser is right 30% of the time.
  - **Push Your Luck:** new `RevealLibraryUntilStop` effect. At least one card is revealed and X is 0 (rulings).
    **AI:** it keeps going while its total is 4 or less.
  - **Squirrel Stack:** the player's own dice, counted on the honor system. **AI:** its simulated stack is usually
    three to six dice.
- **Unfinity Attractions: Centrifuge, Cover the Spot, Dart Throw, Gift Shop (UNF #203, #207, #208, #214).**
  - **Centrifuge:** Draw's `FromLibraryOf$ ToTheirRight` has each player draw from the library of the player to their
    right; the card stays its owner's (rulings).
  - **Dart Throw and Cover the Spot:** simulated with the dexterity toss animation (user ruling): one card touches
    50% of the time, and three cards cover completely 15% of the time.
  - **ThrowCards:** new `NoDamage` and `CoverChance` parameters for throws at a prize.
  - **Gift Shop:** each mode once a game.
- **Unfinity: Wicker Picker (UNF #196).** New `StickerKicker$` parameter on the OptionalCost static mode, one optional
  cost per Wicker Picker (up to four).
  - When it's paid, the caster gets {TK} and may put a sticker on the spell just before it's considered cast, so cast
    triggers see it (rulings).
- **Unfinity: Greatest Show in the Multiverse (UNF #189).** New cost `ExileOutside<N>`: the player picks an artist, then
  N of their instants and sorceries with different names from any card in Forge (user ruling, following the Un-
  rulings). The cards come into the game in exile, exiled with the Saga.
  - A card with several artists counts for each.
  - **AI:** it takes the artist whose five biggest spells are biggest.
- **Unfinity: The Big Top (UNF #197).** Any color, chosen by the player (honor system; in an Un- game any color can be
  added - rulings). No engine change.
- **Unfinity: Souvenir T-Shirt (UNF #192).** New RollDice parameters `SecondChosenSVar` (a second result from the rest)
  and `ChosenOnHost` (kept on the card for its static ability).
  - The Magic-branded items worn are counted as it enters (honor system).
  - **AI:** its simulated wardrobe usually has none.
- **Unfinity: Park Map (UNF #190).** Land printings' illustrations are now in `PrintingTraits.txt` (`art=`); an art
  sticker makes a card's art its own (rulings). New `sameArtAsPermanentYouCtrl` property.
- **Unfinity: D00-DL, Caricaturist (UNF #187).** New Sketch token and creature type.
  - New GenericChoice parameter `MinChoiceAmount` for "up to" choices: the player chooses every feature their
    drawing has (honor system).
  - **AI:** its drawing has a few features at random.
- **Unfinity: Blue Ribbon (UNF #184).** The person outside the game is simulated: they pick at random, likelier the more
  impactful the creature (user ruling, as for Defective Detective).
  - Best in show is a trigger the equipped creature gains.
  - **AI:** it copies only spells and abilities that help a creature onto the ribboned one.
- **Unfinity: Autograph Book (UNF #183).** Signers are counted as it enters (honor system).
  - **AI:** its simulated book has one to five signers.
- **Unfinity: Pietra, Crafter of Clowns (UNF #176).** The joke is a FlavorAction cost; whether the person laughed is
  asked (honor system).
  - **AI:** its audience laughs half the time.
- **Unfinity: Meet and Greet "Sisay" (UNF #172).** New `CardFlavorLines` count and `sharesExpansionWith` property
  (same set, whatever the rarity color; rulings).
- **Unfinity: Lila, Hospitality Hostess (UNF #170).** Common spells by the printing's rarity. No engine change.
- **Unfinity: Claire D'Loon, Joy Sculptor (UNF #165).** New static mode `TokensAreCards`: your tokens go on existing in
  whatever zone they go to. The rule that a token can't move again once it has left the battlefield doesn't apply
  either (rulings).
- **Unfinity: "Brims" Barone, Midway Mobster (UNF #163).**
  - **Counters:** creatures with a hat Forge knows of get theirs at once; you're asked about the rest in one choice.
  - **Menace:** you're asked as it attacks whether you're wearing a hat (user ruling); that's when it matters.
  - **AI:** it counts only known hats, and wears one half the time.
- **Unfinity: Angelic Harold (UNF #162).** New `wordsInName` card property; a token's name ends in "Token" (CR 111.4).
- **Fix: Well Done (UNF #128)** dealt its extra 3 damage for rares but not mythic rares.
  - Its script checked `RarityMythic`, which no card has; mythic rares are `RarityMythicRare`.
    `UnfinityRedGreenBatchTest` gained a mythic case (Baneslayer Angel from M10).
- **Unfinity: Tug of War (UNF #159).** New Subgame parameter `StartingPermanents$ N`.
  - Each player sets aside up to N permanent cards with different names from their library. Those go onto the
    subgame battlefield once mulligans are done, starting player first.
  - The subgame's one winner puts one of their set-aside main-game cards onto the main-game battlefield; after a
    draw, no one does (rulings).
  - **AI:** subgame card choices take the best card.
- **Unfinity: Spelling Bee (UNF #155).** New `SpellChosenWord` effect. The damaged player chooses any word from the
  top card's name, type line or text box.
  - The speller is shown the word, spells it out loud and says whether they got it right (user ruling: honor
    system).
  - **AI:** it chooses the longest, least common word, and spells by chance, less often the longer the word.
- **Unfinity: Tchotchke Elemental (UNF #158).** Knickknack counters; the unique items on the physical card are the
  player's to find (honor system). No engine change.
- **Unfinity: Sole Performer (UNF #154).** New `AddTapSymbols` effect: a player's pool of {T}s.
  - The pool empties as each step and phase ends, as mana does, but isn't mana.
  - A {T} cost can be paid from it instead of tapping the permanent, so an already-tapped permanent's ability can be
    activated too, but never a summoning-sick creature's (rulings).
  - The player is asked whenever they could do either.
  - Spending one is logged, and a {T} spent on a cost that's then backed out goes back to the pool.
  - **AI:** it activates Sole Performer only when it has another permanent with a non-mana {T} ability, and always
    spends a {T} it has before tapping.
- **Unfinity: Plot Armor (UNF #152).** Blockers need flavor text on their printing; stickers neither add nor remove it
  (rulings). No engine change.
- **Unfinity: Pie-Eating Contest (UNF #151).** New cost `Gobble<N>`: the payer picks which Food tokens to sacrifice
  (all at once), and each gobble left over is a bite of food they confirm taking (honor system).
  - **AI:** it takes bites and keeps its Food, with X one per creature it could attack with.
- **Unfinity: Mistakes Were Made (UNF #147).** The fire extinguishers in sight are counted by the caster (honor
  system).
  - **AI:** it counts a simulated room's (mostly none, now and then one, rarely two).
- **Unfinity: Jermane, Pride of the Circus (UNF #144).** Legs are counted in the art, on the figure that is the
  creature (rulings).
  - **Printing data:** art Scryfall Tagger tags with four or more legs is now in `PrintingTraits.txt` (`fourlegs`,
    `Card.KnownFourLegs`). Art with a rider is left out, since those legs are usually the mount's (Black Knight).
  - For any other creature the activator is asked as the ability resolves; if it doesn't have them, nothing happens.
  - **AI:** it targets only creatures known to have four or more legs.
- **Unfinity: An Incident Has Occurred (UNF #143).** New Charm parameter `AfterModesAbility`: an instruction after the
  modes that happens whichever were chosen. Apologizing is honor system; the AI always apologizes.
- **Unfinity: Icing Manipulator (UNF #142).** New static mode `CountersAreTokens`: each +1/+1 counter on a creature you
  control has a stand-in Food token linked to that creature, and the counter stays on it (rulings).
  - The stand-ins start and stop being permanents as the counters come and go, without entering or leaving the
    battlefield. One that leaves on its own (sacrificed, exiled, bounced) takes its counter with it.
  - They're kept in step whenever state-based actions are checked.
  - **AI:** it sacrifices one of these Foods for life only when its life is short.
- **Unfinity: Hardy of Myra's Marvels (UNF #141).** Lines of flavor text as printed on each printing, read off the card
  image, are now in `PrintingTraits.txt` (`flavorlines=`); no flavor text is 0 (rulings). New `flavorLines` card
  property (`Card.flavorLinesEQChosen`).
  - **Known gap:** a printing whose text box couldn't be read (about 2%) is estimated at 35 characters a line.
  - **AI:** it chooses the number paying the most counters over the spells it has left.
- **Unfinity: Vorthos, Steward of Myth (UNF #126).** The named characters are Scryfall Tagger's (3,418), each with every
  name it has gone by (rulings: Lim-Dûl on The Raven Man, Urza on Blind Seer).
  - **Printing data:** `PrintingTraits.txt` now lists them and says which a printed card names in its name or flavor
    text, possessive and derived forms included (Teferi's Protection, Avacynian Priest), or shows in its art
    (`_tools/printing-traits/characters.js`).
  - **Choosing:** the player types any of a character's names, accents optional, and picks among the matches; an
    empty answer shows the whole list, the characters on their own cards first. The AI names the character on the
    most of its spells.
  - **Known gap:** a name hidden inside another word (Niv-Mizzet's Izzet Charm) isn't caught.
- **Unfinity: Alpha Guard (UNF #129).** Untaps a permanent whose name starts with the chosen letter, skipping spaces
  and punctuation (`nameStartsWithChosenLetter`). No engine change.
- **Unfinity: Well Done (UNF #128).** Uses the printed card's rarity, as Collectigull does (`RarityRare`,
  `RarityMythic`). No engine change.
- **Unfinity: Trigger Happy (UNF #125).** New `TriggerAbility` effect: one of the target permanent's own triggered
  abilities (keywords included) is wrapped like a real trigger and put on the stack under the caster's control.
  - The trigger event is ignored, an intervening "if" is still checked as it resolves, and the caster chooses its
    targets (rulings).
  - **AI:** it retriggers the best enters-the-battlefield ability among its own permanents.
- **Unfinity: Ticking Mime Bomb (UNF #124).** A person outside the game picks a creature you don't control (honor
  system: you enter their pick). It's dealt twice your Robots in damage, the Bomb included.
  - **AI:** a simulated person picks at random, via a new ChooseCard logic, `Random`.
- **Unfinity: Rock Star (UNF #122).** You may discard to reveal the top five. A person outside the game picks the most
  rock-band-like card (honor system), it goes to your hand, and the rest go to the bottom in random order.
  - **AI:** a simulated person picks at random.
- **Unfinity: Opening Ceremony (UNF #118).** Adds {W}{U}{B}{R}{G}{C}. You may open a real generated booster of a set
  you choose (Stocking Tiger's `OpenBooster`, now with `Destination$` and `RememberOpened$`).
  - Its nonland cards can be cast from exile this turn, cast from outside the game (rulings).
  - At cleanup the rest go back outside the game, to the sideboard.
- **Unfinity: Omniclown Colossus // Pie-roclasm (UNF #116).** An Adventure card; the Colossus has affinity for Clowns.
  - **Known gap:** Pie-roclasm deals π as 3. Forge's damage is whole numbers, so the smidgen only shows from four
    Pie-roclasms against a creature with toughness 12½.
- **Unfinity: Juggletron (UNF #113).** Forge's stickers have no position, so each art sticker on it counts as an item
  juggled clear of everything else (+3/+0 each). Name, ability and P/T stickers and counters don't count (rulings).
- **Unfinity: Ignacio of Myra's Marvels (UNF #112).** New `artobjects=` printing trait (`Card.ArtHasChosenObject`),
  from Scryfall Tagger's tags for the ten objects and everything under each: about 41,000 printings.
  - Fire is Tagger's fire tree only, so lava and magma aren't fire (rulings).
  - Untagged art is asked about, as for Don't Try This at Home.
  - **AI:** it picks the object in the most of its remaining spells (`MostCommonArtObject`).
- **Unfinity: Goblin Girder Gang (UNF #111).** New `StoreResult` effect: it stores a die result unless that result is
  stored already, checked again as it resolves (rulings).
  - A reflexive trigger pings any target without a hat: a creature with no known hat, or a player (honor system).
  - New count, `Count$StoredRollCount`.
- **Unfinity: Goblin Cruciverbalist (UNF #110).** It makes a vowel token named A, E, I, O or U.
  - New `SpellWord` effect for the attack trigger. It uses the first letters of your permanents' names, each once, and
    a word you haven't spelled this game (`Player.getSpelledWords`).
  - A person types the word. The AI spells the longest common word it can, from a new list,
    `res/lists/SpellingWords.txt`: 38,918 SCOWL common words from two letters up, through Words.txt's blocklist,
    built by `_tools/wordlist/build-spelling.js`. Words.txt only has Hangman's six to eight letters.
- **Unfinity: Don't Try This at Home (UNF #105).** New `hot` printing trait (`Card.HotInArt`).
  - It comes from Scryfall Tagger's own hot tree ("things that are hot, like fire or explosions") plus the sun,
    torches, candles and volcanoes: about 12,500 printings.
  - **User ruling:** art Tagger hasn't tagged at all is asked about when it deals damage. The AI trusts the tags.
  - A hot source you control deals 1 more damage, and the sacrifice ability counts hot cards in your graveyard.
  - **Known gap:** a hot art sticker isn't seen, because Forge's art stickers don't record what they show.
- **Unfinity: Devil K. Nevil (UNF #104).** New `JumpOver` effect.
  - **User ruling:** the jump over any number of other creatures is simulated. It clears one creature 85% of the time,
    12% less for each more, never under 10%, shown with the dexterity toss animation. Clearing them gives that many
    +1/+1 counters.
  - **AI:** it jumps the number of creatures with the best expected counters (four, given enough creatures).
- **Unfinity: Carnival Barker (UNF #102).** Honor system: thirty seconds of lauding to real people, then you enter
  how many applauded.
  - It can be used once per object (`GameActivationLimit$ 1`, rulings).
  - **AI:** the audience is simulated at zero to three people (user ruling), like Gimme Five's high fives.
- **Unfinity: Art Appreciation (UNF #99).** You put an art sticker on a nonland permanent you own, chosen rather than
  targeted. A person outside the game rates it 1 to 5, and a reflexive trigger stops up to that many creatures from
  blocking.
  - **AI:** its simulated person rates evenly from 1 to 5 (user ruling).
- **Unfinity: Amped Up (UNF #98).** It costs {1} less per electrical socket you can see from your seat, announced as
  it's cast (`AnnounceType$ ChooseNumber`, as for Liquid Fire), with a reduce-cost static reading the number.
  - The announcement now shows its own prompt (`AnnounceTitle$`).
  - The AI now makes announcements too (`ComputerUtil`). Its simulated room keeps the same socket count all game
    (`AILogic$ Sockets`).
- **Unfinity: Aardwolf's Advantage (UNF #97).** +2/+0 and alpha strike (Jetpack Janitor's `Card.AlphaStrike`) for the
  creatures you control as it resolves. No engine change.
- **Unfinity: Scooch (UNF #91).** Four modes, each followed by a draw: +1 or -1 power with no duration (rulings), or 1
  life gained or lost.
  - **Known gap:** targeting a die roll mid-roll isn't supported; Forge has no window to cast spells while a roll is
    being modified.
- **Unfinity: A Real Handful (UNF #87).** You report how many hands are touching it with an honor-system action, and
  its panel shows the count (`ChooseNumber`, `NumberLabel$ hands touching`).
  - The +2/+2, menace and lifelink switch on and off with that number.
  - **AI:** it has no hands, so its count is always zero.
- **Unfinity: Rat in the Hat (UNF #86).** Its target can be any creature card, as with Park Re-Entry.
  - A known or claimed hat counts at once. Otherwise you're asked as it resolves; with no hat, the card can't be cast.
  - **AI:** it targets only known hats (`AITgts$`).
- **Unfinity: Questionable Cuisine (UNF #84).** One Food per trash can you can see from your seat (honor system).
  - **AI:** it simulates a room (ChooseNumber `TrashCans`: usually one, sometimes none or two, rarely three).
- **Unfinity: Photo Op (UNF #83).** Honor system: the posing picture is posted outside Forge before the game.
  - If a reply guessed, you name that card and put a copy you own from outside the game into your hand (`MakeCard`).
    Photo Op exiles itself either way.
  - **AI:** it doesn't post pictures, so it never casts this.
- **Unfinity: Nocturno of Myra's Marvels (UNF #82).** You choose a word of five or more letters (Rings a Bell's
  `ChooseType`).
  - New card property, `rulesTextHasChosenWord`: the exact word, so "attacks" isn't "attack" (rulings). It reads the
    rules text the way Kindly Cognician does, without reminder text, name or flavor text.
  - **AI:** new word logic, `OwnSpellsRulesText`, picks the word in the rules text of the most nonland cards it has yet
    to play.
- **Unfinity: Knife and Death (UNF #77).** It has Just a second (the existing keyword, as split second) and a new
  `ThrowCards` effect.
  - **User ruling:** throws are aimed. Each of the ten cards is aimed at a creature or planeswalker you don't control
    and lands touching it half the time, shown with the dexterity throw animation.
  - Damage is assessed after the tenth throw, 1 per card touching (rulings), and you gain that much life.
  - **AI:** it aims at the best opposing permanent that a few hits could finish (`ThrowCardsAi`).
- **Unfinity: Haberthrasher (UNF #76).** A creature with a known or claimed hat (`Card.HasHat`) drains at once.
  - **User ruling:** otherwise you're asked whether the creature has a hat. For any art sticker put on a creature you
    control, you're asked whether it was a hat, wig or headband sticker, since Forge's art stickers don't record what
    they show.
  - **AI:** it counts only known hats.
- **Unfinity: Gray Merchant of Alphabet (UNF #75).** One life per creature with the chosen letter anywhere in its
  name, however often it appears (rulings). New card property, `nameHasChosenLetter`.
  - **AI:** new letter logic, `MostOwnCreatureNamesContaining`.
- **Unfinity: Exit Through the Grift Shop (UNF #74).** You announce a nonland card in your graveyard; everyone bids
  life (`BidLife`, starting at 0).
  - The winner pays, then may cast a free copy (`Play | CopyCard$ | Controller$ Remembered`).
  - If you didn't win, you gain the bid and that many Treasures. That uses a new condition,
    `ConditionPlayerNotContains$`.
- **Unfinity: Animate Graveyard (UNF #65).** It works the way Animate Library does.
  - The Aura spell (`AnimateGraveyard`) makes your graveyard a permanent, a new `GamePieceType.GRAVEYARD`, and enters
    attached to it. It's a creature with no name, color or creature types (rulings).
  - Its base P/T equals the number of cards in it, and it has the text boxes of the creature cards in it
    (`GainsTextBoxOf`).
  - It's still a graveyard: cards still go there, and they aren't on the battlefield (rulings).
  - It never enters or leaves. If it would leave, the Aura is exiled instead, and nothing dies. If another player gains
    control of it, the Aura falls off (`YouOwn+YouCtrl`).
  - **AI:** it casts this with at least four cards in its graveyard.
- **Unfinity: Disemvowel (UNF #71).** Destroy, then the controller loses 1 life per unique vowel in the creature's
  name (Y counts). New count, `Count$UniqueVowelsInName.<Defined>`.
- **Unfinity: Treacherous Trapezist (UNF #60).** New card property, `Alliterative`: two or more capitalized words in
  the name begin with the same letter. The same word twice counts, and sounds don't matter (rulings).
- **Unfinity: Super-Duper Lost (UNF #59).** X is the number of doors you can see from your seat (honor system).
  - **AI:** it simulates a room (ChooseNumber `Doors`: 0, 1, 2 or 3 doors), the same approach as Now You See Me's
    mirrors, and targets the opponent's best creature.
  - **Engine fix:** a ChooseNumber that targets a creature still has its `Defined$` chooser. It used to ask only
    targeted players, so nobody chose.
- **Unfinity: Mobile Clone (UNF #51).** New effect, `Photocopy`: a token photocopy of the printed creature, so copy
  effects don't show in the picture (rulings). Everything in the shot is part of the copy and copiable:
  - the P/T its counters give, and its keyword counters, as intrinsic keywords;
  - its stickers;
  - the statics and triggers of the Auras and Equipment on it. Their "enchanted/equipped creature" now means the
    photocopy itself, so Bonesplitter's +2/+0 and a Pacifism come along.
  - **Known gap:** other counters, such as stun or shield, aren't in the picture yet.
  - **AI:** it photocopies the best creature it can target, anyone's, since the copy is its own either way
    (`PhotocopyAi`).
- **Unfinity: Phone a Friend (UNF #55).** A Charm with a new `PhoneAFriend$` option (`CharmEffect.phoneAFriend`).
  - A person calls someone real and enters which legal mode they picked, or that nobody answered (honor system).
    Only legal modes can be picked (rulings).
  - **User ruling:** the AI's friend is simulated. It picks a random legal mode, and one call in five goes
    unanswered.
  - Unanswered, an opponent chooses. The AI as that opponent picks the mode that helps the caster least: draw seven
    if they have fewer than seven cards in their library, otherwise a rough value per mode (`CharmAi`).
  - The AI casts it only with more than ten cards in its library, and re-targets after the friend's pick.
- **Unfinity: Plate Spinning (UNF #56).** Copies each spell you cast and counts the cards you're balancing
  (`Balanced`).
  - A human reports a balanced card falling, or touching another card, with an honor-system action. That triggers
    the sacrifice.
  - **User ruling:** the AI's cards wobble. Each time it balances another, the chance they fall is 15% per card it's
    then balancing. This uses a static trigger on `ValidActivatingPlayer$ You.IsAI`.
  - No engine change.
- **Unfinity: Octo Opus (UNF #54).** Its Contortionist token (`u_4_4_octopus_performer_contortionist`) keeps its fold
  count in a `Folds` SVar. A characteristic-defining static halves its power and toughness per fold.
  - **User ruling:** it folds only as far as the fork's half values go: 4/4, 2/2, 1/1, then 1/2 / 1/2.
  - Sacrificing Octo Opus draws one card plus one per fold of a Contortionist you choose. With none, it draws one
    (rulings). New count, `Count$SVarOfDefined.<Defined>.<SVar>`.
  - **AI:** it folds only while it controls an Octo Opus (`StoreSVarAi`, `AILogic$ FoldWithOctoOpus`).
- **Unfinity: How Is This a Par Three?! (UNF #49).** Whenever you cast a spell, you may have a player mill X, the
  number of words in the spell's name (`Count$WordsInName`).
  - Milling more cards than they have is an impossible option, so it can't be chosen (ruling). A condition on the
    target's library size handles this.
  - No engine change.
- **Unfinity: Focused Funambulist (UNF #47).** As it enters you may put an art sticker on it. Only then a reflexive
  trigger asks a person outside the game whether it looks balanced (rulings). A yes lets you tap or untap up to one
  target creature.
  - The sticker count is stored on the card first, because an ImmediateTrigger copies its conditions onto the
    trigger.
  - **AI:** new GenericChoice logic, `AskThenTapOrUntap`. It aims at an opponent's best untapped creature, or else
    its own best tapped one, and its simulated person says yes half the time. The plain `Chance.50` logic made the
    AI decline the trigger outright.
- **Unfinity: Decisions, Decisions (UNF #44).** Reveals the top three cards, then you choose a consonant (Y counts).
  Cards with that letter in the first word of their printed name, stickers included (rulings), go to your hand; the
  rest go to the bottom.
  - New card property, `firstWordHasChosenLetter`.
  - **AI:** new letter logic, `FirstWordsOfRemembered`, picks the consonant that takes the most of the revealed
    cards.
- **Unfinity: Busted! (UNF #41).** Mana Leak with X equal to the most words in a name among permanents you control,
  counted as it resolves (ruling).
  - New count, `Count$MostWordsInName.<Valid>`, which shares its word counting with Bosom Buddy's and Wordmail's
    `WordsInName`.
- **Unfinity: Blufferfish (UNF #39).** New effect, `Bluff`. An opponent tells you a statement about themselves and
  secretly notes whether it's true; you guess, and a right guess runs `GuessCorrect$` (+2/+0, can't be blocked this
  turn).
  - A person makes up the statement and notes its truth (honor system).
  - **User ruling:** the AI states a fact about its own hidden cards, false half the time, and notes the truth from
    the game. Examples: "I have a land card in my hand", "I have exactly 2 creature cards in my hand", "The top
    card of my library is a land".
  - An AI guessing has nothing to go on, so it guesses at random.
  - **AI:** it tries once in its first main phase when the Fish can attack (`BluffAi`).
- **Unfinity: Bag Check (UNF #36).** Counters a spell, then asks a person outside the game if it looks dangerous,
  using Flavor Judge's idiom.
  - A human asks a real person and enters the answer (honor system).
  - The AI's simulated person says yes half the time (`AILogic$ Chance.50`).
  - A yes means draw a card, then discard a card.
  - No engine change.
- **Unfinity: Astroquarium (UNF #34).** When it enters, you may put an art sticker on it.
  - Its ability turns a creature into a blue 4/4 Shark, or a blue 8/8 Octopus instead if the cracks are covered. Only
    its colors and creature types change (rulings).
  - Forge's stickers have no position on the card, so the cracks count as completely covered while an art sticker is
    on it (`stickeredWith Art`).
  - This is checked as the ability resolves; covering the cracks later doesn't change the creature (rulings).
  - No engine change.
- **Unfinity: Animate Object (UNF #33).** New effect, `AnimateObject`.
  - You get seven tickets, through the normal counter route, so replacements apply.
  - The object from outside the game means nothing to the game (rulings), so it's a blank, nameless, colorless
    creature token (`c_0_0_inanimate_object`).
  - Before it enters it gets a power/toughness sticker, then any of a name, an art and an ability sticker, each paid
    for in tickets. So it never sits on the battlefield as a 0/0, and enters-the-battlefield effects see the
    stickered creature.
  - Created tokens now carry their prototype's stickers onto the battlefield (`TokenEffectBase`).
  - **AI:** it casts this only when a power/toughness sticker will be affordable (`AnimateObjectAi`).
- **Unfinity: Trapeze Artist (UNF #32).** Its entrance flip is simulated like Orcish Paratroopers' drop: a full
  turn landing face up half the time, shown with the card-toss animation.
  - The outcome is a random choice, not a die roll, so cards that care about rolling dice don't see it.
  - It's a static trigger, so a botched flip sends the card back to its owner's hand before anyone can act
    (rulings).
  - No engine change.
- **Unfinity: T.A.P.P.E.R. (UNF #31).** Its cost taps a creature you control that has a hat (`tapXType` with
  `Creature.HasHat`).
  - **New property `HasHat`:** a hat Forge knows of (a Tagger worn hat, or a "wearing" answer for Goblin Haberdasher)
    or one the controller has pointed out.
  - **Pointing one out:** an honor-system special action (`ClaimHat`, `Card.setHasHatClaim`) says one of your creatures
    has a hat. "Has" is wider than "wearing" (rulings): a held hat counts, as do hat, wig and headband stickers. The
    claim follows the card, like Haberdasher's answer.
  - Hat Trick and Park Re-Entry now read `HasHat` too, so a creature already pointed out isn't asked about again.
  - **AI:** it can't see the art, so it only uses known hats and never makes a claim.
- **Unfinity: Surprise Party (UNF #29).** Printed Clown Robot tokens hidden on the table, built on Entirely Normal
  Armchair's hide-and-point idiom.
  - **User ruling:** tokens can be hidden from the start of the game. A player whose deck has a card with a
    `HidePrintedTokens` SVar starts with a command-zone "Printed Tokens" effect (`GameAction.createPrintedTokenHiding`).
    Its special action (`HidePrintedToken`) hides one behind one of your permanents or in plain sight, one per spot,
    since the tokens can't touch.
  - The tokens aren't game objects yet (`Player.getHiddenPrintedTokens`, `HiddenPrintedToken`).
  - Each opponent gets a "Hidden Tokens" effect: once each turn they point at a spot (`SeekHidden | PrintedTokensOf$`),
    and a token there is spotted.
  - As the Party is cast, a static trigger notes the hidden, unspotted tokens (`PutHiddenTokens | AtCast$`). When it
    enters, "Surprise!" puts any number of those still hidden onto the battlefield, so tokens hidden later or spotted
    earlier don't count.
  - **AI (user ruling):** it hides and seeks. It hides a token at a random spot in its second main phase, up to
    three (`HidePrintedTokenAi`), and points at a random spot once a turn, as it does for the Armchair.
- **Unfinity: Solaflora, Intergalactic Icon (UNF #27).** Her Auras, Equipment, counters and stickers affect your other
  creatures. Those creatures never become enchanted, equipped, countered or stickered themselves (rulings).
  - **Attachments:** new static mode, `ShareAttachments`, takes `ValidAttachment$` and `Affected$`
    (`StaticAbilityShareAttachments`).
    - The `EnchantedBy`, `EquippedBy` and `AttachedBy` card properties count a creature that shares one, and so do
      `Defined$ Enchanted` and `Defined$ Equipped`. That covers attachments' statics, triggers and effects.
    - "Is enchanted" and "is equipped" don't change, matching the ruling about "equipped creatures you control".
    - Only her controller's attachments are shared: an opponent's Pacifism on her stays hers.
  - **Counters:** new counts `Count$CounterPowerBonus` and `CounterToughnessBonus` handle P/T counters. A new
    Continuous param, `GainsCounterKeywordsOfDefined$`, handles keyword counters.
  - **Stickers:** ability stickers use the existing `GainsStickerAbilitiesOfDefined$`. Two new params cover the rest:
    `GainsPTStickersOfDefined$` sets the newest P/T sticker's P/T, and `GainsNameStickersOfDefined$` puts each name
    sticker's word into the other creature's name at the same spot (ruling), filling a blank if it has one.
  - **Known gap:** other counter kinds (stun, shield and so on) aren't shared yet.
- **Unfinity: Park Re-Entry (UNF #22).** It returns up to two target creature cards from your graveyard, each with
  mana value 3 or less or a hat.
  - Mana value 3 or less, or a hat Forge already knows of (`Card.HatInArt`), returns without asking.
  - Any other creature card can be targeted on the caster's word. As the spell resolves, you're asked whether it has
    a hat; if not, it stays in the graveyard, as an illegal target would.
  - "Has" is wider than "wearing" (rulings): a held hat counts, as do hat, wig and headband stickers.
  - **AI:** new RepeatEach logic, `TargetBestCreatures`. It picks the best targets but stays within `AITgts$`, so the
    AI only targets cards it can vouch for without seeing the art: mana value 3 or less, or a known hat.
- **Unfinity: Now You See Me . . . (UNF #20).** A flicker that returns the creature under its owner's control, with
  X +1/+1 counters.
  - X is an honor-system number: you're asked how many mirrors on walls you can see from your seat. The prompt notes
    the rulings (real mirrors only; two facing each other count as two).
  - **AI:** new ChooseNumber logic, `Mirrors`, simulates a room: no mirror half the time, one 35%, two 15%. That
    follows Gimme Five's simulated high fives.
  - The flicker records the exiled card as it resolves (`Imprint$`, Ordinary Pony's approach) rather than when it's
    cast.
- **Unfinity: Main Event Horizon (UNF #19).** The half of the alphabet is chosen as the spell is cast. It's scripted
  as two modes, so the choice is on the stack for everyone to see and a copy keeps it (rulings).
  - New card property `nameInitialIn A-M`. Spaces and punctuation are skipped; accents are dropped and Æ reads as A
    (`NameLetters.latinBase`); a letter from an alphabet without A to Z is in neither half (rulings).
  - Charm's `NotModal$ True` keeps Far Out from adding the other half, since this is an additional-cost choice and
    the spell isn't modal.
  - **AI:** DestroyAllAi picks the half that costs the opponent more.
- **Unfinity: Leading Performance (UNF #18).** Two letter choices, the second into `ChosenType2`.
  - ChooseType's `InvalidTypes$` now accepts `ChosenType`, which takes out the letter already chosen, so the second
    letter must differ (rulings).
  - New card property, `nameStartsWithEitherChosenLetter`. It checks every name a combined creature has, and it's one
    `PutCounterAll`, so a creature gets one counter however many letters match (rulings). Spaces and punctuation are
    skipped (`NameLetters.initial`).
  - **AI:** new ChooseType logic, `OwnCreatureInitials`. It picks the commonest initials among its own creatures, and
    casts only if the two letters reach at least two creatures (`ComputerUtil.mostCommonInitial`).
  - `CountersPutAllAi` now takes `AILogic$ Always`, for a sub-ability whose parent already decided before the letters
    exist.
- **Unfinity: Knight in _____ Armor (UNF #17).** As it enters you may put a name sticker on it (an ETB replacement
  running PutSticker on itself).
  - Its protection uses `Protection:` with a new card property, `NameInitialOfSourceNameSticker`: a name that starts
    with the same letter as any name sticker on the Knight.
  - It uses `NameLetters.initial`, so spaces and punctuation are skipped (ruling): `_____ Goblin` starts with G.
- **Unfinity: Katerina of Myra's Marvels (UNF #16).** You choose a letter as it enters (Monkey Monkey Monkey's
  ChooseType), and each spell you cast whose name begins with that letter makes a white 2/2 flying Cat. It has Partner.
  - **Ruling applied everywhere:** a name now begins with its first letter once spaces and punctuation, blank lines
    included, are skipped. So `_____ Goblin` begins with G, and a name that is only a blank still begins with nothing
    (Stet's ruling).
    - A name whose first letter-or-digit is a digit has no initial letter.
  - That rule lives in `NameLetters.initial`, which `nameStartsWithChosenLetter` (Monkey Monkey Monkey, Katerina) and
    the `MostCommonInitial` AI now use too. The property used to compare the raw first character.
  - **AI:** new letter logic, `MostCommonSpellInitial`, picks the commonest initial among the nonland cards it has yet
    to play.
- **Unfinity: Jetpack Janitor (UNF #15).** Alpha strike is a static that grants first strike through a new card
  property, `AlphaStrike`. It holds while the creature is in combat against a creature (one it blocks, or one blocking
  it) whose displayed name comes later alphabetically.
  - Names are compared case-insensitively, the same way as Zzzyxas's Abyss. A nameless creature (face down, say)
    doesn't come after anything.
  - The AI's combat prediction doesn't foresee the first strike.
- **Unfinity: Impounding Lot-Bot (UNF #14).** Saying "INCARCERATE!" in a robot voice is an optional honor-system
  `FlavorAction` cost, paid when the trigger resolves. The exile is Banisher Priest's, so if the Lot-Bot has already
  left, nothing is exiled (rulings). The AI is a computer, so it always says it. No engine change.
- **Unfinity: Hat Trick (UNF #13).** A hat Forge already knows of counts without asking: one tagged as worn in
  single-figure art, or one the controller already claimed (`Card.HatInArt`, Goblin Haberdasher's).
  - Otherwise the caster is asked. "Has" is wider than "wearing" (rulings): a held hat counts, and so does a hat,
    wig or headband art sticker. Forge's art stickers don't record what they show, so this has to be asked.
  - "You have a hat" is honor system, and it's asked only once the creature has one.
  - Triple strike requires both hats. A creature that already had double strike from elsewhere doesn't count; only
    the one that got double strike from Hat Trick is remembered for this.
  - The AI can't see the art and has no head, so it answers no to both questions.
  - No engine change.
- **Unfinity: Gobsmacked (UNF #11).** Whether you can see the chosen person is the Aura's chosen mode (Visible or
  Hidden), so the card's detail panel shows it; the lock (Arrest's, on any nonland permanent) applies only while Visible.
  - As it enters, you choose a person and say whether you can see them from your seat; the AI picks someone it can see.
  - A human reports the person leaving or coming back with two honor-system special actions (rulings: you don't have
    to keep looking, and it works again when they return).
  - User ruling: the AI's simulated person comes and goes. At each of its upkeeps there's a 10% chance they wander off,
    or a 50% chance an absent one comes back, from one stored roll whose ranges don't overlap. You're notified of
    each change.
  - No engine change.
- **Tests:** 63 new test classes in `forge-gui-desktop/src/test/java/forge/ai/`, one per card or batch:
  `GobsmackedTest`, `HatTrickTest`, `ImpoundingLotBotTest`, `JetpackJanitorTest`, `KaterinaOfMyrasMarvelsTest`,
  `KnightInBlankArmorTest`, `LeadingPerformanceTest`, `MainEventHorizonTest`, `NowYouSeeMeTest`, `ParkReEntryTest`,
  `SolafloraTest`, `SurprisePartyTest`, `TapperTest`, `TrapezeArtistTest`, `AnimateObjectTest`, `AstroquariumTest`,
  `BagCheckTest`, `BlufferfishTest`, `BustedTest`, `DecisionsDecisionsTest`, `FocusedFunambulistTest`,
  `ParThreeTest`, `OctoOpusTest`, `PhoneAFriendTest`, `PlateSpinningTest`, `AnimateGraveyardTest`,
  `MobileCloneTest`, `UnfinityBlackBatchTest`, `UnfinityBlackBatch2Test`, `UnfinityBlueBlackBatchTest`,
  `AmpedUpTest`, `UnfinityBatch3Test`, `UnfinityRedBatchTest`, `UnfinityRedBatch2Test`, `UnfinityRedBatch3Test`,
  `UnfinityRedGreenBatchTest`, `VorthosStewardOfMythTest`, `HardyOfMyrasMarvelsTest`, `AnIncidentHasOccurredTest`,
  `IcingManipulatorTest`, `JermanePrideOfTheCircusTest`, `UnfinityGreenBatchTest`, `SolePerformerTest`,
  `TchotchkeElementalTest`, `SpellingBeeTest`, `TugOfWarTest`, `ClaireDLoonTest`, `UnfinityWhiteBatchTest`,
  `LilaHospitalityHostessTest`, `UnfinityMulticolorBatchTest`, `D00DLCaricaturistTest`, `ParkMapTest`,
  `SouvenirTShirtTest`, `GreatestShowInTheMultiverseTest`, `WickerPickerTest`, `UnfinityAttractionsTest`,
  `UnfinityAttractions2Test`, `UnfinityAttractions3Test`, `UrzasFunHouseTest`, `GrandMarshalMacieTest`,
  `LogFlumeTest`, `ItCameFromPlanetGlurgTest`, `TrussChiefEngineerTest`.

### 2026-10-20, eleventh deploy (desktop, Android, GitHub) — Heroes of the Realm 2019–2021; Unfinity #1–#10 begun; Universes and artifact-creature art data

- **Build:** whole-reactor `android-debug` install; APK versionCode 2026102102. Tests before the build: 1498 run, 0 failed, 7 skipped. No upstream merge this time.
- **Unfinity: Get Your Head in the Game (UNF #10).** The ETB exile lasts until it leaves (`Duration$
  UntilHostLeavesPlay`), and the card is then "touching your head" (SVar `OnHead`).
  - User ruling: a human reports when contact breaks with an honor-system special action, and the AI's grip slips
    with a 5% chance at each of its upkeeps. That needed a new player property, `IsAI`.
  - The slip is rolled once and stored, because an ImmediateTrigger copies its own ability's restrictions onto the
    trigger it makes. For the same reason, the honor action clears `OnHead` before its ImmediateTrigger runs.
  - **AI fix:** `AILogic$ Never` is now honored by ImmediateTriggerAi and StoreSVarAi; both used to ignore it, so the
    AI could have pressed a human's honor-system button. That also covers Form of the Approach of the Second Sun's.
- **Unfinity: Form of the Approach of the Second Sun (UNF #9).**
  - **The card you become:** new `GamePieceType.PERSON`. Per the rulings it's a planeswalker card with no other
    characteristics (no name), made by the new effect `BecomeACard` (`LibraryPosition$`; a player who's already a
    card is moved).
  - **When it ends:** it exists only in its owner's library. `GameAction` ends it when it would go anywhere else,
    and it's still seen as drawn, so "when you draw yourself" works while tutoring yourself just ends it.
    `Player.shuffle` ends it too.
  - **New properties:** `YourselfAsACard`, and `OnYourHead` (the cards above you in your library).
  - **Falling:** new trigger `CardsFellOffHead`, fired by `HeadBalance`. User rulings: a human reports a fall with an
    honor-system special action, and the AI has no head, so its stack wobbles. Each time cards go on top of it, and
    once when it first balances them, there's a 2%-per-card chance (at most 50%) that they fall.
  - **Engine fix:** a planeswalker with no printed loyalty no longer gets the "enters with loyalty counters"
    replacement, which used to crash on it.
- **Unfinity: Far Out (UNF #8).** New static mode `ChooseAnyNumberOfModes` (`ValidPlayer$`,
  `StaticAbilityChooseAnyNumberOfModes`).
  - When it applies, CharmEffect offers its controller one or more different modes in place of the indicated
    number. That means no repeats even on `CanRepeatModes` spells, and only modes with legal targets.
  - It's asked only when that changes anything (`CharmEffect.canChooseAnyNumberOfModes`), and only when the
    spell's controller is the one choosing.
  - A random modal spell stays random, and its controller picks only how many modes (rulings).
  - Pawprint spells indicate a total, not a number of modes, so they're left alone.
  - The AI always takes it, and CharmAi picks every mode it wants.
  - Known gap: Outlaws' Merriment's ruling combines all chosen modes into one token; Forge makes one token per mode.
- **Unfinity: Bar Entry (UNF #4).** Nothing records where heads are in card art, so the head-above-the-bar mode can
  target any creature and the caster checks the two cards side by side (honor system, like Sly Spy's unknown facings).
  The AI can't look, so that mode has `AILogic$ Never` and the AI only uses the power-4 mode.
- **Unfinity: Assembled Ensemble (UNF #3).** New PrintingTraits trait `artifactcreature`, which gives
  `Card.hasKnownArtifactCreatureInArt()` and the property `KnownArtifactCreatureInArt`.
  - Tagger has no artifact-creature tag, so these count: the robot, construct, golem, thopter, servo, automaton,
    juggernaut and animated-object tags, plus any art that is an artifact creature card's own (Dancing Scimitar,
    which the rulings name).
  - Gargoyle, scarecrow, cyborg, clockwork, vehicle and homunculus art counts only when it's an artifact creature's
    own (user ruling). Darksteel Gargoyle counts; Abbey Gargoyles, a scarecrow in a field and Vehicles don't.
  - Untagged art is asked about, as with Acornelia, and the AI answers no.
  - **`generate.js` fix:** Forge numbers some printings with an F that Scryfall doesn't have (Unfinity's acorn cards
    are F3 where Scryfall says 3; Mystery Booster 2's playtest cards too). Those had never been matched, so they had
    no printing traits at all. They're now matched by number when the card name agrees: 266 more rows.
- **Unfinity: Standard Procedure (UNF #1).** Its rulings say it isn't a copy effect: it *becomes* the chosen card
  until end of turn even once cast or shuffled away.
  - New Clone `FollowsCard$ True` adds a `PerpetualClone` (forge.game.card.perpetual). Every zone change rebuilds
    the card from its printing, and this lays the clone on again each time. The duration's end removes it from
    whatever object the card is by then (`game.getCardState`).
  - `CloneZone$ Hand` covers "only if still in hand when it resolves".
  - NameCard `LegalIn$ Standard` limits the names to Forge's current Standard (`GameFormat.currentStandard()`,
    registered by FModel like the PrintingTraits source; `GameFormat.getLegalFrontFaceNames()` leaves out split cards).
  - `CardFacePredicates` valid strings now take every mana-value comparison (`cmcLE3`, `cmcGE8`...), not just `cmcEQ`.
    Until now anything else silently matched nothing. No existing NameCard script used one.
  - AI (`SpecialCardAi.StandardProcedure`): activates in its own main phase only if, with the mana left after {1},
    its own casting logic would cast some Standard instant or sorcery with mana value 3 or less. It names the most
    expensive such spell, and checks once per turn and mana level.
- **2021 Heroes of the Realm: Byode, Inverse Sun.** New card type **Universewalker**
  (`CoreType.Universewalker`, a permanent type; subtype list `[UniversewalkerTypes]` in TypeLists.txt). It isn't a
  planeswalker: loyalty is a planeswalker-only characteristic (CR 306.5), so Byode enters with no counters and isn't
  put into the graveyard at 0, but its `Planeswalker$ True` abilities follow 606 (once a turn, sorcery speed).
  - **Universes** (user ruling): a Universes Beyond printing's umbrella franchise (all Marvel is one Universe, The
    Hobbit and The Lord of the Rings are Middle-earth, Warhammer 40,000 / Age of Sigmar / Blood Bowl are Warhammer),
    and every other printing is the Magic Universe. Byode has the Universes Beyond stamp but no franchise, so it's a
    Universe of its own.
  - `_tools/universes/resolve.py` maps all 10,865 non-token Universes Beyond printings (Scryfall promo type
    `universesbeyond`) to 50 Universes. Its sources, in order: the franchise's set and its parent sets, MTGJSON
    Secret Lair drop names (`subsets`), the names of the MTGJSON sealed products a card comes in, then other
    printings of the same card. The 72 printings with no data source are listed in `MANUAL` with the evidence
    for each, taken from flavor names, flavor text, and the copyright lines on the card images.
  - `printing-traits/generate.js` writes the result as the PrintingTraits trait `universe=<name>`. That gives
    `PrintingTraits.universeOf` / `allUniverses` / `isUniversesBeyond`, `Card.getUniverse()`, and the property
    `ChosenUniverse`.
  - ChooseType `Type$ Universe` offers Magic plus every franchise. With `Note$` it handles "you haven't chosen this
    way this game", and choosing from nothing is allowed. ChangeZone `DifferentUniverses$ True` adds a
    one-at-a-time filter alongside `DifferentNames`.
  - New commander deck rule `DeckRule:ColorIdentity:ExemptUniversesBeyond$ True`. It's a printing-level exemption
    (`DeckRuleColorIdentity.allowsOffColorIdentity(PaperCard)`), used by both deck checks and the editor's legal-card
    filter.
  - AI: `ChooseTypeAi` `AILogic$ Byode` activates −X only for a spell in hand from an unchosen Universe, with X equal
    to its mana value. `ComputerUtil.chooseSomeType("Universe")` then names that spell's Universe.
- **2020 Heroes of the Realm: The Secret Lair.** "Say the secret word" is a `FlavorAction` cost, because the card never
  names the word. The ability that uses it is still a mana ability, since nothing in it targets (605.1a).
- **The List audit against Scryfall (2026-10-20).** PLST matched Scryfall's 5,740 printings by collector number,
  names and artists, except three Judgment † variations, now added (JUD-78†, JUD-119†, JUD-127†). Not discrepancies:
  basic lands are `L` where Scryfall says common (Forge's convention), the Wicked // Cursed role token is two token
  lines under TWOE-17 (as in WOE), and TDFT-14 Start Your Engines! // Max Speed is a helper card Forge doesn't model
  (speed is player state). New edition **The List (Unfinity Foil Edition)** (ULST, 62 printings, fork-owned, under the
  `edition-add-only` merge driver); its Ineffable Blessings are variants C and A and its Everythingamajigs A and F,
  matched to Unstable by Oracle text. PrintingTraits regenerated for the new printings, with their printed lines
  OCR'd; two dropped opening parentheses corrected by hand from the card images (ULST 23 Over My Dead Bodies, UST 135
  Phoebe, Head of S.N.E.A.K.).

- **2019 Heroes of the Realm: Collectigull // Only the Best, Keeper of the Secret Lair, Myntasha, Honored One.**
  Booster Fun is Scryfall's `boosterfun` promo type, now a PrintingTraits trait (`boosterfun`, 9,272 Forge
  printings; `generate.js` writes it), read by `Card.isBoosterFun()` / property `BoosterFun`. A Secret Lair spell is
  one printed in an edition whose name starts with "Secret Lair" (SLD and the four products Scryfall files under it):
  `Card.isSecretLair()` / property `SecretLair`. Protection from common is `K:Protection:Card.RarityCommon:common`.
  Myntasha: new zone `ZoneType.BoosterPile` (made for every player like the whammy deck, outside `ALL_ZONES`, a
  deck zone so random-order moves to it shuffle), new cost `OpenBooster<1>` (`CostOpenBooster`: choose a set that
  makes boosters, a real pack from its template goes to the bottom of the pile in random order; AI picks a random
  set), and keyword `Booster cascade` (cascade with `DigZone$ BoosterPile`, the rest back to the pile's bottom).
  `OpenBoosterEffect.boosterSetCodes()` / `openPack()` are shared with Stocking Tiger's ChooseExpansion.
- **Interplanar Brushwagg: the interplanar zone leaves the screen when empty, and replays no longer freeze.** Once
  nothing is left on the interplanar battlefield (checked with state-based actions) `Game.retireEmptyInterplanarZone`
  fires new `GameEventPlayerRemoved`, and both UIs take its place off the screen (`IGuiGame.removePlayerView`: mobile
  `MatchScreen.removePlayerPanel`, desktop `CMatchUI.removePlayerView` taking the field and hand out of their cells
  the way `register()` clears unused docs); it comes back if something interplanar enters again. The replay freeze
  (phone log: `Could not resolve IdRef(tag=1, id=1000)` then an NPE in `MatchController.getPlayerAvatar`): the
  stand-in player is in no player list, so state sync - what replays and netplay clients get - never sent it, and the
  recorded "player added" event named a player the client didn't have. New GameView property `UnseatedPlayers`
  (appended last, so recorded property numbers don't shift) makes the delta walk reach it and its battlefield
  (`DeltaSyncManager.preScanZoneCollections` covers it too); `FControlGameEventHandler` skips add/remove events for a
  player the client doesn't know, so replays recorded before this fix play on (without the interplanar field).
- **Throw animations for the funny cards that throw** (upstream's mobile Chaos Orb / Falling Star animation, #12183).
  `GameEventFlipOntoBattlefield` gained a `TossStyle` (5-arg constructor kept for upstream calls); the mobile
  `FlipOntoBattlefieldAnimation` draws a torn quarter of the card (Chaos Confetti, FlipOntoBattlefield `Pieces$`), a
  die showing its result landing on exactly what DiceLanding touched (Ol' Buzzbark, one throw per die), the dropped
  land itself (Landfill, `FlipCard$ Imprinted`), or the card tossed up and back onto its own spot, face down for a
  bad drop (new API `TossCard` - Orcish Paratroopers, Clay Pigeon). Slaying Mantis already used it. Desktop has no
  throw animation upstream either. Fixes found on the way: a throw onto an empty table is still shown (it returned
  early); Chaos Confetti's pieces can no longer hit Chaos Confetti itself (it destroyed itself mid-throws); Landfill's
  AI picks the land type it has most of (`AILogic$ MostProminentComputerControls` for land types; it always chose
  Island).
  - Script changes for the styles: Chaos Confetti's throw is `Pieces$ True`; Landfill repeats with
    `UseImprinted$ True` so each throw is the dropped land (`FlipCard$ Imprinted`), and now clears its remembered
    cards at the end; Clay Pigeon tosses itself (`TossCard`, 2 flips) before the "did you catch it" question;
    Orcish Paratroopers' clean landing is a 1-flip toss face up, the bad drop a 0-flip toss landing face down,
    then the sacrifice. `IGuiGame.showFlipOntoBattlefield` gained a `TossStyle` overload that falls back to the
    plain card throw, so desktop and other GUIs are unaffected.
- **Fix: `TossStyle` is serializable.** Game events travel in replays and netplay, and
  `GameEventSerializationTest` caught the new record not implementing `java.io.Serializable`.
- **Tests:** new GetYourHeadInTheGameTest, FormOfTheApproachTest, FarOutTest, AssembledEnsembleTest,
  StandardProcedureTest, HeroesOfTheRealm2021Test (Byode), TheListAuditTest, HeroesOfTheRealm2019Test and
  ThrowAnimationEventsTest; InterplanarBrushwaggTest extended for the zone leaving the screen. (Bar Entry and The
  Secret Lair are script-only, with no test of their own.)

### 2026-10-20, tenth deploy (desktop, Android, GitHub) — Unsanctioned complete; 22-commit upstream merge

- **Upstream merge** (22 commits, 85888fdfa0a): 11 card-script conflicts resolved keeping the fork's Scryfall wording
  and taking upstream's fixes; Oracle lines re-synced to Scryfall 2026-10-20 (26 cards: Alchemy "perpetually gains
  prismatic" rewording, spark costs as "[+1]:"). Tests: 1454 run, 0 failed, 7 skipped.
  - Conflicts resolved (per the merge message): Bog Badger's stray Flash, Genestealer Patriarch's P/T, Stronghold
    Arena's stray P/T, Tarnation Vista's types, Needle Drop's targets, and upstream's Charm `Defined$` cleanup. The
    Java side merged without conflicts.
  - What upstream brought: assimilate (`AssimilateEffect`/`AssimilateAi`) and Borg Queen, Perfection Manifest;
    Captain Jean-Luc Picard; Fluros of Myra's Marvels; card fixes (#12325–#12331: "up to one target" abilities
    allow zero targets, printed P/T, loyalty, mana cost, types and keywords of 18 cards, Roadside Blowout / Venom
    Blast / Enroot as sorceries, Kutzil's Flanker, Sarkhan's Dragonfire, Twigwalker, Resounding Wave, Storm the
    Citadel, Needle Drop, Toralf, Blast from the Past, Dream Beavers, Tresserhorn's Lord); CantPutCounter with
    simultaneous ETB (#12301); an endless loop fixed in ChooseSourceEffect when sources run out, with
    `TargetControls$` removed (#12083, new ChooseSourceEffectShould test); Charm scripts dropping an unread
    `Defined$ You` (#12312); Magik's TrigDigUntil SVar fix; quest worlds for The Hobbit and Reality Fracture; two
    draft cubes (Arena Powered Cube v7.0 2026-10, MTGO Vintage Cube 2026-10); puzzle PS_FRA2; an adventure map fix;
    mobile start button, theme and splash-screen logo updates.
- **Imaginary Friends (Plane) AI.** No strategy, just random: from hand it's offered the first three cards that can
  be cast right now from a random order of the choices its deck builder isn't told to leave out (`AI:RemoveDeck:All`),
  kept for the turn, as ordinary cast-as spells its usual logic weighs. For the chaos free cast, `PlayAi.chooseCardName`
  tries the choices in random order (up to 30) and takes the first whose spell it would cast.
  - From hand it looks at most 15 names down that per-turn order to find its three
    (`StaticAbilityCastAs.aiPlayableChoices`); the chaos free cast falls back to the first allowed name if it
    wouldn't cast any of the 30 it tried.
- **Flavor Judge (UND #4).** Checked against Scryfall and its 2 rulings 2026-10-20. Honor system for a human (they ask
  a real person and enter the answer); the AI's simulated person says no half the time (user ruling). GenericChoice
  `AILogic$ FlavorJudge` targets the top opponent spell or ability that targets an AI permanent. A targeted
  GenericChoice needs `Defined$ You`, or it looks for its chooser among the targets and silently does nothing.
- **Strutting Turkey (UND #14).** Checked against Scryfall and its 2 rulings 2026-10-20. Script plus Augment
  `AugmentCard$ <defined>`: a defined card combines instead of the ability's host (here the remembered exiled card,
  `FromZone$ Exile`), looked up by its current state since Remembered holds the pre-move object.
- **Syr Cadian, Knight Owl (UND #15).** Checked against Scryfall and its 5 rulings 2026-10-20. Knightlifelink = new
  static `Mode$ LifelinkDamage | ValidSource$` (damage also gains its controller life, without granting the keyword;
  a source with lifelink gains once). Real sunrise/sunset: `forge.util.SunTimes` (NOAA general solar position
  equations) at the reference city of the computer's time zone (user ruling), from new
  `res/lists/TimeZoneCoordinates.txt` (IANA tzdata 2026e zone1970.tab + zone.tab + backward links, public domain;
  `_tools/time-zone-coordinates/generate.js`); zones without a city fall back to 6 AM - 6 PM. Counts
  `Count$RealWorldSunUp` / `RealWorldSunDown`, both true on the exact minute (rulings); clock swappable via
  `AbilityUtils.realWorldNow`.
- **Alexander Clamilton (UND #16).** Checked against Scryfall and its 6 rulings 2026-10-20. Count
  `Card$CardRulesLines` (printed rules lines, -1 unread); when the revealed card couldn't be read the activator counts
  (ChooseNumber; AI `RememberedRulesLines` = one per Oracle paragraph with rules text, a floor it can stand behind).
  PeekAndRevealAi `FightAfterReveal`: assumes 2 lines unless it can see the top card, fights only what it kills and
  survives.
- **Rules lines no longer count lines that are only reminder text** (CR 207.2; Clamilton's rulings; the Unstable FAQ
  on Kindly Cognician). `rulelines=` in PrintingTraits.txt regenerated from the stored OCR rows: a line counts if any
  word sits outside parentheses (full-width ones too); an unclosed "(" is ignored, a stray ")" leaves earlier lines
  counted. 18,027 printings changed; wordy (Frazzled Editor, Garbage Elemental) now matches fewer cards, e.g.
  Garbage Elemental B and Paradise Druid aren't wordy. Lexivore's `textlines=` is unchanged.
- **Rings a Bell (UND #27).** Checked against Scryfall and its 3 rulings 2026-10-20. Saying the word = special action
  `ST$ RingBell` (once a turn); opponents share one real five-second window (`PlayerController.ringBellInTime`, human
  via `chooseStringInTime`, AI rings `AIChance$` 75% - user ruling; AI listeners answer first), whoever rang is
  remembered, and a reflexive trigger draws when nobody did. The word: ChooseType `MinLetters$ 4`, AI from Words.txt.
- **Fix: `ActivationLimit$` on special actions never applied.** `MagicStack.add` resolved an `AbilityStatic` without
  counting the use, so a limited special action could be taken again and again (the hidden armchair's once-a-turn
  guess included). Now counted on the card when the action has an activation limit.
- **Acornelia, Fashionable Filcher (UND #31).** Checked against Scryfall and its 2 rulings 2026-10-20. New printing
  traits `squirrel` (Tagger's squirrel tree + secret-squirrel; 411 printings, every Squirrel creature's art among
  them) and `untaggedart` (art Tagger hasn't tagged at all, 52 printings); properties `KnownSquirrelInArt` and
  `ArtUntagged` (also true with no printing data). Only untagged art asks the caster (honor; AI says no). {A} =
  player ACORN counters, `SubCounterYou<X/ACORN>` + `Announce$ X`. New drawn {A} acorn symbol on desktop
  (`CardFaceSymbols.acornSymbol`, symbol cache A.png) and mobile (`FDrawnSymbol.acorn()`).
- **Bat- (UND #32).** Checked against Scryfall and its 2 rulings 2026-10-20. Script only (augment condition on
  `PlayerCountRegisteredOpponents$HighestLifeLostThisTurn`, so an opponent who left the game still counts).
- **Infernius Spawnington III, Esq. (UND #40).** Checked against Scryfall and its ruling 2026-10-20. Cards revealed
  this turn: `GameAction.reveal` records them on the revealing player (`Player.addRevealedThisTurn`, a per-turn set,
  so one card counts once a turn; cleared at cleanup), count `Count$YouRevealedThisTurn`. Showing cards that isn't
  the reveal keyword action now goes through new `GameAction.revealForDisplay`: command-zone cards going to hand,
  transformed-card notices, the suspend notice and discover's exiled cards. New creature type Grandchild.
- **Abstract Iguanart (UND #47).** Checked against Scryfall and its 4 rulings 2026-10-20. API `NoteArtistInitials`:
  the first character of each artist the spell credits (`ArtistCredit.individuals`), kept as the card's noted values,
  a +1/+1 counter per new letter. New creature type Art.
- **Boomstacker (UND #49).** Checked against Scryfall and its 5 rulings 2026-10-20. Simulated dice stack (user
  ruling): `Card.stackedDice` (dice, not counters; shown in the card details), API `StackDice`, count
  `Count$CardStackedDice`, trigger mode `DiceStackFell`. `forge.game.card.DiceStack` odds: a die placed topples the
  stack with 0.006 x height², each tap or untap 0.01 x height² (2-die stack: 2.4% / 4%; 8 dice: 38% / 64%; capped 95%);
  a fall leaves a random 0..height-1 dice standing and a die that knocks it over isn't added.
- **Stet, Draconic Proofreader (UND #57).** Checked against Scryfall and its 2 rulings 2026-10-20. API
  `DeleteFirstLetter` (until end of turn: a permanent gets a timestamped name change, a player's in-game name changes
  and is restored at end of turn - `Player.deleteFirstLetterOfName`/`restoreDeletedName`); `forge.game.card.NameLetters`;
  card and player property `NameInitialOf <Defined>` (a nameless thing, or one beginning with a non-letter, begins
  with no letter). Reflexive trigger objects are `DelayTriggerRemembered`, not `Remembered`. ChangeZoneAi
  `StetInitial`: exile the card whose initial lets 4 damage kill the best opposing creature/planeswalker, else hit an
  opponent; declines otherwise.
- **Pippa, Duchess of Dice (UND #68).** Checked against Scryfall and its 4 rulings 2026-10-20. Dice as permanents:
  `Card.dieFace`/`dieSides` (shown in the card details), Token `DieFace$`/`DieSides$`, token script `g_x_x_die` (CDA
  P/T = `Count$CardDieFace`), property `IsDie`, creature type Die. "Reroll any die": API `RerollDie` for a Die on the
  battlefield (RerollDieAi: own below-average die, opponent's above-average one), and static `RerollAnyDie | Cost$`
  offered after any player's roll (`RollDiceEffect.payToRerollAnyDie`, Wall of Fortune's AI logic); the reroller rolls,
  and their "whenever you roll" triggers fire (ruling). `PlayerController.payRerollCost` (AI really pays; upstream's
  `payCostDuringRoll` is left as it was).
- **Spirit of the Season (UND #70).** Checked against Scryfall and its 3 rulings 2026-10-20. Real astronomical
  seasons (user ruling): `forge.util.Seasons` from the Sun's apparent ecliptic longitude (Astronomical Almanac
  low-precision formula; tested within 30 minutes of the USNO's 2026 equinoxes and solstices), swapped south of the
  equator by the time zone city's latitude (`SunTimes.coordinatesOf`, now public). `Count$RealWorldSeason` (1 spring
  ... 4 winter), clock `AbilityUtils.realWorldNow`.
- **Surgeon General Commander (UND #72).** Checked against Scryfall and its 2 rulings 2026-10-20 (five-color
  identity from its mana ability - tested). New trigger mode `Augments` (run by AugmentEffect with Card + Player);
  `Mutates` now also passes the mutating player (`ValidPlayer$`). "Enchant" is the verb: an Aura you control becoming
  attached to a creature you control (`Mode$ Attached`). New creature type Chameleon.
- **Underdome (UND #86).** Checked against Scryfall and its ruling 2026-10-20. Script only: `RestrictValid$
  SpellAbility.SilverBordered` (spells and every kind of ability of a silver-bordered or acorn card). Known gap: Forge
  never lets restricted mana pay a cost demanded while something resolves (unless costs), so neither can this.
  **Unsanctioned is complete** (und/75 Who // What // When // Where // Why was already scripted from Unhinged).
- **Tests:** new FlavorJudgeTest, StruttingTurkeyTest, SyrCadianTest, AlexanderClamiltonTest, RingsABellTest,
  AcorneliaTest, BatAugmentTest, InfernusSpawningtonTest, AbstractIguanartTest, BoomstackerTest, StetTest,
  PippaTest, SpiritOfTheSeasonTest, SurgeonGeneralCommanderTest and UnderdomeTest; SecretLairShowcasePlanesTest
  gained the Imaginary Friends AI cases (three random picks held all turn; the chaos card cast as another and back
  to itself after); GarbageElementalTest moved to Relentless Rats and Garbage Elemental D/B for the new line count.

### 2026-10-19, ninth deploy (desktop, Android, GitHub) — Secret Lair Showcase Planes; info panel fix

- **Secret Lair Showcase Planes (PSSC)**, all ten (the edition file already listed them; none had scripts, so the
  Deck Editor's Planes section never showed them). Checked against Scryfall 2026-10-19; none have rulings. New
  "Secret Lair" planar type. Engine additions:
  - `CastAs$ AnotherCard` (Imaginary Friends): one stand-in spell per card in hand; choosing it asks which card -
    legal in the game's format (a Commander variant's, else the game type's), other name, same card types and mana
    cost, split cards left out - and that card's spell is cast (`StaticAbilityCastAs.chooseAnotherCard`, from
    `PlaySpellAbility`). The AI isn't offered it. `Play | CastAsAnother$ True` for the chaos ability's free cast.
  - `ChangeTypeWordFrom$`/`ChangeTypeWordTo$` continuous text change (Deceptive Divination); `WordChangedType` now
    swaps a card type word (Sorcery to Instant) on the type line, not only subtypes, and the existing text-change
    path rewrites the card's abilities ("instant or sorcery" becomes "instant or instant").
  - `Count$CardPrintedPower`/`CardPrintedToughness` (Finally!): the original state's values, before copy effects.
  - `Count$YouRollPlanarDieThisTurn` (Li'l Giri, sAnS mERcY): every planar die roll by that player, including
    Wall of Fortune rerolls and rolls from other effects, not only the {X} special action.
  - `Draw | FromBottom$ True` (Finally!).
  - The ten planes: Deceptive Divination; Fblthp: Completely, Utterly, Totally Lost; Finally! Left-Handed Magic
    Cards; Happy Yargle Day!; Imaginary Friends (Plane); Li'l Giri Saves the Day; Math is for Blockers (Plane);
    Mycosynthwave; sAnS mERcY; We Hope You Like Squirrels. New token `lil_giri` (Li'l Giri, a legendary 1/1
    Food Warrior artifact creature).
  - Planar die rolls are counted per player per turn (`Player.rollPlanarDie`, reset with the turn's other roll
    count) in `PlanarDice.roll`, Wall of Fortune rerolls included; the draw-from-bottom flag is set only while the
    `FromBottom$` draw runs (`Player.setDrawingFromBottom`).
- **Info panel**: "(Covered: )" and "(Written: )" no longer show on every card - unset string view properties read
  back as "", not null.
- **Tests:** `SecretLairShowcasePlanesTest` (11 tests).

### 2026-10-19, eighth deploy (desktop, Android, GitHub) — upstream merge (26 commits)

- **Upstream merge** (26 commits, incl. the AI timeout tracker, mobile compact landscape layout, local replay,
  software sound mixing, Karn Liberated restart keeping a commander castable). Conflicts:
  - `AiAttackController.declareAttackers` now runs inside upstream's `AITimeoutTracker` try/finally (whole body
    re-indented); the fork's battle fallback, Lexivore check, battle rotation and the tasty/alone-evasion/support-only
    pruning were re-applied inside it.
  - Mobile settings: the fork's `UI_ZONE_TABS_LEFT` sits beside upstream's `UI_COMPACT_*` prefs. It only affects the
    non-compact landscape layout; upstream's compact layout (default "Left") lays out its own tab column.
  - `TrackableProperty` (`HasPrintedPT`/`AugmentPT`) and `VAvatar` imports kept from both sides.

### 2026-10-15, seventh deploy (desktop, Android, GitHub) — upstream merge (4 commits); Mystery Booster playtest cards #95–119 (CMB1 complete); nontoken filter fixes

- **Upstream merge** (4 commits, incl. Adventure-spell offers for Bilbo and nine more, Play effects' type filters
  moved from `Valid$` to `ValidSA$`). Conflicts: Jeleva, Ken, Skyway Robber and Tetsuo take upstream's filters and keep
  the fork's Scryfall-checked wording. Upstream inlined `MagicStack.push()` into `add()`; it stays as a private
  helper for Stack of Paperwork's `addCombatDamage`.
- **Louvaq, the Aberrant** (CMB1 #95). Property `ModifiedFromPrinted` (`Card.isModifiedFromPrinted`), its reminder
  text's "modified": a power/toughness counter, any boost or set-P/T effect (not one that only defines *), gained or
  lost abilities or changed text, or a copy whose P/T or abilities differ from the printed card - not a change of
  controller, color or type, a face-down 2/2, or a card's other faces (its rulings). Not the NEO "modified".
- **P-Pick Your Poison** (CMB1 #97; P- prefix since MKM has a Pick Your Poison). Charm `PawprintExact$ True`: the
  modes' `Pawprint$` values must total exactly `Pawprint$`, shown as "[n]". The human chooser only offers a mode that
  leaves the rest fillable; CharmAi fills the total greedily from the costliest mode it wants (the -2/-2 against a
  board of x/2s, otherwise Snakes and counters). Modes resolve in printed order (its ruling).
- **Seek Bolas's Counsel** (CMB1 #98). Planeswalk `PlaneName$`: reaches a named plane even outside Planechase; that
  plane isn't a real plane card (SVar `NotAPlaneCard`) and stops existing when it's left. Planes reached that way pass
  to each active player like Planechase planes; outside Planechase a Planeswalk roll (Fractured Powerstone) leaves
  the plane with nowhere new to go, and `Player.planeswalk` with an empty planar deck does nothing. Random charms
  read "Choose one at random —" before their modes.
- **P-Smelt // Herd // P-Saw** (CMB1 #100). A three-face split card on the Who // What // When // Where // Why
  support; the Smelt and Saw faces take the P- prefix because Forge indexes cards by face name and both would shadow
  the real Smelt and Saw. New creature type Half; tokens `g_2_2_elk`, `r_1_2_half`.
- **P-Start // P-Fire** (CMB1 #101). Script only; both faces take the P- prefix since Start // Finish and
  Fire // Ice already own those face names.
- **Zyym, Mesmeric Lord** (CMB1 #104). New API `RevealUntilStop`: the target player orders their hand (an AI
  shuffles it, so no pattern can be read), reveals one card at a time and waits for the activator's stop, then
  discards the last card revealed. The AI stops as in the secretary problem: let the first n/e cards go by, then take
  the first nonland better than all of them.
- **Chronobot** (CMB1 #105). Static mode `SwitchUpkeep` on an effect lasting while it's on the battlefield. As each
  upkeep begins, `PhaseHandler.beginSwitchedUpkeep` follows the turn player's upkeep through the switches in
  timestamp order (the same pair twice switches back) and makes that step's player active, reusing the stolen-phase
  hand-off. Unlike a stolen phase, `isPlayerTurn` still answers with the real turn's player (its rulings); untap and
  draw steps don't move, and one entering or leaving mid-upkeep doesn't change that upkeep.
- **Pithing Spyglass** (CMB1 #108). ChooseType `Type$ KeywordOrAbilityWord` (Legacy's word list; the AI names the
  word on the most of its opponents' known cards, net of its own) and continuous `LoseKeywordOrAbilityWord$`: the
  keyword is removed by what it is (`Card.keywordMatchesWord`, so Flash isn't Flashback) and an ability word takes
  the abilities it introduces (`Card.traitHasAbilityWord`). Every zone, cards only (`!token`).
- **Fixed "nontoken" filters** on Very Cryptic Command (turn over target nontoken creature), Time Machine and
  Graveyard Busybody: `nonToken` reads as "doesn't have the card type Token", which tokens pass too; now `!token`.
  `NonTokenFilterTest` keeps it out of every script.
- **Weaponized Scrap** (CMB1 #111). `K:Upgrade` with a move-to-battlefield replacement running new API `Upgrade`:
  it covers another artifact you control through mutate's merge plumbing (the artifact carries on - tapped, counters,
  no enters triggers) or is exiled if it can't. `CardFactory.getUpgradedCloneStates`: only the top card counts, plus
  haste. The AI covers a token or its weakest artifact and won't cast it with nothing to cover.
- **Barry's Land** (CMB1 #113). Script only: a basic land with the new land type Cloud, which has no mana ability of
  its own (the card's text gives {C}).
- **Gold Mine** (CMB1 #116). Nodes: `SVar:Nodes:5` on the card, marks on the physical card as PaperCard flag `nodes=`,
  written into the deck file like a legacy word (`Match.writeMarkedNodes`); new API `MarkNode`, `Count$UnmarkedNodes`.
  A token or another card copying it has no nodes, so its cost can't be paid (its rulings). Shown as "(Marked nodes:
  1 of 5)".
- **Rift** (CMB1 #119), the last unscripted Mystery Booster playtest card. Pre-game `MayEffectFromOpeningDeck` puts it in
  hand; whatever is in hand as the opening hand is drawn is recorded (`Player.setOpeningHandStarters`) and takes the
  place of a draw for the opening hand and each mulligan, and mulligans don't shuffle it back (its rulings).
- **Pithing Spyglass UI:** the human's prompt reads "Name a keyword ability or ability word", and card detail shows
  the named word as "(named: ...)" rather than "(chosen type: ...)". Zyym's AI target orders its hand by shuffling
  (`PlayerControllerAi.orderMoveToZoneList`) so its reveal order carries no pattern.
- **Edition file:** Mystery Booster Playtest Cards #100 and #101 renamed to `P-Smelt // Herd // P-Saw` and
  `P-Start // P-Fire` to match the scripts.
- **Tests:** `LouvaqTest`, `PickYourPoisonTest`, `SeekBolassCounselTest`, `SmeltHerdSawTest`, `StartFireTest`,
  `ZyymTest`, `ChronobotTest`, `PithingSpyglassTest`, `NonTokenFilterTest`, `WeaponizedScrapTest`, `BarrysLandTest`,
  `GoldMineTest`, `RiftTest`. Test fixes: `SummonThePackTest` no longer fails when a random pick is a card that
  stops being a creature by its own text (Arixmethes, a land while it slumbers) - it checks the printed type, and the
  Zombie type only while it's a creature; `SeekBolassCounselTest` counts the chosen modes in the sub-ability chain,
  since a mode can carry sub-abilities of its own.

### 2026-10-15, sixth deploy (desktop, Android, GitHub) — upstream merge (5 commits); Mystery Booster playtest cards #67–94; Innkeeper's Talent AI

- **Upstream merge** (5 commits, incl. Realm of Legends 1.077, restored alternate landscape layout on mobile,
  targetable stack). Conflicts: Cavalier of Gales and Riverwise Augur take upstream's `Reorder$ True` and keep the
  fork's text; mobile Settings keeps both the fork's "zone tabs on the left" option and upstream's restored
  "Alternate Zone Layout" (a wide bottom row) - they're separate options.
- **AI: creatures that can't attack or block are useless** (reported with Innkeeper's Talent). `isUselessCreature`
  only knew the old "can't attack or block" keyword, but Pacifism and its kin are `CantAttack`/`CantBlock` statics,
  so the AI put +1/+1 counters on a pacified creature. It now also counts a creature that can block nothing and
  attack no one (next-turn checks, so summoning sickness and tapping don't count), and +1/+1 counter targeting
  prefers creatures that aren't useless.
- **Transcantation** (CMB1 #67). `Clone` takes `CopyCardName$ <name>` (copy a card by name, from the same per-game
  template cast-as uses) and `ReplaceSpell$ True`: a spell on the stack that became a copy gets the copy's spell
  ability in its place on the stack (`MagicStack.replaceInPlace`), with no targets and none of the old ability's
  buyback/flashback/kicker (its rulings). It's the same object, so anything that targeted it now targets the new
  ability unconditionally; then its controller may choose targets (`chooseTargetsFor`, cancelable for a human), and
  BecomesTarget triggers run for what's chosen. With none it doesn't resolve. After it resolves the card leaves with
  its own characteristics. CloneAi `AILogic$ Transcantation`: an opponent's spell of mana value 3+ on top of the
  stack, unless a Bolt at the AI would be lethal.
- **Trial and Error** (CMB1 #68). New card type **Elemental**: `CoreType.ElementalCard`, written `ElementalCard` in
  scripts and printed "Elemental" (after Kindred, before the other card types), so "Elemental" in scripts and Valid
  strings keeps meaning the creature type, as its ruling says. Elemental types are a new `[ElementalTypes]` section
  of TypeLists.txt (Fire). "Is countered" is `Mode$ Countered` with no TriggerZones (CR 113.6k: it works from
  wherever the card goes); "fizzles" is `Mode$ ChangesZone | Origin$ Stack | Fizzle$ True`, whose triggered
  `StackSa` (`Defined$ TriggeredStackSa`) is the spell that fizzled. A human choosing new targets for a copy that has
  none left (it fizzled) now gets a normal target prompt instead of keeping zero targets.
- **Whammy Burn** (CMB1 #70). New zone `ZoneType.WhammyDeck`, created for every player but kept out of
  `Player.ALL_ZONES`, so nothing but whammy effects sees its cards (its ruling: effects on cards you own not on the
  battlefield don't reach it). `Player.getWhammyDeck()` forms it from a Plains, Island, Swamp, Mountain, and Forest
  from outside the game the first time it's needed. DigUntil: `DigZone$ WhammyDeck`; `ShuffleFirst$ True` shuffles
  the dig zone before revealing; `MayStop$ True` reveals one card at a time and asks after each whether to reveal
  another ("until you reveal X or choose to stop"). AI `AILogic$ Whammy`: plans its target for 2 damage, then stops
  once the damage kills its target, keeps going while that's at least even odds, and otherwise stops at 2.
- **Bear with Set's Mechanic** (CMB1 #71). New keyword `Aggressive`: as the active player's first combat phase of
  their own turn ends, if they control a creature with aggressive, one more combat phase follows it
  (`PhaseHandler.addAggressiveCombat`, an `ExtraPhase` marked aggressive, added after Throat Wolf's so it comes
  first), and during it `CombatUtil.canAttack` refuses creatures without aggressive (`isAggressiveCombat`). One extra
  combat however many creatures have it; gaining one later in the turn is too late (its rulings).
- **Experiment Five** (CMB1 #73). New mana symbol **{Z}**, "paid with one mana from any source that could produce
  two or more colors of mana": `ManaAtom.IS_MULTI_SOURCE`, `ManaCostShard.Z`, handled wherever snow's {S} is (pool
  payment, `CostPayment`, AI source grouping and shard matching, never by convoke). `Card.couldProduceTwoOrMoreColors()`
  unions the colors every mana-producing ability on the object could make (ignoring conditions and costs); a mana's
  source is its LKI card. Drawn {Z} symbol, a disc of five color wedges, on desktop (`CardFaceSymbols`, symbol cache
  `Z.png`) and mobile (`FDrawnSymbol` wedge constructor).
- **Inspirational Antelope** (CMB1 #78). New keyword `Legacy`: before the game starts (with Cryptic Spires' marking
  in `GameAction`), its owner writes a keyword ability or ability word in its blank. The word is written on the
  physical card for good (its rulings), so it's a PaperCard flag, `legacy=<word>`, saved in the deck file like
  Spires' marked colors: `Match.writeLegacyWord` swaps that one copy in the owner's registered deck, and FModel's
  listener applies the same swap to the stored deck of that name (any folder of constructed, commander,
  oathbreaker, tiny leaders, brawl) and saves it. `Card.legacyWord` comes from the PaperCard, survives zone changes,
  and is copied by clones and token copies (its ruling); card detail shows "(Written: <word>)". The words on offer
  (`GameAction.legacyWordChoices`): CR 702's keyword abilities and 207.2c's ability words
  (`res/lists/LegacyWords.txt`, from the 2026-09-25 rules) plus the engine's own keywords (Legacy itself, Aggressive,
  ...). `Card.hasKeywordOrAbilityWord`: a keyword by name, any modifier; an ability word as a line of rules text that
  starts with it. Property `HasLegacyWord` (vs. the source's word) for the ReduceCost. The AI writes the word the
  most of its nonland cards have.
- **Interplanar Brushwagg** (CMB1 #79). New keyword `Interplanar` and the **interplanar battlefield**: a stand-in
  player (`Game.getInterplanarPlayer`, made the first time something enters it, `Player.isInterplanar`) that isn't
  seated, takes no turns, can't win or lose, and is nobody's opponent, so a permanent on its battlefield is one you
  don't control but not one an opponent controls (its rulings). A card with interplanar entering the battlefield goes
  there instead (`GameAction.changeZone`); `Card.getController` is always that stand-in there, so gaining control of it
  doesn't happen. `Game.getCardsIn(Battlefield)` and the other whole-game card lists include its battlefield, so
  "all creatures" effects see it. Its triggers go on the stack after every player's, so they resolve first. The UIs
  show it as another field (`GameEventPlayerAdded`, as for Better Than One's teammate). New API `JoinAttack`
  (`Defenders$`, `Chooser$`): a creature becomes attacking without being declared - no attack triggers, and
  can't-attack effects don't stop it; the attacking player chooses the defender when there's more than one.
- **Maro's Gone Nuts** (CMB1 #81). Static `Mode$ DoubleDoubling`; `StaticAbilityDoubleDoubling.factor(game)` is what an
  effect that doubles multiplies by: 2, 4 with one out, 8 with two (its rulings; anyone's counts). Applied where the
  engine doubles: MultiplyCounter's default multiplier, Pump `NumAtt$/NumDef$ Double`, ReplaceToken's default
  `Amount`, ReplaceMana `ReplaceAmount$ 2`. Scripts: new math `Doubled` (x factor) and `DoubledExtra` (x factor-1,
  for doubling written as adding as much again), and `Count$DoublingFactor`. `/Twice` stays plain x2, since 217
  cards that don't double use it: 74 doubling cards ("double that damage", "twice that many ... instead", "double
  your life total", "double the value of X") now use `/Doubled` instead, plus Increasing Confusion and The Keeper of
  Dark Pacts. Left at x2, as making twice as many of something isn't doubling (ruling 3): Borborygmos and Fblthp,
  Fangorn, Galloping Lizrog, Lulu, Struggle for Project Purity, Tainted Adversary, Unassuming Gelatinous Serpent,
  devour 2.
- **Patient Turtle** (CMB1 #82), script only (`Count$StartingPlayer`). "Went first" now includes the starting
  player's Two-Headed Giant teammate, who takes that first turn with them (CR 805.4, as Gemstone Caverns already
  had it): `Player.isStartingPlayer` uses `sharesTurnWith`, which also affects Desert Cenote and the other
  `Count$StartingPlayer` cards in Two-Headed Giant games.
- **Soulmates** (CMB1 #84). Auras that enchant two creatures: `Card.attachAlsoTo` / `getAlsoAttachedTo` - attached to
  its main object as usual and also to the rest, which list it among their attachments, so `EnchantedBy` and
  "enchanted creatures" cover all of them. Its `AuraSpell` targets two with Attach `AttachToAll$ True`: with fewer
  legal targets left it goes to the graveyard instead of the battlefield (its ruling), and it remembers what it
  enchants, for "destroy the other". State-based actions treat it as no longer enchanting when either creature is
  gone or can't be enchanted by it, so it's put into the graveyard (its ruling) and lets go of both. AttachAi keeps
  choosing targets for an AttachToAll Aura until it has enough.
- **A Good Thing** (CMB1 #86). New static `Mode$ CantDestroy` (`ValidCard$`, `ValidCause$`;
  `StaticAbilityCantDestroy`, checked in `GameAction.destroy` - only a destroy a spell or ability causes), with the
  existing CantExile / CantTarget (`Activator$ You`) / CantSacrifice (costs included) for the rest of "your spells
  and abilities can't get rid of it". Upkeep: SetLife to `Count$YourLifeTotal/Doubled` - a gain or loss of the
  difference, as its ruling says, so Boon Reflection and Maro's Gone Nuts apply - then you lose at 1,000 or more.
  Fixed on the way: SetLife reads a life-total amount in halves, and `Count$YourLifeTotalHalves` didn't exist, so
  "double your life total" from `Count$YourLifeTotal` set it to 0 - Revival // Revenge and Angelic Enforcer did
  that too.
- **Abian, Luvion Usurper** (CMB1 #87). Static `Mode$ ControllerBecomesThis`: as it enters (after its loyalty
  counters are on it) its controller becomes it (`GameAction.becomeCard`, `Card.getEmbodiedBy` /
  `Player.getEmbodied`). Their life total becomes its loyalty - set the CR 119.5 way, so from 20 that's losing 15 -
  then each follows the other (its rulings): loyalty counters put on or removed, by costs or damage to it, are life
  gained or lost, and any life change resets its loyalty to match. Leaving the battlefield, you're just a player
  again and your life stays. Spending loyalty can't exceed your life (ruling), and the AI's X stops one short of it.
  New planeswalker type Abian; new token rg_3_2_spirit. Not modeled: you also being a red and green legendary
  planeswalker player, and the -X damage coming from you rather than from Abian.
- **Bucket List** (CMB1 #89). Tracker lists: the types a card prints (`SVar:TrackerTypes`) and which have counters
  over them (`Card.getCoveredTrackerTypes`), kept on the object but not counters on it, not copied, and gone with the
  object (its rulings). New API `TrackerCover` (covers each showing type the defined cards have), property
  `TypeShowingOnSource` (a type not yet covered - only those trigger it), `Count$TrackerCovered`; card detail shows
  "(Covered: ...)". The extra card is drawn before the sacrifice, while the tracker list still exists to check.
- **Kaya, Ghost Haunter** (CMB1 #94). Her 0 is `Haunt` with new `ReturnWhenHauntedLeaves$ True`: she's exiled
  haunting the creature, and a leaves-play command on it brings her straight back (a new object with her starting
  loyalty, her rulings) if she's still haunting it from exile. Property `Haunted` (haunted by anything - her emblems
  can target those, ruling 4). Emblems: 3 damage to a haunted creature's owner (targeted through a Pump, since a
  targeting DealDamage hits its target); control of a haunted creature for as long as it stays haunted
  (StaticCommandCheck on `Card.Self+Haunted`). Fixed: a card now stops haunting whenever it leaves exile
  (CR 702.55c, in `GameAction.changeZone`) - the Haunt keyword's static unhaunt trigger looked at the new object,
  which never had the haunting, so creatures stayed haunted.
- **Localization:** zone name "whammy deck" (`lblWhammyDeckZone`) and card type label "Elemental"
  (`lblElementalCard`).
- **Tests:** `AiPlayReportsTest.testCountersSkipACreatureThatCantAttackOrBlock`, plus `TranscantationTest`,
  `TrialAndErrorTest`, `WhammyBurnTest`, `AggressiveTest`, `ExperimentFiveTest`, `InspirationalAntelopeTest`,
  `InterplanarBrushwaggTest`, `MarosGoneNutsTest`, `PatientTurtleTest`, `SoulmatesTest`, `AGoodThingTest`,
  `AbianTest`, `BucketListTest`, `KayaGhostHaunterTest`.

### 2026-10-13, fifth deploy (desktop, Android, GitHub) — upstream merge (41 commits); Mystery Booster playtest cards #51–66

- **Upstream merge** (41 commits). Conflicts: `CardRules.canBePartnerCommanders` keeps Legendary partner and takes upstream's implied partnership; `PhaseHandler.declareAttackersFor` takes upstream's undo barrier and keeps the fork's `active` player; `GameLobby` Archenemy keeps removing Two-Headed Giant and applies upstream's Archenemy teams; The Earth Crystal and Summitfest Closing Ceremony take upstream's scripts (Covercast now works outside the stack, as "this card intensifies" reads). Upstream's counting `MyRandom` wrapper only forwarded `next(bits)`, so a Random given to `setRandom` lost its own `nextInt(bound)`/`nextDouble`/`nextGaussian`; it now forwards each public method (still counting), which the fork's fixed-dice tests rely on.
- **Tibalt the Chaotic** (CMB1 #66, upstream) verified, with a fix: `Play | RandomCopied$` threw away a random pick
  the AI's decks exclude (`RemAIDecks`) after picking it, so an AI Tibalt's +1 cast nothing whenever it rolled
  Ignorant Bliss; the cast is mandatory (its rulings), so that skip now applies only when the AI gets a choice
  (`Optional$`/`ChoiceNum$`, the Vanguard avatars).
- **Throat Wolf** (CMB1 #65; "Summon Wolf" is a creature, per its ruling). Castable during an opponent's combat
  (`CastWithFlash` with `Phases$`/`PlayerTurn$ Opponent`). New keyword `Firstest strike`: when a combat has one,
  the first-strike damage step happens twice, first as a firstest strike step where only those creatures deal damage
  (`Combat.assignFirstestStrikeCombatDamage`; they then count as having dealt first-strike damage, so with first
  strike they hit again in that step and with double strike in all three). New static mode
  `CombatAfterOpponentsFirst`: as a combat phase ends that was its active player's first that turn
  (`PhaseHandler.combatsAsActiveThisTurn`), each opponent's Throat Wolf adds a combat phase right after
  (`ExtraPhase.setOwner`), added in reverse turn order so the rulings' multi-Wolf order comes out; that player is
  the active and attacking player for it like a Clocknapper-stolen phase (still the opponent's turn for
  `getActualTurnPlayer`), and only that Wolf can attack (`PhaseHandler.getOnlyAttacker`, checked in
  `CombatUtil.canAttack`). AI damage order knows firstest strike comes first.
- **Siege Elemental** (CMB1 #64). Script only: `CantBlock` on untapped creatures plus Masako's `BlockTapped` for
  tapped ones, which overlooks only being tapped (its rulings).
- **Seasoned Weaponsmith** (CMB1 #63). New keyword `Tasty`: opponents' tasty creatures are among the defenders
  (`CombatUtil.getAllPossibleDefenders`, and the attack UI already lets you click a defender card); an attacked one
  can block only creatures attacking it (`CombatUtil.canBlock`); an unblocked attacker deals its damage to the
  creature it attacks (the planeswalker path), so trample has nowhere further to go, and the attacked creature deals
  its combat damage to every unblocked creature attacking it as a blocker would (`Combat.assignBlockersDamage`),
  blocking or not (its rulings). `removeAbsentCombatants` keeps an attacked creature while it's a creature on the
  battlefield. AI: creatures it doesn't need at home and isn't attacking with go after a tasty creature they kill and
  survive, when they can't be blocked profitably.
- **Red Herring** (CMB1 #62), scripted as `P-Red Herring` (upstream's convention for playtest cards sharing a name
  with a real card; Murders at Karlov Manor's Red Herring is untouched). The Switch effect gains `StackValid$`: with
  a permanent, the two objects trade printed cards as for Mary O'Kill, so counters, attachments, combat, tapped status
  and targets all stay (its rulings); with a spell, the stack object becomes the hand card's own spell in the same
  place (`MagicStack.replaceInPlace`), and whatever targeted the old spell is changed to the new one if that's a
  legal target, otherwise it keeps the old, now-gone target. SwitchAi `AILogic$ RedHerring` exchanges with the most
  valuable thing of its own an opponent is targeting, when that's worth more than Red Herring.
- **Queue of Beetles** (CMB1 #61). New static mode `StackFirstInFirstOut`; `MagicStack.resolveStack` resolves
  `nextToResolve()` (the bottom object while one applies, per its ruling); nothing else about the stack changes.
  CounterAi doesn't counter under it, since the target always resolves first.
- **Problematic Volcano** (CMB1 #60), with #59 Planequake (upstream) verified: no one has the "Uncovered Cavern"
  plot booster, and its ruling says that part is then ignored. New `ChooseSide` effect: `AllPlayers$ True` (each player
  in turn order from you picks which of their creatures go left, the rest go right, logged so later players see it),
  `Defined$` (each card's controller picks; used as an `ETBReplacement:Other` for every entering creature),
  `Clear$ True` (when it leaves). Sides are kept per volcano object (`Card.setVolcanoSide`, id + game timestamp, per
  its rulings), shown as a "Side:" marker; card property `OtherSideOfVolcano` (relative to the attacker) drives a
  `CantBlockBy` with `ValidBlockerRelative$`, so a creature with no side blocks and is blocked by either. AI
  (`AILogic$ Volcano`) goes to the side with more opposing power, to block.
- **Mana Abundance** (CMB1 #58). `ReplaceMana | AllPlayers$ True` flags the produce-mana event
  (`AbilityKey.ManaForAllPlayers`); `AbilityManaPart.produceMana` then adds the final mana to every player's pool from
  the same mana ability (same source and riders, "you" = whoever adds it, per its rulings) and runs ManaAdded for
  each. Any mana counts, spells too (`ValidSA$ SpellAbility`); `ValidPlayer$ Player` keeps it out of the AI's
  mana-color predictions, which leave the player unset, since it never changes what the producer gets.
- **Lightning Colt** (CMB1 #57). Script only: an Instant Creature, so it resolves onto the battlefield under the
  Visitor from Planet Q ruling; the AI's flash timing logic already casts it at instant speed.
- **Lazier Goblin** (CMB1 #56), with #55 Impatient Iguana (upstream) verified. New keyword `K:Motivate:cost`: a
  sorcery-speed special action (`ST$ Motivate`, offered only until you've paid) that records the payer on the object
  (`Card.addMotivatedBy`, card properties `MotivatedByYou`/`MotivatedByController`), plus `CantAttack,CantBlock`
  unless its current controller has paid, so a new controller pays again and you're still motivated if you get it
  back (its rulings). A new object (zone change) starts unmotivated. AI motivates before combat when it could attack.
- **High Troller** (CMB1 #54). Cost: `ReduceCost` with `ValidSpell$ SpellAbility.numTargets GE1` (costs are set after
  targets, so having any target is what counts). Targets: new static mode `RandomTargets`
  (`StaticAbilityRandomTargets`); `MagicStack.add` re-picks every target at random among legal ones before anything
  sees them, keeping how many there are (the controller's choice, per its rulings), in order, never leaving a later
  target with nothing legal; copies keep their targets. Human players instead choose how many and get random targets
  in `TargetSelection`.
- **Bombardment** (CMB1 #53), with #51 Xyru Specter and #52 Yawgmoth's Testament (upstream) verified. Every card
  you own off the battlefield as it resolves becomes a Missile as a copiable value (ruling 6): `Clone` with new
  `CopyItself$` (each target copies itself, with the changes) and clone param `RemoveAllAbilities$`
  (`CardState.clearAllAbilities`); supertypes stay (ruling 4). Zone changes drop clone states (ruling 1); new
  `KeepWhenCast$` has a spell the clone granted re-apply it on the stack (`SpellAbility.getCloneCauseWhenCast`, applied
  in `Card.setSplitStateToPlayAbility`), new `EndsWhenShuffled$` ends it for a shuffled library's cards (ruling 3),
  and new `ReplacesSpellAbility$` keeps a spell that became a Missile on the stack from resolving, since its Missile
  ability has no target (ruling 5). A granted spell needs its own `Cost$` or it keeps the printed mana cost. AI:
  `AILogic$ Bombardment` (CloneAi) casts it only when the Missiles it can then afford are lethal.
- **AI simulation:** `GameCopier` copies Lazier Goblin's motivated players and Problematic Volcano's chosen sides
  onto the simulated game's cards.
- **Tests:** `BombardmentTest` (5), `HighTrollerTest`, `LazierGoblinTest`, `LightningColtTest`, `ManaAbundanceTest`,
  `ProblematicVolcanoTest`, `QueueOfBeetlesTest`, `RedHerringTest`, `SeasonedWeaponsmithTest`, `SiegeElementalTest`,
  `ThroatWolfTest`, `TibaltTheChaoticTest`.

### 2026-10-13, fourth deploy (desktop, Android, GitHub) — Mystery Booster playtest cards #31–50; zone tabs on the left; deck-gen and AI fixes

- **Witty Demon** (CMB1 #50): new count `Count$MinimumDeckSize` (the format's minimum main deck); the starting deck
  is `YourStartingLibrarySize`, which counts the whole main deck as its ruling asks (Buried Ogre and Memory Bank
  included). **Underdark Beholder** (CMB1 #49, upstream script) fixed: it revealed until a card with mana value up to
  the eyestalk count; the card says less than (`cmcLTZ`).
- **Swarm of Locus** (CMB1 #48). Per its ruling, Locus is a creature type here as well as a land type, the Swarm
  counts both, and cards that refer to one don't see the other. Locus added to the creature types; removing creature
  types (RemoveCreatureTypes, setting creature types) leaves a land its land type Locus; Cloudpost, Glimmerpost and
  Monument to Perfection count Locus lands only.
- **Sunimret** (CMB1 #47): new keyword reverse miracle. As you begin searching your library, before any card moves,
  its bottom card may be cast for its reverse miracle cost (`ChangeZoneEffect.handleCastWhileSearching`, alongside
  Panglacial Wurm). AI: new confirm mode `CastFromSearch` answers with its usual "would I cast this now" judgment;
  ChangeZoneAll's mass-exile logic no longer waits for main 2 or rolls its 80% chance for a spell cast by an effect,
  which is now or never.
- **Spellmorph Raise Dead** (CMB1 #46): new keyword spellmorph. Cast face down for {3} like morph; while face down,
  the permanent can be cast as the card's own spell, from the battlefield, at instant speed, for its spellmorph cost
  (a copy of its spell, `Spellmorph$`, offered alongside morph's turn-face-up abilities). It can't be cast once the
  card has lost the ability (the morph check, CR 702.37e). Any instant or sorcery that something tries to turn face
  up on the battlefield is revealed and stays face down (its ruling; manifest's 701.40c).
- **Largepox** (CMB1 #44, upstream script) fixed: it sacrificed type by type, so an artifact creature given up as the
  artifact could spare a second artifact. Now one `SacEachValid$` sacrifice like Catch // Release, which has to give
  up as many as possible (Release's ruling). #45 One with Death (upstream) verified.
- **Gunk Slug** (CMB1 #43), with #42 Frogkin Kidnapper (upstream) verified. New `MakeCard` param `TokenScript$`
  builds a token card that has no real card behind it from a token script (`c_s_gunk`: colorless Gunk sorcery,
  no mana cost, cycling {4}; Gunk added to the spell types). Judgment call, following the user's design-intent rulings (over CR 111.2/400.3): the
  targeted opponent owns the Gunk shuffled into their library, so it cycles into their graveyard.
- **Everlasting Lich** (CMB1 #41), with #37 Celestine Cave Witch, #38 Chimney Goyf, #39 Corrupted Key and #40
  Cyclopean Titan (upstream) verified; the Cave Witch keeps upstream's intended curse (as printed, its reminder text
  would drain its own controller). "Can't die" per its reminder text: indestructible, `CantSacrifice`, and new
  `IgnoreStateBasedActions` param `Rule$ ZeroToughness`, which exempts only from 704.5f (counters still cancel, the
  legend rule still applies). AI: `wouldDieToStaticPT` honors a card's own exemption, so it casts the 4/0.
- **Buried Ogre** (CMB1 #36), with #35 Bone Rattler (upstream) verified. Uses `MayEffectFromOpeningDeck`, which runs
  before the library is shuffled and after starting life is set (both per its ruling); life lost there no longer counts
  as lost "this turn" on turn 1. ChangeZone's `OptionalPrompt$` now also applies to a defined card. AI:
  `AILogic$ IfDeckReanimates` buries it only when another card in its library returns creature cards from a
  graveyard to the battlefield.
- **Blood Poet** (CMB1 #34): new keyword spark and player counter type `SPARK`. Spark abilities take `Spark$ True`
  (plus `SorcerySpeed$`); `SpellAbilityRestriction` allows one per turn from among everything you control (counted
  from `getAbilityActivatedThisTurn`, so a source that has left still counts) and none from a card without spark.
  New cost `SubCounterYou<N/TYPE>` (`CostRemoveCounterYou`) spends your own counters; "+1:"/"−3:" via `CostDesc$`.
  AI: PumpAi `AILogic$ SparkBuild` takes the +1 unless a spark ability that spends counters is ready; the −3 discard
  uses `AnyPhase`, since discard AI otherwise waits for main 2 and the turn's one spark activation would go to +1.
- **Visitor from Planet Q** (CMB1 #33), an Instant Creature. CR 304.4 keeps instants off the battlefield, so by the
  rules it (and the flash creatures it makes instants) could never resolve as a permanent. User rulings: an instant or
  sorcery that also has a permanent type enters the battlefield (`GameAction`, token copies too), and "creature cards
  you own" covers the stack and battlefield as well. On the battlefield such a card isn't a spell (`Card.isSpell`) and
  shows its permanent abilities.
- **Truth or Dare** (CMB1 #32), with #31 Time Sidewalk (upstream) verified. "Target opponent chooses one": modes come
  before targets (CR 601.2b), so the caster picks which opponent chooses (CR 700.2e) and that opponent is the only
  legal target (new player property `ModeChooser`). `CharmEffect.makeChoices` now settles the chooser before working
  out which modes are legal, and only offers opponents with enough legal modes; Charm takes `ChooserDescription$`
  for the rules text. AI: as chooser it takes Truth unless Dare would mill nothing; as caster it holds the card when
  every opponent is down to ten; once another player has chosen, the AI picks the chosen mode's targets then (any
  legal one if its own targeting declines).
- **Zone tabs on the left again (mobile, landscape).** Upstream #12199 (in the 2026-10-10 merge) made the right-edge
  Hand/Library/Graveyard/Exile column the default and removed the *Alternate Player Zone Layout* setting. New fork
  setting of that name (`UI_ZONE_TABS_LEFT`, off by default) puts those tabs back in the left column with the rest.
- **Watchwing Scarecrow, Wingrattle Scarecrow, Gearsmith Guardian:** `DeckNeeds:Color$`, so random color decks
  without their colors skip them (Watchwing turned up in mono-black). Random commander decks now honor the same hint
  (`DeckGeneratorBase.meetsColorNeeds` is shared).
- **Call from the Grave AI:** the self-damage (`Remembered$CardManaCost`) reads 0 while the AI decides, so it cast it at
  2 life and died. `DamageDealAi` now plans for the costliest card the parent ChangeZone could move.
- **Blood Poet cost payment:** `SubCounterYou` is paid by the AI only when it has enough of its own counters, and a
  human confirms the payment.
- **Tests:** `CallFromTheGraveAiTest`, `ColorlessDeckNeedsTest` (Watchwing Scarecrow kept out of a black-red pool,
  allowed in white-black), `TruthOrDareTest`, `VisitorFromPlanetQTest`, `BloodPoetTest`, `BuriedOgreTest`,
  `EverlastingLichTest`, `GunkSlugTest`, `LargepoxTest`, `SpellmorphTest`, `SunimretTest`, `SwarmOfLocusTest`,
  `WittyDemonTest`.


### 2026-10-10, third deploy (desktop, Android, GitHub) — upstream merge (11 commits); Mystery Booster playtest cards #16–30

- **Upstream merge** (11 commits, through 5222f05e569: mobile card zoom/match screen/command zone layout, simultaneous
  entry, MDD spoilers): conflicts kept both sides. Phyrexian Adapter takes upstream's `AddType$` fix with the Oracle
  wording; the mobile life labels keep the fork's hundredths and fitting on upstream's single life font; the pay-life
  prompt keeps the fork's half-life affordability check with upstream's `confirmPayment`.
  The other conflicts were trivial: `ChangeZoneAi` imports (both sides' kept), `CardView.updateToughness` (the fork's
  `HasHalfToughness` line kept, upstream's explanatory comment dropped), and `TypeLists.txt` (the fork's Mime and
  upstream's Mindless-One both kept).
- **Memory Bank** (CMB1 #27): new keyword bank. Cast from your hand, it's exiled for the rest of the match instead of
  going to the graveyard (not if countered); each later game of the match starts with it in exile, where you may
  cast it.
  - **Engine:** `K:Bank` builds a Moved replacement (stack to graveyard, not countered, `ValidLKI$` cast from your
    hand) running `DB$ Bank` (`BankEffect`), plus a static letting an earlier game's banked card be cast from exile.
    `Card.getBank`, card properties `Banked`/`BankedEarlier`, a "Banked" marker in exile, `Match.recordBank` at game
    end and `Match.startBanked` at the next game's start. `MemoryBankTest` (2).
- **The Grand Tour** (CMB1 #30, upstream script): fixed. A nontoken permanent never finished the tour; it stayed in
  exile, because each step looked for the object from before its last move. Each step now follows the card's newest
  object. It can also target tokens now (ruling: a token stops at exile). `TheGrandTourTest` (1).
- **Squidnapper** (CMB1 #29): takes an opponent's creature until it leaves the battlefield or that player pays the
  ransom ({6} and 2 life, any time they have priority, without using the stack).
  - **Engine:** GainControl `LoseControl$ Ransom` (`ControlGainEffect`, `Card.addRansom`/`payRansom`); special action
    `ST$ Ransom` (`RansomEffect`) with new player property `HeldForRansom`. **AI** (`RansomAi`): pays when the
    creature is worth it and the life can be spared. `SquidnapperTest` (2).
- **Loopy Lobster** (CMB1 #26): new keyword four-faced; each stage's stats and abilities come from its own static.
  - **Engine:** `K:Four-faced` (a Taps trigger), the object's stage (`Card.getStage`, a "Stage N" marker on the
    battlefield), `DB$ AdvanceStage` (`AdvanceStageEffect`, 1→2→3→4→1), and `Stage$ N` on static abilities.
    `LoopyLobsterTest` (2).
- **Learned Learner** (CMB1 #25): has "{T}: Draw a card." while your maximum hand size isn't 7; no maximum hand
  size counts (user's call). Its Cephalid type is Octopus, as real Cephalids now are.
  - **Engine:** player property `maxHandSize <cmp><n>`, computed from the hand-size effects themselves
    (`StaticAbilityContinuous.projectedMaxHandSize`); static abilities now accept `CheckDefinedPlayer$`.
    `LearnedLearnerTest` (1).
- **Innocuous Insect** (CMB1 #23): script only. Its ruling says a bought-back permanent spell goes to your hand
  instead of the battlefield, but the current rules (CR 702.27a) only return a spell that would go to the graveyard
  as it resolves, so buyback does nothing and the Insect enters the battlefield (rules over rulings). AI: it never
  pays buyback on a permanent spell, or holds one back hoping to.
  - The AI change is general: `SpellAbilityAi` skips the buyback optional cost on any permanent spell, and
    `AiController`'s preserve-buyback-spells check no longer applies to permanents. `InnocuousInsectTest` (2).
- **Form of the Mulldrifter** (CMB1 #22): creature cards in your hand can be cast as Mulldrifter, or evoked as one.
  On the stack and the battlefield they are Mulldrifters, and stay that way if the Form leaves.
  - **Engine:** static `Mode$ CastAs` (`StaticAbilityCastAs`, `SpellAbility.getCastAs`): matching cards get the named
    card's spells, alternative costs included, and take its copiable values as a clone state on the stack.
    `FormOfTheMulldrifterTest` (2).
- **Do-Over** (CMB1 #20): restarts the turn, back to exactly how it began. Every card goes back where it was, as
  it was (the engine knows every hidden card, so nothing is left to chance). Tokens, copies and effects made this
  turn are gone; effects and delayed triggers from before the turn still apply, and life totals and turn counts go
  back. AI: it casts Do-Over when the turn has cost it clearly more than its opponents, in life and permanents.
  - **Engine:** `SP$ RestartTurn` (`RestartTurnEffect`, `RestartTurnAi`); `forge.game.TurnRestart` snapshots the game
    between turns (only in a game with a card that can restart one) and restores the same card and player objects
    field by field. The restart waits for the spell to finish resolving (`PhaseHandler.requestTurnRestart`, run at
    the end of `MagicStack.resolveStack`), then begins the turn again at its untap step; nothing triggers while the
    turn is undone. `DoOverTest` (3).
- **Command the Chaff** (CMB1 #18, upstream script): fixed - it could play a land from the sideboard (ruling: you
  can't); now only spells (`ValidSA$ Spell`).
- **Biting Remark** (CMB1 #17): new keyword Scrycast. AI: a permanent spell cast by an effect no longer waits for
  main phase 2 (it's now or never).
  - **Engine:** `K:Scrycast:<cost>` (`Keyword`, reminder text in `Card`), handled in `GameAction.scry`: after the
    cards are seen and before they're arranged, each Scrycast card may be cast for its cost; the rest are scried and
    no further card is looked at (ruling). The AI change is `PermanentAi` (`isCastFromPlayEffect`).
    `BitingRemarkTest` (2).
- **Animate Spell** (CMB1 #16): an Aura on a spell; the card becomes a creature on the battlefield and its owner casts it
  or sacrifices it when the Aura leaves (rulings: P/T from the mana value on the stack, X included; the Aura's enchant
  ability swaps to the permanent; no cast if the creature left first or at the same time).
  - **Engine:** Aura spells can target a spell on the stack (`AttachEffect` attaches to its card); ChangeZone
    `AllowNonPermanent$ True` lets an instant or sorcery onto the battlefield (`GameAction`, 101.1 over 304.4); Play
    `CastFromBattlefield$ True` (`SpellAbility.setCastFromBattlefield`, `Spell.canPlayFromHost`, the zone
    restriction and `PlayAi`), and an animated instant isn't cast as a creature spell; player definition
    `AttachedBy <cards>Owner/Controller` (`AbilityUtils`). `AnimateSpellTest` (3).

### 2026-10-10, second deploy (desktop, Android, GitHub) — 2018 Heroes of the Realm (Optimus Prime, Sol); Ponies: The Galloping; Mystery Booster playtest cards #1–15

- Flaky test fixed: `SlayingMantisTest.testThrowAlwaysLandsAndAimsAtWhatItKills` threw 40 unseeded Mantises and
  needed more than 10 to touch the Dreadmaw (about 40% each), so it failed now and then (10 of 40 on 2026-10-10).
  The throws now run on a seeded `MyRandom`, restored afterward: 22 of 40 every run.

- **Stack of Paperwork** (CMB1 #13), with #11 Sarah's Wings and #12 Scaled Destruction (upstream) verified; Scaled
  Destruction's modes-in-order ruling now has a test.
  - **Engine:** static `Mode$ CombatDamageUsesStack`: while a permanent with it is on the battlefield, each combat
    damage step's assignment goes on the stack as one object controlled by no player
    (`forge.game.combat.CombatDamageOnStack`, pushed by `MagicStack.addCombatDamage` without cast triggers or
    activation counts; `Combat`, `PhaseHandler`) and is dealt when it resolves. A source that left deals damage as it
    last existed; damage to a permanent that left or stopped being a creature/planeswalker/battle isn't dealt
    (rulings). `StackOfPaperworkTest` (4), `ScaledDestructionTest` (1).
- **Ruff, Underdog Champ** (CMB1 #10): "All Hounds are Dogs" is already true in Forge (no Hound type left), so it's
  text only. New count `Count$YouLostAGameThisMatch` (`GameOutcome.isLoser`: a drawn game has no loser).
  `RuffUnderdogChampTest` (3).
- **Priority Avenger** (CMB1 #9): new count `Count$StackSize` (spells and abilities on the stack).
  `PriorityAvengerTest` (1).
- **Metagamer** (CMB1 #8), with #6 Frontier Explorer and #7 Imaginary Friends (upstream) verified. User rulings: the
  latest Pro Tour (only), kept current. Today that's Pro Tour Marvel Super Heroes, whose finals were a draft, so the
  deck is Yuuki Ichikawa's 40-card draft deck (names checked against Scryfall).
  - **Engine:** `forge.card.WinningDeck` and card property `InLatestWinningDeck`, filled by
    `forge.model.MetagamerDeckLoader` (started from `FModel`): at startup from the user-folder cache
    (`metagamer-winning-deck.txt`) or the shipped `res/lists/MetagamerWinningDeck.txt`, then refreshed at most daily
    in the background from magic.gg (the event archive's newest finished Pro Tour, its winner, that event's Top 8
    deck page and the winner's main deck); offline keeps the old deck. `MetagamerTest` (2),
    `MetagamerDeckLoaderTest` (3, against saved archive and Top 8 page samples).
- **Five Kids in a Trenchcoat** (CMB1 #5). User rulings: five creatures in anyone's count of the creatures you
  control, filtered counts and thresholds included; counts of all creatures see one.
  - **Engine:** static `Mode$ CountsAsCreatures | Amount$ N` (`CountsAsCreatures.count`), applied in `Count$Valid`,
    per-player `Valid` counts, `CreaturesInPlay` and the IsPresent checks of triggers, statics, conditions and
    restrictions; Java-coded creature counts still see one. `FiveKidsInATrenchcoatTest` (2).
- **Enroll in the Coalition** (CMB1 #4): you're a Flagbearer (not a creature); with Coalition Honor Guards out,
  opponents may target you or any Flagbearer creature (ruling). Honor Guard on its own still means creatures.
  - **Engine:** static `Mode$ PlayerType` gives a player a subtype (`StaticAbilityPlayerType.hasType`); MustTarget
    `IncludePlayers$ True` lets such players meet the requirement, and same-type requirements merge
    (`StaticAbilityMustTarget`, rewritten for it). The human targeting input keeps allowed players clickable
    (`InputSelectTargets`, `TargetSelection`), and the AI's must-target filter covers players.
    `EnrollInTheCoalitionTest` (5).
- **Baneslayer Aspirant** (CMB1 #3): only your own emblems count. `BaneslayerAspirantTest` (1).
- **Ral's Vanguard** (Mystery Booster playtest, CMB1 #1): first of the set's 60 unscripted cards. Copies don't draw
  or deal extra (ruling); the requirement is checked as the game begins.
  - **Engine:** card line `VanguardRequirement:<types>` (`CardRules`): every main-deck card must have one of the
    types, or the vanguard is left out at game start with a log line (`RegisteredPlayer.getUnmetVanguardAvatars`).
    DrawCards replacements now get the drawing ability as `Cause` (so `ValidCause$` works on them); ability property
    `CastSpell` = a spell that was cast, not a copy. `RalsVanguardTest` (3).

- **Ponies: The Galloping** (new edition PTG, silver-bordered): Nightmare Moon // Princess Luna, Princess Twilight
  Sparkle, Rarity. New creature types Alicorn and Pony. User rulings: nighttime = real local clock 6 PM to 6 AM;
  friendship is mutual; the AI owns a pony toy half the time, with colors fixed for the game; only Tagger-confirmed
  moons can be chosen for Luna. `PrintingTraits.txt` regenerated (moons, PTG, and Arena Promos/@FRA printings the
  last run predated); no existing line lost a trait.
  - **Friends** (Nightmare Moon): mutual, all-game `Player.addFriend` (logged, copied by `GameCopier`), player
    property `Friend`; `BecomeFriend$ True` makes an ability's activator and payment helpers the host controller's
    friends when it goes on the stack (`MagicStack`).
  - **Anyone helps pay** (`AnyPlayerHelpsPay$ True`, `CostAdjustment`): Assist's payment (702.132a) offered to every
    other player in turn order, for activated abilities too; helpers are recorded (`SpellAbility.getPaymentHelpers`).
  - **Play with permission** (`MayPlayWithPermission$` on a MayPlay static, Princess Luna): `MayPlayPermission.ask`
    asks the owner as a human (`PlaySpellAbility`) or AI (`ComputerUtil`) starts casting; a refusal holds for the
    turn. The AI lends only to non-opponents.
  - **Real-world nighttime**: `Count$RealWorldNighttime` (6 PM to 6 AM local) on a replaceable clock,
    `AbilityUtils.realWorldTime`, which `CurrentHour` now reads too.
  - **Moon in the art**: printing trait `moon`/`backmoon` (`PrintingTraits`), card property `KnownMoonInArt`.
  - **Toy colors** (`Protection | Gains$ ToyColors`, `ProtectEffect`, Rarity): a human picks the colors at resolution;
    the AI's toy comes from `PlayerController.getKnownToyColors`, fixed per game (`SpellAbilityAi.gameSeed`).
  - **Everypony wins** (Princess Twilight Sparkle): `WinsGame | Defined$ Player` no longer turns the other winners
    into losers (`GameAction`), and `GameWinAi` won't activate a win whose resolution condition doesn't hold.
  - `PoniesTheGallopingTest` (8).

- **Optimus Prime, Inspiring Leader** and **Sol, Advocate Eternal** (2018 Heroes of the Realm). User rulings:
  Optimus's "turn to its other face" is convert (any double-faced permanent, MDFCs included) and also turns a
  face-down permanent face up; Sol's partner can be anything that could be a commander if it were legendary,
  and it's legendary all game in every zone. Desktop and mobile deck editors accept the nonlegendary partner.
  - **Engine:** `SetState | Mode$ OtherFace` (`SetStateEffect`): face-down turns face up, double-faced (MDFCs
    included) converts under 701.28a, anything else is skipped. **AI** (`SetStateAi`): only turns a card to a
    strictly better face, so a repeatable ability doesn't flip it back and forth.
  - **Engine:** keyword `Legendary partner`: the other commander only has to be a would-be commander
    (`CardRules.canBeCommanderIfLegendary`, `DeckFormat.isLegalCommanderWith`, also used by the desktop
    `CEditorConstructed` and the Adventure duel's commander check) and is made perpetually legendary at game start
    (`Player.promoteLegendaryPartners`). Card property `IsPartner` = the source's owner's other commander.
  - `HeroesOfTheRealm2018Test` (12).

### 2026-10-10 (deployed: desktop, Android, GitHub) — Two-Headed Giant seating and team scoring; ∞ on mobile; AI twin-trigger targets; Arena Promos

- **Two-Headed Giant layout**: teammates now sit together. On mobile, seats 1+2 vs 3+4 used to stack as teammate,
  opponent, opponent, you; on desktop the teammate's field went in the top cell with an opponent. Now your team is the
  bottom half and the other team the top half on both (desktop still lets you move fields).
- **Team wins score for every winner**: the match tally ("Evan: 1 Eva: 0 ...") credited only the first winner it
  found. Every winner now counts, in the summary, match-over check, Ghazban Ogress's and Gus's counts, and the AI's
  sideboarding (which no longer dereferences a missing winner after a drawn game). The win screen's "Team 0 won!"
  now uses the lobby's numbering (Team 1).
- **∞ on mobile**: the life, counter and mana ∞ rendered as nothing because the generated fonts didn't include it.
  ∞ is now in the baked character lists (`FSkinFont.getCharacterSet`, `Assets.COUNTER_FONT_CHARS`), and the
  font-cache marker is bumped to `v2-inf` (`FSkin`) so fonts cached without it regenerate once.
- Engine side of team scoring: `GameOutcome.getWinningPlayers`/`isWinner`, used by `Match`, the outcome summary
  (`GameEventGameOutcome`), the win-streak count in `AbilityUtils` and `PlayerControllerAi`'s sideboarding. Seating:
  `MatchController.seatGiantTeamsTogether` (mobile), `CMatchUI.sortPlayersForGiantTeams` and `VMatchUI.giantSide`
  (desktop). AI twin targets: `ComputerUtil.targetedByPendingTwins`, used by `AnimateAi` and `PumpAi`.
- **AI targets with twin triggers**: two Voices of the Vermin attacking both made the same creature 4/4. Effects that
  set something (Animate, keyword-only pumps) now pick a different target from an identical trigger already on the
  stack when there is one. `PendingTwinTargetsTest`, `GiantTeamSeatingTest`, `TwoHeadedGiantTest` (11).

- **Arena Promos (PANA)**: the card list now has all 83 of Scryfall's printings. Upstream had 12 (CNs 1-7, 101-105);
  the 71 added are CNs 201-271, mostly basic lands, including Bob Ross's and Evolving Wilds. Rarities follow Scryfall,
  as `Magic Online Promos.txt` does. The fork owns this file: `.git/info/attributes` gives it the
  `edition-add-only` merge driver (`_tools/edition-add-only-merge.js`, registered as
  `git config merge.edition-add-only.driver`). Upstream can add cards (a CN new to both the base and ours) and
  metadata keys, but every line of ours stays as written, and a card the fork deleted stays deleted. Both
  settings are local to this clone; set them again on a fresh clone.

### 2026-10-09, third deploy (desktop, Android, GitHub) — Everythingamajig

- Full suite 1128 run, 0 failed, 6 skipped. Unstable: 223 printings in the card database (Scryfall's 268 minus
  the 45 Contraptions, which load separately).

- **Everythingamajig** (ust/147a-f; upstream had only C). All six printings, each with its own three abilities;
  D costs {6} and F {8}. Oracle text and rulings checked against Scryfall, plus the Unstable FAQ. Most abilities
  are other Un-cards' (Giant Fan, My First Tome, Zuran Orb, Ashnod's Altar, Urza's Hot Tub, Mirror Mirror), so
  the fixes below reach those cards too. `EverythingamajigTest` (9).
  - C's coin flip is now a mana ability (CR 605.1a), which upstream's script missed: it went on the stack.
  - "Move a counter from one permanent onto another" (A, Giant Fan) names no target, so both permanents are now
    chosen as it resolves (`ChooseOnResolution$ True`): hexproof and protection no longer stop it. The moved
    counter can become any kind the second permanent's text box names, the player's choice, rather than the first
    one found; a planeswalker's loyalty counts (FAQ). AI: pulls a -1/-1 counter off its own creature or a +1/+1 or
    loyalty counter off an opponent's, onto its best creature.
  - Complete words in a name (E, Urza's Hot Tub) split on spaces only. A hyphenated word is one word (FAQ: Five-Finger
    Discount doesn't find Five-Alarm Fire), and "Urza's" no longer leaves a word "s" that every possessive shared.
  - Exchanging libraries and graveyards (F, Mirror Mirror) keeps each pile's order instead of shuffling (ruling:
    "slide your library over as-is").
  - Saying flavor text (D, My First Tome) reads the text box, so a stolen one counts, and "in your hand" means the
    cards you hold. The AI guessing the name (user's ruling): 35% of the time it recognizes the flavor text and
    names a card that prints it; otherwise it names a card it has seen the sayer play. It used to guess from its
    own cards, and couldn't activate either card at all (`GuessName` had no AI).
  - A functional variant with its own mana cost now takes its color from that cost, not the base face's.

### 2026-10-09, second deploy (desktop, Android, GitHub) — upstream merge (1 commit); Two-Headed Giant; Unstable complete (Better Than One, Mary O'Kill through Watermarket)

- **Upstream merge** (1 commit: GameEventForwarder and its tests). Clean; full suite 1119 run, 0 failed, 6 skipped.

- **Two-Headed Giant** (CR 805, 810; rules checked against the 2026-09-25 Comprehensive Rules). See the engine
  capabilities section for what it covers. Fixed on the way: a human who is a team's second head used the first
  head's phase stops and would have auto-passed through their own main phase; their own stops now apply.
  Follow-ups: every head's field (desktop) / panel (mobile) lights up in the phase display; proliferate gives a
  team one poison counter however many heads are chosen, the proliferating player picking which (701.34b), and a
  head whose partner is poisoned can be chosen (810.10d); the starting player is normalized to their team's primary,
  so the starting team goes first for mulligans and opening-hand actions, "if you're not the starting player" means
  the starting team (103.1a), and team games declare every mulligan before any is taken (805.3a).
  `TwoHeadedGiantTest` (10).
  - **What the first commit built:** a `GameType.TwoHeadedGiant` lobby variant (desktop `VLobby` checkbox, mobile
    `LobbyScreen`, saved in preferences; it and Archenemy exclude each other). `GameLobby` takes equal lobby teams of
    two or more, or pairs seats 1+2 vs 3+4 when every slot is on its own team, seats teammates together, and refuses
    other setups with a message. `Game.formGiantTeam` gives a team 30 life (+15 per head beyond two) and a poison
    limit of 5n + 5. Shared team turns (`PhaseHandler`, `Untap`: each head phases and untaps,
    day/night looks at the whole previous team; `TriggerPhase`/`TriggerTurnBegin`), a shared life total and poison
    count (`Player`), one combined attack (the human `InputAttack` only calls back its own attackers), blocks for
    either head (`CombatUtil`; AI `AiBlockController` and `ComputerUtilCombat` count attackers aimed at the
    teammate), team wins and losses (`GameAction`, CR 810.8a) and teammates seeing each other's hands (`CardView`,
    `PlayerView`, 810.5).
  - **Players joining mid-game:** `Game.addOutsidePlayer` (an AI from the `OutsidePlayers` factory FModel plugs in)
    and `GameEventPlayerAdded`, which adds a field on desktop (`CMatchUI`) and a panel on mobile (`MatchScreen`,
    `MatchController`). Better Than One (below) is its first user. `TwoHeadedGiantTest` was 8 tests in this commit.
- **Better Than One** (ust/128; previously deferred, not scripted upstream). `SP$ GainTeammate`: the caster picks
  hand cards, a number of cards from the top of their library and permanents; an AI-played person from outside the
  game (user ruling) joins beside the caster's team, owns those cards (set directly, not as an ante ownership change -
  the FAQ says they're still yours after the game) and the caster's side becomes a Two-Headed Giant team with the
  caster's life total (FAQ: the teammate brings none), or a Three-Headed one if it already had two heads. AI
  (`GainTeammateAi`) casts it with 20+ cards in library, giving half its library and half the lands in its hand.
  `BetterThanOneTest` (3).
- **Mary O'Kill** (ust/138; not scripted upstream).
  - **Switching a card in hand with a permanent** (`AB$ Switch | HandValid$ | BattlefieldValid$`): the permanent object
    stays put and the two objects trade printed cards (`CardCopyService.swapPrintedCards`; `Card.paperCard` is no
    longer final for it), so tapped/attacking/blocking status, attachments, counters, damage, effects on it and
    anything targeting it carry over and nothing enters or leaves (rulings, FAQ: "pulls off a mask").
  - "One on the battlefield" is any player's (user): that permanent keeps its controller; the card that leaves it
    goes to your hand still owned by its owner, held by you as Five-Finger Discount holds a card.
  - Activatable from hand too (`AdditionalActivationZone$ Hand`, rulings). New creature type **Killbot** in
    `TypeLists.txt` (it was missing, so Killbot checks matched nothing; changelings now count).
  - AI (`SwitchAi`): the best value swing - upgrading its own Killbot or taking an opponent's Mary for its weakest
    Killbot - after blockers on its turn or at an opponent's end step.
  - `MaryOKillTest` (3).
- Already scripted upstream, checked against Scryfall (fields, Oracle, rulings): Angelic Rocket (ust/139), **Border
  Guardian** (ust/140), Buzzing Whack-a-Doodle (141), Clock of DOOOOOOOOOOOOM! (142), Cogmentor (143), Contraption
  Cannon (144), the four Killbots (145a-d).
  Fixed: `BorderColor<color>` (Border Guardian) and `BlackBordered` (Knight of the Kitchen Sink) read the physical
  border, so an acorn spell counted as black-bordered; they now go through `Card.bordersAs()`, which treats acorn
  cards as silver like `SilverBordered` already did (user: acorn = silver-bordered, everywhere). `BorderGuardianTest` (1).
- **Entirely Normal Armchair** (ust/146; not scripted upstream).
  - **Hidden permanents** (`ST$ Hide`, a special action from hand during your turn): it's on the battlefield behind one
    of your permanents or in plain sight (you choose; pointing at yourself = plain sight), and its controller's
    opponents don't see it - it isn't drawn for them (desktop `PlayArea`, mobile `VField`), `CardView.canBeShownTo`
    says no, they can't target it and can't activate its abilities (`Card.isHiddenFrom`, checked in
    `Card.canBeTargetedBy` and `SpellAbilityRestriction.canPlay`). If what it's behind leaves, it's in plain sight.
  - **Finding it** (user: a hidden spot to guess): each opponent gets a "Something Hidden" command-zone effect with
    `ST$ SeekHidden`, once each turn - point at one of the hider's permanents or at them. Right: it's found (no longer
    hidden, the effects go) and they may return it at once with its {0} ability; wrong: nothing. The effects exile
    themselves when it leaves the battlefield.
  - Its controller sees a reminder marker on it - "Hidden: behind <permanent>" or "Hidden: in plain sight" - kept
    current by state-based checks (`Card.refreshHiddenMarker`); only their side ever draws it.
  - AI: hides it in its second main phase at a random spot (`HideAi`); seeks once a turn at a uniformly random spot,
    never using what it knows of the game (`SeekHiddenAi`), and returns it when found.
  - `EntirelyNormalArmchairTest` (4).
- **Handy Dandy Clone Machine** (ust/149; not scripted upstream).
  - **Tokens held by real hands** (Token `RepresentedByHand$ True`, `forge.game.player.Hands`; user: track hands): every
    player has two; a new token takes its controller's free hand, else other players in turn order are asked to lend
    one (AI opponents decline, AI teammates agree), else its controller is asked whether someone outside the game will
    (honor; the AI says no); with none it ceases to exist (FAQ). A hand frees up when its token is gone; a player
    leaving the game takes their hands with them (`Game.onPlayerLost`). Each token shows a "Hand: <whose>" marker.
  - AI won't make one when no hand it can count on is free (`TokenAi`). Not modeled: a hand choosing to stop
    representing its token.
  - `HandyDandyCloneMachineTest` (2).
- **Kindslaver** (ust/150; not scripted upstream). `ControlPlayer | OutsidePerson$ True`: when the target's next turn
  begins, a person from outside the game takes over their decisions for that turn - an AI that's neither the caster
  nor the target, one of three kinds at random (user): aggressive in the target's interest (the AI on its Reckless
  profile), a novice who just says "go" (no land, no spells, no attacks, forced choices at random; rulings), or
  aggressive against them (no land or spells, every creature attacks every combat, the best cards discarded and
  sacrificed when forced). `PlayerControllerOutsidePerson`, built by an `OutsidePlayers` factory FModel registers; the
  log names the person and their kind. `KindslaverTest` (3).
- **Krark's Other Thumb** (ust/151; not scripted upstream). Static `Mode$ RollTwoKeepOne | ValidPlayer$ You`: every
  die its controller rolls - each one of several, every reroll (Wall of Fortune), the planar die - is rolled as two
  and they choose which counts (rulings); the ignored roll never happened (only the kept one counts as rolled, so
  "whenever you roll" triggers and roll counts don't see it). A second Thumb (Mirror Gallery) applies to each of the
  first's dice. `RollDiceEffect.rollCountedDie`, `PlanarDice.rollFace`. The AI now ignores the lowest roll and a
  blank planar face instead of a random one (`PlayerControllerAi.chooseRollToIgnore`/`choosePDRollToIgnore`).
  `KrarksOtherThumbTest` (3).
- Labro Bot (ust/152), Lobe Lobber (153), Mad Science Fair Project (154) already scripted upstream; checked against
  Scryfall.
- **Modular Monstrosity** (ust/155; not scripted upstream). `DB$ ChooseKeywordInTime | Seconds$ 5`:
  - **The keyword list** (`PrintedCreatureKeywords`): every keyword line on a creature face in the card database, by
    title, so only printed forms - flying, annihilator 2, protection from Demons - never annihilator 500 (rulings).
  - **"Today"** (`KeywordDayLog`): the calendar day, per person, across every Monstrosity and every game; a keyword
    counts as used whatever its modifier (protection is protection). FModel keeps it in
    `<user dir>/modular-monstrosity-keywords.txt`.
  - **Five real seconds**: `PlayerController.chooseKeywordInTime` -> `IGuiGame.chooseStringInTime` (desktop
    `ListChooser.showTimed` counts down in the title; mobile `ListChooser.timeOut` on a gdx Timer; untimed elsewhere).
    Chosen: it gains the keyword for good. Not in time: it loses all keyword abilities, ones gained other ways too.
  - AI: never short of time; takes the best keyword it hasn't used today (flying, double strike, indestructible, ...).
  - `ModularMonstrosityTest` (3).
- **Proper Laboratory Attire** (ust/156; not scripted upstream). Protection from die rolls = `Protection:Card.RollsDice:die
  rolls`; card property `RollsDice` (`Card.rollsDice`): anything that allows or requires someone to roll a die, rerolls
  included - a RollDice/RollPlanarDice ability anywhere (sub-abilities, triggers, replacements, script it grants, like
  Lobe Lobber's), Wall of Fortune's reroll permission, Krark's Other Thumb - but not cards that only care about results
  (Snickering Squirrel, Squirrel-Powered Scheme; rulings). `ProperLaboratoryAttireTest` (1).
- **Robo-** (ust/157; not scripted upstream). Script only: the Half-Orc shape (intervening-if end step, uniquely named
  `RoboArtifactEntered` SVar so it can't clash with a host's). Augmenting isn't an artifact entering - the host carries
  on, as with mutate (user ruling; the augment rulings don't say) - so Robo- never meets its own condition.
  `RoboTest` (3).
- **Split Screen** (ust/158; not scripted upstream). Several libraries per player (`forge.game.player.Libraries`):
  - A player's libraries are Library `PlayerZone`s (`Player.getLibraryZones`); `getZone(Library)` is the one "your
    library" means right now. `SplitLibrary` API: `Amount$ 4` shuffles and deals the library into four in its place
    (a second Split Screen splits one of them again - seven, rulings); `Merge$ True` shuffles them all back together.
  - "If anything refers to your library, choose one of your libraries for it." The user chose (2026-10-09):
    - One choice per resolving ability that mentions a library (`AbilityUtils.resolve` -> `Libraries.begin`): asked the
      first time it touches that player's library, kept for the rest of it (search, then shuffle the same one).
    - One per card drawn (rulings: any library; an empty one only loses if you draw from it).
    - One per permanent for continuous effects (Future Sight, Courser), asked at the next state-based check; the
      `TopLibrary` property uses it. Costs and anything else use the library chosen last.
    - Never asked while statics are being worked out (`GameAction.isCheckingStaticAbilities`).
  - Game-wide card lists, lookups and the external-zone activation list see every library. `TopOfItsLibrary` property:
    the top of whichever library a card is in. `AllLibraries$ True` on a static means "every library" (Split Screen's
    own reveal, Yet Another Aether Vortex's), and Vortex puts every library's top permanent onto the battlefield.
  - Animate Library is on only one (`Card.getRepresentedLibrary`; rulings); its size counts that library.
  - View: one library pile with every top card first; each top card has a "Library 2 of 4, 13 cards" marker.
    `PlayerController.chooseLibrary` labels each option with its size and visible top card; the AI draws the top
    that helps most (a land when short) and otherwise picks the biggest.
  - `SplitScreenTest` (7).
- **Staff of the Letter Magus** (ust/159; not scripted upstream). Script: the letter choice as it enters (ChooseType
  letter) and a SpellCast trigger gaining `TriggeredCard$ChosenLetterInName` life. Y counts as a consonant, every Y
  included (user ruling; no Scryfall rulings). New `AILogic$ MostCommonInName`: the letter appearing most often in the
  names of the AI's own nonland cards outside the battlefield. `StaffOfTheLetterMagusTest` (3).
- **Stamp of Approval** (ust/160; not scripted upstream). `ChooseType | Type$ Watermark` offers every watermark in the
  printing data by name (`PrintingTraits.allWatermarkNames`/`watermarkName`/`watermarkId`: "Goblin Explosioneers",
  "Agents of S.N.E.A.K."), the human's own cards' watermarks listed first; card property `withChosenWatermark`.
  Scryfall calls every set-symbol watermark "set" without saying which set, so "Set symbol" is one choice covering
  them all (user ruling). AI: the watermark most of its creatures have. `StampOfApprovalTest` (3).
- **Steam-Powered** (ust/161; not scripted upstream). Script only: Zombified's activated-condition augment shape
  (`AugmentCondition$ True`, `Cost$ 5`), so the host's effect becomes "{5}: ...". `SteamPoweredTest` (1).
- Unstable 162-164 (Steel Squirrel, Sword of Dungeons & Dragons, Voracious Vacuum) were scripted upstream; checked
  against Scryfall's Oracle text and rulings (gold monocolored Dragon token, repeat on a 20) - no changes needed.
- **Secret Base** (ust/165a-e; not scripted upstream). One script for all five printings; each printing's faction
  watermark comes from the printing data. The any-color ability is `RestrictValid$ Spell.sharesWatermarkWith`, a new
  card property (any watermark in common with the source). No rulings. `SecretBaseTest` (2).
- **Watermarket** (ust/166; not scripted upstream). Script only: `RestrictValid$ Spell.Watermarked` on its {C}{C}.
  No rulings. `WatermarketTest` (1). **Every Unstable card is now scripted** (name diff against
  `editions/Unstable.txt`).

### 2026-10-09 (deployed: desktop, Android, GitHub) — upstream merge (94 commits, incl. dice and flip animations); The Grand Calcutron; Hot Fix; Ol' Buzzbark; Phoebe, Head of S.N.E.A.K.; X; Urza, Academy Headmaster audit

- **Upstream merge** (94 commits: dice and coin-flip animations, FlipOntoBattlefield corner hits and a mobile flip
  animation, AI paying for mana abilities that cost mana (Signets, filter lands), simultaneous-entry fix, netplay
  host fixes, TRK/TRC cards, edition and net-deck updates). Conflicts:
  - `AiController.saSideEffects`: took upstream's reordering; the Maro-style `HandSizeAi` check stays right after
    the spells-and-lands gate.
  - `CardView` power/toughness and `Player.loseLife`: kept both sides (half P/T flags and `Infinity.add` alongside
    upstream's lethal-damage refresh and trigger-repeat clearing).
  - `RollDiceEffect`: the fork's dice loop (The Big Idea, Wall of Fortune rerolls, installed results) stays;
    `rollPhysicalDie` now fires upstream's `GameEventRollDie(sides, roll)` and collects each die's final face for
    `GameEventRollDice`. An installed result isn't a face that came up, so the animation shows the die.
  - `FlipOntoBattlefieldEffect`: rebuilt on upstream's version with the fork's empty-battlefield guard, `Thrown$`
    and protected `getNeighboringCard` (Goblin Sleigh Ride's `SlideOntoBattlefieldEffect`). A throw reports
    `TimesFlipped` 0 but animates as one tumble: the mobile board picks real hits only for a card that flew
    (`wanted = flew ? hit.size() : 0`), so a 0 there would have made Slaying Mantis miss everything on Android.
  - Ashling's Prerogative, Demonic Pact: upstream's param fixes with the fork's self-reference wording.
  - Oracle sync afterwards: Accident-Prone Apprentice, Agent of Raffine (Scryfall retemplates).
    Applied by `oracle-drift.js` against the 2026-10-09 oracle-cards bulk file ("this card is in exile"; "Mana of
    any type can be spent to cast this spell").
  - Suite after the merge: 1055 run, 0 failed, 6 skipped.
- **The Grand Calcutron** (ust/131; previously deferred, not scripted upstream).
  - **Programs**: `DB$ ReorderZone | Zone$ Hand | Program$ True` has each player order their hand and records the
    host (id + timestamp) on the player (`Player.addProgramSource` / `hasProgram`); the program lasts while that
    object stays on the battlefield (phased out still counts), so it ends on its own when the Calcutron leaves
    (rulings, FAQ). Its order is the hand zone's order, first = leftmost. While a hand is a program: it's revealed
    to everyone (`CardView.canBeShownTo`), the hand-sorting preference doesn't apply (game layer and desktop
    `CHand`), dragging cards around in the desktop hand is refused, and the card detail panel shows
    "(program: N of M, playable)". `PlayerView.hasProgram` is refreshed in state-based checks.
  - `CantBeCast`/`CantPlayLand` param `OnlyFirstOfProgram$ True`: a player with a program can play only its first
    card, **from anywhere** (user ruling: graveyard, exile, library and command zone are closed too). Suspend
    isn't playing, so it isn't stopped. A spell already moved to the stack is judged by `Card.wasFirstOfProgram`
    (set where `castFrom` is).
  - Static `Mode$ PlaceInProgram`: a card moved into a program player's hand (`GameAction.programPosition`, every
    `zoneTo.add` of a hand move, merged cards included) is placed where that player chooses
    (`PlayerController.chooseProgramPosition`; human: numbered "First, before X / Between X and Y / Last, after Y"
    list), and the game log says where. Applied after other replacements.
  - Player property `hasProgram`; end-step refill is a plain Phase trigger with `ValidPlayer$ Player.hasProgram`,
    an intervening-if on hand size and a resolution check that the program still exists.
  - AI (`AiProgram`): orders turn by turn - a land if it can still play one, then the spells that fit that turn's
    mana, biggest first; a new card goes before the first card the same plan would put after it.
  - `GrandCalcutronTest` (4). Suite: 1023 run, 0 failed, 6 skipped.
- **Hot Fix** (ust/133; not scripted upstream).
  - **Real-time limits**: `ReorderZone | Seconds$ N` gives the player N seconds to rearrange the zone
    (`PlayerController.rearrangeInTime`, `IGuiGame.rearrangeInTime` returning `TimedArrangement`); if they're still
    touching a card when time runs out, the zone is shuffled and the log says so. Contents never change (ruling).
  - Desktop: the drag-to-rearrange window (`ListCardArea.showTimed`, top first, drag anywhere, click = to top)
    counts down in its title and closes itself at zero; "touching" = a mouse button held down on a card, dragging
    or not (`CardPanelContainer.isTouchingCard`). Done before the buzzer = hands off.
  - Mobile: the order dialog counts down in its header and closes itself (`DualListBox.finishNow`); cards moved
    across go on top in that order, the rest keep their order below; "touching" = a finger on the screen at zero
    (`Gdx.input.isTouched`). Network clients get the untimed default.
  - AI (`AiTimedRearrange`): pulls up to five cards it wants next to the top (lands until it has enough for its
    hand, then the best spell it can cast) and always lets go in time.
  - `HotFixTest` (3). Suite: 1026 run, 0 failed, 6 skipped.
- **Ol' Buzzbark** (ust/134; not scripted upstream).
  - **Dice rolled onto the battlefield**: `RollDice | OntoBattlefield$ <height>` - the roller aims the volley at a
    creature before rolling (all dice leave the hand at once - ruling), then each counted die comes to rest
    (`DiceLanding`, simulated like upstream's FlipOntoBattlefield): it lands around the aimed creature in its
    controller's row of creatures, scattering more and staying on a card less often the higher the drop; it can
    stop on the edge between two neighbors and touch both. `DieLandedSubAbility$` runs once per die with
    `DieResult` set and the touched creatures Remembered; each die is announced and logged. Roll modifiers
    (Krark's Other Thumb, Snickering Squirrel, ...) apply since it's a normal roll. Not modeled: one die touching
    creatures of two different players (ruling allows it; the rows don't meet).
  - AI aims at the best opposing creature the dice could plausibly kill, otherwise its own best creature.
  - `OlBuzzbarkTest` (4). Suite: 1030 run, 0 failed, 6 skipped.
- **Phoebe, Head of S.N.E.A.K.** (ust/135; not scripted upstream).
  - New `AB$ StealTextBox` (`StealTextBoxEffect`, `StealTextBoxAi`): the target loses its intrinsic (text-box)
    abilities and keywords - not ones granted by other effects - and the thief gains copies, as changed traits on
    each object, so a zone change ends it for that side only (rulings). Copied statics carry the theft's timestamp,
    so contradicting boxes resolve most-recent-first (rulings). Stealing from a Phoebe takes her stolen boxes too.
  - **Text boxes as data**: `Card.isTextBoxStolen` / `getStolenTextBoxes` (`StolenTextBox`: name, Oracle text,
    flavor text, watermarks, printed line counts), carried into LKI copies and AI game copies. `getTextBoxOracle`,
    `getFlavorText`, `getWatermarks`, `getTextBoxContents`, `getPrintedRulesLines` (wordy) and
    `getPrintedTextLines` (Lexivore) now read all of a card's text boxes as one and see nothing on a robbed one;
    `hasFlavorText`, `Watermarked`, `Watermark_`, `rulesTextHasWord_` and capital-letter counts use them.
  - AI steals from the best opposing creature that has a text box.
  - `PhoebeTest` (6). Suite: 1036 run, 0 failed, 6 skipped.
- Urza, Academy Headmaster (ust/136) already scripted upstream; card fields and Oracle checked against Scryfall.
- **X** (ust/137; not scripted upstream). Script on the HandOf$ idiom (the holder controls a card in their hand
  that isn't theirs, so "that opponent" is `You`); statics from the hand let its owner cast it, stop the holder
  from casting it, and reveal the holder's hand while it's in an opponent's hand.
  - Card property `inOwnersOpponentsHand`.
  - ChangeZone (known origin): when an ability with `Defined$` targets only players, the targets say where the
    Defined cards go rather than replacing them ("Put X into target opponent's hand").
  - The free play uses `DB$ Play` from the hand X is in, activatable only by its owner from there; lands only on
    your turn with a land play left (ruling, PlayEffect's own check). X itself is in that hand and fair game.
  - AI (`AILogic$ SneakIntoOpponentsHand`): at an opponent's end step, into the fullest hand, once it has five
    mana sources for the follow-up.
  - `XTest` (3). Suite: 1039 run, 0 failed, 6 skipped.
- **Urza, Academy Headmaster** (ust/136, upstream) audited against the official tables (Wizards Un-resources
  page and Urza-bilities_DARupdate.pdf, the PDF taking precedence). Fixed:
  - −6 #20 (gain 7 life, draw seven, put up to seven permanents) was scripted but missing from the random table,
    so −6 picked from 19 outcomes instead of 20.
  - −6 #19 gave one extra turn however many of the five coins came up heads; it now gives one per heads.
  - −6 #11 now forgets stale remembered objects before destroying, as Sorin, Lord of Innistrad's identical −6 does.
  - Description wording brought in line with the PDF on −1 #9 and −6 #7, #13, #17, #19.

### 2026-10-03 (deployed: desktop, Android, GitHub) — upstream merge (2 commits, data only); Unstable green complete and multicolor through Grusilda; audit of upstream Unstable scripts; open-ended die results; artists as people; tree-in-art; just a second; whole-card combination

- **Upstream merge** (2 commits: an SLD commander precon deck and FRA/FRC achievements, data only; no conflicts).
  Suite before the merge: 1019 run, 0 failed, 6 skipped.

- **Clever Combo** (ust/105, first green Unstable card; not scripted upstream). Script only: a tutor whose
  `ChangeType$ Host,Card.withAugment` is Labro Bot's host-or-augment filter. Scryfall lists no rulings.
- **Druid of the Sacred Beaker** (ust/106; not scripted upstream). Script plus a new creature type, **Deer**, in
  `TypeLists.txt` (Scryfall's creature-type catalog leaves out Un-only types, as with Hatificer). The mana count is
  Knight of the Widget's `Permanent.YouCtrl+Watermark_crossbreedlabs`; the Unstable FAQ's one ruling for the card
  (Contraptions' faction symbols count) is already covered, since the printing traits carry Contraption
  watermarks. A combined permanent counts once: no host, augment, or mutate printing has a watermark (Scryfall
  search, 2026-10-03), so a second card under one can't add another.
- **Half-Squirrel, Half-** (ust/111; not scripted upstream). Script only: the standard augment shape with the
  first negative adjustment, `PT:-1/-0` - `augmentAdjustment` and `CardFace.parsePT` both read it via
  `Integer.parseInt`, and the P/T box shows the printed `[-1 / -0]`. Its seven Scryfall rulings are the generic
  augment ones.
- **Audit of the upstream-scripted Unstable cards** (ust/1-111, the 47 cards skipped as already scripted, every
  printing and variant). Cost, type line, P/T, color indicator and Oracle text all match Scryfall; each script was
  read against its Oracle text and Scryfall rulings. One real bug: **"on an N or higher" die results** were
  scripted as closed ranges (Inhumaniac `5-6`), so a result pushed past the die's maximum (Snickering Squirrel,
  Squirrel-Powered Scheme, The Big Idea's combined dice) did nothing. `ResultSubAbilities$` now takes an
  open-ended `N+` key (`RollDiceEffect.resolveSub`); Inhumaniac and Lobe Lobber use `5+`, Dungeon Master `12+`
  (a d20 with a modifier can pass 20).
  - `InhumaniacTest` (1).
- **Hydradoodle** (ust/112, upstream) checked against Scryfall and the FAQ: matches, including X = 0 (no dice, dies
  as a 0/0).
- **Ineffable Blessing** (ust/113a-f, all six; not scripted upstream). Each variant chooses as it enters
  (`K:ETBReplacement`) and draws for creatures you control that match. A **Flavorful**/**Bland** (`hasFlavorText`,
  per printing, so Lexivore counts as the ruling says), B **an artist**, C **white-** or **silver-bordered**, D
  **a rarity**, E **odd** or **even** collector number, F **a number** of words in the name (0-25: the longest
  name in the pool is 25 words, and a face-down creature's is none). Choices with a trigger each are gated on the
  chosen mode with only the first printed (`Secondary$`). A face-down creature shows only a card back, so it has no
  flavor text, artist, border, rarity or collector number.
  - **Artists are people, not credit lines** (`ArtistCredit`): "A & B" credits both, and nicknames are dropped
    (ruling: "ignore all the various nicknames"), so `Rebecca "Don't Mess with Me" Guay` is Rebecca Guay. Applies
    to every choose-an-artist card: `ChooseArtist` offers individual names, `ArtistIsChosen` and `artIsBy` match
    any credited artist (Very Cryptic Command's `artIsBy Wayne England` now also finds `Wayne "King of" England`),
    and a combined (augmented) creature has every merged card's artist.
  - New card property `WhiteBordered` (printed border; borderless is neither). `Rarity*` treats a basic land as
    common (ruling: black expansion symbol). `nameWords_EQ` accepts an SVar.
  - AI: GenericChoice `AILogic$ MostMatchingCreatures` picks the choice whose `AIMatch$` the most creature cards
    in library and hand satisfy; ChooseArtist `MostCreaturesToCome`; ChooseNumber `MostCommonNameWordCount`.
  - `IneffableBlessingTest` (9).
- **Joyride Rigger** (ust/114, upstream) checked against Scryfall: matches, no rulings or FAQ entry.
- **Monkey-** (ust/115; not scripted upstream). Script only, the standard augment shape; its rulings are the generic
  augment ones.
- **Mother Kangaroo** (ust/116, upstream) checked against Scryfall: matches; its one ruling is the generic host one.
- **Multi-Headed** (ust/117; not scripted upstream). Script only: Half-Orc's intervening-if shape with
  `Count$YouRollThisTurn`, which counts every die you rolled, the planar die included.
- **Really Epic Punch** (ust/118; not scripted upstream). Script only: Savage Swipe's pump-then-fight with Success!'s
  `Card.Host,Card.withAugment` condition and `Duration$ Permanent`, following the three rulings (the +2/+2 never
  wears off; a combined creature still has augment; any creature you control can fight without the bonus).
- **Selfie Preservation** (ust/119; not scripted upstream). A new printing trait, `tree`: Scryfall Tagger's tree tag
  and everything under it, which follows the rulings and FAQ (dead and fallen trees count, Treefolk count, stumps
  don't and aren't under it), plus bonsai. 4,679 tagged arts; 291 of 375 basic Forest arts. A tagged tree goes
  straight onto the battlefield tapped; Tagger is incomplete, so for an untagged land the caster, who can see the
  revealed art, is asked (Goblin Haberdasher's rule: confirmed data first, a question only for the rest). The AI
  can't look, so it answers no. Search shape is Cultivate's.
  - `PrintingTraits.txt` regenerated (same Scryfall bulk files): only `tree`/`backtree` added, plus two rows for
    the Unfinity sticker-sheet edition upstream added after the last run.
  - `SelfiePreservationTest` (2).
- **Serpentine** (ust/120; not scripted upstream). Script only, the standard augment shape with a landfall
  condition.
- **Shellephant** (ust/121; not scripted upstream). `{0}` Charm activatable in any zone (_____'s
  `AdditionalActivationZone$` list); each mode is `Animate | Duration$ Perpetual`, so the definition follows the card
  between zones - the FAQ has you define it before it's on the battlefield, where an undefined 0/0 (ruling) dies
  first. "Turtle and/or Elephant" is a stack-free special action (`A:ST$`), changeable anywhere, anytime (FAQ); it's
  both until changed. AI: defines it once as a 3/3 (new CharmAi `AIPreferred$` mode marker, gated by
  `AICheckSVar$` on base toughness 0), never changes its types.
  - `ShellephantTest` (4).
- **Slaying Mantis** (ust/122; not scripted upstream).
  - **Just a second** (`Keyword.JUST_A_SECOND`): the ruling makes it split second (no spells, no abilities) plus not
    moving permanents, so `MagicStack.isSplitSecondOnStack` counts it; prints with its reminder text.
  - **The throw** reuses upstream's Chaos Orb/Falling Star simulation (`FlipOntoBattlefield`: aim at a card, land on
    it and/or a neighbor by chance) as an ETB replacement, with a new `Thrown$ True` that skips the flip-over roll
    (it's thrown; it always lands). With nothing on the battlefield it touches nothing (no longer an exception).
  - **The fight**: RepeatEach over the touched opposing creatures with `DamageMap$ True`, so each fight is
    one-on-one but all damage lands at once (the ruling's 3/3 + 4/4 + 5/5 example: all four die).
  - **Flip/throw AI** now aims at an opponent's permanent (it took the first battlefield card, often its own):
    the best creature, or with `AILogic$ ThrowFight` the best one the Mantis kills. Applies to Chaos Orb and
    Falling Star too.
  - New creature type **Wrestler**. `SlayingMantisTest` (4).
- **Squirrel Dealer** (ust/123; not scripted upstream). Script only: honor-system GenericChoice, as It That Gets Left
  Hanging - only a yes makes the Squirrel (FAQ: no reply isn't one); the AI has no one to ask, so no Squirrel.
- **Steamflogger Service Rep, Wild Crocodile, Willing Test Subject** (ust/124-126, upstream) checked against Scryfall
  and rulings: match. **Green Unstable complete.**
- **Baron Von Count** (ust/127, first multicolor; not scripted upstream). One real doom counter (counter removal takes
  it); the numeral it sits on is the chosen number, set by `ChooseNumber` with Min = Max (no prompt) and a new
  `NumberLabel$` so the panel reads "doom counter on: 4". New property `hasNumeral_<digit or SVar>`
  (`CardFactoryUtil.hasNumeral`): mana cost, text box with flavor text, printed power/toughness - not name, art,
  collector number or legal text (FAQ); 10 counts as 1 and 0 (ruling). Moving from 1 is a reflexive trigger
  (checked before the move, so 2 -> 1 doesn't fire it): the targeted player loses the game (rulings), back on 5.
  - **GameLossAi**: a targeted lose-the-game trigger outside combat aimed at the AI itself (the Phage path); it
    now aims at the strongest opponent.
  - `BaronVonCountTest` (4).
- **Better Than One** (ust/128) deferred by the user: needs a player joining mid-game and Two-Headed Giant rules,
  neither of which Forge has (same blocker as Splendid Genesis).
- **Cramped Bunker** (ust/129; not scripted upstream). Script only. The Bunker remembers what touches it (forgotten
  when it leaves the battlefield; each one shows "This permanent is touching Cramped Bunker."). Room for 8 (user:
  one per side and one per corner). "Can't" = full or nothing of theirs left outside, checked before anything
  moves; then everything they control outside is destroyed and the Bunker is sacrificed. The AI moves in its best
  permanent (what's inside survives the wipe).
  - `CrampedBunkerTest` (4).
- **Dr. Julius Jumblemorph** (ust/130; not scripted upstream). Every creature type in every zone (Mistform Ultimus's
  static). The host trigger is a searched `Augment` with a new `SearchZones$ Library,Graveyard`: the searcher picks
  "library and graveyard", "library only" or "graveyard only", so searching just the graveyard keeps the library
  unshuffled; `Shuffle$ True` shuffles only when the library was searched (found or not). AugmentAi takes the
  optional trigger when there's a card with augment to find. No rulings or FAQ entry.
  - `DrJuliusJumblemorphTest` (4).
- **The Grand Calcutron** (ust/131) deferred by the user (too complex; not scripted upstream either).
- **Grusilda, Monster Masher** (ust/132; not scripted upstream).
  - New `DB$ Combine` (`CombineEffect`, `CombineAi`): two target creature cards from graveyards are merged while
    still in the graveyard, then enter as one creature under your control, so both cards' enter abilities fire
    (two hosts both trigger, and the result is still a host an augment can go on - rulings). A host and a card
    with augment combine the augment way; a card with augment and a non-host keep augment with no host, and the
    existing state-based action sends both to the graveyard (ruling).
  - Whole-card combination (`Card.isCombinedWhole`, `CardFactory.getCombinedCloneStates`): summed power and
    toughness, both names (shown "A // B"; `Card.getNames` feeds `sharesNameWith` and the legend rule, which now
    indexes each name and won't put the same permanent away twice), combined mana cost, colors, types, abilities.
    Not done: two characteristic-defining P/T abilities aren't added together (FAQ) - the later one wins.
  - `Combined` card property (hers, host with augment, melded, merged) for her menace static.
  - **Merged permanents leaving the battlefield** now send each card to its own owner's zone (they all went to the
    top card's owner's): mattered for Grusilda's two graveyards and for augmenting another player's host.
  - AI: picks the two best creature cards, a host with an augment when it can, never an augment with a non-host.
  - `GrusildaTest` (4).

### 2026-10-02 (deployed: desktop, Android, GitHub) — upstream merge (12 commits, incl. Unfinity stickers); Unstable red complete (The Big Idea through Three-Headed Goblin); printed-line OCR; infinity; π damage; Party Crasher; triple strike; Jester's Sombrero AI

- **Upstream merge** (12 commits, among them Unfinity stickers and sticker sheets, Goblin Blastronauts, and the
  draw-from-the-bottom keyword becoming a `DrawFromBottom` static). Six conflicts, each kept both sides:
  `StaticAbility` (the fork's `GainsTextBoxOf` beside the sticker params), `PlayerControllerAi`
  (`chooseStringForEffect` beside the sticker choices), `GamePieceType` (`LIBRARY` and `STICKER_SHEET`),
  `CardView` (chosen expansion and player-counter info beside `updateStickers`), `RollDiceEffect` (The Big Idea's
  per-die `RolledDie` triggers now also pass upstream's `SourceSA`), and `Player` (drawing another player's library
  still ignores your own `drawsFromBottom()`). No fork-only script params were stripped. Suite after the merge:
  983 run, 0 failed, 6 skipped.
- **The Big Idea** (ust/76, first red Unstable card; not scripted upstream either). Its shield is an Effect
  (`Duration$ Permanent`, since the card has no "this turn") with a `RollDice` replacement that raises a new
  replacement value, `CombinedDice`. `RollDiceEffect.rollAction` rolls that many extra dice for the first die
  and sums them into one result (`DieRollResult.getCombinedDice()`), so the total feeds everything that reads
  the result, and the log shows "Rolled 2 dice for one: 3 + 3 = 6". Two shields both apply to the same next
  roll (614.5), making it three dice for one. Following the three Scryfall rulings (2018-01-19): `RolledDie`
  triggers fire once per physical die with that die's own value (Chittering Doom doesn't see the total), and a
  roll-two-dice-and-use-the-difference ability gets only one die replaced. `Number$` (Resolute Veggiesaur)
  and "dice rolled this turn" count the physical dice. The AI shields at the opponent's end step
  (`AILogic$ AtOppEOT`), when tapping its Brainiacs costs nothing.
  - `TheBigIdeaTest` (7). Suite: 904 run, 0 failed, 6 skipped.
- **Garbage Elemental A, E and F** (ust/82a, 82e, 82f; B, C and D were upstream and match Scryfall). The edition's
  82a and 82e-f printings no longer load as an unsupported variant.
  - **A** — Frenzy 2 and "can't be blocked by wordy creatures". Frenzy had a `Keyword` entry but no ability and
    no card using it; it now triggers on `AttackerUnblocked` and prints with its reminder text.
  - **E** — Unleash, and creatures you control with any kind of counter have **art menace**: new keyword
    `Art menace`, a `CantBlockBy` static. Art Tagger counts as one figure can't block; untagged art is left to
    the players (as with Sly Spy's facing), except that the AI doesn't block with it.
  - **F** — last strike and a Battalion trigger (target creature can't block this turn).
- **"Wordy" and "most lines of text" now come from the printed card.** Garbage Elemental's wordy is "four or
  more lines of rules text"; the Unstable FAQ's Silver Rule says to look at the actual card, and how text wraps
  isn't in any dataset. `_tools/rules-lines` reads every face of every English paper printing Forge has (92,407)
  off Scryfall's card images with RapidOCR on the GPU, and `PrintingTraits.txt` now carries the counts:
  - Rows below the type line, up to the artist/copyright footer, minus the rows matching that printing's flavor
    text. 90,842 faces read (98.3%); Sagas, Rooms, Classes, Cases, flip and level-up cards, planes, schemes and
    other layouts whose text isn't one block are left unread, and unread never restricts anything.
  - Checked by eye on 54 sampled faces across frames and card types: rules lines exact on all 54 after the one
    miss was fixed ("+1/+1." on its own line had been dropped as a P/T box). Profiling the failures caught
    "Wizards" in rules text taken for the copyright line, and instants and sorceries counted from the wrong row
    (15,851 corrected); comparing flavor rows with Scryfall's flavor text caught trailing artist names counted
    as flavor (471). About 0.2% of flavor-bearing faces still miss a flavor line the OCR didn't detect.
  - **Replaces the fork's Oracle-text estimate** (`CardFactoryUtil.getTextBoxLineCount`, 40 characters a line,
    removed): Frazzled Editor's protection from `wordy` and Lexivore's `mostLinesOfText` read the printed
    counts too. Lexivore may target an unread permanent; the AI goes by known counts only.
  - `GarbageElementalTest` (7), `PrintedLinesTest` (3); `TextBoxTest` loses its three estimator tests. Suite: 911 run, 0 failed, 6 skipped.
- **Goblin Haberdasher** (ust/83; not scripted upstream). New creature type **Hatificer**. "Wearing hats in their
  art" (`Creature.HatInArt`) is confirmed by the printing data or, failing that, asked:
  - **Confirmed** (`hat` in PrintingTraits, 1,116 printings): worn headwear in Tagger's hat tree - helmets and
    crowns included, per the FAQ's "garment worn on the head that's not part of another garment" - on art tagged
    as one figure, since a background character's hat doesn't count (rulings) and Tagger doesn't say who wears
    it. Not counted: a helmet being held, sticker sheets, "not-a-hat", and the plain `helmet` tag alone (8ED
    Seasoned Marshal carries hers and is tagged `helmet`). Checked by eye on 12 random printings: all wearing one.
  - **Asked** otherwise, honor system and only when it matters: right after attackers are declared, the
    controller of each attacker a hat would give something to is asked once (`GameAction.askHatInArt`). The answer
    is about the physical card, so like a signature it stays with the card through every zone. The AI can't look,
    so it never claims a hat.
  - `GoblinHaberdasherTest` (5). Suite: 916 run, 0 failed, 6 skipped.
- **Half-Orc, Half-** (ust/84; not scripted upstream). The first augment whose unfinished condition has an
  intervening "if" (`CheckSVar$` with `PlayerCountRegisteredOpponents$HasPropertywasDealtDamageThisTurn`); the
  combined trigger keeps the condition and its SVar, so no engine change was needed. `HalfOrcTest` (3, with
  Adorable Kitten as the host). Suite: 919 run, 0 failed, 6 skipped.
- **Hammerfest Boomtacular** (ust/87; 85 Hammer Helper and 86 Hammer Jammer were already upstream). Script only:
  a `SpellCast` trigger on `Card.Watermark_goblinexplosioneers`, the printing's watermark from PrintingTraits.
  `HammerfestBoomtacularTest` (2, casting Goblin Haberdasher's UST printing and Grizzly Bears).
- **Infinity Elemental** (ust/88; not scripted upstream) and **infinity** throughout the engine - see the *Infinity*
  index entry. Built to the five Scryfall rulings (2020-02-29) and the Unstable FAQ: its power ties with another
  Infinity Elemental's, lifelink gains infinite life, infinite life survives even infinite damage, setting life works,
  and drawing from an empty library still loses. User decisions (2026-10-02): an infinite number of objects or
  repetitions becomes a finite number the controller picks; X = ∞ is allowed with infinite mana, and Mox Lotus works
  the same way (checked: it pays X = ∞, and ∞ is offered before it's tapped). Mox Lotus's infinite colorless now goes
  through the same per-color pool flag. The desktop text-only card renderer now uses the P/T display strings, so it
  shows half and infinite values too.
  - `InfinityTest` (12, including an AI turn with it on the battlefield). Suite: 933 run, 0 failed, 6 skipped.
- **It That Gets Left Hanging** (ust/89; not scripted upstream). Script only, honor system like Skull Saucer: the
  haste is an `UnlessCost$ FlavorAction` the player pays by confirming the high five - or that there was no one to
  ask, since then it doesn't gain haste (rulings). The AI has no one to ask, so it never gains haste.
  `ItThatGetsLeftHangingTest` (2).
- **Just Desserts** (ust/90; not scripted upstream) and **π damage**. `NumDmg$ 3 | Pi$ True`: the whole 3 goes
  through damage as usual (prevention, redirection, triggers), and `GameAction.dealDamage` marks the π - 3 left over
  on wherever the 3 actually landed, which is how the rulings' redirected-to-a-player case gets its 3.14.
  - Creatures count it exactly (`Card.addPiDamage`, `getExactDamage`; the lethal check compares the real number), so
    the rulings and FAQ hold: one destroys toughness 3, two 6 (not 6 1/2), four 12 1/2 (not 13), eight 25.
  - Players take .14 more per π ("use 3.14"): 20 life becomes 16.86. Life's fraction is now kept in hundredths
    (`Player.lifeHundredths`, replacing the 0-or-1 half): a half is 50, and the half-life API reads the same as
    before. `changeLifeByHundredths` carries like `changeLifeByHalves`; 704.5a counts any fraction as more than 0.
  - Doubled or tripled damage doubles or triples the π; added or partly prevented damage keeps one; fully prevented,
    none. Damage both doubled and then partly prevented is read as one π. π damage to a planeswalker or battle removes
    the whole 3 counters only, since counters are whole.
  - Display: card damage "3.14" / "6.28", life "16.86", "16.36" from 19 1/2, "-0.14" just below zero; the mobile
    life-change label counts in hundredths ("-3.14"), and the mobile life label shrinks its font to fit totals
    longer than two digits. `PlayerView.getLifeHundredths`, trackable `LifeHundredths` and `PiDamage`.
  - Merges: `Pi$` is a fork-only script param the engine depends on - check upstream's linter didn't strip it.
  - `JustDessertsTest` (7). Suite: 942 run, 0 failed, 6 skipped.
- **Jester's Sombrero AI.** It cast the Sombrero and activated it at players with no sideboard. Now:
  - New deck flag `AI:RemoveDeck:NoSideboard` (`CardAiHints.getRemNoSideboardDecks`): out of generated decks for games
    without sideboards - random constructed, Commander and themed decks, through `IS_KEPT_IN_RANDOM_DECKS` and
    `CardThemedDeckBuilder` - but kept by Limited, whose decks have the rest of the pool as a sideboard.
  - ChooseCard `AILogic$ TargetHasChoices`: target only a player with something to choose, and the one with the most
    (a targeted "choose up to N" with MinAmount 0 otherwise counted zero as enough).
  - `SVar:NeedsToPlayVar` on opponents' sideboard cards, so it isn't cast with nothing to raid.
  - `JestersSombreroTest` (3). Suite: 945 run, 0 failed, 6 skipped.
- **Party Crasher** (ust/92; not scripted upstream) - a second player attacking in someone else's combat. New static
  mode `AttackDuringOpponentsTurn` (`StaticAbilityAttackDuringOpponentsTurn`).
  - In the declare attackers step, after the active player declares, each of their opponents in turn order may
    declare such creatures (`PhaseHandler.declareOpponentsTurnAttackers`; the choice is
    `PlayerController.chooseAttackDuringOpponentsTurn`, an optional pick of what it attacks). It attacks any of its
    controller's opponents (user, 2026-10-02), so `Combat.addDefender` adds the active player, their planeswalkers or
    battles when needed. Attack costs and tapping work as for any attacker. Since it's offered once per declare
    attackers step, it's once each combat.
  - Blocking (rulings): the player it attacks blocks after the others (declare blockers already ends with the active
    player). An untapped attacking creature can block it, and the Crasher, if untapped, can block back. Two creatures
    blocking each other deal combat damage to each other once, as attackers. The human block input picks out only
    other players' attackers, so your own untapped attackers can be chosen as blockers, and removing one of them
    removes only the block. Removing a creature that's both attacking and blocking takes it out of both.
  - Combat damage from an attacker is now assigned by its controller, not by the active player.
  - "Whenever you attack" (AttackersDeclared) now fires once for each player who attacked, with their own
    attackers. A player's "attacked this turn" lists now clear at every cleanup, not only on their own turn, so an
    attack on an opponent's turn doesn't count toward their next turn. "Attacked this combat" clears for everyone.
  - AI: it stays home when it's needed to block - its life is in danger, or it can block an attacker, kill it and
    survive - unless it has vigilance. Otherwise it attacks the opponent with the least life among those with nothing
    able to block and kill it. `lifeInDanger` no longer assumes the active player can't be attacked, and the AI's
    blocker reset only undoes the block of a creature that's also attacking.
  - `PartyCrasherTest` (5). Suite: 950 run, 0 failed, 6 skipped.
- **Super-Duper Death Ray** (ust/97; not scripted upstream; 93-96, the Steamfloggers, are upstream and match Oracle).
  Trample on an instant: `K:Trample:Spell`, whose title is plain "Trample" with the reminder "This spell can deal
  excess damage to its target's controller.", and which instant and sorcery text now prints. The damage is the existing
  `ExcessDamage$ TargetedController` (as on Flame Spill), conditioned on the spell having trample. Per the rulings,
  marked damage counts toward lethal, and only the creature is targeted, so a hexproof controller still takes the
  excess.
  - `SuperDuperDeathRayTest` (4). Suite: 954 run, 0 failed, 6 skipped.
- **Three-Headed Goblin** (ust/99; not scripted upstream; 98a-d Target Minotaur, 100 and 101 are upstream) and
  **triple strike**: new `Keyword.TRIPLE_STRIKE` ("Triple strike", printed with its reminder text as on the card).
  The Unstable FAQ says double strike + last strike = triple strike, so `Card.hasDoubleStrike()` and
  `hasLastStrike()` both include it, and the last-strike step deals its third hit. It has to survive each step to
  strike again (ruling): a 3/3 first striker trades with it in the first-strike step, and unblocked it deals 9.
  - AI: `Card.getCombatDamageStrikes()` (3, 2 or 1, also counting first + last strike as 2) replaces the "double
    strike = x2" in damage and poison predictions, attack planning and blocker damage. Creature evaluation values
    triple strike above double strike, and first-strike-safe blockers include it.
  - Every Unstable printing up to 101 now has a script, which completes red.
  - `ThreeHeadedGoblinTest` (4). Suite: 958 run, 0 failed, 6 skipped.

### 2026-10-01, fourth build (deployed: desktop, Android, GitHub) — Deck Editor augment P/T; Kefnet's Monument deck gen and AI; MustBeBlockedByAll text

- **Augment P/T in the Deck Editor.** Display-only cards (negative id) get no keywords, so the augment check
  missed them and Zombified still read 0/0 there; `CardView` now also asks the printed face
  (`CardFactory.isAugmentFace`).
- **Random color decks honor a colorless card's `DeckNeeds:Color$`.** Kefnet's Monument (blue creatures)
  landed in a red/black deck: colorless cards were let in unchecked. `DeckGeneratorBase` now requires one of
  the needed colors (new `DeckHints.getColors()`); colorless cards with no color need are unaffected.
- **"Doesn't untap during its controller's next untap step" AI.** Kefnet's Monument, Fogwalker and Skyline
  Cascade lacked `IsCurse$ True`, so the AI treated the effect as a buff and forced it onto the opponent's
  worst creature, tapped or not. And as a curse, `PumpAiBase` only valued it from main 2 on the AI's turn, so
  a main-1 creature cast fell back to the best creature; now any tapped creature that can untap is the
  useful target, whenever it lands (it stays tapped until that untap step either way).
- **Marble Priest / Talruum Piper text.** `MustBeBlockedByAll:<valid>:<description>` printed the whole
  keyword as card text (upstream too); only the description prints now.
- **Tests:** `AugmentTest` gains a Deck Editor (display-only) Zombified check; new `DoesntUntapCurseAiTest`,
  `ColorlessDeckNeedsTest` and `MustBeBlockedByAllTextTest`.

### 2026-10-01, third build (deployed: desktop, Android, GitHub) — augment P/T fix

- **Fixed the second build's augment P/T box breaking every card's display.** An unset tracked string reads
  back as `""`, not null, so every card off the battlefield got an empty `[]` P/T box, and the card image
  renderers threw splitting it - only the first hand/library card drew and the stack flickered endlessly.
  `getAugmentPT()` now treats empty as "not an augment"; `AugmentTest` checks a non-augment in hand and a
  drawn augment.

### 2026-10-01, second build (deployed: desktop, Android, GitHub) — augment P/T box

- **Augment P/T box shows the adjustment.** Off the battlefield an augment card (Half-Kitten, Half-; Ninja; ...)
  is a 0/0, so its P/T box read "0/0". It now shows the printed adjustment bracketed like a Vehicle's,
  `[+1/+2]`, in the battlefield/hand card overlay, the card detail panel, and mobile's card and list renderers
  (desktop's drawn card image shows `+1/+2` unbracketed, as it does Vehicles). On the battlefield, combined or
  alone, the real P/T shows as before. New tracked `CardStateView.getAugmentPT()` (null on the battlefield).

### 2026-10-01 (deployed: desktop, Android, GitHub) — upstream merge (15 commits, 2.0.16-SNAPSHOT); Unstable black complete (Dirty Rat through Zombified); functional keyword text restored; doubled cost words; AI fixes

- **Upstream merge 2026-10-01** (15 commits): loading decks from a URL in the deck importer (with ghost text
  listing the four supported providers), Adventure New Game+ offering the main quest again, better AI choices for
  Memories Returning, a TargetingOverlay update, an adventure data backup update and an "MTG Card Fetch Bot"
  GitHub workflow. No conflicts.

- **Functional keyword text restored** (found 2026-10-01: Amber Prison's untap choice was gone). The 2026-09-30
  in-game text retemplating rewrote K: lines to Oracle wording, but a K: line is engine input: its head is
  parsed into a `Keyword`, and sentence keywords are found by their exact text from Java
  (`hasKeyword("You may choose not to untap CARDNAME during your untap step.")`). 154 lines on 152 cards had
  silently stopped working:
  - 107 sentence keywords on 105 cards: optional untap (42), must be blocked if able (11), lure (10), ante
    removal (9, so ante cards stayed in decks for games not played for ante), can't attack or block alone and
    its variants (13), damage prevention (9), can't attack or block (2), and one-offs on Okk, Orcish
    Conscripts, Scarred Puma, Panglacial Wurm, Ogre Enforcer, Nacatl War Pride, Butcher Orgg, Cunning Giant and
    Xenosquirrels (its die modification).
  - 46 `Start your engines!` lines no longer parsed as the keyword, so none of those cards gave speed.
  - Collective Brutality's escalate cost had become `Discard.<1/Card>`, which parsed as Escalate {0}.

  All are back to upstream's exact text, which also keeps those lines out of merge conflicts. The Oracle
  wording now comes from the display: `CardFactoryUtil.keywordAsPrinted` puts in "this artifact", "this card"
  or a legendary card's short name, but only when the card's own Oracle text prints that sentence. A full
  re-render: 0 faces newly drifting, 5 more matching Oracle (the ante line now shows on ante sorceries, plus
  Collective Brutality's cost and Gorm's and B.O.B.'s short names).
  - Guard: `KeywordScriptTextTest` fails on any K: line whose keyword head has print punctuation added, whose
    cost type has punctuation glued on, or whose self-reference was reworded away from a string the engine's
    Java looks up. Run against the broken scripts, it flagged the same 153 sentence and keyword lines as the git
    audit (`_tools/oracle-audit/k-line-audit.js`). `FunctionalKeywordTextTest` (9) covers Amber Prison,
    Hivis, a lure, must-be-blocked, ante removal, Xenosquirrels, speed and Collective Brutality. Suite: 882 run,
    0 failed, 6 skipped.
- **Doubled words in in-game cost text** (reported on Spire Mechcycle: "Tap another untapped another Mount or
  Vehicle"). A script's cost description that already said what the engine adds printed twice: "untapped" and
  "another" (`CostTapType`), "a"/"an" (`CostSacrifice`), "card" (`CostDiscard`). The engine now leaves out
  what the description already says. Also a double space after "Exhaust —", "Boast —", "Power-up —" and other
  ability words (from my 2026-09-30 keyword-dash change), and two scripts (Threefold Thunderhulk "another
  artifact", Saruman of Many Colors's ward cost with Oracle's serial comma). A full re-render fixed 15 faces
  and broke none; no doubled words are left. Component Pouch's d20 rows were already restored by the VERT fix
  in the 2026-09-30 deploy.
- **AI fixes from play:**
  - Vibranium Strike Gauntlets (and any "attach it to target creature" Equipment or Aura): an attach trigger
    with no creature to target no longer reads as worth it, so the AI doesn't cast it with no creature out.
    Flash Equipment that attaches as it enters now gets the flash-Aura timing (`AttachAi.doAdvancedFlashAuraLogic`):
    a combat trick on a creature that survives, or a main-phase play, never a buff for a creature that's about
    to die. The attach target skips our creatures that are dying in the current combat when there's another
    choice. That survival check also counts damage already marked and blockers now (it compared power with
    toughness using `>`, so exactly lethal damage read as survivable).
  - Terror of Mount Velus: an enters trigger that pumps our creatures until end of turn (`PumpAll` of
    `...YouCtrl` with keywords or +power) is now a reason to cast precombat when we have a creature that can
    attack (`ComputerUtil.castPermanentInMain1`).
  - Signal Pest: once attackers are chosen, a creature that deals no damage and whose only attack value is
    boosting other attackers (battle cry, "other attacking creatures get") goes home if nothing else attacks
    (`AiAttackController.pruneSupportOnlyAttackers`).
  - Hand-size creatures (Maro, Masumaro, Sturmgeist, Psychosis Crawler, Body of Knowledge, Kagemaro...): with one
    out, a card from hand is played only if it's worth the shrink (`HandSizeAi`, new decision
    `KeepInHandForSize`): removal and other interaction, cards that draw two or more, creatures worth more
    than the lost points, planeswalkers, a land when short of mana for something in hand, a combat trick
    during combat. Anything goes when the hand is over its maximum size or the AI's life is in danger.
  - `AiPlayReportsTest` (6). Suite: 839 run, 0 failed, 6 skipped.
  - Flaky test fixed: `testHandSizeCreatureKeepsTheHand` failed about half the time on "Lightning Bolt is
    played". `HandSizeAi` passed the Bolt every time. The failures came from upstream's
    `ComputerUtilCard.useRemovalNow`, which deliberately rolls dice on holding instant removal: on the AI's
    own main 2, Bolt on a Hill Giant has a value of 0.5, so it was cast 94 times in 200 calls. That's a
    reasonable hold, so the AI is unchanged. The test now checks `HandSizeAi` directly in main 2, and
    checks the full decision at the opponent's end step, where the tempo value doubles to 1.0 and the roll
    always passes (200 of 200).

- **Zombified** (ust/75), the last black Unstable card. Two pieces of augment the fork hadn't built:
  - Combining from the graveyard: `AB$ Augment | ActivationZone$ Graveyard | FromZone$ Graveyard` ("{4}{B}:
    Combine this card from your graveyard with target host."). `AugmentEffect` takes `FromZone$` (default Hand).
  - Activated conditions: an augment's "{2}{B}, Exile a creature card from your graveyard:" is a placeholder
    ability marked `AugmentCondition$ True` (never usable alone); `CardFactory.getAugmentedCloneStates` replaces it
    with an ability of that cost whose effect is the host's enters effect, and the combined Oracle line gets the
    effect capitalized after the colon. Steam-Powered (red) can use the same. Zombified + Adorable Kitten is
    "Zombified Kitten", 3/3, "{2}{B}, Exile a creature card from your graveyard: Roll a six-sided die. You gain life
    equal to the result."
  - A cost with an empty `SpellDescription$` no longer leaves a trailing space (`SpellAbility.rebuiltDescription`);
    `CardState.removeSpellAbility` added. `ZombifiedTest` (2). Suite: 892 run, 0 failed, 6 skipped.

- **Summon the Pack** (ust/74). Upstream's `MakeCard | Booster$ True` (Booster Tutor's random Forge booster) gains
  `PutAll$ True`: every card in the pack that passes `Filter$`, duplicates included, with nothing to choose. Here
  every creature card goes onto the battlefield (their enters abilities trigger, so the FAQ's Phage the
  Untouchable really does lose you the game), and an `Animate` makes them Zombies for as long as they stay. A pack
  with no creature cards does nothing (FAQ). `Reveal$ True` on a booster now shows the whole opened pack to
  everyone, as "reveal the cards" says, for Booster Tutor too (it used to show only the card taken).
  `SummonThePackTest` (2). Suite: 890 run, 0 failed, 6 skipped.

- **Squirrel-Powered Scheme** (ust/70; 71 Steady-Handed Mook and 72 Stinging Scorpion were already upstream and
  match Scryfall). New static mode `IncreaseRollResult` (`ValidPlayer$`, `Amount$`): each die result that player
  rolls goes up, after rerolls and swaps and before Snickering Squirrel is offered; the natural roll is unchanged.
  Several add up. `RollDiceEffect.rollResultIncrease`. `SquirrelPoweredSchemeTest` (2). Suite: 888 run, 0 failed,
  6 skipped.

- **Subcontract** (ust/73). The person outside the game is simulated as for Defective Detective: a random nonland
  card from the target opponent's hand, likelier the more impactful it is (`ChooseCard | AtRandom$ ThreatWeighted`),
  which that player discards. Script only. `SubcontractTest` (2: over 200 casts a land is never picked and Shivan
  Dragon is picked more often than Grizzly Bears).

- **Spike, Tournament Grinder** (ust/69). Outside the game is the sideboard, as for the Wishes. "Banned or restricted
  in a Constructed format" is `res/lists/BannedOrRestricted.txt` (card property `everBannedOrRestricted`,
  `forge.card.BannedOrRestricted`), built by `_tools/spike-list/generate.js`: the Unstable FAQ's list as of December
  2017 (193 names, all matched), every card Scryfall marks banned or restricted on 2026-09-30 in a format Wizards of
  the Coast runs (Standard, Pioneer, Modern, Legacy, Vintage, Pauper, Historic, Timeless, Alchemy, Brawl, Standard
  Brawl, Competitive Brawl, Commander: 377 cards), legendary cards first printed in Legends, Ice Age or Homelands
  (79), and conspiracies (29, per the FAQ). 556 names in all. Ante cards are on it but, as the FAQ says, can't be in a
  game that isn't for ante. Not covered: bans since 2018 that were lifted or whose cards rotated out (Scryfall only
  has current legality), e.g. Oko's 2019 Standard ban (it counts anyway through Pioneer and Modern).
  - `SpikeTournamentGrinderTest` (2). Suite: 884 run, 0 failed, 6 skipped.

- **Snickering Squirrel** (ust/68). New static mode `TapToIncreaseRoll` (not a keyword-text match, so a wording
  change can't break it): once a die's result is final, `RollDiceEffect.tapToIncrease` offers each untapped
  creature with it to its controller, in turn order and again after every increase, so several can go on one
  die and a 6 can become a 7 (rulings). It isn't a {T} ability, so a Squirrel that just arrived can be tapped. The
  AI raises only its own rolls. Note that upstream scripts which encode "N or higher" as a range ending at the
  die's top face (Inhumaniac's 5-6) won't count a raised 7.
  - `SnickeringSquirrelTest` (3, one a real Sly Spy roll in an AI combat). Suite: 870 run, 0 failed, 6 skipped.

- **Sly Spy** (ust/67a-f), all six printings (upstream had only F, the die roll; its text now says "this creature"
  like Oracle). Each is "Whenever this creature deals combat damage to a player, ...":
  - **A** reveals the hand and discards a card with the longest name, counting characters without spaces
    (ruling). New card property `longestNameInHand`.
  - **B/D** destroy a creature facing left/right in its art. Facing comes from Scryfall Tagger's `left-facing`
    and `right-facing` tags, now in `PrintingTraits.txt` (`facingleft`/`facingright`, plus back faces) and
    exposed as `Card.getArtFacing()` and the properties `FacingLeft`, `FacingRight`, `FacingUnknown`. Only 4,090
    of 26,580 creature artworks are tagged (45 both ways, which counts as unknown), so an untagged creature stays
    a legal target for the player to judge from the art (honor system); the AI targets only known facings
    (`AITgts$`). Right means the viewer's right (FAQ; Tagger agrees).
  - **C** honor system: a "Lost Finger" effect for that player, lasting until Sly Spy leaves the battlefield.
  - **E** puts the top card of that player's library into your hand (still theirs: `HandOf$ You`) and you lose
    life equal to its mana value.
  - `_tools/printing-traits/generate.js` writes the facing traits; regenerated from the same bulk data, every
    other trait is unchanged (7,981 new rows, facing only). `SlySpyTest` (7, each variant played through an AI
    combat). Suite: 867 run, 0 failed, 6 skipped.

- **Skull Saucer** (ust/66), honor system. Its enters trigger destroys the target, then asks you to put your head
  on the table (`UnlessCost$ FlavorAction`): decline and it's sacrificed, as the ruling says (the creature is
  still destroyed). Later, "I lifted my head" sacrifices it; that line reads exactly like Oracle via
  `CostDesc$`/`SpellDescription$`. The AI keeps its head down for good. Rulings and FAQ: any part of your head,
  any playing surface, and nobody may physically lift yours.
  - `AILogic$ Never` is now honored for every API (`SpellAbilityAi.canPlayWithSubs`, next to `Chance.N`):
    APIs that override `canPlay` (Sacrifice among them) never reached `checkAiLogic`, so the AI would have lifted
    its head. Upstream scripts that use `Never` on an activated ability or spell now really never do it.
  - `SkullSaucerTest` (2). Suite: 860 run, 0 failed, 6 skipped.

- **"Rumors of My Death . . ."** (ust/65). Script on the existing per-printing `Watermark_` property: the cost
  exiles `Permanent.YouCtrl+Watermark_leagueofdastardlydoom` (itself included: it has that watermark), the
  effect returns a `Permanent.YouOwn` card with it from the graveyard, chosen on resolution. Contraptions count
  (the Unstable FAQ treats their faction symbol as a watermark, and Scryfall records it). No Scryfall rulings or
  FAQ entry. Watermarks follow the printing, so Hoisted Hireling's Unsanctioned reprint (no watermark) doesn't
  qualify.
  - AI (`ChangeZoneAi` logic `ExileAndRetFromGrave`): only when the best returnable card is worth more than the
    cheapest permanent it would exile (`ComputerUtilCard.evaluateCardImpact`), in main 2 or at an opponent's end
    step. The cost is paid with that same card (`ComputerUtil.leastValuableToExile`), keeping Rumors unless it's
    the only choice. The default AI had it backwards: it exiled by lowest power (Rumors itself first) and would
    trade down. `RumorsOfMyDeathTest` (4). Suite: 858 run, 0 failed, 6 skipped.

- **Over My Dead Bodies** (ust/63). Graveyard creatures fight on Masterful Ninja's dual residency, with the
  graveyard as the second zone:
  - Declaring: as attackers (the attacking player's graveyard) or blockers (a defending player's, only when a
    graveyard creature attacks them) are declared, creature cards there are put onto the battlefield unnoticed
    and stay listed in the graveyard (`GameAction.enlistGraveyardCombatants`); the ones not declared go straight
    back. One that state-based actions would remove at once (0 toughness, a legend-rule clash) can't be declared
    (ruling). A card in the graveyard since before this turn isn't summoning sick; "creature cards in your
    graveyard have haste" covers the rest.
  - In combat they're treated as on the battlefield (rulings): lords apply, Giant Growth can target them, attack
    triggers fire, and nothing notices them enter or leave. `CantBlockBy` statics and the new card property
    `GraveyardCombatant` keep graveyard and battlefield creatures from blocking each other.
  - Destroyed (lethal damage, Murder) a graveyard creature is just removed from combat; sent anywhere else
    (Unsummon, exile) it goes from the graveyard. Undeathtouch is a `DamageDone` replacement that exiles the
    creature card instead. At end of combat, or once Over My Dead Bodies is gone, they're just dead again (a
    state-based sweep plus `PhaseHandler.endCombat`).
  - New static mode `GraveyardCombat`; `Card.isInZone(Graveyard)` sees the graveyard listing. The residency
    helpers Masterful Ninja used are now zone-generic.
  - The AI attacks and blocks with graveyard creatures through its normal combat code, but when it attacks it
    doesn't foresee the defender's graveyard blockers (they only show up in the block step).
  - `OverMyDeadBodiesTest` (4, one a full AI turn). Suite: 854 run, 0 failed, 6 skipped.

- **Old-Fashioned Vampire** (ust/62), honor system. Whether it's dark outdoors is one fact for the whole game:
  any player may say "It's dark out" (a `Dark Outdoors` effect in the command zone) or "It's light out"
  (removes it), and every Old-Fashioned Vampire reads it (`IsPresent$ Effect.namedDark_Outdoors`). Only the
  claim that changes things is offered. The AI never says, since it can't look outside. No Scryfall rulings
  or FAQ entry. New creature type **Vampyre** (as printed; plural "Vampyres") in `TypeLists`.
  `OldFashionedVampireTest` (2). Suite: 850 run, 0 failed, 6 skipped.

- **Ninja** (ust/61), an augment. "You may activate this card's augment ability any time you could cast an
  instant": a `CastWithFlash` static from hand (`EffectZone$ Hand`) with `ValidSA$ Activated.Augment` (new SA
  property `Augment`). Combined with a host it's "Ninja <host's last word>" (Ninja Kitten), and the host's
  effect finishes "Whenever this creature deals combat damage to a player,". Augmenting an attacking host
  leaves it attacking.
  - `K:Augment:<cost>:<sentence>`: an optional third field replaces the reminder's "Augment only as a
    sorcery." (Ninja: "Augment only as—oh, never mind.").
  - Every augment card printed its raw keyword (`Augment:3 W`) above its augment line in game; it no longer
    does.
  - AI (`AugmentAi`): an augment it may activate at instant speed goes on an unblocked attacking host after
    blockers are declared; it waits while it has a host that would attack, and otherwise augments in main 2
    or at an opponent's end step. `NinjaAugmentTest` (4). Suite: 848 run, 0 failed, 6 skipped.

- **Masterful Ninja** (ust/60). "Reveal this card from your hand: Masterful Ninja is on the battlefield and in your
  hand until end of turn." Built on the dual residency Yet Another Aether Vortex uses, with the hand as the
  second zone: the card really is on the battlefield (a permanent with haste, its pump works, it attacks and
  blocks) and is still listed in its owner's hand (hand size counts it, it can be chosen to discard).
  - Rulings and FAQ: nothing notices it enter or, at end of turn, leave (new `AbilityKey.Unnoticed` move);
    moving it between battlefield and hand does nothing, so Unsummon leaves it in both; destroyed or discarded,
    it's in the graveyard only. Discarding it to hand size at cleanup happens before it goes back (CR 514.1
    before 514.2), as in paper. A control change no longer ends a second residency (this also fixes a stolen
    Vortex permanent dropping out of its owner's library).
  - Not offered: casting it from your hand while it's in both zones (rules-legal; it would leave the
    battlefield for the stack), or revealing it again then (does nothing).
  - AI (`AlsoOnBattlefieldAi`): on its own turn before combat when the Ninja would attack; on an opponent's turn
    once attackers are declared, to block one it beats or when the attack is dangerous. Never otherwise.
  - `MasterfulNinjaTest` (5).

- **Hoisted Hireling** (ust/58), honor system on the Standing Army idiom: its controller lifts it (a `HOISTED`
  counter) and sets it back down with two free abilities, and it has flying while the counter is on it. No
  Scryfall rulings and no entry in the Unstable FAQ. Script only.

- **Hazmat Suit (Used)** (ust/57), honor system: the touch is an ability any player may activate to own up
  (`FlavorAction`, as on Vile Bile) and lose 2 life. The prompt notes that the sleeve counts (ruling); touching
  the Aura itself isn't offered (ruling). The ability reads exactly as Oracle does, its wording carried by
  `CostDesc$`/`SpellDescription$`.
  - New AI param `AIActivator$ <player>`: an ability any player may activate that the AI only considers when
    it matches. Here `Player.controlsCreature.EnchantedBy`, so only the AI that handles the enchanted creature
    ever "touches" it (10% of its turns, in its own main phase, per `Chance.10`).
  - Vile Bile now reads like its Oracle sentence in game too (its AI is unchanged). `HazmatSuitTest` (2).
    Suite: 833 run, 0 failed, 6 skipped.

- **Hangman** (ust/56). The game keeps the hangman sheet, so nothing here is honor system except that the
  noted word is a real word.
  - Noting: a person types any word of six to eight letters (A-Z only; re-asked if it isn't, and offered the
    given one from the list, told only to them, if they give none); the AI takes a random everyday word from
    `res/lists/Words.txt`. Everyone is told the length (ruling). The word is held in `chosenType` but kept out of `CardView`:
    the detail panel shows `(chosen word: _ A _ _ E _ - wrong: Q, X)`.
  - Guessing: `{1}`, any player may activate, target is `Player.!controlsCard.Self`. The guesser picks an
    unguessed letter from a list or "Guess the word" and types it. A right letter fills in every instance; a
    letter is never offered twice (rulings). A wrong guess resolves `GuessWrong$` (+1/+1 counter). The guess
    that finishes the word runs `WordGuessed`, a real trigger of Hangman's that sacrifices it.
  - AI (`GuessWordAi`) plays from the sheet, never from the word: it matches the pattern, wrong letters and
    wrong words against the list and guesses the letter most matches share (an everyday word counting four
    times a rare one), or the word when one match is left; with no match it goes by English letter frequency. It reads the word itself only if it noted it
    (ruling: the noter keeps answering for the word, and knowing it, after a control change). With its own
    Hangman it makes an opponent guess once a turn (second main phase or the opponent's end step) while three
    or more letters are hidden; against another player's it pays only when its guess will finish the word.
  - New: `forge.util.WordList`, `Card.setGuessableWord` and friends (copied by `CardCopyService` and
    `GameCopier`), `GuessWordEffect`, `TriggerWordGuessed`, `ComputerUtil.chooseSomeType` "word" +
    `AILogic$ Random`. `HangmanTest` (10; against words from the list the AI averaged 2.1 wrong guesses on everyday words and 3.4 on rare ones). Suite: 831 run, 0 failed, 6 skipped.
  - **Word list:** `res/lists/Words.txt` is 43,478 six-to-eight-letter words in American spelling, built by
    `_tools/wordlist/build-words.js` from SCOWL 2020.12.07 (wordlist.aspell.net): its `english-words` and
    `american-words` files only, never the British, Canadian, Australian or variant ones. `[common]` (18,423,
    SCOWL sizes 10-35) is what the AI notes from; `[more]` (25,055, sizes 40-70) is only there so it can
    recognize a rarer word a person notes. 115 crude or grim words were moved from common to more so the AI
    never puts one on screen. SCOWL's license needs its notice kept with copies: it ships verbatim as
    `res/lists/Words-SCOWL-Copyright.txt`. (The first version of this list was 563 words written by hand.)
- **American spelling** in the fork's own comments and this changelog (58 lines: colour, labelled, recognised,
  afterwards and the like). `_tools/american-spelling.js` rewrites only lines the fork added, so upstream's
  lines and merges are untouched.
- **Finders, Keepers** (ust/55) was already in from upstream; script checked against Scryfall, no change.

- **Extremely Slow Zombie** (ust/54a–d, four printings that differ only in flavor text; the edition file already
  listed them) and the **last strike** keyword it needs. `Combat` holds a creature with last strike out of the
  regular damage; when that step ends, `PhaseHandler` repeats `COMBAT_DAMAGE` once as the last-strike step if
  a creature still in combat has last strike (or was held back for it). Players get priority in between, as
  with first strike, and the log names the step "Last strike:". Per the Unstable FAQ: it deals damage only if
  it survives first-strike and regular damage; first strike + last strike deals damage twice (first-strike and
  last-strike steps, not the regular one); double strike + last strike three times.
  - Judgment calls the FAQ doesn't cover, by analogy with CR 510.4: no last-strike step when no creature in
    combat has last strike; a creature that lost last strike after sitting out the regular damage still deals
    its damage in the last-strike step.
  - AI: `ComputerUtilCombat.dealsDamageBefore` replaces the "first strike vs. none" checks in
    `canDestroyAttacker`/`canDestroyBlocker`, the wither predictions and the gang-block filter, so it knows a
    creature that kills the Zombie takes no damage from it. `CreatureEvaluator` values last strike as a
    drawback (-5 per power). `LastStrikeTest` (7). Suite: 821 run, 0 failed, 6 skipped.
  - Last strike is listed in `NonStackingKWList.txt` (a second instance adds nothing).
- **Dirty Rat** (ust/53) was already in from upstream; script checked against Scryfall, no change.

### 2026-09-30 (deployed: desktop, Android, GitHub) — upstream merge (2.0.16-SNAPSHOT); capital offense; Oracle: line sync; in-game ability text sync

- **Upstream merge 2026-09-30** (30 commits, through the 2.0.15 release; the fork is now 2.0.16-SNAPSHOT): one
  conflict, the `Oracle:` line of The Disciple of Vess (upstream removed a stray second line) - took upstream's
  side and re-ran `oracle-drift.js --apply`. No fork params were stripped from card scripts. Suite: 814 run,
  0 failed, 6 skipped.

- **capital offense** (ust/52, printed all lowercase, and the name is too): `Count$ CardCapitalLetters`
  (`CardFactoryUtil.getCapitalLetterCount`) counts capitals in the target's `Oracle:` text, leaving out
  reminder text and mana/tap symbols (printed as icons, not letters). Ability words and mid-sentence capitals
  count (Unstable rulings). AI: `PumpAi` now works out a target-dependent -X/-X (`SVar:X:Targeted...`) for
  each candidate and targets the best creature it kills. Before, X read as 0 or a guess before any target
  was chosen. Also fixes Flunk's AI. `CapitalOffenseTest` (3). Suite: 801 run, 0 failed, 6 skipped.
  - The count skips Forge's own `STATION 8+` lines, which Oracle writes as `8+ |` (they'd add 7 capitals
    per threshold). Now that `Oracle:` lines are synced (below), every card with an unambiguous Scryfall
    text counts the same as Scryfall except Phila, Unsealed (kept on purpose).
- **Big Boa Constrictor** (ust/51): already scripted upstream, checked against Scryfall, no change.
- **`Oracle:` fields synced to current Scryfall Oracle** (bulk data of 2026-09-29): 2,035 lines in 1,999
  scripts that still differed after the self-reference retemplate. 628 reminder text only, 55 keyword
  line splits, 42 punctuation or line breaks, and 1,310 wording (242 of them WotC's "of their choice",
  plus "enters" and "greatest" template changes, boons becoming "one-time boons" and so on).
  - *Forge's house formats are kept*: loyalty abilities stay `[+1]:` / `[-2]:` (Scryfall `+1:` / `−2:`),
    and a Station threshold stays its own `STATION 8+` line (Scryfall `8+ | `; d20 tables keep `15+ |`).
    `_tools/oracle-audit/forge-format.js` does the conversion.
  - *Left alone*: 11 names with more than one Scryfall text (B.F.M.'s halves, Everythingamajig and the other
    Un variants, Cunning's Planechase theme card), Phila, Unsealed (the script implements one outcome of the
    Unknown event; Scryfall prints both), and Khod, Etlan Shiis Envoy (playtest card printed before WotC
    folded Cephalid into Octopus; the script rightly pumps Octopuses).
  - *Verified*: every affected script was parsed with the real `CardRules` reader before and after, and
    every public getter on the rules and each face compared, plus what each game-code reader of the text
    derives (`CountersMoveEffect`'s counter kind, `SpellAbility`'s mana-spent checks, `hasReminderText`,
    the "(Transforms" header, the renderer's "Level up" test, `DeckGeneratorBase`'s creature-type and
    dual/fetch-land regexes). No card's rules changed except two color-identity fixes: **Fetching Garden**
    (G/W) and **The Belligerent and Useless Island** (U) were colorless because their stored text lacked
    the `({T}: Add ...)` line that Tundra-style lands have; Scryfall agrees with the new identities. A control
    run including the Oracle getters flags every edit. Harness: `_tools/oracle-audit/OracleSyncHarness.java`.
  - *Meant to follow the new text*: Punctuate (646 faces), capital offense (357), Lexivore line counts (111),
    Duh's `hasReminderText` (387), the "refers to a kind of counter" check on 18 cards, deck-generator
    type hints on 8, and four back faces now get the "(Transforms from ...)" header in play.
  - *Script bugs the drift exposed*, each confirmed against Scryfall: **Dragonborn Immolator** pumped +2/+0
    (Oracle +1/+0). **Obscura Polymorphist** could exile nothing (Oracle: "exile target creature").
    **Charred Graverobber** escaped for {3}{B} (Oracle {3}{B}{B}). **Disciple of Perdition** could exile
    your own graveyard and never made that player lose 1 life (the life loss wasn't chained). **Psychic
    Whorl** could target you (Oracle: opponent). **Kodama of the West Tree** needed damage to an opponent
    (Oracle: a player). **Mizzix, Replica Rider**'s copies were sacrificed at the next end step (Oracle:
    your end step). **Verdant Dread** triggered on any player's Verdant Dread (Oracle: a permanent you
    control). **The Forgotten Place** didn't enter tapped. **Gryffwing Cavalry** couldn't target itself
    (Oracle drops "another"). **Supernatural Rescue** could enchant any creature (Oracle: creature you
    control). Ability names corrected: Primaris Eliminator's Hyperfrag Round, Lychguard's Guardian
    Protocols, Necron Overlord's Relentless March, Shard of the Void Dragon's Spear of the Void Dragon.
    `OracleDriftFixesTest` (7); `TextBoxTest` now pins Shivan Dragon's current text. Suite: 808 run,
    0 failed, 6 skipped.
  - Merges: resolve an `Oracle:` conflict by taking upstream's side, then
    `node _tools/oracle-audit/oracle-drift.js <oracle-cards.jsonl.gz> --apply` (it replaces
    `sync-oracle-field.js`, which only knew self-references).
- **In-game ability text brought to current Oracle** (2026-09-30). Measured directly this time: every
  face of every card is rendered through the engine (`Card.getAbilityText`, 34,870 faces) and compared with
  its `Oracle:` text sentence by sentence. Line breaks, ability order and which keywords share a line are
  print layout and don't count. **29,592 faces matched at the start; 33,931 (97.3%) match now.**
  - *Engine* (one change, many cards): a card referring to itself in a cost or an "enters with" line says
    "this creature" / "this artifact" / "this Equipment" / "this card" as Oracle does
    (`CardFactoryUtil.getSelfReferenceNoun`, which matches 2,547 of the 2,555 such references in Scryfall's
    Oracle). Where a script can't tell two wordings apart - "Remove X +1/+1 counters" vs "Remove any number
    of storage counters", or a digital-only card that still prints its name - the card's own `Oracle:`
    text picks (`SpellAbility.withSelfReferences`). `Lang.getPlural` uses each type's own plural and leaves
    already-plural phrases alone (no more "creatureses", "Elfs", "Merfolks"). Alchemy cards say "Orcish
    Bowmasters", not "A-Orcish Bowmasters", in their own text. Two protections or hexproofs join
    ("protection from white and from blue"). Periods on "Adapt 1.", "Monstrosity 4.", "Buyback—Sacrifice a
    land."; "Mill a card"; "0:" loyalty; "Living weapon", "Power-up", "Web-slinging", "Start your engines!";
    Aftermath printed once; NICKNAME filled in on keyword lines.
  - *More engine wording*: a self-sacrifice joins onto a self-cost ("... from this enchantment and sacrifice
    it"); "Equip—Sacrifice a creature." and "Cumulative upkeep—Put a -1/-1 counter on this creature." closed up
    and ended like sentences; "Bolster 1.", "Support 2.", "Populate."; "Strive — This spell costs ..."; "You may
    cast this spell as though it had flash"; "protection from artifacts"; instants and sorceries end a non-mana
    keyword cost with a period too ("Entwine—Sacrifice three lands."); "Equip {3}. Activate only ..." loses a
    stray space; "Choose up to five {P} worth of modes." The Eldrazi Spawn, Scion and Sliver, Lander and Syr
    Acha token scripts say "this token", as their Oracle does.
  - *Script batches*: 799 template-only lines in 793 scripts, then 163 in 148 more (Saga chapters and
    ability-word abilities behind engine-built prefixes); 1,030 reviewed lines in 985 scripts (spelling,
    capitalization, short names, missing ability words, and text that was simply stale - Bride's Gown said
    deathtouch for first strike, Oscorp Research Team "Draw a card." for two, Conclave Cavalier "green and green"
    tokens); 37 lines in 35 scripts, among them the Unhinged **Gotcha** abilities, which now print as Oracle
    does ('If an opponent laughs, you may say "Gotcha!" When you do, return this card ...', via `CostDesc$`; the
    prompt is unchanged); and 111 lines in 103 scripts from the audit below, which also stated restrictions the
    scripts enforced but never printed ("Activate only as a sorcery.", "Activate only once each turn.") on 11
    abilities, gave 8 draw steps a "Draw a card." description, and brought Garbage Elemental's variant text
    and `Oracle:` lines to Scryfall.
  - *Scripts*: about 2,100 display fields reworded (`SpellDescription$` and the like, and the description
    part of `K:` lines). Template-only edits were applied automatically; everything else was read first, and
    checked against the script where a rules-bearing word changed. Every rewritten line was verified to
    change no parameter but display ones.
  - **d20 tables were broken by this fork and are restored.** An earlier round (`fix-vert.js`, 2026-08) took
    the scripts' `VERT` placeholder for corruption and replaced it with a literal `|`, which is the script's
    param separator, so 43 cards showed their result ranges with no result text. `VERT` is back (114 rows;
    counts match upstream file by file). `,,,` is a placeholder too (a line break in the card panel).
  - *Script bugs the review exposed*, each confirmed on Scryfall: Corpse Traders, Pilfering Imp, Dread Rider
    and Demonic Pact's discard could target any player (Oracle: opponent). Blood Speaker, Roots of Life and
    Timeless Witness were optional (Oracle has no "you may"). Prayer of Binding and Struggle for Skemfar's
    fight had to target (Oracle: up to one). Resounding Wave's cycle trigger and Twigwalker allowed fewer
    than two targets. Mercenary Informer could target tokens. Vivien Reid's +1 didn't randomize the bottom.
    Blast from the Past couldn't hit planeswalkers.
  - *Mistakes made and repaired along the way*: the text fixer had three flaws - a one-character mis-cut (12
    lines lost a comma or dash), re-inserting text when run on a stale render (20 doubled words), and
    matching a generic tail on the wrong ability (7 cards). All were found by an audit that checks every
    new sentence against the card's Oracle text (`audit-edits.js`), repaired, and guarded against.
  - *Not done* (939 faces): text Forge never renders (Conspiracy draft text, ante and deck-building lines),
    sub-abilities with no description, sentences Oracle and the script split differently, and one-off
    keyword wordings. `render-review.json` lists the open sentences.
  - Tooling in `_tools/oracle-audit/`: `RenderAbilityTextDump.java` + `rerender.sh` (render and diff),
    `render-diff.js`, `render-attribute.js`, `render-fix.js`, `render-triage.js`, `regress.js`,
    `audit-edits.js`, `restore-vert.js`. Always re-render before applying a batch.
  - Tests: `GameSimulationTest` and `CountersPutAiTest` find abilities by their new text. Suite: 808 run,
    0 failed, 6 skipped.

### 2026-09-27 (deployed: desktop, Android, GitHub) — self-reference sweep; AI combat and equip fixes; Unstable white 12–25; Animate Library; Blurry Beeble; Clocknapper; Crafty Octopus; Defective Detective; Five-Finger Discount; Graveyard Busybody; Half-Shark, Half-; Kindly Cognician; Magic Word; More or Less; S.N.E.A.K. Dispatcher; Socketed Sprocketer; Spy Eye; Very Cryptic Command; Wall of Fortune

- **Upstream merge 2026-09-25** (4 commits, right after the 2026-09-25 changelog): one-line script updates to
  Elvish Elegy and three upcoming cards (Venser, Fervent Forger; Living Library; Budding Insurgent). No conflicts.
- **Upstream merge 2026-09-26** (17 commits): match duration in the game log, Command the Stage (FRA), Loot the
  Anomaly, Sanctum Lurker's `IgnorePlaneswalkerZeroLoyaltyRule` param name, an FSkinFont glyph guard and other
  graphics/font updates. Conflicts: `Card.getNetCombatDamage` keeps the fork's half-value path with upstream's
  new `negateCombatAssignedDamage` folded in; 9 card scripts take upstream's lines with the self-reference
  wording reapplied by the oracle-audit tools.
- **Upstream merge 2026-09-27** (12 commits): upstream's card-script linter (#12048) strips params it doesn't
  know, including the fork's `Host$ True` on every host's enter trigger, which augment relied on. A host
  trigger is now also recognized by shape (`CardFactory.isHostTrigger`: a Host card's trigger for itself
  entering), so future linter passes can't break augment. Gnome-Made Engine keeps `TokenOwner$ You` over
  upstream's new `TriggeredCardController`, which has no triggered card once augmented.

- **Wall of Fortune** (ust/50): static `Mode$ RerollWithWall`. After any die roll - including the planar die -
  anyone with the permission may tap an untapped Wall they control to have the roller reroll it, seen first
  and repeatable with more Walls (`RollDiceEffect.tapWallToReroll`, also called from `PlanarDice.roll`). Not an
  activated ability, so a Wall that just arrived can be tapped. AI rerolls its own below-average rolls and
  opponents' above-average ones (not the planar die). `WallOfFortuneTest` (2). Suite: 798 run, 0 failed, 6 skipped.

- **Very Cryptic Command** (ust/49a-f): none of the six printings existed; one script with six variants, each
  its own "Choose two -" Charm (base face `<Unsupported Variant>`, as Everythingamajig).
  - 49a: card property `artIsBy <artist>` (one of the printing's credited artists) - "if that card's art is by
    Wayne England, you may reveal it and draw another card".
  - 49d: SetState `Mode$ TurnOver` - a face-down creature turns face up, a double-faced one transforms, anything
    else turns face down as a 2/2 (Unstable ruling). "Its controller's hand" uses `HandOf$ TargetedController`.
  - Built on earlier fork pieces: `nameWords_EQ1` (49b), Draw `FromLibraryOf$` (49c), `BlackBordered` and
    `Watermarked` (49e), `NudgeNumber` (49f).
  - Not covered: turning over a melded permanent (the ruling splits it into two creatures; it turns face down
    instead). `VeryCrypticCommandTest` (5). Suite: 796 run, 0 failed, 6 skipped.
- Time Out (ust/48, upstream) verified against Scryfall, unchanged.

- **Spy Eye** (ust/46): Draw `FromLibraryOf$ <player>` (`Player.drawCards(..., libraryOf)`) — still your draw
  (counts, Drawn triggers, draw replacements), the card enters your hand still its owner's (controller set, as
  `HandOf$`). Drawing from an empty library still loses (CR 704.5b says "a library"), so the AI declines then.
  `SpyEyeTest` (2, one through real combat). Suite: 791 run, 0 failed, 6 skipped.
- Spell Suck (ust/45, upstream): `Oracle:` gains its reminder text to match Scryfall.

- **Socketed Sprocketer** (ust/44): "installing" a die result. `Card.installedResults` holds the dice sitting on
  a permanent (not counters, so proliferate and counter effects never see them), shown in the detail panel
  ("installed: 5", `TrackableProperty.InstalledResults`); a new object starts without any.
  - `InstallResult` API: `Uninstall$ All`, `Install$ <amount>`.
  - `Uninstall<N>` cost (`CostUninstall`), a real cost so a 6 can't also be spent on a roll in response.
  - Static `Mode$ UseInstalledResult`: after each die you roll (you see it first, per the ruling), an installed
    result on a permanent you control may take its place (`RollDiceEffect.useInstalledResult`); logged.
  - AI: swaps in a higher result, spending a 6 only on a roll of 1-2; re-rolls when nothing 4+ is installed, at
    an opponent's end step or its own second main. `SocketedSprocketerTest` (4). Suite: 789 run, 0 failed, 6 skipped.

- **S.N.E.A.K. Dispatcher** (ust/43): Dig from any player's library; an Agents of S.N.E.A.K. card
  (`Watermark_agentsofsneak`) may go into your hand, still its owner's.
  - Dig `HandOf$` (shared `SpellAbilityEffect.moveToHandOf`, now also behind ChangeZone's `HandOf$`) and
    `RestTopOrBottom$` (the chooser's `willPutCardOnTop` decision per card).
  - AI `willPutCardOnTop` on an opponent's card: keeps it on top only when its owner wouldn't want it.
    `SneakDispatcherTest` (2). Suite: 785 run, 0 failed, 6 skipped.

- **More or Less** (ust/40): new `NudgeNumber` API — one number printed on a spell or permanent reads 1
  higher or lower until end of turn (or until it changes zones, CR 400.7).
  - `NumberInstances.of(card)` lists what can be picked: power, toughness, the numeral in the mana cost,
    numbers in keywords (Bushido 2), activation-cost numerals ({1} in Mind Stone's cost), and ability
    parameters whose number is really printed in that ability's text. "Draw a card" prints "a", which isn't a
    number word, so it isn't offered; X never is (Unstable rulings). Repeated numbers are told apart by
    occurrence ("the second 2"), power bonus before toughness bonus.
  - `Card.numberNudges` (`NumberNudge`) is applied where each is read: base P/T, `getCMC`, keyword text
    (`updateChangedText`), trait parameters (`CardTraitBase.changeText`, which every stack/trigger copy re-runs),
    descriptions (`applyDescriptionTextChangeEffects`, word form kept: "two" → "three"), and activation costs
    (`SpellAbility` rebuilds its `Cost`).
  - `PlayerController.chooseStringForEffect(options, sa, prompt)`: a labeled pick with a real prompt; the AI
    asks the API's `SpellAbilityAi.chooseString`.
  - AI: casts it to kill an opposing creature one less toughness kills; lowers numbers on opponents' objects
    and raises its own. `MoreOrLessTest` (6). Suite: 782 run, 0 failed, 6 skipped.
  - Fix after live testing: a spell on the stack is targeted as its spell ability, so the effect (and its AI)
    read targets with `getCardsfromTargets`, as Mind Bend does - it silently did nothing on a spell before.
    Noncreatures no longer offer a "Power 0"/"Toughness 0" (`CardState.hasPrintedPT`).
  - Not covered: numbers inside token scripts ("create a 1/1") and in SVar-computed amounts.

- **Magic Word** (ust/38): the word is chosen as it enters (Keeper of the Sacred Word's free-input
  `ChooseType`). FlavorAction descriptions accept `CHOSENWORD`: "the chosen word" in the ability text,
  the word itself in the payment prompt (`CostFlavorAction.getPrompt`), so you're asked
  `Whisper "xyzzy"?`. The AI puts it on an opponent's creature (`Curse`) and taps it down on their turn.
  `MagicWordTest` (1).

- **Kindly Cognician** (ust/37): card property `rulesTextHasWord_<word>` — the word or its plural, whole
  words only, in the Oracle text with reminder text and the card's own name removed (the Unstable ruling
  excludes reminder text, name, type line and flavor text; "nonartifact" doesn't count).
  `KindlyCognicianTest` (2).

- **Half-Shark, Half-** (ust/35): augment (+3/+3, {5}{U}) whose condition is "At the beginning of your upkeep,".

- **Graveyard Busybody** (ust/34): "All graveyards are also your graveyards." New static
  `Mode$ AllGraveyardsYours`; `Game.getGraveyardHolder()` is the controller of the one that entered most
  recently, recomputed before the layers on every static-ability pass. Everything is gated on it, so games
  without a Busybody are untouched. Per the Unstable rulings, while it's out:
  - `Player.getCardsIn(Graveyard)` gives the holder every graveyard (own last) and everyone else none —
    counts, delve/escape/exile costs, "from your graveyard" fetches, threshold/delirium all follow.
  - Cards in graveyards are controlled by the holder (`Card.getController`), so only they can use those
    cards' activated, triggered and static abilities; flashback and the like from any graveyard
    (`SpellAbilityRestriction.zoneHolder`, `PlayerZone` activatable list).
  - `YouOwn`/`YouDontOwn`/`OppOwn`/`TargetedPlayerOwn`/`OwnedBy` read a graveyard card's zone holder
    (scripts write "in your graveyard" as YouOwn); ownership itself never changes, so cards still go to,
    and are shuffled back from, their owners' graveyards.
  - "Top/bottom/above" graveyard properties now read the physical graveyard.
  - P/T: `Count$ValidGraveyard Card.YouOwn+hasFlavorText` (the per-printing flavor text table). A second
    Busybody's controller has no graveyards, so theirs is 0/0.
  - `GraveyardBusybodyTest` (5, incl. AI turns played with one out). Suite: 774 run, 0 failed, 6 skipped.

- **Five-Finger Discount** (ust/33): a card in another player's hand. ChangeZone `HandOf$ <player>` puts
  it into that player's hand without changing its owner (unlike `GainOwnership`, which Last-Minute
  Chopping and Gifts Given use — the Unstable rulings say ownership stays put here). While it's there
  the holder controls it, so they see it and the owner doesn't; any later zone change clears that, so it
  dies into, is bounced to, or is shuffled into its owner's zones.
  - Casting from hand and playing lands now check whose hand the card is in rather than its owner
    (`SpellAbilityRestriction.zoneHolder`, `Player.canPlayLand`) — identical for every other card.
  - "Any color the next time you cast that card": a `ManaConvert` effect with `ForgetOnMoved$ Hand`,
    which lasts through paying for the cast and ends when the cast is done.
  - `FiveFingerDiscountTest` (3), including the AI casting a stolen Shivan Dragon off six Islands.
    Suite: 769 run, 0 failed, 6 skipped.

- **Defective Detective** (ust/32): the person outside the game is simulated. New ChooseCard
  `AtRandom$ ThreatWeighted` picks at random with weight = impact², so bigger cards are clearly likelier
  and near-equal ones stay near-equal (Forest 30 / Grizzly Bears 161 / Shivan Dragon 316 came up
  0.5% / 20% / 80% over 3000 trials). The chosen card is revealed; its controller never sees the hand.
  - `ComputerUtilCard.evaluateCardImpact`: one scale across card types — creatures by
    `evaluateCreature`, other spells 50 + 30 × mana value (the AI's own cross-type scale), lands 30.
    It's now `CardThreat`'s evaluator; creatures score as before, so Sacrifice Play is unchanged.
    `ThreatWeightedTest` (2). Suite: 766 run, 0 failed, 6 skipped.

- **Crafty Octopus** (ust/30): host whose ETB has the creature itself assemble a Contraption
  (`AssembleContraption` defaults the assembler to a creature host, so Steamflogger Boss doubles it).

- **Clocknapper** (ust/29): phase stealing. `DB$ StealPhase | Phase$ Beginning|Main1|Combat|Main2|Ending`
  (`PhaseHandler.stealPhase`) marks that phase of the victim's next turn; as it begins the thief becomes
  the active player (`getPlayerTurn()`), and the victim gets it back when it ends. Everything keyed off
  the active player follows from that, matching the Unstable rulings: the thief untaps, gets the upkeep
  triggers and draws; gets sorcery timing; attacks its opponents while the victim can only block; and
  discards to hand size in a stolen cleanup. `getActualTurnPlayer()` says whose turn it really is — the
  next turn, extra turns and turn counts use it, so turn order never shifts.
  - Only the first instance of the phase is stolen (an additional combat after it isn't). A later steal
    of the same phase from the same turn replaces an earlier one; stealing from yourself does nothing; a
    skipped phase fizzles the steal; it expires once that turn ends. "End the turn" during a stolen
    phase hands the cleanup back (or starts a stolen ending phase there). Logged as "X steals Y's …".
  - Carried through `GameSnapshot` and the AI `GameCopier`.
  - Card: a GenericChoice of the five phases (`Defined$ You` so the controller chooses, the target on it).
    AI (`AILogic$ StealPhase`): targets the lowest-life opponent; steals combat when its untapped,
    unsick creatures that won't attack this turn deal lethal, otherwise the beginning phase.
    `StealPhaseTest` (6). Suite: 764 run, 0 failed, 6 skipped.

- **Blurry Beeble** (ust/27): Blurry is honor system. As it's cast (static `SpellCast` trigger), every
  player — the caster too, since a control change can make them the defending player — answers
  whether they were wearing glasses; those who were are remembered, and `CantBlockBy` with
  `Creature.!RememberedPlayerCtrl` lets only their creatures block. A `!wasCast` Beeble (put onto the
  battlefield, or flickered — the copy keeps its remembered players) can't be blocked at all, per the
  Unstable ruling.
  - GenericChoice: an option with `RememberChooser$ True` remembers the player who picked it on the
    host (after any `TempRemember` swap, so it sticks).
  - AI: GenericChoice accepts `AILogic$ GameChance.N.Key` — the second choice is the "yes" answer,
    taken when that player's per-game roll for Key succeeds (`Glasses`, 50%). `BlurryTest` (3).

- **Silver border follows the printing**: `isSilverBorderedOrAcorn` and Border Guardian's `BorderColor*`
  properties read Scryfall's printed border (PrintingTraits) over the edition default. Steamflogger Boss
  (ust/93) is black-bordered, Unstable's Contraptions (ust/167–211) and full-art basics (ust/212–216)
  borderless, so none count as silver-bordered any more. Test in `PrintingTraitsTest`.

- **Animate Library** (ust/26): the library becomes a permanent — new `GamePieceType.LIBRARY`, a
  nameless colorless artifact creature shown as a card back, P/T from the Aura (`Count$InOwnersLibrary`).
  - `SVar:AuraSpell` overrides an Aura's generated Attach spell; `SP$ AnimateLibrary` creates the
    library permanent (or reuses it for a second Animate Library) and enters attached to it.
  - Per the user's ruling it neither enters nor leaves the battlefield: placed and removed without
    zone-change triggers. It stays a permanent while any Animate Library is on it.
  - "Exile this Aura instead" is a plain `Moved` replacement; if a library permanent would still go
    anywhere, `GameAction.changeZone` makes it just stop existing — it never becomes a card in a zone.
  - `Card.getPaperCard()` returns null for a blank name instead of throwing; property
    `AnimatedLibrary`; the library has no printed border.
  - AI casts it with 6+ cards in library and no library already animated. `AnimateLibraryTest` (4).
    Suite: 754 run, 0 failed, 6 skipped.

- **Unstable white, ust/12–25**: Knight of the Kitchen Sink (all six variants), Knight of the Widget,
  Oddly Uneven, Old Guard, Ordinary Pony, Rhino-, Sacrifice Play, Side Quest, Success!, Teacher's Pet.
  - **Printing traits** (new; see the *Printing traits* index entry): `res/lists/PrintingTraits.txt`
    (`forge.card.PrintingTraits`, loaded lazily like `CardFlavorText`, 25,136 rows) records per printing the
    watermark (and back-face watermark), open-mouth art (Scryfall Tagger) and the printed border where Scryfall
    disagrees with the edition files; `CardEdition.BorderColor` gains `BORDERLESS` and `YELLOW`. New card
    properties `BlackBordered`, `CollectorNumberEven`/`Odd`, `Watermarked`, `Watermark_<name>`, `OpenMouthArt`,
    and `nameWords_Odd`/`_Even` (Oddly Uneven).
  - Knight of the Kitchen Sink's protections read the new printing traits (above). Knight of the
    Widget counts `Watermark_orderofthewidget`.
  - Word counts keep "Token" in a generated token's name: CR 111.4 (effective 2026-08-07) names a
    Human Soldier token "Human Soldier Token", superseding the older Unstable ruling. Zero words is even.
  - `hasReminderText` reads a functional variant's own text, not the base face's.
  - Ordinary Pony's errata ("so you can't flicker creatures more than once each turn") marks the
    returned creature, not the Pony: a Pony that gets flickered back (a new object) still can't
    flicker the same creature again that turn, while a later flicker by anything else makes a new,
    unmarked object.
  - Sacrifice Play: `ChooseCard | AtRandom$ ThreatExtremes` — the person outside the game is
    simulated: 40% the biggest threat, 40% the smallest, 20% across the rest. Threat is the AI's
    creature evaluation, plugged into forge-game through `CardThreat` at startup.
  - Side Quest removes the creature from the game (zone `None`, not exile) until your next turn.
    `GameAction.moveTo` now passes move params for `None`, so `Duration$` returns work from there,
    and the return looks in the owner's `None` zone (which `getCardState` doesn't search).
  - Teacher's Pet: `Augment | ChangeType$` searches the library for the augment card.
  - AI: ChooseCard picks its target opponent *before* checking the choices, so `TargetedPlayerCtrl`
    choices (Blot Out, End of the Hunt, Sacrifice Play) no longer always look empty; `AILogic$
    SideQuest` sends a tapped attacker in main 2; Augment won't sacrifice Teacher's Pet into an empty
    search.
  - Tests: `PrintingTraitsTest` (10), `UnstableWhiteTest` (6). Suite: 750 run, 0 failed, 6 skipped.

- **Self-reference sweep over every card's text**, against Scryfall's Oracle bulk data of 2026-09-25.
  The earlier syncs skipped any line that differed from Oracle in more than the self-reference, and
  never handled the shortened legendary name, so Dragon Egg still read "When Dragon Egg dies" and
  Jugan's deck-editor text "When Jugan, the Rising Star dies". New
  `_tools/oracle-audit/selfref-sweep.js` aligns each line with Oracle word by word and rewrites **only**
  the card's own name where it sits opposite "this creature" / "this land" / "it" / the short name,
  leaving any other difference on the line alone:
  - 1,571 `Oracle:` lines and 821 in-game text fields (trigger, spell, static and stack descriptions)
    in 1,960 cards. 1,065 are short names: `CARDNAME` → `NICKNAME` in game ("Klauth", "Jugan",
    "Radiant"), except in `StackDescription$`, whose getter doesn't substitute `NICKNAME`.
  - A bare "it" is only used when the field names the card earlier, so a delayed trigger's own text
    ("Destroy Cinder Wall at end of combat.") isn't left with an "it" that points at nothing.
  - Token self-reference inside quoted granted text (`token-quote-sync.js`): 53 cards whose token
    text now reads "This token can't block." etc., applied only where that swap makes the `Oracle:`
    line identical to Scryfall.
  - Every changed script line was checked parameter by parameter: only display-text keys or the
    `Oracle:` line differ.
  - In all, the sweep touched 1,984 script files (2,449 lines), counting the token-text and `Secondary$` edits.
- **One line instead of two**: Keldon Marauders ("enters or leaves the battlefield") and Tergrid
  ("sacrifices a nontoken permanent or discards a permanent card") showed each trigger as its own
  line; the second is now `Secondary$ True` with the joined Oracle sentence, as the 14 other
  "enters or leaves" cards already did.
- **AI: die-roll attack pumps count.** Combat prediction skipped any attack trigger whose ability
  wasn't a Pump at the top, so Strength-Testing Hammer's roll (and Velukan Dragon's) was worth 0 and a
  Hammer-equipped 1/1 wouldn't attack into a 1/2. It now counts the lowest possible roll — certain,
  and enough to see the 1/2 always dies.
- **AI: blockers that remove what they fight.** Wall of Nets (exiles what it blocked) and Kjeldoran
  Frostbeast (destroys everything in combat with it) do it with an end-of-combat trigger no damage
  math saw, and the 0-power wall also read as a "free" attack. `canDestroyAttacker`/`canDestroyBlocker`
  now know, provided the source survives combat damage to trigger; the free-attack shortcut excludes
  attackers a single blocker removes.
  - Not changed: at low opposing life the attrition estimate counts a blocked attacker's damage in the
    first round, so with a 5/5 and a 2/2 against Wall of Nets at 7 life the AI still goes all-in (it
    does the same against any wall). Upstream heuristic; left alone.
- **AI: no second Witches' Eye on the same creature.** Added `NonStackingAttachEffect` to Witches'
  Eye and the other three Equipment that only grant a {T} ability (Siren Song Lyre, Sorcerer's Wand,
  Lobe Lobber): a second copy's ability can never be used alongside the first.
- New `CombatRemovalAndEquipAiTest` (7 tests; 6 fail on the old code, Velukan Dragon guards the formula). Suite: 734 run, 0 failed, 6 skipped.

### 2026-09-24 to 09-25 (deployed 2026-09-25) — more Un-cards; pink and gold; network fix; Oracle retemplate; upstream merges (28 and 4 commits)

- This deploy also shipped everything in the sections below, back to 2026-08-07: all of it sat under
  the log's "Unreleased" heading until this deploy dated it.

- **More Un-cards**: Rules Lawyer, Grimlock (Dinobot Leader), Nerf War, By Gnome Means,
  Chivalrous Chevalier, Do-It-Yourself Seraph, Gimme Five, GO TO JAIL, and the Unstable augments
  Half-Kitten, Half- and Humming-.
  - *Rules Lawyer* — new `IgnoreStateBasedActions` static mode; every 704 check skips protected
    players and permanents, and the AI's combat/burn/lose predictions know they won't die to SBAs.
  - *Augment* — from-hand, sorcery-speed ability that merges the card onto a host through mutate's
    merge plumbing. The combined card's name, cost, colors, types, P/T and completed ETB triggers
    follow the Unstable mechanics article and FAQ. A hostless augment on the battlefield goes to the
    graveyard (Rules Lawyer exempts it).
  - *Nerf War* — new `ShootAtLibrary` effect simulating the blaster volley (6 darts, 40% hit,
    1–3 cards per hit, tunable per script).
  - *Do-It-Yourself Seraph* — new `GainsTextBoxOf` continuous param that *adds* each matching
    card's traits (unlike `GainTextOf`, which replaces); contradicting gained statics apply in exile
    order, pinned by `DoItYourselfSeraphTest`.
  - *By Gnome Means* — `PutCounter` `CounterTypeChoices$ AnyPrinted` offers every counter kind Forge
    knows.
  - AI: `GameChance.N.Key` rolls once per AI player per game for honor-system facts; `HighFives`
    number logic for Gimme Five.
- **Gold as a seventh color**, for the 4/4 gold Dragon token (Sword of Dungeons & Dragons, Urza
  Academy Headmaster), on the same terms as pink. In a game with any silver-bordered card, "choose a
  color" prompts and protection from a color of your choice also offer pink and gold.
- **Border is now per printing**: edition lines take `${"border": "Silver"}` (13 silver-bordered
  cards in black-bordered sets) and `${"stamp": "acorn"}` (all 296 acorn printings), and
  `SilverBordered` checks the printing instead of any `Type=Funny` edition.
- **13 missing creature types** added to `TypeLists` (Autobot, Cyborg, Automaton, Brainiac, Chicken,
  Custodes, Head, Naga, Reveler, Rukh, Teddy, Urzan, Walrus); type checks against them silently
  failed before.
- **The rest of the new Un-cards:**
  - *Grimlock, Dinobot Leader // Grimlock, Ferocious King* — "{2}, Convert a Transformers toy you own
    to its other mode" is an honor-system `FlavorAction` cost on both faces, transforming back and forth.
    The lord bonus covers *other* Dinosaurs, Vehicles, Autobots and More Than Meets the Eye creatures
    you control (a first version buffed itself). AI: converts at `GameChance.50.TransformersToy`.
    `Autobot` and `Cyborg` were added to `TypeLists` for it; before that `Creature.Autobot` never
    matched and Rules Lawyer's Cyborg type was display-only.
  - *GO TO JAIL* — exiles target creature an opponent controls until it leaves; at the upkeep of the
    exiled card's owner that player rolls two six-sided dice, and doubles sacrifice the enchantment.
  - *Chivalrous Chevalier* — "unless you compliment an opponent" is an honor-system `FlavorAction`
    unless-cost; paying it rolls a d30 to show one of 30 courtly compliments. The AI pays 90% of the
    time and never bounces the Chevalier itself.
  - *Gimme Five* — you enter how many people high-fived you (0–99) and gain that much life; each other
    player gains 1 life by confirming they high-fived you. AI `HighFives` number logic picks 0–3 ("the
    few people plausibly near a computer"), and AI players never confirm a high five.
  - *Half-Kitten, Half-* (Augment {2}{W}) and *Humming-* (Augment {3}{W}) — the first augment cards.
  - *Augment, more*: `AugmentAi` only augments hosts it controls (the combined creature belongs to the
    host's controller). Gnome-Made Engine's token owner is now `You` rather than the ETB trigger's
    context. New `AugmentTest` (5 tests: combined characteristics, an artifact host, the host's ETB
    completing the augment's condition, both cards leaving together and a lone augment dying, the AI
    only augmenting its own hosts).
  - *Nerf War* — `ShootAtLibraryAi` fires at the opponent with the lowest life total and a nonempty
    library, since every card knocked off is also ½ damage. The stack text's ® had been written as a mangled escape (`Nerf00AE`); fixed.
  - *Rules Lawyer* — deathtouch and empty-library flags still lapse while protected. New
    `RulesLawyerTest` (2 tests: SBAs skip the protected player and permanents; the Lawyer itself isn't
    protected, and the protection ends).
- **Gold, more**: `MagicColor.GOLD` is bit 7, outside `ALL_COLORS` like pink, with 64 more `ColorSet`
  constants to keep ordinal == mask, a drawn {O} symbol on desktop and mobile, and the multicolor gold
  shade in the detail panel. The Dragon token had been scripted colorless; it was renamed
  `c_4_4_dragon_flying` → `gold_4_4_dragon_flying`, and the Unstable, Unsanctioned and HasCon 2017
  edition lines follow it. Also: `lblPink`/`lblGold` were missing from `en-US.properties`; the AI mana
  planner no longer files pink or gold under generic for any-color sources; Planar Conquest's (mobile)
  AEther color filter no longer lists pink/gold combinations.
- **Pink and gold, more**: protection from pink or gold and the `Card.Pink`/`Card.Gold` properties work
  (protection threw "unknown protection keyword"). A silver-bordered game is detected by
  `Game.isSilverBorderedGame()`.
- **Border, more**: the 13 silver-bordered cards in black-bordered sets are J17 Rules Lawyer, P30M
  Richard Garfield, Ph.D., six PAL04 cards (Circle of Protection: Art, Mise, Booster Tutor, Goblin
  Mime, Granny's Payback, Ashnod's Coupon) and five SLD cards (Discord, Lord of Disharmony; Applejack;
  Fluttershy; Pinkie Pie; Rainbow Dash). The 296 acorn printings are UNF 279, MB2 7 and MBC 10.
  `Card.isSilverBorderedOrAcorn` backs both the silver-bordered-game check and the `SilverBordered`
  property (Spatula of the Ages, Ass Whuppin'), which used to match any `Type=Funny` edition,
  including black-bordered Heroes of the Realm promos, playtest cards and Unfinity/MB2's
  tournament-legal cards.
- **Fix: images for funny-card variant numbers.** Upstream widened its funny-card collector-number
  pattern to `^F\d+[a-z]?` (variants like `F203a`), but the fork's `ImageUtil.getScryfallCardRef` kept
  `^F\d+` through the 2026-09-24 merge (end of this section); it now uses upstream's pattern.

- **Test suite back to green** (`mvn -pl forge-gui-desktop -am test`: 721 run, 0 failed). Of the 18
  failures, 8 were the fork's and 10 were environmental, reproducing identically on upstream:
  - *Network play was broken* — the five-face split states pushed `CardStateName` past the 16 that
    `DeltaPacket`'s key encoding allowed, so its static guard threw on class load. That was
    `DeltaSyncUnitTest` ×3 and `NetworkPlayIntegrationTest`'s 60s timeout. Fixed by widening the slot.
  - *Stale after the Oracle sync* — `GameSimulationTest` (Thespian's Stage ×2, Lightning
    Berserker) and `SpellAbilityPickerSimulationTest.testLandSearchForCombo` expected the old
    `CARDNAME`/card-name wording; now "This land …" / "This creature …", matching Scryfall.
  - *Profile-dependent* — the test card database loaded `%APPDATA%\Forge\custom\editions`, so
    custom sets there (a Pro Tour Collector Set with eight Hymn to Tourach prints, CEI, WC01)
    changed which printing the CardDb and DeckRecognizer art-preference tests picked. The test
    `CardDatabaseHelper` now uses no custom cards and an empty custom-editions folder.
- **Five-face split cards over the network.** The client only decoded upstream's four card-state
  slots, so Who // What // When // Where // Why's third to fifth faces arrived as raw state numbers:
  every copy logged `Error setting property Split3State` (and 4, 5), and reading its faces on the
  remote client threw `ClassCastException`. Now decoded like the others; new
  `FiveFaceSplitNetworkTest` plays a real networked game with the card and checks all five faces.
- **`Oracle:` fields retemplated to match Scryfall** — 10,927 lines in 10,826 card scripts where the
  *only* difference from current Oracle (Scryfall bulk data of 2026-09-24) was WotC's self-reference
  change: the card's own name → "this creature" / "this land" / "it" (Thespian's Stage, Lightning
  Berserker…). The earlier Oracle sync only covered the in-game ability text, so the deck editor and
  card search still showed the old wording. Only the `Oracle:` line changed; the ability, cost and
  keyword lines didn't.
  - *Verified to keep every card's function*: each script was parsed with the real `CardRules`
    reader before and after, and every public getter on the rules and on each face (abilities,
    keywords, colors, color identity, deckbuilding colors, commander eligibility, AI deck hints)
    compared — 0 differences across all 10,927; a control run that includes the Oracle getters
    flags all 10,927, so the comparison does see the edits. Counter-type detection
    (`CountersMoveEffect`), the AI and deck-generator text regexes and the mana-spent checks are
    also unchanged. Test suite: 722 run, 0 failed.
  - *Intentional side effects on features that measure the text itself*: Punctuate counts on 988
    cards and Lexivore line counts on 1,403 now follow current Oracle; Pygmy Giant loses numbers
    that only came from a card's own name on 18 (e.g. Wall of One Thousand Cuts). Adventure-mode
    reward filters (`cardText` regexes) gain or lose some matches on 2,959 cards — mostly
    accidental own-name substring hits dropping out ("Rat" in "Wrath", "Cat" in "Catapult") and
    "this Enchantment"/"this Equipment" newly matching.
  - The remaining 3,341 differing `Oracle:` fields have other wording drift and were left alone
    (synced 2026-09-30, see that entry).
  - Merges: an upstream edit to one of these lines will now conflict. Take upstream's side of the
    line, then re-run `_tools/oracle-audit/oracle-drift.js <oracle-cards.jsonl.gz> --apply`.
- **Text-box readers now see paragraphs.** Stored Oracle text separates paragraphs with the script's
  literal two-character `\n`, not a real newline, but the text-box readers assumed real newlines:
  - *Lexivore / Frazzled Editor* (`CardFactoryUtil.getTextBoxLineCount`) split on `\r?\n`, so every
    card was measured as one long paragraph with the `\n`s counted as text, and short paragraphs lost
    their one-line minimum ("Flying\nVigilance\nTrample\nHaste" was 1 line, not 4; Shivan Dragon 4,
    not 5). "Wordy" (4+ lines) and "most lines of text" were undercounted on nearly every
    multi-paragraph card. Now splits on the literal separator and real newlines both.
  - *Pygmy Giant and Tainted Monkey* (`Card.getTextBoxContents`) — the separator glued an `n` onto
    each paragraph's first word, so a number word opening a paragraph was missed ("Two target
    creatures…" on Ruthless Disposal read as "ntwo") and a chosen word opening one never matched as a
    whole word. `getTextBoxContents` now turns the separator into a real newline. Punctuate was
    unaffected (backslash isn't one of its marks).
  - New `TextBoxTest` (5 tests). Test suite: 727 run, 0 failed, 6 skipped.

- **Upstream merge (28 commits, 2026-09-24).** Conflicts: `ImageUtil` (kept the fork's
  `ScryfallCardRef` refactor, which dropped upstream's new pattern, restored above); `CardView` (kept
  the fork's five-face `SPLIT_STATE_PROPS` and took upstream's removal of `hasBackSide()`); Bond of
  Passion (kept the fork's `Any.!targetedBy`, so the 2 damage really goes to "any *other* target").
- **Upstream merge (4 commits, 2026-09-25)**, no conflicts.

### 2026-09-23 (released 2026-09-25) — upstream merge (51 commits)

- **Conflict resolution:**
  - *Aetherflame Wall* — took upstream's `CanBlockIfShadow` static. The fork's side was a plain `K:`
    string with no matching keyword, so it displayed the ability but never let the wall block a
    shadow creature.
  - *Sylvan Primordial* — took upstream's current-printing wording (search for up to X Forests) over
    the fork's older one-Forest-per-permanent text; checked against live Oracle.
  - `Card.java` — kept the fork's `addPTBoostHalves`/`getTempPowerBoostHalves`/
    `getTempToughnessBoostHalves`, which the half-P/T layer calls and upstream doesn't have.
  - Mobile `CardImageRenderer`/`CardRenderer` `drawPtBox` — the fork's side referred to an undeclared
    `pieces` variable (a latent bug the merge surfaced); took upstream's `ptPieces`/`listItemPtPieces`
    with the fork's `getPowerString()`/`getToughnessString()`, which also show half P/T.
  - Mobile 180° flip rendering — upstream renamed `CardRendererUtils.drawFoil` (boolean) to
    `getFoilIndex` (int); took that with the fork's `f_x/f_y/f_w/f_h` footprint (a 180° flip must not
    inherit the enlargement meant for a 90° rotation).
  - Mobile damage display — kept the fork's `hasHalfDamage()`/`getDamageString()`.

### 2026-09-19 (released 2026-09-25) — upstream merge (20 commits): 829 script conflicts; split-card face fix

- **Upstream merge (20 commits).** 829 card-script conflicts came from two orthogonal mass edits on the
  same lines: upstream moved static abilities from `Affected$ Card.Self` to `AffectedDefined$ Self`,
  while the fork had reworded `CARDNAME`/`NICKNAME` to Scryfall's "this creature" wording. A script
  took upstream's structure and the fork's wording only where the difference was provably just those
  two changes; afterwards, across the 827 auto-resolved files exactly 876 lines differed from the
  fork's side, every one a pure `Affected`/`AffectedDefined` change. Hand-resolved, each against live
  Scryfall Oracle text:
  - *Rebound Auras (Rancor and others), Camato Scout, Duplicant, Glint-Raker, Forgeborn Phoenix,
    The Space Family Goblinson, Basilisk Collar, Jackhammer, Circle of the Moon Druid* — upstream's
    wording differed from Oracle (or dropped reminder text), so the fork's text was kept on upstream's
    structure.
  - *Nascent Metamorph* — upstream's cleanup pointed `SubAbility$` at `DBCleanup` but defined
    `DBCleanupOne`, a dangling reference; kept the cleanup under a correctly named SVar. Without it a
    later trigger would clone the previously revealed creature.
  - *Providence of Night* — took upstream's renamed `CostsHybridMana` property.
  - `ChooseCardNameEffect` — upstream's per-alternative `ValidCards` loop combined with the fork's
    `ManaCost=Chosen` branch (Richard Garfield, Ph.D.), using a labeled `continue nextPlayer` so
    "nothing chosen" still skips the whole player.
  - `CardUtil.turnToRightFace` (upstream's new shared helper) had the split-card bug the fork had fixed
    in dev mode on 2026-08-10: the combined "Left // Right" name never matches a single face, so the
    left half always became the right half. Fixed inside the helper, so all four callers benefit.
- **Fix: duplicate hybrid-mana helpers.** Both sides had added `ManaCost.hasHybrid()` and
  `ManaCostShard.isHybrid()`; they merged textually clean but didn't compile. `hasHybrid()` was
  identical, so one copy went. For `isHybrid()` upstream's version was kept: it counts hybrid
  Phyrexian symbols such as {G/W/P} as hybrid (rule 107.4f), while the fork's `isPhyrexian()` guard
  also excluded them, more broadly than its own comment intended. Only Providence of Night uses it.

### 2026-09-11 to 09-18 (released 2026-09-25) — upstream merges (15, 60 and 20 commits); Steel Hellkite

- **Upstream merge (15 commits, 2026-09-11)**, no conflicts.
- **Upstream merge (60 commits, 2026-09-15).** *Steel Hellkite*: neither side's `ValidCards` property
  for "whose controller was dealt combat damage by this creature this turn" existed (the fork's
  `controllerWasDealtCombatDamageByThisTurn` and upstream's `wasDealtCombatDamageThisTurnBySource`), so
  the {X} ability silently destroyed nothing either way. Now uses the real `PlayerProperty`
  `wasDealtCombatDamageThisCombatBy Self`, which is scoped to this combat rather than this turn: Forge
  has no per-turn tracking, and Glen Elendra's script already accepts the same precision. Kept the
  fork's "this creature" wording on the pump ability and the `Oracle:` line.
- **Upstream merge (20 commits, 2026-09-18).** Mobile `CardImageRenderer` 180° flip: took upstream's
  `drawCardRoundRect(…, foil)` call with the fork's `f_x/f_y/f_w/f_h` footprint.

### 2026-09-10 (released 2026-09-25) — upstream merge (55 commits): adopt upstream's CdnUuidCache image system

- **Images: the fork's Scryfall CDN path (2026-08-10) retired for upstream's.** Upstream's
  `CdnUuidCache`/`ScryfallSetSync`/`ScryfallRateLimiter` solve the same problem more completely:
  per-language resolution with English fallback, negative caching of misses with retry-after, real
  per-face UUIDs for DFCs, endpoint-aware rate limiting and a bulk-sync path. `ScryfallImageIndex` and
  `MiniJson` were deleted along with every call site (`ImageFetcher`, `SwingImageFetcher`,
  `LibGDXImageFetcher`, the two `ForgeConstants` URLs); two token-image call sites that had merged
  silently now call `CdnUuidCache` directly.
- **Card-script conflicts (12 scripts).** Most were upstream bringing back `CARDNAME` where Oracle reads
  "this creature"/"this artifact"/"this Siege" or a short name (checked live on Scryfall for all nine
  such cards): Endurance, Flamewave Invoker, Goblin Razerunners, Invasion of Arcavios, Kazandu Stomper,
  Nascent Metamorph, Rocco, Cabaretti Caterer, Sandstalker Moloch, Spawning Pit. Three also carried a
  real upstream fix the fork had missed: `ChangeZone` reads `SelectPrompt$` and `Dig` reads
  `PrimaryPrompt$`, and Kazandu Stomper, Rocco and Sandstalker Moloch had them backwards (a silent no-op
  each); took upstream's parameter names with the Oracle wording on top. Kazandu Stomper's `Oracle:`
  line also gained trample's reminder text, and The Seventh Doctor's reads "Whenever" rather than
  "When".
- **Invented parameters dropped.** Five of the fork's script parameters turned out to be read by no
  effect class: `TokenController$` (Orochi Hatchery, Spawning Pit), `Reveal$` on `DigUntil` (Nascent
  Metamorph), `Guess$` on `ChooseCard` (The Seventh Doctor), `ChoiceOptional$` on `ChooseCard`
  (Thunderwave) and `AlternativeMessage$` on `ChangeZone` (Invasion of Arcavios). Orochi Hatchery's
  "This artifact enters with X charge counters on it" needed a custom description on its
  `etbCounter` keyword, whose auto-text always uses the card's name.
- *Thunderwave* — upstream's side wrote its d20 rows with `VERT`; the merge kept the fork's literal
  `|` instead, taking `VERT` for corruption. That was wrong: `VERT` is Forge's placeholder for a printed
  `|`, and the fork's change was reverted on 2026-09-30 (see that entry).
- **`ChangeTargetsEffect`** — the fork's `RandomTarget` branch (absent upstream) had two bugs that only
  showed once the merge forced a recompile: a missing `GameEntity` import, and `sa.getHostCard()` passed
  where `updateTarget` expects the shared `Set<GameObject>` the other branches thread through. Fixed
  both; kept `RandomTarget` and the fork's `resolveMagnet()` player-redirect fix.
- Verified: every module compiles and test-compiles. Full `mvn test` showed 9 failures in upstream's
  new `ScryfallSetSyncTest` (`NoClassDefFoundError` on `ForgeConstants` through
  `FModel.getPreferences()`), unrelated to the merge.

### 2026-09-03 to 09-04 (released 2026-09-25) — Charm info view; Shard of Broken Glass AI; bestow P/T; upstream merges (13 and 13 commits)

- **Fix: Charm modes vanishing from the card text.** `CharmEffect.makePossibleOptions` applied
  CR 603.3c legality filtering even when only describing the printed text, so a mode needing an
  opponent's permanent (Tiller Engine's tap mode) disappeared from the info view whenever no opponent
  controlled one. New `checkLegality` parameter: choosing (`makeChoices`, `CharmAi`) still filters; the
  description builders (stack text and the card's rules text) always show every printed mode.
- **AI: Shard of Broken Glass** milled itself every time, since `MillAi` never asks whether self-mill
  is wanted. New `AILogic$ SelfMillIfGraveyardMatters`, checked in `MillAi.confirmAction` (the real
  decision point for an `Optional$` ability at resolution): it accepts only when delirium or threshold
  already apply, or the AI has seen a graveyard-payoff keyword (Flashback, Escape, Delve, Dredge and so
  on) this game.
- **Fix: bestow P/T display.** `Card.animateBestow`/`unanimateBestow` switched a bestow permanent
  between Aura and Creature without refreshing the view's cached P/T, so a creature falling off what it
  enchanted showed 0/0 until something unrelated refreshed it. Both now call `updatePTforView()`.
- **Upstream merges (13 commits, 2026-09-03; 13 commits, 2026-09-04)**, no conflicts.

### 2026-08-27 to 09-01 (released 2026-09-25) — Conspiracy-format rules; Conspiracy Draft block; upstream merges (3 and 22 commits)

- **Sovereign's Realm** had `K:` lines but no backing logic. Its "deck can't have basic land cards"
  clause is now a deck-validation check in `DeckFormat` ("can't include basic land cards while
  Sovereign's Realm is in the command zone"), and "starting hand size is five" is set before the
  opening hand in `GameAction`, next to the existing Advantageous Proclamation / Power Play /
  Emissary's Ploy special cases.
- **Conspiracy Draft block** — CNS and CN2 as one draftable/sealed pool, as the format is played (each
  set was only draftable on its own).
- **Innistrad block** now lists ISD DKA AVR on one line; Avacyn Restored had been its own standalone
  block, unlike every other three-set block.
- Oracle wording drifts found while auditing all 25 Conspiracy-type cards: Hold the Perimeter's token
  reads "This token can't block", and Secret Summoning "a creature you control with the chosen name".
- **Upstream merge (3 commits, 2026-08-28)**, no conflicts.
- **Upstream merge (22 commits, 2026-09-01).** `StaticAbilityMode`: the fork's `ChoosesTargets`/
  `IgnoreErrata` alongside upstream's new `ManaRestriction`. *Celestial Dawn*: kept the fork's split
  Oracle/as-printed statics (the `PrintedTextActive` pair) over upstream's single `AffectedZone$ All`
  rewrite. *Hogaak, Arisen Necropolis*: took upstream's new `ManaRestriction` static for "You can't
  spend mana to cast this spell" with the fork's "this card" wording.

### 2026-08-13 to 08-26 (released 2026-09-25) — upstream merges (17, 57, 8, 2, 7, 9 and 30 commits)

- **Upstream merges** on 2026-08-13 (17 commits), 08-19 (8), 08-20 (2 and 7) and 08-23 (9), no
  conflicts.
- **Upstream merge (57 commits, 2026-08-18).** *Nether Traitor*: took upstream's `Creature.YouOwn` fix
  ("put into *your* graveyard" means you own it) with the fork's "this card" wording. *Weathered
  Sentinels*: took upstream's `Defined$ Self` on the attack pump.
- **Upstream merge (30 commits, 2026-08-26).** `AiCardMemory.clearAllRemembered`: upstream's
  per-player `memoryMap` iteration, still skipping the fork's `HELD_MANA_SOURCES_FOR_OBLIGATION`, which
  spans turns by design. `SpellAbilityAi`/`ChangeZoneAi`: both sides' imports. *Wormfang Newt* and
  *Wormfang Turtle*: took upstream's simpler exile (hidden `ChangeZone`, returned via `ExiledWith`) over
  the fork's choose-then-remember version, with the fork's "this creature" wording.

### 2026-08-10 (released 2026-09-25) — Happy Holidays; Super Secret Tech; Scryfall CDN images; upstream merge (11 commits)

- **Cards:** Super Secret Tech (unh/141) and the whole Happy Holidays set — Gifts Given, Evil Presents,
  Snow Mercy, Yule Ooze, Naughty // Nice, Stocking Tiger, Mishra's Toy Workshop, Goblin Sleigh Ride,
  Thopter Pie Network, Some Disassembly Required, Bog Humbugs, Decorated Knight // Present Arms,
  Topdeck the Halls, Last-Minute Chopping, Chaos Wrap, Seasonal Sequels, Eggnogger's 'Stache and Cheer.
  Their `Oracle:` text matches Scryfall verbatim.
  - *Super Secret Tech* — "premium" is the card being foil, tracked per printing (new `Premium`
    property); neither clause is limited to your own cards.
  - *Goblin Sleigh Ride* — new `SlideOntoBattlefield` dexterity effect, modeled on how Forge already
    stands in for Chaos Orb and Falling Star: chance decides whether the rider stayed on (70%) and what
    it touched, and a rider that stayed on deals damage equal to its toughness to each other creature
    it touched.
  - *Snow Mercy* — new `TapSequence<…>` cost (`CostTapSequence`): literal {T}/{Q} sequences that sorted
    cost parts can't express, paid in printed order with a short pause per step so the shake is
    visible; `GLOBE` counter.
  - *Chaos Wrap* — new `SLEEVE` counter with its own damage/destroy replacement, behaving like a shield
    counter; how many sleeves you used is asked.
  - *Evil Presents* — new `AttacksItsController` static mode: a creature that may and must attack its
    own controller, an exception CR 506.2 needs in two places (`CombatUtil`'s defender list and
    `StaticAbilityMustAttack`).
  - *Stocking Tiger* — new `OpenBooster` effect: the chosen set's own booster template generates a real
    pack (the draft/sealed generator); `ChooseExpansion` gained `WithBoosters` to offer only sets that
    can produce one.
  - *Decorated Knight // Present Arms* — new `SwapLibraryWithDeck` (exchange your library with one of
    your saved constructed decks, searched through deck folders; the displaced library goes to the
    sideboard, Forge's "outside the game"; the AI declines) and `DrawFromOutsideGame` (draws the top of
    that original deck back). New `DisplacedFromLibrary` and `Decorated` (foil, alternate art or a promo
    edition) properties.
  - *Some Disassembly Required* — new `DistributeKeywords` effect: each of the sacrificed creature's
    keywords goes to exactly one recipient, repeated instances handed out separately; new
    `Count$CurrentMonth` for its December check.
  - *Mishra's Toy Workshop* — new `MakesToken` spell-ability property that walks the whole ability tree,
    so a Charm mode that creates tokens counts.
  - *Seasonal Sequels* — `CopySpellAbility` `NameEachCopy`: each copy takes the title you typed.
  - *Topdeck the Halls* — miracle is no longer offered on lands, which can't be cast (CR 702.94a).
  - Also: `ManifestBase` `Optional`/`OptionalPrompt`; the `Present` creature type; the chosen set shown
    in the card detail panel. Gifts Given, Naughty and Last-Minute Chopping hand a card's ownership over
    before moving it, since a card only ever moves to its owner's hand.
- **Images from Scryfall's CDN.** New `ScryfallImageIndex` built card image URLs on
  `cards.scryfall.io` from MTGJSON's per-set Scryfall ids, so downloading art no longer depended on the
  Scryfall API being reachable; it cached a per-set index on disk, refreshed it on a miss, and resolved
  ids off the UI thread. English printings only. New `MiniJson` reader, since forge-core/forge-gui
  carry no JSON library. (Replaced by upstream's `CdnUuidCache` on 2026-09-10.)
- **Fix: dev mode** — picking the left half of a split card to put onto the battlefield no longer
  gives you the right half.
- **AI:** `ChooseCard` needs enough valid choices before casting (Spry and Mighty was cast with no
  creatures), plus a `PowerDifference` logic for Spry and Mighty that picks the strongest and weakest
  creatures; `PlayAi` applies `ValidSA`/`WithTotalCMC` to the pool `ValidZone` selects (it had been
  casting "cast an instant or sorcery from your graveyard" with none there); `SlideOntoBattlefield` is
  never played by the AI.
- **Oracle text:** 166 faces in 164 scripts synced to Scryfall (self-references, reminder text, line
  breaks), `Oracle:` lines only; the commit calls them Alchemy faces (58 are `A-` rebalanced cards).
- **Upstream merge (11 commits, 2026-08-10).** Image fetchers: kept the fork's CDN placeholder
  resolution and shared naming for both Scryfall hosts, and adopted upstream's new `inScryfallCooldown`
  helper.

### 2026-08-07 to 08-09 (released 2026-09-25) — Unhinged colorless; pink; errata reversal; upstream merge (43 commits)

- **All remaining Unhinged colorless cards** (unh/121–135): Gleemax, Letter Bomb, Mox Lotus,
  My First Tome, Pointy Finger of Doom, Rod of Spanking, Time Machine, Togglodyte, Toy Boat,
  Urza's Hot Tub, Water Gun Balloon Game, World-Bottling Kit, City of Ass, R&D's Secret Lair.
- **Pink** as a sixth color, for the Giant Teddy Bear token.
- **Mox Lotus** produces genuinely infinite colorless mana, shown as ∞.
- **Time Machine** returns cards in a *later game*, queued on the `Match` by card name (a card
  object doesn't survive into the next game) and rebuilt as a Command-zone trigger keyed on the
  turn number.
- Card flags that survive the copy every zone change makes: `signed` (Letter Bomb),
  `switchedOn` (Togglodyte), `chosenExpansion` (World-Bottling Kit).
- **Errata reversal under R&D's Secret Lair**, printing-specific:
  - *Every pre-Dominaria printing* — "target creature or player" can't hit planeswalkers or battles.
  - *Debt of Loyalty* — control is gained unconditionally, not only on a successful regeneration.
  - *Bloodvial Purveyor* — the attack buff is permanent, with no "until end of turn".
  - *Marath, Will of the Wild* — a second ability with no `XMin1`, so X may be 0.
  - *Goblin King* (printed before Ninth Edition) — **all** Goblins get +1/+1 and mountainwalk,
    including itself; "Other" was added in 9ED.
  - *Relic Bind* (Legends printing only) — enchants **any** artifact, including your own; the
    opponent-only restriction was power-level errata applied in Fourth Edition.
  - *Lotus Vale* — a triggered ability rather than a replacement effect, so the land is already in
    play when it resolves and can be tapped for mana in response, then buried.
  - *Ashnod's Coupon* — the target player pays for the drink, not you.
  - *Celestial Dawn* — the printed card changes the **costs themselves** ("All colored mana symbols
    in all costs on all of these cards and permanents are {W}") rather than what mana may be spent
    on them. So the pips really are white — devotion, colored-pip counting and color identity all
    see {W} — and the Oracle version's "other mana only as colorless" restriction doesn't exist.
    Needed a new `SetColoredSymbolsTo$` continuous static (a *transform* of whatever the card
    happens to cost, unlike `ManaCost$`, which sets a fixed one).

- **Misprints under the Lair**, via a new `PrintingIs <SET>[:<collectorNumber>]` property. A misprint
  is printing-specific in a way errata isn't: only the physical copy carrying the mistake plays as
  written, so identity is the whole problem. All three are expressed as *boosts and cost reductions*
  gated on the printing, never as `SetPower`/`SetToughness`/`ManaCost$`, so they layer cleanly and
  the card is untouched without the Lair.
  - *Gríma Wormtongue* — the Secret Lair bonus card (SLD #734) was printed 2/4. Every SLD copy is
    misprinted, and Forge already had that printing.
  - *Orcish Oriflamme* — Alpha (LEA #166) printed the cost as {1}{R}; Beta corrected it to {3}{R}.
    Every Alpha copy is misprinted.
  - *Corpse Knight* — only **some** M20 #206 copies are 2/3, and they share a set and collector
    number with the correct ones, so this one needed a printing of its own: a new
    `206a … ${"variant": "Misprint"}` edition entry (following the existing `F203a`/`F203b`
    precedent) plus a `Variant:Misprint:` block to mark it. The toughness stays Lair-gated — the
    variant marks *which copy you own*, not what it does.

- Known cosmetic gap: the split-card *image* renderer still draws two halves for the five-face card.

- **New effects and properties for the Unhinged colorless cards:**
  - *Gleemax* — new `ChoosesTargets` static mode: its controller picks the targets of every spell and
    ability in the game (the newest Gleemax wins if two contradict). The handover also covers the AI's
    own spells (`ComputerUtil`, after it commits to casting; it keeps its own targets if the new chooser
    can't pick legally) and its triggers (`PlayerControllerAi`), not only the human path through
    `SpellAbility.setupTargets`.
  - *My First Tome* — new `GuessName` effect, shaped like `GuessArtist`: the guesser sees only the
    flavor text, and the verdict names the guess. A new `hasFlavorText` property keeps cards without
    flavor text out of the choice. The AI's `guessString` now guesses a card name when asked for one
    (it had always guessed an artist).
  - *Letter Bomb* — new `SignCard` effect; the `signed` flag shows as "(signed)" in the detail panel.
  - *Togglodyte* — new `ToggleSwitch` effect; `turnedOn`/`turnedOff` properties (a card with no switch
    is neither); "(turned ON)"/"(turned OFF)" in the detail panel.
  - *World-Bottling Kit* — new `ChooseExpansion` effect (every set is offered) and `ExpansionIsChosen`
    property.
  - *Time Machine* — new `QueueNextGameReturn` effect.
  - *Urza's Hot Tub* — new `sharesWordInNameWith` property (whole words, case-insensitive).
  - *Water Gun Balloon Game* — new `POP` counter, kept on players; the detail panel lists counters a
    card keeps on players (`InfoPlayerCounters` SVar). Its Giant Teddy Bear token
    (`p_5_5_giant_teddy_bear`) and the `Teddy Bear` creature type.
  - *Mox Lotus* — `Produced$ Infinity`. Generic costs are settled in bulk from an infinite pool, since
    every payment path otherwise spends generic one point at a time and Gleemax's {1000000} took a
    million trips through the payment machinery. ∞ shows in the desktop player panel and the mobile
    mana pool.
- **Engine, more:**
  - Pink is `MagicColor.PINK` with 32 new `ColorSet` constants (indices 32–63), and a pink shade in the
    card detail panel.
  - `SetColoredSymbolsTo` and `ManaCost` are now registered in `StaticAbility.generateLayer`; without
    that those statics were assigned no layer and silently never ran.
  - `CardView.updateManaCost` reports the changed cost as `ManaCost` and the printed one as
    `OriginalManaCost`; they were the wrong way round, which only showed once a card had a changed cost.
    The desktop card-image renderer now honors the "Perpetual Changes to Mana Cost" overlay preference,
    so a rewritten cost (Celestial Dawn as printed) matches the battlefield overlay.
  - Drawn mana symbols for pink and for generic costs past {20}, on desktop and mobile (new
    `FDrawnSymbol`): neither exists in any skin sprite, and on mobile a missing symbol raised a bug
    report.
  - Under R&D's Secret Lair, an illegal target pick explains the errata reason instead of quoting the
    card's own (overridden) target wording.
- **Fixes:** Ashnod's Coupon and Urza's Contact Lenses were declared "no cost" rather than {0}, making
  them permanently uncastable.
- **Upstream merge (43 commits, 2026-08-09).** Upstream added a `cause` (`SpellAbility`) parameter to
  `Player.loseLife`; the fork threaded it through the half-life paths (`changeLifeByHalves`, the
  private fractional overload, `LifeExchangeEffect`) and kept its half-life branch in `LifeLoseEffect`,
  half-aware mana burn in `PhaseHandler` and the half reset in `setLife`. Kept the fork's
  `entersWithCounters` property in `CardProperty`; `Teddy Bear` slotted in beside upstream's new
  Talosian and Tellarite types.

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
  lord no longer arrives with no Zombies). Random-color piles are preserved by design.
- **Card details (36fdd4c2369):** B-I-N-G-O puts a "chip" on each cast spell's mana value with the new
  `DB$ RememberNumber` and gets +9/+9 per `Count$CardBingoLines`; Elvish House Party is */* equal to
  `Count$CurrentHour`; S.N.O.T. reuses the mutate machinery and squares `Count$MergedCount`; Symbol
  Status creates a token per `Count$DifferentExpansionSymbols` (new `c_1_1_expansion_symbol` token
  script); Fraction Jackson uses `HasHalfSymbol`; Monkey Monkey Monkey chooses a letter and counts
  `nameStartsWithChosenLetter`; Stone-Cold Basilisk uses `fewerLettersInNameThanSource`; Rare-B-Gone
  uses `Rarity<name>`; Keeper of the Sacred Word and Meddling Kids take a typed word (`FreeInput$`,
  Meddling Kids with `MinLetters$ 4` and `textHasChosenWord`). Who // What // When // Where // Why is
  a single script with five faces (Who {X}{W}, What {2}{R}, When {2}{U}, Where {3}{B}, Why {1}{G}).
  The long-name Elemental's script is
  `our_market_research_shows_that_players_like_really_long_card_names_so_we_made_this_card_to_have_the_absolute_longest_card_name_ever_elemental.txt`.
- **Existing scripts touched (36fdd4c2369):** the Gotcha rewording covers Cardpecker, Deal Damage, Kill!
  Destroy!, Number Crunch, Save Life, Spell Counter, Stop That and Touch and Go (the cost now reads
  e.g. `If an opponent says "kill" or "destroy," say "Gotcha!"` instead of `Confirm an opponent
  said …`). Dumb Ass's attack choice gets `AILogic$ HinderAttacker`; Frazzled Editor gets the reminder
  "(Something is wordy if it has four or more lines of rules text.)"; Goblin S.W.A.T. Team now
  announces whether the table was swatted in time (`UnlessResolveSubs$ WhenNotPaid` plus the new
  `UnlessPaidSubAbility`); Yet Another Aether Vortex's "not implemented" note is replaced by the
  `TopLibraryPermanentsOnBattlefield` static; `Rotate180:True` on `role_cursed`, `role_sorcerer` and
  `role_young_hero`; `Expansion-Symbol` added to `TypeLists.txt`.
- **Engine, five-face splits in detail (9b3e57424d1):** `CardStateName` gains `Split3`/`Split4`/`Split5`
  and a `SPLIT_STATES` list. Code that hardcoded the LeftSplit/RightSplit pair now loops over the
  states a card has: perpetual mana-cost adjustment, intrinsic keywords, `sharesNameWith` off the
  battlefield, `CardCopyService` copies, `CardFactory` image/set/rarity setup and state copying, and
  `CardState`'s spell abilities, triggers, statics and replacement effects. `CardRules` combines name,
  mana cost, color, color identity and type over every face, and a card script's `ALTERNATE` now
  advances to the next face (previously it always jumped to face 1). `CardFactory` builds the extra
  states and the combined Oracle text over all faces; `CardView` gains `Split3State`–`Split5State`,
  lists every face in card text, and fills the face views in a separate pass before rendering ability
  text (a face's text can read a sibling). The desktop detail panel lists all five mana costs.
- **Engine, dual residency in detail (9b3e57424d1):** new static mode `TopLibraryPermanentsOnBattlefield`
  (`StaticAbilityTopLibraryOnBattlefield`; instants and sorceries never qualify, CR 304.4/307.4). Each
  state-based check moves a qualifying top library card onto the battlefield and re-lists it in the
  library as a shadow resident (`Card.shadowZone`, `Zone.addShadow`), so the library keeps its size and
  top card; a shadowed card that is no longer on top goes back to its library position and leaves the
  battlefield properly. A nontoken permanent put from the battlefield on top of its owner's library
  while the static applies just gains the shadow, with no leave or enter triggers. `Zone.remove` ends
  residency; a shuffled library leaves the battlefield as the card's primary zone.
- **Engine, other (9b3e57424d1):** `Count$CurrentHour` uses the twelve-hour clock (midnight and noon are
  12); `Count$MergedCount` is 1 for an unmerged card; `Count$DifferentExpansionSymbols` skips tokens;
  B-I-N-G-O's tracker (1 4 7 / 8 5 3 / 2 0 6, eight lines) lives in `getBingoLineCount`, with marks
  kept as remembered numbers rather than counters so proliferate can't touch them. `HasHalfSymbol`
  matches a half in printed P/T, a half mana shard, or ½ in rules text. `MinLetters$` re-asks a
  too-short typed word (up to 10 tries). `NotifyMessage` now pops up for any resolved ability, not only
  a RollDice result branch (the old RollDice-only code is removed). New additional ability
  `UnlessPaidSubAbility` resolves when an Unless cost was paid. Card scripts can set `Rotate180:True`.
- **UI (9b3e57424d1):** Rotate180 tokens are drawn turned 180° on desktop (picture panel and zoomer)
  and mobile, and offer the flip control to show them as physically printed. On mobile a card shown
  flipped 180° no longer renders 38% larger than its own front face. The chosen-type kind is tracked
  (`ChosenTypeKind`) for the "chosen word"/"chosen letter" label. The stack now shows "Kicked" or
  "Kicked xN" for multikicker spells. Dev mode's add-card falls back to the face's real name and then
  to any printing instead of throwing when a name lookup returns nothing.
- **AI (9b3e57424d1):** the attacks-alone fix drops a creature whose "attacking alone" evasion (a
  `CantBlockBy` static with an `attacking … EQ0` condition, e.g. Gutter Skulker) was lost and that a
  blocker would kill, unless its solo hit is lethal, in which case everything else stays home. Letter
  choices: `MostCommonInitial` picks the most common nonland initial on the battlefield (Monkey Monkey
  Monkey), otherwise a random letter (When Fluffy Bunnies Attack). New `HinderAttacker` choice logic
  (Dumb Ass): force the attack only when the AI has a blocker that kills it and survives, otherwise
  forbid it. Attack-damage prediction now counts attack-trigger Animate effects that set power (Shadow
  Puppeteers). Forcefield-style shields are no longer re-activated for an attacker the same source
  already shields (it used to keep paying until out of mana).
- **Deck generation in detail (9b3e57424d1):** `reinforceTypalPayoffs` finds the creature type most
  referenced in the deck's rules text (a card's own name ignored), and if the deck has fewer than
  max(4, size/10) creatures of that type, swaps random filler (not lands, payoffs or the theme) for
  on-color creatures of it. Only the single strongest theme is reinforced.
- **Upstream merge (12 commits, 2f2dc6d1df5).** No conflicts. Includes Hobbit draft rankings, MBC cards
  (Glava, Five-Advents Mage and the 5th August batch), Venser, Visionary Traveler, and several
  Hobbit card script updates.

### 2026-08-05 — Unhinged red; half mana production

- **Cards:** Assquatch, Curse of the Fire Penguin, Deal Damage, Dumb Ass, Frazzled
  Editor, Goblin S.W.A.T. Team, Mana Flair, Mons's Goblin Waiters, Orcish Paratroopers, Punctuate,
  Pygmy Giant, Red-Hot Hottie, Rocket-Powered Turbo Slug, Sauté, Six-y Beast, Touch and Go, Yet
  Another Aether Vortex (first two clauses), Zzzyxas's Abyss, Zombie Fanboy, Working Stiff, When
  Fluffy Bunnies Attack, Wet Willie of the Damned, Vile Bile, Tainted Monkey, Stop That, Phyrexian
  Librarian, Persecute Artist, Necro-Impotence.
- Half mana **production**; half life fixes (cost prompt, affordability, copy, set, repaint).
- Flavor text data available in-game.
- Oracle text sync rounds 4–5 (self-reference pronouns, retemplating).
- Chosen artist shown in the detail panel.
- **Single-card commits:**
  - Necro-Impotence (9448f10be64): new `PayLifeHalves<N>` cost (`CostPayLife` halves mode, paid through
    `changeLifeByHalves`; payable at 1/2 life, not at 0); the exile-and-return half is Necropotence's.
    Follow-up b5c0348ddc5: the human and AI cost prompts read the amount in halves (it asked "Pay 1
    life" and refused at 1/2 life), and the upkeep untap now uses an explicit untap-up-to choice (it
    untapped nothing).
  - Persecute Artist (e44083fd71a): reuses the artist picker and `ArtistIsChosen`, with a new
    `ExcludeArtist` param so it can't name its own illustrator; 032b71cea4a also excludes the database's
    alternate credit `Rebecca "Don't Mess with Me" Guay` and capitalizes `lblReveal` ("Reveal …'s
    hand").
  - Phyrexian Librarian (370e305a620): upkeep exiles the top card as the balanced pile (listed on the
    card via remembered cards); the falling half is an any-player honor-system ability.
  - Stop That (8c345c0598e): targeted discard plus the Gotcha graveyard return.
  - Tainted Monkey (cfa91ee7553): new `textHasChosenWord` property, matched on whole words. Follow-ups:
    f982890ae24 adds `FreeInput$` to ChooseType so a human types the word (the list stays as the AI's
    vocabulary); 2ebac10b8dd adds a `Chooser$` param so the Monkey's controller, not the targeted
    player, picks; f370a17a3f2 matches the generated rules text as well as the stored Oracle field.
  - Vile Bile (137bcae903f): 2½/2½ Ooze; touching it is an any-player ability whose activator loses 2
    life.
  - Wet Willie of the Damned (0214d5f2039): `Halves` on both DealDamage and GainLife.
  - When Fluffy Bunnies Attack (cb3e12235b6): new `Count$ChosenLetterInName` (occurrences of the chosen
    letter in a card's displayed name); the caster picks from a 26-letter list.
  - Working Stiff (2d739356ca4): the arm-straightening instruction is a `Text` line; bending an elbow is
    the any-player honor-system sacrifice.
  - Zombie Fanboy (914e2c233a7): ETB artist choice plus `ArtistIsChosen` on a dies trigger.
  - Zzzyxas's Abyss (12091d4f6ff): new `alphabeticallyFirstNonLand` property (ties all qualify; compares
    displayed names).
- **Red cards in detail (33a3aacc417, unh/71–91):** Assquatch (+1½/+1½ to other Donkeys via
  `AddPowerHalves`/`AddToughnessHalves`), Curse of the Fire Penguin (6/5 trampling Penguin with all
  abilities removed; returns from the graveyard attached to a creature when the enchanted creature
  dies; a second state for the zoom view), Deal Damage and Touch and Go (Gotcha cards), Dumb Ass (coin
  flip; an opponent decides whether it attacks), Frazzled Editor (protection from `wordy`, four or more
  text-box lines measured like Lexivore), Goblin S.W.A.T. Team (say-the-name honor cost, opponent may
  swat), Mana Flair (artist choice, {R} per nonland permanent by that artist), Mons's Goblin Waiters
  (sacrifice for {HR}), Orcish Paratroopers (ETB drop resolved as a two-sided roll), Punctuate
  (`Count$CardPunctuationMarks`, halved damage), Pygmy Giant (`TextBoxNumbersOf$` choice from the
  sacrificed creature's text box), Red-Hot Hottie (new `third-degree-burn` counter, label BURN, plus a
  permanent scream-or-sacrifice trigger), Rocket-Powered Turbo Slug (super haste: put onto the
  battlefield attacking, lose next turn unless its cost is paid), Sauté (3½ damage), Six-y Beast
  (secret 0–6 counters, opponent guesses), Yet Another Aether Vortex (haste and revealed top card).
- **Engine in detail (33a3aacc417):**
  - Half P/T boosts: `addPTBoostHalves` keeps a leftover half per Layer 7c boost; net power, net
    toughness and combat damage are computed from a halves total (fixing two +1½ boosts on a 3½
    toughness reading 5 instead of 6), and the card view shows a half from boosts, not only printed.
  - Half mana production: a produced `H<color>` token is no longer misread as two whole colors; a
    second half of a color merges into a whole mana; with mana burn on, unspent halves burn for
    1/2 life each when the pool empties; a lone floating half now repaints the mana pool display.
  - Flavor text: `CardFlavorText` loads `res/lists/FlavorText.txt` (55,662 printings keyed by set and
    collector number, generated by `_tools/flavor-text/extract.js`) lazily; `Card.getTextBoxContents`
    is rules text plus flavor text, and the chosen-word check reads it too.
  - `getPunctuationMarkCount` (the marks Punctuate lists, straight and curly quotes included) and
    `getTextBoxNumbers` (numerals plus spelled-out number words) for Punctuate and Pygmy Giant; new
    `wordy` card property for Frazzled Editor.
  - `ChosenNumber` is read before the LKI swap in xCount; replacement effects hand replacing objects to
    additional abilities as well as the SubAbility chain (fixes ETB counters from
    `ChooseNumberSubAbility`, which died with an NPE); a ChooseNumber `Guesser` without
    `RemoveChoices` now gets to guess.
  - New `THIRDDEGREEBURN` counter type; creature types Bureaucrat, Paratrooper, Penguin and Waiter.
- **NICKNAME sweep (33a3aacc417):** 764 existing legendary card scripts changed, 855 script lines
  (`CARDNAME` → `NICKNAME` in display text, the commit message counts 856 fields), so they read with
  the shortened name current Oracle uses (e.g. Ace, Fearless Rebel; Zurgo Helmsmasher).
- **AI (33a3aacc417 and single commits):** super haste reserves the mana for next turn's payment
  (`HELD_MANA_SOURCES_FOR_OBLIGATION`, kept across turns until the debt comes due) and only attacks
  if every colored pip can be matched to a separate source and the Slug survives any block; Pygmy
  Giant's `AILogic$ Max` takes the largest number; Mana Flair's `MostProminentNonLand` artist logic
  counts nonland permanents on both sides. `LifeLoseAi` accepts an honor-system life loss once a
  `Chance.N` roll has decided to (not if it would leave the AI below 5 life), only in its own main
  phase (a4d69f26da7, 13368cdd47f).
- **Half-life fixes in detail:** `setLife` clears the leftover half (95a47dbc577); a life total copied
  from another player keeps its half through a new `LifeTotalHalves` count and `setLifeInHalves`
  (f4e11f7cd72, Repay in Kind); changing only the half fires a stats-changed event so the life panel
  repaints (7f5a0b2debc); a player's half damage is applied immediately rather than waiting for later
  damage (8542b8ece60, Wet Willie dealt 2 instead of 2½).
- **UI:** the chosen artist is carried on the card view and printed in the card detail panel
  (7eef4b5d34d). Desktop battlefield zoom no longer hardcodes `mayFlip=false`, and the zoomer applies
  the flip rotation after rebuilding the image panel, so CTRL-rotate works on flip cards again
  (33a3aacc417).
- **Oracle sync rounds in detail:** round 4 (e1e6a418921, after refreshing the Scryfall bulk data, now
  gzipped JSONL) changed 47 display fields in 38 scripts that still named the card where Oracle uses a
  pronoun, mostly the fork's own Un-set scripts (e.g. Bloodletter, Farewell to Arms, Loose Lips,
  Mother of Goons, The Fallen Apart, Greater Morphling) and the Eldrazi Scion family. Round 5
  (91b45f4f066) changed 93 fields in 78 scripts that needed a second change with the pronoun: token
  self-reference ("This token" inside a token's quoted text), When/Whenever corrections, "upkeep or
  whenever" → "and whenever", and small wording fixes; deletion clusters were left out on purpose.
  Display-only keys; no `Cost`, `K:` or `AddHiddenKeyword` changes.
- **Upstream merge (25 commits, 31a58c2c4bc).** One conflict, Shimian Specter: resolved with upstream's
  new reveal/choose logic and the fork's "Whenever this creature deals combat damage" trigger text.
  Includes the first MBC cards, TappedOut and MTGGoldfish deck URL loading, the JSONL Scryfall price
  generator, and "only let a card be cast from its owner's hand or graveyard".
- **Upstream merge (2 commits, 91acb961cb8).** No conflicts (Óin the Brave update; Silvan Reveler loses
  Flying).

### 2026-08-02

- AI stops attacking into walls that just absorb it.
- In detail (35257a92fcf): at aggression 4 a defending power of 0 no longer justifies an attack as
  "free" by itself (a Faerie Miscreant kept swinging into a 0/5 reach defender). Blockers that neither
  die to the attacker nor kill it count as absorbing the attack only when there are enough of them to
  soak up everything the AI could send, so surplus attackers still go in.

### 2026-08-01

- **Cards:** Mother of Goons, Kill! Destroy!, Infernal Spawn of Infernal Spawn of Evil, Farewell to
  Arms, Eye to Eye, Mouth to Mouth, Loose Lips.
- `Chance.N` reworked: rolled once per turn, one attempt per turn, honored by every API.
- AI battle handling: don't let a battle swallow the whole attack step; send leftover attackers;
  only commit the whole attack to a battle it can finish.
- Four AI fixes: prowess timing, vehicle reanimation, named card legality, tap-ability hoarding.
- **Card details:**
  - Farewell to Arms (e321d2d592d): honor-system card built like Handcuffs; choose an opponent as it
    enters, and any player may sacrifice it (cost: confirming the chosen hand isn't behind its owner's
    back) to make that player discard their hand. acd1a197149 has it also choose which hand, through
    ChooseDirection with new optional `ChoiceTitle` and `NoTurnOrderInfo` params; 86c33b76c20 adds a
    `DirectionLabel` param carried through the card view, so the infobox reads "Chosen hand: Left".
  - Infernal Spawn of Infernal Spawn of Evil (e1dd55869f6): the mid-search window ChangeZoneEffect
    opens for Panglacial Wurm now also offers activated abilities marked `ActivateWhileSearching`,
    leaving the card in the library; new `RevealFromLibrary` cost variant. d9e94f04e19 keeps the card
    face up to all players until end of turn after it activates from the library.
  - Kill! Destroy! (22a78768110): Destroy plus the Gotcha graveyard return.
  - Mother of Goons (80a747a6006): whenever an opponent's creature dies, pick one of 34 insults and
    confirm you said it or sacrifice her.
  - Mouth to Mouth (d2ecb382940) and Eye to Eye (9d1cc5464fe): the contest is a real coin flip with
    `NoCall` instead of the caster declaring the winner.
  - Loose Lips (48b91b5f810): the AI says the sentence 95% of the time (was 30%).
- **`Chance.N` in detail:** the roll moved into `canPlayWithSubs`, which every AI decision passes
  through, because most API classes override `canPlay` and never reached it (ControlGain never rolled,
  so the AI took Question Elemental? back at once); GenericChoice is excluded since it reads `Chance.N`
  at resolution (898a33853fd). The roll is seeded by card, turn and ability, so it is one decision per
  turn rather than one per priority (6bae7fc0cde), and the AI gets one attempt per turn, the same check
  `AILogic$ Once` uses; the seed is avalanche-mixed (e158b2ab25c).
- **Four AI fixes in detail (e713165386c):** cast-in-main-1 treats any noncreature spell as a prowess
  trigger, and only when the prowess creature can attack; the hidden-trigger check judges remembered
  cards directly (Nautiloid Ship exiles an opponent's graveyard); the `chooseCardName` fallback respects
  `ValidCards` (Psychic Paper named a sorcery); attacking is skipped for a tap ability only when the AI
  also wants to use it (Sterling Keykeeper never attacked).
- **Battle attacks in detail:** a battle is only the chosen defender when some creature can attack it,
  with a fallback to the protecting player (350843303ac); battles the AI or an ally controls join the
  leftover-attacker rotation, fewest defense counters first (6d82a9bd705); the whole attack goes at a
  battle only when available power covers its remaining defense counters (42826c765a1).
- **Upstream merge (42 commits, ebb60df6e2d).** Conflicts in Cogwork Tracker and Fleshformer, both
  resolved by taking upstream's scripts (Cogwork Tracker's Dog type, Fleshformer's "activate only during
  your turn"). Includes Hobbit (HOB/HOC) cards and the enduring story mechanic, several AI simulation
  improvements, and conspiracy cards in custom cube drafts.

### 2026-07-31

- Dev mode can pick which printing to add.
- Framed! chooses its mode at resolution.
- Flavor names count in name-based mechanics.
- In detail: the add-a-card and cast-a-spell dev tools show a printing chooser when a card has more
  than one printing; cancelling keeps the default printing and repeat-last reuses the chosen one
  (2d3339cb0c7). Framed!'s Charm was a plain SubAbility, so its mode was never chosen; it is now a
  GenericChoice made after the artist is picked (c74d54f6b82). Every name-reading Un mechanic uses the
  displayed name, so a flavor name counts as printed (Goro Rel, Scourge to Spiders reads five words)
  unless an effect has overwritten the name; Seven Questions accepts either name as the guess
  (ad31579576b).

### 2026-07-28 to 07-30

- **Cards:** The Fallen Apart, Eye to Eye, Duh, Bloodletter, Bad Ass, Aesthetic
  Consultation.
- Half damage shown on creatures; half kept when power becomes damage; `WordsInName` readable from a
  static ability.
- Head to Head prevention as a named effect; Man of Measure rules text; granted land types read from
  the card.
- **Card details:**
  - Aesthetic Consultation (f1086f587a4) and Eye to Eye (7bdb7a53e88): new scripts.
  - Bad Ass (e3244bd458a), with its growl prompt reworded in 2b9ffc1c167.
  - Bloodletter (2b9ffc1c167): new `MostSharedInitial` count (the largest group of nonland permanents
    sharing a first letter) so its state trigger is an ordinary CheckSVar.
  - Duh (1ff80103d55): new `hasReminderText` property reading the parenthesized text in a card's Oracle
    field; ea5ab1985cb lets tokens qualify through their keywords' own reminder text, since token
    scripts carry none.
  - The Fallen Apart (b66c7d8d14b): arm and leg custom counters. 8c59991047d: Charm drops modes whose
    own condition can't be met, and the trigger checks for a limb left to lose. 9445d66aec3: new static
    `ActiveDescription` param, an extra line shown only while the static's conditions hold, so it
    reads "This creature can't block." once its arms are gone; counter changes rebuild the card text.
  - Fascist Art Director (39ff06da451): its protection names the picked artist ("protection from Rob
    Alexander"), via a `ChosenArtist` option for Pump's `DefinedKW`.
  - Head to Head: the asker can stop and guess before six questions (428422ba954); the prevention gets
    a named effect card and the chosen card is cleared afterward (4d9a1dd74b8).
  - Man of Measure (5a1eefaea74): two description-only statics show its rules text on the card.
- **Half values:** the mobile floating avatar number shows half life changes as fractions
  (e4318ec4527). Save Life's leftover half shield also prevents ordinary damage, leaving half damage
  marked (14985d03e55), and marked half damage now shows on the battlefield overlay and in the detail
  panel on desktop and mobile (9da18989666). Damage "equal to its power" is re-read in halves
  automatically, and Fight reads both fighters' power in halves, covering the ~400 cards worded that way
  without editing upstream scripts (a31a6d6a112). `WordsInName` moved out of xCount's
  SpellAbility-only block, so Wordmail's static bonus works (a9c411b646c).
- **Granted land types:** 39ff06da451 spelled out a basic land type's mana reminder for land types a
  card wasn't printed with, and f70aeb029b0 read the granted type from the card rather than the text
  state (Yedora, Grave Gardener); 9445d66aec3 then dropped that reminder again, since it only reached
  the hidden front face. A "refresh cached card text when a static attaches" change (22d29047d22) was
  reverted the same day (8ab999f9b07).
- **Upstream merge (11 commits, 09ac715c447).** No conflicts. Includes snapshot fixes (restoring a
  snapshot taken before a card existed, melded permanents), early Hobbit (HOB/HOC) cards, Suspend
  tweaks and Realm of Legends 1.065.

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
- **Upstream merge (26 commits)**, 07-27, no conflicts.
- **Cards, in detail (white):**
  - **Collector Protector** — `{W}` plus giving an opponent a nonland card "from outside the game"
    prevents the next 1 damage to you or it; the card comes from your whole collection (see the
    `GiveFromCollection` cost below).
  - **Drawn Together** (creatures by the chosen artist get +2/+2) and **Fascist Art Director**
    (protection from an artist of your choice) reuse the `ChooseArtist` API.
  - **Erase (Not the Urza's Legacy One)** — free to cast if you control two or more white
    permanents that share an artist; **First Come, First Served** — first strike for the attacking
    or blocking creature with the lowest collector number.
  - **Emcee** — "Presenting . . ." is an honor-system `FlavorAction` unless-cost for the +1/+1
    counter.
  - **Head to Head** — Seven Questions played for real: the engine answers up to six yes-or-no
    questions from a catalog truthfully from the actual top card, then you guess the name; works
    against the AI. **Frankie Peanuts** — rolls a d30 for one of thirty yes-or-no questions and
    takes the target player's answer; abiding by it stays honor system.
  - **Ladies' Knight** — each player is asked as it enters; those who say yes get a per-player
    {1} cost reduction for as long as it's on the battlefield.
  - **Little Girl** — printed `{HW}` mana cost and .5/.5 P/T.
  - **Look at Me, I'm R&D** — choose a number and one a step either side; every printed instance of
    it is changed (whole numbers, not digits, so 20 survives a 2→3 swap; 0 may become −1; fractions
    such as ½ → 1½ or −½ allowed). See the number-swap engine bullet below.
  - **Man of Measure** — your height against your opponents' is declared as it enters (shorter,
    taller, both, neither) and grants the matching bonuses.
  - **Save Life** — modal: gain 2½ life, or prevent the next 2½ damage to a creature; Gotcha
    returns it from the graveyard.
  - **Standing Army** — standing up and sitting down are two honor-system abilities that toggle a
    STANDING counter; vigilance while it's there.
  - **Staying Power** — "until end of turn" and "this turn" effects don't end (new static mode).
  - **Wordmail** — +1/+1 for each word in the enchanted creature's name (`Count$WordsInName`).
- **Cards, in detail (blue):**
  - **_____** — `{1}`: its name becomes the card name of your choice, activatable from any zone.
  - **Ambiguity** — triggers on a spell that counters a spell, and on a spell that will enter with
    counters (looked at while still on the stack); the player who caused it chooses "counter the
    next spell" or "put an additional counter on a permanent that already has one".
  - **Artful Looter** — looter that untaps when a permanent sharing an artist with another of yours
    enters.
  - **Avatar of Me** — costs {1} more per ten years of your age; power is your height in feet and
    toughness your American shoe size, both rounded to the nearest ½; its color is your eye color
    (colorless if that isn't a Magic color, which is recorded and shown; can change at instant speed
    per ruling 4; heterochromia supported).
  - **Brushstroke Paintermage** — a target permanent's artist becomes one of your choice until end
    of turn.
  - **Bursting Beebles** — unblockable while the defending player controls two or more nonland
    permanents that share an artist.
  - **Carnivorous Death-Parrot** — sacrifice unless you say its flavor text (honor-system unless
    cost; the AI says it 85% of the time).
  - **Cheatyface** — sneaks from hand onto the battlefield for free; as that resolves the opponent
    is asked whether they caught you, and exiles it if so (reworked from an opponent-activated
    ability, which the AI never considered, so the AI now catches it 50% of the time; both outcomes
    now describe themselves on the stack).
  - **Double Header** — may bounce a permanent with a two-word name.
  - **Flaccify** — counter unless its controller pays `{3}{½}` (new half-generic shard).
  - **Framed!** — choose an artist; tap all untapped or untap all tapped permanents by that artist.
  - **Greater Morphling** — seven `{2}` abilities: a keyword from its list (landwalk of a chosen
    type, protection from a chosen color, …), colors, creature type, expansion symbol, artist,
    +2/−2 or −2/+2, untap.
  - **Loose Lips** — flying aura; a sentence is chosen from a list as it enters, and an opponent it
    damages either says it or you draw two.
  - **Magical Hacker** — swaps every + and − in a target spell or permanent until end of turn.
  - **Moniker Mage** — saying a middle name is the honor-system part of each ability's cost.
  - **Mouth to Mouth** — the breath-holding contest is answered as a yes/no choice; winning gains
    control of the target creature.
  - **Now I Know My ABC's** — wins at upkeep if your permanents' names cover all 26 letters.
  - **Number Crunch** and **Spell Counter** — Gotcha returns from the graveyard.
  - **Question Elemental?** — an opponent may take control of it by confirming you said something
    that wasn't a question.
  - **Richard Garfield, Ph.D.** — Mental Magic: pick a card in hand, name a different card with the
    same mana cost, exile the original and cast the named one; names can't repeat.
  - **Smart Ass** (2½/1) — names a card on attack; a defending player without it either reveals
    their hand or it can't be blocked this turn.
- **Engine — half mana:**
  - `ManaCostBeingPaid` carries a half beyond its whole shards; paying it takes a whole mana and
    the unused half becomes change. First built as a separate remainder (which hung casting
    Little Girl, since nothing could ever pay it off), then reworked so a half is an ordinary
    `ManaCostShard` in the cost map — payment, counting, display and cancellation use the existing
    machinery, and Little Girl's printed `{HW}` and Cheap Ass's `{½}` reduction are the same object.
  - `ManaPool` holds floating halves per color, spendable on a later half cost and emptied with the
    rest of the pool at end of phase. No change is given during trial (test) payments, which had
    deposited a phantom half and made unaffordable cards look castable.
  - `ManaAtom.IS_HALF` and a half shard of each color (parsed from an `H` prefix, contributing 0 to
    mana value), plus a half-generic `{½}` shard payable with any color (Flaccify).
  - `CostAdjustment` keeps an odd `{½}` reduction via `reduceByHalf` instead of flooring it.
  - Floating halves now fire pool events, trigger the end-of-phase mana-loss warning, and show in
    the mobile zone tab.
- **Engine — other half values:**
  - Half power/toughness moved from `Card` onto `CardState`, beside base P/T, so copies, clones and
    alternate faces carry them; `CardState.copyFrom` copies them; setting a half refreshes the view.
  - `PreventDamage` gained `Halves$ True`: the whole part is a normal shield, the odd half is held on
    the target and consumed by a fractional creature's leftover half; cleared at cleanup.
- **Engine — new APIs, costs and properties:**
  - `GiveFromCollection` cost: choose from the whole card pool (like dev mode's picker), created in
    the receiving opponent's sideboard. `ExileFromSideboard` added as a general cost keyword.
  - `SevenQuestions` (Head to Head), `ChangeNumber` (Look at Me, I'm R&D), `AskFact` and
    `ChooseEyeColor` (Avatar of Me), `SetArtist` (Brushstroke Paintermage), `SetExpansion` (a
    display-only expansion-symbol override, separate from the real set code; Greater Morphling).
  - Number swap held on the `Game` (in halves) and applied wherever a printed number is read:
    literal numbers in `AbilityUtils.calculateAmount`, base P/T, the generic part of a mana cost in
    `CostAdjustment`, and the collector number; displayed rules text rewritten on the view, and the
    chosen pair shown on the enchantment.
  - `StayingPower` static mode: end-of-turn commands aren't run while it's out, so they all run at
    the first cleanup after it leaves; damage still wears off.
  - Player facts (age, height, shoe size) asked once and kept for the game; `SetPowerHalves`/
    `SetToughnessHalves` static params, registered with the characteristic-defining P/T layer.
  - `Card.getArtist` can now differ from the printed artist and every artist check reads it;
    `ChooseArtist` offers every artist in the card database (falling back to an in-game scan), and
    its AI takes `AILogic$ MostProminentYouCtrl` for pump effects.
  - Card properties: `sharesArtistWith` and `sharesArtistWithOther` (measured from the tested card's
    controller, with the nonland requirement built in so the restriction holds no `+`),
    `attackingOrBlocking`, `LowestCollectorNumberAmong` (digits only; no printed number never counts
    as lowest; tokens have numbers too), `entersWithCounters`, `nameWords`.
  - `AdditionalActivationZone` accepts a list; `Animate` takes `Name$ ChosenName`; the "Flashback
    and others" zone now lists cards with additional activation zones, and cards naming Library work
    at any depth, not just on top.
  - `Charm`'s `Chooser` accepts any defined player, not just Opponent.
  - Magical Hacker's flip folded into `ChangeText`: it flips displayed text, Pump amounts, +1/+1
    counters, anthem statics and loyalty costs (a flipped −N gains loyalty; a flipped +N needs the
    counters; payment adds counters through a counter table), and the stack and ability menu
    re-render.
  - `Count$AlphabetCoverage`; `ChooseCardName` gained a `ManaCost$ Chosen` filter and name
    exclusion; `MakeCard` gained `LastNamedCard`.
  - Creature type Elemental? registered.
- **UI:**
  - Half P/T shown in the remaining render paths (the card-state view, mobile
    `CardImageRenderer`, and the shared detail text in `CardDetailUtil`): a bare ½ with no whole
    part, −½ when negative.
  - Floating half mana shown in both mana pools (desktop details label gained a `displayText` hook).
  - Half mana symbols: desktop draws `{HW}` as a half sign beside the color's symbol (a bare
    fraction for the generic half), Android maps half shards to the color's texture; an unknown
    symbol is now skipped instead of crashing the window.
  - Save Life's shield reads 2½ in its text and on the stack.
  - Mobile: battlefield creatures of different colors no longer stack into one tile.
- **Fixes:**
  - Crash on drawing any card (number-swap and collector-number lookups on a UI-only card with no
    game) and hang on casting Little Girl.
  - NPE reading a set code during card construction, on every card at game start.
  - Cancelling Richard Garfield's card pick no longer crashes.
  - Avatar of Me: age cost used a nonexistent `Div` xMath op ({34} instead of {3}); its questions
    moved to ETB (a continuous static can't prompt, so it died as a 0/0); `EffectZone` dropped from
    its P/T and color statics.
  - Bursting Beebles: the restriction's `+` leaked out through both card and player valid-string
    splitting.
  - Greater Morphling: chosen landwalk type now substituted; chosen type and color cleared when the
    effects end, off the stack.
  - Ambiguity's counter-next-spell mode was a blank row in the mode list.

### 2026-07-25

- Unhinged/Arena promo cards; half life support; half power and toughness.
- **Upstream merge (34 commits)**, no conflicts.
- **Cards:** Atinlay Igpay (sacrificed when you speak a non-Pig-Latin word), Bosom Buddy (gain ½
  life per word in a cast spell's name), Cardpecker (Gotcha), Circle of Protection: Art (choose an
  artist; prevent damage from a source by them), Goblin Mime (sacrificed when you speak), Granny's
  Payback (gain life equal to your age), Cheap Ass (spells cost `{½}` less; first a floor-based
  reduction that discarded an odd half, superseded minutes later by real half mana).
- **Fixes:** Ass Whuppin' now matches its Oracle text ("silver-bordered or acorn"), which its
  literal silver-border check couldn't target.
- **Engine:** `ChooseArtist` API, `Card.chosenArtist` and the `ArtistIsChosen` property; the AI
  weights an artist by the opposing permanents they drew. `Count$WordsInName`. `AILogic$ Age` so an
  AI asked for a real-world number doesn't answer with the maximum.
- **Half life:** a separate 0-or-1 half rides alongside the int life total; a fractional
  gain/loss still runs replacements and fires life triggers, with the amount reported downstream
  rounded down (107.1b). `Halves$` on GainLife/LoseLife. Life shows as e.g. "19½" on desktop and
  mobile.
- **Half power and toughness:** `PT:1.5/1` parsed, halves set at card creation; net P/T and
  lethal damage computed in halves (704.5f and the lethal-damage SBA compare in halves), respecting
  P/T switching; combat deals a fractional creature's leftover ½ (as ½ life loss against a player),
  and a ½-power attacker is no longer dropped for dealing 0 whole damage; "1½" rendered on desktop
  and mobile. Cardpecker restored to its printed 1½/1.
- Creature types Mime, Igpay and Donkey registered.

### 2026-07-23

- **The full Unglued (UGL) silver-bordered set.**
- In-game ability text synced to current Scryfall Oracle wording.
- **Upstream merge (11 commits)**: conflict in Fear of Falling resolved by keeping this fork's
  "this creature" description and upstream's `Duration$ UntilYourNextTurn` on the −2/−0.
- **Unglued cards added (49):** Ashnod's Coupon, B.F.M. (Big Furry Monster), Bronze Calendar,
  Bureaucracy, Burning Cinder Fury of Crimson Chaos Fire, Censorship, Chaos Confetti, Charm School,
  Checks and Balances, Clam-I-Am, Clam Session, Clay Pigeon, Common Courtesy, Deadhead, Double
  Cross, Double Deal, Double Dip, Double Play, Double Take, Free-Range Chicken, Gerrymandering, Get
  a Life, Ghazbán Ogress, Giant Fan, Goblin Bookie, Gus, Handcuffs, Hurloon Wrangler, I'm Rubber,
  You're Glue, Infernal Spawn of Evil, Jalum Grifter, Jester's Sombrero, Knight of the Hokey Pokey,
  Landfill, Lexivore, Mesa Chicken, Mirror Mirror, Miss Demeanor, Ow, Prismatic Wardrobe, Psychic
  Network, Ricochet, Sex Appeal, Sorry, Squirrel Farm, Team Spirit, The Ultimate Nightmare of
  Wizards of the Coast® Customer Service, Urza's Contact Lenses, Volrath's Motion Sensor.
- **Unglued cards updated (17):** self-references reworded to current Oracle ("this creature",
  "this artifact", …) on Blacker Lotus, Chicken Egg, Clambassadors, Elvish Impersonators,
  Free-for-All, Goblin Bowling Team, Hungry Hungry Heifer, Jack-in-the-Mox, Jumbo Imp, Krazy Kow,
  Mine, Mine, Mine!, Temp of the Damned (also "equal to the result") and Urza's Science Fair
  Project; Organ Harvest now repeats over your team rather than non-opponents; Poultrygeist's roll
  is optional; Spatula of the Ages finds "silver-bordered or acorn" permanent cards. The Unglued
  edition file drops B.F.M.'s A/B suffixes and adds the ® to The Ultimate Nightmare.
- **Engine (Unglued):**
  - `CostFlavorAction`: an unenforceable physical/verbal action cost (Mesa Chicken, Bronze
    Calendar, Clay Pigeon, Knight of the Hokey Pokey, …); the AI can always "pay" it.
  - `PlayerController.guessString` and the `GuessArtist` API: a free-text guess compared with a
    hidden fact (Squirrel Farm); the AI guesses an artist from the cards it can see.
  - `Gerrymander` API: per-player-count land redistribution (Gerrymandering).
  - Cast-time `Announce$`/RaiseCost for `{X}{Y}{Z}` costs, with announced-value caps checked across
    chained sub-abilities (The Ultimate Nightmare…).
  - "First upkeep of your next game in this match": `NextGameFirstUpkeep` on GainLife, Draw,
    Discard, DealDamage and ChangeZone queues the effect on the `Match`, delivered as a real
    Command-zone trigger at the next game's first upkeep (the Double cycle).
  - `Match.getOutcomesInOrder`, games-won-this-match and per-opponent loss-streak counts (Ghazbán
    Ogress, Gus), scoped to the current match.
  - Card properties `SilverBordered` (any Funny-type edition, covering acorn too),
    `TargetedPlayerTeamCtrl` (Spatula of the Ages, Team Spirit) and `mostLinesOfText` (Lexivore;
    lines estimated by word-wrapping the Oracle text at 40 characters per line).
  - Counter-type conversion on move, read from the destination's printed text (Giant Fan).
  - `ZoneExchangeAll` API (hand/library/graveyard ownership swap) and an exchange-everything `All`
    mode on the control-exchange effect (Mirror Mirror).
  - Wholesale sideboard removal via `RemoveFromMatch` (Jester's Sombrero).
  - Per-card `MayLookAt` "other players" mode, hiding a zone from its own owner only (Psychic
    Network).
  - Team life exchange as a cyclic rotation (Get a Life); `DefinedMagnet` can redirect to a player
    (Ricochet); dice rolls gained lowest-player rerolls to break ties (Ricochet), a repeat-total
    flag (Free-Range Chicken), per-result notify messages (Knight of the Hokey Pokey), Clam-I-Am's
    free reroll of 3s, and Goblin Bookie's reflip/reroll (coin flips too); `ChooseSource`
    `Accumulate`; a tapped-a-nonland-permanent-this-turn player property; `ControllerIsAI` trigger
    condition (I'm Rubber, You're Glue); tap triggers expose the tapping player.
  - Multiword creature subtypes registered for B.F.M. and Lady of Proper Etiquette.
- **AI (Unglued):** `AILogic$ Chance.N` for honor-system abilities, unless-costs and generic
  choices (attempt N% of the time instead of never); Lexivore attack logic that holds back when its
  trigger would be forced onto one of the AI's own valuable permanents; `AvoidTargetColors` color
  choice (Prismatic Wardrobe).
- **UI:** a popup summarizing what a scry did (cards kept on top, in order, and put on the bottom).
- **In-game ability text sync:** 10,531 scripts (10,528 cards, 3 Eldrazi tokens) changed, 12,707
  lines, almost all in `TriggerDescription$` (7,683), `SpellDescription$` (2,866) and
  `Description$` (2,061); 12,319 lines replace `CARDNAME` with the Oracle self-reference ("this
  creature", "This land", "this Aura", "it", …); 123 lines of granted-ability text on Eldrazi
  Spawn/Scion-style tokens change "Sacrifice this creature:" to "Sacrifice this token:" (also in 61
  `Oracle:` lines); 45 lines change "enters" to "enters the battlefield"; about 100 lines of other
  wording drift and typo fixes; 78 `Oracle:` lines touched in all. 121 d20
  result-row descriptions had their `VERT` placeholder replaced with a literal `|` (restored
  2026-09-30).

### 2026-07-21 — first changes

- Runeblade Raiser (Alchemy: Tarkir), Aquatic Subtlety (Alchemy: Lorwyn Eclipsed).
- Several new/unimplemented cards plus supporting engine features.
- Android debug build made self-contained and update-safe — bundles this fork's own
  `assets.zip` so the app never silently replaces it with upstream's card database, and uses a
  timestamp-based versionCode so installs are genuine in-place updates.
- **Upstream merge (5 commits)**, no conflicts.
- **Cards:** Cloudsculpt Armorer, Limitless Rekindling, Opulent Clomper, Providence of Night,
  Summitfest Closing Ceremony, The Mystical Archive, Ursine Guide // Ranger's Merit; Circadian
  Struggle fixed (a dangling `SubAbility$ DBEffect` removed from its cleanup).
- **Engine:**
  - `LosePerpetualEffect`: `Defined$` targeting, triggers matched by their Execute SVar rather than
    object identity, and the ability text refreshed after removal — so Runeblade Raiser can strip
    its own perpetual dies-trigger while changing zones.
  - Perpetual keyword grants (Animate, Pump, PumpAll) hand their own derived keywords to
    `PerpetualKeywords` as its initial cache, so a granted Evoke, Echo, Dethrone, Demonstrate or
    Double team no longer fires twice (found via Aquatic Subtlety).
  - `MakeCard` `RandomSet$` (with `RandomSetTypes$`): conjure at random from whole editions
    (Limitless Rekindling).
  - `ChooseColor` `ExcludeOwnColors` (Opulent Clomper).
  - Starting-deck tracking: library cards are flagged at match setup and keep the flag across zone
    changes and copies; prepared copies inherit it; new `notFromStartingDeck` property (Ursine
    Guide, The Mystical Archive).
  - `ManaCost.hasHybrid`/`ManaCostShard.isHybrid` (Phyrexian excluded) and a `hasHybridManaCost`
    property (Providence of Night).
- **Android:** the bundled `assets.zip` is built by an antrun step at `initialize`; the app
  extracts it instead of downloading; versionCode is `yyyyMMddHH`; in-plugin signing turned off
  (it corrupts signatures under JDK 9+) in favor of `apksigner`.
- **Tooling:** `.gitignore` entries for the local `/dist`, `/devrun` and `/_tools` folders and the
  generated `assets.zip`.


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
