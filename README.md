# Snippet Tray Manager

Desktop Java tray application for storing and searching text snippets with tags.

## Features

- Runs in the system tray
- Manage tags (add multiple tags, delete selected tags)
- Add text snippets with optional title and attach one or more tags
- Search snippets by selected tag
- Search snippets by title or text content (`contains`, case-insensitive)
- Delete text snippets directly from the search results
- Data stored in local SQLite DB file in the same directory as the executed jar (`snippet_tray.db`)

## Requirements

- Java 17+
- Gradle (or use local Gradle wrapper if you generate one)

## Build

```bash
./gradlew build
```

Output jar:

`build/libs/snippet-tray-manager-1.0.0-all.jar`

## Create distributable zip

```bash
./gradlew bundleZip
```

Output zip:

`build/distributions/snippet-tray-manager-1.0.0-bundle.zip`

This project is configured to produce only this single distribution archive.

## Use without building

You can run the app directly from the checked-in distributable bundle:

`build/distributions/snippet-tray-manager-1.0.0-bundle.zip`

Extract it and run `run.sh` (macOS/Linux) or `run.bat` (Windows) from the extracted folder.

## Run

```bash
java -jar build/libs/snippet-tray-manager-1.0.0-all.jar
```

or use launch scripts from the project root:

```bash
./run.sh
```

On Windows:

```
run.bat
```

Both launch scripts verify that `java` is available on `PATH` and show a GUI popup with an `OK` button if Java is not installed or if the installed Java version is lower than 17 (with terminal text fallback where popup tools are unavailable).

## Tray menu

- Add tag
- Add text snippet
- Search snippets
- Show database location
- About
- Exit application

## Notes

- If the execution directory is not writable, startup fails with an error message.
- On Linux/macOS desktop environments without system tray support, the app cannot run as tray app.
- Data is persisted in `snippet_tray.db` and is not deleted on app close/restart.
- Launch scripts intentionally avoid `clean` builds to prevent accidental deletion of the DB in development workflows.
