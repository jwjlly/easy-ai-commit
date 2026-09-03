# Workspace Instructions

## Directory layout

- Keep the production JetBrains plugin as the Gradle project at the repository root; do not recreate a nested `plugin/` layer.
- Keep Kotlin implementation and tests under `src/`, plugin resources under `src/main/resources/`, brand assets under `branding/`, and development utilities under `tools/`.
- Keep the English `README.md` at the repository root and localized files at `readme/README.zh-CN.md`, `readme/README.ja.md`, and `readme/README.ko.md`.
- Keep all four README files synchronized, with working language navigation at the top of every file.

## Versioning and releases

- Follow Semantic Versioning 2.0.0 using `MAJOR.MINOR.PATCH`, with optional prerelease identifiers such as `0.2.0-alpha.1`, `0.2.0-beta.1`, and `0.2.0-rc.1`.
- Treat the root `version` value in `build.gradle.kts` as the single source of truth. The Gradle IntelliJ Platform configuration and packaged artifact must derive their version from it; do not duplicate a version in `plugin.xml` or generated files.
- While the plugin is below `1.0.0`, increment:
  - `PATCH` for backward-compatible bug fixes, documentation, localization, build, test, and internal refactoring changes.
  - `MINOR` for new user-visible capabilities and for any intentional breaking change to behavior, settings, persisted data, or supported IDE versions. Clearly identify breaking changes in release notes.
- Starting with `1.0.0`, increment:
  - `MAJOR` for incompatible changes to behavior, configuration, persisted data, or compatibility guarantees.
  - `MINOR` for backward-compatible functionality.
  - `PATCH` for backward-compatible fixes and maintenance.
- Use lowercase dot-separated prerelease identifiers, advance them monotonically within a release line, and publish the final release without a prerelease suffix. Do not use four-part versions, dates, or build numbers as the public plugin version.
- Tag releases as `vMAJOR.MINOR.PATCH` or `vMAJOR.MINOR.PATCH-prerelease`, exactly matching `build.gradle.kts` after the leading `v` is removed.
- Do not bump the version during ordinary development unless the task explicitly includes release preparation or a version change.
- Before a release, update `changeNotes`, synchronize any version references in user documentation, run `check`, `verifyPluginStructure`, and `buildPlugin`, and verify that the produced ZIP filename contains the intended version. Never edit generated artifacts under `build/` by hand.

## Confirmed product behavior

- Keep the Commit UI visually native to JetBrains and add only one Easy AI Commit logo button above the Commit Message input.
- Clicking the logo starts generation immediately. Do not show a preview, popover, confirmation, “Use message”, or regenerate action.
- While generating, the logo itself communicates progress; on success, the generated commit message is written directly into the existing Commit Message input.
- The configuration page is a separate settings surface and must not interrupt the one-click generation flow.
- Place the configuration entry under JetBrains `Settings → Other Settings → Easy AI Commit`; do not add a plugin-specific settings entry to the Commit Message toolbar.
- Each model connection has a user-editable configuration name. Lists and selectors show that custom name so multiple OpenAI-compatible or Anthropic-compatible endpoints remain distinguishable.
- Use English as the fallback UI language and maintain complete Simplified Chinese, Japanese, and Korean resource bundles with identical keys.
- Keep protocol names `OpenAI Compatible` and `Anthropic Compatible` in English in every locale.

When implementing from a selected generated mock, treat that image as the source of truth for layout, component anatomy, density, spacing, color, typography, visible content, and hierarchy.
