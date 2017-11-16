package com.liveroads.util.executors

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class AndroidExecutorThreadFactory(
        val threadGroupName: String,
        val threadNamePrefix: String,
        val javaPriority: Int,
        val androidPriority: Int
) : ThreadFactory {

    val threadGroup = ThreadGroup(threadGroupName)
    private val nextId = AtomicInteger()

    override fun newThread(runnable: Runnable): Thread {
        val name = "$threadNamePrefix-${nextId.incrementAndGet()}"
        val executorRunnable = AndroidExecutorRunnable(runnable, androidPriority)
        val thread = Thread(threadGroup, executorRunnable, name)
        thread.priority = javaPriority
        return thread
    }

}

private class AndroidExecutorRunnable(val runnable: Runnable, val androidPriority: Int) : Runnable {

    override fun run() {
        android.os.Process.setThreadPriority(androidPriority)
        runnable.run()
    }

}
