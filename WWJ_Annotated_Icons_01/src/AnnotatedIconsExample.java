package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.Earth.*;

import gov.nasa.worldwind.render.AnnotatedIcon;
import gov.nasa.worldwind.render.GlobeAnnotation;
import javax.swing.*;
import java.awt.*;

public class AnnotatedIconsExample
{

    private static class AppFrame extends JFrame
    {

        public AppFrame()
        {
            final WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            wwd.setPreferredSize(new Dimension(500, 400));
            mainPanel.add(wwd, BorderLayout.CENTER);

            StatusBar statusBar = new StatusBar();
            statusBar.setEventSource(wwd);
            mainPanel.add(statusBar, BorderLayout.PAGE_END);
            this.getContentPane().add(mainPanel, BorderLayout.CENTER);

            LayerList layerList = new LayerList();
            layerList.add(new BMNGOneImage());

            layerList.add(buildIconLayer());

            Model m = new BasicModel();
            m.setLayers(layerList);
            wwd.setModel(m);

            JPanel westContainer = new LayerPanel(wwd);
            this.getContentPane().add(westContainer, BorderLayout.WEST);
            this.pack();

            wwd.addSelectListener(new SelectListener()
            {

                AnnotatedIcon lastIcon = null;

                public void selected(SelectEvent event)
                {
                    if (event.getEventAction().equals(SelectEvent.ROLLOVER))
                    {
                        Object object = event.getTopObject();

                        if (object instanceof AnnotatedIcon)
                        {
                            AnnotatedIcon selected = (AnnotatedIcon) object;

                            if (selected != lastIcon)
                            {
                                if (lastIcon != null)
                                {
                                    lastIcon.setShowAnnotation(false);
                                }
                                lastIcon = selected;
                                lastIcon.setShowAnnotation(true);
                            }
                        }
                        else
                        {
                            if (lastIcon != null)
                            {
                                lastIcon.setShowAnnotation(false);
                                lastIcon = null;
                            }
                        }
                    }
                }
            });
        }

        private AnnotatedIconLayer buildIconLayer()
        {
            AnnotatedIconLayer layer = new AnnotatedIconLayer();

            // Distribute little NASA icons around the equator. Put a few at non-zero altitude.
            for (double lat = 0; lat < 10; lat += 10)
            {
                for (double lon = -180; lon < 180; lon += 20)
                {
                    double alt = 0;
                    if (lon % 90 == 0)
                    {
                        alt = 2000000;
                    }
                    AnnotatedIcon icon = new AnnotatedIcon("images/32x32-icon-nasa.png",
                            new Position(Angle.fromDegrees(lat), Angle.fromDegrees(lon), alt));
                    icon.setHighlightScale(1.5);
                    icon.setToolTipText(icon.getImageSource().toString());
                    icon.setToolTipTextColor(java.awt.Color.YELLOW);

                    GlobeAnnotation an = new GlobeAnnotation("This is the annotation", Position.fromDegrees(40, 2, 0));
                    icon.setAnnotation(an);

                    layer.addIcon(icon);
                }
            }

            return layer;
        }
    }

    public static void main(String[] args)
    {
        AnnotatedIconsExample demo = new AnnotatedIconsExample();
        AppFrame appFrame = new AppFrame();
        appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        appFrame.setVisible(true);
    }
}
