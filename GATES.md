# Gates: Remove Cookie Capturing

OWNS: app/src/main/java/com/dettle/app/data/backup/BackupManager.kt, app/src/main/java/com/dettle/app/ui/backup/BackupRestoreSheet.kt, app/src/main/java/com/dettle/app/ui/settings/SettingsScreen.kt, docs/dettle-architecture.html, docs/AI_MODELS_AND_PROVIDERS.md, CHANGELOG.md, task.md

Scope: Remove all logic related to capturing, persisting, and restoring browser cookies via Android CookieManager, as requested to address ethical and privacy concerns.

- [x] G1: No CookieManager references exist in the Kotlin codebase
  CHECK: grep -r "CookieManager" app/src/main/java || echo "CLEAN"
  EXPECT: CLEAN
  EVIDENCE: automatic-evidence=v1; definition-sha256=0c6612811a01e6b1f13cf936b8949225fed1a2204217d946e747ab5595b1791a; exit=0; EXPECT=matched; output-sha256=0b98843240a0b1a2384483206c02be8968dd809eb73838820be24cb7d6d6aeb9; output-bytes=6; shell=/bin/sh; cwd=/home/shiv/Sandilyapoorv/Dettle; path=0ed32491488a/18 entries

- [x] G2: BackupOptions data class no longer includes cookies
  CHECK: grep -r "includeCookies" app/src/main/java || echo "CLEAN"
  EXPECT: CLEAN
  EVIDENCE: automatic-evidence=v1; definition-sha256=bc8dd31f1f935b9635e85f949d06d390c656e14bc719d3a0cd324b7949609d55; exit=0; EXPECT=matched; output-sha256=0b98843240a0b1a2384483206c02be8968dd809eb73838820be24cb7d6d6aeb9; output-bytes=6; shell=/bin/sh; cwd=/home/shiv/Sandilyapoorv/Dettle; path=0ed32491488a/18 entries

- [x] G3: SettingsScreen UI no longer displays cookie count
  CHECK: grep -r "cookieDomainCount" app/src/main/java || echo "CLEAN"
  EXPECT: CLEAN
  EVIDENCE: automatic-evidence=v1; definition-sha256=6f72cff1379a44caddcb469632140d1f274c7f90dca32c4e4c127dabd982e585; exit=0; EXPECT=matched; output-sha256=0b98843240a0b1a2384483206c02be8968dd809eb73838820be24cb7d6d6aeb9; output-bytes=6; shell=/bin/sh; cwd=/home/shiv/Sandilyapoorv/Dettle; path=0ed32491488a/18 entries
