pdf("plot.pdf", width=10, height=5)
plot(c(), c(), main="'+' = complete synthesis; 'o' = incomplete synthesis; different colors indicate different |S|", xlim = c(0.3, 44), ylim=c(1, 300), xlab = "Time (sec)", ylab = "Iterations", log="xy")
for (complete in c("complete", "incomplete")) {
    for(i in 3:12) {
        data = read.table(paste(complete, "-", i, ".plot", sep=""))
        points(data$V1, data$V2, col=(i - 2), pch=(if (complete == "complete") "+" else 1))
        #abline(lm(data$V2 ~ data$V1), col=(i - 2))
        #lines(predict(lm(data$V2~data$V1+I(data$V1^2))), col=(i - 2))
    }
}
dev.off()
