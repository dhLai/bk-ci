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

package com.tencent.devops.process.engine.dao

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.model.process.Tables.T_PIPELINE_RESOURCE_VERSION
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineResVersionDao @Autowired constructor(private val objectMapper: ObjectMapper) {

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        creator: String,
        version: Int,
        versionName: String = "init",
        model: Model
    ) {
        val modelString = objectMapper.writeValueAsString(model)
        create(
            dslContext = dslContext,
            pipelineId = pipelineId,
            creator = creator,
            version = version,
            versionName = versionName,
            modelString = modelString
        )
    }

    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        creator: String,
        version: Int,
        versionName: String = "init",
        modelString: String
    ) {
        with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VERSION,
                VERSION_NAME,
                MODEL,
                CREATOR,
                CREATE_TIME
            ).values(pipelineId, version, versionName, modelString, creator, LocalDateTime.now())
                .onDuplicateKeyUpdate()
                .set(MODEL, modelString)
                .set(CREATOR, creator)
                .set(VERSION_NAME, versionName)
                .set(CREATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun getVersionModelString(
        dslContext: DSLContext,
        pipelineId: String,
        version: Int?
    ): String? {

        return with(T_PIPELINE_RESOURCE_VERSION) {
            val where = dslContext.select(MODEL)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
            if (version != null) {
                where.and(VERSION.eq(version))
            } else {
                where.orderBy(VERSION.desc()).limit(1)
            }
            where.fetchAny(0, String::class.java)
        }
    }

    fun deleteEarlyVersion(
        dslContext: DSLContext,
        pipelineId: String,
        currentVersion: Int,
        maxPipelineResNum: Int
    ): Int {
        return with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.le(currentVersion - maxPipelineResNum))
                .execute()
        }
    }

    fun deleteByVer(dslContext: DSLContext, pipelineId: String, version: Int) {
        return with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .execute()
        }
    }

    fun listPipelineVersion(
        dslContext: DSLContext,
        pipelineId: String,
        offset: Int,
        limit: Int
    ): List<PipelineVersionSimple> {
        val list = mutableListOf<PipelineVersionSimple>()
        with(T_PIPELINE_RESOURCE_VERSION) {
            val result = dslContext.select(CREATE_TIME, CREATOR, VERSION_NAME, VERSION)
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .orderBy(VERSION.desc())
                .limit(limit).offset(offset)
                .fetch()

            result?.forEach {
                list.add(PipelineVersionSimple(
                    pipelineId = pipelineId,
                    creator = it[CREATOR] ?: "unknown",
                    createTime = it.get(CREATE_TIME)?.timestampmilli() ?: 0,
                    version = it[VERSION] ?: 1,
                    versionName = it[VERSION_NAME] ?: "init"
                ))
            }
        }
        return list
    }

    fun count(dslContext: DSLContext, pipelineId: String): Int {
        with(T_PIPELINE_RESOURCE_VERSION) {
            return dslContext.select(DSL.count(PIPELINE_ID))
                .from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun deleteAllVersion(dslContext: DSLContext, pipelineId: String) {
        return with(T_PIPELINE_RESOURCE_VERSION) {
            dslContext.deleteFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }
}
