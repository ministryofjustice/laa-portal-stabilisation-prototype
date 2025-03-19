package com.example.dto;


import lombok.Data;

import java.util.List;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class OfficeData {
    private List<String> selectedOffices;
}
