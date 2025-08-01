package org.mskcc.smile.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.nats.client.Message;
import java.nio.charset.StandardCharsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author ochoaa
 */
public class NatsMsgUtil {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Log LOG = LogFactory.getLog(NatsMsgUtil.class);

    /**
     * Extracts string from NATS message data contents.
     * @param msg
     * @return String
     * @throws JsonProcessingException
     */
    public static String extractNatsJsonString(Message msg)
            throws JsonProcessingException {
        byte[] msgData = msg.getData();
        try {
            String jsonString = mapper.readValue(
                new String(msgData, StandardCharsets.UTF_8),
                String.class);
            return jsonString;
        } catch (MismatchedInputException e) {
            LOG.debug("Failed to deserialize with mapper.readValue() - "
                    + "attempting mapper.convertValue()");
        }

        try {
            String jsonString = mapper.convertValue(
                    new String(msgData, StandardCharsets.UTF_8),
                    String.class);
            return jsonString;
        } catch (Exception e) {
            LOG.debug("Failed to deserialize with mapper.convertValue(). "
                    + "Cannot deserialize message");
            e.printStackTrace();
        }

        LOG.error("Could not deserialize message from contents: " + msg.toString());
        LOG.error(msg);
        return null;
    }

    /**
     * Converts string to given TypeReference.
     * @param input
     * @param valueTypeReference
     * @return Object
     * @throws JsonProcessingException
     */
    public static Object convertObjectFromString(String input, TypeReference valueTypeReference)
            throws JsonProcessingException {
        try {
            return mapper.convertValue(input, valueTypeReference);
        } catch (IllegalArgumentException e) {
            return mapper.readValue(input, valueTypeReference);
        }
    }
}
