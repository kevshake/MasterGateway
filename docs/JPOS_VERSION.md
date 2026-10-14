# jPOS version selection (2.1.10)

## Why not jPOS 3.x

[jPOS 3.0.0](https://github.com/jpos/jPOS/releases/tag/v3.0.0) ships bytecode compiled for **Java 22** (class file major version 66). This gateway targets **Java 21 LTS**, so jPOS 3.x cannot be loaded on the supported runtime (`UnsupportedClassVersionError` on `org.jpos.iso.ISOPackager`).

## Why 2.1.10

| Artifact | Version | Notes |
|----------|---------|--------|
| `org.jpos:jpos` | **2.1.10** | Latest 2.1.x line compatible with Java 21 |
| Bouncy Castle (`bcprov-lts8on`, `bcpg-lts8on`, `bcutil-lts8on`) | **2.73.13** | Managed override (jPOS 2.1.10 pulls 2.73.6) |
| JLine | **4.4.8** | Managed override (jPOS pulls 3.23.0) |
| Apache MINA SSHD (`sshd-core`) | **2.20.0** | Managed override (jPOS optional SFTP stack) |
| Netty | **4.2.19.Final** | Spring Boot BOM override for transport CVE fixes |

`com.sleepycat:je` is **excluded**: optional Berkeley DB Java Edition store unused by this gateway (terminal state uses H2/JPA).

## Q2 deploy / channel configs on jPOS 2.1.10 + Netty 4.2.x

- **Netty POS path (`IsoServer`, `IsoMessageDecoder`/`Encoder`, `IsoServerHandler`)** is covered by `Iso8583MessagePipelineTest` (EmbeddedChannel round trips 0200→0210, 0400→0410, invalid PAN error).
- **Q2 XML deploy descriptors** under `src/main/resources/deploy/` and `jpos-config.xml` are **not** auto-started in tests (`Q2IntegrationService` prepares deploy dir only; full Q2 mux/channel wiring is unchanged from pre-upgrade but **not exercised** in CI). Manual/sign-off testing is required for bank/POS channel XML against a live switch.
