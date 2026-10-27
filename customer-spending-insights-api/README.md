# Customer Spending Insights API

## Prerequisites
- Docker Desktop installed and running.
- Docker Compose available.

## Start the Backend

1. Open a terminal in the project root directory (where `docker-compose.yml` is located).
2. Build and start the backend:

   ```bash
   docker compose up -d --build
   ```

3. Check that the containers are running:

   ```bash
   docker compose ps
   ```

## Access the API

- **Backend API:** http://localhost:8082
- **Keycloak:** http://127.0.0.1:8080

## View Logs

```bash
docker compose logs -f customer-spending-api
```

## Stop the Backend

```bash
docker compose down
```

## Notes
- Seed data is created automatically when the backend starts.
- The development seed runner clears existing customers, transactions, and goals before inserting sample data.
# customer-spending-insights-api
# customer-spending-insights-api
# Customer-Spening-Insights-Api
# Customer-Spening-Insights-Api
# Customer-Spending-Insights-Api
