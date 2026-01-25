#!/bin/bash

# Configuration
ASSETS_DIR="app/src/main/assets/audio"
BACKUP_DIR="audio_backup_raw"
ENHANCE_SCRIPT="./enhance_audio/enhance_audio.sh"

# Ensure enhance script is executable
chmod +x "$ENHANCE_SCRIPT"

# Create backup directory
if [ ! -d "$BACKUP_DIR" ]; then
    echo "Creating backup directory: $BACKUP_DIR"
    mkdir -p "$BACKUP_DIR"
fi

# Counter
count=0

# Find and process files
find "$ASSETS_DIR" -name "*.ogg" | while read -r FILE; do
    ((count++))
    filename=$(basename "$FILE")
    rel_path="${FILE#$ASSETS_DIR/}"
    backup_path="$BACKUP_DIR/$rel_path"
    backup_parent=$(dirname "$backup_path")

    echo "[$count] Processing: $rel_path"

    # 1. Backup
    if [ ! -f "$backup_path" ]; then
        mkdir -p "$backup_parent"
        cp "$FILE" "$backup_path"
        echo "    -> Backed up to $backup_path"
    else
        echo "    -> Backup already exists, skipping backup."
    fi

    # 2. Process
    TEMP_OUTPUT="${FILE}.tmp.ogg"
    
    # Run the enhancement script
    # We suppress output to keep the terminal clean, only showing errors
    if "$ENHANCE_SCRIPT" "$FILE" "$TEMP_OUTPUT" > /dev/null 2>&1; then
        # 3. Overwrite original
        mv -f "$TEMP_OUTPUT" "$FILE"
        echo "    -> Enhanced successfully."
    else
        echo "    -> ERROR: Failed to enhance $FILE"
        # Clean up temp if it exists
        rm -f "$TEMP_OUTPUT"
    fi

    # 4. Pace
    sleep 0.5
done

echo "Batch processing complete!"
