package com.teleport.loadplanner.api;

import com.teleport.loadplanner.businessobjects.loadoptimizer.RequestDTO;
import com.teleport.loadplanner.businessobjects.loadoptimizer.ResponseDTO;

public interface LoadOptimizerService
{
    ResponseDTO optimizeLoad(RequestDTO request) throws Exception;
}
