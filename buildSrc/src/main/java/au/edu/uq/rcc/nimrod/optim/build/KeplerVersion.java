package au.edu.uq.rcc.nimrod.optim.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeplerVersion {

	public final boolean isSource;
	public final int major;
	public final int minor;
	public final int patch;

	public final String version;
	public final String completeVersion;

	private final Path m_Path;

	private KeplerVersion(Path path, boolean isSource, int major, int minor, int patch) {
		this.m_Path = path;
		this.isSource = isSource;
		this.major = major;
		this.minor = minor;
		this.patch = patch;

		if(isSource) {
			this.version = "";
			this.completeVersion = "";
		} else {
			this.version = String.format("%d.%d", major, minor);
			this.completeVersion = String.format("%d.%d.%d", major, minor, patch);
		}
	}

	public String getModuleFolderName(String module) throws IOException {
		if(isSource) {
			if(!Files.exists(m_Path.resolve(module))) {
				throw new IOException();
			}

			return module;
		}

		ModuleInfo info = getModuleInfo(m_Path, module);
		return info.path.getFileName().toString();
	}

	private static class ModuleInfo {

		public final Path path;
		public final String name;
		public final int major;
		public final int minor;
		public final int patch;

		public ModuleInfo(Path path, String name, int major, int minor, int patch) {
			this.path = path;
			this.name = name;
			this.major = major;
			this.minor = minor;
			this.patch = patch;
		}
	}

	private static ModuleInfo getModuleInfo(Path path, String module) throws IOException {
		List<Path> matches = new ArrayList<Path>();

		Pattern pattern = Pattern.compile(String.format("%s-(\\d+).(\\d+)(?:.(\\d+))?", module));

		Files.find(path, 1, (Path t, BasicFileAttributes u) -> {
			if(t.equals(path) || !u.isDirectory()) {
				return false;
			}

			return pattern.matcher(t.getFileName().toString()).matches();
		}).forEach(matches::add);

		if(matches.size() != 1) {
			throw new IOException(String.format("Unable to determine version for module %s. Too many or no matches", module));
		}

		Matcher m = pattern.matcher(matches.get(0).getFileName().toString());
		if(!m.matches()) {
			/* Should never happen. */
			throw new IllegalStateException();
		}

		int major = Integer.parseInt(m.group(1));
		int minor = Integer.parseInt(m.group(2));

		int patch;
		String _patch = m.group(3);
		if(_patch == null) {
			patch = -1;
		} else {
			patch = Integer.parseInt(_patch);
		}

		return new ModuleInfo(matches.get(0), module, major, minor, patch);
	}

	public static KeplerVersion getKeplerVersion(Path path) throws IOException {
		/* Check for source installation. */
		if(Files.exists(path.resolve("build-area").resolve(".svn"))) {
			return new KeplerVersion(path, true, -1, -1, -1);
		}

		/* Check for actual installation. */
		ModuleInfo kepler = getModuleInfo(path, "kepler");

		return new KeplerVersion(path, false, kepler.major, kepler.minor, kepler.patch);
	}

	public static void main(String[] args) throws IOException {
		KeplerVersion v = KeplerVersion.getKeplerVersion(Paths.get("/media/Data/Programs/Kepler-2.4"));

		String nimkName = v.getModuleFolderName("nimrodk");

		int x = 0;
	}
};
