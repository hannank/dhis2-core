package org.hisp.dhis.analytics.table;

import org.hisp.dhis.analytics.AnalyticsTable;
import org.hisp.dhis.analytics.AnalyticsTableColumn;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * @author Henning Håkonsen
 */
public class JdbcValidationResultTableManager
    extends AbstractJdbcTableManager
{
    @Autowired
    PeriodService periodService;

    @Override
    protected void populateTable( AnalyticsTable table )
    {
        final String start = DateUtils.getMediumDateString( table.getPeriod().getStartDate() );
        final String end = DateUtils.getMediumDateString( table.getPeriod().getEndDate() );
        final String tableName = table.getTempTableName();

        String insert = "insert into " + table.getTempTableName() + " (";

        List<AnalyticsTableColumn> columns = getDimensionColumns( table );

        validateDimensionColumns( columns );

        for ( AnalyticsTableColumn col : columns )
        {
            insert += col.getName() + ",";
        }

        insert += "\"leftsidevalue\",\"rightsidevalue\") ";

        String select = "select ";

        for ( AnalyticsTableColumn col : columns )
        {
            select += col.getAlias() + ",";
        }

        select = select.replace( "organisationunitid", "sourceid" ); // Legacy fix

        select +=
            "cdr.leftsidevalue as leftsidevalue, " +
                "cdr.rightsidevalue as rightsidevalue " +
                "from validationresult cdr " +
                "inner join validationrule vr on vr.validationruleid=cdr.validationruleid " +
                "inner join _organisationunitgroupsetstructure ougs on cdr.organisationunitid=ougs.organisationunitid " +
                "left join _orgunitstructure ous on cdr.organisationunitid=ous.organisationunitid " +
                "inner join _categorystructure acs on cdr.attributeoptioncomboid=acs.categoryoptioncomboid " +
                "inner join period pe on cdr.periodid=pe.periodid " +
                "inner join _periodstructure ps on cdr.periodid=ps.periodid " +
                "where pe.startdate >= '" + start + "' " +
                "and pe.startdate <= '" + end + "' " +
                "and cdr.created is not null";

        final String sql = insert + select;

        populateAndLog( sql, tableName );
    }

    @Override
    public AnalyticsTableType getAnalyticsTableType()
    {
        return AnalyticsTableType.VALIDATION_RESULT;
    }

    @Override
    public List<AnalyticsTable> getTables( Date earliest )
    {
        log.info( "Get tables using earliest: " + earliest );

        return getTables( getDataYears( earliest ) );
    }

    @Override
    public Set<String> getExistingDatabaseTables()
    {
        return null;
    }

    @Override
    public String validState()
    {
        boolean hasData = jdbcTemplate.queryForRowSet( "SELECT validationresultid FROM validationresult LIMIT 1" )
            .next();

        if ( !hasData )
        {
            return "No validation violation rules exist, not updating validation violation analytics tables";
        }

        return null;
    }

    @Override
    public void createTable( AnalyticsTable table )
    {
        final String tableName = table.getTempTableName();

        final String sqlDrop = "drop table " + tableName;

        executeSilently( sqlDrop );

        String sqlCreate = "create table " + tableName + " (";

        List<AnalyticsTableColumn> columns = getDimensionColumns( table );

        validateDimensionColumns( columns );

        for ( AnalyticsTableColumn col : columns )
        {
            sqlCreate += col.getName() + " " + col.getDataType() + ",";
        }

        sqlCreate += "\"leftsidevalue\" FLOAT, ";

        sqlCreate += "\"rightsidevalue\" FLOAT)";

        log.info( "Creating table: " + tableName + ", columns: " + columns.size() );

        log.debug( "Create SQL: " + sqlCreate );

        System.out.println("sqlCreate: " + sqlCreate);

        jdbcTemplate.execute( sqlCreate );
    }

    private List<Integer> getDataYears( Date earliest )
    {
        String sql =
            "select distinct(extract(year from pe.startdate)) " +
                "from validationresult cdr " +
                "inner join period pe on cdr.periodid=pe.periodid " +
                "where pe.startdate is not null ";

        if ( earliest != null )
        {
            sql += "and pe.startdate >= '" + DateUtils.getMediumDateString( earliest ) + "'";
        }

        return jdbcTemplate.queryForList( sql, Integer.class );
    }

    @Override
    public Future<?> applyAggregationLevels( ConcurrentLinkedQueue<AnalyticsTable> tables,
        Collection<String> dataElements, int aggregationLevel )
    {
        return null;
    }

    @Override
    public Future<?> vacuumTablesAsync( ConcurrentLinkedQueue<AnalyticsTable> tables )
    {
        return null;
    }

    @Override
    protected List<AnalyticsTableColumn> getDimensionColumns( AnalyticsTable table )
    {
        List<AnalyticsTableColumn> columns = new ArrayList<>();

        List<OrganisationUnitGroupSet> orgUnitGroupSets =
            idObjectManager.getDataDimensionsNoAcl( OrganisationUnitGroupSet.class );

        List<DataElementCategory> attributeCategories =
            categoryService.getAttributeDataDimensionCategoriesNoAcl();

        for ( OrganisationUnitGroupSet groupSet : orgUnitGroupSets )
        {
            columns.add( new AnalyticsTableColumn( quote( groupSet.getUid() ), "character(11)",
                "ougs." + quote( groupSet.getUid() ), groupSet.getCreated() ) );
        }

        for ( DataElementCategory category : attributeCategories )
        {
            columns.add( new AnalyticsTableColumn( quote( category.getUid() ), "character(11)",
                "acs." + quote( category.getUid() ), category.getCreated() ) );
        }

        for ( PeriodType periodType : PeriodType.getAvailablePeriodTypes() )
        {
            String column = quote( periodType.getName().toLowerCase() );
            columns.add( new AnalyticsTableColumn( column, "character varying(15)", "ps." + column ) );
        }

        AnalyticsTableColumn vr = new AnalyticsTableColumn( quote( "dx" ), "character(11) not null",
            "vr.uid" );

        columns.add( vr );

        return filterDimensionColumns( columns );
    }
}
