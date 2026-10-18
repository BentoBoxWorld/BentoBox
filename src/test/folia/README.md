# Lophine runtime probe

**Destructive: use a disposable server, never production worlds.** The probe writes blocks,
regenerates fresh chunks, creates bots and islands, and resets player islands.

Build with Java 25:

```powershell
.\gradlew.bat test shadowJar foliaProbeJar -I src/test/folia/probe.gradle --no-daemon
```

Install BSkyBlock 1.20.0, the built BentoBox jar and
`build/folia-probe/BentoBoxFoliaProbe.jar` on a disposable Lophine 26.3 build 864
Paperclip server. Enable `function.fakeplayer.enable` in
`lophine_config/lophine_global_config.toml`; allow at least four bots.
Use a loopback-only listening address. Accept the Minecraft EULA yourself.

The source plugin descriptor does not yet advertise Folia support. For this isolated
probe only, append `folia-supported: true` to `plugin.yml` inside the copied BentoBox
jar, not to the source descriptor. This is a test opt-in, not a compatibility claim.

Run twice with the same disposable worlds and plugin data. Require
`FOLIA_PROBE_PASSED` and a normal exit on both runs. On the second run also require
`PLAYER_RESTART_PERSISTENCE_PASSED` and `RESTART_PERSISTENCE_PASSED`.
Lophine's bot-saving shutdown prompt may require its `stop force` command.

## Coverage

- Cross-region blueprint paste, owner-thread snapshots and safe entity teleport.
- Clipboard copy without changing source blocks.
- Fresh Overworld, Nether and End terrain, biomes and configured Nether roof.
- Custom-generator chunk regeneration, with unsupported vanilla stages rejected
  before modifying existing terrain.
- Configure `stress-players` (2–16 owners, each with one visitor) and `stress-resets`
  (1–100 sequential resets per owner). Owners run concurrently in distant regions;
  reset and total workload durations are logged in milliseconds. Ensure the bot
  limit is at least twice `stress-players`. Defaults are two owners and one reset.
- Bots create islands, teleport, check visitor block-break protection and reset.
- Database/world-marker persistence and player-island records after restart.
- Global world save and rejection of eviction of a plugin-ticket-pinned chunk.

## Not a full compatibility certification

Other addons and their custom location strategies/generators, real-client GUIs,
economy integrations, prolonged load and online region-file purge are not covered.
Online purge has no proven atomic exclusion of concurrent chunk loads; physical
region-file deletion is therefore rejected on Folia before touching any files.
The pinned-chunk probe verifies eviction rejection only, not
successful physical file deletion. Vanilla terrain/populator regeneration is
explicitly unsupported rather than risking terrain loss.

## Latest local validation (not release approval)

- Lophine 26.3 build 864, BSkyBlock 1.20.0, Java 25: 8 owners and 8 visitors,
  20 resets per owner, 160 completed resets. Workload including cancellation
  checks took 91.263 seconds; successful reset completion latency was
  3.058–5.112 seconds (mean 3.787 seconds).
  These are application completion times, not TPS/MSPT or client latency.
- Latest full unit suite: 3733 tests, 0 failures/errors, 13 skipped.
- Release jar contains no probe/JUnit/Mockito classes and does not advertise Folia support.
- Still unresolved: failures after leave cleanup starts can have irreversible
  inventory/economy/command side effects; charging after disconnect needs a
  complete transaction review.
  Other shared command state and collections, portal/respawn integration, economy,
  other addons/database backends and real-client GUIs remain unverified.
- This short multi-reset run is not prolonged stress testing or full compatibility.

### Extended run: workload completed, overall validation failed

- 8 owners and 8 visitors completed 800 resets and 16 cancellation checks in
  406.059 seconds. Reset completion latency: 3.098–5.010 seconds, mean 3.902.
- 41 process samples had no sampling errors; peak working set was 2,816,417,792
  bytes (2.62 GiB). CPU time at the last sample was 2,752.72 seconds across cores;
  this is cumulative process CPU time, not a utilization percentage.
- Sampled lowest-region TPS ranged from 18.39 to 19.67. Displayed busy-region
  MSPT samples ranged from 0.29 to 8.42; these are command snapshots, not p99 or
  full coverage of every region/tick, and do not establish long-term stability.
- Automatic housekeeping attempted an online deleted-region purge and logged
  three errors when the safety guard rejected it. No region files were deleted.
  Thus the run's workload probe passed, but the complete run did NOT pass the
  no-runtime-errors requirement. Do not treat its zero process exit as approval.
- The external runner now also fails on runtime error/exception log lines rather
  than accepting only the probe marker and normal server shutdown. This stricter
  runner returned exit code 1 on the repeat run: 800 resets and 16 cancellations
  completed in 398.588 seconds (reset latency 3.097–4.897 seconds, mean 3.793),
  but the same three housekeeping errors correctly failed overall validation.
  That repeat predates the payment-response and delayed-home-rename fixes; those
  fixes were included in the next live repeat: 800 resets and 16 cancellations
  completed in 404.163 seconds (reset latency 3.092–5.386 seconds, mean 3.886).
  The same three housekeeping errors again failed overall validation, exit code 1.
  The subsequent admin settings/teleport/disband/unregister changes have unit/build
  coverage only, not a fresh live command integration pass.

## Additional command regressions

- Failed/cancelled reset attempts do not consume reset allowance. Completed Folia
  resets count even if the later player task cannot run after disconnect.
- Cancelled synchronous create/reset does not trigger charging; cancelled create
  does not start the reset cooldown. This does not fix the full economy transaction.
- A delayed reset confirmation cannot clear the old team while another creation
  is in progress for that player.
- Promotion/demotion, settings panels (template and fallback), and banlist commands
  no longer share a mutable target between players. Rank changes and banlist
  reads recheck current membership/permissions as appropriate.
- These command failure/interleaving checks are unit tests, not real-client GUI
  or live economy integration tests; their presence is not a transaction guarantee.
- Admin rank-setting and island-name-reset commands now resolve each invocation
  independently and revalidate at execution. Coop/trust grants no longer share a
  target UUID and recheck the caller's current rank before granting access.
- Admin undelete also resolves its current island at execution and rechecks the
  pending-deletion status before clearing ownership. A cancellation exception
  after reset commit no longer discards the new recovery island as a preclear veto.
- Purge-protection toggles and home-rename prompts no longer share a target
  island. Protection toggles serialize their read/change pair. Home rename checks
  the user's island and required rank again on the player thread when chat input
  commits; leaving the team or a raised rank requirement cannot reuse stale access.
- Admin settings resolve island, flag and value per invocation, including both
  panel paths; execution rejects removed islands/flags rather than reporting success.
- Admin user teleport resolves its player and destination per invocation and
  rechecks disconnects/missing islands. Its success notification uses the admin's
  player scheduler rather than the teleportee's thread.
- Admin team disband no longer shares a team/owner between calls. Its multi-island
  prompt does not dereference an uninitialized island; rank-change events identify
  the removed member and retain their rank from before removal.
- Admin unregister confirmations capture their selected island/owner and recheck
  ownership/index membership before changing it. Interleaved commands cannot
  redirect a pending confirmation to another island.
- Protection-center and bonus-range removal confirmations capture their island,
  coordinates/player and bonus id separately. They reject replaced islands or
  changed player/island associations before mutation; bonus removal rechecks that
  the requested bonus exists. Protection-center rejects incomplete coordinates and
  console invocations without coordinates, and preserves failure reporting without
  requiring a non-null addon.
- Registration no longer shares a mutable island/player between invocations.
  Special-case registration confirmations reject changed ownership or replacement
  islands while retaining their selected island if the administrator moves.
- Team setowner no longer shares mutable invocation targets. Delayed confirmations
  retain the selected island/recipient/previous owner, and recheck ownership,
  island presence and the recipient's concurrent-island limit before mutation.
- Folia registration reservations load the chunk asynchronously and place their
  marker on its region thread. Completion waits for the marker write; a changed
  island, reservation flag or owner prevents the write. Marker failure retains
  the reservation and reports failure instead of deleting potentially changed data.
- These latest command changes have unit/build coverage only, not live command
  integration coverage. Reservation allocation is not yet atomic against concurrent
  creation, and failed markers have no automatic recovery. Cross-player permission
  reads/notifications during ownership transfer still need region-thread review;
  these findings prevent a full-compatibility claim.

## Staged Folia reset and remaining transaction findings

- Folia reset now selects/pastes terrain before old-team cleanup. Cleanup runs on
  each member's owner thread and all leave-reset futures complete before blueprint
  commands or join resets. Primary-island switching and death resets are deferred
  until after leave cleanup. Synchronous legacy entry points retain their old path.
- Before leave cleanup starts, failed terrain paste or a cancelled reset event
  soft-deletes a newly allocated draft on its region; reserved islands have their
  reservation restored instead. The creation lock waits for that cleanup. Deletion
  event vetoes are honored and reported, not bypassed by forcibly deleting data.
- Failures during/after old-team cleanup still need recovery. Leave commands,
  economy hooks and addon events can have irreversible effects; restoring only a
  members map would falsely claim to roll those back. Drafts are retained once
  leave cleanup starts rather than deleting the player's potential recovery target.
- `BlueprintCostHelper` now requires a successful withdrawal response; failure,
  NOT_IMPLEMENTED and null responses are reported rather than counted as payment.
  Payment itself uses the provider's withdrawal result, not a separate balance
  check. Synchronous commands return false on payment failure; asynchronous
  completion logs the generated island ID and player ID for recovery. Cooldowns
  and successful reset counts remain applied because the island already exists.
- Folia completion now schedules reset counts, cooldowns and charging globally,
  independently of a retired player scheduler. Delayed withdrawals capture the
  provider, offline account, world name and price before creation. A charge caches
  its outcome under a lock: repeated calls cannot withdraw twice, including when
  a provider throws after changing the balance. Null responses never trigger a
  second-account withdrawal; only explicit NOT_IMPLEMENTED permits fallback.
- Charging still happens after creation. This is NOT an atomic creation/payment
  transaction or durable exactly-once payment across crashes. Real provider thread
  requirements, affordability/account consistency and commit/refund recovery still
  need implementation or live verification; global scheduling alone does not
  establish that arbitrary economy providers are Folia-safe.
- Latest full core test/build after these changes: 3746 tests, 0 failures, 0 errors,
  13 skipped; `test shadowJar foliaProbeJar -I src/test/folia/probe.gradle` succeeded.
  This is unit/build evidence only, not a fresh isolated-server or economy test.
- Moving old-team cleanup after teleport is not a safe fix: it reverses leave/join
  resets and can clear the new island's inventory after it has been granted.
- The isolated probe additionally requests two cancelled resets per bot (RESET
  and PRECLEAR), for 16 verified cancellations in the latest live run. It checks
  original ownership, members, inventory, reset count and owned-island count before
  proceeding with the normal repeated resets. Both cancellation stages passed
  before all 160 successful resets completed without errors in the server log.
