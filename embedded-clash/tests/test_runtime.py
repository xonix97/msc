"""Real local HTTP tests against the vendored ClashFLAC app, without provider accounts."""
import http.client
import importlib
import json
from pathlib import Path
import secrets
import shutil
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src/main/python"))


class RuntimeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.directory = tempfile.TemporaryDirectory(prefix="msc-clash-test-")
        cls.runtime = importlib.import_module("clash_runtime")
        cls.token = secrets.token_hex(32)
        cls.ffmpeg = shutil.which("ffmpeg")
        if not cls.ffmpeg:
            raise RuntimeError("A real FFmpeg executable is required for this test")
        cls.port = json.loads(cls.runtime.start(cls.directory.name, cls.token, cls.ffmpeg))["port"]

    @classmethod
    def tearDownClass(cls):
        cls.runtime.stop()
        cls.directory.cleanup()

    def request(self, path, token=True, origin=None):
        headers = {"X-Msc-Local-Token": self.token if token is True else token} if token else {}
        if origin:
            headers["Origin"] = origin
        # Explicit loopback IPC, never route a local session token through an outbound proxy.
        connection = http.client.HTTPConnection("127.0.0.1", self.port, timeout=8)
        try:
            connection.request("GET", path, headers=headers)
            response = connection.getresponse()
            return response.status, json.loads(response.read())
        finally:
            connection.close()

    def test_upstream_health_really_runs(self):
        status, body = self.request("/health")
        self.assertEqual(200, status)
        self.assertEqual("ok", body["status"])

    def test_unauthenticated_loopback_access_is_rejected(self):
        self.assertEqual(403, self.request("/health", token=False)[0])

    def test_wrong_token_is_rejected(self):
        self.assertEqual(403, self.request("/health", token="different-session-token")[0])

    def test_non_ascii_token_is_rejected_without_server_error(self):
        self.assertEqual(403, self.request("/health", token="invalid-\u00e9")[0])

    def test_wrong_origin_is_rejected(self):
        self.assertEqual(403, self.request("/health", origin="https://untrusted.invalid")[0])

    def test_static_path_cannot_escape_to_private_configuration(self):
        self.assertEqual(400, self.request("/%2e%2e/config/credentials.bin")[0])

    def test_status_does_not_pretend_an_account_is_authenticated(self):
        status, body = self.request("/_msc/status")
        self.assertEqual(200, status)
        self.assertEqual("ClashFLAC", body["service"])
        self.assertTrue(body["ffmpeg_present"])
        self.assertFalse(body["credential_files_present"])

    def test_real_catalog_requires_real_credentials(self):
        status, body = self.request("/api/search?q=msc-test&limit=1")
        self.assertIn(status, (500, 503))
        self.assertIn("credential", str(body).lower())

    def test_real_tidal_route_reports_signed_out(self):
        status, body = self.request("/api/tidal/auth/status")
        self.assertEqual(200, status)
        self.assertFalse(body["authenticated"])

    def test_start_does_not_create_duplicate_servers(self):
        result = json.loads(self.runtime.start(self.directory.name, self.token, self.ffmpeg))
        self.assertEqual(self.port, result["port"])

    def test_another_session_cannot_claim_the_running_server(self):
        with self.assertRaises(ValueError):
            self.runtime.start(self.directory.name, secrets.token_hex(32), self.ffmpeg)
        self.assertEqual(200, self.request("/health")[0])


if __name__ == "__main__":
    unittest.main()
