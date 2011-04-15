package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Earth.BMNGSurfaceLayer;
import gov.nasa.worldwind.layers.Earth.LandsatI3;
import gov.nasa.worldwind.layers.Earth.ScalebarLayer;
import gov.nasa.worldwind.layers.Earth.WorldMapLayer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.RenderableListLayer;
import gov.nasa.worldwind.render.SurfaceImage;
import javax.swing.JFrame;

/**
 * Using LayerSet.
 *
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class RenderableListLayerExample
{

    private static class AppFrame extends javax.swing.JFrame
    {

        public AppFrame()
        {
            WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
            wwd.setPreferredSize(new java.awt.Dimension(1000, 800));
            this.getContentPane().add(wwd, java.awt.BorderLayout.CENTER);
            this.pack();

            String WWJ_SPLASH_PATH = "images/400x230-splash-nww.png";
            String GEORSS_ICON_PATH = "images/georss.png";
            String NASA_ICON_PATH = "images/32x32-icon-nasa.png";

            SurfaceImage si1 = new SurfaceImage(WWJ_SPLASH_PATH, Sector.fromDegrees(35, 45, -115, -95));
            SurfaceImage si2 = new SurfaceImage(GEORSS_ICON_PATH, Sector.fromDegrees(25, 33, -120, -110));
            SurfaceImage si3 = new SurfaceImage(NASA_ICON_PATH, Sector.fromDegrees(25, 35, -100, -90));

            LayerList ll = new LayerList();
            RenderableListLayer main = new RenderableListLayer();

            main.add(new BMNGSurfaceLayer());
            main.add(new LandsatI3());
            main.add(new ScalebarLayer());              
            
            RenderableListLayer rll = new RenderableListLayer();
            rll.add(si1);
            rll.add(si2);
            rll.add(si3);
            rll.add(new CompassLayer());
            rll.add(new WorldMapLayer());  
            
            main.add(rll);
            main.setPickEnabled(false);

            ll.add(main);
            BasicModel model = new BasicModel();
            model.setLayers(ll);
            wwd.setModel(model);            
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
