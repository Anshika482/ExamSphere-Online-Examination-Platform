/*
 * Frontend configuration. There are no secrets here - the browser only ever holds
 * the user's own access token after login.
 *
 * API_BASE:
 *   - When the UI is served by Spring Boot (http://localhost:8080) the API is on the same origin, so it is "".
 *   - When the frontend folder is served separately (for example VS Code Live Server on port 5500),
 *     requests go to the backend on port 8080. Change the URL below if your backend runs elsewhere.
 */
window.ES_CONFIG = {
    API_BASE: (location.port === "8080" || location.port === "") && location.protocol !== "file:"
        ? ""
        : "http://localhost:8080"
};
