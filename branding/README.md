# Easy AI Commit logo assets

The selected design uses a symmetric white generation mark on a medium sky-blue to lavender-purple gradient. Its thick mirrored geometry and central spark remain recognizable in the 16×16 Commit toolbar slot.

- `easy-ai-commit-logo.png` is the original 1254×1254 selected artwork.
- `../src/main/resources/icons/easyAiCommit.svg` is the 16×16 toolbar icon with a 14×14 visible mark.
- `../src/main/resources/META-INF/pluginIcon.svg` is the full-size 40×40 logo used by the plugin manager.

Both runtime SVGs use the same traced foreground path and the same fixed `#4FADE3 → #7187E2 → #9668D9` gradient. `../tools/render_logo_assets.m` regenerates both assets from the selected source artwork so their colors and geometry cannot drift apart.
