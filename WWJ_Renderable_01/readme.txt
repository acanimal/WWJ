Author: 
-------
Antonio Santiago <asantiagop@gmail.com>

Description:
------------
Modified version of WWJ to adding more common methods to Renderable interface.
The idea is that every Renderable object has a 'render' method but also can
be enable (or not), can has a name and can has an opacity.

Also the Layer interface inherits (without any changes) from Renderable.

These new methods are:

    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    public String getName();

    public void setName(String name);

    public double getOpacity();

    public void setOpacity(double opacity);

in addition to the original one:

    public void render(DrawContext dc);


Although it seems a very big change, the changes are a very few. Finally it must be
necessary to implement the appropiate mechanism to render images, nodes and other
Renderable object taking into account its opacity and enable values.


Changes from original WWJ 0.3.0:
--------------------------------

Affected code:

* Modified 'gov.nasa.worldwind.render.Renderable'. I have added the previous methods:

    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    public String getName();

    public void setName(String name);

    public double getOpacity();

    public void setOpacity(double opacity);

* Added a default implementation on all affected objects (all attached classes except RenderableListLayer).
  The default implementation is:

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


* Created new class 'gov.nasa.worldwind.layers.RenderableListLayer', a modified version of LayerList
  that stores Renderable objects (Layer or some other Renderable implementation).




