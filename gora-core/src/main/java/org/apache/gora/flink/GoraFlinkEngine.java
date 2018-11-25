/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gora.flink;

import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.hadoop.mapreduce.HadoopInputFormat;
import org.apache.flink.api.java.hadoop.mapreduce.HadoopOutputFormat;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Preconditions;
import org.apache.gora.mapreduce.GoraInputFormat;
import org.apache.gora.mapreduce.GoraOutputFormat;
import org.apache.gora.persistency.impl.PersistentBase;
import org.apache.gora.store.DataStore;
import org.apache.gora.store.DataStoreFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Core class which handles Gora - Flink Engine integration.
 */
public class GoraFlinkEngine<KEYIN, VALUEIN extends PersistentBase, KEYOUT, VALUEOUT extends PersistentBase> {

  Class<KEYIN> classKeyIn;
  Class<VALUEIN> classValueIn;
  Class<KEYOUT> classKeyOut;
  Class<VALUEOUT> classValueOut;

  public GoraFlinkEngine(Class<KEYIN> classKeyIn,
                         Class<VALUEIN> classValueIn) {
    this.classKeyIn = classKeyIn;
    this.classValueIn = classValueIn;
  }

  public GoraFlinkEngine(Class<KEYIN> classKeyIn,
                         Class<VALUEIN> classValueIn,
                         Class<KEYOUT> classKeyOut,
                         Class<VALUEOUT> classValueOut) {
    this.classKeyIn = classKeyIn;
    this.classValueIn = classValueIn;
    this.classKeyOut = classKeyOut;
    this.classValueOut = classValueOut;
  }

  public DataSource<Tuple2<KEYIN, VALUEIN>> createDataSource(ExecutionEnvironment env,
                                                             Configuration conf,
                                                             Class<? extends DataStore<KEYIN, VALUEIN>> dataStoreClass)
          throws IOException {
    Preconditions.checkNotNull(classKeyIn);
    Preconditions.checkNotNull(classValueIn);
    Job job = new Job(conf);
    DataStore<KEYIN, VALUEIN> dataStore = DataStoreFactory.getDataStore(dataStoreClass
            , classKeyIn, classValueIn, job.getConfiguration());
    GoraInputFormat.setInput(job, dataStore.newQuery(), true);
    HadoopInputFormat<KEYIN, VALUEIN> wrappedGoraInput =
            new HadoopInputFormat<>(new GoraInputFormat<>(),
                    classKeyIn, classValueIn, job);
    return env.createInput(wrappedGoraInput);
  }

  public DataSource<Tuple2<KEYIN, VALUEIN>> createDataSource(ExecutionEnvironment env,
                                                             Configuration conf,
                                                             DataStore<KEYIN, VALUEIN> dataStore)
          throws IOException {
    Preconditions.checkNotNull(classKeyIn);
    Preconditions.checkNotNull(classValueIn);
    Job job = new Job(conf);
    GoraInputFormat.setInput(job, dataStore.newQuery(), true);
    HadoopInputFormat<KEYIN, VALUEIN> wrappedGoraInput =
            new HadoopInputFormat<>(new GoraInputFormat<>(),
                    classKeyIn, classValueIn, job);
    return env.createInput(wrappedGoraInput);
  }

  public OutputFormat<Tuple2<KEYOUT, VALUEOUT>> createDataSink(Configuration conf,
                                                               DataStore<KEYOUT, VALUEOUT> dataStore)
          throws IOException {
    Preconditions.checkNotNull(classKeyOut);
    Preconditions.checkNotNull(classValueOut);
    Job job = new Job(conf);
    GoraOutputFormat.setOutput(job, dataStore, true);
    HadoopOutputFormat<KEYOUT, VALUEOUT> wrappedGoraOutput =
            new HadoopOutputFormat<>(
                    new GoraOutputFormat<>(), job);
    // Temp fix to prevent NullPointerException from Flink side.
    Path tempPath = Files.createTempDirectory("temp");
    job.getConfiguration().set("mapred.output.dir", tempPath.toAbsolutePath().toString());
    return wrappedGoraOutput;

  }

  public OutputFormat<Tuple2<KEYOUT, VALUEOUT>> createDataSink(Configuration conf,
                                                               Class<? extends DataStore<KEYOUT, VALUEOUT>> dataStoreClass)
          throws IOException {
    Preconditions.checkNotNull(classKeyOut);
    Preconditions.checkNotNull(classValueOut);
    Job job = new Job(conf);
    DataStore<KEYOUT, VALUEOUT> dataStore = DataStoreFactory.getDataStore(dataStoreClass
            , classKeyOut, classValueOut, job.getConfiguration());
    GoraOutputFormat.setOutput(job, dataStore, true);
    HadoopOutputFormat<KEYOUT, VALUEOUT> wrappedGoraOutput =
            new HadoopOutputFormat<>(
                    new GoraOutputFormat<>(), job);
    // Temp fix to prevent NullPointerException from Flink side.
    Path tempPath = Files.createTempDirectory("temp");
    job.getConfiguration().set("mapred.output.dir", tempPath.toAbsolutePath().toString());
    return wrappedGoraOutput;
  }

}
