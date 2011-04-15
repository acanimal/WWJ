package gov.nasa.worldwind.layers.Earth;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.pick.PickSupport;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.sun.opengl.util.j2d.TextRenderer;

import javax.media.opengl.GL;

/** Displays a terrain profile graph
 * @author Patrick Murris
 * @version $Id$
 * Usage: do setEventSource(wwd) to have the graph activated and updated with position changes
 * See public properties for options: keepProportions, followCursor...
 */
public class TerrainProfileLayer extends AbstractLayer implements PositionListener
{

    // Positionning constants TODO: add north, east... center-north... center-screen.
    public final static String NORTHWEST = "gov.nasa.worldwind.TerrainProfileLayer.NorthWest";
    public final static String SOUTHWEST = "gov.nasa.worldwind.TerrainProfileLayer.SouthWest";
    public final static String NORTHEAST = "gov.nasa.worldwind.TerrainProfileLayer.NorthEast";
    public final static String SOUTHEAST = "gov.nasa.worldwind.TerrainProfileLayer.SouthEast";
    // Stretching behavior constants
    public final static String RESIZE_STRETCH = "gov.nasa.worldwind.TerrainProfileLayer.Stretch";
    public final static String RESIZE_SHRINK_ONLY = "gov.nasa.worldwind.TerrainProfileLayer.ShrinkOnly";
    public final static String RESIZE_KEEP_FIXED_SIZE = "gov.nasa.worldwind.TerrainProfileLayer.FixedSize";
    // Units constants
    public final static String UNIT_METRIC = "gov.nasa.worldwind.TerrainProfileLayer.Metric";
    public final static String UNIT_IMPERIAL = "gov.nasa.worldwind.TerrainProfileLayer.Imperial";
    public final static double METER_TO_FEET = 3.280839895;
    // Follow constants
    public final static String FOLLOW_VIEW = "gov.nasa.worldwind.TerrainProfileLayer.FollowView";
    public final static String FOLLOW_EYE = "gov.nasa.worldwind.TerrainProfileLayer.FollowEye";
    public final static String FOLLOW_CURSOR = "gov.nasa.worldwind.TerrainProfileLayer.FollowCursor";
    public final static String FOLLOW_NONE = "gov.nasa.worldwind.TerrainProfileLayer.FollowNone";

    // Display parameters - TODO: make configurable
    private Dimension size = new Dimension(250, 100);
    private Color color = Color.white;
    private int borderWidth = 20;
    private String position = SOUTHWEST;
    private String resizeBehavior = RESIZE_SHRINK_ONLY;
    private String unit = UNIT_METRIC;
    private Font defaultFont = Font.decode("Arial-12-PLAIN");
    private double toViewportScale = 1;
    private Point locationCenter = null;
    private TextRenderer textRenderer = null;
    private PickSupport pickSupport = new PickSupport();
    private int pickedSample = -1;           // Picked sample number if not -1
    Polyline selectionShape;                 // Shape showing section on the ground
    Polyline pickedShape;                    // Shape showing actual pick position on the ground
    private boolean keepProportions = false; // Keep graph distance/elevation proportions
    private String follow = FOLLOW_VIEW;     // Profile position follow behavior
    private boolean showEyePosition = false;  // When FOLLOW_EYE, draw the eye position on graph when true
    private double profileLengthFactor = 1;  // Applied to default profile length (zoom on profile)

    // Terrain profile data
    private int samples = 250;              // Number of position samples
    private double minElevation;            // Minimum elevation along the profile
    private double maxElevation;            // Maximum elevation along the profile
    private double length;                  // Profile length along great circle in meter
    private Position positions[];           // Position list

    // Worldwind
    private WorldWindowGLCanvas wwd;

    // Draw it as ordered with an eye distance of 0 so that it shows up in front of most other things.
    // TODO: Add general support for this common pattern.
    private OrderedIcon orderedImage = new OrderedIcon();

    private class OrderedIcon implements OrderedRenderable
    {

        public double getDistanceFromEye()
        {
            return 0;
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
            TerrainProfileLayer.this.drawProfile(dc);
        }

        public void render(DrawContext dc)
        {
            TerrainProfileLayer.this.drawProfile(dc);
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
     * Renders a terrain profile graphic in a screen corner
     */
    public TerrainProfileLayer()
    {
        this.setName("Terrain profile graph");
    //this.setName(Logging.getMessage("layers.Earth.TerrainProfileLayer.Name"));
    }

    // ** Public properties ************************************************************
    /**
     * Get the graphic Dimension (in pixels)
     * @return the scalebar graphic Dimension
     */
    public Dimension getSize()
    {
        return this.size;
    }

    /**
     * Set the graphic Dimenion (in pixels)
     * @param size the graphic Dimension
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
     * Get the graphic color
     * @return  the graphic Color
     */
    public Color getColor()
    {
        return this.color;
    }

    /**
     * Set the graphic Color
     * @param color the graphic Color
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

    /** Returns the graphic-to-viewport scale factor.
     *
     * @return the graphic-to-viewport scale factor
     */
    public double getToViewportScale()
    {
        return toViewportScale;
    }

    /**
     * Sets the scale factor applied to the viewport size to determine the displayed size of the graphic. This
     * scale factor is used only when the layer's resize behavior is {@link #RESIZE_STRETCH} or {@link
     * #RESIZE_SHRINK_ONLY}. The graphic's width is adjusted to occupy the proportion of the viewport's width indicated by
     * this factor. The graphic's height is adjusted to maintain the graphic's Dimension aspect ratio.
     *
     * @param toViewportScale the graphic to viewport scale factor
     */
    public void setToViewportScale(double toViewportScale)
    {
        this.toViewportScale = toViewportScale;
    }

    public String getPosition()
    {
        return this.position;
    }

    /**
     * Sets the relative viewport location to display the graphic. Can be one of {@link #NORTHEAST} (the default),
     * {@link #NORTHWEST}, {@link #SOUTHEAST}, or {@link #SOUTHWEST}. These indicate the corner of the viewport.
     *
     * @param position the desired graphic position
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

    /**
     * Get the screen location of the graph center if set (can be null)
     * @return the screen location of the graph center if set (can be null)
     */
    public Point getLocationCenter()
    {
        return this.locationCenter;
    }

    /**
     * Set the screen location of the graph center - overrides SetPosition if not null
     * @param point the screen location of the graph center (can be null)
     */
    public void setLocationCenter(Point point)
    {
        this.locationCenter = point;
    }

    /**
     * Returns the layer's resize behavior.
     *
     * @return the layer's resize behavior
     */
    public String getResizeBehavior()
    {
        return resizeBehavior;
    }

    /**
     * Sets the behavior the layer uses to size the graphic when the viewport size changes, typically when the
     * World Wind window is resized. If the value is {@link #RESIZE_KEEP_FIXED_SIZE}, the graphic size is kept to the size
     * specified in its Dimension scaled by the layer's current icon scale. If the value is {@link #RESIZE_STRETCH},
     * the graphic is resized to have a constant size relative to the current viewport size. If the viewport shrinks the
     * graphic size decreases; if it expands then the graphic enlarges. If the value is
     * {@link #RESIZE_SHRINK_ONLY} (the default), graphic sizing behaves as for {@link #RESIZE_STRETCH} but it will
     * not grow larger than the size specified in its Dimension.
     *
     * @param resizeBehavior the desired resize behavior
     */
    public void setResizeBehavior(String resizeBehavior)
    {
        this.resizeBehavior = resizeBehavior;
    }

    public int getBorderWidth()
    {
        return borderWidth;
    }

    /**
     * Sets the graphic offset from the viewport border.
     *
     * @param borderWidth the number of pixels to offset the graphic from the borders indicated by {@link
     * #setPosition(String)}.
     */
    public void setBorderWidth(int borderWidth)
    {
        this.borderWidth = borderWidth;
    }

    public String getUnit()
    {
        return this.unit;
    }

    /**
     * Sets the unit the graphic uses to display distances and elevations.
     * Can be one of {@link #UNIT_METRIC} (the default),
     * or {@link #UNIT_IMPERIAL}.
     *
     * @param unit the desired unit
     */
    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    /**
     * Get the graphic legend Font
     * @return the graphic legend Font
     */
    public Font getFont()
    {
        return this.defaultFont;
    }

    /**
     * Set the graphic legend Font
     * @param font the graphic legend Font
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

    /**
     * Get whether distance/elevation proportions are maintained
     * @return  true if the graph maintains distance/elevation proportions
     */
    public boolean getKeepProportions()
    {
        return this.keepProportions;
    }

    /**
     * Set whether distance/elevation proportions are maintained
     * @param state true if the graph should maintains distance/elevation proportions
     */
    public void setKeepProportions(boolean state)
    {
        this.keepProportions = state;
    }

    /**
     * Get whether the graph center point follows the mouse cursor
     * @return true if the graph center point follows the mouse cursor
     */
    public boolean getFollowCursor()
    {
        return this.follow.compareToIgnoreCase(FOLLOW_CURSOR) == 0;
    }

    /**
     * Set whether the graph center point should follows the mouse cursor
     * @param state true if the graph center point should follows the mouse cursor
     */
    public void setFollowCursor(boolean state)
    {
        this.follow = state ? FOLLOW_CURSOR : FOLLOW_VIEW;
    }

    /**
     * Get the graph center point placement behavior
     * @return the graph center point placement behavior
     */
    public String getFollow()
    {
        return this.follow;
    }

    /**
     * Set the graph center point placement behavior
     * @param behavior the graph center point placement behavior
     */
    public void setFollow(String behavior)
    {
        this.follow = behavior;
    }

    /**
     * Get whether the eye position is shown on the graph
     * @return true if the eye position is shown on the grap
     */
    public boolean getShowEyePosition()
    {
        return this.showEyePosition;
    }

    /**
     * Set whether the eye position is shown on the graph
     * @param state if true the eye position is shown on the graph
     */
    public void setShowEyePosition(Boolean state)
    {
        this.showEyePosition = state;
    }

    /**
     * Set the profile length factor
     * @param factor the new factor
     */
    public void setProfileLengthFactor(double factor)
    {
        this.profileLengthFactor = factor;
    }

    /**
     * Get the profile length factor
     * @return the profile length factor
     */
    public double getProfileLenghtFactor()
    {
        return this.profileLengthFactor;
    }

    // ** Rendering ************************************************************
    @Override
    public void doRender(DrawContext dc)
    {
        // Delegate graph rendering to OrderedRenderable list
        dc.addOrderedRenderable(this.orderedImage);
        // Render line on ground now
        if (this.positions != null && this.selectionShape != null)
        {
            this.selectionShape.render(dc);
            if (this.pickedSample != -1)
            {
                this.pickedShape.render(dc);
            }
        }
    }

    @Override
    public void doPick(DrawContext dc, Point pickPoint)
    {
        // No picking at the graph if selection follows the mouse cursor
        if (this.follow.compareToIgnoreCase(FOLLOW_CURSOR) != 0)
        // Delegate drawing to the ordered renderable list
        {
            dc.addOrderedRenderable(this.orderedImage);
        }
    }

    // Profile graph rendering - ortho
    public void drawProfile(DrawContext dc)
    {
        if (this.positions == null || (this.minElevation == 0 && this.maxElevation == 0))
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

            gl.glDisable(GL.GL_TEXTURE_2D);		// no textures

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
            Vec4 locationSW = this.computeLocation(viewport, scale);
            gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
            gl.glScaled(scale, scale, 1d);

            if (!dc.isPickingMode())
            {
                // Draw grid - Set color using current layer opacity
                this.drawGrid(dc, this.size);

                // Draw profile graph
                this.drawGraph(dc, this.size);

                // Draw labels
                String label = String.format("min %.0fm   max %.0fm", this.minElevation, this.maxElevation);
                if (this.unit.equals(UNIT_IMPERIAL))
                {
                    label = String.format("min %.0fft   max %.0fft", this.minElevation * METER_TO_FEET, this.maxElevation * 3.280839895);
                }
                gl.glLoadIdentity();
                gl.glDisable(GL.GL_CULL_FACE);
                drawLabel(label, locationSW.add3(new Vec4(0, -12, 0)), -1); // left aligned
                if (this.pickedSample != -1)
                {
                    double pickedElevation = positions[this.pickedSample].getElevation();
                    label = String.format("%.0fm", pickedElevation);
                    if (this.unit.equals(UNIT_IMPERIAL))
                    {
                        label = String.format("%.0fft", pickedElevation * METER_TO_FEET);
                    }
                    drawLabel(label, locationSW.add3(new Vec4(width, -12, 0)), 1); // right aligned
                }
            }
            else
            {
                // Picking
                this.pickSupport.clearPickList();
                this.pickSupport.beginPicking(dc);
                // Where in the world are we picking ?
                Position pickPosition =
                        computePickPosition(dc, locationSW, new Dimension((int) (width * scale), (int) (height * scale)));
                // Draw unique color across the rectangle
                Color color = dc.getUniquePickColor();
                int colorCode = color.getRGB();
                // Add our object(s) to the pickable list
                this.pickSupport.addPickableObject(colorCode, this, pickPosition, false);
                gl.glScaled(width, height, 1d);
                gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                gl.glBegin(GL.GL_POLYGON);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(1, 0, 0);
                gl.glVertex3d(1, 1, 0);
                gl.glVertex3d(0, 1, 0);
                gl.glVertex3d(0, 0, 0);
                gl.glEnd();
                // Done picking
                this.pickSupport.endPicking(dc);
                this.pickSupport.resolvePick(dc, dc.getPickPoint(), this);
            }

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

    // Draw grid graphic
    private void drawGrid(DrawContext dc, Dimension dimension)
    {
        // Background color
        Color backColor = getBackgroundColor(this.color);
        drawFilledRectangle(dc, new Vec4(0, 0, 0), dimension, new Color(backColor.getRed(),
                backColor.getGreen(), backColor.getBlue(), (int) (backColor.getAlpha() * .5))); // Increased transparency
        // Grid - minimal
        float[] colorRGB = this.color.getRGBColorComponents(null);
        dc.getGL().glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], this.getOpacity());
        drawVerticalLine(dc, dimension, 0);
        drawVerticalLine(dc, dimension, dimension.getWidth());
        drawHorizontalLine(dc, dimension, 0);
    }

    // Draw profile graphic
    private void drawGraph(DrawContext dc, Dimension dimension)
    {
        GL gl = dc.getGL();
        // Adjust min/max elevation for the graph
        double min = this.minElevation;
        double max = this.maxElevation;
        if (this.follow.compareToIgnoreCase(FOLLOW_EYE) == 0 && this.showEyePosition)
        {
            max = Math.max(max, dc.getView().getEyePosition().getElevation());
        }
        if (min > 0)
        {
            min = 0;
        }
        if (max < 0)
        {
            max = 0;
        }
        int i;
        double stepX = dimension.getWidth() / (this.length);
        double stepY = dimension.getHeight() / (max - min);
        if (this.keepProportions)
        {
            stepX = Math.min(stepX, stepY);
            stepY = stepX;
        }
        double lengthStep = this.length / (this.samples - 1);
        double x = 0, y = 0;
        // Filled graph
        gl.glColor4ub((byte) this.color.getRed(), (byte) this.color.getGreen(),
                (byte) this.color.getBlue(), (byte) 100);
        gl.glBegin(GL.GL_TRIANGLE_STRIP);
        for (i = 0; i < this.samples; i++)
        {
            x = i * lengthStep * stepX;
            y = (this.positions[i].getElevation() - min) * stepY;
            gl.glVertex3d(x, 0, 0);
            gl.glVertex3d(x, y, 0);
        }
        gl.glEnd();
        // Line graph
        float[] colorRGB = this.color.getRGBColorComponents(null);
        gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], this.getOpacity());
        gl.glBegin(GL.GL_LINE_STRIP);
        for (i = 0; i < this.samples; i++)
        {
            x = i * lengthStep * stepX;
            y = (this.positions[i].getElevation() - min) * stepY;
            gl.glVertex3d(x, y, 0);
        }
        gl.glEnd();
        // Middle vertical line
        gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], this.getOpacity() * .3); // increased transparency here
        drawVerticalLine(dc, dimension, x / 2);
        // Eye position
        if (this.follow.compareToIgnoreCase(FOLLOW_EYE) == 0 && this.showEyePosition)
        {
            double eyeY = (dc.getView().getEyePosition().getElevation() - min) * stepY;
            this.drawFilledRectangle(dc, new Vec4(x / 2 - 2, eyeY - 2, 0), new Dimension(5, 5), this.color);
        }
        // Selected/picked vertical and horizontal lines
        if (this.pickedSample != -1)
        {
            double pickedX = this.pickedSample * lengthStep * stepX;
            double pickedY = (positions[this.pickedSample].getElevation() - min) * stepY;
            gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2] * .5, this.getOpacity() * .8); // yellower color
            drawVerticalLine(dc, dimension, pickedX);
            drawHorizontalLine(dc, dimension, pickedY);
            // Eye - picked position line
            if (this.follow.compareToIgnoreCase(FOLLOW_EYE) == 0 && this.showEyePosition)
            {
                // Line
                double eyeY = (dc.getView().getEyePosition().getElevation() - min) * stepY;
                drawLine(dc, pickedX, pickedY, x / 2, eyeY);
                // Distance label
                double distance = dc.getView().getEyePoint().distanceTo3(dc.getGlobe().computePointFromPosition(positions[this.pickedSample]));
                String label = String.format("Dist %.0fm", distance);
                if (this.unit.equals(UNIT_IMPERIAL))
                {
                    label = String.format("Dist %.0fft", distance * METER_TO_FEET);
                }
                drawLabel(label, new Vec4(pickedX + 5, pickedY - 12, 0), -1); // left aligned
            }
        }
        // Min elevation horizontal line
        if (this.minElevation != min)
        {
            y = (this.minElevation - min) * stepY;
            gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], this.getOpacity() * .5);  // medium transparency
            drawHorizontalLine(dc, dimension, y);
        }
        // Max elevation horizontal line
        if (this.maxElevation != max)
        {
            y = (this.maxElevation - min) * stepY;
            gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], this.getOpacity() * .5);  // medium transparency
            drawHorizontalLine(dc, dimension, y);
        }
        // Sea level in between positive elevations only (not across land)
        if (min < 0 && max >= 0)
        {
            gl.glColor4d(colorRGB[0] * .7, colorRGB[1] * .7, colorRGB[2], this.getOpacity() * .5); // bluer color
            y = -this.minElevation * stepY;
            double previousX = -1;
            for (i = 0; i < this.samples; i++)
            {
                x = i * lengthStep * stepX;
                if (this.positions[i].getElevation() > 0 || i == this.samples - 1)
                {
                    if (previousX >= 0)
                    {
                        gl.glBegin(GL.GL_LINE_STRIP);
                        gl.glVertex3d(previousX, y, 0);
                        gl.glVertex3d(x, y, 0);
                        gl.glEnd();
                        previousX = -1;
                    }
                }
                else
                {
                    previousX = previousX < 0 ? x : previousX;
                }
            }
        }
    }

    private void drawHorizontalLine(DrawContext dc, Dimension dimension, double y)
    {
        drawLine(dc, 0, y, dimension.getWidth(), y);
    }

    private void drawVerticalLine(DrawContext dc, Dimension dimension, double x)
    {
        drawLine(dc, x, 0, x, dimension.getHeight());
    }

    private void drawFilledRectangle(DrawContext dc, Vec4 origin, Dimension dimension, Color color)
    {
        GL gl = dc.getGL();
        gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(),
                (byte) color.getBlue(), (byte) color.getAlpha());
        gl.glDisable(GL.GL_TEXTURE_2D);        // no textures
        gl.glBegin(GL.GL_POLYGON);
        gl.glVertex3d(origin.x, origin.y, 0);
        gl.glVertex3d(origin.x + dimension.getWidth(), origin.y, 0);
        gl.glVertex3d(origin.x + dimension.getWidth(), origin.y + dimension.getHeight(), 0);
        gl.glVertex3d(origin.x, origin.y + dimension.getHeight(), 0);
        gl.glVertex3d(origin.x, origin.y, 0);
        gl.glEnd();

    }

    private void drawLine(DrawContext dc, double x1, double y1, double x2, double y2)
    {
        GL gl = dc.getGL();
        gl.glBegin(GL.GL_LINE_STRIP);
        gl.glVertex3d(x1, y1, 0);
        gl.glVertex3d(x2, y2, 0);
        gl.glEnd();
    }

    // Draw a text label
    // Align = -1: left, 0: center and 1: right
    private void drawLabel(String text, Vec4 screenPoint, int align)
    {
        if (this.textRenderer == null)
        {
            this.textRenderer = new TextRenderer(this.defaultFont, true, true);
        }

        Rectangle2D nameBound = this.textRenderer.getBounds(text);
        int x = (int) screenPoint.x();  // left
        if (align == 0)
        {
            x = (int) (screenPoint.x() - nameBound.getWidth() / 2d);
        }  // centered
        if (align > 0)
        {
            x = (int) (screenPoint.x() - nameBound.getWidth());
        }  // right
        int y = (int) screenPoint.y();

        this.textRenderer.begin3DRendering();

        this.textRenderer.setColor(this.getBackgroundColor(this.color));
        this.textRenderer.draw(text, x + 1, y - 1);
        this.textRenderer.setColor(this.color);
        this.textRenderer.draw(text, x, y);

        this.textRenderer.end3DRendering();

    }


    // Compute background color for best contrast
    private Color getBackgroundColor(Color color)
    {
        float[] compArray = new float[4];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), compArray);
        if (compArray[2] > 0.5)
        {
            return new Color(0, 0, 0, (int) (this.color.getAlpha() * 0.7f));
        }
        else
        {
            return new Color(255, 255, 255, (int) (this.color.getAlpha() * 0.7f));
        }
    }

    // ** Dimensions and positionning ************************************************************
    private double computeScale(java.awt.Rectangle viewport)
    {
        if (this.resizeBehavior.equals(RESIZE_SHRINK_ONLY))
        {
            return Math.min(1d, (this.toViewportScale) * viewport.width / this.size.width);
        }
        else if (this.resizeBehavior.equals(RESIZE_STRETCH))
        {
            return (this.toViewportScale) * viewport.width / this.size.width;
        }
        else if (this.resizeBehavior.equals(RESIZE_KEEP_FIXED_SIZE))
        {
            return 1d;
        }
        else
        {
            return 1d;
        }
    }

    private Vec4 computeLocation(java.awt.Rectangle viewport, double scale)
    {
        double scaledWidth = scale * this.size.width;
        double scaledHeight = scale * this.size.height;

        double x;
        double y;

        if (this.locationCenter != null)
        {
            x = this.locationCenter.x - scaledWidth / 2;
            y = this.locationCenter.y - scaledHeight / 2;
        }
        else if (this.position.equals(NORTHEAST))
        {
            x = viewport.getWidth() - scaledWidth - this.borderWidth;
            y = viewport.getHeight() - scaledHeight - this.borderWidth;
        }
        else if (this.position.equals(SOUTHEAST))
        {
            x = viewport.getWidth() - scaledWidth - this.borderWidth;
            y = 0d + this.borderWidth;
        }
        else if (this.position.equals(NORTHWEST))
        {
            x = 0d + this.borderWidth;
            y = viewport.getHeight() - scaledHeight - this.borderWidth;
        }
        else if (this.position.equals(SOUTHWEST))
        {
            x = 0d + this.borderWidth;
            y = 0d + this.borderWidth;
        }
        else // use North East
        {
            x = viewport.getWidth() - scaledWidth / 2 - this.borderWidth;
            y = viewport.getHeight() - scaledHeight / 2 - this.borderWidth;
        }

        return new Vec4(x, y, 0);
    }

    private Position computeGroundPosition(DrawContext dc, View view)
    {
        if (view == null)
        {
            return null;
        }

        Position groundPos = view.computePositionFromScreenPoint(
                view.getViewport().getWidth() / 2, view.getViewport().getHeight() / 2);
        if (groundPos == null)
        {
            return null;
        }

        double elevation = dc.getGlobe().getElevation(groundPos.getLatitude(), groundPos.getLongitude());
        return new Position(
                groundPos.getLatitude(),
                groundPos.getLongitude(),
                elevation * dc.getVerticalExaggeration());
    }

    /**
     * Computes the Position of the pickPoint over the graph and updates pickedSample indice
     *
     * @param dc         the current DrawContext
     * @param locationSW the screen location of the bottom left corner of the graph
     * @param mapSize    the graph screen dimension in pixels
     * @return the picked Position
     */
    private Position computePickPosition(DrawContext dc, Vec4 locationSW, Dimension mapSize)
    {
        Position pickPosition = null;
        this.pickedSample = -1;
        Point pickPoint = dc.getPickPoint();
        if (pickPoint != null && this.positions != null)
        {
            Rectangle viewport = dc.getView().getViewport();
            // Check if pickpoint is inside the graph
            if (pickPoint.getX() >= locationSW.getX() && pickPoint.getX() < locationSW.getX() + mapSize.width && viewport.height - pickPoint.getY() >= locationSW.getY() && viewport.height - pickPoint.getY() < locationSW.getY() + mapSize.height)
            {
                // Find sample - Note: only works when graph expends over the full width
                int sample = (int) (((double) (pickPoint.getX() - locationSW.getX()) / mapSize.width) * this.samples);
                if (sample >= 0 && sample < this.samples)
                {
                    pickPosition = this.positions[sample];
                    this.pickedSample = sample;
                    // Update polyline indicator
                    ArrayList<Position> posList = new ArrayList<Position>();
                    posList.add(positions[sample]);
                    posList.add(new Position(positions[sample].getLatitude(), positions[sample].getLongitude(),
                            positions[sample].getElevation() + this.length / 10));
                    if (this.pickedShape == null)
                    {
                        this.pickedShape = new Polyline(posList);
                        this.pickedShape.setPathType(Polyline.LINEAR);
                        this.pickedShape.setLineWidth(2);
                        this.pickedShape.setColor(new Color((int) (this.color.getRed() * .5),
                                (int) (this.color.getGreen() * .5), (int) (this.color.getBlue() * .3), (int) (255 * .8)));
                    }
                    else
                    {
                        this.pickedShape.setPositions(posList);
                    }
                }
            }
        }
        return pickPosition;
    }

    // ** Position listener impl. ************************************************************
    public void moved(PositionEvent event)
    {
        if (this.wwd != null && this.isEnabled())
        {
            computeProfile();
        }
        else
        {
            this.positions = null;
        }
    }

    // Sets the wwd local reference and add us to the position listeners
    public void setEventSource(WorldWindowGLCanvas wwd)
    {
        if (this.wwd != null)
        {
            this.wwd.removePositionListener(this);
        }
        this.wwd = wwd;
        if (this.wwd != null)
        {
            this.wwd.addPositionListener(this);
        }
    }

    // ** Profile data collection ************************************************************

    // Collects terrain profile data along a great circle line centered
    // at the current position (view or cursor) and perpendicular to the view heading
    private void computeProfile()
    {
        // Find center position
        OrbitView view = (OrbitView) this.wwd.getView(); // TODO: check for OrbitView instance first
        //Position groundPos = view.computePositionFromScreenPoint(
        //        view.getViewport().getWidth() / 2, view.getViewport().getHeight() / 2);
        Position groundPos = new Position(view.getLookAtLatitude(), view.getLookAtLongitude(), 0);
        if (this.follow.compareToIgnoreCase(FOLLOW_CURSOR) == 0)
        {
            groundPos = this.wwd.getCurrentPosition();
        }
        if (this.follow.compareToIgnoreCase(FOLLOW_EYE) == 0)
        {
            groundPos = view.getEyePosition();
        }
        if (groundPos != null)
        {
            this.positions = new Position[samples];
            this.minElevation = Double.MAX_VALUE;
            this.maxElevation = -1e6;  // Note: Double.MIN_VALUE would fail below tests for some reason
            this.length = Math.min(view.getZoom() * .8 * this.profileLengthFactor,
                    this.wwd.getModel().getGlobe().getMaximumRadius() * Math.PI);
            double lengthRadian = this.length / this.wwd.getModel().getGlobe().getMaximumRadius();

            LatLon centerLatLon = new LatLon(groundPos.getLatitude(), groundPos.getLongitude());
            // Iterate on both sides of the center point
            int i;
            double step = lengthRadian / (samples - 1);
            for (i = 0; i < this.samples; i++)
            {
                double azimuth = view.getHeading().subtract(Angle.POS90).radians;
                if (i > (float) (this.samples - 1) / 2f)
                {
                    azimuth = view.getHeading().subtract(Angle.NEG90).radians;
                }
                double distance = Math.abs(((double) i - ((double) (this.samples - 1) / 2d)) * step);
                // Sample lat/lon and elevation
                LatLon latLon = LatLon.endPosition(centerLatLon, azimuth, distance);
                Double elevation = this.wwd.getModel().getGlobe().getElevation(latLon.getLatitude(), latLon.getLongitude());
                this.minElevation = elevation < this.minElevation ? elevation : this.minElevation;
                this.maxElevation = elevation > this.maxElevation ? elevation : this.maxElevation;
                // Add position to the list
                positions[i] = new Position(latLon.getLatitude(), latLon.getLongitude(), elevation);
            }
            // Update shape on ground
            if (this.selectionShape == null)
            {
                this.selectionShape = new Polyline(Arrays.asList(this.positions));
                this.selectionShape.setLineWidth(2);
                this.selectionShape.setFollowTerrain(true);
                this.selectionShape.setColor(new Color(this.color.getRed(),
                        this.color.getGreen(), (int) (this.color.getBlue() * .5), (int) (255 * .8)));
            }
            else
            {
                this.selectionShape.setPositions(Arrays.asList(this.positions));
            }
        }
        else
        {
            // Off globe or something
            this.positions = null;
        }
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
