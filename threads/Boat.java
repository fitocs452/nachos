package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    public static Lock lock = new Lock();
    // Ubicacion inicial del bote
    public static boolean boatOnOahu = true;

    // Variables de condicion
    public static Condition oahuCondition = new Condition(lock);
    public static Condition molokaiCondition = new Condition(lock);

    public static int boatSeatsAvailable = 2;

    public static Isla Oahu = new Isla(0,0, oahuCondition);
    public static Isla Molokai = new Isla(0,0, molokaiCondition);

    public static class Isla {
        
        private int child;
        private int adult;
        private Condition isla;
        private int people; // numero de personas en la isla

        public Isla(int c, int a, Condition cond) {
            this.child = c;
            this.adult = a;
            this.isla = cond;
        }

        //sets y gets
        public int  getChild(){
            return child;
        }
        public int getAdult(){
            return adult;
        }
        public Condition getIsla(){
            return isla;
        }
        public int getPeople(){
            return people;
        }
        public void setChild(int child){
            this.child = child;
        }
        public void setAdult(int adult){
            this.adult = adult;
        }
        public void setIsla(Condition isla){
            this.isla = isla;
        }
        public void setPeople(int people){
            this.people = people;
        }


        // Cambiar estado de la isla
        public void incrementChild() {
            child = child + 1;
        }
        public void decrementChild() {
            child = child - 1;
        }
        public void incrementAdult() {
            adult = adult + 1;
        }
        public void decrementAdult() {
            adult = adult - 1;
        }

        // Estado de la isla
        public int getPopulation(){
            return child + adult;
        }

    }

    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();

	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
    	// Store the externally generated autograder in a class
    	// variable to be accessible by children.
    	bg = b;

    	// Instantiate global variables here
    	Oahu.setAdult(adults);
        Oahu.setChild(children);

        // Create threads here. See section 3.4 of the Nachos for Java
    	// Walkthrough linked from the projects page.

        Runnable adult = new Runnable() {
            public void run() {
                AdultItinerary();
            }
        };

        Runnable child = new Runnable() {
            public void run() {
                ChildItinerary();
            }
        };
        
        for (int i = 0; i < adults; i++) {
            KThread t = new KThread(adult);
            t.setName("adult - " + i);
            t.fork();
        }
        for (int i = 0; i < children; i++) {
            KThread t = new KThread(child);
            t.setName("child - " + i);
            t.fork();
        }
    }

    static void AdultItinerary()
    {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
        boolean isOahu = true;
        lock.acquire();
        while(true) {
            if (!isOahu) {
                Molokai.getIsla().sleep();
                return;
            }

            if(boatOnOahu) {
                if(!(boatSeatsAvailable > 1)) {
                    Oahu.getIsla().sleep();
                } else if (Oahu.getChild() >=2 ) {
                    Oahu.getIsla().sleep();
                } else {
                    // Cambiamos de isla
                    isOahu = false;
                    boatSeatsAvailable = 0;
                    Oahu.decrementAdult();

                    // Salimos de la isla actual
                    bg.AdultRowToMolokai();

                    // Cambiamos el estado de la isla
                    Molokai.incrementAdult();
                    Molokai.setPeople(Oahu.getPopulation());
                    boatSeatsAvailable = 2; 
                    boatOnOahu = false;
                    Molokai.getIsla().wakeAll();
                    Molokai.getIsla().sleep();
                }
            } else{
                Oahu.getIsla().sleep();
            }
        }
    }

    static void ChildItinerary()
    {
        boolean isOahu = true;
        lock.acquire();

        while(true){
            if (isOahu) {
                if(boatOnOahu) {
                    if(Oahu.getChild() < 2) {
                        Oahu.getIsla().sleep();
                    }

                    if(boatSeatsAvailable > 1) {
                        isOahu = false;
                        Oahu.decrementChild();
                        boatSeatsAvailable = 1;
                        bg.ChildRowToMolokai();
                        
                        if(Oahu.getChild() > 0) {
                            Oahu.incrementChild();
                            Oahu.getIsla().wakeAll();
                        } else {
                            boatOnOahu = false;
                            boatSeatsAvailable = 2;
                            Molokai.incrementChild();
                            Molokai.setPeople(Oahu.getPopulation());
                            Molokai.getIsla().wakeAll();
                        }

                        Molokai.getIsla().sleep();
                    } else if(boatSeatsAvailable == 1) {
                        boatSeatsAvailable = 0;
                        Oahu.decrementChild();
                        Oahu.decrementChild();
                        bg.ChildRideToMolokai();
                        Molokai.incrementChild();
                        Molokai.incrementChild();
                        Molokai.setPeople(Oahu.getPopulation());
                        boatSeatsAvailable = 2;
                        isOahu = false;
                        boatOnOahu = false;
                        Molokai.getIsla().wakeAll();
                        Molokai.getIsla().sleep();
                    } else {
                        Oahu.getIsla().sleep();
                    }
                } else {
                    Oahu.getIsla().sleep(); 
                }
            } else {
                if(Molokai.getPeople() == 0) {
                    Molokai.getIsla().sleep();
                } else {
                    if(!boatOnOahu) {
                        isOahu = true;
                        Molokai.decrementChild();
                        boatSeatsAvailable = 1;
                        bg.ChildRowToOahu();
                        Oahu.incrementChild();
                        Oahu.setPeople(Molokai.getPopulation());
                        boatSeatsAvailable = 2;
                        boatOnOahu = true;
                        Oahu.getIsla().wakeAll();
                        Oahu.getIsla().sleep();
                    } else {
                        Molokai.getIsla().sleep();
                    }
                }
            }
        }
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
