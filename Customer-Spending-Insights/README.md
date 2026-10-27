# CustomerSpendingInsights

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.2.4.

## Run with Docker

Requirements: Docker with the Compose plugin, and the backend and Keycloak services running on the host at ports `8082` and `8080`. The backend must allow the frontend origin `http://localhost:4200`, and Keycloak must allow it as a valid web origin and redirect URI.

Clone the repository and start the frontend:

```bash
docker compose up --build
```

Open [http://localhost:4200](http://localhost:4200). The frontend's `/api` requests are proxied by Nginx to the backend at `http://host.docker.internal:8082`. To use another frontend port, set `APP_PORT`, for example `APP_PORT=4300 docker compose up --build`.

Stop the app with `Ctrl+C`, or run `docker compose down` from the project directory.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

```
Login Details:

username: john.doe
password: JohnPassword123!
