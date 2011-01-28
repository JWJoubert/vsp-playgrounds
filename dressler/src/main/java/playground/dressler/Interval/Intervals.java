/* *********************************************************************** *
 * project: org.matsim.*
 * Intervals.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.dressler.Interval;

import java.util.Iterator;

import playground.dressler.control.Debug;

public class Intervals<T extends Interval > implements IntervalsInterface<T> {
//------------------------FIELDS----------------------------------//

	/**
	 * internal binary search tree holding distinct Interval instances
	 */
	AVLTree _tree;
	
	/**
	 * reference to the last Interval
	 */
	 T _last; 
	 
	/**
	 * flags for debug mode
	 */
	private static int _debug = 0;	
	
	//-----------------------METHODS----------------------------------//
	//****************************************************************//
		
		 
	//----------------------CONSTRUCTORS------------------------------//	
		
		/**
		 * Default Constructor Constructs an object containing only 
		 * the given Interval first
		 */
		public Intervals(T first){
			T interval = first;
			_tree = new AVLTree();
			_tree.insert(interval);
			_last =  interval;
		}
		//------------------------------GETTER-----------------------//
		
		
		/**
		 * Finds the VertexInterval containing t in the collection
		 * @param t time
		 * @return Interval  containing t
		 */
		public T getIntervalAt(int t){
			if (Debug.GLOBAL && Debug.INTERVALS_CHECKS) {
				if(t<0){
					throw new IllegalArgumentException("negative time: "+ t);
				}
			}
			
			T i = (T) _tree.contains(t);
			
			if (Debug.GLOBAL && Debug.INTERVALS_CHECKS) {
				if (i==null) throw new IllegalArgumentException("there is no Interval containing "+t);
			}
			return i;
		}
		
		/**
		 * Returns the number of stored intervals
		 * @return the number of stored intervals
		 */
		public int getSize() {		
			return this._tree._size;
		}
		
		public int getMeasure() {
			return this._tree._size;
		}
		
		/**
		 * Gives the last stored Interval
		 * @return Interval with maximal lowbound
		 */
		public T getLast(){
			return _last;
		}
		

		/**
		 * checks whether last is referenced right
		 * @return true iff everything is OK
		 */
		public boolean checkLast(){
			return _last==_tree._getLast().obj;
		}
		
		/**
		 * Checks whether the given Interval is the last
		 * @param o EgeInterval which it test for 
		 * @return true if getLast.equals(o)
		 */
		public boolean isLast(Interval o){			
			return (_last.equals(o));
		}
		
		
		
		/**
		 * gives the next Interval with respect to the order contained 
		 * @param o should be contained
		 * @return next Interval iff o is not last and contained
		 */
		public T getNext(T o){
			_tree.goToNodeAt(o.getLowBound());
			T j = (T) _tree._curr.obj;
			if(j.equals(o)){
				_tree.increment();
				if(!_tree.isAtEnd()){
					T i = (T) _tree._curr.obj;
					_tree.reset();
					return i;
				}else 	throw new IllegalArgumentException("Interval was already last");
			}
			else throw new IllegalArgumentException("Interval was not contained");
		}
		
		public int getLastTime() {		
			return this.getLast().getHighBound();		
		}


		
	//------------------------SPLITTING--------------------------------//	
		
		/**
		 * Finds the Interval containing t and splits this at t 
		 * giving it the same flow as the flow as the original 
		 * it inserts the new Interval after the original
		 * @param t time point to split at
		 * @return the new Interval for further modification
	 	 */
		public T splitAt(int t){
			boolean found = false;
			T j = null;
			T i = this.getIntervalAt(t);
			if (i != null){
				if (Debug.GLOBAL && Debug.INTERVALS_CHECKS) {
					found = true;
				}
				//update last
				if(i == _last){
					j = (T) i.splitAt(t);
					_last = j;
				}else {
					j = (T) i.splitAt(t);
				}
			}
			
			if (Debug.GLOBAL && Debug.INTERVALS_CHECKS) {
				if (!found)
					throw new IllegalArgumentException("there is no Interval that can be split at "+t);
			}

			_tree.insert(j);
			return j;			
		}

		
		/**
		 * setter for debug mode
		 * @param debug debug mode true is on
		 */
		public static void debug(int debug){
			Intervals._debug = debug;
		}
		

		/**
		 * Gives a String representation of all stored Intervals
		 * @return String representation
		 */
		
		public String toString(){
			
			StringBuilder str = new StringBuilder();
			for(_tree.reset();!_tree.isAtEnd();_tree.increment()){
				T i= (T) _tree._curr.obj;
				str.append(i.toString()+" \n");
			}
			return str.toString();	
		}


		@Override
		public Iterator<T> getIterator() {
			return new BinTreeIterator<T>(_tree);			
		}


		@Override
		public Iterator<T> getIteratorAt(int t) {
			return new BinTreeIterator<T>(_tree, t);
		}
}
