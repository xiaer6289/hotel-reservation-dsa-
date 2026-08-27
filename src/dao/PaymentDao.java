/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import entity.Payment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

/**
 *
 * @author Lee Cheng Xuan
 */
public class PaymentDao {
    private String fileName = "payment.dat";
    
    public void saveToFile(Payment[] payments) {
        File file = new File(fileName);
        System.out.println("saving to: " + file.getAbsolutePath());
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (Payment payment : payments) {
                if (payment == null) continue;
                writer.println(payment.getPaymentId() + "|" + payment.getAmount() + "|"
                        + payment.getDateTime() + "|" + payment.getStatus());
            }
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
        Payment[] temp = new Payment[100];
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 4) continue;
                temp[count++] = new Payment(parts[0], Double.parseDouble(parts[1]),
                        LocalDateTime.parse(parts[2]), parts[3].charAt(0));
            }
        } catch (FileNotFoundException ex) {
            System.out.println("\n" + fileName + " not found");
        } catch (IOException ex) {
            System.out.println("\nCannot read from " + fileName);
            ex.printStackTrace();
        }
        Payment[] payments = new Payment[count];
        System.arraycopy(temp, 0, payments, 0, count);
        return payments;
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
            new Payment("PAY005", 260.00, LocalDateTime.now(), 'C'),
            new Payment("PAY006", 320.00, LocalDateTime.now(), 'P'),
            new Payment("PAY007", 480.00, LocalDateTime.now(), 'C'),
            new Payment("PAY008", 220.00, LocalDateTime.now(), 'X'),
            new Payment("PAY009", 610.00, LocalDateTime.now(), 'R'),
            new Payment("PAY010", 390.00, LocalDateTime.now(), 'P')
        };
    }
}
