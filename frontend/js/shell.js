/* Signed-in application shell (sidebar + top bar) and the pages shared by all roles: profile and settings. */
(function (ES) {
    "use strict";
    const ui = ES.ui, esc = ES.esc, api = ES.api, router = ES.router, fmt = ES.fmt, icon = ui.icon;

    const NAV = {
        STUDENT: [
            ["/student/dashboard", "Dashboard", "dashboard"],
            ["/student/exams", "Available Exams", "book"],
            ["/student/attempts", "My Attempts", "clock"],
            ["/student/results", "My Results", "award"],
            ["/student/profile", "Profile", "user"],
            ["/student/settings", "Settings", "settings"]
        ],
        INSTRUCTOR: [
            ["/instructor/dashboard", "Dashboard", "dashboard"],
            ["/instructor/exams", "My Exams", "book"],
            ["/instructor/exams/new", "Create Exam", "plus"],
            ["/instructor/questions", "Question Management", "list"],
            ["/instructor/results", "Results", "award"],
            ["/instructor/analytics", "Analytics", "chart"],
            ["/instructor/profile", "Profile", "user"],
            ["/instructor/settings", "Settings", "settings"]
        ],
        ADMIN: [
            ["/admin/dashboard", "Dashboard", "dashboard"],
            ["/admin/users", "Users", "users"],
            ["/admin/approvals", "Instructor Approvals", "shield"],
            ["/admin/exams", "Exams", "book"],
            ["/admin/results", "Results", "award"],
            ["/admin/reports", "Reports", "chart"],
            ["/admin/activity", "Activity", "activity"],
            ["/admin/settings", "Settings", "settings"]
        ]
    };
    // Where the top-bar search sends its text for each role.
    const SEARCH = {
        STUDENT: ["/student/exams", "Search exams by title or category"],
        INSTRUCTOR: ["/instructor/exams", "Search your exams"],
        ADMIN: ["/admin/users", "Search users by name or email"]
    };

    async function logout() {
        try { await api("/api/account/logout", { method: "POST" }); } catch (e) { /* the session is cleared regardless */ }
        ES.session.clear();
        ES.layout.current = null;
        document.getElementById("app").innerHTML = "";
        sessionStorage.setItem("examsphere.notice", "You have been logged out.");
        router.go("/login");
    }
    ES.logout = logout;

    function buildShell(user) {
        const app = document.getElementById("app");
        const search = SEARCH[user.role];
        app.innerHTML = '<div class="shell" id="shell"><aside class="sidebar" id="sidebar" aria-label="Main navigation">' + ui.brand("#" + router.home(user))
            + '<p class="side-role">' + esc(fmt.label(user.role)) + ' workspace</p><nav class="side-nav" id="side-nav">'
            + NAV[user.role].map(function (item) { return '<a href="#' + item[0] + '" data-path="' + item[0] + '">' + icon(item[2]) + "<span>" + item[1] + "</span></a>"; }).join("")
            + '</nav><div class="side-nav side-bottom"><button type="button" id="logout-btn">' + icon("logout") + "<span>Logout</span></button></div></aside>"
            + '<button type="button" class="scrim" id="scrim" aria-label="Close menu"></button>'
            + '<div class="main-col"><header class="topbar"><button type="button" class="btn btn-ghost btn-icon menu-btn" id="menu-btn" aria-label="Open menu" aria-controls="sidebar">' + icon("menu") + "</button>"
            + '<form class="search-input" id="top-search" role="search">' + icon("search") + '<input class="input" type="search" name="q" placeholder="' + esc(search[1]) + '" aria-label="' + esc(search[1]) + '"></form><span class="spacer"></span>'
            + '<div class="dropdown"><button type="button" class="btn btn-ghost btn-icon bell" id="bell-btn" aria-label="Notifications" aria-haspopup="true" aria-expanded="false">' + icon("bell") + '<span class="dot hidden" id="bell-dot"></span></button><div class="dropdown-menu notif-menu hidden" id="notif-menu"></div></div>'
            + '<div class="dropdown"><button type="button" class="top-user" id="user-btn" aria-haspopup="true" aria-expanded="false"><span id="top-avatar">' + ui.avatar(user) + '</span><span class="who"><b id="top-name">' + esc(user.fullName) + "</b><small>" + esc(fmt.label(user.role)) + "</small></span></button>"
            + '<div class="dropdown-menu hidden" id="user-menu"><a href="#/' + user.role.toLowerCase() + '/profile">' + icon("user") + 'Profile</a><a href="#/' + user.role.toLowerCase() + '/settings">' + icon("settings") + 'Settings</a><hr><button type="button" id="logout-btn-2">' + icon("logout") + "Logout</button></div></div>"
            + '</header><main class="view" id="main"></main></div></div>';

        const shell = app.querySelector("#shell");
        const closeNav = function () { shell.classList.remove("nav-open"); };
        app.querySelector("#menu-btn").addEventListener("click", function () { shell.classList.toggle("nav-open"); });
        app.querySelector("#scrim").addEventListener("click", closeNav);
        app.querySelector("#side-nav").addEventListener("click", closeNav);
        app.querySelector("#logout-btn").addEventListener("click", logout);
        app.querySelector("#logout-btn-2").addEventListener("click", logout);

        app.querySelector("#top-search").addEventListener("submit", function (event) {
            event.preventDefault();
            const q = event.target.elements.q.value.trim();
            router.go(search[0] + (q ? "?q=" + encodeURIComponent(q) : ""));
        });

        // dropdowns
        const menus = [["#user-btn", "#user-menu"], ["#bell-btn", "#notif-menu"]];
        const closeMenus = function (except) {
            menus.forEach(function (pair) {
                if (pair[1] === except) return;
                app.querySelector(pair[1]).classList.add("hidden");
                app.querySelector(pair[0]).setAttribute("aria-expanded", "false");
            });
        };
        menus.forEach(function (pair) {
            app.querySelector(pair[0]).addEventListener("click", function (event) {
                event.stopPropagation();
                const menu = app.querySelector(pair[1]);
                closeMenus(pair[1]);
                const open = menu.classList.toggle("hidden") === false;
                event.currentTarget.setAttribute("aria-expanded", String(open));
                if (open && pair[1] === "#notif-menu") loadNotifications(menu);
            });
        });
        // Document-level listeners are registered once, even if the shell is rebuilt after a new login.
        ES.layout.closeMenus = function () { closeMenus(); };
        ES.layout.closeNav = closeNav;
        if (!ES.layout.globalWired) {
            ES.layout.globalWired = true;
            document.addEventListener("click", function () { if (document.getElementById("shell")) ES.layout.closeMenus(); });
            document.addEventListener("keydown", function (event) {
                if (event.key === "Escape" && document.getElementById("shell")) { ES.layout.closeMenus(); ES.layout.closeNav(); }
            });
        }

        refreshBell();
    }

    function notificationHtml(items) {
        if (!items.length) return '<header>Notifications</header>' + ui.emptyState("You're all caught up", "New activity will show up here.", "", "bell");
        return "<header>Notifications</header><div class=\"notif-list\">" + items.map(function (n) {
            return '<a href="' + esc(n.link || "#") + '"><span class="n-dot ' + esc(n.type) + '"></span><span><b>' + esc(n.title) + "</b><small>" + esc(n.message) + "</small><small>" + esc(fmt.ago(n.time)) + "</small></span></a>";
        }).join("") + "</div>";
    }
    async function loadNotifications(menu) {
        menu.innerHTML = "<header>Notifications</header>" + ui.loading("Loading notifications...");
        try {
            const response = await api("/api/account/notifications");
            menu.innerHTML = notificationHtml(response.data || []);
        } catch (error) {
            menu.innerHTML = "<header>Notifications</header>" + ui.errorState(error.message);
        }
    }
    async function refreshBell() {
        try {
            const response = await api("/api/account/notifications");
            const dot = document.getElementById("bell-dot");
            if (dot) dot.classList.toggle("hidden", !(response.data && response.data.length));
        } catch (e) { /* the bell simply stays quiet */ }
    }

    /** Keeps the top bar in sync after the profile changes. */
    function refreshUserChrome() {
        const user = ES.session.user;
        if (!user) return;
        const name = document.getElementById("top-name"), avatar = document.getElementById("top-avatar");
        if (name) name.textContent = user.fullName;
        if (avatar) avatar.innerHTML = ui.avatar(user);
    }

    Object.assign(ES.layout, {
        current: null,
        /** Prepares the page frame for a route and returns the element the page renders into. */
        mount(route, path) {
            const app = document.getElementById("app");
            if (route.layout !== "app") {
                this.current = null;
                app.innerHTML = "";
                return app;
            }
            const user = ES.session.user;
            const key = "app:" + user.id;
            if (this.current !== key || !document.getElementById("shell")) {
                buildShell(user);
                this.current = key;
            }
            const active = route.nav || path;
            app.querySelectorAll("#side-nav a").forEach(function (link) {
                const on = link.getAttribute("data-path") === active;
                link.classList.toggle("active", on);
                if (on) link.setAttribute("aria-current", "page"); else link.removeAttribute("aria-current");
            });
            document.getElementById("shell").classList.remove("nav-open");
            const searchInput = app.querySelector("#top-search input");
            if (searchInput && path !== SEARCH[user.role][0]) searchInput.value = "";
            const view = document.getElementById("main");
            view.innerHTML = ui.skeleton("Loading " + (route.title || "page") + "...");
            // restart the entrance animation
            view.style.animation = "none"; void view.offsetWidth; view.style.animation = "";
            return view;
        }
    });

    // ------------------------------------------------------------------ profile (all roles)
    ES.pages.profile = async function (ctx) {
        const response = await api("/api/account");
        if (!ctx.alive()) return;
        let user = response.data;
        ES.session.updateUser(user);
        refreshUserChrome();

        const render = function (editing) {
            const student = user.role === "STUDENT", instructor = user.role === "INSTRUCTOR";
            const kv = function (label, value) { return "<div><small>" + label + "</small><b>" + esc(value || "-") + "</b></div>"; };
            const roleInfo = student
                ? kv("College", user.college) + kv("Course", user.course) + kv("Branch", user.branch) + kv("Year / Semester", user.yearSemester) + kv("Student ID", user.studentId)
                : instructor ? kv("Institution", user.institution) + kv("Department", user.department) + kv("Employee / Faculty ID", user.employeeId) + kv("Designation", user.designation) : "";
            const roleFields = student
                ? ui.field({ name: "college", label: "College", value: user.college, required: true }) + ui.field({ name: "course", label: "Course", value: user.course, required: true })
                + ui.field({ name: "branch", label: "Branch", value: user.branch, required: true }) + ui.field({ name: "yearSemester", label: "Year / Semester", value: user.yearSemester, required: true })
                + ui.field({ name: "studentId", label: "Student ID", value: user.studentId, required: true })
                : instructor ? ui.field({ name: "institution", label: "Institution", value: user.institution, required: true }) + ui.field({ name: "department", label: "Department", value: user.department, required: true })
                + ui.field({ name: "employeeId", label: "Employee / Faculty ID", value: user.employeeId, required: true }) + ui.field({ name: "designation", label: "Designation", value: user.designation, required: true }) : "";

            ctx.view.innerHTML = '<div class="page-head"><div><h1>Profile</h1><p>Your account details as other people on ExamSphere see them.</p></div>'
                + (editing ? "" : '<button type="button" class="btn btn-primary" id="edit-btn">' + icon("edit") + "Edit Profile</button>") + "</div>"
                + '<div class="card"><div class="profile-head">' + ui.avatar(user, true) + "<div><h2>" + esc(user.fullName) + '</h2><p class="muted">' + esc(user.email) + '</p><div class="btn-row" style="margin-top:10px">'
                + ui.badge(user.role) + ui.badge(user.emailVerified ? "VERIFIED" : "UNVERIFIED", user.emailVerified ? "Email verified" : "Email not verified")
                + (instructor ? ui.badge(user.approvalStatus) : "") + "</div></div></div></div>"
                + (editing
                    ? '<form class="card stack" id="profile-form"><div class="form-alert" role="alert"></div><div class="form-grid">'
                    + ui.field({ name: "fullName", label: "Full Name", value: user.fullName, required: true })
                    + ui.field({ name: "email", label: "Email", value: user.email, attrs: "readonly", hint: "Email cannot be changed here because it would need to be verified again." })
                    + ui.field({ name: "phone", label: "Phone", type: "tel", value: user.phone, required: true }) + roleFields
                    + '<div class="field span-2" data-field="profilePhoto"><label for="photo-input">Profile photo</label><input class="input" id="photo-input" type="file" accept="image/png,image/jpeg,image/webp">'
                    + (user.profilePhoto ? '<label class="check"><input type="checkbox" name="removePhoto"> Remove current photo</label>' : "") + '<span class="field-error" role="alert"></span></div>'
                    + '</div><div class="btn-row"><button type="submit" class="btn btn-primary">Save Changes</button><button type="button" class="btn" id="cancel-btn">Cancel</button></div></form>'
                    : '<div class="card"><div class="card-head"><h2>Details</h2></div><div class="kv">' + kv("Full Name", user.fullName) + kv("Email", user.email) + kv("Phone", user.phone) + roleInfo + kv("Member since", fmt.date(user.createdAt)) + "</div></div>");

            if (!editing) {
                ctx.view.querySelector("#edit-btn").addEventListener("click", function () { render(true); });
                return;
            }
            const form = ctx.view.querySelector("#profile-form");
            ctx.view.querySelector("#cancel-btn").addEventListener("click", function () { render(false); });
            ui.onSubmit(form, "Saving...", async function (values) {
                const rules = { fullName: [ui.rule.required("Full name")], phone: [ui.rule.required("Phone"), ui.rule.phone] };
                form.querySelectorAll("[required]").forEach(function (el) { if (!rules[el.name]) rules[el.name] = [ui.rule.required("This field")]; });
                const errors = ui.validate(values, rules);
                let photo = null;
                try { photo = await ui.readPhoto(form.querySelector("#photo-input").files[0]); } catch (e) { errors.profilePhoto = e.message; }
                if (Object.keys(errors).length) { ui.showErrors(form, errors); return; }
                delete values.email;
                const saved = await api("/api/account", { method: "PUT", body: Object.assign({}, values, { profilePhoto: photo, removePhoto: !!values.removePhoto }) });
                user = saved.data;
                ES.session.updateUser(user);
                refreshUserChrome();
                ui.toast("Profile updated successfully.");
                render(false);
            });
        };
        render(false);
    };

    // ------------------------------------------------------------------ settings (all roles)
    ES.pages.settings = function (ctx) {
        const data = ES.session.read();
        const reduce = localStorage.getItem("examsphere.reduceMotion") === "1";
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Settings</h1><p>Password, display preferences and this session.</p></div></div>'
            + '<div class="grid-2"><form class="card stack" id="password-form"><div class="card-head"><div><h2>Change password</h2><p>You stay logged in on this device after changing it.</p></div></div><div class="form-alert" role="alert"></div>'
            + ui.field({ name: "currentPassword", label: "Current Password", type: "password", required: true, attrs: 'autocomplete="current-password"' })
            + ui.field({ name: "newPassword", label: "New Password", type: "password", required: true, hint: "At least 8 characters, with a letter and a number.", attrs: 'autocomplete="new-password"' })
            + ui.field({ name: "confirmPassword", label: "Confirm New Password", type: "password", required: true, attrs: 'autocomplete="new-password"' })
            + '<div><button type="submit" class="btn btn-primary">Update password</button></div></form>'
            + '<div class="stack"><div class="card stack"><div class="card-head"><div><h2>Display</h2><p>Saved in this browser only.</p></div></div>'
            + '<label class="check"><input type="checkbox" id="reduce-motion"' + (reduce ? " checked" : "") + "> <span>Reduce motion<br><span class=\"muted small\">Turns off page, chart and card animations.</span></span></label></div>"
            + '<div class="card stack"><div class="card-head"><div><h2>Session</h2><p>Signed in as ' + esc(ctx.user.email) + '</p></div></div><div class="kv"><div><small>Session expires</small><b>' + esc(fmt.dateTime(data && data.expiresAt)) + "</b></div><div><small>Kept after closing the browser</small><b>" + (data && data.remember ? "Yes" : "No") + '</b></div></div>'
            + '<div class="btn-row"><a class="btn" href="#/' + ctx.user.role.toLowerCase() + '/profile">' + icon("user") + 'Open profile</a><button type="button" class="btn btn-danger" id="settings-logout">' + icon("logout") + "Logout</button></div></div></div></div>";

        const form = ctx.view.querySelector("#password-form");
        ui.wirePasswordToggles(form);
        ui.onSubmit(form, "Updating...", async function (values) {
            const errors = ui.validate(values, {
                currentPassword: [ui.rule.required("Current password")],
                newPassword: [ui.rule.required("New password"), ui.rule.password],
                confirmPassword: [ui.rule.required("Confirm password"), ui.rule.matches("newPassword", "Passwords do not match")]
            });
            if (Object.keys(errors).length) { ui.showErrors(form, errors); return; }
            const response = await api("/api/account/password", { method: "POST", body: values });
            form.reset();
            ui.formAlert(form, response.message, "success");
            ui.toast(response.message);
        });
        ctx.view.querySelector("#reduce-motion").addEventListener("change", function (event) {
            localStorage.setItem("examsphere.reduceMotion", event.target.checked ? "1" : "0");
            document.documentElement.classList.toggle("reduce-motion", event.target.checked);
        });
        ctx.view.querySelector("#settings-logout").addEventListener("click", logout);
    };
})(window.ES);
