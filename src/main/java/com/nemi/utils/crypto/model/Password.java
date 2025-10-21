package com.nemi.utils.crypto.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@NoArgsConstructor(force = true)
@Value
public class Password {
    String value;
}
