package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Stores one walk-in registration record.
 *
 * @author Lai Jen Feng
 */
public class WalkInRegistration implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_TIME_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String registrationId;
    private Guest guest;
    private String requestedRoomType;
    private int numberOfGuests;
    private LocalDateTime registrationTime;
    private LocalDateTime checkInDateTime;
    private LocalDateTime checkOutDateTime;
    private RegistrationStatus status;

    public WalkInRegistration(
            String registrationId,
            Guest guest,
            String requestedRoomType,
            int numberOfGuests,
            LocalDateTime checkInDateTime,
            LocalDateTime checkOutDateTime) {

        this.registrationId = registrationId;
        this.guest = guest;
        this.requestedRoomType = requestedRoomType;
        this.numberOfGuests = numberOfGuests;
        this.registrationTime = LocalDateTime.now();
        this.checkInDateTime = checkInDateTime;
        this.checkOutDateTime = checkOutDateTime;
        this.status = RegistrationStatus.WAITING;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public String getRequestedRoomType() {
        return requestedRoomType;
    }

    public void setRequestedRoomType(String requestedRoomType) {
        this.requestedRoomType = requestedRoomType;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public LocalDateTime getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(LocalDateTime registrationTime) {
        this.registrationTime = registrationTime;
    }

    public LocalDateTime getCheckInDateTime() {
        return checkInDateTime;
    }

    public void setCheckInDateTime(LocalDateTime checkInDateTime) {
        this.checkInDateTime = checkInDateTime;
    }

    public LocalDateTime getCheckOutDateTime() {
        return checkOutDateTime;
    }

    public void setCheckOutDateTime(LocalDateTime checkOutDateTime) {
        this.checkOutDateTime = checkOutDateTime;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Registration ID: " + registrationId
                + "\nGuest ID: " + guest.getGuestId()
                + "\nGuest Name: " + guest.getName()
                + "\nPhone Number: " + guest.getPhoneNo()
                + "\nRequested Room Type: " + requestedRoomType
                + "\nNumber of Guests: " + numberOfGuests
                + "\nRegistration Time: "
                + registrationTime.format(DATE_TIME_FORMAT)
                + "\nCheck-In Date Time: "
                + checkInDateTime.format(DATE_TIME_FORMAT)
                + "\nCheck-Out Date Time: "
                + checkOutDateTime.format(DATE_TIME_FORMAT)
                + "\nStatus: " + status;
    }
}