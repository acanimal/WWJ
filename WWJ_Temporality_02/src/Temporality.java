package gov.nasa.worldwind.util;

import gov.nasa.worldwind.avlist.AVListImpl;
import java.util.Calendar;

/**
 * Temporality represents a time of validity from an initial to a final
 * time stamp.
 *
 * @author Antonio Santiago [asantiagop(at)gmail.com]
 */
public class Temporality extends AVListImpl
{
    // TODO - Add this properties to AVKey ???
    /**
     * Initial timestamp property.
     */
    public static final String PROP_INITIAL_TIME_STAMP = "initialTimeStamp";
    /**
     * Final timestamp property.
     */
    public static final String PROP_FINAL_TIME_STAMP = "finalTimeStamp";
    /**
     * The initial timestamp associated with this object (may be null if the
     * object hasn't one).
     */
    private Calendar initTimeStamp = null;
    /**
     * The final timestamp associated with this object (may be null if the
     * object hasn't one).
     */
    private Calendar finalTimeStamp = null;

    /**
     * Creates a new instance.
     */
    public Temporality(Calendar initTimeStamp, Calendar finalTimeStamp)
    {
        if (initTimeStamp == null || finalTimeStamp == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Check if the initial time is before the final time.
        if (initTimeStamp.before(finalTimeStamp))
        {
            this.initTimeStamp = initTimeStamp;
            this.finalTimeStamp = finalTimeStamp;
        } else
        {
            this.initTimeStamp = finalTimeStamp;
            this.finalTimeStamp = initTimeStamp;
        }
    }

    /**
     * Creates a new instance.
     */
    public Temporality(Temporality temporality)
    {
        if (temporality == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.initTimeStamp = temporality.initTimeStamp;
        this.finalTimeStamp = temporality.finalTimeStamp;
    }

    /**
     * Sets the temporality.
     * @param temporality
     */
    public void setTemporality(Temporality temporality)
    {
        if (temporality == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        setInitialTimeStamp(temporality.initTimeStamp);
        setFinalTimeStamp(temporality.finalTimeStamp);
    }

    /**
     * Gets the initial timestamp.
     * @return start time
     */
    public Calendar getInitialTimeStamp()
    {
        return initTimeStamp;
    }

    /**
     * Sets the initial timestamp.
     * @param calendar start time
     */
    public void setInitialTimeStamp(Calendar calendar)
    {
        if (calendar == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        initTimeStamp = calendar;
        firePropertyChange(PROP_INITIAL_TIME_STAMP, null, calendar);
    }

    /**
     * Gets the final timestamp.
     * @return final time
     */
    public Calendar getFinalTimeStamp()
    {
        return finalTimeStamp;
    }

    /**
     * Sets the final timestamp.
     * @param calendar final time
     */
    public void setFinalTimeStamp(Calendar calendar)
    {
        if (calendar == null)
        {
            String message = Logging.getMessage("nullValue.ObjectIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        finalTimeStamp = calendar;
        firePropertyChange(PROP_FINAL_TIME_STAMP, null, calendar);
    }

    /**
     * Checks if this Temporality intersects with the specified one.
     * @param temporality
     */
    public boolean intersects(Temporality temporality)
    {
        // By default intersecting with a null temporality is TRUE.
        if (temporality == null)
        {
            return true;
        }

        return !before(temporality) && !after(temporality);
    }

    /**
     * Checks if this Temporality is before the specified one.
     * @param temporality
     */
    public boolean before(Temporality temporality)
    {
        // By default a temporality is before a null one.
        if (temporality == null)
        {
            return true;
        }

        return finalTimeStamp.before(temporality.getInitialTimeStamp());
    }

    /**
     * Checks if this Temporality is after the specified one.
     * @param temporality
     */
    public boolean after(Temporality temporality)
    {
        // By default a temporality is after a null one.
        if (temporality == null)
        {
            return true;
        }

        return initTimeStamp.after(temporality.getFinalTimeStamp());
    }

    /**
     * Checks if this Temporality is equal with the specified one.
     * @param temporality
     */
    public boolean equals(Temporality temporality)
    {
        // By default a temporality is not equal to a null one.
        if (temporality == null)
        {
            return false;
        }

        return initTimeStamp.equals(temporality.getInitialTimeStamp()) && finalTimeStamp.equals(temporality.getFinalTimeStamp());
    }
}