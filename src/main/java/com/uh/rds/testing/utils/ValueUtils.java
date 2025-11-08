/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.uh.rds.testing.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Miscellaneous value utility methods.
 *
 * @author Charlie Zhang
 */
public class ValueUtils {

    public final static String STRING_NULL = null;

    public final static String STRING_BLANK = "";


	/**
	 * Determine if the given objects are equal, returning <code>true</code>
	 * if both are <code>null</code> or <code>false</code> if only one is
	 * <code>null</code>.
	 * <p>Compares arrays with <code>Arrays.equals</code>, performing an equality
	 * check based on the array elements rather than the array reference.
	 * @param o1 first Object to compare
	 * @param o2 second Object to compare
	 * @return whether the given objects are equal
	 * @see Arrays#equals
	 */
	public static boolean nullSafeEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.equals(o2)) {
			return true;
		}
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			if (o1 instanceof Object[] && o2 instanceof Object[]) {
				return Arrays.equals((Object[]) o1, (Object[]) o2);
			}
			if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
				return Arrays.equals((boolean[]) o1, (boolean[]) o2);
			}
			if (o1 instanceof byte[] && o2 instanceof byte[]) {
				return Arrays.equals((byte[]) o1, (byte[]) o2);
			}
			if (o1 instanceof char[] && o2 instanceof char[]) {
				return Arrays.equals((char[]) o1, (char[]) o2);
			}
			if (o1 instanceof double[] && o2 instanceof double[]) {
				return Arrays.equals((double[]) o1, (double[]) o2);
			}
			if (o1 instanceof float[] && o2 instanceof float[]) {
				return Arrays.equals((float[]) o1, (float[]) o2);
			}
			if (o1 instanceof int[] && o2 instanceof int[]) {
				return Arrays.equals((int[]) o1, (int[]) o2);
			}
			if (o1 instanceof long[] && o2 instanceof long[]) {
				return Arrays.equals((long[]) o1, (long[]) o2);
			}
			if (o1 instanceof short[] && o2 instanceof short[]) {
				return Arrays.equals((short[]) o1, (short[]) o2);
			}
		}
		return false;
	}

    public final static boolean equal(String str1, String str2) {
        if (str1 != null && str2 != null)
            return str1.equals(str2);
        else if (str1 == null && str2 == null)
            return true;
        else
            return false;
    }

    /**
     * if String is null or empty string will return true othterwise return false
     *
     * @param str
     * @return
     */
    public final static boolean isEmpty(String str) {
        return (str == null) || str.isEmpty();
    }

    public static boolean notEmpty(String str) {
        return ((str != null) && (!str.isEmpty()));
    }

    public static boolean notEmpty(Collection collection) {
        return (collection != null && !collection.isEmpty());
    }

	/**
	 * Return whether the given array is empty: that is, <code>null</code>
	 * or of zero length.
	 * @param array the array to check
	 * @return whether the given array is empty
	 */
	public static boolean isEmpty(Object[] array) {
		return (array == null || array.length == 0);
	}

	public static boolean notEmpty(Object[] array) {
		return (array != null && array.length > 0);
	}

	/**
	 * Return <code>true</code> if the supplied Collection is <code>null</code>
	 * or empty. Otherwise, return <code>false</code>.
	 * @param collection the Collection to check
	 * @return whether the given Collection is empty
	 */
	public static boolean isEmpty(Collection collection) {
		return (collection == null || collection.isEmpty());
	}

	/**
	 * Return <code>true</code> if the supplied Map is <code>null</code>
	 * or empty. Otherwise, return <code>false</code>.
	 * @param map the Map to check
	 * @return whether the given Map is empty
	 */
	public static boolean isEmpty(Map map) {
		return (map == null || map.isEmpty());
	}



    /**
     * If str is "true" or "yes" it will return true
     *
     * @param str
     * @return
     */
    public final static boolean isTrue(String str) {
        if ("true".equals(str) || "yes".equals(str)) {
            return true;
        } else {
            return false;
        }
    }

    public final static boolean isFloat(String str) {
        try {
            Float.parseFloat(str);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public final static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
        }
        catch (Exception e) {
            return false;
        }

        return true;
    }

    public final static boolean isLong(String str) {
        try {
            Long.parseLong(str);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public final static String blankNull(String str) {
        if (str == null)
            return STRING_BLANK;
        else
            return str;
    }

	public final static String escapeSQLParam(String str) {
		StringBuffer buf = new StringBuffer(str.length() + 20);
		int len = str.length();
		for (int i = 0; i < len; i++) {
			char a = str.charAt(i);
			if (a == '\'') {
				buf.append(a);
			}
			buf.append(a);
		}
		return buf.toString();
	}


}
