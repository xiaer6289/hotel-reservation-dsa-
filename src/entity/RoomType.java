/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entity;

/**
 *
 * @author User
 */
public enum RoomType {
    DELUXE(150.00),
    DELUXE_TWIN(180.00),
    SUPERIOR(220.00),
    SUPERIOR_TWIN(260.00);
    
    private final double pricePerDay;
    
    RoomType(double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }
    
    public double getPricePerDay() {
        return pricePerDay;
    }
}
