package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.Position;
import java.awt.Dimension;

/**
 * AnnotatedIcon is a special kind of icon that can show an annotation
 * pointing to it. The original idea was to develop an icon that when the mouse
 * was over or rollover show some extended information. Note that the same
 * effect can be achieved using only annotations and changing its attributes
 * when the mouse event occurs 
 * (http://forum.worldwindcentral.com/showpost.php?p=53252&postcount=8).
 * <br/>
 * The annotation associated with the icons can have any posistion but take
 * into account it will be modified by the layer so that the annotation points
 * to the icon.
 * <br/>
 * Note, there is an important restriccion to work with AnnotatedIcons and it is,
 * the AnnotatedIconLayer needs to know the size of the icon to calculate the
 * position of the annotation, so that it can show it pointing on the icon.
 * The problem is the icons 'getSize' method can return null, indicating they 
 * use the size of the texture.
 * To avoid this problem AnnotatedIcons sets forces in the constructor a default
 * size for the icons.
 * 
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class AnnotatedIcon extends UserFacingIcon
{

    private Annotation annotation = null;   // Annotation to be shown
    private boolean showAnnotation = false;  // By default the annotation is hidden.
    private Dimension dimension = new Dimension(24,24);

    /**
     * Creates a new instance setting the icons size to the default size.
     * @param imageSource
     * @param iconPosition
     */
    public AnnotatedIcon(Object imageSource, Position iconPosition)
    {
        super(imageSource, iconPosition);
        this.setSize(dimension);
    }

    /**
     * Creates a new instance setting the icons size to the default size.
     * @param iconPath
     * @param iconPosition
     */
    public AnnotatedIcon(String iconPath, Position iconPosition)
    {
        super(iconPath, iconPosition);
        this.setSize(dimension);
    }

    /**
     * Returns the annotation to be shown by the icon. It may be null.
     * @return
     */
    public Annotation getAnnotation()
    {
        return annotation;
    }

    /**
     * Sets the annotation to be used by the icon.
     * @param annotation
     */
    public void setAnnotation(Annotation annotation)
    {
        this.annotation = annotation;
    }

    /**
     * Returns if the annotation must be shown.
     * @return
     */
    public boolean isShowAnnotation()
    {
        return showAnnotation;
    }

    /**
     * Set if the annotation must be shown.
     * @param showAnnotation
     */
    public void setShowAnnotation(boolean showAnnotation)
    {
        this.showAnnotation = showAnnotation;
    }
}
