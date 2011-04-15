/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.WWIcon;

/**
 * @author tag
 * @version $Id$
 */
public class MovableSurfaceImages extends ApplicationTemplate
{

    private static final String WWJ_SPLASH_PATH = "images/400x230-splash-nww.png";
    private static final String GEORSS_ICON_PATH = "images/georss.png";
    private static final String NASA_ICON_PATH = "images/32x32-icon-nasa.png";

    public static class AppFrame extends ApplicationTemplate.AppFrame
    {

        public AppFrame()
        {
            super(true, true, false);

            try
            {
                SurfaceImage si1 = new SurfaceImage(WWJ_SPLASH_PATH, Sector.fromDegrees(35, 45, -115, -95));
                SurfaceImage si2 = new SurfaceImage(GEORSS_ICON_PATH, Sector.fromDegrees(25, 33, -120, -110));
                SurfaceImage si3 = new SurfaceImage(NASA_ICON_PATH, Sector.fromDegrees(25, 35, -100, -90));

                RenderableLayer layer = new RenderableLayer();
                layer.setName("Surface Images");
                layer.addRenderable(si1);
                layer.addRenderable(si2);
                layer.addRenderable(si3);

                insertBeforeCompass(this.getWwd(), layer);

                this.getLayerPanel().update(this.getWwd());

                this.getWwd().addSelectListener(new SelectListener()
                {
                    private BasicDragger dragger = new BasicDragger(getWwd());

                    public void selected(SelectEvent event)
                    {                                             
                        // Have drag events drag the selected object.
                        if (event.getEventAction().equals(SelectEvent.DRAG_END) || event.getEventAction().equals(SelectEvent.DRAG))
                        {
                            // Delegate dragging computations to a dragger.
                            System.out.println("SELECT EVENT: "+event.getEventAction());
                            this.dragger.selected(event);

                            // We missed any roll-over events while dragging, so highlight any under the cursor now,
                            // or de-highlight the dragged shape if it's no longer under the cursor.
                            if (event.getEventAction().equals(SelectEvent.DRAG_END))
                            {
                                PickedObjectList pol = getWwd().getObjectsAtCurrentPosition();
                                if (pol != null)
                                {
                                    AppFrame.this.getWwd().repaint();
                                }
                            }
                        }
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind Surface Images", MovableSurfaceImages.AppFrame.class);
    }
}
