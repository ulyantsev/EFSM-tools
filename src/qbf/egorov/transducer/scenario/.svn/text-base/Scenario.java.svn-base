package ru.ifmo.ctddev.genetic.transducer.scenario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class Scenario {
	
	private final static Random RANDOM = new Random();
	
	private ArrayList<Vertex> vertices;
	private ArrayList<ArrayList<Edge>> edges;
	
	private int edgesCount;
	
	private int initialVertex;
	
	public Scenario() {
		vertices = new ArrayList<Vertex>();
		edges = new ArrayList<ArrayList<Edge>>();
		edgesCount = 0;
		initialVertex = 0;
	}
	
	public void setInitialVertex(int initialVertex) {
		this.initialVertex = initialVertex;
	}
	
	public int getInitialVertex() {
		return initialVertex;
	}
	
	public String[] getSetOfInputs() {
		HashSet<String> set = new HashSet<String>();
		for (Vertex v : vertices) {
			if (!"".equals(v.event)) {
				set.add(v.event);
			}
		}
		String[] res = new String[set.size()];
		int i = 0;
		for (String s : set) {
			res[i++] = s;
		}
		return res;
	}
	
	public String[] getSetOfOutputs() {
		HashSet<String> set = new HashSet<String>();
		for (ArrayList<Edge> list : edges) {
			for (Edge e : list) {
				for (String s : e.actions) {
					set.add(s);
				}
			}
		}
		String[] res = new String[set.size()];
		int i = 0;
		for (String s : set) {
			res[i++] = s;
		}
		return res;
	}
	
	public int addVertex(String event) {
		Vertex v = new Vertex(event);
		vertices.add(v);
		edges.add(new ArrayList<Edge>());
		return vertices.size() - 1;
	}
	
	public void addEdge(int from, int to, List<String> actions) {
		if (from >= vertices.size()) {
			throw new IllegalArgumentException("No such vertex: " + from);
		}
		if (to >= vertices.size()) {
			throw new IllegalArgumentException("No such vertex: " + to);
		}
		Edge e = new Edge(from, to, actions);
		edges.get(from).add(e);
		edgesCount++;
	}

	private ArrayList<Path> paths;
	
	public ArrayList<Path> traverse(int pathsNum, int sizeLimit) {
		paths = new ArrayList<Path>();
		genPaths(initialVertex, new Path(), pathsNum, sizeLimit);
		return paths;
	}
	
	private void genPaths(int curVertex, Path curPath, int goal, int sizeLimit) {
		if (paths.size() == goal) {
			return;
		}
		if (curPath.size() > sizeLimit) {
			return;
		}
		if (curPath.size() > 0) {
			paths.add(curPath);
		}
		int outDegree = edges.get(curVertex).size();
		if (outDegree == 0) {
			return;
		}
		int[] p = new int[outDegree];
		for (int i = 0; i < outDegree; i++) {
			p[i] = i;
		}
		for (int i = outDegree - 1; i >= 1; i--) {
			int j = RANDOM.nextInt(i);
			int t = p[i];
			p[i] = p[j];
			p[j] = t;
		}
		for (int i = 0; i < outDegree; i++) {
			Path newPath = curPath.appendInput(vertices.get(curVertex).event);
			newPath = newPath.appendEdge(edges.get(curVertex).get(p[i]));
			genPaths(edges.get(curVertex).get(p[i]).to, newPath, goal, sizeLimit);
		}
	}

	public void addEdge(int from, int to, String[] actions) {
		addEdge(from, to, arrayToArrayList(actions));
	}

	private static ArrayList<String> arrayToArrayList(String[] array) {
		ArrayList<String> list = new ArrayList<String>();
		for (String s : array) {
			list.add(s);
		}
		return list;
	}
	
}
