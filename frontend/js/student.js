/* Student pages: dashboard, exam catalogue, exam details, the timed attempt, result, review and history. */
(function (ES) {
    "use strict";
    const ui = ES.ui, esc = ES.esc, api = ES.api, router = ES.router, fmt = ES.fmt, icon = ui.icon;
    const LETTERS = ["A", "B", "C", "D", "E", "F"];

    function resultRows(results, withAction) {
        return results.map(function (r) {
            const done = r.status === "SUBMITTED";
            return "<tr><td><div class=\"cell-title\">" + esc(r.examTitle) + '</div><div class="cell-sub">' + esc(r.category) + "</div></td>"
                + '<td class="nowrap">' + esc(fmt.dateTime(done ? r.submittedAt : r.startedAt)) + "</td>"
                + '<td class="num">' + (done ? r.marksObtained + " / " + r.totalMarks : "-") + "</td>"
                + '<td class="num">' + (done ? fmt.pct(r.percentage) : "-") + "</td>"
                + "<td>" + ui.passBadge(r) + "</td>"
                + '<td class="num">' + (done ? r.attempted + " / " + r.totalQuestions : "-") + "</td>"
                + (withAction ? '<td><div class="actions">' + (done
                    ? '<a class="btn btn-sm" href="#/student/result/' + r.attemptId + '">View Result</a>'
                    : '<a class="btn btn-sm btn-primary" href="#/student/attempt/' + r.attemptId + '">Resume</a>') + "</div></td>" : "") + "</tr>";
        }).join("");
    }

    function examAction(item) {
        const exam = item.exam;
        if (item.attemptState === "COMPLETED") return '<a class="btn btn-sm" href="#/student/result/' + item.attemptId + '">View Result</a>';
        if (item.attemptState === "IN_PROGRESS") return '<a class="btn btn-sm btn-primary" href="#/student/attempt/' + item.attemptId + '">Resume</a>';
        if (exam.availability === "UPCOMING") return '<a class="btn btn-sm" href="#/student/exam/' + exam.id + '">View details</a>';
        return '<a class="btn btn-sm btn-primary" href="#/student/exam/' + exam.id + '">Start</a>';
    }
    function examStatusBadge(item) {
        if (item.attemptState === "COMPLETED") return ui.badge(item.passed ? "PASSED" : "FAILED");
        if (item.attemptState === "IN_PROGRESS") return ui.badge("IN_PROGRESS");
        return ui.badge(item.exam.availability, item.exam.availability === "OPEN" ? "Available" : null);
    }
    function examCard(item) {
        const e = item.exam;
        return '<article class="exam-card"><div class="top"><div><h3>' + esc(e.title) + '</h3><p class="by">' + esc(e.instructorName) + "</p></div>" + examStatusBadge(item) + "</div>"
            + '<div><span class="pill">' + esc(e.category) + "</span></div>"
            + '<div class="facts"><div class="fact"><small>Duration</small><b>' + fmt.duration(e.durationMinutes) + '</b></div><div class="fact"><small>Questions</small><b>' + e.questionCount + "</b></div>"
            + '<div class="fact"><small>Total marks</small><b>' + e.totalMarks + '</b></div><div class="fact"><small>Pass marks</small><b>' + e.passMarks + "</b></div></div>"
            + '<div class="foot"><span class="small muted">' + (e.availability === "UPCOMING" ? "Opens " + esc(fmt.dateTime(e.scheduledAt)) : e.closesAt ? "Closes " + esc(fmt.dateTime(e.closesAt)) : "No closing date") + "</span>" + examAction(item) + "</div></article>";
    }

    // ------------------------------------------------------------------ dashboard
    ES.pages.studentDashboard = async function (ctx) {
        const d = (await api("/api/student/dashboard")).data;
        if (!ctx.alive()) return;
        const attempted = d.examsAttempted;
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Welcome back, ' + esc(ctx.user.fullName.split(" ")[0]) + "</h1><p>"
            + (attempted ? "Here is how your exams are going." : "You have not taken an exam yet. Pick one below to get started.") + '</p></div><a class="btn btn-primary" href="#/student/exams">' + icon("book") + "Browse exams</a></div>"
            + '<div class="stat-grid">' + ui.stat("Available Exams", d.availableExams, "book", "open to you now", true) + ui.stat("Exams Attempted", attempted, "clock")
            + ui.stat("Exams Passed", d.examsPassed, "award") + ui.stat("Average Score", attempted ? fmt.pct(d.averagePercentage) : "-", "chart") + "</div>"
            + '<div class="grid-main"><div class="card"><div class="card-head"><div><h2>Score trend</h2><p>Percentage in each exam, oldest to newest</p></div></div>'
            + ui.lineChart(d.scoreTrend, { suffix: "%", label: "Score trend", emptyTitle: "No scores yet", emptyText: "Your scores will be plotted here after your first exam." }) + "</div>"
            + '<div class="card"><div class="card-head"><div><h2>Pass / fail</h2><p>All submitted exams</p></div></div>'
            + ui.donutChart([{ label: "Passed", value: d.examsPassed, color: "var(--accent)" }, { label: "Failed", value: d.examsFailed, color: "var(--danger)" }], { centerLabel: "exams", emptyTitle: "Nothing to show yet", emptyText: "Your results will appear here after you complete an exam." }) + "</div></div>"
            + '<div class="card"><div class="card-head"><div><h2>Available exams</h2><p>Open or coming up</p></div><a class="btn btn-sm" href="#/student/exams">See all</a></div>'
            + (d.availableExamList.length ? '<div class="exam-grid">' + d.availableExamList.map(examCard).join("") + "</div>" : ui.emptyState("No exams available right now.", "New exams appear here as soon as an instructor publishes them.", "", "book")) + "</div>"
            + '<div class="card flush"><div class="card-head"><div><h2>Recent results</h2></div><a class="btn btn-sm" href="#/student/attempts">View Attempt History</a></div>'
            + (d.recentAttempts.length ? '<div class="table-wrap"><table class="table"><thead><tr><th>Exam</th><th>Date</th><th>Score</th><th>Percentage</th><th>Status</th><th>Attempted</th><th class="right">Action</th></tr></thead><tbody>'
                + resultRows(d.recentAttempts, true) + "</tbody></table></div>" : ui.emptyState("You haven't attempted any exams yet.", "Your results will appear here after you complete an exam.", '<a class="btn btn-primary" href="#/student/exams">Find an exam</a>', "award")) + "</div>";
    };

    // ------------------------------------------------------------------ available exams
    ES.pages.studentExams = async function (ctx) {
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Available Exams</h1><p>Search by title, category or instructor, then open an exam to read its instructions.</p></div></div>'
            + '<form class="toolbar" id="filters" role="search"><div class="field grow"><label for="q">Search</label><div class="search-input">' + icon("search") + '<input class="input" id="q" name="q" type="search" placeholder="Title, category or instructor" value="' + esc(ctx.query.q || "") + '"></div></div>'
            + ui.field({ name: "category", label: "Category", type: "select", options: [["", "All categories"]] })
            + ui.field({ name: "duration", label: "Duration", type: "select", options: [["", "Any length"], ["SHORT", "Up to 20 min"], ["MEDIUM", "21 to 45 min"], ["LONG", "Over 45 min"]] })
            + ui.field({ name: "status", label: "Status", type: "select", options: [["", "All"], ["AVAILABLE", "Available"], ["UPCOMING", "Upcoming"], ["IN_PROGRESS", "In progress"], ["COMPLETED", "Completed"]] })
            + ui.field({ name: "sort", label: "Sort by", type: "select", options: [["newest", "Newest"], ["title", "Title"], ["duration", "Duration"]] })
            + '<button type="button" class="btn" id="clear-btn">Clear</button></form><div id="list">' + ui.loading("Loading exams...") + "</div>";
        const form = ctx.view.querySelector("#filters"), list = ctx.view.querySelector("#list");
        let request = 0;

        const load = async function () {
            const mine = ++request;
            list.innerHTML = ui.loading("Loading exams...");
            try {
                const items = (await api("/api/student/exams", { query: ui.formValues(form) })).data;
                if (!ctx.alive() || mine !== request) return;
                const filtered = Object.values(ui.formValues(form)).some(function (v) { return v && v !== "newest"; });
                list.innerHTML = items.length ? '<p class="muted small" style="margin-bottom:12px">' + items.length + " exam" + (items.length === 1 ? "" : "s") + '</p><div class="exam-grid">' + items.map(examCard).join("") + "</div>"
                    : filtered ? ui.emptyState("No exams match your search", "Try a different word or clear the filters.", "", "search")
                        : ui.emptyState("No exams available right now.", "New exams appear here as soon as an instructor publishes them.", "", "book");
            } catch (error) {
                if (ctx.alive() && mine === request && error.code !== "SESSION_EXPIRED") {
                    list.innerHTML = ui.errorState(error.message, true);
                    list.querySelector("[data-retry]").addEventListener("click", load);
                }
            }
        };
        form.addEventListener("submit", function (e) { e.preventDefault(); load(); });
        form.querySelector("#q").addEventListener("input", ES.debounce(load, 300));
        form.querySelectorAll("select").forEach(function (s) { s.addEventListener("change", load); });
        ctx.view.querySelector("#clear-btn").addEventListener("click", function () { form.reset(); form.querySelector("#q").value = ""; load(); });

        api("/api/student/exams/categories").then(function (response) {
            if (!ctx.alive()) return;
            const select = form.elements.category;
            (response.data || []).forEach(function (c) { const o = document.createElement("option"); o.value = c; o.textContent = c; select.appendChild(o); });
        }).catch(function () { /* the filter just stays at "All categories" */ });
        await load();
    };

    // ------------------------------------------------------------------ exam details
    ES.pages.studentExam = async function (ctx) {
        const item = (await api("/api/student/exams/" + ctx.params.id)).data;
        if (!ctx.alive()) return;
        const e = item.exam;
        const fact = function (label, value) { return '<div class="fact"><small>' + label + "</small><b>" + esc(value) + "</b></div>"; };
        const rules = ["Read every question carefully before answering.", "The exam is timed: the countdown starts as soon as you begin and cannot be paused.",
            "When the time runs out, the exam is submitted automatically with the answers saved so far.", "Answers are evaluated automatically and your result is shown right away.",
            "Avoid refreshing the page. If you do, you return to the same attempt and the timer keeps running.", "Submit before the timer ends. You get one attempt at this exam."];
        let action;
        if (item.attemptState === "COMPLETED") action = '<a class="btn btn-primary btn-lg" href="#/student/result/' + item.attemptId + '">View Result</a>';
        else if (item.attemptState === "IN_PROGRESS") action = '<a class="btn btn-primary btn-lg" href="#/student/attempt/' + item.attemptId + '">Resume Exam</a>';
        else if (e.availability === "UPCOMING") action = '<button type="button" class="btn btn-lg" disabled>Opens ' + esc(fmt.dateTime(e.scheduledAt)) + "</button>";
        else if (e.availability !== "OPEN") action = '<button type="button" class="btn btn-lg" disabled>This exam is closed</button>';
        else action = '<button type="button" class="btn btn-primary btn-lg" id="start-btn">Start Exam</button>';

        ctx.view.innerHTML = '<div><a class="crumb" href="#/student/exams">' + icon("arrowLeft") + 'Available Exams</a><div class="page-head"><div><h1>' + esc(e.title) + "</h1><p>" + esc(e.description || "No description provided.") + "</p></div>" + examStatusBadge(item) + "</div></div>"
            + '<div class="facts-row">' + fact("Category", e.category) + fact("Instructor", e.instructorName) + fact("Duration", fmt.duration(e.durationMinutes)) + fact("Total Marks", e.totalMarks) + fact("Pass Marks", e.passMarks) + fact("Questions", e.questionCount) + "</div>"
            + '<div class="card stack"><div class="card-head"><h2>Instructions</h2></div><ul class="instructions">' + rules.map(function (r) { return "<li>" + icon("check") + "<span>" + r + "</span></li>"; }).join("") + "</ul>"
            + '<div class="btn-row" style="margin-top:8px">' + action + '<a class="btn btn-ghost" href="#/student/exams">Back</a></div></div>';

        const start = ctx.view.querySelector("#start-btn");
        if (start) start.addEventListener("click", async function () {
            const ok = await ui.confirm({ title: "Are you ready to begin?", message: "The " + e.durationMinutes + "-minute timer starts immediately and cannot be paused. You get one attempt.", confirmText: "Start Exam", cancelText: "Cancel" });
            if (!ok) return;
            ui.setBusy(start, true, "Starting...");
            try {
                const response = await api("/api/exams/" + e.id + "/attempts", { method: "POST" });
                router.go("/student/attempt/" + response.data.attemptId);
            } catch (error) {
                ui.setBusy(start, false);
                if (error.code !== "SESSION_EXPIRED") ui.toast(error.message, "error");
            }
        });
    };

    // ------------------------------------------------------------------ the exam itself
    ES.pages.studentAttempt = async function (ctx) {
        const attemptId = ctx.params.id;
        ctx.view.innerHTML = '<div class="exam-screen">' + ui.loading("Loading your exam...") + "</div>";
        const data = (await api("/api/attempts/" + attemptId)).data;
        if (!ctx.alive()) return;
        if (data.status === "SUBMITTED") return router.replace("/student/result/" + attemptId);

        const questions = data.questions;
        // answers: questionId -> { selectedOptionId, markedForReview }. This is the only client state.
        const answers = {};
        questions.forEach(function (q) { answers[q.id] = { selectedOptionId: null, markedForReview: false }; });
        (data.answers || []).forEach(function (a) { if (answers[a.questionId]) answers[a.questionId] = { selectedOptionId: a.selectedOptionId || null, markedForReview: !!a.markedForReview }; });
        let current = 0, submitting = false, finished = false, unsaved = {};
        // The countdown is display only. It is anchored to the server's remainingSeconds; the server decides what is on time.
        let deadline = Date.now() + data.remainingSeconds * 1000;

        ctx.view.innerHTML = '<div class="exam-screen"><header class="exam-top"><div style="min-width:0"><h1>' + esc(data.examTitle) + '</h1><span class="meta">' + esc(data.category) + " &middot; " + questions.length + " questions &middot; " + data.totalMarks + ' marks</span></div>'
            + '<div class="btn-row"><span class="timer" id="timer" role="timer" aria-label="Time remaining">' + icon("clock") + '<span id="timer-text">--:--</span></span><button type="button" class="btn btn-primary" id="submit-btn">Submit Exam</button></div></header>'
            + '<div class="exam-body"><main class="card question-card" id="main"></main><aside class="card palette-card" aria-label="Question palette"><div class="card-head"><h2>Questions</h2><span class="save-state" id="save-state" aria-live="polite"></span></div><div class="palette" id="palette"></div>'
            + '<div class="pal-legend"><span><i class="current"></i>Current</span><span><i class="answered"></i>Answered</span><span><i></i>Unanswered</span><span><i class="marked"></i>Marked for review</span></div></aside></div></div>';

        const main = ctx.view.querySelector("#main"), palette = ctx.view.querySelector("#palette"), timerBox = ctx.view.querySelector("#timer"),
            timerText = ctx.view.querySelector("#timer-text"), saveState = ctx.view.querySelector("#save-state"), submitBtn = ctx.view.querySelector("#submit-btn");

        const counts = function () {
            let answered = 0, marked = 0;
            questions.forEach(function (q) { if (answers[q.id].selectedOptionId) answered++; if (answers[q.id].markedForReview) marked++; });
            return { answered: answered, unanswered: questions.length - answered, marked: marked };
        };
        const payload = function () {
            return questions.map(function (q) { return { questionId: q.id, selectedOptionId: answers[q.id].selectedOptionId, markedForReview: answers[q.id].markedForReview }; });
        };

        const renderPalette = function () {
            palette.innerHTML = questions.map(function (q, i) {
                const a = answers[q.id];
                const states = [i === current ? "current" : "", a.selectedOptionId ? "answered" : "", a.markedForReview ? "marked" : ""].join(" ");
                const label = "Question " + (i + 1) + ": " + (a.selectedOptionId ? "answered" : "unanswered") + (a.markedForReview ? ", marked for review" : "") + (i === current ? ", current" : "");
                return '<button type="button" class="pal-btn ' + states + '" data-index="' + i + '" aria-label="' + label + '"' + (i === current ? ' aria-current="true"' : "") + ">" + (i + 1) + "</button>";
            }).join("");
        };
        const renderQuestion = function () {
            const q = questions[current], a = answers[q.id];
            main.innerHTML = '<div class="q-head"><span>Question ' + (current + 1) + " of " + questions.length + '</span><span class="pill">' + q.marks + " mark" + (q.marks === 1 ? "" : "s") + "</span></div>"
                + '<p class="q-text" id="q-text">' + esc(q.text) + '</p><fieldset class="options" aria-labelledby="q-text">'
                + q.options.map(function (o, i) {
                    return '<label class="option"><input type="radio" name="option" value="' + o.id + '"' + (a.selectedOptionId === o.id ? " checked" : "") + '><span class="letter">' + LETTERS[i] + '</span><span class="opt-text">' + esc(o.text) + "</span></label>";
                }).join("") + "</fieldset>"
                + '<div class="q-actions"><div class="btn-row"><button type="button" class="btn" id="mark-btn" aria-pressed="' + a.markedForReview + '">' + icon("flag") + (a.markedForReview ? "Unmark review" : "Mark for Review") + "</button>"
                + '<button type="button" class="btn btn-ghost" id="clear-btn"' + (a.selectedOptionId ? "" : " disabled") + '>Clear answer</button></div>'
                + '<div class="btn-row"><button type="button" class="btn" id="prev-btn"' + (current === 0 ? " disabled" : "") + ">" + icon("arrowLeft") + 'Previous</button><button type="button" class="btn btn-primary" id="next-btn"' + (current === questions.length - 1 ? " disabled" : "") + ">Next" + icon("arrowRight") + "</button></div></div>";
            main.querySelectorAll('input[name="option"]').forEach(function (input) {
                input.addEventListener("change", function () { a.selectedOptionId = Number(input.value); save(q.id); renderPalette(); main.querySelector("#clear-btn").disabled = false; });
            });
            main.querySelector("#mark-btn").addEventListener("click", function () { a.markedForReview = !a.markedForReview; save(q.id); renderQuestion(); renderPalette(); });
            main.querySelector("#clear-btn").addEventListener("click", function () { a.selectedOptionId = null; save(q.id); renderQuestion(); renderPalette(); });
            main.querySelector("#prev-btn").addEventListener("click", function () { go(current - 1); });
            main.querySelector("#next-btn").addEventListener("click", function () { go(current + 1); });
        };
        const go = function (index) {
            if (index < 0 || index >= questions.length) return;
            current = index; renderQuestion(); renderPalette();
        };
        palette.addEventListener("click", function (event) {
            const button = event.target.closest("[data-index]");
            if (button) go(Number(button.getAttribute("data-index")));
        });

        // Each change is saved to the server straight away, so a refresh or a crash loses nothing.
        const save = async function (questionId) {
            if (finished) return;
            const a = answers[questionId];
            saveState.className = "save-state"; saveState.textContent = "Saving...";
            try {
                await api("/api/attempts/" + attemptId + "/answers", { method: "PUT", body: { questionId: questionId, selectedOptionId: a.selectedOptionId, markedForReview: a.markedForReview } });
                delete unsaved[questionId];
                if (!Object.keys(unsaved).length) saveState.textContent = "All answers saved";
            } catch (error) {
                if (error.code === "ALREADY_SUBMITTED") return leaveToResult();
                if (error.code === "TIME_EXPIRED") return submit(true);
                unsaved[questionId] = true;
                saveState.className = "save-state warn";
                saveState.textContent = "Not saved yet - will retry when you submit";
            }
        };

        const leaveToResult = function () {
            finished = true;
            router.replace("/student/result/" + attemptId);
        };

        const submit = async function (auto) {
            if (submitting || finished) return;
            submitting = true;
            ui.closeModal();
            ui.setBusy(submitBtn, true, "Submitting...");
            try {
                // The request carries answers only. Marks are calculated by the server.
                await api("/api/attempts/" + attemptId + "/submit", { method: "POST", body: { answers: payload(), autoSubmit: !!auto } });
                finished = true;
                ui.toast(auto ? "Time is up. Your exam was submitted automatically." : "Exam submitted successfully.", auto ? "info" : "success");
                router.replace("/student/result/" + attemptId);
            } catch (error) {
                if (error.code === "ALREADY_SUBMITTED") return leaveToResult();
                submitting = false;
                ui.setBusy(submitBtn, false);
                if (error.code === "SESSION_EXPIRED") { finished = true; return; }
                ui.toast(error.message + (auto ? " Retrying..." : ""), "error");
                if (auto && ctx.alive()) setTimeout(function () { submit(true); }, 4000);
            }
        };

        submitBtn.addEventListener("click", async function () {
            const c = counts();
            const ok = await ui.confirm({
                title: "Submit Exam?", message: c.unanswered ? "You still have unanswered questions. After submitting you cannot change any answer." : "After submitting you cannot change any answer.",
                extra: '<div class="count-row"><div><b>' + c.answered + "</b><small>Answered</small></div><div><b>" + c.unanswered + "</b><small>Unanswered</small></div><div><b>" + c.marked + "</b><small>Marked for Review</small></div></div>",
                confirmText: "Submit Exam", cancelText: "Continue Exam"
            });
            if (ok) submit(false);
        });

        const tick = function () {
            const left = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
            timerText.textContent = fmt.clock(left);
            timerBox.classList.toggle("low", left <= 300 && left > 60);
            timerBox.classList.toggle("critical", left <= 60);
            if (left <= 0 && !submitting && !finished) submit(true);
        };
        const interval = setInterval(tick, 500);

        // Re-anchor the countdown to the server whenever the tab becomes visible again (laptops sleep, tabs get throttled).
        const onVisible = async function () {
            if (document.hidden || finished || submitting) return;
            try {
                const fresh = (await api("/api/attempts/" + attemptId)).data;
                if (fresh.status === "SUBMITTED") return leaveToResult();
                deadline = Date.now() + fresh.remainingSeconds * 1000;
                tick();
            } catch (e) { /* keep the local countdown */ }
        };
        const onBeforeUnload = function (event) { if (!finished) { event.preventDefault(); event.returnValue = ""; } };
        document.addEventListener("visibilitychange", onVisible);
        window.addEventListener("beforeunload", onBeforeUnload);
        ctx.onLeave(function () {
            clearInterval(interval);
            document.removeEventListener("visibilitychange", onVisible);
            window.removeEventListener("beforeunload", onBeforeUnload);
        });

        renderQuestion(); renderPalette(); tick();
    };

    // ------------------------------------------------------------------ result
    function statGrid(r) {
        return '<div class="stat-grid">' + ui.stat("Total Questions", r.totalQuestions, "list") + ui.stat("Attempted", r.attempted, "edit") + ui.stat("Correct", r.correct, "check")
            + ui.stat("Incorrect", r.incorrect, "x") + ui.stat("Unanswered", r.unanswered, "info") + ui.stat("Marks Obtained", r.marksObtained, "award") + ui.stat("Total Marks", r.totalMarks, "target") + "</div>";
    }
    ES.pages.studentResult = async function (ctx) {
        const r = (await api("/api/attempts/" + ctx.params.id + "/result")).data;
        if (!ctx.alive()) return;
        const summary = r.passed
            ? "You scored " + r.marksObtained + " of " + r.totalMarks + ", which clears the pass mark of " + r.passMarks + "."
            : "You scored " + r.marksObtained + " of " + r.totalMarks + ". The pass mark is " + r.passMarks + ", so you were " + (r.passMarks - r.marksObtained) + " mark" + (r.passMarks - r.marksObtained === 1 ? "" : "s") + " short.";
        const accuracy = r.attempted ? Math.round((r.correct / r.attempted) * 100) : 0;
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Exam Completed</h1><p>' + esc(r.examTitle) + " &middot; " + esc(r.studentName) + " &middot; submitted " + esc(fmt.dateTime(r.submittedAt)) + "</p></div></div>"
            + (r.autoSubmitted ? '<div class="form-alert info">Time ran out, so this exam was submitted automatically with the answers saved up to that point.</div>' : "")
            + '<div class="card result-hero">' + ui.ring(r.percentage, fmt.pct(r.percentage), r.marksObtained + " / " + r.totalMarks, !r.passed)
            + '<div class="stack"><div>' + ui.badge(r.passed ? "PASSED" : "FAILED") + '</div><div class="verdict ' + (r.passed ? "pass" : "fail") + '">' + (r.passed ? "PASSED" : "FAILED") + '</div><p class="soft">' + esc(summary) + "</p>"
            + '<p class="muted">Performance summary: you answered ' + r.attempted + " of " + r.totalQuestions + " questions and got " + accuracy + "% of those right.</p>"
            + '<div class="btn-row"><a class="btn btn-primary" href="#/student/review/' + r.attemptId + '">Review Answers</a><a class="btn" href="#/student/dashboard">Back to Dashboard</a><a class="btn btn-ghost" href="#/student/attempts">View Attempt History</a></div></div></div>'
            + statGrid(r);
    };

    // ------------------------------------------------------------------ answer review
    ES.pages.studentReview = async function (ctx) {
        const data = (await api("/api/attempts/" + ctx.params.id + "/answers")).data;
        if (!ctx.alive()) return;
        const r = data.result;
        ctx.view.innerHTML = '<div><a class="crumb" href="#/student/result/' + r.attemptId + '">' + icon("arrowLeft") + 'Result</a><div class="page-head"><div><h1>Answer Review</h1><p>' + esc(r.examTitle) + " &middot; " + r.marksObtained + " / " + r.totalMarks + " (" + fmt.pct(r.percentage) + ")</p></div>" + ui.badge(r.passed ? "PASSED" : "FAILED") + "</div></div>"
            + data.items.map(function (item) {
                const letter = function (id) { const i = item.options.findIndex(function (o) { return o.id === id; }); return i >= 0 ? LETTERS[i] : null; };
                const yours = letter(item.selectedOptionId);
                return '<article class="card review-item"><div class="q-head"><span>Question ' + item.number + "</span><span>" + ui.badge(item.outcome, item.outcome === "CORRECT" ? "Correct" : item.outcome === "INCORRECT" ? "Incorrect" : "Not Answered") + ' <span class="pill">' + item.marksAwarded + " / " + item.marks + " marks</span></span></div>"
                    + '<p class="q-text">' + esc(item.text) + '</p><div class="stack" style="gap:8px">' + item.options.map(function (o, i) {
                        const isCorrect = o.id === item.correctOptionId, isYours = o.id === item.selectedOptionId;
                        const cls = isCorrect ? "correct" : isYours ? "wrong" : "";
                        const tag = isCorrect && isYours ? icon("check") + "Your answer &middot; correct" : isCorrect ? icon("check") + "Correct answer" : isYours ? icon("x") + "Your answer" : "";
                        return '<div class="review-opt ' + cls + '"><span class="letter">' + LETTERS[i] + '</span><span style="overflow-wrap:anywhere">' + esc(o.text) + "</span>" + (tag ? '<span class="tag">' + tag + "</span>" : "") + "</div>";
                    }).join("") + '</div><p class="small muted">Your Answer: <b class="soft">' + (yours || "Not Answered") + '</b> &nbsp; Correct Answer: <b class="soft">' + (letter(item.correctOptionId) || "-") + "</b>" + (item.markedForReview ? " &nbsp; You had marked this question for review." : "") + "</p></article>";
            }).join("")
            + '<div class="btn-row"><a class="btn" href="#/student/result/' + r.attemptId + '">Back to result</a><a class="btn btn-ghost" href="#/student/dashboard">Back to Dashboard</a></div>';
    };

    // ------------------------------------------------------------------ attempt history + results
    function historyPage(resultsOnly) {
        return async function (ctx) {
            ctx.view.innerHTML = '<div class="page-head"><div><h1>' + (resultsOnly ? "My Results" : "My Attempts") + "</h1><p>" + (resultsOnly ? "Scores for every exam you have submitted." : "Every exam you have started, including any still in progress.") + "</p></div></div>"
                + '<div id="summary"></div><form class="toolbar" id="filters">'
                + ui.field({ name: "examId", label: "Exam", type: "select", options: [["", "All exams"]] })
                + ui.field({ name: "status", label: "Result", type: "select", options: [["", "All"], ["PASSED", "Passed"], ["FAILED", "Failed"]] })
                + ui.field({ name: "from", label: "From", type: "date" }) + ui.field({ name: "to", label: "To", type: "date" })
                + ui.field({ name: "sort", label: "Sort by", type: "select", options: [["newest", "Newest"], ["oldest", "Oldest"], ["score", "Highest score"]] })
                + '<button type="button" class="btn" id="clear-btn">Clear</button></form><div class="card flush" id="list">' + ui.loading("Loading results...") + "</div>";
            const form = ctx.view.querySelector("#filters"), list = ctx.view.querySelector("#list"), summary = ctx.view.querySelector("#summary");
            let examsLoaded = false;

            const load = async function () {
                list.innerHTML = ui.loading("Loading results...");
                try {
                    let rows = (await api(resultsOnly ? "/api/student/results" : "/api/student/attempts", { query: ui.formValues(form) })).data;
                    if (!ctx.alive()) return;
                    if (resultsOnly) rows = rows.filter(function (r) { return r.status === "SUBMITTED"; });
                    if (!examsLoaded) {
                        examsLoaded = true;
                        const seen = {};
                        rows.forEach(function (r) {
                            if (seen[r.examId]) return; seen[r.examId] = true;
                            const o = document.createElement("option"); o.value = r.examId; o.textContent = r.examTitle; form.elements.examId.appendChild(o);
                        });
                        if (resultsOnly && rows.length) {
                            const best = rows.reduce(function (a, b) { return b.percentage > a.percentage ? b : a; });
                            const avg = rows.reduce(function (s, r) { return s + r.percentage; }, 0) / rows.length;
                            summary.innerHTML = '<div class="stat-grid" style="margin-bottom:8px">' + ui.stat("Results", rows.length, "award") + ui.stat("Passed", rows.filter(function (r) { return r.passed; }).length, "check")
                                + ui.stat("Average Percentage", fmt.pct(avg), "chart") + ui.stat("Best Score", fmt.pct(best.percentage), "target", best.examTitle) + "</div>";
                        }
                    }
                    const filtered = Object.values(ui.formValues(form)).some(function (v) { return v && v !== "newest"; });
                    list.innerHTML = rows.length ? '<div class="table-wrap"><table class="table"><thead><tr><th>Exam</th><th>Date</th><th>Score</th><th>Percentage</th><th>Status</th><th>Attempted Questions</th><th class="right">Action</th></tr></thead><tbody>'
                        + resultRows(rows, true) + "</tbody></table></div>"
                        : filtered ? ui.emptyState("No attempts match these filters", "Change or clear the filters to see more.", "", "search")
                            : ui.emptyState(resultsOnly ? "Your results will appear here after you complete an exam." : "You haven't attempted any exams yet.", "", '<a class="btn btn-primary" href="#/student/exams">Find an exam</a>', "award");
                } catch (error) {
                    if (ctx.alive() && error.code !== "SESSION_EXPIRED") {
                        list.innerHTML = ui.errorState(error.message, true);
                        list.querySelector("[data-retry]").addEventListener("click", load);
                    }
                }
            };
            form.addEventListener("submit", function (e) { e.preventDefault(); });
            form.querySelectorAll("select, input").forEach(function (el) { el.addEventListener("change", load); });
            ctx.view.querySelector("#clear-btn").addEventListener("click", function () { form.reset(); load(); });
            await load();
        };
    }
    ES.pages.studentAttempts = historyPage(false);
    ES.pages.studentResults = historyPage(true);
    ES.parts = { statGrid: statGrid, LETTERS: LETTERS };
})(window.ES);
