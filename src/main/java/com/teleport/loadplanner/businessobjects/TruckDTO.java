package com.teleport.loadplanner.businessobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.teleport.loadplanner.common.LoadPlannerConstants;

public class TruckDTO {

    @JsonProperty(LoadPlannerConstants.ID)
    private String id;

    @JsonProperty(LoadPlannerConstants.MAX_WEIGHT_LBS)
    private Integer maxWeightLbs;

    @JsonProperty(LoadPlannerConstants.MAX_VOLUME_CUFT)
    private Integer maxVolumeCuft;

    public TruckDTO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getMaxWeightLbs() {
        return maxWeightLbs;
    }

    public void setMaxWeightLbs(Integer maxWeightLbs) {
        this.maxWeightLbs = maxWeightLbs;
    }

    public Integer getMaxVolumeCuft() {
        return maxVolumeCuft;
    }

    public void setMaxVolumeCuft(Integer maxVolumeCuft) {
        this.maxVolumeCuft = maxVolumeCuft;
    }
}
