package com.teleport.loadplanner.businessobjects.loadoptimizer;

import com.teleport.loadplanner.businessobjects.OrderDTO;
import com.teleport.loadplanner.businessobjects.TruckDTO;

import java.util.List;

public class RequestDTO {

    private TruckDTO truck;

    private List<OrderDTO> orders;

    public RequestDTO() {}

    public TruckDTO getTruck() {
        return truck;
    }

    public void setTruck(TruckDTO truck) {
        this.truck = truck;
    }

    public List<OrderDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderDTO> orders) {
        this.orders = orders;
    }
}
