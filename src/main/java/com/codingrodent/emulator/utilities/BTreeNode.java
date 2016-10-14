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

public class BTreeNode {
    private IBTreeData data;
    private BTreeNode left, right;

    public BTreeNode() {
        data = null;
        left = null;
        right = null;
    }

    /**
     * Get the data object stored in this node
     *
     * @return The data object
     */
    public IBTreeData getData() {
        return data;
    }

    /**
     * Set the data in the node
     *
     * @param data data to store
     */
    public void setData(IBTreeData data) {
        this.data = data;
    }

    /**
     * Get the left hand node of the b-tree
     *
     * @return Left node
     */
    public BTreeNode getLeft() {
        return left;
    }

    /**
     * Set the left leaf of the b-tree
     *
     * @param node Node to set
     */
    public void setLeft(BTreeNode node) {
        this.left = node;
    }

    /**
     * Get the right hand node of the b-tree
     *
     * @return Right node
     */
    public BTreeNode getRight() {
        return right;
    }

    /**
     * Set the right leaf of the b-tree
     *
     * @param node Node to set
     */
    public void setRight(BTreeNode node) {
        this.right = node;
    }

    /**
     * Get the value for the b-tree node
     *
     * @return Value
     */
    public int getValue() {
        return data.getValue();
    }

    /**
     * Destroy and data in the node
     */
    public void erase() {
        data.erase();
    }

}
