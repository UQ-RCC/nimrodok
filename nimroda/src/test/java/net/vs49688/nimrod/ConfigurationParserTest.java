package net.vs49688.nimrod;

import au.edu.uq.rcc.nimrod.optim.Properties;
import java.text.ParseException;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;

public class ConfigurationParserTest {

	@Test
	public void mots2ZDT4ConfigTest() throws ParseException {
		String cfgString = "mots2.diversify 25\r\n"
				+ "mots2.intensify 15\n"
				+ "mots2.reduce 50\n"
				+ "mots2.start_step 0.1\n"
				+ "mots2.ssrf 0.5\n"
				+ "mots2.n_sample 6\n"
				+ "mots2.loop_limit 10000\n"
				+ "mots2.n_regions 4\n"
				+ "mots2.stm_size 20\n"
				+ "mots2.maximum_improvements 10000\n"
				+ "mots2.maximum_duplicates 100";

		String[] keys = new String[]{
			"mots2.diversify",
			"mots2.intensify",
			"mots2.reduce",
			"mots2.start_step",
			"mots2.ssrf",
			"mots2.n_sample",
			"mots2.loop_limit",
			"mots2.n_regions",
			"mots2.stm_size",
			"mots2.maximum_improvements",
			"mots2.maximum_duplicates"
		};

		String[] values = new String[]{
			"25",
			"15",
			"50",
			"0.1",
			"0.5",
			"6",
			"10000",
			"4",
			"20",
			"10000",
			"100"
		};

		assert keys.length == values.length;
		
		Map<String, String> cfg = Properties.parseString(cfgString).getRawData();

		Assert.assertEquals("Invalid map size", keys.length, cfg.size());
		
		for(int i = 0; i < keys.length; ++i) {
			Assert.assertEquals(String.format("Map missing pair %s:%s", keys[i], values[i]), true, cfg.containsKey(keys[i]));
			Assert.assertEquals("Invalid value for key %s", cfg.get(keys[i]), values[i]);
		}
	}
}
