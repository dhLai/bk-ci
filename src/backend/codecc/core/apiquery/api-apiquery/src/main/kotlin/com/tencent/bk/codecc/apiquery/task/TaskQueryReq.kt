package com.tencent.bk.codecc.apiquery.task

data class TaskQueryReq(
    //bg唯一id
    val bgId : Int?,
    //项目id
    val projectId : String?,
    //任务清单
    val taskIdList : List<Long>?,
    //工具名
    val toolName : String?,
    // 流水线ID列表
    val pipelineIdList: List<String>?,
    /**
     * 以下为查询告警时的过滤条件
     */
    // 开始时间
    val startTime: Long?,
    // 结束时间
    val endTime: Long?,
    //状态
    val status : List<Int>?,
    //规则
    val checker : List<String>?,
    //过滤规则
    val notChecker : String?,
    //过滤字段
    val filterFields: List<String>?

)