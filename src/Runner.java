
public class Runner {
	public static void main(String[] args) {
		/*QbfBuilderMain.main(new String[] {
				"qbf/testing-daniil/50n/nstates=5/10/plain-scenarios",
				"--size", "5",
				"--eventNumber", "2",
				"--eventNames", "A,B",
				"--actionNumber", "2",
				"--actionNames", "z0,z1",
				"--varNumber", "2",
				"--timeout", "100000",
				"--result", "qbf/generated-fsm.gv",
				"--strategy", "BACKTRACKING",
				"--backtrackingErrorNumber", "0"
		});*/
		//QbfBuilderMain.main("qbf/case-instances/clock.sc --ltl qbf/case-instances/clock.ltl --size 3 --eventNumber 4 --eventNames A,T,H,M --actionNumber 7 --actionNames z1,z2,z3,z4,z5,z6,z7 --varNumber 2 --timeout 200000 --result generated-fsm.gv --strategy COUNTEREXAMPLE --completenessType NORMAL --satSolver LINGELING".split(" "));
		QbfBuilderMain.main("qbf/case-instances/elevator.sc --ltl qbf/case-instances/elevator.ltl --size 5 --eventNumber 5 --eventNames e11,e12,e2,e3,e4 --actionNumber 3 --actionNames z1,z2,z3 --varNumber 0 --timeout 2000000 --result generated-fsm.gv --strategy BACKTRACKING --completenessType NO_DEAD_ENDS --satSolver LINGELING".split(" "));
	}
}
