package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.Earth.BMNGSurfaceLayer;
import gov.nasa.worldwind.layers.Earth.LandsatI3;
import gov.nasa.worldwind.layers.LayerList;
import javax.swing.JFrame;

public class OpacityExample
{
    private static class AppFrame extends javax.swing.JFrame
    {
        public AppFrame()
        {
            WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
            wwd.setPreferredSize(new java.awt.Dimension(700, 600));
            this.getContentPane().add(wwd, java.awt.BorderLayout.CENTER);
            this.pack();
            
            LayerList layers = new LayerList();
            layers.add(new BMNGSurfaceLayer());
            LandsatI3 land = new LandsatI3();
            layers.add(land);
            
            land.setOpacity(0.5);
            
            BasicModel model = new BasicModel();
            model.setLayers(layers);
            wwd.setModel(model);
        }
    }

    public static void main(String[] args)
    {
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                AppFrame frame = new AppFrame();
                frame.setVisible(true);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });
    }
}