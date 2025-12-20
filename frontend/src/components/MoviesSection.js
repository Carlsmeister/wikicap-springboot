export function renderMovies(movies) {
  return `
    <section class="mt-12 space-y-4">
      <h2 class="text-2xl font-semibold">Topp filmer</h2>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        ${movies.map(movie => `
          <div class="bg-slate-900/70 p-4 rounded-xl">
            <h3 class="font-medium">${movie.title}</h3>
            <p class="text-slate-400 text-sm"> Released: ${new Date(movie.releaseDate).toLocaleDateString('en-EN', { month: 'long', day: 'numeric' })}</p>
            <p class="text-slate-400 text-sm">⭐ ${movie.rating}</p>
          </div>
        `).join("")}
      </div>
    </section>
  `;
}
