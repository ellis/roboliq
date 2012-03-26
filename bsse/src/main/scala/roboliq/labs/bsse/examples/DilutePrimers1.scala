package roboliq.labs.bsse.examples

class DilutePrimers1 {
	/*
MW = [g/mol]
m = [ug]
conc = [umol/l]
V = [ul]

1e6 [ul/l] * 1/conc [l/umol] * 1e6 [umol/mol] * 1/MW [mol/g] * 1e6 [g/ug] * m [ug]

1e18 * m / conc / MW
1e18 * 786.39 / 100 / 14413.4

[umol] = 786.39 [ug] * 1e6 [ug/g] * 1/14413.4 [mol/g] * 1e6 [umol/mol]

786.39 [ug] * 1/1e6 [g/ug] * 1/14413.4 [mol/g] * 1e9 [nmol/mol]
*/
	// The nano molar amount for SEQUENCE_01 to SEQUENCE_49
	val lMol_n = List[BigDecimal](
		37.08,
		37.41,
		38.14,
		40.75,
		41.33,
		41.83,
		43.38,
		44.01,
		44.18,
		44.33,
		45.13,
		45.35,
		45.39,
		46.24,
		46.53,
		46.62,
		46.81,
		46.95,
		47.12,
		47.16,
		47.36,
		47.76,
		47.84,
		48.24,
		48.48,
		48.5,
		48.5,
		48.51,
		48.59,
		49.18,
		49.73,
		50.06,
		50.42,
		51.11,
		52.4,
		53.01,
		53.71,
		54.26,
		54.3,
		54.62,
		54.74,
		54.95,
		56.27,
		58,
		59.09,
		59.1,
		59.68,
		72.2,
		115.04
	)
}