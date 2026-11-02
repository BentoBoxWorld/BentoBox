# bStats snapshots

Snapshots of the BentoBox bStats custom charts, taken every six hours by the
`bStats snapshot` workflow on the `develop` branch. bStats itself keeps no history
for pie and bar charts, so this branch is the only record of how they change.

One file per snapshot under `snapshots/<year>/<month>/`, each a JSON object with the
UTC `timestamp` and a `charts` map of chart id to the raw API response.
