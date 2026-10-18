"""Run only the Level integration probe in the existing disposable Lophine server."""
import os
import json
import re
import pathlib
import queue
import shutil
import socket
import sys
import subprocess
import threading
import time
import zipfile


def main():
    root = pathlib.Path(os.environ["LOCALAPPDATA"]) / "Temp/opencode/lophine-26.3-test"
    source = pathlib.Path(__file__).resolve().parents[3]
    core = source / "build/libs/BentoBox-3.23.4-SNAPSHOT-LOCAL.jar"
    level = source.parent / "BentoBox-addons/Level/target/Level-2.29.0-SNAPSHOT-LOCAL.jar"
    mode = sys.argv[1] if len(sys.argv) > 1 else "level"
    assert mode in ("level", "warps", "biomes")
    warps = source.parent / "BentoBox-addons/Warps/target/Warps-1.19.1-SNAPSHOT-LOCAL.jar"
    biomes = source.parent / "BentoBox-addons/Biomes/target/Biomes-2.3.2-SNAPSHOT-LOCAL.jar"
    assert root.is_dir() and (root / "lophine-26.3-paperclip.jar").is_file()
    with socket.socket() as port:
        assert port.connect_ex(("127.0.0.1", 25575)) != 0, "Isolation server port already in use"
    properties = (root / "server.properties").read_text()
    assert "server-ip=127.0.0.1" in properties and "server-port=25575" in properties
    for artifact in (core, level, *([warps] if mode == "warps" else []),
                     *([biomes] if mode == "biomes" else [])):
        with zipfile.ZipFile(artifact) as jar:
            assert not any(name.startswith(("probe/", "org/junit/", "org/mockito/")) for name in jar.namelist())
    with zipfile.ZipFile(core) as src, zipfile.ZipFile(root / "plugins/BentoBox.jar", "w") as dst:
        for entry in src.infolist():
            data = src.read(entry.filename)
            if entry.filename == "plugin.yml":
                assert b"folia-supported" not in data
                data += b"\nfolia-supported: true\n"
            dst.writestr(entry, data)
    shutil.copyfile(level, root / "plugins/BentoBox/addons" / level.name)
    if mode == "warps":
        shutil.copyfile(warps, root / "plugins/BentoBox/addons" / warps.name)
    if mode == "biomes":
        shutil.copyfile(biomes, root / "plugins/BentoBox/addons" / biomes.name)
    shutil.copyfile(source / "build/folia-probe/BentoBoxFoliaProbe.jar", root / "plugins/BentoBoxFoliaProbe.jar")
    process = subprocess.Popen(
        ["java", "-Xms1G", "-Xmx2G", f"-Dprobe.{mode}Only=true", "-jar", "lophine-26.3-paperclip.jar", "--nogui"],
        cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, encoding="utf-8", errors="replace")
    lines = queue.Queue()

    def read_output():
        for line in process.stdout:
            lines.put(line)
        lines.put(None)

    threading.Thread(target=read_output, daemon=True).start()
    log_path = root / (time.strftime(f"{mode}-%Y%m%d-%H%M%S") + ".log")
    passed = False
    errors = []
    deadline = time.monotonic() + 240
    try:
        with log_path.open("w", encoding="utf-8") as log:
            while time.monotonic() < deadline:
                try:
                    line = lines.get(timeout=1)
                except queue.Empty:
                    continue
                if line is None:
                    break
                log.write(line)
                log.flush()
                if "ERROR" in line or "Exception" in line or f"{mode.upper()}_PROBE_FAILED" in line:
                    errors.append(line.strip())
                if any(key in line for key in ("Level", "LEVEL_", "Warps", "WARPS_", "Biomes", "BIOMES_", "ERROR", "Exception")):
                    print(line.rstrip(), flush=True)
                if f"{mode.upper()}_PROBE_PASSED" in line:
                    passed = True
                    break
                if f"{mode.upper()}_PROBE_FAILED" in line or "Could not pass event BentoBoxReadyEvent" in line:
                    break
            process.stdin.write("stop\n")
            process.stdin.flush()
            process.wait(timeout=90)
            while not lines.empty():
                line = lines.get_nowait()
                if line is not None:
                    log.write(line)
                    if "ERROR" in line or "Exception" in line:
                        errors.append(line.strip())
    finally:
        if process.poll() is None:
            # Only the child server launched above can be terminated.
            process.terminate()
            process.wait(timeout=30)
    print(f"Log: {log_path}")
    assert passed and not errors and process.returncode == 0, f"Probe failed: passed={passed}, errors={errors}, exit={process.returncode}"
    if mode != "level":
        return
    match = re.search(r"LEVEL_OFFLINE_SCAN_PASSED: (\S+)", log_path.read_text(encoding="utf-8"))
    assert match, "Missing scanned island ID"
    data = json.loads((root / "plugins/BentoBox/database/IslandLevels" / (match[1] + ".json")).read_text(encoding="utf-8"))
    assert data["mdCount"]["DIAMOND_BLOCK"] == 1 and data["mdCount"]["EMERALD_BLOCK"] == 9
    print("LEVEL_DISK_PERSISTENCE_PASSED")


if __name__ == "__main__":
    main()
