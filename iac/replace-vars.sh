#!/bin/bash

# Check if input file path is provided
if [[ -z $1 ]]; then
  echo "Error: Input file path not provided."
  exit 1
fi

# Read the input file
input_file="$1"
content=$(<"$input_file")

# Get the directory path of the input file
dir_path=$(dirname "$input_file")

# Replace variables with their values
while IFS= read -r line; do
  while [[ $line =~ (\$\{([a-zA-Z_][a-zA-Z_0-9]*)\}) ]]; do
    var="${BASH_REMATCH[2]}"
    value="${!var}"
    line=${line//${BASH_REMATCH[1]}/$value}
  done
  echo "$line"
done <<< "$content" > "$dir_path/tf_$(basename "$input_file")"