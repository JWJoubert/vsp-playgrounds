/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.scripts.simplemikrozansusconstrainedaccessibility;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.SingleNest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Alternative;
import playground.ivt.maxess.nestedlogitaccessibility.framework.ChoiceSetIdentifier;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Nest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedChoiceSet;
import playground.ivt.maxess.nestedlogitaccessibility.framework.PrismSampler;
import playground.ivt.maxess.prepareforbiogeme.tripbased.Trip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class SimpleChoiceSetIdentifier implements ChoiceSetIdentifier<SingleNest> {
	private final ObjectAttributes personAttributes;
	private final PrismSampler prismSampler;
	private final TripRouter router;

	public  SimpleChoiceSetIdentifier(
			final TripRouter tripRouter,
			final String activityType,
			final int nSamples,
			final ActivityFacilities facilities,
			final int budget_m,
			final ObjectAttributes personAttributes ) {
		this.router = tripRouter;
		this.personAttributes = personAttributes;
		this.prismSampler = new PrismSampler( activityType, nSamples , facilities , budget_m );
	}

	@Override
	public Map<String, NestedChoiceSet<SingleNest>> identifyChoiceSet( final Person p ) {
		final Nest<SingleNest> nest =
				new Nest.Builder<SingleNest>()
					.setMu( 1 )
					.setName( SingleNest.nest )
					.addAlternatives( calcAlternatives( p ) )
					.build();

		return Collections.singletonMap(
				"default",
				new NestedChoiceSet<>( nest ) );
	}

	private Iterable<Alternative<SingleNest>> calcAlternatives( final Person p ) {
		// for constraints to be valid, we need choice set for each person to be stable
		prismSampler.resetRandomSeed( p.getId().toString().hashCode() );

		final ActivityFacility origin = prismSampler.getOrigin( p );
		final List<ActivityFacility> prism = prismSampler.calcSampledPrism( origin );

		final String mode = isCarAvailable( p ) && hasLicense( p ) ?
				"car" : "pt";

		final List<Alternative<SingleNest>> alternatives = new ArrayList<>( prism.size() );
		int i = 0;
		for ( ActivityFacility destination : prism ) {
			alternatives.add(
					new Alternative<>(
							SingleNest.nest,
							Id.create( i++ , Alternative.class ),
							new Trip(
									origin,
									router.calcRoute(
											mode,
											origin,
											destination,
											12 * 3600,
											p ),
									destination ) ) );
		}
		return alternatives;
	}

	private boolean isCarAvailable( Person person ) {
		final String avail = (String)
				personAttributes.getAttribute(
					person.getId().toString(),
					"availability: car" );
		return avail.equals( "always" );
	}

	private boolean hasLicense( Person p ) {
		final String avail = (String)
				personAttributes.getAttribute(
						p.getId().toString(),
						"driving licence" );
		return avail.equals( "yes" );
	}
}
