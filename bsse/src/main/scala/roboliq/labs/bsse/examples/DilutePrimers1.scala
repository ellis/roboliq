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
}