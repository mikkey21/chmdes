package chmdes;

interface Comparable {
    boolean lessThan(Comparable y);
}

abstract class  AbstractEvent implements Comparable {
    abstract void execute(Simulator simulator);
}

abstract class OrderedSet {
    abstract void insert(Comparable x);
    abstract Comparable  removeFirst();
    abstract int size();
    abstract Comparable remove(Comparable x);
}
    
abstract class Event extends AbstractEvent {
    double time;
    public boolean lessThan(Comparable y) {
        Event e = (Event) y;  // Will throw an exception if y is not an Event
        return this.time < e.time;
    }
}

class Simulator {
    double time;
    double now() {
        return time;
    }
	
	OrderedSet events;
    void insert(AbstractEvent e) {
        events.insert(e);
    }
    AbstractEvent cancel(AbstractEvent e)  {
        throw new java.lang.RuntimeException("Method not implemented");
    }
    
    void doAllEvents() {
        Event e;
        while ( (e= (Event) events.removeFirst()) != null) {
            time = e.time;
            e.execute(this);
        }
    }
}

class ListQueue extends OrderedSet {
	java.util.Vector elements = new java.util.Vector();
	void insert(Comparable x) {
		int i = 0;
		while (i < elements.size() && ((Comparable) elements.elementAt(i)).lessThan(x)) {
			i++;
		}
		elements.insertElementAt(x,i);
	}
	Comparable removeFirst() {
		if (elements.size() ==0) return null;
		Comparable x = (Comparable) elements.firstElement();
		elements.removeElementAt(0);
		return x;
	}
	Comparable remove(Comparable x) {
		for (int i = 0; i < elements.size(); i++) {
			if (elements.elementAt(i).equals(x)) {
				Object y = elements.elementAt(i);
				elements.removeElementAt(i);
				return (Comparable) y;
			}
		}
		return null;
	}
	public int size() {
		return elements.size();
	}	
}

class Random {
    static double exponential(double mean) {
        return - mean * Math.log(Math.random());
    }
  static boolean bernoulli(double p) {
    return Math.random() < p;
  }
    /* .. and other distributions */
}
