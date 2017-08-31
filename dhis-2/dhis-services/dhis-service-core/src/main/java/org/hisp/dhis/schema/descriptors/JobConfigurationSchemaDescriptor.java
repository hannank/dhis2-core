package org.hisp.dhis.schema.descriptors;

import com.google.common.collect.Lists;
import org.hisp.dhis.scheduling.Configuration.JobConfiguration;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaDescriptor;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;

/**
 * Created by henninghakonsen on 31/08/2017.
 * Project: dhis-2.
 */
public class JobConfigurationSchemaDescriptor implements SchemaDescriptor
{
    public static final String SINGULAR = "jobConfiguration";

    public static final String PLURAL = "jobConfigurations";

    public static final String API_ENDPOINT = "/" + PLURAL;

    @Override
    public Schema getSchema()
    {
        Schema schema = new Schema( JobConfiguration.class, SINGULAR, PLURAL );
        schema.setRelativeApiEndpoint( API_ENDPOINT );
        schema.setOrder( 1040 );

        schema.getAuthorities().add( new Authority( AuthorityType.CREATE, Lists.newArrayList( "F_JOBCONFIGURATION_ADD" ) ) );
        schema.getAuthorities().add( new Authority( AuthorityType.DELETE, Lists.newArrayList( "F_JOBCONFIGURATION_DELETE" ) ) );

        System.out.println("Schema: " + schema);
        return schema;
    }
}
