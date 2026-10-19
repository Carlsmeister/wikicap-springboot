# WikiCap

WikiCap is a year-recap web app: enter a year and it shows that year's events, Nobel prizes,
films and music, gathered from Wikipedia, TMDB, Spotify and others. **The app is
`backendjava/` (Spring Boot) plus `frontend/` (vanilla JS).**

## Testing

All commands run from the repo root.

- Full suite: `mvn -q -f backendjava/pom.xml test && (cd frontend && npm test)`
- Smoke: `mvn -q -f backendjava/pom.xml -Dtest=WikicapApplicationTests test`
- One test: Java `mvn -q -f backendjava/pom.xml -Dtest={test} test`, `{test}` as `Class#method`; JS `node --test --test-name-pattern="{test}" "frontend/test/**/*.test.js"`, `{test}` as the `test()` title.
- Coverage on changed lines: Absent — no JaCoCo or c8 configured.
- Coupling change: Absent.
- Architecture rules: Absent.

Naming: Java tests use JUnit 5, one `<Subject>Test` class per subject, with camelCase methods
saying what happens under which condition (`returnsWildcardOriginForApiRoutesOutsideYearPrefix`).
JS `node:test` titles say the same in plain words (`toEventsByMonth leaves undated events
without a prefix`).
