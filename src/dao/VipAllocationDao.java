package dao;

import adt.bst.PriorityHeapInterface;
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
    private final String allocationFileName = "roomAllocations.dat";

    public void saveGuests(PriorityHeapInterface<VipGuest> heap) {
        try (ObjectOutputStream output = new ObjectOutputStream(
                new FileOutputStream(guestFileName))) {

            output.writeInt(heap.getNumberOfEntries());
            for (int i = 0; i < heap.getNumberOfEntries(); i++) {
                output.writeObject(heap.getEntry(i));
            }
        } catch (IOException ex) {
            System.out.println("Unable to save VIP guest data.");
        }
    }

    public void loadGuests(PriorityHeapInterface<VipGuest> heap) {
        File file = new File(guestFileName);
        if (!file.exists()) {
            return;
        }

        try (ObjectInputStream input = new ObjectInputStream(
                new FileInputStream(file))) {

            int total = input.readInt();
            for (int i = 0; i < total; i++) {
                VipGuest guest = (VipGuest) input.readObject();
                heap.add(guest);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("VIP guest data file not found.");
        } catch (EOFException ex) {
            System.out.println("VIP guest data file ended unexpectedly.");
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Unable to load VIP guest data.");
        }
    }

    public void saveAllocations(RoomAllocation[] allocations, int allocationCount) {
        try (ObjectOutputStream output = new ObjectOutputStream(
                new FileOutputStream(allocationFileName))) {

            output.writeInt(allocationCount);
            for (int i = 0; i < allocationCount; i++) {
                output.writeObject(allocations[i]);
            }
        } catch (IOException ex) {
            System.out.println("Unable to save room allocation data.");
        }
    }

    public int loadAllocations(RoomAllocation[] allocations) {
        File file = new File(allocationFileName);
        if (!file.exists()) {
            return 0;
        }

        int count = 0;
        try (ObjectInputStream input = new ObjectInputStream(
                new FileInputStream(file))) {

            int total = input.readInt();
            while (count < total && count < allocations.length) {
                allocations[count] = (RoomAllocation) input.readObject();
                count++;
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Unable to load room allocation data.");
        }
        return count;
    }
}