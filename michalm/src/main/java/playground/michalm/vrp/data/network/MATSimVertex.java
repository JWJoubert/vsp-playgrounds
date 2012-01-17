package playground.michalm.vrp.data.network;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;

import pl.poznan.put.vrp.dynamic.data.network.*;


public interface MATSimVertex
    extends Vertex
{
    //usually getCoord() == getLink().getCoord(); however not always!
    Coord getCoord();


    Link getLink();
}
