package com.tomato.elasticsearchdemo.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.util.ObjectBuilder;
import com.tomato.elasticsearchdemo.services.IndexService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.function.Function;

/**
 * @author w-tomato
 * @description 通过用户密码连接ES的方式操作es
 * @date 2024/1/1
 */
@Service("indexServiceByUP")
public class IndexServiceByUPImpl implements IndexService {

    @Resource(name="clientByPasswd")
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

    }


}
