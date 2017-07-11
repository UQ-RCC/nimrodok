#!/usr/bin/env python

import sys

# Find/replace all the old monash classnames with the new ones

with open(sys.argv[1], "Ur") as f:
	data = f.read()
	data = data.replace("org.monash.nimrod.optim.DefineSearchSpaceActor", "au.edu.uq.rcc.nimrod.optim.DefineSearchSpaceActor")
	data = data.replace("urn:lsid:nimrod.monash.edu.au:actor:301:1", "urn:lsid:au.edu.uq.rcc.nimrod.optim:actor:1:1")
	data = data.replace("org.monash.nimrod.optim.SelectPointsActor", "au.edu.uq.rcc.nimrod.optim.SelectPointsActor")
	data = data.replace("urn:lsid:nimrod.monash.edu.au:class:306:1", "urn:lsid:au.edu.uq.rcc.nimrod.optim:actor:2:1")
	data = data.replace("org.monash.nimrod.optim.NimrodOptimActor", "au.edu.uq.rcc.nimrod.optim.NimrodOptimActor")
	data = data.replace("urn:lsid:nimrod.monash.edu.au:class:307:1", "urn:lsid:au.edu.uq.rcc.nimrod.optim:actor:3:1")
	data = data.replace("org.monash.nimrod.optim.StatsParserActor", "au.edu.uq.rcc.nimrod.optim.StatsParserActor")
	data = data.replace("urn:lsid:nimrod.monash.edu.au:class:308:1", "urn:lsid:au.edu.uq.rcc.nimrod.optim:actor:4:1")
	data = data.replace("org.monash.nimrod.optim.Point2ArrayActor", "au.edu.uq.rcc.nimrod.optim.Point2ArrayActor")
	data = data.replace("urn:lsid:nimrod.monash.edu.au:class:309:1", "urn:lsid:au.edu.uq.rcc.nimrod.optim:actor:5:1")
	data = data.replace("net.vs49688.nimrod.CacheActor", "au.edu.uq.rcc.nimrod.optim.CacheActor")
	data = data.replace("urn:lsid:net.vs49688.nimrod:actor:1:1", "urn:lsid:au.edu.uq.rcc.nimrod.optim:actor:6:1")
	data = data.replace("net.vs49688.nimrod.ParPlotActor", "au.edu.uq.rcc.nimrod.optim.ParPlotActor")
	data = data.replace("urn:lsid:net.vs49688.nimrod:actor:1:2", "urn:lsid:au.edu.uq.rcc.nimrod.optim:actor:7:1")

	data = data.replace("org.monash.nimrod.optim.AlgorithmicChoiceStyle", "au.edu.uq.rcc.nimrod.optim.AlgorithmicChoiceStyle")

with open(sys.argv[2], "wb") as f:
	f.write(data)
	
