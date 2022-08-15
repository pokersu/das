package org.muguang.mybatisenhance.config;

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class IgniteConfig {

    @Bean
    public IgniteConfiguration igniteConfiguration() {
        IgniteConfiguration configuration = new IgniteConfiguration();
        configuration.setClientMode(true);
        List<String> address = List.of("127.0.0.1:47500..47502");
        configuration.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(new TcpDiscoveryVmIpFinder(true).setAddresses(address)));
        return configuration;
    }
}
