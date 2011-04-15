package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Locatable;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.util.Logging;

import java.awt.Point;
import java.util.Iterator;
import javax.media.opengl.GL;
import java.util.logging.Level;

/**
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class AnnotationWindowRenderer
{

    private PickSupport pickSupport = new PickSupport();

    public AnnotationWindowRenderer()
    {
    }

    private static boolean isWindowValid(AnnotationWindow window, boolean checkPosition)
    {
        //noinspection RedundantIfStatement
        if (checkPosition && window.isEnable() && window.getPosition() == null)
        {
            return false;
        }

        return true;
    }

    public void pick(DrawContext dc, Iterable<AnnotationWindow> windows, java.awt.Point pickPoint, Layer layer)
    {
        this.drawMany(dc, windows);
    }

    public void pick(DrawContext dc, AnnotationWindow window, Vec4 windowPoint, java.awt.Point pickPoint, Layer layer)
    {
        if (!isWindowValid(window, false))
        {
            return;
        }

        this.drawOne(dc, window, windowPoint);
    }

    public void render(DrawContext dc, Iterable<AnnotationWindow> windows)
    {
        this.drawMany(dc, windows);
    }

    public void render(DrawContext dc, AnnotationWindow window, Vec4 windowPoint)
    {
        if (!isWindowValid(window, false))
        {
            return;
        }

        this.drawOne(dc, window, windowPoint);
    }

    private void drawMany(DrawContext dc, Iterable<AnnotationWindow> windows)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (dc.getVisibleSector() == null)
        {
            return;
        }

        if (windows == null)
        {
            String msg = Logging.getMessage("nullValue.IconIterator");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Iterator<AnnotationWindow> iterator = windows.iterator();

        if (!iterator.hasNext())
        {
            return;
        }

        while (iterator.hasNext())
        {
            AnnotationWindow window = iterator.next();
            if (!isWindowValid(window, true))
            {
                continue;
            }

            Point point = window.getPoint();
            dc.addOrderedRenderable(new OrderedWindow(window, point, 1));
        }
    }

    private void drawOne(DrawContext dc, AnnotationWindow window, Vec4 windowPoint)
    {
        if (dc == null)
        {
            String msg = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (dc.getVisibleSector() == null)
        {
            return;
        }

        Point point = window.getPoint();
        dc.addOrderedRenderable(new OrderedWindow(window, point, 1));
    }

    private class OrderedWindow implements OrderedRenderable, Locatable
    {

        AnnotationWindow window;
        Point point;
        double eyeDistance;
        java.awt.Point pickPoint;
        Layer layer;

        OrderedWindow(AnnotationWindow window, Point point, double eyeDistance)
        {
            this.window = window;
            this.point = point;
            this.eyeDistance = eyeDistance;
        }

        OrderedWindow(AnnotationWindow window, Point point, java.awt.Point pickPoint, Layer layer, double eyeDistance)
        {
            this.window = window;
            this.point = point;
            this.eyeDistance = eyeDistance;
            this.pickPoint = pickPoint;
            this.layer = layer;
        }

        public double getDistanceFromEye()
        {
            return this.eyeDistance;
        }

        public Position getPosition()
        {
            return this.window.getPosition();
        }

        public void render(DrawContext dc)
        {
            AnnotationWindowRenderer.this.beginDrawWindows(dc);

            try
            {
                AnnotationWindowRenderer.this.drawWindow(dc, this);
            }
            catch (WWRuntimeException e)
            {
                Logging.logger().log(Level.SEVERE, "generic.ExceptionWhileRenderingWindow", e);
            }
            catch (Exception e)
            {
                Logging.logger().log(Level.SEVERE, "generic.ExceptionWhileRenderingWindow", e);
            }
            finally
            {
                AnnotationWindowRenderer.this.endDrawWindows(dc);
            }
        }

        public void pick(DrawContext dc, java.awt.Point pickPoint)
        {
            AnnotationWindowRenderer.this.pickSupport.clearPickList();
            AnnotationWindowRenderer.this.beginDrawWindows(dc);
            try
            {
                AnnotationWindowRenderer.this.drawWindow(dc, this);
            }
            catch (WWRuntimeException e)
            {
                Logging.logger().log(Level.SEVERE, "generic.ExceptionWhileRenderingWindow", e);
            }
            catch (Exception e)
            {
                Logging.logger().log(Level.SEVERE, "generic.ExceptionWhilePickingWindow", e);
            }
            finally
            {
                AnnotationWindowRenderer.this.endDrawWindows(dc);
                AnnotationWindowRenderer.this.pickSupport.resolvePick(dc, pickPoint, layer);
                AnnotationWindowRenderer.this.pickSupport.clearPickList(); // to ensure entries can be garbage collected
            }
        }
    }

    private void beginDrawWindows(DrawContext dc)
    {
        GL gl = dc.getGL();

        int attributeMask =
                GL.GL_DEPTH_BUFFER_BIT // for depth test, depth mask and depth func
                | GL.GL_TRANSFORM_BIT // for modelview and perspective
                | GL.GL_VIEWPORT_BIT // for depth range
                | GL.GL_CURRENT_BIT // for current color
                | GL.GL_COLOR_BUFFER_BIT // for alpha test func and ref, and blend
                | GL.GL_TEXTURE_BIT // for texture env
                | GL.GL_ENABLE_BIT; // for enable/disable changes
        gl.glPushAttrib(attributeMask);

        gl.glDisable(GL.GL_DEPTH_TEST);

        // Load a parallel projection with xy dimensions (viewportWidth, viewportHeight)
        // into the GL projection matrix.
        java.awt.Rectangle viewport = dc.getView().getViewport();
        gl.glMatrixMode(javax.media.opengl.GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrtho(0d, viewport.width, 0d, viewport.height, -1, 1);

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix();

        if (dc.isPickingMode())
        {
            this.pickSupport.beginPicking(dc);

            // Set up to replace the non-transparent texture colors with the single pick color.
            gl.glEnable(GL.GL_TEXTURE_2D);
            gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_COMBINE);
            gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_SRC0_RGB, GL.GL_PREVIOUS);
            gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_COMBINE_RGB, GL.GL_REPLACE);
        }
        else
        {
            gl.glEnable(GL.GL_TEXTURE_2D);
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private void endDrawWindows(DrawContext dc)
    {
        if (dc.isPickingMode())
        {
            this.pickSupport.endPicking(dc);
        }

        GL gl = dc.getGL();
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();

        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPopMatrix();

        gl.glPopAttrib();
    }

    private Point drawWindow(DrawContext dc, OrderedWindow uWindow)
    {
        if (uWindow.point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(msg);
            return null;
        }

        AnnotationWindow window = uWindow.window;
        final Point screenPoint = window.getPoint();
      
        javax.media.opengl.GL gl = dc.getGL();
        if (dc.isPickingMode())
        {
            java.awt.Color color = dc.getUniquePickColor();
            int colorCode = color.getRGB();
            this.pickSupport.addPickableObject(colorCode, window, uWindow.getPosition(), false);
            gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
        }

        window.render(dc);

        return screenPoint;
    }

    @Override
    public String toString()
    {
        return "AnnotationWindowLayer.Name";
    }
}
