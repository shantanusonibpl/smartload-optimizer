package com.teleport.loadplanner.impl.service;

import com.teleport.loadplanner.api.LoadOptimizerService;
import com.teleport.loadplanner.businessobjects.loadoptimizer.RequestDTO;
import com.teleport.loadplanner.businessobjects.loadoptimizer.ResponseDTO;
import com.teleport.loadplanner.businessobjects.OrderDTO;
import com.teleport.loadplanner.businessobjects.TruckDTO;
import com.teleport.loadplanner.impl.service.validator.LoadOptimizerRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LoadOptimizerServiceImpl implements LoadOptimizerService {

    private static final Logger logger = LoggerFactory.getLogger(LoadOptimizerServiceImpl.class);

    public ResponseDTO optimizeLoad(RequestDTO request) throws Exception
    {
        logger.info("Processing optimize load request for truck: {}, orders count: {}",
                request.getTruck() != null ? request.getTruck().getId() : "null",
                request.getOrders() != null ? request.getOrders().size() : 0);

        LoadOptimizerRequestValidator.validate(request);
        OptimalResult optimalResult = calculateOptimalResult(request.getOrders(),request.getTruck().getMaxWeightLbs(),request.getTruck().getMaxVolumeCuft());
        if(optimalResult == null)
        {
            logger.error("No optimal result found for truck: {}", request.getTruck().getId());
            throw new IllegalArgumentException("No optimal result found");
        }

        ResponseDTO response = createResponse(optimalResult,request.getTruck(),request.getOrders());
        logger.info("Optimization completed for truck: {}, selected orders: {}, total payout: {}, total weight: {}, total volume: {}",
                response.getTruckId(),
                response.getSelectedOrderIds().size(),
                response.getTotalPayoutCents(),
                response.getTotalWeightLbs(),
                response.getTotalVolumeCuft());

        return response;
    }

    private OptimalResult calculateOptimalResult(List<OrderDTO> orderDTOList ,Integer maxWeightLbs ,  Integer maxVolumeCuft )
    {
        int n = orderDTOList.size();
        int totalSubsets = 1 << n;

        OptimalResult  [] dp = new OptimalResult[totalSubsets];

        Long maxPayout = Long.MIN_VALUE;
        OptimalResult maxPayoutOptimalResult = null;

        for(int mask = 0; mask < totalSubsets; mask++)
        {
            OptimalResult previousOptimalResult = dp[mask];
            if((previousOptimalResult == null && mask != 0) || previousOptimalResult!=null && previousOptimalResult.isCombinationPossible() == Boolean.FALSE)
            {
                continue;
            }

            for(int i=0;i<n;i++)
            {
                if((mask & (1 << i)) == 0)
                {
                    OrderDTO crrentOrderDTO = orderDTOList.get(i);
                    int nextMask = mask | (1 << i);

                    if (previousOptimalResult == null)
                    {
                        if(isCurrentOrderValidForNexMask(crrentOrderDTO,maxWeightLbs, maxVolumeCuft) )
                        {
                          OptimalResult nextOptimalResult =  new OptimalResult(crrentOrderDTO,nextMask);
                          dp[nextMask] = nextOptimalResult;

                          if(nextOptimalResult.getTotalPayout() > maxPayout)
                          {
                            maxPayout = nextOptimalResult.getTotalPayout();
                            maxPayoutOptimalResult = nextOptimalResult;
                          }
                        }
                        else
                        {
                            continue;
                        }
                    }
                    else if (isOrderValidForNexMask(previousOptimalResult,crrentOrderDTO,maxWeightLbs, maxVolumeCuft))
                    {
                        OptimalResult nextOptimalResult = previousOptimalResult.clone();
                        nextOptimalResult.addPayout(crrentOrderDTO.getPayoutCents());
                        nextOptimalResult.addWeight(crrentOrderDTO.getWeightLbs());
                        nextOptimalResult.addVolume(crrentOrderDTO.getVolumeCuft());
                        nextOptimalResult.setMask(nextMask);
                        dp[nextMask] = nextOptimalResult;
                        if(nextOptimalResult.getTotalPayout() > maxPayout)
                        {
                            maxPayout = nextOptimalResult.getTotalPayout();
                            maxPayoutOptimalResult = nextOptimalResult;
                        }
                    }
                    else
                    {
                        OptimalResult nextOptimalResult = previousOptimalResult.clone();
                        nextOptimalResult.setCombinationPossible(Boolean.FALSE);
                        nextOptimalResult.setMask(nextMask);
                        dp[nextMask] = nextOptimalResult;
                    }
                }

            }
        }

        return maxPayoutOptimalResult;
    }

    private boolean isOrderValidForNexMask(OptimalResult optimalResult, OrderDTO currentOrderDTO , Integer maxWeightLbs ,  Integer maxVolumeCuft)
    {
        if(optimalResult.getTotalVolume() + currentOrderDTO.getVolumeCuft()  >= maxVolumeCuft)
        {
            return false;
        }
        else if (optimalResult.getTotalWeight() + currentOrderDTO.getWeightLbs()  > maxWeightLbs)
        {
            return false;
        }
        else if (LocalDate.parse(currentOrderDTO.getPickupDate()).isAfter(LocalDate.parse(currentOrderDTO.getDeliveryDate())))
        {
            return false;
        }
        else if (Boolean.compare(currentOrderDTO.getIsHazmat(),optimalResult.isContainHazmatOrder())!=0)
        {
            return false;
        }
        else if (!optimalResult.getOrigin().equals(currentOrderDTO.getOrigin()) || !optimalResult.getDestination().equals(currentOrderDTO.getDestination()))
        {
            return false;
        }
        else if (optimalResult.isCombinationPossible() == Boolean.FALSE)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    private boolean isCurrentOrderValidForNexMask(OrderDTO currentOrderDTO , Integer maxWeightLbs ,  Integer maxVolumeCuft)
    {
        if( currentOrderDTO.getVolumeCuft()  > maxVolumeCuft)
        {
            return false;
        }
        else if (currentOrderDTO.getWeightLbs()  > maxWeightLbs)
        {
            return false;
        }
        else if (LocalDate.parse(currentOrderDTO.getPickupDate()).isAfter(LocalDate.parse(currentOrderDTO.getDeliveryDate())))
        {
            return false;
        }
        else
        {
            return true;
        }
    }


    private ResponseDTO createResponse(OptimalResult result, TruckDTO truck, List<OrderDTO> orderDTOList) {
        double weightUtilization = truck.getMaxWeightLbs() > 0 ?
                (result.totalWeight * 100.0 / truck.getMaxWeightLbs()) : 0.0;
        double volumeUtilization = truck.getMaxVolumeCuft() > 0 ?
                (result.totalVolume * 100.0 / truck.getMaxVolumeCuft()) : 0.0;

        // Round to 2 decimal places
        weightUtilization = Math.round(weightUtilization * 100.0) / 100.0;
        volumeUtilization = Math.round(volumeUtilization * 100.0) / 100.0;
        List<String> orderIds = new ArrayList<>();
        for (int i = 0; i < orderDTOList.size(); i++)
        {
            if ((result.getMask() & (1 << i)) != 0) {
                orderIds.add(orderDTOList.get(i).getId());
            }
        }

        return new ResponseDTO(
                truck.getId(),
                orderIds,
                result.totalPayout,
                result.totalWeight,
                result.totalVolume,
                weightUtilization,
                volumeUtilization
        );
    }

    private static class OptimalResult {
        private int mask;
        private Long totalPayout = 0l ;
        private Long totalWeight = 0l;
        private Long totalVolume = 0l ;
        private Boolean containHazmatOrder ;
        private Boolean combinationPossible ;

        public String getOrigin() {
            return origin;
        }

        public String getDestination() {
            return destination;
        }

        private String origin;
        private String destination;

        OptimalResult(OrderDTO orderDTO,int mask)
        {
            this.mask = mask;
            this.totalPayout = this.totalPayout + orderDTO.getPayoutCents();
            this.totalWeight = this.totalWeight + orderDTO.getWeightLbs();
            this.totalVolume = this.totalVolume + orderDTO.getVolumeCuft();
            this.containHazmatOrder = orderDTO.getIsHazmat();
            this.combinationPossible = Boolean.TRUE;
            this.origin = orderDTO.getOrigin();
            this.destination = orderDTO.getDestination();
        }

        public int getMask() {
            return this.mask;
        }

        public void setMask(int mask) {
            this.mask = mask;
        }

        public boolean isCombinationPossible()
        {
            return this.combinationPossible;
        }

        public void setCombinationPossible(Boolean combinationPossible)
        {
            this.combinationPossible = combinationPossible;
        }

        public Long getTotalPayout() {
            return this.totalPayout;
        }

        public void addPayout(Integer payout) {
            this.totalPayout += payout;
        }

        public Long getTotalWeight() {
            return totalWeight;
        }

        public void addWeight(Integer weight) {
            this.totalWeight += weight;
        }

        public Long getTotalVolume() {
            return totalVolume;
        }

        public void addVolume(Integer volume) {
            this.totalVolume += volume;
        }

        public Boolean isContainHazmatOrder() {
            return this.containHazmatOrder;
        }

        public OptimalResult clone() {
            OptimalResult cloned = new OptimalResult();
            cloned.totalPayout = this.totalPayout;
            cloned.totalWeight = this.totalWeight;
            cloned.totalVolume = this.totalVolume;
            cloned.containHazmatOrder = this.containHazmatOrder;
            cloned.combinationPossible = this.combinationPossible;
            cloned.mask = this.mask;
            cloned.origin = this.origin;
            cloned.destination = this.destination;
            return cloned;
        }

        OptimalResult() {
            this.totalPayout = 0L;
            this.totalWeight = 0L;
            this.totalVolume = 0L;
            this.containHazmatOrder = false;
            this.combinationPossible = false;
        }
    }
}
