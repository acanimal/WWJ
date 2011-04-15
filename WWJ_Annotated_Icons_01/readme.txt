Author: 
-------
Antonio Santiago <asantiagop@gmail.com>

Description:
------------
This contribution allows to work with icons that can show an annotation, for
example, when mouse is over and you need to show more complete information.

Note that the same effect can be achieved using only annotations and changing 
its attributes when the mouse event occurs 
(http://forum.worldwindcentral.com/showpost.php?p=53252&postcount=8).

Note, there is an important restriccion to work with AnnotatedIcons and it is,
the AnnotatedIconLayer needs to know the size of the icon to calculate the
position of the annotation, so that it can show it pointing on the icon.
The problem is the icons 'getSize' method can return null, indicating they 
use the size of the texture.
To avoid this problem AnnotatedIcons sets forces in the constructor a default
size for the icons.


Changes from original WWJ 0.4.1:
--------------------------------

* Created class 'gov.nasa.worldwind.render.AnnotatedIcon'.
* Created layer 'gov.nasa.worldwind.layers.AnnotatedIconLayer'.
* Created example 'gov.nasa.worldwind.examples.AnnotatedIconsExample'.


