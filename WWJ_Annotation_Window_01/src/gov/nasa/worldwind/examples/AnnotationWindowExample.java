package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AnnotationWindowLayer;
import gov.nasa.worldwind.layers.Earth.BMNGOneImage;
import gov.nasa.worldwind.layers.FogLayer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.StarsLayer;
import gov.nasa.worldwind.render.AnnotationWindow;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Example using JWindow as annotations.
 *
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class AnnotationWindowExample
{

    private static final String WWJ_SPLASH_PATH = "images/400x230-splash-nww.png";
    private static final String GEORSS_ICON_PATH = "images/georss.png";

    private static class AppFrame extends javax.swing.JFrame
    {

        private final LayerList mainlayers;
        private WorldWindowGLCanvas wwd;

        public AppFrame()
        {
            wwd = new WorldWindowGLCanvas();
            wwd.setPreferredSize(new java.awt.Dimension(1000, 800));
            this.getContentPane().add(wwd, java.awt.BorderLayout.CENTER);
            this.pack();

            BasicModel model = new BasicModel();
            wwd.setModel(model);

            // Create one set of layers.
            mainlayers = new LayerList();
            mainlayers.add(new StarsLayer());
            mainlayers.add(new SkyGradientLayer());
            mainlayers.add(new FogLayer());
            mainlayers.add(new BMNGOneImage());

            // Create Window layer
            AnnotationWindowLayer awl = new AnnotationWindowLayer(wwd);
            AnnotationWindow tip = new AnnotationWindow(Position.fromDegrees(40, 0, 0), wwd);
            tip.setPanel(new thePanel());
            awl.addWindow(tip);

            tip = new AnnotationWindow(Position.fromDegrees(10, 40, 0), wwd);
            tip.setPanel(new thePanel());
            awl.addWindow(tip);

            tip = new AnnotationWindow(Position.fromDegrees(-30, -90, 0), wwd);
            tip.setPanel(new thePanel());
            awl.addWindow(tip);

            mainlayers.add(awl);

            model.setLayers(mainlayers);
        }
    }

    private static class thePanel extends JPanel
    {

        public thePanel()
        {
            setLayout(new FlowLayout());
            add(new JButton("button"));
            add(new JTextField(20));
        }
        
    }

    public static void main(String[] args)
    {
        java.awt.EventQueue.invokeLater(new Runnable()
        {

            public void run()
            {
                AppFrame appFrame = new AppFrame();
                appFrame.setSize(600, 500);
                appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                appFrame.setVisible(true);
            }
        });
    }
}
