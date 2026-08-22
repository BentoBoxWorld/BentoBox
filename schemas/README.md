# Blueprint JSON Schemas

Machine-readable [JSON Schema](https://json-schema.org/) (draft 2020-12) definitions of BentoBox's on-disk blueprint formats:

- **`blueprint.schema.json`** — validates a `.blueprint` file (a single Blueprint object) or a bundle file. Also contains the shared `$defs` for blocks, spawners, entities, and display entities.
- **`blueprint-bundle.schema.json`** — validates a blueprint bundle file (`<uniqueId>.json` in a game mode's `blueprints/` folder) on its own.

The human-readable specification lives in the docs: [Blueprint File Format](https://docs.bentobox.world/en/latest/BentoBox/Blueprint-Format/).

## Validating a file

With [ajv](https://ajv.js.org/):

```bash
ajv validate --spec=draft2020 -s schemas/blueprint.schema.json -d island.blueprint
ajv validate --spec=draft2020 -s schemas/blueprint-bundle.schema.json -d default.json
```

Editors that support JSON Schema (VS Code, IntelliJ) can associate `*.blueprint` and bundle files with these schemas for inline validation and completion.

## Caveats

- ItemStacks are stored as YAML documents inside JSON strings (Bukkit `ConfigurationSerializable`); the schema treats them as opaque strings, so a schema-valid file can still fail to load if an embedded YAML document is malformed.
- Legacy `.blu` files are ZIP archives whose single entry is JSON that validates against `blueprint.schema.json`.
- The schemas describe what the current serializer emits. Keep them in sync with the `world.bentobox.bentobox.blueprints.dataobjects` classes when `@Expose`d fields change.
