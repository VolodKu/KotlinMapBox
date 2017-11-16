package com.liveroads.app

class NavigationRoadVisibilityOperator(
        private val m: NavigationMapRoute,
        private val hdMode: ZoomTilt
) {

    fun onZoomChanged(zoom: Double) {
        if (zoom.isNaN()) { return }

        if (zoom >= hdMode.zoom) {
            hideRoad()
            return
        }

        if (zoom <= hdMode.zoom) {
            showRoad()
        }
    }

    private fun hideRoad() {
        m.hideRoute()
    }

    private fun showRoad() {
        m.showRoute()
    }

}
