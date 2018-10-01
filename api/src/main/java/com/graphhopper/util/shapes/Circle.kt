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
package com.graphhopper.util.shapes

import com.graphhopper.util.DistanceCalc
import com.graphhopper.util.Helper
import com.graphhopper.util.NumHelper

/**
 * @author Peter Karich
 */
open class Circle
    @JvmOverloads constructor(
        val lat: Double,
        val lon: Double,
        private val radiusInMeter: Double,
        private val calc: DistanceCalc = Helper.DIST_EARTH
) : Shape {
    private val normedDist = calc.calcNormalizedDist(radiusInMeter)
    private val bbox = calc.createBBox(lat, lon, radiusInMeter)
    
    override fun contains(lat1: Double, lon1: Double): Boolean {
        return normDist(lat1, lon1) <= normedDist
    }

    override fun getBounds(): BBox {
        return bbox
    }

    override fun getCenter(): GHPoint {
        return GHPoint(lat, lon)
    }

    private fun normDist(lat1: Double, lon1: Double): Double {
        return calc.calcNormalizedDist(lat, lon, lat1, lon1)
    }

    override fun intersect(o: Shape): Boolean {
        if (o is Circle) {
            return intersect(o)
        }
        else if (o is BBox) {
            return intersect(o)
        }

        return o.intersect(this)
    }

    override fun contains(o: Shape): Boolean {
        if (o is Circle) {
            return contains(o)
        }
        else if (o is BBox) {
            return contains(o)
        }

        throw UnsupportedOperationException("unsupported shape")
    }

    open fun intersect(b: BBox): Boolean {
        // test top intersect
        if (lat > b.maxLat) {
            if (lon < b.minLon) {
                return normDist(b.maxLat, b.minLon) <= normedDist
            }
            return if (lon > b.maxLon) {
                normDist(b.maxLat, b.maxLon) <= normedDist
            }
            else b.maxLat - bbox.minLat > 0
        }

        // test bottom intersect
        if (lat < b.minLat) {
            if (lon < b.minLon) {
                return normDist(b.minLat, b.minLon) <= normedDist
            }
            return if (lon > b.maxLon) {
                normDist(b.minLat, b.maxLon) <= normedDist
            }
            else bbox.maxLat - b.minLat > 0
        }

        // test middle intersect
        if (lon < b.minLon) {
            return bbox.maxLon - b.minLon > 0
        }
        return if (lon > b.maxLon) {
            b.maxLon - bbox.minLon > 0
        }
        else true
    }

    open fun intersect(c: Circle): Boolean {
        // necessary to improve speed?
        return if (!bounds.intersect(c.bounds)) {
            false
        }
        else normDist(c.lat, c.lon) <= calc.calcNormalizedDist(radiusInMeter + c.radiusInMeter)

    }

    operator fun contains(b: BBox): Boolean {
        return if (bbox.contains(b)) {
            (contains(b.maxLat, b.minLon) && contains(b.minLat, b.minLon)
                && contains(b.maxLat, b.maxLon) && contains(b.minLat, b.maxLon))
        }
        else false

    }

    operator fun contains(c: Circle): Boolean {
        val res = radiusInMeter - c.radiusInMeter
        return if (res < 0) {
            false
        }
        else calc.calcDist(lat, lon, c.lat, c.lon) <= res

    }

    override fun calculateArea(): Double {
        return Math.PI * radiusInMeter * radiusInMeter
    }

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        val b = other as Circle
        // equals within a very small range
        return NumHelper.equalsEps(lat, b.lat) && NumHelper.equalsEps(lon, b.lon) && NumHelper.equalsEps(radiusInMeter, b.radiusInMeter)
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 17 * hash + (java.lang.Double.doubleToLongBits(this.lat) xor java.lang.Double.doubleToLongBits(this.lat).ushr(32)).toInt()
        hash = 17 * hash + (java.lang.Double.doubleToLongBits(this.lon) xor java.lang.Double.doubleToLongBits(this.lon).ushr(32)).toInt()
        hash = 17 * hash + (java.lang.Double.doubleToLongBits(this.radiusInMeter) xor java.lang.Double.doubleToLongBits(this.radiusInMeter).ushr(32)).toInt()
        return hash
    }

    override fun toString(): String {
        return lat.toString() + "," + lon + ", radius:" + radiusInMeter
    }
}
