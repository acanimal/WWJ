package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.Earth.*;

import gov.nasa.worldwind.render.RotableUserFacingIcon;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.awt.*;
import java.util.Iterator;

public class RotableIconsExample
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

            final IconLayer iconLayer = buildIconLayer();
            layerList.add(iconLayer);

            Thread t = new Thread(new Runnable()
            {

                public void run()
                {
                    while (true)
                    {
                        try
                        {
                            Thread.sleep(50);
                            for (Iterator it = iconLayer.getIcons().iterator(); it.hasNext();)
                            {
                                RotableUserFacingIcon icon = (RotableUserFacingIcon) it.next();
                                icon.setHeading(icon.getHeading().addDegrees(10));
                            }
                            wwd.redrawNow();
                        }
                        catch (InterruptedException ex)
                        {
                            Logger.getLogger(RotableIconsExample.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            });
            t.start();

            Model m = new BasicModel();
            m.setLayers(layerList);
            wwd.setModel(m);

            JPanel westContainer = new LayerPanel(wwd);
            this.getContentPane().add(westContainer, BorderLayout.WEST);
            this.pack();
        }

        private IconLayer buildIconLayer()
        {
            IconLayer layer = new IconLayer();

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
                    RotableUserFacingIcon icon = new RotableUserFacingIcon("images/32x32-icon-nasa.png",
                            new Position(Angle.fromDegrees(lat), Angle.fromDegrees(lon), alt));
                    icon.setHighlightScale(1.5);
                    icon.setToolTipText(icon.getImageSource().toString());
                    icon.setToolTipTextColor(java.awt.Color.YELLOW);
                    icon.setHeading(Angle.fromDegrees(lon));

                    layer.addIcon(icon);
                }
            }

            return layer;
        }
    }

    public static void main(String[] args)
    {
        RotableIconsExample demo = new RotableIconsExample();
        AppFrame appFrame = new AppFrame();
        appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        appFrame.setVisible(true);
    }
}
