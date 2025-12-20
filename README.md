# WikiCap

WikiCap is a web application that lets you enter any year and fetch nostalgic data such as news, top music and popular movies.
The results are presented in an artsy, interactive timeline.

---

## Table of Contents

- [Background](#background)
- [Functions](#functions)
- [Tech Stack](#tech-stack)
- [Get Started](#get-started)
  - [Requirements](#requirements)
  - [Installation](#installation)
  - [Run Locally](#run-locally)

## Background

## Functions

## Tech Stack

**Backend**

- Python – FastAPI (API layer / data aggregation)
- Node.js - For tailwind

**Frontend**

- HTML
- CSS – Tailwind CSS
- JavaScript – Vanilla JS

**External APIs**

- Wikipedia API – for notable events & general info
- Last.fm API – for top tracks / artists by year
- TMDB API – for popular movies by year

---

## Get Started

1. **Python Version**: Ensure you have [Python](https://www.python.org/downloads/) 3.10 or higher installed. Check your version by running:
   ```bash
   python3 --version
   ```
   or
   ```bash
   python --version
   ```

### Requirements

### Installation

1. **Create and activate your Virtual Environment**

### MacOS / Linux

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
```

### **Windows**

```bash
cd backend
python -m venv .venv
.\.venv\Scripts\activate
```

2. ### **Install backend dependencies**

```bash
pip install -r requirements.txt
```

3. **Create your .env file in the backend folder**

Create `backend/.env` and add your API keys.

Example:
```
API_KEY_1=YOUR_KEY
API_KEY_2=YOUR_KEY2
```

4. **Install frontend dependencies**

```bash
cd frontend
npm install
```

5. **Start Tailwind in development mode**

```bash
npm run dev
```

6. **Start the FastAPI server**

```bash
cd backend
uvicorn app.main:app --reload
```

---

### Run Locally

**The server will run at:**

`http://127.0.0.1:8000/`

---
