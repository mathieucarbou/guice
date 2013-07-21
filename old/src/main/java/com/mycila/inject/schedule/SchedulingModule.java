/**
 * Copyright (C) 2010 Mycila (mathieu.carbou@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycila.inject.schedule;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Mathieu Carbou
 */
public final class SchedulingModule extends AbstractModule {

    private static final Logger LOGGER = Logger.getLogger(SchedulingModule.class.getName());

    @Override
    protected void configure() {
        bind(Init.class);
        bind(SchedulerFactory.class).to(StdSchedulerFactory.class).in(Singleton.class);

        bindListener(new AbstractMatcher<TypeLiteral<?>>() {
                @Override
                public boolean matches(TypeLiteral<?> type) {
                    return type.getRawType().isAnnotationPresent(Cron.class) && Runnable.class.isAssignableFrom(type.getRawType());
                }
            }, new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                final Provider<Scheduler> scheduler = encounter.getProvider(Scheduler.class);
                encounter.register(new InjectionListener<I>() {
                    @Override
                    public void afterInjection(I injectee) {
                        Cron cron = injectee.getClass().getAnnotation(Cron.class);
                        if (LOGGER.isLoggable(Level.INFO)) {
                            LOGGER.info("Registering cron job " + injectee.getClass().getName() + " at frequency: " + cron.value());
                        }
                        try {
                            scheduler.get().addJob(newJob(QuartzAdapter.class)
                                .storeDurably()
                                .withIdentity(injectee.getClass().getName() + "-job", SchedulingModule.this.getClass().getSimpleName())
                                .usingJobData(new JobDataMap(ImmutableMap.of(QuartzAdapter.class.getName(), injectee)))
                                .requestRecovery(true)
                                .build(), true);
                            scheduler.get().scheduleJob(newTrigger()
                                .withIdentity(injectee.getClass().getName() + "-trigger", SchedulingModule.this.getClass().getSimpleName())
                                .withSchedule(cronSchedule(cron.value()))
                                .forJob(injectee.getClass().getName() + "-job", SchedulingModule.this.getClass().getSimpleName())
                                .build());
                        } catch (SchedulerException e) {
                            throw new ProvisionException(e.getMessage(), e);
                        }
                    }
                });
            }
        }
        );
    }

    @Provides
    @Singleton
    Scheduler scheduler(SchedulerFactory schedulerFactory) throws SchedulerException {
        Scheduler scheduler = schedulerFactory.getScheduler();
        if (!scheduler.isStarted()) {
            scheduler.start();
        }
        return scheduler;
    }

    @Singleton
    static class Init {

        @Inject
        Scheduler scheduler;

        @PreDestroy
        void close() throws SchedulerException {
            LOGGER.info("Closing scheduler...");
            scheduler.shutdown();
            LOGGER.info("Closed !");
        }

    }

    @DisallowConcurrentExecution
    public static final class QuartzAdapter implements InterruptableJob {

        private AtomicReference<Thread> runningThread = new AtomicReference<Thread>();

        @Override
        public void interrupt() throws UnableToInterruptJobException {
            Thread running = runningThread.get();
            if (running != null) {
                running.interrupt();
            }
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Runnable job = (Runnable) context.getJobDetail().getJobDataMap().get(QuartzAdapter.class.getName());
            if (job == null) {
                throw new JobExecutionException("Job not found !");
            }
            if (runningThread.compareAndSet(null, Thread.currentThread())) {
                try {
                    job.run();
                } catch (RuntimeException e) {
                    LOGGER.log(Level.SEVERE, "Error in job " + job.getClass().getName() + " : " + e.getMessage(), e);
                    throw new JobExecutionException(e.getMessage(), e);
                } finally {
                    runningThread.set(null);
                }
            } else {
                throw new JobExecutionException("Illegal invocation: job is already running from thread: " + runningThread.get().getName());
            }
        }
    }
}
