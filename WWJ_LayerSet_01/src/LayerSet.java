package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import java.util.logging.Level;

/**
 * LayerSet allows create tree of layers. 
 * LayerSet extends the LayerList class and implements the Layer interface,
 * thus you can create composite layers.
 * 
 * You can add Layers, LayersList or LayerSet elements. Take into account, if 
 * you add a LayerList it is internally stored as a LayerSet with all elements
 * of the LayerList.
 * 
 * The methods render, pick, setOpacity, setEnable and setPickEnabled are 
 * invoked on all contained layers.
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class LayerSet extends LayerList implements Layer
{

    public boolean add(LayerList list)
    {
        if (list == null)
        {
            String message = Logging.getMessage("nullValue.LayerIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        LayerSet temp = new LayerSet();
        for (Layer layer : list)
        {
            temp.add(layer);
        }

        super.add(temp);
        temp.addPropertyChangeListener(this);
        this.firePropertyChange(AVKey.LAYERS, null, this);

        return true;
    }

    public boolean add(LayerSet set)
    {
        if (set == null)
        {
            String message = Logging.getMessage("nullValue.LayerIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        super.add(set);
        set.addPropertyChangeListener(this);
        this.firePropertyChange(AVKey.LAYERS, null, this);

        return true;
    }
    private boolean enabled = true;
    private boolean pickable = true;
    private double opacity = 1d;
    private double minActiveAltitude = -Double.MAX_VALUE;
    private double maxActiveAltitude = Double.MAX_VALUE;

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public boolean isPickEnabled()
    {
        return pickable;
    }

    public void setPickEnabled(boolean pickable)
    {
        if (this.pickable == pickable)
        {
            return;
        }

        this.pickable = pickable;

        for (Layer layer : this)
        {
            try
            {
                if (layer != null)
                {
                    layer.setPickEnabled(pickable);
                }
            } catch (Exception e)
            {
                String message = Logging.getMessage("nullValue.LayerIsNull");
                Logging.logger().log(Level.SEVERE, message, e);
                // Don't abort; continue on to the next layer.
            }
        }
    }

    public void setEnabled(boolean enabled)
    {

        if (this.enabled == enabled)
        {
            return;
        }

        this.enabled = enabled;

        for (Layer layer : this)
        {
            try
            {
                if (layer != null)
                {
                    layer.setEnabled(enabled);
                }
            } catch (Exception e)
            {
                String message = Logging.getMessage("nullValue.LayerIsNull");
                Logging.logger().log(Level.SEVERE, message, e);
                // Don't abort; continue on to the next layer.
            }
        }
    }

    public String getName()
    {
        Object n = this.getValue(AVKey.DISPLAY_NAME);

        return n != null ? n.toString() : this.toString();
    }

    public void setName(String name)
    {
        this.setValue(AVKey.DISPLAY_NAME, name);
    }

    public @Override String toString()
    {
        Object n = this.getValue(AVKey.DISPLAY_NAME);

        return n != null ? n.toString() : super.toString();
    }

    public double getOpacity()
    {
        return opacity;
    }

    public void setOpacity(double opacity)
    {

        if (this.opacity == opacity)
        {
            return;
        }

        this.opacity = opacity;

        for (Layer layer : this)
        {
            try
            {
                if (layer != null)
                {
                    layer.setOpacity(opacity);
                }
            } catch (Exception e)
            {
                String message = Logging.getMessage("nullValue.LayerIsNull");
                Logging.logger().log(Level.SEVERE, message, e);
                // Don't abort; continue on to the next layer.
            }
        }
    }

    public double getMinActiveAltitude()
    {
        return minActiveAltitude;
    }

    public void setMinActiveAltitude(double minActiveAltitude)
    {
        this.minActiveAltitude = minActiveAltitude;
    }

    public double getMaxActiveAltitude()
    {
        return maxActiveAltitude;
    }

    public void setMaxActiveAltitude(double maxActiveAltitude)
    {
        this.maxActiveAltitude = maxActiveAltitude;
    }

    /**
     * Indicates whether the layer is in the view. The method implemented here is a default indicating the layer is in
     * view. Subclasses able to determine their presence in the view should override this implementation.
     *
     * @param dc the current draw context
     * @return <code>true</code> if the layer is in the view, <code>false</code> otherwise.
     */
    public boolean isLayerInView(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return true;
    }

    /**
     * Indicates whether the layer is active based on arbitrary criteria. The method implemented here is a default
     * indicating the layer is active if the current altitude is within the layer's min and max active altitudes.
     * Subclasses able to consider more criteria should override this implementation.
     *
     * @param dc the current draw context
     * @return <code>true</code> if the layer is active, <code>false</code> otherwise.
     */
    public boolean isLayerActive(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getView())
        {
            String message = Logging.getMessage("layers.LayerList.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        Position eyePos = dc.getView().getEyePosition();
        if (eyePos == null)
        {
            return false;
        }
        double altitude = eyePos.getElevation();
        return altitude >= this.minActiveAltitude && altitude <= this.maxActiveAltitude;
    }

    /**
     * @param dc the current draw context
     * @throws IllegalArgumentException if <code>dc</code> is null, or <code>dc</code>'s <code>Globe</code> or
     *                                  <code>View</code> is null
     */
    public void render(DrawContext dc)
    {
        if (!this.enabled)
        {
            return; // Don't check for arg errors if we're disabled
        }
        if (null == dc)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getGlobe())
        {
            String message = Logging.getMessage("layers.LayerList.NoGlobeSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getView())
        {
            String message = Logging.getMessage("layers.LayerList.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (!this.isLayerActive(dc))
        {
            return;
        }
        if (!this.isLayerInView(dc))
        {
            return;
        }
        this.doRender(dc);
    }

    /**
     * Render all container layers.
     */
    protected void doRender(DrawContext dc)
    {
        for (Layer layer : this)
        {
            try
            {
                if (layer != null)
                {
                    layer.render(dc);
                }
            } catch (Exception e)
            {
                String message = Logging.getMessage("nullValue.LayerIsNull");
                Logging.logger().log(Level.SEVERE, message, e);
                // Don't abort; continue on to the next layer.
            }
        }
    }

    public void pick(DrawContext dc, java.awt.Point point)
    {
        if (!this.enabled)
        {
            return; // Don't check for arg errors if we're disabled
        }
        if (null == dc)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getGlobe())
        {
            String message = Logging.getMessage("layers.LayerList.NoGlobeSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getView())
        {
            String message = Logging.getMessage("layers.LayerList.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (!this.isLayerActive(dc))
        {
            return;
        }
        if (!this.isLayerInView(dc))
        {
            return;
        }
        this.doPick(dc, point);
    }

    protected void doPick(DrawContext dc, java.awt.Point point)
    {
        for (Layer layer : this)
        {
            try
            {
                if (layer != null)
                {
                    layer.pick(dc, point);
                }
            } catch (Exception e)
            {
                String message = Logging.getMessage("nullValue.LayerIsNull");
                Logging.logger().log(Level.SEVERE, message, e);
                // Don't abort; continue on to the next layer.
            }
        }
    }

    public void dispose()
    {
        for (Layer layer : this)
        {
            try
            {
                if (layer != null)
                {
                    layer.dispose();
                }
            } catch (Exception e)
            {
                String message = Logging.getMessage("nullValue.LayerIsNull");
                Logging.logger().log(Level.SEVERE, message, e);
                // Don't abort; continue on to the next layer.
            }
        }
    }
}