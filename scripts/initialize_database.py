"""Initialize the current application's 17 tables in an existing, empty local database."""
import argparse
import getpass
import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SQL_FILES = [
    "equipment-backend/sql/ry_20260417.sql",
    "sql/001_business_schema.sql",
    "sql/002_business_dict_menu.sql",
    "sql/003_work_order_record.sql",
    "sql/004_work_order_dict_menu.sql",
    "sql/005_work_order_process_review_menu.sql",
    "sql/006_statistics_export_menu.sql",
    "sql/007_remove_unused_module_menus.sql",
    "sql/008_xiaokang_branding.sql",
    "sql/009_hide_upstream_website_menu.sql",
    "sql/010_remove_unused_framework_data.sql",
]

EXPECTED_TABLES = frozenset({
    'eq_equipment', 'eq_work_order', 'eq_work_order_record',
    'sys_config', 'sys_dept', 'sys_dict_data', 'sys_dict_type',
    'sys_logininfor', 'sys_menu', 'sys_oper_log', 'sys_post',
    'sys_role', 'sys_role_dept', 'sys_role_menu', 'sys_user',
    'sys_user_post', 'sys_user_role',
})


def option_value(value):
    return '"' + value.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r') + '"'


def initialize(mysql, host, port, user, database, password):
    if host not in {"localhost", "127.0.0.1", "::1"}:
        raise ValueError("This initializer is for a local demonstration database only.")
    if not re.fullmatch(r"[A-Za-z0-9_]+", database):
        raise ValueError("Database name must contain only letters, numbers and underscores.")
    for relative in SQL_FILES:
        if not (ROOT / relative).is_file():
            raise FileNotFoundError(relative)
    # Pass credentials through a short-lived option file, never the command line.
    with tempfile.TemporaryDirectory(prefix="equipment-mysql-") as folder:
        config = Path(folder) / "client.cnf"
        config.write_text("[client]\npassword=" + option_value(password) + "\n", encoding="utf-8")
        command = [mysql, "--defaults-extra-file=" + str(config), "--protocol=TCP",
                   "--host=" + host, "--port=" + str(port), "--user=" + user,
                   "--database=" + database, "--default-character-set=utf8mb4", "--batch", "--skip-column-names"]

        def run(sql):
            result = subprocess.run(command, input=sql, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            if result.returncode:
                # MySQL stderr can include submitted SQL. Do not echo statements or credentials.
                raise RuntimeError("MySQL operation failed. Check the connection, privileges and selected SQL file.")
            return result.stdout.decode("utf-8").strip()

        count = run(b"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE();")
        if count != "0":
            raise RuntimeError("Refusing to import: the selected database is not empty. No SQL was imported.")
        for relative in SQL_FILES:
            print("Importing " + relative, flush=True)
            run((ROOT / relative).read_bytes())
        tables = set(run(b"SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';").splitlines())
        if tables != EXPECTED_TABLES:
            raise RuntimeError("Unexpected tables after import; missing=" + str(sorted(EXPECTED_TABLES - tables))
                               + ", extra=" + str(sorted(tables - EXPECTED_TABLES)))
        print("Initialized 17 tables. No demonstration business data has been inserted.")
        return len(tables)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mysql", default="mysql")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", required=True)
    parser.add_argument("--database", required=True)
    args = parser.parse_args()
    try:
        initialize(args.mysql, args.host, args.port, args.user, args.database, getpass.getpass("MySQL password (initialization only): "))
    except (ValueError, RuntimeError, FileNotFoundError, OSError) as error:
        parser.exit(1, str(error) + "\nInvestigate a failed partial import in the disposable database; no automatic cleanup or retry is performed.\n")


if __name__ == "__main__":
    main()
