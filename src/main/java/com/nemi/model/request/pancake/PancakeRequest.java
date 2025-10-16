package com.nemi.model.request.pancake;

import com.nemi.configuration.PancakeConfig;
import com.nemi.constant.PancakeConstatns;
import com.nemi.utils.PosUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
@Slf4j
public class PancakeRequest {

    private String shopId;       // ID shop
    private String apiKey;       // API key

    private int pageSize;        // mặc định 30
    private int pageNumber;      // số trang, mặc định 1

    //    private String search;       // tìm theo tên / keyword
//    private String sellingStatus;   // none, bad, normal, star
    private String productStatus;   // locked, not_locked
    private Long startUnix;
    private Long endUnix;

//    private List<String> categoryIds;    // lọc theo category
//    private Boolean isFilterCategoriesByOr; // true = OR, false = AND

//    private LocalDateTime warehouseUntil; // snapshot tồn kho tại thời điểm

//    private String promotionAdvance; // id khuyến mãi hoặc [has_promotion]/[no_promotion]

//    private List<String> manipulationWarehouses; // danh sách kho để lọc

    //    private Long startDate;
//    private Long endDate;
//    private Long startTimeUpdate;
//    private Long endTimeUpdate;
//
//    private String includedComposite; // parent / children
//    private List<String> variationIds; // chỉ lấy các variation cụ thể
    public static PancakeRequest buildRequest(String config, String accesToken, int pageStartNumber, int productBatchSize, LocalDateTime fromdate) {

        Map<String, String> configMap = PosUtils.convertToConfigMap(config);

        String shopId = configMap.get(PancakeConstatns.SHOP_ID);

        int pageNumber = pageStartNumber;

        long startUnix = PosUtils.toEpochSecond(fromdate.minusDays(PancakeConfig.getRecentDaysStatic()));
        long endUnix = PosUtils.toEpochSecond(fromdate);

        log.info("start time unix: {}, end time unix: {}", startUnix, endUnix);

        return PancakeRequest.builder()
                .apiKey(accesToken)
                .pageNumber(pageNumber)
                .pageSize(productBatchSize)
                .shopId(shopId)
                .endUnix(endUnix)
                .startUnix(startUnix)
                .build();
    }
}
