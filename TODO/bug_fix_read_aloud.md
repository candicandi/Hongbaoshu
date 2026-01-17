# Bug Fix: Read Aloud Highlighting and Page Turn Issue

- [x] Locate TTS and Highlighting logic in codebase
- [x] Analyze `ReaderViewModel` and `AudioManager` (or equivalent) for state issues
- [x] Identify why highlighting stops after the first sentence on fresh install (Race condition in `AudioManagerImpl` state updates)
- [x] Identify why audio stops on page turn (Resumption logic failed when page sentences weren't ready)
- [x] Implement fix for highlighting synchronization (Use `StateFlow.update` for atomic operations)
- [x] Implement fix for page turn audio persistence (Observe `currentPageSentences` in `LaunchedEffect`)
- [x] Identify why highlighting is missing on fresh start until page turn (Background thread wiping sentence data in `PageEngine`)
- [x] Refactor `PageEngine.paginate` to not clear sentence ranges
- [x] Verify fix (manual compilation and deploy as per user rules)
- [x] Build Release APK
- [x] Create Git Commit and Tag
- [x] Publish GitHub Release (Uploaded binary via gh CLI)
