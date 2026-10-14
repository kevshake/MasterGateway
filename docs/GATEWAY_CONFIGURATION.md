# Gateway configuration map

Never log or commit production secret values. Historical sample PIN keys and bank host must not be set explicitly (startup fails on known sample fingerprints).

| Setting | Property key | Environment variable | Must also match | How to set |
|---------|--------------|----------------------|-----------------|------------|
| Gateway admin (H2 Basic) | `spring.security.user.password` | `GATEWAY_ADMIN_PASSWORD` | Pod/VM secret, CI job env (`openssl rand` in `.github/workflows/ci.yml`) | `openssl rand -base64 32` → export before start |
| H2 datasource | `spring.datasource.password` | `H2_DATASOURCE_PASSWORD` | Pod/VM secret, CI job env (same workflow step) | Unique per environment; min 16 chars |
| Zonal PIN key | `iso8583.security.gateway-zonal-key` | `GATEWAY_ZONAL_KEY` | Bank/switch HSM ceremony record, live switch PIN zone | HSM export / key ceremony; hex TDES key |
| Default terminal PIN key | `iso8583.security.default-terminal-key` | `GATEWAY_DEFAULT_TERMINAL_KEY` | `TerminalKey` table, terminal/SDK provisioning | Key injection per terminal fleet |
| Bank/switch host | `iso8583.bank.host` | `ISO8583_BANK_HOST` | `src/main/resources/deploy/21_bank_channel.xml`, bank TLS/mTLS allow-list | DNS or IP of switch endpoint |

## Missing values (any profile)

When any of the five environment variables above is **unset** at startup, the gateway:

1. Loads prior install defaults from `GATEWAY_GENERATED_DEFAULTS_FILE` (default: `~/.mastergateway/generated-defaults.properties`, owner-only permissions when supported).
2. Generates any still-missing secrets once per install (`SecureRandom` passwords; double-length TDES hex keys with odd parity; bank host **`127.0.0.1`**).
3. Persists new secrets to that file (never logged) and injects them into the Spring environment.
4. Prints and logs a **banner per defaulted setting** (setting name, env var, alignment points, how to set). Repeats the same text at **WARN every hour** until operators export real values.

Actuator `/actuator/health` remains **status-only** (no flag for generated defaults).

## Explicit weak or sample values

If an operator **sets** a password, key, or host that is too short, blocklisted, or matches a known repository sample fingerprint, startup **still fails** with `[env / property]` messages from `GatewayEnvironmentValidator`.

## Tests

See `GatewayGeneratedDefaultsTest` and `GatewayEnvironmentValidatorTest` for persistence, reuse, loopback bank host, weak-value failure, and banner text that never contains secret values.
