# AI REPLIT SESSION PROMPT — FULL BOT INIT + CRITICAL FIX VALIDATION

## OBJECTIVE
You are launching a Replit session to initialize and repair a Deadside-themed Java Discord bot project uploaded as a `.zip` file. You must unzip the archive, move contents to the root directory, validate the structure, initialize the bot with secrets, and execute a controlled round of repair operations.

Your job is not to guess or assume functionality—your fixes must be based on proof, reproducibility, and validation after execution.

---

## STEP 0 — PROJECT INIT (REQUIRED)

1. Unzip the uploaded `.zip` file (found in attached assets)
2. Move all contents from the unzipped folder to the project root directory
3. Remove any empty folders or duplicates (if extracted in a subfolder like `project/`, `DeadsideBot/`)
4. Scan the directory and log:
   - Main class
   - Parser classes
   - Config files
   - Event/Command system files
   - Any duplicate or orphaned files
5. Detect `.env` or `config.properties`, or prepare one
6. Load secrets provided by Replit (BOT_TOKEN, MONGO_URI, etc.)
7. Start the bot using JDA 5.x with valid startup confirmation
   - Log must confirm Discord connection

Do not proceed if bot does not start or config cannot be validated.

---

## STRUCTURE FIXES (PHASE 1)

- Detect and eliminate:
  - Duplicate class files
  - Orphaned modules
  - Redundant logic that is no longer referenced
- Log every file removed or merged

Use dependency tracing to determine what can safely be eliminated.

---

## MONGODB FIX (PHASE 2)

- Confirm that MongoDB uses **MongoDB Atlas**, not local MongoDriver URI defaults
- Search for hardcoded references to localhost, `27017`, or unscoped collections
- Update any usage that does not support async Atlas connection
- Ensure all classes using DB access (e.g., `UserModel`, `StatModel`) are compatible

Log every class fixed and how compatibility was confirmed.

---

## OWNER ID INTEGRATION FIX (PHASE 3)

- Owner ID is: `462961235382763520`
- Ensure this ID is:
  - Hardcoded in config
  - Properly validated in any commands requiring admin/owner permission
- Fix any classes where the owner bypass is missing or broken

Test a restricted command to confirm owner is correctly elevated.

---

## COMMAND RESPONSE FIX (PHASE 4)

- Discord requires a **response or deferral within 3 seconds** of a command trigger
- Validate all slash or text commands to ensure:
  - `event.deferReply()` is called where needed
  - Async operations are properly handled without timeout
- Fix and test all long-running commands (e.g., CSV processing, database queries)

Log response timestamps and command behavior during test runs.

---

## CSV PARSER FIX (PHASE 5)

- Confirm that the CSV killfeed parser:
  - Executes every 300 seconds (5 minutes)
  - Correctly parses only new lines (no reprocessing or missing lines)
- If line matching fails:
  - Use attached `.csv` file as a validation source
  - Extract a representative line and confirm format compatibility
  - Fix delimiter issues, trimming, or newline normalization

Ensure parser stores last line index and updates correctly after each run.

---

## FINAL INTEGRITY CHECK

After all phases:
- Restart the bot and validate clean startup
- Execute at least 3 test commands from Discord
- Confirm that MongoDB, premium logic, and parser tasks are live and non-failing
- Log success state for each module:
  - `[OK] MongoDB`
  - `[OK] Owner Logic`
  - `[OK] Command Response Timing`
  - `[OK] Killfeed Parser`

If any failure persists, report:
`[FAIL] {Module}` and explain the source.

Do not return "Fix Applied" without observed success and output verification.



---

## PHASE 6 — BOUNTY SYSTEM IMPLEMENTATION

**Objective**: Add a bounty system where players can place bounties on other players, which pay out when a linked user kills the target. The killer must be linked; the target may be unlinked.

### Functional Requirements:

1. **Player-Set Bounties**
   - Command to place a bounty on a player by name
   - Bounty is stored in the database with:
     - Target name
     - Bounty amount
     - Timestamp (expires in 24 hours)
     - Guild/server ID
     - Issuer (who set the bounty)
   - Payout issued automatically to the first linked killer who kills the target within timeframe

2. **Admin Controls**
   - Commands to set min/max bounty amount
   - Validate bounty amounts fall within range

3. **AI-Generated Bounties**
   - Run once per hour
   - Analyze killfeed data for the last 60 minutes
   - If a player meets one of these thresholds: 3, 5, 7, or 10 kills
   - Randomly assign bounty amount scaled by difficulty:
     - 3 kills = easy
     - 5 kills = moderate
     - 7 kills = hard
     - 10+ = elite
   - Use min/max bounds to define amount
   - AI-generated bounties expire after 60 minutes

4. **Killfeed Integration**
   - On parsing a kill, check if the victim has an active bounty
   - If the killer is linked:
     - Award currency
     - Remove bounty
     - Announce payout in Discord embed (with emerald theme)

5. **Embed Format**
   - On bounty set: include issuer, target, amount, duration
   - On payout: include killer, target, amount
   - Emerald styled, right-aligned bounty icon

6. **Cleanup Tasks**
   - Remove expired bounties from database every 10 minutes
   - Auto-expire AI bounties at 60 minutes
   - Auto-expire player-set bounties at 24 hours

---

**Validation Requirements:**
- Command for setting bounty works as intended
- Admin bounds respected
- Auto-bounty logic triggers on schedule and scales payouts appropriately
- Killfeed correctly pays out for bounty kills and removes completed bounties
- Payouts only occur for linked killers
- Expired bounties are automatically cleaned

Log test outputs:
- Bounty created
- Bounty paid
- Bounty expired
