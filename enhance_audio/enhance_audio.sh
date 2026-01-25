#!/bin/bash

# Check if ffmpeg is installed
if ! command -v ffmpeg &> /dev/null; then
    echo "Error: ffmpeg is not installed. Please install it to use this script."
    echo "You can install it using: brew install ffmpeg"
    exit 1
fi

# Check arguments
if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <input_file.ogg> [output_file.ogg]"
    exit 1
fi

INPUT_FILE="$1"
OUTPUT_FILE="${2:-${INPUT_FILE%.*}_enhanced.ogg}"

if [ ! -f "$INPUT_FILE" ]; then
    echo "Error: Input file '$INPUT_FILE' not found."
    exit 1
fi

echo "Processing '$INPUT_FILE'..."
echo "Output will be saved to '$OUTPUT_FILE'"

# Audio enhancement filter chain
# 0. afade: Fade in 0.2s (hsin curve) to completely silence start pops
# 1. adeclip: Remove clipping artifacts
# 2. adeclick: Remove impulsive clicks
# Audio enhancement filter chain
# 0. afade: Fade in 0.2s (hsin curve) to completely silence start pops
# 1. adeclip: Remove clipping artifacts
# 2. adeclick: Remove impulsive clicks
# 3. highpass: Remove rumble (100Hz - relaxed)
# 4. lowpass: Remove high frequency hiss (12kHz - allow clarity)
# 5. afftdn: FFT-based denoising (nr=25dB - preserve texture)
# 6. treble: Boost highs (+5dB) for brightness
# 7. compand: Compression
# 8. gate: Noise Gate (silence sounds below -45dB)
# 9. volume: Boost
FILTER_CHAIN="afade=t=in:ss=0:d=0.4:curve=hsin,adeclip,adeclick,highpass=f=100,lowpass=f=12000,afftdn=nr=25:nf=-30:tn=1,treble=g=5,compand=.3|.3:1|1:-90/-60|-60/-40|-40/-30|-20/-20:6:0:-90:0.2,agate=range=-90dB:threshold=-45dB:release=100,volume=1.5"

ffmpeg -i "$INPUT_FILE" -af "$FILTER_CHAIN" -c:a vorbis -ac 2 -strict -2 -q:a 4 "$OUTPUT_FILE" -y

if [ $? -eq 0 ]; then
    echo "Successfully created '$OUTPUT_FILE'"
else
    echo "Error processing audio file."
    exit 1
fi
