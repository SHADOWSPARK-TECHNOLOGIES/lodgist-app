#!/usr/bin/env python3
"""Truth checks for the existing Lodgist concierge. No new agent."""
import json
import urllib.error
import urllib.request

API = "https://lodgist-api-production.up.railway.app"
CONFIG = "/home/moronto/lodgist-app/concierge/agent_config.json"
BOOKING = "4a794a35-e3a9-4aff-9f6f-4d98c227907d"


def hit(url, method="GET"):
    req = urllib.request.Request(url, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return resp.status, json.loads(resp.read().decode() or "{}")
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode()
        try:
            body = json.loads(raw)
        except json.JSONDecodeError:
            body = {"raw": raw[:160]}
        return exc.code, body


def main():
    cfg = json.load(open(CONFIG))
    assert cfg["configured"]["serviceArea"].startswith("Starting in Imo.")
    assert "city is Imo" not in cfg["configured"]["serviceArea"]
    assert cfg["deployed"]["bookingTool"]["referencedByPlaybook"] is False
    assert cfg["deployed"]["bookingTool"]["servers"] is False

    status, body = hit(f"{API}/api/listings")
    assert status == 200 and body.get("data") == [] and body["pagination"]["total"] == 0, body
    print("empty_inventory pass")

    status, body = hit(f"{API}/api/listings/999999999")
    assert status == 404 and body.get("error") == "Listing not found"
    print("unknown_property pass")

    status, body = hit(f"{API}/api/listings/1/viewing-requests", "POST")
    assert status == 401
    print("unpaid_or_unauthenticated_booking pass")

    status, _ = hit("https://lodgist.online/api/listings")
    assert status != 200
    print("wrong_host_failure pass")

    status, body = hit(f"{API}/api/listings/1/viewing-requests/mine")
    assert status == 401 and "authorization" in json.dumps(body).lower()
    print("cross_user_blocked pass")

    assert BOOKING not in json.dumps(cfg["deployed"]["dialogflowCx"].get("referencedTools", []))
    print("booking_tool_unreferenced_in_record pass")
    print("ok")


if __name__ == "__main__":
    main()
