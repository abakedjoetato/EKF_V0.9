# AI TASK PROTOCOL — CSV & LOG PARSING SYSTEM VALIDATION

## OBJECTIVE
Validate and correct core functionality across the automated and historical CSV parsers, log processing system, and multi-channel output logic. Your job is to ensure that the system parses files in the correct order, accurately detects the correct log paths, separates responsibilities across Discord channels, and resets memory as expected.

Each fix must be validated by test input, with observable confirmation of functionality. Compilation or presence of code is not sufficient.

---

## PHASE 1 — AUTOMATED CSV PARSER BEHAVIOR

- Confirm that the scheduled CSV parser:
  - Only reads the **newest available `.csv` file**
  - Parses **only new lines** since the last recorded line index
- Verify that:
  - It does **not** reparse old files
  - It does **not** miss new files due to name-based filtering errors
  - It correctly records the last parsed line position in persistent storage

**Fix and test** by simulating multiple `.csv` files with sequential timestamps and validating parsed output logs.

---

## PHASE 2 — HISTORICAL CSV PARSER BEHAVIOR

- Confirm that the historical parser:
  - Scans and **parses all `.csv` files**, starting from the **oldest to newest**
  - Clears all existing **killfeed/stat entries** from that server before parsing begins
  - Resets last-line memory so that automated parser starts fresh from the newest parsed file
- Fix any missing purge logic or failure to overwrite last line data
- Validate by:
  - Seeding multiple `.csv` files (e.g., 2023-01.csv, 2023-02.csv, ...)
  - Running historical command
  - Verifying ordered insertion and memory reset

---

## PHASE 3 — DEADSID.LOG DETECTION VALIDATION

- Ensure the log processor is:
  - Properly locating the latest **Deadside.log** file
  - Navigating to `./{Host}_{Server}/Logs/` as expected
  - Handling renamed/rolled log files correctly (file rotation awareness)
- Confirm:
  - Live `Deadside.log` is parsed every 5 minutes
  - No `.bak`, `.old`, or archived logs are picked as current
- Validate by simulating a log file switch or restart

---

## PHASE 4 — MULTI-CHANNEL LOG OUTPUT CORRECTION

- We previously separated log output into 3 categories:
  - Events
  - Player Connections/Disconnections
  - Online Player Count

### Problem:
- Only one command currently exists to set a unified output channel
- The separate channel set commands (e.g., `/setlog:events`, `/setlog:connections`) are **missing**

### Fix:
- Reintroduce commands to independently assign each category to a specific channel
- Confirm that:
  - Each event type is routed to the correct channel
  - Server config is updated and saved
  - Embed formatting remains consistent and Deadside-themed

Test each command individually. Validate Discord output routes to the correct channels.

---

## COMPLETION CRITERIA

For each phase:
- List each file modified
- Log input and output results (test data, observed bot output)
- Confirm that automated processes reflect updated logic
- Print validation footer like:

```plaintext
PHASE X — VALIDATED AND VERIFIED
[✓] Correct file read
[✓] Last-line memory respected
[✓] Channel commands restored and functional
```

If any task fails or cannot be confirmed visually/logically:
```plaintext
FIX UNCONFIRMED — ESCALATION REQUIRED
```
