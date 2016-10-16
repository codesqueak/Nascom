/*
 * MIT License
 *
 * Copyright (c) 2016
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.codingrodent.emulator.utilities;

class BTree {
    private BTreeNode root;

    BTree() {
        root = null;
    }

    /**
     * Insert a node into the b-tree
     *
     * @param node Node to insert
     */
    void insertNode(BTreeNode node) {
        if (null == root) {
            root = node;
        } else {
            insertNode(root, node);
        }
    }

    /**
     * Insert a node into the b-tree
     *
     * @param node Node to insert
     * @param leaf Place in the tree to attempt insert at
     */
    private void insertNode(BTreeNode leaf, BTreeNode node) {
        if (node.getValue() > leaf.getValue()) {
            BTreeNode right = leaf.getRight();
            if (null == right) {
                leaf.setRight(node);
            } else {
                insertNode(right, node);
            }
        } else {
            if (node.getValue() < leaf.getValue()) {
                BTreeNode left = leaf.getLeft();
                if (null == left) {
                    leaf.setLeft(node);
                } else {
                    insertNode(left, node);
                }
            } else {
                leaf.erase();
                leaf.setData(node.getData());
            }
        }
    }

    /**
     * Recurse the tree to find a node with a matching value
     *
     * @param value Value being checked for
     * @return Either a matching node or null if no match found
     */
    BTreeNode getNode(int value) {
        if (null == root) {
            return null;
        } else {
            return getNode(root, value);
        }
    }

    /**
     * Recurse the tree to find a node with a matching value
     *
     * @param leaf  Node to process
     * @param value Value being checked for
     * @return Either a matching node or null if no match found
     */
    private BTreeNode getNode(BTreeNode leaf, int value) {
        if (value > leaf.getValue()) {
            BTreeNode right = leaf.getRight();
            if (null == right) {
                return null;
            } else {
                return getNode(right, value);
            }
        } else {
            if (value < leaf.getValue()) {
                BTreeNode left = leaf.getLeft();
                if (null == left) {
                    return null;
                } else {
                    return getNode(left, value);
                }
            } else {
                return leaf;
            }
        }
    }

    /**
     * Delete all elements of this b-tree
     */
    void erase() {
        erase(root);
    }

    /**
     * Delete a node in the b-tree
     *
     * @param node Node to delete
     */
    private void erase(BTreeNode node) {
        if (null != node) {
            if (null != node.getLeft()) {
                erase(node.getLeft());
                node.setLeft(null);
            }
            //
            if (null != node.getRight()) {
                erase(node.getRight());
                node.setRight(null);
            }
            //
            node.getData().erase();
        }
    }

}
