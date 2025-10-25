
<div align="center">
  <img src="https://raw.githubusercontent.com/Rudragupta8777/EchoMi_App/main/app/src/main/res/drawable/app_logo.png" alt="EchoMi Logo" width="40%" />

  # EchoMi: Your Personal AI Call Assistant

  <p align="center">
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
    <img src="https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white" />
    <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" />
    <img src="https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white" />
    <img src="https://img.shields.io/badge/Twilio-F22F46?style=for-the-badge&logo=twilio&logoColor=white" />
    <img src="https://img.shields.io/badge/Deepgram-13EF93?style=for-the-badge&logo=deepgram&logoColor=black" />
    <img src="https://img.shields.io/badge/Google_Maps-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white" />
    <img src="https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white" />
    <img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white" />
  </p>

  <p align="center"><b>Never miss an important call again. Let your AI assistant handle it.</b></p>
</div>

<p align="center">
  <img src="https://raw.githubusercontent.com/Rudragupta8777/EchoMi_App/main/app/src/main/res/drawable/app_screenshot.png" alt="EchoMi App Screenshots" width="80%" />
</p>

## 🌟 The Problem
What happens when you're **busy**, in a **meeting** or your **phone is on silent** and someone **urgently** needs to reach you? What about a **delivery guy who's lost**, or an **unknown number** that might be **important**?

You're forced to choose: **interrupt your focus** or **risk missing something critical**.

## ✨ Our Solution
**EchoMi** is an intelligent Android assistant that screens and answers calls **on your behalf**. It doesn't just block spam - it interacts with callers, understands their intent and takes smart actions, ensuring you only get alerted for what truly matters.

<div align="center">
  <table>
    <tr>
      <td width="50%">
        <h3 align="center">📱 For You (The User)</h3>
        <ul>
          <li>Get "break-through" alerts for emergencies</li>
          <li>Approve/Decline OTP requests from the app</li>
          <li>Receive summaries from unknown callers</li>
          <li>Read full transcripts of AI-handled calls</li>
          <li>Stop being interrupted by non-urgent calls</li>
        </ul>
      </td>
      <td width="50%">
        <h3 align="center">🤖 For The Caller (The AI)</h3>
        <ul>
          <li>Guide lost delivery drivers to your location</li>
          <li>Securely verify and provide OTPs</li>
          <li>Understand the caller's intent and urgency</li>
          <li>Politely get context from unknown numbers</li>
          <li>Provide a helpful experience, not a dumb voicemail</li>
        </ul>
      </td>
    </tr>
  </table>
</div>

## 🚀 Key Features: Real-World Scenarios

### 🚨 The "Break-Through" Emergency Alert
* **Scenario:** Your phone is on silent, but a family member has an emergency.
* **EchoMi's Action:** The AI detects the caller's urgency. It **immediately** overrides your silent mode, raises the volume, and plays a loud, continuous alert sound. This alert won't stop until you personally open the EchoMi app, guaranteeing you see what's happening.

### 📦 Smart Delivery & OTP Automation
* **Scenario:** A delivery driver calls for an OTP, but you're in a meeting.
* **EchoMi's Action:** The AI takes the call.
    1.  It asks the driver for a **Tracking ID**.
    2.  The app securely scans your recent SMS messages for that specific ID.
    3.  **If a match is found:** The AI provides the correct OTP to the driver. Your parcel is delivered.
    4.  **If no match is found (or no ID given):** The AI sends a push notification *to you* with a simple "Approve" or "Decline" prompt. Only if you tap "Approve" will the AI share the OTP.

### 🗺️ AI Location Guide for Deliveries
* **Scenario:** A delivery driver is lost and can't find your address.
* **EchoMi's Action:** Instead of bothering you, the AI assistant accesses your saved location (via Google Maps integration) and provides clear, simple directions to the driver, guiding them directly to your doorstep.

### 📝 Unknown Caller Summaries
* **Scenario:** You miss a call from an unknown number. Is it a client? A scam? A wrong number?
* **EchoMi's Action:** The AI answers, politely talks to the person, and determines **why** they are calling. After the call, you get a concise summary in your call log (e.g., "John from XYZ Corp called about your job application"). You can then decide whether to call back—no guesswork needed.

### 📜 Full Call Transcripts
For **every single call** the AI handles, a full, word-for-word transcript is securely saved in the app. You can review the entire conversation at any time.

## 🛡️ Privacy & Security First
This power requires trust. EchoMi is built with privacy at its core.

<div align="center"> 
  <table> 
    <tr> 
      <td align="center"> 
        <h3>🚫</h3> <b>Selective Handling</b><br> 
        <small>EchoMi *only* interacts with calls the AI attends. Your personal calls remain 100% private.</small> 
      </td> 
      <td align="center"> 
        <h3>🔐</h3> <b>Secure OTP Flow</b><br> 
        <small>OTPs are *never* shared without Tracking ID verification or your explicit in-app approval.</small> 
      </td> 
      <td align="center"> 
        <h3>🔒</h3> <b>Permission Control</b><br> 
        <small>The app only accesses SMS/Location when *actively handling* an AI-assisted call for a specific task.</small> 
      </td> 
      <td align="center"> 
        <h3>📄</h3> <b>Private Transcripts</b><br> 
        <small>All call summaries and transcripts are stored securely on your device for your eyes only.</small> 
      </td> 
    </tr> 
  </table> 
</div>

## 🛠️ Tech Stack

<div align="center">
  <table>
    <tr>
      <td align="center"><img src="https://cdn.simpleicons.org/kotlin" width="50px"/><br/>Kotlin</td>
      <td align="center"><img src="https://cdn.simpleicons.org/android" width="50px"/><br/>Android SDK</td>
      <td align="center"><img src="https://cdn.simpleicons.org/nodedotjs" width="50px"/><br/>Node.js</td>
      <td align="center"><img src="https://cdn.simpleicons.org/firebase" width="50px"/><br/>Firebase (FCM)</td>
      <td align="center"><img src="https://cdn.simpleicons.org/mongodb" width="50px"/><br/>MongoDB</td>
      <td align="center"><img src="https://cdn.simpleicons.org/twilio" width="50px"/><br/>Twilio</td>
      <td align="center"><img src="https://cdn.simpleicons.org/deepgram" width="50px"/><br/>Deepgram</td>
      <td align="center"><img src="https://cdn.simpleicons.org/googlemaps" width="50px"/><br/>Google Maps</td>
      <td align="center"><img src="https://cdn.simpleicons.org/openai" width="50px"/><br/>OpenAI</td>
      <td align="center"><img src="https://cdn.simpleicons.org/python" width="50px"/><br/>Python</td>
    </tr>
  </table>
</div>

---

## 📱 System Architecture
```

┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│                 │      │                 │      │                 │
│  EchoMi Android │◄────►│  Firebase (FCM) │◄────►│  EchoMi Backend │
│      App        │      │ (Notifications) │      │    (Node.js)    │
│ (Kotlin/XML)    │      │                 │      │                 │
└────────┬────────┘      └─────────────────┘      └────────┬────────┘
  (Fetches SMS)                                  (Handles Call/AI Logic)
(Fetches Location)                                         │
         └─────────────────────────────────────────────────┘
                                                           │
                                        ┌──────────────────┬──────────────────┐
                                        │                  │                  │
                                        ▼                  ▼                  ▼
                              ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
                              │                 │ │                 │ │                 │
                              │  Twilio Voice   │ │  Deepgram STT   │ │     AI Model    │
                              │  (Call Gateway) │ │ (Transcription) │ │   (Bot/Logic)   │
                              └─────────────────┘ └─────────────────┘ └─────────────────┘

```

---

## ⚙️ Implementation Status

| Feature                      | Status          | Notes                                                |
|------------------------------|-----------------|------------------------------------------------------|
| User Authentication          | ✅ Complete    | Google OAuth via Firebase                            |
| AI Call Answering            | ✅ Complete    | Twilio + Deepgram stream working                     |
| Emergency "Break-Through"    | ✅ Complete    | Overrides silent mode and plays alert                |
| Smart OTP Flow               | ✅ Complete    | SMS scanning, ID matching, and in-app approval       |
| AI Location Guide            | ✅ Complete    | Google Maps API integration                          |
| Unknown Caller Summaries     | ✅ Complete    | AI gets context and generates summary                |
| Call Transcripts             | ✅ Complete    | All AI-handled calls are transcribed and saved       |
| Voicemail Feature            | ⏳ In Progress | Recording & storing voicemails                       |

---

## 🚀 Download the App

You can **download the EchoMi APK** for testing directly from our GitHub Releases page:

➡️ **[Download the Latest APK (v1.0.0)](https://github.com/Rudragupta8777/EchoMi_App/releases/tag/v1.0.0)** ⬅️

> 🔒 *Note: This is an initial release. You will need to grant Call, SMS, and Location permissions for the app to function correctly.*

---

## 🔗 Related Repositories

-   [EchoMi AI Model](https://github.com/ruchit2005/EchoMi-AI-Model)
-   [EchoMi Backend](https://github.com/Rudragupta8777/EchoMi_Backend)

## 👥 Meet Our Team

<div align="center">
  <table>
    <tr>
      <td align="center">
        <h3>Rudra Gupta</h3>
        <p><i>App & Backend Developer</i></p>
        <a href="https://github.com/Rudragupta8777"><img src="https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white" height="22" target="_blank"></a>
        <a href="https://www.linkedin.com/in/rudra-gupta-36827828b/"><img src="https://img.shields.io/badge/LinkedIn-0A66C2?style=flat-square&logo=linkedin&logoColor=white" height="22" target="_blank"></a>
      </td>
      <td align="center">
        <h3>Ruchit Gupta</h3>
<p><i>AI & Python Developer</i></p>
<a href="https://github.com/ruchit2005"><img src="https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white" height="22" target="_blank"></a>
<a href="https://www.linkedin.com/in/ruchit-gupta-608a6428b/"><img src="https://img.shields.io/badge/LinkedIn-0A66C2?style=flat-square&logo=linkedin&logoColor=white" height="22" target="_blank">
      </td>
    </tr>
  </table>
</div>


<div align="center">
  <h3>
    <i>An intelligent assistant that's always there, even when you can't be.</i>
  </h3>
   
  ---
</div>
