import os
import subprocess
from concurrent.futures import ThreadPoolExecutor
import time

AUDIO_DIR = "app/src/main/assets/audio"
MAX_WORKERS = 8

def convert_file(wav_path):
    ogg_path = wav_path + ".ogg"
    if os.path.exists(ogg_path):
        return "SKIPPED"
    
    try:
        cmd = [
            "ffmpeg", "-y", "-nostdin", "-i", wav_path,
            "-c:a", "libvorbis", "-q:a", "4", ogg_path
        ]
        # Run quietly
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode == 0:
            return "SUCCESS"
        else:
            print(f"Error converting {wav_path}: {result.stderr}")
            return "FAILED"
    except Exception as e:
        print(f"Exception converting {wav_path}: {e}")
        return "ERROR"

def main():
    wav_files = []
    for root, dirs, files in os.walk(AUDIO_DIR):
        for f in files:
            if f.endswith(".wav"):
                wav_files.append(os.path.join(root, f))
    
    print(f"Found {len(wav_files)} WAV files.")
    
    start_time = time.time()
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        results = list(executor.map(convert_file, wav_files))
    
    converted = results.count("SUCCESS")
    skipped = results.count("SKIPPED")
    failed = results.count("FAILED") + results.count("ERROR")
    
    print(f"Conversion complete in {time.time() - start_time:.2f}s")
    print(f"Converted: {converted}")
    print(f"Skipped: {skipped}")
    print(f"Failed: {failed}")

if __name__ == "__main__":
    main()
