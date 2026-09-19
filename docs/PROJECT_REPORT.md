# SIH26001 Mobile Early-Warning Alert System
## Comprehensive Technical Architecture & Project Audit Report
*Repository: `SIH 2026/m_alert_system`*  
*Updated: September 18, 2026*

---

## 1. What the Project Is About

### 1.1 Mission & Operational Context
The **SIH26001 Mobile Early-Warning Alert System** is an Android edge client engineered for emergency alerting, evacuation guidance, and operational acknowledgment tracking in landslide and geohazard disaster scenarios. Designed for the Smart India Hackathon (SIH 2026), it serves emergency responders, district disaster management authorities (DDMAs), local field officials, and communities located in high-risk, rugged terrains (e.g., Uttarakhand, Himachal Pradesh, Assam, Meghalaya, and the Western Ghats).

The client acts as the critical "last-mile" warning receiver connecting the upstream **SIH26001 Hazard & Risk Engine** with humans on the ground.

```
+-------------------------------------------------------------+
|        SIH26001 Cloud Backend & Risk Inference Engine       |
|    (Rainfall, Soil Saturation, Pore-Pressure, InSAR, ML)    |
+-------------------------------------------------------------+
                               |
               FCM Push Trigger (alert_id ONLY)
                               v
+-------------------------------------------------------------+
|              SIH26001 Android Mobile Application            |
|                                                             |
|  [FCM Ingestion] -> [REST Ingestion] -> [Room Persistence]  |
|          |                  |                   |           |
|          v                  v                   v           |
|   [Alarm Audio]      [Active Alarm]     [Evacuation Route]  |
|   [Vibration]        [Safe Places]      [Offline ACK Queue] |
+-------------------------------------------------------------+
```

### 1.2 Non-Negotiable Contract & Safety Principles
The application is governed by strict core operational rules to guarantee data integrity, prevent false alarms, and preserve safety:

1. **The Client NEVER Calculates Risk**:
   Numerical risk scores, geotechnical stability indices, terrain slope degradation, and machine-learning hazard inference are performed **exclusively** on upstream servers. The mobile client never fabricates or re-calculates risk.
2. **Missing Data is Preserved as `null`**:
   If an environmental driver, expiry timestamp, or risk score is absent from a backend payload, it is stored and displayed as `null` / `"Unavailable"`. It is **strictly prohibited from defaulting to `0` or `0.0`**.
3. **Authoritative Identity & Deduplication**:
   Every alert is uniquely identified by its string `alert_id` (e.g., `ALT-2026-000123`). The `alert_id` drives deduplication across in-memory LRU caches, Room database primary keys, and backend acknowledgment sync records.
4. **Local Silence vs. Operational Acknowledgment**:
   - **`SILENCE`**: A purely local UX action. Silencing stops looping audio alarms and haptic vibrations. It does **NOT** acknowledge the alert and does **NOT** notify upstream services.
   - **`ACKNOWLEDGE`**: An operational safety commitment confirming verified human receipt by emergency personnel. It updates local persistent state and enqueues a durable record for server synchronization.
5. **No False `COMPLETED` Sync State**:
   Until the backend team supplies an authoritative HTTP ACK contract, pending acknowledgments remain honestly displayed as `ACKNOWLEDGED • SYNC PENDING` or `ACKNOWLEDGED • SYNC PENDING (RETRYING)`. The app never fakes server synchronization.

---

## 2. What Things Are in the Project (Complete Inventory)

The codebase is organized under clean architectural boundaries (`domain`, `data`, `presentation`, `core`, and `di`):

```
alert-system/app/src/main/java/com/sih26001/mobilealert/
├── MainActivity.kt
├── MainViewModel.kt
├── MobileAlertApplication.kt
├── core/
│   ├── alarm/          # Audio playback & haptic vibration
│   ├── navigation/     # Jetpack Compose navigation & deep links
│   ├── notification/   # Android notification channels & dispatch
│   ├── ui/theme/       # Material3 typography, color palette, design tokens
│   └── util/           # Constants, EnvironmentConfig, LocaleManager
├── data/
│   ├── ack/            # ACK synchronization state machine, retries, recovery
│   ├── fcm/            # Push message filtering, deduplication, ingestion
│   ├── local/          # Room database, DAOs, entities, migrations, type converters
│   ├── mapper/         # DTO to domain mappers
│   ├── mock/           # Static test data fixtures
│   ├── remote/         # Retrofit API interface, OkHttp client, DTOs, validator
│   └── repository/     # Room + Retrofit repository implementation & in-memory mock
├── di/
│   └── DependencyContainer.kt # Manual singleton dependency injection container
└── presentation/
    ├── activealarm/    # High-urgency alert screen, timer, silence & ACK actions
    ├── alerts/         # Active alert list & simulation controls
    ├── history/        # Historical archive of resolved & expired alerts
    ├── home/           # Main disaster readiness dashboard & guides
    ├── route/          # Visual evacuation canvas & turn-by-turn guidance
    └── safeplace/      # Emergency shelters & safe assembly points list
```

### 2.1 Domain Layer
- `Alert.kt`: Immutable domain model holding alert payload fields (`alertId`, `eventType`, `severity`, `riskScore`, `location`, `issuedAt`, `expiresAt`, `topDrivers`, `recommendedAction`, `affectedAssets`, `source`, `dataQuality`, `requiresAck`, `status`, `receivedAt`, `acknowledgedAt`).
- `AlertSeverity.kt`: Severity enum (`NORMAL`, `HIGH`, `CRITICAL`).
- `AlertStatus.kt`: Lifecycle enum (`RECEIVED`, `DISPLAYED`, `ACTIVE`, `SILENCED`, `ACKNOWLEDGED`, `EXPIRED`).
- `Location.kt`: Spatial model (`name`, `latitude`, `longitude`).
- `AffectedAsset.kt`: Infrastructure impact model (`type`, `identifier`).
- `DeviceSettings.kt`: User preference parameters for audio, vibration, and channels.
- `AlertRepository.kt`: Core contract abstraction defining read streams and mutation suspend functions.
- `AcknowledgeAlertUseCase.kt`, `GetActiveAlertsUseCase.kt`, `GetAlertHistoryUseCase.kt`.

### 2.2 Data Layer (Remote & Ingestion)
- `AlertApiService.kt`: Retrofit interface declaring `@GET("api/v1/alerts")`.
- `RetrofitProvider.kt`: Configures OkHttpClient, timeouts (30s), logging interceptor, and Gson serialization.
- `EnvironmentConfig.kt`: Centralizes environment URLs (`DEBUG` -> `http://127.0.0.1:8080/`, `STAGING`, `PRODUCTION`).
- `AlertDto.kt`: DTO wire representations matching JSON structure.
- `AlertValidator.kt`: Validates incoming DTOs against geographic bounds, ISO-8601 formatting, and required fields.
- `AlertMapper.kt`: Safe conversion from `AlertDto` to `Alert` preserving nullable types.

### 2.3 Data Layer (Local Persistence & Room)
- `AppDatabase.kt`: Room database (version 3) registering `AlertEntity` and `PendingAckEntity`.
- `AlertEntity.kt`: SQLite table `alerts` with `alertId` as primary key.
- `PendingAckEntity.kt`: SQLite table `pending_acks` storing offline operational ACKs.
- `AlertDao.kt`: SQL queries for reactive observation, state mutation, and `deleteAllAlerts()`.
- `PendingAckDao.kt`: Methods for eligible ACK filtering (`status != 'COMPLETED'`) and conflict resolution.
- `Converters.kt`: Converts `Instant` to SQLite `INTEGER`, enum names to strings, and asset lists to JSON.
- `MIGRATION_2_3`: Non-destructive schema migration upgrading `pending_acks` to version 3 with typed `INTEGER` timestamp columns.
- `DatabaseTransactionRunner.kt`: Interface abstracting `database.withTransaction {}` for JVM testing.

### 2.4 Data Layer (ACK State Machine & Sync Engine)
- `AckSyncDataSource.kt`: Transport interface with `UnavailableAckSyncDataSource` enforcing contract safety.
- `AckSyncEngine.kt`: Coordinates state transitions, in-flight mutex locks, stale recovery, and error recording.
- `AckRetryPolicy.kt`: `ExponentialBackoffAckRetryPolicy` (2s base, 2.0 multiplier, 5m cap, 10 retries).
- `AckRecoveryPolicy.kt`: Recovers stale `IN_FLIGHT` records (> 5 minutes) after process death back to `FAILED` for retry eligibility.
- `NetworkConnectivityMonitor.kt`: Validates active Internet capability via `ConnectivityManager`.
- `AckSyncCoordinator.kt`: Listens to Android system network callbacks and triggers recovery/reconciliation on reconnect.
- `AckSyncEventLogger.kt`: Telemetry logger for ACK lifecycle events.

### 2.5 Data Layer (FCM Push Pipeline)
- `SihFirebaseMessagingService.kt`: Service receiving push triggers (`exported="false"`).
- `FcmMessageProcessor.kt`: Thread-safe LRU cache detecting and ignoring duplicate push notifications.
- `FcmAlertTriggerHandler.kt`: Coordinates fetching authoritative alert payloads over REST upon push receipt.

### 2.6 Presentation & UI Layer (Jetpack Compose)
- `HomeScreen.kt` & `HomeViewModel.kt`: Dashboard with primary alert banners, weather/rainfall risk summary, quick action cards, emergency contacts, and evacuation guides.
- `ActiveAlarmScreen.kt` & `ActiveAlarmViewModel.kt`: High-urgency screen rendering warning level colors, risk drivers, `SILENCE ALARM`, `ACKNOWLEDGE`, and sync state labels.
- `RouteScreen.kt`: Canvas-rendered evacuation map showing safe paths, hazard boundaries, blocked roads, and external GPS map launching.
- `SafePlaceScreen.kt`: Directory of emergency shelters and safe assembly points with live capacity bars and facility badges (medical, food, power, satellite comms).
- `AlertsScreen.kt`: List of active alerts with developer simulation buttons.
- `HistoryScreen.kt`: Chronological audit log of acknowledged and expired hazards.
- `LocaleManager.kt`: Runtime dynamic localization supporting English (`en`), Hindi (`hi`), and Assamese (`as`).
- `AppNavigation.kt`: Deep link handling (`sih26001://alert/{alertId}`) and back-stack routing.

### 2.7 Tooling & Knowledge Base
- `server.py`: Standalone Python 3 mock server on port 8080.
- `graphify-out/`: Codebase knowledge graph (922 nodes, 2012 edges, 60 community hubs) providing navigable AST relationships.
- `docs/alert-contract.md` & `docs/ack-sync.md`: Upstream and client contract specifications.

---

## 3. How Things Are Implemented in the Project

### 3.1 Push-to-Fetch Authoritative Data Flow
To prevent untrusted or malformed push payloads from polluting client state, the app uses push messages strictly as signals:
1. **FCM Message Delivery**: Upstream sends a push payload containing only `{"alert_id": "ALT-2026-000123"}`.
2. **Deduplication**: `SihFirebaseMessagingService` hands data to `FcmMessageProcessor`. An in-memory synchronized LRU cache checks if the ID was processed recently.
3. **Authoritative Fetch**: `FcmAlertTriggerHandler` calls `AlertRepository.refreshAlert(alertId)` via Retrofit REST.
4. **Validation**: `AlertValidator` inspects the response to verify that `alert_id`, `event_type`, `severity`, and timestamps are valid and coordinates fall within $[-90, 90]$ and $[-180, 180]$.
5. **Mapping & Expiry**: `AlertMapper` converts the DTO to an immutable domain `Alert`. If `expires_at` is in the past, status is set to `EXPIRED`.
6. **Persistence**: The alert is saved to Room table `alerts` using `OnConflictStrategy.REPLACE`.
7. **Alarm Trigger**: If the alert is `ACTIVE` and severity is `HIGH` or `CRITICAL`, `AlarmController` initiates looping sound and vibration, while `AlertNotificationManager` posts a high-priority system notification.

### 3.2 Operational ACK State Machine
```
[User taps ACKNOWLEDGE in ActiveAlarmScreen or HomeScreen]
                          |
                          v
         AcknowledgeAlertUseCase(alertId)
                          |
                          v
         AlertRepositoryImpl.acknowledgeAlert(alertId)
    ================== Room Transaction ==================
    * AlertDao.updateStatus(alertId, ACKNOWLEDGED)
    * AlertDao.updateAcknowledgedAt(alertId, now)
    * PendingAckDao.insertOrIgnore(PendingAckEntity(status = PENDING))
    ======================================================
                          |
                          v
        AckSyncCoordinator.triggerSync() -> AckSyncEngine
                          |
                          v
+------------------ State Transitions ------------------+
|                                                       |
|  PENDING ------------------------------------------+  |
|     |                                              |  |
|  (In-flight Lock Acquired)                         |  |
|     v                                              |  |
|  IN_FLIGHT                                         |  |
|     |                                              |  |
|     +---> (Transport Success: 2xx) ---> COMPLETED  |  |
|     |                                              |  |
|     +---> (Transport Failure / Unavailable)        |  |
|                 |                                  |  |
|                 v                                  |  |
|              FAILED                                |  |
|                 |                                  |  |
|        (Exponential Backoff Elapsed)               |  |
|                 |                                  |  |
|                 +----------------------------------+  |
+-------------------------------------------------------+
```

- **In-Flight Concurrency Lock**: `AckSyncEngine` maintains a `Mutex` guarding an `inFlightAlerts` set. Concurrent reconciliation cycles cannot double-submit the same alert.
- **Process Death & Stale Recovery**: If Android terminates the process while a record is `IN_FLIGHT`, `AckRecoveryPolicy.isStale()` marks records untouched for $> 5$ minutes as `FAILED` on app restart so they re-enter the backoff cycle.
- **Cancellation Safety**: Suspend loops rethrow `CancellationException` to permit clean coroutine teardown without swallowing cancellations.
- **Overflow-Safe Exponential Backoff**: Delay formula:
  $$\text{Delay} = \min(\text{baseDelay} \times \text{multiplier}^{\min(\text{retryCount}, 30)}, \text{maxDelay})$$
  with base delay 2s, multiplier 2.0, max cap 5 minutes (300s), and max retries 10.

### 3.3 Dynamic Multi-Language System
Implemented via `LocaleManager.kt`:
- Exposes `AppLanguage` enum (`ENGLISH`, `HINDI`, `ASSAMESE`).
- `ProvideAppLocale` wraps the root Compose hierarchy, intercepts `LocalConfiguration`, and overrides `LocalContext` with `context.createConfigurationContext(configuration)`.
- Language toggles update instantly in the UI without restarting `MainActivity`.

### 3.4 Evacuation Route & Shelter Visualization
- `RouteScreen.kt`: Uses Jetpack Compose `Canvas` to draw GIS grids, hazard risk perimeters, road blockages extracted from `alert.affectedAssets`, user location markers, and evacuation waypoints. Includes an external GPS intent launcher (`geo:0,0?q=latitude,longitude(Label)`).
- `SafePlaceScreen.kt`: Renders shelters, current vs. maximum capacity bars, and operational facilities (medical, rations, solar power, satellite link).

---

## 4. What Things Are NOT Set Up for the Project

1. **Production Backend Hosting & Staging Environments**:
   - The app currently points to `http://127.0.0.1:8080/` (accessed via `adb reverse tcp:8080 tcp:8080` from an emulator/device).
   - Production and staging URLs in `EnvironmentConfig.kt` remain unconfigured placeholders (`https://api.placeholder.com/`).
2. **Production Firebase Cloud Messaging Credentials**:
   - `google-services.json` contains development/demo Firebase project bindings. Production service accounts, APNs certificates, and enterprise push channels are not provisioned.
3. **Authoritative Backend ACK REST Endpoint**:
   - The backend team has **not yet deployed or published** an HTTP endpoint, route (e.g. `POST /api/v1/alerts/{id}/ack`), or wire schema for operational acknowledgments.
4. **Remote Idempotency Protocol**:
   - No backend headers (such as `Idempotency-Key`) or server-side duplicate acknowledgment policies have been defined upstream.
5. **Production Authentication & Authorization**:
   - No user identity, responder credentials, JWT token exchange, mTLS, or role-based access control (RBAC) headers exist in networking.
6. **WorkManager Periodic Background Worker**:
   - Intentionally deferred. Running periodic background sync while the backend ACK endpoint does not exist would needlessly drain device battery by repeatedly hitting the unavailable transport.

---

## 5. What Things Are NOT Completed in the Project

1. **`HttpAckSyncDataSource` (Contract-Gated)**:
   - Concrete network synchronization of ACKs is intentionally blocked until the backend team publishes the authoritative ACK wire contract. The client uses `UnavailableAckSyncDataSource`.
2. **Transition to `COMPLETED` on Real Devices**:
   - Because no server endpoint exists to confirm receipt, acknowledgments on real devices remain in `ACKNOWLEDGED • SYNC PENDING` or `ACKNOWLEDGED • SYNC PENDING (RETRYING)`.
3. **Backend-Driven Route & Shelter Ingestion**:
   - Shelter lists and evacuation paths in `RouteScreen` and `SafePlaceScreen` currently use local models and heuristics; dynamic server endpoints for real-time road closures and shelter occupancy are not yet defined.
4. **Hardware Emergency Broadcast Bypass**:
   - The app operates within standard Android platform boundaries; it does not possess specialized firmware or carrier-level permissions to override hardware mute switches or aggressive OEM battery managers without user configuration.

---

## 6. Phases Implemented in the Project

| Phase | Milestone Title | Delivered Features & Technical Scope | Verification & Tests |
| :---: | :--- | :--- | :--- |
| **1** | **Architecture & Domain Foundation** | Clean architecture skeleton; immutable domain models (`Alert`, `AlertSeverity`, `AlertStatus`); contract rules prohibiting client risk calculation and preserving null values. | Model immutability & validation unit tests. |
| **2** | **In-Memory Mock Repository & Testing Controls** | Created `MockAlertRepository` with reactive `StateFlow` streams, deduplication, and developer controls for testing scenarios. | Repository unit tests. |
| **3** | **Compose UI Foundation** | Initial Jetpack Compose and Material3 screens (`HomeScreen`, `AlertsScreen`, `HistoryScreen`, and `ActiveAlarmScreen`). | Compose previews & UI instrumentation tests. |
| **4** | **System Notifications & Alarm UX** | Notification Channels (`CRITICAL`, `HIGH`, `NORMAL`), looping audio via `MediaPlayer`, haptic vibration via `Vibrator`, and full-screen deep links. | Notification manager unit tests. |
| **5** | **Authoritative REST Networking & Validation** | Retrofit REST client (`GET api/v1/alerts`), `AlertValidator`, `AlertMapper`, and standalone Python HTTP `mock-server`. | `AlertValidatorTest`, `AlertMapperTest`, mock server integration. |
| **6A–6D** | **Room Persistence & Database** | Room database (`alerts.db`), `AlertDao`, `PendingAckDao`, `AlertEntity`, `PendingAckEntity`, type converters, and `AlertRepositoryImpl`. | DAO unit tests & transactional persistence tests. |
| **6E–6F** | **Offline ACK Queuing & State Machine** | `AckSyncEngine`, `ExponentialBackoffAckRetryPolicy`, `DefaultAckRecoveryPolicy`, `AndroidNetworkConnectivityMonitor`, and `AckSyncCoordinator`. | `AckSyncEngineTest`, `AckRetryPolicyTest`, `AckRecoveryPolicyTest`. |
| **7** | **Backend Contract Integration Boundary Audit** | Enforced contract gate: added `UnavailableAckSyncDataSource`; verified local ACKs never fabricate server success. | Boundary audit & contract gate verification. |
| **8** | **ACK Lifecycle Hardening & Observability** | Added diagnostic telemetry fields (`lastFailureAt`, `lastFailureMessage`, `completedAt`), `MIGRATION_2_3`, `AckSyncEventLogger`, and exception isolation. | Schema migration tests & telemetry audit. |
| **9** | **Authoritative ACK Contract Discovery Audit** | Comprehensive repository discovery audit verifying that the backend ACK contract is unavailable; strictly halted transport implementation. | Discovery audit report. |
| **10** | **Final Client Readiness & Reliability Audit** | Fixed `MIGRATION_2_3` INTEGER column types; added coroutine cancellation safety across all pipelines; resolved test fake compilation issues. | 100 unit tests passing; `./gradlew assembleDebug` successful. |
| **10+** | **Evacuation Routing, Shelters & Multi-Language** | Added `RouteScreen` (Canvas path visualizer), `SafePlaceScreen` (emergency shelters), `LocaleManager` (English, Hindi, Assamese), modernized `HomeScreen`, and integrated `graphify` knowledge graph. | Full test suite verified (100 passed); debug build verified. |

---

## 7. Current Project Verification Summary

- **Unit Test Suite**: **100 tests executed, 100 passed, 0 failures, 0 skipped**.
- **Build Status**: `./gradlew assembleDebug` builds cleanly with 0 errors.
- **Knowledge Graph**: 922 nodes, 2012 edges, 60 community hubs synchronized in `graphify-out/`.
- **Integration Blocker**: The **Authoritative Backend ACK Contract Specification** from the backend engineering team remains the **SOLE BLOCKER** to Phase 11 (HTTP ACK transport implementation).
