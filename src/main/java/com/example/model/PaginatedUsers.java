package com.example.model;

import lombok.Data;

import java.util.List;

@Data
public class PaginatedUsers {
    private List<User> users;
    private String nextPageLink;
    private int totalUsers;
    private int totalPages;
}