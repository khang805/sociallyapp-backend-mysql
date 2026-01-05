# Socially: A Modern Android Social Media Platform

**Socially** is a comprehensive social networking application built natively for Android using Kotlin and modern development principles. It provides a robust platform for users to connect and interact through real-time chat, content sharing, and a dynamic social feed. This project serves as a demonstration of a scalable, maintainable, and feature-rich mobile application architecture.

---

## Core Features

Socially delivers a complete social media experience with a strong focus on performance, security, and usability:

* 🔐 **Secure Authentication:** Seamless and secure user sign-up and sign-in. Session management is handled via a custom `SessionManager` with token-based authentication.
* 👤 **Dynamic User Profiles:** Fully-featured user profiles displaying profile pictures, bios, and real-time follower/following statistics.
* 🤝 **Follow System:** Users can follow and unfollow others to curate a personalized content feed.
* 📝 **Rich Content Feed:** Users can create posts with images and captions. The home feed intelligently aggregates posts from followed users.
* ❤️ **Social Engagement:** Interactive features allowing users to like and comment on posts.
* 💬 **Real-Time Messaging:** One-on-one private messaging powered by a dedicated backend service.
* 📞 **Video Calling:** Integrated high-quality, real-time video calls using the Agora RTC SDK.
* 🔍 **User Search:** An efficient search feature to discover and connect with other users.
* ✈️ **Offline-First Capabilities:** Content is cached locally using a Room (SQLite) database, ensuring the app remains functional without an internet connection.
* 🔄 **Background Operations:** Offline actions (such as sending messages or liking posts) are queued via WorkManager and synced reliably once network connectivity is restored.

---

## Architecture & Tech Stack

This project adheres to the **MVVM (Model-View-ViewModel)** architecture, ensuring a clean separation of concerns between the UI, business logic, and data layers.

### Technical Specifications
* **Language:** Kotlin (leveraging Coroutines for asynchronous operations).
* **Architecture:** MVVM with Repository Pattern.
* **User Interface:** XML Layouts utilizing ViewBinding for type-safe view interaction.
* **Dependency Management:** Gradle with Version Catalogs (`libs.versions.toml`).

### Key Libraries & Components

**Android Jetpack:**
* *ViewModel & LiveData:* For lifecycle-aware data management.
* *Room:* For local SQLite data persistence and offline caching.
* *WorkManager:* For reliable background task scheduling.

**Backend & Networking:**
* *Architecture:* RESTful APIs backed by a MySQL database.
* *Retrofit & OkHttp:* Robust HTTP client for API communication, featuring custom interceptors for authentication and logging.

**Real-time Communication:**
* *Agora RTC SDK:* Integrated for high-definition video calling.

**UI & Media:**
* *Material Design Components:* For modern, accessible UI elements.
* *Picasso & CircleImageView:* For efficient image loading, caching, and rendering.

---

## Getting Started

Follow these instructions to set up the project locally for development and testing.

### Prerequisites
* **Android Studio:** Version Iguana (2023.2.1) or newer.
* **Android Device/Emulator:** Running API Level 24 (Nougat) or higher.
* **Backend Server:** A running instance of the backend server.
  * **Backend Repository:** [https://github.com/khang805/sociallyapp-backend-mysql]

### Installation & Setup

1. Clone the Repository
```bash
git clone [(https://github.com/khang805/sociallyapp-backend-mysql.git)]
cd socially-android

2. Setup the Backend Server

This app requires the backend server to be running.
* Clone the backend repository:
    ```bash
    git clone [https://github.com/khang805/sociallyapp-backend-mysql.git](https://github.com/khang805/sociallyapp-backend-mysql.git)
    ```
* Follow the instructions in the backend `README` to import the MySQL database and start the server.

3. Configure Backend Connectivity

* Navigate to: `app/src/main/java/com/example/socially/api/RetrofitClient.kt`
* Locate the `BASE_URL` constant.
* Update the IP address to match your backend server:
    * **If using Android Emulator:** Use `http://10.0.2.2:3000/` (or your server port).
    * **If using a Physical Device:** Use your computer's local IP (e.g., `http://192.168.1.5:3000/`).

4. Sync and Run

* Open the project in **Android Studio**.
* Let Gradle sync complete (it may take a few minutes).
* Press **Run** to launch the app on your device or emulator.
