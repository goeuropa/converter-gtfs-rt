## Getting Started
- Setup properties  ->  src/main/resources/application.yml

## Run 

Requirement:  
- `java 17 +` - installed.

Commands:
```bash
./gradlew bootRun        # Run the application
./gradlew build          # Build the project
./gradlew test           # Run all tests
./gradlew clean build    # Clean rebuild
```
## Check
URL:

GET /api/v1/vehicles/positions?agency=agency

GET /api/v1/vehicles/assignments?agency=agency

POST /api/v1/vehicles?to=blockAssignments
````
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
]}
````
### Response

- **200 OK** — confirmation/status message string from the service.
```
{
  "success": true,
  "message": "{123=true, 5=true}"
}
```
- **400 Bad Request** — if the body cannot be parsed; returns the error message string.


