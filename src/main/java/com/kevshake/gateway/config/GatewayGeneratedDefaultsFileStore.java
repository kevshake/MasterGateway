package com.kevshake.gateway.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and persists generated gateway secrets outside the repository tree.
 */
final class GatewayGeneratedDefaultsFileStore {

	static final String DEFAULTS_FILE_ENV = "GATEWAY_GENERATED_DEFAULTS_FILE";

	private static final Set<PosixFilePermission> DIRECTORY_OWNER_ONLY = EnumSet.of(
			PosixFilePermission.OWNER_READ,
			PosixFilePermission.OWNER_WRITE,
			PosixFilePermission.OWNER_EXECUTE);

	private static final Set<PosixFilePermission> FILE_OWNER_READ_WRITE = EnumSet.of(
			PosixFilePermission.OWNER_READ,
			PosixFilePermission.OWNER_WRITE);

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final ConcurrentHashMap<String, Object> IN_PROCESS_LOCKS = new ConcurrentHashMap<>();

	private GatewayGeneratedDefaultsFileStore() {
	}

	/** Resolves the on-disk path from {@link #DEFAULTS_FILE_ENV} or the user home defaults location. */
	static Path resolveDefaultsFilePath(String configuredPath) {
		if (configuredPath != null && !configuredPath.isBlank()) {
			return Path.of(configuredPath.trim());
		}
		return Path.of(System.getProperty("user.home"), ".mastergateway", "generated-defaults.properties");
	}

	/** Reads and validates persisted defaults; returns empty when the file does not exist. */
	static Map<GatewaySettingKind, String> load(Path file) throws IOException {
		if (!Files.exists(file)) {
			return Map.of();
		}
		assertOwnedByCurrentUser(file);
		assertOwnerOnlyAccess(file);
		Path parent = file.getParent();
		if (parent != null) {
			assertOwnedByCurrentUser(parent);
			assertOwnerOnlyAccess(parent);
		}
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(file)) {
			properties.load(input);
		}
		Map<GatewaySettingKind, String> values = new LinkedHashMap<>();
		for (GatewaySettingKind kind : GatewaySettingKind.values()) {
			String entryKey = kind.environmentVariable();
			String value = properties.getProperty(entryKey);
			if (value == null || value.isBlank()) {
				continue;
			}
			value = value.trim();
			validateLoadedEntry(kind, entryKey, value);
			values.put(kind, value);
		}
		return values;
	}

	/**
	 * Under an exclusive lock, re-reads the file and generates only values still missing, then atomically persists.
	 */
	static Map<GatewaySettingKind, String> loadOrGenerateMissing(Path file, Set<GatewaySettingKind> missingKinds)
			throws IOException {
		if (missingKinds.isEmpty()) {
			return Map.of();
		}
		Path directory = ensureOwnerOnlyDirectory(file.getParent());
		Path lockFile = directory.resolve(file.getFileName() + ".lock");
		Object inProcessLock = IN_PROCESS_LOCKS.computeIfAbsent(lockFile.toString(), ignored -> new Object());
		synchronized (inProcessLock) {
			try (FileChannel lockChannel = FileChannel.open(
					lockFile,
					StandardOpenOption.CREATE,
					StandardOpenOption.WRITE)) {
				lockChannel.lock();
				Map<GatewaySettingKind, String> merged = new EnumMap<>(GatewaySettingKind.class);
				merged.putAll(load(file));
				Map<GatewaySettingKind, String> generatedNow = new EnumMap<>(GatewaySettingKind.class);
				for (GatewaySettingKind kind : missingKinds) {
					if (merged.containsKey(kind)) {
						continue;
					}
					String value = generateValue(kind);
					validateLoadedEntry(kind, kind.environmentVariable(), value);
					merged.put(kind, value);
					generatedNow.put(kind, value);
				}
				if (!generatedNow.isEmpty() || !Files.exists(file)) {
					atomicWriteProperties(file, merged);
				}
				return merged;
			}
		}
	}

	/** Generates a URL-safe base64 password of at least 24 characters. */
	static String generateStrongPassword() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Validates one loaded property entry according to its setting kind. */
	private static void validateLoadedEntry(GatewaySettingKind kind, String entryKey, String value) {
		switch (kind) {
			case ADMIN_PASSWORD, H2_DATASOURCE_PASSWORD -> GatewaySecretShapeValidator.validatePasswordEntry(entryKey, value);
			case ZONAL_PIN_KEY, DEFAULT_TERMINAL_PIN_KEY -> GatewaySecretShapeValidator.validateTdesKeyEntry(entryKey, value);
			case BANK_HOST -> GatewaySecretShapeValidator.validateBankHostEntry(entryKey, value);
		}
	}

	/** Creates a secret or host value for a missing setting kind. */
	private static String generateValue(GatewaySettingKind kind) {
		return switch (kind) {
			case ADMIN_PASSWORD, H2_DATASOURCE_PASSWORD -> generateStrongPassword();
			case ZONAL_PIN_KEY, DEFAULT_TERMINAL_PIN_KEY -> GatewayTdesKeyMaterialGenerator.generateDoubleLengthTdesHexKey();
			case BANK_HOST -> GatewayGeneratedDefaultsBootstrap.DEFAULT_BANK_HOST;
		};
	}

	/** Writes properties atomically with an owner-only temp file and fsync. */
	private static void atomicWriteProperties(Path file, Map<GatewaySettingKind, String> values) throws IOException {
		Path directory = ensureOwnerOnlyDirectory(file.getParent());
		Properties properties = new Properties();
		for (Map.Entry<GatewaySettingKind, String> entry : values.entrySet()) {
			properties.setProperty(entry.getKey().environmentVariable(), entry.getValue());
		}
		Path tempFile = createOwnerOnlyTempFile(directory);
		try {
			try (OutputStream output = Files.newOutputStream(tempFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
				properties.store(output, "Gateway generated defaults — do not commit");
				output.flush();
			}
			try (FileChannel channel = FileChannel.open(tempFile, StandardOpenOption.WRITE)) {
				channel.force(true);
			}
			applyOwnerOnlyFilePermissions(tempFile);
			Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			applyOwnerOnlyFilePermissions(file);
			tempFile = null;
		} finally {
			if (tempFile != null) {
				Files.deleteIfExists(tempFile);
			}
		}
	}

	/** Ensures the parent directory exists with owner-only access (0700 on POSIX). */
	private static Path ensureOwnerOnlyDirectory(Path directory) throws IOException {
		if (directory == null) {
			throw new IllegalStateException("[" + DEFAULTS_FILE_ENV + "] Generated defaults file path has no parent directory.");
		}
		if (Files.exists(directory)) {
			assertOwnedByCurrentUser(directory);
			applyOwnerOnlyDirectoryPermissions(directory);
			verifyOwnerOnlyDirectoryPermissions(directory);
			return directory;
		}
		try {
			FileAttribute<?> attrs = PosixFilePermissions.asFileAttribute(DIRECTORY_OWNER_ONLY);
			return Files.createDirectories(directory, attrs);
		} catch (UnsupportedOperationException ex) {
			Files.createDirectories(directory);
			applyOwnerOnlyDirectoryPermissions(directory);
			return directory;
		}
	}

	/** Applies owner-only permissions to a regular file (0600 on POSIX, owner-only ACL on Windows). */
	private static void applyOwnerOnlyFilePermissions(Path file) throws IOException {
		try {
			Files.setPosixFilePermissions(file, FILE_OWNER_READ_WRITE);
		} catch (UnsupportedOperationException ex) {
			applyOwnerOnlyAcl(file, false);
		}
	}

	/** Applies owner-only permissions to a directory (0700 on POSIX, owner-only ACL on Windows). */
	private static void applyOwnerOnlyDirectoryPermissions(Path directory) throws IOException {
		try {
			Files.setPosixFilePermissions(directory, DIRECTORY_OWNER_ONLY);
		} catch (UnsupportedOperationException ex) {
			applyOwnerOnlyAcl(directory, true);
		}
	}

	/** Creates a temp file in the target directory with owner-only permissions when supported. */
	private static Path createOwnerOnlyTempFile(Path directory) throws IOException {
		try {
			FileAttribute<?> ownerWriteOnly = PosixFilePermissions.asFileAttribute(FILE_OWNER_READ_WRITE);
			return Files.createTempFile(directory, ".generated-defaults-", ".tmp", ownerWriteOnly);
		} catch (UnsupportedOperationException ex) {
			Path tempFile = Files.createTempFile(directory, ".generated-defaults-", ".tmp");
			applyOwnerOnlyFilePermissions(tempFile);
			return tempFile;
		}
	}

	/** Verifies the path is owned by the current JVM user. */
	private static void assertOwnedByCurrentUser(Path path) throws IOException {
		UserPrincipal owner = Files.getOwner(path);
		UserPrincipal current = path.getFileSystem()
				.getUserPrincipalLookupService()
				.lookupPrincipalByName(System.getProperty("user.name"));
		if (!owner.getName().equals(current.getName())) {
			throw new IllegalStateException("[" + DEFAULTS_FILE_ENV + "] Path is not owned by the current user: "
					+ path.getFileName());
		}
	}

	/** Verifies owner-only access using POSIX bits or an ACL with no non-owner ALLOW entries. */
	private static void assertOwnerOnlyAccess(Path path) throws IOException {
		if (supportsPosixPermissions(path)) {
			assertPosixOwnerOnly(path);
			return;
		}
		AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
		if (aclView == null) {
			throw unableToVerifyOwnerOnly(path);
		}
		assertOwnerOnlyAcl(aclView.getAcl(), aclView.getOwner(), path.getFileName());
	}

	/** Verifies a directory is not accessible to group/other on POSIX or via widened ACL entries. */
	private static void verifyOwnerOnlyDirectoryPermissions(Path directory) throws IOException {
		assertOwnerOnlyAccess(directory);
	}

	/** Returns true when POSIX permission APIs are supported for this path. */
	private static boolean supportsPosixPermissions(Path path) {
		try {
			Files.getPosixFilePermissions(path);
			return true;
		} catch (UnsupportedOperationException ex) {
			return false;
		} catch (IOException ex) {
			return false;
		}
	}

	/** Verifies no group/other permission bits are set on POSIX filesystems. */
	private static void assertPosixOwnerOnly(Path path) throws IOException {
		Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
		boolean directory = Files.isDirectory(path);
		if (permissions.contains(PosixFilePermission.GROUP_READ)
				|| permissions.contains(PosixFilePermission.OTHERS_READ)
				|| permissions.contains(PosixFilePermission.GROUP_WRITE)
				|| permissions.contains(PosixFilePermission.OTHERS_WRITE)
				|| (directory && (permissions.contains(PosixFilePermission.GROUP_EXECUTE)
						|| permissions.contains(PosixFilePermission.OTHERS_EXECUTE)))) {
			throw new IllegalStateException("[" + DEFAULTS_FILE_ENV + "] Path must be owner-only: " + path.getFileName());
		}
	}

	/** Rejects ACLs that grant ALLOW to any principal other than the file owner (Windows and ACL-only stores). */
	static void assertOwnerOnlyAcl(List<AclEntry> acl, UserPrincipal owner, java.nio.file.Path fileName) {
		for (AclEntry entry : acl) {
			if (entry.type() != AclEntryType.ALLOW) {
				continue;
			}
			if (!entry.principal().equals(owner)) {
				throw new IllegalStateException("[" + DEFAULTS_FILE_ENV
						+ "] Path must not grant access to other principals: "
						+ fileName);
			}
		}
	}

	/** Builds a startup failure when owner-only permissions cannot be verified on load or write. */
	private static IllegalStateException unableToVerifyOwnerOnly(Path path) {
		return new IllegalStateException("[" + DEFAULTS_FILE_ENV
				+ "] Unable to verify owner-only permissions for "
				+ path.getFileName());
	}

	/** Applies an owner-only ACL when POSIX permissions are unavailable. */
	private static void applyOwnerOnlyAcl(Path path, boolean directory) throws IOException {
		AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
		if (aclView == null) {
			throw unableToVerifyOwnerOnly(path);
		}
		UserPrincipal owner = aclView.getOwner();
		AclEntry.Builder entryBuilder = AclEntry.newBuilder()
				.setType(AclEntryType.ALLOW)
				.setPrincipal(owner)
				.setPermissions(EnumSet.allOf(AclEntryPermission.class));
		if (directory) {
			entryBuilder.setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT);
		}
		aclView.setAcl(List.of(entryBuilder.build()));
	}
}
