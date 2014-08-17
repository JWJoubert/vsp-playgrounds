/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.sim2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.socialnetworks.utils.CollectionUtils;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 *
 */
public class Sampler {
	
	private final Collection<ProxyPerson> population;
	
	private final Hamiltonian hamiltonian;
	
	private final MutatorFactory mutatorFactory;
	
	private final Random random;
	
	private SamplerListener listener;
	
	public Sampler(Collection<ProxyPerson> population, Hamiltonian hamiltonian, MutatorFactory factory, Random random) {
		this.population = population;
		this.hamiltonian = hamiltonian;
		this.mutatorFactory = factory;
		this.random = random;
		
		listener = new DefaultListener();
	}
	
	
	public void setSamplerListener(SamplerListener listener) {
		this.listener = listener;
	}
	
	public void run(long iters, int numThreads) {
		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(population.size(), numThreads);
		List<ProxyPerson>[] segments = CollectionUtils.split(population, n);
		/*
		 * create threads
		 */
		Thread[] threads = new Thread[numThreads];
		for(int i = 0; i < numThreads; i++) {
			Mutator thisMutator = mutatorFactory.newInstance();
			Random thisRandom = new XORShiftRandom(random.nextLong());
			threads[i] = new Thread(new SampleThread(segments[i], thisMutator, iters, thisRandom));
			threads[i].start();
		}
		/*
		 * wait for threads
		 */
		for(int i = 0; i < numThreads; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class SampleThread implements Runnable {

		private final List<ProxyPerson> population;
		
		private final Mutator mutator;
		
		private final Random random;
		
		private final long iterations;
		
		public SampleThread(List<ProxyPerson> population, Mutator mutator, long iterations, Random random) {
			this.population = population;
			this.mutator = mutator;
			this.iterations = iterations;
			this.random = random;
		}
		
		@Override
		public void run() {
			for(long i = 0; i < iterations; i++) {
				step();
			}
		}
	
		public void step() {
			/*
			 * select person
			 */
			int idx = random.nextInt(population.size());
			ProxyPerson person = population.get(idx);
			/*
			 * select mutator
			 */
			double H_before = hamiltonian.evaluate(person);
			boolean accepted = false;
			if (mutator.modify(person)) {
				/*
				 * evaluate
				 */
				double H_after = hamiltonian.evaluate(person);

				double p = 1 / (1 + Math.exp(H_after - H_before));

				if (p >= random.nextDouble()) {
					accepted = true;
				} else {
					mutator.revert(person);
				}
			}
			
			listener.afterStep(Sampler.this.population, person, accepted);
		}

	}
	
	public static void main(String args[]) {
		List<ProxyPerson> population = new ArrayList<ProxyPerson>(1000);
		for(int i = 0; i < 1000; i++) {
			population.add(new ProxyPerson(String.valueOf(i)));
		}
		
//		Sampler sampler = new Sampler(new XORShiftRandom());
//		sampler.setHamiltonian(new DummyHamiltonian());
//		sampler.setMutatorFactory(new DummyMutatorFactory());
//		sampler.setSamplerListener(new BlockingSamplerListener(new DefaultListener(), sampler, 10));
//		
//		sampler.run(population, 200);
	}
	
	private static class DummyHamiltonian implements Hamiltonian {

		@Override
		public double evaluate(ProxyPerson person) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 0;
		}
		
	}
	
	private static class DummyMutator implements Mutator {

		@Override
		public boolean modify(ProxyPerson person) {
			return true;
		}

		@Override
		public void revert(ProxyPerson person) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private static class DummyMutatorFactory implements MutatorFactory {

		@Override
		public Mutator newInstance() {
			return new DummyMutator();
		}
		
	}
	
	private static class DefaultListener implements SamplerListener {

		@Override
		public void afterStep(Collection<ProxyPerson> population, ProxyPerson person, boolean accepted) {
			// does nothing
		}
		
	}
}
