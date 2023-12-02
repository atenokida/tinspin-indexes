/*
 * Copyright 2009-2017 Tilmann Zaeschke. All rights reserved.
 * 
 * This file is part of TinSpin.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinspin.index.kdtree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.tinspin.index.Index.*;

public class KDTreeTest {

	protected int getDimension(double[][] point_list) {
		return point_list[0].length;
	}

	@Test
	public void smokeTestShort() {
		double[][] point_list = {{2,3}, {5,4}, {9,6}, {4,7}, {8,1}, {7,2}};
		smokeTest(point_list);
	}
	
	@Test
	public void smokeTestDupl() {
		double[][] point_list = {{2,3}, {2,3}, {2,3}, {2,3}, {2,3}, {2,3}};
		smokeTest(point_list);
	}
	
	@Test
	public void smokeTest2D_0() {
		// double[][] point_list = new double[2000000][2];
		// Random R = new Random(0);
		// for (double[] p : point_list) {
		// 	Arrays.setAll(p, (i) -> R.nextInt(100));
		// }
		double [][] point_list = {{2,6}, {3,1}, {5,4}, {8,7}, {10,2}, {13,3}};
		// smokeTest(point_list);
		KNN2DTest_(point_list);
	}
	
	@Test
	public void smokeTest2D_1() {
		double[][] point_list = new double[1000000][3];
		double[][] search_list = new double[500000][3];
		Random R = new Random(0);
		for (double[] p : point_list) {
			Arrays.setAll(p, (i) -> R.nextInt(10000));
		}
		Random R2 = new Random(1);
		for (double[] p : search_list) {
			Arrays.setAll(p, (i) -> R2.nextInt(10000));
		}
		knnTest(point_list, search_list, false);
		// smokeTest(point_list);
	}
	
	@Test
	public void smokeTest2D_245() {
		double[][] point_list = new double[5][2];
		Random R = new Random(245);
		for (double[] p : point_list) {
			Arrays.setAll(p, (i) -> { return (double)R.nextInt(100);} );
		}
		smokeTest(point_list);
	}
	
	/**
	 * Tests handling of all points being on a line, i.e. correct handling of <=, etc.
	 */
	@Test
	public void smokeTest2D_Line() {
		double[][] point_list = new double[10_000][3];
		int n = 0;
		for (double[] p : point_list) {
			p[0] = n % 3;
			p[1] = n++; 
			p[2] = n % 5;
		}
		List<double[]> list = Arrays.asList(point_list);
		Collections.shuffle(list);
		point_list = list.toArray(point_list);
		smokeTest(point_list);
	}
	
	@Test
	public void smokeTest5D() {
		double[][] point_list = new double[20][5];
		Random R = new Random(0);
		for (double[] p : point_list) {
			Arrays.setAll(p, (i) -> { return (double)R.nextInt(100);} );
		}
		smokeTest(point_list);
	}
	
	@Test
	public void smokeTest1D_Large() {
		double[][] point_list = new double[100_000][1];
		Random R = new Random(0);
		for (double[] p : point_list) {
			Arrays.setAll(p, (i) -> { return (double)R.nextInt(100);} );
		}
		smokeTest(point_list);
	}
	
	@Test
	public void smokeTest3D_Large() {
		double[][] point_list = new double[100_000][3];
		Random R = new Random(0);
		for (double[] p : point_list) {
			Arrays.setAll(p, (i) -> { return (double)R.nextInt(100);} );
		}
		smokeTest(point_list);
	}
	
	@Test
	public void smokeTest10D_Large() {
		double[][] point_list = new double[100_000][10];
		Random R = new Random(0);
		for (double[] p : point_list) {
			Arrays.setAll(p, (i) -> { return (double)R.nextInt(100);} );
		}
		smokeTest(point_list);
	}
	
	private void smokeTest(double[][] point_list) {
		
		int dim = point_list[0].length;
		
		KDTree<double[]> tree = KDTree.create(dim);
		
		for (double[] data : point_list) {
			tree.insert(data, data);
		}
		
		
	    //System.out.println(tree.toStringTree());
		
		double[] queryPoint = {9, 4};
		
		PointIteratorKnn<double[]> resultQueryPoint = tree.queryKnn(queryPoint, 2);
		
		if (resultQueryPoint.hasNext()) {
			System.out.printf("O ponto mais próximo de [10, 2] é %s\n", resultQueryPoint.next());
		}
		
		
		for (double[] key : point_list) {
			if (!tree.contains(key)) {
				throw new IllegalStateException(Arrays.toString(key));
			}
		}

		for (double[] key : point_list) {
//			System.out.println("kNN query: " + Arrays.toString(key));
			PointIteratorKnn<double[]> iter = tree.queryKnn(key, 2);
			if (!iter.hasNext()) {
				throw new IllegalStateException("kNN() failed: " + Arrays.toString(key));
			}
			double[] answer = iter.next().point();
			if (answer != key && !Arrays.equals(answer, key)) {
				throw new IllegalStateException("Expected " + Arrays.toString(key) + " but got " + Arrays.toString(answer));
			}
		}
	    
		for (double[] key : point_list) {
//			System.out.println(tree.toStringTree());
//			System.out.println("Removing: " + Arrays.toString(key));
			if (!tree.contains(key)) {
				throw new IllegalStateException("containsExact() failed: " + Arrays.toString(key));
			}
			double[] answer = tree.remove(key); 
			if (answer != key && !Arrays.equals(answer, key)) {
				throw new IllegalStateException("Expected " + Arrays.toString(key) + " but got " + Arrays.toString(answer));
			}
		}
	}

	private void KNN2DTest_(double[][] point_list) {

		int dim = getDimension(point_list);

		System.out.println("Number of points: " + point_list.length);
		System.out.println("Number of dimensions: " + dim);

		KDTree<double[]> tree = KDTree.create(dim);

		long start_time = System.currentTimeMillis();

		for (double[] data : point_list) {
			tree.insert(data, data);
		}

		long end_time = System.currentTimeMillis();

		long elapsed_time = end_time - start_time;

		System.out.println("Time to build the k-d tree: " + elapsed_time + "ms");



		double[] query_point = {9, 4};

		PointIteratorKnn<double[]> result_query_point = tree.queryKnn(query_point, 1);
		
		if (result_query_point.hasNext()) {
			System.out.printf("O ponto mais próximo de %s é %s\n", Arrays.toString(query_point), result_query_point.next());
		}
	}

	public void knnTest(double[][] point_list, double[][] search_list, boolean print) {

		int dim = getDimension(point_list);

		// System.out.println(Arrays.toString(point_list[0]));
		// System.out.println(Arrays.toString(search_list[0]));


		System.out.println(" ");
		System.out.println("--------------------------- KD-TREE ---------------------------");
		System.out.println("Number of points: " + point_list.length);
		System.out.println("Number of dimensions: " + dim);
		System.out.println("Number of points to search: " + search_list.length);

		KDTree<double[]> tree = KDTree.create(dim);

		long start_time = System.currentTimeMillis();

		for (double[] data : point_list) {

			tree.insert(data, null);

		}

		long end_time = System.currentTimeMillis();

		long elapsed_time = end_time - start_time;

		System.out.println("Time to build the k-d tree: " + elapsed_time + "ms");

		start_time = System.currentTimeMillis();

		for (double[] point : search_list) {

			PointIteratorKnn<double[]> query_result = tree.queryKnn(point, 1);

			// if (print_search && query_result.hasNext()) {

			// 	System.out.printf("O ponto mais próximo de %s é %s\n", Arrays.toString(point), query_result.next());

			// }

		}

		end_time = System.currentTimeMillis();

		elapsed_time = end_time - start_time;

		System.out.println("Time to query " + search_list.length + " points in the k-d tree:" + elapsed_time + "ms");
	}

}
