# ClearLaggEnhanced docs — source files

These Markdown files are a full rewrite of the ClearLaggEnhanced documentation, meant to replace
what's currently live at https://busybeedev.net/docs/clearlaggenhanced/intro. That live site was
found to be stale (missing the Packet Limiter and AFK Optimization modules entirely, among other
gaps) as of 2026-09-20.

No docs-site repo for busybeedev.net exists in this local workspace, so these were written here
instead. To publish: copy this `docs/` directory's contents into the busybeedev.net Docusaurus
repo under `docs/clearlaggenhanced/`, and use `_sidebar-reference.md` to wire up `sidebars.js` (or
per-folder `_category_.json` files, depending on how that site is configured).

Every page uses Docusaurus-style frontmatter (`id`, `title`, `sidebar_position`) since the live
site's URL pattern (`/docs/clearlaggenhanced/...`) and sidebar-category structure strongly suggest
Docusaurus. Adjust if the real site uses something else.

Every fact in these pages (config keys, defaults, commands, permissions) was read directly from
the plugin's source at the time of writing (ClearLaggEnhanced 26.9.2) rather than inferred — if the
plugin changes, these pages will drift and need a refresh the same way the old site did.

Deliberately **not** documented: WorldGuard integration, built-in mob stacking, Discord webhooks,
BossBar performance meter, per-world clear profiles, dry-run preview mode. Those are tracked in
`ROADMAP.md` at the project root as not-yet-built — do not add them to these docs until they ship.
