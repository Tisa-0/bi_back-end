package com.rightmanage.service;

import com.rightmanage.dto.SysMenuVO;
import com.rightmanage.entity.SysMenu;
import java.util.List;

public interface SysMenuService {
    List<SysMenu> list();
    List<SysMenu> listByModuleCode(String moduleCode);
    List<SysMenuVO> listTreeByModuleCode(String moduleCode);

    /**
     * 根据用户ID和模块代码获取用户有权限的菜单树
     * @param userId 用户ID
     * @param moduleCode 模块代码（A/B/C）
     * @return 菜单树列表
     */
    List<SysMenuVO> listTreeByUserIdAndModuleCode(Long userId, String moduleCode);

    List<SysMenuVO> listTreeByUserIdAndModuleCodeAndTenant(Long userId, String moduleCode, String tenantCode);

    SysMenu getById(String menuId);
    boolean save(SysMenu menu);
    boolean updateById(SysMenu menu);
    boolean deleteById(String menuId);
    boolean deleteWithChildren(String menuId);
    boolean updateStatus(String menuId, Integer status);
    boolean validateParentId(String menuId, String parentId);
    List<SysMenuVO> getMenuTreeOptions(String moduleCode);

    // ==================== 产品智能定制模块（全局结构+租户独立状态） ====================

    /**
     * 获取产品智能定制菜单列表（合并全局结构+租户状态）
     * @param tenantCode 租户编码
     * @return 合并后的菜单列表
     */
    List<SysMenu> listProductCustomMenus(String tenantCode);

    /**
     * 获取产品智能定制菜单树（合并全局结构+租户状态）
     * @param tenantCode 租户编码
     * @return 合并后的菜单树
     */
    List<SysMenuVO> listProductCustomMenuTree(String tenantCode);

    /**
     * 获取产品智能定制菜单树选项（用于父菜单选择）
     * @param tenantCode 租户编码
     * @return 菜单树选项
     */
    List<SysMenuVO> getProductCustomMenuTreeOptions(String tenantCode);

    /**
     * 更新产品智能定制菜单
     * 结构字段（menuName/path/component等）：全局生效
     * status字段：仅当前租户生效
     * @param menu 更新的菜单数据
     * @param operateTenantCode 操作租户编码
     * @return 是否成功
     */
    boolean updateProductCustomMenu(SysMenu menu, String operateTenantCode);

    /**
     * 删除产品智能定制菜单（删除全局记录及所有租户的状态记录）
     * @param globalMenuId 全局菜单ID
     * @return 是否成功
     */
    boolean deleteProductCustomMenu(String globalMenuId);

    /**
     * 新增产品智能定制菜单（新增全局记录，并为所有租户生成状态记录）
     * @param menu 菜单数据
     * @param tenantCodes 所有租户编码列表
     * @return 是否成功
     */
    boolean saveProductCustomMenu(SysMenu menu, List<String> tenantCodes);

    /**
     * 同步产品智能定制菜单结构到所有租户
     * @return 是否成功
     */
    boolean syncProductCustomMenuToAllTenants();

    /**
     * 初始化租户菜单状态（为指定租户初始化所有产品智能定制菜单的默认状态）
     * @param tenantCode 租户编码
     * @return 是否成功
     */
    boolean initTenantMenuStatus(String tenantCode);
}
