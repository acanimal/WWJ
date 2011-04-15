/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.retrieve.*;
import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.view.View;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @author tag
 * @version $Id: TiledImageLayer.java 2790 2007-09-10 19:40:33Z tgaskins $
 */
public abstract class TiledImageLayer extends AbstractLayer
{
    // Infrastructure
    private static final LevelComparer levelComparer = new LevelComparer();
    private final LevelSet levels;
    private ArrayList<TextureTile> topLevels;
    private boolean forceLevelZeroLoads = false;
    private boolean levelZeroLoaded = false;
    private boolean retainLevelZeroTiles = false;
    private String tileCountName;
    private double splitScale = 0.9; // TODO: Make configurable

    // Diagnostic flags
    private boolean showImageTileOutlines = false;
    private boolean drawTileBoundaries = false;
    private boolean useTransparentTextures = false;
    private boolean drawTileIDs = false;
    private boolean drawBoundingVolumes = false;
    private TextRenderer textRenderer = null;

    // Stuff computed each frame
    private ArrayList<TextureTile> currentTiles = new ArrayList<TextureTile>();
    private TextureTile currentResourceTile;
    private Vec4 referencePoint;
    private PriorityBlockingQueue<Runnable> requestQ = new PriorityBlockingQueue<Runnable>(200);

    abstract protected void requestTexture(DrawContext dc, TextureTile tile);

    abstract protected void forceTextureLoad(TextureTile tile);

    public TiledImageLayer(LevelSet levelSet)
    {
        if (levelSet == null)
        {
            String message = Logging.getMessage("nullValue.LevelSetIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.levels = new LevelSet(levelSet); // the caller's levelSet may change internally, so we copy it.

        this.createTopLevelTiles();

        this.setPickEnabled(false); // textures are assumed to be terrain unless specifically indicated otherwise.
        this.tileCountName = this.getName() + " Tiles";
    }

    @Override
    public void setName(String name)
    {
        super.setName(name);
        this.tileCountName = this.getName() + " Tiles";
    }

    public boolean isUseTransparentTextures()
    {
        return this.useTransparentTextures;
    }

    public void setUseTransparentTextures(boolean useTransparentTextures)
    {
        this.useTransparentTextures = useTransparentTextures;
    }

    public boolean isForceLevelZeroLoads()
    {
        return this.forceLevelZeroLoads;
    }

    public void setForceLevelZeroLoads(boolean forceLevelZeroLoads)
    {
        this.forceLevelZeroLoads = forceLevelZeroLoads;
    }

    public boolean isRetainLevelZeroTiles()
    {
        return retainLevelZeroTiles;
    }

    public void setRetainLevelZeroTiles(boolean retainLevelZeroTiles)
    {
        this.retainLevelZeroTiles = retainLevelZeroTiles;
    }

    public boolean isDrawTileIDs()
    {
        return drawTileIDs;
    }

    public void setDrawTileIDs(boolean drawTileIDs)
    {
        this.drawTileIDs = drawTileIDs;
    }

    public boolean isDrawTileBoundaries()
    {
        return drawTileBoundaries;
    }

    public void setDrawTileBoundaries(boolean drawTileBoundaries)
    {
        this.drawTileBoundaries = drawTileBoundaries;
    }

    public boolean isShowImageTileOutlines()
    {
        return showImageTileOutlines;
    }

    public void setShowImageTileOutlines(boolean showImageTileOutlines)
    {
        this.showImageTileOutlines = showImageTileOutlines;
    }

    public boolean isDrawBoundingVolumes()
    {
        return drawBoundingVolumes;
    }

    public void setDrawBoundingVolumes(boolean drawBoundingVolumes)
    {
        this.drawBoundingVolumes = drawBoundingVolumes;
    }

    protected LevelSet getLevels()
    {
        return levels;
    }

    protected void setSplitScale(double splitScale)
    {
        this.splitScale = splitScale;
    }

    protected PriorityBlockingQueue<Runnable> getRequestQ()
    {
        return requestQ;
    }

    private void createTopLevelTiles()
    {
        Sector sector = this.levels.getSector();

        Angle dLat = this.levels.getLevelZeroTileDelta().getLatitude();
        Angle dLon = this.levels.getLevelZeroTileDelta().getLongitude();

        // Determine the row and column offset from the common World Wind global tiling origin.
        Level level = levels.getFirstLevel();
        int firstRow = Tile.computeRow(level.getTileDelta().getLatitude(), sector.getMinLatitude());
        int firstCol = Tile.computeColumn(level.getTileDelta().getLongitude(), sector.getMinLongitude());
        int lastRow = Tile.computeRow(level.getTileDelta().getLatitude(), sector.getMaxLatitude());
        int lastCol = Tile.computeColumn(level.getTileDelta().getLongitude(), sector.getMaxLongitude());

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;

        this.topLevels = new ArrayList<TextureTile>(nLatTiles * nLonTiles);

        Angle p1 = Tile.computeRowLatitude(firstRow, dLat);
        for (int row = firstRow; row <= lastRow; row++)
        {
            Angle p2;
            p2 = p1.add(dLat);

            Angle t1 = Tile.computeColumnLongitude(firstCol, dLon);
            for (int col = firstCol; col <= lastCol; col++)
            {
                Angle t2;
                t2 = t1.add(dLon);

                this.topLevels.add(new TextureTile(new Sector(p1, p2, t1, t2), level, row, col));
                t1 = t2;
            }
            p1 = p2;
        }
    }

    private void loadAllTopLevelTextures(DrawContext dc)
    {
        for (TextureTile tile : this.topLevels)
        {
            if (!tile.isTextureInMemory(dc.getTextureCache()))
                this.forceTextureLoad(tile);
        }

        this.levelZeroLoaded = true;
    }

    // ============== Tile Assembly ======================= //
    // ============== Tile Assembly ======================= //
    // ============== Tile Assembly ======================= //

    private void assembleTiles(DrawContext dc)
    {
        this.currentTiles.clear();

        for (TextureTile tile : this.topLevels)
        {
            if (this.isTileVisible(dc, tile))
            {
                this.currentResourceTile = null;
                this.addTileOrDescendants(dc, tile);
            }
        }
    }

    private void addTileOrDescendants(DrawContext dc, TextureTile tile)
    {
        if (this.meetsRenderCriteria(dc, tile))
        {
            this.addTile(dc, tile);
            return;
        }

        // The incoming tile does not meet the rendering criteria, so it must be subdivided and those
        // subdivisions tested against the criteria.

        // All tiles that meet the selection criteria are drawn, but some of those tiles will not have
        // textures associated with them either because their texture isn't loaded yet or because they
        // are finer grain than the layer has textures for. In these cases the tiles use the texture of
        // the closest ancestor that has a texture loaded. This ancestor is called the currentResourceTile.
        // A texture transform is applied during rendering to align the sector's texture coordinates with the
        // appropriate region of the ancestor's texture.

        TextureTile ancestorResource = null;

        try
        {
            if (tile.isTextureInMemory(dc.getTextureCache()) || tile.getLevelNumber() == 0)
            {
                ancestorResource = this.currentResourceTile;
                this.currentResourceTile = tile;
            }

            // Ensure that levels finer than the finest image have the finest image around
            // TODO: find finest level with a non-missing tile
            if (this.levels.isFinalLevel(tile.getLevelNumber()) && !tile.isTextureInMemory(dc.getTextureCache()))
                this.requestTexture(dc, tile);

            TextureTile[] subTiles = tile.createSubTiles(this.levels.getLevel(tile.getLevelNumber() + 1));
            for (TextureTile child : subTiles)
            {
                if (this.isTileVisible(dc, child))
                    this.addTileOrDescendants(dc, child);
            }
        }
        finally
        {
            if (ancestorResource != null) // Pop this tile as the currentResource ancestor
                this.currentResourceTile = ancestorResource;
        }
    }

    private void addTile(DrawContext dc, TextureTile tile)
    {
        tile.setFallbackTile(null);

        if (tile.isTextureInMemory(dc.getTextureCache()))
        {
//            System.out.printf("Sector %s, min = %f, max = %f\n", tile.getSector(),
//                dc.getGlobe().getMinElevation(tile.getSector()), dc.getGlobe().getMaxElevation(tile.getSector()));
            this.addTileToCurrent(tile);
            return;
        }

        // Level 0 loads may be forced
        if (tile.getLevelNumber() == 0 && this.forceLevelZeroLoads && !tile.isTextureInMemory(dc.getTextureCache()))
        {
            this.forceTextureLoad(tile);
            if (tile.isTextureInMemory(dc.getTextureCache()))
            {
                this.addTileToCurrent(tile);
                return;
            }
        }

        // Tile's texture isn't available, so request it
        if (tile.getLevelNumber() < this.levels.getNumLevels())
        {
            // Request only tiles with data associated at this level
            if (!this.levels.isResourceAbsent(tile))
                this.requestTexture(dc, tile);
        }

        // Set up to use the currentResource tile's texture
        if (this.currentResourceTile != null)
        {
            if (this.currentResourceTile.getLevelNumber() == 0 && this.forceLevelZeroLoads &&
                !this.currentResourceTile.isTextureInMemory(dc.getTextureCache()) &&
                !this.currentResourceTile.isTextureInMemory(dc.getTextureCache()))
                this.forceTextureLoad(this.currentResourceTile);

            if (this.currentResourceTile.isTextureInMemory(dc.getTextureCache()))
            {
                tile.setFallbackTile(currentResourceTile);
                this.addTileToCurrent(tile);
            }
        }
    }

    private void addTileToCurrent(TextureTile tile)
    {
        this.currentTiles.add(tile);
    }

    private boolean isTileVisible(DrawContext dc, TextureTile tile)
    {
//        if (!(tile.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates())
//            && (dc.getVisibleSector() == null || dc.getVisibleSector().intersects(tile.getSector()))))
//            return false;
//
//        Position eyePos = dc.getView().getEyePosition();
//        LatLon centroid = tile.getSector().getCentroid();
//        Angle d = LatLon.sphericalDistance(eyePos.getLatLon(), centroid);
//        if ((!tile.getLevelName().equals("0")) && d.compareTo(tile.getSector().getDeltaLat().multiply(2.5)) == 1)
//            return false;
//
//        return true;
//
        return tile.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates()) &&
            (dc.getVisibleSector() == null || dc.getVisibleSector().intersects(tile.getSector()));
    }
//
//    private boolean meetsRenderCriteria2(DrawContext dc, TextureTile tile)
//    {
//        if (this.levels.isFinalLevel(tile.getLevelNumber()))
//            return true;
//
//        Sector sector = tile.getSector();
//        Vec4[] corners = sector.computeCornerPoints(dc.getGlobe());
//        Vec4 centerPoint = sector.computeCenterPoint(dc.getGlobe());
//
//        View view = dc.getView();
//        double d1 = view.getEyePoint().distanceTo3(corners[0]);
//        double d2 = view.getEyePoint().distanceTo3(corners[1]);
//        double d3 = view.getEyePoint().distanceTo3(corners[2]);
//        double d4 = view.getEyePoint().distanceTo3(corners[3]);
//        double d5 = view.getEyePoint().distanceTo3(centerPoint);
//
//        double minDistance = d1;
//        if (d2 < minDistance)
//            minDistance = d2;
//        if (d3 < minDistance)
//            minDistance = d3;
//        if (d4 < minDistance)
//            minDistance = d4;
//        if (d5 < minDistance)
//            minDistance = d5;
//
//        double r = 0;
//        if (minDistance == d1)
//            r = corners[0].getLength3();
//        if (minDistance == d2)
//            r = corners[1].getLength3();
//        if (minDistance == d3)
//            r = corners[2].getLength3();
//        if (minDistance == d4)
//            r = corners[3].getLength3();
//        if (minDistance == d5)
//            r = centerPoint.getLength3();
//
//        double texelSize = tile.getLevel().getTexelSize(r);
//        double pixelSize = dc.getView().computePixelSizeAtDistance(minDistance);
//
//        return 2 * pixelSize >= texelSize;
//    }

    private boolean meetsRenderCriteria(DrawContext dc, TextureTile tile)
    {
        return this.levels.isFinalLevel(tile.getLevelNumber()) || !needToSplit(dc, tile.getSector(), 20);
    }

    private boolean needToSplit(DrawContext dc, Sector sector, int density)
    {
        Vec4[] corners = sector.computeCornerPoints(dc.getGlobe());
        Vec4 centerPoint = sector.computeCenterPoint(dc.getGlobe());

        View view = dc.getView();
        double d1 = view.getEyePoint().distanceTo3(corners[0]);
        double d2 = view.getEyePoint().distanceTo3(corners[1]);
        double d3 = view.getEyePoint().distanceTo3(corners[2]);
        double d4 = view.getEyePoint().distanceTo3(corners[3]);
        double d5 = view.getEyePoint().distanceTo3(centerPoint);

        double minDistance = d1;
        if (d2 < minDistance)
            minDistance = d2;
        if (d3 < minDistance)
            minDistance = d3;
        if (d4 < minDistance)
            minDistance = d4;
        if (d5 < minDistance)
            minDistance = d5;

        double cellSize = (Math.PI * sector.getDeltaLatRadians() * dc.getGlobe().getRadius()) / density;

        return !(Math.log10(cellSize) <= (Math.log10(minDistance) - this.splitScale));
    }

    // ============== Rendering ======================= //
    // ============== Rendering ======================= //
    // ============== Rendering ======================= //

    @Override
    protected final void doRender(DrawContext dc)
    {
        if (this.forceLevelZeroLoads && !this.levelZeroLoaded)
            this.loadAllTopLevelTextures(dc);
        if (dc.getSurfaceGeometry() == null || dc.getSurfaceGeometry().size() < 1)
            return; // TODO: throw an illegal state exception?

        dc.getGeographicSurfaceTileRenderer().setShowImageTileOutlines(this.showImageTileOutlines);

        draw(dc);
    }

    private void draw(DrawContext dc)
    {
        this.referencePoint = this.computeReferencePoint(dc);

        this.assembleTiles(dc); // Determine the tiles to draw.

        if (this.currentTiles.size() >= 1)
        {
            TextureTile[] sortedTiles = new TextureTile[this.currentTiles.size()];
            sortedTiles = this.currentTiles.toArray(sortedTiles);
            Arrays.sort(sortedTiles, levelComparer);

            GL gl = dc.getGL();

            gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_POLYGON_BIT);

            if (this.isUseTransparentTextures())
            {
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            }

            gl.glPolygonMode(GL.GL_FRONT, GL.GL_FILL);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);

            dc.setPerFrameStatistic(PerformanceStatistic.IMAGE_TILE_COUNT, this.tileCountName,
                this.currentTiles.size());
            dc.getGeographicSurfaceTileRenderer().renderTiles(dc, this.currentTiles, this);

            gl.glPopAttrib();

            if (this.drawTileIDs)
                this.drawTileIDs(dc, this.currentTiles);

            if (this.drawBoundingVolumes)
                this.drawBoundingVolumes(dc, this.currentTiles);

            this.currentTiles.clear();
        }

        this.sendRequests();
        this.requestQ.clear();
    }

    private void sendRequests()
    {
        Runnable task = this.requestQ.poll();
        while (task != null)
        {
            if (!WorldWind.getTaskService().isFull())
            {
                WorldWind.getTaskService().addTask(task);
            }
            task = this.requestQ.poll();
        }
    }

    public boolean isLayerInView(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (dc.getView() == null)
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return !(dc.getVisibleSector() != null && !this.levels.getSector().intersects(dc.getVisibleSector()));
    }

    private Vec4 computeReferencePoint(DrawContext dc)
    {
        java.awt.geom.Rectangle2D viewport = dc.getView().getViewport();
        int x = (int) viewport.getWidth() / 2;
        for (int y = (int) (0.75 * viewport.getHeight()); y >= 0; y--)
        {
            Position pos = dc.getView().computePositionFromScreenPoint(x, y);
            if (pos == null)
                continue;

            return dc.getGlobe().computePointFromPosition(pos.getLatitude(), pos.getLongitude(), 0d);
        }

        return null;
    }

    protected Vec4 getReferencePoint()
    {
        return this.referencePoint;
    }

    private static class LevelComparer implements Comparator<TextureTile>
    {
        public int compare(TextureTile ta, TextureTile tb)
        {
            int la = ta.getFallbackTile() == null ? ta.getLevelNumber() : ta.getFallbackTile().getLevelNumber();
            int lb = tb.getFallbackTile() == null ? tb.getLevelNumber() : tb.getFallbackTile().getLevelNumber();

            return la < lb ? -1 : la == lb ? 0 : 1;
        }
    }

    private void drawTileIDs(DrawContext dc, ArrayList<TextureTile> tiles)
    {
        java.awt.Rectangle viewport = dc.getView().getViewport();
        if (this.textRenderer == null)
            this.textRenderer = new TextRenderer(java.awt.Font.decode("Arial-Plain-13"), true, true);

        dc.getGL().glDisable(GL.GL_DEPTH_TEST);
        dc.getGL().glDisable(GL.GL_BLEND);
        dc.getGL().glDisable(GL.GL_TEXTURE_2D);

        this.textRenderer.setColor(java.awt.Color.YELLOW);
        this.textRenderer.beginRendering(viewport.width, viewport.height);
        for (TextureTile tile : tiles)
        {
            String tileLabel = tile.getLabel();

            if (tile.getFallbackTile() != null)
                tileLabel += "/" + tile.getFallbackTile().getLabel();

            LatLon ll = tile.getSector().getCentroid();
            Vec4 pt = dc.getGlobe().computePointFromPosition(ll.getLatitude(), ll.getLongitude(),
                dc.getGlobe().getElevation(ll.getLatitude(), ll.getLongitude()));
            pt = dc.getView().project(pt);
            this.textRenderer.draw(tileLabel, (int) pt.x, (int) pt.y);
        }
        this.textRenderer.endRendering();
    }

    private void drawBoundingVolumes(DrawContext dc, ArrayList<TextureTile> tiles)
    {
        float[] previousColor = new float[4];
        dc.getGL().glGetFloatv(GL.GL_CURRENT_COLOR, previousColor, 0);
        dc.getGL().glColor3d(0, 1, 0);

        for (TextureTile tile : tiles)
        {
            ((Cylinder) tile.getExtent(dc)).render(dc);
        }

        Cylinder c =
            Sector.computeBoundingCylinder(dc.getGlobe(), dc.getVerticalExaggeration(), this.levels.getSector());
        dc.getGL().glColor3d(1, 1, 0);
        c.render(dc);

        dc.getGL().glColor4fv(previousColor, 0);
    }
//
//    private TextureTile getContainingTile(TextureTile tile, Angle latitude, Angle longitude, int levelNumber)
//    {
//        if (!tile.getSector().contains(latitude, longitude))
//            return null;
//
//        if (tile.getLevelNumber() == levelNumber || this.levels.isFinalLevel(tile.getLevelNumber()))
//            return tile;
//
//        TextureTile containingTile;
//        TextureTile[] subTiles = tile.createSubTiles(this.levels.getLevel(tile.getLevelNumber() + 1));
//        for (TextureTile child : subTiles)
//        {
//            containingTile = this.getContainingTile(child, latitude, longitude, levelNumber);
//            if (containingTile != null)
//                return containingTile;
//        }
//
//        return null;
//    }

    // ============== Image Composition ======================= //
    // ============== Image Composition ======================= //
    // ============== Image Composition ======================= //

    private final static String[] formats = new String[] {"jpg", "jpeg", "png", "tiff"};
    private final static String[] suffixes = new String[] {".jpg", ".jpg", ".png", ".tiff"};

    private BufferedImage requestImage(TextureTile tile)
    {
        URL url = null;
        String pathBase = tile.getPath().substring(0, tile.getPath().lastIndexOf("."));
        for (String suffix : suffixes)
        {
            String path = pathBase + suffix;
            url = WorldWind.getDataFileCache().findFile(path, false);
            if (url != null)
                break;
        }

        if (url == null)
            return null;

        if (WWIO.isFileOutOfDate(url, tile.getLevel().getExpiryTime()))
        {
            // The file has expired. Delete it then request download of newer.
            WorldWind.getDataFileCache().removeFile(url);
            String message = Logging.getMessage("generic.DataFileExpired", url);
            Logging.logger().fine(message);
        }
        else
        {
            try
            {
                BufferedImage image = ImageIO.read(new File(url.toURI()));
                if (image == null)
                {
                    return null; // TODO: warn
                }

                this.levels.unmarkResourceAbsent(tile);
                return image;
            }
            catch (IOException e)
            {
                // Assume that something's wrong with the file and delete it.
                gov.nasa.worldwind.WorldWind.getDataFileCache().removeFile(url);
                this.levels.markResourceAbsent(tile);
                String message = Logging.getMessage("generic.DeletedCorruptDataFile", url);
                Logging.logger().info(message);
            }
            catch (URISyntaxException e)
            {
                e.printStackTrace(); // TODO
            }
        }

        return null;
    }

    private void downloadImage(final TextureTile tile)
    {
        try
        {
            String urlString = tile.getResourceURL().toExternalForm().replace("dds", "");
            final URL resourceURL = new URL(urlString);
            Retriever retriever;

            String protocol = resourceURL.getProtocol();

            if ("http".equalsIgnoreCase(protocol))
            {
                retriever = new HTTPRetriever(resourceURL, new HttpRetrievalPostProcessor(tile));
            }
            else
            {
                // TODO:
                Logging.logger().severe(
                    Logging.getMessage("layers.TextureLayer.UnknownRetrievalProtocol", resourceURL));
                return;
            }

            retriever.call();
        }
        catch (Exception e)
        {
            e.printStackTrace(); // TODO
        }
    }

    private static class ImageSector
    {
        private final BufferedImage image;
        private final Sector sector;

        public ImageSector(BufferedImage image, Sector sector)
        {
            this.image = image;
            this.sector = sector;
        }
    }

    public BufferedImage composeImageForSector(Sector sector, int imageSize)
    {
        return this.composeImageForSector(sector, imageSize, -1);
    }

    public BufferedImage composeImageForSector(Sector sector, int imageSize, int levelNumber)
    {
        TiledImageLayer.ImageSector[][] imageSectors = this.getImagesInSector(sector, levelNumber);

        if (imageSectors == null)
        {
            System.out.println("No images available."); // TODO
            return null;
        }

        Sector actualSector = null;
        for (TiledImageLayer.ImageSector[] isa : imageSectors)
        {
            for (TiledImageLayer.ImageSector is : isa)
            {
                actualSector = Sector.union(actualSector, is.sector);
            }
        }

        int ny = imageSectors.length;
        int nx = imageSectors[0].length;

        int overallHeight = ny * imageSectors[0][0].image.getHeight();
        int overallWidth = nx * imageSectors[0][0].image.getWidth();

        int tileWidth;
        int tileHeight;
        if (overallHeight >= overallWidth)
        {
            tileHeight = imageSize / ny;
            double aspect = (double) overallWidth / (double) overallHeight;
            tileWidth = (int) (aspect * imageSize / nx);
        }
        else
        {
            tileWidth = imageSize / nx;
            double aspect = (double) overallHeight / (double) overallWidth;
            tileHeight = (int) (aspect * imageSize / ny);
        }

        int imageHeight = tileHeight * imageSectors.length;
        int imageWidth = tileWidth * imageSectors[0].length;

        //noinspection ConstantConditions
        double sh = 1/sector.getDeltaLat().divide(actualSector.getDeltaLat());
        double sw = 1/sector.getDeltaLon().divide(actualSector.getDeltaLon());
        double dh = -(actualSector.getMaxLatitude().subtract(sector.getMaxLatitude()).divide(actualSector.getDeltaLat())
            * imageHeight);
        double dw = -(
            sector.getMinLongitude().subtract(actualSector.getMinLongitude()).divide(actualSector.getDeltaLon())
                * imageWidth);

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(sw, sh);
        g.translate(dw, dh);

        int y = 0;
        for (TiledImageLayer.ImageSector[] row : imageSectors)
        {
            int x = 0;
            for (TiledImageLayer.ImageSector is : row)
            {
                Image img = is.image.getScaledInstance(tileWidth, tileHeight, Image.SCALE_SMOOTH);
                g.drawImage(img, x, y, null);
                x += tileWidth;
            }
            y += tileHeight;
        }

        return image;
    }

    private ImageSector[][] getImagesInSector(Sector sector, int levelNumber)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // TODO: check level number arg

        Level targetLevel = this.levels.getLastLevel();
        if (levelNumber >= 0)
        {
            for (int i = levelNumber; i < this.getLevels().getLastLevel().getLevelNumber(); i++)
            {
                if (this.levels.isLevelEmpty(i))
                    continue;

                targetLevel = this.levels.getLevel(i);
                break;
            }
        }

        // Collect all the tiles intersecting the input sector.
        LatLon delta = targetLevel.getTileDelta();
        final int nwRow = Tile.computeRow(delta.getLatitude(), sector.getMaxLatitude());
        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.getMinLongitude());
        final int seRow = Tile.computeRow(delta.getLatitude(), sector.getMinLatitude());
        final int seCol = Tile.computeColumn(delta.getLongitude(), sector.getMaxLongitude());

        int numRows = nwRow - seRow + 1;
        int numCols = seCol - nwCol + 1;
        ImageSector[][] imageSectors = new ImageSector[numRows][numCols];

        for (int row = nwRow; row >= seRow; row--)
        {
            for (int col = nwCol; col <= seCol; col++)
            {
                TileKey key = new TileKey(targetLevel.getLevelNumber(), row, col, targetLevel.getCacheName());
                Sector tileSector = this.levels.computeSectorForKey(key);
                TextureTile textureTile = new TextureTile(tileSector, targetLevel, row, col);
                BufferedImage image = this.getImage(textureTile);
                if (image != null)
                    imageSectors[nwRow - row][col - nwCol] = new ImageSector(image, textureTile.getSector());
            }
        }

        return imageSectors;
    }

    private BufferedImage getImage(TextureTile tile)
    {
        // TODO: check args

        // Read the image from disk.
        BufferedImage image = this.requestImage(tile);
        if (image != null)
            return image;

        // Retrieve it from the net since it's not on disk.
        this.downloadImage(tile);

        // Try to read from disk again after retrieving it from the net.
        image = this.requestImage(tile);
        if (image != null)
            return image;

        // All attempts to find the image have failed.
        return null;
    }

    private class HttpRetrievalPostProcessor implements RetrievalPostProcessor
    {
        private TextureTile tile;

        public HttpRetrievalPostProcessor(TextureTile tile)
        {
            this.tile = tile;
        }

        public ByteBuffer run(Retriever retriever)
        {
            if (!retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
                return null;

            HTTPRetriever htr = (HTTPRetriever) retriever;
            if (htr.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT)
            {
                // Mark tile as missing to avoid excessive attempts
                TiledImageLayer.this.levels.markResourceAbsent(tile);
                return null;
            }

            URLRetriever r = (URLRetriever) retriever;
            ByteBuffer buffer = r.getBuffer();

            String suffix = null;
            for (int i = 0; i < formats.length; i++)
            {
                if (htr.getContentType().toLowerCase().contains(formats[i]))
                {
                    suffix = suffixes[i];
                    break;
                }
            }
            if (suffix == null)
            {
                return null; // TODO: logger error
            }

            String path = tile.getPath().substring(0, tile.getPath().lastIndexOf("."));
            path += suffix;

            final File outFile = WorldWind.getDataFileCache().newFile(path);
            if (outFile == null)
            {
                String msg = Logging.getMessage("generic.CantCreateCacheFile", tile.getPath());
                Logging.logger().severe(msg);
                return null;
            }

            try
            {
                WWIO.saveBuffer(buffer, outFile);
                return buffer;
            }
            catch (IOException e)
            {
                e.printStackTrace(); // TODO: logger error
                return null;
            }
        }
    }
}