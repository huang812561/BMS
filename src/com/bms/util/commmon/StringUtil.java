package com.bms.util.commmon;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 工具类
 * 作者：黄国强
 * 时间：2017/2/22
 */
public class StringUtil {

    @SuppressWarnings("unused")
	private final Log logger = LogFactory.getLog(StringUtil.class);

    public static boolean isEmpty(String arg){
        if(null==arg || "".equals(arg)){
            return true;
        }else{
            return false;
        }
    }

    public static boolean isEmpty(Object arg){
        if(null==arg || "".equals(arg)){
            return true;
        }else{
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
	public static boolean isEmpty(List list){
        if (null == list || list.isEmpty()){
            return true;
        }

        return false;
    }

    @SuppressWarnings("rawtypes")
	public static boolean isEmpty(Map map){
        if (null == map || map.isEmpty()){
            return true;
        }

        return false;
    }
    
    /**
     * 判断字符串是否为空
     *
     * @param strVal string
     * @return true 不为空 false 为空
     */
    public static boolean isNotEmpty(final String strVal)
    {
        return !isEmpty(strVal);
    }
    
    	 
    public static String replaceBlank(String str) {
        String dest = "";
        if (str!=null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }
    
    public static String safeToString(final Object obj)
    {
        return obj == null ? "" : obj.toString();
    }


    /**
     * <p>字符串快速分割。</p>
     *
     * <p>
     * 提供更高效简洁的分割算法，在相同情况下，分割效率约为<br>
     * {@link java.lang.String#split(String)}的4倍<br>
     * 支持字符串作为分割符。
     * </p>
     *
     * <p>
     * 当待分割字符串为null或者空串时，返回长度为0的数组<br>
     * 该方法不会抛出任何异常<br>
     * 如果传入的字符串中仅含有1个字符，那么该方法会被转交给{@link #split(String, char)}处理
     * </p>
     *
     * @param source 待分割字符串
     * @param splitStr 分隔符字符串
     * @return 分割完毕的字符串数组
     *
     * @see #split(String, char)
     */
    public static String[] split(final String source, final String splitStr)
    {
        String[] strArr = null;
        if (null == source || source.isEmpty())
        {
            strArr = new String[0];
        }
        else
        {
            if (splitStr.length() == 1)
            {
                strArr = split(source, splitStr.charAt(0));
            }
            else
            {
                int strLen = source.length();
                int splitStrLen = splitStr.length();
                List<String> strList = new LinkedList<String>();
                int start = 0;
                int end = 0;
                while (start < strLen)
                {
                    end = source.indexOf(splitStr, start);
                    if (end == -1)
                    {
                        String fregment = source.substring(start);
                        strList.add(fregment);
                        break;
                    }
                    if (start != end)
                    {
                        String fregment = source.substring(start, end);
                        strList.add(fregment);
                    }
                    start = end + splitStrLen;
                }

                strArr = new String[strList.size()];
                strList.toArray(strArr);
            }
        }

        return strArr;
    }

    /**
     * <p>字符串快速分割。</p>
     *
     * <p>
     * 提供更高效简洁的分割算法，在相同情况下，分割效率约为<br>
     * {@link java.lang.String#split(String)}的4倍<br>
     * 但仅支持单个字符作为分割符。
     * </p>
     *
     * <p>
     * 当待分割字符串为null或者空串时，返回长度为0的数组<br>
     * 该方法不会抛出任何异常
     * </p>
     *
     * @param source 待分割字符串
     * @param splitChar 分隔符
     * @return 分割完毕的字符串数组
     */
    public static String[] split(final String source, final char splitChar)
    {
        String[] strArr = null;
        List<String> strList = new LinkedList<String>();
        if (null == source || source.isEmpty())
        {
            strArr = new String[0];
        }
        else
        {
            char[] charArr = source.toCharArray();
            int start = 0;
            int end = 0;
            while (end < source.length())
            {
                char c = charArr[end];
                if (c == splitChar)
                {
                    if (start != end)
                    {
                        String fragment = source.substring(start, end);
                        strList.add(fragment);
                    }
                    start = end + 1;
                }
                ++end;
            }
            if (start < source.length())
            {
                strList.add(source.substring(start));
            }

            strArr = new String[strList.size()];
            strList.toArray(strArr);
        }

        return strArr;
    }
}
