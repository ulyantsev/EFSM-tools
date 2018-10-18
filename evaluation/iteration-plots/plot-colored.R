pdf("plot.pdf", width=8, height=5)
plot(c(), c(), main="", xlim = c(0.3, 45), ylim=c(1, 300), xlab = "Total synthesis time (s)", ylab = "Number  of iterations", log="xy")
for (complete in c("complete")) {
    for(i in 3:12) {
        data = read.table(paste(complete, "-", i, ".plot", sep=""))
        points(data$V1, data$V2, col=(i - 3) / 5 + 2, pch=if (i <= 7) 1 else 5)
        #abline(lm(data$V2 ~ data$V1), col=(i - 2))
        #lines(predict(lm(data$V2~data$V1+I(data$V1^2))), col=(i - 2))
    }
}
dev.off()
