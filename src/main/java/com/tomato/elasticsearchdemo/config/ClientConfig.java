package com.tomato.elasticsearchdemo.config;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;


import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * ES客户端配置
 * 1. 直连 通过hosts
 * 2. 账号密码  通过username和passwd
 * 3. ApiKey 通过apikey
 * 第二种和第三种方式，需要在ES中配置相应的账号密码或者apikey，并且需要用到证书，证书可以从ES安装目录下的/conf/certs下获取，放在resources目录下即可
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "elasticsearch") //spring配置的前缀
public class ClientConfig {

    @Setter
    private String hosts;

    @Setter
    private String username;

    @Setter
    private String password;

    @Setter
    private String apikey;

    /**
     * 解析配置的字符串，转为HttpHost对象数组
     * @return
     */
    private HttpHost[] toHttpHost() {
        if (!StringUtils.hasLength(hosts)) {
            throw new RuntimeException("invalid elasticsearch configuration");
        }

        String[] hostArray = hosts.split(",");
        HttpHost[] httpHosts = new HttpHost[hostArray.length];
        HttpHost httpHost;
        for (int i = 0; i < hostArray.length; i++) {
            String[] strings = hostArray[i].split(":");
            // 注意这里，如果开启了ES的安全功能，比如使用用户名密码或API模式连接的时候，需要将http改为https
            // 如果是关闭了安全模式的直连模式，就用http
//            httpHost = new HttpHost(strings[0], Integer.parseInt(strings[1]), "http");
            httpHost = new HttpHost(strings[0], Integer.parseInt(strings[1]), "https");

            httpHosts[i] = httpHost;
        }

        return httpHosts;
    }

    /**
     * 创建同步客户端（直连）
     * @return
     */
    @Bean
    public ElasticsearchClient clientByDirect() {
        HttpHost[] httpHosts = toHttpHost();
        RestClient restClient = RestClient.builder(httpHosts).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    /**
     * 创建异步客户端（直连）
     * @return
     */
    @Bean
    public ElasticsearchAsyncClient elasticsearchAsyncClient() {
        HttpHost[] httpHosts = toHttpHost();
        RestClient restClient = RestClient.builder(httpHosts).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchAsyncClient(transport);
    }

    @Bean
    public ElasticsearchClient clientByPasswd() throws Exception {
        ElasticsearchTransport transport = getElasticsearchTransport(username, password, toHttpHost());
        return new ElasticsearchClient(transport);
    }

    private static SSLContext buildSSLContext() {
        ClassPathResource resource = new ClassPathResource("http_ca.crt");
        SSLContext sslContext = null;
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate trustedCa;
            try (InputStream is = resource.getInputStream()) {
                trustedCa = factory.generateCertificate(is);
            }
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", trustedCa);
            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null);
            sslContext = sslContextBuilder.build();
        } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException |
                 KeyManagementException e) {
            log.error("ES连接认证失败", e);
        }

        return sslContext;
    }

    private static ElasticsearchTransport getElasticsearchTransport(String username, String passwd, HttpHost...hosts) {
        // 账号密码的配置
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, passwd));

        // 自签证书的设置，并且还包含了账号密码
        HttpClientConfigCallback callback = httpAsyncClientBuilder -> httpAsyncClientBuilder
                .setSSLContext(buildSSLContext())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setDefaultCredentialsProvider(credentialsProvider);

        // 用builder创建RestClient对象
        RestClient client = RestClient
                .builder(hosts)
                .setHttpClientConfigCallback(callback)
                .build();

        return new RestClientTransport(client, new JacksonJsonpMapper());
    }

    private static ElasticsearchTransport getElasticsearchTransport(String apiKey, HttpHost...hosts) {
        // 将ApiKey放入header中
        Header[] headers = new Header[] {new BasicHeader("Authorization", "ApiKey " + apiKey)};

        // es自签证书的设置
        HttpClientConfigCallback callback = httpAsyncClientBuilder -> httpAsyncClientBuilder
                .setSSLContext(buildSSLContext())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);

        // 用builder创建RestClient对象
        RestClient client = RestClient
                .builder(hosts)
                .setHttpClientConfigCallback(callback)
                .setDefaultHeaders(headers)
                .build();

        return new RestClientTransport(client, new JacksonJsonpMapper());
    }

    @Bean
    public ElasticsearchClient clientByApiKey() throws Exception {
        ElasticsearchTransport transport = getElasticsearchTransport(apikey, toHttpHost());
        return new ElasticsearchClient(transport);
    }
}
