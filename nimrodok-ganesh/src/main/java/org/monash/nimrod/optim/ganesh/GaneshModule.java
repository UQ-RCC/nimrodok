package org.monash.nimrod.optim.ganesh;

import au.edu.uq.rcc.nimrod.optim.IAlgorithmDefinition;
import au.edu.uq.rcc.nimrod.optim.modules.Author;
import au.edu.uq.rcc.nimrod.optim.modules.INimrodOKModule;

public class GaneshModule implements INimrodOKModule {

	@Override
	public String getName() {
		return "Ganesh";
	}

	@Override
	public Author[] getAuthors() {
		return new Author[]{
			new Author("Hoang Anh Nguyen", "h.nguyen30@uq.edu.au", "The University of Queensland"),
			new Author("Zane van Iperen", "zane@zanevaniperen.com", "The University of Queensland")
		};
	}

	@Override
	public IAlgorithmDefinition[] getAlgorithms() {
		return new IAlgorithmDefinition[]{new GaneshDefinition()};
	}

}
