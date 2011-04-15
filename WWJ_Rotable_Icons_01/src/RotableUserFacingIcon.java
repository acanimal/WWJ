package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;

/**
 * RotableUserFacingIcon is a normal UserFacingIcon that can be headed
 * rotated.
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class RotableUserFacingIcon extends UserFacingIcon implements Rotable
{

    private Angle heading = Angle.fromDegrees(0);

    public RotableUserFacingIcon(Object imageSource, Position iconPosition)
    {
        super(imageSource, iconPosition);
    }

    public RotableUserFacingIcon(String iconPath, Position iconPosition)
    {
        super(iconPath, iconPosition);
    }

    /**
     * Get the heading rotation angle.
     * @return
     */
    public Angle getHeading()
    {
        return this.heading;
    }

    /**
     * Set the heading rotation angle.
     * @param heading
     */
    public void setHeading(Angle heading)
    {
        this.heading = heading;
    }
}
