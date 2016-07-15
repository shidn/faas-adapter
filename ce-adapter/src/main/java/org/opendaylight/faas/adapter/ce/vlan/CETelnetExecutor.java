/**
 * Copyright (c) 2015 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Map;
import java.util.concurrent.Executors;

import org.opendaylight.faas.adapter.ce.vlan.task.AbstractTask;
import org.opendaylight.faas.adapter.ce.vlan.task.TaskQueue;

public class CETelnetExecutor {
    private static CETelnetExecutor _instance = new CETelnetExecutor();

    public static CETelnetExecutor getInstance() {
        return _instance;
    }

    private final ListeningExecutorService executor;

    private Map<String, TaskQueue> queues = Maps.newHashMap();

    private Map<String, CETelnetOperator> opers = Maps.newHashMap();

    private CETelnetExecutor() {
        executor = MoreExecutors.listeningDecorator(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    public void addOperator(String device, CETelnetOperator oper) {
        opers.put(device, oper);
    }

    public void addTask(String device, AbstractTask task) {
        TaskQueue queue = null;
        synchronized (queues) {
            queue = queues.get(device);
            if (queue == null) {
                queue = new TaskQueue(executor, opers.get(device));
                queues.put(device, queue);
            }
        }
        queue.addTask(task);
    }
}
