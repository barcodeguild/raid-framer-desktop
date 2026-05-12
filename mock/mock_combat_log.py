# Simple script to simulate a combat.log file in real time.
SAMPLE_LOG = "sample.log"
OUTPUT_LOG = "combat.log"  # Change this path as needed
SPEED_MULTIPLIER = 2.0     # 1.0 = real time, 2.0 = 2x faster, etc.

import os
import time
from datetime import datetime, timedelta

# Parses input log and assumes format: <YYYY-MM-DD HH:MM:SS|...
def parse_timestamp(line):
	if line.startswith('<'):
		try:
			ts_str = line[1:20]
			return datetime.strptime(ts_str, "%Y-%m-%d %H:%M:%S")
		except Exception:
			return None
	return None

# Swaps the timestamps line-by-line with new_dt
def replace_timestamp(line, new_dt):
	if line.startswith('<'):
		return f"<{new_dt.strftime('%Y-%m-%d %H:%M:%S')}{line[20:]}"
	return line

# Reads sample log and simulates writing to output log in real-time. Basically just sleeps a certain
# amount of time in realation to the speed multiplier. 1x = real time, 2x = twice as fast, etc.
def simulate_log():
	
	# Read all lines and timestamps
	with open(SAMPLE_LOG, "r", encoding="utf-8") as f:
		lines = f.readlines()
	timestamps = [parse_timestamp(line) for line in lines]
	
	# Find the first valid timestamp
	first_ts = next((ts for ts in timestamps if ts), None)
	if not first_ts:
		print("No valid timestamps found in sample log. Check engine..")
		return
	
	# Compute time offsets
	offsets = [(ts - first_ts if ts else timedelta(0)) for ts in timestamps]
	
	# Start simulation from now
	start_dt = datetime.now()
	last_offset = timedelta(0)
	
	# Clear the output file at the start
	open(OUTPUT_LOG, "w", encoding="utf-8").close()
	with open(OUTPUT_LOG, "a", encoding="utf-8") as out:
		for i, line in enumerate(lines):
			offset = offsets[i]
			
			# Compute new timestamp
			new_dt = start_dt + offset / SPEED_MULTIPLIER
			out.write(replace_timestamp(line, new_dt))
			
			# Sleep for the difference between this and last offset
			sleep_sec = (offset - last_offset).total_seconds() / SPEED_MULTIPLIER # bigger divisor = faster
			
			if sleep_sec > 0:
				time.sleep(sleep_sec)
			last_offset = offset

# Main Entry
if __name__ == "__main__":
	simulate_log()
