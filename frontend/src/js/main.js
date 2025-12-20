import { renderMovies } from "../components/MoviesSection.js";

document.body.classList.add(
  "min-h-screen",
  "bg-slate-950",
  "bg-[radial-gradient(circle_at_top,#1f2937_0,#020617_45%,#000_100%)]",
  "text-slate-50",
  "flex",
  "items-center",
  "justify-center",
  "px-4"
);

const app = document.getElementById("app")

app.innerHTML = `
  <header class="space-y-2">
    <h1 class="text-4xl md:text-5xl font-semibold tracking-tight uppercase p-8">WikiCap</h1>
  </header>

  <form id="searchForm" class="flex flex-col sm:flex-row gap-3 justify-center">
    <input
      id="year"
      type="text"
      name="year"
      placeholder="Till exempel 1997"
      class="w-full sm:w-48 bg-slate-900/80 border border-slate-700/80 rounded-full px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 placeholder:text-slate-500"
    />
    <button
      id="fetchYear"
      type="submit"
      class="inline-flex items-center justify-center rounded-full px-6 py-3 text-sm font-medium bg-indigo-500 hover:bg-indigo-400 transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 focus:ring-offset-slate-950"
    >
      Hämta år
    </button>
  </form>

  <div id="movieSection"></div>

`;

document.getElementById("searchForm").addEventListener("submit", getMovies);


const baseURL = "http://localhost:8000/api/v1";

async function getMovies(event) {
  event.preventDefault();
  const year = document.getElementById("year").value;
  const URL = `${baseURL}/year/${year}`

  const options = {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        }
    }

    const response = await fetch(URL, options);
    const data = await response.json();
    console.log(data);
    document.getElementById("movieSection").innerHTML = renderMovies(data.movies.topMovies);

}