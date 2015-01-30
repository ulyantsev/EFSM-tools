package qbf.egorov.transducer.algorithm;

public interface Individual {
	public Individual[] crossover(Individual other);
	public Individual mutate();
}
