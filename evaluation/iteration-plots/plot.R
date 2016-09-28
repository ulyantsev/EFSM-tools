pdf("plot.pdf", width=13, height=9)
plot(c(), c(), main="'+' = complete synthesis; 'o' = incomplete synthesis; different colors indicate different |S|", xlim = c(0.3, 70), ylim=c(1, 300), xlab = "Time (sec)", ylab = "Iterations", log="xy")
for (complete in c("complete", "incomplete")) {
    for(i in 3:12) {
        data = read.table(paste(complete, "-", i, ".plot", sep=""))
        #data = data[data$V1 < 10,]
        #data = data[data$V2 < 60,]
        points(data$V1, data$V2, col=(i - 2), pch=(if (complete == "complete") "+" else 1))
        #abline(lm(data$V2 ~ data$V1), col=(i - 2))
        #lines(predict(lm(data$V2~data$V1+I(data$V1^2))), col=(i - 2))
    }
}
dev.off()
