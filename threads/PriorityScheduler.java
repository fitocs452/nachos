package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param   transferPriority    <tt>true</tt> if this queue should
     *                  transfer priority from waiting threads
     *                  to the owning thread.
     * @return  a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
    return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
    Lib.assertTrue(Machine.interrupt().disabled());
               
    return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
    Lib.assertTrue(Machine.interrupt().disabled());
               
    return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());
                   
        Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
    boolean intStatus = Machine.interrupt().disable();
               
    KThread thread = KThread.currentThread();

    int priority = getPriority(thread);
    if (priority == priorityMaximum)
        return false;

    setPriority(thread, priority+1);

    Machine.interrupt().restore(intStatus);
    return true;
    }

    public boolean decreasePriority() {
    boolean intStatus = Machine.interrupt().disable();
               
    KThread thread = KThread.currentThread();

    int priority = getPriority(thread);
    if (priority == priorityMinimum)
        return false;

    setPriority(thread, priority-1);

    Machine.interrupt().restore(intStatus);
    return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param   thread  the thread whose scheduling state to return.
     * @return  the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
    if (thread.schedulingState == null)
        thread.schedulingState = new ThreadState(thread);

    return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;
        ThreadState lockHolder;
        ArrayList<ThreadState> priorityWaitQueue = new ArrayList<ThreadState>();

        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            if (!transferPriority) {
                getThreadState(thread).waitForAccess(this);
                return;
            }

            // Calculamos la prioridad del thread actual con la del Lockholder
            int priorityDiff = lockHolder.getPriority() - getThreadState(thread).getPriority();
            // En el caso en el que la del lockHolder sea menor a la del thread actual
            // entonces cambiamos la prioridad del lockholder
            if(transferPriority && lockHolder != null && priorityDiff < 0) {
                lockHolder.setEffectivePriority(getThreadState(thread).getPriority());
            }

            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());

            ThreadState nextThread = pickNextThread();
            if(nextThread == null) {
                return null;
            }

            priorityWaitQueue.remove(nextThread);
            return nextThread.thread;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return  the next thread that <tt>nextThread()</tt> would
         *      return.
         */
        protected ThreadState pickNextThread() {
            if(priorityWaitQueue.isEmpty()) {
                return null;
            }

            // La prioridad actual deberia ser la mayor
            int actualPriority = -1;
            // ThreadState con la mayor prioridad
            ThreadState threadState = null;


            for(ThreadState ts_actual: priorityWaitQueue) {
                if(ts_actual.getEffectivePriority() > actualPriority) {
                    threadState = ts_actual;
                    actualPriority = ts_actual.getEffectivePriority();
                }
            }

            threadState.setEffectivePriority(0);
            return threadState;
        }

        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param   thread  the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;
            setPriority(priorityDefault);
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return  the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return  the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            if(effectivePriority == 0) {
                return priority;
            }

            return effectivePriority;
        }

        public void setEffectivePriority(int priority){
            effectivePriority = priority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param   priority    the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
            return;
            
            this.priority = priority;
            
            // implement me
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param   waitQueue   the queue that the associated thread is
         *              now waiting on.
         *
         * @see nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityQueue waitQueue) {
            // implement me
            waitQueue.priorityWaitQueue.add(this);
        }

        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see nachos.threads.ThreadQueue#acquire
         * @see nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
            waitQueue.lockHolder = this; // Nuevo lockholder
        }

        /** The thread with which this object is associated. */    
        protected KThread thread;
        /** The priority of the associated thread. */
        protected int priority;
        protected int effectivePriority = 0;
    }
}
