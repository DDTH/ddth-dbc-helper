package com.github.ddth.dao.qnd.nosql.cassandra;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;

import com.github.ddth.cacheadapter.ICacheFactory;
import com.github.ddth.cacheadapter.cacheimpl.guava.GuavaCacheFactory;
import com.github.ddth.cql.SessionManager;
import com.github.ddth.dao.nosql.BaseKvDao;
import com.github.ddth.dao.nosql.cassandra.CassandraKvStorage;

public class QndCassandraKvDao extends BaseNosqlCassandraQnd {
    public static void main(String[] args) throws Exception {
        Random random = new Random(System.currentTimeMillis());

        try (SessionManager sm = sessionManager()) {
            String KEYSPACE = "test";
            sm.execute("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE
                    + " WITH REPLICATION={'class' : 'SimpleStrategy', 'replication_factor' : 1}");

            String TABLE = "tbl_kv", COL_KEY = "key", COL_VALUE = "value";

            sm.execute("DROP TABLE IF EXISTS " + KEYSPACE + "." + TABLE);
            sm.execute("CREATE TABLE " + KEYSPACE + "." + TABLE + "(" + COL_KEY + " text,"
                    + COL_VALUE + " blob,PRIMARY KEY(" + COL_KEY + "))");
            Thread.sleep(1000);

            long t1 = System.currentTimeMillis();
            try (CassandraKvStorage _kvStore = new CassandraKvStorage()) {
                _kvStore.setColumnKey(COL_KEY).setColumnValue(COL_VALUE).setSessionManager(sm)
                        .setDefaultKeyspace(KEYSPACE).setAsyncDelete(true).setAsyncPut(true).init();
                try (BaseKvDao kvDao = new BaseKvDao()) {
                    ICacheFactory cf = new GuavaCacheFactory().init();
                    kvDao.setKvStorage(_kvStore).setCacheName("cache").setCacheFactory(cf).init();

                    System.out.println(kvDao.size(TABLE));

                    for (int i = 0; i < 100; i++) {
                        String value = RandomStringUtils.randomAscii(1 + random.nextInt(1024));
                        kvDao.put(TABLE, String.valueOf(i), value.getBytes(StandardCharsets.UTF_8));
                    }

                    System.out.println(kvDao.size(TABLE));

                    for (int i = 0; i < 100; i++) {
                        byte[] value = kvDao.get(TABLE, String.valueOf(i));
                        System.out.println(i + ": " + new String(value, StandardCharsets.UTF_8));
                        if (random.nextInt() % 3 == 0) {
                            kvDao.delete(TABLE, String.valueOf(i));
                        }
                    }

                    System.out.println(kvDao.size(TABLE));
                }
            }
            long t2 = System.currentTimeMillis();
            long d = t2 - t1;
            System.out.println("Finished in " + d + " ms.");
        }
    }
}
