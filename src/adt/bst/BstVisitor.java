/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt.bst;

/** Callback used by BST traversal so report/control code can process stored
 * objects without exposing the BST's internal Node structure.
 * @author Lee Cheng Xuan
 */
@FunctionalInterface
public interface BstVisitor<T> {
    void visit(T data);
}
