/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.dispatch.dao.DispatchPipelineBuildDao
import com.tencent.devops.dispatch.pojo.PipelineBuild
import com.tencent.devops.dispatch.service.dispatcher.Dispatcher
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.jooq.DSLContext
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class PipelineDispatchService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val buildLogPrinter: BuildLogPrinter,
    private val dispatchPipelineBuildDao: DispatchPipelineBuildDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val jobQuotaBusinessService: JobQuotaBusinessService
) {

    private var dispatchers: Set<Dispatcher>? = null

    private fun getDispatchers(): Set<Dispatcher> {
        if (dispatchers == null) {
            synchronized(this) {
                if (dispatchers == null) {
                    val reflections = Reflections("com.tencent.devops.dispatch.service.dispatcher")
                    val dispatcherClasses = reflections.getSubTypesOf(Dispatcher::class.java)
                    if (dispatcherClasses == null || dispatcherClasses.isEmpty()) {
                        logger.error("The dispatcher is empty $dispatcherClasses")
                        throw InvalidParamException("Dispatcher is empty")
                    }
                    logger.info("Get the dispatch classes $dispatcherClasses")
                    dispatchers = dispatcherClasses.map {
                        SpringContextUtil.getBean(it)
                    }.toSet()
                }
            }
        }
        return dispatchers!!
    }

    fun startUp(startupEvent: PipelineAgentStartupEvent) {
        logger.info("ENGINE|${startupEvent.buildId}|VM_START|j(${startupEvent.vmSeqId})|" +
            "dispatchType=${startupEvent.dispatchType}")
        // Check if the pipeline is running
        val resource = client.get(ServicePipelineResource::class).isPipelineRunning(
            projectId = startupEvent.projectId,
            buildId = startupEvent.buildId,
            channelCode = ChannelCode.valueOf(startupEvent.channelCode)
        )
        if (resource.isNotOk() || resource.data == null) {
            logger.warn("ENGINE|${startupEvent.buildId}|VM_START|j(${startupEvent.vmSeqId})|msg=${resource.message}")
            return
        }

        if (!resource.data!!) {
            logger.warn("ENGINE|${startupEvent.buildId}|VM_START|j(${startupEvent.vmSeqId})|ALREADY_FINISH")
            return
        }

        if (startupEvent.retryTime == 0) {
            buildLogPrinter.addLine(
                buildId = startupEvent.buildId,
                message = "构建环境准备中...",
                tag = VMUtils.genStartVMTaskId(startupEvent.containerId),
                jobId = startupEvent.containerHashId,
                executeCount = startupEvent.executeCount ?: 1
            )
        }

        val dispatcher = findDispatch(startupEvent) // 最少只有一个Dispatcher
        if (dispatcher != null) {
//            // JOB配额判断
//            if (!jobQuotaBusinessService.checkJobQuota(startupEvent, buildLogPrinter)) {
//                dispatcher.onFailBuild(
//                    client = client,
//                    buildLogPrinter = buildLogPrinter,
//                    event = startupEvent,
//                    errorType = ErrorType.USER,
//                    errorCode = ErrorCodeEnum.JOB_QUOTA_EXCESS.errorCode,
//                    errorMsg = "系统JOB配额超限"
//                )
            dispatcher.startUp(startupEvent)
//            } else {
//                // 到这里说明JOB已经启动成功(但是不代表Agent启动成功)，开始累加使用额度
//                val vmType = JobQuotaVmType.parse(startupEvent.dispatchType)
//                if (null != vmType) {
//                    jobQuotaBusinessService.insertRunningJob(
//                        projectId = startupEvent.projectId,
//                        vmType = vmType,
//                        buildId = startupEvent.buildId,
//                        vmSeqId = startupEvent.vmSeqId
//                    )
//                }
//            }
        } else {
            logger.error("BKSystemErrorMonitor|ENGINE|${startupEvent.buildId}|VM_START|j(${startupEvent.vmSeqId})|" +
                "dispatchType=${startupEvent.dispatchType}")
        }
    }

    /**
     * 根据事件[startupEvent]参数，查找合适的[Dispatcher],如果没有则返回空。
     */
    private fun findDispatch(startupEvent: PipelineAgentStartupEvent): Dispatcher? {
        getDispatchers().forEach {
            if (it.canDispatch(startupEvent)) {
                return it
            }
        }
        return null
    }

    fun shutdown(pipelineAgentShutdownEvent: PipelineAgentShutdownEvent) {
        logger.info("Start to finish the pipeline build($pipelineAgentShutdownEvent)")
        getDispatchers().forEach {
//            try {
            it.shutdown(pipelineAgentShutdownEvent)
//            } finally {
//                // 不管shutdown成功失败，都要回收配额；这里回收job，将自动累加agent执行时间
//                jobQuotaBusinessService.deleteRunningJob(
//                    projectId = pipelineAgentShutdownEvent.projectId,
//                    buildId = pipelineAgentShutdownEvent.buildId,
//                    vmSeqId = pipelineAgentShutdownEvent.vmSeqId
//                )
//            }
        }
    }

    fun reDispatch(pipelineAgentStartupEvent: PipelineAgentStartupEvent) {
        findDispatch(pipelineAgentStartupEvent)?.retry(
            client = client,
            buildLogPrinter = buildLogPrinter,
            pipelineEventDispatcher = pipelineEventDispatcher,
            event = pipelineAgentStartupEvent
        )
    }

    fun queryPipelineByBuildAndSeqId(buildId: String, vmSeqId: String): PipelineBuild {
        val list = dispatchPipelineBuildDao.getPipelineByBuildIdOrNull(
            dslContext = dslContext,
            buildId = buildId,
            vmSeqId = vmSeqId
        )
        if (list.isEmpty()) {
            throw throw NotFoundException("VM pipeline[$buildId,$vmSeqId] is not exist")
        }
        return dispatchPipelineBuildDao.convert(list[0])
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineDispatchService::class.java)
    }
}
