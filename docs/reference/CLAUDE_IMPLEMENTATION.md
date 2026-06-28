# CLAUDE Implementation Notes

> AIアシスタント（Claude Code）向けの要約リファレンスです。詳細は [IMPLEMENTING_COMMANDS.md](IMPLEMENTING_COMMANDS.md) を参照してください。

## API baseline
- `StreamConverter.run(...)` is `void`.
- Build pipelines with `StreamConverter.create(...)`.

## Context model
- Use `PipelineContext` for shared per-run state.
- Use `MdcPropagatingRule` to write extracted values into context/MDC.
- Use `PipelineContextTurboFilter` in Logback for automatic MDC synchronization.

## Implementation guidance
- Prefer streaming commands and avoid loading full input into memory.
- Keep command execution side-effect minimal and deterministic.
- Validate all external inputs (headers, path expressions, schema paths).
