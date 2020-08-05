package com.tencent.bk.codecc.quartz.api

import com.tencent.bk.codecc.quartz.pojo.JobInfoVO
import com.tencent.bk.codecc.quartz.pojo.ShardingResultVO
import com.tencent.devops.common.api.pojo.CodeCCResult
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * 获取任务详情接口
 */
@Api(tags = ["USER_JOB"], description = "任务管理接口")
@Path("/user/job")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserJobRestResource {

    @ApiOperation("获取正在执行job信息")
    @Path("/existing")
    @GET
    fun getExistingJob(): CodeCCResult<List<JobInfoVO>>

    @ApiOperation("删除所有正在执行的任务")
    @Path("/all/jobs/{dataDelete}")
    @DELETE
    fun deleteAllJobs(
        @ApiParam("是否删除表中数据")
        @PathParam("dataDelete")
        dataDelete : Int
    ) : CodeCCResult<Boolean>


    @ApiOperation("初始化任务信息")
    @Path("/all/jobs")
    @POST
    fun initAllJobs() : CodeCCResult<Boolean>

    @ApiOperation("获取分片信息")
    @Path("/shardingResult")
    @GET
    fun getShardingResult(): CodeCCResult<ShardingResultVO?>

}