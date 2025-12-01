package nl.vu.psy.ams.suite.data.structures.file5fs;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nl.vu.psy.ams.suite.tools.UnsignedByteBuffer;
/*
 * Time tag info from 5fs file (copied from 5fs code)
 */
public class Ams5fsTime {

	private int	year;	// 00 - Year (current year minus 1900; can/will be
							// >=100 !!!)
	private int	mon;	// 01 - Month (0 - 11; January = 0)
	private int	mday;	// 02 - Day of month (1 - 31)
	private int	hour;	// 03 - Hours since midnight (0 - 23)
	private int	min;	// 04 - Minutes after hour (0 - 59)
	private int	sec;	// 05 - Seconds after minute (0 - 59)
	private byte	isdst;	// 06 - Daylight saving time (zomertijd/wintertijd):
							// Positive if daylight saving time is in effect;
							// (zomertijd)
							// 0 if daylight saving time is not in effect;
							// (wintertijd)
							// negative if status of daylight saving time is
							// unknown.
	private int	wday;	// 07 - Day of week (0 - 6; Sunday = 0)

	public Ams5fsTime() {

	}

	public Ams5fsTime(Ams5fsTime in) {
		Ams5fsTime retTime = new Ams5fsTime();
		retTime.year = in.year;
		retTime.mon = in.mon;
		retTime.mday = in.mday;
		retTime.hour = in.hour;
		retTime.min = in.min;
		retTime.sec = in.sec;
		retTime.isdst = in.isdst;
		retTime.wday = in.wday;
	}

	public Ams5fsTime(Calendar cal) {
		this.year =  (cal.get(Calendar.YEAR) - 1900);
		this.mon =  (cal.get(Calendar.MONTH));
		this.mday =  (cal.get(Calendar.DAY_OF_MONTH));
		this.hour =  (cal.get(Calendar.HOUR_OF_DAY));
		this.min =  (cal.get(Calendar.MINUTE));
		this.sec =  (cal.get(Calendar.SECOND));
		this.wday =  cal.get(Calendar.DAY_OF_WEEK);
		this.isdst = 0;
	}

	public Ams5fsTime(int[] time) {
		year =  time[0];
		mon =  time[1];
		mday =  time[2];
		hour =  time[3];
		min =  time[4];
		sec =  time[5];
		isdst = (byte) time[6];
		wday =  time[7];
	}

	public Ams5fsTime(UnsignedByteBuffer buff) {
		year = buff.getUByte();
		mon = buff.getUByte();
		mday = buff.getUByte();
		hour = buff.getUByte();
		min = buff.getUByte();
		sec = buff.getUByte();
		isdst = buff.getByte();
		wday = buff.getUByte();
	}

	public void AddMilliseconds(long ms) {
		GregorianCalendar tempDate = new GregorianCalendar(1900 + year, mon, mday, hour, min, sec);
		tempDate.add(Calendar.MILLISECOND, 500);
		long absMS;
		boolean isNegative;
		if (ms < 0) {
			absMS = -ms;
			isNegative = true;
		} else {
			absMS = ms;
			isNegative = false;
		}
		while (absMS > Integer.MAX_VALUE) {
			if (isNegative == true) {
				tempDate.add(Calendar.MILLISECOND, -Integer.MAX_VALUE);
			} else {
				tempDate.add(Calendar.MILLISECOND, Integer.MAX_VALUE);
			}
			absMS -= Integer.MAX_VALUE;
		}
		if (isNegative == true) {
			tempDate.add(Calendar.MILLISECOND, (int) -absMS);
		} else {
			tempDate.add(Calendar.MILLISECOND, (int) absMS);
		}
		this.year =  (tempDate.get(Calendar.YEAR) - 1900);
		this.mon =  (tempDate.get(Calendar.MONTH));
		this.mday =  (tempDate.get(Calendar.DAY_OF_MONTH));
		this.hour =  (tempDate.get(Calendar.HOUR_OF_DAY));
		this.min =  (tempDate.get(Calendar.MINUTE));
		this.sec =  (tempDate.get(Calendar.SECOND));
		this.wday =  tempDate.get(Calendar.DAY_OF_WEEK);
	}

	public int[] toDeviceArray() {
		int[] ret = new int[8];
		ret[0] = year;
		ret[1] = mon;
		ret[2] = mday;
		ret[3] = hour;
		ret[4] = min;
		ret[5] = sec;
		ret[6] = isdst;
		ret[7] = wday;
		return ret;
	}

	public GregorianCalendar toGregorianCalendar() {
		GregorianCalendar retDate = new GregorianCalendar(1900 + year, mon, mday, hour, min, sec);
		retDate.add(Calendar.MILLISECOND, 500);
		return retDate;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Ams5fsTime [year=");
		builder.append(year);
		builder.append(", mon=");
		builder.append(mon);
		builder.append(", mday=");
		builder.append(mday);
		builder.append(", hour=");
		builder.append(hour);
		builder.append(", min=");
		builder.append(min);
		builder.append(", sec=");
		builder.append(sec);
		builder.append(", isdst=");
		builder.append(isdst);
		builder.append(", wday=");
		builder.append(wday);
		builder.append("]");
		return builder.toString();
	}

}
