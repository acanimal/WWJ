/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;

/**
 * @author Tom Gaskins
 * @version $Id: Layer.java 2471 2007-07-31 21:50:57Z tgaskins $
 */
public interface Layer extends WWObject, Renderable, Disposable
{
    public boolean isEnabled();

    public void setEnabled(boolean enabled);

    String getName();

    void setName(String name);

    double getOpacity();

    void setOpacity(double opacity);

    boolean isPickEnabled();

    void setPickEnabled(boolean isPickable);

    public void render(DrawContext dc);

    public void pick(DrawContext dc, java.awt.Point pickPoint);
}
