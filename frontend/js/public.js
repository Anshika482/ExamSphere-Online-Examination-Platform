/* Public pages: landing + login, registration, email verification, resend, forgot and reset password. */
(function (ES) {
    "use strict";
    const ui = ES.ui, esc = ES.esc, api = ES.api, router = ES.router, fmt = ES.fmt, icon = ui.icon;

    function publicFrame(inner, plain) {
        return '<div class="public ' + (plain ? "plain" : "") + '"><header class="pub-nav">' + ui.brand("#/")
            + (plain ? "" : '<nav class="pub-links" aria-label="Sections"><button type="button" data-scroll="learn">Learn</button><button type="button" data-scroll="assess">Assess</button><button type="button" data-scroll="grow">Grow</button></nav>')
            + '<div class="btn-row"><a class="btn btn-ghost" href="#/login">Login</a><a class="btn btn-primary" href="#/register/student">Register</a></div></header>'
            + '<main class="pub-main" id="main">' + inner + "</main>"
            + '<footer class="pub-foot"><span>' + icon("cap") + "Built for Education</span><span>" + icon("activity") + "Powered by Technology</span><span>" + icon("target") + "Designed for Growth</span></footer></div>";
    }

    function takeNotice() {
        const notice = sessionStorage.getItem("examsphere.notice");
        if (notice) sessionStorage.removeItem("examsphere.notice");
        return notice;
    }

    // ------------------------------------------------------------------ landing / login
    function loginForm() {
        return '<form class="auth-form" id="login-form"><div class="form-alert" role="alert"></div>'
            + ui.field({ name: "email", label: "Email Address", type: "email", required: true, placeholder: "you@example.com", attrs: 'autocomplete="username"' })
            + ui.field({ name: "password", label: "Password", type: "password", required: true, placeholder: "Your password", attrs: 'autocomplete="current-password"' })
            + '<div class="auth-links"><label class="check"><input type="checkbox" name="remember"> Remember me</label><a href="#/forgot-password">Forgot Password?</a></div>'
            + '<button type="submit" class="btn btn-primary btn-lg btn-block">Login</button></form>'
            + '<details class="demo-box"><summary>Use a demo account</summary><p class="small muted">These seeded accounts are already verified. Pick one to fill in the form.</p><div class="btn-row">'
            + '<button type="button" class="btn btn-sm" data-demo="student@examsphere.demo|Student@123">Student</button>'
            + '<button type="button" class="btn btn-sm" data-demo="instructor@examsphere.demo|Instructor@123">Instructor</button>'
            + '<button type="button" class="btn btn-sm" data-demo="admin@examsphere.demo|Admin@123">Admin</button></div></details>'
            + '<p class="auth-foot">New to ExamSphere? <a href="#/register/student">Register as a student</a> or <a href="#/register/instructor">as an instructor</a></p>';
    }

    function wireLogin(root) {
        const form = root.querySelector("#login-form");
        ui.wirePasswordToggles(form);
        const notice = takeNotice();
        if (notice) ui.formAlert(form, notice, "info");
        root.querySelectorAll("[data-demo]").forEach(function (button) {
            button.addEventListener("click", function () {
                const parts = button.getAttribute("data-demo").split("|");
                form.elements.email.value = parts[0];
                form.elements.password.value = parts[1];
                form.querySelector('button[type="submit"]').focus();
            });
        });
        ui.onSubmit(form, "Signing in...", async function (values) {
            const errors = ui.validate(values, { email: [ui.rule.required("Email"), ui.rule.email], password: [ui.rule.required("Password")] });
            if (Object.keys(errors).length) { ui.showErrors(form, errors); return; }
            try {
                const response = await api("/api/auth/login", { method: "POST", body: { email: values.email, password: values.password } });
                ES.session.save(response.data, values.remember);
                const returnTo = sessionStorage.getItem("examsphere.returnTo");
                sessionStorage.removeItem("examsphere.returnTo");
                ui.toast("Welcome back, " + response.data.user.fullName + ".");
                const prefix = "/" + response.data.user.role.toLowerCase() + "/";
                router.go(returnTo && returnTo.indexOf(prefix) === 0 ? returnTo : router.home(response.data.user));
            } catch (error) {
                if (error.code === "EMAIL_NOT_VERIFIED") {
                    const alert = form.querySelector(".form-alert");
                    alert.className = "form-alert warning";
                    alert.innerHTML = esc(error.message) + ' <a href="#/resend-verification?email=' + encodeURIComponent(values.email) + '">Resend verification email</a>';
                    return;
                }
                throw error;
            }
        });
    }

    ES.pages.landing = function (ctx) {
        const roleCard = function (iconName, title, items) {
            return '<div class="role-card"><div class="ic">' + icon(iconName) + "</div><h3>" + title + "</h3><ul>" + items.map(function (i) { return "<li>" + i + "</li>"; }).join("") + "</ul></div>";
        };
        ctx.view.innerHTML = publicFrame(
            '<section class="hero"><div><span class="hero-badge"><i></i>3-Role Examination Platform</span>'
            + "<h1>ExamSphere</h1><p class=\"tagline\">Smarter Examinations. Brighter Futures.</p>"
            + '<p class="desc">A modern, secure and intelligent examination platform for students, instructors and administrators.</p>'
            + '<div class="role-cards">'
            + roleCard("cap", "For Students", ["Take exams", "Track progress", "Achieve goals"])
            + roleCard("edit", "For Instructors", ["Create exams", "Manage questions", "View performance"])
            + roleCard("shield", "For Administrators", ["Manage users", "Monitor activity", "Ensure security"])
            + "</div></div>"
            + '<div class="card card-glass login-card" id="login"><div><h2>Welcome back</h2><p class="muted">Log in to continue to your dashboard.</p></div>' + loginForm() + "</div></section>"
            + '<section class="pillars" aria-label="How ExamSphere works">'
            + '<article class="pillar" id="learn"><h2>Learn</h2><p>Students find an exam, read the instructions and start when they are ready.</p><ol><li>Browse and search published exams</li><li>Answer at your own pace inside the time limit</li><li>Mark questions to come back to</li></ol></article>'
            + '<article class="pillar" id="assess"><h2>Assess</h2><p>Instructors build multiple-choice exams and the platform marks every attempt the moment it is submitted.</p><ol><li>Write questions and set marks</li><li>Publish now or schedule a window</li><li>Scores and pass or fail are calculated on the server</li></ol></article>'
            + '<article class="pillar" id="grow"><h2>Grow</h2><p>Everyone sees what the results mean: students review answers, instructors see which questions were hard.</p><ol><li>Answer review after submission</li><li>Question-by-question accuracy</li><li>Platform reports for administrators</li></ol></article>'
            + "</section>");
        wireLogin(ctx.view);
        ctx.view.querySelectorAll("[data-scroll]").forEach(function (button) {
            button.addEventListener("click", function () {
                const target = document.getElementById(button.getAttribute("data-scroll"));
                target.scrollIntoView({ behavior: "smooth", block: "center" });
                target.classList.add("flash");
                setTimeout(function () { target.classList.remove("flash"); }, 1400);
            });
        });
        if (ctx.path === "/login") {
            const email = ctx.view.querySelector('#login-form input[name="email"]');
            if (email) email.focus({ preventScroll: true });
        }
    };

    // ------------------------------------------------------------------ registration
    function registerPage(ctx, role) {
        const student = role === "student";
        const fields = student
            ? ui.field({ name: "college", label: "College / Institution", required: true })
            + ui.field({ name: "course", label: "Course / Program", required: true, placeholder: "e.g. B.Tech" })
            + ui.field({ name: "branch", label: "Branch / Department", required: true, placeholder: "e.g. Computer Science" })
            + ui.field({ name: "yearSemester", label: "Year / Semester", required: true, placeholder: "e.g. 4th Year / 7th Sem" })
            + ui.field({ name: "studentId", label: "Student ID / Enrollment Number", required: true, className: "span-2" })
            : ui.field({ name: "institution", label: "Institution", required: true })
            + ui.field({ name: "department", label: "Department", required: true })
            + ui.field({ name: "employeeId", label: "Employee / Faculty ID", required: true })
            + ui.field({ name: "designation", label: "Designation", required: true, placeholder: "e.g. Assistant Professor" });

        ctx.view.innerHTML = publicFrame('<div class="auth-wrap wide"><div class="card card-glass auth-card" id="register-card">'
            + "<div><h1>" + (student ? "Create your student account" : "Apply as an instructor") + '</h1><p class="lede">'
            + (student ? "You will verify your email before your first login." : "After you verify your email, an administrator reviews your application before you can log in.") + "</p></div>"
            + '<div class="btn-row" role="tablist" aria-label="Account type"><a class="btn btn-sm ' + (student ? "btn-primary" : "") + '" href="#/register/student" role="tab" aria-selected="' + student + '">Student</a>'
            + '<a class="btn btn-sm ' + (student ? "" : "btn-primary") + '" href="#/register/instructor" role="tab" aria-selected="' + !student + '">Instructor</a></div>'
            + '<form class="auth-form" id="register-form"><div class="form-alert" role="alert"></div><div class="form-grid">'
            + ui.field({ name: "fullName", label: "Full Name", required: true, attrs: 'autocomplete="name" maxlength="100"' })
            + ui.field({ name: "email", label: student ? "Email Address" : "Official Email", type: "email", required: true, attrs: 'autocomplete="email"' })
            + ui.field({ name: "phone", label: "Phone Number", type: "tel", required: true, placeholder: "+91 98765 43210", attrs: 'autocomplete="tel"' })
            + '<div class="field" data-field="profilePhoto"><label for="photo-input">Profile Photo <span class="muted">(optional)</span></label><input class="input" id="photo-input" type="file" accept="image/png,image/jpeg,image/webp"><span class="field-error" role="alert"></span></div>'
            + ui.field({ name: "password", label: "Password", type: "password", required: true, hint: "At least 8 characters, with a letter and a number.", attrs: 'autocomplete="new-password"' })
            + ui.field({ name: "confirmPassword", label: "Confirm Password", type: "password", required: true, attrs: 'autocomplete="new-password"' })
            + fields
            + '<div class="field span-2" data-field="acceptTerms"><label class="check"><input type="checkbox" name="acceptTerms"> <span>I agree to the ExamSphere Terms &amp; Conditions and to my exam data being stored for evaluation.</span></label><span class="field-error" role="alert"></span></div>'
            + '</div><button type="submit" class="btn btn-primary btn-lg btn-block">Create account</button></form>'
            + '<p class="auth-foot">Already have an account? <a href="#/login">Log in</a></p></div></div>', true);

        const form = ctx.view.querySelector("#register-form");
        ui.wirePasswordToggles(form);
        ui.onSubmit(form, "Creating account...", async function (values) {
            const required = ui.rule.required;
            const rules = {
                fullName: [required("Full name"), function (v) { return v.length < 2 ? "Full name is too short" : null; }],
                email: [required("Email"), ui.rule.email],
                phone: [required("Phone number"), ui.rule.phone],
                password: [required("Password"), ui.rule.password],
                confirmPassword: [required("Confirm password"), ui.rule.matches("password", "Passwords do not match")],
                acceptTerms: [function (v) { return v ? null : "You must accept the Terms & Conditions"; }]
            };
            (student ? ["college", "course", "branch", "yearSemester", "studentId"] : ["institution", "department", "employeeId", "designation"])
                .forEach(function (name) { rules[name] = [required("This field")]; });
            const errors = ui.validate(values, rules);
            let photo = null;
            try {
                photo = await ui.readPhoto(form.querySelector("#photo-input").files[0]);
            } catch (e) {
                errors.profilePhoto = e.message;
            }
            if (Object.keys(errors).length) { ui.showErrors(form, errors); ui.formAlert(form, "Please fix the highlighted fields."); return; }

            const body = Object.assign({}, values, { profilePhoto: photo });
            const response = await api("/api/auth/register/" + role, { method: "POST", body: body });
            showCheckEmail(ctx, response.data, student);
        });
    }

    /** "Check your inbox" panel with the resend button and its cooldown. */
    function showCheckEmail(ctx, data, student) {
        const card = ctx.view.querySelector("#register-card");
        card.innerHTML = '<div class="state success" style="padding:8px 0"><div class="state-icon">' + icon("mail") + "</div><h1>Verify your email</h1>"
            + "<p>We sent a verification link to <b>" + esc(data.email) + "</b>. Open it within 30 minutes to activate your account."
            + (student ? "" : " After that, an administrator will review your instructor application.") + "</p></div>"
            + (data.emailDeliveryConfigured ? "" : '<div class="form-alert warning">This server has no email service (SMTP) configured, so the message could not actually be sent. Ask the administrator to configure email, or use a demo account to explore the platform.</div>')
            + '<div class="form-alert" id="resend-alert" role="status"></div>'
            + '<div class="btn-row" style="justify-content:center"><button type="button" class="btn" id="resend-btn" disabled>Resend email</button><a class="btn btn-primary" href="#/login">Continue to Login</a></div>'
            + '<p class="auth-foot" id="resend-timer"></p>';
        wireResend(ctx, card, data.email, data.resendCooldownSeconds || 60);
    }

    function wireResend(ctx, root, email, initialCooldown) {
        const button = root.querySelector("#resend-btn"), timer = root.querySelector("#resend-timer"), alert = root.querySelector("#resend-alert");
        let stop = null;
        const start = function (seconds) {
            button.disabled = true;
            if (stop) stop();
            stop = ui.countdown(seconds, function (left) {
                timer.textContent = left > 0 ? "Resend available in " + fmt.clock(left) : "";
            }, function () { button.disabled = false; });
        };
        ctx.onLeave(function () { if (stop) stop(); });
        if (initialCooldown > 0) start(initialCooldown); else button.disabled = false;
        button.addEventListener("click", async function () {
            ui.setBusy(button, true, "Sending...");
            alert.className = "form-alert"; alert.textContent = "";
            try {
                const response = await api("/api/auth/resend-verification", { method: "POST", body: { email: typeof email === "function" ? email() : email } });
                ui.setBusy(button, false);
                alert.className = "form-alert success"; alert.textContent = "Verification email sent.";
                ui.toast("Verification email sent.");
                start(response.data.cooldownSeconds || 60);
            } catch (error) {
                ui.setBusy(button, false);
                alert.className = "form-alert error"; alert.textContent = error.message;
                if (error.retryAfterSeconds) start(Math.min(error.retryAfterSeconds, 3600));
            }
        });
        return start;
    }

    ES.pages.registerStudent = function (ctx) { registerPage(ctx, "student"); };
    ES.pages.registerInstructor = function (ctx) { registerPage(ctx, "instructor"); };

    // ------------------------------------------------------------------ verify email
    ES.pages.verifyEmail = async function (ctx) {
        const panel = function (kind, iconName, title, text, actions) {
            ctx.view.innerHTML = publicFrame('<div class="auth-wrap"><div class="card card-glass auth-card"><div class="state ' + kind + '" style="padding:8px 0"><div class="state-icon">' + icon(iconName) + "</div><h1>" + esc(title) + "</h1><p>" + esc(text) + '</p></div><div class="btn-row" style="justify-content:center">' + actions + "</div></div></div>", true);
        };
        const login = '<a class="btn btn-primary" href="#/login">Continue to Login</a>';
        const resend = '<a class="btn btn-primary" href="#/resend-verification">Request a new link</a><a class="btn" href="#/login">Back to Login</a>';

        if (!ctx.query.token) {
            return panel("error", "alert", "Invalid verification link", "This link is incomplete. Open the link from your email again, or request a new one.", resend);
        }
        ctx.view.innerHTML = publicFrame('<div class="auth-wrap"><div class="card card-glass auth-card"><h1 style="text-align:center">Verify your email</h1>' + ui.loading("Checking your verification link...") + "</div></div>", true);
        try {
            const response = await api("/api/auth/verify-email", { method: "POST", body: { token: ctx.query.token } });
            if (!ctx.alive()) return;
            const pending = response.data.approvalRequired;
            if (response.data.status === "ALREADY_VERIFIED") {
                panel("success", "check", "Email already verified", pending ? "Your email is verified. Your instructor account is waiting for admin approval." : "This email address has already been verified. You can log in.", login);
            } else {
                panel("success", "check", "Email Verified Successfully", pending ? "Thank you. An administrator will now review your instructor application; you can log in once it is approved." : "Your account is active. You can log in now.", login);
            }
        } catch (error) {
            if (!ctx.alive()) return;
            if (error.code === "TOKEN_EXPIRED") panel("error", "clock", "This link has expired", "Verification links are valid for 30 minutes. Request a new one to continue.", resend);
            else if (error.code === "TOKEN_USED") panel("error", "alert", "This link was already used", "This link has been used or was replaced by a newer email. Use the latest email we sent you, or request a new link.", resend);
            else if (error.code === "INVALID_TOKEN") panel("error", "alert", "Invalid verification link", "We could not recognise this link. Copy the full link from your email, or request a new one.", resend);
            else panel("error", "alert", "Verification failed", error.message, resend);
        }
    };

    // ------------------------------------------------------------------ resend verification
    ES.pages.resendVerification = function (ctx) {
        ctx.view.innerHTML = publicFrame('<div class="auth-wrap"><div class="card card-glass auth-card"><div><h1>Resend Verification Email</h1><p class="lede">Enter the email you registered with and we will send a fresh link. Older links stop working.</p></div>'
            + '<form class="auth-form" id="resend-form">' + ui.field({ name: "email", label: "Email Address", type: "email", required: true, value: ctx.query.email || "", attrs: 'autocomplete="email"' })
            + '<div class="form-alert" id="resend-alert" role="status"></div><button type="button" class="btn btn-primary btn-lg btn-block" id="resend-btn">Send verification email</button></form>'
            + '<p class="auth-foot countdown" id="resend-timer" aria-live="polite"></p><p class="auth-foot"><a href="#/login">Back to Login</a></p></div></div>', true);
        const form = ctx.view.querySelector("#resend-form");
        form.addEventListener("submit", function (e) { e.preventDefault(); ctx.view.querySelector("#resend-btn").click(); });
        const button = ctx.view.querySelector("#resend-btn");
        // validate before the shared resend handler runs
        button.addEventListener("click", function (event) {
            ui.clearErrors(form);
            const value = form.elements.email.value.trim();
            const message = !value ? "Email is required" : ui.rule.email(value);
            if (message) { event.stopImmediatePropagation(); ui.showErrors(form, { email: message }); }
        });
        wireResend(ctx, ctx.view, function () { return form.elements.email.value.trim(); }, 0);
    };

    // ------------------------------------------------------------------ forgot / reset password
    ES.pages.forgotPassword = function (ctx) {
        ctx.view.innerHTML = publicFrame('<div class="auth-wrap"><div class="card card-glass auth-card"><div><h1>Forgot your password?</h1><p class="lede">Enter your email and we will send you a link to choose a new password.</p></div>'
            + '<form class="auth-form" id="forgot-form"><div class="form-alert" role="alert"></div>' + ui.field({ name: "email", label: "Email Address", type: "email", required: true, attrs: 'autocomplete="email"' })
            + '<button type="submit" class="btn btn-primary btn-lg btn-block">Send reset link</button></form><p class="auth-foot"><a href="#/login">Back to Login</a></p></div></div>', true);
        const form = ctx.view.querySelector("#forgot-form");
        ui.onSubmit(form, "Sending...", async function (values) {
            const errors = ui.validate(values, { email: [ui.rule.required("Email"), ui.rule.email] });
            if (Object.keys(errors).length) { ui.showErrors(form, errors); return; }
            const response = await api("/api/auth/forgot-password", { method: "POST", body: { email: values.email } });
            ui.formAlert(form, response.message + " The link is valid for 30 minutes.", "success");
            form.reset();
        });
    };

    ES.pages.resetPassword = async function (ctx) {
        const frame = function (inner) { ctx.view.innerHTML = publicFrame('<div class="auth-wrap"><div class="card card-glass auth-card">' + inner + "</div></div>", true); };
        const failed = function (title, text) {
            frame('<div class="state error" style="padding:8px 0"><div class="state-icon">' + icon("alert") + "</div><h1>" + esc(title) + "</h1><p>" + esc(text) + '</p></div><div class="btn-row" style="justify-content:center"><a class="btn btn-primary" href="#/forgot-password">Request a new link</a><a class="btn" href="#/login">Back to Login</a></div>');
        };
        const explain = function (error) {
            if (error.code === "TOKEN_EXPIRED") failed("This link has expired", "Password reset links are valid for 30 minutes. Request a new one to continue.");
            else if (error.code === "TOKEN_USED") failed("This link was already used", "Each reset link works once. Request a new one if you still need to change your password.");
            else if (error.code === "INVALID_TOKEN") failed("Invalid reset link", "We could not recognise this link. Copy the full link from your email, or request a new one.");
            else failed("Could not open the reset page", error.message);
        };
        const token = ctx.query.token;
        if (!token) return failed("Invalid reset link", "This link is incomplete. Open the link from your email again, or request a new one.");
        frame("<h1>Reset your password</h1>" + ui.loading("Checking your reset link..."));
        try {
            await api("/api/auth/reset-password/validate", { method: "POST", body: { token: token } });
        } catch (error) {
            if (ctx.alive()) explain(error);
            return;
        }
        if (!ctx.alive()) return;
        frame('<div><h1>Choose a new password</h1><p class="lede">Use at least 8 characters with a letter and a number.</p></div><form class="auth-form" id="reset-form"><div class="form-alert" role="alert"></div>'
            + ui.field({ name: "newPassword", label: "New Password", type: "password", required: true, attrs: 'autocomplete="new-password"' })
            + ui.field({ name: "confirmPassword", label: "Confirm Password", type: "password", required: true, attrs: 'autocomplete="new-password"' })
            + '<button type="submit" class="btn btn-primary btn-lg btn-block">Update password</button></form>');
        const form = ctx.view.querySelector("#reset-form");
        ui.wirePasswordToggles(form);
        ui.onSubmit(form, "Updating...", async function (values) {
            const errors = ui.validate(values, {
                newPassword: [ui.rule.required("New password"), ui.rule.password],
                confirmPassword: [ui.rule.required("Confirm password"), ui.rule.matches("newPassword", "Passwords do not match")]
            });
            if (Object.keys(errors).length) { ui.showErrors(form, errors); return; }
            try {
                const response = await api("/api/auth/reset-password", { method: "POST", body: { token: token, newPassword: values.newPassword, confirmPassword: values.confirmPassword } });
                sessionStorage.setItem("examsphere.notice", response.message);
                router.go("/login");
            } catch (error) {
                if (["TOKEN_EXPIRED", "TOKEN_USED", "INVALID_TOKEN"].indexOf(error.code) >= 0) explain(error); else throw error;
            }
        });
    };

    ES.pages.notFound = function () {
        document.getElementById("app").innerHTML = publicFrame('<div class="auth-wrap"><div class="card card-glass auth-card"><div class="state"><div class="state-icon">' + icon("search") + '</div><h1>Page not found</h1><p>The page you are looking for does not exist or has moved.</p><a class="btn btn-primary" href="#' + router.home(ES.session.user) + '">Go to ' + (ES.session.user ? "dashboard" : "login") + "</a></div></div></div>", true);
        ES.layout.current = null;
    };
})(window.ES);
