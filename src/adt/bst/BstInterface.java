/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt.bst;

/** bst store data efficiently
 *
 * @author Lee Cheng Xuan
 */
public interface BstInterface<K extends Comparable<K>, T> {
    void insert(K key, T data);
    T search(K key);
    void delete(K key);
    boolean isEmpty();
    int size();
    void inorderTraversal(BstVisitor<T> visitor);
}
