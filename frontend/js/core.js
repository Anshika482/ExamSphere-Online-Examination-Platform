/* Core: session storage, the fetch wrapper, formatting helpers and the hash router. */
window.ES = (function () {
    "use strict";

    const STORAGE_KEY = "examsphere.session";

    // ------------------------------------------------------------------ session
    const session = {
        read() {
            try {
                const raw = localStorage.getItem(STORAGE_KEY) || sessionStorage.getItem(STORAGE_KEY);
                if (!raw) return null;
                const data = JSON.parse(raw);
                if (!data || !data.token || !data.user) return null;
                if (data.expiresAt && Date.now() > data.expiresAt) {
                    this.clear();
                    return null;
                }
                return data;
            } catch (e) {
                return null;
            }
        },
        /** remember = true keeps the session after the browser closes; otherwise it lasts for this tab only. */
        save(auth, remember) {
            this.clear();
            const data = {
                token: auth.token,
                user: auth.user,
                expiresAt: Date.now() + (auth.expiresInSeconds || 7200) * 1000,
                remember: !!remember
            };
            (remember ? localStorage : sessionStorage).setItem(STORAGE_KEY, JSON.stringify(data));
        },
        updateUser(user) {
            const data = this.read();
            if (!data) return;
            data.user = user;
            (data.remember ? localStorage : sessionStorage).setItem(STORAGE_KEY, JSON.stringify(data));
        },
        clear() {
            localStorage.removeItem(STORAGE_KEY);
            sessionStorage.removeItem(STORAGE_KEY);
        },
        get user() {
            const data = this.read();
            return data ? data.user : null;
        },
        get token() {
            const data = this.read();
            return data ? data.token : null;
        }
    };

    // ------------------------------------------------------------------ API
    class ApiError extends Error {
        constructor(message, extra) {
            super(message);
            Object.assign(this, { status: 0, code: null, errors: null, retryAfterSeconds: null, network: false }, extra);
        }
    }

    function buildQuery(query) {
        if (!query) return "";
        const params = new URLSearchParams();
        Object.keys(query).forEach(function (key) {
            const value = query[key];
            if (value !== undefined && value !== null && value !== "") params.append(key, value);
        });
        const text = params.toString();
        return text ? "?" + text : "";
    }

    /**
     * Calls the REST API and returns the parsed body ({ message, data }).
     * Throws ApiError with the backend's message / field errors on failure.
     */
    async function api(path, options) {
        const opts = options || {};
        const headers = { Accept: "application/json" };
        const token = session.token;
        if (token) headers.Authorization = "Bearer " + token;
        if (opts.body !== undefined) headers["Content-Type"] = "application/json";

        let response;
        try {
            response = await fetch(window.ES_CONFIG.API_BASE + path + buildQuery(opts.query), {
                method: opts.method || "GET",
                headers: headers,
                body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined
            });
        } catch (e) {
            throw new ApiError("Cannot reach the server. Check that the backend is running and try again.", { network: true });
        }

        let payload = null;
        const text = await response.text();
        if (text) {
            try { payload = JSON.parse(text); } catch (e) { payload = null; }
        }

        if (response.ok) return payload || { message: "OK", data: null };

        // An expired or revoked token: clear the session and send the user to the login page.
        if (response.status === 401 && token && !path.startsWith("/api/auth/")) {
            session.clear();
            sessionStorage.setItem("examsphere.notice", "Your session has expired. Please log in again.");
            router.go("/login");
            throw new ApiError("Your session has expired. Please log in again.", { status: 401, code: "SESSION_EXPIRED" });
        }
        const fallback = response.status >= 500 ? "The server ran into a problem. Please try again." : "Something went wrong.";
        throw new ApiError((payload && payload.message) || fallback, {
            status: response.status,
            code: payload && payload.code,
            errors: payload && payload.errors,
            retryAfterSeconds: payload && payload.retryAfterSeconds
        });
    }

    // ------------------------------------------------------------------ helpers
    const ESCAPES = { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" };

    /** Escapes text before it is placed into HTML. Every piece of user or server data goes through this. */
    function esc(value) {
        if (value === null || value === undefined) return "";
        return String(value).replace(/[&<>"']/g, function (ch) { return ESCAPES[ch]; });
    }

    function parseDate(value) {
        if (!value) return null;
        const date = new Date(value);
        return isNaN(date.getTime()) ? null : date;
    }

    const fmt = {
        date(value) {
            const d = parseDate(value);
            return d ? d.toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" }) : "-";
        },
        dateTime(value) {
            const d = parseDate(value);
            return d ? d.toLocaleString(undefined, { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" }) : "-";
        },
        ago(value) {
            const d = parseDate(value);
            if (!d) return "";
            const seconds = Math.max(0, Math.round((Date.now() - d.getTime()) / 1000));
            if (seconds < 60) return "just now";
            if (seconds < 3600) return Math.floor(seconds / 60) + " min ago";
            if (seconds < 86400) return Math.floor(seconds / 3600) + " h ago";
            if (seconds < 86400 * 7) return Math.floor(seconds / 86400) + " d ago";
            return fmt.date(value);
        },
        pct(value) {
            const n = Number(value || 0);
            return (Math.round(n * 10) / 10).toString().replace(/\.0$/, "") + "%";
        },
        num(value) {
            const n = Number(value || 0);
            return (Math.round(n * 100) / 100).toString();
        },
        clock(totalSeconds) {
            const s = Math.max(0, Math.floor(totalSeconds));
            const m = Math.floor(s / 60);
            return String(m).padStart(2, "0") + ":" + String(s % 60).padStart(2, "0");
        },
        duration(minutes) {
            return minutes + " min";
        },
        /** Value for <input type="datetime-local"> from an ISO local date-time. */
        inputDateTime(value) {
            return value ? String(value).slice(0, 16) : "";
        },
        label(value) {
            if (!value) return "";
            const text = String(value).replace(/_/g, " ").toLowerCase();
            return text.charAt(0).toUpperCase() + text.slice(1);
        },
        initials(name) {
            return String(name || "?").trim().split(/\s+/).slice(0, 2).map(function (p) { return p.charAt(0).toUpperCase(); }).join("");
        }
    };

    function debounce(fn, wait) {
        let timer;
        return function () {
            const args = arguments, self = this;
            clearTimeout(timer);
            timer = setTimeout(function () { fn.apply(self, args); }, wait);
        };
    }

    // ------------------------------------------------------------------ router
    const routes = [];
    let leaveHandlers = [];
    let navToken = 0;

    const router = {
        /**
         * @param pattern e.g. "/student/exam/:id"
         * @param config  { role, layout: "public" | "app" | "bare", title, nav, render(ctx) }
         */
        add(pattern, config) {
            const keys = [];
            const regex = new RegExp("^" + pattern.replace(/:[a-zA-Z]+/g, function (m) {
                keys.push(m.slice(1));
                return "([^/]+)";
            }) + "/?$");
            routes.push(Object.assign({ pattern: pattern, regex: regex, keys: keys }, config));
        },
        go(path) {
            const target = "#" + path;
            if (location.hash === target) this.resolve(); else location.hash = target;
        },
        replace(path) {
            history.replaceState(null, "", "#" + path);
            this.resolve();
        },
        current() {
            const hash = location.hash.replace(/^#/, "") || "/";
            const index = hash.indexOf("?");
            const path = index >= 0 ? hash.slice(0, index) : hash;
            const query = {};
            if (index >= 0) new URLSearchParams(hash.slice(index + 1)).forEach(function (v, k) { query[k] = v; });
            return { path: path || "/", query: query };
        },
        home(user) {
            if (!user) return "/login";
            return { STUDENT: "/student/dashboard", INSTRUCTOR: "/instructor/dashboard", ADMIN: "/admin/dashboard" }[user.role] || "/login";
        },
        async resolve() {
            const here = this.current();
            const token = ++navToken;
            leaveHandlers.forEach(function (fn) { try { fn(); } catch (e) { /* ignore */ } });
            leaveHandlers = [];
            ES.ui.closeModal();

            let match = null, params = {};
            for (const route of routes) {
                const m = route.regex.exec(here.path);
                if (m) {
                    match = route;
                    route.keys.forEach(function (key, i) { params[key] = decodeURIComponent(m[i + 1]); });
                    break;
                }
            }
            const user = session.user;
            if (!match) return ES.pages.notFound();

            // Route guards. They are a convenience only: the backend enforces every permission again.
            if (match.role && !user) {
                sessionStorage.setItem("examsphere.returnTo", location.hash.replace(/^#/, ""));
                return this.replace("/login");
            }
            if (match.role && user.role !== match.role) return this.replace(this.home(user));
            if (match.guestOnly && user) return this.replace(this.home(user));

            document.title = (match.title ? match.title + " - " : "") + "ExamSphere";
            const view = ES.layout.mount(match, here.path);
            const ctx = {
                params: params,
                query: here.query,
                view: view,
                path: here.path,
                user: user,
                /** False once the user has navigated elsewhere; async pages check it before touching the DOM. */
                alive: function () { return token === navToken; },
                onLeave: function (fn) { leaveHandlers.push(fn); }
            };
            try {
                await match.render(ctx);
            } catch (error) {
                if (ctx.alive() && error.code !== "SESSION_EXPIRED") {
                    view.innerHTML = ES.ui.errorState(error.message || "Something went wrong.", true);
                    const retry = view.querySelector("[data-retry]");
                    if (retry) retry.addEventListener("click", function () { router.resolve(); });
                }
            }
            if (ctx.alive()) {
                window.scrollTo(0, 0);
                const main = document.getElementById("main");
                if (main) main.setAttribute("tabindex", "-1");
            }
        },
        start() {
            window.addEventListener("hashchange", function () { router.resolve(); });
            // Coming back with the browser's back button after logout must not show cached private pages.
            window.addEventListener("pageshow", function (event) { if (event.persisted) router.resolve(); });
            this.resolve();
        }
    };

    return { session: session, api: api, ApiError: ApiError, esc: esc, fmt: fmt, debounce: debounce, router: router, pages: {}, layout: {}, ui: {} };
})();
