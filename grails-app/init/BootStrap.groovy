import org.quartz.Scheduler

class BootStrap {

    Scheduler quartzScheduler
    def quartzJobService

    def init = { servletContext ->
        quartzScheduler.listenerManager.addTriggerListener(quartzJobService)
        quartzScheduler.listenerManager.addJobListener(quartzJobService)
    }

    def destroy = {
    }
}
