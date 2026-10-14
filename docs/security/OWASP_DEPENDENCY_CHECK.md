# OWASP Dependency-Check evidence

Command (CI and local):

```bash
export GATEWAY_ADMIN_PASSWORD='…'   # required for Maven/JVM env (CI sets this)
export H2_DATASOURCE_PASSWORD='…'
./mvnw org.owasp:dependency-check-maven:check
```

Configuration (`pom.xml`):

- `failBuildOnCVSS=7` (fails on high/critical)
- `ossindexAnalyzerEnabled=false` — Sonatype OSS Index returns HTTP 401 without project credentials; NVD cache is used instead
- `assemblyAnalyzerEnabled=false` — no .NET runtime in CI/Linux build images

Committed reports (2026-10-14, after Netty **4.2.19.Final** and SSHD **2.20.0** overrides):

- [dependency-check-report.html](./dependency-check-report.html)
- [dependency-check-report.json](./dependency-check-report.json)

## Result summary

| Critical | High | Medium | Low |
|---------:|-----:|-------:|----:|
| **0** | **0** | **0** | **0** |

Previous medium findings (prior to Netty/SSHD bumps) for audit trail:

| CVE | Package | Reachability |
|-----|---------|--------------|
| CVE-2026-89044 | `io.netty:netty-transport` 4.2.17 | **Runtime** — POS ISO8583 Netty server and Tomcat; fixed via `netty.version=4.2.19.Final`. |
| CVE-2023-48795 | `org.apache.sshd:sshd-core` 2.9.2 (jPOS transitive) | **Latent** — jPOS SFTP optional stack, not invoked by this gateway; forced to **2.20.0**. |
| CVE-2023-35887 | `org.apache.sshd:sshd-core` 2.9.2 (jPOS transitive) | Same as above. |

Re-run the check after dependency changes and refresh the committed HTML/JSON artifacts.
