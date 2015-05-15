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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Pump moves data from one source to other.
 * It can be used to import data from text file, or copy store from memory to disk.
 */
public final class Pump {


    private static final Logger LOG = Logger.getLogger(Pump.class.getName());

    /**
     * Sorts large data set by given {@code Comparator}. Data are sorted with in-memory cache and temporary files.
     *
     * @param source iterator over unsorted data
     * @param mergeDuplicates should be duplicate keys merged into single one?
     * @param batchSize how much items can fit into heap memory
     * @param comparator used to sort data
     * @param serializer used to store data in temporary files
     * @return iterator over sorted data set
     */
    public static <E> Iterator<E> sort(Iterator<E> source, boolean mergeDuplicates, final int batchSize,
            Comparator comparator, final Serializer serializer, Executor executor){
        if(batchSize<=0) throw new IllegalArgumentException();
        if(comparator==null)
            comparator=Fun.COMPARATOR;
        if(source==null)
            source = Fun.EMPTY_ITERATOR;

        int counter = 0;
        final Object[] presort = new Object[batchSize];
        final List<File> presortFiles = new ArrayList<File>();
        final List<Integer> presortCount2 = new ArrayList<Integer>();

        try{
            while(source.hasNext()){
                presort[counter]=source.next();
                counter++;

                if(counter>=batchSize){
                    //sort all items
                    arraySort(presort, presort.length, comparator ,executor);

                    //flush presort into temporary file
                    File f = File.createTempFile("mapdb","sort");
                    f.deleteOnExit();
                    presortFiles.add(f);
                    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
                    for(Object e:presort){
                        serializer.serialize(out,e);
                    }
                    out.close();
                    presortCount2.add(counter);
                    Arrays.fill(presort,0);
                    counter = 0;
                }
            }
            //now all records from source are fetch
            if(presortFiles.isEmpty()){
                //no presort files were created, so on-heap sorting is enough
                arraySort(presort, counter, comparator, executor);
                return arrayIterator(presort,0, counter);
            }

            final int[] presortCount = new int[presortFiles.size()];
            for(int i=0;i<presortCount.length;i++) presortCount[i] = presortCount2.get(i);
            //compose iterators which will iterate over data saved in files
            Iterator[] iterators = new Iterator[presortFiles.size()+1];
            final DataInputStream[] ins = new DataInputStream[presortFiles.size()];
            for(int i=0;i<presortFiles.size();i++){
                ins[i] = new DataInputStream(new BufferedInputStream(new FileInputStream(presortFiles.get(i))));
                final int pos = i;
                iterators[i] = new Iterator(){

                    @Override public boolean hasNext() {
                        return presortCount[pos]>0;
                    }

                    @Override public Object next() {
                        try {
                            Object ret =  serializer.deserialize(ins[pos],-1);
                            if(--presortCount[pos]==0){
                                ins[pos].close();
                                presortFiles.get(pos).delete();
                            }
                            return ret;
                        } catch (IOException e) {
                            throw new IOError(e);
                        }
                    }

                    @Override public void remove() {
                        //ignored
                    }

                };
            }

            //and add iterator over data on-heap
            arraySort(presort, counter, comparator, executor);
            iterators[iterators.length-1] = arrayIterator(presort,0,counter);

            //and finally sort presorted iterators and return iterators over them
            return sort(comparator, mergeDuplicates, iterators);

        }catch(IOException e){
            throw new IOError(e);
        }finally{
            for(File f:presortFiles) f.delete();
        }
    }

    /**
     * Reflection method {@link Arrays#parallelSort(Object[], int, int, Comparator)}.
     * Is not invoked directly to keep compatibility with java8
     */
    static private Method parallelSortMethod;
    static{
        try {
            parallelSortMethod = Arrays.class.getMethod("parallelSort", Object[].class, int.class, int.class, Comparator.class);
        } catch (NoSuchMethodException e) {
            //java 6 & 7
            parallelSortMethod = null;
        }
    }

    protected static void arraySort(Object[] array, int arrayLen, Comparator comparator,  Executor executor) {
        //if executor is specified, try to use parallel method in java 8
        if(executor!=null && parallelSortMethod!=null){
            //TODO this uses common pool, but perhaps we should use Executor instead
            try {
                parallelSortMethod.invoke(null, array, 0, arrayLen, comparator);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e); //TODO exception hierarchy here?
            }
        }
        Arrays.sort(array, 0, arrayLen, comparator);
    }


    /**
     * Merge presorted iterators into single sorted iterator.
     *
     * @param comparator used to compare data
     * @param mergeDuplicates if duplicate keys should be merged into single one
     * @param iterators array of already sorted iterators
     * @return sorted iterator
     */
    public static <E> Iterator<E> sort(Comparator comparator, final boolean mergeDuplicates, final Iterator... iterators) {
        final Comparator comparator2 = comparator==null?Fun.COMPARATOR:comparator;
        return new Iterator<E>(){

            final NavigableSet<Object[]> items = new TreeSet<Object[]>(
                    new Fun.ArrayComparator(new Comparator[]{comparator2,Fun.COMPARATOR}));

            Object next = this; //is initialized with this so first `next()` will not throw NoSuchElementException

            {
                for(int i=0;i<iterators.length;i++){
                    if(iterators[i].hasNext()){
                        items.add(new  Object[]{iterators[i].next(), i});
                    }
                }
                next();
            }


            @Override public boolean hasNext() {
                return next!=null;
            }

            @Override public E next() {
                if(next == null)
                    throw new NoSuchElementException();

                Object oldNext = next;

                Object[] lo = items.pollFirst();
                if(lo == null){
                    next = null;
                    return (E) oldNext;
                }

                next = lo[0];

                if(oldNext!=this && comparator2.compare(oldNext,next)>0){
                    throw new IllegalArgumentException("One of the iterators is not sorted");
                }

                Iterator iter = iterators[(Integer)lo[1]];
                if(iter.hasNext()){
                    items.add(new Object[]{iter.next(),lo[1]});
                }

                if(mergeDuplicates){
                    while(true){
                        Iterator<Object[]> subset = Fun.filter(items,next).iterator();
                        if(!subset.hasNext())
                            break;
                        List toadd = new ArrayList();
                        while(subset.hasNext()){
                            Object[] t = subset.next();
                            items.remove(t);
                            iter = iterators[(Integer)t[1]];
                            if(iter.hasNext())
                                toadd.add(new Object[]{iter.next(),t[1]});
                        }
                        items.addAll(toadd);
                    }
                }


                return (E) oldNext;
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }


    /**
     * Merges multiple iterators into single iterator.
     * Result iterator will return entries from all iterators.
     * It does not do sorting or any other special functionality.
     * Does not allow null elements.
     *
     * @param iters - iterators to be merged
     * @return union of all iterators.
     */
    public static <E> Iterator<E> merge(Executor executor, final Iterator... iters){
        if(iters.length==0)
            return Fun.EMPTY_ITERATOR;

        final Iterator<E> ret = new Iterator<E>() {
                int i = 0;
                Object next = this;

                {
                    next();
                }

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public E next() {
                    if (next == null)
                        throw new NoSuchElementException();

                    //move to next iterator if necessary
                    while (!iters[i].hasNext()) {
                        i++;
                        if (i == iters.length) {
                            //reached end of iterators
                            Object ret = next;
                            next = null;
                            return (E) ret;
                        }
                    }

                    //take next item from iterator
                    Object ret = next;
                    next = iters[i].next();
                    return (E) ret;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };


        if(executor == null){
            //single threaded
            return ret;
        }

        final Object poisonPill = new Object();

        //else perform merge in separate thread and use blocking queue
        final BlockingQueue q = new ArrayBlockingQueue(128);
        //feed blocking queue in separate thread
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        while (ret.hasNext())
                            q.put(ret.next());
                    } finally {
                        q.put(poisonPill); //TODO poison pill should be send in non blocking way, perhaps remove elements?
                    }
                } catch (InterruptedException e) {
                    LOG.log(Level.SEVERE, "feeder failed", e);
                }
            }
        });

        return poisonPillIterator(q,poisonPill);
    }

    public static <E> Iterator<E> poisonPillIterator(final BlockingQueue<E> q, final Object poisonPill) {

        return new Iterator<E>() {

            E next = getNext();

            private E getNext() {
                try {
                    E ret = q.take();
                    if(ret==poisonPill)
                        return null;
                    return ret;
                } catch (InterruptedException e) {
                    throw new DBException.Interrupted(e);
                }

            }

            @Override
            public boolean hasNext() {
                return next!=null;
            }

            @Override
            public E next() {
                E ret = next;
                if(ret == null)
                    throw new NoSuchElementException();
                next = getNext();
                return ret;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Build BTreeMap (or TreeSet) from presorted data.
     * This method is much faster than usual import using {@code Map.put(key,value)} method.
     * It is because tree integrity does not have to be maintained and
     * tree can be created in linear way with.
     *
     * This method expect data to be presorted in **reverse order** (highest to lowest).
     * There are technical reason for this requirement.
     * To sort unordered data use {@link Pump#sort(java.util.Iterator, boolean, int, java.util.Comparator, Serializer, Executor)}
     *
     * This method does not call commit. You should disable Write Ahead Log when this method is used {@link DBMaker.Maker#transactionDisable()}
     *
     *
     * @param source iterator over source data, must be reverse sorted
     * @param keyExtractor transforms items from source iterator into keys. If null source items will be used directly as keys.
     * @param valueExtractor transforms items from source iterator into values. If null BTreeMap will be constructed without values (as Set)
     * @param ignoreDuplicates should be duplicate keys merged into single one?
     * @param nodeSize maximal BTree node size before it is splited.
     * @param valuesStoredOutsideNodes if true values will not be stored as part of BTree nodes
     * @param counterRecid TODO make size counter friendly to use
     * @param keySerializer serializer for keys, use null for default value
     * @param valueSerializer serializer for value, use null for default value
     * @throws org.mapdb.DBException.PumpSourceNotSorted if source iterator is not reverse sorted
     * @throws org.mapdb.DBException.PumpSourceDuplicate if source iterator has duplicates
     */
    public static  <E,K,V> long buildTreeMap(Iterator<E> source,
                                             Engine engine,
                                             Fun.Function1<K, E> keyExtractor,
                                             Fun.Function1<V, E> valueExtractor,
                                             boolean ignoreDuplicates,
                                             int nodeSize,
                                             boolean valuesStoredOutsideNodes,
                                             long counterRecid,
                                             BTreeKeySerializer keySerializer,
                                             Serializer<V> valueSerializer,
                                             Executor executor){

        //TODO upper levels of tree  could be created in separate thread

        if(keyExtractor==null)
            keyExtractor= (Fun.Function1<K, E>) Fun.extractNoTransform();
        if(valueSerializer==null){
            //this is set
            valueSerializer = (Serializer<V>) Serializer.BOOLEAN;
            if(valueExtractor!=null)
                throw new IllegalArgumentException();
            valueExtractor = new Fun.Function1() {
                @Override
                public Object run(Object e) {
                    return Boolean.TRUE;
                }
            };
        }

        // update source iterator with new one, which just ignores duplicates
        if(ignoreDuplicates){
            source = ignoreDuplicatesIterator(source,keySerializer.comparator(), keyExtractor);
        }

        source = checkSortedIterator(source,keySerializer.comparator(), keyExtractor);

        final double NODE_LOAD = 0.75;
        // split if node is bigger than this
        final int maxNodeSize = (int) (nodeSize * NODE_LOAD);

        // temporary serializer for nodes
        Serializer<BTreeMap.BNode> nodeSerializer = new BTreeMap.NodeSerializer(valuesStoredOutsideNodes,keySerializer,valueSerializer,0);

        //hold tree structure
        ArrayList<ArrayList<K>> dirKeys = new ArrayList();
        dirKeys.add(new ArrayList());
        ArrayList<ArrayList<Long>> dirRecids = new ArrayList();
        dirRecids.add(arrayList(0L));

        ArrayList<K> leafKeys = new ArrayList<K>();
        ArrayList<Object> leafValues = new ArrayList<Object>();

        long counter = 0;
        long rootRecid = 0;
        long lastLeafRecid = 0;

        SOURCE_LOOP:
        while(source.hasNext()){
            E iterNext = source.next();
            final boolean isLeftMost = !source.hasNext();
            counter++;

            final K key = keyExtractor.run(iterNext);

            Object value = valueExtractor.run(iterNext);
            if(valuesStoredOutsideNodes) {
                long recid = engine.put((V) value, valueSerializer);
                value = new BTreeMap.ValRef(recid);
            }

            leafKeys.add(key);


            // if is not last and is small enough, do not split
            if(!isLeftMost && leafKeys.size()<=maxNodeSize) {
                leafValues.add(value);
                continue SOURCE_LOOP;
            }

            if(isLeftMost) {
                leafValues.add(value);
            }

            Collections.reverse(leafKeys);
            Collections.reverse(leafValues);

            BTreeMap.LeafNode leaf = new BTreeMap.LeafNode(
                    keySerializer.arrayToKeys(leafKeys.toArray()),
                    isLeftMost,             //left most
                    lastLeafRecid==0,   //right most
                    false,
                    valueSerializer.valueArrayFromArray(leafValues.toArray()),
                    lastLeafRecid
            );

            lastLeafRecid = engine.put(leaf,nodeSerializer);

            //handle case when there is only single leaf and no dirs, in that case it will become root
            if(isLeftMost && dirKeys.get(0).size()==0){
                rootRecid = lastLeafRecid;
                break SOURCE_LOOP;
            }

            //update parent directory
            K leafLink = leafKeys.get(0);

            dirKeys.get(0).add(leafLink);
            dirRecids.get(0).add(lastLeafRecid);

            leafKeys.clear();
            leafValues.clear();

            if(!isLeftMost){
                leafKeys.add(key);
                leafKeys.add(key);
                leafValues.add(value);
            }


            // iterate over keys and save them if too large or is last
            for(int level=0;
                level<dirKeys.size();
                level++){

                ArrayList<K> keys = dirKeys.get(level);

                //break loop if current level does not need saving
                //that means this is not last entry and size is small enough
                if(!isLeftMost && keys.size()<=maxNodeSize){
                    continue SOURCE_LOOP;
                }
                if(isLeftMost){
                    //remove redundant first key
                    keys.remove(keys.size()-1);
                }


                //node needs saving

                Collections.reverse(keys);
                List<Long> recids = dirRecids.get(level);
                Collections.reverse(recids);

                boolean isRightMost = (level+1 == dirKeys.size());

                //construct node
                BTreeMap.DirNode dir = new BTreeMap.DirNode(
                    keySerializer.arrayToKeys(keys.toArray()),
                    isLeftMost,
                    isRightMost,
                    false,
                    toLongArray(recids)
                );

                //finally save
                long dirRecid = engine.put(dir,nodeSerializer);

                //if its both most left and most right, save it as new root
                if(isLeftMost && isRightMost) {
                    rootRecid = dirRecid;
                    break SOURCE_LOOP;
                }

                //prepare next directory at the same level, clear and add link to just saved node
                K linkKey = keys.get(0);
                keys.clear();
                recids.clear();
                keys.add(linkKey);
                recids.add(dirRecid);

                //now update directory at parent level
                if(dirKeys.size()==level+1){
                    //dir is empty, so it needs updating
                    dirKeys.add(new ArrayList<K>());
                    dirRecids.add(arrayList(0L));
                }
                dirKeys.get(level+1).add(linkKey);
                dirRecids.get(level+1).add(dirRecid);
            }
        }

        //handle empty iterator, insert empty node
        if(rootRecid == 0) {
            BTreeMap.LeafNode emptyRoot = new BTreeMap.LeafNode(
                    keySerializer.emptyKeys(),
                    true,
                    true,
                    false,
                    valueSerializer.valueArrayEmpty(),
                    0L);

            rootRecid = engine.put(emptyRoot, nodeSerializer);
        }

        if(counterRecid!=0)
            engine.update(counterRecid,counter,Serializer.LONG);


        return engine.put(rootRecid,Serializer.RECID);
    }

    private static <E,K> Iterator<E> checkSortedIterator(final Iterator<E> source, final Comparator comparator, final Fun.Function1<K, E> keyExtractor) {
        return new Iterator<E>() {

            E next = source.hasNext()?
                    source.next():null;


            E advance(){
                if(!source.hasNext())
                    return null;
                E ret = source.next();
                //check order

                int compare = comparator.compare(
                        keyExtractor.run(ret),
                        keyExtractor.run(next));
                if(compare==0){
                    throw new DBException.PumpSourceDuplicate(next);
                }
                if(compare>0) {
                    throw new DBException.PumpSourceNotSorted();
                }

                return ret;
            }

            @Override
            public boolean hasNext() {
                return next!=null;
            }

            @Override
            public E next() {
                if(next==null)
                    throw new NoSuchElementException();

                E ret = next;
                next = advance();
                return ret;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

    }

    private static <E,K> Iterator<E> ignoreDuplicatesIterator(final Iterator<E> source, final Comparator<K> comparator, final Fun.Function1<K, E> keyExtractor) {
        return new Iterator<E>() {

            E next = source.hasNext()?
                    source.next():null;


            E advance(){
                while(source.hasNext()){
                    E n = source.next();
                    if(comparator.compare(
                            keyExtractor.run(n),
                            keyExtractor.run(next))
                            ==0){
                        continue; //ignore duplicate
                    }
                    return n; // new element
                }
                return null; //no more entries in iterator
            }

            @Override
            public boolean hasNext() {
                return next!=null;
            }

            @Override
            public E next() {
                if(next==null)
                    throw new NoSuchElementException();

                E ret = next;
                next = advance();
                return ret;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static Object toLongArray(List<Long> child) {
        boolean allInts = true;
        for(Long l:child){
            if(l>Integer.MAX_VALUE) {
                allInts = false;
                break;
            }

        }
        if(allInts){
            int[] ret = new int[child.size()];
            for(int i=0;i<ret.length;i++){
                ret[i] = child.get(i).intValue();
            }
            return ret;
        }else{
            long[] ret = new long[child.size()];
            for(int i=0;i<ret.length;i++){
                ret[i] = child.get(i);
            }
            return ret;
        }
    }

    /** create array list with single element*/
    private static <E> ArrayList<E> arrayList(E item){
        ArrayList<E> ret = new ArrayList<E>();
        ret.add(item);
        return ret;
    }

    private static <E> Iterator<E> arrayIterator(final Object[] array, final int fromIndex, final int toIndex) {
        return new Iterator<E>(){

            int index = fromIndex;

            @Override
            public boolean hasNext() {
                return index<toIndex;
            }

            @Override
            public E next() {
                if(index>=toIndex) throw new NoSuchElementException();
                return (E) array[index++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <K, V,A> void fillHTreeMap(final HTreeMap<K, V> m,
                                             Iterator<A> pumpSource,
                                             final Fun.Function1<K,A> pumpKeyExtractor,
                                             Fun.Function1<V,A> pumpValueExtractor,
                                             int pumpPresortBatchSize, boolean pumpIgnoreDuplicates,
                                             Serializer<A> sortSerializer,
                                             Executor executor
                                            ) {

        //first sort by hash code
        Comparator hashComparator = new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                o1 = pumpKeyExtractor.run((A) o1);
                o2 = pumpKeyExtractor.run((A) o2);
                int h1 = m.hash(o1);
                int h2 = m.hash(o2);
                if(h1<h2)
                    return -1;
                if(h1==h2)
                    return 0;
                return 1;
            }
        };

        pumpSource = sort(pumpSource,false,pumpPresortBatchSize,hashComparator,sortSerializer,executor);


        //got sorted, now fill the map
        while(pumpSource.hasNext()){
            A o = pumpSource.next();
            K key = pumpKeyExtractor.run(o);
            V val = pumpValueExtractor==null? (V) Boolean.TRUE : pumpValueExtractor.run(o);
            if(pumpIgnoreDuplicates) {
                m.put(key,val);
            }else{
                Object old = m.putIfAbsent(key,val);
                if(old!=null)
                    throw new IllegalArgumentException("Duplicate at: "+o.toString());
            }
        }

    }

    public static void copy(DB src, DB target) {
        //TODO implement
    }
}
