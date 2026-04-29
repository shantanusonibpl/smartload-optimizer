package com.teleport.loadplanner.impl.service.validator;

import com.teleport.loadplanner.businessobjects.loadoptimizer.RequestDTO;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Objects;

public class LoadOptimizerRequestValidator
{
    public static void validate(RequestDTO requestDTO) throws Exception {
        if(Objects.isNull(requestDTO.getTruck()))
        {
            throw new IllegalArgumentException("Truck information is missing");
        }
        if(Objects.isNull(requestDTO.getOrders()))
        {
            throw new IllegalArgumentException("Order information is missing");
        }
        if(requestDTO.getOrders().size() > 60){
            throw new MaxUploadSizeExceededException(60);
        }
    }
}
