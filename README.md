# WikiCap

WikiCap is a web application that lets you enter any year and fetch nostalgic data such as news, top music and popular movies.
The results are presented in an artsy, interactive timeline.

---

## Table of Contents

- [Background](#background)
- [Functions](#functions)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Get Started](#get-started)
  - [Requirements](#requirements)
  - [Installation](#installation)
  - [Run Locally](#5-run-locally)
- [Environment Variables](#6-environment-variables)
- [Troubleshooting](#7-troubleshooting)

---

## Background

WikiCap is a projekt in making a REST API for a mashup site.
It works as an small "year recap machine" fetching multiple sources of data in to one response

This fork ports the team's Python backend to Spring Boot. The original lives at [WikiCap/year-overview](https://github.com/WikiCap/year-overview).

---

## Functions

Depending on the year, WikiCap fetches and renders:

- **Notable events & year context** (Wikipedia)
- **Nobel prize winners** (Wikipedia)
- **Top music and artistists** (Billboard year-end lists from Wikipedia, enriched with Spotify)
- **Popular movies and series** (via TMDB)
- **Oscars awards** (Via TheAward API)

---

## Tech Stack

### Backend

- **Java 25**
- **Spring Boot 4** (Spring MVC; WebClient from WebFlux for outbound calls)
- **WebClient** (HTTP client)
- **Jsoup** (HTML parsing of Wikipedia pages)
- **Caffeine** (caching)

### Frontend

- HTML
- CSS – Tailwind CSS (built with Node.js)
- JavaScript – Vanilla JS

### External APIs

- Wikipedia API – for notable events & general info
- Wikipedia (Billboard year-end list articles) – for the music rankings
- TMDB API – for popular movies by year
- Theawards API - for Academy awards
- Spotify API - for enriching the music rankings

---

## Project Structure

```text
.
├── .github/workflows/ci.yml
├── backendjava/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── .env.example
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── test/
│   ├── index.html
│   └── package.json
├── CLAUDE.md
└── README.md
```

---

## Get Started

### Requirements

- [Java](https://adoptium.net/) 25
- [Maven](https://maven.apache.org/)
- [Node.js](https://nodejs.org/) and npm

```bash
java --version
mvn --version
node --version
npm --version
```

---

## Installation

## 1. **Clone repository**

```bash
git clone https://github.com/Carlsmeister/wikicap-springboot.git
cd wikicap-springboot
```

## 2. **Create your .env file**

Create `backendjava/.env` and add your API keys [See Environment Variables](#6-environment-variables).

## 3. **Install frontend dependencies**

```bash
(cd frontend && npm install)
```

## 4. **Compile Tailwind**

```bash
(cd frontend && npm run build)
```

---

## 5. **Run locally**

Run everything from the repo root: the app loads `./backendjava/.env` relative to the working directory.

**Start the Spring Boot backend (port 8080)**

```bash
mvn -f backendjava/pom.xml spring-boot:run
```

**Serve the frontend**

```bash
npx serve frontend
```

The frontend calls the backend at `http://127.0.0.1:8080`.

**Run the tests**

```bash
mvn -q -f backendjava/pom.xml test && (cd frontend && npm test)
```

---

## 6. **Environment Variables**

# Create `backendjava/.env`

#### TMDB
TMDB_API_KEY=YOUR_TMDB_API_READ_ACCESS_TOKEN

TMDB_API_KEY is the long v4 "API Read Access Token" from your TMDB account settings, not the short v3 API key. It is sent as a `Bearer` token.

#### SPOTIFY ID
SPOTIFY_CLIENT_ID=YOUR_SPOTIFY_CLIENT_ID

#### SPOTIFY CLIENT SECRET
SPOTIFY_CLIENT_SECRET=YOUR_SPOTIFY_CLIENT_SECRET

Notes:

-Wikipedia endpoints usually don’t require an API key.

-`backendjava/.env` is already git-ignored. Never commit it.

---
## 7. **Troubleshooting**

**Serve the frontend with `npx serve frontend` and open the URL it prints.** Opening `frontend/index.html` straight from disk does not work: browsers block the module scripts on `file://`.

---
