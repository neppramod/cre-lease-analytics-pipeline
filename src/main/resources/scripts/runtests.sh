#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status
set -e

# Define your array of file keys
FILES=("sample1" "sample2" "sample3")
BASE_URL="http://localhost:8080/pdfreader/parse"

echo "=================================================="
echo "🚀 Initiating CRE Pipeline Multi-File Test Sweep"
echo "=================================================="

# Loop through each item in the array
for FILE in "${FILES[@]}"; do
    echo -e "\n📂 Processing: ${FILE}.pdf"
    echo "--------------------------------------------------"

    # Use --silent to prevent download progress matrix from printing
    curl --silent -w "\n" "${BASE_URL}?file=${FILE}"
done

echo -e "\n=================================================="
echo "✅ Test Suite Complete!"
echo "=================================================="