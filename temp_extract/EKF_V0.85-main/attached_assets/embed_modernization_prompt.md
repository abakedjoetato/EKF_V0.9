# AI TASK PROTOCOL — EMBED MODERNIZATION & THEMING

## OBJECTIVE

Audit and upgrade all Discord embeds to align with modern design standards, utilizing JDA 5.x features. Ensure consistency with Deadside’s post-apocalyptic theme, incorporating emerald color accents. Properly handle logos with transparent backgrounds to prevent Discord from adding unintended base colors. All embeds must include the footer:

> Powered By Discord.gg/EmeraldServers

Avoid the use of code blocks and emojis in all embeds.

---

## PHASE 1 — EMBED DESIGN STANDARDIZATION

- **Color Scheme**:
  - Utilize Deadside’s muted, desaturated tones:
    - Dark grays and blacks for backgrounds
    - Emerald green (#50C878) for highlights
    - Accents of rust and steel blue
- **Typography**:
  - Use bold titles for emphasis
  - Maintain concise descriptions
- **Layout**:
  - Structure embeds with clear sections: title, description, fields, and footer
  - Ensure readability and visual hierarchy

---

## PHASE 2 — LOGO INTEGRATION

- **Transparent Backgrounds**:
  - Ensure all logos have transparent backgrounds to prevent Discord from adding base colors
- **Placement**:
  - Use `EmbedBuilder.setThumbnail()` for logos
  - Ensure logos are relevant to the embed content
- **File Handling**:
  - When using local images, reference them with `attachment://filename.ext`
  - Attach the image file when sending the embed

---

## PHASE 3 — EMBED STRUCTURE ENHANCEMENTS

- **Fields**:
  - Use `EmbedBuilder.addField()` to organize information
  - Limit each embed to a maximum of 25 fields
- **Timestamps**:
  - Include timestamps with `EmbedBuilder.setTimestamp()` for time-sensitive information
- **Footers**:
  - Standardize footers across all embeds with `EmbedBuilder.setFooter("Powered By Discord.gg/EmeraldServers")`

---

## PHASE 4 — COMPLIANCE AND VALIDATION

- **Consistency**:
  - Ensure all embeds adhere to the established design standards
- **Accessibility**:
  - Verify color contrasts meet accessibility guidelines
- **Testing**:
  - Test embeds across different devices and Discord themes (light and dark modes)

---

## PHASE 5 — KILLFEED NORMALIZATION & EMBED DIFFERENTIATION

**Objective**: Normalize special kill types and separate their visual presentation to improve clarity and immersion.

### Suicide Normalization

- Identify all occurrences of `Suicide_by_relocation` in killfeed logs or CSVs.
- Normalize the name to display as:
  ```
  "Menu Suicide"
  ```

- Ensure this new name appears:
  - In all embeds
  - In any stat-tracking or killfeed announcements

### Falling Deaths & Suicide Embeds

- Separate suicides and falling deaths from standard killfeed messages.
- Generate **distinct embed templates** for:
  1. **Standard Kill** (Killer → Victim, with weapon, distance, etc.)
  2. **Suicide** (e.g., Player killed self or Menu Suicide)
  3. **Falling Death** (no killer, only victim)

- Each template must:
  - Use a unique title (e.g., `"Player Died"` for suicides, `"Fell to Their Death"` for fall damage)
  - Retain Deadside theme and emerald styling
  - Include time, player name, and any available context (e.g., height, if logged)

### Example Phrasing:
- **Menu Suicide**:
  - `PlayerName returned to the void (Menu Suicide)`
- **Falling**:
  - `PlayerName fell from a great height`

All embeds for these events should:
- Follow the modern visual guidelines from PHASE 1
- Include the standard footer
- Use distinct thumbnail icons to differentiate event types

---

## VALIDATION REQUIREMENTS

For each updated embed:

- [ ] Embed uses Deadside-themed color palette with emerald accents
- [ ] Logo is correctly integrated with a transparent background
- [ ] Structure includes title, description, fields, and standardized footer
- [ ] No use of code blocks or emojis
- [ ] Embed passes accessibility and device compatibility tests
- [ ] Suicide and fall events generate their own styled embeds
- [ ] Normalized event types appear correctly in killfeed

Print validation results:
```
[✓] Embed styled correctly
[✓] Menu Suicide normalized
[✓] Falling death differentiated
[✓] Logo displayed with no background error
```

If any issue cannot be confirmed:
```
FIX UNCONFIRMED — ESCALATION REQUIRED
```


---

## PHASE 6 — ADVANCED EMBED VISUAL DESIGN (KILLFEED & LEADERBOARDS)

### Objective
Evolve all critical embeds—especially killfeed and leaderboards—into premium-grade, immersive UI experiences. These embeds must deliver cinematic, highly-informative feedback that reflects the intense PvP-driven and stat-centric environment of Deadside.

### 1. Killfeed Enhancements

- Use dynamic titles based on kill type or streak
- Multi-field layout with:
  - Killer vs Victim profiles
  - Weapon used, distance, time
  - Optional: map/environment context
- Highlight special conditions:
  - Bountied targets (gold accents, bounty icon)
  - Suicides, Falling deaths (dedicated visual variants)
- Animate embed appearance using layout spacing and pseudo-borders

### 2. Leaderboard Embeds

- Paginate if >10 entries using interactive components
- Use rank icons for top 3
- Structured fields:
  - Rank + Name
  - Kills / Deaths / K/D
  - Weapon / Faction / Guild
- Style:
  - Emerald gradients by rank
  - Deadside-themed titles
  - Optional avatars or profile images

### 3. Innovation Rules

- Every embed must visually “tell a story”
- Dynamic formatting (e.g., K/D thresholds)
- Balanced spacing and alignment
- Avoid clutter; maximize impact

---

## PHASE 7 — DYNAMIC TITLES & THEMATIC MESSAGING

### Objective
Introduce varied, lore-rich titles and contextual messages for all embeds to prevent repetition and enhance immersion.

### 1. Dynamic Titles by Embed Type

**Killfeed (PvP)**:
- "Elimination Confirmed"
- "No Survivors"
- "Precision Eliminated"

**Bounty Kills**:
- "Contract Fulfilled"
- "Marked for Death"

**Suicide**:
- "Self-Termination Logged"
- "Silent Exit"

**Falling Deaths**:
- "Terminal Velocity"
- "Gravity Claimed Another"

**Leaderboards**:
- "Apex Predators"
- "Top Hunters of the Week"
- "Factions at War"

### 2. Description Messaging

**Killfeed Examples**:
- "{killer} removed {victim} at {distance}m with {weapon}"
- "Clean shot. No time to react."

**Leaderboard Examples**:
- "Survivors ranked by raw efficiency:"
- "The strongest endure. Here’s who leads:"

**Suicide/Fall Examples**:
- "Sometimes the biggest threat is yourself."
- "They didn’t fall. They descended."

### 3. Implementation Notes

- Use `getRandomTitle(type)` and `getRandomDescription(type, context)`
- Prevent repetition of lines
- Language must match Deadside tone—no casual or whimsical phrases

---

## VALIDATION FOR PHASE 6 & 7

- No static or repeated titles within same event type
- Description lines rotate with context awareness
- Formatting adapts to kill types, bounty status, faction tags
- All embeds remain legible, mobile-safe, and aesthetic

Print validation:
```
[✓] Killfeed dynamically styled and titled
[✓] Suicide/Fall embeds visually distinct
[✓] Leaderboards ranked and themed
[✓] Randomized messages in full effect
```

