/**
 * 
 */
package playground.jbischoff.BAsignals;

import org.apache.log4j.Logger;
import org.matsim.signalsystems.initialization.SignalsControllerListener;
import org.matsim.signalsystems.initialization.SignalsControllerListenerFactory;

/**
 * @author jbischoff
 *
 */
public class JBSignalControllerListenerFactory implements
		SignalsControllerListenerFactory {
	
	private static final Logger log = Logger.getLogger(JBSignalControllerListenerFactory.class);
	private SignalsControllerListenerFactory delegate;
	
	public JBSignalControllerListenerFactory(SignalsControllerListenerFactory signalsControllerListenerFactory){
		this.delegate = signalsControllerListenerFactory;
	}
	
	@Override
	public SignalsControllerListener createSignalsControllerListener() {
		log.info("Using JB SignalControllerListener...");
		return new JBSignalControllerListener(this.delegate.createSignalsControllerListener());
	}

}
