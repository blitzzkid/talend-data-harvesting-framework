# talend-data-harvesting-framework

Spring Boot service that reads Talend ETL audit records from PostgreSQL and publishes dataset/lineage metadata to Talend Data Catalog (TDC) via its REST API.

- **Spring Boot** 4.0.6 · **Java** 21 · **Gradle** (wrapper included)
- **PostgreSQL** — audit table written by Talend Studio ETL jobs (VM `10.4.20.136`)
- **Talend Data Catalog** 11.1.0 — target for harvested metadata (VM `10.4.20.156`)

---

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 21+ |
| IntelliJ IDEA | Any recent version (Community or Ultimate) |
| WSL2 | Required for SSH tunnels to the on-premise VMs |

---

## Local configuration

The app uses `src/main/resources/application-local.yml` for local secrets (git-ignored).  
This file is created from the instructions below — never commit it.

Fill in your TDC credentials:

```yaml
tdc:
  base-url: http://localhost:11480   # via SSH tunnel
  api-path: /MM/api/v1
  default-model-id: Published        # name shown in TDC > Manage > Configuration Manager

  auth:
    type: basic
    username: <your-tdc-username>
    password: <your-tdc-password>

spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/talend_db   # via SSH tunnel
    username: talend_admin
    password: <db-password>
```

---

## Connectivity — SSH tunnels (required before every run)

The development machine (Windows) cannot reach the on-premise VMs directly.  
Twingate routes are WSL-only, so SSH tunnels are used to bridge both services to Windows localhost.

Open **two WSL terminals** and keep them running while the app is active:

**Terminal 1 — Talend Data Catalog (10.4.20.156)**
```bash
ssh -N -L 11480:localhost:11480 work-admin@10.4.20.156
```

**Terminal 2 — PostgreSQL / Talend Studio (10.4.20.136)**
```bash
ssh -N -L 5433:localhost:5432 work-admin@10.4.20.136
```

WSL2 automatically forwards these ports to Windows, so the Java process sees them as `localhost:11480` and `localhost:5433`.  
PostgreSQL sees the connection as coming from its own localhost, so no `pg_hba.conf` changes are needed.

> **Optional — persistent tunnels with autossh** (auto-reconnects on drop):
> ```bash
> sudo apt install autossh
> autossh -N -f -L 11480:localhost:11480 work-admin@10.4.20.156
> autossh -N -f -L 5433:localhost:5432   work-admin@10.4.20.136
> ```

---

## Build

Using the Gradle wrapper (no local Gradle installation required):

```powershell
# Windows (PowerShell / cmd)
.\gradlew.bat build

# WSL / Linux / macOS
./gradlew build
```

To build without running tests:

```powershell
.\gradlew.bat build -x test
```

The executable JAR is produced at `build/libs/metadata-framework-0.0.1-SNAPSHOT.jar`.

---

## Run tests

```powershell
.\gradlew.bat test
```

Test results are written to `build/reports/tests/test/index.html`.  
Tests mock both the database and TDC client, so **no SSH tunnels or running services are needed** to run the test suite.

---

## Run the application

### Option A — Gradle (command line)

```powershell
.\gradlew.bat bootRun
```

### Option B — IntelliJ IDEA run configuration

1. Open the project in IntelliJ: **File → Open** → select the project root folder.  
   IntelliJ will detect the Gradle build automatically.

2. Wait for Gradle sync to complete (progress bar in the bottom status bar).

3. Create a run configuration:
   - Go to **Run → Edit Configurations…**
   - Click **+** → **Spring Boot**
   - Set the fields:

   | Field | Value |
   |---|---|
   | Name | `MetadataFramework (local)` |
   | Main class | `com.talend.framework.metadata_framework.MetadataFrameworkApplication` |
   | Active profiles | `local` |
   | JRE | Java 21 (from toolchain) |

4. Click **OK**, then **Run** (▶) or **Debug** (🐛).

> **Tip:** IntelliJ picks up `application-local.yml` automatically because `spring.profiles.active=local` is set in `application.yml` and the local profile override file is on the classpath.

---

## Project structure

```
src/
├── main/java/.../
│   ├── api/          HarvestController       — REST endpoint to trigger harvesting
│   ├── audit/        AuditRecord, Parser      — reads ETL audit rows from Postgres
│   ├── config/       AppConfig, TdcProperties — configuration beans
│   ├── harvest/      HarvestService           — orchestrates audit → TDC flow
│   ├── model/        Dataset, LineageEdge, …  — domain model
│   └── tdc/          TdcRestClient            — POSTs metadata to TDC REST API
└── main/resources/
    ├── application.yml              — base config (committed)
    └── application-local.yml        — local secrets (git-ignored, created manually)
```