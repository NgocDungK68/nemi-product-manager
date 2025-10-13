package com.nemi.repository.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
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

        try (Connection conn = hikariDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(getSql())) {

            conn.setAutoCommit(false);
            int count = 0;

            for (T entity : entities) {
                setValues(ps, entity);
                ps.addBatch();

                if (count % batchSize == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                }
                count++;
            }

            if (count % batchSize != 0) {
                ps.executeBatch();
            }

            conn.commit();
            log.info("Batch insert done for {}", getClass().getSimpleName());
        } catch (SQLException e) {
            log.error("Error during batch insert in {}: {}", getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }


        public static <T> List<List<T>> createSubList(List<T> list, int subListSize){
            List<List<T>> listOfSubList = new ArrayList<>();
            for (int i = 0; i < list.size(); i+=subListSize) {
                if(i + subListSize <= list.size()){
                    listOfSubList.add(list.subList(i, i + subListSize));
                }else{
                    listOfSubList.add(list.subList(i, list.size()));
                }
            }
            return listOfSubList;
        }


}