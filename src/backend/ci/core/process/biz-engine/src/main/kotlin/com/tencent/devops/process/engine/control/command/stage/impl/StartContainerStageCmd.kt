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

package com.tencent.devops.process.engine.control.command.stage.impl

import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.control.FastKillUtils
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.stage.StageCmd
import com.tencent.devops.process.engine.control.command.stage.StageContext
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Stage下发Container事件命令处理
 */
@Service
class StartContainerStageCmd(
    private val pipelineStageService: PipelineStageService,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : StageCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(StartContainerStageCmd::class.java)
    }

    override fun canExecute(commandContext: StageContext): Boolean {
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE
    }

    override fun execute(commandContext: StageContext) {
//        val event = commandContext.event
        val stageId = commandContext.stage.stageId
        // 执行成功则结束本次事件处理，否则要尝试下一stage
        commandContext.buildStatus = judgeStageContainer(commandContext)
//            LOG.info("ENGINE|${event.buildId}|${event.source}|STAGE_DO|$stageId|${commandContext.buildStatus}")
        commandContext.latestSummary = "from_s($stageId)"
//        }
        commandContext.cmdFlowState = CmdFlowState.FINALLY
    }

    /**
     * 判断当前[commandContext]里的Stage并发下发Container消息，并返回Stage状态
     */
    private fun judgeStageContainer(commandContext: StageContext): BuildStatus {
        val event = commandContext.event
        var stageStatus = commandContext.stage.status
        var newActionType = event.actionType
        // 针对刚启动的Stage
        if (stageStatus.isReadyToRun()) {
            if (newActionType == ActionType.REFRESH) { // 对未启动的Stage要变成开始指令
                newActionType = ActionType.START
            }

            when {
                newActionType.isStart() -> stageStatus = BuildStatus.RUNNING // 要启动Stage
                newActionType.isEnd() -> stageStatus = BuildStatus.CANCELED // 若为终止命令，直接设置为取消
                newActionType == ActionType.SKIP -> stageStatus = BuildStatus.SKIP // 要跳过Stage
            }
        }

        return if (stageStatus.isFinish() || stageStatus == BuildStatus.STAGE_SUCCESS) {
            stageStatus // 已经是结束或者是STAGE_SUCCESS就直接返回
        } else {
            val runningStatus = sendContainerEvent(commandContext, actionType = newActionType, userId = event.userId)
            return when {
                runningStatus != null -> runningStatus
                commandContext.failureContainerNum > 0 -> BuildStatus.FAILED // 存在失败
                commandContext.cancelContainerNum > 0 -> BuildStatus.CANCELED // 存在取消
                commandContext.skipContainerNum == commandContext.containers.size -> BuildStatus.SKIP // 全部跳过
                else -> BuildStatus.SUCCEED
            }
        }
    }

    /**
     * [userId]执行人利用当前[commandContext]的容器列表，以事件动作[actionType]
     * 加上各容器的状态，决定是否对相应容器进行下发容器动作事件，并最终返回当前stage的构建状态:
     * 仍然有容器未结束，会继续向该容器下发事件，并返回：[BuildStatus.RUNNING]
     * 所有容器都结束，但有失败的容器，则直接返回:[BuildStatus.FAILED]
     * 所有容器都结束，但有取消的容器，则直接返回:[BuildStatus.CANCELED]
     */
    private fun sendContainerEvent(commandContext: StageContext, actionType: ActionType, userId: String): BuildStatus? {

        var stageStatus: BuildStatus? = null

        // 查找最后一个结束状态的Stage (排除Finally）
        if (commandContext.stage.controlOption?.finally == true) {
            commandContext.previousStageStatus = pipelineStageService.listStages(commandContext.stage.buildId)
                .lastOrNull {
                    it.stageId != commandContext.stage.stageId &&
                        (it.status.isFinish() || it.status == BuildStatus.STAGE_SUCCESS)
                }?.status
        }
        // 同一Stage下的多个Container是并行
        commandContext.containers.forEach { container ->
            if (container.status.isCancel()) {
                commandContext.cancelContainerNum++
            } else if (ControlUtils.checkContainerFailure(container)) {
                commandContext.failureContainerNum++
            } else if (container.status == BuildStatus.SKIP) {
                commandContext.skipContainerNum++
            } else if (container.status.isRunning() && !actionType.isTerminate()) {
                // 已经在运行中的, 只接受强制终止
                stageStatus = BuildStatus.RUNNING
            } else if (!container.status.isFinish()) {

                stageStatus = BuildStatus.RUNNING
                sendBuildContainerEvent(commandContext, container, actionType = actionType, userId = userId)

                LOG.info("ENGINE|${container.buildId}|STAGE_CONTAINER_SEND|s(${container.stageId})|" +
                    "j(${container.containerId})|status=${container.status}|newActonType=$actionType")
            }
        }

        return stageStatus
    }

    private fun sendBuildContainerEvent(
        commandContext: StageContext,
        container: PipelineBuildContainer,
        actionType: ActionType,
        userId: String
    ) {
        val errorCode: Int
        val errorTypeName: String?
        if (commandContext.fastKill) {
            val fastKillCodeType = FastKillUtils.fastKillCodeType()
            errorCode = fastKillCodeType.second
            errorTypeName = fastKillCodeType.first.name
        } else {
            errorTypeName = null
            errorCode = 0
        }
        // 通知容器构建消息
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "From_s(${container.stageId})",
                projectId = container.projectId,
                pipelineId = container.pipelineId,
                userId = userId,
                buildId = container.buildId,
                stageId = container.stageId,
                previousStageStatus = commandContext.previousStageStatus,
                containerType = container.containerType,
                containerId = container.containerId,
                actionType = actionType,
                errorCode = errorCode,
                errorTypeName = errorTypeName,
                reason = commandContext.latestSummary
            )
        )
    }
}
