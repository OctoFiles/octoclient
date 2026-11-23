import json, os, re, sqlite3, threading, time
from urllib.parse import parse_qs

DB = os.environ.get("OCTOCLIENT_API_DB", os.path.expanduser("~/octoclient_loadouts.db"))
TOKEN = os.environ.get("OCTOCLIENT_API_TOKEN", "").strip()
U = re.compile(r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
START = time.time()
LOCK = threading.Lock()
CON = sqlite3.connect(DB, check_same_thread=False)
CON.execute("PRAGMA journal_mode=WAL")
CON.execute("CREATE TABLE IF NOT EXISTS loadouts (uuid TEXT PRIMARY KEY, name TEXT, loadout TEXT, updated REAL)")
CON.commit()


def out(start, status, obj):
    raw = json.dumps(obj).encode()
    start(str(status) + (" OK" if status == 200 else " ERROR"),
          [("Content-Type", "application/json"), ("Content-Length", str(len(raw)))])
    return [raw]


def application(env, start):
    m = env.get("REQUEST_METHOD", "GET")
    p = env.get("PATH_INFO", "/")
    if p == "/health":
        with LOCK:
            n = CON.execute("SELECT COUNT(*) FROM loadouts").fetchone()[0]
        return out(start, 200, {"ok": True, "entries": n, "uptime": round(time.time() - START, 1)})
    if TOKEN and env.get("HTTP_AUTHORIZATION", "") != "Bearer " + TOKEN:
        return out(start, 401, {"error": "unauthorized"})
    if p == "/v1/loadouts" and m == "GET":
        ids = [i.strip().lower() for i in parse_qs(env.get("QUERY_STRING", "")).get("uuid", [])]
        ids = [i for i in ids if U.match(i)][:100]
        if not ids:
            return out(start, 200, {"loadouts": {}})
        q = "SELECT uuid, loadout FROM loadouts WHERE uuid IN (%s)" % ",".join("?" * len(ids))
        with LOCK:
            rows = CON.execute(q, ids).fetchall()
        return out(start, 200, {"loadouts": {r[0]: r[1] for r in rows}})
    if p == "/v1/loadouts" and m == "POST":
        try:
            n = int(env.get("CONTENT_LENGTH") or 0)
            b = json.loads(env["wsgi.input"].read(n).decode())
        except Exception:
            return out(start, 400, {"error": "invalid json"})
        uid = str(b.get("uuid", "")).strip().lower()
        lo = str(b.get("loadout", ""))
        if not U.match(uid) or len(lo.split("|")) not in (9, 10):
            return out(start, 400, {"error": "invalid loadout"})
        with LOCK:
            CON.execute("INSERT INTO loadouts VALUES (?,?,?,?) ON CONFLICT(uuid) DO UPDATE "
                        "SET name=excluded.name, loadout=excluded.loadout, updated=excluded.updated",
                        (uid, str(b.get("name", ""))[:32], lo, time.time()))
            CON.commit()
        return out(start, 200, {"ok": True})
    if p.startswith("/v1/loadouts/") and m == "DELETE":
        uid = p.split("/")[-1].strip().lower()
        if not U.match(uid):
            return out(start, 400, {"error": "invalid uuid"})
        with LOCK:
            CON.execute("DELETE FROM loadouts WHERE uuid=?", (uid,))
            CON.commit()
        return out(start, 200, {"ok": True})
    return out(start, 404, {"error": "not found"})


if __name__ == "__main__":
    from wsgiref.simple_server import make_server

    with make_server("127.0.0.1", 8767, application) as server:
        print("http://127.0.0.1:8767")
        server.serve_forever()
