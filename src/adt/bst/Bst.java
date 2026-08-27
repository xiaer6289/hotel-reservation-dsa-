/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt.bst;

/**
 * Generic Binary Search Tree implementation.
 *
 * The tree separates the ordering key (K) from the stored business object (T).
 * In the Front Desk module, K is the booking confirmation number and T is the
 * Booking. Insertion and deletion use recursion because each operation works on
 * a subtree, while search is iterative to avoid unnecessary call-stack usage.
 * 
 * @author Lee Cheng Xuan
 */
public class Bst<K extends Comparable<? super K>, T>
        implements BstInterface<K, T> {

    private Node<K, T> root;
    private int size;

    public Bst() {
        root = null;
        size = 0;
    }

    @Override
    public void insert(K key, T data) {
        requireKey(key);
        root = insertHelper(root, key, data);
    }

    /**
     * Recursive insertion. Duplicate keys replace their associated data so the
     * BST continues to contain one node per unique key.
     */
    private Node<K, T> insertHelper(Node<K, T> node, K key, T data) {
        if (node == null) {
            size++;
            return new Node<>(key, data);
        }

        int comparison = key.compareTo(node.getKey());

        if (comparison < 0) {
            node.setLeft(insertHelper(node.getLeft(), key, data));
        } else if (comparison > 0) {
            node.setRight(insertHelper(node.getRight(), key, data));
        } else {
            // Unique-key BST semantics: update the existing value.
            node.setData(data);
        }

        return node;
    }

    @Override
    public T search(K key) {
        requireKey(key);

        // Iterative search demonstrates the same BST ordering without recursion.
        Node<K, T> current = root;

        while (current != null) {
            int comparison = key.compareTo(current.getKey());

            if (comparison == 0) {
                return current.getData();
            }

            current = comparison < 0 ? current.getLeft() : current.getRight();
        }

        return null;
    }

    @Override
    public void delete(K key) {
        requireKey(key);
        root = deleteHelper(root, key);
    }

    /**
     * Recursive BST deletion supporting all three cases:
     * 1) leaf, 2) one child, and 3) two children.
     *
     * For two children, the node is replaced by the in-order successor (the
     * smallest node in the right subtree), then that successor is removed from
     * the right subtree. The size is therefore reduced exactly once.
     */
    private Node<K, T> deleteHelper(Node<K, T> node, K key) {
        if (node == null) {
            return null;
        }

        int comparison = key.compareTo(node.getKey());

        if (comparison < 0) {
            node.setLeft(deleteHelper(node.getLeft(), key));
            return node;
        }

        if (comparison > 0) {
            node.setRight(deleteHelper(node.getRight(), key));
            return node;
        }

        // Key found: zero or one child.
        if (node.getLeft() == null) {
            size--;
            return node.getRight();
        }

        if (node.getRight() == null) {
            size--;
            return node.getLeft();
        }

        // Two children: replace with the in-order successor from RIGHT subtree.
        Node<K, T> successor = findMin(node.getRight());
        node.setKey(successor.getKey());
        node.setData(successor.getData());
        node.setRight(deleteHelper(node.getRight(), successor.getKey()));
        return node;
    }

    private Node<K, T> findMin(Node<K, T> node) {
        Node<K, T> current = node;
        while (current.getLeft() != null) {
            current = current.getLeft();
        }
        return current;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    @Override
    public void inorderTraversal(BstVisitor<T> visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("BST visitor cannot be null.");
        }
        inorderHelper(root, visitor);
    }

    private void inorderHelper(Node<K, T> node, BstVisitor<T> visitor) {
        if (node == null) {
            return;
        }

        inorderHelper(node.getLeft(), visitor);
        visitor.visit(node.getData());
        inorderHelper(node.getRight(), visitor);
    }

    private void requireKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("BST key cannot be null.");
        }
    }
}