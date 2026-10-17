#!/usr/bin/env python3
"""CampusConnect REST API. Python standard library only; SQLite persistence."""
import base64, hashlib, hmac, json, os, secrets, sqlite3, time
from contextlib import contextmanager
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, unquote

HOST = os.environ.get("CAMPUSDESK_HOST", "0.0.0.0")
PORT = int(os.environ.get("PORT", os.environ.get("CAMPUSDESK_PORT", "8000")))
DB_PATH = os.environ.get("CAMPUSDESK_DB", os.path.join(os.path.dirname(__file__), "campusdesk.db"))
TOKEN_TTL = 60 * 60 * 24 * 7
TOKENS = {}
SEED_DEMO_USERS = os.environ.get("CAMPUSDESK_SEED_DEMO_USERS", "true").lower() in ("1", "true", "yes")

@contextmanager
def db():
    con = sqlite3.connect(DB_PATH)
    con.row_factory = sqlite3.Row
    try:
        yield con
        con.commit()
    except Exception:
        con.rollback()
        raise
    finally:
        con.close()

def hash_password(password, salt=None):
    salt = salt or secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 180_000)
    return base64.b64encode(salt).decode() + "$" + base64.b64encode(digest).decode()

def verify_password(password, stored):
    try:
        salt_b64, digest_b64 = stored.split("$", 1)
        salt = base64.b64decode(salt_b64)
        actual = hashlib.pbkdf2_hmac("sha256", password.encode(), salt, 180_000)
        return hmac.compare_digest(actual, base64.b64decode(digest_b64))
    except Exception:
        return False

def init_db():
    with db() as con:
        con.executescript("""
        CREATE TABLE IF NOT EXISTS users (
          id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL,
          password_hash TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('admin','student')),
          roll_no TEXT UNIQUE, full_name TEXT NOT NULL, course TEXT DEFAULT 'BS Computer Applications',
          semester INTEGER DEFAULT 1, cgpa REAL DEFAULT 0, attendance REAL DEFAULT 0,
          created_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS faculty (
          id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, department TEXT NOT NULL,
          email TEXT DEFAULT ''
        );
        """)
        if con.execute("SELECT COUNT(*) FROM users").fetchone()[0] == 0:
            con.execute("INSERT INTO users(username,password_hash,role,full_name) VALUES(?,?,?,?)",
                ("campusadmin", hash_password(os.environ.get("CAMPUSDESK_ADMIN_PASSWORD", "Campus@2026")), "admin", "Campus Administrator"))
            if SEED_DEMO_USERS:
                samples = [
                    ("student105", "student123", "105", "Aarav Sharma", 1, 8.6, 94),
                    ("student106", "student123", "106", "Diya Verma", 1, 8.9, 97),
                    ("student107", "student123", "107", "Kabir Singh", 1, 7.8, 88),
                ]
                for username, password, roll, name, sem, cgpa, attendance in samples:
                    con.execute("""INSERT INTO users(username,password_hash,role,roll_no,full_name,semester,cgpa,attendance)
                        VALUES(?,?,?,?,?,?,?,?)""",
                        (username, hash_password(password), "student", roll, name, sem, cgpa, attendance))
                con.executemany("INSERT INTO faculty(name,department,email) VALUES(?,?,?)", [
                    ("Dr. Meera Kapoor", "Computer Applications", "meera@example.edu"),
                    ("Prof. Rahul Khanna", "Mathematics", "rahul@example.edu"),
                ])

def public_student(row):
    return {"roll_no": row["roll_no"], "full_name": row["full_name"], "course": row["course"],
            "semester": row["semester"], "cgpa": row["cgpa"], "attendance": row["attendance"]}

class Handler(BaseHTTPRequestHandler):
    server_version = "CampusConnectAPI/1.0"
    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args))

    def send_json(self, status, obj):
        data = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Access-Control-Allow-Origin", os.environ.get("CAMPUSDESK_CORS", "*"))
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS")
        self.end_headers()
        self.wfile.write(data)

    def body(self):
        length = min(int(self.headers.get("Content-Length", "0")), 1_000_000)
        raw = self.rfile.read(length) if length else b"{}"
        try: return json.loads(raw.decode("utf-8"))
        except Exception: raise ValueError("Request body must be valid JSON")

    def current_user(self):
        header = self.headers.get("Authorization", "")
        token = header[7:] if header.startswith("Bearer ") else ""
        item = TOKENS.get(token)
        if not item or item["expires"] < time.time():
            TOKENS.pop(token, None)
            return None
        return item["user"]

    def require(self, role=None):
        user = self.current_user()
        if not user: self.send_json(401, {"error":"Sign in required"}); return None
        if role and user["role"] != role:
            self.send_json(403, {"error":"Insufficient permissions"}); return None
        return user

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", os.environ.get("CAMPUSDESK_CORS", "*"))
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS")
        self.end_headers()

    def do_GET(self):
        path = urlparse(self.path).path.rstrip("/") or "/"
        if path == "/health":
            return self.send_json(200, {"ok":True,"service":"CampusConnect API"})
        if path == "/api/leaderboard":
            with db() as con:
                rows = con.execute("SELECT * FROM users WHERE role='student' ORDER BY cgpa DESC, attendance DESC").fetchall()
            return self.send_json(200, {"students":[public_student(r) for r in rows]})
        if path == "/api/me":
            user = self.require()
            if not user: return
            with db() as con:
                row = con.execute("SELECT * FROM users WHERE id=?", (user["id"],)).fetchone()
            return self.send_json(200, {"user":{"username":row["username"],"role":row["role"],"full_name":row["full_name"],
                "roll_no":row["roll_no"],"course":row["course"],"semester":row["semester"]}})
        if path == "/api/student/progress":
            user = self.require("student")
            if not user: return
            with db() as con:
                row = con.execute("SELECT * FROM users WHERE id=?", (user["id"],)).fetchone()
            return self.send_json(200, {"student":public_student(row)})
        if path == "/api/students":
            if not self.require("admin"): return
            with db() as con:
                rows = con.execute("SELECT * FROM users WHERE role='student' ORDER BY roll_no").fetchall()
            return self.send_json(200, {"students":[dict(public_student(r), username=r["username"]) for r in rows]})
        if path == "/api/faculty":
            if not self.require("admin"): return
            with db() as con:
                rows = con.execute("SELECT id,name,department,email FROM faculty ORDER BY name").fetchall()
            return self.send_json(200, {"faculty":[dict(r) for r in rows]})
        return self.send_json(404, {"error":"Endpoint not found"})

    def do_POST(self):
        path = urlparse(self.path).path.rstrip("/")
        try: payload = self.body()
        except ValueError as e: return self.send_json(400, {"error":str(e)})
        if path == "/api/login":
            username = str(payload.get("username","")).strip()
            password = str(payload.get("password",""))
            if not username or not password: return self.send_json(400, {"error":"Username and password are required"})
            with db() as con:
                row = con.execute("SELECT * FROM users WHERE username=?", (username,)).fetchone()
            if not row or not verify_password(password, row["password_hash"]):
                return self.send_json(401, {"error":"Invalid username or password"})
            token = secrets.token_urlsafe(32)
            user = {"id":row["id"],"role":row["role"],"username":row["username"],"full_name":row["full_name"]}
            TOKENS[token] = {"user":user,"expires":time.time()+TOKEN_TTL}
            return self.send_json(200, {"token":token,"expires_in":TOKEN_TTL,"user":user})
        if path == "/api/students":
            admin = self.require("admin")
            if not admin: return
            name = str(payload.get("full_name","")).strip()
            roll = str(payload.get("roll_no","")).strip()
            password = str(payload.get("password",""))
            username = str(payload.get("username", "student"+roll)).strip()
            try:
                semester = int(payload.get("semester",1))
                cgpa = float(payload.get("cgpa",0))
                attendance = float(payload.get("attendance",0))
            except (ValueError, TypeError):
                return self.send_json(400, {"error":"Semester, CGPA and attendance must be numbers"})
            if not name or not roll or len(password) < 8:
                return self.send_json(400, {"error":"Name, roll number and password (8+ chars) are required"})
            if not (1 <= semester <= 12 and 0 <= cgpa <= 10 and 0 <= attendance <= 100):
                return self.send_json(400, {"error":"Semester 1-12, CGPA 0-10, attendance 0-100"})
            try:
                with db() as con:
                    con.execute("""INSERT INTO users(username,password_hash,role,roll_no,full_name,semester,cgpa,attendance)
                        VALUES(?,?, 'student', ?,?,?,?,?)""",
                        (username,hash_password(password),roll,name,semester,cgpa,attendance))
                return self.send_json(201, {"ok":True,"message":"Student created","username":username})
            except sqlite3.IntegrityError:
                return self.send_json(409, {"error":"Username or roll number already exists"})
        if path == "/api/faculty":
            if not self.require("admin"): return
            name = str(payload.get("name","")).strip()
            department = str(payload.get("department","")).strip()
            email = str(payload.get("email","")).strip()
            if not name or not department: return self.send_json(400, {"error":"Name and department are required"})
            with db() as con:
                cur = con.execute("INSERT INTO faculty(name,department,email) VALUES(?,?,?)",(name,department,email))
            return self.send_json(201, {"ok":True,"id":cur.lastrowid})
        return self.send_json(404, {"error":"Endpoint not found"})

    def do_PATCH(self):
        path = urlparse(self.path).path.rstrip("/")
        prefix = "/api/students/"
        if not path.startswith(prefix) or not path.endswith("/cgpa"):
            return self.send_json(404, {"error":"Endpoint not found"})
        if not self.require("admin"): return
        roll = unquote(path[len(prefix):-len("/cgpa")])
        try: payload = self.body(); cgpa = float(payload.get("cgpa"))
        except (ValueError, TypeError) as e: return self.send_json(400, {"error":"CGPA must be a number"})
        if not 0 <= cgpa <= 10: return self.send_json(400, {"error":"CGPA must be between 0 and 10"})
        with db() as con:
            cur = con.execute("UPDATE users SET cgpa=? WHERE roll_no=? AND role='student'",(cgpa,roll))
        if not cur.rowcount: return self.send_json(404, {"error":"Student roll number not found"})
        return self.send_json(200, {"ok":True,"roll_no":roll,"cgpa":cgpa})

if __name__ == "__main__":
    init_db()
    print(f"CampusConnect API listening on http://{HOST}:{PORT}")
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
