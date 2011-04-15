package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Earth.BMNGOneImage;
import gov.nasa.worldwind.layers.Earth.ScalebarLayer;
import gov.nasa.worldwind.layers.Earth.WorldMapLayer;
import gov.nasa.worldwind.layers.LayerSet;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.SurfaceImage;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 * Using LayerSet.
 *
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class HttpImageExample
{

    private static class AppFrame extends javax.swing.JFrame
    {

        public AppFrame()
        {
            WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
            wwd.setPreferredSize(new java.awt.Dimension(1000, 800));
            this.getContentPane().add(wwd, java.awt.BorderLayout.CENTER);
            this.pack();

            BasicModel model = new BasicModel();
            wwd.setModel(model);

            final LayerSet main = new LayerSet();

            main.add(new BMNGOneImage());

            Thread t = new Thread(new Runnable()
            {

                public void run()
                {
                    try
                    {
                        System.out.println("Sleep 10 secs.");
                        for (int i = 9; i >= 0; i--)
                        {
                            Thread.sleep(1000);
                            System.out.println("..." + i);
                        }

                        RenderableLayer rend = new RenderableLayer();
                        SurfaceImage img = new SurfaceImage("http://openclipart.org/people/yeKcim/yeKcim_pirats_boat.png", Sector.fromDegrees(40, 43, 0, 5));

                        rend.addRenderable(img);
                        rend.setPickEnabled(false);
                        main.add(rend);
                    } catch (InterruptedException ex)
                    {
                        Logger.getLogger(HttpImageExample.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            t.start();

            main.add(new ScalebarLayer());
            main.add(new CompassLayer());
            main.add(new WorldMapLayer());
            main.setPickEnabled(false);
            model.setLayers(main);
        }
    }

    public static void main(String[] args)
    {
        java.awt.EventQueue.invokeLater(new Runnable()
        {

            public void run()
            {
                AppFrame appFrame = new AppFrame();
                appFrame.setSize(500, 500);
                appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                appFrame.setVisible(true);
            }
        });
    }
}
