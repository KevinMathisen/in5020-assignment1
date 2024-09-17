package com.ass1.server;

import java.io.Serializable;

public class Response implements Serializable {
    private final int result;
    private final long executionTime;
    private final long waitingTime;
    private final int serverZone;

    public Response(int result, long executionTime, long waitingTime, int serverZone) {
        this.result = result;
        this.executionTime = executionTime;
        this.waitingTime = waitingTime;
        this.serverZone = serverZone;
    }

    public int getResult() {
        return result;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public long getWaitingTime() {
        return waitingTime;
    }

    public int getServerZone() {
        return serverZone;
    }
}