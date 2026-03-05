package com.rightmanage.service;

import com.rightmanage.dto.SysMenuVO;
import com.rightmanage.entity.SysMenu;
import java.util.List;

public interface SysMenuService {
    List<SysMenu> list();
    List<SysMenu> listByModuleCode(String moduleCode);
    List<SysMenuVO> listTreeByModuleCode(String moduleCode);
    SysMenu getById(Long id);
    boolean save(SysMenu menu);
    boolean updateById(SysMenu menu);
    boolean deleteById(Long id);
    boolean deleteWithChildren(Long id);
    boolean updateStatus(Long id, Integer status);
    boolean validateParentId(Long id, Long parentId);
    List<SysMenuVO> getMenuTreeOptions(String moduleCode);

    // ==================== 产品智能定制模块（全局结构+租户独立状态） ====================

    /**
     * 获取产品智能定制菜单列表（合并全局结构+租户状态）
     * @param tenantId 租户ID
     * @return 合并后的菜单列表
     */
    List<SysMenu> listProductCustomMenus(Long tenantId);

    /**
     * 获取产品智能定制菜单树（合并全局结构+租户状态）
     * @param tenantId 租户ID
     * @return 合并后的菜单树
     */
    List<SysMenuVO> listProductCustomMenuTree(Long tenantId);

    /**
     * 获取产品智能定制菜单树选项（用于父菜单选择）
     * @param tenantId 租户ID
     * @return 菜单树选项
     */
    List<SysMenuVO> getProductCustomMenuTreeOptions(Long tenantId);

    /**
     * 更新产品智能定制菜单
     * 结构字段（menuName/path/component等）：全局生效
     * status字段：仅当前租户生效
     * @param menu 更新的菜单数据
     * @param operateTenantId 操作租户ID
     * @return 是否成功
     */
    boolean updateProductCustomMenu(SysMenu menu, Long operateTenantId);

    /**
     * 删除产品智能定制菜单（删除全局记录及所有租户的状态记录）
     * @param globalMenuId 全局菜单ID
     * @return 是否成功
     */
    boolean deleteProductCustomMenu(Long globalMenuId);

    /**
     * 新增产品智能定制菜单（新增全局记录，并为所有租户生成状态记录）
     * @param menu 菜单数据
     * @param tenantIds 所有租户ID列表
     * @return 是否成功
     */
    boolean saveProductCustomMenu(SysMenu menu, List<Long> tenantIds);

    /**
     * 同步产品智能定制菜单结构到所有租户
     * @return 是否成功
     */
    boolean syncProductCustomMenuToAllTenants();

    /**
     * 初始化租户菜单状态（为指定租户初始化所有产品智能定制菜单的默认状态）
     * @param tenantId 租户ID
     * @return 是否成功
     */
    boolean initTenantMenuStatus(Long tenantId);
}
