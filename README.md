# Global Dorm Orchestrator

A Spring Boot application for managing student accommodation, paired with a Python client for programmatic and interactive access. The system provides room searching, application management, distance calculations, and weather forecasts.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
- [REST API Endpoints](#rest-api-endpoints)
- [Python Client](#python-client)
- [Dependencies](#dependencies)
- [License](#license)

## Features

- **Room Management**
  - Search rooms by city, price, furnished status, and spoken languages.
  - Retrieve detailed room information, including amenities, availability, and landlord info.

- **Application Management**
  - Submit applications for rooms.
  - Cancel pending applications.
  - View a user’s application history with timestamps.

- **External Services**
  - Calculate driving distance and estimated travel time between a room and campus using OSRM.
  - Retrieve a 3-day weather forecast for any UK postcode using 7timer.info.

- **System Utilities**
  - Health check endpoint to verify service availability.

- **Python Client**
  - Interactive command-line interface (CLI).
  - Programmatic access to all application and room functionality.
  - Handles API errors and JSON responses gracefully.
  - Provides pretty-printed summaries of rooms, applications, distances, and weather.
  
---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Python 3.10+
- Internet access for external API calls

### Build and Run

1. Clone the repository:

```
git clone <repository_url>
cd orchestrator
```

2. Build the Java Spring Boot application:

```
./mvnw clean package
```

3. Run the application:

```
./mvnw spring-boot:run
```

The application will run on `http://localhost:8080`.

---

## Python Client
The client allows interactive usage or programmatic integration with the orchestrator.

### Installation

1. Navigate to the client folder:
```
cd Client
```

2. Install Python dependencies:
```
python -m pip install -r requirements.txt
```

### Usage
Run the main script:
```
python client.py
```

You can:
1. Search all rooms or apply filters (city, price, furnished, language).
2. Get detailed information for a room.
3. Apply for a room.
4. View your applications.
5. Cancel an application.
6. Calculate distance to campus.
7. Get a 3-day weather forecast.
8. Exit the CLI.

---

## REST API Endpoints
- `GET /api/rooms` - Search and filter rooms
- `GET /api/rooms/{roomId}` - Get room details
- `POST /api/applications` - Apply for a room
- `DELETE /api/applications/{applicationId}` - Cancel an application
- `GET /api/users/{userId}/applications` - Get user application history
- `GET /api/distance` - Calculate driving distance between two postcodes
- `GET /api/weather` - Retrieve 3-day weather forecast
- `GET /api/test/postcode` - Test postcode geocoding
- `GET /api/health` - System health check

---

## Dependencies
- ### Java
    - Spring Boot 3.2.0
    - Spring Web
    - Jackson Databind
- ### Python
    - `requests`

---

## License
This project is licensed under the MIT License. See LICENSE for details.

---