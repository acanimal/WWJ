package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.Logging;

import java.beans.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class RenderableListLayer extends CopyOnWriteArrayList<Renderable> implements WWObject, Layer
{

    private WWObjectImpl wwo = new WWObjectImpl(this);

    public RenderableListLayer()
    {
    }

    public RenderableListLayer(Renderable[] renderables)
    {
        if (renderables == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        for (Renderable rend : renderables)
        {
            this.add(rend);
        }
    }

    @Override
    public Object clone()
    {
        RenderableListLayer newList = (RenderableListLayer) super.clone();
        newList.wwo = new WWObjectImpl(newList);
        for (Renderable r : newList)
        {
            if (r instanceof Layer)
            {
                ((Layer) r).removePropertyChangeListener(this);
            }
        }

        return newList;
    }

    public boolean add(Renderable renderable)
    {
        if (renderable == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        super.add(renderable);
        if (renderable instanceof Layer)
        {
            ((Layer) renderable).addPropertyChangeListener(this);
        }
        this.firePropertyChange(AVKey.LAYERS, null, this);

        return true;
    }

    public void add(int index, Renderable renderable)
    {
        if (renderable == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        super.add(index, renderable);
        if (renderable instanceof Layer)
        {
            ((Layer) renderable).addPropertyChangeListener(this);
        }
        this.firePropertyChange(AVKey.LAYERS, null, this);
    }

    public void remove(Renderable renderable)
    {
        if (renderable == null)
        {
            String msg = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (!this.contains(renderable))
        {
            return;
        }

        if (renderable instanceof Layer)
        {
            ((Layer) renderable).removePropertyChangeListener(this);
        }
        super.remove(renderable);
        this.firePropertyChange(AVKey.LAYERS, null, this);
    }

    public Renderable remove(int index)
    {
        Renderable renderable = get(index);
        if (renderable == null)
        {
            return null;
        }

        if (renderable instanceof Layer)
        {
            ((Layer) renderable).removePropertyChangeListener(this);
        }
        super.remove(index);
        this.firePropertyChange(AVKey.LAYERS, null, this);

        return renderable;
    }

    public Renderable set(int index, Renderable renderable)
    {
        if (renderable == null)
        {
            String message = Logging.getMessage("nullValue.LayerIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        Renderable oldRenderable = this.get(index);
        if (oldRenderable != null)
        {
            if (oldRenderable instanceof Layer)
            {
                ((Layer) oldRenderable).removePropertyChangeListener(this);
            }
        }

        super.set(index, renderable);
        if (renderable instanceof Layer)
        {
            ((Layer) renderable).addPropertyChangeListener(this);
        }
        this.firePropertyChange(AVKey.LAYERS, null, this);

        return oldRenderable;
    }

    public boolean remove(Object o)
    {
        for (Renderable renderable : this)
        {
            if (renderable.equals(o))
            {
                if (renderable instanceof Layer)
                {
                    ((Layer) renderable).removePropertyChangeListener(this);
                }
            }
        }

        boolean removed = super.remove(o);
        if (removed)
        {
            this.firePropertyChange(AVKey.LAYERS, null, this);
        }

        return removed;
    }

    public boolean addIfAbsent(Renderable renderable)
    {
        for (Renderable r : this)
        {
            if (r.equals(renderable))
            {
                return false;
            }
        }

        if (renderable instanceof Layer)
        {
            ((Layer) renderable).addPropertyChangeListener(this);
        }

        boolean added = super.addIfAbsent(renderable);
        if (added)
        {
            this.firePropertyChange(AVKey.LAYERS, null, this);
        }

        return added;
    }

    public boolean removeAll(Collection<?> objects)
    {
        for (Renderable renderable : this)
        {
            if (renderable instanceof Layer)
            {
                ((Layer) renderable).removePropertyChangeListener(this);
            }
        }

        boolean removed = super.removeAll(objects);
        if (removed)
        {
            this.firePropertyChange(AVKey.LAYERS, null, this);
        }

        return removed;
    }

    public int addAllAbsent(Collection<? extends Renderable> renderables)
    {
        for (Renderable renderable : renderables)
        {
            if (!this.contains(renderable))
            {
                if (renderable instanceof Layer)
                {
                    ((Layer) renderable).addPropertyChangeListener(this);
                }
            }
        }

        int numAdded = super.addAllAbsent(renderables);
        if (numAdded > 0)
        {
            this.firePropertyChange(AVKey.LAYERS, null, this);
        }

        return numAdded;
    }

    public boolean addAll(Collection<? extends Renderable> renderables)
    {
        boolean added = super.addAll(renderables);
        if (added)
        {
            this.firePropertyChange(AVKey.LAYERS, null, this);
        }

        return added;
    }

    public boolean addAll(int i, Collection<? extends Renderable> renderables)
    {
        for (Renderable renderable : renderables)
        {
            if (renderable instanceof Layer)
            {
                ((Layer) renderable).addPropertyChangeListener(this);
            }
        }

        boolean added = super.addAll(i, renderables);
        if (added)
        {
            this.firePropertyChange(AVKey.LAYERS, null, this);
        }

        return added;
    }

    public boolean retainAll(Collection<?> objects)
    {
        for (Renderable renderable : this)
        {
            if (!objects.contains(renderable))
            {
                if (renderable instanceof Layer)
                {
                    ((Layer) renderable).removePropertyChangeListener(this);
                }
            }
        }

        boolean added = super.retainAll(objects);
        if (added)
        {
            this.firePropertyChange(AVKey.LAYERS, null, this);
        }

        return added;
    }

    public Object getValue(String key)
    {
        return wwo.getValue(key);
    }

    public Collection<Object> getValues()
    {
        return wwo.getValues();
    }

    public Set<Map.Entry<String, Object>> getEntries()
    {
        return wwo.getEntries();
    }

    public String getStringValue(String key)
    {
        return wwo.getStringValue(key);
    }

    public void setValue(String key, Object value)
    {
        wwo.setValue(key, value);
    }

    public void setValues(AVList avList)
    {
        wwo.setValues(avList);
    }

    public boolean hasKey(String key)
    {
        return wwo.hasKey(key);
    }

    public void removeKey(String key)
    {
        wwo.removeKey(key);
    }

    public AVList copy()
    {
        return wwo.copy();
    }

    public AVList clearList()
    {
        return this.wwo.clearList();
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        wwo.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        wwo.removePropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        wwo.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        wwo.removePropertyChangeListener(listener);
    }

    public void firePropertyChange(PropertyChangeEvent propertyChangeEvent)
    {
        wwo.firePropertyChange(propertyChangeEvent);
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        wwo.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void propertyChange(PropertyChangeEvent propertyChangeEvent)
    {
        wwo.propertyChange(propertyChangeEvent);
    }

    @Override
    public String toString()
    {
        String r = "";
        for (Renderable rend : this)
        {
            r += rend.toString() + ", ";
        }
        return r;
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

        for (Renderable renderable : this)
        {
            try
            {
                if (renderable != null)
                {
                    if (renderable instanceof Layer)
                    {
                        ((Layer) renderable).setPickEnabled(pickable);
                    }
                }
            }
            catch (Exception e)
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

        for (Renderable renderable : this)
        {
            try
            {
                if (renderable != null)
                {
                    renderable.setEnabled(enabled);
                }
            }
            catch (Exception e)
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

        for (Renderable renderable : this)
        {
            try
            {
                if (renderable != null)
                {
                    renderable.setOpacity(opacity);
                }
            }
            catch (Exception e)
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

    protected void doRender(DrawContext dc)
    {
        for (Renderable renderable : this)
        {
            try
            {
                if (renderable != null)
                {
                    renderable.render(dc);
                }
            }
            catch (Exception e)
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
        for (Renderable renderable : this)
        {
            try
            {
                if (renderable != null)
                {
                    if (renderable instanceof Layer)
                    {
                        ((Layer) renderable).pick(dc, point);
                    }
                }
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("nullValue.LayerIsNull");
                Logging.logger().log(Level.SEVERE, message, e);
            // Don't abort; continue on to the next layer.
            }
        }
    }

    public void dispose()
    {
        for (Renderable renderable : this)
        {
            try
            {
                if (renderable != null)
                {
                    if (renderable instanceof Layer)
                    {
                        ((Layer) renderable).dispose();
                    }
                }
            }
            catch (Exception e)
            {
                String message = Logging.getMessage("nullValue.LayerIsNull");
                Logging.logger().log(Level.SEVERE, message, e);
            // Don't abort; continue on to the next layer.
            }
        }
    }

    /**
     * Checks of this RenderableListLayer or some of its contained Layers contains the
     * given object.
     * 
     * @param o
     * @return
     */
    @Override
    public boolean contains(Object o)
    {

        if (super.contains(o))
        {
            return true;
        }

        for (Iterator<Renderable> it = this.iterator(); it.hasNext();)
        {
            Renderable renderable = it.next();
            if (renderable instanceof RenderableListLayer)
            {
                if (((RenderableListLayer) renderable).contains(o))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks of this RenderableListLayer or some of its contained Layers contains and 
     * object of the given class.
     * 
     * @param o
     * @return
     */
    public boolean containsClass(Class<?> clazz)
    {

        for (Iterator it = this.iterator(); it.hasNext();)
        {
            Object object = it.next();

            if (object.getClass().equals(clazz))
            {
                return true;
            }

            if (object instanceof RenderableListLayer)
            {
                if (((RenderableListLayer) object).containsClass(clazz))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns the index of the first occurrence of an element with the specified 
     * class in this list (only search in the current layer not in its contained
     * layers), or -1 if this list does not contain an element of 
     * that type.
     * 
     * @param clazz element's class to search for
     */
    public int indexOf(Class<?> clazz)
    {

        int index = -1;
        for (Iterator it = this.iterator(); it.hasNext();)
        {
            Object object = it.next();
            index++;

            if (object.getClass().equals(clazz))
            {
                return index;
            }
        }

        return -1;
    }
}
