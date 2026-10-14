package com.kevshake.gateway.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Loads and persists generated gateway secrets outside the repository tree.
 */
final class GatewayGeneratedDefaultsFileStore {

	static final String DEFAULTS_FILE_ENV = "GATEWAY_GENERATED_DEFAULTS_FILE";

	private static final SecureRandom RANDOM = new SecureRandom();

	private GatewayGeneratedDefaultsFileStore() {
	}

	/** Resolves the on-disk path from {@link #DEFAULTS_FILE_ENV} or the user home defaults location. */
	static Path resolveDefaultsFilePath(String configuredPath) {
		if (configuredPath != null && !configuredPath.isBlank()) {
			return Path.of(configuredPath.trim());
		}
		return Path.of(System.getProperty("user.home"), ".mastergateway", "generated-defaults.properties");
	}

	/** Reads persisted defaults; returns an empty map when the file does not exist. */
	static Map<GatewaySettingKind, String> load(Path file) throws IOException {
		Map<GatewaySettingKind, String> values = new LinkedHashMap<>();
		if (!Files.exists(file)) {
			return values;
		}
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(file)) {
			properties.load(input);
		}
		for (GatewaySettingKind kind : GatewaySettingKind.values()) {
			String value = properties.getProperty(kind.environmentVariable());
			if (value != null && !value.isBlank()) {
				values.put(kind, value.trim());
			}
		}
		return values;
	}

	/** Merges new values into the file, creating parent directories and owner-only permissions when possible. */
	static void save(Path file, Map<GatewaySettingKind, String> values) throws IOException {
		Properties properties = new Properties();
		if (Files.exists(file)) {
			try (InputStream input = Files.newInputStream(file)) {
				properties.load(input);
			}
		}
		for (Map.Entry<GatewaySettingKind, String> entry : values.entrySet()) {
			properties.setProperty(entry.getKey().environmentVariable(), entry.getValue());
		}
		Path parent = file.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		try (OutputStream output = Files.newOutputStream(file)) {
			properties.store(output, "Gateway generated defaults — do not commit");
		}
		restrictToOwnerOnly(file);
	}

	/** Generates a URL-safe base64 password of at least 24 characters. */
	static String generateStrongPassword() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Applies POSIX owner-read/write when the filesystem supports it. */
	private static void restrictToOwnerOnly(Path file) {
		try {
			Set<PosixFilePermission> permissions = EnumSet.of(
					PosixFilePermission.OWNER_READ,
					PosixFilePermission.OWNER_WRITE);
			Files.setPosixFilePermissions(file, permissions);
		} catch (UnsupportedOperationException | IOException ignored) {
			// Windows or restricted FS — best effort only.
		}
	}
}
