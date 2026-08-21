# 🧾 영수증 쏙 (Receipt)

> **영수증 찍으면 알아서 인식, 정리, 내보내기까지 — 사장님과 직장인을 위한 올인원 AI 영수증 관리 앱**

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Min_SDK-26-blue?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
  <img src="https://img.shields.io/badge/Architecture-MVVM-orange?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Database-Room_DB-47A248?style=for-the-badge&logo=sqlite&logoColor=white"/>
</p>

---

## 📱 앱 미리보기 (App Preview)

<div align="center">
  <table>
    <tr>
      <th width="33%">🏠 홈 화면</th>
      <th width="33%">🧾 영수증 목록 & 필터</th>
      <th width="33%">📤 지출결의서 내보내기</th>
    </tr>
    <tr>
      <td align="center">
        <img src="docs/images/홈화면 1.jpg" width="240" alt="홈 화면"/>
      </td>
      <td align="center">
        <img src="docs/images/영수증 목록 화면.jpg" width="240" alt="영수증 목록"/>
      </td>
      <td align="center">
        <img src="docs/images/내보내기 화면.jpg" width="240" alt="내보내기 화면"/>
      </td>
    </tr>
  </table>
</div>

---

## ✨ 핵심 기능 (Key Features)

### 📸 1. AI 스마트 영수증 스캔 & OCR
- **Gemini AI 기반 초정밀 OCR**: 영수증 사진 한 장으로 상호명, 결제금액, 결제일시, 결제수단 및 카테고리를 실시간 자동 파싱합니다.
- **AI 카테고리 추천 & 즉시 생성**: 기존에 없는 새로운 업종도 AI가 스마트하게 감지하여 터치 한 번으로 카테고리를 자동 생성합니다.
- **단계별 스캔 피드백 & 실패 대응**: 1단계 텍스트 스캔 ➔ 2단계 정보 추출 ➔ 3단계 카테고리 분류 모션과 함께 실패 시 친절한 재촬영 가이드를 제공합니다.

<div align="center">
  <table>
    <tr>
      <th width="50%">✨ 스캔 및 AI 자동 추출 성공</th>
      <th width="50%">⚠️ 영수증 인식 실패 & 재촬영 안내</th>
    </tr>
    <tr>
      <td align="center">
        <img src="docs/gif/영수증 스캔 성공 영상.gif" width="260" alt="스캔 성공"/>
      </td>
      <td align="center">
        <img src="docs/gif/영수증 스캔 실패 영상.gif" width="260" alt="스캔 실패"/>
      </td>
    </tr>
  </table>
</div>

---

### 📊 2. 스마트 소비 통계 & 카테고리 분석
- **월/주/일별 실시간 동적 차트**: 소비 패턴을 터치 한 번으로 월별, 주별, 일별 막대 차트로 시각화합니다.
- **인터랙티브 3dp 평행 도넛 차트**: 3dp 균일 평행 분할선이 적용된 도넛 차트를 탭하면 1위 지출 금액과 비율이 스프링 버블 모션으로 뿅! 나타납니다.
- **100% 온디바이스 카테고리 관리**: Room DB 기반 `categories` 마스터 테이블과 실시간 Flow로 연동되어 하드코딩 없이 동작합니다.

<div align="center">
  <table>
    <tr>
      <th width="50%">📈 월/주/일별 소비 차트 전환</th>
      <th width="50%">🍩 카테고리별 도넛 인터랙션</th>
    </tr>
    <tr>
      <td align="center">
        <img src="docs/gif/월,주,일별 사용량.gif" width="260" alt="사용량 차트"/>
      </td>
      <td align="center">
        <img src="docs/gif/카테고리별 사용량.gif" width="260" alt="카테고리 도넛"/>
      </td>
    </tr>
  </table>
</div>

---

### 📄 3. 지출결의서 PDF & 엑셀 CSV 내보내기
- **제출용 지출결의서 PDF 생성**: 회사 제출 및 세무 증빙에 최적화된 고품질 PDF 리포트를 모바일에서 즉시 생성하고 미리볼 수 있습니다.
- **CSV & 영수증 원본 사진 ZIP 압축**: 엑셀 처리를 위한 CSV 파일과 고화질 영수증 실물 이미지를 압축 파일 하나로 깔끔하게 추출합니다.

<div align="center">
  <table>
    <tr>
      <th width="50%">📄 PDF 지출결의서 미리보기</th>
      <th width="50%">⚙️ PDF 리포트 포맷 커스텀 설정</th>
    </tr>
    <tr>
      <td align="center">
        <img src="docs/images/pdf미리보기 화면.jpg" width="240" alt="PDF 미리보기"/>
      </td>
      <td align="center">
        <img src="docs/images/설정 화면(pdf).jpg" width="240" alt="PDF 설정"/>
      </td>
    </tr>
  </table>
</div>

---

### ⚙️ 4. 포맷 커스텀, 알림 & 고객센터

<div align="center">
  <table>
    <tr>
      <th width="33%">📊 CSV 출력 포맷 설정</th>
      <th width="33%">🔔 스마트 알림 센터</th>
      <th width="33%">💬 고객센터 & 오픈채팅</th>
    </tr>
    <tr>
      <td align="center">
        <img src="docs/images/설정 화면(csv).jpg" width="240" alt="CSV 설정"/>
      </td>
      <td align="center">
        <img src="docs/images/알림 화면.jpg" width="240" alt="알림 화면"/>
      </td>
      <td align="center">
        <img src="docs/images/고객센터 화면.jpg" width="240" alt="고객센터"/>
      </td>
    </tr>
  </table>
</div>

---

## 🛠️ 기술 스택 (Tech Stack)

| 레이어 | 사용 기술 |
|---|---|
| **Language** | Kotlin (100%) |
| **UI Framework** | Jetpack Compose, Material 3, Haze (Glassmorphism Blur) |
| **Architecture** | MVVM + Clean Architecture, Hilt (Dependency Injection) |
| **Database & Persistence** | Room DB (`MIGRATION_3_4`), DataStore Preferences |
| **AI Engine** | Google Gemini Generative AI (Receipt OCR & Auto-Categorization) |
| **Async & Reactive** | Kotlin Coroutines, StateFlow, SharedFlow |
| **Document Generation** | Android Native `PdfDocument`, Custom CSV & ZIP Engine |
| **Icons & Design** | Lucide Icons, Modern Tailwind Vivid Palette |
| **Build & Tools** | Gradle (KTS), KSP, AndroidX |

---

## 🚀 시작하기 (Getting Started)

### 사전 요구 사항

| 항목 | 권장 사양 |
|---|---|
| Android Studio | Ladybug (2024.2.1) 이상 |
| JDK | JDK 17 이상 |
| Min / Target SDK | API 26 (Android 8.0) ~ API 35 (Android 15) |
| Google Services | `google-services.json` 발급 필요 |

### 빌드 및 실행 방법

1. **프로젝트 클론**
   ```bash
   git clone https://github.com/cdiff/Receipt.git
   cd Receipt
   ```

2. **`google-services.json` 배치**
   - [Firebase Console](https://console.firebase.google.com/)에서 다운로드한 `google-services.json` 파일을 `app/` 디렉토리에 배치합니다.
   ```text
   Receipt/
   └── app/
       └── google-services.json  ← 여기에 배치
   ```

3. **Gemini API Key 설정** (AI 스캔 기능 활성화)
   - 프로젝트 루트의 `local.properties` 파일에 발급받은 Gemini API 키를 추가합니다.
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

4. **빌드 및 실행**
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📁 프로젝트 패키지 구조

<details>
<summary><b>📂 패키지 트리 펼쳐보기 (Click)</b></summary>

```text
com.pasic.receipt
├── ai                  (Gemini Receipt OCR 엔진 & 프롬프트)
├── data
│   ├── local           (Room Database, DAO, Categories & Receipts Entity)
│   ├── notification    (알림 로컬 데이터 모델 & 리포지토리)
│   ├── preferences     (UserPreferences DataStore)
│   └── repository      (ReceiptRepository - 100% DB Flow 공급)
├── di                  (Hilt 의존성 주입 모듈 - DatabaseModule, AppModule)
├── ui
│   ├── components      (글래스모피즘 탑바, 플로팅 네비게이션, 토스트)
│   ├── export          (PDF/CSV 지출결의서 생성 & 바텀시트)
│   ├── home            (메인 대시보드, 3dp 평행 도넛 차트, 소비 바 차트)
│   ├── notification    (알림 목록 & 공지사항 상세 뷰)
│   ├── receipts        (영수증 목록, 날짜 범위 피커, 카테고리 필터)
│   ├── scan            (카메라 스캔, 크롭, OCR 검증 & 카테고리 선택)
│   ├── settings        (PDF/CSV 포맷 설정, 백업 및 복원, 테마 설정)
│   ├── support         (고객센터, 오픈카톡 연동, FAQ 바텀시트)
│   └── theme           (Tailwind 500 모던 비비드 컬러, CategoryThemeRegistry)
└── util                (PDF 리포트 생성기, ZIP 백업 엔진, 이미지 스토리지)
```

</details>

---

## 📝 라이선스 (License)
Copyright © 2026 PASIC Receipt Team. All rights reserved.
