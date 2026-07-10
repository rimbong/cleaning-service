package com.boot.cleanhub.util.date;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
/**
 * <pre>
    com.boot.cleanhub.util.date.DateUtil 사용 바람
 * </pre>
 * @deprecated
 * @author In-seong Hwang
 * @version 1.0
 */
@Deprecated
public class DateMo {

	/**
	 * <pre>
	 * 현재 일자를 반환한다.(yyyy + flag + MM + flag + dd)
	 * </pre>
	 * 
	 * @param flag
	 *            : 날짜 구분자 (/,- 등)
	 * 
	 * @return 현재일자(yyyy + flag + MM + flag + dd)
	 */
	public static String getDate(String flag) {

		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		return new SimpleDateFormat(dateFormat).format(new Date());
	}

	/**
	 * <pre>
	 * 현재 일자를 반환한다.(yyyyMMdd)
	 * </pre>
	 * 
	 * @return 현재일자(yyyyMMdd)
	 */
	public static String getDate() {

		String dateFormat = "yyyyMMdd";
		return new SimpleDateFormat(dateFormat).format(new Date());
	}
	
	/**
	 * <pre>
	 * 현재일자 기준으로 +hour(시간) 날짜를 계산하여 반환한다. (yyyyMMdd)
	 * </pre>
	 * 
	 * @param day
	 *            : 이동할 일자 (+n일)
	 * 
	 * @return 현재일자 + day (yyyy MM dd HH mm ss)
	 */
	public static String getDateTime24MissAfter(int hour, String flag, String flag2) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, hour);
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd" + " HH" + flag2 + "mm" + flag2 + "ss";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}
	
	/**
	 * <pre>
	 * 특정일자 기준으로 +hour(시간) 날짜를 계산하여 반환한다. (yyyyMMdd)
	 * </pre>
	 * 
	 * @param day
	 *            : 이동할 일자 (+n일)
	 * 
	 * @return 특정일자 + day (yyyy MM dd HH mm ss)
	 */
	public static String getSpecificDateTime24MissAfter(String date, int hour, String flag, String flag2) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd" + " HH" + flag2 + "mm" + flag2 + "ss";
		
		try {
			cal.setTime(simpleDateFormat.parse(date));
			cal.add(Calendar.HOUR, hour);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 현재 년도를 반환한다.(yyyy)
	 * </pre>
	 * 
	 * @return 현재년도(yyyy)
	 */
	public static String getYear() {

		String dateFormat = "yyyy";
		return new SimpleDateFormat(dateFormat).format(new Date());
	}

	/**
	 * <pre>
	 * 특정 날짜의 년를 반환한다.(yyyy)
	 * </pre>
	 * 
	 * @param date
	 *            : yyyy-MM-dd 형식의 날짜
	 * @return 현재년도(yyyy)
	 * @throws ParseException
	 */
	public static String getYear(String date) {

		String dateFormat = "yyyy";
		String result = "";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			result = new SimpleDateFormat(dateFormat).format(simpleDateFormat.parse(date));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * <pre>
	 * 현재 월를 반환한다.(mm)
	 * </pre>
	 * 
	 * @return 현재 월(mm)
	 */
	public static String getMonth() {

		String dateFormat = "MM";
		return new SimpleDateFormat(dateFormat).format(new Date());
	}

	/**
	 * <pre>
	 * 특정 패턴에 의해서 현재 월를 반환한다.
	 * </pre>
	 * 
	 * @return 현재 월(mm)
	 */
	public static String getMonthPattern(String pattern) {

		String dateFormat = pattern;
		return new SimpleDateFormat(dateFormat).format(new Date());
	}
	
	/**
	 * <pre>
	 * 특정일자의 월를 반환한다.(mm)
	 * </pre>
	 * 
	 * @param date
	 *            : : yyyy-MM-dd 형식의 날짜
	 * @return 현재 월(mm)
	 */
	public static String getMonth(String date) {

		String dateFormat = "MM";
		String result = "";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			result = new SimpleDateFormat(dateFormat).format(simpleDateFormat.parse(date));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	/**
	 * <pre>
	 * 	현재 일자를 반환한다. (yyyy + f1 + MM + f1 + dd + HH + f2 + mm + f2 + ss)
	 * </pre>
	 * 
	 * @param f1
	 *            : 날짜 구분자 (/,- 등) f2 : 초 구분자 (: 등)
	 * 
	 * @return 현재일자(yyyy + f1 + MM + f1 + dd + HH + f2 + mm + f2 + ss)
	 */
	public static String getDateTime(String f1, String f2) {
		String dateFormat = "yyyy" + f1 + "MM" + f1 + "dd" + " HH" + f2 + "mm" + f2 + "ss";
		return new SimpleDateFormat(dateFormat).format(new Date());
	}

	/**
	 * <pre>
	 * 	현재 일자를 반환한다. (yyyyMMddHHmmss) 12시간기준
	 * </pre>
	 * 
	 * @return 현재일자(yyyyMMddHHmmss)
	 */
	public static String getDateTime() {
		String dateFormat = "yyyyMMddhhmmss";
		return new SimpleDateFormat(dateFormat).format(new Date());
	}

	/**
	 * <pre>
	 * 	현재 일자를 반환한다. (yyyyMMddHHmmss) 24시간기준
	 * </pre>
	 * 
	 * @return 현재일자(yyyyMMddHHmmss)
	 */
	public static String getDateTime24Miss() {
		String dateFormat = "yyyyMMddHHmmss";
		return new SimpleDateFormat(dateFormat).format(new Date());
	}
	
	/**
	 * <pre>
	 * 	현재 일자를 반환한다. (yyyyMMddHHmmssSSS) 24시간기준
	 * </pre>
	 * 
	 * @return 현재일자(yyyyMMddHHmmss)
	 */
	public static String getDateTime24MissSSS() {
		String dateFormat = "yyyyMMddHHmmssSSS";
		return new SimpleDateFormat(dateFormat).format(new Date());
	}

	/**
	 * <pre>
	 * 현재일자 기준으로 -day(일) 날짜를 계산하여 반환한다. (yyyyMMdd)
	 * </pre>
	 * 
	 * @param day
	 *            : 이동할 일자 (-n일)
	 * 
	 * @return 현재일자 - day (yyyyMMdd)
	 */
	public static String getDayBefore(int day) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -day);
		String dateFormat = "yyyyMMdd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 특정일자 기준으로 -day(일) 날짜를 계산하여 반환한다. (yyyyMMdd)
	 * </pre>
	 * 
	 * @param date
	 *            : String 형식의 기준일자(yyyy-MM-dd),day : 이동할 일자 (+n일) , flag : 출력될 년/월/일 구분자
	 * 
	 * @return 계산된 일자 - day (yyyyMMdd)
	 * @throws ParseException
	 */
	public static String getDayBefore(String date, int day, String flag) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		try {
			cal.setTime(simpleDateFormat.parse(date));
			cal.add(Calendar.DATE, -day);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 현재일자 기준으로 -day(일) 날짜를 계산하여 반환한다. (yyyy + flag + MM + flag + dd)
	 * </pre>
	 * 
	 * @param day
	 *            : 이동할 일자 (-n일)
	 * 
	 * @return 현재일자 - day (yyyy + flag + MM + flag + dd)
	 */
	public static String getDayBeforeWithFlag(int day, String flag) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -day);
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 현재일자 기준으로 +day(일) 날짜를 계산하여 반환한다. (yyyyMMdd)
	 * </pre>
	 * 
	 * @param day
	 *            : 이동할 일자 (+n일)
	 * 
	 * @return 현재일자 + day (yyyyMMdd)
	 */
	public static String getDayAfter(int day, String flag) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, day);
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 특정일자 기준으로 +day(일) 날짜를 계산하여 반환한다. (yyyyMMdd)
	 * </pre>
	 * 
	 * @param date
	 *            : String 형식의 기준일자(yyyy-MM-dd),day : 이동할 일자 (+n일) , flag : 출력될 년/월/일 구분자
	 * 
	 * @return 현재일자 + day (yyyyMMdd)
	 */
	public static String getDayAfter(String date, int day, String flag) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		try {
			cal.setTime(simpleDateFormat.parse(date));
			cal.add(Calendar.DATE, day);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}
	
	/**
	 * <pre>
	 * 특정일자가 포함된 월의 마지막 날짜를 계산하여 반환한다. (yyyy-MM-dd)
	 * </pre>
	 * 
	 * @param date
	 *            : String 형식의 기준일자(yyyy-MM-dd),day : 이동할 일자 (+n일) , flag : 출력될 년/월/일 구분자
	 * 
	 * @return 현재일자 + day (yyyyMMdd)
	 */
	public static int getLastDayOfMonth(String date) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			cal.setTime(simpleDateFormat.parse(date));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	/**
	 * <pre>
	 * 현재일자 기준으로 -month(월) 날짜를 계산하여 반환한다. (yyyyMMdd)
	 * </pre>
	 * 
	 * @param month
	 *            : 이동할 월 (-n월)
	 * 
	 * @return 현재일자 + month (yyyyMMdd)
	 */
	public static String getMonthBefore(int month) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -month);
		String dateFormat = "yyyyMMdd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 현재일자 기준으로 -month(월) 날짜를 계산하여 특정 패턴에 의한 일자를 반환한다.
	 * </pre>
	 * 
	 * @param month
	 *            : 이동할 월 (-n월)
	 * 
	 * @return 패턴에  따라다름
	 */
	public static String getMonthBeforePattern(int month, String pattern) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -month);
		String dateFormat = pattern;
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}
	
	/**
	 * <pre>
	 * 현재일자 기준으로 -month(월) 날짜를 계산하여 반환한다. (yyyy + flag + MM + flag + dd)
	 * </pre>
	 * 
	 * @param month
	 *            : 이동할 월 (-n월)
	 * 
	 * @return 현재일자 + month (yyyy + flag + MM + flag + dd)
	 */
	public static String getMonthBeforeWithFlag(int month, String flag) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -month);
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 현재일자 기준으로 +month(월) 날짜를 계산하여 반환한다. (yyyy + flag + MM + flag + dd)
	 * </pre>
	 * 
	 * @param month
	 *            : 이동할 월 (-n월)
	 * 
	 * @return 현재일자 + month (yyyy + flag + MM + flag + dd)
	 */
	public static String getMonthAfterWithFlag(int month, String flag) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, +month);
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 현재일자 기준으로 +month(월) 날짜를 계산하여 반환한다. (yyyyMMdd)
	 * </pre>
	 * 
	 * @param month
	 *            : 이동할 월 (+n월)
	 * 
	 * @return 현재일자 + month (yyyyMMdd)
	 */
	public static String getMonthAfter(int month) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, month);
		String dateFormat = "yyyyMMdd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 현재일자 기준으로 -year(년) 날짜를 계산하여 반환한다. (yyyyMMdd)
	 * </pre>
	 * 
	 * @param year
	 *            : 이동할 년도 (-n년)
	 * 
	 * @return 현재일자 + year (yyyyMMdd)
	 */
	public static String getYearBefore(int year) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -year);
		String dateFormat = "yyyyMMdd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 현재일자 기준으로 +year(년) 날짜를 계산하여 반환한다. (yyyyMMdd)
	 * </pre>
	 * 
	 * @param year
	 *            : 이동할 년도 (+n년)
	 * 
	 * @return 현재일자 + year (yyyyMMdd)
	 */
	public static String getYearAfter(int year) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, year);
		String dateFormat = "yyyyMMdd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}
	
	/**
	 * <pre>
	 * 현재일자 기준으로 -year(년) 날짜를 계산하여 반환한다. (yyyy + flag + MM + flag + dd)
	 * </pre>
	 * 
	 * @param year
	 *            : 이동할 년도 (-n년)
	 * 
	 * @return 현재일자 + year (yyyy + flag + MM + flag + dd)
	 */
	public static String getYearBeforeWithFlag(int year, String flag) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -year);
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}
	
	/**
	 * <pre>
	 * 현재일자 기준으로 +year(년) 날짜를 계산하여 반환한다. (yyyy + flag + MM + flag + dd)
	 * </pre>
	 * 
	 * @param year
	 *            : 이동할 년도 (+n년)
	 * 
	 * @return 현재일자 + year (yyyy + flag + MM + flag + dd)
	 */
	public static String getYearAfterWithFlag(int year, String flag) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, year);
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 *  현재일자 기준으로 일주일 간의 날짜를 반환한다. (yyyy-MM-dd)
	 * </pre>
	 * 
	 * @return 현재일자 + date (yyyy-MM-dd)
	 */
	public static ArrayList<String> getOneWeekDateList() {
		Calendar cal = Calendar.getInstance();
		ArrayList<String> oneWeekDateList = new ArrayList<String>();

		for (int i = 0; i < 7; i++) {
			cal.setTime(new Date());
			cal.add(Calendar.DATE, -i);
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			String date = simpleDateFormat.format(cal.getTime());
			oneWeekDateList.add(date);
		}
		Collections.sort(oneWeekDateList);

		return oneWeekDateList;
	}

	/**
	 * <pre>
	 * 시작일과 종료일 사이의 날짜를 계산하여 반환한다. (yyyy-MM-dd)
	 * </pre>
	 * 
	 * @return 시작일과 종료일 사이의 날짜를 담은 ArrayList<String>
	 */
	public static ArrayList<String> getDateList(String startDate, String endDate) {
		ArrayList<String> dateList = new ArrayList<String>();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar sCal = Calendar.getInstance();
		Calendar eCal = Calendar.getInstance();
		Date sDate = new Date();
		Date eDate = new Date();

		try {
			sDate = simpleDateFormat.parse(startDate);
			eDate = simpleDateFormat.parse(endDate);
		} catch (Exception e) {
			e.printStackTrace();
		}

		sCal.setTime(sDate);
		eCal.setTime(eDate);

		while (!(sCal.compareTo(eCal) == 1)) {
			dateList.add(simpleDateFormat.format(sCal.getTime()));
			sCal.add(Calendar.DATE, 1);
		}

		return dateList;
	}

	/**
	 * <pre>
	 * 시작일과 종료일 사이의 주단위 시작일을 계산하여 반환한다. (yyyy-MM-dd)
	 * </pre>
	 * 
	 * @return 시작일과 종료일 사이의 주단위 시작일을 담은 ArrayList<String>
	 */
	public static ArrayList<String> getWeekList(ArrayList<String> dateList) {
		ArrayList<String> dList = dateList;
		ArrayList<String> wList = new ArrayList<String>();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();

		for (int i = 0; i < dList.size(); i++) {
			try {
				cal.setTime(simpleDateFormat.parse(dList.get(i)));
				cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				wList.add(simpleDateFormat.format(cal.getTime()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		HashSet<String> hashSet = new HashSet<String>(wList);
		ArrayList<String> weekList = new ArrayList<String>(hashSet);
		Collections.sort(weekList);

		return weekList;
	}

	/**
	 * <pre>
	 * 시작일과 종료일 사이의 월을 계산하여 반환한다. (yyyy-MM-dd)
	 * </pre>
	 * 
	 * @return 시작일과 종료일 사이의 월을 담은 ArrayList<String>
	 */
	public static ArrayList<String> getMonthList(ArrayList<String> dateList) {
		ArrayList<String> dList = dateList;
		ArrayList<String> mList = new ArrayList<String>();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();

		for (int i = 0; i < dList.size(); i++) {
			try {
				cal.setTime(simpleDateFormat.parse(dList.get(i)));
				if ((cal.get(Calendar.MONTH) + 1) < 10) {
					mList.add("0" + (cal.get(Calendar.MONTH) + 1));
				} else {
					mList.add("" + (cal.get(Calendar.MONTH) + 1));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		HashSet<String> hashSet = new HashSet<String>(mList);
		ArrayList<String> monthList = new ArrayList<String>(hashSet);
		Collections.sort(monthList);

		return monthList;
	}

	/**
	 * <pre>
	 * 특정일자 기준으로 -day(일) 날짜를 계산하여 반환한다. (yyyy + flag + MM + flag + dd)
	 * </pre>
	 * 
	 * @param day
	 *            : 이동할 일자 (-n일)
	 * 
	 * @return 특정일자 - day (yyyy + flag + MM + flag + dd)
	 */
	public static String getSpecificDayBeforeWithFlag(String date, int day, String flag) {
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		Calendar cal = Calendar.getInstance();
		Date specificDate = new Date();

		try {
			specificDate = new SimpleDateFormat(dateFormat).parse(date);
		} catch (Exception e) {
			e.printStackTrace();
		}

		cal.setTime(specificDate);
		cal.add(Calendar.DATE, -day);

		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 특정일자 기준으로 +day(일) 날짜를 계산하여 반환한다. (yyyy + flag + MM + flag + dd)
	 * </pre>
	 * 
	 * @param day
	 *            : 이동할 일자 (+n일)
	 * 
	 * @return 특정일자 + day (yyyy + flag + MM + flag + dd)
	 */
	public static String getSpecificDayAfterWithFlag(String date, int day, String flag) {
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		Calendar cal = Calendar.getInstance();
		Date specificDate = new Date();

		try {
			specificDate = new SimpleDateFormat(dateFormat).parse(date);
		} catch (Exception e) {
			e.printStackTrace();
		}

		cal.setTime(specificDate);
		cal.add(Calendar.DATE, day);

		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 특정일자 기준으로 -month(월) 날짜를 계산하여 반환한다. (yyyy + flag + MM + flag + dd)
	 * </pre>
	 * 
	 * @param month
	 *            : 이동할 월 (-n월)
	 * 
	 * @return 특정일자 + month (yyyy + flag + MM + flag + dd)
	 */
	public static String getSpecificMonthBeforeWithFlag(String date, int month, String flag) {
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		Calendar cal = Calendar.getInstance();
		Date specificDate = new Date();

		try {
			specificDate = new SimpleDateFormat(dateFormat).parse(date);
		} catch (Exception e) {
			e.printStackTrace();
		}

		cal.setTime(specificDate);
		cal.add(Calendar.MONTH, -month);

		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 특정일자 기준으로 +month(월) 날짜를 계산하여 반환한다. (yyyy + flag + MM + flag + dd)
	 * </pre>
	 * 
	 * @param month
	 *            : 이동할 월 (+n월)
	 * 
	 * @return 특정일자 + month (yyyy + flag + MM + flag + dd)
	 */
	public static String getSpecificMonthAfterWithFlag(String date, int month, String flag) {
		String dateFormat = "yyyy" + flag + "MM" + flag + "dd";
		Calendar cal = Calendar.getInstance();
		Date specificDate = new Date();

		try {
			specificDate = new SimpleDateFormat(dateFormat).parse(date);
		} catch (Exception e) {
			e.printStackTrace();
		}

		cal.setTime(specificDate);
		cal.add(Calendar.MONTH, month);

		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 특정일자가 포함된 주의 시작일을 계산하여 반환한다. (yyyy-MM-dd)
	 * </pre>
	 * 
	 * @return 특정일자가 포함된 주의 시작일 (yyyy-MM-dd)
	 */
	public static String getStartDayOfWeek(String date) {
		String dateFormat = "yyyy-MM-dd";
		Calendar cal = Calendar.getInstance();
		Date specificDate = new Date();

		try {
			specificDate = new SimpleDateFormat(dateFormat).parse(date);
		} catch (Exception e) {
			e.printStackTrace();
		}

		cal.setTime(specificDate);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 * 특정일자가 포함된 주의 종료일을 계산하여 반환한다. (yyyy-MM-dd)
	 * </pre>
	 * 
	 * @return 특정일자가 포함된 주의 종료일 (yyyy-MM-dd)
	 */
	public static String getEndDayOfWeek(String date) {
		String dateFormat = "yyyy-MM-dd";
		Calendar cal = Calendar.getInstance();
		Date specificDate = new Date();

		try {
			specificDate = new SimpleDateFormat(dateFormat).parse(date);
		} catch (Exception e) {
			e.printStackTrace();
		}

		cal.setTime(specificDate);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);

		return new SimpleDateFormat(dateFormat).format(cal.getTime());
	}

	/**
	 * <pre>
	 *  일일 시간 리스트
	 * </pre>
	 * 
	 * @return 00 ~ 23 까지 하루 시간을 담은 ArrayList<String>
	 */
	public static ArrayList<String> getDayTime() {
		ArrayList<String> dayTimeList = new ArrayList<String>();

		for (int i = 0; i < 24; i++) {
			String dayTime = (i < 10) ? "0" + i : i + "";
			dayTimeList.add(dayTime);
		}
		Collections.sort(dayTimeList);

		return dayTimeList;
	}
	
	/**
	 * <pre>
	 *  일 분 리스트 10~60 / 10분 단위
	 * </pre>
	 * 
	 * @return 10~60 까지 10분단위로 데이터 출력함.
	 */
	public static ArrayList<String> getDayMinutePer10(){
		ArrayList<String> dayTimeList = new ArrayList<String>();
		dayTimeList.add("00");
		for(int i=10; i<=60; i=i+10){
			dayTimeList.add(i + "");
		}
		return dayTimeList;
	}

	/**
	 * <pre>
	 *  금주의 시작일부터, 종료일까지 7일간의 데이터를 반환한다.
	 *  일,월,화,수,목,금,토
	 * </pre>
	 * 
	 * @return 시작부터 종료일까지 날짜 ex ) yyyy-MM-dd 형태의 날짜리스트
	 */
	public static ArrayList<String> getThisWeekList() {
		String startDate = DateMo.getStartDayOfWeek(DateMo.getDate("-"));
		String endDate = DateMo.getEndDayOfWeek(DateMo.getDate("-"));
		ArrayList<String> days = DateMo.getDateList(startDate, endDate);
		return days;
	}

	/**
	 * <pre>
	 *  특정 시작일부터, 종료일까지 7일간의 데이터를 반환한다.
	 *  일,월,화,수,목,금,토
	 * </pre>
	 * 
	 * @param date
	 *            : yyyy-MM-dd 형태
	 * @return 시작부터 종료일까지 날짜 ex ) yyyy-MM-dd 형태의 날짜리스트
	 */
	public static ArrayList<String> getThisWeekList(String date) {
		String startDate = DateMo.getStartDayOfWeek(date);
		String endDate = DateMo.getEndDayOfWeek(date);
		ArrayList<String> days = DateMo.getDateList(startDate, endDate);
		return days;
	}
	}
