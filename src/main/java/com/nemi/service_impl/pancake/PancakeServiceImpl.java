package com.nemi.service_impl.pancake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.model.response.nhanhvn.managershop.ShopResponse;
import com.nemi.model.response.nhanhvn.saleanalytics.SaleAnalyticsResponse;
import com.nemi.model.response.pancake.PancakeApiResponse;
import com.nemi.model.response.pancake.PancakeProductsResponse;
import com.nemi.model.response.pancake.PancakeResponse;
import com.nemi.service.pancake.PancakeService;
import com.nemi.util.MapConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Service
public class PancakeServiceImpl implements PancakeService {
    private static final Logger log = LoggerFactory.getLogger(PancakeServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    @Value("${pancake.base-url}")
    private String BASE_URL;
    @Value("${pancake.api-key}")
    private String API_KEY;
    @Value("${pancake.shop-id}")
    private String SHOP_ID;
    @Override
    public Optional<PancakeResponse> getOrderDetail(String shopId, String orderId, Map<String, String> queryParams) {
        log.info("[PancakeServiceImpl.getOrderDetail] shopId: {}, orderId: {}, queryParams: {}", shopId, orderId, queryParams);

        try {
            // step1: setup request
            queryParams.put("api_key", API_KEY);
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL + shopId + "/orders/" + orderId)
                    .queryParams(MapConvert.convertToMultiValueMap(queryParams));

            // step2: call API
            String jsonResp = restTemplate.getForObject(builder.toUriString(), String.class, queryParams);

            // step3: parse response to object and map to simple response
            PancakeApiResponse apiResponse = objectMapper.readValue(jsonResp, PancakeApiResponse.class);
            if (apiResponse.getData() == null) {
                throw new Exception("Order not found or empty response");
            }

            PancakeResponse pancakeResponse = mapToSimpleResponse(apiResponse.getData());
            return Optional.of(pancakeResponse);

        } catch (Exception e) {
            log.error("Get order detail failed for orderId {}: {}", orderId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<ShopResponse> getShopInfo(Map<String, String> queryParams) {
        log.info("[PancakeServiceImpl.getShopInfo] queryParams: {}", queryParams);

        try {
            //step 1: setup request
            queryParams.put("api_key", API_KEY);
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL)
                    .queryParams(MapConvert.convertToMultiValueMap(queryParams));

            //step 2: call API
            String jsonResp = restTemplate.getForObject(builder.toUriString(), String.class, queryParams);

            //Step 3: parse response
            ShopResponse shopResponse = objectMapper.readValue(jsonResp, ShopResponse.class);
            return Optional.of(shopResponse);
        } catch (Exception e) {
            log.error("Get shop info failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<SaleAnalyticsResponse> getSaleAnalytics(Map<String, String> queryParams) {
        log.info("[PancakeServiceImpl.getSaleAnalytics] queryParams: {}", queryParams);

        try {
            // step1: setup request
            queryParams.put("api_key", API_KEY);
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL + SHOP_ID + "/analytics/sale")
                    .queryParams(MapConvert.convertToMultiValueMap(queryParams));

            // step2: call API
            String jsonResp = restTemplate.getForObject(builder.toUriString(), String.class, queryParams);

            // step3: parse response to object
            SaleAnalyticsResponse saleAnalyticsResponse = objectMapper.readValue(jsonResp, SaleAnalyticsResponse.class);
            return Optional.of(saleAnalyticsResponse);



//            JsonNode rootNode = mapper.readTree(jsonResp);
//            JsonNode dataNode = rootNode.path("data");
//            if (dataNode.isMissingNode()) {
//                throw new Exception("Data node is missing in response");
//            }
//
//            SaleAnalyticsResponse analyticsResponse = mapper.treeToValue(dataNode, SaleAnalyticsResponse.class);
//            return Optional.of(analyticsResponse);

        } catch (Exception e) {
            log.error("Get sale analytics failed for : {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private PancakeResponse mapToSimpleResponse(PancakeApiResponse.OrderData data) {
        PancakeResponse response = new PancakeResponse();

        // Simple mapping - just copy the values
        response.setOrderId(data.getId());
        response.setOrderCode(data.getSystemId());
        response.setStatus(data.getStatusName());
        response.setTotalPrice(data.getTotalPrice());
        response.setShippingFee(data.getShippingFee());
        response.setDiscountAmount(data.getTotalDiscount());
        response.setCreatedAt(data.getInsertedAt());
        response.setUpdatedAt(data.getUpdatedAt());

        // Customer info
        if (data.getCustomer() != null) {
            response.setCustomerName(data.getCustomer().getName());
            if (data.getCustomer().getEmails() != null && !data.getCustomer().getEmails().isEmpty()) {
                response.setCustomerEmail(data.getCustomer().getEmails().get(0));
            }
            if (data.getCustomer().getPhoneNumbers() != null && !data.getCustomer().getPhoneNumbers().isEmpty()) {
                response.setCustomerPhone(data.getCustomer().getPhoneNumbers().get(0));
            }
        }

        // Phone from bill_phone_number (priority)
        if (data.getBillPhoneNumber() != null) {
            response.setCustomerPhone(data.getBillPhoneNumber());
        }

        // Shipping address
        if (data.getShippingAddress() != null) {
            response.setShippingAddress(data.getShippingAddress().getFullAddress());
        }

        // Default values
        response.setPaymentMethod("COD");
        response.setShippingMethod("Standard");

        return response;
    }

    @Override
    public Optional<PancakeProductsResponse> getProducts(String shopId, Map<String, String> queryParams) {
        log.info("[PancakeServiceImpl.getProducts] shopId: {}, queryParams: {}", shopId, queryParams);

        try {
            // step1: setup request
            queryParams.put("api_key", API_KEY);
            String url = BASE_URL + shopId + "/products";
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(url)
                    .queryParams(MapConvert.convertToMultiValueMap(queryParams));

            String finalUrl = builder.toUriString();
            log.info("[PancakeServiceImpl.getProducts] Calling URL: {}", finalUrl);

            // step2: call API
            String jsonResp = restTemplate.getForObject(finalUrl, String.class);
            log.info("[PancakeServiceImpl.getProducts] Response received, length: {} chars",
                    jsonResp != null ? jsonResp.length() : 0);

            // step3: parse response to object
            PancakeProductsResponse productsResponse = objectMapper.readValue(jsonResp, PancakeProductsResponse.class);
            log.info("[PancakeServiceImpl.getProducts] Parsed response: {} products, page {}/{}",
                    productsResponse.getData() != null ? productsResponse.getData().size() : 0,
                    productsResponse.getPageNumber(),
                    productsResponse.getTotalPages());

            return Optional.of(productsResponse);

        } catch (Exception e) {
            log.error("Get products failed for shopId {}: {}", shopId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}