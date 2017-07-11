package au.edu.uq.rcc.nimrod.optim.modules;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModuleLoader {

	public static void main(String[] args) throws MalformedURLException, IOException, Exception {
		List<Module> modules = new ArrayList<>();
		List<ModuleException> failed = new ArrayList<>();
		ModuleLoader.loadModulesFromPath(Paths.get("E:/Programs/kepler-2.4/nimrodok-2.4.0/lib/nimrodok-modules"), modules, failed);
		int x = 0;
	}

	public static void loadModulesFromPath(Path modulesFolder, List<Module> modules, List<ModuleException> failed) throws IOException {
		/* List all folders. */
		List<Path> moduleFolders = Files.list(modulesFolder).filter(new Predicate<Path>() {
			@Override
			public boolean test(Path t) {
				return Files.isDirectory(t);
			}

		}).collect(Collectors.toCollection(new Supplier<List<Path>>() {
			@Override
			public List<Path> get() {
				return new ArrayList<>();
			}

		}));

		/* Load the modules. */
		for(int i = 0; i < moduleFolders.size(); ++i) {
			Path f = moduleFolders.get(i);
			String modName = f.getFileName().toString();

			try {
				modules.add(loadModule(f, modName));
			} catch(Throwable e) {
				if(e instanceof ModuleException) {
					failed.add((ModuleException)e);
					continue;
				}

				failed.add(new ModuleException(modName, f, e.getMessage()));
			}
		}
	}

	private static Module loadModule(Path moduleFolder, String moduleName) throws ModuleException {
		Path propsFile = moduleFolder.resolve("module.properties");

		java.util.Properties moduleInfo = new java.util.Properties();
		try(BufferedReader br = Files.newBufferedReader(propsFile)) {
			moduleInfo.load(br);
		} catch(IOException e) {
			throw new ModuleException(moduleName, moduleFolder, "Error loading module.properties");
		}

		String mainClass = moduleInfo.getProperty("module.mainClass");
		if(mainClass == null) {
			throw new ModuleException(moduleName, moduleFolder, "Missing 'module.mainClass' directive");
		}

		String classpath = moduleInfo.getProperty("module.classpath");
		if(classpath == null) {
			throw new ModuleException(moduleName, moduleFolder, "Missing 'module.classpath' directive");
		}

		URL[] classPathURLs;

		try {
			classPathURLs = getURLs(moduleFolder, classpath);
		} catch(MalformedURLException e) {
			throw new ModuleException(moduleName, moduleFolder, "Invalid classpath entry");
		}

		URLClassLoader cl = new URLClassLoader(classPathURLs);

		try {
			Class<?> rawClass = cl.loadClass(mainClass);
			if(INimrodOKModule.class.isAssignableFrom(rawClass)) {
				return new Module((INimrodOKModule)rawClass.newInstance(), cl, moduleFolder);
			}
		} catch(ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			throw new ModuleException(moduleName, moduleFolder, e.getMessage());
		}

		throw new ModuleException(moduleName, moduleFolder, String.format("Class '%s' is not a valid module definition", mainClass));
	}

	private static URL[] getURLs(Path folder, String classpath) throws MalformedURLException {
		String[] components = classpath.split(";");

		URL[] urls = new URL[components.length];
		for(int i = 0; i < components.length; ++i) {
			urls[i] = folder.resolve(components[i]).toUri().toURL();
		}
		return urls;
	}

}
