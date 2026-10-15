"""Create 10 devices, 20 work orders and 5 single-role accounts in an empty local demo app."""
import argparse
import base64
import datetime
import getpass
import json
import secrets
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ACTORS = [
    ("reporter01", "模拟报修甲", "eq_reporter"),
    ("reporter02", "模拟报修乙", "eq_reporter"),
    ("maintainer01", "模拟维护甲", "eq_maintainer"),
    ("maintainer02", "模拟维护乙", "eq_maintainer"),
    ("manager01", "模拟设备管理", "eq_manager"),
]


class DemoClient:
    def __init__(self, base_url, output_dir, captcha_reader=None):
        parsed = urllib.parse.urlparse(base_url)
        if parsed.scheme != "http" or parsed.hostname not in {"localhost", "127.0.0.1", "::1"} or parsed.path not in {"", "/"} or parsed.query or parsed.fragment or parsed.username:
            raise ValueError("Use the loopback HTTP backend URL, for example http://127.0.0.1:8080")
        self.base_url = base_url.rstrip("/")
        self.output_dir = Path(output_dir)
        self.captcha_reader = captcha_reader or self.prompt_captcha
        self.sessions = []
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

    def request(self, method, path, data=None, token=None):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        body = json.dumps(data, ensure_ascii=False).encode("utf-8") if data is not None else None
        request = urllib.request.Request(self.base_url + path, data=body, headers=headers, method=method)
        with self.opener.open(request, timeout=30) as response:
            value = json.load(response)
        if value.get("code") != 200:
            raise RuntimeError(path + ": " + str(value.get("code")) + " " + value.get("msg", "Request failed"))
        return value

    def prompt_captcha(self, captcha):
        image = self.output_dir / "login-captcha.png"
        image.write_bytes(base64.b64decode(captcha["img"]))
        print("Open the local captcha image: " + str(image))
        return input("Captcha answer: ").strip()

    def login(self, username, password):
        captcha = self.request("GET", "/captchaImage")
        code = self.captcha_reader(captcha) if captcha.get("captchaEnabled") else ""
        token = self.request("POST", "/login", {"username": username, "password": password, "code": str(code), "uuid": captcha.get("uuid", "")})["token"]
        self.sessions.append(token)
        return token

    def logout(self):
        failed = False
        for token in self.sessions:
            try:
                self.request("POST", "/logout", token=token)
            except Exception:
                failed = True
        self.sessions.clear()
        if failed:
            print("Some initialization sessions could not be logged out; they will expire normally.")


def seed(client, admin_password):
    """Use normal business APIs so demonstration histories obey the real workflow."""
    accounts_file = client.output_dir / "demo-accounts.json"
    if accounts_file.exists():
        raise RuntimeError("Output already contains demo-accounts.json; refusing to overwrite credentials or rerun initialization.")
    client.output_dir.mkdir(parents=True, exist_ok=True)
    try:
        admin = client.login("admin", admin_password)
        for path in ["/equipment/asset/list", "/equipment/workorder/list"]:
            if client.request("GET", path, token=admin)["total"] != 0:
                raise RuntimeError("Refusing to seed: business data already exists. No demo accounts or business records were created.")
        if client.request("GET", "/system/user/list?pageSize=1", token=admin)["total"] != 2:
            raise RuntimeError("Demo initialization expects only the two original framework accounts. Use a new demo database.")
        roles = {row["roleKey"]: row["roleId"] for row in client.request("GET", "/system/role/list?pageSize=100", token=admin)["rows"]}
        if not all(role in roles for _, _, role in ACTORS):
            raise RuntimeError("Business roles are missing. Import SQL 001 through 006 first.")
        credentials = {"baseUrl": client.base_url, "accounts": []}
        for name, nickname, role in ACTORS:
            credentials["accounts"].append({"username": name, "nickname": nickname, "role": role, "password": "Demo-" + secrets.token_urlsafe(9) + "!"})
        # Record the generated local passwords before creating accounts; never print or commit them.
        with accounts_file.open("x", encoding="utf-8") as stream:
            json.dump(credentials, stream, ensure_ascii=False, indent=2)
        actors = {}
        for account in credentials["accounts"]:
            name = account["username"]
            client.request("POST", "/system/user", {"userName": name, "nickName": account["nickname"], "password": account["password"], "deptId": 100,
                           "roleIds": [roles[account["role"]]], "status": "0", "remark": "本地演示模拟账号，非真实人员"}, admin)
            users = client.request("GET", "/system/user/list?userName=" + name, token=admin)["rows"]
            ident = next(row["userId"] for row in users if row["userName"] == name)
            actors[name] = {"id": str(ident), "token": client.login(name, account["password"])}
        manager = actors["manager01"]["token"]
        devices = []
        types = ["MOTOR", "TEST_BENCH", "ACQUISITION", "OTHER"]
        for index in range(10):
            device = client.request("POST", "/equipment/asset", {"equipmentCode": f"DEMO-EQ-{index + 1:03}", "equipmentName": f"模拟设备{index + 1:02}", "equipmentType": types[index % 4],
                                     "model": "DEMO-ONLY", "location": "模拟实验室A" if index < 5 else "模拟实验室B", "remark": "P6可复现演示数据，非真实设备"}, manager)["data"]
            devices.append(device)
        orders = []
        occurred = (datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))) - datetime.timedelta(hours=1)).strftime("%Y-%m-%d %H:%M:%S")
        categories = ["MECHANICAL", "ELECTRICAL", "SOFTWARE", "OTHER"]
        status_names = ["待受理", "处理中", "待复核", "已关闭", "已取消"]
        for index in range(20):
            actor_number = index % 2 + 1
            reporter = actors[f"reporter0{actor_number}"]["token"]
            maintainer = actors[f"maintainer0{actor_number}"]
            stage = index % 5
            order = client.request("POST", "/equipment/workorder", {"equipmentId": devices[index // 2]["equipmentId"], "title": f"模拟工单{index + 1:02} · {status_names[stage]}",
                                   "symptom": "模拟现象：读数波动，供流程演示使用，不代表真实检测结论。", "faultCategory": categories[index % 4], "priority": "HIGH" if index % 3 == 0 else "NORMAL",
                                   "occurredTime": occurred, "confirmExisting": True}, reporter)["data"]
            path = "/equipment/workorder/" + order["workOrderId"]
            if stage == 4:
                order = client.request("POST", path + "/cancel", {"version": order["version"], "reason": "模拟重复登记，保留取消记录"}, reporter)["data"]
            elif stage > 0:
                order = client.request("POST", path + "/assign", {"version": order["version"], "handlerId": maintainer["id"], "description": "模拟分配维护任务"}, manager)["data"]
                order = client.request("POST", path + "/records", {"version": order["version"], "description": "模拟排查：检查连接与采集设置"}, maintainer["token"])["data"]
                if stage >= 2:
                    def submit(current, result):
                        return client.request("POST", path + "/submit-review", {"version": current["version"], "causeAnalysis": "待确认，模拟判断为连接接触异常", "measures": "模拟重新连接并复查设置", "resultDescription": result}, maintainer["token"])["data"]
                    order = submit(order, "模拟第一轮读数稳定")
                    if index == 3:
                        order = client.request("POST", path + "/review", {"version": order["version"], "decision": "REJECT", "description": "模拟退回：补充通道对比结果"}, manager)["data"]
                        order = client.request("POST", path + "/records", {"version": order["version"], "description": "模拟补充通道对比检查"}, maintainer["token"])["data"]
                        order = submit(order, "模拟第二轮：对比检查完成，读数稳定")
                    if stage == 3:
                        order = client.request("POST", path + "/review", {"version": order["version"], "decision": "APPROVE", "description": "模拟复核通过，仅验证记录流程"}, manager)["data"]
            orders.append(order)
        for index, status in [(8, "OUT_OF_SERVICE"), (9, "RETIRED")]:
            devices[index] = client.request("PUT", "/equipment/asset/" + devices[index]["equipmentId"] + "/status", {"version": devices[index]["version"], "equipmentStatus": status}, manager)["data"]
        overview = client.request("GET", "/equipment/statistics/overview", token=manager)["data"]
        expected = {"WAITING": 4, "PROCESSING": 4, "PENDING_REVIEW": 4, "CLOSED": 4, "CANCELLED": 4}
        if overview["equipmentTotal"] != 10 or overview["orderTotal"] != 20 or overview["openOrderCount"] != 12 or {row["code"]: row["count"] for row in overview["orderStatuses"]} != expected:
            raise RuntimeError("Seeded counts did not match the planned demonstration. Inspect the isolated database.")
        result = {"createdAt": datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat(), "deviceCount": 10, "workOrderCount": 20,
                  "statusCounts": expected, "roundTripOrder": orders[3]["workOrderNo"], "devices": devices, "orders": orders, "statistics": overview}
        (client.output_dir / "demo-manifest.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
        print("Created 10 simulated devices, 20 work orders and 5 single-role accounts.")
        print("Local account passwords: " + str(accounts_file))
        print("All API initialization sessions will be logged out. Accounts remain enabled for your local demonstration.")
        return result
    finally:
        client.logout()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--output-dir", type=Path, default=ROOT / ".local" / "demo")
    args = parser.parse_args()
    try:
        seed(DemoClient(args.base_url, args.output_dir), getpass.getpass("Initial admin password (initialization only): "))
    except (RuntimeError, ValueError, OSError, StopIteration) as error:
        parser.exit(1, str(error) + "\nNo automatic deletion or credential reset was performed. A partial run requires inspection of the disposable demo database.\n")


if __name__ == "__main__":
    main()
