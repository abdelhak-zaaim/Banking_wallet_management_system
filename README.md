# Banking Wallet Management System (Java + Oracle)

This project is a small wallet management system that simulates real-world, banking‑grade wallet transactions using Java, Oracle Database, and JDBC. It focuses on correct transactional behavior, idempotent money transfers, and a realistic split between a Java service layer and PL/SQL business logic.

## Quickstart

### Prerequisites

- JDK 17+ (or the version configured in `build.gradle.kts`)  
- Oracle Database instance you can connect to  
- Network access from your machine to the Oracle DB  
- A `.env` file in the project root with Oracle connection settings

You can configure the database either via individual fields or a full JDBC URL:

- Option A – separate fields:
  - `ORACLE_HOST` – Oracle host (e.g. `localhost`)  
  - `ORACLE_PORT` – Oracle port (e.g. `1521`)  
  - `ORACLE_SERVICE` – service name or SID (e.g. `XE` or `ORCLCDB`)  
  - `ORACLE_USER` – schema user the app should connect as  
  - `ORACLE_PASSWORD` – password for the user  

- Option B – full URL (overrides host/port/service if present):
  - `ORACLE_URL` – full JDBC URL (e.g. `jdbc:oracle:thin:@//localhost:1521/XE`)

The `DataSourceModule` reads these variables using Dotenv, configures an `OracleDataSource`, and exposes it for injection.

### Build and Run

From the project root:

```bash
./gradlew clean build
```

Run the application (e.g. via the `Main` class):

```bash
./gradlew run
```

Or, if you prefer running the built JAR:

```bash
./gradlew jar
java -jar build/libs/Banking_wallet_management_system-1.0-SNAPSHOT.jar
```

### Migrations at Startup

On startup, the application runs Flyway-style migrations located in:

- `src/main/resources/db/migration/..`

A small migration bootstrap (e.g. `DatabaseMigrator`) initializes the schema and installs/updates the `wallet_pkg` PL/SQL package before any business logic executes. This ensures:

- Required tables, indexes, and sequences exist  

If the database is already migrated, the scripts are skipped or re-run idempotently depending on how the migrator is configured.

## Architecture Overview

At a high level, the system is split into three layers:

1. **Java Service Layer (JVM)**  
   - Core code lives under `src/main/java/com/wallet`.  

2. **Data Access and Infrastructure**  
   - `DataSourceModule` configures an `OracleDataSource` using values from the `.env` file.  
   - JDBC connections are acquired from this `DataSource` and participate in explicit transactions (auto-commit disabled when running transfers).  
   - `DatabaseMigrator` (or equivalent) runs the SQL migration scripts at startup.

## Transaction Safety

The system is intentionally focused on safe, consistent money transfers:

- **Row locking in the database**  
  The PL/SQL code in `wallet_pkg.transfer` locks the relevant wallet rows (e.g. via `SELECT ... FOR UPDATE`) so concurrent transfers cannot overspend the same balance.

- **Balance validation inside the update**  
  Balance and limit checks are done in the database, often as part of the same transactional block as the debit/credit operations. If an account does not have enough funds, the procedure raises an error and the transaction is rolled back.

- **Idempotent transfers via `REQUEST_ID`**  
  Each transfer is associated with a unique `REQUEST_ID` stored in the journal or a dedicated ledger table. If the same `REQUEST_ID` is submitted again, `wallet_pkg.transfer` detects this and returns the existing result instead of posting a duplicate transfer.

This combination ensures that each logical transfer is all‑or‑nothing, safe under concurrency, and robust against retries or network issues.

## Status and Disclaimer

This repository is intended as a **learning/demo project** to explore:

- Java + Oracle Database integration with JDBC  
- Guice-based dependency injection and configuration via environment variables  
- Flyway-style migrations and PL/SQL packaging  
- Transaction-safe wallet transfers and idempotency patterns

