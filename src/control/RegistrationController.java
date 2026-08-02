package control;

import adt.linear.DoublyLinkedList;
import adt.linear.LinearADT;
import entity.WalkInRegistration;

/**
 *
 * @author Lai Jen Feng
 */
public class RegistrationController {

    private final LinearADT<WalkInRegistration> registrationQueue;

    public RegistrationController() {
        registrationQueue = new DoublyLinkedList<>();
    }

    public void addRegistration(WalkInRegistration registration) {
        registrationQueue.addLast(registration);
    }

    public int getWaitingCount() {
        return registrationQueue.size();
    }

    public WalkInRegistration getRegistrationAt(int index) {
        if (index < 0 || index >= registrationQueue.size()) {
            return null;
        }

        return registrationQueue.get(index);
    }

    public WalkInRegistration getNextRegistration() {
        if (registrationQueue.isEmpty()) {
            return null;
        }

        return registrationQueue.get(0);
    }

    public WalkInRegistration processNextRegistration() {
        if (registrationQueue.isEmpty()) {
            return null;
        }

        return registrationQueue.removeFirst();
    }
}