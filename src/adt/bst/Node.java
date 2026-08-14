/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt.bst;

import java.io.Serializable;

/**
 * Internal BST node. Package-private visibility prevents business modules from
 * depending on tree links; clients interact only through BstInterface.
 * @author Lee Cheng Xuan
 */
final class Node<K extends Comparable<? super K>, T> {

    private K key;
    private T data;
    private Node<K, T> left;
    private Node<K, T> right;

    Node(K key, T data) {
        this.key = key;
        this.data = data;
    }

    K getKey() {
        return key;
    }

    void setKey(K key) {
        this.key = key;
    }

    T getData() {
        return data;
    }

    void setData(T data) {
        this.data = data;
    }

    Node<K, T> getLeft() {
        return left;
    }

    void setLeft(Node<K, T> left) {
        this.left = left;
    }

    Node<K, T> getRight() {
        return right;
    }

    void setRight(Node<K, T> right) {
        this.right = right;
    }
}