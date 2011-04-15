package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Earth.BMNGOneImage;
import gov.nasa.worldwind.layers.Earth.BMNGSurfaceLayer;
import gov.nasa.worldwind.layers.Earth.EarthNASAPlaceNameLayer;
import gov.nasa.worldwind.layers.Earth.FogLayer;
import gov.nasa.worldwind.layers.Earth.ScalebarLayer;
import gov.nasa.worldwind.layers.Earth.SkyGradientLayer;
import gov.nasa.worldwind.layers.Earth.StarsLayer;
import gov.nasa.worldwind.layers.Earth.WorldMapLayer;
import gov.nasa.worldwind.layers.LayerSet;
import javax.swing.JFrame;

/**
 * Using LayerSet.
 *
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class LayerSetExample
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

            LayerSet main = new LayerSet();
            
            LayerSet groupA = new LayerSet();
            groupA.add(new StarsLayer());
            groupA.add(new SkyGradientLayer());
            groupA.add(new FogLayer());

            LayerSet groupB = new LayerSet();
            groupB.add(new BMNGOneImage());
            groupB.add(new BMNGSurfaceLayer());            
            groupB.add(new EarthNASAPlaceNameLayer());
            
            LayerSet groupC = new LayerSet();
            groupC.add(new ScalebarLayer());
            groupC.add(new CompassLayer());
            groupC.add(new WorldMapLayer());

            main.add(groupA);
            main.add(groupB);
            main.add(groupC);
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
                appFrame.setSize(700, 600);
                appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                appFrame.setVisible(true);
            }
        });
    }
}