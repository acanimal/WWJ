package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.layers.Earth.TimelineLayer;
import gov.nasa.worldwind.util.TemporalityAdapter;
import gov.nasa.worldwind.util.Temporality;
import gov.nasa.worldwind.layers.LayerSet;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Earth.BMNGOneImage;
import gov.nasa.worldwind.layers.Earth.BMNGSurfaceLayer;
import gov.nasa.worldwind.layers.Earth.EarthNASAPlaceNameLayer;
import gov.nasa.worldwind.layers.Earth.FogLayer;
import gov.nasa.worldwind.layers.Earth.ScalebarLayer;
import gov.nasa.worldwind.layers.Earth.SkyGradientLayer;
import gov.nasa.worldwind.layers.Earth.StarsLayer;
import gov.nasa.worldwind.layers.Earth.WorldMapLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.SurfaceCircle;
import gov.nasa.worldwind.render.SurfaceImage;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 * Using Temporality.
 *
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class TemporalityExample
{

    private static final String WWJ_SPLASH_PATH = "images/400x230-splash-nww.png";
    private static final String GEORSS_ICON_PATH = "images/georss.png";

    private static class AppFrame extends javax.swing.JFrame
    {

        private WorldWindowGLCanvas wwd;
        private Calendar cal_ini;
        private Calendar cal_fin;
        private Temporality tempor;

        public AppFrame()
        {
            wwd = new WorldWindowGLCanvas();
            wwd.setPreferredSize(new java.awt.Dimension(1000, 800));
            this.getContentPane().add(wwd, java.awt.BorderLayout.CENTER);
            this.pack();

            BasicModel model = new BasicModel();
            wwd.setModel(model);

            // Create one set of layers.
            LayerSet mainlayers = new LayerSet();
            mainlayers.add(new StarsLayer());
            mainlayers.add(new SkyGradientLayer());
            mainlayers.add(new FogLayer());
            mainlayers.add(new BMNGOneImage());
            mainlayers.add(new BMNGSurfaceLayer());
            mainlayers.add(new WorldMapLayer());
            mainlayers.add(new EarthNASAPlaceNameLayer());
            mainlayers.add(new ScalebarLayer());
            mainlayers.add(new CompassLayer());
            mainlayers.add(new TimelineLayer());

            // Create a second set of layers with temporary objects.
            LayerSet templayers = new LayerSet();

            // Temporary image.
            SurfaceImage img = new SurfaceImage(WWJ_SPLASH_PATH, Sector.fromDegrees(35, 50, 0, 10));
            cal_ini = new GregorianCalendar(2007, 12, 20);
            cal_fin = new GregorianCalendar(2007, 12, 22);
            tempor = new Temporality(cal_ini, cal_fin);
            TemporalityAdapter tadapter1 = new TemporalityAdapter(img, tempor);

            // Temporary circle
            LatLon position = new LatLon(Angle.fromDegrees(20), Angle.fromDegrees(-10));
            Globe globe = wwd.getModel().getGlobe();
            SurfaceCircle circle = new SurfaceCircle(globe, position, 1200e3, 30);
            cal_ini = new GregorianCalendar(2007, 12, 21);
            cal_fin = new GregorianCalendar(2007, 12, 23);
            tempor = new Temporality(cal_ini, cal_fin);
            TemporalityAdapter tadapter2 = new TemporalityAdapter(circle, tempor);

            // Temporary image.
            img = new SurfaceImage(GEORSS_ICON_PATH, Sector.fromDegrees(0, 30, 0, 30));
            cal_ini = new GregorianCalendar(2007, 12, 23);
            cal_fin = new GregorianCalendar(2007, 12, 26);
            tempor = new Temporality(cal_ini, cal_fin);
            TemporalityAdapter tadapter3 = new TemporalityAdapter(img, tempor);

            // Add object to a renderable layer.
            RenderableLayer rend = new RenderableLayer();
            rend.addRenderable(tadapter1);
            rend.addRenderable(tadapter2);
            rend.addRenderable(tadapter3);
            templayers.add(rend);

            // Set the temporality for the View.
            cal_ini = new GregorianCalendar(2007, 12, 17);
            cal_fin = new GregorianCalendar(2007, 12, 24, 10, 30);
            tempor = new Temporality(cal_ini, cal_fin);
            wwd.getView().setTemporality(tempor);
            // Set the cursor increment in 1 day.
            wwd.getView().setTemporalityCursorSize(Calendar.DAY_OF_MONTH, 1);

            // Set layers in the model.
            LayerSet layers = new LayerSet();
            layers.add(mainlayers);
            layers.add(templayers);
            layers.setPickEnabled(false);

            model.setLayers(layers);

            // Create a thread to step up the cursor every 500 ms.
            Thread th = new Thread(new stepUp());
            th.start();
        }

        public class stepUp implements Runnable
        {

            public void run()
            {
                while (true)
                {
                    try
                    {
                        Thread.sleep(500);

                        wwd.getView().temporalityCursorStepUp();
//                        wwd.getView().temporalityCursorStepDown();
                        wwd.redrawNow();
                    } catch (InterruptedException ex)
                    {
                        Logger.getLogger(TemporalityExample.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    public static void main(String[] args)
    {
        java.awt.EventQueue.invokeLater(new Runnable()
        {

            public void run()
            {
                AppFrame appFrame = new AppFrame();
                appFrame.setSize(700, 600);
                appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                appFrame.setVisible(true);
            }
        });
    }
}