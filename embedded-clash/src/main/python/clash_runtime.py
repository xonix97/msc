"""Run the vendored ClashFLAC ASGI app on a private authenticated loopback socket.

No credentials are bundled. This module deliberately does not create accounts,
device keys, or substitute fake catalog/playback responses.
"""
import hmac
import importlib
import json
import os
from contextlib import asynccontextmanager
from pathlib import Path
import socket
import sys
import threading
import time
from urllib.parse import unquote

_lock = threading.RLock()
_server = None
_thread = None
_socket = None
_port = None
_workdir = None
_session_token = None
_allowed_environment = {
    "TIDAL_CLIENT_ID", "TIDAL_CLIENT_SECRET", "TIDAL_WEB_CLIENT_TOKEN",
    "QOBUZ_APP_ID", "QOBUZ_APP_SECRET", "QOBUZ_USER_AUTH_TOKEN", "QOBUZ_USER_ID",
    "SPOTIFY_CLIENT_ID", "SPOTIFY_CLIENT_SECRET",
}


def start(files_dir, token, ffmpeg_path):
    global _server, _thread, _socket, _port, _workdir, _session_token
    if not isinstance(token, str) or len(token) < 32:
        raise ValueError("A private random session token is required")
    with _lock:
        if _thread is not None and _thread.is_alive():
            if not hmac.compare_digest(token.encode("utf-8"), _session_token):
                raise ValueError("The running engine belongs to another local session")
            return json.dumps({"port": _port, "running": bool(_server.started)})
        root = Path(files_dir).resolve()
        root.mkdir(parents=True, exist_ok=True)
        root.chmod(0o700)
        _workdir = root
        os.chdir(root)
        config = root / "embedded-env.json"
        if config.exists():
            values = json.loads(config.read_text(encoding="utf-8"))
            if not isinstance(values, dict):
                raise ValueError("Provider configuration must be a JSON object")
            for name, value in values.items():
                if name not in _allowed_environment or not isinstance(value, str):
                    raise ValueError("Unsupported provider configuration field")
                os.environ[name] = value
        ffmpeg = Path(ffmpeg_path).resolve()
        if not ffmpeg.is_file() or not os.access(ffmpeg, os.X_OK):
            raise RuntimeError("The Android-native FFmpeg executable is missing")
        os.environ["IMAGEIO_FFMPEG_EXE"] = str(ffmpeg)
        import clashflac
        vendor = Path(clashflac.__file__).resolve().parent
        if str(vendor) not in sys.path:
            sys.path.insert(0, str(vendor))
        os.environ["FRONTEND_DIR"] = str(vendor / "frontend" / "dist")
        backend = importlib.import_module("server")
        from fastapi import FastAPI, Request
        from fastapi.responses import JSONResponse
        import uvicorn

        @asynccontextmanager
        async def lifespan(app):
            # Mounted ASGI applications do not receive startup/shutdown automatically.
            async with backend.app.router.lifespan_context(backend.app):
                yield

        app = FastAPI(title="msc embedded ClashFLAC", docs_url=None, redoc_url=None,
                      openapi_url=None, lifespan=lifespan)
        listener = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        listener.bind(("127.0.0.1", 0))
        listener.listen(128)
        _port = listener.getsockname()[1]
        expected_origin = f"http://127.0.0.1:{_port}"

        @app.middleware("http")
        async def private_session(request: Request, call_next):
            supplied = request.headers.get("X-Msc-Local-Token", "")
            if not hmac.compare_digest(supplied.encode("utf-8"), token.encode("utf-8")):
                return JSONResponse({"detail": "Private local session required"}, status_code=403)
            origin = request.headers.get("Origin")
            if origin is not None and origin != expected_origin:
                return JSONResponse({"detail": "Origin not allowed"}, status_code=403)
            path = request.scope.get("raw_path", b"").decode("ascii", "ignore")
            for _ in range(3):
                path = unquote(path)
            if "\\" in path or "\x00" in path or ".." in path.split("/"):
                return JSONResponse({"detail": "Invalid path"}, status_code=400)
            return await call_next(request)

        @app.get("/_msc/status")
        async def status():
            # File presence is configuration evidence, not proof of provider authorization.
            return {
                "service": "ClashFLAC", "upstream_version": backend.app.version,
                "ffmpeg_present": True,
                "credential_files_present": any((root / p).is_file() for p in (
                    "config/credentials.bin", "config/tidal_tokens.json", "config/qobuz_tokens.json")),
            }

        # Mount the real upstream application, including its actual provider routes.
        app.mount("/", backend.app)
        config = uvicorn.Config(app, host="127.0.0.1", port=_port, loop="asyncio", http="h11",
                                ws="none", access_log=False, log_level="warning", timeout_graceful_shutdown=5)
        _server = uvicorn.Server(config)
        _session_token = token.encode("utf-8")
        _socket = listener
        _thread = threading.Thread(target=_server.run, kwargs={"sockets": [listener]}, name="msc-clashflac", daemon=True)
        _thread.start()
        deadline = time.monotonic() + 30
        while not _server.started and _thread.is_alive() and time.monotonic() < deadline:
            time.sleep(0.05)
        if not _server.started:
            _server.should_exit = True
            listener.close()
            raise RuntimeError("ClashFLAC did not reach HTTP readiness")
        return json.dumps({"port": _port, "running": True})


def stop():
    global _server, _thread, _socket, _port, _session_token
    with _lock:
        server, thread, listener = _server, _thread, _socket
        if server is not None:
            server.should_exit = True
    if thread is not None:
        thread.join(timeout=10)
        if thread.is_alive():
            raise RuntimeError("ClashFLAC has not stopped; its isolated Android process must be stopped")
    if listener is not None:
        listener.close()
    with _lock:
        _server = _thread = _socket = _port = _session_token = None
