package com.example.search.service;

import com.example.search.vo.SearchParam;
import com.example.search.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam searchParam);
}
