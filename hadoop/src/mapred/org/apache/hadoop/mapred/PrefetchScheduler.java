package org.apache.hadoop.mapred;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.server.jobtracker.TaskTracker;

import java.util.List;

abstract class PrefetchScheduler implements Configurable {
    Configuration conf;

    public Configuration getConf() {
        return conf;
    }

    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Assign new prefetch task to task tracker to do the prefetch
     * */
    abstract List<PrefetchTask> assignPrefetchTask(TaskTracker taskTracker);
}
