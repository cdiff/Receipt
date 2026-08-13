# 🧾 Receipt

> 영수증 찍으면 알아서 인식, 정리, 내보내기까지 — 사장님과 직장인을 위한 올인원 영수증 관리 앱

<p>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Min_SDK-26-blue?style=flat-square"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white"/>
  <img src="https://img.shields.io/badge/Architecture-MVVM-orange?style=flat-square"/>
  <img src="https://img.shields.io/badge/License-Proprietary-lightgrey?style=flat-square"/>
</p>

---

## ✨ 주요 기능 (Key Features)

### 📸 1. AI 스마트 영수증 스캔 & OCR
- **자동 모서리 인식 & 잘라내기**: 영수증 촬영 시 모서리를 자동으로 감지하고 자르기 보정을 수행합니다.
- **스마트 정보 자동 추출**: 영수증 실물 사진에서 상호명, 결제금액, 결제일자, 부가세, 결제수단 및 카테고리를 AI로 자동 파싱합니다.
- **흑백 문서 향상 모드**: 선명도를 높여 훼손된 영수증의 글자 인식률을 극대화합니다.

### 📊 2. 소비 통계 & 지출 리포트
- **월/주/일별 동적 차트**: 최근 소비 패턴을 실시간 집계하고 직관적인 바 차트로 시각화합니다.
- **카테고리별 지출 분석**: 식비, 교통비, 사무용품 등 카테고리별 소비 비중을 한눈에 파악할 수 있습니다.
- **스마트 검색 & 필터링**: 날짜 범위, 카테고리 칩 필터, 상호명 검색으로 원하는 영수증을 빠르게 조회합니다.

### 📄 3. 지출결의서 PDF & CSV/ZIP 내보내기
- **PDF 지출결의서 생성**: 회사 제출 및 증빙에 최적화된 고급 레이아웃의 PDF 문서를 자동 생성합니다.
- **CSV & 이미지 ZIP 내보내기**: 엑셀 연동을 위한 CSV 파일과 영수증 원본 사진 묶음을 한 번에 내보냅니다.
- **맞춤 포맷 설정**: 기본 작성자, 부서, 사용 목적, 날짜 포맷, 금액 표기 형식을 원하는 대로 커스텀할 수 있습니다.

### 💾 4. 데이터 백업 & 복원 (.zip)
- **100% 원스톱 데이터 보존**: DB 영수증 내역(`backup_data.json`)과 실물 영수증 사진을 하나의 압축파일(.zip)로 안전하게 백업합니다.
- **중복 방지 & 원본 시각 복원**: 기기를 변경하거나 앱을 재설치하더라도 원본 결제시각 그대로 중복 없이 완벽히 복구합니다.

### 🎨 5. 프리미엄 디자인 & 테마
- **글래스모피즘 & 모던 뷰**: Haze 라이브러리 기반 은은한 블러 탑바 및 깔끔한 플랫 구분선 레이아웃.
- **테마 지원**: 시스템 기본값, 라이트 모드, 다크 모드 3가지 카드형 테마를 지원합니다.
- **커스텀 토스트 바**: 안드로이드 기본 아이콘을 제거하고 반투명 연회색 캡슐 플로팅 토스트 바를 제공합니다.

---

## 🛠️ 기술 스택 (Tech Stack)

| 레이어 | 사용 기술 |
|---|---|
| **Language** | Kotlin (100%) |
| **UI Framework** | Jetpack Compose, Material 3, Haze (Glassmorphism) |
| **Architecture** | MVVM + Clean Architecture, Hilt (Dependency Injection) |
| **Database & Persistence** | Room DB, DataStore Preferences |
| **Async & Flow** | Kotlin Coroutines, StateFlow, SharedFlow |
| **Icons & UI Utility** | Lucide Icons, Custom Dropdown Spinner, Custom Wheel Picker |
| **Build & Tools** | Gradle (KTS), KSP, Hilt Android |

---

## 🚀 시작하기 (Getting Started)

### 사전 요구 사항

| 항목 | 버전 |
|---|---|
| Android Studio | Ladybug 이상 권장 |
| JDK | 17 이상 |
| Android SDK | API Level 26+ (Android 8.0 Oreo 이상) |
| Google Services | `google-services.json` 발급 필요 |

### 빌드 설정

1. **프로젝트 클론**
   ```bash
   git clone https://github.com/cdiff/Receipt.git
   cd Receipt
   ```

2. **`google-services.json` 배치**
   - [Firebase Console](https://console.firebase.google.com/)에서 프로젝트를 생성하고 `google-services.json` 파일을 다운로드합니다.
   - 다운로드한 파일을 `app/` 디렉토리에 복사합니다.
   ```text
   Receipt/
   └── app/
       └── google-services.json  ← 여기에 배치
   ```

3. **Gemini API Key 설정** (AI 스캔 기능 사용 시)
   - `local.properties` 파일에 Gemini API Key를 추가합니다.
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```

4. **빌드 및 실행**
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📁 프로젝트 구조

<details>
<summary>패키지 구조 보기</summary>

```text
com.pasic.receipt
├── data
│   ├── local           (Room Database & Entities)
│   ├── preferences     (UserPreferences DataStore)
│   └── repository      (ReceiptRepository)
├── ui
│   ├── home            (소비 차트 & 메인 뷰)
│   ├── receipts        (영수증 목록 & 상세 페이지)
│   ├── scan            (카메라 스캔 & OCR 결과 검증)
│   ├── export          (PDF/CSV 지출결의서 생성)
│   └── settings        (설정, 백업/복원)
│       ├── components
│       ├── csv
│       ├── dialogs
│       └── sections
└── util                (PDF 리포트 생성기, 이미지 압축기)
```

</details>

---

## 📝 라이선스 (License)
Copyright © 2026 PASIC Receipt Team. All rights reserved.
