% MZN file model for EFSM induction
% Unfortunately, local variables in predicate are also global variables to solver
% Author: Vladimir Ulyantsev (ulyantsev@rain.ifmo.ru)

% given constants and domains

int: C; % inducted machine size
int: V; % scenarios tree size
int: E; % events (with expressions) count
int: A; % actions count
int: AE; % consistency graph edges count (adjacent edges)

set of int: colors = 0..C-1;
set of int: vertices = 0..V-1;
set of int: apta_edges = 1..V-1;
set of int: adjacent_edges = 1..AE;
set of int: events = 0..E-1;
set of int: actions = 0..A-1;


%% given data

array [apta_edges] of int: tree_event;
array [apta_edges] of int: tree_action;
array [apta_edges] of int: tree_parent;

array [adjacent_edges] of int: edge_src;
array [adjacent_edges] of int: edge_dst;

% String representation of events and actions
array [events] of string: event_str;
array [actions] of string: action_str;


%% variables

array [vertices] of var colors: x; % vertices colors

predicate is_satisfying_coloring(array [vertices] of var colors: x,
                                 array [apta_edges] of int: tree_event,
                                 array [apta_edges] of int: tree_parent) = 
    let {
        array [colors, events] of var colors: y    
    } in (
        forall (i in apta_edges) (
            y[x[tree_parent[i]], tree_event[i]] = x[i]
        )
    );

%% constraints

% start vertex corresponds to start state
constraint x[0] = 0;

constraint is_satisfying_coloring(x, tree_event, tree_parent);

% adjacent graph constraints
constraint forall (i in adjacent_edges) (
    x[edge_src[i]] != x[edge_dst[i]]
);


solve satisfy;

output ["# Colors = ", show(x), "\n",
        "digraph Automaton {\n",
        "    node [shape = circle];\n",
        "    0 [style = \"bold\"];\n"] ++
        
        [show(exists (i in apta_edges) (tree_event[i] == e /\ 
                                        tree_action[i] == a /\ 
                                        x[tree_parent[i]] == c1 /\
                                        x[i] == c2)) ++ 
         "    " ++ show(c1) ++ " -> " ++ show(c2) ++ 
         " [label = \" " ++ show(event_str[e + 1]) ++ " (" ++  show(action_str[a + 1]) ++ ") \"];\n" 
         | c1 in colors, c2 in colors, e in events, a in actions] ++

        ["}"];
