# Piazza context — CS 440 (Cursor / assignment reference)

This file collects questions, answers, follow-ups, and notes from the course Piazza so they can be used as `@` context in Cursor. **Instructor guidance changed over the semester** as bugs were fixed; when adding threads, include the **date** (from Piazza) so newer answers can override older ones when they conflict.

**Course staff (for interpreting answers):** **Andrew Wood** — professor. **Collin Barber**, **Selene Wu**, and **Aakash Kumar** — TAs / course staff (Piazza may show “instructor” endorsements for some staff posts). Their posts are strong ground truth for assignment intent; student posts can still be useful but may be incomplete or outdated.

**SCC (in threads below):** **Shared Computing Cluster** — the course’s remote Linux cluster for running long training jobs (you’ll get accounts, paths, and workflow detail from course/lab docs). When staff say “upload to the SCC,” they mean your **home/project space on that cluster**, not your laptop’s disk.

**`cs440` paths in logs:** **CS 440** is the course number; many students keep all course work in a folder literally named **`cs440`** on the cluster (e.g. `[user@scc1 cs440]$`). That directory name is **not** special to Java—it is just how you organized uploads.

**Your deadline / SCC workflow (Sun Cho):** Prefer **short, queue-friendly `qsub` requests**, **checkpoint/resume** (`SequentialTrain` flags in **Thread 020 — @422**), **`qstat`**, and the checklist in **Personal workflow — Sun Cho** at the end of this file.

## Conventions

- Each thread is separated by a horizontal rule and a level-2 heading: `---` then `## Thread …`.
- **Post number** (e.g. `@246`) and **title** are recorded at the top of each block.
- **Tags / folders** are listed when visible.
- **Authors** are noted for each item. Piazza anonymous posts are recorded as **Student**.
- **Thread order:** oldest → newest (by post date in Piazza).
- **“N Followup Discussions” in Piazza:** treat this as a count of follow-up entries/replies in the thread (questions/notes/answers), not necessarily distinct separate subthreads.
- **Piazza `@###` in post bodies** (e.g. `@388`) refers to **another discussion thread** by that post number on Piazza, not a GitHub handle. In this file, match `@NNN` to **`## Thread … — @NNN`** when present.
- **Code or pseudocode** from screenshots is transcribed into fenced code blocks; a short note explains what it refers to if helpful.

---

## Thread 001 — @246 — Agent Issues

**Piazza tags (visible):** `pas`, `pas/pa3`, `pas/pa2`  
**Approx. timeline (from UI):** updated ~1 month ago from capture; original week shown in sidebar **3/15–3/21** (post dated **3/21/26** in list).

### Question (Student)

How do I get an agent instance for an opponent?

### Student answer / note (Student)

Using `game.getAgent(int logicalPlayerIdx)` appears to break (student-reported behavior).

### Answer (Andrew Wood — professor)

You cannot obtain an opponent’s agent instance. You do not get access to opponents.

### Follow-up discussion

1. **Student (unresolved follow-up):** How should we account for opponents in simulations?

2. **Andrew Wood (professor):** During rollouts you need to invent moves for **all** agents in the game. The intended approach was to use **“proxy”** agents, though there is more than one acceptable implementation.

3. **Student:** Acknowledgment — thanks.

### Code references (from thread)

```java
// Student attempted API (reported problematic / not valid for opponents per instructor)
game.getAgent(int logicalPlayerIdx);
```

**Takeaway:** Do not rely on accessing real opponent agent objects; model other players (e.g. with proxy agents or equivalent) when simulating rollouts.

---

## Thread 002 — @346 — Registry Error - Risk

**Piazza tags (visible):** `pas`, `pas/pa3`  
**Status:** Resolved  
**Sidebar date:** 4/4/26  
**Timeline (from UI):** updated ~3 weeks ago at capture

### Question (Alexander Smeulders)

When using `getTerritoryOwners`, Java reports generics / type issues:

1. **Error A:** “The type Registry is not generic; it cannot be parameterized with arguments `<TerritoryOwnerView>`.”
2. **Error B (after removing type parameters):** “Type mismatch: cannot convert from `Registry<TerritoryOwnerView>` to `Registry`.”

The student tried type casting; that led back to the first style of error inside the cast.

### Answer (Selene Wu — TA)

Make sure you imported the **correct** `Registry` type — a similarly named `Registry` can exist in other packages, which can produce exactly this kind of confusion.

**Endorsement:** Collin Barber (TA) endorsed this answer.

### Follow-up (Alexander Smeulders)

Resolved: “Yeah for some reason it auto imported another one even though I already had all the util stuff. Thanks!”

**Endorsement:** Andrew Wood (professor) endorsed this follow-up.

### Code / types (from thread)

```text
// API discussed (exact signature not fully visible in capture)
getTerritoryOwners
// Types involved
Registry<TerritoryOwnerView>
Registry   // raw / wrong import scenario
```

**Takeaway:** Wrong `Registry` import (non-generic or from another package) vs the assignment’s intended `Registry` causes the compiler errors; fix imports rather than forcing casts.

---

## Thread 003 — @347 — Board Setup Question

**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/4/26 (same calendar day as @346; ordered after @346 by post number)  
**Timeline (from UI):** updated ~3 weeks ago at capture  
**Endorsement:** Andrew Wood (professor) endorsed the question (per Piazza UI in capture).

### Question (Alexander Smeulders)

1. **Default map / test data:** Is there a rundown of the values used to build the official board setup (e.g. continent `id`, reinforcement **value** / “armies per turn”, etc.)? They want to build a test suite without breaking invariants and asked whether staff could provide anything helpful—even just a list of territories and continents with defaults (not necessarily a new API).

2. **Circular construction:** Territories require a set of adjacent territories in the constructor, but not all territories exist yet—feels circular. Similarly, territory vs continent: how to construct a territory without its continent, or a continent without its territories?

### Answer (Collin Barber — TA)

Intro (paraphrased): Staff considered distributing more maps; shared helper code instead.

**`loadBoard` — load a full board from the JAR’s map JSON**

```java
/**
 * Loads a board from the new map directory structure.
 *
 * @param mapName e.g. "world"
 */
public static Board loadBoard(String mapName) {
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(Board.class, new BoardDeserializer())
            .create();

    String path = "/maps/" + mapName + "/map.json";
    try (InputStream stream = Main.class.getResourceAsStream(path)) {
        if (stream == null) throw new IllegalArgumentException("Map not found: " + path);
        InputStreamReader reader = new InputStreamReader(stream);
        return gson.fromJson(reader, Board.class);
    } catch (Exception e) {
        throw new RuntimeException("Failed to load board from " + path, e);
    }
}
```

**Where the files live:** Unzip the Risk assignment materials / JAR the course provides. Example layout from capture: under something like `risk/lib/` there is **`pas-risk-jar-1.0.0.jar`**; inside it, **`/maps/world/map.json`** (and e.g. `overlay.png`). That JSON is the source of truth for continent ids, values, territory ids, adjacency lists, etc.

**Sample `map.json` fragment (continents — from capture)**

```json
{
  "continents": {
    "Asia": {
      "name": "Asia",
      "value": 7,
      "id": 0
    },
    "North America": {
      "name": "North America",
      "value": 5,
      "id": 1
    },
    "South America": {
      "name": "South America",
      "value": 2,
      "id": 2
    },
    "Africa": {
      "name": "Africa",
      "value": 3,
      "id": 3
    }
  }
}
```

**Staff note:** The same loading approach can load **arbitrary** maps (e.g. smaller test maps). They *might* release tooling to author custom maps later, depending on time.

### Follow-up (Collin Barber — TA) — resolving the circularity

Collin quoted the student’s circularity paragraph, then: “so that's the neat part... here is how we did it:”

Below is the course’s **`BoardDeserializer`** pattern (transcribed from multiple screenshots). **Opening of `deserialize`:** the first lines were partially cut off in captures; the loop structure matches the visible body—confirm against `pas-risk-jar-1.0.0.jar` if a line differs.

```java
package edu.bu.pas.risk.territory;

import com.google.gson.*;
import edu.bu.pas.risk.util.Registry;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Used internally to parse a {@link Board} from a json file.
 * This can be used to load custom maps.
 */
public class BoardDeserializer implements JsonDeserializer<Board> {

    @Override
    public Board deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();
        JsonObject continentsJson = root.get("continents").getAsJsonObject();

        List<Continent> continents = new ArrayList<>();
        Map<Continent, Set<Territory>> continentsTerritoriesCache = new HashMap<>();

        for (String continentKey : continentsJson.keySet()) {
            JsonObject continentJson = continentsJson.get(continentKey).getAsJsonObject();
            String name = continentJson.get("name").getAsString();
            int value = continentJson.get("value").getAsInt();
            int id = continentJson.get("id").getAsInt();

            // Staff: wrap the mutable territory set in an unmodifiable view so accidental student mutation fails fast
            // (not described as a security measure in the thread).
            Set<Territory> mutableTerritories = new LinkedHashSet<>();
            Continent continent = new Continent(id, name, value, Collections.unmodifiableSet(mutableTerritories));
            continents.add(continent);
            continentsTerritoriesCache.put(continent, mutableTerritories);
        }

        JsonObject territoriesJson = root.get("territories").getAsJsonObject();
        List<Territory> territories = new ArrayList<>();
        Map<Territory, List<String>> adjacencyCache = new HashMap<>();

        for (String territoryName : territoriesJson.keySet()) {
            JsonObject territoryJson = territoriesJson.get(territoryName).getAsJsonObject();
            String name = territoryJson.get("name").getAsString();
            String continentName = territoryJson.get("continent").getAsString();
            List<String> neighbors = territoryJson.get("neighbours").getAsJsonArray().asList().stream()
                    .map(JsonElement::getAsString)
                    .toList();
            int id = territoryJson.get("id").getAsInt();

            Continent continent = continents.stream()
                    .filter(temp -> temp.name().equals(continentName))
                    .findAny()
                    .orElseThrow();

            Territory territory = new Territory(id, name, continent, null);
            territories.add(territory);

            // No full territory graph yet; map isn't a DAG — defer wiring neighbors ("postprocessing").
            adjacencyCache.put(territory, neighbors);
        }

        List<Territory> territoriesFullGraph = new ArrayList<>();
        for (Territory territory : territories) {
            Set<Territory> neighbors = adjacencyCache.get(territory).stream()
                    .map(n -> territories.stream()
                            .filter(t -> t.name().equals(n))
                            .findAny()
                            .orElseThrow())
                    .collect(Collectors.toUnmodifiableSet());

            Territory territoryFullGraph = new Territory(
                    territory.id(), territory.name(), territory.continent(), neighbors);
            territoriesFullGraph.add(territoryFullGraph);
        }

        // After territories exist with adjacency, fill each continent's mutable territory set for graph queries.
        for (Map.Entry<Continent, Set<Territory>> entry : continentsTerritoriesCache.entrySet()) {
            Continent continent = entry.getKey();
            Set<Territory> mutableTerritories = entry.getValue();

            territoriesFullGraph.stream()
                    .filter(t -> t.continent().equals(continent))
                    .forEachOrdered(mutableTerritories::add);
        }

        return new Board(new Registry<>(continents), new Registry<>(territoriesFullGraph));
    }
}
```

**Takeaways:** Use `loadBoard` + bundled `map.json` for canonical numbers and graph shape. For hand-built tests, mimic the two-phase pattern: create territories with `null` neighbors (or equivalent), record neighbor **names**, then replace with real `Territory` references once all nodes exist; then attach territories back onto continents.

---

## Thread 004 — @365 — Rules for Risk

**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/9/26  
**Status:** Resolved (per capture)  
**Question endorsement (Piazza):** Aakash Kumar (staff)

### Question (Student)

After watching a video about classic Risk, the student expected **no unowned territories** after initial setup. They noticed **`GameView.getUnownedTerritories()`** and asked whether it is **only** for early setup and effectively **empty** for the rest of the game. They also asked whether the course Risk matches the rules in this video: `https://www.youtube.com/watch?v=Xo8RSozX6Ac`

### Answer (Aakash Kumar — TA / course staff)

The assignment uses **modified** rules to make **Q-learning** more feasible. Main differences called out:

1. **Fortify:** only between **adjacent** territories (not full classic fortify rules).
2. **Territory cards:** redemption is based on **how many cards** you hold, **not** on meeting the classic “specific combinations” style conditions.

*(Paraphrased closing from capture: those are the main differences.)*

**Endorsement:** Andrew Wood (professor) endorsed this answer.

### Follow-up (Student)

“So basically, it's all infantry?”

### Reply (Collin Barber — TA)

In the physical board game, troop types are separated; **in this codebase they are combined**. You can think in terms of converting back and forth because it is effectively a **linear combination** (single troop representation).

**Nested clarification (same thread, capture):** Regarding **`getUnownedTerritories()`** again — **yes:** it is mostly empty for most of the game but **can still be useful near the start** of the game.

**Endorsement:** Aakash Kumar endorsed Collin’s reply (per Piazza UI).

### API reference (from thread)

```java
// Discussed on GameView
getUnownedTerritories()
```

**Takeaways:** Do not assume full classic Risk or the YouTube video line-for-line; use staff-stated deltas (adjacent fortify, simplified card redemption, combined “infantry” model). Treat `getUnownedTerritories()` as a **setup / early-game** helper, not a midgame invariant.

---

## Thread 005 — @374 — Meaning of higher bound and lower bound in reward function

**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/11/26  
**Timeline (from UI):** updated ~2 weeks ago at capture

### Question (LI JIAJUN)

In the reward function files there are two default methods that return a **lower bound** and an **upper bound**. The defaults imply a range **0.0–100.0**.

The student asked:

- Are **negative** rewards allowed? If yes, what does the **lower bound** mean? If no, how should a **“shock”** (strong negative signal) be represented?
- (Closing) Hope someone will answer.

### Answer (Andrew Wood — professor)

The reward function **must be bounded**. The student **chooses the bounds**. Every value returned by the reward function **must stay within** that range (never exceed it).

The `[0, 100]` defaults are **examples only**; bounds may be set to **any finite values** the student wants.

### Code reference (from thread — defaults shown as examples)

```java
public double getLowerBound() { return 0.0; }
public double getUpperBound() { return 100.0; }
```

**Takeaway:** Pick a finite `[lower, upper]` that actually contains all rewards you will emit (including negative values if you set a negative lower bound). The bounds are a contract for the learner, not fixed to `[0, 100]`.

---

## Thread 006 — @375 — Training Game Lengths

**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/11/26 (same day as @374; ordered after @374 by post number)  
**Timeline (from UI):** updated ~2 weeks ago at capture

### Question (Student)

Games take **very long** in Risk when training. Against **static** agents, games were quick; adding even **one random** agent makes runs extremely slow—worried there will not be enough training time even after days. Is this a bug on the student’s side, or expected?

### Answer (Collin Barber — TA)

Several hurdles are possible, but in short:

- The **Random** agent is **good enough to beat static** agents, but against other setups it **almost certainly leads to very long or “infinite-feeling” games**.
- If the student’s agent faces a **random** opponent, they need a way to **encourage actions that end the game sooner** (e.g. shaping rewards, heuristics, or training curriculum).
- Collin suggests the human designer might **bias exploration** so that “**good random**” moves are selected more often (rather than uniform chaos).

**Endorsement:** Andrew Wood (professor) endorsed this answer.

### Follow-up (Student) — resolved

Updating to **Risk 1.0.2** fixed very long games **without other code changes** (student observation).

### Reply (Collin Barber — TA)

There was a **bug in how armies / continent ownership were reflected** in what the student received: they were **rewarded too much for “having” territories / continents** in a way that did not match true full continent control.

**`getContinentsOwnedBy` — before (buggy):** a continent counted if the agent owned **any** territory in it (`anyMatch`).

**After (fixed in 1.0.2):** a continent counts only if the agent owns **all** territories in it (`allMatch`).

```java
public List<Continent> getContinentsOwnedBy(int agentIdx) {
    return board.continents().stream()
            .filter(continent -> continent.territories().stream()
                    .map(territoryOwners::get)
                    .anyMatch(owner -> owner.getOwner() == agentIdx))
            .toList();
}
```

```java
public List<Continent> getContinentsOwnedBy(int agentIdx) {
    return board.continents().stream()
            .filter(continent -> continent.territories().stream()
                    .map(territoryOwners::get)
                    .allMatch(owner -> owner.getOwner() == agentIdx))
            .toList();
}
```

**Staff note (from thread):** With the buggy version, the agent did not have to work as hard to get a strong reward—training dynamics and game length could look wrong until the JAR fix.

**Related:** Sidebar references a **“New version of Risk Out (4/13/26)”** post about **1.0.2** and incorrect bonus armies when only a **single territory** in a continent was controlled—same family of “continent ownership” correctness issues.

**Takeaways:** Long games vs Random can be **environment + opponent dynamics**, not only your bug—but **upgrade to Risk 1.0.2** if continent/army rewards looked “too easy.” Prefer **`allMatch`** semantics for “owns continent” when mirroring staff logic.

---

## Thread 007 — @376 — Reward Function Advice

**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/12/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 0

### Question (Anthony Hardman)

Based on examples in lecture slides, the **absolute scale** of reward values seems important for state/action transitions. The student asked:

- Is there general advice for designing reward functions or tuning them based on model performance?
- Is a small range like **-1 to +1** too small to learn from?
- Is a larger range like **-10 to +10** too large / unstable?
- They also referenced a claim that poor policy vs optimal policy differed by only decimal places and asked for guidance.

### Answer (Collin Barber — TA)

Key points from Collin’s response:

1. You are designing your reward function; you can change it to match your needs and learning dynamics.
2. Start with a **canonical/simple baseline** first (e.g. a reward setup that should let the network learn an obvious policy), then iterate.
3. On scale questions (`-1..+1` vs `-10..+10`), “too small / too large” depends on your setup and what you mean by “consistent,” rather than a single universal threshold.
4. Important implementation constraint in the course libs: if your model uses **`float64`**, outputs often flow through a final nonlinearity; with **`tanh`** as final activation, output is bounded to **`[-1, +1]`**, so producing targets like `-10` can be difficult/impossible without changing architecture/training setup.

**Endorsement:** Andrew Wood (professor) endorsed this answer.

### Code / model note (from thread)

```text
If final activation is tanh, output range is [-1, +1].
Large-magnitude reward targets may be hard/impossible to realize directly.
```

**Takeaway:** Start simple, verify learning, then scale rewards deliberately. Keep reward magnitude aligned with model output range/activation choices (especially if the head is effectively bounded like `tanh`).

---

## Thread 008 — @380 — New Version of Risk Out

**Post type:** Note  
**Piazza tags (visible):** `pas`, `pas/pa3`, `logistics`  
**Sidebar date:** 4/13/26  
**Timeline (from UI):** updated ~2 weeks ago at capture

### Note (Andrew Wood — professor)

New version announcement: **Risk 1.0.2** is out.

Bug fix called out:

- Earlier versions could incorrectly award **continent bonus armies** when an agent controlled only a **single territory** in that continent.
- Correct behavior: you must control **all territories** in a continent to receive that continent bonus.

### Follow-up kept (Aakash Kumar — TA / course staff)

Short confirmation-style comment: “There are no bugs in Colin’s risk.”

### Follow-up omitted by request

Per your instruction, the second follow-up entry (an accidental random PNG/meme image) is intentionally **not** included in this context file.

**Takeaway:** If your experiments relied on pre-1.0.2 behavior, retrain/re-evaluate after upgrading because reward/bonus signals around continent control changed materially.

---

## Thread 009 — @384 — Can't find getExplorationReplacement

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/13/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 0

### Question (Student)

The student could not find `getExplorationReplacement` referenced in assignment docs/slides.

They attached a screenshot snippet that includes lines like:

- `getExplorationPlacement` (territory placement when ignoring the model)
- `getExploration*Action*` methods (action selection when ignoring model)
- `shouldExplorePlacementPhase` (whether to place via `getExplorationPlacement`)
- `shouldExplore*MovePhase` (whether to use exploration action methods)

After the image, the student asked (with your correction for the cut-off text):

“**is this supposed to be getExplorationPlacement instead of getExplorationReplacement.**”

### Notes

- No staff/student answer is visible in the provided capture.
- Interpreting the naming in the pasted excerpt, this appears likely to be a **documentation typo** (`Replacement` vs `Placement`), but keep this as an open question until confirmed by a staff reply or updated docs.

### API names mentioned in thread

```text
getExplorationReplacement   // questioned / likely typo in docs
getExplorationPlacement
getExploration*Action*
shouldExplorePlacementPhase
shouldExplore*MovePhase
```

**Takeaway:** Treat this thread as a nomenclature mismatch report. Prefer actual method names present in the provided code/API (`...Placement`) until a staff correction says otherwise.

---

## Thread 010 — @388 — Framework Bugs in Risk

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/14/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 0

### Question (Student)

Student reports two framework-related issues while using **Risk 1.0.2**:

1. **Random agent elimination can hang forever**
   - When an agent receives 5+ cards and is forced to redeem, it sometimes returns `NoAction`, gets eliminated, and replaced with a `StaticAgent`.
   - This allegedly causes repeated spam of:
     - `[ERROR] FSMAgent.onTurnDone: tryout ended but I am not the FORTIFY_SKIP state?`
     - and related game-loop hangs / possible neural-agent detach stack traces (`forceOrderIn=true` / `canRedeemCards=false` references mentioned in screenshot text).

2. **Eval mode appears to ignore redeem-phase exploration hook**
   - During eval (`isTraining=false`), the framework seems to bypass `shouldExploreRedeemMovePhase` and call argmax directly.
   - Student says `shouldExploreRedeemMovePhase` returns `true` and `getExplorationRedeemAction` properly sets `includeNoAction=false`, but this path appears bypassed in eval, leading to invalid forced-redeem behavior.

### Answer (Andrew Wood — professor)

Andrew’s response has two points:

1. “let me look at this”
2. During **evaluation**, intended behavior is **no exploration** (eval is to evaluate the learned function, not to collect training transitions).  
   However, when an agent is **forced to redeem cards**, `NoAction` should **not** be in the available options. He asks the student to verify whether their agent is actually choosing `NoAction` in forced-redeem situations.

### API / state references mentioned

```text
shouldExploreRedeemMovePhase
getExplorationRedeemAction(includeNoAction = false)
isTraining = false (eval)
NoAction
StaticAgent
FSMAgent.onTurnDone ... FORTIFY_SKIP
```

**Takeaway:** Eval-mode no-exploration is expected, but forced-redeem should still forbid `NoAction`. If hangs occur, focus on verifying forced-redeem action filtering and whether invalid `NoAction` is entering the action set.

---

## Thread 011 — @390 — New Version of Risk Out

**Post type:** Note  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/14/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 0

### Note (Andrew Wood — professor)

Full text (from capture):

> Hey everone,
>
> New version is **1.0.3**. Thanks to **@388** for helping us identify and quash a bug in how redeeming cards are handled when you are forced to redeem after knocking out an opponent with a large enough hand as well as whether `NoAction` should be considered by your agent during the redeem phase of you turn.
>
> -Andrew

**Cross-reference:** `@388` points at Piazza discussion **#388** — transcribed here as **Thread 010 — @388 — Framework Bugs in Risk**.

---

## Thread 012 — @391 — New Version of Risk Out (1.1.0)

**Post type:** Note  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/14/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 0

### Note (Andrew Wood — professor)

**Risk 1.1.0** is out.

New executable: **`edu.bu.pas.risk.MultiGameEval`**

- Runs **multiple evaluation games headlessly** (no GUI).
- Prints the **winner** of each game.

*(Capture UI suggests this note may have been edited across versions; treat the course’s latest JAR/README as authoritative if wording differs.)*

---

## Thread 013 — @395 — [ERROR] FSMAgent.onTurnEnd: my turn just ended but I am not in the FORTIFY_SKIP state?

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/14/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 0

### Question (Student)

Somewhere in the provided framework, **`onTurnEnd` keeps being invoked on agents after they have been eliminated**, which spams the error:

`[ERROR] FSMAgent.onTurnEnd: my turn just ended but I am not in the FORTIFY_SKIP state?`

The student asks whether it is acceptable to **override** that listener so it **does not call `super`**, to stop the spam.

*(Capture metadata mentions Selene Wu on the question header; voice is first-person student. Recorded as **Student** unless you confirm otherwise.)*

### Answer (Andrew Wood — professor)

Yes—**go ahead**. Andrew checked that **`FSM.onTurnEnd`** and **`NeuralQAgent.onTurnEnd`** are **not doing anything important** in the provided code path, so skipping `super` for eliminated agents is acceptable for this workaround.

### Symbols / methods mentioned

```text
FSMAgent.onTurnEnd
FORTIFY_SKIP
NeuralQAgent.onTurnEnd
FSM.onTurnEnd
```

**Takeaway:** Known framework quirk: eliminated agents may still receive `onTurnEnd`; overriding without `super` is explicitly allowed here.

---

## Thread 014 — @397 — Infinite loop in eval games

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/15/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 2

### Question (Student)

During **eval** (`isTraining == false`), the student sees an apparent **infinite loop**. In the **attack** phase, **`argmax`** runs on the action list; their Q-function scores **`NoAction`** higher than **`AttackAction`**. When **`NoAction`** is chosen, it is **terminal**, but **`actionCounter`** appears to **reset to 0** instead of advancing/ending the turn, so the same situation repeats.

**Sample log (from thread):**

```text
[EVAL] game=2 ac=0 action=AttackAction terminal=false result=-5.07
[EVAL] game=2 ac=0 action=NoAction terminal=true result=-2.12
[EVAL] game=2 ac=0 action=AttackAction terminal=false result=-5.07
[EVAL] game=2 ac=0 action=AttackAction terminal=false result=-5.07
```

They ask whether **`actionCounter` resetting** when `NoAction` is terminal is expected, a known issue, or a bug in turn progression.

### Answer (Selene Wu — TA) *(Piazza “Students’ answer” slot)*

If you play against **static** agents that only **end their turn without attacking**, and **your agent does the same**, the game may **never end** because **nobody attacks**.

### Answer (Andrew Wood — professor)

Ask whether they can detect that the game is **still progressing** by some signal **other than** `actionCounter`.

### Follow-up (Student)

Clarifies the issue happens **against a random agent**, not only static passivity.

### Follow-up (Selene Wu — TA)

Debugging suggestion: **print** (each turn end) **army counts and territory counts per player** to verify whether the match is actually making progress.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s follow-up.

### Symbols mentioned

```text
isTraining == false
argmax
NoAction
AttackAction
actionCounter
```

**Takeaway:** “Infinite” eval loops can be **stalemate dynamics** (no attacks) or **misleading progress signals**; verify real game progress (armies/territories) and opponent type (random vs static), not only `actionCounter` patterns.

---

## Thread 015 — @400 — Uploading files - SCC

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/15/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Context:** SCC = Shared Computing Cluster (see note under course staff above).

### Question (Student)

How should students **upload project files to the SCC** to start **training from the terminal**? (Recommended workflow.)

### Answer (Andrew Wood — professor)

Use the **file explorer** and the **upload button** (cluster web UI / OnDemand-style file manager—exact name depends on BU’s current SCC portal).

### Follow-up (Student) — resolved

“Do we upload our **entire CS 440 folder** into our student directory?”

### Reply (Andrew Wood — professor)

“You can if you want.”

**Takeaway:** Upload via the cluster’s file UI is fine; uploading the whole course/project tree is optional, not forbidden.

---

## Thread 016 — @405 — I uploaded my entire cs440 … / HeadlessException on SCC

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/15/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Environment (from capture):** login node **`scc1`**, working directory **`cs440`** (see **`cs440` paths** note above).

### Question (Student)

After uploading the whole **CS 440** project tree to the SCC, they compile and run:

```bash
javac -cp "./lib/*:." @risk.srcs
java -cp "./lib/*:." edu.bu.pas.risk.SingleGameEval
```

Agents print (`StaticAgent`, `StaticAgent`, `RandomAgent`), then the JVM throws **`java.awt.HeadlessException`**: no X11 `DISPLAY`, while the program tries to open AWT/Swing (`JFrame` / `GraphicsEnvironment`), with stack frames pointing at **`SingleGameEval` around line 152** (per capture).

They ask how to fix this on the cluster.

### Answer (Andrew Wood — professor)

The **SCC is headless**: you **cannot open GUI windows** there **unless** you obtain a **desktop / graphical session** (interactive visualization node—course-specific; not the default SSH shell).

### Related course artifact (cross-thread)

Staff later announced **`edu.bu.pas.risk.MultiGameEval`** for **headless** multi-game evaluation (**Thread 012 — @391**). Prefer **headless entry points** for batch training/eval on SCC instead of anything that constructs Swing/`JFrame`.

**Takeaway:** `SingleGameEval` as run in the capture expects a display → fails on default SCC SSH. Use a **no-GUI** eval driver (or an approved graphical node), not `DISPLAY` hacks on the login node unless policy allows.

---

## Thread 017 — @407 — Cannot invoke `Sound.getAudioStream()` because `sound` is null

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/15/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 8 (only key branches transcribed below; Piazza counts all replies/notes in the thread.)

### Question (Student)

On **SCC** (`scc-ge1`, `cs440/`), after `javac` / `java …SingleGameEval`, agents print then a thread crashes with:

`java.lang.NullPointerException: Cannot invoke "edu.bu.pas.risk.renderer.Sound.getAudioStream()" because "sound" is null`

Stack highlights (from capture): `RiskAnimationListener.queueSound` → `onSetupStart` → `Game.setup` → `SingleGameEval` (lambda ~line 166).

Student guesses **audio / SCC settings** need changing.

### Answer (Andrew Wood — professor)

Do **not** run games **with rendering** on the SCC unless it is a **desktop instance**; e.g. use **`--headless`**.

### Follow-up (Student) — resolved

Running **`--headless`** avoids the problem, but then you do not get to **see** the actions visually.

**Endorsement:** Andrew Wood (professor) endorsed this follow-up.

### Reply (Andrew Wood — professor)

If you want **rendering and sound on the SCC**, you **must** request a **desktop instance** (interactive graphical node).

### Follow-up (Selene Wu — TA) — unresolved (per capture)

Same issue on **laptop** but not **desktop PC**; Collin reportedly unsure why; if it also reproduces on SCC maybe fixable on framework side.

**Endorsement:** Collin Barber (TA) endorsed Selene’s comment.

### Follow-up (LI JIAJUN)

Opposite pattern for them: reproduces on **SCC desktop object** but **not** on laptop; audio works on laptop with game rendering.

### Follow-up (Student — “Anonymous Scale”) — unresolved branch

- Same class of issue on **laptop** (different symptom): sound does not play — `Failed to play sound due to 'No line available to play sound'` (not the `null` `getAudioStream` path).
- **Andrew Wood:** turn **sound off** for now; asks for **OS / hardware**.
- **Student:** Windows, **NVIDIA** discrete + **Radeon** integrated graphics, **Ryzen** CPU.

### Symbols / classes

```text
edu.bu.pas.risk.renderer.Sound.getAudioStream()
RiskAnimationListener.queueSound / onSetupStart
SingleGameEval
--headless
```

**Takeaway:** SCC default shells are **headless**; GUI+audio paths need **`--headless`** or a **desktop session**. Local “no line available” / laptop-vs-desktop quirks are **environment/driver** territory—disable sound or adjust hardware/OS context; staff were still triaging.

---

## Thread 018 — @411 — Some strange issue from running Sequential Train on laptop

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/15/26 (nearby captures also show 4/16/26 posts in same folder)  
**Timeline (from UI):** updated ~2 weeks ago at capture

### Question (LI JIAJUN)

`SequentialTrain` on a **laptop**: a **single** game can run cleanly, but longer runs eventually produce errors such as:

```text
[ERROR] FSMAgent.onTurnEnd: my turn just ended but I am not in the FORTIFY_SKIP state? Agent 2 was required to trade-in, but instead tried to: NoAction[agentId=2] hasAttacked=false forceTradeIn=false actionCounter=0 inventory.size()=5 => canRedeemCards=true, thus has been eliminated and replaced with a StaticAgent

[ERROR] FSMAgent.onTurnEnd: my turn just ended but I am not in the FORTIFY_SKIP state?
```

### Answer (Andrew Wood — professor)

Asks which **Risk JAR version** they are using.

### Follow-up (LI JIAJUN)

**`pas-risk-jar-1.1.0`** (student believes newest at time of post).

### Reply (Andrew Wood — professor)

It looks like the agent **skipped the redeem-cards phase** when that is **not allowed** if the turn **starts with 5+ cards** in hand.

### Follow-up (LI JIAJUN)

Confirms understanding: with **5+ cards**, the agent is **forced** to redeem (cannot “skip the turn” with `NoAction`). References `getAttackRedeemActions(game, actionCounter, canRedeemCards)` and suspected `inventory.size() >= 6` vs **5** threshold confusion.

### Reply (Andrew Wood — professor)

**`getAttackRedeemActions` is for the attack phase.** Card-redemption option generation belongs in **`getRedeemActions`** (and related redeem-phase hooks), not by misusing the attack-phase API.

### Follow-up (LI JIAJUN)

Restates rule: **5+ cards ⇒ must redeem; cannot skip redeem with `NoAction`.**

### Reply (Andrew Wood — professor)

**“Yep!”**

### Staff summary: turn structure (Andrew Wood — professor)

Paraphrased rules from the thread (for this course’s Risk):

1. **Redeem phase**
   - Player may redeem **as many legal card triples** as they want.
   - If the player **begins the redeem phase with ≥ 5 cards**, they **must** perform **at least one** redemption (cannot skip out of the phase).

2. **Attack phase**
   - Follows redeem; player may keep attacking while legal attacks exist.
   - **Elimination edge case:** if knocking out an opponent leaves the player with **≥ 6 cards**, they **must redeem** until they hold **≤ 4 cards** before continuing.

3. **Fortify phase**
   - Follows attack; **exactly one** legal fortify **or** skip, then the turn ends.

*(Thread note: reinforcements/placements may occur **before** the formal turn phases; staff said that part is outside this breakdown.)*

### Related follow-up (Kenneth Wilber) — resolved

**Question:** Is this the correct way to build the action list for exploration during redeem?

```java
final List<Action> options = this.getRedeemActions(
    game,
    actionCounter,
    canRedeemCards,
    game.getAgentInventory(this.agentId()).size() < 5);
```

**Answer (Andrew Wood — professor):** **“yes.”**

### Symbols / methods

```text
SequentialTrain
FSMAgent.onTurnEnd / FORTIFY_SKIP
NoAction (illegal when forced redeem)
getRedeemActions
getAttackRedeemActions   // attack phase only
getExplorationRedeemAction (discussion context)
pas-risk-jar-1.1.0
```

**Takeaway:** **Mandatory redeem** when starting redeem with **≥ 5 cards** (and post-elimination **≥ 6 ⇒ redeem down to ≤ 4**). Implement redeem exploration using **`getRedeemActions`**, not **`getAttackRedeemActions`**. Kenneth’s `includeNoAction`-style boolean via `inventory.size() < 5` pattern was **explicitly approved** by Andrew.

---

## Thread 019 — @418 — getFortifyActions is overly restrictive

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/15/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 3

### Question (Selene Wu — TA)

After decompiling, **`getFortifyActions`** appears to only consider fortifications onto the **frontier** of owned territory. Classic Risk allows more general fortify paths; this restriction can make learning harder if armies get stuck in “bad” interior stacks with no legal fortify to improve position.

### Answer (Andrew Wood — professor)

This was an intentional **simplifying assumption**: the staff expected students would primarily want to fortify **to the frontier**.

### Follow-up (Selene Wu — TA) — unresolved

Further observation: in exploration, the function **seems to always return only `NoAction`**, even when logging what it returns.

### Reply (Andrew Wood — professor)

Do you have any territories that **can donate armies**? Fortify is only possible when some territory has **enough armies** to legally donate to another territory (per assignment rules).

### Follow-up (Selene Wu — TA)

Has not fully traced it, but would still expect a non-`NoAction` fortify **at least occasionally** across **hundreds** of games.

### Symbols

```text
getFortifyActions
NoAction
```

**Takeaway:** Frontier-only fortify is **by design** (not full Risk). Persistent `NoAction` may be **legitimate** if no donating territory exists; verify game states against the “can donate armies” precondition before assuming a bug.

---

## Thread 020 — @422 — How long does it take for SequentialTrain to run when you first start training

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/16/26  
**Timeline (from UI):** updated ~2 weeks ago at capture

### Question (Student)

Wants a rough **runtime estimate** for **`SequentialTrain`** on the **SCC** so they can request an appropriate **wall time** in the batch scheduler.

### Students’ answer (Student)

Practical SCC guidance (paraphrased):

- Submit long training with **`qsub`** (batch job), not an interactive shell that might die when you disconnect.
- **`qsub` time limits:** default on the order of **12 hours**, with capability for **much longer** jobs (capture mentions up to **~30 days** in principle—confirm current SCC policy in official docs).
- Training can be **resumed from an earlier cycle** without starting from scratch (see follow-up).
- Strategy: request “enough” time (e.g. overnight), monitor progress, extend/resubmit as needed.
- **Queueing:** SCC “best practices” materials encourage requesting **≤ 12 hours when reasonable**, because **more nodes** tend to serve short queues faster than very long requests.

**Endorsement:** Andrew Wood (professor) endorsed this students’-wiki style answer.

### Follow-up (Student) — resolved

“How exactly can we **start training from a previous cycle**?”

### Answer (Selene Wu — TA)

Use **`SequentialTrain`** checkpointing flags (names from thread):

- **`--inFile`** — path to an existing **`.model`** file to initialize from (example: `--inFile params/qfunction10.model`).
- **`--outFile`** — write outputs to a **new path** so you do not overwrite prior artifacts.
- **`--outOffset`** — shift the **numeric suffix** of emitted model filenames when continuing a run.

**Example (from thread):** resume from **cycle 10** by loading that checkpoint and offsetting output numbering:

```bash
SequentialTrain --inFile params/qfunction10.model --outOffset 11
```

**Endorsement:** Collin Barber (TA) endorsed Selene’s instructions.

**Takeaway:** Wall-time requests should follow SCC policy + best-practices doc; **checkpoint resume** is supported via **`--inFile`** / **`--outFile`** / **`--outOffset`** on `SequentialTrain`.

---

## Thread 021 — @423 — Clarification on our rules for Risk

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/16/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 0

### Question (Student)

Does the course’s version of Risk model **alliances** between players?

### Answer (Andrew Wood — professor)

Alliances in classic Risk are **implicit agreements between humans**; the assignment **does not formalize** that kind of agreement, so it is **not encoded in the API**.

Students **may** design a **team of agents** that **communicate** to help their learner during **training**, but for **evaluation** (autograder / tournament) the submitted agent is **on its own** (no alliance mechanism in the official eval setup).

**Takeaway:** No first-class “alliance” object in the course Risk API; multi-agent coordination is a **training-time** design choice only, not part of graded solo eval.

---

## Thread 022 — @424 — Define Action Counter

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/16/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 0

### Question (Student)

What is **`actionCounter`** for? Why track an action count, and how is it meant to be used?

### Answer (Andrew Wood — professor)

`actionCounter` is **how many actions the player has taken this turn**.

It exists for **internal engine reasons** (example given: edge cases around **redeeming cards** while in the **redeem phase** of a turn).

Students **may** also use it to track how many moves they have made within **different phases** of the turn if that helps their agent logic.

**Takeaway:** Treat `actionCounter` as **per-turn action index** exposed for framework correctness + optional student bookkeeping—not arbitrary global state.

---

## Thread 023 — @425 — ActionSensor vs MyPlacementSensor

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar date:** 4/16/26  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Follow-up count shown:** 0

### Question (Student)

What is the difference between **action sensors** and **placement sensors**? The student understands action sensors relate to which actions are better, but is unsure how that differs from a **placement** sensor that looks at territory/army setup.

### Students’ answer (Student)

Both are used so the agent can compute **Q-values** from encoded state/action features, but they target **different decision types**:

**Action sensor (general actions)**

- Used when evaluating a **generic action** (not only placement).
- Features called out in the thread include:
  - **action type**
  - **source / destination** territories (e.g. attacks)
  - **number of armies** involved in the action

**Placement sensor (reinforcement placement)**

- Used specifically when deciding whether / how strongly to **place an army** in a given territory.
- Features called out include:
  - **which territory** is under consideration
  - **territory-specific** signals relevant to good placements

**Endorsement:** Andrew Wood (professor) endorsed this students’-wiki answer.

**Takeaway:** **PlacementSensor** ≈ “where to put new armies”; **ActionSensor** ≈ “evaluate concrete moves (types, endpoints, troop counts).” Keep feature engineering aligned with the decision each head is responsible for.

---

## Thread 024 — @426 — Risk params.model where???

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Sidebar / file timestamps (from capture):** around **4/16–4/17/26**  
**Timeline (from UI):** updated ~2 weeks ago at capture

### Question (Student)

Expected **`qFunction` / `.model` checkpoints** under a **`params/`** directory (like **Lab 5**), but during Risk training the **`params/`** folder did not seem to update. Later noticed artifacts like **`params.model1.model`, `params.model2.model`, …** appearing **outside** `params/` and planned to **pass an explicit output directory**.

### Students’ answer (Selene Wu — TA)

- Checkpoints **do** go under **`params/`** when configured that way.
- When a **full training cycle** completes, the trainer should print **cycle index**, **average utility**, and **eval win counts** (per thread).
- If a cycle **never finishes**, games may be **not terminating**:
  - **Random vs Random** can take **extremely long**.
  - A **trained** agent may **never attack** a **static** agent → de facto infinite training games.
- Suggestion: train against a **more aggressive / termination-friendly** opponent so cycles complete.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

### Answer (Andrew Wood — professor)

If checkpoint **filenames are non-default**, the student is probably passing an **output path flag**. That argument is the **full path**, not “just a filename” — misunderstanding can drop files **outside** `params/` even though the basename still looks like `params.modelK.model`.

### Follow-up (Student) — resolved

1v1 games **finish**, but files still seemed missing from `params/` — then realized outputs were landing **outside** `params/` and asked why (resolved after Andrew’s path explanation / manual folder choice).

### Filenames (from capture)

```text
params.model1.model
params.model2.model
…
params/
```

**Takeaway:** Treat training **`--outFile` / output-path arguments as full filesystem paths**; verify **cwd** on SCC vs laptop. If cycles stall, fix **game termination / opponent choice** before chasing “missing” models.

---

## Thread 025 — @428 — More Agents to Train/Test Against?

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~2 weeks ago at capture  
**Note:** Sidebar date not legible in capture; ordered after **@426** by post number.

### Question (Student)

When will staff release extra **Risk bots / fixed-function opponents** mentioned in the assignment PDF?

**Student update (inline in post):** Asked in lecture; told **“this weekend.”**

### Students’ answer (Selene Wu — TA)

Students likely **will not receive** the autograder-only opponents as standalone artifacts; those live **behind the autograder**. For local training, students should supply their own opponents (hand-coded policies, heuristics, **self-play**, etc.).

### Answer (Andrew Wood — professor)

He will release some **fixed-function agents** **that weekend** (per his reply).

**Takeaway:** Expect downloadable/reference **fixed-policy** opponents for local experiments; hidden tournament/autograder bots remain separate.

---

## Thread 026 — @434 — Does early elimination prevent learning from losing games?

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~2 weeks ago at capture

### Question (Student)

If their agent is **eliminated mid-training**, the game may end with **`training=false`**. The student worries this **drops transitions** so the agent cannot learn from **losses**, and asks whether they should **manually record** transitions up to elimination.

### Answer (Andrew Wood — professor)

Transitions are recorded **live during the game** by **`TrainerAgent`**. That component listens for **turn-end** events; the listener still runs when an agent is **disqualified / eliminated**.

Therefore the student **does not** need to manually stitch transitions on elimination—the framework records them when the listener fires.

**Takeaway:** Elimination does not silently erase learning signal **as long as** the standard trainer/listener path is in use; don’t duplicate recording unless you have a custom training pipeline and know what you’re overriding.

---

## Thread 027 — @436 — qsub not working on bash script

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~1 week ago at capture

### Question (Student)

On the **SCC**, running:

```bash
qsub risk_train.sh
```

prints **`qsub: Unknown option`** three times. Directory listing includes `risk_train.sh`, `risk_train.py`, `risk.srcs`, `lib`, `params`, `src`, etc.

They ask what might cause this on the cluster.

### Answer (Andrew Wood — professor)

Likely a **bad scheduler directive** inside the submit script (wrong **`#PBS`** / **`#$`** / similar **embedded option line** for the site’s `qsub` flavor).

**Takeaway:** `qsub` parses **directives in the shell script** first; repeated “Unknown option” usually means **typo or unsupported flag** in those lines, not the filename itself.

---

## Thread 028 — @440 — Territory ID vals

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~1 week ago at capture  
**Follow-up count shown:** 1

### Question (Student)

Are territory **id** values simply **0–41**?

### Answer (Selene Wu — TA)

**Yes.**

**Endorsement:** Collin Barber (TA) endorsed this answer.

### Follow-up (Anthony Hardimon) — resolved

You can inspect **`maps/world/map.json`** (e.g. under the unzipped / exploded JAR layout **`.jar/maps/world/map.json`** as written in the thread) for full territory / country metadata.

**Endorsement:** Andrew Wood (professor) endorsed this follow-up.

**Takeaway:** Treat **0–41** as the standard territory id span for the bundled world map; **`map.json`** is the authoritative schema dump.

---

## Thread 029 — @443 — Risk Vocab / Rules Questions

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~1 week ago at capture  
**Follow-up count shown:** 0

### Question (Kenneth Wilber & Student)

*(Piazza shows co-updaters **Kenneth Wilber** and **Anonymous Atom** → **Student**.)*

1. **Army budget:** What is an “army budget”? You cannot bank armies across turns—so is it **0** whenever it is not your placement window?

2. **Card visibility:** Can you see **other players’ hands**? (Classic Risk often hides this.)

3. **Redemption payouts:** Online references mention **fixed vs progressive** reinforcement for card sets—which does this codebase use?

### Answer (Selene Wu — TA)

1. **Army budget:** It is the count of armies **you still have left to place this placement window**. You **cannot bank** unplaced armies across turns, so outside of receiving/placing reinforcements it behaves like **0** in the relevant sense.

2. **Card hands:** In this assignment you **can observe opponents’ hands** (whether that signal is useful is separate).

3. **Redemption curve:** The course Risk uses **progressive** redemption: as **more total redemptions** happen (across players), the **next** redemption tends to grant **larger** army payouts (per thread wording).

**Implementation hook (from thread):** inspect `TerritoryCard.getRedemptionAmount(n)` for how many armies the **n-th** redemption awards.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

**Takeaway:** Expect **transparent card hands**, **no banking** of placement armies, and **progressive** redemption scaling—confirm exact numbers via `getRedemptionAmount`.

---

## Thread 030 — @444 — How to get a Static agent occurrence (On purpose)

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~1 week ago at capture

### Question (Student)

What **player moves / policy choices** cause an agent to be **replaced by a `StaticAgent`** (or any other replacement type if applicable)?

**Edit:** Student wondered if simply **failing to redeem cards** was enough.

### Students’ answer (Selene Wu — TA)

Any **illegal move** should trigger replacement with **`StaticAgent`**. There is **no supported path** to be swapped to a **different** replacement agent type.

If the **JAR is correct**, the framework **should not present** the neural policy with **illegal** actions in normal operation.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

### Answer (Andrew Wood — professor)

Reminder of the **forced-redeem** edge case: during the **`redeem cards`** phase, the **default exploration** path **does not** fully respect the rule that with **≥ 5 cards** you **must redeem** and **cannot** take **`NoAction`**. Violating that can **disqualify** the agent (hence `StaticAgent` replacement).

**Takeaway:** `StaticAgent` swap = **rule violation / illegal action**. Watch **forced redeem** vs exploration defaults; do not expect other replacement agent types.

---

## Thread 031 — @445 — QSUB not putting job into queue?

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3` (and related logistics captures)  
**Timeline (from UI):** updated ~1 week ago at capture

### Question (Student)

`qsub` fails immediately with:

```text
Unable to run job: error: no suitable queues. Exiting.
```

Observations from the thread:

- Seems tied to requesting **> 12 hours** in some attempts.
- Confusion about **`-l h_rt=hh:mm:ss`** vs documentation implying **very long** max wall times (how to express multi-day limits in `hh:mm:ss`).

### Answer (Andrew Wood — professor)

The failure may **not** be wall time alone—it can be **cores (`-pe omp …`)** and **`mem_per_core`** together. If no queue matches the **combined** resource shape, you get **“no suitable queues.”**

### Follow-up (Student)

“Asks what the submit script looks like.”

### Example working script (Anthony Hardimon — student)

```bash
#!/bin/bash -l
#$ -N risk_seqtrain
#$ -l h_rt=11:00:00
#$ -pe omp 2
#$ -l mem_per_core=12G
#$ -j y
#$ -o /projectnb/cs440/students/ajh756/CS440/logs

module load java/21.0.4
# … compile / run …
```

### Failed attempts (same thread)

Student tried **higher** shapes first, e.g.:

```bash
#$ -l h_rt=24:00:00
#$ -pe omp 4
#$ -l mem_per_core=28G
```

then reduced to:

```bash
#$ -l h_rt=24:00:00
#$ -pe omp 2
#$ -l mem_per_core=12G
```

Still had issues; lowering **`h_rt`** alone did not always fix “no suitable queues” if the **remaining** request was still unsatisfiable.

### Answer (Collin Barber — TA)

- The **more specific** your combined request (cores × memory × time), the **fewer** queues can satisfy it—often **trial and error** unless you read the **official SCC queue / host capability** documentation linked from the course.
- Example of a **rare but valid** “big” request shape mentioned in-thread (do **not** assume you can get this): `h_rt=24:00:00`, **`omp 36`**, **`mem_per_core=16G`** — staff noted only **one** matching queue instance existed when they checked.
- Practical tuning: prefer **smaller / power-of-two-ish** `mem_per_core`, drop optional knobs you are willing to let the scheduler choose, and narrow down systematically (“scientific method”).

### Follow-up (Anthony Hardimon)

How do you request **more than 24 hours**? Are these valid?

```bash
#$ -l h_rt=99:00:00
#$ -l h_rt=100:00:00
```

### Reply (Collin Barber — TA)

Believes **`99:00:00`** and **`100:00:00`** are **accepted syntax** for filtering queues that allow **at least** that wall time.

**Due-date advice (from thread):** with the assignment due soon, **avoid** launching **~99h** monoliths—**chunk training** into **multiple shorter jobs** with checkpoints instead.

**Takeaway:** “No suitable queues” = **no partition matches all** of `{time, cores, memory, …}`. Start from a **known-good small template** (like Anthony’s **11h / 2 cores / 12G-per-core**), then change **one variable at a time**. Use **`qstat` / cluster docs** to confirm the job actually runs after browser hiccups.

---

## Thread 032 — @449 — Ending training game early

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~1 week ago at capture

### Question (Student)

Is there a supported way to **terminate a training game early** (e.g. after a **turn limit**) and advance to the next episode **from code**?

### Students’ answer (Selene Wu — TA)

Hacky approach: force an **illegal move** so the agent is **disqualified** and replaced with a **`StaticAgent`**, which **random** opponents can usually **finish off quickly**, ending the match.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

### Answer (Andrew Wood — professor)

There is **no first-class “graceful early stop” API** in the framework. Returning an **invalid / disqualifying** action **does** end the training game early.

**Critical caveat:** design that hack so it **does not corrupt the replay buffer** (unwanted transitions, wrong terminal flags, etc.).

**Endorsement:** Aakash Kumar (TA / course staff) endorsed Andrew’s answer.

### Follow-up (Student)

Against **static** opponents, games can run **10k+ turns**; disqualification alone may not give the “train faster” behavior they want. They consider **living penalty** per turn or a **hard turn cap** in the reward / logic.

### Follow-up (Collin Barber — TA)

- Expose **turn index / turn counter** as a **sensor feature** so the policy can condition on “how long this game has dragged.”
- Often easier to **reward aggression / progress** than to punish **turtling** in isolation.

### Follow-up (Student)

They **removed many shaping rewards** after **exploits**; now mostly **sparse “checkpoint” rewards** (e.g. eliminations, win) plus a **per-turn living penalty**, but the agent still **stalls early** in training.

### Follow-up (Selene Wu — TA)

Train against **more aggressive opponents** so passivity loses quickly—forces responsive play.

**Endorsement:** Collin Barber (TA) endorsed Selene’s suggestion.

### Follow-up (Collin Barber — TA)

If you want a behavior, **positively reward instances of that behavior** when they occur. **Pure penalty** on “bad” play often teaches the **minimum** needed to dodge the penalty—not the skill you actually want.

**Endorsement:** Andrew Wood (professor) endorsed Collin’s follow-up.

**Takeaway:** No official early-abort hook—**DQ via illegal move** is a dangerous hammer; prefer **opponent curriculum**, **turn-count sensors**, and **careful positive reward** over long **only-penalty** regimes, and always guard the **replay buffer**.

---

## Thread 033 — @450 — What utility scores are you guys getting for risk rn

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~1 week ago at capture  
**Note:** Post number **`@450`** inferred from sequence after **@449**—replace if your Piazza header shows a different id.

### Question (Student)

Asks peers what **utility / average reward** numbers they are seeing in Risk training right now (title only + “^^^^” in body per capture).

### Students’ answer (Selene Wu — TA)

Reported numbers are **not comparable** across students: they depend heavily on **reward lower/upper bounds**, the **exact shaping**, and **γ (gamma)**.

Example datapoint only: with an **upper reward bound ~20** and **γ = 0.99**, one run peaked around **~200 average reward**—but Selene stresses that **without the full reward definition**, the scalar is **almost meaningless** for benchmarking others.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

**Takeaway:** Compare policies with **matched reward scaling + γ + opponent mix**, not raw headline utilities from different reward designs.

---

## Thread 034 — @451 — Sensory Array

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~1 week ago at capture  
**Note:** Post number **`@451`** inferred after **@450**—correct if Piazza shows otherwise.

### Question (Student)

Should **sensory / state-sensor** features be mostly **boolean `{0,1}`** or **real-valued in `[0,1]`**? May the vector be **shorter** than the default template (e.g. fewer than **15** entries)?

### Students’ answer (Student)

- **Either** representation can be right: pick **boolean** vs **continuous** per feature based on what the signal **means**.
- **Continuous** encodings must be **semantically justified**—example called out as **bad:** `territoryId / 42` just to force a float in `[0,1]` **does not** add useful geometry for learning.
- You may use **fewer or more** features than the starter layout **if you know why**.

**Performance warning (from thread):** Andrew’s bundled **matrix code** is described as **“highly suboptimal”** with **\(O(n^3)\)**-style cost for multiplication in the implementation students use—**very wide** sensor vectors can **slow training** badly.

**Endorsement:** Andrew Wood (professor) endorsed this students’-wiki answer.

**Takeaway:** Design features for **meaning**, not dtype theater; mind **dimensionality** for **runtime**, not only for ML bias/variance.

---

## Thread 035 — @452 — track when our turn begins inside of reward function

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~1 week ago at capture  
**Note:** Post number **`@452`** inferred after **@451**—correct if Piazza shows otherwise.

### Question (Student)

Inside **`MyActionRewardFunction.java`**, they want to detect when **control passes from an opponent to their agent**—effectively whether the current transition is the **first action of their turn**.

They note **`RiskListener.onTurnStart()`** exists in the framework but believe they **cannot access** a listener instance from the reward-function API surface.

### Answer (Andrew Wood — professor)

No **convenient supported** way surfaced in the current design.

Andrew admits the engine **already stores** the needed information internally, but it was **not threaded through** the **reward-function API** when that interface was designed—so students **lack** an official hook for “turn just started” from inside the reward class as given.

**Takeaway:** Treat “first action of my turn” as **not exposed** unless you add **your own bookkeeping** outside the provided reward API (e.g. agent-side state) or get a **staff-approved** extension—don’t rely on undocumented internals.

---

## Thread 036 — @453 — AggroAgent out

**Post type:** Note (announcement)  
**Piazza tags (visible):** `pas`, `pas/pa3`, `logistics`  
**Timeline (from UI):** updated ~7 days ago at capture  
**Note:** Post number **`@453`** inferred after **@452**—correct if Piazza shows otherwise.

### Note (Andrew Wood — professor)

Andrew finished testing **`AggroAgent`**, described as the **first** of several **fixed-function** opponents he is releasing. A **`TurtleAgent`** is expected **soon after**.

**Where to find artifacts:** **General Resources** (Piazza course resources area).

**Packaging:** **`.jar`** files whose names include **`fixed-function`** (per post). One JAR holds the **code**; a separate JAR holds **documentation** (as described in the announcement).

**Takeaway:** Use **`AggroAgent`** JARs from **General Resources** as a stronger training opponent than pure random/static defaults; watch for **`TurtleAgent`** follow-up.

---

## Thread 037 — @461 — 3-player training gets stuck after my agent is eliminated

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~6 days ago at capture  
**Status:** Resolved (per second capture)

### Question (Student)

Running **`SequentialTrain`** with **`RiskQAgent`** vs **two `RandomAgent`s** (3-player). Example command shape from thread:

```bash
java -cp "./lib/*:./src:." edu.bu.pas.risk.SequentialTrain \
  pas.risk.agent.RiskQAgent random random \
  -x 20000 -t 1 -v 1 -u 5 ...
```

When the student’s agent is **eliminated early**, the two remaining **random** players can play **extremely long** games, so **training cycles never finish** (e.g. ~**5 cycles / 20 minutes** vs thousands/hour in 2-player).

They ask whether a **max-turns** flag exists, what the **recommended** setup is, or whether to **stick to 2-player** training.

### Answer (Collin Barber — TA)

Because **random** agents are weak / chaotic, **indefinite** games become likely when the student’s bot is also **not strong enough** to force closure.

Suggested curricula:

- Train vs **even weaker** opponents so the student agent **almost always wins quickly**, or
- Train vs **much stronger** opponents so games **end quickly** in the other direction.

**Endorsement:** Andrew Wood (professor) endorsed Collin’s answer.

### Follow-up (Student) — resolved

“Why are you doing **1 training game per cycle** and **1 eval game per cycle**?” *(Challenge to inefficient `SequentialTrain` flags / throughput.)*

**Endorsement:** Andrew Wood (professor) endorsed this follow-up.

**Takeaway:** **3× random** after you die is a **stochastic slow game** hazard—fix **opponent mix / strength**, and review **`-t` / `-v` / games-per-cycle`** so you are not burning wall time on **1 train + 1 eval** if that is unintentional.

---

## Thread 038 — @463 — Action Sensor Questions

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~6 days ago at capture

### Question (Student)

1. In **`AttackAction`**, what is **`movingArmies`** vs **`attackingArmies`**?
2. Should **everything** be normalized to **`[0,1]`** (including **`actionCounter`**, army counts, etc.)? What if there is **no natural max**?
3. Is it reasonable to normalize by **another quantity from the live `gameState`** that **changes over time**?
4. How to spot **useless / redundant** features—e.g. **`feature3 = feature1 / feature2`**?

### Students’ answer (Selene Wu — TA)

1. **`attackingArmies`:** integer in **1–3** range tied to **how many dice** you roll to attack.  
   **`movingArmies`:** how many armies you **move into** the captured territory **after a successful attack** (capture advance).

2. **Normalization:** generally **helpful**. For **unbounded** signals, pick a **practical upper bound estimate** or apply a **nonlinear squashing** so values do not blow up.

3. **State-dependent scaling:** sounds **reasonable** if the denominator has a clear meaning and avoids divide-by-zero.

4. **Redundancy:** networks often learn **linear mixes**; a strict **linear combination** of existing inputs may be **redundant**, but you might **keep** it if you do not trust the net to discover that combination quickly enough.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

### Follow-up (Student)

How to normalize **army counts** with **no obvious cap**?

### Follow-up (Selene Wu — TA)

Lab-style trick: **`log(x + 1)`** with a **scalar multiplier** so typical values sit **below 1**, then **hard-clamp** outputs to **`1.0` max**. Reported as “seemed okay so far” but **not guaranteed optimal**.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s follow-up.

**Takeaway:** Encode **dice count (1–3)** vs **post-battle troop movement** distinctly; for heavy-tailed counts use **log + clamp** or similar rather than pretending a fake max exists.

---

## Thread 039 — @466 — Amount of features

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~6 days ago at capture

### Question (Student)

Starter templates show default **sensor sizes** like **5 / 10 / 15** for different arrays. Is it sensible to shrink to e.g. **4 placement**, **8 action**, **10 state**?

### Students’ answer (Selene Wu — TA)

Those defaults are **not sacred**—“random numbers Prof Wood made up.” Whether **5/10/15** is “enough” depends on **how granular** your features are; Selene thinks the defaults may be **a bit low** for rich encodings but it is **not a hard rule**.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

### Answer (Andrew Wood — professor)

Play **Risk yourself** and notice what you look at when choosing moves: what **board/move information** you use to judge quality. Formalize **those** signals as features—good starting point for sizing and designing the arrays.

**Takeaway:** Feature **count** follows **feature semantics**, not the default triple; justify each slot from **human decision cues** you want the net to approximate.

---

## Thread 040 — @467 — TurtleAgent out

**Post type:** Note (announcement)  
**Piazza tags (visible):** `pas`, `pas/pa3`, `logistics`  
**Timeline (from UI):** updated ~6 days ago at capture  
**Follow-up count shown:** 0

### Note (Andrew Wood — professor)

**Documentation** and **code** **`.jar`** artifacts for **`TurtleAgent`** are now published (**version `1.1.0`**).

**Takeaway:** Pair with earlier **`AggroAgent`** drop (**Thread 036 — @453**): staff are shipping a small **fixed-function opponent zoo** under course resources—pull **`1.1.0`** JARs for training curricula.

---

## Thread 041 — @469 — Max # of cards, max # of troops

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~6 days ago at capture  
**Follow-up count shown:** 2

### Question (Student)

For **sensor normalization**, can they assume an agent holds **at most 5 cards** at a time? Is there a **360 troop** cap like some classic Risk references, or can troop counts grow **unbounded**?

### Students’ answer (Selene Wu — TA)

- You can temporarily hold **more than 5 cards** right after **eliminating** an opponent and **taking their deck**.
- There is **no fixed global max** on **troop counts** in the sense of “stops at 360”; treat armies as **unbounded** for normalization purposes (use squashing / logs / caps as elsewhere).

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

### Follow-up (Student)

In classic Risk, after a knockout you may exceed 5 cards and must **redeem down to ≤ 4** before continuing—does that apply here?

### Reply (Selene Wu — TA)

**Yes**, the same **forced-redemption** behavior applies in this codebase’s rules.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s reply.

**Takeaway:** **Card count** is **not bounded by 5** transiently; **troop totals** are **not capped at 360**—design sensors with **log/clip** style bounds, and remember **post-elimination redeem chains** align with earlier **Thread 018 (@411)** / **Thread 030 (@444)** guidance.

---

## Thread 042 — @470 — Some strange behavior on average trajectory utility

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~6 days ago at capture

### Question (LI JIAJUN)

They added **printouts** in **placement** and **action** reward hooks and saw **varying** per-step rewards (examples ranged from **roughly -6 … +32** in captures—not uniformly stuck at one constant).

Yet after training against a **static** opponent “just to sanity-check,” logged cycles report **`avg(utility) ≈ -100`**, matching their configured **lower reward bound** (`-100`), while **`avg(wins)=1.0`**.

They ask how **per-step reward diversity** can coexist with an **average utility pinned at the lower bound**.

### Answer (Andrew Wood — professor)

**Evaluation games** compute, for each trajectory, the **sum of discounted rewards** along that trajectory, then **average** those trajectory totals **across eval games**.

So the printed per-step rewards need not “look like” the aggregated **discounted return** metric the trainer reports.

**Takeaway:** **`avg(utility)`** is a **return-style aggregate**, not a simple mean of the same printouts you sprinkled in reward shaping—check **γ**, **episode length**, **terminal transitions**, and **which phase** your prints sample vs what eval integrates.

---

## Thread 043 — @473 — How much does the reward function matter

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** updated ~6 days ago at capture

### Question (Student)

They instrumented **`QRiskAgent`** during **`SequentialTrain`** with prints/counters and observe long stretches like:

```text
Attacking: 1000
Attacking: 1001
Fortifying: 1002
Attacking: 1003
… (many alternating Attack / Fortify / Redeem steps into the 1000s+)
```

They wonder whether this “stuck” feeling is because the **reward signal already pegged the configured upper bound** even though the **game is not actually won**.

*(Separate placement-phase question: **Thread 044 — @475**.)*

### Answer (Andrew Wood — professor)

The **reward function matters a lot**.

Typical good practice: align **global max / min returns** with genuinely **best-case** vs **worst-case** **overall outcomes** (e.g. dominant win vs catastrophic loss). You **do not** want routine actions that are **not** truly best/worst trajectories to **saturate** those **extrema**—that hides learning signal and can create odd training dynamics.

### Follow-up (Student)

**Stalemate / termination hacks:** e.g. if the bot exceeds ~**100 turns**, downgrade to **`StaticAgent`** so it **loses quickly** and the episode ends; or crown the **largest army** player winner and force others **static** (noted to work only with **custom** opponents you author).

**Takeaway:** Long repetitive phase logs often mean **shaping / exploration / opponent mix** issues—not necessarily “buggy RL”—and **never** tie **max reward** to mundane churn unless that churn truly equals your intended “best world.”

---

## Thread 044 — @475 — Setup placement vs normal placement

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** question updated ~6 days ago; students’ answer ~5 days ago at capture  
**Follow-up count shown:** 0

### Question (Student)

For the **placement reward function**: is **`state.getNumTurns() == 0`** true for the **entire** initial **setup placement** phase, or do **turn counters advance during setup** too?

### Students’ answer (Selene Wu — TA)

Use **`state.getUnownedTerritories().size() > 0`** as the setup check. That means **late setup** can start looking like “regular” placement once **all territories are claimed**—poster considers that **reasonable**.

**Takeaway:** **`getNumTurns() == 0`** is **not** the reliable discriminator for “still in setup”; prefer **unowned-territory emptiness** (or another state invariant you verify in code) if you need a clean setup vs post-setup split.

---

## Thread 045 — @476 — Instr EvalAgent for Risk :)

**Post type:** Note (staff)  
**Piazza tags (visible):** `pas`, `pas/pa3` (and broader course tags in nav: `logistics`, `other`, `was`, `las`, …)  
**Timeline (from UI):** Thursday; updated ~5 days ago at capture  
**Views (capture):** 101  
**Follow-up count shown:** 0

### Note (Collin Barber — TA)

Students in office hours struggled to **evaluate a trained policy by eye** during normal play. Collin shared a small **`EvalAgent`** subclass you can drop beside **`RiskQAgent.java`**: load **your** saved weights, **disable exploration** so behavior is **greedy / eval-only**, and run **one agent in the UI** for qualitative inspection. **“Code is lightly tested.”**

**Endorsement:** Andrew Wood (professor) marked **good note**; **3** positive reactions at capture.

### Where your hooks live (paths from the note)

```text
# rewards
src/pas/risk/rewards/MyActionRewardFunction.java
src/pas/risk/rewards/MyPlacementRewardFunction.java

# senses
src/pas/risk/senses/MyActionSensorArray.java
src/pas/risk/senses/MyPlacementSensorArray.java
src/pas/risk/senses/MyStateSensorArray.java

# agent
src/pas/risk/agent/RiskQAgent.java
src/pas/risk/agent/EvalAgent.java
```

### Reference implementation (archive of Piazza snippet)

Copy into **`EvalAgent.java`** (same package as **`RiskQAgent`**), then point **`model.load(...)`** at **your** checkpoint path.

```java
package pas.risk.agent;

import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.action.Action;
import edu.bu.pas.risk.model.DualDecoderModel;
import edu.bu.pas.risk.territory.Territory;

public class EvalAgent extends RiskQAgent {

    public EvalAgent(int agentId) {
        super(agentId);
    }

    @Override
    public DualDecoderModel initModel() {
        DualDecoderModel model = super.initModel();

        // TODO: put your agent model file here
        try {
            model.load("./params/qFunction0.model");
        } catch (Throwable ex) {
            // failed to load model
            ex.printStackTrace();
            System.exit(-1);
        }
        return model;
    }

    @Override
    public boolean shouldExploreRedeemMovePhase(GameView game, int actionCounter, boolean canRedeemCards) {
        return false;
    }

    @Override
    public boolean shouldExploreAttackRedeemIfForcedMovePhase(GameView game, int actionCounter, boolean canRedeemCards) {
        return false;
    }

    @Override
    public boolean shouldExploreFortifySkipMovePhase(GameView game, int actionCounter, boolean canRedeemCards) {
        return false;
    }

    @Override
    public boolean shouldExplorePlacementPhase(GameView game, int actionCounter, boolean isDuringSetup, int remainingArmies) {
        return false;
    }

    @Override
    public Action getExplorationRedeemAction(GameView game, int actionCounter, boolean canRedeemCards) {
        System.err.println("[EvalAgent] Somehow #getExplorationRedeemAction was called... do you call it manually?");
        return super.getExplorationRedeemAction(game, actionCounter, canRedeemCards);
    }

    @Override
    public Action getExplorationAttackActionRedeemIfForced(GameView game, int actionCounter, boolean canRedeemCards) {
        System.err.println("[EvalAgent] Somehow #getExplorationAttackActionRedeemIfForced was called... do you call it manually?");
        return super.getExplorationAttackActionRedeemIfForced(game, actionCounter, canRedeemCards);
    }

    @Override
    public Action getExplorationFortifySkipAction(GameView game, int actionCounter, boolean canRedeemCards) {
        System.err.println("[EvalAgent] Somehow #getExplorationFortifySkipAction was called... do you call it manually?");
        return super.getExplorationFortifySkipAction(game, actionCounter, canRedeemCards);
    }

    @Override
    public Territory getExplorationPlacement(GameView game, boolean isDuringSetup, int remainingArmies) {
        System.err.println("[EvalAgent] Somehow #getExplorationPlacement was called... do you call it manually?");
        return super.getExplorationPlacement(game, isDuringSetup, remainingArmies);
    }
}
```

**Takeaway:** For **human-in-the-loop** checks, mirror staff pattern: **subclass `RiskQAgent`**, **`load` weights**, **`shouldExplore* → false`**, and treat stray **`getExploration*`** calls as **bugs** (stderr breadcrumbs) rather than silent random play.

---

## Thread 046 — @478 — avg(utility)=NaN avg(wins)=NaN

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~5 days ago at capture; Thursday in sidebar  
**Follow-up count shown:** multiple (sidebar at one point: **1 unresolved**)

### Question (Student)

Running **`SequentialTrain`**, logs show **`avg(utility)=NaN`** and **`avg(wins)=NaN`** every cycle—what could cause this?

Example command / output (as posted):

```bash
java -cp "./lib/*:src:." edu.bu.pas.risk.SequentialTrain pas.risk.agent.RiskQAgent static -t 5 -v 0 -x 2
```

```text
AgentIdx Agent
0        edu.bu.pas.risk.agent.StaticAgent
1        edu.bu.pas.risk.agent.TrainerAgent
Game ended
… (repeated)
[INFO] after cycle=0/2 avg(utility)=NaN avg(wins)=NaN
…
[INFO] after cycle=1/2 avg(utility)=NaN avg(wins)=NaN
```

### Students’ answer (Selene Wu — TA)

Suspect **`0` eval games**—that would produce **`NaN`** aggregates.

### Instructors’ answer (Andrew Wood — professor)

If the **model** ever outputs **`NaN`**, **downstream everything** becomes **`NaN`**. Audit that **features** and **rewards** are **never** **`NaN`**.

### Follow-up (Student) — resolved branch

Even with **eval games = 1**, it **never seems to end**—why?

### Follow-up (Collin Barber — TA)

**NaN is sticky:** almost any op involving **`NaN`** returns **`NaN`**. A **`NaN` in a sensor** → **`NaN` forward pass** → **`NaN` backward** → **`NaN` optimizer step** → “everything is **`NaN`**.”

### Follow-up (Student)

They loop-check **placement** sensor entries with **`Double.isNaN`**—**no hits**. They also print when **reward** is **`NaN`**—**never fires**.

```java
for (int i = 0; i < MyPlacementSensorArray.getShape().numCols(); i++) {
    if (Double.isNaN(MyPlacementSensorArray.get(0, i)) == true) {
        System.out.println("Placement NaN at id: " + i);
    }
}
```

### Follow-up (Selene Wu — TA) — ties together “stuck eval”

If **`evalGames = 0`**, that **explains the `NaN` metrics**. If **eval games do not finish**, the **match is stuck**—often because **neither side is strong enough** to **force terminal outcomes**; you need **your learner** or **the opponent** competent enough to **end games**.

### Follow-up (Student) — separate bug hypothesis (still open in capture)

In **action** sensor they **divide by total armies across their territories**; sometimes the **denominator is `0`**. Shouldn’t post-setup imply **≥ 1 army somewhere**?

### Follow-up (Selene Wu — TA)

Maybe **right after elimination** (no owned territories / zero total). Regardless, **that division issue is not what’s driving the reported `avg(*)=NaN`** here—the poster’s **`evalGames = 0`** setting **is**.

**Takeaway:** Treat **`NaN` aggregates** as (1) **empty or invalid eval sample** (e.g. **`0` eval games** → **`0/0`**) and/or (2) **numeric contagion** from **bad tensors**; **stuck infinite eval** is a **gameplay / opponent-strength** problem. For **divide-by-army-total** features, **guard** or define behavior at **elimination** / **zero-mass** states. *(Related trainer metric semantics: **Thread 042 — @470**.)*

---

## Thread 047 — @494 — Does anyone else's agent just get stuck during training?

**Post type:** Question (resolved in capture)  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~3 days ago at capture  
**Views (capture):** 100

### Question (Student)

**Curriculum:** **`RandomAgent`**, then switch to **`AggroAgent`** after ~**200** turns of play.

After **~1.5 h** on the **SCC**, training reached **cycle 10**, then appeared **wedged on one game** for **~7 h** until the job was killed the next morning. Others seeing the same?

Log flavor (abbrev.):

```text
[INFO] after cycle=9/1000000 avg(utility)=48.702677327876636 avg(wins)=0.0
switch to aggro
Game ended 301
…
switch to aggro
Game ended 314
```

### Students’ answer (Selene Wu — TA)

Same failure mode: over **200** turns their learner built such a **positional/material edge** that **`AggroAgent` could not break them**, yet the learner also **did not attack enough to actually win**—**indefinite stall**.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

### Instructors’ answer (Aakash Kumar — TA)

**Reward alignment:** if the agent earns **net positive reward each turn**, it may **prefer never ending**—if the **discounted return from “keep farming small positives”** dominates the **return from forcing a terminal win**, **stalling** is rational under that shaping.

### Follow-up (Student)

**Mitigation idea:** end the **random** phase **earlier** (e.g. **~100** turns), then make **other** bots **`StaticAgent`** so only **one** **`AggroAgent`** remains active.

### Follow-up (Selene Wu — TA)

How do you **force** an opponent to behave **`StaticAgent`**-like mid-run?

### Follow-up (Student)

Either have it **return `NoAction` every round**, or deliberately **break rules** (e.g. **refuse mandatory card redeem** when holding **≥ 5** cards) so the engine treats it as static / non-progressing—**courseware-dependent**; verify against your local **`Risk`** rules before relying on “illegal move” tricks.

**Takeaway:** Long-run **curriculum handoffs** can land in **non-terminal equilibria**—neither side can close—especially when **rewards reward existing** more than **winning**. Combine **shorter early phases**, **stronger closers**, **`StaticAgent` fillers**, and **terminal-biased shaping** (see also **Thread 043 — @473** stalemate discussion).

---

## Thread 048 — @496 — SCC QSUB issues

**Post type:** Question (resolved in capture)  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~3 days ago at capture  
**Views (capture):** 84  
**Follow-up count shown:** ~6 (Piazza aggregate)

### Question (Student)

After **`qsub`**’ing a wrapper on the **SCC**, the job **finishes in ~2 minutes** with a completion email—suspected **immediate startup failure**. Unsure whether the **shell script** or **Java** was wrong. Initial script only had **SGE directives** + **`module load java/21.0.4`**—**no** **`javac` / `java`**.

Example **SGE header** (as posted):

```bash
#!/bin/bash -l

#$ -P cs440
#$ -l h_rt=12:00:00
#$ -N risk
#$ -M torrese9@bu.edu
#$ -pe omp 8
#$ -l mem_per_core=8G
#$ -j y
#$ -o /projectnb/cs440/students/torrese9/CS440/risk_models

module load java/21.0.4
```

### Instructors’ answer (Collin Barber — TA)

**“What do you expect that script, as written, to do? What commands are being run?”**—there were **no** compile/run lines.

After the student pasted intended workload:

```bash
javac -cp "./lib/*:." @risk.srcs
java -cp "./lib/*:./src" edu.bu.pas.risk.SequentialTrain -x 200000 -t 20 -v 10 pas.risk.agent.RiskQAgent pas.risk.agent.CurriculumWrapperAgent | tee my_logfile.log
```

Collin reiterated: the **submitted** script still showed **only** headers + **`module load`**—**no** **`javac` / `java`**.

### Updated script (student — still short runtime at first)

After edits (paths normalized to **`.:lib/*`** style in capture):

```bash
#!/bin/bash -l
#$ -P cs440
#$ -l h_rt=12:00:00
#$ -N risk
#$ -M torrese9@bu.edu
#$ -pe omp 8
#$ -l mem_per_core=8G
#$ -j y
#$ -o /projectnb/cs440/students/torrese9/CS440/risk_models

module load java/21.0.4

javac -cp ".:lib/*:." @risk.srcs
java -cp ".:lib/*:./src" edu.bu.pas.risk.SequentialTrain -x 200000 -t 20 -v 10 pas.risk.agent.RiskQAgent pas.risk.agent.CurriculumWrapperAgent | tee my_logfile.log
```

### Follow-up (Selene Wu — TA)

Given **`#$ -o …`**, **Grid Engine** should write the batch **stdout/stderr** to that **log path**—**read that file first** when a job “ends instantly”; it usually contains the real **`javac`**/**classpath**/**cwd** error.

### Resolution (Student)

**Training succeeded** after debugging with the above.

**Takeaway:** A **`qsub`** script must **`cd`** into the project (if needed), reference **`@risk.srcs` / `lib/`** from the **right working directory**, and actually invoke **`javac` + `java`** (or a driver script). **Instant exit** almost always means **read the `-o` log** (or **`$JOB_NAME.o$JOB_ID`**) before blaming the learner code.

---

## Thread 049 — @497 — How can we let the agent we trained fight with itself?

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~3 days ago at capture  
**Views (capture):** 84  
**Follow-up count shown:** 0 (not visible)

### Question (Student)

How can the **trained `RiskQAgent`** play **against a copy of itself**? Is that a **good practice in RL**?

### Instructors’ answer (Andrew Wood — professor)

See **@476** for a concrete pattern: write a **small wrapper class** around **`RiskQAgent`** and supply that as the **opponent** (“enemy”) implementation.

**Takeaway:** **Self-play** is implemented by **instantiating two policies** (often the **same codebase** with **different seeds / IDs / checkpoint paths**), not by magic in the trainer—mirror staff’s **wrapper + loaded weights** idea (**Thread 045 — @476**). Whether **self-play** is “good practice” depends on **non-stationarity** and **exploitation loops**; for this assignment, follow **course / curriculum** guidance first.

---

## Thread 050 — @501 — What command to run to load an infile?

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** at capture (recent)  
**Views (capture):** 82  
**Follow-up count shown:** 0

### Question (Student)

They run **`SequentialTrain`** with **`--inFile`** and get **`Failed to load params. File does not exist`**. What is the correct invocation?

Command as posted:

```bash
java -cp "./lib/*:." edu.bu.pas.risk.SequentialTrain pas.risk.agent.RiskQAgent static edu.bu.pas.risk.agent.ff.AggroAgent -x 200 -t 140 -v 10 --inFile ./src/pas/risk/agents/params.model -o params/qFunction --outOffset 0 -a 5000 -p 2500 -g 0.99 -n 0.001 > 0pOutput.txt 2>&1
```

### Instructors’ answer (Andrew Wood — professor)

Does **`./src/pas/risk/agents/params.model`** actually exist **on disk**, **relative to the shell’s current working directory**, at the moment you launch **`java`**?

**Takeaway:** **`--inFile`** is a **filesystem path**, not a classpath resource—**`cd`** to the repo root (or pass an **absolute path**) and confirm the **`.model`** file lives where you think it does. Same class of bug as **Gradescope “params won’t load”** threads: **path drift** between environments.

---

## Thread 051 — @502 — Which of the 3 reward functions?

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~2 days ago at capture  
**Follow-up count shown:** ≥1

### Question (Student)

How does the framework know which **reward function** implementation to use? If they implement **`getFullTransitionReward`**, must they **comment out** the other hooks, or **call** something explicitly? Unsure whether **`My*RewardFunction`** is actually wired in.

### Students’ answer (Selene Wu — TA)

It depends what you configure in the **reward class constructor** (how you select / parameterize the behavior).

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

### Instructors’ answer (Andrew Wood — professor)

The **constructor** of your reward class sets an **enum** (or similar selector). **Course framework code** reads that **enum** and dispatches to the right reward API (e.g. which override is authoritative for a transition).

### Follow-up (Student)

They paste a **`createActionReward`** body that **does not construct** a normal **`MyActionRewardFunction`**—it looks like a confused mix of **`new MyActionRewardFunction(...)`** with a **`getFullTransitionReward(...)`** call jammed inside (and is **not valid Java** as written):

```java
public RewardFunction<Action> createActionReward()
{
    return new MyActionRewardFunction(this.agentId().getFullTransitionReward(state, action, nextState);
}
```

### Follow-up (Collin Barber — TA)

**“That is not a constructor.”**—go back to the **starter template**: **`new MyActionRewardFunction(...)`** should pass whatever **constructor arguments** the class defines (often including the **enum / mode** Andrew described), not arbitrary **transition** tuples at factory time.

**Takeaway:** **Selection is declarative** via **constructor-time enum**, not by **comment roulette**; **`createActionReward` / `createPlacementReward`** should return a **fresh reward object** built like the **reference skeleton**.

---

## Thread 052 — @503 — Training cycles + opponent mix

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~2 days ago at capture  
**Follow-up count shown:** 3

### Question (Student)

With the **default starter network**, **`AggroAgent` + `TurtleAgent`**, they **plateau at ~0% wins** after **~60 cycles**. They ask:

1. Which **opponents** have worked for others?  
2. How many **cycles** before **consistent** wins?  
3. Does **opponent mix** matter a lot?  
4. Anyone try **bigger win bonuses** (e.g. **+200**)—help or hurt?

### Students’ answer (Selene Wu — TA)

- **Custom `AggroRandomAgent` (1v1):** like **`RandomAgent`** but **attacks more** and **reinforces frontiers**—more likely to **terminate** games once ahead, reducing **infinite training stalls**.  
- **Skill gate:** if exploration already yields **~20%** wins vs that bot, expect useful learning after **a few dozen** cycles; run **several parallel seeds** because **variance** is high.  
- **Reward centering / sparsity:** they had trouble when **“decent default play”** still looked **strongly positive**. They moved to **≈0 or mildly negative** rewards in **neutral / ~split-map** situations and **positive** rewards only when **clearly winning**—helped **average return** correlate with **actual wins** instead of **milking mediocrity**.

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

### Follow-up (Student)

After **“moving the center”** of their shaping (baseline reward level), the agent went from **no learning signal** to **consistent wins**—they suggest the **plateau utility** should line up with what **winning** means numerically.

### Follow-up (Student)

They train vs custom **`AggressiveRisk`** (hard) and **`RandomFighter`** (medium) with **`+500` win reward** to make **winning** dominate **per-turn farming**. Even after **~1400 battles** and **100 eval games (1v1)** they still had **0 eval wins** mid-run (post-bug **restart from scratch**, **~6 h** training)—may need **more wall-clock**; hypothesizes **2p** builds **1v1** habits while **3p+** adds strategic diversity.

### Follow-up (Selene Wu — TA)

You generally need to **penalize stalling / “do nothing profitable”** so the **optimal policy is to finish**, not **park in a won-on-paper state**. With **discounted returns**, a useful sanity inequality is: **`R_win` (appropriately scaled through the terminal transition)** should exceed the **supremum** of what the agent can **extract by stalling forever** under your per-step shaping—otherwise **γ-discounted accumulation** makes **never ending** look best (ties to **Thread 047 — @494** / **Thread 043 — @473**).

**Takeaway:** **Opponents that end games** + **rewards that do not pay for stalemates** beat **slow pacifist teachers**; **big terminal bonuses** can help **only if** mundane steps are not accidentally **more attractive** than **closure**.

---

## Thread 053 — @510 — PA3 consistent issue

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~1 day ago at capture  
**Views (capture):** 67  
**Follow-up count shown:** 0

### Question (Student)

Training / play consistently ends with **elimination** → replacement by **`StaticAgent`**. Example log (abridged):

```text
Agent 1 was required to trade-in, but instead tried to:
AttackAction[agentId=1, from=Pakistan, to=Ukraine, attackingArmies=2, movingArmies=4,
 hasAttacked=false forceTradeIn=false actionCounter=0 inventory.size()=5
 => canRedeemCards=true]
… thus has been eliminated and replaced with a StaticAgent.
```

### Instructors’ answer (Andrew Wood — professor)

Likely the **starter “exploration” logic for the redeem phase**: with **≥ 5 cards**, the rules **require a redeem**—**`NoAction` / skipping redeem** is **illegal**. The **default exploration** path he shipped **does not honor** that constraint.

**Takeaway:** Override **`shouldExplore*`** / redeem exploration so you **never** propose **non-redeem** moves when **forced trade-in** applies—same rules family as **forced redemption after knockouts** (**Thread 041 — @469**).

---

## Thread 054 — @512 — SCC queue times + parallel runs

**Post type:** Question (resolved branches in capture)  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~1 day ago at capture  
**Views (capture):** 75  
**Follow-up count shown:** several

### Question (Student)

**SCC / SGE:** jobs sit in **`qw`** for **3+ hours** with **~5500** jobs visible in the aggregate queue. Is that **normal end-of-semester** load vs a bad **`qsub`** script? Are people launching **many parallel trainers**—and does heavy queue depth make that **pointless**?

### Students’ answer (Selene Wu — TA)

They personally seldom wait long and ask for the poster’s **exact resource ask**: **cores**, **`mem_per_core`**, **hard runtime**.

Selene’s typical reservation: **`#$ -pe omp 16`**, **`8G` per core**, **`12:00:00`** wall clock—usually **< ~10 min** queue in their experience.

### Follow-up (Student) — resolved

Their ask was roughly **`72:00:00`**, **`#$ -pe omp 4`**, **no explicit memory line**.

### Follow-up (Selene Wu — TA)

**72 h** is **enormous** for this workload—**learning curves** usually **flatten well before** the course’s common **12 h**-class requests. **Shorter wall times** queue **faster** under many fair-share policies.

### Follow-up (Student)

Does **`16` cores** make **`SequentialTrain` cycles/minute** faster, or only **scheduling**?

### Follow-up (Selene Wu — TA) — endorsed by Andrew Wood

- The **Java trainer is effectively single-threaded**—extra cores **do not speed RL updates**.  
- A common reason students request **`omp 16`** anyway is to unlock **large RAM** (order **~128 GB** in the configuration described—**verify current SCC docs**).  
- **`16` slots** can also be the **minimum slice that pins a whole node**, reducing the chance a **noisy neighbor** spikes RAM and **OOM-kills** your JVM.

### Follow-up (Student)

Someone else still **queued since morning**.

### Follow-up (Collin Barber — TA)

Cluster waits can **stretch to a day or two** at bad times; **GPU** pools are especially **quota-throttled**. Advises against **multi-day single seeds** this late—prefer **breadth** (more experiments / checkpoints) over **one heroic 72 h run**.

**Takeaway:** **`qw` pain** usually tracks **how greedy your `-l h_rt` / PE / RAM** request is vs **cluster policy**; **Risk training** does not **parallelize across those cores**—buy cores for **RAM + isolation**, not **FLOPs**, and **shard work** across shorter jobs.

---

## Thread 055 — @514 — Placement details

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** at capture (recent)  
**Follow-up count shown:** 0 (not visible)

### Question (Student)

Does **placement** put **one army per decision** or **dump all `remainingArmies` in one shot**? **`IAgent` / `RiskListener`** look **single-step**. For **placement sensor / placement reward**, should features assume **exactly one** troop will land, or can a transition place **the whole remainder**?

### Students’ answer (Student — author not visible in capture)

**Always one at a time.** The engine queries your model for a **value / score over every legal territory**, places **one** army in the **argmax** territory, then **loops** until **`remainingArmies` hits zero**.

**Endorsement:** Andrew Wood (professor) endorsed this answer.

**Takeaway:** Shape **placement tensors** as **per-single-placement decisions**, not batched “allocate all reinforcements in one forward pass.” *(Setup vs post-setup phase detection is still separate—**Thread 044 — @475**.)*

---

## Thread 056 — @515 — Placement and FortifyAction deterministic?

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~23 hours ago at capture  
**Views (capture):** 66  
**Follow-up count shown:** 0 (not visible)

### Question (Student)

**Placement** and **fortify** feel **fully deterministic**—no **dice** or RNG. They had a parallel question about **troop counts from card redemption**, but assume the engine exposes a deterministic **`redemptionAmount`** (or equivalent) even if the **deck sequence** is stochastic elsewhere.

### Students’ answer (Selene Wu — TA)

**Yes—placement and fortify are deterministic** in this codebase. The **only stochastic player action** is **attacking** (dice-driven).

**Endorsement:** Andrew Wood (professor) endorsed Selene’s answer.

**Takeaway:** Do not inject fake noise into **placement / fortify sensors**; reserve **expectations of randomness** for **battle outcomes** (and any **explicit** random processes the spec names).

---

## Thread 057 — @516 — agent stuck at 0 wins after 116 cycles

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~14 h / ~9 h offsets at capture  
**Follow-up count shown:** ≥1 (incl. one **“same”** resolved)

### Question (Student)

Two **~12 h** parallel **`SequentialTrain`** runs:

1. **`AggroAgent` curriculum:** reached **116 cycles**; logged **utility ~200 → ~330** but **`avg(wins)=0`** across eval.  
2. **`RandomAgent` + `StaticAgent`:** appeared **hung**—only **cycle 0** checkpoint, logs stalled after printing the **agent lineup** → suspected **non-terminating** games because opponents were **too passive**.

**Shaping (their numbers):** ~**0** baseline, **+500** win, **−200** elimination, **−0.5** per-step living penalty.

They ask: (a) how to run **passive** mixes without **infinite episodes**, and (b) whether **rising utility + zero wins** means **bad rewards** vs simply **need more cycles**.

### Students’ answer (Selene Wu — TA)

1. For **passive teachers**, often need a **hand-crafted policy** that **switches to aggression** after **N turns** (or similar) so someone **forces closure**.  
2. **Utility climbs then saturates at 0 wins** → classic **local optimum / reward hacking**: the net found **non-terminal score farming** that beats **risking a loss** under current weights—**re-audit what is accidentally attractive** vs **what a win costs in expectation**.  
3. **Watch replays / telemetry** to see *how* points accrue without victories, then **re-center** penalties / terminal bonuses.

**Endorsement:** Collin Barber (TA) endorsed Selene’s answer.

### Instructors’ answer (Collin Barber — TA)

This pain is exactly why a **curriculum-style progression** (easier opponents → harder / more decisive mixes) is **recommended**—it avoids both **stall worlds** and **misaligned return plateaus**.

### Follow-up (Student)

**“Same”** (seconded the issue).

**Takeaway:** Passive-only lineups need **explicit termination / escalation** (**Thread 047 — @494**, **Thread 052 — @503**); **monotonic utility without wins** is almost always **shaping / local max**, not “just train longer.”

---

## Thread 058 — @492 — Losing Easy Autograder but Winning Hard Autograder

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~13 h / ~9 h offsets at capture  
**Follow-up count shown:** 1 unresolved (at capture)  
**Piazza id note:** Header **`@…`** not visible in the provided crop—**`@492`** matches the **pas/pa3** sidebar listing from related captures; **confirm in Piazza**.

### Question (Student)

They **score on medium + hard** Gradescope autograders but **0 on easy**, which feels backwards—are others seeing this?

### Students’ answer (Selene Wu — TA)

Try to **reproduce the exact opponent roster / seeds locally** (same matchup mix as each autograder tier).

### Instructors’ answer (Andrew Wood — professor)

The **graded opponents are not identical playstyles** across tiers—it is plausible your policy is **weak vs `AggroAgent` (easy track)** while still **doing fine vs the medium bot** (and whatever “hard” maps to that week).

### Follow-up (Student)

Seconded: **fails easy, passes medium** for them too.

**Takeaway:** **Autograder difficulty labels ≠ monotonic strategy difficulty** for your specific net—**diagnose per-opponent**, not by label. *(Compare **Thread 056 — @515** / **Thread 055 — @514** for mechanics certainty vs **opponent policy** variance.)*

---

## Thread 059 — @493 — Recommended curriculum order for training opponents?

**Post type:** Question  
**Piazza tags (visible):** `pas`, `pas/pa3`  
**Timeline (from UI):** ~5 h / ~3 h offsets at capture  
**Follow-up count shown:** not visible  
**Piazza id note:** **`@493`** inferred from **pas/pa3** sidebar ordering in related captures—**confirm in Piazza** (header not in crop).

### Question (Student)

**Curriculum learning** with **stock** agents:

- **`StaticAgent` only / `RandomAgent` only:** games **drag or never finish** (no one forces closure).  
- **`AggroAgent` from cold start:** **0 wins** for a long time → **weak learning signal** even after **100+** cycles.  
- **`RandomAgent` + `StaticAgent`:** hits an **`FSMAgent` error loop** (they tie it to the **forced-redeem / framework** family from earlier Piazza).

They ask:

1. Is a **custom training opponent** basically required, or is there a known-good **stock-only** curriculum?  
2. If **custom** is expected, what **progression** do staff suggest (e.g. **custom-easy → `AggroAgent` → `TurtleAgent`**)? Is **`AggroAgent` 1v1** enough prep for the **easy autograder** tier?  
3. **Warm-start** pitfalls when chaining runs with **`SequentialTrain -i` / `--inFile`**—**catastrophic forgetting** when swapping opponent mixes?

### Students’ answer (Student)

“In **lab**, the **TA** said to use a **trainer custom class**.”

**Takeaway:** Course staff steer students toward **authoring a small curriculum wrapper** (not relying on naïve **`Random`+`Static`** or **pure `Aggro` cold start**)—pair with **Thread 057 — @516** / **Thread 054 — @512** (queue + training breadth) and **Thread 010 — @388** for **FSM / forced redeem** stability when mixing bots.

---

## Thread 060 — @435 — QSUB NOTES (SCC onboarding)

**Post type:** Pinned **Note** (staff)  
**Piazza tags (visible):** `logistics`, `pas`, `pas/pa3`, `las`, …  
**Author:** Collin Barber — TA  
**Timeline (from UI):** pinned **4/17/26**; “every semester” **newbie-oriented** SCC primer  
**Views (capture):** 144+ (varies by scroll)

High-level archive of the long Piazza note—**always defer to current [RCS / SCC docs](https://rcs.bu.edu/)** if anything drifts.

### SSH login

- From a terminal: **`ssh <kerberos>@scc1.bu.edu`** (example in post: `ssh nerd@scc1.bu.edu` → substitute your **BU Kerberos** name).  
- Use your **BU Kerberos password**, not your laptop password.  
- Network: prefer **eduroam** on campus; from home use **BU VPN**. Disable **consumer VPNs** that hijack routes.  
- After login, banners link **Acceptable Use** and **[SCC system usage](https://www.bu.edu/tech/support/research/system-usage/)**; support **`help@scc.bu.edu`**.

### Shell literacy

- Basics named: **`pwd`**, **`cd`**, **`ls`**, **`tree`**.  
- Post nudges web search for “hidden files”, copy, create, edit—**learn GNU coreutils**, not ad-hoc LLM guesses, for muscle memory.

### CS 440 project location on SCC

```bash
cd /projectnb/cs440/students
cd <your_kerberos_name>   # then into your project folder (often literally cs440/)
```

### Getting files onto the cluster

- Expectation: **sync a copy** of the project into your **`/projectnb/cs440/students/...`** tree rather than treating the login node as the sole source of truth.  
- **Git:** [git-scm.com](https://git-scm.com/) — clone / pull on SCC, or push to a remote you then pull from on SCC.  
- **`rsync` (from your laptop, not inside an SCC session):** from the project root,

```bash
rsync -r ./ <kerberos>@scc1.bu.edu:/projectnb/cs440/students/<kerberos>/<target_folder>/
```

Kerberos password at prompt. Trim **stale `.jar`s / huge artifacts** if sync is slow.

### Deleting files safely

- **`rm -rf /`** is disabled.  
- To wipe **contents of cwd**: **`rm -rf *`** (still dangerous—know your **`pwd`** first).

### Why `qsub`

Login nodes have **tight CPU/RAM/time** limits—**long `java SequentialTrain` runs belong in batch jobs**, not interactive shells on the head node.

### Minimal `run.sh` skeleton (from note; swap Python for Java on PA3)

```bash
#!/bin/bash
#$ -l h_rt=24:00:00
#$ -pe omp 1
# Replace with your PA3 compile + java -cp ... SequentialTrain ... lines
python3 task.py
```

Workflow: **`touch run.sh`**, edit (**`nano run.sh`**), then **`qsub run.sh`**. Check **`qstat -u <kerberos>`**; cancel **`qdel <jobid>`**. Keep a canonical **`run.sh` in your repo root** so `rsync`/`git` always ships the version you think.

### Parallel “spray and pray” training

If you launch **many concurrent JVM trainers**, request **enough cores / jobs** honestly—overloading a single slot with 10 processes is unfair and brittle.

### Follow-ups (humor + tooling)

- Student asked ChatGPT to summarize the thread → **Collin** joked about **Subway Surfers** recap videos (**Andrew** endorsed the banter).  
- **Collin** (endorsed **Aakash Kumar**): after a job finishes, **`qacct -j <job_id>`** dumps **scheduler accounting / exit metadata**—useful when **`qsub` exits instantly** or you need **walltime / failed node** hints.

**Takeaway:** **SCC = SSH → project dir → sync → `qsub` script that actually runs your train command → read logs / `qacct`.** Pair with **Thread 048 — @496** (forgotten `java` lines) and **Thread 054 — @512** (queue math).

---

## Personal workflow — Sun Cho

Use this as a **short checklist** alongside the threads above.

- **Shell:** do everything from a **terminal** (macOS Terminal is fine).  
- **Identity:** BU Kerberos **`suncho`** → **`ssh suncho@scc1.bu.edu`**.  
- **Auth / network:** Kerberos password; **eduroam** on campus or **BU VPN** off campus; avoid random **VPNs** that block SCC routes.  
- **Where to work on SCC:** **`cd /projectnb/cs440/students/suncho`** then into your course directory (e.g. **`cs440`**).  
- **Code on cluster:** you plan to use **`git`**. Minimal loop once a remote exists: **(laptop)** `git add -A && git commit -m "…" && git push` → **(SCC, inside clone)** `git pull` → then **`qsub`**. **One-time on SCC:** `git clone <your-repo-URL>` into something like `~/cs440-src` or under `/projectnb/cs440/students/suncho/…`, then always **`cd`** there before **`git pull`**. If large generated artifacts should not live in git, use **`.gitignore`** and/or **`rsync`** for those only (**Thread 060**).  
- **Jobs:** maintain a **`run.sh`** with real **`javac` …`** and **`java -cp … SequentialTrain …`** (**Thread 060**, **Thread 048 — @496**); submit with **`qsub`**, watch **`qstat -u suncho`**, kill mistakes with **`qdel`**, post-mortem weird finishes with **`qacct -j <job_id>`**.  
- **Queue discipline:** shorter **`-l h_rt`**, honest **`-pe omp`**, and **RAM** requests per **Thread 054 — @512**—extra cores are mostly for **memory / isolation**, not faster single-process RL.

---

*End of current Piazza archive pass — add new threads above **Personal workflow — Sun Cho** with the next `## Thread …` block.*
