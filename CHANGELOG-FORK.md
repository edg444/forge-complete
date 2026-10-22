# Fork changelog

Everything this fork adds on top of upstream [Card-Forge/forge](https://github.com/Card-Forge/forge).
Upstream's own daily changes are **not** listed here — only work done in this fork.

- `origin` → `https://github.com/edg444/forge-complete.git` (this fork)
- `upstream` → `https://github.com/Card-Forge/forge.git` (merged in regularly; see README of the workflow below)

Reconstructed 2026-08-06 from git history (`git log --no-merges HEAD --not upstream/master`),
which remains the authoritative record if this file and the commits ever disagree.

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

---

## Log

### Unreleased

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
  - Ransack, the Lab is in `ransack_the_lab_unknown_event.txt`: its file name collides with Ransack the Lab's.
- **Unknown Event: Groaaaaag, Hungry Monster, Monet, Sensei of the Sewers, The Mystery Raceway, Crow Scarer (UNK).**
  - `K:Ninjutsu:<cost>:FixedCommander` and `K:Suspend:N:<cost>:Commander`: from the command zone they pay the
    commander tax and count toward it, like a cast (user ruling).
  - Abilities take `CommanderTaxApplies$`.
  - The Mystery Raceway's "Team X won the race" is your own race team (user ruling).
  - Crow Scarer's Cowards can't attack you or your planeswalkers and battles, and can't block your creatures (user
    ruling).
- **Unknown Event: Bag of Stroopwafels, Famous Museum, Guilded Lotus, Kibo's Bananamobile, Sawtooth Avenger, Tinfoil
  Helm, Power Level Analyzer, Twenty Lessons, That Which Was Compleated (UNK uncommons).**
  - `Play AnySupportedCard$ Type:<type>` (every card with that type or subtype) and `XIs$` (the value of X when
    the played spell has X in its mana cost).
  - Player property `hasConspiracyInCommand`.
- **Unknown Event: Ano'thr, Equipment Commander, Zimone's Homework, First Stage of Magic Design, The Dogronmaster
  (UNK rares).**
  - `Count$YourLifeIn.LazyCaterer|Fibonacci|Square`; card property `powerEqualsToughness`.
- **Unknown Event: Wheel of Not Ideal, The Cobra King, Autumn Willow and Baron Sengir, The Bird Champion, Emerin,
  Shivan Angel (UNK).**
  - New effect `PassLibraries`: each player's library, in order, goes to the next player in turn order; the cards
    keep their owners.
  - Card property `Damaged <defined>`: cards that dealt damage this turn to the defined card.
- **Unknown Event: Genevieve, Conniving Dragon, Margle, Cousin of Yargle, The Knight of Commentary, The Judge of
  Height, Tarkiran Towelsbane, Drop Bear (UNK rares).**
  - Continuous statics take `CantGainAbilities$`: from that effect on, nothing in layer 6 gives the card abilities
    back, whatever its timestamp (Pump grants included).
  - `Count$ShorterCreatures`: creatures whose toughness plus mana value is less than the source's.
  - New creature types Artist, Athlete, Chef, Judge and Koala, printed on UNK cards; Forge dropped them before.
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
- **Unknown Event: The Fifth Alias, Gomif, Fast Racer, The Mox Painter, Efteekay, Flame of the Kav, Frie, Displaced
  in Time, Pirk, Heroic Captain, The Cookout Creator, The Wizardly Barge (UNK rares).**
  - A deck with SVar `AsksSecretList:N` secretly lists N nonland cards as the game begins; card property
    `OnYourSecretList`.
  - New static `SpeedUncapped`: speed can rise past 4, which still counts as max speed.
  - `NameCard ExcludeControlled$`; `LegalIn$` now takes any format by name (Vintage for "tournament-legal").
  - `Count$DifferentNames <valid>`.
  - Warp's end-step exile is marked `WarpExile` (spell property) for "exiled because it was warped".
  - `CounterAddedOnce FullyStations$`: the counters took a Spacecraft to its highest station number.
  - New effect `NameIt`: a player gives a permanent a name of their own.
- **Unknown Event: The Asker of More Mana, The Keeper of Favorite Cards, The Water Maro, The Vegetable Car, Jiffy,
  Vehicle Repairer, The Bearded Teakeeper, Uh, Squen & Cragg Made a Card, The Herald of Numot, The Caffeinated
  Runner, The Weekly Princess, The Milling Runner (UNK rares).**
  - New static `ManaPaymentTax`: every mana payment costs more, effects and triggers included (a cost of no mana
    at all is left alone).
  - A per-game favorites list on each player: `NameCard AddToFavorites$`, card property `YourFavorite`.
  - Card property `nameHasWord_<word>`.
  - New keyword Onionfect: the damage is still dealt, but it becomes Food tokens for the source's controller.
  - New static `InvertPTCounters`, which turns +1/+1 and -1/-1 counters around.
  - New static `JumpStartNotExiled`; jump-start's exile now checks spell property `JumpstartExiled`.
  - `DayTime Value$ Neither` stops tracking day and night, and the match background goes back to normal.
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
- **Unknown Event: Guild Pact, The Five Stages of Grief, Call Up Emrakul to Help, Everyone's an Impostor, Pulse of the
  Hunter Maze, The Paradise Bird, Phyrexian Cytoshaper, Maître Tree, Grek the Ogre, Land Station.**
  - Everyone's an Impostor humors its own reminder text: granted changeling gives every creature type.
  - New Donkey and Ogre Noble tokens.
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
### 2026-10-21, twelfth deploy (desktop, Android, GitHub) — Unfinity complete; 24-commit upstream merge

- **Upstream merge (24 commits).** Five card-script conflicts, resolved by keeping upstream's fixes with the fork's
  Scryfall wording: Bell Borca, Spectral Sergeant (`CharacteristicDefining$`), Fear of Change (`Origin$ Battlefield`
  on its dies trigger), Overclocked Electromancer (`CombatDamage$ True`); Disciple of Perdition and Urza, Academy
  Headmaster kept the fork's text, upstream's change already being in it.
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
### 2026-10-20, tenth deploy (desktop, Android, GitHub) — Unsanctioned complete; 22-commit upstream merge

- **Upstream merge** (22 commits, 85888fdfa0a): 11 card-script conflicts resolved keeping the fork's Scryfall wording
  and taking upstream's fixes; Oracle lines re-synced to Scryfall 2026-10-20 (26 cards: Alchemy "perpetually gains
  prismatic" rewording, spark costs as "[+1]:"). Tests: 1454 run, 0 failed, 7 skipped.
- **Imaginary Friends (Plane) AI.** No strategy, just random: from hand it's offered the first three cards that can
  be cast right now from a random order of the choices its deck builder isn't told to leave out (`AI:RemoveDeck:All`),
  kept for the turn, as ordinary cast-as spells its usual logic weighs. For the chaos free cast, `PlayAi.chooseCardName`
  tries the choices in random order (up to 30) and takes the first whose spell it would cast.
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
- **Info panel**: "(Covered: )" and "(Written: )" no longer show on every card - unset string view properties read
  back as "", not null.

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
  cards that don't double use it: 76 doubling cards ("double that damage", "twice that many ... instead", "double
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

### 2026-10-10, third deploy (desktop, Android, GitHub) — upstream merge (11 commits); Mystery Booster playtest cards #16–30

- **Upstream merge** (11 commits, through 5222f05e569: mobile card zoom/match screen/command zone layout, simultaneous
  entry, MDD spoilers): conflicts kept both sides. Phyrexian Adapter takes upstream's `AddType$` fix with the Oracle
  wording; the mobile life labels keep the fork's hundredths and fitting on upstream's single life font; the pay-life
  prompt keeps the fork's half-life affordability check with upstream's `confirmPayment`.
- **Memory Bank** (CMB1 #27): new keyword bank. Cast from your hand, it's exiled for the rest of the match instead of
  going to the graveyard (not if countered); each later game of the match starts with it in exile, where you may
  cast it.
- **The Grand Tour** (CMB1 #30, upstream script): fixed. A nontoken permanent never finished the tour; it stayed in
  exile, because each step looked for the object from before its last move. Each step now follows the card's newest
  object. It can also target tokens now (ruling: a token stops at exile).
- **Squidnapper** (CMB1 #29): takes an opponent's creature until it leaves the battlefield or that player pays the
  ransom ({6} and 2 life, any time they have priority, without using the stack).
- **Loopy Lobster** (CMB1 #26): new keyword four-faced; each stage's stats and abilities come from its own static.
- **Learned Learner** (CMB1 #25): has "{T}: Draw a card." while your maximum hand size isn't 7; no maximum hand
  size counts (user's call). Its Cephalid type is Octopus, as real Cephalids now are.
- **Innocuous Insect** (CMB1 #23): script only. Its ruling says a bought-back permanent spell goes to your hand
  instead of the battlefield, but the current rules (CR 702.27a) only return a spell that would go to the graveyard
  as it resolves, so buyback does nothing and the Insect enters the battlefield (rules over rulings). AI: it never
  pays buyback on a permanent spell, or holds one back hoping to.
- **Form of the Mulldrifter** (CMB1 #22): creature cards in your hand can be cast as Mulldrifter, or evoked as one.
  On the stack and the battlefield they are Mulldrifters, and stay that way if the Form leaves.
- **Do-Over** (CMB1 #20): restarts the turn, back to exactly how it began. Every card goes back where it was, as
  it was (the engine knows every hidden card, so nothing is left to chance). Tokens, copies and effects made this
  turn are gone; effects and delayed triggers from before the turn still apply, and life totals and turn counts go
  back. AI: it casts Do-Over when the turn has cost it clearly more than its opponents, in life and permanents.
- **Command the Chaff** (CMB1 #18, upstream script): fixed - it could play a land from the sideboard (ruling: you
  can't); now only spells (`ValidSA$ Spell`).
- **Biting Remark** (CMB1 #17): new keyword Scrycast. AI: a permanent spell cast by an effect no longer waits for
  main phase 2 (it's now or never).
- **Animate Spell** (CMB1 #16): an Aura on a spell; the card becomes a creature on the battlefield and its owner casts it
  or sacrifices it when the Aura leaves (rulings: P/T from the mana value on the stack, X included; the Aura's enchant
  ability swaps to the permanent; no cast if the creature left first or at the same time).

### 2026-10-10, second deploy (desktop, Android, GitHub) — 2018 Heroes of the Realm (Optimus Prime, Sol); Ponies: The Galloping; Mystery Booster playtest cards #1–15

- Flaky test fixed: `SlayingMantisTest.testThrowAlwaysLandsAndAimsAtWhatItKills` threw 40 unseeded Mantises and
  needed more than 10 to touch the Dreadmaw (about 40% each), so it failed now and then (10 of 40 on 2026-10-10).
  The throws now run on a seeded `MyRandom`, restored afterward: 22 of 40 every run.

- **Stack of Paperwork** (CMB1 #13), with #11 Sarah's Wings and #12 Scaled Destruction (upstream) verified; Scaled
  Destruction's modes-in-order ruling now has a test.
- **Ruff, Underdog Champ** (CMB1 #10): "All Hounds are Dogs" is already true in Forge (no Hound type left), so it's
  text only. New count `Count$YouLostAGameThisMatch` (`GameOutcome.isLoser`: a drawn game has no loser).
- **Priority Avenger** (CMB1 #9): new count `Count$StackSize` (spells and abilities on the stack).
- **Metagamer** (CMB1 #8), with #6 Frontier Explorer and #7 Imaginary Friends (upstream) verified. User rulings: the
  latest Pro Tour (only), kept current. Today that's Pro Tour Marvel Super Heroes, whose finals were a draft, so the
  deck is Yuuki Ichikawa's 40-card draft deck (names checked against Scryfall).
- **Five Kids in a Trenchcoat** (CMB1 #5). User rulings: five creatures in anyone's count of the creatures you
  control, filtered counts and thresholds included; counts of all creatures see one.
- **Enroll in the Coalition** (CMB1 #4): you're a Flagbearer (not a creature); with Coalition Honor Guards out,
  opponents may target you or any Flagbearer creature (ruling). Honor Guard on its own still means creatures.
- **Baneslayer Aspirant** (CMB1 #3): only your own emblems count.
- **Ral's Vanguard** (Mystery Booster playtest, CMB1 #1): first of the set's 60 unscripted cards. Copies don't draw
  or deal extra (ruling); the requirement is checked as the game begins.

- **Ponies: The Galloping** (new edition PTG, silver-bordered): Nightmare Moon // Princess Luna, Princess Twilight
  Sparkle, Rarity. New creature types Alicorn and Pony. User rulings: nighttime = real local clock 6 PM to 6 AM;
  friendship is mutual; the AI owns a pony toy half the time, with colors fixed for the game; only Tagger-confirmed
  moons can be chosen for Luna. `PrintingTraits.txt` regenerated (moons, PTG, and Arena Promos/@FRA printings the
  last run predated); no existing line lost a trait.

- **Optimus Prime, Inspiring Leader** and **Sol, Advocate Eternal** (2018 Heroes of the Realm). User rulings:
  Optimus's "turn to its other face" is convert (any double-faced permanent, MDFCs included) and also turns a
  face-down permanent face up; Sol's partner can be anything that could be a commander if it were legendary,
  and it's legendary all game in every zone. Desktop and mobile deck editors accept the nonlegendary partner.

### 2026-10-10 (deployed: desktop, Android, GitHub) — Two-Headed Giant seating and team scoring; ∞ on mobile; AI twin-trigger targets; Arena Promos

- **Two-Headed Giant layout**: teammates now sit together. On mobile, seats 1+2 vs 3+4 used to stack as teammate,
  opponent, opponent, you; on desktop the teammate's field went in the top cell with an opponent. Now your team is the
  bottom half and the other team the top half on both (desktop still lets you move fields).
- **Team wins score for every winner**: the match tally ("Evan: 1 Eva: 0 ...") credited only the first winner it
  found. Every winner now counts, in the summary, match-over check, Ghazban Ogress's and Gus's counts, and the AI's
  sideboarding (which no longer dereferences a missing winner after a drawn game). The win screen's "Team 0 won!"
  now uses the lobby's numbering (Team 1).
- **∞ on mobile**: the life, counter and mana ∞ rendered as nothing because the generated fonts didn't include it.
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

### 2026-09-25 — Unhinged colorless; more Un-cards; pink and gold; errata reversal; Oracle retemplate

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
- `Chance.N` reworked: rolled once per turn, one attempt per turn, honored by every API.
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
