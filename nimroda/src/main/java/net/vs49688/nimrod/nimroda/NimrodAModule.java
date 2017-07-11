package net.vs49688.nimrod.nimroda;

import au.edu.uq.rcc.nimrod.optim.IAlgorithmDefinition;
import au.edu.uq.rcc.nimrod.optim.modules.Author;
import au.edu.uq.rcc.nimrod.optim.modules.INimrodOKModule;

public class NimrodAModule implements INimrodOKModule {

	public NimrodAModule() {
		NimrodA.initLibrary();
	}

	@Override
	public String getName() {
		return "Nimrod/A";
	}

	@Override
	public Author[] getAuthors() {
		return new Author[]{new Author("Zane van Iperen", "zane@zanevaniperen.com", "The University of Queensland")};
	}

	@Override
	public IAlgorithmDefinition[] getAlgorithms() {
		return NimrodAAlgorithm.getDefinitions();
	}

}
