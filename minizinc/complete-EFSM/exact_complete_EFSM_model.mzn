% MZN file model for EFSM induction
% Author: Vladimir Ulyantsev (ulyantsev@rain.ifmo.ru)

% given constants and domains

int: C; % inducted machine size
int: V; % scenarios tree size
int: E; % events (with expressions) count
int: A; % actions count
int: AE; % consistency graph edges count (adjacent edges)
int: TE; % events (without expressions) count
int: TSC; % total sets count = 2 ^ vars_count

set of int: colors = 0..C-1;
set of int: vertices = 0..V-1;
set of int: apta_edges = 1..V-1;
set of int: adjacent_edges = 1..AE;
set of int: events = 1..E;
set of int: true_events = 1..TE;
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


% event-expression satisfiability sets count
array [events] of int: ee_sat_sets;

% mapping event-expression to event number
array [events] of int: ee_event;


%% variables

array [vertices] of var colors: x; % vertices colors
array [colors, events] of var colors: y; % EFSM transitions destinations
array [colors, events] of var bool: z; % does transition covered by scenarios

array [colors, colors] of var bool: tr;
array [1..C-1] of var colors: p; 
array [colors, colors] of var events: m;


%% constraints

% start vertex corresponds to start state
constraint x[0] = 0;

% adjacent graph constraints
constraint forall (i in adjacent_edges) (
    x[edge_src[i]] != x[edge_dst[i]]
);

% transitions constraints
constraint forall (i in apta_edges) (
    y[x[tree_parent[i]], tree_event[i]] = x[i]
);

% completeness constraints

constraint forall (c in colors, e in events) (
   (exists (i in apta_edges where tree_event[i] == e) (x[tree_parent[i]] == c) <-> z[c, e] = true) /\
   (not z[c, e] -> y[c, e] = c)
);

constraint forall (c in colors, te in true_events) (
    forall (e in events where ee_event[e] == te) (z[c, e] == false) \/ 
    (sum (e in events where ee_event[e] == te) (bool2int(z[c, e]) * ee_sat_sets[e]) == TSC)
);


% symmetry breaking constraints

% transition definition
constraint forall (i in colors, j in colors) (
    if i < j then
        tr[i, j] = exists (e in events) (y[i, e] = j)
    else
        tr[i, j] = false
    endif
);

% parent definition
constraint forall (i in colors, j in colors where j < i) (
    (p[i] = j) <-> (tr[j, i] /\ forall (k in 0..j-1) (not tr[k, i]))
);

% next node i parent not less then our i-1
constraint forall (i in colors, where i > 1) (
    p[i - 1] <= p[i]
);

% node i is the last child of j
constraint forall (i in colors, j in colors, where i + 1 < j) (
    (p[i] = j /\ p[i + 1] > j) -> 
        forall (k in colors where k > i + 1) (p[k] != j)
);

% defining m[i, j] as minimum label on transitions between i and j
constraint forall (i in colors, j in colors, e in events) (
    if i < j then
        (tr[i, j] -> (
            m[i, j] = e <-> (y[i, e] = j /\ forall (ee in events where ee < e) (y[i, ee] != j))
        )) 
%         /\
%         ((not tr[i, j]) -> m[i, j] = 0)
    else
        m[i, j] = 1
    endif
);

% ordering nodes i and i+1 with the same parent j
constraint forall (i in colors, j in colors where i + 1 < j) (
    (p[i] = j /\ p[i + 1] = j) -> (m[j, i] < m[j, i + 1])
);


solve satisfy;


% I have no idea, why do I have to add " + 1" in event_str[e + 1] and action_str[a + 1]

output ["# x = ", show(x), "\n",
        "# y = ", show(y), "\n",
        "# tr = ", show(tr), "\n",
        "# m = ", show(m), "\n",
        "# p = ", show(p), "\n",

        "digraph Automaton {\n",
        "    node [shape = circle];\n",
        "    0 [style = \"bold\"];\n"] ++
        
        [show(exists (i in apta_edges) (tree_event[i] == e /\ 
                                        tree_action[i] == a /\ 
                                        x[tree_parent[i]] == c1 /\
                                        x[i] == c2)) ++ 
         "    " ++ show(c1) ++ " -> " ++ show(c2) ++ 
         " [label = \" " ++ show(event_str[e]) ++ " (" ++  show(action_str[a + 1]) ++ ") \"];\n" 
         | c1 in colors, c2 in colors, e in events, a in actions] ++

        ["}"];

