"""Print per-test skip reasons and the overall totals from target/surefire-reports."""
import glob
import sys
import xml.etree.ElementTree as ET

tests = failures = errors = skipped = 0
skips = []
for path in sorted(glob.glob("target/surefire-reports/TEST-*.xml")):
    root = ET.parse(path).getroot()
    tests += int(root.get("tests", 0))
    failures += int(root.get("failures", 0))
    errors += int(root.get("errors", 0))
    skipped += int(root.get("skipped", 0))
    for case in root.iter("testcase"):
        sk = case.find("skipped")
        if sk is not None:
            skips.append("%s.%s: %s" % (case.get("classname"), case.get("name"), sk.get("message") or "(no message)"))

print("Skipped tests (%d):" % len(skips))
for line in skips:
    print("  SKIPPED " + line)
print("Surefire totals: tests=%d passed=%d failures=%d errors=%d skipped=%d"
      % (tests, tests - failures - errors - skipped, failures, errors, skipped))
sys.exit(0 if tests > 0 else 1)
