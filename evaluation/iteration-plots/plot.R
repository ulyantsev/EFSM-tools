pdf("plot.pdf", width=7, height=4.9)
par(mar = c(3.1, 3.4, 0.3, 0.3))
#plot(c(), c(), xlim = c(0.3, 44), ylim=c(1, 300), log="xy", ann=FALSE)
plot(c(), c(), xlim = c(0.3, 260), ylim=c(1, 620), log="xy", ann=FALSE)
grid(6, 5)
mtext(side = 1, text = "Execution time (sec)", line = 2)
mtext(side = 2, text = "Iterations", line = 2.3)
for (complete in c("complete", "incomplete")) {
    for(i in 3:12) {
        data = read.table(paste(complete, "-", i, ".plot", sep=""))
        points(data$V1, data$V2, pch=1)
        #abline(lm(data$V2 ~ data$V1), col=(i - 2))
        #lines(predict(lm(data$V2~data$V1+I(data$V1^2))), col=(i - 2))
    }
}
dev.off()
