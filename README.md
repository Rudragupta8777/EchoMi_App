<div align="center">
  <img src="https://ik.imagekit.io/d5u8bqewg/Frame%2048095840.png?updatedAt=1756613303888" alt="EchoMI Logo" width="20%" />

  # EchoMI: Your AI Personal Call Assistant
  <p>
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
    <img src="https://img.shields.io/badge/Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white" />
    <img src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white" />
    <img src="https://img.shields.io/badge/Twilio-F22F46?style=for-the-badge&logo=twilio&logoColor=white" />
    <img src="https://img.shields.io/badge/Deepgram-13EF93?style=for-the-badge&logo=deepgram&logoColor=black" />
    <img src="https://img.shields.io/badge/OpenAI-412991?style=for-the-badge&logo=openai&logoColor=white" />
    <img src="https://img.shields.io/badge/Google_Maps-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white" />
  </p>

  **Your intelligent receptionist that answers calls, assists deliveries, and alerts you in emergencies.**
</div>

---

## 🌟 Overview
EchoMI is an AI-powered personal assistant that automatically answers calls when you can't. It understands the context of calls, assists delivery drivers, and sends immediate alerts for emergencies. The companion Android app allows you to review call transcripts and customize AI prompts.  

---

## 🗣️ How It Works

### 1️⃣ Call Handling Flow
1. **Intercept** – EchoMI answers calls automatically when busy.
2. **Transcribe** – Deepgram converts the caller's voice into text in real-time.
3. **Understand** – AI (Python service) analyzes context, call history, and caller type.
4. **Respond** – AI generates a human-like response.
5. **Synthesize** – Converts AI response into realistic audio.
6. **Converse** – Audio is streamed live back to the caller.

### 2️⃣ Smart Features
| Feature | Description |
|---------|-------------|
| Smart Delivery Assist | Guides drivers using live directions via Google Maps/Mapbox. |
| Urgent Call Alerts | Detects emergencies and sends high-priority FCM notifications. |
| Call History | Full transcripts and AI-generated summaries available in-app. |
| Custom Prompts | Define AI responses for family, colleagues, or unknown numbers. |

---

## 📞 Core Features
- Hands-free, automatic call answering  
- Real-time AI conversation with caller  
- Customizable prompts  
- Review call transcripts & summaries  
- Voicemail and message-taking capabilities  

## 🧠 Advanced AI Capabilities
- **Delivery Assistance** – Guides drivers live  
- **Emergency Detection** – Detects urgent keywords  
- **OTP Handling** – Manages delivery codes securely (WIP)  
- **Contextual Understanding** – Differentiates family, work, unknown callers  

---

## 🛠️ Tech Stack

<div align="center">
  <table>
    <tr>
      <td align="center"><img src="https://cdn.simpleicons.org/kotlin" width="50px"/><br/>Kotlin</td>
      <td align="center"><img src="https://cdn.simpleicons.org/nodedotjs" width="50px"/><br/>Node.js</td>
      <td align="center"><img src="https://cdn.simpleicons.org/mongodb" width="50px"/><br/>MongoDB</td>
      <td align="center"><img src="https://cdn.simpleicons.org/firebase" width="50px"/><br/>Firebase</td>
      <td align="center"><img src="https://cdn.simpleicons.org/python" width="50px"/><br/>Python</td>
      <td align="center"><img src="https://cdn.simpleicons.org/flask" width="50px"/><br/>Flask</td>
      <td align="center"><img src="https://cdn.simpleicons.org/twilio" width="50px"/><br/>Twilio</td>
      <td align="center"><img src="https://cdn.simpleicons.org/deepgram" width="50px"/><br/>Deepgram</td>
      <td align="center"><img src="https://cdn.simpleicons.org/openai" width="50px"/><br/>OpenAI</td>
      <td align="center"><img src="https://cdn.simpleicons.org/googlemaps" width="50px"/><br/>Google Maps</td>
    </tr>
  </table>
</div>

---

## 📱 System Architecture

```
          [Caller]
             │
             ▼
┌──────────────────────────┐      ┌──────────────────────────┐
│      Twilio Voice        │      │      Android App         │
└────────────┬─────────────┘      └────────────┬─────────────┘
       (Call & Audio)                     (REST API)
             │                                 │ 
             ▼                                 ▼
┌──────────────────────────┐      ┌──────────────────────────┐
│    Node.js Backend       │◄────►│   Python AI Service      │
│  (Express, WebSockets)   │      │ (Flask, OpenAI, Maps)    │
└────────────┬─────────────┘      └──────────────────────────┘
             │                                 ▲
             ▼                                 │ (FCM Push)
┌──────────────────────────┐                   │
│    MongoDB Database      │───────────────────┘
└──────────────────────────┘

````

---

## ⚙️ Implementation Status

|         Component         |      Status      |                Notes                      |
|---------------------------|------------------|------------------------------------------|
| User Authentication       | ✅ Complete     | Firebase Auth login                       |
| Live Call Handling        | ✅ Complete     | Twilio -> Node.js -> AI -> Deepgram       |
| Delivery Directions       | ✅ Complete     | Google Maps/Mapbox directions integrated  |
| Emergency Detection       | ✅ Complete     | Keyword detection & FCM push              |
| Custom Prompt Management  | ✅ Complete     | Users can set prompts via app             |
| Call Transcript Storage   | ✅ Complete     | Saving full transcripts to MongoDB        |
| AI Call Summarization     | ⏳ In Progress  | Fine-tuning for accurate summaries        |
| Voicemail Feature         | ⏳ In Progress  | Recording & storing voicemails            |
| Secure OTP Handling       | 🟡 Planned      | Researching secure delivery OTP methods   |

---

## 🚀 Getting Started

### Prerequisites
- Node.js v18+  
- Python v3.9+  
- MongoDB account  
- API Keys: Twilio, Deepgram, OpenAI, Google Maps/Mapbox  

### Backend Setup (Node.js)
```bash
git clone https://github.com/Rudragupta8777/EchoMi_Backend.git
cd EchoMi_Backend
npm install express mongoose twilio @deepgram/sdk axios ws dotenv cors firebase-admin
# Add .env keys
npm start
````

### AI Service Setup (Python)

```bash
git clone https://github.com/ruchit2005/EchoMi-AI-Model.git
cd EchoMi-AI-Model
pip install -r requirements.txt
# Add .env keys
python app.py
```

### Expose Services

```bash
ngrok http 5000
# Use ngrok URL in Twilio webhook
```

---

## 🔗 Related Repositories

- [EchoMi AI Model](https://github.com/ruchit2005/EchoMi-AI-Model.git)
- [EchoMi Backend](https://github.com/Rudragupta8777/EchoMi_Backend.git)

---

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
<a href="https://www.linkedin.com/in/ruchit-gupta-608a6428b/"><img src="https://img.shields.io/badge/LinkedIn-0A66C2?style=flat-square&logo=linkedin&logoColor=white" height="22" target="_blank"></a>
      </td>
    </tr>
  </table>
</div>

---

<div align="center">
<h3><i>Your personal AI receptionist | Built to make life easier</i></h3>
</div>
