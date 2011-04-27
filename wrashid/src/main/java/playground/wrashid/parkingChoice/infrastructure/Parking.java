package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.GeneralLib;

public class Parking implements Comparable<Parking> {
	public void setParkingId(Id parkingId) {
		this.parkingId = parkingId;
	}

	public void setMaxCapacity(Integer maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

	public void setCurrentOccupancy(int currentOccupancy) {
		this.currentOccupancy = currentOccupancy;
	}

	public void setPrice(Double price) {
		Price = price;
	}

	public void setAccessTime(Double accessTime) {
		this.accessTime = accessTime;
	}

	public void setSearchTime(Double searchTime) {
		this.searchTime = searchTime;
	}

	public void setCoord(Coord coord) {
		this.coord = coord;
	}

	Id parkingId = null;
	Integer maxCapacity = null;
	int currentOccupancy = 0;
	Double Price = null;
	Double accessTime = null;
	Double searchTime = null;
	Coord coord = null;
	double score = 0;

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public Id getParkingId() {
		return parkingId;
	}

	public Integer getMaxCapacity() {
		return maxCapacity;
	}

	public int getCurrentOccupancy() {
		return currentOccupancy;
	}

	public Double getPrice() {
		return Price;
	}

	public Double getAccessTime() {
		return accessTime;
	}

	public Double getSearchTime() {
		return searchTime;
	}

	public boolean hasFreeCapacity() {
		return maxCapacity > currentOccupancy;
	}

	public void parkVehicle() {
		currentOccupancy++;
	}

	public void removeVehicle() {
		currentOccupancy--;
	}

	public Coord getCoord() {
		return coord;
	}

	public Parking(Coord coord) {
		super();
		this.coord = coord;
	}

	public double getWalkingDistance(Coord targetCoordinate) {
		return GeneralLib.getDistance(coord, targetCoordinate);
	}

	@Override
	public int compareTo(Parking otherParking) {
		if (score > otherParking.getScore()) {
			return 1;
		} else if (getScore() < otherParking.getScore()) {
			return -1;
		} else {
			return 0;
		}
	}
}
