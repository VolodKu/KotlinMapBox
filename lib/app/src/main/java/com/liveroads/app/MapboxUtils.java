package com.liveroads.app;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.commons.models.Position;

public class MapboxUtils {

    public static MapboxDirections createDirections(Position origin, Position destination) {
        MapboxDirections directions = new MapboxDirections.Builder()
                .setOrigin(origin)
                .setDestination(destination)
                .setAccessToken(Mapbox.getAccessToken())
                .setProfile(DirectionsCriteria.PROFILE_DRIVING)
                .setOverview(DirectionsCriteria.OVERVIEW_FULL)
                .setAnnotation(DirectionsCriteria.ANNOTATION_DISTANCE)
                .setSteps(true)
                .build();

        return directions;
    }
}
