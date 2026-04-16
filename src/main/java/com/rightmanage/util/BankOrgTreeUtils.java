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
        Map<String, BankOrgTreeVO> map = new LinkedHashMap<>();
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
            if (node.getParentId() == null || node.getParentId().trim().isEmpty()) {
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
    public static List<BankOrg> getPath(String id, Map<String, BankOrg> byId) {
        List<BankOrg> path = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String current = id;
        while (current != null && !current.trim().isEmpty()) {
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
    public static BankOrg getParent(String id, Map<String, BankOrg> byId) {
        BankOrg current = byId.get(id);
        if (current == null || current.getParentId() == null || current.getParentId().trim().isEmpty()) {
            return null;
        }
        return byId.get(current.getParentId());
    }

    /**
     * 获取直接子节点
     */
    public static List<BankOrg> getChildren(String id, Map<String, List<BankOrg>> childrenMap) {
        return childrenMap.getOrDefault(id, new ArrayList<>());
    }

    /**
     * 获取全部后代节点（BFS）
     */
    public static List<BankOrg> getDescendants(String id, Map<String, List<BankOrg>> childrenMap) {
        List<BankOrg> result = new ArrayList<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(id);

        while (!queue.isEmpty()) {
            String parentId = queue.poll();
            List<BankOrg> children = childrenMap.getOrDefault(parentId, new ArrayList<>());
            for (BankOrg child : children) {
                result.add(child);
                queue.add(child.getId());
            }
        }
        return result;
    }

    public static Map<String, BankOrg> toByIdMap(List<BankOrg> all) {
        return all.stream().collect(Collectors.toMap(BankOrg::getId, o -> o, (a, b) -> a, HashMap::new));
    }

    public static Map<String, List<BankOrg>> toChildrenMap(List<BankOrg> all) {
        Map<String, List<BankOrg>> map = new HashMap<>();
        for (BankOrg org : all) {
            String parentId = org.getParentId() == null ? "" : org.getParentId();
            map.computeIfAbsent(parentId, k -> new ArrayList<>()).add(org);
        }
        for (List<BankOrg> list : map.values()) {
            list.sort(Comparator.comparing(BankOrg::getSort, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(BankOrg::getId));
        }
        return map;
    }

    private static void sortTree(List<BankOrgTreeVO> nodes, Map<String, BankOrg> rawMap) {
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
