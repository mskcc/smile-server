package org.mskcc.cmo.metadb.persistence;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 *
 * @author ochoaa
 */
@SpringBootApplication
@EntityScan(basePackages = "org.mskcc.cmo.metadb.model")
public class TestApplication {

}
