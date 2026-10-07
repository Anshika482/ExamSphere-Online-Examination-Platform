/* UI toolkit: icons, toasts, modals, form helpers, state blocks, pagination and charts. */
(function (ES) {
    "use strict";
    const esc = ES.esc, fmt = ES.fmt;

    // ------------------------------------------------------------------ icons
    const PATHS = {
        dashboard: '<rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/>',
        book: '<path d="M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2z"/><path d="M4 19V5"/><path d="M9 8h6"/>',
        clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
        chart: '<path d="M4 20V10"/><path d="M10 20V4"/><path d="M16 20v-7"/><path d="M22 20H2"/>',
        user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
        users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0"/><path d="M16 4.6a3.5 3.5 0 0 1 0 6.8"/><path d="M18 14.5a6 6 0 0 1 3.5 5.5"/>',
        settings: '<circle cx="12" cy="12" r="3"/><path d="M19 12a7 7 0 0 0-.1-1.3l2-1.5-2-3.4-2.3.9a7 7 0 0 0-2.2-1.3L14 3h-4l-.4 2.4a7 7 0 0 0-2.2 1.3l-2.3-.9-2 3.4 2 1.5a7 7 0 0 0 0 2.6l-2 1.5 2 3.4 2.3-.9a7 7 0 0 0 2.2 1.3L10 21h4l.4-2.4a7 7 0 0 0 2.2-1.3l2.3.9 2-3.4-2-1.5c.07-.4.1-.85.1-1.3z"/>',
        logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/>',
        plus: '<path d="M12 5v14"/><path d="M5 12h14"/>',
        check: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
        x: '<path d="M6 6l12 12"/><path d="M18 6L6 18"/>',
        search: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/>',
        bell: '<path d="M6 9a6 6 0 0 1 12 0c0 6 2.5 7 2.5 7h-17S6 15 6 9z"/><path d="M10 20a2 2 0 0 0 4 0"/>',
        menu: '<path d="M4 7h16"/><path d="M4 12h16"/><path d="M4 17h16"/>',
        eye: '<path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/>',
        eyeOff: '<path d="M3 3l18 18"/><path d="M10.6 5.2A9.5 9.5 0 0 1 12 5c6.4 0 10 7 10 7a17 17 0 0 1-3.2 4"/><path d="M6.5 6.6C3.7 8.4 2 12 2 12s3.6 7 10 7a9.6 9.6 0 0 0 4.2-1"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/>',
        edit: '<path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17z"/><path d="M13.5 7.5l3 3"/>',
        trash: '<path d="M4 7h16"/><path d="M9 7V4h6v3"/><path d="M6 7l1 13h10l1-13"/><path d="M10 11v6"/><path d="M14 11v6"/>',
        flag: '<path d="M5 21V4"/><path d="M5 4h12l-2 4 2 4H5"/>',
        shield: '<path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/><path d="M9 12l2 2 4-4"/>',
        list: '<path d="M8 6h13"/><path d="M8 12h13"/><path d="M8 18h13"/><circle cx="4" cy="6" r="1"/><circle cx="4" cy="12" r="1"/><circle cx="4" cy="18" r="1"/>',
        award: '<circle cx="12" cy="9" r="6"/><path d="M8.5 14l-1.5 7 5-3 5 3-1.5-7"/>',
        activity: '<path d="M3 12h4l3-8 4 16 3-8h4"/>',
        alert: '<path d="M12 3l10 18H2z"/><path d="M12 10v5"/><circle cx="12" cy="18" r=".6"/>',
        info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6"/><circle cx="12" cy="7.5" r=".6"/>',
        mail: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/>',
        arrowLeft: '<path d="M19 12H5"/><path d="M11 6l-6 6 6 6"/>',
        arrowRight: '<path d="M5 12h14"/><path d="M13 6l6 6-6 6"/>',
        up: '<path d="M6 15l6-6 6 6"/>',
        down: '<path d="M6 9l6 6 6-6"/>',
        file: '<path d="M6 3h8l5 5v13H6z"/><path d="M14 3v5h5"/><path d="M9 13h6"/><path d="M9 17h6"/>',
        inbox: '<path d="M3 13l3-8h12l3 8v6H3z"/><path d="M3 13h5l1 3h6l1-3h5"/>',
        send: '<path d="M21 3L10 14"/><path d="M21 3l-7 18-4-7-7-4z"/>',
        lock: '<rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
        cap: '<path d="M2 9l10-5 10 5-10 5z"/><path d="M6 11.5V16c0 1.5 3 3 6 3s6-1.5 6-3v-4.5"/><path d="M22 9v5"/>',
        target: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1"/>'
    };
    function icon(name) {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' + (PATHS[name] || PATHS.info) + "</svg>";
    }
    const LOGO = '<svg viewBox="0 0 32 32" aria-hidden="true"><circle cx="16" cy="16" r="10" fill="none" stroke="#34d399" stroke-width="2.4"/><path d="M6.5 18.5c4.5 3.4 14.5 3.4 19-1.7" fill="none" stroke="#34d399" stroke-width="2" stroke-linecap="round" opacity=".55"/><circle cx="16" cy="16" r="2.8" fill="#34d399"/></svg>';
    function brand(href) {
        return '<a class="brand" href="' + href + '" aria-label="ExamSphere home">' + LOGO + "<span>Exam<span>Sphere</span></span></a>";
    }

    // ------------------------------------------------------------------ toast
    function toast(message, type) {
        const root = document.getElementById("toast-root");
        const kind = type || "success";
        const node = document.createElement("div");
        node.className = "toast " + kind;
        node.setAttribute("role", kind === "error" ? "alert" : "status");
        node.innerHTML = icon(kind === "error" ? "alert" : kind === "info" ? "info" : "check")
            + "<span>" + esc(message) + '</span><button type="button" aria-label="Dismiss">' + icon("x").replace("<svg", '<svg width="14" height="14"') + "</button>";
        const remove = function () {
            node.classList.add("leaving");
            setTimeout(function () { node.remove(); }, 220);
        };
        node.querySelector("button").addEventListener("click", remove);
        root.appendChild(node);
        setTimeout(remove, kind === "error" ? 6500 : 4200);
    }

    // ------------------------------------------------------------------ modal
    let activeModal = null;

    function closeModal(result) {
        if (!activeModal) return;
        const modal = activeModal;
        activeModal = null;
        document.removeEventListener("keydown", modal.onKey);
        modal.node.remove();
        if (modal.lastFocus && document.contains(modal.lastFocus)) modal.lastFocus.focus();
        modal.resolve(result);
    }

    /**
     * Opens a dialog. Returns a promise that resolves with the value passed to closeModal
     * (undefined when dismissed with Escape, the X button or a click on the backdrop).
     */
    function openModal(options) {
        closeModal();
        const root = document.getElementById("modal-root");
        const node = document.createElement("div");
        node.className = "modal-backdrop";
        node.innerHTML = '<div class="modal ' + (options.wide ? "wide" : "") + '" role="dialog" aria-modal="true" aria-labelledby="modal-title">'
            + '<div class="modal-head"><h2 id="modal-title">' + esc(options.title) + "</h2>"
            + '<button type="button" class="btn btn-ghost btn-icon btn-sm" data-close aria-label="Close">' + icon("x") + "</button></div>"
            + '<div class="modal-body">' + (options.body || "") + "</div>"
            + (options.footer ? '<div class="modal-foot">' + options.footer + "</div>" : "") + "</div>";
        root.appendChild(node);
        return new Promise(function (resolve) {
            const onKey = function (event) {
                if (event.key === "Escape") closeModal();
                if (event.key === "Tab") {
                    const focusable = node.querySelectorAll("button, [href], input, select, textarea");
                    if (!focusable.length) return;
                    const first = focusable[0], last = focusable[focusable.length - 1];
                    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
                    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
                }
            };
            activeModal = { node: node, resolve: resolve, onKey: onKey, lastFocus: document.activeElement };
            document.addEventListener("keydown", onKey);
            node.addEventListener("mousedown", function (event) { if (event.target === node) closeModal(); });
            node.querySelector("[data-close]").addEventListener("click", function () { closeModal(); });
            node.querySelectorAll("[data-result]").forEach(function (button) {
                button.addEventListener("click", function () { closeModal(button.getAttribute("data-result")); });
            });
            const autofocus = node.querySelector("[autofocus]") || node.querySelector(".modal-foot .btn-primary, .modal-foot .btn-danger") || node.querySelector("[data-close]");
            if (autofocus) autofocus.focus();
            if (options.onOpen) options.onOpen(node);
        });
    }

    /** Confirmation dialog; resolves to true only when the confirm button is pressed. */
    function confirmDialog(options) {
        return openModal({
            title: options.title,
            body: "<p>" + esc(options.message) + "</p>" + (options.extra || ""),
            footer: '<button type="button" class="btn" data-result="cancel">' + esc(options.cancelText || "Cancel") + "</button>"
                + '<button type="button" class="btn ' + (options.danger ? "btn-danger" : "btn-primary") + '" data-result="ok">' + esc(options.confirmText || "Confirm") + "</button>"
        }).then(function (result) { return result === "ok"; });
    }

    // ------------------------------------------------------------------ state blocks
    function loading(text) {
        return '<div class="state" role="status"><span class="spinner spinner-lg"></span><p>' + esc(text || "Loading...") + "</p></div>";
    }
    function skeleton(text) {
        return '<div class="sr-only" role="status">' + esc(text || "Loading...") + '</div><div class="skeleton" style="height:34px;width:min(320px,70%)"></div>'
            + '<div class="skeleton-grid">' + '<div class="skeleton" style="height:96px"></div>'.repeat(4) + "</div>"
            + '<div class="skeleton" style="height:260px"></div>';
    }
    function emptyState(title, text, actionHtml, iconName) {
        return '<div class="state"><div class="state-icon">' + icon(iconName || "inbox") + "</div><h3>" + esc(title) + "</h3>"
            + (text ? "<p>" + esc(text) + "</p>" : "") + (actionHtml || "") + "</div>";
    }
    function errorState(message, retry) {
        return '<div class="state error" role="alert"><div class="state-icon">' + icon("alert") + "</div><h3>Could not load this page</h3><p>" + esc(message) + "</p>"
            + (retry ? '<button type="button" class="btn" data-retry>Try again</button>' : "") + "</div>";
    }

    // ------------------------------------------------------------------ small components
    const BADGE_TONE = {
        PASSED: "success", FAILED: "danger", PUBLISHED: "success", DRAFT: "warning", CLOSED: "", OPEN: "success", UPCOMING: "info",
        APPROVED: "success", PENDING: "warning", REJECTED: "danger", NOT_REQUIRED: "", ACTIVE: "success", INACTIVE: "danger",
        VERIFIED: "success", UNVERIFIED: "warning", IN_PROGRESS: "info", SUBMITTED: "success", COMPLETED: "success", NOT_STARTED: "",
        STUDENT: "info", INSTRUCTOR: "success", ADMIN: "warning", CORRECT: "success", INCORRECT: "danger", UNANSWERED: ""
    };
    function badge(value, text) {
        return '<span class="badge ' + (BADGE_TONE[value] || "") + '">' + esc(text || fmt.label(value)) + "</span>";
    }
    function passBadge(result) {
        if (result.status !== "SUBMITTED") return badge("IN_PROGRESS");
        return badge(result.passed ? "PASSED" : "FAILED");
    }
    function avatar(user, large) {
        const inner = user && user.profilePhoto && /^data:image\//.test(user.profilePhoto)
            ? '<img src="' + esc(user.profilePhoto) + '" alt="">' : esc(fmt.initials(user && user.fullName));
        return '<span class="avatar ' + (large ? "lg" : "") + '">' + inner + "</span>";
    }
    function stat(label, value, iconName, note, accent) {
        return '<div class="stat ' + (accent ? "accent" : "") + '"><span class="stat-label">' + icon(iconName) + esc(label) + '</span><span class="stat-value">' + esc(value) + "</span>"
            + (note ? '<span class="stat-note">' + esc(note) + "</span>" : "") + "</div>";
    }
    function meter(percent) {
        const p = Math.max(0, Math.min(100, Number(percent) || 0));
        return '<div class="meter ' + (p < 40 ? "low" : p < 70 ? "mid" : "") + '" role="img" aria-label="' + fmt.pct(p) + '"><span style="width:' + p + '%"></span></div>';
    }
    function pagination(page, onChange) {
        if (!page || page.totalElements === 0) return "";
        const from = page.page * page.size + 1;
        const to = Math.min(page.totalElements, (page.page + 1) * page.size);
        const id = "pg-" + Math.random().toString(36).slice(2, 8);
        setTimeout(function () {
            const node = document.getElementById(id);
            if (!node) return;
            node.querySelectorAll("button[data-page]").forEach(function (button) {
                button.addEventListener("click", function () { onChange(Number(button.getAttribute("data-page"))); });
            });
        }, 0);
        return '<div class="pagination" id="' + id + '"><span>Showing ' + from + "-" + to + " of " + page.totalElements + "</span>"
            + '<div class="btn-row"><button type="button" class="btn btn-sm" data-page="' + (page.page - 1) + '"' + (page.page <= 0 ? " disabled" : "") + ">Previous</button>"
            + '<span class="num">Page ' + (page.page + 1) + " of " + Math.max(1, page.totalPages) + "</span>"
            + '<button type="button" class="btn btn-sm" data-page="' + (page.page + 1) + '"' + (page.page + 1 >= page.totalPages ? " disabled" : "") + ">Next</button></div></div>";
    }

    // ------------------------------------------------------------------ forms
    /**
     * Renders a labelled input. options: { name, label, type, value, required, placeholder, hint, attrs, options: [[value, text]] }
     */
    function field(o) {
        const id = "f-" + o.name + "-" + Math.random().toString(36).slice(2, 7);
        const required = o.required ? " required" : "";
        const attrs = o.attrs ? " " + o.attrs : "";
        let control;
        if (o.type === "select") {
            control = '<select class="select" id="' + id + '" name="' + o.name + '"' + required + attrs + ">"
                + o.options.map(function (opt) {
                    return '<option value="' + esc(opt[0]) + '"' + (String(opt[0]) === String(o.value === undefined || o.value === null ? "" : o.value) ? " selected" : "") + ">" + esc(opt[1]) + "</option>";
                }).join("") + "</select>";
        } else if (o.type === "textarea") {
            control = '<textarea class="textarea" id="' + id + '" name="' + o.name + '" placeholder="' + esc(o.placeholder || "") + '"' + required + attrs + ">" + esc(o.value || "") + "</textarea>";
        } else if (o.type === "password") {
            control = '<div class="input-wrap"><input class="input" id="' + id + '" name="' + o.name + '" type="password" placeholder="' + esc(o.placeholder || "") + '"' + required + attrs + ">"
                + '<button type="button" class="toggle-pass" aria-label="Show password" aria-pressed="false">' + icon("eye") + "</button></div>";
        } else {
            control = '<input class="input" id="' + id + '" name="' + o.name + '" type="' + (o.type || "text") + '" value="' + esc(o.value === undefined || o.value === null ? "" : o.value) + '" placeholder="' + esc(o.placeholder || "") + '"' + required + attrs + ">";
        }
        return '<div class="field ' + (o.className || "") + '" data-field="' + o.name + '"><label for="' + id + '">' + esc(o.label) + (o.required ? '<span class="req" aria-hidden="true">*</span>' : "") + "</label>"
            + control + (o.hint ? '<span class="field-hint">' + esc(o.hint) + "</span>" : "") + '<span class="field-error" role="alert"></span></div>';
    }

    /** Wires the show/hide password buttons inside a container. */
    function wirePasswordToggles(container) {
        container.querySelectorAll(".toggle-pass").forEach(function (button) {
            button.addEventListener("click", function () {
                const input = button.parentElement.querySelector("input");
                const show = input.type === "password";
                input.type = show ? "text" : "password";
                button.innerHTML = icon(show ? "eyeOff" : "eye");
                button.setAttribute("aria-label", show ? "Hide password" : "Show password");
                button.setAttribute("aria-pressed", String(show));
            });
        });
    }

    function formValues(form) {
        const values = {};
        Array.prototype.forEach.call(form.elements, function (el) {
            if (!el.name) return;
            if (el.type === "checkbox") values[el.name] = el.checked;
            else if (el.type === "radio") { if (el.checked) values[el.name] = el.value; }
            else values[el.name] = typeof el.value === "string" ? el.value.trim() : el.value;
        });
        return values;
    }

    function clearErrors(form) {
        form.querySelectorAll(".field.has-error").forEach(function (f) { f.classList.remove("has-error"); });
        form.querySelectorAll(".field-error").forEach(function (e) { e.textContent = ""; });
        const alert = form.querySelector(".form-alert");
        if (alert) { alert.textContent = ""; alert.className = "form-alert"; }
    }

    /** Shows backend (or client) validation errors next to their fields. Returns true if any field was marked. */
    function showErrors(form, errors) {
        let shown = false, first = null;
        Object.keys(errors || {}).forEach(function (name) {
            const wrap = form.querySelector('[data-field="' + name + '"]');
            if (!wrap) return;
            wrap.classList.add("has-error");
            const slot = wrap.querySelector(".field-error");
            if (slot) slot.textContent = errors[name];
            if (!first) first = wrap.querySelector("input, select, textarea");
            shown = true;
        });
        if (first) first.focus();
        return shown;
    }

    function formAlert(form, message, type) {
        const alert = form.querySelector(".form-alert");
        if (!alert) return toast(message, type === "success" ? "success" : "error");
        alert.className = "form-alert " + (type || "error");
        alert.textContent = message;
    }

    /** Handles an ApiError for a form: field errors inline, everything else in the form alert. */
    function handleFormError(form, error) {
        const marked = showErrors(form, error.errors);
        if (!marked || error.code !== "VALIDATION_FAILED") formAlert(form, error.message || "Something went wrong.");
        else formAlert(form, "Please fix the highlighted fields.");
    }

    /** Disables a button and shows a spinner + busy text while a request is pending (prevents double clicks). */
    function setBusy(button, busy, busyText) {
        if (!button) return;
        if (busy) {
            button.dataset.label = button.innerHTML;
            button.disabled = true;
            button.classList.add("is-busy");
            button.innerHTML = '<span class="spinner"></span>' + esc(busyText || "Working...");
        } else {
            button.disabled = false;
            button.classList.remove("is-busy");
            if (button.dataset.label !== undefined) button.innerHTML = button.dataset.label;
        }
    }

    /** Standard submit wiring: validation reset, busy state, error display. handler(values) returns a promise. */
    function onSubmit(form, busyText, handler) {
        form.setAttribute("novalidate", "");
        form.addEventListener("submit", async function (event) {
            event.preventDefault();
            const button = form.querySelector('button[type="submit"]') || (form.id ? document.querySelector('button[form="' + form.id + '"]') : null);
            if (button && button.disabled) return;
            clearErrors(form);
            setBusy(button, true, busyText);
            try {
                await handler(formValues(form), form);
            } catch (error) {
                if (error.code !== "SESSION_EXPIRED") handleFormError(form, error);
            } finally {
                if (button && document.contains(button)) setBusy(button, false);
            }
        });
    }

    /** Client-side checks that mirror the backend rules; returns { field: message }. */
    function validate(values, rules) {
        const errors = {};
        Object.keys(rules).forEach(function (name) {
            const value = values[name];
            for (const rule of rules[name]) {
                const message = rule(value, values);
                if (message) { errors[name] = message; break; }
            }
        });
        return errors;
    }
    const rule = {
        required: function (label) { return function (v) { return v === undefined || v === null || v === "" || v === false ? label + " is required" : null; }; },
        email: function (v) { return v && !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v) ? "Enter a valid email address" : null; },
        phone: function (v) { return v && !/^\+?[0-9][0-9\s-]{6,17}$/.test(v) ? "Enter a valid phone number" : null; },
        password: function (v) { return v && (v.length < 8 || v.length > 72 || !/[A-Za-z]/.test(v) || !/[0-9]/.test(v)) ? "Use 8 or more characters with at least one letter and one number" : null; },
        matches: function (other, label) { return function (v, all) { return v !== all[other] ? label : null; }; },
        min: function (min, label) { return function (v) { return v !== "" && Number(v) < min ? label : null; }; }
    };

    /** Reads an image file, resizes it to at most 256px and returns a JPEG data URL (keeps uploads tiny). */
    function readPhoto(file) {
        return new Promise(function (resolve, reject) {
            if (!file) return resolve(null);
            if (!/^image\/(png|jpeg|webp)$/.test(file.type)) return reject(new Error("Choose a PNG, JPEG or WebP image."));
            if (file.size > 5 * 1024 * 1024) return reject(new Error("The image must be smaller than 5 MB."));
            const reader = new FileReader();
            reader.onerror = function () { reject(new Error("The image could not be read.")); };
            reader.onload = function () {
                const img = new Image();
                img.onerror = function () { reject(new Error("The image could not be read.")); };
                img.onload = function () {
                    const size = 256, canvas = document.createElement("canvas");
                    const side = Math.min(img.width, img.height);
                    canvas.width = size; canvas.height = size;
                    canvas.getContext("2d").drawImage(img, (img.width - side) / 2, (img.height - side) / 2, side, side, 0, 0, size, size);
                    resolve(canvas.toDataURL("image/jpeg", 0.85));
                };
                img.src = reader.result;
            };
            reader.readAsDataURL(file);
        });
    }

    // ------------------------------------------------------------------ charts (all values come from the API)
    const SERIES = ["var(--accent)", "var(--info)", "var(--warning)", "var(--violet)", "var(--danger)"];

    /** Horizontal bars. points: [{ label, value }]. */
    function barChart(points, options) {
        const o = options || {};
        if (!points || !points.length) return emptyState(o.emptyTitle || "No data yet", o.emptyText || "", "", "chart");
        const max = o.max || Math.max.apply(null, points.map(function (p) { return p.value; }).concat([1]));
        return '<div class="bars" role="img" aria-label="' + esc(o.label || "Bar chart") + '">' + points.map(function (p, i) {
            const width = Math.max(0, Math.min(100, (p.value / max) * 100));
            return '<div class="bar-row"><span class="bar-label" title="' + esc(p.label) + '">' + esc(p.label) + '</span><div class="bar-track"><div class="bar-fill ' + (p.tone || "") + '" style="width:' + width + "%;animation-delay:" + (i * 60) + 'ms"></div></div><span class="bar-value">'
                + esc(o.format ? o.format(p.value) : fmt.num(p.value)) + "</span></div>";
        }).join("") + "</div>";
    }

    /** Vertical columns, e.g. submissions per day. */
    function columnChart(points, options) {
        const o = options || {};
        if (!points || !points.length) return emptyState("No data yet", "", "", "chart");
        const max = Math.max.apply(null, points.map(function (p) { return p.value; }).concat([1]));
        const every = Math.ceil(points.length / 7);
        return '<div class="columns" role="img" aria-label="' + esc(o.label || "Column chart") + '">' + points.map(function (p, i) {
            return '<div class="column" title="' + esc(p.label + ": " + p.value) + '"><span class="col-val">' + (p.value ? esc(fmt.num(p.value)) : "") + '</span><div class="col-bar ' + (p.value ? "" : "zero") + '" style="height:'
                + Math.max(1.5, (p.value / max) * 100) + "%;animation-delay:" + (i * 30) + 'ms"></div><span class="col-label">' + (i % every === 0 || points.length <= 7 ? esc(p.label) : "&nbsp;") + "</span></div>";
        }).join("") + "</div>";
    }

    /** Donut with legend. segments: [{ label, value }]. */
    function donutChart(segments, options) {
        const o = options || {};
        const total = segments.reduce(function (sum, s) { return sum + s.value; }, 0);
        if (!total) return emptyState(o.emptyTitle || "No data yet", o.emptyText || "", "", "chart");
        const r = 52, c = 2 * Math.PI * r;
        let offset = 0;
        const arcs = segments.map(function (s, i) {
            const length = (s.value / total) * c;
            const arc = '<circle class="donut-seg" cx="66" cy="66" r="' + r + '" stroke="' + (s.color || SERIES[i % SERIES.length]) + '" stroke-dasharray="' + length + " " + (c - length) + '" stroke-dashoffset="' + (-offset) + '" transform="rotate(-90 66 66)"/>';
            offset += length;
            return arc;
        }).join("");
        return '<div class="donut-wrap"><svg class="donut" viewBox="0 0 132 132" role="img" aria-label="' + esc(o.label || "Donut chart") + '"><circle cx="66" cy="66" r="' + r + '" stroke="rgba(255,255,255,.06)"/>' + arcs
            + '<text x="66" y="64" text-anchor="middle" font-size="22">' + esc(o.center !== undefined ? o.center : total) + '</text><text x="66" y="82" text-anchor="middle" font-size="10" style="fill:var(--muted);font-family:var(--font-body);font-weight:400">' + esc(o.centerLabel || "total") + "</text></svg>"
            + '<ul class="legend">' + segments.map(function (s, i) {
                return '<li><span class="swatch" style="background:' + (s.color || SERIES[i % SERIES.length]) + '"></span>' + esc(s.label) + "<b>" + esc(fmt.num(s.value)) + "</b></li>";
            }).join("") + "</ul></div>";
    }

    /** Line chart on a 0-100 scale (score trend). */
    function lineChart(points, options) {
        const o = options || {};
        if (!points || !points.length) return emptyState(o.emptyTitle || "No data yet", o.emptyText || "", "", "chart");
        const W = 600, H = 190, L = 34, R = 14, T = 14, B = 26;
        const max = o.max || 100;
        const x = function (i) { return points.length === 1 ? (L + W - R) / 2 : L + (i * (W - L - R)) / (points.length - 1); };
        const y = function (v) { return T + (1 - Math.min(v, max) / max) * (H - T - B); };
        const coords = points.map(function (p, i) { return x(i).toFixed(1) + "," + y(p.value).toFixed(1); });
        const grid = [0, 25, 50, 75, 100].map(function (g) {
            const v = (g / 100) * max;
            return '<line class="grid-line" x1="' + L + '" x2="' + (W - R) + '" y1="' + y(v) + '" y2="' + y(v) + '"/><text class="axis-text" x="' + (L - 6) + '" y="' + (y(v) + 3) + '" text-anchor="end">' + Math.round(v) + "</text>";
        }).join("");
        const area = points.length > 1 ? '<polygon class="area" points="' + L + "," + (H - B) + " " + coords.join(" ") + " " + (W - R) + "," + (H - B) + '"/>' : "";
        const line = points.length > 1 ? '<polyline class="series" pathLength="1" points="' + coords.join(" ") + '"/>' : "";
        const dots = points.map(function (p, i) {
            return '<circle class="dot" cx="' + x(i).toFixed(1) + '" cy="' + y(p.value).toFixed(1) + '" r="4"><title>' + esc(p.label + ": " + fmt.num(p.value) + (o.suffix || "")) + "</title></circle>";
        }).join("");
        return '<svg class="line-chart" viewBox="0 0 ' + W + " " + H + '" preserveAspectRatio="none" role="img" aria-label="' + esc(o.label || "Line chart") + '"><defs><linearGradient id="line-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stop-color="rgba(52,211,153,.28)"/><stop offset="1" stop-color="rgba(52,211,153,0)"/></linearGradient></defs>'
            + grid + area + line + dots + "</svg>";
    }

    /** Circular score indicator for the result page. */
    function ring(percent, big, small, failed) {
        const r = 84, c = 2 * Math.PI * r;
        const p = Math.max(0, Math.min(100, Number(percent) || 0));
        const id = "ring-" + Math.random().toString(36).slice(2, 8);
        setTimeout(function () {
            const node = document.getElementById(id);
            if (node) node.style.strokeDashoffset = String(c * (1 - p / 100));
        }, 60);
        return '<div class="ring ' + (failed ? "fail" : "") + '" role="img" aria-label="Score ' + fmt.pct(p) + '"><svg viewBox="0 0 190 190"><circle class="ring-track" cx="95" cy="95" r="' + r + '"/><circle class="ring-value" id="' + id + '" cx="95" cy="95" r="' + r + '" stroke-dasharray="' + c + '" stroke-dashoffset="' + c + '"/></svg>'
            + '<div class="ring-center"><strong>' + esc(big) + "</strong><span>" + esc(small) + "</span></div></div>";
    }

    /** Ticks a mm:ss countdown inside an element; calls done() at zero. Returns a stop function. */
    function countdown(seconds, onTick, done) {
        const end = Date.now() + seconds * 1000;
        let stopped = false;
        const tick = function () {
            if (stopped) return;
            const left = Math.max(0, Math.ceil((end - Date.now()) / 1000));
            onTick(left);
            if (left <= 0) { stopped = true; clearInterval(timer); if (done) done(); }
        };
        const timer = setInterval(tick, 500);
        tick();
        return function () { stopped = true; clearInterval(timer); };
    }

    ES.ui = {
        icon: icon, brand: brand, toast: toast, openModal: openModal, closeModal: closeModal, confirm: confirmDialog,
        loading: loading, skeleton: skeleton, emptyState: emptyState, errorState: errorState,
        badge: badge, passBadge: passBadge, avatar: avatar, stat: stat, meter: meter, pagination: pagination,
        field: field, wirePasswordToggles: wirePasswordToggles, formValues: formValues, clearErrors: clearErrors, showErrors: showErrors,
        formAlert: formAlert, handleFormError: handleFormError, setBusy: setBusy, onSubmit: onSubmit, validate: validate, rule: rule,
        readPhoto: readPhoto, barChart: barChart, columnChart: columnChart, donutChart: donutChart, lineChart: lineChart, ring: ring,
        countdown: countdown
    };
})(window.ES);
