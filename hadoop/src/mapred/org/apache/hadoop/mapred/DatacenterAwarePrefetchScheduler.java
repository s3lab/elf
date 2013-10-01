package org.apache.hadoop.mapred;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.hadoop.mapreduce.server.jobtracker.TaskTracker;

public class DatacenterAwarePrefetchScheduler extends PrefetchScheduler {

    private JobQueueTaskScheduler jtScheduler = null;

    public DatacenterAwarePrefetchScheduler(TaskScheduler jtScheduler) {
        if(jtScheduler instanceof JobQueueTaskScheduler){
            this.jtScheduler = (JobQueueTaskScheduler) jtScheduler;
        } else {
            JobTracker.LOG.error("Incompatible scheduler");
            throw new NullPointerException();
        }
    }

    @Override
    List<PrefetchTask> assignPrefetchTask(TaskTracker taskTracker) {
        TaskTrackerStatus taskTrackerStatus = taskTracker.getStatus();
        Collection<JobInProgress> jobQueue = jtScheduler.getJobs();

        List<PrefetchTask> assignedTasks = new ArrayList<PrefetchTask>();

        int availablePrefetchTaskSlot = taskTrackerStatus.getAvailablePrefetchSlot();
        for (int i = 0; i < availablePrefetchTaskSlot; i++) {
            synchronized (jobQueue){
                for(JobInProgress job: jobQueue){
                    PrefetchTask t =
                            job.getPrefetchTasks(taskTracker);
                    if(t != null){
                        assignedTasks.add(t);
                        break; // jump out and re-scan all the jobs
                    }
                }
            }
        }
        return assignedTasks;
    }
}
