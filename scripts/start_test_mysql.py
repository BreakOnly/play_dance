"""Start an isolated MySQL instance in WSL. Never uses the workstation database."""
import json
import pathlib
import subprocess
import tempfile
import time

root = pathlib.Path(__file__).resolve().parents[1]
state_file = root / "output/test-mysql.json"
state_file.parent.mkdir(exist_ok=True)
if state_file.exists():
    saved = json.loads(state_file.read_text(encoding="utf-8"))
    check = subprocess.run(["mysqladmin", "--no-defaults", "--socket="+saved["socket"], "-uroot", "--password=dance-test-local-only", "ping"], capture_output=True)
    if check.returncode == 0:
        print("ISOLATED_MYSQL_ALREADY_READY port=13316", flush=True)
        raise SystemExit(0)
    raise RuntimeError("Stale test instance state; run scripts/stop_test_mysql.py before starting again")
data_root = pathlib.Path(tempfile.mkdtemp(prefix="dance-repair-mysql-"))
data = data_root / "data"
socket = data_root / "mysql.sock"
log = data_root / "mysql.log"
subprocess.run(["mysqld", "--no-defaults", "--initialize-insecure", "--datadir="+str(data), "--log-error="+str(log)], check=True)
with log.open("ab") as output:
    proc = subprocess.Popen(["mysqld", "--no-defaults", "--datadir="+str(data), "--socket="+str(socket),
        "--port=13316", "--bind-address=127.0.0.1", "--mysqlx=0", "--skip-log-bin", "--log-error="+str(log),
        "--pid-file="+str(data_root / "mysql.pid")], stdin=subprocess.DEVNULL, stdout=output, stderr=output, start_new_session=True)
state_file.write_text(json.dumps({"pid":proc.pid,"directory":str(data_root),"socket":str(socket),"port":13316}), encoding="utf-8")
for _ in range(60):
    result = subprocess.run(["mysql", "--no-defaults", "--socket="+str(socket), "-uroot", "-e", "SELECT 1"], capture_output=True)
    if result.returncode == 0:
        subprocess.run(["mysql", "--no-defaults", "--socket="+str(socket), "-uroot"], input=b"CREATE DATABASE dance_repair_test CHARACTER SET utf8mb4; ALTER USER 'root'@'localhost' IDENTIFIED BY 'dance-test-local-only';", check=True)
        print("ISOLATED_MYSQL_READY port=13316 database=dance_repair_test", flush=True)
        break
    if proc.poll() is not None:
        raise RuntimeError(log.read_text())
    time.sleep(0.5)
else:
    raise RuntimeError("MySQL startup timeout: " + str(log))
