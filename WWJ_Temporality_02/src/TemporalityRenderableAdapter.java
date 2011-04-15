package gov.nasa.worldwind.util;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;

/**
 * TemporalityRenderableAdapter joins a Renderable object with a Temporality. Then
 * the Renderable object will only be rendered when the View intersects with
 * the specified Temporality.
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class TemporalityRenderableAdapter implements Renderable
{

    private Temporality temporality = null;
    private Renderable renderable = null;

    public TemporalityRenderableAdapter(Renderable renderable, Temporality temporality)
    {
        if (temporality == null || renderable == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.renderable = renderable;
        this.temporality = temporality;
    }

    public void render(DrawContext dc)
    {
        if (temporality.intersects(dc.getView().getTemporalityCursor()))
        {
            renderable.render(dc);
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

    public Renderable getRenderable()
    {
        return renderable;
    }

    public void setRenderable(Renderable renderable)
    {
        if (renderable == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.renderable = renderable;
    }
}