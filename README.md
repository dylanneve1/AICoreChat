# AICore Chat

AICore Chat is a simple, modern Android chat application built to demonstrate the capabilities of Google's on-device **AICore SDK**. It provides a real-time, streaming chat experience with a generative AI model running directly on your Android device.

## ✨ Features

-   **On-Device AI:** All generative AI processing happens locally on the device, ensuring privacy and offline capability.
-   **Streaming Responses:** The model's responses are streamed word-by-word for a fluid, real-time user experience.
-   **Modern UI:** Built entirely with Jetpack Compose and Material 3, following modern Android development practices.
-   **Conversation History:** The chat history is maintained in memory and used as context for subsequent prompts.
-   **Core Chat Functionality:**
    -   Stop ongoing message generation.
    -   Copy messages to the clipboard with a long-press.
    -   Clear the entire conversation.
    -   Auto-scroll to the latest message.
    -   "Scroll to Bottom" button for navigating long chats.
-   **Robust State Management:** Utilizes a `ViewModel` and Kotlin `StateFlow` to manage UI state effectively.

## 🛠️ Tech Stack & Key Libraries

-   **Language:** [Kotlin](https://kotlinlang.org/)
-   **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
-   **AI Library:** [Google AICore SDK](https://developer.android.com/ml/aicore) (`com.google.ai.edge.aicore:aicore`)
-   **Architecture:** MVVM (Model-View-ViewModel)
-   **Asynchronous Programming:** Kotlin Coroutines and Flow
-   **Material Design:** [Material 3](https://m3.material.io/)

## ⚙️ How It Works

The application's logic is primarily handled by the `ChatViewModel`.

1.  **Model Initialization:** On startup, the `ViewModel` initializes the `GenerativeModel` from the AICore SDK. It prepares the inference engine for use, a process that happens once.
2.  **Prompt Construction:** When a user sends a message, the `ViewModel` constructs a detailed prompt. This prompt includes a preamble, a few-shot example, and the entire conversation history, formatted with `[USER]` and `[ASSISTANT]` tags to provide context to the model.
3.  **Streaming Generation:** The app calls `generativeModel.generateContentStream(prompt)`. This returns a Kotlin `Flow` that emits response chunks as the model generates them.
4.  **UI Updates:** The `ViewModel` collects this flow and updates its `ChatUiState`, which is exposed to the Compose UI via a `StateFlow`. The UI observes this state and recomposes automatically, displaying the streamed text as it arrives.
5.  **Error and State Handling:** The `ViewModel` manages loading, generating, and error states, ensuring the UI always reflects the current status of the model.

## 🚀 Getting Started

### Prerequisites

-   Android Studio
-   An Android device or emulator running API level 31 or higher.

### Build and Run

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    ```
2.  **Open in Android Studio:**
    -   Open Android Studio.
    -   Click on `File -> Open` and select the cloned project directory.
3.  **Sync Gradle:**
    -   Let Android Studio sync the project and download all the required dependencies.
4.  **Run the app:**
    -   Select a target device (emulator or physical device).
    -   Click the "Run" button (▶️).

> **Note:** The Google AICore SDK is experimental (`0.0.1-exp01`). It may require specific device capabilities or participation in an early access program for the on-device model to be available.

## 📂 Code Structure

The project follows a standard Android application structure. All the core application logic and UI are located in one place for easy reference:

-   `app/src/main/java/org/dylanneve1/aicorechat/MainActivity.kt`: Contains the `ChatViewModel`, all Jetpack Composables (`ChatScreen`, `MessageRow`, etc.), and the `MainActivity`.
-   `app/src/main/java/org/dylanneve1/aicorechat/ui/theme/`: Standard files for Compose theming (Color, Theme, Type).
-   `app/build.gradle.kts`: The app-level build script where the AICore SDK dependency is declared.

## 📄 License

This project is licensed under the Apache 2.0 License. See the [LICENSE](LICENSE) file for details.
