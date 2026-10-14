# Gateway configuration map

Never log or commit production secret values. Sample PIN keys and bank host exist **only** in `application-local.yml` for `spring.profiles.active=local`.

| Setting | Property key | Environment variable | Must also match | How to set |
|---------|--------------|----------------------|-----------------|------------|
| Gateway admin (H2 Basic) | `spring.security.user.password` | `GATEWAY_ADMIN_PASSWORD` | CI GitHub Actions secret, runtime pod/VM env | `openssl rand -base64 32` → export before start |
| H2 datasource | `spring.datasource.password` | `H2_DATASOURCE_PASSWORD` | CI secret, runtime env | Unique per environment; min 16 chars |
| Zonal PIN key | `iso8583.security.gateway-zonal-key` | `GATEWAY_ZONAL_KEY` | Bank/switch HSM ceremony record, live switch PIN zone | HSM export / key ceremony; hex TDES key |
| Default terminal PIN key | `iso8583.security.default-terminal-key` | `GATEWAY_DEFAULT_TERMINAL_KEY` | `TerminalKey` table, terminal/SDK provisioning | Key injection per terminal fleet |
| Bank/switch host | `iso8583.bank.host` | `ISO8583_BANK_HOST` | `src/main/resources/deploy/21_bank_channel.xml`, bank TLS/mTLS allow-list | DNS or IP of switch endpoint |

Startup enforcement: `GatewayEnvironmentValidator` (`META-INF/spring.factories`, `org.springframework.boot.EnvironmentPostProcessor`) fails closed on missing/unsafe passwords, H2 console outside local-only profile, and sample PCI fingerprints outside local-only profile.
