# EFSM-tools

My tools for finite-state machine induction and testing

 * [automaton-generator.jar](/jars/automaton-generator.jar) to generate random EFSM with given papameters
 * [scenarios-generator.jar](/jars/scenarios-generator.jar) to generate scenarios for given EFSM
 * [builder.jar](/jars/builder.jar) and [sat-builder.jar](/jars/sat-builder.jar) to induct EFSM with given states number from given scenarios
 * [isomorphism-checker.jar](/jars/isomorphism-checker.jar) to check two EFSM's for isomorphism
 * [completeness-checker.jar](/jars/completeness-checker.jar) to check single EFSM completeness
 * [consistency-checker.jar](/jars/consistency-checker.jar) to check EFSM and scenarios consistency

### Examples

TODO: add usage examples

### Citation

If you want to cite EFSM-tools work plaese use one of the next:

 - Ulyantsev V., Tsarev F. Extended Finite-State Machine Induction using SAT-Solver / Proceedings of the 14th IFAC Symposium “Information Control Problems in Manufacturing - INCOM'12". IFAC, 2012, pp. 512–517
 - Ulyantsev V., Tsarev F. Extended Finite-State Machine Induction using SAT-Solver / Proceedings of the Tenth International Conference on Machine Learning and Applications. - Honolulu: IEEE Computer Society, 2011. - Vol. 2. - P. 346–349. - DOI 10.1109/ICMLA.2011.166

### Install

To rebuild JAR tools yourself:

> ant all
