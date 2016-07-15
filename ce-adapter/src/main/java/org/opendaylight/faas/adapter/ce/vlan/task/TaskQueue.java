/**
 * Copyright (c) 2015 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.opendaylight.faas.adapter.ce.vlan.CETelnetOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskQueue {
    private static final Logger LOG = LoggerFactory.getLogger(TaskQueue.class);

    private Queue<AbstractTask> queue = new ConcurrentLinkedQueue<>();

    private final ListeningExecutorService executor;

    private final RunNextTaskCallback callback = new RunNextTaskCallback();

    private final CETelnetOperator oper;

    private boolean isRunning = false;

    public TaskQueue(ListeningExecutorService executor, CETelnetOperator oper) {
        this.executor = executor;
        this.oper = oper;
    }

    public void addTask(AbstractTask task) {
        synchronized (queue) {
            if (!isRunning && queue.isEmpty()) {
                startTask(task);
                isRunning = true;
            } else {
                queue.add(task);
            }
        }
    }

    void startTask(AbstractTask task) {
        task.setOperator(oper);
        ListenableFuture<Void> future = executor.submit(task);
        Futures.addCallback(future, callback);
    }

    private final class RunNextTaskCallback implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
            synchronized (queue) {
                AbstractTask nextTask = queue.poll();
                if (nextTask != null) {
                    startTask(nextTask);
                } else {
                    isRunning = false;
                }
            }
        }

        @Override
        public void onFailure(Throwable thr) {
            LOG.error("", thr);

            synchronized (queue) {
                AbstractTask nextTask = queue.poll();
                if (nextTask != null) {
                    startTask(nextTask);
                } else {
                    isRunning = false;
                }
            }
        }

    }
}
