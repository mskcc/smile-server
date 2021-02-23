package org.mskcc.cmo.metadb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Controller;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Controller
@SpringBootApplication(scanBasePackages = {"org.mskcc.cmo.messaging",
        "org.mskcc.cmo.metadb.service", "org.mskcc.cmo.metadb"})
@EnableCaching
@EnableSwagger2 // enable swagger2 documentation
public class MetaDbRequestApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(MetaDbRequestApplication.class, args);
    }
}
