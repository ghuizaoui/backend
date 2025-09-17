package com.mercedes.workflowrh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KpiDTO {
    private String label;
    private Long value;
}
