Author: 
-------
Antonio Santiago <asantiagop@gmail.com>

Description:
------------
LayerSet allows create like tree of layers. It is a subclass of LayerList and also implements
the Layer interface, so you can create composite LayerSets. Example: 

MainLayer
 |
 |-- GroupA
 |      |-- IconLayer
 |      |-- RenderableLayer
 |
 |-- GroupB
        |-- ScalebarLayer
        |-- WorldmapLayer

Because it is a LayerList you can pass to any Model class.

The operation setOpacity, setEnable, render and so on, makes changes iteratively in all
contained elements.


Changes from original WWJ 0.3.0:
--------------------------------

This code doesn't modify the WWJ 0.3.0 at all, you can copy the files in the specified
directories and build the project.

* Added "gov.nasa.worldwind.layers.LayerSet" class.
* Added "gov.nasa.worldwind.examples.LayerSetExample" class.


