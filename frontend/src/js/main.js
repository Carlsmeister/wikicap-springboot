import { renderMovies } from "../components/MoviesSection.js";
const app = document.getElementById("app")

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