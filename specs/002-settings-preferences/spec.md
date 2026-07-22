# Feature Specification: Settings & Preferences

**Feature Branch**: `002-settings-preferences`

**Created**: 2026-07-20

**Status**: Draft

**Input**: User description: "Settings feature: a Settings screen that aggregates preferences owned by each feature module. App-wide preferences (theme mode, locale), Mushaf-specific preferences (tajweed toggle, reciter selection, audio download management). Audio download of mushaf recitations is a separate mushaf feature (reciter list, download with progress, storage management, playback) linked from settings rather than implemented inside it. All new UI strings in English and Arabic, RTL-aware."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Control app appearance and language (Priority: P1)

A reader opens Settings and changes how the app looks and which language it speaks. They pick
a light, dark, or system-matching appearance, and switch between Arabic and English. The change
takes effect immediately across every screen, and the app remembers it the next time they open it.

**Why this priority**: This is the smallest complete slice that delivers value on its own. It
requires no other feature to exist, it is the setting users reach for most often, and an
Arabic-first audience expects language control to be present from day one.

**Independent Test**: Open Settings, switch appearance to Dark and language to Arabic, confirm
the whole app redraws in dark colors with Arabic text laid out right-to-left, force-close the
app, reopen it, and confirm both choices persisted.

**Acceptance Scenarios**:

1. **Given** the app is using the system appearance, **When** the user selects "Dark", **Then**
   every visible screen switches to dark colors immediately without restarting the app.
2. **Given** the app is in English, **When** the user selects Arabic, **Then** all interface text
   appears in Arabic and the layout mirrors to right-to-left.
3. **Given** the user selected Dark and Arabic, **When** they close and reopen the app, **Then**
   the app starts in Dark and Arabic without showing a flash of the previous appearance.
4. **Given** the user has never opened Settings, **When** they launch the app, **Then** appearance
   follows the device system setting and language follows the device language where supported.

---

### User Story 2 - Adjust reading preferences from one place (Priority: P2)

A reader who wants plain, uncolored Arabic script opens Settings and turns off tajweed coloring.
When they next open the Mushaf, the script honors that choice. Reading preferences appear in the
same Settings screen as appearance and language, so the reader has one place to configure the app.

**Why this priority**: It delivers the core promise of the feature — a single, coherent Settings
screen — and it makes an existing reading capability discoverable. It builds on Story 1's screen
but is separately testable and shippable.

**Independent Test**: Open Settings, toggle tajweed coloring off, navigate to the Mushaf, and
confirm the page renders in plain script; return to Settings and confirm the toggle still reads
off.

**Acceptance Scenarios**:

1. **Given** tajweed coloring is on, **When** the user turns it off in Settings, **Then** the
   Mushaf renders pages in plain script the next time it is shown.
2. **Given** the user changed a reading preference, **When** they reopen Settings, **Then** the
   control reflects the value they last chose.
3. **Given** the user is on the Settings screen, **When** they view it, **Then** appearance,
   language, and reading preferences are presented as clearly labeled, visually distinct groups.
4. **Given** a reading preference is changed, **When** the change is saved, **Then** it persists
   across app restarts.

---

### User Story 3 - Choose a reciter (Priority: P3)

A reader browses the available reciters and picks the voice they want to hear. Their choice is
remembered and becomes the voice used whenever recitation audio plays.

**Why this priority**: Reciter choice is a prerequisite for meaningful audio download and
playback, and it is valuable on its own as a stated preference even before any audio is
downloaded.

**Independent Test**: Open Settings, open the reciter list, select a reciter, return to Settings,
and confirm the selected reciter's name is displayed as the current choice and survives a restart.

**Acceptance Scenarios**:

1. **Given** the reader has not chosen a reciter, **When** they open Settings, **Then** a sensible
   default reciter is shown as selected.
2. **Given** the reciter list is open, **When** the reader selects a different reciter, **Then**
   the selection is saved and shown as current in Settings.
3. **Given** the reader has selected a reciter, **When** recitation audio is played anywhere in
   the app, **Then** it uses the selected reciter's voice.

---

### User Story 4 - Download recitation audio for offline listening (Priority: P4)

A reader with limited or intermittent connectivity downloads recitation audio ahead of time. They
can see download progress, cancel a download in flight, see how much storage the downloaded audio
occupies, and delete audio they no longer need.

**Why this priority**: It is the highest-effort part of the feature and depends on Story 3. It is
deferred so the Settings screen can ship and be useful before audio storage work lands.

**Independent Test**: Choose a reciter, start a download, observe progress advance to completion,
go offline, confirm the audio is still available, then delete it and confirm the reported storage
usage decreases accordingly.

**Acceptance Scenarios**:

1. **Given** the reader has selected a reciter, **When** they start a download, **Then** visible
   progress is shown and updates until the download completes.
2. **Given** a download is in progress, **When** the reader cancels it, **Then** the download stops
   and any partially downloaded data is removed rather than left occupying storage.
3. **Given** audio has been downloaded, **When** the device has no network connection, **Then**
   that audio still plays.
4. **Given** downloaded audio exists, **When** the reader views download management, **Then** the
   amount of storage used is shown in a human-readable form.
5. **Given** downloaded audio exists, **When** the reader deletes it, **Then** they are asked to
   confirm first, and on confirmation the audio is removed and reported storage usage decreases.
6. **Given** a download fails because the connection dropped, **When** the reader returns to
   download management, **Then** they see that it failed and can retry it.

---

### Edge Cases

- **Storage exhausted mid-download**: The download stops, the reader is told storage is
  insufficient, and partial data is cleaned up rather than silently consuming space.
- **Connection lost mid-download**: The download is reported as interrupted and can be retried;
  already-completed portions are not re-fetched unnecessarily.
- **Reciter switched while a download is in progress**: The in-flight download is not silently
  abandoned; the reader is told what will happen to it before the switch takes effect.
- **Deleting audio that is currently playing**: Playback stops cleanly and the reader is informed,
  rather than the app continuing against removed data.
- **Language switched while a screen is open**: The visible screen re-renders in the new language
  and mirrors direction without losing the reader's position or in-progress input.
- **Appearance set to "system" and the device switches to night mode while the app is open**: The
  app follows the change without needing a restart.
- **Reciter list unavailable** (no connection on first launch): The reader sees an explicit
  unavailable state with a retry option, not an empty screen or a crash.
- **Settings opened before any preference has ever been saved**: Every control shows its documented
  default rather than a blank or indeterminate state.

## Requirements *(mandatory)*

### Functional Requirements

**Settings screen**

- **FR-001**: The system MUST provide a Settings screen reachable from the app's primary navigation.
- **FR-002**: The Settings screen MUST present preferences in labeled groups: appearance and
  language, reading preferences, and recitation audio.
- **FR-003**: The Settings screen MUST show each preference's current value without the reader
  having to open the control.
- **FR-004**: Every preference change MUST take effect without requiring an app restart.
- **FR-005**: Every preference MUST persist across app restarts.
- **FR-006**: The Settings screen MUST remain usable and correctly laid out when a preference group
  is unavailable, showing only the groups that apply.

**Appearance and language**

- **FR-007**: Users MUST be able to choose an appearance of Light, Dark, or Match System.
- **FR-008**: Users MUST be able to choose between Arabic and English as the app language.
- **FR-009**: Changing the language MUST update all interface text and mirror layout direction to
  match the language's reading direction.
- **FR-010**: On first launch, appearance MUST default to Match System and language MUST default to
  the device language when it is a supported language, and to Arabic otherwise.
- **FR-011**: The app MUST apply the saved appearance and language during startup without a visible
  flash of the previous or default values.

**Reading preferences**

- **FR-012**: Users MUST be able to turn tajweed coloring on or off.
- **FR-013**: A reading preference change MUST be reflected the next time the affected reading
  surface is displayed.

**Reciter selection**

- **FR-014**: The system MUST present a list of available reciters with each reciter's name.
- **FR-015**: Users MUST be able to select exactly one reciter as their current choice.
- **FR-016**: The system MUST show the currently selected reciter on the Settings screen.
- **FR-017**: The system MUST apply a documented default reciter when the user has not chosen one.
- **FR-018**: The system MUST show an explicit unavailable state with a retry action when the
  reciter list cannot be obtained.

**Audio download and storage**

- **FR-019**: Users MUST be able to download recitation audio for their selected reciter at
  [NEEDS CLARIFICATION: download granularity not specified — per surah, per juz, whole mushaf only,
  or a combination?]
- **FR-020**: The system MUST show download progress while a download is running.
- **FR-021**: Users MUST be able to cancel an in-progress download, and cancellation MUST remove
  partially downloaded data.
- **FR-022**: The system MUST report the total storage occupied by downloaded audio in a
  human-readable form.
- **FR-023**: Users MUST be able to delete downloaded audio, and the system MUST require explicit
  confirmation before deleting.
- **FR-024**: Downloaded audio MUST be playable with no network connection.
- **FR-025**: The system MUST report a failed or interrupted download distinctly from a completed
  one and MUST offer a retry action.
- **FR-026**: The system MUST prevent a download from starting when available storage is
  insufficient, and MUST tell the reader why.
- **FR-027**: The system MUST indicate which audio is already downloaded so the reader does not
  download the same content twice.

**Localization and presentation**

- **FR-028**: Every user-facing string introduced by this feature MUST be available in both English
  and Arabic.
- **FR-029**: Every screen introduced by this feature MUST lay out correctly in both left-to-right
  and right-to-left reading directions.
- **FR-030**: Numeric values shown to the reader (storage sizes, progress percentages, counts) MUST
  be formatted according to the active language's conventions.
- **FR-031**: Each screen introduced by this feature MUST present explicit loading, empty, and
  error states rather than a blank screen.

### Key Entities

- **App Preferences**: The reader's app-wide choices. Attributes: appearance mode (Light / Dark /
  Match System), language (Arabic / English). One set per install.
- **Reading Preferences**: The reader's Mushaf reading choices. Attributes: tajweed coloring
  enabled, last page read. Owned by the reading experience, surfaced in Settings.
- **Reciter**: A recitation voice the reader can choose. Attributes: identifier, display name in
  both supported languages, and where its audio is obtained from. Many exist; exactly one is
  selected at a time.
- **Audio Download**: A unit of downloaded recitation audio for a given reciter. Attributes: what
  content it covers, its state (not downloaded / downloading / downloaded / failed), progress while
  running, and storage occupied. Relates to exactly one Reciter.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reader can change the app's appearance or language and see the result applied
  everywhere in under 3 seconds, without restarting the app.
- **SC-002**: 100% of preferences the reader sets are still in effect after closing and reopening
  the app.
- **SC-003**: A reader can find and change any single preference in under 20 seconds from the app's
  main screen, in at most 3 taps.
- **SC-004**: 100% of user-facing text on the Settings screen and every screen it links to is
  displayed in the reader's chosen language, with no untranslated text in either language.
- **SC-005**: Every screen introduced by this feature renders without clipped, overlapping, or
  mis-mirrored elements in right-to-left layout.
- **SC-006**: A reader can complete an audio download, disconnect from the network, and play the
  downloaded audio successfully on the first attempt.
- **SC-007**: After deleting downloaded audio, reported storage usage matches actual storage
  released within 1% and no orphaned data remains.
- **SC-008**: Interrupted downloads (cancelled, failed, or storage-exhausted) leave 0 bytes of
  unreferenced partial data behind.
- **SC-009**: 90% of readers who open Settings successfully complete the change they intended on
  the first attempt, without backing out and retrying.

## Assumptions

- **Supported languages are Arabic and English only** for this feature; additional languages are out
  of scope and no user-facing language-management UI beyond these two is required.
- **Language is an in-app override**: the reader's choice in Settings takes precedence over the
  device language for as long as it is set, rather than only following the system.
- **Appearance offers exactly three options** (Light, Dark, Match System); per-screen or scheduled
  theming is out of scope.
- **Exactly one reciter is selected at a time**; multi-reciter comparison or simultaneous downloads
  across several reciters are out of scope.
- **Audio playback controls themselves are out of scope for this feature.** This feature covers
  choosing a reciter, downloading audio, and managing downloaded audio. The listening experience is
  reached from Settings but specified separately.
- **The reader is signed in or anonymous alike**; preferences are per-install and are not
  synchronized across the reader's devices in this feature.
- **Tajweed coloring is the only reading preference in scope** for the initial release; the reading
  preferences group is expected to grow, and the Settings screen must accommodate additions without
  rework.
- **The existing reading experience already stores tajweed coloring and last-page state**; this
  feature surfaces those existing preferences rather than redefining them.
- **Downloaded audio is stored in app-private storage** and is removed if the app is uninstalled;
  export to shared storage is out of scope.
- **Reciter audio is obtained from** [NEEDS CLARIFICATION: reciter catalog and audio source not
  specified — bundled with the app, fetched from a specific existing recitation service, or from a
  backend the project will provide?]
- **Downloads run while the app is in the foreground**; background and resume-after-kill download
  behavior is assumed out of scope unless stated otherwise.
