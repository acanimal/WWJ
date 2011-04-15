package gov.nasa.worldwind.render;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import javax.media.opengl.GL;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * <p>SwingAnnotation allows to create rich annotations using Swing components.</p>
 * <p>It is implemented using a JPanel that must be put in the same container as
 * the WorldWindowGLCanvas. <b>The parent container must use a null layout</b>.</p>
 *
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class SwingAnnotation extends JPanel
{

    private static final Dimension DEFAULT_SIZE = new Dimension(250, 100);
    private boolean enable = true;
    private JPanel panel = null;
    private WorldWindowGLCanvas wwd = null;
    private Point point = new Point(0, 0);
    private Position position = Position.fromDegrees(0, 0, 0);
    private Color color = Color.LIGHT_GRAY;
    private JScrollPane scroll = null;

    /**
     * Creates a new annotation at the given position.
     *
     * @param position
     */
    public SwingAnnotation(Position position)
    {
        this.position = position;

        // Initially the panel is hidden.
        this.setVisible(false);

        this.setLayout(new BorderLayout());
        this.setSize(DEFAULT_SIZE);

        // Set border
        this.setBorder(BorderFactory.createLineBorder(color, 2));
    }

    /**
     * Get the WorldWindowGLCanvas component.
     *
     * @return
     */
    public WorldWindowGLCanvas getWorldWindowGLCanvas()
    {
        return wwd;
    }

    /**
     * Sets a reference to the WorldWindowGLCanvas component on which the
     * annotation is drawn.
     *
     * @param wwd
     */
    public void setWorldWindowGLCanvas(WorldWindowGLCanvas wwd)
    {
        this.wwd = wwd;
    }

    /**
     * Returns the content panel.
     *
     * @return the content.
     */
    public JPanel getPanel()
    {
        return panel;
    }

    /**
     * Sets the content panel to be rendered.
     * @param panel
     */
    public void setPanel(JPanel panel)
    {
        if (panel == null)
        {
            return;
        }
        this.panel = panel;

        scroll = new JScrollPane(panel);
        this.add(scroll, BorderLayout.CENTER);
    }

    /**
     * Returns the position of the annotation.
     *
     * @return the position
     */
    public Position getPosition()
    {
        return position;
    }

    /**
     * Sets the annotation's position.
     * @param position
     */
    public void setPosition(Position position)
    {
        this.position = position;
    }

    /**
     * Check if the annotation is enable.
     *
     * @return true if enable, false otherwise.
     */
    public boolean isEnable()
    {
        return enable;
    }

    /**
     * Changes the status of the annotation.
     * @param enable
     */
    public void setEnable(boolean enable)
    {
        this.enable = enable;
    }

    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
    {
        this.color = color;
    }

    public Point getPoint()
    {
        return this.point;
    }

    /**
     * Draws the annotation at the position specified.
     * 
     * @param dc
     */
    public void render(DrawContext dc)
    {
        if (!enable)
        {
            return;
        }

        // Get position and check if it is inside the frustum
        Vec4 cartpoint = dc.getGlobe().computePointFromPosition(this.position);
        if (dc.getView().getFrustumInModelCoordinates().contains(cartpoint))
        {
            this.setVisible(true);
        }
        else
        {
            this.setVisible(false);
            return;
        }

        // Translate position to screen point
        Vec4 scrpoint = dc.getView().project(cartpoint);

        point.x = (int) scrpoint.x - 20;
        point.y = wwd.getSize().height - this.getSize().height - (int) scrpoint.y - 20;

        // Set the panel location.
        this.setLocation(point);

        // Draw triangle
        GL gl = dc.getGL();

        float[] c = this.color.getRGBComponents(null);
        gl.glColor3f(c[0], c[1], c[2]);

        gl.glLineWidth(2);

        gl.glBegin(GL.GL_TRIANGLES);

        gl.glVertex2d(scrpoint.x, scrpoint.y);
        gl.glVertex2d(scrpoint.x + 20, scrpoint.y + 20);
        gl.glVertex2d(scrpoint.x, scrpoint.y + 20);

        gl.glEnd();
    }
}
