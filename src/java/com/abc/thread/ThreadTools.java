package com.abc.thread;

public class ThreadTools {
    private static long nsStartTime = System.nanoTime();

    // no instances
    private ThreadTools() {
    }

    public static void outln(String fmt, Object... args) {
        String msg = String.format(fmt, args);
        synchronized (ThreadTools.class) {
            double secondsElapsed = (System.nanoTime() - nsStartTime) / 1e9;
            System.out.printf("%10.5f|%-12.12s|%s%n", secondsElapsed, Thread.currentThread().getName(), msg);
        }
    }

    public static void busyStall(long msToSpin) {
        long msEndTime = System.currentTimeMillis() + msToSpin;
        while (System.currentTimeMillis() < msEndTime); // spin
    }

    public static void interruptableBusyStall(long msToSpin) throws InterruptedException {
        long msEndTime = System.currentTimeMillis() + msToSpin;
        while (System.currentTimeMillis() < msEndTime) {
            if (Thread.interrupted()) throw new InterruptedException();
        }
    }
}
