package com.pyg.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.pyg.pojo.TbItem;
import com.pyg.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemSearchServiceImpl implements ItemSearchService {
    @Autowired
    private SolrTemplate solrTemplate;

    /**
     * 搜索方法
     *
     * @param searchMap
     * @return
     */
    @Override
    public Map search(Map searchMap) {
        String keywords = (String) searchMap.get("keywords");
        searchMap.put("keywords", keywords.replace(" ", ""));
        System.out.println("搜索关键字为:"+keywords);
        Map map = new HashMap();
        //1. 查询列表
        map.putAll(searchList(searchMap));
        //2. 分组查询商品分类列表
        List<String> categoryList = searchCategoryList(searchMap);
        map.put("categoryList", categoryList);
        //3.查询品牌和规格列表
        String category = (String) searchMap.get("category");
        if (!category.equals("")) {
            //如果有分类名称
            map.putAll(searchBrandAndSpecList(category));
        } else {
            //如果没有分类名称
            //默认加载显示第一个分类的品牌和规格
            map.putAll(searchBrandAndSpecList(categoryList.get(0)));
        }
        return map;
    }

    public void importList(List list) {
        solrTemplate.saveBeans(list);
        solrTemplate.commit();
    }

    public void deleteByGoodsId(List goodsIds) {
        SimpleQuery query = new SimpleQuery("*:*");
        Criteria criteria = new Criteria("item_goodsid").in(goodsIds);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();
    }

    //查询列表
    public Map searchList(Map searchMap) {
        Map map = new HashMap();

        //高亮显示
        SimpleHighlightQuery query = new SimpleHighlightQuery();
        HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");//高亮域
        highlightOptions.setSimplePrefix("<em style = 'color:red'>");//前缀
        highlightOptions.setSimplePostfix("</em>");//后缀
        query.setHighlightOptions(highlightOptions);

        //1.1 关键字查询
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);

        //1.2 按照商品分类过滤
        if (!"".equals(searchMap.get("category"))) {
            //如果用户选择了商品分类
            FilterQuery filterQuery = new SimpleFilterQuery();
            Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
            filterQuery.addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }

        //1.3 按照商品品牌过滤
        if (!"".equals(searchMap.get("brand"))) {
            //如果用户选择了商品品牌
            FilterQuery filterQuery = new SimpleFilterQuery();
            Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
            filterQuery.addCriteria(filterCriteria);
            query.addFilterQuery(filterQuery);
        }

        //1.4 按照商品规格过滤
        if (searchMap.get("spec") != null) {
            //如果用户选择了商品规格
            Map<String, String> specMap = (Map<String, String>) searchMap.get("spec");
            for (String key : specMap.keySet()) {
                FilterQuery filterQuery = new SimpleFilterQuery();
                //动态域
                Criteria filterCriteria = new Criteria("item_spec_" + key).is(searchMap.get(key));
                filterQuery.addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
        }

        //1.5 按照价格过滤
        if (!"".equals(searchMap.get("price"))) {
            String[] price = ((String) searchMap.get("price")).split("-");
            if (!price[0].equals("0")) {
                //如果最低价格不等于0
                FilterQuery filterQuery = new SimpleFilterQuery();
                Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(price[0]);
                filterQuery.addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
            if (!price[1].equals("*")) {
                //如果最高价格不等于*
                FilterQuery filterQuery = new SimpleFilterQuery();
                Criteria filterCriteria = new Criteria("item_price").lessThanEqual(price[1]);
                filterQuery.addCriteria(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
        }

        //1.6 分页
        Integer pageNo = (Integer) searchMap.get("pageNo");
        if (pageNo == null) {
            pageNo = 1;
        }
        Integer pageSize = (Integer) searchMap.get("pageSize");
        if (pageSize == null) {
            pageSize = 20;
        }
        query.setOffset((pageNo - 1) * pageSize);
        query.setRows(pageSize);

        //1.7按照价格排序
        String sortValue = (String) searchMap.get("sort");//排序方式
        String sortField = (String) searchMap.get("sortField");//排序字段

        if (sortValue!=null&&!sortValue.equals("")){//如果存在排序筛选
            System.out.println(sortValue);
            if(sortValue.equals("ASC")){//如果是升序筛选
                Sort sort = new Sort(Sort.Direction.ASC, "item_" + sortField);
                query.addSort(sort);
            }
            if (sortValue.equals("DESC")){//如果是降序排列
                Sort sort = new Sort(Sort.Direction.DESC, "item_" + sortField);
                System.out.println(sortValue);
                query.addSort(sort);
            }
        }

        //高亮页对象
        HighlightPage<TbItem> pages = solrTemplate.queryForHighlightPage(query, TbItem.class);

        //高亮入口集合
        List<HighlightEntry<TbItem>> entryList = pages.getHighlighted();

        for (HighlightEntry<TbItem> h : entryList) {
            TbItem item = h.getEntity();// 获取原实体类
            if (h.getHighlights().size() > 0 && h.getHighlights().get(0).getSnipplets().size() > 0) {
                item.setTitle(h.getHighlights().get(0).getSnipplets().get(0));// 设置高亮的结果
            }
        }
        map.put("rows", pages.getContent());
        map.put("totalPages", pages.getTotalPages());//总页数
        map.put("total", pages.getTotalElements());//总记录数

        return map;
    }

    /**
     * 查询分类列表
     *
     * @param searchMap
     * @return
     */
    private List<String> searchCategoryList(Map searchMap) {
        List<String> list = new ArrayList<String>();

        SimpleQuery query = new SimpleQuery("*:*");
        //关键字查询
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);
        //设置分组选项
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");//指定域搜索
        query.setGroupOptions(groupOptions);
        //获得分组页
        GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
        //获取分组结果对象
        GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
        //获取分组页入口
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
        //获取分组入口集合
        List<GroupEntry<TbItem>> entryList = groupEntries.getContent();
        for (GroupEntry<TbItem> entry : entryList) {
            list.add(entry.getGroupValue());
        }
        return list;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据商品分类名称从缓存中查询品牌和规格列表
     *
     * @param category
     * @return
     */
    private Map searchBrandAndSpecList(String category) {
        Map<Object, Object> map = new HashMap<>();
        //1.根据商品分类名称得到模版ID
        Long templateId = (long) redisTemplate.boundHashOps("itemCat").get(category);
        if (templateId != null) {
            //2. 根据模版ID获取品牌列表
            List brandList = (List) redisTemplate.boundHashOps("brandList").get(templateId);
            map.put("brandList", brandList);
            //3. 根据模版ID获取规格列表
            List specList = (List) redisTemplate.boundHashOps("specList").get(templateId);
            map.put("specList", specList);
        }
        return map;
    }

}
