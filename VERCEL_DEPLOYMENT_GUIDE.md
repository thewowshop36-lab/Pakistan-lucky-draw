# 🇵🇰 Pakistan Lottery — Vercel Deployment & Platform Guide

## اردو رہنمائی (Urdu Guide)

### ورسل (Vercel) پر ڈپلائی کیوں نہیں ہو رہا تھا؟
1. **اصل وجہ:** ورسل (Vercel) صرف اور صرف **ویب سائٹس اور ویب ایپس (Web Projects)** کو ہوسٹ کرتا ہے۔ اس پروجیکٹ میں پہلے صرف اینڈرائیڈ موبائل ایپ (`.gradle`, `app/`) کا ڈھانچہ موجود تھا اور ویب کی بنیادی فائلیں (`package.json` اور `vercel.json`) موجود نہیں تھیں، جس کی وجہ سے ورسل کو کوئی بلڈ فریم ورک نہیں مل رہا تھا اور ایرر آ رہا تھا۔
2. **ہم نے کیا حل کیا؟**
   - ہم نے روٹ پر **`package.json`** شامل کر دیا ہے۔
   - ہم نے **`vercel.json`** کی درست کنفیگریشن شامل کر دی ہے۔
   - ہم نے `public/index.html` میں ایک مکمل، جدید، موبائل ریسپانسیو **ویب ایپلیکیشن (Web App)** تیار کر دی ہے جو بالکل متحدہ عرب امارات کی "The UAE Lottery" کے ڈیزائن، ایمرلڈ گرین اور گولڈ تھیم، جاز کیش/ایزی پیسہ/بینک ٹرانسفر، 6+1 لکی ڈرا، اور 100% مینوئل ایڈمن ونر سلیکشن کے ساتھ لائیو چلتی ہے۔

---

### ورسل (Vercel) پر 1-کلک سے ڈپلائی کرنے کا طریقہ:
1. **GitHub پر پش کریں (Push to GitHub):**
   - اوپر دائیں کونے میں **Settings** یا **Git** آپشن سے اس کوڈ کو اپنے GitHub اکاؤنٹ میں پش/کمٹ کریں۔
2. **Vercel پر جائیں (Go to Vercel):**
   - [vercel.com](https://vercel.com) کھولیں اور لاگ ان کریں۔
   - **Add New Project** پر کلک کریں۔
   - اپنے GitHub سے `pakistan-lottery` ریپو کو منتخب کریں (Import)۔
3. **Deploy پر کلک کریں:**
   - **Framework Preset:** `Other` (خود بخود منتخب ہوگا)۔
   - **Output Directory:** `public` (خود بخود `vercel.json` سے لے لے گا)۔
   - **Deploy** کے بٹن پر کلک کریں۔
   - صرف 30 سیکنڈ میں آپ کی ویب سائٹ لائیو ہو جائے گی! 🚀

---

## English Deployment Instructions

### Why Vercel was failing:
Vercel is a web hosting platform that requires a `package.json` or `vercel.json` config and a web entry point. Because the repository originally only contained Android Gradle files (`build.gradle.kts`, `app/src`), Vercel couldn't detect a web framework and threw a build error.

### What has been added:
1. `vercel.json` — Pre-configured for instant zero-configuration static deployment.
2. `package.json` — Standard npm definition with build script recognized by Vercel.
3. `public/index.html` — Fully interactive, responsive web portal mirroring the Android application (6+1 Lucky Draw, Scratch Cards, JazzCash/EasyPaisa wallet, and Super Admin manual winner tool).
4. `public/logo.jpg` & `public/banner.jpg` — Official high-resolution visual branding assets.
