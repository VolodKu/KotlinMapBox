package com.liveroads.common.executors

import android.os.Process
import com.liveroads.util.executors.AndroidExecutorThreadFactory
import com.liveroads.util.executors.ImmediateExecutor

object Executors {

    val NORMAL_PRIORITY_IMMEDIATE = ImmediateExecutor(
            AndroidExecutorThreadFactory(
                    "LrNormImmedExecutor",
                    "LrNormImmedExec-",
                    Thread.NORM_PRIORITY,
                    Process.THREAD_PRIORITY_DEFAULT
            )
    )

}
