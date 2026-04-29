package com.teleport.loadplanner.businessobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.teleport.loadplanner.common.LoadPlannerConstants;

import java.time.LocalDate;

public class OrderDTO {

    @JsonProperty(LoadPlannerConstants.ID)
    private String id;

    @JsonProperty(LoadPlannerConstants.PAYOUT_CENTS)
    private Integer payoutCents;

    @JsonProperty(LoadPlannerConstants.VOLUME_CUFT)
    private Integer volumeCuft;

    @JsonProperty(LoadPlannerConstants.WEIGHT_LBS)
    private Integer weightLbs;

    @JsonProperty(LoadPlannerConstants.ORIGIN)
    private String origin;

    @JsonProperty(LoadPlannerConstants.DESTINATION)
    private String destination;
    

    @JsonProperty(LoadPlannerConstants.PICKUP_DATE)
    private String pickupDate;
    

    @JsonProperty(LoadPlannerConstants.DELIVERY_DATE)
    private String deliveryDate;
    

    @JsonProperty(LoadPlannerConstants.IS_HAZMAT)
    private Boolean isHazmat;

    public OrderDTO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getPayoutCents() {
        return payoutCents;
    }

    public void setPayoutCents(Integer payoutCents) {
        this.payoutCents = payoutCents;
    }

    public Integer getWeightLbs() {
        return weightLbs;
    }

    public void setWeightLbs(Integer weightLbs) {
        this.weightLbs = weightLbs;
    }

    public Integer getVolumeCuft() {
        return volumeCuft;
    }

    public void setVolumeCuft(Integer volumeCuft) {
        this.volumeCuft = volumeCuft;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(String pickupDate) {
        this.pickupDate = pickupDate;
    }

    public String getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(String deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public Boolean getIsHazmat() {
        return isHazmat;
    }

    public void setIsHazmat(Boolean isHazmat) {
        this.isHazmat = isHazmat;
    }
}
