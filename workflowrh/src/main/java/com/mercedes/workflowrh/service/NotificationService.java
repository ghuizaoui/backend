    package com.mercedes.workflowrh.service;

    import com.mercedes.workflowrh.entity.Demande;

    public interface NotificationService {
        void notifyManagerOfNewDemand(Demande d);
        void notifyEmployeeOnValidation(Demande d);
        void notifyEmployeeOnRefuse(Demande d);
        void markAsRead(Long notifId, String matricule);
        void notifyEmployeeOnCreation(Demande d);

    }
