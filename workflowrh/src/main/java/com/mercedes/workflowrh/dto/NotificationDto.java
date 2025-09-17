// src/main/java/com/mercedes/workflowrh/dto/NotificationDto.java
package com.mercedes.workflowrh.dto;

import com.mercedes.workflowrh.entity.StatutNotification;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String subject;
    private String message;
    private StatutNotification statut;
    private LocalDateTime dateCreation;
}
