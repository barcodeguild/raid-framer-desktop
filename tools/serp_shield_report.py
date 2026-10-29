#!/usr/bin/env python3
"""
Serp Shield Usage Report

Unpacks packed item usage Longs from player_cache CSV exports and produces
a human-readable report of Serp Shield usage counts and timestamps.

The packed Long format (from util.kt packItemUsageLong):
  - Upper 32 bits = epoch seconds (last used timestamp)
  - Lower 32 bits = usage counter

Usage:
    python serp_shield_report.py <csv_file>

Example:
    python serp_shield_report.py serp_shield_uses.csv
"""
import sys
import csv
from datetime import datetime
from pathlib import Path


def print_usage():
    script_name = Path(sys.argv[0]).name
    print(f"Usage: python {script_name} <csv_file>")
    print('Example: python {script_name} serp_shield_uses.csv')
    sys.exit(1)


# Unpack the packed Long: upper 32 bits = epoch seconds, lower 32 bits = usage counter
ITEM_USAGE_COUNTER_MASK = 0xFFFFFFFF


def unpack_usage_counter(packed: int) -> int:
    """Extract the usage counter from the lower 32 bits."""
    return packed & ITEM_USAGE_COUNTER_MASK


def unpack_timestamp(packed: int) -> datetime:
    """Extract the epoch timestamp from the upper 32 bits."""
    seconds = (packed >> 32) & ITEM_USAGE_COUNTER_MASK
    return datetime.fromtimestamp(seconds)


def main():
    if len(sys.argv) < 2:
        print_usage()

    input_file = Path(sys.argv[1])
    if not input_file.is_file():
        print(f"Error: File '{input_file}' does not exist.")
        sys.exit(1)

    rows = []
    with open(input_file, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            shield_val = row.get("lastSerpShield", "0").strip()
            if shield_val and shield_val != "0":
                packed = int(shield_val)
                uses = unpack_usage_counter(packed)
                if uses > 0:
                    last_used_dt = unpack_timestamp(packed)
                    rows.append({
                        "playerName": row["playerName"].strip(),
                        "uses": uses,
                        "lastUsed": last_used_dt,
                    })

    # Sort by usage count descending
    rows.sort(key=lambda r: r["uses"], reverse=True)

    print(f"{'#':<6} {'Uses':<8} {'Player Name':<45} {'Last Used (Local Time)':<22}")
    print("-" * 85)

    for i, row in enumerate(rows, 1):
        last_used_str = row["lastUsed"].strftime("%Y-%m-%d %H:%M:%S")
        print(f"{i:<6} {row['uses']:<8} {row['playerName']:<45} {last_used_str:<22}")

    print(f"\nTotal players with Serp Shield uses: {len(rows)}")
    print(f"Total Serp Shield casts across all players: {sum(r['uses'] for r in rows)}")


if __name__ == "__main__":
    main()
