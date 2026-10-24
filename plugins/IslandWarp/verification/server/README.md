# Test-server harness

Only deploy this helper on `D:\GongZhuo\IslandWarp-test` (loopback port 25619).
It requires IslandWarp, Vault, and PlaceholderAPI. Never deploy it on production.

`ServerCheck.java` creates synthetic Player/InventoryView implementations and uses the
real server to exercise menu construction, listener clicks, commands, SQLite, Folia
region loading/safety checks, and an isolated Vault provider with temporary balances.
Synthetic teleport transport deliberately returns failure to verify refund behavior.
No real Minecraft client connection or successful real-player teleport is tested.
Skyllia is not installed on this test server, so only its fail-closed behavior is verified.

The helper writes `runtime-ui.html` from actual Bukkit inventory items/Lore and removes
its temporary warps at the end. Runner stops the test server normally on completion.
Database history/audits produced by the test can remain: preserve an initial backup and
restore the original test database while the server is stopped, then remove the helper.

Run the compiled helper with `python verification/server/run_server.py`.
After removing the helper, use `--smoke` for final startup/status/reload/shutdown checks.
