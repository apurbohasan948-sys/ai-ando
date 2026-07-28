# AuraAssistant AI - Autonomous Web Agent App

**AuraAssistant** হলো একটি এআই-পাওয়ার্ড অটোমেটাক ওয়েব এজেন্ট (Android Application)। এতে Gemini 3.5 Flash, ভয়েস কমান্ড (বাংলা ও ইংরেজি), অটোমেটিক ওয়েব ভিউ কন্ট্রোল এবং রুম সিকিউর ভল্ট ফিচার যুক্ত রয়েছে।

---

## 📱 কম্পিউটার ছাড়া মোবাইল দিয়ে GitHub Actions থেকে APK বিল্ড করার উপায় (Step-by-Step)

আপনার কাছে কম্পিউটার না থাকলেও আপনি সরাসরি আপনার অ্যান্ড্রয়েড মোবাইল ফোন থেকে **GitHub Actions** ব্যবহার করে অ্যাপটির **APK** বানিয়ে ডাউনলোড করতে পারবেন।

### ধাপ ১: GitHub-এ রিপোজিটরি আপলোড / Fork করুন
১. আপনার গিটহাব একাউন্টে লগইন করুন।
২. এই প্রজেক্টটি গিটহাবে আপলোড (Push) করুন অথবা **Fork** করুন।

---

### ধাপ ২: Gemini API Key সিক্রেট হিসেবে যুক্ত করুন
১. আপনার গিটহাব রিপোজিটরিতে যান।
২. উপরের **Settings** ট্যাবে ক্লিক করুন।
৩. বাম পাশের মেনু থেকে **Secrets and variables** -> **Actions** এ যান।
৪. **New repository secret** বাটনে চাপ দিন।
৫. Secret-এর নাম লিখুন: `GEMINI_API_KEY`
৬. Secret-এর ঘরে আপনার [Google AI Studio](https://aistudio.google.com/app/apikey) থেকে পাওয়া Gemini API Key বসিয়ে **Add secret** দিন।

---

### ধাপ ৩: APK বিল্ড চালু করুন (GitHub Actions)
১. রিপোজিটরির **Actions** ট্যাবে যান।
২. বাম পাশ থেকে **Build Android APK** বেছে নিন।
৩. **Run workflow** বাটনে ক্লিক করে **Run workflow** চাপুন।
৪. প্রায় ২-৩ মিনিট সময় নেবে বিল্ড সম্পন্ন হতে।

---

### ধাপ ৪: APK ডাউনলোড ও ইনস্টল করুন
১. বিল্ড সম্পন্ন হলে (সবুজ টিক চিহ্ন আসলে) **Build Android APK** জবটির উপর ক্লিক করুন।
২. নিচের দিকে স্ক্রোল করে **Artifacts** সেকশনে যান।
৩. **AuraAssistant-APK** লিংকে ক্লিক করে Zip ফাইলটি ডাউনলোড করুন।
৪. ফাইলটি আনজিপ (Unzip) করলে **`app-debug.apk`** পাবেন।
৫. আপনার অ্যান্ড্রয়েড মোবাইলে ইন্সটল করুন এবং ব্যবহার শুরু করুন!

---

## 🌟 প্রধান ফিচারসমূহ (Key Features)

- **Autonomous WebView Navigation**: জেনারেটিভ এআই দিয়ে ওয়েবসাইট ন্যাভিগেশন ও স্ক্র্যাপ।
- **Bilingual Voice Command Engine**: বাংলা (বাংলা ব্যাকগ্রাউন্ড স্পীচ) এবং ইংরেজিতে কথা বলে ইনস্ট্রাকশন দেওয়ার সুবিধা।
- **Gemini 3.5 Flash Integration**: দ্রুত সিদ্ধান্ত গ্রহণ ও জাভাস্ক্রিপ্ট কোড তৈরি।
- **AES-256 Room Vault Storage**: পাসওয়ার্ড ও অটো-ফিল ক্রেডেনশিয়াল নিরাপদে সংরক্ষণের ব্যবস্থা।
- **Dynamic Memory Store**: প্রজেক্টে স্থায়ী `MemoryStore` এবং Room ইন্টিগ্রেশন যার মাধ্যমে এআই আপনার পছন্দের টাস্ক প্যাটার্ন মনে রাখে।

---

## 🛠 প্রযুক্তি ও ফ্রেমওয়ার্ক (Tech Stack)

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Local DB:** Room Database with KSP
- **Architecture:** Clean Architecture with ViewModel & StateFlow
- **AI Engine:** Gemini REST API (gemini-3.5-flash)
- **CI/CD:** GitHub Actions (Automated APK Builder)
