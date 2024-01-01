package com.tomato.elasticsearchdemo;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.util.ObjectBuilder;
import com.tomato.elasticsearchdemo.services.IndexService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.function.Function;

/**
 * @author w-tomato
 * @description
 * @date 2023/12/31
 */
@SpringBootTest
public class IndexServiceTest {

//    @Resource(name="indexServiceByDirect")
//    IndexService indexService;
//    @Resource(name="indexServiceByUP")
//    IndexService indexService;
    @Resource(name="indexServiceByApiKey")
    IndexService indexService;

    @Test
    void addIndex() throws Exception {
        String indexName = "test_index";

        if (indexService.indexExists(indexName)) {
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

    @Test
    void indexExists() throws Exception {
        indexService.indexExists("a");
    }

    @Test
    void createIndex() throws Exception {
        // 索引名
        String indexName = "product002";

        // 构建setting时，builder用到的lambda
        Function<IndexSettings.Builder, ObjectBuilder<IndexSettings>> settingFn = sBuilder -> sBuilder
                .index(iBuilder -> iBuilder
                        // 三个分片
                        .numberOfShards("3")
                        // 一个副本
                        .numberOfReplicas("1")
                );

        // 新的索引有三个字段，每个字段都有自己的property，这里依次创建
        Property keywordProperty = Property.of(pBuilder -> pBuilder.keyword(kBuilder -> kBuilder.ignoreAbove(256)));
        Property textProperty = Property.of(pBuilder -> pBuilder.text(tBuilder -> tBuilder));
        Property integerProperty = Property.of(pBuilder -> pBuilder.integer(iBuilder -> iBuilder));

        // // 构建mapping时，builder用到的lambda
        Function<TypeMapping.Builder, ObjectBuilder<TypeMapping>> mappingFn = mBuilder -> mBuilder
                .properties("name", keywordProperty)
                .properties("description", textProperty)
                .properties("price", integerProperty);

        // 创建索引，并且指定了setting和mapping
        indexService.create(indexName, settingFn, mappingFn);
    }
}
