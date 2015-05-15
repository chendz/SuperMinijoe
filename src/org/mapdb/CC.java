/*
 *  Copyright (c) 2012 Jan Kotek
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.mapdb;

/**
 * <p>
 * Compiler Configuration. There are some static final boolean fields, which describe features MapDB was compiled with.
 * </p><p>
 *
 * MapDB can be compiled with/without some features. For example fine logging is useful for debugging,
 * but should not be present in production version. Java does not have preprocessor so
 * we use <a href="http://en.wikipedia.org/wiki/Dead_code_elimination">Dead code elimination</a> to achieve it.
 * </p><p>
 *
 * Typical usage:
 * </p>
 * <pre>{@code
 *     if(CC.ASSERT && arg.calculateSize()!=33){  //calculateSize may take long time
 *         throw new IllegalArgumentException("wrong size");
 *     }
 * }</pre>
 *
 *
 * @author  Jan Kotek
 */
public interface CC {

    /**
     * Compile with more assertions and verifications.
     * For example HashMap may check if keys implements hash function correctly.
     * This will slow down MapDB significantly.
     */
    boolean ASSERT = true;

    boolean PARANOID = false;


    /**
     * Compile-in detailed log messages from store.
     */
    boolean LOG_STORE = false;

    /**
     * Compile-in detailed log messages from Engine Wrappers
     */
    boolean LOG_EWRAP = false;

//    /**
//     * Log lock/unlock events. Useful to diagnose deadlocks
//     */
//    boolean LOG_LOCKS = false;
//
//    /**
//     * If true MapDB will display warnings if user is using MapDB API wrong way.
//     */
//    boolean LOG_HINTS = true;



    /**
     * Compile-in detailed log messages from HTreeMap.
     */
    boolean LOG_HTREEMAP = false;


    /**
     * <p>
     * Default concurrency level. Should be greater than number of threads accessing
     * MapDB concurrently. On other side larger number consumes more memory
     * </p><p>
     *
     * This number must be power of two: {@code CONCURRENCY = 2^N}
     * </p>
     */
    int DEFAULT_LOCK_SCALE = 16;


//    int BTREE_DEFAULT_MAX_NODE_SIZE = 32;


    int DEFAULT_CACHE_SIZE = 2048;

    String DEFAULT_CACHE = DBMaker.Keys.cache_disable;

    /** default executor scheduled rate for {@link org.mapdb.Store.Cache.WeakSoftRef} */
    long DEFAULT_CACHE_EXECUTOR_PERIOD = 1000;

    int DEFAULT_FREE_SPACE_RECLAIM_Q = 5;

    /** controls if locks used in MapDB are fair */
    boolean FAIR_LOCKS = false;


    int VOLUME_PAGE_SHIFT = 20; // 1 MB

    boolean STORE_INDEX_CRC = false; //TODO move to feature bit field

    /**
     * Will print stack trace of all operations which are write any data at given offset
     * Used for debugging.
     */
    long VOLUME_PRINT_STACK_AT_OFFSET = 0;


    long DEFAULT_HTREEMAP_EXECUTOR_PERIOD = 1000;
    long DEFAULT_STORE_EXECUTOR_SCHED_RATE = 1000;

    long DEFAULT_METRICS_LOG_PERIOD = 10000;

    boolean METRICS_CACHE = true;
    boolean METRICS_STORE = true;

    int DEFAULT_ASYNC_WRITE_QUEUE_SIZE = 1024;

    Volume.VolumeFactory DEFAULT_MEMORY_VOLUME_FACTORY = Volume.ByteArrayVol.FACTORY;

    //TODO AppendStoreTest par* test fails if this changes  to FileChannelVol
    Volume.VolumeFactory DEFAULT_FILE_VOLUME_FACTORY = Volume.RandomAccessFileVol.FACTORY;
}

