#!/usr/bin/env python3
import sys
import os
import json
from pathlib import Path
from tqdm import tqdm

def print_usage():
    print("Usage: ./validate_buff_ids.py \"<comma_separated_buff_ids>\" <target_directory>")
    print("Example: ./validate_buff_ids.py \"8000340, 8000344, 9000770\" ./combat_logs")
    sys.exit(1)

def main():
    if len(sys.argv) < 3:
        print_usage()

    # Parse comma-separated string of IDs into a set of integers for O(1) lookups
    try:
        target_buff_ids = {int(bid.strip()) for bid in sys.argv[1].split(",") if bid.strip()}
    except ValueError:
        print("Error: Buff IDs must be valid integers separated by commas.")
        sys.exit(1)

    target_dir = Path(sys.argv[2])
    if not target_dir.is_dir():
        print(f"Error: Directory '{target_dir}' does not exist or is not a valid directory.")
        sys.exit(1)

    # Gather all .rf files recursively
    rf_files = list(target_dir.rglob("*.rf"))
    if not rf_files:
        print(f"No .rf files found in target directory: {target_dir}")
        sys.exit(0)

    print(f"Found {len(rf_files)} .rf file(s). Calculating total size for progress tracking...")

    # Calculate total size in bytes for a precise progress bar across gigabytes of logs
    total_bytes = sum(f.stat().st_size for f in rf_files if f.is_file())
    
    seen_buffs = set()

    # Process files with a progress bar mapped to bytes read
    with tqdm(total=total_bytes, unit="B", unit_scale=True, unit_divisor=1024, desc="Parsing logs") as pbar:
        for file_path in rf_files:
            try:
                with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                    for line in f:
                        line_bytes = len(line.encode("utf-8", errors="ignore"))
                        
                        # Fast text pre-filtering to avoid expensive json loads on irrelevant lines
                        if "SPELL_AURA_APPLIED" in line:
                            try:
                                data = json.loads(line)
                                if data.get("type") == "SPELL_AURA_APPLIED":
                                    buff_id = data.get("buffId")
                                    if buff_id in target_buff_ids:
                                        seen_buffs.add(buff_id)
                            except json.JSONDecodeError:
                                pass # Skip malformed trailing/corrupted log lines safely
                                
                        pbar.update(line_bytes)
            except Exception as e:
                # Catch permission errors or file read issues gracefully without crashing
                tqdm.write(f"Warning: Could not read file {file_path}: {e}")

    # Compute results
    never_seen_buffs = target_buff_ids - seen_buffs

    # Sort outputs for clean presentation
    sorted_seen = sorted(list(seen_buffs))
    sorted_never_seen = sorted(list(never_seen_buffs))

    print("\n" + "="*50)
    print("RESULTS SUMMARY")
    print("="*50)
    print(f"Total Unique Target Buffs Checked: {len(target_buff_ids)}")
    print(f"Seen Buffs Count:                 {len(sorted_seen)}")
    print(f"Never Seen Buffs Count:           {len(sorted_never_seen)}")
    print("-" * 50)
    print("Seen Buff IDs (Comma-Separated):")
    print(",".join(map(str, sorted_seen)) if sorted_seen else "None")
    print("-" * 50)
    print("Never Seen Buff IDs (Comma-Separated):")
    print(",".join(map(str, sorted_never_seen)) if sorted_never_seen else "None")
    print("="*50)

if __name__ == "__main__":
    main()
