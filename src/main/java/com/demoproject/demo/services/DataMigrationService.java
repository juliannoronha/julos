package com.demoproject.demo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for handling data migration operations.
 * This class provides methods to migrate data between different database schemas or structures.
 */
@Service
public class DataMigrationService {

    // JdbcTemplate for executing SQL queries and updates
    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(DataMigrationService.class);

    /**
     * Constructs a new DataMigrationService with the provided JdbcTemplate.
     *
     * @param jdbcTemplate The JdbcTemplate to be used for database operations
     */
    public DataMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeMigration(String sql) {
        logger.debug("Executing migration SQL: {}", sql);
        try {
            jdbcTemplate.execute(sql);
            logger.info("Migration executed successfully");
        } catch (Exception e) {
            logger.error("Migration failed: {}", sql, e);
            throw new RuntimeException("Migration failed", e);
        }
    }

    // Add try-with-resources for batch operations
    @Transactional
    public void executeBatchMigration(List<String> sqlStatements) {
        try {
            jdbcTemplate.batchUpdate(sqlStatements.toArray(new String[0]));
            logger.info("Batch migration completed successfully");
        } catch (Exception e) {
            logger.error("Batch migration failed", e);
            throw new RuntimeException("Batch migration failed", e);
        }
    }

    // TODO: Add error handling and logging for migration processes
    // TODO: Implement rollback mechanisms for failed migrations
    // TODO: Consider adding progress tracking for long-running migrations
}