package com.rightmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rightmanage.entity.BankOrg;
import com.rightmanage.entity.SysUser;

import java.util.List;

public interface SysUserService {

    List<SysUser> list();

    IPage<SysUser> page(Integer pageNum, Integer pageSize, String username);

    SysUser getById(Long id);

    boolean save(SysUser user);

    boolean updateById(SysUser user);

    boolean deleteById(Long id);

    boolean updateStatus(Long id, Integer status);

    List<Long> getRoleIdsByUserId(Long userId);

    List<Long> getRoleIdsByUserId(Long userId, String moduleCode, Long tenantId);

    boolean bindRoles(Long userId, List<Long> roleIds, String moduleCode, Long tenantId);

    BankOrg getAuthorizedOrg(Long userId, String moduleCode, Long tenantId);

    boolean bindAuthorizedOrg(Long userId, String moduleCode, Long tenantId, Long orgId);

    /**
     * 获取指定机构的所有祖先机构ID列表（从根到父，包含自身）
     */
    List<Long> getAncestorOrgIds(Long orgId);

    /**
     * 获取指定机构的所有后代机构ID列表（包含自身）
     */
    List<Long> getDescendantOrgIds(Long orgId);

    /**
     * 判断用户在指定模块/租户下，是否有权限审批机构相关任务
     * <p>判断逻辑：从 sourceOrgId 开始向上遍历（通过 parentId），直到 parentId=0（顶层），
     * 如果用户授权机构在此路径上（包括 sourceOrgId 本身），则有权审批。
     * <p>后续可扩展其他判断维度（如机构类型、层级范围等），只需修改此方法。
     *
     * @param userId      用户ID
     * @param moduleCode  模块编码
     * @param tenantId    租户ID（可为null）
     * @param sourceOrgId 发起机构ID（待审批任务的 sourceOrgId）
     * @return true=有权审批，false=无权审批
     */
    boolean isUserAuthorizedForOrgLevel(Long userId, String moduleCode, Long tenantId, Long sourceOrgId);
}
