Author: 
-------
Antonio Santiago <asantiagop@gmail.com>

Description:
------------
The idea is to allow associate a period of validity to a renderable object. This period
can be represented with the Temporality class, which has an initial and a final Calendar to indicate the valid range where an object must be rendered.

View interface (and AbstractView) acts as camera and also can have associated a period of time,
for example: from October 1951 to October 1952.

When the "camera" has a temporality associated, we can step up or down into these period incrementally.
This step size is so called the "cursor". In the previous example we can define a cursor of
size "one month", "two days" or "20 minutes" to step up or down. The cursor can be moved within
the views temporality with the "go to" method.

Also, when the "camera" has a temporality associated it only renders objects that conforms with:
1- If the object has an associated temporality, then it is only rendered if his temporality is
   inside the View's cursor.
2- If the object hasn't a temporality, then it is always rendered.

Finally I have created a Timeline layer to show the current view temporality and cursor position.


Changes from original WWJ 0.3.0
----------------------------------

This code only modifies a bit the View interface and AbstractView class adding some methods
to set the view temporality and cursor operations.
Follow the next messages to know the changes.

* On file "gov.nasa.worldwind.util.MessageStrings.properties" added line:
    layers.Earth.TimelineLayer.Name=Timeline

* Added "gov.nasa.worldwind.util.TemporalityAdapter" class.

* Added "gov.nasa.worldwind.util.Temporality" class.

* Added "gov.nasa.worldwind.layers.Earth.TimelineLayer" class.

* Modified interface "gov.nasa.worldwind.view.View", added methods:

    public void setTemporality(Temporality temporality);
    public Temporality getTemporality();
    public Temporality getTemporalityCursor();
    public void setTemporalityCursorSize(int type, int value);
    public void temporalityCursorStepUp();
    public void temporalityCursorStepDown();
    public void temporalityCursorGoTo(Calendar calendar);

* Modified class "gov.nasa.worldwind.view.AbstractView", added code to 
  implements previous methods.

* Added example file "gov.nasa.worldwind.examples.TemporalityExample" using LayerSet and Temporality.

