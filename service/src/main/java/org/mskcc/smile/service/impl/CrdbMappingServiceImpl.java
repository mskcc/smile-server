package org.mskcc.smile.service.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.mskcc.smile.persistence.jpa.CrdbRepository;
import org.mskcc.smile.service.CrdbMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CrdbMappingServiceImpl implements CrdbMappingService {
    @Value("${crdb.query_timeout_seconds:5}")
    private int crdbQueryTimeoutSeconds;

    private ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    private CrdbRepository crdbRepository;

    public CrdbMappingServiceImpl() {}

    public CrdbMappingServiceImpl(CrdbRepository crdbRepository) {
        this.crdbRepository = crdbRepository;
    }

    @Override
    public String getCmoPatientIdbyDmpId(String dmpId) {
        Callable<Object> task = new Callable<Object>() {
            @Override
            public Object call() {
                return crdbRepository.getCmoPatientIdbyDmpId(dmpId);
            }
        };
        Object result = runQueryWithForcedTimeout(task);
        if (result != null) {
            return result.toString();
        }
        return null;
    }

    @Override
    public String getCmoPatientIdByInputId(String inputId) {
        Callable<Object> task = new Callable<Object>() {
            @Override
            public Object call() {
                return crdbRepository.getCmoPatientIdByInputId(inputId);
            }
        };
        Object result = runQueryWithForcedTimeout(task);
        if (result != null) {
            return result.toString();
        }
        return null;
    }

    private Object runQueryWithForcedTimeout(Callable<Object> task) {
        Future<Object> future = executor.submit(task);
        try {
            return future.get(crdbQueryTimeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
    }
}
