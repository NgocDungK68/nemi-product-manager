package com.nemi.model.request.pancake;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PancakeRequest {
    private String shopId;       // ID shop
    private String apiKey;       // API key

    private int pageSize;        // mặc định 30
    private int pageNumber;      // số trang, mặc định 1

//    private String search;       // tìm theo tên / keyword
//    private String sellingStatus;   // none, bad, normal, star
    private String productStatus;   // locked, not_locked

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
}
