package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.render.SwingAnnotation;
import gov.nasa.worldwind.render.SwingAnnotationRenderer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import java.awt.Container;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SwingAnnotationLayer is a layer container for SwingAnnotation objects.
 * All SwingAnnotation contained in the SwingAnnotationLayer are rendered
 * through the SwingAnnotationRenderer.
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 * @see SwingAnnotation
 * @see SwingAnnotationRenderer
 */
public class SwingAnnotationLayer extends AbstractLayer
{

    private final java.util.Collection<SwingAnnotation> annotations = new ConcurrentLinkedQueue<SwingAnnotation>();
    private SwingAnnotationRenderer annotationRenderer = new SwingAnnotationRenderer();
    private WorldWindowGLCanvas wwd = null;

    /**
     * Creates a new instance.
     * @param wwd 
     */
    public SwingAnnotationLayer(WorldWindowGLCanvas wwd)
    {
        this.wwd = wwd;
    }

    /**
     * Adds a new SwingAnnotation to the layer.
     * @param annotation
     */
    public void addAnnotation(SwingAnnotation annotation)
    {
        if (annotation == null)
        {
            String msg = "SwingAnnotation is null";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.annotations.add(annotation);
        annotation.setWorldWindowGLCanvas(wwd);

        Container parent = wwd.getParent();
        if (parent == null)
        {
            String msg = "WorldWindowGLCanvas has no parent reference.";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        parent.add(annotation, 0);
    }

    /**
     * Removes the specified annotation from the layer.
     * @param annotation
     */
    public void removeWidget(SwingAnnotation annotation)
    {
        if (annotation == null)
        {
            String msg = "SwingAnnotation is null";
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.annotations.remove(annotation);
    }

    /**
     * Returns a collection of annotations.
     * @return annotations
     */
    public java.util.Collection<SwingAnnotation> getSwingAnnotations()
    {
        return this.annotations;
    }

    /**
     * Renders the annotations of the layer.
     * 
     * @param dc
     */
    @Override
    protected void doRender(DrawContext dc)
    {
        this.annotationRenderer.render(dc, this.annotations);
    }
}
