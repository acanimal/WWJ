/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.cache.TextureCache;
import com.sun.opengl.util.texture.*;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.retrieve.HTTPRetriever;
import gov.nasa.worldwind.retrieve.RetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.WWIO;
import java.util.logging.Logger;
import javax.media.opengl.*;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Modified version to allow loads data asynchronously.
 * ( Antonio Santiago [asantiagop(at)gmail.com] )
 * 
 * @author tag
 * @version $Id$
 */
public class SurfaceImage implements SurfaceTile, Renderable
{

    private final Object imageSource;
    private Sector sector;
    private Extent extent;
    private double extentVerticalExaggertion = Double.MIN_VALUE; // VE used to calculate the extent
    private TextureData textureData = null;
    private boolean loading = false;
    private boolean hasProblem = false;

    public SurfaceImage(Object imageSource, Sector sector)
    {
        if (imageSource == null)
        {
            String message = Logging.getMessage("nullValue.ImageSource");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.Sector");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        this.imageSource = imageSource;
        this.sector = sector;
    }

    public Object getImageSource()
    {
        return imageSource;
    }

    public Sector getSector()
    {
        return this.sector;
    }

    public void setSector(Sector sector)
    {
        this.sector = sector;
    }

    public Extent getExtent(DrawContext dc)
    {
        if (this.extent == null || dc.getVerticalExaggeration() != this.extentVerticalExaggertion)
        {
            this.extent = Sector.computeBoundingCylinder(dc.getGlobe(), dc.getVerticalExaggeration(), this.sector);
            this.extentVerticalExaggertion = dc.getVerticalExaggeration();
        }

        return this.extent;
    }

    private void setTexture(TextureCache tc, Texture texture)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        tc.put(this.imageSource, texture);
    }

    private Texture getTexture(TextureCache tc)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return tc.get(this.imageSource);
    }

    private Texture initializeTexture(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t = null;
        if (this.imageSource instanceof String)
        {
            String path = (String) this.imageSource;
            if (!this.loading && !this.hasProblem && this.textureData == null)
            {
                sendLoadRequests(path);
            }

            if (this.textureData != null)
            {
                t = TextureIO.newTexture(this.textureData);
            }
        }
        else if (this.imageSource instanceof BufferedImage)
        {
            try
            {
                t = TextureIO.newTexture((BufferedImage) this.imageSource, true);
            }
            catch (Exception e)
            {
                Logging.logger().log(java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
                return null;
            }
        }
        else
        {
        // TODO: Log case of unknown image-source type.
        }
        if (t == null)
        {
            // TODO: Log case.
            return null;
        }

        // Textures with the same path are assumed to be identical textures, so key the texture id off the
        // image source.
        this.setTexture(dc.getTextureCache(), t);
        t.bind();

        GL gl = dc.getGL();
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);//_MIPMAP_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

        return t;
    }

    public boolean bind(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Texture t = this.getTexture(dc.getTextureCache());
        if (t == null)
        {
            t = this.initializeTexture(dc);
            if (t != null)
            {
                return true;
            } // texture was bound during initialization.

        }

        if (t != null)
        {
            t.bind();
        }

        return t != null;
    }

    public void applyInternalTransform(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        // Use the tile's texture if available.
        Texture t = this.getTexture(dc.getTextureCache());
        if (t == null)
        {
            t = this.initializeTexture(dc);
        }

        if (t != null)
        {
            if (t.getMustFlipVertically())
            {
                GL gl = GLContext.getCurrent().getGL();
                gl.glMatrixMode(GL.GL_TEXTURE);
                gl.glLoadIdentity();
                gl.glScaled(1, -1, 1);
                gl.glTranslated(0, -1, 0);
            }

        }
    }

    // Render the surface image tile
    public void render(DrawContext dc)
    {
        if (!this.sector.intersects(dc.getVisibleSector()))
        {
            return;
        }

        GL gl = dc.getGL();
        gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_POLYGON_BIT);
        try
        {
            if (!dc.isPickingMode())
            {
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            }

            gl.glPolygonMode(GL.GL_FRONT, GL.GL_FILL);
            gl.glEnable(GL.GL_CULL_FACE);
            gl.glCullFace(GL.GL_BACK);

            dc.getGeographicSurfaceTileRenderer().renderTile(dc, this);
        }
        finally
        {
            gl.glPopAttrib();
        }

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

        SurfaceImage that = (SurfaceImage) o;

        return imageSource.equals(that.imageSource) && sector.equals(that.sector);

    }

    public int hashCode()
    {
        int result;
        result = imageSource.hashCode();
        result = 31 * result + sector.hashCode();
        return result;
    }

    private void sendLoadRequests(String path)
    {
        if (WorldWind.getTaskService().isFull())
        {
            return;
        }

        this.loading = true;
        WorldWind.getTaskService().addTask(new RequestTask(path));
    }

    private class RequestTask implements Runnable
    {

        private final String path;

        public RequestTask(String path)
        {
            this.path = path;
        }

        public void run()
        {

            final java.net.URL textureURL = WorldWind.getDataFileCache().findFile(path, false);

            if (textureURL != null)
            {
                // Load cached texture
                loadCachedTexture(textureURL);
            }
            else
            {
                // Load texture
                loadTexture(path);
            }
        }

        private boolean loadCachedTexture(java.net.URL textureURL)
        {
            if (WWIO.isFileOutOfDate(textureURL, 0))
            {
                // The file has expired. Delete it then request download of newer.
                gov.nasa.worldwind.WorldWind.getDataFileCache().removeFile(textureURL);
                String message = Logging.getMessage("generic.DataFileExpired", textureURL);
                Logging.logger().fine(message);
            }

            TextureData textureData = null;
            try
            {
                textureData = TextureIO.newTextureData(textureURL, true, null);
                SurfaceImage.this.textureData = textureData;
            }
            catch (Exception e)
            {
                Logging.logger().log(java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
            }

            if (textureData == null)
            {
                return false;
            }

            return true;
        }

        private void loadTexture(final String path)
        {

            java.io.InputStream iconStream = null;

            // Try to handle path as an URL
            try
            {
                URL url = new URL(path);
                if ("http".equalsIgnoreCase(url.getProtocol()))
                {
                    // Load asynchronously
                    readTexture(url);
                }
                else
                {
                    SurfaceImage.this.hasProblem = true;
                    Logger.getLogger(SurfaceImage.class.getName()).warning("Invalid protocol for: " + url);
                }
            }
            catch (MalformedURLException ex)
            {
                // Not an URL. Handle as local image.
                iconStream = this.getClass().getResourceAsStream("/" + path);

                if (iconStream == null)
                {
                    java.io.File iconFile = new java.io.File(path);
                    if (iconFile.exists())
                    {
                        try
                        {
                            SurfaceImage.this.textureData = TextureIO.newTextureData(iconFile, true, null);
                        }
                        catch (Exception e)
                        {
                            Logging.logger().log(java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
                        }
                    }
                    else
                    {
                        SurfaceImage.this.hasProblem = true;
                        Logger.getLogger(SurfaceImage.class.getName()).warning("Resource not exists: " + path);
                    }
                }
                else
                {
                    try
                    {
                        SurfaceImage.this.textureData = TextureIO.newTextureData(iconStream, true, null);
                    } 
                    catch (Exception e)
                    {
                        Logging.logger().log(java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
                    }
                }
            }
        }

        private void readTexture(final URL url)
        {
            if (WorldWind.getRetrievalService().isFull())
            {
                return;
            }

            if ("http".equalsIgnoreCase(url.getProtocol()))
            {
                Retriever retriever = new HTTPRetriever(url, new DownloadPostProcessor());
                WorldWind.getRetrievalService().runRetriever(retriever);
            }
            else
            {
                SurfaceImage.this.hasProblem = true;
                Logging.logger().severe(Logging.getMessage("layers.TextureLayer.UnknownRetrievalProtocol", url.toString()));
                return;
            }
        }
    }

    private class DownloadPostProcessor implements RetrievalPostProcessor
    {

        public ByteBuffer run(Retriever retriever)
        {
            if (retriever == null)
            {
                // Mark as missing data.
                SurfaceImage.this.hasProblem = true;
                String msg = Logging.getMessage("nullValue.RetrieverIsNull");
                Logging.logger().severe(msg);
                throw new IllegalArgumentException(msg);
            }

            if (!retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
            {
                Logging.logger().severe("Retriver state not successful.");
                // Mark as missing data.
                SurfaceImage.this.hasProblem = true;
                return null;
            }

            HTTPRetriever htr = (HTTPRetriever) retriever;
            if (htr.getResponseCode() != HttpURLConnection.HTTP_OK)
            {
                Logging.logger().severe("HTTP response not OK (" + htr.getResponseMessage() + ").");
                // Mark as missing data.
                SurfaceImage.this.hasProblem = true;
                return null;
            }

            URLRetriever r = (URLRetriever) retriever;
            ByteBuffer buffer = r.getBuffer();
            if (buffer != null)
            {
                try
                {
                    // Store the file in the cache
                    String name = htr.getUrl().toExternalForm();
                    final File outFile = WorldWind.getDataFileCache().newFile(name);
                    if (outFile == null)
                    {
                        String msg = Logging.getMessage("generic.CantCreateCacheFile", name);
                        Logging.logger().warning(msg);
                        return null;
                    }
                    else
                    {
                        WWIO.saveBuffer(buffer, outFile);
                    }

                    SurfaceImage.this.textureData = TextureIO.newTextureData(new ByteArrayInputStream(buffer.array()), true, null);
                }
                catch (Exception e)
                {
                    Logging.logger().log(java.util.logging.Level.SEVERE, "layers.TextureLayer.ExceptionAttemptingToReadTextureFile", e);
                }
            }

            SurfaceImage.this.hasProblem = true;
            return null;
        }
    }
}
