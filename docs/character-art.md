# Character artwork provenance

Built-in imagegen generated a 12-frame sprite sheet based on the user's supplied Q-style reference chart, panel 1. The source asset is `app/src/main/res/drawable-nodpi/anime_cat_sheet.png`.

Generation brief: the same wholesome, fully clothed two-head chibi cat-eared character in twelve poses; white/silver bobbed hair, blue eyes, black/pink bows, black jacket, white top, skirt over opaque shorts, stockings and shoes. Four columns and three rows contain paired idle, wave, jump, stretch, sleep and happy poses.

The final built-in edit prompt:
"Production game sprite sheet background replacement ONLY. Preserve the exact twelve chibi white-haired cat-eared girl poses, faces, blue eyes, white hair, black jackets, white shirts, skirts, bows, paws and tail, all artwork positions and the 4-column 3-row grid exactly. Replace EVERY gray/white checkerboard background pixel outside the characters with a perfectly uniform BRIGHT CHROMA GREEN #00FF00 background. Also green in gaps between arms/body/tail. Background must be FLAT solid pure RGB 0,255,0, with NO checkerboard, NO texture, NO gradient, NO shadows. Do not put green in character hair or clothing. Keep black clean outline around each cutout, no glow. This intentionally uses opaque green for a runtime game chroma-key renderer; do not attempt transparency and do not depict a transparency checkerboard. No labels or captions. Keep all twelve entire characters in the same grid cells."

The raw generated pixels are stored unchanged. Android's renderer prepares transparency once in memory, measures each pose, then scales and positions the sprite on a stable baseline. Robolectric tests render each action, check screen borders and background transparency, and export previews.
