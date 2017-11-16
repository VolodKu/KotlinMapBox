package com.liveroads.app;

import android.content.res.TypedArray;
import android.location.Location;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;

import com.liveroads.app.adviser.UserRoadFollower;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.functions.Function;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.v5.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteStepProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.StepManeuver;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.functions.stops.Stop.stop;
import static com.mapbox.mapboxsdk.style.functions.stops.Stops.exponential;

/**
 * Provide a route using {@link NavigationMapRoute#addRoute(DirectionsRoute)} and a route will be drawn using runtime
 * styling. The route will automatically be placed below all labels independent of specific style. If the map styles
 * changed when a routes drawn on the map, the route will automatically be redrawn onto the new map style. If during
 * a navigation session, the user gets re-routed, the route line will be redrawn to reflect the new geometry. To remove
 * the route from the map, use {@link NavigationMapRoute#hideRoute()}.
 * <p>
 * <i>NOTE</i>: based on <a href="https://github.com/mapbox/mapbox-navigation-android/blob/master/navigation/libandroid-navigation-ui/src/main/java/com/mapbox/services/android/navigation/ui/v5/NavigationMapRoute.java">NavigationMapRoute.java</a>
 */
public class NavigationMapRoute implements ProgressChangeListener, MapView.OnMapChangedListener {

    @StyleRes
    private int styleRes;
    @ColorInt
    private int routeDefaultColor;

    private List<String> layerIds;
    private final MapView mapView;
    private final MapboxMap mapboxMap;
    private final MapboxNavigation navigation;
    private DirectionsRoute route;
    private boolean visible;

    /**
     * Construct an instance of {@link NavigationMapRoute}.
     *
     * @param navigation an instance of the {@link MapboxNavigation} object. Passing in null means your route won't
     *                   consider rerouting during a navigation session.
     * @param mapView    the MapView to apply the route to
     * @param mapboxMap  the MapboxMap to apply route with
     */
    public NavigationMapRoute(
            @Nullable MapboxNavigation navigation,
            @NonNull MapView mapView,
            @NonNull MapboxMap mapboxMap)
    {
        this.styleRes = R.style.NavigationMapRoute;
        this.mapView = mapView;
        this.mapboxMap = mapboxMap;
        this.navigation = navigation;
        addListeners();
        initialize();
    }

    /**
     * Adds source and layers to the map.
     */
    private void initialize() {
        layerIds = new ArrayList<>();

        addSource(route);

        TypedArray typedArray = mapView.getContext().obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute);

        routeDefaultColor = typedArray.getColor(R.styleable.NavigationMapRoute_routeColor,
                ContextCompat.getColor(mapView.getContext(), R.color.mapbox_navigation_route_layer_blue));

        addNavigationRouteLayer(1.0f);
        typedArray.recycle();
    }

    /**
     * Adds the necessary listeners
     */
    private void addListeners() {
        if (navigation != null) {
            navigation.addProgressChangeListener(this);
        }
        mapView.addOnMapChangedListener(this);
    }

    /**
     * Pass in a {@link DirectionsRoute} and display the route geometry on your map.
     *
     * @param route a {@link DirectionsRoute} used to draw the route line
     * @since 0.4.0
     */
    public void addRoute(@NonNull DirectionsRoute route) {
        this.route = route;
        addSource(route);

        UserRoadFollower.INSTANCE.updateRouteDistance(Math.round(route.getDistance()));

        setLayerVisibility(true);
        if (navigation != null) {
            navigation.startNavigation(route);
        }
    }

    public void showRoute() {
        setLayerVisibility(true);
    }

    public void hideRoute() {
        setLayerVisibility(false);
    }

    /**
     * Get the current route being used to draw the route, if one hasn't been added to the map yet, this will return
     * {@code null}
     *
     * @return the {@link DirectionsRoute} used to draw the route line
     * @since 0.4.0
     */
    public DirectionsRoute getRoute() {
        return route;
    }

    /**
     * Called when a map change events occurs. Used specifically to detect loading of a new style, if applicable reapply
     * the route line source and layers.
     *
     * @param change the map change event that occurred
     * @since 0.4.0
     */
    @Override
    public void onMapChanged(int change) {
        if (change == MapView.DID_FINISH_LOADING_STYLE) {
            initialize();
            setLayerVisibility(visible);
        }
    }

    /**
     * Called when the user makes new progress during a navigation session. Used to determine whether or not a re-route
     * has occurred and if so the route is redrawn to reflect the change.
     *
     * @param location      the users current location
     * @param routeProgress a {@link RouteProgress} reflecting the users latest progress along the route
     * @since 0.4.0
     */
    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        // TODO they'll probably never be equal till https://github.com/mapbox/mapbox-java/issues/440 gets resolved
        // Check if the route's the same as the route currently drawn
        if (!routeProgress.route().equals(route)) {
            route = routeProgress.route();
            addSource(route);
        }
        proccessRouteProgress(location, routeProgress);
    }

    private void proccessRouteProgress(Location location, RouteProgress routeProgress) {
        int curLeg = routeProgress.getLegIndex();
        RouteLegProgress curLegProgress = routeProgress.getCurrentLegProgress();
        RouteStepProgress curStepProgress = curLegProgress.getCurrentStepProgress();

        int curStepProgressIndex = curLegProgress.stepIndex();
        boolean isLastStepProgress = curStepProgressIndex == curLegProgress.routeLeg().getSteps().size()-1;
        if (isLastStepProgress) {
            UserRoadFollower.INSTANCE.updateNextTurnCoordinates(
                    0.0,
                    0.0,
                    null);
        } else {
            StepManeuver nextManeuver = curLegProgress.routeLeg()
                    .getSteps().get(curStepProgressIndex+1)
                    .getManeuver();
            Position curManeuverPosition = Position
                    .fromCoordinates(nextManeuver.getLocation()[0], nextManeuver.getLocation()[1]);
            String nextManeuverModifier = nextManeuver.getModifier();

            UserRoadFollower.INSTANCE.updateNextTurnCoordinates(
                    curManeuverPosition.getLongitude(),
                    curManeuverPosition.getLatitude(),
                    nextManeuverModifier);
        }
    }

    /**
     * Toggle whether or not the route lines visible or not, used in {@link NavigationMapRoute#addRoute(DirectionsRoute)}
     * and {@link NavigationMapRoute#hideRoute()}.
     *
     * @param visible true if you want the route to be visible, else false
     */
    private void setLayerVisibility(boolean visible) {
        if (this.visible == visible) { return; }

        this.visible = visible;
        List<Layer> layers = mapboxMap.getLayers();
        String id;

        for (Layer layer : layers) {
            id = layer.getId();
            if (layerIds.contains(layer.getId())) {
                if (id.equals(NavigationMapLayers.NAVIGATION_ROUTE_LAYER_ID)) {
                    layer.setProperties(PropertyFactory.visibility(visible ? Property.VISIBLE : Property.NONE));
                }
            }
        }
    }

    /**
     * Adds the route source to the map.
     */
    private void addSource(@Nullable DirectionsRoute route) {
        FeatureCollection routeLineFeature;
        // Either add an empty GeoJson featureCollection or the route's Geometry
        if (route == null) {
            routeLineFeature = FeatureCollection.fromFeatures(new Feature[] {});
        } else {
            routeLineFeature = addTrafficToSource(route);
        }

        // Determine whether the source needs to be added or updated
        GeoJsonSource source = mapboxMap.getSourceAs(NavigationMapSources.NAVIGATION_ROUTE_SOURCE_ID);
        if (source == null) {
            GeoJsonSource routeSource = new GeoJsonSource(NavigationMapSources.NAVIGATION_ROUTE_SOURCE_ID, routeLineFeature);
            mapboxMap.addSource(routeSource);
        } else {
            source.setGeoJson(routeLineFeature);
        }
    }

    /**
     * Generic method for adding layers to the map.
     */
    private void addLayerToMap(@NonNull Layer layer, @Nullable String idBelowLayer) {
        if (idBelowLayer == null) {
            mapboxMap.addLayer(layer);
        } else {
            mapboxMap.addLayerBelow(layer, idBelowLayer);
        }
        layerIds.add(layer.getId());
    }

    /**
     * If the {@link DirectionsRoute} request contains congestion information via annotations, breakup the source into
     * pieces so data-driven styling can be used to change the route colors accordingly.
     */
    private FeatureCollection addTrafficToSource(DirectionsRoute route) {
        List<Feature> features = new ArrayList<>();
        LineString originalGeometry = LineString.fromPolyline(route.getGeometry(), Constants.PRECISION_6);
        features.add(Feature.fromGeometry(originalGeometry));

        LineString lineString = LineString.fromPolyline(route.getGeometry(), Constants.PRECISION_6);
        Feature feature = Feature.fromGeometry(lineString);
        features.add(feature);

        return FeatureCollection.fromFeatures(features);
    }

    /**
     * Add the route layer to the map either using the custom style values or the default.
     */
    private void addNavigationRouteLayer(float scale) {
        Layer routeLayer = new LineLayer(NavigationMapLayers.NAVIGATION_ROUTE_LAYER_ID,
                NavigationMapSources.NAVIGATION_ROUTE_SOURCE_ID).withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_SQUARE),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.visibility(Property.NONE),
                PropertyFactory.lineWidth(Function.zoom(
                        exponential(
                                stop(4f, PropertyFactory.lineWidth(2f * scale)),
                                stop(10f, PropertyFactory.lineWidth(3f * scale)),
                                stop(13f, PropertyFactory.lineWidth(4f * scale)),
                                stop(16f, PropertyFactory.lineWidth(7f * scale)),
                                stop(19f, PropertyFactory.lineWidth(14f * scale)),
                                stop(22f, PropertyFactory.lineWidth(18f * scale))
                        ).withBase(1.5f))
                ),
                PropertyFactory.lineColor(routeDefaultColor));
        addLayerToMap(routeLayer, null);
    }

    /**
     * Layer id constants.
     */
    static class NavigationMapLayers {
        static final String NAVIGATION_ROUTE_LAYER_ID = "mapbox-plugin-navigation-route-layer";
    }

    /**
     * Source id constants.
     */
    static class NavigationMapSources {
        static final String NAVIGATION_ROUTE_SOURCE_ID = "mapbox-plugin-navigation-route-source";
    }
}