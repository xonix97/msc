import os
from pathlib import Path
from typing import Dict, Any

class TidalConfig:
    API_URL = "https://api.tidal.com/v1"
    AUTH_URL = "https://auth.tidal.com/v1/oauth2"
    
    # Optional token supplied by the user's own provider configuration.
    WEB_CLIENT_TOKEN = os.getenv("TIDAL_WEB_CLIENT_TOKEN", "")
    
    # No community or shared client credentials are bundled with msc.
    CLIENT_KEYS = []  # Supply your own registered client credentials.

    # Token storage locations
    TOKEN_FILE = Path("config/tidal_tokens.json")

    # Quality mapping (Tidal unified tier delivers genuine FLAC via HI_RES_LOSSLESS)
    QUALITY_MAP = {
        "LOW": "LOW",              # 96 kbps AAC
        "HIGH": "HIGH",            # 320 kbps AAC
        "HD": "HI_RES_LOSSLESS",   # 16-bit/24-bit bit-perfect FLAC
        "UHD": "HI_RES_LOSSLESS",  # 24-bit Hi-Res FLAC / Master
        "LOSSLESS": "HI_RES_LOSSLESS",
        "HI_RES": "HI_RES_LOSSLESS",
        "HI_RES_LOSSLESS": "HI_RES_LOSSLESS",
        "MASTER": "HI_RES_LOSSLESS",
    }

    DEFAULT_COUNTRY_CODE = os.getenv("TIDAL_COUNTRY_CODE", "US")

    @classmethod
    def get_primary_client_key(cls) -> Dict[str, str]:
        custom_client_id = os.getenv("TIDAL_CLIENT_ID")
        custom_client_secret = os.getenv("TIDAL_CLIENT_SECRET")
        if custom_client_id:
            return {
                "platform": "Custom",
                "clientId": custom_client_id,
                "clientSecret": custom_client_secret or "",
                "scope": "r_usr+w_usr+w_sub",
            }
        raise RuntimeError("Configure your TIDAL_CLIENT_ID and TIDAL_CLIENT_SECRET before signing in.")
