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

import com.graphhopper.util.Helper
import com.graphhopper.util.NumHelper

import java.util.ArrayList

/**
 * A simple bounding box defined as follows: minLon, maxLon followed by minLat which is south(!) and
 * maxLat. Equally to EX_GeographicBoundingBox in the ISO 19115 standard see
 * http://osgeo-org.1560.n6.nabble.com/Boundingbox-issue-for-discussion-td3875533.html
 *
 *
 * Nice German overview:
 * http://www.geoinf.uni-jena.de/fileadmin/Geoinformatik/Lehre/Diplomarbeiten/DA_Andres.pdf
 *
 *
 *
 * @author Peter Karich
 */
open class BBox @JvmOverloads constructor(
    // longitude (theta) = x, latitude (phi) = y, elevation = z
    @JvmField var minLon: Double,
    @JvmField var maxLon: Double,
    @JvmField var minLat: Double,
    @JvmField var maxLat: Double,
    @JvmField var minEle: Double,
    @JvmField var maxEle: Double,
    private val elevation: Boolean = true) : Shape, Cloneable {

    // second longitude should be bigger than the first
    // second latitude should be smaller than the first
    // equal elevation is okay
    val isValid: Boolean
        get() {
            if (minLon >= maxLon)
                return false
            if (minLat >= maxLat)
                return false

            if (elevation) {
                if (minEle > maxEle)
                    return false

                if (java.lang.Double.compare(maxEle, -java.lang.Double.MAX_VALUE) == 0 || java.lang.Double.compare(minEle, java.lang.Double.MAX_VALUE) == 0)
                    return false
            }

            return (java.lang.Double.compare(maxLat, -java.lang.Double.MAX_VALUE) != 0
                && java.lang.Double.compare(minLat, java.lang.Double.MAX_VALUE) != 0
                && java.lang.Double.compare(maxLon, -java.lang.Double.MAX_VALUE) != 0
                && java.lang.Double.compare(minLon, java.lang.Double.MAX_VALUE) != 0)
        }

    constructor(coords: DoubleArray) : this(coords[0], coords[2], coords[1], coords[3]) {}

    constructor(minLon: Double, maxLon: Double, minLat: Double, maxLat: Double) : this(minLon, maxLon, minLat, maxLat, java.lang.Double.NaN, java.lang.Double.NaN, false) {}

    fun hasElevation(): Boolean {
        return elevation
    }

    fun update(lat: Double, lon: Double) {
        if (lat > maxLat) {
            maxLat = lat
        }

        if (lat < minLat) {
            minLat = lat
        }

        if (lon > maxLon) {
            maxLon = lon
        }
        if (lon < minLon) {
            minLon = lon
        }
    }

    fun update(lat: Double, lon: Double, elev: Double) {
        if (elevation) {
            if (elev > maxEle) {
                maxEle = elev
            }
            if (elev < minEle) {
                minEle = elev
            }
        }
        else {
            throw IllegalStateException("No BBox with elevation to update")
        }
        update(lat, lon)

    }

    /**
     * Calculates the intersecting BBox between this and the specified BBox
     *
     * @return the intersecting BBox or null if not intersecting
     */
    fun calculateIntersection(bBox: BBox): BBox? {
        if (!this.intersect(bBox))
            return null

        val minLon = Math.max(this.minLon, bBox.minLon)
        val maxLon = Math.min(this.maxLon, bBox.maxLon)
        val minLat = Math.max(this.minLat, bBox.minLat)
        val maxLat = Math.min(this.maxLat, bBox.maxLat)

        return BBox(minLon, maxLon, minLat, maxLat)
    }

    public override fun clone(): BBox {
        return BBox(minLon, maxLon, minLat, maxLat, minEle, maxEle, elevation)
    }

    override fun intersect(s: Shape): Boolean {
        if (s is BBox) {
            return intersect(s)
        }
        else if (s is Circle) {
            return s.intersect(this)
        }

        throw UnsupportedOperationException("unsupported shape")
    }

    override fun contains(s: Shape): Boolean {
        if (s is BBox) {
            return contains(s)
        }
        else if (s is Circle) {
            return contains(s)
        }

        throw UnsupportedOperationException("unsupported shape")
    }

    open fun intersect(s: Circle): Boolean {
        return s.intersect(this)
    }

    open fun intersect(o: BBox): Boolean {
        // return (o.minLon < minLon && o.maxLon > minLon || o.minLon < maxLon && o.minLon >= minLon)
        //  && (o.maxLat < maxLat && o.maxLat >= minLat || o.maxLat >= maxLat && o.minLat < maxLat);
        return minLon < o.maxLon && minLat < o.maxLat && o.minLon < maxLon && o.minLat < maxLat
    }

    override fun contains(lat: Double, lon: Double): Boolean {
        return lat <= maxLat && lat >= minLat && lon <= maxLon && lon >= minLon
    }

    operator fun contains(b: BBox): Boolean {
        return maxLat >= b.maxLat && minLat <= b.minLat && maxLon >= b.maxLon && minLon <= b.minLon
    }

    operator fun contains(c: Circle): Boolean {
        return contains(c.bounds)
    }

    override fun toString(): String {
        var str = minLon.toString() + "," + maxLon + "," + minLat + "," + maxLat
        if (elevation)
            str += ",$minEle,$maxEle"

        return str
    }

    fun toLessPrecisionString(): String {
        return minLon.toFloat().toString() + "," + maxLon.toFloat() + "," + minLat.toFloat() + "," + maxLat.toFloat()
    }

    override fun getBounds(): BBox {
        return this
    }

    override fun getCenter(): GHPoint {
        return GHPoint((maxLat + minLat) / 2, (maxLon + minLon) / 2)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        val b = other as BBox
        // equals within a very small range
        return (NumHelper.equalsEps(minLat, b.minLat) && NumHelper.equalsEps(maxLat, b.maxLat)
            && NumHelper.equalsEps(minLon, b.minLon) && NumHelper.equalsEps(maxLon, b.maxLon))
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 17 * hash + (java.lang.Double.doubleToLongBits(this.minLon) xor java.lang.Double.doubleToLongBits(this.minLon).ushr(32)).toInt()
        hash = 17 * hash + (java.lang.Double.doubleToLongBits(this.maxLon) xor java.lang.Double.doubleToLongBits(this.maxLon).ushr(32)).toInt()
        hash = 17 * hash + (java.lang.Double.doubleToLongBits(this.minLat) xor java.lang.Double.doubleToLongBits(this.minLat).ushr(32)).toInt()
        hash = 17 * hash + (java.lang.Double.doubleToLongBits(this.maxLat) xor java.lang.Double.doubleToLongBits(this.maxLat).ushr(32)).toInt()
        return hash
    }

    /**
     * @return array containing this bounding box. Attention: GeoJson is lon,lat! If 3D is gets even
     * worse: lon,lat,ele
     */
    fun toGeoJson(): List<Double> {
        val list = ArrayList<Double>(4)
        list.add(Helper.round6(minLon))
        list.add(Helper.round6(minLat))
        // hmh
        if (elevation)
            list.add(Helper.round2(minEle))

        list.add(Helper.round6(maxLon))
        list.add(Helper.round6(maxLat))
        if (elevation)
            list.add(Helper.round2(maxEle))

        return list
    }

    /**
     * @return an estimated area in m^2 using the mean value of latitudes for longitude distance
     */
    override fun calculateArea(): Double {
        val meanLat = (maxLat + minLat) / 2
        return (Helper.DIST_PLANE.calcDist(meanLat, minLon, meanLat, maxLon)
            // left side should be equal to right side no mean value necessary
            * Helper.DIST_PLANE.calcDist(minLat, minLon, maxLat, minLon))
    }

    companion object {

        /**
         * Prefills BBox with minimum values so that it can increase.
         */
        @JvmStatic
        fun createInverse(elevation: Boolean): BBox {
            return if (elevation) {
                BBox(java.lang.Double.MAX_VALUE, -java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE, -java.lang.Double.MAX_VALUE,
                    java.lang.Double.MAX_VALUE, -java.lang.Double.MAX_VALUE, true)
            }
            else {
                BBox(java.lang.Double.MAX_VALUE, -java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE, -java.lang.Double.MAX_VALUE,
                    java.lang.Double.NaN, java.lang.Double.NaN, false)
            }
        }

        /**
         * This method creates a BBox out of a string in format lat1,lon1,lat2,lon2
         */
        @JvmStatic
        fun parseTwoPoints(objectAsString: String): BBox {
            val splittedObject = objectAsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (splittedObject.size != 4)
                throw IllegalArgumentException("BBox should have 4 parts but was $objectAsString")

            var minLat = java.lang.Double.parseDouble(splittedObject[0])
            var minLon = java.lang.Double.parseDouble(splittedObject[1])

            var maxLat = java.lang.Double.parseDouble(splittedObject[2])
            var maxLon = java.lang.Double.parseDouble(splittedObject[3])

            if (minLat > maxLat) {
                val tmp = minLat
                minLat = maxLat
                maxLat = tmp
            }

            if (minLon > maxLon) {
                val tmp = minLon
                minLon = maxLon
                maxLon = tmp
            }

            return BBox(minLon, maxLon, minLat, maxLat)
        }

        /**
         * This method creates a BBox out of a string in format lon1,lon2,lat1,lat2
         */
        @JvmStatic
        fun parseBBoxString(objectAsString: String): BBox {
            val splittedObject = objectAsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (splittedObject.size != 4)
                throw IllegalArgumentException("BBox should have 4 parts but was $objectAsString")

            val minLon = java.lang.Double.parseDouble(splittedObject[0])
            val maxLon = java.lang.Double.parseDouble(splittedObject[1])

            val minLat = java.lang.Double.parseDouble(splittedObject[2])
            val maxLat = java.lang.Double.parseDouble(splittedObject[3])

            return BBox(minLon, maxLon, minLat, maxLat)
        }
    }

}
