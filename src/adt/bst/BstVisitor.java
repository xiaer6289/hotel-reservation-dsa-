/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package adt.bst;

/** visitor enhance reusability by using same inorderTraversal without touching ADT
 *
 * @author Lee Cheng Xuan
 */
public interface BstVisitor<T> {
    void visit(T data);
}
