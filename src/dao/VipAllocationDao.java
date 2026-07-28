package dao;

import entity.RoomAllocation;
import entity.VipGuest;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author Low Enn Toong
 */
public class VipAllocationDao {

    private final String guestFileName = "vipGuests.dat";
    private final String allocationFileName ="roomAllocations.dat";

    public void saveGuests(VipGuest[] guests, int guestCount) {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(guestFileName))) {
            output.writeInt(guestCount);
            for (int i = 0; i < guestCount; i++) {
                output.writeObject(guests[i]);
            }
        } catch (IOException ex) {
            System.out.println("Unable to save VIP guest data.");
            ex.printStackTrace();
        }
    }

    public int loadGuests(VipGuest[] guests) {
        File file = new File(guestFileName);
        if (!file.exists()) {
            return 0;
        }
        int count = 0;
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            int total = input.readInt();
            while (count < total && count < guests.length) {
                guests[count] = (VipGuest) input.readObject();
                count++;
            }
        } catch (FileNotFoundException ex) {
            System.out.println("VIP guest data file not found.");
        } catch (EOFException ex) {
            System.out.println("VIP guest data file ended unexpectedly.");
        } catch (IOException ex) {
            System.out.println("Unable to load VIP guest data.");
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            System.out.println("VIP guest class not found.");
            ex.printStackTrace();
        }
        return count;
    }

    public void saveAllocations(RoomAllocation[] allocations, int allocationCount) {
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(allocationFileName))) {
            output.writeInt(allocationCount);
            for (int i = 0; i < allocationCount; i++) {
                output.writeObject(allocations[i]);
            }
        } catch (IOException ex) {
            System.out.println("Unable to save room allocation data.");
            ex.printStackTrace();
        }
    }

    public int loadAllocations(RoomAllocation[] allocations) {
        File file = new File(allocationFileName);
        if (!file.exists()) {
            return 0;
        }
        int count = 0;
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            int total = input.readInt();
            while (count < total && count < allocations.length) {
                allocations[count] = (RoomAllocation) input.readObject();
                count++;
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Room allocation file not found.");
        } catch (EOFException ex) {
            System.out.println("Room allocation file ended unexpectedly.");
        } catch (IOException ex) {
            System.out.println("Unable to load room allocation data.");
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            System.out.println("Room allocation class not found.");
            ex.printStackTrace();
        }
        return count;
    }
}