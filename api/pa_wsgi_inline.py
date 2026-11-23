#!/usr/bin/env python3
"""Self-contained OctoClient cosmetics API for PythonAnywhere.

Nothing to import, nothing to upload: paste this whole file into the WSGI
configuration file of your PythonAnywhere web app (Web tab -> "WSGI
configuration file"), replacing everything that is there, then Reload.

Client config:
    { "enabled": true, "url": "https://USERNAME.pythonanywhere.com",
      "token": "", "pollSeconds": 5 }

Optional token: environment variable OCTOCLIENT_API_TOKEN.
The database is stored as ~/octoclient_loadouts.db.
"""

import json
import os
import re
import sqlite3
import threading
import time
from urllib.parse import parse_qs

UUID_RE = re.compile(r"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
                     r"[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
# Keep accepting optional fields added by newer clients, including nametag
# settings, while preserving the original nine-field minimum.
LOADOUT_RE = re.compile(r"^[^|\r\n]{0,120}(\|[^|\r\n]{0,120}){8,31}$")
DB_PATH = os.environ.get("OCTOCLIENT_API_DB",
                         os.path.expanduser("~/octoclient_loadouts.db"))
TOKEN = os.environ.get("OCTOCLIENT_API_TOKEN", "").strip()
START = time.time()
LOCK = threading.Lock()


def connect():
    connection = sqlite3.connect(DB_PATH, timeout=10.0)
    connection.execute("PRAGMA journal_mode=WAL")
    return connection


def init_db():
    with LOCK:
        connection = connect()
        try:
            connection.execute(
                "CREATE TABLE IF NOT EXISTS loadouts ("
                "uuid TEXT PRIMARY KEY, name TEXT NOT NULL, "
                "loadout TEXT NOT NULL, updated REAL NOT NULL)")
            connection.commit()
        finally:
            connection.close()


def store(entry_uuid, name, loadout):
    with LOCK:
        connection = connect()
        try:
            connection.execute(
                "INSERT INTO loadouts (uuid, name, loadout, updated) VALUES (?,?,?,?) "
                "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, "
                "loadout=excluded.loadout, updated=excluded.updated",
                (entry_uuid, name, loadout, time.time()))
            connection.commit()
        finally:
            connection.close()


def lookup(uuids):
    if not uuids:
        return {}
    marks = ",".join("?" for _ in uuids)
    with LOCK:
        connection = connect()
        try:
            rows = connection.execute(
                "SELECT uuid, loadout FROM loadouts WHERE uuid IN (%s)" % marks,
                uuids).fetchall()
        finally:
            connection.close()
    return {row[0]: row[1] for row in rows}


def count():
    with LOCK:
        connection = connect()
        try:
            return connection.execute("SELECT COUNT(*) FROM loadouts").fetchone()[0]
        finally:
            connection.close()


init_db()


def respond(start_response, status, payload):
    raw = json.dumps(payload, separators=(",", ":")).encode("utf-8")
    start_response(str(status) + (" OK" if status == 200 else " ERROR"),
                   [("Content-Type", "application/json"),
                    ("Content-Length", str(len(raw))),
                    ("Cache-Control", "no-store")])
    return [raw]


def application(environ, start_response):
    method = (environ.get("REQUEST_METHOD") or "GET").upper()
    path = environ.get("PATH_INFO") or "/"
    if path != "/" and path.endswith("/"):
        path = path.rstrip("/")

    if path == "/health":
        return respond(start_response, 200, {"ok": True, "entries": count(),
                                             "uptime": round(time.time() - START, 1)})

    if TOKEN and environ.get("HTTP_AUTHORIZATION", "") != "Bearer " + TOKEN:
        return respond(start_response, 401, {"error": "unauthorized"})

    if path == "/v1/loadouts" and method == "GET":
        wanted = [v.strip().lower()
                  for v in parse_qs(environ.get("QUERY_STRING", "")).get("uuid", [])]
        wanted = [v for v in wanted if UUID_RE.match(v)][:100]
        return respond(start_response, 200, {"loadouts": lookup(wanted)})

    if path == "/v1/loadouts" and method == "POST":
        try:
            length = int(environ.get("CONTENT_LENGTH") or 0)
        except ValueError:
            length = 0
        raw = environ["wsgi.input"].read(length) if 0 < length <= 4096 else b""
        try:
            body = json.loads(raw.decode("utf-8"))
        except Exception:
            return respond(start_response, 400, {"error": "invalid json"})
        entry_uuid = str(body.get("uuid", "")).strip().lower()
        name = str(body.get("name", ""))[:32]
        loadout = str(body.get("loadout", ""))
        if not UUID_RE.match(entry_uuid):
            return respond(start_response, 400, {"error": "invalid uuid"})
        if len(loadout) > 1024 or not LOADOUT_RE.match(loadout):
            return respond(start_response, 400, {"error": "invalid loadout"})
        store(entry_uuid, name, loadout)
        return respond(start_response, 200, {"ok": True})

    if path.startswith("/v1/loadouts/") and method == "DELETE":
        entry_uuid = path[len("/v1/loadouts/"):].strip().lower()
        if not UUID_RE.match(entry_uuid):
            return respond(start_response, 400, {"error": "invalid uuid"})
        with LOCK:
            connection = connect()
            try:
                removed = connection.execute(
                    "DELETE FROM loadouts WHERE uuid = ?", (entry_uuid,)).rowcount > 0
                connection.commit()
            finally:
                connection.close()
        return respond(start_response, 200, {"ok": removed})

    return respond(start_response, 404, {"error": "not found"})


if __name__ == "__main__":
    from wsgiref.simple_server import make_server

    with make_server("127.0.0.1", 8766, application) as server:
        print("http://127.0.0.1:8766")
        server.serve_forever()
