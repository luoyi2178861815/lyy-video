package com.lyy.interaction.utils;


import com.lyy.interaction.entity.po.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommentTreeBuilder {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentTreeNode {
        private Comment comment;
        private List<CommentTreeNode> children = new ArrayList<>();
    }
    /**
     * 将评论列表构建为树形结构（返回所有一级评论节点）
     */
    public static List<CommentTreeNode> buildTree(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, CommentTreeNode> nodeMap = new HashMap<>();
        for (Comment c : comments) {
            nodeMap.put(c.getId(), new CommentTreeNode(c, new ArrayList<>()));
        }

        List<CommentTreeNode> roots = new ArrayList<>();
        for (Comment c : comments) {
            CommentTreeNode node = nodeMap.get(c.getId());
            Long parentId = c.getParentId();
            
            if (parentId == null || parentId == 0) {
                roots.add(node);
            } else {
                CommentTreeNode parent = nodeMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        
        return roots;
    }

    /**
     * 根据根评论ID过滤，只返回该根评论下的树
     */
    public static CommentTreeNode buildTreeByRootId(List<Comment> comments, Long rootId) {
        List<Comment> filteredComments = comments.stream()
                .filter(c -> rootId.equals(c.getRootId()))
                .collect(Collectors.toList());
        
        List<CommentTreeNode> tree = buildTree(filteredComments);
        return tree.isEmpty() ? null : tree.get(0);
    }
}
