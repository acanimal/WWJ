Author: 
-------
Antonio Santiago <asantiagop@gmail.com>

Description:
------------
This modified version of SurfaceImage allows to load remote images (only http protocol) asynchronously.
Both local and remote images are loaded in a separate thread.
Remote images are cached on disk.
SurfaceImage implements the movable interface so you can move it with the mouse.


Changes from original WWJ 0.3.0:
--------------------------------

* Modified version of "gov.nasa.worldwind.render.SurfaceImage" class.
* Implementation of the interface 'gov.nasa.worldwind.Movable'.
* Added a 'setSector' method to allow modify the sector size and position of the image.



