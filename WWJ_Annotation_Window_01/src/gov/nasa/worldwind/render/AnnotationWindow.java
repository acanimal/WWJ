package gov.nasa.worldwind.render;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import javax.media.opengl.GL;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.border.LineBorder;

/**
 *
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class AnnotationWindow extends JWindow
{

    private boolean enable = true;
    private JPanel panel = null;
    private WorldWindowGLCanvas wwd = null;
    private Point point = new Point(0, 0);
    private Position position = Position.fromDegrees(0, 0, 0);
    private Color color = Color.LIGHT_GRAY;

    public AnnotationWindow(Position position, WorldWindowGLCanvas wwd)
    {
        this.position = position;
        this.wwd = wwd;

        // Initially the JWindow is hidden.
        this.setVisible(false);

        // Set window border.
        JPanel content = (JPanel) this.getContentPane();
        content.setBorder(new LineBorder(this.color, 2));
    }

    public JPanel getPanel()
    {
        return panel;
    }

    public void setPanel(JPanel panel)
    {
        if (panel == null)
        {
            return;
        }
        this.panel = panel;

        this.getContentPane().add(panel);
        this.pack();
    }

    public Position getPosition()
    {
        return position;
    }

    public void setPosition(Position position)
    {
        this.position = position;
    }

    public boolean isEnable()
    {
        return enable;
    }

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

    public void render(DrawContext dc)
    {

        Dimension size = this.getSize();
        Dimension base = wwd.getSize();
        Point loc = wwd.getLocationOnScreen();

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
        this.point.x = loc.x + (int) scrpoint.x;
        this.point.y = loc.y + base.height - size.height - (int) scrpoint.y - 20;

        // Set the window location.
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
