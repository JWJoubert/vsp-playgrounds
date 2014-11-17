/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.pieter.pseudosimulation.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * @author illenberger
 *         <p/>
 *         Used to divide tasks between threads in {@link playground.pieter.pseudosimulation.mobsim.PSim}. Copied here to
 *         avoid dependence.
 */
public class CollectionUtils {

    public static <T> List<T>[] split(Collection<T> set, int n) {
        if (set.size() >= n) {
            @SuppressWarnings("unchecked")
            List<T>[] arrays = new List[n];
            int minSegmentSize = (int) Math.floor(set.size() / (double) n);

            int start = 0;
            int stop = minSegmentSize;

            Iterator<T> it = set.iterator();

            for (int i = 0; i < n - 1; i++) {
                int segmentSize = stop - start;
                List<T> segment = new ArrayList<>(segmentSize);
                for (int k = 0; k < segmentSize; k++) {
                    segment.add(it.next());
                }
                arrays[i] = segment;
                start = stop;
                stop += segmentSize;
            }

            int segmentSize = set.size() - start;
            List<T> segment = new ArrayList<>(segmentSize);
            for (int k = 0; k < segmentSize; k++) {
                segment.add(it.next());
            }
            arrays[n - 1] = segment;

            return arrays;
        } else {
            throw new IllegalArgumentException("n must not be smaller set size!");
        }
    }

    /**
     * Split the collection into weights.length
     *
     * @param set     the colleciton to split
     * @param weights is an array of weights that will be normalized, so sum can be larger than one, but all should be positive
     * @return an array of lists each containing a number of objects from the collection, the number determined by the weight relative to total weight
     */
    public static <T> List<T>[] split(Collection<T> set, double[] weights) {
        if (set.size() >= weights.length) {
            @SuppressWarnings("unchecked")
            List<T>[] arrays = new List[weights.length];

            double totalweight = 0.0;
            for (double w : weights)
                totalweight += w;
            double[] relativeWeight = new double[weights.length];
            for (int i = 0; i < weights.length; i++)
                relativeWeight[i] = weights[i] / totalweight;
            int[] listSizes = new int[weights.length];
            for (int i = 0; i < weights.length; i++)
                listSizes[i] = (int) (weights[i] / totalweight * (double) set.size());
            int allocated = 0;
            for (int ls : listSizes)
                allocated += ls;
            int remainder = set.size() - allocated;

            listSizes[0] += remainder;

            Iterator<T> it = set.iterator();
            for (int i = 0; i < listSizes.length; i++) {
                List<T> segment = new ArrayList<>(listSizes[i]);
                for (int k = 0; k < listSizes[i]; k++) {
                    segment.add(it.next());
                }
                arrays[i] = segment;
            }
            return arrays;
        } else {
            throw new IllegalArgumentException("weigths.length must not be smaller than set size!");
        }
    }

    public static <T> List<T>[] split(Collection<T> set, int[] weights) {
        double[] doubleWeights = new double[weights.length];
        for(int i=0;i<weights.length;i++){
            doubleWeights[i] = weights[i];
        }
        return split(set,doubleWeights);
    }

    public static void main(String[] args) {
        ArrayList<Integer> a = new ArrayList<>();
        for (int i = 1; i <= 21; i++) {
            a.add(i);
        }
        List<Integer>[] out;
        out = split(a, new double[]{15.0, 5.0});
        out = split(a, new double[]{15.0, 5.0, 1.0});
        out = split(a, new double[]{15.0, 5.0, 5.1});


    }
}
