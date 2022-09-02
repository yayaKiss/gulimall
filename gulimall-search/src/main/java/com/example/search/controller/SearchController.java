package com.example.search.controller;

import com.example.search.service.MallSearchService;
import com.example.search.vo.SearchParam;
import com.example.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String list(SearchParam searchParam, Model model, HttpServletRequest request){
        //拿到页面请求后面的条件字符串
        String queryString = request.getQueryString();

        searchParam.set_queryString(queryString);
        SearchResult result = mallSearchService.search(searchParam);
        model.addAttribute("result",result);

        return "list";
    }
}
