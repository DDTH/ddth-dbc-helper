package com.github.ddth.dao.qnd;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.github.ddth.dao.BaseBo;
import com.github.ddth.dao.jdbc.AbstractGenericBoJdbcDao;
import com.github.ddth.dao.jdbc.annotations.AnnotatedGenericRowMapper;
import com.github.ddth.dao.jdbc.annotations.ChecksumColumn;
import com.github.ddth.dao.jdbc.annotations.ColumnAttribute;
import com.github.ddth.dao.jdbc.annotations.PrimaryKeyColumns;
import com.github.ddth.dao.jdbc.annotations.UpdateColumns;
import com.github.ddth.dao.jdbc.impl.UniversalRowMapper;

public class QndUniversalRowMapper {

    public static class MyBo extends BaseBo {
    }

    @PrimaryKeyColumns(columns = { "pk1", "pk2" })
    @UpdateColumns(columns = { "col1", "col2", "col3" })
    @ChecksumColumn("checksum")
    @ColumnAttribute(column = "pk1", attr = "pk1", attrClass = int.class)
    @ColumnAttribute(column = "pk2", attr = "pk2", attrClass = String.class)
    @ColumnAttribute(column = "col1", attr = "col1", attrClass = int.class)
    @ColumnAttribute(column = "col2", attr = "col2", attrClass = float.class)
    @ColumnAttribute(column = "col3", attr = "col3", attrClass = boolean.class)
    @ColumnAttribute(column = "checksum", attr = "checksum", attrClass = long.class)
    public static class MyRowMapper extends AnnotatedGenericRowMapper<MyBo> {
    }

    public static class MyJdbcDao1 extends AbstractGenericBoJdbcDao<Map<String, Object>> {
    }

    public static class MyJdbcDao2 extends AbstractGenericBoJdbcDao<MyBo> {
    }

    public static void main(String[] args) {
        try (MyJdbcDao2 dao = new MyJdbcDao2()) {
            System.out.println(dao);
        }

        try (MyJdbcDao1 dao = new MyJdbcDao1()) {
            dao.setRowMapper(UniversalRowMapper.INSTANCE);
            dao.init();

            System.out.println(new ToStringBuilder(null).append(UniversalRowMapper.INSTANCE.getPrimaryKeyColumns()));
            System.out.println(new ToStringBuilder(null).append(UniversalRowMapper.INSTANCE.getInsertColumns()));
            System.out.println(new ToStringBuilder(null).append(UniversalRowMapper.INSTANCE.getUpdateColumns()));
            System.out.println(new ToStringBuilder(null).append(UniversalRowMapper.INSTANCE.getAllColumns()));
            System.out.println(UniversalRowMapper.INSTANCE.getChecksumColumn());
        }
    }

}
