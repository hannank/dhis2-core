package org.hisp.dhis.scheduling;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

/**
 * Custom deserializer for {@link JobConfiguration} objects. This is due to different objects
 * being stored in the parameter variable.
 *
 * @author Henning Håkonsen
 */
public class JobConfigurationDeserializer
    extends JsonDeserializer<JobConfiguration>
{
    @Override
    public JobConfiguration deserialize( JsonParser jsonParser,
        DeserializationContext deserializationContext )
        throws IOException
    {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree(jsonParser);

        String jobTypeString = root.get( "jobType" ).toString();
        JobType jobType = JobType.valueOf( jobTypeString.substring( 1, jobTypeString.length() - 1 ) );
        assertNotNull(jobType, "jobType must not be null.");

        String cronExpression = mapper.convertValue( root.get( "cronExpression" ), String.class );
        assertNotNull(cronExpression, "cronExpression must not be null.");

        JobParameters jobParameters = mapper.convertValue( root.get( "jobParameters" ), jobType.getClazz() );
        assertNotNull(jobParameters, "jobParameters must not be null.");

        boolean enabled = root.get( "enabled" ) == null || root.get( "enabled" ).booleanValue();

        return new JobConfiguration( root.get( "name" ).textValue(), jobType, cronExpression, jobParameters, enabled );
    }
}