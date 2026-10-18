#!/usr/bin/env python3
import sys
import json
from pathlib import Path
from collections import defaultdict
from tqdm import tqdm

def print_usage():
    print("Usage: ./mine_buff_ids.py <target_directory>")
    print("Example: ./mine_buff_ids.py ./combat_logs")
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

    aura_types = {
        "SPELL_AURA_APPLIED",
        "SPELL_AURA_APPLIED_DOSE",
        "SPELL_AURA_REMOVED",
        "SPELL_AURA_REFRESH",
    }

    # Group buff IDs by buff name (A-Z sorted output)
    # Aura events use buffId/buffName, not spellId/spellName
    buff_map = defaultdict(set)

    with tqdm(total=total_bytes, unit="B", unit_scale=True, unit_divisor=1024, desc="Parsing logs") as pbar:
        for file_path in rf_files:
            try:
                with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                    for line in f:
                        line_bytes = len(line.encode("utf-8", errors="ignore"))

                        if "SPELL_AURA_" in line:
                            try:
                                data = json.loads(line)
                                event_type = data.get("type", "")
                                if event_type in aura_types:
                                    buff_id = data.get("buffId")
                                    buff_name = data.get("buffName", "")
                                    if buff_id is not None:
                                        buff_map[buff_name].add(buff_id)
                            except json.JSONDecodeError:
                                pass

                        pbar.update(line_bytes)
            except Exception as e:
                tqdm.write(f"Warning: Could not read file {file_path}: {e}")

    print("\n" + "=" * 50)
    print("BUFF/DEBUFF IDs BY NAME")
    print("=" * 50)

    for buff_name in sorted(buff_map.keys()):
        ids = sorted(buff_map[buff_name])
        ids_str = ", ".join(str(i) for i in ids)
        if buff_name:
            print(f"{buff_name}: {ids_str}")
        else:
            print(f"(no name): {ids_str}")

    print("=" * 50)
    total_unique = set()
    for ids in buff_map.values():
        total_unique.update(ids)
    print(f"Total unique buff names found: {len(buff_map)}")
    print(f"Total unique buff/debuff IDs found: {len(total_unique)}")

if __name__ == "__main__":
    main()
