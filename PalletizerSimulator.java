package chmdes;

/*
 * git status # View the state of the repo
 * git add <some-file> # Stage a file
 * git commit # Commit a file</some-file>
 * git push origin master
 */


/**
* This class is empty because customers are just passive tokens that
* are passed around the system.
*/
class Tote {
	double minTime; // earliest time that this tote can be processed by next event
	public void setMinTime(double minTime) {
		this.minTime = minTime;
	}
}

abstract class MHE extends Event {
	protected String name;
	public String getName() {
		return this.name;
	}
	public void printStats() {
	}
}

/**
* Generate a stream of totes for 8.0 time units.
*/
class Generator extends MHE {
    Infeed infeed;
    int totesGenerated;
    
    public void printStats() {
    	System.out.println(this.name+": totesGenerated: "+totesGenerated);
    }
    
    public Generator() {
    	this.name = "Generator";
    	this.totesGenerated = 0;
    }    
    
    /**
    * Create a new Tote.  Add the tote to the infeed  and
    * schedule the creation of the next tote
    */
    void execute(Simulator simulator) {
        Tote tote = new Tote();
        //System.out.println("New Tote Generated " + tote + " at time " + time);
        simulator.logEvent(this, "New Tote Generated " + tote);
        this.totesGenerated++;
        infeed.insert(simulator, tote);
        time += Random.exponential(4.0);
        if (time < 200.0) simulator.insert(this);
    }
}


class Infeed extends MHE {
    /**
    * Use the Java Vector to implement a FIFO infeed.
    */
    private java.util.Vector totes = new java.util.Vector();
    Palletizer palletizer;
    double infeedLength;
    double infeedSpeed;
    int totesMax;
    
    public void printStats() {
    }
    
    
    public Infeed(double infeedLength, double infeedSpeed, int totesMax) {
    	this.infeedLength = infeedLength;
    	this.infeedSpeed = infeedSpeed;
    	this.totesMax = totesMax;
    	this.name = "Infeed";
    }    
    
    /**
    * Add a tote to the infeed.
    * If the palletizer is available (which also implies this infeed is empty),
    * pass the tote on to the palletizer.  
    * Otherwise add the tote to the infeed.
    */
    void insert(Simulator simulator, Tote tote) {
    	
    	if (totes.size() >= this.totesMax) {
    		//System.out.println("ERROR: Infeed Full " + tote);
            simulator.logEvent(this, "ERROR: Infeed Full " + tote);
    	} else {
    		// compute the minimal time the tote will take to traverse the infeed
    		double minTime;
    		minTime = ((Simulator)simulator).now()+this.infeedLength/this.infeedSpeed;
    		tote.setMinTime(minTime);
    		totes.addElement(tote);
        	//System.out.println("Infeed added tote, infeed size: " + totes.size());	        
        	//System.out.println("Infeed: "+totes);
            simulator.logEvent(this, "Infeed added tote, infeed size: " + totes.size());
            simulator.logEvent(this, "Infeed: "+totes);

    		
    		if (totes.size() == 1) {
    			// only tote on the conveyor, schedule event for time tote arrives to outfeed
    			// advance time of infeed conveyor
	            time = minTime;
    			simulator.insert(this); //
    			//System.out.println("Infeed: added  " + tote+"new time: "+time);
                simulator.logEvent(this, "Infeed: added  " + tote);
    		} else {
    			// else there are other totes on infeed and event already scheduled   
    			//System.out.println("Infeed: added  " + tote);
                simulator.logEvent(this, "Infeed: added  " + tote);
    		}

    	}
    }
    /* Infeed execute event, expect there to be a tote at the head of the infeed queue, remove
     * the tote and send it to the palletizer.   If the palletizer isn't ready, the palletizer
     * needs to check the infeed for available totes which it becomes available.
     */
    void execute(Simulator simulator) {

    	Tote tote = (Tote) totes.firstElement();
        if (palletizer.isAvailable()) {
			palletizer.insert(simulator);
			//System.out.println("Infeed: tote available for palletizer: " + tote);
            simulator.logEvent(this, "Infeed: tote available for palletizer: " + tote);
        } else {
        	// If the palletizer is busy, wait for the palletizer event
        	//System.out.println("Infeed: Tote available on infeed, palletizer busy " + tote);
        	simulator.logEvent(this, "Tote available on infeed, palletizer busy " + tote);
        }      
    }
    
    void scheduleNextInfeedEvent(Simulator simulator) {
		// Gave a tote to palletizer, now schedule next infeed event
    	if (totes.size() > 0) {
    		Tote tote = (Tote) totes.firstElement();
    		//get the max of minTime and curr_time+single tote process time
    		// assume the infeed can process 1 tote every 2 secs
    		double serviceTime = 2.0;
    		double nextToteTime = ((Simulator)simulator).now() + serviceTime;
    		nextToteTime = Math.max(tote.minTime, nextToteTime);
            time = nextToteTime;
    		simulator.insert(this); // 
    		//System.out.println("Infeed: reschedule infeed " + tote+ "time: "+time);
    		simulator.logEvent(this, "Infeed: reschedule infeed " + tote);
    	}	else { 
    	// Don't schedule an infeed event if there are no more totes on the infeed
    		//System.out.println("Infeed: no more totes available");
    		simulator.logEvent(this, "no more totes available");
    	}
    }
        
    /**
    * @return the first tote in the infeed
    */
    Tote remove(Simulator simulator) {
        Tote tote = (Tote) totes.firstElement();
        totes.removeElementAt(0);        
		//System.out.println("Infeed: Sent tote to palletizer " + tote);
		simulator.logEvent(this, "Sent tote to palletizer " + tote);
		scheduleNextInfeedEvent(simulator);
        return tote; // send tote to the palletizer!
    }
    
    int size() {
        return totes.size();
    }
}

/**
 * 
* A TotePalletizer that moves totes from the infeed to a pallet, buffer, jackpot, or fault.
* Assume palletizer works at 480 totes per hour or one tote move every 8 seconds.
* 
* Exception cases:
*  1) can't read barcode
*  2) unknown destination
*  3) pallet's locked
*/
class Palletizer extends MHE {
    private Tote toteBeingServed;
    Infeed infeed;
    int totesPalletized;
    
    public void printStats() {
    	System.out.println(this.name+": totesPalletized: "+totesPalletized);
    }
    
    /**
    * The customer's service is completed so print a message.
    * If the infeed is not empty, get the next customer.
    */
    public Palletizer() {
    	this.name = "Palletizer";
    	this.totesPalletized = 0;
    }    
  
    // Call this function when done palletizing a tote, check if there is another tote ready
    void execute(Simulator simulator) {
        //System.out.println("Palletized tote " + toteBeingServed + " at time " + time);
		simulator.logEvent(this, "Palletized tote " + toteBeingServed);
		this.totesPalletized++;
        toteBeingServed = null;
        if (infeed.size()>0) {
            insert((Simulator) simulator);
        }        
    }
    boolean isAvailable() {
        return (toteBeingServed == null);
    }
    /**
    * Start a customer's service.  The simulator must be passed in
    * as a parameter so that this can  schedule the time
    * when this palletizer will be done with the customer.
    */
    //call this function when a new tote is ready to palletizer
    void insert(Simulator simulator) {
    	time = simulator.now();
    	if (toteBeingServed != null) {
            /* Should never reach here */
            //System.out.println("Palletizer: Error: I am busy serving someone else");
    		simulator.logEvent(this, "Error: I am busy serving someone else");
            return;
        }
        
        Tote tote = infeed.remove(simulator);
        toteBeingServed = tote;
        //System.out.println("Palletizer: Found tote " + toteBeingServed + " at time " + time);
		simulator.logEvent(this, "Found tote " + toteBeingServed);
        
        //double serviceTime = Random.exponential(1.0);
        double serviceTime = 8.0;
        time = ((Simulator)simulator).now() + serviceTime;
        simulator.insert(this);
    }
} 

class PalletizerSimulator extends Simulator {
    public static void main(String[] args) {
        new PalletizerSimulator().start();
    }
    void start() {
        events = new ListQueue();
                
        /* Create the generator, infeed, and simulator */
        Generator generator = new Generator();
        Infeed infeed = new Infeed(6.0, 1.0, 20);
        Palletizer palletizer = new Palletizer();
        
        /* Connect them together. */
        generator.infeed = infeed;
        infeed.palletizer = palletizer;
        palletizer.infeed = infeed;
        
        /* Start the generator by creating one tote immediately */
        generator.time = 0.0;
        insert(generator);

        doAllEvents();
        
        /* Print out some stats */
        generator.printStats();
        infeed.printStats();
        palletizer.printStats();
    }
}



