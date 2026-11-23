# OctoClient multi-version porting instructions

## Goal and scope

Port OctoClient incrementally across every Minecraft release from 1.8.9 through
26.2, prioritizing Minecraft 1.8.9 first. Keep each release in a clearly
isolated, version-specific source/build target so a port never breaks another
release.

Choose Fabric for a release when a compatible Fabric Loader is available.
Choose Forge only for releases without Fabric support. Record the chosen loader
and exact Minecraft/loader versions in that target's build configuration and
documentation.

## Compatibility requirements

- Preserve the existing dashboard, navigation, module catalog, settings,
  keybind behavior, HUD editor, cosmetics, and feature behavior across targets.
- Reuse the same names, defaults, ranges, and persistence semantics for settings.
  Use the closest supported rendering/API equivalent on older releases; do not
  silently drop or misrepresent a feature when an exact equivalent is
  impossible.
- Keep version-specific Minecraft/loader APIs inside that target. Share code
  only when it does not introduce cross-version API or behavior regressions.
- Treat the 1.8.9 Forge port as the first priority and close its feature gaps
  against the current 26.2 Fabric client before moving on to additional releases.
- Do not claim a release is supported until its artifact builds and the client
  has been launched for a smoke test. For UI changes, inspect a dashboard and
  HUD-editor capture at a known resolution and fix visible layout regressions.

## Required workflow

1. Inspect the current target, its upstream feature/settings behavior, and the
   existing build/test conventions before editing.
2. Make changes in small, complete increments. Keep feature parity and settings
   parity explicit; document unavoidable version-specific differences.
3. After every code change, build **every configured mod target**, not only the
   target being edited. From the repository root, use the JDK version required
   by each target:
   - Main Fabric target: `.\gradlew.bat build`
   - Forge 1.8.9 target: `.\forge-1.8.9\gradlew.bat build`
4. Run relevant automated checks and launch each affected client target. For UI
   work, capture and inspect the same screens/resolution on the affected target
   and the reference target. Fix failures before considering the increment done.
5. Report exactly which targets built, which clients were launched, what was
   visually/functionally checked, and any remaining feature gaps. Never present
   a build-only check as a successful in-game test.
6. Every finished change ends with a Minecraft smoke test: launch the client
   (`.\gradlew.bat runClient`), check the title menu and the dashboard, and load
   a world when in-game rendering is involved. Say what was actually seen.

## Initial priority

Start with Forge 1.8.9. Compare its module/settings catalog and screens against
the current Fabric client, then port missing behavior in reviewable increments.
Once 1.8.9 reaches documented parity, add the remaining Minecraft releases
incrementally using the loader-selection rule above. Keep the root and
`forge-1.8.9` targets building throughout.
