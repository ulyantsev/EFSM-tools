#include <cryptominisat4/cryptominisat.h>
#include <cryptominisat4/solvertypesmini.h>
#include <vector>
//#include <sstream>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <byteswap.h>

using namespace CMSat;

int read(FILE *log) {
    int value;
    fread(&value, sizeof(int), 1, stdin);
    value = bswap_32(value);
    //fprintf(log, "%d ", value);
    return value;
}

void write(int value) {
    value = bswap_32(value);
    fwrite(&value, sizeof(int), 1, stdout);
}

int main(int argc, const char* argv[]) {
    if (argc < 2) {
        std::cout << "Run: incremental-cryptominisat-binary <varNumber> <timeLimit in seconds>" << std::endl;
        std::cout << "Time limit is currently not supported." << std::endl;
        std::cout << "Input for this process is binary (supply 4-byte integers): " << std::endl;
        std::cout << " * Add clause:    0 <clause terminated by 0>" << std::endl;
        std::cout << " * Add variables: 1 <number of new variables>" << std::endl;
        std::cout << " * Solve:         2" << std::endl;
        std::cout << " * Terminate:     3" << std::endl;
        std::cout << "As well as output: " << std::endl;
        std::cout << " * SAT:     0 <assignemnt terminated by 0>" << std::endl;
        std::cout << " * UNSAT:   1" << std::endl;
        std::cout << " * UNKNOWN: 2" << std::endl;
        return 1;
    }
    
    int var_number = atoi(argv[1]);
    int time_limit = atoi(argv[2]);
    SATSolver solver (NULL, NULL);
    solver.new_vars(var_number);
    freopen(NULL, "rb", stdin);
    freopen(NULL, "wb", stdout);
    
    //FILE *f = fopen("inccr.log", "wt");
    FILE *f = NULL;

    while (true) {
        int code = read(f);
        if (code == 2) { // solve
            lbool res = solver.solve();
            if (res == l_True) {
                write(0);
                std::vector<lbool> model = solver.get_model();
                for (int i = 0; i < model.size(); i++) {
                    write((model[i] == l_True ? 1 : -1) * (i + 1));
                }
                write(0);
            } else if (res == l_False) {
                write(1);
            } else {
                write(2);
            }
            fflush(stdout);
        } else if (code == 1) {
            int num = read(f);
            solver.new_vars(num);
        } else if (code == 3) {
            break;
        } else {
            std::vector<Lit> lits;
            while (true) {
                int literal = read(f);
                if (literal == 0) {
                    break;
                }
                lits.push_back(Lit(abs(literal) - 1, literal < 0));
            }
            solver.add_clause(lits);
        }
    }
    
    fclose(f);

    return 0;
}
