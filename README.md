# utils for TC

Spring Boot bridge service: pulls vehicle positions from a GlobalTeam-style source, republishes them as GTFS-Realtime, and forwards inbound block assignments to one or more TransitClock instances.

## Getting Started

- Configure `src/main/resources/application.yml`:
  - `api.base-url` — source (GlobalTeam) JSON endpoint base.
  - `api.tokens` — map `agency → token` (one poll per entry, every `api.get-interval` s).
  - `api.tc-base-urls` — map `agency → full TransitClock URL` (must contain `…/key/<key>/…`; the key is parsed out by regex).
  - `api.out-path`, `api.postfix` — directory + suffix for the protobuf snapshot file written every `api.refresh-interval` s.
  - `api.time-zone` — IANA zone used for GTFS-RT timestamps (e.g. `Europe/Warsaw`).
  - `api.user`, `api.password` — credentials for the single in-memory admin user (HTTP Basic, role `ADMIN`, required on all routes).

## Run

Requirement:
- `Java 21 +` installed (toolchain pinned in `build.gradle.kts`).

```bash
./gradlew bootRun                          # run the application
./gradlew build                            # compile + test + package
./gradlew test                             # run all tests
./gradlew clean build                      # clean rebuild
```

All routes are protected by HTTP Basic. Provide `-u <api.user>:<api.password>` in every curl below.

## Endpoints

Base path: `/api/v1`.

### GET `/api/v1/vehicles/positions?agency=<agency>`
Returns the current GTFS-Realtime `FeedMessage` for the given agency as a **text dump** (`FeedMessage.toString()`), built on demand from in-memory positions polled from the source.

### GET `/api/v1/vehicles/assignments?agency=<agency>`
Returns the list of `Assignment` objects already segregated for the given agency (i.e. the subset whose `vehicleId` is known to that agency). Empty list if none.

### POST `/api/v1/vehicles`
Accepts a bundle of assignments, stores them, then asynchronously fans them out to every agency in `api.tc-base-urls` (filtered per agency by matching `vehicleId` against the polled vehicle list).

Body:
```json
{
  "key": "<agency-key>",
  "vehicles": [
    {
      "vehicleId": "123",
      "blockId":   "BLK-001",
      "tripId":    "TRIP-999",
      "validFrom": "2024-06-01T06:00:00",
      "validTo":   "2024-06-01T14:00:00"
    }
  ]
}
```

### POST `/api/v1/vehicles/assignments`
Manual retry: re-sends the already-segregated assignments for a given agency to that agency's TransitClock. Body is a free-form JSON object; the agency name is extracted by regex `:\s*"([^"]+)"` — in practice `{"agency": "agency1"}` works.

### POST `/api/v1/key/{key}/agency/{agency}/command/vehiclesToBlockAssignments`

**TransitClock-shaped passthrough.** This endpoint mirrors the URL layout that downstream TransitClock instances expose at `/command/vehiclesToBlockAssignments` (see `TransitclockClient`), so a client already wired for TransitClock can post here unchanged and let this service take over the fan-out.

Behaviour:
- Accepts the same `AssignmentDto` body as `POST /api/v1/vehicles`.
- Delegates straight to `VehicleUpdatesService.addAllAssignments(body)` — i.e. stores the bundle, then spawns a background thread that filters per agency in `api.tc-base-urls` and POSTs each subset to the corresponding TransitClock with `@Retryable` retries.
- The authoritative key is `body.key` (used as the storage map key and forwarded as the outgoing `AssignmentDto.key`); the destination agencies come from `api.tc-base-urls`, not from `{agency}`.
- Returns `ApiResponseDto` — `{ "success": true, "message": "{<vehicleId>=true, ...}" }` on success, `{ "success": false, "message": "<error>" }` on failure (HTTP 200 in both cases; this handler does not propagate the exception).

Example:
```bash
curl -u "$API_USER:$API_PASSWORD" \
  -X POST "http://<host>:<port>/api/v1/key/abc123/agency/agency1/command/vehiclesToBlockAssignments" \
  -H 'Content-Type: application/json' \
  -d '{
        "key": "abc123",
        "vehicles": [
          {
            "vehicleId": "123",
            "blockId":   "BLK-001",
            "tripId":    "TRIP-999",
            "validFrom": "2024-06-01T06:00:00",
            "validTo":   "2024-06-01T14:00:00"
          }
        ]
      }'
```

## Response shape

All `POST` handlers (except the manual-retry one) return `ApiResponseDto`:

```json
{
  "success": true,
  "message": "{123=true, 5=true}"
}
```

- `success: true` — bundle stored; the fan-out runs asynchronously, so a `true` here means *accepted*, not *delivered*. Check application logs for `Post N assignments to TC …` to confirm downstream delivery.
- `success: false` — the handler caught an exception; `message` carries the error text.
- HTTP `400` is returned by Spring when the body fails to parse or required query/path params are missing.
