package control;

import adt.linear.QueueInterface;
import adt.linear.RegistrationQueue;
import entity.WalkInRegistration;

/**
 *
 * @author Lai Jen Feng
 */
public class RegistrationController {

    private final QueueInterface<WalkInRegistration> registrationQueue;

    public RegistrationController() {
        registrationQueue = new RegistrationQueue<>();
    }

    public void addRegistration(WalkInRegistration registration) {
        registrationQueue.enqueue(registration);
    }

    public int getWaitingCount() {
        return registrationQueue.getNumberOfEntries();
    }
}