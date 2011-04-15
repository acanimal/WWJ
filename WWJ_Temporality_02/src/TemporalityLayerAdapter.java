package gov.nasa.worldwind.util;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;

/**
 * TemporalityLayerAdapter joins a Layer object with a Temporality. Then
 * the Layer object will only be rendered when the View intersects with
 * the specified Temporality.
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class TemporalityLayerAdapter implements Renderable
{

    private Temporality temporality = null;
    private Layer layer = null;

    public TemporalityLayerAdapter(Layer layer, Temporality temporality)
    {
        if (temporality == null || layer == null)
        {
            String message = Logging.getMessage("nullValue.LayerIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.layer = layer;
        this.temporality = temporality;
    }

    public void render(DrawContext dc)
    {
        if (temporality.intersects(dc.getView().getTemporalityCursor()))
        {
            layer.render(dc);
        }
    }

    public Temporality getTemporality()
    {
        return temporality;
    }

    public void setTemporality(Temporality temporality)
    {
        if (temporality == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.temporality = temporality;
    }

    public Layer getRenderable()
    {
        return layer;
    }

    public void setRenderable(Layer layer)
    {
        if (layer == null)
        {
            String message = Logging.getMessage("nullValue.LayerIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.layer = layer;
    }
}