/* *********************************************************************** *
 * project: org.matsim.*
 * UtilityChangesBVWP2003.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.kai.bvwp;

import playground.kai.bvwp.Values.Entry;



/**
 * @author Ihab
 *
 */
 class UtilityChangesBVWP2015 extends UtilityChanges {
	
		
		@Override
		UtlChangesData utlChangePerEntry(Entry entry,
				double deltaAmount, double quantityNullfall, double quantityPlanfall, double margUtl) {

		UtlChangesData utlChanges = new UtlChangesData() ;
		
		if ( entry.equals(Entry.priceUser)) {
			utlChanges.utl = 0. ;
		} else {
			if ( deltaAmount > 0 ) {
				// wir sind aufnehmend; es zaehlt der Planfall:
				utlChanges.utl = quantityPlanfall * margUtl ;
			} else {
				utlChanges.utl = -quantityNullfall * margUtl ;
			}
		}

		return utlChanges;
	}

	@Override
	double computeImplicitUtility(ValuesForAUserType econValues,
			ValuesForAUserType quantitiesNullfall,
			ValuesForAUserType quantitiesPlanfall) {
		double sum = 0. ;
		for ( Entry entry : Entry.values() ) {
			if ( entry != Entry.XX && entry != Entry.priceProduction ) {
				final double quantityPlanfall = quantitiesPlanfall.getByEntry(entry);
				final double quantityNullfall = quantitiesNullfall.getByEntry(entry);
				final double margUtl = econValues.getByEntry(entry) ;
				
				sum += - margUtl * (quantityPlanfall+quantityNullfall)/2. ;
			}
		}
		return sum ;
	}

}
