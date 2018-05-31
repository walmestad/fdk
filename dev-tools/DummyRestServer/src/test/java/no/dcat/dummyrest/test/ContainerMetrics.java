package no.dcat.dummyrest.test;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ContainerMetrics {
    @JsonProperty(value="mem")
    private int mem;
    @JsonProperty(value="systemload.average")
    private int systemloadAverage;
    @JsonProperty(value="threads")
    private int threads;

    public int getMem() {
        return mem;
    }

    public void setMem(int mem) {
        this.mem = mem;
    }

    public int getSystemloadAverage() {
        return systemloadAverage;
    }

    public void setSystemloadAverage(int systemloadAverage) {
        this.systemloadAverage = systemloadAverage;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }
}
