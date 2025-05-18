

---

## PHASE 8 — LOGO INTEGRATION: RIGHT-SIDE THUMBNAILS WITH TRANSPARENCY

### Objective
Ensure all embeds incorporate relevant logos or insignias using Discord’s right-side thumbnail system to avoid background color injection and preserve transparency.

### Technical Standard

- **EmbedBuilder Method**: Use `setThumbnail(String url)` or `setThumbnail("attachment://filename.png")`
- **Rendering Behavior**:
  - The thumbnail appears **right-aligned** in the embed
  - When the image has a **transparent background**, Discord will **not apply a background color** if the image is hosted or embedded correctly
  - The overlay that Discord sometimes adds to transparent PNGs is avoided when using thumbnails rather than `setImage()` or avatar previews

### Implementation Requirements

- All embeds must call:
  ```java
  embed.setThumbnail("attachment://deadside_logo.png");
  ```
  or
  ```java
  embed.setThumbnail("https://your.cdn/transparent_logo.png");
  ```
  depending on how the assets are stored

- **Image Format**: PNG or WebP with alpha channel
- **File Scope**:
  - Use server-specific or context-specific logos where appropriate (e.g., faction emblems, bounty mark)
  - Always ensure the visual fits within thumbnail proportions (80x80 recommended)

### Validation

- Image should render on the **right-hand side** of all embeds
- No visible square background (Discord does not auto-background thumbnails)
- Logo transparency must render cleanly against all Discord theme backgrounds (light/dark)
- Test image by uploading manually if needed and attaching in embed call

Print visual validation results:
```
[✓] Logo renders cleanly on right side
[✓] Transparency respected (no base fill)
[✓] Embed remains balanced and visually striking
```

