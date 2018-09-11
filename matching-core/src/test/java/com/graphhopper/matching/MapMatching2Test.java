/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper GmbH licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.matching;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.graphhopper.gpx.Gpx;
import com.graphhopper.matching.MapMatchingTest.TestGraphHopper;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.index.QueryResult;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static com.graphhopper.matching.MapMatchingTest.fetchStreets;
import static org.junit.Assert.*;

/**
 * @author Peter Karich
 */
public class MapMatching2Test {

    private XmlMapper xmlMapper = new XmlMapper();

    @Test
    public void testIssue13() throws IOException {
        CarFlagEncoder encoder = new CarFlagEncoder();
        TestGraphHopper hopper = new TestGraphHopper();
        hopper.setDataReaderFile("../map-data/map-issue13.osm.gz");
        hopper.setGraphHopperLocation("../target/mapmatchingtest-13");
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.importOrLoad();

        AlgorithmOptions opts = AlgorithmOptions.start().build();
        MapMatching mapMatching = new MapMatching(hopper, opts);

        Gpx gpx = xmlMapper.readValue(getClass().getResourceAsStream("/issue-13.gpx"), Gpx.class);
        MatchResult mr = mapMatching.doWork(gpx.trk.getEntries());

        // make sure no virtual edges are returned
        int edgeCount = hopper.getGraphHopperStorage().getAllEdges().length();
        for (EdgeMatch em : mr.getEdgeMatches()) {
            assertTrue("result contains virtual edges:" + em.getEdgeState().toString(),
                    em.getEdgeState().getEdge() < edgeCount);
            validateEdgeMatch(em);
        }

        // create street names
        assertEquals(Arrays.asList("", "", "", "", "", ""),
                fetchStreets(mr.getEdgeMatches()));
        assertEquals(mr.getGpxEntriesLength(), mr.getMatchLength(), 2.5);
        assertEquals(28790, mr.getMatchMillis(), 50);
    }
    
    @Test
    public void testIssue70() throws IOException {
        CarFlagEncoder encoder = new CarFlagEncoder();
        TestGraphHopper hopper = new TestGraphHopper();
        hopper.setDataReaderFile("../map-data/issue-70.osm.gz");
        hopper.setGraphHopperLocation("../target/mapmatchingtest-70");
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.importOrLoad();

        AlgorithmOptions opts = AlgorithmOptions.start().build();
        MapMatching mapMatching = new MapMatching(hopper, opts);

        Gpx gpx = xmlMapper.readValue(getClass().getResourceAsStream("/issue-70.gpx"), Gpx.class);
        MatchResult mr = mapMatching.doWork(gpx.trk.getEntries());
        
        assertEquals(Arrays.asList("Милана Видака", "Милана Видака", "Милана Видака",
        		"Бранка Радичевића", "Бранка Радичевића", "Здравка Челара"),
                fetchStreets(mr.getEdgeMatches()));
        // TODO: length/time

        for (EdgeMatch edgeMatch : mr.getEdgeMatches()) {
            validateEdgeMatch(edgeMatch);
        }
    }

    private void validateEdgeMatch(EdgeMatch edgeMatch) {
        for (GPXExtension gpxExtension : edgeMatch.getGpxExtensions()) {
            if (gpxExtension.getQueryResult().getSnappedPosition() == QueryResult.Position.TOWER) {
                if (gpxExtension.getQueryResult().getClosestNode() != edgeMatch.getEdgeState().getAdjNode()
                        && gpxExtension.getQueryResult().getClosestNode() != edgeMatch.getEdgeState().getAdjNode()) {
                    fail();
                }
            } else {
                if (gpxExtension.getQueryResult().getClosestEdge().getEdge() != edgeMatch.getEdgeState().getEdge()) {
                    fail();
                }
            }
        }
    }

}
