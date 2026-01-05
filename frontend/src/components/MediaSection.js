function formatVotes(votes) {
  if (votes >= 1000000) {
    return (votes / 1000000).toFixed(1) + 'M';
  }
  if (votes >= 1000) {
    return (votes / 1000).toFixed(1) + 'K';
  }
  return votes;
}

export function renderHighlights(highlights) {
    return `
        <section class="mb-12">
            <h2 class="text-2xl font-semibold">Awards & Honors</h2>
    
            <ul class="mt-4 space-y-2 text-slate-300">
                <li> Best Picture: <strong>${highlights.oscars.bestPicture.title}</strong></li>
                <li> Best Actor: ${highlights.oscars.bestActor.name}</li>
                <li> Best Actress: ${highlights.oscars.bestActress.name}</li>
            </ul>
        </section>
    `;
}

export function renderMovies(movies) {
    return `
    <section class="mt-12 space-y-4">
      <h2 class="text-2xl font-semibold">Top Movies</h2>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        ${movies.map(movie => `
          <div class="bg-slate-900/70 p-4 rounded-xl">
            <h3 class="font-medium">${movie.title}</h3>
            <p class="text-slate-400 text-sm"> Released: ${new Date(movie.releaseDate).toLocaleDateString('en-EN', { year: 'numeric', month: 'long', day: 'numeric' })}</p>
            <p class="text-slate-400 text-sm">⭐ ${movie.rating} (${formatVotes(movie.votes)} votes)</p>
          </div>
        `).join("")}
      </div>
    </section>
  `;
}

export function renderSeries(series) {
  return `
    <section class="mt-12 space-y-4">
      <h2 class="text-2xl font-semibold">Top Series</h2>

      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        ${series.map(serie => `
          <div class="bg-slate-900/70 p-4 rounded-xl">
            <h3 class="font-medium">${serie.title}</h3>
            <p class="text-slate-400 text-sm"> Released: ${new Date(serie.releaseDate).toLocaleDateString('en-EN', { year: 'numeric', month: 'long', day: 'numeric' })}</p>
            <p class="text-slate-400 text-sm">⭐ ${serie.rating} (${formatVotes(serie.votes)} votes)</p>
          </div>
        `).join("")}
      </div>
    </section>
  `;
}
