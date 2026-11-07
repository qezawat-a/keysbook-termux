# Agent notes

keysbook-termux project notes:
- Single module `keysbook` (package com.keysbook.app), minSdk 26, targetSdk 34, compileSdk 36, JDK 17. settings.gradle.kts and build.gradle.kts live in keysbook/. No Gradle wrapper in repo.
- Plugin versions are pinned in keysbook/build.gradle.kts (AGP 8.7.3, Kotlin 2.0.21) because CI runs plain gradle 8.10 and no root build file exists. Local `assemble:keysbook:release` passes with these.
- .github/workflows/release.yml: on tag v*, runs `gradle assembleRelease` (gradle 8.10 via gradle/actions/setup-gradle@v4) in keysbook/, then publishes keysbook/**/*release*.apk via softprops/action-gh-release@v2. Use github.ref_name (not github.ref.name).
- Signing: env KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD (CI), else keystore.properties (keysbook/, local only), else debug signing. VERSION_CODE=run_number, VERSION_NAME=tag.
- Tags: prior tags used `v.1.8.x` (dot after v). Use `v1.8.3` style for new tags.
- Models screen COPY (item_model.xml @id/copy) calls copyModel(model); base URL uses copy_base -> copyBaseUrl(). Verified in source.
- Keys row buttons are wired in KeysActivity.KeyAdapter.getView().
- Local debug build succeeds with run_task assemble:keysbook:debug.
