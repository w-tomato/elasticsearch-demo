package com.tomato.elasticsearchdemo.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.util.ObjectBuilder;
import com.tomato.elasticsearchdemo.services.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.function.Function;

/**
 * @author w-tomato
 * @description
 * 在ES的Java库Java API Client中，与REST API对应的的类和接口都在统一的包名co.elastic.clients.elasticsearch之下，
 * 然后再通过下一级package进行分类，例如索引相关的，在REST API中的feature是Index APIs，那么在Java API Client中，
 * 完整的package就是co.elastic.clients.elasticsearch.indices，这里面有索引操作所需的请求、响应、服务等各种类
 * @date 2023/12/31
 */
@Service("indexServiceByDirect")
public class IndexServiceByDirectImpl implements IndexService {

    @Resource(name="clientByDirect")
    private ElasticsearchClient elasticsearchClient;

    @Override
    public void addIndex(String name) throws IOException {
        elasticsearchClient.indices().create(c -> c.index(name));
    }

    @Override
    public boolean indexExists(String name) throws IOException {
        return elasticsearchClient.indices().exists(b -> b.index(name)).value();
    }

    @Override
    public void delIndex(String name) throws IOException {
        elasticsearchClient.indices().delete(c -> c.index(name));
    }

    @Override
    public void create(String name, Function<IndexSettings.Builder, ObjectBuilder<IndexSettings>> settingFn, Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> mappingFn) throws IOException {
        elasticsearchClient
                .indices()
                .create(c -> c
                        .index(name)
                        .settings(settingFn)
                        .mappings(mappingFn)
                );
    }

    public static void main(String[] args) throws IOException {
        String indexName = "test_index";
        IndexServiceByDirectImpl indexService = new IndexServiceByDirectImpl();

        if (indexService.indexExists(indexName)) {
            // 如果索引存在，则删除
            System.out.println("索引已存在，删除索引");
            indexService.delIndex(indexName);
        }
        System.out.println("创建索引");
        indexService.addIndex(indexName);
        System.out.println("索引是否存在：" + indexService.indexExists(indexName));
        System.out.println("删除索引");
        indexService.delIndex(indexName);
        System.out.println("索引是否存在：" + indexService.indexExists(indexName));
    }
}
