"""Reject missing, empty or failed connected Android test results after a fresh run."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
files = list((root / "app/build/outputs/androidTest-results/connected/debug").glob("TEST-*.xml"))
expected = {
    "org.msc.app.FoundationTest",
    "org.msc.app.PlaybackFixtureTest",
}
seen = set()
count = 0
for file in files:
    result = ET.parse(file).getroot()
    for suite in result.iter():
        if suite.tag in {"testsuite", "testsuites"}:
            if int(suite.get("failures", 0)) or int(suite.get("errors", 0)):
                sys.exit(f"Android test failures/errors in {file.name}")
    for case in result.iter("testcase"):
        if case.find("failure") is not None or case.find("error") is not None or case.find("skipped") is not None:
            sys.exit(f"Android case did not pass: {case.get('name')}")
        count += 1
        seen.add(case.get("classname"))
if count < 2 or not expected.issubset(seen):
    sys.exit(f"Android runtime gate failed: {count} completed cases; missing {sorted(expected - seen)}")
print(f"Android runtime gate: {count} cases passed")
