package com.amazon.jenkins.ec2fleet;

import hudson.slaves.Cloud;
import hudson.widgets.Widget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FleetStatusWidgetUpdaterTest {

    private MockedStatic<FleetStatusWidgetUpdater> mockedFleetStatusWidgetUpdater;

    @Mock
    private FleetCloud cloud1;

    @Mock
    private FleetCloud cloud2;

    @Mock
    private FleetStatusWidget widget1;

    @Mock
    private FleetStatusWidget widget2;

    private List<Widget> widgets = new ArrayList<>();

    private List<Cloud> clouds = new ArrayList<>();

    private FleetStateStats stats1 = new FleetStateStats(
            "f1", 1, new FleetStateStats.State(true, false, "a"), Collections.emptySet(), Collections.<String, Double>emptyMap());

    private FleetStateStats stats2 = new FleetStateStats(
            "f2", 1, new FleetStateStats.State(true, false, "a"), Collections.emptySet(), Collections.<String, Double>emptyMap());

    @Before
    public void before() throws Exception {
        mockedFleetStatusWidgetUpdater = Mockito.mockStatic(FleetStatusWidgetUpdater.class);
        mockedFleetStatusWidgetUpdater.when(FleetStatusWidgetUpdater::getClouds).thenReturn(clouds);
        mockedFleetStatusWidgetUpdater.when(FleetStatusWidgetUpdater::getWidgets).thenReturn(widgets);

        when(cloud1.getLabelString()).thenReturn("a");
        when(cloud2.getLabelString()).thenReturn("");
        when(cloud1.getFleet()).thenReturn("f1");
        when(cloud2.getFleet()).thenReturn("f2");

        when(cloud1.getStats()).thenReturn(stats1);
        when(cloud2.getStats()).thenReturn(stats2);
    }

    private FleetStatusWidgetUpdater getMockFleetStatusWidgetUpdater() {
        return new FleetStatusWidgetUpdater();
    }

    @After
    public void after() {
        mockedFleetStatusWidgetUpdater.close();
    }

    @Test
    public void shouldDoNothingIfNoCloudsAndWidgets() {
        getMockFleetStatusWidgetUpdater().doRun();
    }

    @Test
    public void shouldDoNothingIfNoWidgets() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        getMockFleetStatusWidgetUpdater().doRun();

        verifyNoInteractions(widget1, widget2);
    }

    @Test
    public void shouldIgnoreNonFleetClouds() {
        clouds.add(cloud1);

        Cloud nonEc2FleetCloud = mock(Cloud.class);
        clouds.add(nonEc2FleetCloud);

        widgets.add(widget2);

        getMockFleetStatusWidgetUpdater().doRun();

        verify(cloud1).getStats();
        verifyNoInteractions(nonEc2FleetCloud);
    }

    @Test
    public void shouldUpdateCloudCollectAllResultAndUpdateWidgets() {
        clouds.add(cloud1);
        clouds.add(cloud2);

        widgets.add(widget1);

        getMockFleetStatusWidgetUpdater().doRun();

        verify(widget1).setStatusList(Arrays.asList(
                new FleetStatusInfo(cloud1.getFleet(), stats1.getState().getDetailed(), cloud1.getLabelString(), stats1.getNumActive(), stats1.getNumDesired()),
                new FleetStatusInfo(cloud2.getFleet(), stats2.getState().getDetailed(), cloud2.getLabelString(), stats2.getNumActive(), stats2.getNumDesired())
        ));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldIgnoreNonEc2FleetWidgets() {
        clouds.add(cloud1);

        Widget nonEc2FleetWidget = mock(Widget.class);
        widgets.add(nonEc2FleetWidget);

        widgets.add(widget1);

        getMockFleetStatusWidgetUpdater().doRun();

        verify(widget1).setStatusList(any(List.class));
        verifyNoInteractions(nonEc2FleetWidget);
    }

}
