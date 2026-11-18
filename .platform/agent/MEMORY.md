# Agent notes

keysbook-termux project notes:
- Single module `keysbook` (package com.keysbook.app), minSdk 26, targetSdk 34, compileSdk 36, JDK 17. Gradle files live in keysbook/. No Gradle wrapper.
- Root keysbook.py (Termux Python app) is the source of the provider catalogue. Providers.kt mirrors it 1:1 (keep in sync when keysbook.py changes).
- Max 10 keys per provider: enforced in EditKeyActivity.save() via MAX_KEYS_PER_PROVIDER. Each key is one vault entry (provider, label, base_url, api_key, notes).
- Workflows: .github/workflows/release.yml runs on tag v*, builds keysbook/ assembleDebug + assembleRelease with gradle 8.10, uploads keysbook-debug.apk / keysbook-release.apk + SHA256SUMS.txt to a GitHub release. android.yml runs `gradle build` on push/PR to main.
- Signing: env KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD (CI), else keystore.properties (local), else debug signing. VERSION_CODE/VERSION_NAME come from env.
- Tags: use `v1.8.3` style.
- Local debug build succeeds with run_task assemble:keysbook:debug.
- Editor diagnostic may show a stale "Unresolved reference" after adding a string; trust the build.
