package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.AnnotatedIcon;
import gov.nasa.worldwind.render.AnnotationRenderer;
import gov.nasa.worldwind.render.BasicAnnotationRenderer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.IconRenderer;
import gov.nasa.worldwind.render.Pedestal;
import gov.nasa.worldwind.render.WWIcon;
import gov.nasa.worldwind.util.Logging;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AnnotatedIconLayer is a special kind of layer that knows how to render
 * AnnotatedIcons.
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class AnnotatedIconLayer extends AbstractLayer
{

    private final java.util.Collection<WWIcon> icons = new ConcurrentLinkedQueue<WWIcon>();
    private IconRenderer iconRenderer = new IconRenderer();
    private AnnotationRenderer annotationRenderer = new BasicAnnotationRenderer();
    private Pedestal pedestal;

    /**
     * Creates a new instance.
     */
    public AnnotatedIconLayer()
    {
    }

    /**
     * Adds a new AnnotatedIcon to the layer.
     * @param icon
     */
    public void addIcon(AnnotatedIcon icon)
    {
        if (icon == null)
        {
            String msg = Logging.getMessage("nullValue.Icon");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.icons.add(icon);
    }

    /**
     * Removes the specified icon from the layer.
     * @param icon
     */
    public void removeIcon(AnnotatedIcon icon)
    {
        if (icon == null)
        {
            String msg = Logging.getMessage("nullValue.Icon");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.icons.remove(icon);
    }

    /**
     * Returns a collection of icons.
     * @return
     */
    public java.util.Collection<WWIcon> getIcons()
    {
        return this.icons;
    }

    /**
     * Gets the pedestal to be used by the layer.
     * @return
     */
    public Pedestal getPedestal()
    {
        return pedestal;
    }

    /**
     * Sets the pedestal to be used by the layer.
     * @param pedestal
     */
    public void setPedestal(Pedestal pedestal)
    {
        this.pedestal = pedestal;
    }

    
    @Override
    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        this.iconRenderer.setPedestal(this.pedestal);
        this.iconRenderer.pick(dc, this.icons, pickPoint, this);
    }
    
    /**
     * Renders the icons of the layer. For each annotation to be shown, its
     * position is calculates so that it point to its respective icon.
     * 
     * @param dc
     */
    @Override
    protected void doRender(DrawContext dc)
    {
        this.iconRenderer.setPedestal(this.pedestal);

        for (WWIcon icon : icons)
        {
            // Render the icon
            this.iconRenderer.render(dc, icon, null);

            // Render its annotation.
            AnnotatedIcon aicon = (AnnotatedIcon) icon;
            GlobeAnnotation annotation = (GlobeAnnotation) aicon.getAnnotation();
            if (aicon.isShowAnnotation() && annotation != null)
            {
                // Get the screen position of the icon
                Vec4 point = dc.getGlobe().computePointFromPosition(aicon.getPosition());
                Vec4 screenpoint = dc.getView().project(point);
                // Adds to the screen position the pixel size of the icon
                
                System.out.println("he: "+aicon.getSize().getHeight());
                Vec4 incremented = new Vec4(screenpoint.x(), screenpoint.y() + aicon.getSize().getHeight(), screenpoint.z(), screenpoint.w());
                // Calculate the corresponding world position of the incremente screen size.
                Vec4 position = dc.getView().unProject(incremented);

                Position posan = dc.getGlobe().computePositionFromPoint(position);

                // Set the annotation position so that it point to the icon.
                annotation.setPosition(posan);
                this.annotationRenderer.render(dc, aicon.getAnnotation(), null);
            }
        }
    }
}
