package com.pyg.service.impl;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.pyg.mapper.TbSpecificationOptionMapper;
import com.pyg.pojo.TbSpecificationOption;
import com.pyg.pojo.TbSpecificationOptionExample;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pyg.mapper.TbTypeTemplateMapper;
import com.pyg.pojo.TbTypeTemplate;
import com.pyg.pojo.TbTypeTemplateExample;
import com.pyg.pojo.TbTypeTemplateExample.Criteria;
import com.pyg.service.TypeTemplateService;

import entity.PageResult;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service(timeout = 5000)
@Transactional
public class TypeTemplateServiceImpl implements TypeTemplateService {

	@Autowired
	private TbTypeTemplateMapper typeTemplateMapper;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbTypeTemplate> findAll() {
		return typeTemplateMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbTypeTemplate> page=   (Page<TbTypeTemplate>) typeTemplateMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbTypeTemplate typeTemplate) {
		typeTemplateMapper.insert(typeTemplate);		
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbTypeTemplate typeTemplate){
		typeTemplateMapper.updateByPrimaryKey(typeTemplate);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbTypeTemplate findOne(Long id){
		return typeTemplateMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			typeTemplateMapper.deleteByPrimaryKey(id);
		}		
	}
	
	
	@Override
	public PageResult findPage(TbTypeTemplate typeTemplate, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbTypeTemplateExample example=new TbTypeTemplateExample();
		Criteria criteria = example.createCriteria();
		
		if(typeTemplate!=null){			
						if(typeTemplate.getName()!=null && typeTemplate.getName().length()>0){
				criteria.andNameLike("%"+typeTemplate.getName()+"%");
			}
			if(typeTemplate.getSpecIds()!=null && typeTemplate.getSpecIds().length()>0){
				criteria.andSpecIdsLike("%"+typeTemplate.getSpecIds()+"%");
			}
			if(typeTemplate.getBrandIds()!=null && typeTemplate.getBrandIds().length()>0){
				criteria.andBrandIdsLike("%"+typeTemplate.getBrandIds()+"%");
			}
			if(typeTemplate.getCustomAttributeItems()!=null && typeTemplate.getCustomAttributeItems().length()>0){
				criteria.andCustomAttributeItemsLike("%"+typeTemplate.getCustomAttributeItems()+"%");
			}
	
		}
		
		Page<TbTypeTemplate> page= (Page<TbTypeTemplate>)typeTemplateMapper.selectByExample(example);		
		//查询分页时调用缓存方法
		saveToRedis();
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Autowired
	private RedisTemplate redisTemplate;

	private void saveToRedis(){
		List<TbTypeTemplate> templateList = findAll();
		for (TbTypeTemplate template : templateList) {
			//得到品牌列表
			List<Map> brandList = JSON.parseArray(template.getBrandIds(), Map.class);
			redisTemplate.boundHashOps("brandList").put(template.getId(),brandList);
			//得到规格列表
			List<Map> specList = findSpecList(template.getId());
			redisTemplate.boundHashOps("specList").put(template.getId(),specList);
		}
		System.out.println("TypeTemplateServiceImpl中saveToRedis保存了品牌和规格缓存");
	}

	@Override
	public List<Map> selectOptionList() {
		return typeTemplateMapper.selectOptionList();
	}

	@Autowired
	private TbSpecificationOptionMapper specificationOptionMapper;

	@Override
	public List<Map> findSpecList(long id) {
		//查询模版
		TbTypeTemplate typeTemplate = typeTemplateMapper.selectByPrimaryKey(id);

		List<Map> list = JSON.parseArray(typeTemplate.getSpecIds(), Map.class) ;
		for(Map map:list){
			//查询规格选项列表
			TbSpecificationOptionExample example=new TbSpecificationOptionExample();
			com.pyg.pojo.TbSpecificationOptionExample.Criteria criteria = example.createCriteria();
			criteria.andSpecIdEqualTo( Long.valueOf( (Integer)map.get("id") ) );
			List<TbSpecificationOption> options =
					specificationOptionMapper.selectByExample(example);
			map.put("options", options);
		}
		return list;
	}

}