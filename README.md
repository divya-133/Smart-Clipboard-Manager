# SmartClipboardManager

**SmartClipboardManager** is an advanced clipboard management desktop application built with Java Swing. It intelligently captures and organizes both text and image clipboard data, offering features such as AES encryption, tagging, search, auto-cleanup, and real-time synchronization. Designed with productivity and privacy in mind, it offers a clean, modern UI using FlatLaf and supports dark/light modes.

---

## ✨ Key Features

* **📋 Clipboard Monitoring** – Automatically tracks text and image clipboard activity.
* **🔐 AES Encryption** – Secures all clipboard entries using AES-256 encryption.
* **🏷️ Smart Tagging** – Drag-and-drop tagging for Text, URL, Email, Image, and more.
* **🧠 Categorization & Search** – Search and filter clips by tag, keyword, or content type.
* **📌 Pin Important Clips** – Keep essential clips always accessible.
* **📝 Notes Support** – Add quick notes or descriptions to any clip.
* **🌗 Theme Toggle** – Switch between Dark and Light themes using FlatLaf.
* **🧹 Auto-Cleanup** – Automatically removes unused clips older than 7 days.
* **📤 Import/Export** – Backup or share clipboard history as JSON files.
* **🚫 Sensitive Data Filter** – Detects and flags passwords, emails, and other sensitive data.
* **🔄 Real-time Sync** – Sync clipboards across devices using WebSocket (experimental).
* **📈 Usage Tracking** – Records how often each clip is used for smarter organization.

---

## 🖼️ Screenshots 

<img width="1920" height="1080" alt="Screenshot 2025-05-04 075641" src="https://github.com/user-attachments/assets/a276ebc5-2706-4a08-8a41-9a06bd00f0c1" />

## Light Theme
<img width="1920" height="1080" alt="Screenshot 2025-05-04 075659" src="https://github.com/user-attachments/assets/6e14b291-6061-4dff-8395-0630eef2be71" />

## Dark Theme 
<img width="1920" height="1080" alt="Screenshot 2025-05-04 075715" src="https://github.com/user-attachments/assets/45acc941-5c62-4676-85d7-91f7a56069b7" />

---


## 🛠 Tech Stack

* **Language**: Java 17+
* **GUI**: Java Swing + FlatLaf (Modern Look and Feel)
* **Data Storage**: JSON (Encrypted with AES)
* **Libraries Used**:

  * `FlatLaf` for UI theming
  * `Gson` for JSON handling
  * `Javax.crypto` for encryption

---

## 🚀 Getting Started

### Prerequisites

* Java Development Kit (JDK) 17 or higher
* A Java-compatible IDE (e.g., IntelliJ IDEA, Eclipse)
* FlatLaf library (included in `libs/`)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/divya-133/SmartClipboardManager.git
   cd SmartClipboardManager  
   ```
