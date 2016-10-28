package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat {
    public static Lock boatLock = new Lock();
    public static int availableSeatsBoat;

    public static Island islaOahu;
    public static Island islaMolokai;

    public static int islaActual = 1;
    public static BoatGrader bg;
    public static Communicator communicator = new Communicator();

    public static class Island {
        private int child = 0;
        private int adult = 0;
        private Condition2 lock;
        private int population = 0;

        public Island(int adult, int child, Lock lock) {
            this.child = child;
            this.adult = adult;
            this.lock = new Condition2(lock);
        }

        public void setChild(int child) {
            this.child = child;
        }

        public void setAdult(int adult) {
            this.adult = adult;
        }

        public void setPopulation(int population) {
            this.population = population;
        }

        public int getPopulation() {
            return population;
        }

        public Condition2 getLock() {
            return lock;
        }

        public int getAdult() {
            return adult;
        }

        public int getChild() {
            return child;
        }
    }

    public static void selfTest()
    {
        BoatGrader boatGrader = new BoatGrader();
        bg = boatGrader;

        // System.out.println("\n ***Testing Boats with only 2 children***");
        // begin(2, 2, boatGrader);

        // System.out.println("\n ***Testing Boats with only 2 children***");
        // begin(19, 2, boatGrader);

     System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
         begin(1, 2, boatGrader);

         // System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
         // begin(3, 3, boatGrader);
    }

    public static void begin(int adults, int children, BoatGrader b)
    {
        islaOahu = new Island(adults, children, boatLock);
        islaMolokai = new Island(0, 0, boatLock);

        for(int i = 0; i < adults; i++) {
            Runnable r = new Runnable() {
            public void run() {
                    AdultItinerary();
                }
            };

            KThread t = new KThread(r);
            t.setName("Adult - #" + i);
            t.fork();
        }

        for(int i = 0; i < children; i++) {
            Runnable r = new Runnable() {
            public void run() {
                    ChildItinerary();
                }
            };

            KThread t = new KThread(r);
            t.setName("Child - #" + i);
            t.fork();
        }

        communicator.listen();
    }

    public static void AdultItinerary() {
        int isla = 1;
        boatLock.acquire();

        while(true) {
            // Caso Base: NO estamos en la isla actual entonces nos dormimos
            if(isla != islaActual) {
                if(isla == 1){
                    islaOahu.getLock().sleep();
                }else
                    islaMolokai.getLock().sleep();

                continue;
            }

            // Cuando estamos en Oahu
            if(isla == 2) {
                islaMolokai.getLock().sleep();
                continue;
            }

            // Cuando estamos en Molokay
            if (islaOahu.getChild() >=2 ) {
                islaOahu.getLock().sleep();
                continue;
            }

           if(availableSeatsBoat == 0) {
                availableSeatsBoat = 2;
                isla = 2;
                islaOahu.setAdult(islaOahu.getAdult() - 1);

                bg.AdultRowToMolokai();

                islaMolokai.setAdult(islaMolokai.getAdult() + 1);
                islaMolokai.setPopulation(islaOahu.getChild()+ islaOahu.getAdult());
                islaMolokai.getLock().wakeAll();

                availableSeatsBoat = 0;
                islaActual = 2;
                islaMolokai.getLock().sleep();
            }
        }
    }

    static void ChildItinerary(){
        int isla = 1;
        boatLock.acquire();
        while(true) {
            // Caso base: no estamos en la isla con el bote entonces nos dormimos
            if (isla != islaActual) {
                if(islaActual == 1) {
                    islaOahu.getLock().sleep();
                } else {
                    islaMolokai.getLock().sleep();
                }

                continue;
            }

            /*** Caso General: Estamos en la isla con el bote ***/

            // Cuando estamos en Molokai
            if(isla == 2) {
                // Cuando ya no hay personas terminamos
                if(islaMolokai.getPopulation() == 0) {
                    communicator.speak(islaOahu.getPopulation());
                    return;
                }

                isla = 1;
                islaActual = 1;

                bg.ChildRowToOahu();

                islaMolokai.setChild(islaMolokai.getChild() - 1);
                islaOahu.setChild(islaOahu.getChild() + 1);

                availableSeatsBoat = 0;
                islaOahu.getLock().wakeAll();
                islaOahu.getLock().sleep();

                continue;
            }

            // Cuando estamos en Oahu

            if(islaOahu.getChild() < 2) {
                islaOahu.getLock().sleep();
                continue;
            }

            // Cuando tenemos los asientos libres usamos el bote
            if(availableSeatsBoat == 0) {
                // Nos vamos a cambiar de isla
                isla = 2;
                // Cambiamos el estado del bote
                availableSeatsBoat = 1;
                // Nos movemos a la otra isla
                bg.ChildRowToMolokai();
                islaOahu.getLock().wakeAll();
                islaMolokai.getLock().sleep();
                continue;
            }

            // Cambiamos de isla
            isla = 2;
            islaActual = 2;

            // Nos movemos a Molokai
            bg.ChildRideToMolokai();
            // MOvemos los dos ninos a Molokai
            islaOahu.setChild(islaOahu.getChild() - 2);
            // Recibimos los dos ninos desde Oahu
            islaMolokai.setChild(islaMolokai.getChild() + 2);
            islaMolokai.setPopulation(islaOahu.getChild()+islaOahu.getAdult());

            availableSeatsBoat = 0;
            islaMolokai.getLock().wakeAll();
            islaMolokai.getLock().sleep();
        }

    }

    public static void SampleItinerary()
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
