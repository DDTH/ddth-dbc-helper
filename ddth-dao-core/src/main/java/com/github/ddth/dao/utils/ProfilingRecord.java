package com.github.ddth.dao.utils;

/**
 * Captures profiling record of a storage action.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */

public class ProfilingRecord {
    public final static ProfilingRecord[] EMPTY_ARRAY = new ProfilingRecord[0];

    public long timestamp, duration;
    public String command;

    public ProfilingRecord() {
    }

    public ProfilingRecord(long timestamp, String command, long duration) {
        this.timestamp = timestamp;
        this.command = command;
        this.duration = duration;
    }
}
