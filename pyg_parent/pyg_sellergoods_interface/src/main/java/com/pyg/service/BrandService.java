package com.pyg.service;

import com.pyg.pojo.TbBrand;
import entity.PageResult;

import java.util.List;
import java.util.Map;

public interface BrandService {

     public List<TbBrand> findAll();

     public PageResult findPage(int pageNum,int pageSize);

     public void add(TbBrand brand);

     public TbBrand findOne(long id );

     public void update(TbBrand brand);

     public void delete(long[] ids);

     public PageResult findPage(TbBrand brand,int pageNum,int pageSize);

     public List<Map> selectOptionList();
}
