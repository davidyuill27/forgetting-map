package com.origami;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ForgettingMapTest {

    private ForgettingMap<Integer, String> forgettingMap;
    private final String TEST_STRING_CONTENT = "TEST";

    @Before
    public void setup() {
        int CAPACITY = 10;
        forgettingMap = new ForgettingMap<Integer, String>(CAPACITY);
    }

    /**
     * Tests adding an association.
     */
    @Test
    public void testAddAssociation() {
        assertTrue(forgettingMap.getImmutableBackingMap().isEmpty());
        forgettingMap.add(1, TEST_STRING_CONTENT);
        assertFalse(forgettingMap.getImmutableBackingMap().isEmpty());
        assertEquals(TEST_STRING_CONTENT, forgettingMap.getImmutableBackingMap().get(1));
    }

    /**
     * Tests that adding an association using a key that already exists replaces the content and resets the counter.
     */
    @Test
    public void testAddSameKeyAssociationOnMaxCapacity() {
        forgettingMap = new ForgettingMap<>(2);
        forgettingMap.add(1, TEST_STRING_CONTENT);
        forgettingMap.add(2, TEST_STRING_CONTENT);
        assertEquals(TEST_STRING_CONTENT, forgettingMap.find(2));
        assertEquals(1, forgettingMap.getImmutableAssociationCounterMap().get(2).getCount().intValue());

        String OTHER_TEST_STRING_CONTENT = "OTHER_TEST";
        forgettingMap.add(2, OTHER_TEST_STRING_CONTENT);
        assertEquals(OTHER_TEST_STRING_CONTENT, forgettingMap.getImmutableBackingMap().get(2));
        assertEquals(0, forgettingMap.getImmutableAssociationCounterMap().get(2).getCount().intValue());
    }

    /**
     * Tests that the capacity limit is functioning and the correct association is removed and replaced.
     */
    @Test
    public void testMaxCapacityLeastFoundRemoved() {
        forgettingMap = new ForgettingMap<>(2);
        forgettingMap.add(1, TEST_STRING_CONTENT);
        forgettingMap.add(2, TEST_STRING_CONTENT);
        forgettingMap.find(2);
        forgettingMap.add(3, TEST_STRING_CONTENT);
        assertNull(forgettingMap.find(1));
        assertEquals(TEST_STRING_CONTENT, forgettingMap.find(3));
    }

    /**
     * Tests that multiple threads can add and remove at the same time.
     *
     * @throws InterruptedException for invocation.
     */
    @Test
    public void testThreadSafetyOfForgettingMap() throws InterruptedException {
        forgettingMap = new ForgettingMap<>(10000);
        List<Callable<String>> tasks = new ArrayList<>();
        IntStream.range(0, 1000000).forEach(value ->
                tasks.add(() -> {
                    forgettingMap.add(value, TEST_STRING_CONTENT);
                    return "Done";
                }));
        IntStream.range(0, 1000000).forEach(value ->
                tasks.add(() -> {
                    forgettingMap.remove(value);
                    return "Done";
                }));
        Collections.shuffle(tasks);
        ExecutorService service = Executors.newFixedThreadPool(100);
        service.invokeAll(tasks);
    }

    /**
     * Tests the find function :
     * returns null on non-existent key;
     * returns correct content;
     * increments the association counter;
     */
    @Test
    public void testFindAssociation() {
        assertNull(forgettingMap.find(1));
        forgettingMap.add(1, TEST_STRING_CONTENT);
        String returnedContent = forgettingMap.find(1);
        assertEquals(TEST_STRING_CONTENT, returnedContent);
        assertEquals(1, forgettingMap.getImmutableAssociationCounterMap().get(1).getCount().intValue());
    }

    /**
     * Tests removing an association.
     */
    @Test
    public void testRemoveAssociation() {
        assertNull(forgettingMap.remove(1));
        forgettingMap.add(1, TEST_STRING_CONTENT);
        assertEquals(TEST_STRING_CONTENT, forgettingMap.remove(1));
    }

    /**
     * Tests that the Comparable implementation works as expected.
     *
     * @throws InterruptedException for thread sleep
     */
    @Test
    public void testCounterAndTimeCompare() throws InterruptedException {
        ForgettingMap.CountAndTime countAndTime1 = new ForgettingMap.CountAndTime();
        Thread.sleep(100L);
        ForgettingMap.CountAndTime countAndTime2 = new ForgettingMap.CountAndTime();
        Thread.sleep(100L);
        ForgettingMap.CountAndTime countAndTime3 = new ForgettingMap.CountAndTime();
        countAndTime3.incrementCount();
        Thread.sleep(100L);

        List<ForgettingMap.CountAndTime> listToBeOrdered = new ArrayList<>();
        listToBeOrdered.add(countAndTime3); //should be last in list
        listToBeOrdered.add(countAndTime1); //should be first in list
        listToBeOrdered.add(countAndTime2); //should be middle of list
        Collections.sort(listToBeOrdered);
        Assert.assertEquals(countAndTime1, listToBeOrdered.get(0));
        Assert.assertEquals(countAndTime2, listToBeOrdered.get(1));
        Assert.assertEquals(countAndTime3, listToBeOrdered.get(2));
    }
}