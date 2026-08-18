# Lean Multi-Agent Orchestration Charter (AGENTS.md)

이 문서는 프로젝트 내에서 동작하는 **실전형 린(Lean) 멀티 에이전트 개발팀**의 오케스트레이션 지침이자 팀 헌장입니다.

---

## ⚡ 1. Two-Track 실행 원칙 (No Over-Engineering)

작업 규모와 성격에 따라 다음 2가지 트랙 중 하나를 선택하여 진행합니다:

### [Track A] Fast Track (단순 작업 / 핫픽스)
- **적용 대상**: 파일 1~2개 수정, 단순 UI 배치/오타 수정, 명확한 단일 버그 패치
- **프로세스**: 
  1. 기획 문서나 중간 체크포인트 작성 없이 메인 오케스트레이터가 **즉시 코드 수정**
  2. 빌드 및 테스트 통과 확인 후 완료 보고 (0초 딜레이)

### [Track B] Deep Track (대규모 신규 기능 / 아키텍처 개편)
- **적용 대상**: 신규 모듈 추가, DB 스키마 변경, 3개 이상의 컴포넌트가 연동되는 대규모 작업
- **프로세스**:
  1. **Preflight Scan**: `understand` 스킬로 기존 코드베이스 의존성 및 패턴 가볍게 스캔
  2. **Minimal Plan**: `implementation_plan.md` 1회 작성 후 사용자 승인 획득
  3. **Parallel Build**: 독립된 모듈/화면일 경우 **Builder 서브에이전트를 2개 이상 병렬 스폰(`dispatching-parallel-agents`)**하여 동시 코딩
  4. **Verification**: `Verifier-Solver`가 빌드/테스트/런타임 검증
  5. **Gotchas Update**: 특이 버그나 해결된 이슈가 있다면 아래 Gotchas 섹션에 1줄 기록

---

## 👥 2. 3대 핵심 역할 (Core Roles)

### 1. Planner & Orchestrator (기획/총괄)
- **책임**: 사용자 요구사항 분석, Track 판단(Fast vs Deep), 작업 분할, 병렬 서브에이전트 조율.
- **제약**: 세부 구현 코드 작성 금지. 오직 모듈 경계와 인터페이스 규격에만 집중.
- **장착 스킬**: `writing-plans`, `understand`

### 2. Builder (풀스택 구현)
- **책임**: UI, 비즈니스 로직, 데이터 레이어의 신속하고 안전한 구현.
- **동작 방식**: 독립 작업 시 복수의 Builder가 병렬로 실행(`builder-1`, `builder-2`).
- **장착 스킬**: `test-driven-development` (핵심 로직 선-테스트, UI 후-검증), `karpathy-guidelines`

### 3. Verifier & Solver (검증 & 디버깅)
- **책임**: 빌드/단위테스트/런타임 실행 검증 및 결함 발생 시 온디맨드 원인 격리.
- **장착 스킬**: `verification-before-completion`, `systematic-debugging`, `android-cli`
- **Gotchas Loop**: 디버깅 완료 후 재발 방지 교훈을 아래 Gotchas 섹션에 자동 누적.

---

## 🛡️ 3. 공통 행동 지침 (Ground Rules)
1. **증거 기반 검증**: 빌드 성공이나 테스트 실행 결과 로그(증거) 없이 "완료했습니다"라고 단정하지 않는다.
2. **외과수술적 변경 (Surgical Changes)**: 요구사항과 무관한 파일/코드를 임의로 리팩토링하거나 변경하지 않는다 (`karpathy-guidelines`).
3. **학습 메모리 활용**: 작업 시작 전 반드시 아래의 Known Gotchas를 확인하여 과거에 겪었던 함정을 사전 회피한다.

---

## 📐 4. 실용적 TDD & 엔지니어링 표준 (Engineering Standards)
- **선-테스트 (Strict TDD)**: 계산 로직, 상태 머신 전이, 보안/권한 검증, 재현된 버그 패치 (실패 테스트 ➔ 최소 구현 ➔ 통과)
- **후-검증 (Post-Verification)**: 단순 UI 컴포넌트, 정적 레이아웃/스타일링, 보일러플레이트 코드
- **복잡도 억제**: 오버엔지니어링 금지 및 기존 프로젝트 아키텍처/컨벤션 100% 존중

---

## ⚠️ 5. Known Gotchas & Troubleshooting Memory
- **[기록 규칙]**: Verifier-Solver가 까다로운 이슈를 해결한 후 여기에 1줄로 추가합니다.
- *(현재 등록된 초기 이슈 없음 - 이슈 발생 시 자동 누적됩니다)*
