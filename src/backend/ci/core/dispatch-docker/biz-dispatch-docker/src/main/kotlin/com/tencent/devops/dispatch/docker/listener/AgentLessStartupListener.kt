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

package com.tencent.devops.dispatch.docker.listener

import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.DispatchSdkErrorCode
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.service.PipelineAgentLessDispatchService
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AgentLessStartupListener @Autowired
constructor(
    private val pipelineAgentLessDispatchService: PipelineAgentLessDispatchService,
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) {

    @RabbitListener(
        bindings = [(QueueBinding(
            key = [MQ.ROUTE_BUILD_LESS_AGENT_STARTUP_DISPATCH], value = Queue(
                value = MQ.QUEUE_BUILD_LESS_AGENT_STARTUP_DISPATCH, durable = "true"
            ),
            exchange = Exchange(
                value = MQ.EXCHANGE_BUILD_LESS_AGENT_LISTENER_DIRECT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.DIRECT
            )
        ))]
    )
    fun listenAgentStartUpEvent(event: PipelineBuildLessStartupDispatchEvent) {
        try {
            logger.info("start build less($event)")
            pipelineAgentLessDispatchService.startUpBuildLess(event)
        } catch (discard: Throwable) {
            logger.warn("[${event.buildId}|${event.vmSeqId}] Container startup failure")

            buildLogPrinter.addRedLine(
                buildId = event.buildId,
                message = "Start buildless Docker VM failed. ${discard.message}",
                tag = VMUtils.genStartVMTaskId(event.vmSeqId),
                jobId = event.containerHashId,
                executeCount = event.executeCount ?: 1
            )

            val (errorType, errorCode, errorMsg) = if (discard is DockerServiceException) {
                Triple(first = discard.errorType, second = discard.errorCode, third = discard.message)
            } else {
                Triple(
                    first = ErrorType.SYSTEM,
                    second = DispatchSdkErrorCode.SDK_SYSTEM_ERROR,
                    third = "Fail to handle the start up message")
            }

            try {
                client.get(ServiceBuildResource::class).setVMStatus(
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId,
                    status = BuildStatus.FAILED,
                    errorType = errorType,
                    errorCode = errorCode,
                    errorMsg = errorMsg
                )
            } catch (ignore: ClientException) {
                logger.error("SystemErrorLogMonitor|listenAgentStartUpEvent|${event.buildId}|error=${ignore.message}")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AgentLessStartupListener::class.java)
    }
}
