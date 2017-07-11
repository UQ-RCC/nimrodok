package au.edu.uq.rcc.nimrod.optim.build;

import java.util.Map;

public final class ActorDefinition {

	public final String name;
	public final String klass;
	public final String icon;
	public final String entity;
	public final String xmlName;

	public ActorDefinition(String name, String klass, String icon, String entity) {
		this.name = name;
		this.klass = klass;
		this.icon = icon;
		this.entity = entity;

		String[] comps = klass.trim().split("\\.");
		this.xmlName = String.format("%s.xml", comps[comps.length - 1].trim());
	}

	public ActorDefinition(Map<String, String> params) {
		this(params.get("name"), params.get("klass"), params.get("icon"), params.get("entity"));
	}
}
