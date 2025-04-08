package com.example.model;

import com.microsoft.graph.models.ServicePrincipal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServicePrincipalModel extends ServicePrincipal {
    private boolean selected = false;
}
