#!/usr/bin/env python3
import sys
import os
import json
from pathlib import Path
from collections import defaultdict
from tqdm import tqdm

def print_usage():
    print("Usage: ./mine_spell_cast_ids.py <target_directory>")
    print("Example: ./mine_spell_cast_ids.py ./combat_logs")
    sys.exit(1)

def main():
    if len(sys.argv) < 2:
        print_usage()

    target_dir = Path(sys.argv[1])
    if not target_dir.is_dir():
        print(f"Error: Directory '{target_dir}' does not exist or is not a valid directory.")
        sys.exit(1)

    rf_files = list(target_dir.rglob("*.rf"))
    if not rf_files:
        print(f"No .rf files found in target directory: {target_dir}")
        sys.exit(0)

    print(f"Found {len(rf_files)} .rf file(s). Calculating total size for progress tracking...")

    total_bytes = sum(f.stat().st_size for f in rf_files if f.is_file())

    spell_map = defaultdict(set)

    with tqdm(total=total_bytes, unit="B", unit_scale=True, unit_divisor=1024, desc="Parsing logs") as pbar:
        for file_path in rf_files:
            try:
                with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                    for line in f:
                        line_bytes = len(line.encode("utf-8", errors="ignore"))

                        if "SPELL_CAST_" in line:
                            try:
                                data = json.loads(line)
                                if data.get("type", "").startswith("SPELL_CAST_"):
                                    spell_name = data.get("spellName", "")
                                    spell_id = data.get("spellId")
                                    if spell_name and spell_id is not None:
                                        spell_map[spell_name].add(spell_id)
                            except json.JSONDecodeError:
                                pass

                        pbar.update(line_bytes)
            except Exception as e:
                tqdm.write(f"Warning: Could not read file {file_path}: {e}")

    print("\n" + "=" * 50)
    print("SPELL CAST IDs BY SKILL")
    print("=" * 50)

    for spell_name in sorted(spell_map.keys()):
        ids = sorted(spell_map[spell_name])
        ids_str = ", ".join(str(i) for i in ids)
        print(f"{spell_name}: {ids_str}")

    print("=" * 50)
    print(f"Total unique spell names found: {len(spell_map)}")
    print(f"Total unique spell IDs found: {sum(len(v) for v in spell_map.values())}")

if __name__ == "__main__":
    main()
