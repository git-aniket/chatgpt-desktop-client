package nl.vu.psy.ams.suite.data.structures.file5fs;

import nl.vu.psy.ams.suite.tools.UnsignedByteBuffer;
/*
 * Time zone info from 5fs file (copied from 5fs code)
 */
public class Ams5fsTimeZoneDate {
	int	m_month;		// 00 - Month (0 - 11; January = 0)
	int	m_week;		// 01 - Weeknumber (1 - 5, 5 is always last week in
							// month)
	int	m_dayOfWeek;	// 02 - day of week (0 - 6, 0 is sunday)
	int	m_hour;		// 03 - Hours since midnight (0 - 23)

	public Ams5fsTimeZoneDate() {

	}

	public Ams5fsTimeZoneDate(UnsignedByteBuffer buff) {
		m_month = buff.getUByte();
		m_week = buff.getUByte();
		m_dayOfWeek = buff.getUByte();
		m_hour = buff.getUByte();
	}

}
