/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.gora.cassandra.serializers;

import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.TableMetadata;
import org.apache.gora.cassandra.store.CassandraClient;
import org.apache.gora.cassandra.store.CassandraMapping;
import org.apache.gora.cassandra.store.CassandraStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Created by madhawa on 6/26/17.
 */
public abstract class CassandraSerializer<K, T> {
  CassandraClient client;

  private Class<K> keyClass;

  private Class<T> persistentClass;

  private CassandraMapping mapping;

  private static final Logger LOG = LoggerFactory.getLogger(CassandraStore.class);

  CassandraSerializer(CassandraClient cc, Class<K> keyClass, Class<T> persistantClass, CassandraMapping mapping) {
    this.keyClass = keyClass;
    this.persistentClass = persistantClass;
    this.client = cc;
    this.mapping = mapping;
  }

  public void createSchema() {
    LOG.debug("creating Cassandra keyspace {}", mapping.getKeySpace().getName());
    this.client.getSession().execute(CassandraQueryFactory.getCreateKeySpaceQuery(mapping));
    LOG.debug("creating Cassandra column family / table {}", mapping.getCoreName());
    this.client.getSession().execute(CassandraQueryFactory.getCreateTableQuery(mapping));
  }

  public void deleteSchema() {
    LOG.debug("dropping Cassandra table {}", mapping.getCoreName());
    this.client.getSession().execute(CassandraQueryFactory.getDropTableQuery(mapping));
    LOG.debug("dropping Cassandra keyspace {}", mapping.getKeySpace().getName());
    this.client.getSession().execute(CassandraQueryFactory.getDropKeySpaceQuery(mapping));
  }

  public void close() {
    this.client.close();
  }

  public void truncateSchema() {
    LOG.debug("truncating Cassandra table {}", mapping.getCoreName());
    this.client.getSession().execute(CassandraQueryFactory.getTruncateTableQuery(mapping));
  }

  public boolean schemaExists() {
    KeyspaceMetadata keyspace = this.client.getCluster().getMetadata().getKeyspace(mapping.getKeySpace().getName());
    if (keyspace != null) {
      TableMetadata table = keyspace.getTable(mapping.getCoreName());
      return table != null;
    } else {
      return false;
    }
  }

  public static <K, T> CassandraSerializer getSerializer(CassandraClient cc, String type, final Class<K> keyClass, final Class<T> persistentClass, CassandraMapping mapping) {
    CassandraStore.SerializerType serType = type.isEmpty() ? CassandraStore.SerializerType.NATIVE : CassandraStore.SerializerType.valueOf(type.toUpperCase(Locale.ENGLISH));
    CassandraSerializer ser;
    switch (serType) {
      case AVRO:
        ser = new AvroSerializer(cc,keyClass, persistentClass, mapping);
        break;
      case NATIVE:
      default:
        ser = new NativeSerializer(cc, keyClass, persistentClass, mapping);

    }
    return ser;
  }

  public abstract void put(K key, T value);

  public abstract T get(K key);

  public abstract boolean delete(K key);

}