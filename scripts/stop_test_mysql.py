"""Stop and remove only the isolated MySQL instance created by start_test_mysql.py."""
import json
import pathlib
import subprocess
import shutil
import time

workspace = pathlib.Path(__file__).resolve().parents[1]
state_file = workspace / "output/test-mysql.json"
if not state_file.exists():
    print("No isolated MySQL state")
    raise SystemExit(0)
state = json.loads(state_file.read_text(encoding="utf-8"))
directory = pathlib.Path(state["directory"])
if directory.parent != pathlib.Path("/tmp") or not directory.name.startswith("dance-repair-mysql-") or directory.resolve() != directory:
    raise RuntimeError("Refusing unexpected data directory")
socket = directory / "mysql.sock"
if str(socket) != state["socket"]:
    raise RuntimeError("Unexpected socket")
pid = int(state["pid"])
command = pathlib.Path("/proc") / str(pid) / "cmdline"
if command.exists():
    if ("--datadir=" + str(directory / "data")).encode() not in command.read_bytes().split(b"\0"):
        raise RuntimeError("PID does not belong to the isolated instance")
    subprocess.run(["mysqladmin", "--no-defaults", "--socket="+str(socket), "-uroot", "--password=dance-test-local-only", "shutdown"], check=True, capture_output=True)
    for _ in range(30):
        if not socket.exists(): break
        time.sleep(0.2)
    if socket.exists(): raise RuntimeError("MySQL has not shut down")
shutil.rmtree(directory)
state_file.unlink()
print("ISOLATED_MYSQL_REMOVED")
