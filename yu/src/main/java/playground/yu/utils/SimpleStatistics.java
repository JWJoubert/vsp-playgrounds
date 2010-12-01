/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleStatistics.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 *
 */
package playground.yu.utils;

import java.util.Collection;

import playground.yu.utils.container.Collection2Array;

/**
 * offers a few simple statistics function
 * 
 * @author yu
 * 
 */
public class SimpleStatistics {
	// average
	public static double average(double[] array, int firstIdx, int lastIdx) {
		double sum = 0.0;
		for (int i = firstIdx; i <= lastIdx; i++) {
			sum += array[i];
		}

		return sum / (lastIdx - firstIdx + 1);
	}

	public static double average(double[] array) {
		return average(array, 0, array.length - 1);
	}

	public static double average(Collection<Double> collection) {
		return average(Collection2Array.toArrayFromDouble(collection));
	}

	public static double doubleAverage(Collection<Integer> collection) {
		return average(Collection2Array.toDoubleArray(collection));
	}

	// min
	public static double min(double[] array, int firstIdx, int lastIdx) {
		double min = array[firstIdx];
		for (int i = firstIdx + 1; i <= lastIdx; i++) {
			min = Math.min(min, array[i]);
		}
		return min;
	}

	public static double min(double[] array) {
		return min(array, 0, array.length - 1);
	}

	public static double min(Collection<Double> collection) {
		return min(Collection2Array.toArrayFromDouble(collection));
	}

	public static int min(Collection<Integer> collection) {
		return (int) min(Collection2Array.toDoubleArray(collection));
	}

	// max
	public static double max(double[] array, int firstIdx, int lastIdx) {
		double min = array[firstIdx];
		for (int i = firstIdx + 1; i <= lastIdx; i++) {
			min = Math.max(min, array[i]);
		}
		return min;
	}

	public static double max(double[] array) {
		return max(array, 0, array.length - 1);
	}

	public static double max(Collection<Double> collection) {
		return max(Collection2Array.toArrayFromDouble(collection));
	}

	public static int max(Collection<Integer> collection) {
		return (int) max(Collection2Array.toDoubleArray(collection));
	}

	// variance
	public static double variance(double[] array, int firstIdx, int lastIdx) {
		double N = lastIdx - firstIdx + 1, avg = average(array, firstIdx,
				lastIdx);
		double sum = 0.0;
		for (int i = firstIdx; i <= lastIdx; i++) {
			sum += array[i] * array[i];
		}

		return sum / N - avg * avg;// var(x)=E(x^2)-E(x)^2
	}

	public static double variance(double[] array) {
		return variance(array, 0, array.length - 1);
	}

	public static double variance(Collection<Double> collection) {
		return variance(Collection2Array.toArrayFromDouble(collection));
	}

	public static double doubleVariance(Collection<Integer> collection) {
		return variance(Collection2Array.toDoubleArray(collection));
	}
}
