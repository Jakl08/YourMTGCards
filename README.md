# YourMTGCards

YourMTGCards is a web application and backend concept for automatically scanning, sorting, and storing Magic The Gathering cards.  
The concept was first developed in 2022.

## Architecture

The frontend is intended to provide a card inventory, search, and controls for retrieving cards from storage. Its static files are organized under `src/main/resources/static/`: `index.html` is the entry point, with styles in `css/` and JavaScript in `js/`. Spring Boot serves the page at the application root. The current page is a starter shell; card inventory and backend integrations are not implemented yet. A Java backend will coordinate the web application with the inventory database, scanning and storage hardware, and a third-party Magic card API.

Backend Java packages are grouped under `space.dragonfruit.YourMTGCards.backend`:

```text
backend/
├── config/       # Backend configuration
├── controller/   # REST endpoints
├── dto/          # API request and response objects
├── exception/    # Exceptions and API error handling
├── model/        # Domain models and persistence entities
├── repository/   # Data access
└── service/      # Application and business logic
```

The package folders are initialized with `package-info.java` files. Add backend classes to the corresponding package as the application grows.

![Conceptual YourMTGCards architecture](images/YourMTGCards.png)

## Tech stack

- Java 25
- Spring Boot 4.1.1
- Maven

## Prerequisites

- A JDK 25 installation
- The included Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Build, test, and run

From the repository root, use the Maven Wrapper:

```bash
# Compile and package
./mvnw clean package

# Run the tests
./mvnw test

# Start the application
./mvnw spring-boot:run
```

On Windows, run the same Maven goals with `mvnw.cmd`, for example:

```bat
mvnw.cmd spring-boot:run
```

The Spring Boot application is configured with the name `YourMTGCards`. Since the REST API and frontend are not implemented yet, the application currently provides its Spring Boot foundation rather than the full card-management experience.
