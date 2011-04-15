package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.Angle;

/**
 * Rotable interfaces defines the common functionallities for those objects
 * that can be headed rotated.
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public interface Rotable
{

    public Angle getHeading();

    public void setHeading(Angle heading);
}
