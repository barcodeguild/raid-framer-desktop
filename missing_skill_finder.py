import os
import re

# Read the input file and build a list of .png references
with open('src/main/kotlin/core/helpers/GeneralHelper.kt', 'r') as file:
    data = file.read()
png_references = re.findall(r'painterResource\("(.*?\.png)"\)', data)

# List all .png files in a specified directory
directory_path = 'src/main/resources/'  # replace with your directory path
png_files = [f for f in os.listdir(directory_path) if f.endswith('.png')]

# Compare the two lists
missing_files = set(png_references) - set(png_files)
extra_files = set(png_files) - set(png_references)

# Print the results
print("Missing files:", missing_files)
print("Extra files:", extra_files)