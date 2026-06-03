# Status Bar Color Update

## TL;DR
Replace the reddish-purple status bar idle/pulse colors with dark slate-teal tones. Red is inappropriate for a guidance status indicator.

## Work Objectives
- Status bar idle: `Color(25, 45, 55, 255)` — dark slate-teal
- Pulse: oscillates between `(25,45,55)` and `(50,80,95)` — subtle brightness lift
- No red in any state colors

## TODOs

- [ ] 1. Update statusBarBox paintComponent color values in FrameGuidance.initWindow()
  - Idle: `new Color(25, 45, 55, 255)`
  - Pulse: `new Color((int)(25 + pulse * 25), (int)(45 + pulse * 35), (int)(55 + pulse * 40), 255)`
  - File: FrameGuidance.java, the custom JPanel paintComponent in initWindow
