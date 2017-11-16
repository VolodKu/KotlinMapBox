package com.liveroads.util.executors

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val CORE_POOL_SIZE = 1
private const val MAX_POOL_SIZE = Integer.MAX_VALUE
private const val KEEP_ALIVE_SECONDS = 2L

/**
 * An executor that creates a new thread and executes tasks immediately if there are no idle threads in the pool.
 */
class ImmediateExecutor(threadFactory: ThreadFactory?) : ThreadPoolExecutor(
        CORE_POOL_SIZE,
        MAX_POOL_SIZE,
        KEEP_ALIVE_SECONDS,
        TimeUnit.SECONDS,
        SynchronousQueue<Runnable>(),
        threadFactory
)
