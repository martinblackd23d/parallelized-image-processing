package com.abc.thread;

public class NanoTimer {
    private static final double NS_PER_MILLISECOND = 1_000_000.0;
    private static final double NS_PER_SECOND = NS_PER_MILLISECOND * 1_000.0;
    private static final double NS_PER_MINUTE = NS_PER_SECOND * 60.0;
    private static final double NS_PER_HOUR = NS_PER_MINUTE * 60.0;
    private static final double NS_PER_DAY = NS_PER_HOUR * 24.0;

    private long nsStartTimeOfInterval;
    private long nsTotalOfStoppedIntervals = 0L;
    private boolean paused = true;

    // private, use createXYZ methods
    private NanoTimer() {
    }

    public static NanoTimer createStarted() {
        NanoTimer timer = new NanoTimer();
        timer.start();
        return timer;
    }

    public static NanoTimer createStopped() {
        return new NanoTimer();
    }

    public synchronized void start() {
        if ( paused ) {
            paused = false;
            nsStartTimeOfInterval = System.nanoTime();
        }
    }

    public synchronized void stop() {
        if ( !paused ) {
            paused = true;
            nsTotalOfStoppedIntervals += System.nanoTime() - nsStartTimeOfInterval;
        }
    }

    public synchronized void reset() {
        stop();
        nsTotalOfStoppedIntervals = 0L;
    }

    public synchronized long getElapsedNanoseconds() {
        return nsTotalOfStoppedIntervals +
            (paused ? 0 : System.nanoTime() - nsStartTimeOfInterval);
    }

    public double getElapsedMilliseconds() {
        return getElapsedNanoseconds() / NS_PER_MILLISECOND;
    }

    public double getElapsedSeconds() {
        return getElapsedNanoseconds() / NS_PER_SECOND;
    }

    public double getElapsedMinutes() {
        return getElapsedNanoseconds() / NS_PER_MINUTE;
    }

    public double getElapsedHours() {
        return getElapsedNanoseconds() / NS_PER_HOUR;
    }

    public double getElapsedDays() {
        return getElapsedNanoseconds() / NS_PER_DAY;
    }
}
