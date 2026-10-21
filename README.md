# YourMTGCards

YourMTGCards is a web application and backend concept for automatically scanning, sorting, and storing Magic The Gathering cards.  
The concept was first developed in 2022.

## Architecture

The frontend is intended to provide a card inventory, search, and controls for retrieving cards from storage. A Java backend will coordinate the web application with the inventory database, scanning and storage hardware, and a third-party Magic card API.

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
