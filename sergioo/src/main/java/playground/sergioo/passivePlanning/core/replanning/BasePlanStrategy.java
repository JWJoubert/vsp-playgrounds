package playground.sergioo.passivePlanning.core.replanning;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.utils.misc.Counter;

import playground.sergioo.passivePlanning.api.population.BasePerson;

public class BasePlanStrategy implements PlanStrategy {

	//Constants
	private static final Logger log = Logger.getLogger(BasePlanStrategy.class);

	//Static Classes
	private static class BasePlanThread extends Thread {
		
		//Attributes
		private final Collection<BasePerson> persons = new LinkedList<BasePerson>();
		private final Counter counter;
		
		//Methods
		public BasePlanThread(Counter counter, String name) {
			super(name);
			this.counter = counter;
		}
		@Override
		public void run() {
			for(BasePerson person:persons) {
				person.getBasePlan().getAndSelectPlan();
				counter.incCounter();
			}
		}
		public void addPerson(BasePerson person) {
			persons.add(person);
		}
		
	}
	private static class ExceptionHandler implements UncaughtExceptionHandler {

		//Attributes
		private final AtomicBoolean hadException;

		//Constructors
		public ExceptionHandler(final AtomicBoolean hadException) {
			this.hadException = hadException;
		}

		//Methods
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			log.error("Thread " + t.getName() + " died with exception. Will stop after all threads finished.", e);
			this.hadException.set(true);
		}

	}

	//Attributes
	private int count;
	private final BasePlanThread[] basePlanThreads;
	private final AtomicBoolean hadException = new AtomicBoolean(false);
	private final ExceptionHandler exceptionHandler = new ExceptionHandler(hadException);

	//Constructors
	public BasePlanStrategy(int numThreads) {
		if(numThreads<1)
			numThreads = 1;
		basePlanThreads = new BasePlanThread[numThreads];
	}

	//Methods
	@Override
	public void addStrategyModule(PlanStrategyModule module) {
		
	}
	@Override
	public int getNumberOfStrategyModules() {
		return 0;
	}
	@Override
	public void run(Person person) {
		if(person instanceof BasePerson) {
			basePlanThreads[count % basePlanThreads.length].addPerson((BasePerson)person);
			count++;
		}
	}
	@Override
	public void init() {
		Counter counter = new Counter("[BasePlanStrategy] handled person # ");
		for (int i=0; i<basePlanThreads.length; i++) {
			basePlanThreads[i] = new BasePlanThread(counter, "BasePlanStrategy." + i);
			basePlanThreads[i].setUncaughtExceptionHandler(this.exceptionHandler);
		}
	}
	@Override
	public void finish() {
		for (Thread thread : this.basePlanThreads)
			thread.start();
		try {
			for (Thread thread : this.basePlanThreads)
				thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		log.info("[BasePlanStrategy] all threads finished.");
		if (hadException.get()) {
			throw new RuntimeException("Some threads crashed, thus not all plans may have been handled.");
		}
	}
	@Override
	public PlanSelector getPlanSelector() {
		return new KeepSelected();
	}

}
