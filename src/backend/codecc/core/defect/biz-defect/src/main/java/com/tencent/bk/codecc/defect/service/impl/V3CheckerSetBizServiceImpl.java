package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetProjectRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetTaskRelationshipRepository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CheckerDetailDao;
import com.tencent.bk.codecc.defect.dao.mongotemplate.CheckerSetDao;
import com.tencent.bk.codecc.defect.model.CheckerDetailEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerPropsEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetCatagoryEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetProjectRelationshipEntity;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.service.ToolBuildInfoService;
import com.tencent.bk.codecc.defect.vo.CheckerCommonCountVO;
import com.tencent.bk.codecc.defect.vo.CheckerCountListVO;
import com.tencent.bk.codecc.defect.vo.CheckerListQueryReq;
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.defect.vo.OtherCheckerSetListQueryReq;
import com.tencent.bk.codecc.defect.vo.UpdateAllCheckerReq;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.api.ServiceToolRestResource;
import com.tencent.bk.codecc.task.vo.BaseDataVO;
import com.tencent.bk.codecc.task.vo.BatchRegisterVO;
import com.tencent.bk.codecc.task.vo.TaskBaseVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.checkerset.CheckerPropVO;
import com.tencent.devops.common.api.checkerset.CheckerSetCategoryVO;
import com.tencent.devops.common.api.checkerset.CheckerSetCodeLangVO;
import com.tencent.devops.common.api.checkerset.CheckerSetManagementReqVO;
import com.tencent.devops.common.api.checkerset.CheckerSetParamsVO;
import com.tencent.devops.common.api.checkerset.CheckerSetRelationshipVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVersionVO;
import com.tencent.devops.common.api.checkerset.CreateCheckerSetReqVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.CheckerConstants;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.util.ListSortUtil;
import com.tencent.devops.common.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.CloneUtils;
import org.json.JSONArray;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.tencent.devops.common.web.mq.ConstantsKt.EXCHANGE_TASK_CHECKER_CONFIG;
import static com.tencent.devops.common.web.mq.ConstantsKt.ROUTE_IGNORE_CHECKER;

/**
 * V3规则集服务实现类
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Slf4j
@Service
public class V3CheckerSetBizServiceImpl implements IV3CheckerSetBizService
{
    /**
     * 规则集语言参数
     */
    private static final String KEY_LANG = "LANG";

    @Autowired
    private CheckerSetRepository checkerSetRepository;

    @Autowired
    private CheckerSetDao checkerSetDao;

    @Autowired
    private Client client;

    @Autowired
    private ToolBuildInfoService toolBuildInfoService;

    @Autowired
    private CheckerSetProjectRelationshipRepository checkerSetProjectRelationshipRepository;

    @Autowired
    private CheckerSetTaskRelationshipRepository checkerSetTaskRelationshipRepository;

    @Autowired
    private CheckerDetailDao checkerDetailDao;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 创建规则集
     *
     * @param user
     * @param projectId
     * @param createCheckerSetReqVO
     * @return
     */
    @Override
    public void createCheckerSet(String user, String projectId, CreateCheckerSetReqVO createCheckerSetReqVO)
    {
        if (StringUtils.isEmpty(createCheckerSetReqVO.getCheckerSetId()) || StringUtils.isEmpty(createCheckerSetReqVO.getCheckerSetName()))
        {
            String errMsgStr = "规则集ID、规则集名称";
            log.error("{}不能为空", errMsgStr);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_NULL, new String[]{errMsgStr}, null);
        }

        // 校验规则集ID是否已存在
        checkIdDuplicate(createCheckerSetReqVO.getCheckerSetId(), createCheckerSetReqVO.getCheckerSetName());

        // 校验规则集名称在项目中是否已存在
        checkNameExistInProject(createCheckerSetReqVO.getCheckerSetName(), projectId);

        // 获取规则集基础信息
        long currentTime = System.currentTimeMillis();
        CheckerSetEntity checkerSetEntity = new CheckerSetEntity();
        BeanUtils.copyProperties(createCheckerSetReqVO, checkerSetEntity);
        checkerSetEntity.setCreateTime(currentTime);
        checkerSetEntity.setCreator(user);
        checkerSetEntity.setLastUpdateTime(currentTime);
        checkerSetEntity.setOfficial(CheckerConstants.CheckerSetOfficial.NOT_OFFICIAL.code());
        checkerSetEntity.setVersion(CheckerConstants.DEFAULT_VERSION);
        checkerSetEntity.setEnable(CheckerConstants.CheckerSetEnable.ENABLE.code());
        checkerSetEntity.setProjectId(projectId);
        checkerSetEntity.setScope(CheckerConstants.CheckerSetScope.PRIVATE.code());

        checkerSetEntity.setDefaultCheckerSet(false);
        checkerSetEntity.setOfficial(CheckerConstants.CheckerSetOfficial.NOT_OFFICIAL.code());

        // 查询语言参数列表
        CodeCCResult<List<BaseDataVO>> paramsCodeCCResult = client.get(ServiceBaseDataResource.class).getParamsByType(ComConstants.KEY_CODE_LANG);
        if (paramsCodeCCResult.isNotOk() || CollectionUtils.isEmpty(paramsCodeCCResult.getData()))
        {
            log.error("param list is empty! param type: {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<BaseDataVO> codeLangParams = paramsCodeCCResult.getData();
        checkerSetEntity.setCheckerSetLang(List2StrUtil.toString(getCodelangs(createCheckerSetReqVO.getCodeLang(), codeLangParams), ","));

        // 加入规则集类型中英文名称
        List<CheckerSetCatagoryEntity> catagoryEntities = getCatagoryEntities(createCheckerSetReqVO.getCatagories());
        checkerSetEntity.setCatagories(catagoryEntities);

        // 如果选择了基于某个规则集或者复制与某个规则集，则需要更新规则集中的规则
        if (StringUtils.isNotEmpty(createCheckerSetReqVO.getBaseCheckerSetId()))
        {
            CheckerSetEntity baseCheckerSet;
            if (createCheckerSetReqVO.getBaseCheckerSetVersion() == null || createCheckerSetReqVO.getBaseCheckerSetVersion() == Integer.MAX_VALUE)
            {
                List<CheckerSetEntity> baseCheckerSets = checkerSetRepository.findByCheckerSetId(createCheckerSetReqVO.getBaseCheckerSetId());
                baseCheckerSets.sort(((o1, o2) -> o2.getVersion().compareTo(o1.getVersion())));
                baseCheckerSet = baseCheckerSets.get(0);
            }
            else
            {
                baseCheckerSet = checkerSetRepository.findByCheckerSetIdAndVersion(createCheckerSetReqVO.getBaseCheckerSetId(), createCheckerSetReqVO.getBaseCheckerSetVersion());
            }
            if (baseCheckerSet == null)
            {
                String errMsg = "找不到规则集，ID：" + createCheckerSetReqVO.getBaseCheckerSetId();
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            checkerSetEntity.setCheckerProps(baseCheckerSet.getCheckerProps());
            checkerSetEntity.setInitCheckers(true);
        }
        else
        {
            checkerSetEntity.setInitCheckers(false);
        }

        // 入库
        checkerSetRepository.save(checkerSetEntity);

        // 保存规则集与项目的关系
        CheckerSetProjectRelationshipEntity relationshipEntity = new CheckerSetProjectRelationshipEntity();
        relationshipEntity.setCheckerSetId(checkerSetEntity.getCheckerSetId());
        relationshipEntity.setVersion(checkerSetEntity.getVersion());
        relationshipEntity.setProjectId(projectId);
        relationshipEntity.setCreatedBy(user);
        relationshipEntity.setCreatedDate(System.currentTimeMillis());
        relationshipEntity.setUselatestVersion(true);
        relationshipEntity.setDefaultCheckerSet(false);
        checkerSetProjectRelationshipRepository.save(relationshipEntity);
    }

    @Override
    public Boolean updateCheckersOfSetForAll(String user, UpdateAllCheckerReq updateAllCheckerReq)
    {
        CheckerListQueryReq checkerListQueryReq = updateAllCheckerReq.getCheckerListQueryReq();
        List<CheckerPropVO> checkerPropVOS;
        if (CollectionUtils.isEmpty(updateAllCheckerReq.getCheckerProps()))
        {
            checkerPropVOS = new ArrayList<>();
        }
        else
        {
            checkerPropVOS = updateAllCheckerReq.getCheckerProps();
        }
        List<CheckerDetailEntity> checkerDetailEntityList = checkerDetailDao.findByComplexCheckerCondition(checkerListQueryReq.getKeyWord(), checkerListQueryReq.getCheckerLanguage(),
                checkerListQueryReq.getCheckerCategory(), checkerListQueryReq.getToolName(), checkerListQueryReq.getTag(),
                checkerListQueryReq.getSeverity(), checkerListQueryReq.getEditable(), checkerListQueryReq.getCheckerRecommend(),
                null, null, null, null, null, null);
        if (CollectionUtils.isNotEmpty(checkerDetailEntityList))
        {
            checkerDetailEntityList.forEach(checkerDetailEntity ->
            {
                if (checkerPropVOS.stream().noneMatch(checkerPropVO -> checkerPropVO.getCheckerKey().equals(checkerDetailEntity.getCheckerKey()) &&
                        checkerPropVO.getToolName().equals(checkerDetailEntity.getToolName())))
                {
                    CheckerPropVO checkerPropVO = new CheckerPropVO();
                    checkerPropVO.setToolName(checkerDetailEntity.getToolName());
                    checkerPropVO.setCheckerKey(checkerDetailEntity.getCheckerKey());
                    checkerPropVO.setCheckerName(checkerDetailEntity.getCheckerName());
                    checkerPropVOS.add(checkerPropVO);
                }
            });
        }
        updateCheckersOfSet(checkerListQueryReq.getCheckerSetId(), user, checkerPropVOS);
        return true;

    }


    /**
     * 更新规则集中的规则
     *
     * @param checkerSetId
     * @param checkerProps
     * @return
     */
    @Override
    public void updateCheckersOfSet(String checkerSetId, String user, List<CheckerPropVO> checkerProps)
    {
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isNotEmpty(checkerSetEntities))
        {
            List<CheckerPropsEntity> checkerPropsEntities = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(checkerProps))
            {
                for (CheckerPropVO checkerPropVO : checkerProps)
                {
                    CheckerPropsEntity checkerPropsEntity = new CheckerPropsEntity();
                    BeanUtils.copyProperties(checkerPropVO, checkerPropsEntity);
                    checkerPropsEntities.add(checkerPropsEntity);
                }
            }

            CheckerSetEntity checkerSetEntity = null;
            Set<Long> updateTaskSet = Sets.newHashSet();
            CheckerSetEntity firstCheckerSetEntity = checkerSetEntities.get(0);
            if (checkerSetEntities.size() == 1
                    && firstCheckerSetEntity.getInitCheckers() != null && !checkerSetEntities.get(0).getInitCheckers())
            {
                checkerSetEntity = checkerSetEntities.get(0);
                checkerSetEntity.setInitCheckers(true);
            }
            else
            {
                if (CollectionUtils.isNotEmpty(checkerSetEntities))
                {
                    checkerSetEntity = checkerSetEntities.get(0);
                    for (CheckerSetEntity currentCheckerSetEntity : checkerSetEntities)
                    {
                        if (currentCheckerSetEntity.getVersion() > checkerSetEntity.getVersion())
                        {
                            checkerSetEntity = currentCheckerSetEntity;
                        }
                    }
                    checkerSetEntity.setVersion(checkerSetEntity.getVersion() + 1);
                    checkerSetEntity.setEntityId(null);
                }
            }
            if (checkerSetEntity != null)
            {
                checkerSetEntity.setCheckerProps(checkerPropsEntities);

                // 包装规则集数据入库
                checkerSetRepository.save(checkerSetEntity);

                // 刷新已关联此规则集，切选择了latest版本自动更新的项目数据
                List<CheckerSetProjectRelationshipEntity> projectRelationships = checkerSetProjectRelationshipRepository.findByCheckerSetId(checkerSetId);
                if (CollectionUtils.isNotEmpty(projectRelationships))
                {
                    Map<String, Integer> currentCheckerSetVersionMap = Maps.newHashMap();
                    List<CheckerSetProjectRelationshipEntity> latestRelationships = Lists.newArrayList();
                    for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : projectRelationships)
                    {
                        currentCheckerSetVersionMap.put(projectRelationshipEntity.getCheckerSetId(), projectRelationshipEntity.getVersion());
                        if (projectRelationshipEntity.getUselatestVersion() != null && projectRelationshipEntity.getUselatestVersion())
                        {
                            projectRelationshipEntity.setVersion(checkerSetEntity.getVersion());
                            latestRelationships.add(projectRelationshipEntity);
                        }
                    }
                    if (CollectionUtils.isNotEmpty(latestRelationships))
                    {
                        checkerSetProjectRelationshipRepository.save(latestRelationships);

                        // 刷新告警状态并设置强制全量扫描标志
                        Map<Long, Map<String, Integer>> currentTaskCheckerSetMap = Maps.newHashMap();
                        List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities = checkerSetTaskRelationshipRepository.findByCheckerSetId(checkerSetId);
                        if (CollectionUtils.isNotEmpty(taskRelationshipEntities))
                        {
                            for (CheckerSetTaskRelationshipEntity taskRelationshipEntity : taskRelationshipEntities)
                            {
                                currentTaskCheckerSetMap.computeIfAbsent(taskRelationshipEntity.getTaskId(), k -> Maps.newHashMap());
                                currentTaskCheckerSetMap.get(taskRelationshipEntity.getTaskId()).put(taskRelationshipEntity.getCheckerSetId(),
                                        currentCheckerSetVersionMap.get(taskRelationshipEntity.getCheckerSetId()));

                                updateTaskSet.add(taskRelationshipEntity.getTaskId());
                            }
                        }

                        Map<Long, Map<String, Integer>> updatedTaskCheckerSetMap = Maps.newHashMap();
                        updatedTaskCheckerSetMap.putAll(currentTaskCheckerSetMap);

                        for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskCheckerSetMap.entrySet())
                        {
                            if (entry.getValue().get(checkerSetId) != null)
                            {
                                entry.getValue().put(checkerSetId, checkerSetEntity.getVersion());
                            }
                        }

                        // 对各任务设置强制全量扫描标志，并修改告警状态
                        ThreadPoolUtil.addRunnableTask(() -> setForceFullScanAndUpdateDefectAndToolStatus(currentTaskCheckerSetMap, updatedTaskCheckerSetMap, user));
                    }
                }
            }
        }
    }

    /**
     * 查询规则集列表
     *
     * @param projectId
     * @param queryCheckerSetReq
     * @return
     */
    @Override
    public Page<CheckerSetVO> getOtherCheckerSets(String projectId, OtherCheckerSetListQueryReq queryCheckerSetReq)
    {
        if (null == queryCheckerSetReq.getSortType())
        {
            queryCheckerSetReq.setSortType(Sort.Direction.DESC);
        }

        if (StringUtils.isEmpty(queryCheckerSetReq.getSortField()))
        {
            queryCheckerSetReq.setSortField("task_usage");
        }
        int pageNum = queryCheckerSetReq.getPageNum() - 1 < 0 ? 0 : queryCheckerSetReq.getPageNum() - 1;
        int pageSize = queryCheckerSetReq.getPageSize() <= 0 ? 10 : queryCheckerSetReq.getPageSize();
        Pageable pageable = new PageRequest(pageNum, pageSize, new Sort(queryCheckerSetReq.getSortType(), queryCheckerSetReq.getSortField()));

        // 先查出项目已安装的规则集列表
        Set<String> projectCheckerSetIds = Sets.newHashSet();
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities = checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Map<String, Boolean> defaultCheckerSetMap;
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities))
        {
            defaultCheckerSetMap = projectRelationshipEntities.stream().collect(Collector.of(HashMap::new, (k, v) ->
                k.put(v.getCheckerSetId(), v.getDefaultCheckerSet()), (k, v) -> v, Collector.Characteristics.IDENTITY_FINISH
            ));
            for (CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity : projectRelationshipEntities)
            {
                projectCheckerSetIds.add(checkerSetProjectRelationshipEntity.getCheckerSetId());
            }
        }
        else
        {
            defaultCheckerSetMap = new HashMap<>();
        }

        List<CheckerSetEntity> checkerSetEntities = checkerSetDao.findMoreByCondition(queryCheckerSetReq.getQuickSearch(),
                queryCheckerSetReq.getCheckerSetLanguage(), queryCheckerSetReq.getCheckerSetCategory(), projectCheckerSetIds,
                queryCheckerSetReq.getProjectInstalled(), pageable);

        if (CollectionUtils.isEmpty(checkerSetEntities))
        {
            return new PageImpl<>(Lists.newArrayList(), pageable, 0);
        }

        List<CheckerSetVO> result = checkerSetEntities.stream().map(checkerSetEntity ->
        {
            CheckerSetVO checkerSetVO = new CheckerSetVO();
            BeanUtils.copyProperties(checkerSetEntity, checkerSetVO, "checkerProps");
            checkerSetVO.setCodeLangList(List2StrUtil.fromString(checkerSetEntity.getCheckerSetLang(), ","));
            checkerSetVO.setToolList(Sets.newHashSet());
            if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
            {
                for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps())
                {
                    checkerSetVO.getToolList().add(checkerPropsEntity.getToolName());
                }
            }
            int checkerCount = checkerSetEntity.getCheckerProps() != null ? checkerSetEntity.getCheckerProps().size() : 0;
            checkerSetVO.setCheckerCount(checkerCount);
            if (CheckerSetSource.DEFAULT.name().equals(checkerSetEntity.getCheckerSetSource())
                    || CheckerSetSource.RECOMMEND.name().equals(checkerSetEntity.getCheckerSetSource())
                    || projectCheckerSetIds.contains(checkerSetEntity.getCheckerSetId()))
            {
                checkerSetVO.setProjectInstalled(true);
            }
            else
            {
                checkerSetVO.setProjectInstalled(false);
            }
            //设置默认标签
            checkerSetVO.setDefaultCheckerSet((CheckerSetSource.DEFAULT.name().
                equals(checkerSetVO.getCheckerSetSource()) && null == defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId()) ||
                (null != defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId()) && defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId()))));
            return checkerSetVO;
        }).collect(Collectors.toList());

        long total = pageNum * pageSize + result.size() + 1;

        //封装分页类
        return new PageImpl<>(result, pageable, total);
    }

    /**
     * 查询规则集列表
     *
     * @param queryCheckerSetReq
     * @return
     */
    @Override
    public List<CheckerSetVO> getCheckerSetsOfProject(CheckerSetListQueryReq queryCheckerSetReq)
    {
        String projectId = queryCheckerSetReq.getProjectId();
        List<CheckerSetProjectRelationshipEntity> checkerSetRelationshipRepositoryList = checkerSetProjectRelationshipRepository.
                findByProjectId(projectId);
        Set<String> checkerSetIds;
        Map<String, Integer> checkerSetVersionMap = Maps.newHashMap();
        Map<String, Boolean> checkerSetDefaultMap = Maps.newHashMap();
        Set<String> latestVersionCheckerSets = Sets.newHashSet();
        if (CollectionUtils.isEmpty(checkerSetRelationshipRepositoryList))
        {
            checkerSetIds = new HashSet<>();
        }
        else
        {
            checkerSetIds = checkerSetRelationshipRepositoryList.stream().
                    map(CheckerSetProjectRelationshipEntity::getCheckerSetId).
                    collect(Collectors.toSet());
            for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : checkerSetRelationshipRepositoryList)
            {
                checkerSetVersionMap.put(projectRelationshipEntity.getCheckerSetId(), projectRelationshipEntity.getVersion());
                checkerSetDefaultMap.put(projectRelationshipEntity.getCheckerSetId(), projectRelationshipEntity.getDefaultCheckerSet());
                if (projectRelationshipEntity.getUselatestVersion() != null && projectRelationshipEntity.getUselatestVersion())
                {
                    latestVersionCheckerSets.add(projectRelationshipEntity.getCheckerSetId());
                }
            }
        }
        List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(queryCheckerSetReq.getKeyWord(),
                checkerSetIds, queryCheckerSetReq.getCheckerSetLanguage(), queryCheckerSetReq.getCheckerSetCategory(),
                queryCheckerSetReq.getToolName(), queryCheckerSetReq.getCheckerSetSource(), queryCheckerSetReq.getCreator(), true,
                null, true);

        if (CollectionUtils.isEmpty(checkerSetEntityList))
        {
            return new ArrayList<>();
        }

        // 查询使用量
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList = checkerSetTaskRelationshipRepository.findByProjectId(projectId);
        Map<String, Long> checkerSetCountMap = checkerSetTaskRelationshipEntityList.stream().filter(checkerSetTaskRelationshipEntity ->
                StringUtils.isNotBlank(checkerSetTaskRelationshipEntity.getCheckerSetId())).
                collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId, Collectors.counting()));

        //按任务使用量排序
        if (CheckerConstants.CheckerSetSortField.TASK_USAGE.name().equalsIgnoreCase(queryCheckerSetReq.getSortField()))
        {

            //要去除版本信息，设置到versionList字段里面
            return checkerSetEntityList.stream().filter(checkerSetEntity ->
                    judgeQualifiedCheckerSet(null, null, null, queryCheckerSetReq.getCheckerSetSource(), checkerSetEntity)).
                    collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream().
                    map(entry -> getCheckerSetVO(entry, checkerSetVersionMap, checkerSetDefaultMap, checkerSetCountMap, latestVersionCheckerSets)).
                    sorted(Comparator.comparingLong(o -> sortByOfficialProps(o) + (checkerSetCountMap.containsKey(o.getCheckerSetId()) ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L))).
                    collect(Collectors.toList());
        }
        //按创建时间倒序(默认)
        else
        {
            if (StringUtils.isEmpty(queryCheckerSetReq.getSortType()))
            {
                queryCheckerSetReq.setSortType(Sort.Direction.DESC.name());
            }
            Long coefficient = queryCheckerSetReq.getSortType().equals(Sort.Direction.ASC.name()) ? 1L : -1L;
            return checkerSetEntityList.stream().filter(checkerSetEntity ->
                    judgeQualifiedCheckerSet(null, null, null, queryCheckerSetReq.getCheckerSetSource(), checkerSetEntity)).
                    collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream().
                    map(entry -> getCheckerSetVO(entry, checkerSetVersionMap, checkerSetDefaultMap, checkerSetCountMap, latestVersionCheckerSets)).
                    sorted(Comparator.comparingLong(o -> sortByOfficialProps(o) + coefficient * o.getCreateTime())).collect(Collectors.toList());
        }
    }


    private Long sortByOfficialProps(CheckerSetVO checkerSetVO)
    {
        Long sortNum = (long) Integer.MAX_VALUE;
        if (null != checkerSetVO.getDefaultCheckerSet() && checkerSetVO.getDefaultCheckerSet())
        {
            sortNum = sortNum + ((long) Integer.MAX_VALUE * -1000000);
        }
        if (CheckerSetSource.DEFAULT.name().equals(checkerSetVO.getCheckerSetSource()))
        {
            sortNum = sortNum + ((long) Integer.MAX_VALUE * -100000);
        }
        if (CheckerSetSource.RECOMMEND.name().equals(checkerSetVO.getCheckerSetSource()))
        {
            sortNum = sortNum + ((long) Integer.MAX_VALUE * -10000);
        }
        return sortNum;
    }

    @Override
    public Map<String, List<CheckerSetVO>> getAvailableCheckerSetsOfProject(String projectId)
    {
        Map<String, List<CheckerSetVO>> resultCheckerSetMap = new LinkedHashMap<>();
        for (CheckerSetSource checkerSetSource : CheckerSetSource.values())
        {
            resultCheckerSetMap.put(checkerSetSource.getName(), new ArrayList<>());
        }

        // 根据项目ID查询非旧插件规则集
        List<Boolean> legacyList = new ArrayList<>();
        legacyList.add(false);
        legacyList.add(true);
        List<CheckerSetEntity> filteredCheckerSetList = findAvailableCheckerSetsByProject(projectId, legacyList);
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities = checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Map<String, CheckerSetProjectRelationshipEntity> checkerSetRelationshipMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities))
        {
            for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : projectRelationshipEntities)
            {
                checkerSetRelationshipMap.put(projectRelationshipEntity.getCheckerSetId(), projectRelationshipEntity);
            }
        }
        if (CollectionUtils.isNotEmpty(filteredCheckerSetList))
        {
            for (CheckerSetEntity checkerSetEntity : filteredCheckerSetList)
            {
                CheckerSetProjectRelationshipEntity projectRelationshipEntity = checkerSetRelationshipMap.get(checkerSetEntity.getCheckerSetId());
                if ((projectRelationshipEntity != null && null != projectRelationshipEntity.getDefaultCheckerSet()
                        && projectRelationshipEntity.getDefaultCheckerSet()) || (CheckerSetSource.DEFAULT.name().
                        equals(checkerSetEntity.getCheckerSetSource()) && null == projectRelationshipEntity))
                {
                    checkerSetEntity.setDefaultCheckerSet(true);
                }
                else
                {
                    checkerSetEntity.setDefaultCheckerSet(false);
                }
            }
        }


        if (CollectionUtils.isEmpty(filteredCheckerSetList))
        {
            return resultCheckerSetMap;
        }

        //官方优选 官方推荐版本
        Map<String, Integer> officialMap = filteredCheckerSetList.stream().filter(checkerSetEntity ->
                Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetEntity.getCheckerSetSource())).
                collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream().
                collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().max(Comparator.comparingInt(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity()).getVersion()));


        //进行过滤，去掉规则为空、单语言的规则集
        filteredCheckerSetList = filteredCheckerSetList.stream().filter(checkerSetEntity ->
                CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())
                        && (StringUtils.isEmpty(checkerSetEntity.getCheckerSetLang()) || !checkerSetEntity.getCheckerSetLang().contains(ComConstants.STRING_SPLIT))).collect(Collectors.toList());
        // 查询语言参数列表
        CodeCCResult<List<BaseDataVO>> paramsCodeCCResult = client.get(ServiceBaseDataResource.class).getParamsByType(ComConstants.KEY_CODE_LANG);
        if (paramsCodeCCResult.isNotOk() || CollectionUtils.isEmpty(paramsCodeCCResult.getData()))
        {
            log.error("param list is empty! param type: {}", ComConstants.KEY_CODE_LANG);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<BaseDataVO> codeLangParams = paramsCodeCCResult.getData();

        //按使用量排序
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList = checkerSetTaskRelationshipRepository.findByProjectId(projectId);
        Map<String, Long> checkerSetCountMap = checkerSetTaskRelationshipEntityList.stream().
                collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId, Collectors.counting()));
        filteredCheckerSetList.stream().sorted(Comparator.comparingLong(o -> checkerSetCountMap.containsKey(o.getCheckerSetId()) ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L)).
                forEach(checkerSetEntity ->
                {
                    if (CheckerSetSource.DEFAULT.name().equals(checkerSetEntity.getCheckerSetSource()) ||
                            CheckerSetSource.RECOMMEND.name().equals(checkerSetEntity.getCheckerSetSource()))
                    {
                        resultCheckerSetMap.compute(CheckerSetSource.valueOf(checkerSetEntity.getCheckerSetSource()).getName(), (k, v) ->
                                {
                                    if (null == v)
                                    {
                                        return new ArrayList<>();
                                    }
                                    else
                                    {
                                        if (!checkerSetEntity.getVersion().equals(officialMap.get(checkerSetEntity.getCheckerSetId())))
                                        {
                                            return v;
                                        }
                                        v.add(handleCheckerSetForCateList(checkerSetEntity, codeLangParams));
                                        return v;
                                    }
                                }
                        );
                    }
                    else
                    {
                        resultCheckerSetMap.compute(CheckerSetSource.SELF_DEFINED.getName(), (k, v) ->
                        {
                            if (null == v)
                            {
                                return new ArrayList<>();
                            }
                            else
                            {
                                v.add(handleCheckerSetForCateList(checkerSetEntity, codeLangParams));
                                return v;
                            }
                        });
                    }
                });
        return resultCheckerSetMap;
    }


    private CheckerSetVO handleCheckerSetForCateList(CheckerSetEntity checkerSetEntity, List<BaseDataVO> codeLangParams)
    {
        CheckerSetVO checkerSetVO = new CheckerSetVO();
        BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
        if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
        {
            checkerSetVO.setToolList(checkerSetEntity.getCheckerProps().stream().
                    map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
            checkerSetVO.setCheckerCount(checkerSetEntity.getCheckerProps().size());
        }
        List<String> codeLangs = Lists.newArrayList();
        for (BaseDataVO codeLangParam : codeLangParams)
        {
            int paramCodeInt = Integer.valueOf(codeLangParam.getParamCode());
            if (null != checkerSetVO.getCodeLang() && (checkerSetVO.getCodeLang() & paramCodeInt) != 0)
            {
                // 蓝盾流水线使用的是语言别名的第一个值作为语言的ID来匹配的
                codeLangs.add(new JSONArray(codeLangParam.getParamExtend2()).getString(0));
            }
        }
        checkerSetVO.setCodeLangList(codeLangs);

        return checkerSetVO;
    }

    private List<String> getCodelangs(long codeLang, List<BaseDataVO> codeLangParams)
    {
        List<String> codeLangs = Lists.newArrayList();
        for (BaseDataVO codeLangParam : codeLangParams)
        {
            int paramCodeInt = Integer.valueOf(codeLangParam.getParamCode());
            if ((codeLang & paramCodeInt) != 0)
            {
                codeLangs.add(codeLangParam.getParamName());
            }
        }
        return codeLangs;
    }

    private CheckerSetVO getCheckerSetVO(Map.Entry<String, List<CheckerSetEntity>> entry, Map<String, Integer> versionMap, Map<String, Boolean> defaultMap,
                                         Map<String, Long> checkerSetCountMap, Set<String> latestVersionCheckerSets)
    {
        List<CheckerSetEntity> checkerSetEntities = entry.getValue();

        CheckerSetVO checkerSetVO = new CheckerSetVO();
        checkerSetVO.setToolList(Sets.newHashSet());
        CheckerSetEntity selectedCheckerSet = checkerSetEntities.stream().
                filter(checkerSetEntity ->
                {
                    if (null != versionMap.get(checkerSetEntity.getCheckerSetId()))
                    {
                        return checkerSetEntity.getVersion().equals
                                (versionMap.get(checkerSetEntity.getCheckerSetId()));
                    }
                    else
                    {
                        return true;
                    }

                }).
                max(Comparator.comparingInt(CheckerSetEntity::getVersion)).
                orElse(new CheckerSetEntity());
        BeanUtils.copyProperties(selectedCheckerSet, checkerSetVO);

        // 加入工具列表
        if (CollectionUtils.isNotEmpty(selectedCheckerSet.getCheckerProps()))
        {
            for (CheckerPropsEntity checkerPropsEntity : selectedCheckerSet.getCheckerProps())
            {
                checkerSetVO.getToolList().add(checkerPropsEntity.getToolName());
            }
        }

        List<CheckerSetVersionVO> versionList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(checkerSetEntities))
        {
            // 加入latest
            CheckerSetVersionVO latestCheckerSetVersionVO = new CheckerSetVersionVO();
            latestCheckerSetVersionVO.setVersion(Integer.MAX_VALUE);
            latestCheckerSetVersionVO.setDisplayName("latest");
            versionList.add(latestCheckerSetVersionVO);

            for (CheckerSetEntity checkerSetEntity : checkerSetEntities)
            {
                CheckerSetVersionVO checkerSetVersionVO = new CheckerSetVersionVO();
                checkerSetVersionVO.setVersion(checkerSetEntity.getVersion());
                checkerSetVersionVO.setDisplayName("V" + checkerSetEntity.getVersion());
                versionList.add(checkerSetVersionVO);
            }
        }
        versionList.sort(((o1, o2) -> o2.getVersion().compareTo(o1.getVersion())));
        checkerSetVO.setVersionList(versionList);
        checkerSetVO.setTaskUsage(checkerSetCountMap.get(checkerSetVO.getCheckerSetId()) == null ? 0 : checkerSetCountMap.get(checkerSetVO.getCheckerSetId()).intValue());

        // 加入语言显示名称
        checkerSetVO.setCodeLangList(List2StrUtil.fromString(selectedCheckerSet.getCheckerSetLang(), ","));

        // 如果选择了latest，或者是默认的规则集，则传入整数最大值对应版本列表中的latest
        if (latestVersionCheckerSets.contains(selectedCheckerSet.getCheckerSetId()) ||
                (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetVO.getCheckerSetSource()) &&
                        null == defaultMap.get(checkerSetVO.getCheckerSetId())))
        {
            checkerSetVO.setVersion(Integer.MAX_VALUE);
        }
        int checkerCount = selectedCheckerSet.getCheckerProps() != null ? selectedCheckerSet.getCheckerProps().size() : 0;
        checkerSetVO.setCheckerCount(checkerCount);
        if (null == checkerSetVO.getCreateTime())
        {
            checkerSetVO.setCreateTime(0L);
        }
        //加入是否默认
        checkerSetVO.setDefaultCheckerSet((CheckerSetSource.DEFAULT.name().equals(checkerSetVO.getCheckerSetSource()) &&
                null == defaultMap.get(checkerSetVO.getCheckerSetId())) || (null != defaultMap.get(checkerSetVO.getCheckerSetId()) && defaultMap.get(checkerSetVO.getCheckerSetId())));
        return checkerSetVO;
    }

    /**
     * 查询规则集列表
     * 对于服务创建的任务，有可能存在规则集迁移自动生成的多语言规则，此处查询逻辑如下：
     * 1、展示适合项目语言的新规则集
     * 2、展示适合项目语言的单语言的老规则集
     * 3、如果有多语言的老规则集，且已经被迁移脚本开启了，则也需要进行展示。用户关闭后则不再展示。
     * 4、多语言的老规则集只能关闭，不能再打开，需做下限制
     *
     * @param queryCheckerSetReq
     * @return
     */
    @Override
    public List<CheckerSetVO> getCheckerSetsOfTask(CheckerSetListQueryReq queryCheckerSetReq)
    {
        String projectId = queryCheckerSetReq.getProjectId();
        Long taskId = queryCheckerSetReq.getTaskId();
        List<CheckerSetProjectRelationshipEntity> taskRelationships = checkerSetProjectRelationshipRepository.findByProjectId(projectId);

        if (CollectionUtils.isEmpty(taskRelationships))
        {
            return new ArrayList<>();
        }
        Map<String, Boolean> defaultCheckerSetMap = taskRelationships.stream().collect(Collector.of(HashMap::new, (k, v) ->
                k.put(v.getCheckerSetId(), v.getDefaultCheckerSet()), (k, v) -> v, Collector.Characteristics.IDENTITY_FINISH
        ));
        //查出项目纬度的id集合
        Set<String> projCheckerSetIds = taskRelationships.stream().
                map(CheckerSetProjectRelationshipEntity::getCheckerSetId).
                collect(Collectors.toSet());
        Map<String, Integer> checkerSetVersionMap = taskRelationships.stream().
                collect(HashMap::new, (m, v) -> m.put(v.getCheckerSetId(), v.getVersion()), HashMap::putAll);

        //查出任务维度的id集合
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList = checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        CodeCCResult<TaskDetailVO> taskDetailVOCodeCCResult = client.get(ServiceTaskRestResource.class).getTaskInfoWithoutToolsByTaskId(taskId);
        Long codeLang;
        if (taskDetailVOCodeCCResult.isNotOk() || taskDetailVOCodeCCResult.getData() == null)
        {
            log.error("task info empty! task id: {}", taskId);
            codeLang = 0L;
        }
        else
        {
            codeLang = taskDetailVOCodeCCResult.getData().getCodeLang();
        }
        Set<String> taskCheckerSetIds = checkerSetTaskRelationshipEntityList.stream().
                map(CheckerSetTaskRelationshipEntity::getCheckerSetId).
                collect(Collectors.toSet());
        //查出项目纬度下的规则集
        List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(queryCheckerSetReq.getKeyWord(),
                projCheckerSetIds, queryCheckerSetReq.getCheckerSetLanguage(), queryCheckerSetReq.getCheckerSetCategory(),
                queryCheckerSetReq.getToolName(), queryCheckerSetReq.getCheckerSetSource(), queryCheckerSetReq.getCreator(), true,
                null, true);
        //官方优选 官方推荐版本
        Map<String, Integer> officialMap = checkerSetEntityList.stream().filter(checkerSetEntity ->
                !projCheckerSetIds.contains(checkerSetEntity.getCheckerSetId()) &&
                        Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetEntity.getCheckerSetSource())).
                collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream().
                collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().max(Comparator.comparingInt(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity()).getVersion()));

//        // 任务是否是老插件
//        boolean isLegacyTask = false;
//        //如果是流水线创建的
//        if (taskDetailVOResult.getData() != null && ComConstants.BsTaskCreateFrom.BS_PIPELINE.value().
//                equals(taskDetailVOResult.getData().getCreateFrom()) && StringUtils.isEmpty(taskDetailVOResult.getData().getAtomCode()))
//        {
//            isLegacyTask = true;
//        }
        List<CheckerSetVO> result = Lists.newArrayList();
        List<CheckerSetVO> taskCheckerSets = Lists.newArrayList();
        List<CheckerSetVO> otherCheckerSets = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(checkerSetEntityList))
        {
            for (CheckerSetEntity checkerSetEntity : checkerSetEntityList)
            {
                //对应版本号
                if (null != checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId()) && !checkerSetEntity.getVersion().equals(checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId())))
                {
                    continue;
                }
                else if (null != officialMap.get(checkerSetEntity.getCheckerSetId()) && !checkerSetEntity.getVersion().equals(officialMap.get(checkerSetEntity.getCheckerSetId())))
                {
                    continue;
                }

                //如果是老规则集，且没有被该任务使用，且是多语言规则集，那么就不展示
                if (checkerSetEntity.getLegacy() != null && checkerSetEntity.getLegacy()
                        && !taskCheckerSetIds.contains(checkerSetEntity.getCheckerSetId())
                        && StringUtils.isNotEmpty(checkerSetEntity.getCheckerSetLang()) && checkerSetEntity.getCheckerSetLang().contains(ComConstants.STRING_SPLIT))
                {
                    continue;
                }
//                if(!isLegacyTask && isLegacyCheckerSet)
//                {
//                    continue;
//                }

                //规则数为空的不显示
                if (CollectionUtils.isEmpty(checkerSetEntity.getCheckerProps()))
                {
                    continue;
                }
                CheckerSetVO checkerSetVO = new CheckerSetVO();
                BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                checkerSetVO.setToolList(Sets.newHashSet());
                if ((codeLang & checkerSetEntity.getCodeLang()) > 0L)
                {
                    if (taskCheckerSetIds.contains(checkerSetVO.getCheckerSetId()))
                    {
                        checkerSetVO.setTaskUsing(true);
                        taskCheckerSets.add(checkerSetVO);
                    }
                    else
                    {
                        checkerSetVO.setTaskUsing(false);
                        otherCheckerSets.add(checkerSetVO);
                    }
                }

                // 加工具列表
                if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                {
                    for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps())
                    {
                        checkerSetVO.getToolList().add(checkerPropsEntity.getToolName());
                    }
                }

                // 加语言显示名称
                checkerSetVO.setCodeLangList(List2StrUtil.fromString(checkerSetEntity.getCheckerSetLang(), ","));


                //设置默认标签
                checkerSetVO.setDefaultCheckerSet((CheckerSetSource.DEFAULT.name().
                        equals(checkerSetVO.getCheckerSetSource()) && null == defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId()) ||
                        (null != defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId()) && defaultCheckerSetMap.get(checkerSetVO.getCheckerSetId()))));
            }

            // 任务使用的规则在前，未使用的规则在后，然后再按创建时间倒序
            if (CollectionUtils.isNotEmpty(taskCheckerSets))
            {
                taskCheckerSets.sort(Comparator.comparingLong(o -> sortByOfficialProps(o) - o.getCreateTime()));
                result.addAll(taskCheckerSets);
            }
            if (CollectionUtils.isNotEmpty(otherCheckerSets))
            {
                otherCheckerSets.sort(Comparator.comparingLong(o -> sortByOfficialProps(o) - o.getCreateTime()));
                result.addAll(otherCheckerSets);
            }
        }

        return result;
    }

    /**
     * 查询规则集参数
     *
     * @param projectId
     * @return
     */
    @Override
    public CheckerSetParamsVO getParams(String projectId)
    {
        // 查询规则集类型列表
        CheckerSetParamsVO checkerSetParams = new CheckerSetParamsVO();
        checkerSetParams.setCatatories(Lists.newArrayList());
        for (CheckerSetCategory checkerSetCategory : CheckerSetCategory.values())
        {
            CheckerSetCategoryVO categoryVO = new CheckerSetCategoryVO();
            categoryVO.setCnName(checkerSetCategory.getName());
            categoryVO.setEnName(checkerSetCategory.name());
            checkerSetParams.getCatatories().add(categoryVO);
        }

        // 查询规则集语言列表
        CodeCCResult<List<BaseDataVO>> langsParamsCodeCCResult = client.get(ServiceBaseDataResource.class).getParamsByType(KEY_LANG);
        if (langsParamsCodeCCResult.isNotOk() || CollectionUtils.isEmpty(langsParamsCodeCCResult.getData()))
        {
            log.error("checker set langs is empty!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        checkerSetParams.setCodeLangs(Lists.newArrayList());
        for (BaseDataVO baseDataVO : langsParamsCodeCCResult.getData())
        {
            CheckerSetCodeLangVO checkerSetCodeLangVO = new CheckerSetCodeLangVO();
            checkerSetCodeLangVO.setCodeLang(Integer.valueOf(baseDataVO.getParamCode()));
            checkerSetCodeLangVO.setDisplayName(baseDataVO.getParamName());
            checkerSetParams.getCodeLangs().add(checkerSetCodeLangVO);
        }

        // 查询项目下的规则集列表
        CheckerSetListQueryReq queryCheckerSetReq = new CheckerSetListQueryReq();
        queryCheckerSetReq.setProjectId(projectId);
        queryCheckerSetReq.setSortField(CheckerConstants.CheckerSetSortField.TASK_USAGE.value());
        queryCheckerSetReq.setSortType(Sort.Direction.DESC.name());
        List<CheckerSetVO> checkerSetVOS = getCheckerSetsOfProject(queryCheckerSetReq);
        checkerSetParams.setCheckerSets(checkerSetVOS);

        return checkerSetParams;
    }

    /**
     * 规则集ID
     *
     * @param checkerSetId
     */
    @Override
    public CheckerSetVO getCheckerSetDetail(String checkerSetId, int version)
    {
        CheckerSetEntity selectedCheckerSetEntity = null;
        if (version == Integer.MAX_VALUE)
        {
            List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetIdIn(Sets.newHashSet(checkerSetId));
            if (CollectionUtils.isNotEmpty(checkerSetEntities))
            {
                int latestVersion = CheckerConstants.DEFAULT_VERSION;
                selectedCheckerSetEntity = checkerSetEntities.get(0);
                for (CheckerSetEntity checkerSetEntity : checkerSetEntities)
                {
                    if (checkerSetEntity.getVersion() > latestVersion)
                    {
                        selectedCheckerSetEntity = checkerSetEntity;
                    }
                }
            }
        }
        else
        {
            selectedCheckerSetEntity = checkerSetRepository.findByCheckerSetIdAndVersion(checkerSetId, version);
        }
        CheckerSetVO checkerSetVO = new CheckerSetVO();
        if (selectedCheckerSetEntity != null)
        {
            BeanUtils.copyProperties(selectedCheckerSetEntity, checkerSetVO);
            checkerSetVO.setCodeLangList(List2StrUtil.fromString(selectedCheckerSetEntity.getCheckerSetLang(), ","));
        }

        // 加入工具列表
        Set<String> toolNames = Sets.newHashSet();
        if (selectedCheckerSetEntity != null)
        {
            if (CollectionUtils.isNotEmpty(selectedCheckerSetEntity.getCheckerProps()))
            {
                for (CheckerPropsEntity checkerPropsEntity : selectedCheckerSetEntity.getCheckerProps())
                {
                    toolNames.add(checkerPropsEntity.getToolName());
                }
            }
        }
        checkerSetVO.setToolList(toolNames);

        return checkerSetVO;
    }

    /**
     * 修改规则集基础信息
     *
     * @param checkerSetId
     * @param updateCheckerSetReq
     */
    @Override
    public void updateCheckerSetBaseInfo(String checkerSetId, String projectId, V3UpdateCheckerSetReqVO updateCheckerSetReq)
    {
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isNotEmpty(checkerSetEntities))
        {
            List<CheckerSetCatagoryEntity> catagoryEntities = getCatagoryEntities(updateCheckerSetReq.getCatagories());
            for (CheckerSetEntity checkerSetEntity : checkerSetEntities)
            {
                if (!projectId.equals(checkerSetEntity.getProjectId()))
                {
                    String errMsg = "不能修改其他项目的规则集！";
                    log.error(errMsg);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
                }
                checkerSetEntity.setCheckerSetName(updateCheckerSetReq.getCheckerSetName());
                checkerSetEntity.setDescription(updateCheckerSetReq.getDescription());
                checkerSetEntity.setCatagories(catagoryEntities);
            }
            checkerSetRepository.save(checkerSetEntities);
        }
    }

    /**
     * 规则集关联到项目或任务
     *
     * @param checkerSetId
     * @param checkerSetRelationshipVO
     */
    @Override
    public void setRelationships(String checkerSetId, String user, CheckerSetRelationshipVO checkerSetRelationshipVO)
    {
        CheckerSetProjectRelationshipEntity projectRelationshipEntity = null;
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities = checkerSetProjectRelationshipRepository.findByProjectId(checkerSetRelationshipVO.getProjectId());
        Map<String, Integer> checkerSetVersionMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities))
        {
            for (CheckerSetProjectRelationshipEntity relationshipEntity : projectRelationshipEntities)
            {
                if (relationshipEntity.getCheckerSetId().equals(checkerSetId))
                {
                    projectRelationshipEntity = relationshipEntity;
                }
                checkerSetVersionMap.put(relationshipEntity.getCheckerSetId(), relationshipEntity.getVersion());
            }
        }
        if (CheckerConstants.CheckerSetRelationshipType.PROJECT.name().equals(checkerSetRelationshipVO.getType()))
        {
            if (projectRelationshipEntity != null)
            {
                String errMsg = "关联已存在！";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            CheckerSetProjectRelationshipEntity newProjectRelationshipEntity = new CheckerSetProjectRelationshipEntity();
            newProjectRelationshipEntity.setCheckerSetId(checkerSetId);
            newProjectRelationshipEntity.setProjectId(checkerSetRelationshipVO.getProjectId());
            newProjectRelationshipEntity.setUselatestVersion(true);
            newProjectRelationshipEntity.setDefaultCheckerSet(false);
            if (checkerSetRelationshipVO.getVersion() == null)
            {
                Map<String, Integer> latestVersionMap = getLatestVersionMap(Sets.newHashSet(checkerSetId));
                newProjectRelationshipEntity.setVersion(latestVersionMap.get(checkerSetId));
            }
            else
            {
                newProjectRelationshipEntity.setVersion(checkerSetRelationshipVO.getVersion());
            }
            checkerSetProjectRelationshipRepository.save(newProjectRelationshipEntity);
        }
        else if (CheckerConstants.CheckerSetRelationshipType.TASK.name().equals(checkerSetRelationshipVO.getType()))
        {
            if (projectRelationshipEntity == null)
            {
                List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
                if (CollectionUtils.isNotEmpty(checkerSetEntities))
                {
                    CheckerSetEntity latestVersionCheckerSet = checkerSetEntities.get(0);
                    for (CheckerSetEntity checkerSetEntity : checkerSetEntities)
                    {
                        if (checkerSetEntity.getVersion() > latestVersionCheckerSet.getVersion())
                        {
                            latestVersionCheckerSet = checkerSetEntity;
                        }
                    }
                    if (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(latestVersionCheckerSet.getCheckerSetSource()))
                    {
                        projectRelationshipEntity = new CheckerSetProjectRelationshipEntity();
                        projectRelationshipEntity.setCheckerSetId(checkerSetId);
                        projectRelationshipEntity.setProjectId(checkerSetRelationshipVO.getProjectId());
                        projectRelationshipEntity.setUselatestVersion(true);
                        //默认是默认规则集
                        if (CheckerSetSource.DEFAULT.name().equals(latestVersionCheckerSet.getCheckerSetSource()))
                        {
                            projectRelationshipEntity.setDefaultCheckerSet(true);
                        }
                        else
                        {
                            projectRelationshipEntity.setDefaultCheckerSet(false);
                        }
                        projectRelationshipEntity.setVersion(latestVersionCheckerSet.getVersion());
                        checkerSetVersionMap.put(checkerSetId, latestVersionCheckerSet.getVersion());
                        checkerSetProjectRelationshipRepository.save(projectRelationshipEntity);
                    }
                }
                if (projectRelationshipEntity == null)
                {
                    String errMsg = "规则集没有安装到项目！";
                    log.error(errMsg);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
                }
            }

            CheckerSetTaskRelationshipEntity taskRelationshipEntity = null;
            Map<Long, Map<String, Integer>> currentTaskCheckerSetMap = Maps.newHashMap();
            List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities = checkerSetTaskRelationshipRepository.findByTaskId(checkerSetRelationshipVO.getTaskId());
            if (CollectionUtils.isNotEmpty(taskRelationshipEntities))
            {
                for (CheckerSetTaskRelationshipEntity relationshipEntity : taskRelationshipEntities)
                {
                    if (relationshipEntity.getCheckerSetId().equals(checkerSetId))
                    {
                        taskRelationshipEntity = relationshipEntity;
                    }
                    currentTaskCheckerSetMap.computeIfAbsent(relationshipEntity.getTaskId(), k -> Maps.newHashMap());
                    currentTaskCheckerSetMap.get(relationshipEntity.getTaskId()).put(relationshipEntity.getCheckerSetId(), checkerSetVersionMap.get(relationshipEntity.getCheckerSetId()));
                }
            }
            if (taskRelationshipEntity != null)
            {
                String errMsg = "关联已存在！";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            CheckerSetTaskRelationshipEntity newTaskRelationshipEntity = new CheckerSetTaskRelationshipEntity();
            newTaskRelationshipEntity.setCheckerSetId(checkerSetId);
            newTaskRelationshipEntity.setProjectId(checkerSetRelationshipVO.getProjectId());
            newTaskRelationshipEntity.setTaskId(checkerSetRelationshipVO.getTaskId());
            checkerSetTaskRelationshipRepository.save(newTaskRelationshipEntity);

            // 任务关联规则集需要设置全量扫描
            CheckerSetEntity checkerSetEntity = checkerSetRepository.findByCheckerSetIdAndVersion(checkerSetId, projectRelationshipEntity.getVersion());
            if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
            {
                Set<String> toolSet = Sets.newHashSet();
                for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps())
                {
                    toolSet.add(checkerPropsEntity.getToolName());
                }
                toolBuildInfoService.setForceFullScan(checkerSetRelationshipVO.getTaskId(), Lists.newArrayList(toolSet));
            }

            // 设置强制全量扫描标志并刷新告警状态
            Map<Long, Map<String, Integer>> updatedTaskCheckerSetMap;
            try
            {
                updatedTaskCheckerSetMap = Maps.newHashMap();
                updatedTaskCheckerSetMap.put(checkerSetRelationshipVO.getTaskId(), CloneUtils.cloneObject(currentTaskCheckerSetMap.get(checkerSetRelationshipVO.getTaskId())));
                if (null != updatedTaskCheckerSetMap.get(checkerSetRelationshipVO.getTaskId()))
                {
                    updatedTaskCheckerSetMap.get(checkerSetRelationshipVO.getTaskId()).put(checkerSetId, checkerSetVersionMap.get(checkerSetId));
                }

                // 对各任务设置强制全量扫描标志，并修改告警状态
                Map<Long, Map<String, Integer>> finalUpdatedTaskCheckerSetMap = updatedTaskCheckerSetMap;
                ThreadPoolUtil.addRunnableTask(() -> setForceFullScanAndUpdateDefectAndToolStatus(currentTaskCheckerSetMap, finalUpdatedTaskCheckerSetMap, user));
            }
            catch (CloneNotSupportedException e)
            {
                log.error("copy currentTaskCheckerSetMap fail!");
                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
            }
        }
        else
        {
            String errMsg = "关联类型非法！";
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }

    }

    /**
     * 任务批量关联规则集
     *
     * @param projectId
     * @param taskId
     * @param checkerSetList
     * @param user
     * @return
     */
    @Override
    public Boolean batchRelateTaskAndCheckerSet(String projectId, Long taskId, List<CheckerSetVO> checkerSetList, String user, Boolean isOpenSource)
    {
        List<CheckerSetEntity> maxCheckerSetEntityList = new ArrayList<>();
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntityList = checkerSetProjectRelationshipRepository.findByProjectId(projectId);
        Set<String> projInstallCheckerSets;
        if (CollectionUtils.isEmpty(projectRelationshipEntityList))
        {
            projInstallCheckerSets = new HashSet<>();
        }
        else
        {
            projInstallCheckerSets = projectRelationshipEntityList.stream().map(CheckerSetProjectRelationshipEntity::getCheckerSetId).collect(Collectors.toSet());
        }

        List<CheckerSetTaskRelationshipEntity> existTaskRelationshipEntityList = checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        Map<String, CheckerSetTaskRelationshipEntity> existTaskRelatedCheckerMap = null;
        if (CollectionUtils.isNotEmpty(existTaskRelationshipEntityList))
        {
            existTaskRelatedCheckerMap = existTaskRelationshipEntityList.stream()
                    .collect(Collectors.toMap(CheckerSetTaskRelationshipEntity::getCheckerSetId, Function.identity(), (k, v) -> v));
        }
        List<CheckerSetEntity> checkerSetEntityList = checkerSetRepository.findByCheckerSetIdIn(checkerSetList.stream().map(CheckerSetVO::getCheckerSetId).collect(Collectors.toSet()));

        // 项目还没安装的规则集是非法规则集
        List<String> invalidCheckerSet = new ArrayList<>();

        // 关联规则集需要设置全量扫描的任务工具
        Set<String> toolSet = new HashSet<>();
        List<CheckerSetTaskRelationshipEntity> taskRelationshipEntityList = new ArrayList<>();
        //如果是官方推荐和官方优选的话，还需要关联项目表
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities = new ArrayList<>();
        long currTime = System.currentTimeMillis();
        for (CheckerSetVO checkerSetVO : checkerSetList)
        {
            String checkerSetId = checkerSetVO.getCheckerSetId();
            if (!projInstallCheckerSets.contains(checkerSetId))
            {
                CheckerSetEntity checkerSetEntity = checkerSetEntityList.stream().filter(checkerSetEntity1 -> checkerSetEntity1.getCheckerSetId().equals(checkerSetId)).findFirst().orElse(new CheckerSetEntity());
                //如果是官方的话 需要关联
                if ((null != isOpenSource && isOpenSource) || Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetEntity.getCheckerSetSource())
                        || (checkerSetEntity.getLegacy() != null && checkerSetEntity.getLegacy() && CheckerConstants.CheckerSetOfficial.OFFICIAL.code() == checkerSetEntity.getOfficial()))
                {
                    if (CollectionUtils.isEmpty(maxCheckerSetEntityList))
                    {
                        maxCheckerSetEntityList = checkerSetRepository.findByCheckerSetIdIn(checkerSetList.stream().
                                map(CheckerSetVO::getCheckerSetId).collect(Collectors.toSet()));
                    }
                    CheckerSetEntity maxVersionCheckerSet = maxCheckerSetEntityList.stream().filter(maxCheckerSetEntity -> maxCheckerSetEntity.getCheckerSetId().equals(checkerSetId)).
                            max(Comparator.comparing(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity());
                    //关联项目关联表
                    CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity = new CheckerSetProjectRelationshipEntity();
                    checkerSetProjectRelationshipEntity.setProjectId(projectId);
                    checkerSetProjectRelationshipEntity.setVersion(maxVersionCheckerSet.getVersion());
                    checkerSetProjectRelationshipEntity.setCheckerSetId(checkerSetVO.getCheckerSetId());
                    checkerSetProjectRelationshipEntity.setUselatestVersion(true);
                    if (CheckerSetSource.DEFAULT.name().equals(checkerSetVO.getCheckerSetSource()))
                    {
                        checkerSetProjectRelationshipEntity.setDefaultCheckerSet(true);
                    }
                    else
                    {
                        checkerSetProjectRelationshipEntity.setDefaultCheckerSet(false);
                    }
                    projectRelationshipEntities.add(checkerSetProjectRelationshipEntity);

                    //在关联任务的关联表
                    CheckerSetTaskRelationshipEntity newRelationshipEntity = new CheckerSetTaskRelationshipEntity();
                    newRelationshipEntity.setCheckerSetId(checkerSetId);
                    newRelationshipEntity.setProjectId(projectId);
                    newRelationshipEntity.setTaskId(taskId);
                    newRelationshipEntity.setCreatedBy(user);
                    newRelationshipEntity.setCreatedDate(currTime);
                    taskRelationshipEntityList.add(newRelationshipEntity);
                }
                else
                {
                    invalidCheckerSet.add(checkerSetId);
                }
            }
            // 还没有被任务关联的规则集则创建关联
            else if (MapUtils.isEmpty(existTaskRelatedCheckerMap) || !existTaskRelatedCheckerMap.containsKey(checkerSetId))
            {
                CheckerSetTaskRelationshipEntity newRelationshipEntity = new CheckerSetTaskRelationshipEntity();
                newRelationshipEntity.setCheckerSetId(checkerSetId);
                newRelationshipEntity.setProjectId(projectId);
                newRelationshipEntity.setTaskId(taskId);
                newRelationshipEntity.setCreatedBy(user);
                newRelationshipEntity.setCreatedDate(currTime);
                taskRelationshipEntityList.add(newRelationshipEntity);
                if (CollectionUtils.isNotEmpty(checkerSetVO.getToolList()))
                {
                    toolSet.addAll(checkerSetVO.getToolList());
                }
            }

            // 不在本次关联列表中的都要解除关联
            if (MapUtils.isNotEmpty(existTaskRelatedCheckerMap))
            {
                existTaskRelatedCheckerMap.remove(checkerSetId);
            }
        }

        if (CollectionUtils.isNotEmpty(invalidCheckerSet))
        {
            StringBuffer errMsg = new StringBuffer();
            errMsg.append("项目未安装规则集: ").append(JsonUtil.INSTANCE.toJson(invalidCheckerSet));
            log.error(errMsg.toString());
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg.toString()}, null);
        }

        checkerSetTaskRelationshipRepository.save(taskRelationshipEntityList);

        //保存官方优选和官方推荐
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities))
        {
            checkerSetProjectRelationshipRepository.save(projectRelationshipEntities);
        }

        // 关联规则集需要设置全量扫描
        toolBuildInfoService.setForceFullScan(taskId, Lists.newArrayList(toolSet));

        // 解除规则集关联
        if (MapUtils.isNotEmpty(existTaskRelatedCheckerMap))
        {
            checkerSetTaskRelationshipRepository.delete(existTaskRelatedCheckerMap.values());
        }
        return true;
    }

    @Override
    public void management(String user, String checkerSetId, CheckerSetManagementReqVO checkerSetManagementReqVO)
    {
        //兼容官方推荐官方优选
        CheckerSetProjectRelationshipEntity sourceProjectRelationEntity = null;
        // 校验规则集是否存在
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        if (CollectionUtils.isEmpty(checkerSetEntities))
        {
            String errMsg = "规则集不存在";
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
        }

        CheckerSetEntity firstCheckerSetEntity = checkerSetEntities.get(0);
        if (CheckerSetSource.DEFAULT.name().equals(firstCheckerSetEntity.getCheckerSetSource())
                || CheckerSetSource.RECOMMEND.name().equals(firstCheckerSetEntity.getCheckerSetSource()))
        {
            if (checkerSetManagementReqVO.getUninstallCheckerSet() != null && checkerSetManagementReqVO.getUninstallCheckerSet())
            {
                String errMsg = "官方推荐和官方优选规则集不能进行此项操作";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }

        // 校验设置为公开的规则集名称是否与公共规则集重复
        if (checkerSetManagementReqVO.getScope() != null && checkerSetManagementReqVO.getScope() == CheckerConstants.CheckerSetScope.PUBLIC.code())
        {
            checkNameExistInPublic(firstCheckerSetEntity.getCheckerSetName());
        }

        // 校验用户是否有权限
        if (!authExPermissionApi.authProjectManager(checkerSetManagementReqVO.getProjectId(), user)
                && !firstCheckerSetEntity.getCreator().equals(user))
        {
            String errMsg = "当前用户不是项目管理员或者规则集创建者，无权进行此操作！";
            log.error(errMsg);
            throw new CodeCCException(CommonMessageCode.PERMISSION_DENIED, new String[]{"当前用户" + user}, null);
        }

        // 查询任务关联规则集记录
        List<CheckerSetTaskRelationshipEntity> taskRelationshipEntities = checkerSetTaskRelationshipRepository.findByProjectId(checkerSetManagementReqVO.getProjectId());
        //过滤限制当前规则集id的规则集
        List<CheckerSetTaskRelationshipEntity> selectCheckerSetEntities = new ArrayList<>();
        Map<Long, CheckerSetTaskRelationshipEntity> taskRelationshipEntityMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(taskRelationshipEntities))
        {
            for (CheckerSetTaskRelationshipEntity taskRelationshipEntity : taskRelationshipEntities)
            {
                if (checkerSetId.equalsIgnoreCase(taskRelationshipEntity.getCheckerSetId()))
                {
                    taskRelationshipEntityMap.put(taskRelationshipEntity.getTaskId(), taskRelationshipEntity);
                    selectCheckerSetEntities.add(taskRelationshipEntity);
                }
            }
        }

        /**
         * 1、不允许删除非本项目的规则集，
         * 2、不允许卸载本项目的规则集
         * 3、已在任务中使用的规则集不允许删除或卸载
         */
        if (checkerSetManagementReqVO.getDeleteCheckerSet() != null && checkerSetManagementReqVO.getDeleteCheckerSet())
        {
            if (!checkerSetEntities.get(0).getProjectId().equals(checkerSetManagementReqVO.getProjectId()))
            {
                String errMsg = "不允许删除非本项目的规则集";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);

            }
            if (CollectionUtils.isNotEmpty(selectCheckerSetEntities))
            {
                String errMsg = "该项目下还有任务使用此规则集，不允许删除或卸载";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }
        if (checkerSetManagementReqVO.getUninstallCheckerSet() != null && checkerSetManagementReqVO.getUninstallCheckerSet())
        {
            if (checkerSetEntities.get(0).getProjectId().equals(checkerSetManagementReqVO.getProjectId()))
            {
                String errMsg = "不允许卸载本项目的规则集";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
            if (CollectionUtils.isNotEmpty(selectCheckerSetEntities))
            {
                String errMsg = "该项目下还有任务使用此规则集，不允许删除或卸载";
                log.error(errMsg);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
            }
        }

        // 查询当前项目关联规则集的列表
        List<CheckerSetProjectRelationshipEntity> projectRelationshipEntities = checkerSetProjectRelationshipRepository.findByProjectId(checkerSetManagementReqVO.getProjectId());
        Map<String, CheckerSetProjectRelationshipEntity> projectRelationshipEntityMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(projectRelationshipEntities))
        {
            for (CheckerSetProjectRelationshipEntity projectRelationshipEntity : projectRelationshipEntities)
            {
                projectRelationshipEntityMap.put(projectRelationshipEntity.getCheckerSetId(), projectRelationshipEntity);
            }
        }

        // 获取各任务当前使用的规则集列表
        Map<Long, Map<String, Integer>> currentTaskUseCheckerSetsMap = Maps.newHashMap();
        Map<Long, Map<String, Integer>> updatedTaskUseCheckerSetsMap = Maps.newHashMap();
        for (Map.Entry<Long, CheckerSetTaskRelationshipEntity> entry : taskRelationshipEntityMap.entrySet())
        {
            CheckerSetProjectRelationshipEntity projectRelationshipEntity = projectRelationshipEntityMap.get(entry.getValue().getCheckerSetId());
            currentTaskUseCheckerSetsMap.computeIfAbsent(entry.getKey(), k -> Maps.newHashMap());
            currentTaskUseCheckerSetsMap.get(entry.getKey()).put(projectRelationshipEntity.getCheckerSetId(), projectRelationshipEntity.getVersion());
            taskRelationshipEntities.stream().filter(taskRelationshipEntity -> taskRelationshipEntity.getTaskId().equals(entry.getKey())).
                    forEach(taskRelationshipEntity -> currentTaskUseCheckerSetsMap.get(entry.getKey()).put(taskRelationshipEntity.getCheckerSetId(),
                            projectRelationshipEntityMap.get(taskRelationshipEntity.getCheckerSetId()).getVersion()));

            updatedTaskUseCheckerSetsMap.computeIfAbsent(entry.getKey(), k -> Maps.newHashMap());
            updatedTaskUseCheckerSetsMap.get(entry.getKey()).put(projectRelationshipEntity.getCheckerSetId(), projectRelationshipEntity.getVersion());
            taskRelationshipEntities.stream().filter(taskRelationshipEntity -> taskRelationshipEntity.getTaskId().equals(entry.getKey())).
                    forEach(taskRelationshipEntity -> updatedTaskUseCheckerSetsMap.get(entry.getKey()).put(taskRelationshipEntity.getCheckerSetId(),
                            projectRelationshipEntityMap.get(taskRelationshipEntity.getCheckerSetId()).getVersion()));
        }

        // 用HashMap深拷贝初始化更新后的任务规则集列表,原来putall不是深拷贝。。。
//        Map<Long, Map<String, Integer>> updatedTaskUseCheckerSetsMap = Maps.newHashMap();
//        updatedTaskUseCheckerSetsMap.putAll(currentTaskUseCheckerSetsMap);

        // 设置项目维度的默认规则集
        if (checkerSetManagementReqVO.getDefaultCheckerSet() != null)
        {
            CheckerSetProjectRelationshipEntity projectRelationshipEntity = projectRelationshipEntityMap.get(checkerSetId);
            if (null != projectRelationshipEntity)
            {
                projectRelationshipEntity.setDefaultCheckerSet(checkerSetManagementReqVO.getDefaultCheckerSet());
                checkerSetProjectRelationshipRepository.save(projectRelationshipEntity);
            }
            else
            {
                //兼容官方推荐官方优选
                CheckerSetEntity checkerSetEntity = checkerSetEntities.get(0);
                if (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetEntity.getCheckerSetSource()))
                {
                    sourceProjectRelationEntity = new CheckerSetProjectRelationshipEntity();
                    sourceProjectRelationEntity.setCheckerSetId(checkerSetId);
                    sourceProjectRelationEntity.setProjectId(checkerSetManagementReqVO.getProjectId());
                    sourceProjectRelationEntity.setDefaultCheckerSet(checkerSetManagementReqVO.getDefaultCheckerSet());
                }
            }
        }

        // 规则集的可见范围、是否设为默认都要更新到所有版本
        for (CheckerSetEntity checkerSetEntity : checkerSetEntities)
        {
            if (checkerSetManagementReqVO.getScope() != null)
            {
                checkerSetEntity.setScope(checkerSetManagementReqVO.getScope());
            }

            // 从本项目删除后，规则集需要设置为私有，这样其他没安装的项目就找不到了
            if (checkerSetManagementReqVO.getDeleteCheckerSet() != null && checkerSetManagementReqVO.getDeleteCheckerSet())
            {
                checkerSetEntity.setScope(CheckerConstants.CheckerSetScope.PRIVATE.code());
            }
        }
        checkerSetRepository.save(checkerSetEntities);

        // 从本项目中卸载规则集，或者删除本项目的规则集，都要删除关联数据
        CheckerSetProjectRelationshipEntity relationshipEntity = projectRelationshipEntityMap.get(checkerSetId);
        if ((checkerSetManagementReqVO.getDeleteCheckerSet() != null && checkerSetManagementReqVO.getDeleteCheckerSet())
                || (checkerSetManagementReqVO.getUninstallCheckerSet() != null && checkerSetManagementReqVO.getUninstallCheckerSet()))
        {
            if (relationshipEntity != null)
            {
                checkerSetProjectRelationshipRepository.delete(relationshipEntity);
            }

            // 修改更新后的任务规则集列表
            for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet())
            {
                if (entry.getValue().containsKey(checkerSetId))
                {
                    entry.getValue().remove(checkerSetId);
                }
            }
        }

        // 切换项目关联的规则集版本
        if (checkerSetManagementReqVO.getVersionSwitchTo() != null)
        {
            if (relationshipEntity != null)
            {
                if (checkerSetManagementReqVO.getVersionSwitchTo() == Integer.MAX_VALUE)
                {
                    Map<String, Integer> latestVersionMap = getLatestVersionMap(Sets.newHashSet(checkerSetId));
                    relationshipEntity.setVersion(latestVersionMap.get(checkerSetId));
                    relationshipEntity.setUselatestVersion(true);
                }
                else
                {
                    relationshipEntity.setVersion(checkerSetManagementReqVO.getVersionSwitchTo());
                    relationshipEntity.setUselatestVersion(false);
                }
            }
            else
            {
                CheckerSetEntity checkerSetEntity = checkerSetEntities.get(0);
                if (Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetEntity.getCheckerSetSource()))
                {
                    if (null == sourceProjectRelationEntity)
                    {
                        sourceProjectRelationEntity = new CheckerSetProjectRelationshipEntity();
                    }
                    sourceProjectRelationEntity.setCheckerSetId(checkerSetId);
                    sourceProjectRelationEntity.setProjectId(checkerSetManagementReqVO.getProjectId());
                    if (checkerSetManagementReqVO.getVersionSwitchTo() == Integer.MAX_VALUE)
                    {
                        Map<String, Integer> latestVersionMap = getLatestVersionMap(Sets.newHashSet(checkerSetEntity.getCheckerSetId()));
                        sourceProjectRelationEntity.setVersion(latestVersionMap.get(checkerSetId));
                        sourceProjectRelationEntity.setUselatestVersion(true);
                    }
                    else
                    {
                        sourceProjectRelationEntity.setVersion(checkerSetManagementReqVO.getVersionSwitchTo());
                        sourceProjectRelationEntity.setUselatestVersion(false);
                    }
                    if (null == sourceProjectRelationEntity.getDefaultCheckerSet())
                    {
                        if (CheckerSetSource.DEFAULT.name().equals(checkerSetEntity.getCheckerSetSource()))
                        {
                            sourceProjectRelationEntity.setDefaultCheckerSet(true);
                        }
                        else
                        {
                            sourceProjectRelationEntity.setDefaultCheckerSet(false);
                        }
                    }
                }
            }
            if (relationshipEntity != null)
            {
                checkerSetProjectRelationshipRepository.save(relationshipEntity);
            }

            // 修改更新后的任务规则集列表
            for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet())
            {
                if (entry.getValue().containsKey(checkerSetId))
                {
                    entry.getValue().put(checkerSetId, relationshipEntity.getVersion());
                }
            }
        }

        if (null != sourceProjectRelationEntity)
        {
            checkerSetProjectRelationshipRepository.save(sourceProjectRelationEntity);
        }

        // 任务不再使用该规则集
        if (checkerSetManagementReqVO.getDiscardFromTask() != null)
        {
            CheckerSetTaskRelationshipEntity taskRelationshipEntity = taskRelationshipEntityMap.get(checkerSetManagementReqVO.getDiscardFromTask());
            if (taskRelationshipEntity != null)
            {
                List<CheckerSetTaskRelationshipEntity> currentTaskRelationships = checkerSetTaskRelationshipRepository.findByTaskId(checkerSetManagementReqVO.getDiscardFromTask());
                if (currentTaskRelationships.size() == 1)
                {
                    String errMsg = "任务必须至少使用一个规则集！";
                    log.error(errMsg);
                    throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{errMsg}, null);
                }
                checkerSetTaskRelationshipRepository.delete(taskRelationshipEntity);
            }

            // 修改更新后的任务规则集列表
            Map<String, Integer> taskCheckerSetVersionMap = updatedTaskUseCheckerSetsMap.get(checkerSetManagementReqVO.getDiscardFromTask());
            taskCheckerSetVersionMap.remove(checkerSetId);
        }

        // 对各任务设置强制全量扫描标志，并修改告警状态
        ThreadPoolUtil.addRunnableTask(() -> setForceFullScanAndUpdateDefectAndToolStatus(currentTaskUseCheckerSetsMap, updatedTaskUseCheckerSetsMap, user));
    }

    @Override
    public List<CheckerSetVO> queryCheckerSets(Set<String> checkerSetList, String projectId)
    {
        List<CheckerSetProjectRelationshipEntity> relationshipList = checkerSetProjectRelationshipRepository.findByCheckerSetIdInAndProjectId(checkerSetList, projectId);
        Set<String> relationshipSet;
        if (CollectionUtils.isNotEmpty(relationshipList))
        {
            relationshipSet = relationshipList.stream().map(relationship -> relationship.getCheckerSetId() + "_" + relationship.getVersion())
                    .collect(Collectors.toSet());
        }
        else
        {
            relationshipSet = new HashSet<>();
        }

        List<CheckerSetEntity> checkerSets = checkerSetDao.findByComplexCheckerSetCondition(null,
                checkerSetList, null, null, null, null, null, true,
                null, true);

        //官方优选 官方推荐版本
        Map<String, Integer> officialMap = checkerSets.stream().filter(checkerSetEntity ->
                Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetEntity.getCheckerSetSource()) &&
                        checkerSetList.contains(checkerSetEntity.getCheckerSetId())).
                collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream().
                collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().max(Comparator.comparingInt(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity()).getVersion()));

        if (CollectionUtils.isNotEmpty(checkerSets))
        {
            List<CheckerSetEntity> finalCheckerSet = new ArrayList<>();
            Set<String> finalCheckerSetId = new HashSet<>();
            checkerSets.forEach(checkerSetEntity -> {
                // 优先取关系表的规则集
                if (relationshipSet.contains(checkerSetEntity.getCheckerSetId() + "_" + checkerSetEntity.getVersion())) {
                    finalCheckerSet.add(checkerSetEntity);
                    finalCheckerSetId.add(checkerSetEntity.getCheckerSetId());
                    return;
                }

                // 没在关系表，但在官方推荐的，版本号也对应上的， 也可以取
                if (!finalCheckerSetId.contains(checkerSetEntity.getCheckerSetId()) && checkerSetEntity.getVersion().equals(officialMap.get(checkerSetEntity.getCheckerSetId()))) {
                    finalCheckerSet.add(checkerSetEntity);
                    finalCheckerSetId.add(checkerSetEntity.getCheckerSetId());
                }
            });

            return finalCheckerSet.stream().map(checkerSetEntity ->
                    {
                        CheckerSetVO checkerSetVO = new CheckerSetVO();
                        BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                        if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                        {
                            checkerSetVO.setToolList(checkerSetEntity.getCheckerProps().stream().map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
                        }
                        return checkerSetVO;
                    }).collect(Collectors.toList());
        }

        log.error("project {} has not install checker set: {}", projectId, checkerSetList);
        return null;
    }

    @Override
    public List<CheckerSetVO> getCheckerSetsByTaskId(Long taskId)
    {
        //查出任务维度的id集合
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList = checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        if (CollectionUtils.isEmpty(checkerSetTaskRelationshipEntityList))
        {
            return new ArrayList<>();
        }
        Set<String> checkerSetIds = checkerSetTaskRelationshipEntityList.stream().
                map(CheckerSetTaskRelationshipEntity::getCheckerSetId).
                collect(Collectors.toSet());
        String projectId = checkerSetTaskRelationshipEntityList.get(0).getProjectId();

        List<CheckerSetProjectRelationshipEntity> checherSetProjectRelateEntityList =
                checkerSetProjectRelationshipRepository.findByCheckerSetIdInAndProjectId(checkerSetIds, projectId);
        // 计算规则集的使用量
        Map<String, Long> checkerSetCountMap = checkerSetTaskRelationshipEntityList.stream().
                collect(Collectors.groupingBy(CheckerSetTaskRelationshipEntity::getCheckerSetId, Collectors.counting()));

        Map<String, CheckerSetProjectRelationshipEntity> checkerSetRelationshipMap = checherSetProjectRelateEntityList.stream()
                .collect(Collectors.toMap(CheckerSetProjectRelationshipEntity::getCheckerSetId, Function.identity(), (k, v) -> v));

        List<CheckerSetEntity> checkerSetEntityList = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);

        // 按版本过滤，按使用量排序
        return checkerSetEntityList.stream()
                .filter(checkerSetEntity -> filterCheckerSetEntity(checkerSetEntity, checkerSetRelationshipMap))
                .sorted(Comparator.comparingLong(o -> checkerSetCountMap.containsKey(o.getCheckerSetId()) ? -checkerSetCountMap.get(o.getCheckerSetId()) : 0L))
                .map(checkerSetEntity ->
                {
                    CheckerSetVO checkerSetVO = new CheckerSetVO();
                    BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                    Integer useCount = checkerSetCountMap.get(checkerSetVO.getCheckerSetId()) == null ? 0 : Integer.valueOf(checkerSetCountMap.get(checkerSetVO.getCheckerSetId()).toString());
                    checkerSetVO.setTaskUsage(useCount);
                    if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                    {
                        Set<String> toolList = new HashSet<>();
                        checkerSetVO.setCheckerProps(checkerSetEntity.getCheckerProps().stream()
                                .map(checkerPropsEntity ->
                                {
                                    toolList.add(checkerPropsEntity.getToolName());
                                    CheckerPropVO checkerPropVO = new CheckerPropVO();
                                    BeanUtils.copyProperties(checkerPropsEntity, checkerPropVO);
                                    return checkerPropVO;
                                }).collect(Collectors.toList()));
                        checkerSetVO.setToolList(toolList);
                    }

                    CheckerSetProjectRelationshipEntity projectRelationshipEntity = checkerSetRelationshipMap.get(checkerSetEntity.getCheckerSetId());
                    if ((projectRelationshipEntity != null && null != projectRelationshipEntity.getDefaultCheckerSet()
                            && projectRelationshipEntity.getDefaultCheckerSet()) || (CheckerSetSource.DEFAULT.name().
                            equals(checkerSetEntity.getCheckerSetSource()) && null == projectRelationshipEntity))
                    {
                        checkerSetVO.setDefaultCheckerSet(true);
                    }
                    else
                    {
                        checkerSetVO.setDefaultCheckerSet(false);
                    }

                    return checkerSetVO;
                }).collect(Collectors.toList());
    }

    private boolean filterCheckerSetEntity(CheckerSetEntity checkerSetEntity, Map<String, CheckerSetProjectRelationshipEntity> checkerSetRelationshipMap) {
        if (checkerSetRelationshipMap.get(checkerSetEntity.getCheckerSetId()) == null) {
            return false;
        }
        if (checkerSetEntity.getVersion() == null) {
            return false;
        }
        return checkerSetEntity.getVersion().equals(checkerSetRelationshipMap.get(checkerSetEntity.getCheckerSetId()).getVersion());
    }


    @Override
    public List<CheckerCommonCountVO> queryCheckerSetCountList(CheckerSetListQueryReq checkerSetListQueryReq)
    {
        //1. 语言数量map
        Map<String, Integer> langMap = new HashMap<>();
        List<String> langOrder = Arrays.asList(redisTemplate.opsForValue().get(RedisKeyConstants.KEY_LANG_ORDER).split(","));
        for (String codeLang : langOrder)
        {
            langMap.put(codeLang, 0);
        }
        //2.规则类别数量map
        CheckerSetCategory[] checkerSetCategoryList = CheckerSetCategory.values();
        Map<String, Integer> checkerSetCateMap = new HashMap<>();
        for (CheckerSetCategory checkerSetCategory : checkerSetCategoryList)
        {
            checkerSetCateMap.put(checkerSetCategory.name(), 0);
        }
        //3.工具类别数量map
        Map<String, Integer> toolMap = new HashMap<>();
        List<String> toolOrder = Arrays.asList(redisTemplate.opsForValue().get(RedisKeyConstants.KEY_TOOL_ORDER).split(","));
        for (String tool : toolOrder)
        {
            toolMap.put(tool, 0);
        }
        //4.来源数量筛选
        CheckerSetSource[] checkerSetSources = CheckerSetSource.values();
        Map<String, Integer> sourceMap = new HashMap<>();
        for (CheckerSetSource checkerSetSource : checkerSetSources)
        {
            sourceMap.put(checkerSetSource.name(), 0);
        }
        //5.总数
        List<CheckerSetEntity> totalList = new ArrayList<>();
        List<CheckerCommonCountVO> checkerCommonCountVOList = new ArrayList<>();
        String projectId = checkerSetListQueryReq.getProjectId();
        if (StringUtils.isEmpty(projectId))
        {
            log.error("project id is empty!");
            throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, new String[]{"project id"}, null);
        }
        List<CheckerSetProjectRelationshipEntity> checkerSetRelationshipRepositoryList = checkerSetProjectRelationshipRepository.
                findByProjectId(projectId);
        Set<String> checkerSetIds;
        if (CollectionUtils.isNotEmpty(checkerSetRelationshipRepositoryList))
        {
            checkerSetIds = checkerSetRelationshipRepositoryList.stream().map(CheckerSetProjectRelationshipEntity::getCheckerSetId).
                    collect(Collectors.toSet());
        }
        else
        {
            checkerSetIds = new HashSet<>();
        }

        List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(checkerSetListQueryReq.getKeyWord(),
                checkerSetIds, null, null, null, null, null, true,
                null, true);
        if (CollectionUtils.isNotEmpty(checkerSetEntityList))
        {
            List<CheckerSetEntity> finalCheckerList = checkerSetEntityList.stream().collect(Collectors.groupingBy(
                    CheckerSetEntity::getCheckerSetId)).entrySet().stream().map(entry ->
            {
                List<CheckerSetEntity> checkerSetEntities = entry.getValue();
                return checkerSetEntities.stream().max(Comparator.comparing(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity());
            }).collect(Collectors.toList());
            finalCheckerList.forEach(checkerSetEntity ->
            {
                //1. 计算语言数量
                if (judgeQualifiedCheckerSet(null, checkerSetListQueryReq.getCheckerSetCategory(), checkerSetListQueryReq.getToolName(),
                        checkerSetListQueryReq.getCheckerSetSource(), checkerSetEntity) && StringUtils.isNotEmpty(checkerSetEntity.getCheckerSetLang()))
                {
                    //要分新插件和老插件
                    if (null != checkerSetEntity.getLegacy() && checkerSetEntity.getLegacy())
                    {
                        if (CollectionUtils.isNotEmpty(Arrays.asList(checkerSetEntity.getCheckerSetLang().split(","))))
                        {
                            for (String lang : checkerSetEntity.getCheckerSetLang().split(","))
                            {
                                langMap.compute(lang, (k, v) ->
                                {
                                    if (null == v)
                                    {
                                        return 1;
                                    }
                                    else
                                    {
                                        v++;
                                        return v;
                                    }
                                });
                            }
                        }
                    }
                    else
                    {
                        langMap.compute(checkerSetEntity.getCheckerSetLang(), (k, v) ->
                        {
                            if (null == v)
                            {
                                return 1;
                            }
                            else
                            {
                                v++;
                                return v;
                            }
                        });
                    }
                }
                //2. 规则类别数量计算
                if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(), null, checkerSetListQueryReq.getToolName(),
                        checkerSetListQueryReq.getCheckerSetSource(), checkerSetEntity) && CollectionUtils.isNotEmpty(checkerSetEntity.getCatagories()))
                {
                    checkerSetEntity.getCatagories().forEach(category ->
                            checkerSetCateMap.compute(category.getEnName(), (k, v) ->
                            {
                                if (null == v)
                                {
                                    return 1;
                                }
                                else
                                {
                                    v++;
                                    return v;
                                }
                            })
                    );
                }
                //3. 工具数量计算
                if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(), checkerSetListQueryReq.getCheckerSetCategory(), null,
                        checkerSetListQueryReq.getCheckerSetSource(), checkerSetEntity) && CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                {
                    checkerSetEntity.getCheckerProps().stream().map(CheckerPropsEntity::getToolName).distinct().
                            forEach(tool ->
                            {
                                if (StringUtils.isBlank(tool))
                                {
                                    return;
                                }
                                toolMap.compute(tool, (k, v) ->
                                {
                                    if (null == v)
                                    {
                                        return 1;
                                    }
                                    else
                                    {
                                        v++;
                                        return v;
                                    }
                                });
                            });
                }
                //4. 来源数量计算
                if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(), checkerSetListQueryReq.getCheckerSetCategory(),
                        checkerSetListQueryReq.getToolName(), null, checkerSetEntity))
                {
                    sourceMap.compute(StringUtils.isBlank(checkerSetEntity.getCheckerSetSource()) ? "SELF_DEFINED" : checkerSetEntity.getCheckerSetSource(), (k, v) ->
                    {
                        if (null == v)
                        {
                            return 1;
                        }
                        else
                        {
                            v++;
                            return v;
                        }
                    });
                }

                //5. 总数计算
                if (judgeQualifiedCheckerSet(checkerSetListQueryReq.getCheckerSetLanguage(), checkerSetListQueryReq.getCheckerSetCategory(),
                        checkerSetListQueryReq.getToolName(), checkerSetListQueryReq.getCheckerSetSource(), checkerSetEntity))
                {
                    totalList.add(checkerSetEntity);
                }

            });
        }

        //按照语言顺序
        List<CheckerCountListVO> checkerSetLangCountVOList = langMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(entry.getKey(), null, entry.getValue())
        ).sorted(Comparator.comparingInt(o -> langOrder.indexOf(o.getKey()))).collect(Collectors.toList());
        //按照类别枚举排序
        List<CheckerSetCategory> categoryOrder = Arrays.asList(CheckerSetCategory.values());
        List<CheckerCountListVO> checkerSetCateCountVOList = checkerSetCateMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(CheckerSetCategory.valueOf(entry.getKey()).name(),
                        CheckerSetCategory.valueOf(entry.getKey()).getName(), entry.getValue())
        ).sorted(Comparator.comparingInt(o -> categoryOrder.indexOf(CheckerSetCategory.valueOf(o.getKey())))).
                collect(Collectors.toList());
        //按照工具的排序
        List<CheckerCountListVO> checkerSetToolCountVOList = toolMap.entrySet().stream().filter(entry -> entry.getValue() > 0).
                map(entry ->
                        new CheckerCountListVO(entry.getKey(), null, entry.getValue())
                ).sorted(Comparator.comparingInt(o -> toolOrder.indexOf(o.getKey()))).collect(Collectors.toList());

        List<CheckerSetSource> sourceOrder = Arrays.asList(CheckerSetSource.values());
        List<CheckerCountListVO> checkerSetSourceCountVOList = sourceMap.entrySet().stream().map(entry ->
                new CheckerCountListVO(CheckerSetSource.valueOf(entry.getKey()).name(),
                        CheckerSetSource.valueOf(entry.getKey()).getName(), entry.getValue())
        ).sorted(Comparator.comparingInt(o -> sourceOrder.indexOf(CheckerSetSource.valueOf(o.getKey())))).
                collect(Collectors.toList());
        List<CheckerCountListVO> checkerSetTotalCountVOList = Collections.singletonList(new CheckerCountListVO("total", null, totalList.size()));

        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerSetLanguage", checkerSetLangCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerSetCategory", checkerSetCateCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("toolName", checkerSetToolCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("checkerSetSource", checkerSetSourceCountVOList));
        checkerCommonCountVOList.add(new CheckerCommonCountVO("total", checkerSetTotalCountVOList));
        return checkerCommonCountVOList;

    }


    @Override
    public List<CheckerSetEntity> findAvailableCheckerSetsByProject(String projectId, List<Boolean> legacy)
    {
        List<CheckerSetProjectRelationshipEntity> checkerSetProjectRelationshipEntityList = checkerSetProjectRelationshipRepository.
                findByProjectId(projectId);
        Set<String> checkerSetIds;
        Map<String, Integer> checkerSetVersionMap;
        if (CollectionUtils.isEmpty(checkerSetProjectRelationshipEntityList))
        {
            checkerSetIds = new HashSet<>();
            checkerSetVersionMap = new HashMap<>();
        }
        else
        {
            checkerSetIds = checkerSetProjectRelationshipEntityList.stream().
                    map(CheckerSetProjectRelationshipEntity::getCheckerSetId).
                    collect(Collectors.toSet());
            checkerSetVersionMap = checkerSetProjectRelationshipEntityList.stream().
                    filter(checkerSetProjectRelationshipEntity ->
                            StringUtils.isNotBlank(checkerSetProjectRelationshipEntity.getCheckerSetId()) &&
                                    null != checkerSetProjectRelationshipEntity.getVersion()
                    ).
                    collect(Collectors.toMap(CheckerSetProjectRelationshipEntity::getCheckerSetId,
                            CheckerSetProjectRelationshipEntity::getVersion, (k, v) -> v));
        }

        List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(null,
                checkerSetIds, null, null, null, null, null, true,
                null, true);

        return checkerSetEntityList.stream()
                .filter(
                        checkerSetEntity ->
                                ((null != checkerSetEntity.getVersion() && null != checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId()) &&
                                        checkerSetEntity.getVersion().equals(checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId()))) ||
                                        Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetEntity.getCheckerSetSource()))
                                        // checkerSetEntity.getLegacy() == legacy 或者 legacy == false时checkerSetEntity.getLegacy() == null
                                        && (legacy.contains(checkerSetEntity.getLegacy())
                                        || (CollectionUtils.isNotEmpty(legacy) && legacy.contains(false) && checkerSetEntity.getLegacy() == null))
                                        && CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())
                ).collect(Collectors.toList());
    }

    @Override
    public Boolean updateCheckerSetAndTaskRelation(Long taskId, Long codeLang, String user)
    {
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList = checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        List<CheckerSetEntity> projectCheckerSetList;
        Map<Long, Map<String, Integer>> updatedCheckerSetMap = Maps.newHashMap();
        Map<Long, Map<String, Integer>> currentCheckerSetMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(checkerSetTaskRelationshipEntityList))
        {
            String projectId = checkerSetTaskRelationshipEntityList.get(0).getProjectId();
            List<CheckerSetProjectRelationshipEntity> checkerSetProjectRelationshipEntityList = checkerSetProjectRelationshipRepository.
                    findByProjectId(projectId);
            Set<String> checkerSetIds;

            if (CollectionUtils.isEmpty(checkerSetProjectRelationshipEntityList))
            {
                projectCheckerSetList = new ArrayList<>();
                checkerSetIds = new HashSet<>();
            }
            else
            {
                checkerSetIds = checkerSetProjectRelationshipEntityList.stream().
                        map(CheckerSetProjectRelationshipEntity::getCheckerSetId).
                        collect(Collectors.toSet());

                Map<String, Integer> checkerSetVersionMap = checkerSetProjectRelationshipEntityList.stream().
                        collect(HashMap::new, (m, v) -> m.put(v.getCheckerSetId(), v.getVersion()), HashMap::putAll);

                List<CheckerSetEntity> checkerSetEntityList = checkerSetDao.findByComplexCheckerSetCondition(null,
                        checkerSetIds, null, null, null, null, null, true,
                        null, true);


                projectCheckerSetList = checkerSetEntityList.stream().filter(
                        checkerSetEntity ->
                                (checkerSetEntity.getVersion().equals(checkerSetVersionMap.get(checkerSetEntity.getCheckerSetId())) ||
                                        Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetEntity.getCheckerSetSource()))
                                        && (Arrays.asList(true, false).contains(checkerSetEntity.getLegacy()) ||
                                        checkerSetEntity.getLegacy() == null)
                ).collect(Collectors.toList());
                currentCheckerSetMap.put(taskId, checkerSetTaskRelationshipEntityList.stream().collect(Collectors.toMap(
                        CheckerSetTaskRelationshipEntity::getCheckerSetId, checkerSetTaskRelationEntity -> checkerSetVersionMap.get(checkerSetTaskRelationEntity.getCheckerSetId()),
                        (k, v) -> v
                )));
                updatedCheckerSetMap.put(taskId, checkerSetTaskRelationshipEntityList.stream().collect(Collectors.toMap(
                        CheckerSetTaskRelationshipEntity::getCheckerSetId, checkerSetTaskRelationEntity -> checkerSetVersionMap.get(checkerSetTaskRelationEntity.getCheckerSetId()),
                        (k, v) -> v
                )));
            }

            //官方优选 官方推荐版本
            Map<String, Integer> officialMap = projectCheckerSetList.stream().filter(checkerSetEntity ->
                    Arrays.asList(CheckerSetSource.DEFAULT.name(), CheckerSetSource.RECOMMEND.name()).contains(checkerSetEntity.getCheckerSetSource())).
                    collect(Collectors.groupingBy(CheckerSetEntity::getCheckerSetId)).entrySet().stream().
                    collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> entry.getValue().stream().max(Comparator.comparingInt(CheckerSetEntity::getVersion)).orElse(new CheckerSetEntity()).getVersion()));

            List<CheckerSetEntity> taskCheckerSetList = projectCheckerSetList.stream().filter(checkerSetEntity ->
                    checkerSetTaskRelationshipEntityList.stream().anyMatch(checkerSetTaskRelationshipEntity ->
                            checkerSetTaskRelationshipEntity.getCheckerSetId().equals(checkerSetEntity.getCheckerSetId()))
            ).collect(Collectors.toList());
            //1. 解绑规则集
            List<CheckerSetEntity> needToUnbindList = taskCheckerSetList.stream().filter(checkerSetEntity -> (codeLang &
                    checkerSetEntity.getCodeLang()) == 0).collect(Collectors.toList());
            List<CheckerSetTaskRelationshipEntity> needToUnbindRelationEntityList = checkerSetTaskRelationshipEntityList.stream().
                    filter(checkerSetTaskRelationshipEntity -> needToUnbindList.stream().anyMatch(checkerSetEntity ->
                            checkerSetEntity.getCheckerSetId().equals(checkerSetTaskRelationshipEntity.getCheckerSetId()))).
                    collect(Collectors.toList());
            checkerSetTaskRelationshipRepository.delete(needToUnbindRelationEntityList);
            if (CollectionUtils.isNotEmpty(needToUnbindRelationEntityList))
            {
                needToUnbindRelationEntityList.forEach(unbindRelationEntity ->
                        updatedCheckerSetMap.get(taskId).remove(unbindRelationEntity.getCheckerSetId())
                );
            }

            //2. 新增语言自动绑定默认规则集
            Set<String> needToBindDefaultCheckerSet = new HashSet<>();
            Map<String, CheckerSetProjectRelationshipEntity> projectRelationshipEntityMap = checkerSetProjectRelationshipEntityList.stream().collect(Collectors.toMap(
                    CheckerSetProjectRelationshipEntity::getCheckerSetId, Function.identity(), (k, v) -> v
            ));
            String binaryCodeLang = Long.toBinaryString(codeLang);
            Long originalCodeLang = 1L << (binaryCodeLang.length() - 1);
            for (int i = 0; i < binaryCodeLang.length(); i++)
            {
                if ((binaryCodeLang.charAt(i) + "").equals("1"))
                {
                    Long selectedCodeLang = originalCodeLang >> i;
                    if (taskCheckerSetList.stream().allMatch(taskCheckerSetEntity ->
                            (selectedCodeLang & taskCheckerSetEntity.getCodeLang()) == 0L))
                    {
                        needToBindDefaultCheckerSet.addAll(projectCheckerSetList.stream().filter(projectCheckerSet ->
                                //条件1, 符合相应语言的
                                (projectCheckerSet.getCodeLang() & selectedCodeLang) > 0L &&
                                        //条件2, 默认的
                                        ((CheckerSetSource.DEFAULT.name().equals(projectCheckerSet.getCheckerSetSource()) &&
                                                null == projectRelationshipEntityMap.get(projectCheckerSet.getCheckerSetId())) ||
                                                (null != projectRelationshipEntityMap.get(projectCheckerSet.getCheckerSetId()) &&
                                                        null != projectRelationshipEntityMap.get(projectCheckerSet.getCheckerSetId()).getDefaultCheckerSet() &&
                                                        projectRelationshipEntityMap.get(projectCheckerSet.getCheckerSetId()).getDefaultCheckerSet())) &&
                                        //条件3，非legacy
                                        !(null != projectCheckerSet.getLegacy() && projectCheckerSet.getLegacy())
                        ).
                                map(CheckerSetEntity::getCheckerSetId).collect(Collectors.toSet()));
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(needToBindDefaultCheckerSet))
            {

                checkerSetProjectRelationshipRepository.save(
                        needToBindDefaultCheckerSet.stream().filter(checkerSetId ->
                                !checkerSetIds.contains(checkerSetId)
                        ).map(checkerSetId ->
                        {
                            CheckerSetProjectRelationshipEntity checkerSetProjectRelationshipEntity = new CheckerSetProjectRelationshipEntity();
                            checkerSetProjectRelationshipEntity.setCheckerSetId(checkerSetId);
                            checkerSetProjectRelationshipEntity.setProjectId(projectId);
                            checkerSetProjectRelationshipEntity.setDefaultCheckerSet(true);
                            checkerSetProjectRelationshipEntity.setUselatestVersion(true);
                            checkerSetProjectRelationshipEntity.setVersion(officialMap.get(checkerSetId));
                            return checkerSetProjectRelationshipEntity;
                        }).collect(Collectors.toList()));

                checkerSetTaskRelationshipRepository.save(needToBindDefaultCheckerSet.stream().filter(StringUtils::isNotBlank).
                        map(checkerSetId ->
                        {
                            CheckerSetTaskRelationshipEntity checkerSetTaskRelationshipEntity = new CheckerSetTaskRelationshipEntity();
                            checkerSetTaskRelationshipEntity.setTaskId(taskId);
                            checkerSetTaskRelationshipEntity.setProjectId(projectId);
                            checkerSetTaskRelationshipEntity.setCheckerSetId(checkerSetId);
                            return checkerSetTaskRelationshipEntity;
                        }).collect(Collectors.toSet()));
                needToBindDefaultCheckerSet.stream().filter(checkerSetId -> StringUtils.isNotBlank(checkerSetId) && officialMap.containsKey(checkerSetId)).
                        forEach(checkerSetId ->
                                updatedCheckerSetMap.get(taskId).put(checkerSetId, officialMap.get(checkerSetId))
                        );
            }
            // 对各任务设置强制全量扫描标志，并修改告警状态
            ThreadPoolUtil.addRunnableTask(() -> setForceFullScanAndUpdateDefectAndToolStatus(currentCheckerSetMap, updatedCheckerSetMap, user));
            return true;
        }
        return true;
    }

    @Override
    public TaskBaseVO getCheckerAndCheckerSetCount(Long taskId, String projectId)
    {
        TaskBaseVO taskBaseVO = new TaskBaseVO();
        taskBaseVO.setTaskId(taskId);
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList = checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        if (CollectionUtils.isNotEmpty(checkerSetTaskRelationshipEntityList))
        {
            Set<String> taskCheckerSetList = checkerSetTaskRelationshipEntityList.stream().map(CheckerSetTaskRelationshipEntity::getCheckerSetId).
                    distinct().collect(Collectors.toSet());
            List<CheckerSetEntity> checkerSetEntityList = findAvailableCheckerSetsByProject(projectId, Arrays.asList(true, false));
            if (CollectionUtils.isNotEmpty(checkerSetEntityList))
            {
                List<CheckerSetEntity> taskCheckerSetEntityList = checkerSetEntityList.stream().filter(checkerSetEntity ->
                        taskCheckerSetList.contains(checkerSetEntity.getCheckerSetId())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(taskCheckerSetEntityList))
                {
                    taskBaseVO.setCheckerSetName(taskCheckerSetEntityList.stream().map(CheckerSetEntity::getCheckerSetName).distinct().
                            reduce((o1, o2) -> String.format("%s,%s", o1, o2)).get());
                    taskBaseVO.setCheckerCount(taskCheckerSetEntityList.stream().filter(checkerSetEntity -> CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps())).
                            map(CheckerSetEntity::getCheckerProps).flatMap(Collection::stream).map(CheckerPropsEntity::getCheckerKey).distinct().count());
                    return taskBaseVO;
                }
            }
        }
        taskBaseVO.setCheckerCount(0L);
        taskBaseVO.setCheckerSetName("");
        return taskBaseVO;
    }

    private void setForceFullScanAndUpdateDefectAndToolStatus(Map<Long, Map<String, Integer>> currentTaskUseCheckerSetsMap,
                                                              Map<Long, Map<String, Integer>> updatedTaskUseCheckerSetsMap, String user)
    {
        // 获取所有涉及的规则集Entity
        Set<String> checkerSetIds = Sets.newHashSet();
        for (Map.Entry<Long, Map<String, Integer>> entry : currentTaskUseCheckerSetsMap.entrySet())
        {
            checkerSetIds.addAll(entry.getValue().keySet());
        }
        for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet())
        {
            checkerSetIds.addAll(entry.getValue().keySet());
        }
        Map<String, CheckerSetEntity> checkerSetIdVersionMap = Maps.newHashMap();
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);
        for (CheckerSetEntity checkerSetEntity : checkerSetEntities)
        {
            checkerSetIdVersionMap.put(checkerSetEntity.getCheckerSetId() + "_" + checkerSetEntity.getVersion(), checkerSetEntity);
        }

        // 设置强制全量扫描标志并更新告警状态
        for (Map.Entry<Long, Map<String, Integer>> entry : currentTaskUseCheckerSetsMap.entrySet())
        {
            List<CheckerSetEntity> fromCheckerSets = Lists.newArrayList();
            List<CheckerSetEntity> toCheckerSets = Lists.newArrayList();
            Map<String, Integer> updatedCheckerSetVersionMap = updatedTaskUseCheckerSetsMap.get(entry.getKey());
            if (MapUtils.isNotEmpty(entry.getValue()))
            {
                for (Map.Entry<String, Integer> checketSetIdVersionEntry : entry.getValue().entrySet())
                {
                    fromCheckerSets.add(checkerSetIdVersionMap.get(checketSetIdVersionEntry.getKey() + "_" + checketSetIdVersionEntry.getValue()));
                }
            }
            if (MapUtils.isNotEmpty(updatedCheckerSetVersionMap))
            {
                for (Map.Entry<String, Integer> checketSetIdVersionEntry : updatedCheckerSetVersionMap.entrySet())
                {
                    toCheckerSets.add(checkerSetIdVersionMap.get(checketSetIdVersionEntry.getKey() + "_" + checketSetIdVersionEntry.getValue()));
                }
            }
            setForceFullScanAndUpdateDefectAndToolStatus(entry.getKey(), fromCheckerSets, toCheckerSets);
        }

        // 更新工具状态
        for (Map.Entry<Long, Map<String, Integer>> entry : updatedTaskUseCheckerSetsMap.entrySet())
        {
            Set<String> updatedToolSet = Sets.newHashSet();
            if (MapUtils.isNotEmpty(entry.getValue()))
            {
                for (Map.Entry<String, Integer> checkerSetVersionEntry : entry.getValue().entrySet())
                {
                    CheckerSetEntity checkerSetEntity = checkerSetIdVersionMap.get(checkerSetVersionEntry.getKey() + "_" + checkerSetVersionEntry.getValue());
                    if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                    {
                        for (CheckerPropsEntity checkerPropsEntity : checkerSetEntity.getCheckerProps())
                        {
                            if (StringUtils.isNotBlank(checkerPropsEntity.getToolName()))
                            {
                                updatedToolSet.add(checkerPropsEntity.getToolName());
                            }
                        }
                    }
                }
            }

            long taskId = entry.getKey();
            BatchRegisterVO batchRegisterVO = new BatchRegisterVO();
            batchRegisterVO.setTaskId(taskId);
            List<ToolConfigInfoVO> toolConfigInfoVOS = Lists.newArrayList();
            for (String toolName : updatedToolSet)
            {
                ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
                toolConfigInfoVO.setTaskId(taskId);
                toolConfigInfoVO.setToolName(toolName);
                toolConfigInfoVOS.add(toolConfigInfoVO);
            }
            batchRegisterVO.setTools(toolConfigInfoVOS);
            client.get(ServiceToolRestResource.class).updateTools(taskId, user, batchRegisterVO);
        }
    }

    private void setForceFullScanAndUpdateDefectAndToolStatus(long taskId, List<CheckerSetEntity> fromCheckerSets, List<CheckerSetEntity> toCheckerSets)
    {
        // 初始化结果对象
        List<CheckerPropsEntity> openDefectCheckerProps = Lists.newArrayList();
        List<CheckerPropsEntity> closeDefectCheckeProps = Lists.newArrayList();
        List<CheckerPropsEntity> updatePropsCheckers = Lists.newArrayList();
        Set<String> toolNames = Sets.newHashSet();

        // 初始化校验用的切换后规则集临时Map
        Map<String, Map<String, CheckerPropsEntity>> toToolCheckersMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(toCheckerSets))
        {
            for (CheckerSetEntity checkerSetVO : toCheckerSets)
            {

                if (CollectionUtils.isNotEmpty(checkerSetVO.getCheckerProps()))
                {
                    for (CheckerPropsEntity checkerPropVO : checkerSetVO.getCheckerProps())
                    {
                        toToolCheckersMap.computeIfAbsent(checkerPropVO.getToolName(), k -> Maps.newHashMap());
                        toToolCheckersMap.get(checkerPropVO.getToolName()).put(checkerPropVO.getCheckerKey(), checkerPropVO);
                        toolNames.add(checkerPropVO.getToolName());
                    }
                }
            }
        }

        // 初始化校验用的切换前规则集临时Map，并记录需要关闭和需要更新的规则列表
        Map<String, Map<String, CheckerPropsEntity>> fromToolCheckersMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(fromCheckerSets))
        {
            for (CheckerSetEntity checkerSetVO : fromCheckerSets)
            {

                if (CollectionUtils.isNotEmpty(checkerSetVO.getCheckerProps()))
                {
                    for (CheckerPropsEntity checkerPropVO : checkerSetVO.getCheckerProps())
                    {
                        fromToolCheckersMap.computeIfAbsent(checkerPropVO.getToolName(), k -> Maps.newHashMap());
                        fromToolCheckersMap.get(checkerPropVO.getToolName()).put(checkerPropVO.getCheckerKey(), checkerPropVO);

                        if (toToolCheckersMap.get(checkerPropVO.getToolName()) == null
                                || !toToolCheckersMap.get(checkerPropVO.getToolName()).containsKey(checkerPropVO.getCheckerKey()))
                        {
                            closeDefectCheckeProps.add(checkerPropVO);
                        }
                        else
                        {
                            updatePropsCheckers.add(toToolCheckersMap.get(checkerPropVO.getToolName()).get(checkerPropVO.getCheckerKey()));
                        }
                    }
                }
            }
        }

        // 记录需要打开的规则列表
        if (CollectionUtils.isNotEmpty(toCheckerSets))
        {
            for (CheckerSetEntity checkerSetVO : toCheckerSets)
            {
                if (CollectionUtils.isNotEmpty(checkerSetVO.getCheckerProps()))
                {
                    for (CheckerPropsEntity checkerPropVO : checkerSetVO.getCheckerProps())
                    {
                        if (fromToolCheckersMap.get(checkerPropVO.getToolName()) == null
                                || !fromToolCheckersMap.get(checkerPropVO.getToolName()).containsKey(checkerPropVO.getCheckerKey()))
                        {
                            openDefectCheckerProps.add(checkerPropVO);
                        }
                    }
                }
            }
        }

        // 设置强制全量扫描
        toolBuildInfoService.setForceFullScan(taskId, Lists.newArrayList(toolNames));

        // 刷新告警状态
        Map<String, ConfigCheckersPkgReqVO> toolDefectRefreshConfigMap = Maps.newHashMap();
        for (CheckerPropsEntity checkerPropsEntity : openDefectCheckerProps)
        {
            ConfigCheckersPkgReqVO configCheckersPkgReq = getConfigCheckersReqVO(taskId, checkerPropsEntity.getToolName(), toolDefectRefreshConfigMap);
            configCheckersPkgReq.getOpenedCheckers().add(checkerPropsEntity.getCheckerKey());
        }
        for (CheckerPropsEntity checkerPropsEntity : closeDefectCheckeProps)
        {
            ConfigCheckersPkgReqVO configCheckersPkgReq = getConfigCheckersReqVO(taskId, checkerPropsEntity.getToolName(), toolDefectRefreshConfigMap);
            configCheckersPkgReq.getClosedCheckers().add(checkerPropsEntity.getCheckerKey());
        }
        for (Map.Entry<String, ConfigCheckersPkgReqVO> entry : toolDefectRefreshConfigMap.entrySet())
        {
            rabbitTemplate.convertAndSend(EXCHANGE_TASK_CHECKER_CONFIG, ROUTE_IGNORE_CHECKER, entry.getValue());
        }
    }

    private ConfigCheckersPkgReqVO getConfigCheckersReqVO(long taskId, String toolName, Map<String, ConfigCheckersPkgReqVO> toolDefectRefreshConfigMap)
    {
        ConfigCheckersPkgReqVO configCheckersPkgReq = toolDefectRefreshConfigMap.get(toolName);
        if (configCheckersPkgReq == null)
        {
            configCheckersPkgReq = new ConfigCheckersPkgReqVO();
            configCheckersPkgReq.setTaskId(taskId);
            configCheckersPkgReq.setToolName(toolName);
            configCheckersPkgReq.setOpenedCheckers(Lists.newArrayList());
            configCheckersPkgReq.setClosedCheckers(Lists.newArrayList());
            toolDefectRefreshConfigMap.put(toolName, configCheckersPkgReq);
        }
        return configCheckersPkgReq;
    }


    private Boolean judgeQualifiedCheckerSet(Set<String> checkerSetLanguage, Set<CheckerSetCategory> checkerSetCategorySet,
                                             Set<String> toolName, Set<CheckerSetSource> checkerSetSource, CheckerSetEntity checkerSetEntity)
    {
        //语言筛选要分新版本插件和老版本插件
        if (CollectionUtils.isNotEmpty(checkerSetLanguage))
        {
            if (null != checkerSetEntity.getLegacy() && checkerSetEntity.getLegacy())
            {
                if (checkerSetLanguage.stream().noneMatch(language -> checkerSetEntity.getCheckerSetLang().contains(language)))
                {
                    return false;
                }
            }
            else
            {
                if (!checkerSetLanguage.contains(checkerSetEntity.getCheckerSetLang()))
                {
                    return false;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(checkerSetCategorySet) && checkerSetCategorySet.stream().noneMatch(checkerSetCategory ->
                checkerSetEntity.getCatagories().stream().anyMatch(category -> checkerSetCategory.name().equalsIgnoreCase(category.getEnName()))))
        {
            return false;

        }
        if (CollectionUtils.isNotEmpty(toolName) && (CollectionUtils.isEmpty(checkerSetEntity.getCheckerProps()) ||
                toolName.stream().noneMatch(tool -> checkerSetEntity.getCheckerProps().stream().anyMatch(
                        checkerPropsEntity -> tool.equalsIgnoreCase(checkerPropsEntity.getToolName())))))
        {
            return false;
        }
        if (CollectionUtils.isNotEmpty(checkerSetSource) && !checkerSetSource.contains(CheckerSetSource.valueOf(
                StringUtils.isBlank(checkerSetEntity.getCheckerSetSource()) ? "SELF_DEFINED" : checkerSetEntity.getCheckerSetSource())))
        {
            return false;
        }
        return true;
    }


    /**
     * 校验规则集是否重复
     *
     * @param checkerSetId
     * @param checkerSetName
     */
    private void checkIdDuplicate(String checkerSetId, String checkerSetName)
    {
        boolean checkerSetIdDuplicate = false;
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetId(checkerSetId);
        for (CheckerSetEntity checkerSets : checkerSetEntities)
        {
            if (checkerSetIdDuplicate)
            {
                break;
            }
            if (checkerSets.getCheckerSetId().equals(checkerSetId))
            {
                checkerSetIdDuplicate = true;
            }
        }
        StringBuffer errMsg = new StringBuffer();
        if (checkerSetIdDuplicate)
        {
            errMsg.append("规则集ID");
        }
        if (errMsg.length() > 0)
        {
            String errMsgStr = errMsg.toString();
            log.error("{}已存在", errMsgStr);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsgStr}, null);
        }
    }

    /**
     * 校验规则集名称是否与公开规则集重复
     *
     * @param checkerSetName
     */
    private void checkNameExistInPublic(String checkerSetName)
    {
        boolean checkerSetNameDuplicate = false;
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByScope(CheckerConstants.CheckerSetScope.PUBLIC.code());
        for (CheckerSetEntity checkerSets : checkerSetEntities)
        {
            if (checkerSetNameDuplicate)
            {
                break;
            }
            if (checkerSets.getCheckerSetName().equals(checkerSetName))
            {
                checkerSetNameDuplicate = true;
            }
        }
        StringBuffer errMsg = new StringBuffer();
        if (checkerSetNameDuplicate)
        {
            errMsg.append("规则集名称");
        }
        if (errMsg.length() > 0)
        {
            String errMsgStr = errMsg.toString();
            log.error("{}已存在", errMsgStr);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsgStr}, null);
        }
    }

    /**
     * 校验规则集名称是否与项目规则集重复
     *
     * @param checkerSetName
     * @param projectId
     */
    private void checkNameExistInProject(String checkerSetName, String projectId)
    {
        boolean checkerSetNameDuplicate = false;
        List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByProjectId(projectId);
        for (CheckerSetEntity checkerSets : checkerSetEntities)
        {
            if (checkerSetNameDuplicate)
            {
                break;
            }
            if (checkerSets.getCheckerSetName().equals(checkerSetName))
            {
                checkerSetNameDuplicate = true;
            }
        }
        StringBuffer errMsg = new StringBuffer();
        if (checkerSetNameDuplicate)
        {
            errMsg.append("规则集名称");
        }
        if (errMsg.length() > 0)
        {
            String errMsgStr = errMsg.toString();
            log.error("{}已存在", errMsgStr);
            throw new CodeCCException(CommonMessageCode.RECORD_EXIST, new String[]{errMsgStr}, null);
        }
    }

    private Map<String, Integer> getLatestVersionMap(Set<String> checkerSetIds)
    {
        Map<String, Integer> latestVersionMap = Maps.newHashMap();
        if (CollectionUtils.isNotEmpty(checkerSetIds))
        {
            List<CheckerSetEntity> checkerSetEntities = checkerSetRepository.findByCheckerSetIdIn(checkerSetIds);
            if (CollectionUtils.isNotEmpty(checkerSetEntities))
            {
                for (CheckerSetEntity checkerSetEntity : checkerSetEntities)
                {
                    if (latestVersionMap.get(checkerSetEntity.getCheckerSetId()) == null
                            || checkerSetEntity.getVersion() > latestVersionMap.get(checkerSetEntity.getCheckerSetId()))
                    {
                        latestVersionMap.put(checkerSetEntity.getCheckerSetId(), checkerSetEntity.getVersion());
                    }
                }
            }
        }
        return latestVersionMap;
    }

    private List<CheckerSetCatagoryEntity> getCatagoryEntities(List<String> catatories)
    {
        List<CheckerSetCatagoryEntity> catagoryEntities = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(catatories))
        {
            Map<String, String> catagoryNameMap = Maps.newHashMap();
            for (CheckerSetCategory checkerSetCategory : CheckerSetCategory.values())
            {
                catagoryNameMap.put(checkerSetCategory.name(), checkerSetCategory.getName());
            }
            for (String categoryEnName : catatories)
            {
                CheckerSetCatagoryEntity catagoryEntity = new CheckerSetCatagoryEntity();
                catagoryEntity.setEnName(categoryEnName);
                catagoryEntity.setCnName(catagoryNameMap.get(categoryEnName));
                catagoryEntities.add(catagoryEntity);
            }
        }
        return catagoryEntities;
    }

    /**
     * 排序并分页
     *
     * @param pageNum
     * @param pageSize
     * @param sortField
     * @param sortType
     * @param defectVOs
     * @param <T>
     * @return
     */
    public <T> org.springframework.data.domain.Page<T> sortAngPage(int pageNum, int pageSize, String sortField,
                                                                   Sort.Direction sortType, List<T> defectVOs)
    {
        if (null == sortType)
        {
            sortType = Sort.Direction.ASC;
        }

        // 严重程度要跟前端传入的排序类型相反
        if ("severity".equals(sortField))
        {
            if (sortType.isAscending())
            {
                sortType = Sort.Direction.DESC;
            }
            else
            {
                sortType = Sort.Direction.ASC;
            }
        }
        ListSortUtil.sort(defectVOs, sortField, sortType.name());
        int total = defectVOs.size();
        pageNum = pageNum - 1 < 0 ? 0 : pageNum - 1;
        pageSize = pageSize <= 0 ? 10 : pageSize;
        int subListBeginIdx = pageNum * pageSize;
        int subListEndIdx = subListBeginIdx + pageSize;
        if (subListBeginIdx > total)
        {
            subListBeginIdx = 0;
        }
        defectVOs = defectVOs.subList(subListBeginIdx, subListEndIdx > total ? total : subListEndIdx);

        //封装分页类
        Pageable pageable = new PageRequest(pageNum, pageSize, new Sort(sortType, sortField));
        return new PageImpl<>(defectVOs, pageable, total);
    }

    @Override
    public List<CheckerSetVO> queryCheckerSetsForOpenScan(Set<String> checkerSetList, String projectId)
    {
        List<CheckerSetEntity> checkerSets = checkerSetDao.findByComplexCheckerSetCondition(null,
                checkerSetList, null, null, null, null, null, false,
                null, true);
        Map<String, Integer> latestVersionMap = new HashMap<>();
        for (CheckerSetEntity checkerSetEntity : checkerSets)
        {
            if (latestVersionMap.get(checkerSetEntity.getCheckerSetId()) == null
                    || checkerSetEntity.getVersion() > latestVersionMap.get(checkerSetEntity.getCheckerSetId()))
            {
                latestVersionMap.put(checkerSetEntity.getCheckerSetId(), checkerSetEntity.getVersion());
            }
        }
        if(CollectionUtils.isNotEmpty(checkerSets))
        {
            return checkerSets.stream()
                    .filter(checkerSetEntity ->
                            latestVersionMap.containsKey(checkerSetEntity.getCheckerSetId()) && null != latestVersionMap.get(checkerSetEntity.getCheckerSetId()) &&
                                latestVersionMap.get(checkerSetEntity.getCheckerSetId()).equals(checkerSetEntity.getVersion())
                    )
                    .map(checkerSetEntity ->
                    {
                        CheckerSetVO checkerSetVO = new CheckerSetVO();
                        BeanUtils.copyProperties(checkerSetEntity, checkerSetVO);
                        if (CollectionUtils.isNotEmpty(checkerSetEntity.getCheckerProps()))
                        {
                            checkerSetVO.setToolList(checkerSetEntity.getCheckerProps().stream().map(CheckerPropsEntity::getToolName).collect(Collectors.toSet()));
                        }
                        return checkerSetVO;
                    }).collect(Collectors.toList());
        }

        return null;
    }
}
