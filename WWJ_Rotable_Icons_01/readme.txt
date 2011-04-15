Author: 
-------
Antonio Santiago <asantiagop@gmail.com>

Description:
------------

These contribution allows to rotate icons in the same way the compass rotates.
There are many ways to do things but I prefer to create a new Rotable interface
and a new subclass RotableUserFacingIcon that allows to get/set a heading angle.

These code is based on WWJ 0.4.1 version and only modifies a bit the IconRenderer
class adding these peace of code in its 'drawIcon' method:

        if (icon instanceof Rotable)
        {
            Angle heading = ((Rotable) icon).getHeading();
            gl.glRotated(heading.getDegrees(), 0d, 0d, 1d);
            gl.glTranslated(-width / 2, -height / 2, 0d);
        }        

Also, I have created a little example to see how it works.


Changes from original WWJ 0.4.1:
--------------------------------

* Added to 'gov.nasa.worldwind.render.IconRenderer' the above code in the
  'drawIcon' method.

* Created new interface 'gov.nasa.worldwind.render.Rotable'.
* Created new class 'gov.nasa.worldwind.render.RotableUserFacingIcon'.
* Created example class 'gov.nasa.worldwind.examples.RotableIconsExample'        

