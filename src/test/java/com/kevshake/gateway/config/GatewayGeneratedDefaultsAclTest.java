package com.kevshake.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GatewayGeneratedDefaultsAclTest {

	@TempDir
	Path tempDir;

	/** Ensures a second persistence pass only generates settings still missing on disk. */
	@Test
	void secondSaveAddsMissingSettingWhileKeepingExistingValues() throws Exception {
		Path defaultsFile = tempDir.resolve("generated-defaults.properties");
		Set<GatewaySettingKind> adminOnly = EnumSet.of(GatewaySettingKind.ADMIN_PASSWORD);
		String admin = GatewayGeneratedDefaultsFileStore.loadOrGenerateMissing(defaultsFile, adminOnly)
				.get(GatewaySettingKind.ADMIN_PASSWORD);
		assertNotNull(admin);

		Set<GatewaySettingKind> h2Only = EnumSet.of(GatewaySettingKind.H2_DATASOURCE_PASSWORD);
		String h2 = GatewayGeneratedDefaultsFileStore.loadOrGenerateMissing(defaultsFile, h2Only)
				.get(GatewaySettingKind.H2_DATASOURCE_PASSWORD);
		assertNotNull(h2);

		var reloaded = GatewayGeneratedDefaultsFileStore.load(defaultsFile);
		assertEquals(admin, reloaded.get(GatewaySettingKind.ADMIN_PASSWORD));
		assertEquals(h2, reloaded.get(GatewaySettingKind.H2_DATASOURCE_PASSWORD));
	}

	/** Rejects simulated ACL entries that ALLOW a non-owner principal (Windows-style ACL review). */
	@Test
	void rejectsSimulatedWidenedAllowAcl() {
		NamedUser owner = new NamedUser("owner");
		NamedUser other = new NamedUser("BUILTIN\\Users");
		List<AclEntry> widened = List.of(
				ownerAllowEntry(owner),
				ownerAllowEntry(other));
		IllegalStateException failure = assertThrows(
				IllegalStateException.class,
				() -> GatewayGeneratedDefaultsFileStore.assertOwnerOnlyAcl(
						widened,
						owner,
						Path.of("generated-defaults.properties")));
		assertTrue(failure.getMessage().contains(GatewayGeneratedDefaultsFileStore.DEFAULTS_FILE_ENV));
		assertTrue(failure.getMessage().contains("generated-defaults.properties"));
		assertTrue(failure.getMessage().contains("other principals"));
	}

	/** On ACL-only filesystems, rejects a widened on-disk ACL when loading persisted defaults. */
	@Test
	void rejectsWidenedAclOnLoadWhenAclFilesystemSupported() throws Exception {
		assumeTrue(isAclOnlyFilesystem(tempDir));
		Path defaultsFile = tempDir.resolve("generated-defaults.properties");
		GatewayGeneratedDefaultsFileStore.loadOrGenerateMissing(
				defaultsFile,
				EnumSet.of(GatewaySettingKind.ADMIN_PASSWORD));

		AclFileAttributeView aclView = Files.getFileAttributeView(defaultsFile, AclFileAttributeView.class);
		assumeTrue(aclView != null);
		UserPrincipal owner = aclView.getOwner();
		UserPrincipal other = resolveSecondaryPrincipal(defaultsFile, owner);
		assumeTrue(other != null);

		List<AclEntry> widened = new ArrayList<>(aclView.getAcl());
		widened.add(AclEntry.newBuilder()
				.setType(AclEntryType.ALLOW)
				.setPrincipal(other)
				.setPermissions(AclEntryPermission.READ_DATA)
				.build());
		aclView.setAcl(widened);

		IllegalStateException failure = assertThrows(
				IllegalStateException.class,
				() -> GatewayGeneratedDefaultsFileStore.load(defaultsFile));
		assertTrue(failure.getMessage().contains(GatewayGeneratedDefaultsFileStore.DEFAULTS_FILE_ENV));
		assertTrue(failure.getMessage().contains(defaultsFile.getFileName().toString()));
	}

	/** True when POSIX permission APIs are unavailable but an ACL view can be opened on a new file. */
	private static boolean isAclOnlyFilesystem(Path directory) throws IOException {
		if (supportsPosixPermissions(directory)) {
			return false;
		}
		Path probe = directory.resolve(".acl-probe");
		Files.createFile(probe);
		try {
			return Files.getFileAttributeView(probe, AclFileAttributeView.class) != null;
		} finally {
			Files.deleteIfExists(probe);
		}
	}

	/** Mirrors production detection of POSIX permission support for a path. */
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

	/** Resolves a secondary principal name commonly present on Windows ACL filesystems. */
	private static UserPrincipal resolveSecondaryPrincipal(Path context, UserPrincipal owner) {
		var lookup = context.getFileSystem().getUserPrincipalLookupService();
		for (String candidate : List.of("Everyone", "BUILTIN\\Users", "Users")) {
			try {
				UserPrincipal principal = lookup.lookupPrincipalByName(candidate);
				if (!principal.equals(owner)) {
					return principal;
				}
			} catch (IOException ignored) {
				// try next candidate
			}
		}
		return null;
	}

	/** Builds an ALLOW ACL entry for unit tests and widened-ACL integration probes. */
	private static AclEntry ownerAllowEntry(UserPrincipal principal) {
		return AclEntry.newBuilder()
				.setType(AclEntryType.ALLOW)
				.setPrincipal(principal)
				.setPermissions(EnumSet.allOf(AclEntryPermission.class))
				.build();
	}

	private record NamedUser(String name) implements UserPrincipal {

		@Override
		public String getName() {
			return name;
		}
	}
}
