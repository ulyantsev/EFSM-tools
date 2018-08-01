#include <cryptominisat4/cryptominisat.h>
#include <cryptominisat4/solvertypesmini.h>
#include <vector>
#include <sstream>
#include <stdlib.h>
#include <string.h>

using namespace CMSat;

int main(int argc, const char* argv[]) {
    if (argc < 2) {
        std::cout << "Run: incremental-cryptominisat <varNumber> <timeLimit in seconds>" << std::endl;
        std::cout << "Time limit is currently not supported." << std::endl;
        return 1;
    }
    
    int var_number = atoi(argv[1]);
    int time_limit = atoi(argv[2]);
    SATSolver solver;
    solver.new_vars(var_number);

    while (true) {
        std::string line;
        std::getline(std::cin, line);
        if (line == "solve") {
            lbool res = solver.solve();
            if (res == l_True) {
                std::cout << "SAT" << std::endl;
                std::vector<lbool> model = solver.get_model();
                std::cout << "v ";
                for (int i = 0; i < model.size(); i++) {
                    std::cout << (model[i] == l_True ? "" : "-") << (i + 1) << " ";
                }
                std::cout << std::endl;
            } else if (res == l_False) {
                std::cout << "UNSAT" << std::endl;
            } else {
                std::cout << "UNKNOWN" << std::endl;
            }
        } else if (strncmp(line.c_str(), "new_vars", 8) == 0) {
            std::istringstream iss(line);
            std::string sub;
            iss >> sub;
            iss >> sub;
            int num = atoi(sub.c_str());
            solver.new_vars(num);
        } else if (line == "halt") {
            break;
        } else {
            std::vector<Lit> lits;
            std::istringstream iss(line);
            do {
                std::string sub;
                iss >> sub;
                int literal = atoi(sub.c_str());
                if (literal == 0) {
                    break;
                }
                lits.push_back(Lit(abs(literal) - 1, literal < 0));
            } while (iss);
            solver.add_clause(lits);
        }
    }
    
    return 0;
}
