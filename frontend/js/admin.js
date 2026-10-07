/* Admin pages: dashboard, users, instructor approvals, exams, results, reports and activity. */
(function (ES) {
    "use strict";
    const ui = ES.ui, esc = ES.esc, api = ES.api, fmt = ES.fmt, icon = ui.icon;

    const ACTION_ICON = { LOGIN: "user", LOGOUT: "logout", REGISTRATION: "plus", EMAIL_VERIFIED: "mail", EXAM_CREATED: "file", EXAM_PUBLISHED: "send", EXAM_STARTED: "clock", EXAM_SUBMITTED: "check", INSTRUCTOR_APPROVED: "shield", INSTRUCTOR_REJECTED: "x" };
    function activityList(items) {
        return '<ul class="activity">' + items.map(function (a) {
            return '<li><span class="a-ic">' + icon(ACTION_ICON[a.action] || "activity") + "</span><span><b>" + esc(fmt.label(a.action)) + "</b><small>" + esc(a.description) + (a.userEmail ? " &middot; " + esc(a.userEmail) : "") + '</small></span><time datetime="' + esc(a.createdAt) + '" title="' + esc(fmt.dateTime(a.createdAt)) + '">' + esc(fmt.ago(a.createdAt)) + "</time></li>";
        }).join("") + "</ul>";
    }
    function retryable(ctx, node, error, retry) {
        if (!ctx.alive() || error.code === "SESSION_EXPIRED") return;
        node.innerHTML = ui.errorState(error.message, true);
        node.querySelector("[data-retry]").addEventListener("click", retry);
    }

    // ------------------------------------------------------------------ dashboard
    ES.pages.adminDashboard = async function (ctx) {
        const d = (await api("/api/admin/dashboard")).data;
        if (!ctx.alive()) return;
        const has = d.totalAttempts > 0;
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Admin Dashboard</h1><p>Platform totals, straight from the database.</p></div>'
            + (d.pendingInstructors ? '<a class="btn btn-primary" href="#/admin/approvals">' + icon("shield") + "Review " + d.pendingInstructors + " pending instructor" + (d.pendingInstructors === 1 ? "" : "s") + "</a>" : "") + "</div>"
            + '<div class="stat-grid">' + ui.stat("Total Students", d.totalStudents, "cap", null, true) + ui.stat("Total Instructors", d.totalInstructors, "users") + ui.stat("Pending Instructors", d.pendingInstructors, "shield")
            + ui.stat("Total Exams", d.totalExams, "book") + ui.stat("Published Exams", d.publishedExams, "send") + ui.stat("Total Attempts", d.totalAttempts, "edit") + ui.stat("Pass Rate", has ? fmt.pct(d.passRate) : "-", "award") + "</div>"
            + '<div class="grid-3"><div class="card"><div class="card-head"><div><h2>User distribution</h2></div></div>' + ui.donutChart(d.userDistribution, { centerLabel: "users", label: "User distribution" }) + "</div>"
            + '<div class="card"><div class="card-head"><div><h2>Exam activity</h2><p>Exams by status</p></div></div>' + (d.totalExams ? ui.barChart([{ label: "Published", value: d.publishedExams }, { label: "Draft", value: d.draftExams, tone: "warning" }, { label: "Closed", value: d.closedExams, tone: "info" }], { label: "Exams by status" }) : ui.emptyState("No exams yet", "", "", "book")) + "</div>"
            + '<div class="card"><div class="card-head"><div><h2>Pass / fail overview</h2></div></div>' + ui.donutChart([{ label: "Passed", value: d.passCount, color: "var(--accent)" }, { label: "Failed", value: d.failCount, color: "var(--danger)" }], { centerLabel: "attempts", emptyTitle: "No attempts yet", emptyText: "This fills in when students submit exams." }) + "</div></div>"
            + '<div class="grid-main"><div class="card"><div class="card-head"><div><h2>Attempt trend</h2><p>Submissions per day, last 14 days</p></div></div>' + (has ? ui.columnChart(d.attemptTrend, { label: "Submissions per day" }) : ui.emptyState("No attempts yet", "Daily submissions will be charted here.", "", "chart")) + "</div>"
            + '<div class="card"><div class="card-head"><div><h2>Recent activity</h2></div><a class="btn btn-sm" href="#/admin/activity">View all</a></div>' + (d.recentActivity.length ? activityList(d.recentActivity) : ui.emptyState("No activity yet", "", "", "activity")) + "</div></div>";
    };

    // ------------------------------------------------------------------ users
    function userModal(u) {
        const kv = function (label, value) { return "<div><small>" + label + "</small><b>" + esc(value || "-") + "</b></div>"; };
        return ui.openModal({
            title: u.fullName, wide: true,
            body: '<div class="btn-row">' + ui.badge(u.role) + ui.badge(u.active ? "ACTIVE" : "INACTIVE") + ui.badge(u.emailVerified ? "VERIFIED" : "UNVERIFIED") + (u.role === "INSTRUCTOR" ? ui.badge(u.approvalStatus) : "") + (u.demoAccount ? '<span class="badge plain">Demo account</span>' : "") + "</div>"
                + '<div class="kv">' + kv("Email", u.email) + kv("Phone", u.phone) + kv(u.role === "STUDENT" ? "College" : "Institution", u.organisation) + kv(u.role === "STUDENT" ? "Branch" : "Department", u.department)
                + (u.role === "STUDENT" ? kv("Course", u.course) + kv("Year / Semester", u.yearSemester) + kv("Student ID", u.idNumber) : u.role === "INSTRUCTOR" ? kv("Faculty ID", u.idNumber) + kv("Designation", u.designation) : "")
                + kv("Registered", fmt.dateTime(u.createdAt)) + kv("Email verified", u.emailVerifiedAt ? fmt.dateTime(u.emailVerifiedAt) : "Not yet") + "</div>",
            footer: '<button type="button" class="btn btn-primary" data-result="close">Close</button>'
        });
    }

    ES.pages.adminUsers = async function (ctx) {
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Users</h1><p>Everyone registered on the platform. Deactivated users cannot log in.</p></div></div>'
            + '<form class="toolbar" id="filters" role="search"><div class="field grow"><label for="q">Search</label><div class="search-input">' + icon("search") + '<input class="input" id="q" name="q" type="search" placeholder="Name or email" value="' + esc(ctx.query.q || "") + '"></div></div>'
            + ui.field({ name: "role", label: "Role", type: "select", options: [["", "All roles"], ["STUDENT", "Student"], ["INSTRUCTOR", "Instructor"], ["ADMIN", "Admin"]] })
            + ui.field({ name: "active", label: "Status", type: "select", options: [["", "Any status"], ["true", "Active"], ["false", "Inactive"]] })
            + ui.field({ name: "verified", label: "Verification", type: "select", options: [["", "Any"], ["true", "Verified"], ["false", "Not verified"]] })
            + ui.field({ name: "approval", label: "Approval", type: "select", options: [["", "Any"], ["PENDING", "Pending"], ["APPROVED", "Approved"], ["REJECTED", "Rejected"]] })
            + '<button type="button" class="btn" id="clear-btn">Clear</button></form><div class="card flush" id="list"></div>';
        const form = ctx.view.querySelector("#filters"), list = ctx.view.querySelector("#list");
        let users = [], page = 0;
        const load = async function (p) {
            page = p || 0;
            list.innerHTML = ui.loading("Loading users...");
            try {
                const data = (await api("/api/admin/users", { query: Object.assign(ui.formValues(form), { page: page, size: 10 }) })).data;
                if (!ctx.alive()) return;
                users = data.content;
                const filtered = Object.values(ui.formValues(form)).some(Boolean);
                list.innerHTML = users.length ? '<div class="table-wrap"><table class="table"><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Verification</th><th>Approval</th><th>Status</th><th>Created Date</th><th class="right">Actions</th></tr></thead><tbody>'
                    + users.map(function (u, i) {
                        const self = u.id === ctx.user.id;
                        return '<tr><td><div class="cell-title">' + esc(u.fullName) + (self ? ' <span class="muted small">(you)</span>' : "") + "</div>" + (u.demoAccount ? '<div class="cell-sub">Demo account</div>' : "") + "</td><td>" + esc(u.email) + "</td><td>" + ui.badge(u.role) + "</td><td>" + ui.badge(u.emailVerified ? "VERIFIED" : "UNVERIFIED")
                            + "</td><td>" + (u.role === "INSTRUCTOR" ? ui.badge(u.approvalStatus) : '<span class="muted">-</span>') + "</td><td>" + ui.badge(u.active ? "ACTIVE" : "INACTIVE") + '</td><td class="nowrap">' + esc(fmt.date(u.createdAt)) + '</td><td><div class="actions"><button type="button" class="btn btn-sm" data-view="' + i + '">View</button>'
                            + (self ? "" : u.active ? '<button type="button" class="btn btn-sm btn-danger" data-toggle="' + i + '">Deactivate</button>' : '<button type="button" class="btn btn-sm btn-primary" data-toggle="' + i + '">Activate</button>') + "</div></td></tr>";
                    }).join("") + "</tbody></table></div>" + ui.pagination(data, load)
                    : filtered ? ui.emptyState("No users match these filters", "Change or clear the filters to see more.", "", "search") : ui.emptyState("No users yet", "Registered users will be listed here.", "", "users");
            } catch (error) { retryable(ctx, list, error, function () { load(page); }); }
        };
        list.addEventListener("click", async function (event) {
            const view = event.target.closest("[data-view]"), toggle = event.target.closest("[data-toggle]");
            if (view) return userModal(users[Number(view.getAttribute("data-view"))]);
            if (!toggle) return;
            const u = users[Number(toggle.getAttribute("data-toggle"))], activate = !u.active;
            const ok = await ui.confirm({ title: (activate ? "Activate " : "Deactivate ") + u.fullName + "?", message: activate ? "This user will be able to log in again." : "This user will be logged out and will not be able to log in until reactivated.", confirmText: activate ? "Activate" : "Deactivate", danger: !activate });
            if (!ok) return;
            try {
                const response = await api("/api/admin/users/" + u.id + "/status", { method: "PATCH", body: { active: activate } });
                ui.toast(response.message);
                load(page);
            } catch (error) { if (error.code !== "SESSION_EXPIRED") ui.toast(error.message, "error"); }
        });
        form.addEventListener("submit", function (e) { e.preventDefault(); load(0); });
        form.querySelector("#q").addEventListener("input", ES.debounce(function () { load(0); }, 300));
        form.querySelectorAll("select").forEach(function (s) { s.addEventListener("change", function () { load(0); }); });
        ctx.view.querySelector("#clear-btn").addEventListener("click", function () { form.reset(); form.querySelector("#q").value = ""; load(0); });
        await load(0);
    };

    // ------------------------------------------------------------------ instructor approvals
    ES.pages.adminApprovals = async function (ctx) {
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Instructor Approvals</h1><p>Instructors can log in only after their email is verified and you approve them. They are emailed your decision.</p></div></div><div class="card flush" id="list"></div>';
        const list = ctx.view.querySelector("#list");
        let pending = [];
        const load = async function () {
            list.innerHTML = ui.loading("Loading applications...");
            try {
                pending = (await api("/api/admin/instructors/pending")).data;
                if (!ctx.alive()) return;
                list.innerHTML = pending.length ? '<div class="table-wrap"><table class="table"><thead><tr><th>Name</th><th>Contact</th><th>Institution</th><th>Faculty ID</th><th>Designation</th><th>Registration Date</th><th>Email</th><th class="right">Actions</th></tr></thead><tbody>'
                    + pending.map(function (u, i) {
                        return '<tr><td><div class="cell-title">' + esc(u.fullName) + "</div></td><td>" + esc(u.email) + '<div class="cell-sub">' + esc(u.phone) + "</div></td><td>" + esc(u.organisation) + '<div class="cell-sub">' + esc(u.department) + "</div></td><td>" + esc(u.idNumber) + "</td><td>" + esc(u.designation) + '</td><td class="nowrap">' + esc(fmt.date(u.createdAt)) + "</td><td>" + ui.badge(u.emailVerified ? "VERIFIED" : "UNVERIFIED")
                            + '</td><td><div class="actions"><button type="button" class="btn btn-sm btn-primary" data-approve="' + i + '"' + (u.emailVerified ? "" : ' disabled title="The instructor must verify their email first"') + '>Approve</button><button type="button" class="btn btn-sm btn-danger" data-reject="' + i + '">Reject</button></div>'
                            + (u.emailVerified ? "" : '<div class="cell-sub right">Waiting for email verification</div>') + "</td></tr>";
                    }).join("") + "</tbody></table></div>" : ui.emptyState("No pending instructor applications.", "New instructor registrations will appear here for review.", "", "shield");
            } catch (error) { retryable(ctx, list, error, load); }
        };
        list.addEventListener("click", async function (event) {
            const approve = event.target.closest("[data-approve]"), reject = event.target.closest("[data-reject]");
            const button = approve || reject;
            if (!button || button.disabled) return;
            const u = pending[Number(button.getAttribute(approve ? "data-approve" : "data-reject"))];
            const ok = await ui.confirm(approve
                ? { title: "Approve " + u.fullName + "?", message: "They will be able to log in and create exams. An approval email is sent to " + u.email + ".", confirmText: "Approve" }
                : { title: "Reject " + u.fullName + "?", message: "They will not be able to log in. A rejection email is sent to " + u.email + ".", confirmText: "Reject", danger: true });
            if (!ok) return;
            ui.setBusy(button, true, approve ? "Approving..." : "Rejecting...");
            try {
                const response = await api("/api/admin/instructors/" + u.id + (approve ? "/approve" : "/reject"), { method: "POST" });
                ui.toast(response.message);
                load();
            } catch (error) { ui.setBusy(button, false); if (error.code !== "SESSION_EXPIRED") ui.toast(error.message, "error"); }
        });
        await load();
    };

    // ------------------------------------------------------------------ exams
    ES.pages.adminExams = async function (ctx) {
        const filters = (await api("/api/admin/exams/filters")).data;
        if (!ctx.alive()) return;
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Exams</h1><p>Every exam on the platform, including drafts.</p></div></div>'
            + '<form class="toolbar" id="filters" role="search"><div class="field grow"><label for="q">Search</label><div class="search-input">' + icon("search") + '<input class="input" id="q" name="q" type="search" placeholder="Exam title"></div></div>'
            + ui.field({ name: "status", label: "Status", type: "select", options: [["", "All statuses"], ["DRAFT", "Draft"], ["PUBLISHED", "Published"], ["CLOSED", "Closed"]] })
            + ui.field({ name: "category", label: "Category", type: "select", options: [["", "All categories"]].concat(filters.categories.map(function (c) { return [c, c]; })) })
            + ui.field({ name: "instructorId", label: "Instructor", type: "select", options: [["", "All instructors"]].concat(filters.instructors.map(function (i) { return [i.id, i.name]; })) })
            + '<button type="button" class="btn" id="clear-btn">Clear</button></form><div class="card flush" id="list"></div>';
        const form = ctx.view.querySelector("#filters"), list = ctx.view.querySelector("#list");
        const load = async function () {
            list.innerHTML = ui.loading("Loading exams...");
            try {
                const exams = (await api("/api/admin/exams", { query: ui.formValues(form) })).data;
                if (!ctx.alive()) return;
                const filtered = Object.values(ui.formValues(form)).some(Boolean);
                list.innerHTML = exams.length ? '<div class="table-wrap"><table class="table"><thead><tr><th>Exam</th><th>Instructor</th><th>Category</th><th>Status</th><th>Created date</th><th>Questions</th><th>Attempts</th><th class="right">Action</th></tr></thead><tbody>'
                    + exams.map(function (e) {
                        return '<tr><td><div class="cell-title">' + esc(e.title) + "</div>" + (e.demoData ? '<div class="cell-sub">Demo data</div>' : "") + "</td><td>" + esc(e.instructorName) + "</td><td>" + esc(e.category) + "</td><td>" + ui.badge(e.status) + '</td><td class="nowrap">' + esc(fmt.date(e.createdAt)) + '</td><td class="num">' + e.questionCount + '</td><td class="num">' + (e.attempts || 0)
                            + '</td><td><div class="actions"><a class="btn btn-sm" href="#/admin/exam/' + e.id + '">Details</a></div></td></tr>';
                    }).join("") + "</tbody></table></div>" : filtered ? ui.emptyState("No exams match these filters", "Change or clear the filters to see more.", "", "search") : ui.emptyState("No exams yet", "Exams created by instructors will be listed here.", "", "book");
            } catch (error) { retryable(ctx, list, error, load); }
        };
        form.addEventListener("submit", function (e) { e.preventDefault(); load(); });
        form.querySelector("#q").addEventListener("input", ES.debounce(load, 300));
        form.querySelectorAll("select").forEach(function (s) { s.addEventListener("change", load); });
        ctx.view.querySelector("#clear-btn").addEventListener("click", function () { form.reset(); load(); });
        await load();
    };

    ES.pages.adminExam = async function (ctx) {
        const loaded = await Promise.all([api("/api/exams/" + ctx.params.id), api("/api/admin/exams/" + ctx.params.id + "/analytics")]);
        if (!ctx.alive()) return;
        const detail = loaded[0].data, e = detail.exam, analytics = loaded[1].data, LETTERS = ES.parts.LETTERS;
        const fact = function (label, value) { return '<div class="fact"><small>' + label + "</small><b>" + esc(value) + "</b></div>"; };
        ctx.view.innerHTML = '<div><a class="crumb" href="#/admin/exams">' + icon("arrowLeft") + 'Exams</a><div class="page-head"><div><h1>' + esc(e.title) + "</h1><p>" + esc(e.description || "No description provided.") + '</p></div><div class="btn-row">' + ui.badge(e.status)
            + (e.status === "PUBLISHED" ? '<button type="button" class="btn btn-danger" id="close-btn">Close exam</button>' : "") + '<a class="btn" href="#/admin/results?examId=' + e.id + '">Results</a></div></div></div>'
            + '<div class="facts-row">' + fact("Instructor", e.instructorName) + fact("Category", e.category) + fact("Duration", fmt.duration(e.durationMinutes)) + fact("Total Marks", e.totalMarks) + fact("Pass Marks", e.passMarks) + fact("Questions", e.questionCount) + fact("Created", fmt.date(e.createdAt)) + "</div>"
            + "<h2>Performance</h2>" + ES.parts.analyticsHtml(analytics)
            + '<div class="card flush"><div class="card-head"><div><h2>Questions and answer key</h2></div></div>' + (detail.questions.length ? '<div class="table-wrap"><table class="table"><thead><tr><th>#</th><th>Question</th><th>Marks</th><th>Correct Option</th></tr></thead><tbody>'
                + detail.questions.map(function (q, i) {
                    const ci = q.options.findIndex(function (o) { return o.correct; });
                    return '<tr><td class="num">' + (i + 1) + '</td><td><div class="q-row-text">' + esc(q.text) + '</div></td><td class="num">' + q.marks + "</td><td>" + (ci >= 0 ? "<b>" + LETTERS[ci] + ".</b> " + esc(q.options[ci].text) : "-") + "</td></tr>";
                }).join("") + "</tbody></table></div>" : ui.emptyState("No questions yet", "The instructor has not added questions to this exam.", "", "list")) + "</div>";
        const close = ctx.view.querySelector("#close-btn");
        if (close) close.addEventListener("click", async function () {
            const ok = await ui.confirm({ title: "Close this exam?", message: "No new attempts will be allowed for \"" + e.title + "\". Existing results are kept. This cannot be undone.", confirmText: "Close exam", danger: true });
            if (!ok) return;
            try { const response = await api("/api/exams/" + e.id + "/close", { method: "POST" }); ui.toast(response.message); ES.router.resolve(); }
            catch (error) { if (error.code !== "SESSION_EXPIRED") ui.toast(error.message, "error"); }
        });
    };

    // ------------------------------------------------------------------ results
    ES.pages.adminResults = async function (ctx) {
        const exams = (await api("/api/admin/exams")).data;
        if (!ctx.alive()) return;
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Results</h1><p>Every submitted attempt on the platform.</p></div></div>'
            + '<form class="toolbar" id="filters" role="search"><div class="field grow"><label for="q">Search</label><div class="search-input">' + icon("search") + '<input class="input" id="q" name="q" type="search" placeholder="Student name, email or exam"></div></div>'
            + ui.field({ name: "examId", label: "Exam", type: "select", value: ctx.query.examId || "", options: [["", "All exams"]].concat(exams.map(function (e) { return [e.id, e.title]; })) })
            + ui.field({ name: "status", label: "Pass / Fail", type: "select", options: [["", "All"], ["PASSED", "Passed"], ["FAILED", "Failed"]] })
            + ui.field({ name: "from", label: "From", type: "date" }) + ui.field({ name: "to", label: "To", type: "date" }) + '<button type="button" class="btn" id="clear-btn">Clear</button></form><div class="card flush" id="list"></div>';
        const form = ctx.view.querySelector("#filters"), list = ctx.view.querySelector("#list");
        const load = async function (page) {
            list.innerHTML = ui.loading("Loading results...");
            try {
                const data = (await api("/api/admin/results", { query: Object.assign(ui.formValues(form), { page: page || 0, size: 10 }) })).data;
                if (!ctx.alive()) return;
                const filtered = Object.values(ui.formValues(form)).some(Boolean);
                list.innerHTML = data.content.length ? '<div class="table-wrap"><table class="table">' + ES.parts.attemptHead(true) + "<tbody>" + ES.parts.attemptRows(data.content, true) + "</tbody></table></div>" + ui.pagination(data, load)
                    : filtered ? ui.emptyState("No results match these filters", "Change or clear the filters to see more.", "", "search") : ui.emptyState("No results yet", "Results appear here once students submit exams.", "", "award");
            } catch (error) { retryable(ctx, list, error, function () { load(page); }); }
        };
        form.addEventListener("submit", function (e) { e.preventDefault(); load(0); });
        form.querySelector("#q").addEventListener("input", ES.debounce(function () { load(0); }, 300));
        form.querySelectorAll("select, input[type=date]").forEach(function (el) { el.addEventListener("change", function () { load(0); }); });
        ctx.view.querySelector("#clear-btn").addEventListener("click", function () { form.reset(); form.elements.examId.value = ""; load(0); });
        await load(0);
    };

    // ------------------------------------------------------------------ reports
    ES.pages.adminReports = async function (ctx) {
        const r = (await api("/api/admin/reports")).data;
        if (!ctx.alive()) return;
        const has = r.totalAttempts > 0;
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Reports</h1><p>Platform overview, per-exam and per-student figures. All values are queried from the database when this page loads.</p></div></div>'
            + '<div class="stat-grid">' + ui.stat("Total Users", r.totalUsers, "users", null, true) + ui.stat("Students", r.students, "cap") + ui.stat("Instructors", r.instructors, "edit") + ui.stat("Published Exams", r.publishedExams, "send")
            + ui.stat("Total Attempts", r.totalAttempts, "clock") + ui.stat("Average Score", has ? fmt.pct(r.averagePercentage) : "-", "chart") + ui.stat("Pass Rate", has ? fmt.pct(r.passRate) : "-", "award") + "</div>"
            + '<div class="grid-2"><div class="card"><div class="card-head"><div><h2>Average score by exam</h2><p>Average percentage</p></div></div>' + ui.barChart(r.examReport.map(function (s) { return { label: s.title, value: s.averagePercentage }; }), { max: 100, format: fmt.pct, label: "Average percentage by exam", emptyTitle: "No attempts yet", emptyText: "Figures appear once students submit exams." }) + "</div>"
            + '<div class="card"><div class="card-head"><div><h2>Pass rate by exam</h2></div></div>' + ui.barChart(r.examReport.map(function (s) { return { label: s.title, value: s.passRate, tone: s.passRate < 40 ? "danger" : s.passRate < 70 ? "warning" : "" }; }), { max: 100, format: fmt.pct, label: "Pass rate by exam", emptyTitle: "No attempts yet", emptyText: "Figures appear once students submit exams." }) + "</div></div>"
            + '<div class="card flush"><div class="card-head"><div><h2>Exam report</h2></div></div>' + (r.examReport.length ? '<div class="table-wrap"><table class="table"><thead><tr><th>Exam</th><th>Attempts</th><th>Average Score</th><th>Highest</th><th>Lowest</th><th>Pass Rate</th></tr></thead><tbody>'
                + r.examReport.map(function (s) { return '<tr><td class="cell-title">' + esc(s.title) + '</td><td class="num">' + s.attempts + '</td><td class="num">' + fmt.num(s.averageScore) + " / " + s.totalMarks + " (" + fmt.pct(s.averagePercentage) + ')</td><td class="num">' + s.highestScore + '</td><td class="num">' + s.lowestScore + '</td><td><div style="display:flex;align-items:center;gap:10px"><span class="num" style="min-width:44px">' + fmt.pct(s.passRate) + "</span>" + ui.meter(s.passRate) + "</div></td></tr>"; }).join("")
                + "</tbody></table></div>" : ui.emptyState("No exam data yet", "This report fills in when students submit exams.", "", "chart")) + "</div>"
            + '<div class="card flush"><div class="card-head"><div><h2>User report</h2><p>Students with at least one submitted attempt</p></div></div>' + (r.studentReport.length ? '<div class="table-wrap"><table class="table"><thead><tr><th>Student</th><th>Attempts</th><th>Average Score</th><th>Pass Count</th><th>Fail Count</th></tr></thead><tbody>'
                + r.studentReport.map(function (s) { return '<tr><td><div class="cell-title">' + esc(s.name) + '</div><div class="cell-sub">' + esc(s.email) + '</div></td><td class="num">' + s.attempts + '</td><td class="num">' + fmt.pct(s.averagePercentage) + '</td><td class="num">' + s.passCount + '</td><td class="num">' + s.failCount + "</td></tr>"; }).join("")
                + "</tbody></table></div>" : ui.emptyState("No student data yet", "This report fills in when students submit exams.", "", "users")) + "</div>";
    };

    // ------------------------------------------------------------------ activity
    ES.pages.adminActivity = async function (ctx) {
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Activity</h1><p>Sign-ins, registrations, exam and approval events, newest first.</p></div></div><div class="card flush" id="list"></div>';
        const list = ctx.view.querySelector("#list");
        const load = async function (page) {
            list.innerHTML = ui.loading("Loading activity...");
            try {
                const data = (await api("/api/admin/activity", { query: { page: page || 0, size: 15 } })).data;
                if (!ctx.alive()) return;
                list.innerHTML = data.content.length ? '<div style="padding:4px 20px">' + activityList(data.content) + "</div>" + ui.pagination(data, load) : ui.emptyState("No activity yet", "Events are recorded as people use the platform.", "", "activity");
            } catch (error) { retryable(ctx, list, error, function () { load(page); }); }
        };
        await load(0);
    };
})(window.ES);
