package nachos.threads;

import nachos.machine.*;
import java.util.ArrayList;

public class SleptThread {
    private KThread thread;
    private long wakeTime;

    public SleptThread(KThread kt, long t) {
        this.thread = kt;
        this.wakeTime = t;
    }

    public KThread getThread() {
        return this.thread;
    }

    public long getWakeTime() {
        return this.wakeTime;
    }
}
