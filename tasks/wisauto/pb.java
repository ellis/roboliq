import java.util.LinkedList;
import JSHOP2.*;

public class pb
{
	private static String[] defineConstants()
	{
		String[] problemConstants = new String[61];

		problemConstants[0] = "r1";
		problemConstants[1] = "user";
		problemConstants[2] = "offsite";
		problemConstants[3] = "is-labware";
		problemConstants[4] = "pcrplate1";
		problemConstants[5] = "r1_bench_001x1";
		problemConstants[6] = "r1_bench_001x2";
		problemConstants[7] = "r1_bench_001x3";
		problemConstants[8] = "r1_bench_001x4";
		problemConstants[9] = "r1_bench_001x5";
		problemConstants[10] = "r1_bench_001x6";
		problemConstants[11] = "r1_bench_001x7";
		problemConstants[12] = "r1_bench_001x8";
		problemConstants[13] = "r1_bench_035x1";
		problemConstants[14] = "r1_bench_035x2";
		problemConstants[15] = "r1_bench_035x3";
		problemConstants[16] = "r1_bench_041x1";
		problemConstants[17] = "r1_bench_041x2";
		problemConstants[18] = "r1_bench_041x3";
		problemConstants[19] = "r1_bench_047x1";
		problemConstants[20] = "r1_bench_047x2";
		problemConstants[21] = "r1_bench_047x3";
		problemConstants[22] = "r1_bench_059x1";
		problemConstants[23] = "r1_bench_059x2";
		problemConstants[24] = "r1_device_-1x1";
		problemConstants[25] = "r1_device_213x1";
		problemConstants[26] = "r1_device_219x1";
		problemConstants[27] = "r1_device_299x1";
		problemConstants[28] = "r1_device_300x1";
		problemConstants[29] = "r1_device_301x1";
		problemConstants[30] = "r1_device_44x1";
		problemConstants[31] = "r1_device_44x2";
		problemConstants[32] = "r1_device_87x1";
		problemConstants[33] = "r1_hotel_225x1";
		problemConstants[34] = "r1_hotel_85x1";
		problemConstants[35] = "r1_hotel_85x2";
		problemConstants[36] = "r1_hotel_85x3";
		problemConstants[37] = "r1_hotel_85x4";
		problemConstants[38] = "r1_hotel_85x5";
		problemConstants[39] = "r1_hotel_93x1";
		problemConstants[40] = "r1_hotel_93x2";
		problemConstants[41] = "r1_hotel_93x3";
		problemConstants[42] = "r1_hotel_93x4";
		problemConstants[43] = "r1_hotel_93x5";
		problemConstants[44] = "reagentplate1";
		problemConstants[45] = "m001";
		problemConstants[46] = "m002";
		problemConstants[47] = "offsitemodel";
		problemConstants[48] = "sm1";
		problemConstants[49] = "sm2";
		problemConstants[50] = "r1_pipetter1";
		problemConstants[51] = "is-platemodel";
		problemConstants[52] = "is-sitemodel";
		problemConstants[53] = "r1_transporter1";
		problemConstants[54] = "r1_transporter2";
		problemConstants[55] = "userarm";
		problemConstants[56] = "r1_transporterspec0";
		problemConstants[57] = "userarmspec";
		problemConstants[58] = "device-can-model";
		problemConstants[59] = "device-can-spec";
		problemConstants[60] = "spec0003";

		return problemConstants;
	}

	private static void createState0(State s)	{
		s.add(new Predicate(1, 0, new TermList(TermConstant.getConstant(23), TermList.NIL)));
		s.add(new Predicate(1, 0, new TermList(TermConstant.getConstant(24), TermList.NIL)));
		s.add(new Predicate(5, 0, new TermList(TermConstant.getConstant(68), TermList.NIL)));
		s.add(new Predicate(5, 0, new TermList(TermConstant.getConstant(69), TermList.NIL)));
		s.add(new Predicate(5, 0, new TermList(TermConstant.getConstant(70), TermList.NIL)));
		s.add(new Predicate(5, 0, new TermList(TermConstant.getConstant(71), TermList.NIL)));
		s.add(new Predicate(5, 0, new TermList(TermConstant.getConstant(72), TermList.NIL)));
		s.add(new Predicate(22, 0, new TermList(TermConstant.getConstant(73), TermList.NIL)));
		s.add(new Predicate(3, 0, new TermList(TermConstant.getConstant(27), TermList.NIL)));
		s.add(new Predicate(3, 0, new TermList(TermConstant.getConstant(67), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(25), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(28), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(29), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(30), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(31), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(32), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(33), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(34), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(35), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(36), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(37), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(38), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(39), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(40), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(41), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(42), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(43), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(44), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(45), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(46), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(47), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(48), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(49), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(50), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(51), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(52), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(53), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(54), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(55), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(56), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(57), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(58), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(59), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(60), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(61), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(62), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(63), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(64), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(65), TermList.NIL)));
		s.add(new Predicate(4, 0, new TermList(TermConstant.getConstant(66), TermList.NIL)));
		s.add(new Predicate(2, 0, new TermList(TermConstant.getConstant(76), TermList.NIL)));
		s.add(new Predicate(2, 0, new TermList(TermConstant.getConstant(77), TermList.NIL)));
		s.add(new Predicate(2, 0, new TermList(TermConstant.getConstant(78), TermList.NIL)));
		s.add(new Predicate(6, 0, new TermList(TermConstant.getConstant(79), TermList.NIL)));
		s.add(new Predicate(6, 0, new TermList(TermConstant.getConstant(80), TermList.NIL)));
		s.add(new Predicate(7, 0, new TermList(TermConstant.getConstant(23), new TermList(TermConstant.getConstant(73), TermList.NIL))));
		s.add(new Predicate(7, 0, new TermList(TermConstant.getConstant(23), new TermList(TermConstant.getConstant(76), TermList.NIL))));
		s.add(new Predicate(7, 0, new TermList(TermConstant.getConstant(23), new TermList(TermConstant.getConstant(77), TermList.NIL))));
		s.add(new Predicate(7, 0, new TermList(TermConstant.getConstant(24), new TermList(TermConstant.getConstant(78), TermList.NIL))));
		s.add(new Predicate(10, 0, new TermList(TermConstant.getConstant(70), new TermList(TermConstant.getConstant(68), TermList.NIL))));
		s.add(new Predicate(10, 0, new TermList(TermConstant.getConstant(70), new TermList(TermConstant.getConstant(69), TermList.NIL))));
		s.add(new Predicate(10, 0, new TermList(TermConstant.getConstant(71), new TermList(TermConstant.getConstant(68), TermList.NIL))));
		s.add(new Predicate(10, 0, new TermList(TermConstant.getConstant(71), new TermList(TermConstant.getConstant(69), TermList.NIL))));
		s.add(new Predicate(10, 0, new TermList(TermConstant.getConstant(72), new TermList(TermConstant.getConstant(69), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(27), new TermList(TermConstant.getConstant(68), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(36), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(37), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(38), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(39), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(40), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(41), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(42), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(43), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(44), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(48), new TermList(TermConstant.getConstant(72), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(51), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(52), new TermList(TermConstant.getConstant(72), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(53), new TermList(TermConstant.getConstant(72), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(54), new TermList(TermConstant.getConstant(72), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(55), new TermList(TermConstant.getConstant(71), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(58), new TermList(TermConstant.getConstant(72), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(59), new TermList(TermConstant.getConstant(72), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(60), new TermList(TermConstant.getConstant(72), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(61), new TermList(TermConstant.getConstant(72), TermList.NIL))));
		s.add(new Predicate(9, 0, new TermList(TermConstant.getConstant(67), new TermList(TermConstant.getConstant(68), TermList.NIL))));
		s.add(new Predicate(11, 0, new TermList(TermConstant.getConstant(27), new TermList(TermConstant.getConstant(25), TermList.NIL))));
		s.add(new Predicate(11, 0, new TermList(TermConstant.getConstant(67), new TermList(TermConstant.getConstant(25), TermList.NIL))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(36), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(37), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(38), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(39), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(40), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(41), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(48), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(49), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(50), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(51), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(52), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(53), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(54), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(56), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(57), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(58), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(59), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(60), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(61), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(62), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(63), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(64), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(65), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(76), new TermList(TermConstant.getConstant(66), new TermList(TermConstant.getConstant(79), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(78), new TermList(TermConstant.getConstant(25), new TermList(TermConstant.getConstant(80), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(78), new TermList(TermConstant.getConstant(36), new TermList(TermConstant.getConstant(80), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(78), new TermList(TermConstant.getConstant(37), new TermList(TermConstant.getConstant(80), TermList.NIL)))));
		s.add(new Predicate(8, 0, new TermList(TermConstant.getConstant(78), new TermList(TermConstant.getConstant(38), new TermList(TermConstant.getConstant(80), TermList.NIL)))));
		s.add(new Predicate(14, 0, new TermList(TermConstant.getConstant(73), new TermList(TermConstant.getConstant(36), TermList.NIL))));
		s.add(new Predicate(14, 0, new TermList(TermConstant.getConstant(73), new TermList(TermConstant.getConstant(37), TermList.NIL))));
		s.add(new Predicate(14, 0, new TermList(TermConstant.getConstant(73), new TermList(TermConstant.getConstant(38), TermList.NIL))));
	}

	public static LinkedList<Plan> getPlans()
	{
		LinkedList<Plan> returnedPlans = new LinkedList<Plan>();
		TermConstant.initialize(84);

		Domain d = new domain();

		d.setProblemConstants(defineConstants());

		State s = new State(23, d.getAxioms());

		JSHOP2.initialize(d, s);

		TaskList tl;
		SolverThread thread;

		createState0(s);

		tl = new TaskList(1, true);
		tl.subtasks[0] = new TaskList(new TaskAtom(new Predicate(11, 2, new TermList(TermVariable.getVariable(0), new TermList(TermVariable.getVariable(1), new TermList(TermConstant.getConstant(83), new TermList(TermConstant.getConstant(67), new TermList(TermConstant.getConstant(27), TermList.NIL)))))), false, false));

		thread = new SolverThread(tl, 1);
		thread.start();

		try {
			while (thread.isAlive())
				Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		returnedPlans.addAll( thread.getPlans() );

		return returnedPlans;
	}

	public static LinkedList<Predicate> getFirstPlanOps() {
		return getPlans().getFirst().getOps();
	}
}