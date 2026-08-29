/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entity;

/**
 *
 * @author Lai Jen Feng
 */
public enum RoomStatus {
    AVAILABLE('A', "Available", true),
    OCCUPIED('O', "Occupied", false),
    DIRTY('D', "Dirty", false),
    CLEANING_IN_PROGRESS('C', "Cleaning In Progress", false),
    INSPECTED('I', "Inspected", false),
    READY('R', "Ready", true);

    private final char code;
    private final String displayName;
    private final boolean assignable;

    RoomStatus(char code, String displayName, boolean assignable) {
        this.code = code;
        this.displayName = displayName;
        this.assignable = assignable;
    }

    public char getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAssignable() {
        return assignable;
    }

    public static RoomStatus fromCode(char code) {
        for (RoomStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }

        return AVAILABLE;
    }
}
