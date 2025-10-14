package com.nemi.repository.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseBatchRepository<T> {

    final HikariDataSource hikariDataSource;


    protected abstract String getSql();

    protected abstract void setValues(PreparedStatement ps, T entity) throws SQLException;


    public void batchInsert(List<T> entities, int batchSize) throws SQLException {
        if (CollectionUtils.isEmpty(entities)) return;

        log.info("Batch inserting {} entities into {}", entities.size(), getClass().getSimpleName());

        Connection conn = hikariDataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(getSql());
        conn.setAutoCommit(false);

        try  {

            int count = 0;

            for (T entity : entities) {
                setValues(ps, entity);
                ps.addBatch();

                count++;

                if (count % batchSize == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                }

            }

            if (count % batchSize != 0) {
                ps.executeBatch();

            }
            ps.clearBatch();
            conn.commit();
            log.info("Batch insert done for {}", getClass().getSimpleName());
        } catch (SQLException e) {
            log.error("Error during batch insert in {}: {}", getClass().getSimpleName(), e.getMessage());
            throw e;
        }finally {
            if (ps != null) {
                try { ps.close(); } catch (SQLException ignored) {}
            }
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public static <T> List<List<T>> createSubList(List<T> list, int subListSize) {
        List<List<T>> listOfSubList = new ArrayList<>();
        for (int i = 0; i < list.size(); i += subListSize) {
            if (i + subListSize <= list.size()) {
                listOfSubList.add(list.subList(i, i + subListSize));
            } else {
                listOfSubList.add(list.subList(i, list.size()));
            }
        }
        return listOfSubList;
    }


}