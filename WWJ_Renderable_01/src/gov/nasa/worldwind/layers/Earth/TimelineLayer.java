package gov.nasa.worldwind.layers.Earth;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;
import javax.media.opengl.GL;
import java.awt.*;
import java.awt.geom.*;

/**
 * Renders a timeline bar graphic in the screen.
 * Derived from ScalebarLayer (thanks to Patrick).
 *
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class TimelineLayer extends RenderableLayer
{

    // Positionning constants
    public static final String NORTH = "TimelineLayer.North";
    public static final String SOUTH = "TimelineLayer.South";
    // Display parameters - TODO: make configurable
    private Dimension size = new Dimension(300, 10);
    private Color color = Color.white;
    private Color cursorColor = Color.green;
    private int borderWidth = 20;
    private String position = NORTH;
    private Font defaultFont = Font.decode("Arial-12-PLAIN");
    private TextRenderer textRenderer = null;
    // Draw it as ordered with an eye distance of 0 so that it shows up in front of most other things.
    private OrderedIcon orderedImage = new OrderedIcon();

    private class OrderedIcon implements OrderedRenderable
    {

        public double getDistanceFromEye()
        {
            return 0;
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
        }

        public void render(DrawContext dc)
        {
            TimelineLayer.this.draw(dc);
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

    /**
     * Creates a new instance.
     */
    public TimelineLayer()
    {
        this.setName(Logging.getMessage("layers.Earth.TimelineLayer.Name"));
    }

    /**
     * Get the timeline graphic Dimension (in pixels)
     * @return the timeline graphic Dimension
     */
    public Dimension getSize()
    {
        return this.size;
    }

    /**
     * Set the timeline graphic Dimenion (in pixels)
     * @param size the timeline graphic Dimension
     */
    public void setSize(Dimension size)
    {
        if (size == null)
        {
            String message = Logging.getMessage("nullValue.DimensionIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.size = size;
    }

    /**
     * Get the timeline color
     * @return  the timeline Color
     */
    public Color getColor()
    {
        return this.color;
    }

    /**
     * Set the timeline Color
     * @param color the timeline Color
     */
    public void setColor(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.color = color;
    }

    public String getPosition()
    {
        return this.position;
    }

    /**
     * Sets the relative viewport location to display the timeline. Can be one of {@link #NORTH} 
     * (the default) or {@link #SOUTH}
     *
     * @param position 
     */
    public void setPosition(String position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.position = position;
    }

    public int getBorderWidth()
    {
        return borderWidth;
    }

    /**
     * Sets the timeline offset from the viewport border.
     *
     * @param borderWidth the number of pixels to offset the timeline from the borders indicated by {@link
     * #setPosition(String)}.
     */
    public void setBorderWidth(int borderWidth)
    {
        this.borderWidth = borderWidth;
    }

    /**
     * Get the timeline legend Fon
     * @return the timeline legend Font
     */
    public Font getFont()
    {
        return this.defaultFont;
    }

    /**
     * Set the timeline legend Fon
     * @param font the timeline legend Font
     */
    public void setFont(Font font)
    {
        if (font == null)
        {
            String msg = Logging.getMessage("nullValue.FontIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.defaultFont = font;
    }

    // Rendering
    @Override
    public void doRender(DrawContext dc)
    {
        dc.addOrderedRenderable(this.orderedImage);
    }

    // Rendering
    public void draw(DrawContext dc)
    {
        // Check if the view has a temporality associated
        if (dc.getView().getTemporality() == null)
        {
            return;
        }

        GL gl = dc.getGL();

        boolean attribsPushed = false;
        boolean modelviewPushed = false;
        boolean projectionPushed = false;

        try
        {
            gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT | GL.GL_ENABLE_BIT | GL.GL_TEXTURE_BIT | GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT | GL.GL_CURRENT_BIT);
            attribsPushed = true;

            gl.glDisable(GL.GL_TEXTURE_2D); // no textures
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glDisable(GL.GL_DEPTH_TEST);

            double width = this.size.width;
            double height = this.size.height;

            // Load a parallel projection with xy dimensions (viewportWidth, viewportHeight)
            // into the GL projection matrix.
            java.awt.Rectangle viewport = dc.getView().getViewport();
            gl.glMatrixMode(javax.media.opengl.GL.GL_PROJECTION);
            gl.glPushMatrix();
            projectionPushed = true;
            gl.glLoadIdentity();
            double maxwh = width > height ? width : height;
            gl.glOrtho(0d, viewport.width, 0d, viewport.height, -0.6 * maxwh, 0.6 * maxwh);

            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPushMatrix();
            modelviewPushed = true;
            gl.glLoadIdentity();

            // Scale to a width x height space
            // located at the proper position on screen
            double scale = this.computeScale(viewport);
            Vec4 locationSW = this.computeLocation(viewport);
            gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
            gl.glScaled(scale, scale, 1);

            // Draw cursor
            float[] colorRGB = this.cursorColor.getRGBColorComponents(null);
            gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], this.getOpacity() * 0.75);
            drawCursor(dc);
            // Draw time line
            // Set color using current layer opacity
            Color backColor = this.getBackgroundColor(this.color);
            colorRGB = backColor.getRGBColorComponents(null);
            gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], (double) backColor.getAlpha() / 255d * this.getOpacity());
            this.drawTimeLine(dc);

            colorRGB = this.color.getRGBColorComponents(null);
            gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], this.getOpacity());
            gl.glTranslated(-1d / scale, 1d / scale, 0d);
            this.drawTimeLine(dc);

            // Draw labels
            gl.glLoadIdentity();
            gl.glDisable(GL.GL_CULL_FACE);

            // Draw view temporality
            String start = dc.getView().getTemporality().getInitialTimeStamp().getTime().toString();
            String end = dc.getView().getTemporality().getFinalTimeStamp().getTime().toString();
            Vec4 startpos = new Vec4(locationSW.x(), locationSW.y() + height, locationSW.z());
            drawLabel(start, startpos);
            Vec4 endpos = new Vec4(locationSW.x() + width, locationSW.y() + height, locationSW.z());
            drawLabel(end, endpos);

            // Draw cursor label
            String cstart = dc.getView().getTemporalityCursor().getInitialTimeStamp().getTime().toString();
            String cend = dc.getView().getTemporalityCursor().getFinalTimeStamp().getTime().toString();
            String cursorstring = cstart + " - " + cend;
            Vec4 cpos = new Vec4(locationSW.x() + width / 2, locationSW.y(), locationSW.z());
            drawCursorLabel(cursorstring, cpos);
        }
        finally
        {
            if (projectionPushed)
            {
                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glPopMatrix();
            }
            if (modelviewPushed)
            {
                gl.glMatrixMode(GL.GL_MODELVIEW);
                gl.glPopMatrix();
            }
            if (attribsPushed)
            {
                gl.glPopAttrib();
            }
        }
    }

    private void drawTimeLine(DrawContext dc)
    {
        double width = this.size.width;
        double height = this.size.height;

        // Draw time line
        GL gl = dc.getGL();
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(0, height, 0);
        gl.glVertex3d(0, 0, 0);
        gl.glVertex3d(width, 0, 0);
        gl.glVertex3d(width, height, 0);
        gl.glEnd();
    }

    private void drawCursor(DrawContext dc)
    {
        double width = this.size.width;
        double height = this.size.height * 0.75;

        // Compute cursor position
        long ini = dc.getView().getTemporality().getInitialTimeStamp().getTimeInMillis();
        long fin = dc.getView().getTemporality().getFinalTimeStamp().getTimeInMillis();
        long ss = dc.getView().getTemporalityCursor().getInitialTimeStamp().getTimeInMillis();
        long se = dc.getView().getTemporalityCursor().getFinalTimeStamp().getTimeInMillis();

        int pss = (int) ((width) * (ss - ini) / (fin - ini));
        int pse = (int) ((width) * (se - ini) / (fin - ini));

        // Draw cursor
        GL gl = dc.getGL();
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex2d(pss, 0);
        gl.glVertex2d(pse, 0);
        gl.glVertex2d(pse, height);
        gl.glVertex2d(pss, height);
        gl.glEnd();
    }

    private void drawLabel(String text, Vec4 screenPoint)
    {
        if (this.textRenderer == null)
        {
            this.textRenderer = new TextRenderer(this.defaultFont, true, true);
        }

        Rectangle2D nameBound = this.textRenderer.getBounds(text);
        int x = (int) (screenPoint.x() - nameBound.getWidth() / 2d);
        int y = (int) screenPoint.y();

        this.textRenderer.begin3DRendering();

        this.textRenderer.setColor(this.getBackgroundColor(this.color));
        this.textRenderer.draw(text, x + 1, y - 1);
        this.textRenderer.setColor(this.color);
        this.textRenderer.draw(text, x, y);

        this.textRenderer.end3DRendering();
    }

    private void drawCursorLabel(String text, Vec4 screenPoint)
    {
        if (this.textRenderer == null)
        {
            this.textRenderer = new TextRenderer(this.defaultFont, true, true);
        }

        Rectangle2D nameBound = this.textRenderer.getBounds(text);
        int x = (int) (screenPoint.x() - nameBound.getWidth() / 2d);
        int y = (int) (screenPoint.y() - nameBound.getHeight());

        this.textRenderer.begin3DRendering();

        this.textRenderer.setColor(this.getBackgroundColor(this.color));
        this.textRenderer.draw(text, x + 1, y - 1);
        this.textRenderer.setColor(this.color);
        this.textRenderer.draw(text, x, y);

        this.textRenderer.end3DRendering();
    }
    private final float[] compArray = new float[4];

    // Compute background color for best contrast
    private Color getBackgroundColor(Color color)
    {
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), compArray);
        if (compArray[2] > 0.5)
        {
            return new Color(0, 0, 0, 0.7f);
        }
        else
        {
            return new Color(1, 1, 1, 0.7f);
        }
    }

    private double computeScale(java.awt.Rectangle viewport)
    {
        return Math.min(1d, viewport.width / this.size.width);
    }

    private Vec4 computeLocation(java.awt.Rectangle viewport)
    {
        // Initialize x,y with NORTH position.
        double x = (viewport.getWidth() - this.size.width) / 2 - this.borderWidth;
        double y = viewport.getHeight() - this.size.height - this.borderWidth;

        if (this.position.equals(SOUTH))
        {
            y = this.borderWidth;
        }

        return new Vec4(x, y, 0);
    }

    public void dispose()
    {
        if (this.textRenderer != null)
        {
            this.textRenderer.dispose();
            this.textRenderer = null;
        }
    }

    @Override
    public String toString()
    {
        return this.getName();
    }
}
