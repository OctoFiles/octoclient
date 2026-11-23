"""Static server for the OctoClient web preview.

Serves the folder it lives in and accepts POST /save, which stores the body as
shot.png next to this script. The save endpoint exists so the preview can dump
its own framebuffer (the desktop screenshot tool is unavailable in this
environment).
"""

import os
import http.server
import socketserver

ROOT = os.path.dirname(os.path.abspath(__file__))


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=ROOT, **kwargs)

    def end_headers(self):
        # always serve fresh files while iterating on the preview
        self.send_header("Cache-Control", "no-store, max-age=0")
        super().end_headers()

    def do_POST(self):
        if self.path != "/save":
            self.send_error(404)
            return
        length = int(self.headers.get("Content-Length", 0))
        data = self.rfile.read(length)
        with open(os.path.join(ROOT, "shot.png"), "wb") as f:
            f.write(data)
        body = f"saved {len(data)} bytes".encode()
        self.send_response(200)
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("127.0.0.1", 8123), Handler) as httpd:
        print("serving", ROOT, "on http://127.0.0.1:8123")
        httpd.serve_forever()
