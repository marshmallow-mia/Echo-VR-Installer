# sectionBox transparency refactoring

- Changed `sectionBox(int y, int h, int arc, int w)` → `sectionBox(int y, int h, int arc, int w, Color fill)`
- Removed `BOX_FILL` constant (was `new Color(200, 0, 150, 200)`), replaced with per-call-site colors:
  - Content+TipBox box: `new Color(200, 0, 150, 120)` — more transparent
  - Progress bar box: `new Color(200, 0, 150, 160)` — less transparent
- `BOX_BORDER` constant left untouched
- Both gradle compileJava and gradle test pass
