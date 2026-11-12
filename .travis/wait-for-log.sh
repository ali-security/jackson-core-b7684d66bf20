#!/bin/bash
# Keep the job open until Travis has archived this job's test summary, so the
# log harvested right after the build finishes is complete. Never changes the
# build result (after_script). Prints a heartbeat to avoid the 10-minute no-output kill.
URL="https://api.travis-ci.com/v3/job/${TRAVIS_JOB_ID}/log.txt"
MARK='Surefire totals: tests='
waited=0
while [ "$waited" -lt 1500 ]; do
  if curl -fsSL "$URL" 2>/dev/null | grep -q "$MARK"; then
    echo "log archived after ${waited}s"
    exit 0
  fi
  echo "waiting for log archive... (${waited}s)"
  sleep 20
  waited=$((waited + 20))
done
echo "gave up waiting after ${waited}s"
exit 0
