# talend-data-harvesting-framework

Middleware between an ETL audit table and Talend Data Catalog (TDC).

TDC harvests the PostgreSQL database directly (via a JDBC Imported Model) so it knows the tables and columns — *the dots*. But a database doesn't record *how* its tables were populated, so TDC can't draw lineage on its own. This service supplies that missing layer from the ETL audit table: it reads the audit records (job name/id, source & target table/schema), then automatically:

1. **Refreshes** the harvested JDBC model so the catalog matches the live DB
2. **Generates** a lineage SQL file from the audit records
3. **Delivers** the SQL to the TDC VM via SFTP (SSH)
4. **Triggers** TDC to import it, which builds the source→target lineage graph

TDC then stitches the lineage model to the harvested JDBC model in a Configuration — *connecting the dots* — with zero manual steps after initial setup.

Because it is driven off a generic audit table — not Talend-specific internals — it works for any ETL tool that writes the audit table.

- **Spring Boot** 4.0.6 · **Java** 21 · **Gradle** (wrapper included)
- **PostgreSQL** — audit table written by Talend Studio ETL jobs (VM `10.4.20.136`)
- **Talend Data Catalog** 11.1.0 — target for harvested metadata (VM `10.4.20.156`)

---

## How it works — automated flow

```
Spring Boot app
  │
  ├─1─ Read audit_table from PostgreSQL (10.4.20.136 via SSH tunnel)
  │      └─ AuditRecord rows → ParsedAuditRecord → JobLineageGraph
  │
  ├─2─ Generate lineage SQL (Data Mapping Script DSL)
  │      └─ LineageSqlGenerator: JobLineageGraph → SQL string
  │         e.g. SELECT col AS col INTO public."bronze_table"@"PostgreSQL"
  │                                  FROM "file.csv"@"FileSystem";
  │
  ├─3─ SFTP the SQL to TDC VM (10.4.20.156, direct TCP)
  │      └─ TdcSshClient (JSch): writes to /home/work-admin/SQL/customer_pipeline.sql
  │
  ├─4─ POST /MM/api/ImportHarvestableContent  (contentId = Data Mapping Script)
  │      └─ TDC parses the SQL, builds the lineage model version
  │
  └─5─ POST /MM/api/ImportHarvestableContent  (contentId = PostgreSQL JDBC model)
         └─ TDC re-harvests the live DB schema to sync "the dots"
```

**Auth**: `TdcSession` logs in once via `POST /MM/rest/v1/auth/login` and sends the token as the `x-auth-token` cookie on internal `/MM/api/` calls (the form the TDC web UI itself uses — captured via DevTools).

**No manual steps once set up.** The only one-time setup is creating the models in the TDC UI (see below).

---

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 21+ |
| IntelliJ IDEA | Any recent version (Community or Ultimate) |
| WSL2 | Required for SSH tunnels to the on-premise VMs |

---

## One-time TDC setup (do this once, then never again)

These steps create the model shells in TDC. After this, everything is automated.

### Step 1 — Create the PostgreSQL JDBC model

1. Open TDC at `http://localhost:11480` (via SSH tunnel)
2. **Manage → Configuration Manager → New**
3. Bridge: **PostgreSQL Database (via JDBC)**
4. Name: `PostgreSQL Database (via JDBC)`
5. Import Setup: Host `10.4.20.136`, Port `5432`, Database `talend_db`, User `talend_admin`, Password `admin`
6. Save, then click **Import** (Full Source Import: Yes) to run the first harvest
7. Note the **content ID** from the browser URL: `objectId=-1_XX` → the value `-1_XX`

### Step 2 — Create the Data Mapping Script model

1. **Manage → Configuration Manager → New**
2. Bridge: **Data Mapping Script**
3. Name: `Data Mapping Script`
4. Import Setup: File path → `/home/work-admin/SQL/customer_pipeline.sql`
5. Save (do not import yet — the app will write the file and trigger import automatically)
6. Note the **content ID** from the browser URL: `objectId=-1_XX` → the value `-1_XX`

### Step 3 — Note the Configuration ID

In the browser URL when either model is selected: `configId=-1_XX` → the value `-1_XX`

### Step 4 — Fill in `application-local.yml`

```yaml
tdc:
  base-url: http://localhost:11480
  auth:
    username: Administrator
    password: Administrator
  harvested-model-content-id: "-1_56"   # from Step 1 (your value may differ)
  lineage-model-content-id:   "-1_39"   # from Step 2 (your value may differ)
  config-id:                  "-1_4"    # from Step 3 (your value may differ)
  ssh:
    host: 10.4.20.156                   # TDC VM — direct, no tunnel needed for SSH
    username: work-admin
    password: <your-ssh-password>
    remote-sql-path: /home/work-admin/SQL/customer_pipeline.sql
```

> **Important**: The SSH connection goes **directly** to `10.4.20.156` — not through localhost. TDC REST API calls go through `localhost:11480` (the SSH tunnel). These are two separate connections.

---

## Local configuration

The app uses `src/main/resources/application-local.yml` for local secrets (git-ignored).

Fill in the values from the one-time setup above, plus the database credentials:

```yaml
tdc:
  base-url: http://localhost:11480
  auth:
    username: Administrator
    password: Administrator
  harvested-model-content-id: "-1_56"
  lineage-model-content-id:   "-1_39"
  config-id:                  "-1_4"
  ssh:
    host: 10.4.20.156
    username: work-admin
    password: <your-ssh-password>
    remote-sql-path: /home/work-admin/SQL/customer_pipeline.sql

spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/talend_db
    username: talend_admin
    password: admin
```

---

## Connectivity — SSH tunnels (required before every run)

The development machine (Windows) cannot reach the on-premise VMs directly.  
Twingate routes are WSL-only, so SSH tunnels are used to bridge the REST API and the PostgreSQL database to Windows localhost.

Open **two WSL terminals** and keep them running while the app is active:

**Terminal 1 — Talend Data Catalog REST API (10.4.20.156)**
```bash
ssh -N -L 11480:localhost:11480 work-admin@10.4.20.156
```

**Terminal 2 — PostgreSQL / Talend Studio (10.4.20.136)**
```bash
ssh -N -L 5433:localhost:5432 work-admin@10.4.20.136
```

WSL2 automatically forwards these ports to Windows, so the Java process sees them as `localhost:11480` and `localhost:5433`.  
PostgreSQL sees the connection as coming from its own localhost, so no `pg_hba.conf` changes are needed.

> **Note**: The SFTP delivery of the lineage SQL connects **directly** to `10.4.20.156:22` — this uses Twingate, which works from WSL but **also works from Windows Java** as a direct TCP connection. No tunnel needed for SSH port 22.

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

## Trigger a harvest

With the app running and both WSL tunnels open, run this in PowerShell:

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/harvest/job/ETL_MetadataDrivenCSV2Postgres_2" | ConvertTo-Json -Depth 8
```

This triggers the full end-to-end flow:

1. Reads all audit rows for the job from PostgreSQL
2. Builds the lineage graph (file → bronze → silver → gold)
3. Refreshes the JDBC model in TDC so the catalog matches the live DB
4. Generates the Data Mapping Script SQL and SFTPs it to the TDC VM
5. Triggers TDC to import the lineage SQL, building the source → target lineage graph

The response JSON shows how many edges were pushed, whether the model refresh succeeded, and any failures.

To preview what the lineage graph looks like before pushing it to TDC:

```powershell
Invoke-RestMethod "http://localhost:8080/harvest/job/ETL_MetadataDrivenCSV2Postgres_2/lineage" | ConvertTo-Json -Depth 8
```

---

## Project structure

```
src/
├── main/java/.../
│   ├── api/          HarvestController         — REST endpoint to trigger harvesting
│   ├── audit/        AuditRecord, Parser        — reads ETL audit rows from Postgres
│   ├── config/       AppConfig, TdcProperties   — configuration beans
│   ├── harvest/      HarvestService             — orchestrates the full flow
│   │                 LineageBuilder             — builds JobLineageGraph from audit rows
│   │                 LineageSqlGenerator        — converts graph → Data Mapping Script SQL
│   ├── model/        Dataset, LineageEdge, …    — domain model
│   └── tdc/          TdcSession                 — login / token management
│                     TdcRestClient              — triggers TDC imports via /MM/api/
│                     TdcSshClient               — SFTP delivery of lineage SQL to TDC VM
└── main/resources/
    ├── application.yml              — base config (committed)
    └── application-local.yml        — local secrets (git-ignored, created manually)
```

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
    username: <your-tdc-username>   # session login — POSTed to /MM/j_spring_security_check
    password: <your-tdc-password>

  # Internal /MM/api operation paths used to POST metadata. These are not
  # publicly documented — capture the exact path (and JSON payload) from
  # Chrome DevTools > Network while performing the action in the TDC UI.
  api:
    dataset-path: <e.g. /MM/api/...>
    lineage-path: <e.g. /MM/api/...>

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
│   └── tdc/          TdcSession, TdcRestClient — session login + authenticated POST to TDC
└── main/resources/
    ├── application.yml              — base config (committed)
    └── application-local.yml        — local secrets (git-ignored, created manually)
```