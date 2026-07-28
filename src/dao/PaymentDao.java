/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import entity.Payment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;

/**
 *
 * @author Lee Cheng Xuan
 */
public class PaymentDao {
    private String fileName = "room.dat";
    
    public void saveToFile(Payment[] payment) {
        File file = new File(fileName);
        System.out.println("saving to: " + file.getAbsolutePath());
        try {
            ObjectOutputStream ooStream = new ObjectOutputStream(new FileOutputStream(file));
            ooStream.writeObject(payment);
            ooStream.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("\ncannot save to " + fileName);
            ex.printStackTrace();
        }
    }
    
    public Payment[] retrieveFromFile() {
        File file = new File(fileName);
        Payment[] payments = new Payment[0];
        try {
            ObjectInputStream oiStream = new ObjectInputStream(new FileInputStream(file));
            payments = (Payment[]) (oiStream.readObject());
            oiStream.close();
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("\nCannot read from " + fileName);
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            System.out.println("\nclass not found");
            ex.printStackTrace();
        } finally {
            return payments;
        }
    }
    
    public Payment[] loadOrSeed() {
        File file = new File(fileName);
        if (file.exists()) {
            Payment[] loaded = retrieveFromFile();
            if (loaded.length > 0) return loaded;
        }
        return seedSampleData();
    }
    
    public Payment[] seedSampleData() {
        return new Payment[] {
            new Payment("PAY001", 450.00, LocalDateTime.now(), 'C'),
            new Payment("PAY002", 360.00, LocalDateTime.now(), 'C'),
            new Payment("PAY003", 540.00, LocalDateTime.now(), 'C'),
            new Payment("PAY004", 660.00, LocalDateTime.now(), 'C'),
            new Payment("PAY005", 260.00, LocalDateTime.now(), 'C')
        };
    }
}
