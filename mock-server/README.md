# SIH26001 Mock Alert Server

This is a lightweight local HTTP server written in Python to mock the SIH26001 alert service.
It returns realistic mock alerts (matching `AlertDto`) for local Android testing.

## Prerequisites
- Python 3.x installed on your host machine.

## How to Start the Server
Navigate to the `mock-server` directory and run the server using Python:

```bash
cd mock-server
python server.py
```

## Port
The server runs locally on port **8080**.

## Available Endpoints
- **Health Check**: `GET /health`
  - Returns `{"status": "ok"}`
- **Alerts**: `GET /api/v1/alerts`
  - Returns a JSON array of mock alerts from `data/alerts.json`.

## Example Curl Command (Host Machine)
```bash
curl http://127.0.0.1:8080/health
curl http://127.0.0.1:8080/api/v1/alerts
```

## Android Emulator Base URL
To connect the Android emulator to this server running on the host machine, use the `10.0.2.2` alias.

- **Emulator Base URL**: `http://10.0.2.2:8080/`
- **Emulator Alerts Endpoint**: `http://10.0.2.2:8080/api/v1/alerts`

*(Note: Do not use `127.0.0.1` or `localhost` from within the Android emulator, as it will point to the emulator's own loopback interface instead of the host machine's).*

## How to Stop the Server
Press `Ctrl+C` in the terminal where the server is running to stop it.
