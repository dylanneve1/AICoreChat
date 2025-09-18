# AICore Chat

AICore Chat is a modern, Jetpack Compose Android application that showcases Google's on-device **AICore SDK** with a fully offline-capable chat experience. The app focuses on demonstrating how Gemini Nano can be embedded inside a polished messaging workflow that feels comparable to cloud-backed assistants while keeping all inference on device.

## ✨ Features

- **On-Device Gemini Nano** – All text generation happens locally through the experimental `com.google.ai.edge.aicore:aicore` SDK for private and low-latency responses.
- **Streaming Conversation Flow** – Messages stream token-by-token with controls to stop generation, retry, copy, clear, and jump back to the latest turn in long threads.
- **Multi-Session Workspace** – A navigation drawer lets you create, rename, and delete conversations, with automatic cleanup of empty chats and in-memory persistence of history.
- **Personalized Onboarding & Settings** – Collect a preferred name, toggle personal context, bio context, web search, multimodal support, memory usage, and custom instructions before entering the chat.
- **Local Memory Management** – Curate what the assistant should remember about you by adding, editing, toggling, or deleting memory entries and optional biographical details stored on device.
- **Multimodal Attachments** – Attach camera shots or gallery images. ML Kit's on-device Image Description API generates captions that are injected into the prompt when multimodal mode is enabled.
- **Contextual Awareness Tools** – Opt-in personal context includes device model, battery, locale, storage, time, and coarse location. A built-in web search tool (DuckDuckGo HTML results) fetches fresh snippets when online.
- **Device Support Guardrails** – The app verifies AICore availability and gracefully surfaces onboarding, unsupported, and loading states before the main chat renders.

## 🛠️ Tech Stack & Key Libraries

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 components
- **AI Runtime:** [Google AICore SDK](https://developer.android.com/ml/aicore)
- **Async & State:** Kotlin Coroutines, Flow, and `AndroidViewModel`
- **Location & Services:** Google Play Services Location, Android connectivity & battery APIs
- **Multimodal Support:** ML Kit Generative AI Image Description and Coil for image loading
- **Architecture:** MVVM-inspired with dedicated repositories for chat sessions, memory, and tool integrations

## ⚙️ How It Works

1. **ViewModel Orchestration** – `ChatViewModel` bootstraps settings, restores sessions via `ChatRepository`, loads memory and bio data from `MemoryRepository`, and prepares the AICore `GenerativeModel`.
2. **Prompt Assembly** – Each turn composes a system preamble, few-shot examples, optional personal context, custom instructions, relevant memories, pending image descriptions, and the full `[USER]` / `[ASSISTANT]` formatted transcript.
3. **Tooling Pipeline** – Web searches are requested with `[SEARCH]` tags when enabled and online, image attachments run through `ImageDescriptionService`, and location/device metadata is injected through `PersonalContextBuilder`.
4. **Streaming & Persistence** – Responses stream into a `StateFlow`, updating Compose UI in real time while persisting chat content back to disk so session switching is instantaneous.

## 🚀 Getting Started

### Prerequisites

- Android Studio
- An Android device or emulator running API level 31 or higher with the AICore Gemini Nano preview installed

### Build and Run

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   ```
2. **Open in Android Studio:**
   - Open Android Studio.
   - Click on `File -> Open` and select the cloned project directory.
3. **Sync Gradle:**
   - Let Android Studio sync the project and download all the required dependencies.
4. **Run the app:**
   - Select a target device (emulator or physical device).
   - Click the "Run" button (▶️).

### Running Quality Checks

The project ships with a consolidated quality script that runs formatting, static analysis, Android Lint, and the JVM test suite:

```bash
./scripts/quality.sh
```

Under the hood this executes `spotlessCheck`, `detekt`, `lint`, and `test` using JDK 17. Run it locally (or in CI) before opening pull requests to keep the codebase consistent.

For a one-stop build helper you can use `run_all.sh` from the repository root:

```bash
# Run full clean build + QA gates + assembleDebug
./run_all.sh

# Only run unit tests
./run_all.sh tests

# Build the app
./run_all.sh assemble

# Execute connected Android tests (requires device/emulator)
./run_all.sh connected
```

If you intentionally fix or introduce code that changes the current lint/detekt findings, regenerate the baselines first and re-run the script:

```bash
./gradlew lintDebug       # updates app/lint-baseline.xml
./gradlew detektBaseline  # updates config/detekt/baseline.xml
./scripts/quality.sh
```

### Permissions

The sample declares the following runtime capabilities:

- `INTERNET` and `ACCESS_NETWORK_STATE` for web search and connectivity checks
- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` to include coarse device context when enabled
- Scoped storage access via a `FileProvider` for photo capture attachments

## 🗂️ Project Structure

- `app/src/main/java/org/dylanneve1/aicorechat/MainActivity.kt` – Hosts the Compose hierarchy, onboarding flow, and device support gating.
- `app/src/main/java/org/dylanneve1/aicorechat/data/` – `ChatViewModel`, session & memory repositories, prompt utilities, and integrations for search, personal context, and image description.
- `app/src/main/java/org/dylanneve1/aicorechat/ui/` – Compose screens for chat, onboarding, settings, memory management, and shared UI components/themes.
- `app/src/main/java/org/dylanneve1/aicorechat/util/` – Utility helpers for device checks, formatting, and token cleanup.
- `scripts/` – Automation helpers such as `quality.sh` for enforcing formatting and analysis gates.

## 📄 License

This project is licensed under the Apache 2.0 License. See the [LICENSE](LICENSE) file for details.
