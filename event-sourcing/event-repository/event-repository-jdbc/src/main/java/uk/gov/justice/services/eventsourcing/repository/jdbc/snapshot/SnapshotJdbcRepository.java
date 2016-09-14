package uk.gov.justice.services.eventsourcing.repository.jdbc.snapshot;


import static java.lang.String.format;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.domain.snapshot.AggregateSnapshot;
import uk.gov.justice.services.eventsourcing.repository.core.SnapshotRepository;
import uk.gov.justice.services.jdbc.persistence.AbstractJdbcRepository;
import uk.gov.justice.services.jdbc.persistence.JdbcRepositoryException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import javax.naming.NamingException;

/**
 * JDBC based repository for snapshot records.
 */
public class SnapshotJdbcRepository extends AbstractJdbcRepository
        implements SnapshotRepository {

    /**
     * Column Names
     */
    static final String COL_STREAM_ID = "stream_id";
    /**
     * The Col version id.
     */
    static final String COL_VERSION_ID = "version_id";
    /**
     * The Col type.
     */
    static final String COL_TYPE = "type";
    /**
     * The Col aggregate.
     */
    static final String COL_AGGREGATE = "aggregate";

    /**
     * Statements
     */
    static final String SQL_FIND_LATEST_BY_STREAM_ID = "SELECT * FROM snapshot WHERE stream_id=? ORDER BY version_id DESC";
    /**
     * The Sql find earlier snapshot by stream id and version id.
     */
    static final String SQL_FIND_EARLIER_SNAPSHOT_BY_STREAM_ID_AND_VERSION_ID = "select * from snapshot where version_id = (SELECT version_id FROM snapshot WHERE stream_id=? and version_id < ? ORDER BY version_id DESC LIMIT 1)";

    /**
     * The Sql insert event log.
     */
    static final String SQL_INSERT_EVENT_LOG = "INSERT INTO snapshot (stream_id, version_id, type, aggregate ) VALUES(?, ?, ?, ?)";

    /**
     * The Delete all snapshots for stream id and class.
     */
    static final String DELETE_ALL_SNAPSHOTS_FOR_STREAM_ID_AND_CLASS = "delete from snapshot where stream_id =? and  type=?";

    /**
     * The constant READING_STREAM_EXCEPTION.
     */

    protected static final String READING_STREAM_EXCEPTION = "Exception while reading stream %s";
    private static final String JNDI_DS_EVENT_STORE_PATTERN = "java:/app/%s/DS.eventstore";


    /**
     * Insert the given aggregateSnapshot into Snapshot log.
     *
     * @param aggregateSnapshot the event to insert
     */
    @Override
    public void storeSnapshot(final AggregateSnapshot aggregateSnapshot) {

        try (Connection connection = getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_INSERT_EVENT_LOG)) {
            ps.setObject(1, aggregateSnapshot.getStreamId());
            ps.setLong(2, aggregateSnapshot.getVersionId());
            ps.setObject(3, aggregateSnapshot.getType());
            ps.setBytes(4, aggregateSnapshot.getAggregateByteRepresentation());
            ps.executeUpdate();
        } catch (SQLException | NamingException e) {
            throw new JdbcRepositoryException(format("Exception while storing sequence %s of stream %s",
                    aggregateSnapshot.getVersionId(), aggregateSnapshot.getStreamId()), e);
        }
    }

    /**
     * Returns a {@link AggregateSnapshot} for the given stream streamId.
     *
     * @param streamId streamId of the stream.
     * @return a {@link AggregateSnapshot}. Never returns null.
     */
    @Override
    public Optional<AggregateSnapshot> getLatestSnapshot(final UUID streamId) {

        try (final Connection connection = getDataSource().getConnection();
             final PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND_LATEST_BY_STREAM_ID)) {

            preparedStatement.setObject(1, streamId);

            return extractResults(preparedStatement);

        } catch (SQLException | NamingException e) {
            throw new JdbcRepositoryException(format(READING_STREAM_EXCEPTION, streamId), e);
        }
    }

    @Override
    public <T extends Aggregate> void removeAllSnapshots(final UUID streamId, final Class<T> clazz) {
        try (Connection connection = getDataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(DELETE_ALL_SNAPSHOTS_FOR_STREAM_ID_AND_CLASS)) {
            ps.setObject(1, streamId);
            ps.setObject(2, clazz);
            ps.executeUpdate();
        } catch (SQLException | NamingException e) {
            throw new JdbcRepositoryException(format("Exception while removing snapshots %s of stream %s",
                    clazz, streamId), e);
        }
    }

    /**
     * Extract results optional.
     *
     * @param preparedStatement the prepared statement
     * @return the optional
     * @throws SQLException the sql exception
     */
    protected Optional<AggregateSnapshot> extractResults(final PreparedStatement preparedStatement) throws SQLException {

        try (final ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                return createAggregateSnapshot(resultSet);
            }
        }
        return Optional.empty();
    }

    private Optional<AggregateSnapshot> createAggregateSnapshot(final ResultSet resultSet) throws
            SQLException {

        return Optional.of(new AggregateSnapshot(
                (UUID) resultSet.getObject(COL_STREAM_ID),
                resultSet.getLong(COL_VERSION_ID),
                (Class<? extends Aggregate>) resultSet.getObject(COL_TYPE),
                resultSet.getBytes(COL_AGGREGATE)));
    }

    @Override
    protected String jndiName() throws NamingException {
        return format(JNDI_DS_EVENT_STORE_PATTERN, warFileName());
    }
}
