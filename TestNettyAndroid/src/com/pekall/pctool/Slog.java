package com.pekall.pctool;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;
/**
 * This class defines the Logger
 */

public final class Slog {
	private static String TAG = "PekallPhoneManager";

	private static boolean USE_DETAIL_LOG = true;	
	private static boolean USE_IS_LOGGABLE = false;
	private static boolean USE_THREAD_ID = false;
	
	public static void setTag(String tag) {
		TAG = tag;
	}
	
	public static void initLog(String tag, boolean useDetailLog, boolean useIsLoggable, boolean useThreadId) {
		TAG = tag;
		USE_DETAIL_LOG = useDetailLog;
		USE_IS_LOGGABLE = useIsLoggable;
		USE_THREAD_ID = useThreadId;
	}

	@SuppressWarnings(value="all")
	private static boolean isLoggable(int level) {
		return !USE_IS_LOGGABLE || android.util.Log.isLoggable(TAG, level);
	}

	private Slog() {
	}

	private static String buildMsg(String msg) {
		StringBuilder buffer = new StringBuilder();

		if (USE_DETAIL_LOG) {
			final StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[4];
			
			buffer.append("[ ");
			
			if (USE_THREAD_ID) {
			    buffer.append(Thread.currentThread().getName());
			    buffer.append(": ");
			}
			buffer.append(stackTraceElement.getFileName());
			buffer.append(": ");
			buffer.append(stackTraceElement.getLineNumber());
			buffer.append(" ] _____ ");
			buffer.append(stackTraceElement.getMethodName());
		}

		buffer.append(" - ");

		buffer.append(msg);

		return buffer.toString();
	}

	public static void i(String msg) {
		if (isLoggable(INFO)) {
			android.util.Log.i(TAG, buildMsg(msg));
		}
	}

	public static void e(String msg) {
		if (isLoggable(ERROR)) {
			android.util.Log.e(TAG, buildMsg(msg));
		}
	}

	public static void e(String msg, Exception e) {
		if (isLoggable(ERROR)) {
			android.util.Log.e(TAG, buildMsg(msg), e);
		}
	}

	public static void d(String msg) {
		if (isLoggable(DEBUG)) {
			android.util.Log.d(TAG, buildMsg(msg));
		}
	}

	public static void v(String msg) {
		if (isLoggable(VERBOSE)) {
			android.util.Log.v(TAG, buildMsg(msg));
		}
	}

	public static void w(String msg) {
		if (isLoggable(WARN)) {
			android.util.Log.w(TAG, buildMsg(msg));
		}
	}

	public static void w(String msg, Exception e) {
		if (isLoggable(WARN)) {
			android.util.Log.w(TAG, msg, e);
		}
	}
}

