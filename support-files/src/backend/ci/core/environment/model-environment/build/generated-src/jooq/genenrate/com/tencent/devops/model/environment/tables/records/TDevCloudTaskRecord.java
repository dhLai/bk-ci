/*
 * This file is generated by jOOQ.
*/
package com.tencent.devops.model.environment.tables.records;


import com.tencent.devops.model.environment.tables.TDevCloudTask;

import java.time.LocalDateTime;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record21;
import org.jooq.Row21;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TDevCloudTaskRecord extends UpdatableRecordImpl<TDevCloudTaskRecord> implements Record21<Long, String, String, String, String, String, String, String, String, String, String, Integer, String, String, Integer, String, LocalDateTime, LocalDateTime, String, Long, String> {

    private static final long serialVersionUID = 827507009;

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.TASK_ID</code>. ID
     */
    public TDevCloudTaskRecord setTaskId(Long value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.TASK_ID</code>. ID
     */
    public Long getTaskId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.PROJECT_ID</code>. 项目ID
     */
    public TDevCloudTaskRecord setProjectId(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.PROJECT_ID</code>. 项目ID
     */
    public String getProjectId() {
        return (String) get(1);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.OPERATOR</code>.
     */
    public TDevCloudTaskRecord setOperator(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.OPERATOR</code>.
     */
    public String getOperator() {
        return (String) get(2);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.ACTION</code>.
     */
    public TDevCloudTaskRecord setAction(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.ACTION</code>.
     */
    public String getAction() {
        return (String) get(3);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.STATUS</code>.
     */
    public TDevCloudTaskRecord setStatus(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.STATUS</code>.
     */
    public String getStatus() {
        return (String) get(4);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.REGISTRY_HOST</code>. 仓库地址
     */
    public TDevCloudTaskRecord setRegistryHost(String value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.REGISTRY_HOST</code>. 仓库地址
     */
    public String getRegistryHost() {
        return (String) get(5);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.REGISTRY_USER</code>. 仓库用户名
     */
    public TDevCloudTaskRecord setRegistryUser(String value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.REGISTRY_USER</code>. 仓库用户名
     */
    public String getRegistryUser() {
        return (String) get(6);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.REGISTRY_PWD</code>. 仓库密码
     */
    public TDevCloudTaskRecord setRegistryPwd(String value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.REGISTRY_PWD</code>. 仓库密码
     */
    public String getRegistryPwd() {
        return (String) get(7);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.CONTAINER_NAME</code>. 容器名称
     */
    public TDevCloudTaskRecord setContainerName(String value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.CONTAINER_NAME</code>. 容器名称
     */
    public String getContainerName() {
        return (String) get(8);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.CONTAINER_TYPE</code>. 容器类型
     */
    public TDevCloudTaskRecord setContainerType(String value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.CONTAINER_TYPE</code>. 容器类型
     */
    public String getContainerType() {
        return (String) get(9);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.IMAGE</code>. 镜像
     */
    public TDevCloudTaskRecord setImage(String value) {
        set(10, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.IMAGE</code>. 镜像
     */
    public String getImage() {
        return (String) get(10);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.CPU</code>. 容器cpu核数
     */
    public TDevCloudTaskRecord setCpu(Integer value) {
        set(11, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.CPU</code>. 容器cpu核数
     */
    public Integer getCpu() {
        return (Integer) get(11);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.MEMORY</code>. 容器内存大小
     */
    public TDevCloudTaskRecord setMemory(String value) {
        set(12, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.MEMORY</code>. 容器内存大小
     */
    public String getMemory() {
        return (String) get(12);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.DISK</code>. 容器磁盘大小
     */
    public TDevCloudTaskRecord setDisk(String value) {
        set(13, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.DISK</code>. 容器磁盘大小
     */
    public String getDisk() {
        return (String) get(13);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.REPLICA</code>. 容器副本数
     */
    public TDevCloudTaskRecord setReplica(Integer value) {
        set(14, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.REPLICA</code>. 容器副本数
     */
    public Integer getReplica() {
        return (Integer) get(14);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.PASSWORD</code>. 容器密码
     */
    public TDevCloudTaskRecord setPassword(String value) {
        set(15, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.PASSWORD</code>. 容器密码
     */
    public String getPassword() {
        return (String) get(15);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.CREATED_TIME</code>.
     */
    public TDevCloudTaskRecord setCreatedTime(LocalDateTime value) {
        set(16, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.CREATED_TIME</code>.
     */
    public LocalDateTime getCreatedTime() {
        return (LocalDateTime) get(16);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.UPDATE_TIME</code>.
     */
    public TDevCloudTaskRecord setUpdateTime(LocalDateTime value) {
        set(17, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.UPDATE_TIME</code>.
     */
    public LocalDateTime getUpdateTime() {
        return (LocalDateTime) get(17);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.DEV_CLOUD_TASK_ID</code>. devCloud的任务Id
     */
    public TDevCloudTaskRecord setDevCloudTaskId(String value) {
        set(18, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.DEV_CLOUD_TASK_ID</code>. devCloud的任务Id
     */
    public String getDevCloudTaskId() {
        return (String) get(18);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.NODE_LONG_ID</code>. nodeId
     */
    public TDevCloudTaskRecord setNodeLongId(Long value) {
        set(19, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.NODE_LONG_ID</code>. nodeId
     */
    public Long getNodeLongId() {
        return (Long) get(19);
    }

    /**
     * Setter for <code>devops_environment.T_DEV_CLOUD_TASK.DESCRIPTION</code>.
     */
    public TDevCloudTaskRecord setDescription(String value) {
        set(20, value);
        return this;
    }

    /**
     * Getter for <code>devops_environment.T_DEV_CLOUD_TASK.DESCRIPTION</code>.
     */
    public String getDescription() {
        return (String) get(20);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record21 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row21<Long, String, String, String, String, String, String, String, String, String, String, Integer, String, String, Integer, String, LocalDateTime, LocalDateTime, String, Long, String> fieldsRow() {
        return (Row21) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row21<Long, String, String, String, String, String, String, String, String, String, String, Integer, String, String, Integer, String, LocalDateTime, LocalDateTime, String, Long, String> valuesRow() {
        return (Row21) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field1() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.TASK_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field2() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.PROJECT_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field3() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.OPERATOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field4() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.ACTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field5() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.STATUS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field6() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.REGISTRY_HOST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field7() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.REGISTRY_USER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field8() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.REGISTRY_PWD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field9() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.CONTAINER_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field10() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.CONTAINER_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field11() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.IMAGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field12() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.CPU;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field13() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.MEMORY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field14() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.DISK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Integer> field15() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.REPLICA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field16() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.PASSWORD;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<LocalDateTime> field17() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.CREATED_TIME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<LocalDateTime> field18() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.UPDATE_TIME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field19() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.DEV_CLOUD_TASK_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field20() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.NODE_LONG_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field21() {
        return TDevCloudTask.T_DEV_CLOUD_TASK.DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value1() {
        return getTaskId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value2() {
        return getProjectId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value3() {
        return getOperator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value4() {
        return getAction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value5() {
        return getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value6() {
        return getRegistryHost();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value7() {
        return getRegistryUser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value8() {
        return getRegistryPwd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value9() {
        return getContainerName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value10() {
        return getContainerType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value11() {
        return getImage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value12() {
        return getCpu();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value13() {
        return getMemory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value14() {
        return getDisk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer value15() {
        return getReplica();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value16() {
        return getPassword();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime value17() {
        return getCreatedTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalDateTime value18() {
        return getUpdateTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value19() {
        return getDevCloudTaskId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value20() {
        return getNodeLongId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value21() {
        return getDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value1(Long value) {
        setTaskId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value2(String value) {
        setProjectId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value3(String value) {
        setOperator(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value4(String value) {
        setAction(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value5(String value) {
        setStatus(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value6(String value) {
        setRegistryHost(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value7(String value) {
        setRegistryUser(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value8(String value) {
        setRegistryPwd(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value9(String value) {
        setContainerName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value10(String value) {
        setContainerType(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value11(String value) {
        setImage(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value12(Integer value) {
        setCpu(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value13(String value) {
        setMemory(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value14(String value) {
        setDisk(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value15(Integer value) {
        setReplica(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value16(String value) {
        setPassword(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value17(LocalDateTime value) {
        setCreatedTime(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value18(LocalDateTime value) {
        setUpdateTime(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value19(String value) {
        setDevCloudTaskId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value20(Long value) {
        setNodeLongId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord value21(String value) {
        setDescription(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TDevCloudTaskRecord values(Long value1, String value2, String value3, String value4, String value5, String value6, String value7, String value8, String value9, String value10, String value11, Integer value12, String value13, String value14, Integer value15, String value16, LocalDateTime value17, LocalDateTime value18, String value19, Long value20, String value21) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        value14(value14);
        value15(value15);
        value16(value16);
        value17(value17);
        value18(value18);
        value19(value19);
        value20(value20);
        value21(value21);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached TDevCloudTaskRecord
     */
    public TDevCloudTaskRecord() {
        super(TDevCloudTask.T_DEV_CLOUD_TASK);
    }

    /**
     * Create a detached, initialised TDevCloudTaskRecord
     */
    public TDevCloudTaskRecord(Long taskId, String projectId, String operator, String action, String status, String registryHost, String registryUser, String registryPwd, String containerName, String containerType, String image, Integer cpu, String memory, String disk, Integer replica, String password, LocalDateTime createdTime, LocalDateTime updateTime, String devCloudTaskId, Long nodeLongId, String description) {
        super(TDevCloudTask.T_DEV_CLOUD_TASK);

        set(0, taskId);
        set(1, projectId);
        set(2, operator);
        set(3, action);
        set(4, status);
        set(5, registryHost);
        set(6, registryUser);
        set(7, registryPwd);
        set(8, containerName);
        set(9, containerType);
        set(10, image);
        set(11, cpu);
        set(12, memory);
        set(13, disk);
        set(14, replica);
        set(15, password);
        set(16, createdTime);
        set(17, updateTime);
        set(18, devCloudTaskId);
        set(19, nodeLongId);
        set(20, description);
    }
}