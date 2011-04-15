package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.layers.RenderableLayer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import gov.nasa.worldwind.render.Prism;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.FlatOrbitView;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * <p>Prism objects example.</p>
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class PrismObjects extends ApplicationTemplate
{

    public static class AppFrame extends ApplicationTemplate.AppFrame
    {

        private static final String LIMITS_FILE = "data/limits.txt";
        private static final String POLYGON_SEPARATOR = "-";
        private static final String COORD_SEPARATOR = ",";
        private JList list = null;
        private static DefaultListModel model = new DefaultListModel();

        public AppFrame()
        {
            super(true, false, false);

            setSize(900, 600);

            RenderableLayer layer = new RenderableLayer();
            readWorldLimits(layer);
            ApplicationTemplate.insertAfterPlacenames(this.getWwd(), layer);

            this.getContentPane().add(this.makeControlPanel(), BorderLayout.WEST);
        }

        private JPanel makeControlPanel()
        {

            list = new JList(model);
            list.addListSelectionListener(new ListSelectionListener()
            {

                public void valueChanged(ListSelectionEvent e)
                {
                    PrismElement element = (PrismElement) list.getSelectedValue();
                    if (element != null)
                    {
                        Prism prism = element.getPrism();
                        btColor.setBackground(prism.getColor());
                        slAlpha.setValue(prism.getColor().getAlpha());
                        btWireColor.setBackground(prism.getWiredColor());
                        slWireAlpha.setValue(prism.getWiredColor().getAlpha());
                        spLineWidth.setValue(prism.getLineWidth());
                        ckFilled.setSelected(prism.isFilled());
                        slScale.setValue((int) prism.getTopElevation());
                    }
                }
            });

            JScrollPane scroll = new JScrollPane(list);
            JPanel settings = makeSettingPanel();

            JPanel cPanel = new JPanel(new BorderLayout(10, 10));

            cPanel.add(scroll, BorderLayout.CENTER);
            cPanel.add(settings, BorderLayout.SOUTH);
            cPanel.setBorder(new CompoundBorder(new TitledBorder("Prisms"), new EmptyBorder(20, 10, 20, 10)));

            return cPanel;
        }
        private JButton btColor = new JButton("");
        private JSlider slAlpha = new JSlider(0, 255, 200);
        private JButton btWireColor = new JButton("");
        private JSlider slWireAlpha = new JSlider(0, 255, 200);
        private JSpinner spLineWidth = new JSpinner(new SpinnerNumberModel(1, 1, 5, 0.2));
        private JCheckBox ckFilled = new JCheckBox("Filled", true);
        private JSlider slScale = new JSlider(0, 10000000, 300000);

        private JPanel makeSettingPanel()
        {

            final WorldWindowGLCanvas wwd = this.getWwd();
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(0, 1));

            // Color
            final JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            colorPanel.add(new JLabel("Color:"));
            btColor.addActionListener(new ActionListener()
            {

                public void actionPerformed(ActionEvent event)
                {
                    Color c = JColorChooser.showDialog(colorPanel,
                            "Choose a color...", ((JButton) event.getSource()).getBackground());
                    if (c != null)
                    {
                        JButton button = (JButton) event.getSource();
                        button.setBackground(c);

                        Prism p = getSelectedPrism();
                        if (p != null)
                        {
                            Color oc = p.getColor();
                            Color nc = new Color(c.getRed(), c.getGreen(), c.getBlue(), oc.getAlpha());
                            p.setColor(nc);
                            wwd.redrawNow();
                        }
                    }
                }
            });
            colorPanel.add(btColor);

            // Color alpha
            final JPanel alphaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            alphaPanel.add(new JLabel("Alpha:"));
            slAlpha.addChangeListener(new ChangeListener()
            {

                public void stateChanged(ChangeEvent e)
                {
                    Prism p = getSelectedPrism();
                    if (p != null)
                    {
                        JSlider slider = (JSlider) e.getSource();
                        Color c = p.getColor();
                        Color nc = new Color(c.getRed(), c.getGreen(), c.getBlue(), slider.getValue());
                        p.setColor(nc);
                        wwd.redrawNow();
                    }
                }
            });
            alphaPanel.add(slAlpha);

            // Wire color
            final JPanel wiredColorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            wiredColorPanel.add(new JLabel("Wired Color:"));
            btWireColor.addActionListener(new ActionListener()
            {

                public void actionPerformed(ActionEvent event)
                {
                    Color c = JColorChooser.showDialog(wiredColorPanel,
                            "Choose a color...", ((JButton) event.getSource()).getBackground());
                    if (c != null)
                    {
                        JButton button = (JButton) event.getSource();
                        button.setBackground(c);

                        Prism p = getSelectedPrism();
                        if (p != null)
                        {
                            Color oc = p.getWiredColor();
                            Color nc = new Color(c.getRed(), c.getGreen(), c.getBlue(), oc.getAlpha());
                            p.setWiredColor(nc);
                            wwd.redrawNow();
                        }
                    }
                }
            });
            wiredColorPanel.add(btWireColor);

            // Wire alpha
            final JPanel wiredAlphaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            wiredAlphaPanel.add(new JLabel("Wired Alpha:"));
            slWireAlpha.addChangeListener(new ChangeListener()
            {

                public void stateChanged(ChangeEvent e)
                {
                    Prism p = getSelectedPrism();
                    if (p != null)
                    {
                        JSlider slider = (JSlider) e.getSource();
                        Color c = p.getWiredColor();
                        Color nc = new Color(c.getRed(), c.getGreen(), c.getBlue(), slider.getValue());
                        p.setWiredColor(nc);
                        wwd.redrawNow();
                    }
                }
            });
            wiredAlphaPanel.add(slWireAlpha);

            // Line width
            final JPanel lineWidthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            lineWidthPanel.add(new JLabel("Line Width:"));
            spLineWidth.addChangeListener(new ChangeListener()
            {

                public void stateChanged(ChangeEvent e)
                {
                    Prism p = getSelectedPrism();
                    if (p != null)
                    {
                        JSpinner spinner = (JSpinner) e.getSource();
                        p.setLineWidth((Double) spinner.getValue());
                        wwd.redrawNow();
                    }
                }
            });
            lineWidthPanel.add(spLineWidth);

            // Fill
            final JPanel fillPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            ckFilled.addChangeListener(new ChangeListener()
            {

                public void stateChanged(ChangeEvent e)
                {
                    Prism p = getSelectedPrism();
                    if (p != null)
                    {
                        JCheckBox check = (JCheckBox) e.getSource();
                        p.setFilled(check.isSelected());
                        wwd.redrawNow();
                    }
                }
            });
            fillPanel.add(spLineWidth);

            // Scale
            final JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            scalePanel.add(new JLabel("Scale:"));
            slScale.addChangeListener(new ChangeListener()
            {

                public void stateChanged(ChangeEvent e)
                {
                    Prism p = getSelectedPrism();
                    if (p != null)
                    {
                        JSlider slider = (JSlider) e.getSource();
                        p.setTopElevation(slider.getValue());
                        wwd.redrawNow();
                    }
                }
            });
            scalePanel.add(slScale);

            panel.add(colorPanel);
            panel.add(alphaPanel);
            panel.add(wiredColorPanel);
            panel.add(wiredAlphaPanel);
            panel.add(lineWidthPanel);
            panel.add(fillPanel);
            panel.add(scalePanel);

            return panel;
        }

        /**
         * Returns the selected Prism object or null otherwise.
         * @return
         */
        private Prism getSelectedPrism()
        {
            Object o = list.getSelectedValue();
            if (o != null)
            {
                PrismElement element = (PrismElement) o;
                return element.getPrism();
            }

            return null;
        }

        /**
         * Read world limits file and creates prism objects.
         * @param layer
         */
        private void readWorldLimits(RenderableLayer layer)
        {
            try
            {
                Random generator = new Random();
                double baseElevation = 150e3;

                InputStream stream = this.getClass().getResourceAsStream("/" + LIMITS_FILE);
                BufferedReader in = new BufferedReader(new InputStreamReader(stream));
                String str;
                String name = "";
                ArrayList<LatLon> positions = new ArrayList<LatLon>();
                while ((str = in.readLine()) != null)
                {
                    if (str.equals(POLYGON_SEPARATOR))
                    {
                        if (positions.size() > 4)
                        {

                            double topElevation = baseElevation + generator.nextInt(1000000);
                            int r,  g,  b;
                            r = generator.nextInt(255);
                            g = generator.nextInt(255);
                            b = generator.nextInt(255);
                            Color color = new Color(r, g, b, 200);

                            Prism prism = new Prism(positions, topElevation, color);
                            layer.addRenderable(prism);

                            model.addElement(new PrismElement(name, prism));
                        }
                        positions = new ArrayList<LatLon>();

                        continue;
                    }

                    if (str.matches("NAME:.*"))
                    {
                        name = str.split(":")[1].trim();
                    }
                    else
                    {
                        String[] coords = str.split(COORD_SEPARATOR);

                        double lat = Double.parseDouble(coords[0]);
                        double lon = Double.parseDouble(coords[1]);

                        positions.add(LatLon.fromDegrees(lat, lon));
                    }
                }
                // Create the last polygon
                if (positions.size() > 4)
                {
                    double topElevation = baseElevation + generator.nextInt(1000000);
                    int r,  g,  b;
                    r = generator.nextInt(255);
                    g = generator.nextInt(255);
                    b = generator.nextInt(255);
                    Color color = new Color(r, g, b, 200);

                    Prism prism = new Prism(positions, topElevation, color);
                    layer.addRenderable(prism);

                    model.addElement(new PrismElement(name, prism));
                }
                in.close();
            }
            catch (IOException e)
            {
                Logging.logger().severe(e.getMessage());
            }
        }
    }

    /**
     * Stores a Prism object with an identifier name.
     */
    public static class PrismElement
    {

        private String name = "";
        private Prism prism = null;

        public PrismElement(String name, Prism prism)
        {
            this.name = name;
            this.prism = prism;
        }

        public String getName()
        {
            return name;
        }

        public Prism getPrism()
        {
            return prism;
        }

        @Override
        public String toString()
        {
            return this.name;
        }
    }

    public static void main(String[] args)
    {
        // Uncomment to use a flat world
        Configuration.setValue(AVKey.GLOBE_CLASS_NAME, EarthFlat.class.getName());
        Configuration.setValue(AVKey.VIEW_CLASS_NAME, FlatOrbitView.class.getName());

        ApplicationTemplate.start("World Wind Prism Objects", AppFrame.class);
    }
}
