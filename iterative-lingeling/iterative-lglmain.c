#include "lglib.h"

#include <assert.h>
#include <ctype.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>

static void (*sig_alrm_handler)(int);
static int caughtalarm = 0;

static void catchalrm (int sig) {
  if (!caughtalarm) {
    caughtalarm = 1;
  }
}

static int checkalarm (void * ptr) {
  return caughtalarm;
}

int main (int argc, char ** argv) {
    if (argc != 2) {
        printf("Run: iterative-lingeling <varNumber>\n");
        return 1;
    }
    int var_number = atoi(argv[1]);
    int timelimit = 0;
    LGL * lgl = lglinit();
    lglseterm(lgl, checkalarm, &caughtalarm);
    sig_alrm_handler = signal(SIGALRM, catchalrm);
    char buf[1024];
    int res, lit, i;

    int *fixed = malloc(var_number * sizeof(int));
    memset(fixed, 0, var_number * sizeof(int));

    while (1) {
        scanf("%s", buf);
        if (strcmp(buf, "fix") == 0) {
            scanf("%s", buf);
            fixed[atoi(buf) - 1] = 1;
        } else if (strcmp(buf, "solve") == 0) {
            scanf("%s", buf);
            timelimit = atoi(buf);
            alarm(timelimit);
            res = lglsat(lgl);
            alarm(0);
            if (res == 10) {
                printf("SAT\nv ");
                for (i = 1; i <= var_number; i++) {
                    printf(lglusable(lgl, i) && lglderef(lgl, i) > 0 ? "" : "-");
                    printf("%d ", i);
                }
                printf("\n");
            } else if (res == 20) {
                printf("UNSAT\n");
            } else {
                printf("UNKNOWN\n");
            }
            fflush(stdout);
        } else if (strcmp(buf, "halt") == 0) {
            return 0;
        } else {
            lit = atoi(buf);
            if (abs(lit) > var_number) {
                printf("Bad literal.\n");
                return 2;
            }
            lgladd(lgl, lit);
            if (lit != 0 && !lglfrozen(lgl, lit) && !fixed[abs(lit) - 1]) {
                lglfreeze(lgl, abs(lit));
            }
        }
    }
}
