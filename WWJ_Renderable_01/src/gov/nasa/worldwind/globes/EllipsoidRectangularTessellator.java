/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.globes;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.View;

import javax.media.opengl.GL;
import java.nio.*;
import java.util.*;

/**
 * @author tag
 * @version $Id: EllipsoidRectangularTessellator.java 3210 2007-10-06 22:14:23Z tgaskins $
 */
public class EllipsoidRectangularTessellator extends WWObjectImpl implements Tessellator
{
    // TODO: Make all this configurable
    private static final int DEFAULT_DENSITY = 24;
    private static final double DEFAULT_LOG10_RESOLUTION_TARGET = 1.3;
    private static final int DEFAULT_MAX_LEVEL = 12;
    private static final int DEFAULT_NUM_LAT_SUBDIVISIONS = 5;
    private static final int DEFAULT_NUM_LON_SUBDIVISIONS = 10;

    private static class RenderInfo
    {

        private final int density;
        private final Vec4 referenceCenter;
        private final DoubleBuffer vertices;
        private final DoubleBuffer texCoords;
        private final IntBuffer indices;
        private final int resolution;

        private RenderInfo(int density, DoubleBuffer vertices, DoubleBuffer texCoords, Vec4 refCenter, int resolution)
        {
            this.density = density;
            this.vertices = vertices;
            this.texCoords = texCoords;
            this.referenceCenter = refCenter;
            this.indices = RectTile.getIndices(this.density);
            this.resolution = resolution;
        }

        private long getSizeInBytes()
        {
            // Texture coordinates are shared among all tiles of the same density, so do not count towards size.
            // 8 references, doubles in buffer.
            return 8 * 4 + (this.vertices.limit()) * Double.SIZE;
        }
    }

    private static class CacheKey
    {

        private final Sector sector;
        private int resolution;
        private final double verticalExaggeration;
        private int density;

        private CacheKey(RectTile tile, int resolution, double verticalExaggeration, int density)
        {
            this.sector = tile.sector;
            this.resolution = resolution;
            this.verticalExaggeration = verticalExaggeration;
            this.density = density;
        }

        @Override
        public String toString()
        {
            return "density " + this.density + " ve " + this.verticalExaggeration + " resolution " + this.resolution + " sector " + this.sector;
        }

        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            CacheKey cacheKey = (CacheKey) o;

            if (density != cacheKey.density)
            {
                return false;
            }
            if (resolution != cacheKey.resolution)
            {
                return false;
            }
            if (Double.compare(cacheKey.verticalExaggeration, verticalExaggeration) != 0)
            {
                return false;
            }
            //noinspection RedundantIfStatement
            if (sector != null ? !sector.equals(cacheKey.sector) : cacheKey.sector != null)
            {
                return false;
            }

            return true;
        }

        public int hashCode()
        {
            int result;
            long temp;
            result = (sector != null ? sector.hashCode() : 0);
            result = 31 * result + resolution;
            temp = verticalExaggeration != +0.0d ? Double.doubleToLongBits(verticalExaggeration) : 0L;
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + density;
            return result;
        }
    }

    private static class RectTile implements SectorGeometry
    {

        private static final HashMap<Integer, DoubleBuffer> parameterizations = new HashMap<Integer, DoubleBuffer>();
        private static final HashMap<Integer, IntBuffer> indexLists = new HashMap<Integer, IntBuffer>();
        private final Globe globe;
        private final int level;
        private final Sector sector;
        private final Cylinder extent; // extent of triangle in object coordinates
        private final int density;
        private final double log10CellSize;
        private long byteSize;
        private RenderInfo ri;
        private PickSupport pickSupport = new PickSupport();
        private int minColorCode = 0;
        private int maxColorCode = 0;

        public RectTile(Globe globe, int level, int density, Sector sector)
        {
            this.globe = globe;
            this.level = level;
            this.density = density;
            this.sector = sector;
            this.extent = Sector.computeBoundingCylinder(globe, 1d, this.getSector());
            double cellSize = (sector.getDeltaLatRadians() * globe.getRadius()) / density;
            this.log10CellSize = Math.log10(cellSize);
        }

        public Sector getSector()
        {
            return this.sector;
        }

        public Extent getExtent()
        {
            return this.extent;
        }

        public long getSizeInBytes()
        {
            return this.byteSize;
        }

        private RectTile[] split()
        {
            Sector[] sectors = this.sector.subdivide();

            RectTile[] subTiles = new RectTile[4];
            subTiles[0] = new RectTile(this.globe, this.level + 1, this.density, sectors[0]);
            subTiles[1] = new RectTile(this.globe, this.level + 1, this.density, sectors[1]);
            subTiles[2] = new RectTile(this.globe, this.level + 1, this.density, sectors[2]);
            subTiles[3] = new RectTile(this.globe, this.level + 1, this.density, sectors[3]);

            return subTiles;
        }

        private void makeVerts(DrawContext dc)
        {
            int resolution = dc.getGlobe().getElevationModel().getTargetResolution(dc, this.sector, this.density);

            if (this.ri != null && this.ri.resolution >= resolution)
            {
                return;
            }

            MemoryCache cache = WorldWind.getMemoryCache(RectTile.class.getName());
            CacheKey cacheKey = new CacheKey(this, resolution, dc.getVerticalExaggeration(), this.density);
            this.ri = (RenderInfo) cache.getObject(cacheKey);
            if (this.ri != null)
            {
                return;
            }

            this.ri = this.buildVerts(dc, this.density, resolution, true);
            if (this.ri != null && this.ri.resolution >= 0)
            {
                cacheKey = new CacheKey(this, this.ri.resolution, dc.getVerticalExaggeration(), this.density);
                cache.add(cacheKey, this.ri, this.byteSize = this.ri.getSizeInBytes());
            }
        }

        private RenderInfo buildVerts(DrawContext dc, int density, int resolution, boolean makeSkirts)
        {
            int numVertices = (density + 3) * (density + 3);
            java.nio.DoubleBuffer verts = BufferUtil.newDoubleBuffer(numVertices * 3);

            Globe globe = dc.getGlobe();
            ElevationModel.Elevations elevations = globe.getElevationModel().getElevations(this.sector, resolution);

            double latMin = this.sector.getMinLatitude().radians;
            double latMax = this.sector.getMaxLatitude().radians;
            double dLat = (latMax - latMin) / density;

            double lonMin = this.sector.getMinLongitude().radians;
            double lonMax = this.sector.getMaxLongitude().radians;
            double dLon = (lonMax - lonMin) / density;

            int iv = 0;
            double lat = latMin;
            double verticalExaggeration = dc.getVerticalExaggeration();
            double exaggeratedMinElevation = makeSkirts ? globe.getMinElevation() * verticalExaggeration : 0;
            double equatorialRadius = globe.getEquatorialRadius();
            double eccentricity = globe.getEccentricitySquared();

            LatLon centroid = this.sector.getCentroid();
            Vec4 refCenter = globe.computePointFromPosition(centroid.getLatitude(), centroid.getLongitude(), 0d);

            for (int j = 0; j <= density + 2; j++)
            {
                double cosLat = Math.cos(lat);
                double sinLat = Math.sin(lat);
                double rpm = equatorialRadius / Math.sqrt(1.0 - eccentricity * sinLat * sinLat);
                double lon = lonMin;
                for (int i = 0; i <= density + 2; i++)
                {
                    double elevation = verticalExaggeration * elevations.getElevation(lat, lon);
                    if (j == 0 || j >= density + 2 || i == 0 || i >= density + 2)
                    {   // use abs to account for negative elevation.
                        elevation -= exaggeratedMinElevation >= 0 ? exaggeratedMinElevation : -exaggeratedMinElevation;
                    }

                    double x = ((rpm + elevation) * cosLat * Math.sin(lon)) - refCenter.x;
                    double y = ((rpm * (1.0 - eccentricity) + elevation) * sinLat) - refCenter.y;
                    double z = ((rpm + elevation) * cosLat * Math.cos(lon)) - refCenter.z;

                    verts.put(iv++, x).put(iv++, y).put(iv++, z);

                    if (i > density)
                    {
                        lon = lonMax;
                    }
                    else if (i != 0)
                    {
                        lon += dLon;
                    }
                }
                if (j > density)
                {
                    lat = latMax;
                }
                else if (j != 0)
                {
                    lat += dLat;
                }
            }

            return new RenderInfo(density, verts, getGeographicTextureCoordinates(density), refCenter,
                    elevations.getResolution());
        }

        public void renderMultiTexture(DrawContext dc, int numTextureUnits)
        {
            if (dc == null)
            {
                String msg = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            if (numTextureUnits < 1)
            {
                String msg = Logging.getMessage("generic.NumTextureUnitsLessThanOne");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            this.render(dc, numTextureUnits);
        }

        public void render(DrawContext dc)
        {
            if (dc == null)
            {
                String msg = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            this.render(dc, 1);
        }

        private long render(DrawContext dc, int numTextureUnits)
        {
            if (this.ri == null)
            {
                String msg = Logging.getMessage("nullValue.RenderInfoIsNull");
                Logging.logger().severe(msg);
                throw new IllegalStateException(msg);
            }

            dc.getView().pushReferenceCenter(dc, ri.referenceCenter);

            GL gl = dc.getGL();
            gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT);
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL.GL_DOUBLE, 0, this.ri.vertices.rewind());

            for (int i = 0; i < numTextureUnits; i++)
            {
                gl.glClientActiveTexture(GL.GL_TEXTURE0 + i);
                gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
                gl.glTexCoordPointer(2, GL.GL_DOUBLE, 0, ri.texCoords.rewind());
            }

            gl.glDrawElements(javax.media.opengl.GL.GL_TRIANGLE_STRIP, this.ri.indices.limit(),
                    javax.media.opengl.GL.GL_UNSIGNED_INT, this.ri.indices.rewind());

            gl.glPopClientAttrib();

            dc.getView().popReferenceCenter(dc);

            return this.ri.indices.limit() - 2; // return number of triangles rendered
        }

        public void renderWireframe(DrawContext dc, boolean showTriangles, boolean showTileBoundary)
        {
            if (dc == null)
            {
                String msg = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            if (this.ri == null)
            {
                String msg = Logging.getMessage("nullValue.RenderInfoIsNull");
                Logging.logger().severe(msg);
                throw new IllegalStateException(msg);
            }

            java.nio.IntBuffer indices = getIndices(this.ri.density);
            indices.rewind();

            dc.getView().pushReferenceCenter(dc, this.ri.referenceCenter);

            javax.media.opengl.GL gl = dc.getGL();
            gl.glPushAttrib(
                    GL.GL_DEPTH_BUFFER_BIT | GL.GL_POLYGON_BIT | GL.GL_TEXTURE_BIT | GL.GL_ENABLE_BIT | GL.GL_CURRENT_BIT);
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
            gl.glDisable(javax.media.opengl.GL.GL_DEPTH_TEST);
            gl.glEnable(javax.media.opengl.GL.GL_CULL_FACE);
            gl.glCullFace(javax.media.opengl.GL.GL_BACK);
            gl.glDisable(javax.media.opengl.GL.GL_TEXTURE_2D);
            gl.glColor4d(1d, 1d, 1d, 0.2);
            gl.glPolygonMode(javax.media.opengl.GL.GL_FRONT, javax.media.opengl.GL.GL_LINE);

            if (showTriangles)
            {
                gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT);
                gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

                gl.glVertexPointer(3, GL.GL_DOUBLE, 0, this.ri.vertices);
                gl.glDrawElements(javax.media.opengl.GL.GL_TRIANGLE_STRIP, indices.limit(),
                        javax.media.opengl.GL.GL_UNSIGNED_INT, indices);

                gl.glPopClientAttrib();
            }

            dc.getView().popReferenceCenter(dc);

            if (showTileBoundary)
            {
                this.renderPatchBoundary(dc, gl);
            }

            gl.glPopAttrib();
        }

        private void renderPatchBoundary(DrawContext dc, GL gl)
        {
            // TODO: Currently only works if called from renderWireframe because no state is set here.
            // TODO: Draw the boundary using the vertices along the boundary rather than just at the corners.
            gl.glColor4d(1d, 0, 0, 1d);
            Vec4[] corners = this.sector.computeCornerPoints(dc.getGlobe());

            gl.glBegin(javax.media.opengl.GL.GL_QUADS);
            gl.glVertex3d(corners[0].x, corners[0].y, corners[0].z);
            gl.glVertex3d(corners[1].x, corners[1].y, corners[1].z);
            gl.glVertex3d(corners[2].x, corners[2].y, corners[2].z);
            gl.glVertex3d(corners[3].x, corners[3].y, corners[3].z);
            gl.glEnd();
        }

        public void renderBoundingVolume(DrawContext dc)
        {
            ((Cylinder) this.getExtent()).render(dc);
        }

        public void pick(DrawContext dc, java.awt.Point pickPoint)
        {
            if (this.ri == null)
            {
                return;
            }

            renderTrianglesWithUniqueColors(dc, ri);

            int colorCode = pickSupport.getTopColor(dc, pickPoint);
            if (colorCode < minColorCode || colorCode > maxColorCode)
            {
                return;
            }

            double EPSILON = (double) 0.00001f;

            int triangleIndex = colorCode - minColorCode - 1;

            if ((null != ri.indices) && (triangleIndex < ri.indices.capacity() - 2))
            {
                double centerX = ri.referenceCenter.x;
                double centerY = ri.referenceCenter.y;
                double centerZ = ri.referenceCenter.z;

                int vIndex = 3 * ri.indices.get(triangleIndex);
                Vec4 v0 = new Vec4((ri.vertices.get(vIndex++) + centerX),
                        (ri.vertices.get(vIndex++) + centerY),
                        (ri.vertices.get(vIndex) + centerZ));

                vIndex = 3 * ri.indices.get(triangleIndex + 1);
                Vec4 v1 = new Vec4((ri.vertices.get(vIndex++) + centerX),
                        (ri.vertices.get(vIndex++) + centerY),
                        (ri.vertices.get(vIndex) + centerZ));

                vIndex = 3 * ri.indices.get(triangleIndex + 2);
                Vec4 v2 = new Vec4((ri.vertices.get(vIndex++) + centerX),
                        (ri.vertices.get(vIndex++) + centerY),
                        (ri.vertices.get(vIndex) + centerZ));

                // get triangle edge vectors and plane normal
                Vec4 e1 = v1.subtract3(v0);
                Vec4 e2 = v2.subtract3(v0);
                Vec4 N = e1.cross3(e2);  // if N is 0, the triangle is degenerate, we are not dealing with it

                Line ray = dc.getView().computeRayFromScreenPoint(pickPoint.getX(), pickPoint.getY());

                Vec4 w0 = ray.getOrigin().subtract3(v0);
                double a = -N.dot3(w0);
                double b = N.dot3(ray.getDirection());
                if (java.lang.Math.abs(b) < EPSILON) // ray is parallel to triangle plane
                {
                    return;
                }                    // if a == 0 , ray lies in triangle plane
                double r = a / b;

                Vec4 intersect = ray.getOrigin().add3(ray.getDirection().multiply3(r));
                Position pp = dc.getGlobe().computePositionFromPoint(intersect);

                // Draw the elevation from the elevation model, not the geode.
                double elev = dc.getGlobe().getElevation(pp.getLatitude(), pp.getLongitude());
                Position p = new Position(pp.getLatitude(), pp.getLongitude(), elev);

                PickedObject po = new PickedObject(colorCode, p, pp.getLatitude(), pp.getLongitude(), elev, true);
                dc.addPickedObject(po);
            }
        }

        private void renderTrianglesWithUniqueColors(DrawContext dc, RenderInfo ri)
        {
            if (dc == null)
            {
                String message = Logging.getMessage("nullValue.DrawContextIsNull");
                Logging.logger().severe(message);
                throw new IllegalStateException(message);
            }

            if (ri.vertices == null)
            {
                return;
            }

            ri.vertices.rewind();
            ri.indices.rewind();

            javax.media.opengl.GL gl = dc.getGL();

            if (null != ri.referenceCenter)
            {
                dc.getView().pushReferenceCenter(dc, ri.referenceCenter);
            }

            minColorCode = dc.getUniquePickColor().getRGB();
            int trianglesNum = ri.indices.capacity() - 2;

            gl.glBegin(GL.GL_TRIANGLES);
            for (int i = 0; i < trianglesNum; i++)
            {
                java.awt.Color color = dc.getUniquePickColor();
                gl.glColor3ub((byte) (color.getRed() & 0xFF),
                        (byte) (color.getGreen() & 0xFF),
                        (byte) (color.getBlue() & 0xFF));

                int vIndex = 3 * ri.indices.get(i);
                gl.glVertex3d(ri.vertices.get(vIndex), ri.vertices.get(vIndex + 1), ri.vertices.get(
                        vIndex + 2));

                vIndex = 3 * ri.indices.get(i + 1);
                gl.glVertex3d(ri.vertices.get(vIndex), ri.vertices.get(vIndex + 1), ri.vertices.get(
                        vIndex + 2));

                vIndex = 3 * ri.indices.get(i + 2);
                gl.glVertex3d(ri.vertices.get(vIndex), ri.vertices.get(vIndex + 1), ri.vertices.get(
                        vIndex + 2));
            }
            gl.glEnd();
            maxColorCode = dc.getUniquePickColor().getRGB();

            if (null != ri.referenceCenter)
            {
                dc.getView().popReferenceCenter(dc);
            }
        }

        public Vec4 getSurfacePoint(Angle latitude, Angle longitude, double metersOffset)
        {
            if (latitude == null || longitude == null)
            {
                String msg = Logging.getMessage("nullValue.LatLonIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            if (!this.sector.contains(latitude, longitude))
            {
                // not on this geometry
                return null;
            }

            if (this.ri == null)
            {
                return null;
            }

            double lat = latitude.getDegrees();
            double lon = longitude.getDegrees();

            double bottom = this.sector.getMinLatitude().getDegrees();
            double top = this.sector.getMaxLatitude().getDegrees();
            double left = this.sector.getMinLongitude().getDegrees();
            double right = this.sector.getMaxLongitude().getDegrees();

            double leftDecimal = (lon - left) / (right - left);
            double bottomDecimal = (lat - bottom) / (top - bottom);

            int row = (int) (bottomDecimal * (this.density));
            int column = (int) (leftDecimal * (this.density));

            double l = createPosition(column, leftDecimal, ri.density);
            double h = createPosition(row, bottomDecimal, ri.density);

            Vec4 result = RectTile.interpolate(row, column, l, h, ri);
            result = result.add3(ri.referenceCenter);
            if (metersOffset != 0)
            {
                result = applyOffset(this.globe, result, metersOffset);
            }

            return result;
        }

        /**
         * Offsets <code>point</code> by <code>metersOffset</code> meters.
         *
         * @param globe        the <code>Globe</code> from which to offset
         * @param point        the <code>Vec4</code> to offset
         * @param metersOffset the magnitude of the offset
         * @return <code>point</code> offset along its surface normal as if it were on <code>globe</code>
         */
        private static Vec4 applyOffset(Globe globe, Vec4 point, double metersOffset)
        {
            Vec4 normal = globe.computeSurfaceNormalAtPoint(point);
            point = Vec4.fromLine3(point, metersOffset, normal);
            return point;
        }

        /**
         * Computes from a column (or row) number, and a given offset ranged [0,1] corresponding to the distance along
         * the edge of this sector, where between this column and the next column the corresponding position will fall,
         * in the range [0,1].
         *
         * @param start   the number of the column or row to the left, below or on this position
         * @param decimal the distance from the left or bottom of the current sector that this position falls
         * @param density the number of intervals along the sector's side
         * @return a decimal ranged [0,1] representing the position between two columns or rows, rather than between two
         *         edges of the sector
         */
        private static double createPosition(int start, double decimal, int density)
        {
            double l = ((double) start) / (double) density;
            double r = ((double) (start + 1)) / (double) density;

            return (decimal - l) / (r - l);
        }

        /**
         * Calculates a <code>Point</code> that sits at <code>xDec</code> offset from <code>column</code> to
         * <code>column + 1</code> and at <code>yDec</code> offset from <code>row</code> to <code>row + 1</code>.
         * Accounts for the diagonals.
         *
         * @param row    represents the row which corresponds to a <code>yDec</code> value of 0
         * @param column represents the column which corresponds to an <code>xDec</code> value of 0
         * @param xDec   constrained to [0,1]
         * @param yDec   constrained to [0,1]
         * @param ri     the render info holding the vertices, etc.
         * @return a <code>Point</code> geometrically within or on the boundary of the quadrilateral whose bottom left
         *         corner is indexed by (<code>row</code>, <code>column</code>)
         */
        private static Vec4 interpolate(int row, int column, double xDec, double yDec, RenderInfo ri)
        {
            row++;
            column++;

            int numVerticesPerEdge = ri.density + 3;

            int bottomLeft = row * numVerticesPerEdge + column;

            bottomLeft *= 3;

            int numVertsTimesThree = numVerticesPerEdge * 3;

            Vec4 bL = new Vec4(ri.vertices.get(bottomLeft), ri.vertices.get(bottomLeft + 1), ri.vertices.get(
                    bottomLeft + 2));
            Vec4 bR = new Vec4(ri.vertices.get(bottomLeft + 3), ri.vertices.get(bottomLeft + 4),
                    ri.vertices.get(bottomLeft + 5));

            bottomLeft += numVertsTimesThree;

            Vec4 tL = new Vec4(ri.vertices.get(bottomLeft), ri.vertices.get(bottomLeft + 1), ri.vertices.get(
                    bottomLeft + 2));
            Vec4 tR = new Vec4(ri.vertices.get(bottomLeft + 3), ri.vertices.get(bottomLeft + 4),
                    ri.vertices.get(bottomLeft + 5));

            return interpolate(bL, bR, tR, tL, xDec, yDec);
        }

        /**
         * Calculates the point at (xDec, yDec) in the two triangles defined by {bL, bR, tL} and {bR, tR, tL}. If
         * thought of as a quadrilateral, the diagonal runs from tL to bR. Of course, this isn't a quad, it's two
         * triangles.
         *
         * @param bL   the bottom left corner
         * @param bR   the bottom right corner
         * @param tR   the top right corner
         * @param tL   the top left corner
         * @param xDec how far along, [0,1] 0 = left edge, 1 = right edge
         * @param yDec how far along, [0,1] 0 = bottom edge, 1 = top edge
         * @return the point xDec, yDec in the co-ordinate system defined by bL, bR, tR, tL
         */
        private static Vec4 interpolate(Vec4 bL, Vec4 bR, Vec4 tR, Vec4 tL, double xDec, double yDec)
        {
            double pos = xDec + yDec;
            if (pos == 1)
            {
                // on the diagonal - what's more, we don't need to do any "oneMinusT" calculation
                return new Vec4(
                        tL.x * yDec + bR.x * xDec,
                        tL.y * yDec + bR.y * xDec,
                        tL.z * yDec + bR.z * xDec);
            }
            else if (pos > 1)
            {
                // in the "top right" half

                // vectors pointing from top right towards the point we want (can be thought of as "negative" vectors)
                Vec4 horizontalVector = (tL.subtract3(tR)).multiply3(1 - xDec);
                Vec4 verticalVector = (bR.subtract3(tR)).multiply3(1 - yDec);

                return tR.add3(horizontalVector).add3(verticalVector);
            }
            else
            {
                // pos < 1 - in the "bottom left" half

                // vectors pointing from the bottom left towards the point we want
                Vec4 horizontalVector = (bR.subtract3(bL)).multiply3(xDec);
                Vec4 verticalVector = (tL.subtract3(bL)).multiply3(yDec);

                return bL.add3(horizontalVector).add3(verticalVector);
            }
        }

        public String toString()
        {
            return "level " + this.level + ", density " + this.density + ", sector " + this.sector;
        }

        protected static java.nio.DoubleBuffer getGeographicTextureCoordinates(int density)
        {
            if (density < 1)
            {
                density = 1;
            }

            // Approximate 1 to avoid shearing off of right and top skirts in SurfaceTileRenderer.
            // TODO: dig into this more: why are the skirts being sheared off?
            final double one = 0.999999;

            java.nio.DoubleBuffer p = parameterizations.get(density);
            if (p != null)
            {
                return p;
            }

            int coordCount = (density + 3) * (density + 3);
            p = com.sun.opengl.util.BufferUtil.newDoubleBuffer(2 * coordCount);
            double delta = 1d / density;
            int k = 2 * (density + 3);
            for (int j = 0; j < density; j++)
            {
                double v = j * delta;

                // skirt column; duplicate first column
                p.put(k++, 0d);
                p.put(k++, v);

                // interior columns
                for (int i = 0; i < density; i++)
                {
                    p.put(k++, i * delta); // u
                    p.put(k++, v);
                }

                // last interior column; force u to 1.
                p.put(k++, one);//1d);
                p.put(k++, v);

                // skirt column; duplicate previous column
                p.put(k++, one);//1d);
                p.put(k++, v);
            }

            // Last interior row
            //noinspection UnnecessaryLocalVariable
            double v = one;//1d;
            p.put(k++, 0d); // skirt column
            p.put(k++, v);

            for (int i = 0; i < density; i++)
            {
                p.put(k++, i * delta); // u
                p.put(k++, v);
            }
            p.put(k++, one);//1d); // last interior column
            p.put(k++, v);

            p.put(k++, one);//1d); // skirt column
            p.put(k++, v);

            // last skirt row
            int kk = k - 2 * (density + 3);
            for (int i = 0; i < density + 3; i++)
            {
                p.put(k++, p.get(kk++));
                p.put(k++, p.get(kk++));
            }

            // first skirt row
            k = 0;
            kk = 2 * (density + 3);
            for (int i = 0; i < density + 3; i++)
            {
                p.put(k++, p.get(kk++));
                p.put(k++, p.get(kk++));
            }

            parameterizations.put(density, p);

            return p;
        }
//
//        protected static DoubleBuffer getGeometricTextureCoordinates(Sector sector, double radius, int density)
//        {
//            if (density < 1)
//                density = 1;
//
//            // Approximate 1 to avoid shearing off of right and top skirts in SurfaceTileRenderer.
//            // TODO: dig into this more: why are the skirts being sheared off?
//            final double one = 0.999999;
//
//            DoubleBuffer p;
//            int coordCount = (density + 3) * (density + 3);
//            p = BufferUtil.newDoubleBuffer(2 * coordCount);
//
//            LatLon sw = new LatLon(sector.getMinLatitude(), sector.getMinLongitude());
//            LatLon nw = new LatLon(sector.getMaxLatitude(), sector.getMinLongitude());
//            LatLon se = new LatLon(sector.getMinLatitude(), sector.getMaxLongitude());
//            LatLon ne = new LatLon(sector.getMaxLatitude(), sector.getMaxLongitude());
//
//            double swse = SphericalGeometry.unitDistance(sw, se);
//            double nwne = SphericalGeometry.unitDistance(nw, ne);
//            double swnw = SphericalGeometry.unitDistance(sw, nw);
//            double sene = SphericalGeometry.unitDistance(se, ne);
//            double s3 = 0.5 * (swse - nwne) / swse;
//            double s4 = s3 + nwne / swse;
//
//            double dt = 1d / density;
//            int k = 2 * (density + 3);
//            for (int j = 0; j < density; j++)
//            {
//                double t = j * dt;
//                double s3p = t * s3;
//                double s4p = 1 - s3p;//1 + t * (s4 - 1);
//                double ds = (s4p - s3p) / density;
//                double s = s3p;
//
//                // skirt column; duplicate first column
//                p.put(k++, s);
//                p.put(k++, t);
//
//                // interior columns
//                for (int i = 0; i <= density; i++)
//                {
//                    p.put(k++, s + i * ds); // u
//                    p.put(k++, t);
//                }
//
//                // skirt column; duplicate previous column
//                p.put(k++, s + ds * density);
//                p.put(k++, t);
//            }
//
//            // Last interior row
//            //noinspection UnnecessaryLocalVariable
//            double t = one;
//            double ds = (s4 - s3) / density;
//            //noinspection UnnecessaryLocalVariable
//            double s = s3;
//
//            p.put(k++, s); // skirt column
//            p.put(k++, t);
//
//            for (int i = 0; i <= density; i++)
//            {
//                p.put(k++, s + i * ds); // u
//                p.put(k++, t);
//            }
//
//            p.put(k++, s + ds * density);//1d); // skirt column
//            p.put(k++, t);
//
//            // last skirt row
//            int kk = k - 2 * (density + 3);
//            for (int i = 0; i < density + 3; i++)
//            {
//                p.put(k++, p.get(kk++));
//                p.put(k++, p.get(kk++));
//            }
//
//            // first skirt row
//            k = 0;
//            kk = 2 * (density + 3);
//            for (int i = 0; i < density + 3; i++)
//            {
//                p.put(k++, p.get(kk++));
//                p.put(k++, p.get(kk++));
//            }
//
//            return p;
//        }
        private static java.nio.IntBuffer getIndices(int density)
        {
            if (density < 1)
            {
                density = 1;
            }

            // return a pre-computed buffer if possible.
            java.nio.IntBuffer buffer = indexLists.get(density);
            if (buffer != null)
            {
                return buffer;
            }

            int sideSize = density + 2;

            int indexCount = 2 * sideSize * sideSize + 4 * sideSize - 2;
            buffer = com.sun.opengl.util.BufferUtil.newIntBuffer(indexCount);
            int k = 0;
            for (int i = 0; i < sideSize; i++)
            {
                buffer.put(k);
                if (i > 0)
                {
                    buffer.put(++k);
                    buffer.put(k);
                }

                if (i % 2 == 0) // even
                {
                    buffer.put(++k);
                    for (int j = 0; j < sideSize; j++)
                    {
                        k += sideSize;
                        buffer.put(k);
                        buffer.put(++k);
                    }
                }
                else // odd
                {
                    buffer.put(--k);
                    for (int j = 0; j < sideSize; j++)
                    {
                        k -= sideSize;
                        buffer.put(k);
                        buffer.put(--k);
                    }
                }
            }

            indexLists.put(density, buffer);

            return buffer;
        }

        /**
         * Default Renderable implementation.
         */
        public boolean isEnabled()
        {
            return true;
        }

        public void setEnabled(boolean enabled)
        {
        // Do nothing.
        }

        public String getName()
        {
            return null;
        }

        public void setName(String name)
        {
        // Do nothing.
        }

        public double getOpacity()
        {
            return 1;
        }

        public void setOpacity(double opacity)
        {
        // Do nothing.
        }
    }
    private java.util.ArrayList<RectTile> topLevels;
    private SectorGeometryList currentTiles = new SectorGeometryList();
    private Frustum currentFrustum;
    private int currentLevel;
    private int maxLevel = DEFAULT_MAX_LEVEL;//14; // TODO: Make configurable
    private Sector sector; // union of all tiles selected during call to render()
    private int density = DEFAULT_DENSITY; // TODO: make configurable

    public EllipsoidRectangularTessellator()
    {
//        this.topLevels = createTopLevelTiles(globe, DEFAULT_NUM_LAT_SUBDIVISIONS, DEFAULT_NUM_LON_SUBDIVISIONS);
    }

    public void setGlobe(Globe globe)
    {
        if (globe == null)
        {
            String msg = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.topLevels = createTopLevelTiles(globe, DEFAULT_NUM_LAT_SUBDIVISIONS, DEFAULT_NUM_LON_SUBDIVISIONS);
    }

    public Sector getSector()
    {
        return this.sector;
    }

    private ArrayList<RectTile> createTopLevelTiles(Globe globe, int nRows, int nCols)
    {
        ArrayList<RectTile> tops = new ArrayList<RectTile>(nRows * nCols);

        double deltaLat = 180d / nRows;
        double deltaLon = 360d / nCols;
        Angle lastLat = Angle.NEG90;

        for (int row = 0; row < DEFAULT_NUM_LAT_SUBDIVISIONS; row++)
        {
            Angle lat = lastLat.addDegrees(deltaLat);
            if (lat.getDegrees() + 1d > 90d)
            {
                lat = Angle.POS90;
            }

            Angle lastLon = Angle.NEG180;

            for (int col = 0; col < DEFAULT_NUM_LON_SUBDIVISIONS; col++)
            {
                Angle lon = lastLon.addDegrees(deltaLon);
                if (lon.getDegrees() + 1d > 180d)
                {
                    lon = Angle.POS180;
                }

                tops.add(new RectTile(globe, 0, this.density, new Sector(lastLat, lat, lastLon, lon)));
                lastLon = lon;
            }
            lastLat = lat;
        }

        return tops;
    }

    public SectorGeometryList tessellate(DrawContext dc)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (dc.getView() == null)
        {
            String msg = Logging.getMessage("nullValue.ViewIsNull");
            Logging.logger().severe(msg);
            throw new IllegalStateException(msg);
        }

        String cacheName = RectTile.class.getName();
        if (!WorldWind.getMemoryCacheSet().containsCache(cacheName))
        {
            long size = Configuration.getLongValue(AVKey.SECTOR_GEOMETRY_CACHE_SIZE, 20000000L);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName("Terrain");
            WorldWind.getMemoryCacheSet().addCache(cacheName, cache);
        }

        this.currentTiles.clear();
        this.currentLevel = 0;
        this.sector = null;

        this.currentFrustum = dc.getView().getFrustumInModelCoordinates();
        for (RectTile tile : topLevels)
        {
            this.selectVisibleTiles(dc, tile);
        }

        dc.setVisibleSector(this.getSector());

        for (SectorGeometry tile : this.currentTiles)
        {
            ((RectTile) tile).makeVerts(dc);
        }

        return this.currentTiles;
    }

    private void selectVisibleTiles(DrawContext dc, RectTile tile)
    {
        if (!tile.getExtent().intersects(this.currentFrustum))
        {
            return;
        }

        if (this.currentLevel < this.maxLevel && needToSplit(dc, tile))
        {
            ++this.currentLevel;
            RectTile[] subtiles = tile.split();
            for (RectTile child : subtiles)
            {
                this.selectVisibleTiles(dc, child);
            }
            --this.currentLevel;
            return;
        }
        this.sector = tile.getSector().union(this.sector);
        this.currentTiles.add(tile);
    }

    private static boolean needToSplit(DrawContext dc, RectTile tile)
    {
        Vec4[] corners = tile.sector.computeCornerPoints(dc.getGlobe());
        Vec4 centerPoint = tile.sector.computeCenterPoint(dc.getGlobe());

        View view = dc.getView();
        double d1 = view.getEyePoint().distanceTo3(corners[0]);
        double d2 = view.getEyePoint().distanceTo3(corners[1]);
        double d3 = view.getEyePoint().distanceTo3(corners[2]);
        double d4 = view.getEyePoint().distanceTo3(corners[3]);
        double d5 = view.getEyePoint().distanceTo3(centerPoint);

        double minDistance = d1;
        if (d2 < minDistance)
        {
            minDistance = d2;
        }
        if (d3 < minDistance)
        {
            minDistance = d3;
        }
        if (d4 < minDistance)
        {
            minDistance = d4;
        }
        if (d5 < minDistance)
        {
            minDistance = d5;
        }

        double logDist = Math.log10(minDistance);
        boolean useTile = tile.log10CellSize <= (logDist - DEFAULT_LOG10_RESOLUTION_TARGET);

        return !useTile;
    }
//
//    private static void printTextureCoords(DoubleBuffer tcs, int density)
//    {
//        tcs.rewind();
//        for (int j = density + 2; j >= 0; j--)
//        {
//            System.out.printf("%2d: ", j * (density + 3));
//            for (int i = 0; i <= 2 * (density + 2); i += 2)
//            {
//                System.out.printf("(%6.3f, %6.3f)  ",
//                    tcs.get(2 * j * (density + 3) + i), tcs.get(2 * j * (density + 3) + i + 1));
//            }
//            System.out.println();
//        }
//    }
}
