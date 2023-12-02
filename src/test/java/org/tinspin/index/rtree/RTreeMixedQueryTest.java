/*
 * Copyright 2017 Christophe Schmaltz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinspin.index.rtree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;
import org.tinspin.index.BoxDistance;
import org.tinspin.index.Index.BoxEntryKnn;

import static org.tinspin.index.Index.*;

public class RTreeMixedQueryTest {
	
	private static final int DIMS = 8;
	private static final int REPEAT = 10;

	// seed chosen randomly using a well equilibrated dice :-)
	// [makes test reproducible]
	Random rnd = new Random(4);
	{
		// fail here if Random implementation changes
		assertEquals(-4969378402838085704l, rnd.nextLong());
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void test() {
		RTree<String> tree = RTree.createRStar(DIMS);

		int N_ELEMENTS = 100000;
		for (int i = 0; i < N_ELEMENTS; i++) {
			double[] position = randDouble(DIMS);
			assert tree.queryExact(position, position) == null;
			tree.insert(position, "#" + i);
		}

		Iterable<BoxEntryKnn<String>> q = tree.queryRangedNearestNeighbor(
				createAndFill( 1 ), 
				BoxDistance.CENTER, BoxDistance.EDGE,
				createAndFill( 0.5 ), createAndFill( 1 ));


		double lastDistance = 0;
		int maxQueueSize = 0;
		int nElements = 0;
		Set<String> duplicateCheck = new HashSet<>();
		for (Iterator<BoxEntryKnn<String>> iterator = q.iterator(); iterator.hasNext();) {
			BoxEntryKnn<String> e = iterator.next();
			//System.out.println(nElements + " " + iterator + " " + e);

			assertTrue(e.value() + " @" + nElements, duplicateCheck.add(e.value()));
			assertTrue("Order should be ascending", lastDistance <= e.dist());
			lastDistance = e.dist();
			nElements++;
			maxQueueSize = Math.max(maxQueueSize, ((RTreeMixedQuery) iterator).queueSize());

			if (true) {
				iterator.remove();
				assertEquals(tree.size(), N_ELEMENTS - nElements);
			}
		}

		perfTestNN(tree);
		System.out.println("maxQueueSize=" + maxQueueSize + " / ");
		System.out.println("nElements=" + nElements);
		// if (DIMS == 3) { 
		// 	assertEquals("Test should be reproducible thanks to fixed seed", 12582, nElements);
		// }
	}

	private void perfTestNN(RTree<String> tree) {
		int k = tree.size() / 8;
		double[] center = createAndFill( 1 );

		{
			Iterable<BoxEntryKnn<String>> q = tree.queryRangedNearestNeighbor(
					center, BoxDistance.EDGE,
					BoxDistance.EDGE, Filter.ALL);
			RTreeQueryKnn<String> res = tree.queryKnn(center, k, BoxDistance.EDGE);
			// test that we get the same results
			Iterator<BoxEntryKnn<String>> iterator = q.iterator();
			int i=0;
			for (; iterator.hasNext();) {
				assertTrue("I="+i, res.hasNext());
				assertEquals(res.next().value(), iterator.next().value());
				i++;
				if (i >= k)
					break;
			}
			assertFalse(res.hasNext());
		}

		fillProcessorCache();

		long timeRef = timeOf(() -> {
			RTreeQueryKnn<String> res = tree.queryKnn(center, k, BoxDistance.EDGE);
			int cnt = 0;
			for(;res.hasNext();) {
				cnt++;
				BoxEntryKnn<String> e = res.next();
				assertNotNull(e);
			}
			assertEquals(k, cnt);
		});

		fillProcessorCache();

		long timeMixed = timeOf(() -> {
			Iterable<BoxEntryKnn<String>> q = tree.queryRangedNearestNeighbor(
					center, BoxDistance.EDGE,
					BoxDistance.EDGE, Filter.ALL);
			int cnt = 0;
			// if (false) {
			// 	/* 
			// 	 * A lot of the speedup is simply due to the copy. Adding this
			// 	 * makes the code 6,28 times slower for 12500 neighbors out of 100000.
			// 	 * 
			// 	 * Probably cache locality as my code is only faster for large results.
			// 	 * 
			// 	 * It seems as if executing the query multiple times is better than caching the results...
			// 	 */
			// 	List<BoxEntryKnn<String>> arr = new ArrayList<>();
			// 	q.forEach(arr::add);
			// 	q = arr;
			// }
			for (Iterator<BoxEntryKnn<String>> iterator = q.iterator(); iterator.hasNext();) {
				BoxEntryKnn<String> e = iterator.next();
				assertNotNull(e);
				cnt++;
				if (cnt >= k)
					break;
			}
		});

		System.out.println("timeMixed=" + timeMixed + ", timeRef=" + timeRef + " # speedup:" + (timeRef / (double)timeMixed));
	}

	private void fillProcessorCache() {
		// 20MB
		int[] mem = new int[1024 * 1024 * 20];
		for (int i = 0; i < mem.length; i++) {
			mem[i] = i;
		}
	}

	public long timeOf(Runnable run) {
		final int nRuns = REPEAT;
		long time = 0;
		for (int i = 0; i <= nRuns; i++) {
			long timeBefore = System.nanoTime();
			run.run();
			long delta = System.nanoTime() - timeBefore;
			if (i > 0) {
				// ignore the first one for warm up
				time += delta;
			}
		}
		return time / nRuns;
	}

	private static double[] createAndFill(double d) {
		double[] ret = new double[DIMS];
		Arrays.fill(ret, d);
		return ret;
	}
	
	private double[] randDouble(int n) {
		double[] r = new double[n];
		for (int i = 0; i < n; i++) {
			r[i] = rnd.nextDouble();
		}
		return r;
	}


	/**
	 * Função utilizada somente para teste.
	 * O seguinte conjunto de pontos {(2,6),(3,1),(5,4),(8,7),(10,2),(13,3)}
	 * para a entrada (9,4), deve retornar como ponto mais próximo (10,2).
	 **/
	private void smokeTestShort() {

		double [][] point_list = {{2,6}, {3,1}, {5,4}, {8,7}, {10,2}, {13,3}};
		double[] query_point = {9, 4}; //(10, 2)
		double[] result = {10, 2};

		int dim = point_list[0].length;

		RTree<double[]> tree = RTree.createRStar(dim);

		for (double[] data : point_list) {
			tree.insert(data, data);
		}

		RTreeQueryKnn<double[]> query_result = tree.queryKnn(query_point, 1);

		assert Arrays.equals(query_result.next().value(), result);

	}

	public void knnTest(double[][] entries, double[][] search_list, boolean print) {
		
		int dim = entries[0].length;

		// debug
		// System.out.println(Arrays.toString(point_list[1]));
		// System.out.println(Arrays.toString(search_list[1]));

		System.out.println(" ");
		System.out.println("--------------------------- R*-TREE ---------------------------");
		System.out.println("DIMENSIONS -> " + dim);
		System.out.println("Number of points: " + entries.length);
		System.out.println("Number of points to search: " + search_list.length);

		RTree<double[]> tree = RTree.createRStar(dim);

		long start_time = System.currentTimeMillis();

		for (double[] data : entries) {

			tree.insert(data, data);

		}

		long end_time = System.currentTimeMillis();

		long elapsed_time = end_time - start_time;

		System.out.println("Time to build the R*-Tree: " + elapsed_time + "ms");

		start_time = System.currentTimeMillis();

		for (double[] point : search_list) {

			RTreeQueryKnn<double[]> query_result = tree.queryKnn(point, 1);

			if (print && query_result.hasNext()) {

				System.out.printf("O ponto mais próximo de %s é %s\n", Arrays.toString(point), Arrays.toString(query_result.next().value()));
			
			}

		}

		end_time = System.currentTimeMillis();

		elapsed_time = end_time - start_time;

		System.out.println("Time to find " + search_list.length + " points in the R*-Tree:" + elapsed_time + "ms");

	}
}
