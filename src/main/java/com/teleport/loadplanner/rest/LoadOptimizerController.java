package com.teleport.loadplanner.rest;

import com.teleport.loadplanner.businessobjects.loadoptimizer.RequestDTO;
import com.teleport.loadplanner.businessobjects.loadoptimizer.ResponseDTO;
import com.teleport.loadplanner.impl.service.LoadOptimizerServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/load-optimizer")
public class LoadOptimizerController {

    private static final Logger logger = LoggerFactory.getLogger(LoadOptimizerController.class);

    @Autowired
    private LoadOptimizerServiceImpl loadOptimizerService;

    @PostMapping("/optimize")
    public ResponseEntity<ResponseDTO> optimizeLoad(@RequestBody RequestDTO request) throws Exception {
        logger.info("Received optimize load request for truck: {}", request.getTruck() != null ? request.getTruck().getId() : "null");
        ResponseDTO response = loadOptimizerService.optimizeLoad(request);
        logger.info("Returning optimize load response for truck: {}, selected orders: {}", response.getTruckId(), response.getSelectedOrderIds().size());
        return ResponseEntity.ok(response);
    }
}