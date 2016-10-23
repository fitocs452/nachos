package nachos.threads;

import nachos.machine.*;
import java.util.ArrayList;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    private ArrayList<SleptThread> sleptThreads = new ArrayList();
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
        long actual_time =Machine.timer().getTime();

        ArrayList awake_threads_index = new ArrayList();
        for(int i = 0; i < sleptThreads.size(); i++) {
            if(sleptThreads.get(i).getWakeTime() <= actual_time) {
                awake_threads_index.add(i);
                KThread t = sleptThreads.get(i).getThread();
                t.ready();
            }
        }

        for(int j = 0; j < awake_threads_index.size(); j++) {
            sleptThreads.remove((int)awake_threads_index.get(j));
        }

        KThread.currentThread().yield();
    }
    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */

    public void waitUntil(long x) {
        Machine.interrupt().disable();
        long wakeTime = Machine.timer().getTime() + x;
        SleptThread st = new SleptThread(KThread.currentThread(), wakeTime);
        sleptThreads.add(st);
        KThread.sleep();
        Machine.interrupt().enable();
    }
}
