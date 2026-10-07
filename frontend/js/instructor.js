/* Instructor pages: dashboard, exams, exam form, question manager, results and analytics. */
(function (ES) {
    "use strict";
    const ui = ES.ui, esc = ES.esc, api = ES.api, router = ES.router, fmt = ES.fmt, icon = ui.icon;
    const LETTERS = ES.parts.LETTERS;

    function attemptRows(rows, showExam) {
        return rows.map(function (r) {
            return '<tr><td><div class="cell-title">' + esc(r.studentName) + '</div><div class="cell-sub">' + esc(r.studentEmail) + "</div></td>"
                + (showExam ? "<td>" + esc(r.examTitle) + "</td>" : "")
                + '<td class="nowrap">' + esc(fmt.dateTime(r.submittedAt)) + '</td><td class="num">' + r.marksObtained + " / " + r.totalMarks + '</td><td class="num">' + fmt.pct(r.percentage) + "</td><td>" + ui.passBadge(r) + "</td>"
                + '<td class="num">' + r.attempted + '</td><td class="num">' + r.correct + '</td><td class="num">' + r.incorrect + "</td></tr>";
        }).join("");
    }
    const ATTEMPT_HEAD = function (showExam) {
        return "<thead><tr><th>Student</th>" + (showExam ? "<th>Exam</th>" : "") + "<th>Attempt Date</th><th>Score</th><th>Percentage</th><th>Status</th><th>Attempted</th><th>Correct</th><th>Incorrect</th></tr></thead>";
    };
    ES.parts.attemptRows = attemptRows;
    ES.parts.attemptHead = ATTEMPT_HEAD;

    // ------------------------------------------------------------------ dashboard
    ES.pages.instructorDashboard = async function (ctx) {
        const d = (await api("/api/instructor/dashboard")).data;
        if (!ctx.alive()) return;
        const has = d.totalAttempts > 0;
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Welcome back, ' + esc(ctx.user.fullName) + "</h1><p>" + (d.totalExams ? "Your exams and how students are doing in them." : "Create your first exam to get started.") + '</p></div><a class="btn btn-primary" href="#/instructor/exams/new">' + icon("plus") + "Create Exam</a></div>"
            + '<div class="stat-grid">' + ui.stat("Total Exams", d.totalExams, "book", null, true) + ui.stat("Published Exams", d.publishedExams, "send") + ui.stat("Draft Exams", d.draftExams, "edit")
            + ui.stat("Total Attempts", d.totalAttempts, "users") + ui.stat("Average Score", has ? fmt.pct(d.averagePercentage) : "-", "chart") + ui.stat("Pass Rate", has ? fmt.pct(d.passRate) : "-", "award") + "</div>"
            + '<div class="grid-main"><div class="card"><div class="card-head"><div><h2>Performance overview</h2><p>Average percentage per exam</p></div><a class="btn btn-sm" href="#/instructor/analytics">Analytics</a></div>'
            + ui.barChart(d.examPerformance.map(function (s) { return { label: s.title, value: s.averagePercentage }; }), { max: 100, format: fmt.pct, label: "Average percentage per exam", emptyTitle: "No attempts yet", emptyText: "Averages appear once students submit your exams." }) + "</div>"
            + '<div class="card"><div class="card-head"><div><h2>Recent activity</h2><p>Latest submissions</p></div></div>'
            + (d.recentAttempts.length ? '<ul class="activity">' + d.recentAttempts.map(function (r) {
                return '<li><span class="a-ic">' + icon(r.passed ? "check" : "x") + "</span><span><b>" + esc(r.studentName) + "</b><small>" + esc(r.examTitle) + " &middot; " + r.marksObtained + " / " + r.totalMarks + "</small></span><time>" + esc(fmt.ago(r.submittedAt)) + "</time></li>";
            }).join("") + "</ul>" : ui.emptyState("No submissions yet", "Student submissions will be listed here.", "", "inbox")) + "</div></div>"
            + '<div class="card flush"><div class="card-head"><div><h2>Recent exams</h2></div><a class="btn btn-sm" href="#/instructor/exams">All exams</a></div>'
            + (d.recentExams.length ? '<div class="table-wrap"><table class="table"><thead><tr><th>Exam</th><th>Status</th><th>Questions</th><th>Total marks</th><th>Attempts</th><th class="right">Action</th></tr></thead><tbody>'
                + d.recentExams.map(function (e) {
                    return '<tr><td><div class="cell-title">' + esc(e.title) + '</div><div class="cell-sub">' + esc(e.category) + "</div></td><td>" + ui.badge(e.status) + '</td><td class="num">' + e.questionCount + '</td><td class="num">' + e.totalMarks + '</td><td class="num">' + (e.attempts || 0)
                        + '</td><td><div class="actions"><a class="btn btn-sm" href="#/instructor/exam/' + e.id + '/questions">Manage</a></div></td></tr>';
                }).join("") + "</tbody></table></div>" : ui.emptyState("No exams yet", "Create an exam, add questions and publish it for your students.", '<a class="btn btn-primary" href="#/instructor/exams/new">Create Exam</a>', "book")) + "</div>";
    };

    // ------------------------------------------------------------------ my exams (also used as the question-management picker)
    async function examAction(ctx, action, exam, reload) {
        const config = {
            publish: ["Publish this exam?", "Students will be able to see and take \"" + exam.title + "\". Questions are locked once it is published.", "Publish", false, "POST", "/publish", "Publishing..."],
            unpublish: ["Move back to draft?", "\"" + exam.title + "\" will be hidden from students so you can edit it. This only works while nobody has started it.", "Move to draft", false, "POST", "/unpublish", "Updating..."],
            close: ["Close this exam?", "No new attempts will be allowed for \"" + exam.title + "\". Existing results are kept. This cannot be undone.", "Close exam", true, "POST", "/close", "Closing..."],
            remove: ["Delete this draft?", "\"" + exam.title + "\" and all of its questions will be permanently deleted.", "Delete exam", true, "DELETE", "", "Deleting..."]
        }[action];
        const ok = await ui.confirm({ title: config[0], message: config[1], confirmText: config[2], danger: config[3] });
        if (!ok) return;
        try {
            const response = await api("/api/exams/" + exam.id + config[5], { method: config[4] });
            ui.toast(response.message);
            reload();
        } catch (error) {
            if (error.code !== "SESSION_EXPIRED") ui.toast(error.message, "error");
        }
    }

    function examsPage(picker) {
        return async function (ctx) {
            ctx.view.innerHTML = '<div class="page-head"><div><h1>' + (picker ? "Question Management" : "My Exams") + "</h1><p>" + (picker ? "Choose an exam to add, edit, reorder or delete its questions. Questions can be changed while the exam is a draft." : "Drafts are private to you. Published exams are visible to students.") + '</p></div><a class="btn btn-primary" href="#/instructor/exams/new">' + icon("plus") + "Create Exam</a></div>"
                + '<form class="toolbar" id="filters" role="search"><div class="field grow"><label for="q">Search</label><div class="search-input">' + icon("search") + '<input class="input" id="q" name="q" type="search" placeholder="Title or category" value="' + esc(ctx.query.q || "") + '"></div></div>'
                + ui.field({ name: "status", label: "Status", type: "select", options: [["", "All statuses"], ["DRAFT", "Draft"], ["PUBLISHED", "Published"], ["CLOSED", "Closed"]] }) + '</form><div class="card flush" id="list">' + ui.loading("Loading exams...") + "</div>";
            const form = ctx.view.querySelector("#filters"), list = ctx.view.querySelector("#list");
            let exams = [];
            const load = async function () {
                list.innerHTML = ui.loading("Loading exams...");
                try {
                    exams = (await api("/api/instructor/exams", { query: ui.formValues(form) })).data;
                    if (!ctx.alive()) return;
                    const v = ui.formValues(form);
                    if (!exams.length) {
                        list.innerHTML = v.q || v.status ? ui.emptyState("No exams match your search", "Try a different word or status.", "", "search")
                            : ui.emptyState("No exams yet", "Create an exam, add questions and publish it for your students.", '<a class="btn btn-primary" href="#/instructor/exams/new">Create Exam</a>', "book");
                        return;
                    }
                    list.innerHTML = '<div class="table-wrap"><table class="table"><thead><tr><th>Exam</th><th>Status</th><th>Duration</th><th>Questions</th><th>Marks (pass)</th><th>Attempts</th><th>Created</th><th class="right">Actions</th></tr></thead><tbody>'
                        + exams.map(function (e, i) {
                            const draft = e.status === "DRAFT", published = e.status === "PUBLISHED";
                            let actions = '<a class="btn btn-sm ' + (picker ? "btn-primary" : "") + '" href="#/instructor/exam/' + e.id + '/questions">' + (draft ? "Questions" : "View questions") + "</a>";
                            if (!picker) {
                                if (draft) actions += '<a class="btn btn-sm" href="#/instructor/exam/' + e.id + '/edit">Edit</a><button type="button" class="btn btn-sm btn-primary" data-act="publish" data-i="' + i + '">Publish</button><button type="button" class="btn btn-sm btn-danger" data-act="remove" data-i="' + i + '">Delete</button>';
                                if (published) actions += (e.attempts ? "" : '<button type="button" class="btn btn-sm" data-act="unpublish" data-i="' + i + '">Unpublish</button>') + '<button type="button" class="btn btn-sm btn-danger" data-act="close" data-i="' + i + '">Close</button>';
                                if (!draft) actions += '<a class="btn btn-sm" href="#/instructor/results?examId=' + e.id + '">Results</a><a class="btn btn-sm" href="#/instructor/analytics?examId=' + e.id + '">Analytics</a>';
                            }
                            return '<tr><td><div class="cell-title">' + esc(e.title) + '</div><div class="cell-sub">' + esc(e.category) + (e.availability === "UPCOMING" ? " &middot; opens " + esc(fmt.dateTime(e.scheduledAt)) : "") + "</div></td><td>" + ui.badge(e.status) + (e.availability === "UPCOMING" ? " " + ui.badge("UPCOMING", "Scheduled") : "") + '</td><td class="num nowrap">' + fmt.duration(e.durationMinutes)
                                + '</td><td class="num">' + e.questionCount + '</td><td class="num">' + e.totalMarks + " (" + e.passMarks + ')</td><td class="num">' + (e.attempts || 0) + '</td><td class="nowrap">' + esc(fmt.date(e.createdAt)) + '</td><td><div class="actions">' + actions + "</div></td></tr>";
                        }).join("") + "</tbody></table></div>";
                } catch (error) {
                    if (ctx.alive() && error.code !== "SESSION_EXPIRED") {
                        list.innerHTML = ui.errorState(error.message, true);
                        list.querySelector("[data-retry]").addEventListener("click", load);
                    }
                }
            };
            list.addEventListener("click", function (event) {
                const button = event.target.closest("[data-act]");
                if (button) examAction(ctx, button.getAttribute("data-act"), exams[Number(button.getAttribute("data-i"))], load);
            });
            form.addEventListener("submit", function (e) { e.preventDefault(); load(); });
            form.querySelector("#q").addEventListener("input", ES.debounce(load, 300));
            form.elements.status.addEventListener("change", load);
            await load();
        };
    }
    ES.pages.instructorExams = examsPage(false);
    ES.pages.instructorQuestions = examsPage(true);

    // ------------------------------------------------------------------ create / edit exam
    ES.pages.instructorExamForm = async function (ctx) {
        const editing = !!ctx.params.id;
        let detail = null;
        if (editing) {
            detail = (await api("/api/exams/" + ctx.params.id)).data;
            if (!ctx.alive()) return;
        }
        const e = detail ? detail.exam : {};
        if (detail && !detail.editable) {
            ctx.view.innerHTML = ui.emptyState("This exam can no longer be edited", "Only draft exams can be changed. This exam is " + fmt.label(e.status).toLowerCase() + ".", '<a class="btn btn-primary" href="#/instructor/exams">Back to My Exams</a>', "lock");
            return;
        }
        ctx.view.innerHTML = '<div><a class="crumb" href="#/instructor/exams">' + icon("arrowLeft") + 'My Exams</a><div class="page-head"><div><h1>' + (editing ? "Edit Exam" : "Create Exam") + "</h1><p>" + (editing ? "Change the details of this draft." : "The exam starts as a draft. You add questions next, then publish.") + "</p></div></div></div>"
            + '<form class="card stack" id="exam-form"><div class="form-alert" role="alert"></div><div class="form-grid">'
            + ui.field({ name: "title", label: "Exam Title", value: e.title, required: true, className: "span-2", attrs: 'maxlength="150"', placeholder: "e.g. Java Fundamentals" })
            + ui.field({ name: "description", label: "Description", type: "textarea", value: e.description, className: "span-2", attrs: 'maxlength="2000"', placeholder: "What this exam covers (optional)" })
            + ui.field({ name: "category", label: "Category", value: e.category, required: true, placeholder: "e.g. Java, DBMS, OOP", attrs: 'list="category-list" maxlength="80"' })
            + '<datalist id="category-list"><option value="Java"><option value="DBMS"><option value="OOP"><option value="Web Development"><option value="General Aptitude"><option value="Data Structures"></datalist>'
            + ui.field({ name: "durationMinutes", label: "Duration (minutes)", type: "number", value: e.durationMinutes || 30, required: true, attrs: 'min="1" max="600" step="1"' })
            + ui.field({ name: "passMarks", label: "Pass Marks", type: "number", value: e.passMarks === undefined ? 0 : e.passMarks, required: true, attrs: 'min="0" step="1"', hint: editing && e.questionCount ? "Total marks are currently " + e.totalMarks + " (calculated from the questions)." : "Total marks are calculated automatically from the questions you add." })
            + '<div class="field"><span class="label">Total Marks</span><input class="input" value="' + (e.totalMarks || 0) + '" readonly aria-label="Total marks (calculated)"><span class="field-hint">Calculated by the system.</span></div>'
            + ui.field({ name: "scheduledAt", label: "Scheduled Start", type: "datetime-local", value: fmt.inputDateTime(e.scheduledAt), hint: "Optional. Leave empty to open as soon as it is published." })
            + ui.field({ name: "closesAt", label: "Scheduled End / Closing Time", type: "datetime-local", value: fmt.inputDateTime(e.closesAt), hint: "Optional. No new attempts after this time." })
            + '</div><div class="btn-row"><button type="submit" class="btn btn-primary">' + (editing ? "Save Changes" : "Create exam and add questions") + '</button><a class="btn" href="#/instructor/exams">Cancel</a></div></form>';
        const form = ctx.view.querySelector("#exam-form");
        ui.onSubmit(form, editing ? "Saving..." : "Creating...", async function (values) {
            const errors = ui.validate(values, {
                title: [ui.rule.required("Exam title")], category: [ui.rule.required("Category")],
                durationMinutes: [ui.rule.required("Duration"), ui.rule.min(1, "Duration must be at least 1 minute")],
                passMarks: [ui.rule.required("Pass marks"), ui.rule.min(0, "Pass marks cannot be negative")],
                closesAt: [function (v, all) { return v && all.scheduledAt && v <= all.scheduledAt ? "Closing time must be after the scheduled start" : null; }]
            });
            if (Object.keys(errors).length) { ui.showErrors(form, errors); return; }
            const body = { title: values.title, description: values.description || null, category: values.category, durationMinutes: Number(values.durationMinutes), passMarks: Number(values.passMarks), scheduledAt: values.scheduledAt || null, closesAt: values.closesAt || null };
            const response = await api(editing ? "/api/exams/" + ctx.params.id : "/api/exams", { method: editing ? "PUT" : "POST", body: body });
            ui.toast(response.message);
            router.go("/instructor/exam/" + response.data.exam.id + "/questions");
        });
    };

    // ------------------------------------------------------------------ question manager
    function questionModal(existing) {
        const q = existing || { text: "", marks: 1, options: [{ text: "", correct: true }, { text: "", correct: false }, { text: "", correct: false }, { text: "", correct: false }] };
        const row = function (o, i) {
            return '<div class="opt-edit" data-opt><input type="radio" name="correct" value="' + i + '"' + (o.correct ? " checked" : "") + ' aria-label="Mark option as correct"><input class="input" data-opt-text value="' + esc(o.text) + '" maxlength="1000" placeholder="Option text" aria-label="Option text"><button type="button" class="btn btn-ghost btn-icon btn-sm" data-remove aria-label="Remove option">' + icon("x") + "</button></div>";
        };
        return ui.openModal({
            title: existing ? "Edit question" : "Add question", wide: true,
            body: '<form id="q-form" class="stack"><div class="form-alert" role="alert"></div>'
                + ui.field({ name: "text", label: "Question Text", type: "textarea", value: q.text, required: true, attrs: 'maxlength="2000" autofocus' })
                + ui.field({ name: "marks", label: "Marks", type: "number", value: q.marks, required: true, attrs: 'min="1" max="100" step="1" style="max-width:140px"' })
                + '<div class="field" data-field="options"><span class="label">Options <span class="muted">(select the one correct answer)</span></span><div class="stack" id="opt-list" style="gap:8px">' + q.options.map(row).join("") + '</div><span class="field-error" role="alert"></span>'
                + '<div><button type="button" class="btn btn-sm" id="add-opt">' + icon("plus") + "Add option</button></div></div></form>",
            footer: '<button type="button" class="btn" data-result="cancel">Cancel</button><button type="submit" form="q-form" class="btn btn-primary">' + (existing ? "Save question" : "Add question") + "</button>",
            onOpen: function (node) {
                const list = node.querySelector("#opt-list"), add = node.querySelector("#add-opt");
                const sync = function () {
                    const rows = list.querySelectorAll("[data-opt]");
                    rows.forEach(function (r, i) {
                        r.querySelector('input[type="radio"]').value = i;
                        r.querySelector("[data-opt-text]").placeholder = "Option " + LETTERS[i];
                        r.querySelector("[data-remove]").disabled = rows.length <= 2;
                    });
                    add.disabled = rows.length >= 6;
                };
                add.addEventListener("click", function () { list.insertAdjacentHTML("beforeend", row({ text: "", correct: false }, 0)); sync(); });
                list.addEventListener("click", function (event) {
                    const remove = event.target.closest("[data-remove]");
                    if (remove) { remove.closest("[data-opt]").remove(); sync(); }
                });
                sync();
            }
        });
    }

    ES.pages.instructorQuestionManager = async function (ctx) {
        const examId = ctx.params.id;
        const render = async function () {
            const detail = (await api("/api/exams/" + examId)).data;
            if (!ctx.alive()) return;
            const e = detail.exam, questions = detail.questions, editable = detail.editable;
            ctx.view.innerHTML = '<div><a class="crumb" href="#/instructor/exams">' + icon("arrowLeft") + 'My Exams</a><div class="page-head"><div><h1>' + esc(e.title) + "</h1><p>" + esc(e.category) + " &middot; " + fmt.duration(e.durationMinutes) + " &middot; pass at " + e.passMarks + " of " + e.totalMarks + ' marks</p></div><div class="btn-row">' + ui.badge(e.status)
                + (editable ? '<a class="btn" href="#/instructor/exam/' + e.id + '/edit">' + icon("edit") + 'Edit details</a><button type="button" class="btn btn-primary" id="publish-btn">' + icon("send") + "Publish</button>" : '<a class="btn" href="#/instructor/results?examId=' + e.id + '">Results</a><a class="btn" href="#/instructor/analytics?examId=' + e.id + '">Analytics</a>') + "</div></div></div>"
                + (editable ? "" : '<div class="form-alert info">This exam is ' + fmt.label(e.status).toLowerCase() + ", so its questions are locked. That keeps existing attempts and results consistent." + (e.status === "PUBLISHED" && !detail.hasAttempts ? " Nobody has started it yet, so you can still move it back to draft from My Exams." : "") + "</div>")
                + '<div class="stat-grid">' + ui.stat("Questions", e.questionCount, "list") + ui.stat("Total Marks", e.totalMarks, "target", "calculated automatically") + ui.stat("Pass Marks", e.passMarks, "award") + ui.stat("Duration", fmt.duration(e.durationMinutes), "clock") + "</div>"
                + '<div class="card flush"><div class="card-head"><div><h2>Questions</h2></div>' + (editable ? '<button type="button" class="btn btn-primary btn-sm" id="add-btn">' + icon("plus") + "Add question</button>" : "") + "</div>"
                + (questions.length ? '<div class="table-wrap"><table class="table"><thead><tr><th>#</th><th>Question</th><th>Marks</th><th>Correct Option</th>' + (editable ? '<th class="right">Actions</th>' : "") + "</tr></thead><tbody>"
                    + questions.map(function (q, i) {
                        const ci = q.options.findIndex(function (o) { return o.correct; });
                        return '<tr><td class="num">' + (i + 1) + '</td><td><div class="q-row-text">' + esc(q.text) + '</div><div class="cell-sub">' + q.options.length + " options</div></td><td class=\"num\">" + q.marks + "</td><td>" + (ci >= 0 ? "<b>" + LETTERS[ci] + ".</b> " + esc(q.options[ci].text) : "-") + "</td>"
                            + (editable ? '<td><div class="actions"><button type="button" class="btn btn-sm btn-icon" data-move="-1" data-i="' + i + '" aria-label="Move question up"' + (i === 0 ? " disabled" : "") + ">" + icon("up") + '</button><button type="button" class="btn btn-sm btn-icon" data-move="1" data-i="' + i + '" aria-label="Move question down"' + (i === questions.length - 1 ? " disabled" : "") + ">" + icon("down")
                                + '</button><button type="button" class="btn btn-sm" data-edit="' + i + '">Edit</button><button type="button" class="btn btn-sm btn-danger" data-delete="' + i + '">Delete</button></div></td>' : "") + "</tr>";
                    }).join("") + "</tbody></table></div>"
                    : ui.emptyState("No questions yet", editable ? "Add at least one question before you publish this exam." : "This exam has no questions.", editable ? '<button type="button" class="btn btn-primary" id="add-btn-2">Add the first question</button>' : "", "list")) + "</div>";

            const reload = function () { render().catch(function (error) { if (error.code !== "SESSION_EXPIRED") ui.toast(error.message, "error"); }); };

            const edit = async function (existing) {
                const pending = questionModal(existing);
                const form = document.getElementById("q-form");
                ui.onSubmit(form, "Saving...", async function (values) {
                    const rows = Array.prototype.slice.call(form.querySelectorAll("[data-opt]"));
                    const options = rows.map(function (r) { return { text: r.querySelector("[data-opt-text]").value.trim(), correct: r.querySelector('input[type="radio"]').checked }; });
                    const errors = ui.validate(values, { text: [ui.rule.required("Question text")], marks: [ui.rule.required("Marks"), ui.rule.min(1, "Marks must be at least 1")] });
                    if (options.some(function (o) { return !o.text; })) errors.options = "Fill in every option, or remove the empty ones";
                    else if (options.filter(function (o) { return o.correct; }).length !== 1) errors.options = "Select exactly one correct option";
                    if (Object.keys(errors).length) { ui.showErrors(form, errors); return; }
                    const body = { text: values.text, marks: Number(values.marks), options: options };
                    const response = existing ? await api("/api/questions/" + existing.id, { method: "PUT", body: body }) : await api("/api/exams/" + examId + "/questions", { method: "POST", body: body });
                    ui.closeModal("saved");
                    ui.toast(response.message);
                    reload();
                });
                await pending;
            };

            ["#add-btn", "#add-btn-2"].forEach(function (sel) { const b = ctx.view.querySelector(sel); if (b) b.addEventListener("click", function () { edit(null); }); });
            ctx.view.querySelectorAll("[data-edit]").forEach(function (b) { b.addEventListener("click", function () { edit(questions[Number(b.getAttribute("data-edit"))]); }); });
            ctx.view.querySelectorAll("[data-delete]").forEach(function (b) {
                b.addEventListener("click", async function () {
                    const q = questions[Number(b.getAttribute("data-delete"))];
                    const ok = await ui.confirm({ title: "Delete this question?", message: "\"" + (q.text.length > 120 ? q.text.slice(0, 120) + "..." : q.text) + "\" will be removed from the exam.", confirmText: "Delete question", danger: true });
                    if (!ok) return;
                    try { const response = await api("/api/questions/" + q.id, { method: "DELETE" }); ui.toast(response.message); reload(); }
                    catch (error) { if (error.code !== "SESSION_EXPIRED") ui.toast(error.message, "error"); }
                });
            });
            ctx.view.querySelectorAll("[data-move]").forEach(function (b) {
                b.addEventListener("click", async function () {
                    const i = Number(b.getAttribute("data-i")), j = i + Number(b.getAttribute("data-move"));
                    const ids = questions.map(function (q) { return q.id; });
                    const moved = ids[i]; ids[i] = ids[j]; ids[j] = moved;
                    b.disabled = true;
                    try { await api("/api/exams/" + examId + "/questions/order", { method: "PUT", body: { questionIds: ids } }); reload(); }
                    catch (error) { b.disabled = false; if (error.code !== "SESSION_EXPIRED") ui.toast(error.message, "error"); }
                });
            });
            const publish = ctx.view.querySelector("#publish-btn");
            if (publish) publish.addEventListener("click", function () { examAction(ctx, "publish", e, reload); });
        };
        await render();
    };

    // ------------------------------------------------------------------ results
    ES.pages.instructorResults = async function (ctx) {
        const exams = (await api("/api/instructor/exams")).data;
        if (!ctx.alive()) return;
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Results</h1><p>Submitted attempts for your exams. Results are calculated by the system and cannot be edited.</p></div></div>'
            + '<form class="toolbar" id="filters">' + ui.field({ name: "examId", label: "Exam", type: "select", value: ctx.query.examId || "", options: [["", "All exams"]].concat(exams.map(function (e) { return [e.id, e.title]; })) })
            + '<div class="field grow"><label for="student">Student</label><div class="search-input">' + icon("search") + '<input class="input" id="student" name="student" type="search" placeholder="Name or email"></div></div>'
            + ui.field({ name: "status", label: "Pass / Fail", type: "select", options: [["", "All"], ["PASSED", "Passed"], ["FAILED", "Failed"]] })
            + ui.field({ name: "from", label: "From", type: "date" }) + ui.field({ name: "to", label: "To", type: "date" }) + '<button type="button" class="btn" id="clear-btn">Clear</button></form><div class="card flush" id="list"></div>';
        const form = ctx.view.querySelector("#filters"), list = ctx.view.querySelector("#list");
        const load = async function (page) {
            list.innerHTML = ui.loading("Loading results...");
            try {
                const data = (await api("/api/instructor/results", { query: Object.assign(ui.formValues(form), { page: page || 0, size: 10 }) })).data;
                if (!ctx.alive()) return;
                const filtered = Object.values(ui.formValues(form)).some(Boolean);
                list.innerHTML = data.content.length ? '<div class="table-wrap"><table class="table">' + ATTEMPT_HEAD(true) + "<tbody>" + attemptRows(data.content, true) + "</tbody></table></div>" + ui.pagination(data, load)
                    : filtered ? ui.emptyState("No results match these filters", "Change or clear the filters to see more.", "", "search") : ui.emptyState("No results yet", "Results appear here once students submit your published exams.", "", "award");
            } catch (error) {
                if (ctx.alive() && error.code !== "SESSION_EXPIRED") { list.innerHTML = ui.errorState(error.message, true); list.querySelector("[data-retry]").addEventListener("click", function () { load(page); }); }
            }
        };
        form.addEventListener("submit", function (e) { e.preventDefault(); load(0); });
        form.querySelectorAll("select, input[type=date]").forEach(function (el) { el.addEventListener("change", function () { load(0); }); });
        form.querySelector("#student").addEventListener("input", ES.debounce(function () { load(0); }, 300));
        ctx.view.querySelector("#clear-btn").addEventListener("click", function () { form.reset(); form.elements.examId.value = ""; load(0); });
        await load(0);
    };

    // ------------------------------------------------------------------ analytics
    function analyticsHtml(a) {
        const s = a.summary;
        if (!s.attempts) return ui.emptyState("No attempts for this exam yet", "Analytics are calculated from real submissions, so there is nothing to show until a student submits this exam.", "", "chart");
        return '<div class="stat-grid">' + ui.stat("Total Attempts", s.attempts, "users", null, true) + ui.stat("Average Score", fmt.num(s.averageScore) + " / " + s.totalMarks, "chart") + ui.stat("Highest Score", s.highestScore, "up") + ui.stat("Lowest Score", s.lowestScore, "down")
            + ui.stat("Pass Rate", fmt.pct(s.passRate), "check", s.passCount + " passed") + ui.stat("Fail Rate", fmt.pct(s.failRate), "x", s.failCount + " failed") + ui.stat("Average Percentage", fmt.pct(s.averagePercentage), "target") + "</div>"
            + '<div class="grid-2"><div class="card"><div class="card-head"><div><h2>Score distribution</h2><p>Number of attempts in each percentage band</p></div></div>' + ui.columnChart(a.distribution, { label: "Score distribution" }) + "</div>"
            + '<div class="card"><div class="card-head"><div><h2>Pass / fail</h2></div></div>' + ui.donutChart([{ label: "Passed", value: s.passCount, color: "var(--accent)" }, { label: "Failed", value: s.failCount, color: "var(--danger)" }], { centerLabel: "attempts" }) + "</div></div>"
            + '<div class="card flush"><div class="card-head"><div><h2>Question-wise performance</h2><p>Accuracy = correct answers out of the times the question was attempted</p></div></div><div class="table-wrap"><table class="table"><thead><tr><th>#</th><th>Question</th><th>Times Attempted</th><th>Correct</th><th>Incorrect</th><th>Accuracy</th></tr></thead><tbody>'
            + a.questions.map(function (q) {
                return '<tr><td class="num">Q' + q.number + '</td><td><div class="q-row-text">' + esc(q.text) + '</div></td><td class="num">' + q.timesAttempted + '</td><td class="num">' + q.correctCount + '</td><td class="num">' + q.incorrectCount + '</td><td><div style="display:flex;align-items:center;gap:10px"><span class="num" style="min-width:44px">' + fmt.pct(q.accuracy) + "</span>" + ui.meter(q.accuracy) + "</div></td></tr>";
            }).join("") + "</tbody></table></div></div>";
    }
    ES.parts.analyticsHtml = analyticsHtml;

    ES.pages.instructorAnalytics = async function (ctx) {
        const loaded = await Promise.all([api("/api/instructor/exams"), api("/api/instructor/analytics")]);
        if (!ctx.alive()) return;
        const exams = loaded[0].data.filter(function (e) { return e.status !== "DRAFT"; }), overview = loaded[1].data;
        if (!exams.length) {
            ctx.view.innerHTML = '<div class="page-head"><div><h1>Analytics</h1></div></div><div class="card">' + ui.emptyState("Nothing to analyse yet", "Publish an exam first. Analytics are built from your students' real submissions.", '<a class="btn btn-primary" href="#/instructor/exams">Go to My Exams</a>', "chart") + "</div>";
            return;
        }
        const withAttempts = exams.find(function (e) { return e.attempts > 0; });
        const selected = ctx.query.examId || (withAttempts || exams[0]).id;
        ctx.view.innerHTML = '<div class="page-head"><div><h1>Analytics</h1><p>Calculated live from submitted attempts.</p></div><form class="toolbar" style="margin:0">' + ui.field({ name: "examId", label: "Exam", type: "select", value: selected, options: exams.map(function (e) { return [e.id, e.title + " (" + (e.attempts || 0) + ")"]; }) }) + "</form></div>"
            + '<div class="card"><div class="card-head"><div><h2>All exams</h2><p>Average percentage and pass rate</p></div></div>' + ui.barChart(overview.map(function (s) { return { label: s.title, value: s.averagePercentage }; }), { max: 100, format: fmt.pct, label: "Average percentage per exam", emptyTitle: "No attempts yet", emptyText: "Averages appear once students submit your exams." }) + "</div>"
            + '<div class="stack" id="detail" style="gap:var(--s-5)"></div>';
        const detail = ctx.view.querySelector("#detail"), select = ctx.view.querySelector("select");
        const load = async function () {
            detail.innerHTML = ui.loading("Loading analytics...");
            try {
                const a = (await api("/api/instructor/exams/" + select.value + "/analytics")).data;
                if (ctx.alive()) detail.innerHTML = "<h2>" + esc(a.exam.title) + "</h2>" + analyticsHtml(a);
            } catch (error) {
                if (ctx.alive() && error.code !== "SESSION_EXPIRED") { detail.innerHTML = ui.errorState(error.message, true); detail.querySelector("[data-retry]").addEventListener("click", load); }
            }
        };
        select.addEventListener("change", load);
        await load();
    };
})(window.ES);
