package au.edu.uq.rcc.nimrod.optim.modules;

import java.nio.file.Path;

public class ModuleException extends Exception {
	public final String name;
	public final Path modulePath;
	public final String message;
	
	public ModuleException(String name, Path path, String message) {
		super(String.format("Module '%s' @ '%s': %s", name, path, message.trim()));
		
		this.name = name;
		this.modulePath = path;
		this.message = message;
	}
}
