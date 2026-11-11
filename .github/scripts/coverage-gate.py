#!/usr/bin/env python3
"""Fails when the aggregated JaCoCo coverage drops below the minimums.

Usage: coverage-gate.py <jacoco.xml>
Minimums come from the COVERAGE_MIN_BRANCH and COVERAGE_MIN_LINE environment variables (percent).
Writes a summary table to $GITHUB_STEP_SUMMARY when it is set.
"""

import os
import sys
import xml.etree.ElementTree as ET


def percent(counter):
    covered = int(counter.get("covered"))
    total = covered + int(counter.get("missed"))
    return 100.0 * covered / total if total else 100.0


def main():
    report = ET.parse(sys.argv[1]).getroot()
    totals = {c.get("type"): percent(c) for c in report.findall("counter")}
    minimums = {
        "BRANCH": float(os.environ.get("COVERAGE_MIN_BRANCH", "0")),
        "LINE": float(os.environ.get("COVERAGE_MIN_LINE", "0")),
    }

    rows = ["| Counter | Coverage | Minimum | |", "|---|---|---|---|"]
    failed = False
    for counter, minimum in minimums.items():
        actual = totals.get(counter, 100.0)
        ok = actual >= minimum
        failed |= not ok
        rows.append(f"| {counter.lower()} | {actual:.1f}% | {minimum:.0f}% | {'✅' if ok else '❌'} |")

    summary = "## Test coverage\n\n" + "\n".join(rows) + "\n"
    print(summary)
    if "GITHUB_STEP_SUMMARY" in os.environ:
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as out:
            out.write(summary)
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
