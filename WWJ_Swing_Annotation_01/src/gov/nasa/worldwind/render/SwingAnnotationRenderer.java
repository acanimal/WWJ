package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Locatable;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.Logging;

import java.awt.Point;
import java.util.Iterator;
import javax.media.opengl.GL;
import java.util.logging.Level;

/**
 * SwingAnnotationRenderer is responsible to render the SwingAnnotations.
 *
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class SwingAnnotationRenderer
{

    private static boolean isAnnotationValid(SwingAnnotation annotation, boolean checkPosition)
    {
        //noinspection RedundantIfStatement
        if (checkPosition && annotation.isEnable() && annotation.getPosition() == null)
        {
            return false;
        }

        return true;
    }

    public void render(DrawContext dc, Iterable<SwingAnnotation> annotations)
    {
        this.drawMany(dc, annotations);
    }

    private void drawMany(DrawContext dc, Iterable<SwingAnnotation> annotations)
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

        if (annotations == null)
        {
            String msg = "SwingAnnotation iterator is null";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        Iterator<SwingAnnotation> iterator = annotations.iterator();
        if (!iterator.hasNext())
        {
            return;
        }

        while (iterator.hasNext())
        {
            SwingAnnotation annotation = iterator.next();
            if (!isAnnotationValid(annotation, true))
            {
                continue;
            }

            Point point = annotation.getPoint();

            dc.addOrderedRenderable(new OrderedAnnotation(annotation, point, 1));
        }
    }

    private void beginDrawAnnotations(DrawContext dc)
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

        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void endDrawAnnotations(DrawContext dc)
    {

        GL gl = dc.getGL();
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();

        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPopMatrix();

        gl.glPopAttrib();
    }

    private Point drawAnnotation(DrawContext dc, OrderedAnnotation uAnnotation)
    {
        if (uAnnotation.point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(msg);
            return null;
        }

        SwingAnnotation annotation = uAnnotation.getAnnotation();
        javax.media.opengl.GL gl = dc.getGL();
        annotation.render(dc);

        final Point screenPoint = annotation.getPoint();

        return screenPoint;
    }

    private class OrderedAnnotation implements OrderedRenderable, Locatable
    {

        private SwingAnnotation annotation;
        Point point;
        double eyeDistance;
        java.awt.Point pickPoint;
        Layer layer;

        OrderedAnnotation(SwingAnnotation annotation, Point point, double eyeDistance)
        {
            this.annotation = annotation;
            this.point = point;
            this.eyeDistance = eyeDistance;
        }

        OrderedAnnotation(SwingAnnotation annotation, Point point, java.awt.Point pickPoint, Layer layer, double eyeDistance)
        {
            this.annotation = annotation;
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
            return this.annotation.getPosition();
        }

        public void render(DrawContext dc)
        {
            SwingAnnotationRenderer.this.beginDrawAnnotations(dc);

            try
            {
                SwingAnnotationRenderer.this.drawAnnotation(dc, this);
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
                SwingAnnotationRenderer.this.endDrawAnnotations(dc);
            }
        }

        public void pick(DrawContext dc, java.awt.Point pickPoint)
        {
        }

        public SwingAnnotation getAnnotation()
        {
            return annotation;
        }
    }
}
