package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    // Lock
    private Lock com_lock;

    // Condition Variables
    private Condition2 speakers_sleep;
    private Condition2 listeners_sleep;
    private Condition2 actual_sp;

    // Controllers 
    private boolean is_bussy;
    private int transfer;

    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        this.com_lock = new Lock();
        this.speakers_sleep = new Condition2(com_lock);
        this.listeners_sleep = new Condition2(com_lock);
        this.actual_sp = new Condition2(com_lock);
        this.is_bussy = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        com_lock.acquire();
        
        while(is_bussy) {
            speakers_sleep.sleep();
        }

        is_bussy = true;
        transfer = word;

        System.out.println("word speak " + word);
        listeners_sleep.wake();
        actual_sp.sleep();

        com_lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        com_lock.acquire();

        while(!is_bussy){
            listeners_sleep.sleep();
        }

        int t_word = transfer;
        is_bussy = false;

        System.out.println("word listen" + t_word);
        actual_sp.wake();
        speakers_sleep.wake();
        com_lock.release();

        return t_word;
    }
}
