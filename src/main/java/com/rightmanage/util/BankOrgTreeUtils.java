package com.rightmanage.util;

import com.rightmanage.dto.BankOrgTreeVO;
import com.rightmanage.entity.BankOrg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 银行机构树工具函数集合
 */
public final class BankOrgTreeUtils {

    private BankOrgTreeUtils() {
    }

    /**
     * 构建标准树结构（id、parentId、name、level、code、children）
     */
    public static List<BankOrgTreeVO> buildTree(List<BankOrg> all) {
        Map<Long, BankOrgTreeVO> map = new LinkedHashMap<>();
        for (BankOrg org : all) {
            BankOrgTreeVO node = new BankOrgTreeVO();
            node.setId(org.getId());
            node.setParentId(org.getParentId());
            node.setName(org.getName());
            node.setCode(org.getCode());
            node.setLevel(org.getLevel());
            node.setChildren(new ArrayList<>());
            map.put(org.getId(), node);
        }

        List<BankOrgTreeVO> roots = new ArrayList<>();
        for (BankOrgTreeVO node : map.values()) {
            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(node);
                continue;
            }
            BankOrgTreeVO parent = map.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            }
        }

        sortTree(roots, all.stream().collect(Collectors.toMap(BankOrg::getId, o -> o)));
        return roots;
    }

    /**
     * 获取完整路径（总行 -> ... -> 当前）
     */
    public static List<BankOrg> getPath(Long id, Map<Long, BankOrg> byId) {
        List<BankOrg> path = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Long current = id;
        while (current != null && current != 0L) {
            if (visited.contains(current)) {
                break;
            }
            visited.add(current);
            BankOrg node = byId.get(current);
            if (node == null) {
                break;
            }
            path.add(node);
            current = node.getParentId();
        }
        // 反转为从总行到当前机构
        List<BankOrg> result = new ArrayList<>();
        for (int i = path.size() - 1; i >= 0; i--) {
            result.add(path.get(i));
        }
        return result;
    }

    /**
     * 获取父节点
     */
    public static BankOrg getParent(Long id, Map<Long, BankOrg> byId) {
        BankOrg current = byId.get(id);
        if (current == null || current.getParentId() == null || current.getParentId() == 0L) {
            return null;
        }
        return byId.get(current.getParentId());
    }

    /**
     * 获取直接子节点
     */
    public static List<BankOrg> getChildren(Long id, Map<Long, List<BankOrg>> childrenMap) {
        return childrenMap.getOrDefault(id, new ArrayList<>());
    }

    /**
     * 获取全部后代节点（BFS）
     */
    public static List<BankOrg> getDescendants(Long id, Map<Long, List<BankOrg>> childrenMap) {
        List<BankOrg> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(id);

        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            List<BankOrg> children = childrenMap.getOrDefault(parentId, new ArrayList<>());
            for (BankOrg child : children) {
                result.add(child);
                queue.add(child.getId());
            }
        }
        return result;
    }

    public static Map<Long, BankOrg> toByIdMap(List<BankOrg> all) {
        return all.stream().collect(Collectors.toMap(BankOrg::getId, o -> o, (a, b) -> a, HashMap::new));
    }

    public static Map<Long, List<BankOrg>> toChildrenMap(List<BankOrg> all) {
        Map<Long, List<BankOrg>> map = new HashMap<>();
        for (BankOrg org : all) {
            Long parentId = org.getParentId() == null ? 0L : org.getParentId();
            map.computeIfAbsent(parentId, k -> new ArrayList<>()).add(org);
        }
        for (List<BankOrg> list : map.values()) {
            list.sort(Comparator.comparing(BankOrg::getSort, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(BankOrg::getId));
        }
        return map;
    }

    private static void sortTree(List<BankOrgTreeVO> nodes, Map<Long, BankOrg> rawMap) {
        nodes.sort(Comparator
                .comparing((BankOrgTreeVO n) -> {
                    BankOrg raw = rawMap.get(n.getId());
                    return raw == null ? Integer.MAX_VALUE : raw.getSort();
                }, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BankOrgTreeVO::getId));
        for (BankOrgTreeVO node : nodes) {
            sortTree(node.getChildren(), rawMap);
        }
    }
}
