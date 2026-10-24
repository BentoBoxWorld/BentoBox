"""Run only the local IslandWarp test server; stop normally when its test helper finishes."""
from pathlib import Path
import subprocess
import threading
import sys
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--smoke", action="store_true", help="Startup without helper, then status/reload/normal stop")
options = parser.parse_args()

root = Path(r"D:\GongZhuo\IslandWarp-test")
log = root / ("clean-startup-test.log" if options.smoke else "functional-test.log")
result = {"passed": False, "finished": False, "errors": False}
process = subprocess.Popen(["java", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Xms512M", "-Xmx1G", "-jar", "lophine-26.3-paperclip.jar", "nogui"], cwd=root,
                           stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
                           encoding="utf-8", errors="replace", bufsize=1)

def stop():
    if process.poll() is None:
        process.stdin.write("stop\n")
        process.stdin.flush()

timer = threading.Timer(180, stop)
timer.start()
try:
    with log.open("w", encoding="utf-8") as output:
        for line in process.stdout:
            output.write(line)
            output.flush()
            if " ERROR]:" in line or "/ERROR]" in line:
                result["errors"] = True
            if "SERVER_CHECK" in line or "ERROR" in line or "Exception" in line:
                print(line.rstrip())
            if "SERVER_CHECK_ALL_PASS" in line:
                result["passed"] = True
            if "SERVER_CHECK_FAILED" in line or "fixture cleanup failed" in line:
                result["passed"] = False
            if "SERVER_CHECK_FINISHED" in line:
                result["finished"] = True
                stop()
            if options.smoke and "Done (" in line:
                process.stdin.write("iw status\niw reload\niw status\nstop\n")
                process.stdin.flush()
            if options.smoke and "SQLite：正常" in line:
                result["passed"] = True
                result["finished"] = True
        code = process.wait(timeout=60)
finally:
    timer.cancel()
print(f"Test server exit={code}; all_pass={result['passed']}; cleanup_finished={result['finished']}; log={log}")
sys.exit(0 if code == 0 and result["passed"] and result["finished"] and not result["errors"] else 1)
