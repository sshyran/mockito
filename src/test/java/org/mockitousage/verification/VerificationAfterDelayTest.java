/*
 * Copyright (c) 2007 Mockito contributors This program is made available under the terms of the MIT License.
 */

package org.mockitousage.verification;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mockitoutil.Stopwatch.createNotStarted;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoAssertionError;
import org.mockito.junit.MockitoRule;
import org.mockitousage.IMethods;
import org.mockitoutil.Stopwatch;

public class VerificationAfterDelayTest {

    @Rule
    public MockitoRule mockito = rule();

    @Rule
    public ExpectedException exception = none();

    @Mock
    private IMethods mock;

    @Captor
    private ArgumentCaptor<Character> captor;

    private ScheduledExecutorService executor;

    private Stopwatch stopWatch;

    @Before
    public void setUp() {
        executor = newSingleThreadScheduledExecutor();
        stopWatch = createNotStarted();
    }

    @After
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(5, SECONDS);
    }

    @Test
    public void shouldVerifyNormallyWithSpecificTimes() throws Exception {
        // given
        DelayedExecution delayedExecution = new DelayedExecution(executor, mock, 20, MILLISECONDS);
        delayedExecution.recordAsyncCall('1');

        // when
        delayedExecution.allAsyncCallsStarted();

        // then
        verify(mock, after(100).times(1)).oneArg('1');
    }

    @Test
    public void shouldVerifyNormallyWithAtLeast() throws Exception {
        // given
        DelayedExecution delayedExecution = new DelayedExecution(executor, mock, 20, MILLISECONDS);
        delayedExecution.recordAsyncCall('1');

        // when
        delayedExecution.allAsyncCallsStarted();

        // then
        verify(mock, after(100).atLeast(1)).oneArg('1');
    }

    @Test
    public void shouldFailVerificationWithWrongTimes() throws Exception {
        // given
        DelayedExecution delayedExecution = new DelayedExecution(executor, mock, 20, MILLISECONDS);
        delayedExecution.recordAsyncCall('1');

        // when
        delayedExecution.allAsyncCallsStarted();

        // then
        verify(mock, times(0)).oneArg('1');
        exception.expect(MockitoAssertionError.class);
        verify(mock, after(100).times(2)).oneArg('1');
    }

    @Test
    public void shouldWaitTheFullTimeIfTheTestCouldPass() throws Exception {
        // given
        DelayedExecution delayedExecution = new DelayedExecution(executor, mock, 20, MILLISECONDS);
        delayedExecution.recordAsyncCall('1');

        // when
        delayedExecution.allAsyncCallsStarted();

        // then
        stopWatch.start();

        try {
            verify(mock, after(100).atLeast(2)).oneArg('1');
            Assert.fail("Expected behavior was to throw an exception, and never reach this line");
        } catch (MockitoAssertionError ignored) {
        }

        stopWatch.assertElapsedTimeIsMoreThan(100, MILLISECONDS);
    }

    @Test
    public void shouldStopEarlyIfTestIsDefinitelyFailed() throws Exception {
        // given
        DelayedExecution delayedExecution = new DelayedExecution(executor, mock, 20, MILLISECONDS);
        delayedExecution.recordAsyncCall('1');

        // when
        delayedExecution.allAsyncCallsStarted();

        // then
        exception.expect(MockitoAssertionError.class);
        verify(mock, after(10000).never()).oneArg('1');
    }

    /**
     * Test for issue #345.
     */
    @Test
    public void shouldReturnListOfArgumentsWithSameSizeAsGivenInAtMostVerification() {
        // given
        int n = 3;

        // when
        exerciseMockNTimes(n);

        stopWatch.start();
        // then
        verify(mock, after(200).atMost(n)).oneArg((char) captor.capture());

        stopWatch.assertElapsedTimeIsMoreThan(200, MILLISECONDS);
        assertThat(captor.getAllValues()).containsExactly('0', '1', '2');
    }

    @Test
    public void shouldReturnListOfArgumentsWithSameSizeAsGivenInTimesVerification() {
        // given
        int n = 3;

        // when
        exerciseMockNTimes(n);

        //Then
        verify(mock, after(200).times(n)).oneArg((char) captor.capture());
        assertEquals(n, captor.getAllValues().size());
        assertEquals('0', (char) captor.getAllValues().get(0));
        assertEquals('1', (char) captor.getAllValues().get(1));
        assertEquals('2', (char) captor.getAllValues().get(2));
    }

    @Test
    public void shouldReturnListOfArgumentsWithSameSizeAsGivenInAtLeastVerification() {
        // given
        int n = 3;

        // when
        exerciseMockNTimes(n);

        //Then
        verify(mock, after(200).atLeast(n)).oneArg((char) captor.capture());
        assertEquals(n, captor.getAllValues().size());
        assertEquals('0', (char) captor.getAllValues().get(0));
        assertEquals('1', (char) captor.getAllValues().get(1));
        assertEquals('2', (char) captor.getAllValues().get(2));
    }

    private void exerciseMockNTimes(int n) {
        for (int i = 0; i < n; i++) {
            mock.oneArg((char) ('0' + i));
        }
    }

}