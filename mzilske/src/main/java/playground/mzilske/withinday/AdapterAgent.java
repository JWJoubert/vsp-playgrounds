package playground.mzilske.withinday;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.pt.qsim.MobsimDriverPassengerAgent;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;

public class AdapterAgent implements MobsimDriverPassengerAgent, SimulationBeforeSimStepListener {
	
	Id id;
	
	RealAgent realAgent;

	private String mode ;

	protected TeleportationBehavior teleportationBehavior;
	
	private DrivingBehavior drivingBehavior;
	
	private Id nextLinkId;
	
	private MobsimAgent.State state ;
	@Override
	public MobsimAgent.State getState() {
		return this.state ;
	}
	
	TeleportationWorld teleportationWorld = new TeleportationWorld() {

		@Override
		public double getTime() {
			return now;
		}

		@Override
		public void stop() {
			System.out.println("Agent wants to stop, but stopping teleportation isn't supported.");
			// yy stopping is supported nearly nowhere, since it does not make sense.  An airplane can also not
			// just stop.  Shouldn't we get rid of this method?  kai, jun'11
		}
		
	};
	
	DrivingWorld drivingWorld = new DrivingWorld() {

	    @Override
	    public double getTime()
	    {
	        return now;
	    }
	    
	    
		@Override
		public void park() {
			
		}

		@Override
		public void nextTurn(Id nextLinkId) {
			// storing nextLinkId internally:
			AdapterAgent.this.nextLinkId = nextLinkId;
		}

		@Override
		public boolean requiresAction() {
			if (nextLinkId == null) {
				return true;
			} else {
				return false;
			}
		}
		
	};
	
	World world = new World() {

		@Override
		public ActivityPlane getActivityPlane() {
			return new ActivityPlane() {

				@Override
				public void startDoing(ActivityBehavior activityBehavior) {
					AdapterAgent.this.activityBehavior = activityBehavior;
					eventsManager.processEvent(eventsManager.getFactory().createActivityStartEvent(now, id, currentLinkId, null, activityBehavior.getActivityType()));
					
//					simulation.arrangeActivityStart(AdapterAgent.this);
					AdapterAgent.this.state = MobsimAgent.State.ACTIVITY ;
					simulation.insertAgentIntoMobsim(AdapterAgent.this) ;
					// yyyyyy 000000
					
				}
				
			};
		}

		@Override
		public TeleportationPlane getTeleportationPlane() {
			return new TeleportationPlane() {

				@Override
				public void startTeleporting(TeleportationBehavior teleportTo) {
					Id destination = teleportTo.getDestinationLinkId();
					if (destination == null) {
						throw new RuntimeException();
					}
					AdapterAgent.this.mode = teleportTo.getMode() ;

//					simulation.arrangeAgentDeparture(AdapterAgent.this);
					AdapterAgent.this.state = MobsimAgent.State.LEG ;
					simulation.insertAgentIntoMobsim(AdapterAgent.this) ;
					// yyyyyy 000000


					AdapterAgent.this.teleportationBehavior = teleportTo;
				}
				
			};
		}

		@Override
		public double getTime() {
			return now;
		}

		@Override
		public void done() {
			System.out.println("I'm done. I am at: " + currentLinkId);
			simulation.getAgentCounter().decLiving();
			AdapterAgent.this.living = false;
		}

		@Override
		public Id getLocation() {
			return currentLinkId;
		}

		@Override
		public RoadNetworkPlane getRoadNetworkPlane() {
			return new RoadNetworkPlane() {

				@Override
				public void startDriving(DrivingBehavior drivingBehavior) {
					AdapterAgent.this.drivingBehavior = drivingBehavior;
					AdapterAgent.this.mode = TransportMode.car ;
					drivingBehavior.doSimStep(drivingWorld);

//					simulation.arrangeAgentDeparture(AdapterAgent.this);
					AdapterAgent.this.state = MobsimAgent.State.LEG ;
					simulation.insertAgentIntoMobsim(AdapterAgent.this) ;
					// yyyyyy 000000
					
				}
				
			};
		}
		
	};

	private double now;

	private MobsimVehicle veh;
	
	private boolean firstTimeGetCurrentLinkId = true;

	private Id currentLinkId;

	private Mobsim simulation;

	private EventsManager eventsManager;

	private ActivityBehavior activityBehavior;

	protected double activityEndTime;

	private ActivityWorld activityWorld = new ActivityWorld() {

		@Override
		public double getTime() {
			return now;
		}

		@Override
		public void stopActivity() {
			System.out.println("I want to stop my activity.");
			eventsManager.processEvent(eventsManager.getFactory().createActivityEndEvent(now, id, currentLinkId, null, activityBehavior.getActivityType()));
			AdapterAgent.this.activityEndTime = now ;
			simulation.rescheduleActivityEnd(AdapterAgent.this, Double.POSITIVE_INFINITY, now);

			simulation.getAgentCounter().decLiving(); 
			// This is necessary because the QSim thinks it must increase the living agents counter in the rescheduling step.  mz
			// The intention there was that an agent that is no longer alive has an activity end time of infinity (rather
			// than removing it completely from teh mobsim).  The number of
			// alive agents is only modified when an activity end time is changed between a finite time and infinite.  kai, jun'11

			activityBehavior = null;
		}
		
	};

	public AdapterAgent(Plan selectedPlan, Mobsim simulation) {
	    this(selectedPlan, simulation, new MzPlanAgentImpl(selectedPlan));
    }
	
	public AdapterAgent(Plan selectedPlan, Mobsim simulation, RealAgent realAgent) {
		id = selectedPlan.getPerson().getId();

		this.realAgent = realAgent;
		
		currentLinkId = ((Activity) selectedPlan.getPlanElements().get(0)).getLinkId();
		this.simulation = simulation;
		this.eventsManager = simulation.getEventsManager();
		
		this.simulation.getAgentCounter().incLiving();
	}
	
	
	private boolean living = true;

	@Override
	public void notifySimulationBeforeSimStep(@SuppressWarnings("rawtypes") SimulationBeforeSimStepEvent e) {
	    if (!living) {
	        return;//or maybe should be unregistered from listeners in World.done() method
	    }
	    
		now = e.getSimulationTime();
		if (teleportationBehavior != null) {
			teleportationBehavior.doSimStep(teleportationWorld);
		} else if (activityBehavior != null) {
			activityBehavior.doSimStep(activityWorld);
		} else if (drivingBehavior != null) {
			drivingBehavior.doSimStep(drivingWorld);
		}else {
			realAgent.doSimStep(world);
			// I guess you get here in the time step after endLeg/ActAndAssumeControl, since this particular agent does not 
			// do anything in those methods.  So the agent is in limbo for the time being.  Is "being in limbo" consistent with
			// the original design?  kai, jun'11
		}
	}

	@Override
	public double getActivityEndTime() {
		// I get asked about this first thing in the morning.
		// After I have decided I am in an activity, I need to give the
		// same answer again every time I am asked (until I am told my activity
		// is over), or I will confuse the simulation. mz
		//
		// The main reason for this is that the activity end queue is time-sorted, so that not all agents need to be asked
		// if they want to depart.  If you take care of agent departure yourself, this could just return infinity.
		return this.activityEndTime ;
	}

	@Override
	public void endActivityAndAssumeControl(double now) {
		// The simulation tells me when the time has come to end my activity.
		// This may be later than the time I told it I wanted to end my activity, 
		// because it may already have been later then.
		// On the other hand, "really" ending the activity is still up to me, because I have 
		// to throw the event.
		// So all the simulation really does is set an alarm clock for me and tell me if
		// I've missed it. It doesn't really care about activities otherwise.
		//
		// I should just try to ignore the Simulation's views about activities and decide:
		// Do I want to leave right now? Then I'm going to depart.
		// Or do I not? Then I will tell the Simulation I am having an activity until the next timestep.
		
	}

	@Override
	public void endLegAndAssumeControl(double now) {
		// The simulation tells me I have arrived.
		// Interestingly, I have to throw the event myself. mz
		//
		// In theory, transport-related events should be thrown by the transport part of the simulation, activity-related
		// events should be thrown by the agent.  In practice, there are many transport-related modules, such as teleportation, 
		// walk, public transit, etc., and many of them are pluggable.  It is quite possible that one of them forgets the 
		// corresponding arrival event.  So it seems easier to ensure consistency in the agent than in the framework.
		// In part also because the user of the agent arrival event is probably the agent programmer, not the framework
		// programmer.  Given that this was also the structure that I found, I decided to leave it that way.  kai, jun'11

		eventsManager.processEvent(eventsManager.getFactory().createAgentArrivalEvent(now, id, currentLinkId, this.mode));
		teleportationBehavior = null;
		drivingBehavior = null;
	}
	
	@Override
	public Double getExpectedTravelTime() {
		return null ; 	
	}
    
	@Override
	public String getMode() {
		return this.mode ;
	}
	
	@Override
	public final Id getPlannedVehicleId() {
		return null ;
	}

	@Override
	public void notifyTeleportToLink(Id linkId) {
		// I am told this when the Simulation decides to not move me to my destination over the network, but to teleport me there.
		// This is a little silly - apparently the Simulation thinks that when I'm moved over the network, I can keep track of
		// my current link myself, but when I'm "teleported", I can't do that. But I can! After all, the Simulation just asked me where I 
		// want to go. mz
		//
		// Actually no.  The teleportation arrival is much later than the teleportation departure.  Also, there is no guarantee
		// that the mobsim delivers to where it should deliver.  The corresponding notification method for network movement 
		// is "notifyMoveOverNode". kai, jun'11
		this.currentLinkId = linkId ;
	}

	@Override
	public Id getCurrentLinkId() {
		// I am asked this right at the beginning so the Simulation can park my vehicle on that link. mz
		//
		// Many pieces of the code are easier if the agent knows where she is.  In most places, this is retrieved over the
		// person, but this seems the better way (I think).  kai, jun'11
		if (firstTimeGetCurrentLinkId) {
			return currentLinkId ;
		} else {
			throw new RuntimeException();
		}
	}

	@Override
	public Id getDestinationLinkId() {
		// If I want to "arrive", I need to
		// return the link I am currently on (I need to know that),
		// and ALSO, on the next call to chooseNextLinkId, choose null.
		// I will then be told to endLegAndAssumeControl immediately afterwards.
		//
		// Also, when I am being teleported, I will be asked via this method
		// where it is I wanted to go, even though the simulation should know
		// this because it is in my Leg, which it already used to determine
		// that I need to be teleported.
		// I just have to be honest in answering that question. The simulation will
		// immediately notify me that I was teleported to the link which I answer here. mz
		//
		// The idea was/is to make the mobsim run without knowing about legs and routes.  kai, jun'11
		return this.currentLinkId ; // the agent always has the current link as destination link; it actually parks if
		// simultaneously, chooseNextLinkId returns null.
	}

	@Override
	public Id getId() {
		return id;
	}

	@Override
	public Id chooseNextLinkId() {
		return nextLinkId;
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		// The Simulation tells me what vehicle I get to use. Don't know what I should do with that information.
		// I need to remember it so I can give it back when getVehicle is called. mz
		//
		// This is just a back pointer.  I would prefer bi-directional pointers (i.e. connectors), but they
		// don't exist in java.  As an example: Person may receive a recommendation to change lanes via his/her iPhone.
		// The person would need to be able to do give this info to the steering wheel of the vehicle.
		// It is, however, not clear why the agent needs to expose that info.  kai, jun'11

		this.veh = veh;
	}

	@Override
	public MobsimVehicle getVehicle() {
		// The simulation asks me what vehicle I am using. This is silly. I am an agent! Nobody should use me as a data container.
		// mz
		//
		// agreed.  it is used only in two locations; maybe we can get rid of it.  
		// (I fact, both places where it is needed have to do with non-physical behavior.)  kai, jun'11
		return this.veh;
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		// I think I am told this some time after I was asked about my next link. I think this means that I have entered it now. mz
		//
		// yes, exactly.  It may have happened that it was not possible to cross the intersection, and the mobsim may have made you
		// "stuckAndAbort", or a police person may have waved you into a link into which you did not want to go.  
		// (It is, however, quite strange that this does not pass on the new link as an argument.)
		// kai, jun'11
		this.nextLinkId = null; // this is set to null because it means the "next" linkId, not the "current" linkId.  kai, jun'11
		this.currentLinkId = newLinkId ;
	}

	@Override
	public boolean getEnterTransitRoute(TransitLine line,
			TransitRoute transitRoute, List<TransitRouteStop> stopsToCome) {
		// Do I want to enter that bus?
		return false;
	}

	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		// Do I want to get off here?
		return false;
	}

	@Override
	public double getWeight() {
		// whatever. mz
		//
		// yyyy well, would be better to set this to "1" since this is, in the end, the space that the agent uses, say in the
		// bus.  But it is one of these functions that someone added without thinking it through, and nobody has used so far. 
		// kai, jun'11
		return 0;
	}

}
