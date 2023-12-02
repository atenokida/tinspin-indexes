package org.tinspin.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;

import org.tinspin.index.kdtree.KDTreeTest;
import org.tinspin.index.rtree.RTreeMixedQueryTest;
import org.tinspin.covetree.CoverTreeTest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class TreeComparison {

	private static double[][] getBounds(double[][] point_list) {
		int dimensions = point_list[0].length;

		double[][] bounds = new double[dimensions][2];
		for (int i = 0; i < dimensions; i++) {
			bounds[i][0] = Double.MAX_VALUE;
			bounds[i][1] = Double.MIN_VALUE;
		}

		for (int i = 0; i < point_list.length; i++) {
			for (int j = 0; j < dimensions; j++) {
				bounds[j][0] = Math.min(bounds[j][0], point_list[i][j]);
				bounds[j][1] = Math.max(bounds[j][1], point_list[i][j]);
			}
		}

		return bounds;

	}

	@Deprecated
	private static double[] __getBounds(double[][] point_list) {
		double minValue = Double.MAX_VALUE;
		double maxValue = Double.MIN_VALUE;

		for (double[] array : point_list) {
			for (double value : array) {
				minValue = Math.min(minValue, value);
				maxValue = Math.max(maxValue, value);
			}
		}

		// System.out.println("Minimum value: " + minValue);
		// System.out.println("Maximum value: " + maxValue);

		return new double[] { minValue, maxValue };

	}

	private double[][] threeDimensional_1() {
		String filePath = "src/test/resources/3d/3D_spatial_network.txt";

		List<double[]> points = new ArrayList<>();

		// DecimalFormat df = new DecimalFormat("##");

		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;

			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");

				double[] array = Arrays.stream(values, 1, values.length)
						.mapToDouble(Double::parseDouble)
						.toArray();

				// for (int i = 0; i < array.length; i++) {
				// // array[i] = Double.parseDouble(df.format(array[i]));
				// array[i] = Math.round(array[i]);
				// }

				points.add(array);
			}

		} catch (IOException e) {
			e.printStackTrace();

		}

		double[][] point_list = points.toArray(new double[0][]);

		return point_list;

	}

	private double[][] twoDimensional_1() {
		String csvFilePath = "src/test/resources/2d/online_retail_ii.csv";

		List<double[]> points = new ArrayList<>();

		try (FileReader reader = new FileReader(csvFilePath);
				CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {

			for (CSVRecord csvRecord : csvParser) {
				// Validação de registros inválidos (sem Customer ID)
				if (csvRecord.size() < 8 || csvRecord.get(6).isEmpty())
					continue;

				// 1 = Product ID; 6 = Customer ID
				String str = csvRecord.get(1);
				double productId = 0;
				for (char c : str.toCharArray()) {
					productId += (double) c;
				}

				double costumerId = Double.parseDouble(csvRecord.get(6));

				double[] values = { productId, costumerId };
				points.add(values);

			}
		} catch (IOException e) {
			e.printStackTrace();

		}

		double[][] point_list = points.toArray(new double[0][]);

		return point_list;

	}

	private double[][] fourDimensional_1() {
		String csvFilePath = "src/test/resources/2d/online_retail_ii.csv";

		List<double[]> points = new ArrayList<>();

		try (FileReader reader = new FileReader(csvFilePath);
				CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {

			for (CSVRecord csvRecord : csvParser) {
				// Validação de registros inválidos (sem Customer ID)
				if (csvRecord.size() < 8 || csvRecord.get(6).isEmpty())
					continue;

				// 1 = Product ID; 3 = Quantity; 5 = UnitPrice; 6 = Customer ID;
				String str = csvRecord.get(1);
				double productId = 0;
				for (char c : str.toCharArray()) {
					productId += (double) c;
				}

				double quantity = Double.parseDouble(csvRecord.get(3));
				double unitPrice = Double.parseDouble(csvRecord.get(5));
				double costumerId = Double.parseDouble(csvRecord.get(6));

				double[] values = { productId, quantity, unitPrice, costumerId };
				points.add(values);

			}
		} catch (IOException e) {
			e.printStackTrace();

		}

		double[][] point_list = points.toArray(new double[0][]);

		return point_list;
	}

	public static void main(String[] args) {

		TreeComparison comparison = new TreeComparison();
		KDTreeTest kdtree = new KDTreeTest();
		RTreeMixedQueryTest rtree = new RTreeMixedQueryTest();
		CoverTreeTest covertree = new CoverTreeTest();

		// Datasets
		// double[][] point_list = comparison.twoDimensional_1();
		double[][] point_list = comparison.threeDimensional_1();
		// double[][] point_list = comparison.fourDimensional_1();

		// Pontos aleatórios para busca - Proporção de 10%
		int dimension = point_list[0].length;
		int points = point_list.length / 10;
		double[][] search_list = new double[points][dimension];
		double[][] bounds = getBounds(point_list);
		int seed = 0;
		Random R = new Random(seed);
		for (double[] p : search_list) {
			Arrays.setAll(p, (i) -> {
				double lower_bound = bounds[i][0];
				double upper_bound = bounds[i][1];
				return R.nextDouble(upper_bound - lower_bound) + lower_bound;
			});
		}

		kdtree.knnTest(point_list, search_list, false);
		rtree.knnTest(point_list, search_list, false);
		covertree.knnTest(point_list, search_list, false);

	}

}
