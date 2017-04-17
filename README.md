Warning: this is a development branch. Some features may not work properly.

## Introduction

EFSM-tools is a toolset for finite-state machine (FSM) synthesis. Is is mostly based on satisfiability solvers, and uses *traces* (aka IO-traces, *scenarios*) and LTL formulae as input. EFSM means "extended finite-state machine", an FSM with variables. However, within this toolset variables are Boolean and are often treated analogously to input events. The variant of the FSM synthesis problem being solved is *exact*: an FSM is generated with the given number of states, or its non-existence is reported.

The toolset implements the techniques described in the following papers:

* [Ulyantsev V., Tsarev F. Extended Finite-State Machine Induction using SAT-Solver / Proceedings of the Tenth International Conference on Machine Learning and Applications. - Honolulu: IEEE Computer Society, 2011. - Vol. 2. - P. 346-349](http://dx.doi.org/10.1109/ICMLA.2011.166)
* [Ulyantsev V., Buzhinsky I., Shalyto A. Exact Finite-State Machine Identification from Scenarios and Temporal Properties. International Journal on Software Tools for Technology Transfer, 2016, DOI: 10.1007/s10009-016-0442-1](http://arxiv.org/abs/1601.06945)
* [Buzhinsky I., Vyatkin V. Automatic Inference of Finite-State Plant Models from Traces and Temporal Properties. IEEE Transactions on Industrial Informatics, 2017, DOI: 10.1109/TII.2017.2670146](http://rain.ifmo.ru/~buzhinsky/papers/tii-2017-automatic.pdf)

EFSM-tools is developed in Computer Technologies Laboratory, ITMO University. You may find related projects by the same research group below:

* [Toolset for deterministic finite automata (DFA) synthesis](https://github.com/ctlab/DFA-Inductor)
* [MuACO toolset for EFSM synthesis based on ant colony optimization](https://bitbucket.org/chivilikhin_daniil/aco-for-automata)

## Prerequisites

To build the toolset, you need JDK 1.8 (or greater) and [ant](https://ant.apache.org/). The tool is intended to work on Linux and Windows. The most recent features are not tested on Windows, though. Running some examples may require bash.

EFSM-tools works with [cryptominisat](http://www.msoos.org/cryptominisat4/) and [lingeling](http://fmv.jku.at/lingeling/) SAT solvers, [Choco](http://choco-solver.org/) constraint (CSP) solver and [DepQBF](https://github.com/lonsing/depqbf) QSAT (QBF) solver. Choco is included into the toolset as a library. As for the rest, they must be installed separately. Make them available in command line as "cryptominisat4", "lingeling" and "depqbf".

Some of the tools which work with solvers require [limboole](http://fmv.jku.at/limboole/). Make it available in command line as "limboole".

Different features of the toolset require different solvers, so probably there is no need to install all of them. More information is provided in other sections.

To view generated FSMs, you need [Graphviz](http://www.graphviz.org/) (on Linux, you may additionally install the XDot viewer).

## Building and running

To build the toolset, move to the root of the project and run:

> ant

The JAR executables will be generated in the [jars](/jars/) directory. Then you can try running JAR tools like this and see the summaries of their command line options:

> java -jar jars/<tool-name\>.jar

In addition, recent synthesis methods by default use the incremental version of cryptominisat4. The pre-built Linux binary is located in the [incremental-cryptominisat](/incremental-cryptominisat/) directory. Make the binary file incremental-cryptominisat-binary available in command line, e.g.:

> sudo ln -s incremental-cryptominisat-binary /usr/bin

To run incremental-cryptominisat-binary, you at least need to have cryptominisat4 installed. To rebuild it yourself, you need make, g++ and cryptominisat4. Run:

> make

If for some reason you are unable to use the incremental version of cryptominisat, the usual versions of cryptominisat and lingeling can be used instead (some jar tools have command line options to set the used solver).

## Input data format for traces

Behavior traces, or scenarios, are represented as text files. Each scenario is a pair of two consecutive lines: the first line represents inputs and the second one represents outputs. Trace elements are separated with semicolons. Trace files may have empty lines, which are ignored.

Inputs have the formal *event[&lt;Boolean formula over input variables&gt;]*. The boolean formula can be just "1" if there are no input variables. Nontrivial Boolean formulae can use Boolean operators "&" (and), "|" (or), "~" (not). Here are some examples of input declarations:

* A[1]
* e1[x0 & ~x1]

While each input is a combination of an event and, optionally, of variable values, each output is a combination of comma-separated actions, for example:

* z0, z1, z4
* close, logout

To see complete examples of trace files, you may examine the files [examples/elevator.sc](/examples/elevator.sc) and [examples/clock.sc](/examples/clock.sc).

More notes:

* For Mealy machine synthesis, the order of actions is important (i.e. outputs are compared as action strings), while for Moore machines it is not (i.e. outputs are compared as action sets).
* For Moore machine synthesis, the first input is always dummy (represented by the empty string). Thus, each input line starts with a semicolon.

## Input data format for LTL properties

LTL properties, or LTL formulae, are represented as text files. Each line represents a single LTL formula. Formulae may use temporal operators X (next), F (future), G (globally), U (until), R (release), Boolean operators "&&" (and), "||" (or), "!" (not), and atomic propositions *event(&lt;event name or a comma-separated list of event names&gt;)*, *action(&lt;action name&gt;)*, *variable(&lt;variable name&gt;)*.

To see complete examples of LTL property files, you may examine the files [examples/elevator.ltl](/examples/elevator.ltl) and [examples/clock.ltl](/examples/clock.ltl).

More notes:

* Empty lines are not permitted.
* There is a deprecated format which requires to write atomic propositions as *wasAction(co.&lt;action name&gt;)* and *wasEvent(ep.&lt;event name&gt;)*. Avoid this format.

## Mealy machine synthesis from traces and LTL properties based on incremental SAT solvers

This method is described here as the "Iterative SAT-based" one:

* [Ulyantsev V., Buzhinsky I., Shalyto A. Exact Finite-State Machine Identification from Scenarios and Temporal Properties. International Journal on Software Tools for Technology Transfer, 2016, DOI: 10.1007/s10009-016-0442-1](http://arxiv.org/abs/1601.06945).

It uses incremental cryptominisat by default, but switching to usual versions of cryptominisat or lingeling are possible using command line options. To run the tool:

> java -jar jars/fast-automaton-generator.jar

Here are scripts to run several examples from the paper mentioned above:

> cd examples

> ln -s ../c-lib .

> ./clock-fast.sh # Alarm clock

> ./elevator-fast.sh # Elevator

> ./cash-dispenser-fast.sh # ATM

> ./editor-fast.sh # Text editor

> ./jhotdraw-fast.sh # JHotDraw

> ./cvs-fast.sh # CVS client

## Mealy machine synthesis from traces and LTL properties based on SAT and QSAT solvers

Additional (but generally slower) methods are also available for the same problem. They are described here:

* [Ulyantsev V., Buzhinsky I., Shalyto A. Exact Finite-State Machine Identification from Scenarios and Temporal Properties. International Journal on Software Tools for Technology Transfer, 2016, DOI: 10.1007/s10009-016-0442-1](http://arxiv.org/abs/1601.06945).

Run the following tool:

> java -jar jars/qbf-automaton-generator.jar

It supports four methods of FSM synthesis based on:

* running a QSAT (QBF) solver (this method is quite slow)
* translating the QSAT instance to a SAT one and running a SAT solver (this method may require much memory)
* backtracking (no solver is used)
* iterative SAT-solving using a non-incremental solver (not described in the paper)

The concrete method can be selected using the "--strategy" command line option. FSM completeness requirement can be switched on by adding "--completenessType NORMAL". The only supported QBF solver is DepQBF.

Here are scripts to run several examples from the paper mentioned above:

> cd examples

> ln -s ../c-lib .

> ./clock.sh # Alarm clock

> ./elevator.sh # Elevator

> ./cash-dispenser.sh # ATM

> ./editor.sh # Text editor

> ./jhotdraw.sh # JHotDraw

> ./cvs.sh # CVS client

## Plant model synthesis from traces and LTL properties

This is a spin-off of the FSM synthesis project which focuses on Moore machine synthesis for a specific application. In the industrial automation domain, controllers co-exist with plants. In formal modeling, it is natural to represent plants as nondeterministic Moore machines. Read more in:

* [Buzhinsky I., Vyatkin V. Automatic Inference of Finite-State Plant Models from Traces and Temporal Properties. IEEE Transactions on Industrial Informatics, 2017, DOI: 10.1109/TII.2017.2670146](http://rain.ifmo.ru/~buzhinsky/papers/tii-2017-automatic.pdf)

Run the tool:

> java -jar jars/plant-automaton-generator.jar

Here are scripts to run some examples (the first one is from the paper):

> cd evaluation/plant-synthesis

> ln -s ../../c-lib .

> ./cylinder.sh

> ./water-level.sh

## Plant model synthesis from traces

If the input data doesn't have any LTL formulae, run plant-automaton-generator.jar with the option "--fast". This will switch the solver-based algorithm to a much simpler and faster graph algorithm. The proper number of states will be determined automatically.

## Moore machine synthesis from traces and LTL properties

The tool jars/plant-automaton-generator.jar can also be used to generate usual deterministic Moore machines. Add "--deterministic" (and, optionally, "--bfsConstraints") to command line options. Note that in this case all traces have to start with the same tuple of outputs.

Here are scripts to run some examples:

> cd evaluation/moore-machine-synthesis

> ln -s ../../c-lib .

> ./cylinder.sh

> ./water-level.sh

## Mealy machine synthesis from traces based on SAT and CSP solvers

This set of tools is partially described in the paper:

* [Ulyantsev V., Tsarev F. Extended Finite-State Machine Induction using SAT-Solver / Proceedings of the Tenth International Conference on Machine Learning and Applications. - Honolulu: IEEE Computer Society, 2011. - Vol. 2. - P. 346-349](http://dx.doi.org/10.1109/ICMLA.2011.166)

FSM identification using the Choco CSP solver:

> java -jar jars/builder.jar

FSM identification using the cryptominisat SAT solver (must be available under the name "cryptominisat4"):

> java -jar jars/sat-builder.jar

Auxiliary tools (for experiments, etc.):

* [automaton-generator.jar](/jars/automaton-generator.jar): generate a random EFSM with the given parameters
* [scenarios-generator.jar](/jars/scenarios-generator.jar): generate scenarios for the given EFSM
* [isomorphism-checker.jar](/jars/isomorphism-checker.jar): check two EFSMs for isomorphism
* [completeness-checker.jar](/jars/completeness-checker.jar): check an EFSM for completeness (completeness here means rather coverage of transitions by traces)
* [consistency-checker.jar](/jars/consistency-checker.jar): check EFSM compliance with a set of scenarios

## Troubleshooting, questions, research collaboration, etc.
Regarding research collaboration, email Vladimir Ulyantsev (ulyantsev@rain.ifmo.ru) and Igor Buzhinsky (igor_buzhinsky@corp.ifmo.ru).

Regarding issues with the tool, bugs, etc., email Igor Buzhinsky.
