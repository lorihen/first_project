package com.pyg.web;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pyg.service.ItemSearchService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/itemSearch")
public class ItemSearchController {

    @Reference(timeout = 5000)
    private ItemSearchService itemSearchService;

    @RequestMapping("/search")
    public Map Search(@RequestBody Map searchMap) {
        return itemSearchService.search(searchMap);
    }
}
