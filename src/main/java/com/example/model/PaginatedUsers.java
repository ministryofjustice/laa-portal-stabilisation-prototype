package com.example.model;

import lombok.Data;

import java.util.List;

@Data
public class PaginatedUsers {
    private List<UserModel> users;
    private String previousPageLink;
    private String nextPageLink;
    private int totalUsers;
}