CampusDesk App

CampusDesk is an Android campus-management app backed by a Python REST API and SQLite. The Android project is in android/; the API is in backend/.

DOWNLOAD THE ANDROID APK
1. Deploy the backend using render.yaml (see DEPLOY THE BACKEND below).
2. In GitHub, open Settings > Secrets and variables > Actions > Variables. Add CAMPUSDESK_API_BASE_URL with the backend HTTPS base URL, without a trailing slash.
3. Open Actions > Build CampusDesk APK > Run workflow.
4. Open the completed run and download the CampusDesk-debug-apk artifact. Extract app-debug.apk and install it on your Android device. If Android blocks installation, allow installation from your browser or file manager.

This is a debug APK for testing, not a signed Play Store release. The build fails unless a valid HTTPS CAMPUSDESK_API_BASE_URL variable is set, so it will not silently use localhost.

DEPLOY THE BACKEND
1. In Render, create a new Blueprint from this GitHub repository and approve the settings in render.yaml.
2. The Blueprint creates a Docker web service and a 1 GB persistent disk mounted at /var/data. Persistent disks require a paid Render service; check current Render pricing before creating it.
3. Render generates CAMPUSDESK_ADMIN_PASSWORD. Retrieve it in the service's environment settings and sign in with username campusadmin. Store it securely; changing this environment variable later does not update an existing admin password in the database.
4. Check that the service's /health endpoint returns {"ok":true,"service":"CampusDesk API"}.
5. Add the service's HTTPS base URL as the GitHub Actions variable above, then run the APK workflow.

The hosted backend does not seed sample students or faculty. Create students after signing in as admin. Local development seeds sample records by default.

RUN LOCALLY
Requires Python 3.10+; the API uses only the Python standard library.

PowerShell:
  cd backend
  $env:CAMPUSDESK_ADMIN_PASSWORD = "choose-a-unique-local-password"
  python server.py

The API listens on http://127.0.0.1:8000; test http://127.0.0.1:8000/health. A fresh local database gets these demo accounts:
  Admin: campusadmin / Campus@2026 (override with CAMPUSDESK_ADMIN_PASSWORD)
  Student: student105 / student123
  Students student106 and student107 also use student123

Never use demo credentials or sample data on a public deployment. To skip seeding local demo users, set CAMPUSDESK_SEED_DEMO_USERS=false before the first run.

Open the android folder in Android Studio to build/run locally. The default API URL http://10.0.2.2:8000 works in the Android emulator. For a physical phone, set the API address in the app's Server settings to your computer's LAN address.

API ENDPOINTS
GET /health — health check
POST /api/login — returns a bearer token
GET /api/me — current signed-in user
GET /api/leaderboard — student leaderboard
GET /api/student/progress — signed-in student's progress
GET /api/students — admin-only student directory
POST /api/students — admin-only create student
PATCH /api/students/{roll_no}/cgpa — admin-only CGPA update
GET /api/faculty — admin-only faculty directory
POST /api/faculty — admin-only add faculty

SECURITY AND PERSISTENCE
The Render configuration uses HTTPS at the service edge, a generated initial admin password, no demo student records, and a persistent SQLite disk. API bearer tokens are held in memory and expire after seven days; service restarts invalidate active sessions. This starter app has no login rate limiting and is not security-audited. Do not use real student records or deploy as a production college system without a security review and operational safeguards.
