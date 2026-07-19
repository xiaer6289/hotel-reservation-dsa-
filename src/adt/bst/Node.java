/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt.bst;

/**
 *
 * @author Lee Cheng Xuan
 */
public class Node<K extends Comparable<K>, T> {
    private K key;
    private T data;
    private Node<K, T> left;
    private Node<K, T> right;
    
    public Node(K key, T data) {
        this.key = key;
        this.data = data;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Node<K, T> getLeft() {
        return left;
    }

    public void setLeft(Node<K, T> left) {
        this.left = left;
    }

    public Node<K, T> getRight() {
        return right;
    }

    public void setRight(Node<K, T> right) {
        this.right = right;
    }
    
    
}
