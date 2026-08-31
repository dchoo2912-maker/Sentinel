# Implementation Plan - Sentinel App

This plan outlines the step-by-step development of the Sentinel app based on the provided 13-screen wireframe walkthrough.

## User Review Required

> [!IMPORTANT]
> The app relies heavily on SVG maps and specific animations (radar pulse, progress rings). I will use standard Compose `Canvas` and `Animation` APIs to approximate these.

> [!NOTE]
> Navigation will be handled via Jetpack Navigation Compose. Screens 4-13 will share a persistent Bottom Navigation Bar.

## Proposed Changes

### 1. Theme and Foundation
- **[MODIFY] [Color.kt](file:///Users/davichoo/AndroidStudioProjects/Sentinel/app/src/main/java/com/example/sentinel/ui/theme/Color.kt)**: Define the custom palette (Blue, Slate, Red, Green shades).
- **[MODIFY] [Theme.kt](file:///Users/davichoo/AndroidStudioProjects/Sentinel/app/src/main/java/com/example/sentinel/ui/theme/Theme.kt)**: Set up `LightColorScheme` and `DarkColorScheme` using the new palette.

### 2. Navigation Structure
- **[NEW] [Screen.kt](file:///Users/davichoo/AndroidStudioProjects/Sentinel/app/src/main/java/com/example/sentinel/navigation/Screen.kt)**: Define the navigation routes.
- **[NEW] [SentinelNavGraph.kt](file:///Users/davichoo/AndroidStudioProjects/Sentinel/app/src/main/java/com/example/sentinel/navigation/SentinelNavGraph.kt)**: Main navigation host.
- **[MODIFY] [MainActivity.kt](file:///Users/davichoo/AndroidStudioProjects/Sentinel/app/src/main/java/com/example/sentinel/MainActivity.kt)**: Entry point for the navigation graph.

### 3. Shared Components
- **[NEW] [BottomNavBar.kt](file:///Users/davichoo/AndroidStudioProjects/Sentinel/app/src/main/java/com/example/sentinel/ui/components/BottomNavBar.kt)**: The persistent navigation bar for main screens.
- **[NEW] [SentinelButton.kt](file:///Users/davichoo/AndroidStudioProjects/Sentinel/app/src/main/java/com/example/sentinel/ui/components/SentinelButton.kt)**: Custom styled buttons (Primary, Outline, SOS).

### 4. Screen Implementations (Sequential)
- **Phase 1: Entry Flow**
    - [NEW] `SplashScreen` (with radar pulse animation and auto-advance).
    - [NEW] `AuthScreen` (Tabs for Sign In/Sign Up, minimal design).
    - [NEW] `DevicePairingScreen` (Sonar animation, nearby devices list).
- **Phase 2: Main Application**
    - [NEW] `HomeDashboard` (Status cards, quick actions grid, safe route).
    - [NEW] `EmergencyScreen` (SOS hold-to-activate, pulsing countdown).
    - [NEW] `LiveTrackingScreen` (Map simulation, sharing stats).
- **Phase 3: Navigation and Risks**
    - [NEW] `SafeRouteNavigationScreen` (Polyline on map, risk zones).
    - [NEW] `CommunityRiskMapScreen` (Heatmap clusters, filter chips).
    - [NEW] `ReportIncidentScreen` (Category grid, photo upload placeholder).
- **Phase 4: Management and History**
    - [NEW] `ActivityHistoryScreen` (Grouped history cards).
    - [NEW] `EmergencyContactsScreen` (Contact cards with status indicators).
    - [NEW] `DeviceStatusScreen` (Battery bar, sensor list).
    - [NEW] `ProfileSettingsScreen` (Settings sections, quick nav grid).

## Verification Plan

### Automated Tests
- Build verification: `gradlew assembleDebug`
- Navigation testing: Verify screen transitions.

### Manual Verification
- Deploy to emulator/device to verify animations (Splash pulse, SOS ring).
- Check the layout scaling on different screen sizes.
- Verify the 2.8s delay in Splash screen.
