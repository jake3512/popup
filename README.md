# 팝업스쿨 (PopupSchool)

다른 앱 위에 항상 떠있는 **플로팅 버블** 형태의 안드로이드 앱입니다. 버블을 탭하면 카드가 펼쳐지며 다음 정보를 보여줍니다.

- **디데이** — 앱에서 직접 등록/관리하는 디데이 목록 (가까운 순 최대 3개 표시)
- **오늘의 시간표** — NEIS(나이스) Open API로 조회
- **오늘의 급식(식단표)** — NEIS(나이스) Open API로 조회

## 동작 방식

- `overlay/BubbleService`가 포그라운드 서비스로 실행되며 `WindowManager`에 작은 원형 버블 뷰를 띄웁니다.
- 버블은 드래그로 이동할 수 있고, 짧게 탭하면 확장 카드(`view_popup.xml`)로 전환됩니다.
- 학교 정보(교육청 코드, 학교 코드, 학년/반)와 NEIS API 키는 하드코딩되어 있지 않고, 앱 내 **설정** 화면에서 학교 이름 검색으로 채워 넣도록 되어 있습니다. 사람마다 다니는 학교가 다르기 때문입니다.

## 시작하기

### 1. NEIS Open API 키 발급

1. https://open.neis.go.kr 접속 후 회원가입
2. "인증키 신청" 메뉴에서 키 발급 (즉시 발급됨)

### 2. 프로젝트 열기

1. Android Studio(최신 버전 권장)에서 이 저장소를 엽니다.
2. Gradle sync가 끝날 때까지 기다립니다. (`compileSdk 34`, `minSdk 26`)
3. 실제 기기 또는 에뮬레이터에서 앱을 실행합니다.

> 이 리포지토리를 생성한 개발 환경(샌드박스)에는 Android SDK와 구글 메이븐 저장소(`dl.google.com`)로의 네트워크 접근이 없어 이 안에서는 `./gradlew build`를 끝까지 실행할 수 없었습니다. 소스는 전체 검토를 마쳤지만, **Android Studio에서 최초 빌드 및 실행 확인이 필요**합니다.

### 3. 앱 사용 순서

1. 앱 실행 → **"다른 앱 위에 표시 권한 허용하기"** 버튼으로 오버레이 권한 부여
2. **"학교 정보 설정"** 진입 → NEIS API 키 입력 → 학교 이름 검색 → 목록에서 학교 선택 → 학년/반 입력 → 저장
3. **"디데이 관리"**에서 디데이 항목 추가 (제목 + 날짜)
4. 홈 화면에서 **"버블 시작"** 클릭 → 화면 위에 버블이 뜸 → 탭하면 디데이/시간표/급식 카드 펼쳐짐
5. 버블을 다시 탭하거나 카드의 닫기(X) 버튼으로 접기, 알림의 "중지" 액션 또는 홈 화면의 "버블 중지"로 서비스 종료

## 프로젝트 구조

```
app/src/main/java/com/jake/popupschool/
├── MainActivity.kt              # Compose 진입점, 3개 화면 NavHost
├── ui/home, ui/settings, ui/dday  # Compose 화면들
├── overlay/BubbleService.kt      # 플로팅 버블 + 확장 팝업 (WindowManager)
├── overlay/OverlayPermissionHelper.kt, NotificationHelper.kt
├── data/remote/                  # NEIS Open API (Retrofit + Gson)
├── data/repository/              # NEIS 응답 → 도메인 모델 매핑
├── data/settings/                # DataStore 기반 학교/API 키 설정 저장
├── data/dday/                    # DataStore 기반 디데이 목록 저장 (kotlinx.serialization)
├── domain/model/                 # MealInfo, TimetableSlot, SchoolLevel, SchoolSearchResult
└── util/DateUtils.kt             # yyyyMMdd 포맷, 디데이 계산, 학년도/학기 계산
```

## 알려진 제한 사항 (추후 개선 여지)

- 기기 재부팅 후 버블이 자동으로 다시 뜨지 않습니다 (BOOT_COMPLETED 리시버 미구현).
- 홈 화면의 "버블 실행 중" 상태는 로컬 상태값이라, 알림의 "중지" 버튼으로 서비스를 끄면 홈 화면 버튼 표시가 실제 상태와 어긋날 수 있습니다.
- NEIS 시간표 API는 학교/교육청에 따라 응답 지연이 있을 수 있습니다.
