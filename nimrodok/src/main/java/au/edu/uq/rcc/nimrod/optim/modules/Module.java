package au.edu.uq.rcc.nimrod.optim.modules;

import au.edu.uq.rcc.nimrod.optim.modules.INimrodOKModule;
import java.nio.file.Path;

public class Module {

	Module(INimrodOKModule module, ClassLoader loader, Path path) {
		m_Module = module;
		m_ClassLoader = loader;
		m_Path = path;
	}

	public ClassLoader getClassLoader() {
		return m_ClassLoader;
	}

	public INimrodOKModule getModule() {
		return m_Module;
	}
	
	public Path getPath() {
		return m_Path;
	}

	private final INimrodOKModule m_Module;
	private final ClassLoader m_ClassLoader;
	private final Path m_Path;
}
