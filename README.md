# CampusPulse

An Android app for finding things to do around the University of Illinois campus. It pulls from a dataset of about 2,700 real events scraped from the university calendar (talks, exhibits, club meetings, performances, virtual sessions) and lets you browse, search, filter, and star the ones you care about.

This README is written for someone trying to understand how the project works and the decisions behind it, not for someone trying to run it. If you do want to run it, there's a short section at the bottom.

## What the app does

When you open it, you land on a list of today's events sorted by start time. Each row shows the title, date, time, and location. From there you can:

- Search by keyword, or use a small query syntax like `robotics location:siebel` or `workshop virtual:true`
- Toggle three filters in the toolbar: today only, virtual only, and starred only
- Tap an event to see its full details (description, categories, source, link back to the official listing)
- Star an event from the detail screen, and have that star show up right away on the main list

Your filter settings and search text are remembered, so closing and reopening the app puts you back where you were.

## How it's put together

The interesting constraint in this project is that there is no real backend. Instead, the app starts a local HTTP server inside itself on launch (using OkHttp's `MockWebServer`), loads the event data from a bundled JSON file, and then talks to that server over real HTTP like it would talk to any API. That meant I got to write both sides of a client/server interaction, and had to deal with threading, response codes, and serialization the same way I would in a production app.

```
UI layer          MainActivity, EventActivity, SummaryListAdapter
                        |
                        | callbacks (background thread -> runOnUiThread)
                        v
Network client    Client  (OkHttp, ExecutorService, Jackson)
                        |
                        | HTTP on localhost:8024
                        v
Local server      Server  (MockWebServer Dispatcher, routes + JSON)
                        |
                        v
Data              events.json  ->  EventData records -> Summary / Event models
```

### The server

`Server` extends OkHttp's `Dispatcher` and routes requests by path and method. The routes I implemented:

| Route | Method | What it does |
|---|---|---|
| `/summary` | GET | Returns lightweight summaries for every event starting today or later (in Chicago time) |
| `/event/{id}` | GET | Returns the full event, or 404 if the id doesn't exist |
| `/favorite/{id}` | GET | Returns whether an event is starred (defaults to false) |
| `/favorite` | POST | Sets a star. Validates the JSON body, returns 400 on bad input, 404 on unknown events, and a 302 redirect to the new resource on success |
| `/reset` | GET | Clears favorites, used between tests |

Splitting "summary" and "event" into two endpoints was deliberate. The list screen only needs five fields per event, so it doesn't pay the cost of downloading thousands of descriptions it will never show.

### The client

`Client` wraps OkHttp and exposes async methods like `getSummaries(callback)` and `setFavorite(id, value, callback)`. Every request runs on an `ExecutorService` so the UI thread never blocks, and results come back wrapped in a small `ResultMightThrow<T>` type that holds either a value or an exception. That kept error handling consistent: every callback does the same `try { result.getValue() } catch` dance instead of each screen inventing its own convention.

The client also has to treat a 302 from the POST as a success, since the server answers a successful write with a redirect rather than a 200.

### Search

Search lives in `Summary.search()` and was the most fiddly part of the project. It's a small hand-written parser rather than a regex:

- Plain words match against both title and location
- `location:` takes everything after it (including multiple words) until the next filter token
- `virtual:true` and `virtual:false` filter by format, and any other value is treated as invalid and returns nothing
- Once a filter is present, the plain words only match titles, so `talk location:illini union` means "talks at the Illini Union" and not "anything mentioning talk or the Illini Union"
- Free text has to come before filters, so the query reads naturally left to right

Everything is case insensitive, and the results get combined with whatever toolbar toggles are active.

### Keeping stars in sync across screens

The trickiest bug I ran into was staleness. You'd star an event on the detail screen, press back, and the main list wouldn't know about it until it re-fetched.

I fixed it with a small shared `FavoritesRepository` that both screens read from. It's a synchronized in-memory map with a listener list. When you tap the star, the detail screen updates the repository immediately (so the UI feels instant), then fires the POST in the background. If the server call fails, it rolls the change back and unchecks the button. The main screen subscribes to the repository and re-renders whenever it changes.

It's basically optimistic updates with rollback, done by hand without a framework.

### Time handling

All "today" logic is computed in `America/Chicago` rather than the device's time zone, since the events are all local to campus. The current time comes from a `TimeProvider` abstraction instead of calling `Instant.now()` directly. That's what let the test suite freeze the clock on a specific date and check that the "today" filter and the server's "upcoming only" rule behave correctly, and the build actually fails if code bypasses it.

## Testing and code quality

The project runs 21 automated tests using JUnit, Robolectric, and Espresso. They cover the server routes directly over HTTP, the client's parsing and error paths, and full UI flows like "type a query, check the list, open an event, star it, go back, confirm it's starred."

On top of the tests, every build runs Checkstyle and Spotless (Google Java Format), so formatting and style problems fail the build the same way a broken test does. Getting used to that was a good habit. Code that works but doesn't pass lint is treated as unfinished.

## Tech stack

- Java 21, Android SDK 36
- OkHttp 5 for HTTP, MockWebServer for the embedded server
- Jackson for JSON (including Java records for the raw event data)
- RecyclerView and ConstraintLayout for the UI
- SharedPreferences for saving filter state
- JUnit 4, Robolectric, Espresso, and Truth for tests
- Gradle (Kotlin DSL), Checkstyle, Spotless

## Project layout

```
app/src/main/java/edu/illinois/cs/cs124/ay2025/mp/
  activities/     MainActivity (list, search, filters), EventActivity (details, starring)
  adapters/       SummaryListAdapter for the RecyclerView
  application/    App startup, launches the local server and client
  helpers/        FavoritesRepository, ResultMightThrow, shared Jackson and time utilities
  models/         EventData (record), Event, Summary (search and filtering), Favorite
  network/        Client (OkHttp) and Server (MockWebServer dispatcher)
app/src/main/resources/events.json    ~2,700 events from the Illinois calendar
app/src/test/                          Test suites (unit, network, and UI)
```

## Running it

Open the project in Android Studio with a JDK 21 toolchain and run it on an emulator with API 36. The app starts its own server, so there's nothing else to set up.

To run the tests from the command line:

```
./gradlew test
```
