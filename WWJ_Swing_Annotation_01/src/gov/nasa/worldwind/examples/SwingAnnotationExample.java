package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.SwingAnnotationLayer;
import gov.nasa.worldwind.layers.Earth.BMNGOneImage;
import gov.nasa.worldwind.layers.FogLayer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.StarsLayer;
import gov.nasa.worldwind.render.SwingAnnotation;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JFrame;

/**
 * <p>Example using SwingAnnotation as annotations. Currently there are three
 * requirements:
 * <ul>
 * <li>You must use JDK6u12 which allows merge heavyweidth and lightweight 
 * components.</li>
 * <li>The container on which WorldWindowGLCanvas resides must use a null layout.</li>
 * <li>The WorldWindowGLCanvas component must be added to a container befare 
 * creating any annotation. The SwingAnnotation are implemented using JPanel
 * which are added to the same container like the WorldWindowGLCanvas.</li>
 * </p>
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class SwingAnnotationExample
{

    private static class AppFrame extends javax.swing.JFrame
    {

        private final LayerList mainlayers;
        private WorldWindowGLCanvas wwd;

        public AppFrame()
        {
            wwd = new WorldWindowGLCanvas();
            this.addComponentListener(new ComponentAdapter()
            {

                @Override
                public void componentResized(ComponentEvent e)
                {
                    wwd.setSize(new java.awt.Dimension(e.getComponent().getSize()));
                }
            });

            // NOTE: The container where the WWJ canvas resides must use a null layout.
            this.getContentPane().setLayout(null);
            this.getContentPane().add(wwd);

            BasicModel model = new BasicModel();
            wwd.setModel(model);

            // Create one set of layers.
            mainlayers = new LayerList();
            mainlayers.add(new StarsLayer());
            mainlayers.add(new SkyGradientLayer());
            mainlayers.add(new FogLayer());
            mainlayers.add(new BMNGOneImage());

            // Create annotation layer
            SwingAnnotationLayer sal = new SwingAnnotationLayer(wwd);
            SwingAnnotation swingannot = new SwingAnnotation(Position.fromDegrees(40, 0, 0));
            swingannot.setSize(300, 200);            
            swingannot.setPanel(new SwingAnnotationPanel());
            sal.addAnnotation(swingannot);

            swingannot = new SwingAnnotation(Position.fromDegrees(10, 40, 0));
            swingannot.setPanel(new SwingAnnotationPanel());
            sal.addAnnotation(swingannot);

            swingannot = new SwingAnnotation(Position.fromDegrees(0, -10, 0));
            swingannot.setPanel(new SwingAnnotationPanel());
            sal.addAnnotation(swingannot);

            mainlayers.add(sal);

            model.setLayers(mainlayers);

            this.pack();
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
