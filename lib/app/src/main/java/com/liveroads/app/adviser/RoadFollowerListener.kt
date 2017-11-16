package com.liveroads.app.adviser

interface RoadFollowerListener {
    fun onNextTurnInfoChanged(nt: UserRoadFollower.NextTurnInfo)
    fun onRouteInfoChanged(ri: UserRoadFollower.RouteInfo)
}
