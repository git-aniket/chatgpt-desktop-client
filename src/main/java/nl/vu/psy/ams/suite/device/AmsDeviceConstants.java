package nl.vu.psy.ams.suite.device;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
/*
 * Constants used by the AMS device, and in communication.
 * Copied from original 5fs program.
 */
public class AmsDeviceConstants {

	public static final int					AMSII_CMDINDEX_DO_NOTHING			= 0;			// used
																								// for
																								// init
																								// value
																								// for
																								// command
																								// variable
																								// in
																								// device
																								// software
	public static final int					AMSII_CMDINDEX_ABORT_ONLINE			= 1;			// stop
																								// online
																								// mode;
																								// stop
																								// collecting
																								// online
																								// data
																								// in
																								// buffer
	public static final int					AMSII_CMDINDEX_CONTINUE_ONLINE		= 2;			// prevent
																								// automatic
																								// abortion
																								// (timeout)
																								// of
																								// online
																								// mode
	public static final int					AMSII_CMDINDEX_FORCE_BUF_OVERFLOW	= 3;			// force
																								// a
																								// buffer
																								// overflow
																								// for
																								// testing
																								// purposes
	public static final int					AMSII_CMDINDEX_ERASE_SETTINGS_SEC	= 4;			// erase
																								// settings
																								// sector
																								// from
																								// flash
	public static final int					AMSII_CMDINDEX_START_RECORDING		= 5;			// start
																								// new
																								// recording
																								// if
																								// possible
	public static final int					AMSII_CMDINDEX_STOP_RECORDING		= 6;			// stop
																								// recording
																								// if
																								// running
	public static final int					AMSII_CMDINDEX_RESET				= 7;			// request
																								// reboot
																								// or
																								// erase
																								// data,
																								// index
																								// (AMSII_RESET_*)
	public static final int					AMSII_CMDINDEX_ERASE_STATICS_SEC	= 8;			// erase
																								// statics
																								// sector
																								// from
																								// flash
	public static final int					AMSII_CMDINDEX_FORCE_WATCHDOG		= 9;			// force
																								// a
																								// watchdog
																								// reset
																								// for
																								// testing
																								// purposes
	public static final int					AMSII_CMDINDEX_TERMINATE_SIMULATOR	= 255;			// for
																								// windows
																								// debugging
																								// purposes
																								// only
	public static final int					VUAMSII_DEVSTATE_UNKNOWN			= -1;			// unkown
																								// state
																								// (for
																								// windows)
	public static final int					VUAMSII_DEVSTATE_INITIALIZING		= 0;			// Startup
																								// state
	public static final int					VUAMSII_DEVSTATE_WAITFORCF			= 1;
	public static final int					VUAMSII_DEVSTATE_COVEROPEN			= 2;
	public static final int					VUAMSII_DEVSTATE_WAITFORSTART		= 3;
	public static final int					VUAMSII_DEVSTATE_RECORDING			= 4;
	public static final int					VUAMSII_DEVSTATE_CFFULL				= 5;
	public static final int					VUAMSII_DEVSTATE_BATTERYEMPTY		= 6;			// End
																								// state
																								// (only
																								// exit
																								// is
																								// RESET)
	public static final int					AMSII_PARMINDEX_AMSSTATE			= 100;
	public static final int					AMSII_PARMINDEX_MSCOUNTER			= 101;
	public static final int					AMSII_PARMINDEX_A25					= 102;
	public static final int					AMSII_PARMINDEX_DZ					= 103;
	public static final int					AMSII_PARMINDEX_SCLRAW				= 104;
	public static final int					AMSII_PARMINDEX_ECG					= 105;
	public static final int					AMSII_PARMINDEX_XT1					= 106;
	public static final int					AMSII_PARMINDEX_PCG					= 107;
	public static final int					AMSII_PARMINDEX_IRES				= 108;
	public static final int					AMSII_PARMINDEX_BATRAW				= 109;
	public static final int					AMSII_PARMINDEX_Z0RAW				= 110;
	public static final int					AMSII_PARMINDEX_YMT					= 111;
	public static final int					AMSII_PARMINDEX_XMT					= 112;
	public static final int					AMSII_PARMINDEX_LDRRAW				= 113;
	public static final int					AMSII_PARMINDEX_XMT_DC				= 114;
	public static final int					AMSII_PARMINDEX_XMT_AC				= 115;
	public static final int					AMSII_PARMINDEX_YMT_DC				= 116;
	public static final int					AMSII_PARMINDEX_YMT_AC				= 117;
	public static final int					AMSII_PARMINDEX_Z0_AVG				= 118;
	public static final int					AMSII_PARMINDEX_BAT_AVG				= 119;
	public static final int					AMSII_PARMINDEX_LDR_AVG				= 120;
	public static final int					AMSII_PARMINDEX_SCL_FILT			= 121;
	public static final int					AMSII_PARMINDEX_NRFREESECTORS		= 122;
	public static final int					AMSII_PARMINDEX_NRSECTORS			= 123;
	public static final int					AMSII_PARMINDEX_BAT_MON				= 124;

	public static final int					AMSII_PARMINDEX_DEVICEID			= 200;
	public static final int					AMSII_PARMINDEX_VERSION				= 201;
	public static final int					AMSII_PARMINDEX_SERIALNR			= 202;

	public static final int					CH_A25								= 0;			// ADC16
																								// switch
																								// 0
	public static final int					CH_DZ								= 1;			// ADC16
																								// switch
																								// 1
	public static final int					CH_SCL								= 2;			// ADC16
																								// switch
																								// 2
	public static final int					CH_ECG								= 3;			// ADC16
																								// switch
																								// 3
	public static final int					CH_Xt1								= 4;			// ADC12
																								// input
																								// 0
	public static final int					CH_PCG								= 5;			// ADC12
																								// input
																								// 1
	public static final int					CH_Ire								= 6;			// ADC12
																								// input
																								// 2
	public static final int					CH_BAT								= 7;			// ADC12
																								// input
																								// 3
	public static final int					CH_Z0								= 8;			// ADC12
																								// input
																								// 4
	public static final int					CH_YMT								= 9;			// ADC12
																								// input
																								// 5
	public static final int					CH_XMT								= 10;			// ADC12
																								// input
																								// 6
	public static final int					CH_LDR								= 11;			// ADC12
																								// input
																								// 7
	public static final int					CHX_SCL								= 12;			// virtual
																								// channel
	public static final int					CHX_BTA								= 13;			// virtual
																								// channel:
																								// average
																								// BAT
	public static final int					CHX_Z0A								= 14;			// virtual
																								// channel:
																								// average
																								// Z0
	public static final int					CHX_MXA								= 15;			// virtual
																								// channel:
																								// motility
																								// X
																								// AC
	public static final int					CHX_MXD								= 16;			// virtual
																								// channel:
																								// motility
																								// X
																								// DC
	public static final int					CHX_MYA								= 17;			// virtual
																								// channel:
																								// motility
																								// Y
																								// AC
	public static final int					CHX_MYD								= 18;			// virtual
																								// channel:
																								// motility
																								// Y
																								// DC
	public static final int					CHX_LDA								= 19;			// virtual
																								// channel:
																								// average
																								// LDR
	public static final int					CHX_MYR								= 20;			// virtual
																								// channel:
																								// motility
																								// Y
																								// RAW
																								// (can
																								// be
																								// sampled
																								// slower
																								// then
																								// CH_YMT,
																								// which
																								// has
																								// to
																								// be
																								// 1kHz
																								// for
																								// Sampleprocessing)
	public static final int					N_CHANNELS							= 21;

	public static final int					AMSII_FLAG_BUZDISABLE				= 0x00000001;	// set
																								// =
																								// buzzer
																								// disabled
	public static final int					AMSII_FLAG_LEDDISABLE				= 0x00000002;	// set
																								// =
																								// LED
																								// disabled
	public static final int					AMSII_FLAG_UNMOUNTMODE				= 0x00000004;	// set
																								// =
																								// manual
																								// (not
																								// set
																								// =
																								// LDR)
	public static final int					AMSII_FLAG_SCLAC					= 0x00000008;	// set
																								// =
																								// AC
																								// (not
																								// set
																								// =
																								// DC)
	public static final int					AMSII_FLAG_BATEMPTY_BEEP			= 0x00000010;	// set
																								// =
																								// Battery
																								// empty
																								// beep
																								// disabled
																								// (the
																								// check
																								// can't
																								// be
																								// disabled)
	public static final int					AMSII_FLAG_BATLOW_BEEP				= 0x00000020;	// set
																								// =
																								// Battery
																								// low
																								// beep
																								// disabled
	public static final int					AMSII_FLAG_BATLOW					= 0x00000040;	// set
																								// =
																								// Battery
																								// low
																								// check
																								// disabled
	public static final int					AMSII_FLAG_ECGRANGE_BEEP			= 0x00000080;	// set
																								// =
																								// ECG
																								// out
																								// of
																								// range
																								// beep
																								// disabled
	public static final int					AMSII_FLAG_ICGRANGE_BEEP			= 0x00000100;	// set
																								// =
																								// ICG
																								// out
																								// of
																								// range
																								// beep
																								// disabled
	public static final int					AMSII_FLAG_SCLRANGE_BEEP			= 0x00000200;	// set
																								// =
																								// SCL
																								// out
																								// of
																								// range
																								// beep
																								// disabled
	public static final int					AMSII_FLAG_ECGRANGE					= 0x00000400;	// set
																								// =
																								// ECG
																								// range
																								// check
																								// disabled
	public static final int					AMSII_FLAG_ICGRANGE					= 0x00000800;	// set
																								// =
																								// ICG
																								// range
																								// check
																								// disabled
	public static final int					AMSII_FLAG_SCLRANGE					= 0x00001000;	// set
																								// =
																								// SCL
																								// range
																								// check
																								// disabled
	public static final int					AMSII_FLAG_AUTOSTARTSTOPRESET		= 0x00002000;	// set
																								// =
																								// Auto
																								// start
																								// after
																								// any
																								// stop
																								// of
																								// file
																								// or
																								// reset
	public static final int					AMSII_FLAG_AUTOSTARTCOVER			= 0x00004000;	// set
																								// =
																								// Auto
																								// start
																								// after
																								// battery
																								// cover
																								// is
																								// closed
	public static final int					AMSII_FLAG_STARTBUTTON				= 0x00008000;	// set
																								// =
																								// start
																								// button
																								// DISABLED
	public static final int					AMSII_FLAG_STOPBUTTON				= 0x00010000;	// set
																								// =
																								// stop
																								// button
																								// DISABLED

	public static final Map<Long, String>	DIVIDERMAP;
	public static final Map<String, Long>	REVERSEDIVIDERMAP;

	static {
		Map<Long, String> tempMap = new HashMap<Long, String>();
		tempMap.put(0L, "Off");
		tempMap.put(60000L, "1/60 Hz");
		tempMap.put(32768L, "1/33 Hz");
		tempMap.put(30000L, "1/30 Hz");
		tempMap.put(20000L, "1/20 Hz");
		tempMap.put(15000L, "1/15 Hz");
		tempMap.put(10000L, "1/10 Hz");
		tempMap.put(8192L, "1/8.2 Hz");
		tempMap.put(5000L, "1/5 Hz");
		tempMap.put(1024L, "1.024 Hz");
		tempMap.put(1000L, "1 Hz");
		tempMap.put(500L, "2 Hz");
		tempMap.put(200L, "5 Hz");
		tempMap.put(100L, "10 Hz");
		tempMap.put(10L, "100 Hz");
		tempMap.put(8L, "125 Hz");
		tempMap.put(5L, "200 Hz");
		tempMap.put(4L, "250 Hz");
		tempMap.put(2L, "500 Hz");
		tempMap.put(1L, "1000 Hz");
		DIVIDERMAP = Collections.unmodifiableMap(tempMap);

		Map<String, Long> tempMap2 = new HashMap<String, Long>();
		for (Entry<Long, String> entry : DIVIDERMAP.entrySet()) {
			tempMap2.put(entry.getValue(), entry.getKey());
		}
		REVERSEDIVIDERMAP = Collections.unmodifiableMap(tempMap2);
	}

	public static final int					BAUDRATE							= 38400;
	public static final int					NCHANNELS							= 21;

	public static final int					AMSII_TAG_RESETCOM					= 0x00;		// send
																								// after
																								// communication
																								// error
																								// ???
	public static final int					AMSII_TAG_GETPARAMETER				= 0x01;
	public static final int					AMSII_TAG_SETPARAMETER				= 0x02;
	public static final int					AMSII_TAG_GETSTRING					= 0x03;			// send
																								// word
																								// as
																								// zero
																								// terminated
																								// string
	public static final int					AMSII_TAG_SETSTRING					= 0x04;		// request
																								// word,
																								// includes
																								// word
																								// index
	public static final int					AMSII_TAG_GETTIME					= 0x05;		// request
																								// current
																								// date
																								// and
																								// time
	public static final int					AMSII_TAG_SETTIME					= 0x06;		// send
																								// current
																								// date
																								// and
																								// time
																								// as
																								// AMSII_Time
	public static final int					AMSII_TAG_GETSETTINGS				= 0x07;		// request
																								// current
																								// settings
	public static final int					AMSII_TAG_SETSETTINGS				= 0x08;		// send
																								// settings
	public static final int					AMSII_TAG_GETSTATICS				= 0x09;		// request
																								// current
																								// statics
	public static final int					AMSII_TAG_SETSTATICS				= 0x0a;		// send
																								// statics
	public static final int					AMSII_TAG_COMMAND					= 0x0b;		// index
																								// contains
																								// the
																								// actual
																								// command,
																								// [DATA]=char
																								// value
	public static final int					AMSII_TAG_ONLINE_START				= 0x0c;		// send
																								// AMSII_OnlineStart
	public static final int					AMSII_TAG_ONLINE_DATA				= 0x0d;		// send
																								// AMSII_OnlineData
	public static final int					AMSII_TAG_SERIAL_EVENT				= 0x0e;		// send
																								// AMSII_Event
	public static final int					AMSII_TAG_RESERVED					= 0xff;		// might
																								// be
																								// used
																								// to
																								// indicate
																								// a
																								// longer
																								// tag
																								// when
																								// 126
																								// codes
																								// are
																								// not
																								// sufficient

	public static final int					AMSII_TAG_NACK						= 0x7f;		// nack.
																								// cannot
																								// perform
																								// action,
																								// includes
																								// received
																								// tag
																								// and
																								// long
																								// error
																								// code
	public static final int					AMSII_TAG_ACKBIT					= 0x80;		// acknowledge
																								// action

}
