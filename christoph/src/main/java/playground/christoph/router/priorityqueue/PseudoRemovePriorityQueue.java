/* *********************************************************************** *
 * project: org.matsim.*
 * FastRemovePriorityQueue.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.christoph.router.priorityqueue;

import java.io.Serializable;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A simple re-implementation of a priority queue that offers a much better
 * performance for {@link #remove(Object) remove} operations than the standard
 * {@link PriorityQueue}, but uses more memory to manage the data.<br />
 *
 * The actual implementation is backed by a standard {@link PriorityQueue}. Each
 * added object is encapsulated in a simple structure together with a valid-flag.
 * If an element should be removed, the valid-flag is invalidated but the element
 * remains in the collection, foregoing the expensive linear remove operation
 * from the standard <tt>PriorityQueue</tt> implementation. Polling for elements
 * makes sure only valid elements are returned, all others are just ignored.
 *
 * @param <E> the type of elements held in this collection
 *
 * @see PriorityQueue
 *
 * @author mrieser
 */
public class PseudoRemovePriorityQueue<E> implements RouterPriorityQueue<E> {

	private final PriorityQueue<PseudoEntry<E>> delegate;
	/*package*/ final Map<E, PseudoEntry<E>> lastEntry;

	public PseudoRemovePriorityQueue(final int initialCapacity) {
		this.delegate = new PriorityQueue<PseudoEntry<E>>(initialCapacity, new PseudoComparator<E>());
		this.lastEntry = new IdentityHashMap<E, PseudoEntry<E>>(initialCapacity);
	}

	/**
	 * Adds the specified element to this priority queue, with the given priority.
	 * If the element is already present in the queue, it is not added a second
	 * time.
	 * @param o
	 * @param priority
	 * @return <tt>true</tt> if the element was added to the collection.
	 */
	@Override
	public boolean add(final E o, final double priority) {
		if (o == null) {
      throw new NullPointerException();
		}
		if (this.lastEntry.containsKey(o)) {
			return false;
		}
		PseudoEntry<E> entry = new PseudoEntry<E>(o, priority);
		if (this.delegate.add(entry)) {
			this.lastEntry.put(o, entry);
			return true;
		}
		return false; // this should never happen
	}

	/**
	 * Retrieves and removes the head of this queue, or <tt>null</tt>
	 * if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this
	 *         queue is empty.
	 */
	@Override
	public E remove() {
		PseudoEntry<E> entry = this.delegate.poll();
		while ((entry != null) && (!entry.valid)) {
			entry = this.delegate.poll();
		}
		if (entry == null) {
			return null;
		}
		this.lastEntry.remove(entry.value);
		return entry.value;
	}

	/**
	 * Retrieves, but does not remove, the head of this queue, returning 
	 * <tt>null</tt> if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this
	 *         queue is empty.
	 */
	@Override
	public E peek() {
		PseudoEntry<E> entry = this.delegate.peek();
		while ((entry != null) && (!entry.valid)) {
			this.delegate.poll();	// remove invalid entry
			entry = this.delegate.peek();
		}
		if (entry == null) {
			return null;
		}
		return entry.value;
	}
	
	/**
	 * Removes a single instance of the specified element from this
	 * queue, if it is present.
	 *
	 * @return <tt>true</tt> if the queue contained the specified
	 *         element.
	 */
	@Override
	public boolean remove(final E o) {
		PseudoEntry<E> entry = this.lastEntry.remove(o);
		if ((entry != null) && (entry.valid)) {
			entry.valid = false;
			return true;
		}
		return false;
	}

	/**
	 * Checks whether the queue is empty.
	 *
	 * @return <tt>true</tt> if the queue is empty.
	 */
	public boolean isEmpty() {
		return this.lastEntry.size() == 0;
	}
	
	/**
	 * Returns the number of elements in this priority queue.  If the collection
	 * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
	 *
	 * @return the number of elements in this collection.
	 */
	@Override
	public int size() {
		return this.lastEntry.size();
	}

	/**
	 * Returns an iterator over the elements in this queue. The iterator
	 * does not return the elements in any particular order. Removing
	 * elements is not supported via the iterator.
	 *
	 * @return an iterator over the elements in this queue.
	 */
	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {

			final Iterator<E> iterDelegate = PseudoRemovePriorityQueue.this.lastEntry.keySet().iterator();

			@Override
			public boolean hasNext() {
				return this.iterDelegate.hasNext();
			}

			@Override
			public E next() {
				return this.iterDelegate.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Increases the priority (=decrease the given double value) of the element.
	 * If the element ins not part of the queue, it is added. If the new priority
	 * is lower than the existing one, the method returns <tt>false</tt>
	 *
	 * @return <tt>true</tt> if the elements priority was decreased.
	 */
	@Override
	public boolean decreaseKey(E o, double priority) {
		PseudoEntry<E> entry = this.lastEntry.remove(o);
		if (entry != null && entry.valid) {
			if (priority > entry.priority) {
				return false;
			} else {			
				entry.valid = false;				
			}
		} 
		return this.add(o, priority);
	}
	
	/**
	 * Resets the queue to its initial state.
	 */
	@Override
	public void reset() {
		// nothing to do here.
	}
	
	private static class PseudoEntry<E> {
		final E value;
		final double priority;
		boolean valid = true;

		public PseudoEntry(final E value, final double priority) {
			this.value = value;
			this.priority = priority;
		}
	}

	/*package*/ static class PseudoComparator<T> implements Comparator<PseudoEntry<T>>, Serializable {
		private static final long serialVersionUID = 1L;
		@Override
		public int compare(final PseudoEntry<T> o1, final PseudoEntry<T> o2) {
			return Double.compare(o1.priority, o2.priority);
		}
	}
}