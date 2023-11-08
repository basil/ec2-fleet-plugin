package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloudNannyTest {

    private MockedStatic<CloudNanny> mockedCloudNanny;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private FleetCloud cloud1;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private FleetCloud cloud2;

    private List<Cloud> clouds = new ArrayList<>();

    private FleetStateStats stats1 = new FleetStateStats(
            "f1", 1, new FleetStateStats.State(true, false, "a"), Collections.emptySet(), Collections.<String, Double>emptyMap());

    private FleetStateStats stats2 = new FleetStateStats(
            "f2", 1, new FleetStateStats.State(true, false, "a"), Collections.emptySet(), Collections.<String, Double>emptyMap());

    private int recurrencePeriod = 45;

    private AtomicInteger recurrenceCounter1 = new AtomicInteger();
    private AtomicInteger recurrenceCounter2 = new AtomicInteger();

    private Map<FleetCloud, AtomicInteger> recurrenceCounters = Collections.synchronizedMap(new WeakHashMap<>());

    @Before
    public void before() throws Exception {
        mockedCloudNanny = Mockito.mockStatic(CloudNanny.class);
        mockedCloudNanny.when(CloudNanny::getClouds).thenReturn(clouds);

        when(cloud1.getLabelString()).thenReturn("a");
        when(cloud2.getLabelString()).thenReturn("");
        when(cloud1.getFleet()).thenReturn("f1");
        when(cloud2.getFleet()).thenReturn("f2");

        when(cloud1.update()).thenReturn(stats1);
        when(cloud2.update()).thenReturn(stats2);

        when(cloud1.getCloudStatusIntervalSec()).thenReturn(recurrencePeriod);
        when(cloud2.getCloudStatusIntervalSec()).thenReturn(recurrencePeriod * 2);

        recurrenceCounters.put(cloud1, recurrenceCounter1);
        recurrenceCounters.put(cloud2, recurrenceCounter2);
    }

    @After
    public void after() {
        mockedCloudNanny.close();
    }

    private CloudNanny getMockCloudNannyInstance() {
        CloudNanny cloudNanny = new CloudNanny();

        // next execution should trigger running the status check.
        recurrenceCounter1.set(1);
        recurrenceCounter2.set(1);

        setInternalState(cloudNanny, "recurrenceCounters", recurrenceCounters);

        return cloudNanny;
    }

    private static void setInternalState(Object obj, String fieldName, Object newValue) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, newValue);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void shouldDoNothingIfNoCloudsAndWidgets() {
        getMockCloudNannyInstance().doRun();
    }

    @Test
    public void shouldUpdateCloudAndDoNothingIfNoWidgets() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        getMockCloudNannyInstance().doRun();
    }

    @Test
    public void shouldIgnoreNonFleetClouds() {
        clouds.add(cloud1);

        Cloud nonEc2FleetCloud = mock(Cloud.class);
        clouds.add(nonEc2FleetCloud);

        getMockCloudNannyInstance().doRun();

        verify(cloud1).update();
        verifyNoInteractions(nonEc2FleetCloud);
    }

    @Test
    public void shouldUpdateCloudCollectAll() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        getMockCloudNannyInstance().doRun();

        verify(cloud1).update();
        verify(cloud2).update();
    }

    @Test
    public void shouldIgnoreExceptionsFromUpdateForOneofCloudAndUpdateOther() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        when(cloud1.update()).thenThrow(new IllegalArgumentException("test"));

        getMockCloudNannyInstance().doRun();

        verify(cloud1).update();
        verify(cloud2).update();
    }

    @Test
    public void resetCloudInterval() {
        clouds.add(cloud1);
        clouds.add(cloud2);
        CloudNanny cloudNanny = getMockCloudNannyInstance();

        cloudNanny.doRun();

        verify(cloud1).update();
        verify(cloud1, atLeastOnce()).getCloudStatusIntervalSec();
        verify(cloud2).update();
        verify(cloud2, atLeastOnce()).getCloudStatusIntervalSec();


        assertEquals(cloud1.getCloudStatusIntervalSec(), recurrenceCounter1.get());
        assertEquals(cloud2.getCloudStatusIntervalSec(), recurrenceCounter2.get());
    }

    @Test
    public void skipCloudIntervalExecution() {
        clouds.add(cloud1);
        clouds.add(cloud2);
        CloudNanny cloudNanny = getMockCloudNannyInstance();
        recurrenceCounter1.set(2);
        recurrenceCounter2.set(3);

        cloudNanny.doRun();

        verify(cloud1, atLeastOnce()).getCloudStatusIntervalSec();
        verify(cloud2, atLeastOnce()).getCloudStatusIntervalSec();
        verifyNoMoreInteractions(cloud1, cloud2);

        assertEquals(1, recurrenceCounter1.get());
        assertEquals(2, recurrenceCounter2.get());
    }

    @Test
    public void updateOnlyOneCloud() {
        clouds.add(cloud1);
        clouds.add(cloud2);
        CloudNanny cloudNanny = getMockCloudNannyInstance();
        recurrenceCounter1.set(2);
        recurrenceCounter2.set(1);

        cloudNanny.doRun();

        verify(cloud2, atLeastOnce()).getCloudStatusIntervalSec();
        verify(cloud2).update();

        verify(cloud1, atLeastOnce()).getCloudStatusIntervalSec();
        verifyNoMoreInteractions(cloud1);

        assertEquals(1, recurrenceCounter1.get());
        assertEquals(cloud2.getCloudStatusIntervalSec(), recurrenceCounter2.get());
    }
}
