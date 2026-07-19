/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt.bst;


/**
 *
 * @author Lee Cheng Xuan
 */
public class Bst<K extends Comparable<K>, T> implements BstInterface<K, T>{ // binary search tree
    private Node<K, T> root;
    private int size;
    
    public Bst() {
        root = null;
        size = 0;
    }
    
    @Override
    public void insert(K key, T data) {
        root = insertHelper(root, key, data); // recursion by using helper method
    }
    
    private Node<K, T> insertHelper(Node<K, T> node, K key, T data) {
        if (node == null) {
            size++;
            return new Node<>(key, data);
        }
        int cmp = key.compareTo(node.getKey());
        if (cmp < 0) node.setLeft(insertHelper(node.getLeft(), key, data));
        else if (cmp > 0) node.setRight(insertHelper(node.getRight(), key, data));
        return node;
    }
    
//    public void display() {
//        displayHelper(root);
//    }
//    
//    private void displayHelper(Node root) {
//        if (root != null) {
//            displayHelper(root.left);
//            System.out.println(root.data);
//            displayHelper(root.right); // display in non-increasing order
//        }
//    }
    
    @Override
    public T search(K key) {
        return searchHelper(root, key);
    }
    
    private T searchHelper(Node<K, T> node, K key) {
        if (node == null) return null;
        int cmp = key.compareTo(node.getKey());
        if (cmp == 0) return node.getData();
        return (cmp < 0) ? searchHelper(node.getLeft(), key) : searchHelper(node.getRight(), key);
    }
    
    @Override
    public void delete(K key) {
        root = deleteHelper(root, key);
    }
    
    public Node<K, T> deleteHelper(Node<K, T> node, K key) {
        if (node == null) return root;
        int cmp = key.compareTo(node.getKey());
        if (cmp < 0) {
            node.setLeft(deleteHelper(node.getLeft(), key));
        } else if (cmp > 0) {
            node.setRight(deleteHelper(node.getRight(), key));
        } else {
            if (node.getLeft() == null && node.getRight() == null) {
                size--;
                return null;
            } else if (node.getRight() != null) {
                Node<K,T> successor = findMin(node.getRight());
                node.setKey(successor.getKey());
                node.setData(successor.getData());
                node.setRight(deleteHelper(node.getLeft(), successor.getKey()));
            } else {
                Node<K,T> predecessor = findMax(node.getLeft());
                node.setKey(predecessor.getKey());
                node.setData(predecessor.getData());
                node.setLeft(deleteHelper(node.getLeft(), predecessor.getKey()));
            }
        }
        return node;
    }
    
    private Node<K, T> findMin(Node<K, T> node) {
        while (node.getLeft() != null) node = node.getLeft();
        return node;
    }
    
    private Node<K, T> findMax(Node<K, T> node) {
        while (node.getRight() != null) node = node.getRight();
        return node;
    }
    
    @Override
    public boolean isEmpty() {
        return root == null;
    }
    
    @Override 
    public int size() {
        return size;
    }
    
    public void inorderTraversal(BstVisitor<T> visitor) {
        inorderHelper(root, visitor);
    }
    
    private void inorderHelper(Node<K, T> node, BstVisitor<T> visitor) {
        if (node != null) {
            inorderHelper(node.getLeft(), visitor);
            visitor.visit(node.getData());
            inorderHelper(node.getRight(), visitor);
        }
    }
}
