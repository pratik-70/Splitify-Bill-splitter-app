# Splitify 💸

Splitify is a modern, real-time bill-splitting application built with **Jetpack Compose** and **Firebase**. It allows users to manage shared expenses, track group balances, and settle debts seamlessly with a smooth, intuitive UI.

## ✨ Features

- **Real-time Sync**: Instant updates across all members using Cloud Firestore.
- **Smart Search**: Quickly find groups, friends, or specific expenses with built-in search functionality across all main screens.
- **Horizontal Swipe Navigation**: Seamlessly switch between **Groups**, **Friends**, **Activity**, and **Account** tabs using a synchronized `HorizontalPager` and `BottomNavigationBar`.
- **Intelligent Settlements**: Automatically calculates the most efficient way to "Settle Up" within a group.
- **Persistent Activity Feed**: Full history of expenses and settlements. Unlike many apps, Splitify preserves your expense history even after settling up, marking them as archived while creating new settlement records.
- **Group Management**: 
    - Create and manage groups for trips, households, or events.
    - **Safety First**: Groups can only be deleted if all member balances are zero, preventing accidental data loss of unsettled debts.
- **Auto-Profile Creation**: Automatic Firestore profile synchronization upon first login via Firebase Auth.
- **Security**: Granular Firestore Security Rules to protect user data and ensure only group members can modify balances.

## 🛠️ Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Language**: Kotlin
- **Asynchronous**: Coroutines & Flow
- **Backend**: 
    - Firebase Authentication (Email/Password)
    - Cloud Firestore (Real-time DB)
- **Navigation**: Jetpack Navigation Compose
- **Architecture**: MVVM (Model-View-ViewModel)

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- A Firebase Project

### Setup
1. **Clone the repo**:
   ```bash
   git clone https://github.com/pratik-70/Splitify-Bill-splitter-app.git
   ```
2. **Firebase Integration**:
   - Create an Android app in your Firebase Console (`com.example.splitify`).
   - Place your `google-services.json` in the `app/` folder.
   - Enable **Email/Password** Auth and **Firestore** in the console.
3. **Security Rules**:
   Deploy the following rules to your Firestore instance:
   ```javascript
   service cloud.firestore {
     match /databases/{database}/documents {
       match /groups/{groupId} {
         allow read, create: if request.auth != null;
         allow update, delete: if request.auth != null && request.auth.uid in resource.data.members;
       }
       match /expenses/{expenseId} {
         allow read, create: if request.auth != null;
         allow update, delete: if request.auth != null;
       }
       match /users/{userId} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```
4. **Build and Run**: Sync Gradle and run the `:app` module.

## 📱 Screenshots
*(Placeholders for your app screenshots)*

---
Developed with ❤️ by [Pratik](https://github.com/pratik-70)
