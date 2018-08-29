package chmdes;

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
/**
* Generate a stream of totes for 8.0 time units.
*/
class Generator extends Event {
    Infeed infeed;
    /**
    * Create a new Tote.  Add the tote to the infeed  and
    * schedule the creation of the next tote
    */
    void execute(Simulator simulator) {
        Tote tote = new Tote();
        System.out.println("New Tote Generated " + tote + " at time " + time);
        infeed.insert(simulator, tote);
        time += Random.exponential(4.0);
        if (time < 500.0) simulator.insert(this);
    }
}

class Infeed {
    /**
    * Use the Java Vector to implement a FIFO infeed.
    */
    private java.util.Vector totes = new java.util.Vector();
    Palletizer palletizer;
    double infeedLength;
    double infeedSpeed;
    int totesMax;
    
    public Infeed(double infeedLength, double infeedSpeed, int totesMax) {
    	this.infeedLength = infeedLength;
    	this.infeedSpeed = infeedSpeed;
    	this.totesMax = totesMax;
    }    
    
    /**
    * Add a tote to the infeed.
    * If the palletizer is available (which also implies this infeed is empty),
    * pass the tote on to the palletizer.  
    * Otherwise add the tote to the infeed.
    */
    void insert(Simulator simulator, Tote tote) {
    	if (totes.size() >= this.totesMax) {
    		System.out.println("ERROR: Infeed Full " + tote);
    	} else {
    		// compute the minimal time the tote will take to traverse the infeed
    		double minTime;
    		minTime = this.infeedLength/this.infeedSpeed;
    		tote.setMinTime(minTime);
    		
    		if (palletizer.isAvailable()) {
	            palletizer.insert(simulator,tote);
	        } else {
	            totes.addElement(tote);
	        }
	        System.out.println("Added tote, infeed size: " + totes.size());
    	}
    }
    /**
    * @return the first tote in the infeed
    */
    Tote remove() {
        Tote tote = (Tote) totes.firstElement();
        totes.removeElementAt(0);        
        return tote;
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
class Palletizer extends Event {
    private Tote toteBeingServed;
    Infeed infeed;
    /**
    * The customer's service is completed so print a message.
    * If the infeed is not empty, get the next customer.
    */
    void execute(Simulator simulator) {
        System.out.println("Palletized tote " + toteBeingServed + " at time " + time);
        toteBeingServed = null;
        if (infeed.size()>0) {
            Tote tote = infeed.remove();
            insert((Simulator) simulator, tote);
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
    void insert(Simulator simulator, Tote tote) {
        if (toteBeingServed != null) {
            /* Should never reach here */
            System.out.println("Error: I am busy serving someone else");
            return;
        }
        toteBeingServed = tote;
        System.out.println("Palletizer Found tote " + toteBeingServed + " at time " + time);
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
        Infeed infeed = new Infeed(10.0, 1.0, 20);
        Palletizer palletizer = new Palletizer();
        
        /* Connect them together. */
        generator.infeed = infeed;
        infeed.palletizer = palletizer;
        palletizer.infeed = infeed;
        
        /* Start the generator by creating one tote immediately */
        generator.time = 0.0;
        insert(generator);

        doAllEvents();
    }
}



