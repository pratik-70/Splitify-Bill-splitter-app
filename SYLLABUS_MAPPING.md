# Splitify - Syllabus Implementation Mapping 📚

This document maps the concepts from the Android Development course syllabus to their practical implementation within the **Splitify** application.

---

## Unit I: Views & Components in Jetpack Compose
*   **Lists and Grids**: 
    *   Implemented using `LazyColumn` and `items` in `GroupsScreen`, `FriendsScreen`, `ActivityScreen`, and `GroupDetailScreen` to efficiently render dynamic data from Firestore.
*   **Splash Screen & LaunchedEffect**:
    *   `SplashScreen.kt` utilizes `LaunchedEffect` to handle the initial delay and routing logic (Auth check) when the app starts.
*   **Progressbar**:
    *   `CircularProgressIndicator` is integrated across all main screens, controlled by an `isLoading` state in `MainViewModel`, providing visual feedback during network operations.

## Unit IV: Custom UI Components in Jetpack Compose
*   **Material & Foundation Composables**:
    *   Extensive use of Material 3 components: `Scaffold`, `Card`, `Surface`, `TopAppBar`, `IconButton`, `FloatingActionButton`, and `TextField`.
*   **Fully Declarative UI Approach**:
    *   The entire app follows a unidirectional data flow where the UI reactively updates based on `StateFlow` objects emitted by the `MainViewModel`.
*   **Creating Custom Components**:
    *   `CommonComponents.kt` contains parameterized composables like `SplitifyTopAppBar` and `SplitifySummaryCard` to ensure UI consistency and code reusability.
*   **Modifiers, Shape, and Color**:
    *   Customized `Modifier` chains for layout control, `RoundedCornerShape` for modern aesthetics, and a cohesive color palette defined in `Theme.kt`.

## Unit V: Modern Data Storage & Permission Management
*   **Cloud Firestore (Advanced Storage)**:
    *   While the syllabus focuses on `DataStore`, Splitify implements **Cloud Firestore** for real-time, multi-user data synchronization.
*   **Firestore Security Rules**:
    *   Implemented rules to handle data access permissions, ensuring users can only read/write data relevant to their own accounts and groups.

## Unit VI: Advanced Navigation & Paging UI Components
*   **TabRow + HorizontalPager**:
    *   The main application shell uses a `HorizontalPager` synchronized with the `BottomNavigationBar`. This allows users to navigate between "Groups", "Friends", "Activity", and "Account" using both taps and **horizontal swipe gestures**.
*   **Navigation Compose**:
    *   Implemented a type-safe navigation graph in `NavGraph.kt` using `NavHostController` to manage screen transitions and argument passing (e.g., passing `groupId` to `GroupDetailScreen`).

---

## Summary of Applied Syllabus Topics

| Syllabus Topic | Splitify Functionality |
| :--- | :--- |
| **Lists (LazyColumn)** | Groups, Friends, and Expense lists. |
| **LaunchedEffect** | Splash screen navigation logic. |
| **ProgressBar** | Global loading states during Firestore sync. |
| **Material 3** | TopAppBars, Cards, and FABs. |
| **Custom Components** | Reusable `SplitifyTopAppBar` and `SummaryCard`. |
| **HorizontalPager** | Swipeable tab navigation between main screens. |
| **Declarative UI** | State-driven UI updates via ViewModels. |
| **Intents (Nav)** | Navigation between screens using NavController. |
