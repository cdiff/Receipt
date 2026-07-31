# Receipt Manager Phase 1: Base Setup & Home Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Android 영수증 관리 앱의 기본 Gradle 환경/Hilt/Room/Theme를 구축하고, 전달받은 이미지와 동일한 모던 & 프리미엄 스타일의 홈 화면(Home Screen) UI를 완성합니다.

**Architecture:** MVVM + Clean Architecture (Jetpack Compose UI, Hilt DI, Room DB Scaffold, StateFlow 기반 HomeUiState)

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Hilt, Room DB, Navigation Compose

## Global Constraints

- Android API Floor: minSdk 26, targetSdk 34/35
- Package Name: `com.pasic.receipt`
- UI Style: Screenshot Matching (Soft rounded corners, `#F5F7FF` light background, `#0A192F` dark navy FAB, `#4C8CFF` card blue, high contrast text)

---

### Task 1: Gradle Dependencies & Hilt Setup

**Files:**
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/pasic/receipt/ReceiptApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `@HiltAndroidApp` class `ReceiptApp`

- [ ] **Step 1: Update root `build.gradle.kts` with KSP and Hilt plugins**
- [ ] **Step 2: Add Compose, Hilt, Room, Navigation KTS dependencies to `app/build.gradle.kts`**
- [ ] **Step 3: Create `ReceiptApp.kt` annotated with `@HiltAndroidApp`**
- [ ] **Step 4: Register `ReceiptApp` in `AndroidManifest.xml`**

---

### Task 2: Design System & Color Palette

**Files:**
- Create: `app/src/main/java/com/pasic/receipt/ui/theme/Color.kt`
- Create: `app/src/main/java/com/pasic/receipt/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/pasic/receipt/ui/theme/Type.kt`

**Interfaces:**
- Produces: `ReceiptTheme`, Color tokens (`NavyPrimary`, `BlueCard`, `DarkCard`, `SlateCard`, `BackgroundLight`, `TextPrimary`, `TextSecondary`, `TrendRed`)

- [ ] **Step 1: Define exact Color palette matching design screenshot**
- [ ] **Step 2: Define Typography rules (Headlines, Body, Label bold formatting)**
- [ ] **Step 3: Create `ReceiptTheme` composable wrapper**

---

### Task 3: Room Database & Domain Model Scaffold

**Files:**
- Create: `app/src/main/java/com/pasic/receipt/data/local/entity/ReceiptEntity.kt`
- Create: `app/src/main/java/com/pasic/receipt/data/local/dao/ReceiptDao.kt`
- Create: `app/src/main/java/com/pasic/receipt/data/local/ReceiptDatabase.kt`
- Create: `app/src/main/java/com/pasic/receipt/data/repository/ReceiptRepository.kt`
- Create: `app/src/main/java/com/pasic/receipt/di/DatabaseModule.kt`

**Interfaces:**
- Produces: `ReceiptRepository` with `getAllReceipts()`, `insertReceipt()`, `getHomeSummary()`

- [ ] **Step 1: Create `ReceiptEntity` and `ExchangeRateEntity`**
- [ ] **Step 2: Create `ReceiptDao` with Flow query methods**
- [ ] **Step 3: Create `ReceiptDatabase` abstract class**
- [ ] **Step 4: Implement `ReceiptRepository` interface and Hilt DI module**

---

### Task 4: Home Screen Composable Components & UI Implementation

**Files:**
- Create: `app/src/main/java/com/pasic/receipt/ui/home/HomeScreen.kt`
- Create: `app/src/main/java/com/pasic/receipt/ui/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/pasic/receipt/ui/home/components/TotalSpendingHeader.kt`
- Create: `app/src/main/java/com/pasic/receipt/ui/home/components/MonthlyScoreCard.kt`
- Create: `app/src/main/java/com/pasic/receipt/ui/home/components/QuickActionGrid.kt`
- Create: `app/src/main/java/com/pasic/receipt/ui/home/components/RecentRegisteredCards.kt`
- Create: `app/src/main/java/com/pasic/receipt/ui/home/components/RecentReceiptsList.kt`
- Create: `app/src/main/java/com/pasic/receipt/ui/home/components/ReceiptBottomNavigation.kt`

**Interfaces:**
- Produces: `HomeScreen` Composable

- [ ] **Step 1: Build `TotalSpendingHeader` (26,809,600원 & trend text)**
- [ ] **Step 2: Build `MonthlyScoreCard` (이번달 내 점수, pill badges, pagination dots)**
- [ ] **Step 3: Build `QuickActionGrid` (스캔인증, 잔고확인, 교환하기, 더보기)**
- [ ] **Step 4: Build `RecentRegisteredCards` (MODERN CARD, THE BLACK, HYUNDAI horizontal carousel)**
- [ ] **Step 5: Build `RecentReceiptsList` (스타벅스 강남점, 명동교자 본점 cards)**
- [ ] **Step 6: Build `ReceiptBottomNavigation` & Floating Camera Button**
- [ ] **Step 7: Assemble `HomeScreen` and link to `HomeViewModel`**

---

### Task 5: MainActivity & NavHost Integration

**Files:**
- Modify: `app/src/main/java/com/pasic/receipt/MainActivity.kt`

- [ ] **Step 1: Set up AndroidEntryPoint in `MainActivity`**
- [ ] **Step 2: Implement Navigation Compose `NavHost` containing HomeScreen**
- [ ] **Step 3: Verify build and preview rendering**
