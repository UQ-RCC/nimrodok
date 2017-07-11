package org.monash.nimrod.optim.ganesh;

import au.edu.uq.rcc.nimrod.optim.ArrayOfPoints;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import org.ganesh.plugin.ExperimentPvt;
import org.ganesh.core.PopulationInitialiser;
import org.ganesh.core.Population;
import org.ganesh.core.Universe;
import org.ganesh.core.ExnGAChained;
import org.ganesh.core.ExnOutOfRange;
import org.ganesh.core.Organism;
import org.ganesh.core.Chromosome;
import org.ganesh.core.Gene;
import org.ganesh.core.GeneReal;
import org.ganesh.core.GeneLong;
import org.ganesh.core.MixedChromosome;
import org.ganesh.core.MixedChromosomeInitialiser;
import org.ganesh.core.ObjFunc;
import org.ganesh.core.Range;
import org.ganesh.core.Variable;
import org.ganesh.core.VariableLong;
import org.ganesh.core.VariableDouble;

import java.util.List;
import java.util.ArrayList;

public class GaneshPopulationInitialiser extends PopulationInitialiser {

	//starting point
	private ArrayOfPoints startingPop = null;

	public GaneshPopulationInitialiser(ExperimentPvt ep, ArrayOfPoints _startPoints) {
		super(ep);
		startingPop = _startPoints;
	}

	/**
	 * The default population method.
	 *
	 * @param emptyPop An empty population
	 * @param pM The Mutation probability
	 * @param pC The Crossover probability
	 * @param uni The universe in which the Organism will exist
	 * @throws ExnGAChained
	 */
	@Override
	public void populate(Population emptyPop, float pM, float pC, Universe uni)
			throws ExnGAChained {
		// Add Organisms to the (empty) Population
		// the Experiment plugin defines the Chromosome type & chromosome initialiser
		Organism org = null;
		if(emptyPop.getSize() != startingPop.numPoints) {
			throw new ExnGAChained("[GanehPopulationInitialiser::populate] Starting pop is not the same as the one given by Nimrod/OK");
		}
		//init this listVars
		for(int idx = 0; idx < emptyPop.getSize(); idx++) {
			OptimPoint _point = startingPop.pointArray[idx];
			//create a list of genes for the chromosome
			List<Variable> _varList = new ArrayList(_point.dimensionality);
			for(int i = 0; i < _point.dimensionality; i++) {
				Variable _aVar = null;
//	    		if(_point.dataType[i] == DatType.INT){
//	    			Range _aRange = new Range(_point.min[i], _point.max[i]);
//	    			try {
//						 _aVar = new VariableLong(_aRange, (long)_point.coords[i]);
//					} catch (ExnOutOfRange e) {
//						throw new ExnGAChained("[GanehPopulationInitialiser::populate] Error:" + e.getMessage());
//					}
//	    		}
//	    		else if(_point.dataType[i] == DatType.FLT){
				Range _aRange = new Range(_point.min[i], _point.max[i]);
				try {
					_aVar = new VariableDouble(_aRange, _point.coords[i]);
				} catch(ExnOutOfRange e) {
					throw new ExnGAChained("[GanehPopulationInitialiser::populate] Error:" + e.getMessage());
				}
//	    		}
//	    		else
//	    			throw new ExnGAChained("[GanehPopulationInitialiser::populate] Only int and float are supported!!!");
				_varList.add(i, _aVar);
			}
			//not use this one
			//Chromosome _chr = uni.getExperimentPvt().newChromosome(pM, pC);
			Chromosome _chr = new MixedChromosome(_varList, pM, pC, new MixedChromosomeInitialiser() {
				@Override
				public void initialise(Chromosome chr, Gene[] genes, List<? extends Variable> listVars) {
					assert (genes != null) : "aGenes array is null";

					for(int i = 0; i < genes.length; i++) {
						// set to random value in the Variable range
						Variable vv = listVars.get(i);
						if(vv instanceof VariableLong) {
							genes[i] = new GeneLong(vv.getLongValue());
						} else if(vv instanceof VariableDouble) {
							genes[i] = new GeneReal(vv.getDoubleValue());
						}
						//should not happen in this else
					}//for
				}//initialise
			});
			List<ObjFunc> _objFuncs = uni.getExperimentPvt().getListObjFuncs();
			for(ObjFunc _oFunc : _objFuncs) {
				_chr.addObjFunc(_oFunc);
			}
			org = new Organism(_chr, uni);
			emptyPop.add(org);
		}//for
	}

}
