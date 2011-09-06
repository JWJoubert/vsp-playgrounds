/*******************************************************************************
 * Copyright (C) 2011 Stefan Schr�der.
 * eMail: stefan.schroeder@kit.edu
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package vrp.basics;

import java.util.Collection;
import java.util.HashMap;

import java.util.Map;

import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.VRP;

public class VRPWithMultipleDepotsImpl implements VRP{

	private Costs costs;
	
	private Constraints constraints;
	
	private Map<String,Customer> customers;
	
	private Map<String,Customer> depots;
	
	public VRPWithMultipleDepotsImpl(Collection<String> depots, Collection<Customer> customers, Costs costs, Constraints constraints) {
		super();
		this.costs = costs;
		this.constraints = constraints;
		mapCustomers(customers);
		mapDepots(depots);
	}

	private void mapDepots(Collection<String> depots) {
		this.depots = new HashMap<String,Customer>();
		for(String id : depots){
			if(customers.containsKey(id)){
				this.depots.put(id, customers.get(id));
			}
			else{
				throw new IllegalStateException("depot not in customerList which contains all customers inclusive depots");
			}
		}
	}
	
	private void mapCustomers(Collection<Customer> customers) {
		this.customers = new HashMap<String, Customer>();
		for(Customer customer : customers){
			this.customers.put(customer.getId(), customer);
		}
	}

	@Override
	public Constraints getConstraints() {
		return constraints;
	}

	@Override
	public Costs getCosts() {
		return costs;
	}

	@Override
	public Customer getDepot() {
		return null;
	}

	@Override
	public Map<String, Customer> getCustomers() {
		return customers;
	}

	@Override
	public Map<String, Customer> getDepots() {
		return depots;
	}

	@Override
	public VehicleType getVehicleType(String depotId) {
		// TODO Auto-generated method stub
		return null;
	}



}
