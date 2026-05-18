# Flora 🌿

**Flora** is a modern Android application designed to help users track their digestive health and nutrition. By correlating meals with bowel movements using the **Bristol Stool Scale**, Flora provides actionable insights into how different foods affect your well-being.

## ✨ Features

-   **📅 Health Diary**: Log bowel movements and meals in a clean, intuitive timeline.
-   **💩 Bristol Scale Integration**: Easy-to-use interface to categorize movements based on the medical Bristol Stool Chart.
-   **🍽️ Nutrition Tracking**: Register meals with descriptions and types (Breakfast, Lunch, Dinner, Snacks).
-   **📊 Smart Analytics**:
    -   **Health Distribution**: Visual breakdown of stool consistency percentages.
    -   **Food Correlation**: Automatically detects which foods lead to better or worse digestion based on a 24-hour analysis window.
    -   **Historical Patterns**: Weekly activity charts to monitor frequency.
-   **📅 Interactive Calendar**: Navigate through your history with a custom-built expandable calendar (Week/Month views).
-   **📤 Data Export**: Generate and share text-based health reports for medical consultations.

## 🛠 Tech Stack

-   **Language**: [Kotlin](https://kotlinlang.org/) (100%)
-   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3.
-   **Architecture**: MVVM (Model-View-ViewModel) following **Clean Architecture** principles.
-   **Local Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room).
-   **Reactive Programming**: Kotlin Coroutines and StateFlow.
-   **Date & Time**: Java 8+ Time API (`java.time`).

## 🏗 Project Structure

The project follows clean architecture principles to ensure high scalability and maintainability:

```
com.example.flora/
├── data/           # Room entities, DAOs, and Repositories (Data Source)
├── model/          # Domain models for stats and analysis
├── ui/
│   ├── components/ # Reusable UI atoms (Cards, Calendar, Items)
│   ├── screens/    # Top-level UI screens (Home, Stats)
│   ├── sheets/     # Bottom sheet contents and forms
│   └── theme/      # Material 3 Theme, Typography, and Semantic Colors
├── util/           # Helper functions for Dates and Exporting
└── FloraViewModel  # Centralized business logic and state management
```

## 🚀 Getting Started

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/your-username/flora.git
    ```
    
2.  **Open in Android Studio**:
    Recommended version: **Android Studio Ladybug** or newer.
3.  **Sync Gradle**:
    Let the IDE download the necessary dependencies (Compose BOM, Room, etc.).
4.  **Run**:
    Deploy to a physical device or emulator with API 26 (Android 8.0) or higher.

## 📈 Analysis Logic
Flora uses a weight-based algorithm to calculate **Food Correlation**:
-   It analyzes bowel movements occurring within **24 hours after** a registered meal.
-   **Bristol Types 3 & 4** contribute positively to the "Digestive Score".
-   **Types 1, 7, and 6** contribute negatively.
-   The app highlights your top "Excellent" and "Heavy" foods based on these averages.

---
*Developed with ❤️ focusing on clean code and user privacy.*
