package com.rightmanage.util;

import com.rightmanage.dto.AssetTypeTreeVO;
import com.rightmanage.entity.AssetType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产类型树工具函数集合
 */
public final class AssetTypeTreeUtils {

    private AssetTypeTreeUtils() {
    }

    /**
     * 构建标准树结构
     */
    public static List<AssetTypeTreeVO> buildTree(List<AssetType> all) {
        Map<Long, AssetTypeTreeVO> map = new LinkedHashMap<>();
        for (AssetType item : all) {
            AssetTypeTreeVO node = new AssetTypeTreeVO();
            node.setId(item.getId());
            node.setParentId(item.getParentId());
            node.setTypeName(item.getTypeName());
            node.setTypeCode(item.getTypeCode());
            node.setSort(item.getSort());
            node.setStatus(item.getStatus());
            node.setRemark(item.getRemark());
            node.setModuleCode(item.getModuleCode());
            node.setChildren(new ArrayList<>());
            map.put(item.getId(), node);
        }

        List<AssetTypeTreeVO> roots = new ArrayList<>();
        for (AssetTypeTreeVO node : map.values()) {
            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(node);
                continue;
            }
            AssetTypeTreeVO parent = map.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            }
        }

        sortTree(roots, all.stream().collect(Collectors.toMap(AssetType::getId, o -> o, (a, b) -> a)));
        return roots;
    }

    /**
     * 获取全部后代节点（BFS）
     */
    public static List<AssetType> getDescendants(Long id, Map<Long, List<AssetType>> childrenMap) {
        List<AssetType> result = new ArrayList<>();
        List<Long> queue = new ArrayList<>();
        queue.add(id);

        while (!queue.isEmpty()) {
            Long parentId = queue.remove(0);
            List<AssetType> children = childrenMap.getOrDefault(parentId, new ArrayList<>());
            for (AssetType child : children) {
                result.add(child);
                queue.add(child.getId());
            }
        }
        return result;
    }

    public static Map<Long, List<AssetType>> toChildrenMap(List<AssetType> all) {
        Map<Long, List<AssetType>> map = new HashMap<>();
        for (AssetType item : all) {
            Long parentId = item.getParentId() == null ? 0L : item.getParentId();
            map.computeIfAbsent(parentId, k -> new ArrayList<>()).add(item);
        }
        for (List<AssetType> list : map.values()) {
            list.sort(Comparator.comparing(AssetType::getSort, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(AssetType::getId));
        }
        return map;
    }

    private static void sortTree(List<AssetTypeTreeVO> nodes, Map<Long, AssetType> rawMap) {
        nodes.sort(Comparator
                .comparing((AssetTypeTreeVO n) -> {
                    AssetType raw = rawMap.get(n.getId());
                    return raw == null ? Integer.MAX_VALUE : raw.getSort();
                }, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(AssetTypeTreeVO::getId));
        for (AssetTypeTreeVO node : nodes) {
            sortTree(node.getChildren(), rawMap);
        }
    }
}
