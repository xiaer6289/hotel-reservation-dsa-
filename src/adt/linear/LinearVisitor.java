package adt.linear;

/**
 * Callback for sequential traversal of a LinearADT without exposing internal
 * nodes or requiring repeated indexed access.
 *
 * @param <T> element type visited
 * @author Low Wei Shin
 */
@FunctionalInterface
public interface LinearVisitor<T> {
    void visit(T data);
}