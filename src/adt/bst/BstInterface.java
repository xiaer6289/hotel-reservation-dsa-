/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt.bst;

/** bst store data efficiently
 *
 * @author Lee Cheng Xuan
 */
public interface BstInterface<K extends Comparable<? super K>, T> {

    /**
     * Inserts a key-data pair. If the key already exists, its data is replaced
     * and the number of nodes is unchanged.
     */
    void insert(K key, T data);

    /** Returns the data associated with key, or null when the key is absent. */
    T search(K key);

    /** Removes the node identified by key when it exists. */
    void delete(K key);

    /** Returns true when no nodes are stored. */
    boolean isEmpty();

    /** Returns the number of unique keys currently stored. */
    int size();

    /** Removes all nodes from the tree. */
    void clear();

    /** Visits stored data in ascending key order without exposing tree nodes. */
    void inorderTraversal(BstVisitor<T> visitor);
}
