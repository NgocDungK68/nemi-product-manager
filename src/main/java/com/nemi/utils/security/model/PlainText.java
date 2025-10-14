package com.nemi.utils.security.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@NoArgsConstructor(force = true)
@Value
public class PlainText {
    String value;
}
