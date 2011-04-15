package gov.nasa.worldwind.render;

import com.sun.opengl.util.BufferUtil;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.util.Logging;
import java.awt.Color;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;

/**
 * <p>Prism objects are like prism optical element but with the specified 
 * shape. The prism goes from globe surface (elevation 0) to the specified
 * top elevation and can be filled or wired</p>
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class Prism implements Renderable
{
    // Attributes
    private double topElevation = 50e3;
    private Color color = new Color(0.5f, 0.6f, 0.7f, 0.9f);
    private Color wiredColor = new Color(0.5f, 0.6f, 0.7f, 0.9f).brighter();
    private double lineWidth = 1;
    private boolean filled = true;
    // Private data
    private double bottomElevation = 0;
    private ArrayList<LatLon> positions = null;
    private int numpositions = 0;
    double[][] polygon = null;
    DoubleBuffer vertexBuffer = null;
    IntBuffer indexesBuffer = null;
    IntBuffer indexesLineBuffer = null;
    private int polygonList = -1;
    private boolean changed = false; // Indicates some property has changed and the display list must be recreated.

    /**
     * Creates a new Prism instance.
     * @param positions Shape to use for the prism.
     * @param elevation Top elevation.
     */
    public Prism(ArrayList<LatLon> positions, double elevation)
    {
        if (positions == null || positions.size() < 3)
        {
            String msg = "Invalid number of positions.";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.positions = positions;
        this.topElevation = Math.abs(elevation);
    }

    /**
     * Creates a new Prism instance.
     * @param positions Shape to use for the prism.
     * @param elevation Top elevation.
     * @param color Color to fill the prism. The wire color is the same as color
     * but slightly brighter.
     */
    public Prism(ArrayList<LatLon> positions, double elevation, Color color)
    {
        if (positions == null || positions.size() < 3)
        {
            String msg = "Invalid number of positions.";
            Logging.logger().severe(msg);
        }
        if (color == null)
        {
            String msg = "Invalid color.";
            Logging.logger().severe(msg);
        }

        this.positions = positions;
        this.topElevation = Math.abs(elevation);
        if (color != null)
        {
            this.setColor(color);
        }
    }

    /**
     * Creates a new Prism instance.
     * @param positions Shape to use for the prism.
     * @param elevation Top elevation.
     * @param color Color to fill the prism.     
     * @param wiredColor Color for the wire.
     */
    public Prism(ArrayList<LatLon> positions, double elevation, Color color, Color wiredColor)
    {
        this.positions = positions;
        this.topElevation = Math.abs(elevation);
        this.color = color;
        this.wiredColor = wiredColor;
    }

    /**
     * Initialize the prism for the first time rendering. Creates vertex buffer
     * and tessellates the polygon.
     * 
     * @param dc
     */
    private void initialize(DrawContext dc)
    {
        this.numpositions = getPositions().size();

        // Create array to store polygon vertex for future tessellation
        polygon = new double[this.numpositions][3];

        // We need to create two polygons, one at globe's surface and
        // one at the specified elevation and connect all point from top
        // to bottom.
        //
        // This Vec4 array stores temporally vertex for both bottom and top polygons.
        Vec4[] points = new Vec4[this.numpositions * 2];
        for (int i = 0; i < this.numpositions; i++)
        {
            LatLon latLon = getPositions().get(i);
            // We store first the bottom polygon and then the top polygon
            points[i] = dc.getGlobe().
                    computePointFromPosition(latLon.getLatitude(), latLon.getLongitude(), bottomElevation);
            points[i + this.numpositions] = dc.getGlobe().
                    computePointFromPosition(latLon.getLatitude(), latLon.getLongitude(), getTopElevation());

            polygon[i][0] = points[i + this.numpositions].x;
            polygon[i][1] = points[i + this.numpositions].y;
            polygon[i][2] = points[i + this.numpositions].z;
        }

        // Create a double buffer to store the previous vertex.
        vertexBuffer = BufferUtil.newDoubleBuffer(points.length * 3);
        for (int i = 0; i < points.length; i++)
        {
            Vec4 v = points[i];
            vertexBuffer.put(v.x);
            vertexBuffer.put(v.y);
            vertexBuffer.put(v.z);
        }
        vertexBuffer.rewind();

        // Creates an array with vertex indexes to draw filled laterals.
        indexesBuffer = BufferUtil.newIntBuffer(this.numpositions * 4);
        for (int i = 0; i < this.numpositions - 1; i++)
        {
            indexesBuffer.put(i);
            indexesBuffer.put(i + 1);
            indexesBuffer.put(i + 1 + this.numpositions);
            indexesBuffer.put(i + this.numpositions);
        }
        indexesBuffer.rewind();

        // Creates an array with vertex indexes to draw wired laterals.
        indexesLineBuffer = BufferUtil.newIntBuffer(this.numpositions * 2);
        for (int i = 0; i < this.numpositions; i++)
        {
            indexesLineBuffer.put(i);
            indexesLineBuffer.put(i + this.numpositions);
        }
        indexesLineBuffer.rewind();

        // Tessellate polygon.
        tessellate(dc);
    }

    /**
     * Tessellates the polygon and creates a display list to render it.
     * @param dc
     */
    private void tessellate(DrawContext dc)
    {
        GL gl = dc.getGL();
        GLU glu = new GLU();

        // Tessellate polygon and create a display list.
        GLUtessellator tobj = glu.gluNewTess();
        TessCallback tessCallback = new TessCallback(gl, glu);

        glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, tessCallback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_END, tessCallback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, tessCallback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, tessCallback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, tessCallback);

        this.polygonList = gl.glGenLists(1);
        gl.glNewList(this.polygonList, GL.GL_COMPILE);
        glu.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_POSITIVE);
        glu.gluTessBeginPolygon(tobj, null);
        glu.gluTessBeginContour(tobj);

        for (int i = 0; i < this.numpositions; i++)
        {
            glu.gluTessVertex(tobj, polygon[i], 0, polygon[i]);
        }

        glu.gluTessEndContour(tobj);
        glu.gluTessEndPolygon(tobj);
        gl.glEndList();
        glu.gluDeleteTess(tobj);
    }

    /**
     * Renders the prism
     * @param dc
     */
    public void render(DrawContext dc)
    {
        GL gl = dc.getGL();

        if (this.getPositions() == null)
        {
            return;
        }
        if (this.getPositions().size() < 3)
        {
            return;
        }

        // Initializes the prism for the first time rendering.
        if (this.polygon == null || this.changed)
        {
            initialize(dc);
            this.changed = false;
        }

        // Render
        gl.glPushAttrib(GL.GL_CURRENT_BIT | GL.GL_LINE_BIT | GL.GL_COLOR_BUFFER_BIT);
        try
        {
            if (!dc.isPickingMode())
            {
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            }
            gl.glLineWidth((float) this.getLineWidth());

            // Enable vertex array and set the vertex buffer
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL.GL_DOUBLE, 0, vertexBuffer);

            // If filled then draw laterals and top polygon specifying polygon
            // offset to avoid z-figthing with the wired geometry.
            if (this.isFilled())
            {
                if (!dc.isPickingMode())
                {
                    gl.glColor4ub((byte) this.getColor().getRed(), (byte) this.getColor().getGreen(),
                            (byte) this.getColor().getBlue(), (byte) this.getColor().getAlpha());
                }
                gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
                gl.glPolygonOffset(1.0f, 1.0f);
                gl.glDrawElements(GL.GL_QUADS, this.indexesBuffer.capacity(), GL.GL_UNSIGNED_INT, indexesBuffer);
                gl.glCallList(this.polygonList);
                gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
            }

            // Always draw the wired skeleton    
            if (!dc.isPickingMode())
            {
                gl.glColor4ub((byte) this.getWiredColor().getRed(), (byte) this.getWiredColor().getGreen(),
                        (byte) this.getWiredColor().getBlue(), (byte) this.getWiredColor().getAlpha());
            }
            gl.glDrawElements(GL.GL_LINES, this.indexesLineBuffer.capacity(), GL.GL_UNSIGNED_INT, indexesLineBuffer);
            gl.glDrawArrays(GL.GL_LINE_LOOP, this.numpositions, this.numpositions);

            gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        }
        finally
        {
            gl.glPopAttrib();
        }
    }

    public double getTopElevation()
    {
        return topElevation;
    }

    public void setTopElevation(double topElevation)
    {
        this.topElevation = Math.abs(topElevation);
        this.changed = true;
    }

    public ArrayList<LatLon> getPositions()
    {
        return positions;
    }

    public Color getColor()
    {
        return color;
    }

    public void setColor(Color color)
    {
        this.color = color;
        this.wiredColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).brighter();
    }

    public Color getWiredColor()
    {
        return wiredColor;
    }

    public void setWiredColor(Color wiredColor)
    {
        this.wiredColor = wiredColor;
    }

    public double getLineWidth()
    {
        return lineWidth;
    }

    public void setLineWidth(double lineWidth)
    {
        this.lineWidth = lineWidth;
    }

    public boolean isFilled()
    {
        return filled;
    }

    public void setFilled(boolean filled)
    {
        this.filled = filled;
    }

    /**
     * Tessellation callback implementation.
     */
    public static class TessCallback extends javax.media.opengl.glu.GLUtessellatorCallbackAdapter
    {

        GL gl;
        GLU glu;

        public TessCallback(GL gl, GLU glu)
        {
            this.gl = gl;
            this.glu = glu;
        }

        @Override
        public void begin(int type)
        {
            gl.glBegin(type);
        }

        @Override
        public void end()
        {
            gl.glEnd();
        }

        @Override
        public void vertex(Object data)
        {
            if (data instanceof double[])
            {
                double[] d = (double[]) data;
//                if (d.length == 6)
//                {
//                    gl.glColor3dv(d, 3);
//                }
                gl.glVertex3dv(d, 0);
            }
        }

        @Override
        public void error(int errnum)
        {
            String estring = glu.gluErrorString(errnum);
            String msg = "Tessellation Error: " + estring;
            Logging.logger().severe(msg);
            throw new RuntimeException();
        }

        @Override
        public void combine(double[] coords, Object[] data,
                float[] weight, Object[] outData)
        {
            double[] vertex = new double[6];

            int i;
            vertex[0] = coords[0];
            vertex[1] = coords[1];
            vertex[2] = coords[2];
//            for (i = 3; i < 6; i++)
//            {
//                vertex[i] = weight[0] * ((double[]) data[0])[i] +
//                        weight[1] * ((double[]) data[1])[i] +
//                        weight[2] * ((double[]) data[2])[i] +
//                        weight[3] * ((double[]) data[3])[i];
//            }
            outData[0] = vertex;
        }
    }
}
