# File Xplorer Clear Studio v0.5 · editable design handoff

The `v0.2/` SVG references are retained for the existing tablet, submenu and media viewer layout. The v0.3 Figma importer adds the sketched phone overview and phone Browse in grid and list modes, for both light and dark themes, while preserving the tablet and viewer screens. Roboto is the product font; sample files, storage values and vector landscape are illustrative.

## Native Figma layers

The remote file https://www.figma.com/design/Kib2t3tMfhqztEAejCcfgT remains empty because account canvas quota blocked access again. Run the supplied local importer to populate it or another Design file:

1. In Figma Desktop, **Plugins → Development → New plugin → Figma design → Run once**. Create a local plugin folder and retain its generated plugin ID.
2. Copy `figma-import-clear-studio.js` into that folder. Copy `manifest-clear-studio.template.json` as `manifest.json`, replacing the placeholder ID with the generated ID.
3. Ensure Roboto Regular, Medium and Bold are available. Import the manifest under **Plugins → Development → Import plugin from manifest**, then run the plugin in your Design file.
4. It creates a new page with twenty screens, light/dark color modes, bound variables, reusable icon/button/file-row components, editable Roboto text, auto-layout controls and background blur effects. Only the discrete landscape artwork is imported as an SVG asset; complete UI screens are not flattened into SVG or images.

The importer has passed JavaScript syntax checks. It could not be executed or visually validated inside Figma due to the quota. The SVG/PNG references were rendered and visually inspected separately. Running the importer again creates another page; remove the previous imported page if you want one copy.

Regenerate vector references with `python3 tools/design_v2.py`, then regenerate the importer with `python3 tools/figma_export_v2.py`. The v0.1 designs remain for reference.

The selected concept is Clear Studio (C): warm neutrals, terracotta accent, clear cards, compact square browse tiles, and a 4-item dock. The importer includes overview, grid/list browsing, Search, Tools, Settings, file actions, tablet, and media viewer screens in light and dark modes.
