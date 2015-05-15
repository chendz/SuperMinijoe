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

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * Centerpiece for record management, {@code Engine} is simple key value store.
 * Engine is low-level interface and is not meant to be used directly
 * by user. For most operations user should use {@link org.mapdb.DB} class.
 * </p><p>
 *
 * In this store key is primitive {@code long} number, typically pointer to index table.
 * Value is class instance. To turn value into/from binary form serializer is
 * required as extra argument for most operations.
 * </p><p>
 *
 * Unlike other DBs MapDB does not expect user to (de)serialize data before
 * they are passed as arguments. Instead MapDB controls (de)serialization itself.
 * This gives DB a lot of flexibility: for example instances may be held in
 * cache to minimise number of deserializations, or modified instance can
 * be placed into queue and asynchronously written on background thread.
 * </p><p>
 *
 * There is {@link Store} subinterface for raw persistence
 * </p><p>
 *
 * In default configuration MapDB runs with this {@code Engine} stack:
 * </p>
 *
 * <ol>
 *  <li> <b>DISK</b> - raw file or memory
 *  <li> {@link org.mapdb.StoreWAL} - permanent record store with transactions
 *  <li> <b>USER</b> - {@link DB} and collections
 * </ol>
 *
 * <p>
 * TODO document more examples of Engine  wrappers
 * </p><p>
 *
 * Engine uses {@code recid} to identify records. There is zero error handling in case recid is invalid
 * (random number or already deleted record). Passing illegal recid may result into anything
 * (return null, throw EOF or even corrupt store). Engine is considered low-level component
 * and it is responsibility of upper layers (collections) to ensure recid is consistent.
 * Lack of error handling is trade of for speed (similar way as manual memory management in C++)
 * </p><p>
 *
 * Engine must support {@code null} record values. You may insert, update and fetch null records.
 * Nulls play important role in recid preallocation and asynchronous writes.
 * </p><p>
 * Recid can be reused after it was deleted. If your application relies on unique being unique,
 * you should update record with null value, instead of delete.
 * Null record consumes only 8 bytes in store and is preserved during defragmentation.
 * </p>
 * @author Jan Kotek
 */
public interface Engine  extends Closeable {

    /**
     * <p>
     * Content of this map is manipulated by {@link org.mapdb.DB} class.
     * </p>
     * There are 8 reserved record ids. They store information relevant to
     * {@link org.mapdb.DB} and higher level functions. Those are preallocated when store is created.
     */
    long RECID_NAME_CATALOG = 1;

    /**
     * <p>
     * Points to class catalog. A list of classes used in {@link org.mapdb.SerializerPojo}
     * to serialize java objects.
     * </p>
     * There are 8 reserved record ids. They store information relevant to
     * {@link org.mapdb.DB} and higher level functions. Those are preallocated when store is created.
     */
    long RECID_CLASS_CATALOG = 2;

    /**
     * <p>
     * Recid used for 'record check'. This record is loaded when store is open,
     * to ensure configuration such as encryption and compression is correctly set and \
     * data are read-able.
     * </p><p>
     * There are 8 reserved record ids. They store information relevant to
     * {@link org.mapdb.DB} and higher level functions. Those are preallocated when store is created.
     * </p>
     */
    long RECID_RECORD_CHECK = 3;

    /**
     * <p>
     * There are 8 reserved record ids. They store information relevant to
     * {@link org.mapdb.DB} and higher level functions. Those are preallocated when store is created.
     * </p><p>
     * This value is last reserved record id. User ids (recids returned by {@link Engine#put(Object, Serializer)})
     * starts from {@code RECID_LAST_RESERVED+1}
     * </p>
     */
    long RECID_LAST_RESERVED = 7;

    /**
     * <p>
     * There are 8 reserved record ids. They store information relevant to
     * {@link org.mapdb.DB} and higher level functions. Those are preallocated when store is created.
     * </p><p>
     * This constant is first recid available to user. It is first value returned by {@link #put(Object, Serializer)} if store is empty.
     * </p>
     */
    long RECID_FIRST = RECID_LAST_RESERVED+1;

    /**
     * Preallocates recid for not yet created record. It does not insert any data into it.
     * @return new recid
     */
    //TODO in some cases recid is persisted and used between compaction. perhaps use put(null)
    //TODO clarify difference between put/update(null) and delete/preallocate
    long preallocate();


    /**
     * Insert new record.
     *
     * @param value records to be added
     * @param serializer used to convert record into/from binary form
     * @return recid (record identifier) under which record is stored.
     * @throws java.lang.NullPointerException if serializer is null
     */
    <A> long put(A value, Serializer<A> serializer);


    /**
     * <p>
     * Get existing record.
     * </p><p>
     *
     * Recid must be a number returned by 'put' method.
     * Behaviour for invalid recid (random number or already deleted record)
     * is not defined, typically it returns null or throws 'EndOfFileException'
     * </p>
     *
     * @param recid (record identifier) under which record was persisted
     * @param serializer used to deserialize record from binary form
     * @return record matching given recid, or null if record is not found under given recid.
     * @throws java.lang.NullPointerException if serializer is null
     */
    <A> A get(long recid, Serializer<A> serializer);

    /**
     * <p>
     * Update existing record with new value.
     * </p><p>
     * Recid must be a number returned by 'put' method.
     * Behaviour for invalid recid (random number or already deleted record)
     * is not defined, typically it throws 'EndOfFileException',
     * but it may also corrupt store.
     * </p>
     *
     * @param recid (record identifier) under which record was persisted.
     * @param value new record value to be stored
     * @param serializer used to serialize record into binary form
     * @throws java.lang.NullPointerException if serializer is null
     */
    <A> void update(long recid, A value, Serializer<A> serializer);

    /**
     * <p>
     * Updates existing record in atomic <a href="http://en.wikipedia.org/wiki/Compare-and-swap">(Compare And Swap)</a> manner.
     * Value is modified only if old value matches expected value. There are three ways to match values, MapDB may use any of them:
     * </p>
     * <ol>
     *    <li>Equality check <code>oldValue==expectedOldValue</code> when old value is found in instance cache</li>
     *    <li>Deserializing <code>oldValue</code> using <code>serializer</code> and checking <code>oldValue.equals(expectedOldValue)</code></li>
     *    <li>Serializing <code>expectedOldValue</code> using <code>serializer </code> and comparing binary array with already serialized <code>oldValue</code>
     * </ol>
     * <p>
     * Recid must be a number returned by 'put' method.
     * Behaviour for invalid recid (random number or already deleted record)
     * is not defined, typically it throws 'EndOfFileException',
     * but it may also corrupt store.
     * </p>
     *
     * @param recid (record identifier) under which record was persisted.
     * @param expectedOldValue old value to be compared with existing record
     * @param newValue to be written if values are matching
     * @param serializer used to serialize record into binary form
     * @return true if values matched and newValue was written
     * @throws java.lang.NullPointerException if serializer is null
     */
    <A> boolean compareAndSwap(long recid, A expectedOldValue, A newValue, Serializer<A> serializer);


    /**
     * <p>
     * Remove existing record from store/cache
     * </p><p>
     *
     * Recid must be a number returned by 'put' method.
     * Behaviour for invalid recid (random number or already deleted record)
     * is not defined, typically it throws 'EndOfFileException',
     * but it may also corrupt store.
     * </p>
     *
     * @param recid (record identifier) under which was record persisted
     * @param serializer which may be used in some circumstances to deserialize and store old object
     * @throws java.lang.NullPointerException if serializer is null
     */
    <A> void delete(long recid, Serializer<A>  serializer);

    /**
     * <p>
     * Close store/cache. This method must be called before JVM exits to flush all caches and prevent store corruption.
     * Also it releases resources used by MapDB (disk, memory..).
     * </p><p>
     *
     * Engine can no longer be used after this method was called. If Engine is used after closing, it may
     * throw any exception including <code>NullPointerException</code>
     * </p><p>
     *
     * There is an configuration option {@link DBMaker.Maker#closeOnJvmShutdown()} which uses shutdown hook to automatically
     * close Engine when JVM shutdowns.
     * </p>
     */
    void close();


    /**
     * Checks whether Engine was closed.
     *
     * @return true if engine was closed
     */
    public boolean isClosed();


    /**
     * Makes all changes made since the previous commit/rollback permanent.
     * In transactional mode (on by default) it means creating journal file and replaying it to storage.
     * In other modes it may flush disk caches or do nothing at all (check your config options)
     */
    void commit();

    /**
     * Undoes all changes made in the current transaction.
     * If transactions are disabled it throws {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException if transactions are disabled
     */
    void rollback() throws UnsupportedOperationException;

    /**
     * Check if you can write into this Engine. It may be readonly in some cases (snapshot, read-only files).
     *
     * @return true if engine is read-only
     */
    boolean isReadOnly();

    /** @return true if engine supports rollback*/
    boolean canRollback();

    /** @return true if engine can create read-only snapshots*/
    boolean canSnapshot();

    /**
     * Returns read-only snapshot of data in Engine.
     *
     * @throws UnsupportedOperationException if snapshots are not supported/enabled
     */
    Engine snapshot() throws UnsupportedOperationException;

    /** if this is wrapper return underlying engine, or null */
    Engine getWrappedEngine();

    /** clears any underlying cache */
    void clearCache();


    void compact();

    /**
     * Wraps an <code>Engine</code> and throws
     * <code>UnsupportedOperationException("Read-only")</code>
     * on any modification attempt.
     */
    public static final class ReadOnly implements Engine {


        protected final Engine engine;


        public ReadOnly(Engine engine){
            this.engine = engine;
        }

        @Override
        public long preallocate() {
            throw new UnsupportedOperationException("Read-only");
        }


        @Override
        public <A> boolean compareAndSwap(long recid, A expectedOldValue, A newValue, Serializer<A> serializer) {
            throw new UnsupportedOperationException("Read-only");
        }


        @Override
        public <A> long put(A value, Serializer<A> serializer) {
            throw new UnsupportedOperationException("Read-only");
        }

        @Override
        public <A> A get(long recid, Serializer<A> serializer) {
            return engine.get(recid,serializer);
        }

        @Override
        public <A> void update(long recid, A value, Serializer<A> serializer) {
            throw new UnsupportedOperationException("Read-only");
        }

        @Override
        public <A> void delete(long recid, Serializer<A> serializer){
            throw new UnsupportedOperationException("Read-only");
        }

        @Override
        public void close() {
             engine.close();
        }

        @Override
        public boolean isClosed() {
            return engine.isClosed();
        }

        @Override
        public void commit() {
            throw new UnsupportedOperationException("Read-only");
        }

        @Override
        public void rollback() {
            throw new UnsupportedOperationException("Read-only");
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }


        @Override
        public boolean canRollback() {
            return engine.canRollback();
        }

        @Override
        public boolean canSnapshot() {
            return true;
        }

        @Override
        public Engine snapshot() throws UnsupportedOperationException {
            return engine.snapshot();
        }

        @Override
        public Engine getWrappedEngine() {
            return engine;
        }

        @Override
        public void clearCache() {
            engine.clearCache();
        }

        @Override
        public void compact() {
            throw new UnsupportedOperationException("Read-only");
        }

    }


    /**
     * Closes Engine on JVM shutdown using shutdown hook: {@link Runtime#addShutdownHook(Thread)}
     * If engine was closed by user before JVM shutdown, hook is removed to save memory.
     */
    class CloseOnJVMShutdown implements Engine{


        final protected AtomicBoolean shutdownHappened = new AtomicBoolean(false);

        final Runnable hookRunnable = new Runnable() {
            @Override
            public void run() {
                shutdownHappened.set(true);
                CloseOnJVMShutdown.this.hook = null;
                if(CloseOnJVMShutdown.this.isClosed())
                    return;
                CloseOnJVMShutdown.this.close();
            }
        };

        protected final Engine engine;

        protected Thread hook;

        public CloseOnJVMShutdown(Engine engine) {
            this.engine = engine;
            hook = new Thread(hookRunnable,"MapDB shutdown hook");
            Runtime.getRuntime().addShutdownHook(hook);
        }


        @Override
        public long preallocate() {
            return engine.preallocate();
        }

        @Override
        public <A> long put(A value, Serializer<A> serializer) {
            return engine.put(value,serializer);
        }

        @Override
        public <A> A get(long recid, Serializer<A> serializer) {
            return engine.get(recid,serializer);
        }

        @Override
        public <A> void update(long recid, A value, Serializer<A> serializer) {
            engine.update(recid,value,serializer);
        }

        @Override
        public <A> boolean compareAndSwap(long recid, A expectedOldValue, A newValue, Serializer<A> serializer) {
            return engine.compareAndSwap(recid,expectedOldValue,newValue,serializer);
        }

        @Override
        public <A> void delete(long recid, Serializer<A> serializer) {
            engine.delete(recid,serializer);
        }

        @Override
        public void close() {
            engine.close();
            if(!shutdownHappened.get() && hook!=null){
                Runtime.getRuntime().removeShutdownHook(hook);
            }
            hook = null;
        }

        @Override
        public boolean isClosed() {
            return engine.isClosed();
        }

        @Override
        public void commit() {
            engine.commit();
        }

        @Override
        public void rollback() throws UnsupportedOperationException {
            engine.rollback();
        }

        @Override
        public boolean isReadOnly() {
            return engine.isReadOnly();
        }

        @Override
        public boolean canRollback() {
            return engine.canRollback();
        }

        @Override
        public boolean canSnapshot() {
            return engine.canSnapshot();
        }

        @Override
        public Engine snapshot() throws UnsupportedOperationException {
            return engine.snapshot();
        }

        @Override
        public Engine getWrappedEngine() {
            return engine;
        }

        @Override
        public void clearCache() {
            engine.clearCache();
        }

        @Override
        public void compact() {
            engine.compact();
        }

    }
    /** throws {@code IllegalAccessError("already closed")} on all access */
    Engine CLOSED_ENGINE = new Engine(){


        @Override
        public long preallocate() {
            throw new IllegalAccessError("already closed");
        }


        @Override
        public <A> long put(A value, Serializer<A> serializer) {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public <A> A get(long recid, Serializer<A> serializer) {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public <A> void update(long recid, A value, Serializer<A> serializer) {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public <A> boolean compareAndSwap(long recid, A expectedOldValue, A newValue, Serializer<A> serializer) {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public <A> void delete(long recid, Serializer<A> serializer) {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public void close() {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public boolean isClosed() {
            return true;
        }

        @Override
        public void commit() {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public void rollback() throws UnsupportedOperationException {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public boolean isReadOnly() {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public boolean canRollback() {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public boolean canSnapshot() {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public Engine snapshot() throws UnsupportedOperationException {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public Engine getWrappedEngine() {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public void clearCache() {
            throw new IllegalAccessError("already closed");
        }

        @Override
        public void compact() {
            throw new IllegalAccessError("already closed");
        }


    };
}
