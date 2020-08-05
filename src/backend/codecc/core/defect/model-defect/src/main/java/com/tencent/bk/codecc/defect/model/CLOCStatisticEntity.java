/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.tencent.bk.codecc.defect.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * cloc统计信息表
 * 
 * @date 2020/4/9
 * @version V1.0
 */
@Data
@Document(collection = "t_cloc_statistic")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_language_1", def = "{'task_id': 1, 'language': 1}")
})
public class CLOCStatisticEntity extends CommonEntity
{
    @Field("task_id")
    @Indexed
    private Long taskId;

    @Field("stream_name")
    private String streamName;

    @Field("tool_name")
    private String toolName;

    @Field("sum_blank")
    private Long sumBlank;

    @Field("sum_code")
    private Long sumCode;

    @Field("sum_comment")
    private Long sumComment;

    @Field("language")
    private String language;
}
