package com.nemi.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MapConvert {

    @SafeVarargs
    public static MultiValueMap<String, String> convertToMultiValueMap(Map<String, String>... maps) {
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        Arrays.stream(maps).forEach(map -> map.forEach(multiValueMap::add));
        return multiValueMap;
    }
}
